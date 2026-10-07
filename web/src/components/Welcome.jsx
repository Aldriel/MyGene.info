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

import { useI18n } from '../i18n/I18nContext.jsx'

/** Well-known genes suggested on the welcome screen. */
export const EXAMPLE_GENES = ['BRCA1', 'TP53', 'CFTR', 'HLA-A']

/**
 * Welcome screen shown before the first search.
 *
 * @param {{onSearch: (symbol: string) => void, onOpenFile: () => void}} props
 */
export default function Welcome({ onSearch, onOpenFile }) {
  const { t } = useI18n()

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
      <h2 className="text-xl font-semibold text-slate-900">{t('welcome.title')}</h2>
      <p className="mt-2 max-w-3xl text-slate-600">{t('welcome.text')}</p>

      <div className="mt-5 flex flex-wrap items-center gap-2">
        <span className="text-sm text-slate-600">{t('welcome.examples')}</span>
        {EXAMPLE_GENES.map((symbol) => (
          <button
            key={symbol}
            type="button"
            onClick={() => onSearch(symbol)}
            className="rounded-md bg-indigo-50 px-3 py-1 font-mono text-sm font-medium text-indigo-700 ring-1 ring-indigo-200 transition ring-inset hover:bg-indigo-100"
          >
            {symbol}
          </button>
        ))}
      </div>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-5">
        <button
          type="button"
          onClick={onOpenFile}
          className="text-sm font-medium text-indigo-600 hover:text-indigo-800 hover:underline"
        >
          {t('welcome.open')}
        </button>
        <p className="text-xs text-slate-500">{t('app.disclaimer')}</p>
      </div>
    </section>
  )
}
