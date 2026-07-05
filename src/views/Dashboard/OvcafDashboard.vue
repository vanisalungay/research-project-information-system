<template>
  <div class="ovcaf-shell">
    <!-- ── Main Area ── -->
    <div class="ovcaf-main">
      <!-- Topbar -->
      <header class="ovcaf-topbar">
        <div class="ovcaf-topbar-left">
          <div>
            <div class="ovcaf-breadcrumbs"><span>OVCAF</span></div>
            <h2 class="ovcaf-page-title">Dashboard</h2>
          </div>
        </div>
      </header>

      <!-- ── Page Content ── -->
      <main class="ovcaf-body">
        <!-- Stat Cards -->
        <div class="stats-grid">
          <div class="card card-hover stat-card stat-pending">
            <p class="stat-label">PENDING</p>
            <h3 class="stat-val">{{ stats.pending }}</h3>
            <p class="stat-sub">Awaiting review</p>
          </div>
          <div class="card card-hover stat-card stat-total">
            <p class="stat-label">TOTAL REVIEWED</p>
            <h3 class="stat-val">{{ stats.totalReviewed }}</h3>
            <p class="stat-sub">Processed by OVCAF</p>
          </div>
          <div class="card card-hover stat-card stat-approved">
            <p class="stat-label">APPROVED</p>
            <h3 class="stat-val">{{ stats.approved }}</h3>
            <p class="stat-sub">Passed validation</p>
          </div>
        </div>

        <div class="db-grid">
          <!-- Recent Pending Table -->
          <div class="card">
            <div class="flex-between mb-4">
              <h4 class="card-title" style="margin-bottom:0">Recent Pending Proposals</h4>
              <router-link to="/ovcaf/inbox" class="btn btn-secondary btn-sm">View All</router-link>
            </div>
            <div v-if="loading" class="loading-msg">Loading...</div>
            <div v-else-if="pendingProposals.length === 0" class="empty-box">
              <p>No pending proposals. All caught up!</p>
              <router-link to="/ovcaf/inbox" class="btn btn-primary btn-sm" style="margin-top:.75rem">Go to
                Inbox</router-link>
            </div>
            <div v-else class="table-container">
              <table class="custom-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Proponent</th>
                    <th>Budget</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="p in pendingProposals.slice(0, 4)" :key="p.id">
                    <td class="mono semibold">{{ p.id }}</td>
                    <td>
                      <div class="truncate-cell" :title="p.title">{{ p.title }}</div>
                    </td>
                    <td>{{ p.proponent }}</td>
                    <td class="mono">{{ fmt(p.budget) }}</td>
                    <td><router-link :to="`/ovcaf/validate/${p.id}`"
                        class="btn btn-primary btn-sm">Validate</router-link></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Activity Feed -->
          <div class="card">
            <h4 class="card-title">Recent Activity</h4>
            <div v-if="loading" class="loading-msg">Loading...</div>
            <div v-else-if="recentActivity.length === 0" class="empty-box">
              <p>No activity yet.</p>
            </div>
            <div v-else class="activity-list">
              <div v-for="act in recentActivity" :key="act.id" class="act-item">
                <div class="act-dot" :class="dotClass(act.action)"></div>
                <div class="act-body">
                  <div class="act-meta"><span class="mono semibold small">{{ act.proposalId }}</span><span
                      class="muted small">{{ relDate(act.date) }}</span></div>
                  <p class="act-desc">{{ act.action }} — <router-link :to="`/ovcaf/proposals/${act.proposalId}`"
                      class="act-link">{{ act.title }}</router-link></p>
                  <p class="muted small">By {{ act.user }}</p>
                  <p v-if="act.notes" class="act-note">"{{ act.notes }}"</p>
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
const proposals = ref([]);
const recentActivity = ref([]);
const stats = ref({ pending: 0, approved: 0, totalReviewed: 0 });
const unreadCount = ref(0);

const pendingProposals = computed(() =>
  proposals.value.filter(p => p.status === 'Pending Validation' || p.status === 'Under Validation')
);

// ── Mock Data ─────────────────────────────────────
// TODO: Replace mock data with backend API
const MOCK_PROPOSALS = [];


// ── localStorage helpers ──────────────────────────
const PROPOSAL_KEY = 'ovcaf_proposals';
const NOTIF_KEY = 'ovcaf_notifications';

const getAllProposals = () => {
  const s = localStorage.getItem(PROPOSAL_KEY);
  if (!s) { localStorage.setItem(PROPOSAL_KEY, JSON.stringify(MOCK_PROPOSALS)); return MOCK_PROPOSALS; }
  return JSON.parse(s);
};

const getAllNotifications = () => {
  const s = localStorage.getItem(NOTIF_KEY);
  if (!s) { localStorage.setItem(NOTIF_KEY, JSON.stringify(MOCK_NOTIFICATIONS)); return MOCK_NOTIFICATIONS; }
  return JSON.parse(s);
};

// ── Load Page Data ────────────────────────────────
// TODO: Replace with backend API calls
const loadDashboard = async () => {
  loading.value = true;
  await new Promise(r => setTimeout(r, 300));
  const all = getAllProposals();
  proposals.value = all;

  const s = { pending: 0, approved: 0, totalReviewed: 0 };
  all.forEach(p => {
    if (p.status === 'Pending Validation' || p.status === 'Under Validation') s.pending++;
    else if (p.status === 'Approved & Endorsed') { s.approved++; s.totalReviewed++; }
  });
  stats.value = s;

  const acts = [];
  all.forEach(p => {
    p.history.forEach(h => {
      if (h.stage === 'OVCAF') acts.push({ id: `${p.id}-${h.date}`, proposalId: p.id, title: p.title, action: h.action, user: h.user, date: h.date, notes: h.notes });
    });
  });
  acts.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  recentActivity.value = acts.slice(0, 5);

  const notifs = getAllNotifications();
  unreadCount.value = notifs.filter(n => !n.read).length;
  loading.value = false;
};

