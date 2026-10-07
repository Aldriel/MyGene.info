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

// @vitest-environment jsdom
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { toResultJson } from './export/resultFile.js'
import { STORAGE_KEY } from './state/preferences.js'
import { fakeBiothings, RESULT } from './test/fixtures.js'

beforeEach(() => {
  localStorage.clear()
  window.history.replaceState(null, '', '/')
  document.documentElement.removeAttribute('style')
  document.documentElement.lang = 'en'
})

function renderApp() {
  const user = userEvent.setup()
  return { user, ...render(<App />) }
}

const storedPreferences = () => JSON.parse(localStorage.getItem(STORAGE_KEY))

async function searchGene(user, symbol) {
  const input = screen.getByRole('searchbox', { name: /Gene symbol|Symbole du gène/ })
  await user.clear(input)
  await user.type(input, `${symbol}{Enter}`)
}

describe('App', () => {
  it('welcomes the user with examples and the signature', () => {
    renderApp()

    expect(screen.getByRole('heading', { level: 1, name: 'MyGene Explorer' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Explore a human gene' })).toBeInTheDocument()
    expect(screen.getByText('© 2026 Maxime Ethier - Biocomputing Consultant')).toBeInTheDocument()
    expect(document.title).toBe('MyGene Explorer')
  })

  it('searches a gene and shows its variants', async () => {
    const fetchMock = fakeBiothings(vi)
    const { user } = renderApp()

    await searchGene(user, 'brca1')

    expect(await screen.findByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()
    expect(screen.getByText('Showing 3 of 3 loaded · 13,542 in ClinVar')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(new URL(fetchMock.mock.calls[1][0]).searchParams.get('size')).toBe('100')
    expect(window.location.search).toBe('?gene=BRCA1')
    expect(document.title).toBe('BRCA1 · MyGene Explorer')
    expect(screen.getByRole('searchbox', { name: 'Gene symbol' })).toHaveValue('BRCA1')
  })

  it('fetches the chosen number of variants and remembers it', async () => {
    const fetchMock = fakeBiothings(vi)
    const { user } = renderApp()

    await user.selectOptions(screen.getByRole('combobox', { name: 'Max. variants' }), '500')
    await searchGene(user, 'BRCA1')
    await screen.findByRole('heading', { name: 'BRCA1' })

    expect(new URL(fetchMock.mock.calls[1][0]).searchParams.get('size')).toBe('500')
    expect(storedPreferences().maxVariants).toBe(500)
  })

  it('shows a loading indicator during the search', async () => {
    let release
    fakeBiothings(vi, (url) =>
      url.host === 'mygene.info'
        ? new Promise((resolve) => {
            release = () => resolve({ body: { hits: [] } })
          })
        : { body: {} },
    )
    const { user } = renderApp()

    await searchGene(user, 'BRCA1')

    expect(screen.getByText('Querying MyGene.info and MyVariant.info…')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Searching…' })).toBeDisabled()
    await act(async () => release())
    expect(await screen.findByRole('alert')).toHaveTextContent('No human gene found')
  })

  it('rejects an invalid symbol without calling the API', async () => {
    const fetchMock = fakeBiothings(vi)
    const { user } = renderApp()

    await searchGene(user, 'B*')

    expect(screen.getByRole('alert')).toHaveTextContent('"B*" is not a valid gene symbol.')
    expect(screen.getByRole('searchbox')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('explains an unknown gene and API failures', async () => {
    fakeBiothings(vi, (url) =>
      url.searchParams.get('q') === 'symbol:BAD' ? { status: 503 } : { body: { hits: [] } },
    )
    const { user } = renderApp()

    await searchGene(user, 'NOPE1')
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No human gene found for symbol "NOPE1".',
    )

    await searchGene(user, 'BAD')
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(
        'mygene.info is temporarily unavailable (HTTP 503).',
      ),
    )
    expect(window.location.search).toBe('')
  })

  it('switches the language live, including the current error, and remembers it', async () => {
    fakeBiothings(vi)
    const { user } = renderApp()
    await searchGene(user, 'NOPE1')
    await screen.findByRole('alert')

    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'fr')

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Aucun gène humain trouvé pour le symbole « NOPE1 ».',
    )
    expect(screen.getByRole('button', { name: 'Rechercher' })).toBeInTheDocument()
    expect(
      screen.getByText('© 2026 Maxime Ethier - Consultant en Bio-informatique'),
    ).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('fr')
    expect(storedPreferences().language).toBe('fr')
  })

  it('applies and remembers the text size', async () => {
    const { user } = renderApp()

    expect(document.documentElement.style.fontSize).toBe('16px')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Text size' }), 'Large')

    expect(document.documentElement.style.fontSize).toBe('18px')
    expect(storedPreferences().fontSize).toBe(18)
  })

  it('restores the stored preferences', () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ language: 'fr', fontSize: 20, maxVariants: 250, recentSearches: ['CFTR'] }),
    )

    renderApp()

    expect(screen.getByRole('heading', { name: 'Explorez un gène humain' })).toBeInTheDocument()
    expect(document.documentElement.style.fontSize).toBe('20px')
    expect(screen.getByRole('combobox', { name: 'Variants max.' })).toHaveValue('250')
    expect(screen.getByText('Recherches récentes :')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'CFTR' })).toHaveLength(2)
  })

  it('keeps recent searches and lets the user clear them', async () => {
    fakeBiothings(vi)
    const { user } = renderApp()

    await searchGene(user, 'tp53')
    await screen.findByRole('heading', { name: 'TP53' })
    await searchGene(user, 'BRCA1')
    await screen.findByRole('heading', { name: 'BRCA1' })

    expect(storedPreferences().recentSearches).toEqual(['BRCA1', 'TP53'])
    await user.click(screen.getByRole('button', { name: 'TP53' }))
    expect(await screen.findByRole('heading', { name: 'TP53' })).toBeInTheDocument()
    expect(storedPreferences().recentSearches).toEqual(['TP53', 'BRCA1'])

    await user.click(screen.getByRole('button', { name: 'Clear history' }))
    expect(screen.queryByText('Recent searches:')).toBeNull()
    expect(storedPreferences().recentSearches).toEqual([])
  })

  it('searches the gene of a shared link on load', async () => {
    fakeBiothings(vi)
    window.history.replaceState(null, '', '/?gene=tp53')

    renderApp()

    expect(await screen.findByRole('heading', { name: 'TP53' })).toBeInTheDocument()
    expect(window.location.search).toBe('?gene=tp53')
  })

  it('follows the browser history', async () => {
    fakeBiothings(vi)
    const { user } = renderApp()
    await searchGene(user, 'BRCA1')
    await screen.findByRole('heading', { name: 'BRCA1' })
    await searchGene(user, 'TP53')
    await screen.findByRole('heading', { name: 'TP53' })

    await act(async () => {
      window.history.replaceState(null, '', '/?gene=BRCA1')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })
    expect(await screen.findByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()

    await act(async () => {
      window.history.replaceState(null, '', '/')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })
    expect(screen.getByRole('heading', { name: 'Explore a human gene' })).toBeInTheDocument()
    expect(screen.getByRole('searchbox')).toHaveValue('')
  })

  it('returns to the welcome page from the title', async () => {
    fakeBiothings(vi)
    const { user } = renderApp()
    await searchGene(user, 'BRCA1')
    await screen.findByRole('heading', { name: 'BRCA1' })

    await user.click(screen.getByRole('link', { name: 'MyGene Explorer' }))

    expect(screen.getByRole('heading', { name: 'Explore a human gene' })).toBeInTheDocument()
    expect(window.location.search).toBe('')
    expect(document.title).toBe('MyGene Explorer')
  })

  it('opens a results file', async () => {
    const { user } = renderApp()
    const file = new File([toResultJson(RESULT, '1.0.0')], 'BRCA1_clinvar.json', {
      type: 'application/json',
    })

    await user.upload(screen.getByTestId('result-file-input'), file)

    expect(await screen.findByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()
    expect(screen.getByText('Showing 5 of 5 loaded · 13,542 in ClinVar')).toBeInTheDocument()
    expect(screen.getByText(/^Results opened from BRCA1_clinvar\.json/)).toBeInTheDocument()
    expect(window.location.search).toBe('?gene=BRCA1')
  })

  it('reports an invalid results file and keeps the current screen', async () => {
    const { user } = renderApp()
    const file = new File(['{"format":"other"}'], 'notes.json', { type: 'application/json' })

    await user.upload(screen.getByTestId('result-file-input'), file)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'notes.json is not a valid MyGene Explorer results file.',
    )
    expect(screen.getByRole('heading', { name: 'Explore a human gene' })).toBeInTheDocument()
  })

  it('searches an example gene from the welcome page', async () => {
    fakeBiothings(vi)
    const { user } = renderApp()

    await user.click(screen.getByRole('button', { name: 'BRCA1' }))

    expect(await screen.findByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()
  })
})
