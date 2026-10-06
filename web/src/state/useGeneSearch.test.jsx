// @vitest-environment jsdom
import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { GeneNotFoundError } from '../i18n/errors.js'
import { fakeBiothings, RESULT } from '../test/fixtures.js'
import { useGeneSearch } from './useGeneSearch.js'

describe('useGeneSearch', () => {
  it('starts idle and returns the result of a search', async () => {
    fakeBiothings(vi)
    const { result } = renderHook(() => useGeneSearch())
    expect(result.current.status).toBe('idle')

    let found
    await act(async () => {
      found = await result.current.search('brca1', 250)
    })

    expect(result.current.status).toBe('success')
    expect(result.current.result).toBe(found)
    expect(found).toMatchObject({ query: 'BRCA1', gene: { symbol: 'BRCA1' } })
    expect(Date.parse(found.retrievedAt)).not.toBeNaN()
  })

  it('reports an unknown gene', async () => {
    fakeBiothings(vi)
    const { result } = renderHook(() => useGeneSearch())

    await act(() => result.current.search('NOPE1', 100))

    expect(result.current.status).toBe('error')
    expect(result.current.error).toBeInstanceOf(GeneNotFoundError)
  })

  it('ignores a slow search superseded by a newer one', async () => {
    const pending = []
    fakeBiothings(vi, (url, { signal }) => {
      if (url.host !== 'mygene.info') return { body: { total: 0, hits: [] } }
      const symbol = url.searchParams.get('q').replace('symbol:', '')
      return new Promise((resolve, reject) => {
        signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
        pending.push({ symbol, resolve })
      })
    })
    const { result } = renderHook(() => useGeneSearch())

    let first
    let second
    await act(async () => {
      first = result.current.search('BRCA1', 100)
      second = result.current.search('TP53', 100)
    })
    await act(async () => {
      pending
        .find((p) => p.symbol === 'TP53')
        .resolve({ body: { hits: [{ _id: '7157', symbol: 'TP53' }] } })
      await second
    })

    expect(await first).toBeNull()
    expect(result.current.result.gene.symbol).toBe('TP53')
  })

  it('shows an opened result and resets', () => {
    const { result } = renderHook(() => useGeneSearch())

    act(() => result.current.showResult(RESULT))
    expect(result.current).toMatchObject({ status: 'success', result: RESULT })

    act(() => result.current.reset())
    expect(result.current).toMatchObject({ status: 'idle', result: null, error: null })
  })
})
