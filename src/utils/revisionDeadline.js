/**
 * revisionDeadline.js
 * Revision-deadline helpers for the Proponent views.
 *
 * The eligibility rule mirrors the backend (ProposalService enforces it when a revised
 * proposal is resubmitted): a revision can still be revised and resubmitted while the
 * RPS-set deadline has NOT passed. A deadline later today is therefore still eligible, a
 * deadline already in the past is not, and a revision without a deadline never expires.
 * The same "passed" test is what the Revisions page shows as overdue.
 *
 * Nothing here changes a stored status, a deadline value, or the deadline calculation
 * used elsewhere - it only reads the saved values.
 */

/** Statuses that mean the proposal is with the proponent for revision. */
export const PENDING_REVISION_STATUSES = ['RPS_RETURNED', 'REC_REVISION', 'REVISION']

/** Milliseconds for a usable deadline value, or null when missing/unparsable. */
export function revisionDeadlineTime(deadline) {
  if (!deadline) return null
  const time = new Date(deadline).getTime()
  return Number.isNaN(time) ? null : time
}

/**
 * True only once the deadline is strictly in the past - the same comparison the backend
 * uses (`LocalDateTime.now().isAfter(deadline)`), so a deadline that is still ahead
 * (including later today) remains eligible.
 */
export function isRevisionDeadlinePassed(deadline, now = Date.now()) {
  const time = revisionDeadlineTime(deadline)
  return time !== null && time < now
}

/** Whole days remaining until the deadline (0 when passed, missing or unparsable). */
export function revisionDaysLeft(deadline, now = Date.now()) {
  const time = revisionDeadlineTime(deadline)
  if (time === null) return 0
  return Math.max(0, Math.floor((time - now) / (1000 * 60 * 60 * 24)))
}

/** "1 day left" / "2 days left" - singular only for exactly one remaining day. */
export function revisionDaysLeftLabel(days) {
  const value = Number.isFinite(days) ? days : 0
  return `${value} ${value === 1 ? 'day' : 'days'} left`
}

/**
 * A revision still counts as pending only while it is in a revision status AND its
 * deadline has not passed. The stored status is never modified by this check.
 */
export function isPendingRevision(proposal, now = Date.now()) {
  if (!proposal || !PENDING_REVISION_STATUSES.includes(proposal.status)) return false
  return !isRevisionDeadlinePassed(proposal.revisionDeadline, now)
}

/** Number of revisions the proponent can still revise, counted once each. */
export function countPendingRevisions(proposals, now = Date.now()) {
  return (proposals || []).filter((proposal) => isPendingRevision(proposal, now)).length
}

/**
 * Display order for the proponent revision queue: revisions that can still be revised come
 * first (nearest deadline first), deadline-expired revisions follow (most recently expired
 * first). A request with no usable deadline is not expired, so it stays with the actionable
 * group but sorts after the dated ones. Equal keys return 0, so the previous relative order
 * is preserved (Array#sort is stable).
 */
export function compareRevisionDeadlines(aDeadline, bDeadline, now = Date.now()) {
  const aExpired = isRevisionDeadlinePassed(aDeadline, now)
  const bExpired = isRevisionDeadlinePassed(bDeadline, now)
  if (aExpired !== bExpired) return aExpired ? 1 : -1

  const aTime = revisionDeadlineTime(aDeadline)
  const bTime = revisionDeadlineTime(bDeadline)
  if (aTime === null && bTime === null) return 0
  if (aTime === null) return 1
  if (bTime === null) return -1

  // Active: soonest deadline first. Expired: most recently expired first.
  return aExpired ? bTime - aTime : aTime - bTime
}
