import { useState, type FormEvent } from 'react'
import type { SearchInput, UserLocation } from '../types/search'

export function validateSearchInput(input: SearchInput): Record<string, string> {
  const errors: Record<string, string> = {}
  const queryLength = input.query.trim().length
  if (queryLength < 1 || queryLength > 200) errors.query = 'Nhập từ khóa từ 1 đến 200 ký tự.'
  if (!Number.isFinite(input.latitude) || input.latitude < -90 || input.latitude > 90) errors.latitude = 'Hãy cấp quyền GPS hoặc nhập vĩ độ hợp lệ.'
  if (!Number.isFinite(input.longitude) || input.longitude < -180 || input.longitude > 180) errors.longitude = 'Hãy cấp quyền GPS hoặc nhập kinh độ hợp lệ.'
  if (input.radiusKm < 0.1 || input.radiusKm > 10) errors.radiusKm = 'Bán kính phải từ 0.1 đến 10 km.'
  if (input.visitAt && new Date(input.visitAt).getTime() < Date.now() - 60_000) {
    errors.visitAt = 'Thời gian ghé thăm phải từ hiện tại trở đi.'
  }
  return errors
}

type Props = { loading: boolean; onSearch: (input: SearchInput) => void; onLocationChange?: (location: UserLocation) => void }
type FormInput = { query: string; latitude: string; longitude: string; radiusKm: number; visitAt?: string }

export function SearchForm({ loading, onSearch, onLocationChange }: Props) {
  const [input, setInput] = useState<FormInput>({ query: '', latitude: '', longitude: '', radiusKm: 2 })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [locating, setLocating] = useState(false)
  const [locationStatus, setLocationStatus] = useState('Chưa có vị trí. Hãy bấm “Dùng vị trí của tôi” hoặc nhập tọa độ thật trước khi tìm.')

  function toSearchInput(current: FormInput): SearchInput {
    const toCoordinate = (value: string) => value.trim() ? Number(value) : Number.NaN
    return {
      query: current.query,
      latitude: toCoordinate(current.latitude),
      longitude: toCoordinate(current.longitude),
      radiusKm: current.radiusKm,
      visitAt: current.visitAt,
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const searchInput = toSearchInput(input)
    const nextErrors = validateSearchInput(searchInput)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length === 0) onSearch(searchInput)
  }

  function updateNumber(field: 'latitude' | 'longitude' | 'radiusKm', value: string) {
    const next = { ...input, [field]: field === 'radiusKm' ? Number(value) : value }
    setInput(next)
    if (field === 'latitude' || field === 'longitude') {
      const latitude = Number(next.latitude)
      const longitude = Number(next.longitude)
      if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
        onLocationChange?.({ latitude, longitude })
        setLocationStatus(`Tọa độ đang chọn: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}.`)
      }
    }
  }

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
        setLocationStatus(`Vị trí hiện tại: ${nextLocation.latitude.toFixed(5)}, ${nextLocation.longitude.toFixed(5)} ± ${nextLocation.accuracyMeters} m.`)
        if (nextInput.query.trim()) onSearch(toSearchInput(nextInput))
        setLocating(false)
      },
      (reason) => {
        setLocationStatus(reason.code === reason.PERMISSION_DENIED ? 'Bạn chưa cấp quyền vị trí cho trình duyệt.' : 'Không lấy được GPS, hãy thử lại hoặc nhập tọa độ thủ công.')
        setLocating(false)
      },
      { enableHighAccuracy: true, maximumAge: 60_000, timeout: 10_000 },
    )
  }

  return (
    <form className="search-form" onSubmit={submit} aria-label="Tìm địa điểm">
      <div className="query-field">
        <label htmlFor="query">Bạn muốn đi đâu?</label>
        <input id="query" value={input.query} placeholder="cafe, supermarket, khách sạn..."
          onChange={(event) => setInput({ ...input, query: event.target.value })} />
        {errors.query && <span className="field-error">{errors.query}</span>}
      </div>
      <div className="compact-field">
        <label htmlFor="latitude">Vĩ độ</label>
        <input id="latitude" type="number" step="any" value={input.latitude} placeholder="Từ GPS"
          onChange={(event) => updateNumber('latitude', event.target.value)} />
        {errors.latitude && <span className="field-error">{errors.latitude}</span>}
      </div>
      <div className="compact-field">
        <label htmlFor="longitude">Kinh độ</label>
        <input id="longitude" type="number" step="any" value={input.longitude} placeholder="Từ GPS"
          onChange={(event) => updateNumber('longitude', event.target.value)} />
        {errors.longitude && <span className="field-error">{errors.longitude}</span>}
      </div>
      <div className="compact-field radius-field">
        <label htmlFor="radius">Bán kính · {input.radiusKm} km</label>
        <input id="radius" type="range" min="0.1" max="10" step="0.1" value={input.radiusKm}
          onChange={(event) => updateNumber('radiusKm', event.target.value)} />
        {errors.radiusKm && <span className="field-error">{errors.radiusKm}</span>}
      </div>
      <div className="compact-field">
        <label htmlFor="visitAt">Ghé lúc</label>
        <input id="visitAt" type="datetime-local" value={input.visitAt ?? ''}
          onChange={(event) => setInput({ ...input, visitAt: event.target.value || undefined })} />
        {errors.visitAt && <span className="field-error">{errors.visitAt}</span>}
      </div>
      <button type="button" className="location-button" disabled={loading || locating} onClick={useCurrentLocation}>
        {locating ? 'Đang lấy GPS...' : 'Dùng vị trí của tôi'}
      </button>
      <button className="search-button" disabled={loading}>{loading ? 'Đang tìm…' : 'Khám phá'}</button>
      <p className="location-status" aria-live="polite">{locationStatus}</p>
    </form>
  )
}
