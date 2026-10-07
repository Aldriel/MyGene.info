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
 * Export of ClinVar variants as CSV (RFC 4180), readable by spreadsheet software. Same rules
 * as the desktop application.
 */

import { clinvarUrl } from '../model/variants.js'

/** Byte order mark that lets Excel detect UTF-8. */
export const UTF8_BOM = '\uFEFF'

const LINE_END = '\r\n'
const VALUE_SEPARATOR = ', '

/**
 * Returns the field separator spreadsheet software expects for a locale: `;` where the decimal
 * separator is a comma (e.g. French), `,` otherwise.
 *
 * @param {string} locale Locale tag such as `fr-CA`.
 * @returns {',' | ';'} The separator.
 */
export function separatorFor(locale) {
  const decimal = new Intl.NumberFormat(locale)
    .formatToParts(1.5)
    .find((part) => part.type === 'decimal')?.value
  return decimal === ',' ? ';' : ','
}

/**
 * Quotes a value when it contains the separator, a quote or a line break, and neutralizes
 * values that spreadsheet software would evaluate as formulas.
 *
 * @param {unknown} value Raw value.
 * @param {string} separator Field separator.
 * @returns {string} The value as written in the CSV document.
 */
export function escapeCsv(value, separator) {
  if (value == null || value === '') return ''
  const text = String(value)
  const safe = '=+-@'.includes(text[0]) ? `'${text}` : text
  const needsQuotes = [separator, '"', '\n', '\r'].some((c) => safe.includes(c))
  return needsQuotes ? `"${safe.replaceAll('"', '""')}"` : safe
}

/**
 * Builds the CSV document, without byte order mark.
 *
 * @param {string} geneSymbol Symbol of the gene, repeated on every row.
 * @param {object[]} variants Variants to export, in order.
 * @param {string[]} headers The six column labels: gene, ID, HGVS, significance, origin, URL.
 * @param {string} separator Field separator.
 * @returns {string} The CSV text.
 */
export function toCsv(geneSymbol, variants, headers, separator) {
  if (headers.length !== 6) throw new RangeError(`Expected 6 headers, got ${headers.length}`)
  const row = (values) => values.map((value) => escapeCsv(value, separator)).join(separator)
  const lines = [
    row(headers),
    ...variants.map((variant) =>
      row([
        geneSymbol,
        variant.variantId,
        variant.hgvs,
        variant.significances.join(VALUE_SEPARATOR),
        variant.origins.join(VALUE_SEPARATOR),
        clinvarUrl(variant),
      ]),
    ),
  ]
  return lines.map((line) => line + LINE_END).join('')
}

/**
 * Returns the localized column headers of the CSV export.
 *
 * @param {(key: string) => string} t Translator.
 * @returns {string[]} The six headers.
 */
export function csvHeaders(t) {
  return [
    t('export.gene'),
    t('table.variantId'),
    t('table.hgvs'),
    t('table.significance'),
    t('table.origin'),
    t('export.clinvarUrl'),
  ]
}
