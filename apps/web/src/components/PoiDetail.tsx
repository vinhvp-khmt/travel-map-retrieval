import type { SearchResult } from '../types/search'
import { ScoreBreakdown } from './ScoreBreakdown'
import { useMemo, useState } from 'react'
import { CalendarClock, Coffee, CreditCard, MapPin, Users } from 'lucide-react'
import { confirmMockPayment, createExternalBooking, createPaymentSession, type BookingResponse, type PaymentSessionResponse } from '../api/booking'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Spinner } from '@/components/ui/spinner'
import { Textarea } from '@/components/ui/textarea'

function defaultVisitAt() {
  const next = new Date(Date.now() + 60 * 60 * 1000)
  next.setMinutes(0, 0, 0)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}T${pad(next.getHours())}:${pad(next.getMinutes())}`
}

function money(amount: number, currency: string) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(amount)
}

/**
 * POI mở từ một lượt search thật có đủ scoreDetail/distanceMeters/open. POI mở từ
 * "Địa điểm đã xem" (lịch sử) thì không — dữ liệu đó chỉ là snapshot cũ, không phải kết quả
 * ranking sống, nên không có căn cứ để hiện điểm số hay trạng thái mở cửa. Nới ba trường này
 * thành optional để tái dùng đúng một component cho cả hai nguồn, thay vì tạo bản sao.
 */
export type PoiDetailSubject = Pick<SearchResult, 'poiId' | 'name' | 'category' | 'address' | 'latitude' | 'longitude'>
  & Partial<Pick<SearchResult, 'distanceMeters' | 'open' | 'scoreDetail'>>

export function PoiDetail({ poi, token, onClose }: { poi: PoiDetailSubject; token?: string; onClose: () => void }) {
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
    <Sheet open onOpenChange={(open) => { if (!open) onClose() }}>
      <SheetContent className="overflow-y-auto" aria-label={`Chi tiết ${poi.name}`}>
        <SheetHeader>
          <p className="eyebrow">{poi.category} · Coffee place</p>
          <SheetTitle className="pr-6">{poi.name}</SheetTitle>
          <SheetDescription className="flex gap-2"><MapPin className="mt-0.5 h-4 w-4 shrink-0" />{poi.address}</SheetDescription>
        </SheetHeader>
        <div className="grid gap-4 px-6 pb-6">
      {(poi.distanceMeters !== undefined || poi.open !== undefined) && <div className="flex flex-wrap gap-2">
        {poi.distanceMeters !== undefined && <Badge variant="secondary">{Math.round(poi.distanceMeters)} m</Badge>}
        {poi.open !== undefined && <Badge variant={poi.open ? 'default' : 'destructive'}>{poi.open ? 'Đang mở cửa' : 'Đang đóng cửa'}</Badge>}
      </div>}
      {poi.scoreDetail && <ScoreBreakdown score={poi.scoreDetail} />}
      <Separator />
      <section className="grid gap-3" aria-label="Đặt chỗ">
        <div>
          <h3 className="font-serif text-2xl">Giữ bàn nhanh</h3>
          <p className="text-sm text-muted-foreground">CoffeeScope sẽ giữ bàn trong 10 phút để bạn hoàn tất thanh toán demo.</p>
        </div>
        <Label className="grid gap-2 normal-case tracking-normal text-foreground"><span className="flex items-center gap-2"><CalendarClock className="h-4 w-4" />Thời gian đến</span>
          <Input type="datetime-local" value={visitAt} onChange={(event) => setVisitAt(event.target.value)} />
        </Label>
        <Label className="grid gap-2 normal-case tracking-normal text-foreground"><span className="flex items-center gap-2"><Users className="h-4 w-4" />Số người</span>
          <Input type="number" min="1" max="20" value={partySize} onChange={(event) => setPartySize(Number(event.target.value))} />
        </Label>
        <Label className="grid gap-2 normal-case tracking-normal text-foreground">Ghi chú
          <Textarea maxLength={1000} placeholder="Ví dụ: bàn gần cửa sổ, ổ cắm, khu yên tĩnh" value={notes} onChange={(event) => setNotes(event.target.value)} />
        </Label>
        <Button loading={busy && !payment} disabled={!canBook || busy} onClick={book}>{busy && !payment ? 'Đang giữ bàn...' : <><Coffee className="h-4 w-4" />Giữ bàn + payment</>}</Button>
        {busy && !payment && <Spinner label="Đang giữ bàn và tạo phiên thanh toán..." />}
        {error && <Alert className="border-destructive/40 bg-destructive/10"><AlertDescription className="text-destructive">{error}</AlertDescription></Alert>}
        {booking && <Card className="bg-secondary/50 shadow-none">
          <CardContent className="grid gap-1 p-4 text-sm">
            <p><strong>Booking:</strong> {booking.status}</p>
            <p><strong>Giữ chỗ:</strong> {booking.holdExpiresAt ? new Date(booking.holdExpiresAt).toLocaleTimeString('vi-VN') : 'Đã xác nhận'}</p>
            <p><strong>Deposit:</strong> {money(Number(booking.depositAmount), booking.currency)}</p>
          </CardContent>
        </Card>}
        {payment && <Card className="bg-primary/5 shadow-none">
          <CardContent className="grid gap-3 p-4 text-sm">
            <p className="flex items-center gap-2"><CreditCard className="h-4 w-4" /><strong>Payment:</strong> {payment.status} · {payment.gateway}</p>
            <p>{money(Number(payment.amount), payment.currency)}</p>
            {payment.status !== 'PAID' && <Button variant="outline" loading={busy} disabled={busy} onClick={confirmPayment}>Xác nhận thanh toán demo</Button>}
            {busy && <Spinner label="Đang xác nhận thanh toán demo..." />}
          </CardContent>
        </Card>}
      </section>
        </div>
      </SheetContent>
    </Sheet>
  )
}
