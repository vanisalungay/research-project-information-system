/**
 * endorsementColleges.js
 * ---------------------------------------------------------------------------
 * The implementing college/department values the RPIS uses. They are the exact
 * values the Create Proposal form offers (ProponentSubmitProp.vue, section 01), so
 * an endorsement request names its college the same way the proposal that follows
 * it does - which is also what routes the request to the Dean authorized for it.
 *
 * Kept in one place so the Request for Endorsement form and the Dean's scope
 * selector cannot drift apart. Adding a college is a data change: it must be added
 * here and to the Create Proposal form's dropdown together.
 */
export const RPIS_COLLEGES = [
  'College of Fisheries and Marine Sciences (CFMS)',
  'College of Environment and Life Sciences (CELS)',
  'College of Education and Social Sciences (CESS)',
  'College of Business and Information Technology (CBIT)',
  'Integrated Development School (IDS)',
]

/** True when the given value is one of the official colleges. */
export function isKnownCollege(value) {
  return RPIS_COLLEGES.includes(String(value ?? '').trim())
}
