<template>
  <div class="page">
    <!-- The same proposal header every other proposal page uses. This page's action
         (setting the deadline and forwarding) stays in the form below. -->
    <ProposalReviewHeader
      :title="proposal.projectTitle"
      :document-id="proposal.documentId || proposal.proposalCode"
      :status="proposal.status"
      :show-download="false"
      @back="router.push('/rps-revision-requests')"
    />
    <p class="subtitle">
      <template v-if="isOvcrigeRejection">
        {{ proposal.returnedByOffice || 'A reviewing office' }} rejected this proposal and returned it
        to ORPS. Review the reason and forward the rejection to the proponent.
      </template>
      <template v-else>
        Review the returning office's remarks, set the deadline, and forward to the proponent.
      </template>
    </p>

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
              <div><label>Status</label><span>{{ proposalStatusLabel(proposal.status) || '—' }}</span></div>
            </div>
            <div class="remarks">
              <label>{{ isOvcrigeRejection ? 'Rejection remarks' : 'Revision remarks' }}</label>
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

          <div v-if="!isOvcrigeRejection" class="card">
            <SectionRemarksEditor v-model="sectionRemarks" />
          </div>

          <div v-if="!isOvcrigeRejection" class="card">
            <h3>Set Revision Deadline</h3>
            <label class="field-label">Deadline for resubmission</label>
            <input type="date" v-model="deadline" />
            <p class="hint">Only RPS can set the revision deadline. The proponent will be required to submit before this date.</p>
          </div>

          <div class="card">
            <h3>{{ isOvcrigeRejection ? 'ORPS Note (optional)' : 'RPS Note (optional)' }}</h3>
            <textarea
              v-model="notes"
              :placeholder="isOvcrigeRejection
                ? 'Add any note that should accompany the rejection sent to the proponent...'
                : 'Add any additional instructions for the proponent...'"
            ></textarea>
          </div>

          <button
            v-if="isOvcrigeRejection"
            class="btn btn-danger"
            :disabled="submitting"
            @click="finalizeRejection"
          >
            {{ submitting ? 'Forwarding...' : 'Forward Rejection to Proponent' }}
          </button>
          <button
            v-else
            class="btn btn-primary"
            :disabled="submitting || !deadline"
            @click="forward"
          >
            {{ submitting ? 'Forwarding...' : 'Forward to Proponent' }}
          </button>
          <p v-if="success" class="success">{{ success }}</p>
          <p v-if="submitError" class="error">{{ submitError }}</p>
        </div>

      </div>

      <!-- Full width: a 360px side column was what made the history feel crammed. -->
      <SubmissionHistory :proposal-id="proposal.id" />
    </template>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import ProposalReviewHeader from '@/components/ProposalReviewHeader.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'
import { useUserDataStore } from '@/stores/userData'
import SubmissionHistory from '@/components/SubmissionHistory.vue'
import SectionRemarksEditor from '@/components/SectionRemarksEditor.vue'
import { activeRemarksBySection, PROPOSAL_SECTIONS, toSectionRemarkPayload } from '@/utils/proposalSections'
import { buildFieldRemarkIndex } from '@/utils/proposalFields'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserDataStore()
const { dialogState, showConfirm } = useDialog()

const proposal = ref({})
const loading = ref(false)
const error = ref('')
const deadline = ref('')
const notes = ref('')
const submitting = ref(false)
const success = ref('')
const submitError = ref('')

/**
 * A reviewing office (OVCRIGE or OVCAF) rejecting a proposal sends it here as
 * RETURNED_TO_RPS with return_kind=REJECTION. ORPS completes the rejection and is the
 * office that informs the proponent; the revision-forward form is not shown for this
 * kind of return.
 */
const isOvcrigeRejection = computed(
  () =>
    String(proposal.value.status || '').toUpperCase() === 'RETURNED_TO_RPS' &&
    String(proposal.value.returnKind || '').toUpperCase() === 'REJECTION',
)

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
  if (submitting.value) return
  if (!deadline.value) {
    submitError.value = 'Please set a revision deadline.'
    return
  }
  const confirmed = await showConfirm(
    `Forward the revision request for "${proposal.value.projectTitle || 'this proposal'}" ` +
      `to the proponent? The deadline is ${deadline.value} and the proponent will be notified.`,
    {
      title: 'Forward Revision Request',
      type: 'warning',
      confirmText: 'Forward to Proponent',
    },
  )
  if (!confirmed) return
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

async function finalizeRejection() {
  if (submitting.value) return
  const confirmed = await showConfirm(
    `Forward the rejection of "${proposal.value.projectTitle || 'this proposal'}" to the ` +
      'proponent? The proposal will be closed as rejected.',
    {
      title: 'Forward Rejection to Proponent',
      type: 'danger',
      confirmText: 'Forward Rejection',
    },
  )
  if (!confirmed) return
  submitting.value = true
  submitError.value = ''
  success.value = ''
  try {
    await api.put(`/api/proposals/${proposal.value.id}/finalize-rejection`, {
      remarks: notes.value || null,
    })
    success.value = 'Rejection forwarded to the proponent.'
    setTimeout(() => router.push('/rps-revision-requests'), 1200)
  } catch (err) {
    console.error(err)
    submitError.value =
      err.response?.data?.error ||
      err.response?.data?.message ||
      'Failed to forward the rejection.'
  } finally {
    submitting.value = false
  }
}

onMounted(fetchProposal)

// Vue Router reuses this component when only the id changes, so the route id is watched:
// opening another proposal, or Back/Forward between two, reloads the right record.
watch(() => route.params.id, () => fetchProposal())
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: var(--ui-font-ui);
}
.page-header h2 {
  margin: 8px 0 0;
  font-size: var(--ui-font-page-title);
  color: #1f2937;
  font-weight: var(--ui-weight-bold);
}
.page-header > .back-button {
  margin-bottom: 8px;
}
.subtitle {
  color: #6b7280;
  margin: 4px 0 16px;
  font-size: var(--ui-font-small);
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
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
  font-size: var(--ui-font-card-title);
  color: #111827;
}
.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.meta label {
  display: block;
  font-size: var(--ui-font-label);
  font-weight: var(--ui-weight-semibold);
  color: #6b7280;
  text-transform: uppercase;
}
.meta span {
  font-size: var(--ui-font-caption);
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
  font-size: var(--ui-font-label);
  font-weight: var(--ui-weight-semibold);
  color: #6b7280;
  text-transform: uppercase;
}
.remarks p {
  margin: 4px 0 0;
  color: #475569;
  font-size: var(--ui-font-small);
  white-space: pre-wrap;
}
.field-label {
  display: block;
  font-size: var(--ui-font-label);
  font-weight: var(--ui-weight-semibold);
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
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-bold);
  color: #3730a3;
}

.flagged-section-remark {
  margin: 4px 0 0;
  font-size: var(--ui-font-small);
  line-height: var(--ui-line-body);
  color: #475569;
  white-space: pre-wrap;
}
input[type='date'],
textarea {
  width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  font-size: var(--ui-field-font);
}
textarea {
  min-height: 90px;
  resize: vertical;
}
.hint {
  color: #6b7280;
  font-size: var(--ui-font-caption);
  margin: 8px 0 0;
}
.btn {
  display: inline-block;
  padding: 10px 16px;
  border-radius: 8px;
  border: none;
  font-weight: var(--ui-weight-semibold);
  font-size: var(--ui-control-font-md);
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.success {
  color: #16a34a;
  font-weight: var(--ui-weight-semibold);
}
.error {
  color: #dc2626;
}
.muted {
  color: #6b7280;
}
</style>
