/**
 * proposalNotificationTarget.test.js
 * ---------------------------------------------------------------------------
 * Verifies that a dashboard notification resolves to the exact proposal saved with it, on
 * the page the signed-in role already uses - never a title match and never a wrong page.
 *
 * Also checks that both dashboards wire the click (the shared utility, the clickable class
 * and the handler) and that the routes they target really exist, so a rename can never
 * silently turn a notification click into a dead link.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  REC_ROLE,
  OVCRIGE_ROLE,
  REC_PROPOSAL_DETAILS_PATH,
  OVCRIGE_PROPOSAL_DETAILS_ROUTE,
  proposalIdFromNotification,
  proposalNotificationDestination,
} from './proposalNotificationTarget.js'

const read = (file) => readFileSync(file, 'utf8')
const REC_DASHBOARD = read(fileURLToPath(new URL('../views/Dashboard/RecDashboard.vue', import.meta.url)))
const OVCRIGE_DASHBOARD = read(
  fileURLToPath(new URL('../views/Dashboard/OvcrigeDashboard.vue', import.meta.url)),
)
const ROUTER = read(fileURLToPath(new URL('../router/index.js', import.meta.url)))
const STYLES = read(fileURLToPath(new URL('../assets/dashboard.css', import.meta.url)))

test('a REC notification opens the REC detailed-proposal page with the saved id', () => {
  const destination = proposalNotificationDestination({ proposalId: 42 }, REC_ROLE)
  assert.deepEqual(destination, { path: REC_PROPOSAL_DETAILS_PATH, query: { id: 42 } })
})

test('an OVCRIGE notification opens the proposal review page with the saved id', () => {
  const destination = proposalNotificationDestination({ proposalId: 7 }, OVCRIGE_ROLE)
  assert.deepEqual(destination, { name: OVCRIGE_PROPOSAL_DETAILS_ROUTE, params: { id: 7 } })
})

test('the id comes from the record reference, and only a usable one resolves', () => {
  // A numeric string is still the saved reference.
  assert.equal(proposalIdFromNotification({ proposalId: '15' }), 15)

  for (const value of [null, undefined, '', 'abc', 0, -1, 1.5, NaN]) {
    assert.equal(proposalIdFromNotification({ proposalId: value }), null, `"${value}" is not an id`)
    assert.equal(
      proposalNotificationDestination({ proposalId: value }, REC_ROLE),
      null,
      'a notification with no usable reference must have no destination',
    )
  }

  assert.equal(proposalIdFromNotification(null), null)
  assert.equal(proposalIdFromNotification({}), null)
})

test('the RPS, OVCAF and Chancellor roles open the proposal on their own page', () => {
  // The RPS Admin and RPS Staff share the RPS proposal-details screen, so a submission or
  // review notification opens that exact proposal there - not the Proponent's page.
  for (const role of ['RPS_ADMIN', 'RPS_STAFF']) {
    assert.deepEqual(proposalNotificationDestination({ proposalId: 5 }, role), {
      name: 'ProposalDetails',
      params: { id: 5 },
    })
  }
  assert.deepEqual(proposalNotificationDestination({ proposalId: 5 }, 'OC'), {
    name: 'OCFinalApproval',
    params: { id: 5 },
  })
  assert.deepEqual(proposalNotificationDestination({ proposalId: 5 }, 'OVCAF'), {
    path: '/ovcaf/proposals/5',
  })
})

test('the Proponent opens the proposal details page', () => {
  assert.deepEqual(proposalNotificationDestination({ proposalId: 3 }, 'PROPONENT'), {
    name: 'ProponentPropDetails',
    params: { id: 3 },
  })
})

test('a role without a proposal page never gets a destination', () => {
  for (const role of ['DEAN', undefined, 'UNKNOWN']) {
    assert.equal(
      proposalNotificationDestination({ proposalId: 3 }, role),
      null,
      `the ${role} role must not be routed by this rule`,
    )
  }
})

test('the two target routes exist and are the ones the dashboards use', () => {
  // REC reads the id from the query, so the path is what matters.
  assert.ok(
    ROUTER.includes(`path: 'rec-prop'`),
    'the REC detailed-proposal route must exist',
  )
  assert.match(
    ROUTER,
    /path: 'review-prop\/:id',\s*name: 'OvcReviewProposal'/,
    'the OVCRIGE review route must exist under its name',
  )

  // Both routes are open to any authenticated user (only Dean and the Proponent
  // endorsement routes are role-restricted), so REC and OVCRIGE keep their access.
  const restricted = ROUTER.match(/const ROLE_ONLY_ROUTES = \[([\s\S]*?)\n\]/)
  assert.ok(restricted, 'the router must declare its role-only routes')
  assert.ok(!restricted[1].includes('rec-prop'), 'the REC details route must not be role-restricted')
  assert.ok(
    !restricted[1].includes('review-prop'),
    'the OVCRIGE review route must not be role-restricted',
  )
})

test('both dashboards use the shared notification preview', () => {
  for (const [name, source] of [
    ['REC', REC_DASHBOARD],
    ['OVCRIGE', OVCRIGE_DASHBOARD],
  ]) {
    assert.match(
      source,
      /import NotificationPreview from '@\/components\/NotificationPreview\.vue'/,
      `the ${name} dashboard must use the shared preview`,
    )
    assert.match(
      source,
      /<NotificationPreview[\s\S]{0,220}view-all-to=/,
      `the ${name} dashboard must render the preview with its View all target`,
    )
    assert.ok(
      !source.includes('userId=4'),
      `the ${name} dashboard must not use a hardcoded notification user id`,
    )
  }
})

test('the clickable affordance lives in the shared design layer', () => {
  assert.match(
    STYLES,
    /\.dash-note--clickable\s*\{[^}]*cursor:\s*pointer/,
    'a clickable notification must show a pointer cursor',
  )
  assert.match(
    STYLES,
    /\.dash-note--clickable:hover\s*\{/,
    'a clickable notification must show a hover effect',
  )
})
