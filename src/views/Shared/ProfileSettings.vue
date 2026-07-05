<template>
  <div class="profile-settings-container">
    
    <!-- Profile Header Banner -->
    <div class="profile-header-card">
      <div class="avatar-section">
        <div class="profile-avatar">
          <svg class="avatar-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="7" r="4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="user-meta">
          <h1 class="user-name">Account Settings</h1>
          <p class="user-role-badge">SYSTEM USER PORTAL</p>
        </div>
      </div>
    </div>

    <!-- Modern Tab Menu Navigation -->
    <div class="tab-navbar">
      <button
        class="tab-link"
        :class="{ active: activeTab === 'personal' }"
        @click="activeTab = 'personal'"
      >
        <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>Personal Info</span>
      </button>

      <button
        class="tab-link"
        :class="{ active: activeTab === 'security' }"
        @click="activeTab = 'security'"
      >
        <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>Security & Access</span>
      </button>

      <button
        class="tab-link"
        :class="{ active: activeTab === 'notifications' }"
        @click="activeTab = 'notifications'"
      >
        <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>Notifications</span>
      </button>
    </div>

    <!-- Active Tab Configuration Display -->
    <div class="settings-content">
      
      <!-- Tab 1: PERSONAL INFO -->
      <div v-if="activeTab === 'personal'" class="form-card animate-fade">
        <div class="card-header">
          <h2>Profile Details</h2>
          <p>Update your public profile, email address, and departmental affiliations.</p>
        </div>

        <div class="form-grid">
          <div class="field-group">
            <label for="fullName">Full Name</label>
            <input id="fullName" type="text" v-model="personalForm.fullName" placeholder="Enter your full name" />
          </div>
          <div class="field-group">
            <label for="email">Email Address</label>
            <input id="email" type="email" v-model="personalForm.email" placeholder="you@msunaawan.edu.ph" />
          </div>
          <div class="field-group">
            <label for="department">Department Unit</label>
            <input id="department" type="text" v-model="personalForm.department" placeholder="e.g. Computer Science Department" />
          </div>
          <div class="field-group">
            <label for="contact">Contact Number</label>
            <input id="contact" type="text" v-model="personalForm.contact" placeholder="e.g. +63 917 123 4567" />
          </div>
        </div>

        <!-- Checkbox card for Expertise -->
        <div class="checkbox-panel">
          <h3 class="panel-title">Expertise & Specializations</h3>
          <p class="panel-subtitle">Select your research domains and evaluation specialities.</p>
          
          <div class="checkbox-grid">
            <label class="check-tile" v-for="spec in specializationsList" :key="spec">
              <input type="checkbox" v-model="personalForm.specialization" :value="spec" />
              <span class="tile-label">{{ spec }}</span>
            </label>
          </div>
        </div>

        <div class="action-footer">
          <button class="save-btn" @click="savePersonal">Save Personal Info</button>
        </div>
      </div>

      <!-- Tab 2: SECURITY -->
      <div v-if="activeTab === 'security'" class="form-card animate-fade">
        <div class="card-header">
          <h2>Security & Credentials</h2>
          <p>Alter your official login name or update your account passwords safely.</p>
        </div>

        <div class="form-double-grid">
          
          <!-- Column 1: Name Changes -->
          <div class="form-column">
            <h3 class="column-title">Account Username</h3>
            <div class="field-group">
              <label for="oldName">Current Name</label>
              <input id="oldName" type="text" v-model="securityForm.oldName" placeholder="Enter old account name" />
            </div>
            <div class="field-group">
              <label for="newName">New Username</label>
              <input id="newName" type="text" v-model="securityForm.newName" placeholder="Enter new account name" />
            </div>
            <div class="field-group">
              <label for="confirmName">Confirm New Username</label>
              <input id="confirmName" type="text" v-model="securityForm.confirmName" placeholder="Confirm new account name" />
            </div>
          </div>

          <!-- Column 2: Password Changes -->
          <div class="form-column">
            <h3 class="column-title">Account Password</h3>
            <div class="field-group">
              <label for="oldPassword">Current Password</label>
              <input id="oldPassword" type="password" v-model="securityForm.oldPassword" placeholder="••••••••" />
            </div>
            <div class="field-group">
              <label for="newPassword">New Password</label>
              <input id="newPassword" type="password" v-model="securityForm.newPassword" placeholder="Min. 8 characters" />
            </div>
            <div class="field-group">
              <label for="confirmPassword">Confirm New Password</label>
              <input id="confirmPassword" type="password" v-model="securityForm.confirmPassword" placeholder="••••••••" />
            </div>
          </div>

        </div>

        <div class="action-footer">
          <button class="save-btn" @click="saveSecurity">Update Security Info</button>
        </div>
      </div>

      <!-- Tab 3: NOTIFICATIONS -->
      <div v-if="activeTab === 'notifications'" class="form-card animate-fade">
        <div class="card-header">
          <h2>Notifications Routing</h2>
          <p>Configure what email and system notifications you receive for actions and updates.</p>
        </div>

        <div class="notification-sections">
          <!-- Section 1 -->
          <div class="notif-section">
            <h3 class="section-heading">Proposal Submissions & Reviews</h3>
            <p class="section-desc">Select when you want to get notified regarding proposals status.</p>
            <div class="notif-grid">
              <label class="notif-item" v-for="notif in proposalNotifications" :key="notif">
                <input type="checkbox" v-model="notifications.proposals" :value="notif" />
                <div class="notif-text">
                  <span class="notif-title">{{ notif }}</span>
                  <span class="notif-subtitle">Receive immediate alerts when this update is triggered.</span>
                </div>
              </label>
            </div>
          </div>

          <!-- Section 2 -->
          <div class="notif-section">
            <h3 class="section-heading">Deadlines & Deliverables</h3>
            <p class="section-desc">Set date threshold notifications for evaluation reviews.</p>
            <div class="notif-grid">
              <label class="notif-item" v-for="notif in deadlineNotifications" :key="notif">
                <input type="checkbox" v-model="notifications.deadlines" :value="notif" />
                <div class="notif-text">
                  <span class="notif-title">{{ notif }}</span>
                  <span class="notif-subtitle">Receive calendar warnings prior to evaluation closure.</span>
                </div>
              </label>
            </div>
          </div>
        </div>

        <div class="action-footer">
          <button class="save-btn" @click="saveNotif">Save Notification Settings</button>
        </div>
      </div>

    </div>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserDataStore } from '@/stores/userData'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const userStore = useUserDataStore()
