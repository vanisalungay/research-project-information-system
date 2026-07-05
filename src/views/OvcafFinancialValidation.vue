<template>
    <div class="ovcaf-shell">
        <!-- ── Main Area ── -->
        <div class="ovcaf-main">

            <main class="ovcaf-body">
                <div v-if="loading" class="card">
                    <p style="padding:2rem;text-align:center;color:var(--text-muted)">Loading proposal...</p>
                </div>

                <div v-else-if="!proposal" class="card empty-state-box">
                    <p class="empty-title">Proposal Not Found</p>
                    <p class="empty-desc">The proposal you are attempting to validate cannot be found.</p>
                    <router-link to="/ovcaf/inbox" class="btn btn-primary" style="margin-top:1rem">Back to
                        Inbox</router-link>
                </div>

                <div v-else>
                    <div class="action-bar">
                        <router-link :to="`/ovcaf/proposals/${proposal.id}`" class="btn btn-secondary btn-sm">← Back to
                            Details</router-link>
                    </div>

                    <div class="validation-layout">
                        <!-- Left: Summary + Checklist -->
                        <div class="left-col">
                            <!-- Proposal Summary -->
                            <div class="card summary-card">
                                <span class="proposal-id font-mono">{{ proposal.id }}</span>
                                <h3 class="proposal-title-text">{{ proposal.title }}</h3>
                                <div class="meta-chips">
                                    <div><span class="chip-label">Proponent</span><span class="chip-value">{{
                                        proposal.proponent }}</span></div>
                                    <div><span class="chip-label">Proposed Budget</span><span
                                            class="chip-value font-mono primary-text font-semibold">{{
                                                formatCurrency(proposal.budget) }}</span></div>
                                    <div><span class="chip-label">Funding Source</span><span class="chip-value">{{
                                        proposal.fundingSource }}</span></div>
                                    <div><span class="chip-label">College</span><span class="chip-value">{{
                                        proposal.college }}</span></div>
                                </div>
                            </div>

                            <!-- Validation Checklist -->
                            <div class="card mt-6">
                                <h4 class="card-title">Financial Validation Checklist</h4>
                                <p class="checklist-hint">Review and check each item to confirm budget compliance before
                                    submitting your decision.</p>
                                <div class="checklist-grid">
                                    <label v-for="(label, key) in checklistLabels" :key="key" class="checklist-item"
                                        :class="{ checked: checklist[key] }">
                                        <input type="checkbox" v-model="checklist[key]" class="sr-only" />
                                        <div class="cb-box" :class="{ filled: checklist[key] }">{{ checklist[key] ? '✓'
                                            : '' }}</div>
                                        <span class="cb-label">{{ label }}</span>
                                    </label>
                                </div>
                                <div v-if="isAllChecked" class="all-checked-banner">✅ All items verified — Approval is
                                    now available.</div>
                            </div>
                        </div>

                        <!-- Right: Decision Form -->
                        <div class="right-col">
                            <div class="card decision-card">
                                <h4 class="card-title">Evaluation Decision</h4>
                                <form @submit.prevent="handleSubmit">
                                    <!-- Decision Radio Options -->
                                    <div class="form-group">
                                        <span class="form-label section-label">Select Administrative Action</span>
                                        <div class="radio-list">
                                            <label class="radio-option"
                                                :class="{ 'selected approve': decision === 'Approved & Endorsed' }">
                                                <input type="radio" v-model="decision" value="Approved & Endorsed"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title approve-title">✅ Approve &amp;
                                                        Endorse</span>
                                                    <span class="option-desc">Budget meets all compliance conditions.
                                                        Proposal will be endorsed back to RII with OVCAF
                                                        certification.</span>
                                                </div>
                                            </label>
                                            <label class="radio-option"
                                                :class="{ 'selected return': decision === 'Returned for Revision' }">
                                                <input type="radio" v-model="decision" value="Returned for Revision"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title return-title">🔄 Return for
                                                        Revision</span>
                                                    <span class="option-desc">Budget requires corrections. Proposal is
                                                        sent back with revision instructions.</span>
                                                </div>
                                            </label>
                                            <label class="radio-option"
                                                :class="{ 'selected reject': decision === 'Rejected' }">
                                                <input type="radio" v-model="decision" value="Rejected"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title reject-title">❌ Reject Proposal</span>
                                                    <span class="option-desc">Permanently deny the financial validation
                                                        request.</span>
                                                </div>
                                            </label>
                                        </div>
                                    </div>

                                    <!-- Remarks -->
                                    <div class="form-group mt-6">
                                        <label for="validation-remarks" class="form-label section-label">
                                            Evaluator Remarks / Instructions
                                            <span v-if="decision !== 'Approved & Endorsed'" class="required-mark">*
                                                Required</span>
                                        </label>
                                        <textarea id="validation-remarks" v-model="remarks" rows="5"
                                            class="form-control" style="resize:vertical;min-height:110px"
                                            placeholder="Provide evaluation notes, revision directions, or justification..."></textarea>
                                    </div>

                                    <!-- Error Banner -->
                                    <div v-if="formError" class="error-banner">⚠ {{ formError }}</div>

                                    <!-- Actions -->
                                    <div class="form-actions">
                                        <button type="button" @click="resetForm" class="btn btn-secondary"
                                            :disabled="submitLoading">Reset</button>
                                        <button type="submit" class="btn btn-primary" :disabled="submitLoading">
                                            {{ submitLoading ? 'Processing...' : 'Submit Evaluation' }}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Confirmation Modal -->
                <Teleport to="body">
                    <transition name="modal-fade">
                        <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
                            <div class="modal-box card">
                                <div class="modal-header">
                                    <h4 class="modal-title">Confirm: {{ decision }}</h4>
                                    <button @click="showModal = false" class="modal-close-btn">&times;</button>
                                </div>
                                <p class="modal-message">{{ modalMessage }}</p>
                                <div class="modal-actions">
                                    <button @click="showModal = false" class="btn btn-secondary">Cancel</button>
                                    <button @click="submitDecision" class="btn" :class="confirmBtnClass">Confirm &amp;
                                        Proceed</button>
                                </div>
                            </div>
                        </div>
                    </transition>
                </Teleport>
            </main>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

