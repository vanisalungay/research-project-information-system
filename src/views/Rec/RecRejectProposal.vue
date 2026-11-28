<template>
  <div class="reject-wrapper">
    <!-- BACK BUTTON -->
    <BackButton label="Back to Submission" to="/assigned-proposals" />

    <!-- HEADER -->
    <div class="page-header">
      <h2>Reject Proposal</h2>
      <span class="badge-danger status-badge is-rejected">⚠️ Permanent Action</span>
    </div>

    <p class="subtitle">Provide clear justification for proposal rejection</p>

    <div class="layout-grid">
      <!-- LEFT SECTION -->
      <div class="left-col">
       <!-- PROPOSAL INFORMATION -->
<div class="card">
  <h3 class="card-title">Proposal Information</h3>

  <div class="info-grid">
    <div class="info-box">
      <label>Title</label>
      <p>{{ proposal.title || '-' }}</p>
    </div>

    <div class="info-box">
      <label>Proponent</label>
      <p>{{ proposal.proponent || '-' }}</p>
    </div>

    <div class="info-box">
      <label>Type</label>
      <p>{{ proposal.type || '-' }}</p>
    </div>

    <div class="info-box">
      <label>Program ID</label>
      <p>{{ proposal.programId || '-' }}</p>
    </div>

    <div class="info-box">
      <label>Category</label>
      <p>{{ proposal.category || '-' }}</p>
    </div>
  </div>
</div>

        <!-- REJECTION DETAILS -->
        <div class="card">
          <h3 class="card-title">Rejection Details</h3>

          <div class="form-group">
            <label for="rejectReason">Primary Reason for Rejection</label>
            <input
              id="rejectReason"
              v-model="reason"
              type="text"
              placeholder="Enter primary reason"
              required
            />
          </div>

          <div class="form-group">
            <label for="rejectExplanation">Detailed Explanation</label>
            <textarea
              id="rejectExplanation"
              v-model="explanation"
              rows="5"
              placeholder="Provide a clear and structured explanation for the rejection decision. Be specific and concise."
              required
            ></textarea>
          </div>

          <!-- WARNING BOX -->
          <div class="warning-box">
            ⚠️ This rejection is submitted to ORPS (RPS) for review. ORPS reviews the reason
            recorded here and forwards the decision to the proponent.
          </div>
        </div>

      </div>

      <!-- RIGHT SECTION -->
      <div class="right-col">
        <div class="side-card">
          <h4 class="side-title">Rejection Confirmation</h4>

          <p>This action will return the proposal to <strong>ORPS</strong> as a rejection.</p>

          <ul class="side-list">
            <li>ORPS reviews the recorded rejection reason</li>
            <li>ORPS forwards the decision to the proponent</li>
            <li>Decision cannot be undone</li>
          </ul>

          <button class="reject-btn btn-reject" @click="rejectProposal">Reject Proposal</button>

          <div class="note-box">
            Ensure the rejection details are properly filled before submitting.
          </div>
        </div>
      </div>
    </div>
    <!-- ERROR STATE -->
    <div v-if="error" class="error-state">
      <p>{{ error }}</p>
      <BackButton label="Back to Submission" to="/assigned-proposals" />
    </div>

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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import BackButton from '@/components/BackButton.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const router = useRouter()
const route = useRoute()
const { dialogState, showAlert, showConfirm } = useDialog()

const reason = ref('')
const explanation = ref('')
const loading = ref(false)
const error = ref('')

// Proposal data
const proposal = ref({
  id: null,
  title: '',
  proponent: '',
  type: '',
  programId: '',
  category: ''
})

// Fetch proposal data
onMounted(async () => {
  const proposalId = route.params.id || route.query.id
  if (proposalId) {
    try {
      loading.value = true
      const res = await api.get(`/api/proposals/${proposalId}`)
      const data = res.data
      proposal.value = {
        id: data.id,
        title: data.projectTitle || data.title || '',
        proponent: data.projectLeader || data.proponent?.name || '',
        type: data.researchType || '',
        programId: data.programTitle || '',
        category: data.programTitle || ''
      }
    } catch (err) {
      console.error(err)
      error.value = 'Failed to load proposal details.'
    } finally {
      loading.value = false
    }
  }
})

