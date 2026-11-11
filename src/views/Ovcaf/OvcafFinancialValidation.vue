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
                                <span class="proposal-id font-mono">{{ proposal.documentId || '—' }}</span>
                                <h3 class="proposal-title-text">{{ proposal.title }}</h3>
                                <div class="meta-chips">
                                    <div><span class="chip-label">Project Leader</span><span class="chip-value">{{
                                        projectLeader }}</span></div>
                                    <div><span class="chip-label">College</span><span class="chip-value">{{
                                        proposal.college }}</span></div>
                                </div>

                                <div class="budget-document">
                                    <span class="chip-label">In-line Item Budget</span>
                                    <div v-if="budgetFileName" class="budget-document-row">
                                        <span class="budget-document-name">{{ budgetFileName }}</span>
                                        <button type="button" class="btn btn-secondary btn-sm"
                                            @click="viewBudgetDocument">View</button>
                                        <button type="button" class="btn btn-secondary btn-sm"
                                            @click="saveBudgetDocument">Download</button>
                                    </div>
                                    <!-- A stored reference that yielded no printable name is a
                                         retrieval problem, not a missing document. -->
                                    <p v-else-if="hasBudgetReference" class="document-error">
                                        The Line-Item Budget document is attached, but its reference could not be
                                        resolved.
                                    </p>
                                    <!-- The saved record could not be read, so an empty state would be
                                         an unverified claim rather than a fact. -->
                                    <p v-else-if="fullProposalError" class="document-error">
                                        The Line-Item Budget document could not be retrieved.
                                    </p>
                                    <p v-else class="budget-document-empty">No Line-Item Budget uploaded</p>
                                    <p v-if="documentError" class="document-error">{{ documentError }}</p>
                                </div>

                                <div class="summary-actions">
                                    <router-link :to="`/ovcaf/proposals/${proposal.id}`" class="btn btn-primary btn-sm">
                                        View Proposal Details
                                    </router-link>
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

                                    <!-- A proposal cannot be approved for implementation until
                                         the Office of the Chancellor has recorded its Special
                                         Order. The backend refuses the approval as well. -->
                                    <div v-if="approvalBlocked" class="error-banner">
                                        ⚠ No Special Order (SO) has been issued for this proposal.
                                        The Office of the Chancellor must record the SO (Final
                                        Approval) before it can be approved for implementation.
                                    </div>

                                    <!-- Actions -->
                                    <div class="form-actions">
                                        <button type="button" @click="resetForm" class="btn btn-secondary"
                                            :disabled="submitLoading">Reset</button>
                                        <button type="submit" class="btn btn-primary"
                                            :disabled="submitLoading || approvalBlocked">
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

        <!-- Line-Item Budget preview: opens over this page, never navigates away. -->
        <LineItemBudgetPreviewModal v-if="showBudgetPreview" :file-path="budgetDocumentPath(budgetSource)"
            :file-name="budgetFileName" @close="showBudgetPreview = false" />
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/utils/api';
import LineItemBudgetPreviewModal from '@/components/LineItemBudgetPreviewModal.vue';
import { budgetDocumentName, budgetDocumentPath } from '@/utils/budgetDocument';
import { downloadUploadedFile } from '@/utils/fileDownload';

const route = useRoute();
const router = useRouter();

// ── Page State ────────────────────────────────────
const loading = ref(true);
const submitLoading = ref(false);
const error = ref(null);
const proposal = ref(null);
// The complete saved proposal record (GET /api/proposals/{id}) - the exact record the
// proposal viewer renders. Loaded only after the OVCAF detail route has authorized this
// proposal, and preferred for the Project Leader and Line-Item Budget so the evaluation
// card can never disagree with the viewer about the same proposal.
const fullProposal = ref(null);
const fullProposalError = ref(false);
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

