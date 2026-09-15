import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { SearchForm, validateSearchInput } from '../components/SearchForm'

describe('SearchForm', () => {
  it('validates the radius business rule', () => {
    expect(validateSearchInput({ query: 'cafe', latitude: 10, longitude: 106, radiusKm: 10.1 }))
      .toHaveProperty('radiusKm')
  })

  it('blocks an empty query and shows a field error', () => {
    const search = vi.fn()
    render(<SearchForm loading={false} onSearch={search} />)
    fireEvent.click(screen.getByRole('button', { name: 'Tìm quán' }))
    expect(screen.getByText(/Nhập từ khóa/)).toBeInTheDocument()
    expect(search).not.toHaveBeenCalled()
  })

  it('requires a real location before searching', () => {
    const search = vi.fn()
    render(<SearchForm loading={false} onSearch={search} />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'cà phê' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm quán' }))
    expect(screen.getByText(/Hãy bấm “Dùng vị trí của tôi” trước khi tìm/)).toBeInTheDocument()
    expect(search).not.toHaveBeenCalled()
  })

  it('uses browser GPS coordinates and searches around the current position', async () => {
    const search = vi.fn()
    const onLocationChange = vi.fn()
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: vi.fn((success) => success({
          coords: { latitude: 10.78123, longitude: 106.70456, accuracy: 18 },
        })),
      },
    })
    render(<SearchForm loading={false} onSearch={search} onLocationChange={onLocationChange} />)
    fireEvent.change(screen.getByLabelText('Bạn muốn quán coffee kiểu nào?'), { target: { value: 'coffee làm việc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dùng vị trí của tôi' }))
    await waitFor(() => expect(search).toHaveBeenCalledWith(expect.objectContaining({
      query: 'coffee làm việc',
      latitude: 10.78123,
      longitude: 106.70456,
    })))
    expect(onLocationChange).toHaveBeenCalledWith(expect.objectContaining({ accuracyMeters: 18 }))
    expect(screen.getByText(/Vị trí hiện tại/)).toBeInTheDocument()
  })
})
