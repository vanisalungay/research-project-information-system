/**
 * officialProposalForm.js
 *
 * Dedicated renderer for the official MSU-Naawan
 * "FULL-BLOWN R & D PROJECT PROPOSAL" paper form.
 *
 * The layout is reproduced from the official form PDF, which is the design
 * source of truth. Geometry taken from that document (mm, measured from the
 * sheet edge):
 *   - page A4 portrait 210.2 x 297.1 mm
 *   - content table x 12.9 -> 197.0 mm (13 mm side margins)
 *   - boxed header table y 17.8 -> 53.4 mm, columns at
 *     12.9 | 44.6 | 157.9 | 187.1 | 197.0 mm, rows 17.8 / 27.3 / 36.5 / 53.4
 *   - body table starts at y 66.5 mm on page 1 and at y 22.0 mm on
 *     continuation pages; the boxed header appears on page 1 only
 *   - two-column body rows split at x 109.5 mm
 *   - thin black rules, white background, no fills or decoration
 *   - type: 10pt body, 11pt header lines, 16pt bold document title,
 *     8pt table column headers
 *
 * Only the proposal values vary between documents; the form itself is fixed.
 * This module intentionally does not import from documentExport.js for its
 * markup helpers - it only borrows the shared print routine.
 */

import sealImage from '@/assets/images/msu-naawan-seal.jpg?inline'
import { printDocument } from '@/utils/documentExport'

/** Escape a value so proposal text can never inject markup into the document. */
function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** Multi-line safe text. Empty values stay blank, as on the paper form. */
function val(value) {
  const text = value == null ? '' : String(value).trim()
  if (!text) return ''
  return esc(text).replace(/\r?\n/g, '<br />')
}

/** ISO dates are shown in a readable form; anything else is passed through. */
function dateVal(value) {
  const text = value == null ? '' : String(value).trim()
  if (!text) return ''
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(text)
  if (!m) return esc(text)
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
    'December'
  ]
  const month = months[Number(m[2]) - 1] || m[2]
  return `${month} ${Number(m[3])}, ${m[1]}`
}

/** An array from the API, or a plain object coerced to a list. */
function list(value) {
  if (Array.isArray(value)) return value
  if (value && typeof value === 'object') return Object.values(value)
  return []
}

