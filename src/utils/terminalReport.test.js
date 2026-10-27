import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  MAX_ABSTRACT_WORDS,
  TERMINAL_BODY_SECTIONS,
  TERMINAL_LISTS,
  TERMINAL_SECTIONS,
  abstractWordCount,
  appendixNumbers,
  appendixTableNumbers,
  bodyTableNumbers,
  emptySection,
  emptyTable,
  emptyTerminalReport,
  hasAnyTerminalContent,
  hasTerminalReportContent,
  referenceLine,
  sectionHasText,
  sectionSubsectionNumbers,
  summaryRowHasValue,
  terminalReportFormState,
  terminalReportPayload,
  terminalReportValidation,
  terminalSectionFilled,
  terminalSectionOf,
  textParagraphs,
} from './terminalReport.js'

const proposal = {
  programTitle: 'Fisheries Program',
  projectTitle: 'Fish Landing Survey',
  projectLeader: 'Juan Dela Cruz',
  projectLeaderSex: 'Male',
  college: 'College of Marine Sciences, MSU-Naawan',
  cooperatingAgencies: 'LGU Naawan',
  duration: '12',
  addressMunicipality: 'Naawan',
  addressProvince: 'Misamis Oriental',
  proponent: { email: 'juan.delacruz@msunaawan.edu.ph' },
}

const FIXED_TODAY = new Date('2026-10-26T00:00:00Z')

const filled = (overrides = {}) =>
  terminalReportFormState(
    {
      ...terminalReportFormState(null, proposal, { today: FIXED_TODAY }),
      ...overrides,
    },
    proposal,
    { today: FIXED_TODAY }
  )

describe('terminalReport - structure of the official format', () => {
  it('has every field of the report, with the repeated sections empty', () => {
    const report = emptyTerminalReport()
    assert.equal(report.projectTitle, '')
    assert.equal(report.summaryTotalBudget, '')
    assert.equal(report.abstractText, '')
    assert.deepEqual(report.tableOfContents, [])
    assert.deepEqual(report.literatureCited, [])
    assert.deepEqual(report.appendices, [])
    assert.deepEqual(report.appendixTables, [])
    for (const section of TERMINAL_BODY_SECTIONS) {
      if (section.kind !== 'text') continue
      assert.deepEqual(report[section.key], { text: '', subsections: [], tables: [] })
    }
  })

  it('numbers the body sections 1 to 10 in document order', () => {
    assert.deepEqual(
      TERMINAL_BODY_SECTIONS.map((section) => `${section.number}. ${section.title}`),
      [
        '1. INTRODUCTION',
        '2. REVIEW OF RELATED LITERATURE',
        '3. METHODOLOGY',
        '4. RESULTS AND DISCUSSION',
        '5. CONCLUSIONS AND RECOMMENDATIONS',
        '6. LITERATURE CITED',
        '7. APPENDIX',
        '8. PROBLEMS ENCOUNTERED',
        '9. OUTPUT',
        '10. UTILIZATION',
      ]
    )
  })

  it('keeps the RPIS-only sections of the form (graphs, acronyms, problems, output, utilization)', () => {
    const listKeys = TERMINAL_LISTS.map((list) => list.key)
    assert.ok(listKeys.includes('listOfGraphs'))
    assert.ok(listKeys.includes('listOfAcronyms'))
    const sectionKeys = TERMINAL_BODY_SECTIONS.map((section) => section.key)
    assert.ok(sectionKeys.includes('problems'))
    assert.ok(sectionKeys.includes('outputs'))
    assert.ok(sectionKeys.includes('utilization'))
  })

  it('edits every sheet of the document through the steps of the form', () => {
    const sheets = TERMINAL_SECTIONS.flatMap((step) => step.sheets)
    for (const key of [
      'cover',
      'summary',
      'acknowledgment',
      'abstract',
      'contents',
      'listOfTables',
      'listOfFigures',
      'listOfGraphs',
      'listOfAppendices',
      'listOfAcronyms',
      ...TERMINAL_BODY_SECTIONS.map((section) => section.key),
    ]) {
      assert.ok(sheets.includes(key), `${key} is reachable from the form`)
    }
  })
})

