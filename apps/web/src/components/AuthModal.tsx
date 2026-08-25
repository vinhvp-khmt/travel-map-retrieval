import { useState, type FormEvent } from 'react'
import { login, register, type AuthSession } from '../api/auth'

type Props = { embedded?: boolean; onClose?: () => void; onAuthenticated: (session: AuthSession) => void }

export function AuthModal({ embedded = false, onClose, onAuthenticated }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('user@travelmap.local')
  const [password, setPassword] = useState('Demo1234!')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(undefined)
    if (mode === 'register' && password !== confirmPassword) { setError('Mật khẩu xác nhận không khớp.'); return }
    setLoading(true)
    try { onAuthenticated(await (mode === 'login' ? login(email, password) : register(email, password))) }
    catch (reason) { setError((reason as Error).message) }
    finally { setLoading(false) }
  }

  function switchMode(next: 'login' | 'register') { setMode(next); setError(undefined); setConfirmPassword('') }

  const dialog = (
    <section className="auth-dialog" role="dialog" aria-modal={!embedded} aria-labelledby="auth-title">
      {!embedded && <button className="auth-close" aria-label="Đóng" onClick={onClose}>×</button>}
      <p className="eyebrow">TRAVELMAP ACCOUNT</p>
      <h2 id="auth-title">{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản du khách'}</h2>
      <div className="auth-tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'login'} onClick={() => switchMode('login')}>Đăng nhập</button>
        <button role="tab" aria-selected={mode === 'register'} onClick={() => switchMode('register')}>Đăng ký</button>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoComplete="email" /></label>
        <label>Mật khẩu<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} /></label>
        {mode === 'register' && <label>Xác nhận mật khẩu<input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required minLength={8} autoComplete="new-password" /></label>}
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="auth-submit" disabled={loading}>{loading ? 'Đang xử lý…' : mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}</button>
      </form>
      {mode === 'login' && <p className="demo-hint">Demo: user@travelmap.local / Demo1234!</p>}
    </section>
  )

  if (embedded) return dialog

  return <div className="auth-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose?.()}>
    {dialog}
  </div>
}
