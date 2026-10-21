import { it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { EditorCanvasHost } from './editor-host'
it('refuses unsafe canvas URLs before an iframe can be loaded', async () => {
  const render = (url: string) => renderToString(createSSRApp({ render: () => h(EditorCanvasHost, { url, title: '画布', document: { version: 1, sections: [] }, locale: 'zh-CN' }) }))
  for (const url of ['javascript:alert(1)', 'https://user:password@example.com/__editor', 'http://example.com/__editor', 'https://example.com/other', '/__editor', 'invalid']) expect(await render(url)).not.toContain('<iframe')
  const html = await render('https://example.com/__editor?token=fixture')
  expect(html).toContain('<iframe'); expect(html).toContain('referrerpolicy="no-referrer"'); expect(html).toContain('sandbox="allow-scripts allow-same-origin"')
})
