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
import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createTranslator } from '../i18n/translate.js'
import { RESULT, stubObjectUrls } from '../test/fixtures.js'
import { renderWithI18n } from '../test/render.jsx'
import ResultsView from './ResultsView.jsx'

const SHARE_URL = 'https://example.org/?gene=BRCA1'

function renderResults() {
  const onStatus = vi.fn()
  const onOpenFile = vi.fn()
  const view = renderWithI18n(
    <ResultsView
      result={RESULT}
      shareUrl={SHARE_URL}
      onOpenFile={onOpenFile}
      onStatus={onStatus}
    />,
  )
  /** Messages reported through `onStatus`, rendered in English. */
  const messages = () => {
    const en = createTranslator('en')
    return onStatus.mock.calls.map(([render, isError]) => [render(en), Boolean(isError)])
  }
  return { ...view, onStatus, onOpenFile, messages }
}

const visibleIds = () =>
  screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent)

/** Captures the blobs handed to the browser for download. */
function captureDownloads() {
  const { createObjectURL } = stubObjectUrls(vi)
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  return {
    click,
    blobs: () => createObjectURL.mock.calls.map(([blob]) => blob),
  }
}

describe('ResultsView', () => {
  it('shows the gene card and the variants tab', () => {
    renderResults()

    expect(screen.getByRole('heading', { name: 'BRCA1' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Variants' })).toHaveAttribute('aria-selected', 'true')
    expect(visibleIds()).toEqual(['10', '2', '33', '4', '—'])
  })

  it('filters and sorts the table', async () => {
    const { user } = renderResults()

    await user.click(screen.getByRole('button', { name: /Variant ID/ }))
    expect(visibleIds()).toEqual(['2', '4', '10', '33', '—'])

    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Clinical significance category' }),
      'pathogenic',
    )
    expect(visibleIds()).toEqual(['2', '4'])

    await user.type(screen.getByRole('searchbox', { name: 'Filter variants' }), 'somatic')
    expect(visibleIds()).toEqual(['2'])
  })

  it('switches to the summary tab', async () => {
    const { user } = renderResults()

    await user.click(screen.getByRole('tab', { name: 'Summary' }))

    expect(screen.getByRole('tab', { name: 'Summary' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Clinical significance distribution')
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('exports the visible rows as CSV', async () => {
    const downloads = captureDownloads()
    const { user, messages } = renderResults()

    await user.type(screen.getByRole('searchbox', { name: 'Filter variants' }), 'benign')
    await user.click(screen.getByRole('button', { name: /Export/ }))
    await user.click(screen.getByRole('menuitem', { name: /CSV/ }))

    await waitFor(() => expect(downloads.click).toHaveBeenCalledOnce())
    const csv = await downloads.blobs()[0].text()
    expect(csv.trim().split('\r\n')).toHaveLength(2)
    expect(csv).toContain('Benign')
    expect(messages()).toEqual([
      [expect.stringMatching(/^BRCA1_clinvar_.*\.csv downloaded\.$/), false],
    ])
  })

  it('announces the PDF preparation then the download', async () => {
    const downloads = captureDownloads()
    const { user, messages } = renderResults()

    await user.click(screen.getByRole('button', { name: /Export/ }))
    await user.click(screen.getByRole('menuitem', { name: /PDF/ }))

    await waitFor(() => expect(downloads.click).toHaveBeenCalledOnce())
    expect(downloads.blobs()[0].type).toBe('application/pdf')
    expect(messages()).toEqual([
      ['Preparing the PDF report…', false],
      [expect.stringMatching(/\.pdf downloaded\.$/), false],
    ])
  })

  it('reports a failed export', async () => {
    stubObjectUrls(vi).createObjectURL.mockImplementation(() => {
      throw new Error('blocked')
    })
    const { user, messages } = renderResults()

    await user.click(screen.getByRole('button', { name: /Export/ }))
    await user.click(screen.getByRole('menuitem', { name: /JSON/ }))

    await waitFor(() =>
      expect(messages()).toEqual([['The export failed. Please try again.', true]]),
    )
    expect(screen.getByRole('button', { name: /Export/ })).toBeEnabled()
  })

  it('copies the shareable link', async () => {
    const { user, messages } = renderResults()
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()

    await user.click(screen.getByRole('button', { name: 'Copy link' }))

    expect(writeText).toHaveBeenCalledWith(SHARE_URL)
    expect(messages()).toEqual([['Link copied to the clipboard.', false]])
  })

  it('reports a clipboard failure', async () => {
    const { user, messages } = renderResults()
    vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValue(new Error('denied'))

    await user.click(screen.getByRole('button', { name: 'Copy link' }))

    expect(messages()).toEqual([[expect.stringContaining('Unable to copy the link'), true]])
  })

  it('opens a results file', async () => {
    const { user, onOpenFile } = renderResults()

    await user.click(screen.getByRole('button', { name: 'Open results…' }))

    expect(onOpenFile).toHaveBeenCalledOnce()
  })
})
