/**
 * Filtering and sorting of the variant table.
 */

import { categoriesOf, primaryOf, severityOf } from './significance.js'

const CLINVAR_VARIATION_URL = 'https://www.ncbi.nlm.nih.gov/clinvar/variation'

/**
 * Returns the ClinVar page of a variant.
 *
 * @param {{variantId?: number|null}} variant ClinVar variant.
 * @returns {string|null} URL, or `null` without identifier.
 */
export function clinvarUrl(variant) {
  return variant.variantId == null ? null : `${CLINVAR_VARIATION_URL}/${variant.variantId}/`
}

/**
 * Tests whether a variant passes the filter.
 *
 * @param {object} variant ClinVar variant.
 * @param {{text?: string, category?: string}} filter Case-insensitive text searched in the
 *   identifier, HGVS notation, significances and origins; category the variant must belong to.
 * @returns {boolean} Whether the variant is kept.
 */
export function matchesFilter(variant, { text = '', category = '' } = {}) {
  if (category && !categoriesOf(variant).has(category)) return false
  const needle = text.trim().toLowerCase()
  if (!needle) return true
  return [
    String(variant.variantId ?? ''),
    variant.hgvs,
    ...variant.significances,
    ...variant.origins,
  ]
    .filter(Boolean)
    .some((value) => value.toLowerCase().includes(needle))
}

/** Value compared for each sortable column. */
const SORT_KEYS = {
  variantId: (variant) => variant.variantId ?? Number.MAX_SAFE_INTEGER,
  hgvs: (variant) => variant.hgvs ?? '',
  significance: (variant) => severityOf(primaryOf(variant)),
  origin: (variant) => variant.origins.join(', '),
}

/**
 * Compares two values: numbers numerically, text with natural ordering ("chr2" before
 * "chr10").
 */
function compareValues(a, b) {
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' })
}

/**
 * Returns a sorted copy of the variants.
 *
 * @param {object[]} variants ClinVar variants.
 * @param {{column: string, direction: 'asc'|'desc'}|null} sort Column and direction, or `null`
 *   to keep the API order.
 * @returns {object[]} Sorted variants. Ties keep the significance text order so the result is
 *   stable.
 */
export function sortVariants(variants, sort) {
  if (!sort) return [...variants]
  const key = SORT_KEYS[sort.column]
  const factor = sort.direction === 'desc' ? -1 : 1
  return [...variants].sort((a, b) => {
    const result = compareValues(key(a), key(b))
    if (result !== 0) return result * factor
    return compareValues(a.significances.join(', '), b.significances.join(', '))
  })
}
