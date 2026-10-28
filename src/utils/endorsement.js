/**
 * endorsement.js
 * ---------------------------------------------------------------------------
 * The Dean / Department Chair endorsement workflow: the Request for Endorsement a
 * Faculty Proponent raises before starting the full proposal submission, the Dean's
 * decision on it, and the official endorsement letter that is filed in the
 * Proponent's Certifications section.
 *
 * The presentation rules and the client-side validation live here so they can be
 * verified without a browser (npm test), and every request goes through the shared
 * axios client (utils/api.js), which attaches the bearer token. Nothing is decided
 * on the client: the backend re-checks ownership, the Dean's scope, the request
 * status and the letter template on every call, so a modified request cannot bypass
 * what this module shows.
 *
 * Run the rules with: npm test
 */
import api from './api.js'
import {
  CREATE_NEW_PROGRAM_TITLE,
  MAX_PROGRAM_TITLE_LENGTH,
  isCreatingProgramTitle,
} from './programTitleOptions.js'
import { formatTimestamp } from './timestamps.js'

/** Base path of the endorsement endpoints. */
export const ENDORSEMENTS_ENDPOINT = '/api/endorsements'

/** Base path of the Proponent's certification documents. */
export const CERTIFICATIONS_ENDPOINT = '/api/certifications'

/** Business states of an endorsement request. */
export const ENDORSEMENT_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
}

/** Label of each state, as shown to the Proponent and to the Dean. */
export const ENDORSEMENT_STATUS_LABELS = {
  [ENDORSEMENT_STATUS.PENDING]: 'Pending',
  [ENDORSEMENT_STATUS.APPROVED]: 'Approved',
  [ENDORSEMENT_STATUS.REJECTED]: 'Rejected',
}

/** Short explanation of each state. */
export const ENDORSEMENT_STATUS_DESCRIPTIONS = {
  [ENDORSEMENT_STATUS.PENDING]: 'Waiting for the authorized Dean to review the request.',
  [ENDORSEMENT_STATUS.APPROVED]:
    'Endorsed. The official endorsement letter has been issued and is in Certifications.',
  [ENDORSEMENT_STATUS.REJECTED]: 'Not endorsed. The Dean recorded a reason for the decision.',
}

/** The document type an issued endorsement letter is filed under. */
export const DOCUMENT_TYPE_ENDORSEMENT_LETTER = 'ENDORSEMENT_LETTER'

/** Label of a certification document type. */
export function documentTypeLabel(documentType) {
  return documentType === DOCUMENT_TYPE_ENDORSEMENT_LETTER ? 'Endorsement Letter' : 'Document'
}

/** Normalized status of a request ('' when there is none). */
export function normalizeEndorsementStatus(status) {
  return String(status ?? '')
    .trim()
    .toUpperCase()
}

/** Label of a request's state. */
export function endorsementStatusLabel(status) {
  const normalized = normalizeEndorsementStatus(status)
  return ENDORSEMENT_STATUS_LABELS[normalized] || normalized || 'Unknown'
}

/** Explanation of a request's state. */
export function endorsementStatusDescription(status) {
  return ENDORSEMENT_STATUS_DESCRIPTIONS[normalizeEndorsementStatus(status)] || ''
}

/** Tone class of a state, matching the status pills used across RPIS. */
export function endorsementStatusTone(status) {
  const normalized = normalizeEndorsementStatus(status)
  if (normalized === ENDORSEMENT_STATUS.APPROVED) return 'is-approved'
  if (normalized === ENDORSEMENT_STATUS.REJECTED) return 'is-rejected'
  return 'is-pending'
}

/** True while the Dean still has to decide. */
export function isPending(status) {
  return normalizeEndorsementStatus(status) === ENDORSEMENT_STATUS.PENDING
}

/** True once the official letter has been issued. */
export function isApproved(status) {
  return normalizeEndorsementStatus(status) === ENDORSEMENT_STATUS.APPROVED
}

/** True when the Dean rejected the request. */
export function isRejected(status) {
  return normalizeEndorsementStatus(status) === ENDORSEMENT_STATUS.REJECTED
}

/**
 * The counts the dashboard shows for the Proponent's endorsement requests: the total and
 * one count per business state. A request whose status is not one of the three states is
 * counted in the total only, so the numbers never claim more than the records.
 */
