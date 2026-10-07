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
import { describe, expect, it } from 'vitest'
import { variant, VARIANTS } from '../test/fixtures.js'
import { renderWithI18n } from '../test/render.jsx'
import SummaryChart from './SummaryChart.jsx'

describe('SummaryChart', () => {
  it('draws one segment per non-empty category and lists every category', () => {
    const { container } = renderWithI18n(<SummaryChart variants={VARIANTS} total={13542} />)

    expect(screen.getByRole('img', { name: /Pie chart/ })).toBeInTheDocument()
    expect(
      [...container.querySelectorAll('circle[data-category]')].map((c) => c.dataset.category),
    ).toEqual(['pathogenic', 'uncertain', 'benign', 'other'])
    expect(screen.getAllByRole('listitem')).toHaveLength(7)
    expect(screen.getByText('Pathogenic or likely pathogenic: 2 (40%)')).toBeInTheDocument()
    expect(screen.getAllByText('1 (20%)')).toHaveLength(3)
    expect(screen.getByText(/Based on the 5 variants loaded out of 13,542/)).toBeInTheDocument()
  })

  it('makes the segments add up to the full circle', () => {
    const { container } = renderWithI18n(
      <SummaryChart variants={[variant(1, ['Benign']), variant(2, ['Pathogenic'])]} total={2} />,
    )

    const lengths = [...container.querySelectorAll('circle[data-category]')].map((c) =>
      Number(c.getAttribute('stroke-dasharray').split(' ')[0]),
    )
    expect(lengths).toEqual([50, 50])
  })

  it('explains when there is nothing to summarize', () => {
    renderWithI18n(<SummaryChart variants={[]} total={0} />)

    expect(screen.getByText('No variant to summarize.')).toBeInTheDocument()
  })

  it('is translated', () => {
    renderWithI18n(<SummaryChart variants={VARIANTS} total={13542} />, { language: 'fr' })

    expect(
      screen.getByText(/Pathogènes ou probablement pathogènes : 2 \(40\s%\)/),
    ).toBeInTheDocument()
  })
})
