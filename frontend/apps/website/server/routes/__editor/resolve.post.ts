import { defineEventHandler, readBody, createError } from 'h3'
import { previewRequest, privatePreviewHeaders } from '../../utils/preview'
export default defineEventHandler(async event => {
  privatePreviewHeaders(event)
  const body = await readBody(event)
  if (!body || typeof body.token !== 'string' || !body.partOverrides || typeof body.partOverrides !== 'object' || Array.isArray(body.partOverrides) || JSON.stringify(body).length > 1_048_576 || Object.keys(body).some(key => !['token', 'partOverrides'].includes(key))) throw createError({ statusCode: 400, statusMessage: 'Invalid preview request' })
  return previewRequest(event, body.token, body.partOverrides)
})
