<template>
  <!--
    The page an account lands on while it still holds an administrator-issued temporary
    password. It cannot be dismissed: the router guard keeps the account here, and the backend
    refuses every other API until the password is changed.
  -->
  <div class="cp-page">
    <div class="cp-card">
      <h1 class="cp-title">Change your temporary password</h1>
      <p class="cp-sub">
        This account was created with a temporary password. Choose a new one to continue - the
        temporary password stops working as soon as you save.
      </p>

      <p v-if="error" class="cp-error">{{ error }}</p>
      <p v-if="success" class="cp-success">{{ success }}</p>

      <form class="cp-form" @submit.prevent="submit">
        <div class="cp-field">
          <label for="current">Current password</label>
          <div class="cp-row">
            <input id="current" v-model="currentPassword" :type="reveal.current ? 'text' : 'password'"
              autocomplete="current-password" :disabled="submitting" />
            <button type="button" class="cp-toggle" @click="reveal.current = !reveal.current">
              {{ reveal.current ? 'Hide' : 'Show' }}
            </button>
          </div>
        </div>

        <div class="cp-field">
          <label for="next">New password</label>
          <div class="cp-row">
            <input id="next" v-model="newPassword" :type="reveal.next ? 'text' : 'password'"
              autocomplete="new-password" :disabled="submitting" />
            <button type="button" class="cp-toggle" @click="reveal.next = !reveal.next">
              {{ reveal.next ? 'Hide' : 'Show' }}
            </button>
          </div>
        </div>

        <div class="cp-field">
          <label for="confirm">Confirm new password</label>
          <div class="cp-row">
            <input id="confirm" v-model="confirmPassword" :type="reveal.confirm ? 'text' : 'password'"
              autocomplete="new-password" :disabled="submitting" />
            <button type="button" class="cp-toggle" @click="reveal.confirm = !reveal.confirm">
              {{ reveal.confirm ? 'Hide' : 'Show' }}
            </button>
          </div>
        </div>

        <p v-if="hint" class="cp-hint">{{ hint }}</p>

        <button type="submit" class="cp-submit" :disabled="submitting || !isComplete">
          {{ submitting ? 'Saving...' : 'Change password' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/utils/api'
import { dashboardForRole } from '@/utils/roleDashboard'
import { useUserDataStore } from '@/stores/userData'

const router = useRouter()
const userStore = useUserDataStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const reveal = reactive({ current: false, next: false, confirm: false })

const submitting = ref(false)
const error = ref('')
const success = ref('')

/** Everything the form needs before it can be sent. */
const isComplete = computed(
  () => currentPassword.value !== '' && newPassword.value !== '' && confirmPassword.value !== '',
)

/** The first thing wrong with the new password, or '' when it is acceptable. */
const hint = computed(() => {
  if (newPassword.value === '') return ''
  if (newPassword.value.length < 8) return 'The new password must be at least 8 characters long.'
  if (!/[A-Z]/.test(newPassword.value)) return 'Add at least one upper-case letter.'
  if (!/[a-z]/.test(newPassword.value)) return 'Add at least one lower-case letter.'
  if (!/[0-9]/.test(newPassword.value)) return 'Add at least one digit.'
  if (!/[^A-Za-z0-9]/.test(newPassword.value)) return 'Add at least one symbol.'
  if (confirmPassword.value !== '' && newPassword.value !== confirmPassword.value) {
    return 'The new password and its confirmation do not match.'
  }
  return ''
})

const submit = async () => {
  if (submitting.value) return
  error.value = ''
  success.value = ''

  if (hint.value) {
    error.value = hint.value
    return
  }

  submitting.value = true
  try {
    await api.put('/api/users/change-password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value,
    })

    // The account no longer holds a temporary password, so the guard lets it through and the
    // user goes to the dashboard their own role owns.
    userStore.setUser({ ...(userStore.user || {}), mustChangePassword: false })
    success.value = 'Your password has been changed.'

    const role = userStore.user?.role
    setTimeout(() => router.push(dashboardForRole(role)), 600)
  } catch (err) {
    const body = err?.response?.data
    error.value =
      typeof body === 'string' && body.trim()
        ? body
        : 'The password could not be changed. Please try again.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.cp-page {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px 20px;
  min-height: 60vh;
}

.cp-card {
  width: 100%;
  max-width: 460px;
  padding: 28px;
  background: #ffffff;
  border: 1px solid #e4e4f2;
  border-radius: var(--radius-xl, 14px);
  box-shadow: 0 12px 32px rgba(38, 35, 66, 0.08);
}

.cp-title {
  margin: 0 0 8px;
  font-size: var(--fs-xl, 1.5rem);
  font-weight: 700;
  color: #1c1a3c;
}

.cp-sub {
  margin: 0 0 20px;
  font-size: var(--fs-sm, 0.875rem);
  line-height: 1.55;
  color: #5b5b7a;
}

.cp-form {
  display: grid;
  gap: 16px;
}

.cp-field label {
  display: block;
  margin-bottom: 6px;
  font-size: var(--fs-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #6b6b8c;
}

.cp-row {
  display: flex;
  gap: 8px;
}

.cp-row input {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #d8d8ec;
  border-radius: var(--radius-md, 8px);
  font-family: inherit;
  font-size: var(--fs-sm, 0.875rem);
  color: #1c1a3c;
}

.cp-row input:focus {
  outline: none;
  border-color: #315bff;
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.12);
}

.cp-toggle {
  padding: 0 12px;
  border: 1px solid #d8d8ec;
  border-radius: var(--radius-md, 8px);
  background: #ffffff;
  font-family: inherit;
  font-size: var(--fs-xs, 0.75rem);
  font-weight: 700;
  color: #262342;
  cursor: pointer;
}

.cp-hint {
  margin: 0;
  font-size: var(--fs-xs, 0.75rem);
  color: #92400e;
}

.cp-error {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md, 8px);
  background: #fee2e2;
  color: #991b1b;
  font-size: var(--fs-sm, 0.875rem);
}

.cp-success {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md, 8px);
  background: #dcfce7;
  color: #166534;
  font-size: var(--fs-sm, 0.875rem);
}

.cp-submit {
  padding: 12px 18px;
  border: none;
  border-radius: var(--radius-md, 8px);
  background: #315bff;
  color: #ffffff;
  font-family: inherit;
  font-size: var(--fs-sm, 0.875rem);
  font-weight: 700;
  cursor: pointer;
}

.cp-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>