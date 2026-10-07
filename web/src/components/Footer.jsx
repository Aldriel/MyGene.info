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

import { BRAND } from '../brand.js'
import { useI18n } from '../i18n/I18nContext.jsx'

const LINK_CLASSES = 'hover:text-indigo-700 hover:underline'

/**
 * Discreet footer, always visible at the bottom of the window: the licence at the left, then,
 * centred in the remaining space on one line (more if the window is narrow), author and contact,
 * availability and the Ko-fi button. The button reproduces the official Ko-fi widget, whose script
 * writes into the page with document.write and cannot run in a React application; its cup is
 * drawn inline, so the page loads nothing from Ko-fi.
 */
export default function Footer() {
  const { t } = useI18n()

  return (
    <footer className="sticky bottom-0 z-10 border-t border-slate-200 bg-white/95 backdrop-blur">
      <div className="flex items-center gap-[2em] py-2 pl-[1.5em] pr-4 text-[15.6px] text-slate-500">
        <a
          href={BRAND.license}
          target="_blank"
          rel="noreferrer"
          title={t('footer.licenseTitle')}
          className={`whitespace-nowrap ${LINK_CLASSES}`}
        >
          {t('footer.license')}
        </a>
        <div className="flex flex-1 flex-wrap items-center justify-center gap-x-[3em] gap-y-1.5 text-center">
          <p>
            <a href={t('brand.website')} target="_blank" rel="noreferrer" className={LINK_CLASSES}>
              © 2026 {t('brand.title')}
            </a>
            <span aria-hidden="true"> · </span>
            <a href={`mailto:${BRAND.email}`} className={LINK_CLASSES}>
              {BRAND.email}
            </a>
          </p>
          <p className="italic">{t('footer.availability')}</p>
          <p>
            <a
              href={BRAND.kofi}
              target="_blank"
              rel="noreferrer"
              title={t('footer.kofiTitle')}
              className="inline-flex items-center gap-2 rounded-[9px] bg-[#4169e1] px-4 py-1.5 text-lg font-bold text-white shadow-[1px_1px_0_rgba(0,0,0,0.2)] transition hover:bg-[#2f54c4]"
            >
              <svg
                viewBox="0 0 14 11"
                className="h-[18px] w-[21px] fill-current"
                aria-hidden="true"
              >
                <path d="M1 1h10v4.5A3.5 3.5 0 0 1 7.5 9h-3A3.5 3.5 0 0 1 1 5.5z" />
                <path d="M11 2h1.2a2.2 2.2 0 0 1 0 4.4H11V5.2h1.2a1 1 0 0 0 0-2H11z" />
              </svg>
              {t('footer.kofi')}
            </a>
          </p>
        </div>
      </div>
    </footer>
  )
}
