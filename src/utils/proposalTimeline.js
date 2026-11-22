/**
 * proposalTimeline.js
 * ---------------------------------------------------------------------------
 * Builds the proposal Status Timeline shown on the OVCRIGE review page.
 *
 * The timeline is a union of:
 *   - the six workflow milestones the page has always shown, and
 *   - the real rows from `proposal_status_history` (newest first from the API).
 *
 * A milestone takes its timestamp and actor from the latest history row that reached
 * its target status; older rows for the same status remain as separate entries so no
 * record is dropped. `BASELINE_SEED` rows are not events (the migration derived them
 * from an arbitrary updated_at), so they are never used for ordering or display.
 *
 * Entries are sorted by the raw event timestamp (never a formatted date string),
 * newest first, with `changed_at` ties broken by the history id and then by workflow
 * stage. Entries with no known timestamp sort after timestamped ones, by stage rank,
 * so the order is still newest-first where the data allows.
 *
 * Run the rules with: npm test
 */
import { proposalStatusLabel } from './proposalStatus.js'

/** The six milestones the OVCRIGE page has always displayed, in workflow order. */
const MILESTONES = [
  {
    key: 'submitted',
    label: 'Proposal Submitted',
    targetStatuses: ['FOR_REVIEW', 'SUBMITTED'],
    rank: 1,
    activeLabel: 'Completed',
  },
  {
    key: 'endorsed',
    label: 'RPS Endorsed',
    targetStatuses: ['ENDORSED'],
    rank: 2,
    activeLabel: 'Completed',
  },
  {
    key: 'forwarded-rec',
    label: 'Forwarded to REC',
    targetStatuses: ['UNDER_REVIEW'],
    rank: 3,
    activeLabel: 'In Progress',
  },
  {
    key: 'rec-approved',
    label: 'REC Approved',
    targetStatuses: ['REC_APPROVED'],
    rank: 4,
    activeLabel: 'Current',
  },
  {
    key: 'forwarded-oc',
    label: 'Forwarded to Chancellor',
    targetStatuses: ['FOR_OC_APPROVAL'],
    rank: 5,
    activeLabel: 'In Progress',
  },
  {
    key: 'final-approval',
    label: 'Final Approval',
    targetStatuses: ['APPROVED'],
    rank: 6,
    activeLabel: 'Completed',
  },
]

/** The workflow position of the statuses the timeline knows about. */
const STATUS_RANK = {
  FOR_REVIEW: 1,
  SUBMITTED: 1,
  ENDORSED: 2,
  UNDER_REVIEW: 3,
  REC_APPROVED: 4,
  OVC_APPROVED: 4,
  FOR_OVCAF_APPROVAL: 5,
  FOR_OC_APPROVAL: 5,
  APPROVED: 6,
}

const normalize = (value) => String(value || '').trim().toUpperCase().replace(/[\s-]+/g, '_')

/** Milliseconds for a timestamp, or null when it is missing/unparseable. */
function timeOf(value) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : null
}

/** Real history rows only; baseline seed rows are not events. */
function realHistory(history) {
  if (!Array.isArray(history)) return []
  return history
    .filter((row) => row && String(row.source || '').toUpperCase() !== 'BASELINE_SEED')
    .slice()
    .sort((a, b) => {
      const at = timeOf(a.changedAt) ?? 0
      const bt = timeOf(b.changedAt) ?? 0
      if (bt !== at) return bt - at
      return (Number(b.id) || 0) - (Number(a.id) || 0)
    })
}

/** A human label for a history transition. */
function historyLabel(row) {
  const toStatus = String(row.toStatus || '').trim()
  return proposalStatusLabel(toStatus) || toStatus || 'Status updated'
}

/**
 * @param {object} proposal the proposal returned by GET /api/proposals/{id}
 * @param {Array} history rows returned by GET /api/proposals/{id}/status-history
 * @returns {Array} timeline entries, newest first
 */
