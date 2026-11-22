/**
 * dashboardProposals.js
 * ---------------------------------------------------------------------------
 * The one rule every role dashboard uses for its proposal preview.
 *
 * A dashboard preview shows at most five DISTINCT proposals, newest first. The
 * full list stays available to the dashboard for its role-specific counts and
 * for the "View all" destination; only the preview is shortened.
 *
 * The date field differs by section: a "newest submissions" table sorts by
 * createdAt, while a status queue (endorsed, revision, budget) sorts by the
 * moment the status last changed (updatedAt). `dateField` selects the primary
 * field; the remaining known date fields are fallbacks so an older record that
 * lacks one still orders sensibly. Ties fall back to the proposal id, then to
 * the original order, so the result is deterministic.
 *
 * Verified without a browser - see dashboardProposals.test.js.
 */

/** The maximum number of proposals a dashboard preview may show. */
export const DASHBOARD_PROPOSAL_LIMIT = 5

/** The date fields a proposal record may carry, most specific first after the requested one. */
const DATE_FALLBACKS = ['updatedAt', 'createdAt', 'submittedAt', 'submissionDate']

function timestamp(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

/**
 * The recency of one proposal, in milliseconds; 0 when no usable date exists.
 *
 * `fallbackDateFields` replaces the default fallback order. A caller that sorts by a
 * specific event (for example the submission date) can pass only the acceptable
 * fallbacks, so the order can never silently fall through to an unrelated timestamp
 * such as the last edit.
 */
export function proposalRecency(proposal, dateField = 'updatedAt', fallbackDateFields = DATE_FALLBACKS) {
  if (!proposal || typeof proposal !== 'object') return 0
  const fallbacks = Array.isArray(fallbackDateFields) ? fallbackDateFields : DATE_FALLBACKS
  const fields = [dateField, ...fallbacks.filter((field) => field !== dateField)]
  for (const field of fields) {
    const time = timestamp(proposal[field])
    if (time) return time
  }
  return 0
}

/** Numeric id of a proposal, or 0 when it has none. */
function proposalId(proposal) {
  const id = Number(proposal?.id)
  return Number.isFinite(id) ? id : 0
}

/**
 * A new array of the same proposals, newest first. Used for a dashboard's full-listing
 * view so a "View all" table reads in the same order as its five-item preview.
 */
export function sortProposalsNewestFirst(proposals, options = {}) {
  const { dateField = 'updatedAt', fallbackDateFields = DATE_FALLBACKS } = options
  const source = Array.isArray(proposals) ? proposals : []
  return source
    .map((proposal, index) => ({
      proposal,
      index,
      time: proposalRecency(proposal, dateField, fallbackDateFields),
      id: proposalId(proposal),
    }))
    .sort((a, b) => {
      if (b.time !== a.time) return b.time - a.time
      if (b.id !== a.id) return b.id - a.id
      return a.index - b.index
    })
    .map((entry) => entry.proposal)
}

/**
 * The latest proposals for a dashboard preview.
 *
 * @param {Array} proposals the authorized proposals already loaded for the dashboard
 * @param {{ limit?: number, dateField?: string, fallbackDateFields?: string[] }} options
 * @returns {Array} a new array with at most `limit` distinct proposals, newest first
 */
export function latestProposalSnippets(proposals, options = {}) {
  const {
    limit = DASHBOARD_PROPOSAL_LIMIT,
    dateField = 'updatedAt',
    fallbackDateFields = DATE_FALLBACKS,
  } = options
  const safeLimit = Math.max(0, Number.isFinite(Number(limit)) ? Number(limit) : DASHBOARD_PROPOSAL_LIMIT)
  const ordered = sortProposalsNewestFirst(proposals, { dateField, fallbackDateFields })

  const result = []
  const seen = new Set()
  let position = 0
  for (const proposal of ordered) {
    if (result.length >= safeLimit) break
    position += 1
    const key = proposal?.id ?? `position:${position}`
    if (seen.has(key)) continue
    seen.add(key)
    result.push(proposal)
  }
  return result
}
