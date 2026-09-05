/**
 * documentExport.js
 * Shared utilities for generating a standalone, print-ready document from the
 * on-screen form data so users can save a properly formatted PDF (via the
 * browser's "Save as PDF") that mirrors the structure/layout of the form.
 */

/** Escape a value so it can be safely embedded in generated HTML. */
export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** Convert plain text (possibly multi-line) to safe HTML with <br> line breaks. */
export function formatText(value, fallback = 'Not provided') {
  const text = value == null ? '' : String(value).trim()
  if (!text) return `<span class="empty">${escapeHtml(fallback)}</span>`
  return escapeHtml(text).replace(/\r?\n/g, '<br/>')
}

/** Read an uploaded File/Blob as a base64 data URL (used for signature images). */
export function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    if (!file) return resolve(null)
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/** Render a labelled field (label + value). */
export function field(label, value, options = {}) {
  const { inline = false } = options
  const cls = inline ? 'field inline' : 'field'
  return `<div class="${cls}">
    <span class="label">${escapeHtml(label)}</span>
    <span class="value">${formatText(value)}</span>
  </div>`
}

/** Render a multi-line text block (label above a paragraph). */
export function textBlock(label, value) {
  return `<div class="field full">
    <span class="label">${escapeHtml(label)}</span>
    <div class="text">${formatText(value)}</div>
  </div>`
}

/** Render a file-upload field using the uploaded file's name (File object or string). */
export function fileField(label, file) {
  const name =
    file && typeof file === 'object' && file.name
      ? file.name
      : typeof file === 'string'
        ? file
        : null
  return field(label, name || '', { inline: true })
}

/** Render a table from headers + rows (rows is an array of arrays). */
export function tableBlock(headers, rows) {
  const head = headers.map((h) => `<th>${escapeHtml(h)}</th>`).join('')
  const body = rows
    .map((row) => `<tr>${row.map((c) => `<td>${formatText(c)}</td>`).join('')}</tr>`)
    .join('')
  return `<div class="table-wrap">
    <table>
      <thead><tr>${head}</tr></thead>
      <tbody>${body}</tbody>
    </table>
  </div>`
}

/** Render a numbered form section (number badge + title + body). */
export function section({ number, title, sub = '', body }) {
  return `<section class="doc-section">
    <div class="section-head">
      <span class="section-num">${escapeHtml(number)}</span>
      <div class="section-titles">
        <h2>${escapeHtml(title)}</h2>
        ${sub ? `<span class="section-sub">${escapeHtml(sub)}</span>` : ''}
      </div>
    </div>
    <div class="section-body">${body}</div>
  </section>`
}

/** Render a signature block with name, designation, date and optional image. */
export function signatureBlock({ label, name, designation, date, imageUrl }) {
  const hasImage = Boolean(imageUrl)
  return `<div class="signature-block">
    <div class="signature-line">
      ${hasImage
        ? `<img class="signature-img" src="${imageUrl}" alt="${escapeHtml(name || 'Signature')}" />`
        : `<span class="signature-placeholder">Signed</span>`}
    </div>
    <div class="signature-name">${formatText(name)}</div>
    <div class="signature-meta">${formatText(designation)}</div>
    <div class="signature-meta">${formatText(date)}</div>
    <div class="signature-caption">${escapeHtml(label)}</div>
  </div>`
}

