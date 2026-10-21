import { defineComponent, h, onMounted, onBeforeUnmount, shallowRef, watch } from 'vue'
import type { PropType } from 'vue'
import { Editor, EditorContent, Node } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { safeLink } from '@b2bweb/shared/richtext'
import type { RichTextNode } from '@b2bweb/shared/richtext'
import { formTexts } from './texts/validation'
const InlineDocument = Node.create({ name: 'doc', topNode: true, content: 'inline*' })
export const RichTextEditor = defineComponent({ name: 'RichTextEditor', props: { modelValue: { type: Object as PropType<RichTextNode> }, readonly: Boolean, inline: Boolean }, emits: ['update:modelValue'], setup(props, { emit }) {
  const editor = shallowRef<Editor>()
  onMounted(() => {
    const starter = StarterKit.configure({ underline: false, link: { openOnClick: false, autolink: false, isAllowedUri: url => Boolean(safeLink(url, () => '/zh/placeholder')) }, ...(props.inline ? { document: false, paragraph: false, heading: false, blockquote: false, bulletList: false, orderedList: false, listItem: false, listKeymap: false, codeBlock: false, horizontalRule: false } : {}) })
    editor.value = new Editor({ extensions: props.inline ? [InlineDocument, starter] : [starter], content: props.modelValue ?? { type: 'doc', content: props.inline ? [] : [{ type: 'paragraph' }] }, editable: !props.readonly, onUpdate: ({ editor }) => emit('update:modelValue', editor.getJSON()) })
  })
  watch(() => props.readonly, value => editor.value?.setEditable(!value))
  watch(() => props.modelValue, value => { if (value && JSON.stringify(value) !== JSON.stringify(editor.value?.getJSON())) editor.value?.commands.setContent(value, { emitUpdate: false }) }, { deep: true })
  onBeforeUnmount(() => editor.value?.destroy())
  return () => h('div', { class: 'richtext-editor' }, [h('div', { role: 'toolbar', 'aria-label': formTexts.richtext }, [h('button', { type: 'button', disabled: props.readonly, onClick: () => editor.value?.chain().focus().toggleBold().run() }, formTexts.bold), h('button', { type: 'button', disabled: props.readonly, onClick: () => editor.value?.chain().focus().toggleItalic().run() }, formTexts.italic)]), h(EditorContent, { editor: editor.value })])
} })
