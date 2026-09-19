import { API_BASE_URL } from './config'
import type { SearchHistoryItem, ViewedPoiItem } from '../types/search'

async function request<T>(path: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, ...(init?.headers ?? {}) },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(String(error?.message ?? `Request failed (${response.status})`))
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

/** Phase 6: "Tìm kiếm gần đây" — mới nhất trước. */
export async function fetchSearchHistory(token: string, limit = 8): Promise<SearchHistoryItem[]> {
  return request(`/api/v1/search/history?limit=${limit}`, token)
}

export async function deleteSearchHistoryItem(id: string, token: string): Promise<void> {
  await request(`/api/v1/search/history/${id}`, token, { method: 'DELETE' })
}

export async function clearSearchHistory(token: string): Promise<void> {
  await request('/api/v1/search/history', token, { method: 'DELETE' })
}

/** Phase 7: "Địa điểm đã xem" — mới nhất trước. */
export async function fetchViewedPois(token: string, limit = 8): Promise<ViewedPoiItem[]> {
  return request(`/api/v1/users/me/viewed-pois?limit=${limit}`, token)
}

/** Ghi nhận user vừa mở chi tiết một POI. Gọi fire-and-forget, lỗi không nên làm gián đoạn UI. */
export async function recordPoiView(poiId: string, token: string): Promise<void> {
  await request(`/api/v1/pois/${poiId}/view`, token, { method: 'POST' })
}

export async function removeViewedPoi(poiId: string, token: string): Promise<void> {
  await request(`/api/v1/users/me/viewed-pois/${poiId}`, token, { method: 'DELETE' })
}