describe('terminalReport - prefill from the project record', () => {
  it('fills the cover page from the project RPIS already holds', () => {
    const state = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    assert.equal(state.programTitle, 'Fisheries Program')
    assert.equal(state.projectTitle, 'Fish Landing Survey')
    assert.equal(state.projectTeam, 'Juan Dela Cruz')
    assert.equal(state.collaborators, 'LGU Naawan')
    assert.equal(state.submissionMonthYear, 'OCTOBER 2026')
  })

  it('fills the summary sheet items of the official format', () => {
    const state = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    const byLabel = Object.fromEntries(state.summaryRows.map((row) => [row.label, row.detail]))

    assert.equal(byLabel['Program Title'], 'Fisheries Program')
    assert.equal(byLabel['Project Title'], 'Fish Landing Survey')
    assert.equal(byLabel['Project Leader/Gender'], 'Juan Dela Cruz / Male')
    assert.equal(byLabel['Agency'], 'College of Marine Sciences, MSU-Naawan')
    assert.equal(
      byLabel['Address/Email'],
      'Naawan, Misamis Oriental / juan.delacruz@msunaawan.edu.ph'
    )
    assert.equal(byLabel['Cooperating Agency'], 'LGU Naawan')
    // Summary items required by the RPIS reporting requirements.
    assert.equal(byLabel['Report Title'], 'Fish Landing Survey')
    assert.equal(byLabel['Project Duration'], '12')
  })

  it('never invents budget, location or result information', () => {
    const state = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    const byLabel = Object.fromEntries(state.summaryRows.map((row) => [row.label, row.detail]))
    assert.equal(byLabel['Base Station'], '')
    assert.equal(byLabel['Area Covered'], '')
    assert.equal(byLabel['Data Collection'], '')
    assert.equal(byLabel['Implementing Agencies'], '')
    assert.equal(state.summaryTotalBudget, '')
    assert.equal(state.summarySourceOfFunds, '')
    assert.equal(state.abstractText, '')
    assert.equal(state.keywords, '')
  })

  it('offers the subsection headings and tables the reporting requirements expect', () => {
    const state = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    assert.deepEqual(
      state.methodology.subsections.map((subsection) => subsection.heading),
      [
        'Procedures Involved',
        'Materials Used',
        'Sampling Procedures',
        'Statistical Analysis',
        'Scope and Limitations',
        'Time and Seasonality',
        'Place of Study',
      ]
    )
    assert.deepEqual(
      state.conclusions.subsections.map((subsection) => subsection.heading),
      ['Conclusions', 'Recommendations']
    )
    assert.deepEqual(
      state.problems.subsections.map((subsection) => subsection.heading),
      [
        'Technical problems encountered',
        'Administrative problems encountered',
        'Recommended solutions',
      ]
    )
    assert.deepEqual(state.outputs.tables[0].columns, [
      'Product Name',
      'Process',
      'Methods',
      'Technology',
    ])
    assert.deepEqual(state.utilization.tables[0].columns, [
      'Beneficiary',
      'Partner Industry',
      'Community',
      'Description of utilization',
    ])
    assert.equal(state.outputs.tables[0].caption, 'Outputs of the project')
  })

  it('keeps the saved content of a reopened draft', () => {
    const saved = {
      ...terminalReportFormState(null, proposal, { today: FIXED_TODAY }),
      projectTitle: 'Edited title',
      abstractText: 'Saved abstract.',
      summaryRows: [{ label: 'Program Title', detail: 'Saved program' }],
      resultsAndDiscussion: {
        text: '',
        subsections: [{ heading: 'Swine production', text: 'A 24.43% decline.' }],
        tables: [],
      },
    }

    const state = terminalReportFormState(saved, proposal, { today: FIXED_TODAY })
    assert.equal(state.projectTitle, 'Edited title', 'the saved value wins over the prefill')
    assert.equal(state.abstractText, 'Saved abstract.')
    assert.deepEqual(state.summaryRows, [{ label: 'Program Title', detail: 'Saved program' }])
    assert.equal(state.resultsAndDiscussion.subsections[0].heading, 'Swine production')
    assert.equal(state.resultsAndDiscussion.subsections[0].text, 'A 24.43% decline.')
  })
})


