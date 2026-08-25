import type { SearchInput, SearchResponse } from '../types/search'
import { API_BASE_URL } from './config'

export async function searchPois(input: SearchInput, signal?: AbortSignal): Promise<SearchResponse> {
  const params = new URLSearchParams({
    q: input.query.trim(),
    latitude: String(input.latitude),
    longitude: String(input.longitude),
    radiusKm: String(input.radiusKm),
  })
  if (input.visitAt) params.set('visitAt', new Date(input.visitAt).toISOString())
  const response = await fetch(`${API_BASE_URL}/api/v1/search?${params}`, { signal })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(error?.message ?? `Search failed (${response.status})`)
  }
  return response.json()
}
