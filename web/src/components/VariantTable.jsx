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
import { CATEGORIES } from '../model/significance.js'
import { clinvarUrl } from '../model/variants.js'
import SignificanceBadge from './SignificanceBadge.jsx'

/** Sortable columns, in display order. */
const COLUMNS = [
  { id: 'variantId', label: 'table.variantId' },
  { id: 'hgvs', label: 'table.hgvs' },
  { id: 'significance', label: 'table.significance' },
  { id: 'origin', label: 'table.origin' },
]

const FIELD_CLASSES = [
  'rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800',
  'focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 focus:outline-none',
].join(' ')

/**
 * Returns the sort following a click on a column header: ascending first, then toggling.
 *
 * @param {{column: string, direction: 'asc'|'desc'}|null} current Current sort.
 * @param {string} column Column clicked.
 * @returns {{column: string, direction: 'asc'|'desc'}} The new sort.
 */
export function nextSort(current, column) {
  if (current?.column !== column) return { column, direction: 'asc' }
  return { column, direction: current.direction === 'asc' ? 'desc' : 'asc' }
}

/**
 * Filterable, sortable table of ClinVar variants. Filter and sort are controlled by the parent
 * so the exports contain exactly the rows on screen.
 *
 * @param {{rows: object[], loaded: number, total: number,
 *   filter: {text: string, category: string}, onFilterChange: (filter: object) => void,
 *   sort: {column: string, direction: string}|null, onSortChange: (sort: object) => void}} props
 */
export default function VariantTable({
  rows,
  loaded,
  total,
  filter,
  onFilterChange,
  sort,
  onSortChange,
}) {
  const { t } = useI18n()
  const filtered = Boolean(filter.text || filter.category)

  if (loaded === 0) {
    return <p className="p-6 text-slate-600">{t('table.empty')}</p>
  }

  return (
    <div>
      <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-4">
        <input
          type="search"
          value={filter.text}
          onChange={(event) => onFilterChange({ ...filter, text: event.target.value })}
          placeholder={t('filter.prompt')}
          aria-label={t('filter.label')}
          className={`${FIELD_CLASSES} min-w-0 flex-1`}
        />
        <select
          value={filter.category}
          onChange={(event) => onFilterChange({ ...filter, category: event.target.value })}
          aria-label={t('filter.category')}
          className={FIELD_CLASSES}
        >
          <option value="">{t('filter.allCategories')}</option>
          {CATEGORIES.map(({ id }) => (
            <option key={id} value={id}>
              {t(`significance.${id}`)}
            </option>
          ))}
        </select>
        {filtered && (
          <button
            type="button"
            onClick={() => onFilterChange({ text: '', category: '' })}
            className="text-sm text-slate-600 hover:text-slate-900 hover:underline"
          >
            {t('filter.clear')}
          </button>
        )}
        <p className="w-full text-xs text-slate-500" aria-live="polite">
          {t('filter.count', { shown: rows.length, loaded, total })}
        </p>
      </div>

      {rows.length === 0 ? (
        <p className="p-6 text-slate-600">{t('filter.noMatch')}</p>
      ) : (
        <div className="max-h-[36rem] overflow-auto">
          <table className="min-w-full text-sm">
            <caption className="sr-only">{t('table.title')}</caption>
            <thead className="sticky top-0 z-10 bg-slate-50 text-left text-xs font-semibold tracking-wide text-slate-600 uppercase shadow-[inset_0_-1px_0_var(--color-slate-200)]">
              <tr>
                {COLUMNS.map(({ id, label }) => (
                  <SortableHeader
                    key={id}
                    label={t(label)}
                    sortLabel={t('table.sortBy', { column: t(label) })}
                    direction={sort?.column === id ? sort.direction : null}
                    onClick={() => onSortChange(nextSort(sort, id))}
                  />
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((variant) => (
                <VariantRow key={variant.key} variant={variant} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/** Column header that sorts the table when clicked. */
function SortableHeader({ label, sortLabel, direction, onClick }) {
  const ariaSort = direction === 'asc' ? 'ascending' : direction === 'desc' ? 'descending' : 'none'
  return (
    <th scope="col" aria-sort={ariaSort} className="px-4 py-3">
      <button
        type="button"
        onClick={onClick}
        title={sortLabel}
        className="inline-flex items-center gap-1 uppercase hover:text-slate-900"
      >
        {label}
        <span aria-hidden="true" className={direction ? 'text-indigo-600' : 'text-slate-300'}>
          {direction === 'desc' ? '▼' : '▲'}
        </span>
      </button>
    </th>
  )
}

/** One table row: identifier, HGVS notation, clinical significances and origins. */
function VariantRow({ variant }) {
  const url = clinvarUrl(variant)
  return (
    <tr className="align-top transition-colors hover:bg-slate-50">
      <td className="px-4 py-2.5 whitespace-nowrap">
        {url ? (
          <a
            href={url}
            target="_blank"
            rel="noreferrer"
            className="font-medium text-indigo-600 hover:underline"
          >
            {variant.variantId}
          </a>
        ) : (
          <span className="text-slate-400">—</span>
        )}
      </td>
      <td className="px-4 py-2.5 font-mono text-xs break-all text-slate-600">
        {variant.hgvs ?? '—'}
      </td>
      <td className="px-4 py-2.5">
        <div className="flex flex-wrap gap-1.5">
          {variant.significances.length > 0 ? (
            variant.significances.map((significance) => (
              <SignificanceBadge key={significance} significance={significance} />
            ))
          ) : (
            <span className="text-slate-400">—</span>
          )}
        </div>
      </td>
      <td className="px-4 py-2.5 text-slate-700">
        {variant.origins.length > 0 ? variant.origins.join(', ') : '—'}
      </td>
    </tr>
  )
}
