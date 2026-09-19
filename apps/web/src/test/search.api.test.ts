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

  it('omits latitude/longitude entirely when the caller has no GPS (không bắt buộc phải có vị trí)', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ normalizedQuery: 'cafe', page: 0, size: 20, total: 0, results: [], suggestion: null }),
    })
    vi.stubGlobal('fetch', fetch)

    await searchPois({ query: 'cafe', latitude: Number.NaN, longitude: Number.NaN, radiusKm: 2 })

    const url = new URL(fetch.mock.calls[0][0], 'http://localhost')
    expect(url.searchParams.has('latitude')).toBe(false)
    expect(url.searchParams.has('longitude')).toBe(false)
  })

  it('attaches the Authorization header only when a token is given (để backend ghi được search history)', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ normalizedQuery: 'cafe', page: 0, size: 20, total: 0, results: [], suggestion: null }),
    })
    vi.stubGlobal('fetch', fetch)

    await searchPois({ query: 'cafe', latitude: 10.77, longitude: 106.70, radiusKm: 2 }, { token: 'my-token' })
    const [, initWithToken] = fetch.mock.calls[0]
    expect(new Headers(initWithToken?.headers).get('Authorization')).toBe('Bearer my-token')

    await searchPois({ query: 'cafe', latitude: 10.77, longitude: 106.70, radiusKm: 2 })
    const [, initWithoutToken] = fetch.mock.calls[1]
    expect(initWithoutToken?.headers).toBeUndefined()
  })
})
