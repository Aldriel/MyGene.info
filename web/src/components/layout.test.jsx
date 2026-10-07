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
import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithI18n } from '../test/render.jsx'
import Footer from './Footer.jsx'
import Header from './Header.jsx'
import SearchBar from './SearchBar.jsx'
import Welcome, { EXAMPLE_GENES } from './Welcome.jsx'

describe('Header', () => {
  function renderHeader(language = 'en') {
    const handlers = { onLanguageChange: vi.fn(), onFontSizeChange: vi.fn(), onHome: vi.fn() }
    const view = renderWithI18n(<Header language={language} fontSize={16} {...handlers} />, {
      language,
    })
    return { ...view, ...handlers }
  }

  it('changes the language and the text size', async () => {
    const { user, onLanguageChange, onFontSizeChange } = renderHeader()

    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'Français')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Text size' }), 'Extra large')

    expect(onLanguageChange).toHaveBeenCalledWith('fr')
    expect(onFontSizeChange).toHaveBeenCalledWith(20)
  })

  it('returns to the welcome page from the title', async () => {
    const { user, onHome } = renderHeader()

    await user.click(screen.getByRole('link', { name: 'MyGene Explorer' }))

    expect(onHome).toHaveBeenCalledOnce()
  })

  it('is translated', () => {
    renderHeader('fr')

    expect(screen.getByRole('combobox', { name: 'Taille du texte' })).toHaveDisplayValue('Normale')
  })
})

describe('SearchBar', () => {
  function renderBar(props = {}) {
    const handlers = {
      onQueryChange: vi.fn(),
      onMaxVariantsChange: vi.fn(),
      onSearch: vi.fn(),
      onClearRecent: vi.fn(),
    }
    const view = renderWithI18n(
      <SearchBar
        query="brca1"
        invalid={false}
        loading={false}
        maxVariants={100}
        recentSearches={['TP53', 'CFTR']}
        {...handlers}
        {...props}
      />,
    )
    return { ...view, ...handlers }
  }

  it('submits the query', async () => {
    const { user, onSearch } = renderBar()

    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(onSearch).toHaveBeenCalledWith('brca1')
  })

  it('reports typing and the number of variants', async () => {
    const { user, onQueryChange, onMaxVariantsChange } = renderBar({ query: '' })

    await user.type(screen.getByRole('searchbox', { name: 'Gene symbol' }), 'T')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Max. variants' }), '1,000')

    expect(onQueryChange).toHaveBeenCalledWith('T')
    expect(onMaxVariantsChange).toHaveBeenCalledWith(1000)
    expect(screen.getByRole('button', { name: 'Search' })).toBeDisabled()
  })

  it('searches a recent gene and clears the history', async () => {
    const { user, onSearch, onClearRecent } = renderBar()

    await user.click(screen.getByRole('button', { name: 'CFTR' }))
    await user.click(screen.getByRole('button', { name: 'Clear history' }))

    expect(onSearch).toHaveBeenCalledWith('CFTR')
    expect(onClearRecent).toHaveBeenCalledOnce()
  })

  it('hides the history when empty', () => {
    renderBar({ recentSearches: [] })

    expect(screen.queryByText('Recent searches:')).toBeNull()
  })

  it('shows the loading and invalid states', () => {
    renderBar({ loading: true, invalid: true })

    expect(screen.getByRole('button', { name: 'Searching…' })).toBeDisabled()
    expect(screen.getByRole('searchbox')).toHaveAttribute('aria-invalid', 'true')
  })
})

describe('Welcome', () => {
  it('suggests example genes and opening a file', async () => {
    const onSearch = vi.fn()
    const onOpenFile = vi.fn()
    const { user } = renderWithI18n(<Welcome onSearch={onSearch} onOpenFile={onOpenFile} />)

    for (const symbol of EXAMPLE_GENES) {
      expect(screen.getByRole('button', { name: symbol })).toBeInTheDocument()
    }
    await user.click(screen.getByRole('button', { name: 'HLA-A' }))
    await user.click(screen.getByRole('button', { name: /Open a results file/ }))

    expect(onSearch).toHaveBeenCalledWith('HLA-A')
    expect(onOpenFile).toHaveBeenCalledOnce()
    expect(screen.getByText(/Not intended for clinical decision-making/)).toBeInTheDocument()
  })
})

describe('Footer', () => {
  it('signs with the English business title, contact, licence, availability and Ko-fi', () => {
    renderWithI18n(<Footer />)

    expect(
      screen.getByRole('link', { name: '© 2026 Maxime Ethier - Biocomputing Consultant' }),
    ).toHaveAttribute('href', 'https://www.maximeethier.com/en')
    expect(screen.getByRole('link', { name: 'contact@maximeethier.com' })).toHaveAttribute(
      'href',
      'mailto:contact@maximeethier.com',
    )
    expect(screen.getByRole('link', { name: 'Apache License 2.0' })).toHaveAttribute(
      'href',
      expect.stringMatching(/LICENSE\.txt$/),
    )
    expect(
      screen.getByText('Available for bioinformatics contracts or employment.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Support me on Ko-fi' })).toHaveAttribute(
      'href',
      'https://ko-fi.com/K1S228CL7A',
    )
  })

  it('signs with the French business title and website', () => {
    renderWithI18n(<Footer />, { language: 'fr' })

    expect(
      screen.getByRole('link', { name: '© 2026 Maxime Ethier - Consultant en Bio-informatique' }),
    ).toHaveAttribute('href', 'https://www.maximeethier.com')
    expect(
      screen.getByText('Disponible pour des contrats ou un emploi en bio-informatique.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Licence Apache 2.0' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Soutenez-moi sur Ko-fi' })).toBeInTheDocument()
  })
})
