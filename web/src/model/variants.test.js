import { describe, expect, it } from 'vitest'
import { variant, VARIANTS } from '../test/fixtures.js'
import { clinvarUrl, matchesFilter, sortVariants } from './variants.js'

describe('clinvarUrl', () => {
  it('links to the ClinVar variation page', () => {
    expect(clinvarUrl(variant(55001, []))).toBe(
      'https://www.ncbi.nlm.nih.gov/clinvar/variation/55001/',
    )
  })

  it('returns null without identifier', () => {
    expect(clinvarUrl(variant(null, []))).toBeNull()
  })
})

describe('matchesFilter', () => {
  const v = variant(55001, ['Likely pathogenic'], ['germline', 'de novo'], 'chr17:g.43045712G>A')

  it.each(['', '   ', '55001', '4304571', 'LIKELY', 'de novo', 'G>A'])(
    'keeps the variant for the text %j',
    (text) => {
      expect(matchesFilter(v, { text })).toBe(true)
    },
  )

  it('rejects a variant that does not contain the text', () => {
    expect(matchesFilter(v, { text: 'benign' })).toBe(false)
  })

  it('filters by any category of the variant', () => {
    const mixed = variant(1, ['Benign', 'Pathogenic'])

    expect(matchesFilter(mixed, { category: 'benign' })).toBe(true)
    expect(matchesFilter(mixed, { category: 'pathogenic' })).toBe(true)
    expect(matchesFilter(mixed, { category: 'uncertain' })).toBe(false)
  })

  it('combines text and category', () => {
    expect(matchesFilter(v, { text: 'germline', category: 'likely-pathogenic' })).toBe(true)
    expect(matchesFilter(v, { text: 'germline', category: 'benign' })).toBe(false)
  })

  it('keeps everything without filter', () => {
    expect(matchesFilter(v)).toBe(true)
  })

  it('tolerates a variant without identifier or HGVS', () => {
    const bare = { ...variant(null, []), hgvs: null }

    expect(matchesFilter(bare, { text: 'x' })).toBe(false)
    expect(matchesFilter(bare, { category: 'other' })).toBe(true)
  })
})

describe('sortVariants', () => {
  const ids = (variants) => variants.map((v) => v.variantId)

  it('keeps the API order without sort, in a copy', () => {
    const sorted = sortVariants(VARIANTS, null)

    expect(sorted).toEqual(VARIANTS)
    expect(sorted).not.toBe(VARIANTS)
  })

  it('sorts identifiers numerically, missing identifiers last', () => {
    expect(ids(sortVariants(VARIANTS, { column: 'variantId', direction: 'asc' }))).toEqual([
      2,
      4,
      10,
      33,
      null,
    ])
    expect(ids(sortVariants(VARIANTS, { column: 'variantId', direction: 'desc' }))).toEqual([
      null,
      33,
      10,
      4,
      2,
    ])
  })

  it('sorts by severity, the most severe first', () => {
    expect(ids(sortVariants(VARIANTS, { column: 'significance', direction: 'asc' }))).toEqual([
      4,
      2,
      33,
      10,
      null,
    ])
  })

  it('uses natural order for HGVS notations', () => {
    const variants = [
      variant(1, [], [], 'chr10:g.5A>G'),
      variant(2, [], [], 'chr2:g.5A>G'),
      variant(3, [], [], 'chr2:g.40A>G'),
    ]

    expect(ids(sortVariants(variants, { column: 'hgvs', direction: 'asc' }))).toEqual([2, 3, 1])
  })

  it('sorts by origin, ties ordered by significance text', () => {
    expect(ids(sortVariants(VARIANTS, { column: 'origin', direction: 'asc' }))).toEqual([
      null,
      10,
      4,
      33,
      2,
    ])
  })

  it('breaks ties with the significance text', () => {
    const variants = [variant(1, ['Pathogenic']), variant(1, ['Benign'])]

    expect(
      sortVariants(variants, { column: 'variantId', direction: 'asc' }).map(
        (v) => v.significances[0],
      ),
    ).toEqual(['Benign', 'Pathogenic'])
  })
})
