/**
 * backButton.test.js
 * ---------------------------------------------------------------------------
 * Guards the one Back-button standard of RPIS.
 *
 * The reference design is implemented once in components/BackButton.vue: a white pill
 * with a dark-navy 30px icon circle, a yellow left arrow and a navy label; hover turns
 * the circle yellow, keyboard focus shows a blue ring, pressed uses a light-grey surface,
 * and the navy variant serves dark surfaces. These tests pin that contract down and make
 * sure no page slips back to its own ad-hoc Back control.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const COMPONENT = read('../components/BackButton.vue')

/** Every file that renders page-level Back navigation. */
const BACK_CONTROLS = [
  '../components/ProposalReviewHeader.vue',
  '../components/ProposalReviewPage.vue',
  '../components/QuarterlyReportsMaster.vue',
  '../views/Auth/CreateAccount.vue',
  '../views/Auth/ForgotPassword.vue',
  '../views/Auth/ProponentForgotPassword.vue',
  '../views/Dean/DeanEndorsementReview.vue',
  '../views/Notifications/RpsNotificationDetails.vue',
  '../views/Oc/OCFinalApproval.vue',
  '../views/Oc/OcDetailedProposal.vue',
  '../views/Ovc/OvcReviewProposal.vue',
  '../views/Ovcaf/OvcafFinancialValidation.vue',
  '../views/Ovcaf/OvcafProposalDetails.vue',
  '../views/Proponent/ProponentAnnouncementDetails.vue',
  '../views/Proponent/ProponentDetiailedProp.vue',
  '../views/Proponent/ProponentEndorsementDetails.vue',
  '../views/Proponent/ProponentEndorsementRequestForm.vue',
  '../views/Proponent/ProponentPropDetails.vue',
  '../views/Proponent/ProponentQuarterlyReportForm.vue',
  '../views/Proponent/ProponentQuarterlyReports.vue',
  '../views/Proponent/ProponentSubmitProp2.vue',
  '../views/Proponent/ProponentSubmitProp3.vue',
  '../views/Proponent/ProponentSubmitRev.vue',
  '../views/Rec/RecDetailedProp.vue',
  '../views/Rec/RecMeetingDetails.vue',
  '../views/Rec/RecRejectProposal.vue',
  '../views/Rec/RecReturn4Revision.vue',
  '../views/Rps/RpsAdminVerification.vue',
  '../views/Rps/RpsBudgetRelease.vue',
  '../views/Rps/RpsEndPro.vue',
  // RpsForwardRevision's Back control now lives in ProposalReviewHeader (listed above),
  // the same shared header every other proposal page uses.
  '../views/Rps/RpsProposalDetails.vue',
  '../views/Rps/RpsProposalMonitoringDetails.vue',
  '../views/Rps/RpsProposalReview.vue',
  '../views/Rps/RpsReturn4Rev.vue',
]

test('the component implements all four interaction states', () => {
  assert.match(COMPONENT, /\.back-button:hover \.back-button__icon\s*\{[\s\S]*?background:\s*var\(--bb-yellow\)/,
    'hover must turn the icon circle yellow')
  assert.match(COMPONENT, /\.back-button:hover \.back-button__icon\s*\{[\s\S]*?color:\s*var\(--bb-navy\)/,
    'hover must turn the arrow navy')
  assert.match(COMPONENT, /\.back-button:focus-visible\s*\{[\s\S]*?outline:\s*2px solid var\(--ui-focus-color/,
    'keyboard focus must show the blue ring')
  assert.match(COMPONENT, /outline-offset:\s*3px/, 'the focus ring must have a visible gap')
  assert.match(COMPONENT, /\.back-button:active\s*\{[\s\S]*?background:\s*#f3f4f6/,
    'pressed must use the muted surface')
})

test('the default pill matches the reference measurements', () => {
  assert.match(COMPONENT, /height:\s*var\(--ui-control-height-md, 38px\)/)
  assert.match(COMPONENT, /border-radius:\s*var\(--ui-radius-pill, 999px\)/)
  assert.match(COMPONENT, /width:\s*var\(--ui-control-icon-size, 30px\)/)
  assert.match(COMPONENT, /height:\s*var\(--ui-control-icon-size, 30px\)/)
  assert.match(COMPONENT, /border:\s*1px solid var\(--bb-border\)/)
  assert.match(COMPONENT, /font-size:\s*var\(--ui-control-font-md, 14px\)/)
  assert.match(COMPONENT, /font-weight:\s*600/)
})

test('the navy variant keeps the yellow circle and light label', () => {
  assert.match(COMPONENT, /\.back-button--navy\s*\{[\s\S]*?--bb-surface:\s*rgba\(255, 255, 255, 0\.12\)/)
  assert.match(COMPONENT, /\.back-button--navy \.back-button__icon\s*\{[\s\S]*?background:\s*var\(--bb-yellow\)/)
  assert.match(COMPONENT, /--bb-text:\s*var\(--rpis-on-dark, #ffffff\)/)
})

test('the control is a real accessible button', () => {
  assert.match(COMPONENT, /<button[\s\S]*?:type="type"/)
  assert.match(COMPONENT, /type:\s*\{ type: String, default: 'button' \}/)
  assert.match(COMPONENT, /:aria-label="accessibleLabel"/)
  assert.match(COMPONENT, /aria-hidden="true"/)
  assert.match(COMPONENT, /prefers-reduced-motion: reduce/)
})

test('navigation keeps history first, then an explicit route, then a safe fallback', () => {
  assert.match(COMPONENT, /router\.push\(props\.to\)/)
  assert.match(COMPONENT, /state\.back/)
  assert.match(COMPONENT, /router\.push\(props\.fallback\)/)
  assert.match(COMPONENT, /props\.navigate/)
  assert.match(COMPONENT, /attrs\.onClick/)
})

test('every page-level Back control uses the shared component', () => {
  for (const path of BACK_CONTROLS) {
    const source = read(path)
    assert.ok(source.includes('<BackButton'), `${path} must render the shared BackButton`)
    assert.ok(source.includes('BackButton'), `${path} must import the shared BackButton`)
    assert.ok(!/←\s*Back|class="back-btn"|class="btn-back"|class="qpr-back"/.test(source),
      `${path} must not keep the old ad-hoc Back button`)
  }
})

test('no page still styles its own Back pill', () => {
  for (const path of BACK_CONTROLS) {
    const source = read(path)
    assert.ok(!/^\.(back-btn|btn-back|qpr-back)\s*\{/m.test(source),
      `${path} must not keep a competing Back-button style`)
  }
})
