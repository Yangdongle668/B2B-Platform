export type PropsMigration = (props: Record<string, unknown>) => Record<string, unknown>
export function defineMigrations(type: string, steps: Record<number, PropsMigration>): Record<number, PropsMigration> {
 if (!/^[a-z][a-z0-9-]*$/.test(type) || Object.entries(steps).some(([version, step]) => !/^[1-9][0-9]*$/.test(version) || typeof step !== 'function')) throw new Error('Invalid component migration registry')
 return Object.freeze({ ...steps })
}

/** Plain text becomes a single paragraph; text is never parsed as HTML. */
export function inlineFromText(value: unknown): Record<string, unknown> {
 if (typeof value !== 'string') throw new Error('Migration text must be a string')
 return { type: 'doc', content: value ? [{ type: 'paragraph', content: [{ type: 'text', text: value }] }] : [] }
}
