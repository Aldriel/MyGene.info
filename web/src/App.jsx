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

import { useCallback, useEffect, useRef, useState } from 'react'
import { InvalidSymbolError } from './api/geneSymbol.js'
import { BRAND } from './brand.js'
import Footer from './components/Footer.jsx'
import Header from './components/Header.jsx'
import ResultsView from './components/ResultsView.jsx'
import SearchBar from './components/SearchBar.jsx'
import StatusToast from './components/StatusToast.jsx'
import Welcome from './components/Welcome.jsx'
import { parseResultJson, ResultFileError } from './export/resultFile.js'
import { describeError } from './i18n/errors.js'
import { I18nProvider, useI18n } from './i18n/I18nContext.jsx'
import { geneFromSearch, urlForGene } from './state/shareUrl.js'
import { useGeneSearch } from './state/useGeneSearch.js'
import { usePreferences } from './state/usePreferences.js'

/** Root component: applies the preferences and provides the interface language. */
export default function App() {
  const prefs = usePreferences()
  return (
    <I18nProvider language={prefs.preferences.language}>
      <MainScreen {...prefs} />
    </I18nProvider>
  )
}

/**
 * Main screen: header, search, welcome or results, footer and notifications. The searched
 * gene is mirrored in the address (`?gene=BRCA1`) so results can be bookmarked and shared, and
 * the browser back button returns to the previous gene.
 */
function MainScreen({
  preferences,
  setLanguage,
  setFontSize,
  setMaxVariants,
  rememberSearch,
  clearRecentSearches,
}) {
  const translator = useI18n()
  const { t } = translator
  const { status, result, error, search, showResult, reset } = useGeneSearch()
  const [query, setQuery] = useState(() => geneFromSearch(window.location.search) ?? '')
  const [toast, setToast] = useState(null)
  const fileInputRef = useRef(null)
  const { maxVariants } = preferences

  const toastCount = useRef(0)
  const notify = useCallback((render, isError = false) => {
    toastCount.current += 1
    setToast({ render, isError, id: toastCount.current })
  }, [])
  const dismissToast = useCallback(() => setToast(null), [])

  /** Searches a gene; `push` records it in the browser history. */
  const runSearch = useCallback(
    async (input, { push = true } = {}) => {
      setQuery(input)
      const found = await search(input, maxVariants)
      if (!found) return
      const symbol = found.gene.symbol
      setQuery(symbol)
      rememberSearch(symbol)
      const url = urlForGene(window.location.href, symbol)
      if (push && url !== window.location.href) window.history.pushState(null, '', url)
    },
    [search, rememberSearch, maxVariants],
  )

  useEffect(() => {
    const gene = geneFromSearch(window.location.search)
    if (gene) runSearch(gene, { push: false })
  }, []) // Only the address at load time triggers a search.

  useEffect(() => {
    const onPopState = () => {
      const gene = geneFromSearch(window.location.search)
      if (gene) {
        runSearch(gene, { push: false })
      } else {
        setQuery('')
        reset()
      }
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [runSearch, reset])

  useEffect(() => {
    document.title = result ? `${result.gene.symbol} · ${BRAND.application}` : BRAND.application
  }, [result])

  function goHome() {
    setQuery('')
    reset()
    const url = urlForGene(window.location.href, null)
    if (url !== window.location.href) window.history.pushState(null, '', url)
  }

  async function handleFile(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    let opened
    try {
      let text
      try {
        text = await file.text()
      } catch (cause) {
        throw new ResultFileError('unreadable', file.name, cause)
      }
      opened = parseResultJson(text, file.name)
    } catch (err) {
      notify((tr) => describeError(err, tr.t), true)
      return
    }
    showResult(opened)
    setQuery(opened.gene.symbol)
    const url = urlForGene(window.location.href, opened.gene.symbol)
    if (url !== window.location.href) window.history.pushState(null, '', url)
    notify((tr) =>
      tr.t('status.loaded', { file: file.name, date: tr.dateTime(opened.retrievedAt) }),
    )
  }

  const openFile = () => fileInputRef.current?.click()
  const loading = status === 'loading'

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Header
        language={preferences.language}
        fontSize={preferences.fontSize}
        onLanguageChange={setLanguage}
        onFontSizeChange={setFontSize}
        onHome={goHome}
      />

      <main className="mx-auto w-full max-w-6xl flex-1 space-y-6 px-4 py-8">
        <SearchBar
          query={query}
          onQueryChange={setQuery}
          invalid={status === 'error' && error instanceof InvalidSymbolError}
          loading={loading}
          maxVariants={maxVariants}
          onMaxVariantsChange={setMaxVariants}
          onSearch={(input) => runSearch(input)}
          recentSearches={preferences.recentSearches}
          onClearRecent={clearRecentSearches}
        />

        {status === 'idle' && (
          <Welcome onSearch={(symbol) => runSearch(symbol)} onOpenFile={openFile} />
        )}

        {loading && (
          <div
            role="status"
            className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-5 text-slate-600"
          >
            <span className="h-5 w-5 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600" />
            {t('loading')}
          </div>
        )}

        {status === 'error' && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5 text-red-800">
            <p className="font-semibold">{t('error.title')}</p>
            <p className="text-sm">{describeError(error, t)}</p>
          </div>
        )}

        {status === 'success' && result && (
          <ResultsView
            key={`${result.gene.symbol}-${result.retrievedAt}`}
            result={result}
            shareUrl={urlForGene(window.location.href, result.gene.symbol)}
            onOpenFile={openFile}
            onStatus={notify}
          />
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept=".json,application/json"
          onChange={handleFile}
          className="hidden"
          data-testid="result-file-input"
        />
      </main>

      <Footer />

      <StatusToast
        status={
          toast && { message: toast.render(translator), isError: toast.isError, id: toast.id }
        }
        onDismiss={dismissToast}
        dismissLabel={t('actions.dismiss')}
      />
    </div>
  )
}
