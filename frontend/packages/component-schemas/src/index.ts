import Ajv2020 from 'ajv/dist/2020.js'
import hero from '../components/page/hero.json'
import { componentDefinitions } from './generated'
import common from '../defs/common.json'
export const heroSchema = hero.propsSchema
export interface ComponentDefinition {
 type: string; version: number; module: string; name?: string; label?: string; allowedIn?: readonly string[]; variants: readonly string[]
 propsSchema: Record<string, unknown>; styleOptions?: readonly string[]; seo?: { lcpCandidate: boolean }
 variantAliases?: Record<string, string>; children?: { slots: string[]; allowed: '*' | string[] }
}
export const definitions: readonly ComponentDefinition[] = Object.freeze(componentDefinitions)
export function compiledProps(schema: Record<string, unknown>): Record<string, unknown> {
 const copy = JSON.parse(JSON.stringify(schema))
 const visit = (node: unknown): void => { if (!node || typeof node !== 'object') return; if (Array.isArray(node)) { node.forEach(visit); return }; const value = node as Record<string, unknown>; if ('$id' in value) throw new Error('Nested schema identifier forbidden'); if (typeof value.$ref === 'string') { if (value.$ref.startsWith('../../defs/common.json#/$defs/')) value.$ref = value.$ref.replace('../../defs/common.json', ''); else if (!value.$ref.startsWith('#/$defs/')) throw new Error('External schema reference forbidden') }; Object.values(value).forEach(visit) }
 visit(copy); for (const key of Object.keys(copy.$defs ?? {})) if (Object.hasOwn(common.$defs, key)) throw new Error('Shared schema definition override forbidden'); copy.$defs = { ...common.$defs, ...copy.$defs }; return copy
}
export function createValidator(): Ajv2020 {
 const ajv = new Ajv2020({ allErrors: true, strict: true })
 for (const keyword of ['x-editor','x-translatable','x-placeholder','x-group','x-order','x-visible-if','x-secret','x-enum-labels','x-media','x-content','x-term','x-ref','x-source','x-seo']) ajv.addKeyword({ keyword, valid: true })
 return ajv
}
export type PropsMigration = (props: Record<string, unknown>) => Record<string, unknown>
export function defineMigrations(type: string, steps: Record<number, PropsMigration>): Record<number, PropsMigration> {
 if (!/^[a-z][a-z0-9-]*$/.test(type) || Object.entries(steps).some(([version, step]) => !/^[1-9][0-9]*$/.test(version) || typeof step !== 'function')) throw new Error('Invalid component migration registry')
 return Object.freeze({ ...steps })
}
function freezeProps(value: unknown): void { if (value && typeof value === 'object') { Object.values(value).forEach(freezeProps); Object.freeze(value) } }
function references(value: unknown): Set<string> {
 const result = new Set<string>()
 const visit = (node: unknown): void => { if (!node || typeof node !== 'object') return; for (const [key, child] of Object.entries(node)) { if (['$media', '$content', '$term', '$source'].includes(key)) result.add(`${key}:${JSON.stringify(child)}`); visit(child) } }
 visit(value); return result
}
export class SchemaCatalog {
 private readonly entries = new Map<string, ComponentDefinition>()
 private readonly validators = new Map<string, import('ajv').ValidateFunction>()
 constructor(defs: readonly ComponentDefinition[], private readonly migrations: Record<string, Record<number, PropsMigration>> = {}) {
  const ajv = createValidator()
  for (const def of defs) {
   if (this.entries.has(def.type) || !def.module || !Number.isInteger(def.version) || def.version < 1 || !def.variants.length) throw new Error('Invalid component definition')
   for (let version = 1; version < def.version; version++) if (!migrations[def.type]?.[version]) throw new Error('Missing component migration')
   this.entries.set(def.type, def); this.validators.set(def.type, ajv.compile(compiledProps(def.propsSchema)))
  }
 }
 component(type: string): ComponentDefinition | undefined { return this.entries.get(type) }
 validate(type: string, props: unknown): boolean { const validate = this.validators.get(type); if (!validate) throw new Error('Unknown component type'); return Boolean(validate(props)) }
 errors(type: string): readonly import('ajv').ErrorObject[] { return [...(this.validators.get(type)?.errors ?? [])] }
 migrate(type: string, props: unknown, fromVersion: number): { props: Record<string, unknown>; version: number } {
  const def = this.entries.get(type)
  if (!def || !Number.isInteger(fromVersion) || fromVersion < 1 || fromVersion > def.version || props === null || typeof props !== 'object' || Array.isArray(props)) throw new Error('Invalid component version or props')
  let next = structuredClone(props as Record<string, unknown>)
  for (let version = fromVersion; version < def.version; version++) {
   const before = references(next); freezeProps(next)
   const result = this.migrations[type]![version]!(next)
   if (!result || typeof result !== 'object' || Array.isArray(result) || result instanceof Promise) throw new Error('Invalid component migration result')
   const after = references(result)
   if ([...before].some(reference => !after.has(reference))) throw new Error('Component migration discarded a reference')
   next = structuredClone(result)
  }
  if (!this.validate(type, next)) throw new Error('Invalid migrated props')
  return { props: next, version: def.version }
 }
 resolveVariant(type: string, variant: string): string { const def = this.entries.get(type); return def?.variantAliases?.[variant] ?? variant }
}
export const schemaCatalog = new SchemaCatalog(definitions)
export function validateComponent(type: string, props: unknown): boolean { return schemaCatalog.validate(type, props) }
