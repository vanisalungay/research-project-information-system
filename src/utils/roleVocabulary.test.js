/**
 * roleVocabulary.test.js
 * ---------------------------------------------------------------------------
 * Guards the application's role vocabulary (src/stores/userData.js).
 *
 * A role value is not a label. The value in the vocabulary is what the login request
 * carries, and the backend compares it against the value stored in `users.role` and
 * embedded in the bearer token (UserRoles). A rename that misses one of those layers
 * either locks the account out or answers 403 on the endpoint it guards - which is
 * exactly what the earlier `RPS_DEAN` / `DEAN` mismatch did.
 *
 * The Dean is an independent role, not part of RPS, so it is spelled `DEAN`. These
 * tests fail if it is ever declared as an RPS sub-role again, or if a production file
 * still refers to the retired `RPS_DEAN` value.
 *
 * Test files are excluded from the source scan on purpose: the regression tests keep
 * the old value in their explanations of the defect they guard.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const STORE_FILE = fileURLToPath(new URL('../stores/userData.js', import.meta.url))
const SRC_ROOT = fileURLToPath(new URL('..', import.meta.url))

/** The retired Dean role value: the Dean is not an RPS sub-role any more. */
const RETIRED_DEAN_ROLE = 'RPS_DEAN'

/** The role vocabulary, as declared key -> the value the backend stores. */
function roleVocabulary() {
  const source = readFileSync(STORE_FILE, 'utf8')
  const block = source.match(/export const UserRole = \{([\s\S]*?)\n\}/)
  assert.ok(block, 'the role vocabulary must be declared in src/stores/userData.js')

  const vocabulary = new Map()
  for (const match of block[1].matchAll(/([A-Z_]+):\s*'([A-Z_]+)'/g)) {
    vocabulary.set(match[1], match[2])
  }
  assert.ok(vocabulary.size > 0, 'the role vocabulary must not be empty')
  return vocabulary
}

/** Every production source under src/ - the tests themselves are skipped. */
function productionSources(dir = SRC_ROOT, collected = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules') continue
    const path = join(dir, entry.name)
    if (entry.isDirectory()) {
      productionSources(path, collected)
    } else if (/\.(vue|js)$/.test(entry.name) && !/\.test\.js$/.test(entry.name)) {
      collected.push(path)
    }
  }
  return collected
}

const relative = (path) => path.slice(SRC_ROOT.length + 1)

test('every role constant is named after the value it holds', () => {
  const vocabulary = roleVocabulary()

  for (const [key, value] of vocabulary) {
    assert.equal(
      value,
      key,
      `UserRole.${key} holds "${value}"; the key and the value the backend stores must be identical`,
    )
  }
})

test('the Dean is an independent role spelled DEAN', () => {
  const vocabulary = roleVocabulary()

  assert.equal(vocabulary.get('DEAN'), 'DEAN', 'the Dean role must be declared as DEAN')
  assert.ok(
    !vocabulary.has(RETIRED_DEAN_ROLE),
    `the Dean must not be declared as an RPS sub-role (${RETIRED_DEAN_ROLE})`,
  )
  assert.ok(
    !vocabulary.get('DEAN').startsWith('RPS_'),
    'the Dean is an independent role, so its value must not carry the RPS_ prefix',
  )
})

test('no role is declared as a Dean sub-role of RPS', () => {
  for (const [key, value] of roleVocabulary()) {
    assert.ok(
      !value.includes('_DEAN'),
      `UserRole.${key} holds "${value}"; the Dean role is DEAN, not a prefixed variant of it`,
    )
  }
})

test('every role the application references is declared in the vocabulary', () => {
  const vocabulary = roleVocabulary()
  const files = productionSources()

  assert.ok(files.length > 10, 'the source scan found too few files to be meaningful')
  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    for (const match of source.matchAll(/UserRole\.([A-Za-z_]+)/g)) {
      assert.ok(
        vocabulary.has(match[1]),
        `${relative(file)} references the unknown role UserRole.${match[1]}; ` +
          'an undeclared role is undefined at runtime',
      )
    }
  }
})

test('the retired RPS_DEAN value is gone from the application sources', () => {
  for (const file of productionSources()) {
    const source = readFileSync(file, 'utf8')
    assert.ok(
      !source.includes(RETIRED_DEAN_ROLE),
      `${relative(file)} still references the retired role value ${RETIRED_DEAN_ROLE}; ` +
        'the Dean role is DEAN (and the data migration renames the stored value)',
    )
  }
})
