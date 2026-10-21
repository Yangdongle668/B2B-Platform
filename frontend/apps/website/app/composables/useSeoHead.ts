import { useHead } from '#imports'
import { toValue } from 'vue'
import type { MaybeRefOrGetter } from 'vue'
import { deliveryHead } from '@b2bweb/website-kit'
import type { DeliveryHeadInput } from '@b2bweb/website-kit'
export function useSeoHead(resolve: MaybeRefOrGetter<DeliveryHeadInput>, preview: MaybeRefOrGetter<boolean> = false) { return useHead(() => deliveryHead(toValue(resolve), toValue(preview))) }