const PRINT_STYLES = `
  :root { color-scheme: light; }
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; }
  body {
    font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
    color: #1e293b;
    background: #fff;
    font-size: 12.5px;
    line-height: 1.5;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .doc-page { padding: 6px 4px; }

  .doc-header {
    border-bottom: 3px solid #4b3f72;
    padding-bottom: 12px;
    margin-bottom: 18px;
  }
  .doc-header .org {
    font-size: 11px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #64748b;
    font-weight: 700;
  }
  .doc-header h1 {
    margin: 4px 0 2px;
    font-size: 22px;
    color: #0f172a;
    letter-spacing: -0.01em;
  }
  .doc-header .subtitle { color: #475569; font-size: 13px; }

  .doc-section {
    break-inside: avoid;
    margin-bottom: 18px;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    overflow: hidden;
  }
  .section-head {
    display: flex;
    align-items: center;
    gap: 12px;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    padding: 10px 14px;
  }
  .section-num {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background: #4b3f72;
    color: #fff;
    font-weight: 700;
    font-size: 13px;
    flex: 0 0 auto;
  }
  .section-titles h2 {
    margin: 0;
    font-size: 15px;
    color: #0f172a;
  }
  .section-sub {
    display: block;
    font-size: 11.5px;
    color: #64748b;
    margin-top: 1px;
  }
  .section-body { padding: 14px; }

  .field {
    display: flex;
    flex-direction: column;
    gap: 3px;
    padding: 7px 0;
    border-bottom: 1px dashed #eef2f7;
  }
  .field.full { grid-column: 1 / -1; }
  .field .label {
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #64748b;
  }
  .field .value { font-weight: 500; color: #0f172a; }
  .field .text { color: #1e293b; white-space: normal; }
  .field .empty { color: #94a3b8; font-style: italic; font-weight: 400; }

  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
  .row .field:last-child { border-bottom: none; }

  .sub-heading {
    margin: 12px 0 4px;
    font-size: 12px;
    font-weight: 700;
    color: #4b3f72;
  }

  .table-wrap { margin: 8px 0; overflow-x: visible; }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
  }
  th, td {
    border: 1px solid #e2e8f0;
    padding: 7px 9px;
    text-align: left;
    vertical-align: top;
  }
  th { background: #eef2f7; color: #334155; font-weight: 700; }
  td { color: #1e293b; }

  .file-list { margin: 6px 0 0; padding-left: 18px; color: #1e293b; }
  .file-list li { margin: 2px 0; }

  .signature-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
    margin-top: 8px;
  }
  .signature-block {
    break-inside: avoid;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 12px 14px;
    background: #fbfdff;
  }
  .signature-line {
    min-height: 78px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    padding-bottom: 6px;
    border-bottom: 1.5px solid #94a3b8;
    margin-bottom: 8px;
  }
  .signature-img {
    max-height: 74px;
    max-width: 220px;
    object-fit: contain;
  }
  .signature-placeholder {
    color: #cbd5e1;
    font-size: 12px;
    font-style: italic;
  }
  .signature-name {
    font-weight: 700;
    color: #0f172a;
    text-align: center;
    min-height: 16px;
  }
  .signature-meta {
    text-align: center;
    color: #475569;
    font-size: 11.5px;
    min-height: 15px;
  }
  .signature-caption {
    margin-top: 8px;
    text-align: center;
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #64748b;
  }

  @page {
    size: A4;
    margin: 16mm 15mm;
  }
`

