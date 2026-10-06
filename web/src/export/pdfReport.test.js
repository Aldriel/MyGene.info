import { inflateSync } from 'node:zlib'
import { describe, expect, it } from 'vitest'
import { createTranslator } from '../i18n/translate.js'
import { RESULT, variant } from '../test/fixtures.js'
import { buildPdfReport, paperFor, pdfText } from './pdfReport.js'

const GENERATED_AT = new Date('2026-10-06T15:00:00Z')

function build(
  language,
  { rows = RESULT.clinvar.variants, paper = 'letter', result = RESULT } = {},
) {
  return buildPdfReport(result, rows, {
    translator: createTranslator(language),
    appVersion: '1.0.0',
    paper,
    generatedAt: GENERATED_AT,
  })
}

/** PDF content with its compressed page streams inflated, so drawn texts are readable. */
function raw(doc) {
  return doc
    .output()
    .replace(/stream\r?\n([\s\S]*?)\r?\nendstream/g, (_match, data) =>
      inflateSync(Buffer.from(data, 'latin1')).toString('latin1'),
    )
}

describe('pdfText', () => {
  it.each([
    [null, ''],
    [undefined, ''],
    [42, '42'],
    ['Pathogène — “quoted” ‘x’ …', 'Pathogène - "quoted" \'x\' ...'],
    ['TNF-α and β-catenin', 'TNF-alpha and beta-catenin'],
    ['line\nbreak\u00a0nbsp', 'line break nbsp'],
    ['emoji 🧬 日本', 'emoji ? ??'],
  ])('cleans %j', (value, expected) => {
    expect(pdfText(value)).toBe(expected)
  })
})

describe('paperFor', () => {
  it.each([
    ['en-US', 'letter'],
    ['fr-CA', 'letter'],
    ['en', 'letter'],
    ['fr', 'a4'],
    ['fr-FR', 'a4'],
    ['de-DE', 'a4'],
    ['not a locale!', 'a4'],
  ])('uses %j paper for %j', (locale, paper) => {
    expect(paperFor(locale)).toBe(paper)
  })
})

describe('buildPdfReport', () => {
  it('writes the gene, the distribution and the variants', () => {
    const content = raw(build('en'))

    for (const text of [
      'BRCA1',
      'BRCA1 DNA repair associated',
      'protein-coding',
      '17q21.31',
      'BRCAI, BRCC1, RNF53',
      'Clinical significance distribution',
      'ClinVar variants',
      'chr17:g.43000002G>A',
      'Uncertain significance',
    ]) {
      expect(content).toContain(text)
    }
  })

  it('signs every page with clickable website and email links', () => {
    const rows = Array.from({ length: 120 }, (_, i) => variant(i + 1, ['Benign']))
    const doc = build('en', { rows })
    const pages = doc.getNumberOfPages()
    const content = raw(doc)

    expect(pages).toBeGreaterThan(2)
    expect(content.split('(Maxime Ethier - Biocomputing Consultant) Tj').length - 1).toBe(pages)
    expect(content).toContain(`Page ${pages} of ${pages}`)
    expect(content.split('/URI (https://www.maximeethier.com/en)').length - 1).toBe(pages)
    expect(content.split('/URI (mailto:contact@maximeethier.com)').length - 1).toBe(pages)
  })

  it('is translated', () => {
    const content = raw(build('fr'))

    expect(content).toContain('Maxime Ethier - Consultant en Bio-informatique')
    expect(content).toContain('/URI (https://www.maximeethier.com)')
    expect(content).toContain('Page 1 sur 1')
  })

  it('records the metadata', () => {
    const content = raw(build('en'))

    expect(content).toContain('/Title (BRCA1 - ClinVar variant report)')
    expect(content).toContain('/Author (Maxime Ethier - Biocomputing Consultant)')
  })

  it('uses the requested paper size', () => {
    expect(build('en', { paper: 'letter' }).internal.pageSize.getWidth()).toBeCloseTo(612, 0)
    expect(build('en', { paper: 'a4' }).internal.pageSize.getWidth()).toBeCloseTo(595.28, 1)
  })

  it('handles a gene without details or variants', () => {
    const result = {
      ...RESULT,
      gene: { ...RESULT.gene, name: null, summary: null, aliases: [], typeOfGene: null },
      clinvar: { total: 0, variants: [] },
    }

    const doc = build('en', { result, rows: [] })

    expect(doc.getNumberOfPages()).toBe(1)
    expect(raw(doc)).toContain('No variant to summarize.')
  })
})
