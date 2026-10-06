import { createContext, useContext, useMemo } from 'react'
import { createTranslator, DEFAULT_LANGUAGE } from './translate.js'

const I18nContext = createContext(createTranslator(DEFAULT_LANGUAGE))

/**
 * Provides the translator of the current language to the components below.
 *
 * @param {{language: string, children: React.ReactNode}} props
 */
export function I18nProvider({ language, children }) {
  const translator = useMemo(() => createTranslator(language), [language])
  return <I18nContext.Provider value={translator}>{children}</I18nContext.Provider>
}

/**
 * Returns the translator of the current language: `t(key, params)` and number, percent and
 * date formatters.
 */
export function useI18n() {
  return useContext(I18nContext)
}
