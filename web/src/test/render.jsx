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

import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { I18nProvider } from '../i18n/I18nContext.jsx'

/**
 * Renders a component in a language, with a user-event session.
 *
 * @param {React.ReactElement} ui Component to render.
 * @param {{language?: string}} [options] Interface language, English by default.
 */
export function renderWithI18n(ui, { language = 'en' } = {}) {
  const user = userEvent.setup()
  const view = render(<I18nProvider language={language}>{ui}</I18nProvider>)
  return {
    user,
    ...view,
    rerenderWith: (next, nextLanguage = language) =>
      view.rerender(<I18nProvider language={nextLanguage}>{next}</I18nProvider>),
  }
}
