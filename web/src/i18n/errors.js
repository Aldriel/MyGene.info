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
 * Translated, user-friendly descriptions of the errors raised by the application.
 */

import { ApiError } from '../api/biothings.js'
import { InvalidSymbolError, MAX_SYMBOL_LENGTH } from '../api/geneSymbol.js'
import { ResultFileError } from '../export/resultFile.js'

/** Error raised when no gene matches a valid symbol. */
export class GeneNotFoundError extends Error {
  /** @param {string} symbol Normalized symbol searched. */
  constructor(symbol) {
    super(`No human gene found for symbol "${symbol}".`)
    this.name = 'GeneNotFoundError'
    this.symbol = symbol
  }
}

/**
 * Describes an API failure from its code and HTTP status.
 *
 * @param {ApiError} error Failed call.
 * @param {(key: string, params?: object) => string} t Translator.
 * @returns {string} Message suitable for display.
 */
function describeApiError(error, t) {
  const { host, status } = error
  switch (error.code) {
    case 'timeout':
      return t('error.api.timeout', { host })
    case 'unreachable':
      return t('error.api.unreachable', { host })
    case 'invalidResponse':
      return t('error.api.invalidResponse', { host })
    case 'http':
      if (status === 429) return t('error.api.tooManyRequests', { host })
      if (status >= 500) return t('error.api.unavailable', { host, status: String(status) })
      if (status === 400) return t('error.api.badRequest', { host })
      return t('error.api.unexpectedStatus', { host, status: String(status) })
    default:
      return error.message || t('error.unexpected')
  }
}

/**
 * Describes an error in the current language.
 *
 * @param {unknown} error Error to describe.
 * @param {(key: string, params?: object) => string} t Translator.
 * @returns {string} Message suitable for display.
 */
export function describeError(error, t) {
  if (error instanceof InvalidSymbolError) {
    switch (error.reason) {
      case 'empty':
        return t('error.symbol.empty')
      case 'tooLong':
        return t('error.symbol.tooLong', { max: MAX_SYMBOL_LENGTH })
      default:
        return t('error.symbol.malformed', { symbol: error.symbol })
    }
  }
  if (error instanceof GeneNotFoundError) return t('error.geneNotFound', { symbol: error.symbol })
  if (error instanceof ApiError) return describeApiError(error, t)
  if (error instanceof ResultFileError) {
    const key = {
      unreadable: 'error.file.unreadable',
      invalid: 'error.file.invalid',
      unsupported: 'error.file.unsupported',
    }[error.reason]
    return t(key, { file: error.fileName })
  }
  return error?.message || t('error.unexpected')
}