/** Print-only stylesheet. Kept separate from the on-screen RPIS styles. */
export const OFFICIAL_FORM_STYLES = `
  @page {
    size: A4;
    /* Continuation pages start their table at 22mm; page 1 at 17.8mm. */
    margin: 22mm 13mm 27mm;
  }
  @page :first {
    margin-top: 17.8mm;
  }

  * { box-sizing: border-box; }

  html, body {
    margin: 0;
    padding: 0;
    background: #fff;
    color: #000;
    font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif;
    font-size: 10pt;
    line-height: 1.18;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  /* ---------- Boxed institutional header ---------- */
  table.hdr {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
    margin: 0;
    /* The official form's header block is set in Calibri (11pt); the body is Arial. */
    font-family: Calibri, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  }
  table.hdr td {
    border: 1px solid #000;
    padding: 1.1mm 1.6mm;
    vertical-align: middle;
  }
  table.hdr td.seal-cell { text-align: center; vertical-align: middle; }
  img.seal { width: 23.8mm; height: 23.8mm; display: block; margin: 0 auto; }
  .inst-name {
    font-size: 11pt;
    font-weight: bold;
    text-align: center;
    text-transform: uppercase;
    vertical-align: middle;
  }
  .inst-office {
    font-size: 10pt;
    text-align: center;
    vertical-align: middle;
    text-transform: uppercase;
  }
  .doc-title {
    display: block;
    font-size: 16pt;
    font-weight: bold;
    margin-top: 1.6mm;
  }
  .dc-label { font-size: 9pt; vertical-align: middle; }
  .dc-value {
    /* The official value cells are only 9.9mm wide - sized for a short
       handwritten number, so long values wrap inside the cell rather than
       widening the form. */
    font-size: 8pt;
    line-height: 1.05;
    vertical-align: middle;
    word-break: break-all;
  }
  .hdr-gap { height: 13.1mm; }

  /* ---------- Body form table ---------- */
  table.form {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
  }
  table.form > tbody > tr > td {
    border: 1px solid #000;
    padding: 1.4mm 1.8mm;
    vertical-align: top;
  }
  .sec-head { font-weight: bold; text-transform: uppercase; }
  .sec-note { font-size: 8.5pt; font-style: italic; }
  .lbl { font-weight: normal; }
  .val-line { margin-top: 0; }
  .answers { margin-top: 1.2mm; }
  .blank { display: inline-block; min-width: 40mm; }
  .tick {
    display: inline-block;
    width: 12mm;
    border-bottom: 1px solid #000;
    text-align: center;
    line-height: 1.1;
  }

  /* ---------- Nested data tables (sites, personnel, budget, ...) ---------- */
  table.grid {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
    margin-top: 1.2mm;
  }
  table.grid th,
  table.grid td {
    border: 1px solid #000;
    padding: 0.8mm 1.2mm;
    vertical-align: top;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }
  table.grid th {
    font-size: 8pt;
    font-weight: bold;
    text-align: center;
    text-transform: uppercase;
  }
  table.grid td { font-size: 9.5pt; }
  /* The official sites grid is 152.7mm wide inside the (3) row. */
  table.grid.sites { width: 152.7mm; }

  /* Never split a table row across a page. */
  tr, td { break-inside: avoid; page-break-inside: avoid; }

`

/** The official boxed institutional header (page 1 only). */
export function renderOfficialHeader(proposal = {}) {
  const p = proposal || {}
  // The official value cells are only 9.9mm wide - sized for a short handwritten
  // number. RPIS stores both a 3-digit proposalCode ("041") and a composite
  // documentId ("MSUN-ORPS-DA-041-2026-REV00"); the short code is the value that
  // fits the cell without changing the official header geometry.
  const docNo = p.proposalCode || ''
  const revNo =
    p.revisionNumber === 0 || p.revisionNumber ? String(p.revisionNumber) : ''
  return `
<table class="hdr">
  <colgroup>
    <col style="width: 31.7mm" />
    <col style="width: 113.3mm" />
    <col style="width: 29.2mm" />
    <col style="width: 9.9mm" />
  </colgroup>
  <tbody>
    <tr style="height: 9.5mm">
      <td class="seal-cell" rowspan="3">
        <img class="seal" src="${sealImage}" alt="Mindanao State University at Naawan seal" />
      </td>
      <td class="inst-name">MINDANAO STATE UNIVERSITY AT NAAWAN</td>
      <td class="dc-label">Document No.</td>
      <td class="dc-value">${val(docNo)}</td>
    </tr>
    <tr style="height: 9.2mm">
      <td class="inst-office" rowspan="2">
        OFFICE OF VICE CHANCELLOR FOR RESEARCH, INNOVATION AND GLOBAL ENGAGEMENT
        <span class="doc-title">FULL-BLOWN R &amp; D PROJECT PROPOSAL</span>
      </td>
      <td class="dc-label">Revision No.</td>
      <td class="dc-value">${val(revNo)}</td>
    </tr>
    <tr style="height: 16.9mm">
      <td class="dc-label">Revision Date:</td>
      <td class="dc-value"></td>
    </tr>
  </tbody>
</table>
<div class="hdr-gap"></div>`
}

/**
 * A bordered nested data table. `rows` is an array of arrays of cell values;
 * an empty value renders as a blank cell, exactly like the paper form.
 */
