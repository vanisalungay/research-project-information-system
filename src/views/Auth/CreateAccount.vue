<template>
  <div class="auth-page">
    <!-- Left: the same MSU-Naawan branding panel the Sign In page uses. -->
    <AuthBrandingPanel />

    <!-- Right: the registration form. -->
    <main class="auth-right">
      <div class="auth-form auth-form--register">
        <BackButton label="Back to Login" to="/login" class="auth-back" />

        <header class="auth-header">
          <h2 class="auth-heading">Create an account</h2>
          <p class="auth-subtitle">
            Register as a Proponent to submit and manage research project proposals.
          </p>
        </header>

        <!-- Error Message -->
        <div v-if="errorMessage" class="auth-error" role="alert">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4M12 16h.01" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- FORM -->
        <form class="auth-form__fields" @submit.prevent="handleRegister">
          <div class="auth-field">
            <label for="name" class="auth-label">Full Name</label>
            <input
              id="name"
              class="auth-input"
              type="text"
              v-model="name"
              placeholder="Juan Dela Cruz"
              required
              @blur="name = formatPersonName(name)"
            />
            <FullNameHint />
          </div>

          <div class="auth-field">
            <label for="idNumber" class="auth-label">ID Number</label>
            <input
              id="idNumber"
              class="auth-input"
              type="text"
              v-model="idNumber"
              placeholder="Enter your ID number"
              required
            />
          </div>

          <div class="auth-field">
            <label for="department" class="auth-label">Department / Office</label>
            <select
              id="department"
              v-model="departmentOffice"
              class="auth-input auth-select"
              required
            >
              <option value="" disabled>Select your College / Office</option>
              <option v-for="college in colleges" :key="college" :value="college">
                {{ college }}
              </option>
            </select>
          </div>

          <div class="auth-field">
            <label for="email" class="auth-label">University Email Address</label>
            <input
              id="email"
              class="auth-input"
              type="email"
              :value="generatedEmail"
              readonly
              aria-readonly="true"
              aria-describedby="email-generated-note"
              placeholder="Enter your full name"
            />
            <p id="email-generated-note" class="auth-generated">
              <span v-if="emailChecking">Checking availability…</span>
              <span v-else>
                Automatically generated from your full name. This address cannot be edited.
              </span>
            </p>
          </div>

          <div class="auth-half">
            <div class="auth-field">
              <label for="password" class="auth-label">Password</label>
              <PasswordInput
                id="password"
                v-model="password"
                class="auth-input auth-input--password"
                placeholder="Create password"
                autocomplete="new-password"
                aria-describedby="password-hint password-requirements"
                required
              />
              <PasswordFieldHint id="password-hint" kind="new" />
            </div>

            <div class="auth-field">
              <label for="confirmPassword" class="auth-label">Confirm Password</label>
              <PasswordInput
                id="confirmPassword"
                v-model="confirmPassword"
                class="auth-input auth-input--password"
                placeholder="Confirm password"
                autocomplete="new-password"
                aria-describedby="confirmPassword-hint"
                required
              />
              <PasswordFieldHint id="confirmPassword-hint" kind="confirm" />
            </div>
          </div>

          <PasswordRequirements
            id="password-requirements"
            :password="password"
            :confirm="confirmPassword"
            :columns="2"
            show-progress
          />

          <!-- TERMS -->
          <div class="auth-terms">
            <input type="checkbox" id="agree" v-model="agree" class="terms-checkbox" />
            <label for="agree" class="terms-label">
              I have read and agree to the
              <span class="terms-link" @click.prevent="showTermsModal = true">
                Terms and Conditions
              </span>
              and
              <span class="terms-link" @click.prevent="showTermsModal = true">
                Privacy Policy
              </span>.
            </label>
          </div>

          <button class="auth-submit" type="submit" :disabled="!passwordReady">
            <span>Create account</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>

          <p class="auth-switch">
            Already have an account?
            <router-link to="/login" class="auth-link">Sign in</router-link>
          </p>
        </form>
      </div>
    </main>

    <!-- TERMS & CONDITIONS MODAL -->
    <Transition name="modal">
      <div v-if="showTermsModal" class="modal-overlay" @click.self="showTermsModal = false">
        <div class="modal-box">
          <button class="modal-close-btn" @click="showTermsModal = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6L6 18" />
              <path d="M6 6l12 12" />
            </svg>
          </button>

          <h2>TERMS AND CONDITIONS</h2>

          <div class="modal-content">
            <p>
              Welcome to the Research Project Information System of
              Mindanao State University at Naawan.
            </p>

            <h3>TERMS AND CONDITIONS</h3>

            <ol>
              <li>
                Only official <strong>@msunaawan.edu.ph</strong> email addresses
                may be used to register.
              </li>
              <li>
                You are responsible for keeping your account credentials secure.
              </li>
              <li>
                All information you provide must be accurate and truthful.
              </li>
              <li>
                You are responsible for all research proposals submitted through
                your account.
              </li>
              <li>
                Any misuse of the system may result in suspension or termination
                of your account.
              </li>
            </ol>

            <h3>PRIVACY POLICY</h3>

            <p>
              The system collects your name, university email, department, and
              other information solely for research proposal management.
            </p>

            <p>
              Your personal information will only be accessed by authorized
              university personnel and handled in accordance with the
              <strong>Data Privacy Act of 2012 (Republic Act No. 10173).</strong>
            </p>
          </div>

          <button class="close-btn btn-primary" @click="showTermsModal = false">
            I Understand
          </button>
        </div>
      </div>
    </Transition>

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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import '@/assets/auth.css'
import AuthBrandingPanel from '@/components/AuthBrandingPanel.vue'
import BackButton from '@/components/BackButton.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PasswordInput from '@/components/PasswordInput.vue'
import PasswordRequirements from '@/components/PasswordRequirements.vue'
import PasswordFieldHint from '@/components/PasswordFieldHint.vue'
import FullNameHint from '@/components/FullNameHint.vue'
import { useDialog } from '@/composables/useDialog'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'
import { institutionalEmail } from '@/utils/institutionalEmail'
import { firstLastNameError, formatPersonName } from '@/utils/personName'
import { passwordFormReady, passwordPolicyMessage } from '@/utils/passwordPolicy'

