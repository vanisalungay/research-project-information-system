#!/usr/bin/env node
/**
 * Structural verification for the presentation ERD bundle.
 * Read-only: checks the JSON snapshot and the generated artifacts.
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const read = (name) => fs.readFileSync(path.join(here, name), 'utf8');
const fail = (message) => { console.error(`FAIL: ${message}`); process.exitCode = 1; };

const data = JSON.parse(read('rpis-business-erd.json'));
const tables = new Map(data.tables.map((t) => [t.name, t]));
const fks = data.foreign_keys;

if (tables.size !== 30) fail(`expected 30 tables, found ${tables.size}`);
if (fks.length !== 57) fail(`expected 57 foreign keys, found ${fks.length}`);
if (tables.has('database_migration_rollback_log')) fail('rollback registry present in tables');
if (data.excluded_tables.length !== 1 || data.excluded_tables[0] !== 'database_migration_rollback_log') {
  fail('excluded_tables must contain exactly database_migration_rollback_log');
}
for (const t of data.tables) {
  if (!t.columns.length) fail(`${t.name} has no columns`);
  const pks = t.columns.filter((c) => c.is_primary_key).length;
  if (!pks) fail(`${t.name} has no primary key column`);
}

for (const fk of fks) {
  if (!tables.has(fk.child_table)) fail(`${fk.constraint_name}: missing child table ${fk.child_table}`);
  if (!tables.has(fk.parent_table)) fail(`${fk.constraint_name}: missing parent table ${fk.parent_table}`);
  if (!fk.child_columns.length || !fk.parent_columns.length) fail(`${fk.constraint_name}: empty column list`);
  if (tables.has(fk.child_table) && !fk.child_columns.every((name) =>
      tables.get(fk.child_table).columns.some((c) => c.name === name))) {
    fail(`${fk.constraint_name}: child column not present on ${fk.child_table}`);
  }
  if (tables.has(fk.parent_table) && !fk.parent_columns.every((name) =>
      tables.get(fk.parent_table).columns.some((c) => c.name === name))) {
    fail(`${fk.constraint_name}: parent column not present on ${fk.parent_table}`);
  }
}
if (new Set(fks.map((f) => f.constraint_name)).size !== fks.length) fail('duplicate FK constraint names');

const expectedNorm = new Map([
  ['fk_proposals_status', ['proposals', ['status'], 'proposal_statuses', ['code']]],
  ['fk_psh_from_status', ['proposal_status_history', ['from_status'], 'proposal_statuses', ['code']]],
  ['fk_psh_to_status', ['proposal_status_history', ['to_status'], 'proposal_statuses', ['code']]],
]);
for (const [name, [child, childCols, parent, parentCols]] of expectedNorm) {
  const fk = fks.find((f) => f.constraint_name === name);
  if (!fk) { fail(`missing NORM-04 foreign key ${name}`); continue; }
  if (fk.child_table !== child || fk.parent_table !== parent ||
      fk.child_columns.join(',') !== childCols.join(',') ||
      fk.parent_columns.join(',') !== parentCols.join(',')) {
    fail(`${name} has unexpected endpoints`);
  }
  if (fk.on_delete !== 'RESTRICT' || fk.on_update !== 'NO ACTION' || fk.validated !== true) {
    fail(`${name} has unexpected actions or is not validated`);
  }
}

const svg = read('rpis-business-erd.svg');
const drawio = read('rpis-business-erd.drawio');
const dot = read('rpis-business-erd.dot');
const html = read('rpis-business-erd.html');

const svgNodes = (svg.match(/class="table-node"/g) || []).length;
const svgEdges = (svg.match(/class="fk-edge/g) || []).length;
const svgNorm = (svg.match(/class="fk-edge norm04"/g) || []).length;
if (svgNodes !== 30) fail(`SVG has ${svgNodes} table nodes`);
if (svgEdges !== 57) fail(`SVG has ${svgEdges} edges`);
if (svgNorm !== 3) fail(`SVG has ${svgNorm} NORM-04 edges`);
for (const fk of fks) {
  if (!svg.includes(fk.constraint_name)) fail(`SVG is missing FK ${fk.constraint_name}`);
}
for (const name of tables.keys()) {
  if (!svg.includes(`data-name="${name}"`)) fail(`SVG is missing table node ${name}`);
}
if (svg.includes('data-name="database_migration_rollback_log"')) fail('SVG contains a registry node');
if (!html.includes(svg)) fail('HTML does not embed the generated SVG');

const drawioVertices = (drawio.match(/vertex="1"/g) || []).length;
const drawioEdges = (drawio.match(/edge="1"/g) || []).length;
if (drawioVertices !== 32) fail(`draw.io has ${drawioVertices} vertices (expected 30 tables + 2 titles)`);
if (drawioEdges !== 57) fail(`draw.io has ${drawioEdges} edges`);
for (const name of tables.keys()) {
  if (!drawio.includes(`id="t_${name}"`)) fail(`draw.io is missing table ${name}`);
}

const dotEdges = (dot.match(/ -> /g) || []).length;
if (dotEdges !== 57) fail(`DOT has ${dotEdges} edges`);

console.log(`tables=${tables.size} foreign_keys=${fks.length}`);
console.log(`svg: nodes=${svgNodes} edges=${svgEdges} norm04=${svgNorm}`);
console.log(`drawio: vertices=${drawioVertices} edges=${drawioEdges}`);
console.log(`dot: edges=${dotEdges}`);
console.log(`registry excluded: ${!tables.has('database_migration_rollback_log') && !svg.includes('data-name="database_migration_rollback_log"')}`);
console.log(process.exitCode ? 'VERIFICATION FAILED' : 'VERIFICATION PASSED');
