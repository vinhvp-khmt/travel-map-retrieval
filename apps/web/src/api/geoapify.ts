import { GEOAPIFY_API_KEY } from './config'
import { distanceMeters } from '../lib/geo'
import type { SearchInput } from '../types/search'

type GeoapifyFeature = {
  properties?: {
    name?: string
    formatted?: string
    address_line1?: string
    address_line2?: string
    categories?: string[]
    category?: string
    distance?: number
    lat?: number
    lon?: number
    place_id?: string
    result_type?: string
  }
  geometry?: {
    coordinates?: [number, number]
  }
}

type GeoapifyResponse = {
  features?: GeoapifyFeature[]
}

export type AddressSuggestion = {
  placeId: string
  name: string
  address: string
  latitude: number
  longitude: number
  distanceMeters: number
}


function toAddressSuggestion(feature: GeoapifyFeature, input: SearchInput): AddressSuggestion | undefined {
  const [longitude, latitude] = feature.geometry?.coordinates ?? [feature.properties?.lon, feature.properties?.lat]
  if (typeof latitude !== 'number' || typeof longitude !== 'number') return undefined
  const name = feature.properties?.name
    ?? feature.properties?.address_line1
    ?? feature.properties?.formatted
    ?? 'Geoapify place'
  const distance = feature.properties?.distance ?? distanceMeters(input, { latitude, longitude })
  if (distance > input.radiusKm * 1000) return undefined
  return {
    placeId: feature.properties?.place_id ?? `${latitude}:${longitude}:${name}`,
    name,
    address: feature.properties?.formatted ?? feature.properties?.address_line2 ?? 'Địa điểm từ Geoapify',
    latitude,
    longitude,
    distanceMeters: distance,
  }
}

export async function geocodeAddresses(input: SearchInput, signal?: AbortSignal): Promise<AddressSuggestion[]> {
  if (!GEOAPIFY_API_KEY || !input.query.trim()) return []
  const radiusMeters = Math.round(input.radiusKm * 1000)
  const params = new URLSearchParams({
    text: input.query.trim(),
    lat: String(input.latitude),
    lon: String(input.longitude),
    bias: `proximity:${input.longitude},${input.latitude}`,
    filter: `circle:${input.longitude},${input.latitude},${radiusMeters}`,
    limit: '8',
    apiKey: GEOAPIFY_API_KEY,
  })
  const response = await fetch(`https://api.geoapify.com/v1/geocode/search?${params}`, { signal })
  if (!response.ok) throw new Error(`Geoapify search failed (${response.status})`)
  const data = await response.json() as GeoapifyResponse
  return (data.features ?? [])
    .map((feature) => toAddressSuggestion(feature, input))
    .filter((result): result is AddressSuggestion => Boolean(result))
    .sort((left, right) => left.distanceMeters - right.distanceMeters)
}
