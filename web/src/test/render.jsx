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
