<template>
  <div class="manage-container">
    <h1 class="page-title">MANAGE ACCOUNT</h1>

    <!-- TAB MENU -->
    <div class="tab-menu">
      <button
        class="tab-item"
        :class="{ active: activeTab === 'security' }"
        @click="activeTab = 'security'"
      >
        Security
      </button>

      <button
        class="tab-item"
        :class="{ active: activeTab === 'personal' }"
        @click="activeTab = 'personal'"
      >
        Personal Info
      </button>

      <button
        class="tab-item"
        :class="{ active: activeTab === 'notifications' }"
        @click="activeTab = 'notifications'"
      >
        Notifications
      </button>
    </div>

    <!-- SECURITY -->
    <div v-if="activeTab === 'security'">
      <button class="tab-button">Security Settings</button>

      <div class="form-card">
        <div class="form-row">
          <div class="form-group">
            <label>Change Name</label>
            <input type="text" v-model="securityForm.newName" />
          </div>
          <div class="form-group">
            <label>Change Password</label>
            <input type="password" v-model="securityForm.newPassword" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Old Name</label>
            <input type="text" v-model="securityForm.oldName" />
          </div>
          <div class="form-group">
            <label>Old Password</label>
            <input type="password" v-model="securityForm.oldPassword" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Confirm Name</label>
            <input type="text" v-model="securityForm.confirmName" />
          </div>
          <div class="form-group">
            <label>Confirm Password</label>
            <input type="password" v-model="securityForm.confirmPassword" />
          </div>
        </div>

        <div class="button-group">
          <button class="save-btn" @click="saveSecurity">Save Changes</button>
        </div>
      </div>
    </div>

    <!-- PERSONAL INFO -->
    <div v-if="activeTab === 'personal'">
      <button class="tab-btn">Profile Information</button>

      <div class="form-card">
        <div class="form-grid">
          <div class="form-group">
            <label>Full Name</label>
            <input type="text" v-model="personalForm.fullName" />
          </div>
          <div class="form-group">
            <label>Email Address</label>
            <input type="email" v-model="personalForm.email" />
          </div>
          <div class="form-group">
            <label>Department Unit</label>
            <input type="text" v-model="personalForm.department" />
          </div>
          <div class="form-group full-width">
            <label>Contact Number</label>
            <input type="text" v-model="personalForm.contact" />
          </div>
        </div>

        <div class="specialization-box">
          <h3>Expertise / Specialization</h3>
          <div class="checkbox-grid">
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Social Sciences"
              />
              Social Sciences</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Health Sciences"
              />
              Health Sciences</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Information Technology"
              />
              Information Technology</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Quantitative Research"
              />
              Quantitative Research</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Qualitative Research"
              />
              Qualitative Research</label
            >
            <label
              ><input type="checkbox" v-model="personalForm.specialization" value="Mix Method" />
              Mix Method</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Sustainability"
              />
              Sustainability</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Community Development"
              />
              Community Development</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Public Administration"
              />
              Public Administration</label
            >
            <label
              ><input
                type="checkbox"
                v-model="personalForm.specialization"
                value="Data Analytics"
              />
              Data Analytics</label
            >
          </div>
        </div>

        <div class="btn-row">
          <button class="save-btn" @click="savePersonal">Save Changes</button>
        </div>
      </div>
    </div>

    <!-- NOTIFICATIONS -->
    <div v-if="activeTab === 'notifications'">
      <button class="tab-button">Notification Settings</button>

      <div class="form-box">
        <div class="section">
          <h3 class="section-title">Proposal Notifications</h3>
          <label
            ><input
              type="checkbox"
              v-model="notifications.proposals"
              value="New Proposal Assigned"
            />
            New Proposal Assigned</label
          >
          <label
            ><input
              type="checkbox"
              v-model="notifications.proposals"
              value="Proponent Submitted Revision"
            />
            Proponent Submitted Revision</label
          >
          <label
            ><input
              type="checkbox"
              v-model="notifications.proposals"
              value="New Attachment Uploaded"
            />
            New Attachment Uploaded</label
          >
          <label
            ><input
              type="checkbox"
              v-model="notifications.proposals"
              value="Proposal Status Updated"
            />
            Proposal Status Updated</label
          >
        </div>

        <div class="section">
          <h3 class="section-title">Deadline Notifications</h3>
          <label
            ><input
              type="checkbox"
              v-model="notifications.deadlines"
              value="Evaluation Deadline Reminders"
            />
            Evaluation Deadline Reminders</label
          >
          <label
            ><input
              type="checkbox"
              v-model="notifications.deadlines"
              value="Revision Deadline Reminders"
            />
            Revision Deadline Reminders</label
          >
        </div>

        <div class="section">
          <h3 class="section-title">System Notifications</h3>
          <label
            ><input type="checkbox" v-model="notifications.system" value="Email Reminders" /> Email
            Reminders</label
          >
          <label
            ><input type="checkbox" v-model="notifications.system" value="System Notifications" />
            System Notifications</label
          >
        </div>

        <button class="btn-save" @click="saveNotif">Save Settings</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const activeTab = ref('security')

