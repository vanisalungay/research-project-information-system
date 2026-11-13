import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import api from '@/utils/api'

//
export const UserRole = {
  PROPONENT: 'PROPONENT',
  RPS_ADMIN: 'RPS_ADMIN',
  RPS_STAFF: 'RPS_STAFF',
  // Dean / Department Chair: an independent role, not part of RPS. It is created or
  // assigned by an RPS administrator (Dean accounts) and authenticates through this
  // same login. It is deliberately not a privileged role: a Dean only reaches the
  // endorsement requests inside their authorized college.
  DEAN: 'DEAN',
  OVCRIGE: 'OVCRIGE',
  OVCAF: 'OVCAF',
  REC: 'REC',
  OC: 'OC',
}

const STORAGE_KEY = 'user_data'

/**
 * Extracts the readable reason from an axios failure.
 *
 * The backend answers a refused login with a plain string (for example
 * "Account is pending approval.") and other failures with an object carrying
 * `message` or `error`. Passing the raw value to `new Error(...)` would show
 * "[object Object]" to the user, so the reason is picked out explicitly. The
 * previous behaviour for a string body is unchanged.
 */
function loginErrorMessage(error, fallback) {
  const body = error?.response?.data
  if (typeof body === 'string' && body.trim() !== '') return body
  if (body && typeof body === 'object') {
    if (typeof body.message === 'string' && body.message.trim() !== '') return body.message
    if (typeof body.error === 'string' && body.error.trim() !== '') return body.error
  }
  return fallback
}

export const useUserDataStore = defineStore('userData', () => {
  // state
  const user = ref(null)
  const isLoading = ref(false)

  // getters
  const isLoggedIn = computed(() => !!user.value)
  const displayName = computed(() => user.value?.name ?? 'Guest')

  // helpers
  function persist(payload) {
    if (payload) {
      localStorage.setItem(STORAGE_KEY, btoa(JSON.stringify(payload)))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  // actions
  function hydrate() {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      user.value = JSON.parse(atob(stored))
    }
  }

  function setUser(payload) {
    user.value = payload
    persist(payload)
  }

  function clearUser() {
    user.value = null
    persist(null)
  }

  /**
   * Authenticates against the backend.
   *
   * This is the only way in. The store previously fell back to a hardcoded table
   * of accounts (with their passwords) and to a `offline_users` list kept in
   * localStorage whenever the API could not be reached, so a wrong or unavailable
   * backend still produced a "successful" login into a fabricated identity that
   * never existed in the database. Those paths are gone: every account now comes
   * from the backend, and an unreachable server is reported as such.
   *
   * @returns {Promise<boolean>} true when the backend accepted the credentials
   * @throws {Error} with the reason the backend refused, or with the fact that the
   *                 server could not be reached
   */
  async function login(email, password, role) {
    isLoading.value = true
    try {
      const response = await api.post('/api/users/login', { email, password, role })

      if (response.data) {
        setUser(response.data)
        return true
      }

      return false
    } catch (error) {
      // A response from the backend (401/403/...) carries the real reason.
      if (error.response) {
        throw new Error(loginErrorMessage(error, 'Invalid email, password, or role.'))
      }

      // No response at all: the server is unreachable. This is reported instead of
      // being masked by a local fallback account.
      throw new Error(
        'Cannot reach the RPIS server. Please check your connection and try again.',
      )
    } finally {
      isLoading.value = false
    }
  }

  async function googleLogin(token, role) {
    try {
      isLoading.value = true
      const response = await api.post(
        '/api/users/google-login',
        {
          token,
          role,
        }
      )

      if (response.data) {
        setUser(response.data)
        return true
      }

      return false
    } catch (error) {
      console.error(error)
      throw new Error(loginErrorMessage(error, 'Google login failed.'))
    } finally {
      isLoading.value = false
    }
  }

  function logout() {
    clearUser()
  }

  return {
    user,
    isLoading,
    isLoggedIn,
    displayName,
    hydrate,
    setUser,
    clearUser,
    login,
    googleLogin,
    logout,
  }
})

