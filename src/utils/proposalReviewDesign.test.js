/**
 * proposalReviewDesign.test.js
 * ---------------------------------------------------------------------------
 * Guards the shared review design layer and the field section it repaints.
 *
 * The redesign is visual: every page keeps its own fields, actions, states and handlers. The
 * risk this covers is that a restyle silently drops one of them on the way. The files are read
 * as text - the technique the other guard tests use - so this needs no browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CSS = read('../assets/review.css')
const FIELD_SECTION = read('../components/ProposalFieldSection.vue')
const HEADER = read('../components/ProposalReviewHeader.vue')
const SIDEBAR = read('../components/ProposalReviewSidebar.vue')
const PAGE = read('../components/ProposalReviewPage.vue')

test('the design layer defines the page, header, card and summary primitives', () => {
  for (const cls of [
    '.rv-page',
    '.rv-header',
    '.rv-back',
    '.rv-title',
    '.rv-id',
    '.rv-status',
    '.rv-header-actions',
    '.rv-body',
    '.rv-main',
    '.rv-aside',
    '.rv-card',
    '.rv-card-head',
    '.rv-card-body',
    '.rv-rows',
    '.rv-row',
    '.rv-state',
    '.rv-spinner',
  ]) {
    assert.ok(
      CSS.includes(cls + ' ') || CSS.includes(cls + ','),
      'review.css must define ' + cls,
    )
  }
})

test('the design layer is responsive and cannot overflow horizontally', () => {
  assert.ok(CSS.includes('@media (max-width: 1024px)'), 'the summary column drops below content')
  assert.ok(CSS.includes('@media (max-width: 640px)'), 'the header stacks on a phone')
  assert.ok(CSS.includes('minmax(0, 1fr)'), 'grid tracks are minmax(0, ...) so long values wrap')
  assert.ok(CSS.includes('overflow-wrap: anywhere'), 'long values wrap instead of overflowing')
})

test('the shared field section paints the review card design', () => {
  assert.ok(FIELD_SECTION.includes('Review design layer'), 'the design rules are present')
  assert.ok(FIELD_SECTION.includes('linear-gradient(100deg, #262342'), 'the navy header band')
  assert.ok(FIELD_SECTION.includes('border-radius: var(--radius-xl, 14px)'), 'the card radius')
  assert.ok(FIELD_SECTION.includes('max-width: 640px'), 'the field grid collapses on a phone')
})

test('the field section still renders every field through the registry', () => {
  // No field may be removed or merged by the redesign: the sections still come from the
  // registry and every field still carries its own remark control.
  assert.ok(FIELD_SECTION.includes('FieldRemarkControl'), 'remark controls are untouched')
  assert.ok(FIELD_SECTION.includes('displayValue(field)'), 'values still come from the registry')
  assert.ok(FIELD_SECTION.includes('section.fields'), 'fields are still iterated from the section')
  assert.ok(FIELD_SECTION.includes('agendaFields'), 'keyed agenda entries are still rendered')
})

test('the shared header and summary cards are prop-driven and never fetch', () => {
  for (const [name, source] of [['header', HEADER], ['sidebar', SIDEBAR], ['page', PAGE]]) {
    assert.ok(!/api\.(get|post|put|delete)/.test(source), name + ' must not call the API')
    assert.ok(source.includes('defineProps'), name + ' must take what the page already loaded')
  }
  assert.ok(HEADER.includes("$emit('back')"), 'the header keeps the page in charge of Back')
  assert.ok(HEADER.includes("$emit('download')"), 'and of DOWNLOAD')
  assert.ok(PAGE.includes('slot name="actions"'), 'the role actions render in the header')
  assert.ok(SIDEBAR.includes('Proposal Status'), 'the status card')
  assert.ok(SIDEBAR.includes('Quick Information'), 'the quick information card')
  assert.ok(SIDEBAR.includes('Review Information'), 'the review information card')
})

test('the page shell owns the four states, so no page can render blank', () => {
  assert.ok(PAGE.includes('v-if="loading"'), 'a loading state')
  assert.ok(PAGE.includes('v-else-if="error"'), 'an error state with a retry')
  assert.ok(PAGE.includes('v-else-if="notFound"'), 'a not-found state')
  assert.ok(PAGE.includes("$emit('retry')"), 'the retry path')
})

test('every component that renders .rv-* markup carries the stylesheet', () => {
  // The defect this guards: the design layer was imported only by a page wrapper that no page
  // used, so Rollup dropped the whole layer from the bundle and every .rv-* class was
  // undefined at runtime - the header lost its row, the cards lost their boxes and the labels
  // ran into their values. A component that renders .rv-* markup must import the stylesheet.
  for (const [name, source] of [
    ['ProposalReviewHeader.vue', HEADER],
    ['ProposalReviewSidebar.vue', SIDEBAR],
    ['ProposalReviewPage.vue', PAGE],
  ]) {
    assert.ok(
      source.includes('@/assets/review.css'),
      name + ' must import the review design layer it renders with',
    )
  }
  assert.ok(HEADER.includes('class="rv-header"'), 'the header renders .rv-* markup')
  assert.ok(SIDEBAR.includes('class="rv-cards"'), 'the summary cards render .rv-* markup')
})

test('a label and its value can never run together', () => {
  // The reported symptom was "CreatedJul 29, 2026". The row is a flex column, so the two are
  // always on their own lines whatever else is loaded around them.
  assert.match(CSS, /\.rv-row \{[^}]*display: flex/, 'the summary row is a flex box')
  assert.match(CSS, /\.rv-row \{[^}]*flex-direction: column/, 'stacked, never inline')
  assert.match(CSS, /\.rv-row-value \{[^}]*text-align: left/, 'the value sits under its label')
})

test('the header keeps its actions on the far side and wraps instead of overlapping', () => {
  assert.match(CSS, /\.rv-header-actions \{[^}]*margin-left: auto/, 'DOWNLOAD stays right')
  assert.match(CSS, /\.rv-header \{[^}]*flex-wrap: wrap/, 'the header wraps on a narrow screen')
  assert.match(CSS, /\.rv-heading-meta \{[^}]*flex-wrap: wrap/, 'the status pill never overlaps the id')
})

test('the OVCRIGE review page downloads the proposal through the shared helper', () => {
  // The defect this guards: this page fired only the four attachment downloads from its
  // header, so on a proposal without them the DOWNLOAD button did nothing at all.
  const ovc = read('../views/Ovc/OvcReviewProposal.vue')
  assert.ok(
    ovc.includes("import { downloadOfficialProposal } from '@/utils/officialProposalForm'"),
    'the page must use the shared document builder',
  )
  assert.ok(ovc.includes('@download="downloadProposal"'), 'the header calls the page handler')
  assert.ok(
    ovc.includes('downloadOfficialProposal(proposal.value)'),
    'which builds the complete proposal document',
  )
  assert.ok(
    !ovc.includes('@download="downloadAll"'),
    'the attachment-only action is no longer the header action',
  )
  assert.ok(ovc.includes('downloadLoading'), 'the button reports that it is working')
  assert.ok(ovc.includes('Could not prepare the proposal document'), 'a failure is reported')
  // The attachments stay reachable one by one.
  assert.ok(ovc.includes('downloadFile(proposal.reviewFileName)'), 'the attachment buttons stay')
})

test('every role keeps using the shared proposal-download helper', () => {
  for (const file of [
    '../views/Rps/RpsProposalDetails.vue',
    '../views/Rps/RpsProposalReview.vue',
    '../views/Ovcaf/OvcafValidate.vue',
    '../views/Proponent/ProponentDetiailedProp.vue',
  ]) {
    assert.ok(
      read(file).includes('downloadOfficialProposal('),
      file + ' must still use the shared helper',
    )
  }
  assert.ok(HEADER.includes('downloadLoading'), 'the shared header exposes the busy state')
  assert.ok(CSS.includes('.rv-download:disabled'), 'the button shows it is busy')
})

test('every adopted page uses the shared layer and keeps its own actions', () => {
  // A page is restyled by swapping its chrome for the shared components - never by dropping
  // what it does. This is the list of pages that have adopted the design, each with the
  // handlers that must still be reachable on it.
  const ADOPTED = [
    [
      '../views/Ovc/OvcReviewProposal.vue',
      ['forwardToRec', 'forwardToOc', 'returnForRevision', 'rejectProposal', 'downloadProposal', 'goBack'],
    ],
    ['../views/Rec/RecDetailedProp.vue', ['goBack', 'loadProposal']],
    ['../views/Rps/RpsProposalDetails.vue', ['beginReview', 'downloadProposal', 'goBack']],
    [
      '../views/Rps/RpsProposalReview.vue',
      ['endorseProposal', 'returnForRevision', 'rejectProposal', 'downloadProposal'],
    ],
  ]

  for (const [file, handlers] of ADOPTED) {
    const source = read(file)
    assert.ok(source.includes('ProposalReviewHeader'), file + ' must use the shared header')
    assert.ok(source.includes('rv-page'), file + ' must use the shared page shell')
    for (const handler of handlers) {
      assert.ok(source.includes(handler), file + ' must still expose ' + handler)
    }
  }
})

test('the submission history shown on the detail pages is on the shared design', () => {
  const history = read('../components/SubmissionHistory.vue')
  assert.ok(history.includes('Review design layer'), 'the history is restyled')
  assert.ok(history.includes('linear-gradient(100deg, #262342'), 'with the navy header band')
  assert.match(history, /\.sh-meta \{[^}]*flex-direction: column/, 'label above value')
  assert.ok(history.includes('@media (max-width: 640px)'), 'and a phone breakpoint')
  // The version loading and the details toggle are untouched.
  assert.ok(history.includes('/versions'), 'the version endpoint is unchanged')
  assert.ok(history.includes('toggleDetails'), 'the details toggle still works')
})

/** Every page that renders whichever proposal the route points at. */
const DETAIL_PAGES = [
  '../views/Proponent/ProponentDetiailedProp.vue',
  '../views/Proponent/ProponentPropDetails.vue',
  '../views/Proponent/ProponentProposalMonitoring.vue',
  '../views/Rps/RpsProposalDetails.vue',
  '../views/Rps/RpsProposalReview.vue',
  '../views/Rps/RpsProposalMonitoringDetails.vue',
  '../views/Rps/RpsForwardRevision.vue',
  '../views/Ovc/OvcReviewProposal.vue',
  '../views/Ovcaf/OvcafProposalDetails.vue',
  '../views/Ovcaf/OvcafValidate.vue',
  '../views/Oc/OCFinalApproval.vue',
  '../views/Oc/OcDetailedProposal.vue',
  '../views/Rec/RecDetailedProp.vue',
  '../views/Rec/RecReviewForm.vue',
]

