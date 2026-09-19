import { useEffect, useState, type FormEvent } from 'react'
import { AlertCircle, Crosshair, MapPin, Search, SlidersHorizontal, X } from 'lucide-react'
import type { CoffeeFilters, CoffeePurpose, CoffeeSpace, SearchInput, UserLocation } from '../types/search'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { useMobile } from '@/hooks/useMobile'
import { cn } from '@/lib/utils'

export function validateSearchInput(input: SearchInput): Record<string, string> {
  const errors: Record<string, string> = {}
  const queryLength = input.query.trim().length
  if (queryLength < 1 || queryLength > 200) errors.query = 'Nhập từ khóa từ 1 đến 200 ký tự.'
  if (!Number.isFinite(input.latitude) || input.latitude < -90 || input.latitude > 90) errors.latitude = 'Hãy bấm “Dùng vị trí của tôi” trước khi tìm.'
  if (!Number.isFinite(input.longitude) || input.longitude < -180 || input.longitude > 180) errors.longitude = 'Hãy bấm “Dùng vị trí của tôi” trước khi tìm.'
  if (input.radiusKm < 0.1 || input.radiusKm > 10) errors.radiusKm = 'Thiết lập tìm kiếm không hợp lệ.'
  if (input.visitAt && new Date(input.visitAt).getTime() < Date.now() - 60_000) {
    errors.visitAt = 'Thời gian ghé thăm phải từ hiện tại trở đi.'
  }
  return errors
}

type Props = { loading: boolean; onSearch: (input: SearchInput) => void; onLocationChange?: (location: UserLocation) => void }
type FormInput = {
  query: string
  latitude: string
  longitude: string
  radiusKm: number
  visitAt?: string
  filters: CoffeeFilters
}

const DEFAULT_FILTERS: CoffeeFilters = {
  priceMin: 0,
  priceMax: 1_000_000,
  priceLimit: 1_000_000,
  spaces: [],
  purposes: [],
}

const MIN_PRICE_LIMIT = 1_000
const MIN_RADIUS_KM = 0.1
const MAX_RADIUS_KM = 10
const DEFAULT_RADIUS_KM = 2

// Vị trí mẫu để test nhanh khi không đứng đúng khu vực có dữ liệu (toàn bộ POI seed nằm
// quanh Quận 1/3, bán kính tìm kiếm tối đa 10km — GPS thật ở xa trung tâm, ví dụ Cần Giờ,
// sẽ luôn ra 0 kết quả). Toạ độ là tâm gần đúng của mỗi quận.
const PRESET_LOCATIONS: ReadonlyArray<{ label: string; latitude: number; longitude: number }> = [
  { label: 'Quận 7', latitude: 10.729, longitude: 106.7019 },
  { label: 'Quận 2', latitude: 10.7929, longitude: 106.7419 },
  { label: 'Quận 3', latitude: 10.7822, longitude: 106.6889 },
]

const spaceOptions: Array<{ value: CoffeeSpace; label: string }> = [
  { value: 'indoor', label: 'Indoor' },
  { value: 'outdoor', label: 'Outdoor' },
  { value: 'garden', label: 'Sân vườn' },
  { value: 'rooftop', label: 'Rooftop' },
]

const purposeOptions: Array<{ value: CoffeePurpose; label: string }> = [
  { value: 'work', label: 'Làm việc' },
  { value: 'study', label: 'Học tập' },
  { value: 'date', label: 'Hẹn hò' },
  { value: 'checkin', label: 'Checkin' },
]

function parseList<T extends string>(value: string | null, options: readonly T[]) {
  if (!value) return []
  const allowed = new Set(options)
  return value.split(',').filter((item): item is T => allowed.has(item as T))
}

