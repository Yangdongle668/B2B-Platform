import { useHead } from '#imports'
import { toValue } from 'vue'
import type { MaybeRefOrGetter } from 'vue'
import { imagePreload } from '@b2bweb/website-kit'
import type { ImageSources } from '@b2bweb/website-kit'
export function useImagePreload(sources: MaybeRefOrGetter<ImageSources | undefined>, priority: MaybeRefOrGetter<boolean>, sizes: MaybeRefOrGetter<string> = '100vw') {
  return useHead(() => { const source = toValue(sources); const link = toValue(priority) && source ? imagePreload(source, toValue(sizes)) : null; return { link: link ? [link] : [] } })
}
