import { describe,it,expect } from 'vitest'
import { freshnessHeaders,text } from './index'
import { defineAdminModule,enabledModules } from '../../admin-kit/src/index'
import { validateComponent } from '../../component-schemas/src/index'
const start = Date.parse('2030-01-01T00:00:00Z')
describe('absolute freshness', () => {
 it('takes earliest trusted dependency and deducts rendering', () => expect(freshnessHeaders({ startedAt:start, status:200, deadline:new Date(start+55000).toISOString() },start+2300,[new Date(start+41000).toISOString()])['CDN-Cache-Control']).toBe('public, max-age=38'))
 for (const status of [200,301,404,410]) it(`uses no-store for expired ${status}`, () => expect(freshnessHeaders({startedAt:start,status,deadline:new Date(start+55000).toISOString()},start+55000)['CDN-Cache-Control']).toBe('no-store'))
 it('rejects invalid deadlines, slow loaders, preview and private responses', () => { for (const override of [{deadline:'invalid'},{preview:true},{private:true}]) expect(freshnessHeaders({startedAt:start,status:200,deadline:new Date(start+55000).toISOString(),...override},start)['CDN-Cache-Control']).toBe('no-store'); expect(freshnessHeaders({startedAt:start,status:200,deadline:new Date(start+55000).toISOString()},start+6000)['CDN-Cache-Control']).toBe('no-store') })
 it('returns retry metadata for unavailable source', () => expect(freshnessHeaders({startedAt:start,status:503},start)).toMatchObject({'Retry-After':'120','Cache-Control':'no-store'}))
})
describe('framework boundaries', () => {
 it('rejects cross-module route names', () => expect(() => defineAdminModule({id:'product',routes:[{path:'list',name:'inquiry.list',component:async()=>({}),meta:{title:'x'}}]})).toThrow())
 it('filters disabled modules', () => expect(enabledModules([{id:'product',routes:[]},{id:'inquiry',routes:[]}],new Set(['product']))).toHaveLength(1))
 it('validates schema and rejects extra executable props', () => { const props = {title:'sample',description:'sample'}; expect(validateComponent('hero',props)).toBe(true); expect(validateComponent('hero',{...props,script:'alert(1)'})).toBe(false); expect(validateComponent('hero',{...props,actionPath:'/zh/'})).toBe(false) })
 it('never silently falls back for a missing text', () => { expect(text({'greeting':'Hello {name}'},'greeting',{name:'Lin'})).toBe('Hello Lin'); expect(()=>text({},'missing')).toThrow() })
})
