/**
 * terminalReportCoverTeam.test.js
 * ---------------------------------------------------------------------------
 * Guards how the project team (the authors) is printed on the cover page and the first body
 * page of the Terminal Report.
 *
 * The field is a multiline input - "one author per line" - so the members are split on line
 * breaks only. A name like "Bagares, Xarzha Maurine" stays a single member (it is never split
 * on the comma), and each member prints on its own line instead of running together into one
 * string. The files are read as text / pure functions, like the other guards.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { projectTeamMembers } from './terminalReport.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const FORM = read('../components/TerminalReportForm.vue')
const CSS = read('../assets/terminalReportDocument.css')

test('each team member is its own entry', () => {
  assert.deepEqual(projectTeamMembers('Bagares, Xarzha Maurine\nLungay, Vanisa'), [
    'Bagares, Xarzha Maurine',
    'Lungay, Vanisa',
  ])
})

test('a name with a comma is never split into two members', () => {
  assert.deepEqual(projectTeamMembers('Bagares, Xarzha Maurine'), ['Bagares, Xarzha Maurine'])
})

test('any number of members is supported, blank lines skipped', () => {
  assert.deepEqual(projectTeamMembers('A, One\nLungay, Vanisa\nC, Three'), [
    'A, One',
    'Lungay, Vanisa',
    'C, Three',
  ])
  assert.deepEqual(projectTeamMembers('A, One\n\nB, Two\r\nC, Three\n   \n'), [
    'A, One',
    'B, Two',
    'C, Three',
  ])
})

test('a single-line or empty value is handled without inventing names', () => {
  assert.deepEqual(projectTeamMembers('Lungay, Vanisa'), ['Lungay, Vanisa'])
  assert.deepEqual(projectTeamMembers(''), [])
  assert.deepEqual(projectTeamMembers('   '), [])
  assert.deepEqual(projectTeamMembers(null), [])
})

test('the cover and the first page print one line per member', () => {
  assert.ok(FORM.includes('const coverTeamMembers = computed('), 'the members are computed')
  const loops = FORM.match(/v-for="\(member, index\) in coverTeamMembers"/g) || []
  assert.equal(loops.length, 2, 'the cover and the first body page each render the members')
  assert.ok(FORM.includes('class="tr-team-member"'), 'each member carries the member class')
  assert.ok(
    !FORM.includes('{{ model.projectTeam }}'),
    'the team is never printed as one run-together string'
  )
})

test('each member is a block, so it prints on its own line', () => {
  assert.ok(/\.tr-team-member\s*\{[^}]*display:\s*block/.test(CSS), 'the member class is a block')
})
