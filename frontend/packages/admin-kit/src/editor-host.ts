import { computed, defineComponent, h, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import type { PropType } from 'vue'
import { acceptEditorMessage, envelope } from '@b2bweb/shared/editor-protocol'
import type { EditorEnvelope, LayoutDocument } from '@b2bweb/shared/editor-protocol'
import { formTexts } from './texts/validation'
function canvasURL(value: string): URL | undefined {
  try {
    const url = new URL(value)
    if (url.username || url.password || url.hash || url.pathname !== '/__editor' || url.protocol !== 'https:' && !(url.protocol === 'http:' && ['localhost', '127.0.0.1'].includes(url.hostname))) return undefined
    return url
  } catch { return undefined }
}
export const EditorCanvasHost = defineComponent({ name: 'EditorCanvasHost', props: { url: { type: String, required: true }, title: { type: String, required: true }, document: { type: Object as PropType<LayoutDocument>, required: true }, locale: { type: String, required: true }, device: { type: String as PropType<'desktop' | 'tablet' | 'mobile'>, default: 'desktop' } }, emits: ['message'], setup(props, { emit, expose }) {
  const frame = ref<HTMLIFrameElement>(); const ready = ref(false)
  const target = computed(() => canvasURL(props.url))
  const send = (message: EditorEnvelope) => { if (target.value) frame.value?.contentWindow?.postMessage(message, target.value.origin) }
  const initialize = () => { if (ready.value) send(envelope('editor:init', { document: JSON.parse(JSON.stringify(props.document)), locale: props.locale, device: props.device })) }
  const receive = (event: MessageEvent) => {
    if (!target.value || !acceptEditorMessage(event, target.value.origin, frame.value?.contentWindow ?? null, 'preview')) return
    if (event.data.type === 'preview:ready') { ready.value = true; initialize() }
    emit('message', event.data)
  }
  watch(() => props.url, () => { ready.value = false })
  watch(() => [props.document, props.locale, props.device], initialize, { deep: true })
  onMounted(() => window.addEventListener('message', receive)); onBeforeUnmount(() => { ready.value = false; window.removeEventListener('message', receive) })
  expose({ send })
  return () => target.value ? h('iframe', { ref: frame, src: target.value.href, title: props.title, sandbox: 'allow-scripts allow-same-origin', referrerpolicy: 'no-referrer' }) : h('p', { role: 'alert' }, formTexts.invalidCanvas)
} })