// ── Project Leader and Line-Item Budget ───────────
// Both are read from the saved proposal record the actual viewer uses when it is
// available; the OVCAF response is only the fallback while that record loads. The
// Project Leader is the value typed into the proposal form, never the login account.
const budgetSource = computed(() => fullProposal.value || proposal.value);
const projectLeader = computed(() => {
    const fromForm = fullProposal.value
        ? String(fullProposal.value.projectLeader || '').trim()
        : '';
    return fromForm || proposal.value?.projectLeader || 'Unknown';
});
// Named from the proposal record (name, or the stored path when the name was lost), so
// an attached document is never reported as missing.
const budgetFileName = computed(() => budgetDocumentName(budgetSource.value));
const hasBudgetReference = computed(() => Boolean(budgetDocumentPath(budgetSource.value)));
const documentError = ref('');

// An implementation approval is only valid once OC has issued the Special Order.
// The backend enforces the same rule; showing it here explains why approval is
// unavailable instead of letting the reviewer submit and receive an error.
const hasIssuedSo = computed(() => {
    const value = fullProposal.value ? fullProposal.value.soNumber : proposal.value?.soNumber;
    return typeof value === 'string' && value.trim() !== '';
});
const approvalBlocked = computed(() => decision.value === 'Approved & Endorsed' && !hasIssuedSo.value);

// View opens an in-page modal with the actual uploaded document; the user stays on
// this evaluation page (no navigation, no new tab) and nothing is uploaded again.
const showBudgetPreview = ref(false);

const viewBudgetDocument = () => {
    if (!budgetDocumentPath(budgetSource.value)) {
        documentError.value = 'The Line-Item Budget document could not be retrieved.';
        return;
    }
    documentError.value = '';
    showBudgetPreview.value = true;
};

const saveBudgetDocument = async () => {
    const path = budgetDocumentPath(budgetSource.value);
    if (!path) {
        documentError.value = 'The Line-Item Budget document could not be retrieved.';
        return;
    }
    documentError.value = '';
    try {
        await downloadUploadedFile(path, budgetFileName.value);
    } catch (err) {
        console.error('Error downloading Line-Item Budget:', err);
        documentError.value = 'The Line-Item Budget document could not be downloaded. Please try again.';
    }
};

// ── Computed ──────────────────────────────────────
const isAllChecked = computed(() => Object.values(checklist).every(v => v === true));
const confirmBtnClass = computed(() => {
    if (decision.value.includes('Approved')) return 'btn-success';
    if (decision.value.includes('Returned')) return 'btn-warning';
    return 'btn-danger';
});
const modalMessage = computed(() => {
    if (decision.value.includes('Approved')) return `You are about to approve and endorse "${proposal.value?.title}". This certifies financial compliance and the proposal will be approved for implementation with or without funding.`;
    if (decision.value.includes('Returned')) return `You are about to return "${proposal.value?.title}" for budget revision. The proponent will be notified with your remarks.`;
    return `You are about to permanently reject "${proposal.value?.title}". This action cannot be undone.`;
});

/**
 * Load the complete saved proposal record used by the proposal viewer. The OVCAF detail
 * route above has already authorized this proposal, so this never exposes a draft. A
 * failure here keeps the OVCAF response (and its retrieval messages) as the fallback.
 */
const loadFullProposal = async (id) => {
    try {
        const res = await api.get(`/api/proposals/${id}`);
        fullProposal.value = res.data || null;
        fullProposalError.value = !fullProposal.value;
    } catch (err) {
        console.error('Error fetching the complete proposal:', err);
        fullProposal.value = null;
        fullProposalError.value = true;
    }
};

