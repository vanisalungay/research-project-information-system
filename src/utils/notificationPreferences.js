/**
 * notificationPreferences.js
 * ---------------------------------------------------------------------------
 * The stable vocabulary for Profile Settings -> Notifications.
 *
 * The two account lists store these keys. Older accounts stored the visible labels;
 * normalizeKeys maps them so no database rewrite is needed. The labels are display
 * text only - changing a label must never change what a saved preference means.
 */

export const PREFERENCE_KEYS = Object.freeze({
  NEW_ASSIGNED: 'proposal.new_assigned',
  REVISION_SUBMITTED: 'proposal.revision_submitted',
  ATTACHMENT_UPLOADED: 'proposal.attachment_uploaded',
  STATUS_UPDATED: 'proposal.status_updated',
  DEADLINE_EVALUATION: 'deadline.evaluation',
  DEADLINE_REVISION: 'deadline.revision',
})

const LEGACY_LABELS = Object.freeze({
  'New Proposal Assigned': PREFERENCE_KEYS.NEW_ASSIGNED,
  'Proponent Submitted Revision': PREFERENCE_KEYS.REVISION_SUBMITTED,
  'New Attachment Uploaded': PREFERENCE_KEYS.ATTACHMENT_UPLOADED,
  'Proposal Status Updated': PREFERENCE_KEYS.STATUS_UPDATED,
  'Evaluation Deadline Reminders': PREFERENCE_KEYS.DEADLINE_EVALUATION,
  'Revision Deadline Reminders': PREFERENCE_KEYS.DEADLINE_REVISION,
})

export const PROPOSAL_OPTIONS = Object.freeze([
  {
    key: PREFERENCE_KEYS.NEW_ASSIGNED,
    label: 'New Proposal Assigned',
    description: 'Receive an alert when a new proposal reaches your office.',
  },
  {
    key: PREFERENCE_KEYS.REVISION_SUBMITTED,
    label: 'Proponent Submitted Revision',
    description: 'Receive an alert when a Proponent resubmits a returned proposal.',
  },
  {
    key: PREFERENCE_KEYS.ATTACHMENT_UPLOADED,
    label: 'New Attachment Uploaded',
    description: 'Receive an alert when a meaningful attachment is added.',
  },
  {
    key: PREFERENCE_KEYS.STATUS_UPDATED,
    label: 'Proposal Status Updated',
    description: 'Receive an alert when an optional proposal status update occurs.',
  },
])

export const DEADLINE_OPTIONS = Object.freeze([
  {
    key: PREFERENCE_KEYS.DEADLINE_REVISION,
    label: 'Revision Deadline Reminders',
    description: 'Receive a reminder while a Proponent revision deadline is approaching.',
    available: true,
  },
  {
    key: PREFERENCE_KEYS.DEADLINE_EVALUATION,
    label: 'Evaluation Deadline Reminders',
    description: 'No evaluation deadline is recorded in RPIS yet, so this reminder is unavailable.',
    available: false,
  },
])

/** The optional proposal keys that are enabled before a user saves the tab. */
export const DEFAULT_PROPOSAL_KEYS = Object.freeze(
  PROPOSAL_OPTIONS.map((option) => option.key),
)

/** Maps legacy labels to stable keys, trims, de-duplicates and drops blanks. */
export function normalizeKeys(values) {
  const result = []
  const seen = new Set()
  for (const value of Array.isArray(values) ? values : []) {
    if (typeof value !== 'string' || !value.trim()) continue
    const trimmed = value.trim()
    const key = LEGACY_LABELS[trimmed] || trimmed
    if (!seen.has(key)) {
      seen.add(key)
      result.push(key)
    }
  }
  return result
}

/** Effective form state: an unsaved account sees the optional proposal defaults on. */
export function initialProposalSelection(user) {
  const saved = normalizeKeys(user?.notifyProposals)
  if (user?.notifyPreferencesConfigured === true) return saved
  return saved.length > 0 ? saved : [...DEFAULT_PROPOSAL_KEYS]
}

export function initialDeadlineSelection(user) {
  return normalizeKeys(user?.notifyDeadlines)
}

export function preferenceLabel(key) {
  const option = PROPOSAL_OPTIONS.find((item) => item.key === key)
    || DEADLINE_OPTIONS.find((item) => item.key === key)
  return option?.label || key
}
