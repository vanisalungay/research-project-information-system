<template>
  <div class="register-wrapper">
    <!-- LEFT PANEL -->
    <div class="left-panel">
      <div class="glow-effect"></div>
      <div class="branding-content">
        <img src="@/assets/images/logo.png" class="school-logo" alt="MSU Naawan Logo" />

        <h1 class="title">
          Research <span class="highlight">Project</span><br />
          Information<br />
          System
        </h1>

        <p class="school-name">
          RESEARCH AND PUBLICATION SERVICES<br />
          <span class="sub-school">Mindanao State University at Naawan</span>
        </p>
      </div>
    </div>

    <!-- RIGHT PANEL -->
    <div class="right-panel">
      <div class="right-content">
        <button class="back-btn" @click="router.push('/login')">
          <svg class="back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5" />
            <path d="M12 19l-7-7 7-7" />
          </svg>
          Back to Login
        </button>

        <div class="header-section">
          <h2 class="welcome">Create Proponent Account</h2>
          <p class="subtitle">
            Register as a Proponent to submit and manage research project proposals.
          </p>
        </div>

        <!-- Error Message -->
        <div v-if="errorMessage" class="error-banner">
          <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4M12 16h.01" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- FORM -->
        <form class="register-form" @submit.prevent="handleRegister">
          <div class="input-group">
            <label for="name">Name</label>
            <input
              id="name"
              type="text"
              v-model="name"
              placeholder="Enter your full name"
              required
            />
          </div>

          <div class="input-group">
            <label for="idNumber">ID Number</label>
            <input
              id="idNumber"
              type="text"
              v-model="idNumber"
              placeholder="Enter your ID number"
              required
            />
          </div>

          <div class="input-group">
            <label for="department">Department / Office</label>
            <input
              id="department"
              type="text"
              v-model="departmentOffice"
              placeholder="Enter your department or office"
              required
            />
          </div>

          <div class="input-group">
            <label for="email">University Email Address</label>
            <input
              id="email"
              type="email"
              v-model="email"
              placeholder="juan.delacruz@msunaawan.edu.ph"
              pattern="^[A-Za-z0-9._%+-]+@msunaawan\.edu\.ph$"
              title="Please use your MSU Naawan email."
              required
            />
          </div>

          <div class="half-inputs">
            <div class="input-group">
              <label for="password">Password</label>
              <input
                id="password"
                type="password"
                v-model="password"
                placeholder="Create a password"
                required
              />
            </div>

            <div class="input-group">
              <label for="confirmPassword">Confirm Password</label>
              <input
                id="confirmPassword"
                type="password"
                v-model="confirmPassword"
                placeholder="Confirm your password"
                required
              />
            </div>
          </div>

          <!-- TERMS -->
          <div class="terms">
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

          <button class="register-btn" type="submit">
            Create Proponent Account
          </button>

          <p class="signin-text">
            Already have an account? <router-link to="/login" class="signin-link">Sign in</router-link>
          </p>
        </form>
      </div>
    </div>

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

          <button class="close-btn" @click="showTermsModal = false">
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const router = useRouter()

const name = ref('')
const idNumber = ref('')
const departmentOffice = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)
const showTermsModal = ref(false)
const errorMessage = ref('')

const handleRegister = async () => {
  errorMessage.value = ''

  // Must agree first
  if (!agree.value) {
    errorMessage.value = 'Please read and agree to the Terms and Conditions and Privacy Policy.'
    return
  }

  // Password check
  if (password.value !== confirmPassword.value) {
    errorMessage.value = 'Passwords do not match.'
    return
  }

  // University email validation
  const emailRegex = /^[A-Za-z0-9._%+-]+@msunaawan\.edu\.ph$/i

  if (!emailRegex.test(email.value.trim())) {
    errorMessage.value = 'Only MSU Naawan email addresses (@msunaawan.edu.ph) are allowed.'
    return
  }

  const payload = {
    name: name.value.trim(),
    email: email.value.trim().toLowerCase(),
    password: password.value,
    role: 'PROPONENT',
    departmentOffice: departmentOffice.value.trim()
  }

  try {
    await api.post('/api/users', payload)

    await showAlert('Account created successfully! You can now log in.', { type: 'success', title: 'Account Created' })
    router.push('/login')
  } catch (error) {
    console.log(error)

    if (error.response?.status === 400) {
      errorMessage.value =
        error.response.data?.message || 'Registration failed. Email already exists.'
      return
    }

    console.warn('Backend unavailable. Saving locally...')

    const offlineUsers = JSON.parse(
      localStorage.getItem('offline_users') || '[]'
    )

    const exists = offlineUsers.some(
      user => user.email.toLowerCase() === email.value.toLowerCase()
    )

    if (exists) {
      errorMessage.value = 'Registration failed. Email already exists.'
      return
    }

    const newUser = {
      id: Date.now(),
      name: name.value.trim(),
      email: email.value.trim().toLowerCase(),
      password: password.value,
      role: 'PROPONENT',
      departmentOffice: departmentOffice.value.trim(),
      status: 'ACTIVE',
      dateRegistered: new Date().toISOString()
    }

    offlineUsers.push(newUser)
    localStorage.setItem('offline_users', JSON.stringify(offlineUsers))

    await showAlert('Account created successfully! You can now log in.', { type: 'success', title: 'Account Created' })
    router.push('/login')
  }
}
</script>

