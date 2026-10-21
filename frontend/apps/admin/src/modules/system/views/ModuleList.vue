<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAdminRuntime } from '@b2bweb/admin-kit'
import type { SiteBootstrap } from '@b2bweb/admin-kit'
import { api } from '@b2bweb/api-client'
import { texts } from '../texts'
const runtime = useAdminRuntime()
const modules = ref<SiteBootstrap['modules']>([])
const loading = ref(true)
const failed = ref(false)
onMounted(async () => {
  const siteId = runtime.siteId
  try {
    const bootstrap = await api<SiteBootstrap>('/api/admin/v1/system/bootstrap', { headers: { 'X-Site-Id': siteId ?? '' } })
    if (runtime.siteId === siteId) modules.value = bootstrap.modules
  } catch { failed.value = true } finally { loading.value = false }
})
</script>
<template><section><h2>{{ texts.title }}</h2><el-alert v-if="runtime.readonly" :title="texts.readonly" type="info" :closable="false" /><el-alert v-if="failed" :title="texts.failed" type="error" :closable="false" /><el-skeleton v-else-if="loading" animated :aria-label="texts.loading" /><template v-else><h3>{{ texts.enabled }}</h3><ul><li v-for="module in modules" :key="module.moduleId">{{ module.name }}</li></ul><el-empty v-if="!modules.length" :description="texts.empty" /></template></section></template>
