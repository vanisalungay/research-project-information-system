<template>
  <div ref="viewerRef" class="qpr-viewer">
    <!-- Only this area scrolls: the current page of the submitted report. The
         modal header above and the page navigation footer below stay in place. -->
    <div ref="scrollRef" class="viewer-scroll">
      <!-- Wizard header: the same card, page title, reporting period, page badge and
         progress indicator the submission form shows, rendered read only. -->
      <div class="wizard-card">
        <div class="wizard-head">
          <div class="wizard-head-text">
            <span class="wizard-eyebrow">MSUN Internally-Funded Project</span>
            <h2 class="wizard-title">{{ currentStepTitle }}</h2>
            <p class="wizard-meta">
              <span class="wizard-meta-item">{{ projectTitle || '—' }}</span>
              <span v-if="periodLabel" class="wizard-meta-item">
                Reporting period: {{ periodLabel }}
              </span>
            </p>
          </div>
          <span class="wizard-page-badge">Page {{ currentStep }} of {{ TOTAL_STEPS }}</span>
        </div>

        <ol class="wizard-progress">
          <li
            v-for="(step, index) in wizardSteps"
            :key="step.key"
            class="wizard-progress-item"
            :class="`is-${stepStateOf(index + 1)}`"
          >
            <button
              type="button"
              class="wizard-progress-btn"
              :aria-current="currentStep === index + 1 ? 'step' : undefined"
              @click="goToStep(index + 1)"
            >
              <span class="wizard-progress-marker" aria-hidden="true">
                <template v-if="stepStateOf(index + 1) === 'done'">✓</template>
                <template v-else>{{ index + 1 }}</template>
              </span>
              <span class="wizard-progress-label">{{ step.label }}</span>
            </button>
          </li>
        </ol>
      </div>

      <!-- Page 1 and Page 2: the submitted Quarterly Progress Report. Both pages are
         rendered by one component instance through its `page` prop, so switching
         pages never reloads the report and never loses the loaded data. -->
      <div v-show="currentStep < TOTAL_STEPS" class="wizard-panel">
        <QuarterlyReportForm :report="report" readonly :page="currentStep" />
      </div>

      <!-- Page 3: the submitted Project Workplan. -->
      <div v-show="currentStep === TOTAL_STEPS" class="wizard-panel">
        <ProjectWorkplanForm v-if="workplan" :model-value="workplan" readonly />
        <p v-else class="viewer-notice">
          No Project Workplan has been saved for this reporting stage.
        </p>
      </div>
    </div>

    <!-- Fixed modal footer: the page navigation, placed outside the scrollable
         report content. Previous Page only exists from page 2 on and the last page
         offers Close instead of Next Page. There is no Save, Update or Submit
         action anywhere in this viewer. "Create a Copy" is offered only when the
         host page says the proponent may start the next reporting period: it hands
         the report being viewed back to that page, which opens the existing Create
         Quarterly Report workflow. Nothing is copied or saved from here. -->
    <footer class="viewer-footer">
      <div class="viewer-footer-left">
        <button v-if="currentStep > 1" class="btn-secondary" @click="goToStep(currentStep - 1)">
          Previous Page
        </button>
        <button
          v-if="canCreateCopy"
          type="button"
          class="btn-copy"
          :disabled="copying"
          @click="emit('create-copy')"
        >
          {{ copying ? 'Opening...' : 'Create a Copy' }}
        </button>
      </div>
      <div class="viewer-footer-right">
        <button
          v-if="currentStep < TOTAL_STEPS"
          class="btn-submit"
          @click="goToStep(currentStep + 1)"
        >
          Next Page
        </button>
        <button v-else class="btn-submit" @click="emit('close')">Close</button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'
import ProjectWorkplanForm from '@/components/ProjectWorkplanForm.vue'
import {
  REPORT_WIZARD_PAGES,
  REPORT_WIZARD_STEPS,
  clampStep,
  stepState,
} from '@/utils/reportWizard'

/**
 * Read-only, three page viewer of a submitted Quarterly Progress Report.
 *
 * It mirrors the proponent's submission wizard instead of introducing a new
 * design: the same page definitions (src/utils/reportWizard.js - Page 1 and 2 the
 * Quarterly Progress Report, Page 3 the Project Workplan), the same progress
 * indicator and page navigation, and the same report components
 * (QuarterlyReportForm for pages 1-2, ProjectWorkplanForm for page 3) rendered in
 * their read-only form. Nothing here edits, saves or submits anything.
 */
const props = defineProps({
  // The submitted report exactly as returned by the existing API
  // (GET /api/quarterly-reports/{id}). It is only read, never modified.
  report: { type: Object, default: null },
  // Whether the proponent viewing this report may start the next reporting period
  // from it. The host page decides (project eligible for quarterly reporting, a
  // next quarterly stage still to be written); the viewer only offers the action.
  canCreateCopy: { type: Boolean, default: false },
  // True while the host page is opening the Create Quarterly Report workflow.
  copying: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'create-copy'])

