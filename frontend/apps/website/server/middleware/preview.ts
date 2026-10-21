import { defineEventHandler, getQuery, getRequestURL, setResponseHeader, setResponseStatus, send, createError } from 'h3'
import { useRuntimeConfig } from '#imports'
import { previewRequest, privatePreviewHeaders } from '../utils/preview'
export default defineEventHandler(async event => {
  const path = getRequestURL(event).pathname
  const config = useRuntimeConfig(event)
  if (path.startsWith('/__preview')) privatePreviewHeaders(event)
  let ancestor = "'self'"
  if (path === '/__editor') {
    privatePreviewHeaders(event)
    try { const origin = new URL(config.public.adminOrigin); if (origin.origin !== config.public.adminOrigin || !['http:', 'https:'].includes(origin.protocol)) throw new Error(); ancestor = origin.origin }
    catch { throw createError({ statusCode: 503, statusMessage: 'Preview unavailable' }) }
  }
  setResponseHeader(event, 'Content-Security-Policy', `frame-ancestors ${ancestor}`)
  if (path === '/__editor') {
    const token = getQuery(event).token
    if (typeof token !== 'string' || !token) throw createError({ statusCode: 404, statusMessage: 'Preview unavailable' })
    try { await previewRequest(event, token, {}) }
    catch (error) {
      const status = (error as { statusCode?: number }).statusCode === 404 ? 404 : 503
      setResponseStatus(event, status); setResponseHeader(event, 'Content-Type', 'application/json')
      return send(event, JSON.stringify({ statusCode: status, code: status === 404 ? 'DELIVERY_PREVIEW_INVALID' : 'DELIVERY_PREVIEW_UNAVAILABLE' }))
    }
  }
})
