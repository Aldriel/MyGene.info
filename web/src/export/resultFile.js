/**
 * Self-describing JSON document of a search result, identical to the one of the desktop
 * application: a file exported by either client can be opened by the other.
 */

import { clinvarUrl } from '../model/variants.js'

export const FORMAT = 'mygene-explorer/search-result'
export const FORMAT_VERSION = 1

/** Error raised when a file is not a result file this version can open. */
export class ResultFileError extends Error {
  /**
   * @param {'unreadable'|'invalid'|'unsupported'} reason Why the file cannot be opened.
   * @param {string} fileName Name of the file.
   * @param {unknown} [cause] Underlying error.
   */
  constructor(reason, fileName, cause) {
    super(`${fileName} cannot be opened (${reason}).`, { cause })
    this.name = 'ResultFileError'
    this.reason = reason
    this.fileName = fileName
  }
}

/**
 * Serializes a search result.
 *
 * @param {{query: string, gene: object, clinvar: {total: number, variants: object[]},
 *   retrievedAt: string}} result Search result.
 * @param {string} appVersion Version of the application, recorded in the document.
 * @returns {string} Indented JSON document.
 */
export function toResultJson(result, appVersion) {
  const { gene } = result
  return JSON.stringify(
    {
      format: FORMAT,
      formatVersion: FORMAT_VERSION,
      generator: { application: 'MyGene Explorer', version: appVersion },
      sources: {
        gene: 'https://mygene.info',
        variants: 'https://myvariant.info',
        clinicalData: 'https://www.ncbi.nlm.nih.gov/clinvar/',
      },
      query: result.query,
      retrievedAt: result.retrievedAt,
      gene: {
        id: gene.id,
        symbol: gene.symbol,
        name: gene.name,
        entrezGene: gene.entrezGene,
        typeOfGene: gene.typeOfGene,
        mapLocation: gene.mapLocation,
        summary: gene.summary,
        aliases: gene.aliases,
      },
      clinvar: {
        total: result.clinvar.total,
        variants: result.clinvar.variants.map((variant) => ({
          variantId: variant.variantId,
          hgvs: variant.hgvs,
          clinicalSignificances: variant.significances,
          origins: variant.origins,
          clinvarUrl: clinvarUrl(variant),
        })),
      },
    },
    null,
    2,
  )
}

/** Reads a text field, `null` when missing. */
const text = (value) => (value == null ? null : String(value))

/** Reads an array of texts, skipping null entries. */
const texts = (value) => (Array.isArray(value) ? value.filter((v) => v != null).map(String) : [])

/** Reads an integer field, `null` when missing. */
const integer = (value) => (value == null || Number.isNaN(Number(value)) ? null : Number(value))

/**
 * Parses a result document.
 *
 * @param {string} json Document content.
 * @param {string} fileName Name used in error messages.
 * @returns {object} The search result.
 * @throws {ResultFileError} If the document is not a valid result file.
 */
export function parseResultJson(json, fileName) {
  let root
  try {
    root = JSON.parse(json)
  } catch (err) {
    throw new ResultFileError('invalid', fileName, err)
  }
  if (root?.format !== FORMAT) throw new ResultFileError('invalid', fileName)
  if (Number(root.formatVersion) > FORMAT_VERSION) {
    throw new ResultFileError('unsupported', fileName)
  }

  const gene = root.gene ?? {}
  const retrievedAt = text(root.retrievedAt)
  if (!gene.symbol || root.query == null || !retrievedAt || Number.isNaN(Date.parse(retrievedAt))) {
    throw new ResultFileError('invalid', fileName)
  }

  const variants = (Array.isArray(root.clinvar?.variants) ? root.clinvar.variants : []).map(
    (variant, index) => ({
      key: `${variant.hgvs ?? ''}-${variant.variantId ?? ''}-${index}`,
      hgvs: text(variant.hgvs),
      variantId: integer(variant.variantId),
      significances: texts(variant.clinicalSignificances),
      origins: texts(variant.origins),
    }),
  )

  return {
    query: String(root.query),
    retrievedAt,
    gene: {
      id: text(gene.id),
      symbol: String(gene.symbol),
      name: text(gene.name),
      entrezGene: integer(gene.entrezGene),
      summary: text(gene.summary),
      typeOfGene: text(gene.typeOfGene),
      mapLocation: text(gene.mapLocation),
      aliases: texts(gene.aliases),
    },
    clinvar: { total: integer(root.clinvar?.total) ?? variants.length, variants },
  }
}
