<script setup lang="ts">
import { useSeoHead } from '../../composables/useSeoHead'
import { useRequestFetch, useAsyncData, createError } from '#imports'
import { shallowRef } from 'vue'
import { createLayoutRenderer, text } from '@b2bweb/website-kit'
import { schemaCatalog } from '@b2bweb/component-schemas'
import { isLayoutDocument } from '@b2bweb/shared/editor-protocol'
import { themeRegistry } from '../../theme'
import texts from '../../texts/zh-CN'
const token = String(useRoute().params.token ?? '')
interface PreviewData { snapshot: { payload: { parts?: { layout?: unknown } } }; siteId: string; enabledModules: string[] }
const request = useRequestFetch()
const { data, error } = await useAsyncData('preview', () => request<PreviewData>('/__preview/resolve', { method: 'POST', body: { token } }))
if (error.value || !data.value) throw createError({ statusCode: error.value?.statusCode === 404 ? 404 : 503, statusMessage: text(texts, 'preview.unavailable') })
const layout = data.value.snapshot.payload.parts?.layout
if (!isLayoutDocument(layout)) throw createError({ statusCode: 503, statusMessage: text(texts, 'preview.unavailable') })
const renderer = shallowRef(createLayoutRenderer({ schemas: schemaCatalog, registry: themeRegistry, theme: 'industrial', enabledModules: new Set(data.value.enabledModules), siteId: data.value.siteId, path: '/__preview' }))
useSeoHead({}, true)
if (import.meta.server) { useResponseHeader('Cache-Control').value = 'no-store'; useResponseHeader('CDN-Cache-Control').value = 'no-store'; useResponseHeader('X-Robots-Tag').value = 'noindex, nofollow'; useResponseHeader('Referrer-Policy').value = 'no-referrer' }
</script>
<template><main><p role="status">{{ text(texts, 'preview.banner') }}</p><component :is="renderer" :document="layout" /></main></template>
