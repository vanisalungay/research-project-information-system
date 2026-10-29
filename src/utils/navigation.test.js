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

import { ENDORSEMENT_TABS } from './endorsement.js'

const NAV_FILE = fileURLToPath(new URL('../components/NavMenu.vue', import.meta.url))
const STORE_FILE = fileURLToPath(new URL('../stores/userData.js', import.meta.url))
const ROUTER_FILE = fileURLToPath(new URL('../router/index.js', import.meta.url))

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

/** The role vocabulary as the object the component imports (key -> stored value). */
function userRoleObject() {
  const source = readFileSync(STORE_FILE, 'utf8')
  const block = source.match(/export const UserRole = \{([\s\S]*?)\n\}/)
  assert.ok(block, 'the role vocabulary must be declared in src/stores/userData.js')

  const vocabulary = {}
  for (const match of block[1].matchAll(/([A-Z_]+):\s*'([A-Z_]+)'/g)) {
    vocabulary[match[1]] = match[2]
  }
  return vocabulary
}

/**
 * The items the sidebar actually renders for a role.
 *
 * This evaluates the shipped `allNavItems` array with the shipped filter expression, which
 * is exactly what the component does at run time - so it checks what ends up on screen
 * rather than what the configuration appears to say. `filteredItems` renders nothing at all
 * while no user is signed in, which is the guard inside the evaluated body.
 */
