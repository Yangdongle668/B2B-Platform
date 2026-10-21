/** Shared wire contract. Messages are data; paths never access an object's prototype. */
export interface Section {
  id: string
  component: string
  componentVersion: number
  variant?: string
  props: Record<string, unknown>
  hidden?: boolean
  style?: Record<string, string>
  visibility?: { mobile?: boolean; tablet?: boolean; desktop?: boolean }
  children?: Record<string, Section[]>
}
export interface LayoutDocument { version: 1; sections: Section[] }
export type JsonPatchOperation = { op: 'add' | 'replace' | 'test'; path: string; value: unknown } | { op: 'remove'; path: string } | { op: 'copy' | 'move'; path: string; from: string }
export interface EditorPayloads {
  'editor:init': { document: LayoutDocument; locale: string; device: 'desktop' | 'tablet' | 'mobile' }
  'editor:patch': { ops: JsonPatchOperation[] }
  'editor:select': { sectionId: string | null }
  'editor:scrollTo': { sectionId: string | null }
  'preview:ready': { protocolVersion: 1 }
  'preview:select': { sectionId: string | null }
  'preview:hover': { sectionId: string | null }
  'preview:insert': { parentId: string | null; slot: string | null; index: number }
  'preview:error': { sectionId: string | null; message: string }
}
export type EditorEnvelope<K extends keyof EditorPayloads = keyof EditorPayloads> = { [T in K]: { source: 'b2bweb-editor'; version: 1; type: T; payload: EditorPayloads[T] } }[K]
export function envelope<K extends keyof EditorPayloads>(type: K, payload: EditorPayloads[K]): EditorEnvelope<K> { return { source: 'b2bweb-editor', version: 1, type, payload } as EditorEnvelope<K> }
const record = (v: unknown): v is Record<string, unknown> => v !== null && typeof v === 'object' && !Array.isArray(v)
const nullableId = (v: unknown) => v === null || typeof v === 'string' && /^[A-Za-z0-9_-]{1,80}$/.test(v)
export function isLayoutDocument(value: unknown): value is LayoutDocument {
  try { if (JSON.stringify(value).length > 1_048_576) return false } catch { return false }
  if (!record(value) || value.version !== 1 || !Array.isArray(value.sections)) return false
  const ids = new Set<string>(); let count = 0
  function sections(items: unknown[], depth: number): boolean {
    return depth <= 2 && items.every(item => {
      if (++count > 500 || !record(item) || typeof item.id !== 'string' || !nullableId(item.id) || ids.has(item.id) || typeof item.component !== 'string' || !/^[a-z][a-z0-9.-]*$/.test(item.component) || !Number.isInteger(item.componentVersion) || Number(item.componentVersion) < 1 || !record(item.props)) return false
      ids.add(item.id)
      if (item.hidden !== undefined && typeof item.hidden !== 'boolean' || item.variant !== undefined && typeof item.variant !== 'string') return false
      if (item.style !== undefined && (!record(item.style) || Object.values(item.style).some(v => typeof v !== 'string'))) return false
      if (item.visibility !== undefined && (!record(item.visibility) || Object.entries(item.visibility).some(([k, v]) => !['mobile', 'tablet', 'desktop'].includes(k) || typeof v !== 'boolean'))) return false
      return item.children === undefined || record(item.children) && Object.entries(item.children).every(([key, children]) => /^[a-z][a-zA-Z0-9]*$/.test(key) && Array.isArray(children) && sections(children, depth + 1))
    })
  }
  return sections(value.sections, 0)
}
export function isEditorEnvelope(value: unknown): value is EditorEnvelope {
  try { if (JSON.stringify(value).length > 1_048_576) return false } catch { return false }
  if (!record(value) || value.source !== 'b2bweb-editor' || value.version !== 1 || !record(value.payload)) return false
  const p = value.payload
  switch (value.type) {
    case 'editor:init': return isLayoutDocument(p.document) && typeof p.locale === 'string' && ['desktop', 'tablet', 'mobile'].includes(String(p.device))
    case 'editor:patch': return Array.isArray(p.ops) && p.ops.length <= 500 && p.ops.every(op => record(op) && typeof op.path === 'string' && (['add', 'replace', 'test'].includes(String(op.op)) && Object.hasOwn(op, 'value') || op.op === 'remove' || ['move', 'copy'].includes(String(op.op)) && typeof op.from === 'string'))
    case 'editor:select': case 'editor:scrollTo': case 'preview:select': case 'preview:hover': return nullableId(p.sectionId)
    case 'preview:ready': return p.protocolVersion === 1
    case 'preview:error': return nullableId(p.sectionId) && typeof p.message === 'string' && p.message.length <= 2000
    case 'preview:insert': return nullableId(p.parentId) && nullableId(p.slot) && Number.isInteger(p.index) && Number(p.index) >= 0
    default: return false
  }
}
export function acceptEditorMessage(event: Pick<MessageEvent, 'origin' | 'source' | 'data'>, origin: string, source: MessageEventSource | null, direction: 'editor' | 'preview'): event is MessageEvent<EditorEnvelope> {
  return source !== null && event.source === source && event.origin === origin && isEditorEnvelope(event.data) && event.data.type.startsWith(`${direction}:`)
}
function pathTokens(path: string): string[] {
  if (path === '') return []
  if (!path.startsWith('/') || /~(?![01])/u.test(path)) throw new Error('Invalid JSON pointer')
  const tokens = path.slice(1).split('/').map(v => v.replace(/~1/g, '/').replace(/~0/g, '~'))
  if (tokens.some(v => ['__proto__', 'prototype', 'constructor'].includes(v))) throw new Error('Unsafe JSON pointer')
  return tokens
}
export function patchDocument(document: LayoutDocument, ops: JsonPatchOperation[]): LayoutDocument {
  let result: unknown = structuredClone(document)
  function access(tokens: string[], write: boolean, operation?: 'add' | 'replace' | 'remove', value?: unknown): unknown {
    if (!tokens.length) { const previous = result; if (write) result = operation === 'remove' ? undefined : structuredClone(value); return previous }
    let parent: unknown = result
    for (const token of tokens.slice(0, -1)) {
      if (!record(parent) && !Array.isArray(parent) || !Object.hasOwn(parent, token)) throw new Error('Missing JSON pointer')
      parent = (parent as Record<string, unknown>)[token]
    }
    if (!record(parent) && !Array.isArray(parent)) throw new Error('Invalid JSON pointer parent')
    const key = tokens.at(-1)!
    if (Array.isArray(parent)) {
      const index = key === '-' && operation === 'add' ? parent.length : /^(0|[1-9][0-9]*)$/.test(key) ? Number(key) : -1
      if (index < 0 || index > parent.length || index === parent.length && operation !== 'add') throw new Error('Invalid array index')
      const previous = parent[index]
      if (write) { if (operation === 'add') parent.splice(index, 0, structuredClone(value)); else if (operation === 'remove') parent.splice(index, 1); else parent[index] = structuredClone(value) }
      return previous
    }
    if (operation !== 'add' && !Object.hasOwn(parent, key)) throw new Error('Missing JSON pointer')
    const previous = parent[key]
    if (write) { if (operation === 'remove') delete parent[key]; else parent[key] = structuredClone(value) }
    return previous
  }
  if (ops.length > 500) throw new Error('Too many operations')
  for (const op of ops) {
    const path = pathTokens(op.path)
    if (op.op === 'test') { if (!jsonEqual(access(path, false), op.value)) throw new Error('JSON patch test failed') }
    else if (op.op === 'copy' || op.op === 'move') {
      const from = pathTokens(op.from)
      if (op.op === 'move' && path.length > from.length && from.every((v, i) => path[i] === v)) throw new Error('Cannot move into child')
      const value = structuredClone(access(from, false))
      if (op.op === 'move') access(from, true, 'remove')
      access(path, true, 'add', value)
    } else access(path, true, op.op, 'value' in op ? op.value : undefined)
  }
  if (!isLayoutDocument(result)) throw new Error('Invalid patched layout')
  return result
}
function jsonEqual(a: unknown, b: unknown, depth = 0): boolean {
  if (a === b) return true
  if (depth > 30) return false
  if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((value, index) => jsonEqual(value, b[index], depth + 1))
  if (record(a) && record(b)) { const keys = Object.keys(a); return keys.length === Object.keys(b).length && keys.every(key => Object.hasOwn(b, key) && jsonEqual(a[key], b[key], depth + 1)) }
  return false
}
