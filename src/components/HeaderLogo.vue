<template>
  <!--
    Application header (the reference shell): a dark navy bar with a continuous yellow
    bottom border, the sidebar toggle and the RPIS brand on the left, and the
    notification control plus the account dropdown on the right. Everything it shows
    comes from the existing stores (current user, unread notifications) and every
    control keeps the behaviour it already had.
  -->
  <header class="header-root">
    <div class="header-content">
      <!-- LEFT: sidebar toggle, circular RP logo, brand -->
      <div class="header-left">
        <button
          type="button"
          class="hamburger-btn"
          aria-label="Toggle sidebar"
          :aria-expanded="collapsed ? 'false' : 'true'"
          @click="$emit('toggle-sidebar')"
        >
          <svg class="hamburger-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
            stroke-linecap="round" aria-hidden="true">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        <!-- The system favicon (public/favicon.ico, the same file declared in index.html). -->
        <img class="brand-logo" src="/favicon.ico" alt="" aria-hidden="true" />

        <div class="brand-text">
          <p class="brand-name">
            <span class="brand-research">Research</span><span class="brand-project">Project</span>
          </p>
          <p class="brand-subtitle">Information System</p>
        </div>
      </div>

      <!-- RIGHT: notifications, separator, account -->
      <div class="header-right">
        <button
          type="button"
          class="notif-btn"
          :title="notificationLabel"
          :aria-label="notificationLabel"
          @click="openNotifications"
        >
          <svg class="notif-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <!-- The yellow dot appears only while the account really has unread items. -->
          <span v-if="notificationStore.hasUnread" class="notif-dot" aria-hidden="true"></span>
        </button>

        <span class="header-separator" aria-hidden="true"></span>

        <div
          ref="userInfoRef"
          class="user-info"
          role="button"
          tabindex="0"
          aria-haspopup="menu"
          :aria-expanded="showDropdown ? 'true' : 'false'"
          :aria-label="`Account menu for ${userStore.user?.name ?? 'Guest'}`"
          @click="toggleDropdown"
          @keydown.enter.prevent="toggleDropdown"
          @keydown.space.prevent="toggleDropdown"
        >
          <span class="user-avatar" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
              stroke-linejoin="round">
              <circle cx="12" cy="8.4" r="3.6" />
              <path d="M4.6 20.4a7.4 7.4 0 0114.8 0" />
            </svg>
          </span>

          <div class="user-details">
            <p class="user-name">{{ userStore.user?.name ?? 'Guest' }}</p>
            <p class="user-role">{{ formatRole(userStore.user?.role) }}</p>
          </div>

          <svg class="chevron-icon" :class="{ open: showDropdown }" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" aria-hidden="true">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>

          <!-- Dropdown Menu (unchanged: the same role-based items and actions) -->
          <div v-if="showDropdown" class="dropdown-menu" role="menu">
            <button
              v-for="menuItem in menuItems"
              :key="menuItem"
              class="dropdown-item"
              :class="{ 'logout-item': menuItem === 'LOGOUT' }"
              role="menuitem"
              @click.stop="handleMenuItemClick(menuItem)"
            >
              <svg v-if="menuItem === 'LOGOUT'" class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2">
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
  </header>
</template>


<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'
import { useNotificationStore } from '@/stores/notifications'

defineProps({
  /** True while the sidebar is collapsed (the hamburger reflects it). */
  collapsed: { type: Boolean, default: false },
})
defineEmits(['toggle-sidebar'])

const router = useRouter()
const userStore = useUserDataStore()
const notificationStore = useNotificationStore()

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

/** The account's role, as it is shown under the name (existing mapping). */
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

/** Label of the notification control: it also announces the unread count. */
const notificationLabel = computed(() =>
  notificationStore.hasUnread
    ? `Notifications (${notificationStore.unreadCount} unread)`
    : 'Notifications',
)

