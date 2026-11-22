/**
 * proposalFormRevisions.test.js
 * ---------------------------------------------------------------------------
 * Guards the Create Proposal revisions as static wiring:
 *   - section 01 no longer shows a District;
 *   - sites use the section-01 cascade through one reusable row component;
 *   - required labels carry the asterisk and the three optional ones do not;
 *   - the budget is optional;
 *   - the incomplete-section banner navigates to each section.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const STEP1 = read('../views/Proponent/ProponentSubmitProp.vue')
const STEP3 = read('../views/Proponent/ProponentSubmitProp3.vue')
const OWNER = read('../views/Proponent/ProponentProposals.vue')
const SITE_ROW = read('../components/SiteLocationRow.vue')
const CASCADE = read('../composables/useAddressCascade.js')
const LOCATIONS = read('philippineLocations.js')

test('section 01 no longer renders the District control', () => {
  assert.ok(!STEP1.includes('id="address-district"'))
  assert.ok(!STEP1.includes("addressCascade.select('district'"))
})

test('section 01 uses the cascade without a district-gated city list', () => {
  assert.ok(STEP1.includes('useAddressCascade(addressDetails, { districtRequired: false })'))
  assert.ok(CASCADE.includes('districtRequired = true'))
  assert.ok(CASCADE.includes('districtRequired && districtOptions.value.length > 0'))
  assert.ok(!LOCATIONS.includes("return 'Please select a district.'"))
})

test('sites reuse the same cascade through the one row component', () => {
  assert.ok(STEP1.includes("import SiteLocationRow from '@/components/SiteLocationRow.vue'"))
  assert.ok(STEP1.includes('<SiteLocationRow'))
  assert.ok(!STEP1.includes('v-model="site.country"'), 'the raw text inputs are gone')
  assert.ok(SITE_ROW.includes('useAddressCascade(model, { districtRequired: false })'))
  assert.ok(SITE_ROW.includes("cascade.select('country'"))
  assert.ok(SITE_ROW.includes("cascade.select('region'"))
  assert.ok(SITE_ROW.includes("cascade.select('province'"))
  assert.ok(SITE_ROW.includes("cascade.select('district'"))
  assert.ok(SITE_ROW.includes("cascade.select('municipality'"))
  assert.ok(SITE_ROW.includes("cascade.select('barangay'"))
})

test('the site dropdowns wait for their parent and the district is manual', () => {
  assert.match(SITE_ROW, /:disabled="!countryChosen \|\| !cascade\.regionEnabled"/)
  assert.match(SITE_ROW, /:disabled="!countryChosen \|\| !cascade\.provinceEnabled"/)
  assert.match(SITE_ROW, /:disabled="!countryChosen \|\| !cascade\.cityEnabled"/)
  assert.match(SITE_ROW, /:disabled="!countryChosen \|\| !cascade\.barangayEnabled"/)
  assert.ok(SITE_ROW.includes(':list="districtListId"'), 'the district is a typed input with suggestions')
  assert.ok(SITE_ROW.includes('<datalist'))
  assert.ok(SITE_ROW.includes('districtOptions'), 'the suggestions are the existing PSGC districts')
})

test('District is optional in the sites section and on the review step', () => {
  // The row never reports a missing district and the municipality is offered without
  // one, including in NCR where the PSGC has districts.
  assert.match(SITE_ROW, /useAddressCascade\(model, \{ districtRequired: false \}\)/)
  assert.ok(!SITE_ROW.includes("return 'type or select a district.'"))
  assert.ok(!SITE_ROW.includes("return 'choose a district from the suggestions.'"))
  assert.ok(!SITE_ROW.includes('Type or select a district first'))
  assert.ok(SITE_ROW.includes('district (optional)'))
  // Final review: district is not in the site completeness list any more.
  assert.ok(!STEP3.includes("'country', 'region', 'province', 'district', 'municipality', 'barangay'"))
  assert.ok(STEP3.includes("['country', 'region', 'municipality', 'barangay']"))
  assert.ok(STEP3.includes('siteNeedsProvince(site) && isBlank(site.province)'))
})

test('the six site fields appear in the requested order', () => {
  const labels = ['country', 'region', 'province', 'district', 'municipality', 'barangay']
  const positions = labels.map((field) => SITE_ROW.indexOf(`\`Site \${siteNumber} ${field === 'municipality' ? 'municipality or city' : field}\``))
  positions.forEach((position) => assert.ok(position >= 0, 'each control has an accessible label'))
  assert.deepEqual([...positions].sort((a, b) => a - b), positions, 'the controls are in the requested order')
})

test('the three optional fields are not marked with an asterisk', () => {
  assert.ok(STEP1.includes('<label>Program Title</label>'))
  assert.ok(STEP1.includes('<label>Number of other projects:</label>'))
  assert.ok(STEP1.includes('<span class="section-sub">(optional)</span>'))
  assert.ok(!STEP1.includes('Program Title <span class="req">'))
})

test('required sections carry a visible asterisk', () => {
  for (const heading of [
    'Sites of Implementation',
    'Type of Research',
    'Priority Agenda',
    'Innovation Goals',
    'Sector Relevance',
    'Sustainable Development Goals (SDG)',
    'Executive Summary',
    'Review of Literature',
    'Methodology',
    'Expected Outputs (6Ps)',
    'Potential Outcomes',
    'Target Beneficiaries',
    'Sustainability Plan',
    'Gender and Development (GAD) Score',
    'Limitations of the Project',
    'Risk Management Plan',
    'Logical Framework',
    'Literature Cited',
    'Personnel Requirement',
  ]) {
    assert.match(
      STEP1,
      new RegExp(`<h3>${heading.replace(/[()]/g, '\\$&')} <span class="req">\\*</span></h3>`),
      `${heading} is required`,
    )
  }
})

test('the budget is optional in both the details and review steps', () => {
  assert.ok(!STEP1.includes("errors.line_item_budget_file = 'Line-Item Budget file is required.'"))
  assert.ok(!STEP3.includes("errs.push('Line-Item Budget file is missing.')"))
  assert.ok(STEP1.includes('Line-Item Budget Requirement'))
  assert.ok(STEP3.includes('beneficiariesFileName'))
})

test('incomplete sections are listed, clickable and navigable', () => {
  assert.ok(STEP1.includes('groupErrorsBySection(errorList.value, sectionRefs)'))
  assert.ok(STEP1.includes('incompleteSections.length > 0'))
  assert.ok(STEP1.includes('validation-section-link'))
  assert.ok(STEP1.includes('@click="goToError(group.errors[0])"'))
  assert.ok(STEP1.includes('@click.prevent="goToError(err)"'))
  assert.ok(STEP1.includes('showIncompleteSections'))
  assert.ok(STEP1.includes('validation-highlight'))
  // The click waits for the activated page, scrolls the modal's own container (not
  // the page behind it) and marks every incomplete site row named by the message.
  assert.ok(STEP1.includes('body.scrollTo({ top:'))
  assert.ok(STEP1.includes('requestAnimationFrame'))
  assert.ok(STEP1.includes("matchAll(/Site (\\d+):/g)"))
  // Each incomplete site row gets its own message under the row, not only the
  // combined sentence at the bottom of the section.
  assert.ok(STEP1.includes('siteRowErrors[index]'))
  assert.ok(STEP1.includes('class="site-error-row"'))
  // The banner revalidates as the form changes, so a completed field loses its error.
  assert.match(STEP1, /watch\(proposal,[\s\S]*?validateStep1\(\)[\s\S]*?\{ deep: true, flush: 'post' \}\)/)
  assert.ok(OWNER.includes('@reviewMissing="reviewMissingSections"'))
  assert.ok(OWNER.includes('step1Ref.value?.showIncompleteSections?.()'))
  assert.ok(STEP3.includes("emit('reviewMissing')"))
})
