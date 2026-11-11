/**
 * soDraft.test.js
 * ---------------------------------------------------------------------------
 * The "is this proposal carrying a saved SO draft?" rule is shared by the OC
 * final-approval page and the Chancellor dashboard. These tests pin it down:
 *
 *  - a draft is still at FOR_OC_APPROVAL and has no SO date (a draft must never be
 *    mistaken for a completed OC approval by the OVCAF/quarterly checks);
 *  - an SO number or an SO document is what makes it a draft;
 *  - a budget choice alone is NOT a draft: legacy/seeded records can carry one
 *    without any OC form ever being saved.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { isSoDraft, soDraftFileLabel, draftSnapshot } from './soDraft.js'

test('a saved SO number at OC is a draft', () => {
  assert.equal(isSoDraft({ status: 'FOR_OC_APPROVAL', soNumber: 'SO-2026-001' }), true)
  assert.equal(isSoDraft({ status: 'for_oc_approval', soNumber: '  SO-2026-001  ' }), true)
})

test('a saved SO document at OC is a draft, even without a number', () => {
  assert.equal(
    isSoDraft({
      status: 'FOR_OC_APPROVAL',
      soFilePath: 'special-orders/uuid_special-order.pdf',
      soFileName: 'special-order.pdf',
    }),
    true,
  )
})

test('a budget choice or notes alone are not a draft', () => {
  assert.equal(isSoDraft({ status: 'FOR_OC_APPROVAL', needsBudget: true }), false)
  assert.equal(isSoDraft({ status: 'FOR_OC_APPROVAL', needsBudget: false }), false)
  assert.equal(
    isSoDraft({ status: 'FOR_OC_APPROVAL', remarks: 'Synthetic evaluation passed.' }),
    false,
  )
})

test('a finalized approval is not a draft', () => {
  assert.equal(
    isSoDraft({
      status: 'FOR_OC_APPROVAL',
      soNumber: 'SO-2026-001',
      soUploadedAt: '2026-11-11T09:00:00',
    }),
    false,
    'soUploadedAt marks the completed OC approval',
  )
  assert.equal(isSoDraft({ status: 'FOR_OVCAF_APPROVAL', soNumber: 'SO-2026-001' }), false)
  assert.equal(isSoDraft({ status: 'APPROVED', soNumber: 'SO-2026-001' }), false)
})

test('missing or malformed proposals are never drafts', () => {
  assert.equal(isSoDraft(null), false)
  assert.equal(isSoDraft(undefined), false)
  assert.equal(isSoDraft({}), false)
  assert.equal(isSoDraft({ status: 'FOR_OC_APPROVAL' }), false)
  assert.equal(isSoDraft({ status: 'FOR_OC_APPROVAL', soNumber: '   ', soFilePath: '  ' }), false)
})

test('the draft file label prefers the display name and falls back to the stored path', () => {
  assert.equal(
    soDraftFileLabel({ soFileName: 'special-order.pdf', soFilePath: 'special-orders/x.pdf' }),
    'special-order.pdf',
  )
  assert.equal(soDraftFileLabel({ soFilePath: 'special-orders\\x.pdf' }), 'x.pdf')
  assert.equal(soDraftFileLabel({ soFilePath: 'special-orders/x.pdf' }), 'x.pdf')
  assert.equal(soDraftFileLabel({}), '')
  assert.equal(soDraftFileLabel(null), '')
})

test('the snapshot ignores whitespace-only edits but reports real ones', () => {
  const base = draftSnapshot({
    soNumber: 'SO-2026-001',
    needsBudget: false,
    chancellorNotes: 'Note',
    fileName: 'so.pdf',
  })

  assert.equal(
    base,
    draftSnapshot({
      soNumber: '  SO-2026-001 ',
      needsBudget: false,
      chancellorNotes: ' Note ',
      fileName: 'so.pdf',
    }),
    'whitespace alone is not an edit',
  )
  assert.notEqual(base, draftSnapshot({ soNumber: 'SO-2026-002', needsBudget: false }))
  assert.notEqual(base, draftSnapshot({ soNumber: 'SO-2026-001', needsBudget: true }))
  assert.notEqual(base, draftSnapshot({ soNumber: 'SO-2026-001', needsBudget: false, chancellorNotes: 'Changed' }))
  assert.notEqual(base, draftSnapshot({ soNumber: 'SO-2026-001', needsBudget: false, fileName: 'new.pdf' }))
})

test('the snapshot keeps a false budget choice distinct from an unset one', () => {
  assert.notEqual(
    draftSnapshot({ needsBudget: false }),
    draftSnapshot({ needsBudget: null }),
    '"Without Budget" is a real choice and must count as a change',
  )
})
