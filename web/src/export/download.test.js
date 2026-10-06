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
