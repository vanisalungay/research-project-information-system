<template>
  <div class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <div class="header-left">
          <h2>Submit New Proposal</h2>
          <span class="header-badge">Step 1 of 4</span>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="close" aria-label="Close">✕</button>
        </div>
      </header>

      <!-- PROGRESS BAR (4 steps: this page is the new first step) -->
      <div class="progress-bar">
        <div class="progress-step active">
          <span class="step-number">1</span>
          <span class="step-label">Call for Proposal</span>
        </div>
        <div class="progress-line"></div>
        <!-- The remaining steps are only reachable through a selected call, so they
             are shown but not clickable here. -->
        <div class="progress-step">
          <span class="step-number">2</span>
          <span class="step-label">Proposal Details</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step">
          <span class="step-number">3</span>
          <span class="step-label">Certification</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step">
          <span class="step-number">4</span>
          <span class="step-label">Review &amp; Submit</span>
        </div>
      </div>

      <!-- BODY -->
      <div class="modal-body">
        <div class="call-head">
          <h3 class="call-title">{{ CALL_FOR_PROPOSAL_TITLE }}</h3>
          <p class="call-instruction">{{ CALL_FOR_PROPOSAL_INSTRUCTION }}</p>
        </div>

        <!-- LOADING -->
        <div v-if="loading" class="state-card">
          <span class="spinner" aria-hidden="true"></span>
          <p>Loading calls for proposals…</p>
        </div>

        <!-- LOAD FAILURE -->
        <div v-else-if="error" class="state-card state-error">
          <p>{{ error }}</p>
          <button class="btn-secondary" @click="loadCalls">Try again</button>
        </div>

        <!-- EMPTY STATE -->
        <div v-else-if="calls.length === 0" class="state-card">
          <p>{{ NO_AVAILABLE_CALLS_MESSAGE }}</p>
        </div>

        <!-- OPEN AND SCHEDULED CALLS -->
        <div v-else class="call-grid">
          <article
            v-for="call in calls"
            :key="call.id"
            class="call-card"
            :class="{ 'call-card--scheduled': isScheduled(call) }"
          >
            <div class="call-card-head">
              <span class="call-status" :class="statusClass(call)">
                <span class="status-dot" aria-hidden="true"></span>{{ statusOf(call) }}
              </span>
            </div>

            <h4 class="call-card-title">{{ call.name }}</h4>

            <dl class="call-meta">
              <!-- A scheduled call shows when it opens, so the proponent knows when
                   to come back. -->
              <div v-if="isScheduled(call)" class="call-meta-row">
                <dt>{{ CALL_OPENING_LABEL }}</dt>
                <dd class="call-opening">{{ callOpeningLabel(call) }}</dd>
              </div>
              <div class="call-meta-row">
                <dt>{{ CALL_DEADLINE_LABEL }}</dt>
                <dd class="call-deadline">{{ callDeadlineLabel(call) }}</dd>
              </div>
              <div class="call-meta-row">
                <dt>{{ CALL_WINDOW_LABEL }}</dt>
                <dd>{{ callWindowLabel(call) }}</dd>
              </div>
            </dl>

            <!-- Only the information the call actually carries is shown. -->
            <p v-if="call.additionalAnnouncement" class="call-description">
              {{ call.additionalAnnouncement }}
            </p>

            <!-- Enabled while the submission window is open; disabled (and explained
                 below) while the call is still scheduled. -->
            <button
              class="btn-primary call-select"
              :disabled="!isOpen(call)"
              @click="selectCall(call)"
            >
              {{ CALL_SELECTION_BUTTON_LABEL }}
            </button>
            <p v-if="isScheduled(call)" class="call-notice">
              {{ applicationsOpenOnLabel(call) }}
            </p>
          </article>
        </div>

        <p v-if="selectionError" class="selection-error">{{ selectionError }}</p>
      </div>

      <!-- FOOTER -->
      <footer class="modal-footer">
        <button class="btn-secondary" @click="close">Cancel</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import api from '@/utils/api'
import { announcementStatus } from '@/utils/announcementStatus'
import {
  AVAILABLE_CALLS_ENDPOINT,
  CALL_DEADLINE_LABEL,
  CALL_FOR_PROPOSAL_INSTRUCTION,
  CALL_FOR_PROPOSAL_TITLE,
  CALL_OPENING_LABEL,
  CALL_SELECTION_BUTTON_LABEL,
  CALL_STATUS_SCHEDULED,
  CALL_WINDOW_LABEL,
  NO_AVAILABLE_CALLS_MESSAGE,
  applicationsOpenOnLabel,
  callDeadlineLabel,
  callOpeningLabel,
  callWindowLabel,
  isCallListable,
  isCallOpen,
  isCallScheduled,
  selectableCall,
  sortCallsForDisplay,
} from '@/utils/callForProposal'

/**
 * Step 1 of the Create Proposal workflow: the Calls for Proposal a proponent can see.
 *
 * The list comes from the existing Proposal Announcement data
 * (GET /api/proposal-announcements/available), which the backend already narrows to
 * the published calls a proponent may act on: the ones that are open for submissions
 * right now and the ones that open later (scheduled). Drafts, closed, cancelled and
 * past-deadline calls are never returned. Nothing is hardcoded: the cards show the
 * saved title, opening date, deadline and submission period, and the status comes
 * from the same rule the rest of RPIS uses (src/utils/announcementStatus.js).
 *
 * A scheduled call is displayed with a disabled "Apply to This Call" button and the
 * date its applications open; only an open call can be selected, and the backend
 * re-validates the selected call when the proposal is created or submitted.
 */
const emit = defineEmits(['select', 'close'])