function readUrlInput(): FormInput {
  const params = typeof window === 'undefined' ? new URLSearchParams() : new URLSearchParams(window.location.search)
  const legacyPrice = params.get('price')
  const rawLimit = params.has('priceLimit') ? Number(params.get('priceLimit')) : Number.NaN
  const rawMax = Number(params.get('priceMax') ?? legacyPrice?.split('-').at(-1))
  const rawRadius = params.has('radiusKm') ? Number(params.get('radiusKm')) : Number.NaN
  const priceLimit = Number.isFinite(rawLimit) ? Math.max(MIN_PRICE_LIMIT, rawLimit) : DEFAULT_FILTERS.priceLimit
  const radiusKm = Number.isFinite(rawRadius) ? Math.min(MAX_RADIUS_KM, Math.max(MIN_RADIUS_KM, rawRadius)) : DEFAULT_RADIUS_KM
  return {
    query: params.get('q') ?? '',
    latitude: '',
    longitude: '',
    radiusKm,
    filters: {
      priceMin: DEFAULT_FILTERS.priceMin,
      priceMax: Math.min(priceLimit, Number.isFinite(rawMax) ? rawMax : DEFAULT_FILTERS.priceMax),
      priceLimit,
      spaces: parseList(params.get('space'), spaceOptions.map((item) => item.value)),
      purposes: parseList(params.get('purpose'), purposeOptions.map((item) => item.value)),
    },
  }
}

function updateUrl(input: FormInput) {
  if (typeof window === 'undefined') return
  const params = new URLSearchParams(window.location.search)
  if (input.query.trim()) params.set('q', input.query.trim())
  else params.delete('q')
  if (input.radiusKm !== DEFAULT_RADIUS_KM) params.set('radiusKm', String(input.radiusKm))
  else params.delete('radiusKm')

  const { priceMax, priceLimit, spaces, purposes } = input.filters
  if (priceMax !== DEFAULT_FILTERS.priceMax) params.set('priceMax', String(priceMax))
  else params.delete('priceMax')
  if (priceLimit !== DEFAULT_FILTERS.priceLimit) params.set('priceLimit', String(priceLimit))
  else params.delete('priceLimit')
  params.delete('price')
  if (spaces.length) params.set('space', spaces.join(','))
  else params.delete('space')
  if (purposes.length) params.set('purpose', purposes.join(','))
  else params.delete('purpose')

  const nextQuery = params.toString()
  window.history.replaceState(null, '', `${window.location.pathname}${nextQuery ? `?${nextQuery}` : ''}${window.location.hash}`)
}

function money(value: number) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value)
}

function formatRadius(value: number) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(value)
}

function cloneInput(input: FormInput): FormInput {
  return {
    ...input,
    filters: {
      ...input.filters,
      spaces: [...input.filters.spaces],
      purposes: [...input.filters.purposes],
    },
  }
}

function normalizeFilters(nextFilters: CoffeeFilters) {
  const priceLimit = Math.max(MIN_PRICE_LIMIT, nextFilters.priceLimit)
  return {
    ...nextFilters,
    priceMin: 0,
    priceLimit,
    priceMax: Math.min(priceLimit, Math.max(0, nextFilters.priceMax)),
  }
}