function dataGrid(headers, rows, widths, extraClass = '') {
  const cols = headers.map((h, i) => `<col style="width: ${widths[i]}" />`).join('')
  const head = headers.map((h) => `<th>${esc(h)}</th>`).join('')
  const body = rows
    .map((row) => `<tr>${row.map((cell) => `<td>${val(cell)}</td>`).join('')}</tr>`)
    .join('')
  return `<table class="grid${extraClass ? ' ' + extraClass : ''}"><colgroup>${cols}</colgroup><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`
}

/* ---------- small row helpers (colgroup = 96.6mm | 87.5mm) ---------- */

/**
 * Reserve the official row height with an inner block instead of a `height` on
 * the cell: Chromium's print pipeline emits a stray rasterised paint chunk (and
 * an extra page) when body cells carry an explicit height.
 */
function cellBox(inner, height) {
  return `<div style="min-height: calc(${height} - 2.8mm)">${inner}</div>`
}

function fullRow(inner, height) {
  return `<tr><td colspan="2">${cellBox(inner, height)}</td></tr>`
}

function splitRow(left, right, height) {
  return `<tr><td>${cellBox(left, height)}</td><td>${cellBox(right, height)}</td></tr>`
}

function head(text) {
  return `<div class="sec-head">${esc(text)}</div>`
}

/** A tick box over the official blank line. */
function tick(selected) {
  return `<span class="tick">${selected ? '&#10003;' : ''}</span>`
}

function line(label, value) {
  return `<div class="val-line"><span class="lbl">${esc(label)}</span> ${val(value)}</div>`
}

/* ---------- page 1 sections ---------- */

function section1(p) {
  const inner = `${head('(1) PROJECT PROFILE')}
    ${line('Program Title:', p.programTitle)}
    ${line('Project Title:', p.projectTitle)}
    ${line('Project Leader/Sex:', [p.projectLeader, p.projectLeaderSex].filter(Boolean).join(' / '))}
    ${line('Project Duration (number of months):', p.duration)}
    ${line('Project Start Date:', dateVal(p.startDate))}
    ${line('Project End Date:', dateVal(p.endDate))}
    ${line('Implementing College (Department):', p.college)}
    ${line('Address/Telephone/Fax/Email (Barangay, Municipality, District, Province, Region):', p.address)}`
  return fullRow(inner, '41.3mm')
}

function section2(p) {
  const inner = `${head('(2) COOPERATING AGENCY/IES')}
    <div class="answers">${val(p.cooperatingAgencies)}</div>`
  return fullRow(inner, '12.7mm')
}

function section3(p) {
  const sites = list(p.sites)
  const rows = sites.map((s, i) => [
    `${i + 1}.`,
    s.country,
    s.region,
    s.province,
    s.district,
    s.municipality,
    s.barangay
  ])
  while (rows.length < 2) rows.push([`${rows.length + 1}.`, '', '', '', '', '', ''])
  const grid = dataGrid(
    ['IMPLEMENTATION SITES NO.', 'COUNTRY', 'REGION', 'PROVINCE', 'DISTRICT', 'MUNICIPALITY', 'BARANGAY'],
    rows,
    ['23.3mm', '23.6mm', '23.6mm', '23.7mm', '21.2mm', '21.1mm', '16.2mm'],
    'sites'
  )
  return fullRow(`${head('(3) SITE(S) OF IMPLEMENTATION')}${grid}`, '33.5mm')
}

/**
 * Section 5 prints the agendas the proposal itself stores, so a framework an
 * administrator configured later - or an agenda that has since been deactivated - is
 * rendered correctly. Display names come from `p.agendaLabels`
 * ({ agendaKey: { name, subLabel } }) when the caller resolved them from the master
 * data; otherwise the stable key is used. Nothing here is hard-coded: the coverage
 * period note comes from `p.agendaFrameworkLabel`.
 */
