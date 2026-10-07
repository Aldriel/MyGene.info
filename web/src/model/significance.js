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
 * Clinical significance categories of ClinVar variants, ordered from the most to the least
 * severe. Colors match the desktop application and the PDF report.
 */
export const CATEGORIES = [
  { id: 'pathogenic', color: '#dc2626' },
  { id: 'likely-pathogenic', color: '#f97316' },
  { id: 'conflicting', color: '#9333ea' },
  { id: 'uncertain', color: '#f59e0b' },
  { id: 'likely-benign', color: '#34d399' },
  { id: 'benign', color: '#059669' },
  { id: 'other', color: '#94a3b8' },
]

const SEVERITY = new Map(CATEGORIES.map((category, index) => [category.id, index]))

/** Order matters: "likely pathogenic" must be tested before "pathogenic". */
const KEYWORDS = [
  ['conflicting', 'conflicting'],
  ['likely pathogenic', 'likely-pathogenic'],
  ['pathogenic', 'pathogenic'],
  ['likely benign', 'likely-benign'],
  ['benign', 'benign'],
  ['uncertain', 'uncertain'],
]

/**
 * Classifies a ClinVar clinical significance, e.g. "Likely pathogenic".
 *
 * @param {string} significance Text reported by ClinVar.
 * @returns {string} Category identifier, `other` when unrecognized.
 */
export function classify(significance) {
  const lower = (significance ?? '').toLowerCase()
  const match = KEYWORDS.find(([keyword]) => lower.includes(keyword))
  return match ? match[1] : 'other'
}

/**
 * Returns every category a variant belongs to.
 *
 * @param {{significances: string[]}} variant ClinVar variant.
 * @returns {Set<string>} Category identifiers; `other` if the variant has no significance.
 */
export function categoriesOf(variant) {
  if (variant.significances.length === 0) return new Set(['other'])
  return new Set(variant.significances.map(classify))
}

/**
 * Returns the most severe category of a variant, used for sorting and statistics.
 *
 * @param {{significances: string[]}} variant ClinVar variant.
 * @returns {string} Category identifier.
 */
export function primaryOf(variant) {
  return [...categoriesOf(variant)].sort((a, b) => SEVERITY.get(a) - SEVERITY.get(b))[0]
}

/**
 * Returns the severity rank of a category, 0 being the most severe.
 *
 * @param {string} categoryId Category identifier.
 * @returns {number} Rank.
 */
export function severityOf(categoryId) {
  return SEVERITY.get(categoryId)
}

/**
 * Counts variants by most severe category.
 *
 * @param {object[]} variants ClinVar variants.
 * @returns {{id: string, color: string, count: number}[]} Every category in severity order,
 *   including those with no variant.
 */
export function countByPrimary(variants) {
  const counts = new Map(CATEGORIES.map((category) => [category.id, 0]))
  for (const variant of variants) {
    const primary = primaryOf(variant)
    counts.set(primary, counts.get(primary) + 1)
  }
  return CATEGORIES.map((category) => ({ ...category, count: counts.get(category.id) }))
}

/**
 * Returns the color of a category.
 *
 * @param {string} categoryId Category identifier.
 * @returns {string} CSS hexadecimal color.
 */
export function colorOf(categoryId) {
  return CATEGORIES[SEVERITY.get(categoryId)].color
}
