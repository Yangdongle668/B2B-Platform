import Ajv2020 from 'ajv/dist/2020.js'
export const heroSchema = { $schema: 'https://json-schema.org/draft/2020-12/schema', type: 'object', additionalProperties: false, required: ['title','description','actionLabel','actionPath'], properties: { title: { type: 'string', minLength: 1, 'x-translatable': true }, description: { type: 'string', 'x-translatable': true }, actionLabel: { type: 'string', 'x-translatable': true }, actionPath: { type: 'string', pattern: '^/zh/', 'x-translatable': false } } } as const
const ajv = new Ajv2020({ allErrors: true, strict: true }); ajv.addKeyword({ keyword: 'x-translatable', schemaType: 'boolean', valid: true })
const validateHero = ajv.compile(heroSchema)
export const definitions = Object.freeze([{ type: 'page.hero', version: 1, name: '首屏展示', variants: ['default'], propsSchema: heroSchema }])
export function validateComponent(type: string, props: unknown): boolean { if (type !== 'page.hero') throw new Error('Unknown component type'); return Boolean(validateHero(props)) }
