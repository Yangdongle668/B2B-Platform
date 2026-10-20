import type { ProviderGetImage } from '@nuxt/image'
// Fixture only: verifies the provider extension point without signing production URLs.
export const getImage: ProviderGetImage = (src, { modifiers }) => ({
  url: `${src}?probeWidth=${encodeURIComponent(String(modifiers.width || ''))}`,
})
export default () => ({ getImage })
