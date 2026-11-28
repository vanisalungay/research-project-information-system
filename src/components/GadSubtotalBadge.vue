<template>
  <span class="gad-subtotal" :class="`is-${state}`">
    Subtotal {{ formatGadScore(score) }} / {{ formatGadScore(max) }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { formatGadScore } from '@/utils/gadChecklist'

const props = defineProps({
  score: { type: Number, default: 0 },
  max: { type: Number, default: 2 },
})

/**
 * Full, partial and zero each get their own pill treatment. The comparison uses
 * the configured maximum, so the state follows the actual score rather than a
 * hard-coded category.
 */
const state = computed(() => {
  if (props.score >= props.max - 0.005) return 'full'
  if (props.score > 0) return 'partial'
  return 'zero'
})
</script>

<style scoped>
.gad-subtotal {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-bold);
  letter-spacing: var(--ui-tracking-label);
  white-space: nowrap;
}

.gad-subtotal.is-full {
  background: #ddf5e8;
  color: #248b58;
}

.gad-subtotal.is-partial {
  background: #fff0cf;
  color: #a66a12;
}

.gad-subtotal.is-zero {
  background: #f1f2f8;
  color: #6b6f8a;
}
</style>
