/**
 * reviewScores.js
 * ---------------------------------------------------------------------------
 * The four weighted groups of the REC evaluation form and the score arithmetic
 * used when a proposal's evaluation is printed.
 *
 * The 15 score columns are stored one per criterion (1-5 each); the document only
 * reports the group averages and the overall weighted score, so the arithmetic is
 * defined once here and can be tested without the print renderer.
 */

/** The evaluation groups exactly as ProposalReview exposes their score fields. */
export const REVIEW_GROUPS = [
  {
    key: 'soundness',
    label: 'A. Soundness of Proposal',
    weight: 20,
    fields: [
      'soundnessSectorNeedScore',
      'soundnessEffectiveSolutionScore',
      'soundnessReasonableBudgetScore',
      'soundnessDoableWorkplanScore',
    ],
  },
  {
    key: 'suitability',
    label: 'B. Suitability of Output',
    weight: 30,
    fields: [
      'suitabilityCostEffectiveScore',
      'suitabilityPartnersIdentifiedScore',
      'suitabilityCommercializationScore',
      'suitabilityTimelyUtilizationScore',
    ],
  },
  {
    key: 'significance',
    label: 'C. Significance of Outcome',
    weight: 30,
    fields: [
      'significanceEconomicImpactScore',
      'significanceSocialImpactScore',
      'significanceEnvironmentalImpactScore',
      'significanceSustainabilityScore',
    ],
  },
  {
    key: 'competence',
    label: 'D. Competence of Proponent',
    weight: 20,
    fields: [
      'competenceAlignmentScore',
      'competenceCollaborationScore',
      'competenceDeliverablesUnderstandingScore',
    ],
  },
]

/**
 * Mean of the numeric 1-5 scores present in a review, or null when the review
 * carries none for that group. Out-of-range values are ignored rather than
 * silently skewing the average.
 */
export function groupAverage(review, fields) {
  const values = (fields || [])
    .map((field) => review?.[field])
    .filter((value) => Number.isFinite(value) && value >= 1 && value <= 5)
  if (!values.length) return null
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

/**
 * The weighted score (1-5) of a review. Only groups that carry scores are
 * counted and the weights are renormalized over them, so a partially filled
 * review still yields a meaningful average. Returns null when no group is scored.
 */
export function reviewWeightedScore(review) {
  let weighted = 0
  let weight = 0
  for (const group of REVIEW_GROUPS) {
    const average = groupAverage(review, group.fields)
    if (average == null) continue
    weighted += average * group.weight
    weight += group.weight
  }
  return weight ? weighted / weight : null
}
