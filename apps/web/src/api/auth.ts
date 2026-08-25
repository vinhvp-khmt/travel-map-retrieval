import { API_BASE_URL } from './config'

export type AuthSession = {
  email: string
  tokenType: string
  accessToken: string
  accessExpiresInSeconds: number
  refreshToken: string
  refreshExpiresInSeconds: number
}

type AuthResponse = Omit<AuthSession, 'email'>

async function request<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    const fieldMessage = error?.fields && Object.values(error.fields)[0]
    throw new Error(String(fieldMessage ?? error?.message ?? `Request failed (${response.status})`))
  }
  return response.status === 204 ? undefined as T : response.json()
}

export async function login(email: string, password: string): Promise<AuthSession> {
  const tokens = await request<AuthResponse>('/api/v1/auth/login', { email, password })
  return { email, ...tokens }
}

export async function register(email: string, password: string): Promise<AuthSession> {
  await request('/api/v1/auth/register', { email, password })
  return login(email, password)
}

export async function logout(refreshToken: string): Promise<void> {
  await request('/api/v1/auth/logout', { refreshToken })
}
