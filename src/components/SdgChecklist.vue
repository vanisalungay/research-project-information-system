<template>
  <ul class="sdg-checklist">
    <li
      v-for="goal in SDG_OPTIONS"
      :key="goal"
      class="sdg-item"
      :class="{ 'sdg-item--selected': selected.has(goal) }"
    >
      <span class="sdg-check">{{ selected.has(goal) ? '☑' : '☐' }}</span>
      <span class="sdg-name">{{ goal }}</span>
    </li>
  </ul>
</template>

<script setup>
import { computed } from 'vue'
import { SDG_OPTIONS, sdgToSet } from '@/utils/sdg'

const props = defineProps({
  value: { type: [Array, String], default: () => [] },
})

const selected = computed(() => sdgToSet(props.value))
</script>

<style scoped>
.sdg-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 6px 16px;
}

.sdg-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #475569;
  line-height: 1.5;
}

.sdg-check {
  flex-shrink: 0;
  width: 18px;
  text-align: center;
  color: #94a3b8;
  font-weight: 700;
}

.sdg-item--selected .sdg-check {
  color: #16a34a;
}

.sdg-item--selected .sdg-name {
  color: #1e293b;
  font-weight: 600;
}
</style>
