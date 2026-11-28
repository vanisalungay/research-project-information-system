<template>
    <div class="ovcaf-shell">
        <!-- ── Main Area ── -->
        <div class="ovcaf-main">
            <header class="ovcaf-topbar">
            <div>
                <h2 class="ovcaf-page-title">Financial Reports</h2>
            </div>
            </header>

            <main class="ovcaf-body">
                <div v-if="loading" class="card">
                    <p style="padding:2rem;text-align:center;color:var(--text-muted)">Loading report analytics...</p>
                </div>

                <div v-else class="reports-content">
                    <!-- Overview Cards -->
                    <div class="overview-grid">
                        <div class="card overview-card">
                            <div class="ov-icon-box" style="background-color:var(--color-info-bg)"><span>💰</span></div>
                            <div>
                                <p class="ov-label">Total Validated Budget</p>
                                <h3 class="ov-value font-mono">{{ formatCurrency(totalBudget) }}</h3>
                                <p class="ov-sub">Across all colleges</p>
                            </div>
                        </div>
                        <div class="card overview-card">
                            <div class="ov-icon-box" style="background-color:var(--color-pending-bg)"><span>📊</span>
                            </div>
                            <div>
                                <p class="ov-label">Average Project Budget</p>
                                <h3 class="ov-value font-mono">{{ formatCurrency(avgBudget) }}</h3>
                                <p class="ov-sub">Per processed proposal</p>
                            </div>
                        </div>
                        <div class="card overview-card">
                            <div class="ov-icon-box" style="background-color:var(--color-approved-bg)"><span>✅</span>
                            </div>
                            <div>
                                <p class="ov-label">Endorsement Rate</p>
                                <h3 class="ov-value font-mono">{{ endorsementRate }}%</h3>
                                <p class="ov-sub">Budget approval percentage</p>
                            </div>
                        </div>
                    </div>

                    <!-- Layout Grid -->
                    <div class="reports-grid">
            
                        <!-- Status Breakdown -->
                        <div class="card">
                            <h4 class="card-title">Decisions by Status</h4>
                            <p class="section-sub">Volume share and budget totals per validation decision.</p>
                            <div class="metrics-list">
                                <div class="metric-block">
                                    <div class="metric-header"><span class="badge badge-approved">Approved</span><span
                                            class="metric-budget font-mono">{{ formatCurrency(statusBudgets.Approved)
                                            }}</span></div>
                                    <div class="progress-bg">
                                        <div class="progress-fill fill-approved"
                                            :style="{ width: getStatusPct(statusCounts.Approved) + '%' }"></div>
                                    </div>
                                    <span class="metric-sub">{{ statusCounts.Approved }} proposals ({{
                                        getStatusPct(statusCounts.Approved) }}%)</span>
                                </div>
                
                                <div class="metric-block">
                                    <div class="metric-header"><span class="badge badge-pending">Pending</span><span
                                            class="metric-budget font-mono">{{ formatCurrency(statusBudgets.Pending)
                                            }}</span></div>
                                    <div class="progress-bg">
                                        <div class="progress-fill fill-pending"
                                            :style="{ width: getStatusPct(statusCounts.Pending) + '%' }"></div>
                                    </div>
                                    <span class="metric-sub">{{ statusCounts.Pending }} proposals ({{
                                        getStatusPct(statusCounts.Pending) }}%)</span>
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

// ── Shell State ───────────────────────────────────
const sidebarOpen = ref(false);
const dropdownOpen = ref(false);
const dropdownRef = ref(null);


// ── Page State ────────────────────────────────────
const loading = ref(true);

// ── Mock Data ─────────────────────────────────────
// TODO: Replace mock data with backend API
const MOCK_PROPOSALS = [
    { id: 'PRP-2026-042', college: 'College of Engineering and Technology', budget: 875000, status: 'Pending Validation' },
    { id: 'PRP-2026-039', college: 'College of Science and Mathematics', budget: 620000, status: 'Pending Validation' },
    { id: 'PRP-2026-035', college: 'College of Agriculture', budget: 980000, status: 'Pending Validation' },
    { id: 'PRP-2026-031', college: 'College of Science and Mathematics', budget: 540000, status: 'Under Validation' },
    { id: 'PRP-2026-025', college: 'College of Arts and Social Sciences', budget: 1630000, status: 'Approved & Endorsed' },
    { id: 'PRP-2026-018', college: 'College of Engineering and Technology', budget: 2150000, status: 'Returned for Revision' },
    { id: 'PRP-2026-012', college: 'College of Education', budget: 380000, status: 'Approved & Endorsed' },
    { id: 'PRP-2026-005', college: 'College of Information Technology', budget: 450000, status: 'Rejected' }
];

