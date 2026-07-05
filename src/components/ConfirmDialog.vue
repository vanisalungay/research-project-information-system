<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="handleOverlayClick">
      <div class="dialog-modal" :class="[`dialog-${type}`, `dialog-${variant}`]">
        <!-- Icon -->
        <div class="dialog-icon-wrapper">
          <div class="dialog-icon" :class="`icon-${type}`">
            <!-- Check / Success -->
            <svg v-if="type === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke-linecap="round" stroke-linejoin="round"/>
              <polyline points="22 4 12 14.01 9 11.01" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- Warning / Triangle -->
            <svg v-else-if="type === 'warning'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="12" y1="9" x2="12" y2="13" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="12" y1="17" x2="12.01" y2="17" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- Error / X circle -->
            <svg v-else-if="type === 'error' || type === 'danger'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="15" y1="9" x2="9" y2="15" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="9" y1="9" x2="15" y2="15" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- Info / default -->
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="12" y1="16" x2="12" y2="12" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="12" y1="8" x2="12.01" y2="8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- Title -->
        <h3 class="dialog-title">{{ title }}</h3>

        <!-- Message -->
        <p class="dialog-message" v-if="message">{{ message }}</p>

        <!-- Buttons -->
        <div class="dialog-buttons">
          <button
            v-if="variant === 'confirm'"
            class="dialog-btn dialog-btn-cancel"
            @click="handleCancel"
            :disabled="loading"
          >
            {{ cancelText }}
          </button>
          <button
            class="dialog-btn dialog-btn-action"
            :class="`btn-${type}`"
            @click="handleConfirm"
            :disabled="loading"
          >
            <span v-if="loading" class="dialog-spinner"></span>
            <span>{{ loading ? loadingText : confirmText }}</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  /** Dialog type: 'info' | 'success' | 'warning' | 'error' | 'danger' */
  type: { type: String, default: 'info' },
  /** Dialog variant: 'alert' (OK only) | 'confirm' (Cancel + OK) */
  variant: { type: String, default: 'alert' },
  /** Dialog title */
  title: { type: String, default: 'Notice' },
  /** Dialog message body */
  message: { type: String, default: '' },
  /** Text for the confirm/action button */
  confirmText: { type: String, default: 'OK' },
  /** Text for the cancel button (confirm variant only) */
  cancelText: { type: String, default: 'Cancel' },
  /** Text shown on action button while loading */
  loadingText: { type: String, default: 'Processing...' },
  /** Whether an action is in progress (disables buttons, shows spinner) */
  loading: { type: Boolean, default: false },
  /** Whether clicking the overlay closes the dialog */
  closeOnOverlay: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'cancel', 'close'])

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
  emit('close')
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    emit('cancel')
    emit('close')
  }
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: dialogFadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.dialog-modal {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 16px;
  width: 400px;
  max-width: 90%;
  padding: 32px 28px 24px;
  text-align: center;
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: dialogSlideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

/* Icon */
.dialog-icon-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.dialog-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.dialog-icon svg {
  width: 100%;
  height: 100%;
}

.icon-info {
  background: #eff6ff;
  color: #2563eb;
}
.icon-success {
  background: #f0fdf4;
  color: #16a34a;
}
.icon-warning {
  background: #fffbeb;
  color: #d97706;
}
.icon-error,
.icon-danger {
  background: #fef2f2;
  color: #dc2626;
}

/* Dark mode icon backgrounds */
@media (prefers-color-scheme: dark) {
  .icon-info {
    background: rgba(37, 99, 235, 0.15);
    color: #60a5fa;
  }
  .icon-success {
    background: rgba(22, 163, 74, 0.15);
    color: #4ade80;
  }
  .icon-warning {
    background: rgba(217, 119, 6, 0.15);
    color: #fbbf24;
  }
  .icon-error,
  .icon-danger {
    background: rgba(220, 38, 38, 0.15);
    color: #f87171;
  }
}

/* Title */
.dialog-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 8px 0;
  line-height: 1.3;
}

/* Message */
.dialog-message {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 24px 0;
  line-height: 1.5;
  max-width: 340px;
  margin-left: auto;
  margin-right: auto;
}

/* Buttons */
.dialog-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.dialog-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 100px;
}

.dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dialog-btn-cancel {
  background: transparent;
  border: 1px solid var(--color-border, #e2e8f0);
  color: var(--color-text, #475569);
}

.dialog-btn-cancel:hover:not(:disabled) {
  background: var(--color-background-soft, #f8fafc);
  color: var(--color-heading, #0f172a);
}

.dialog-btn-action {
  border: 1px solid transparent;
  color: white !important;
}

.btn-info {
  background: #2452ff;
  border-color: #2452ff;
}
.btn-info:hover:not(:disabled) {
  background: #1d40cc;
  border-color: #1d40cc;
}

.btn-success {
  background: #16a34a;
  border-color: #16a34a;
}
.btn-success:hover:not(:disabled) {
  background: #15803d;
  border-color: #15803d;
}

.btn-warning {
  background: #d97706;
  border-color: #d97706;
}
.btn-warning:hover:not(:disabled) {
  background: #b45309;
  border-color: #b45309;
}

.btn-error,
.btn-danger {
  background: #dc2626;
  border-color: #dc2626;
}
.btn-error:hover:not(:disabled),
.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
  border-color: #b91c1c;
}

/* Spinner */
.dialog-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: dialogSpin 0.8s linear infinite;
}

@keyframes dialogSpin {
  to { transform: rotate(360deg); }
}

@keyframes dialogFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes dialogSlideUp {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>