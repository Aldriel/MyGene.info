const CLINVAR_VARIATION_URL = 'https://www.ncbi.nlm.nih.gov/clinvar/variation'

const THEAD_CLASSES = [
  'sticky top-0 bg-slate-50 text-left text-xs font-semibold',
  'uppercase tracking-wide text-slate-600',
].join(' ')

const BADGE_CLASSES = [
  'inline-flex items-center rounded-full px-2.5 py-0.5',
  'text-xs font-medium ring-1 ring-inset',
].join(' ')

/**
 * Couleurs des badges selon la signification clinique.
 * L'ordre compte : « likely pathogenic » doit être testé avant « pathogenic ».
 */
const SIGNIFICANCE_COLORS = [
  ['conflicting', 'bg-purple-100 text-purple-800 ring-purple-200'],
  ['likely pathogenic', 'bg-orange-100 text-orange-800 ring-orange-200'],
  ['pathogenic', 'bg-red-100 text-red-800 ring-red-200'],
  ['benign', 'bg-emerald-100 text-emerald-800 ring-emerald-200'],
  ['uncertain', 'bg-amber-100 text-amber-800 ring-amber-200'],
]
const DEFAULT_COLOR = 'bg-slate-100 text-slate-700 ring-slate-200'

function significanceColor(value) {
  const lower = value.toLowerCase()
  const match = SIGNIFICANCE_COLORS.find(([keyword]) => lower.includes(keyword))
  return match ? match[1] : DEFAULT_COLOR
}

/**
 * Tableau des variants ClinVar d'un gène.
 *
 * @param {object[]} variants Variants renvoyés par `fetchClinvarVariants`.
 * @param {number} total Nombre total de variants disponibles dans ClinVar.
 */
export default function ClinvarTable({ variants, total }) {
  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
        <h3 className="font-semibold text-slate-900">Variants ClinVar</h3>
        <span className="text-sm text-slate-500">
          {variants.length} affichés sur {total.toLocaleString('fr-CA')}
        </span>
      </div>

      <div className="max-h-[32rem] overflow-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className={THEAD_CLASSES}>
            <tr>
              <th className="px-5 py-3">ID du variant</th>
              <th className="px-5 py-3">Signification clinique</th>
              <th className="px-5 py-3">Origine</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {variants.map((variant) => (
              <VariantRow key={variant.key} variant={variant} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

/** Une ligne du tableau : identifiant, significations cliniques et origines. */
function VariantRow({ variant }) {
  return (
    <tr className="align-top transition-colors hover:bg-slate-50">
      <td className="px-5 py-3">
        <a
          href={`${CLINVAR_VARIATION_URL}/${variant.variantId}/`}
          target="_blank"
          rel="noreferrer"
          className="font-medium text-indigo-600 hover:underline"
        >
          {variant.variantId}
        </a>
        <div className="font-mono text-xs text-slate-500">{variant.hgvs}</div>
      </td>

      <td className="px-5 py-3">
        <div className="flex flex-wrap gap-1.5">
          {variant.significances.length > 0 ? (
            variant.significances.map((significance) => (
              <span
                key={significance}
                className={`${BADGE_CLASSES} ${significanceColor(significance)}`}
              >
                {significance}
              </span>
            ))
          ) : (
            <span className="text-slate-400">—</span>
          )}
        </div>
      </td>

      <td className="px-5 py-3 text-slate-700">
        {variant.origins.length > 0 ? variant.origins.join(', ') : '—'}
      </td>
    </tr>
  )
}
