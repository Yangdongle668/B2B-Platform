import type { AdminModuleDefinition, AdminModuleContext, AdminRoute, ContentTabDefinition, DashboardWidgetDefinition, RefSourceDefinition } from './index'
import { inject } from 'vue'
import type { InjectionKey } from 'vue'

export const adminRuntimeKey: InjectionKey<AdminRuntime> = Symbol('AdminRuntime')
export function useAdminRuntime(): AdminRuntime {
  const runtime = inject(adminRuntimeKey)
  if (!runtime) throw new Error('Admin runtime is not provided')
  return runtime
}

export interface PermissionSnapshot { code: string; dataScope: 'ALL' | 'GROUP' | 'OWN' | null; localeScope: { unrestricted: boolean; locales: string[] } }
export interface SiteBootstrap {
  site: { siteId: string; status: string }
  modules: Array<{ moduleId: string; name: string }>
  permissions: PermissionSnapshot[]
  menus: Array<{ key: string; name: string; route: string | null }>
}
export interface RegisteredAdminRoute extends AdminRoute { moduleId: string }
export interface AdminRouteRegistry { add(route: RegisteredAdminRoute): () => void }

/** One runtime per browser tab. A pending or failed switch never exposes the previous site's data. */
export class AdminRuntime {
  private revision = 0
  private removers: Array<() => void> = []
  private resets = new Set<() => void>()
  private permissions = new Map<string, PermissionSnapshot>()
  private active: AdminModuleDefinition[] = []
  private snapshot?: SiteBootstrap
  private routes = new Map<string, RegisteredAdminRoute>()

  constructor(private readonly modules: readonly AdminModuleDefinition[], private readonly registry: AdminRouteRegistry) {
    if (new Set(modules.map(module => module.id)).size !== modules.length) throw new Error('Duplicate admin module')
  }

  get siteId(): string | undefined { return this.snapshot?.site.siteId }
  get readonly(): boolean { return this.snapshot?.site.status === 'ARCHIVED' }
  get ready(): boolean { return this.snapshot !== undefined }
  hasPermission = (permission: string): boolean => this.permissions.has(permission)
  dataScope(permission: string): PermissionSnapshot['dataScope'] { return this.permissions.get(permission)?.dataScope ?? null }
  canAccessLocale(permission: string, locale: string): boolean {
    const scope = this.permissions.get(permission)?.localeScope
    return Boolean(scope && (scope.unrestricted || scope.locales.includes(locale)))
  }
  onSiteReset(reset: () => void): () => void { this.resets.add(reset); return () => { this.resets.delete(reset) } }

  clear(): void {
    this.revision++
    this.snapshot = undefined
    this.permissions.clear()
    this.active = []
    this.routes.clear()
    for (const remove of this.removers.splice(0)) remove()
    for (const reset of this.resets) reset()
  }

  async switchSite(siteId: string, load: () => Promise<SiteBootstrap>): Promise<SiteBootstrap | undefined> {
    this.clear()
    const revision = this.revision
    const loaded = await load()
    if (revision !== this.revision) return undefined
    if (loaded.site.siteId !== siteId) throw new Error('Bootstrap site mismatch')
    const snapshot = structuredClone(loaded)
    const enabled = new Set(snapshot.modules.map(module => module.moduleId))
    this.permissions = new Map(snapshot.permissions.map(permission => [permission.code, permission]))
    this.active = this.modules.filter(module => enabled.has(module.id))
    const context: AdminModuleContext = { siteId, enabledModules: enabled, hasPermission: permission => revision === this.revision && this.hasPermission(permission) }
    try {
      for (const module of this.active) {
        const prefix = module.apiPrefix ?? module.id
        for (const route of module.routes) {
          if (this.routes.has(route.name)) throw new Error('Duplicate admin route')
          const registered = { ...route, path: `/${prefix}/${route.path}`, moduleId: module.id }
          this.routes.set(route.name, registered)
          this.removers.push(this.registry.add(registered))
        }
      }
      this.snapshot = snapshot
      for (const module of this.active) module.setup?.(context)
      return snapshot
    } catch (error) { this.clear(); throw error }
  }

  access(name: string, authenticated: boolean): 'login' | 'not-found' | 'forbidden' | 'allowed' {
    if (!authenticated) return 'login'
    const route = this.routes.get(name)
    if (!this.ready || !route) return 'not-found'
    return route.meta.permission && !this.hasPermission(route.meta.permission) ? 'forbidden' : 'allowed'
  }
  menus(): SiteBootstrap['menus'] {
    const paths = new Set([...this.routes.values()].filter(route => this.access(route.name, true) === 'allowed').map(route => route.path))
    return this.snapshot?.menus.filter(menu => menu.route !== null && paths.has(menu.route)) ?? []
  }
  contentTabs(contentType: string, isSource: boolean): ContentTabDefinition[] {
    return this.active.flatMap(module => module.contentTabs ?? []).filter(tab =>
      (!tab.permission || this.hasPermission(tab.permission)) &&
      (tab.contentTypes === '*' || tab.contentTypes.includes(contentType)) &&
      (!tab.locales || tab.locales === 'all' || (isSource ? tab.locales === 'source' : tab.locales === 'target'))
    ).sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || a.key.localeCompare(b.key))
  }
  widgets(): DashboardWidgetDefinition[] {
    return this.active.flatMap(module => module.widgets ?? []).filter(widget => !widget.permission || this.hasPermission(widget.permission)).sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || a.key.localeCompare(b.key))
  }
  refSources(): RefSourceDefinition[] { return this.active.flatMap(module => module.refSources ?? []) }
}
