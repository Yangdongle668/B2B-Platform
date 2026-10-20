export type { paths, components } from './schema'
export interface Problem { code: string; status: number; title: string }
export async function api<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  if (!path.startsWith('/api/')) throw new Error('Invalid API path')
  const response = await fetch(path, { ...options, credentials: 'same-origin', headers: { 'Content-Type': 'application/json', ...(token ? { 'X-XSRF-TOKEN': token } : {}), ...options.headers } })
  if (!response.ok) throw await response.json() as Problem
  return (response.status === 204 ? undefined : await response.json()) as T
}
