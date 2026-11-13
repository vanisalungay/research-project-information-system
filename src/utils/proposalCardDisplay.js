/**
 * proposalCardDisplay.js
 * ---------------------------------------------------------------------------
 * Small presentation helpers for the OVCAF proposal information card. Every value the
 * card shows must come from the saved proposal record, so these helpers only reshape
 * data the card already has - they never invent a name, a college or a file.
 *
 *   initialsFromName('Andrea Mae Villanueva')            -> 'AV'
 *   collegeDisplayParts('College of ... (CELS)')         -> { name: 'College of ...', abbreviation: 'CELS' }
 *   fileTypeDescription('line-item-budget.pdf')          -> 'PDF document'
 *
 * Kept in one place so the card can be tested without parsing a Vue single-file component.
 */

// Honorifics and academic suffixes are not part of a person's initials.
const NAME_PREFIXES = new Set(['dr', 'dr.', 'prof', 'prof.', 'mr', 'mr.', 'ms', 'ms.', 'mrs', 'mrs.', 'engr', 'engr.', 'atty', 'atty.'])
const NAME_SUFFIXES = new Set(['phd', 'ph.d.', 'md', 'm.d.', 'jr', 'jr.', 'sr', 'sr.', 'ii', 'iii', 'iv'])

/**
 * The initials a circular avatar shows: the first letter of the first and last name.
 * A single-word name keeps one letter; a missing name yields ''. Never fabricates a
 * name from an account.
 *
 * @param {string} [name]
 * @returns {string} one or two uppercase letters
 */
export function initialsFromName(name) {
  const words = String(name ?? '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .filter((word) => !NAME_PREFIXES.has(word.toLowerCase()) && !NAME_SUFFIXES.has(word.toLowerCase()))
    .map((word) => word.replace(/^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$/gu, ''))
    .filter(Boolean)

  if (words.length === 0) return ''
  const first = words[0]
  const last = words.length > 1 ? words[words.length - 1] : ''
  const letters = [first, last]
    .filter(Boolean)
    .map((word) => Array.from(word)[0].toUpperCase())
    .join('')
  return letters
}

const ABBREVIATION_STOP_WORDS = new Set(['of', 'and', 'the', 'for', 'in', 'on', 'at', 'to', 'a', 'an', '&'])

/**
 * Split a saved college value into the full name and the abbreviation badge.
 *
 * The Create Proposal form stores the official value with its abbreviation in
 * parentheses, e.g. "College of Environment and Life Sciences (CELS)". The card shows
 * the name without the parentheses and the abbreviation as its own pill. A record
 * without the parentheses gets an abbreviation derived from the significant words, so
 * no value is invented. 'Unknown' (the OVCAF fallback) has no badge.
 *
 * @param {string} [college]
 * @returns {{ name: string, abbreviation: string }}
 */
export function collegeDisplayParts(college) {
  const raw = String(college ?? '').trim().replace(/\s+/g, ' ')
  if (!raw) return { name: '', abbreviation: '' }

  const parenthesised = raw.match(/^(.*?)\s*\(([^()]+)\)\s*$/)
  if (parenthesised) {
    const name = parenthesised[1].trim()
    const abbreviation = parenthesised[2].trim().toUpperCase()
    return abbreviation
      ? { name: name || raw, abbreviation }
      : { name: name || raw, abbreviation: '' }
  }

  if (/^(unknown|n\/a|none)$/i.test(raw)) return { name: raw, abbreviation: '' }

  const abbreviation = raw
    .split(/[^\p{L}\p{N}]+/u)
    .filter(Boolean)
    .filter((word) => !ABBREVIATION_STOP_WORDS.has(word.toLowerCase()))
    .map((word) => Array.from(word)[0].toUpperCase())
    .join('')
    .slice(0, 6)

  return { name: raw, abbreviation }
}

const FILE_TYPES = {
  pdf: 'PDF document',
  doc: 'Word document',
  docx: 'Word document',
  xls: 'Spreadsheet',
  xlsx: 'Spreadsheet',
  csv: 'Spreadsheet',
  ppt: 'Presentation',
  pptx: 'Presentation',
  png: 'Image',
  jpg: 'Image',
  jpeg: 'Image',
  gif: 'Image',
  webp: 'Image',
  txt: 'Text document',
}

/**
 * A short description of the uploaded file, read from its extension. An unknown
 * extension is still a document; no name yields no description.
 *
 * @param {string} [fileName]
 * @returns {string}
 */
export function fileTypeDescription(fileName) {
  const name = String(fileName ?? '').trim()
  if (!name) return ''
  const dot = name.lastIndexOf('.')
  if (dot < 0 || dot === name.length - 1) return 'Document'
  const extension = name.slice(dot + 1).toLowerCase()
  return FILE_TYPES[extension] || 'Document'
}
