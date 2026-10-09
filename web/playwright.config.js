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

import { defineConfig } from '@playwright/test'

/**
 * Functional tests: a real browser searches real genes on the live BioThings APIs, like a
 * visitor. By default they test the production build served by `vite preview`; set
 * E2E_BASE_URL to test a deployed copy instead, e.g. https://maximeethier.com/MyGene.info/.
 */
const deployed = process.env.E2E_BASE_URL
const PREVIEW_URL = 'http://localhost:4173/'
const MONITOR_USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) ' +
  'Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0 site-monitor'

export default defineConfig({
  testDir: 'e2e',
  // The public APIs are sometimes slow: one retry tells a hiccup from a bug.
  retries: 1,
  timeout: 120_000,
  expect: { timeout: 60_000 },
  fullyParallel: true,
  workers: process.env.CI ? 2 : 3,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: deployed ?? PREVIEW_URL,
    // Microsoft Edge is already installed on Windows; CI uses the browser of Playwright.
    channel: process.env.CI ? undefined : 'msedge',
    // maximeethier.com refuses "HeadlessChrome": the monitor names itself instead.
    userAgent: deployed ? MONITOR_USER_AGENT : undefined,
    acceptDownloads: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  // The interface follows the language of the browser.
  projects: [
    { name: 'en', use: { locale: 'en-CA' } },
    { name: 'fr', use: { locale: 'fr-CA' } },
  ],
  webServer: deployed
    ? undefined
    : {
        command: 'npm run build && npm run preview -- --port 4173 --strictPort',
        url: PREVIEW_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
})
