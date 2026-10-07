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
 * Client for the public BioThings APIs.
 *
 * - MyGene.info    : gene information (symbol, name, summary, type, location, aliases).
 * - MyVariant.info : ClinVar variants. MyGene.info does not expose ClinVar,
 *                    so this data comes from its sibling service.
 *
 * Every failure is reported as an `ApiError` whose `code`, `host` and `status` let the
 * interface build a translated message; its `message` is an English default.
 */

import { normalizeGeneSymbol } from './geneSymbol.js'

const MYGENE_URL = 'https://mygene.info/v3'
const MYVARIANT_URL = 'https://myvariant.info/v1'

export const REQUEST_TIMEOUT_MS = 20_000

/** Number of variants the user can choose to fetch; the API accepts at most 1000. */
export const MAX_VARIANTS_CHOICES = [100, 250, 500, 1000]

const GENE_FIELDS = 'symbol,name,entrezgene,summary,type_of_gene,map_location,alias'

const CLINVAR_FIELDS = [
  'clinvar.variant_id',
  'clinvar.rcv.clinical_significance',
  'clinvar.rcv.origin',
].join(',')

/** Error with a user-friendly message describing why an API call failed. */
export class ApiError extends Error {
  /**
   * @param {string} message English message suitable for display.
   * @param {{code?: 'timeout'|'unreachable'|'http'|'invalidResponse', host?: string,
   *   status?: number, cause?: unknown}} [details]
   */
  constructor(message, { code, host, status, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.code = code
    this.host = host
    this.status = status
  }
}

/** Builds the English message for a non-2xx HTTP status. */
export function httpErrorMessage(status, host) {
  if (status === 429) {
    return `Too many requests sent to ${host}. Please wait a moment and try again.`
  }
  if (status >= 500) {
    return `${host} is temporarily unavailable (HTTP ${status}). Please try again later.`
  }
  if (status === 400) {
    return `${host} rejected the request (HTTP 400). Please check the gene symbol.`
  }
  return `Unexpected response from ${host} (HTTP ${status}).`
}

/**
 * The API returns either a single object or an array depending on the
 * number of items: always work with an array.
 */
const toArray = (value) => {
  if (value == null) return []
  return Array.isArray(value) ? value : [value]
}

/** Keeps the non-blank texts of a list, without duplicates. */
const uniqueTexts = (values) => [
  ...new Set(values.filter((value) => value != null && String(value).trim() !== '').map(String)),
]

/**
 * Performs a GET request and returns the parsed JSON.
 *
 * Cancellation through `signal` is propagated as an `AbortError` so callers
 * can ignore superseded requests; every other failure becomes an `ApiError`.
 */
async function getJson(url, signal) {
  const host = new URL(url).host
  const timeout = AbortSignal.timeout(REQUEST_TIMEOUT_MS)
  const combined = signal ? AbortSignal.any([signal, timeout]) : timeout

  let response
  try {
    response = await fetch(url, { signal: combined })
  } catch (err) {
    if (signal?.aborted) throw err
    if (timeout.aborted) {
      throw new ApiError(`${host} did not respond in time. Please try again.`, {
        code: 'timeout',
        host,
        cause: err,
      })
    }
    throw new ApiError(`Unable to reach ${host}. Please check your internet connection.`, {
      code: 'unreachable',
      host,
      cause: err,
    })
  }

  if (!response.ok) {
    throw new ApiError(httpErrorMessage(response.status, host), {
      code: 'http',
      host,
      status: response.status,
    })
  }

  try {
    return await response.json()
  } catch (err) {
    throw new ApiError(`Received an invalid response from ${host}.`, {
      code: 'invalidResponse',
      host,
      cause: err,
    })
  }
}

/**
 * Looks up a human gene by symbol.
 *
 * @param {string} symbol Gene symbol (e.g. "BRCA1").
 * @param {AbortSignal} [signal] Signal used to cancel the request.
 * @returns {Promise<{id: string, symbol: string, name: string|null, entrezGene: number|null,
 *   summary: string|null, typeOfGene: string|null, mapLocation: string|null,
 *   aliases: string[]}|null>} The gene, or `null` if none matches.
 * @throws {InvalidSymbolError} If the symbol is malformed.
 * @throws {ApiError} If the request fails.
 */
export async function fetchGene(symbol, signal) {
  const params = new URLSearchParams({
    q: `symbol:${normalizeGeneSymbol(symbol)}`,
    species: 'human',
    fields: GENE_FIELDS,
    size: '1',
  })
  const data = await getJson(`${MYGENE_URL}/query?${params}`, signal)
  const hit = data.hits?.[0]
  if (!hit) return null

  return {
    id: String(hit._id),
    symbol: hit.symbol,
    name: hit.name ?? null,
    entrezGene: hit.entrezgene ?? null,
    summary: hit.summary ?? null,
    typeOfGene: hit.type_of_gene ?? null,
    mapLocation: hit.map_location ?? null,
    aliases: uniqueTexts(toArray(hit.alias)),
  }
}

/**
 * Fetches the ClinVar variants associated with a gene.
 *
 * A variant may have several ClinVar submissions (RCV): their clinical
 * significances and origins are merged without duplicates.
 *
 * @param {string} symbol Gene symbol.
 * @param {AbortSignal} [signal] Signal used to cancel the request.
 * @param {number} [size=100] Maximum number of variants to fetch, between 1 and 1000.
 * @returns {Promise<{total: number, variants: object[]}>}
 * @throws {InvalidSymbolError} If the symbol is malformed.
 * @throws {RangeError} If the size is out of range.
 * @throws {ApiError} If the request fails.
 */
export async function fetchClinvarVariants(symbol, signal, size = 100) {
  if (!Number.isInteger(size) || size < 1 || size > 1000) {
    throw new RangeError(`size must be an integer between 1 and 1000: ${size}`)
  }
  const params = new URLSearchParams({
    q: `clinvar.gene.symbol:${normalizeGeneSymbol(symbol)}`,
    fields: CLINVAR_FIELDS,
    size: String(size),
  })
  const data = await getJson(`${MYVARIANT_URL}/query?${params}`, signal)

  const variants = toArray(data.hits).flatMap((hit) =>
    toArray(hit.clinvar).map((clinvar, index) => {
      const rcvs = toArray(clinvar.rcv)
      return {
        key: `${hit._id}-${clinvar.variant_id ?? `#${index}`}`,
        hgvs: hit._id,
        variantId: clinvar.variant_id ?? null,
        significances: uniqueTexts(rcvs.map((rcv) => rcv.clinical_significance)),
        origins: uniqueTexts(rcvs.flatMap((rcv) => toArray(rcv.origin))),
      }
    }),
  )

  return { total: data.total ?? 0, variants }
}
