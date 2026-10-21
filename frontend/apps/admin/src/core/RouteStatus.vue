<script setup lang="ts">
import { texts } from '../texts'
</script>
<template><el-result v-if="$route.name === 'forbidden'" icon="warning" :title="texts.forbidden" :sub-title="texts.forbiddenHint" /><el-result v-else-if="$route.name === 'not-found' || $route.name === 'unmatched'" icon="info" :title="texts.notFound" :sub-title="texts.notFoundHint" /></template>
