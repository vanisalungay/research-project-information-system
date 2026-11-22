<template>
  <div class="adm-overlay" @click.self="requestClose" @keydown.esc="requestClose">
    <!-- ================= DEAN ACCOUNT CREATION =================
         The same component and the same creation call as every other account, with the
         compact Dean-specific presentation used by Dean Role Management: fixed header,
         fixed footer, and one scrollable body. -->
    <div
      v-if="deanMode"
      ref="deanModal"
      class="modal dean-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dean-create-title"
    >
      <header class="dean-header">
        <span class="dean-header-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 19a6 6 0 0 0-12 0" />
            <circle cx="9" cy="7" r="4" />
            <line x1="19" y1="8" x2="19" y2="14" />
            <line x1="16" y1="11" x2="22" y2="11" />
          </svg>
        </span>
        <div class="dean-heading">
          <h2 id="dean-create-title">
            {{ isReplacement ? 'Create Dean Account' : 'Create user account' }}
          </h2>
          <p v-if="isReplacement">
            Create the incoming Dean. The assigned college is fixed to the sitting Dean's college,
            the sitting Dean's pending endorsement requests will be transferred to this new Dean,
            and the previous appointment will be automatically closed as history (REVOKED).
          </p>
          <p v-else>Add an official account and choose what this person can access.</p>
        </div>
        <button
          type="button"
          class="dean-close"
          aria-label="Close create user account modal"
          :disabled="isLoading"
          @click="requestClose"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </header>

      <form v-if="!created" id="dean-create-form" class="dean-form" @submit.prevent="createUser">
        <div class="dean-body">
          <div v-if="errorMessage" class="error-banner dean-error" role="alert">
            <svg class="error-icon" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
            </svg>
            <span>{{ errorMessage }}</span>
          </div>

          <div class="dean-field">
            <label for="deanFullName">Full name (First Name Last Name)</label>
            <input
              id="deanFullName"
              ref="deanNameInput"
              v-model="user.name"
              placeholder="Juan Dela Cruz"
              autocomplete="name"
              :disabled="isLoading"
              required
              @blur="user.name = formatPersonName(user.name)"
            />
            <p class="dean-hint">First Name Last Name - for example, Juan Dela Cruz.</p>
          </div>

          <div class="dean-row">
            <div class="dean-field">
              <label for="deanRole">Role</label>
              <input
                id="deanRole"
                class="dean-fixed-value"
                :value="deanRoleLabel"
                readonly
                aria-readonly="true"
                tabindex="0"
              />
            </div>
            <div class="dean-field">
              <label for="deanDesignation">Designation / title</label>
              <input
                id="deanDesignation"
                v-model="user.designation"
                placeholder="Dean"
                :disabled="isLoading"
                required
              />
            </div>
          </div>

          <div class="dean-field">
            <label for="deanCollege">Assigned college</label>
            <!-- Replacement: the college comes from the appointment being replaced and is
                 read-only here, so the incoming Dean cannot be appointed elsewhere. -->
            <input
              v-if="lockCollege"
              id="deanCollege"
              class="dean-fixed-value"
              :value="user.assignedCollege || 'Institution-wide (no college)'"
              readonly
              aria-readonly="true"
              tabindex="0"
            />
            <select
              v-else
              id="deanCollege"
              v-model="user.assignedCollege"
              :disabled="isLoading"
              required
            >
              <option value="" disabled>Select the college this Dean endorses for</option>
              <option v-for="college in colleges" :key="college" :value="college">
                {{ college }}
              </option>
            </select>
            <p v-if="lockCollege" class="dean-hint">
              Fixed to the college of the Dean being replaced and cannot be changed here.
            </p>
          </div>

          <section class="created-for-you" aria-labelledby="createdForYouTitle">
            <div class="created-for-you-heading">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 3l1.9 4.9L19 10l-5.1 2.1L12 17l-1.9-4.9L5 10l5.1-2.1L12 3z" />
                <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z" />
              </svg>
              <span id="createdForYouTitle">Created for you</span>
            </div>

            <div class="created-field">
              <span class="created-label">Email address</span>
              <div class="credential-preview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <polyline points="3 7 12 13 21 7" />
                </svg>
                <span :class="{ 'credential-muted': !generatedEmail }">
                  {{ generatedEmail || 'Enter a full name to preview the address' }}
                </span>
              </div>
              <p class="dean-hint">
                Built from the name. If this person already has an account, that account is
                reused - the Dean role is added without a duplicate address or password.
              </p>
            </div>

            <div class="created-field">
              <span class="created-label">Temporary password</span>
              <div class="credential-preview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <rect x="4" y="10" width="16" height="10" rx="2" />
                  <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                </svg>
                <span class="credential-muted">Generated securely when you create the account</span>
              </div>
              <p class="dean-hint">You will see it once, right after the account is created.</p>
            </div>
          </section>
        </div>

        <footer class="dean-footer">
          <button type="button" class="dean-cancel" :disabled="isLoading" @click="requestClose">
            Cancel
          </button>
          <button
            type="submit"
            class="dean-create"
            :disabled="isLoading || !canCreateDean"
          >
            <span v-if="isLoading" class="spinner" aria-hidden="true"></span>
            <span>{{ isLoading ? 'Creating...' : 'Create account' }}</span>
            <svg v-if="!isLoading" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="4" y1="12" x2="19" y2="12" />
              <polyline points="13 6 19 12 13 18" />
            </svg>
          </button>
        </footer>
      </form>

      <div v-else class="dean-created-wrap">
        <div class="dean-body">
          <div class="credentials dean-credentials">
            <h3 class="credentials-title">
              {{ created.reused ? 'Existing account reused' : 'Account created' }}
            </h3>
            <p class="credentials-note">
              <template v-if="created.reused">
                This person already had an account with this institutional address. The Dean
                role was added to it; the address and the existing password are unchanged, so
                no new credentials are issued.
              </template>
              <template v-else>
                Give these credentials to the account holder. The temporary password must be
                changed at first login - it stops working as soon as it is.
              </template>
            </p>

            <div class="cred-row">
              <span class="cred-label">Full Name</span>
              <span class="cred-value">{{ created.name }}</span>
            </div>
            <div class="cred-row">
              <span class="cred-label">Email</span>
              <span class="cred-value">{{ created.email }}</span>
              <button type="button" class="copy-btn btn-view" @click="copy(created.email, 'email')">Copy</button>
            </div>
            <div v-if="!created.reused" class="cred-row">
              <span class="cred-label">Temporary Password</span>
              <span class="cred-value cred-password">{{ created.temporaryPassword }}</span>
              <button type="button" class="copy-btn btn-view" @click="copy(created.temporaryPassword, 'password')">Copy</button>
            </div>

            <button v-if="!created.reused" type="button" class="copy-both btn-primary" @click="copyBoth">Copy both credentials</button>
            <p v-if="copyMessage" class="copy-message">{{ copyMessage }}</p>
          </div>
        </div>

        <footer class="dean-footer">
          <button type="button" class="dean-cancel" @click="requestClose">Close</button>
          <button v-if="!created.reused" type="button" class="dean-create" @click="copyBoth">
            Copy credentials
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="11" height="11" rx="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
        </footer>
      </div>
    </div>

    <!-- ================= STANDARD ACCOUNT CREATION =================
         The compact administrator dialog shared with Reset password: same header,
         information panel, fields and footer language (see assets/adminModal.css). -->
    <div
      v-else
      ref="standardModal"
      class="adm-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-account-title"
      tabindex="-1"
    >
      <header class="adm-header">
        <span class="adm-header-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 19a6 6 0 0 0-12 0" />
            <circle cx="9" cy="7" r="4" />
            <line x1="19" y1="8" x2="19" y2="14" />
            <line x1="16" y1="11" x2="22" y2="11" />
          </svg>
        </span>
        <div class="adm-heading">
          <h2 id="create-account-title">Create user account</h2>
          <p>Add an official account and choose what this person can access.</p>
        </div>
        <button
          type="button"
          class="adm-close"
          aria-label="Close create user account modal"
          :disabled="isLoading"
          @click="requestClose"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </header>

      <form v-if="!created" id="create-account-form" class="adm-form" @submit.prevent="createUser">
        <div class="adm-body">
          <div v-if="errorMessage" class="adm-error" role="alert">
            <svg viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
            </svg>
            <span>{{ errorMessage }}</span>
          </div>

          <div class="adm-field">
            <label for="name">Full name</label>
            <input
              id="name"
              v-model="user.name"
              class="adm-input"
              placeholder="Juan Dela Cruz"
              autocomplete="name"
              :disabled="isLoading"
              required
              @blur="user.name = formatPersonName(user.name)"
            />
            <p class="adm-hint">First name and last name only. Example: Juan Dela Cruz</p>
          </div>

          <div class="adm-field">
            <label for="role">User role</label>
            <select
              id="role"
              v-model="user.role"
              class="adm-select"
              :disabled="isLoading || lockRole"
              required
            >
              <option value="" disabled>Select a role</option>
              <option value="RPS_STAFF">RPS STAFF (Technical review &amp; support)</option>
              <option value="RPS_ADMIN">RPS ADMIN (System administrator)</option>
              <option value="DEAN">Dean (Dean / Department Chair)</option>
              <option value="OVCAF">OVCAF (Vice Chancellor for Administration &amp; Finance)</option>
              <option value="OVCRIGE">OVCRIGE (Vice Chancellor for Research)</option>
              <option value="REC">REC (Research Evaluation Committee)</option>
              <option value="OC">OC (Office of the Chancellor)</option>
            </select>
          </div>

          <!-- Dean-specific fields, shown only for the Dean role. -->
          <template v-if="isDean">
            <div class="adm-field">
              <label for="designation">Designation / title</label>
              <input
                id="designation"
                v-model="user.designation"
                class="adm-input"
                placeholder="e.g. Dean, Department Chair"
                :disabled="isLoading"
                required
              />
            </div>

            <div class="adm-field">
              <label for="assignedCollege">Assigned college</label>
              <select
                id="assignedCollege"
                v-model="user.assignedCollege"
                class="adm-select"
                :disabled="isLoading"
                required
              >
                <option value="" disabled>Select the college this Dean endorses for</option>
                <option v-for="college in colleges" :key="college" :value="college">
                  {{ college }}
                </option>
              </select>
            </div>
          </template>

          <!-- Everything in this panel is produced by the backend, not typed here. -->
          <section class="adm-panel" aria-labelledby="createdForYouTitle">
            <h3 id="createdForYouTitle" class="adm-panel-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 3l1.9 4.9L19 10l-5.1 2.1L12 17l-1.9-4.9L5 10l5.1-2.1L12 3z" />
                <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z" />
              </svg>
              <span>Created for you</span>
            </h3>

            <div class="adm-panel-field">
              <label for="email">Email address</label>
              <div class="adm-preview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <polyline points="3 7 12 13 21 7" />
                </svg>
                <input
                  id="email"
                  :value="generatedEmail"
                  class="adm-preview-input"
                  readonly
                  :placeholder="'Enter a full name to preview the address'"
                  aria-label="Generated institutional email address"
                />
              </div>
              <p class="adm-hint">
                Built from the name. If the address is taken, a number is added before the domain.
              </p>
            </div>

            <div class="adm-panel-field">
              <label for="tempPassword">Temporary password</label>
              <div class="adm-preview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <rect x="4" y="10" width="16" height="10" rx="2" />
                  <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                </svg>
                <input
                  id="tempPassword"
                  class="adm-preview-input adm-preview-muted"
                  value="Generated securely when you create the account"
                  readonly
                  aria-label="Temporary password generation status"
                />
              </div>
              <p class="adm-hint">You will see it once, right after the account is created.</p>
            </div>
          </section>
        </div>

        <footer class="adm-footer">
          <button
            type="button"
            class="adm-btn adm-btn--secondary"
            :disabled="isLoading"
            @click="requestClose"
          >
            Cancel
          </button>
          <button type="submit" class="adm-btn adm-btn--primary" :disabled="isLoading">
            <span v-if="isLoading" class="adm-spinner" aria-hidden="true"></span>
            <span>{{ isLoading ? 'Creating…' : 'Create account' }}</span>
            <svg v-if="!isLoading" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="4" y1="12" x2="19" y2="12" />
              <polyline points="13 6 19 12 13 18" />
            </svg>
          </button>
        </footer>
      </form>

      <!-- The credentials, shown only after a successful creation. -->
      <template v-else>
        <div class="adm-body adm-created">
          <h3 class="adm-created-title">
            {{ created.reused ? 'Existing account reused' : 'Account created' }}
          </h3>
          <p class="adm-created-note">
            <template v-if="created.reused">
              This person already had an account with this institutional address. The Dean
              role was added to it; the address and the existing password are unchanged.
            </template>
            <template v-else>
              Give these credentials to the account holder. The temporary password must be
              changed at first login - it stops working as soon as it is.
            </template>
          </p>

          <div class="adm-credential-row">
            <span class="adm-credential-label">Full name</span>
            <span class="adm-credential-value">{{ created.name }}</span>
          </div>
          <div class="adm-credential-row">
            <span class="adm-credential-label">Email</span>
            <span class="adm-credential-value">{{ created.email }}</span>
            <button type="button" class="copy-btn btn-view" @click="copy(created.email, 'email')">Copy</button>
          </div>
          <div v-if="!created.reused" class="adm-credential-row">
            <span class="adm-credential-label">Temporary password</span>
            <span class="adm-credential-value cred-password">{{ created.temporaryPassword }}</span>
            <button type="button" class="copy-btn btn-view" @click="copy(created.temporaryPassword, 'password')">Copy</button>
          </div>
          <p v-if="copyMessage" class="adm-copy-message">{{ copyMessage }}</p>
        </div>

        <footer class="adm-footer">
          <button type="button" class="adm-btn adm-btn--secondary" @click="requestClose">Close</button>
          <button v-if="!created.reused" type="button" class="adm-btn adm-btn--primary" @click="copyBoth">
            Copy credentials
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="11" height="11" rx="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import '@/assets/adminModal.css'
import api from '@/utils/api'
import { UserRole } from '@/stores/userData'
import { institutionalEmail } from '@/utils/institutionalEmail'
import { formatPersonName } from '@/utils/personName'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'

