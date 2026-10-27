/**
 * terminalReportPrint.js
 * ---------------------------------------------------------------------------
 * Print / PDF export of a completed Terminal Report.
 *
 * It reuses the existing document exporter (src/utils/documentExport.js -> printDocument),
 * which renders the HTML in a hidden iframe and opens the browser's print dialog, where the
 * proponent can print the report or save it as PDF. The rendered document on screen is
 * reused as-is and the same stylesheets are inlined, so print and screen never diverge.
 */
import printCss from '@/assets/print.css?raw'
import terminalCss from '@/assets/terminalReportDocument.css?raw'
import { printDocument } from '@/utils/documentExport'
import { buildTerminalReportPrintHtml } from '@/utils/terminalReportDocument'

/** Global print guard + the Terminal Report document styling. */
const TERMINAL_REPORT_CSS = [printCss, terminalCss].join('\n')

/**
 * The Terminal Report is printed on US Letter pages with the margins the official format
 * uses: the sheet itself carries the page padding, so the print margins are zero and the
 * page numbers printed at the bottom of each sheet land where the document shows them.
 * Declared after the application print guard so it takes precedence for this document.
 */
const TERMINAL_REPORT_PAGE_CSS = `
@page {
  size: letter;
  margin: 0;
}
`

/**
 * Prints the rendered Terminal Report document - the `.doc-scroll` element of the viewer -
 * or saves it as PDF through the browser's print dialog. Returns the print promise; an
 * absent element (nothing rendered) resolves to false instead of opening an empty dialog.
 */
export function printTerminalReport({ title, element }) {
  if (!element) return Promise.resolve(false)
  const html = buildTerminalReportPrintHtml({
    title,
    bodyHtml: element.innerHTML,
    css: `${TERMINAL_REPORT_CSS}\n${TERMINAL_REPORT_PAGE_CSS}`,
  })
  return printDocument({ title, html })
}
