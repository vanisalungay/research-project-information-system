/**
 * announcementForm.js
 * ---------------------------------------------------------------------------
 * Validation and payload rules of the ORPS Call for Proposal modal.
 *
 * Creating a call sends the complete announcement (name, submission window, priority
 * agenda, announcement reminders, additional text), so a new call requires an announcement
 * name. Updating an existing call sends only the submission window and the status: the name
 * and the other restricted fields are not editable any more and are never part of an update
 * payload, so an update never asks the ORPS user for the announcement name again.
 *
 * The priority-agenda selection is normalized against the agenda keys of the currently
 * active framework, which the caller passes in (they are master data now, not a constant).
 *
 * The rules live here (instead of inside the view) so they can be verified without a
 * browser - see announcementForm.test.js.
 */
import { normalizeProgramKeys, normalizeReminderKeys } from './announcementOptions.js'

/** The only fields an update of an existing call may send. */
export const ANNOUNCEMENT_UPDATE_FIELDS = ['startDate', 'endDate', 'status']

/** True for a missing, non-string or whitespace-only value. */
function isBlank(value) {
  return typeof value !== 'string' || value.trim() === ''
}

/**
 * The message that blocks saving the modal, or '' when it may be saved.
 *
 * Only a NEW call requires the announcement name - an existing call already has one and the
 * update path never sends it. The submission-window rules are the same for both.
 */
export function announcementFormError(form, { isEditing = false } = {}) {
  if (!isEditing && isBlank(form?.name)) {
    return 'Announcement name is required.'
  }
  if (isBlank(form?.startDate) || isBlank(form?.endDate)) {
    return 'Start date and end date are required.'
  }
  if (form.endDate < form.startDate) {
    return 'End date cannot be earlier than the start date.'
  }
  return ''
}

/**
 * The payload of an update: the submission window and the status only.
 *
 * The restricted fields (name, priority agenda, announcement reminders, additional
 * announcement) are never included, so a stored value can never be replaced by an empty one
 * even if the form state happens to hold them.
 */
export function announcementUpdatePayload(form) {
  return {
    startDate: form?.startDate,
    endDate: form?.endDate,
    status: form?.status,
  }
}

/**
 * The payload of a new call: the complete announcement, with normalised selections.
 * {@code allowedProgramKeys} is the key list of the currently active Priority Agenda
 * framework; an identifier outside it is dropped rather than persisted.
 */
export function announcementCreatePayload(form, allowedProgramKeys) {
  return {
    name: (form?.name || '').trim(),
    startDate: form?.startDate,
    endDate: form?.endDate,
    status: form?.status,
    priorityAgendas: normalizeProgramKeys(form?.priorityAgendas, allowedProgramKeys),
    reminderIds: normalizeReminderKeys(form?.reminderIds),
    additionalAnnouncement: (form?.additionalAnnouncement || '').trim(),
  }
}
