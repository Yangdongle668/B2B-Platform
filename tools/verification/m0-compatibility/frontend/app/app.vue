<script setup lang="ts">
import texts from './texts/zh-CN.json'
useHead({ title: texts.probe.title, htmlAttrs: { lang: 'zh-CN' } })
</script>
<template>
  <main>
    <h1>{{ texts.probe.title }}</h1>
    <NuxtImg provider="probe" src="/probe.png" width="320" height="180" :alt="texts.probe.imageAlt" />
    <div class="probe-spacer" aria-hidden="true" />
    <LazyProbeCounter hydrate-on-visible />
  </main>
</template>
<style>
/* Verification fixture: keep the counter below the viewport to test deferred hydration. */
.probe-spacer { min-height: 110vh; }
</style>
