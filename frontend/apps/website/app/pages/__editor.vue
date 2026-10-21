<script setup lang="ts">
import { useSeoHead } from '../composables/useSeoHead'
import { useRuntimeConfig } from '#imports'
import { onMounted, onBeforeUnmount, shallowRef, ref, watch, nextTick } from 'vue'
import { acceptEditorMessage, envelope, isLayoutDocument, patchDocument } from '@b2bweb/shared/editor-protocol'
import type { EditorEnvelope, LayoutDocument } from '@b2bweb/shared/editor-protocol'
import { createLayoutRenderer, text } from '@b2bweb/website-kit'
import { schemaCatalog } from '@b2bweb/component-schemas'
import { themeRegistry } from '../theme'
import texts from '../texts/zh-CN'
const token = String(useRoute().query.token ?? ''), adminOrigin = useRuntimeConfig().public.adminOrigin
const document = shallowRef<LayoutDocument>({ version: 1, sections: [] }), renderer = shallowRef<ReturnType<typeof createLayoutRenderer>>()
const selected = ref<string | null>(null), failure = ref(false), readonly = ref(false), authorized = ref(false)
let pendingLayout: LayoutDocument | undefined
let sequence = 0, timer: ReturnType<typeof setTimeout> | undefined, refresh: ReturnType<typeof setInterval> | undefined, stopped = false
useSeoHead({}, true)
function send(message: EditorEnvelope) { if (window.parent !== window) window.parent.postMessage(message, adminOrigin) }
function fail() { sequence++; authorized.value = false; failure.value = true; renderer.value = undefined; document.value = { version: 1, sections: [] }; send(envelope('preview:error', { sectionId: null, message: text(texts, 'preview.unavailable') })) }
interface PreviewData { snapshot: { payload: { parts?: { layout?: unknown } } }; siteId: string; enabledModules: string[]; readonly: boolean; locale: string }
async function resolve(layout?: LayoutDocument): Promise<void> {
  const current = ++sequence
  try {
    const response = await fetch('/__editor/resolve', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token, partOverrides: layout ? { layout } : {} }), cache: 'no-store' })
    if (!response.ok) throw new Error()
    const data = await response.json() as PreviewData
    if (stopped || current !== sequence) return
    readonly.value = data.readonly; authorized.value = true; failure.value = false
    const next = layout ?? data.snapshot.payload.parts?.layout
    if (next !== undefined && !isLayoutDocument(next)) throw new Error()
    if (next) document.value = next
    pendingLayout = undefined
    renderer.value = createLayoutRenderer({ schemas: schemaCatalog, registry: themeRegistry, theme: 'industrial', enabledModules: new Set(data.enabledModules), siteId: data.siteId, path: '/__editor', diagnostic: event => send(envelope('preview:error', { sectionId: event.sectionId, message: text(texts, 'preview.renderError') })) })
    if (!layout) send(envelope('preview:ready', { protocolVersion: 1 }))
  } catch { if (!stopped && current === sequence) fail() }
}
function signature(value: unknown): string { return JSON.stringify(value, (key, item) => key === 'props' ? references(item) : key === 'style' || key === 'visibility' || key === 'hidden' ? undefined : item) }
function references(value: unknown): unknown { if (Array.isArray(value)) return value.map(references); if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).filter(([key, item]) => key.startsWith('$') || item && typeof item === 'object').map(([key, item]) => [key, key.startsWith('$') ? item : references(item)])); return value }
function receive(event: MessageEvent) {
  if (event.source !== window.parent || event.origin !== adminOrigin) return
  if (event.data?.source === 'b2bweb-editor' && event.data.version !== 1) { stopped = true; fail(); return }
  if (!authorized.value || !acceptEditorMessage(event, adminOrigin, window.parent, 'editor')) return
  const message = event.data
  if (message.type === 'editor:select' || message.type === 'editor:scrollTo') { selected.value = message.payload.sectionId; if (message.type === 'editor:scrollTo' && selected.value) window.document.getElementById(`s-${selected.value}`)?.scrollIntoView({ block: 'center' }); return }
  try {
    const next = message.type === 'editor:init' ? message.payload.document : message.type === 'editor:patch' ? patchDocument(pendingLayout ?? document.value, message.payload.ops) : undefined
    if (!next || readonly.value && message.type !== 'editor:init') return
    if (readonly.value) return
    const reparse = pendingLayout !== undefined || message.type === 'editor:init' || signature(next) !== signature(document.value)
    sequence++
    if (reparse) { pendingLayout = next; clearTimeout(timer); timer = setTimeout(() => void resolve(next), 300) }
    else document.value = next
  } catch { send(envelope('preview:error', { sectionId: null, message: text(texts, 'preview.invalidLayout') })) }
}
watch([selected, document], async () => { await nextTick(); for (const element of window.document.querySelectorAll<HTMLElement>('[data-section-id]')) element.classList.toggle('b-editor-selected', element.dataset.sectionId === selected.value) })
function hover(event: MouseEvent) { const target = (event.target as Element).closest<HTMLElement>('[data-section-id]'); send(envelope('preview:hover', { sectionId: target?.dataset.sectionId ?? null })) }
function insert(index: number) { if (!readonly.value && authorized.value) send(envelope('preview:insert', { parentId: null, slot: null, index })) }
function select(event: MouseEvent) { const target = (event.target as Element).closest<HTMLElement>('[data-section-id]'); event.preventDefault(); if (target) { selected.value = target.dataset.sectionId ?? null; send(envelope('preview:select', { sectionId: selected.value })) } }
onMounted(() => { window.addEventListener('message', receive); void resolve(); refresh = setInterval(() => { if (!stopped) void resolve(readonly.value ? undefined : pendingLayout ?? document.value) }, 30_000) })
onBeforeUnmount(() => { stopped = true; sequence++; clearTimeout(timer); clearInterval(refresh); window.removeEventListener('message', receive) })
</script>
<template><main @click.capture="select" @mouseover="hover"><p v-if="failure" role="alert">{{ text(texts, 'preview.unavailable') }}</p><component :is="renderer" v-else-if="renderer" :document="document" /><p v-else role="status">{{ text(texts, 'preview.loading') }}</p><nav v-if="authorized && !readonly" :aria-label="text(texts, 'preview.insert')"><button v-for="(_, index) in document.sections.length + 1" :key="index" type="button" @click.stop="insert(index)">{{ text(texts, 'preview.insert') }} {{ index + 1 }}</button></nav></main></template>
