<template>
    <div class="ovcaf-shell">
        <!-- ── Main Area ── -->
        <div class="ovcaf-main">
            <main class="ovcaf-body">
                <div v-if="loading" class="card">
                    <p style="padding:2rem;text-align:center;color:var(--text-muted)">Loading proposal details...</p>
                </div>

                <div v-else-if="!proposal" class="card empty-state-box">
                    <p class="empty-title">Proposal Not Found</p>
                    <p class="empty-desc">The proposal you are trying to view does not exist or has been removed.</p>
                    <router-link to="/ovcaf/inbox" class="btn btn-primary" style="margin-top:1rem">Back to
                        Inbox</router-link>
                </div>

                <div v-else>
                    <!-- Action Bar -->
                    <div class="action-bar">
                        <router-link to="/ovcaf/inbox" class="btn btn-secondary btn-sm">← Back to Inbox</router-link>
                        <router-link v-if="isPending" :to="`/ovcaf/validate/${proposal.id}`"
                            class="btn btn-primary">Proceed to Validation</router-link>
                    </div>

                    <!-- Header Card -->
                    <div class="card proposal-header-card">
                        <div class="header-layout">
                            <div class="title-section">
                                <div class="id-badge-row">
                                    <span class="proposal-id font-mono">{{ proposal.id }}</span>
                                    <span class="badge" :class="getStatusClass(proposal.status)">{{ proposal.status
                                        }}</span>
                                </div>
                                <h2 class="proposal-title">{{ proposal.title }}</h2>
                                <div class="meta-row">
                                    <span>{{ proposal.proponent }} (Proponent)</span>
                                    <span>{{ proposal.college }} &bull; {{ proposal.department }}</span>
                                    <span>Received on {{ formatDateLong(proposal.dateReceived) }}</span>
                                </div>
                            </div>
                            <div class="budget-box">
                                <p class="budget-label">Proposed Budget</p>
                                <h3 class="budget-value font-mono">{{ formatCurrency(proposal.budget) }}</h3>
                                <p class="funding-src">Source: <strong>{{ proposal.fundingSource }}</strong></p>
                            </div>
                        </div>
                    </div>

                    <!-- Two-column layout -->
                    <div class="details-grid">
                        <!-- Left -->
                        <div class="col-left">
                            <!-- Abstract -->
                            <div class="card">
                                <h4 class="card-title">Project Abstract</h4>
                                <p class="abstract-text">{{ proposal.abstract }}</p>
                                <div class="meta-grid-2">
                                    <div><span class="field-label">Project Timeline</span>
                                        <p class="field-value">{{ proposal.timeline }}</p>
                                    </div>
                                    <div><span class="field-label">Funding Program</span>
                                        <p class="field-value">{{ proposal.fundingSource }}</p>
                                    </div>
                                </div>
                            </div>

                            <!-- Budget Breakdown -->
                            <div class="card mt-6">
                                <h4 class="card-title">Detailed Line-Item Budget (LIB)</h4>
                                <div class="table-container">
                                    <table class="custom-table budget-table">
                                        <thead>
                                            <tr>
                                                <th>Category</th>
                                                <th>Item / Description</th>
                                                <th class="text-right">Qty</th>
                                                <th>Unit</th>
                                                <th class="text-right">Unit Cost</th>
                                                <th class="text-right">Total Cost</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr v-for="(row, idx) in proposal.budgetBreakdown" :key="idx">
                                                <td><span class="category-tag"
                                                        :class="getCategoryClass(row.category)">{{
                                                        getCategoryShort(row.category) }}</span></td>
                                                <td class="font-medium">{{ row.item }}</td>
                                                <td class="text-right font-mono">{{ row.quantity }}</td>
                                                <td>{{ row.unit }}</td>
                                                <td class="text-right font-mono">{{ formatCurrency(row.unitCost) }}</td>
                                                <td class="text-right font-mono font-semibold primary-text">{{
                                                    formatCurrency(row.total) }}</td>
                                            </tr>
                                        </tbody>
                                        <tfoot>
                                            <tr class="total-row">
                                                <td colspan="5" class="total-label">Total Proposed Budget</td>
                                                <td class="text-right font-mono total-amount">{{
                                                    formatCurrency(budgetTotal) }}</td>
                                            </tr>
                                        </tfoot>
                                    </table>
                                </div>
                            </div>

                            <!-- Documents -->
                            <div class="card mt-6">
                                <h4 class="card-title">Attached Financial Documents</h4>
                                <div class="documents-list">
                                    <div v-for="(doc, idx) in proposal.documents" :key="idx" class="document-item">
                                        <div>
                                            <p class="doc-name">{{ doc.name }}</p>
                                            <p class="doc-meta">{{ doc.size }} &bull; Uploaded {{ doc.uploadedAt }}</p>
                                        </div>
                                        <button class="btn btn-secondary btn-sm">Download</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Right -->
                        <div class="col-right">
                            <!-- OC Remarks -->
                            <div class="card oc-remarks-card" v-if="proposal.ocRemarks">
                                <h4 class="card-title">Office of the Chancellor Remarks</h4>
                                <blockquote class="chancellor-quote">"{{ proposal.ocRemarks }}"</blockquote>
                            </div>

                            <!-- Validation Result (completed proposals) -->
                            <div v-if="!isPending" class="card mt-6">
                                <h4 class="card-title">Validation Result</h4>
                                <div class="checklist-list">
                                    <div v-for="(label, key) in checklistLabels" :key="key" class="checklist-row"
                                        :class="{ checked: proposal.validationChecklist?.[key] }">
                                        <div class="checkbox-dot"
                                            :class="{ filled: proposal.validationChecklist?.[key] }">
                                            <span v-if="proposal.validationChecklist?.[key]">✓</span>
                                        </div>
                                        <span class="checklist-text">{{ label }}</span>
                                    </div>
                                </div>
                                <div class="ovcaf-remarks" v-if="proposal.remarks">
                                    <span class="remarks-label">OVCAF Evaluator Remarks:</span>
                                    <p class="remarks-text">"{{ proposal.remarks }}"</p>
                                </div>
                            </div>

                            <!-- History Timeline -->
                            <div class="card mt-6">
                                <h4 class="card-title">Proposal Workflow Timeline</h4>
                                <div class="timeline-wrapper">
                                    <div v-for="(event, idx) in proposal.history" :key="idx" class="timeline-item">
                                        <div class="tl-connector" v-if="idx < proposal.history.length - 1"></div>
                                        <div class="tl-icon" :class="getTlClass(event.action)"></div>
                                        <div class="tl-content">
                                            <div class="tl-header">
                                                <span class="stage-label">{{ event.stage }}</span>
                                                <span class="tl-date">{{ event.date }}</span>
                                            </div>
                                            <h5 class="tl-action">{{ event.action }}</h5>
                                            <p class="tl-user">By <strong>{{ event.user }}</strong></p>
                                            <p v-if="event.notes" class="tl-notes">"{{ event.notes }}"</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

