import { APP_VERSION, BRAND } from '../brand.js'
import { useI18n } from '../i18n/I18nContext.jsx'

/** Author signature, data sources, version and disclaimer. */
export default function Footer() {
  const { t } = useI18n()
  const website = t('brand.website')

  return (
    <footer className="mt-16 border-t border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-wrap items-start justify-between gap-6 px-4 py-6 text-sm">
        <div>
          <p className="text-slate-500">{t('footer.builtBy')}</p>
          <p className="font-semibold text-slate-900">{t('brand.title')}</p>
          <p className="mt-1 flex flex-wrap gap-x-3">
            <a
              href={website}
              target="_blank"
              rel="noreferrer"
              className="text-indigo-600 hover:underline"
            >
              {website.replace(/^https:\/\//, '')}
            </a>
            <a href={`mailto:${BRAND.email}`} className="text-indigo-600 hover:underline">
              {BRAND.email}
            </a>
          </p>
        </div>
        <div className="text-right text-xs text-slate-500">
          <p>{t('status.sources')}</p>
          <p className="mt-1">
            {BRAND.application} · {t('footer.version', { version: APP_VERSION })}
          </p>
          <p className="mt-1">{t('app.disclaimer')}</p>
        </div>
      </div>
    </footer>
  )
}
