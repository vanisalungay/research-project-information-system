/**
 * gadChecklist.js
 * ---------------------------------------------------------------------------
 * Source of truth for Section 19 - Gender and Development (GAD) Score.
 *
 * The section is the official "GAD CHECKLIST 2: FOR THE PROJECT IDENTIFICATION AND DESIGN
 * STAGES". Every criterion, assessment item, wording, option and score below is reproduced
 * from that document verbatim; nothing is added, reworded or dropped. The module holds the
 * content, the scoring rules, the interpretation bands and the state/payload/validation
 * helpers, so the interactive form, the reviewer view and the printable document all read the
 * same data and always agree. `utils/gadChecklist.test.js` guards the content and the scoring.
 */

/** The title printed above the checklist. */
export const GAD_CHECKLIST_TITLE = 'GAD CHECKLIST 2: FOR THE PROJECT IDENTIFICATION AND DESIGN STAGES'

/** Instruction shown under the section heading. */
export const GAD_SECTION_INSTRUCTION =
  'Please accomplish the following Gender and Development (GAD) Checklist. Read each ' +
  'assessment item carefully and select the appropriate response. The system will ' +
  'automatically calculate your score based on your answers. Use the comments field to ' +
  'explain your responses and identify relevant gender issues.'

/** Instruction shown under the checklist title. */
export const GAD_CHECKLIST_INSTRUCTION = "Note: Put an 'X' mark on the appropriate box."

/** The three mutually exclusive response options (the official 2a / 2b / 2c columns). */
export const GAD_RESPONSE_OPTIONS = [
  { key: 'NO', label: 'No', sub: '2a' },
  { key: 'PARTLY', label: 'Partly', sub: '2b' },
  { key: 'YES', label: 'Yes', sub: '2c' },
]

/**
 * The four scoring scales the official checklist assigns to its items. The score is chosen by
 * the item's own scale, never by position, because the items do not share one scale.
 */
export const GAD_SCALES = {
  HALF: { NO: 0, PARTLY: 0.5, YES: 1.0 },
  FULL: { NO: 0, PARTLY: 1.0, YES: 2.0 },
  THIRD: { NO: 0, PARTLY: 0.33, YES: 0.67 },
  THIRD_66: { NO: 0, PARTLY: 0.33, YES: 0.66 },
}

/** The official interpretation of the total GAD score, in order. */
export const GAD_INTERPRETATIONS = [
  {
    min: 0,
    max: 3.99,
    range: '0-3.9',
    label: 'GAD is invisible in the project (proposal is returned).',
  },
  {
    min: 4,
    max: 7.99,
    range: '4.0-7.9',
    label:
      'Proposed project has promising GAD prospects (proposal earns a "conditional pass," ' +
      'pending identification of gender issues and strategies and activities to address these ' +
      'and inclusion of the collection of sex-disaggregated data in the monitoring and ' +
      'evaluation plan).',
    short: 'Proposed project has promising GAD prospects',
  },
  {
    min: 8,
    max: 14.99,
    range: '8.0-14.9',
    label: 'Proposed project is gender-sensitive (proposal passes the GAD test).',
    short: 'Proposed project is gender-sensitive',
  },
  {
    min: 15,
    max: 20,
    range: '15.0-20.0',
    label: 'Proposed project is gender-responsive (proponent is commended).',
    short: 'Proposed project is gender-responsive',
  },
]

/** The theoretical maximum total of the checklist. */
export const GAD_MAX_SCORE = 20

/**
 * The official document states a maximum of 2 points for every criterion. Criterion 10's
 * three items are 0.67 each (2.01 when all are "Yes") - an official rounding artifact - so a
 * criterion subtotal is bounded by this maximum and the total never exceeds 20.
 */
export const GAD_CRITERION_MAX = 2

/** The four column headings of the checklist table. */
export const GAD_COLUMNS = [
  'Element and Items/Question',
  'Done?',
  'Score for an Item/Element',
  'Comments/Gender Issues Identified',
]

