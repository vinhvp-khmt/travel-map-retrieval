import type { SearchInput, SearchResponse } from '../types/search'
import { API_BASE_URL } from './config'

export type SearchOptions = {
  signal?: AbortSignal
  /** Kèm token thì backend mới ghi được vào "Tìm kiếm gần đây" của đúng user đó. */
  token?: string
}

export async function searchPois(input: SearchInput, options?: SearchOptions): Promise<SearchResponse> {
  const params = new URLSearchParams({
    q: input.query.trim(),
    radiusKm: String(input.radiusKm),
  })
  // GPS là tuỳ chọn (backend chấp nhận thiếu cả hai) — chỉ gửi khi thật sự là số hợp lệ,
  // tránh gửi literal "NaN" khi user chưa từng đặt vị trí.
  if (Number.isFinite(input.latitude)) params.set('latitude', String(input.latitude))
  if (Number.isFinite(input.longitude)) params.set('longitude', String(input.longitude))
  if (input.visitAt) params.set('visitAt', new Date(input.visitAt).toISOString())
  const response = await fetch(`${API_BASE_URL}/api/v1/search?${params}`, {
    signal: options?.signal,
    headers: options?.token ? { Authorization: `Bearer ${options.token}` } : undefined,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(error?.message ?? `Search failed (${response.status})`)
  }
  return response.json()
}
