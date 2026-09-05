<template>
  <div class="login-container">
    <div class="login-card">
      
      <!-- Left Panel: Branding & High-Contrast Visuals -->
      <div class="left-panel">
        <div class="glow-effect"></div>
        <div class="branding-content">
          <img src="@/assets/images/logo.png" class="school-logo" alt="MSU Naawan Logo" />
          <h1 class="title">
            Research <span class="highlight">Project</span><br />
            Information System
          </h1>
          <p class="school-name">
            RESEARCH AND PUBLICATION SERVICES<br />
            <span class="sub-school">Mindanao State University at Naawan</span>
          </p>
        </div>
      </div>

      <!-- Right Panel: Role Select and User Verification Form -->
      <div class="right-panel">
        <div class="right-content">
          <div class="header-section">
            <h2 class="welcome">Welcome Back</h2>
            <p class="subtitle">Please select your active role and sign in to access your dashboard.</p>
          </div>

          <!-- Role Selection Cards Grid -->
          <div class="role-grid">
            <div
              v-for="role in roles"
              :key="role.name"
              class="role-card"
              :class="{ active: selectedRole === role.value }"
              @click="selectedRole = role.value"
            >
              <div class="role-icon-placeholder">
                <svg v-if="role.value === UserRole.PROPONENT" class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 009 11V5a2 2 0 00-2-2H4a2 2 0 00-2 2v6a13.91 13.91 0 003.44 9.16l.054.09m3.44-2.04A13.916 13.916 0 009 11M9 11h6" />
                  <path d="M12 11c0 3.517 1.009 6.799 2.753 9.571M12 11V5a2 2 0 012-2h3a2 2 0 012 2v6c0 3.14-.82 6.09-2.258 8.641" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else-if="role.value === UserRole.RPS_STAFF" class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 00-3-3.87" />
                  <path d="M16 3.13a4 4 0 010 7.75" />
                </svg>
                <svg v-else-if="role.value === UserRole.OVCRIGE" class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
                <svg v-else-if="role.value === UserRole.REC" class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <svg v-else-if="role.value === UserRole.OVCAF" class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                  <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16" />
                </svg>
                <svg v-else class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16M12 11v.01" />
                </svg>
              </div>
              <div class="role-info">
                <h3>{{ role.name }}</h3>
                <p>{{ role.desc }}</p>
              </div>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="error-banner">
            <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{{ errorMessage }}</span>
          </div>

          <!-- Inputs Form -->
          <form class="login-form" @submit.prevent="handleLogin">
            
            <div class="input-group">
              <label for="email">Email Address</label>
              <input 
                id="email" 
                type="email" 
                v-model="email" 
                placeholder="you@msunaawan.edu.ph" 
                required
              />
            </div>

            <div class="input-group">
              <div class="label-row">
                <label for="password">Password</label>
                <a href="/register" class="forgot-link">Forgot password?</a>
              </div>
              <input 
                id="password" 
                type="password" 
                v-model="password" 
                placeholder="••••••••" 
                required
              />
            </div>

            <div class="form-actions">
              <div class="remember-me-container">
                <input type="checkbox" id="remember" class="remember-input" />
                <label for="remember" class="remember-label">Remember me on this computer</label>
              </div>
            </div>

            <button class="login-btn" type="submit">Sign In</button>

            <p class="signup">Don't have an account? <a href="/create-account" class="signup-link">Sign up</a></p>
          </form>
        </div>
      </div>
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
  { name: 'Proponent', desc: 'Faculty Submitter', value: UserRole.PROPONENT, next: '/proponent-dashboard' },
  { name: 'ORPS', desc: 'Technical & Admin Services', value: UserRole.RPS_STAFF, next: '/proponent-dashboard' },
  { name: 'OVCRIGE', desc: 'Research Vice Chancellor', value: UserRole.OVCRIGE, next: '/proponent-dashboard'  },
  { name: 'REC', desc: 'Evaluation Committee', value: UserRole.REC, next: '/proponent-dashboard'  },
  { name: 'OVCAF', desc: 'Finance Vice Chancellor', value: UserRole.OVCAF, next: '/proponent-dashboard'  },
  { name: 'Chancellor (OC)', desc: 'Office of the Chancellor', value: UserRole.OC, next: '/proponent-dashboard'  },
]

const selectedRole = ref(UserRole.RPS_STAFF)

const handleLogin = async () => {
  try {
    errorMessage.value = ''

    const isLoggedIn = await userStore.login(
      email.value,
      password.value,
      selectedRole.value
    )

    if (isLoggedIn) {
      router.push('/home')
    } else {
      errorMessage.value = 'Invalid email, password, or role.'
    }
  } catch (err) {
    errorMessage.value = err.message || 'Invalid email, password, or role.'
  }
}