const emit = defineEmits(['close', 'created'])

/**
 * Optional: a page that creates one kind of account can open this form already on that
 * role and fix it, so the administrator cannot accidentally create something else. The
 * User Accounts page passes nothing and keeps the standard behaviour.
 */
const props = defineProps({
  initialRole: { type: String, default: null },
  lockRole: { type: Boolean, default: false },
  initialDesignation: { type: String, default: '' },
  initialCollege: { type: String, default: '' },
  // Dean Role Management: the office is fixed to the Dean role, so the "User Role Authority"
  // control selects the college the Dean is appointed to instead of the system role. The role
  // itself still comes from `initial-role` (DEAN) and stays locked, so this never changes the
  // account's authorization.
  collegeAuthority: { type: Boolean, default: false },
  // Replace Dean (Dean Role Management -> Manage Dean): the college is the appointment's
  // college, shown read-only, and the replacement is completed by the backend when the
  // account is created. When the appointment is institution-wide the field stays blank.
  lockCollege: { type: Boolean, default: false },
  replacementAssignmentId: { type: [Number, String], default: null },
})

// The official colleges, from the one place the whole application reads them
// (the Create Proposal form and the Dean's scope selector use the same list).
const colleges = RPIS_COLLEGES

/** What the administrator types: the person's name, their role, and the Dean's own fields. */
const user = ref({
  name: '',
  // College-authority mode only exists for the Dean appointment, so the role is fixed
  // there even if a caller forgets initial-role. The standard form still starts empty.
  role: props.initialRole || (props.collegeAuthority ? UserRole.DEAN : ''),
  // Dean-specific fields. They are only sent when the Dean role is selected.
  designation: props.initialDesignation,
  assignedCollege: props.initialCollege || ''
})