// ── Page State ────────────────────────────────────
const loading = ref(true);
const submitLoading = ref(false);
const proposal = ref(null);
const decision = ref('Approved & Endorsed');
const remarks = ref('');
const formError = ref('');
const showModal = ref(false);

const checklist = reactive({
    budgetComplete: false,
    budgetComputationCorrect: false,
    supportingDocsComplete: false,
    expensesReasonable: false,
    fundingSourceIdentified: false,
    complianceVerified: false
});

const checklistLabels = {
    budgetComplete: 'Line-Item Budget is complete and matches all pages',
    budgetComputationCorrect: 'Budget computations are mathematically correct',
    supportingDocsComplete: 'All supporting financial files are attached and verified',
    expensesReasonable: 'Proposed expenses are reasonable and justified',
    fundingSourceIdentified: 'Funding source is officially identified and compliant',
    complianceVerified: 'Compliance with standard government/university funding guidelines verified'
};

// ── Computed ──────────────────────────────────────
const isAllChecked = computed(() => Object.values(checklist).every(v => v === true));
const confirmBtnClass = computed(() => {
    if (decision.value.includes('Approved')) return 'btn-success';
    if (decision.value.includes('Returned')) return 'btn-warning';
    return 'btn-danger';
});
const modalMessage = computed(() => {
    if (decision.value.includes('Approved')) return `You are about to approve and endorse "${proposal.value?.title}". This certifies financial compliance and will be forwarded back to RII.`;
    if (decision.value.includes('Returned')) return `You are about to return "${proposal.value?.title}" for budget revision. RII will be notified with your remarks.`;
    return `You are about to permanently reject "${proposal.value?.title}". This action cannot be undone.`;
});

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
const saveAllProposals = (data) => localStorage.setItem(PROPOSAL_KEY, JSON.stringify(data));

// ── Lifecycle ─────────────────────────────────────
// TODO: Replace with backend API call
onMounted(async () => {
    loading.value = true;
    await new Promise(r => setTimeout(r, 300));
    const all = getAllProposals();
    const id = route.params.id;
    const p = all.find(x => x.id === id) || null;
    proposal.value = p;
    if (p?.validationChecklist) Object.assign(checklist, p.validationChecklist);
    if (p?.remarks) remarks.value = p.remarks;
    loading.value = false;
    document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

const handleOutsideClick = (e) => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Actions ───────────────────────────────────────
const handleSubmit = () => {
    formError.value = '';
    if (decision.value === 'Approved & Endorsed' && !isAllChecked.value) {
        formError.value = 'All compliance checklist items must be verified before approving.';
        return;
    }
    if (decision.value !== 'Approved & Endorsed' && !remarks.value.trim()) {
        formError.value = 'Remarks are required when returning or rejecting a proposal.';
        return;
    }
    showModal.value = true;
};

// TODO: Replace with backend API call (approveProposal / returnProposal / rejectProposal)
const submitDecision = async () => {
    showModal.value = false;
    submitLoading.value = true;
    await new Promise(r => setTimeout(r, 600));

    // Update in localStorage
    const all = getAllProposals();
    const idx = all.findIndex(p => p.id === proposal.value.id);
    if (idx !== -1) {
        all[idx].status = decision.value;
        all[idx].validationChecklist = { ...checklist };
        all[idx].remarks = remarks.value;
        all[idx].history = [
            ...(all[idx].history || []),
            { stage: 'OVCAF', action: decision.value, user: user.value.name, date: new Date().toLocaleString('en-PH'), notes: remarks.value }
        ];
        saveAllProposals(all);
    }

    submitLoading.value = false;
    router.push('/ovcaf/inbox');
};

const resetForm = () => {
    decision.value = 'Approved & Endorsed';
    remarks.value = '';
    formError.value = '';
    Object.keys(checklist).forEach(k => { checklist[k] = false; });
};

const formatCurrency = (v) => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 0 }).format(v);
</script>

