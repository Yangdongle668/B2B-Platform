import { readdir, readFile } from 'node:fs/promises'
import ts from 'typescript'

const errors = []
const root = 'frontend/apps/admin/src/modules'
const descriptors = new Map()
descriptors.set('system', JSON.parse(await readFile('backend/platform/platform-core/src/main/resources/META-INF/b2bweb/core-module.json', 'utf8')))
for (const entry of await readdir('backend/modules', { withFileTypes: true })) {
  if (entry.isDirectory()) descriptors.set(entry.name, JSON.parse(await readFile(`backend/modules/${entry.name}/src/main/resources/META-INF/b2bweb/modules/${entry.name}/module.json`, 'utf8')))
}
const permissions = new Set([...descriptors.values()].flatMap(module => module.permissions.map(permission => permission.code)))
const literal = node => node && ts.isStringLiteralLike(node) ? node.text : undefined
const property = (node, key) => node && ts.isObjectLiteralExpression(node) ? node.properties.find(item => ts.isPropertyAssignment(item) && item.name.getText() === key)?.initializer : undefined
let routeCount = 0
let pendingMenus = 0
for (const directory of await readdir(root, { withFileTypes: true })) {
  if (!directory.isDirectory()) continue
  const path = `${root}/${directory.name}/index.ts`
  const descriptor = descriptors.get(directory.name)
  if (!descriptor) { errors.push(`${path}: unknown module`); continue }
  const source = ts.createSourceFile(path, await readFile(path, 'utf8'), ts.ScriptTarget.Latest, true)
  const calls = []
  function visit(node) { if (ts.isCallExpression(node) && node.expression.getText() === 'defineAdminModule') calls.push(node); ts.forEachChild(node, visit) }
  visit(source)
  if (calls.length !== 1) { errors.push(`${path}: exactly one static defineAdminModule is required`); continue }
  const definition = calls[0].arguments[0]
  if (literal(property(definition, 'id')) !== descriptor.id) errors.push(`${path}: module id mismatch`)
  if ((literal(property(definition, 'apiPrefix')) ?? descriptor.id) !== descriptor.apiPrefix) errors.push(`${path}: API prefix mismatch`)
  const routes = property(definition, 'routes')
  if (!routes || !ts.isArrayLiteralExpression(routes)) { errors.push(`${path}: routes must be a static array`); continue }
  const paths = new Set()
  const names = new Set()
  for (const route of routes.elements) {
    const pathValue = literal(property(route, 'path'))
    const name = literal(property(route, 'name'))
    const meta = property(route, 'meta')
    const permissionNode = property(meta, 'permission')
    const permission = literal(permissionNode)
    if (pathValue === undefined || pathValue.startsWith('/') || pathValue.includes('..') || paths.has(pathValue)) errors.push(`${path}: invalid or duplicate static route path`)
    if (!name?.startsWith(`${descriptor.apiPrefix}.`) || names.has(name)) errors.push(`${path}: invalid or duplicate route name`)
    if (permissionNode && (!permission || !permissions.has(permission))) errors.push(`${path}: undeclared route permission`)
    const menuKeyNode = property(meta, 'menuKey')
    if (menuKeyNode && !descriptor.menus.some(menu => menu.key === literal(menuKeyNode) && menu.route === `/${descriptor.apiPrefix}/${pathValue}`)) errors.push(`${path}: route/menu ownership mismatch`)
    paths.add(pathValue); names.add(name); routeCount++
  }
  pendingMenus += descriptor.menus.filter(menu => menu.route && !paths.has(menu.route.replace(`/${descriptor.apiPrefix}/`, ''))).length
}
console.log(JSON.stringify({ admin_modules: descriptors.size, contributed_routes: routeCount, menu_routes_pending_implementation: pendingMenus, errors }, null, 2))
if (errors.length) process.exit(1)
