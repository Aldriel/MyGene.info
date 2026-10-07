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
 * Shareable links: the searched gene is kept in the `gene` query parameter, e.g.
 * `https://example.org/?gene=BRCA1`.
 */

export const GENE_PARAM = 'gene'

/**
 * Reads the gene of a link.
 *
 * @param {string} search Query string of the location, e.g. `?gene=BRCA1`.
 * @returns {string|null} The gene symbol as written, `null` when absent or blank.
 */
export function geneFromSearch(search) {
  const gene = new URLSearchParams(search).get(GENE_PARAM)?.trim()
  return gene || null
}

/**
 * Builds the link of a gene, keeping the other parts of the current address.
 *
 * @param {string} href Current address.
 * @param {string|null} symbol Gene symbol, or `null` for the welcome page.
 * @returns {string} The address.
 */
export function urlForGene(href, symbol) {
  const url = new URL(href)
  if (symbol) url.searchParams.set(GENE_PARAM, symbol)
  else url.searchParams.delete(GENE_PARAM)
  url.hash = ''
  return url.toString()
}
