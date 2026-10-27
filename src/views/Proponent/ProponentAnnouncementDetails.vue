<template>
  <div class="announcement-page">
    <!-- BACK -->
    <button type="button" class="back-btn" @click="goBack">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
        stroke-linejoin="round" aria-hidden="true">
        <path d="M19 12H5" />
        <polyline points="12 19 5 12 12 5" />
      </svg>
      Back
    </button>

    <!-- LOADING STATE -->
    <div v-if="loading" class="state-card">
      <div class="spinner"></div>
      <p>Loading announcement details...</p>
    </div>

    <!-- NOT FOUND / NOT AUTHORIZED / FAILED REQUEST -->
    <div v-else-if="error" class="state-card error-state">
      <h1>{{ errorTitle }}</h1>
      <p>{{ error }}</p>
      <div class="state-actions">
        <button type="button" class="btn-secondary" @click="goBack">Go Back</button>
        <button v-if="errorKind === 'error'" type="button" class="btn-primary" @click="loadAnnouncement">
          Retry
        </button>
      </div>
    </div>

    <!-- ANNOUNCEMENT -->
    <template v-else-if="announcement">
      <!-- ANNOUNCEMENT BANNER -->
      <section class="banner" :class="{ 'has-countdown': countdown }">
        <span class="ring ring-1" aria-hidden="true"></span>
        <span class="ring ring-2" aria-hidden="true"></span>
        <span class="ring ring-3" aria-hidden="true"></span>

        <div class="banner-main">
          <div class="banner-head">
            <span class="banner-eyebrow">Call for Proposals</span>
            <span class="status-badge" :class="statusClass">
              <span class="status-dot" aria-hidden="true"></span>{{ displayStatus }}
            </span>
          </div>

          <h1 class="banner-title">{{ announcement.name }}</h1>

          <p class="banner-summary">
            <span>{{ summary.before }}</span><span v-if="summary.date" class="banner-date">{{ summary.date }}</span><span>{{ summary.after }}</span>
          </p>

          <p class="banner-publisher">
            Announcement details published by the Research and Publication Services (RPS) office.
          </p>

          <div class="banner-actions">
            <button type="button" class="submit-btn" @click="goToMyProjects">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
                <polyline points="14 3 14 8 19 8" />
                <path d="M12 12v5M9.5 14.5h5" />
              </svg>
              Submit a proposal
            </button>
            <span v-if="deadlineNote" class="deadline-note">{{ deadlineNote }}</span>
          </div>
        </div>

        <div v-if="countdown" class="countdown">
          <span class="countdown-value">{{ countdown.days }}</span>
          <span class="countdown-label">{{ countdown.label }}</span>
        </div>

        <div class="timeline">
          <div class="timeline-track">
            <div class="timeline-progress" :style="{ width: `${timelineProgress}%` }"></div>
          </div>
          <div class="timeline-dates">
            <div class="timeline-item">
              <span class="timeline-label">Submission opens</span>
              <span class="timeline-date">{{ formatDate(announcement.startDate) }}</span>
            </div>
            <div class="timeline-item timeline-item-end">
              <span class="timeline-label">Submission deadline</span>
              <span class="timeline-date">{{ formatDate(announcement.endDate) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- DEADLINE EXTENSION NOTICE
           Shown only when the call has a recorded deadline extension AND is still open
           under its current (extended) deadline, so an officially extended call is never
           reported as closed just because its original deadline has passed. -->
      <section v-if="extension" class="extension-notice">
        <span class="extension-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
            <circle cx="12" cy="12" r="9" />
            <polyline points="12 7.5 12 12 15.5 14" />
          </svg>
        </span>
        <div class="extension-text">
          <p class="extension-message">{{ extension.message }}</p>
          <p v-if="extension.previousDeadlineMessage" class="extension-previous">
            {{ extension.previousDeadlineMessage }}
          </p>
        </div>
      </section>

      <!-- CONTENT PANELS -->
      <div class="panels">
        <section class="panel panel-agenda">
          <div class="panel-head">
            <div>
              <h2 class="panel-title">Priority agenda</h2>
              <p class="panel-subtitle">Programs this call is accepting proposals for</p>
            </div>
            <span v-if="priorityAgendaPrograms.length" class="count-badge">
              {{ priorityAgendaPrograms.length }} program{{ priorityAgendaPrograms.length === 1 ? '' : 's' }}
            </span>
          </div>

          <ul v-if="priorityAgendaPrograms.length" class="program-list">
            <li v-for="program in priorityAgendaPrograms" :key="program.key" class="program-item">
              <span class="program-icon" aria-hidden="true">{{ programInitial(program) }}</span>
              <span class="program-text">
                <span class="program-name">{{ program.label }}</span>
                <span class="program-description">{{ program.description }}</span>
              </span>
            </li>
          </ul>
          <p v-else class="panel-empty">No priority programs specified for this announcement.</p>
        </section>

        <div class="side-column">
          <!-- READY TO SUBMIT? (navigation only - no proposal is created here) -->
          <section class="ready-card">
            <h2 class="ready-title">Ready to submit?</h2>
            <p class="ready-description">{{ readyDescription }}</p>

            <ol class="ready-steps">
              <li v-for="(step, index) in readySteps" :key="step" class="ready-step">
                <span class="step-badge" aria-hidden="true">{{ index + 1 }}</span>
                <span class="step-text">{{ step }}</span>
              </li>
            </ol>

            <button type="button" class="ready-btn" @click="goToMyProjects">
              <span>Submit a proposal</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14" />
                <polyline points="12 5 19 12 12 19" />
              </svg>
            </button>
          </section>

          <section class="panel panel-reminders">
            <div class="reminder-head">
              <span class="bell-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                  stroke-linejoin="round">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
              </span>
              <h2 class="panel-title">Announcement reminders</h2>
            </div>

            <ul v-if="reminderTexts.length" class="reminder-list">
              <li v-for="(reminder, index) in reminderTexts" :key="index" class="reminder-item">
                <span class="check-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                    stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10" />
                    <polyline points="16 9.5 10.5 15 8 12.5" />
                  </svg>
                </span>
                <span class="reminder-text">{{ reminder }}</span>
              </li>
            </ul>
            <p v-else class="panel-empty">No reminders were saved for this announcement.</p>
          </section>
        </div>
      </div>

      <!-- SAVED ADDITIONAL ANNOUNCEMENT TEXT (only when the announcement has it) -->
      <section v-if="announcement.additionalAnnouncement" class="panel panel-details">
        <h2 class="panel-title">Announcement details</h2>
        <p class="details-body">{{ announcement.additionalAnnouncement }}</p>
      </section>

      <!-- READ-ONLY DISCLAIMER -->
      <p class="read-only-note">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
          stroke-linejoin="round" aria-hidden="true">
          <rect x="3" y="11" width="18" height="11" rx="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        <span>This is a read-only view of the announcement saved by RPS. Proposals can only be submitted while the call is open.</span>
      </p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import { announcementStatus, localTodayISO } from '@/utils/announcementStatus'
import { extensionNotice } from '@/utils/announcementUpdates'
import { ANNOUNCEMENT_REMINDERS, normalizeReminderKeys } from '@/utils/announcementOptions'
import { usePriorityAgenda } from '@/composables/usePriorityAgenda'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const errorKind = ref('')
const announcement = ref(null)

/** Smallest visible share of the timeline track, so a just-opened call still shows a marker. */
const MIN_TIMELINE_PROGRESS = 4

const errorTitle = computed(() => {
  const titles = {
    'not-found': 'Announcement not found',
    unauthorized: 'Access denied',
    error: 'Unable to load the announcement',
  }
  return titles[errorKind.value] || 'Announcement unavailable'
})

/** Draft / Scheduled / Open / Closed, from the saved status plus the saved dates. */
const displayStatus = computed(() =>
  announcement.value ? announcementStatus(announcement.value) : ''
)

const statusClass = computed(() => {
  const classes = {
    Draft: 'status-draft',
    Scheduled: 'status-scheduled',
    Open: 'status-active',
    Closed: 'status-closed',
  }
  return classes[displayStatus.value] || ''
})

/**
 * The deadline extension notice, or null when there is nothing to announce. It is shown
 * only while the call is genuinely open under its current (extended) deadline, so an
 * extended call is never reported as closed because its superseded deadline has passed.
 * Its "previous deadline" is the deadline the latest recorded extension replaced.
 */
const extension = computed(() => extensionNotice(announcement.value))

/**
 * The saved priority agendas, resolved to their display names. The names come from the
 * Priority Agenda master data (through the lookup API), so an agenda that has since
 * been deactivated still shows the name it was stored with - and a call created under
 * a new framework shows that framework's agendas, never a hard-coded list.
 */
const { resolveLabels } = usePriorityAgenda()
const programLabels = ref({})

const priorityAgendaPrograms = computed(() =>
  (announcement.value?.priorityAgendas || []).map((key) => ({
    key,
    label: programLabels.value[key]?.name || key,
    description: programLabels.value[key]?.description || '',
  }))
)

/** The saved standard reminders, resolved to their text. */
const reminderTexts = computed(() => {
  const ids = normalizeReminderKeys(announcement.value?.reminderIds)
  return ANNOUNCEMENT_REMINDERS.filter((reminder) => ids.includes(reminder.key)).map(
    (reminder) => reminder.text
  )
})

/**
 * The window wording shown under the title: the same message as before, split so the real
 * saved date can be emphasised in the banner.
 */
const summary = computed(() => {
  if (!announcement.value) return { before: '', date: '', after: '' }
  const status = displayStatus.value
  if (status === 'Open') {
    return {
      before: 'Accepting submissions until ',
      date: formatDate(announcement.value.endDate),
      after: '.',
    }
  }
  if (status === 'Scheduled') {
    return {
      before: 'Submissions open on ',
      date: formatDate(announcement.value.startDate),
      after: '.',
    }
  }
  if (status === 'Closed') {
    return { before: 'Submissions for this call are closed.', date: '', after: '' }
  }
  return { before: 'This call is not open for submissions.', date: '', after: '' }
})

/**
 * Presentation-only day counter for the yellow card, derived from the saved dates (never
 * from the title or a stored value): days until the deadline while the call is open, days
 * until it opens while it is scheduled. Closed and draft calls show no counter, so a
 * misleading positive number is never displayed. Deadline values are never altered here.
 */
const countdown = computed(() => {
  if (!announcement.value) return null
  const status = displayStatus.value
  const today = localTodayISO()
  if (status === 'Open') {
    const days = daysBetween(today, announcement.value.endDate)
    return days === null || days < 0 ? null : { days, label: 'Days left' }
  }
  if (status === 'Scheduled') {
    const days = daysBetween(today, announcement.value.startDate)
    return days === null || days < 0 ? null : { days, label: 'Days to open' }
  }
  return null
})

/**
 * Compact hint shown beside the banner action. It reuses the countdown above, so it only
 * appears while a remaining time is meaningful (open call: closes in n days; scheduled
 * call: opens in n days) and never shows a misleading positive number for closed calls.
 */
const deadlineNote = computed(() => {
  if (!countdown.value) return ''
  const { days, label } = countdown.value
  const unit = days === 1 ? 'day' : 'days'
  return label === 'Days left' ? `Closes in ${days} ${unit}.` : `Opens in ${days} ${unit}.`
})

/** Fixed instructional labels for the Ready to submit? card (informational only). */
const readySteps = [
  'Pick a priority program',
  'Add your plan, objectives, outcomes and budget',
  'Submit before the deadline',
]

/**
 * Supporting line for the Ready to submit? card. It is built from the saved announcement and
 * the same status rules the banner uses, so it never claims submissions are open while the
 * call is closed, scheduled or still a draft.
 */
const readyDescription = computed(() => {
  if (!announcement.value) return ''
  const name = announcement.value.name
  const status = displayStatus.value
  if (status === 'Open') {
    return `Start your proposal for ${name} before ${formatDate(announcement.value.endDate)}.`
  }
  if (status === 'Scheduled') {
    return `Start your proposal for ${name} when submissions open on ${formatDate(announcement.value.startDate)}.`
  }
  if (status === 'Closed') {
    return `Submissions for ${name} are closed. You can review your projects anytime.`
  }
  return `${name} is not open for submissions yet.`
})

/**
 * Navigation only: opens the existing My Projects page (route "/proposals", the same
 * destination as the "My Projects" side-nav item). No proposal is created, saved or
 * submitted from this page, and no API mutation is performed.
 */
function goToMyProjects() {
  router.push('/proposals')
}

/**
 * Elapsed share of the saved submission window - the same elapsed-window meaning the RPS
 * staff dashboard already uses. Clamped to a small visible minimum so the track always
 * shows a marker, and never used to communicate a workflow state.
 */
const timelineProgress = computed(() => {
  const current = announcement.value
  if (!current) return MIN_TIMELINE_PROGRESS
  const total = daysBetween(current.startDate, current.endDate)
  const elapsed = daysBetween(current.startDate, localTodayISO())
  if (total === null || total <= 0 || elapsed === null) return 100
  const percent = Math.round((Math.min(Math.max(elapsed, 0), total) / total) * 100)
  return Math.min(100, Math.max(MIN_TIMELINE_PROGRESS, percent))
})

/** The existing program badge convention: first letter of the stable program key. */
function programInitial(program) {
  return String(program?.key || '').charAt(0).toUpperCase()
}

function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  return d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
}

/** Local midnight for an ISO date, matching the application's local-date convention. */
function localDate(iso) {
  const parts = String(iso || '').split('-').map(Number)
  if (parts.length !== 3 || parts.some((part) => Number.isNaN(part))) return null
  return new Date(parts[0], parts[1] - 1, parts[2])
}

/** Whole days between two ISO dates, or null when either date is unusable. */
function daysBetween(fromISO, toISO) {
  const from = localDate(fromISO)
  const to = localDate(toISO)
  if (!from || !to) return null
  return Math.round((to - from) / 86400000)
}

async function loadAnnouncement() {
  const announcementId = route.params.id
  if (!announcementId) {
    errorKind.value = 'not-found'
    error.value = 'No Call for Proposals was referenced by this notification.'
    return
  }

  loading.value = true
  error.value = ''
  errorKind.value = ''
  try {
    const res = await api.get(`/api/proposal-announcements/${announcementId}/details`)
    const data = res.data
    if (!data || !data.id) {
      announcement.value = null
      errorKind.value = 'not-found'
      error.value = 'This Call for Proposals could not be found. It may have been removed.'
      return
    }
    announcement.value = data
    // Resolve the saved agenda names from the master data (a deactivated agenda keeps
    // showing the name it was stored with; a failure degrades to the raw key).
    resolveLabels(data.priorityAgendas).then((index) => {
      programLabels.value = index
    })
  } catch (err) {
    console.error(err)
    announcement.value = null
    const status = err.response?.status
    if (status === 404) {
      errorKind.value = 'not-found'
      error.value =
        'This Call for Proposals could not be found. It may have been removed or is not published yet.'
    } else if (status === 401 || status === 403) {
      errorKind.value = 'unauthorized'
      error.value = 'You are not authorized to view this announcement.'
    } else {
      errorKind.value = 'error'
      error.value = 'Failed to load the announcement details. Please try again.'
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  // Return to the view the proponent came from (normally the notification list). When the
  // details URL was opened directly there is no previous page, so fall back to notifications.
  if (router.options?.history?.state?.back) {
    router.back()
  } else {
    router.push('/proponent-notification')
  }
}

// Re-load when the referenced announcement changes, so a second notification opens its own call.
watch(
  () => route.params.id,
  (id) => {
    if (id) loadAnnouncement()
  }
)

onMounted(loadAnnouncement)
</script>

<style scoped>
.announcement-page {
  /* Typography scale - the single source of truth for every text size on this page. */
  --font-title: 26px; /* announcement title */
  --font-section: 17px; /* panel / section headings */
  --font-card: 16px; /* card headings */
  --font-body: 13px; /* descriptions, reminders, dates, instructions, buttons */
  --font-secondary: 12px; /* labels, badges, program descriptions, back button */
  --font-supporting: 11px; /* timeline labels, disclaimer */
  --font-countdown: 32px; /* countdown number */
  --font-countdown-label: 10px; /* countdown caption */
  /* Fill the layout content area so the light canvas reaches the application edges. */
  margin: -32px;
  min-height: calc(100% + 64px);
  box-sizing: border-box;
  padding: 16px;
  background: #f3f4f6;
}

/* BACK BUTTON */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 12px;
  padding: 6px 11px;
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 7px;
  color: #25245b;
  font-size: var(--font-secondary);
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.back-btn:hover {
  border-color: #25245b;
}

.back-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.back-btn svg {
  width: 14px;
  height: 14px;
}

/* ANNOUNCEMENT BANNER */
.banner {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  min-height: 190px;
  padding: 20px;
  background: #25234f;
  border-radius: 14px;
}

.ring {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  pointer-events: none;
}

.ring-1 {
  width: 150px;
  height: 150px;
  top: -58px;
  right: 22px;
}

.ring-2 {
  width: 186px;
  height: 186px;
  top: -76px;
  right: 4px;
  border-color: rgba(255, 255, 255, 0.05);
}

.ring-3 {
  width: 222px;
  height: 222px;
  top: -94px;
  right: -14px;
  border-color: rgba(255, 255, 255, 0.05);
}

.banner-main {
  position: relative;
  z-index: 1;
  margin-bottom: 20px;
}

.banner-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.banner-eyebrow {
  color: #ffd400;
  font-size: var(--font-secondary);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: var(--font-secondary);
  font-weight: 600;
  line-height: 1.4;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.status-active {
  background: #d9fbe7;
  color: #16834b;
}

.status-scheduled {
  background: #dbeafe;
  color: #1e40af;
}

.status-closed {
  background: #fee2e2;
  color: #991b1b;
}

.status-draft {
  background: #e5e7eb;
  color: #374151;
}

.banner-title {
  margin: 10px 0 8px;
  color: #ffffff;
  font-size: var(--font-title);
  font-weight: 700;
  line-height: 1.2;
}

.banner.has-countdown .banner-title {
  padding-right: 90px;
}

.banner-summary {
  margin: 0;
  color: #e5e7f5;
  font-size: var(--font-body);
  font-weight: 400;
  line-height: 1.35;
}

.banner-date {
  color: #ffd400;
  font-weight: 700;
}

.banner-publisher {
  margin: 6px 0 0;
  color: #d7d8e9;
  font-size: var(--font-body);
  line-height: 1.4;
}

/* COUNTDOWN CARD */
.countdown {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 70px;
  height: 70px;
  background: #ffd400;
  border-radius: 14px;
}

.countdown-value {
  color: #25245b;
  font-size: var(--font-countdown);
  font-weight: 700;
  line-height: 1;
}

.countdown-label {
  margin-top: 3px;
  color: #25245b;
  font-size: var(--font-countdown-label);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  text-align: center;
}

/* SUBMISSION TIMELINE */
.timeline {
  position: relative;
  z-index: 1;
  margin-top: auto;
  box-sizing: border-box;
  width: 100%;
  padding: 12px;
  background: #32305f;
  border-radius: 8px;
}

.timeline-track {
  height: 5px;
  background: #494772;
  border-radius: 999px;
  overflow: hidden;
}

.timeline-progress {
  height: 100%;
  background: #ffd400;
  border-radius: 999px;
}

.timeline-dates {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 10px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-item-end {
  text-align: right;
  align-items: flex-end;
}

.timeline-label {
  color: #e5e7f5;
  font-size: var(--font-supporting);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.timeline-date {
  color: #ffffff;
  font-size: var(--font-body);
  font-weight: 600;
  line-height: 1.2;
}

/* BANNER ACTION */
.banner-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  background: #ffd400;
  color: #25245b;
  border: none;
  border-radius: 7px;
  font-size: var(--font-body);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.submit-btn:hover {
  background: #f0c800;
}

.submit-btn:focus-visible {
  outline: 2px solid #ffd400;
  outline-offset: 2px;
}

.submit-btn svg {
  width: 14px;
  height: 14px;
}

.deadline-note {
  color: #e5e7f5;
  font-size: var(--font-body);
  font-weight: 400;
}
/* CONTENT PANELS */
.panels {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  margin-top: 12px;
}

.panel {
  box-sizing: border-box;
  padding: 14px;
  border-radius: 12px;
}

.panel-agenda,
.panel-details {
  background: #ffffff;
  border: 1px solid #e8eaf2;
}

.panel-reminders {
  background: #fff8df;
  border: 1px solid #f5d98b;
}

/* RIGHT COLUMN (Ready to submit? above the reminders) */
.side-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* READY TO SUBMIT? CARD */
.ready-card {
  box-sizing: border-box;
  width: 100%;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #25245b;
  border-radius: 10px;
}

.ready-title {
  margin: 0 0 4px;
  color: #25245b;
  font-size: var(--font-card);
  font-weight: 700;
  line-height: 1.2;
}

.ready-description {
  margin: 0 0 8px;
  color: #858ba5;
  font-size: var(--font-body);
  line-height: 1.4;
}

.ready-steps {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0 0 9px;
  padding: 0;
  list-style: none;
}

.ready-step {
  display: flex;
  align-items: flex-start;
  gap: 7px;
}

.step-badge {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: #25245b;
  color: #ffffff;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
}

.step-text {
  color: #25245b;
  font-size: var(--font-body);
  font-weight: 400;
  line-height: 1.4;
}

.ready-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 32px;
  background: #ffd400;
  color: #25245b;
  border: none;
  border-radius: 7px;
  font-size: var(--font-body);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.ready-btn:hover {
  background: #f0c800;
}

.ready-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.ready-btn svg {
  width: 14px;
  height: 14px;
}

.panel-details {
  margin-top: 12px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.panel-title {
  margin: 0;
  color: #25245b;
  font-size: var(--font-section);
  font-weight: 700;
  line-height: 1.2;
}

.panel-subtitle {
  margin: 4px 0 0;
  color: #858ba5;
  font-size: var(--font-secondary);
  line-height: 1.4;
}

.panel-empty {
  margin: 0;
  color: #858ba5;
  font-size: var(--font-body);
  line-height: 1.4;
}

.count-badge {
  flex-shrink: 0;
  padding: 5px 10px;
  background: #eeeef8;
  color: #25245b;
  border-radius: 999px;
  font-size: var(--font-secondary);
  font-weight: 600;
  white-space: nowrap;
}

/* PRIORITY AGENDA */
.program-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.program-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: #fafafd;
  border: 1px solid #e8eaf2;
  border-radius: 9px;
}

.program-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: #25245b;
  color: #ffd400;
  border-radius: 6px;
  font-size: var(--font-secondary);
  font-weight: 700;
}

.program-text {
  display: flex;
  flex-direction: column;
}

.program-name {
  color: #25245b;
  font-size: var(--font-body);
  font-weight: 600;
  line-height: 1.3;
}

.program-description {
  margin-top: 3px;
  color: #858ba5;
  font-size: var(--font-secondary);
  line-height: 1.4;
}

/* ANNOUNCEMENT REMINDERS */
.reminder-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.bell-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #ffe8a3;
  border-radius: 6px;
  color: #b8860b;
}

.bell-icon svg {
  width: 13px;
  height: 13px;
}

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.reminder-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.check-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: #16834b;
}

.check-icon svg {
  width: 13px;
  height: 13px;
}

.reminder-text {
  color: #3f4661;
  font-size: var(--font-body);
  line-height: 1.5;
}

/* SAVED ADDITIONAL ANNOUNCEMENT TEXT */
.details-body {
  margin: 8px 0 0;
  color: #4b5163;
  font-size: var(--font-body);
  line-height: 1.5;
  white-space: pre-line;
}

/* DEADLINE EXTENSION NOTICE */
.extension-notice {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  box-sizing: border-box;
  margin-top: 12px;
  padding: 12px 14px;
  background: #fff7ed;
  border: 1px solid #fcd9a4;
  border-left: 4px solid #f59e0b;
  border-radius: 10px;
}

.extension-icon {
  flex-shrink: 0;
  color: #b45309;
}

.extension-icon svg {
  width: 16px;
  height: 16px;
}

.extension-text {
  min-width: 0;
}

.extension-message {
  margin: 0;
  color: #7c3a06;
  font-size: var(--font-body);
  font-weight: 700;
  line-height: 1.4;
}

.extension-previous {
  margin: 2px 0 0;
  color: #92620c;
  font-size: var(--font-secondary);
  line-height: 1.4;
}

/* READ-ONLY DISCLAIMER */
.read-only-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 14px 0 0;
  color: #858ba5;
  font-size: var(--font-supporting);
  line-height: 1.4;
  text-align: center;
}

.read-only-note svg {
  flex-shrink: 0;
  width: 12.5px;
  height: 12.5px;
}

/* LOADING / ERROR STATES */
.state-card {
  box-sizing: border-box;
  padding: 48px 24px;
  background: #ffffff;
  border: 1px solid #e8eaf2;
  border-radius: 12px;
  text-align: center;
  color: #858ba5;
}

.state-card h1 {
  margin: 0 0 8px;
  color: #25245b;
  font-size: var(--font-section);
  font-weight: 700;
}

.state-card p {
  margin: 0;
  font-size: var(--font-body);
  line-height: 1.5;
}

.error-state h1 {
  color: #991b1b;
}

.state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.spinner {
  width: 26px;
  height: 26px;
  margin: 0 auto 12px;
  border: 3px solid #e3e6ef;
  border-top-color: #25245b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.btn-primary,
.btn-secondary {
  padding: 9px 16px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
}

.btn-primary {
  background: #25245b;
  color: #ffffff;
}

.btn-primary:hover {
  background: #32305f;
}

.btn-secondary {
  background: #ffffff;
  border-color: #e3e6ef;
  color: #25245b;
}

.btn-secondary:hover {
  border-color: #25245b;
}

.btn-primary:focus-visible,
.btn-secondary:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

/* RESPONSIVE */
@media (max-width: 900px) {
  .panels {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .announcement-page {
    padding: 12px;
  }

  .banner {
    padding: 16px;
  }

  .banner-title {
    font-size: 22px;
  }

  .banner.has-countdown .banner-title {
    padding-right: 78px;
  }

  .countdown {
    top: 14px;
    right: 14px;
    width: 64px;
    height: 64px;
    border-radius: 13px;
  }

  .countdown-value {
    font-size: 26px;
  }

  .countdown-label {
    font-size: 10px;
  }

  .timeline-dates {
    flex-direction: column;
    gap: 8px;
  }

  .timeline-item-end {
    text-align: left;
    align-items: flex-start;
  }

  .ring-3 {
    display: none;
  }

  .banner-actions {
    gap: 6px;
  }

  .submit-btn,
  .ready-btn {
    height: 34px;
  }
}
</style>
