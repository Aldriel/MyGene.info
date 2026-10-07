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
import { InvalidSymbolError, MAX_SYMBOL_LENGTH, normalizeGeneSymbol } from './geneSymbol.js'

describe('normalizeGeneSymbol', () => {
  it.each([
    ['BRCA1', 'BRCA1'],
    ['brca1', 'BRCA1'],
    ['  TP53  ', 'TP53'],
    ['HLA-A', 'HLA-A'],
    ['C9orf72', 'C9ORF72'],
    ['NKX2-1', 'NKX2-1'],
    ['H2AC1', 'H2AC1'],
    ['MT-ND1', 'MT-ND1'],
    ['SNORD3A_1', 'SNORD3A_1'],
    ['GBA1.2', 'GBA1.2'],
  ])('accepts %j and returns %j', (input, expected) => {
    expect(normalizeGeneSymbol(input)).toBe(expected)
  })

  it.each([undefined, null, '', '   '])('asks for a symbol when the input is %j', (input) => {
    expect(() => normalizeGeneSymbol(input)).toThrow('Please enter a gene symbol.')
  })

  it.each([
    ['wildcard', 'B*'],
    ['single-character wildcard', 'BRCA?'],
    ['several words', 'BRCA1 TP53'],
    ['field prefix', 'name:kinase'],
    ['parentheses', '((('],
    ['quotes', '"BRCA1"'],
    ['boolean operator', 'BRCA1||TP53'],
    ['accented letters', 'GÈNE1'],
    ['symbols', '@#!'],
    ['leading hyphen', '-BRCA1'],
    ['HTML', '<script>'],
  ])('rejects a %s (%j)', (_label, input) => {
    expect(() => normalizeGeneSymbol(input)).toThrow(InvalidSymbolError)
    expect(() => normalizeGeneSymbol(input)).toThrow('is not a valid gene symbol')
  })

  it.each([
    ['', 'empty', ''],
    ['  B* ', 'malformed', 'B*'],
    ['A'.repeat(MAX_SYMBOL_LENGTH + 1), 'tooLong', 'A'.repeat(MAX_SYMBOL_LENGTH + 1)],
  ])('reports why %j is rejected', (input, reason, symbol) => {
    const error = (() => {
      try {
        normalizeGeneSymbol(input)
      } catch (err) {
        return err
      }
    })()

    expect(error).toBeInstanceOf(InvalidSymbolError)
    expect(error).toMatchObject({ name: 'InvalidSymbolError', reason, symbol })
  })

  it('rejects symbols longer than the maximum length', () => {
    const tooLong = 'A'.repeat(MAX_SYMBOL_LENGTH + 1)

    expect(normalizeGeneSymbol('A'.repeat(MAX_SYMBOL_LENGTH))).toHaveLength(MAX_SYMBOL_LENGTH)
    expect(() => normalizeGeneSymbol(tooLong)).toThrow(
      `Gene symbols are at most ${MAX_SYMBOL_LENGTH} characters long.`,
    )
  })
})
