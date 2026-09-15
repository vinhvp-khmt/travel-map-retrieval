import { useState, type FormEvent } from 'react'
import { login, register, type AuthSession } from '../api/auth'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { TabsList, TabsTrigger } from '@/components/ui/tabs'

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

  const form = (
    <>
      <TabsList className="mb-5 grid-cols-2">
        <TabsTrigger active={mode === 'login'} onClick={() => switchMode('login')}>Đăng nhập</TabsTrigger>
        <TabsTrigger active={mode === 'register'} onClick={() => switchMode('register')}>Đăng ký</TabsTrigger>
      </TabsList>
      <form className="grid gap-4" onSubmit={submit}>
        <Label className="grid gap-2 normal-case tracking-normal text-foreground">Email<Input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoComplete="email" /></Label>
        <Label className="grid gap-2 normal-case tracking-normal text-foreground">Mật khẩu<Input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} /></Label>
        {mode === 'register' && <Label className="grid gap-2 normal-case tracking-normal text-foreground">Xác nhận mật khẩu<Input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required minLength={8} autoComplete="new-password" /></Label>}
        {error && <Alert className="border-destructive/40 bg-destructive/10"><AlertDescription className="text-destructive">{error}</AlertDescription></Alert>}
        <Button className="w-full" loading={loading} disabled={loading}>{loading ? 'Đang xử lý…' : mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}</Button>
      </form>
      {mode === 'login' && <p className="mt-4 text-center text-xs text-muted-foreground">Demo: user@travelmap.local / Demo1234!</p>}
    </>
  )

  if (embedded) {
    return (
      <Card className="w-full max-w-md border-primary/10 shadow-2xl">
        <CardContent className="grid gap-5 p-6">
          <div className="grid gap-1.5">
            <p className="eyebrow">COFFEESCOPE ACCOUNT</p>
            <CardTitle id="auth-title" className="pr-9 text-3xl">{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản coffee lover'}</CardTitle>
            <CardDescription>{mode === 'login' ? 'Đăng nhập để tiếp tục tìm quán, giữ bàn và thanh toán demo.' : 'Tài khoản giúp CoffeeScope bảo vệ booking và lịch sử thanh toán của bạn.'}</CardDescription>
          </div>
          {form}
        </CardContent>
      </Card>
    )
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open) onClose?.() }}>
      <DialogContent>
        <DialogHeader>
          <p className="eyebrow">COFFEESCOPE ACCOUNT</p>
          <DialogTitle id="auth-title" className="pr-9">{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản coffee lover'}</DialogTitle>
          <DialogDescription>{mode === 'login' ? 'Đăng nhập để tiếp tục tìm quán, giữ bàn và thanh toán demo.' : 'Tài khoản giúp CoffeeScope bảo vệ booking và lịch sử thanh toán của bạn.'}</DialogDescription>
        </DialogHeader>
        {form}
      </DialogContent>
    </Dialog>
  )
}
