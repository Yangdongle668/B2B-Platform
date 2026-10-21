import { describe, expect, it, vi } from 'vitest'
import { AdminRuntime, defineAdminModule } from './index'
import type { SiteBootstrap } from './runtime'
import type { AdminModuleContext } from './index'

const component = async () => ({})
const product = defineAdminModule({ id: 'product', routes: [{ path: 'products', name: 'product.products', component, meta: { title: 'Products', permission: 'product:product:view' } }], contentTabs: [{ module: 'product', key: 'product-details', label: 'Details', contentTypes: ['PRODUCT'], permission: 'product:product:view', locales: 'source', component }], widgets: [{ key: 'product-count', title: 'Products', permission: 'product:product:view', size: 'sm', component }], refSources: [{ kind: 'product', label: 'Products', search: async () => [] }] })
function bootstrap(id = '1', enabled = true, permission = true): SiteBootstrap {
  return { site: { siteId: id, status: 'ACTIVE' }, modules: enabled ? [{ moduleId: 'product', name: 'Products' }] : [], permissions: permission ? [{ code: 'product:product:view', dataScope: 'OWN', localeScope: { unrestricted: false, locales: ['zh-CN'] } }] : [], menus: [{ key: 'products', name: 'Products', route: '/product/products' }, { key: 'unimplemented', name: 'Other', route: '/product/future' }] }
}
describe('admin module runtime', () => {
  it('registers enabled routes and checks authentication before permission', async () => {
    const add = vi.fn(() => vi.fn()); const runtime = new AdminRuntime([product], { add })
    await runtime.switchSite('1', async () => bootstrap())
    expect(add).toHaveBeenCalledWith(expect.objectContaining({ path: '/product/products', moduleId: 'product' }))
    expect(runtime.access('product.products', false)).toBe('login')
    expect(runtime.access('product.products', true)).toBe('allowed')
    expect(runtime.menus()).toHaveLength(1)
    expect(runtime.dataScope('product:product:view')).toBe('OWN')
    expect(runtime.canAccessLocale('product:product:view', 'en')).toBe(false)
  })
  it('removes routes, contributions and site stores before a pending switch', async () => {
    const remove = vi.fn(); const reset = vi.fn(); const runtime = new AdminRuntime([product], { add: () => remove })
    runtime.onSiteReset(reset); await runtime.switchSite('1', async () => bootstrap())
    expect(runtime.contentTabs('PRODUCT', true)).toHaveLength(1)
    expect(runtime.contentTabs('PRODUCT', false)).toHaveLength(0)
    const switching = runtime.switchSite('2', async () => bootstrap('2', false))
    expect(runtime.ready).toBe(false); expect(runtime.widgets()).toHaveLength(0)
    await switching
    expect(remove).toHaveBeenCalledOnce(); expect(reset).toHaveBeenCalledTimes(2)
    expect(runtime.access('product.products', true)).toBe('not-found')
    expect(runtime.refSources()).toHaveLength(0)
  })
  it('keeps an enabled but forbidden route distinguishable from a disabled module', async () => {
    const runtime = new AdminRuntime([product], { add: () => () => {} })
    await runtime.switchSite('1', async () => bootstrap('1', true, false))
    expect(runtime.access('product.products', true)).toBe('forbidden')
    expect(runtime.menus()).toEqual([]); expect(runtime.widgets()).toEqual([])
  })
  it('ignores late responses from an earlier site and never restores stale state on failure', async () => {
    const runtime = new AdminRuntime([product], { add: () => () => {} })
    let resolve!: (value: SiteBootstrap) => void
    const first = runtime.switchSite('1', () => new Promise(done => { resolve = done }))
    await runtime.switchSite('2', async () => bootstrap('2', false))
    resolve(bootstrap()); expect(await first).toBeUndefined(); expect(runtime.siteId).toBe('2')
    await expect(runtime.switchSite('3', async () => { throw new Error('offline') })).rejects.toThrow('offline')
    expect(runtime.ready).toBe(false); expect(runtime.menus()).toEqual([])
  })
  it('rejects a wrong-site response and clears all authorization on logout', async () => {
    const runtime = new AdminRuntime([product], { add: () => () => {} })
    await expect(runtime.switchSite('1', async () => bootstrap('2'))).rejects.toThrow('site mismatch')
    expect(runtime.ready).toBe(false)
    await runtime.switchSite('1', async () => bootstrap()); runtime.clear()
    expect(runtime.hasPermission('product:product:view')).toBe(false); expect(runtime.siteId).toBeUndefined()
  })
  it('invalidates retained setup contexts and detaches permission snapshots from loader objects', async () => {
    let context!: AdminModuleContext
    const setup = vi.fn((value: AdminModuleContext) => { context = value })
    const runtime = new AdminRuntime([{ ...product, setup }], { add: () => () => {} })
    const loaded = bootstrap()
    await runtime.switchSite('1', async () => loaded)
    loaded.permissions.length = 0
    expect(context.hasPermission('product:product:view')).toBe(true)
    await runtime.switchSite('2', async () => bootstrap('2', false))
    expect(setup).toHaveBeenCalledOnce()
    expect(context.hasPermission('product:product:view')).toBe(false)
  })
  it('allows declared content permission namespaces on a business editor route', () => {
    expect(() => defineAdminModule({ id: 'product', routes: [{ path: 'products/:id', name: 'product.edit', component, meta: { title: 'Edit', permission: 'content:product:edit' } }] })).not.toThrow()
  })
})
