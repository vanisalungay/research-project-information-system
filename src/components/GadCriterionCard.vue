<template>
  <section class="gad-card">
    <header class="gad-card-header">
      <span class="gad-card-number">{{ criterion.number }}</span>
      <div class="gad-card-heading">
        <h3 class="gad-card-title">{{ criterion.title }}</h3>
        <p class="gad-card-note">{{ criterion.note || 'Maximum score: 2' }}</p>
      </div>
      <GadSubtotalBadge :score="subtotal" :max="max" />
    </header>

    <GadQuestionRow
      v-for="item in criterion.items"
      :key="item.key"
      :item="item"
      :response="responses?.[item.key] || ''"
      :comment="comments?.[item.key] || ''"
      :readonly="readonly"
      @update:response="emit('update:response', item.key, $event)"
      @update:comment="emit('update:comment', item.key, $event)"
      @clear="emit('clear', item.key)"
    />
  </section>
</template>

<script setup>
import GadQuestionRow from '@/components/GadQuestionRow.vue'
import GadSubtotalBadge from '@/components/GadSubtotalBadge.vue'
import { GAD_CRITERION_MAX } from '@/utils/gadChecklist'

defineProps({
  criterion: { type: Object, required: true },
  responses: { type: Object, default: () => ({}) },
  comments: { type: Object, default: () => ({}) },
  subtotal: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:response', 'update:comment', 'clear'])

// Every official criterion is worth 2 points; the component still receives the
// value rather than hiding it, so the badge and the calculation use one source.
const max = GAD_CRITERION_MAX
</script>

<style scoped>
.gad-card {
  border: 1px solid #e1e4ef;
  border-radius: 13px;
  background: #ffffff;
  overflow: hidden;
}

.gad-card-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  border-bottom: 1px solid #eceef6;
}

.gad-card-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 25px;
  height: 25px;
  border-radius: 7px;
  background: #211d43;
  color: #ffd21f;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.gad-card-heading {
  flex: 1;
  min-width: 0;
}

.gad-card-title {
  margin: 0;
  color: #24274a;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
}

.gad-card-note {
  margin: 1px 0 0;
  color: #6f7694;
  font-size: 9.5px;
  line-height: 1.35;
}

@media (max-width: 480px) {
  .gad-card-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .gad-card-heading {
    flex-basis: calc(100% - 34px);
  }
}
</style>