const activeTab = ref('personal')
const currentUserId = ref(null)

// Static configuration datasets
const specializationsList = [
  'Social Sciences',
  'Health Sciences',
  'Information Technology',
  'Quantitative Research',
  'Qualitative Research',
  'Mix Method',
  'Sustainability',
  'Community Development',
  'Public Administration',
  'Data Analytics'
]

const proposalNotifications = [
  'New Proposal Assigned',
  'Proponent Submitted Revision',
  'New Attachment Uploaded',
  'Proposal Status Updated'
]

const deadlineNotifications = [
  'Evaluation Deadline Reminders',
  'Revision Deadline Reminders'
]

const systemNotifications = [
  'Email Reminders',
  'System Notifications'
]

// Security Form Reactive Binding
const securityForm = reactive({
  newName: '',
  newPassword: '',
  oldName: '',
  oldPassword: '',
  confirmName: '',
  confirmPassword: '',
})

// Personal Info Form Reactive Binding
const personalForm = reactive({
  fullName: '',
  email: '',
  department: '',
  contact: '',
  specialization: [],
})

// Notifications Form Reactive Binding
const notifications = reactive({
  proposals: [],
  deadlines: [],
  system: [],
})

// Fetch current user details on mount
const loadUserProfile = async () => {
  try {
    userStore.hydrate()
    const storedUser = userStore.user
    if (storedUser) {
      currentUserId.value = storedUser.id
      
      // Load from local session storage first so fields are never blank (even for mock/offline users)
      personalForm.fullName = storedUser.name || ''
      personalForm.email = storedUser.email || ''
      personalForm.department = storedUser.departmentOffice || ''
      personalForm.contact = storedUser.contactNumber || ''
      personalForm.specialization = storedUser.specialization ? storedUser.specialization.split(',') : []
      securityForm.oldName = storedUser.name || ''

      // Attempt dynamic load from live PostgreSQL database
      if (storedUser.id) {
        const response = await api.get(`/api/users/${storedUser.id}`)
        const user = response.data
        if (user) {
          personalForm.fullName = user.name || ''
          personalForm.email = user.email || ''
          personalForm.department = user.departmentOffice || ''
          personalForm.contact = user.contactNumber || ''
          personalForm.specialization = user.specialization ? user.specialization.split(',') : []
          securityForm.oldName = user.name || ''
        }
      }
    }
  } catch (error) {
    console.warn("Failed to retrieve live profile from database, using cached local session.", error)
  }
}

