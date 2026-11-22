/**
 * rejectionQueue.js
 * ---------------------------------------------------------------------------
 * The read model of the ORPS Rejected Proposals queue.
 *
 * A rejection reaches ORPS in one of two shapes:
 *  - a reviewing office handed it over: status RETURNED_TO_RPS with
 *    return_kind = 'REJECTION';
 *  - it is already finalized: status REJECTED (ORPS forwarded it, or ORPS itself
 *    made the decision).
 *
 * The rejecting office is displayed but never used to classify the row: an ordinary
 * revision return also carries returnedByOffice.
 */

/** True while a reviewing office's rejection is waiting for ORPS. */
export function isPendingRejection(proposal) {
  return String(proposal?.status || '').toUpperCase() === 'RETURNED_TO_RPS'
    && String(proposal?.returnKind || '').toUpperCase() === 'REJECTION'
}

/** True once the rejection has been finalized by ORPS (status REJECTED). */
export function isFinalRejection(proposal) {
  return String(proposal?.status || '').toUpperCase() === 'REJECTED'
}

/** True for any row in the Rejected Proposals category, pending or finalized. */
export function isRejectedProposal(proposal) {
  return isPendingRejection(proposal) || isFinalRejection(proposal)
}

/**
 * The Rejected Proposals status filter, matched against the stored workflow record:
 *  - ALL      any rejection (pending hand-off or finalized);
 *  - RETURNED exactly the pending hand-off (RETURNED_TO_RPS + return_kind REJECTION);
 *  - REJECTED exactly the finalized status.
 * An unknown/empty value behaves like ALL.
 */
export function matchesRejectionStatus(proposal, filter) {
  const value = String(filter || 'ALL').toUpperCase()
  if (value === 'RETURNED') return isPendingRejection(proposal)
  if (value === 'REJECTED') return isFinalRejection(proposal)
  return isRejectedProposal(proposal)
}

/** The office that made the rejection, never a blank cell. */
export function rejectionOfficeOf(proposal) {
  const office = String(proposal?.returnedByOffice || '').trim().toUpperCase()
  return office || 'ORPS'
}

/** The reason recorded by the rejecting office, capped for a table row. */
export function rejectionRemarksOf(proposal, maxLength = 220) {
  const remarks = String(proposal?.returnRemarks || proposal?.remarks || '').trim()
  if (!remarks) return ''
  return remarks.length > maxLength ? `${remarks.slice(0, maxLength)}…` : remarks
}

/** The distinct rejecting offices in the given rows, alphabetically, for the filter. */
export function rejectionOfficesOf(proposals) {
  const offices = new Set()
  ;(Array.isArray(proposals) ? proposals : []).forEach((proposal) => {
    if (isRejectedProposal(proposal)) offices.add(rejectionOfficeOf(proposal))
  })
  return Array.from(offices).sort((a, b) => a.localeCompare(b))
}
