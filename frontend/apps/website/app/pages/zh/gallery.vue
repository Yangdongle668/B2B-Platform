<script setup lang="ts">
import { createLayoutRenderer, text } from '@b2bweb/website-kit'
import { componentExamples, definitions, schemaCatalog } from '@b2bweb/component-schemas'
import { themeRegistry } from '../../theme'
import texts from '../../texts/zh-CN'
const examples = componentExamples
const renderer = createLayoutRenderer({ schemas: schemaCatalog, registry: themeRegistry, theme: 'industrial', enabledModules: new Set(definitions.map(definition => definition.module)), siteId: 'gallery', headingLevel: 2, path: '/zh/gallery' })
const document = { version: 1 as const, sections: definitions.flatMap(definition => (examples[definition.type as keyof typeof examples] ?? []).map((example, index) => ({ id: `${definition.type}-${example.variant}-${index}`, component: definition.type, componentVersion: definition.version, variant: example.variant, props: example.props }))) }
useHead({ htmlAttrs: { lang: 'zh-CN' }, title: text(texts,'gallery.title'), meta: [{ name: 'robots', content: 'noindex,nofollow' }] })
if (import.meta.server) useResponseHeader('Cache-Control').value = 'no-store'
</script>
<template><main><h1>{{ text(texts,'gallery.title') }}</h1><component :is="renderer" :document="document" /></main></template>
