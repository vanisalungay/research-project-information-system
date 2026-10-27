/**
 * endorsement.test.js
 * Verifies the presentation rules, the client-side validation and the payloads of
 * the Dean endorsement workflow. The rules mirror the backend, which re-validates
 * every payload and is the authority.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ENDORSEMENT_STATUS,
  approveBlockedReason,
  buildApprovalPayload,
  buildEndorsementRequestPayload,
  buildRejectionPayload,
  documentTypeLabel,
  endorsementStatusLabel,
  endorsementStatusTone,
  formatEndorsementDate,
  fundingProgramLabel,
  isApproved,
  isEndorsementRequestValid,
  isPending,
  isRejected,
  letterSettingsNotice,
  normalizeEndorsementStatus,
  suggestedLetterFileName,
  validateApproval,
  validateEndorsementRequest,
  validateRejection,
} from './endorsement.js'

const VALID_REQUEST = {
  proposalTitle: 'Mangrove Rehabilitation Study',
  proposalAnnouncementId: 3,
  college: 'College of Fisheries and Marine Sciences (CFMS)',
}

test('the three business states are labelled, toned and recognized', () => {
  assert.equal(endorsementStatusLabel('PENDING'), 'Pending')
  assert.equal(endorsementStatusLabel('approved'), 'Approved')
  assert.equal(endorsementStatusLabel('REJECTED'), 'Rejected')
  assert.equal(endorsementStatusTone('PENDING'), 'is-pending')
  assert.equal(endorsementStatusTone('APPROVED'), 'is-approved')
  assert.equal(endorsementStatusTone('REJECTED'), 'is-rejected')

  assert.equal(isPending('pending'), true)
  assert.equal(isApproved('approved'), true)
  assert.equal(isRejected('rejected'), true)
  assert.equal(isPending('APPROVED'), false)
  assert.equal(normalizeEndorsementStatus(' approved '), 'APPROVED')
  assert.equal(normalizeEndorsementStatus(null), '')
})

test('the endorsement letter is a named certification document type', () => {
  assert.equal(documentTypeLabel('ENDORSEMENT_LETTER'), 'Endorsement Letter')
  assert.equal(documentTypeLabel('SOMETHING_ELSE'), 'Document')
})

test('the request form requires a title, a call and a college', () => {
  assert.equal(isEndorsementRequestValid(VALID_REQUEST), true)

  const missing = validateEndorsementRequest({})
  assert.ok(missing.proposalTitle)
  assert.ok(missing.proposalAnnouncementId)
  assert.ok(missing.college)

  const blank = validateEndorsementRequest({
    proposalTitle: '   ',
    proposalAnnouncementId: '',
    college: '',
  })
  assert.equal(Object.keys(blank).length, 3)

  const tooLong = validateEndorsementRequest({
    ...VALID_REQUEST,
    proposalTitle: 'x'.repeat(501),
  })
  assert.ok(tooLong.proposalTitle)
})

test('the request payload is trimmed and optional fields may be omitted', () => {
  assert.deepEqual(buildEndorsementRequestPayload(VALID_REQUEST), {
    proposalTitle: 'Mangrove Rehabilitation Study',
    proposalAnnouncementId: 3,
    college: 'College of Fisheries and Marine Sciences (CFMS)',
    department: null,
    researchSummary: null,
    notes: null,
  })

  assert.deepEqual(
    buildEndorsementRequestPayload({
      ...VALID_REQUEST,
      proposalTitle: '  Padded  ',
      department: '  Department of Marine Biology ',
      researchSummary: '  A summary  ',
      notes: '   ',
    }),
    {
      proposalTitle: 'Padded',
      proposalAnnouncementId: 3,
      college: 'College of Fisheries and Marine Sciences (CFMS)',
      department: 'Department of Marine Biology',
      researchSummary: 'A summary',
      notes: null,
    },
  )
})

test('approving needs the endorser name, designation and date', () => {
  assert.deepEqual(
    validateApproval({
      endorserName: 'Dean Maria Santos',
      endorserDesignation: 'Dean',
      endorsementDate: '2026-10-27',
    }),
    {},
  )

  const errors = validateApproval({
    endorserName: '  ',
    endorserDesignation: '',
    endorsementDate: '',
  })
  assert.ok(errors.endorserName)
  assert.ok(errors.endorserDesignation)
  assert.ok(errors.endorsementDate)
})

test('the approval and rejection payloads are trimmed', () => {
  assert.deepEqual(
    buildApprovalPayload({
      endorserName: ' Dean Maria Santos ',
      endorserDesignation: ' Dean ',
      endorsementDate: ' 2026-10-27 ',
    }),
    {
      endorserName: 'Dean Maria Santos',
      endorserDesignation: 'Dean',
      endorsementDate: '2026-10-27',
    },
  )
  assert.deepEqual(buildRejectionPayload({ rejectionReason: '  Incomplete budget. ' }), {
    rejectionReason: 'Incomplete budget.',
  })
})

test('rejecting requires a reason', () => {
  assert.deepEqual(validateRejection({ rejectionReason: 'Incomplete budget.' }), {})
  assert.ok(validateRejection({ rejectionReason: '   ' }).rejectionReason)
  assert.ok(validateRejection({}).rejectionReason)
})

test('the Approve action explains why it is unavailable', () => {
  const pending = { status: ENDORSEMENT_STATUS.PENDING, letterTemplateAvailable: true }
  assert.equal(approveBlockedReason(pending), '')

  assert.match(
    approveBlockedReason({ ...pending, status: ENDORSEMENT_STATUS.APPROVED }),
    /already approved/i,
  )
  assert.match(
    approveBlockedReason({
      ...pending,
      letterTemplateAvailable: false,
      fundingProgramLabel: 'Externally Funded Project Proposal',
    }),
    /No official endorsement letter is configured for Externally Funded Project Proposal/,
  )
  assert.match(approveBlockedReason(pending, { settingsComplete: false }), /ORPS Director/)
  assert.match(approveBlockedReason(pending, { hasSignature: false }), /electronic signature/)
})

test('a non-IFP call reports its program', () => {
  assert.equal(
    fundingProgramLabel({
      fundingProgram: 'IFP',
      fundingProgramLabel: 'Internally Funded Project Proposal (IFP)',
    }),
    'Internally Funded Project Proposal (IFP)',
  )
  assert.equal(fundingProgramLabel({ fundingProgram: 'EXTERNALLY_FUNDED' }), 'EXTERNALLY_FUNDED')
  assert.equal(fundingProgramLabel({}), 'Unspecified funding program')
})

test('the Director notice appears only while the settings are incomplete', () => {
  assert.equal(letterSettingsNotice({ complete: true }), '')
  assert.equal(letterSettingsNotice(null), '')
  assert.match(letterSettingsNotice({ complete: false }), /ORPS Director is not configured/)
})

test('the issued letter has a stable suggested file name', () => {
  assert.equal(
    suggestedLetterFileName('MSUN-ORPS-END-007-2026'),
    'Endorsement-Letter-MSUN-ORPS-END-007-2026.pdf',
  )
  assert.equal(suggestedLetterFileName('  '), 'Endorsement-Letter.pdf')
  assert.equal(suggestedLetterFileName(null), 'Endorsement-Letter.pdf')
})

test('dates are shown in the official long form', () => {
  assert.equal(formatEndorsementDate('2026-10-27'), 'October 27, 2026')
  assert.equal(formatEndorsementDate(''), '')
  assert.equal(formatEndorsementDate(null), '')
  assert.equal(formatEndorsementDate('not-a-date'), 'not-a-date')
})
