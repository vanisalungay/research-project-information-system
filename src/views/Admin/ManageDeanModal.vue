<template>
  <div class="md-overlay" @click.self="$emit('close')">
    <div
      ref="modal"
      class="md-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="manage-dean-title"
      tabindex="-1"
    >
      <!-- Header: icon, title, subtitle, close. -->
      <header class="md-header">
        <span class="md-header-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 10 12 5 2 10l10 5 10-5z" />
            <path d="M6 12v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5" />
            <path d="M22 10v6" />
          </svg>
        </span>
        <div class="md-heading">
          <h3 id="manage-dean-title">Manage Dean</h3>
          <p>Manage the account and assignment of this college dean.</p>
        </div>
        <button
          type="button"
          class="md-close"
          aria-label="Close manage dean"
          @click="$emit('close')"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </header>

      <!-- Body: one scrollable area, so the actions below stay reachable. -->
      <div class="md-body" :aria-busy="loading">
        <div v-if="loadError" class="md-note" role="status">{{ loadError }}</div>

        <section class="md-profile" aria-label="Dean profile">
          <span class="md-avatar" aria-hidden="true">{{ initials }}</span>
          <div class="md-identity">
            <h4>{{ deanName || '—' }}</h4>
            <span class="md-status" :class="statusClass">
              <span class="md-status-dot" aria-hidden="true"></span>
              {{ statusLabel }}
            </span>
          </div>
        </section>

        <section class="md-card" aria-label="Dean account information">
          <div class="md-info-row">
            <span class="md-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="5" width="18" height="14" rx="2" />
                <polyline points="3 7 12 13 21 7" />
              </svg>
            </span>
            <div class="md-info">
              <span class="md-label">Email</span>
              <span class="md-value">{{ email || '—' }}</span>
            </div>
          </div>

          <div class="md-divider" role="presentation"></div>

          <div class="md-info-row">
            <span class="md-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 21h18" />
                <path d="M5 21V9l7-5 7 5v12" />
                <path d="M9 21v-6h6v6" />
                <path d="M9 11h.01M15 11h.01" />
              </svg>
            </span>
            <div class="md-info">
              <span class="md-label">Assigned College / Department</span>
              <span class="md-value">{{ collegeName }}</span>
              <span v-if="collegeCode" class="md-code-badge">{{ collegeCode }}</span>
            </div>
          </div>

          <div class="md-divider" role="presentation"></div>

          <div class="md-info-row">
            <span class="md-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="7" width="18" height="13" rx="2" />
                <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                <path d="M3 12h18" />
              </svg>
            </span>
            <div class="md-info">
              <span class="md-label">Designation</span>
              <span class="md-value">{{ designation || '—' }}</span>
            </div>
          </div>

          <div class="md-divider" role="presentation"></div>

          <div class="md-info-row">
            <span class="md-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3 4.5 6v5c0 4.5 3.2 7.8 7.5 10 4.3-2.2 7.5-5.5 7.5-10V6L12 3z" />
                <path d="m8.5 12 2.2 2.2 4.8-4.8" />
              </svg>
            </span>
            <div class="md-info">
              <span class="md-label">Role</span>
              <span class="md-value">{{ roleLabel }}</span>
            </div>
          </div>

          <div class="md-divider" role="presentation"></div>

          <div class="md-info-row">
            <span class="md-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
            </span>
            <div class="md-info">
              <span class="md-label">Date Assigned</span>
              <span class="md-value">{{ dateAssigned }}</span>
            </div>
          </div>
        </section>

      </div>

      <!-- The one management action. Replacing the Dean transfers the pending requests and
           closes the outgoing appointment automatically, in one transaction. -->
      <footer class="md-footer">
        <button
          type="button"
          class="md-action md-replace btn-primary"
          :disabled="!canManage"
          @click="$emit('replace')"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M19 8v6" />
            <path d="M22 11h-6" />
          </svg>
          Replace Dean
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
/**
 * Manage Dean (Dean Role Management).
 *
 * One dialog for a sitting Dean: the account and appointment details and the single
 * management action, Replace Dean. The details are the real appointment row plus the
 * account's live record (role), never sample data; the account is only read here, and
 * nothing is edited in place.
 *
 * Replace Dean emits up to the page, which opens the Create Dean Account form locked to
 * this appointment's college. The backend transfers the pending endorsement requests and
 * closes the outgoing appointment automatically in one transaction - there is no separate
 * Revoke action.
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '@/utils/api'
import { deanAssignmentStatusLabel } from '@/utils/statusCatalog.js'
import { collegeDisplayParts, initialsFromName } from '@/utils/proposalCardDisplay'

const props = defineProps({
  /** The ACTIVE appointment row the Manage Dean button belongs to. */
  assignment: { type: Object, required: true },
})