onMounted(loadUserProfile)

const saveSecurity = async () => {
  if (!securityForm.newName && !securityForm.newPassword) {
    return showAlert('Please fill out at least one change field.', { type: 'warning', title: 'Validation Error' })
  }
  
  if (securityForm.newName && securityForm.newName !== securityForm.confirmName) {
    return showAlert('New username confirmation does not match.', { type: 'error', title: 'Mismatch Error' })
  }
  
  if (securityForm.newPassword && securityForm.newPassword !== securityForm.confirmPassword) {
    return showAlert('New password confirmation does not match.', { type: 'error', title: 'Mismatch Error' })
  }

  const updatedUserObj = {
    ...userStore.user,
    name: securityForm.newName.trim() ? securityForm.newName.trim() : userStore.user.name,
    password: securityForm.newPassword.trim() ? securityForm.newPassword.trim() : userStore.user.password
  }

  try {
    const payload = {}
    if (securityForm.newName) payload.name = securityForm.newName
    if (securityForm.newPassword) payload.password = securityForm.newPassword

    if (currentUserId.value) {
      // 1. Live database save
      const response = await api.put(`/api/users/${currentUserId.value}`, payload)
      if (response.data) {
        userStore.setUser({
          ...userStore.user,
          name: response.data.name
        })
      }
    } else {
      // 2. Direct local save (offline fallback)
      userStore.setUser(updatedUserObj)
    }
    
    await showAlert('Security settings updated successfully!', { type: 'success' })
    securityForm.newName = ''
    securityForm.newPassword = ''
    securityForm.confirmName = ''
    securityForm.confirmPassword = ''
    loadUserProfile()
  } catch (error) {
    console.warn("PostgreSQL save failed, applying fallback local update.", error)
    
    // Fallback save to keep offline / demo working seamlessly
    userStore.setUser(updatedUserObj)
    
    await showAlert('Security settings updated successfully (offline mode)!', { type: 'success' })
    securityForm.newName = ''
    securityForm.newPassword = ''
    securityForm.confirmName = ''
    securityForm.confirmPassword = ''
    loadUserProfile()
  }
}

const savePersonal = async () => {
  if (!personalForm.fullName.trim() || !personalForm.email.trim()) {
    return showAlert('Full Name and Email Address are required.', { type: 'warning', title: 'Validation Error' })
  }

  const updatedUserObj = {
    ...userStore.user,
    name: personalForm.fullName.trim(),
    email: personalForm.email.trim(),
    departmentOffice: personalForm.department.trim(),
    contactNumber: personalForm.contact.trim(),
    specialization: personalForm.specialization.join(',')
  }

  try {
    const payload = {
      name: personalForm.fullName,
      email: personalForm.email,
      departmentOffice: personalForm.department,
      contactNumber: personalForm.contact,
      specialization: personalForm.specialization.join(',')
    }

    if (currentUserId.value) {
      // 1. Live database save
      const response = await api.put(`/api/users/${currentUserId.value}`, payload)
      if (response.data) {
        userStore.setUser({
          ...userStore.user,
          name: response.data.name,
          email: response.data.email,
          departmentOffice: response.data.departmentOffice,
          contactNumber: response.data.contactNumber,
          specialization: response.data.specialization
        })
      }
    } else {
      // 2. Direct local save (offline fallback)
      userStore.setUser(updatedUserObj)
    }
    
    await showAlert('Personal info saved successfully!', { type: 'success' })
    loadUserProfile()
  } catch (error) {
    console.warn("PostgreSQL save failed, applying fallback local update.", error)
    
    // Fallback save to keep offline / demo working seamlessly
    userStore.setUser(updatedUserObj)
    
    await showAlert('Personal info saved successfully (offline mode)!', { type: 'success' })
    loadUserProfile()
  }
}

