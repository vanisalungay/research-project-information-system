import { ref, reactive } from 'vue'

/**
 * Composable for showing alert/confirm dialogs programmatically.
 * 
 * Usage in <script setup>:
 *   import { useDialog } from '@/composables/useDialog'
 *   const { dialogState, showAlert, showConfirm } = useDialog()
 * 
 * Then in template, add:
 *   <ConfirmDialog v-bind="dialogState" @confirm="dialogState.onConfirm" @cancel="dialogState.onCancel" @close="dialogState.show = false" />
 */
export function useDialog() {
  const dialogState = reactive({
    show: false,
    type: 'info',        // 'info' | 'success' | 'warning' | 'error' | 'danger'
    variant: 'alert',    // 'alert' | 'confirm'
    title: '',
    message: '',
    confirmText: 'OK',
    cancelText: 'Cancel',
    loadingText: 'Processing...',
    loading: false,
    closeOnOverlay: false,
    onConfirm: () => {},
    onCancel: () => {}
  })

  /**
   * Show an alert dialog (OK button only). Returns a promise that resolves when dismissed.
   * @param {string} message - The message to display
   * @param {object} options - Optional overrides: { title, type, confirmText }
   */
  function showAlert(message, options = {}) {
    return new Promise((resolve) => {
      dialogState.type = options.type || 'info'
      dialogState.variant = 'alert'
      dialogState.title = options.title || getDefaultTitle(options.type || 'info')
      dialogState.message = message
      dialogState.confirmText = options.confirmText || 'OK'
      dialogState.loading = false
      dialogState.closeOnOverlay = false
      dialogState.onConfirm = () => {
        dialogState.show = false
        resolve()
      }
      dialogState.onCancel = () => {
        dialogState.show = false
        resolve()
      }
      dialogState.show = true
    })
  }

  /**
   * Show a confirm dialog (Cancel + Confirm buttons). Returns a promise that resolves to true/false.
   * @param {string} message - The message to display
   * @param {object} options - Optional overrides: { title, type, confirmText, cancelText }
   */
  function showConfirm(message, options = {}) {
    return new Promise((resolve) => {
      dialogState.type = options.type || 'warning'
      dialogState.variant = 'confirm'
      dialogState.title = options.title || 'Confirm Action'
      dialogState.message = message
      dialogState.confirmText = options.confirmText || 'Confirm'
      dialogState.cancelText = options.cancelText || 'Cancel'
      dialogState.loading = false
      dialogState.closeOnOverlay = false
      dialogState.onConfirm = () => {
        dialogState.show = false
        resolve(true)
      }
      dialogState.onCancel = () => {
        dialogState.show = false
        resolve(false)
      }
      dialogState.show = true
    })
  }

  function getDefaultTitle(type) {
    switch (type) {
      case 'success': return 'Success'
      case 'warning': return 'Warning'
      case 'error':
      case 'danger': return 'Error'
      default: return 'Notice'
    }
  }

  return {
    dialogState,
    showAlert,
    showConfirm
  }
}