<template>
  <div class="auth-page">
    <!-- Left: the shared, dark-navy MSU-Naawan branding panel. -->
    <AuthBrandingPanel />

    <!-- Right: the sign-in form. -->
    <main class="auth-right">
      <div class="auth-form auth-form--signin">
        <header class="auth-header">
          <h2 class="auth-heading">Welcome back</h2>
          <p class="auth-subtitle">Choose your role, then sign in to open your dashboard.</p>
        </header>

        <!-- Role Selection Cards -->
        <div class="auth-roles" role="group" aria-label="Select your role">
          <button
            v-for="role in roles"
            :key="role.name"
            type="button"
            class="auth-role"
            :class="{ 'is-active': selectedRole === role.value }"
            :aria-pressed="selectedRole === role.value"
            @click="selectedRole = role.value"
          >
            <span class="auth-role__icon" aria-hidden="true">
              <svg v-if="role.value === UserRole.PROPONENT" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2">
                <path
                  d="M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 009 11V5a2 2 0 00-2-2H4a2 2 0 00-2 2v6a13.91 13.91 0 003.44 9.16l.054.09m3.44-2.04A13.916 13.916 0 009 11M9 11h6" />
                <path
                  d="M12 11c0 3.517 1.009 6.799 2.753 9.571M12 11V5a2 2 0 012-2h3a2 2 0 012 2v6c0 3.14-.82 6.09-2.258 8.641"
                  stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <svg v-else-if="role.value === UserRole.RPS_STAFF" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 00-3-3.87" />
                <path d="M16 3.13a4 4 0 010 7.75" />
              </svg>
              <svg v-else-if="role.value === UserRole.OVCRIGE" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <svg v-else-if="role.value === UserRole.REC" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <svg v-else-if="role.value === UserRole.OVCAF" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16M12 11v.01" />
              </svg>
            </span>
            <span class="auth-role__text">
              <span class="auth-role__name">{{ role.name }}</span>
              <span class="auth-role__desc">{{ role.desc }}</span>
            </span>
            <span v-if="selectedRole === role.value" class="auth-role__check" aria-hidden="true">
              <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.6"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="m4 10.5 4 4 8-9" />
              </svg>
            </span>
          </button>
        </div>

        <!-- Error Message -->
        <div v-if="errorMessage" class="auth-error" role="alert">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Inputs Form -->
        <form class="auth-form__fields" @submit.prevent="handleLogin">
          <div class="auth-field">
            <label for="email" class="auth-label">Email</label>
            <input
              id="email"
              class="auth-input"
              type="email"
              v-model="email"
              placeholder="you@msunaawan.edu.ph"
              required
              autocomplete="username"
              @blur="email = normalizeEmailInput(email)"
            />
          </div>

          <div class="auth-field">
            <label for="password" class="auth-label">Password</label>
            <PasswordInput
              id="password"
              v-model="password"
              class="auth-input auth-input--password"
              placeholder="••••••••"
              autocomplete="current-password"
              required
            />
          </div>

          <div class="auth-meta">
            <label class="auth-remember" for="remember">
              <input type="checkbox" id="remember" />
              <span>Remember me</span>
            </label>
            <a href="/register" class="auth-forgot">Forgot password?</a>
          </div>

          <button class="auth-submit" type="submit" :disabled="submitting" :aria-busy="submitting">
            <span v-if="submitting" class="auth-spinner" aria-hidden="true"></span>
            <span>{{ submitting ? 'Signing in…' : 'Sign In' }}</span>
            <svg v-if="!submitting" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>

          <p class="auth-switch">
            Don't have an account?
            <a href="/create-account" class="auth-link">Sign up</a>
          </p>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import '@/assets/auth.css'
import AuthBrandingPanel from '@/components/AuthBrandingPanel.vue'
import PasswordInput from '@/components/PasswordInput.vue'
import { UserRole, useUserDataStore } from '@/stores/userData'
import { normalizeEmailInput } from '@/utils/institutionalEmail'

const userStore = useUserDataStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const errorMessage = ref('')
// A UI state only: the submit handler and the store call are unchanged, but the button
// cannot send a second sign-in request while the first one is in flight.
const submitting = ref(false)

const roles = [
  { name: 'Proponent', desc: 'Faculty Submitter', value: UserRole.PROPONENT, next: '/proponent-dashboard' },
  // Dean / Department Chair: an independent role, not part of RPS. The value must be
  // UserRole.DEAN - it is the role the account is stored with, and UserService.login
  // authenticates only when the role sent by this form matches it exactly.
  { name: 'Dean', desc: 'College Endorsement', value: UserRole.DEAN, next: '/dean-dashboard' },
  { name: 'ORPS', desc: 'Technical & Admin', value: UserRole.RPS_STAFF, next: '/proponent-dashboard' },
  { name: 'OVCRIGE', desc: 'Research VC', value: UserRole.OVCRIGE, next: '/proponent-dashboard' },
  { name: 'REC', desc: 'Evaluation Committee', value: UserRole.REC, next: '/proponent-dashboard' },
  { name: 'OVCAF', desc: 'Finance VC', value: UserRole.OVCAF, next: '/proponent-dashboard' },
  { name: 'Chancellor (OC)', desc: 'Chancellor Office', value: UserRole.OC, next: '/proponent-dashboard' },
]

const selectedRole = ref(UserRole.RPS_STAFF)

const handleLogin = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    errorMessage.value = ''

    const isLoggedIn = await userStore.login(
      email.value,
      password.value,
      selectedRole.value
    )

    if (isLoggedIn) {
      // An account created by an administrator holds a temporary password: it goes straight to
      // the change-password page and cannot reach its dashboard until it is replaced.
      if (userStore.user?.mustChangePassword) {
        router.push('/change-password')
      } else {
        router.push('/home')
      }
    } else {
      errorMessage.value = 'Invalid email, password, or role.'
    }
  } catch (err) {
    errorMessage.value = err.message || 'Invalid email, password, or role.'
  } finally {
    submitting.value = false
  }
}

const handleGoogleLogin = async (response) => {
  try {
    errorMessage.value = ''

    const isLoggedIn = await userStore.googleLogin(
      response.credential,
      selectedRole.value
    )

    if (isLoggedIn) {
      if (userStore.user?.mustChangePassword) {
        router.push('/change-password')
      } else {
        router.push('/home')
      }
    } else {
      errorMessage.value = 'Google Login failed.'
    }
  } catch (err) {
    errorMessage.value = err.message || 'Google Login failed.'
  }
}
</script>
