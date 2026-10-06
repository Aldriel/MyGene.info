import { describe, expect, it, vi } from 'vitest'
import {
  addRecentSearch,
  DEFAULT_FONT_SIZE,
  DEFAULT_MAX_VARIANTS,
  defaultPreferences,
  loadPreferences,
  MAX_RECENT_SEARCHES,
  sanitizePreferences,
  savePreferences,
  STORAGE_KEY,
} from './preferences.js'

/** In-memory `Storage`. */
function memoryStorage(initial = {}) {
  const data = new Map(Object.entries(initial))
  return {
    getItem: (key) => (data.has(key) ? data.get(key) : null),
    setItem: (key, value) => data.set(key, String(value)),
    data,
  }
}

describe('defaultPreferences', () => {
  it('follows the browser language', () => {
    expect(defaultPreferences('fr-CA')).toEqual({
      language: 'fr',
      fontSize: DEFAULT_FONT_SIZE,
      maxVariants: DEFAULT_MAX_VARIANTS,
      recentSearches: [],
    })
    expect(defaultPreferences('ja-JP').language).toBe('en')
  })
})

describe('sanitizePreferences', () => {
  const defaults = defaultPreferences('en')

  it('keeps valid values', () => {
    const stored = { language: 'fr', fontSize: 20, maxVariants: 500, recentSearches: ['TP53'] }

    expect(sanitizePreferences(stored, defaults)).toEqual(stored)
  })

  it('replaces invalid values with the defaults', () => {
    expect(
      sanitizePreferences(
        { language: 'de', fontSize: 99, maxVariants: 5000, recentSearches: 'TP53' },
        defaults,
      ),
    ).toEqual(defaults)
    expect(sanitizePreferences({ language: 'fr-CA' }, defaults).language).toBe('en')
  })

  it('cleans the recent searches', () => {
    const recent = ['A', 'A', '', 3, null, ...Array.from({ length: 20 }, (_, i) => `G${i}`)]

    const { recentSearches } = sanitizePreferences({ recentSearches: recent }, defaults)

    expect(recentSearches).toHaveLength(MAX_RECENT_SEARCHES)
    expect(recentSearches.slice(0, 2)).toEqual(['A', 'G0'])
  })

  it.each([null, 'text', 42])('ignores a stored %j', (stored) => {
    expect(sanitizePreferences(stored, defaults)).toBe(defaults)
  })
})

describe('loadPreferences and savePreferences', () => {
  it('round-trips the preferences', () => {
    const storage = memoryStorage()
    const preferences = { language: 'fr', fontSize: 18, maxVariants: 250, recentSearches: ['CFTR'] }

    savePreferences(storage, preferences)

    expect(JSON.parse(storage.data.get(STORAGE_KEY))).toEqual(preferences)
    expect(loadPreferences(storage, 'en')).toEqual(preferences)
  })

  it('returns the defaults on a first visit', () => {
    expect(loadPreferences(memoryStorage(), 'fr')).toEqual(defaultPreferences('fr'))
    expect(loadPreferences(undefined, 'fr')).toEqual(defaultPreferences('fr'))
  })

  it('returns the defaults when the stored value is corrupted', () => {
    expect(loadPreferences(memoryStorage({ [STORAGE_KEY]: '{not json' }), 'en')).toEqual(
      defaultPreferences('en'),
    )
  })

  it('ignores storage failures', () => {
    const broken = {
      getItem: vi.fn(() => {
        throw new Error('denied')
      }),
      setItem: vi.fn(() => {
        throw new Error('quota')
      }),
    }

    expect(loadPreferences(broken, 'en')).toEqual(defaultPreferences('en'))
    expect(() => savePreferences(broken, defaultPreferences('en'))).not.toThrow()
  })
})

describe('addRecentSearch', () => {
  it('puts the symbol first without duplicate', () => {
    expect(addRecentSearch(['TP53', 'BRCA1', 'CFTR'], 'BRCA1')).toEqual(['BRCA1', 'TP53', 'CFTR'])
  })

  it('keeps at most the maximum number of searches', () => {
    const recent = Array.from({ length: MAX_RECENT_SEARCHES }, (_, i) => `G${i}`)

    const updated = addRecentSearch(recent, 'NEW')

    expect(updated).toHaveLength(MAX_RECENT_SEARCHES)
    expect(updated[0]).toBe('NEW')
    expect(updated).not.toContain(`G${MAX_RECENT_SEARCHES - 1}`)
  })
})
