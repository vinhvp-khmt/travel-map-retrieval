import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthModal } from '../components/AuthModal'

describe('AuthModal', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('logs in with the demo account and returns a session', async () => {
    const onAuthenticated = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true, status: 200,
      json: async () => ({ tokenType: 'Bearer', accessToken: 'access', accessExpiresInSeconds: 1800,
        refreshToken: 'refresh', refreshExpiresInSeconds: 604800 }),
    }))
    render(<AuthModal onClose={() => undefined} onAuthenticated={onAuthenticated} />)
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith(expect.objectContaining({
      email: 'user@travelmap.local', accessToken: 'access',
    })))
  })

  it('shows registration fields when switching tabs', () => {
    render(<AuthModal onClose={() => undefined} onAuthenticated={() => undefined} />)
    fireEvent.click(screen.getByRole('tab', { name: 'Đăng ký' }))
    expect(screen.getByLabelText('Xác nhận mật khẩu')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tạo tài khoản' })).toBeInTheDocument()
  })
})
