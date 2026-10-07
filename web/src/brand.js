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

/**
 * Application and author identity. The author's title and website are translated (see the
 * `brand.*` messages).
 */
export const BRAND = {
  application: 'MyGene Explorer',
  author: 'Maxime Ethier',
  email: 'contact@maximeethier.com',
  kofi: 'https://ko-fi.com/K1S228CL7A',
  // Licence text, published next to the application by the build (see vite.config.js).
  license: `${import.meta.env.BASE_URL}LICENSE.txt`,
}

/** Version of the application, injected from package.json at build time. */
export const APP_VERSION = typeof __APP_VERSION__ === 'string' ? __APP_VERSION__ : 'dev'
