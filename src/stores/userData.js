import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import api from '@/utils/api'

//
export const UserRole = {
  PROPONENT: 'PROPONENT',
  RPS_ADMIN: 'RPS_ADMIN',
  RPS_STAFF: 'RPS_STAFF',
  OVCRIGE: 'OVCRIGE',
  OVCAF: 'OVCAF',
  REC: 'REC',
  OC: 'OC',
}

const STORAGE_KEY = 'user_data'

// temp only
const TEMPORARY_ACCOUNTS = {
  [UserRole.PROPONENT]: {
    id: 1,
    name: 'Proponent User',
    email: 'proponent@gmail.com',
    role: UserRole.PROPONENT,
    password: 'proponentpassword1234',
  },
  [UserRole.RPS_STAFF]: {
    id: 2,
    name: 'RPS Staff',
    email: 'rps_staff@gmail.com',
    role: UserRole.RPS_STAFF,
    password: 'rpsstaffpassword1234',
  },
  [UserRole.OVCAF]: {
    id: 3,
    name: 'OVCAF User',
    email: 'ovcaf@gmail.com',
    role: UserRole.OVCAF,
    password: 'ovcafpassword1234',
  },
  [UserRole.OVCRIGE]: {
    id: 4,
    name: 'OVCRIGE User',
    email: 'ovcrige@gmail.com',
    role: UserRole.OVCRIGE,
    password: 'ovcrigepassword1234',
  },
  [UserRole.REC]: {
    id: 5,
    name: 'REC User',
    email: 'rec@gmail.com',
    role: UserRole.REC,
    password: 'recpassword1234',
  },
  [UserRole.RPS_ADMIN]: {
    id: 6,
    name: 'RPS Admin User',
    email: 'rps_admin@gmail.com',
    role: UserRole.RPS_ADMIN,
    password: 'rpsadminpassword1234',
  },
  [UserRole.OC]: {
    id: 7,
    name: 'OC User',
    email: 'oc@gmail.com',
    role: UserRole.OC,
    password: 'ocpassword1234',
  },
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

  async function login(email, password, role) {
    try {
      isLoading.value = true

      console.log('EMAIL:', email)
      console.log('PASSWORD:', password)
      console.log('ROLE:', role)

      const response = await api.post(
        '/api/users/login',
        {
          email,
          password,
          role,
        }
      )

      console.log('LOGIN RESPONSE:', response.data)
      if (response.data) {
        setUser(response.data)
        return true
      }

      return false
    } catch (error) {
      console.warn("Backend login failed or server offline. Checking local mock and offline users database...")

      // 1. Check offline registered users in localStorage
      const offlineUsers = JSON.parse(localStorage.getItem('offline_users') || '[]')
      const offlineUser = offlineUsers.find(
        (u) => u.email.toLowerCase() === email.toLowerCase() &&
          u.role.toUpperCase() === role.toUpperCase()
      )

      if (offlineUser) {
        if (offlineUser.password === password) {
          if (offlineUser.status && offlineUser.status.toUpperCase() === 'PENDING') {
            throw new Error('Your account is pending RPS ADMIN approval.')
          } else if (offlineUser.status && offlineUser.status.toUpperCase() === 'REJECTED') {
            throw new Error('Your account registration was rejected.')
          } else if (offlineUser.status && offlineUser.status.toUpperCase() === 'APPROVED') {
            console.log("Logged in using offline registered account.")
            setUser(offlineUser)
            return true
          }
        }
      }

      // 2. Check hardcoded TEMPORARY_ACCOUNTS
      const tempUser = TEMPORARY_ACCOUNTS[role]
      if (
        tempUser &&
        tempUser.email.toLowerCase() === email.toLowerCase() &&
        tempUser.password === password
      ) {
        console.log("Logged in using local mock credentials.")
        setUser(tempUser)
        return true
      }

      console.error(error)
      throw new Error(error.response?.data || 'Invalid email, password, or role.')
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
      throw new Error(error.response?.data || 'Google login failed.')
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
