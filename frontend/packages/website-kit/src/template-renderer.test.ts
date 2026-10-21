import { expect, it } from 'vitest'
import { createSSRApp, defineComponent, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { TemplateCatalog, SchemaCatalog, definitions, templates } from '@b2bweb/component-schemas'
import { ThemeRegistry, baseTheme, defineTheme } from './theme'
const industrialTheme = defineTheme({ ...baseTheme, key: 'industrial', components: { hero: { default: 'default', variants: { default: async () => defineComponent({ props: ['title'], setup: props => () => h('h2', props.title) }) } } } })
import { createTemplateResolver } from './template-renderer'
it('renders the registered template through its named slots and rejects foreign content types', async () => {
 const catalog = new TemplateCatalog(templates, definitions)
 expect(() => catalog.resolve('generic', 'PRODUCT')).toThrow()
 const Resolver = createTemplateResolver({ schemas: new SchemaCatalog(definitions), registry: new ThemeRegistry(definitions, [industrialTheme]), theme: 'industrial', enabledModules: new Set(['page']), siteId: '1', path: '/zh/a' }, catalog)
 const html = await renderToString(createSSRApp({ render: () => h(Resolver, { templateKey: 'generic', contentType: 'PAGE', regions: { main: { version: 1, sections: [{ id: 'intro', component: 'hero', componentVersion: 1, props: { title: 'Escaped <title>', description: 'Description' } }] } } }) }))
 expect(html).toContain('s-intro'); expect(html).toContain('Escaped &lt;title&gt;')
})
it('rejects duplicate and ambiguous regions before rendering', () => {
 expect(() => new TemplateCatalog([{ key: 'bad', version: 1, contentTypes: ['PAGE'], label: 'Bad', regions: [{ key: 'main', fixed: true, slot: true }] }], definitions)).toThrow()
})