/**
 * The source with its comments removed, so documentation is never mistaken for code. Both
 * comment styles are stripped: a block comment in this very file mentions
 * "/detailed-proposal?id=52", which is exactly the kind of text a naive scan would flag.
 */
const codeOnly = (source) =>
  source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split('\n')
    .filter((line) => !line.trim().startsWith('//'))
    .join('\n')

test('the design is universal: no detail page pins a proposal id', () => {
  // A literal id in any of these would tie the page - or its data - to one record instead of
  // rendering whichever proposal the route points at.
  for (const file of DETAIL_PAGES) {
    const code = codeOnly(read(file))
    assert.ok(
      !/(?<![\w-])(?:id|proposalId)\s*[:=]\s*['\"]?\d+/.test(code),
      file + ' must not hardcode a proposal id',
    )
  }
})

test('each detail page takes its id from the route and reacts when it changes', () => {
  // Vue Router reuses the component when only the id changes, so a page that only loads on
  // mount would keep showing the previous proposal under the new id.
  const REACTIVE = [
    '../views/Proponent/ProponentDetiailedProp.vue',
    '../views/Proponent/ProponentPropDetails.vue',
    '../views/Rps/RpsProposalDetails.vue',
    '../views/Rps/RpsForwardRevision.vue',
    '../views/Ovc/OvcReviewProposal.vue',
    '../views/Ovcaf/OvcafProposalDetails.vue',
    '../views/Ovcaf/OvcafValidate.vue',
    '../views/Rec/RecDetailedProp.vue',
    '../views/Rec/RecReviewForm.vue',
  ]

  for (const file of REACTIVE) {
    const source = read(file)
    assert.ok(/route\.(params|query)\.id/.test(source), file + ' must read the id from the route')
    assert.match(source, /watch\(/, file + ' must reload when the route id changes')
  }
})

test('the shared components carry no proposal data of their own', () => {
  for (const [name, source] of [
    ['header', HEADER],
    ['sidebar', SIDEBAR],
    ['page shell', PAGE],
    ['field section', FIELD_SECTION],
  ]) {
    assert.ok(!/MSUN|RIIDE|DAGAT|PUNLA/.test(source), name + ' must not carry sample proposal data')
    assert.ok(
      !/(?<![\w-])(?:id|proposalId)\s*[:=]\s*['\"]?\d+/.test(codeOnly(source)),
      name + ' must not pin a proposal id',
    )
  }
})