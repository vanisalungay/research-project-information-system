/**
 * riskManagementPlan.test.js
 * ---------------------------------------------------------------------------
 * Guards how section 21 (Risk Management Plan) is wired end to end now that it is a
 * structured table instead of one big text field:
 *
 *   * the Proponent form and the Revision form edit rows through the same component,
 *   * the four values travel to the API as separate fields, and a proposal saved before
 *     the table existed keeps its single text (backward compatibility),
 *   * every review page and both generated documents show the same table.
 *
 * The files are read as text - the technique the other guard tests use - so this needs no
 * browser, no database and no build. The rules themselves are executed in
 * riskManagement.test.js. Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const FORM = read('../views/Proponent/ProponentSubmitProp.vue')
const STEP4 = read('../views/Proponent/ProponentSubmitProp3.vue')
const REVISION = read('../views/Proponent/ProponentSubmitRev.vue')
const TABLE = read('../components/RiskManagementTable.vue')
const REGISTRY = read('./proposalFields.js')
const fieldSection = read('../components/ProposalFieldSection.vue')
const DETAIL = read('../views/Proponent/ProponentDetiailedProp.vue')
const HISTORY = read('../components/SubmissionHistory.vue')
const DOCUMENT = read('./documentExport.js')
const OFFICIAL = read('./officialProposalForm.js')

const COLUMN_LABELS = ['Identified Risks', 'Likelihood', 'Impact', 'Mitigation Measure']

test('the Create Proposal form replaces the single text field with the table', () => {
  assert.ok(FORM.includes("import RiskManagementTable from '@/components/RiskManagementTable.vue'"))
  assert.ok(FORM.includes('v-model="proposal.risk_management"'), 'the table is bound to the form')
  assert.ok(FORM.includes('risk_management: [blankRiskEntry()]'), 'a new proposal starts with one row')
  assert.ok(
    !FORM.includes('v-model="proposal.risks_assumptions"'),
    'the old single text field is gone from section 21'
  )
  assert.ok(FORM.includes("risk_management: 'section_risks'"), 'an error routes to section 21')
})

test('the table offers the four fields, the dropdowns and add/remove', () => {
  for (const label of COLUMN_LABELS) {
    assert.ok(TABLE.includes(label) || REGISTRY.includes(label), `the table shows ${label}`)
  }
  assert.ok(TABLE.includes('RISK_COLUMNS'), 'the columns come from the shared module')
  assert.ok(TABLE.includes('RISK_LEVELS'), 'the levels come from the shared module')
  assert.ok(TABLE.includes('<select'), 'likelihood and impact are dropdowns')
  assert.ok(!TABLE.includes('type="text"'), 'no free-text control for a level')
  assert.ok(TABLE.includes('+ Add Risk'), 'the Add Risk button is below the table')
  assert.ok(TABLE.includes('blankRiskEntry()'), 'it adds an empty row')
  assert.ok(TABLE.includes('removeRow'), 'and every row can be removed')
  // Adding keeps every row that is already there, and removing touches exactly one row.
  assert.ok(
    TABLE.includes('rows.value = [...rows.value, blankRiskEntry()]'),
    'adding a row never drops the existing entries'
  )
  assert.ok(
    TABLE.includes('rows.value.filter((_, position) => position !== index)'),
    'removing a row never drops the other entries'
  )
})

test('the entries are submitted as separate fields through every proponent step', () => {
  for (const [name, source] of [['form', FORM], ['review & submit', STEP4], ['revision', REVISION]]) {
    assert.ok(source.includes('riskManagement: riskEntriesPayload('), `${name} sends the entries`)
    assert.ok(source.includes('validateRiskPlan('), `${name} applies the plan rule`)
  }
  assert.ok(
    FORM.includes('risk_management = ensureRiskRows(riskEntriesFromSaved('),
    'a draft reopens its rows'
  )
  assert.ok(
    REVISION.includes('risk_management: ensureRiskRows(riskEntriesFromSaved('),
    'so does a revision'
  )
})

test('a proposal saved before the table keeps its single text on both proponent forms', () => {
  // The Create Proposal workflow now saves through Review & Submit, so that step is the
  // one that must still send the legacy text; the details form keeps and shows it.
  assert.ok(FORM.includes('legacyRiskPlan'), 'the details form keeps and shows the old plan')
  assert.ok(REVISION.includes('legacyRiskPlan'), 'the revision form keeps and shows the old plan')
  for (const [name, source] of [['review & submit', STEP4], ['revision', REVISION]]) {
    assert.ok(
      source.includes('risks: data.risks') || source.includes('risks: data.risks_assumptions'),
      `${name} still sends the legacy text`
    )
  }
  assert.ok(
    REGISTRY.includes("when: (proposal) => collectionRows(proposal, 'riskManagement').length === 0"),
    'the review only shows the single text while there are no entries'
  )
  assert.ok(fieldSection.includes('fieldsOf('), "the shared renderer honours a field's when()")
})

test('the review pages render the structured plan from the shared registry', () => {
  assert.ok(REGISTRY.includes("prop: 'riskManagement'"), 'section 21 declares the collection')
  assert.ok(REGISTRY.includes("key: 'identifiedRisks'"), 'identified risks are rendered')
  assert.ok(REGISTRY.includes("key: 'likelihood'"), 'likelihood is rendered')
  assert.ok(REGISTRY.includes("key: 'impact'"), 'impact is rendered')
  assert.ok(REGISTRY.includes("key: 'mitigationMeasure'"), 'the mitigation measure is rendered')
  assert.ok(fieldSection.includes('section.collection'), 'the shared renderer draws the collection')
})

test('every page that shows the plan itself shows the table and keeps the old text as fallback', () => {
  const pages = [
    ['ProponentDetiailedProp', DETAIL],
    ['SubmissionHistory', HISTORY]
  ]
  for (const [name, source] of pages) {
    assert.ok(source.includes('riskManagement'), `${name} reads the structured entries`)
    for (const label of COLUMN_LABELS) {
      assert.ok(source.includes(label), `${name} shows the ${label} column`)
    }
    assert.ok(source.includes('risks'), `${name} keeps the single-text fallback`)
  }
  // The unwired RPS summary pages carried a placeholder property instead of the plan.
  for (const file of ['RpsEndPro', 'RpsBudgetRelease', 'RpsRevResultViewer']) {
    const source = read(`../views/Rps/${file}.vue`)
    assert.ok(!source.includes('risk_management_plan'), `${file} no longer uses the placeholder`)
    assert.ok(source.includes('riskManagement'), `${file} reads the structured entries`)
    assert.ok(source.includes('proposal.risks'), `${file} keeps the single-text fallback`)
  }
})

test('both generated documents print the structured table', () => {
  for (const [name, source] of [['proposal document', DOCUMENT], ['official form', OFFICIAL]]) {
    for (const label of COLUMN_LABELS) {
      assert.ok(
        source.includes(label) || source.includes(label.toUpperCase()),
        `${name} prints ${label}`
      )
    }
    assert.ok(source.includes('riskRowsFrom('), `${name} reads the structured entries`)
    assert.ok(source.includes('p.risks'), `${name} falls back to the single text`)
  }
})

test('the backend stores the entries separately, in their own column', () => {
  const migration = readFileSync(
    fileURLToPath(new URL('../../backend/migration-add-risk-management-json.sql', import.meta.url)),
    'utf8'
  )
  assert.ok(migration.includes('ALTER TABLE proposals'), 'the migration targets the proposals table')
  assert.ok(
    migration.includes('ADD COLUMN IF NOT EXISTS risk_management_json TEXT'),
    'it adds the structured column additively'
  )
  assert.ok(!/DROP\s+(TABLE|COLUMN)/i.test(migration), 'nothing is dropped')
  const schema = readFileSync(
    fileURLToPath(new URL('../../backend/schema.sql', import.meta.url)),
    'utf8'
  )
  assert.ok(schema.includes('risk_management_json TEXT'), 'the development schema has the column too')
  const launcher = readFileSync(
    fileURLToPath(new URL('../../run-backend.bat', import.meta.url)),
    'utf8'
  )
  assert.ok(launcher.includes('migration-add-risk-management-json.sql'), 'the launcher applies it')

  const entry = readFileSync(
    fileURLToPath(new URL('../../backend/src/main/java/com/rpis/backend/model/RiskManagementEntry.java', import.meta.url)),
    'utf8'
  )
  for (const field of ['identifiedRisks', 'likelihood', 'impact', 'mitigationMeasure']) {
    assert.ok(entry.includes(field), `the entry stores ${field}`)
  }
  const entity = readFileSync(
    fileURLToPath(new URL('../../backend/src/main/java/com/rpis/backend/model/Proposal.java', import.meta.url)),
    'utf8'
  )
  assert.ok(entity.includes('RiskManagementListConverter'), 'the entity converts the rows to JSON')
  assert.ok(entity.includes('riskManagement'), 'and exposes them as a collection')
  const service = readFileSync(
    fileURLToPath(new URL('../../backend/src/main/java/com/rpis/backend/service/ProposalService.java', import.meta.url)),
    'utf8'
  )
  assert.ok(service.includes('validateRiskManagement('), 'the backend validates the rows')
  assert.ok(service.includes('RISK_LEVELS'), 'and only accepts High / Medium / Low')
  assert.ok(service.includes('renderRiskManagement('), 'the legacy text is derived from the entries')
  assert.ok(service.includes('copy.setRiskManagement('), 'a duplicate keeps its own copy of the rows')
})

test('no sample risk entry is stored by the application', () => {
  // The example values only ever appear as placeholders / documentation - never as data
  // written to a proposal.
  assert.ok(!/setRiskManagement\s*\(/.test(FORM), 'the form never hardcodes rows')
  assert.ok(!TABLE.includes('riskEntriesPayload([') && !TABLE.includes('rows = ['), 'the table starts from props')
  const seeder = readFileSync(
    fileURLToPath(new URL('../../backend/src/main/java/com/rpis/backend/dev/SyntheticTestDataSeeder.java', import.meta.url)),
    'utf8'
  )
  assert.ok(!seeder.includes('riskManagement'), 'the synthetic seeder writes no risk entries')
})
