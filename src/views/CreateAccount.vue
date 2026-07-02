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
        RESEARCH AND PUBLICATION SERVICES<br />
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
        <label>Department / Office</label>
        <input
          type="text"
          v-model="departmentOffice"
          placeholder="Enter your department or office"
          required
        />

        <label>University Email Address</label>
        <input
  type="email"
          v-model="email"
          placeholder="juan.delacruz@msunaawan.edu.ph"
          pattern="^[A-Za-z0-9._%+-]+@msunaawan\.edu\.ph$"
          title="Please use your MSU Naawan email."
          required
        />

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
import axios from 'axios'

const router = useRouter()

const roles = [
  { name: 'RPS', desc: 'Research and Publication Services' },
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
const departmentOffice = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)

const handleRegister = async () => {
  // Password check
  if (password.value !== confirmPassword.value) {
    alert('Passwords do not match.')
    return
  }

  // University email validation
  const emailRegex = /^[A-Za-z0-9._%+-]+@msunaawan\.edu\.ph$/i

  if (!emailRegex.test(email.value.trim())) {
    alert('Only MSU Naawan email addresses (@msunaawan.edu.ph) are allowed.')
    return
  }

  const roleMap = {
    RPS: 'RPS_STAFF',
    OVCRIGE: 'OVCRIGE',
    REC: 'REC',
    OVCAF: 'OVCAF',
    OC: 'OC'
  }
  if (!email.value.toLowerCase().endsWith('@msunaawan.edu.ph')) {
  alert('Please use your official MSU Naawan email address.')
  return
}
  const payload = {
    name: name.value.trim(),
    email: email.value.trim().toLowerCase(),
    password: password.value,
    role: roleMap[selectedRole.value] || 'PROPONENT',
    departmentOffice: departmentOffice.value.trim()
  }

  try {
    await axios.post(
      'http://localhost:8081/api/users',
      payload
    )

    alert(
      'Account registration submitted successfully.\n\nPlease wait for RPS Admin approval before logging in.'
    )

    router.push('/login')
  } catch (error) {

    console.log(error)

    if (error.response?.status === 400) {
      alert(
        error.response.data?.message ||
        'Registration failed. Email already exists.'
      )
      return
    }

    console.warn('Backend unavailable. Saving locally...')

    const offlineUsers = JSON.parse(
      localStorage.getItem('offline_users') || '[]'
    )

    const exists = offlineUsers.some(
      user =>
        user.email.toLowerCase() ===
        email.value.toLowerCase()
    )

    if (exists) {
      alert('Registration failed. Email already exists.')
      return
    }

    const newUser = {
      id: Date.now(),
      name: name.value.trim(),
      email: email.value.trim().toLowerCase(),
      password: password.value,
      role: roleMap[selectedRole.value] || 'PROPONENT',
      departmentOffice: departmentOffice.value.trim(),
      status: 'PENDING',
      dateRegistered: new Date().toISOString()
    }

    offlineUsers.push(newUser)

    localStorage.setItem(
      'offline_users',
      JSON.stringify(offlineUsers)
    )

    alert(
      'Account registration submitted successfully.\n\nPlease wait for RPS Admin approval before logging in.'
    )

    router.push('/login')
  }
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