/**
 * The 10 criteria of the checklist, in document order, with every assessment item, its exact
 * question, its scoring scale and (where the official form prints one) its alternative or
 * conditional question. `alt` is the "OR, ..." alternative question; `conditional` is the
 * "IF ..." question that stays inside the same item and is never scored separately.
 */
export const GAD_CRITERIA = [
  {
    number: '1.0',
    title: 'Involvement of women and men',
    note: 'Maximum score: 2; for each item, 1.',
    items: [
      {
        key: '1.1',
        question:
          'Participation of women and men in beneficiary groups in the identification of the problem.',
        scale: 'HALF',
      },
      {
        key: '1.2',
        question: 'Participation of women and men in beneficiary groups in project design.',
        scale: 'HALF',
      },
    ],
  },
  {
    number: '2.0',
    title: 'Collection of sex-disaggregated data and gender-related information',
    items: [
      {
        key: '2.0',
        question: 'Collection of sex-disaggregated data and gender-related information.',
        scale: 'FULL',
      },
    ],
  },
  {
    number: '3.0',
    title: 'Conduct of gender analysis and identification of gender issues',
    note: 'Maximum score: 2; for each item, 1.',
    items: [
      {
        key: '3.1',
        question:
          'Analysis of gender gaps and inequalities related to gender roles, perspectives and needs, or access to and control of resources.',
        scale: 'HALF',
      },
      {
        key: '3.2',
        question:
          "Analysis of constraints and opportunities related to women's and men's participation in the project.",
        scale: 'HALF',
      },
    ],
  },
  {
    number: '4.0',
    title: 'Gender equality goals, outcomes, and outputs',
    items: [
      {
        key: '4.0',
        question:
          'Does the project have clearly stated gender equality goals, objectives, outcomes or outputs?',
        scale: 'FULL',
      },
    ],
  },
  {
    number: '5.0',
    title: 'Matching of strategies with gender issues',
    items: [
      {
        key: '5.0',
        question:
          'Do the strategies and activities match the gender issues and gender equality goals identified?',
        scale: 'FULL',
      },
    ],
  },
  {
    number: '6.0',
    title: 'Gender analysis of the likely impacts of the project',
    note: 'Maximum score: 2; for each item, 0.67.',
    items: [
      {
        key: '6.1',
        question: 'Are women and girl children among the direct or indirect beneficiaries?',
        scale: 'THIRD',
      },
      {
        key: '6.2',
        question:
          "Has the project considered its long-term impact on women's socioeconomic status and empowerment?",
        scale: 'THIRD',
      },
      {
        key: '6.3',
        question:
          "Has the project included strategies for avoiding or minimizing negative impacts on women's status and welfare?",
        scale: 'THIRD_66',
      },
    ],
  },
  {
    number: '7.0',
    title: 'Monitoring targets and indicators',
    items: [
      {
        key: '7.0',
        question:
          'Does the project include gender equality targets and indicators to measure gender equality outputs and outcomes?',
        scale: 'FULL',
      },
    ],
  },
  {
    number: '8.0',
    title: 'Sex-disaggregated database requirements',
    items: [
      {
        key: '8.0',
        question: 'Does the project M&E system require the collection of sex-disaggregated data?',
        scale: 'FULL',
      },
    ],
  },
  {
    number: '9.0',
    title: 'Resources',
    note: 'Maximum score: 2; for each item, 1.',
    items: [
      {
        key: '9.1',
        question:
          'Is the budget allotted by the project sufficient for gender equality promotion or integration?',
        alt: 'OR, will the project tap counterpart funds from LGUs/partners for its GAD efforts?',
        scale: 'HALF',
      },
      {
        key: '9.2',
        question:
          "Does the project have the expertise to promote gender equality and women's empowerment?",
        alt: 'OR, is the project committed to investing project staff time in building capacities within the project to integrate GAD or promote gender equality?',
        scale: 'HALF',
      },
    ],
  },
  {
    number: '10.0',
    title: "Relationship with the agency's GAD efforts",
    note: 'Maximum score: 2; for each item, 0.67.',
    items: [
      {
        key: '10.1',
        question:
          "Will the project build on or strengthen the agency/PCW/government's commitment to the empowerment of women?",
        conditional:
          "IF THE AGENCY HAS NO GAD PLAN: Will the project help in formulating the implementing agency's GAD plan?",
        scale: 'THIRD',
      },
      {
        key: '10.2',
        question:
          'Will the project build on the initiatives or actions of other organizations in the area?',
        scale: 'THIRD',
      },
      {
        key: '10.3',
        question:
          'Does the project have an exit plan that will ensure the sustainability of GAD efforts and benefits?',
        scale: 'THIRD',
      },
    ],
  },
]