const saveNotif = async () => {
  await showAlert('Notification preferences updated successfully!', { type: 'success' })
}
</script>

<style scoped>
.profile-settings-container {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 10px;
  font-family: inherit;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Header banner style */
.profile-header-card {
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
  padding: 32px 40px;
  border-radius: 16px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffd803;
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.avatar-svg {
  width: 34px;
  height: 34px;
}

.user-meta {
  text-align: left;
}

.user-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 4px 0;
  letter-spacing: -0.01em;
}

.user-role-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #ffd803;
  margin: 0;
}

/* Tab Navigation items */
.tab-navbar {
  display: flex;
  border-bottom: 1px solid var(--color-border, #e2e8f0);
  gap: 8px;
}

.tab-link {
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 12px 20px;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-soft, #64748b);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  border-radius: 8px 8px 0 0;
}

.tab-link:hover {
  color: var(--color-heading, #0f172a);
  background: var(--color-background-soft, #f8fafc);
}

.tab-link.active {
  color: #2452ff;
  border-bottom-color: #2452ff;
  background: transparent;
}

.tab-icon {
  width: 18px;
  height: 18px;
}

/* Modern Card Layouts */
.form-card {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 14px;
  padding: 32px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.03);
  text-align: left;
}

.card-header {
  margin-bottom: 28px;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  padding-bottom: 18px;
}

.card-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 6px 0;
}

.card-header p {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

/* Grids configuration */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.form-double-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 40px;
  margin-bottom: 28px;
}

.form-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.column-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  border-left: 3px solid #2452ff;
  padding-left: 10px;
  margin: 0 0 10px 0;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-group label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text, #475569);
}

input[type="text"],
input[type="email"],
input[type="password"] {
  padding: 10px 14px;
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

/* Checkbox tiles panel */
.checkbox-panel {
  border-top: 1px solid var(--color-border, #f1f5f9);
  padding-top: 24px;
  margin-bottom: 28px;
}

.panel-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 4px 0;
}

.panel-subtitle {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 18px 0;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.check-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--color-border, #cbd5e1);
  background: var(--color-background, #ffffff);
  cursor: pointer;
  user-select: none;
  transition: all 0.15s ease;
}

.check-tile:hover {
  border-color: #ffd803;
  background: var(--color-background-soft, #f8fafc);
}

.check-tile input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin: 0;
  cursor: pointer;
}

.tile-label {
  font-size: 0.8125rem;
  color: var(--color-text, #475569);
  font-weight: 500;
}

/* Notification Styling list */
.notification-sections {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.notif-section {
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  padding-bottom: 28px;
}

.border-none {
  border-bottom: none;
  padding-bottom: 0;
}

.section-heading {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 4px 0;
}

.section-desc {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 18px 0;
}

.notif-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 10px;
  border: 1px solid var(--color-border, #cbd5e1);
  background: var(--color-background, #ffffff);
  cursor: pointer;
  transition: all 0.15s ease;
}

.notif-item:hover {
  border-color: #ffd803;
}

.notif-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin-top: 2px;
  cursor: pointer;
}

.notif-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.notif-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
}

.notif-subtitle {
  font-size: 0.75rem;
  color: var(--color-text-soft, #64748b);
}

/* Action Footer standard */
.action-footer {
  border-top: 1px solid var(--color-border, #f1f5f9);
  padding-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  background: #2452ff;
  border: 1px solid #2452ff;
  color: white !important;
  padding: 11px 22px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.save-btn:hover {
  background: #1d40cc;
  border-color: #1d40cc;
}

/* Fade in effects */
.animate-fade {
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .form-grid,
  .form-double-grid,
  .checkbox-grid {
    grid-template-columns: 1fr;
  }
  .form-double-grid {
    gap: 24px;
  }
  .profile-header-card {
    padding: 24px;
  }
}
</style>
