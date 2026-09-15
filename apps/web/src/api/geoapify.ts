import { GEOAPIFY_API_KEY } from './config'
import type { SearchInput, SearchResult } from '../types/search'

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

const COFFEE_CATEGORIES = ['catering.cafe']

const CATEGORY_KEYWORDS: Array<{ matches: string[]; categories: string[] }> = [
  {
    matches: [
      'cafe', 'coffee', 'ca phe', 'cà phê', 'espresso', 'latte', 'cold brew',
      'bánh', 'banh', 'bakery', 'cake', 'croissant', 'sân vườn', 'san vuon',
      'garden', 'thú cưng', 'thu cung', 'pet', 'làm việc', 'lam viec',
      'work', 'wifi', 'rooftop', 'view', 'yên tĩnh', 'yen tinh',
    ],
    categories: COFFEE_CATEGORIES,
  },
]

function normalizeText(value: string) {
  return value.trim().toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

function categoriesForQuery(query: string) {
  const normalized = normalizeText(query)
  const matched = CATEGORY_KEYWORDS.find((entry) => entry.matches.some((keyword) => normalized.includes(normalizeText(keyword))))
  return matched?.categories ?? COFFEE_CATEGORIES
}

function distanceMeters(from: Pick<SearchInput, 'latitude' | 'longitude'>, to: { latitude: number; longitude: number }) {
  const earthRadiusMeters = 6_371_000
  const toRadians = (value: number) => value * Math.PI / 180
  const dLat = toRadians(to.latitude - from.latitude)
  const dLng = toRadians(to.longitude - from.longitude)
  const lat1 = toRadians(from.latitude)
  const lat2 = toRadians(to.latitude)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * earthRadiusMeters * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function categoryLabel(feature: GeoapifyFeature) {
  const type = feature.properties?.result_type ?? feature.properties?.category ?? feature.properties?.categories?.[0]
  if (!type) return 'Geoapify'
  return type.replaceAll('_', ' ')
    .replaceAll('.', ' ')
}

function queryWithFilters(input: SearchInput) {
  const spaceText: Record<string, string> = {
    indoor: 'indoor',
    outdoor: 'outdoor',
    garden: 'garden sân vườn',
    rooftop: 'rooftop',
  }
  const purposeText: Record<string, string> = {
    work: 'quiet wifi work làm việc',
    study: 'quiet study học tập',
    date: 'cozy date hẹn hò',
    checkin: 'beautiful checkin view',
  }
  const filters = input.filters
  const additions = [
    ...(filters?.spaces ?? []).map((item) => spaceText[item]),
    ...(filters?.purposes ?? []).map((item) => purposeText[item]),
  ].filter(Boolean)
  return ['coffee cafe', input.query.trim(), ...additions].filter(Boolean).join(' ')
}

function toSearchResult(feature: GeoapifyFeature, input: SearchInput): SearchResult | undefined {
  const [longitude, latitude] = feature.geometry?.coordinates ?? [feature.properties?.lon, feature.properties?.lat]
  if (typeof latitude !== 'number' || typeof longitude !== 'number') return undefined
  const name = feature.properties?.name
    ?? feature.properties?.address_line1
    ?? feature.properties?.formatted
    ?? 'Geoapify place'
  const distance = feature.properties?.distance ?? distanceMeters(input, { latitude, longitude })
  if (distance > input.radiusKm * 1000) return undefined
  return {
    poiId: `geoapify:${feature.properties?.place_id ?? `${latitude}:${longitude}:${name}`}`,
    name,
    category: categoryLabel(feature),
    address: feature.properties?.formatted ?? feature.properties?.address_line2 ?? 'Địa điểm từ Geoapify',
    latitude,
    longitude,
    distanceMeters: distance,
    open: true,
    source: 'geoapify',
    scoreDetail: { bm25: 0.72, spatial: Math.max(0, 1 - distance / (input.radiusKm * 1000)), temporal: 1, rating: 0.5, finalScore: 0.72 },
  }
}

export async function geocodePlaces(input: SearchInput, signal?: AbortSignal): Promise<SearchResult[]> {
  if (!GEOAPIFY_API_KEY || !input.query.trim()) return []
  const radiusMeters = Math.round(input.radiusKm * 1000)
  const params = new URLSearchParams({
    text: queryWithFilters(input),
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
    .map((feature) => toSearchResult(feature, input))
    .filter((result): result is SearchResult => Boolean(result))
    .sort((left, right) => left.distanceMeters - right.distanceMeters)
}

export async function placesByCategory(input: SearchInput, signal?: AbortSignal): Promise<SearchResult[]> {
  const categories = categoriesForQuery(input.query)
  if (!GEOAPIFY_API_KEY || !categories) return []
  const radiusMeters = Math.round(input.radiusKm * 1000)
  const params = new URLSearchParams({
    categories: categories.join(','),
    filter: `circle:${input.longitude},${input.latitude},${radiusMeters}`,
    bias: `proximity:${input.longitude},${input.latitude}`,
    limit: '20',
    apiKey: GEOAPIFY_API_KEY,
  })
  const response = await fetch(`https://api.geoapify.com/v2/places?${params}`, { signal })
  if (!response.ok) throw new Error(`Geoapify places failed (${response.status})`)
  const data = await response.json() as GeoapifyResponse
  return (data.features ?? [])
    .map((feature) => toSearchResult(feature, input))
    .filter((result): result is SearchResult => Boolean(result))
    .sort((left, right) => left.distanceMeters - right.distanceMeters)
}

export async function searchGeoapify(input: SearchInput, signal?: AbortSignal): Promise<SearchResult[]> {
  const [places, geocoded] = await Promise.all([
    placesByCategory(input, signal).catch(() => []),
    geocodePlaces(input, signal).catch(() => []),
  ])
  const seen = new Set<string>()
  return [...places, ...geocoded].filter((item) => {
    const key = `${item.name.toLowerCase()}:${item.latitude.toFixed(5)}:${item.longitude.toFixed(5)}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  }).sort((left, right) => left.distanceMeters - right.distanceMeters)
}