export function endorsementSummaryCounts(requests) {
  const list = Array.isArray(requests) ? requests : []
  const counts = { total: list.length, pending: 0, approved: 0, rejected: 0 }
  for (const request of list) {
    const status = normalizeEndorsementStatus(request?.status)
    if (status === ENDORSEMENT_STATUS.PENDING) counts.pending += 1
    else if (status === ENDORSEMENT_STATUS.APPROVED) counts.approved += 1
    else if (status === ENDORSEMENT_STATUS.REJECTED) counts.rejected += 1
  }
  return counts
}

/**
 * How many of the given requests have an issued endorsement letter.
 *
 * <p>Only an approved request carries one - the backend creates the letter (and its
 * Certifications entry) inside the approval transaction - so a pending or rejected
 * request is never counted as an issued document here, and no separate Certifications
 * request is needed to know the number.
 */
export function issuedLetterCount(requests) {
  return (Array.isArray(requests) ? requests : []).filter(
    (request) => request?.hasLetter === true,
  ).length
}

/** Filter options of the Dean's request list. */
export const ENDORSEMENT_FILTERS = [
  { value: '', label: 'All requests' },
  { value: ENDORSEMENT_STATUS.PENDING, label: 'Pending' },
  { value: ENDORSEMENT_STATUS.APPROVED, label: 'Approved' },
  { value: ENDORSEMENT_STATUS.REJECTED, label: 'Rejected' },
]

/**
 * The sections of the consolidated My Endorsements page, in the order they are shown.
 *
 * <p>They are declared here - not inside the page - so the page, the `?tab=` deep links
 * (the redirects from the old Request for Endorsement and Certifications URLs) and the
 * tests all agree on the ids and the wording, and so a typo cannot quietly hide a whole
 * section of the workflow.
 */
export const ENDORSEMENT_TABS = [
  {
    id: 'requests',
    label: 'My Requests',
    description:
      'Every Request for Endorsement you have raised, with its current status and the official letter once your Dean has endorsed it.',
  },
  {
    id: 'new',
    label: 'Request Endorsement',
    description:
      'Ask your College Dean / Department Chair to endorse your project before you fill in the full proposal. Only the preliminary information below is needed at this stage.',
  },
  {
    id: 'certifications',
    label: 'Issued Certifications',
    description:
      'The official endorsement letters your Dean has issued. A letter is filed here automatically once a request is approved — you never upload it yourself.',
  },
]

/** The section the consolidated page opens on: the tracking list of requests. */
export const DEFAULT_ENDORSEMENT_TAB = 'requests'

/**
 * The section a `?tab=` query value means. A missing, unknown or non-string value falls
 * back to the default section, so an old or hand-edited link can never open a blank page.
 */
export function endorsementTabFromQuery(value) {
  const candidate = typeof value === 'string' ? value.trim() : ''
  return ENDORSEMENT_TABS.some((tab) => tab.id === candidate) ? candidate : DEFAULT_ENDORSEMENT_TAB
}

/** The section definition of an id, or the default one when the id is unknown. */
export function endorsementTab(value) {
  const id = endorsementTabFromQuery(value)
  return ENDORSEMENT_TABS.find((tab) => tab.id === id)
}

/** Label of a funding program (call type) as reported by the backend. */
export function fundingProgramLabel(request) {
  const label = request?.fundingProgramLabel
  if (label) return label
  const program = String(request?.fundingProgram ?? '').trim()
  return program || 'Unspecified funding program'
}

/**
 * Why the Approve action is unavailable, or '' when it is available.
 *
 * The backend is the authority; this only explains the state on screen, so a Dean
 * never clicks Approve and then reads an error.
 */
export function approveBlockedReason(request, { hasSignature = true } = {}) {
  if (!isPending(request?.status)) {
    return `This request is already ${endorsementStatusLabel(request?.status).toLowerCase()}.`
  }
  if (request?.letterTemplateAvailable === false) {
    return `No official endorsement letter is configured for ${fundingProgramLabel(request)} yet, so it cannot be endorsed.`
  }
  if (!hasSignature) {
    return 'Save your electronic signature in Manage Profile → Electronic Signature before approving: the official letter is signed with it.'
  }
  return ''
}

/** The suggested file name of an issued letter. */
export function suggestedLetterFileName(referenceNumber) {
  const reference = String(referenceNumber ?? '').trim()
  return reference ? `Endorsement-Letter-${reference}.pdf` : 'Endorsement-Letter.pdf'
}

