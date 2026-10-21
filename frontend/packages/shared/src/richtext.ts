export interface RichTextNode { type: string; text?: string; attrs?: Record<string, unknown>; marks?: Array<{ type: string; attrs?: Record<string, unknown> }>; content?: RichTextNode[] }
const blocks: Readonly<Record<string, string>> = { paragraph: 'p', bulletList: 'ul', orderedList: 'ol', listItem: 'li', blockquote: 'blockquote', codeBlock: 'pre', table: 'table', tableRow: 'tr', tableCell: 'td', tableHeader: 'th' }
export const richTextNodes = Object.freeze(['doc', 'text', 'heading', 'hardBreak', 'horizontalRule', ...Object.keys(blocks), 'image', 'video', 'alert', 'productCard', 'cta'])
export const richTextMarks = Object.freeze(['bold', 'italic', 'strike', 'code', 'link'])
export function safeLink(value: unknown, resolveContent: (id: string) => string | undefined): string | undefined {
  if (typeof value !== 'string') return undefined
  const reference = /^content:\/\/([1-9][0-9]*)$/.exec(value)
  if (reference) { const path = resolveContent(reference[1]!); return path?.startsWith('/') && !path.startsWith('//') && !/[\\\s]/u.test(path) ? path : undefined }
  try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : undefined } catch { return undefined }
}
export function richTextPlainText(node: RichTextNode): string {
 let count = 0
 const text = (current: RichTextNode, depth: number): string => { if (++count > 10_000 || depth > 20 || !richTextNodes.includes(current.type)) return ''; if (current.type === 'text') return current.text ?? ''; return (current.content ?? []).map(child => text(child, depth + 1)).join(['doc', 'bulletList', 'orderedList'].includes(current.type) ? '\n' : '') }
 return text(node, 0)
}
export function richTextToc(node: RichTextNode): Array<{ id: string; level: number; title: string }> {
  const headings: Array<{ id: string; level: number; title: string }> = []
  function visit(current: RichTextNode, depth: number) { if (depth > 20 || !richTextNodes.includes(current.type)) return; if (current.type === 'heading') headings.push({ id: `heading-${headings.length + 1}`, level: Math.min(6, Math.max(2, Number(current.attrs?.level) || 2)), title: richTextPlainText(current) }); current.content?.forEach(child => visit(child, depth + 1)) }
  visit(node, 0); return headings
}
/** All output goes through h; arbitrary attributes/HTML and unresolved embeds are never forwarded. */
export function renderRichText<T>(doc: RichTextNode, options: { h: (tag: string, attrs: Record<string, unknown>, children: Array<T | string>) => T; resolveLink: (id: string) => string | undefined; components?: Record<string, (node: RichTextNode) => T | null>; unknown?: (type: string) => void; headingOffset?: number }): Array<T | string> {
  let heading = 0, count = 0
  function render(node: RichTextNode, depth: number): Array<T | string> {
    if (++count > 10_000 || depth > 20 || !richTextNodes.includes(node.type)) { options.unknown?.(node.type); return [] }
    const content = (node.content ?? []).flatMap(child => render(child, depth + 1))
    if (node.type === 'doc') return content
    if (node.type === 'text') {
      let output: Array<T | string> = [node.text ?? '']
      for (const mark of node.marks ?? []) {
        const tag = ({ bold: 'strong', italic: 'em', strike: 's', code: 'code' } as Record<string, string>)[mark.type]
        if (tag) output = [options.h(tag, {}, output)]
        else if (mark.type === 'link') { const href = safeLink(mark.attrs?.href, options.resolveLink); if (href) output = [options.h('a', { href, rel: 'noopener' + (mark.attrs?.nofollow ? ' nofollow' : '') + (mark.attrs?.sponsored ? ' sponsored' : '') }, output)] }
        else options.unknown?.(mark.type)
      }
      return output
    }
    if (node.type === 'heading') { const level = Math.min(6, Math.max(2, (Number(node.attrs?.level) || 2) + (options.headingOffset ?? 0))); return [options.h(`h${level}`, { id: `heading-${++heading}` }, content)] }
    if (blocks[node.type]) return [options.h(blocks[node.type]!, {}, content)]
    if (node.type === 'hardBreak' || node.type === 'horizontalRule') return [options.h(node.type === 'hardBreak' ? 'br' : 'hr', {}, [])]
    const component = options.components?.[node.type]?.(node)
    return component ? [component] : []
  }
  return render(doc, 0)
}