describe('terminalReport - save payload', () => {
  it('keeps the content the proponent entered and drops what was left empty', () => {
    const state = filled({
      abstractText: 'An abstract.',
      keywords: 'fish, landings',
      introduction: { text: 'Intro text.', subsections: [], tables: [] },
      resultsAndDiscussion: {
        text: '',
        subsections: [
          { heading: 'Swine production', text: 'A 24.43% decline.' },
          { heading: 'Not written yet', text: '' },
        ],
        tables: [
          {
            caption: 'Production and market indicators',
            note: '',
            columns: ['Indicator', '2020 Index'],
            rows: [
              ['Swine production', '75.57'],
              ['', ''],
            ],
          },
          emptyTable(['Empty', 'Table']),
        ],
      },
      literatureCited: [{ author: 'Dela Cruz, J.', year: '2020' }],
    })

    const payload = terminalReportPayload(state)
    assert.equal(payload.abstractText, 'An abstract.')
    assert.equal(payload.keywords, 'fish, landings')
    assert.equal(payload.introduction.text, 'Intro text.')
    assert.equal(payload.resultsAndDiscussion.subsections.length, 1, 'empty subsection dropped')
    assert.equal(payload.resultsAndDiscussion.tables.length, 1, 'table without cells dropped')
    assert.equal(payload.resultsAndDiscussion.tables[0].rows[0][1], '75.57')
    assert.equal(payload.literatureCited.length, 1)
    assert.equal(payload.methodology.subsections.length, 0, 'untouched starters are not stored')
    assert.equal(payload.outputs.tables.length, 0, 'untouched starter table is not stored')
  })

  it('prints only the summary items that carry a value', () => {
    const state = filled({ summaryTotalBudget: 'PHP 25,000.00' })
    assert.equal(summaryRowHasValue(state.summaryRows[6]), false, 'Base Station is empty')
    const payload = terminalReportPayload(state)
    assert.ok(payload.summaryRows.every((row) => row.detail.trim() !== ''))
    assert.equal(payload.summaryRows.length, 8)
  })

  it('survives a payload round trip', () => {
    const state = filled({
      abstractText: 'An abstract.',
      introduction: { text: 'Intro.', subsections: [], tables: [] },
      resultsAndDiscussion: {
        text: '',
        subsections: [{ heading: 'Landings', text: '24 species.' }],
        tables: [
          {
            caption: 'Catch',
            note: 'Source: records.',
            columns: ['Site', 'Total'],
            rows: [['Poblacion', '12']],
          },
        ],
      },
      listOfAcronyms: [{ label: 'CPUE', detail: 'Catch per unit effort' }],
    })

    const payload = terminalReportPayload(state)
    const reopened = terminalReportFormState(payload, proposal, { today: FIXED_TODAY })
    assert.equal(reopened.abstractText, 'An abstract.')
    assert.deepEqual(reopened.resultsAndDiscussion.tables[0].columns, ['Site', 'Total'])
    assert.deepEqual(reopened.resultsAndDiscussion.tables[0].rows, [['Poblacion', '12']])
    assert.equal(reopened.resultsAndDiscussion.subsections[0].heading, 'Landings')
    assert.deepEqual(reopened.listOfAcronyms, [
      { label: 'CPUE', detail: 'Catch per unit effort' },
    ])
  })
})

describe('terminalReport - abstract word limit', () => {
  it('counts words the same way the backend does', () => {
    assert.equal(abstractWordCount(''), 0)
    assert.equal(abstractWordCount('   '), 0)
    assert.equal(abstractWordCount('one'), 1)
    assert.equal(abstractWordCount('  one   two \n three  '), 3)
  })

  it('blocks only a submission, never a draft', () => {
    const report = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    report.projectTitle = 'Fish Landing Survey'
    report.abstractText = Array.from({ length: MAX_ABSTRACT_WORDS + 1 }, () => 'word').join(' ')
    report.introduction.text = 'Intro.'

    assert.deepEqual(terminalReportValidation(report).errors, {}, 'a draft is not blocked')

    const { errors } = terminalReportValidation(report, { forSubmission: true })
    assert.equal(abstractWordCount(report.abstractText), MAX_ABSTRACT_WORDS + 1)
    assert.match(errors.abstractText, /350 words/)
    assert.match(errors.abstractText, /351/)
  })

  it('accepts exactly the limit', () => {
    const report = terminalReportFormState(null, proposal, { today: FIXED_TODAY })
    report.projectTitle = 'Fish Landing Survey'
    report.abstractText = Array.from({ length: MAX_ABSTRACT_WORDS }, () => 'word').join(' ')
    report.introduction.text = 'Intro.'
    report.resultsAndDiscussion.text = 'Results.'

    const { errors } = terminalReportValidation(report, { forSubmission: true })
    assert.equal(errors.abstractText, undefined)
  })
})


