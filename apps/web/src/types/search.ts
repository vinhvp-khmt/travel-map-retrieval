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
  /** null khi request search không kèm GPS (backend không tính được khoảng cách). */
  distanceMeters: number | null
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

/** Một mục "tìm kiếm gần đây" — khớp SearchHistoryItem phía backend. */
export type SearchHistoryItem = {
  id: string
  query: string
  latitude: number
  longitude: number
  radiusKm: number
  resultCount: number
  createdAt: string
}

/** Một POI đã xem — khớp ViewedPoiItem phía backend. */
export type ViewedPoiItem = {
  poiId: string
  name: string
  category: string
  address: string
  latitude: number
  longitude: number
  avgRating: number
  ratingCount: number
  viewedAt: string
}
