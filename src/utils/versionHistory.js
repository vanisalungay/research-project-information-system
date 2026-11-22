/**
 * versionHistory.js
 * ---------------------------------------------------------------------------
 * Ordering for the Submission & Revision History.
 *
 * Each version row records the event that created it:
 *  - a revision has a revisionDate (the actual resubmission event);
 *  - an original submission has a submissionDate.
 * createdAt is only a fallback for legacy rows that carry neither, so an unrelated
 * row-creation timestamp cannot pull a version above a newer submission.
 *
 * Newest event first. Equal timestamps fall back to the version number and then the
 * stored id, both descending, so the order is deterministic after a refresh and the
 * same on every proposal. Missing/unparseable timestamps sort last.
 */

export function versionEventTimestamp(version) {
  const raw = version?.revisionDate || version?.submissionDate || version?.createdAt
  if (!raw) return null
  const time = new Date(raw).getTime()
  return Number.isNaN(time) ? null : time
}

function numericDesc(a, b) {
  return (Number(b) || 0) - (Number(a) || 0)
}

/** A new array, newest submission/revision event first. The source is never mutated. */
export function sortVersionsNewestFirst(versions) {
  const list = Array.isArray(versions) ? [...versions] : []
  return list.sort((a, b) => {
    const timeA = versionEventTimestamp(a)
    const timeB = versionEventTimestamp(b)
    if (timeA === null && timeB === null) {
      return numericDesc(a?.versionNumber, b?.versionNumber) || numericDesc(a?.id, b?.id)
    }
    if (timeA === null) return 1
    if (timeB === null) return -1
    if (timeA !== timeB) return timeB - timeA
    return numericDesc(a?.versionNumber, b?.versionNumber) || numericDesc(a?.id, b?.id)
  })
}
