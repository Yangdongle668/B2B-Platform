import { it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { B2bLink, B2bImage, createRefs, deliveryHead, imagePreload, resolveLink, serializeJsonLd } from './delivery'
import type { ImageSources } from './delivery'
const refs = createRefs({ content: { '1': { title: 'Published', url: '/en/current' }, '2': { title: 'Unavailable path', url: null } }, media: { '3': { url: 'https://img.example.com/signed/photo', width: 640, height: 480, alt: null }, '4': { url: 'https://private.example.com/object', visibility: 'PRIVATE', fileName: 'secret.pdf', mime: 'application/pdf', size: 1 } }, terms: { '5': { name: 'Term', url: '/en/term' } } })
it('uses only supplied published refs without synthesizing paths or fetching missing locales', () => {
  expect(refs.content({ $content: '1' })?.url).toBe('/en/current'); expect(refs.content({ $content: '999' })).toBeNull(); expect(refs.content({ $content: '1e0' })).toBeNull()
  expect(resolveLink({ $content: '2' }, refs)).toBeNull(); expect(refs.file({ $media: '4' })).toBeNull(); expect(refs.media({ $media: '3' })?.alt).toBeNull(); expect(refs.term({ $term: '5' })?.name).toBe('Term')
})
it('rejects executable links, raw internal hrefs and credentials while preserving external rel', () => {
  for (const href of ['javascript:alert(1)', 'data:text/html,x', '//example.com', '/zh/internal', 'https://user:pass@example.com']) expect(resolveLink({ href }, refs)).toBeNull()
  expect(resolveLink({ href: 'https://example.com', rel: ['nofollow', 'sponsored'] }, refs)).toEqual({ href: 'https://example.com/', rel: 'noopener nofollow sponsored' })
})
it('renders missing targets as text and published links with escaped slot content in SSR', async () => {
  const render = (target: string) => renderToString(createSSRApp({ render: () => h(B2bLink, { link: { $content: target }, refs }, () => '<script>untrusted</script>') }))
  expect(await render('999')).not.toContain('<a'); expect(await render('1')).toContain('href="/en/current"'); expect(await render('1')).toContain('&lt;script&gt;')
})
const sources: ImageSources = { mediaId: '3', preset: 'HERO', width: 1920, height: 1080, alt: null, fallbackUrl: 'https://img.example.com/signed.jpg', srcsets: { AVIF: [{ width: 640, url: 'https://img.example.com/signed-640.avif' }, { width: 320, url: 'javascript:alert(1)' }], WEBP: [{ width: 640, url: 'https://img.example.com/signed-640.webp' }], JPEG: [{ width: 640, url: 'https://img.example.com/signed-640.jpg' }] } }
it('renders server-signed responsive sources with stable dimensions and explicit LCP priority', async () => {
  const render = (priority: boolean) => renderToString(createSSRApp({ render: () => h(B2bImage, { sources, priority, sizes: '(min-width: 960px) 100vw, 100vw' }) }))
  const html = await render(true)
  expect(html).toContain('image/avif'); expect(html).toContain('image/webp'); expect(html).toContain('width="1920" height="1080"'); expect(html).toContain('fetchpriority="high"'); expect(html).toContain('loading="eager"'); expect(html).not.toContain('javascript:'); expect(html).toMatch(/\balt(?:=""|(?=\s))/u)
  expect(await render(false)).toContain('loading="lazy"')
  expect(imagePreload(sources)).toMatchObject({ rel: 'preload', as: 'image', type: 'image/avif', imagesrcset: 'https://img.example.com/signed-640.avif 640w' })
  expect(imagePreload({ ...sources, fallbackUrl: 'javascript:bad' })).toBeNull()
})
it('projects only Delivery SEO and forces private preview robots without deriving replacements', () => {
  const head = deliveryHead({ route: { canonical: 'https://example.com/en/item' }, seo: { title: '<title data>', meta: [{ name: 'robots', content: 'index,follow' }, { property: 'og:title', content: 'Published title' }], alternates: [{ hreflang: 'en', href: 'https://example.com/en/item' }, { hreflang: 'de', href: 'javascript:bad' }] } }, true)
  expect(head.title).toBe('<title data>'); expect(head.meta).toContainEqual({ name: 'robots', content: 'noindex, nofollow' }); expect(head.meta).not.toContainEqual({ name: 'robots', content: 'index,follow' }); expect(head.link).toHaveLength(2)
  expect(deliveryHead({})).toEqual({ meta: [], link: [], script: [] })
})
it('serializes JSON-LD without a script-closing injection or executable separators', () => {
  const value = { '@context': 'https://schema.org', text: '</script><script>&\u2028\u2029' }, encoded = serializeJsonLd(value)
  expect(encoded).not.toMatch(/[<>&\u2028\u2029]/u); expect(JSON.parse(encoded)).toEqual(value)
  expect(() => serializeJsonLd('untrusted')).toThrow()
})
