import { API_BASE_URL } from './config'
import type { SearchResult } from '../types/search'

type BookablePoi = Pick<SearchResult, 'poiId' | 'name' | 'category' | 'address' | 'latitude' | 'longitude'>

export type BookingResponse = {
  bookingId: string
  poiId: string
  poiName: string
  visitAt: string
  slotEndAt: string
  partySize: number
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED'
  holdExpiresAt: string | null
  depositAmount: number
  currency: string
  paymentRequired: boolean
}

export type PaymentSessionResponse = {
  paymentId: string
  bookingId: string
  gateway: string
  status: 'CREATED' | 'PAID' | 'FAILED' | 'REFUNDED'
  amount: number
  currency: string
  paymentUrl: string
}

export type BookingDraft = {
  visitAt: string
  partySize: number
  notes?: string
}

async function request<T>(path: string, token: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    const fieldMessage = error?.fields && Object.values(error.fields)[0]
    throw new Error(String(fieldMessage ?? error?.message ?? `Request failed (${response.status})`))
  }
  return response.json()
}

export async function createExternalBooking(poi: BookablePoi, draft: BookingDraft, token: string) {
  return request<BookingResponse>('/api/v1/bookings/external', token, {
    method: 'POST',
    body: JSON.stringify({
      externalId: poi.poiId.slice(0, 160),
      name: poi.name,
      category: poi.category,
      address: poi.address,
      latitude: poi.latitude,
      longitude: poi.longitude,
      visitAt: new Date(draft.visitAt).toISOString(),
      partySize: draft.partySize,
      notes: draft.notes?.trim() || undefined,
    }),
  })
}

export async function createPaymentSession(bookingId: string, token: string) {
  return request<PaymentSessionResponse>(`/api/v1/bookings/${bookingId}/payment-sessions`, token, {
    method: 'POST',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  })
}

export async function confirmMockPayment(paymentId: string, token: string) {
  return request<PaymentSessionResponse>(`/api/v1/payments/${paymentId}/mock-confirm`, token, { method: 'POST' })
}
