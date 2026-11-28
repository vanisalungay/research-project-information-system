<template>
  <nav class="pagination-bar" aria-label="Pagination">
    <p class="pagination-summary">
      Showing {{ rangeStart }}–{{ rangeEnd }} of {{ total }}
      {{ total === 1 ? itemLabel : itemLabelPlural }}
    </p>

    <div class="pagination-controls">
      <label v-if="showPageSize" class="pagination-size">
        <span>Rows per page</span>
        <select :value="pageSize" @change="onPageSizeChange">
          <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
        </select>
      </label>

      <button
        type="button"
        class="pagination-btn"
        :disabled="page <= 1"
        aria-label="Previous page"
        @click="go(page - 1)"
      >
        ‹
      </button>

      <button
        v-for="(token, index) in tokens"
        :key="`${token}-${index}`"
        type="button"
        class="pagination-btn pagination-number"
        :class="{ active: token === page, gap: token === '…' }"
        :disabled="token === '…'"
        :aria-current="token === page ? 'page' : undefined"
        @click="token !== '…' && go(token)"
      >
        {{ token }}
      </button>

      <button
        type="button"
        class="pagination-btn"
        :disabled="page >= pages"
        aria-label="Next page"
        @click="go(page + 1)"
      >
        ›
      </button>

      <span class="pagination-status">Page {{ page }} of {{ pages }}</span>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { buildPageTokens, pageCount, pageEnd, pageStart, PAGE_SIZE_OPTIONS } from '@/composables/usePagination.js'

const props = defineProps({
  /** Current 1-based page. */
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  itemLabel: { type: String, default: 'record' },
  itemLabelPlural: { type: String, default: 'records' },
  showPageSize: { type: Boolean, default: true },
  pageSizeOptions: { type: Array, default: () => PAGE_SIZE_OPTIONS },
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const pages = computed(() => pageCount(props.total, props.pageSize))
const rangeStart = computed(() => pageStart(props.page, props.pageSize, props.total))
const rangeEnd = computed(() => pageEnd(props.page, props.pageSize, props.total))
const tokens = computed(() => buildPageTokens(props.page, pages.value))

function go(value) {
  const next = Math.min(Math.max(1, Number(value) || 1), pages.value)
  if (next !== props.page) emit('update:page', next)
}

function onPageSizeChange(event) {
  emit('update:pageSize', Number(event.target.value))
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-top: 1px solid #e3e6ef;
  background: #fff;
  color: #5b6278;
  font-size: var(--ui-font-caption);
}

.pagination-summary {
  margin: 0;
  font-weight: var(--ui-weight-medium);
}

.pagination-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.pagination-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 6px;
}

.pagination-size select {
  height: 30px;
  padding: 0 6px;
  border: 1px solid #d7dbea;
  border-radius: 7px;
  background: #fff;
  color: #20234f;
  font: inherit;
}

.pagination-btn {
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #d7dbea;
  border-radius: 7px;
  background: #fff;
  color: #3b4160;
  font: inherit;
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #1f1a3d;
  color: #1f1a3d;
}

.pagination-btn:disabled {
  opacity: 0.45;
  cursor: default;
}

.pagination-number.active {
  background: #1f1a3d;
  border-color: #1f1a3d;
  color: #ffdf00;
}

.pagination-number.gap {
  border-color: transparent;
  background: transparent;
  opacity: 1;
}

.pagination-status {
  margin-left: 4px;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-size span {
    display: none;
  }
}
</style>
