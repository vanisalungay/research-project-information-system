/**
 * draftPersistence.test.js
 * ---------------------------------------------------------------------------
 * Guards the create-proposal draft flow end to end, at the source level:
 *
 *  - a draft saved from any step stores everything typed so far, including files that
 *    were already stored (an empty file input never deletes one);
 *  - the one save path creates the draft first, uploads the attachments, then writes the
 *    requested status - so a submission snapshot sees the uploaded paths, and a repeated
 *    save updates the same draft instead of creating a duplicate;
 *  - the GAD checklist is sent as structured data (not a required file) and the budget /
 *    certification validation accepts what a reopened draft already stores;
 *  - the reviewer and detail views show the literature text, the logical framework, the
 *    budget document and the certification instead of reporting a missing file.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const STEP1 = read('../views/Proponent/ProponentSubmitProp.vue')
const STEP2 = read('../views/Proponent/ProponentSubmitProp2.vue')
const STEP3 = read('../views/Proponent/ProponentSubmitProp3.vue')
const WORKFLOW = read('../views/Proponent/ProponentProposals.vue')
const DETAIL = read('../views/Proponent/ProponentDetiailedProp.vue')
const OVC = read('../views/Ovc/OvcReviewProposal.vue')
const REGISTRY = read('./proposalFields.js')
const FIELD_SECTION = read('../components/ProposalFieldSection.vue')

// --------------------------------------------------------------- Proposal Details

test('the details step emits its whole form for the workflow to save', () => {
  assert.ok(STEP1.includes("emit('savedraft', { ...proposal })"),
    'Save as Draft must hand the current form to the workflow')
  assert.ok(!STEP1.includes('mapFormToDTO'), 'the details step no longer builds its own payload')
  assert.ok(STEP1.includes('getFormData: () => ({ ...proposal })'),
    'the workflow can read the current form when jumping steps')
})

test('the details step restores the stored file names and the GAD saved flag', () => {
  assert.ok(STEP1.includes('review_file_name: \'\''), 'the initial form carries the review name')
  assert.ok(STEP1.includes("proposal.review_file_name = data.reviewFileName || ''"),
    'a reopened draft restores the legacy literature document name')
  assert.ok(STEP1.includes('proposal.roadmap_file_name = data.roadmapFileName ||'),
    'a reopened draft restores the technology map name')
  assert.ok(STEP1.includes('proposal.roadmap_file_path = data.roadmapFilePath ||'),
    'a reopened draft restores the technology map path')
  assert.ok(STEP1.includes('proposal.gad_checklist_saved = Boolean(data.gadChecklist)'),
    'a stored checklist is marked as saved')
})

test('the details step keeps stored documents visible and downloadable', () => {
  assert.ok(STEP1.includes('previewUploadedFile'), 'a saved technology map can be viewed')
  assert.ok(STEP1.includes('downloadUploadedFile'), 'a saved budget can be downloaded')
  assert.ok(STEP1.includes('proposal.roadmap_file_path || proposal.roadmap_file_name'),
    'an empty file input does not hide the stored map')
  assert.ok(STEP1.includes('proposal.budget_file_path || proposal.budget_file_name'),
    'an empty file input does not hide the stored budget')
})

test('the details budget validation accepts a file the draft already stores', () => {
  assert.ok(STEP1.includes(
    '!proposal.line_item_budget_file && !proposal.budget_file_path && !proposal.budget_file_name'
  ), 'a reopened draft is not asked for its budget file again')
})

// --------------------------------------------------------------- Certification

test('the certification step saves a draft through the workflow', () => {
  assert.ok(STEP2.includes("const saveDraft = () => emit('savedraft', getCertificationData())"),
    'Save as Draft is no longer a no-op')
  assert.ok(STEP2.includes("'savedraft'"))
  assert.ok(STEP2.includes('getCertificationData'), 'the certification data travels with the save')
})

test('the certification step stays mounted so typed values survive step navigation', () => {
  assert.ok(
    WORKFLOW.includes('<ProponentSubmitProp2 ref="step2Ref" :open="showModal2"'),
    'the step is hidden/opened by its prop, never unmounted (an unmount discarded the typed certification)',
  )
  assert.ok(
    !WORKFLOW.includes('ref="step2Ref" v-if="showModal2"'),
    'the old v-if destroyed Submitted By / Endorsed By when going back',
  )
})

test('a draft saved from any step reads both steps, not the last emitted snapshot', () => {
  const start = WORKFLOW.indexOf('const saveDraftFromAnyStep = async () => {')
  assert.ok(start >= 0, 'the one draft save path exists')
  const end = WORKFLOW.indexOf('\nconst ', start + 1)
  const body = WORKFLOW.slice(start, end === -1 ? WORKFLOW.length : end)

  assert.ok(body.includes('mergeStep1Form(step1Ref.value?.getFormData?.())'),
    'the details form is always merged before saving')
  assert.ok(body.includes('mergeCertification(step2Ref.value?.getCertificationData?.())'),
    'the certification (Submitted By / Endorsed By, signature, endorsement PDF) is always merged')
})

test('reopening a draft restores the Submitted By date from the key the parent supplies', () => {
  assert.ok(STEP2.includes("fill('submitted_date', saved.submittedByDate || saved.submittedDate)"),
    'the date must be read from submittedByDate (the saved key), not the old typo')
})

test('saving a draft never runs the final-submission signature checks', () => {
  const saveStart = STEP2.indexOf('const saveDraft = () =>')
  const saveEnd = STEP2.indexOf('\nconst submitForm', saveStart)
  const saveBody = STEP2.slice(saveStart, saveEnd === -1 ? STEP2.length : saveEnd)
  assert.ok(saveStart >= 0, 'the draft save exists')
  assert.ok(!saveBody.includes('validateStep2'),
    'a draft is saved without the required-field/signature check')

  const submitStart = STEP2.indexOf('const submitForm = async () => {')
  assert.ok(submitStart >= 0, 'the final submission path exists')
  assert.ok(STEP2.slice(submitStart).includes('validateStep2()'),
    'final submission still validates the required fields')
  assert.ok(STEP2.includes("errors.submitted_signature = 'Signature file (Submitted By) is required.'"),
    'a genuinely missing signature still blocks submission with a specific message')
  assert.ok(STEP2.includes('if (!uploadedFiles.endorsed && !hasSavedEndorsement.value)'),
    'the endorsement PDF requirement is still enforced on submission')

  const draftStart = STEP3.indexOf('async function saveDraft() {')
  const draftEnd = STEP3.indexOf('\ndefineExpose', draftStart)
  assert.ok(draftStart >= 0 && !STEP3.slice(draftStart, draftEnd).includes('validationErrors'),
    'the review step does not block a draft on final-submission requirements')
})

test('a stored signature is detected by its path or its file name', () => {
  assert.ok(STEP2.includes('props.savedSubmittedSignatureFilePath || \'\''),
    'the stored signature path is checked')
  assert.ok(STEP2.includes('props.savedSubmittedSignatureFileName || \'\''),
    'a legacy record with only a file name still counts as signed')
  assert.ok(STEP2.includes('props.savedEndorsementFilePath || \'\''),
    'the stored endorsement path is checked')
  assert.ok(STEP2.includes('props.savedEndorsementFileName || \'\''),
    'a legacy record with only an endorsement name still counts as attached')
})

test('the certification step restores and accepts the stored submitted signature', () => {
  assert.ok(STEP2.includes('hasSavedSubmittedSignature'), 'a stored signature is detected')
  assert.ok(STEP2.includes('!uploadedFiles.submitted && !hasSavedSubmittedSignature.value'),
    'validation accepts a signature the draft already stores')
  assert.ok(STEP2.includes('v-else-if="hasSavedSubmittedSignature"'),
    'a stored signature is shown instead of an empty input')
  assert.ok(STEP2.includes('previewUploadedFile(submittedSignaturePath)'),
    'the stored signature can be viewed')
  assert.ok(STEP2.includes('downloadUploadedFile(submittedSignaturePath)'),
    'the stored signature can be downloaded')
  assert.ok(STEP2.includes('applySavedCertification()'),
    'the certification fields restore when a draft is reopened')
})

// --------------------------------------------------------------- Review & Submit

test('the review payload carries the checklist, the stored names and the certification', () => {
  assert.ok(STEP3.includes('gadChecklistPayloadForSave(data.gad_checklist, data.gad_checklist_saved)'),
    'the checklist is sent as structured data')
  assert.ok(STEP3.includes('gadFileName: data.gad_file_name || null'),
    'a legacy GAD document name is preserved, never invented')
  assert.ok(STEP3.includes('reviewFileName: data.review_file_name || null'))
  assert.ok(STEP3.includes('roadmapFileName: data.roadmap_file_name || data.technology_roadmap_file?.name || null'))
  assert.ok(STEP3.includes('beneficiariesFileName: data.budget_file_name || data.line_item_budget_file?.name || null'))
  assert.ok(
    STEP3.includes('certificationValue(certification.submitted_name, data.submittedByName)'),
    'a typed name wins, otherwise the reopened draft stored value is preserved',
  )
  assert.ok(
    STEP3.includes('certificationValue(certification.endorsed_name, data.endorsedByName)'),
    'the endorsed name travels with the same fallback',
  )
  assert.ok(
    STEP3.includes('submittedByDate: certificationValue(certification.submitted_date, data.submittedByDate)'),
    'the Submitted By date is saved with the same fallback',
  )
})

test('the review step creates the draft once and then updates it', () => {
  assert.ok(STEP3.includes('data.proposal_id = proposalId'),
    'the id of the created draft is kept, so a second save updates it')
  assert.ok(STEP3.includes("if (!proposalId)"))
})

test('the attachments are uploaded before the final status is written', () => {
  const create = STEP3.indexOf("api.post(\n      '/api/proposals'")
  const createFallback = STEP3.indexOf("'/api/proposals',")
  const uploadMap = STEP3.indexOf('uploadTechnologyMapIfNeeded')
  const uploadSignature = STEP3.indexOf('uploadSubmittedSignatureIfNeeded')
  const finalPut = STEP3.indexOf('api.put(`/api/proposals/${proposalId}`')

  assert.ok(create >= 0 || createFallback >= 0, 'the draft is created first')
  assert.ok(uploadMap >= 0 && uploadSignature >= 0, 'every attachment is uploaded')
  assert.ok(finalPut > uploadMap && finalPut > uploadSignature,
    'the status is written after the uploads, so the submission snapshot sees them')
  assert.ok(STEP3.includes('uploadSubmittedSignature(proposalId, file)'),
    'the submitted signature has its own upload endpoint')
})

test('the review validation uses the checklist, not a GAD file, and accepts stored files', () => {
  assert.ok(STEP3.includes('gadChecklistValidation(d.gad_checklist)'),
    'the checklist is what makes the GAD section complete')
  assert.ok(!STEP3.includes('if (!d.gad_score_file)'),
    'a legacy GAD file is no longer required')
  assert.ok(STEP3.includes('!d.line_item_budget_file && !d.budget_file_path && !d.budget_file_name'),
    'a budget the draft already stores satisfies the requirement')
  assert.ok(
    STEP3.includes('!signatureFiles.submitted && !d.submittedByFilePath && !d.submittedByFileName'),
    'a stored signature (path or legacy file name) satisfies the certification requirement',
  )
  assert.ok(
    STEP3.includes('certificationValue(cert.submitted_name, d.submittedByName)'),
    'final validation accepts the certification a reopened draft already stores',
  )
})

test('an empty cooperating-agency snapshot preserves the stored selection', () => {
  assert.ok(STEP3.includes('cooperatingAgencyIds: agencyIds.length ? agencyIds : null'),
    'the checklist loads asynchronously, so an empty snapshot must mean "preserve"')
})

// --------------------------------------------------------------- the workflow

test('the workflow binds Save as Draft on the steps that offer it', () => {
  assert.ok(WORKFLOW.includes('@savedraft="handleStep1SaveDraft"'))
  assert.ok(WORKFLOW.includes('@savedraft="handleStep2SaveDraft"'))
  assert.ok(WORKFLOW.includes('step3Ref.value?.saveDraft?.()'),
    'both steps are saved through the one Review & Submit save path')
  assert.ok(WORKFLOW.includes('mergeCertification(certificationData)'),
    'the certification data is merged before the save')
})

test('jumping steps hydrates the shared snapshot so stored rows are not cleared', () => {
  assert.ok(WORKFLOW.includes('...(step1Ref.value?.getFormData?.() || {})'),
    'the details form is merged into the snapshot')
  assert.ok(WORKFLOW.includes('mergeCertification(step2Ref.value?.getCertificationData?.())'),
    'the certification step is merged when jumping to Review & Submit')
})

test('reopening a draft restores the certification and the stored signature', () => {
  assert.ok(WORKFLOW.includes('submittedByName: loaded.submittedByName ||'),
    'the certification name restores')
  assert.ok(WORKFLOW.includes('submittedByFilePath: loaded.submittedByFilePath ||'),
    'the stored signature restores')
  assert.ok(WORKFLOW.includes(':saved-certification="savedCertificationForStep2"'),
    'the certification fields are handed back to the step')
  assert.ok(WORKFLOW.includes(':saved-submitted-signature-file-path='),
    'the stored signature path is handed back to the step')
})

// --------------------------------------------------------------- the registry

test('the field registry treats the literature and GAD document as optional', () => {
  assert.ok(REGISTRY.includes(
    "key: 'reviewFileName', label: 'Review of Literature (legacy document)'"
  ))
  assert.ok(REGISTRY.includes("key: 'gadChecklist', label: 'GAD Checklist 2'"),
    'the interactive checklist is registered')
  assert.ok(REGISTRY.includes(
    "key: 'gadFileName', label: 'GAD Score (legacy document)'"
  ))
  assert.ok(!REGISTRY.includes("label: 'Review of Literature', form: 'review_of_literature_file', kind: 'file'"),
    'Section 11 is no longer a mandatory file')
})

test('the field registry registers the certification and endorsement fields', () => {
  for (const key of [
    'submittedByName', 'submittedByDesignation', 'submittedByDate', 'submittedByFileName',
    'endorsedByName', 'endorsedByDesignation', 'endorsedByDate', 'endorsementFileName',
  ]) {
    assert.ok(REGISTRY.includes(`key: '${key}'`), `section 28 must register ${key}`)
  }
})

test('the shared field section renders the checklist and hides missing optional files', () => {
  assert.ok(FIELD_SECTION.includes("field.kind === 'gadChecklist'"))
  assert.ok(FIELD_SECTION.includes('<GadChecklist :model-value="gadValue" readonly />'))
  assert.ok(FIELD_SECTION.includes("field.kind === 'optionalFile'"))
  // Only the genuine mandatory `file` branch still reports a missing upload.
  const missing = FIELD_SECTION.indexOf('No file uploaded.')
  const optional = FIELD_SECTION.indexOf("field.kind === 'optionalFile'")
  assert.ok(missing >= 0 && optional > missing,
    'the empty-file message belongs to the mandatory file branch only')
})

// --------------------------------------------------------------- detail / review

test('a stored budget document is named even when only its path survived', () => {
  assert.ok(STEP1.includes('budgetDocumentNameFromPath(proposal.budget_file_path)'),
    'the details step names a stored budget whose display name was lost')
  assert.ok(DETAIL.includes('budgetDocumentName(proposal)'),
    'the detail view names the attached budget instead of leaving the line blank')
  assert.ok(REGISTRY.includes('budgetDocumentName(proposal)'),
    'the reviewer field registry resolves the budget document through the same rule')
  assert.ok(REGISTRY.includes("if (key === 'beneficiariesFileName')"),
    'the registry special-cases only the budget document field')
})

test('the detail view shows the literature text and no false missing-file message', () => {
  assert.ok(DETAIL.includes("proposal.review || 'Not provided'"),
    'Review of Literature is text')
  assert.ok(DETAIL.includes("field-key=\"'review'\""), 'the text has its own remark control')
})

test('the detail view renders the logical framework, budget and certification', () => {
  assert.ok(DETAIL.includes('proposal.logFrames && proposal.logFrames.length > 0'),
    'the guard matches the collection it iterates')
  assert.ok(!DETAIL.includes('proposal.budgetFileName'),
    'the non-existent budgetFileName column is gone')
  assert.ok(DETAIL.includes('proposal.beneficiariesFilePath || proposal.beneficiariesFileName'),
    'the budget downloads from the stored path')
  assert.ok(DETAIL.includes('hasSubmittedByData'), 'the Submitted By block is shown')
  assert.ok(DETAIL.includes('hasEndorsedByData'), 'the Endorsed By block is shown')
  assert.ok(DETAIL.includes('proposal.submittedByFilePath || proposal.submittedByFileName'),
    'the submitted signature can be downloaded')
  assert.ok(DETAIL.includes('proposal.endorsementFilePath || proposal.endorsementFileName'),
    'the endorsement PDF can be downloaded')
})

test('the OVC review view uses the real budget fields and shows the new documents', () => {
  assert.ok(!OVC.includes('proposal.budgetFileName'))
  assert.ok(!OVC.includes('proposal.budgetFilePath'))
  assert.ok(OVC.includes('proposal.beneficiariesFilePath || proposal.beneficiariesFileName'),
    'the reviewer downloads the line-item budget')
  assert.ok(OVC.includes('proposal.submittedByFilePath || proposal.submittedByFileName'),
    'the reviewer can download the submitted signature')
  assert.ok(OVC.includes('proposal.endorsementFilePath || proposal.endorsementFileName'),
    'the reviewer can download the endorsement PDF')
})
