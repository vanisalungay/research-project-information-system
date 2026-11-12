#!/usr/bin/env node
/**
 * Generate a presentation ERD from docs/erd/rpis-business-erd.json.
 *
 * Outputs (all beside this script):
 *   rpis-business-erd.svg     standalone, presentation-ready diagram
 *   rpis-business-erd.html    offline viewer with pan/zoom, labels, search
 *   rpis-business-erd.drawio  editable diagram for diagrams.net / VS Code
 *   rpis-business-erd.dot     Graphviz source for future re-rendering
 *
 * No network access, npm packages, database connection, or DDL is used.
 * The only excluded table is public.database_migration_rollback_log.
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const data = JSON.parse(fs.readFileSync(path.join(here, 'rpis-business-erd.json'), 'utf8'));

// ---------------------------------------------------------------------------
// Constants and layout
// ---------------------------------------------------------------------------

const ROW = 15;
const HEADER = 40;
const PAD = 8;
const GAP = 34;
const TOP = 130;

const NORM04 = new Set(['fk_proposals_status', 'fk_psh_from_status', 'fk_psh_to_status']);
const NORM04_ORDER = ['fk_proposals_status', 'fk_psh_from_status', 'fk_psh_to_status'];
const EXCLUDED = 'database_migration_rollback_log';

const GROUPS = {
  reference:   { label: 'Reference & identity',        color: '#1d4ed8', header: '#1e3a8a', fill: '#eff6ff' },
  proposal:    { label: 'Proposals core',              color: '#047857', header: '#065f46', fill: '#ecfdf5' },
  detail:      { label: 'Proposal detail',             color: '#0f766e', header: '#115e59', fill: '#f0fdfa' },
  endorsement: { label: 'Endorsement & certification', color: '#b45309', header: '#92400e', fill: '#fffbeb' },
  reporting:   { label: 'Reports & OVCAF',             color: '#7c3aed', header: '#5b21b6', fill: '#f5f3ff' },
  operations:  { label: 'Notifications & audit',       color: '#be123c', header: '#9f1239', fill: '#fff1f2' },
};

const TABLE_GROUP = {
  users: 'reference', proposal_statuses: 'reference', cooperating_agencies: 'reference',
  priority_agenda_sets: 'reference', priority_agendas: 'reference',
  proposal_announcements: 'reference',

  proposals: 'proposal', proposal_versions: 'proposal',
  proposal_status_history: 'proposal', proposal_announcement_updates: 'proposal',

  proposal_sites: 'detail', proposal_personnel: 'detail', proposal_budget: 'detail',
  proposal_log_frames: 'detail', proposal_other_projects: 'detail',
  proposal_limitations: 'detail', proposal_priority_agendas: 'detail',
  proposal_cooperating_agencies: 'detail', proposal_section_remarks: 'detail',
  proposal_reviews: 'detail',

  certifications: 'endorsement', endorsement_requests: 'endorsement',
  endorsement_request_events: 'endorsement', endorsement_letter_settings: 'endorsement',
  dean_assignments: 'endorsement',

  ovcaf_validations: 'reporting', quarterly_progress_reports: 'reporting',
  project_reports: 'reporting',

  notifications: 'operations', activity_logs: 'operations',
};

const COLUMNS = [
  { x: 40,   width: 360, group: 'reference', tables: [
      'users', 'proposal_statuses', 'cooperating_agencies', 'priority_agenda_sets',
      'priority_agendas', 'proposal_announcements', 'dean_assignments' ] },
  { x: 480,  width: 720, group: 'proposal', tables: [
      'proposals', 'proposal_versions', 'proposal_status_history',
      'proposal_announcement_updates' ] },
  { x: 1280, width: 360, group: 'detail', tables: [
      'proposal_sites', 'proposal_personnel', 'proposal_budget',
      'proposal_log_frames', 'proposal_other_projects', 'proposal_limitations',
      'proposal_priority_agendas', 'proposal_cooperating_agencies',
      'proposal_section_remarks', 'proposal_reviews' ] },
  { x: 1720, width: 380, group: 'endorsement', tables: [
      'endorsement_requests', 'certifications', 'endorsement_request_events',
      'endorsement_letter_settings', 'notifications', 'activity_logs' ] },
  { x: 2180, width: 380, group: 'reporting', tables: [
      'ovcaf_validations', 'quarterly_progress_reports', 'project_reports' ] },
];

// ---------------------------------------------------------------------------
// Validation and lookup maps
// ---------------------------------------------------------------------------

const tables = new Map(data.tables.map((t) => [t.name, t]));
const fks = data.foreign_keys;

if (tables.size !== 30) throw new Error(`Expected 30 business tables, found ${tables.size}`);
if (fks.length !== 57) throw new Error(`Expected 57 foreign keys, found ${fks.length}`);
if (tables.has(EXCLUDED)) throw new Error('The rollback registry must not be in the presentation ERD');
for (const fk of fks) {
  if (fk.child_table === EXCLUDED || fk.parent_table === EXCLUDED) {
    throw new Error(`A relationship to ${EXCLUDED} leaked into the snapshot`);
  }
}

const layout = new Map();
const tableColumn = new Map();
let maxBottom = TOP;
const seen = new Set();

for (const col of COLUMNS) {
  let y = TOP;
  for (const name of col.tables) {
    const t = tables.get(name);
    if (!t) throw new Error(`Layout names a missing table: ${name}`);
    if (seen.has(name)) throw new Error(`Table laid out twice: ${name}`);
    seen.add(name);
    const twoLane = t.columns.length > 60;
    const rows = twoLane ? Math.ceil(t.columns.length / 2) : t.columns.length;
    const h = HEADER + rows * ROW + PAD;
    const w = twoLane ? col.width : (col.width === 720 ? 360 : col.width);
    layout.set(name, { name, x: col.x, y, w, h, twoLane, group: col.group });
    tableColumn.set(name, col);
    y += h + GAP;
  }
  maxBottom = Math.max(maxBottom, y - GAP);
}

for (const name of tables.keys()) {
  if (!seen.has(name)) throw new Error(`Table is not in the layout: ${name}`);
}

const SVG_W = 2180 + 380 + 80;
const SVG_H = Math.round(maxBottom + 60);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const esc = (s) => String(s ?? '')
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const escAttr = (s) => esc(s).replace(/"/g, '&quot;').replace(/'/g, '&#39;');

function shortType(type) {
  return String(type)
    .replace(/character varying\((\d+)\)/g, 'varchar($1)')
    .replace(/character varying/g, 'varchar')
    .replace(/timestamp\((\d+)\) without time zone/g, 'timestamp($1)')
    .replace(/timestamp without time zone/g, 'timestamp')
    .replace(/time without time zone/g, 'time')
    .replace(/double precision/g, 'double');
}

function tableTheme(name) {
  return GROUPS[TABLE_GROUP[name] || 'reference'];
}

function columnPosition(tableName, columnName) {
  const t = tables.get(tableName);
  const L = layout.get(tableName);
  const i = t.columns.findIndex((c) => c.name === columnName);
  if (i < 0) return { row: 0, lane: 0 };
  if (!L.twoLane) return { row: i, lane: 0 };
  const half = Math.ceil(t.columns.length / 2);
  return i < half ? { row: i, lane: 0 } : { row: i - half, lane: 1 };
}

function columnY(tableName, columnName) {
  const L = layout.get(tableName);
  const { row } = columnPosition(tableName, columnName);
  return L.y + HEADER + row * ROW + ROW / 2;
}

function normalizeTable(name) {
  return String(name).replace(/^public\./, '').replace(/^"|"$/g, '');
}

// ---------------------------------------------------------------------------
// SVG table nodes
// ---------------------------------------------------------------------------

function tableSvg(t, L) {
  const theme = tableTheme(t.name);
  const cols = t.columns;
  const twoLane = L.twoLane;
  const half = Math.ceil(cols.length / 2);
  const lanes = twoLane ? [cols.slice(0, half), cols.slice(half)] : [cols];
  const laneW = (L.w - 8) / lanes.length;

  let s = `<g class="table-node" id="tbl-${escAttr(t.name)}" data-name="${escAttr(t.name)}" ` +
          `data-x="${L.x}" data-y="${L.y}" data-w="${L.w}" data-h="${L.h}">`;

  s += `<rect class="box-shadow" x="${L.x + 3}" y="${L.y + 4}" width="${L.w}" height="${L.h}" rx="5" fill="#0f172a" opacity="0.10"/>`;
  s += `<rect class="box" x="${L.x}" y="${L.y}" width="${L.w}" height="${L.h}" rx="5" ` +
       `fill="#ffffff" stroke="${theme.color}" stroke-width="1.4"/>`;
  s += `<path d="M ${L.x} ${L.y + HEADER} L ${L.x} ${L.y + 5} Q ${L.x} ${L.y} ${L.x + 5} ${L.y} ` +
       `L ${L.x + L.w - 5} ${L.y} Q ${L.x + L.w} ${L.y} ${L.x + L.w} ${L.y + 5} ` +
       `L ${L.x + L.w} ${L.y + HEADER} Z" fill="${theme.header}"/>`;

  s += `<text x="${L.x + 10}" y="${L.y + 24}" class="table-title">${esc(t.name)}</text>`;
  s += `<text x="${L.x + L.w - 10}" y="${L.y + 24}" text-anchor="end" class="table-sub">` +
       `${cols.length} cols</text>`;

  lanes.forEach((lane, li) => {
    const lx = L.x + 4 + li * laneW;
    lane.forEach((c, ri) => {
      const y = L.y + HEADER + ri * ROW;
      const fill = c.is_primary_key ? '#fef3c7' : (ri % 2 ? '#f8fafc' : '#ffffff');
      s += `<rect x="${lx}" y="${y}" width="${laneW}" height="${ROW}" fill="${fill}"/>`;
      s += `<line x1="${lx}" y1="${y + ROW}" x2="${lx + laneW}" y2="${y + ROW}" ` +
           `stroke="#e2e8f0" stroke-width="0.5"/>`;

      let bx = lx + 5;
      if (c.is_primary_key) {
        s += `<text x="${bx}" y="${y + 10.5}" class="badge pk">PK</text>`;
        bx += 17;
      }
      if (c.is_foreign_key) {
        s += `<text x="${bx}" y="${y + 10.5}" class="badge fk">FK</text>`;
      }

      s += `<text x="${lx + 44}" y="${y + 10.5}" class="col-name">${esc(c.name)}</text>`;
      s += `<text x="${lx + laneW - 6}" y="${y + 10.5}" text-anchor="end" class="col-type">` +
           `${esc(shortType(c.type))}${c.nullable ? ' ?' : ''}</text>`;
    });
    if (twoLane && li === 0) {
      s += `<line x1="${lx + laneW - 2}" y1="${L.y + HEADER}" x2="${lx + laneW - 2}" ` +
           `y2="${L.y + L.h - 4}" stroke="#e2e8f0" stroke-width="0.8"/>`;
    }
  });

  s += `</g>`;
  return s;
}

// ---------------------------------------------------------------------------
// SVG edges
// ---------------------------------------------------------------------------

function edgeGeometry(fk) {
  const childName = normalizeTable(fk.child_table);
  const parentName = normalizeTable(fk.parent_table);
  const child = layout.get(childName);
  const parent = layout.get(parentName);
  const cy = columnY(childName, fk.child_columns[0]);
  const py = columnY(parentName, fk.parent_columns[0]);

  if (childName === parentName) {
    const sx = child.x + child.w;
    return {
      d: `M ${sx} ${cy} C ${sx + 70} ${cy} ${sx + 70} ${py} ${sx} ${py}`,
      sx, sy: cy, tx: sx, ty: py, mx: sx + 45, my: (cy + py) / 2,
    };
  }

  const childCol = tableColumn.get(childName);
  const parentCol = tableColumn.get(parentName);
  const sameColumn = childCol === parentCol;

  if (sameColumn) {
    const useRight = child.x > 400;
    const sx = useRight ? child.x + child.w : child.x;
    const tx = useRight ? parent.x + parent.w : parent.x;
    const off = useRight ? 70 : -70;
    return {
      d: `M ${sx} ${cy} C ${sx + off} ${cy} ${tx + off} ${py} ${tx} ${py}`,
      sx, sy: cy, tx, ty: py, mx: (sx + tx) / 2 + (useRight ? 30 : -52), my: (cy + py) / 2,
    };
  }

  if (parent.x < child.x) {
    const sx = child.x;
    const sy = cy;
    const tx = parent.x + parent.w;
    const normIdx = NORM04_ORDER.indexOf(fk.constraint_name);
    const ty = py + (normIdx >= 0 ? (normIdx - 1) * 4 : 0);
    const off = Math.min(210, Math.max(55, (sx - tx) * 0.35));
    return {
      d: `M ${sx} ${sy} C ${sx - off} ${sy} ${tx + off} ${ty} ${tx} ${ty}`,
      sx, sy, tx, ty,
      mx: (sx + tx) / 2,
      my: (sy + ty) / 2 + (normIdx === 1 ? -20 : normIdx === 2 ? 20 : 0),
    };
  }

  const sx = child.x + child.w;
  const sy = cy;
  const tx = parent.x;
  const ty = py;
  const off = Math.min(210, Math.max(55, (tx - sx) * 0.35));
  return {
    d: `M ${sx} ${sy} C ${sx + off} ${sy} ${tx - off} ${ty} ${tx} ${ty}`,
    sx, sy, tx, ty,
    mx: (sx + tx) / 2, my: (sy + ty) / 2 - (Math.abs(sy - ty) < 30 ? 0 : 8),
  };
}

function edgeSvg(fk) {
  const g = edgeGeometry(fk);
  const isNorm = NORM04.has(fk.constraint_name);
  const colour = isNorm ? '#dc2626' : '#94a3b8';
  const marker = isNorm ? 'red' : 'gray';
  const width = isNorm ? 2.2 : 1.1;
  const opacity = isNorm ? 1 : 0.55;
  const cls = isNorm ? 'fk-edge norm04' : 'fk-edge';
  const title = `${fk.constraint_name}: ${normalizeTable(fk.child_table)}.${fk.child_columns.join(',')} -> ` +
                `${normalizeTable(fk.parent_table)}.${fk.parent_columns.join(',')} ` +
                `(ON DELETE ${fk.on_delete}, ON UPDATE ${fk.on_update}${fk.validated ? ', validated' : ''})`;
  const labelText = isNorm ? `NORM-04 · ${fk.constraint_name}` : fk.constraint_name;
  const labelCls = isNorm ? 'fk-label norm04-label' : 'fk-label';

  let s = `<path class="${cls}" d="${g.d}" fill="none" stroke="${colour}" stroke-width="${width}" ` +
          `stroke-opacity="${opacity}" marker-start="url(#many-${marker})" ` +
          `marker-end="url(#one-${marker})"><title>${esc(title)}</title></path>`;
  s += `<text class="${labelCls}" x="${g.mx}" y="${g.my}" text-anchor="middle">${esc(labelText)}</text>`;
  return s;
}

// ---------------------------------------------------------------------------
// SVG document
// ---------------------------------------------------------------------------

function buildSvg() {
  const edges = fks.map(edgeSvg).join('\n');
  const nodes = data.tables.map((t) => tableSvg(t, layout.get(t.name))).join('\n');

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${SVG_W}" height="${SVG_H}" viewBox="0 0 ${SVG_W} ${SVG_H}">
  <title>RPIS business ERD - 30 tables, 57 foreign keys</title>
  <desc>Presentation ERD generated from the rpis_db catalog. Excludes public.database_migration_rollback_log.</desc>
  <defs>
    <marker id="one-gray" viewBox="0 0 10 10" refX="8.5" refY="5" markerWidth="7" markerHeight="7" orient="auto">
      <path d="M 2,5 L 8,5 M 8,2 L 8,8" stroke="#94a3b8" stroke-width="1.2" fill="none"/>
    </marker>
    <marker id="many-gray" viewBox="0 0 10 10" refX="1.2" refY="5" markerWidth="8" markerHeight="8" orient="auto">
      <path d="M 9,5 L 1,1 M 9,5 L 1,5 M 9,5 L 1,9" stroke="#94a3b8" stroke-width="1.2" fill="none"/>
    </marker>
    <marker id="one-red" viewBox="0 0 10 10" refX="8.5" refY="5" markerWidth="7" markerHeight="7" orient="auto">
      <path d="M 2,5 L 8,5 M 8,2 L 8,8" stroke="#dc2626" stroke-width="1.5" fill="none"/>
    </marker>
    <marker id="many-red" viewBox="0 0 10 10" refX="1.2" refY="5" markerWidth="8" markerHeight="8" orient="auto">
      <path d="M 9,5 L 1,1 M 9,5 L 1,5 M 9,5 L 1,9" stroke="#dc2626" stroke-width="1.5" fill="none"/>
    </marker>
    <style>
      .table-title { font-family: "Segoe UI", Arial, sans-serif; font-size: 11px; font-weight: 700; fill: #ffffff; }
      .table-sub   { font-family: "Segoe UI", Arial, sans-serif; font-size: 8px; fill: #e2e8f0; }
      .col-name    { font-family: Consolas, "Cascadia Mono", monospace; font-size: 8.5px; fill: #0f172a; }
      .col-type    { font-family: Consolas, "Cascadia Mono", monospace; font-size: 7.5px; fill: #64748b; }
      .badge       { font-family: "Segoe UI", Arial, sans-serif; font-size: 6.5px; font-weight: 700; letter-spacing: .2px; }
      .badge.pk    { fill: #b45309; }
      .badge.fk    { fill: #0369a1; }
      .fk-label    { display: none; font-family: "Segoe UI", Arial, sans-serif; font-size: 7px; fill: #475569;
                     paint-order: stroke; stroke: #ffffff; stroke-width: 3px; }
      .norm04-label{ display: block; fill: #b91c1c; font-weight: 700; }
      .table-heading { font-family: "Segoe UI", Arial, sans-serif; }
      .table-node.highlight .box { stroke: #f59e0b; stroke-width: 3.5; }
      .table-node.highlight .box-shadow { opacity: 0.22; }
    </style>
  </defs>

  <rect width="100%" height="100%" fill="#f8fafc"/>

  <g class="erd-header">
    <text x="40" y="44" class="table-heading" font-size="24" font-weight="700" fill="#0f172a">RPIS Business ERD</text>
    <text x="40" y="66" class="table-heading" font-size="10.5" fill="#475569">30 business tables &#183; 57 foreign keys &#183; public schema &#183; excludes public.database_migration_rollback_log (migration infrastructure)</text>
    <line x1="40" y1="80" x2="${SVG_W - 40}" y2="80" stroke="#cbd5e1" stroke-width="1"/>
    <g transform="translate(40,98)">
      <line x1="0" y1="0" x2="34" y2="0" stroke="#94a3b8" stroke-width="1.4"/>
      <text x="42" y="3" class="table-heading" font-size="9" fill="#475569">existing relationship</text>
      <line x1="200" y1="0" x2="234" y2="0" stroke="#dc2626" stroke-width="2.4"/>
      <text x="242" y="3" class="table-heading" font-size="9" fill="#b91c1c" font-weight="700">NORM-04 relationship</text>
      <text x="410" y="3" class="table-heading" font-size="9" fill="#475569"><tspan font-weight="700" fill="#b45309">PK</tspan> primary key   <tspan font-weight="700" fill="#0369a1">FK</tspan> foreign key   <tspan fill="#94a3b8">?</tspan> nullable</text>
      <text x="720" y="3" class="table-heading" font-size="9" fill="#475569">generated from the live catalog on ${esc(data.generated_at)}</text>
    </g>
  </g>

  <g class="edges">
${edges}
  </g>

  <g class="tables">
${nodes}
  </g>
</svg>`;
}

// ---------------------------------------------------------------------------
// HTML viewer
// ---------------------------------------------------------------------------

function buildHtml(svg) {
  const names = [...tables.keys()].sort();
  const options = names.map((n) => `<option value="${escAttr(n)}"></option>`).join('');
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>RPIS Business ERD</title>
<style>
  :root { color-scheme: light; }
  * { box-sizing: border-box; }
  html, body { margin: 0; height: 100%; overflow: hidden; background: #eef2f7;
               font-family: "Segoe UI", Arial, sans-serif; }
  #toolbar {
    position: fixed; inset: 12px 12px auto 12px; z-index: 10; display: flex; gap: 8px;
    align-items: center; flex-wrap: wrap; padding: 8px 10px; border: 1px solid #cbd5e1;
    border-radius: 10px; background: rgba(255,255,255,.94); box-shadow: 0 8px 24px rgba(15,23,42,.12);
  }
  #toolbar .brand { font-weight: 700; color: #0f172a; margin-right: 6px; white-space: nowrap; }
  #toolbar .brand small { display: block; font-weight: 400; color: #64748b; font-size: 11px; }
  #toolbar button, #toolbar input {
    font: inherit; font-size: 12px; padding: 6px 10px; border: 1px solid #cbd5e1;
    border-radius: 7px; background: #fff; color: #0f172a; cursor: pointer;
  }
  #toolbar button:hover { background: #f1f5f9; }
  #toolbar button.active { background: #dbeafe; border-color: #93c5fd; color: #1d4ed8; }
  #toolbar input { width: 170px; cursor: text; }
  #stage { position: fixed; inset: 0; cursor: grab; overflow: hidden; }
  #stage.dragging { cursor: grabbing; }
  #canvas { position: absolute; transform-origin: 0 0; will-change: transform; }
  #canvas svg { display: block; box-shadow: 0 10px 40px rgba(15,23,42,.12); }
  body.show-labels .fk-label { display: block; }
  #hint { position: fixed; right: 14px; bottom: 12px; z-index: 10; font-size: 11px;
          color: #64748b; background: rgba(255,255,255,.9); padding: 5px 9px;
          border-radius: 7px; border: 1px solid #e2e8f0; }
  @media print {
    @page { size: A3 landscape; margin: 8mm; }
    #toolbar, #hint { display: none !important; }
    html, body { overflow: visible; background: #fff; height: auto; }
    #stage { position: static; overflow: visible; }
    #canvas { position: static; transform: none !important; }
    #canvas svg { box-shadow: none; width: 100%; height: auto; }
  }
</style>
</head>
<body>
<div id="toolbar">
  <div class="brand">RPIS Business ERD<small>30 tables &#183; 57 relationships</small></div>
  <button id="zoom-out" title="Zoom out">&#8722;</button>
  <button id="zoom-in" title="Zoom in">+</button>
  <button id="fit" title="Fit to window">Fit</button>
  <button id="labels" title="Show or hide foreign-key names">FK labels</button>
  <input id="find" list="table-names" placeholder="Find a table..." autocomplete="off">
  <datalist id="table-names">${options}</datalist>
  <button id="clear" title="Clear highlight">Clear</button>
</div>
<div id="stage">
  <div id="canvas">
${svg}
  </div>
</div>
<div id="hint">Drag to pan &#183; wheel to zoom &#183; hover a line for its constraint</div>
<script>
(() => {
  const stage = document.getElementById('stage');
  const canvas = document.getElementById('canvas');
  const svg = canvas.querySelector('svg');
  let scale = 1, tx = 20, ty = 80, dragging = false, lastX = 0, lastY = 0;

  function apply() {
    canvas.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')';
  }
  function fit() {
    const vw = window.innerWidth, vh = window.innerHeight;
    const w = svg.viewBox.baseVal.width || svg.width.baseVal.value;
    const h = svg.viewBox.baseVal.height || svg.height.baseVal.value;
    scale = Math.min((vw - 40) / w, (vh - 100) / h);
    scale = Math.max(0.05, Math.min(1.5, scale));
    tx = 20;
    ty = 80;
    apply();
  }
  function zoomAt(factor, cx, cy) {
    const next = Math.max(0.08, Math.min(3, scale * factor));
    tx = cx - (cx - tx) * (next / scale);
    ty = cy - (cy - ty) * (next / scale);
    scale = next;
    apply();
  }

  document.getElementById('fit').onclick = fit;
  document.getElementById('zoom-in').onclick = () => zoomAt(1.18, window.innerWidth / 2, window.innerHeight / 2);
  document.getElementById('zoom-out').onclick = () => zoomAt(1 / 1.18, window.innerWidth / 2, window.innerHeight / 2);
  document.getElementById('labels').onclick = (e) => {
    document.body.classList.toggle('show-labels');
    e.currentTarget.classList.toggle('active', document.body.classList.contains('show-labels'));
  };
  document.getElementById('clear').onclick = () => {
    document.querySelectorAll('.table-node.highlight').forEach((n) => n.classList.remove('highlight'));
  };

  const find = document.getElementById('find');
  function locate(name) {
    const node = document.querySelector('.table-node[data-name="' + CSS.escape(name) + '"]');
    if (!node) return;
    document.querySelectorAll('.table-node.highlight').forEach((n) => n.classList.remove('highlight'));
    node.classList.add('highlight');
    const x = Number(node.dataset.x), y = Number(node.dataset.y);
    const w = Number(node.dataset.w), h = Number(node.dataset.h);
    scale = Math.max(0.55, Math.min(1.2, scale));
    tx = window.innerWidth / 2 - (x + w / 2) * scale;
    ty = window.innerHeight / 2 - (y + h / 2) * scale;
    apply();
  }
  find.addEventListener('change', () => locate(find.value.trim()));
  find.addEventListener('keydown', (e) => { if (e.key === 'Enter') locate(find.value.trim()); });

  stage.addEventListener('mousedown', (e) => {
    if (e.button !== 0) return;
    dragging = true; lastX = e.clientX; lastY = e.clientY;
    stage.classList.add('dragging');
  });
  window.addEventListener('mousemove', (e) => {
    if (!dragging) return;
    tx += e.clientX - lastX; ty += e.clientY - lastY;
    lastX = e.clientX; lastY = e.clientY;
    apply();
  });
  window.addEventListener('mouseup', () => { dragging = false; stage.classList.remove('dragging'); });
  stage.addEventListener('wheel', (e) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.12 : 1 / 1.12;
    const rect = stage.getBoundingClientRect();
    zoomAt(factor, e.clientX - rect.left, e.clientY - rect.top);
  }, { passive: false });

  window.addEventListener('resize', () => {});
  fit();
})();
</script>
</body>
</html>`;
}

// ---------------------------------------------------------------------------
// draw.io
// ---------------------------------------------------------------------------

function drawioLabel(t, L) {
  const theme = tableTheme(t.name);
  const cols = t.columns;
  const half = Math.ceil(cols.length / 2);
  const lanes = L.twoLane ? [cols.slice(0, half), cols.slice(half)] : [cols];

  const row = (c) => {
    const badge = c.is_primary_key ? 'PK' : (c.is_foreign_key ? 'FK' : '');
    const badgeColour = c.is_primary_key ? '#b45309' : '#0369a1';
    return `<table style="border-collapse:collapse;width:100%;table-layout:fixed"><tr style="height:15px">` +
      `<td style="width:24px;color:${badgeColour};font-size:6px;font-weight:700">${badge}</td>` +
      `<td style="text-align:left;white-space:nowrap;font-size:8px;overflow:hidden">${escHtml(c.name)}${c.nullable ? '<span style="color:#94a3b8">?</span>' : ''}</td>` +
      `<td style="text-align:right;color:#64748b;font-size:7px;white-space:nowrap">${escHtml(shortType(c.type))}</td>` +
      `</tr></table>`;
  };

  const laneHtml = lanes.map((lane, i) => {
    const body = lane.map(row).join('');
    const border = i === 0 && lanes.length > 1 ? 'border-right:1px solid #e2e8f0;padding-right:3px;' : 'padding-left:3px;';
    return `<td style="vertical-align:top;width:${100 / lanes.length}%;${border}">${body}</td>`;
  }).join('');

  return `<div style="font-family:Segoe UI,Arial;font-size:9px;line-height:1.15;background:#ffffff">` +
    `<div style="background:${theme.header};color:#ffffff;padding:5px 7px;font-weight:700;font-size:11px">` +
    `${escHtml(t.name)}<span style="float:right;font-weight:400;font-size:8px;opacity:.85">${cols.length} cols</span></div>` +
    `<table style="border-collapse:collapse;width:100%;table-layout:fixed"><tr>${laneHtml}</tr></table>` +
    `</div>`;
}

function escHtml(s) {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function buildDrawio() {
  const cells = [];
  cells.push('<mxCell id="0"/>');
  cells.push('<mxCell id="1" parent="0"/>');
  cells.push(`<mxCell id="title" value="RPIS Business ERD &#8212; 30 tables &#183; 57 foreign keys" ` +
    `style="text;html=1;align=left;verticalAlign=middle;fontSize=22;fontStyle=1;fontColor=#0f172a;" ` +
    `vertex="1" parent="1"><mxGeometry x="40" y="20" width="900" height="34" as="geometry"/></mxCell>`);
  cells.push(`<mxCell id="subtitle" value="public schema &#183; excludes public.database_migration_rollback_log (migration infrastructure) &#183; red edges are the NORM-04 relationships" ` +
    `style="text;html=1;align=left;verticalAlign=middle;fontSize=11;fontColor=#475569;" ` +
    `vertex="1" parent="1"><mxGeometry x="40" y="56" width="1200" height="24" as="geometry"/></mxCell>`);

  for (const t of data.tables) {
    const L = layout.get(t.name);
    const theme = tableTheme(t.name);
    cells.push(`<mxCell id="t_${escAttr(t.name)}" value="${escAttr(drawioLabel(t, L))}" ` +
      `style="rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=0;` +
      `strokeColor=${theme.color};strokeWidth=1.4;fillColor=#ffffff;" vertex="1" parent="1">` +
      `<mxGeometry x="${L.x}" y="${L.y}" width="${L.w}" height="${L.h}" as="geometry"/></mxCell>`);
  }

  let edgeNo = 1;
  for (const fk of fks) {
    const child = normalizeTable(fk.child_table);
    const parent = normalizeTable(fk.parent_table);
    const isNorm = NORM04.has(fk.constraint_name);
    const colour = isNorm ? '#dc2626' : '#94a3b8';
    const width = isNorm ? 2 : 1;
    const label = isNorm ? `NORM-04: ${fk.constraint_name}` : fk.constraint_name;
    cells.push(`<mxCell id="e_${edgeNo++}" value="${escAttr(label)}" ` +
      `style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;` +
      `startArrow=ERmany;startFill=0;endArrow=ERone;endFill=0;strokeColor=${colour};strokeWidth=${width};` +
      `fontSize=7;labelBackgroundColor=#ffffff;${isNorm ? 'fontStyle=1;fontColor=#b91c1c;' : 'fontColor=#475569;'}" ` +
      `edge="1" parent="1" source="t_${escAttr(child)}" target="t_${escAttr(parent)}">` +
      `<mxGeometry relative="1" as="geometry"/></mxCell>`);
  }

  return `<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net" modified="${escAttr(data.generated_at)}" agent="RPIS ERD generator" version="24.0.0">
  <diagram id="rpis-business-erd" name="RPIS Business ERD">
    <mxGraphModel dx="1600" dy="1000" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="2600" pageHeight="2200" math="0" shadow="0">
      <root>
        ${cells.join('\n        ')}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>`;
}

// ---------------------------------------------------------------------------
// Graphviz DOT
// ---------------------------------------------------------------------------

function dotLabel(t) {
  const rows = t.columns.map((c) => {
    const badge = c.is_primary_key ? 'PK' : (c.is_foreign_key ? 'FK' : '');
    return `<TR><TD ALIGN="LEFT"><FONT COLOR="${c.is_primary_key ? '#b45309' : '#0369a1'}" POINT-SIZE="5">${badge}</FONT></TD>` +
           `<TD ALIGN="LEFT">${escHtml(c.name)}${c.nullable ? '<FONT COLOR="#94a3b8">?</FONT>' : ''}</TD>` +
           `<TD ALIGN="RIGHT"><FONT COLOR="#64748b" POINT-SIZE="7">${escHtml(shortType(c.type))}</FONT></TD></TR>`;
  }).join('');
  const theme = tableTheme(t.name);
  return `<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="1" COLOR="${theme.color}">` +
         `<TR><TD COLSPAN="3" BGCOLOR="${theme.header}" ALIGN="LEFT"><FONT COLOR="#ffffff" POINT-SIZE="9"><B>${escHtml(t.name)}</B></FONT></TD></TR>` +
         rows + `</TABLE>>`;
}

function buildDot() {
  const nodes = data.tables.map((t) =>
    `  "${t.name}" [label=${dotLabel(t)}];`).join('\n');
  const edges = fks.map((fk) => {
    const isNorm = NORM04.has(fk.constraint_name);
    return `  "${normalizeTable(fk.child_table)}" -> "${normalizeTable(fk.parent_table)}" ` +
      `[label="${escAttr(fk.constraint_name)}"${isNorm ? ' color="#dc2626" penwidth=2 fontcolor="#b91c1c"' : ''}];`;
  }).join('\n');
  return `// RPIS business ERD - generated from rpis-business-erd.json
// Render with: dot -Tsvg rpis-business-erd.dot -o rpis-business-erd-graphviz.svg
digraph rpis_business_erd {
  graph [rankdir=LR, fontname="Segoe UI", bgcolor="#ffffff", pad=0.4, nodesep=0.55,
         ranksep=1.2, label="RPIS Business ERD - 30 tables, 57 foreign keys (excludes database_migration_rollback_log)",
         labelloc=t, fontsize=18];
  node [shape=plaintext, fontname="Segoe UI", fontsize=8];
  edge [color="#94a3b8", arrowsize=0.7, dir=both, arrowtail=crow, arrowhead=tee,
        fontname="Segoe UI", fontsize=7, fontcolor="#475569"];

${nodes}

${edges}
}
`;
}

// ---------------------------------------------------------------------------
// Write and verify
// ---------------------------------------------------------------------------

const svg = buildSvg();
fs.writeFileSync(path.join(here, 'rpis-business-erd.svg'), svg, 'utf8');
fs.writeFileSync(path.join(here, 'rpis-business-erd.html'), buildHtml(svg), 'utf8');
fs.writeFileSync(path.join(here, 'rpis-business-erd.drawio'), buildDrawio(), 'utf8');
fs.writeFileSync(path.join(here, 'rpis-business-erd.dot'), buildDot(), 'utf8');

const tableNodes = (svg.match(/class="table-node"/g) || []).length;
const edges = (svg.match(/class="fk-edge/g) || []).length;
const normEdges = (svg.match(/class="fk-edge norm04"/g) || []).length;
const hasRegistry = svg.includes(`data-name="${EXCLUDED}"`) || svg.includes(`id="tbl-${EXCLUDED}"`);
const normNames = [...NORM04].filter((n) => svg.includes(n)).length;

console.log(`tables=${tableNodes} fks=${edges} norm04_edges=${normEdges} norm04_names=${normNames}`);
console.log(`registry_present=${hasRegistry}`);
console.log(`svg=${SVG_W}x${SVG_H}`);

if (tableNodes !== 30) throw new Error(`SVG table count ${tableNodes} != 30`);
if (edges !== 57) throw new Error(`SVG edge count ${edges} != 57`);
if (normEdges !== 3) throw new Error(`NORM-04 edge count ${normEdges} != 3`);
if (normNames !== 3) throw new Error('Not all NORM-04 constraints are named in the SVG');
if (hasRegistry) throw new Error('The rollback registry appears in the SVG');
