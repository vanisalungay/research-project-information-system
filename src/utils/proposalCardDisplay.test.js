/**
 * proposalCardDisplay.test.js
 * ---------------------------------------------------------------------------
 * The OVCAF proposal information card shows an avatar, a college badge and a file type.
 * Each of those is derived from the saved proposal record, never from sample data.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  collegeDisplayParts,
  fileTypeDescription,
  initialsFromName,
} from './proposalCardDisplay.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const CARD = read('../views/Ovcaf/OvcafFinancialValidation.vue')

test('the avatar initials come from the saved project leader name', () => {
  assert.equal(initialsFromName('Andrea Mae Villanueva'), 'AV')
  assert.equal(initialsFromName('  andrea   villanueva  '), 'AV')
  assert.equal(initialsFromName('Andrea Mae Villanueva, PhD'), 'AV')
  assert.equal(initialsFromName('Dr. Andrea Villanueva'), 'AV', 'a title is not an initial')
  assert.equal(initialsFromName('Cher'), 'C', 'a single name keeps one letter')
  assert.equal(initialsFromName(''), '')
  assert.equal(initialsFromName(null), '')
  assert.equal(initialsFromName(undefined), '')
})

test('the college name and abbreviation are split from the saved college value', () => {
  assert.deepEqual(
    collegeDisplayParts('College of Environment and Life Sciences (CELS)'),
    { name: 'College of Environment and Life Sciences', abbreviation: 'CELS' },
  )
  assert.deepEqual(
    collegeDisplayParts('College of Business and Information Technology (CBIT)'),
    { name: 'College of Business and Information Technology', abbreviation: 'CBIT' },
  )
})

test('a college without a stored abbreviation gets one derived from its words', () => {
  assert.deepEqual(
    collegeDisplayParts('College of Environment and Life Sciences'),
    { name: 'College of Environment and Life Sciences', abbreviation: 'CELS' },
  )
  assert.deepEqual(
    collegeDisplayParts('Integrated Development School'),
    { name: 'Integrated Development School', abbreviation: 'IDS' },
  )
})

test('an unknown or empty college never invents a badge', () => {
  assert.deepEqual(collegeDisplayParts('Unknown'), { name: 'Unknown', abbreviation: '' })
  assert.deepEqual(collegeDisplayParts(''), { name: '', abbreviation: '' })
  assert.deepEqual(collegeDisplayParts(null), { name: '', abbreviation: '' })
})

test('the file type description is read from the uploaded file name', () => {
  assert.equal(fileTypeDescription('line-item-budget.pdf'), 'PDF document')
  assert.equal(fileTypeDescription('Line-Item Budget.PDF'), 'PDF document')
  assert.equal(fileTypeDescription('budget.docx'), 'Word document')
  assert.equal(fileTypeDescription('budget.xlsx'), 'Spreadsheet')
  assert.equal(fileTypeDescription('photo.png'), 'Image')
  assert.equal(fileTypeDescription('archive.zip'), 'Document', 'an unknown type is still a document')
  assert.equal(fileTypeDescription('budget'), 'Document', 'a missing extension is still a document')
  assert.equal(fileTypeDescription(''), '')
})

test('the card renders every value from the proposal, not from sample data', () => {
  assert.ok(CARD.includes('initialsFromName('), 'the avatar initials are derived')
  assert.ok(CARD.includes('collegeDisplayParts('), 'the college name/abbreviation are derived')
  assert.ok(CARD.includes('fileTypeDescription('), 'the file type is derived')
  assert.ok(CARD.includes('proposal.documentId'), 'the document code is the saved value')
  assert.ok(CARD.includes('proposal.title'), 'the title is the saved value')
  assert.ok(CARD.includes('budgetFileName'), 'the file name is the uploaded name')

  assert.ok(!CARD.includes('Andrea'), 'no sample project leader')
  assert.ok(!CARD.includes('MSUN-ORPS-DA-123'), 'no sample document code')
  assert.ok(!CARD.includes('endorsement-letter'), 'no sample file name')
})

test('the card keeps the real view, download and navigation actions', () => {
  assert.ok(CARD.includes('@click="viewBudgetDocument"'), 'View opens the existing preview')
  assert.ok(CARD.includes('@click="saveBudgetDocument"'), 'Download saves the uploaded file')
  assert.ok(CARD.includes('@click="openProposalDetails"'), 'View Proposal Details opens the viewer')
  assert.ok(CARD.includes(':proposal-id="proposal.id"'), 'the correct proposal is shown')
  assert.ok(CARD.includes('LineItemBudgetPreviewModal'), 'the existing preview modal is reused')
})
