/**
 * fundingProgram.test.js
 * ---------------------------------------------------------------------------
 * The user-facing funding program name. The stored value stays IFP; every screen
 * reads the complete name.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  FUNDING_PROGRAM_IFP,
  FUNDING_PROGRAM_IFP_LABEL,
  fundingProgramDisplayName,
  fundingProgramLabelOf,
} from './fundingProgram.js'

test('IFP is displayed as Internally Funded Proposal', () => {
  assert.equal(FUNDING_PROGRAM_IFP, 'IFP')
  assert.equal(FUNDING_PROGRAM_IFP_LABEL, 'Internally Funded Proposal')
  assert.equal(fundingProgramDisplayName('IFP'), 'Internally Funded Proposal')
  assert.equal(fundingProgramDisplayName(' ifp '), 'Internally Funded Proposal')
})

test('a backend IFP label is normalized to the same complete name', () => {
  assert.equal(
    fundingProgramDisplayName('IFP', 'Internally Funded Project Proposal (IFP)'),
    'Internally Funded Proposal',
  )
  assert.equal(
    fundingProgramDisplayName(undefined, 'Internally Funded Project Proposal (IFP)'),
    'Internally Funded Proposal',
  )
})

test('another program keeps its own label or key', () => {
  assert.equal(
    fundingProgramDisplayName('EXTERNALLY_FUNDED', 'Externally Funded Project Proposal'),
    'Externally Funded Project Proposal',
  )
  assert.equal(fundingProgramDisplayName('EXTERNALLY_FUNDED'), 'EXTERNALLY_FUNDED')
})

test('a call with no program has the explicit unspecified label', () => {
  assert.equal(fundingProgramDisplayName(), 'Unspecified funding program')
  assert.equal(fundingProgramLabelOf({}), 'Unspecified funding program')
  assert.equal(
    fundingProgramLabelOf({ fundingProgram: 'IFP' }),
    'Internally Funded Proposal',
  )
})
