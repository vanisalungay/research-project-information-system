/**
 * navigation.test.js
 * ---------------------------------------------------------------------------
 * Guards the side navigation (src/components/NavMenu.vue).
 *
 * The navigation items live inside a Vue component, so nothing type-checks or tests
 * them: an item can be dropped by a bad merge or a careless edit and the page it
 * points at simply becomes unreachable, with no error anywhere. That already happened
 * once to the Proponent's endorsement links, so the definition is read here as text
 * and asserted for every role that needs a specific link.
 *
 * The role vocabulary is read from the same place the component reads it from
 * (src/stores/userData.js), so a link can never be attached to a role that does not
 * exist - a mistyped role silently hides the item forever.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const NAV_FILE = fileURLToPath(new URL('../components/NavMenu.vue', import.meta.url))
const STORE_FILE = fileURLToPath(new URL('../stores/userData.js', import.meta.url))

/** The role keys declared by the application's role vocabulary. */
function roleKeys() {
  const source = readFileSync(STORE_FILE, 'utf8')
  const block = source.match(/export const UserRole = \{([\s\S]*?)\n\}/)
  assert.ok(block, 'the role vocabulary must be declared in src/stores/userData.js')
  return [...block[1].matchAll(/([A-Z_]+):\s*'[A-Z_]+'/g)].map((match) => match[1])
}

/**
 * Every entry of the allNavItems array, with the roles it is offered to. The block is
 * parsed property by property so reordering or reformatting an item cannot break the
 * check.
 */
function navItems() {
  const source = readFileSync(NAV_FILE, 'utf8')
  const start = source.indexOf('const allNavItems = [')
  assert.notEqual(start, -1, 'allNavItems must be declared in NavMenu.vue')
  const end = source.indexOf('\n]', start)
  assert.ok(end > start, 'the allNavItems array must be closed at the start of a line')

  const items = []
  for (const raw of source.slice(start, end).matchAll(/\{([^{}]*)\}/g)) {
    const body = raw[1]
    const text = (key) => {
      const match = body.match(new RegExp(`${key}:\\s*'([^']*)'`))
      return match ? match[1] : ''
    }
    const roles = body.match(/roles:\s*\[([^\]]*)\]/)
    items.push({
      iconKey: text('iconKey'),
      text: text('text'),
      path: text('path'),
      section: text('section'),
      roles: roles
        ? [...roles[1].matchAll(/UserRole\.([A-Z_]+)/g)].map((match) => match[1])
        : [],
    })
  }
  return items
}

/** The paths a role can reach from the navigation. */
function pathsFor(role) {
  return navItems()
    .filter((item) => item.roles.includes(role))
    .map((item) => item.path)
}

