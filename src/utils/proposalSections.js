/**
 * proposalSections.js
 * Central definition of the numbered proposal sections.
 *
 * The same numbering and titles are used by the proposal form
 * (ProponentSubmitProp.vue), the Revision Page (ProponentSubmitRev.vue) and the
 * backend ProposalSectionCatalog, so a reviewer's remark always refers to the
 * same part of the proposal on every screen.
 */

export const PROPOSAL_SECTIONS = [
  { key: '01', label: 'Project Profile' },
  { key: '02', label: 'Cooperating Agencies' },
  { key: '03', label: 'Sites of Implementation' },
  { key: '04', label: 'Type of Research' },
  { key: '05', label: 'Priority Agenda' },
  { key: '06', label: 'Innovation Goals' },
  { key: '07', label: 'Sector Relevance' },
  { key: '08', label: 'Sustainable Development Goals (SDG)' },
  { key: '09', label: 'Executive Summary' },
  { key: '10', label: 'Introduction' },
  { key: '11', label: 'Review of Literature' },
  { key: '12', label: 'Methodology' },
  { key: '13', label: 'Technology Roadmap' },
  { key: '14', label: 'Expected Outputs (6Ps)' },
  { key: '15', label: 'Potential Outcomes' },
  { key: '16', label: 'Potential Impacts (2Is)' },
  { key: '17', label: 'Target Beneficiaries' },
  { key: '18', label: 'Sustainability Plan' },
  { key: '19', label: 'Gender and Development (GAD) Score' },
  { key: '20', label: 'Limitations of the Project' },
  { key: '21', label: 'Risk Management Plan' },
  { key: '22', label: 'Logical Framework' },
  { key: '23', label: 'Literature Cited' },
  { key: '24', label: 'Personnel Requirement' },
  { key: '25', label: 'Line-Item Budget Requirement' },
  { key: '26', label: 'Other Ongoing Projects' },
  { key: '27', label: 'Other Supporting Documents' },
]

/** Canonical label for a section key ("12" -> "Methodology"). */
export function sectionLabel(key) {
  const normalized = normalizeSectionKey(key)
  const match = PROPOSAL_SECTIONS.find((section) => section.key === normalized)
  return match ? match.label : String(key ?? '')
}

/** Normalizes "12", "012" or "section_12" to the canonical two-digit key. */
export function normalizeSectionKey(key) {
  if (key === null || key === undefined) return ''
  const value = String(key).trim().toLowerCase().replace(/^section_/, '')
  if (/^\d{1,2}$/.test(value)) return value.padStart(2, '0')
  return value
}

/**
 * Flattens a `GET /api/proposals/{id}/section-remarks` response into
 * `{ '12': [remark, ...] }` for the currently active revision round, so each
 * remark can be rendered beside its own section.
 */
export function activeRemarksBySection(response) {
  const map = {}
  const rounds = response?.rounds || []
  const activeRound =
    response?.activeRound != null
      ? rounds.find((round) => round.round === response.activeRound)
      : rounds.find((round) => round.active)
  for (const item of activeRound?.remarks || []) {
    // Field-level remarks are rendered beside their own field, not as a section notice.
    if (item.fieldKey) continue
    const key = normalizeSectionKey(item.sectionKey)
    if (!map[key]) map[key] = []
    map[key].push(item)
  }
  return map
}

/** Every round except the active one, newest first (kept as revision history). */
export function previousRounds(response) {
  return (response?.rounds || []).filter((round) => !round.active)
}

/**
 * Converts the editor's `{ '12': 'remark' }` map into the API payload shape
 * (`[{ sectionKey, sectionLabel, remark }]`), dropping empty entries.
 */
export function toSectionRemarkPayload(map) {
  return Object.entries(map || {})
    .map(([key, value]) => ({
      sectionKey: normalizeSectionKey(key),
      sectionLabel: sectionLabel(key),
      remark: String(value ?? '').trim(),
    }))
    .filter((entry) => entry.remark !== '')
}
