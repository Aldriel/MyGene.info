/**
 * Validation of gene symbols entered by the user.
 *
 * Official symbols (HGNC) only contain letters, digits, hyphens, dots and
 * underscores (e.g. BRCA1, HLA-A, C9orf72). Rejecting anything else before
 * calling the API prevents query-syntax injection (wildcards, field
 * prefixes, parentheses) that would yield misleading results or HTTP 400s.
 */

export const MAX_SYMBOL_LENGTH = 32

const SYMBOL_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]*$/

/** English default messages, by reason. The interface translates them from the reason. */
const DEFAULT_MESSAGES = {
  empty: () => 'Please enter a gene symbol.',
  tooLong: () => `Gene symbols are at most ${MAX_SYMBOL_LENGTH} characters long.`,
  malformed: (symbol) =>
    `"${symbol}" is not a valid gene symbol. Use letters, digits, hyphens, dots or ` +
    'underscores only (e.g. BRCA1, HLA-A, C9orf72).',
}

/** Error raised when the user input is not a valid gene symbol. */
export class InvalidSymbolError extends Error {
  /**
   * @param {'empty'|'tooLong'|'malformed'} reason Why the symbol was rejected.
   * @param {string} symbol Trimmed user input.
   */
  constructor(reason, symbol) {
    super(DEFAULT_MESSAGES[reason](symbol))
    this.name = 'InvalidSymbolError'
    this.reason = reason
    this.symbol = symbol
  }
}

/**
 * Validates a gene symbol and returns it trimmed and upper-cased.
 *
 * @param {string} input Raw user input.
 * @returns {string} The normalized symbol.
 * @throws {InvalidSymbolError} If the input is empty or malformed.
 */
export function normalizeGeneSymbol(input) {
  const symbol = (input ?? '').trim()

  if (!symbol) throw new InvalidSymbolError('empty', symbol)
  if (symbol.length > MAX_SYMBOL_LENGTH) throw new InvalidSymbolError('tooLong', symbol)
  if (!SYMBOL_PATTERN.test(symbol)) throw new InvalidSymbolError('malformed', symbol)

  return symbol.toUpperCase()
}
