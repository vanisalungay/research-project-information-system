<template>
    <div class="ovcaf-shell">
      <!-- ── Main Area ── -->
        <div class="ovcaf-main">
            <header class="ovcaf-topbar">
                <div>
                    <h2 class="ovcaf-page-title">Proposal Inbox</h2>
                </div>
            </header>

            <main class="ovcaf-body">
                <!-- Filters -->
                <div class="card filters-card">
                    <div class="filters-layout">
                        <div class="search-section">
                            <label class="form-label">Search Proposal</label>
                            <div class="search-bar-wrapper">
                                <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="11" cy="11" r="8" />
                                    <path d="m21 21-4.3-4.3" />
                                </svg>
                                <input type="text" v-model="filters.search" @input="filters.page = 1"
                                    placeholder="Search by ID, title, or proponent..."
                                    class="form-control search-input" />
                                <button v-if="filters.search" @click="filters.search = ''; filters.page = 1"
                                    class="clear-btn" type="button">&times;</button>
                            </div>
                        </div>
                        <div class="filter-controls">
                            <div>
                                <label class="form-label">Status</label>
                                <select v-model="filters.status" @change="filters.page = 1" class="form-control">
                                    <option value="All">All Statuses</option>
                                    <option value="Pending Validation">Pending Validation</option>
                                    <option value="Approved & Endorsed">Approved & Endorsed</option>
                                </select>
                            </div>
                            <div>
                                <label class="form-label">Sort By</label>
                                <select v-model="filters.sortBy" @change="filters.page = 1" class="form-control">
                                    <option value="dateReceived">Date Received</option>
                                    <option value="budget">Budget</option>
                                    <option value="title">Title</option>
                                    <option value="id">Proposal ID</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Loading -->
                <div v-if="loading" class="card">
                    <p style="padding:2rem;text-align:center;color:var(--text-muted)">Loading proposals...</p>
                </div>

                <!-- Empty State -->
                <div v-else-if="paginatedProposals.length === 0" class="card empty-state-box">
                    <p class="empty-title">No Proposals Found</p>
                    <p class="empty-desc">No proposals match your current search criteria or filters.</p>
                </div>

                <!-- Table -->
                <div v-else class="card">
                    <div class="table-container">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th class="sortable" @click="toggleSort('id')">ID <span class="sort-ind">{{
                                            sortIndicator('id') }}</span></th>
                                    <th class="sortable" @click="toggleSort('title')">Research Title <span
                                            class="sort-ind">{{ sortIndicator('title') }}</span></th>
                                    <th class="sortable" @click="toggleSort('proponent')">Proponent <span
                                            class="sort-ind">{{ sortIndicator('proponent') }}</span></th>
                                    <th class="sortable" @click="toggleSort('budget')">Budget <span class="sort-ind">{{
                                            sortIndicator('budget') }}</span></th>
                                    <th class="sortable" @click="toggleSort('dateReceived')">Date Received <span
                                            class="sort-ind">{{ sortIndicator('dateReceived') }}</span></th>
                                    <th>Status</th>
                                    <th class="actions-header">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="p in paginatedProposals" :key="p.id" class="proposal-row">
                                    <td class="font-mono font-semibold">{{ p.id }}</td>
                                    <td>
                                        <div class="title-cell" :title="p.title">{{ p.title }}</div>
                                    </td>
                                    <td>{{ p.proponent }}</td>
                                    <td><span class="college-abbr" :title="p.college">{{ getCollegeAbbr(p.college)
                                            }}</span></td>
                                    <td class="font-mono font-semibold">{{ formatCurrency(p.budget) }}</td>
                                    <td>{{ formatDate(p.dateReceived) }}</td>
                                    <td><span class="badge" :class="getStatusClass(p.status)">{{ p.status }}</span></td>
                                    <td>
                                        <div class="actions-cell">
                                            <router-link :to="`/ovcaf/proposals/${p.id}`"
                                                class="btn btn-secondary btn-sm">View</router-link>
                                            <router-link v-if="isPending(p)" :to="`/ovcaf/validate/${p.id}`"
                                                class="btn btn-primary btn-sm">Validate</router-link>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- Pagination -->
                    <div class="pagination-container" v-if="totalPages > 1">
                        <div class="pagination-info">
                            Showing <strong>{{ rangeStart }}</strong>–<strong>{{ rangeEnd }}</strong> of <strong>{{
                                filteredProposals.length }}</strong> proposals
                        </div>
                        <div class="pagination-controls">
                            <button @click="filters.page--" :disabled="filters.page === 1"
                                class="pagination-btn">&lsaquo;</button>
                            <button v-for="pg in totalPages" :key="pg" @click="filters.page = pg"
                                class="pagination-btn page-num" :class="{ active: pg === filters.page }">{{ pg
                                }}</button>
                            <button @click="filters.page++" :disabled="filters.page === totalPages"
                                class="pagination-btn">&rsaquo;</button>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';

