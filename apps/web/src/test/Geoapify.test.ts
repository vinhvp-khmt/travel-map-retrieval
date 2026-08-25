import { afterEach, describe, expect, it, vi } from 'vitest'
import { geocodePlaces, placesByCategory, searchGeoapify } from '../api/geoapify'

describe('Geoapify geocoding', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('maps geocoding features into search results', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        features: [{
          properties: {
            place_id: 'place-1',
            name: 'Ben Thanh Market',
            formatted: 'Ben Thanh, District 1, Ho Chi Minh City',
            result_type: 'amenity',
            distance: 120,
          },
          geometry: { coordinates: [106.6983, 10.7725] },
        }],
      }),
    })
    vi.stubGlobal('fetch', fetch)
    const results = await geocodePlaces({ query: 'Ben Thanh', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(fetch.mock.calls[0][0]).toContain('filter=circle%3A106.7009%2C10.7769%2C2000')
    expect(results[0]).toMatchObject({
      poiId: 'geoapify:place-1',
      name: 'Ben Thanh Market',
      source: 'geoapify',
      distanceMeters: 120,
    })
  })

  it('drops geocoding results outside the requested radius', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        features: [{
          properties: {
            place_id: 'foreign-cafe',
            name: 'Far Away Cafe',
            formatted: 'Another country',
            result_type: 'amenity',
          },
          geometry: { coordinates: [-0.1586, 51.5237] },
        }],
      }),
    }))
    const results = await geocodePlaces({ query: 'cafe', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(results).toEqual([])
  })

  it('searches Geoapify Places by category around the user location', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        features: [{
          properties: {
            place_id: 'supermarket-1',
            name: 'Local Supermarket',
            formatted: '1 Market Street',
            categories: ['commercial.supermarket'],
            distance: 240,
          },
          geometry: { coordinates: [106.701, 10.777] },
        }],
      }),
    })
    vi.stubGlobal('fetch', fetch)
    const results = await placesByCategory({ query: 'supermarket', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/v2/places?'), expect.any(Object))
    expect(fetch.mock.calls[0][0]).toContain('categories=commercial.supermarket')
    expect(fetch.mock.calls[0][0]).toContain('filter=circle%3A106.7009%2C10.7769%2C2000')
    expect(results[0]).toMatchObject({
      name: 'Local Supermarket',
      category: 'commercial supermarket',
      source: 'geoapify',
    })
  })

  it('combines category places and geocoding results', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ features: [{ properties: { place_id: 'place-1', name: 'Coffee One', formatted: '1 Nguyen Hue', categories: ['catering.cafe'] }, geometry: { coordinates: [106.7, 10.77] } }] }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ features: [{ properties: { place_id: 'place-2', name: 'Coffee Address', formatted: '2 Nguyen Hue', result_type: 'amenity' }, geometry: { coordinates: [106.71, 10.78] } }] }),
      }))
    const results = await searchGeoapify({ query: 'cafe', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(results.map((item) => item.name)).toEqual(['Coffee One', 'Coffee Address'])
  })
})
