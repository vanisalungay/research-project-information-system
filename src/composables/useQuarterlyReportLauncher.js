/**
 * useQuarterlyReportLauncher.js
 * ---------------------------------------------------------------------------
 * The one way the proponent interface opens the Quarterly Progress Reports
 * overview for a project.
 *
 * The Project Details page and Proposal Monitoring both call this composable, so
 * they cannot disagree about eligibility or the target route. It only navigates
 * to the existing overview page: the overview loads the project's reports and
 * resolves the next required stage itself, and its own Start Quarterly Report
 * button opens the report form. Nothing is created here.
 *
 * The backend re-checks the reporting rule on every quarterly-report request, so
 * the hidden button is never the authorization.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { isQuarterlyReportEligible } from '@/utils/quarterlyReport'

export function useQuarterlyReportLauncher() {
  const router = useRouter()

  /** Id of the project currently being opened, so its button can show progress. */
  const openingReportId = ref(null)

  /** The shared eligibility rule (approved for implementation + issued SO). */
  const canCreateQuarterlyReport = (proposal) => isQuarterlyReportEligible(proposal)

  /**
   * Opens the existing Quarterly Progress Reports overview for the project. The
   * overview page reads the submitted reports and the next required stage, so this
   * never guesses a quarter, opens the form directly or creates a report record.
   */
  async function openQuarterlyReport(proposal) {
    if (!proposal?.id || !canCreateQuarterlyReport(proposal) || openingReportId.value) return

    openingReportId.value = proposal.id
    try {
      await router.push({
        name: 'ProponentQuarterlyReports',
        params: { proposalId: proposal.id },
      })
    } finally {
      openingReportId.value = null
    }
  }

  return { openingReportId, canCreateQuarterlyReport, openQuarterlyReport }
}