describe('terminalReport - submission rules', () => {
  const valid = () => {
    const report = emptyTerminalReport()
    report.projectTitle = 'Fish Landing Survey'
    report.abstractText = 'An abstract.'
    report.introduction.text = 'An introduction.'
    report.resultsAndDiscussion.text = 'The results.'
    return report
  }

  it('requires the report title, the abstract and the two core sections', () => {
    const report = valid()
    report.projectTitle = ''
    report.introduction.text = '   '
    report.resultsAndDiscussion.text = ''

    const { errors, summary } = terminalReportValidation(report, { forSubmission: true })
    assert.equal(errors.projectTitle, 'Project Title is required.')
    assert.equal(errors.introduction, 'Section 1 (INTRODUCTION) is required.')
    assert.equal(errors.resultsAndDiscussion, 'Section 4 (RESULTS AND DISCUSSION) is required.')
    assert.equal(errors.abstractText, undefined)
    assert.match(summary, /required sections/)
  })

  it('accepts a section written as numbered subsections, as the official format does', () => {
    const report = valid()
    report.introduction = emptySection()
    report.introduction.subsections = [{ heading: 'Background', text: 'Fish landings.' }]
    report.resultsAndDiscussion = {
      text: '',
      subsections: [{ heading: 'Landings', text: '24 species were recorded.' }],
      tables: [],
    }

    const { errors } = terminalReportValidation(report, { forSubmission: true })
    assert.deepEqual(errors, {})
    assert.equal(sectionHasText(report.resultsAndDiscussion), true)
    assert.equal(hasTerminalReportContent(report), true)
  })

  it('leaves the optional sections optional', () => {
    const { errors } = terminalReportValidation(valid(), { forSubmission: true })
    assert.deepEqual(errors, {})
    assert.equal(hasTerminalReportContent(valid()), true)
    assert.equal(hasTerminalReportContent(emptyTerminalReport()), false)
    assert.equal(hasTerminalReportContent(null), false)
  })

  it('distinguishes any entered content from the core content', () => {
    const report = emptyTerminalReport()
    assert.equal(hasAnyTerminalContent(report), false)
    assert.equal(hasAnyTerminalContent(null), false)

    report.acknowledgment = 'Thanks.'
    assert.equal(hasAnyTerminalContent(report), true, 'a draft with anything entered is shown')
    assert.equal(hasTerminalReportContent(report), false)

    report.acknowledgment = ''
    report.summaryRows = [{ label: 'Base Station', detail: '' }]
    assert.equal(hasAnyTerminalContent(report), false, 'an item without a value is not content')
    report.summaryRows = [{ label: 'Base Station', detail: 'Naawan' }]
    assert.equal(hasAnyTerminalContent(report), true)
  })
})

describe('terminalReport - form steps', () => {
  it('knows which step owns a field or a section', () => {
    assert.equal(terminalSectionOf('projectTitle'), 'cover')
    assert.equal(terminalSectionOf('abstractText'), 'preliminary')
    assert.equal(terminalSectionOf('keywords'), 'preliminary')
    assert.equal(terminalSectionOf('summaryTotalBudget'), 'summary')
    assert.equal(terminalSectionOf('tableOfContents'), 'contents')
    assert.equal(terminalSectionOf('methodology'), 'methodology')
    assert.equal(terminalSectionOf('unknownField'), null)
  })

  it('reports a step as filled once it holds something', () => {
    const report = emptyTerminalReport()
    assert.equal(terminalSectionFilled(report, 'cover'), false)
    assert.equal(terminalSectionFilled(report, 'summary'), false)
    assert.equal(terminalSectionFilled(report, 'methodology'), false)

    report.programTitle = 'Fisheries Program'
    assert.equal(terminalSectionFilled(report, 'cover'), true)

    report.summaryRows = [{ label: 'Program Title', detail: 'Fisheries Program' }]
    assert.equal(terminalSectionFilled(report, 'summary'), true)

    report.methodology.subsections = [{ heading: 'Study design', text: 'Monthly sampling.' }]
    assert.equal(terminalSectionFilled(report, 'methodology'), true)

    report.resultsAndDiscussion.tables = [{ caption: '', note: '', columns: ['A'], rows: [['1']] }]
    assert.equal(terminalSectionFilled(report, 'resultsAndDiscussion'), true)
    assert.equal(terminalSectionFilled(null, 'cover'), false)
  })
})


