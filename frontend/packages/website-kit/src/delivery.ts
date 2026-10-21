import { Fragment, defineComponent, h } from 'vue'
import type { PropType } from 'vue'

export type ContentReference = { $content: string }
export type MediaReference = { $media: string }
export type TermReference = { $term: string }
export interface PublishedContentReference { title: string; url: string | null; path?: string | null; [key: string]: unknown }
export interface PublishedTermReference { name: string; url: string | null; path?: string | null; [key: string]: unknown }
export interface PublicMediaReference { url: string | null; width?: number; height?: number; alt?: string | null; fileName?: string; mime?: string; size?: number; visibility?: 'PUBLIC' | 'PRIVATE'; [key: string]: unknown }
/** Tables already resolved by Delivery for the current site and language; no additional requests. */
export interface DeliveryRefs { content?: Readonly<Record<string, PublishedContentReference>>; media?: Readonly<Record<string, PublicMediaReference>>; terms?: Readonly<Record<string, PublishedTermReference>> }
export function safePublicUrl(value: unknown, allowPath = true): string | undefined {
  if (typeof value !== 'string' || !value || /[\s\\]/u.test(value) || [...value].some(char => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127)) return undefined
  if (allowPath && value.startsWith('/') && !value.startsWith('//')) return value
  try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : undefined } catch { return undefined }
}
const id = (value: unknown): value is string => typeof value === 'string' && /^[1-9][0-9]*$/.test(value)
export function createRefs(table: DeliveryRefs) {
  const content = (reference: ContentReference): PublishedContentReference | null => {
    const value = id(reference?.$content) ? table.content?.[reference.$content] : undefined
    return value && typeof value.title === 'string' && (value.path == null || safePublicUrl(value.path)) && (value.url == null || safePublicUrl(value.url)) ? value : null
  }
  const media = (reference: MediaReference): PublicMediaReference | null => {
    const value = id(reference?.$media) ? table.media?.[reference.$media] : undefined
    return value && value.visibility !== 'PRIVATE' && safePublicUrl(value.url) ? value : null
  }
  const term = (reference: TermReference): PublishedTermReference | null => {
    const value = id(reference?.$term) ? table.terms?.[reference.$term] : undefined
    return value && typeof value.name === 'string' && (value.path == null || safePublicUrl(value.path)) && (value.url == null || safePublicUrl(value.url)) ? value : null
  }
  return Object.freeze({ content, media, term, file: (reference: MediaReference) => { const value = media(reference); return value && typeof value.fileName === 'string' && typeof value.mime === 'string' && typeof value.size === 'number' ? value : null } })
}
export type ResolvedRefs = ReturnType<typeof createRefs>
export type LinkValue = ContentReference | { href: string; rel?: Array<'nofollow' | 'sponsored'> }
export function resolveLink(link: LinkValue, refs: ResolvedRefs): { href: string; rel?: string } | null {
  if (!link || typeof link !== 'object') return null
  if ('$content' in link) { const value = refs.content(link); const href = safePublicUrl(value?.path ?? value?.url); return href ? { href } : null }
  const href = safePublicUrl(link.href, false)
  if (!href) return null
  return { href, rel: ['noopener', ...(Array.isArray(link.rel) ? link.rel.filter(value => ['nofollow', 'sponsored'].includes(value)) : [])].filter((value, index, all) => all.indexOf(value) === index).join(' ') }
}
export const B2bLink = defineComponent({ name: 'B2bLink', inheritAttrs: false, props: { link: { type: Object as PropType<LinkValue>, required: true }, refs: { type: Object as PropType<ResolvedRefs>, required: true } }, setup: (props, { slots }) => () => {
  const link = resolveLink(props.link, props.refs)
  return link ? h('a', link, slots.default?.()) : h(Fragment, slots.default?.())
} })