// Security Form
const securityForm = reactive({
  newName: '',
  newPassword: '',
  oldName: '',
  oldPassword: '',
  confirmName: '',
  confirmPassword: '',
})

const saveSecurity = () => {
  const hasInput = Object.values(securityForm).some((v) => v.trim() !== '')
  if (!hasInput) return alert('Please fill out at least one field before saving.')
  alert('Security settings saved!')
}
const cancelSecurity = () => window.history.back()

// Personal Info Form
const personalForm = reactive({
  fullName: '',
  email: '',
  department: '',
  contact: '',
  specialization: [],
})

const savePersonal = () => {
  const hasInput =
    personalForm.fullName.trim() ||
    personalForm.email.trim() ||
    personalForm.department.trim() ||
    personalForm.contact.trim() ||
    personalForm.specialization.length > 0
  if (!hasInput) return alert('Please fill out at least one field before saving.')
  alert('Personal info saved!')
}
const cancelPersonal = () => window.history.back()

// Notifications Form
const notifications = reactive({
  proposals: [],
  deadlines: [],
  system: [],
})

const saveNotif = () => {
  const hasInput = Object.values(notifications).some((v) => v.length > 0)
  if (!hasInput) return alert('Please select at least one notification type before saving.')
  alert('Notification settings saved!')
}
const cancelNotif = () => window.history.back()
</script>

<style>
.page-title {
  background: #1f1d39;
  color: #f5cd22;
  padding: 14px 25px;
  font-size: 32px;
  font-weight: 700;
  width: fit-content;
}

.tab-menu {
  display: flex;
  gap: 40px;
  margin-top: 20px;
  margin-left: 10px;
}

.tab-item {
  background: transparent;
  border: none;
  color: #1f1d39;
  font-weight: bold;
  font-size: 18px;
  cursor: pointer;
  padding-bottom: 6px;
}

.tab-item.active {
  border-bottom: 4px solid #f5cd22;
}

.tab-button {
  margin-top: 20px;
  margin-left: 10px;
  background: #f5cd22;
  padding: 6px 20px;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
}

.tab-button {
  pointer-events: none;
}

.form-card {
  background: #1f1d39;
  margin-top: 20px;
  padding: 40px;
  border-radius: 6px;
  width: 80%;
}

.form-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 25px;
}

.form-group {
  width: 45%;
  display: flex;
  flex-direction: column;
}
/* PERSONAL INFO SPECIALIZATION CHECKBOXES */
.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px 20px;
}

.checkbox-grid label {
  display: flex;
  align-items: center; 
  gap: 8px; 
  font-weight: normal;
  color: white;
  cursor: pointer;
}

.section label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: normal;
  color: white;
  cursor: pointer;
}

input[type='checkbox'] {
  width: 16px;
  height: 16px;
  margin: 0;
}

.form-box {
  background: #1c1a3c;
  padding: 40px;
  border-radius: 8px;
  color: white;
  margin-top: 20px;
}
.btn-container {
  display: flex;
  gap: 10px; /* spacing between buttons */
}

.btn-save {
  background: #ffd429;
  border: none;
  padding: 8px 18px;
  border-radius: 6px;
  margin-top: 20px;
}

label {
  color: #f5cd22;
  font-weight: bold;
  margin-bottom: 6px;
}

input {
  padding: 8px;
  border-radius: 5px;
  border: none;
  width: 100%;
}

.button-group {
  display: flex;
  justify-content: center;
  margin-top: 25px;
  gap: 20px;
}

.save-btn {
  background: #f5cd22;
  padding: 8px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-weight: normal;
}
</style>
