/**
 * apaReferences.test.js
 * ---------------------------------------------------------------------------
 * Covers the APA 7th edition reference builder of section 23 (Literature Cited):
 * every supported source type, the author/date/edition/DOI rules, the italics of the
 * rich-text rendering, the validation messages, and the entry helpers that keep a
 * list of references (plain text + structured entries) in sync with the proposal.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ADD_REFERENCE_LABEL,
  CONFERENCE_STATUS,
  MANUAL_REFERENCE_TYPE,
  REFERENCE_INSTRUCTION,
  REFERENCE_TEXT_SEPARATOR,
  REFERENCE_TYPES,
  THESIS_AVAILABILITY,
  THESIS_TYPES,
  authorList,
  dateLabel,
  doiUrl,
  editionLabel,
  editorList,
  editorName,
  emptyReferenceFields,
  entriesToJson,
  entriesToText,
  entryHtml,
  entryText,
  entryTypeLabel,
  formatReference,
  isDuplicateReference,
  makeGeneratedEntry,
  makeManualEntry,
  parseReferenceEntries,
  personName,
  referenceType,
  supportedReferenceTypes,
  textToEntries,
  validateReference,
  yearLabel,
} from './apaReferences.js'

/** Formats a type with the given values, asserting that nothing is missing. */
const build = (type, fields) => {
  const result = formatReference(type, fields)
  assert.equal(result.text !== '', true, `${type} produced no reference: ${result.missing}`)
  return result
}

test('the ten source types of the requirement are supported', () => {
  assert.deepEqual(supportedReferenceTypes(), [
    'journal',
    'website',
    'book',
    'chapter',
    'report',
    'conference',
    'thesis',
    'government',
    'newspaper',
    'other',
  ])
  assert.equal(REFERENCE_TYPES.length, 10)
  assert.equal(referenceType('journal').label, 'Journal Article')
  assert.equal(referenceType('government').label, 'Government or Organizational Publication')
  assert.equal(referenceType('nope'), undefined)
  // Every type exposes its own fields, and only those.
  assert.deepEqual(emptyReferenceFields('book'), {
    authors: '',
    year: '',
    edition: '',
    title: '',
    publisher: '',
    doi: '',
    url: '',
  })
  assert.equal(emptyReferenceFields('journal').pages, '')
  assert.equal('editors' in emptyReferenceFields('journal'), false)
})

test('section 23 shows the required instruction and the Add Reference action', () => {
  assert.equal(
    REFERENCE_INSTRUCTION,
    'Provide the references cited in your proposal using APA 7th edition format. You may generate a reference using the reference builder or enter an already formatted APA reference manually.',
  )
  assert.equal(ADD_REFERENCE_LABEL, '+ Add Reference')
})

// ===========================================================================
// Scenario 1 - journal article with a DOI
// ===========================================================================

test('a journal article is formatted with the journal title and volume in italics', () => {
  const { text, html } = build('journal', {
    authors: 'Dela Cruz, J. P.\nSantos, M. L.',
    year: '2020',
    title: 'Assessing the coral reef cover of Naawan',
    journal: 'Philippine Journal of Science',
    volume: '149',
    issue: '3',
    pages: '45–67',
    doi: '10.1000/xyz123',
  })

  assert.equal(
    text,
    'Dela Cruz, J. P., & Santos, M. L. (2020). Assessing the coral reef cover of Naawan. Philippine Journal of Science, 149(3), 45–67. https://doi.org/10.1000/xyz123',
  )
  assert.equal(
    html,
    'Dela Cruz, J. P., &amp; Santos, M. L. (2020). Assessing the coral reef cover of Naawan. <i>Philippine Journal of Science</i>, <i>149</i>(3), 45–67. https://doi.org/10.1000/xyz123',
  )
  // No Markdown asterisks anywhere - the italics are real markup.
  assert.equal(text.includes('*'), false)
  assert.equal(html.includes('*'), false)
})

test('the optional journal elements are left out instead of invented', () => {
  const withoutIssueOrPages = build('journal', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: 'Coral cover',
    journal: 'Philippine Journal of Science',
    volume: '149',
  })
  assert.equal(
    withoutIssueOrPages.text,
    'Dela Cruz, J. P. (2020). Coral cover. Philippine Journal of Science, 149.',
  )

  const withUrl = build('journal', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: 'Coral cover',
    journal: 'Philippine Journal of Science',
    volume: '149',
    issue: '3',
    url: 'https://example.org/article',
  })
  assert.equal(
    withUrl.text,
    'Dela Cruz, J. P. (2020). Coral cover. Philippine Journal of Science, 149(3). https://example.org/article',
  )
})

