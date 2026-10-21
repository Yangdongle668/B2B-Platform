import { describe, expect, it } from 'vitest'
import { baseTheme, defineTheme, ThemeRegistry, themeComponent } from './theme'
import type { ThemeDefinition, ThemeDiagnostic } from './theme'

const loader = async () => ({})
const catalog = [{ type: 'hero', variants: ['default', 'split'] }]
function theme(): ThemeDefinition { return defineTheme({ ...baseTheme, key: 'industrial', label: 'Industrial', extends: 'base', components: { 'hero': { default: 'default', variants: { default: loader, split: { component: loader, hydrate: 'visible' } } } } }) }
describe('theme registry', () => {
  it('rejects undeclared schema variants and invalid defaults at registration', () => {
    expect(() => new ThemeRegistry([], [theme()])).toThrow('component schemas')
    const invalid = { ...theme(), components: { 'hero': { default: 'missing', variants: { default: loader } } } }
    expect(() => defineTheme(invalid)).toThrow('default')
    expect(() => new ThemeRegistry(catalog, [theme(), theme()])).toThrow('Duplicate')
  })
  it('resolves declared variants and preserves their hydration strategy', () => {
    const registry = new ThemeRegistry(catalog, [theme()])
    const resolved = registry.resolveComponent('industrial', 'hero', 'split')!
    expect(resolved).toMatchObject({ theme: 'industrial', variant: 'split', hydrate: 'visible' })
    expect(themeComponent(resolved)).toBeDefined()
  })
  it('falls back to the current default and reports obsolete snapshot variants', () => {
    const events: ThemeDiagnostic[] = []
    const registry = new ThemeRegistry(catalog, [theme()], event => events.push(event))
    expect(registry.resolveComponent('industrial', 'hero', 'deleted')?.variant).toBe('default')
    expect(events).toContainEqual(expect.objectContaining({ code: 'variant.fallback' }))
    events.length = 0
    expect(registry.resolveComponent('industrial', 'hero')?.hydrate).toBe('none')
    expect(events).toEqual([])
  })
  it('falls back through the base theme and hides wholly missing implementations', () => {
    const events: ThemeDiagnostic[] = []
    const base = { ...baseTheme, components: theme().components }
    const child = { ...theme(), components: {} }
    const registry = new ThemeRegistry(catalog, [child], event => events.push(event), base)
    expect(registry.resolveComponent('industrial', 'hero', 'split')?.theme).toBe('base')
    expect(events).toContainEqual(expect.objectContaining({ code: 'variant.fallback' }))
    expect(registry.resolveComponent('industrial', 'missing')).toBeNull()
    expect(events).toContainEqual(expect.objectContaining({ code: 'variant.missing' }))
  })
  it('uses the base generic template for unknown themes and missing templates', () => {
    const events: ThemeDiagnostic[] = []
    const registry = new ThemeRegistry(catalog, [theme()], event => events.push(event))
    expect(registry.template('unknown', 'removed')).toBe(baseTheme.templates.generic)
    expect(events.map(event => event.code)).toEqual(['theme.missing', 'template.missing'])
    expect(registry.layout('industrial', 'blank')).toBe(baseTheme.layouts.blank)
  })
})