const { dialogState, showAlert, showConfirm } = useDialog()

const router = useRouter()

const name = ref('')
const idNumber = ref('')
const departmentOffice = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)
const showTermsModal = ref(false)
const errorMessage = ref('')

/** The one authoritative college/unit list the endorsement and proposal forms already use. */
const colleges = RPIS_COLLEGES

/**
 * The university address is not typed: it is derived from the full name with the same rule
 * the administrator's Create Account form uses (utils/institutionalEmail), then checked
 * against the existing accounts through the server so the duplicate suffix (2, 3, ...) is
 * exactly the address the registration will receive.
 */
const emailPreview = ref('')
const emailChecking = ref(false)
const localEmail = computed(() => institutionalEmail(name.value))
const generatedEmail = computed(() => emailPreview.value || localEmail.value)
let emailPreviewTimer = null
let emailPreviewSequence = 0

watch(name, (value) => {
  if (emailPreviewTimer !== null) {
    clearTimeout(emailPreviewTimer)
    emailPreviewTimer = null
  }
  emailPreview.value = ''
  emailChecking.value = false

  const trimmed = String(value || '').trim()
  const sequence = ++emailPreviewSequence
  if (!trimmed) return

  // Debounced: a name being typed produces one availability request, not one per key.
  emailPreviewTimer = setTimeout(async () => {
    emailPreviewTimer = null
    if (sequence !== emailPreviewSequence) return
    emailChecking.value = true
    try {
      const response = await api.get('/api/users/registration-email', {
        params: { name: trimmed },
      })
      if (sequence === emailPreviewSequence && response?.data?.email) {
        emailPreview.value = response.data.email
      }
    } catch {
      // The local rule remains on screen; the server still assigns the authoritative
      // address (including a duplicate suffix) when the registration is submitted.
    } finally {
      if (sequence === emailPreviewSequence) emailChecking.value = false
    }
  }, 350)
})

onBeforeUnmount(() => {
  if (emailPreviewTimer !== null) {
    clearTimeout(emailPreviewTimer)
    emailPreviewTimer = null
  }
  emailPreviewSequence += 1
})

/** The submit button stays disabled until the password and its confirmation are valid. */
const passwordReady = computed(() => passwordFormReady(password.value, confirmPassword.value))

