<template>
  <div class="page">
    <div class="page-header">
      <h2>Revision Requests</h2>
      <p class="subtitle">Review returned proposals and forward revision requests to proponents.</p>
    </div>

    <div class="tabs">
      <button :class="['tab', { active: activeTab === 'awaiting' }]" @click="activeTab = 'awaiting'">
        Awaiting RPS Review ({{ awaiting.length }})
      </button>
      <button :class="['tab', { active: activeTab === 'forwarded' }]" @click="activeTab = 'forwarded'">
        Forwarded to Proponent ({{ forwarded.length }})
      </button>
    </div>

    <p v-if="loading" class="muted">Loading...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="activeTab === 'awaiting'">
      <p v-if="awaiting.length === 0" class="muted">No proposals awaiting RPS review.</p>
      <div v-for="p in awaiting" :key="p.id" class="card">
        <div class="card-top">
          <h3>{{ p.projectTitle || 'Untitled' }}</h3>
          <span class="badge returned">{{ p.status }}</span>
        </div>
        <div class="meta">
          <div><label>Returned by</label><span>{{ p.returnedByOffice || '—' }}</span></div>
          <div><label>User</label><span>{{ p.returnedByName || '—' }}</span></div>
          <div><label>Date returned</label><span>{{ formatDate(p.returnedAt) }}</span></div>
        </div>
        <div class="remarks">
          <label>Revision remarks</label>
          <p>{{ p.returnRemarks || p.remarks || 'No remarks provided.' }}</p>
        </div>
        <div class="actions">
          <router-link :to="`/rps-forward-revision/${p.id}`" class="btn primary">Review &amp; Forward</router-link>
        </div>
      </div>
    </template>

    <template v-else>
      <p v-if="forwarded.length === 0" class="muted">No revision requests forwarded yet.</p>
      <div v-for="p in forwarded" :key="p.id" class="card">
        <div class="card-top">
          <h3>{{ p.projectTitle || 'Untitled' }}</h3>
          <span class="badge forwarded">{{ p.status }}</span>
        </div>
        <div class="meta">
          <div><label>Returned by</label><span>{{ p.returnedByOffice || '—' }}</span></div>
          <div><label>Deadline</label><span>{{ formatDate(p.revisionDeadline) }}</span></div>
          <div><label>Forwarded on</label><span>{{ formatDate(p.revisionForwardedAt) }}</span></div>
          <div>
            <label>Deadline status</label>
            <span :class="{ overdue: isOverdue(p) }">{{ isOverdue(p) ? 'Overdue' : 'On time' }}</span>
          </div>
        </div>
        <div class="remarks">
          <label>Revision remarks</label>
          <p>{{ p.returnRemarks || p.remarks || 'No remarks provided.' }}</p>
          <p v-if="p.revisionNotes" class="rps-note"><label>RPS note</label>{{ p.revisionNotes }}</p>
        </div>
        <p class="hint">The proponent has not yet resubmitted this revision. Once submitted, it will appear under Submitted Proposals.</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'

const proposals = ref([])
const loading = ref(false)
const error = ref('')
const activeTab = ref('awaiting')

const awaiting = computed(() => proposals.value.filter(p => p.status === 'RETURNED_TO_RPS'))
const forwarded = computed(() => proposals.value.filter(p => p.status === 'RPS_RETURNED'))

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

function isOverdue(p) {
  if (!p.revisionDeadline) return false
  const d = new Date(p.revisionDeadline)
  return !isNaN(d.getTime()) && d.getTime() < Date.now()
}

async function fetchProposals() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/api/proposals?statusIn=RETURNED_TO_RPS&statusIn=RPS_RETURNED')
    proposals.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load revision requests.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchProposals)
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: 'Inter', sans-serif;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1f2937;
}
.subtitle {
  color: #6b7280;
  margin: 4px 0 16px;
  font-size: 14px;
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tab {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
  font-weight: 600;
  color: #374151;
}
.tab.active {
  background: #4f46e5;
  color: #fff;
  border-color: #4f46e5;
}
.muted {
  color: #6b7280;
}
.error {
  color: #dc2626;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 14px;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.card-top h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
}
.badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.badge.returned {
  background: #fef3c7;
  color: #92400e;
}
.badge.forwarded {
  background: #e0e7ff;
  color: #3730a3;
}
.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 12px 0;
}
.meta label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
}
.meta span {
  font-size: 13px;
  color: #1f2937;
}
.meta .overdue {
  color: #dc2626;
  font-weight: 700;
}
.remarks {
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 4px;
}
.remarks label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
}
.remarks p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 13px;
  white-space: pre-wrap;
}
.remarks .rps-note {
  margin-top: 8px;
  color: #3730a3;
}
.actions {
  margin-top: 12px;
}
.btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  font-size: 13px;
}
.btn.primary {
  background: #4f46e5;
  color: #fff;
}
.hint {
  color: #6b7280;
  font-size: 12px;
  margin: 10px 0 0;
}
</style>
