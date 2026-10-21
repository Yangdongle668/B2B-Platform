import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { SchemaCatalog, definitions } from './index'
import type { PropsMigration } from './index'
const directory = resolve(import.meta.dirname, '..')
const modules = import.meta.glob('../components/**/migrations/*.ts', { eager: true }) as Record<string, { default: Record<number, PropsMigration> }>
describe('canonical migration fixtures', () => {
 it('executes every historical step against its schema and exact expected output', () => {
  for (const definition of definitions) {
   if (definition.version === 1) continue
   const owner = resolve(directory, 'components', definition.module)
   const migration = Object.entries(modules).find(([path]) => path.endsWith(`/${definition.type}.ts`))?.[1].default
   expect(migration, definition.type).toBeDefined()
   const catalog = new SchemaCatalog([definition], { [definition.type]: migration! })
   for (let version = 1; version < definition.version; version++) {
    const fixture = JSON.parse(readFileSync(resolve(owner, '__fixtures__', definition.type, `v${version}.json`), 'utf8'))
    const before = JSON.stringify(fixture.input)
    expect(catalog.migrate(definition.type, fixture.input, version).props).toEqual(fixture.expected)
    expect(JSON.stringify(fixture.input)).toBe(before)
   }
  }
 })
 it('rejects unowned or orphan migration fixture directories', () => {
  for (const module of readdirSync(resolve(directory, 'components'))) {
   const fixtures = resolve(directory, 'components', module, '__fixtures__')
   if (existsSync(fixtures)) for (const type of readdirSync(fixtures)) expect(definitions.some(definition => definition.type === type && definition.module === module && definition.version > 1)).toBe(true)
  }
 })
})