const loading = ref(false)
const error = ref('')
const selectionError = ref('')
const calls = ref([])

/** Draft / Scheduled / Open / Closed, from the saved status plus the saved dates. */
const statusOf = (call) => announcementStatus(call)

/** Badge modifier, so an upcoming call looks different from an open one. */
const statusClass = (call) =>
  announcementStatus(call) === CALL_STATUS_SCHEDULED
    ? 'call-status--scheduled'
    : 'call-status--open'

const isOpen = (call) => isCallOpen(call)
const isScheduled = (call) => isCallScheduled(call)

/**
 * Loads the open and upcoming calls. A call that is no longer listable (for example
 * the deadline passed while the page was open) is dropped instead of being shown as
 * selectable, and the order is the page order: open calls first, then the upcoming
 * ones by opening date.
 */
async function loadCalls() {
  loading.value = true
  error.value = ''
  selectionError.value = ''
  try {
    const res = await api.get(AVAILABLE_CALLS_ENDPOINT)
    const loaded = Array.isArray(res.data) ? res.data : []
    calls.value = sortCallsForDisplay(loaded.filter((call) => isCallListable(call)))
  } catch (err) {
    console.error('Failed to load the calls for proposals:', err)
    error.value = 'Unable to load the calls for proposals. Please try again.'
    calls.value = []
  } finally {
    loading.value = false
  }
}

/**
 * Hands the selected call to the workflow. The call is re-checked first, so a call
 * that is not open (still scheduled, closed in the meantime) can never be carried
 * into the proposal form.
 */
function selectCall(call) {
  const selected = selectableCall(call)
  if (!selected) {
    selectionError.value = isScheduled(call)
      ? `${applicationsOpenOnLabel(call)}`
      : 'This call for proposal is no longer open for submissions. Please select an open call.'
    loadCalls()
    return
  }
  selectionError.value = ''
  emit('select', selected)
}

function close() {
  emit('close')
}

// Escape closes the step, like the other dialogs in the application.
function onKeydown(event) {
  if (event.key === 'Escape') close()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  loadCalls()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ============================================================
   Step 1 - Call for Proposal
   The modal chrome (backdrop, container, header, progress bar, footer) mirrors the
   existing Create Proposal steps so the workflow looks like one form.
   ============================================================ */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

.modal-container {
  background: #f8fafc;
  width: 100%;
  max-width: 1100px;
  max-height: 92vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-badge {
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

/* ===== PROGRESS BAR ===== */
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: #94a3b8;
  transition: all 0.2s;
}

.progress-step.active .step-number {
  background: #4f46e5;
  color: #fff;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}

.progress-step.active .step-label {
  color: #1e293b;
  font-weight: 600;
}

.progress-line {
  width: 40px;
  height: 2px;
  background: #e2e8f0;
  margin: 0 12px;
}

/* ===== BODY ===== */
.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

.call-head {
  margin-bottom: 20px;
}

.call-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.call-instruction {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

/* ===== OPEN CALL CARDS ===== */
.call-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.call-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  transition:
    box-shadow 0.15s,
    border-color 0.15s;
}

.call-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08);
}

.call-card-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.call-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

/* An open call keeps the existing green badge. */
.call-status--open {
  background: #ecfdf5;
  color: #047857;
}

.call-status--open .status-dot {
  background: #10b981;
}

/* A scheduled (upcoming) call is clearly different: amber, with an outline dot. */
.call-status--scheduled {
  background: #fff7ed;
  color: #b45309;
}

.call-status--scheduled .status-dot {
  background: transparent;
  border: 2px solid #f59e0b;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-sizing: border-box;
}

.call-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.call-meta {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.call-meta-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.call-meta-row dt {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.call-meta-row dd {
  margin: 0;
  font-size: 13px;
  color: #1e293b;
}

.call-deadline {
  font-weight: 600;
}

.call-description {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  /* The saved announcement text can be long: keep the cards the same height. */
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.call-select {
  margin-top: auto;
  justify-content: center;
  width: 100%;
  font-size: 13px;
  padding: 10px 16px;
}

/* An upcoming call cannot be applied to yet: the button is visibly unavailable and
   the line under it says when applications open. */
.call-select:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.call-select:disabled:hover {
  background: #e2e8f0;
}

.call-notice {
  margin: 8px 0 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #b45309;
}

.call-opening {
  font-weight: 600;
}

/* An upcoming card keeps the same shape, with a quieter border. */
.call-card--scheduled {
  border-color: #fed7aa;
}

.selection-error {
  margin: 16px 0 0 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 500;
}

/* ===== LOADING / ERROR / EMPTY STATES ===== */
.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 24px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  text-align: center;
  font-size: 14px;
  line-height: 1.5;
  color: #64748b;
}

.state-card p {
  margin: 0;
  max-width: 520px;
}

.state-error {
  color: #b91c1c;
}

.spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: callStepSpin 0.8s linear infinite;
}

@keyframes callStepSpin {
  to {
    transform: rotate(360deg);
  }
}

/* ===== FOOTER ===== */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.btn-secondary {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #4f46e5;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary:hover {
  background: #4338ca;
}

/* ===== SMALL SCREENS =====
   The step indicator wraps, the cards become a single column and the actions keep
   the modal width (no horizontal overflow). */
@media (max-width: 760px) {
  .modal-backdrop {
    padding: 12px;
  }

  .modal-header,
  .progress-bar,
  .modal-footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .modal-body {
    padding: 18px 16px;
  }

  .step-label {
    font-size: 12px;
  }

  .progress-line {
    width: 16px;
    margin: 0 6px;
  }

  .call-grid {
    grid-template-columns: 1fr;
  }
}
</style>
