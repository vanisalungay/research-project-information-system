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
                            <!-- Proposal information card. Every value is read from the saved
                                 proposal record; the header/document code, leader, college and
                                 attachment are never sample data. -->
                            <div class="card summary-card">
                                <header class="oc-header">
                                    <span class="oc-doc-badge">
                                        <svg class="oc-doc-icon" viewBox="0 0 24 24" width="14" height="14"
                                            fill="none" stroke="currentColor" stroke-width="2"
                                            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                            <polyline points="14 2 14 8 20 8" />
                                        </svg>
                                        <span class="oc-doc-code">{{ proposal.documentId || '—' }}</span>
                                    </span>
                                    <h3 class="oc-title">{{ proposal.title }}</h3>
                                </header>

                                <div class="oc-body">
                                    <section class="oc-info" aria-label="Project information">
                                        <div class="oc-info-col">
                                            <span class="oc-label">Project Leader</span>
                                            <div class="oc-leader">
                                                <span class="oc-avatar" aria-hidden="true">{{ projectLeaderInitials
                                                    }}</span>
                                                <span class="oc-leader-name">{{ projectLeader }}</span>
                                            </div>
                                        </div>
                                        <div class="oc-info-col">
                                            <span class="oc-label">College</span>
                                            <div class="oc-college">
                                                <span class="oc-college-name">{{ collegeName }}</span>
                                                <span v-if="collegeAbbreviation" class="oc-college-badge">{{
                                                    collegeAbbreviation }}</span>
                                            </div>
                                        </div>
                                    </section>

                                    <section class="oc-budget" aria-label="In-line Item Budget">
                                        <span class="oc-label">In-line Item Budget</span>
                                        <div v-if="budgetFileName" class="oc-file">
                                            <span class="oc-file-icon" aria-hidden="true">
                                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none"
                                                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                                                    stroke-linejoin="round">
                                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                                    <polyline points="14 2 14 8 20 8" />
                                                    <path d="M8 13h4" />
                                                    <path d="M8 17h6" />
                                                </svg>
                                            </span>
                                            <div class="oc-file-info">
                                                <span class="oc-file-name" :title="budgetFileName">{{ budgetFileName
                                                    }}</span>
                                                <span class="oc-file-meta">{{ budgetFileType }}</span>
                                            </div>
                                            <div class="oc-file-actions">
                                                <button type="button" class="oc-view-btn"
                                                    @click="viewBudgetDocument">
                                                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none"
                                                        stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                                        stroke-linejoin="round" aria-hidden="true">
                                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                        <circle cx="12" cy="12" r="3" />
                                                    </svg>
                                                    <span>View</span>
                                                </button>
                                                <button type="button" class="oc-download-btn"
                                                    :aria-label="'Download ' + budgetFileName" title="Download"
                                                    @click="saveBudgetDocument">
                                                    <svg viewBox="0 0 24 24" width="17" height="17" fill="none"
                                                        stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                                        stroke-linejoin="round" aria-hidden="true">
                                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                                                        <polyline points="7 10 12 15 17 10" />
                                                        <line x1="12" y1="15" x2="12" y2="3" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </div>
                                        <!-- A stored reference that yielded no printable name is a
                                             retrieval problem, not a missing document. -->
                                        <p v-else-if="hasBudgetReference" class="oc-file-state oc-file-state-error">
                                            The Line-Item Budget document is attached, but its reference could not be
                                            resolved.
                                        </p>
                                        <!-- The saved record could not be read, so an empty state would be
                                             an unverified claim rather than a fact. -->
                                        <p v-else-if="fullProposalError" class="oc-file-state oc-file-state-error">
                                            The Line-Item Budget document could not be retrieved.
                                        </p>
                                        <p v-else class="oc-file-state">No Line-Item Budget uploaded</p>
                                        <p v-if="documentError" class="oc-file-state oc-file-state-error">
                                            {{ documentError }}</p>
                                    </section>

                                    <button ref="detailsTriggerRef" type="button" class="oc-details-btn"
                                        @click="openProposalDetails">
                                        <span>View Proposal Details</span>
                                        <svg viewBox="0 0 24 24" width="17" height="17" fill="none"
                                            stroke="currentColor" stroke-width="2.2" stroke-linecap="round"
                                            stroke-linejoin="round" aria-hidden="true">
                                            <line x1="5" y1="12" x2="19" y2="12" />
                                            <polyline points="12 5 19 12 12 19" />
                                        </svg>
                                    </button>
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
                                                :class="{ 'selected approve': decision === 'APPROVED_ENDORSED' }">
                                                <input type="radio" v-model="decision" value="APPROVED_ENDORSED"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title approve-title">✅ {{
                                                        ovcafDecisionLabel('APPROVED_ENDORSED') }}</span>
                                                    <span class="option-desc">Budget meets all compliance conditions.
                                                        Proposal will be endorsed back to RII with OVCAF
                                                        certification.</span>
                                                </div>
                                            </label>
                                            <label class="radio-option"
                                                :class="{ 'selected return': decision === 'RETURNED_FOR_REVISION' }">
                                                <input type="radio" v-model="decision" value="RETURNED_FOR_REVISION"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title return-title">🔄 {{
                                                        ovcafDecisionLabel('RETURNED_FOR_REVISION') }}</span>
                                                    <span class="option-desc">Budget requires corrections. Proposal is
                                                        sent back with revision instructions.</span>
                                                </div>
                                            </label>
                                            <label class="radio-option"
                                                :class="{ 'selected reject': decision === 'REJECTED' }">
                                                <input type="radio" v-model="decision" value="REJECTED"
                                                    class="sr-only" />
                                                <div class="radio-indicator"></div>
                                                <div>
                                                    <span class="option-title reject-title">❌ {{
                                                        ovcafDecisionLabel('REJECTED') }}</span>
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
                                            <span v-if="decision !== 'APPROVED_ENDORSED'" class="required-mark">*
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
                                    <h4 class="modal-title">Confirm: {{ ovcafDecisionLabel(decision) }}</h4>
                                    <button @click="showModal = false" class="modal-close-btn">&times;</button>
                                </div>
                                <p class="modal-message">{{ modalMessage }}</p>
                                <div class="modal-actions">
                                    <button type="button" @click="showModal = false"
                                        class="btn btn-secondary">Cancel</button>
                                    <button type="button" @click="submitDecision" class="btn"
                                        :class="confirmBtnClass">Confirm &amp; Proceed</button>
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

        <!-- Proposal details: the same read-only viewer the details route renders, opened
             in place so the evaluator never loses the checklist/remarks they are editing. -->
        <Teleport to="body">
            <transition name="modal-fade">
                <div v-if="showProposalDetails && proposal" class="proposal-details-overlay"
                    @click.self="closeProposalDetails">
                    <section class="proposal-details-surface" role="dialog" aria-modal="true"
                        aria-labelledby="ovcaf-proposal-details-title">
                        <header class="proposal-details-header">
                            <div class="proposal-details-heading">
                                <h2 id="ovcaf-proposal-details-title">Proposal Details</h2>
                                <p class="proposal-details-subtitle">
                                    {{ proposal.documentId || '—' }} · {{ proposal.title }}
                                </p>
                            </div>
                            <button ref="detailsCloseRef" type="button" class="proposal-details-close"
                                aria-label="Close proposal details" @click="closeProposalDetails">✕</button>
                        </header>
                        <div class="proposal-details-body">
                            <OvcafProposalDetails :proposal-id="proposal.id" embedded
                                @preview-change="detailsPreviewOpen = $event" />
                        </div>
                    </section>
                </div>
            </transition>
        </Teleport>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/utils/api';
