<script setup lang="ts">
import { B2bImage as Image } from '@b2bweb/website-kit'
import type { ImageSources } from '@b2bweb/website-kit'
import { useImagePreload } from '../composables/useImagePreload'
const props = withDefaults(defineProps<{ sources: ImageSources; priority?: boolean; sizes?: string }>(), { priority: false, sizes: '100vw' })
useImagePreload(() => props.sources, () => props.priority, () => props.sizes)
</script>
<template><Image :sources="props.sources" :priority="props.priority" :sizes="props.sizes" /></template>
