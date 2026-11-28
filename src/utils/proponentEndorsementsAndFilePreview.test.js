/**
 * proponentEndorsementsAndFilePreview.test.js
 * ---------------------------------------------------------------------------
 * Guards the two Proponent changes:
 *
 *   1. The My Endorsements table reads as part of the RPIS design system instead of a
 *      white block: the page carries its own modifier, the head is painted explicitly
 *      (the shared `.end-table th` assumes a blue band the page does not have, which is
 *      what made the labels white-on-white), and columns and rows are separated.
 *   2. Every file in the proposal details' Certification & Endorsement section has a
 *      real View action immediately before Download, opening the same stored file.
 *
 * The SFCs and the shared stylesheet are read as text, the technique the neighbouring
 * tests use, so this needs no browser or build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const ENDORSEMENTS = read('../views/Proponent/ProponentEndorsements.vue')
const DETAILS = read('../views/Proponent/ProponentDetiailedProp.vue')
const STYLES = read('../assets/endorsement.css')

test('the My Endorsements page opts into its own table treatment', () => {
  assert.match(ENDORSEMENTS, /<div class="end-page end-page--fluid end-page--proponent">/)
})

test('the table head gets an explicit, readable RPIS band', () => {
  // The page was white-on-white because the shared rule sets white labels while the
  // global th rule paints only a near-white band. The page now paints the band itself.
  const head = /\.end-page--proponent \.end-table thead th\s*\{([^}]*)\}/.exec(STYLES)
  assert.ok(head, 'the page must style its own table head')
  assert.match(head[1], /background:\s*#3c3b6e/, 'the RPIS navy band')
  assert.match(head[1], /color:\s*#ffffff/, 'white labels on that band')
  assert.match(head[1], /font-weight:\s*var\(--ui-weight-semibold\)/)
  assert.match(head[1], /text-transform|letter-spacing/, 'the shared uppercase label treatment')
  assert.match(STYLES, /\.end-page--proponent \.end-table thead th\s*\{[^}]*border-right:/,
    'the head keeps the columns visually separated')
})

test('the body has visible row dividers, column separation and a subtle zebra', () => {
  const body = /\.end-page--proponent \.end-table tbody td\s*\{([^}]*)\}/.exec(STYLES)
  assert.ok(body, 'the body must be styled')
  assert.match(body[1], /border-bottom:\s*1px solid #e8edf6/, 'a hairline under every row')
  assert.match(body[1], /border-right:\s*1px solid #eef2f8/, 'a hairline between columns')
  assert.match(STYLES, /\.end-page--proponent \.end-table tbody tr:nth-child\(even\) td\s*\{[^}]*background:\s*#fafbff/,
    'alternate rows are tinted')
})

test('the table is scoped to this page, not the other endorsement screens', () => {
  assert.ok(
    !/^\.end-page--fluid \.end-table thead th\s*\{[^}]*background:\s*#3c3b6e/m.test(STYLES),
    'the Dean/REC tables must keep their own treatment',
  )
  assert.match(STYLES, /\.end-page--proponent \.end-table \{\s*min-width:\s*980px/,
    'the wider table scrolls inside the existing end-table-scroll wrapper')

  // Column-specific widths belong to the requests table only: the certifications table
  // on the same page has different columns and must keep its own layout.
  assert.match(ENDORSEMENTS, /<table class="end-table end-table--requests">/)
  assert.ok(
    !/^\.end-page--proponent \.end-table (?:th|td):nth-child/m.test(STYLES),
    'column-width rules must not hit every .end-table on the page',
  )
  assert.match(STYLES, /\.end-page--proponent \.end-table--requests td:nth-child\(2\)\s*\{[^}]*min-width:\s*180px/)
})

test('the reference columns keep their order and the action stays a real button', () => {
  const head = ENDORSEMENTS.slice(
    ENDORSEMENTS.indexOf('<table class="end-table end-table--requests">'),
    ENDORSEMENTS.indexOf('</thead>'),
  )
  const order = ['Reference', 'Project title', 'Call for Proposal', 'College', 'Submitted', 'Status', 'Action']
    .map((label) => head.indexOf(`>${label}</th>`))
  assert.ok(order.every((index) => index > -1), 'all seven columns are present')
  assert.deepEqual([...order].sort((a, b) => a - b), order, 'the columns keep their order')

  assert.match(ENDORSEMENTS, /class="end-btn is-view"/, 'View is the shared blue tonal button')
  assert.match(ENDORSEMENTS, /:to="`\/endorsements\/\$\{request\.id\}`"/, 'the existing route is preserved')
  assert.match(STYLES, /\.end-page--proponent \.end-table th:last-child,[\s\S]{0,80}text-align:\s*right/,
    'the action column is right-aligned')
})

test('both Certification & Endorsement files have View before Download', () => {
  for (const file of ['submittedByFile', 'endorsementFile']) {
    const view = DETAILS.indexOf(`@click="viewFile(${file},`)
    const download = DETAILS.indexOf(`@click="downloadFile(${file})"`)
    assert.ok(view > -1, `${file} must have a View action`)
    assert.ok(download > -1, `${file} must keep its Download action`)
    assert.ok(view < download, 'View must be rendered before Download')
    assert.ok(DETAILS.lastIndexOf('class="file-actions"', view) > -1,
      'the pair sits in the shared file-actions row')
  }

  // Two eye icons, one per file action.
  assert.equal((DETAILS.match(/<path d="M2 12s3\.5-7 10-7/g) || []).length, 2)
})

test('View opens the same stored file through the existing authenticated preview helper', () => {
  // The path the preview uses is resolved exactly like the download path.
  assert.match(DETAILS, /const submittedByFile = computed\([\s\S]{0,160}submittedByFilePath \|\| proposal\.value\.submittedByFileName/,
    'the signature path is preferred, with the name as the legacy fallback')
  assert.match(DETAILS, /const endorsementFile = computed\([\s\S]{0,160}endorsementFilePath \|\| proposal\.value\.endorsementFileName/,
    'the endorsement path is preferred, with the name as the legacy fallback')
  assert.match(DETAILS, /import \{ downloadUploadedFile, previewUploadedFile \} from '@\/utils\/fileDownload'/)
  assert.match(DETAILS, /await previewUploadedFile\(path\)/, 'View opens the bytes, it does not download them')

  // An unsupported format is reported honestly instead of opening an empty tab.
  assert.match(DETAILS, /previewKindOf\(nameForKind\) === PREVIEW_UNSUPPORTED/)
  assert.match(DETAILS, /Preview Not Available/)
  assert.match(DETAILS, /Use Download to open it in its own application/)

  // A second click while the authenticated bytes are loading is ignored.
  assert.match(DETAILS, /if \(!path \|\| viewingFile\.value\) return/)
  assert.match(DETAILS, /:disabled="viewingFile === submittedByFile"/)
  assert.match(DETAILS, /:disabled="viewingFile === endorsementFile"/)
})

test('the two file actions share one compact button language', () => {
  assert.match(DETAILS, /\.file-actions\s*\{[^}]*display:\s*flex[^}]*gap:\s*8px/)
  const view = /\.btn-view-file\s*\{([^}]*)\}/.exec(DETAILS)
  const download = /\.btn-download\s*\{([^}]*)\}/.exec(DETAILS)
  assert.ok(view && download)
  assert.match(view[1], /padding:\s*8px 14px/)
  assert.match(view[1], /border-radius:\s*6px/)
  assert.match(view[1], /font-size:\s*var\(--ui-control-font-sm\)/)
  assert.match(download[1], /padding:\s*8px 16px/)
  assert.match(download[1], /border-radius:\s*6px/)
  assert.match(download[1], /font-size:\s*var\(--ui-control-font-sm\)/)
  assert.match(DETAILS, /@media \(max-width: 640px\)[\s\S]*?\.file-actions\s*\{[^}]*width:\s*100%/,
    'the pair wraps under the file details on a phone')
})
