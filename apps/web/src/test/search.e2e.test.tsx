import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { cafe } from './fixtures'

const searchGeoapify = vi.fn()
vi.mock('../api/geoapify', () => ({ searchGeoapify: (...args: unknown[]) => searchGeoapify(...args) }))
vi.mock('../components/MapView', () => ({
  MapView: ({ results }: { results: unknown[] }) => <div aria-label="Bản đồ kết quả">{results.length} markers</div>,
}))

describe('Search journey E2E', () => {
  beforeEach(() => {
    searchGeoapify.mockReset()
    sessionStorage.clear()
  })

  it('shows login/register before allowing access to the home search page', () => {
    render(<App />)
    expect(screen.getByText('Đăng nhập để vào TravelMap.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Tìm địa điểm')).not.toBeInTheDocument()
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
    fireEvent.change(screen.getByLabelText('Bạn muốn đi đâu?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Khám phá' }))
    await waitFor(() => expect(screen.getByText(/Hãy cấp quyền GPS hoặc nhập vĩ độ hợp lệ/)).toBeInTheDocument())
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
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn đi đâu?'), { target: { value: 'cà phê' } })
    fireEvent.change(screen.getByLabelText('Vĩ độ'), { target: { value: '21.0278' } })
    fireEvent.change(screen.getByLabelText('Kinh độ'), { target: { value: '105.8342' } })
    fireEvent.click(screen.getByRole('button', { name: 'Khám phá' }))
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
    render(<App />)
    fireEvent.change(screen.getByLabelText('Bạn muốn đi đâu?'), { target: { value: 'cafe' } })
    fireEvent.change(screen.getByLabelText('Vĩ độ'), { target: { value: '21.0278' } })
    fireEvent.change(screen.getByLabelText('Kinh độ'), { target: { value: '105.8342' } })
    fireEvent.click(screen.getByRole('button', { name: 'Khám phá' }))
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
