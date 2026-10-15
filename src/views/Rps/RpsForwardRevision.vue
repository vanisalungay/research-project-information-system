<template>
  <div class="page">
    <div class="page-header">
      <button class="back" @click="$router.push('/rps-revision-requests')">← Back to Revision Requests</button>
      <h2>Forward Revision Request</h2>
      <p class="subtitle">Review the returning office's remarks, set the deadline, and forward to the proponent.</p>
    </div>

    <p v-if="loading" class="muted">Loading...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="proposal.id">
      <div class="layout">
        <div class="main">
          <div class="card">
            <h3>Proposal</h3>
            <div class="meta">
              <div><label>Title</label><span>{{ proposal.projectTitle || 'Untitled' }}</span></div>
              <div><label>Document ID</label><span>{{ proposal.documentId || '—' }}</span></div>
              <div><label>Proponent</label><span>{{ proposal.proponent?.name || proposal.projectLeader || '—' }}</span></div>
            </div>
          </div>

          <div class="card">
            <h3>Return Details (from reviewing office)</h3>
            <div class="meta">
              <div><label>Returned by</label><span>{{ proposal.returnedByOffice || '—' }}</span></div>
              <div><label>User</label><span>{{ proposal.returnedByName || '—' }}</span></div>
              <div><label>Date returned</label><span>{{ formatDate(proposal.returnedAt) }}</span></div>
              <div><label>Status</label><span>{{ proposal.status || '—' }}</span></div>
            </div>
            <div class="remarks">
              <label>Revision remarks</label>
              <p>{{ proposal.returnRemarks || proposal.remarks || 'No remarks provided.' }}</p>
            </div>
          </div>

          <div v-if="fieldRemarks.length > 0" class="card">
            <h3>Field remarks recorded for this proposal ({{ fieldRemarks.length }})</h3>
            <p class="hint">
              These were left beside individual fields on the review page and will be visible to the
              proponent next to each field.
            </p>
            <div v-for="item in fieldRemarks" :key="item.id" class="flagged-section">
              <span class="flagged-section-head">
                {{ item.fieldLabel || item.fieldKey }} · {{ item.sectionKey }}
                <template v-if="item.reviewerRole"> — {{ item.reviewerRole }}</template>
              </span>
              <p class="flagged-section-remark">{{ item.remark }}</p>
            </div>
          </div>

          <div v-if="existingFlaggedSections.length > 0" class="card">
            <h3>Sections flagged by the reviewing office</h3>
            <div v-for="section in existingFlaggedSections" :key="section.key" class="flagged-section">
              <span class="flagged-section-head">
                {{ section.key }} · {{ section.label }}
                <template v-if="section.reviewerRole"> — {{ section.reviewerRole }}</template>
              </span>
              <p v-for="(remark, index) in section.remarks" :key="remark.id ?? index" class="flagged-section-remark">
                {{ remark.remark }}
              </p>
            </div>
          </div>

          <div class="card">
            <SectionRemarksEditor v-model="sectionRemarks" />
          </div>

          <div class="card">
            <h3>Set Revision Deadline</h3>
            <label class="field-label">Deadline for resubmission</label>
            <input type="date" v-model="deadline" />
            <p class="hint">Only RPS can set the revision deadline. The proponent will be required to submit before this date.</p>
          </div>

          <div class="card">
            <h3>RPS Note (optional)</h3>
            <textarea v-model="notes" placeholder="Add any additional instructions for the proponent..."></textarea>
          </div>

          <button class="btn primary" :disabled="submitting || !deadline" @click="forward">
            {{ submitting ? 'Forwarding...' : 'Forward to Proponent' }}
          </button>
          <p v-if="success" class="success">{{ success }}</p>
          <p v-if="submitError" class="error">{{ submitError }}</p>
        </div>

        <div class="side">
          <SubmissionHistory :proposal-id="proposal.id" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'
