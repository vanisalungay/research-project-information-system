<template>
  <div class="header-content">
    <!-- Left: Logo & System Title -->
    <div class="logo-section">
      <img src="@/assets/images/logo.png" alt="MSU-Naawan Logo" class="logo-img" />
      <div class="system-title">
        <p class="system-name">
          <span class="research">Research</span>
          <span class="project">Project</span>
        </p>
        <p class="system-subname">Information System</p>
      </div>
    </div>

    <!-- Right: User Info & Dropdown -->
    <div class="user-info" ref="userInfoRef" @click="toggleDropdown">
      <img src="@/assets/images/avatar.png" alt="User Avatar" class="user-avatar" />

      <div class="user-details">
        <p class="user-name">{{ userStore.user?.name ?? 'Guest' }}</p>
        <p class="user-role">{{ formatRole(userStore.user?.role) }}</p>
      </div>

      <svg class="chevron-icon" :class="{ open: showDropdown }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9"></polyline>
      </svg>

      <!-- Dropdown Menu -->
      <div v-if="showDropdown" class="dropdown-menu">
        <button
          v-for="menuItem in menuItems"
          :key="menuItem"
          class="dropdown-item"
          :class="{ 'logout-item': menuItem === 'LOGOUT' }"
          @click.stop="handleMenuItemClick(menuItem)"
        >
          <svg v-if="menuItem === 'LOGOUT'" class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          <svg v-else class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <polyline points="17 11 19 13 23 9"></polyline>
          </svg>
          {{ menuItem }}
        </button>
      </div>
    </div>

    <!-- Switch Role Login Dialog -->
    <div v-if="showLoginDialog" class="dialog-overlay" @click.self="closeLoginDialog">
      <div class="dialog-box">
        <button class="dialog-close" @click="closeLoginDialog">×</button>

        <h3 class="dialog-title">Switch to {{ switchRoleLabel }}</h3>
        <p class="dialog-subtitle">Enter your credentials for the {{ switchRoleLabel }} account.</p>

        <form class="login-form" @submit.prevent="handleLogin">
          <label>Email Address</label>
          <input type="email" v-model="email" placeholder="you@msunaawan.edu.ph" required />

          <label>Password</label>
          <input type="password" v-model="password" placeholder="Enter your password" required />

          <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

          <button class="login-btn" type="submit" :disabled="loginLoading">
            {{ loginLoading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'

const router = useRouter()
const userStore = useUserDataStore()

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const loginLoading = ref(false)

const showDropdown = ref(false)
const showLoginDialog = ref(false)
const selectedRole = ref(null)
const userInfoRef = ref(null)

const switchRoleLabel = computed(() => {
  if (selectedRole.value === UserRole.RPS_STAFF) return 'RPS Staff'
  if (selectedRole.value === UserRole.RPS_ADMIN) return 'RPS Admin'
  return ''
})

const formatRole = (role) => {
  const roleMap = {
    'PROPONENT': 'Proponent',
    'RPS_ADMIN': 'RPS Admin',
    'RPS_STAFF': 'RPS Staff',
    'OVCRIGE': 'OVCRIGE',
    'OVCAF': 'OVCAF',
    'REC': 'REC',
    'OC': 'Chancellor',
  }
  return roleMap[role] || role || ''
}

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const closeLoginDialog = () => {
  showLoginDialog.value = false
  selectedRole.value = null
  email.value = ''
  password.value = ''
  errorMessage.value = ''
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

const handleLogin = async () => {
  errorMessage.value = ''
  loginLoading.value = true

  try {
    const success = await userStore.login(
      email.value,
      password.value,
      selectedRole.value
    )

    if (!success) {
      errorMessage.value = 'Invalid email or password.'
      return
    }

    closeLoginDialog()
    router.push('/home')
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loginLoading.value = false
  }
}

const handleMenuItemClick = (menuItem) => {
  showDropdown.value = false

  switch (menuItem) {
    case 'SWITCH TO RPS STAFF':
      selectedRole.value = UserRole.RPS_STAFF
      showLoginDialog.value = true
      break

    case 'SWITCH TO RPS ADMIN':
      selectedRole.value = UserRole.RPS_ADMIN
      showLoginDialog.value = true
      break

    case 'LOGOUT':
      logout()
      break
  }
}

const menuItems = computed(() => {
  const items = []

  if (userStore.user?.role === UserRole.RPS_ADMIN) {
    items.push('SWITCH TO RPS STAFF')
  }

  if (userStore.user?.role === UserRole.RPS_STAFF) {
    items.push('SWITCH TO RPS ADMIN')
  }

  items.push('LOGOUT')

  return items
})

// Click-outside handler to close dropdown
const handleOutsideClick = (event) => {
  if (userInfoRef.value && !userInfoRef.value.contains(event.target)) {
    showDropdown.value = false
  }
}

// Escape key handler to close dialog
const handleEscapeKey = (event) => {
  if (event.key === 'Escape') {
    if (showLoginDialog.value) {
      closeLoginDialog()
    }
    if (showDropdown.value) {
      showDropdown.value = false
    }
  }
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
  document.addEventListener('keydown', handleEscapeKey)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
  document.removeEventListener('keydown', handleEscapeKey)
})
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* ===== Logo Section ===== */
.logo-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.system-title {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.system-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin: 0;
}

.research {
  color: #fff;
}

.project {
  color: #ffd803;
}

.system-subname {
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.5px;
  margin: 2px 0 0 0;
}

/* ===== User Info & Dropdown ===== */
.user-info {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 10px;
  transition: background 0.15s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 2px solid rgba(255, 216, 3, 0.4);
}

.user-details {
  text-align: left;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  line-height: 1.3;
}

.user-role {
  font-size: 12px;
  color: #ffd803;
  margin: 0;
  line-height: 1.3;
}

.chevron-icon {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.2s ease;
}

.chevron-icon.open {
  transform: rotate(180deg);
}

/* ===== Dropdown Menu ===== */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #2d2952;
  border: 1px solid rgba(255, 216, 3, 0.15);
  border-radius: 10px;
  min-width: 240px;
  z-index: 100;
  padding: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: #ffd803;
  border-radius: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: background 0.15s ease;
}

.dropdown-item:hover {
  background: rgba(255, 216, 3, 0.1);
}

.dropdown-item.logout-item {
  color: #f87171;
  margin-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 12px;
  border-radius: 0 0 8px 8px;
}

.dropdown-item.logout-item:hover {
  background: rgba(248, 113, 113, 0.1);
}

.menu-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* ===== Login Dialog ===== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.dialog-box {
  background: #fff;
  border-radius: 14px;
  padding: 32px;
  width: 400px;
  max-width: 90vw;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.dialog-close {
  position: absolute;
  top: 12px;
  right: 16px;
  background: none;
  border: none;
  font-size: 24px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}

.dialog-close:hover {
  color: #1f2937;
}

.dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.dialog-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px 0;
}

.login-form {
  display: flex;
  flex-direction: column;
}

.login-form label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
  margin-top: 14px;
}

.login-form label:first-child {
  margin-top: 0;
}

.login-form input {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease;
}

.login-form input:focus {
  border-color: #2452ff;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.1);
}

.login-btn {
  background: #2452ff;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  margin-top: 20px;
  transition: background 0.15s ease;
}

.login-btn:hover:not(:disabled) {
  background: #1d40cc;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #dc2626;
  font-size: 13px;
  margin-top: 12px;
  background: #fef2f2;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #fecaca;
}
</style>