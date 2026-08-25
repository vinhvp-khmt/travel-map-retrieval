import type { SearchResult } from '../types/search'
import { ScoreBreakdown } from './ScoreBreakdown'
import { useMemo, useState } from 'react'
import { confirmMockPayment, createExternalBooking, createPaymentSession, type BookingResponse, type PaymentSessionResponse } from '../api/booking'

function defaultVisitAt() {
  const next = new Date(Date.now() + 60 * 60 * 1000)
  next.setMinutes(0, 0, 0)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}T${pad(next.getHours())}:${pad(next.getMinutes())}`
}

function money(amount: number, currency: string) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(amount)
}

export function PoiDetail({ poi, token, onClose }: { poi: SearchResult; token?: string; onClose: () => void }) {
  const [visitAt, setVisitAt] = useState(defaultVisitAt)
  const [partySize, setPartySize] = useState(2)
  const [notes, setNotes] = useState('')
  const [booking, setBooking] = useState<BookingResponse>()
  const [payment, setPayment] = useState<PaymentSessionResponse>()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()
  const canBook = useMemo(() => Boolean(token && visitAt && partySize > 0), [token, visitAt, partySize])

  async function book() {
    if (!token) return
    setBusy(true); setError(undefined); setPayment(undefined)
    try {
      const nextBooking = await createExternalBooking(poi, { visitAt, partySize, notes }, token)
      setBooking(nextBooking)
      const nextPayment = await createPaymentSession(nextBooking.bookingId, token)
      setPayment(nextPayment)
    } catch (reason) {
      setError((reason as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function confirmPayment() {
    if (!token || !payment) return
    setBusy(true); setError(undefined)
    try {
      const paid = await confirmMockPayment(payment.paymentId, token)
      setPayment(paid)
      setBooking((current) => current ? { ...current, status: 'CONFIRMED', holdExpiresAt: null } : current)
    } catch (reason) {
      setError((reason as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <aside className="poi-detail" aria-label={`Chi tiết ${poi.name}`}>
      <button className="close-button" onClick={onClose} aria-label="Đóng chi tiết">×</button>
      <p className="eyebrow">{poi.category} · Geoapify</p>
      <h2>{poi.name}</h2>
      <p>{poi.address}</p>
      <div className="poi-facts">
        <span>{Math.round(poi.distanceMeters)} m</span>
        <span className={poi.open ? 'open' : 'closed'}>{poi.open ? 'Đang mở cửa' : 'Đang đóng cửa'}</span>
      </div>
      <ScoreBreakdown score={poi.scoreDetail} />
      <section className="booking-box" aria-label="Đặt chỗ">
        <h3>Đặt chỗ nhanh</h3>
        <label>Thời gian đến
          <input type="datetime-local" value={visitAt} onChange={(event) => setVisitAt(event.target.value)} />
        </label>
        <label>Số người
          <input type="number" min="1" max="20" value={partySize} onChange={(event) => setPartySize(Number(event.target.value))} />
        </label>
        <label>Ghi chú
          <textarea maxLength={1000} placeholder="Ví dụ: bàn gần cửa sổ" value={notes} onChange={(event) => setNotes(event.target.value)} />
        </label>
        <button className="booking-button" disabled={!canBook || busy} onClick={book}>{busy ? 'Đang xử lý...' : 'Tạo booking + payment'}</button>
        {error && <p className="booking-error" role="alert">{error}</p>}
        {booking && <div className="booking-summary">
          <p><strong>Booking:</strong> {booking.status}</p>
          <p><strong>Giữ chỗ:</strong> {booking.holdExpiresAt ? new Date(booking.holdExpiresAt).toLocaleTimeString('vi-VN') : 'Đã xác nhận'}</p>
          <p><strong>Deposit:</strong> {money(Number(booking.depositAmount), booking.currency)}</p>
        </div>}
        {payment && <div className="booking-summary payment-summary">
          <p><strong>Payment:</strong> {payment.status} · {payment.gateway}</p>
          <p>{money(Number(payment.amount), payment.currency)}</p>
          {payment.status !== 'PAID' && <button className="booking-button secondary" disabled={busy} onClick={confirmPayment}>Xác nhận thanh toán demo</button>}
        </div>}
      </section>
    </aside>
  )
}
