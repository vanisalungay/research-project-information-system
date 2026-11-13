<template>
  <div v-if="remarks && remarks.length" class="section-remarks-notice">
    <div class="notice-head">
      <svg
        class="notice-icon"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
      </svg>
      <span class="notice-title">
        Reviewer remark{{ remarks.length > 1 ? 's' : '' }} for this section
      </span>
    </div>

    <div v-for="(remark, index) in remarks" :key="remark.id ?? index" class="remark-item">
      <p class="remark-text">{{ remark.remark }}</p>
      <p class="remark-meta">
        <span v-if="remark.reviewerRole" class="remark-role">{{ remark.reviewerRole }}</span>
        <span v-if="remark.reviewerName">{{ remark.reviewerName }}</span>
        <span v-if="remark.createdAt">{{ formatDate(remark.createdAt) }}</span>
      </p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  /** Section key the notice belongs to (used for the accessible label only). */
  sectionKey: {
    type: String,
    default: '',
  },
  /** Remarks for this section in the currently active revision round. */
  remarks: {
    type: Array,
    default: () => [],
  },
})

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}
</script>

<style scoped>
.section-remarks-notice {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-left: 4px solid #f59e0b;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.notice-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: #b45309;
}

.notice-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #92400e;
}

.remark-item + .remark-item {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #fde68a;
}

.remark-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #3f3f46;
  white-space: pre-wrap;
}

.remark-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 6px 0 0;
  font-size: 11px;
  color: #78716c;
}

.remark-role {
  font-weight: 700;
  color: #92400e;
  background: #fde68a;
  border-radius: 999px;
  padding: 1px 8px;
}
</style>
