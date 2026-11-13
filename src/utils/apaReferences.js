/**
 * apaReferences.js
 * ---------------------------------------------------------------------------
 * Deterministic APA 7th edition reference formatting for section 23 (Literature
 * Cited) of the Create Proposal form.
 *
 * Every supported source type has its own formatter and its own field list, so the
 * builder only asks for (and only validates) the information that type needs. No
 * external service is involved: the same input always produces the same reference.
 *
 * Two renderings are produced for every reference:
 *   text  the plain-text APA reference (no markup) - this is what is stored in the
 *         proposal's references_text column, so every existing screen (reviewer
 *         views, exports, the printable official form) keeps showing clean text.
 *   html  the same reference with <i> around the italic elements, built from
 *         HTML-escaped values only - the proponent's own list and the Review &
 *         Submit page render it, so italics are real italics and never literal
 *         asterisks.
 *
 * Nothing is invented: a reference is only produced from the values the proponent
 * typed, optional elements that are unavailable are left out, and a source without
 * a date is reported as "n.d.".
 *
 * Run the rules with: npm test
 */

/** How entries are separated in the stored plain text (blank line between them). */
export const REFERENCE_TEXT_SEPARATOR = '\n\n'

/** Instruction shown under the "23. Literature Cited" heading. */
export const REFERENCE_INSTRUCTION =
  'Provide the references cited in your proposal using APA 7th edition format. You may generate a reference using the reference builder or enter an already formatted APA reference manually.'

/** Label of the button that opens the reference builder. */
export const ADD_REFERENCE_LABEL = '+ Add Reference'

/** Month names, used by the date selects and the formatted dates. */
export const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
]

/** Thesis / dissertation kinds (APA puts this in the bracketed description). */
export const THESIS_TYPES = ["Bachelor's thesis", "Master's thesis", 'Doctoral dissertation']

/** Where the thesis is available (APA distinguishes these three cases). */
export const THESIS_AVAILABILITY = ['In a repository or database', 'Published', 'Unpublished']

/** Conference source kinds (a published paper differs from a presentation). */
export const CONFERENCE_STATUS = ['Published in proceedings', 'Presentation (unpublished)']

/**
 * Field kinds: 'text' (default), 'textarea', 'year', 'select', 'day'.
 * `half: true` lets the modal place two fields on one row on wide screens,
 * `full: true` always uses the whole row.
 */
const text = (key, label, extra = {}) => ({ key, label, kind: 'text', ...extra })
const year = (key, label, extra = {}) => ({ key, label, kind: 'year', ...extra })
const month = (key, label, extra = {}) => ({
  key,
  label,
  kind: 'select',
  options: MONTHS,
  half: true,
  ...extra,
})
const day = (key, label, extra = {}) => ({
  key,
  label,
  kind: 'day',
  placeholder: 'e.g. 4',
  half: true,
  ...extra,
})

const AUTHORS_FIELD = (label, extra = {}) =>
  text('authors', label, {
    kind: 'textarea',
    full: true,
    placeholder: 'Dela Cruz, J. P.\nSantos, M. L.',
    hint: 'One author per line as Surname, Initials. An organization needs no comma.',
    ...extra,
  })

/**
 * The ten source types of the builder, each with the fields that type actually
 * needs. `required` drives validation; every other field is optional and simply
 * left out of the reference when it is empty.
 */
