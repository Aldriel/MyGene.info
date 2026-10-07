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

// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest'
import { createTranslator } from '../i18n/translate.js'
import { RESULT, stubObjectUrls } from '../test/fixtures.js'
import { buildExport, exportResult } from './exportResult.js'
import { parseResultJson } from './resultFile.js'

const en = createTranslator('en')
const fr = createTranslator('fr')
const rows = RESULT.clinvar.variants.slice(0, 2)

const textOf = (blob) => blob.text()

describe('buildExport', () => {
  it('exports the rows on screen as CSV with a byte order mark', async () => {
    const { blob, fileName } = await buildExport('csv', RESULT, rows, en)
    const bytes = new Uint8Array(await blob.arrayBuffer())
    const text = await textOf(blob)

    expect(fileName).toMatch(/^BRCA1_clinvar_\d{4}-\d{2}-\d{2}\.csv$/)
    expect(blob.type).toBe('text/csv;charset=utf-8')
    expect([...bytes.slice(0, 3)]).toEqual([0xef, 0xbb, 0xbf])
    expect(text.split('\r\n')).toHaveLength(rows.length + 2)
    expect(text).toContain('Gene,Variant ID,HGVS')
  })

  it('uses the separator and headers of the interface language', async () => {
    const text = await textOf((await buildExport('csv', RESULT, rows, fr)).blob)

    expect(text).toContain('Gène;ID du variant;HGVS')
  })

  it('exports the whole result as reopenable JSON', async () => {
    const { blob, fileName } = await buildExport('json', RESULT, rows, en)

    expect(fileName.endsWith('.json')).toBe(true)
    const reopened = parseResultJson(await textOf(blob), fileName)
    expect(reopened.clinvar.variants).toHaveLength(RESULT.clinvar.variants.length)
  })

  it('exports a PDF report', async () => {
    const { blob, fileName } = await buildExport('pdf', RESULT, rows, en, {
      paperLocale: 'fr-FR',
      now: new Date('2026-10-06T15:00:00Z'),
    })

    expect(fileName.endsWith('.pdf')).toBe(true)
    expect(blob.type).toBe('application/pdf')
    expect((await textOf(blob)).startsWith('%PDF-')).toBe(true)
  })

  it('rejects an unknown format', async () => {
    await expect(buildExport('xls', RESULT, rows, en)).rejects.toThrow(RangeError)
  })
})

describe('exportResult', () => {
  it('downloads the file and returns its name', async () => {
    const { createObjectURL } = stubObjectUrls(vi)
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    const fileName = await exportResult('json', RESULT, rows, en)

    expect(fileName).toMatch(/\.json$/)
    expect(createObjectURL).toHaveBeenCalledOnce()
    expect(click).toHaveBeenCalledOnce()
  })
})
