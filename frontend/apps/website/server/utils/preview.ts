import { getRequestHeader, createError, setResponseHeader } from 'h3'
import type { H3Event } from 'h3'
import { useRuntimeConfig } from '#imports'
export function privatePreviewHeaders(event: H3Event): void {
  setResponseHeader(event, 'Cache-Control', 'no-store'); setResponseHeader(event, 'CDN-Cache-Control', 'no-store')
  setResponseHeader(event, 'X-Robots-Tag', 'noindex, nofollow'); setResponseHeader(event, 'Referrer-Policy', 'no-referrer')
}
export async function previewRequest(event: H3Event, token: string, parts?: Record<string, unknown>): Promise<unknown> {
  privatePreviewHeaders(event)
  const config = useRuntimeConfig(event)
  if (!config.internalToken) throw createError({ statusCode: 503, statusMessage: 'Preview unavailable' })
  const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 4000)
  try {
    const response = await fetch(`${config.backendUrl}/api/public/v1/delivery/preview${parts ? '' : `?token=${encodeURIComponent(token)}`}`, {
      method: parts ? 'POST' : 'GET', redirect: 'error', signal: controller.signal,
      headers: { 'X-Internal-Token': config.internalToken, 'X-Site-Host': getRequestHeader(event, 'host') ?? '', ...(parts ? { 'Content-Type': 'application/json' } : {}) },
      ...(parts ? { body: JSON.stringify({ token, partOverrides: parts }) } : {}),
    })
    if (!response.ok) throw createError({ statusCode: [400, 404, 413, 422].includes(response.status) ? response.status : 503, statusMessage: 'Preview unavailable' })
    return await response.json()
  } catch (error) {
    const status = (error as { statusCode?: number }).statusCode
    if (status) throw error
    throw createError({ statusCode: 503, statusMessage: 'Preview unavailable' })
  } finally { clearTimeout(timeout) }
}
