export type ScoreDetail = {
  bm25: number
  spatial: number
  temporal: number
  rating: number
  finalScore: number
  rankingProfile?: string
  rawBm25?: number
  distanceMeters?: number
  averageRating?: number
  ratingCount?: number
  bm25Weight?: number
  spatialWeight?: number
  temporalWeight?: number
  ratingWeight?: number
  bm25Contribution?: number
  spatialContribution?: number
  temporalContribution?: number
  ratingContribution?: number
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
  scoreDetail: ScoreDetail
}

export type SearchResponse = {
  normalizedQuery: string
  page: number
  size: number
  total: number
  results: SearchResult[]
  suggestion: string | null
  rankingProfile?: string
}

export type CoffeeSpace = 'indoor' | 'outdoor' | 'garden' | 'rooftop'
export type CoffeePurpose = 'work' | 'study' | 'date' | 'checkin'

export type CoffeeFilters = {
  priceMin: number
  priceMax: number
  priceLimit: number
  spaces: CoffeeSpace[]
  purposes: CoffeePurpose[]
}

export type SearchInput = {
  query: string
  latitude: number
  longitude: number
  radiusKm: number
  visitAt?: string
  filters?: CoffeeFilters
}

export type UserLocation = {
  latitude: number
  longitude: number
  accuracyMeters?: number
}