const handleGoogleLogin = async (response) => {
  try {
    errorMessage.value = ''
    
    const isLoggedIn = await userStore.googleLogin(
      response.credential,
      selectedRole.value
    )

    if (isLoggedIn) {
      router.push('/home')
    } else {
      errorMessage.value = 'Google Login failed.'
    }
  } catch (err) {
    errorMessage.value = err.message || 'Google Login failed.'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: stretch;
  justify-content: center;
  height: 100vh;
  width: 100%;
  background: var(--color-background, #f3f4f6);
  padding: 0;
  overflow: hidden;
}

.login-card {
  display: flex;
  width: 100%;
  height: 100vh;
  background: var(--color-card-background, #ffffff);
  border: none;
  border-radius: 0;
  overflow: hidden;
  box-shadow: none;
}

/* Left Panel Style (Branding) - Fully Vertical */
.left-panel {
  position: relative;
  width: 40%;
  height: 100vh;
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
  padding: 64px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.glow-effect {
  position: absolute;
  top: -10%;
  left: -10%;
  width: 120%;
  height: 120%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 60%);
  pointer-events: none;
}

.branding-content {
  position: relative;
  z-index: 2;
  max-width: 440px;
  margin: 0 auto;
}

.school-logo {
  width: 110px;
  height: auto;
  margin-bottom: 32px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin: 0 0 28px 0;
  letter-spacing: -0.025em;
  text-align: left;
}

.title .highlight {
  color: #ffd803;
}

.school-name {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.6;
  opacity: 0.9;
  letter-spacing: 0.05em;
  margin: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  padding-top: 28px;
  text-align: left;
}

.sub-school {
  font-size: 0.75rem;
  font-weight: 400;
  opacity: 0.8;
  letter-spacing: 0;
}

/* Right Panel Style (Form Area) - Fully Vertical & Scrollable */
.right-panel {
  width: 60%;
  height: 100vh;
  padding: 64px 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow-y: auto;
}

.right-content {
  width: 100%;
  max-width: 540px;
}

.header-section {
  margin-bottom: 32px;
  text-align: left;
}

.welcome {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 0.9375rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

/* Role Selection Grid */
.role-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 32px;
}

.role-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--color-border, #e2e8f0);
  background: var(--color-card-background, #ffffff);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.role-card:hover {
  border-color: #ffd803;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.role-card.active {
  background: #ffd803;
  border-color: #ffd803;
  box-shadow: 0 4px 14px rgba(255, 216, 3, 0.25);
}

.role-icon-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-text, #475569);
  transition: all 0.2s ease;
}

.role-card.active .role-icon-placeholder {
  background: rgba(0, 0, 0, 0.08);
  color: #000000;
}

.role-icon {
  width: 18px;
  height: 18px;
}

.role-info {
  text-align: left;
}

.role-info h3 {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 2px 0;
  transition: color 0.2s ease;
}

.role-card.active .role-info h3 {
  color: #000000;
}

.role-info p {
  font-size: 0.6875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
  line-height: 1.2;
}

.role-card.active .role-info p {
  color: rgba(0, 0, 0, 0.7);
}

/* Login Form Styles */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-group label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text, #475569);
}

.forgot-link {
  font-size: 0.75rem;
  font-weight: 500;
  color: #2452ff;
  text-decoration: none;
  transition: color 0.15s ease;
}

.forgot-link:hover {
  color: #1d40cc;
  text-decoration: underline;
}

input[type="email"],
input[type="password"] {
  padding: 12px 14px;
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: all 0.2s ease;
  background: var(--color-background, #ffffff);
  color: var(--color-text, #1f2937);
  width: 100%;
}

input:focus {
  outline: none;
  border-color: #2452ff;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.15);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  user-select: none;
}

.remember-me-container {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  gap: 8px !important;
  width: auto !important;
  margin: 0 !important;
}

.remember-input {
  width: 16px !important;
  height: 16px !important;
  margin: 0 !important;
  cursor: pointer;
}

.remember-label {
  font-size: 0.8125rem !important;
  color: var(--color-text, #475569) !important;
  margin: 0 !important;
  padding: 0 !important;
  width: auto !important;
  display: inline-block !important;
  cursor: pointer;
}

.login-btn {
  background: #2452ff;
  color: white;
  padding: 13px;
  border: none;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
  width: 100%;
}

.login-btn:hover {
  background: #1d40cc;
}

.google-login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10px;
}

.or-divider {
  width: 100%;
  text-align: center;
  border-bottom: 1px solid var(--color-border, #e2e8f0);
  line-height: 0.1em;
  margin: 10px 0 16px;
}

.or-divider span {
  background: var(--color-card-background, #ffffff);
  padding: 0 10px;
  color: var(--color-text-soft, #94a3b8);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  color: #991b1b;
  font-size: 0.8125rem;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.signup {
  margin: 12px 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  text-align: center;
}

.signup-link {
  color: #2452ff;
  font-weight: 500;
  text-decoration: none;
}

.signup-link:hover {
  text-decoration: underline;
}

/* Responsive breakpoint styling */
@media (max-width: 960px) {
  .login-container {
    height: auto;
    min-height: 100vh;
    overflow-y: auto;
  }
  .login-card {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }
  .left-panel {
    width: 100%;
    height: auto;
    padding: 40px;
    align-items: center;
    border-right: none;
    border-bottom: 1px solid var(--color-border, #e2e8f0);
  }
  .school-logo {
    margin-bottom: 16px;
  }
  .title {
    font-size: 1.875rem;
    text-align: center;
    margin-bottom: 16px;
  }
  .school-name {
    text-align: center;
    padding-top: 16px;
  }
  .right-panel {
    width: 100%;
    height: auto;
    padding: 40px 24px;
  }
  .role-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 540px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
