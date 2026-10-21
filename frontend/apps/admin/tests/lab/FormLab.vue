<script setup lang="ts">
import { ref, computed } from 'vue'
import { SchemaForm, formSubmission, referenceControls } from '@b2bweb/admin-kit'
import type { FormSchema, SchemaFormContext } from '@b2bweb/admin-kit'
const value = ref<Record<string, unknown>>({ title: '', mode: 'image', imageText: '图片说明', videoText: '隐藏值', secret: '****mask', items: ['one'], source: { $source: { provider: 'sample.items', params: { limit: 2 } } }, richtext: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: '原始内容' }] }] } })
const readonly = ref(false), automatic = ref(false), valid = ref(false)
const context = computed<SchemaFormContext>(() => ({ locale: 'zh-CN', mode: 'component', automatic: automatic.value }))
const schema: FormSchema = { type: 'object', additionalProperties: false, required: ['title'], properties: {
  title: { type: 'string', title: '标题', minLength: 1 }, mode: { type: 'string', title: '模式', enum: ['image', 'video'], 'x-enum-labels': ['图片', '视频'] },
  imageText: { type: 'string', title: '图片文字', 'x-visible-if': { prop: 'mode', equals: 'image' } }, videoText: { type: 'string', title: '视频文字', 'x-visible-if': { prop: 'mode', equals: 'video' } },
  secret: { type: 'string', title: '密钥', 'x-secret': true }, items: { type: 'array', title: '项目', minItems: 1, maxItems: 3, items: { type: 'string', title: '项目文字' } },
  media: { type: 'object', title: '媒体', 'x-editor': 'media', additionalProperties: false, required: ['$media'], properties: { $media: { type: 'string' } } },
  source: { type: 'object', title: '数据源', 'x-editor': 'data-source' }, richtext: { type: 'object', title: '正文', 'x-editor': 'richtext' },
} }
const controls = referenceControls({ search: async kind => kind === 'data-source' ? [{ key: 'sample.items', label: '示例数据源' }] : [{ key: '11', label: '已扫描图片' }], sourceSchema: async () => ({ type: 'object', properties: { limit: { title: '数量', type: 'integer', minimum: 1, maximum: 10 } } }) })
const submission = computed(() => JSON.stringify(formSubmission(schema, value.value)))
</script>
<template><main><h1>Schema 表单协议夹具</h1><button type="button" @click="readonly = !readonly">切换只读</button><button type="button" @click="automatic = !automatic">切换自动语言</button><p role="status">{{ valid ? '校验通过' : '校验失败' }}</p><SchemaForm v-model="value" :schema="schema" :context="context" :readonly="readonly" :controls="controls" @validate="valid = $event.valid" /><output aria-label="提交内容">{{ submission }}</output></main></template>