<style scoped>

.ovcaf-main{
    width:130%;
}

/* ---------------- Layout ---------------- */

.action-bar{
    margin-bottom:1.5rem;
}

.validation-layout{
    display:grid;
    grid-template-columns:1.1fr 1fr;
    gap:24px;
}

@media(max-width:1024px){
    .validation-layout{
        grid-template-columns:1fr;
    }
}

/* ---------------- Cards ---------------- */

.card{
    background:#fff;
    border:1px solid #dde5ec;
    border-radius:18px;
    box-shadow:0 10px 28px rgba(15,23,42,.05);
}

.summary-card{
    border-left:6px solid #8b0000;
    padding:28px;
}

.decision-card{
    padding:28px;
}


/* ---------------- Summary ---------------- */

.proposal-id{
    font-family:ui-monospace,monospace;
    font-size:.95rem;
    font-weight:700;
    color:#64748b;
}

.proposal-title-text{
    font-size:2rem;
    font-weight:700;
    line-height:1.25;
    color:#0f172a;
    margin:.5rem 0 1.4rem;
}

.meta-chips{
    display:grid;
    grid-template-columns:repeat(2,1fr);
    gap:22px;
    border-top:1px solid #e5e7eb;
    padding-top:22px;
}

.chip-label{
    display:block;
    font-size:.72rem;
    text-transform:uppercase;
    letter-spacing:.08em;
    font-weight:700;
    color:#94a3b8;
    margin-bottom:5px;
}

.chip-value{
    font-size:1rem;
    font-weight:500;
    color:#334155;
}

.primary-text{
    color:#8b0000;
    font-weight:700;
}

/* ---------------- Checklist ---------------- */

.card-title{
    font-size:1.45rem;
    font-weight:700;
    color:#16213d;
    margin-bottom:8px;
}

.checklist-hint{
    color:#64748b;
    font-size:.9rem;
    margin-bottom:22px;
}

.checklist-grid{
    display:grid;
    grid-template-columns:repeat(2,1fr);
    gap:16px;
}

@media(max-width:768px){
    .checklist-grid{
        grid-template-columns:1fr;
    }
}

.checklist-item{
    display:flex;
    align-items:center;
    gap:14px;
    padding:14px;
    border:1px solid transparent;
    border-radius:12px;
    transition:.25s;
    cursor:pointer;
}

.checklist-item:hover{
    background:#f8fafc;
}

.checklist-item.checked{
    background:#eefbf3;
    border-color:#22c55e;
}

.cb-box{
    width:28px;
    height:28px;
    border:2px solid #cbd5e1;
    border-radius:8px;
    display:flex;
    justify-content:center;
    align-items:center;
    font-weight:700;
    color:#fff;
    transition:.25s;
    flex-shrink:0;
}

.cb-box.filled{
    background:#22c55e;
    border-color:#22c55e;
}

.cb-label{
    font-size:.93rem;
    color:#475569;
    line-height:1.5;
}

.all-checked-banner{
    margin-top:20px;
    background:#dcfce7;
    border:1px solid #22c55e33;
    color:#15803d;
    padding:14px 18px;
    border-radius:12px;
    font-weight:600;
}

/* ---------------- Evaluation ---------------- */

.section-label{
    display: block;
    margin-bottom: 18px;
    font-size: .95rem;
    font-weight: 700;
    color: #16213d;

    line-height:1.9;
    min-height:48px;  
}

.radio-list{
    display:flex;
    flex-direction:column;
    gap:16px;
    width: 220%;
}

