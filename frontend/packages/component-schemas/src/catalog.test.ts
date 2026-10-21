import { it, expect } from 'vitest'
import { SchemaCatalog } from './index'
it('chains migrations, validates current props, rejects future versions and leaves snapshots immutable', () => {
  const catalog = new SchemaCatalog([{ type: 'test', version: 3, module: 'page', name: '测试', variants: ['default'], propsSchema: { type: 'object', additionalProperties: false, required: ['title'], properties: { title: { type: 'string' } } } }], { test: { 1: props => ({ heading: props.old }), 2: props => ({ title: props.heading }) } })
  const input = { old: 'Title' }
  expect(catalog.migrate('test', input, 1)).toEqual({ props: { title: 'Title' }, version: 3 }); expect(input).toEqual({ old: 'Title' })
  expect(() => catalog.migrate('test', input, 4)).toThrow()
  expect(() => catalog.migrate('test', { title: 2 }, 3)).toThrow()
  expect(() => new SchemaCatalog([{ type: 'test', version: 2, module: 'page', name: '测试', variants: ['default'], propsSchema: { type: 'object' } }])).toThrow('Missing component migration')
})

it('compiles shared reference schemas without accepting executable or raw internal URLs', () => {
  const catalog = new SchemaCatalog([{ type: 'ref-test', version: 1, module: 'page', label: '引用测试', variants: ['default'], propsSchema: { type: 'object', additionalProperties: false, required: ['link'], properties: { link: { $ref: '../../defs/common.json#/$defs/link' } } } }])
  expect(catalog.validate('ref-test', { link: { $content: '123' } })).toBe(true)
  expect(catalog.validate('ref-test', { link: { href: 'https://example.com/path' } })).toBe(true)
  for (const href of ['/zh/private', 'javascript:alert(1)', 'https://user:secret@example.com']) expect(catalog.validate('ref-test', { link: { href } })).toBe(false)
  expect(() => new SchemaCatalog([{ type: 'bad', version: 1, module: 'page', label: '错误', variants: ['default'], propsSchema: { $ref: 'https://example.com/schema' } }])).toThrow('External schema reference')
})
