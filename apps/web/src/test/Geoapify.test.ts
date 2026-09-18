import { afterEach, describe, expect, it, vi } from 'vitest'
import { geocodeAddresses } from '../api/geoapify'

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
    const results = await geocodeAddresses({ query: 'Ben Thanh', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(fetch.mock.calls[0][0]).toContain('filter=circle%3A106.7009%2C10.7769%2C2000')
    expect(results[0]).toMatchObject({
      placeId: 'place-1',
      name: 'Ben Thanh Market',
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
    const results = await geocodeAddresses({ query: 'cafe', latitude: 10.7769, longitude: 106.7009, radiusKm: 2 })
    expect(results).toEqual([])
  })
})
