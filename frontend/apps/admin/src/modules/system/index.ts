import { defineAdminModule } from '@b2bweb/admin-kit'
import { texts } from './texts'
export default defineAdminModule({ id: 'core', apiPrefix: 'system', routes: [
  { path: 'modules', name: 'system.modules', component: () => import('./views/ModuleList.vue'), meta: { title: texts.title, permission: 'system:module:manage', menuKey: 'system-modules' } },
] })