const isLoading = ref(false)
const errorMessage = ref('')

/** The credentials returned once by a successful creation, or null before one. */
const created = ref(null)
const copyMessage = ref('')

/** A preview of the naming rule; the backend derives the authoritative address on create. */
const generatedEmail = computed(() => institutionalEmail(user.value.name))

/** True while the Dean role is selected - the only role with extra fields. */
const isDean = computed(() => user.value.role === UserRole.DEAN)

/**
 * Dean Role Management opens the form in college-authority mode with the role locked to
 * Dean. Only that entry point gets the compact Dean presentation; the User Accounts form
 * keeps its existing layout and its free role choice.
 */
const deanMode = computed(() => props.collegeAuthority)

/**
 * True when this form creates the incoming Dean of an existing appointment (Replace Dean).
 * The account creation and the succession are one backend call, so the college is read-only
 * and comes from the appointment.
 */
const isReplacement = computed(
  () => props.replacementAssignmentId !== null
    && props.replacementAssignmentId !== undefined
    && props.replacementAssignmentId !== '',
)

/** The fixed role label. The value sent is still `user.role` (DEAN). */
const deanRoleLabel = computed(() =>
  user.value.role === UserRole.DEAN ? 'Dean' : (user.value.role || 'Dean'),
)

/** The Dean footer button is enabled only when every required field is present. */
const canCreateDean = computed(() =>
  Boolean(user.value.name.trim()) &&
  Boolean(user.value.role) &&
  Boolean(user.value.designation && user.value.designation.trim()) &&
  // A replacement inherits the appointment's college, which may be institution-wide.
  (props.lockCollege || Boolean(user.value.assignedCollege)) &&
  Boolean(generatedEmail.value),
)

