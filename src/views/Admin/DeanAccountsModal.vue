<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>Dean Role Management</h2>
        <button class="close-x" @click="$emit('close')" :disabled="isLoading">&times;</button>
      </div>

      <div class="modal-body">
        <p class="subtitle">
          A Dean is an ordinary RPIS account with the Dean role, so it signs in through
          the same login. The account is created on the User Accounts page through the
          standard Create Account form; this screen manages an account that already holds
          the role - the college it endorses for, its designation, or revoking the role.
        </p>

        <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
        <div v-if="successMessage" class="success-banner">{{ successMessage }}</div>

        <!-- EXISTING DEANS -->
        <table class="dean-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>College</th>
              <th>Designation</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dean in deans" :key="dean.id">
              <td>{{ dean.name }}</td>
              <td>{{ dean.email }}</td>
              <td>{{ dean.assignedCollege || '—' }}</td>
              <td>{{ dean.designation || '—' }}</td>
              <td>{{ dean.status || 'APPROVED' }}</td>
              <td class="actions-cell">
                <button class="link-btn" @click="edit(dean)">Edit</button>
                <button class="link-btn danger" @click="revoke(dean)">Revoke role</button>
              </td>
            </tr>
            <tr v-if="deans.length === 0">
              <td colspan="6" class="empty">No Dean accounts yet.</td>
            </tr>
          </tbody>
        </table>

        <!-- EDIT ONLY. The account is created through the standard Create Account form on
             the User Accounts page, so there is no create form here. -->
        <form v-if="form.userId" class="form-group" @submit.prevent="submit">
          <h3 class="form-title">Update {{ form.name }}</h3>

          <div class="field-group">
            <label for="deanName">Full Name</label>
            <input id="deanName" v-model="form.name" :disabled="isLoading" required />
          </div>

          <div class="field-group">
            <label for="deanEmail">Institutional Email</label>
            <input id="deanEmail" v-model="form.email" type="email" :disabled="true" required />
          </div>

          <div class="field-group">
            <label for="deanCollege">Assigned College / Department</label>
            <select id="deanCollege" v-model="form.assignedCollege" :disabled="isLoading">
              <option value="">No college assigned yet (institution-wide fallback)</option>
              <option v-for="college in colleges" :key="college" :value="college">
                {{ college }}
              </option>
            </select>
          </div>

          <div class="field-group">
            <label for="deanDesignation">Designation / Title</label>
            <input
              id="deanDesignation"
              v-model="form.designation"
              placeholder="e.g. Dean, Department Chair"
              :disabled="isLoading"
            />
          </div>

          <div class="field-group">
            <label for="deanStatus">Account Status</label>
            <select id="deanStatus" v-model="form.status" :disabled="isLoading">
              <option value="APPROVED">Active</option>
              <option value="INACTIVE">Deactivated</option>
            </select>
          </div>

          <div class="buttons">
            <button type="submit" class="create-btn" :disabled="isLoading">
              {{ isLoading ? 'Saving…' : 'Save Dean' }}
            </button>
          </div>
        </form>

        <p v-else class="empty">Select a Dean above to manage the role.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/utils/api'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'

const props = defineProps({
  /** The Dean account whose role management was opened, from its row in the table. */
  deanId: { type: [Number, String], default: null },
})

const emit = defineEmits(['close', 'changed'])

const colleges = RPIS_COLLEGES
const deans = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// The account is created on the User Accounts page through the standard Create
// Account form, so this screen never creates one: `userId` is always set before the
// form can be submitted, and an existing account's password is never touched.
const form = reactive({
  userId: null,
  name: '',
  email: '',
  assignedCollege: '',
  designation: '',
  status: 'APPROVED',
})

async function loadDeans() {
  try {
    const response = await api.get('/api/users/deans')
    deans.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.error || 'The Dean accounts could not be loaded.'
  }
}

function clearForm() {
  form.userId = null
  form.name = ''
  form.email = ''
  form.assignedCollege = ''
  form.designation = ''
  form.status = 'APPROVED'
}

function edit(dean) {
  errorMessage.value = ''
  successMessage.value = ''
  form.userId = dean.id
  form.name = dean.name || ''
  form.email = dean.email || ''
  form.assignedCollege = dean.assignedCollege || ''
  form.designation = dean.designation || ''
  form.status = dean.status || 'APPROVED'
}

/** Loads the Dean list and preselects the Dean this screen was opened for. */
async function load() {
  await loadDeans()
  const requested = deans.value.find((dean) => String(dean.id) === String(props.deanId))
  if (requested) {
    edit(requested)
  }
}

async function submit() {
  if (!form.userId) {
    errorMessage.value = 'Select a Dean account to manage.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    // Only the scope and the status of an account that already holds the Dean role are
    // changed here. The backend re-checks that the caller is an RPS administrator and
    // records the change in the audit trail.
    await api.post('/api/users/deans', {
      userId: form.userId,
      name: form.name,
      email: form.email,
      assignedCollege: form.assignedCollege || null,
      designation: form.designation || null,
      status: form.status,
    })
    successMessage.value = 'The Dean account was updated.'
    await loadDeans()
    emit('changed')
  } catch (error) {
    errorMessage.value = error?.response?.data?.error || 'The Dean account could not be saved.'
  } finally {
    isLoading.value = false
  }
}

async function revoke(dean) {
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await api.delete(`/api/users/deans/${dean.id}`)
    successMessage.value = `${dean.name} no longer holds the Dean role; the account keeps working as a Faculty Proponent.`
    clearForm()
    await loadDeans()
    emit('changed')
  } catch (error) {
    errorMessage.value = error?.response?.data?.error || 'The role could not be revoked.'
  } finally {
    isLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  background: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 860px;
  max-height: 92vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.125rem;
  color: #1e293b;
}

.close-x {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  color: #94a3b8;
}

.modal-body {
  padding: 24px;
}

.subtitle {
  margin: 0 0 18px;
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.5;
}

.error-banner,
.success-banner {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.8125rem;
  margin-bottom: 16px;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.success-banner {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.dean-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  margin-bottom: 24px;
}

.dean-table th {
  text-align: left;
  padding: 8px 10px;
  border-bottom: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 0.75rem;
  text-transform: uppercase;
}

.dean-table td {
  padding: 10px;
  border-bottom: 1px solid #f1f5f9;
  color: #1f2937;
}

.dean-table .empty {
  text-align: center;
  color: #94a3b8;
}

.actions-cell {
  white-space: nowrap;
}

.link-btn {
  background: transparent;
  border: none;
  color: #2452ff;
  cursor: pointer;
  font-size: 0.8125rem;
  padding: 2px 6px;
}

.link-btn.danger {
  color: #b91c1c;
}

.form-title {
  margin: 0 0 14px;
  font-size: 0.9375rem;
  color: #1e293b;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-top: 1px solid #e2e8f0;
  padding-top: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-group label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
}

input,
select {
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  color: #1f2937;
  width: 100%;
  box-sizing: border-box;
}

.buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.create-btn {
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.cancel-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.create-btn {
  background: #2452ff;
  border: 1px solid #2452ff;
  color: #ffffff;
}

.create-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
