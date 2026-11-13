/**
 * officialProposalForm.test.js
 * ---------------------------------------------------------------------------
 * Section 25 of the official proposal form ("Line-Item Budget Requirement") used to render
 * the legacy amount table. The form now uploads a filled PDF/DOCX budget document instead,
 * so the generated draft must name the attached document - or say that none is attached -
 * and must never rebuild/fabricate an amount table from empty budget rows.
 *
 * The renderer is validated at the source level (it imports the seal with a Vite `?inline`
 * query, which Node cannot resolve); the document-name rule itself is imported and tested
 * directly.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  budgetDocumentName,
  budgetDocumentNameFromPath,
  budgetDocumentPath,
} from './budgetDocument.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const OFFICIAL = read('./officialProposalForm.js')
const EXPORT = read('./documentExport.js')
const STEP1 = read('../views/Proponent/ProponentSubmitProp.vue')

/** The source of one `function sectionNN(p) { ... }` block. */
const sectionSource = (source, number) => {
  const start = source.indexOf(`function section${number}(p)`)
  assert.ok(start >= 0, `section ${number} must exist`)
  const end = source.indexOf('\nfunction ', start + 1)
  return source.slice(start, end === -1 ? source.length : end)
}

test('the budget document name prefers the form field and falls back to the stored column', () => {
  assert.equal(
    budgetDocumentName({ budgetFileName: 'Sample_Budget_Proposal.pdf' }),
    'Sample_Budget_Proposal.pdf',
  )
  assert.equal(
    budgetDocumentName({ beneficiariesFileName: 'Stored_Budget.pdf' }),
    'Stored_Budget.pdf',
  )
  assert.equal(
    budgetDocumentName({
      budgetFileName: 'Sample_Budget_Proposal.pdf',
      beneficiariesFileName: 'Stored_Budget.pdf',
    }),
    'Sample_Budget_Proposal.pdf',
    'the name typed/uploaded in this session wins',
  )
  assert.equal(budgetDocumentName({ budgetFileName: '  Sample Budget.pdf  ' }), 'Sample Budget.pdf')
  assert.equal(budgetDocumentName({ budgetFileName: '   ' }), '')
  assert.equal(budgetDocumentName({}), '')
  assert.equal(budgetDocumentName(null), '')
})

test('a draft that lost its display name still names the document from the stored path', () => {
  const storedPath = 'budget-files/3f1c2a9e-8b7d-4c6a-9e2f-1a2b3c4d5e6f_line-item-budget.docx'

  assert.equal(budgetDocumentPath({ beneficiariesFilePath: storedPath }), storedPath)
  assert.equal(budgetDocumentPath({ budgetFilePath: storedPath }), storedPath)
  assert.equal(budgetDocumentPath({ budget_file_path: storedPath }), storedPath,
    'the Proponent form state uses the snake_case key')
  assert.equal(budgetDocumentPath({ beneficiariesFileName: 'Budget.pdf' }), '')

  assert.equal(budgetDocumentNameFromPath(storedPath), 'line-item-budget.docx',
    'the generated uniqueness prefix is not shown to the proponent')
  assert.equal(
    budgetDocumentNameFromPath('uploads/technology-maps/9f8e7d6c-5b4a-3210-9f8e-7d6c5b4a3210_map.pdf'),
    'map.pdf',
  )
  assert.equal(budgetDocumentNameFromPath('budget-files/'), '')
  assert.equal(budgetDocumentNameFromPath('   '), '')
  assert.equal(budgetDocumentNameFromPath(null), '')

  assert.equal(
    budgetDocumentName({ beneficiariesFilePath: storedPath }),
    'line-item-budget.docx',
    'a stored file must not be reported as missing just because its name is empty',
  )
  assert.equal(
    budgetDocumentName({ beneficiariesFileName: '   ', beneficiariesFilePath: storedPath }),
    'line-item-budget.docx',
    'a blank display name never hides a document that is still attached',
  )
  assert.equal(
    budgetDocumentName({ beneficiariesFileName: 'Budget.pdf', beneficiariesFilePath: storedPath }),
    'Budget.pdf',
    'the real uploaded name always wins',
  )
})

test('section 25 names the attached budget document instead of rebuilding the table', () => {
  const section = sectionSource(OFFICIAL, 25)

  assert.ok(section.includes('budgetDocumentName(p)'),
    'the attached document reference is read from the shared rule')
  assert.ok(section.includes('Attached Budget Document: ${esc(budgetDocument)}'),
    'the attached document is named exactly as the requirement asks')
  assert.ok(section.includes('No budget document uploaded.'),
    'a proposal without a budget says so instead of printing an empty table')
  assert.ok(!section.includes('dataGrid('), 'the legacy budget grid is gone')
  assert.ok(!section.includes('p.budget'), 'no amount rows are read')
})

test('section 25 never invents amounts or falls back to a zeroed table', () => {
  const section = sectionSource(OFFICIAL, 25)
  for (const amount of ['PS', 'MOOE', 'EO', 'TOTAL']) {
    assert.ok(!section.includes(`'${amount}'`),
      `the column header ${amount} must not be rendered`)
  }
  assert.ok(!section.includes('b.ps') && !section.includes('b.mooe'),
    'no per-row amounts are printed')
})

test('the budget document is not mislabelled as a target-beneficiaries attachment', () => {
  const section = sectionSource(OFFICIAL, 27)
  assert.ok(!section.includes('beneficiariesFileName'),
    'section 27 must not print the budget document as "Target Beneficiaries"')
})

test('the legacy export builder uses the same budget-document rendering', () => {
  assert.ok(EXPORT.includes("import { budgetDocumentName } from '@/utils/budgetDocument'"))
  // The export builder passes section objects rather than positional functions, so assert
  // on its section 25 definition directly.
  const start = EXPORT.indexOf("number: '25'")
  assert.ok(start >= 0, 'section 25 exists')
  const block = EXPORT.slice(start, EXPORT.indexOf('}),', start))
  assert.ok(block.includes('fileField(\'Attached Budget Document\', budgetFileName)'))
  assert.ok(block.includes('No budget document uploaded.'))
  assert.ok(!block.includes('budgetRows'), 'the old amount table is not used')
  assert.ok(!EXPORT.includes('const budgetRows = '), 'no legacy budget rows are built')
})

test('the Proponent form hands the official renderer the uploaded budget name', () => {
  assert.ok(
    STEP1.includes('budgetFileName: d.budget_file_name || fileName(d.line_item_budget_file)'),
    'the stored/uploaded budget name reaches section 25',
  )
  assert.ok(
    STEP1.includes('budgetFilePath: d.budget_file_path || \'\''),
    'the stored path reaches section 25, so a lost display name can be recovered',
  )
  assert.ok(
    STEP1.includes('beneficiariesFilePath: d.budget_file_path || \'\''),
    'the backend column name carries the same stored path',
  )
})
