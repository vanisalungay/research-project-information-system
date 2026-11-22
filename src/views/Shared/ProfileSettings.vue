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

      <button
        class="tab-link"
        :class="{ active: activeTab === 'signature' }"
        @click="activeTab = 'signature'"
      >
        <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 17.5c2.6 0 3.4-9.5 6-9.5s2.4 7 4.6 7 1.9-3 3.4-3" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M4 21h16" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>Electronic Signature</span>
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
            <input
              id="fullName"
              type="text"
              v-model="personalForm.fullName"
              placeholder="Juan Dela Cruz"
              @blur="personalForm.fullName = formatPersonName(personalForm.fullName)"
            />
            <FullNameHint />
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
          <!-- Display-only: the Dean's endorsement scope is assigned by the RPS
               Administrator in Dean Role Management. -->
          <div v-if="isDean" class="field-group">
            <label for="assignedCollege">Assigned College / Department</label>
            <input
              id="assignedCollege"
              type="text"
              :value="assignedCollege || 'Not assigned'"
              readonly
              aria-readonly="true"
            />
            <p class="field-note">Assigned by the RPS Administrator and not editable here.</p>
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
          <button class="save-btn btn-primary" @click="savePersonal">Save Personal Info</button>
        </div>
      </div>

      <!-- Tab 2: SECURITY -->
      <div v-if="activeTab === 'security'" class="form-card animate-fade">
        <div class="card-header">
          <h2>Security & Credentials</h2>
          <p>Update your account's full name or change your account password safely.</p>
        </div>

        <div class="form-double-grid">

          <!-- Column 1: Name Changes -->
          <div class="form-column">
            <h3 class="column-title">Account Full Name</h3>
            <div class="field-group">
              <label for="oldName">Current Full Name</label>
              <input
                id="oldName"
                type="text"
                v-model="securityForm.oldName"
                placeholder="Current account name"
                @blur="securityForm.oldName = formatPersonName(securityForm.oldName)"
              />
            </div>
            <div class="field-group">
              <label for="newName">New Full Name</label>
              <input
                id="newName"
                type="text"
                v-model="securityForm.newName"
                placeholder="Juan Dela Cruz"
                @blur="securityForm.newName = formatPersonName(securityForm.newName)"
              />
              <FullNameHint />
            </div>
            <div class="field-group">
              <label for="confirmName">Confirm New Full Name</label>
              <input
                id="confirmName"
                type="text"
                v-model="securityForm.confirmName"
                placeholder="Repeat the new full name"
                @blur="securityForm.confirmName = formatPersonName(securityForm.confirmName)"
              />
            </div>
          </div>

          <!-- Column 2: Password Changes -->
          <div class="form-column">
            <h3 class="column-title">Account Password</h3>
            <div class="field-group">
              <label for="oldPassword">Current Password</label>
              <PasswordInput
                id="oldPassword"
                v-model="securityForm.oldPassword"
                placeholder="••••••••"
                autocomplete="current-password"
                aria-describedby="oldPassword-hint"
              />
              <PasswordFieldHint id="oldPassword-hint" kind="current" />
            </div>

            <PasswordRequirements
              id="password-requirements"
              :password="securityForm.newPassword"
              :confirm="securityForm.confirmPassword"
            />

            <div class="field-group">
              <label for="newPassword">New Password</label>
              <PasswordInput
                id="newPassword"
                v-model="securityForm.newPassword"
                placeholder="Create a new password"
                autocomplete="new-password"
                aria-describedby="newPassword-hint password-requirements"
              />
              <PasswordFieldHint id="newPassword-hint" kind="new" />
            </div>
            <div class="field-group">
              <label for="confirmPassword">Confirm New Password</label>
              <PasswordInput
                id="confirmPassword"
                v-model="securityForm.confirmPassword"
                placeholder="••••••••"
                autocomplete="new-password"
                aria-describedby="confirmPassword-hint"
              />
              <PasswordFieldHint id="confirmPassword-hint" kind="confirm" />
            </div>
          </div>

        </div>

        <div class="action-footer">
          <button
            class="save-btn btn-primary"
            :disabled="securitySubmitDisabled || securitySaving"
            @click="saveSecurity"
          >
            {{ securitySaving ? 'Saving...' : 'Update Security Info' }}
          </button>
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
            <p class="section-desc">Select which optional proposal updates you want to receive.</p>
            <div class="notif-grid">
              <label class="notif-item" v-for="option in proposalOptions" :key="option.key">
                <input type="checkbox" v-model="notifications.proposals" :value="option.key" />
                <div class="notif-text">
                  <span class="notif-title">{{ option.label }}</span>
                  <span class="notif-subtitle">{{ option.description }}</span>
                </div>
              </label>
            </div>
            <p class="section-desc notif-mandatory-note">
              Core workflow alerts — review requests, returns, rejections, approvals,
              financial actions and endorsement requests — are always delivered.
            </p>
          </div>

          <!-- Section 2 -->
          <div class="notif-section">
            <h3 class="section-heading">Deadlines & Deliverables</h3>
            <p class="section-desc">Set date reminders for deadlines that exist in RPIS.</p>
            <div class="notif-grid">
              <label
                class="notif-item"
                v-for="option in deadlineOptions"
                :key="option.key"
                :class="{ 'notif-item--unavailable': !option.available }"
              >
                <input
                  type="checkbox"
                  v-model="notifications.deadlines"
                  :value="option.key"
                  :disabled="!option.available"
                />
                <div class="notif-text">
                  <span class="notif-title">{{ option.label }}</span>
                  <span class="notif-subtitle">{{ option.description }}</span>
                </div>
              </label>
            </div>
          </div>
        </div>

        <div class="action-footer">
          <button class="save-btn btn-primary" @click="saveNotif">Save Notification Settings</button>
        </div>
      </div>

      <!-- Tab 4: ELECTRONIC SIGNATURE -->
      <div v-if="activeTab === 'signature'" class="form-card animate-fade">
        <ElectronicSignaturePanel />
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
import { computed, ref, reactive, onMounted } from 'vue'
import { UserRole, useUserDataStore } from '@/stores/userData'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import ElectronicSignaturePanel from '@/components/ElectronicSignaturePanel.vue'
import PasswordInput from '@/components/PasswordInput.vue'
import PasswordRequirements from '@/components/PasswordRequirements.vue'
import PasswordFieldHint from '@/components/PasswordFieldHint.vue'
import FullNameHint from '@/components/FullNameHint.vue'
import { useDialog } from '@/composables/useDialog'
import { firstLastNameError, formatPersonName } from '@/utils/personName'
import { passwordFormReady, passwordPolicyMessage } from '@/utils/passwordPolicy'
import {
  DEADLINE_OPTIONS,
  PROPOSAL_OPTIONS,
  initialDeadlineSelection,
  initialProposalSelection,
} from '@/utils/notificationPreferences.js'

