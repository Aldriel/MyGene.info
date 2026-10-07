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