// ── Lifecycle ─────────────────────────────────────
onMounted(() => {
  loadDashboard();
  document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

const handleOutsideClick = (e) => {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Helpers ───────────────────────────────────────
const fmt = v => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 0 }).format(v);
const dotClass = a => { const l = (a || '').toLowerCase(); if (l.includes('approve') || l.includes('endorse')) return 'dot-approved'; };
const relDate = d => { if (!d) return ''; try { const dt = new Date(d.replace(/-/g, '/')); const diff = Math.floor((Date.now() - dt) / 60000); if (diff < 1) return 'Just now'; if (diff < 60) return `${diff}m ago`; const dh = Math.floor(diff / 60); if (dh < 24) return `${dh}h ago`; const dd = Math.floor(dh / 24); if (dd === 1) return 'Yesterday'; return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }); } catch { return d; } };
</script>

<style scoped>
body {
  background: #f5f7fb;
}

.ovcaf-main{
  width:136%;
}

.ovcaf-body{
  padding:24px;
  background:#f5f7fb;
  min-height:100vh;
}

.ovcaf-topbar{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:25px 30px;
  background:#fff;
  border-bottom:1px solid #e5e7eb;
}

.custom-table{
    width:100%;
    border-collapse:collapse;
}

.custom-table th{
    background:#f8fafc;
    color:#6b7280;
    font-size:.82rem;
    text-transform:uppercase;
    letter-spacing:.03rem;
    padding:12px;
    text-align:left;
}

.custom-table td{
    padding:14px 12px;
    border-bottom:1px solid #f1f5f9;
}

.custom-table tbody tr:hover{
    background:#fafafa;
}

.ovcaf-page-title{
  font-size:28px;
  font-weight:700;
  color:#1f2937;
}

.ovcaf-breadcrumbs{
  color:#6b7280;
  font-size:14px;
}

.stats-grid{
    display:grid;
    grid-template-columns:repeat(3,minmax(250px,1fr));
    gap:24px;
    margin-bottom:24px;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.btn-sm{
    display:inline-flex;
    align-items:center;
    justify-content:center;

    width:auto;
    min-width:unset;
    height:34px;

    padding:6px 14px;

    flex:none;
}

.btn-secondary{
    background:#fff;
    border:1px solid #d1d5db;
    color:#374151;
    text-decoration:none;
    font-weight:600;
}

.btn-secondary:hover{
    background:#f9fafb;
}

.btn-primary{
    background:#7f1d1d;
    color:white;
    border:none;
    padding:7px 14px;
    border-radius:8px;
    font-size:.82rem;
    font-weight:600;
    text-decoration:none;
}

.btn-primary:hover{
    background:#991b1b;
}

.card-title{
    font-size:1rem;
    font-weight:700;
    color:#374151;
}

.act-desc{
    color:#374151;
    font-weight:600;
}

.act-link{
    color:#7f1d1d;
    text-decoration:none;
}

.act-link:hover{
    text-decoration:underline;
}

.stat-card{
  min-height:120px;
  display:flex;
  flex-direction:column;
  justify-content:center;
}
.stat-val{
    font-size:2.2rem;
    margin:4px 0;
}

.stat-label{
    margin-bottom:6px;
}

.stat-sub{
    margin-top:4px;
}

.stat-pending{
  border-left:5px solid #f59e0b;
}
.stat-total{
  border-left:5px solid #3b82f6;
}
.stat-approved{
  border-left:5px solid #10b981;
}


.stat-pending .stat-val{
  color:#d97706;
}
.stat-total .stat-val{
  color:#2563eb;
}
.stat-approved .stat-val{
  color:#059669;
}


.stat-sub {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

/* Quick Actions */
.qa-row {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.qa-badge {
  background-color: var(--color-accent);
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  margin-left: 0.25rem;
}

/* Dashboard grid */
.db-grid {
  display: grid;
  grid-template-columns: 1.8fr 1.2fr;
  gap: 1.5rem;
  align-items:start;
}

@media (max-width: 1100px) {
  .db-grid {
    grid-template-columns: 1fr;
  }
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb-4 {
  margin-bottom: 1rem;
}

.truncate-cell {
  max-width: 240px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  color: var(--text-primary);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.semibold {
  font-weight: 600;
}

.small {
  font-size: 0.75rem;
}

.muted {
  color: var(--text-muted);
}

.loading-msg {
  padding: 1.5rem;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.empty-box {
  padding: 2rem;
  text-align: center;
  color: var(--text-muted);
}

/* Activity feed */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.act-item {
  display: flex;
  gap: 1rem;
}

.act-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.dot-approved {
  background-color: var(--color-approved);
  box-shadow: 0 0 0 3px var(--color-approved-bg);
}



.dot-info {
  background-color: var(--color-info);
  box-shadow: 0 0 0 3px var(--color-info-bg);
}

.act-body {
  flex: 1;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 1rem;
}

.act-item:last-child .act-body {
  border-bottom: none;
  padding-bottom: 0;
}

.act-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.125rem;
}

.act-desc {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.4;
}

.act-link {
  color: var(--color-primary);
  font-weight: 600;
}

.act-link:hover {
  text-decoration: underline;
}

.act-note {
  margin-top: 0.375rem;
  font-size: 0.78rem;
  font-style: italic;
  color: var(--text-secondary);
  background: #f8fafc;
  border-left: 2px solid #cbd5e1;
  padding: 0.25rem 0.5rem;
  border-radius: 0 4px 4px 0;
}

</style>