<style scoped>
.register-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--color-background, #f3f4f6);
  overflow: hidden;
}

/* ========== LEFT PANEL ========== */
.left-panel {
  position: relative;
  width: 40%;
  height: 100vh;
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
  padding: 64px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.glow-effect {
  position: absolute;
  top: -10%;
  left: -10%;
  width: 120%;
  height: 120%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 60%);
  pointer-events: none;
}

.branding-content {
  position: relative;
  z-index: 2;
  max-width: 440px;
  margin: 0 auto;
}

.school-logo {
  width: 110px;
  height: auto;
  margin-bottom: 32px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin: 0 0 28px 0;
  letter-spacing: -0.025em;
  text-align: left;
}

.title .highlight {
  color: #ffd803;
}

.school-name {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.6;
  opacity: 0.9;
  letter-spacing: 0.05em;
  margin: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  padding-top: 28px;
  text-align: left;
}

.sub-school {
  font-size: 0.75rem;
  font-weight: 400;
  opacity: 0.8;
  letter-spacing: 0;
}

/* ========== RIGHT PANEL ========== */
.right-panel {
  width: 60%;
  height: 100vh;
  padding: 64px 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow-y: auto;
}

.right-content {
  width: 100%;
  max-width: 540px;
}

.header-section {
  margin-bottom: 24px;
  text-align: left;
}

.welcome {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 0.9375rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

/* ========== BACK BUTTON ========== */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: #2452ff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 24px;
  padding: 6px 0;
  transition: color 0.2s ease;
}

.back-btn:hover {
  color: #1d40cc;
}

.back-icon {
  width: 18px;
  height: 18px;
}

/* ========== ERROR BANNER ========== */
.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  color: #991b1b;
  font-size: 0.8125rem;
  margin-bottom: 20px;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* ========== FORM ========== */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.input-group label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text, #475569);
}

.input-group input {
  padding: 12px 14px;
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: all 0.2s ease;
  background: var(--color-background, #ffffff);
  color: var(--color-text, #1f2937);
  width: 100%;
}

.input-group input:focus {
  outline: none;
  border-color: #2452ff;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.15);
}

.input-group input::placeholder {
  color: #94a3b8;
}

.half-inputs {
  display: flex;
  gap: 12px;
}

.half-inputs .input-group {
  flex: 1;
}

/* ========== TERMS ========== */
.terms {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 4px 0;
  font-size: 0.8125rem;
}

.terms-checkbox {
  width: 16px !important;
  height: 16px !important;
  margin-top: 3px !important;
  cursor: pointer;
  accent-color: #2452ff;
}

.terms-label {
  color: var(--color-text, #475569) !important;
  font-size: 0.8125rem !important;
  font-weight: 400 !important;
  line-height: 1.5;
  cursor: default;
}

.terms-link {
  color: #2452ff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.15s ease;
}

.terms-link:hover {
  color: #1d40cc;
}

/* ========== REGISTER BUTTON ========== */
.register-btn {
  background: #2452ff;
  color: white;
  padding: 13px;
  border: none;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  width: 100%;
  margin-top: 4px;
}

.register-btn:hover {
  background: #1d40cc;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(36, 82, 255, 0.3);
}

.register-btn:active {
  transform: translateY(0);
}

/* ========== SIGN IN LINK ========== */
.signin-text {
  margin: 12px 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  text-align: center;
}

.signin-link {
  color: #2452ff;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.15s ease;
}

.signin-link:hover {
  text-decoration: underline;
  color: #1d40cc;
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
  font-size: 1.25rem;
  text-align: center;
}

.modal-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.8;
  color: var(--color-text, #444);
  text-align: justify;
  padding-right: 8px;
}

.modal-content h3 {
  margin-top: 24px;
  margin-bottom: 12px;
  color: var(--color-heading, #1f1a3d);
  font-size: 1.125rem;
  font-weight: 700;
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
  background: #2452ff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9375rem;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #1d40cc;
  box-shadow: 0 4px 14px rgba(36, 82, 255, 0.3);
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

/* ========== RESPONSIVE ========== */
@media (max-width: 960px) {
  .register-wrapper {
    height: auto;
    min-height: 100vh;
    overflow-y: auto;
  }

  .left-panel {
    width: 100%;
    height: auto;
    padding: 40px;
    align-items: center;
  }

  .school-logo {
    margin-bottom: 16px;
  }

  .title {
    font-size: 1.875rem;
    text-align: center;
    margin-bottom: 16px;
  }

  .school-name {
    text-align: center;
    padding-top: 16px;
  }

  .right-panel {
    width: 100%;
    height: auto;
    padding: 40px 24px;
  }

  .half-inputs {
    flex-direction: column;
    gap: 16px;
  }
}

@media (max-width: 540px) {
  .left-panel {
    padding: 32px 24px;
  }

  .right-panel {
    padding: 32px 20px;
  }

  .welcome {
    font-size: 1.5rem;
  }

  .title {
    font-size: 1.5rem;
  }
}
</style>