export function SearchForm({ loading, onSearch, onLocationChange }: Props) {
  const isMobile = useMobile()
  const [input, setInput] = useState<FormInput>(() => readUrlInput())
  const [draftInput, setDraftInput] = useState<FormInput>(() => cloneInput(input))
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [locating, setLocating] = useState(false)
  const [filterOpen, setFilterOpen] = useState(false)
  const [showSearchPrompt, setShowSearchPrompt] = useState(false)
  const [locationStatus, setLocationStatus] = useState('Chưa có vị trí. Hãy bấm “Dùng vị trí của tôi” trước khi tìm.')

  useEffect(() => updateUrl(input), [input])

  function toSearchInput(current: FormInput): SearchInput {
    const toCoordinate = (value: string) => value.trim() ? Number(value) : Number.NaN
    return {
      query: current.query,
      latitude: toCoordinate(current.latitude),
      longitude: toCoordinate(current.longitude),
      radiusKm: current.radiusKm,
      visitAt: current.visitAt,
      filters: current.filters,
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const searchInput = toSearchInput(input)
    const nextErrors = validateSearchInput(searchInput)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length === 0) {
      setShowSearchPrompt(false)
      onSearch(searchInput)
    }
  }

  function updateAppliedFilter(nextFilters: CoffeeFilters) {
    setShowSearchPrompt(true)
    setInput({ ...input, filters: normalizeFilters(nextFilters) })
  }

  function updateAppliedRadius(radiusKm: number) {
    setShowSearchPrompt(true)
    setInput({ ...input, radiusKm: Math.min(MAX_RADIUS_KM, Math.max(MIN_RADIUS_KM, radiusKm)) })
  }

  function updateDraftFilter(nextFilters: CoffeeFilters) {
    setDraftInput({ ...draftInput, filters: normalizeFilters(nextFilters) })
  }

  function updateDraftRadius(radiusKm: number) {
    setDraftInput({ ...draftInput, radiusKm: Math.min(MAX_RADIUS_KM, Math.max(MIN_RADIUS_KM, radiusKm)) })
  }

  function changeFilterOpen(open: boolean) {
    if (open) setDraftInput(cloneInput(input))
    setFilterOpen(open)
  }

  function applyFilters() {
    setInput(cloneInput(draftInput))
    setShowSearchPrompt(true)
    setFilterOpen(false)
  }

  function toggleFilter<T extends string>(items: T[], value: T) {
    return items.includes(value) ? items.filter((item) => item !== value) : [...items, value]
  }

  const activeTags = [
    ...(input.radiusKm !== DEFAULT_RADIUS_KM ? [{
      key: 'radius',
      label: `Bán kính ${formatRadius(input.radiusKm)} km`,
      clear: () => updateAppliedRadius(DEFAULT_RADIUS_KM),
    }] : []),
    ...(input.filters.priceMax !== DEFAULT_FILTERS.priceMax ? [{
      key: 'price',
      label: `Đến ${money(input.filters.priceMax)}đ`,
      clear: () => updateAppliedFilter({ ...input.filters, priceMax: DEFAULT_FILTERS.priceMax }),
    }] : []),
    ...spaceOptions
      .filter((option) => input.filters.spaces.includes(option.value))
      .map((option) => ({
        key: `space-${option.value}`,
        label: option.label,
        clear: () => updateAppliedFilter({ ...input.filters, spaces: input.filters.spaces.filter((item) => item !== option.value) }),
      })),
    ...purposeOptions
      .filter((option) => input.filters.purposes.includes(option.value))
      .map((option) => ({
        key: `purpose-${option.value}`,
        label: option.label,
        clear: () => updateAppliedFilter({ ...input.filters, purposes: input.filters.purposes.filter((item) => item !== option.value) }),
      })),
  ]

  function useCurrentLocation() {
    if (!navigator.geolocation) {
      setLocationStatus('Trình duyệt này chưa hỗ trợ lấy vị trí GPS.')
      return
    }
    setLocating(true)
    setLocationStatus('Đang xin quyền và lấy vị trí hiện tại...')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const nextLocation = {
          latitude: Number(position.coords.latitude.toFixed(6)),
          longitude: Number(position.coords.longitude.toFixed(6)),
          accuracyMeters: Math.round(position.coords.accuracy),
        }
        const nextInput = { ...input, latitude: String(nextLocation.latitude), longitude: String(nextLocation.longitude) }
        setInput(nextInput)
        setErrors({})
        onLocationChange?.(nextLocation)
        setLocationStatus(`Vị trí hiện tại đã sẵn sàng ± ${nextLocation.accuracyMeters} m.`)
        if (nextInput.query.trim()) {
          setShowSearchPrompt(false)
          onSearch(toSearchInput(nextInput))
        }
        setLocating(false)
      },
      (reason) => {
        setLocationStatus(reason.code === reason.PERMISSION_DENIED ? 'Bạn chưa cấp quyền vị trí cho trình duyệt.' : 'Không lấy được GPS, hãy thử lại.')
        setLocating(false)
      },
      { enableHighAccuracy: true, maximumAge: 60_000, timeout: 10_000 },
    )
  }

  function selectPresetLocation(preset: (typeof PRESET_LOCATIONS)[number]) {
    const nextLocation = { latitude: preset.latitude, longitude: preset.longitude }
    const nextInput = { ...input, latitude: String(preset.latitude), longitude: String(preset.longitude) }
    setInput(nextInput)
    setErrors({})
    onLocationChange?.(nextLocation)
    setLocationStatus(`Đang dùng vị trí mẫu: ${preset.label}.`)
    if (nextInput.query.trim()) {
      setShowSearchPrompt(false)
      onSearch(toSearchInput(nextInput))
    }
  }

  return (
    <Card className="overflow-hidden border-primary/10 bg-card/95 shadow-xl">
      <CardContent className="p-0">
    <form className={cn('grid gap-px bg-border', isMobile ? 'grid-cols-1' : 'lg:grid-cols-[minmax(260px,1fr)_auto_auto_auto]')} onSubmit={submit} aria-label="Tìm quán coffee">
      <div className="relative bg-card p-4">
        <Label htmlFor="query">Bạn muốn quán coffee kiểu nào?</Label>
        <Input id="query" value={input.query} placeholder="coffee bánh, sân vườn, thú cưng, làm việc, rooftop..."
          className={cn('mt-2 border-0 bg-transparent px-0 text-base font-semibold shadow-none focus-visible:ring-0', showSearchPrompt && 'pr-32')}
          onChange={(event) => setInput({ ...input, query: event.target.value })} />
        {errors.query && <span className="field-error">{errors.query}</span>}
        {showSearchPrompt && (
          <Button type="submit" size="sm" className="absolute bottom-3 right-3 z-10 h-8 rounded-full px-3 shadow-lg">
            <Search className="h-3.5 w-3.5" />
            Tìm kiếm ngay
          </Button>
        )}
      </div>
      <Button type="button" variant="outline" className={cn('h-full min-h-16 rounded-none border-0 bg-card px-5', isMobile && 'justify-start')} loading={locating} disabled={loading || locating} onClick={useCurrentLocation}>
        {locating ? <Crosshair className="h-4 w-4" /> : <MapPin className="h-4 w-4" />}
        {locating ? 'Đang lấy GPS...' : 'Dùng vị trí của tôi'}
      </Button>
      <Button type="button" variant="outline" className={cn('h-full min-h-16 rounded-none border-0 bg-card px-5', isMobile && 'justify-start')} onClick={() => changeFilterOpen(true)}>
        <SlidersHorizontal className="h-4 w-4" />
        Bộ lọc
      </Button>
      <Button className={cn('h-full min-h-16 rounded-none px-6', isMobile && 'justify-start')} loading={loading} disabled={loading}>
        {!loading && <Search className="h-4 w-4" />}
        {loading ? 'Đang tìm…' : 'Tìm quán'}
      </Button>
      <div className="col-span-full flex flex-wrap items-center gap-2 bg-card px-4 py-2" aria-label="Vị trí mẫu để test nhanh">
        <span className="text-xs text-muted-foreground">Hoặc test nhanh với vị trí mẫu:</span>
        {PRESET_LOCATIONS.map((preset) => (
          <Button key={preset.label} type="button" size="sm" variant="outline" className="h-7 rounded-full px-3 text-xs" disabled={loading} onClick={() => selectPresetLocation(preset)}>
            {preset.label}
          </Button>
        ))}
      </div>
      {activeTags.length > 0 && (
        <div className="col-span-full flex flex-wrap gap-2 bg-card px-4 py-3" aria-label="Bộ lọc đang chọn">
          {activeTags.map((tag) => (
            <Badge key={tag.key} variant="secondary" className="gap-1.5 pr-1">
              {tag.label}
              <button type="button" className="grid h-5 w-5 place-items-center rounded-full text-muted-foreground transition hover:bg-card hover:text-foreground" aria-label={`Bỏ ${tag.label}`} onClick={tag.clear}>
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
      <Alert className="col-span-full rounded-none border-0 bg-secondary/60" aria-live="polite">
        <AlertCircle className="mr-2 inline h-4 w-4 text-accent" aria-hidden="true" />
        <AlertDescription className="inline">{locationStatus}</AlertDescription>
      </Alert>
    </form>
    <Sheet open={filterOpen} onOpenChange={changeFilterOpen}>
      <SheetContent side="right" className="overflow-y-auto" aria-label="Bộ lọc quán coffee">
        <SheetHeader>
          <SheetTitle>Bộ lọc</SheetTitle>
          <SheetDescription>Chọn bán kính, giá, không gian và mục đích để tinh chỉnh kết quả coffee.</SheetDescription>
        </SheetHeader>
        <div className="grid gap-6 px-6 pb-6">
          <section className="grid gap-3" aria-label="Giá">
            <div className="flex items-end justify-between gap-3">
              <Label className="grid gap-1 normal-case tracking-normal text-foreground">
                <span className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Giá tối đa</span>
                <strong className="font-serif text-2xl text-primary">{money(draftInput.filters.priceMax)}đ</strong>
              </Label>
              <Label className="grid w-36 gap-1 normal-case tracking-normal text-foreground">
                <span className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Limit max</span>
                <Input type="number" min={MIN_PRICE_LIMIT} step="1000" value={draftInput.filters.priceLimit}
                  className="h-9 text-right"
                  onChange={(event) => updateDraftFilter({ ...draftInput.filters, priceLimit: Number(event.target.value) })} />
              </Label>
            </div>
            <input aria-label="Giá tối đa" className="w-full accent-accent" type="range" min="0" max={draftInput.filters.priceLimit} step="10000" value={draftInput.filters.priceMax}
              onChange={(event) => updateDraftFilter({ ...draftInput.filters, priceMax: Number(event.target.value) })} />
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>0đ</span>
              <span>{money(draftInput.filters.priceLimit)}đ</span>
            </div>
          </section>
          <section className="grid gap-3" aria-label="Bán kính tìm kiếm">
            <div className="flex items-end justify-between gap-3">
              <Label className="grid gap-1 normal-case tracking-normal text-foreground">
                <span className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Bán kính</span>
                <strong className="font-serif text-2xl text-primary">{formatRadius(draftInput.radiusKm)} km</strong>
              </Label>
              <Label className="grid w-28 gap-1 normal-case tracking-normal text-foreground">
                <span className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Km</span>
                <Input type="number" min={MIN_RADIUS_KM} max={MAX_RADIUS_KM} step="0.1" value={draftInput.radiusKm}
                  className="h-9 text-right"
                  onChange={(event) => {
                    const radiusKm = Number(event.target.value)
                    if (Number.isFinite(radiusKm)) updateDraftRadius(radiusKm)
                  }} />
              </Label>
            </div>
            <input aria-label="Bán kính tìm kiếm" className="w-full accent-accent" type="range" min={MIN_RADIUS_KM} max={MAX_RADIUS_KM} step="0.1" value={draftInput.radiusKm}
              onChange={(event) => updateDraftRadius(Number(event.target.value))} />
            {errors.radiusKm && <span className="field-error">{errors.radiusKm}</span>}
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>{formatRadius(MIN_RADIUS_KM)} km</span>
              <span>{formatRadius(MAX_RADIUS_KM)} km</span>
            </div>
          </section>
          <section>
            <Label>Không gian</Label>
            <div className="mt-3 flex flex-wrap gap-2">
              {spaceOptions.map((option) => (
                <Button key={option.value} type="button" size="sm" variant={draftInput.filters.spaces.includes(option.value) ? 'default' : 'outline'}
                  onClick={() => updateDraftFilter({ ...draftInput.filters, spaces: toggleFilter(draftInput.filters.spaces, option.value) })}>
                  {option.label}
                </Button>
              ))}
            </div>
          </section>
          <section>
            <Label>Mục đích</Label>
            <div className="mt-3 flex flex-wrap gap-2">
              {purposeOptions.map((option) => (
                <Button key={option.value} type="button" size="sm" variant={draftInput.filters.purposes.includes(option.value) ? 'default' : 'outline'}
                  onClick={() => updateDraftFilter({ ...draftInput.filters, purposes: toggleFilter(draftInput.filters.purposes, option.value) })}>
                  {option.label}
                </Button>
              ))}
            </div>
          </section>
          <Button type="button" onClick={applyFilters}>Áp dụng bộ lọc</Button>
        </div>
      </SheetContent>
    </Sheet>
      </CardContent>
    </Card>
  )
}
