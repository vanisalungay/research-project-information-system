/**
 * proposalSubsteps.js
 * ---------------------------------------------------------------------------
 * The pages ("substeps") of the Create Proposal form's main step 2 - Proposal
 * Details - and the small rules that drive the Next / Previous navigation.
 *
 * Step 2 has 27 numbered sections. Instead of one very long page they are shown as
 * consecutive groups, so a proponent completes a small set of fields at a time:
 *
 *   const pages = PROPOSAL_SUBSTEPS.map((page) => page.title)
 *
 * The groups are slices of the existing order - each section appears in exactly one
 * page, in the order the form already renders them - so no field is moved, renamed
 * or removed by the multi-page presentation. The four main steps of the workflow are
 * untouched: these pages only exist inside step 2.
 *
 * Validation is not duplicated here: a page "owns" the fields whose section it
 * contains, taken from the form's own field -> section map (`sectionRefs`, which the
 * form already uses to scroll to an error). `validateStep1()` stays the single
 * source of the rules.
 */

/** The pages of step 2, in order. `sections` are the form's own section refs. */
export const PROPOSAL_SUBSTEPS = [
  {
    id: 'project-profile',
    title: 'Project Profile',
    sections: ['section_project_profile'],
  },
  {
    id: 'agencies-sites',
    title: 'Agencies, Sites & Research Focus',
    sections: [
      'section_cooperating_agencies',
      'section_sites',
      'section_research_type',
      'section_priority_agendas',
    ],
  },
  {
    id: 'background',
    title: 'Project Background',
    sections: [
      'section_innovation_goals',
      'section_sector_relevance',
      'section_sdg',
      'section_executive_summary',
      'section_introduction',
    ],
  },
  {
    id: 'methodology',
    title: 'Methodology & Roadmap',
    sections: [
      'section_review_of_literature',
      'section_methodology',
      'section_technology',
      'section_expected_outputs',
      'section_potential_outcomes',
      'section_impacts',
    ],
  },
  {
    id: 'plans',
    title: 'Beneficiaries, GAD & Plans',
    sections: [
      'section_target_beneficiaries',
      'section_sustainability_plan',
      'section_gad_score',
      'section_limitations',
      'section_risks',
      'section_logical_framework',
    ],
  },
  {
    id: 'references-budget',
    title: 'References, Team & Budget',
    sections: [
      'section_literature_cited',
      'section_personnel',
      'section_budget',
      'section_other_projects',
      'section_supporting_documents',
    ],
  },
]

/** How many pages step 2 has. */
export const PROPOSAL_SUBSTEP_COUNT = PROPOSAL_SUBSTEPS.length

/** Keeps a page number inside the pages that exist. */
export function clampSubstep(number) {
  const value = Number.isFinite(Number(number)) ? Math.trunc(Number(number)) : 1
  return Math.min(Math.max(value, 1), PROPOSAL_SUBSTEP_COUNT)
}

/** The sections shown on one page (1-based); an unknown page has none. */
export function sectionsOfSubstep(number) {
  return PROPOSAL_SUBSTEPS[number - 1]?.sections || []
}

/** The title of one page ('' for an unknown page). */
export function substepTitle(number) {
  return PROPOSAL_SUBSTEPS[number - 1]?.title || ''
}

/**
 * The validation fields of one page: every field of `sectionRefs` whose section is
 * rendered on that page. Deriving them keeps the rules in one place.
 */
export function fieldsOfSubstep(sectionRefs, number) {
  const sections = new Set(sectionsOfSubstep(number))
  return Object.entries(sectionRefs || {})
    .filter(([, section]) => sections.has(section))
    .map(([field]) => field)
}

/** The errors of one page, keeping the order the validation produced them in. */
export function errorsOfSubstep(errors, sectionRefs, number) {
  const fields = new Set(fieldsOfSubstep(sectionRefs, number))
  return (Array.isArray(errors) ? errors : []).filter((entry) => fields.has(entry?.field))
}

/** The page a field belongs to, or null when no page covers it. */
export function substepOfField(sectionRefs, field) {
  const section = (sectionRefs || {})[field]
  if (!section) return null
  const index = PROPOSAL_SUBSTEPS.findIndex((substep) => substep.sections.includes(section))
  return index < 0 ? null : index + 1
}

/**
 * The fields of `sectionRefs` that no page covers.
 *
 * Must stay empty: a field without a page would never be shown, and its error could
 * not be reported on the page the proponent is looking at.
 */
export function uncoveredFields(sectionRefs) {
  return Object.keys(sectionRefs || {}).filter(
    (field) => substepOfField(sectionRefs, field) === null,
  )
}
