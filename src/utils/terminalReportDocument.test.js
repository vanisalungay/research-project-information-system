import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { buildTerminalReportPrintHtml, escapeHtml } from './terminalReportDocument.js'

describe('terminalReportDocument - print HTML', () => {
  it('wraps the rendered sheets in a complete, styled HTML document', () => {
    const html = buildTerminalReportPrintHtml({
      title: 'Terminal Report - Fish Landing Survey',
      bodyHtml: '<section class="doc-sheet"><div class="doc-body">Hello</div></section>',
      css: '.doc-sheet { width: 210mm; }',
    })

    assert.match(html, /^<!doctype html>/)
    assert.match(html, /<title>Terminal Report - Fish Landing Survey<\/title>/)
    assert.match(html, /\.doc-sheet \{ width: 210mm; \}/, 'the document stylesheets are inlined')
    assert.match(html, /<div class="doc-scroll"><section class="doc-sheet">/)
    assert.match(html, /Hello/)
    assert.match(html, /@page|\.doc-scroll/, 'print layout rules are included')
  })

  it('escapes the document title', () => {
    const html = buildTerminalReportPrintHtml({
      title: 'Report <script>alert(1)</script>',
      bodyHtml: '',
      css: '',
    })
    assert.ok(!html.includes('<script>alert(1)</script>'))
    assert.match(html, /&lt;script&gt;/)
  })

  it('escapes text used in markup', () => {
    assert.equal(escapeHtml('A & B <tag> "quoted"'), 'A &amp; B &lt;tag&gt; &quot;quoted&quot;')
    assert.equal(escapeHtml(null), '')
  })

  it('keeps the rendered body untouched (the document is the data that was saved)', () => {
    const body = '<section class="doc-sheet"><p class="tr-para">A &amp; B</p></section>'
    const html = buildTerminalReportPrintHtml({ title: 'T', bodyHtml: body, css: '' })
    assert.ok(html.includes(body), 'already-escaped document markup is not double escaped')
  })
})
