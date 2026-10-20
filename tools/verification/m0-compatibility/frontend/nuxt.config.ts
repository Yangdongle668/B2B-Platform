export default defineNuxtConfig({
  compatibilityDate: '2026-10-20',
  ssr: true,
  devtools: { enabled: false },
  modules: ['@nuxt/image'],
  image: { providers: { probe: { provider: '~/providers/probe.ts' } } },
})
