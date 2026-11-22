<template>
  <p :id="id" class="pw-field-hint">{{ text }}</p>
</template>

<script setup>
import { computed } from 'vue'
import { PASSWORD_FIELD_HINTS } from '@/utils/passwordPolicy'

const props = defineProps({
  /** 'current', 'new' or 'confirm'. */
  kind: { type: String, default: 'new' },
  /** The id the input points at with aria-describedby. */
  id: { type: String, default: undefined },
})

const text = computed(() => PASSWORD_FIELD_HINTS[props.kind] || PASSWORD_FIELD_HINTS.new)
</script>

<style scoped>
.pw-field-hint {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 0.75rem;
  line-height: 1.45;
}
</style>