const MOCK_MONTHLY = [
    { month: 'Jan', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Feb', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Mar', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Apr', budget: 450000, approved: 0, returned: 0, rejected: 1, isCurrent: false },
    { month: 'May', budget: 2150000, approved: 0, returned: 1, rejected: 0, isCurrent: false },
    { month: 'Jun', budget: 2640000, approved: 2, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Jul', budget: 3015000, approved: 0, returned: 0, rejected: 0, isCurrent: true },
    { month: 'Aug', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Sep', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Oct', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Nov', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false },
    { month: 'Dec', budget: 0, approved: 0, returned: 0, rejected: 0, isCurrent: false }
];

// ── localStorage helpers ──────────────────────────
const PROPOSAL_KEY = 'ovcaf_proposals';
const getAllProposals = () => {
    const s = localStorage.getItem(PROPOSAL_KEY);
    if (!s) { localStorage.setItem(PROPOSAL_KEY, JSON.stringify(MOCK_PROPOSALS)); return MOCK_PROPOSALS; }
    return JSON.parse(s);
};

// ── Computed from proposals ───────────────────────
const proposals = ref([]);

const statusCounts = computed(() => {
    const c = { Approved: 0, Returned: 0, Rejected: 0, Pending: 0 };
    proposals.value.forEach(p => {
        if (p.status === 'Approved & Endorsed') c.Approved++;
        else if (p.status === 'Returned for Revision') c.Returned++;
        else if (p.status === 'Rejected') c.Rejected++;
        else c.Pending++;
    });
    return c;
});

const statusBudgets = computed(() => {
    const b = { Approved: 0, Returned: 0, Rejected: 0, Pending: 0 };
    proposals.value.forEach(p => {
        if (p.status === 'Approved & Endorsed') b.Approved += p.budget;
        else if (p.status === 'Returned for Revision') b.Returned += p.budget;
        else if (p.status === 'Rejected') b.Rejected += p.budget;
        else b.Pending += p.budget;
    });
    return b;
});

const totalBudget = computed(() => Object.values(statusBudgets.value).reduce((s, v) => s + v, 0));
const totalCount = computed(() => proposals.value.length);
const avgBudget = computed(() => totalCount.value > 0 ? totalBudget.value / totalCount.value : 0);
const endorsementRate = computed(() => {
    const reviewed = statusCounts.value.Approved + statusCounts.value.Returned + statusCounts.value.Rejected;
    return reviewed > 0 ? Math.round((statusCounts.value.Approved / reviewed) * 100) : 0;
});

const collegeSummary = computed(() => {
    const map = {};
    proposals.value.forEach(p => {
        if (!map[p.college]) map[p.college] = { college: p.college, proposalCount: 0, totalBudget: 0 };
        map[p.college].proposalCount++;
        map[p.college].totalBudget += p.budget;
    });
    return Object.values(map);
});

const sortedCollegeSummary = computed(() => [...collegeSummary.value].sort((a, b) => b.totalBudget - a.totalBudget));
const monthlySummary = ref(MOCK_MONTHLY);

// ── Lifecycle ─────────────────────────────────────
// TODO: Replace with backend API call
onMounted(async () => {
    loading.value = true;
    await new Promise(r => setTimeout(r, 350));
    proposals.value = getAllProposals();
    loading.value = false;
    document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

const handleOutsideClick = (e) => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Helpers ───────────────────────────────────────
const getSharePct = (budget) => totalBudget.value === 0 ? '0.0' : ((budget / totalBudget.value) * 100).toFixed(1);
const getStatusPct = (count) => totalCount.value === 0 ? 0 : Math.round(((count || 0) / totalCount.value) * 100);
const formatCurrency = (v) => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 0 }).format(v || 0);
const formatShort = (v) => {
    if (!v) return '₱0';
    if (v >= 1000000) return `₱${(v / 1000000).toFixed(1)}M`;
    if (v >= 1000) return `₱${(v / 1000).toFixed(0)}k`;
    return `₱${v}`;
};
</script>

<style scoped>

.ovcaf-main{
    width:132%;
}

.ovcaf-body{
    max-width:1400px;
    margin:0 auto;
    padding:24px;
}

.overview-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1.5rem;
    margin-bottom: 1.5rem;
}

.overview-card{
    display:flex;
    align-items:center;
    gap:18px;
    min-height:120px;
}

.ov-icon-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 54px;
    height: 54px;
    border-radius: var(--border-radius-sm);
    font-size: var(--ui-font-page-title);
    flex-shrink: 0;
}

.ov-label {
    font-size: var(--ui-font-label);
    font-weight: var(--ui-weight-semibold);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: var(--ui-tracking-caps);
    margin-bottom: 0.125rem;
}

.ov-value {
    font-size: var(--ui-font-page-title);
    font-weight: var(--ui-weight-bold);
    color: var(--text-primary);
    line-height: var(--ui-line-display);
}

.ov-sub {
    font-size: var(--ui-font-caption);
    color: var(--text-muted);
    margin-top: 0.125rem;
}

.status-summary-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1.25rem;
    margin-bottom: 1.5rem;
}