.radio-option{
    display:flex;
    gap:16px;
    align-items:flex-start;
    padding:18px;
    border:1px solid #dbe4ee;
    border-radius:16px;
    transition:.25s;
    cursor:pointer;
    background:#fff;
}

.radio-option:hover{
    transform:translateY(-2px);
    box-shadow:0 10px 24px rgba(0,0,0,.05);
}

.radio-indicator{
    width:22px;
    height:22px;
    border:2px solid #cbd5e1;
    border-radius:50%;
    position:relative;
    flex-shrink:0;
    margin-top:2px;
}

.radio-option.selected .radio-indicator::after{
    content:"";
    position:absolute;
    inset:4px;
    border-radius:50%;
}

.radio-option.selected.approve{
    background:#dcfce7;
    border:2px solid #22c55e;
}

.radio-option.selected.approve .radio-indicator{
    border-color:#22c55e;
}

.radio-option.selected.approve .radio-indicator::after{
    background:#22c55e;
}

.radio-option.selected.return{
    background:#fff7ed;
    border:2px solid #f59e0b;
}

.radio-option.selected.return .radio-indicator{
    border-color:#f59e0b;
}

.radio-option.selected.return .radio-indicator::after{
    background:#f59e0b;
}

.radio-option.selected.reject{
    background:#fee2e2;
    border:2px solid #ef4444;
}

.radio-option.selected.reject .radio-indicator{
    border-color:#ef4444;
}

.radio-option.selected.reject .radio-indicator::after{
    background:#ef4444;
}

.option-title{
    display:block;
    font-size:1rem;
    font-weight:700;
    margin-bottom:6px;
}

.approve-title{
    color:#15803d;
}

.return-title{
    color:#d97706;
}

.reject-title{
    color:#dc2626;
}

.option-desc{
    color:#64748b;
    font-size:.92rem;
    line-height:1.6;
}

/* ---------------- Form ---------------- */

.required-mark{
    color:#dc2626;
}

.form-control{
    width:100%;
    padding:15px;
    border:1px solid #d5dce6;
    border-radius:12px;
    font-size:.95rem;
    color:#334155;
    transition:.2s;
}

.form-control:focus{
    outline:none;
    border-color:#8b0000;
    box-shadow:0 0 0 4px rgba(139,0,0,.08);
}

.error-banner{
    margin-top:18px;
    background:#fee2e2;
    color:#dc2626;
    border:1px solid #fecaca;
    padding:14px;
    border-radius:12px;
}

.form-actions{
    margin-top:24px;
    padding-top:20px;
    border-top:1px solid #e5e7eb;
    display:flex;
    justify-content:flex-end;
    gap:14px;
}

/* ---------------- Buttons ---------------- */

.btn{
    border-radius:10px;
    font-weight:600;
    transition:.25s;
}

.btn-secondary{
    background:#fff;
    border:1px solid #d5dce6;
    color:#475569;
    padding:12px 22px;
}

.btn-secondary:hover{
    background:#f8fafc;
}

.btn-primary{
    background:#8b0000;
    color:#fff;
    border:none;
    padding:12px 24px;
}

.btn-primary:hover{
    background:#6d0000;
}

/* ---------------- Modal ---------------- */

.modal-overlay{
    position:fixed;
    inset:0;
    background:rgba(15,23,42,.45);
    backdrop-filter:blur(4px);
    display:flex;
    justify-content:center;
    align-items:center;
    z-index:999;
}

.modal-box{
    width:90%;
    max-width:500px;
    padding:26px;
}

.modal-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    border-bottom:1px solid #e5e7eb;
    padding-bottom:14px;
    margin-bottom:18px;
}

.modal-title{
    font-size:1.2rem;
    font-weight:700;
}

.modal-close-btn{
    border:none;
    background:none;
    font-size:28px;
    cursor:pointer;
}

.modal-message{
    color:#475569;
    line-height:1.6;
}

.modal-actions{
    display:flex;
    justify-content:flex-end;
    gap:12px;
    margin-top:22px;
}

.modal-fade-enter-active,
.modal-fade-leave-active{
    transition:.2s;
}

.modal-fade-enter-from,
.modal-fade-leave-to{
    opacity:0;
}

/* ---------------- Utility ---------------- */

.mt-6{
    margin-top:24px;
}

.empty-state-box{
    text-align:center;
    padding:80px 20px;
}

.empty-title{
    font-size:1.2rem;
    font-weight:700;
}

.empty-desc{
    color:#64748b;
    margin-top:8px;
}

.sr-only{
    position:absolute;
    width:1px;
    height:1px;
    overflow:hidden;
    clip:rect(0,0,0,0);
    white-space:nowrap;
}

</style>