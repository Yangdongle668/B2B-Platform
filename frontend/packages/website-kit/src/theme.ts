import { defineAsyncComponent, defineComponent, hydrateOnIdle, hydrateOnInteraction, hydrateOnVisible } from 'vue'
import type { Component, HydrationStrategy } from 'vue'
import { kitTexts } from './texts/zh-CN'

export type AsyncComponent = () => Promise<Component>
export type HydrationMode = 'none' | 'visible' | 'idle' | 'interaction'
export interface VariantSet {
  default: string
  variants: Record<string, AsyncComponent | { component: AsyncComponent; hydrate?: HydrationMode }>
}
export interface ThemeDefinition {
  key: string
  label: string
  extends?: 'base'
  layouts: Record<'default' | 'landing' | 'blank', AsyncComponent>
  header: VariantSet
  footer: VariantSet
  templates: Record<string, AsyncComponent>
  components: Record<string, VariantSet>
  texts: () => Promise<Record<string, string>>
}
export interface ComponentCatalogEntry { type: string; variants: readonly string[] }
export interface ThemeDiagnostic { code: 'theme.missing' | 'variant.fallback' | 'variant.missing' | 'template.missing'; theme: string; type?: string; variant?: string }
export interface ResolvedVariant { component: AsyncComponent; hydrate: HydrationMode; theme: string; variant: string }

export function defineTheme(definition: ThemeDefinition): ThemeDefinition {
  if (!/^[a-z][a-z0-9-]*$/.test(definition.key) || !definition.label) throw new Error('Invalid theme identity')
  for (const set of [definition.header, definition.footer, ...Object.values(definition.components)]) {
    if (!set.default || !set.variants[set.default]) throw new Error('Missing default theme variant')
    for (const variant of Object.values(set.variants)) {
      if (typeof variant !== 'function' && (typeof variant.component !== 'function' || variant.hydrate && !['none', 'visible', 'idle', 'interaction'].includes(variant.hydrate))) throw new Error('Invalid theme component')
    }
  }
  return Object.freeze(definition)
}

const NeutralLayout = defineComponent({ name: 'BaseLayout', inheritAttrs: false, setup: (_, { slots }) => () => slots.default?.() })
const EmptyRegion = defineComponent({ name: 'BaseEmptyRegion', setup: () => () => null })
const emptyRegion: VariantSet = { default: 'default', variants: { default: async () => EmptyRegion } }
export const baseTheme = defineTheme({ key: 'base', label: kitTexts.baseTheme, layouts: { default: async () => NeutralLayout, landing: async () => NeutralLayout, blank: async () => NeutralLayout }, header: emptyRegion, footer: emptyRegion, templates: { generic: async () => NeutralLayout }, components: {}, texts: async () => ({}) })

export class ThemeRegistry {
  private readonly themes = new Map<string, ThemeDefinition>()
  private readonly definitions: Map<string, ComponentCatalogEntry>
  constructor(catalog: readonly ComponentCatalogEntry[], themes: readonly ThemeDefinition[], private readonly diagnostic: (event: ThemeDiagnostic) => void = () => {}, private readonly base: ThemeDefinition = baseTheme) {
    this.definitions = new Map(catalog.map(definition => [definition.type, definition]))
    if (this.definitions.size !== catalog.length || base.key !== 'base' || !base.templates.generic) throw new Error('Invalid base theme or component catalog')
    for (const theme of [base, ...themes]) {
      if (this.themes.has(theme.key)) throw new Error('Duplicate theme key')
      defineTheme(theme)
      for (const [type, set] of Object.entries(theme.components)) {
        const definition = this.definitions.get(type)
        if (!definition || Object.keys(set.variants).some(variant => !definition.variants.includes(variant))) throw new Error('Theme variant is absent from component schemas')
      }
      this.themes.set(theme.key, theme)
    }
  }
  private theme(key: string): ThemeDefinition {
    const theme = this.themes.get(key)
    if (!theme) this.diagnostic({ code: 'theme.missing', theme: key })
    return theme ?? this.base
  }
  private resolve(theme: ThemeDefinition, type: string, requested: string | undefined, current: VariantSet | undefined, base: VariantSet | undefined, allowed?: readonly string[]): ResolvedVariant | null {
    const candidates: Array<[ThemeDefinition, VariantSet | undefined, string | undefined]> = [[theme, current, requested], [theme, current, current?.default], [this.base, base, requested], [this.base, base, base?.default]]
    for (let index = 0; index < candidates.length; index++) {
      const [owner, set, variant] = candidates[index]!
      if (!variant || allowed && !allowed.includes(variant) || !set?.variants[variant]) continue
      const implementation = set.variants[variant]!
      if (index > 0 && (requested || owner !== theme)) this.diagnostic({ code: 'variant.fallback', theme: theme.key, type, variant: requested })
      return { component: typeof implementation === 'function' ? implementation : implementation.component, hydrate: typeof implementation === 'function' ? 'none' : implementation.hydrate ?? 'none', theme: owner.key, variant }
    }
    this.diagnostic({ code: 'variant.missing', theme: theme.key, type, variant: requested })
    return null
  }
  resolveComponent(themeKey: string, type: string, variant?: string): ResolvedVariant | null {
    const theme = this.theme(themeKey)
    const definition = this.definitions.get(type)
    if (!definition) { this.diagnostic({ code: 'variant.missing', theme: themeKey, type, variant }); return null }
    return this.resolve(theme, type, variant, theme.components[type], this.base.components[type], definition.variants)
  }
  region(themeKey: string, region: 'header' | 'footer', variant?: string): ResolvedVariant | null {
    const theme = this.theme(themeKey)
    return this.resolve(theme, region, variant, theme[region], this.base[region])
  }
  template(themeKey: string, key: string): AsyncComponent {
    const theme = this.theme(themeKey)
    const implementation = theme.templates[key] ?? this.base.templates[key]
    if (implementation) return implementation
    this.diagnostic({ code: 'template.missing', theme: theme.key, type: key })
    return this.base.templates.generic!
  }
  layout(themeKey: string, key: 'default' | 'landing' | 'blank'): AsyncComponent { return this.theme(themeKey).layouts[key] ?? this.base.layouts[key] }
}

/** Static variants leave SSR HTML intact; interactive variants hydrate using the declared strategy. */
export function themeComponent(resolved: ResolvedVariant): Component {
  const strategies: Record<HydrationMode, () => HydrationStrategy> = { none: () => () => {}, visible: () => hydrateOnVisible(), idle: () => hydrateOnIdle(), interaction: () => hydrateOnInteraction(['click', 'keydown', 'focusin']) }
  return defineAsyncComponent({ loader: resolved.component, hydrate: strategies[resolved.hydrate]() })
}