/** Opens the same notifications page the sidebar links to. */
const openNotifications = () => {
  router.push('/notifications')
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
  notificationStore.reset()
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
/* ===== Header container =====
   The 67px height includes the 3px yellow bottom border (box-sizing: border-box is
   global), so the border runs continuously from the left to the right of the viewport. */
.header-root {
  height: var(--rpis-header-height);
  width: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: var(--rpis-header-bg);
  border-bottom: var(--rpis-header-border-height) solid var(--rpis-header-border);
  color: var(--rpis-on-dark);
  position: relative;
  z-index: 30;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

/* ===== Left: sidebar toggle, logo, brand ===== */
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.hamburger-btn {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--rpis-icon);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.hamburger-btn:hover {
  background: var(--rpis-surface-control);
  color: var(--rpis-on-dark);
}

.hamburger-btn:focus-visible {
  outline: 2px solid var(--rpis-accent);
  outline-offset: 2px;
}

.hamburger-icon {
  width: 19px;
  height: 19px;
}

/* The favicon keeps its own design and transparency: no circle, border or background is
   added around it, and object-fit keeps the seal's proportions at header size. */
.brand-logo {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  object-fit: contain;
  display: block;
}

.brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.brand-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.brand-research {
  color: var(--rpis-on-dark);
}

.brand-project {
  color: var(--rpis-accent);
}

.brand-subtitle {
  margin: 0;
  font-size: 12.5px;
  font-weight: 500;
  line-height: 16px;
  color: var(--rpis-on-dark-soft);
  white-space: nowrap;
}

/* ===== Right: notifications, separator, account ===== */
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-shrink: 0;
}

.notif-btn {
  position: relative;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 13px;
  background: var(--rpis-surface-control);
  color: var(--rpis-icon);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.notif-btn:hover {
  background: var(--rpis-surface-control-hover);
  color: var(--rpis-on-dark);
}

.notif-btn:focus-visible {
  outline: 2px solid var(--rpis-accent);
  outline-offset: 2px;
}

.notif-icon {
  width: 20px;
  height: 20px;
}

.notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--rpis-accent);
  box-shadow: 0 0 0 2px var(--rpis-surface-control);
}

.header-separator {
  width: 1px;
  height: 36px;
  flex-shrink: 0;
  background: var(--rpis-separator);
}

.user-info {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 202px;
  height: 45px;
  padding: 0 12px 0 4px;
  border-radius: 26px;
  background: var(--rpis-surface-control);
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-info:hover {
  background: var(--rpis-surface-control-hover);
}

.user-info:focus-visible {
  outline: 2px solid var(--rpis-accent);
  outline-offset: 2px;
}

.user-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--rpis-accent);
  color: #1c1a3c;
}

.user-avatar svg {
  width: 20px;
  height: 20px;
}

.user-details {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.user-name {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 17px;
  color: var(--rpis-on-dark);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  line-height: 15px;
  color: var(--rpis-accent);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chevron-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--rpis-on-dark-soft);
  transition: transform 0.2s ease;
}

.chevron-icon.open {
  transform: rotate(180deg);
}

/* ===== Dropdown menu (unchanged behaviour, kept on the shell palette) ===== */
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
  color: var(--rpis-accent);
  border-radius: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: background 0.15s ease;
}

.dropdown-item:hover {
  background: rgba(255, 216, 3, 0.1);
}

.dropdown-item:focus-visible {
  outline: 2px solid var(--rpis-accent);
  outline-offset: -2px;
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

/* ===== Responsive =====
   The account pill simplifies before anything can overlap: the avatar and the
   chevron stay, and the dropdown keeps every account action reachable. */
@media (max-width: 1024px) {
  .header-root {
    padding: 0 14px;
  }
}

@media (max-width: 768px) {
  .user-info {
    width: auto;
    padding: 0 10px 0 4px;
  }

  .user-details {
    display: none;
  }
}

@media (max-width: 560px) {
  .header-root {
    padding: 0 10px;
  }

  .header-left {
    gap: 8px;
  }

  .brand-logo {
    width: 40px;
    height: 40px;
  }

  .brand-subtitle {
    display: none;
  }

  .header-right {
    gap: 10px;
  }
}
</style>
