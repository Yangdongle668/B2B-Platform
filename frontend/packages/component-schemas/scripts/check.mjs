/** Validate the shared source definitions. This checks implemented entries; module backlog is reported. */
import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve, relative, basename } from 'node:path'
import { execFileSync } from 'node:child_process'
import Ajv2020 from 'ajv/dist/2020.js'
const root = resolve(import.meta.dirname, '../../../..')
const directory = resolve(root, 'frontend/packages/component-schemas')
const read = path => JSON.parse(readFileSync(path, 'utf8'))
const files = (directory, includeHistory = false) => {
  try { return readdirSync(directory, { withFileTypes: true }).flatMap(entry => entry.isDirectory() ? ['examples', '__fixtures__', 'migrations'].includes(entry.name) || entry.name === 'history' && !includeHistory ? [] : files(resolve(directory, entry.name), includeHistory) : entry.name.endsWith('.json') ? [resolve(directory, entry.name)] : []) } catch (error) { if (error.code === 'ENOENT') return []; throw error }
}
const ajv = new Ajv2020({ strict: true, allErrors: true })
for (const keyword of ['x-editor', 'x-translatable', 'x-seo', 'x-media', 'x-content', 'x-term', 'x-ref', 'x-source', 'x-group', 'x-order', 'x-visible-if', 'x-enum-labels', 'x-placeholder']) ajv.addKeyword({ keyword, valid: true })
const meta = ajv.compile(read(resolve(directory, 'meta/component.schema.json')))
const templateMeta = ajv.compile(read(resolve(directory, 'meta/template.schema.json')))
const common = read(resolve(directory, 'defs/common.json'))
const styles = read(resolve(directory, 'defs/style-options.json'))
const tokenSource = readFileSync(resolve(root, 'frontend/packages/design-tokens/src/index.ts'), 'utf8')
const tokenEnums = Object.fromEntries([...tokenSource.matchAll(/(background|paddingY|container): \[([^\]]+)\]/g)].map(([, key, values]) => [key, [...values.matchAll(/'([^']+)'/g)].map(value => value[1])]))
if (JSON.stringify(styles) !== JSON.stringify(tokenEnums)) throw new Error('Style options differ from design tokens')
const compile = schema => {
  const copy = JSON.parse(JSON.stringify(schema))
  const walk = node => {
    if (!node || typeof node !== 'object') return
    if (Array.isArray(node)) return node.forEach(walk)
    if ('$id' in node) throw new Error('Nested schema identifiers are forbidden')
    if ('$ref' in node) {
      if (node.$ref.startsWith('../../defs/common.json#/$defs/')) node.$ref = node.$ref.replace('../../defs/common.json', '')
      else if (!node.$ref.startsWith('#/$defs/')) throw new Error('Remote schema references are forbidden')
    }
    Object.values(node).forEach(walk)
  }
  walk(copy); for (const key of Object.keys(copy.$defs ?? {})) if (Object.hasOwn(common.$defs, key)) throw new Error('Shared schema definition override forbidden'); copy.$defs = { ...common.$defs, ...copy.$defs }
  return ajv.compile(copy)
}
const definitions = new Map(); const versions = new Map(); const validators = new Map()
const checkProperties = (schema, pointer = '') => {
  let sources = 0
  const walk = (node, path) => {
    if (!node || typeof node !== 'object') return
    if (node['x-editor'] === 'data-source') sources++
    if (node.properties) for (const [key, prop] of Object.entries(node.properties)) {
      if (!prop.title?.match(/[\u3400-\u9fff]/u)) throw new Error(`${path}/${key}: missing Chinese title`)
      if (['style', 'className'].includes(key)) throw new Error(`${path}/${key}: inline style forbidden`)
      if (typeof prop.default === 'string' && !prop.enum?.includes(prop.default) && prop.const !== prop.default) throw new Error(`${path}/${key}: text default forbidden`)
      if ((prop.enum || prop.const !== undefined || prop.pattern || ['uri', 'email'].includes(prop.format)) && prop['x-translatable'] !== false) throw new Error(`${path}/${key}: code/enum/URL must be untranslatable`)
      if (['media', 'content', 'term', 'data-source', 'link', 'richtext', 'richtext-inline'].includes(prop['x-editor']) && !prop.$ref?.startsWith('../../defs/common.json#/$defs/')) throw new Error(`${path}/${key}: shared reference required`)
      walk(prop, `${path}/${key}`)
    }
    if (node.items) walk(node.items, `${path}/*`)
  }
  walk(schema, pointer)
  if (sources > 1) throw new Error('More than one data source per component')
}
for (const file of files(resolve(directory, 'components'), true)) {
  const definition = read(file); const path = relative(directory, file)
  if (!meta(definition)) throw new Error(`${path}: ${ajv.errorsText(meta.errors)}`)
  const descriptor = read(resolve(root, `backend/modules/${definition.module}/src/main/resources/META-INF/b2bweb/modules/${definition.module}/module.json`))
  const historic = file.includes('/history/')
  if (!file.includes(`/components/${definition.module}/`) || basename(file) !== `${definition.type}${historic ? `.v${definition.version}` : ''}.json` || !descriptor.components.includes(definition.type)) throw new Error(`${path}: descriptor/type/path mismatch`)
  if (definition.styleOptions.some(key => !styles[key])) throw new Error(`${path}: undeclared style option`)
  checkProperties(definition.propsSchema)
  const key = `${definition.type}:${definition.version}`
  if (versions.has(key)) throw new Error(`${path}: duplicate version`)
  versions.set(key, definition)
  const validate = compile(definition.propsSchema)
  if (historic) continue
  if (definitions.has(definition.type)) throw new Error(`${path}: duplicate type`)
  definitions.set(definition.type, definition); validators.set(definition.type, validate)
  const examples = read(resolve(directory, `components/${definition.module}/examples/${definition.type}.json`))
  for (const variant of definition.variants) if (!examples.some(example => example.variant === variant && validate(example.props))) throw new Error(`${path}: missing valid example ${variant}`)
  for (const example of examples) if (!definition.variants.includes(example.variant) || !validate(example.props)) throw new Error(`${path}: invalid example`)
  for (let version = 1; version < definition.version; version++) if (!versions.has(`${definition.type}:${version}`) && !files(resolve(directory, `components/${definition.module}/history`), true).some(path => basename(path) === `${definition.type}.v${version}.json`)) throw new Error(`${path}: missing historical schema`)
  if (definition.version > 1) readFileSync(resolve(file, '..', definition.migrations), 'utf8')
}
// Keep the statically imported client catalog in sync with every canonical source file.
// Nuxt/Vite can tree-shake these imports; no browser filesystem access or remote schema loading.
const componentFiles = files(resolve(directory, 'components')).sort()
const generatedIndex = '// Generated by check:schemas --write-index; canonical definitions remain JSON.\n'
  + componentFiles.map((file, index) => `import component${index} from '${relative(resolve(directory, 'src'), file).replaceAll('\\', '/')}'`).join('\n')
  + `\nexport const componentDefinitions = [${componentFiles.map((_, index) => `component${index}`).join(', ')}] as const\n`
const generatedFile = resolve(directory, 'src/generated.ts')
if (process.argv.includes('--write-index')) writeFileSync(generatedFile, generatedIndex)
if (readFileSync(generatedFile, 'utf8') !== generatedIndex) throw new Error('Client schema catalog drift; run pnpm check:schemas --write-index')
for (const file of files(resolve(directory, 'templates'))) {
  const template = read(file)
  if (!templateMeta(template) || basename(file) !== `${template.key}.json`) throw new Error(`${file}: invalid template`)
  const keys = new Set()
  for (const region of template.regions) {
    if (keys.has(region.key)) throw new Error(`${file}: duplicate region`)
    keys.add(region.key)
    if (region.fixed && !validators.get(region.component)?.(region.props)) throw new Error(`${file}: invalid fixed region`)
    if (Array.isArray(region.allowed) && region.allowed.some(type => !definitions.get(type)?.allowedIn.includes('slot'))) throw new Error(`${file}: invalid slot component`)
    for (const section of region.default ?? []) if (!validators.get(section.component)?.(section.props) || !definitions.get(section.component)?.allowedIn.includes('slot')) throw new Error(`${file}: invalid default section`)
  }
}
// CI supplies the real previous/base commit; local checks compare the working tree to HEAD.
const baseIndex = process.argv.indexOf('--base')
const base = baseIndex >= 0 ? process.argv[baseIndex + 1] : 'HEAD'
if (!base || !/^[A-Za-z0-9_./-]+$/.test(base)) throw new Error('Invalid schema comparison base')
execFileSync('git', ['rev-parse', '--verify', `${base}^{commit}`], { cwd: root, stdio: 'pipe' })
const oldFiles = execFileSync('git', ['ls-tree', '-r', '--name-only', base, '--', 'frontend/packages/component-schemas'], { cwd: root, encoding: 'utf8' }).split('\n')
const oldJson = path => JSON.parse(execFileSync('git', ['show', `${base}:${path}`], { cwd: root, encoding: 'utf8' }))
const shrinks = (previous = [], current = []) => previous.some(value => !current.includes(value))
const breaksProps = (old, current) => {
  if (!current) return true
  if (JSON.stringify(old.type) !== JSON.stringify(current.type) || old.$ref !== current.$ref || old.pattern !== current.pattern || old.format !== current.format || current.const !== undefined && old.const !== current.const) return true
  if (current.enum && (!old.enum || shrinks(old.enum, current.enum)) || shrinks(current.required, old.required) || old.additionalProperties !== false && current.additionalProperties === false) return true
  for (const key of ['maxLength', 'maximum', 'maxItems']) if (current[key] !== undefined && (old[key] === undefined || current[key] < old[key])) return true
  for (const key of ['minLength', 'minimum', 'minItems']) if (current[key] !== undefined && (old[key] === undefined || current[key] > old[key])) return true
  if (['oneOf', 'anyOf', 'allOf', 'not'].some(key => JSON.stringify(old[key]) !== JSON.stringify(current[key]))) return true
  if (old.items && breaksProps(old.items, current.items)) return true
  return Object.entries(old.properties ?? {}).some(([key, value]) => breaksProps(value, current.properties?.[key]))
}
let breakingComparisons = 0
for (const path of oldFiles.filter(path => /components\/[^/]+\/[^/]+\.json$/.test(path))) {
  const old = oldJson(path); const current = definitions.get(old.type)
  if (!current) throw new Error(`${path}: retain deprecated definitions until usage cleanup is reviewed`)
  const breaking = breaksProps(old.propsSchema, current.propsSchema) || shrinks(old.variants, current.variants) || shrinks(old.allowedIn, current.allowedIn)
  if (current.version < old.version || current.version > old.version + 1) throw new Error(`${path}: version must advance by exactly one`)
  if (breaking && current.version !== old.version + 1) throw new Error(`${path}: breaking schema change requires version +1`)
  if (current.version > old.version) {
    const archived = read(resolve(directory, `components/${old.module}/history/${old.type}.v${old.version}.json`))
    if (JSON.stringify(archived) !== JSON.stringify(old)) throw new Error(`${path}: archived definition differs from previous source`)
    const fixture = read(resolve(directory, `components/${current.module}/__fixtures__/${current.type}/v${old.version}.json`))
    if (!Object.hasOwn(fixture, 'input') || !Object.hasOwn(fixture, 'expected')) throw new Error(`${path}: migration input/expected fixture required`)
  }
  breakingComparisons++
}
for (const path of oldFiles.filter(path => /templates\/[^/]+\/[^/]+\.json$/.test(path))) {
  const old = oldJson(path); const current = read(resolve(root, path))
  const breaking = old.regions.some(region => { const next = current.regions.find(item => item.key === region.key); return !next || region.component !== next.component || region.slot !== next.slot || region.allowed === '*' && next.allowed !== '*' || Array.isArray(region.allowed) && Array.isArray(next.allowed) && shrinks(region.allowed, next.allowed) })
  if (breaking && current.version !== old.version + 1) throw new Error(`${path}: breaking template change requires version +1`)
}
console.log(JSON.stringify({ comparison_base: base, breaking_comparisons: breakingComparisons, shared_component_definitions: definitions.size, historical_versions: versions.size - definitions.size, template_definitions: files(resolve(directory, 'templates')).length, scope: 'implemented M0 sample; remaining module components are business slices', valid: true }))