const TOTAL_STEPS = REPORT_WIZARD_STEPS
const wizardSteps = REPORT_WIZARD_PAGES

const viewerRef = ref(null)
const scrollRef = ref(null)
const currentStep = ref(1)

const currentStepTitle = computed(() => wizardSteps[currentStep.value - 1].title)

// Same progress-indicator states as the submission form.
const stepStateOf = (step) => stepState(step, currentStep.value)

const projectTitle = computed(() => props.report?.projectTitle || '')
const periodLabel = computed(() =>
  `${props.report?.period || ''} ${props.report?.year || ''}`.trim(),
)

// Page 3 renders the saved Project Workplan. It is deep-copied so the read-only
// component can normalise its own display without ever touching the loaded
// submission record.
const workplan = computed(() => {
  const saved = props.report?.workplan
  if (!saved || typeof saved !== 'object') return null
  return JSON.parse(JSON.stringify(saved))
})

// Every page starts at the top of the report content area - the scrollable
// container that belongs to this viewer (the modal header and the page
// navigation footer never scroll).
async function scrollToTop() {
  await nextTick()
  const scrollArea = scrollRef.value
  if (scrollArea) scrollArea.scrollTop = 0
}

function goToStep(target) {
  const wanted = clampStep(target)
  if (wanted === currentStep.value) return
  currentStep.value = wanted
  scrollToTop()
}

// Opening the viewer for another submitted report always starts on page 1.
watch(
  () => props.report,
  () => {
    currentStep.value = 1
    scrollToTop()
  },
)

onMounted(scrollToTop)
</script>

<style scoped>
/* ============================================================
   Read-only three page report viewer
   The card, progress indicator, page badge and page navigation below mirror the
   proponent's submission wizard (views/Proponent/ProponentQuarterlyReportForm.vue)
   so viewing a submitted report looks exactly like filling it in - with no
   editable control anywhere.
   ============================================================ */
.qpr-viewer {
  /* Column layout inside the modal body: the report content scrolls while the
     modal header and the page navigation footer keep their place. */
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}

/* The only area that scrolls: the current page of the submitted report. */
.viewer-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px 24px;
}

/* ---------- Wizard card, header and progress indicator ---------- */
.wizard-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;
}
.wizard-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.wizard-head-text {
  min-width: 0;
}
.wizard-eyebrow {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: #64748b;
}
.wizard-title {
  margin: 4px 0 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: #1c2145;
}
.wizard-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin: 6px 0 0;
  font-size: 15px;
  line-height: 1.45;
  color: #475569;
}
.wizard-page-badge {
  flex-shrink: 0;
  padding: 7px 14px;
  border-radius: 999px;
  background: #ffd400;
  color: #1c2145;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}
/* Progress indicator: descriptive labels, the current page highlighted and
   completed pages ticked. */
.wizard-progress {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}
.wizard-progress-item {
  flex: 1 1 210px;
  min-width: 0;
}
.wizard-progress-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 52px;
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
}
.wizard-progress-marker {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}
.wizard-progress-item.is-current .wizard-progress-btn {
  border-color: #a5b4fc;
  background: #eef2ff;
  color: #1c2145;
}
.wizard-progress-item.is-current .wizard-progress-marker {
  background: #1c2145;
  color: #fff;
}
.wizard-progress-item.is-done .wizard-progress-btn {
  border-color: #86efac;
  background: #f0fdf4;
  color: #166534;
}
.wizard-progress-item.is-done .wizard-progress-marker {
  background: #16834b;
  color: #fff;
}
.wizard-panel {
  margin-bottom: 4px;
}
/* Shown on page 3 when the reporting stage has no saved Project Workplan. */
.viewer-notice {
  margin: 0;
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 15px;
  line-height: 1.5;
  text-align: center;
}

/* ---------- Fixed modal footer: page navigation ----------
   Styled after the existing modal footers (.modal-footer / .review-footer): one
   top divider, the modal's side padding and the wizard's button sizing. It is a
   sibling of .viewer-scroll, so it never scrolls with the report content. */
