<template>
  <div class="header-content">
    <div class="logo-section">
      <img src="@/assets/images/logo.png" alt="Logo" />
      <div class="system-title">
        <p class="system-name">
          <span class="research-bold">Research</span>
          <span class="project-yellow">Project</span>
        </p>
        <p class="system-subname">Information System</p>
      </div>
    </div>

    <div class="user-info" @click="toggleDropdown">
      <img src="@/assets/images/avatar.png" alt="User Avatar" class="user-avatar" />

      <div class="user-details">
        <p class="user-name poppins-semibold">
          {{ userStore.user?.name ?? 'Guest' }}
        </p>
        <p class="user-email poppins-semibold">
          {{ userStore.user?.email ?? '' }}
        </p>
      </div>

      <div v-if="showDropdown" class="dropdown-menu">
        <button
          v-for="menuItem in menuItems"
          class="dropdown-item"
          @click.stop="handleMenuItemClick(menuItem)"
        >
          {{ menuItem }}
        </button>
      </div>
    </div>
    <dialog class="login-dialog" ref="loginDialog" closedby="any">
      <form class="login-form" @submit.prevent="handleLogin">
        <a class="close-icon" @click="closeDialog">×</a>
        <label>Email Address</label>
        <input type="email" v-model="email" placeholder="you@msunaawan.edu.ph" />

        <label>Password</label>
        <input type="password" v-model="password" placeholder="Enter your password" />

        <button class="login-btn">Sign In</button>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      </form>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'

const router = useRouter()
const userStore = useUserDataStore()

const email = ref('')
const password = ref('')
const errorMessage = ref('')

const showDropdown = ref(false)
const loginDialog = ref(null)
const selectedRole = ref(null)

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const closeDialog = () => {
  loginDialog.value?.close()
}

const logout = () => {
  const role = userStore.user?.role

  userStore.logout()

  if (role === UserRole.PROPONENT) {
    router.push('/proponent-login')
  } else {
    router.push('/login')
  }
}

const handleLogin = async () => {
  errorMessage.value = ''

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

    loginDialog.value?.close()

    switch (selectedRole.value) {
      case UserRole.RPS_ADMIN:
        router.push('/rpsadmin-dash')
        break

      case UserRole.RPS_STAFF:
        router.push('/rpsstaff-dash')
        break

      case UserRole.OVCRIGE:
        router.push('/home')
        break

      case UserRole.OVCAF:
        router.push('/ovcaf-dash')
        break

      case UserRole.REC:
        router.push('/rec-dash')
        break

      case UserRole.OC:
        router.push('/oc-dashboard')
        break

      case UserRole.PROPONENT:
        router.push('/proponent-dashboard')
        break

      default:
        router.push('/login')
    }

    window.location.reload()
  } catch (err) {
    errorMessage.value = err.message
  }
}

const handleMenuItemClick = (menuItem) => {
  showDropdown.value = false

  switch (menuItem) {
    case 'SWITCH TO RPS STAFF':
      selectedRole.value = UserRole.RPS_STAFF
      loginDialog.value?.showModal()
      break

    case 'SWITCH TO RPS ADMIN':
      selectedRole.value = UserRole.RPS_ADMIN
      loginDialog.value?.showModal()
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
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.logo-section {
  display: flex;
  gap: 12px;
}

.logo-section img {
  width: 54px;
  object-fit: contain;
}

.system-title {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  line-height: 1.1;
}

.system-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.system-subname {
  font-size: 25px;
  font-weight: 500;
  letter-spacing: 1px;
  text-align: left;
}

.user-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-avatar {
  width: 33px;
  height: 33px;
}

.user-details {
  text-align: left;
}

.user-name {
  font-size: 16px;
  color: #fff;
}

.user-email {
  font-size: 14px;
  color: #fff;
}

.user-info {
  position: relative;
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: #433d71;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  z-index: 10;
  width: 238px;
  padding: 10px;
}

.dropdown-item {
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: #262342;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
  color: #ffd803;
  border-radius: 45px;
  font-weight: bold;
  text-transform: uppercase;
  margin-top: 7px;
}

.dropdown-item:hover {
  background: #262342;
  opacity: 0.8;
}

.research-bold {
  font-weight: bold;
  font-size: 35px;
  margin-right: 10px;
}

.project-yellow {
  color: #ffd803;
  font-weight: bold;
  font-size: 35px;
}

.login-dialog {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;

  max-width: 900px;
  max-height: none;
  margin: 0;
  padding: 0;
  border: none;

  background: rgba(0, 0, 0, 0.77);
}

.login-form {
  display: flex;
  flex-direction: column;
  width: 350px;
  margin: auto;
  background-color: #ffffff;
  padding: 20px;
  border-radius: 6px;
  margin-top: 20px;
  position: relative;
}

.login-form input {
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #bbb;
}

.login-btn {
  background: #2452ff;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 20px;
}

.close-icon {
  position: absolute;
  top: 1px;
  right: 10px;
  font-size: 25px;
  cursor: pointer;
}
</style>
