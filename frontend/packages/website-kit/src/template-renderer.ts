import { defineAsyncComponent, defineComponent, h } from 'vue'
import type { PropType, Component } from 'vue'
import { TemplateCatalog } from '@b2bweb/component-schemas'
import type { LayoutDocument, Section } from '@b2bweb/shared/editor-protocol'
import { isLayoutDocument } from '@b2bweb/shared/editor-protocol'
import { createLayoutRenderer } from './render'
import type { RenderContext } from './render'
/** Request-owned template selection. Fixed regions and named slots share the section renderer. */
export function createTemplateResolver(context: RenderContext, catalog: TemplateCatalog): Component {
 const Layout = createLayoutRenderer(context)
 const implementations = new Map<string, Component>()
 return defineComponent({ name: 'TemplateResolver', props: { templateKey: { type: String, required: true }, contentType: { type: String, required: true }, regions: { type: Object as PropType<Record<string, LayoutDocument>>, required: true } }, setup(props) {
  return () => {
   const template = catalog.resolve(props.templateKey, props.contentType)
   const slots: Record<string, () => unknown> = {}
   for (const region of template.regions) {
    let document: LayoutDocument
    if (region.fixed) {
     const definition = context.schemas.component(region.component!)
     if (!definition) throw new Error('Unknown fixed template component')
     document = { version: 1, sections: [{ id: `template-${region.key}`, component: region.component!, componentVersion: definition.version, props: region.props ?? {} }] }
    } else {
     document = props.regions[region.key] ?? { version: 1, sections: structuredClone(region.default ?? []) as Section[] }
     if (!isLayoutDocument(document) || region.allowed !== '*' && document.sections.some(section => !region.allowed?.includes(section.component))) throw new Error('Invalid template slot')
    }
    slots[region.key] = () => h(Layout, { document })
   }
   // Generic template components may use the default slot; named templates consume their regions.
   slots.default = () => template.regions.map(region => slots[region.key]!())
   if (!implementations.has(template.key)) implementations.set(template.key, defineAsyncComponent(context.registry.template(context.theme, template.key)))
   const Template = implementations.get(template.key)!
   return h(Template, { templateKey: template.key }, slots)
  }
 } })
}
