const SESSION_KEY = 'aomap.session'

export interface Session {
  token: string
  userId: number
  orgId: number
  role: string
  name: string
  email: string
}

export function loadSession(): Session | null {
  try {
    const saved = JSON.parse(localStorage.getItem(SESSION_KEY) || '')
    if (saved && typeof saved.token === 'string' && saved.token) {
      return saved as Session
    }
  } catch {
    // 没有登录记录时留在登录页。
  }
  return null
}

export function saveSession(session: Session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function authHeader(): Record<string, string> {
  const session = loadSession()
  return session ? { Authorization: `Bearer ${session.token}` } : {}
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
      ...(init?.headers ?? {})
    }
  })
  const body = await response.json().catch(() => null)
  if (response.status === 401) {
    clearSession()
    if (!window.location.pathname.startsWith('/login')) {
      window.location.assign('/login')
    }
  }
  if (!response.ok || !body || body.code !== 0) {
    throw new Error(body?.message || `请求失败（${response.status}）`)
  }
  return body.data as T
}
