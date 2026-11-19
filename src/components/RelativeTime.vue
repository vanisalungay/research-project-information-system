<template>
  <time :datetime="isoValue" :title="exactTime">{{ label }}</time>
</template>

<script setup>
import { computed } from 'vue'

import { useNow } from '@/composables/useNow'
import {
  DEFAULT_TIME_ZONE,
  formatAbsoluteTime,
  formatRelativeTime,
  toNotificationDate,
} from '@/utils/relativeTime'

const props = defineProps({
  /** The saved notification timestamp (ISO string, Date, or epoch milliseconds). */
  value: { type: [String, Number, Date], default: null },
  /** Display zone used for the calendar-day boundaries. */
  timeZone: { type: String, default: DEFAULT_TIME_ZONE },
})

const now = useNow()

const label = computed(() =>
  formatRelativeTime(props.value, now.value, { timeZone: props.timeZone }),
)

const exactTime = computed(() =>
  formatAbsoluteTime(props.value, { timeZone: props.timeZone }),
)

const isoValue = computed(() => {
  const date = toNotificationDate(props.value)
  return date ? date.toISOString() : ''
})
</script>
