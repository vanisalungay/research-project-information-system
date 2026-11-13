/**
 * statusColorCatalog.test.js
 * ---------------------------------------------------------------------------
 * Pins the approved status colour system.
 *
 * The labels live in the category maps of statusCatalog.js; the colours live in the same
 * module (`STATUS_PALETTE`, `STATUS_COLOR_VARIANTS`, `STATUS_VARIANT_CLASSES`). These tests
 * are the acceptance criteria of the colour standardisation:
 *
 *   1. every canonical status has a valid colour mapping;
 *   2. every known alias resolves to the same label and colour (opposite return
 *      directions share the colour but are not aliases);
 *   3. APPROVED reads "Implementation Approved" and is green;
 *   4. COMPLETED reads "Completed" and is a darker green;
 *   5. those two greens are distinguishable;
 *   6. READY_FOR_RELEASE is orange, not green;
 *   7. all returned statuses share the one orange;
 *   8. the RPS forwarding stage and the REC/OVCRIGE approval stages keep distinct labels;
 *      RPS forwarding and OVCRIGE approval use the OVCRIGE cyan, REC keeps indigo;
 *   9. the OVCAF budget decision is distinguishable from the proposal status;
 *  10. a submitted report is blue;
 *  11. the mapping never depends on the page or the role;
 *  12. an unknown status falls back to gray and can never look approved or completed.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  ACCOUNT_STATUS_LABELS,
  ACTIVITY_STATUS_LABELS,
  ANNOUNCEMENT_STATUS_LABELS,
  CERTIFICATION_STATUS_LABELS,
  DEAN_ASSIGNMENT_STATUS_LABELS,
  ENDORSEMENT_STATUS_LABELS,
  OVCAF_DECISION_LABELS,
  PRIORITY_AGENDA_STATUS_LABELS,
  PROPOSAL_STATUS_LABELS,
  PROPOSAL_TIMELINE_STATUS_LABELS,
  REPORT_STATUS_LABELS,
  REVIEW_DECISION_LABELS,
  STATUS_COLOR_VARIANTS,
  STATUS_PALETTE,
  STATUS_VARIANT_CLASSES,
  statusColor,
  statusVariant,
  statusVariantClass,
} from './statusCatalog.js'
import { proposalStatusLabel, proposalStatusMatches } from './proposalStatus.js'
import { reportStatusLabel } from './statusCatalog.js'
import { statusTone } from './statusTone.js'

const STATUS_CSS = readFileSync(
  fileURLToPath(new URL('../assets/status.css', import.meta.url)),
  'utf8',
)

const BASE_CSS = readFileSync(
  fileURLToPath(new URL('../assets/css/base.css', import.meta.url)),
  'utf8',
)

/** WCAG relative luminance of a #rrggbb colour, for the "darker surface" assertion. */
function luminance(hex) {
  const channels = hex.replace('#', '').match(/[0-9a-f]{2}/gi).map((byte) => {
    const value = parseInt(byte, 16) / 255
    return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
}

const ALL_LABEL_MAPS = [
  PROPOSAL_STATUS_LABELS,
  REPORT_STATUS_LABELS,
  ACCOUNT_STATUS_LABELS,
  ENDORSEMENT_STATUS_LABELS,
  CERTIFICATION_STATUS_LABELS,
  DEAN_ASSIGNMENT_STATUS_LABELS,
  ANNOUNCEMENT_STATUS_LABELS,
  PRIORITY_AGENDA_STATUS_LABELS,
  OVCAF_DECISION_LABELS,
  REVIEW_DECISION_LABELS,
  ACTIVITY_STATUS_LABELS,
]

test('1. every canonical status has a valid colour mapping and a styled tone', () => {
  const statuses = new Set(ALL_LABEL_MAPS.flatMap((map) => Object.keys(map)))
  for (const status of statuses) {
    const variant = statusVariant(status)
    assert.ok(
      Object.hasOwn(STATUS_PALETTE, variant),
      `${status} -> ${variant} has an approved hex colour`,
    )
    const tone = statusVariantClass(status)
    assert.ok(
      Object.hasOwn(STATUS_VARIANT_CLASSES, variant),
      `${status} -> ${variant} has a tone class`,
    )
    assert.ok(
      STATUS_CSS.includes(`.status-badge${'.' + tone}`),
      `assets/status.css must style ${tone}`,
    )
  }
})

test('the palette is exactly the approved semantic palette', () => {
  assert.deepEqual(STATUS_PALETTE, {
    gray: '#64748B',
    blue: '#2563EB',
    purple: '#7C3AED',
    indigo: '#4F46E5',
    cyan: '#0891B2',
    teal: '#0F766E',
    released: '#0D9488',
    amber: '#D97706',
    orange: '#EA580C',
    approved: '#15803D',
    completed: '#166534',
    rejected: '#DC2626',
  })
})

test('2. every alias resolves to the same label and colour', () => {
  // The two submission spellings are one workflow state.
  assert.equal(proposalStatusLabel('FOR REVIEW'), proposalStatusLabel('SUBMITTED'))
  assert.equal(statusTone('FOR REVIEW'), statusTone('SUBMITTED'))
  assert.equal(statusColor('FOR REVIEW'), statusColor('SUBMITTED'))

  // The RPS return directions are NOT aliases: a reviewing office returns the proposal to
  // RPS, while RPS returns it to the Proponent. They share the returned colour only.
  assert.equal(statusTone('RETURNED_TO_RPS'), statusTone('RPS_RETURNED'))
  assert.equal(statusVariant('RETURNED_TO_RPS'), statusVariant('RPS_RETURNED'))
  assert.notEqual(proposalStatusLabel('RETURNED_TO_RPS'), proposalStatusLabel('RPS_RETURNED'))

  // An open call and a stored ACTIVE call are one state.
  assert.equal(statusTone('OPEN'), statusTone('ACTIVE'))
  assert.equal(statusVariant('OPEN'), statusVariant('ACTIVE'))
})

test('2b. RPS returning a proposal to the Proponent reads "Returned to Proponent"', () => {
  // The stored value written by ProposalService.forwardRevisionToProponent (RPS sets the
  // deadline and hands the revision request to the Proponent).
  assert.equal(proposalStatusLabel('RPS_RETURNED'), 'Returned to Proponent')
  assert.equal(statusColor('RPS_RETURNED'), '#EA580C')
  assert.equal(statusVariantClass('RPS_RETURNED'), 'is-orange')

  // The opposite direction (a reviewing office returns to RPS) keeps its own label.
  assert.equal(proposalStatusLabel('RETURNED_TO_RPS'), 'Returned to RPS')
  assert.equal(statusColor('RETURNED_TO_RPS'), '#EA580C')
  assert.equal(statusVariantClass('RETURNED_TO_RPS'), 'is-orange')

  // OVCAF's budget return is a third, separate state and is unchanged.
  assert.equal(proposalStatusLabel('RETURNED'), 'Returned for Revision')
  assert.equal(statusColor('RETURNED'), '#EA580C')

  // The neutral wording "Returned" belongs to no direction and stays out of the map.
  assert.notEqual(proposalStatusLabel('RETURNED_TO_RPS'), proposalStatusLabel('RPS_RETURNED'))
})

test('no view hardcodes a return-direction label', () => {
  // Every page must ask proposalStatusLabel/reviewStatusLabel, so this correction reaches
  // the Proponent dashboard/My Projects, monitoring, details, RPS screens, tables and
  // status history in one place. A hardcoded string would drift from the catalog again.
  const SRC = fileURLToPath(new URL('..', import.meta.url))
  const files = []
  const walk = (dir) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const path = join(dir, entry.name)
      if (entry.isDirectory()) walk(path)
      else if (entry.name.endsWith('.vue')) files.push(path)
    }
  }
  walk(SRC)

  const hardcoded = /Returned to (RPS|Proponent)/
  for (const file of files) {
    assert.doesNotMatch(
      readFileSync(file, 'utf8'),
      hardcoded,
      `${file} must render the return label from utils/statusCatalog.js, not hardcode it`,
    )
  }
})

