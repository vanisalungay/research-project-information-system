/**
 * sdg.js
 * Central definition of the official 17 Sustainable Development Goals and
 * helpers for normalizing/reading the selected SDGs stored on a proposal.
 */

export const SDG_OPTIONS = [
  'No Poverty',
  'Zero Hunger',
  'Good Health and Well-being',
  'Quality Education',
  'Gender Equality',
  'Clean Water and Sanitation',
  'Affordable and Clean Energy',
  'Decent Work and Economic Growth',
  'Industry, Innovation and Infrastructure',
  'Reduced Inequalities',
  'Sustainable Cities and Communities',
  'Responsible Consumption and Production',
  'Climate Action',
  'Life Below Water',
  'Life on Land',
  'Peace, Justice and Strong Institutions',
  'Partnerships for the Goals',
]

/**
 * Normalize any stored SDG value into an array of selected SDG names.
 * Accepts an array (new format) or a free-form string (legacy format that may
 * be separated by semicolons or newlines).
 */
export function sdgToList(value) {
  if (Array.isArray(value)) {
    return value.map((v) => String(v).trim()).filter(Boolean)
  }
  if (value == null) {
    return []
  }
  const text = String(value).trim()
  if (!text) {
    return []
  }
  return text
    .replace(/\r/g, '\n')
    .split(/[;\n]/)
    .map((v) => v.trim())
    .filter(Boolean)
}

/**
 * Convert a stored SDG value into a Set of selected SDG names for quick lookup.
 */
export function sdgToSet(value) {
  return new Set(sdgToList(value))
}
