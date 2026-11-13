/**
 * temporaryPasswordWorkflow.test.js
 * ---------------------------------------------------------------------------
 * Guards the whole administrator-created-account workflow, layer by layer:
 *
 *   name -> generated institutional address -> generated temporary password -> credentials
 *   shown once -> login -> forced password change -> the restriction is lifted.
 *
 * The pieces live in different layers (Vue component, router, Spring service, Spring filter,
 * SQL migration), so the files are read as text - the technique the other guard tests use.
 * The risk this covers is a later edit quietly dropping one link of the chain.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const exists = (path) => existsSync(fileURLToPath(new URL(path, import.meta.url)))

const MODAL = read('../views/Admin/CreateUserModal.vue')
const LOGIN = read('../views/Auth/Login.vue')
const CHANGE = read('../views/Auth/ChangePassword.vue')
const ROUTER = read('../router/index.js')
const STORE = read('../stores/userData.js')
const CREDENTIALS = read('../../backend/src/main/java/com/rpis/backend/service/InstitutionalCredentialsService.java')
const USER_SERVICE = read('../../backend/src/main/java/com/rpis/backend/service/UserService.java')
const CONTROLLER = read('../../backend/src/main/java/com/rpis/backend/controller/UserController.java')
const FILTER = read('../../backend/src/main/java/com/rpis/backend/config/TokenAuthenticationFilter.java')
const ENTITY = read('../../backend/src/main/java/com/rpis/backend/model/User.java')

test('the modal no longer asks for an email or a password', () => {
  assert.ok(!MODAL.includes('v-model="user.email"'), 'the email is never typed')
  assert.ok(!MODAL.includes('v-model="user.password"'), 'the password is never typed')
  assert.ok(MODAL.includes('id="email" :value="generatedEmail"'), 'the email is read-only')
  assert.ok(MODAL.includes('institutionalEmail'), 'and previews the naming rule')
  assert.ok(MODAL.includes("role: user.value.role"), 'the role is still chosen by the administrator')
})

test('the modal shows the credentials the backend returned, once', () => {
  assert.ok(MODAL.includes('body.temporaryPassword'), 'the temporary password comes from the response')
  assert.ok(MODAL.includes('body.email'), 'so does the address')
  assert.ok(MODAL.includes('v-if="created"'), 'and only after a successful creation')
  assert.ok(MODAL.includes('const copy ='), 'with a copy button per credential')
  assert.ok(MODAL.includes('const copyBoth ='), 'and one for both')
  assert.ok(MODAL.includes("emit('created')"), 'the list behind the modal is refreshed')
  assert.ok(MODAL.includes('if (isLoading.value) return'), 'a double submit cannot create two accounts')
})

test('the backend is the authority for the address and the password', () => {
  assert.ok(CREDENTIALS.includes('"@msunawaan.edu.ph"'), 'the canonical domain')
  assert.ok(CREDENTIALS.includes('temporaryPasswordFor(String fullName)'),
    'the password is derived from the name, in one place')
  assert.ok(CREDENTIALS.includes('TEMPORARY_PASSWORD_SUFFIX = "123"'), 'and ends with 123')
  assert.ok(USER_SERVICE.includes('temporaryPasswordFor('),
    'the standard account-creation path uses that one rule')
  assert.ok(CONTROLLER.includes('temporaryPasswordFor('),
    'and so does the Dean path, without repeating it')
  assert.ok(CREDENTIALS.includes('availableEmailFor'), 'the uniqueness loop')
  assert.ok(USER_SERVICE.includes('createStaffAccountWithGeneratedCredentials'), 'wired into creation')
  assert.ok(USER_SERVICE.includes('passwordService.hash('), 'hashed with the existing encoder')
  assert.ok(ENTITY.includes('mustChangePassword'), 'the flag lives on the account')
  assert.ok(exists('../../backend/migration-add-must-change-password.sql'), 'and has a migration')
  assert.ok(CONTROLLER.includes('@PutMapping("/change-password")'), 'the change endpoint exists')
  assert.ok(CONTROLLER.includes('currentUser.requireUserId()'), 'and acts on the signed-in account')
})

test('the restriction is enforced on the server, not only in the browser', () => {
  assert.ok(FILTER.includes('getMustChangePassword'), 'the filter checks the flag')
  assert.ok(FILTER.includes('isAllowedWhileChangingPassword'), 'and allows only what it must')
  assert.ok(FILTER.includes('SC_FORBIDDEN'), 'refusing everything else')
  assert.ok(FILTER.includes('"/api/users/change-password"'), 'the change endpoint stays reachable')
  assert.ok(FILTER.includes('"/api/users/login"'), 'and so does signing in')
})

test('the frontend sends the user to the change-password page and keeps them there', () => {
  assert.ok(STORE.includes('setUser(response.data)'), 'the flag arrives with the login response')
  assert.ok(LOGIN.includes("router.push('/change-password')"), 'login redirects on the flag')
  assert.ok(ROUTER.includes("path: 'change-password'"), 'the page is routed')
  assert.ok(ROUTER.includes('mustChangePassword'), 'the guard reads the flag')
  assert.match(ROUTER, /to\.path !== '\/change-password'/, 'and bounces every other route back')
  assert.ok(CHANGE.includes("api.put('/api/users/change-password'"), 'the page calls the endpoint')
  assert.ok(CHANGE.includes('mustChangePassword: false'), 'and clears the flag locally on success')
  assert.ok(CHANGE.includes('dashboardForRole'), 'then sends the user to their own dashboard')
})