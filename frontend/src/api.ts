export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {})
    }
  })
  const body = await response.json().catch(() => null)
  if (!response.ok || !body || body.code !== 0) {
    throw new Error(body?.message || `请求失败（${response.status}）`)
  }
  return body.data as T
}
