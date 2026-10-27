/**
 * terminalReportDocument.js
 * ---------------------------------------------------------------------------
 * The standalone HTML document of a printed Terminal Report.
 *
 * The body is the markup already rendered on screen by TerminalReportForm.vue (the printed
 * paper shell of reportDocument.css: one .doc-sheet per page, header, footer, page badge),
 * and the stylesheets are inlined, so the printed / PDF report is exactly the document the
 * proponent reviewed - same sections, same order, same page numbering.
 *
 * Pure string building only: no DOM, no report data is generated here.
 */

/** Escapes text that ends up inside the document markup. */
export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * A complete HTML document for the print dialog: the rendered report body plus the inlined
 * RPIS document stylesheets (global print guard, printed paper shell, Terminal Report
 * styling).
 */
export function buildTerminalReportPrintHtml({ title, bodyHtml, css = '' }) {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>${escapeHtml(title)}</title>
<style>
${css}
</style>
<style>
  /* The printed document is the document on screen: no application chrome, no extra
     margins - the sheets themselves carry the page layout and the @page rule of
     src/assets/print.css keeps A4 with 16mm/15mm margins. */
  html,
  body {
    margin: 0;
    padding: 0;
    background: #ffffff;
  }
  .doc-scroll {
    margin: 0;
    padding: 0;
    overflow: visible;
  }
</style>
</head>
<body>
<div class="doc-scroll">${bodyHtml}</div>
</body>
</html>`
}