test('a DOI is shown as a resolvable URL whatever the proponent pasted', () => {
  assert.equal(doiUrl('10.1000/xyz123'), 'https://doi.org/10.1000/xyz123')
  assert.equal(doiUrl('doi:10.1000/xyz123'), 'https://doi.org/10.1000/xyz123')
  assert.equal(doiUrl('https://doi.org/10.1000/xyz123'), 'https://doi.org/10.1000/xyz123')
  assert.equal(doiUrl('http://dx.doi.org/10.1000/xyz123'), 'https://doi.org/10.1000/xyz123')
  assert.equal(doiUrl(''), '')
  // The DOI wins over a plain URL (APA asks for one of the two).
  const { text } = build('journal', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: 'Coral cover',
    journal: 'PJS',
    volume: '149',
    doi: '10.1000/xyz123',
    url: 'https://example.org/other',
  })
  assert.equal(text.endsWith('https://doi.org/10.1000/xyz123'), true)
})

// ===========================================================================
// Scenarios 2 and 3 - webpages, with and without a date
// ===========================================================================

test('a webpage with an author and a full date is formatted, site name dropped when identical', () => {
  const { text, html } = build('website', {
    authors: 'World Health Organization',
    year: '2021',
    month: 'March',
    day: '4',
    title: 'Coronavirus disease (COVID-19) pandemic',
    site: 'World Health Organization',
    url: 'https://www.who.int/emergencies/diseases/novel-coronavirus-2019',
  })

  assert.equal(
    text,
    'World Health Organization. (2021, March 4). Coronavirus disease (COVID-19) pandemic. https://www.who.int/emergencies/diseases/novel-coronavirus-2019',
  )
  assert.equal(
    html,
    'World Health Organization. (2021, March 4). <i>Coronavirus disease (COVID-19) pandemic</i>. https://www.who.int/emergencies/diseases/novel-coronavirus-2019',
  )
})

test('a webpage with a different site name keeps it', () => {
  const { text } = build('website', {
    authors: 'Dela Cruz, J. P.',
    year: '2022',
    month: 'January',
    day: '9',
    title: 'Seagrass mapping in Naawan',
    site: 'MSU at Naawan News',
    url: 'https://example.org/news/seagrass',
  })
  assert.equal(
    text,
    'Dela Cruz, J. P. (2022, January 9). Seagrass mapping in Naawan. MSU at Naawan News. https://example.org/news/seagrass',
  )
})

test('a webpage without a publication date uses n.d. and needs no date field', () => {
  const { text, html } = build('website', {
    authors: '',
    year: '',
    title: 'About the Research Project Information System',
    site: 'MSU at Naawan',
    url: 'https://example.org/about',
  })

  assert.equal(
    text,
    'About the Research Project Information System. (n.d.). MSU at Naawan. https://example.org/about',
  )
  // No author: the title takes the author position and stays italic.
  assert.equal(html.startsWith('<i>About the Research Project Information System</i>.'), true)
  assert.equal(yearLabel({}), 'n.d.')
  assert.equal(dateLabel({}), 'n.d.')
})

test('a retrieval date is added only when it is provided', () => {
  const { text } = build('website', {
    authors: '',
    year: '2024',
    title: 'Community wiki',
    site: 'MSU Wiki',
    url: 'https://example.org/wiki',
    retrievalDate: 'March 4, 2026',
  })
  assert.equal(
    text,
    'Community wiki. (2024). MSU Wiki. Retrieved March 4, 2026, from https://example.org/wiki',
  )
})

// ===========================================================================
// Scenario 4 - book
// ===========================================================================

test('a book is formatted with the title in italics and the edition ordinal', () => {
  const { text, html } = build('book', {
    authors: 'Dela Cruz, J. P.',
    year: '2019',
    title: 'Marine biology of Mindanao',
    edition: '2',
    publisher: 'MSU Press',
    url: 'https://example.org/book',
  })

  assert.equal(
    text,
    'Dela Cruz, J. P. (2019). Marine biology of Mindanao (2nd ed.). MSU Press. https://example.org/book',
  )
  assert.equal(
    html,
    'Dela Cruz, J. P. (2019). <i>Marine biology of Mindanao</i> (2nd ed.). MSU Press. https://example.org/book',
  )
})