function section4and5(p) {
  const type = (p.researchType || '').trim()
  const left = `${head('(4) TYPE OF RESEARCH')}
    <div class="val-line">${tick(type.toLowerCase() === 'basic')} Basic</div>
    <div class="val-line">${tick(type.toLowerCase() === 'applied')} Applied</div>`

  const labels = p.agendaLabels && typeof p.agendaLabels === 'object' ? p.agendaLabels : {}
  const note = esc(String(p.agendaFrameworkLabel || '').trim())
  const rows = list(p.priorityAgendas)
    .filter((row) => row && row.selected)
    .map((row) => {
      const key = row.agendaKey || row.agenda || ''
      const meta = labels[key] || {}
      const name = meta.name || String(key).toUpperCase()
      const sub = meta.subLabel ? ` ${meta.subLabel}` : ''
      return line(`____ ${name}${sub}:`, row.value || '')
    })
    .join('')

  const right = `${head('(5) Priority Agenda')}
    ${note ? `<div class="sec-note">${note}</div>` : ''}
    ${rows || '<div class="val-line">No priority agenda selected.</div>'}`
  return splitRow(left, right, '52.9mm')
}

function section6(p) {
  return splitRow(head('(6) Innovation Goals'), `<div>${val(p.innovationGoals)}</div>`, '10.6mm')
}

function section7(p) {
  return splitRow(
    `${head('(7) Sector Relevance')}<div class="sec-note">(Public Health, Environment, Agriculture, etc.)</div>`,
    `<div>${val(p.sectorRelevance)}</div>`,
    '12.3mm'
  )
}

function section8(p) {
  const sdg = Array.isArray(p.sdg) ? p.sdg.join('; ') : p.sdg
  return splitRow(
    `${head('(8) Applicable Sustainable Development Goal (SDG) Addressed')}`,
    `<div>${val(sdg)}</div>`,
    '12.4mm'
  )
}

function section9(p) {
  const inner = `${head('(9) EXECUTIVE SUMMARY')}
    <div class="sec-note">(not to exceed 200 words)</div>
    <div class="answers">${val(p.executiveSummary)}</div>`
  return fullRow(inner, '15.9mm')
}

function section10() {
  return fullRow(head('(10) INTRODUCTION'), '12.3mm')
}

/* ---------- page 2 sections ---------- */

function section101(p) {
  const inner = `${head('(10.1) RATIONALE/SIGNIFICANCE')}
    <div class="sec-note">(not to exceed 300 words)</div>
    <div class="answers">${val(p.rationale)}</div>`
  return fullRow(inner, '12.4mm')
}

function section102(p) {
  const inner = `${head('(10.2) SCIENTIFIC BASIS/THEORETICAL FRAMEWORK')}
    <div class="answers">${val(p.framework)}</div>`
  return fullRow(inner, '12.3mm')
}

function section103(p) {
  const inner = `${head('(10.3) OBJECTIVES')}
    ${line('General:', p.objectivesGeneral)}
    ${line('Specific:', p.objectivesSpecific)}`
  return fullRow(inner, '32.9mm')
}

function section11(p) {
  const inner = `${head('(11) REVIEW OF LITERATURE')}
    <div class="sec-note">For startup proposals, results of previous R&amp;D conducted related to the proposed
    technology (product/process/service) and the status of the intellectual property (IP) protection of the
    proposed technology should be included. Also, include a background on the development of the technology
    (i.e., evolution of the startup, first prototype, first test, first sale).</div>
    <div class="answers">${val(p.review)}</div>`
  return fullRow(inner, '24.6mm')
}

function section12(p) {
  const inner = `${head('(12) METHODOLOGY')}
    <div class="answers">${val(p.methodology)}</div>`
  return fullRow(inner, '16.6mm')
}

function section13(p) {
  const inner = `${head('(13) TECHNOLOGY ROADMAP')}
    <div class="sec-note">(if applicable) (use the attached sheet). Indicate the target TRL level of the
    target product.</div>
    ${line('Target TRL level:', p.technologyTrl)}`
  return fullRow(inner, '16.3mm')
}

