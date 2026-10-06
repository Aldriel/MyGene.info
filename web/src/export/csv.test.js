import { describe, expect, it } from 'vitest'
import { createTranslator } from '../i18n/translate.js'
import { variant } from '../test/fixtures.js'
import { csvHeaders, escapeCsv, separatorFor, toCsv } from './csv.js'

describe('separatorFor', () => {
  it.each([
    ['en-US', ','],
    ['en-CA', ','],
    ['fr-CA', ';'],
    ['fr-FR', ';'],
    ['de-DE', ';'],
  ])('uses %j → %j', (locale, separator) => {
    expect(separatorFor(locale)).toBe(separator)
  })
})

describe('escapeCsv', () => {
  it.each([
    [null, ''],
    [undefined, ''],
    ['', ''],
    [42, '42'],
    ['plain', 'plain'],
    ['a,b', '"a,b"'],
    ['say "hi"', '"say ""hi"""'],
    ['two\nlines', '"two\nlines"'],
    ['=SUM(A1)', "'=SUM(A1)"],
    ['+1', "'+1"],
    ['-1', "'-1"],
    ['@cmd', "'@cmd"],
  ])('writes %j as %j', (value, expected) => {
    expect(escapeCsv(value, ',')).toBe(expected)
  })

  it('quotes values containing the semicolon separator only when it is used', () => {
    expect(escapeCsv('a;b', ';')).toBe('"a;b"')
    expect(escapeCsv('a;b', ',')).toBe('a;b')
  })
})

describe('toCsv', () => {
  const headers = ['Gene', 'ID', 'HGVS', 'Significance', 'Origin', 'URL']

  it('writes one CRLF-terminated line per variant', () => {
    const csv = toCsv(
      'BRCA1',
      [variant(5, ['Pathogenic', 'Likely pathogenic'], ['germline', 'somatic'], 'chr17:g.1A>G')],
      headers,
      ',',
    )

    expect(csv).toBe(
      'Gene,ID,HGVS,Significance,Origin,URL\r\n' +
        'BRCA1,5,chr17:g.1A>G,"Pathogenic, Likely pathogenic","germline, somatic",' +
        'https://www.ncbi.nlm.nih.gov/clinvar/variation/5/\r\n',
    )
  })

  it('leaves missing values empty', () => {
    const csv = toCsv('BRCA1', [variant(null, [], [], 'chr1:g.1del')], headers, ';')

    expect(csv.split('\r\n')[1]).toBe('BRCA1;;chr1:g.1del;;;')
  })

  it('requires six headers', () => {
    expect(() => toCsv('BRCA1', [], ['a'], ',')).toThrow(RangeError)
  })
})

describe('csvHeaders', () => {
  it('translates the headers', () => {
    expect(csvHeaders(createTranslator('fr').t)).toEqual([
      'Gène',
      'ID du variant',
      'HGVS',
      'Signification clinique',
      'Origine',
      'URL ClinVar',
    ])
  })
})
