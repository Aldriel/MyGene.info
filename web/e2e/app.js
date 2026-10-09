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
 * Page object of MyGene Explorer for the functional tests: what a visitor does, written with
 * the labels of the interface in its language.
 */

import { readFile } from 'node:fs/promises'
import { test as base, expect } from '@playwright/test'
import { MESSAGES } from '../src/i18n/messages.js'

/** Interface of the application in one language. */
export class App {
  /**
   * @param {import('@playwright/test').Page} page Browser tab.
   * @param {'en'|'fr'} language Interface language.
   */
  constructor(page, language) {
    this.page = page
    this.language = language
  }

  /** Text of a message, without its {placeholders}. */
  t(key) {
    return MESSAGES[this.language][key]
  }

  /** Message with its {placeholders} replaced. */
  format(key, values) {
    return this.t(key).replace(/\{(\w+)\}/g, (_match, name) => String(values[name]))
  }

  async open(path = './') {
    await this.page.goto(path)
    await expect(this.page.getByRole('searchbox', { name: this.t('search.label') })).toBeVisible()
  }

  /** Searches a gene with the search form and waits for its variants. */
  async search(symbol, maxVariants = 100) {
    const form = this.page.getByRole('search')
    await form.getByRole('searchbox').fill(symbol)
    await form.getByRole('combobox').selectOption(String(maxVariants))
    await form.getByRole('button', { name: this.t('search.button') }).click()
    await this.waitForVariants()
  }

  /** Waits until the variants table of a result is shown. */
  async waitForVariants() {
    await expect(this.page.getByRole('tab', { name: this.t('tab.variants') })).toBeVisible()
    await expect(this.page.getByRole('table')).toBeVisible()
  }

  /** Visible variant rows of the table. */
  rows() {
    return this.page.getByRole('table').locator('tbody tr')
  }

  /**
   * Exports the result and returns the downloaded file.
   *
   * @param {'csv'|'json'|'pdf'} format Export format.
   * @returns {Promise<{name: string, path: string, content: Buffer}>} The file.
   */
  async export(format) {
    const key = { csv: 'actions.exportCsv', json: 'actions.exportJson', pdf: 'actions.exportPdf' }
    await this.page.getByRole('button', { name: this.t('actions.export'), exact: true }).click()
    const download = this.page.waitForEvent('download')
    await this.page.getByRole('menuitem', { name: this.t(key[format]) }).click()
    // Fails at once with the message shown, rather than waiting for a download that never comes.
    const failure = this.alert()
      .waitFor()
      .then(async () => {
        throw new Error(`${format} export failed: ${await this.alert().textContent()}`)
      })
    const file = await Promise.race([download, failure])
    failure.catch(() => {})
    const path = test.info().outputPath(file.suggestedFilename())
    await file.saveAs(path)
    await expect(this.page.getByRole('status')).toContainText(
      this.format('status.downloaded', { file: file.suggestedFilename() }),
    )
    return { name: file.suggestedFilename(), path, content: await readFile(path) }
  }

  /** Error message shown to the visitor, if any. */
  alert() {
    return this.page.getByRole('alert')
  }
}

/**
 * Test whose page fails it on any JavaScript error or error message shown, even when the
 * scenario itself does not look for one.
 */
export const test = base.extend({
  app: async ({ page }, use, testInfo) => {
    const language = testInfo.project.use.locale?.startsWith('fr') ? 'fr' : 'en'
    const errors = []
    page.on('pageerror', (error) => errors.push(`JavaScript error: ${error.message}`))
    page.on('console', (message) => {
      if (message.type() === 'error') errors.push(`Console error: ${message.text()}`)
    })
    const app = new App(page, language)
    await use(app)
    if (!testInfo.annotations.some((a) => a.type === 'expects-alert')) {
      const alerts = await app.alert().allTextContents()
      errors.push(...alerts.filter((text) => text.trim()).map((text) => `Alert: ${text}`))
    }
    expect(errors, 'errors seen by the visitor').toEqual([])
  },
})

export { expect }