const handleRegister = async () => {
  errorMessage.value = ''

  // Must agree first
  if (!agree.value) {
    errorMessage.value = 'Please read and agree to the Terms and Conditions and Privacy Policy.'
    return
  }

  // The same policy the backend enforces; the live checklist above explains what is missing.
  const policyMessage = passwordPolicyMessage(password.value)
  if (policyMessage) {
    errorMessage.value = policyMessage
    return
  }

  // Password check
  if (password.value !== confirmPassword.value) {
    errorMessage.value = 'Passwords do not match.'
    return
  }

  // Format once more in case the field was never blurred (e.g. the form was submitted
  // with the keyboard while the input still had focus).
  name.value = formatPersonName(name.value)
  const nameError = firstLastNameError(name.value)
  if (nameError) {
    errorMessage.value = nameError
    return
  }

  // The address is generated from the name and is never typed. The server derives the
  // authoritative final address again on submit, including any duplicate suffix.
  if (!generatedEmail.value) {
    errorMessage.value = 'Enter your full name so the university email can be generated.'
    return
  }

  const payload = {
    name: name.value,
    email: generatedEmail.value,
    password: password.value,
    role: 'PROPONENT',
    departmentOffice: departmentOffice.value.trim()
  }

  try {
    const response = await api.post('/api/users', payload)
    // The server is the authority on the address; echo the one it actually issued, so the
    // applicant sees the final value even if another registration took the previewed one.
    const createdEmail = response?.data?.email
    await showAlert(
      createdEmail
        ? `Account created successfully! Your university email is ${createdEmail}. You can now log in.`
        : 'Account created successfully! You can now log in.',
      { type: 'success', title: 'Account Created' },
    )
    router.push('/login')
  } catch (error) {
    // The axios error can carry the submitted form (including its password) in
    // `config.data`, so it is never logged. Only a fixed, value-free note is kept.
    console.error('Registration request failed.')

    if (error.response) {
      // The backend refused the registration (an invalid payload, a name that cannot
      // produce an address, ...). Its reason is the one shown to the user.
      const body = error.response.data
      errorMessage.value =
        (typeof body === 'string' && body) ||
        body?.message ||
        body?.error ||
        'Registration failed. Please try again.'
      return
    }

    // No response at all: the server could not be reached. The registration is NOT
    // saved anywhere else. It previously wrote the new account - including its
    // password - into a `offline_users` list in localStorage and then reported
    // "Account created successfully!", producing an account that existed only in
    // that one browser and could never be approved, found or signed into anywhere
    // else.
    errorMessage.value =
      'Cannot reach the RPIS server. Your account was not created. Please check your connection and try again.'
  }
}
</script>

<style scoped>
/* The split-screen layout, the branding panel, the fields and the buttons are shared
   with the Sign In page (assets/auth.css). Only the Terms and Conditions dialog is
   unique to this page. */

/* The shared Back control, rendered as the compact blue text link the auth page needs.
   Its routing/click behaviour is unchanged. */
.auth-form .auth-back {
  --bb-surface: transparent;
  --bb-border: transparent;
  --bb-text: var(--color-primary, #2452ff);
  height: auto;
  padding: 0;
  gap: 6px;
  border-radius: 0;
  font-size: var(--ui-font-small);
  font-weight: var(--ui-weight-semibold);
}

.auth-form .auth-back:hover {
  --bb-border: transparent;
  border-color: transparent;
  background: transparent;
}

.auth-form .auth-back :deep(.back-button__icon) {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  background: transparent;
  color: var(--color-primary, #2452ff);
}

.auth-form .auth-back :deep(.back-button__icon svg) {
  width: 14px;
  height: 14px;
}

.auth-form .auth-back:hover :deep(.back-button__icon) {
  background: transparent;
  color: var(--color-primary-hover, #1d40cc);
}

/* Keep the field hints readable at the enlarged registration type scale. */
.auth-form--register :deep(.full-name-hint),
.auth-form--register :deep(.pw-field-hint) {
  font-size: var(--ui-font-caption);
}

/* ========== MODAL ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-box {
  background: var(--color-card-background, #ffffff);
  width: 650px;
  max-width: 90%;
  max-height: 85vh;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow-y: auto;
}

.modal-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: var(--color-background-mute, #f1f5f9);
  color: var(--color-text, #475569);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.modal-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.modal-close-btn svg {
  width: 18px;
  height: 18px;
}

.modal-box h2 {
  margin-bottom: 20px;
  color: var(--color-heading, #1f1a3d);
  font-size: var(--ui-font-card-title);
  text-align: center;
}

.modal-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: var(--ui-line-body);
  color: var(--color-text, #444);
  text-align: justify;
  padding-right: 8px;
}

.modal-content h3 {
  margin-top: 24px;
  margin-bottom: 12px;
  color: var(--color-heading, #1f1a3d);
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
  text-align: center;
}

.modal-content p {
  margin-bottom: 14px;
  text-align: justify;
}

.modal-content li {
  margin-bottom: 10px;
  text-align: justify;
}

.modal-content ol {
  padding-left: 20px;
}

.close-btn {
  margin-top: 24px;
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: var(--ui-weight-semibold);
  font-size: var(--ui-icon-md);
  transition: all 0.2s ease;
}

/* ========== MODAL TRANSITION ========== */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-box,
.modal-leave-active .modal-box {
  transition: transform 0.2s ease;
}

.modal-enter-from .modal-box {
  transform: scale(0.95);
}

.modal-leave-to .modal-box {
  transform: scale(0.95);
}
</style>