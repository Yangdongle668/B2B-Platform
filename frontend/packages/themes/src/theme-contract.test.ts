import { expect, it } from 'vitest'
import { definitions } from '@b2bweb/component-schemas'
import { ThemeRegistry } from '@b2bweb/website-kit'
import { industrialTheme } from './index'

it('implements every declared current schema variant in the delivered theme', async () => {
  const registry = new ThemeRegistry(definitions, [industrialTheme])
  for (const definition of definitions) for (const variant of definition.variants) {
    const resolved = registry.resolveComponent('industrial', definition.type, variant)
    expect(resolved, `${definition.type}:${variant}`).not.toBeNull()
    // A missing requested variant must not be hidden by falling back to the default.
    expect(resolved?.variant).toBe(variant)
    expect(await resolved!.component()).toBeTruthy()
  }
})
