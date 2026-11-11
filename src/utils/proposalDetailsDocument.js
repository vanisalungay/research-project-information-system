/**
 * proposalDetailsDocument.js
 * ---------------------------------------------------------------------------
 * The single, role-neutral "Download Proposal Details" document used by every
 * authorized screen (Office of the Chancellor, OVCRIGE, OVCAF, RPS and the
 * Proponent's own proposal).
 *
 * It is a cover/summary page followed by the official Full-Blown R&D Proposal
 * form, generated from the proposal record at click time:
 *
 *   1. identification and proponent information
 *   2. research details
 *   3. a readable digest of the proposal description
 *   4. evaluation and approval / Special Order details
 *   5. the current status
 *   6. the official form, rendered by the existing renderer
 *
 * The download pipeline is shared end to end: this module only composes HTML and
 * hands it to savePdfDocument(), which the backend renders to a PDF. No server
 * endpoint composes a document and no value is hardcoded - every field comes from
 * the proposal, its reviews or its remarks. Nothing in the document identifies the
 * role of the person who downloaded it.
 */

import sealImage from '@/assets/images/msu-naawan-seal.jpg?inline'
import api from '@/utils/api'
import { savePdfDocument } from '@/utils/documentExport'
import { OFFICIAL_FORM_STYLES, renderOfficialProposalForm } from '@/utils/officialProposalForm'
import { proposalStatusLabel } from '@/utils/proposalStatus'
import { isSoDraft, soDraftFileLabel } from '@/utils/soDraft'
import { REVIEW_GROUPS, groupAverage, reviewWeightedScore } from '@/utils/reviewScores'

/* ---------- escaping (same approach as officialProposalForm.js) ---------- */

function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function text(value) {
  return value == null ? '' : String(value).trim()
}

/** Multi-line safe text; an empty value renders as a dash. */
function val(value, empty = '&mdash;') {
  const content = text(value)
  if (!content) return empty
  return esc(content).replace(/\r?\n/g, '<br />')
}

/** ISO dates are shown in a readable form; anything else is passed through. */
function dateVal(value, empty = '&mdash;') {
  const content = text(value)
  if (!content) return empty
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(content)
  if (!m) return esc(content)
  const months = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ]
  return `${months[Number(m[2]) - 1] || m[2]} ${Number(m[3])}, ${m[1]}`
}

function list(value) {
  if (Array.isArray(value)) return value
  if (value && typeof value === 'object') return Object.values(value)
  return []
}

/* ---------- evaluation scores ---------- */

function score(value) {
  return value == null ? '&mdash;' : value.toFixed(2)
}

/** The readable label of a review's reviewer (never a computed display name). */
function reviewerName(review) {
  return text(review?.reviewer?.name) || 'Reviewer'
}

function reviewerRole(review) {
  return text(review?.reviewerRole) || text(review?.reviewer?.role) || ''
}

/* ---------- cover markup ---------- */

function row(label, value) {
  return `<tr><th>${esc(label)}</th><td>${value}</td></tr>`
}

function coverTable(rows) {
  return `<table class="oc-table"><colgroup><col class="oc-col-label" /><col /></colgroup><tbody>${rows.join('')}</tbody></table>`
}

function sectionTitle(title) {
  return `<h2 class="oc-section">${esc(title)}</h2>`
}

function coverHeader(proposal) {
  return `
<header class="oc-header">
  <table class="oc-header-table">
    <colgroup><col class="oc-seal-col" /><col /><col class="oc-id-col" /></colgroup>
    <tbody>
      <tr>
        <td class="oc-seal-cell" rowspan="3">
          <img class="oc-seal" src="${sealImage}" alt="Mindanao State University at Naawan seal" />
        </td>
        <td class="oc-inst">MINDANAO STATE UNIVERSITY AT NAAWAN</td>
        <td class="oc-doc-id">
          <!-- The document carries no download timestamp, URL or route: those are browser
               metadata, not part of the official record. -->
          <div>Document No.: <strong>${val(proposal.documentId)}</strong></div>
        </td>
      </tr>
      <tr><td class="oc-office">RESEARCH PROJECT INFORMATION SYSTEM</td></tr>
      <tr><td class="oc-title">PROPOSAL DETAILS</td></tr>
    </tbody>
  </table>
  <p class="oc-generated-note">
    Generated from the saved proposal record. The official Full-Blown R&amp;D Project
    Proposal form follows this summary.
  </p>
</header>`
}

