import { defineEventHandler, readBody, createError } from 'h3'
import { previewRequest, privatePreviewHeaders } from '../../utils/preview'
export default defineEventHandler(async event => {
  privatePreviewHeaders(event)
  const body = await readBody(event)
  if (!body || typeof body.token !== 'string' || body.token.length > 4096) throw createError({ statusCode: 404, statusMessage: 'Preview unavailable' })
  return previewRequest(event, body.token)
})