const { dialogState, showAlert, showConfirm } = useDialog()

const userStore = useUserDataStore()
const activeTab = ref('personal')
const currentUserId = ref(null)

// A Dean's college / department is the endorsement scope assigned by the RPS
// Administrator. It is shown read-only here; the administrator owns that field.
// The signed-in session carries it, and the account fetched below refreshes it, so
// a reassignment made by the administrator is reflected rather than a stale copy.
const isDean = computed(() => userStore.user?.role === UserRole.DEAN)
const assignedCollege = ref(userStore.user?.assignedCollege || '')

// Specializations offered by the Manage Profile form. This is a fixed list of
// research areas, not user data.
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

// The Notification Routing options a user can subscribe to. The visible labels and
// stable keys live in one module so a copy edit cannot silently change what a saved
// preference means.
const proposalOptions = PROPOSAL_OPTIONS
const deadlineOptions = DEADLINE_OPTIONS

// Security Form Reactive Binding
const securityForm = reactive({
  newName: '',
  newPassword: '',
  oldName: '',
  oldPassword: '',
  confirmName: '',
  confirmPassword: '',
})

const securitySaving = ref(false)

// A password change is an all-or-nothing block: once any of the three password
// fields has been touched, the footer button waits for a current password, a new
// password that satisfies every requirement, and a matching confirmation. Editing
// only the name leaves the button available as before.
const passwordFieldsTouched = computed(() =>
  Boolean(securityForm.oldPassword || securityForm.newPassword || securityForm.confirmPassword),
)
const passwordFormValid = computed(
  () =>
    securityForm.oldPassword !== '' &&
    passwordFormReady(securityForm.newPassword, securityForm.confirmPassword),
)
const securitySubmitDisabled = computed(
  () => passwordFieldsTouched.value && !passwordFormValid.value,
)

// Personal Info Form Reactive Binding
// The name currently stored on the account. A legacy "Last, First" stored name is
// preserved as-is: only a newly entered name has to follow the First Name Last Name rule.
const loadedName = ref('')

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
})

