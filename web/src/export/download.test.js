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
import { afterEach, describe, expect, it, vi } from 'vitest'
import { stubObjectUrls } from '../test/fixtures.js'
import { downloadBlob, exportFileName } from './download.js'

describe('exportFileName', () => {
  it('combines the symbol, "clinvar" and the local retrieval date', () => {
    const retrievedAt = new Date(2026, 0, 5, 23, 59).toISOString()

    expect(exportFileName({ gene: { symbol: 'HLA-A' }, retrievedAt }, 'pdf')).toBe(
      'HLA-A_clinvar_2026-01-05.pdf',
    )
  })
})

describe('downloadBlob', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('clicks a temporary link and revokes its URL later', () => {
    vi.useFakeTimers()
    const { createObjectURL, revokeObjectURL } = stubObjectUrls(vi)
    const clicked = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function click() {
      clicked.push({ href: this.href, download: this.download, attached: this.isConnected })
    })
    const blob = new Blob(['data'])

    downloadBlob(blob, 'file.csv')

    expect(createObjectURL).toHaveBeenCalledWith(blob)
    expect(clicked).toEqual([{ href: 'blob:fake', download: 'file.csv', attached: true }])
    expect(document.querySelector('a')).toBeNull()
    expect(revokeObjectURL).not.toHaveBeenCalled()
    vi.runAllTimers()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake')
  })
})
