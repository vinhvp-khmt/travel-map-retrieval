import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { cafe } from './fixtures'

const searchGeoapify = vi.fn()
vi.mock('../api/geoapify', () => ({ searchGeoapify: (...args: unknown[]) => searchGeoapify(...args) }))
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
    searchGeoapify.mockReset()
    sessionStorage.clear()
  })

  it('shows login/register before allowing access to the home search page', () => {
    render(<App />)
    expect(screen.getByText('Đăng nhập để tìm quán coffee hợp gu.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Tìm quán coffee')).not.toBeInTheDocument()
  })

  it('searches, synchronizes the result list and opens POI score detail', async () => {
    sessionStorage.setItem('travelmap.session', JSON.stringify({
      email: 'user@travelmap.local',
      tokenType: 'Bearer',
      accessToken: 'access',
      accessExpiresInSeconds: 1800,
      refreshToken: 'refresh',
      refreshExpiresInSeconds: 604800,
    }))
    searchGeoapify.mockResolvedValue([])
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm quán' }))
    await waitFor(() => expect(screen.getByText(/Hãy bấm “Dùng vị trí của tôi” trước khi tìm/)).toBeInTheDocument())
    expect(searchGeoapify).not.toHaveBeenCalled()
  })

  it('searches Geoapify places after the user provides a location', async () => {
    sessionStorage.setItem('travelmap.session', JSON.stringify({
      email: 'user@travelmap.local',
      tokenType: 'Bearer',
      accessToken: 'access',
      accessExpiresInSeconds: 1800,
      refreshToken: 'refresh',
      refreshExpiresInSeconds: 604800,
    }))
    searchGeoapify.mockResolvedValue([cafe])
    mockLocation()
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dùng vị trí của tôi' }))
    await waitFor(() => expect(screen.getByText('1 markers')).toBeInTheDocument())
    expect(searchGeoapify).toHaveBeenCalledWith(expect.objectContaining({ latitude: 21.0278, longitude: 105.8342 }), expect.any(AbortSignal))
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
    searchGeoapify.mockResolvedValue(Array.from({ length: 12 }, (_, index) => ({
      ...cafe,
      poiId: `geoapify:${index + 1}`,
      name: `Cafe ${index + 1}`,
      latitude: cafe.latitude + index * 0.0001,
      longitude: cafe.longitude + index * 0.0001,
    })))
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