/** Mirrors ImageSources from platform-api. URLs and dimensions come from Media, never client signing. */
export interface ImageSources { mediaId: string; preset: 'THUMB' | 'CARD' | 'CONTENT' | 'HERO' | 'OG'; width: number; height: number; alt: string | null; fallbackUrl: string; srcsets: Partial<Record<'AVIF' | 'WEBP' | 'JPEG' | 'PNG', Array<{ width: number; url: string }>>> }
function candidates(values: Array<{ width: number; url: string }> | undefined): string | undefined {
  if (!Array.isArray(values)) return undefined
  const result = new Map<number, string>()
  for (const item of values) { const url = safePublicUrl(item?.url); if (url && Number.isInteger(item.width) && item.width >= 1 && item.width <= 4096) result.set(item.width, url) }
  return result.size ? [...result].sort((a, b) => a[0] - b[0]).map(([width, url]) => `${url} ${width}w`).join(', ') : undefined
}
export function imagePreload(source: ImageSources, sizes = '100vw') {
  const fallback = safePublicUrl(source?.fallbackUrl)
  if (!fallback || !Number.isInteger(source.width) || !Number.isInteger(source.height) || source.width < 1 || source.height < 1 || source.width > 4096 || source.height > 4096) return null
  const avif = candidates(source.srcsets?.AVIF), webp = candidates(source.srcsets?.WEBP), original = candidates(source.srcsets?.JPEG ?? source.srcsets?.PNG)
  return { rel: 'preload' as const, as: 'image' as const, href: fallback, imagesrcset: avif ?? webp ?? original, imagesizes: sizes, fetchpriority: 'high' as const, ...(avif ? { type: 'image/avif' } : webp ? { type: 'image/webp' } : {}) }
}
export const B2bImage = defineComponent({ name: 'B2bImage', inheritAttrs: false, props: { sources: { type: Object as PropType<ImageSources>, required: true }, sizes: { type: String, default: '100vw' }, priority: Boolean }, setup: props => () => {
  const source = props.sources, url = safePublicUrl(source?.fallbackUrl)
  if (!url || !Number.isInteger(source.width) || !Number.isInteger(source.height) || source.width < 1 || source.height < 1 || source.width > 4096 || source.height > 4096) return null
  const sets = source.srcsets ?? {}
  return h('picture', [
    ...(['AVIF', 'WEBP'] as const).flatMap(format => { const srcset = candidates(sets[format]); return srcset ? [h('source', { type: `image/${format.toLowerCase()}`, srcset, sizes: props.sizes })] : [] }),
    h('img', { src: url, srcset: candidates(sets.JPEG ?? sets.PNG), sizes: props.sizes, width: source.width, height: source.height, alt: source.alt ?? '', loading: props.priority ? 'eager' : 'lazy', fetchpriority: props.priority ? 'high' : 'auto', decoding: 'async' }),
  ])
} })

export interface DeliveryHeadInput { route?: { canonical?: string }; seo?: { title?: string; meta?: Array<{ name?: string; property?: string; content: string }>; alternates?: Array<{ hreflang: string; href: string }>; jsonLd?: unknown } }
export function serializeJsonLd(value: unknown): string {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) throw new Error('Invalid JSON-LD root')
  return JSON.stringify(value).replace(/[<>&\u2028\u2029]/gu, char => ({ '<': '\\u003c', '>': '\\u003e', '&': '\\u0026', '\u2028': '\\u2028', '\u2029': '\\u2029' }[char]!))
}
/** Projects Delivery's already computed SEO. It never derives titles, canonicals or language fallbacks. */
export function deliveryHead(input: DeliveryHeadInput, preview = false) {
  const seo = input.seo
  const meta = (seo?.meta ?? []).filter(item => typeof item.content === 'string' && (typeof item.name === 'string' || typeof item.property === 'string') && !(preview && item.name?.toLowerCase() === 'robots')).map(item => ({ ...(item.name ? { name: item.name } : { property: item.property! }), content: item.content }))
  if (preview) meta.push({ name: 'robots', content: 'noindex, nofollow' })
  const link: Array<{ rel: 'canonical'; href: string } | { rel: 'alternate'; href: string; hreflang: string }> = []
  const canonical = safePublicUrl(input.route?.canonical, false)
  if (canonical) link.push({ rel: 'canonical', href: canonical })
  for (const alternate of seo?.alternates ?? []) { const href = safePublicUrl(alternate.href, false); if (href && /^(?:[a-z]{2,8}(?:-[a-z0-9]{1,8})*|x-default)$/iu.test(alternate.hreflang)) link.push({ rel: 'alternate', hreflang: alternate.hreflang, href }) }
  return { ...(typeof seo?.title === 'string' ? { title: seo.title } : {}), meta, link, script: seo?.jsonLd == null ? [] : [{ type: 'application/ld+json' as const, innerHTML: serializeJsonLd(seo.jsonLd) }] }
}
