/**
 * soDraft.js
 * ---------------------------------------------------------------------------
 * The single definition of what an "SO draft" is.
 *
 * A draft is stored on the proposal itself (no separate table): the OC form's
 * fields are written while the proposal stays at FOR_OC_APPROVAL and the SO date
 * is deliberately left empty. Both the final-approval page and the Chancellor
 * dashboard read the state through these helpers, so the two views can never
 * disagree about whether a draft exists.
 *
 * `needsBudget` is intentionally not part of the test: legacy/seeded records can
 * carry a budget choice without any OC form ever being saved.
 */

const text = (value) => (value == null ? '' : String(value).trim())

/**
 * Whether the proposal has an SO form saved as a draft: it is still awaiting the
 * Chancellor's final approval, has no issued-SO date, and carries at least an SO
 * number or an SO document.
 *
 * The presence of `soUploadedAt` is what marks a finalized OC approval downstream
 * (OvcafService.hasCompletedOcApproval), so a draft must never set it.
 *
 * @param {object|null|undefined} proposal
 * @returns {boolean}
 */
export function isSoDraft(proposal) {
  if (!proposal) return false
  const status = text(proposal.status).toUpperCase()
  if (status !== 'FOR_OC_APPROVAL') return false
  if (proposal.soUploadedAt) return false
  return Boolean(text(proposal.soNumber) || text(proposal.soFilePath))
}

/** The readable name of the SO document staged on the draft, or an empty string. */
export function soDraftFileLabel(proposal) {
  const name = text(proposal?.soFileName)
  if (name) return name
  const path = text(proposal?.soFilePath).replace(/\\/g, '/')
  return path ? path.split('/').pop() : ''
}

/**
 * A stable representation of the SO form's values, used to decide whether the
 * form has unsaved changes. Values are normalized so whitespace alone never
 * counts as an edit.
 *
 * @param {{soNumber?: string, needsBudget?: boolean|null, chancellorNotes?: string, fileName?: string}} form
 * @returns {string}
 */
export function draftSnapshot({ soNumber, needsBudget, chancellorNotes, fileName } = {}) {
  return JSON.stringify({
    soNumber: text(soNumber),
    needsBudget: needsBudget === true ? 'true' : needsBudget === false ? 'false' : '',
    chancellorNotes: text(chancellorNotes),
    fileName: text(fileName),
  })
}
