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

import { MAX_VARIANTS_CHOICES } from '../api/biothings.js'
import { useI18n } from '../i18n/I18nContext.jsx'

const INPUT_CLASSES = [
  'min-w-0 flex-1 rounded-lg border bg-white px-4 py-2.5 text-slate-900 shadow-sm',
  'placeholder:text-slate-400 focus:ring-2 focus:outline-none',
].join(' ')

const BUTTON_CLASSES = [
  'rounded-lg bg-indigo-600 px-5 py-2.5 font-medium text-white shadow-sm transition',
  'hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50',
].join(' ')

const CHIP_CLASSES = [
  'rounded-full border border-slate-300 bg-white px-3 py-0.5 font-mono text-xs text-slate-700',
  'transition hover:border-indigo-400 hover:text-indigo-700',
].join(' ')

/**
 * Search form: gene symbol, number of variants to fetch and recent searches.
 *
 * @param {{query: string, onQueryChange: (query: string) => void, invalid: boolean,
 *   loading: boolean, maxVariants: number, onMaxVariantsChange: (count: number) => void,
 *   onSearch: (query: string) => void, recentSearches: string[],
 *   onClearRecent: () => void}} props
 */
export default function SearchBar({
  query,
  onQueryChange,
  invalid,
  loading,
  maxVariants,
  onMaxVariantsChange,
  onSearch,
  recentSearches,
  onClearRecent,
}) {
  const { t, number } = useI18n()

  return (
    <section className="space-y-3">
      <form
        role="search"
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          onSearch(query)
        }}
        className="flex flex-wrap gap-3"
      >
        <input
          type="search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder={t('search.placeholder')}
          aria-label={t('search.label')}
          aria-invalid={invalid}
          autoComplete="off"
          spellCheck={false}
          className={`${INPUT_CLASSES} ${
            invalid
              ? 'border-red-400 focus:border-red-500 focus:ring-red-200'
              : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-200'
          }`}
        />
        <label className="flex items-center gap-2 text-sm text-slate-600">
          {t('search.maxVariants')}
          <select
            value={maxVariants}
            onChange={(event) => onMaxVariantsChange(Number(event.target.value))}
            className="rounded-lg border border-slate-300 bg-white px-2 py-2.5 text-slate-800 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 focus:outline-none"
          >
            {MAX_VARIANTS_CHOICES.map((count) => (
              <option key={count} value={count}>
                {number(count)}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" disabled={!query.trim() || loading} className={BUTTON_CLASSES}>
          {loading ? t('search.searching') : t('search.button')}
        </button>
      </form>

      {recentSearches.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 text-sm text-slate-600">
          <span>{t('search.recent')}</span>
          <ul className="contents">
            {recentSearches.map((symbol) => (
              <li key={symbol} className="contents">
                <button
                  type="button"
                  className={CHIP_CLASSES}
                  disabled={loading}
                  onClick={() => onSearch(symbol)}
                >
                  {symbol}
                </button>
              </li>
            ))}
          </ul>
          <button
            type="button"
            onClick={onClearRecent}
            className="text-xs text-slate-500 underline-offset-2 hover:text-slate-800 hover:underline"
          >
            {t('search.clearRecent')}
          </button>
        </div>
      )}
    </section>
  )
}
