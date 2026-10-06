/**
 * Exports of a search result to the formats offered by the interface.
 */

import { APP_VERSION } from '../brand.js'
import { csvHeaders, separatorFor, toCsv, UTF8_BOM } from './csv.js'
import { downloadBlob, exportFileName } from './download.js'
import { toResultJson } from './resultFile.js'

export const EXPORT_FORMATS = ['csv', 'json', 'pdf']

/**
 * Builds the file of an export.
 *
 * The CSV and PDF files contain the rows on screen (filtered and sorted); the JSON file
 * contains the whole result so it can be reopened later. The PDF library is only downloaded
 * the first time a report is requested.
 *
 * @param {'csv'|'json'|'pdf'} format Export format.
 * @param {object} result Search result.
 * @param {object[]} rows Variants on screen, in display order.
 * @param {ReturnType<import('../i18n/translate.js').createTranslator>} translator Translator of
 *   the interface language.
 * @param {{paperLocale?: string, now?: Date}} [options] Locale choosing the paper size (the
 *   browser locale by default) and generation date.
 * @returns {Promise<{blob: Blob, fileName: string}>} The file.
 */
export async function buildExport(format, result, rows, translator, options = {}) {
  const fileName = exportFileName(result, format)
  switch (format) {
    case 'csv': {
      const csv = toCsv(
        result.gene.symbol,
        rows,
        csvHeaders(translator.t),
        separatorFor(translator.locale),
      )
      return { blob: new Blob([UTF8_BOM + csv], { type: 'text/csv;charset=utf-8' }), fileName }
    }
    case 'json':
      return {
        blob: new Blob([toResultJson(result, APP_VERSION)], { type: 'application/json' }),
        fileName,
      }
    case 'pdf': {
      const { buildPdfReport, paperFor } = await import('./pdfReport.js')
      const doc = buildPdfReport(result, rows, {
        translator,
        appVersion: APP_VERSION,
        paper: paperFor(options.paperLocale ?? globalThis.navigator?.language ?? translator.locale),
        generatedAt: options.now ?? new Date(),
      })
      return { blob: doc.output('blob'), fileName }
    }
    default:
      throw new RangeError(`Unsupported export format: ${format}`)
  }
}

/**
 * Builds an export and makes the browser download it.
 *
 * @param {'csv'|'json'|'pdf'} format Export format.
 * @param {object} result Search result.
 * @param {object[]} rows Variants on screen, in display order.
 * @param {object} translator Translator of the interface language.
 * @returns {Promise<string>} Name of the downloaded file.
 */
export async function exportResult(format, result, rows, translator) {
  const { blob, fileName } = await buildExport(format, result, rows, translator)
  downloadBlob(blob, fileName)
  return fileName
}
