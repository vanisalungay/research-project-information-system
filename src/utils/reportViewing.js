/**
 * reportViewing.js
 * ---------------------------------------------------------------------------
 * Presentation helpers shared by the office-wide quarterly-report pages
 * (QuarterlyReportsMaster for ORPS/OVCRIGE and the OVCAF read-only page).
 *
 * These derive everything from the stored report record: there is no display-only
 * state and no invented name. The values match the Proponent's report wizard.
 */

/** True when the row is the final stage's Terminal Report. */
export function isTerminalReport(report) {
  return report?.reportType === 'TERMINAL_REPORT'
}

/** "Quarterly Progress Report" or "Terminal Report". */
export function reportTypeLabel(report) {
  return isTerminalReport(report) ? 'Terminal Report' : 'Quarterly Progress Report'
}

/** "Q3 2026" for a quarterly report; empty for the terminal stage. */
export function reportPeriodLabel(report) {
  if (isTerminalReport(report)) return ''
  return `${report?.period || ''} ${report?.year || ''}`.trim()
}

/** The report column value, e.g. "Quarterly Progress Report — Q3 2026". */
export function reportName(report) {
  const period = reportPeriodLabel(report)
  return period ? `${reportTypeLabel(report)} — ${period}` : reportTypeLabel(report)
}

/** A readable submission date, or '' when the report has none. */
export function formatReportDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}
