import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/biothings.js'
import { InvalidSymbolError } from '../api/geneSymbol.js'
import { ResultFileError } from '../export/resultFile.js'
import { describeError, GeneNotFoundError } from './errors.js'
import { createTranslator } from './translate.js'

const en = createTranslator('en').t
const fr = createTranslator('fr').t

describe('describeError', () => {
  it.each([
    [new InvalidSymbolError('empty', ''), 'Please enter a gene symbol.'],
    [new InvalidSymbolError('tooLong', 'A'), 'Gene symbols are at most 32 characters long.'],
    [new InvalidSymbolError('malformed', 'B*'), '"B*" is not a valid gene symbol.'],
    [new GeneNotFoundError('XYZ'), 'No human gene found for symbol "XYZ".'],
    [new ApiError('x', { code: 'timeout', host: 'a.org' }), 'a.org did not respond in time.'],
    [new ApiError('x', { code: 'unreachable', host: 'a.org' }), 'Unable to reach a.org.'],
    [new ApiError('x', { code: 'invalidResponse', host: 'a.org' }), 'invalid response from a.org'],
    [new ApiError('x', { code: 'http', host: 'a.org', status: 429 }), 'Too many requests'],
    [new ApiError('x', { code: 'http', host: 'a.org', status: 503 }), '(HTTP 503)'],
    [new ApiError('x', { code: 'http', host: 'a.org', status: 400 }), 'rejected the request'],
    [new ApiError('x', { code: 'http', host: 'a.org', status: 418 }), 'Unexpected response'],
    [new ApiError('Raw message'), 'Raw message'],
    [new ResultFileError('unreadable', 'a.json'), 'Unable to read a.json.'],
    [new ResultFileError('invalid', 'a.json'), 'a.json is not a valid'],
    [new ResultFileError('unsupported', 'a.json'), 'newer version'],
    [new Error('Boom'), 'Boom'],
    [null, 'An unexpected error occurred.'],
  ])('describes %o in English', (error, expected) => {
    expect(describeError(error, en)).toContain(expected)
  })

  it('describes errors in French', () => {
    expect(describeError(new InvalidSymbolError('empty', ''), fr)).toBe(
      'Veuillez saisir un symbole de gène.',
    )
    expect(describeError(new ApiError('x', { code: 'http', host: 'a.org', status: 500 }), fr)).toBe(
      'a.org est temporairement indisponible (HTTP 500). Veuillez réessayer plus tard.',
    )
  })

  it('keeps the HTTP status unformatted', () => {
    const error = new ApiError('x', { code: 'http', host: 'a.org', status: 1000 })

    expect(describeError(error, en)).toContain('HTTP 1000')
  })
})

describe('GeneNotFoundError', () => {
  it('keeps the searched symbol', () => {
    expect(new GeneNotFoundError('XYZ')).toMatchObject({
      name: 'GeneNotFoundError',
      symbol: 'XYZ',
      message: 'No human gene found for symbol "XYZ".',
    })
  })
})