/** Wrap body content into a full standalone HTML document ready for printing. */
export function buildStandaloneHtml({ title, subtitle = '', body }) {
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${escapeHtml(title)}</title>
  <style>${PRINT_STYLES}</style>
</head>
<body>
  <div class="doc-page">
    <header class="doc-header">
      <div class="org">MSU at Naawan &middot; Research Project Information System</div>
      <h1>${escapeHtml(title)}</h1>
      ${subtitle ? `<div class="subtitle">${escapeHtml(subtitle)}</div>` : ''}
    </header>
    ${body}
  </div>
</body>
</html>`
}

/**
 * Render the generated HTML in a hidden iframe and open the browser's print
 * dialog so the user can save it as a PDF with full formatting preserved.
 */
export function printDocument({ title, html }) {
  return new Promise((resolve) => {
    const iframe = document.createElement('iframe')
    iframe.setAttribute('aria-hidden', 'true')
    Object.assign(iframe.style, {
      position: 'fixed',
      right: '0',
      bottom: '0',
      width: '0',
      height: '0',
      border: '0',
      visibility: 'hidden'
    })
    document.body.appendChild(iframe)

    const win = iframe.contentWindow
    const doc = win.document
    doc.open()
    doc.write(html)
    doc.close()

    let cleanedUp = false
    const cleanup = () => {
      if (cleanedUp) return
      cleanedUp = true
      setTimeout(() => {
        if (iframe.parentNode) iframe.parentNode.removeChild(iframe)
        resolve()
      }, 300)
    }

    const trigger = () => {
      try {
        win.addEventListener('afterprint', cleanup, { once: true })
      } catch (e) {
        /* older browsers fall back to the timer below */
      }
      win.focus()
      win.print()
      // Safety fallback in case afterprint is not fired.
      setTimeout(cleanup, 120000)
    }

    const images = Array.from(doc.images || [])
    if (images.length === 0) {
      setTimeout(trigger, 150)
    } else {
      Promise.all(
        images.map(
          (img) =>
            img.complete
              ? Promise.resolve()
              : new Promise((r) => {
                  img.onload = r
                  img.onerror = r
                })
        )
      ).then(() => setTimeout(trigger, 200))
    }
  })
}

// ===== Proposal document builder (backend `Proposal` DTO shape) =====

const AGENDA_LABELS = {
  dagat: 'DAGAT Program (Commodity)',
  punla: 'PUNLA Program (Priority Topic)',
  kalikasan: 'KALIKASAN Program (Sector)',
  negosyo: 'NEGOSYO Program (Sector)',
  tanglaw: 'TANGLAW Program (Sector)'
}

/** Normalize a value that may be an array or a plain object into an array. */
function asList(value) {
  if (Array.isArray(value)) return value
  if (value && typeof value === 'object') return Object.values(value)
  return []
}

/** Convert a serialized "Label: value" multi-line string into labelled fields. */
function parseLabeledLines(text) {
  const raw = text == null ? '' : String(text).trim()
  if (!raw) return ''
  return raw
    .split(/\r?\n/)
    .filter((line) => line.trim() !== '')
    .map((line) => {
      const idx = line.indexOf(':')
      if (idx === -1) return field('', line.trim())
      return field(line.slice(0, idx).trim(), line.slice(idx + 1).trim())
    })
    .join('')
}

/**
 * Build the full proposal document body from a backend `Proposal` object
 * (camelCase fields and nested entity lists). Returns HTML for the form
 * sections so the downloaded/printed file mirrors the on-screen form.
 */
export function buildProposalDocument(p = {}) {
  const researchTypeLabel =
    p.researchType === 'Basic'
      ? 'Basic Research'
      : p.researchType === 'Applied'
        ? 'Applied Research'
        : p.researchType

  const agendaBody = asList(p.priorityAgendas)
    .filter((a) => a && a.selected)
    .map((a) => field(AGENDA_LABELS[a.agendaKey] || a.agendaKey || 'Priority Agenda', a.value || ''))
    .join('')

  const sitesRows = asList(p.sites).map((s) => [
    s.country,
    s.region,
    s.province,
    s.district,
    s.municipality,
    s.barangay
  ])
  const logFrameRows = asList(p.logFrames).map((r) => [r.outcome, r.output])
  const personnelRows = asList(p.personnel).map((r) => [r.position, r.timeDevoted, r.responsibilities])
  const budgetRows = asList(p.budget).map((b) => [b.agency, b.ps, b.mooe, b.eo, b.total])
  const otherProjectsRows = asList(p.otherProjects).map((o) => [o.title, o.agency, o.involvement])
  const limitationRows = asList(p.limitations).map((l) => [l.limitation, l.remarks])

  const sections = [
    section({
      number: '01',
      title: 'Project Profile',
      body: `<div class="row">
          ${field('Program Title', p.programTitle)}
          ${field('Project Title', p.projectTitle)}
          ${field('Project Leader', p.projectLeader)}
          ${field('Duration (months)', p.duration)}
          ${field('Start Date', p.startDate)}
          ${field('End Date', p.endDate)}
          ${field('Implementing College / Department', p.college)}
        </div>
        ${textBlock('Address / Contact', p.address)}`
    }),
    section({
      number: '02',
      title: 'Cooperating Agencies',
      body: textBlock('Name/s and Address/es', p.cooperatingAgencies)
    }),
    section({
      number: '03',
      title: 'Sites of Implementation',
      body: tableBlock(
        ['Country', 'Region', 'Province', 'District', 'Municipality', 'Barangay'],
        sitesRows
      )
    }),
    section({
      number: '04',
      title: 'Type of Research',
      body: field('Type of Research', researchTypeLabel)
    }),
    section({
      number: '05',
      title: 'Priority Agenda',
      sub: '(based on MSUN RIIDE 2025–2028)',
      body: agendaBody || textBlock('Priority Agenda', '')
    }),
    section({
      number: '06',
      title: 'Innovation Goals',
      body: textBlock('Innovation Goals', p.innovationGoals)
    }),
    section({
      number: '07',
      title: 'Sector Relevance',
      body: textBlock('Sector Relevance', p.sectorRelevance)
    }),
    section({
      number: '08',
      title: 'Sustainable Development Goals (SDG)',
      body: textBlock('Sustainable Development Goals', p.sdg)
    }),
    section({
      number: '09',
      title: 'Executive Summary',
      body: textBlock('Executive Summary', p.executiveSummary)
    }),
    section({
      number: '10',
      title: 'Introduction',
      body: `${textBlock('10.1 Rationale / Significance', p.rationale)}
        ${textBlock('10.2 Scientific Basis / Theoretical Framework', p.framework)}
        ${textBlock('10.3 General Objective', p.objectivesGeneral)}
        ${textBlock('10.4 Specific Objectives', p.objectivesSpecific)}`
    }),
    section({
      number: '11',
      title: 'Review of Literature',
      body: `${textBlock('Review of Literature', p.review)}
        ${p.reviewFileName ? fileField('Review of Literature (file)', p.reviewFileName) : ''}`
    }),
    section({
      number: '12',
      title: 'Methodology',
      body: textBlock('Methodology', p.methodology)
    }),
    section({
      number: '13',
      title: 'Technology Roadmap',
      body: `${p.technologyTrl ? field('Technology Readiness Level (TRL)', p.technologyTrl) : ''}
        ${p.roadmapFileName ? fileField('Technology Roadmap (file)', p.roadmapFileName) : ''}`
    }),
    section({
      number: '14',
      title: 'Expected Outputs (6Ps)',
      body: parseLabeledLines(p.outputs) || textBlock('Expected Outputs', '')
    }),
    section({
      number: '15',
      title: 'Potential Outcomes',
      body: textBlock('Potential Outcomes', p.outcomes)
    }),
    section({
      number: '16',
      title: 'Potential Impacts (2Is)',
      body: `${textBlock('a. Economic Impact', p.impactEconomic)}
        ${textBlock('b. Social / Ethical Impact', p.impactSocial)}`
    }),
    section({
      number: '17',
      title: 'Target Beneficiaries',
      body: `${textBlock('Target Beneficiaries', p.beneficiaries)}
        ${p.beneficiariesFileName ? fileField('Target Beneficiaries (file)', p.beneficiariesFileName) : ''}`
    }),
    section({
      number: '18',
      title: 'Sustainability Plan',
      body: textBlock('Sustainability Plan', p.sustainability)
    }),
    section({
      number: '19',
      title: 'Gender and Development (GAD) Score',
      body: `${p.gadScore != null ? field('GAD Score', p.gadScore) : ''}
        ${p.gadFileName ? fileField('GAD Score Document', p.gadFileName) : ''}`
    }),
    section({
      number: '20',
      title: 'Limitations of the Project',
      body: limitationRows.length
        ? tableBlock(['Limitation', 'Remarks'], limitationRows)
        : textBlock('Limitations', '')
    }),
    section({
      number: '21',
      title: 'Risk Management Plan',
      body: textBlock('Risks & Assumptions', p.risks)
    }),
    section({
      number: '22',
      title: 'Logical Framework',
      sub: 'Outcome and Output Indicators',
      body: tableBlock(['Outcome Indicators', 'Output Indicators / Physical Target'], logFrameRows)
    }),
    section({
      number: '23',
      title: 'Literature Cited',
      body: textBlock('Literature Cited', p.referencesText)
    }),
    section({
      number: '24',
      title: 'Personnel Requirement',
      body: tableBlock(['Position', '% Time Devoted', 'Responsibilities'], personnelRows)
    }),
    section({
      number: '25',
      title: 'Line-Item Budget Requirement',
      body: budgetRows.length
        ? tableBlock(['Agency', 'PS', 'MOOE', 'EO', 'Total'], budgetRows)
        : textBlock('Line-Item Budget', '')
    }),
    section({
      number: '26',
      title: 'Other Ongoing Projects',
      sub: 'Being handled by the Project Leader',
      body: `${field('Number of other projects', p.otherProjectsNumber)}
        ${tableBlock(['Title', 'Funding Agency', 'Involvement'], otherProjectsRows)}`
    })
  ]

  return sections.join('')
}

/**
 * One-call helper: build + print/download a proposal as a clean A4 document.
 * Used by every read-only view's "Print" / "Download Proposal" action so the
 * behavior is identical across all roles and pages.
 */
export function downloadProposalDocument(proposal) {
  const p = proposal || {}
  const html = buildStandaloneHtml({
    title: 'Research Proposal Form',
    subtitle: p.projectTitle || p.programTitle || 'Project Proposal',
    body: buildProposalDocument(p)
  })
  return printDocument({ title: 'Research Proposal Form', html })
}
