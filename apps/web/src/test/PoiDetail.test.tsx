import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PoiDetail } from '../components/PoiDetail'
import { cafe } from './fixtures'

describe('PoiDetail and ScoreBreakdown', () => {
  it('renders location facts and explainable ranking signals', () => {
    render(<PoiDetail poi={cafe} onClose={vi.fn()} />)
    expect(screen.getByRole('heading', { name: cafe.name })).toBeInTheDocument()
    expect(screen.getByText('Đang mở cửa')).toBeInTheDocument()
    expect(screen.getByLabelText('Phù hợp')).toHaveValue(1)
    expect(screen.getByText('91')).toBeInTheDocument()
  })
})
