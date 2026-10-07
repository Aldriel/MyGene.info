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

import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'

const ITEMS = [
  { format: 'csv', label: 'actions.exportCsv' },
  { format: 'json', label: 'actions.exportJson' },
  { format: 'pdf', label: 'actions.exportPdf' },
]

/**
 * Drop-down menu of the export formats. Closes on selection, on Escape and on a click outside.
 *
 * @param {{onExport: (format: 'csv'|'json'|'pdf') => void, disabled?: boolean}} props
 */
export default function ExportMenu({ onExport, disabled = false }) {
  const { t } = useI18n()
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)
  const buttonRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false)
    }
    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOpen(false)
        buttonRef.current?.focus()
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  return (
    <div ref={rootRef} className="relative">
      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => setOpen((value) => !value)}
        className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-50"
      >
        {t('actions.export')}
        <span aria-hidden="true">▾</span>
      </button>
      {open && (
        <ul
          role="menu"
          className="absolute right-0 z-20 mt-2 w-64 overflow-hidden rounded-lg border border-slate-200 bg-white py-1 shadow-lg"
        >
          {ITEMS.map(({ format, label }) => (
            <li key={format} role="none">
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setOpen(false)
                  onExport(format)
                }}
                className="block w-full px-4 py-2 text-left text-sm text-slate-700 hover:bg-indigo-50 hover:text-indigo-800"
              >
                {t(label)}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
