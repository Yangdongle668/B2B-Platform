import { defineComponent, h, ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import type { PropType } from 'vue'
import { SchemaForm } from './schema-form'
import type { FormSchema, SchemaFormContext, SchemaControls } from './schema-form'
import { formTexts } from './texts/validation'
export interface ReferenceChoice { key: string; label: string; disabled?: boolean }
export interface ReferenceServices {
  search(kind: string, keyword: string, schema: FormSchema, context: SchemaFormContext): Promise<ReferenceChoice[]>
  sourceSchema(provider: string): Promise<FormSchema>
}
/** A caller must wire actual module endpoints; unavailable providers never become free-text IDs. */
export function referenceControls(services: ReferenceServices): SchemaControls {
  const controls: Record<string, ReturnType<typeof defineComponent>> = {}
  for (const kind of ['media', 'content-ref', 'term-ref', 'key-ref', 'color-token', 'spacing-token', 'link', 'data-source']) {
    controls[kind] = defineComponent({ name: 'SchemaReferenceControl', props: {
      modelValue: { type: [Object, String] as PropType<unknown> }, schema: { type: Object as PropType<FormSchema>, required: true }, context: { type: Object as PropType<SchemaFormContext>, required: true }, readonly: Boolean,
    }, emits: ['update:modelValue'], setup(props, { emit }) {
      const choices = shallowRef<ReferenceChoice[]>([]), paramsSchema = shallowRef<FormSchema>(), loading = ref(false), unavailable = ref(false), keyword = ref(''), external = ref('')
      let sequence = 0, definitionSequence = 0
      const selection = () => {
        const value = props.modelValue
        if (typeof value === 'string') return value
        if (!value || typeof value !== 'object') return ''
        const ref = value as Record<string, unknown>
        if (kind === 'data-source') return String((ref.$source as Record<string, unknown>)?.provider ?? '')
        return String(ref[kind === 'media' ? '$media' : kind === 'term-ref' ? '$term' : '$content'] ?? '')
      }
      async function search() {
        const request = ++sequence; loading.value = true; unavailable.value = false
        try { const result = await services.search(kind, keyword.value, props.schema, props.context); if (request === sequence) choices.value = result }
        catch { if (request === sequence) { choices.value = []; unavailable.value = true } }
        finally { if (request === sequence) loading.value = false }
      }
      async function select(key: string) {
        if (props.readonly || !choices.value.some(item => item.key === key && !item.disabled)) return
        paramsSchema.value = undefined
        if (kind === 'data-source') {
          const request = ++definitionSequence
          try { const schema = await services.sourceSchema(key); if (request !== definitionSequence) return; paramsSchema.value = schema; emit('update:modelValue', { $source: { provider: key, params: {} } }) }
          catch { if (request === definitionSequence) unavailable.value = true }
        } else emit('update:modelValue', ['key-ref', 'color-token', 'spacing-token'].includes(kind) ? key : { [kind === 'media' ? '$media' : kind === 'term-ref' ? '$term' : '$content']: key })
      }
      async function loadExisting() {
        if (kind !== 'data-source' || !selection()) return
        const request = ++definitionSequence
        try { const schema = await services.sourceSchema(selection()); if (request === definitionSequence) paramsSchema.value = schema }
        catch { if (request === definitionSequence) unavailable.value = true }
      }
      watch(() => [props.context, props.schema], () => { definitionSequence++; choices.value = []; paramsSchema.value = undefined; void search(); void loadExisting() }, { immediate: true })
      watch(selection, () => { if (!paramsSchema.value) void loadExisting() })
      onBeforeUnmount(() => { sequence++; definitionSequence++ })
      return () => h('div', [
        h('label', [formTexts.search, h('input', { value: keyword.value, disabled: props.readonly, onChange: (event: Event) => { keyword.value = (event.target as HTMLInputElement).value; void search() } })]),
        loading.value ? h('p', { role: 'status' }, formTexts.loading) : unavailable.value ? h('p', { role: 'status' }, formTexts.controlUnavailable) : h('select', { 'aria-label': props.schema.title, disabled: props.readonly, value: selection(), onChange: (event: Event) => void select((event.target as HTMLSelectElement).value) }, [h('option', { value: '', disabled: true }, formTexts.choose), ...choices.value.map(item => h('option', { value: item.key, disabled: item.disabled }, item.label))]),
        kind === 'link' ? h('label', [formTexts.externalLink, h('input', { type: 'url', value: external.value, disabled: props.readonly, onChange: (event: Event) => { external.value = (event.target as HTMLInputElement).value; try { const url = new URL(external.value); if (!props.readonly && ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password) emit('update:modelValue', { href: url.href, rel: [] }); else unavailable.value = true } catch { unavailable.value = true } } })]) : null,
        kind === 'data-source' && paramsSchema.value ? h(SchemaForm, { schema: paramsSchema.value, modelValue: ((props.modelValue as { $source?: { params?: Record<string, unknown> } })?.$source?.params ?? {}), context: { ...props.context, mode: 'data-source' }, readonly: props.readonly, controls, 'onUpdate:modelValue': (params: unknown) => { if (!props.readonly) emit('update:modelValue', { $source: { provider: selection(), params } }) } }) : null,
      ])
    } })
  }
  return Object.freeze(controls)
}
