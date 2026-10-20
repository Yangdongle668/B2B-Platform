<script setup lang="ts">
import { Hero } from '@b2bweb/themes'
import { text } from '@b2bweb/website-kit'
import texts from '../../texts/zh-CN'
useHead({ htmlAttrs: { lang: 'zh-CN' }, title: text(texts,'gallery.title'), meta: [{ name: 'robots', content: 'noindex,nofollow' }] })
if (import.meta.server) useResponseHeader('Cache-Control').value = 'no-store'
</script>
<template><main><h1>{{ text(texts,'gallery.title') }}</h1><Hero :heading-level="2" :title="text(texts,'hero.title')" :description="text(texts,'hero.description')" :action-label="text(texts,'nav.home')" action-path="/zh/" /></main></template>
