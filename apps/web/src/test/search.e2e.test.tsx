import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { cafe } from './fixtures'

const searchPois = vi.fn()
vi.mock('../api/search', () => ({ searchPois: (...args: unknown[]) => searchPois(...args) }))
vi.mock('../components/MapView', () => ({
  MapView: ({ results }: { results: unknown[] }) => <div aria-label="Bản đồ kết quả">{results.length} markers</div>,
}))

function mockLocation(latitude = 21.0278, longitude = 105.8342) {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn((success) => success({
        coords: { latitude, longitude, accuracy: 24 },
      })),
    },
  })
}

describe('Search journey E2E', () => {
  beforeEach(() => {
    searchPois.mockReset()
    sessionStorage.clear()
  })

  it('shows login/register before allowing access to the home search page', () => {
    render(<App />)
    expect(screen.getByText('Đăng nhập để tìm quán coffee hợp gu.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Tìm quán coffee')).not.toBeInTheDocument()
  })

  it('searches without GPS (không bắt buộc phải có vị trí) and sends the login token', async () => {
    sessionStorage.setItem('travelmap.session', JSON.stringify({
      email: 'user@travelmap.local',
      tokenType: 'Bearer',
      accessToken: 'access',
      accessExpiresInSeconds: 1800,
      refreshToken: 'refresh',
      refreshExpiresInSeconds: 604800,
    }))
    searchPois.mockResolvedValue({ normalizedQuery: 'ca phe', page: 0, size: 20, total: 0, results: [], suggestion: null })
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm quán' }))
    await waitFor(() => expect(searchPois).toHaveBeenCalled())
    const [calledInput, calledOptions] = searchPois.mock.calls[0]
    expect(calledInput.query).toBe('cà phê')
    expect(Number.isFinite(calledInput.latitude)).toBe(false)
    expect(Number.isFinite(calledInput.longitude)).toBe(false)
    expect(calledOptions).toEqual(expect.objectContaining({ token: 'access', signal: expect.any(AbortSignal) }))
  })

  it('searches backend-ranked POIs after the user provides a location', async () => {
    sessionStorage.setItem('travelmap.session', JSON.stringify({
      email: 'user@travelmap.local',
      tokenType: 'Bearer',
      accessToken: 'access',
      accessExpiresInSeconds: 1800,
      refreshToken: 'refresh',
      refreshExpiresInSeconds: 604800,
    }))
    searchPois.mockResolvedValue({ normalizedQuery: 'ca phe', page: 0, size: 20, total: 1, results: [cafe], suggestion: null })
    mockLocation()
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dùng vị trí của tôi' }))
    await waitFor(() => expect(screen.getByText('1 markers')).toBeInTheDocument())
    expect(searchPois).toHaveBeenCalledWith(
      expect.objectContaining({ latitude: 21.0278, longitude: 105.8342 }),
      expect.objectContaining({ token: 'access', signal: expect.any(AbortSignal) }),
    )
    fireEvent.click(screen.getByRole('button', { name: new RegExp(cafe.name) }))
    expect(screen.getByLabelText(`Chi tiết ${cafe.name}`)).toBeInTheDocument()
    expect(screen.getByLabelText('Chi tiết điểm xếp hạng')).toBeInTheDocument()
  })

  it('paginates visible search results with 10 items per page', async () => {
    sessionStorage.setItem('travelmap.session', JSON.stringify({
      email: 'user@travelmap.local',
      tokenType: 'Bearer',
      accessToken: 'access',
      accessExpiresInSeconds: 1800,
      refreshToken: 'refresh',
      refreshExpiresInSeconds: 604800,
    }))
    const results = Array.from({ length: 12 }, (_, index) => ({
      ...cafe,
      poiId: `local:${index + 1}`,
      name: `Cafe ${index + 1}`,
      latitude: cafe.latitude + index * 0.0001,
      longitude: cafe.longitude + index * 0.0001,
    }))
    searchPois.mockResolvedValue({ normalizedQuery: 'cafe', page: 0, size: 20, total: 12, results, suggestion: null })
    mockLocation()
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cafe' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dùng vị trí của tôi' }))
    await waitFor(() => expect(screen.getByText('10 markers')).toBeInTheDocument())
    expect(screen.getByText('Cafe 10')).toBeInTheDocument()
    expect(screen.queryByText('Cafe 11')).not.toBeInTheDocument()
    expect(screen.getByText('Trang 1/2 · Hiển thị 10/12')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Sau' }))
    expect(screen.getByText('2 markers')).toBeInTheDocument()
    expect(screen.getByText('Cafe 11')).toBeInTheDocument()
    expect(screen.getByText('Cafe 12')).toBeInTheDocument()
  })
})
