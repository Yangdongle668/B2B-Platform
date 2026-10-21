import { it, expect } from 'vitest'
import { richTextPlainText, richTextToc, renderRichText, safeLink } from './richtext'
it('drops unknown HTML and unsafe links, resolves content only to published paths', () => {
  const h = (tag: string, attrs: Record<string, unknown>, children: unknown[]): object => ({ tag, attrs, children })
  const doc = { type: 'doc', content: [{ type: 'text', text: '<script>text</script>', marks: [{ type: 'link', attrs: { href: 'javascript:alert(1)' } }] }, { type: 'script', content: [{ type: 'text', text: 'bad' }] }, { type: 'image', attrs: { src: 'https://evil.test' } }] }
  expect(renderRichText(doc, { h, resolveLink: () => undefined })).toEqual(['<script>text</script>'])
  expect(safeLink('content://123', () => '/zh/products/item')).toBe('/zh/products/item')
  expect(safeLink('content://123', () => undefined)).toBeUndefined()
  expect(safeLink('content://123', () => '//evil.test')).toBeUndefined()
  expect(safeLink('data:text/html,evil', () => undefined)).toBeUndefined()
  expect(safeLink('https://user:password@example.test', () => undefined)).toBeUndefined()
})
it('produces text and stable heading IDs without using H1', () => {
  const doc = { type: 'doc', content: [{ type: 'heading', attrs: { level: 1 }, content: [{ type: 'text', text: 'Title' }] }, { type: 'paragraph', content: [{ type: 'text', text: 'Body' }] }] }
  expect(richTextPlainText(doc)).toBe('Title\nBody'); expect(richTextToc(doc)).toEqual([{ id: 'heading-1', level: 2, title: 'Title' }])
})