test('the edition is written as an ordinal and an already written one is kept', () => {
  assert.equal(editionLabel('1'), '1st ed.')
  assert.equal(editionLabel('2'), '2nd ed.')
  assert.equal(editionLabel('3'), '3rd ed.')
  assert.equal(editionLabel('4'), '4th ed.')
  assert.equal(editionLabel('11'), '11th ed.')
  assert.equal(editionLabel('21'), '21st ed.')
  assert.equal(editionLabel('2nd ed.'), '2nd ed.')
  assert.equal(editionLabel('Revised'), 'Revised ed.')
  assert.equal(editionLabel(''), '')
})

// ===========================================================================
// Chapter, report, conference, thesis, government, newspaper, other
// ===========================================================================

test('a chapter in an edited book puts the editors initials-first with (Eds.)', () => {
  const { text, html } = build('chapter', {
    authors: 'Dela Cruz, J. P.',
    year: '2019',
    title: 'Seagrass ecosystems of Misamis Oriental',
    editors: 'Santos, M. L.\nReyes, A. B.',
    bookTitle: 'Coastal resources of the Philippines',
    pages: '45–67',
    publisher: 'MSU Press',
  })

  assert.equal(
    text,
    'Dela Cruz, J. P. (2019). Seagrass ecosystems of Misamis Oriental. In M. L. Santos & A. B. Reyes (Eds.), Coastal resources of the Philippines (pp. 45–67). MSU Press.',
  )
  assert.equal(
    html,
    'Dela Cruz, J. P. (2019). Seagrass ecosystems of Misamis Oriental. In M. L. Santos &amp; A. B. Reyes (Eds.), <i>Coastal resources of the Philippines</i> (pp. 45–67). MSU Press.',
  )

  // One editor is singular, and a missing page range is simply left out.
  const single = build('chapter', {
    authors: 'Dela Cruz, J. P.',
    year: '2019',
    title: 'Seagrass ecosystems',
    editors: 'Santos, M. L.',
    bookTitle: 'Coastal resources',
    publisher: 'MSU Press',
  })
  assert.equal(
    single.text,
    'Dela Cruz, J. P. (2019). Seagrass ecosystems. In M. L. Santos (Ed.), Coastal resources. MSU Press.',
  )
})

test('a research report includes the report number and omits an absent publisher', () => {
  const { text, html } = build('report', {
    authors:
      'Philippine Council for Agriculture, Aquatic and Natural Resources Research and Development',
    year: '2020',
    title: 'Annual research report',
    number: '123',
    url: 'https://example.org/report',
  })

  assert.equal(
    text,
    'Philippine Council for Agriculture, Aquatic and Natural Resources Research and Development. (2020). Annual research report (Report No. 123). https://example.org/report',
  )
  assert.equal(html.includes('<i>Annual research report</i>'), true)
})

test('a published conference paper and an unpublished presentation are formatted differently', () => {
  const published = build('conference', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    status: CONFERENCE_STATUS[0],
    title: 'Coastal resilience in Naawan',
    proceedings: 'Proceedings of the 5th National Marine Conference',
    editors: 'Santos, M. L.',
    pages: '45–67',
    publisher: 'MSU Press',
    doi: '10.1000/xyz123',
  })
  assert.equal(
    published.text,
    'Dela Cruz, J. P. (2020). Coastal resilience in Naawan. In M. L. Santos (Ed.), Proceedings of the 5th National Marine Conference (pp. 45–67). MSU Press. https://doi.org/10.1000/xyz123',
  )

  const presentation = build('conference', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    status: CONFERENCE_STATUS[1],
    title: 'Coastal resilience in Naawan',
    proceedings: '5th National Marine Conference',
    url: 'https://example.org/paper',
  })
  assert.equal(
    presentation.text,
    'Dela Cruz, J. P. (2020). Coastal resilience in Naawan [Paper presentation]. 5th National Marine Conference. https://example.org/paper',
  )
  assert.equal(presentation.html.includes('<i>Coastal resilience in Naawan</i>'), true)
})