test('every navigation item is well formed', () => {
  const items = navItems()
  assert.ok(items.length > 0, 'the navigation must not be empty')

  for (const item of items) {
    assert.ok(item.text, `an item has no label: ${JSON.stringify(item)}`)
    assert.match(item.path, /^\//, `${item.text} must link to an absolute path`)
    assert.ok(['main', 'bottom'].includes(item.section), `${item.text} has no section`)
    assert.ok(item.roles.length > 0, `${item.text} is offered to no role`)
    assert.ok(item.iconKey, `${item.text} has no icon key`)
  }
})

test('every role a navigation item is offered to exists in the role vocabulary', () => {
  const known = roleKeys()
  assert.ok(known.length > 0, 'the role vocabulary must not be empty')

  for (const item of navItems()) {
    for (const role of item.roles) {
      assert.ok(
        known.includes(role),
        `"${item.text}" is offered to the unknown role ${role}; a mistyped role hides the link`,
      )
    }
  }
})

test('the Proponent can reach the endorsement workflow and Certifications', () => {
  const paths = pathsFor('PROPONENT')

  for (const required of [
    '/proposals',
    '/request-endorsement',
    '/endorsements',
    '/certifications',
    '/proponent-proposal-monitoring',
  ]) {
    assert.ok(paths.includes(required), `a Proponent must reach ${required}`)
  }
})

test('the Dean sees exactly the three Dean items, and nothing obsolete', () => {
  const items = navItems().filter((item) => item.roles.includes('DEAN'))
  const main = items.filter((item) => item.section === 'main')
  const bottom = items.filter((item) => item.section === 'bottom')

  // Dashboard, Endorsement Requests, Notifications - in that order, and nothing else.
  assert.deepEqual(
    main.map((item) => item.text),
    ['Dashboard', 'Endorsement Requests', 'Notifications'],
    'the Dean main navigation must be exactly Dashboard, Endorsement Requests, Notifications',
  )
  assert.deepEqual(
    main.map((item) => item.path),
    ['/home', '/dean-requests', '/notifications'],
    'each Dean item must point at its own route',
  )

  // Dashboard is the shared /home route, which renders the Dean dashboard for this role,
  // so clicking it always returns to the Dean dashboard.
  const dashboard = main.find((item) => item.text === 'Dashboard')
  assert.ok(dashboard.roles.includes('DEAN'), 'Dashboard must be offered to the Dean')

  // Profile Settings stays: Manage Profile -> Electronic Signature is where the Dean
  // saves the signature the endorsement letter is signed with.
  assert.deepEqual(bottom.map((item) => item.text), ['Profile Settings'])

  // The obsolete letter-settings screen is gone from every role's navigation.
  for (const role of roleKeys()) {
    assert.ok(
      !pathsFor(role).includes('/dean-endorsement-settings'),
      `${role} must not be offered the obsolete endorsement letter settings`,
    )
  }
})

test('the Dean sidebar is the same on every Dean route', () => {
  // The visible items are a static configuration filtered by the signed-in role only, so
  // every Dean page - including a direct load or a refresh of any of them - produces the
  // same three items. Nothing in the filter may depend on the current route.
  const source = readFileSync(NAV_FILE, 'utf8')
  const start = source.indexOf('const filteredItems')
  const end = source.indexOf('const mainNavItems')
  assert.ok(start !== -1 && end > start, 'the navigation filter must be declared')

  const filter = source.slice(start, end)
  assert.match(filter, /userStore\.user\??\.role/, 'the items must be filtered by role')
  assert.ok(
    !filter.includes('route.path'),
    'the visible items must not depend on the current route',
  )

  assert.deepEqual(
    pathsFor('DEAN'),
    ['/home', '/dean-requests', '/notifications', '/manage-profile'],
    'the same items on every Dean route: the three Dean items plus the shared Profile Settings',
  )
})

test('a nested Dean route keeps its parent item highlighted', () => {
  const source = readFileSync(NAV_FILE, 'utf8')
  const match = source.match(/const isSelected = \(path\) => ([^\n]+)\n/)
  assert.ok(match, 'isSelected must be a single-line arrow function in NavMenu.vue')

  // Evaluate the shipped expression with a stub route, so this asserts the behaviour
  // rather than the text of the line.
  const onReview = new Function('route', `return (path) => ${match[1]}`)({
    path: '/dean-requests/7',
  })
  assert.equal(onReview('/dean-requests'), true, 'the review page keeps its list highlighted')
  assert.equal(onReview('/home'), false, 'an unrelated item is not highlighted')
  assert.equal(onReview('/notifications'), false)

  const onList = new Function('route', `return (path) => ${match[1]}`)({ path: '/dean-requests' })
  assert.equal(onList('/dean-requests'), true, 'the list page highlights its own item')
  assert.equal(onList('/notifications'), false)
})

test('the administrator can reach the Dean accounts and the Call for Proposal', () => {
  const paths = pathsFor('RPS_ADMIN')

  assert.ok(paths.includes('/user-accounts'), 'an administrator must reach the user accounts')
  assert.ok(
    paths.includes('/proposal-announcements'),
    'an administrator must reach the Call for Proposal management',
  )
})

test('no role is offered the same path twice', () => {
  for (const role of roleKeys()) {
    const paths = pathsFor(role)
    const duplicates = paths.filter((path, index) => paths.indexOf(path) !== index)
    assert.deepEqual(duplicates, [], `${role} sees a duplicated navigation path`)
  }
})

test('Profile Settings stays available to every role', () => {
  const known = roleKeys()
  const profile = navItems().find((item) => item.path === '/manage-profile')

  assert.ok(profile, 'Profile Settings must exist in the navigation')
  assert.equal(profile.section, 'bottom')
  for (const role of known) {
    assert.ok(profile.roles.includes(role), `${role} must still reach Profile Settings`)
  }
})
