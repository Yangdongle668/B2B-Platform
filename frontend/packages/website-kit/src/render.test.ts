import { it, expect } from 'vitest'
import { createSSRApp, defineComponent, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { SchemaCatalog } from '@b2bweb/component-schemas'
import { baseTheme, defineTheme, ThemeRegistry } from './theme'
import { createLayoutRenderer, sectionClasses } from './render'
import type { LayoutDocument, Section } from '@b2bweb/shared/editor-protocol'
const definition = { type: 'test', version: 1, module: 'page', name: '测试', variants: ['default'], propsSchema: { type: 'object', additionalProperties: false, required: ['title'], properties: { title: { type: 'string' } } }, styleOptions: ['background'] }
const View = defineComponent({ props: ['title', 'priority'], setup: props => () => h('p', { 'data-priority': props.priority }, props.title) })
const theme = defineTheme({ ...baseTheme, key: 'test', components: { test: { default: 'default', variants: { default: async () => View } } } })
const section: Section = { id: 'one', component: 'test', componentVersion: 1, props: { title: '<script>alert(1)</script>' } }
async function render(sections: Section[], enabled = true) {
  const events: string[] = []
  const Renderer = createLayoutRenderer({ schemas: new SchemaCatalog([definition]), registry: new ThemeRegistry([definition], [theme]), theme: 'test', enabledModules: new Set(enabled ? ['page'] : []), siteId: '1', path: '/zh/test', diagnostic: event => events.push(event.code) })
  const document: LayoutDocument = { version: 1, sections }
  return { html: await renderToString(createSSRApp({ render: () => h(Renderer, { document }) })), events }
}
it('SSR escapes content and omits hidden, unknown and disabled components', async () => {
  const result = await render([section, { ...section, id: 'hidden', hidden: true }, { ...section, id: 'unknown', component: 'absent' }])
  expect(result.html).toContain('&lt;script&gt;'); expect(result.html).not.toContain('s-hidden'); expect(result.events).toContain('component.unknown')
  expect((await render([section], false)).html).not.toContain('s-one')
})
it('isolates invalid props, future versions and unsafe styles to their section', async () => {
  const result = await render([{ ...section, id: 'bad', props: { title: 2 } }, { ...section, id: 'future', componentVersion: 2 }, section])
  expect(result.events.filter(code => code === 'section.render_error')).toHaveLength(2); expect(result.html).toContain('s-one'); expect(result.html).not.toContain('s-bad')
  expect(() => sectionClasses({ ...section, style: { background: 'url(evil)' } }, ['background'])).toThrow()
  expect(sectionClasses({ ...section, style: { background: 'panel' }, visibility: { mobile: false } }, ['background'])).toEqual(['b-bg-panel', 'b-hide-mobile'])
})

it('allocates LCP priority in document order including nested slots', async () => {
  const box = { ...definition, type: 'box', children: { slots: ['main'], allowed: '*' as const } }
  const candidate = { ...definition, seo: { lcpCandidate: true } }
  const Box = defineComponent({ setup: (_, { slots }) => () => h('div', slots.main?.()) })
  const nestedTheme = defineTheme({ ...theme, components: { test: theme.components.test!, box: { default: 'default', variants: { default: async () => Box } } } })
  const Renderer = createLayoutRenderer({ schemas: new SchemaCatalog([candidate, box]), registry: new ThemeRegistry([candidate, box], [nestedTheme]), theme: 'test', enabledModules: new Set(['page']), siteId: '1', path: '/' })
  const document: LayoutDocument = { version: 1, sections: [{ id: 'box', component: 'box', componentVersion: 1, props: { title: 'box' }, children: { main: [{ ...section, id: 'nested', props: { title: 'nested' } }] } }, { ...section, id: 'later', props: { title: 'later' } }] }
  const html = await renderToString(createSSRApp({ render: () => h(Renderer, { document }) }))
  expect(html).toMatch(/data-priority="true"[^>]*>nested/); expect(html).toMatch(/data-priority="false"[^>]*>later/)
})

it('contains a throwing SSR component while sibling sections remain available', async () => {
  const Broken = defineComponent({ setup: () => () => { throw new Error('render fixture') } })
  const broken = { ...definition, type: 'broken' }
  const events: string[] = []
  const faultTheme = defineTheme({ ...theme, components: { ...theme.components, broken: { default: 'default', variants: { default: async () => Broken } } } })
  const Renderer = createLayoutRenderer({ schemas: new SchemaCatalog([definition, broken]), registry: new ThemeRegistry([definition, broken], [faultTheme]), theme: 'test', enabledModules: new Set(['page']), siteId: '1', path: '/', diagnostic: event => events.push(event.code) })
  const document: LayoutDocument = { version: 1, sections: [{ ...section, id: 'fault', component: 'broken' }, section] }
  const html = await renderToString(createSSRApp({ render: () => h(Renderer, { document }) }))
  expect(html).toContain('s-one'); expect(events).toContain('section.render_error')
})
