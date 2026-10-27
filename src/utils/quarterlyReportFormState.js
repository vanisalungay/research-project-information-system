/**
 * quarterlyReportFormState.js
 * ---------------------------------------------------------------------------
 * The form state of a Quarterly Progress Report: how a saved report becomes the form's
 * reactive state, and how that state becomes the payload the existing save/submit
 * endpoints receive.
 *
 * Both directions live here instead of inside the components so the mapping can be
 * verified without a browser (see quarterlyReportFormState.test.js) - and so the values
 * shown in the form are exactly the values that are saved.
 *
 * The report entity, the form and the payload use the same field names, so nothing has to
 * be renamed: the rules below only add the form's own defaults and keep every value the
 * report carries (0, false and empty arrays included).
 *
 * Run the rules with: npm test
 */

/** The 6Ps output types of section C, in the order the form shows them. */
export const OUTPUT_TYPES = [
  'Publications',
  'Patents/IP',
  'Products',
  'People Services',
  'Partnerships',
  'Policy',
]

/** Twelve month columns of the Project Workplan. */
export const WORKPLAN_MONTHS = 12

/** Minimum number of objective rows the editable Project Workplan shows. */
export const MIN_WORKPLAN_ROWS = 3

/** Text of any value, without turning 0 or false into an empty string. */
function text(value) {
  if (value === null || value === undefined) return ''
  return typeof value === 'string' ? value : String(value)
}

function listOf(value) {
  return Array.isArray(value) ? value : []
}

/** A blank objective row of section A (objectives, accomplishments, percentages). */
export function emptyObjective() {
  return {
    objective: '',
    target: '',
    actual: '',
    percentagePeriod: '',
    percentageCumulative: '',
  }
}

/** The blank 6Ps rows of section C, one per output type. */
export function seedOutputs() {
  return OUTPUT_TYPES.map((outputType) => ({ outputType, expectedOutput: '', actualOutput: '' }))
}

/**
 * The form state of a saved report - or of the blank report the Create workflow builds.
 * Every field the report carries is applied as it is; the form's defaults only fill what
 * the report does not provide, so a copied value is never replaced by an empty default.
 * Rows keep every property they carry, so a row field the form does not know yet is not
 * silently dropped.
 */
export function reportFormState(report) {
  const r = report && typeof report === 'object' ? report : {}
  const objectives = listOf(r.objectives)
  const rows = objectives.length ? objectives : [emptyObjective()]

  return {
    period: text(r.period) || 'Q1',
    year: r.year ?? new Date().getFullYear(),
    projectTitle: text(r.projectTitle),
    projectLeader: text(r.projectLeader),
    leaderGender: text(r.leaderGender),
    baseStation: text(r.baseStation),
    sitesOfImplementation: text(r.sitesOfImplementation),
    projectDuration: text(r.projectDuration),
    projectStartDate: text(r.projectStartDate),
    projectEndDate: text(r.projectEndDate),
    catchUpPlan: text(r.catchUpPlan),
    problemsConcerns: text(r.problemsConcerns),
    suggestedSolutions: text(r.suggestedSolutions),
    objectives: rows.map((row) => ({ ...emptyObjective(), ...row })),
    outputs: seedOutputs().map((seed) => {
      const found = listOf(r.outputs).find((row) => row?.outputType === seed.outputType)
      return { ...seed, ...(found || {}) }
    }),
  }
}

/**
 * The payload of the existing save/submit endpoints, built from the form state. It is the
 * same data the form displays - nothing is renamed, dropped or defaulted away.
 */
export function reportPayload(form) {
  const f = form && typeof form === 'object' ? form : {}

  return {
    period: f.period,
    year: f.year,
    projectTitle: f.projectTitle,
    projectLeader: f.projectLeader,
    leaderGender: f.leaderGender,
    baseStation: f.baseStation,
    sitesOfImplementation: f.sitesOfImplementation,
    projectDuration: f.projectDuration,
    projectStartDate: f.projectStartDate || null,
    projectEndDate: f.projectEndDate || null,
    catchUpPlan: f.catchUpPlan,
    problemsConcerns: f.problemsConcerns,
    suggestedSolutions: f.suggestedSolutions,
    objectives: listOf(f.objectives),
    outputs: listOf(f.outputs),
  }
}

/** A blank Project Workplan objective row (objective + twelve month cells + total). */
export function emptyWorkplanObjective() {
  return { objective: '', months: Array(WORKPLAN_MONTHS).fill(''), total: '' }
}

/** A saved workplan row with exactly twelve month cells and text values. */
export function normalizeWorkplanObjective(row) {
  const months = listOf(row?.months)
    .slice(0, WORKPLAN_MONTHS)
    .map((month) => text(month))
  while (months.length < WORKPLAN_MONTHS) months.push('')
  return { objective: text(row?.objective), months, total: text(row?.total) }
}

/**
 * The editable Project Workplan of a report: the saved workplan when one exists, seeded
 * from the proposal otherwise. Saved values always win, so reopening a report restores
 * exactly what the proponent entered - and a copied workplan keeps its as-of date, header,
 * month cells and remarks.
 */
export function workplanFormState(saved, proposal) {
  const objectives = listOf(saved?.objectives).map(normalizeWorkplanObjective)
  while (objectives.length < MIN_WORKPLAN_ROWS) objectives.push(emptyWorkplanObjective())

  return {
    asOf: text(saved?.asOf),
    projectTitle: text(saved?.projectTitle) || text(proposal?.projectTitle),
    projectLeader: text(saved?.projectLeader) || text(proposal?.projectLeader),
    projectDuration: text(saved?.projectDuration) || text(proposal?.duration),
    objectives,
    remarks: text(saved?.remarks),
  }
}

/** The Project Workplan payload saved with the report (same fields, same values). */
export function workplanPayload(workplan) {
  const w = workplan && typeof workplan === 'object' ? workplan : {}

  return {
    asOf: text(w.asOf),
    projectTitle: text(w.projectTitle),
    projectLeader: text(w.projectLeader),
    projectDuration: text(w.projectDuration),
    objectives: listOf(w.objectives).map((row) => ({
      objective: text(row?.objective),
      months: listOf(row?.months)
        .slice(0, WORKPLAN_MONTHS)
        .map((month) => text(month)),
      total: text(row?.total),
    })),
    remarks: text(w.remarks),
  }
}
