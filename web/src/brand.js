/**
 * Application and author identity. The author's title and website are translated (see the
 * `brand.*` messages).
 */
export const BRAND = {
  application: 'MyGene Explorer',
  author: 'Maxime Ethier',
  email: 'contact@maximeethier.com',
}

/** Version of the application, injected from package.json at build time. */
export const APP_VERSION = typeof __APP_VERSION__ === 'string' ? __APP_VERSION__ : 'dev'
