import { describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  fetchClinvarVariants,
  fetchGene,
  httpErrorMessage,
  MAX_VARIANTS_CHOICES,
  REQUEST_TIMEOUT_MS,
} from './biothings.js'
import { InvalidSymbolError } from './geneSymbol.js'

function mockFetch(body, { ok = true, status = 200 } = {}) {
  const fetchMock = vi.fn().mockResolvedValue({ ok, status, json: async () => body })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function requestedUrl(fetchMock) {
  return new URL(fetchMock.mock.calls[0][0])
}

describe('fetchGene', () => {
  it('returns the first gene found, normalized', async () => {
    const fetchMock = mockFetch({
      total: 1,
      hits: [
        {
          _id: '672',
          symbol: 'BRCA1',
          name: 'BRCA1 DNA repair associated',
          entrezgene: 672,
          summary: 'Tumor suppressor.',
          type_of_gene: 'protein-coding',
          map_location: '17q21.31',
          alias: ['BRCAI', 'BRCC1', 'BRCAI', ''],
        },
      ],
    })

    const gene = await fetchGene('BRCA1')

    expect(gene).toEqual({
      id: '672',
      symbol: 'BRCA1',
      name: 'BRCA1 DNA repair associated',
      entrezGene: 672,
      summary: 'Tumor suppressor.',
      typeOfGene: 'protein-coding',
      mapLocation: '17q21.31',
      aliases: ['BRCAI', 'BRCC1'],
    })
    const url = requestedUrl(fetchMock)
    expect(url.host).toBe('mygene.info')
    expect(url.searchParams.get('q')).toBe('symbol:BRCA1')
    expect(url.searchParams.get('species')).toBe('human')
    expect(url.searchParams.get('fields')).toContain('map_location')
  })

  it('fills missing optional fields with null and accepts a single alias', async () => {
    mockFetch({ hits: [{ _id: 7157, symbol: 'TP53', alias: 'P53' }] })

    expect(await fetchGene('TP53')).toEqual({
      id: '7157',
      symbol: 'TP53',
      name: null,
      entrezGene: null,
      summary: null,
      typeOfGene: null,
      mapLocation: null,
      aliases: ['P53'],
    })
  })

  it('normalizes the symbol before querying', async () => {
    const fetchMock = mockFetch({ total: 0, hits: [] })

    await fetchGene('  brca1 ')

    expect(requestedUrl(fetchMock).searchParams.get('q')).toBe('symbol:BRCA1')
  })

  it('returns null when no gene matches', async () => {
    mockFetch({ total: 0, hits: [] })

    expect(await fetchGene('ZZZ999')).toBeNull()
  })

  it('rejects an invalid symbol without calling the API', async () => {
    const fetchMock = mockFetch({})

    await expect(fetchGene('B*')).rejects.toThrow(InvalidSymbolError)
    expect(fetchMock).not.toHaveBeenCalled()
  })
})

describe('error handling', () => {
  it.each([
    [400, 'mygene.info rejected the request (HTTP 400). Please check the gene symbol.'],
    [404, 'Unexpected response from mygene.info (HTTP 404).'],
    [429, 'Too many requests sent to mygene.info. Please wait a moment and try again.'],
    [500, 'mygene.info is temporarily unavailable (HTTP 500). Please try again later.'],
    [503, 'mygene.info is temporarily unavailable (HTTP 503). Please try again later.'],
  ])('explains HTTP %i', async (status, message) => {
    mockFetch({}, { ok: false, status })

    const error = await fetchGene('BRCA1').catch((err) => err)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ code: 'http', host: 'mygene.info', status, message })
  })

  it('reports a network failure', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await fetchGene('BRCA1').catch((err) => err)

    expect(error).toMatchObject({
      code: 'unreachable',
      host: 'mygene.info',
      message: 'Unable to reach mygene.info. Please check your internet connection.',
    })
    expect(error.cause).toBeInstanceOf(TypeError)
  })

  it('reports a timeout', async () => {
    const timeout = new AbortController()
    const timeoutSpy = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(timeout.signal)
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_url, { signal }) =>
          new Promise((_resolve, reject) => {
            signal.addEventListener('abort', () => reject(signal.reason))
          }),
      ),
    )

    const request = fetchGene('BRCA1').catch((err) => err)
    timeout.abort(new DOMException('Timed out', 'TimeoutError'))
    const error = await request

    expect(timeoutSpy).toHaveBeenCalledWith(REQUEST_TIMEOUT_MS)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.code).toBe('timeout')
    expect(error.message).toBe('mygene.info did not respond in time. Please try again.')
  })

  it('reports an invalid JSON body', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => {
          throw new SyntaxError('Unexpected token <')
        },
      }),
    )

    await expect(fetchGene('BRCA1')).rejects.toThrow(
      'Received an invalid response from mygene.info.',
    )
  })

  it('propagates user cancellation as an AbortError', async () => {
    const controller = new AbortController()
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_url, { signal }) =>
          new Promise((_resolve, reject) => {
            signal.addEventListener('abort', () =>
              reject(new DOMException('Aborted', 'AbortError')),
            )
          }),
      ),
    )

    const request = fetchGene('BRCA1', controller.signal)
    controller.abort()

    await expect(request).rejects.toMatchObject({ name: 'AbortError' })
  })

  it('uses a generic message for unexpected statuses', () => {
    expect(httpErrorMessage(418, 'example.org')).toBe(
      'Unexpected response from example.org (HTTP 418).',
    )
  })
})

