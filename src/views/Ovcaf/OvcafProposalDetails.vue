<template>
    <div class="ovcaf-shell" :class="{ 'is-embedded': embedded }">
        <!-- ── Main Area ── -->
        <div class="ovcaf-main">
            <div class="ovcaf-body">
                <div v-if="loading" class="card">
                    <p style="padding:2rem;text-align:center;color:var(--text-muted)">Loading proposal details...</p>
                </div>

                <div v-else-if="!proposal" class="card empty-state-box">
                    <p class="empty-title">Proposal Not Found</p>
                    <p class="empty-desc">The proposal you are trying to view does not exist or has been removed.</p>
                    <router-link v-if="!embedded" to="/ovcaf/inbox" class="btn btn-primary"
                        style="margin-top:1rem">Back to
                        Inbox</router-link>
                </div>

                <div v-else>
                    <!-- Action Bar -->
                    <div class="action-bar">
                        <router-link v-if="!embedded" to="/ovcaf/inbox" class="btn btn-secondary btn-sm">← Back
                            to Inbox</router-link>
                        <div class="action-bar-right">
                            <button type="button" class="btn btn-download"
                                :disabled="downloadLoading || !fullProposal" @click="downloadProposal">
                                {{ downloadLoading ? 'Preparing...' : 'Download Proposal Document' }}
                            </button>
                            <router-link v-if="isPending && !embedded" :to="`/ovcaf/validate/${proposal.id}`"
                                class="btn btn-primary">Proceed to Validation</router-link>
                        </div>
                    </div>
                    <p v-if="downloadError" class="download-error">{{ downloadError }}</p>

                    <!-- Header Card -->
                    <div class="card proposal-header-card">
                        <div class="header-layout">
                            <div class="title-section">
                                <div class="id-badge-row">
                                    <span class="proposal-id font-mono">{{ proposal.documentId || '—' }}</span>
                                    <span class="badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) }}</span>
                                </div>
                                <h2 class="proposal-title">{{ proposal.title }}</h2>
                                <div class="meta-row">
                                    <span><strong>{{ projectLeader }}</strong> (Project Leader)</span>
                                    <span>{{ proposal.college }}</span>
                                    <span>Received on {{ formatDateLong(proposal.dateReceived) }}</span>
                                </div>
                            </div>
                            <div class="budget-box">
                                <p class="budget-label">In-line Item Budget</p>
                                <template v-if="budgetFileName">
                                    <p class="budget-document-name">📄 {{ budgetFileName }}</p>
                                    <div class="budget-document-actions">
                                        <button type="button" class="btn btn-view btn-sm"
                                            @click="viewBudgetDocument">View</button>
                                        <button type="button" class="btn btn-download btn-sm"
                                            @click="saveBudgetDocument">Download</button>
                                    </div>
                                </template>
                                <!-- A stored reference that yielded no printable name is a
                                     retrieval problem, not a missing document. -->
                                <p v-else-if="hasBudgetReference" class="document-error">
                                    The Line-Item Budget document is attached, but its reference could not be
                                    resolved.
                                </p>
                                <!-- The saved record could not be read, so an empty state would be
                                     an unverified claim rather than a fact. -->
                                <p v-else-if="sectionsError" class="document-error">
                                    The Line-Item Budget document could not be retrieved.
                                </p>
                                <p v-else class="budget-document-empty">No Line-Item Budget uploaded</p>
                                <p v-if="documentError" class="document-error">{{ documentError }}</p>
                            </div>
                        </div>
                    </div>

                    <!-- Two-column layout -->
                    <div class="details-grid">
                        <!-- Left -->
                        <div class="col-left">
                            <!-- Complete read-only proposal information, rendered from the saved
                                 record through the same field registry the Create Proposal form and
                                 the reviewer screens use. Nothing here is editable by OVCAF. -->
                            <div class="card proposal-sections">
                                <h4 class="card-title">Complete Proposal Information</h4>
                                <p class="sections-note">
                                    Read-only view of the submitted proposal. Every section below is
                                    rendered from the saved record; attachments can be downloaded.
                                </p>
                                <p v-if="sectionsError" class="document-error">{{ sectionsError }}</p>
                                <p v-else-if="!fullProposal" class="sections-loading">Loading proposal
                                    sections...</p>
                                <template v-else>
                                    <ProposalFieldSection v-for="section in PROPOSAL_FIELD_SECTIONS"
                                        :key="section.key" :section="section" :proposal="fullProposal"
                                        :proposal-id="proposal.id" :editable="false"
                                        @download="downloadAttachment" />
                                </template>
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
            </div>
        </div>

        <!-- Line-Item Budget preview: opens over this page, never navigates away. -->
        <LineItemBudgetPreviewModal v-if="showBudgetPreview" :file-path="budgetDocumentPath(budgetSource)"
            :file-name="budgetFileName" @close="showBudgetPreview = false" />
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import api from '@/utils/api';
import ProposalFieldSection from '@/components/ProposalFieldSection.vue';
import LineItemBudgetPreviewModal from '@/components/LineItemBudgetPreviewModal.vue';
import { PROPOSAL_FIELD_SECTIONS } from '@/utils/proposalFields';
import { budgetDocumentName, budgetDocumentPath } from '@/utils/budgetDocument';
import { downloadUploadedFile } from '@/utils/fileDownload';
import { downloadOfficialProposal } from '@/utils/officialProposalForm';
import { statusTone } from '@/utils/statusTone.js';
import { proposalStatusLabel } from '@/utils/proposalStatus.js';
import { normalizeStatusKey } from '@/utils/statusLabel.js';

