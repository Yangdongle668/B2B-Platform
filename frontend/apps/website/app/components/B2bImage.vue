<script setup lang="ts">
import { computed } from 'vue'
import { B2bImage as Image, useRefs } from '@b2bweb/website-kit'
import type { ImageSources, MediaReference } from '@b2bweb/website-kit'
import { useImagePreload } from '../composables/useImagePreload'
const props = withDefaults(defineProps<{ media: MediaReference; preset: ImageSources['preset']; priority?: boolean; sizes?: string }>(), { priority: false, sizes: '100vw' })
const refs = useRefs()
const sources = computed(() => refs.image(props.media, props.preset))
useImagePreload(() => sources.value ?? undefined, () => props.priority, () => props.sizes)
</script>
<template><Image v-if="sources" :sources="sources" :priority="props.priority" :sizes="props.sizes" /></template>
