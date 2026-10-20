<script setup lang="ts">
import { Hero } from '@b2bweb/themes'
import { text } from '@b2bweb/website-kit'
import texts from '../../texts/zh-CN'
useHead({ htmlAttrs: { lang: 'zh-CN' }, title: text(texts, 'site.brand') })
if (import.meta.server) useResponseHeader('Cache-Control').value = 'no-store'
</script>
<template><div><nav><span>{{ text(texts,'site.brand') }}</span><NuxtLink to="/zh/gallery">{{ text(texts,'nav.gallery') }}</NuxtLink></nav><main><Hero :title="text(texts,'hero.title')" :description="text(texts,'hero.description')" :action-label="text(texts,'hero.action')" action-path="/zh/gallery" /></main></div></template>