function section14(p) {
  const inner = `${head('(14) EXPECTED OUTPUTS (6Ps)')}
    <div class="answers">${val(p.outputs)}</div>`
  return fullRow(inner, '12.7mm')
}

function section15(p) {
  const inner = `${head('(15) POTENTIAL OUTCOMES')}
    <div class="answers">${val(p.outcomes)}</div>`
  return fullRow(inner, '12.3mm')
}

function section16(p) {
  const inner = `${head('(16) POTENTIAL IMPACTS (2Is)')}
    ${line('a. Economic', p.impactEconomic)}
    ${line('b. Social/Ethical', p.impactSocial)}`
  return fullRow(inner, '37.4mm')
}

function section17(p) {
  const inner = `${head('(17) TARGET BENEFICIARIES')}
    <div class="answers">${val(p.beneficiaries)}</div>`
  return fullRow(inner, '16.6mm')
}

function section18(p) {
  const inner = `${head('(18) SUSTAINABILITY PLAN')}
    <div class="answers">${val(p.sustainability)}</div>`
  return fullRow(inner, '14.8mm')
}

function section19(p) {
  const score = p.gadScore === 0 || p.gadScore ? String(p.gadScore) : ''
  const inner = `${head('(19) GENDER AND DEVELOPMENT (GAD) SCORE')}
    <div class="sec-note">(refer to the attached GAD checklist):</div>
    ${line('GAD Score:', score)}`
  return fullRow(inner, '8.5mm')
}

function section20(p) {
  const rows = list(p.limitations).map((l) => [l.limitation, l.remarks])
  let body = ''
  if (rows.length) {
    body = dataGrid(['LIMITATION', 'REMARKS'], rows, ['50%', '50%'])
  } else {
    body = `<div class="answers">${val(p.limitationsText)}</div>`
  }
  return fullRow(`${head('(20) LIMITATIONS OF THE PROJECT')}${body}`, '12.3mm')
}

function section21(p) {
  const inner = `${head('(21) LIST OF RISKS AND ASSUMPTIONS')}
    <div class="sec-head">RISK MANAGEMENT PLAN</div>
    <div class="sec-note">(List possible risks and assumptions in attaining target outputs or objectives.)</div>
    <div class="answers">${val(p.risks)}</div>`
  return fullRow(inner, '15.5mm')
}

/* ---------- page 3 sections ---------- */

function section22(p) {
  const rows = list(p.logFrames).map((r) => [r.outcome, r.output])
  while (rows.length < 2) rows.push(['', ''])
  const grid = dataGrid(
    ['OUTCOME INDICATORS', 'OUTPUT INDICATORS/PHYSICAL TARGET ACCOMPLISHMENT'],
    rows,
    ['42%', '58%']
  )
  return fullRow(`${head('(22) LOGICAL FRAMEWORK: OUTCOME AND OUTPUT INDICATORS')}${grid}`, '8.5mm')
}

function section23(p) {
  const inner = `${head('(23) LITERATURE CITED')}
    <div class="answers">${val(p.referencesText)}</div>`
  return fullRow(inner, '16.6mm')
}

function section24(p) {
  const rows = list(p.personnel).map((r) => [r.position, r.timeDevoted, r.responsibilities])
  const grid = dataGrid(
    ['POSITION', 'PERCENT TIME DEVOTED TO THE PROJECT', 'RESPONSIBILITIES'],
    rows,
    ['28%', '20%', '52%']
  )
  return fullRow(`${head('(24) PERSONNEL REQUIREMENT')}${grid}`, '5.6mm')
}

function section25(p) {
  const rows = list(p.budget).map((b) => [b.agency, b.ps, b.mooe, b.eo, b.total])
  rows.push(['TOTAL', '', '', '', ''])
  const grid = dataGrid(
    ['IMPLEMENTING AGENCY', 'PS', 'MOOE', 'EO', 'TOTAL'],
    rows,
    ['40%', '15%', '15%', '15%', '15%']
  )
  return fullRow(`${head('(25) LINE-ITEM BUDGET REQUIREMENT')}${grid}`, '4.9mm')
}