import LineItemBudgetPreviewModal from '@/components/LineItemBudgetPreviewModal.vue';
import OvcafProposalDetails from './OvcafProposalDetails.vue';
import { budgetDocumentName, budgetDocumentPath } from '@/utils/budgetDocument';
import { collegeDisplayParts, fileTypeDescription, initialsFromName } from '@/utils/proposalCardDisplay';
import { downloadUploadedFile } from '@/utils/fileDownload';
import { getScrollContainer } from '@/utils/scrollReset';
import { ovcafDecisionLabel } from '@/utils/statusCatalog.js';

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
const decision = ref('APPROVED_ENDORSED');
const remarks = ref('');
const formError = ref('');
const showModal = ref(false);

// ── Proposal details modal ────────────────────────
// The read-only viewer is opened in place so the evaluator never loses the checklist and
// remarks they are editing. The nested budget preview reports when it is open, so Escape
// closes only the top layer.
const showProposalDetails = ref(false);
const detailsPreviewOpen = ref(false);
const detailsTriggerRef = ref(null);
const detailsCloseRef = ref(null);

let isBackgroundScrollLocked = false;
let lockedBodyOverflow = '';
let lockedContainer = null;
let lockedContainerOverflow = '';

// The authenticated layout scrolls main.app-content, not the document body, so both are
// frozen while the modal is open and restored to their exact previous values on close.
const lockBackgroundScroll = () => {
    if (isBackgroundScrollLocked) return;
    const body = document.body;
    lockedBodyOverflow = body.style.overflow;
    body.style.overflow = 'hidden';

    const container = getScrollContainer();
    if (container && container !== body) {
        lockedContainer = container;
        lockedContainerOverflow = container.style.overflow;
        container.style.overflow = 'hidden';
    }
    isBackgroundScrollLocked = true;
};

