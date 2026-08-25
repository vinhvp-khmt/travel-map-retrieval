export type ScoreDetail = {
  bm25: number
  spatial: number
  temporal: number
  rating: number
  finalScore: number
}

export type SearchResult = {
  poiId: string
  name: string
  category: string
  address: string
  latitude: number
  longitude: number
  distanceMeters: number
  open: boolean
  source?: 'local' | 'geoapify'
  scoreDetail: ScoreDetail
}

export type SearchResponse = {
  normalizedQuery: string
  page: number
  size: number
  total: number
  results: SearchResult[]
  suggestion: string | null
}

export type SearchInput = {
  query: string
  latitude: number
  longitude: number
  radiusKm: number
  visitAt?: string
}

export type UserLocation = {
  latitude: number
  longitude: number
  accuracyMeters?: number
}