// ── Fetch proposal from API ───────────────────────
const fetchProposal = async () => {
    try {
        loading.value = true;
        error.value = null;
        fullProposal.value = null;
        fullProposalError.value = false;
        documentError.value = '';
        showBudgetPreview.value = false;
        const id = route.params.id;
        const response = await api.get(`/api/ovcaf/proposals/${id}`);
        const p = response.data;
        if (p) {
            proposal.value = {
                id: p.id,
                documentId: p.documentId || '',
                title: p.projectTitle || 'Untitled Project',
                // The form's Project Leader must win; `proponentName` is only the
                // server-side fallback for records that never carried the form field.
                projectLeader: p.projectLeader || p.proponentName || '',
                college: p.college || 'Unknown',
                // Section 25 is an uploaded document; it is the budget source OVCAF reviews.
                beneficiariesFileName: p.beneficiariesFileName || '',
                beneficiariesFilePath: p.beneficiariesFilePath || '',
                // The SO must already exist before the proposal can be approved for
                // implementation. `fullProposal` below is the fallback for records the
                // OVCAF response did not expose.
                soNumber: p.soNumber || '',
                status: p.status,
                rawStatus: p.status
            };
            // Prefer the exact record the proposal viewer renders. This is also the
            // record that still carries the attachment when an older OVCAF response
            // did not expose it.
            await loadFullProposal(id);
            // Pre-fill checklist if validation exists
            if (p.budgetComplete !== undefined) checklist.budgetComplete = p.budgetComplete;
            if (p.budgetComputationCorrect !== undefined) checklist.budgetComputationCorrect = p.budgetComputationCorrect;
            if (p.supportingDocsComplete !== undefined) checklist.supportingDocsComplete = p.supportingDocsComplete;
            if (p.expensesReasonable !== undefined) checklist.expensesReasonable = p.expensesReasonable;
            if (p.fundingSourceIdentified !== undefined) checklist.fundingSourceIdentified = p.fundingSourceIdentified;
            if (p.complianceVerified !== undefined) checklist.complianceVerified = p.complianceVerified;
            if (p.ovcafRemarks) remarks.value = p.ovcafRemarks;
        }
    } catch (err) {
        console.error('Error fetching proposal for validation:', err);
        error.value = 'Failed to load proposal. Please try again.';
        proposal.value = null;
    } finally {
        loading.value = false;
    }
};

// ── Lifecycle ─────────────────────────────────────
onMounted(async () => {
    await fetchProposal();
});

