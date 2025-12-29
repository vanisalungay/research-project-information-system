<template>
  <div class="manage-container">
    <h1 class="page-title">MANAGE ACCOUNT</h1>

    <button class="tab-btn">Profile Information</button>

    <div class="form-card">
      <div class="form-grid">
        <div class="form-group">
          <label>Full Name</label>
          <input type="text" v-model="form.fullName" />
        </div>

        <div class="form-group">
          <label>Email Address</label>
          <input type="email" v-model="form.email" />
        </div>

        <div class="form-group">
          <label>Department Unit</label>
          <input type="text" v-model="form.department" />
        </div>

        <div class="form-group full-width">
          <label>Contact Number</label>
          <input type="text" v-model="form.contact" />
        </div>
      </div>

      <div class="specialization-box">
        <h3>Expertise / Specialization</h3>

        <div class="checkbox-grid">
          <label
            ><input type="checkbox" v-model="form.specialization" value="Social Sciences" /> Social
            Sciences</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Health Sciences" /> Health
            Sciences</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Information Technology" />
            Information Technology</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Quantitative Research" />
            Quantitative Research</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Qualitative Research" />
            Qualitative Research</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Mix Method" /> Mix
            Method</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Sustainability" />
            Sustainability</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Community Development" />
            Community Development</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Public Administration" />
            Public Administration</label
          >
          <label
            ><input type="checkbox" v-model="form.specialization" value="Data Analytics" /> Data
            Analytics</label
          >
        </div>
      </div>

      <div class="btn-row">
        <button class="save-btn" @click="saveChanges">Save Changes</button>
        <button class="cancel-btn" @click="cancelChanges">Cancel</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const form = reactive({
  fullName: '',
  email: '',
  department: '',
  contact: '',
  specialization: [],
})

const saveChanges = () => {
  const hasInput =
    form.fullName.trim() ||
    form.email.trim() ||
    form.department.trim() ||
    form.contact.trim() ||
    form.specialization.length > 0

  if (!hasInput) {
    alert('Please fill out at least one field before saving.')
    return
  }

  alert('You successfully saved your changes.')
}

const cancelChanges = () => {
  window.history.back()
}
</script>

<style>
.page-title {
  background: #1e1c3a;
  color: #f7d900;
  padding: 16px;
  font-size: 32px;
  font-weight: 700;
  width: 100%;
}

.tab-btn {
  background: #f7d900;
  border-radius: 8px;
  border: none;
  padding: 8px 16px;
  margin-top: 20px;
  font-weight: 600;
}

.form-card {
  background: #1e1c3a;
  margin-top: 20px;
  padding: 40px;
  border-radius: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 25px;
}

.full-width {
  grid-column: span 3;
}

label {
  color: #f7d900;
  font-size: 13px;
}

input {
  width: 100%;
  padding: 8px;
  border-radius: 5px;
  border: none;
  margin-top: 6px;
}

.specialization-box {
  border: 1px solid #f7d900;
  padding: 20px;
  border-radius: 8px;
  margin-top: 30px;
}

.specialization-box h3 {
  color: #f7d900;
  margin-bottom: 15px;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.checkbox-grid label {
  color: #fff;
}

.btn-row {
  display: flex;
  gap: 20px;
  margin-top: 35px;
}

.save-btn {
  background: #f7d900;
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-weight: 600;
}

.cancel-btn {
 background: #f7d900;
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-weight: 600;
}
</style>