/** A date (YYYY-MM-DD) as "Month D, YYYY"; '' when it is missing. */
export function formatEndorsementDate(value) {
  const text = String(value ?? '').trim()
  if (!text) return ''
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
}

/** A timestamp as a readable local date and time; '' when it is missing. */
export function formatEndorsementTimestamp(value) {
  // The format itself is the application's shared one (utils/timestamps.js), so an
  // endorsement timestamp and a notification timestamp read the same way.
  return formatTimestamp(value)
}

/** Today as YYYY-MM-DD, the default of the endorsement date field. */
export function todayISO() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/** True when the given string is blank. */
function isBlank(value) {
  return !String(value ?? '').trim()
}

/** Maximum length of the Project Title; it matches the stored column. */
export const MAX_PROJECT_TITLE_LENGTH = 500

/**
 * Client-side validation of the Request for Endorsement form. It mirrors the
 * backend rules so the Proponent sees the problem before submitting; the backend
 * validates the same payload again and is the authority.
 *
 * <p>Program Title and Project Title are validated as the two independent fields they
 * are: the Program Title follows the Create Proposal rule (optional, but the
 * "create a new program title" flow must produce a value once it is used) and the
 * Project Title is always required. Neither value can satisfy the other's rule.
 */
export function validateEndorsementRequest(form = {}) {
  const errors = {}

  const programTitle = String(form.programTitle ?? '').trim()
  if (isCreatingProgramTitle(form.programTitleChoice) && programTitle === '') {
    errors.programTitle = 'Enter the new program title, or cancel to choose an existing one.'
  } else if (programTitle.length > MAX_PROGRAM_TITLE_LENGTH) {
    errors.programTitle = `The program title must be ${MAX_PROGRAM_TITLE_LENGTH} characters or fewer.`
  }

  const projectTitle = String(form.projectTitle ?? '').trim()
  if (projectTitle === '') {
    errors.projectTitle = 'The project title is required.'
  } else if (projectTitle.length > MAX_PROJECT_TITLE_LENGTH) {
    errors.projectTitle = `The project title must be ${MAX_PROJECT_TITLE_LENGTH} characters or fewer.`
  }

  if (isBlank(form.proposalAnnouncementId)) {
    errors.proposalAnnouncementId = 'Select the Call for Proposal being applied to.'
  }

  if (isBlank(form.college)) {
    errors.college = 'Select the implementing college/department.'
  }

  return errors
}

/** True when the payload passes the client-side validation. */
export function isEndorsementRequestValid(form = {}) {
  return Object.keys(validateEndorsementRequest(form)).length === 0
}

/**
 * The payload sent to the backend, trimmed. Program Title and Project Title travel as
 * two separate values - the program title is omitted when the field was left empty
 * (it is optional) and is never copied into the project title.
 */
export function buildEndorsementRequestPayload(form = {}) {
  const programTitle = String(form.programTitle ?? '').trim()
  return {
    // The sentinel that marks "create a new program title" is a control value, never a
    // title: only what the Proponent actually typed can be submitted.
    programTitle:
      programTitle === '' || isCreatingProgramTitle(programTitle) ? null : programTitle,
    projectTitle: String(form.projectTitle ?? '').trim(),
    proposalAnnouncementId: form.proposalAnnouncementId ? Number(form.proposalAnnouncementId) : null,
    college: String(form.college ?? '').trim(),
    department: isBlank(form.department) ? null : String(form.department).trim(),
    researchSummary: isBlank(form.researchSummary) ? null : String(form.researchSummary).trim(),
    notes: isBlank(form.notes) ? null : String(form.notes).trim(),
  }
}

/**
 * The values the Create Proposal form is prefilled with from an approved endorsement.
 *
 * <p>The keys are the ones the Create Proposal form's `loadProposal` reads (the proposal
 * DTO shape, camelCase), and the two titles are carried over independently: the program
 * title is never substituted for the project title or the other way round. An
 * endorsement saved before the two fields were split carries its single title as the
 * project title (`proposalTitle`), which is what it always was.
 */
export function endorsementProposalPrefill(endorsement = {}) {
  const programTitle = String(endorsement?.programTitle ?? '').trim()
  const projectTitle = String(endorsement?.projectTitle ?? endorsement?.proposalTitle ?? '').trim()
  return {
    programTitle,
    projectTitle,
    college: String(endorsement?.college ?? '').trim(),
  }
}