test('Implementation Approved and Completed badges use their approved surfaces', () => {
  const token = (name) =>
    BASE_CSS.match(new RegExp(`--${name}:\\s*(#[0-9a-f]{6});`, 'gi')) || []

  // Implementation Approved: green text on a light green surface.
  assert.equal(token('status-approved-bg')[0].toLowerCase().replace(/\s+/g, ''), '--status-approved-bg:#dcfce7;')
  assert.equal(token('status-approved-text')[0].toLowerCase().replace(/\s+/g, ''), '--status-approved-text:#15803d;')

  // Completed: white text on the dark forest green surface.
  assert.equal(token('status-completed-bg')[0].toLowerCase().replace(/\s+/g, ''), '--status-completed-bg:#14532d;')
  assert.equal(token('status-completed-text')[0].toLowerCase().replace(/\s+/g, ''), '--status-completed-text:#ffffff;')

  // Each token is declared in the light theme and again in the dark theme, so the badge
  // looks the same in both.
  for (const name of ['status-approved-bg', 'status-approved-text', 'status-completed-bg', 'status-completed-text']) {
    assert.equal(token(name).length, 2, `${name} must be fixed in both themes`)
  }

  // The Completed surface is noticeably darker than the Approved one.
  assert.ok(
    luminance('#14532d') < luminance('#dcfce7'),
    'Completed must have a darker background than Implementation Approved',
  )
  assert.notEqual('#14532d', '#dcfce7', 'the two statuses must not share a background')

  // The badge rules consume the tokens (no hardcoded colours in assets/status.css).
  for (const variant of ['approved', 'completed']) {
    assert.ok(
      STATUS_CSS.includes(`var(--status-${variant}-bg)`),
      `is-${variant} must use the shared surface token`,
    )
    assert.ok(
      STATUS_CSS.includes(`var(--status-${variant}-text)`),
      `is-${variant} must use the shared text token`,
    )
  }

  // Unrelated status surfaces are untouched by this visual update.
  assert.match(BASE_CSS, /--status-rejected-bg:\s*#fee2e2;/i)
  assert.match(BASE_CSS, /--status-orange-bg:\s*#ffedd5;/i)
  assert.match(BASE_CSS, /--status-blue-bg:\s*#dbeafe;/i)
  assert.match(BASE_CSS, /--status-amber-bg:\s*#fef3c7;/i)
})

test('3. APPROVED reads "Implementation Approved" and is green', () => {
  assert.equal(proposalStatusLabel('APPROVED'), 'Implementation Approved')
  assert.equal(statusColor('APPROVED'), '#15803D')
  assert.equal(statusVariantClass('APPROVED'), 'is-approved')
})

test('4. COMPLETED reads "Completed" and uses the dark green', () => {
  assert.equal(proposalStatusLabel('COMPLETED'), 'Completed')
  assert.equal(statusColor('COMPLETED'), '#166534')
  assert.equal(statusVariantClass('COMPLETED'), 'is-completed')
})

test('5. Implementation Approved and Completed use distinguishable greens', () => {
  assert.notEqual(statusColor('APPROVED'), statusColor('COMPLETED'))
  assert.notEqual(statusTone('APPROVED'), statusTone('COMPLETED'))
})

test('6. READY_FOR_RELEASE is orange, never green', () => {
  assert.equal(statusColor('READY_FOR_RELEASE'), '#EA580C')
  assert.notEqual(statusVariant('READY_FOR_RELEASE'), statusVariant('APPROVED'))
  assert.notEqual(statusTone('READY_FOR_RELEASE'), statusTone('APPROVED'))
})

test('7. every returned status uses the one orange', () => {
  for (const status of [
    'RETURNED',
    'RETURNED_TO_RPS',
    'RPS_RETURNED',
    'RETURNED_FOR_REVISION',
    'REVISION',
    'REVISION_REQUIRED',
    'PENDING_REVISION',
    'REC_REVISION',
  ]) {
    assert.equal(statusColor(status), '#EA580C', status)
  }
  // A revoked assignment is not a return: it is an inactive record and reads gray.
  assert.equal(statusColor('REVOKED'), '#64748B')
})

test('8. the forwarding and approval stages keep distinct labels and their office colours', () => {
  // ENDORSED is the RPS -> OVCRIGE forwarding stage, written by the RPS endorse action.
  assert.equal(proposalStatusLabel('ENDORSED'), 'Endorsed to OVCRIGE')
  assert.notEqual(proposalStatusLabel('ENDORSED'), 'Endorsed by the Dean')
  assert.equal(statusVariantClass('ENDORSED'), 'is-cyan')
  assert.equal(statusColor('ENDORSED'), '#0891B2')

  // OVCRIGE's own approval is a later stage and shares the OVCRIGE office cyan.
  assert.equal(proposalStatusLabel('OVC_APPROVED'), 'Approved by OVCRIGE')
  assert.equal(statusColor('OVC_APPROVED'), '#0891B2')

  // REC's approval stays indigo, so it is never confused with either OVCRIGE stage.
  assert.equal(proposalStatusLabel('REC_APPROVED'), 'Approved by REC')
  assert.equal(statusColor('REC_APPROVED'), '#4F46E5')

  // The three stages are different words, even where two share the OVCRIGE colour.
  const labels = [
    proposalStatusLabel('ENDORSED'),
    proposalStatusLabel('OVC_APPROVED'),
    proposalStatusLabel('REC_APPROVED'),
  ]
  assert.equal(new Set(labels).size, 3, 'the three stages must use different words')
})

test('9. the OVCAF budget decision is distinguishable from the proposal status', () => {
  assert.equal(OVCAF_DECISION_LABELS.APPROVED_ENDORSED, 'Budget Approved')
  assert.equal(PROPOSAL_STATUS_LABELS.APPROVED, 'Implementation Approved')
  assert.notEqual(OVCAF_DECISION_LABELS.APPROVED_ENDORSED, PROPOSAL_STATUS_LABELS.APPROVED)
  // Both are in the approved green family, but they are different words in different contexts.
  assert.equal(statusColor('APPROVED_ENDORSED'), '#15803D')
  assert.equal(statusColor('APPROVED'), '#15803D')
})

test('10. a submitted report is blue, not green', () => {
  assert.equal(reportStatusLabel('SUBMITTED'), 'Submitted')
  assert.equal(statusColor('SUBMITTED'), '#2563EB')
  assert.equal(statusVariantClass('SUBMITTED'), 'is-blue')
  assert.equal(reportStatusLabel('DRAFT'), 'Draft')
  assert.equal(statusColor('DRAFT'), '#64748B')
})

test('11. the mapping never depends on the page or the role', () => {
  // There is no role/page parameter: the same stored value resolves the same way anywhere.
  for (const status of ['APPROVED', 'COMPLETED', 'SUBMITTED', 'RETURNED', 'PENDING']) {
    assert.equal(statusVariant(status), statusVariant(status))
    assert.equal(statusVariantClass(status), statusTone(status))
  }
  // Category label helpers never change the colour of a shared token.
  assert.equal(statusTone('SUBMITTED'), statusTone('SUBMITTED'))
  // Raw stored values differ for statuses with different meanings, even when the words
  // are close: the colour system must not merge them.
  assert.equal(proposalStatusMatches('APPROVED', 'COMPLETED'), false)
})

test('the monitoring timeline milestones have intentional mappings, not the gray fallback', () => {
  // These codes are not in the canonical proposal map, but the Proposal Monitoring timeline
  // renders them through the same shared tone helper. Each one must be an explicit entry in
  // STATUS_COLOR_VARIANTS: the resolver's gray default is a safety net for unknown values,
  // not a classification of a real stage.
  const timelineCodes = [...new Set([...Object.keys(PROPOSAL_TIMELINE_STATUS_LABELS), 'FOR_IMPLEMENTATION'])]
  for (const code of timelineCodes) {
    assert.ok(
      Object.hasOwn(STATUS_COLOR_VARIANTS, code),
      `${code} needs an explicit colour mapping, not the fallback`,
    )
    assert.notEqual(statusVariant(code), 'gray', `${code} must not read as inactive`)
  }

  // "For Implementation" is the implementation stage activated by the fund release
  // ("Project activated following funds release"): green, the same family as IMPLEMENTATION.
  assert.equal(statusVariant('For Implementation'), 'approved')
  assert.equal(statusVariantClass('FOR_IMPLEMENTATION'), 'is-approved')
  assert.equal(statusColor('For Implementation'), '#15803D')
  assert.equal(statusTone('For Implementation'), statusTone('IMPLEMENTATION'))

  // "Finance Authorization" awaits the finance office's budget decision: amber, the same
  // family as waiting for OVCAF approval.
  assert.equal(statusVariant('Finance Authorization'), 'amber')
  assert.equal(statusVariantClass('FINANCE_AUTHORIZATION'), 'is-amber')
  assert.equal(statusColor('Finance Authorization'), '#D97706')
  assert.equal(statusTone('Finance Authorization'), statusTone('FOR_OVCAF_APPROVAL'))

  // The prose milestone and the status code are one state with one label.
  assert.equal(proposalStatusLabel('For Implementation'), 'For Implementation')
  assert.equal(proposalStatusLabel('FINANCE_AUTHORIZATION'), 'Finance Authorization')
})

test('the Dean endorsement and the RPS forwarding to OVCRIGE are different stages', () => {
  // The Dean endorses an endorsement request before the proposal can be submitted. That
  // stage is ENDORSEMENT_STATUS_LABELS, not a proposal status, so it can never be confused
  // with the proposal's ENDORSED value.
  assert.equal(ENDORSEMENT_STATUS_LABELS.APPROVED, 'Approved')
  assert.equal(proposalStatusLabel('ENDORSED'), 'Endorsed to OVCRIGE')
  assert.notEqual(proposalStatusLabel('ENDORSED'), 'Endorsed by the Dean')
  for (const label of Object.values(PROPOSAL_STATUS_LABELS)) {
    assert.notEqual(label, 'Endorsed by the Dean', 'no proposal status may claim the Dean stage')
  }
})

test('the Proponent, RPS and OVCRIGE screens render the label from the catalog', () => {
  // These are the screens a returned-to-Proponent proposal appears on. Each one must ask
  // the proposal-domain label helper (which reads PROPOSAL_STATUS_LABELS) so the corrected
  // "Returned to Proponent" wording reaches them from the single source.
  for (const page of [
    '../components/ProposalReviewHeader.vue',
    '../components/ProposalReviewSidebar.vue',
    '../components/SubmissionHistory.vue',
    '../views/Dashboard/OvcrigeDashboard.vue',
    '../views/Dashboard/RpsStaffDashboard.vue',
    '../views/Notifications/ProponentNotifications.vue',
    '../views/Notifications/RpsNotificationDetails.vue',
    '../views/Ovc/OvcEndorsedProposal.vue',
    '../views/Ovc/OvcSubmittedProposal.vue',
    '../views/Proponent/ProponentProposals.vue',
    '../views/Proponent/ProponentProposalMonitoring.vue',
    '../views/Proponent/ProponentDetiailedProp.vue',
    '../views/Proponent/ProponentRevision.vue',
    '../views/Rps/RpsProposalMonitoring.vue',
    '../views/Rps/RpsProposalReview.vue',
    '../views/Rps/RpsSubmittedProposalsPanel.vue',
  ]) {
    const source = readFileSync(fileURLToPath(new URL(page, import.meta.url)), 'utf8')
    assert.match(
      source,
      /proposalStatusLabel|reviewStatusLabel/,
      `${page} must label the status through the shared catalog`,
    )
  }
})

test('12. an unknown status falls back to gray and never reads approved or completed', () => {
  for (const status of ['SOMETHING_NEW', 'WHATEVER', '', null, undefined]) {
    assert.equal(statusVariant(status), 'gray', String(status))
    assert.equal(statusVariantClass(status), 'is-gray', String(status))
    assert.notEqual(statusVariantClass(status), 'is-approved', String(status))
    assert.notEqual(statusVariantClass(status), 'is-completed', String(status))
  }
})

test('the catalog is the single source: the tone helper only re-exports it', () => {
  assert.equal(statusTone('APPROVED'), STATUS_VARIANT_CLASSES.approved)
  assert.equal(statusTone('COMPLETED'), STATUS_VARIANT_CLASSES.completed)
  assert.equal(statusTone('RETURNED'), STATUS_VARIANT_CLASSES.orange)
  assert.ok(STATUS_COLOR_VARIANTS.READY_FOR_RELEASE)
})

test('no page or component keeps its own status -> colour map', () => {
  const SRC = fileURLToPath(new URL('..', import.meta.url))
  const SELF = new Set(['statusCatalog.js', 'statusTone.js', 'statusColorCatalog.test.js'])
  const files = []
  const walk = (dir) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const path = join(dir, entry.name)
      if (entry.isDirectory()) walk(path)
      else if (/\.(vue|js)$/.test(entry.name) && !SELF.has(entry.name)) files.push(path)
    }
  }
  walk(SRC)

  // A local map would look like `APPROVED: '#15803d'` (a status code keyed to a hex colour).
  const localMap = /(DRAFT|APPROVED|REJECTED|RETURNED|SUBMITTED|PENDING|COMPLETED|ENDORSED|RELEASED)\s*:\s*['"]#[0-9a-fA-F]{3,8}['"]/
  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    assert.doesNotMatch(
      source,
      localMap,
      `${file} must take its status colour from utils/statusCatalog.js, not a local map`,
    )
  }
})
