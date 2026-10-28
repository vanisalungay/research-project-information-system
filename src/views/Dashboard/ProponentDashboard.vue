<template>
  <!--
    The Proponent's summary dashboard.

    Everything shown here is a summary of records the Proponent already owns, read from the
    endpoints the workflow pages use (proposals, Calls for Proposal, endorsement requests
    and the shared notification store). The full lists, forms and histories stay on their
    own pages: this screen answers "what needs my attention?" and links to them.

    The page fills the whole content area beside the sidebar (.end-page--fluid), and its
    sections pair up into columns only when there is real room for them (see
    assets/dashboard.css).
  -->
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== WELCOME ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true">{{ initials }}</span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">Welcome back, {{ displayName }}!</h2>
        <p class="dash-banner-sub">
          Track your proposals, monitor progress, and manage your endorsement requests in
          one place.
        </p>
      </div>
    </section>

    <!-- ===================== FEATURED CALL + QUICK ACTIONS ===================== -->
    <div class="dash-row dash-row--main">
      <section class="dash-section dash-call">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.megaphone"></span>
          <h3 class="dash-section-title">Call for Proposals</h3>
          <span v-if="activeCall" class="end-badge" :class="activeCallTone">
            {{ activeCallStatus }}
          </span>
        </header>

        <template v-if="callsLoading">
          <span class="dash-skeleton dash-skeleton--title"></span>
          <span class="dash-skeleton dash-skeleton--line"></span>
          <span class="dash-skeleton dash-skeleton--line" style="width: 68%"></span>
        </template>

        <p v-else-if="callsError" class="end-field-error">{{ callsError }}</p>
        <p v-else-if="!activeCall" class="dash-empty">{{ NO_AVAILABLE_CALLS_MESSAGE }}</p>

        <template v-else>
          <h4 class="dash-call-title">{{ activeCall.name }}</h4>
          <p v-if="activeCall.additionalAnnouncement" class="dash-call-desc">
            {{ activeCall.additionalAnnouncement }}
          </p>

          <div class="dash-meta">
            <div class="dash-meta-item">
              <span class="dash-meta-icon" v-html="icons.calendar"></span>
              <span class="dash-meta-body">
                <span class="dash-meta-label">Opening date</span>
                <span class="dash-meta-value">{{ callOpeningLabel(activeCall) }}</span>
              </span>
            </div>
            <div class="dash-meta-item">
              <span class="dash-meta-icon" v-html="icons.clock"></span>
              <span class="dash-meta-body">
                <span class="dash-meta-label">Deadline</span>
                <span class="dash-meta-value">{{ callDeadlineLabel(activeCall) }}</span>
              </span>
            </div>
            <div class="dash-meta-item">
              <span class="dash-meta-icon" v-html="icons.window"></span>
              <span class="dash-meta-body">
                <span class="dash-meta-label">Submission period</span>
                <span class="dash-meta-value">{{ callWindowLabel(activeCall) }}</span>
              </span>
            </div>
            <div class="dash-meta-item">
              <span class="dash-meta-icon" v-html="icons.fund"></span>
              <span class="dash-meta-body">
                <span class="dash-meta-label">Funding program</span>
                <span class="dash-meta-value">{{ fundingProgramLabel(activeCall) }}</span>
              </span>
            </div>
          </div>

          <!-- Only shown when the deadline is genuinely near, and always from the saved date. -->
          <p v-if="deadlineSoon" class="dash-deadline">
            <span v-html="icons.clock"></span>
            {{ callDaysLeft === 0 ? 'Closes today' : `Closes in ${callDaysLeft} day${callDaysLeft === 1 ? '' : 's'}` }}
          </p>

          <div class="dash-call-actions">
            <router-link
              class="end-btn is-primary"
              :to="`/proponent-announcement/${activeCall.id}`"
            >
              View Call Details
            </router-link>
            <router-link class="end-btn" to="/proposals">Start a Proposal</router-link>
          </div>
        </template>
      </section>

      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.bolt"></span>
          <h3 class="dash-section-title">Quick actions</h3>
        </header>

        <nav class="dash-actions" aria-label="Quick actions">
          <router-link class="dash-action dash-action--primary" to="/proposals">
            <span class="dash-action-icon" v-html="icons.proposals"></span>
            <span class="dash-action-text">
              <span>View My Projects</span>
              <span class="dash-action-hint">All your proposals</span>
            </span>
          </router-link>
          <router-link class="dash-action" to="/proponent-proposal-monitoring">
            <span class="dash-action-icon" v-html="icons.history"></span>
            <span class="dash-action-text">
              <span>View History</span>
              <span class="dash-action-hint">Progress tracking</span>
            </span>
          </router-link>
          <router-link class="dash-action" to="/endorsements">
            <span class="dash-action-icon" v-html="icons.endorse"></span>
            <span class="dash-action-text">
              <span>My Endorsements</span>
              <span class="dash-action-hint">Requests &amp; letters</span>
            </span>
          </router-link>
          <router-link class="dash-action" to="/notifications">
            <span class="dash-action-icon" v-html="icons.bell"></span>
            <span class="dash-action-text">
              <span>View All Notifications</span>
              <span class="dash-action-hint">
                {{ unreadCount > 0 ? `${unreadBadgeLabel(unreadCount)} unread` : 'Nothing unread' }}
              </span>
            </span>
          </router-link>
        </nav>
      </section>
    </div>

    <!-- ===================== MY PROPOSALS ===================== -->
    <section class="dash-section">
      <header class="dash-section-head">
        <span class="dash-section-icon" v-html="icons.proposals"></span>
        <h3 class="dash-section-title">My proposals</h3>
        <div class="dash-section-actions">
          <router-link class="end-btn is-link" to="/proposals">View Proposals</router-link>
          <router-link class="end-btn is-link" to="/proponent-proposal-monitoring">
            View History
          </router-link>
        </div>
      </header>

      <template v-if="proposalsLoading">
        <div class="dash-stats">
          <span v-for="n in 6" :key="n" class="dash-skeleton dash-skeleton--stat"></span>
        </div>
      </template>

      <p v-else-if="proposalsError" class="end-field-error">{{ proposalsError }}</p>

      <template v-else>
        <!-- Summary only: the cards are not links, because a bucket groups several stored
             statuses and the My Projects filter accepts one exact status. The two actions
             above open the full list and the tracking view. -->
        <div class="dash-stats dash-stats--six">
          <div
            v-for="card in proposalCards"
            :key="card.id"
            class="dash-stat"
            :class="`dash-stat--${card.accent}`"
          >
            <span class="dash-stat-head">
              <span class="dash-stat-icon" v-html="icons[card.icon]"></span>
              <span class="dash-stat-label">{{ card.label }}</span>
            </span>
            <span class="dash-stat-value">{{ card.value }}</span>
          </div>
        </div>

        <div v-if="revisionDeadline" class="end-note is-warning">
          <strong>Revision deadline:</strong>
          {{ revisionDeadline.title }}
          <template v-if="revisionDeadline.proposalId">
            ({{ revisionDeadline.proposalId }})
          </template>
          — {{ revisionDaysLeftLabel(revisionDeadline.daysLeft) }}.
          <router-link class="end-btn is-link" to="/revisions">Submit Revision</router-link>
        </div>
        <p v-else class="dash-empty">
          {{
            pendingRevisions > 0
              ? `${pendingRevisions} revision(s) still to resubmit.`
              : 'No pending revisions — you are all caught up.'
          }}
        </p>
      </template>
    </section>


    <!-- ===================== ENDORSEMENTS + NOTIFICATIONS ===================== -->
    <div class="dash-row dash-row--half">
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.endorse"></span>
          <h3 class="dash-section-title">My endorsement requests</h3>
          <div class="dash-section-actions">
            <router-link class="end-btn is-link" to="/endorsements">View all</router-link>
          </div>
        </header>

        <template v-if="endorsementsLoading">
          <div class="dash-stats">
            <span v-for="n in 4" :key="n" class="dash-skeleton dash-skeleton--stat"></span>
          </div>
        </template>

        <p v-else-if="endorsementsError" class="end-field-error">{{ endorsementsError }}</p>

        <template v-else>
          <div class="dash-stats">
            <div
              v-for="card in endorsementCards"
              :key="card.id"
              class="dash-stat"
              :class="`dash-stat--${card.accent}`"
            >
              <span class="dash-stat-head">
                <span class="dash-stat-icon" v-html="icons[card.icon]"></span>
                <span class="dash-stat-label">{{ card.label }}</span>
              </span>
              <span class="dash-stat-value">{{ card.value }}</span>
            </div>
          </div>

          <p v-if="recentEndorsements.length === 0" class="dash-empty">
            You have not raised an endorsement request yet. Raise one before you start a new
            proposal — a request only becomes an issued document once your Dean approves it.
          </p>

          <div v-else class="end-table-scroll">
            <table class="end-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Project title</th>
                  <th>Submitted</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="request in recentEndorsements" :key="request.id">
                  <td>{{ request.referenceNumber }}</td>
                  <td>
                    {{ request.projectTitle }}
                    <div v-if="request.programTitle" class="end-muted">
                      {{ request.programTitle }}
                    </div>
                  </td>
                  <td>{{ formatEndorsementTimestamp(request.createdAt) }}</td>
                  <td>
                    <span class="end-badge" :class="endorsementStatusTone(request.status)">
                      {{ endorsementStatusLabel(request.status) }}
                    </span>
                  </td>
                  <td>
                    <router-link class="end-btn is-link" :to="`/endorsements/${request.id}`">
                      View
                    </router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="dash-call-actions">
            <router-link class="end-btn" to="/endorsements">My Endorsements</router-link>
            <router-link class="end-btn" to="/endorsements?tab=certifications">
              Issued Certificates
            </router-link>
          </div>
        </template>
      </section>

      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.bell"></span>
          <h3 class="dash-section-title">Recent notifications</h3>
          <span v-if="unreadCount > 0" class="end-badge is-pending">
            {{ unreadBadgeLabel(unreadCount) }} unread
          </span>
          <div class="dash-section-actions">
            <router-link class="end-btn is-link" to="/notifications">View all</router-link>
          </div>
        </header>

        <template v-if="notificationsLoading">
          <span v-for="n in 3" :key="n" class="dash-skeleton dash-skeleton--row"></span>
        </template>

        <p v-else-if="recentNotificationsList.length === 0" class="dash-empty">
          You have no notifications yet. Calls for Proposal, review decisions and deadline
          reminders will appear here.
        </p>

        <ul v-else class="dash-notes">
          <li
            v-for="notification in recentNotificationsList"
            :key="notification.id"
            class="dash-note"
            :class="{ 'dash-note--unread': isUnreadNotification(notification) }"
          >
            <span class="dash-note-icon" v-html="icons.bell"></span>
            <span class="dash-note-body">
              <span class="dash-note-title">{{ notification.title }}</span>
              <p class="dash-note-text">{{ notification.message }}</p>
              <span class="dash-note-foot">
                <span class="dash-note-date">{{ formatTimestamp(notification.createdAt) }}</span>
                <span v-if="isUnreadNotification(notification)" class="end-badge is-pending">
                  Unread
                </span>
              </span>
            </span>
            <router-link
              class="end-btn is-link dash-note-action"
              to="/notifications"
              :aria-label="`Open the notifications page for: ${notification.title}`"
            >
              Open
            </router-link>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>


