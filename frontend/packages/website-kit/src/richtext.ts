import { defineComponent, h } from 'vue'
import type { Component, PropType, VNode } from 'vue'
import { renderRichText } from '@b2bweb/shared/richtext'
import type { RichTextNode } from '@b2bweb/shared/richtext'
export const RichTextRenderer = defineComponent({ name: 'RichTextRenderer', props: { document: { type: Object as PropType<RichTextNode>, required: true }, resolveLink: { type: Function as PropType<(id: string) => string | undefined>, required: true }, components: { type: Object as PropType<Record<string, Component>>, default: () => ({}) }, headingOffset: { type: Number, default: 0 } }, setup: props => () => h('div', { class: 'b-richtext' }, renderRichText<VNode>(props.document, { h: (tag, attrs, children) => h(tag, attrs, children), resolveLink: props.resolveLink, headingOffset: props.headingOffset, components: Object.fromEntries(Object.entries(props.components).map(([type, component]) => [type, (node: RichTextNode) => h(component, { node })])) })) })
