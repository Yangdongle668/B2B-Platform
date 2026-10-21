import { describe, it, expect } from 'vitest'
import { envelope, acceptEditorMessage, isEditorEnvelope, isLayoutDocument, patchDocument } from './editor-protocol'
import type { LayoutDocument } from './editor-protocol'
const document: LayoutDocument = { version: 1, sections: [{ id: 'one', component: 'hero', componentVersion: 1, props: { title: 'one' } }] }
describe('editor wire contract', () => {
  it('binds exact origin, window source, direction and version', () => {
    const source = {} as Window
    const event = { origin: 'https://admin.test', source, data: envelope('editor:init', { document, locale: 'zh-CN', device: 'desktop' }) }
    expect(acceptEditorMessage(event, event.origin, source, 'editor')).toBe(true)
    expect(acceptEditorMessage(event, 'https://other.test', source, 'editor')).toBe(false)
    expect(acceptEditorMessage(event, event.origin, {} as Window, 'editor')).toBe(false)
    expect(acceptEditorMessage(event, event.origin, null, 'editor')).toBe(false)
    expect(acceptEditorMessage(event, event.origin, source, 'preview')).toBe(false)
    expect(isEditorEnvelope({ ...event.data, version: 2 })).toBe(false)
    expect(isEditorEnvelope({ ...event.data, type: 'editor:execute' })).toBe(false)
  })
  it('rejects malformed payloads, duplicate section IDs and excessive nesting', () => {
    expect(isLayoutDocument({ ...document, sections: [...document.sections, ...document.sections] })).toBe(false)
    expect(isLayoutDocument({ ...document, sections: [{ ...document.sections[0], visibility: { mobile: 'false' } }] })).toBe(false)
    expect(isEditorEnvelope(envelope('editor:patch', { ops: [{ op: 'add', path: '/' }] as never }))).toBe(false)
    expect(isEditorEnvelope(envelope('preview:insert', { parentId: null, slot: null, index: -1 }))).toBe(false)
  })
  it('applies all RFC6902 operations atomically without changing input', () => {
    const next = patchDocument(document, [{ op: 'test', path: '/sections/0/props/title', value: 'one' }, { op: 'copy', from: '/sections/0/props/title', path: '/sections/0/props/copied' }, { op: 'move', from: '/sections/0/props/copied', path: '/sections/0/props/moved' }, { op: 'replace', path: '/sections/0/props/title', value: 'two' }, { op: 'remove', path: '/sections/0/props/moved' }])
    expect(next.sections[0]!.props).toEqual({ title: 'two' }); expect(document.sections[0]!.props.title).toBe('one')
    expect(() => patchDocument(document, [{ op: 'test', path: '/sections/0/props/title', value: 'bad' }])).toThrow()
    expect(() => patchDocument(document, [{ op: 'remove', path: '/sections/9' }])).toThrow()
    expect(() => patchDocument(document, [{ op: 'replace', path: '/version', value: 2 }])).toThrow()
  })
  it('blocks prototype traversal and descendant moves', () => {
    for (const path of ['/__proto__/polluted', '/constructor/prototype/polluted', '/sections/0/props/__proto__']) expect(() => patchDocument(document, [{ op: 'add', path, value: true }])).toThrow()
    expect(() => patchDocument(document, [{ op: 'move', from: '/sections/0', path: '/sections/0/props/a' }])).toThrow()
    expect({}).not.toHaveProperty('polluted')
    const cyclic: Record<string, unknown> = {}; cyclic.self = cyclic
    expect(isLayoutDocument(cyclic)).toBe(false); expect(isEditorEnvelope(cyclic)).toBe(false)
    expect(() => patchDocument({ ...document, sections: [{ ...document.sections[0]!, props: { a: 1, b: 2 } }] }, [{ op: 'test', path: '/sections/0/props', value: { b: 2, a: 1 } }])).not.toThrow()
  })
})
