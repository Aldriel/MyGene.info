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
