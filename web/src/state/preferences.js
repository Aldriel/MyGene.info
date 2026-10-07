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
 * User preferences remembered by the browser: language, text size, number of variants to fetch
 * and recent searches. Same settings as the desktop application.
 */

import { MAX_VARIANTS_CHOICES } from '../api/biothings.js'
import { supportedLanguage } from '../i18n/translate.js'

export const STORAGE_KEY = 'mygene-explorer/preferences'

/** Text sizes offered, in CSS pixels applied to the root element. */
export const FONT_SIZES = [
  { value: 14, label: 'textSize.small' },
  { value: 16, label: 'textSize.normal' },
  { value: 18, label: 'textSize.large' },
  { value: 20, label: 'textSize.extraLarge' },
]

export const DEFAULT_FONT_SIZE = 16
export const DEFAULT_MAX_VARIANTS = 100
export const MAX_RECENT_SEARCHES = 10

/**
 * Returns the preferences of a first visit.
 *
 * @param {string} [browserLanguage] Language of the browser, e.g. `navigator.language`.
 * @returns {{language: string, fontSize: number, maxVariants: number, recentSearches: string[]}}
 */
export function defaultPreferences(browserLanguage) {
  return {
    language: supportedLanguage(browserLanguage),
    fontSize: DEFAULT_FONT_SIZE,
    maxVariants: DEFAULT_MAX_VARIANTS,
    recentSearches: [],
  }
}

/**
 * Keeps the valid fields of stored preferences and completes the others with the defaults, so
 * a corrupted or outdated entry never breaks the application.
 *
 * @param {unknown} stored Parsed stored value.
 * @param {ReturnType<typeof defaultPreferences>} defaults Fallback values.
 * @returns {ReturnType<typeof defaultPreferences>} Valid preferences.
 */
export function sanitizePreferences(stored, defaults) {
  if (stored == null || typeof stored !== 'object') return defaults
  const { language, fontSize, maxVariants, recentSearches } = stored
  return {
    language:
      typeof language === 'string' && supportedLanguage(language) === language
        ? language
        : defaults.language,
    fontSize: FONT_SIZES.some((size) => size.value === fontSize) ? fontSize : defaults.fontSize,
    maxVariants: MAX_VARIANTS_CHOICES.includes(maxVariants) ? maxVariants : defaults.maxVariants,
    recentSearches: Array.isArray(recentSearches)
      ? [...new Set(recentSearches.filter((s) => typeof s === 'string' && s))].slice(
          0,
          MAX_RECENT_SEARCHES,
        )
      : defaults.recentSearches,
  }
}

/**
 * Reads the stored preferences.
 *
 * @param {Storage|undefined} storage Where preferences are kept, usually `localStorage`.
 * @param {string} [browserLanguage] Language used on a first visit.
 * @returns {ReturnType<typeof defaultPreferences>} The preferences, defaults when absent or
 *   unreadable.
 */
export function loadPreferences(storage, browserLanguage) {
  const defaults = defaultPreferences(browserLanguage)
  try {
    const json = storage?.getItem(STORAGE_KEY)
    return json ? sanitizePreferences(JSON.parse(json), defaults) : defaults
  } catch {
    return defaults
  }
}

/**
 * Stores the preferences. Failures (private browsing, full storage) are ignored: preferences
 * are a convenience, not data.
 *
 * @param {Storage|undefined} storage Where preferences are kept.
 * @param {object} preferences Preferences to store.
 */
export function savePreferences(storage, preferences) {
  try {
    storage?.setItem(STORAGE_KEY, JSON.stringify(preferences))
  } catch {
    // Ignored on purpose.
  }
}

/**
 * Puts a symbol at the top of the recent searches.
 *
 * @param {string[]} recent Current recent searches, most recent first.
 * @param {string} symbol Symbol just searched.
 * @returns {string[]} New list without duplicates, at most `MAX_RECENT_SEARCHES` long.
 */
export function addRecentSearch(recent, symbol) {
  return [symbol, ...recent.filter((s) => s !== symbol)].slice(0, MAX_RECENT_SEARCHES)
}
