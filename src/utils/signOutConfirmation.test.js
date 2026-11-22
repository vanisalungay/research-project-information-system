/**
 * signOutConfirmation.test.js
 * ---------------------------------------------------------------------------
 * Sign Out is one shared sidebar action for every role. It must ask first, and the
 * session may only be cleared after the user confirms in the shared dialog.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')
const SIDEBAR = read('../components/NavMenu.vue')
const HEADER = read('../components/HeaderLogo.vue')

test('the shared sidebar Sign Out action opens a confirmation dialog', () => {
  assert.ok(SIDEBAR.includes('@click="signOut"'))
  assert.ok(SIDEBAR.includes("import ConfirmDialog from '@/components/ConfirmDialog.vue'"))
  assert.ok(SIDEBAR.includes("import { useDialog } from '@/composables/useDialog'"))
  assert.ok(SIDEBAR.includes('<ConfirmDialog'))
  assert.ok(SIDEBAR.includes('v-bind="dialogState"'))
  assert.ok(SIDEBAR.includes('@confirm="dialogState.onConfirm"'))
  assert.ok(SIDEBAR.includes('@cancel="dialogState.onCancel"'))
})

test('the dialog uses the required title, message and button labels', () => {
  assert.ok(SIDEBAR.includes("title: 'Confirm Sign Out'"))
  assert.ok(SIDEBAR.includes("'Are you sure you want to sign out of your account?'"))
  assert.ok(SIDEBAR.includes("confirmText: 'Confirm Sign Out'"))
  assert.ok(SIDEBAR.includes("cancelText: 'Cancel'"))
})

test('the session is cleared only after a confirmation and only once', () => {
  const start = SIDEBAR.indexOf('const signOut = async () => {')
  const end = SIDEBAR.indexOf('\n}', start)
  const body = SIDEBAR.slice(start, end)
  const confirmIndex = body.indexOf('const confirmed = await showConfirm(')
  const guardIndex = body.indexOf('if (!confirmed || signingOut.value) return')
  const logoutIndex = body.indexOf('userStore.logout()')
  assert.ok(confirmIndex >= 0 && guardIndex > confirmIndex && logoutIndex > guardIndex,
    'logout happens after the dialog resolves true')
  assert.ok(body.indexOf('if (signingOut.value || dialogState.show) return') >= 0,
    'a second click while the dialog is open or logging out is ignored')
  assert.ok(body.includes('notificationStore.reset()'))
  assert.ok(body.includes("router.push('/login')"))
})

test('Sign Out is not offered from the header dropdown any more', () => {
  assert.ok(!HEADER.includes("case 'LOGOUT':"))
  assert.ok(!HEADER.includes('>LOGOUT<'))
})