<script setup>
import { computed, onMounted, ref } from 'vue'
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'
import api from '@/utils/api'
import { useNotificationStore } from '@/stores/notifications'
import { useUserDataStore } from '@/stores/userData'
import { announcementStatus } from '@/utils/announcementStatus'
import {
  AVAILABLE_CALLS_ENDPOINT,
  NO_AVAILABLE_CALLS_MESSAGE,
  callDeadlineLabel,
  callOpeningLabel,
  callWindowLabel,
  isCallOpen,
  sortCallsForDisplay,
} from '@/utils/callForProposal.js'
import {
  endorsementStatusLabel,
  endorsementStatusTone,
  endorsementSummaryCounts,
  fetchEndorsementRequests,
  formatEndorsementTimestamp,
  fundingProgramLabel,
  issuedLetterCount,
} from '@/utils/endorsement.js'
import { isUnreadNotification, unreadBadgeLabel } from '@/utils/notifications.js'
import { PROPOSAL_SUMMARY_BUCKETS, proposalSummaryCounts } from '@/utils/proposalSummary.js'
import {
  PENDING_REVISION_STATUSES,
  countPendingRevisions,
  isRevisionDeadlinePassed,
  revisionDaysLeft,
  revisionDaysLeftLabel,
} from '@/utils/revisionDeadline.js'
import { formatTimestamp } from '@/utils/timestamps.js'

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  megaphone: svg(
    '<path d="M3 11v2a1 1 0 0 0 1 1h2l4 4V6L6 10H4a1 1 0 0 0-1 1z"/><path d="M15 8.5a4 4 0 0 1 0 7"/><path d="M18 6a8 8 0 0 1 0 12"/>',
  ),
  calendar: svg(
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 11h18"/>',
  ),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  window: svg('<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 11h18M8 15h5"/>'),
  fund: svg('<path d="M12 3v18"/><path d="M16 7H10a3 3 0 0 0 0 6h4a3 3 0 0 1 0 6H7"/>'),
  bolt: svg('<path d="M13 2 4 14h6l-1 8 9-12h-6z"/>'),
  proposals: svg(
    '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>',
  ),
  history: svg('<path d="M3 12a9 9 0 1 0 3-6.7"/><path d="M3 4v5h5"/><path d="M12 8v4l3 2"/>'),
  endorse: svg('<path d="M22 11.1V12a10 10 0 1 1-5.9-9.1"/><path d="m22 4-10 10-3-3"/>'),
  bell: svg(
    '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/>',
  ),
  stack: svg(
    '<rect x="3" y="4" width="18" height="5" rx="1"/><rect x="3" y="11" width="18" height="5" rx="1"/><rect x="3" y="18" width="11" height="3" rx="1"/>',
  ),
  draft: svg('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>'),
  sent: svg('<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4z"/>'),
  review: svg('<circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/>'),
  approved: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  returned: svg(
    '<path d="M3 7v6h6"/><path d="M3.5 13a9 9 0 1 0 2.6-8.3"/><path d="M12 8.5v4"/><path d="M12 16h.01"/>',
  ),
  letter: svg('<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>'),
}

