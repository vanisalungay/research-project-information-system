<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>Create User Account</h2>
        <button class="close-x" @click="$emit('close')" :disabled="isLoading">&times;</button>
      </div>

      <div class="modal-body">
        <p class="subtitle">Register a new official account with specific role-based permissions.</p>

        <div v-if="errorMessage" class="error-banner">
          <svg class="error-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <form @submit.prevent="createUser" class="form-group">
          <div class="field-group">
            <label for="name">Full Name</label>
            <input 
              id="name" 
              v-model="user.name" 
              placeholder="e.g. Dr. Jane Doe" 
              :disabled="isLoading"
              required 
            />
          </div>

          <div class="field-group">
            <label for="email">Email Address</label>
            <input 
              id="email" 
              type="email"
              v-model="user.email" 
              placeholder="e.g. jane.doe@msunaawan.edu.ph" 
              :disabled="isLoading"
              required 
            />
          </div>

          <div class="field-group">
            <label for="password">Password</label>
            <input 
              id="password" 
              type="password" 
              v-model="user.password" 
              placeholder="Min. 6 characters" 
              :disabled="isLoading"
              minlength="6"
              required 
            />
          </div>

          <div class="field-group">
            <label for="role">User Role Authority</label>
            <select id="role" v-model="user.role" :disabled="isLoading">
              <option value="RPS_STAFF">RPS STAFF (Technical review & support)</option>
              <option value="RPS_ADMIN">RPS ADMIN (System administrator)</option>
              <option value="OVCAF">OVCAF (Vice Chancellor for Administration & Finance)</option>
              <option value="OVCRIGE">OVCRIGE (Vice Chancellor for Research)</option>
              <option value="REC">REC (Research Evaluation Committee)</option>
              <option value="OC">OC (Office of the Chancellor)</option>
            </select>
          </div>

          <div class="buttons">
            <button 
              type="button" 
              class="cancel-btn" 
              @click="$emit('close')"
              :disabled="isLoading"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              class="create-btn"
              :disabled="isLoading"
            >
              <span v-if="isLoading" class="spinner"></span>
              <span>{{ isLoading ? 'Creating...' : 'Create Account' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/utils/api'

const emit = defineEmits(['close', 'created'])

const user = ref({
  name: '',
  email: '',
  password: '',
  role: 'RPS_STAFF'
})

const isLoading = ref(false)
const errorMessage = ref('')

const createUser = async () => {
  if (!user.value.name || !user.value.email || !user.value.password) {
    errorMessage.value = "All fields are required."
    return
  }

  try {
    isLoading.value = true
    errorMessage.value = ""

    await api.post(
      "/api/users/admin/create",
      user.value
    )

    emit("created")
    emit("close")
  } catch (error) {
    console.error(error)
    errorMessage.value = error.response?.data || "Failed to create user account. Please try again."
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 14px;
  width: 440px;
  max-width: 90%;
  box-shadow: 
    0 20px 25px -5px rgba(0, 0, 0, 0.1), 
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
}

.modal-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0;
}

.close-x {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  color: var(--color-text-soft, #94a3b8);
  transition: color 0.15s ease;
  padding: 4px;
}

.close-x:hover {
  color: var(--color-heading, #1e293b);
}

.modal-body {
  padding: 24px;
}

.subtitle {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 20px 0;
  line-height: 1.4;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  color: #991b1b;
  font-size: 0.8125rem;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  text-align: left;
}

input,
select {
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

input:focus,
select:focus {
  outline: none;
  border-color: #2452ff;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.15);
}

input:disabled,
select:disabled {
  background: var(--color-background-soft, #f1f5f9);
  cursor: not-allowed;
  opacity: 0.7;
}

.buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

button {
  padding: 10px 18px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.cancel-btn {
  background: transparent;
  border: 1px solid var(--color-border, #e2e8f0);
  color: var(--color-text, #475569);
}

.cancel-btn:hover:not(:disabled) {
  background: var(--color-background-soft, #f8fafc);
  color: var(--color-heading, #0f172a);
}

.create-btn {
  background: #2452ff;
  border: 1px solid #2452ff;
  color: white !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.create-btn:hover:not(:disabled) {
  background: #1d40cc;
  border-color: #1d40cc;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { 
    opacity: 0; 
    transform: translateY(12px); 
  }
  to { 
    opacity: 1; 
    transform: translateY(0); 
  }
}
</style>