function identificationSection(proposal) {
  const revision =
    proposal.revisionNumber === 0 || proposal.revisionNumber ? String(proposal.revisionNumber) : ''
  return `
${sectionTitle('1. Project and Proponent Information')}
${coverTable([
  row('Project Title', val(proposal.projectTitle)),
  row('Program Title', val(proposal.programTitle)),
  row('Proposal Code', val(proposal.proposalCode)),
  row('Document ID', val(proposal.documentId)),
  row('Revision', revision ? esc(revision) : '&mdash;'),
  row('Current Status', val(proposalStatusLabel(proposal.status))),
  row('Project Leader', val(proposal.projectLeader || proposal.proponent?.name)),
  row('Sex', val(proposal.projectLeaderSex)),
  row('College / Department', val(proposal.college || proposal.proponent?.departmentOffice)),
  row('Contact', val(proposal.contactValue || proposal.proponent?.contactNumber)),
  row('Email', val(proposal.proponent?.email)),
  row('Address', val(proposal.address)),
])}`
}

function researchSection(proposal) {
  const agendas = list(proposal.priorityAgendas)
    .filter((row) => row && row.selected)
    .map((row) => {
      const label = text(row.agendaKey || row.agenda)
      return label ? `${esc(label)}${row.value ? `: ${esc(text(row.value))}` : ''}` : ''
    })
    .filter(Boolean)
    .join('; ')

  const sites = list(proposal.sites)
    .map((site) => {
      const parts = [site.barangay, site.municipality, site.district, site.province, site.region, site.country]
        .map(text)
        .filter(Boolean)
      return parts.length ? esc(parts.join(', ')) : ''
    })
    .filter(Boolean)
    .join('<br />')

  const personnel = list(proposal.personnel)
    .map((row) => {
      const parts = [text(row.position), text(row.responsibilities)].filter(Boolean).map(esc)
      return parts.length ? `&bull; ${parts.join(' &mdash; ')}` : ''
    })
    .filter(Boolean)
    .join('<br />')

  const otherProjects = list(proposal.otherProjects)
    .map((row) => {
      const parts = [text(row.title), text(row.agency), text(row.involvement)].filter(Boolean).map(esc)
      return parts.length ? `&bull; ${parts.join(' &mdash; ')}` : ''
    })
    .filter(Boolean)
    .join('<br />')

  return `
${sectionTitle('2. Research Details')}
${coverTable([
  row('Type of Research', val(proposal.researchType)),
  row('Duration', text(proposal.duration) ? `${esc(text(proposal.duration))} month(s)` : '&mdash;'),
  row('Start Date', dateVal(proposal.startDate)),
  row('End Date', dateVal(proposal.endDate)),
  row('Cooperating Agencies', val(proposal.cooperatingAgencies)),
  row('Implementation Sites', sites || '&mdash;'),
  row('Priority Agendas', agendas || '&mdash;'),
  row('Sustainable Development Goal', val(Array.isArray(proposal.sdg) ? proposal.sdg.join('; ') : proposal.sdg)),
  row('Target TRL', val(proposal.technologyTrl)),
  row('GAD Score', val(proposal.gadScore)),
  row('Target Beneficiaries', val(proposal.beneficiaries)),
  row('Personnel', personnel || '&mdash;'),
  row('Other Ongoing Projects', otherProjects || '&mdash;'),
])}`
}

function descriptionSection(proposal) {
  return `
${sectionTitle('3. Proposal Description')}
<div class="oc-block"><h3>Executive Summary</h3><p>${val(proposal.executiveSummary)}</p></div>
<div class="oc-block"><h3>Rationale / Significance</h3><p>${val(proposal.rationale)}</p></div>
<div class="oc-block"><h3>Objectives</h3>
  <p><strong>General:</strong> ${val(proposal.objectivesGeneral)}</p>
  <p><strong>Specific:</strong> ${val(proposal.objectivesSpecific)}</p>
</div>
<div class="oc-block"><h3>Methodology</h3><p>${val(proposal.methodology)}</p></div>`
}