const emit = defineEmits(['close', 'replace'])

const ROLE_LABELS = {
  DEAN: 'Dean',
  PROPONENT: 'Proponent',
  RPS_ADMIN: 'RPS Administrator',
  RPS_STAFF: 'RPS Staff',
  OVCRIGE: 'OVCRIGE',
  OVCAF: 'OVCAF',
  REC: 'REC',
  OC: 'OC',
}

// The live account record, loaded on open for the current role and any value changed
// since the appointment was made. The appointment row itself is the fallback.
const account = ref(null)
const loading = ref(false)
const loadError = ref('')

const modal = ref(null)
let previouslyFocused = null
let bodyOverflowBefore = ''
let requestId = 0

const deanName = computed(
  () => props.assignment.name || account.value?.name || '',
)
const initials = computed(() => initialsFromName(deanName.value))
const email = computed(
  () => props.assignment.email || account.value?.email || '',
)
const designation = computed(
  () => props.assignment.designation || account.value?.designation || '',
)
const roleLabel = computed(() => {
  const roles = Array.isArray(account.value?.roles) && account.value.roles.length
    ? account.value.roles
    : [account.value?.role].filter(Boolean)
  return roles.map((role) => ROLE_LABELS[role] || role).join(', ') || 'Dean'
})
const statusLabel = computed(
  () => deanAssignmentStatusLabel(props.assignment.status) || 'Active',
)
const statusClass = computed(() =>
  String(props.assignment.status).toUpperCase() === 'REVOKED'
    ? 'md-status--revoked'
    : 'md-status--active',
)
const college = computed(() => {
  const raw = props.assignment.assignedCollege || account.value?.assignedCollege || ''
  return collegeDisplayParts(raw)
})
const collegeName = computed(() => college.value.name || 'Institution-wide (no college)')
const collegeCode = computed(() => college.value.abbreviation || '')
const dateAssigned = computed(() => formatDate(props.assignment.dateAssigned))
const canManage = computed(() => String(props.assignment.status).toUpperCase() === 'ACTIVE')

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString()
}

async function loadAccount() {
  if (!props.assignment.userId) return
  const request = ++requestId
  loading.value = true
  try {
    const response = await api.get(`/api/users/${props.assignment.userId}`)
    if (request === requestId) account.value = response.data || null
  } catch (error) {
    console.error('Could not load the Dean account', error)
    if (request === requestId) {
      loadError.value =
        'Some current account details could not be loaded. Showing the saved appointment record.'
    }
  } finally {
    if (request === requestId) loading.value = false
  }
}

/** Escape closes; Tab stays inside the dialog while it is open. */
function onKeydown(event) {
  if (event?.key === 'Escape') {
    emit('close')
    return
  }
  if (event?.key !== 'Tab' || !modal.value) return

  const focusable = [...modal.value.querySelectorAll(
    'button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  )].filter((element) => element.offsetParent !== null)
  if (focusable.length === 0) return

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey &&
      (document.activeElement === first || document.activeElement === modal.value)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  previouslyFocused = document.activeElement
  bodyOverflowBefore = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  window.addEventListener('keydown', onKeydown)
  nextTick(() => modal.value?.focus())
  loadAccount()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = bodyOverflowBefore
  nextTick(() => previouslyFocused?.focus?.())
})
</script>

<style scoped>
.md-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.52);
  backdrop-filter: blur(2px);
}

