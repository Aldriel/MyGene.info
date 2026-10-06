/**
 * Translation and locale-aware formatting, independent of React so the CSV and PDF exports can
 * use them too.
 */

import { MESSAGES } from './messages.js'

export const DEFAULT_LANGUAGE = 'en'

/** Locale used for numbers and dates in each language. */
const FORMAT_LOCALES = { en: 'en-US', fr: 'fr-CA' }

/**
 * Returns the supported language matching a browser language tag.
 *
 * @param {string} [tag] Language tag such as `fr-CA`.
 * @returns {'en'|'fr'} The language, English when unsupported.
 */
export function supportedLanguage(tag) {
  const language = String(tag ?? '')
    .slice(0, 2)
    .toLowerCase()
  return Object.hasOwn(MESSAGES, language) ? language : DEFAULT_LANGUAGE
}

/**
 * Creates the translator of a language.
 *
 * @param {string} language Language code; unsupported languages fall back to English.
 * @returns {{language: string, locale: string, t: (key: string, params?: object) => string,
 *   number: (value: number) => string, percent: (fraction: number) => string,
 *   dateTime: (iso: string|Date) => string}}
 */
export function createTranslator(language) {
  const code = supportedLanguage(language)
  const messages = MESSAGES[code]
  const locale = FORMAT_LOCALES[code]
  const numberFormat = new Intl.NumberFormat(locale)
  const percentFormat = new Intl.NumberFormat(locale, {
    style: 'percent',
    maximumFractionDigits: 0,
  })
  const dateFormat = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' })

  const number = (value) => numberFormat.format(value)

  /**
   * Translates a key, replacing `{name}` placeholders; numbers are formatted for the language.
   * A missing key is returned as `!key!` so it is noticed rather than hidden.
   */
  const t = (key, params = {}) => {
    const pattern = messages[key]
    if (pattern == null) return `!${key}!`
    return pattern.replace(/\{(\w+)\}/g, (placeholder, name) => {
      if (!Object.hasOwn(params, name)) return placeholder
      const value = params[name]
      return typeof value === 'number' ? number(value) : String(value)
    })
  }

  return {
    language: code,
    locale,
    t,
    number,
    percent: (fraction) => percentFormat.format(fraction),
    dateTime: (value) => dateFormat.format(new Date(value)),
  }
}