const unlockBackgroundScroll = () => {
    if (!isBackgroundScrollLocked) return;
    document.body.style.overflow = lockedBodyOverflow;
    if (lockedContainer) {
        lockedContainer.style.overflow = lockedContainerOverflow;
        lockedContainer = null;
    }
    isBackgroundScrollLocked = false;
};

const openProposalDetails = () => {
    if (!proposal.value?.id) return;
    detailsPreviewOpen.value = false;
    showProposalDetails.value = true;
};

const closeProposalDetails = () => {
    showProposalDetails.value = false;
};

watch(showProposalDetails, async (open) => {
    if (open) {
        lockBackgroundScroll();
        await nextTick();
        detailsCloseRef.value?.focus?.();
        return;
    }
    unlockBackgroundScroll();
    detailsPreviewOpen.value = false;
    detailsTriggerRef.value?.focus?.();
});

const onDetailsKeydown = (event) => {
    if (event.key !== 'Escape' || !showProposalDetails.value) return;
    // The nested budget preview handles its own Escape; do not close both at once.
    if (detailsPreviewOpen.value) return;
    closeProposalDetails();
};

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

// ── Card presentation ─────────────────────────────
// The card's avatar, college badge and file type are derived from the same saved values
// it already shows; nothing here invents a person, a college or a document.
const projectLeaderInitials = computed(() => initialsFromName(projectLeader.value));
const collegeParts = computed(() => collegeDisplayParts(proposal.value?.college));
const collegeName = computed(() => collegeParts.value.name);
const collegeAbbreviation = computed(() => collegeParts.value.abbreviation);
const budgetFileType = computed(() => fileTypeDescription(budgetFileName.value));

// An implementation approval is only valid once OC has issued the Special Order.
// The backend enforces the same rule; showing it here explains why approval is
// unavailable instead of letting the reviewer submit and receive an error.
const hasIssuedSo = computed(() => {
    const value = fullProposal.value ? fullProposal.value.soNumber : proposal.value?.soNumber;
    return typeof value === 'string' && value.trim() !== '';
});
const approvalBlocked = computed(() => decision.value === 'APPROVED_ENDORSED' && !hasIssuedSo.value);

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
    if (decision.value === 'APPROVED_ENDORSED') return 'btn-success';
    if (decision.value === 'RETURNED_FOR_REVISION') return 'btn-warning';
    return 'btn-danger';
});
const modalMessage = computed(() => {
    if (decision.value === 'APPROVED_ENDORSED') return `You are about to approve and endorse "${proposal.value?.title}". This certifies financial compliance and the proposal will be approved for implementation with or without funding.`;
    if (decision.value === 'RETURNED_FOR_REVISION') return `You are about to return "${proposal.value?.title}" for budget revision. The proponent will be notified with your remarks.`;
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
    window.addEventListener('keydown', onDetailsKeydown);
    await fetchProposal();
});

