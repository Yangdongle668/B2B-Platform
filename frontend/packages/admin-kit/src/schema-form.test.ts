import { it, expect } from 'vitest'
import { formSubmission, inferEditor } from './schema-form'
it('omits hidden fields and blank/masked secrets recursively without changing the draft', () => {
  const schema = { type: 'object', properties: { mode: { type: 'string' }, hidden: { type: 'string', 'x-visible-if': { prop: 'mode', equals: 'video' } }, secret: { type: 'string', 'x-secret': true }, items: { type: 'array', items: { type: 'object', properties: { secret: { type: 'string', 'x-secret': true } } } } } }
  const value = { mode: 'image', hidden: 'private', secret: '********', items: [{ secret: '' }, { secret: 'new' }] }
  expect(formSubmission(schema, value)).toEqual({ mode: 'image', items: [{}, { secret: 'new' }] })
  expect(value.hidden).toBe('private')
})
it('infers standard controls and keeps explicit reference controls', () => {
  expect(['string', 'object', 'array', 'boolean', 'integer'].map(type => inferEditor({ type }))).toEqual(['text', 'group', 'repeater', 'switch', 'number'])
  expect(inferEditor({ enum: ['a'] })).toBe('select'); expect(inferEditor({ type: 'object', 'x-editor': 'media' })).toBe('media')
})
it('preserves schema-permitted reference and data-source objects during submission', () => {
  const value = { $source: { provider: 'sample.items', params: { limit: 3 } } }
  expect(formSubmission({ type: 'object' }, value)).toEqual(value)
  expect(formSubmission({ type: 'object', additionalProperties: false }, value)).toEqual({})
})