.viewer-footer {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}
.viewer-footer-right {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
/* Left group: the page navigation and the "Create a Copy" action. */
.viewer-footer-left {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
/* Same comfortable button size as the submission wizard. */
.viewer-footer .btn-submit,
.viewer-footer .btn-secondary,
.viewer-footer .btn-copy {
  min-height: 46px;
  padding: 12px 26px;
  font-size: 15.5px;
}
.btn-secondary {
  background: #e2e8f0;
  color: #1c2145;
  border: none;
  padding: 12px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
/* "Create a Copy" reuses the existing primary-action shape with the outline
   treatment of a secondary action, so it never competes with the page
   navigation. */
.btn-copy {
  background: #fff;
  color: #1c2145;
  border: 2px solid #ffd400;
  padding: 10px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-copy:hover:not(:disabled) {
  background: #ffd400;
}
.btn-copy:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-submit {
  background: #ffd400;
  color: #1c2145;
  border: none;
  padding: 12px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

/* ---------- On-screen reading scale for the report document ----------
   The report sheets are authored for a printed A4 page (reportDocument.css sets
   .doc-sheet { width: 210mm; min-height: 297mm; font-size: 10px }), which is far
   too small - and far too tall - on a screen. Inside the viewer the submitted
   report is presented at the same comfortable reading size as the submission
   form, and min-height: 0 keeps the sheet content-driven so no empty A4 page is
   left below the report. Scoped to @media screen, so the printed document keeps
   its existing A4 styling. */
@media screen {
  .qpr-viewer :deep(.doc-sheet) {
    width: 100%;
    min-width: 760px;
    min-height: 0; /* content-driven height: no empty A4 sheet */
    font-size: 16px;
    line-height: 1.5;
  }
  .qpr-viewer :deep(.doc-body) {
    padding: 16px 20px 20px;
  }
  /* Document header */
  .qpr-viewer :deep(.doc-header) {
    min-height: 88px;
    padding: 14px 20px;
  }
  .qpr-viewer :deep(.doc-emblem) {
    width: 40px;
    height: 40px;
  }
  .qpr-viewer :deep(.doc-eyebrow) {
    font-size: 12px;
  }
  .qpr-viewer :deep(.doc-title) {
    font-size: 28px;
  }
  .qpr-viewer :deep(.doc-badge) {
    padding: 6px 14px;
    font-size: 14px;
  }
  .qpr-viewer :deep(.doc-badge-sub) {
    font-size: 13px;
  }
  /* Section headings */
  .qpr-viewer :deep(.doc-section) {
    gap: 10px;
    margin: 24px 0 12px;
    font-size: 21px;
  }
  .qpr-viewer :deep(.doc-section-accent) {
    width: 4px;
    height: 24px;
  }
  .qpr-viewer :deep(.doc-subsection) {
    margin: 16px 0 8px;
    font-size: 17px;
  }
  .qpr-viewer :deep(.doc-hint) {
    margin: 0 0 10px;
    font-size: 14.5px;
  }
  /* Labels, values and narrative notes */
  .qpr-viewer :deep(.doc-label) {
    font-size: 15px;
    letter-spacing: 0.2px;
  }
  .qpr-viewer :deep(.doc-value) {
    margin-top: 4px;
    font-size: 16px;
  }
  .qpr-viewer :deep(.doc-note) {
    padding: 12px 14px 12px 20px;
    font-size: 16px;
  }
  /* Document tables */
  .qpr-viewer :deep(.doc-info-table td) {
    padding: 10px 12px;
  }
  .qpr-viewer :deep(.doc-table th) {
    padding: 9px 8px;
    font-size: 13.5px;
  }
  .qpr-viewer :deep(.doc-table td) {
    padding: 8px 8px;
    font-size: 15px;
  }
  /* Submitted values. Only typography is scaled: read-only values have no
     control to size, so no minimum height is applied (nothing empty is left
     behind on a page). */
  .qpr-viewer :deep(.doc-sheet .doc-value) {
    padding: 8px 0;
  }
  .qpr-viewer :deep(.doc-sheet .qr-cell-text),
  .qpr-viewer :deep(.doc-sheet .qr-cell-value) {
    font-size: 15px;
  }
  .qpr-viewer :deep(.doc-sheet .qr-note-text) {
    font-size: 16px;
  }
  .qpr-viewer :deep(.wp-cell-text),
  .qpr-viewer :deep(.wp-note-text) {
    font-size: 15px;
  }
  /* The workplan grid carries 12 month columns beside the objectives column, so
     its cells use slightly smaller text and tighter padding to stay readable
     instead of wrapping every word. */
  .qpr-viewer :deep(.doc-sheet .wp-table th),
  .qpr-viewer :deep(.doc-sheet .wp-table td) {
    padding: 6px 5px;
  }
  /* Signatures */
  .qpr-viewer :deep(.wp-sign-name) {
    font-size: 16px;
  }
  .qpr-viewer :deep(.wp-sign-role) {
    font-size: 13.5px;
  }
  /* Document footer */
  .qpr-viewer :deep(.doc-footer) {
    padding: 10px 20px;
    font-size: 12.5px;
  }
  .qpr-viewer :deep(.doc-page-badge) {
    padding: 4px 12px;
    font-size: 12.5px;
  }
}

/* Keep the progress indicator and the page navigation usable on small screens. */
@media (max-width: 640px) {
  .wizard-card {
    padding: 16px;
  }
  .wizard-title {
    font-size: 19px;
  }
  .wizard-head {
    flex-direction: column;
  }
  .wizard-progress-item {
    flex: 1 1 100%;
  }
  .viewer-footer,
  .viewer-footer-left,
  .viewer-footer-right {
    flex-direction: column;
    align-items: stretch;
  }
  .viewer-footer .btn-submit,
  .viewer-footer .btn-secondary,
  .viewer-footer .btn-copy {
    width: 100%;
  }
}
</style>