// Fetch current user details on mount
const loadUserProfile = async () => {
  try {
    userStore.hydrate()
    const storedUser = userStore.user
    if (storedUser) {
      currentUserId.value = storedUser.id

      // Prefill from the signed-in session so the form is never blank while the
      // request is in flight. The authoritative values are then read from the
      // backend below.
      personalForm.fullName = storedUser.name || ''
      loadedName.value = storedUser.name || ''
      personalForm.email = storedUser.email || ''
      personalForm.department = storedUser.departmentOffice || ''
      personalForm.contact = storedUser.contactNumber || ''
      personalForm.specialization = storedUser.specialization ? storedUser.specialization.split(',') : []
      securityForm.oldName = storedUser.name || ''
      assignedCollege.value = storedUser.assignedCollege || ''

      // Load the account from PostgreSQL.
      if (storedUser.id) {
        const response = await api.get(`/api/users/${storedUser.id}`)
        const user = response.data
        if (user) {
          personalForm.fullName = user.name || ''
          loadedName.value = user.name || ''
          personalForm.email = user.email || ''
          personalForm.department = user.departmentOffice || ''
          personalForm.contact = user.contactNumber || ''
          personalForm.specialization = user.specialization ? user.specialization.split(',') : []
          securityForm.oldName = user.name || ''
          assignedCollege.value = user.assignedCollege || ''

          // Notification Routing: the selection stored on the account. Legacy labels
          // are normalized to stable keys, and an account that never saved the tab
          // shows the effective defaults (optional proposal alerts on, reminders off).
          notifications.proposals = initialProposalSelection(user)
          notifications.deadlines = initialDeadlineSelection(user)
        }
      }
    }
  } catch (error) {
    // The stored values stay on screen, but the reason is reported instead of being
    // swallowed: the previous version silently treated a failed load as "offline
    // mode" and continued as if the backend had answered.
    console.error('Failed to load the account from the backend:', error)
    await showAlert('Your profile could not be loaded. Please try again.', {
      type: 'error',
      title: 'Load Failed',
    })
  }
}

onMounted(loadUserProfile)

const clearPasswordFields = () => {
  securityForm.oldPassword = ''
  securityForm.newPassword = ''
  securityForm.confirmPassword = ''
}

const saveSecurity = async () => {
  if (securitySaving.value) return

  // Format both name entries before comparing them, so a case-only difference is not
  // reported as a mismatch.
  securityForm.newName = formatPersonName(securityForm.newName)
  securityForm.confirmName = formatPersonName(securityForm.confirmName)

  const wantsPasswordChange = passwordFieldsTouched.value

  if (!securityForm.newName && !wantsPasswordChange) {
    return showAlert('Please fill out at least one change field.', { type: 'warning', title: 'Validation Error' })
  }

  // A newly entered account name is First Name first, then Last Name - the
  // "Last, First" order is reserved for names inside proposal documents.
  if (securityForm.newName) {
    const nameError = firstLastNameError(securityForm.newName)
    if (nameError) {
      return showAlert(nameError, { type: 'error', title: 'Name Required' })
    }
    if (securityForm.newName !== securityForm.confirmName) {
      return showAlert('New username confirmation does not match.', { type: 'error', title: 'Mismatch Error' })
    }
  }

  // The password block is validated as a unit, against the same rules the backend
  // enforces. Nothing is sent until the current password is present, every
  // requirement is met and the confirmation matches.
  if (wantsPasswordChange) {
    if (securityForm.oldPassword === '') {
      return showAlert('Enter your current password to choose a new one.', { type: 'warning', title: 'Validation Error' })
    }
    const policyMessage = passwordPolicyMessage(securityForm.newPassword)
    if (policyMessage) {
      return showAlert(policyMessage, { type: 'error', title: 'Password Requirements' })
    }
    if (securityForm.newPassword !== securityForm.confirmPassword) {
      return showAlert('New password confirmation does not match.', { type: 'error', title: 'Mismatch Error' })
    }
  }

  let passwordChanged = false
  try {
    securitySaving.value = true

    if (!currentUserId.value) {
      throw new Error('No signed-in account was found.')
    }

    // A password change goes through the one endpoint that verifies the current
    // password and re-validates the policy on the server. It is attempted first, so
    // a wrong current password never leaves the name half-updated.
    if (wantsPasswordChange) {
      await api.put('/api/users/change-password', {
        currentPassword: securityForm.oldPassword,
        newPassword: securityForm.newPassword,
        confirmPassword: securityForm.confirmPassword,
      })
      passwordChanged = true
      clearPasswordFields()
    }

    if (securityForm.newName) {
      const response = await api.put(`/api/users/${currentUserId.value}`, {
        name: formatPersonName(securityForm.newName),
      })
      if (response.data) {
        userStore.setUser({
          ...userStore.user,
          name: response.data.name,
        })
      }
    }

    await showAlert('Security settings updated successfully!', { type: 'success' })
    securityForm.newName = ''
    securityForm.confirmName = ''
    clearPasswordFields()
    loadUserProfile()
  } catch (error) {
    // The change is reported as failed. The previous version wrote the new name and
    // password into the browser's local session instead and then said "Security
    // settings updated successfully (offline mode)!" - the account in the database
    // still had the old credentials, so the next login with the "new" password
    // failed.
    // Never log the error object: its config carries the password request body.
    console.error('Could not update the security settings.', error?.response?.status ?? 'no response')
    const body = error?.response?.data
    const reason = (typeof body === 'string' && body.trim()) || body?.error
    if (passwordChanged) {
      await showAlert(
        reason || 'Your password was changed, but the name could not be updated. Please try again.',
        { type: 'warning', title: 'Partly Updated' },
      )
      loadUserProfile()
      return
    }
    await showAlert(
      reason || 'Your security settings could not be updated. Please check your connection and try again.',
      { type: 'error', title: 'Update Failed' },
    )
  } finally {
    securitySaving.value = false
  }
}