onBeforeUnmount(() => {
    window.removeEventListener('keydown', onDetailsKeydown);
    unlockBackgroundScroll();
});

// ── Actions ───────────────────────────────────────
const handleSubmit = () => {
    formError.value = '';
    if (approvalBlocked.value) {
        formError.value = 'No Special Order (SO) has been issued for this proposal. The Office of the Chancellor must record the SO before implementation approval.';
        return;
    }
    if (decision.value === 'APPROVED_ENDORSED' && !isAllChecked.value) {
        formError.value = 'All compliance checklist items must be verified before approving.';
        return;
    }
    if (decision.value !== 'APPROVED_ENDORSED' && !remarks.value.trim()) {
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
        // The radio model already holds the backend decision value; the canonical
        // wording ("Approved") is display-only.
        const payload = {
            decision: decision.value,
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
    decision.value = 'APPROVED_ENDORSED';
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


/* ---------------- Proposal information card ---------------- */
/* Reference-led redesign: navy header with the document code and proposal title, a light
   project-information panel, the uploaded Line-Item Budget row, and one full-width action.
   The card keeps its existing data bindings and handlers; only its presentation changed. */

.summary-card {
    --oc-navy: #211b3d;
    --oc-yellow: #ffc928;
    --oc-royal: #3155f5;
    --oc-ink: #1b2140;
    --oc-muted: #7c86a8;
    --oc-panel: #f5f6fc;
    --oc-panel-border: #e5e8f3;
    --oc-file-border: #e7eaf3;

    overflow: hidden;
    padding: 0;
    border: none;
    border-left: none;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 18px 40px rgba(24, 32, 63, .12), 0 2px 6px rgba(24, 32, 63, .05);
}

.oc-header {
    position: relative;
    padding: 24px 22px 22px;
    background: var(--oc-navy);
}

/* The 3px yellow divider along the bottom edge of the header. */
.oc-header::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 3px;
    background: var(--oc-yellow);
}

.oc-doc-badge {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    min-width: 0;
    max-width: 100%;
    padding: 6px 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, .12);
    color: #d9ddf1;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: .7rem;
    font-weight: 700;
    line-height: 1.4;
    letter-spacing: .03em;
}

.oc-doc-icon {
    flex: 0 0 auto;
    opacity: .8;
}

/* The full document code is preserved; it wraps inside the pill instead of overflowing. */
.oc-doc-code {
    min-width: 0;
    white-space: normal;
    overflow-wrap: anywhere;
}

.oc-title {
    margin: 16px 0 0;
    color: #fff;
    font-size: 1.35rem;
    font-weight: 700;
    line-height: 1.35;
    letter-spacing: .01em;
    overflow-wrap: anywhere;
}

.oc-body {
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding: 22px;
    background: transparent;
}

/* ---------------- Project information panel ---------------- */

.oc-info {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
    padding: 16px;
    border: 1px solid var(--oc-panel-border);
    border-radius: 13px;
    background: var(--oc-panel);
}

.oc-info-col {
    min-width: 0;
}

.oc-label {
    display: block;
    margin-bottom: 9px;
    color: var(--oc-muted);
    font-size: .67rem;
    font-weight: 700;
    line-height: 1.4;
    letter-spacing: .09em;
    text-transform: uppercase;
}

.oc-leader {
    display: flex;
    align-items: center;
    gap: 11px;
    min-width: 0;
}

.oc-avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    width: 43px;
    height: 43px;
    border-radius: 50%;
    background: var(--oc-navy);
    color: var(--oc-yellow);
    font-size: .84rem;
    font-weight: 700;
    letter-spacing: .02em;
}