// ── Shell State ───────────────────────────────────
const sidebarOpen = ref(false);
const dropdownOpen = ref(false);
const dropdownRef = ref(null);

// ── Page State ────────────────────────────────────
const loading = ref(true);
const proposal = ref(null);

// ── Computed ──────────────────────────────────────
const isPending = computed(() =>
    proposal.value?.status === 'Pending Validation' || proposal.value?.status === 'Under Validation'
);
const budgetTotal = computed(() =>
    (proposal.value?.budgetBreakdown || []).reduce((s, i) => s + i.total, 0)
);

const checklistLabels = {
    budgetComplete: 'Line-Item Budget is complete and matches all pages',
    budgetComputationCorrect: 'Budget computations are mathematically correct',
    supportingDocsComplete: 'All supporting financial files are attached and verified',
    expensesReasonable: 'Proposed expenses are reasonable and justified',
    fundingSourceIdentified: 'Funding source is officially identified and compliant',
    complianceVerified: 'Compliance with standard government/university funding guidelines verified'
};

// ── Mock Data ─────────────────────────────────────
// TODO: Replace mock data with backend API
const MOCK_PROPOSALS = [];

// ── localStorage helpers ──────────────────────────
const PROPOSAL_KEY = 'ovcaf_proposals';
const getAllProposals = () => {
    const s = localStorage.getItem(PROPOSAL_KEY);
    if (!s) { localStorage.setItem(PROPOSAL_KEY, JSON.stringify(MOCK_PROPOSALS)); return MOCK_PROPOSALS; }
    return JSON.parse(s);
};

// ── Lifecycle ─────────────────────────────────────
// TODO: Replace with backend API call
onMounted(async () => {
    loading.value = true;
    await new Promise(r => setTimeout(r, 300));
    const all = getAllProposals();
    const id = route.params.id;
    proposal.value = all.find(p => p.id === id) || null;
    loading.value = false;
    document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

const handleOutsideClick = (e) => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Helpers ───────────────────────────────────────
const formatCurrency = (v) => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 0 }).format(v);
const formatDateLong = (d) => d ? new Date(d).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) : '';
const getStatusClass = (s) => {
    if (!s) return '';
    if (s.includes('Pending') || s.includes('Under')) return 'badge-pending';
    if (s.includes('Approved')) return 'badge-approved';
    if (s.includes('Returned')) return 'badge-returned';
    if (s.includes('Rejected')) return 'badge-rejected';
    return 'badge-info';
};
const getCategoryShort = (c) => {
    if (!c) return c;
    if (c.includes('PS') || c.includes('Personal')) return 'PS';
    if (c.includes('MOOE') || c.includes('Maintenance')) return 'MOOE';
    if (c.includes('CO') || c.includes('Capital')) return 'CO';
    return c;
};
const getCategoryClass = (c) => {
    if (!c) return '';
    if (c.includes('PS') || c.includes('Personal')) return 'cat-ps';
    if (c.includes('MOOE') || c.includes('Maintenance')) return 'cat-mooe';
    if (c.includes('CO') || c.includes('Capital')) return 'cat-co';
    return 'cat-other';
};
const getTlClass = (action) => {
    const a = (action || '').toLowerCase();
    if (a.includes('approve') || a.includes('endorse') || a.includes('submit')) return 'tl-success';
    if (a.includes('return')) return 'tl-warning';
    if (a.includes('reject')) return 'tl-danger';
    return 'tl-info';
};
</script>

