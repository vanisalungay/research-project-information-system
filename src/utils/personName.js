/**
 * personName.js
 * ---------------------------------------------------------------------------
 * The one place RPIS formats a person's name.
 *
 * Every name field asks for the same order - last name first - and the same
 * title case, so an entry typed in capitals, in lower case or in a mixture of
 * both is stored the same way:
 *
 *   JUAN DELA CRUZ            -> Juan Dela Cruz
 *   juan dela cruz            -> Juan Dela Cruz
 *   JUAN, DELA CRUZ, M.       -> Juan, Dela Cruz, M.
 *   DELA CRUZ, JUAN, M., JR.  -> Dela Cruz, Juan, M., Jr.
 *
 * The formatting is deliberately conservative: commas, periods, spaces,
 * hyphens and apostrophes are kept, only the letters around them are cased.
 * Roman-numeral suffixes (II, III, IV) stay upper case; every other word has
 * its first letter capitalised and the rest lower-cased.
 *
 * It is used on blur (when the user finishes a field) and again before a form
 * is submitted, and it is mirrored by PersonNameFormatter on the backend so a
 * request that bypasses the browser is stored the same way.
 */

/** The instruction shown beside every name field. */
export const PERSON_NAME_FORMAT =
  'Last Name, First Name, Middle Initial (optional), Suffix (optional)'

/** The one example shown with the instruction. */
export const PERSON_NAME_EXAMPLE = 'Dela Cruz, Juan, M., Jr.'

/** One complete sentence, for fields that have room for only one line of help. */
export const PERSON_NAME_HINT = `${PERSON_NAME_FORMAT}. Example: ${PERSON_NAME_EXAMPLE}.`

/** II, III, IV ... are suffixes and keep their capitals; "Jr" is title-cased. */
const ROMAN_NUMERAL = /^(i{1,3}|iv|v|vi{1,3}|ix|x)$/i

/** One run of letters, accents included, so Dela Cruz / O'Brien / Ana-Maria all work. */
const LETTER_RUN = /\p{L}+/gu

/** Title-case one word without disturbing its punctuation. */
function titleCaseWord(word) {
  return word.replace(LETTER_RUN, (run) => {
    if (ROMAN_NUMERAL.test(run)) return run.toUpperCase()
    return run.charAt(0).toUpperCase() + run.slice(1).toLowerCase()
  })
}

/**
 * The standard name format for a single name.
 *
 * The value is split on commas so each component ("Dela Cruz", "Juan", "M.",
 * "Jr.") is cased on its own and the commas are put back with one space after
 * them. Empty components are dropped, so a stray ",," never becomes part of
 * the saved value. A blank value stays blank.
 */
export function formatPersonName(value) {
  if (value === null || value === undefined) return value
  const text = String(value).replace(/\s+/g, ' ').trim()
  if (!text) return ''

  return text
    .split(',')
    .map((segment) => segment.trim())
    .filter((segment) => segment !== '')
    .map((segment) => segment.split(' ').map(titleCaseWord).join(' '))
    .join(', ')
}

/**
 * The same rule for a field that holds several names, one per line (the
 * terminal report's Project Team). Each line is formatted independently and
 * the line structure - including blank lines - is preserved.
 */
export function formatPersonNameLines(value) {
  if (value === null || value === undefined) return value
  return String(value)
    .split(/\r\n|\r|\n/)
    .map((line) => formatPersonName(line))
    .join('\n')
}
