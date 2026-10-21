import { templateDefinitions } from './generated'
import type { ComponentDefinition } from './index'
export interface TemplateSection { id: string; component: string; componentVersion: number; props: Record<string, unknown>; variant?: string }
export interface TemplateRegion { key: string; fixed?: true; slot?: true; component?: string; props?: Record<string, unknown>; allowed?: '*' | readonly string[]; default?: readonly TemplateSection[] }
export interface TemplateDefinition { key: string; version: number; contentTypes: readonly string[]; label: string; regions: readonly TemplateRegion[]; legal?: boolean }
export const templates: readonly TemplateDefinition[] = templateDefinitions as unknown as readonly TemplateDefinition[]
export class TemplateCatalog {
 private readonly entries = new Map<string, TemplateDefinition>()
 constructor(definitions: readonly TemplateDefinition[], components: readonly ComponentDefinition[]) {
  const types = new Set(components.map(component => component.type))
  for (const template of definitions) {
   if (this.entries.has(template.key) || !template.contentTypes.length || !Number.isInteger(template.version) || template.version < 1) throw new Error('Invalid template definition')
   const keys = new Set<string>()
   for (const region of template.regions) {
    if (keys.has(region.key) || Boolean(region.fixed) === Boolean(region.slot) || region.fixed && (!region.component || !types.has(region.component)) || Array.isArray(region.allowed) && region.allowed.some(type => !types.has(type))) throw new Error('Invalid template region')
    keys.add(region.key)
   }
   this.entries.set(template.key, structuredClone(template))
  }
 }
 resolve(key: string, contentType: string): TemplateDefinition {
  const template = this.entries.get(key)
  if (!template || !template.contentTypes.includes(contentType)) throw new Error('Template is not available for this content type')
  return structuredClone(template)
 }
}