function reviewsHtml(reviews) {
  const rows = list(reviews)
  if (!rows.length) {
    return '<p class="oc-empty">No evaluation reviews recorded.</p>'
  }
  return rows
    .map((review) => {
      const groupRows = REVIEW_GROUPS.map((group) => {
        const average = groupAverage(review, group.fields)
        return `<tr><td>${esc(group.label)}</td><td class="oc-center">${group.weight}%</td><td class="oc-center">${score(average)} / 5</td></tr>`
      }).join('')
      const overall = reviewWeightedScore(review)
      return `
<div class="oc-review">
  <table class="oc-review-head"><tbody><tr>
    <th>Reviewer</th><td>${val(reviewerName(review))}</td>
    <th>Role</th><td>${val(reviewerRole(review))}</td>
    <th>Decision</th><td>${val(review.decision)}</td>
    <th>Date</th><td>${dateVal(review.createdAt)}</td>
  </tr></tbody></table>
  <table class="oc-table oc-scores"><colgroup><col /><col class="oc-col-weight" /><col class="oc-col-score" /></colgroup>
    <thead><tr><th>Evaluation Group</th><th class="oc-center">Weight</th><th class="oc-center">Average</th></tr></thead>
    <tbody>${groupRows}
      <tr class="oc-overall"><th colspan="2">Overall weighted score</th><td class="oc-center"><strong>${score(overall)} / 5</strong>${overall == null ? '' : ` (${Math.round((overall / 5) * 100)}%)`}</td></tr>
    </tbody>
  </table>
  <div class="oc-block"><h3>Overall Comments</h3><p>${val(review.overallComments)}</p></div>
</div>`
    })
    .join('')
}

function activeRemarks(remarks) {
  const rounds = list(remarks?.rounds)
  const active = rounds.find((round) => round?.active) || rounds[0]
  const items = list(active?.remarks)
  if (!items.length) return ''
  const rows = items
    .map((item) => {
      const where =
        text(item.fieldLabel) || text(item.sectionLabel) || text(item.sectionKey) || 'Proposal'
      return `<tr><td>${val(where)}</td><td>${val(item.remark)}</td><td>${val(item.reviewerName)}</td></tr>`
    })
    .join('')
  return `
<div class="oc-block"><h3>Field-level Review Remarks</h3>
  <table class="oc-table oc-remarks"><colgroup><col class="oc-col-field" /><col /><col class="oc-col-reviewer" /></colgroup>
    <thead><tr><th>Field</th><th>Remark</th><th>Reviewer</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
</div>`
}

function approvalSection(proposal, reviews, remarks) {
  const draft = isSoDraft(proposal)
  const finalized = Boolean(proposal.soUploadedAt)
  const soNumber = text(proposal.soNumber)
  const budget =
    proposal.needsBudget === true
      ? 'With Budget (forwarded to Finance Office / OVCAF)'
      : proposal.needsBudget === false
        ? 'Without Budget (immediate implementation)'
        : ''
  const notesLabel = draft || finalized ? "Chancellor's Notes" : 'Remarks on Record'
  const soDocument = draft ? soDraftFileLabel(proposal) : proposal.soFileName

  // Data-driven, never role-driven: a row is printed only when the record actually
  // holds that value, so an untransacted proposal shows "no Special Order yet"
  // instead of a table of empty cells.
  const rows = []
  if (draft || soNumber) {
    rows.push(
      row(
        'Special Order (SO) Number',
        draft ? `${val(soNumber)} <span class="oc-tag">DRAFT &mdash; not yet issued</span>` : val(soNumber),
      ),
    )
  }
  if (draft || finalized) {
    rows.push(row('SO Date', finalized ? dateVal(proposal.soUploadedAt) : 'Not yet issued'))
  }
  if (text(soDocument)) {
    rows.push(row('SO Document', val(soDocument)))
  }
  if (budget) {
    rows.push(row('Budget Routing', esc(budget)))
  }
  if (text(proposal.remarks)) {
    rows.push(row(notesLabel, val(proposal.remarks)))
  }

  return `
${sectionTitle('4. Evaluation, Approval and Special Order')}
${reviewsHtml(reviews)}
${activeRemarks(remarks)}
${sectionTitle('5. Office of the Chancellor')}
${
  draft
    ? '<p class="oc-draft-note">SO draft &mdash; not yet finalized. This proposal is still awaiting the Chancellor\'s final approval.</p>'
    : ''
}
${rows.length ? coverTable(rows) : '<p class="oc-empty">No Special Order recorded yet.</p>'}`
}