// ── Actions ───────────────────────────────────────
const handleSubmit = () => {
    formError.value = '';
    if (approvalBlocked.value) {
        formError.value = 'No Special Order (SO) has been issued for this proposal. The Office of the Chancellor must record the SO before implementation approval.';
        return;
    }
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

// Submit validation decision to backend
const submitDecision = async () => {
    showModal.value = false;
    submitLoading.value = true;

    try {
        // Map frontend decision to backend decision
        const backendDecision = decision.value === 'Approved & Endorsed' ? 'APPROVED_ENDORSED' :
            decision.value === 'Returned for Revision' ? 'RETURNED_FOR_REVISION' :
                'REJECTED';

        const payload = {
            decision: backendDecision,
            remarks: remarks.value,
            budgetComplete: checklist.budgetComplete,
            budgetComputationCorrect: checklist.budgetComputationCorrect,
            supportingDocsComplete: checklist.supportingDocsComplete,
            expensesReasonable: checklist.expensesReasonable,
            fundingSourceIdentified: checklist.fundingSourceIdentified,
            complianceVerified: checklist.complianceVerified
        };

        await api.post(`/api/ovcaf/proposals/${proposal.value.id}/validate`, payload);

        // Navigate back to inbox on success
        router.push('/ovcaf/inbox');
    } catch (err) {
        console.error('Error submitting validation:', err);
        // Surface the backend's domain message (for example the missing-SO rule)
        // instead of hiding it behind a generic retry prompt.
        const body = err.response?.data;
        formError.value =
            (body && typeof body === 'object' ? body.error : body) ||
            'Failed to submit validation. Please try again.';
    } finally {
        submitLoading.value = false;
    }
};

const resetForm = () => {
    decision.value = 'Approved & Endorsed';
    remarks.value = '';
    formError.value = '';
    Object.keys(checklist).forEach(k => { checklist[k] = false; });
};
</script>

<style scoped>
.ovcaf-main {
    width: 100%;
}

/* ---------------- Layout ---------------- */

.action-bar {
    margin-bottom: 1.5rem;
}

.validation-layout {
    display: grid;
    grid-template-columns: 1.1fr 1fr;
    gap: 24px;
}

@media(max-width:1024px) {
    .validation-layout {
        grid-template-columns: 1fr;
    }
}

/* ---------------- Cards ---------------- */

.card {
    background: #fff;
    border: 1px solid #dde5ec;
    border-radius: 18px;
    box-shadow: 0 10px 14px rgba(15, 23, 42, .05);
}

.summary-card {
    border-left: 6px solid #8b0000;
    padding: 12px;
}

.decision-card {
    padding: 12px;
}


/* ---------------- Summary ---------------- */

.proposal-id {
    font-family: ui-monospace, monospace;
    font-size: .95rem;
    font-weight: 700;
    color: #64748b;
}

.proposal-title-text {
    font-size: 2rem;
    font-weight: 700;
    line-height: 1.25;
    color: #0f172a;
    margin: .5rem 0 1.4rem;
}

.meta-chips {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 22px;
    border-top: 1px solid #e5e7eb;
    padding-top: 22px;
}

.chip-label {
    display: block;
    font-size: .72rem;
    text-transform: uppercase;
    letter-spacing: .08em;
    font-weight: 700;
    color: #94a3b8;
    margin-bottom: 5px;
}

.chip-value {
    font-size: 1rem;
    font-weight: 500;
    color: #334155;
}

.primary-text {
    color: #8b0000;
    font-weight: 700;
}

/* ---------------- In-line Item Budget ---------------- */

.budget-document {
    margin-top: 22px;
    padding-top: 18px;
    border-top: 1px solid #e5e7eb;
}

.budget-document-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.budget-document-name {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px;
    border: 1px solid #d5dce6;
    border-radius: 10px;
    background: #f8fafc;
    color: #334155;
    font-size: .92rem;
    font-weight: 600;
    word-break: break-word;
}

.budget-document-empty {
    color: #94a3b8;
    font-size: .92rem;
    font-style: italic;
}

.document-error {
    margin-top: 10px;
    color: #dc2626;
    font-size: .85rem;
}

.summary-actions {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.btn-sm {
    padding: 8px 16px;
    font-size: .85rem;
}

/* ---------------- Checklist ---------------- */

.card-title {
    font-size: 1.45rem;
    font-weight: 700;
    color: #16213d;
    margin-bottom: 8px;
}

.checklist-hint {
    color: #64748b;
    font-size: .9rem;
    margin-bottom: 22px;
}

.checklist-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

@media(max-width:768px) {
    .checklist-grid {
        grid-template-columns: 1fr;
    }
}

.checklist-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px;
    border: 1px solid transparent;
    border-radius: 12px;
    transition: .25s;
    cursor: pointer;
}

.checklist-item:hover {
    background: #f8fafc;
}

.checklist-item.checked {
    background: #eefbf3;
    border-color: #22c55e;
}

.cb-box {
    width: 18px;
    height: 18px;
    border: 2px solid #cbd5e1;
    border-radius: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: 700;
    color: #fff;
    transition: .25s;
    flex-shrink: 0;
}

.cb-box.filled {
    background: #22c55e;
    border-color: #22c55e;
}

.cb-label {
    font-size: .93rem;
    color: #475569;
    line-height: 1.5;
}

.all-checked-banner {
    margin-top: 20px;
    background: var(--status-approved-bg);
    border: 1px solid var(--status-approved-border);
    color: var(--status-approved-text);
    padding: 14px 18px;
    border-radius: 12px;
    font-weight: 600;
}

/* ---------------- Evaluation ---------------- */

.section-label {
    display: block;
    margin-bottom: 18px;
    font-size: .95rem;
    font-weight: 700;
    color: #16213d;

    line-height: 1.9;
    min-height: 48px;
}

.radio-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 100%;
}

