import { defineComponent, h, computed, watch } from 'vue'
import type { Component, PropType, VNode } from 'vue'
import { createValidator, compiledProps } from '@b2bweb/component-schemas'
import { formTexts } from './texts/validation'
import { RichTextEditor } from './richtext-editor'
export interface FormSchema {
  type?: string; title?: string; description?: string; properties?: Record<string, FormSchema>; items?: FormSchema
  enum?: unknown[]; required?: string[]; minItems?: number; maxItems?: number; minimum?: number; maximum?: number; maxLength?: number
  'x-editor'?: string; 'x-secret'?: boolean; 'x-order'?: number; 'x-group'?: string; 'x-enum-labels'?: string[]; 'x-placeholder'?: string
  'x-visible-if'?: { prop: string; equals: unknown }
  [key: string]: unknown
}
export interface SchemaFormContext { locale: string; mode: 'component' | 'settings' | 'data-source'; contentId?: string; contentType?: string; automatic?: boolean }
export interface FormError { field: string; message: string }
export type SchemaControls = Readonly<Record<string, Component>>
function visible(schema: FormSchema, parent: Record<string, unknown>): boolean { const condition = schema['x-visible-if']; return !condition || parent[condition.prop] === condition.equals }
/** Projection used for submission: hidden fields are omitted; blank secrets mean keep the old value. */
export function formSubmission(schema: FormSchema, value: unknown): unknown {
  if (schema.type === 'object' && value !== null && typeof value === 'object' && !Array.isArray(value)) {
    const parent = value as Record<string, unknown>
    const output: Record<string, unknown> = schema.additionalProperties === false ? {} : Object.fromEntries(Object.entries(parent).filter(([key]) => !Object.hasOwn(schema.properties ?? {}, key)))
    for (const [key, child] of Object.entries(schema.properties ?? {})) if (Object.hasOwn(parent, key) && visible(child, parent) && !(child['x-secret'] && (!parent[key] || typeof parent[key] === 'string' && /^\*{4}/u.test(parent[key])))) output[key] = formSubmission(child, parent[key])
    return output
  }
  if (schema.type === 'array' && Array.isArray(value)) return value.map(v => formSubmission(schema.items ?? {}, v))
  return value
}
export function inferEditor(schema: FormSchema): string { return schema['x-editor'] ?? (schema.enum ? 'select' : ({ object: 'group', array: 'repeater', boolean: 'switch', number: 'number', integer: 'number' }[schema.type ?? ''] ?? 'text')) }
export const SchemaForm = defineComponent({
  name: 'SchemaForm', props: {
    modelValue: { type: Object as PropType<Record<string, unknown>>, required: true }, schema: { type: Object as PropType<FormSchema>, required: true },
    context: { type: Object as PropType<SchemaFormContext>, required: true }, readonly: Boolean,
    serverErrors: { type: Array as PropType<FormError[]>, default: () => [] }, controls: { type: Object as PropType<SchemaControls>, default: () => ({}) },
  }, emits: ['update:modelValue', 'validate'], setup(props, { emit, expose }) {
    const validate = computed(() => createValidator().compile(compiledProps(props.schema)))
    const errors = computed<FormError[]>(() => {
      validate.value(props.modelValue)
      return [...(validate.value.errors ?? []).map(e => ({ field: e.instancePath + (e.keyword === 'required' ? `/${e.params.missingProperty}` : ''), message: formTexts[e.keyword] ?? formTexts.invalid! })), ...props.serverErrors]
    })
    watch(errors, current => emit('validate', { valid: current.length === 0, errors: current }), { immediate: true })
    expose({ submission: () => formSubmission(props.schema, props.modelValue) })
    const update = (path: string[], value: unknown) => {
      if (props.readonly || props.context.automatic) return
      const next = JSON.parse(JSON.stringify(props.modelValue)); let target: Record<string, unknown> = next
      for (const key of path.slice(0, -1)) { if (!target[key] || typeof target[key] !== 'object') target[key] = {}; target = target[key] as Record<string, unknown> }
      target[path.at(-1)!] = value; emit('update:modelValue', next)
    }
    const field = (schema: FormSchema, value: unknown, path: string[], parent: Record<string, unknown>): VNode | null => {
      if (!visible(schema, parent)) return null
      const id = `field-${path.join('-')}`, pointer = '/' + path.join('/'), editor = inferEditor(schema), disabled = props.readonly || props.context.automatic
      const messages = errors.value.filter(error => error.field === pointer || error.field === path.join('.'))
      const attrs = { id, disabled, 'aria-invalid': messages.length > 0, 'aria-describedby': `${id}-help`, onChange: (event: Event) => { const input = event.target as HTMLInputElement; update(path, editor === 'switch' ? input.checked : editor === 'number' ? input.value === '' ? undefined : Number(input.value) : input.value) } }
      let input: VNode
      if (editor === 'group') input = h('fieldset', { disabled }, Object.entries(schema.properties ?? {}).sort((a, b) => (a[1]['x-order'] ?? 0) - (b[1]['x-order'] ?? 0)).map(([key, child]) => field(child, (value as Record<string, unknown>)?.[key], [...path, key], (value ?? {}) as Record<string, unknown>)))
      else if (editor === 'repeater') {
        const items = Array.isArray(value) ? value : []
        input = h('div', items.map((item, i) => h('div', { key: i }, [field(schema.items ?? {}, item, [...path, String(i)], {}), h('button', { type: 'button', disabled: disabled || items.length <= (schema.minItems ?? 0), onClick: () => update(path, items.filter((_, index) => i !== index)) }, formTexts.remove), h('button', { type: 'button', disabled: disabled || i === 0, onClick: () => { const next = [...items]; [next[i - 1], next[i]] = [next[i], next[i - 1]]; update(path, next) } }, formTexts.up)] )).concat([h('button', { type: 'button', disabled: disabled || items.length >= (schema.maxItems ?? 500), onClick: () => update(path, [...items, schema.items?.type === 'object' ? {} : '']) }, formTexts.add)]))
      } else if (editor === 'select') input = h('select', { ...attrs, value: JSON.stringify(value), onChange: (e: Event) => update(path, JSON.parse((e.target as HTMLSelectElement).value)) }, [h('option', { value: '', disabled: true }, formTexts.choose), ...(schema.enum ?? []).map((option, i) => h('option', { value: JSON.stringify(option) }, schema['x-enum-labels']?.[i] ?? String(option)))])
      else if (editor === 'textarea') input = h('textarea', { ...attrs, onInput: attrs.onChange, onChange: undefined, value: String(value ?? ''), maxlength: schema.maxLength, placeholder: schema['x-placeholder'] })
      else if (['text', 'number', 'switch'].includes(editor)) input = h('input', { ...attrs, onInput: attrs.onChange, onChange: undefined, type: schema['x-secret'] ? 'password' : editor === 'switch' ? 'checkbox' : editor === 'number' ? 'number' : 'text', value: schema['x-secret'] && typeof value === 'string' && /^\*{4}/u.test(value) ? '' : value, checked: value === true, min: schema.minimum, max: schema.maximum, maxlength: schema.maxLength, autocomplete: schema['x-secret'] ? 'new-password' : undefined, placeholder: schema['x-secret'] ? formTexts.keepSecret : schema['x-placeholder'] })
      else if (editor === 'richtext' || editor === 'richtext-inline') input = h(RichTextEditor, { modelValue: value as never, readonly: disabled, inline: editor === 'richtext-inline', 'onUpdate:modelValue': (next: unknown) => update(path, next) })
      else if (props.controls[editor]) input = h(props.controls[editor]!, { schema, context: props.context, modelValue: value, readonly: disabled, 'onUpdate:modelValue': (next: unknown) => update(path, next) })
      else input = h('p', { role: 'status' }, formTexts.controlUnavailable)
      return h('div', { key: pointer, 'data-field': pointer, 'data-group': schema['x-group'] ?? 'content' }, [h('label', { for: id }, schema.title ?? path.at(-1)), input, h('div', { id: `${id}-help` }, [schema.description, ...messages.map(error => h('p', { role: 'alert' }, error.message))])])
    }
    return () => h('div', { class: 'schema-form' }, Object.entries(props.schema.properties ?? {}).sort((a, b) => (a[1]['x-order'] ?? 0) - (b[1]['x-order'] ?? 0)).map(([key, schema]) => field(schema, props.modelValue[key], [key], props.modelValue)))
  },
})