function section26(p) {
  const count = p.otherProjectsNumber ? ` ${esc(String(p.otherProjectsNumber))} (number)` : ' ______ (number)'
  const rows = list(p.otherProjects).map((o) => [o.title, o.agency, o.involvement])
  const grid = dataGrid(
    ['TITLE OF THE PROJECT', 'FUNDING AGENCY', 'INVOLVEMENT IN THE PROJECT'],
    rows,
    ['45%', '27%', '28%']
  )
  return fullRow(
    `${head('(26) OTHER ONGOING PROJECTS BEING HANDLED BY THE PROJECT LEADER:')}${count}${grid}`,
    '8.1mm'
  )
}

function section27(p) {
  const attachments = [
    ['Review of Literature', p.reviewFileName],
    ['Technology Roadmap', p.roadmapFileName],
    ['Target Beneficiaries', p.beneficiariesFileName],
    ['GAD Score Document', p.gadFileName],
    ['Special Order', p.soFileName]
  ].filter(([, file]) => file)
  const extra = (Array.isArray(p.supportingDocuments) ? p.supportingDocuments : [])
    .filter(Boolean)
    .map((name) => ['Supporting document', name])
  const all = attachments.concat(extra)
  const files = all.length
    ? `<div class="answers">${all
        .map(([label, file]) => `<div>${esc(label)}: ${esc(String(file))}</div>`)
        .join('')}</div>`
    : ''
  // The exported proposal form ends here. The certification paragraph, the
  // SUBMITTED BY / ENDORSED BY signature block and the trailing
  // "See guidelines/definitions at the back" note are deliberately not part of
  // the proposal document - the certification is handled by its own separate
  // document in RPIS.
  const inner = `${head('(27) OTHER SUPPORTING DOCUMENTS')}
    <div class="sec-note">(Please refer to page 2 for the additional necessary documents.)</div>
    ${files}`
  return fullRow(inner, '6.0mm')
}


/* ---------- assembly ---------- */

/** The complete official form: boxed header + sections 1-27 + signature block. */
export function renderOfficialProposalForm(proposal = {}) {
  const p = proposal || {}
  const rows = [
    section1(p),
    section2(p),
    section3(p),
    section4and5(p),
    section6(p),
    section7(p),
    section8(p),
    section9(p),
    section10(),
    section101(p),
    section102(p),
    section103(p),
    section11(p),
    section12(p),
    section13(p),
    section14(p),
    section15(p),
    section16(p),
    section17(p),
    section18(p),
    section19(p),
    section20(p),
    section21(p),
    section22(p),
    section23(p),
    section24(p),
    section25(p),
    section26(p),
    section27(p)
  ].join('')
  return `${renderOfficialHeader(p)}
<table class="form">
  <colgroup><col style="width: 96.6mm" /><col style="width: 87.5mm" /></colgroup>
  <tbody>${rows}</tbody>
</table>`
}

/** Wrap the official form in a standalone, print-ready HTML document. */
export function buildOfficialProposalHtml(proposal = {}) {
  const p = proposal || {}
  const title = p.projectTitle || p.programTitle || 'Research Project Proposal'
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${esc(title)}</title>
  <style>${OFFICIAL_FORM_STYLES}</style>
</head>
<body>
${renderOfficialProposalForm(p)}
</body>
</html>`
}

/**
 * The single shared entry point for every full-proposal Download and PRINT
 * action. Renders the official form into a hidden iframe and opens the
 * browser print dialog, so "Save as PDF" and "Print" produce the same
 * document and the RPIS page itself is never printed.
 */
export function downloadOfficialProposal(proposal) {
  const p = proposal || {}
  const title = p.projectTitle || p.programTitle || 'Research Project Proposal'
  return printDocument({ title, html: buildOfficialProposalHtml(p) })
}