describe('fetchClinvarVariants', () => {
  it('merges RCV submissions without duplicates', async () => {
    mockFetch({
      total: 14415,
      hits: [
        {
          _id: 'chr17:g.41199721C>T',
          clinvar: {
            variant_id: 125845,
            rcv: [
              { clinical_significance: 'Pathogenic', origin: 'germline' },
              { clinical_significance: 'Pathogenic', origin: 'germline' },
              { clinical_significance: 'Pathogenic', origin: 'unknown' },
            ],
          },
        },
      ],
    })

    const { total, variants } = await fetchClinvarVariants('BRCA1')

    expect(total).toBe(14415)
    expect(variants).toEqual([
      {
        key: 'chr17:g.41199721C>T-125845',
        hgvs: 'chr17:g.41199721C>T',
        variantId: 125845,
        significances: ['Pathogenic'],
        origins: ['germline', 'unknown'],
      },
    ])
  })

  it('accepts a single RCV and an array of origins', async () => {
    mockFetch({
      total: 1,
      hits: [
        {
          _id: 'chr17:g.41199724G>A',
          clinvar: {
            variant_id: 865740,
            rcv: { clinical_significance: 'not provided', origin: ['germline', 'somatic'] },
          },
        },
      ],
    })

    const { variants } = await fetchClinvarVariants('BRCA1')

    expect(variants[0].significances).toEqual(['not provided'])
    expect(variants[0].origins).toEqual(['germline', 'somatic'])
  })

  it('keeps one row per ClinVar record when a hit has several', async () => {
    mockFetch({
      total: 1,
      hits: [
        {
          _id: 'chr1:g.1A>G',
          clinvar: [
            { variant_id: 1, rcv: { clinical_significance: 'Benign' } },
            { variant_id: 2, rcv: { clinical_significance: 'Pathogenic' } },
          ],
        },
      ],
    })

    const { variants } = await fetchClinvarVariants('BRCA1')

    expect(variants.map((v) => v.variantId)).toEqual([1, 2])
    expect(variants[0].origins).toEqual([])
  })

  it('ignores hits without ClinVar data', async () => {
    mockFetch({ total: 1, hits: [{ _id: 'chr1:g.1A>G' }] })

    const { variants } = await fetchClinvarVariants('BRCA1')

    expect(variants).toEqual([])
  })

  it('tolerates a response without hits or total', async () => {
    mockFetch({})

    expect(await fetchClinvarVariants('BRCA1')).toEqual({ total: 0, variants: [] })
  })

  it('sends the requested size to the API', async () => {
    const fetchMock = mockFetch({ total: 0, hits: [] })

    await fetchClinvarVariants('TP53', undefined, 25)

    const url = requestedUrl(fetchMock)
    expect(url.host).toBe('myvariant.info')
    expect(url.searchParams.get('q')).toBe('clinvar.gene.symbol:TP53')
    expect(url.searchParams.get('size')).toBe('25')
  })

  it('fetches 100 variants by default', async () => {
    const fetchMock = mockFetch({ total: 0, hits: [] })

    await fetchClinvarVariants('TP53')

    expect(requestedUrl(fetchMock).searchParams.get('size')).toBe('100')
  })

  it.each(MAX_VARIANTS_CHOICES)('accepts the size choice %i', async (size) => {
    const fetchMock = mockFetch({ total: 0, hits: [] })

    await fetchClinvarVariants('TP53', undefined, size)

    expect(requestedUrl(fetchMock).searchParams.get('size')).toBe(String(size))
  })

  it.each([0, 1001, 2.5, Number.NaN])(
    'rejects the size %s without calling the API',
    async (size) => {
      const fetchMock = mockFetch({})

      await expect(fetchClinvarVariants('TP53', undefined, size)).rejects.toThrow(RangeError)
      expect(fetchMock).not.toHaveBeenCalled()
    },
  )

  it('keeps variants without identifier or significance', async () => {
    mockFetch({ total: 1, hits: [{ _id: 'chr1:g.1A>G', clinvar: { rcv: [{ origin: null }] } }] })

    const { variants } = await fetchClinvarVariants('BRCA1')

    expect(variants).toEqual([
      {
        key: 'chr1:g.1A>G-#0',
        hgvs: 'chr1:g.1A>G',
        variantId: null,
        significances: [],
        origins: [],
      },
    ])
  })
})
