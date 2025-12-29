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

    <div class="right-panel">
      <h2 class="welcome">CREATE ACCOUNT</h2>
      <p class="subtitle">Register to access the Research Project Information System</p>

      <p class="register-as-label">Register as</p>

      <div class="role-container">
        <div
          v-for="role in roles"
          :key="role.name"
          class="role-box"
          :class="{ active: selectedRole === role.name }"
          @click="selectedRole = role.name"
        >
          <h3>{{ role.name }}</h3>
          <p>{{ role.desc }}</p>
        </div>
      </div>

      <!-- FORM -->
      <form class="register-form" @submit.prevent="handleRegister">
        <label>Name</label>
        <input type="text" v-model="name" placeholder="Enter your full name" required />

        <label>ID Number</label>
        <input type="text" v-model="idNumber" placeholder="Enter your ID number" required />

        <label>University Email Address</label>
        <input type="email" v-model="email" placeholder="you@msunaawan.edu.ph" required />

        <div class="half-inputs">
          <div>
            <label>Password</label>
            <input type="password" v-model="password" placeholder="Create a password" required />
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

        <!-- TERMS BOX -->
        <div class="terms">
          <input type="checkbox" v-model="agree" required />
          <label
            >I agree to the terms and conditions of the Research Project Information System.</label
          >
        </div>

        <button class="register-btn">Create Account</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const roles = [
  { name: 'Proponent', desc: 'Research Proposal Submitter' },
  { name: 'RII', desc: 'Research and Innovation Institute' },
  {
    name: 'OVCRIGE',
    desc: 'Office of the Vice Chancellor for Research, Innovation, and Global Engagement',
  },
  { name: 'REC', desc: 'Research Evaluation Committee' },
  { name: 'OVCAF', desc: 'Office of the Vice Chancellor for Admin and Finance' },
  { name: 'OC', desc: 'Office of the Chancellor' },
]

const selectedRole = ref('Proponent')

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

  alert('Account Created Successfully!')
  router.push('/login')
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
  margin-bottom: 20px;
  color: #555;
}

.register-as-label {
  margin-top: 10px;
  font-weight: 600;
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
  margin: 5px 0 0;
  font-size: 12px;
}

.register-form {
  display: flex;
  flex-direction: column;
  width: 350px;
}

.register-form input {
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #bbb;
}

.half-inputs {
  display: flex;
  gap: 10px;
}

.terms {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 15px 0;
}

.register-btn {
  background: #2452ff;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
}
label {
  color: #1f1a3d;
}
</style>
