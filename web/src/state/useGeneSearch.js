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

import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchClinvarVariants, fetchGene } from '../api/biothings.js'
import { normalizeGeneSymbol } from '../api/geneSymbol.js'
import { GeneNotFoundError } from '../i18n/errors.js'

const IDLE = { status: 'idle', result: null, error: null }

/**
 * State of the gene search: `idle`, `loading`, `success` (with a result) or `error`.
 *
 * Starting a search aborts the previous one, so a slow earlier response can never replace a
 * newer result.
 *
 * @returns {{status: string, result: object|null, error: unknown,
 *   search: (input: string, maxVariants: number) => Promise<object|null>,
 *   showResult: (result: object) => void, reset: () => void}}
 */
export function useGeneSearch() {
  const [state, setState] = useState(IDLE)
  const controllerRef = useRef(null)

  const abortRunning = () => {
    controllerRef.current?.abort()
    controllerRef.current = null
  }

  useEffect(() => abortRunning, [])

  /**
   * Searches a gene and its ClinVar variants.
   *
   * @returns The result, or `null` if the search failed or was superseded.
   */
  const search = useCallback(async (input, maxVariants) => {
    abortRunning()
    let symbol
    try {
      symbol = normalizeGeneSymbol(input)
    } catch (error) {
      setState({ status: 'error', result: null, error })
      return null
    }

    const controller = new AbortController()
    controllerRef.current = controller
    setState({ status: 'loading', result: null, error: null })
    try {
      const gene = await fetchGene(symbol, controller.signal)
      if (!gene) throw new GeneNotFoundError(symbol)
      const clinvar = await fetchClinvarVariants(gene.symbol, controller.signal, maxVariants)
      const result = { query: symbol, gene, clinvar, retrievedAt: new Date().toISOString() }
      if (controller.signal.aborted) return null
      controllerRef.current = null
      setState({ status: 'success', result, error: null })
      return result
    } catch (error) {
      if (controller.signal.aborted) return null
      controllerRef.current = null
      setState({ status: 'error', result: null, error })
      return null
    }
  }, [])

  /** Displays a result obtained elsewhere, e.g. read from a file. */
  const showResult = useCallback((result) => {
    abortRunning()
    setState({ status: 'success', result, error: null })
  }, [])

  const reset = useCallback(() => {
    abortRunning()
    setState(IDLE)
  }, [])

  return { ...state, search, showResult, reset }
}
