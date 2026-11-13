/**
 * proposalActionLabels.test.js
 * ---------------------------------------------------------------------------
 * Guards the proposal action button labels of the REC and OVCRIGE modules.
 *
 * The labels were shortened to REVIEW and VIEW. What must not change with them is the
 * behaviour behind the buttons: each one still calls the same handler, which still opens the
 * same page for the same proposal id. The files are read as text - the technique
 * navigation.test.js and the dashboard guard tests already use - so this needs no browser.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const REC_DASHBOARD = read('../views/Dashboard/RecDashboard.vue')
const OVCRIGE_DASHBOARD = read('../views/Dashboard/OvcrigeDashboard.vue')
const REC_ASSIGNED = read('../views/Rec/RecAssignedProposal.vue')
const REC_MEETING = read('../views/Rec/RecMeetingDetails.vue')
const REC_REVIEW_FORM = read('../views/Rec/RecReviewForm.vue')
const OVC_SUBMITTED = read('../views/Ovc/OvcSubmittedProposal.vue')
const OVC_ENDORSED = read('../views/Ovc/OvcEndorsedProposal.vue')

/** Every REC / OVCRIGE page that shows a proposal action, with the labels it must show. */
const PAGES = [
  ['RecDashboard.vue', REC_DASHBOARD, ['REVIEW', 'VIEW']],
  ['RecAssignedProposal.vue', REC_ASSIGNED, ['REVIEW', 'VIEW']],
  ['RecMeetingDetails.vue', REC_MEETING, ['VIEW']],
  ['RecReviewForm.vue', REC_REVIEW_FORM, ['VIEW']],
  ['OvcrigeDashboard.vue', OVCRIGE_DASHBOARD, ['REVIEW']],
  ['OvcSubmittedProposal.vue', OVC_SUBMITTED, ['REVIEW']],
  ['OvcEndorsedProposal.vue', OVC_ENDORSED, ['REVIEW']],
]

const rendersLabel = (source, label) =>
  new RegExp('>\\s*' + label + '\\s*<').test(source) ||
  new RegExp('^\\s*' + label + '\\s*$', 'm').test(source)

test('the REC and OVCRIGE proposal action buttons read REVIEW and VIEW', () => {
  for (const [name, source, labels] of PAGES) {
    for (const label of labels) {
      assert.ok(rendersLabel(source, label), name + ' must render the ' + label + ' label')
    }
  }
})

test('the long labels are gone from both modules', () => {
  for (const [name, source] of PAGES) {
    assert.ok(!source.includes('REVIEW PROPOSAL'), name + ' must not still say REVIEW PROPOSAL')
    assert.ok(!source.includes('VIEW DETAILED PROPOSAL'), name + ' must not still say VIEW DETAILED PROPOSAL')
    assert.ok(!source.includes('SEE DETAILED PROPOSAL'), name + ' must not say SEE DETAILED PROPOSAL')
  }
})

test('each button still runs the handler it ran before', () => {
  // REVIEW -> the review page for that proposal. VIEW -> the details page for that proposal.
  assert.ok(REC_DASHBOARD.includes('@click="goToReview(proposal.id)"'), 'REC REVIEW handler')
  assert.ok(REC_DASHBOARD.includes('@click="goToMeeting(proposal.id)"'), 'REC VIEW handler')
  assert.ok(REC_ASSIGNED.includes('@click="goToReview(proposal.id)"'), 'assigned REVIEW handler')
  assert.ok(REC_ASSIGNED.includes('@click="goToView(proposal.id)"'), 'assigned VIEW handler')
  assert.ok(REC_MEETING.includes('@click="goToDetailed"'), 'meeting VIEW handler')
  assert.ok(REC_REVIEW_FORM.includes('@click="goToDetailed"'), 'review form VIEW handler')
  assert.ok(OVCRIGE_DASHBOARD.includes('@click="goToReview(proposal.id)"'), 'OVCRIGE REVIEW handler')
  assert.ok(OVC_SUBMITTED.includes('@click="goToReview(proposal.id)"'), 'submitted REVIEW handler')
  assert.ok(OVC_ENDORSED.includes('@click="goToReview(proposal.id)"'), 'endorsed REVIEW handler')
})

test('the destinations behind those handlers are unchanged', () => {
  for (const source of [REC_DASHBOARD, REC_ASSIGNED]) {
    assert.ok(source.includes("name: 'RecReviewForm', params: { id }"))
    assert.ok(source.includes("name: 'RecMeetingDetails', params: { id }"))
  }
  for (const source of [REC_REVIEW_FORM, REC_MEETING]) {
    assert.ok(source.includes("path: '/rec-prop'"))
  }
})

test('no other role page was relabelled', () => {
  // The Proponent and Shared pages keep exactly the wording they had.
  for (const file of [
    '../views/Proponent/ProponentFeedback.vue',
    '../views/Proponent/ProponentPropDetails.vue',
    '../views/Shared/FundViewProp.vue',
  ]) {
    assert.ok(read(file).includes('VIEW DETAILED PROPOSAL'), file + ' must keep its own label')
  }
})