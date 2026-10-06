import { useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'

/** Summaries longer than this are collapsed until the user expands them. */
export const SUMMARY_PREVIEW_LENGTH = 320

const LINK_CLASSES = [
  'inline-flex items-center gap-1 rounded-md border border-slate-200 px-2.5 py-1 text-sm',
  'font-medium text-indigo-700 transition hover:border-indigo-300 hover:bg-indigo-50',
].join(' ')

/**
 * Identity card of a gene: name, type, location, aliases, summary and external links.
 *
 * @param {{gene: object, retrievedAt: string}} props
 */
export default function GeneCard({ gene, retrievedAt }) {
  const { t, dateTime } = useI18n()
  const [expanded, setExpanded] = useState(false)

  const details = [
    [t('gene.type'), gene.typeOfGene],
    [t('gene.location'), gene.mapLocation],
    [t('gene.aliases'), gene.aliases?.length ? gene.aliases.join(', ') : null],
  ].filter(([, value]) => value)

  const summary = gene.summary ?? ''
  const collapsible = summary.length > SUMMARY_PREVIEW_LENGTH
  const shownSummary =
    collapsible && !expanded ? `${summary.slice(0, SUMMARY_PREVIEW_LENGTH).trimEnd()}…` : summary

  return (
    <article className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold text-slate-900">{gene.symbol}</h2>
          {gene.name && <p className="mt-1 text-lg text-slate-600">{gene.name}</p>}
        </div>
        <div className="flex flex-wrap gap-2">
          {gene.entrezGene != null && (
            <a
              href={`https://www.ncbi.nlm.nih.gov/gene/${gene.entrezGene}`}
              target="_blank"
              rel="noreferrer"
              className={LINK_CLASSES}
            >
              {t('gene.ncbiLink', { id: String(gene.entrezGene) })}
              <ExternalIcon />
            </a>
          )}
          <a
            href={`https://www.genecards.org/cgi-bin/carddisp.pl?gene=${encodeURIComponent(gene.symbol)}`}
            target="_blank"
            rel="noreferrer"
            className={LINK_CLASSES}
          >
            GeneCards
            <ExternalIcon />
          </a>
        </div>
      </div>

      {details.length > 0 && (
        <dl className="mt-4 grid gap-x-8 gap-y-1 text-sm sm:grid-cols-[max-content_1fr]">
          {details.map(([label, value]) => (
            <div key={label} className="contents">
              <dt className="font-medium text-slate-500">{label}</dt>
              <dd className="text-slate-800">{value}</dd>
            </div>
          ))}
        </dl>
      )}

      {summary && (
        <div className="mt-4">
          <h3 className="text-sm font-semibold text-slate-700">{t('gene.summary')}</h3>
          <p className="mt-1 text-sm leading-relaxed text-slate-600">{shownSummary}</p>
          {collapsible && (
            <button
              type="button"
              aria-expanded={expanded}
              onClick={() => setExpanded((value) => !value)}
              className="mt-1 text-sm font-medium text-indigo-600 hover:underline"
            >
              {expanded ? t('gene.showLess') : t('gene.showMore')}
            </button>
          )}
        </div>
      )}

      <p className="mt-4 text-xs text-slate-400">
        {t('status.retrievedAt', { date: dateTime(retrievedAt) })}
      </p>
    </article>
  )
}

/** Small arrow marking links that open another site. */
function ExternalIcon() {
  return (
    <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" fill="currentColor" aria-hidden="true">
      <path d="M11 3a1 1 0 1 0 0 2h2.59l-6.3 6.29a1 1 0 1 0 1.42 1.42L15 6.41V9a1 1 0 1 0 2 0V4a1 1 0 0 0-1-1h-5Z" />
      <path d="M5 5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-3a1 1 0 1 0-2 0v3H5V7h3a1 1 0 0 0 0-2H5Z" />
    </svg>
  )
}