function renderedSidebar(role) {
  const source = readFileSync(NAV_FILE, 'utf8')
  const start = source.indexOf('const allNavItems = [')
  assert.notEqual(start, -1, 'allNavItems must be declared in NavMenu.vue')
  const end = source.indexOf('\n]', start)
  assert.ok(end > start, 'the allNavItems array must be closed at the start of a line')

  const guard = source.match(/const filteredItems = computed\(\(\) => \{([\s\S]*?)\n\}\)/)
  assert.ok(guard, 'the visible items must be computed by `filteredItems` in NavMenu.vue')

  const allNavItems = new Function('UserRole', `return ${source.slice(source.indexOf('[', start), end + 2)}`)(
    userRoleObject(),
  )
  const visible = new Function('allNavItems', 'userStore', guard[1].trim())(
    allNavItems,
    { user: { role } },
  )

  return {
    main: visible.filter((item) => item.section === 'main').map((item) => item.text),
    bottom: visible.filter((item) => item.section === 'bottom').map((item) => item.text),
  }
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

test('the Proponent reaches the whole endorsement workflow through one sidebar item', () => {
  const main = navItems().filter(
    (item) => item.roles.includes('PROPONENT') && item.section === 'main',
  )

  // Dashboard, My Projects, My Endorsements, Notifications, Proposal Monitoring - in that
  // order, and nothing else. The Request for Endorsement form and the issued letters are
  // sections of the My Endorsements page, so they must never reappear as items of their own.
  assert.deepEqual(
    main.map((item) => item.text),
    ['Dashboard', 'My Projects', 'My Endorsements', 'Notifications', 'Proposal Monitoring'],
    'the Proponent main navigation must be exactly the five agreed items',
  )
  assert.deepEqual(
    main.map((item) => item.path),
    [
      '/home',
      '/proposals',
      '/endorsements',
      '/notifications',
      '/proponent-proposal-monitoring',
    ],
    'each Proponent item must point at its own route',
  )

  const paths = pathsFor('PROPONENT')
  for (const gone of ['/request-endorsement', '/certifications']) {
    assert.ok(
      !paths.includes(gone),
      `the Proponent must not be offered a separate ${gone} item`,
    )
  }
})

test('the consolidated endorsement page is the only endorsement route left', () => {
  const source = readFileSync(ROUTER_FILE, 'utf8')

  // The page and the per-request detail route it links to.
  assert.match(
    source,
    /path:\s*'endorsements',\s*\n\s*name:\s*'ProponentEndorsements',\s*\n\s*component:\s*ProponentEndorsements/,
    'the consolidated My Endorsements page must still be routed',
  )
  assert.match(source, /path:\s*'endorsements\/:id'/, 'the request detail route must stay')

  // The two URLs that used to be pages are redirects into the sections of that page, so
  // old bookmarks and links cannot land on a page that no longer exists.
  for (const [path, tab] of [
    ['request-endorsement', 'new'],
    ['certifications', 'certifications'],
  ]) {
    assert.match(
      source,
      new RegExp(
        `path:\\s*'${path}',\\s*\\n\\s*redirect:\\s*\\{\\s*path:\\s*'\\/endorsements',\\s*query:\\s*\\{\\s*tab:\\s*'${tab}'\\s*\\}`,
      ),
      `${path} must redirect to the consolidated page's ${tab} section`,
    )
    // The section the redirect names must exist, or the link would fall back silently.
    assert.ok(
      ENDORSEMENT_TABS.some((definition) => definition.id === tab),
      `the redirect to ?tab=${tab} must name a real section of the page`,
    )
  }

  // Nothing renders the two standalone pages any more, and no component is imported only
  // to be left unused.
  assert.ok(
    !/component:\s*ProponentCertifications/.test(source),
    'the standalone Certifications page must not be routed',
  )
  assert.ok(
    !/component:\s*ProponentEndorsementRequestForm/.test(source),
    'the Request for Endorsement form must not be routed on its own',
  )

  // The Proponent-only guard still covers the page and both old URLs.
  for (const path of ['/request-endorsement', '/endorsements', '/certifications']) {
    assert.ok(
      source.includes(`path.startsWith('${path}')`),
      `the ${path} route must stay restricted to the Proponent`,
    )
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

test('the administrator sees exactly Dashboard, User Accounts and Notifications', () => {
  const items = navItems().filter((item) => item.roles.includes('RPS_ADMIN'))
  const main = items.filter((item) => item.section === 'main')

  // The administrator manages accounts and reads their own notifications. The ORPS staff
  // surfaces - the Call for Proposal, the Priority Agendas, the quarterly reports and the
  // review queues - belong to RPS Staff and must never reappear in this list.
  assert.deepEqual(
    main.map((item) => item.text),
    ['Dashboard', 'User Accounts', 'Notifications'],
    'the administrator main navigation must be exactly these three items',
  )
  assert.deepEqual(
    main.map((item) => item.path),
    ['/home', '/user-accounts', '/notifications'],
    'each administrator item must point at its own route',
  )

  for (const staffPath of [
    '/proposal-announcements',
    '/priority-agendas',
    '/rps-quarterly-reports',
    '/rps-proposal-monitoring',
    '/rps-subproposal',
    '/budget-processing',
  ]) {
    assert.ok(
      !pathsFor('RPS_ADMIN').includes(staffPath),
      `the administrator must not be offered ${staffPath}, which is an ORPS staff surface`,
    )
  }

  // Profile Settings is shared by every role - it is not a staff item - so it stays.
  assert.deepEqual(
    items.filter((item) => item.section === 'bottom').map((item) => item.text),
    ['Profile Settings'],
  )
})

test('restricting the administrator leaves every RPS Staff item in place', () => {
  const main = navItems().filter(
    (item) => item.roles.includes('RPS_STAFF') && item.section === 'main',
  )

  assert.deepEqual(
    main.map((item) => item.text),
    [
      'Dashboard',
      'Submitted Proposals',
      'Proposal Announcements',
      'Priority Agendas',
      'Notifications',
      'Budget Processing',
      'Proposal Monitoring',
      'Quarterly Reports',
    ],
    'the ORPS staff navigation must keep every item it is authorized to use',
  )
  assert.deepEqual(
    main.map((item) => item.path),
    [
      '/home',
      '/rps-subproposal',
      '/proposal-announcements',
      '/priority-agendas',
      '/notifications',
      '/budget-processing',
      '/rps-proposal-monitoring',
      '/rps-quarterly-reports',
    ],
    'each ORPS staff item must point at its own route',
  )
})

test('the sidebar the administrator actually sees is the three administrator items', () => {
  // This is the evaluation NavMenu performs for the signed-in role, not a reading of the
  // configuration: the shipped array with the shipped filter applied.
  const admin = renderedSidebar('RPS_ADMIN')

  assert.deepEqual(
    admin.main,
    ['Dashboard', 'User Accounts', 'Notifications'],
    'the rendered administrator sidebar must be exactly these three items',
  )
  assert.deepEqual(
    admin.bottom,
    ['Profile Settings'],
    'Profile Settings is shared by every role, so it stays',
  )

  // The same evaluation for ORPS staff, which is what proves the restriction was applied to
  // the administrator alone.
  assert.deepEqual(
    renderedSidebar('RPS_STAFF').main,
    [
      'Dashboard',
      'Submitted Proposals',
      'Proposal Announcements',
      'Priority Agendas',
      'Notifications',
      'Budget Processing',
      'Proposal Monitoring',
      'Quarterly Reports',
    ],
    'the rendered ORPS staff sidebar must be unchanged',
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