.radio-option {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    padding: 18px;
    border: 1px solid #dbe4ee;
    border-radius: 16px;
    transition: .25s;
    cursor: pointer;
    background: #fff;
}

.radio-option:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 24px rgba(0, 0, 0, .05);
}

.radio-indicator {
    width: 22px;
    height: 22px;
    border: 2px solid #cbd5e1;
    border-radius: 50%;
    position: relative;
    flex-shrink: 0;
    margin-top: 2px;
}

.radio-option.selected .radio-indicator::after {
    content: "";
    position: absolute;
    inset: 4px;
    border-radius: 50%;
}

.radio-option.selected.approve {
    background: var(--status-approved-bg);
    border: 2px solid var(--status-approved-solid);
}

.radio-option.selected.approve .radio-indicator {
    border-color: var(--status-approved-solid);
}

.radio-option.selected.approve .radio-indicator::after {
    background: var(--status-approved-solid);
}

.radio-option.selected.return {
    background: var(--status-returned-bg);
    border: 2px solid var(--status-returned-solid);
}

.radio-option.selected.return .radio-indicator {
    border-color: var(--status-returned-solid);
}

.radio-option.selected.return .radio-indicator::after {
    background: var(--status-returned-solid);
}

.radio-option.selected.reject {
    background: var(--status-rejected-bg);
    border: 2px solid var(--status-rejected-solid);
}

.radio-option.selected.reject .radio-indicator {
    border-color: var(--status-rejected-solid);
}

.radio-option.selected.reject .radio-indicator::after {
    background: var(--status-rejected-solid);
}

.option-title {
    display: block;
    font-size: 1rem;
    font-weight: 700;
    margin-bottom: 6px;
}

.approve-title {
    color: var(--status-approved-text);
}

.return-title {
    color: var(--status-returned-text);
}

.reject-title {
    color: var(--status-rejected-text);
}

.option-desc {
    color: #64748b;
    font-size: .92rem;
    line-height: 1.6;
}

/* ---------------- Form ---------------- */

.required-mark {
    color: #dc2626;
}

.form-control {
    width: 100%;
    padding: 15px;
    border: 1px solid #d5dce6;
    border-radius: 12px;
    font-size: .95rem;
    color: #334155;
    transition: .2s;
}

.form-control:focus {
    outline: none;
    border-color: #8b0000;
    box-shadow: 0 0 0 4px rgba(139, 0, 0, .08);
}

.error-banner {
    margin-top: 18px;
    background: #fee2e2;
    color: #dc2626;
    border: 1px solid #fecaca;
    padding: 14px;
    border-radius: 12px;
}

.form-actions {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #e5e7eb;
    display: flex;
    justify-content: flex-end;
    gap: 14px;
}

/* ---------------- Buttons ---------------- */

.btn {
    border-radius: 10px;
    font-weight: 600;
    transition: .25s;
}

/* Layout only; the colours come from assets/actions.css. */
.btn-secondary {
    padding: 12px 22px;
}

.btn-primary {
    padding: 12px 24px;
}

/* ---------------- Modal ---------------- */

.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, .45);
    backdrop-filter: blur(4px);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 999;
}

.modal-box {
    width: 90%;
    max-width: 500px;
    padding: 26px;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 14px;
    margin-bottom: 18px;
}

.modal-title {
    font-size: 1.2rem;
    font-weight: 700;
}

.modal-close-btn {
    border: none;
    background: none;
    font-size: 28px;
    cursor: pointer;
}

.modal-message {
    color: #475569;
    line-height: 1.6;
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 22px;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
    transition: .2s;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
    opacity: 0;
}

/* ---------------- Utility ---------------- */

.mt-6 {
    margin-top: 24px;
}

.empty-state-box {
    text-align: center;
    padding: 80px 20px;
}

.empty-title {
    font-size: 1.2rem;
    font-weight: 700;
}

.empty-desc {
    color: #64748b;
    margin-top: 8px;
}

.sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
}
</style>