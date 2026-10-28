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
  DEFAULT_ENDORSEMENT_TAB,
  ENDORSEMENT_STATUS,
  ENDORSEMENT_TABS,
  MAX_PROJECT_TITLE_LENGTH,
  approveBlockedReason,
  buildApprovalPayload,
  buildEndorsementRequestPayload,
  buildRejectionPayload,
  documentTypeLabel,
  endorsementProposalPrefill,
  endorsementStatusLabel,
  endorsementStatusTone,
  endorsementSummaryCounts,
  endorsementTab,
  endorsementTabFromQuery,
  formatEndorsementDate,
  fundingProgramLabel,
  isApproved,
  isEndorsementRequestValid,
  isPending,
  isRejected,
  issuedLetterCount,
  normalizeEndorsementStatus,
  suggestedLetterFileName,
  validateApproval,
  validateEndorsementRequest,
  validateRejection,
} from './endorsement.js'
import { CREATE_NEW_PROGRAM_TITLE } from './programTitleOptions.js'
import * as endorsementModule from './endorsement.js'

const VALID_REQUEST = {
  // Two different titles on purpose: a rule or a mapping that confused the Program Title
  // with the Project Title would fail every assertion below.
  programTitle: 'MSUN RIIDE - Blue Economy and Marine Resources',
  projectTitle: 'Mangrove Rehabilitation Study',
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

test('the endorsement counts match the records and the states', () => {
  const requests = [
    { status: 'PENDING' },
    { status: 'pending' },
    { status: 'APPROVED', hasLetter: true },
    { status: 'APPROVED', hasLetter: true },
    { status: 'REJECTED' },
    { status: 'SOMETHING_ELSE' },
  ]
  assert.deepEqual(endorsementSummaryCounts(requests), {
    total: 6,
    pending: 2,
    approved: 2,
    rejected: 1,
  })

  // An empty or odd response reports zeros instead of failing.
  const empty = { total: 0, pending: 0, approved: 0, rejected: 0 }
  assert.deepEqual(endorsementSummaryCounts([]), empty)
  assert.deepEqual(endorsementSummaryCounts(null), empty)
  assert.deepEqual(endorsementSummaryCounts(undefined), empty)
  assert.deepEqual(endorsementSummaryCounts({}), empty)
})

test('only a request with an issued letter is counted as an issued document', () => {
  // A pending or rejected request is never an issued document, and neither is an approved
  // request whose letter is missing.
  assert.equal(
    issuedLetterCount([
      { status: 'PENDING' },
      { status: 'REJECTED' },
      { status: 'APPROVED' },
      { status: 'APPROVED', hasLetter: false },
      { status: 'APPROVED', hasLetter: true },
    ]),
    1,
  )
  assert.equal(issuedLetterCount([]), 0)
  assert.equal(issuedLetterCount(null), 0)
  assert.equal(issuedLetterCount(undefined), 0)
})

test('the consolidated page has one section per part of the workflow', () => {
  // The Request for Endorsement, the tracking list and the issued letters are the three
  // parts of the same workflow, and they are sections of one page instead of three pages.
  assert.deepEqual(
    ENDORSEMENT_TABS.map((tab) => tab.id),
    ['requests', 'new', 'certifications'],
  )
  assert.deepEqual(
    ENDORSEMENT_TABS.map((tab) => tab.label),
    ['My Requests', 'Request Endorsement', 'Issued Certifications'],
  )
  // Every section explains itself, so the page needs no second level of navigation.
  for (const tab of ENDORSEMENT_TABS) {
    assert.ok(tab.description.length > 20, `${tab.id} must describe what it shows`)
  }
  // Opening the page shows the tracking list, which is what "My Endorsements" always meant.
  assert.equal(DEFAULT_ENDORSEMENT_TAB, 'requests')
  assert.equal(endorsementTab().id, 'requests')
  assert.equal(endorsementTab('certifications').label, 'Issued Certifications')
})

test('a ?tab= value selects a section and an unknown one falls back to the list', () => {
  // The values the old routes redirect with.
  assert.equal(endorsementTabFromQuery('new'), 'new')
  assert.equal(endorsementTabFromQuery('certifications'), 'certifications')
  assert.equal(endorsementTabFromQuery('requests'), 'requests')
  assert.equal(endorsementTabFromQuery('  certifications  '), 'certifications')

  // A missing, empty, unknown or non-string value can never open a blank page.
  assert.equal(endorsementTabFromQuery(''), DEFAULT_ENDORSEMENT_TAB)
  assert.equal(endorsementTabFromQuery('   '), DEFAULT_ENDORSEMENT_TAB)
  assert.equal(endorsementTabFromQuery('nope'), DEFAULT_ENDORSEMENT_TAB)
  assert.equal(endorsementTabFromQuery(undefined), DEFAULT_ENDORSEMENT_TAB)
  assert.equal(endorsementTabFromQuery(null), DEFAULT_ENDORSEMENT_TAB)
  assert.equal(endorsementTabFromQuery(['new', 'certifications']), DEFAULT_ENDORSEMENT_TAB)
})

test('the request form requires a project title, a call and a college', () => {
  assert.equal(isEndorsementRequestValid(VALID_REQUEST), true)

  const missing = validateEndorsementRequest({})
  assert.ok(missing.projectTitle)
  assert.ok(missing.proposalAnnouncementId)
  assert.ok(missing.college)
  // The Program Title is optional, exactly like the Create Proposal form.
  assert.equal(missing.programTitle, undefined)

  const blank = validateEndorsementRequest({
    projectTitle: '   ',
    proposalAnnouncementId: '',
    college: '',
  })
  assert.equal(Object.keys(blank).length, 3)

  const tooLong = validateEndorsementRequest({
    ...VALID_REQUEST,
    projectTitle: 'x'.repeat(501),
  })
  assert.ok(tooLong.projectTitle)
  assert.equal(MAX_PROJECT_TITLE_LENGTH, 500)
})

test('the Program Title and the Project Title are validated independently', () => {
  // A program title can never satisfy the required project title.
  const onlyProgramTitle = validateEndorsementRequest({ ...VALID_REQUEST, projectTitle: '' })
  assert.ok(onlyProgramTitle.projectTitle)
  assert.equal(onlyProgramTitle.programTitle, undefined)

  // A project title can never satisfy the "create a new program title" flow.
  const creating = validateEndorsementRequest({
    ...VALID_REQUEST,
    programTitle: '   ',
    programTitleChoice: CREATE_NEW_PROGRAM_TITLE,
  })
  assert.ok(creating.programTitle)
  assert.equal(creating.projectTitle, undefined)

  // While the field is not in its creation flow it is simply optional.
  assert.deepEqual(
    validateEndorsementRequest({ ...VALID_REQUEST, programTitle: '', programTitleChoice: '' }),
    {},
  )

  // The program title is capped at the width a proposal can store (255).
  assert.ok(
    validateEndorsementRequest({ ...VALID_REQUEST, programTitle: 'x'.repeat(256) }).programTitle,
  )
  assert.equal(
    validateEndorsementRequest({ ...VALID_REQUEST, programTitle: 'x'.repeat(255) }).programTitle,
    undefined,
  )
})

test('the request payload carries the two titles as separate values', () => {
  assert.deepEqual(buildEndorsementRequestPayload(VALID_REQUEST), {
    programTitle: 'MSUN RIIDE - Blue Economy and Marine Resources',
    projectTitle: 'Mangrove Rehabilitation Study',
    proposalAnnouncementId: 3,
    college: 'College of Fisheries and Marine Sciences (CFMS)',
    department: null,
    researchSummary: null,
    notes: null,
  })

  assert.deepEqual(
    buildEndorsementRequestPayload({
      ...VALID_REQUEST,
      programTitle: '  Padded Program  ',
      projectTitle: '  Padded Project  ',
      department: '  Department of Marine Biology ',
      researchSummary: '  A summary  ',
      notes: '   ',
    }),
    {
      programTitle: 'Padded Program',
      projectTitle: 'Padded Project',
      proposalAnnouncementId: 3,
      college: 'College of Fisheries and Marine Sciences (CFMS)',
      department: 'Department of Marine Biology',
      researchSummary: 'A summary',
      notes: null,
    },
  )

  // An empty (optional) program title is omitted rather than filled with the project
  // title, and the control's sentinel can never be submitted as a title.
  assert.equal(
    buildEndorsementRequestPayload({ ...VALID_REQUEST, programTitle: '' }).programTitle,
    null,
  )
  assert.equal(
    buildEndorsementRequestPayload({
      ...VALID_REQUEST,
      programTitle: CREATE_NEW_PROGRAM_TITLE,
      programTitleChoice: CREATE_NEW_PROGRAM_TITLE,
    }).programTitle,
    null,
  )
})

test('the Create Proposal prefill carries both titles independently', () => {
  assert.deepEqual(
    endorsementProposalPrefill({
      programTitle: 'MSUN RIIDE - Blue Economy and Marine Resources',
      projectTitle: 'Mangrove Rehabilitation Study',
      college: 'College of Fisheries and Marine Sciences (CFMS)',
    }),
    {
      programTitle: 'MSUN RIIDE - Blue Economy and Marine Resources',
      projectTitle: 'Mangrove Rehabilitation Study',
      college: 'College of Fisheries and Marine Sciences (CFMS)',
    },
  )

  // A record saved before the two fields were split still carries its single title as the
  // project title - never as the program title.
  assert.deepEqual(
    endorsementProposalPrefill({ proposalTitle: 'Legacy Single Title', college: 'CFMS' }),
    { programTitle: '', projectTitle: 'Legacy Single Title', college: 'CFMS' },
  )

  // Neither value ever moves into the other field, and an empty program title stays empty.
  const prefill = endorsementProposalPrefill({
    programTitle: 'A Program',
    projectTitle: 'A Project',
  })
  assert.equal(prefill.programTitle, 'A Program')
  assert.equal(prefill.projectTitle, 'A Project')

  assert.deepEqual(endorsementProposalPrefill(), {
    programTitle: '',
    projectTitle: '',
    college: '',
  })
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
  // The obsolete ORPS Director configuration can never block approval: the option that
  // used to carry it is gone, and passing it changes nothing.
  assert.equal(approveBlockedReason(pending, { settingsComplete: false }), '')
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

test('the obsolete letter-settings helpers are gone from the module', () => {
  // Removing them is what makes "a missing ORPS Director cannot block an endorsement"
  // structural instead of a rule someone has to remember: no client helper is left that
  // fetches, saves or reports that configuration.
  assert.equal(endorsementModule.fetchLetterSettings, undefined)
  assert.equal(endorsementModule.saveLetterSettings, undefined)
  assert.equal(endorsementModule.letterSettingsNotice, undefined)
  assert.equal(endorsementModule.buildLetterSettingsPayload, undefined)
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
