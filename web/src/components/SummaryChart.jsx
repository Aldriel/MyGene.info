import { useI18n } from '../i18n/I18nContext.jsx'
import { countByPrimary } from '../model/significance.js'

/** Radius giving a circumference of 100, so dash lengths are percentages. */
const RADIUS = 100 / (2 * Math.PI)

/**
 * Distribution of the loaded variants by most severe clinical significance: donut chart,
 * legend and share of pathogenic variants.
 *
 * @param {{variants: object[], total: number}} props
 */
export default function SummaryChart({ variants, total }) {
  const { t, number, percent } = useI18n()

  if (variants.length === 0) {
    return <p className="p-6 text-slate-600">{t('summary.empty')}</p>
  }

  const counts = countByPrimary(variants)
  const loaded = variants.length
  const pathogenic = counts
    .filter(({ id }) => id === 'pathogenic' || id === 'likely-pathogenic')
    .reduce((sum, { count }) => sum + count, 0)

  let offset = 0
  const segments = counts
    .filter(({ count }) => count > 0)
    .map((category) => {
      const length = (category.count / loaded) * 100
      const segment = { ...category, length, start: offset }
      offset += length
      return segment
    })

  return (
    <div className="p-6">
      <h3 className="font-semibold text-slate-900">{t('summary.title')}</h3>

      <div className="mt-4 flex flex-wrap items-center gap-10">
        <svg viewBox="0 0 42 42" className="h-56 w-56 shrink-0" role="img">
          <title>{t('summary.chart')}</title>
          <circle cx="21" cy="21" r={RADIUS} fill="none" stroke="#f1f5f9" strokeWidth="6" />
          {segments.map(({ id, color, length, start }) => (
            <circle
              key={id}
              data-category={id}
              cx="21"
              cy="21"
              r={RADIUS}
              fill="none"
              stroke={color}
              strokeWidth="6"
              strokeDasharray={`${length} ${100 - length}`}
              strokeDashoffset={25 - start}
            />
          ))}
          <text
            x="21"
            y="20"
            textAnchor="middle"
            className="fill-slate-900"
            style={{ fontSize: '6px', fontWeight: 700 }}
          >
            {number(loaded)}
          </text>
          <text
            x="21"
            y="26"
            textAnchor="middle"
            className="fill-slate-500"
            style={{ fontSize: '3px' }}
          >
            {t('tab.variants')}
          </text>
        </svg>

        <ul className="min-w-64 flex-1 space-y-2">
          {counts.map(({ id, color, count }) => (
            <li key={id} className="flex items-center gap-3 text-sm">
              <span
                className="h-3 w-3 shrink-0 rounded-sm"
                style={{ backgroundColor: color }}
                aria-hidden="true"
              />
              <span className="flex-1 text-slate-800">{t(`significance.${id}`)}</span>
              <span className="text-slate-500 tabular-nums">
                {t('summary.legendCount', { count, percent: percent(count / loaded) })}
              </span>
            </li>
          ))}
        </ul>
      </div>

      <p className="mt-6 font-semibold text-slate-900">
        {t('summary.pathogenicShare', {
          count: pathogenic,
          percent: percent(pathogenic / loaded),
        })}
      </p>
      <p className="mt-1 text-xs text-slate-500">{t('summary.caption', { loaded, total })}</p>
    </div>
  )
}