// ── Shell State ───────────────────────────────────
const sidebarOpen = ref(false);
const dropdownOpen = ref(false);
const dropdownRef = ref(null);

// ── Page State ────────────────────────────────────
const loading = ref(true);
const proposals = ref([]);

const filters = reactive({
    search: '',
    status: 'All',
    college: 'All',
    sortBy: 'dateReceived',
    sortOrder: 'desc',
    page: 1,
    limit: 8
});

// ── Mock Data ─────────────────────────────────────
// TODO: Replace mock data with backend API
const MOCK_PROPOSALS = [
    { id: 'PRP-2026-042', title: 'Development of an IoT-Based Water Quality Monitoring System for Local Aquaculture', proponent: 'Dr. Maria C. Velasco', college: 'College of Engineering and Technology', department: 'Environmental Engineering', budget: 875000, dateReceived: '2026-07-02', status: 'Pending Validation' },
    { id: 'PRP-2026-039', title: 'Bioremediation of Heavy Metal Pollutants in Urban Drainage Using Local Fungal Isolates', proponent: 'Dr. Rosa A. Fuentes', college: 'College of Science and Mathematics', department: 'Biology', budget: 620000, dateReceived: '2026-06-28', status: 'Pending Validation' },
    { id: 'PRP-2026-035', title: 'Machine Learning-Based Prediction of Rice Yield in Typhoon-Prone Regions', proponent: 'Dr. Kenneth L. Abarra', college: 'College of Agriculture', department: 'Agronomy', budget: 980000, dateReceived: '2026-06-20', status: 'Pending Validation' },
    { id: 'PRP-2026-031', title: 'Indigenous Medicinal Plants of Misamis Oriental: Ethnobotanical and Phytochemical Survey', proponent: 'Prof. Ana T. Baguio', college: 'College of Science and Mathematics', department: 'Botany', budget: 540000, dateReceived: '2026-06-15', status: 'Under Validation' },
    { id: 'PRP-2026-025', title: 'Socio-Economic Impact of Renewable Microgrids in Off-Grid Coastal Communities', proponent: 'Prof. Ben M. Laguna', college: 'College of Arts and Social Sciences', department: 'Economics', budget: 1630000, dateReceived: '2026-06-10', status: 'Approved & Endorsed' },
    { id: 'PRP-2026-012', title: 'Gamified Mobile Application for K-12 Science Education in Rural Municipalities', proponent: 'Dr. Lourdes F. Patricio', college: 'College of Education', department: 'Educational Technology', budget: 380000, dateReceived: '2026-05-05', status: 'Approved & Endorsed' },
];

// ── localStorage helpers ──────────────────────────
const PROPOSAL_KEY = 'ovcaf_proposals';
const getAllProposals = () => {
    const s = localStorage.getItem(PROPOSAL_KEY);
    if (!s) { localStorage.setItem(PROPOSAL_KEY, JSON.stringify(MOCK_PROPOSALS)); return MOCK_PROPOSALS; }
    return JSON.parse(s);
};