export function buildProposalTimeline(proposal, history = []) {
  const source = proposal && typeof proposal === 'object' ? proposal : {}
  const rows = realHistory(history)
  const currentStatus = String(source.status || '').trim()
  const currentRank = STATUS_RANK[normalize(currentStatus)] || 0

  const reachedStatuses = new Set(rows.map((row) => normalize(row.toStatus)))
  const consumed = new Set()

  const reached = {
    submitted: true,
    endorsed:
      currentRank >= 2 ||
      reachedStatuses.has('ENDORSED') ||
      Boolean(source.endorsedAt),
    'forwarded-rec': currentRank >= 3 || reachedStatuses.has('UNDER_REVIEW'),
    'rec-approved':
      currentRank >= 4 ||
      reachedStatuses.has('REC_APPROVED') ||
      reachedStatuses.has('OVC_APPROVED'),
    'forwarded-oc': currentRank >= 5 || reachedStatuses.has('FOR_OC_APPROVAL'),
    'final-approval': currentRank >= 6 || reachedStatuses.has('APPROVED'),
  }

  const fallbackTimestamps = {
    submitted: source.submittedAt || source.createdAt || null,
    endorsed: source.endorsedAt || null,
    'final-approval': source.soUploadedAt || null,
  }

  const entries = []

  for (const milestone of MILESTONES) {
    if (!reached[milestone.key]) continue

    // The latest real row that reached this milestone supplies its event time.
    const matchedRow = rows.find(
      (row) =>
        !consumed.has(row) &&
        milestone.targetStatuses.includes(normalize(row.toStatus)),
    )
    if (matchedRow) consumed.add(matchedRow)

    const timestamp = matchedRow?.changedAt || fallbackTimestamps[milestone.key] || null
    const time = timeOf(timestamp)
    const state =
      milestone.key === 'submitted'
        ? 'completed'
        : currentRank === milestone.rank
          ? 'active'
          : 'completed'

    entries.push({
      key: `milestone-${milestone.key}`,
      kind: 'milestone',
      label: milestone.label,
      timestamp,
      time,
      state,
      detail: time ? '' : state === 'active' ? milestone.activeLabel : 'Completed',
      actorName: matchedRow?.actorName || '',
      remarks: matchedRow?.remarks || '',
      rank: milestone.rank,
      historyId: matchedRow ? Number(matchedRow.id) || 0 : 0,
    })
  }

  for (const row of rows) {
    if (consumed.has(row)) continue
    const time = timeOf(row.changedAt)
    entries.push({
      key: `history-${row.id ?? `${row.changedAt}-${row.toStatus}`}`,
      kind: 'history',
      label: historyLabel(row),
      timestamp: row.changedAt || null,
      time,
      state: 'completed',
      detail: time ? '' : 'Recorded',
      actorName: row.actorName || '',
      remarks: row.remarks || '',
      rank: STATUS_RANK[normalize(row.toStatus)] || 0,
      historyId: Number(row.id) || 0,
    })
  }

  // Some legacy milestones have no recorded event timestamp. They still need to sort
  // between the stages that do: place each one next to its nearest dated milestone,
  // just before a later stage or just after an earlier one. This affects ordering only;
  // the UI keeps showing "Completed" instead of inventing a date.
  const datedMilestones = entries.filter(
    (entry) => entry.kind === 'milestone' && entry.time !== null,
  )
  for (const entry of entries) {
    if (entry.kind !== 'milestone') {
      entry.sortTime = entry.time
      continue
    }
    if (entry.time !== null) {
      entry.sortTime = entry.time
      continue
    }
    let nearest = null
    for (const known of datedMilestones) {
      const distance = Math.abs(known.rank - entry.rank)
      if (distance === 0) continue
      if (!nearest || distance < nearest.distance) {
        nearest = { known, distance }
      }
    }
    entry.sortTime = nearest
      ? nearest.known.time + (nearest.known.rank > entry.rank ? -nearest.distance : nearest.distance) * 1000
      : null
  }

  return entries
    .map((entry, index) => ({ ...entry, index }))
    .sort((a, b) => {
      // The proposal's current stage is the newest event by definition, even when the
      // history does not (yet) carry an exact timestamp for it.
      if (a.state === 'active' && b.state !== 'active') return -1
      if (b.state === 'active' && a.state !== 'active') return 1
      if (a.sortTime !== null && b.sortTime !== null && a.sortTime !== b.sortTime) {
        return b.sortTime - a.sortTime
      }
      if (a.sortTime !== null && b.sortTime === null) return -1
      if (a.sortTime === null && b.sortTime !== null) return 1
      // Same time (or both unknown): newer history id first, then later stage.
      if (b.historyId !== a.historyId) return b.historyId - a.historyId
      if (b.rank !== a.rank) return b.rank - a.rank
      return a.index - b.index
    })
}