const deanNameInput = ref(null)
const deanModal = ref(null)
const standardModal = ref(null)
let previouslyFocused = null
let bodyOverflowBefore = ''

function requestClose() {
  if (isLoading.value) return
  emit('close')
}

/** The dialog currently on screen (Dean or standard), for focus management. */
function activeModal() {
  return deanMode.value ? deanModal.value : standardModal.value
}

/**
 * Escape closes the modal; Tab stays inside it, so keyboard users cannot land on the
 * page behind the overlay. This applies to both the Dean and the standard form.
 */
function onModalKeydown(event) {
  if (event?.key === 'Escape') {
    requestClose()
    return
  }
  const modal = activeModal()
  if (event?.key !== 'Tab' || !modal) return

  const focusable = [...modal.querySelectorAll(
    'button:not([disabled]), input:not([disabled]), select:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  )].filter((element) => element.offsetParent !== null)
  if (focusable.length === 0) return

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
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
  window.addEventListener('keydown', onModalKeydown)
  nextTick(() => {
    if (deanMode.value) {
      deanNameInput.value?.focus()
    } else {
      const firstField = standardModal.value?.querySelector('#name')
        || standardModal.value?.querySelector('button, input, select')
      firstField?.focus()
    }
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onModalKeydown)
  document.body.style.overflow = bodyOverflowBefore
  // Return focus to the trigger when the modal closes.
  if (previouslyFocused && typeof previouslyFocused.focus === 'function') {
    previouslyFocused.focus()
  }
})

const createUser = async () => {
  // A second submit while the first is in flight would create two accounts.
  if (isLoading.value) return

  // Format before validating/saving: the address preview above and the stored name both
  // follow from this value.
  user.value.name = formatPersonName(user.value.name)

  if (!user.value.name || !user.value.name.trim()) {
    errorMessage.value = 'The full name is required.'
    return
  }

  // The Create User Account form asks for a plain First Name Last Name entry. The old
  // structured order ("Dela Cruz, Juan, M., Jr.") is no longer required or accepted here:
  // there is no middle initial or suffix, and the surname comes last, not first.
  if (user.value.name.includes(',')) {
    errorMessage.value =
      'Enter the first name and last name only, without commas - for example, Juan Dela Cruz.'
    return
  }
  if (user.value.name.trim().split(/\s+/).filter(Boolean).length < 2) {
    errorMessage.value =
      'Enter both the first name and the last name - for example, Juan Dela Cruz.'
    return
  }

  // The role is chosen, never defaulted: an account created with the wrong authority is
  // harder to undo than a second click. The backend validates it again.
  if (!user.value.role) {
    errorMessage.value = 'Select the user role this account is created with.'
    return
  }

  // A Dean signs the official endorsement letter with their designation and only reaches the
  // endorsement requests of one college, so both are required here. The backend validates them
  // again (DeanAccountService), so this is only the first of the two checks.
  if (isDean.value && !user.value.designation.trim()) {
    errorMessage.value = "The Dean's designation / title is required."
    return
  }

  if (isDean.value && !props.lockCollege && !user.value.assignedCollege) {
    errorMessage.value = 'Select the college this Dean is authorized to endorse for.'
    return
  }

  try {
    isLoading.value = true
    errorMessage.value = ''

    // One endpoint for every role. The backend recognises the Dean role and routes it to the
    // Dean provisioning logic (its own validation, a hashed password and an audit entry),
    // which only an RPS administrator is allowed to reach. The address and the temporary
    // password are generated by the backend for every role - the browser never chooses them.
    let response
    if (isReplacement.value) {
      // One call creates the account AND completes the succession: the sitting appointment
      // is closed as history and this account becomes the Dean of the same college. The
      // college is sent so the server can confirm it matches the appointment.
      response = await api.post('/api/users/deans/replacement', {
        assignmentId: Number(props.replacementAssignmentId),
        name: user.value.name.trim(),
        designation: user.value.designation.trim(),
        assignedCollege: user.value.assignedCollege || '',
      })
    } else {
      const payload = {
        name: user.value.name.trim(),
        role: user.value.role
      }

      if (isDean.value) {
        payload.designation = user.value.designation.trim()
        payload.assignedCollege = user.value.assignedCollege
      }

      response = await api.post('/api/users/admin/create', payload)
    }
    const body = response.data || {}

    // Shown once, here. The backend never returns these values again, so the modal stays open
    // for the administrator to read and copy them.
    created.value = {
      name: body.user?.name || user.value.name.trim(),
      email: body.email || '',
      temporaryPassword: body.temporaryPassword || '',
      // The backend reused an existing account: there is no new password to show.
      reused: Boolean(body.accountReused)
    }

    emit('created', {
      replacement: isReplacement.value,
      reused: created.value.reused,
      email: created.value.email
    })
  } catch (error) {
    console.error(error)
    // The Dean endpoints answer with { error: "reason" }; the general one may answer with
    // the reason as plain text. Both are shown as the readable reason.
    const data = error.response?.data
    errorMessage.value =
      (data && typeof data === 'object' ? data.error : data)
      || 'Failed to create user account. Please try again.'
  } finally {
    isLoading.value = false
  }
}

/** Copies one credential, or both, so the administrator can pass them on as they are. */
const copy = async (value, which) => {
  try {
    await navigator.clipboard.writeText(value)
    copyMessage.value = which === 'email' ? 'Email copied.' : 'Temporary password copied.'
  } catch (err) {
    console.error(err)
    copyMessage.value = 'Copy failed - select the value and copy it manually.'
  }
}

const copyBoth = async () => {
  const text = `Email: ${created.value.email}\nTemporary password: ${created.value.temporaryPassword}`
  try {
    await navigator.clipboard.writeText(text)
    copyMessage.value = 'Both credentials copied.'
  } catch (err) {
    console.error(err)
    copyMessage.value = 'Copy failed - select the values and copy them manually.'
  }
}
</script>

<style scoped>
.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  color: #991b1b;
  font-size: 0.8125rem;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { 
    opacity: 0; 
    transform: translateY(12px); 
  }
  to { 
    opacity: 1; 
    transform: translateY(0); 
  }
}
/* ===== Generated credentials ===== */
.credentials {
  margin-top: 8px;
  padding: 16px;
  background: #f5f5fc;
  border: 1px solid #e4e4f2;
  border-radius: 12px;
}

.credentials-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1c1a3c;
}

