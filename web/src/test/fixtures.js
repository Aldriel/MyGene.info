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

/**
 * Sample data and fakes shared by the tests.
 */

import { onTestFinished } from 'vitest'

export const GENE = {
  id: '672',
  symbol: 'BRCA1',
  name: 'BRCA1 DNA repair associated',
  entrezGene: 672,
  summary: 'This gene encodes a nuclear phosphoprotein that plays a role in genomic stability.',
  typeOfGene: 'protein-coding',
  mapLocation: '17q21.31',
  aliases: ['BRCAI', 'BRCC1', 'RNF53'],
}

/** Builds a ClinVar variant with sensible defaults. */
export function variant(variantId, significances, origins = ['germline'], hgvs) {
  return {
    key: `v-${variantId}`,
    hgvs: hgvs ?? `chr17:g.${43_000_000 + (variantId ?? 0)}G>A`,
    variantId,
    significances,
    origins,
  }
}

export const VARIANTS = [
  variant(10, ['Benign']),
  variant(2, ['Pathogenic'], ['germline', 'somatic']),
  variant(33, ['Uncertain significance']),
  variant(4, ['Likely pathogenic', 'Pathogenic']),
  variant(null, [], [], 'chr17:g.1del'),
]

export const RESULT = {
  query: 'BRCA1',
  gene: GENE,
  clinvar: { total: 13542, variants: VARIANTS },
  retrievedAt: '2026-10-06T14:30:00.000Z',
}

/** API responses of a successful BRCA1 search. */
export const MYGENE_RESPONSE = {
  total: 1,
  hits: [
    {
      _id: '672',
      symbol: 'BRCA1',
      name: GENE.name,
      entrezgene: 672,
      summary: GENE.summary,
      type_of_gene: GENE.typeOfGene,
      map_location: GENE.mapLocation,
      alias: GENE.aliases,
    },
  ],
}

export const MYVARIANT_RESPONSE = {
  total: 13542,
  hits: [
    {
      _id: 'chr17:g.43045712G>A',
      clinvar: {
        variant_id: 55001,
        rcv: { clinical_significance: 'Pathogenic', origin: 'germline' },
      },
    },
    {
      _id: 'chr17:g.43045800C>T',
      clinvar: { variant_id: 55002, rcv: { clinical_significance: 'Benign', origin: 'germline' } },
    },
    {
      _id: 'chr17:g.43045900A>G',
      clinvar: {
        variant_id: 55003,
        rcv: { clinical_significance: 'Uncertain significance', origin: 'somatic' },
      },
    },
  ],
}

/**
 * Replaces `URL.createObjectURL` and `URL.revokeObjectURL` for the current test.
 *
 * @returns {{createObjectURL: Function, revokeObjectURL: Function}} The mocks.
 */
export function stubObjectUrls(vi) {
  const original = {
    createObjectURL: URL.createObjectURL,
    revokeObjectURL: URL.revokeObjectURL,
  }
  const mocks = {
    createObjectURL: vi.fn(() => 'blob:fake'),
    revokeObjectURL: vi.fn(),
  }
  Object.assign(URL, mocks)
  onTestFinished(() => Object.assign(URL, original))
  return mocks
}

/**
 * Replaces `fetch` with a fake BioThings server.
 *
 * @param {(url: URL) => {status?: number, body?: unknown}|Promise<...>} [handler] Response of
 *   each request; by default a successful BRCA1 search.
 * @returns The mock, whose calls are the requested URLs.
 */
export function fakeBiothings(vi, handler = defaultHandler) {
  const fetchMock = vi.fn(async (input, init) => {
    const url = new URL(input)
    const { status = 200, body = {} } = await handler(url, init)
    return { ok: status < 400, status, json: async () => body }
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function defaultHandler(url) {
  if (url.host === 'mygene.info') {
    const symbol = url.searchParams.get('q').replace('symbol:', '')
    if (symbol !== 'BRCA1' && symbol !== 'TP53') return { body: { total: 0, hits: [] } }
    if (symbol === 'TP53') {
      return { body: { hits: [{ _id: '7157', symbol: 'TP53', name: 'tumor protein p53' }] } }
    }
    return { body: MYGENE_RESPONSE }
  }
  return { body: MYVARIANT_RESPONSE }
}