export const REFERENCE_TYPES = [
  {
    key: 'journal',
    label: 'Journal Article',
    fields: [
      AUTHORS_FIELD('Author or Authors', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      text('title', 'Article Title', { required: true, full: true }),
      text('journal', 'Journal Title', {
        required: true,
        full: true,
        placeholder: 'e.g. Philippine Journal of Science',
      }),
      text('volume', 'Volume Number', { required: true, half: true, placeholder: 'e.g. 12' }),
      text('issue', 'Issue Number (optional)', { half: true, placeholder: 'e.g. 3' }),
      text('pages', 'Page Range or Article Number (optional)', {
        half: true,
        placeholder: 'e.g. 45–67',
      }),
      text('doi', 'DOI (optional)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (optional)', { full: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'website',
    label: 'Website or Webpage',
    fields: [
      AUTHORS_FIELD('Author or Organization', {
        hint: 'Leave empty when the page has no author: the title then starts the reference.',
      }),
      year('year', 'Publication Year', {
        half: true,
        placeholder: 'e.g. 2020 (leave empty if none)',
      }),
      month('month', 'Publication Month'),
      day('day', 'Publication Day'),
      text('title', 'Webpage Title', { required: true, full: true }),
      text('site', 'Website Name (optional)', {
        full: true,
        hint: 'Leave empty when it is the same as the author.',
      }),
      text('url', 'URL', { required: true, full: true, placeholder: 'https://…' }),
      text('retrievalDate', 'Retrieval Date (only when the content changes over time)', {
        full: true,
        placeholder: 'e.g. March 4, 2026',
      }),
    ],
  },
  {
    key: 'book',
    label: 'Book',
    fields: [
      AUTHORS_FIELD('Author or Authors', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      text('edition', 'Edition (optional)', { half: true, placeholder: 'e.g. 2' }),
      text('title', 'Book Title', { required: true, full: true }),
      text('publisher', 'Publisher', { required: true, full: true }),
      text('doi', 'DOI (when applicable)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when applicable)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'chapter',
    label: 'Chapter in an Edited Book',
    fields: [
      AUTHORS_FIELD('Chapter Author or Authors', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      text('edition', 'Book Edition (optional)', { half: true, placeholder: 'e.g. 2' }),
      text('title', 'Chapter Title', { required: true, full: true }),
      text('editors', 'Editor or Editors', {
        kind: 'textarea',
        required: true,
        full: true,
        placeholder: 'Santos, M. L.',
        hint: 'One editor per line as Surname, Initials. "(Eds.)" is added automatically.',
      }),
      text('bookTitle', 'Book Title', { required: true, full: true }),
      text('pages', 'Page Range (optional)', { half: true, placeholder: 'e.g. 45–67' }),
      text('publisher', 'Publisher', { required: true, half: true }),
      text('doi', 'DOI (when applicable)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when applicable)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'report',
    label: 'Research Report',
    fields: [
      AUTHORS_FIELD('Author or Organization', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      text('number', 'Report Number (optional)', { half: true, placeholder: 'e.g. 123' }),
      text('title', 'Report Title', { required: true, full: true }),
      text('publisher', 'Publisher (optional when appropriate)', { full: true }),
      text('doi', 'DOI (when applicable)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when applicable)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'conference',
    label: 'Conference Paper or Conference Proceedings',
    fields: [
      AUTHORS_FIELD('Author or Authors', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      {
        key: 'status',
        label: 'Source Kind',
        kind: 'select',
        options: CONFERENCE_STATUS,
        required: true,
        half: true,
      },
      text('title', 'Paper Title', { required: true, full: true }),
      text('proceedings', 'Conference or Proceedings Title', { full: true }),
      text('editors', 'Editors (optional)', {
        kind: 'textarea',
        full: true,
        placeholder: 'Santos, M. L.',
        hint: 'One editor per line as Surname, Initials.',
      }),
      text('pages', 'Page Range (optional)', { half: true, placeholder: 'e.g. 45–67' }),
      text('publisher', 'Publisher (when applicable)', { half: true }),
      text('doi', 'DOI (when applicable)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when applicable)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'thesis',
    label: 'Thesis or Dissertation',
    fields: [
      AUTHORS_FIELD('Author', { required: true }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      {
        key: 'thesisType',
        label: 'Thesis Type',
        kind: 'select',
        options: THESIS_TYPES,
        required: true,
        half: true,
      },
      text('title', 'Thesis or Dissertation Title', { required: true, full: true }),
      text('institution', 'Institution', {
        required: true,
        full: true,
        placeholder: 'e.g. Mindanao State University at Naawan',
      }),
      {
        key: 'availability',
        label: 'Availability',
        kind: 'select',
        options: THESIS_AVAILABILITY,
        required: true,
        half: true,
      },
      text('repository', 'Repository or Database (when applicable)', { half: true }),
      text('url', 'URL (when available)', { full: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'government',
    label: 'Government or Organizational Publication',
    fields: [
      AUTHORS_FIELD('Authoring Agency or Organization', {
        required: true,
        placeholder: 'Department of Science and Technology',
      }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      month('month', 'Publication Month (when available)'),
      day('day', 'Publication Day (when available)'),
      text('title', 'Document Title', { required: true, full: true }),
      text('number', 'Report or Document Number (optional)', { half: true }),
      text('publisher', 'Publisher (when applicable)', {
        half: true,
        hint: 'Leave empty when it is the same as the authoring agency.',
      }),
      text('doi', 'DOI (when available)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when available)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'newspaper',
    label: 'Newspaper Article',
    fields: [
      AUTHORS_FIELD('Author or Authors', {
        hint: 'Leave empty when the article has no byline: the title then starts the reference.',
      }),
      year('year', 'Publication Year', { required: true, half: true, placeholder: 'e.g. 2020' }),
      month('month', 'Publication Month'),
      day('day', 'Publication Day'),
      text('title', 'Article Title', { required: true, full: true }),
      text('newspaper', 'Newspaper Name', { required: true, full: true }),
      text('pages', 'Page Number or Page Range (optional for print articles)', {
        half: true,
        placeholder: 'e.g. A12',
      }),
      text('url', 'URL (for online articles)', { half: true, placeholder: 'https://…' }),
    ],
  },
  {
    key: 'other',
    label: 'Other Source',
    fields: [
      AUTHORS_FIELD('Author or Organization'),
      year('year', 'Publication Year', { half: true, placeholder: 'e.g. 2020' }),
      month('month', 'Publication Month'),
      day('day', 'Publication Day'),
      text('title', 'Title', { required: true, full: true }),
      text('source', 'Source or Publisher', { full: true }),
      text('doi', 'DOI (when available)', { half: true, placeholder: 'e.g. 10.1000/xyz123' }),
      text('url', 'URL (when available)', { half: true, placeholder: 'https://…' }),
      text('details', 'Additional Source Details (when necessary)', { full: true }),
    ],
  },
]

/** The type definition of a key, or undefined when the key is unknown. */
export function referenceType(key) {
  return REFERENCE_TYPES.find((type) => type.key === key)
}

/** Every field of a type, or [] for an unknown type. */
export function referenceFields(typeKey) {
  return referenceType(typeKey)?.fields || []
}

/** A blank field set for a type (every field present, so the form never has holes). */
export function emptyReferenceFields(typeKey) {
  const fields = {}
  for (const field of referenceFields(typeKey)) fields[field.key] = ''
  return fields
}

/** The formatter keys, in the order of REFERENCE_TYPES. */
export function supportedReferenceTypes() {
  return REFERENCE_TYPES.map((type) => type.key)
}

// ===========================================================================
// Small value helpers
// ===========================================================================

/** Collapses whitespace and trims. */
const clean = (value) =>
  String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()

/** The non-empty lines of a multi-value field (authors / editors). */
const valueLines = (value) =>
  String(value ?? '')
    .split(/[\n;]+/)
    .map(clean)
    .filter(Boolean)

/** HTML-escapes a value, so only our own markup can ever reach the page. */
export function escapeReferenceHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** A trailing period is never doubled. */
const withoutTrailingPeriod = (value) => clean(value).replace(/\.+$/, '')

/**
 * APA sentence case, applied conservatively: the first word is capitalised, and so
 * is the first word after a colon or question mark. Words are never lowercased,
 * because a proper noun cannot be detected deterministically - silently turning
 * "Philippine Sea" into "philippine sea" would corrupt the reference.
 */
function sentenceCase(value) {
  const title = withoutTrailingPeriod(value)
  if (!title) return ''
  return title
    .replace(/^([a-z])/, (match, first) => first.toUpperCase())
    .replace(/([:?]\s+)([a-z])/g, (match, separator, first) => separator + first.toUpperCase())
}

/** "2" -> "2nd ed."; "2nd ed." stays as typed. */
export function editionLabel(value) {
  const raw = clean(value)
  if (!raw) return ''
  if (/ed\./i.test(raw)) return raw
  const number = Number(raw)
  if (Number.isInteger(number) && number > 0) {
    const suffixes = ['th', 'st', 'nd', 'rd']
    const rest = number % 100
    const suffix = suffixes[(rest - 20) % 10] || suffixes[rest] || suffixes[0]
    return `${number}${suffix} ed.`
  }
  return `${raw} ed.`
}

/** A DOI is always shown as a resolvable URL, whatever the proponent pasted. */
export function doiUrl(value) {
  const doi = clean(value)
    .replace(/^(https?:\/\/)?(dx\.)?doi\.org\//i, '')
    .replace(/^doi:\s*/i, '')
  return doi ? `https://doi.org/${doi}` : ''
}

/** The DOI when there is one, otherwise the plain URL (APA: one of the two). */
const doiOrUrl = (fields) => doiUrl(fields.doi) || clean(fields.url)

/** The year, or "n.d." when the source carries no date. */
export function yearLabel(fields) {
  return clean(fields?.year) || 'n.d.'
}

/** "(2020)" / "(2020, March)" / "(2020, March 4)" / "(n.d.)". */
export function dateLabel(fields) {
  const year = clean(fields?.year)
  const month = clean(fields?.month)
  const day = clean(fields?.day)
  if (!year) return 'n.d.'
  if (month && day) return `${year}, ${month} ${day}`
  if (month) return `${year}, ${month}`
  return year
}

/**
 * True when the text after the first comma is written as initials ("J. P.", "M. L. R.",
 * optionally with a suffix like Jr.). This is the documented input format for a
 * person; anything else (an organization that happens to contain a comma, or a full
 * given name) is kept exactly as the proponent typed it instead of being guessed at.
 */
function looksLikeInitials(rest) {
  const tokens = String(rest ?? '')
    .split(/[\s.]+/)
    .filter(Boolean)
  if (!tokens.length || tokens.length > 4) return false
  return tokens.every(
    (token) => /^[A-Za-z]{1,3}$/.test(token) || /^(jr|sr|ii|iii|iv)$/i.test(token),
  )
}

/** The initials of "J. P." as "J. P." (used by both person and editor names). */
const initialsOf = (rest) =>
  String(rest ?? '')
    .split(/[\s.]+/)
    .filter(Boolean)
    .map((part) => `${part[0].toUpperCase()}.`)
    .join(' ')

/**
 * A person's name in APA reference-list form: "Dela Cruz, J. P.". A value without a
 * comma, or with a part after the comma that is not written as initials, is an
 * organization ("Philippine Council for Agriculture, Aquatic and Natural Resources
 * Research and Development") and is kept as typed.
 */
export function personName(value) {
  const name = clean(value)
  if (!name || !name.includes(',')) return name
  const [surname, ...rest] = name.split(',')
  const initials = initialsOf(rest.join(','))
  if (!looksLikeInitials(rest.join(',')) || !initials) return name
  return `${surname.trim()}, ${initials}`
}

/** An editor's name, printed initials-first: "Dela Cruz, J. P." -> "J. P. Dela Cruz". */
export function editorName(value) {
  const name = clean(value)
  if (!name || !name.includes(',')) return name
  const [surname, ...rest] = name.split(',')
  const initials = initialsOf(rest.join(','))
  if (!looksLikeInitials(rest.join(',')) || !initials) return name
  return `${initials} ${surname.trim()}`
}

/**
 * A name list with APA punctuation. `commaBeforeAmpersand` is true for authors
 * ("One, A., & Two, B.") and false for the editors inside "In ... (Eds.)," which APA
 * writes without a comma before the ampersand ("A. One & B. Two").
 */
export function nameList(value, mapper = personName, { commaBeforeAmpersand = true } = {}) {
  const people = valueLines(value).map(mapper).filter(Boolean)
  if (!people.length) return ''
  if (people.length === 1) return people[0]
  if (people.length <= 20) {
    const separator = commaBeforeAmpersand ? ', & ' : ' & '
    return `${people.slice(0, -1).join(', ')}${separator}${people[people.length - 1]}`
  }
  return `${people.slice(0, 19).join(', ')}, ... ${people[people.length - 1]}`
}

/** The author list of a reference. */
export function authorList(value) {
  return nameList(value, personName, { commaBeforeAmpersand: true })
}

/** The editor list inside "In ... (Eds.)," - no comma before the ampersand. */
export function editorList(value) {
  return nameList(value, editorName, { commaBeforeAmpersand: false })
}

/** True when two names refer to the same organization (used to drop a duplicate publisher). */
export function sameOrganization(first, second) {
  const normalize = (value) => clean(value).toLowerCase().replace(/\.+$/, '')
  const a = normalize(first)
  const b = normalize(second)
  return Boolean(a) && a === b
}

// ===========================================================================
// Reference assembly
// ===========================================================================

/** A plain piece of the reference. */
const plain = (value) => ({ value: String(value ?? ''), italicize: false })
/** A piece that is italicised in APA (journal title, volume, book title, ...). */
const italic = (value) => ({ value: String(value ?? ''), italicize: true })

/** Ends a sentence with exactly one period. */
const sentence = (value) => {
  const text = clean(value)
  if (!text) return ''
  return /[.?!]$/.test(text) ? text : `${text}.`
}

/** The italicised title of a standalone work, without its closing period. */
const titleText = (value) => withoutTrailingPeriod(sentenceCase(value))

/** Collapses the whitespace inside a piece without trimming its separator space. */
const collapse = (value) => String(value ?? '').replace(/\s+/g, ' ')

/**
 * Joins the pieces into the plain text and the HTML rendering. Both come from the
 * same pieces, so they can never drift apart, and only HTML-escaped values ever
 * reach the page.
 */
function renderReference(pieces) {
  const kept = pieces.filter((piece) => clean(piece.value) !== '')
  const text = kept.map((piece) => piece.value).join('')
  const html = kept
    .map((piece) => {
      const value = escapeReferenceHtml(collapse(piece.value))
      return piece.italicize ? `<i>${value}</i>` : value
    })
    .join('')
  return { text: tidy(text), html: tidy(html) }
}

/** Removes doubled spaces and any space that ended up in front of punctuation. */
const tidy = (value) =>
  String(value ?? '')
    .replace(/\s+/g, ' ')
    .replace(/\s+([.,;:)])/g, '$1')
    // A name like "Dela Cruz, J. P." already ends with a period: never double it
    // ("J. P.. (2020)" -> "J. P. (2020)"). The "..." of the 21+ author rule has
    // three dots, so it is never touched.
    .replace(/\.\.(?=\s|$)/g, '.')
    .trim()

/** "Report No. 123" is not repeated when the proponent already typed it. */
const reportNumber = (value) => {
  const number = clean(value)
  if (!number) return ''
  return /no\./i.test(number) ? number : `Report No. ${number}`
}

/** "In J. P. Dela Cruz (Ed.), " / "In A. A. One & B. B. Two (Eds.), ". */
const editorPart = (value) => {
  const editors = editorList(value)
  if (!editors) return ''
  return `In ${editors} (${valueLines(value).length > 1 ? 'Eds.' : 'Ed.'}), `
}

/**
 * One formatter per source type, in the order of REFERENCE_TYPES. Each returns the
 * APA 7th edition reference for the values it was given; nothing is invented and an
 * unavailable optional element is simply left out.
 */
const FORMATTERS = {
  journal: (f) => {
    const volume = clean(f.volume)
    const issue = clean(f.issue)
    const pages = clean(f.pages)
    const link = doiOrUrl(f)
    return renderReference([
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      plain(`${sentence(sentenceCase(f.title))} `),
      italic(f.journal),
      volume ? plain(', ') : plain(''),
      volume ? italic(volume) : plain(''),
      issue ? plain(`(${issue})`) : plain(''),
      pages ? plain(`, ${pages}`) : plain(''),
      plain('.'),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  website: (f) => {
    const authors = authorList(f.authors)
    const site = clean(f.site)
    const url = clean(f.url)
    const retrieval = clean(f.retrievalDate)
    const date = `(${dateLabel(f)})`
    const pieces = []
    if (authors) {
      pieces.push(
        plain(`${authors}. `),
        plain(`${date}. `),
        italic(titleText(f.title)),
        plain('. '),
      )
    } else {
      // No author: the title takes the author position (APA 7).
      pieces.push(italic(titleText(f.title)), plain(`. ${date}. `))
    }
    if (site && !sameOrganization(authors, site)) pieces.push(plain(`${site}. `))
    pieces.push(plain(retrieval ? `Retrieved ${retrieval}, from ${url}` : url))
    return renderReference(pieces)
  },

  book: (f) => {
    const edition = editionLabel(f.edition)
    const link = doiOrUrl(f)
    return renderReference([
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      italic(titleText(f.title)),
      edition ? plain(` (${edition})`) : plain(''),
      plain('. '),
      plain(`${clean(f.publisher)}.`),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  chapter: (f) => {
    const edition = editionLabel(f.edition)
    const pages = clean(f.pages)
    const details = [edition, pages ? `pp. ${pages}` : ''].filter(Boolean).join(', ')
    const link = doiOrUrl(f)
    return renderReference([
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      plain(`${sentence(sentenceCase(f.title))} `),
      plain(editorPart(f.editors)),
      italic(titleText(f.bookTitle)),
      details ? plain(` (${details})`) : plain(''),
      plain('. '),
      plain(`${clean(f.publisher)}.`),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  report: (f) => {
    const number = reportNumber(f.number)
    const publisher = clean(f.publisher)
    const link = doiOrUrl(f)
    return renderReference([
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      italic(titleText(f.title)),
      number ? plain(` (${number})`) : plain(''),
      plain('. '),
      publisher ? plain(`${publisher}.`) : plain(''),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  conference: (f) => {
    const published = clean(f.status) !== CONFERENCE_STATUS[1]
    const pages = clean(f.pages)
    const publisher = clean(f.publisher)
    const proceedings = clean(f.proceedings)
    const link = doiOrUrl(f)
    if (!published) {
      // An unpublished presentation: APA uses the bracketed description instead of
      // the proceedings information.
      return renderReference([
        plain(`${authorList(f.authors)}. `),
        plain(`(${yearLabel(f)}). `),
        italic(titleText(f.title)),
        plain(' [Paper presentation]. '),
        proceedings ? plain(`${proceedings}.`) : plain(''),
        link ? plain(` ${link}`) : plain(''),
      ])
    }
    return renderReference([
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      plain(`${sentence(sentenceCase(f.title))} `),
      plain(editorPart(f.editors) || (proceedings ? 'In ' : '')),
      proceedings ? italic(titleText(proceedings)) : plain(''),
      pages ? plain(` (pp. ${pages})`) : plain(''),
      plain('. '),
      publisher ? plain(`${publisher}.`) : plain(''),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  thesis: (f) => {
    const availability = clean(f.availability)
    const type = clean(f.thesisType)
    const institution = clean(f.institution)
    const repository = clean(f.repository)
    const url = clean(f.url)
    const unpublished = availability === THESIS_AVAILABILITY[2]
    const description = unpublished
      ? `Unpublished ${type.toLowerCase()}`
      : `${type}, ${institution}`
    const pieces = [
      plain(`${authorList(f.authors)}. `),
      plain(`(${yearLabel(f)}). `),
      italic(titleText(f.title)),
      plain(` [${description}]. `),
    ]
    if (unpublished) {
      pieces.push(plain(`${institution}.`))
    } else {
      if (repository) pieces.push(plain(`${repository}.`))
      if (url) pieces.push(plain(`${repository ? ' ' : ''}${url}`))
    }
    return renderReference(pieces)
  },

  government: (f) => {
    const authors = authorList(f.authors)
    const publisher = clean(f.publisher)
    const number = reportNumber(f.number)
    const link = doiOrUrl(f)
    return renderReference([
      plain(`${authors}. `),
      plain(`(${dateLabel(f)}). `),
      italic(titleText(f.title)),
      number ? plain(` (${number})`) : plain(''),
      plain('. '),
      // APA omits the publisher when it is the same as the authoring agency.
      publisher && !sameOrganization(authors, publisher) ? plain(`${publisher}.`) : plain(''),
      link ? plain(` ${link}`) : plain(''),
    ])
  },

  newspaper: (f) => {
    const authors = authorList(f.authors)
    const pages = clean(f.pages)
    const url = clean(f.url)
    const title = sentence(sentenceCase(f.title))
    const pieces = []
    if (authors) {
      pieces.push(plain(`${authors}. `), plain(`(${dateLabel(f)}). `), plain(`${title} `))
    } else {
      // No byline: the title comes first and the date follows it (APA 7).
      pieces.push(plain(`${title} `), plain(`(${dateLabel(f)}). `))
    }
    pieces.push(italic(clean(f.newspaper)))
    if (pages) pieces.push(plain(`, ${pages}`))
    pieces.push(plain('.'))
    if (url) pieces.push(plain(` ${url}`))
    return renderReference(pieces)
  },

  other: (f) => {
    const authors = authorList(f.authors)
    const source = clean(f.source)
    const details = clean(f.details)
    const link = doiOrUrl(f)
    const pieces = []
    if (authors) {
      pieces.push(
        plain(`${authors}. `),
        plain(`(${dateLabel(f)}). `),
        italic(titleText(f.title)),
        plain('. '),
      )
    } else {
      // No author: the title takes the author position (APA 7).
      pieces.push(italic(titleText(f.title)), plain(`. (${dateLabel(f)}). `))
    }
    pieces.push(
      source ? plain(`${source}.`) : plain(''),
      details ? plain(` ${sentence(details)}`) : plain(''),
      link ? plain(` ${link}`) : plain(''),
    )
    return renderReference(pieces)
  },
}

// ===========================================================================
// Validation and generation
// ===========================================================================

/** The type of a manually pasted reference. */
export const MANUAL_REFERENCE_TYPE = 'manual'

/**
 * Validates only what the selected type requires: the required fields, a four-digit
 * year, a real day, and - for a newspaper article - either the print page or the
 * online URL. Optional information (DOI, issue, pages, ...) is never demanded.
 *
 * Returns `{ valid, errors }` with one message per field key, ready to be shown
 * beside that field.
 */
export function validateReference(typeKey, fields) {
  const type = referenceType(typeKey)
  if (!type) return { valid: false, errors: { type: 'Choose a reference type.' } }

  const values = fields || {}
  const errors = {}
  for (const field of type.fields) {
    if (field.required && !clean(values[field.key])) {
      const label = field.label.replace(/\s*\(.*\)\s*$/, '')
      errors[field.key] = `${label} is required.`
    }
  }

  const year = clean(values.year)
  if (year && !/^\d{4}$/.test(year)) errors.year = 'Enter the year as four digits (e.g. 2020).'
  if (clean(values.month) && !year) errors.month = 'Add the publication year to use a month.'
  const day = clean(values.day)
  if (day && !/^([1-9]|[12]\d|3[01])$/.test(day)) errors.day = 'Enter a day between 1 and 31.'
  if (typeKey === 'newspaper' && !clean(values.pages) && !clean(values.url)) {
    errors.url = 'Add the page number for a print article, or the URL for an online one.'
  }

  return { valid: Object.keys(errors).length === 0, errors }
}

/**
 * The APA 7th edition reference for a type and its field values.
 *
 * `{ text, html, missing }` - `missing` lists the field keys that must be filled in
 * first, and both renderings are empty while anything is missing (a reference is
 * never produced from invented information).
 */
export function formatReference(typeKey, fields) {
  const { valid, errors } = validateReference(typeKey, fields)
  if (!valid) return { text: '', html: '', missing: Object.keys(errors) }
  const formatter = FORMATTERS[typeKey]
  if (!formatter) return { text: '', html: '', missing: ['type'] }
  return { ...formatter(fields || {}), missing: [] }
}

// ===========================================================================
// Reference entries (the list shown under Item 23)
// ===========================================================================

let referenceCounter = 0

/** A stable id for a list entry (used as the Vue key and by Edit / Remove). */
function nextReferenceId() {
  referenceCounter += 1
  return `ref-${referenceCounter}`
}

/** A generated reference as a list entry: the type, its values and the plain text. */
export function makeGeneratedEntry(typeKey, fields) {
  const { text } = formatReference(typeKey, fields)
  return { id: nextReferenceId(), type: typeKey, fields: { ...(fields || {}) }, text }
}

/** A manually pasted reference as a list entry (kept exactly as the proponent wrote it). */
export function makeManualEntry(value) {
  const text = String(value ?? '').trim()
  return { id: nextReferenceId(), type: MANUAL_REFERENCE_TYPE, fields: { text }, text }
}

/** "Journal Article" / "Entered manually" - the badge shown on a list entry. */
export function entryTypeLabel(entry) {
  if (!entry || entry.type === MANUAL_REFERENCE_TYPE) return 'Entered manually'
  return referenceType(entry.type)?.label || 'Reference'
}

/** The plain text of an entry (never empty for a stored entry). */
export function entryText(entry) {
  if (!entry) return ''
  if (String(entry.text ?? '').trim()) return String(entry.text).trim()
  if (entry.type === MANUAL_REFERENCE_TYPE) return ''
  return formatReference(entry.type, entry.fields).text
}

/**
 * The HTML of an entry: italics for a generated reference (rebuilt from its values,
 * so nothing but escaped text can reach the page) and the proponent's own text for a
 * manual one.
 */
export function entryHtml(entry) {
  if (!entry) return ''
  if (entry.type === MANUAL_REFERENCE_TYPE) {
    return escapeReferenceHtml(entry.text).replace(/\n/g, '<br>')
  }
  const { html } = formatReference(entry.type, entry.fields)
  if (html) return html
  return escapeReferenceHtml(entryText(entry))
}

/** The plain-text rendering of every entry: one reference per block, blank line apart. */
export function entriesToText(entries) {
  return (Array.isArray(entries) ? entries : [])
    .map((entry) => entryText(entry))
    .filter(Boolean)
    .join(REFERENCE_TEXT_SEPARATOR)
}

/** The structured entries as the JSON string stored with the proposal ('' when empty). */
export function entriesToJson(entries) {
  const list = (Array.isArray(entries) ? entries : [])
    .map((entry) => ({
      type: entry.type || MANUAL_REFERENCE_TYPE,
      fields: { ...(entry.fields || {}) },
      text: entryText(entry),
    }))
    .filter((entry) => entry.text)
  return list.length ? JSON.stringify(list) : ''
}

/**
 * The entries of a Literature Cited text that was not written with the builder (an
 * older proposal, or text typed before this feature): a blank line separates
 * references, and a text without blank lines is read one reference per line.
 */
export function textToEntries(value) {
  const raw = String(value ?? '').trim()
  if (!raw) return []
  const blocks = raw
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter(Boolean)
  const parts =
    blocks.length > 1
      ? blocks
      : raw
          .split(/\n+/)
          .map((line) => line.trim())
          .filter(Boolean)
  return parts.map((part) => makeManualEntry(part))
}

/**
 * The entries of a proposal: the structured ones when the proposal carries them,
 * otherwise the ones derived from the stored plain text, so an existing proposal
 * record is never lost.
 */
export function parseReferenceEntries(json, text) {
  const raw = String(json ?? '').trim()
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        const entries = parsed
          .filter((entry) => entry && typeof entry === 'object')
          .map((entry) => {
            const type = typeof entry.type === 'string' ? entry.type : MANUAL_REFERENCE_TYPE
            const fields =
              entry.fields && typeof entry.fields === 'object' ? { ...entry.fields } : {}
            const value = typeof entry.text === 'string' ? entry.text : ''
            if (type === MANUAL_REFERENCE_TYPE) return makeManualEntry(value)
            return { id: nextReferenceId(), type, fields, text: value }
          })
          .filter((entry) => entryText(entry))
        if (entries.length) return entries
      }
    } catch (error) {
      // A stored value that is not readable JSON falls back to the plain text below.
    }
  }
  return textToEntries(text)
}

/** A comparison key for duplicate detection: case, spacing and a trailing period ignored. */
export function normalizeReferenceText(value) {
  return clean(value)
    .toLowerCase()
    .replace(/[.\s]+$/, '')
}

/** True when the same reference is already in the list. */
export function isDuplicateReference(entries, value) {
  const key = normalizeReferenceText(value)
  if (!key) return false
  return (Array.isArray(entries) ? entries : []).some(
    (entry) => normalizeReferenceText(entryText(entry)) === key,
  )
}
