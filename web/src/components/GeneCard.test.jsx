// @vitest-environment jsdom
import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { GENE, RESULT } from '../test/fixtures.js'
import { renderWithI18n } from '../test/render.jsx'
import GeneCard, { SUMMARY_PREVIEW_LENGTH } from './GeneCard.jsx'

const renderCard = (gene = GENE, language) =>
  renderWithI18n(<GeneCard gene={gene} retrievedAt={RESULT.retrievedAt} />, { language })

describe('GeneCard', () => {
  it('shows the gene identity and details', () => {
    renderCard()

    expect(screen.getByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()
    expect(screen.getByText('BRCA1 DNA repair associated')).toBeInTheDocument()
    expect(screen.getByText('protein-coding')).toBeInTheDocument()
    expect(screen.getByText('17q21.31')).toBeInTheDocument()
    expect(screen.getByText('BRCAI, BRCC1, RNF53')).toBeInTheDocument()
    expect(screen.getByText(GENE.summary)).toBeInTheDocument()
    expect(screen.getByText(/^Data retrieved .*2026/)).toBeInTheDocument()
  })

  it('links to NCBI Gene and GeneCards in a new tab', () => {
    renderCard()

    expect(screen.getByRole('link', { name: 'NCBI Gene 672' })).toHaveAttribute(
      'href',
      'https://www.ncbi.nlm.nih.gov/gene/672',
    )
    const geneCards = screen.getByRole('link', { name: 'GeneCards' })
    expect(geneCards).toHaveAttribute(
      'href',
      'https://www.genecards.org/cgi-bin/carddisp.pl?gene=BRCA1',
    )
    expect(geneCards).toHaveAttribute('target', '_blank')
    expect(geneCards).toHaveAttribute('rel', 'noreferrer')
  })

  it('collapses a long summary until expanded', async () => {
    const summary = 'word '.repeat(SUMMARY_PREVIEW_LENGTH)
    const { user } = renderCard({ ...GENE, summary })

    const toggle = screen.getByRole('button', { name: 'Show full summary' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByText(/…$/)).toBeInTheDocument()

    await user.click(toggle)

    expect(screen.getByRole('button', { name: 'Collapse summary' })).toHaveAttribute(
      'aria-expanded',
      'true',
    )
    expect(screen.queryByText(/…$/)).toBeNull()
  })

  it('omits missing information', () => {
    renderCard({
      ...GENE,
      name: null,
      entrezGene: null,
      summary: null,
      typeOfGene: null,
      mapLocation: null,
      aliases: [],
    })

    expect(screen.queryByText('Type')).toBeNull()
    expect(screen.queryByText('Summary')).toBeNull()
    expect(screen.queryByRole('link', { name: /NCBI/ })).toBeNull()
    expect(screen.getByRole('link', { name: 'GeneCards' })).toBeInTheDocument()
  })

  it('is translated', () => {
    renderCard(GENE, 'fr')

    expect(screen.getByText('Localisation')).toBeInTheDocument()
    expect(screen.getByText('Résumé')).toBeInTheDocument()
  })
})