// ── Computed: Filtered & Sorted ───────────────────
const filteredProposals = computed(() => {
    let result = [...proposals.value];
    if (filters.search) {
        const q = filters.search.toLowerCase();
        result = result.filter(p => p.title.toLowerCase().includes(q) || p.id.toLowerCase().includes(q) || p.proponent.toLowerCase().includes(q));
    }
    if (filters.status !== 'All') result = result.filter(p => p.status === filters.status);
    if (filters.college !== 'All') result = result.filter(p => p.college === filters.college);
    result.sort((a, b) => {
        let fa = a[filters.sortBy], fb = b[filters.sortBy];
        if (filters.sortBy === 'dateReceived') { fa = new Date(fa).getTime(); fb = new Date(fb).getTime(); }
        else if (typeof fa === 'string') { fa = fa.toLowerCase(); fb = fb.toLowerCase(); }
        if (fa < fb) return filters.sortOrder === 'asc' ? -1 : 1;
        if (fa > fb) return filters.sortOrder === 'asc' ? 1 : -1;
        return 0;
    });
    return result;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProposals.value.length / filters.limit)));
const paginatedProposals = computed(() => { const start = (filters.page - 1) * filters.limit; return filteredProposals.value.slice(start, start + filters.limit); });
const rangeStart = computed(() => Math.min((filters.page - 1) * filters.limit + 1, filteredProposals.value.length));
const rangeEnd = computed(() => Math.min(filters.page * filters.limit, filteredProposals.value.length));

// ── Lifecycle ─────────────────────────────────────
// TODO: Replace with backend API call
onMounted(async () => {
    loading.value = true;
    await new Promise(r => setTimeout(r, 300));
    proposals.value = getAllProposals();
    loading.value = false;
    document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

const handleOutsideClick = (e) => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Actions ───────────────────────────────────────
const toggleSort = (field) => {
    if (filters.sortBy === field) { filters.sortOrder = filters.sortOrder === 'asc' ? 'desc' : 'asc'; }
    else { filters.sortBy = field; filters.sortOrder = 'desc'; }
    filters.page = 1;
};

// ── Helpers ───────────────────────────────────────
const isPending = (p) => p.status === 'Pending Validation' || p.status === 'Under Validation';
const sortIndicator = (field) => filters.sortBy === field ? (filters.sortOrder === 'asc' ? '▲' : '▼') : '';
const getCollegeAbbr = (c) => ({ 'College of Engineering and Technology': 'CET', 'College of Science and Mathematics': 'CSM', 'College of Arts and Social Sciences': 'CASS', 'College of Information Technology': 'CIT', 'College of Education': 'COED', 'College of Agriculture': 'COA' }[c] || c);
const formatCurrency = (v) => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 0 }).format(v);
const formatDate = (d) => d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '';
const getStatusClass = (s) => {
    if (s === 'Pending Validation' || s === 'Under Validation') return 'badge-pending';
    if (s === 'Approved & Endorsed') return 'badge-approved';
    return 'badge-info';
};
</script>

<style scoped>

.ovcaf-main{
  width:102%;
}
.ovcaf-body{
    padding:24px;
}

.ovcaf-page-title{
    font-size:1.7rem;
    font-weight:700;
    color:#1F2937;
}

.filters-card {
    padding: 1.25rem 1.5rem;
    margin-bottom: 1.5rem;
}

.filters-layout {
    display: grid;
    grid-template-columns: 1.5fr 3fr;
    gap: 1.5rem;
    align-items: flex-end;
}

.filter-controls {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 1rem;
    align-items: flex-end;
}

.card{
    background:white;
    border-radius:14px;
    padding:22px;
    box-shadow:0 2px 8px rgba(0,0,0,.05);
}

.form-control{
    width:100%;
    height:42px;

    border:1px solid #D1D5DB;
    border-radius:8px;

    padding:0 12px;

    font-size:.9rem;
}

