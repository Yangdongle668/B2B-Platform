import type { AdminModuleDefinition } from '@b2bweb/admin-kit'
const entries = import.meta.glob<{ default: AdminModuleDefinition }>('./*/index.ts', { eager: true })
export const modules = Object.values(entries).map(entry => entry.default)
