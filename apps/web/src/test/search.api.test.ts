import { afterEach, describe, expect, it, vi } from 'vitest'
import { searchPois } from '../api/search'

describe('backend POI search client', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('forwards the fixed retrieval context to the backend search endpoint', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        normalizedQuery: 'ca phe', page: 0, size: 20, total: 0, results: [], suggestion: null,
      }),
    })
    vi.stubGlobal('fetch', fetch)

    await searchPois({
      query: 'cà phê',
      latitude: 10.7769,
      longitude: 106.7009,
      radiusKm: 4.5,
      visitAt: '2026-09-18T20:00:00+07:00',
    })

    const url = new URL(fetch.mock.calls[0][0], 'http://localhost')
    expect(url.pathname).toBe('/api/v1/search')
    expect(url.searchParams.get('q')).toBe('cà phê')
    expect(url.searchParams.get('latitude')).toBe('10.7769')
    expect(url.searchParams.get('longitude')).toBe('106.7009')
    expect(url.searchParams.get('radiusKm')).toBe('4.5')
    expect(url.searchParams.get('visitAt')).toBe('2026-09-18T13:00:00.000Z')
    expect(url.hostname).not.toContain('geoapify')
  })
})