const route = useRoute();

// The same read-only viewer is rendered both as the standalone /ovcaf/proposals/:id
// page and inside the evaluation screen's details modal. When embedded, the caller
// supplies the proposal id and the page-level navigation actions are suppressed.
const props = defineProps({
    proposalId: { type: [String, Number], default: null },
    embedded: { type: Boolean, default: false }
});
const emit = defineEmits(['preview-change']);

const proposalKey = computed(() => {
    const fromProp = props.proposalId;
    if (fromProp !== null && fromProp !== undefined && String(fromProp).trim() !== '') {
        return String(fromProp);
    }
    return String(route.params.id ?? '');
});

// ── Shell State ───────────────────────────────────
const sidebarOpen = ref(false);
const dropdownOpen = ref(false);
const dropdownRef = ref(null);

// ── Page State ────────────────────────────────────
const loading = ref(true);
const error = ref(null);
const proposal = ref(null);
// The full saved proposal (GET /api/proposals/{id}), used to render every section from the
// same field registry as the proposal form. Loaded only after the OVCAF detail route has
// authorized the record, so a draft is never fetched or rendered here.
const fullProposal = ref(null);
const sectionsError = ref('');
const documentError = ref('');
const downloadLoading = ref(false);
const downloadError = ref('');
// View opens an in-page preview of the uploaded document; reset whenever the page
// loads another proposal so the modal never shows the previous record.
const showBudgetPreview = ref(false);

// Let the host of the embedded viewer know when the nested budget preview is open, so
// Escape closes only the preview instead of both layers at once.
watch(showBudgetPreview, (open) => emit('preview-change', open));

// ── Computed ──────────────────────────────────────
const isPending = computed(() =>
    normalizeStatusKey(proposal.value?.status) === 'FOR_OVCAF_APPROVAL'
);
// The header follows the exact saved record the actual proposal viewer renders
// (GET /api/proposals/{id}); the OVCAF response is only the fallback while it loads.
const budgetSource = computed(() => fullProposal.value || proposal.value);
const projectLeader = computed(() => {
    const fromForm = fullProposal.value
        ? String(fullProposal.value.projectLeader || '').trim()
        : '';
    return fromForm || proposal.value?.projectLeader || 'Unknown';
});
const budgetFileName = computed(() => budgetDocumentName(budgetSource.value));
const hasBudgetReference = computed(() => Boolean(budgetDocumentPath(budgetSource.value)));

const checklistLabels = {
    budgetComplete: 'Line-Item Budget is complete and matches all pages',
    budgetComputationCorrect: 'Budget computations are mathematically correct',
    supportingDocsComplete: 'All supporting financial files are attached and verified',
    expensesReasonable: 'Proposed expenses are reasonable and justified',
    fundingSourceIdentified: 'Funding source is officially identified and compliant',
    complianceVerified: 'Compliance with standard government/university funding guidelines verified'
};

