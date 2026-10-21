import { defineComponent, h, onErrorCaptured, ref } from 'vue'
import type { Component, PropType, VNode } from 'vue'
import type { LayoutDocument, Section } from '@b2bweb/shared/editor-protocol'
import { styleValues } from '@b2bweb/design-tokens'
import { SchemaCatalog } from '@b2bweb/component-schemas'
import type { ThemeRegistry } from './theme'
import { themeComponent } from './theme'
export interface RenderDiagnostic { code: string; sectionId: string; component: string; siteId: string; path: string }
export interface RenderContext { schemas: SchemaCatalog; registry: ThemeRegistry; theme: string; enabledModules: ReadonlySet<string>; siteId: string; path: string; headingLevel?: 1 | 2; dataSources?: Record<string, unknown>; diagnostic?: (event: RenderDiagnostic) => void }
export function sectionClasses(section: Section, allowed: readonly string[]): string[] {
  const classes: string[] = []
  for (const [key, value] of Object.entries(section.style ?? {})) {
    if (!allowed.includes(key) || !styleValues[key]?.includes(value)) throw new Error('Invalid section style')
    classes.push(`b-${{ background: 'bg', paddingY: 'py', container: 'container' }[key]}-${value}`)
  }
  for (const device of ['mobile', 'tablet', 'desktop'] as const) if (section.visibility?.[device] === false) classes.push(`b-hide-${device}`)
  return classes
}
const SectionBoundary = defineComponent({ name: 'SectionBoundary', props: { failure: { type: Function as PropType<() => void>, required: true } }, setup(props, { slots }) { const failed = ref(false); onErrorCaptured(() => { failed.value = true; props.failure(); return false }); return () => failed.value ? null : slots.default?.() } })
/** Each renderer owns its component cache; no site state is shared across SSR requests. */
export function createLayoutRenderer(context: RenderContext): Component {
  const implementations = new Map<string, Component>()
  const report = (section: Section, code: string) => context.diagnostic?.({ code, sectionId: section.id, component: section.component, siteId: context.siteId, path: context.path })
  return defineComponent({ name: 'LayoutRenderer', props: { document: { type: Object as PropType<LayoutDocument>, required: true } }, setup(props) {
    return () => {
      let lcpClaimed = false
      const sectionView = (section: Section, depth: number): VNode | null => {
        if (section.hidden) return null
        const def = context.schemas.component(section.component)
        if (!def) { report(section, 'component.unknown'); return null }
        if (!context.enabledModules.has(def.module)) return null
        try {
          if (depth > 2) throw new Error('Section nesting exceeded')
          const migrated = context.schemas.migrate(section.component, section.props, section.componentVersion)
          const resolved = context.registry.resolveComponent(context.theme, section.component, context.schemas.resolveVariant(section.component, section.variant ?? def.variants[0]!))
          if (!resolved) { report(section, 'variant.missing'); return null }
          const key = `${resolved.theme}:${section.component}:${resolved.variant}`
          if (!implementations.has(key)) implementations.set(key, themeComponent(resolved))
          const classes = sectionClasses(section, def.styleOptions ?? [])
          for (const [name, children] of Object.entries(section.children ?? {})) if (!def.children?.slots.includes(name) || def.children.allowed !== '*' && children.some(child => !def.children!.allowed.includes(child.component))) throw new Error('Invalid children slot')
          const priority = !lcpClaimed && def.seo?.lcpCandidate === true
          if (priority) lcpClaimed = true
          const slots: Record<string, () => Array<VNode | null>> = {}
          for (const [name, children] of Object.entries(section.children ?? {})) {
            const views = children.map(child => sectionView(child, depth + 1))
            slots[name] = () => views
          }
          return h(SectionBoundary, { key: section.id, failure: () => report(section, 'section.render_error') }, { default: () => h('section', { id: `s-${section.id}`, 'data-section-id': section.id, class: classes }, [h(implementations.get(key)!, { ...migrated.props, props: migrated.props, sectionId: section.id, variant: resolved.variant, data: context.dataSources?.[section.id], ...(context.headingLevel ? { headingLevel: context.headingLevel } : {}), priority }, slots)]) })
        } catch { report(section, 'section.render_error'); return null }
      }
      return h('div', { class: 'b-layout' }, props.document.sections.map(section => sectionView(section, 0)))
    }
  } })
}