.credentials-note {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.5;
  color: #5b5b7a;
}

.cred-row {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e0e0f0;
}

.cred-row:last-of-type {
  border-bottom: none;
}

.cred-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #6b6b8c;
}

.cred-value {
  font-size: 13px;
  font-weight: 600;
  color: #1c1a3c;
  overflow-wrap: anywhere;
}

.cred-password {
  font-family: monospace;
  letter-spacing: 0.02em;
}

/* Layout only; the action colours come from assets/actions.css. */
.copy-btn {
  padding: 5px 12px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.copy-both {
  margin-top: 14px;
  width: 100%;
  padding: 10px 16px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}

.copy-message {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--status-approved-text);
}

/* ===== Dean account creation =================================================
   Compact administrative form: a fixed header and footer around one scrollable
   body, matching the approved reference proportions (roughly 456px wide and up
   to 90% of the viewport tall). Only this mode is restyled; the standard account
   form above keeps its own layout. */
.dean-modal {
  display: flex;
  flex-direction: column;
  width: 456px;
  max-width: calc(100vw - 24px);
  max-height: 90vh;
  border-radius: 17px;
  border: 1px solid #e6e8f0;
  overflow: hidden;
  background: #ffffff;
  box-shadow:
    0 24px 48px -16px rgba(20, 24, 50, 0.32),
    0 8px 18px -10px rgba(20, 24, 50, 0.2);
}

