<template>
  <div class="panel">
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
      <p v-if="awaiting.length === 0" class="muted">No revision requests found.</p>
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
      <p v-if="forwarded.length === 0" class="muted">No revision requests found.</p>
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
.panel {
  width: 100%;
  font-family: 'Inter', sans-serif;
}

/* TABS */
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tab {
  padding: 7px 13px;
  border-radius: 8px;
  border: 1px solid #e3e6ef;
  background: #ffffff;
  color: #25245b;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.tab:hover {
  border-color: #25245b;
}

.tab:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.tab.active {
  background: #25245b;
  border-color: #25245b;
  color: #ffd400;
  font-weight: 600;
}

.muted {
  color: #78809a;
  font-size: 12px;
}

.error {
  color: #dc2626;
  font-size: 12px;
}

/* CARDS */
.card {
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.card-top h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #20234f;
}

.badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
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
  gap: 10px 14px;
  margin: 10px 0;
}

.meta label {
  display: block;
  font-size: 10px;
  font-weight: 600;
  color: #78809a;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.meta span {
  font-size: 12px;
  color: #20234f;
}

.meta .overdue {
  color: #dc2626;
  font-weight: 700;
}

.remarks {
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 6px;
}

.remarks label {
  display: block;
  font-size: 10px;
  font-weight: 600;
  color: #78809a;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.remarks p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 12px;
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
  padding: 8px 15px;
  border-radius: 7px;
  text-decoration: none;
  font-weight: 600;
  font-size: 12px;
  transition: background-color 0.15s ease;
}

.btn.primary {
  background: #ffd400;
  color: #25245b;
}

.btn.primary:hover {
  background: #f0c800;
}

.hint {
  color: #78809a;
  font-size: 11px;
  margin: 10px 0 0;
}
</style>