// ── Fetch proposal from API ───────────────────────
const fetchProposal = async () => {
    try {
        loading.value = true;
        error.value = null;
        sectionsError.value = '';
        documentError.value = '';
        downloadError.value = '';
        fullProposal.value = null;
        showBudgetPreview.value = false;
        const id = proposalKey.value;
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
                // Section 25 is an uploaded document, not a recomputable amount table.
                beneficiariesFileName: p.beneficiariesFileName || '',
                beneficiariesFilePath: p.beneficiariesFilePath || '',
                dateReceived: p.createdAt,
                status: p.status,
                ocRemarks: p.remarks || null,
                validationChecklist: {
                    budgetComplete: p.budgetComplete || false,
                    budgetComputationCorrect: p.budgetComputationCorrect || false,
                    supportingDocsComplete: p.supportingDocsComplete || false,
                    expensesReasonable: p.expensesReasonable || false,
                    fundingSourceIdentified: p.fundingSourceIdentified || false,
                    complianceVerified: p.complianceVerified || false
                },
                remarks: p.ovcafRemarks || null,
                history: buildHistory(p)
            };
            await loadFullProposal(id);
        }
    } catch (err) {
        console.error('Error fetching proposal details:', err);
        error.value = 'Failed to load proposal details. Please try again.';
        proposal.value = null;
    } finally {
        loading.value = false;
    }
};

/**
 * Load the complete saved proposal for the read-only field sections. The OVCAF detail
 * response above already authorized the record, so this never renders a proposal OVCAF
 * cannot review; a failure here degrades to a message instead of an empty page.
 */
const loadFullProposal = async (id) => {
    try {
        const res = await api.get(`/api/proposals/${id}`);
        fullProposal.value = res.data || null;
        sectionsError.value = fullProposal.value ? '' : 'The proposal details could not be loaded.';
    } catch (err) {
        console.error('Error fetching complete proposal:', err);
        fullProposal.value = null;
        sectionsError.value = 'The complete proposal information could not be loaded. Please try again.';
    }
};

// Build history from proposal data
const buildHistory = (p) => {
    const history = [];
    if (p.createdAt) {
        history.push({
            stage: 'Submission',
            action: 'Proposal Submitted',
            user: p.projectLeader || p.proponentName || 'Project Leader',
            date: new Date(p.createdAt).toLocaleString('en-PH'),
            notes: null
        });
    }
    if (p.status === 'FOR_OVCAF_APPROVAL' || p.status === 'APPROVED' || p.status === 'FOR_OC_APPROVAL') {
        history.push({
            stage: 'OVCAF',
            action: 'Received for Financial Validation',
            user: 'System',
            date: new Date(p.updatedAt || p.createdAt).toLocaleString('en-PH'),
            notes: null
        });
    }
    if (p.ovcafDecision) {
        history.push({
            stage: 'OVCAF',
            action: p.ovcafDecision,
            user: 'OVCAF Evaluator',
            date: new Date(p.updatedAt).toLocaleString('en-PH'),
            notes: p.ovcafRemarks || null
        });
    }
    return history;
};

// ── Lifecycle ─────────────────────────────────────
onMounted(async () => {
    await fetchProposal();
    document.addEventListener('click', handleOutsideClick);
});
onUnmounted(() => document.removeEventListener('click', handleOutsideClick));

// Vue Router reuses this component when only the id changes, so the proposal key is
// watched: opening another proposal, Back/Forward between two, or the embedding host
// passing a different id reloads the right record.
watch(proposalKey, (id, previous) => {
    if (id && id !== previous) fetchProposal();
});

const handleOutsideClick = (e) => {
    if (dropdownRef.value && !dropdownRef.value.contains(e.target)) dropdownOpen.value = false;
};

// ── Document actions ──────────────────────────────
// The Line-Item Budget is the uploaded section 25 document; both actions go through the
// authenticated /api/files route, which already enforces who may read the stored file.
// View opens the preview modal over this page instead of navigating or opening a tab.
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

