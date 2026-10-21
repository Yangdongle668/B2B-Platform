import { ref } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { AdminRuntime } from '@b2bweb/admin-kit'
import { modules } from '../modules'
import { texts } from '../texts'
import RouteStatus from './RouteStatus.vue'

export const authStage = ref('LOADING')
export const router = createRouter({ history: createWebHistory(import.meta.env.BASE_URL), routes: [
  { path: '/', name: 'dashboard', component: RouteStatus },
  { path: '/login', name: 'login', component: RouteStatus },
  { path: '/forbidden', name: 'forbidden', component: RouteStatus },
  { path: '/not-found', name: 'not-found', component: RouteStatus },
  { path: '/:pathMatch(.*)*', name: 'unmatched', component: RouteStatus },
] })
export const runtime = new AdminRuntime(modules, { add: route => router.addRoute({ path: route.path, name: route.name, component: route.component, meta: { ...route.meta, moduleId: route.moduleId } }) })
router.beforeEach(to => {
  if (authStage.value === 'LOADING') return true
  if (authStage.value !== 'AUTHENTICATED') return to.name === 'login' ? true : { name: 'login' }
  if (to.name === 'login') return { name: 'dashboard' }
  if (to.meta.moduleId) {
    const access = runtime.access(String(to.name), true)
    if (access !== 'allowed') return { name: access }
  }
  if (to.name === 'unmatched' && runtime.ready) return { name: 'not-found' }
  return true
})
router.afterEach(to => { document.title = `${typeof to.meta.title === 'string' ? to.meta.title : texts.welcome} · ${texts.title}` })