import SubmissionHistory from '@/components/SubmissionHistory.vue'
import SectionRemarksEditor from '@/components/SectionRemarksEditor.vue'
import { activeRemarksBySection, PROPOSAL_SECTIONS, toSectionRemarkPayload } from '@/utils/proposalSections'
import { buildFieldRemarkIndex } from '@/utils/proposalFields'

const route = useRoute()
const router = useRouter()
const userStore = useUserDataStore()

const proposal = ref({})
const loading = ref(false)
const error = ref('')
const deadline = ref('')
const notes = ref('')
const submitting = ref(false)
const success = ref('')
const submitError = ref('')

// RPS's own per-section remarks, plus the remarks already recorded by the
// reviewing office that returned the proposal.
const sectionRemarks = ref({})
const existingRemarks = ref(null)

const existingFlaggedSections = computed(() => {
  const map = activeRemarksBySection(existingRemarks.value)
  return PROPOSAL_SECTIONS.filter((section) => (map[section.key] || []).length > 0).map((section) => ({
    key: section.key,
    label: section.label,
    reviewerRole: map[section.key][0]?.reviewerRole || '',
    remarks: map[section.key]
  }))
})

/** Field-level remarks already recorded for this proposal (active round). */
const fieldRemarks = computed(() => {
  const map = buildFieldRemarkIndex(existingRemarks.value)
  return Object.values(map).flat()
})

async function fetchExistingSectionRemarks(id) {
  try {
    const res = await api.get(`/api/proposals/${id}/field-remarks`)
    existingRemarks.value = res.data || null
  } catch (err) {
    // The forward step must still work when the remark history cannot be read.
    console.error(err)
    existingRemarks.value = null
  }
}

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

async function fetchProposal() {
  loading.value = true
  error.value = ''
  try {
    const id = route.params.id
    const res = await api.get(`/api/proposals/${id}`)
    proposal.value = res.data
    await fetchExistingSectionRemarks(id)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal.'
  } finally {
    loading.value = false
  }
}

async function forward() {
  if (!deadline.value) {
    submitError.value = 'Please set a revision deadline.'
    return
  }
  submitting.value = true
  submitError.value = ''
  success.value = ''
  try {
    await api.put(`/api/proposals/${proposal.value.id}/forward-revision`, {
      forwardedById: userStore.user?.id || null,
      forwardedByName: userStore.user?.name || null,
      deadline: deadline.value,
      notes: notes.value || null,
      sectionRemarks: toSectionRemarkPayload(sectionRemarks.value)
    })
    success.value = 'Revision request forwarded to the proponent.'
    setTimeout(() => router.push('/rps-revision-requests'), 1200)
  } catch (err) {
    console.error(err)
    submitError.value = err.response?.data?.message || 'Failed to forward revision request.'
  } finally {
    submitting.value = false
  }
}

onMounted(fetchProposal)
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: 'Inter', sans-serif;
}
.page-header h2 {
  margin: 8px 0 0;
  font-size: 22px;
  color: #1f2937;
}
.back {
  background: none;
  border: none;
  color: #4f46e5;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}
.subtitle {
  color: #6b7280;
  margin: 4px 0 16px;
  font-size: 14px;
}
.layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
  align-items: start;
}
.main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.card h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #111827;
}
.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
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
.remarks {
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 10px;
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
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.flagged-section + .flagged-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.flagged-section-head {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #3730a3;
}

.flagged-section-remark {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  white-space: pre-wrap;
}
input[type='date'],
textarea {
  width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  font-size: 14px;
}
textarea {
  min-height: 90px;
  resize: vertical;
}
.hint {
  color: #6b7280;
  font-size: 12px;
  margin: 8px 0 0;
}
.btn {
  display: inline-block;
  padding: 10px 16px;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
}
.btn.primary {
  background: #4f46e5;
  color: #fff;
}
.btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.success {
  color: #16a34a;
  font-weight: 600;
}
.error {
  color: #dc2626;
}
.muted {
  color: #6b7280;
}
.side {
  min-width: 0;
}
</style>
