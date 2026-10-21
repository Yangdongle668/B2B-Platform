<script setup lang="ts">
import { HeroView as Hero, HeaderView, FooterView } from '../../theme'
import { text } from '@b2bweb/website-kit'
import texts from '../../texts/zh-CN'
useHead({ htmlAttrs: { lang: 'zh-CN' }, title: text(texts, 'site.brand') })
if (import.meta.server) useResponseHeader('Cache-Control').value = 'no-store'
</script>
<template><div><HeaderView :brand="text(texts,'site.brand')" :links="[{ label: text(texts,'nav.gallery'), path: '/zh/gallery' }]" /><main><Hero :title="text(texts,'hero.title')" :description="text(texts,'hero.description')" :action-label="text(texts,'hero.action')" action-path="/zh/gallery" /></main><FooterView :label="text(texts,'site.brand')" :home-label="text(texts,'nav.home')" home-path="/zh/" /></div></template>
