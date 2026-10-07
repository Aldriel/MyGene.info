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

import { useCallback, useEffect, useState } from 'react'
import { addRecentSearch, loadPreferences, savePreferences } from './preferences.js'

/** Returns `localStorage`, or `undefined` where the browser denies access to it. */
function browserStorage() {
  try {
    return globalThis.localStorage
  } catch {
    return undefined
  }
}

/**
 * Loads, applies and persists the user preferences.
 *
 * The text size is applied to the root element so every `rem` of the interface scales with it,
 * and the language to its `lang` attribute for screen readers and hyphenation.
 *
 * @returns {{preferences: object, setLanguage: (language: string) => void,
 *   setFontSize: (size: number) => void, setMaxVariants: (count: number) => void,
 *   rememberSearch: (symbol: string) => void, clearRecentSearches: () => void}}
 */
export function usePreferences() {
  const [preferences, setPreferences] = useState(() =>
    loadPreferences(browserStorage(), globalThis.navigator?.language),
  )

  useEffect(() => {
    savePreferences(browserStorage(), preferences)
  }, [preferences])

  useEffect(() => {
    document.documentElement.lang = preferences.language
  }, [preferences.language])

  useEffect(() => {
    document.documentElement.style.fontSize = `${preferences.fontSize}px`
  }, [preferences.fontSize])

  const update = useCallback((changes) => setPreferences((p) => ({ ...p, ...changes })), [])

  return {
    preferences,
    setLanguage: useCallback((language) => update({ language }), [update]),
    setFontSize: useCallback((fontSize) => update({ fontSize }), [update]),
    setMaxVariants: useCallback((maxVariants) => update({ maxVariants }), [update]),
    rememberSearch: useCallback(
      (symbol) =>
        setPreferences((p) => ({
          ...p,
          recentSearches: addRecentSearch(p.recentSearches, symbol),
        })),
      [],
    ),
    clearRecentSearches: useCallback(() => update({ recentSearches: [] }), [update]),
  }
}
