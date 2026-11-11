<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>Create User Account</h2>
        <button class="close-x" @click="$emit('close')" :disabled="isLoading">&times;</button>
      </div>

      <div class="modal-body">
        <p class="subtitle">Register a new official account with specific role-based permissions.</p>

        <div v-if="errorMessage" class="error-banner">
          <svg class="error-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <form v-if="!created" @submit.prevent="createUser" class="form-group">
          <div class="field-group">
            <label for="name">Full Name</label>
            <input 
              id="name" 
              v-model="user.name" 
              placeholder="e.g. Dr. Jane Doe" 
              :disabled="isLoading"
              required 
            />
          </div>

          <!-- Read-only: the address is derived from the name, never typed. What is shown is a
               preview of the naming rule; the backend derives the authoritative address when
               the account is created and numbers it if it is already taken. -->
          <div class="field-group">
            <label for="email">Email Address</label>
            <input id="email" :value="generatedEmail" readonly />
            <p class="field-hint">
              Generated from the name as first+middle.last@msunawaan.edu.ph - no spaces, all
              lower case. A number is added before the domain if the address is already taken.
            </p>
          </div>

          <div class="field-group">
            <label for="tempPassword">Temporary Password</label>
            <input id="tempPassword" :value="created ? created.temporaryPassword : ''"
              placeholder="Generated securely when you create the account" readonly />
            <p class="field-hint">
              Generated on the server and shown once, here, after the account is created.
            </p>
          </div>
          <div class="field-group">
            <label :for="collegeAuthority ? 'collegeAuthority' : 'role'">User Role Authority</label>
            <!-- Dean Role Management: the role is fixed to DEAN (below, locked), so this
                 dropdown selects the college the Dean is appointed to. It binds to the same
                 `assignedCollege` field the Assigned College selector uses, so the college is
                 stored once in the field the backend DTO expects - not a second source of
                 truth. Reuses the one college list (RPIS_COLLEGES); the system role and its
                 authorization mapping are unchanged. -->
            <select
              v-if="collegeAuthority"
              id="collegeAuthority"
              v-model="user.assignedCollege"
              :disabled="isLoading"
              required
            >
              <option value="" disabled>Select College</option>
              <option v-for="college in colleges" :key="college" :value="college">
                {{ college }}
              </option>
            </select>
            <select v-else id="role" v-model="user.role" :disabled="isLoading || lockRole" required>
              <option value="" disabled>Select User Role</option>
              <option value="RPS_STAFF">RPS STAFF (Technical review & support)</option>
              <option value="RPS_ADMIN">RPS ADMIN (System administrator)</option>
              <option value="DEAN">Dean (Dean / Department Chair)</option>
              <option value="OVCAF">OVCAF (Vice Chancellor for Administration & Finance)</option>
              <option value="OVCRIGE">OVCRIGE (Vice Chancellor for Research)</option>
              <option value="REC">REC (Research Evaluation Committee)</option>
              <option value="OC">OC (Office of the Chancellor)</option>
            </select>
          </div>

          <!-- Dean-specific fields, shown only for the Dean role. The Dean is an
               ordinary account whose role carries an endorsement scope: it needs a
               designation for the letter it signs and the one college it endorses for.
               These are the only two extra fields, and they are sent only for this role. -->
          <template v-if="isDean">
            <div class="field-group">
              <label for="designation">Designation / Title</label>
              <input
                id="designation"
                v-model="user.designation"
                placeholder="e.g. Dean, Department Chair"
                :disabled="isLoading"
                required
              />
            </div>

            <div class="field-group">
              <label for="assignedCollege">Assigned College</label>
              <select
                id="assignedCollege"
                v-model="user.assignedCollege"
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

          <div class="buttons">
            <button 
              type="button" 
              class="cancel-btn btn-secondary"
              @click="$emit('close')"
              :disabled="isLoading"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              class="create-btn"
              :disabled="isLoading"
            >
              <span v-if="isLoading" class="spinner"></span>
              <span>{{ isLoading ? 'Creating...' : 'Create Account' }}</span>
            </button>
          </div>
        </form>

        <!-- The credentials, shown only after a successful creation. Nothing is shown while
             the form is still being filled in, and a failed attempt shows only the error. -->
        <div v-if="created" class="credentials">
          <h3 class="credentials-title">Account created</h3>
          <p class="credentials-note">
            Give these credentials to the account holder. The temporary password must be
            changed at first login - it stops working as soon as it is.
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
          <div class="cred-row">
            <span class="cred-label">Temporary Password</span>
            <span class="cred-value cred-password">{{ created.temporaryPassword }}</span>
            <button type="button" class="copy-btn btn-view" @click="copy(created.temporaryPassword, 'password')">Copy</button>
          </div>

          <button type="button" class="copy-both btn-primary" @click="copyBoth">Copy both credentials</button>
          <p v-if="copyMessage" class="copy-message">{{ copyMessage }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '@/utils/api'
import { UserRole } from '@/stores/userData'
import { institutionalEmail } from '@/utils/institutionalEmail'
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
})

// The official colleges, from the one place the whole application reads them
// (the Create Proposal form and the Dean's scope selector use the same list).
const colleges = RPIS_COLLEGES

/** What the administrator types: the person's name, their role, and the Dean's own fields. */
const user = ref({
  name: '',
  role: props.initialRole || '',
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

const createUser = async () => {
  // A second submit while the first is in flight would create two accounts.
  if (isLoading.value) return

  if (!user.value.name || !user.value.name.trim()) {
    errorMessage.value = 'The full name is required.'
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

  if (isDean.value && !user.value.assignedCollege) {
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
    const payload = {
      name: user.value.name.trim(),
      role: user.value.role
    }

    if (isDean.value) {
      payload.designation = user.value.designation.trim()
      payload.assignedCollege = user.value.assignedCollege
    }

    const response = await api.post('/api/users/admin/create', payload)
    const body = response.data || {}

    // Shown once, here. The backend never returns these values again, so the modal stays open
    // for the administrator to read and copy them.
    created.value = {
      name: body.user?.name || user.value.name.trim(),
      email: body.email || '',
      temporaryPassword: body.temporaryPassword || ''
    }

    emit('created')
  } catch (error) {
    console.error(error)
    errorMessage.value =
      error.response?.data || 'Failed to create user account. Please try again.'
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
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 14px;
  width: 440px;
  max-width: 90%;
  box-shadow: 
    0 20px 25px -5px rgba(0, 0, 0, 0.1), 
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
}

.modal-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0;
}

.close-x {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  color: var(--color-text-soft, #94a3b8);
  transition: color 0.15s ease;
  padding: 4px;
}

.close-x:hover {
  color: var(--color-heading, #1e293b);
}

.modal-body {
  padding: 24px;
}

.subtitle {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 20px 0;
  line-height: 1.4;
}

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

.form-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-group label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text, #475569);
  text-align: left;
}

input,
select {
  padding: 10px 14px;
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: all 0.2s ease;
  background: var(--color-background, #ffffff);
  color: var(--color-text, #1f2937);
  width: 100%;
}

input:focus,
select:focus {
  outline: none;
  border-color: #2452ff;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.15);
}

input:disabled,
select:disabled {
  background: var(--color-background-soft, #f1f5f9);
  cursor: not-allowed;
  opacity: 0.7;
}

.buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

button {
  padding: 10px 18px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

/* Layout only; the action colours come from assets/actions.css. */
.cancel-btn {
  border-radius: 8px;
}

.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
.field-hint {
  margin: 6px 0 0;
  font-size: 11.5px;
  line-height: 1.45;
  color: #6b7280;
}

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

</style>