.md-modal {
  display: flex;
  flex-direction: column;
  width: 468px;
  max-width: calc(100vw - 24px);
  max-height: 90vh;
  overflow: hidden;
  border: 1px solid #e5e8f1;
  border-radius: 17px;
  background: #ffffff;
  box-shadow:
    0 24px 48px -16px rgba(20, 24, 50, 0.32),
    0 8px 18px -10px rgba(20, 24, 50, 0.18);
}

.md-modal:focus {
  outline: none;
}

/* Header */
.md-header {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  flex: 0 0 auto;
  padding: 16px 18px 14px;
  border-bottom: 1px solid #eceef5;
}

.md-header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 35px;
  height: 35px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: #262342;
  color: var(--rpis-accent, #ffd000);
}

.md-header-icon svg {
  width: 19px;
  height: 19px;
}

.md-heading {
  flex: 1;
  min-width: 0;
}

.md-heading h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.25;
  color: #1c1a3c;
}

.md-heading p {
  margin: 3px 0 0;
  font-size: 11.5px;
  line-height: 1.4;
  color: #777d91;
}

.md-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #8a8fa3;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.md-close svg {
  width: 16px;
  height: 16px;
}

.md-close:hover {
  background: #f2f3f9;
  color: #262342;
}

.md-close:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

/* Body */
.md-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 16px 18px 18px;
}

.md-body::-webkit-scrollbar {
  width: 7px;
}

.md-body::-webkit-scrollbar-thumb {
  background: #d7dbe8;
  border-radius: 999px;
}

.md-body::-webkit-scrollbar-track {
  background: transparent;
}

.md-note {
  margin: 0;
  padding: 8px 10px;
  border: 1px solid #f2dfb8;
  border-radius: 8px;
  background: #fffbeb;
  color: #8a5a0a;
  font-size: 11px;
  line-height: 1.4;
}

/* Profile summary */
.md-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.md-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #ffd000;
  color: #262342;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.md-identity {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.md-identity h4 {
  margin: 0;
  font-size: 16.5px;
  font-weight: 700;
  line-height: 1.3;
  color: #1c1a3c;
  overflow-wrap: anywhere;
}

.md-status {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  white-space: nowrap;
}

.md-status-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: currentColor;
}

.md-status--active {
  background: #e8f7ee;
  color: #16794a;
}

.md-status--revoked {
  background: #fdf0ef;
  color: #b42318;
}

/* Information card */
.md-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 14px;
  border: 1px solid #e4e9f4;
  border-radius: 13px;
  background: #f6f8fc;
}

.md-info-row {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  min-width: 0;
}

.md-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 9px;
  background: #e9eefb;
  color: #315bff;
}

.md-icon svg {
  width: 16px;
  height: 16px;
}

.md-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.md-label {
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.35;
  color: #7b8298;
}

.md-value {
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.45;
  color: #1c1a3c;
  overflow-wrap: anywhere;
}

.md-code-badge {
  align-self: flex-start;
  margin-top: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #e6edfc;
  color: #31539b;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.md-divider {
  height: 1px;
  margin: 12px 0;
  background: #e3e8f2;
}

/* Actions */
.md-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  flex: 0 0 auto;
  padding: 12px 18px 16px;
  border-top: 1px solid #eceef5;
  background: #ffffff;
}

.md-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border-radius: 9px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.md-action svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
}

.md-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.md-action:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

@media (max-width: 480px) {
  .md-overlay {
    padding: 12px;
  }

  .md-modal {
    width: calc(100vw - 24px);
    max-height: 92vh;
    border-radius: 16px;
  }

  .md-header {
    padding: 14px 14px 12px;
  }

  .md-body {
    padding: 14px;
  }

  .md-footer {
    grid-template-columns: minmax(0, 1fr);
    padding: 10px 14px 13px;
  }
}
</style>