.dean-header {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  flex: 0 0 auto;
  padding: 15px 16px 13px;
  border-bottom: 1px solid #eceef5;
  background: #ffffff;
}

.dean-header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border-radius: 9px;
  background: #262342;
  color: var(--rpis-accent, #ffd000);
}

.dean-header-icon svg {
  width: 17px;
  height: 17px;
}

.dean-heading {
  min-width: 0;
  flex: 1;
}

.dean-heading h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.25;
  color: #1c1a3c;
}

.dean-heading p {
  margin: 3px 0 0;
  font-size: 11px;
  line-height: 1.4;
  color: #777d91;
}

.dean-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  margin-left: 2px;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #8a8fa3;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.dean-close svg {
  width: 16px;
  height: 16px;
}

.dean-close:hover:not(:disabled) {
  background: #f1f2f8;
  color: #262342;
}

.dean-close:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.dean-form,
.dean-created-wrap {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.dean-body {
  display: flex;
  flex-direction: column;
  gap: 13px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 15px 16px 16px;
}

.dean-body::-webkit-scrollbar {
  width: 7px;
}

.dean-body::-webkit-scrollbar-thumb {
  background: #d7dbe8;
  border-radius: 999px;
}

.dean-body::-webkit-scrollbar-track {
  background: transparent;
}

.dean-error {
  margin-bottom: 0;
  padding: 8px 10px;
  border-radius: 7px;
  font-size: 11.5px;
}

.dean-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.dean-field label,
.created-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #3b3d55;
  text-align: left;
}