test('a thesis is described by its type and institution, and unpublished ones differ', () => {
  const inRepository = build('thesis', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    thesisType: THESIS_TYPES[1],
    title: 'Seagrass cover and fish abundance in Naawan',
    institution: 'Mindanao State University at Naawan',
    availability: THESIS_AVAILABILITY[0],
    repository: 'UPLB Knowledge Commons',
    url: 'https://example.org/thesis',
  })
  assert.equal(
    inRepository.text,
    "Dela Cruz, J. P. (2020). Seagrass cover and fish abundance in Naawan [Master's thesis, Mindanao State University at Naawan]. UPLB Knowledge Commons. https://example.org/thesis",
  )

  const unpublished = build('thesis', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    thesisType: THESIS_TYPES[2],
    title: 'Seagrass cover and fish abundance in Naawan',
    institution: 'Mindanao State University at Naawan',
    availability: THESIS_AVAILABILITY[2],
  })
  assert.equal(
    unpublished.text,
    'Dela Cruz, J. P. (2020). Seagrass cover and fish abundance in Naawan [Unpublished doctoral dissertation]. Mindanao State University at Naawan.',
  )
})

test('a government publication does not repeat the agency as its publisher', () => {
  const repeated = build('government', {
    authors: 'Department of Science and Technology',
    year: '2022',
    title: 'Science and technology report',
    publisher: 'Department of Science and Technology',
    url: 'https://example.org/dost',
  })
  assert.equal(
    repeated.text,
    'Department of Science and Technology. (2022). Science and technology report. https://example.org/dost',
  )

  const withNumber = build('government', {
    authors: 'Department of Science and Technology',
    year: '2022',
    month: 'June',
    day: '1',
    title: 'Science and technology report',
    number: '12',
  })
  assert.equal(
    withNumber.text,
    'Department of Science and Technology. (2022, June 1). Science and technology report (Report No. 12).',
  )
})

test('a newspaper article is formatted for print and for online, with or without a byline', () => {
  const online = build('newspaper', {
    authors: 'Reyes, A. B.',
    year: '2023',
    month: 'July',
    day: '12',
    title: 'Naawan researchers win national award',
    newspaper: 'SunStar Cebu',
    url: 'https://example.org/news/award',
  })
  assert.equal(
    online.text,
    'Reyes, A. B. (2023, July 12). Naawan researchers win national award. SunStar Cebu. https://example.org/news/award',
  )

  const print = build('newspaper', {
    authors: '',
    year: '2023',
    month: 'July',
    day: '12',
    title: 'Naawan researchers win national award',
    newspaper: 'SunStar Cebu',
    pages: 'A12',
  })
  assert.equal(
    print.text,
    'Naawan researchers win national award. (2023, July 12). SunStar Cebu, A12.',
  )
  assert.equal(print.html.includes('<i>SunStar Cebu</i>'), true)
})

test('an "Other Source" reference is best effort and never invents a missing part', () => {
  const withAuthor = build('other', {
    authors: 'Dela Cruz, J. P.',
    year: '2021',
    title: 'Community-based mangrove rehabilitation',
    source: 'Field notes, MSU at Naawan',
    details: 'Unpublished field notes',
    url: 'https://example.org/notes',
  })
  assert.equal(
    withAuthor.text,
    'Dela Cruz, J. P. (2021). Community-based mangrove rehabilitation. Field notes, MSU at Naawan. Unpublished field notes. https://example.org/notes',
  )

  const withoutAuthor = build('other', {
    year: '2021',
    title: 'Community-based mangrove rehabilitation',
    source: 'Field notes',
  })
  assert.equal(withoutAuthor.text, 'Community-based mangrove rehabilitation. (2021). Field notes.')
})

// ===========================================================================
// Names, validation, escaping
// ===========================================================================

