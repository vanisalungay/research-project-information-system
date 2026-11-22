/**
 * gadSection19.test.js
 * ---------------------------------------------------------------------------
 * Guards how Section 19 is wired: the Proponent form uses the interactive GAD Checklist 2
 * (no file upload), the payload and the submission validation use the checklist helpers, and
 * the reviewer / detail views render the completed checklist read-only. Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const FORM = read('../views/Proponent/ProponentSubmitProp.vue')
const CHECKLIST = read('../components/GadChecklist.vue')
const CRITERION = read('../components/GadCriterionCard.vue')
const QUESTION = read('../components/GadQuestionRow.vue')
const DETAIL = read('../views/Proponent/ProponentDetiailedProp.vue')
const REVIEW = read('../views/Ovc/OvcReviewProposal.vue')

test('the Proponent form uses the interactive checklist instead of a file upload', () => {
  assert.ok(FORM.includes("import GadChecklist from '@/components/GadChecklist.vue'"))
  assert.ok(FORM.includes('v-model="proposal.gad_checklist"'), 'the checklist is bound to the form')
  assert.ok(FORM.includes('gad_checklist: \'section_gad_score\''), 'an error routes to section 19')
  assert.ok(!FORM.includes('Upload GAD score document'), 'the GAD file upload is gone')
  assert.ok(!FORM.includes("handleFileUpload('gad'"), 'the GAD file handler is no longer used')
})

test('the form sends and validates the checklist', () => {
  assert.ok(FORM.includes('gadChecklistPayload('), 'the payload is built from the checklist')
  assert.ok(FORM.includes('gadChecklistValidation('), 'submission requires every item answered')
  assert.ok(FORM.includes('gadChecklistFromSaved('), 'a reopened draft restores the checklist')
  assert.ok(FORM.includes('gadChecklist: gadChecklistPayload(d.gad_checklist)'), 'it travels in the DTO')
})

test('the checklist component renders every item with a response, a score and a comment', () => {
  assert.ok(CHECKLIST.includes('GAD_CHECKLIST_TITLE'), 'it keeps the official title')
  assert.ok(CHECKLIST.includes('GAD_CRITERIA'), 'it renders every criterion')
  assert.ok(CHECKLIST.includes('GadCriterionCard'), 'each category has its own card')
  assert.ok(CHECKLIST.includes('model.responses'), 'the No/Partly/Yes state')
  assert.ok(CHECKLIST.includes('model.comments'), 'a comment per item')
  assert.ok(CHECKLIST.includes('GadInterpretationTable'), 'it shows the interpretation table')

  assert.ok(CRITERION.includes('GadQuestionRow'), 'every criterion renders its questions')
  assert.ok(CRITERION.includes(':score="subtotal"'), 'the card receives its subtotal')

  assert.ok(QUESTION.includes('GAD_RESPONSE_OPTIONS'), 'the three answers come from the shared config')
  assert.ok(QUESTION.includes('gadItemMaxScore'), 'the maximum comes from the item scale')
  assert.ok(QUESTION.includes("emit('update:comment'"), 'each question writes its own comment')
  assert.ok(QUESTION.includes('Comments / gender issues identified'), 'the comments label')
  assert.ok(QUESTION.includes('Add a short note...'), 'the comments placeholder')
  assert.ok(QUESTION.includes("emit('clear')"), 'the Clear action')
})

test('the reviewer and detail views show the completed checklist read-only', () => {
  for (const [name, source] of [['detail', DETAIL], ['review', REVIEW]]) {
    assert.ok(source.includes('<GadChecklist'), `${name} renders the checklist`)
    assert.ok(source.includes('readonly'), `${name} renders it read-only`)
  }
})
