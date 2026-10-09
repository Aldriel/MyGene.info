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
 * Journeys of a visitor on real data: the bugs that matter show up with real genes, whose
 * long variant descriptions and large tables small test fixtures do not have.
 */

import { expect, test } from './app.js'

/** Genes searched, with many variants and long clinical significances. */
const GENES = [
  { symbol: 'BRCA1', maxVariants: 1000 },
  { symbol: 'TP53', maxVariants: 250 },
  { symbol: 'CFTR', maxVariants: 100 },
]

for (const { symbol, maxVariants } of GENES) {
  test(`${symbol}: search, then export to CSV, JSON and PDF`, async ({ app }) => {
    await app.open()
    await app.search(symbol, maxVariants)
    await expect(app.page.getByRole('main')).toContainText(symbol)
    await expect(app.rows().first()).toBeVisible()

    const csv = await app.export('csv')
    const lines = csv.content.toString('utf8').trim().split(/\r?\n/)
    expect(lines[0]).toContain(app.t('export.gene'))
    expect(lines.length).toBeGreaterThan(1)
    expect(lines[1]).toContain(symbol)

    const json = JSON.parse((await app.export('json')).content.toString('utf8'))
    expect(json.result?.gene?.symbol ?? json.gene?.symbol).toBe(symbol)

    const pdf = await app.export('pdf')
    expect(pdf.name).toMatch(/\.pdf$/)
    expect(pdf.content.subarray(0, 5).toString('latin1')).toBe('%PDF-')
    expect(pdf.content.toString('latin1').trimEnd()).toMatch(/%%EOF$/)
  })
}

test('filtered and sorted variants export to PDF', async ({ app }) => {
  await app.open()
  await app.search('BRCA1', 500)

  const category = app.page.getByRole('combobox', { name: app.t('filter.category') })
  const options = await category.locator('option').allTextContents()
  await category.selectOption({ index: Math.min(1, options.length - 1) })
  await app.page.getByRole('columnheader', { name: app.t('table.significance') }).click()
  await expect(app.rows().first()).toBeVisible()

  const pdf = await app.export('pdf')
  expect(pdf.content.subarray(0, 5).toString('latin1')).toBe('%PDF-')
})

test('an exported result reopens', async ({ app }) => {
  await app.open()
  await app.search('TP53')
  const json = await app.export('json')

  await app.open()
  const chooser = app.page.waitForEvent('filechooser')
  await app.page.getByRole('button', { name: app.t('welcome.open') }).click()
  await (await chooser).setFiles(json.path)

  await app.waitForVariants()
  await expect(app.page.getByRole('main')).toContainText('TP53')
})

test('the summary tab draws the distribution', async ({ app }) => {
  await app.open()
  await app.search('CFTR')
  await app.page.getByRole('tab', { name: app.t('tab.summary') }).click()
  await expect(app.page.getByRole('heading', { name: app.t('summary.title') })).toBeVisible()
  await expect(app.page.getByRole('img', { name: app.t('summary.chart') })).toBeVisible()
})

test('a shared link opens its gene', async ({ app }) => {
  await app.open('./?gene=TP53')
  await app.waitForVariants()
  await expect(app.page.getByRole('main')).toContainText('TP53')
})

test('an unknown gene is reported clearly', async ({ app }) => {
  test.info().annotations.push({ type: 'expects-alert' })
  await app.open()
  const form = app.page.getByRole('search')
  await form.getByRole('searchbox').fill('NOTAGENE123')
  await form.getByRole('button', { name: app.t('search.button') }).click()
  await expect(
    app
      .alert()
      .or(app.page.getByText(/NOTAGENE123/))
      .first(),
  ).toContainText('NOTAGENE123')
})