.form-control:focus{
    outline:none;
    border-color:#7f1d1d;
    box-shadow:0 0 0 3px rgba(127,29,29,.15);
}

@media (max-width: 1200px) {
    .filters-layout {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .filter-controls {
        grid-template-columns: 1fr;
    }
}

.search-bar-wrapper{
    position:relative;
}

.search-input{
    padding-left:42px;
}

.search-icon{
    position:absolute;
    top:50%;
    left:14px;
    transform:translateY(-50%);
    color:#9CA3AF;
}

.btn{
    display:inline-flex;
    align-items:center;
    justify-content:center;

    padding:7px 14px;
    border-radius:8px;
    font-size:.82rem;
    font-weight:600;

    text-decoration:none;
    cursor:pointer;
    transition:.2s;

    white-space:nowrap;
}

.btn-sm{
    height:34px;
}

.btn-primary{
    background:#7f1d1d;
    color:white;
    border:none;
}

.btn-primary:hover{
    background:#991b1b;
}

.btn-secondary{
    background:white;
    border:1px solid #d1d5db;
    color:#374151;
}

.btn-secondary:hover{
    background:#f3f4f6;
}

.badge{
    display:inline-block;
    padding:5px 12px;
    border-radius:999px;
    font-size:.75rem;
    font-weight:700;
}

.badge-pending{
    background:#FEF3C7;
    color:#B45309;
}

.badge-approved{
    background:#DCFCE7;
    color:#166534;
}


.table-container{
    overflow-x:auto;
}

.custom-table{
    width:100%;
    border-collapse:collapse;
}

.custom-table th{
    background:#F8FAFC;
    color:#6B7280;
    font-size:.8rem;
    text-transform:uppercase;
    padding:14px;
    text-align:left;
}

.custom-table td{
    padding:15px 14px;
    border-bottom:1px solid #E5E7EB;
}

.custom-table tbody tr:hover{
    background:#FAFAFA;
}

.badge-info{
    background:#DBEAFE;
    color:#1D4ED8;
}

.clear-btn {
    position: absolute;
    right: 0.875rem;
    background: none;
    border: none;
    font-size: 1.25rem;
    color: var(--text-muted);
    cursor: pointer;
    line-height: 1;
}

.clear-btn:hover {
    color: var(--text-primary);
}

.sortable {
    cursor: pointer;
    user-select: none;
    white-space: nowrap;
}

.sortable:hover {
    background-color: #e2e8f0;
}

.sort-ind {
    font-size: 0.65rem;
    margin-left: 3px;
    opacity: 0.7;
}

.form-label{
    display:block;
    margin-bottom:6px;
    font-size:.82rem;
    font-weight:600;
    color:#6B7280;
}

.actions-cell{
    display:flex;
    gap:8px;
}

.font-mono {
    font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.font-semibold {
    font-weight: 600;
}

.title-cell {
    max-width: 280px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 500;
    color: var(--text-primary);
}

.college-abbr {
    border-bottom: 1px dashed var(--text-muted);
    cursor: help;
    font-size: 0.85rem;
}

.actions-header {
    text-align: right;
}

.actions-cell {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
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
}

.pagination-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 0;
    margin-top: 1rem;
    border-top: 1px solid #e2e8f0;
    flex-wrap: wrap;
    gap: 1rem;
}

.pagination-info {
    font-size: 0.875rem;
    color: var(--text-secondary);
}

.pagination-controls {
    display: flex;
    align-items: center;
    gap: 0.375rem;
}

.pagination-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--border-radius-sm);
    border: 1px solid #cbd5e1;
    background-color: white;
    color: var(--text-secondary);
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 500;
    transition: all var(--transition-fast);
}

.pagination-btn:hover:not(:disabled) {
    background-color: #f1f5f9;
    color: var(--text-primary);
    border-color: #94a3b8;
}

.pagination-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.page-num.active {
    background-color: var(--color-primary);
    color: white;
    border-color: var(--color-primary);
}
</style>
