import js from '@eslint/js'
import ts from 'typescript-eslint'
import vue from 'eslint-plugin-vue'
export default [
  { ignores: ['**/node_modules/**','**/.nuxt/**','**/.output/**','**/dist/**','**/schema.d.ts'] },
  js.configs.recommended,
  ...ts.configs.recommended,
  ...vue.configs['flat/essential'],
  { files: ['frontend/apps/website/app/pages/**/*.vue'], rules: { 'vue/multi-word-component-names': 'off' } },
  { files: ['**/*.vue'], rules: { 'no-useless-assignment': 'off' }, languageOptions: { parserOptions: { parser: ts.parser } } },
  { languageOptions: { globals: { window: 'readonly', MouseEvent: 'readonly', Element: 'readonly', HTMLElement: 'readonly', setTimeout: 'readonly', clearTimeout: 'readonly', setInterval: 'readonly', clearInterval: 'readonly', localStorage: 'readonly', fetch: 'readonly', RequestInit: 'readonly', MessageEvent: 'readonly', MessageEventSource: 'readonly', console: 'readonly', process: 'readonly', navigateTo: 'readonly', useHead: 'readonly', useResponseHeader: 'readonly', defineNuxtConfig: 'readonly', defineEventHandler: 'readonly', sendRedirect: 'readonly', useRoute: 'readonly', definePageMeta: 'readonly', setResponseHeader: 'readonly', defineNuxtPlugin: 'readonly' } }, rules: { '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }] } },
]