test('authors are joined with commas and an ampersand, and the 21+ rule is applied', () => {
  assert.equal(personName('Dela Cruz, J. P.'), 'Dela Cruz, J. P.')
  assert.equal(personName('Dela Cruz, J.P.'), 'Dela Cruz, J. P.')
  assert.equal(personName('World Health Organization'), 'World Health Organization')
  // An organization that contains a comma is never turned into initials.
  assert.equal(
    personName(
      'Philippine Council for Agriculture, Aquatic and Natural Resources Research and Development',
    ),
    'Philippine Council for Agriculture, Aquatic and Natural Resources Research and Development',
  )
  assert.equal(editorName('Dela Cruz, J. P.'), 'J. P. Dela Cruz')
  assert.equal(editorName('World Health Organization'), 'World Health Organization')

  assert.equal(authorList('Dela Cruz, J. P.'), 'Dela Cruz, J. P.')
  assert.equal(authorList('Dela Cruz, J. P.\nSantos, M. L.'), 'Dela Cruz, J. P., & Santos, M. L.')
  assert.equal(
    authorList('Dela Cruz, J. P.\nSantos, M. L.\nReyes, A. B.'),
    'Dela Cruz, J. P., Santos, M. L., & Reyes, A. B.',
  )
  // Editors take no comma before the ampersand.
  assert.equal(editorList('Santos, M. L.\nReyes, A. B.'), 'M. L. Santos & A. B. Reyes')

  const many = Array.from({ length: 21 }, (unused, index) => `Author${index + 1}, A. A.`)
  const joined = authorList(many.join('\n'))
  assert.equal(joined.startsWith('Author1, A. A.,'), true)
  assert.equal(joined.includes('Author19, A. A.'), true)
  assert.equal(joined.includes('Author20, A. A.'), false)
  assert.equal(joined.endsWith(', ... Author21, A. A.'), true)
})

test('the validation only demands what the selected type requires', () => {
  const journal = validateReference('journal', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: '',
    journal: 'PJS',
    volume: '149',
  })
  assert.equal(journal.valid, false)
  assert.equal(journal.errors.title, 'Article Title is required.')
  assert.equal('issue' in journal.errors, false)
  assert.equal('pages' in journal.errors, false)
  assert.equal('doi' in journal.errors, false)

  assert.equal(validateReference('journal', {}).errors.authors, 'Author or Authors is required.')
  assert.equal(validateReference('website', {}).errors.url, 'URL is required.')
  assert.equal(
    validateReference('journal', {
      authors: 'A',
      year: '20',
      title: 'T',
      journal: 'J',
      volume: '1',
    }).errors.year,
    'Enter the year as four digits (e.g. 2020).',
  )
  assert.equal(
    validateReference('website', { title: 'T', url: 'u', month: 'March' }).errors.month,
    'Add the publication year to use a month.',
  )
  assert.equal(
    validateReference('newspaper', { year: '2020', title: 'T', newspaper: 'N' }).errors.url,
    'Add the page number for a print article, or the URL for an online one.',
  )
  assert.equal(validateReference('unknown-type', {}).valid, false)
  // A complete type is valid.
  assert.equal(
    validateReference('book', { authors: 'A', year: '2020', title: 'T', publisher: 'P' }).valid,
    true,
  )
})

test('an incomplete reference produces no output at all', () => {
  const result = formatReference('journal', { title: 'Only a title' })
  assert.equal(result.text, '')
  assert.equal(result.html, '')
  assert.deepEqual(result.missing.includes('authors'), true)
  assert.deepEqual(result.missing.includes('year'), true)
})

test("titles are capitalised without lowercasing the proponent's own words", () => {
  const { text } = build('journal', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: 'coral reefs of the Philippine Sea: a baseline study',
    journal: 'Philippine Journal of Science',
    volume: '149',
  })
  assert.equal(
    text,
    'Dela Cruz, J. P. (2020). Coral reefs of the Philippine Sea: A baseline study. Philippine Journal of Science, 149.',
  )
})

test('values are HTML-escaped, so nothing typed can inject markup', () => {
  const { text, html } = build('book', {
    authors: 'Dela Cruz, J. P.',
    year: '2020',
    title: '<script>alert(1)</script> & more',
    publisher: 'MSU <Press>',
  })
  assert.equal(html.includes('<script>'), false)
  assert.equal(html.includes('&lt;script&gt;'), true)
  assert.equal(html.includes('&amp;'), true)
  // The plain text keeps what the proponent typed.
  assert.equal(text.includes('<script>'), true)
})

// ===========================================================================
// Scenarios 6, 7, 8 - manual entry, several references, editing
// ===========================================================================

