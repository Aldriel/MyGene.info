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

import { describe, expect, it } from 'vitest'
import { variant } from '../test/fixtures.js'
import {
  CATEGORIES,
  categoriesOf,
  classify,
  colorOf,
  countByPrimary,
  primaryOf,
  severityOf,
} from './significance.js'

describe('classify', () => {
  it.each([
    ['Pathogenic', 'pathogenic'],
    ['Likely pathogenic', 'likely-pathogenic'],
    ['Pathogenic/Likely pathogenic', 'likely-pathogenic'],
    ['Conflicting interpretations of pathogenicity', 'conflicting'],
    ['Uncertain significance', 'uncertain'],
    ['Likely benign', 'likely-benign'],
    ['Benign', 'benign'],
    ['BENIGN', 'benign'],
    ['not provided', 'other'],
    ['risk factor', 'other'],
    ['', 'other'],
    [undefined, 'other'],
  ])('classifies %j as %s', (significance, category) => {
    expect(classify(significance)).toBe(category)
  })
})

describe('categoriesOf and primaryOf', () => {
  it('returns every category of a variant and its most severe one', () => {
    const v = variant(1, ['Benign', 'Likely pathogenic', 'Benign'])

    expect(categoriesOf(v)).toEqual(new Set(['benign', 'likely-pathogenic']))
    expect(primaryOf(v)).toBe('likely-pathogenic')
  })

  it('puts a variant without significance in "other"', () => {
    expect(categoriesOf(variant(1, []))).toEqual(new Set(['other']))
    expect(primaryOf(variant(1, []))).toBe('other')
  })
})

describe('severityOf and colorOf', () => {
  it('ranks categories from the most to the least severe', () => {
    expect(CATEGORIES.map(({ id }) => severityOf(id))).toEqual([0, 1, 2, 3, 4, 5, 6])
    expect(severityOf('pathogenic')).toBeLessThan(severityOf('benign'))
  })

  it('uses the desktop application colors', () => {
    expect(colorOf('pathogenic')).toBe('#dc2626')
    expect(colorOf('other')).toBe('#94a3b8')
  })
})

describe('countByPrimary', () => {
  it('counts each variant once, in its most severe category', () => {
    const counts = countByPrimary([
      variant(1, ['Pathogenic', 'Benign']),
      variant(2, ['Benign']),
      variant(3, ['Benign']),
      variant(4, []),
    ])

    expect(counts.map(({ id, count }) => [id, count])).toEqual([
      ['pathogenic', 1],
      ['likely-pathogenic', 0],
      ['conflicting', 0],
      ['uncertain', 0],
      ['likely-benign', 0],
      ['benign', 2],
      ['other', 1],
    ])
    expect(counts[0].color).toBe('#dc2626')
  })
})
