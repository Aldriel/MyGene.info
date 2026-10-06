import { BRAND } from '../brand.js'
import { LANGUAGES } from '../i18n/messages.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { FONT_SIZES } from '../state/preferences.js'

const SELECT_CLASSES = [
  'rounded-md border border-slate-300 bg-white px-2 py-1 text-sm text-slate-800',
  'focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 focus:outline-none',
].join(' ')

/**
 * Application title with the language and text size settings.
 *
 * @param {{language: string, fontSize: number, onLanguageChange: (language: string) => void,
 *   onFontSizeChange: (size: number) => void, onHome: () => void}} props
 */
export default function Header({ language, fontSize, onLanguageChange, onFontSizeChange, onHome }) {
  const { t } = useI18n()

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            <a
              href="./"
              onClick={(event) => {
                event.preventDefault()
                onHome()
              }}
              className="inline-flex items-center gap-2 hover:text-indigo-700"
            >
              <Logo />
              {BRAND.application}
            </a>
          </h1>
          <p className="mt-0.5 text-sm text-slate-600">{t('app.subtitle')}</p>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-600">
            {t('header.language')}
            <select
              value={language}
              onChange={(event) => onLanguageChange(event.target.value)}
              className={SELECT_CLASSES}
            >
              {LANGUAGES.map(({ code, name }) => (
                <option key={code} value={code} lang={code}>
                  {name}
                </option>
              ))}
            </select>
          </label>

          <label className="flex items-center gap-2 text-sm text-slate-600">
            {t('header.textSize')}
            <select
              value={fontSize}
              onChange={(event) => onFontSizeChange(Number(event.target.value))}
              className={SELECT_CLASSES}
            >
              {FONT_SIZES.map(({ value, label }) => (
                <option key={value} value={value}>
                  {t(label)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>
    </header>
  )
}

/** Double helix mark of the application. */
function Logo() {
  return (
    <svg viewBox="0 0 24 24" className="h-7 w-7 text-indigo-600" aria-hidden="true">
      <path
        d="M7 3c0 4.5 10 4.5 10 9s-10 4.5-10 9M17 3c0 4.5-10 4.5-10 9s10 4.5 10 9"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path d="M8 4h8M8.5 12h7M8 20h8" stroke="currentColor" strokeWidth="1.5" opacity="0.6" />
    </svg>
  )
}