const userStore = useUserDataStore()
const notificationStore = useNotificationStore()

const displayName = computed(() => userStore.user?.name || 'Proponent')

/** The avatar carries the initials of the signed-in name - no image is uploaded anywhere. */
const initials = computed(() => {
  const parts = String(userStore.user?.name || 'Proponent')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (parts.length === 0) return 'P'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
})

/** The existing badge tones, chosen from the status the rest of the system shows. */
const CALL_BADGE_TONES = {
  Open: 'is-approved',
  Scheduled: 'is-pending',
  Closed: 'is-rejected',
  Draft: 'is-pending',
}

// ===== Calls for Proposal =====
const calls = ref([])
const callsLoading = ref(true)
const callsError = ref('')

// ===== Proposals =====
const proposals = ref([])
const proposalsLoading = ref(true)
const proposalsError = ref('')

// ===== Endorsement requests =====
const endorsements = ref([])
const endorsementsLoading = ref(true)
const endorsementsError = ref('')


/**
 * The call to show: the open one, or - when nothing is open yet - the next scheduled one.
 * The order is the same rule the Create Proposal workflow uses, and the list itself is the
 * backend's (`/available` never returns a draft, closed or expired call).
 */
const activeCall = computed(() => sortCallsForDisplay(calls.value)[0] ?? null)
const activeCallStatus = computed(() => announcementStatus(activeCall.value))
const activeCallTone = computed(() => CALL_BADGE_TONES[activeCallStatus.value] || 'is-pending')