.oc-leader-name,
.oc-college-name {
    color: var(--oc-ink);
    font-size: .875rem;
    font-weight: 600;
    line-height: 1.45;
    overflow-wrap: anywhere;
}

.oc-leader-name {
    min-width: 0;
}

.oc-college {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    min-width: 0;
}

.oc-college-badge {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 999px;
    background: #e8edff;
    color: #2563eb;
    font-size: .66rem;
    font-weight: 700;
    line-height: 1.5;
    letter-spacing: .05em;
}

/* ---------------- In-line Item Budget ---------------- */

.oc-budget {
    display: flex;
    flex-direction: column;
}

.oc-file {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 11px;
    min-width: 0;
    padding: 10px 12px;
    border: 1px solid var(--oc-file-border);
    border-radius: 12px;
    background: #fff;
}

.oc-file-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: #fff0ee;
    color: #ef4444;
}

.oc-file-info {
    display: flex;
    flex-direction: column;
    gap: 1px;
    flex: 1 1 130px;
    min-width: 0;
}

/* Long upload names ellipsize; the actions never get pushed out of the container. */
.oc-file-name {
    color: var(--oc-ink);
    font-size: .8rem;
    font-weight: 600;
    line-height: 1.35;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.oc-file-meta {
    color: var(--oc-muted);
    font-size: .7rem;
    line-height: 1.4;
}

.oc-file-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
    margin-left: auto;
}

.oc-view-btn,
.oc-download-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 42px;
    border-radius: 9px;
    font-family: inherit;
    cursor: pointer;
    transition: background-color .18s ease, border-color .18s ease, transform .18s ease;
}

.oc-view-btn {
    gap: 6px;
    padding: 0 14px;
    border: none;
    background: #e8edff;
    color: var(--oc-royal);
    font-size: .8rem;
    font-weight: 700;
}

.oc-view-btn:hover {
    background: #dbe2ff;
}

.oc-download-btn {
    width: 42px;
    padding: 0;
    border: 1px solid #e0e5f2;
    background: #fff;
    color: var(--oc-ink);
}

.oc-download-btn:hover {
    border-color: #cfd7ea;
    background: #f6f8fd;
}

.oc-view-btn:active,
.oc-download-btn:active {
    transform: translateY(1px);
}

.oc-view-btn:focus-visible,
.oc-download-btn:focus-visible,
.oc-details-btn:focus-visible {
    outline: 2px solid var(--oc-royal);
    outline-offset: 2px;
}

.oc-file-state {
    margin: 0;
    padding: 12px 14px;
    border: 1px dashed var(--oc-panel-border);
    border-radius: 12px;
    color: var(--oc-muted);
    font-size: .82rem;
}

.oc-file-state-error {
    margin-top: 8px;
    border-style: solid;
    color: #dc2626;
}

/* ---------------- View Proposal Details ---------------- */

.oc-details-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 9px;
    width: 100%;
    min-height: 49px;
    padding: 0 18px;
    border: none;
    border-radius: 11px;
    background: var(--oc-royal);
    color: #fff;
    font-family: inherit;
    font-size: .92rem;
    font-weight: 700;
    cursor: pointer;
    transition: background-color .18s ease, transform .18s ease, box-shadow .18s ease;
}

.oc-details-btn:hover {
    background: #2846de;
    box-shadow: 0 10px 20px rgba(49, 85, 245, .22);
}

.oc-details-btn:active {
    transform: translateY(1px);
    box-shadow: none;
}

