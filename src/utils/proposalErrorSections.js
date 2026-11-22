/**
 * proposalErrorSections.js
 * ---------------------------------------------------------------------------
 * Groups the Create Proposal form's validation errors by the section that owns
 * them, so the error banner can name every incomplete section and navigate to
 * it.
 *
 * The form already owns the two inputs this needs:
 *   - `sectionRefs` (field key -> section ref) in the component;
 *   - PROPOSAL_SUBSTEPS, whose ordered `sections` are the same refs in the same
 *     order as PROPOSAL_SECTIONS (01..27).
 *
 * Nothing is duplicated: the section number and title come from the shared
 * catalogue, the page number from the substep definition, and the errors from
 * the one validator.
 */
import { PROPOSAL_SECTIONS } from './proposalSections.js'
import { PROPOSAL_SUBSTEPS } from './proposalSubsteps.js'

/**
 * Every section ref with its number, title and the page ("substep") it appears on.
 * Built from the single ordered list of pages, so the mapping cannot drift from
 * the form's own navigation.
 */
export const SECTION_NAVIGATION = (() => {
  const navigation = {}
  let sectionIndex = 0
  PROPOSAL_SUBSTEPS.forEach((substep, substepIndex) => {
    substep.sections.forEach((ref) => {
      const section = PROPOSAL_SECTIONS[sectionIndex]
      if (section) {
        navigation[ref] = {
          ref,
          number: section.key,
          title: section.label,
          substep: substepIndex + 1,
        }
      }
      sectionIndex += 1
    })
  })
  return navigation
})()

/** The navigation record of one section ref, or null. */
export function sectionNavigation(ref) {
  return SECTION_NAVIGATION[ref] || null
}

/**
 * Groups `errors` (the validator's `{ field, message }` list) by the section
 * their field belongs to, preserving the order in which the validation produced
 * them. A field with no section mapping is kept under a fallback group so it is
 * never silently hidden.
 *
 * @param {{field: string, message: string}[]} errors
 * @param {Record<string, string>} sectionRefs field -> section ref
 * @returns {{ref: string, number: string, title: string, substep: number|null, errors: object[]}[]}
 */
export function groupErrorsBySection(errors, sectionRefs) {
  const groups = []
  const byRef = new Map()

  for (const entry of Array.isArray(errors) ? errors : []) {
    const ref = sectionRefs?.[entry?.field] || ''
    const navigation = sectionNavigation(ref)
    const key = navigation?.ref || 'unknown'
    let group = byRef.get(key)
    if (!group) {
      group = {
        ref: navigation?.ref || '',
        number: navigation?.number || '',
        title: navigation?.title || 'Other required information',
        substep: navigation?.substep ?? null,
        errors: [],
      }
      byRef.set(key, group)
      groups.push(group)
    }
    group.errors.push(entry)
  }

  return groups
}
