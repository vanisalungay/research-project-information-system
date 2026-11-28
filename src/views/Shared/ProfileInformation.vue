<template>
  <div class="manage-container">
    <h1 class="page-title">MANAGE ACCOUNT</h1>

    <button class="tab-btn">Profile Information</button>

    <div class="form-card">
      <div class="form-grid">
        <div class="form-group">
          <label>Full Name</label>
          <input
            type="text"
            v-model="form.fullName"
            placeholder="Juan Dela Cruz"
            @blur="form.fullName = formatPersonName(form.fullName)"
          />
          <FullNameHint />
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
        <button class="save-btn btn-primary" @click="saveChanges">Save Changes</button>
        <button class="cancel-btn btn-secondary" @click="cancelChanges">Cancel</button>
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
import { reactive } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import FullNameHint from '@/components/FullNameHint.vue'
import { useDialog } from '@/composables/useDialog'
import { firstLastNameError, formatPersonName } from '@/utils/personName'

const { dialogState, showAlert, showConfirm } = useDialog()

const form = reactive({
  fullName: '',
  email: '',
  department: '',
  contact: '',
  specialization: [],
})

const saveChanges = async () => {
  const hasInput =
    form.fullName.trim() ||
    form.email.trim() ||
    form.department.trim() ||
    form.contact.trim() ||
    form.specialization.length > 0

  if (!hasInput) {
    await showAlert('Please fill out at least one field before saving.', { type: 'warning', title: 'Validation Error' })
    return
  }

  // The account name is entered First Name first, then Last Name - never the
  // "Last, First" order used inside proposal documents.
  if (form.fullName.trim()) {
    form.fullName = formatPersonName(form.fullName)
    const nameError = firstLastNameError(form.fullName)
    if (nameError) {
      await showAlert(nameError, { type: 'error', title: 'Name Required' })
      return
    }
  }

  await showAlert('You successfully saved your changes.', { type: 'success', title: 'Changes Saved' })
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
  font-size: var(--ui-font-page-title);
  font-weight: var(--ui-weight-bold);
  width: 100%;
}

.tab-btn {
  background: #f7d900;
  border-radius: 8px;
  border: none;
  padding: 8px 16px;
  margin-top: 20px;
  font-weight: var(--ui-weight-semibold);
}

.form-card {
  background: #1e1c3a;
  margin-top: 20px;
  padding: 40px;
  border-radius: 4px;
  color: #fff;
}

/* The shared name-format hint inherits this card's light text; the default opacity is
   too faint on the dark background. */
.form-card .full-name-hint {
  color: #c9c7e2;
  opacity: 1;
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
  font-size: var(--ui-font-label);
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

/* Layout only; the action colours come from assets/actions.css. */
.save-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: var(--ui-weight-semibold);
}

.cancel-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: var(--ui-weight-semibold);
}
</style>