@media (max-width: 560px) {
    .oc-info {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 480px) {

    .oc-header {
        padding: 20px 17px 19px;
    }

    .oc-body {
        padding: 17px;
    }

    .oc-file-actions {
        width: 100%;
        margin-left: 0;
        justify-content: flex-end;
    }
}

/* The card keeps the reference palette in dark mode too; base.css otherwise repaints
   every card/section/span, which would erase the navy header and the light panel. */
@media (prefers-color-scheme: dark) {
    .summary-card {
        border: none !important;
        background: #1e293b !important;
    }

    .oc-title {
        color: #f8fafc !important;
    }

    .oc-doc-code {
        color: #e2e8f0 !important;
    }

    .oc-label {
        color: #a9b4cf !important;
    }

    .oc-avatar {
        color: var(--oc-yellow) !important;
    }

    .oc-leader-name,
    .oc-college-name {
        color: #f1f5f9 !important;
    }

    .oc-info {
        border-color: #334155 !important;
        background: #242c42 !important;
    }

    .oc-college-badge {
        background: #2b3a63;
        color: #93b4ff !important;
    }

    .oc-file {
        border-color: #334155 !important;
        background: #1b2436 !important;
    }

    .oc-file-icon {
        background: #38242b;
        color: #fb7185 !important;
    }

    .oc-file-name {
        color: #f1f5f9 !important;
    }

    .oc-file-meta {
        color: #9aa6c0 !important;
    }

    .oc-file-state {
        border-color: #3b475f;
        color: #a9b4cf !important;
    }

    .oc-file-state-error {
        color: #f87171 !important;
    }

    .oc-view-btn {
        background: #26345a;
        color: #93b4ff !important;
    }

    .oc-view-btn:hover {
        background: #2e3f6b;
    }

    .oc-download-btn {
        border-color: #3b475f;
        background: #1e293b;
        color: #93b4ff !important;
    }

    .oc-download-btn:hover {
        background: #26324a;
    }

    .oc-details-btn {
        background: #3155f5 !important;
        color: #fff !important;
    }

    /* base.css forces every span to near-white; keep nested labels on their button. */
    .oc-view-btn span,
    .oc-details-btn span {
        color: inherit !important;
    }
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
    /* The row is the containing block for the visually-hidden checkbox. Without this the
       absolutely positioned input resolves against the initial containing block, and the
       browser's focus scroll moves the whole document instead of this page's scroller. */
    position: relative;
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
    /* Same containment as .checklist-item: the hidden radio must not be positioned
       against the initial containing block. */
    position: relative;
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
    max-height: calc(100dvh - 32px);
    overflow-y: auto;
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

.modal-actions .btn {
    min-width: 140px;
    justify-content: center;
    white-space: nowrap;
}

/* Cancel: a visible neutral surface instead of white-on-white. */
.modal-actions .btn-secondary {
    background: #f1f5f9 !important;
    border-color: #cbd5e1 !important;
    color: #334155 !important;
}

.modal-actions .btn-secondary:hover:not(:disabled) {
    background: #e2e8f0 !important;
}

.modal-actions .btn-secondary:active:not(:disabled) {
    background: #cbd5e1 !important;
}

/* Confirm: explicit semantic tones from the shared action tokens. */
.modal-actions .btn-success {
    background: var(--btn-approve-bg) !important;
    border-color: var(--btn-approve-border) !important;
    color: var(--btn-approve-text) !important;
}

.modal-actions .btn-warning {
    background: var(--btn-return-bg) !important;
    border-color: var(--btn-return-border) !important;
    color: var(--btn-return-text) !important;
}

.modal-actions .btn-danger {
    background: var(--btn-reject-bg) !important;
    border-color: var(--btn-reject-border) !important;
    color: var(--btn-reject-text) !important;
}

.modal-actions .btn-success:hover:not(:disabled),
.modal-actions .btn-success:active:not(:disabled) {
    background: var(--btn-approve-hover-bg) !important;
    border-color: var(--btn-approve-hover-bg) !important;
}

.modal-actions .btn-warning:hover:not(:disabled),
.modal-actions .btn-warning:active:not(:disabled) {
    background: var(--btn-return-hover-bg) !important;
    border-color: var(--btn-return-hover-bg) !important;
}

.modal-actions .btn-danger:hover:not(:disabled),
.modal-actions .btn-danger:active:not(:disabled) {
    background: var(--btn-reject-hover-bg) !important;
    border-color: var(--btn-reject-hover-bg) !important;
}

.modal-actions .btn:active:not(:disabled) {
    transform: translateY(1px);
}

@media (max-width: 520px) {
    .modal-actions {
        flex-direction: column-reverse;
    }

    .modal-actions .btn {
        width: 100%;
        min-width: 0;
    }
}

@media (prefers-color-scheme: dark) {
    .modal-actions .btn-secondary {
        background: #334155 !important;
        border-color: #475569 !important;
        color: #f1f5f9 !important;
    }

    .modal-actions .btn-secondary:hover:not(:disabled) {
        background: #475569 !important;
    }

    .modal-actions .btn-secondary:active:not(:disabled) {
        background: #64748b !important;
    }

    /* base.css forces an opaque dark background onto [class*="modal"]; restore the scrim. */
    .modal-overlay {
        background: rgba(15, 23, 42, .7) !important;
    }
}

.modal-fade-enter-active,
.modal-fade-leave-active {
    transition: .2s;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
    opacity: 0;
}

/* ---------------- Proposal details modal ---------------- */
/* Class names deliberately avoid "modal"/"card"/"panel"/"box"/"container"/"content" so
   the dark-mode surface rules in base.css do not repaint them with !important. */

.proposal-details-overlay {
    position: fixed;
    inset: 0;
    z-index: 1100; /* budget preview is 1200; the confirmation modal is 999 */
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: rgba(15, 23, 42, .55);
    backdrop-filter: blur(4px);
}

.proposal-details-surface {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 1200px;
    height: 90vh;
    height: 90dvh;
    max-height: calc(100dvh - 48px);
    overflow: hidden;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, .25);
}

.proposal-details-header {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #e2e8f0;
}

.proposal-details-heading {
    min-width: 0;
}

.proposal-details-heading h2 {
    margin: 0;
    font-size: 1.15rem;
    font-weight: 700;
    color: #0f172a;
}

.proposal-details-subtitle {
    margin: 2px 0 0;
    font-size: .8rem;
    color: #64748b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.proposal-details-close {
    flex: 0 0 auto;
    width: 38px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: none;
    border-radius: 8px;
    font-size: 22px;
    line-height: 1;
    color: #64748b;
    cursor: pointer;
    transition: background-color .15s ease, color .15s ease;
}

.proposal-details-close:hover {
    background: #f1f5f9;
    color: #334155;
}

.proposal-details-close:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

.proposal-details-body {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    background: #f8fafc;
}

@media (max-width: 768px) {
    .proposal-details-overlay {
        padding: 0;
    }

    .proposal-details-surface {
        max-width: none;
        height: 100vh;
        height: 100dvh;
        max-height: 100vh;
        max-height: 100dvh;
        border-radius: 0;
    }

    .proposal-details-header {
        padding: 14px 16px;
    }

    .proposal-details-subtitle {
        white-space: normal;
        overflow-wrap: anywhere;
    }
}

@media (prefers-color-scheme: dark) {
    .proposal-details-surface {
        background: #1e293b;
    }

    .proposal-details-header {
        background: #1e293b;
        border-bottom-color: #334155;
    }

    .proposal-details-heading h2 {
        color: #f8fafc !important;
    }

    .proposal-details-subtitle {
        color: #94a3b8 !important;
    }

    .proposal-details-close {
        color: #cbd5e1;
    }

    .proposal-details-close:hover {
        background: #334155;
        color: #f1f5f9;
    }

    .proposal-details-body {
        background: #0f172a;
    }
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

/* The native control is not painted; the whole row/option is the clickable control.
   It is pinned to the top-left of its positioned parent so that, when a label click
   focuses it, the browser has nothing to scroll into view. */
.sr-only {
    position: absolute;
    top: 0;
    left: 0;
    width: 1px;
    height: 1px;
    margin: -1px;
    padding: 0;
    border: 0;
    overflow: hidden;
    white-space: nowrap;
    clip: rect(0, 0, 0, 0);
    clip-path: inset(50%);
}
</style>