// Rejection is a workflow action: the reason is required before the confirmation, and
// nothing is sent to the backend until the reviewer confirms.
const rejectProposal = async () => {
  if (loading.value) return
  if (!reason.value.trim()) {
    await showAlert('Enter the primary reason for rejecting this proposal.', {
      type: 'warning',
      title: 'Reason Required',
    })
    return
  }
  if (!explanation.value.trim()) {
    await showAlert('Provide a detailed explanation for the rejection decision.', {
      type: 'warning',
      title: 'Explanation Required',
    })
    return
  }

  const remarks = `${reason.value.trim()}\n\n${explanation.value.trim()}`
  const confirmed = await showConfirm(
    'This rejection will be submitted to ORPS for review and forwarded to the proponent. Continue?',
    {
      title: 'Reject Proposal',
      type: 'danger',
      confirmText: 'Reject Proposal',
      cancelText: 'Cancel',
    },
  )
  if (!confirmed) return

  try {
    loading.value = true
    await api.put(`/api/proposals/${proposal.value.id}/reject`, { remarks })
    await showAlert(
      'The rejection was submitted to ORPS. ORPS will review it and forward the decision to the proponent.',
      { type: 'success', title: 'Rejection Submitted' },
    )
    router.push('/rec-dash')
  } catch (err) {
    console.error(err)
    const message =
      err?.response?.data?.error ||
      err?.response?.data?.message ||
      'Failed to reject the proposal. Please try again.'
    await showAlert(message, { type: 'error', title: 'Rejection Failed' })
  } finally {
    loading.value = false
  }
}
</script>

<style>
.reject-wrapper {
  padding: 25px 30px;
  background: #f4f5fa;
}

.reject-wrapper > .back-button {
  margin-bottom: 10px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: var(--ui-font-page-title);
  font-weight: var(--ui-weight-bold);
}

/* Geometry only; the rejected tone comes from assets/status.css. */
.badge-danger {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: var(--ui-font-caption);
}

.subtitle {
  margin-top: 6px;
  font-size: var(--ui-font-small);
  color: #777;
}

.layout-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 25px;
  margin-top: 20px;
}

.card {
  background: white;
  padding: 22px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-title {
  margin-bottom: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.info-box label {
  font-size: var(--ui-font-label);
  color: #777;
}

.info-box p {
  font-size: var(--ui-font-body);
  margin-top: 4px;
}

.form-group {
  margin-bottom: 18px;
}

input,
textarea {
  width: 100%;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  margin-top: 6px;
  font-size: var(--ui-field-font);
}

/* Warning Box */
.warning-box {
  background: #ffecec;
  border-left: 4px solid #d32f2f;
  padding: 12px;
  border-radius: 6px;
  font-size: var(--ui-font-small);
  color: #a62828;
}

.summary-header {
  margin-bottom: 4px;
}

.summary-note {
  font-size: var(--ui-font-caption);
  color: #555;
  margin-bottom: 12px;
}

.side-card {
  background: white;
  padding: 22px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.side-title {
  margin-top: 0;
}

.side-list {
  padding-left: 18px;
  margin: 14px 0;
  font-size: var(--ui-font-body);
}

/* Layout only; the reject/cancel colours come from assets/actions.css. */
.reject-btn {
  width: 100%;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
  font-size: var(--ui-control-font-md);
}

.cancel-btn {
  width: 100%;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
  font-size: var(--ui-control-font-md);
}

.note-box {
  background: #fff8d8;
  padding: 12px;
  border-radius: 6px;
  margin-top: 15px;
  font-size: var(--ui-font-caption);
  color: #7a6a00;
}
</style>