test('a manually entered reference is kept exactly as the proponent wrote it', () => {
  const pasted =
    'Dela Cruz, J. P. (2020). Coral cover. Philippine Journal of Science, 149(3), 45–67.'
  const entry = makeManualEntry(`  ${pasted}  `)

  assert.equal(entry.type, MANUAL_REFERENCE_TYPE)
  assert.equal(entry.text, pasted)
  assert.equal(entryText(entry), pasted)
  assert.equal(entryTypeLabel(entry), 'Entered manually')
  // Nothing is rewritten: the manual text is escaped as-is, never reformatted.
  assert.equal(entryHtml(entry), pasted.replace('&', '&amp;'))
  assert.equal(makeManualEntry('   ').text, '')
})

test('generated and manual references live in the same list', () => {
  const generated = makeGeneratedEntry('book', {
    authors: 'Dela Cruz, J. P.',
    year: '2019',
    title: 'Marine biology of Mindanao',
    publisher: 'MSU Press',
  })
  const manual = makeManualEntry('Santos, M. L. (2018). Reef fish. MSU Press.')

  assert.equal(entryTypeLabel(generated), 'Book')
  assert.equal(entryTypeLabel(manual), 'Entered manually')
  assert.equal(
    entriesToText([generated, manual]),
    'Dela Cruz, J. P. (2019). Marine biology of Mindanao. MSU Press.\n\nSantos, M. L. (2018). Reef fish. MSU Press.',
  )
  assert.equal(REFERENCE_TEXT_SEPARATOR, '\n\n')
  assert.equal(entriesToText([]), '')
})

test('the entries survive a save and a reload through the stored JSON', () => {
  const entries = [
    makeGeneratedEntry('journal', {
      authors: 'Dela Cruz, J. P.',
      year: '2020',
      title: 'Coral cover',
      journal: 'Philippine Journal of Science',
      volume: '149',
    }),
    makeManualEntry('Santos, M. L. (2018). Reef fish. MSU Press.'),
  ]

  const json = entriesToJson(entries)
  const text = entriesToText(entries)
  const restored = parseReferenceEntries(json, text)

  assert.equal(restored.length, 2)
  assert.equal(restored[0].type, 'journal')
  assert.equal(restored[0].fields.volume, '149')
  assert.equal(entryText(restored[0]), entryText(entries[0]))
  // The italics survive too: the entry is re-rendered from its values.
  assert.equal(entryHtml(restored[0]).includes('<i>Philippine Journal of Science</i>'), true)
  assert.equal(restored[1].type, MANUAL_REFERENCE_TYPE)
  assert.equal(entryText(restored[1]), entries[1].text)

  assert.equal(entriesToJson([]), '')
})

test('a proposal without structured entries is read from its plain text', () => {
  // Legacy content: one reference per line.
  const perLine = parseReferenceEntries('', 'First reference.\nSecond reference.')
  assert.equal(perLine.length, 2)
  assert.equal(perLine[0].type, MANUAL_REFERENCE_TYPE)
  assert.equal(entryText(perLine[1]), 'Second reference.')

  // References separated by a blank line stay whole, even across two lines.
  const blocks = textToEntries('First reference\ncontinued.\n\nSecond reference.')
  assert.equal(blocks.length, 2)
  assert.equal(entryText(blocks[0]), 'First reference\ncontinued.')

  // Nothing at all.
  assert.deepEqual(parseReferenceEntries('', ''), [])
  assert.deepEqual(parseReferenceEntries('not json', ''), [])
  // Unreadable JSON falls back to the plain text instead of losing it.
  assert.equal(parseReferenceEntries('{oops', 'Kept reference.').length, 1)
})

test('the same reference is recognised as a duplicate', () => {
  const entries = [makeManualEntry('Dela Cruz, J. P. (2020). Coral cover.')]

  assert.equal(isDuplicateReference(entries, 'Dela Cruz, J. P. (2020). Coral cover.'), true)
  assert.equal(isDuplicateReference(entries, 'dela cruz, j. p. (2020). coral cover'), true)
  assert.equal(isDuplicateReference(entries, 'Dela Cruz, J. P. (2020). Coral cover'), true)
  assert.equal(isDuplicateReference(entries, 'Santos, M. L. (2020). Coral cover.'), false)
  assert.equal(isDuplicateReference(entries, ''), false)
  assert.equal(isDuplicateReference([], 'Anything.'), false)
})
