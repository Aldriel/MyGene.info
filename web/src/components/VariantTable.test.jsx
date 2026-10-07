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
import { screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { VARIANTS } from '../test/fixtures.js'
import { renderWithI18n } from '../test/render.jsx'
import VariantTable, { nextSort } from './VariantTable.jsx'

const NO_FILTER = { text: '', category: '' }

function renderTable(props = {}, language) {
  const handlers = { onFilterChange: vi.fn(), onSortChange: vi.fn() }
  const view = renderWithI18n(
    <VariantTable
      rows={VARIANTS}
      loaded={VARIANTS.length}
      total={13542}
      filter={NO_FILTER}
      sort={null}
      {...handlers}
      {...props}
    />,
    { language },
  )
  return { ...view, ...handlers }
}

describe('nextSort', () => {
  it('sorts a new column ascending, then toggles', () => {
    expect(nextSort(null, 'hgvs')).toEqual({ column: 'hgvs', direction: 'asc' })
    expect(nextSort({ column: 'hgvs', direction: 'asc' }, 'hgvs')).toEqual({
      column: 'hgvs',
      direction: 'desc',
    })
    expect(nextSort({ column: 'hgvs', direction: 'desc' }, 'hgvs').direction).toBe('asc')
    expect(nextSort({ column: 'hgvs', direction: 'desc' }, 'origin').direction).toBe('asc')
  })
})

describe('VariantTable', () => {
  it('shows one row per variant with ClinVar links and colored badges', () => {
    renderTable()

    const rows = screen.getAllByRole('row').slice(1)
    expect(rows).toHaveLength(VARIANTS.length)
    expect(within(rows[1]).getByRole('link', { name: '2' })).toHaveAttribute(
      'href',
      'https://www.ncbi.nlm.nih.gov/clinvar/variation/2/',
    )
    expect(within(rows[1]).getByText('Pathogenic')).toHaveAttribute('data-category', 'pathogenic')
    expect(within(rows[1]).getByText('germline, somatic')).toBeInTheDocument()
    expect(within(rows[4]).queryByRole('link')).toBeNull()
    expect(screen.getByText('Showing 5 of 5 loaded · 13,542 in ClinVar')).toBeInTheDocument()
  })

  it('reports the sort state and requests a new sort on click', async () => {
    const { user, onSortChange } = renderTable({
      sort: { column: 'significance', direction: 'desc' },
    })

    const significance = screen.getByRole('columnheader', { name: /Clinical significance/ })
    expect(significance).toHaveAttribute('aria-sort', 'descending')
    expect(screen.getByRole('columnheader', { name: /Origin/ })).toHaveAttribute(
      'aria-sort',
      'none',
    )

    await user.click(within(significance).getByRole('button'))
    await user.click(screen.getByRole('button', { name: /HGVS/ }))

    expect(onSortChange.mock.calls).toEqual([
      [{ column: 'significance', direction: 'asc' }],
      [{ column: 'hgvs', direction: 'asc' }],
    ])
  })

  it('edits the text and category filter', async () => {
    const { user, onFilterChange } = renderTable()

    await user.type(screen.getByRole('searchbox', { name: 'Filter variants' }), 'x')
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Clinical significance category' }),
      'benign',
    )

    expect(onFilterChange).toHaveBeenNthCalledWith(1, { text: 'x', category: '' })
    expect(onFilterChange).toHaveBeenNthCalledWith(2, { text: '', category: 'benign' })
  })

  it('clears an active filter', async () => {
    const { user, onFilterChange } = renderTable({
      filter: { text: 'abc', category: 'benign' },
      rows: [],
    })

    expect(screen.getByText('No variant matches the filter.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Clear filter' }))

    expect(onFilterChange).toHaveBeenCalledWith(NO_FILTER)
  })

  it('hides the clear button without filter', () => {
    renderTable()

    expect(screen.queryByRole('button', { name: 'Clear filter' })).toBeNull()
  })

  it('explains when the gene has no variant', () => {
    renderTable({ rows: [], loaded: 0, total: 0 })

    expect(screen.getByText('No ClinVar variant found for this gene.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('is translated', () => {
    renderTable({}, 'fr')

    expect(screen.getByRole('columnheader', { name: /ID du variant/ })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Probablement pathogène' })).toBeInTheDocument()
    expect(screen.getByText(/5 affichés sur 5 chargés · 13\s542 dans ClinVar/)).toBeInTheDocument()
  })
})