/** Whole days until the featured call closes (0 when it is missing or already past). */
const callDaysLeft = computed(() => revisionDaysLeft(activeCall.value?.endDate))

/** Only a call that is genuinely open and genuinely close to its deadline is flagged. */
const deadlineSoon = computed(
  () => Boolean(activeCall.value) && isCallOpen(activeCall.value) && callDaysLeft.value <= 14,
)

/** The proposal cards, carrying the counts of the records the backend returned. */
const proposalCounts = computed(() => proposalSummaryCounts(proposals.value))
const proposalCards = computed(() =>
  PROPOSAL_SUMMARY_BUCKETS.map((bucket) => ({ ...bucket, value: proposalCounts.value[bucket.id] })),
)

/** Revisions the Proponent can still resubmit (the backend's own deadline rule). */
const pendingRevisions = computed(() => countPendingRevisions(proposals.value))

/** The revision whose deadline comes first and has not passed, or null. */
const revisionDeadline = computed(() => {
  const upcoming = proposals.value
    .filter(
      (proposal) =>
        PENDING_REVISION_STATUSES.includes(proposal.status) &&
        !isRevisionDeadlinePassed(proposal.revisionDeadline),
    )
    .sort((a, b) => new Date(a.revisionDeadline) - new Date(b.revisionDeadline))
  const nearest = upcoming[0]
  if (!nearest) return null
  return {
    title: nearest.projectTitle || 'Project Revision Required',
    proposalId: nearest.documentId || nearest.proposalCode || '—',
    daysLeft: revisionDaysLeft(nearest.revisionDeadline),
  }
})