function statusSection(proposal) {
  return `
${sectionTitle('6. Current Project Status')}
${coverTable([
  row('Status', val(proposalStatusLabel(proposal.status))),
  row('Last Updated', dateVal(proposal.updatedAt)),
  row('Created', dateVal(proposal.createdAt)),
  row('Submitted', dateVal(proposal.submittedByDate)),
])}`
}

/* ---------- cover styles ---------- */

export const DETAILS_COVER_STYLES = `
  .oc-cover { color: #000; }

  .oc-header { margin-bottom: 4mm; }
  table.oc-header-table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
  }
  table.oc-header-table td {
    border: 1px solid #000;
    padding: 1.6mm 2.4mm;
    vertical-align: middle;
  }
  .oc-seal-col { width: 27mm; }
  .oc-id-col { width: 52mm; }
  .oc-seal-cell { text-align: center; }
  img.oc-seal { width: 21mm; height: 21mm; display: block; margin: 0 auto; }
  .oc-inst { font-size: 11pt; font-weight: bold; text-align: center; letter-spacing: 0.02em; }
  .oc-office { font-size: 10pt; text-align: center; text-transform: uppercase; }
  .oc-title { font-size: 15pt; font-weight: bold; text-align: center; letter-spacing: 0.04em; }
  .oc-doc-id { font-size: 8pt; line-height: 1.5; }
  .oc-generated-note { font-size: 8pt; font-style: italic; margin: 2mm 0 0; }

  h2.oc-section {
    font-size: 10.5pt;
    text-transform: uppercase;
    border-bottom: 1px solid #000;
    padding-bottom: 1mm;
    margin: 5mm 0 2mm;
    break-after: avoid;
    page-break-after: avoid;
  }

  table.oc-table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
    margin: 0 0 2mm;
  }
  table.oc-table th,
  table.oc-table td {
    border: 1px solid #000;
    padding: 1.2mm 1.6mm;
    vertical-align: top;
    font-size: 9pt;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }
  table.oc-table th { font-weight: bold; text-align: left; background: #f2f2f2; }
  .oc-col-label { width: 38mm; }
  .oc-col-weight { width: 16mm; }
  .oc-col-score { width: 30mm; }
  .oc-col-field { width: 45mm; }
  .oc-col-reviewer { width: 32mm; }
  .oc-center { text-align: center; }

  .oc-block { margin: 0 0 2.5mm; }
  .oc-block h3 { font-size: 9.5pt; margin: 0 0 0.8mm; }
  .oc-block p { margin: 0; font-size: 9pt; }

  table.oc-review-head th,
  table.oc-review-head td {
    border: 1px solid #000;
    padding: 1mm 1.4mm;
    font-size: 8.5pt;
    text-align: left;
    vertical-align: top;
  }
  table.oc-review-head th { background: #f2f2f2; width: 18mm; }
  .oc-review { margin: 0 0 4mm; break-inside: avoid; page-break-inside: avoid; }
  tr.oc-overall th { text-align: right; }

  .oc-tag { font-size: 8pt; font-weight: bold; white-space: nowrap; }
  .oc-draft-note { font-size: 8.5pt; font-style: italic; margin: 1mm 0 0; }
  .oc-empty { font-size: 9pt; font-style: italic; margin: 0 0 2mm; }

  /* The official form always starts on a fresh page. The break lives on the section
     itself - no empty spacer element (which used to print a blank page of its own) and
     no extra top padding (the shared stylesheet already reserves the page margins). */
  section.oc-official {
    break-before: page;
    page-break-before: always;
  }
`

/* ---------- assembly ---------- */

/**
 * Build the complete print document. Kept separate from downloadProposalDetails
 * so the markup can be inspected without opening the print dialog.
 *
 * @param {{proposal: object, reviews?: Array, remarks?: object|null, generatedAt?: string}} input
 * @returns {string} a standalone HTML document
 */
