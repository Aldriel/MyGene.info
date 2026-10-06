import { describe, expect, it } from 'vitest'
import { LANGUAGES, MESSAGES } from './messages.js'
import { createTranslator, DEFAULT_LANGUAGE, supportedLanguage } from './translate.js'

const placeholders = (text) => [...text.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort()

describe('messages', () => {
  it('defines the same keys in every language', () => {
    const englishKeys = Object.keys(MESSAGES.en).sort()
    for (const { code } of LANGUAGES) {
      expect(Object.keys(MESSAGES[code]).sort(), code).toEqual(englishKeys)
    }
  })

  it('uses the same placeholders in every language', () => {
    for (const [key, english] of Object.entries(MESSAGES.en)) {
      expect(placeholders(MESSAGES.fr[key]), key).toEqual(placeholders(english))
    }
  })

  it('has no blank message', () => {
    for (const { code } of LANGUAGES) {
      for (const [key, text] of Object.entries(MESSAGES[code])) {
        expect(text.trim(), `${code}:${key}`).not.toBe('')
      }
    }
  })

  it('offers each language named in itself', () => {
    expect(LANGUAGES).toEqual([
      { code: 'en', name: 'English' },
      { code: 'fr', name: 'Français' },
    ])
  })

  it('signs with the translated business title and website', () => {
    expect(MESSAGES.en['brand.title']).toBe('Maxime Ethier - Biocomputing Consultant')
    expect(MESSAGES.en['brand.website']).toBe('https://www.maximeethier.com/en')
    expect(MESSAGES.fr['brand.title']).toBe('Maxime Ethier - Consultant en Bio-informatique')
    expect(MESSAGES.fr['brand.website']).toBe('https://www.maximeethier.com')
  })
})

describe('supportedLanguage', () => {
  it.each([
    ['fr', 'fr'],
    ['fr-CA', 'fr'],
    ['FR-fr', 'fr'],
    ['en-US', 'en'],
    ['de-DE', DEFAULT_LANGUAGE],
    ['', DEFAULT_LANGUAGE],
    [undefined, DEFAULT_LANGUAGE],
  ])('maps %j to %s', (tag, language) => {
    expect(supportedLanguage(tag)).toBe(language)
  })
})

describe('createTranslator', () => {
  it('translates and interpolates', () => {
    const { t } = createTranslator('fr')

    expect(t('search.button')).toBe('Rechercher')
    expect(t('error.geneNotFound', { symbol: 'XYZ' })).toBe(
      'Aucun gène humain trouvé pour le symbole « XYZ ».',
    )
  })

  it('formats numbers for the language', () => {
    const en = createTranslator('en')
    const fr = createTranslator('fr')

    expect(en.t('filter.count', { shown: 5, loaded: 100, total: 13542 })).toBe(
      'Showing 5 of 100 loaded · 13,542 in ClinVar',
    )
    expect(fr.number(13542)).toMatch(/^13\s542$/)
    expect(fr.percent(0.3)).toMatch(/^30\s%$/)
    expect(en.percent(0.3)).toBe('30%')
  })

  it('formats dates for the language', () => {
    const date = '2026-10-06T14:30:00.000Z'

    expect(createTranslator('en').dateTime(date)).toContain('2026')
    expect(createTranslator('fr').dateTime(date)).toContain('oct.')
  })

  it('exposes the language and the formatting locale', () => {
    expect(createTranslator('fr')).toMatchObject({ language: 'fr', locale: 'fr-CA' })
    expect(createTranslator('es')).toMatchObject({ language: 'en', locale: 'en-US' })
  })

  it('marks missing keys and leaves unknown placeholders', () => {
    const { t } = createTranslator('en')

    expect(t('no.such.key')).toBe('!no.such.key!')
    expect(t('status.downloaded')).toBe('{file} downloaded.')
  })
})