<style scoped>
.ovcaf-main{
    width:100%;
    max-width:1600px;
    margin:0 auto;
}

.ovcaf-body{
    padding:32px;
}

.action-bar{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-bottom:1.5rem;
}

.action-bar .btn{
    width:auto;
    flex:none;
    display:inline-flex;
    align-items:center;
    justify-content:center;

    padding:6px 14px;
    height:34px;

    font-size:.78rem;
    border-radius:8px;
}

.action-bar .btn-secondary{
    background: #ffffff;
    color: #475569;
    border: 1px solid #cbd5e1;
}

.action-bar .btn-secondary:hover{
    background: #f8fafc;
    border-color: #94a3b8;
    color: #1e293b;
}

.action-bar .btn-primary{
    background: #0f766e;
    border: 1px solid #0f766e;
    color: white;
}

.action-bar .btn-primary:hover{
    background: #115e59;
    border-color: #115e59;
    transform: translateY(-1px);
}


.card{
    background:#fff;
    border:1px solid #e5e7eb;
    border-radius:16px;
    box-shadow:0 8px 24px rgba(0,0,0,.05);
}

.custom-table{
    width:100%;
    border-collapse:collapse;
}

.custom-table th{
    padding:14px 18px;
}

.custom-table td{
    padding:16px 18px;
}

.proposal-header-card{
    padding:28px;
}

.header-layout {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 2rem;
    flex-wrap: wrap;
}

/* ===== Compact Buttons ===== */
.btn {
    padding: 0.45rem 1rem !important;
    min-height: 36px !important;
    height: 36px !important;
    line-height: 1 !important;

    display: inline-flex;
    align-items: center;
    justify-content: center;

    font-size: 0.82rem !important;
    font-weight: 600;
    border-radius: 6px !important;
}

/* Small buttons */
.btn-sm {
    padding: 0.35rem 0.9rem !important;
    min-height: 32px !important;
    height: 32px !important;
    font-size: 0.78rem !important;
}

/* Download button */
.document-item .btn {
    width: fit-content !important;
    display: inline-flex !important;
    align-items: center;
    justify-content: center;
    padding: 6px 16px !important;
    min-width: 110px;
    height: 34px;
}
/* Action bar buttons */
.action-bar .btn {
    height: 36px !important;
    min-height: 36px !important;
    padding: 0.45rem 1.2rem !important;
}

.budget-box{
    padding:24px;
}

.title-section {
    flex: 1.8;
}

.id-badge-row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.5rem;
}

.proposal-id {
    font-weight: 700;
    font-size: 1.05rem;
    color: var(--text-secondary);
    font-family: ui-monospace, monospace;
}

.proposal-title {
    font-size: 1.45rem;
    font-weight: 700;
    line-height: 1.3;
    color: var(--text-primary);
    margin-bottom: 0.75rem;
}

.meta-row {
    display: flex;
    gap: 1.5rem;
    flex-wrap: wrap;
    font-size: 0.85rem;
    color: var(--text-secondary);
}

.budget-box {
    flex: 1;
    min-width: 190px;
    background-color: #f8fafc;
    padding: 1rem;
    border-radius: var(--border-radius-sm);
    border: 1px solid #e2e8f0;
    text-align: right;
}

.budget-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--text-muted);
}

.budget-value {
    font-size: 1.65rem;
    font-weight: 700;
    color: var(--color-primary);
    margin: 0.2rem 0;
}

.funding-src {
    font-size: 0.8rem;
    color: var(--text-secondary);
}

.details-grid{
    display:grid;
    grid-template-columns:2fr 1fr;
    gap:24px;
    align-items:start;
}

@media (max-width: 1024px) {
    .details-grid {
        grid-template-columns: 1fr;
    }

    .budget-box {
        text-align: left;
    }
}

.abstract-text {
    font-size: 0.95rem;
    line-height: 1.65;
    color: var(--text-secondary);
}

.meta-grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    border-top: 1px solid #e2e8f0;
    padding-top: 1rem;
    margin-top: 1rem;
}

