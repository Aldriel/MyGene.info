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
