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
import { RESULT } from '../test/fixtures.js'
import {
  FORMAT,
  FORMAT_VERSION,
  parseResultJson,
  ResultFileError,
  toResultJson,
} from './resultFile.js'

/** Document as written by the desktop application (Jackson, `Instant.toString()`). */
const DESKTOP_JSON = JSON.stringify({
  format: 'mygene-explorer/search-result',
  formatVersion: 1,
  generator: { application: 'MyGene Explorer', version: '1.0.0' },
  sources: { gene: 'https://mygene.info' },
  query: 'tp53',
  retrievedAt: '2026-10-06T14:30:00Z',
  gene: {
    id: '7157',
    symbol: 'TP53',
    name: 'tumor protein p53',
    entrezGene: 7157,
    typeOfGene: 'protein-coding',
    mapLocation: '17p13.1',
    summary: null,
    aliases: ['P53', 'LFS1'],
  },
  clinvar: {
    total: 4000,
    variants: [
      {
        variantId: 12345,
        hgvs: 'chr17:g.7673802C>T',
        clinicalSignificances: ['Pathogenic'],
        origins: ['germline'],
        clinvarUrl: 'https://www.ncbi.nlm.nih.gov/clinvar/variation/12345/',
      },
      { variantId: null, hgvs: null, clinicalSignificances: [], origins: [], clinvarUrl: null },
    ],
  },
})

function parseError(json, fileName = 'file.json') {
  try {
    parseResultJson(json, fileName)
  } catch (err) {
    return err
  }
  throw new Error('Expected parseResultJson to fail')
}

describe('toResultJson', () => {
  it('writes a self-describing document', () => {
    const doc = JSON.parse(toResultJson(RESULT, '1.2.3'))

    expect(doc).toMatchObject({
      format: FORMAT,
      formatVersion: FORMAT_VERSION,
      generator: { application: 'MyGene Explorer', version: '1.2.3' },
      query: 'BRCA1',
      retrievedAt: RESULT.retrievedAt,
      gene: { symbol: 'BRCA1', entrezGene: 672, aliases: ['BRCAI', 'BRCC1', 'RNF53'] },
      clinvar: { total: 13542 },
    })
    expect(doc.clinvar.variants[1]).toEqual({
      variantId: 2,
      hgvs: 'chr17:g.43000002G>A',
      clinicalSignificances: ['Pathogenic'],
      origins: ['germline', 'somatic'],
      clinvarUrl: 'https://www.ncbi.nlm.nih.gov/clinvar/variation/2/',
    })
    expect(doc.clinvar.variants[4].clinvarUrl).toBeNull()
  })

  it('is indented for readability', () => {
    expect(toResultJson(RESULT, '1')).toContain('\n  "format"')
  })
})

describe('parseResultJson', () => {
  it('round-trips a result', () => {
    const parsed = parseResultJson(toResultJson(RESULT, '1'), 'a.json')

    expect(parsed.gene).toEqual(RESULT.gene)
    expect(parsed.query).toBe(RESULT.query)
    expect(parsed.retrievedAt).toBe(RESULT.retrievedAt)
    expect(parsed.clinvar.total).toBe(RESULT.clinvar.total)
    expect(parsed.clinvar.variants.map(({ key: _key, ...rest }) => rest)).toEqual(
      RESULT.clinvar.variants.map(({ key: _key, ...rest }) => rest),
    )
  })

  it('opens a file written by the desktop application', () => {
    const parsed = parseResultJson(DESKTOP_JSON, 'TP53_clinvar.json')

    expect(parsed.gene).toEqual({
      id: '7157',
      symbol: 'TP53',
      name: 'tumor protein p53',
      entrezGene: 7157,
      summary: null,
      typeOfGene: 'protein-coding',
      mapLocation: '17p13.1',
      aliases: ['P53', 'LFS1'],
    })
    expect(parsed.clinvar.variants).toEqual([
      {
        key: 'chr17:g.7673802C>T-12345-0',
        hgvs: 'chr17:g.7673802C>T',
        variantId: 12345,
        significances: ['Pathogenic'],
        origins: ['germline'],
      },
      { key: '--1', hgvs: null, variantId: null, significances: [], origins: [] },
    ])
  })

  it('gives unique keys to identical variants', () => {
    const doc = JSON.parse(DESKTOP_JSON)
    doc.clinvar.variants = [doc.clinvar.variants[0], doc.clinvar.variants[0]]

    const keys = parseResultJson(JSON.stringify(doc), 'a.json').clinvar.variants.map((v) => v.key)

    expect(new Set(keys).size).toBe(2)
  })

  it('counts the variants when the total is missing', () => {
    const doc = JSON.parse(DESKTOP_JSON)
    delete doc.clinvar.total

    expect(parseResultJson(JSON.stringify(doc), 'a.json').clinvar.total).toBe(2)
  })

  it.each([
    ['not JSON', '{oops'],
    ['another document', JSON.stringify({ format: 'other' })],
    ['an array', '[]'],
    [
      'no gene symbol',
      JSON.stringify({ format: FORMAT, query: 'x', retrievedAt: '2026-01-01T00:00:00Z' }),
    ],
    [
      'no query',
      JSON.stringify({ format: FORMAT, gene: { symbol: 'A' }, retrievedAt: '2026-01-01' }),
    ],
    [
      'an invalid date',
      JSON.stringify({ format: FORMAT, query: 'a', gene: { symbol: 'A' }, retrievedAt: 'soon' }),
    ],
  ])('rejects %s as invalid', (_label, json) => {
    const error = parseError(json, 'bad.json')

    expect(error).toBeInstanceOf(ResultFileError)
    expect(error).toMatchObject({ reason: 'invalid', fileName: 'bad.json' })
  })

  it('rejects a newer format version', () => {
    const doc = JSON.parse(DESKTOP_JSON)
    doc.formatVersion = FORMAT_VERSION + 1

    expect(parseError(JSON.stringify(doc))).toMatchObject({ reason: 'unsupported' })
  })
})