describe('terminalReport - numbering of the document', () => {
  it('numbers the tables of the body in document order, skipping the empty ones', () => {
    const report = emptyTerminalReport()
    report.methodology.tables = [
      { caption: 'Sites', note: '', columns: ['Site'], rows: [['Poblacion']] },
    ]
    report.resultsAndDiscussion.tables = [
      { caption: 'Indicators', note: '', columns: ['Indicator'], rows: [['Production']] },
      { caption: 'Not filled yet', note: '', columns: ['A'], rows: [['']] },
    ]

    const numbers = bodyTableNumbers(report)
    assert.deepEqual(numbers.methodology.map((entry) => entry.number), ['1'])
    assert.deepEqual(numbers.resultsAndDiscussion.map((entry) => entry.number), ['2'])
    assert.equal(numbers.resultsAndDiscussion[0].table.caption, 'Indicators')
    assert.equal(numbers.introduction, undefined)
  })

  it('numbers the appendix tables A1, A2 ... and the appendices 1, 2 ...', () => {
    const report = emptyTerminalReport()
    report.appendices = [
      { title: 'Production-Index Computation', text: 'Notes.' },
      { title: '', text: '' },
    ]
    report.appendixTables = [
      { caption: 'Computation', note: '', columns: ['Indicator'], rows: [['Production']] },
    ]

    assert.deepEqual(appendixTableNumbers(report).map((entry) => entry.number), ['A1'])
    assert.deepEqual(appendixNumbers(report).map((entry) => entry.number), [1])
  })

  it('numbers the subsections of a section, skipping the ones without text', () => {
    const section = {
      text: '',
      subsections: [
        { heading: 'Production Effects', text: 'Text.' },
        { heading: 'Suggested but empty', text: '' },
        { heading: 'Market Effects', text: 'More text.' },
      ],
      tables: [],
    }
    assert.deepEqual(
      sectionSubsectionNumbers(section, 2).map(
        (entry) => `${entry.number} ${entry.subsection.heading}`
      ),
      ['2.1 Production Effects', '2.2 Market Effects']
    )
  })
})

describe('terminalReport - literature cited and paragraphs', () => {
  it('builds a reference line only from the entered fields', () => {
    assert.equal(
      referenceLine({
        author: 'Dela Cruz, J.',
        year: '2020',
        title: 'Landing survey methods',
        edition: '2nd',
        place: 'Naawan',
        journal: '',
        pages: '12-18',
      }),
      'Dela Cruz, J. (2020). Landing survey methods. 2nd. Naawan. 12-18.'
    )
  })

  it('joins a journal article with its page number', () => {
    assert.equal(
      referenceLine({
        author: 'Woonwong, Y.',
        year: '2020',
        title: 'The future of the pig industry',
        journal: 'Animal Frontiers',
        pages: '30-37',
      }),
      'Woonwong, Y. (2020). The future of the pig industry. Animal Frontiers, 30-37.'
    )
  })

  it('never invents missing bibliographic information', () => {
    assert.equal(referenceLine({ author: 'Reyes, A.' }), 'Reyes, A.')
    assert.equal(referenceLine({}), '')
    assert.equal(referenceLine(null), '')
  })

  it('splits a narrative field into the paragraphs the document prints', () => {
    assert.deepEqual(textParagraphs('One.\n\nTwo.\n\n\nThree.'), ['One.', 'Two.', 'Three.'])
    assert.deepEqual(textParagraphs('Line one\nline two'), ['Line one\nline two'])
    assert.deepEqual(textParagraphs('   '), [])
    assert.deepEqual(textParagraphs(null), [])
  })
})