/** Validation of the approval form: the endorsement details the Dean confirms. */
export function validateApproval(form = {}) {
  const errors = {}

  if (isBlank(form.endorserName)) {
    errors.endorserName = 'The full name of the endorser is required.'
  }
  if (isBlank(form.endorserDesignation)) {
    errors.endorserDesignation = 'The designation/title is required.'
  }
  if (isBlank(form.endorsementDate)) {
    errors.endorsementDate = 'The date of the endorsement is required.'
  }

  return errors
}

/** Validation of the rejection form: the reason is required. */
export function validateRejection(form = {}) {
  const errors = {}
  if (isBlank(form.rejectionReason)) {
    errors.rejectionReason = 'A reason is required to reject an endorsement request.'
  }
  return errors
}

/** The payload of an approval, trimmed. */
export function buildApprovalPayload(form = {}) {
  return {
    endorserName: String(form.endorserName ?? '').trim(),
    endorserDesignation: String(form.endorserDesignation ?? '').trim(),
    endorsementDate: String(form.endorsementDate ?? '').trim(),
  }
}

/** The payload of a rejection, trimmed. */
export function buildRejectionPayload(form = {}) {
  return { rejectionReason: String(form.rejectionReason ?? '').trim() }
}

// ==================== API ====================
// Every call goes through the shared axios client, which attaches the bearer token.
// The backend resolves the caller from that token, so nothing here can act on
// another account's behalf.

/** Every request the authenticated caller may see (own, or Dean-scoped). */
export async function fetchEndorsementRequests({ status = '' } = {}) {
  const params = status ? { status } : {}
  const response = await api.get(ENDORSEMENTS_ENDPOINT, { params })
  return Array.isArray(response.data) ? response.data : []
}

/** One request. */
export async function fetchEndorsementRequest(id) {
  const response = await api.get(`${ENDORSEMENTS_ENDPOINT}/${id}`)
  return response.data
}

/** The audit trail of one request. */
export async function fetchEndorsementEvents(id) {
  const response = await api.get(`${ENDORSEMENTS_ENDPOINT}/${id}/events`)
  return Array.isArray(response.data) ? response.data : []
}

/** Raises a Request for Endorsement. */
export async function createEndorsementRequest(payload) {
  const response = await api.post(ENDORSEMENTS_ENDPOINT, payload)
  return response.data
}

/** Approves a pending request and issues the official letter. */
export async function approveEndorsementRequest(id, payload) {
  const response = await api.put(`${ENDORSEMENTS_ENDPOINT}/${id}/approve`, payload)
  return response.data
}

/** Rejects a pending request. */
export async function rejectEndorsementRequest(id, payload) {
  const response = await api.put(`${ENDORSEMENTS_ENDPOINT}/${id}/reject`, payload)
  return response.data
}

/** Explicitly reissues an issued letter as a new version. */
export async function reissueEndorsementLetter(id, payload) {
  const response = await api.put(`${ENDORSEMENTS_ENDPOINT}/${id}/reissue`, payload)
  return response.data
}

/** Links an approved endorsement to the proposal the Proponent continued to. */
export async function linkEndorsementToProposal(id, proposalId) {
  const response = await api.put(`${ENDORSEMENTS_ENDPOINT}/${id}/proposal`, { proposalId })
  return response.data
}

/** The Proponent's certification documents. */
export async function fetchCertifications() {
  const response = await api.get(CERTIFICATIONS_ENDPOINT)
  return Array.isArray(response.data) ? response.data : []
}

/** Fetches an issued endorsement letter as a Blob. */
export async function fetchEndorsementDocument(id, { download = false } = {}) {
  const response = await api.get(`${ENDORSEMENTS_ENDPOINT}/${id}/document`, {
    params: download ? { download: true } : {},
    responseType: 'blob',
  })
  return response.data
}

/** Fetches a certification document as a Blob. */
export async function fetchCertificationDocument(id, { download = false } = {}) {
  const response = await api.get(`${CERTIFICATIONS_ENDPOINT}/${id}/document`, {
    params: download ? { download: true } : {},
    responseType: 'blob',
  })
  return response.data
}

/**
 * Opens a fetched document Blob in a new tab (the in-app preview) or saves it
 * (the download). Preview and download request the same stored file, so what the
 * user sees is what they get.
 */
export function openDocumentBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  window.open(url, '_blank')
  setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  return url
}

/** Saves a fetched document Blob under the given file name. */
export function saveDocumentBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

