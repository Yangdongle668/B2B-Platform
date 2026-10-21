export type Texts = Readonly<Record<string,string>>
export { defineTheme, baseTheme, ThemeRegistry, themeComponent } from './theme'
export type { ThemeDefinition, VariantSet, ResolvedVariant, ThemeDiagnostic } from './theme'
export function text(texts: Texts, key: string, values: Record<string,string> = {}): string { const value = texts[key]; if (value === undefined) throw new Error(`Missing text: ${key}`); return value.replace(/\{([a-zA-Z][a-zA-Z0-9]*)\}/g, (_, name: string) => values[name] ?? `{${name}}`) }
export interface FreshResponse { deadline?: string; status: number; private?: boolean; preview?: boolean; startedAt: number }
export function freshnessHeaders(response: FreshResponse, now: number, dependencyDeadlines: readonly string[] = []): Record<string,string> {
  const deadlines = [response.deadline, ...dependencyDeadlines].map(value => value === undefined ? NaN : Date.parse(value))
  const until = Math.min(response.startedAt + 55_000, ...deadlines)
  const ttl = Math.floor(Math.min(55, (until - now) / 1000))
  if (!Number.isFinite(until) || ttl <= 0 || now - response.startedAt > 5000 || response.private || response.preview || response.status >= 500) return { 'Cache-Control': 'no-store', 'CDN-Cache-Control': 'no-store', ...(response.status === 503 ? { 'Retry-After': '120' } : {}) }
  return { 'Cache-Control': 'public, max-age=0, must-revalidate', 'CDN-Cache-Control': `public, max-age=${ttl}`, 'X-Content-Fresh-Until': new Date(until).toISOString() }
}
export { createLayoutRenderer, sectionClasses } from './render'
export type { RenderContext, RenderDiagnostic } from './render'
export { RichTextRenderer } from './richtext'
