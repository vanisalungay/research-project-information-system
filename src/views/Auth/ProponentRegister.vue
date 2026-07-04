<template>
  <div class="register-wrapper">
    <!-- LEFT PANEL -->
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

    <!-- RIGHT PANEL -->
    <div class="right-panel">
      <h2 class="welcome">CREATE PROPONENT ACCOUNT</h2>
      <p class="subtitle">
        Register as a Proponent to access the Research Project Information System
      </p>

      <!-- FORM -->
      <form class="register-form" @submit.prevent="handleRegister">
        <label>Full Name</label>
        <input
          type="text"
          v-model="name"
          placeholder="Enter your full name"
          required
        />

        <label>ID Number</label>
        <input
          type="text"
          v-model="idNumber"
          placeholder="Enter your ID number"
          required
        />

        <label>University Email Address</label>
        <input
          type="email"
          v-model="email"
          placeholder="you@msunaawan.edu.ph"
          required
        />

        <div class="half-inputs">
          <div>
            <label>Password</label>
            <input
              type="password"
              v-model="password"
              placeholder="Create a password"
              required
            />
          </div>

          <div>
            <label>Confirm Password</label>
            <input
              type="password"
              v-model="confirmPassword"
              placeholder="Confirm your password"
              required
            />
          </div>
        </div>

        <div class="terms">
          <input type="checkbox" v-model="agree" required />
          <label>
            I agree to the terms and conditions of the Research Project Information System.
          </label>
        </div>

        <button class="register-btn">
          Create Account
        </button>

        <p class="signin-link">
          Already have an account?
          <router-link to="/proponent-login">Sign In</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const name = ref('')
const idNumber = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)

const handleRegister = () => {
  if (password.value !== confirmPassword.value) {
    alert('Passwords do not match')
    return
  }

  alert('Proponent account created successfully!')
  router.push('/proponent-login')
}
</script>

<style scoped>
.register-wrapper {
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
  margin-bottom: 25px;
  color: #555;
}

.register-form {
  display: flex;
  flex-direction: column;
  width: 420px;
}

.register-form label {
  color: #1f1a3d;
  margin-bottom: 5px;
  font-weight: 500;
}

.register-form input {
  padding: 10px;
  margin-bottom: 15px;
  border-radius: 6px;
  border: 1px solid #bbb;
}

.half-inputs {
  display: flex;
  gap: 10px;
}

.half-inputs > div {
  flex: 1;
}

.terms {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 15px 0;
  font-size: 14px;
}

.terms input {
  margin-top: 3px;
}

.register-btn {
  background: #2452ff;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
  font-size: 15px;
}

.register-btn:hover {
  background: #1b44d6;
}

.signin-link {
  text-align: center;
  margin-top: 20px;
}

.signin-link a {
  color: #2452ff;
  text-decoration: none;
  font-weight: 600;
}

.signin-link a:hover {
  text-decoration: underline;
}
</style>