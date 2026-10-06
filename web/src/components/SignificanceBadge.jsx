import { classify, colorOf } from '../model/significance.js'

/**
 * Clinical significance as reported by ClinVar, with the color of its category.
 *
 * @param {{significance: string}} props
 */
export default function SignificanceBadge({ significance }) {
  const category = classify(significance)
  return (
    <span
      data-category={category}
      className="inline-flex items-center gap-1.5 rounded-full bg-slate-50 px-2.5 py-0.5 text-xs font-medium text-slate-800 ring-1 ring-slate-200 ring-inset"
    >
      <span
        className="h-2 w-2 shrink-0 rounded-full"
        style={{ backgroundColor: colorOf(category) }}
        aria-hidden="true"
      />
      {significance}
    </span>
  )
}
