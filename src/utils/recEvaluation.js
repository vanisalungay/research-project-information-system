/**
 * recEvaluation.js
 * ---------------------------------------------------------------------------
 * The REC approval rule, in one place:
 *  - every evaluation criterion must have a whole-number score from 1 to 5;
 *  - an overall comment is required, and whitespace-only text does not count.
 *
 * The scoring system, its criteria and its weights are unchanged; this module only
 * decides whether an approval may be submitted. The backend enforces the same rule,
 * so a direct API request cannot bypass the form.
 */

/** The allowed score range shown by the evaluation form. */
export const EVALUATION_SCORE_MIN = 1
export const EVALUATION_SCORE_MAX = 5

/** A valid score is a whole number 1-5; anything else (including '') is null. */
export function normalizeEvaluationScore(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isInteger(number)
    && number >= EVALUATION_SCORE_MIN
    && number <= EVALUATION_SCORE_MAX
    ? number
    : null
}

/** The criteria that still have no valid score. */
export function missingEvaluationScores(scores, fields) {
  const list = Array.isArray(fields) ? fields : []
  return list.filter((field) => normalizeEvaluationScore(scores?.[field]) === null)
}

/**
 * The message shown when approval is not possible yet; '' when it is ready.
 * The message names the missing requirement so the reviewer knows what to fix.
 */
export function evaluationApprovalError(scores, fields, comment) {
  const missing = missingEvaluationScores(scores, fields)
  if (missing.length > 0) {
    return `Complete all ${fields.length} evaluation criteria before approving this proposal. `
      + `${missing.length} score${missing.length === 1 ? ' is' : 's are'} still missing.`
  }
  if (!String(comment ?? '').trim()) {
    return 'An overall comment is required before approving this proposal.'
  }
  return ''
}

/** True only when every score and the comment satisfy the approval rule. */
export function isEvaluationApprovalReady(scores, fields, comment) {
  return evaluationApprovalError(scores, fields, comment) === ''
}