.dean-field input,
.dean-field select {
  width: 100%;
  height: 38px;
  padding: 0 11px;
  border: 1px solid #dfe3ee;
  border-radius: 8px;
  background: #ffffff;
  color: #1c1a3c;
  font-family: inherit;
  font-size: 12.5px;
}

.dean-field input::placeholder {
  color: #a0a5b6;
}

.dean-field select {
  appearance: none;
  -webkit-appearance: none;
  padding-right: 31px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23777d91' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 11px center;
  cursor: pointer;
}

.dean-field input:focus,
.dean-field select:focus {
  outline: none;
  border-color: #315bff;
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.12);
}

.dean-field input:disabled,
.dean-field select:disabled {
  background-color: #f7f8fc;
  color: #8a8fa3;
}

.dean-fixed-value {
  background: #f7f8fc !important;
  color: #3b3d55 !important;
  cursor: default;
}

.dean-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.dean-hint {
  margin: 0;
  font-size: 10px;
  line-height: 1.4;
  color: #81869a;
  overflow-wrap: anywhere;
}

/* ===== Generated credentials preview ===== */
.created-for-you {
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin-top: 1px;
  padding: 11px 12px 12px;
  border: 1px solid #e2e7f3;
  border-radius: 12px;
  background: #f4f6fc;
}

.created-for-you-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #2b3567;
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.created-for-you-heading svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #3f6fe5;
}

.created-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.credential-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 35px;
  padding: 8px 10px;
  border: 1px solid #e0e5f1;
  border-radius: 8px;
  background: #ffffff;
  color: #262342;
  font-size: 11.5px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.credential-preview svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #6c7695;
}

.credential-muted {
  color: #8a8fa3;
}

/* ===== Fixed footer ===== */
.dean-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  flex: 0 0 auto;
  padding: 11px 16px 15px;
  border-top: 1px solid #eceef5;
  background: #ffffff;
}

.dean-cancel,
.dean-create {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.dean-cancel {
  border: 1px solid #dfe3ee;
  background: #ffffff;
  color: #2f3248;
}

.dean-cancel:hover:not(:disabled) {
  background: #f6f7fb;
  border-color: #cdd3e3;
}

.dean-create {
  border: 1px solid #d8e0f0;
  background: #e7ecf6;
  color: #262342;
}

.dean-create svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
}

.dean-create:hover:not(:disabled) {
  background: #dbe4f3;
  border-color: #c8d4e9;
}

.dean-create:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.dean-cancel:focus-visible,
.dean-create:focus-visible,
.dean-close:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.dean-create .spinner {
  border-color: rgba(38, 35, 66, 0.24);
  border-top-color: #262342;
}

.dean-credentials {
  margin-top: 0;
}

@media (max-width: 520px) {
  .dean-modal {
    width: calc(100vw - 24px);
    max-height: 94vh;
    border-radius: 16px;
  }

  .dean-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .dean-header {
    padding: 13px 14px 12px;
  }

  .dean-body {
    padding: 13px 14px 15px;
  }

  .dean-footer {
    padding: 10px 14px 13px;
  }
}

</style>
