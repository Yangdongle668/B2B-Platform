<script setup lang="ts">
import texts from '../texts/zh-CN.json'
const count = ref(0)
const hydrated = ref(false)
onMounted(() => { hydrated.value = true })
</script>
<template>
  <button data-testid="counter" :data-hydrated="hydrated" type="button" @click="count++">{{ texts.probe.increment }}: {{ count }}</button>
</template>
