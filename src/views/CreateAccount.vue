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

    <!-- RIGHT PANEL -->
    <div class="right-panel">

     <button class="back-btn" @click="router.push('/login')">
      ← Back to Login
         </button>
         
      <h2 class="welcome">CREATE PROPONENT ACCOUNT</h2>

      <p class="subtitle">
        Register as a Proponent to submit and manage research project proposals.
      </p>

      <!-- FORM -->
      <form class="register-form" @submit.prevent="handleRegister">
        <label>Name</label>
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

        <!-- TERMS -->
        <div class="terms">
          <input type="checkbox" v-model="agree" />

          <label>
            I have read and agree to the
            <span class="terms-link" @click.prevent="showTermsModal = true">
              Terms and Conditions
            </span>
            and
            <span class="terms-link" @click.prevent="showTermsModal = true">
              Privacy Policy
            </span>.
          </label>
        </div>

        <button class="register-btn" type="submit">
          Create Proponent Account
        </button>
      </form>
    </div>

    <!-- TERMS & CONDITIONS MODAL -->
    <div v-if="showTermsModal" class="modal-overlay">
      <div class="modal-box">

        <h2>TERMS AND CONDITIONS</h2>

        <div class="modal-content">

          <p>
            Welcome to the Research Project Information System of
            Mindanao State University at Naawan.
          </p>

          <h3>TERMS AND CONDITIONS</h3>

          <ol>
            <li>
              Only official <strong>@msunaawan.edu.ph</strong> email addresses
              may be used to register.
            </li>

            <li>
              You are responsible for keeping your account credentials secure.
            </li>

            <li>
              All information you provide must be accurate and truthful.
            </li>

            <li>
              You are responsible for all research proposals submitted through
              your account.
            </li>

            <li>
              Any misuse of the system may result in suspension or termination
              of your account.
            </li>
          </ol>

          <h3>PRIVACY POLICY</h3>

          <p>
            The system collects your name, university email, department, and
            other information solely for research proposal management.
          </p>

          <p>
            Your personal information will only be accessed by authorized
            university personnel and handled in accordance with the
            <strong>Data Privacy Act of 2012 (Republic Act No. 10173).</strong>
          </p>

        </div>

        <button
          class="close-btn"
          @click="showTermsModal = false"
        >
          Close
        </button>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const name = ref('')
const idNumber = ref('')
const departmentOffice = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)

// NEW
const showTermsModal = ref(false)

const handleRegister = async () => {
  // Must agree first
  if (!agree.value) {
    alert('Please read and agree to the Terms and Conditions and Privacy Policy.')
    return
  }

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

  const payload = {
    name: name.value.trim(),
    email: email.value.trim().toLowerCase(),
    password: password.value,
    role: 'PROPONENT',
    departmentOffice: departmentOffice.value.trim()
  }

  try {
    await axios.post(
      'http://localhost:8081/api/users',
      payload
    )

    alert('Account created successfully! You can now log in.')

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
      role: 'PROPONENT',
      departmentOffice: departmentOffice.value.trim(),
      status: 'ACTIVE',
      dateRegistered: new Date().toISOString()
    }

    offlineUsers.push(newUser)

    localStorage.setItem(
      'offline_users',
      JSON.stringify(offlineUsers)
    )

    alert('Account created successfully! You can now log in.')

    router.push('/login')
  }
}
</script>

<style scoped>

.back-btn {
  background: transparent;
  border: none;
  color: #2452ff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 18px;
  padding: 0;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #163dbd;
  text-decoration: underline;
}
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

/* TERMS SECTION */
.terms {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 15px 0;
  font-size: 14px;
}

.terms input {
  margin-top: 4px;
}

.terms-link {
  color: #2452ff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}

.terms-link:hover {
  color: #163dbd;
}

/* MODAL */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .55);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-box {
  background: white;
  width: 650px;
  max-width: 90%;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 10px 30px rgba(0,0,0,.2);
}

.modal-box h2 {
  margin-bottom: 15px;
  color: #1f1a3d;
}

.modal-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.8;
  color: #444;
  text-align: justify;   /* Justify all paragraphs */
  padding-right: 12px;
}

.modal-content h3 {
  margin-top: 22px;
  margin-bottom: 12px;
  color: #1f1a3d;
  font-size: 22px;
  font-weight: 800;
  text-align: center;
}

.modal-content p {
  margin-bottom: 14px;
  text-align: justify;
}

.modal-content li {
  margin-bottom: 10px;
  text-align: justify;
} 

.modal-content ol {
  padding-left: 20px;
}

.close-btn {
  margin-top: 20px;
  width: 100%;
  padding: 12px;
  background: #2452ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}

.close-btn:hover {
  background: #1d43d8;
}
</style>
