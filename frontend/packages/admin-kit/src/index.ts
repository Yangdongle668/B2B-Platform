import type { Component } from 'vue'
export { AdminRuntime, adminRuntimeKey, useAdminRuntime } from './runtime'
export type { SiteBootstrap, PermissionSnapshot, RegisteredAdminRoute, AdminRouteRegistry } from './runtime'
export interface AdminRoute { path: string; name: string; component: () => Promise<Component>; meta: { title: string; permission?: string; menuKey?: string; keepAlive?: boolean } }
export interface ContentTabDefinition { module: string; key: string; label: string; contentTypes: '*' | string[]; permission?: string; locales?: 'source' | 'target' | 'all'; order?: number; component: () => Promise<Component> }
export interface DashboardWidgetDefinition { key: string; title: string; permission?: string; size: 'sm' | 'md' | 'lg'; order?: number; component: () => Promise<Component> }
export interface RefSourceDefinition { kind: string; label: string; search: (keyword: string) => Promise<Array<{ key: string; label: string }>> }
export interface AdminModuleContext { siteId: string; enabledModules: ReadonlySet<string>; hasPermission: (permission: string) => boolean }
export interface AdminModuleDefinition { id: string; apiPrefix?: string; routes: AdminRoute[]; contentTabs?: ContentTabDefinition[]; widgets?: DashboardWidgetDefinition[]; refSources?: RefSourceDefinition[]; setup?: (ctx: AdminModuleContext) => void }
export function defineAdminModule(definition: AdminModuleDefinition): AdminModuleDefinition {
  if (!/^[a-z][a-z0-9]{1,31}$/.test(definition.id)) throw new Error('Invalid module id')
  const prefix = definition.apiPrefix ?? definition.id
  if (prefix !== (definition.id === 'core' ? 'system' : definition.id)) throw new Error('Invalid API prefix')
  const names = new Set<string>()
  for (const route of definition.routes) {
    if (!route.name.startsWith(`${prefix}.`) || route.path.startsWith('/') || route.path.includes('..') || names.has(route.name)) throw new Error('Invalid module route')
    if (route.meta.permission && !/^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$/.test(route.meta.permission)) throw new Error('Invalid module permission')
    names.add(route.name)
  }
  for (const tab of definition.contentTabs ?? []) if (tab.module !== definition.id) throw new Error('Invalid tab owner')
  const localKeys = [...(definition.contentTabs ?? []).map(item => item.key), ...(definition.widgets ?? []).map(item => item.key)]
  if (new Set(localKeys).size !== localKeys.length) throw new Error('Duplicate module contribution key')
  return Object.freeze(definition)
}
export function enabledModules(modules: AdminModuleDefinition[], enabled: ReadonlySet<string>): AdminModuleDefinition[] { return modules.filter(module => enabled.has(module.id)) }
export { SchemaForm, formSubmission, inferEditor } from './schema-form'
export type { FormSchema, SchemaFormContext, SchemaControls } from './schema-form'
export { EditorCanvasHost } from './editor-host'
export { RichTextEditor } from './richtext-editor'
export { referenceControls } from './reference-controls'
export type { ReferenceServices, ReferenceChoice } from './reference-controls'
