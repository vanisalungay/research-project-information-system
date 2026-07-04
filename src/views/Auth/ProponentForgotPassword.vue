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
      <h2 class="welcome">Forgot Password</h2>

      <p class="subtitle">
        Enter your registered email address and we'll send you a password reset link.
      </p>

      <form class="forgot-form" @submit.prevent="handleReset">
        <label>Email Address</label>

        <input
          type="email"
          v-model="email"
          placeholder="you@msunaawan.edu.ph"
          required
        />

        <button type="submit" class="login-btn">
          Send Reset Link
        </button>

        <p v-if="successMessage" class="success">
          {{ successMessage }}
        </p>

        <p v-if="errorMessage" class="error">
          {{ errorMessage }}
        </p>

        <p class="back">
          Remember your password?
          <router-link to="/proponent-login">
            Back to Sign In
          </router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const email = ref('')
const successMessage = ref('')
const errorMessage = ref('')

const handleReset = () => {
  errorMessage.value = ''

  if (!email.value) {
    errorMessage.value = 'Please enter your email address.'
    return
  }

  successMessage.value =
    'A password reset link has been sent to your email address.'
}
</script>

<style scoped>
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
  margin-bottom: 25px;
  color: #555;
  max-width: 500px;
}

.forgot-form {
  width: 400px;
  display: flex;
  flex-direction: column;
}

.forgot-form label {
  margin-bottom: 8px;
  color: #1f1a3d;
  font-weight: 500;
}

.forgot-form input {
  padding: 12px;
  margin-bottom: 15px;
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
  margin-bottom: 15px;
  font-size: 15px;
}

.login-btn:hover {
  background: #1b44d6;
}

.success {
  color: #16a34a;
  margin-bottom: 10px;
}

.error {
  color: #dc2626;
  margin-bottom: 10px;
}

.back {
  margin-top: 10px;
}

.back a {
  color: #2452ff;
  text-decoration: none;
  font-weight: 600;
}

.back a:hover {
  text-decoration: underline;
}
</style>