/** Download one attachment (supporting documents, maps, workplans) from a section card. */
const downloadAttachment = async (target) => {
    if (!target) return;
    documentError.value = '';
    try {
        await downloadUploadedFile(target);
    } catch (err) {
        console.error('Error downloading attachment:', err);
        documentError.value = 'The attached document could not be downloaded. Please try again.';
    }
};

/** Build the complete official proposal document from the saved record. */
const downloadProposal = async () => {
    if (!fullProposal.value) {
        downloadError.value = 'The complete proposal is still loading. Please try again.';
        return;
    }
    downloadLoading.value = true;
    downloadError.value = '';
    try {
        await downloadOfficialProposal(fullProposal.value);
    } catch (err) {
        console.error('Error preparing the proposal document:', err);
        downloadError.value = 'Could not prepare the proposal document. Please try again.';
    } finally {
        downloadLoading.value = false;
    }
};

// ── Helpers ───────────────────────────────────────
const formatDateLong = (d) => d ? new Date(d).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) : '';
const getTlClass = (action) => {
    const a = (action || '').toLowerCase();
    if (a.includes('approve') || a.includes('endorse') || a.includes('submit')) return 'tl-success';
    if (a.includes('return')) return 'tl-warning';
    if (a.includes('reject')) return 'tl-danger';
    return 'tl-info';
};
</script>

<style scoped>
.ovcaf-main {
    width: 100%;
    max-width: 1600px;
    margin: 0 auto;
}

.ovcaf-body {
    padding: 32px;
}

/* Embedded viewer (inside the evaluation screen's details modal): the host provides the
   chrome, so the page keeps only its content and the Download Proposal Document action. */
.ovcaf-shell.is-embedded .ovcaf-main {
    max-width: none;
}

.ovcaf-shell.is-embedded .action-bar {
    justify-content: flex-end;
}

.ovcaf-shell.is-embedded .ovcaf-body {
    padding: 20px 24px 32px;
}

@media (max-width: 640px) {
    .ovcaf-shell.is-embedded .ovcaf-body {
        padding: 16px;
    }
}

.action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
}

.action-bar .btn {
    width: auto;
    flex: none;
    display: inline-flex;
    align-items: center;
    justify-content: center;

    padding: 6px 14px;
    height: 34px;

    font-size: .78rem;
    border-radius: 8px;
}

.action-bar .btn-primary:hover {
    transform: translateY(-1px);
}


.card {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, .05);
}

.custom-table {
    width: 100%;
    border-collapse: collapse;
}

.custom-table th {
    padding: 14px 18px;
}

.custom-table td {
    padding: 16px 18px;
}

.proposal-header-card {
    padding: 28px;
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

.budget-box {
    padding: 24px;
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
    min-width: 240px;
    background-color: #f8fafc;
    padding: 1rem;
    border-radius: var(--border-radius-sm);
    border: 1px solid #e2e8f0;
    text-align: left;
}

.budget-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--text-muted);
    margin-bottom: 0.4rem;
}

.budget-document-name {
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--text-primary);
    overflow-wrap: anywhere;
}

.budget-document-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-top: 0.6rem;
}

.budget-document-actions .btn {
    min-width: 96px;
    justify-content: center;
}

@media (max-width: 520px) {
    .budget-document-actions {
        width: 100%;
    }

    .budget-document-actions .btn {
        flex: 1 1 auto;
    }
}

.budget-document-empty {
    font-size: 0.85rem;
    font-style: italic;
    color: var(--text-muted);
}

.document-error {
    margin-top: 0.5rem;
    font-size: 0.8rem;
    color: #dc2626;
}

.download-error {
    margin: -1rem 0 1rem;
    font-size: 0.85rem;
    color: #dc2626;
}

.action-bar-right {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
}

.proposal-sections {
    padding: 24px;
}

.sections-note {
    margin-bottom: 1.25rem;
    font-size: 0.85rem;
    color: var(--text-muted);
}

.sections-loading {
    font-size: 0.9rem;
    color: var(--text-muted);
}

.details-grid {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
    gap: 24px;
    align-items: start;
}

.col-left,
.col-right {
    min-width: 0;
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

.document-item {
    padding: 16px 18px;
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

.timeline-item {
    padding-bottom: 28px;
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

.tl-icon {
    width: 44px;
    height: 44px;
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

.tl-content {
    padding: 18px;
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
