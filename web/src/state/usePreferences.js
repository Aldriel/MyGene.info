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
