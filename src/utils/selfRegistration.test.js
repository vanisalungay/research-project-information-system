/**
 * selfRegistration.test.js
 * ---------------------------------------------------------------------------
 * Guards the two Sign Up changes:
 *
 *   1. Department / Office is a dropdown built from the one college list RPIS already
 *      uses for endorsement requests and proposals (utils/endorsementColleges.js).
 *   2. The university address is generated from the full name, previewed by the server
 *      and re-derived authoritatively on registration - the browser's value is never
 *      trusted.
 *
 * The SFC and the backend sources are read as text, the technique the password and
 * registration tests already use.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CREATE = read('../views/Auth/CreateAccount.vue')
const COLLEGES = read('../utils/endorsementColleges.js')
const USER_SERVICE = read('../../backend/src/main/java/com/rpis/backend/service/UserService.java')
const CONTROLLER = read('../../backend/src/main/java/com/rpis/backend/controller/UserController.java')
const SECURITY = read('../../backend/src/main/java/com/rpis/backend/config/SecurityConfig.java')

test('Department / Office is a dropdown fed by the existing college list', () => {
  assert.ok(CREATE.includes("import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'"))
  assert.ok(CREATE.includes('const colleges = RPIS_COLLEGES'))

  const department = /<select\s+id="department"[\s\S]*?<\/select>/.exec(CREATE)
  assert.ok(department, 'the department field must be a select')
  assert.match(department[0], /v-model="departmentOffice"/)
  assert.match(department[0], /v-for="college in colleges"/)
  assert.match(department[0], /<option value="" disabled>Select your College \/ Office<\/option>/)
  assert.match(department[0], /required/)
  assert.ok(!CREATE.includes('<input\n              id="department"'), 'the old text input is gone')

  for (const college of [
    'College of Fisheries and Marine Sciences (CFMS)',
    'College of Environment and Life Sciences (CELS)',
    'College of Education and Social Sciences (CESS)',
    'College of Business and Information Technology (CBIT)',
    'Integrated Development School (IDS)',
  ]) {
    assert.ok(COLLEGES.includes(`'${college}'`), `the authoritative list must contain ${college}`)
  }
})

test('the university address is generated and read-only', () => {
  assert.ok(CREATE.includes("import { institutionalEmail } from '@/utils/institutionalEmail'"))
  assert.match(CREATE, /const localEmail = computed\(\(\) => institutionalEmail\(name\.value\)\)/)
  assert.match(CREATE, /const generatedEmail = computed\(\(\) => emailPreview\.value \|\| localEmail\.value\)/)
  assert.match(CREATE, /:value="generatedEmail"[\s\S]{0,200}readonly/)
  assert.ok(CREATE.includes('aria-readonly="true"'))
  assert.ok(!CREATE.includes('v-model="email"'), 'the address is never typed')
  assert.ok(!CREATE.includes('emailRegex'), 'the manual address check is gone')
  assert.match(CREATE, /Automatically generated from your full name/)
})

test('the preview asks the server for the duplicate-aware address', () => {
  assert.match(CREATE, /api\.get\('\/api\/users\/registration-email'/)
  assert.match(CREATE, /params: \{ name: trimmed \}/)
  assert.match(CREATE, /emailPreview\.value = response\.data\.email/)
  assert.match(CREATE, /email: generatedEmail\.value/, 'the preview is what the form submits')
})

test('the backend re-derives the authoritative address for self-registration', () => {
  assert.match(USER_SERVICE, /user\.setEmail\(credentialsService\.availableEmailFor\(user\.getName\(\)\)\)/)
  assert.match(CONTROLLER, /@GetMapping\("\/registration-email"\)/)
  assert.match(CONTROLLER, /availableEmailFor\(name\)/)
  assert.match(SECURITY, /"\/api\/users\/registration-email"/)
})

test('the existing registration requirements are still enforced in the form', () => {
  assert.ok(CREATE.includes(':disabled="!passwordReady"'))
  assert.ok(CREATE.includes('<PasswordRequirements'))
  assert.ok(CREATE.includes('passwordPolicyMessage('))
  assert.ok(CREATE.includes('if (password.value !== confirmPassword.value)'))
  assert.ok(CREATE.includes('if (!agree.value)'))
  assert.ok(CREATE.includes("await api.post('/api/users', payload)"))
})