/** Every assessment item, flattened in document order (17 items). */
export const GAD_ITEMS = GAD_CRITERIA.flatMap((criterion) =>
  criterion.items.map((item) => ({ ...item, criterion: criterion.number }))
)

/** The number of assessment items the checklist requires. */
export const GAD_ITEM_COUNT = GAD_ITEMS.length

/** The three valid responses, as a Set, so a stored value can be checked quickly. */
const GAD_RESPONSE_KEYS = new Set(GAD_RESPONSE_OPTIONS.map((option) => option.key))

/** Look up one item by its key (e.g. "6.3"). */
export function gadItem(itemKey) {
  return GAD_ITEMS.find((item) => item.key === itemKey) || null
}

/** Rounds a score to two decimals without floating-point noise (all scores are 2-dp). */
function roundScore(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

/** A score displayed the same way everywhere: two decimals ("0.5" -> "0.50"). */
export function formatGadScore(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '0.00'
  return number.toFixed(2)
}

/** "No = 0.00, Partly = 0.50, Yes = 1.00" - the possible scores of an item's scale. */
export function gadItemScoreLabel(scaleKey) {
  const scale = GAD_SCALES[scaleKey] || {}
  return GAD_RESPONSE_OPTIONS.map(
    (option) => `${option.label} = ${formatGadScore(scale[option.key] ?? 0)}`
  ).join(', ')
}

/** The score of one item for a chosen response, or null when the item is unanswered. */
export function gadItemScore(checklist, itemKey) {
  const item = gadItem(itemKey)
  if (!item) return null
  const response = checklist?.responses?.[itemKey]
  if (!GAD_RESPONSE_KEYS.has(response)) return null
  return GAD_SCALES[item.scale][response]
}

/** The subtotal of one criterion (the sum of its answered items, bounded by the maximum 2). */
export function gadCriterionSubtotal(checklist, criterion) {
  const sum = criterion.items.reduce(
    (total, item) => total + (gadItemScore(checklist, item.key) ?? 0),
    0
  )
  return Math.min(roundScore(sum), GAD_CRITERION_MAX)
}

/** The index of the interpretation band a total falls in, or -1. */
export function gadInterpretationIndex(totalScore) {
  const value = Number(totalScore)
  return GAD_INTERPRETATIONS.findIndex((band) => value >= band.min && value <= band.max)
}

/** The interpretation of a total GAD score. */
export function gadInterpretation(totalScore) {
  const index = gadInterpretationIndex(totalScore)
  return index >= 0 ? GAD_INTERPRETATIONS[index] : null
}

/**
 * The whole assessment: the per-criterion subtotals, the total GAD score, the completion
 * state and the interpretation. An incomplete checklist is never presented as final -
 * `completed` is false and `unanswered` lists the missing item keys.
 */
export function gadChecklistSummary(checklist) {
  const subtotals = {}
  let total = 0
  for (const criterion of GAD_CRITERIA) {
    const subtotal = gadCriterionSubtotal(checklist, criterion)
    subtotals[criterion.number] = subtotal
    total += subtotal
  }
  const totalScore = roundScore(total)
  const unanswered = GAD_ITEMS.filter((item) => !GAD_RESPONSE_KEYS.has(checklist?.responses?.[item.key])).map(
    (item) => item.key
  )
  return {
    subtotals,
    totalScore,
    maxScore: GAD_MAX_SCORE,
    answeredCount: GAD_ITEM_COUNT - unanswered.length,
    itemCount: GAD_ITEM_COUNT,
    unanswered,
    completed: unanswered.length === 0,
    interpretation: gadInterpretation(totalScore),
    interpretationIndex: gadInterpretationIndex(totalScore),
    formattedTotal: formatGadScore(totalScore),
  }
}

/** A blank checklist: every item unanswered, every comment empty. */
export function emptyGadChecklist() {
  return { responses: {}, comments: {} }
}

/**
 * The form state of the checklist from a saved value. A stored response that is not one of
 * NO/PARTLY/YES is treated as unanswered, and only the known items are kept, so a renamed or
 * removed item can never lose the rest of the assessment.
 */
export function gadChecklistFromSaved(saved) {
  const source = saved && typeof saved === 'object' ? saved : {}
  const storedResponses = source.responses && typeof source.responses === 'object' ? source.responses : {}
  const storedComments = source.comments && typeof source.comments === 'object' ? source.comments : {}
  const responses = {}
  const comments = {}
  for (const item of GAD_ITEMS) {
    if (GAD_RESPONSE_KEYS.has(storedResponses[item.key])) responses[item.key] = storedResponses[item.key]
    const comment = storedComments[item.key]
    comments[item.key] = typeof comment === 'string' ? comment : ''
  }
  return { responses, comments }
}

/**
 * The checklist as it is stored with the proposal: the responses and the comments the
 * proponent entered, plus the derived total, interpretation and completion state. The backend
 * recalculates the derived values from the responses, so they are a convenience, never the
 * authority.
 */
export function gadChecklistPayload(checklist) {
  const state = gadChecklistFromSaved(checklist)
  const summary = gadChecklistSummary(state)
  const comments = {}
  for (const [key, value] of Object.entries(state.comments)) {
    if (String(value || '').trim() !== '') comments[key] = value
  }
  const interpretation = summary.interpretation
  return {
    responses: state.responses,
    comments,
    totalScore: summary.totalScore,
    interpretation: summary.completed && interpretation ? interpretation.short || interpretation.label : '',
    completed: summary.completed,
  }
}

/**
 * The checklist to send when a proposal is saved.
 *
 * <p>Returns `null` - meaning "this client does not edit the checklist, keep whatever the
 * proposal already has" - only when the proposal never had a stored checklist and the
 * proponent has not entered a single response or comment yet. That keeps a legacy
 * proposal's stored GAD score/document from being replaced by an untouched empty
 * checklist while the form was never opened.</p>
 *
 * <p>Once a checklist has been saved, or once anything was entered, the current state is
 * sent instead - including an explicitly emptied one - so clearing the last answer is an
 * intentional clear, not "never started".</p>
 */
export function gadChecklistPayloadForSave(checklist, hasSavedChecklist = false) {
  const state = gadChecklistFromSaved(checklist)
  const hasResponse = Object.keys(state.responses).length > 0
  const hasComment = Object.values(state.comments).some(
    (value) => String(value || '').trim() !== ''
  )
  if (!hasSavedChecklist && !hasResponse && !hasComment) return null
  return gadChecklistPayload(state)
}

/** Validation for submission: every one of the 17 assessment items must have a response. */
export function gadChecklistValidation(checklist) {
  const summary = gadChecklistSummary(checklist)
  if (summary.completed) {
    return { valid: true, message: '', unanswered: [] }
  }
  const count = summary.unanswered.length
  const message =
    'Please select No, Partly or Yes for every GAD assessment item. ' +
    `${count} ${count === 1 ? 'item is' : 'items are'} still unanswered: ` +
    `${summary.unanswered.join(', ')}.`
  return { valid: false, message, unanswered: summary.unanswered }
}
