/**
 * useQuarterlyReportLauncher.js
 * ---------------------------------------------------------------------------
 * The one way the proponent interface opens the existing quarterly-report
 * creation form.
 *
 * The Project Details page and Proposal Monitoring both call this composable,
 * so they cannot disagree about eligibility, the report stage or the target
 * route. It only navigates: the report row is written by the existing form when
 * the proponent saves a draft or submits it.
 *
 * The backend re-checks the same rule on every quarterly-report request, so the
 * hidden button is never the authorization.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { useDialog } from '@/composables/useDialog'
import { isQuarterlyReportEligible, nextQuarterlyReportPath } from '@/utils/quarterlyReport'

export function useQuarterlyReportLauncher() {
  const router = useRouter()
  const { showAlert } = useDialog()

  /** Id of the project currently being opened, so its button can show progress. */
  const openingReportId = ref(null)

  /** The shared eligibility rule (approved for implementation + issued SO). */
  const canCreateQuarterlyReport = (proposal) => isQuarterlyReportEligible(proposal)

  /**
   * Opens the existing quarterly-report form for the project's next required
   * stage. An unfinished draft is continued instead of duplicated, and a project
   * whose reporting is already complete is reported rather than shown an empty
   * form. Nothing is created here.
   */
  async function openQuarterlyReport(proposal) {
    if (!proposal?.id || !canCreateQuarterlyReport(proposal) || openingReportId.value) return

    openingReportId.value = proposal.id
    try {
      const res = await api.get(`/api/quarterly-reports/next-stage/${proposal.id}`)
      const path = nextQuarterlyReportPath(proposal.id, res.data)
      if (!path) {
        await showAlert('All required reports for this project have already been submitted.', {
          type: 'info',
          title: 'Quarterly Reporting',
        })
        return
      }
      router.push(path)
    } catch (err) {
      console.error('Could not open the quarterly report form:', err)
      const body = err.response?.data
      const message =
        (body && typeof body === 'object' ? body.error : body) ||
        'Quarterly reporting is not available for this project.'
      await showAlert(String(message), { type: 'error', title: 'Quarterly Report' })
    } finally {
      openingReportId.value = null
    }
  }

  return { openingReportId, canCreateQuarterlyReport, openQuarterlyReport }
}
