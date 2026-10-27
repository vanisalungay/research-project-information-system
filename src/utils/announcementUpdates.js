/**
 * announcementUpdates.js
 * ---------------------------------------------------------------------------
 * The deadline extension notice of a Call for Proposal, as the Proponent Side shows it.
 *
 * A Call for Proposal keeps only its CURRENT values, so a deadline extension would
 * otherwise erase the deadline it replaced. The backend appends every significant,
 * public-facing change to `proposal_announcement_updates` and returns that history
 * (oldest first) as `announcement.updates` on the read-only Proponent details view.
 *
 * Only those recorded entries are read here: each extension event carries the deadline it
 * replaced (`previousEndDate`) next to the deadline it announced (`newEndDate`), so the
 * previous deadline is never taken from the call's current (already updated) deadline and
 * never from an older event.
 *
 * Run the rules with: npm test
 */
import { announcementStatus, localTodayISO } from './announcementStatus.js'
import { formatCallDate } from './callForProposal.js'

/** The recorded type of a deadline that was moved later (see ProposalAnnouncementUpdate). */
export const UPDATE_DEADLINE_EXTENDED = 'DEADLINE_EXTENDED'

/** One recorded change, reduced to the deadlines it moved between. */
function normalizeEntry(update) {
  return {
    id: update?.id ?? null,
    type: String(update?.updateType ?? '')
      .trim()
      .toUpperCase(),
    // The deadline the call had before this change (empty for the first publication).
    previousEndDate: update?.previousEndDate || '',
    // The deadline the call had after this change.
    newEndDate: update?.newEndDate || '',
  }
}

/**
 * The recorded changes of one Call for Proposal, oldest first.
 *
 * A call without recorded history yields an empty list: no event is invented for it. When
 * the backend supplied ids the entries are ordered by id - the ids are assigned in
 * insertion order - so the sequence of changes stays chronological even if the array
 * arrives unordered.
 */
export function announcementHistory(announcement) {
  const updates = announcement?.updates
  if (!Array.isArray(updates)) return []
  const entries = updates.map((update) => normalizeEntry(update))
  if (entries.every((entry) => Number.isInteger(entry.id))) {
    entries.sort((a, b) => a.id - b.id)
  }
  return entries
}

/**
 * The most recent recorded deadline extension of a call, or null when the call was never
 * extended. Both deadlines belong to that one extension event: `previousEndDate` is the
 * deadline it replaced and `newEndDate` the deadline it announced.
 */
export function latestDeadlineExtension(announcement) {
  const extensions = announcementHistory(announcement).filter(
    (entry) => entry.type === UPDATE_DEADLINE_EXTENDED && entry.newEndDate,
  )
  return extensions.length ? extensions[extensions.length - 1] : null
}

/**
 * The deadline-extension notice shown in the call details, or null when there is nothing
 * to announce. A notice is produced only when the call has a recorded extension AND is
 * currently Open under the system's own status rule (stored status plus the CURRENT
 * submission window). That is what keeps an officially extended call from being reported
 * as closed merely because its superseded deadline has passed, while a call that really is
 * closed after an extension still shows no "extended until" notice.
 *
 * The previous deadline always comes from the same extension event as the new deadline:
 * an extension from November 12 to November 20 shows November 12, and a later extension
 * from November 20 to November 25 shows November 20 - never the new deadline itself and
 * never the call's very first deadline.
 */
export function extensionNotice(announcement, today = localTodayISO()) {
  const extension = latestDeadlineExtension(announcement)
  if (!extension) return null
  if (announcementStatus(announcement, today) !== 'Open') return null

  return {
    newDeadline: extension.newEndDate,
    previousDeadline: extension.previousEndDate,
    message: `This call for proposal has been extended until ${formatCallDate(extension.newEndDate)}.`,
    previousDeadlineMessage: extension.previousEndDate
      ? `Previous deadline: ${formatCallDate(extension.previousEndDate)}`
      : '',
  }
}