.field-label {
    font-size: 0.7rem;
    text-transform: uppercase;
    color: var(--text-muted);
    font-weight: 600;
    display: block;
}

.field-value {
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--text-primary);
}

.mt-6 {
    margin-top: 1.5rem;
}

.font-mono {
    font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.font-medium {
    font-weight: 500;
}

.font-semibold {
    font-weight: 600;
}

.primary-text {
    color: var(--color-primary);
}

.text-right {
    text-align: right;
}

.category-tag {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    font-size: 0.7rem;
    font-weight: 700;
}

.cat-ps {
    background-color: #e0f2fe;
    color: #0369a1;
}

.cat-mooe {
    background-color: #fef3c7;
    color: #b45309;
}

.cat-co {
    background-color: #f3e8ff;
    color: #6b21a8;
}

.cat-other {
    background-color: #e2e8f0;
    color: #475569;
}

.total-row td {
    border-top: 2px solid #94a3b8;
    border-bottom: none;
    font-weight: 700;
}

.total-label {
    text-align: right;
    font-size: 1rem;
    color: var(--text-primary);
}

.total-amount {
    font-size: 1.05rem;
    color: var(--color-primary);
}

.documents-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.document-item{
    padding:16px 18px;
}

.doc-name {
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--text-primary);
}

.doc-meta {
    font-size: 0.725rem;
    color: var(--text-muted);
}

.oc-remarks-card {
    border-left: 4px solid var(--color-accent);
}

.chancellor-quote {
    font-style: italic;
    color: var(--text-secondary);
    font-size: 0.9rem;
    border-left: 2px solid #cbd5e1;
    padding-left: 0.75rem;
    margin: 0;
}

.checklist-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    margin-bottom: 1rem;
}

.checklist-row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.5rem;
    border-radius: 6px;
}

.checklist-row.checked {
    background-color: var(--color-approved-bg);
}

.checkbox-dot {
    width: 22px;
    height: 22px;
    border: 2px solid #cbd5e1;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.8rem;
    font-weight: 700;
    color: white;
    flex-shrink: 0;
}

.checkbox-dot.filled {
    background-color: var(--color-approved);
    border-color: var(--color-approved);
}

.checklist-text {
    font-size: 0.85rem;
    color: var(--text-secondary);
}

.ovcaf-remarks {
    background-color: #f8fafc;
    border-left: 3px solid var(--color-primary);
    padding: 0.75rem;
    border-radius: 0 4px 4px 0;
}

.remarks-label {
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--text-secondary);
    text-transform: uppercase;
    display: block;
    margin-bottom: 0.25rem;
}

.remarks-text {
    font-size: 0.85rem;
    font-style: italic;
    color: var(--text-secondary);
}

.timeline-item{
    padding-bottom:28px;
}

.timeline-item {
    display: flex;
    gap: 1.25rem;
    position: relative;
    padding-bottom: 1.75rem;
}

.timeline-item:last-child {
    padding-bottom: 0;
}

.tl-connector {
    position: absolute;
    left: 20px;
    top: 42px;
    bottom: 0;
    width: 2px;
    background-color: #e2e8f0;
}

.tl-icon{
    width:44px;
    height:44px;
}

.tl-success {
    border-color: var(--color-approved);
    background-color: var(--color-approved-bg);
}

.tl-warning {
    border-color: var(--color-returned);
    background-color: var(--color-returned-bg);
}

.tl-danger {
    border-color: var(--color-rejected);
    background-color: var(--color-rejected-bg);
}

.tl-info {
    border-color: var(--color-info);
    background-color: var(--color-info-bg);
}

.tl-content{
    padding:18px;
}

.tl-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.25rem;
    flex-wrap: wrap;
    gap: 0.5rem;
}

.stage-label {
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    background-color: #e2e8f0;
    color: var(--text-secondary);
    padding: 0.125rem 0.5rem;
    border-radius: 4px;
}

.tl-date {
    font-size: 0.725rem;
    color: var(--text-muted);
}

.tl-action {
    font-size: 0.925rem;
    font-weight: 600;
    color: var(--text-primary);
}

.tl-user {
    font-size: 0.8rem;
    color: var(--text-secondary);
}

.tl-notes {
    margin-top: 0.5rem;
    font-size: 0.8rem;
    font-style: italic;
    color: var(--text-secondary);
    background-color: white;
    border-left: 3px solid var(--color-primary);
    padding: 0.375rem 0.75rem;
    border-radius: 0 4px 4px 0;
}

.empty-state-box {
    padding: 3rem 2rem;
    text-align: center;
}

.empty-title {
    font-size: 1.05rem;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 0.5rem;
}

.empty-desc {
    font-size: 0.875rem;
    color: var(--text-muted);
    margin-bottom: 1rem;
}
</style>