export function buildProposalDetailsHtml({ proposal, reviews, remarks } = {}) {
  const p = proposal || {}
  // A draft SO document is not an issued attachment; the official form must not
  // list it as such (the cover already reports it as a draft).
  const officialProposal = isSoDraft(p) ? { ...p, soFileName: null } : p
  const title = `Proposal Details — ${text(p.projectTitle) || text(p.programTitle) || 'Proposal'}`

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${esc(title)}</title>
  <style>${OFFICIAL_FORM_STYLES}${DETAILS_COVER_STYLES}</style>
</head>
<body>
  <section class="oc-cover">
    ${coverHeader(p)}
    ${identificationSection(p)}
    ${researchSection(p)}
    ${descriptionSection(p)}
    ${approvalSection(p, reviews, remarks)}
    ${statusSection(p)}
  </section>
  <section class="oc-official">${renderOfficialProposalForm(officialProposal)}</section>
</body>
</html>`
}

/**
 * Render the document and save it as a PDF file.
 *
 * The PDF is generated by the backend from the HTML built here (one shared
 * template for every role), not by the browser's print dialog: Chrome always
 * prints its own automatic header (date + document title) and footer when the
 * dialog's "Headers and footers" option is on, regardless of CSS margins. The
 * server render contains the document only, with the document's own bottom-right
 * "Page n of N" pagination.
 *
 * `generatedAt` is accepted for API compatibility but is deliberately never
 * printed: a download timestamp is browser metadata, not part of the official
 * record (the request explicitly forbids timestamps in the document).
 *
 * @param {{proposal: object, reviews?: Array, remarks?: object|null, generatedAt?: string}} input
 * @returns {Promise<void>} resolves once the PDF file has been handed to the browser
 */
export function downloadProposalDetails({ proposal, reviews, remarks } = {}) {
  const p = proposal || {}
  const project = text(p.projectTitle) || text(p.programTitle) || 'Proposal'
  return savePdfDocument({
    title: p.projectTitle || p.programTitle || 'Proposal Details',
    filename: `Proposal Details - ${project}.pdf`,
    html: buildProposalDetailsHtml({ proposal: p, reviews, remarks }),
  })
}

/**
 * The one entry point every role's Download / Print action calls.
 *
 * - With a `proposalId`, the proposal (and, unless the caller already has them,
 *   its reviews) is re-read from the server first, so the printed document
 *   always matches the record as it stands now rather than a stale page copy.
 * - With an in-memory `proposal` only (the Create / Submit form, where the
 *   record may not be saved yet), it renders exactly the values given - no
 *   fetch, no invented data.
 * - `reviews` and `remarks` are optional and are used as supplied when the
 *   caller already loaded them.
 *
 * It ends in the same savePdfDocument() pipeline as every other export, so the
 * Download PDF and Print actions produce identical documents for every role.
 *
 * @param {{proposalId?: number|string, proposal?: object, reviews?: Array, remarks?: object|null, generatedAt?: string}} input
 * @returns {Promise<void>} resolves once the print dialog closes; rejects when
 *   the document cannot be prepared or the print dialog cannot be opened.
 */
export async function downloadProposalRecord({ proposalId, proposal, reviews, remarks, generatedAt } = {}) {
  const id = proposalId != null && String(proposalId).trim() !== '' ? proposalId : null
  let record = proposal || null
  let reviewRows = Array.isArray(reviews) ? reviews : null

  if (id != null) {
    const [proposalRes, reviewsRes] = await Promise.all([
      api.get(`/api/proposals/${id}`),
      reviewRows ? Promise.resolve(null) : api.get(`/api/proposals/${id}/reviews`),
    ])
    record = proposalRes?.data?.proposal || proposalRes?.data || record
    if (!reviewRows) {
      const data = reviewsRes?.data
      reviewRows = Array.isArray(data) ? data : Array.isArray(data?.reviews) ? data.reviews : []
    }
  }

  return downloadProposalDetails({
    proposal: record || {},
    reviews: reviewRows || [],
    remarks,
    generatedAt,
  })
}
