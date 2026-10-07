/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useMemo, useState } from 'react'
import { APP_VERSION, BRAND } from '../brand.js'
import { exportResult } from '../export/exportResult.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { matchesFilter, sortVariants } from '../model/variants.js'
import ExportMenu from './ExportMenu.jsx'
import GeneCard from './GeneCard.jsx'
import SummaryChart from './SummaryChart.jsx'
import VariantTable from './VariantTable.jsx'

const TABS = ['variants', 'summary']

const SECONDARY_BUTTON_CLASSES = [
  'rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700',
  'shadow-sm transition hover:bg-slate-50',
].join(' ')

/**
 * Result of a search: gene card, actions (export, share, open), the variants and summary tabs,
 * then the data sources, version and disclaimer.
 *
 * @param {{result: object, shareUrl: string, onOpenFile: () => void,
 *   onStatus: (render: (translator: object) => string, isError?: boolean) => void}} props
 *   `onStatus` receives a function of the translator so the message follows language changes.
 */
export default function ResultsView({ result, shareUrl, onOpenFile, onStatus }) {
  const translator = useI18n()
  const { t } = translator
  const [tab, setTab] = useState('variants')
  const [filter, setFilter] = useState({ text: '', category: '' })
  const [sort, setSort] = useState(null)
  const [exporting, setExporting] = useState(false)

  const { variants, total } = result.clinvar
  const rows = useMemo(
    () =>
      sortVariants(
        variants.filter((variant) => matchesFilter(variant, filter)),
        sort,
      ),
    [variants, filter, sort],
  )

  async function handleExport(format) {
    setExporting(true)
    if (format === 'pdf') onStatus((tr) => tr.t('status.preparingPdf'))
    try {
      const file = await exportResult(format, result, rows, translator)
      onStatus((tr) => tr.t('status.downloaded', { file }))
    } catch {
      onStatus((tr) => tr.t('error.export'), true)
    } finally {
      setExporting(false)
    }
  }

  async function handleCopyLink() {
    try {
      await navigator.clipboard.writeText(shareUrl)
      onStatus((tr) => tr.t('status.linkCopied'))
    } catch {
      onStatus((tr) => tr.t('error.clipboard'), true)
    }
  }

  return (
    <div className="space-y-6">
      <GeneCard gene={result.gene} retrievedAt={result.retrievedAt} />

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 pt-3">
          <div role="tablist" aria-label={t('table.title')} className="flex gap-1">
            {TABS.map((id) => (
              <button
                key={id}
                id={`tab-${id}`}
                type="button"
                role="tab"
                aria-selected={tab === id}
                aria-controls={`panel-${id}`}
                onClick={() => setTab(id)}
                className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition ${
                  tab === id
                    ? 'border-indigo-600 text-indigo-700'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                {t(`tab.${id}`)}
              </button>
            ))}
          </div>
          <div className="flex flex-wrap items-center gap-2 pb-3">
            <button type="button" onClick={handleCopyLink} className={SECONDARY_BUTTON_CLASSES}>
              {t('actions.copyLink')}
            </button>
            <button type="button" onClick={onOpenFile} className={SECONDARY_BUTTON_CLASSES}>
              {t('actions.open')}
            </button>
            <ExportMenu onExport={handleExport} disabled={exporting} />
          </div>
        </div>

        <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
          {tab === 'variants' ? (
            <VariantTable
              rows={rows}
              loaded={variants.length}
              total={total}
              filter={filter}
              onFilterChange={setFilter}
              sort={sort}
              onSortChange={setSort}
            />
          ) : (
            <SummaryChart variants={variants} total={total} />
          )}
        </div>
      </section>

      <p className="text-center text-xs text-slate-500">
        {t('status.sources')} · {BRAND.application} {t('footer.version', { version: APP_VERSION })}{' '}
        · {t('app.disclaimer')}
      </p>
    </div>
  )
}
