import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

//
export const UserRole = {
  PROPONENT: 'PROPONENT',
  RII_ADMIN: 'RII_ADMIN',
  RII_STAFF: 'RII_STAFF',
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
  [UserRole.RII_STAFF]: {
    id: 2,
    name: 'RII Staff',
    email: 'rii_staff@gmail.com',
    role: UserRole.RII_STAFF,
    password: 'riistaffpassword1234',
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
  [UserRole.RII_ADMIN]: {
    id: 6,
    name: 'RII Admin User',
    email: 'rii_admin@gmail.com',
    role: UserRole.RII_ADMIN,
    password: 'riiadminpassword1234',
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

      const targetUser = TEMPORARY_ACCOUNTS[role]

      if (targetUser && targetUser.email === email && targetUser.password === password) {
        const { password, ...safeUser } = targetUser
        setUser(safeUser)
        return true
      }

      return false
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
    logout,
  }
})