const savePersonal = async () => {
  if (!personalForm.fullName.trim() || !personalForm.email.trim()) {
    return showAlert('Full Name and Email Address are required.', { type: 'warning', title: 'Validation Error' })
  }

  // Only a name that was actually edited is re-validated; an unchanged legacy
  // stored name must not block saving the other personal fields.
  personalForm.fullName = formatPersonName(personalForm.fullName)
  const nameChanged = personalForm.fullName.trim() !== loadedName.value.trim()
  if (nameChanged) {
    const nameError = firstLastNameError(personalForm.fullName)
    if (nameError) {
      return showAlert(nameError, { type: 'error', title: 'Name Required' })
    }
  }

  try {
    const payload = {
      name: formatPersonName(personalForm.fullName),
      email: personalForm.email,
      departmentOffice: personalForm.department,
      contactNumber: personalForm.contact,
      specialization: personalForm.specialization.join(',')
    }

    if (!currentUserId.value) {
      throw new Error('No signed-in account was found.')
    }

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

    await showAlert('Personal info saved successfully!', { type: 'success' })
    loadUserProfile()
  } catch (error) {
    // No local "offline mode" success: an unsaved profile is reported as unsaved.
    console.error('Could not save the profile:', error)
    await showAlert(
      'Your personal information could not be saved. Please check your connection and try again.',
      { type: 'error', title: 'Save Failed' },
    )
  }
}

/**
 * Saves the Notification Routing selection on the account.
 *
 * The two option lists used to be hardcoded and this button only displayed a
 * confirmation dialog, so nothing was ever stored and the selection was lost on
 * reload. The selection is now written to `notify_proposals` / `notify_deadlines`
 * on the account, which is what makes the page truthful.
 */
const saveNotif = async () => {
  try {
    if (!currentUserId.value) {
      throw new Error('No signed-in account was found.')
    }

    const response = await api.put(`/api/users/${currentUserId.value}`, {
      notifyProposals: notifications.proposals,
      notifyDeadlines: notifications.deadlines,
    })

    if (response.data) {
      userStore.setUser({
        ...userStore.user,
        notifyProposals: response.data.notifyProposals ?? [],
        notifyDeadlines: response.data.notifyDeadlines ?? [],
        notifyPreferencesConfigured: response.data.notifyPreferencesConfigured ?? true,
      })
    }

    await showAlert('Notification preferences updated successfully!', { type: 'success' })
  } catch (error) {
    console.error('Could not save the notification preferences:', error)
    await showAlert(
      'Your notification preferences could not be saved. Please check your connection and try again.',
      { type: 'error', title: 'Save Failed' },
    )
  }
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
  flex-wrap: wrap;
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

/* A helper under a display-only field, e.g. the Dean's assigned college. */
.field-note {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.4;
  color: var(--color-text-soft, #64748b);
}

.field-group input[readonly] {
  background: var(--color-background-soft, #f1f5f9);
  cursor: default;
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

.notif-item--unavailable {
  cursor: not-allowed;
  opacity: 0.6;
}

.notif-item--unavailable:hover {
  border-color: var(--color-border, #cbd5e1);
}

.notif-item--unavailable input[type="checkbox"] {
  cursor: not-allowed;
}

.notif-mandatory-note {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
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

/* Layout only; the action colours come from assets/actions.css. */
.save-btn {
  padding: 11px 22px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

/* At phone width the app shell already narrows the content area; the card's
   desktop padding left the password checklist cramped. */
@media (max-width: 480px) {
  .form-card {
    padding: 20px 16px;
  }
}
</style>
