/**
 * loginRoleOptions.test.js
 * ---------------------------------------------------------------------------
 * Guards the role cards on the login page (src/views/Auth/Login.vue).
 *
 * The card the user selects decides the `role` value sent to
 * POST /api/users/login, and the backend authenticates only when that value
 * matches the account's stored role (UserService.login). The list lives inside a
 * Vue component, so nothing type-checks it: a mistyped role key is `undefined`,
 * JSON.stringify then drops the property from the request body, and the backend
 * answers "Invalid email, password, or role." for every account and every
 * password. That is exactly what happened to the Dean card, which named
 * `UserRole._DEAN` - a key the role vocabulary does not declare - so a Dean could
 * never sign in.
 *
 * The vocabulary is read from the same place the component reads it from
 * (src/stores/userData.js), so a card can never be attached to a role that does
 * not exist.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const LOGIN_FILE = fileURLToPath(new URL('../views/Auth/Login.vue', import.meta.url))
const STORE_FILE = fileURLToPath(new URL('../stores/userData.js', import.meta.url))

/** The only e-mail domain RPIS accepts, as the login form itself states it. */
const INSTITUTIONAL_DOMAIN = '@msunaawan.edu.ph'

/** The role vocabulary, as declared key -> the role string the backend stores. */
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

/** The login page source. */
function loginSource() {
  return readFileSync(LOGIN_FILE, 'utf8')
}

/**
 * The role cards offered by the login page, parsed property by property so
 * reordering or reformatting a card cannot break the check.
 */
function roleCards() {
  const source = loginSource()
  const start = source.indexOf('const roles = [')
  assert.notEqual(start, -1, 'the role cards must be declared as `const roles = [`')
  const end = source.indexOf('\n]', start)
  assert.ok(end > start, 'the roles array must be closed at the start of a line')

  const cards = []
  for (const raw of source.slice(start, end).matchAll(/\{([^{}]*)\}/g)) {
    const body = raw[1]
    const text = (key) => {
      const match = body.match(new RegExp(`${key}:\\s*'([^']*)'`))
      return match ? match[1] : ''
    }
    const role = body.match(/value:\s*UserRole\.([A-Za-z_]+)/)
    cards.push({
      name: text('name'),
      desc: text('desc'),
      roleKey: role ? role[1] : null,
    })
  }
  return cards
}

test('every role card names a role the vocabulary declares', () => {
  const known = roleVocabulary()
  const cards = roleCards()

  assert.ok(cards.length > 0, 'the login page must offer at least one role')
  for (const card of cards) {
    assert.ok(card.name, `a role card has no label: ${JSON.stringify(card)}`)
    assert.ok(card.desc, `the "${card.name}" card has no description`)
    assert.ok(card.roleKey, `the "${card.name}" card selects no role`)
    assert.ok(
      known.has(card.roleKey),
      `the "${card.name}" card selects the unknown role UserRole.${card.roleKey}; ` +
        'it would be sent as undefined and the backend would refuse every login',
    )
  }
})

test('the Dean card sends the role a Dean account is stored with', () => {
  const dean = roleCards().find((card) => card.name === 'Dean')

  assert.ok(dean, 'the login page must offer the Dean role')
  assert.equal(dean.roleKey, 'DEAN', 'the Dean card must select UserRole.DEAN')
  assert.equal(
    roleVocabulary().get('DEAN'),
    'DEAN',
    'the Dean role must be sent as DEAN, which is the role the account is stored with',
  )
})

test('every role the login page references exists in the vocabulary', () => {
  const known = roleVocabulary()

  for (const match of loginSource().matchAll(/UserRole\.([A-Za-z_]+)/g)) {
    assert.ok(
      known.has(match[1]),
      `the login page references the unknown role UserRole.${match[1]}; ` +
        'a mistyped role is undefined at runtime',
    )
  }
})

test('no two role cards select the same role', () => {
  const keys = roleCards().map((card) => card.roleKey)
  const duplicates = keys.filter((key, index) => keys.indexOf(key) !== index)

  assert.deepEqual(duplicates, [], 'a role offered twice is ambiguous')
})

test('a role is selected by default', () => {
  const selected = loginSource().match(/const selectedRole = ref\(UserRole\.([A-Za-z_]+)\)/)

  assert.ok(selected, 'the login page must preselect a role')
  assert.ok(
    roleVocabulary().has(selected[1]),
    `the preselected role UserRole.${selected[1]} is not declared in the vocabulary`,
  )
})

test('the form asks for an address on the domain the backend accepts', () => {
  const placeholder = loginSource().match(/placeholder="([^"]*@[^"]*)"/)

  assert.ok(placeholder, 'the login form must show an example e-mail address')
  assert.ok(
    placeholder[1].toLowerCase().endsWith(INSTITUTIONAL_DOMAIN),
    `the example address "${placeholder[1]}" is not on ${INSTITUTIONAL_DOMAIN}, ` +
      'which is the only domain the backend accepts',
  )
})
