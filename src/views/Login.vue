<template>
  <div class="login-wrapper">
    <div class="left-panel">
      <img src="@/assets/images/logo.png" class="school-logo" />

      <h1 class="title">
        Research <span>Project</span><br />
        Information<br />
        System
      </h1>

      <p class="school-name">
        RESEARCH AND INNOVATION INSTITUTE<br />
        Mindanao State University at Naawan
      </p>
    </div>

    <div class="right-panel">
      <h2 class="welcome">Welcome!</h2>
      <p class="subtitle">Sign in to continue to your dashboard.</p>

      <div class="role-container">
        <div
          v-for="role in roles"
          :key="role.name"
          class="role-box"
          :class="{ active: selectedRole === role.value }"
          @click="selectedRole = role.value"
        >
          <h3>{{ role.name }}</h3>
          <p>{{ role.desc }}</p>
        </div>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label>Email Address</label>
        <input type="email" v-model="email" placeholder="you@msunaawan.edu.ph" />

        <label>Password</label>
        <input type="password" v-model="password" placeholder="Enter your password" />
        <p class="forgot"><a href="/register">Forgot password?</a></p>
        <div class="remember">
          <input type="checkbox" id="remember" />
          <label for="remember">Remember me</label>
        </div>

        <button class="login-btn">Sign In</button>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <p class="signup">Don't have an account? <a href="/create-account">Sign up</a></p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'

const userStore = useUserDataStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const errorMessage = ref('')

const roles = [
  { name: 'Proponent', desc: 'Research Proposal Submitter', value: UserRole.PROPONENT },
  { name: 'RII', desc: 'Research and Innovation Institute', value: UserRole.RII_STAFF },
  {
    name: 'OVCRIGE',
    desc: 'Office of the Vice Chancellor for Research, Innovation, and Global Engagement',
    value: UserRole.OVCRIGE,
  },
  { name: 'REC', desc: 'Research Evaluation Committee', value: UserRole.REC },
  {
    name: 'OVCAF',
    desc: 'Office of the Vice Chancellor for Admin and Finance',
    value: UserRole.OVCAF,
  },
  { name: 'OC', desc: 'Office of the Chancellor', value: UserRole.OC },
]

const selectedRole = ref('PROPONENT')

const handleLogin = async () => {
  const isLoggedIn = await userStore.login(email.value, password.value, selectedRole.value)
  if (isLoggedIn) {
    router.push('/home')
  } else {
    errorMessage.value = 'Invalid email or password.'
  }
}
</script>

<style>
.login-wrapper {
  display: flex;
  height: 100vh;
  background: #f8f8f8;
}

.left-panel {
  width: 40%;
  background: #1f1a3d;
  padding: 40px;
  color: white;
}

.school-logo {
  width: 90px;
  margin-bottom: 20px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.title span {
  color: #ffdf00;
}

.school-name {
  margin-top: 40px;
  font-size: 14px;
}

.right-panel {
  width: 60%;
  padding: 50px;
}

.welcome {
  font-size: 36px;
  font-weight: 700;
}

.subtitle {
  margin-bottom: 20px;
  color: #555;
}

.role-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin: 20px 0;
}

.role-box {
  padding: 15px;
  border-radius: 8px;
  border: 2px solid #ddd;
  background: white;
  cursor: pointer;
  transition: 0.2s;
}

.role-box.active {
  background: #ffdf00;
  border-color: #ffdf00;
}

.role-box h3 {
  margin: 0;
  font-weight: 700;
}

.role-box p {
  margin: 5px 0 0 0;
  font-size: 12px;
}

.login-form label {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  width: 350px;
  color: #1f1a3d;
}

.login-form input {
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #bbb;
  color: #1f1a3d;
}

.remember {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.remember input[type='checkbox'] {
  width: 16px;
  height: 16px;
  margin: 0;
}

.remember label {
  font-size: 14px;
  color: #1f1a3d;
  cursor: pointer;
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

.error {
  color: red;
}

.forgot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 15px;
}

.signup {
  margin-top: 10px;
}
</style>