@media (max-width: 900px) {
    .status-summary-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

.status-card {
    border-left: 4px solid #cbd5e1;
}

.stat-approved {
    border-left-color: var(--color-approved);
}

.stat-returned {
    border-left-color: var(--color-returned);
}

.stat-rejected {
    border-left-color: var(--color-rejected);
}

.stat-pending {
    border-left-color: var(--color-pending);
}

.stat-label {
    font-size: var(--ui-font-label);
    font-weight: var(--ui-weight-semibold);
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: var(--ui-tracking-caps);
    margin-bottom: 0.375rem;
}

.stat-val {
    font-size: var(--ui-font-display);
    font-weight: var(--ui-weight-bold);
    line-height: var(--ui-line-display);
    color: var(--text-primary);
}

.stat-approved .stat-val {
    color: var(--color-approved-text);
}

.stat-returned .stat-val {
    color: var(--color-returned-text);
}

.stat-rejected .stat-val {
    color: var(--color-rejected-text);
}

.stat-pending .stat-val {
    color: var(--color-pending-text);
}

.stat-sub {
    font-size: var(--ui-font-caption);
    margin-top: 0.25rem;
}

.reports-grid{
    display:grid;
    grid-template-columns:2fr 1fr;
    gap:24px;
    align-items:start;
}

.reports-content{
    display:flex;
    flex-direction:column;
    gap:24px;
    width:100%;
}

@media (max-width: 1024px) {
    .reports-grid {
        grid-template-columns: 1fr;
    }
}

.section-sub {
    font-size: var(--ui-font-caption);
    color: var(--text-muted);
    margin-top: -0.375rem;
    margin-bottom: 1rem;
}

.font-mono {
    font-family: var(--ui-font-mono);
}

.font-medium {
    font-weight: var(--ui-weight-medium);
}

.text-center {
    text-align: center;
}

.text-right {
    text-align: right;
}

.share-row {
    display: flex;
    align-items: center;
    gap: 0.625rem;
    min-width: 130px;
}

.share-pct {
    font-size: var(--ui-font-small);
    font-weight: var(--ui-weight-medium);
    color: var(--text-primary);
    width: 40px;
    text-align: right;
    flex-shrink: 0;
}

.progress-bg {
    flex: 1;
    height: 6px;
    background-color: #e2e8f0;
    border-radius: 3px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    background-color: var(--color-primary);
    border-radius: 3px;
    transition: width 0.4s ease;
}

.fill-approved {
    background-color: var(--color-approved);
}

.fill-returned {
    background-color: var(--color-returned);
}

.fill-rejected {
    background-color: var(--color-rejected);
}

.fill-pending {
    background-color: var(--color-pending);
}

.metrics-list {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.metric-block {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
}

.metric-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.metric-budget {
    font-size: var(--ui-font-body);
    font-weight: var(--ui-weight-semibold);
    color: var(--text-primary);
}

.metric-sub {
    font-size: var(--ui-font-caption);
    color: var(--text-muted);
}

.monthly-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
    gap: 0.75rem;
}

.month-card{
    padding:14px 10px;
}

.month-card.is-current {
    border-color: var(--color-primary);
    background-color: #fff5f5;
}

.month-name {
    font-size: var(--ui-font-body);
    font-weight: var(--ui-weight-bold);
    color: var(--text-primary);
}

.month-budget {
    font-size: var(--ui-font-body);
    font-weight: var(--ui-weight-bold);
    color: var(--color-primary);
}

.month-badges {
    display: flex;
    gap: 3px;
    flex-wrap: wrap;
    justify-content: center;
}

.mini-badge {
    font-size: var(--ui-font-caption);
    font-weight: var(--ui-weight-semibold);
    padding: 2px 5px;
    border-radius: 3px;
}

.mb-approved {
    background-color: var(--color-approved-bg);
    color: var(--color-approved-text);
}

.mb-returned {
    background-color: var(--color-returned-bg);
    color: var(--color-returned-text);
}

.mb-rejected {
    background-color: var(--color-rejected-bg);
    color: var(--color-rejected-text);
}

.mt-6 {
    margin-top: 1.5rem;
}

.card-title{
    margin-bottom:4px;
    font-size:var(--ui-font-card-title);
    font-weight:var(--ui-weight-semibold);
}

.section-sub{
    margin-bottom:20px;
}

.table-container{
    overflow-x:auto;
}

.custom-table{
    width:100%;
    border-collapse:collapse;
}

.badge{
    display:inline-flex;
    align-items:center;
    justify-content:center;
    padding:6px 12px;
    border-radius:999px;
    font-size:var(--ui-font-caption);
    font-weight:var(--ui-weight-semibold);
}

.badge-approved{
    background:#dcfce7;
    color:#15803d;
}

.badge-returned{
    background:#fef3c7;
    color:#b45309;
}

.badge-rejected{
    background:#fee2e2;
    color:#b91c1c;
}

.badge-pending{
    background:#dbeafe;
    color:#1d4ed8;
}

.progress-bg{
    height:8px;
    border-radius:999px;
}

.progress-fill{
    border-radius:999px;
}

.overview-card,
.status-card{
    transition:.2s;
}

.overview-card:hover,
.status-card:hover{
    transform:translateY(-2px);
    box-shadow:0 10px 25px rgba(0,0,0,.08);
}

</style>
