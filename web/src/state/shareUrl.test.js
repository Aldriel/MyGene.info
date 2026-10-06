import { describe, expect, it } from 'vitest'
import { geneFromSearch, urlForGene } from './shareUrl.js'

describe('geneFromSearch', () => {
  it.each([
    ['?gene=BRCA1', 'BRCA1'],
    ['?gene=%20tp53%20&lang=fr', 'tp53'],
    ['?gene=', null],
    ['?other=1', null],
    ['', null],
  ])('reads %j as %j', (search, gene) => {
    expect(geneFromSearch(search)).toBe(gene)
  })
})

describe('urlForGene', () => {
  it('sets the gene and keeps the rest of the address', () => {
    expect(urlForGene('https://example.org/app/?x=1#top', 'HLA-A')).toBe(
      'https://example.org/app/?x=1&gene=HLA-A',
    )
  })

  it('replaces the current gene', () => {
    expect(urlForGene('https://example.org/?gene=TP53', 'BRCA1')).toBe(
      'https://example.org/?gene=BRCA1',
    )
  })

  it('removes the gene for the welcome page', () => {
    expect(urlForGene('https://example.org/?gene=TP53', null)).toBe('https://example.org/')
  })
})
