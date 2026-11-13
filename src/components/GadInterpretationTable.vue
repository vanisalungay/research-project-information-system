<template>
  <div class="gad-interpretation">
    <h4 class="gad-interpretation-title">GAD Score Interpretation</h4>
    <table class="gad-interpretation-table">
      <thead>
        <tr>
          <th>GAD Score</th>
          <th>Interpretation</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(band, index) in GAD_INTERPRETATIONS"
          :key="band.range"
          :class="{ 'is-active': index === activeIndex }"
        >
          <td class="gad-interpretation-range">{{ band.range }}</td>
          <td>{{ band.label }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { GAD_INTERPRETATIONS } from '@/utils/gadChecklist'

defineProps({
  /** Index of the band the current total falls in, so it can be highlighted (-1 = none). */
  activeIndex: { type: Number, default: -1 },
})
</script>

<style scoped>
.gad-interpretation {
  margin-top: 18px;
}

.gad-interpretation-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.gad-interpretation-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.gad-interpretation-table th,
.gad-interpretation-table td {
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
}

.gad-interpretation-table th {
  background: #f1f5f9;
  font-weight: 600;
}

.gad-interpretation-range {
  white-space: nowrap;
  font-weight: 600;
  width: 110px;
}

.gad-interpretation-table tr.is-active td {
  background: #dcfce7;
  font-weight: 600;
}
</style>
