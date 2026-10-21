import { getRequestURL, getQuery, setResponseHeader, removeResponseHeader } from 'h3'
import type { H3Event } from 'h3'
import { defineNitroPlugin, useRuntimeConfig } from '#imports'
import { privatePreviewHeaders } from '../utils/preview'
/** Error rendering can replace early middleware headers; enforce private policy at the final write. */
export default defineNitroPlugin(app => {
  const originalPath = (event: H3Event) => {
    const path = getRequestURL(event).pathname
    if (path !== '/__nuxt_error') return path
    try { return new URL(String(getQuery(event).url), 'http://localhost').pathname } catch { return path }
  }
  app.hooks.hook('beforeResponse', event => {
    const path = originalPath(event)
    if (!path.startsWith('/__editor') && !path.startsWith('/__preview')) return
    privatePreviewHeaders(event)
    if (path === '/__editor') {
      const origin = useRuntimeConfig(event).public.adminOrigin
      try { if (new URL(origin).origin !== origin) throw new Error(); setResponseHeader(event, 'Content-Security-Policy', `frame-ancestors ${origin}`); removeResponseHeader(event, 'X-Frame-Options') }
      catch { setResponseHeader(event, 'Content-Security-Policy', "frame-ancestors 'none'") }
    }
  })
  app.hooks.hook('render:response', (response, { event }) => {
    const path = originalPath(event)
    if (!path.startsWith('/__editor') && !path.startsWith('/__preview')) return
    const headers = response.headers ?? (response.headers = {})
    for (const key of Object.keys(headers)) if (['cache-control', 'cdn-cache-control', 'x-frame-options'].includes(key.toLowerCase())) delete headers[key]
    headers['cache-control'] = 'no-store'; headers['cdn-cache-control'] = 'no-store'
    headers['x-robots-tag'] = 'noindex, nofollow'; headers['referrer-policy'] = 'no-referrer'
    if (path === '/__editor') headers['content-security-policy'] = `frame-ancestors ${useRuntimeConfig(event).public.adminOrigin}`
  })
})
