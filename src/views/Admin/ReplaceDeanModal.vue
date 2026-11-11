<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>Manage Dean Role</h2>
        <button class="close-x" @click="$emit('close')" :disabled="isLoading">&times;</button>
      </div>

      <div class="modal-body">
        <p class="subtitle">
          Replace the Dean assigned to this college or department. The outgoing Dean's
          appointment is kept as history (REVOKED); the incoming Dean becomes the ACTIVE Dean.
        </p>

        <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
        <div v-if="successMessage" class="success-banner">{{ successMessage }}</div>

        <!-- The currently assigned Dean -->
        <div class="current-dean">
          <h3>Currently Assigned Dean</h3>
          <div class="detail-grid">
            <div><span class="label">Dean</span><span class="value">{{ assignment.name || '—' }}</span></div>
            <div><span class="label">Email</span><span class="value">{{ assignment.email || '—' }}</span></div>
            <div><span class="label">Assigned College / Department</span>
              <span class="value">{{ assignment.assignedCollege || 'Institution-wide (no college)' }}</span></div>
            <div><span class="label">Designation</span><span class="value">{{ assignment.designation || '—' }}</span></div>
            <div><span class="label">Date Assigned</span><span class="value">{{ formatDate(assignment.dateAssigned) }}</span></div>
          </div>
        </div>

        <form class="form-group" @submit.prevent="confirmReplace">
          <div class="field-group">
            <label for="replacement">Replacement Dean</label>
            <select id="replacement" v-model="replacementId" :disabled="isLoading">
              <option value="">Select the new Dean</option>
              <option v-for="candidate in candidates" :key="candidate.id" :value="String(candidate.id)">
                {{ candidate.name }} — {{ candidate.email }}
              </option>
            </select>
            <p v-if="!isLoading && candidates.length === 0" class="no-candidates">
              No eligible replacement Dean found
            </p>
            <button type="button" class="link-btn btn-primary" @click="showCreate = true" :disabled="isLoading">
              + Create a new Dean account
            </button>
          </div>

          <div class="field-group">
            <label for="replaceCollege">Assigned College / Department</label>
            <select id="replaceCollege" v-model="college" :disabled="isLoading">
              <option value="">Institution-wide (no college)</option>
              <option v-for="item in colleges" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>

          <div class="field-group">
            <label for="replaceDesignation">Designation / Title</label>
            <input id="replaceDesignation" v-model="designation" placeholder="e.g. Dean, Department Chair"
              :disabled="isLoading" />
          </div>

          <p v-if="confirming" class="confirm-note">
            Confirm: revoke <strong>{{ assignment.name }}</strong> and appoint the selected Dean as the new
            <strong>{{ designation || 'Dean' }}</strong> of <strong>{{ college || 'the institution' }}</strong>?
          </p>

          <div class="buttons">
            <button type="button" class="cancel-btn btn-secondary" @click="$emit('close')" :disabled="isLoading">Cancel</button>
            <button v-if="!confirming" type="button" class="create-btn" @click="askConfirm" :disabled="isLoading">
              Replace Dean
            </button>
            <button v-else type="submit" class="create-btn" :disabled="isLoading">
              {{ isLoading ? 'Replacing…' : 'Confirm replacement' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Reuse the one account-creation form; the new Dean is selected above afterwards. -->
      <Teleport to="body">
        <CreateUserModal
          v-if="showCreate"
          initial-role="DEAN"
          lock-role
          initial-designation="Dean"
          college-authority
          :initial-college="college"
          @close="showCreate = false"
          @created="onCreated"
        />
      </Teleport>
    </div>
  </div>
</template>

<script setup>
/**
 * Manage Dean Role (RPS ADMIN): replace the sitting Dean of a college with a new
 * appointment, keeping the previous Dean as history.
 *
 * Reuses the existing endpoints - the candidates and the appointment history come from the
 * Dean management API, account creation reuses the standard Create Account form, and the
 * replacement itself is `POST /api/users/deans/replace`, which performs the revoke-then-
 * activate in one backend transaction (the frontend never decides the outcome).
 */
import { onMounted, ref, watch } from 'vue'
import api from '@/utils/api'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'

const props = defineProps({
  /** The ACTIVE appointment being replaced (from the Dean Role Management list). */
  assignment: { type: Object, required: true },
})

const emit = defineEmits(['close', 'replaced'])

const colleges = RPIS_COLLEGES
const candidates = ref([])
const replacementId = ref('')
const college = ref(props.assignment.assignedCollege || '')
const designation = ref(props.assignment.designation || 'Dean')
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const confirming = ref(false)
const showCreate = ref(false)

async function loadCandidates() {
  try {
    // The backend returns only the eligible replacement Deans: accounts whose system role is
    // DEAN, approved, not the outgoing Dean of this unit and not already assigned elsewhere.
    const response = await api.get('/api/users/deans/candidates', {
      params: college.value ? { college: college.value } : {},
    })
    candidates.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.error || 'The replacement candidates could not be loaded.'
  }
}

/** First click asks for confirmation; the replacement is only sent on the second. */
function askConfirm() {
  errorMessage.value = ''
  if (!replacementId.value) {
    errorMessage.value = 'Select the account that will be the new Dean.'
    return
  }
  confirming.value = true
}

async function confirmReplace() {
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await api.post('/api/users/deans/replace', {
      userId: Number(replacementId.value),
      assignedCollege: college.value || null,
      designation: designation.value || null,
    })
    successMessage.value =
      'The Dean was replaced. The previous appointment is kept as history (REVOKED).'
    confirming.value = false
    emit('replaced')
  } catch (error) {
    errorMessage.value = error?.response?.data?.error || 'The Dean could not be replaced.'
  } finally {
    isLoading.value = false
  }
}

/** A new Dean account was created: reload candidates so it can be selected, then confirm. */
async function onCreated() {
  showCreate.value = false
  await loadCandidates()
  successMessage.value =
    'The new Dean account was created. Select it above and confirm the replacement.'
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString()
}

/** The eligible candidates depend on the unit being managed, so a change reloads the list. */
watch(college, () => {
  replacementId.value = ''
  loadCandidates()
})

onMounted(loadCandidates)
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
  max-width: 720px;
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

.current-dean {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 20px;
}

.current-dean h3 {
  margin: 0 0 12px;
  font-size: 0.875rem;
  color: #1e293b;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
}

.detail-grid > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-grid .label {
  font-size: 0.6875rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #94a3b8;
}

.detail-grid .value {
  font-size: 0.8125rem;
  color: #1f2937;
  font-weight: 600;
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

/* Layout only; the action colours come from assets/actions.css. */
.link-btn {
  align-self: flex-start;
  cursor: pointer;
  font-size: 0.8125rem;
  padding: 2px 0;
}

.confirm-note {
  margin: 0;
  padding: 10px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 0.8125rem;
  color: #92400e;
}

.buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Layout only; the action colours come from assets/actions.css. */
.cancel-btn,
.create-btn {
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.create-btn:disabled,
.cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.no-candidates {
  margin: 6px 0 0;
  font-size: 0.8125rem;
  color: #b45309;
}
</style>