const endorsementCounts = computed(() => endorsementSummaryCounts(endorsements.value))

/** Issued letters: only an approved request carries one (1:1 with its certification). */
const issuedLetters = computed(() => issuedLetterCount(endorsements.value))

/** The endorsement cards, in the order the workflow moves through them. */
const endorsementCards = computed(() => [
  { id: 'pending', label: 'Pending', value: endorsementCounts.value.pending, accent: 'amber', icon: 'clock' },
  { id: 'approved', label: 'Approved', value: endorsementCounts.value.approved, accent: 'green', icon: 'approved' },
  { id: 'rejected', label: 'Rejected', value: endorsementCounts.value.rejected, accent: 'red', icon: 'returned' },
  { id: 'letters', label: 'Issued letters', value: issuedLetters.value, accent: 'indigo', icon: 'letter' },
])

/** The latest endorsement activity; the API returns the list newest first. */
const recentEndorsements = computed(() => endorsements.value.slice(0, 3))

/**
 * Notifications come from the shared store: the shell already reads them for the bell and
 * the sidebar badge, so the dashboard shows the same records without a second request.
 */
const recentNotificationsList = computed(() => notificationStore.recent)
const unreadCount = computed(() => notificationStore.unreadCount)
const notificationsLoading = computed(() => notificationStore.loading)

/** The readable reason a request failed, or the fallback sentence. */
const failureMessage = (reason, fallback) => reason?.response?.data?.error || fallback

/**
 * Loads the three lists the summary is built from. They are requested together but handled
 * separately, so one unavailable endpoint leaves the rest of the dashboard usable.
 */
async function load() {
  callsLoading.value = true
  proposalsLoading.value = true
  endorsementsLoading.value = true
  callsError.value = ''
  proposalsError.value = ''
  endorsementsError.value = ''

  const [proposalsResult, callsResult, endorsementsResult] = await Promise.allSettled([
    api.get('/api/proposals'),
    api.get(AVAILABLE_CALLS_ENDPOINT),
    fetchEndorsementRequests(),
  ])

  if (proposalsResult.status === 'fulfilled') {
    proposals.value = Array.isArray(proposalsResult.value.data) ? proposalsResult.value.data : []
  } else {
    proposals.value = []
    proposalsError.value = failureMessage(
      proposalsResult.reason,
      'Your proposals could not be loaded.',
    )
  }

  if (callsResult.status === 'fulfilled') {
    calls.value = Array.isArray(callsResult.value.data) ? callsResult.value.data : []
  } else {
    calls.value = []
    callsError.value = failureMessage(
      callsResult.reason,
      'The Calls for Proposal could not be loaded.',
    )
  }

  if (endorsementsResult.status === 'fulfilled') {
    endorsements.value = Array.isArray(endorsementsResult.value) ? endorsementsResult.value : []
  } else {
    endorsements.value = []
    endorsementsError.value = failureMessage(
      endorsementsResult.reason,
      'Your endorsement requests could not be loaded.',
    )
  }

  callsLoading.value = false
  proposalsLoading.value = false
  endorsementsLoading.value = false
}

onMounted(load)
</script>

