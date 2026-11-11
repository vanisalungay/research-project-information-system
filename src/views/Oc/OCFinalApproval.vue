<template>
  <div class="page">
    <div class="back-link">
      <router-link to="/oc-dashboard" class="back-btn btn-secondary">
        <span class="back-arrow">&#8592;</span> Back to Dashboard
      </router-link>
      <button v-if="proposal.id" class="btn-download" :disabled="downloadLoading" @click="downloadDetails">
        {{ downloadLoading ? 'Preparing…' : 'Download Proposal Details' }}
      </button>
    </div>
    <p v-if="downloadError" class="error-text download-error" role="alert">{{ downloadError }}</p>

    <!-- Loading -->
    <div v-if="loading" class="card center">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card center">
      <p class="error-text">{{ error }}</p>
      <button class="btn-secondary" @click="$router.push('/oc-dashboard')" style="margin-top:12px;">Back to
        Dashboard</button>
    </div>

    <!-- Proposal Content -->
    <div v-else class="layout">
      <!-- LEFT COLUMN -->
      <div class="left-col">
        <div class="card proposal-card">
          <div class="proposal-header">
            <h1>{{ proposal.projectTitle || 'Untitled' }}</h1>
            <span class="status-badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) || 'N/A' }}</span>
          </div>
          <div class="proposal-meta">
            <div class="meta-item">
              <span class="meta-label">Proponent</span>
              <p class="meta-value">{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">Program</span>
              <p class="meta-value">{{ proposal.programTitle || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">Duration</span>
              <p class="meta-value">{{ proposal.duration || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">College</span>
              <p class="meta-value">{{ proposal.college || 'N/A' }}</p>
            </div>
          </div>
        </div>

        <div class="card">
          <h3>Executive Summary</h3>
          <p class="summary-text">{{ proposal.executiveSummary || 'No executive summary provided.' }}</p>
        </div>

        <!-- Field-level review: a remark can be left beside any proposal field. -->
        <div class="card">
          <h3>Proposal Field Review</h3>
          <p class="field-note">
            Leave a remark beside any field that needs correction. The proponent sees each remark
            beside that exact field on the revision page.
          </p>
          <p v-if="remarksError" class="error-text">{{ remarksError }}</p>
          <ProposalFieldSection
            v-for="section in PROPOSAL_FIELD_SECTIONS"
            :key="section.key"
            :section="section"
            :proposal="proposal"
            :proposal-id="proposal.id"
            :remark-index="remarkIndex"
            :editable="true"
            @remark-saved="onRemarkSaved"
            @remark-removed="onRemarkRemoved"
          />
        </div>

        <div class="card">
          <h3>Chancellor's Notes</h3>
          <textarea v-model="chancellorNotes" placeholder="Add your notes and comments..."></textarea>
        </div>
      </div>

      <!-- RIGHT COLUMN -->
      <div class="right-col">
        <!-- Status Timeline -->
        <div class="card">
          <h3>Approval Trail</h3>
          <div class="timeline">
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">RPS / RII DARES</p>
                <p class="tl-desc">Endorsed</p>
                <small>{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">REC Evaluation</p>
                <p class="tl-desc">Completed</p>
                <small>{{ proposal.updatedAt?.substring(0, 10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">OVCRIGE</p>
                <p class="tl-desc">Forwarded to OC</p>
                <small>{{ proposal.updatedAt?.substring(0, 10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item current">
              <div class="dot pulse"></div>
              <div>
                <p class="tl-title">Office of the Chancellor</p>
                <p class="tl-desc">Pending Final Decision</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Budget Determination -->
        <div class="card">
          <h3>Budget Requirement</h3>
          <p class="budget-desc">Determine whether this project requires budget funding.</p>
          <!-- ONE classification per proposal: both options belong to the same named radio
               group, so the browser itself clears the other option when one is chosen. The
               model holds exactly one boolean (true = with budget, false = without budget);
               it can never hold both. -->
          <div class="budget-options" role="radiogroup" aria-label="Budget classification">
            <label class="budget-option" :class="{ selected: needsBudget === true, disabled: !atOc || busy }">
              <span class="budget-radio">
                <input type="radio" name="budget-classification" v-model="needsBudget" :value="true"
                  :disabled="!atOc || busy" @change="chooseBudget(true)" />
              </span>
              <span class="budget-content">
                <span class="budget-title">With Budget</span>
                <span class="budget-description">Forward to Finance Office / OVCAF for budget endorsement and
                  release</span>
              </span>
              <span v-if="needsBudget === true" class="budget-selected-tag">Selected</span>
            </label>
            <label class="budget-option" :class="{ selected: needsBudget === false, disabled: !atOc || busy }">
              <span class="budget-radio">
                <input type="radio" name="budget-classification" v-model="needsBudget" :value="false"
                  :disabled="!atOc || busy" @change="chooseBudget(false)" />
              </span>
              <span class="budget-content">
                <span class="budget-title">Without Budget</span>
                <span class="budget-description">Proponent proceeds directly with project implementation</span>
              </span>
              <span v-if="needsBudget === false" class="budget-selected-tag">Selected</span>
            </label>
          </div>
        </div>

        <!-- Action Buttons. Only a proposal still at FOR_OC_APPROVAL can be acted on; for any
             other record the controls are replaced by the recorded decision instead of showing
             buttons that can only do nothing. -->
        <template v-if="atOc">
          <div class="action-buttons">
            <button type="button" class="btn-approve" :class="{ disabled: needsBudget === null }"
              :disabled="busy || needsBudget === null" @click="openApproval">
              {{ actionLoading ? 'Processing...' : approvalActionLabel }}
            </button>
            <button type="button" class="btn-return" :disabled="busy" @click="openReturn">
              {{ actionLoading ? 'Processing...' : 'Return to OVCRIGE' }}
            </button>
          </div>
          <p v-if="needsBudget === null" class="hint">
            Select exactly one classification (With Budget or Without Budget) to enable approval.
          </p>
        </template>
        <p v-else class="hint decision-locked">
          This proposal is <strong>{{ proposalStatusLabel(proposal.status) || proposal.status }}</strong> and is
          no longer awaiting the Chancellor. The recorded budget classification is shown above; approval and
          return are not available.
        </p>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirm" class="modal-overlay" @click.self="closeConfirm">
      <div class="modal so-modal">
        <div class="modal-header">
          <span>Confirm Approval & Issue Special Order</span>
          <span class="close" @click="closeConfirm">&times;</span>
        </div>
        <div class="modal-body">
          <div class="check-icon">&#10003;</div>
          <h4>Approve This Proposal?</h4>

          <!-- Special Order Fields -->
          <div class="so-section">
            <div class="so-field">
              <label class="so-label">Special Order (SO) Number <span class="required">*</span></label>
              <input type="text" v-model="soNumber" class="so-input" placeholder="e.g., SO-2026-001" />
            </div>
            <div class="so-field">
              <label class="so-label">SO Document (PDF/DOC)</label>
              <div class="file-upload-wrapper">
                <input type="file" ref="soFileInput" class="so-file-input" accept=".pdf,.doc,.docx"
                  @change="handleSoFileChange" />
                <div class="file-display" v-if="soFile">
                  <span class="file-icon">📄</span>
                  <span class="file-name">{{ soFile.name }}</span>
                  <button type="button" class="file-remove" @click="removeSoFile">&times;</button>
                </div>
                <!-- A document saved by an earlier draft is kept on the proposal until
                     it is replaced or explicitly removed. -->
                <div class="file-display saved-file" v-else-if="savedSoFileName && !removeSavedSoFile">
                  <span class="file-icon">📄</span>
                  <span class="file-name">{{ savedSoFileName }}</span>
                  <span class="saved-tag">Saved draft</span>
                  <button type="button" class="file-link" @click="downloadSavedSoFile">Download</button>
                  <button type="button" class="file-remove" title="Remove the saved SO document"
                    @click="markSavedSoFileRemoved">&times;</button>
                </div>
                <p v-else-if="removeSavedSoFile" class="saved-file-note">
                  The saved SO document will be removed when you save.
                </p>
              </div>
            </div>
          </div>

          <!-- Budget Routing Info: the destination is stated before anything is sent. -->
          <div class="routing-info" :class="needsBudget ? 'with-budget' : 'no-budget'">
            <p v-if="needsBudget">
              <strong>With Budget:</strong> This proposal will be forwarded to the Finance Office / OVCAF for budget
              endorsement and release. Its status becomes <strong>FOR_OVCAF_APPROVAL</strong>.
            </p>
            <p v-else>
              <strong>Without Budget:</strong> This proposal will be approved for immediate implementation. Its status
              becomes <strong>APPROVED</strong> and it skips the OVCAF step.
            </p>
          </div>

          <!-- The real reason a failed approval did not go through. Nothing was saved
               on the server, so the form (including the SO number and file) stays open
               and can be retried. -->
          <p v-if="approvalError" class="modal-error" role="alert">{{ approvalError }}</p>
          <p v-if="draftError" class="modal-error" role="alert">{{ draftError }}</p>
          <p v-if="draftMessage" class="draft-success" role="status">{{ draftMessage }}</p>
          <p v-if="isSoDraft(proposal) && !draftMessage" class="draft-saved-note">
            This SO form is saved as a draft and has not been finalized.
          </p>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-cancel" :disabled="busy" @click="closeConfirm">Cancel</button>
          <button type="button" class="btn-secondary" :disabled="busy || !atOc" @click="saveDraft">
            {{ draftSaving ? 'Saving Draft...' : 'Save Draft' }}
          </button>
          <button type="button" class="btn-confirm btn-approve"
            :disabled="!soNumber.trim() || busy || !atOc" @click="confirmApproval">
            {{ soUploading ? 'Uploading & Approving...' : 'Confirm Approval' }}
          </button>
        </div>
        <p v-if="!soNumber.trim()" class="modal-hint">
          Please enter the Special Order number to proceed with the final approval.
        </p>
        <p v-else-if="!atOc" class="modal-hint">
          This proposal is no longer awaiting the Chancellor's final approval.
        </p>
      </div>
    </div>

    <!-- RETURN CONFIRMATION MODAL -->
    <div v-if="showReturnConfirm" class="modal-overlay" @click.self="closeReturn">
      <div class="modal">
        <div class="modal-header">
          <span>Return Proposal to OVCRIGE</span>
          <span class="close" @click="closeReturn">&times;</span>
        </div>
        <div class="modal-body">
          <div class="check-icon return-icon">&#8617;</div>
          <h4>Return This Proposal?</h4>
          <p class="return-desc">
            The proposal goes back to the OVCRIGE queue (<strong>REC_APPROVED</strong>) so it can be
            re-forwarded to the Office of the Chancellor. No Special Order is issued and no approval is
            recorded.
          </p>
          <p v-if="chancellorNotes.trim()" class="return-notes">
            <strong>Remarks:</strong> {{ chancellorNotes.trim() }}
          </p>
          <!-- A failed return is a real failure: it is reported here and never shown as success. -->
          <p v-if="returnError" class="modal-error" role="alert">{{ returnError }}</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-cancel" :disabled="busy" @click="closeReturn">Cancel</button>
          <button type="button" class="btn-confirm btn-return" :disabled="busy" @click="returnToOvcrige">
            {{ actionLoading ? 'Returning...' : 'Return to OVCRIGE' }}
          </button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay" @click.self="handleSuccessClose">
      <div class="modal success-modal">
        <div class="modal-body">
          <div class="check-icon success">&#10003;</div>
          <p>{{ successMessage }}</p>
        </div>
        <div class="modal-actions center">
          <button class="btn-confirm btn-primary" @click="handleSuccessClose">OK</button>
        </div>
      </div>
    </div>

    <!-- UNSAVED CHANGES: closing the SO form would lose the entered values. -->
    <ConfirmDialog v-if="showDiscardConfirm" type="warning" variant="confirm" title="Discard unsaved changes?"
      message="The SO details you entered have not been saved to a draft. Closing now will discard them."
      confirm-text="Discard" cancel-text="Keep editing" @confirm="confirmDiscard"
      @cancel="showDiscardConfirm = false" @close="showDiscardConfirm = false" />
  </div>
</template>

<script>
import api from '@/utils/api'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { PROPOSAL_FIELD_SECTIONS, buildFieldRemarkIndex } from '@/utils/proposalFields'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'
import { downloadUploadedFile } from '@/utils/fileDownload'
import { downloadProposalRecord } from '@/utils/proposalDetailsDocument'
import { isSoDraft, soDraftFileLabel, draftSnapshot } from '@/utils/soDraft'

// Mirrors the server's Special Order rules (rpis.special-order.max-size-mb in the
// backend configuration). The server remains authoritative; checking here as well
// reports an invalid file before a request is sent.
const SO_MAX_BYTES = 25 * 1024 * 1024
const SO_EXTENSIONS = ['pdf', 'doc', 'docx']

// An in-progress budget classification, remembered per proposal and per browser tab so a page
// refresh does not lose the Chancellor's selection. It is only a convenience: the classification
// becomes part of the record when the approval (or a draft) is saved, and the saved value always
// wins when the page is reopened. The value is a single boolean - "with" and "without" can never
// both be remembered.
const BUDGET_CHOICE_PREFIX = 'rpis.oc.budget-classification.'

function readRememberedBudget(proposalId) {
  if (!proposalId) return null
  try {
    const raw = sessionStorage.getItem(BUDGET_CHOICE_PREFIX + proposalId)
    if (raw === 'true') return true
    if (raw === 'false') return false
  } catch (e) {
    /* storage can be unavailable (private mode); the selection simply is not remembered */
  }
  return null
}

function rememberBudget(proposalId, value) {
  if (!proposalId) return
  try {
    if (value === true || value === false) {
      sessionStorage.setItem(BUDGET_CHOICE_PREFIX + proposalId, String(value))
    } else {
      sessionStorage.removeItem(BUDGET_CHOICE_PREFIX + proposalId)
    }
  } catch (e) {
    /* see above */
  }
}

export default {
  name: 'FinalApproval',
  components: { ProposalFieldSection, ConfirmDialog },
  data() {
    return {
      proposal: {},
      loading: true,
      error: null,
      showConfirm: false,
      showReturnConfirm: false,
      showSuccess: false,
      successMessage: '',
      actionLoading: false,
      chancellorNotes: '',
      needsBudget: null,
      soNumber: '',
      soFile: null,
      soUploading: false,
      approvalError: '',
      returnError: '',
      // SO draft state
      draftSaving: false,
      draftError: '',
      draftMessage: '',
      removeSavedSoFile: false,
      showDiscardConfirm: false,
      formSnapshot: '',
      // Proposal details document
      downloadLoading: false,
      downloadError: '',
      // Field-level remarks of the active revision round.
      PROPOSAL_FIELD_SECTIONS,
      remarksResponse: null,
      remarksError: '',
    }
  },
  computed: {
    remarkIndex() {
      return buildFieldRemarkIndex(this.remarksResponse)
    },
    /** True only while the proposal is still waiting for the Chancellor. */
    atOc() {
      return String(this.proposal?.status ?? '').trim().toUpperCase() === 'FOR_OC_APPROVAL'
    },
    /** The SO document saved by an earlier draft (empty when there is none). */
    savedSoFileName() {
      return isSoDraft(this.proposal) ? soDraftFileLabel(this.proposal) : ''
    },
    /** The file the form currently represents, saved or newly picked. */
    currentSoFileName() {
      if (this.soFile) return this.soFile.name
      if (this.removeSavedSoFile) return ''
      return this.savedSoFileName
    },
    formSnapshotValue() {
      return draftSnapshot({
        soNumber: this.soNumber,
        needsBudget: this.needsBudget,
        chancellorNotes: this.chancellorNotes,
        fileName: this.currentSoFileName,
      })
    },
    formDirty() {
      return this.formSnapshot !== '' && this.formSnapshotValue !== this.formSnapshot
    },
    busy() {
      return this.draftSaving || this.soUploading || this.actionLoading
    },
    /**
     * The approval is one workflow action with two destinations. The label names the
     * destination of the selected classification so "Approve" never claims the proposal
     * is implemented when it is actually being forwarded to OVCAF for funding.
     */
    approvalActionLabel() {
      return this.needsBudget === true ? 'Approve & Forward to OVCAF' : 'Approve for Implementation'
    },
    /** Mirrors the server's draft rule: an SO number or an SO document is required. */
    canSaveDraft() {
      return Boolean(
        String(this.soNumber || '').trim() ||
          this.soFile ||
          (this.savedSoFileName && !this.removeSavedSoFile),
      )
    },
  },
  async mounted() {
    await this.loadProposal()
    await this.loadFieldRemarks()
  },
  methods: {
    statusTone,
    proposalStatusLabel,
    // Exposed as a method, never as a computed: the template calls it with a
    // proposal, and a computed would expose its (boolean) result instead of the
    // function - which crashed the confirmation dialog when the Approve button
    // opened it.
    isSoDraft,
    async loadFieldRemarks() {
      if (!this.proposal.id) return
      try {
        const res = await api.get(`/api/proposals/${this.proposal.id}/field-remarks`)
        this.remarksResponse = res.data || null
        this.remarksError = ''
      } catch (err) {
        console.error(err)
        this.remarksResponse = null
        this.remarksError = 'Unable to load the existing remarks.'
      }
    },
    onRemarkSaved(item) {
      const current = { ...(this.remarksResponse || { activeRound: 1, rounds: [] }) }
      const rounds = Array.isArray(current.rounds) ? [...current.rounds] : []
      let activeIndex = rounds.findIndex((round) => round.active)
      if (activeIndex < 0) {
        rounds.unshift({ round: current.activeRound || 1, active: true, remarks: [] })
        activeIndex = 0
        current.activeRound = rounds[0].round
      }
      const remarks = [...(rounds[activeIndex].remarks || [])]
      const existing = remarks.findIndex((row) => row.id === item.id || row.fieldKey === item.fieldKey)
      if (existing >= 0) remarks[existing] = item
      else remarks.push(item)
      rounds[activeIndex] = { ...rounds[activeIndex], remarks }
      this.remarksResponse = { ...current, rounds }
    },
    onRemarkRemoved(remarkId) {
      if (!this.remarksResponse) return
      const rounds = (this.remarksResponse.rounds || []).map((round) => ({
        ...round,
        remarks: (round.remarks || []).filter((row) => row.id !== remarkId),
      }))
      this.remarksResponse = { ...this.remarksResponse, rounds }
    },
    async loadProposal() {
      this.loading = true
      this.error = null
      try {
        const proposalId = this.$route.params.id
        if (!proposalId) {
          this.error = 'No proposal ID provided.'
          this.loading = false
          return
        }
        const res = await api.get(`/api/proposals/${proposalId}`)
        this.proposal = res.data || {}
        this.applyProposalPrefill()
      } catch (err) {
        console.error('Failed to load proposal:', err)
        this.error = 'Failed to load proposal details. Check that the backend is running.'
      } finally {
        this.loading = false
      }
    },
    /**
     * Restores the SO form from the proposal. Only a saved draft (or a proposal
     * being reopened) owns `remarks`: on an untouched proposal that field may hold
     * the REC reviewer's comment and must never appear as the Chancellor's notes.
     *
     * The budget classification is a stored property of the record (needs_budget),
     * so a saved value is always restored - a reopened proposal and a refresh show
     * the same single selection. Only when the record has no saved classification
     * does the page fall back to the choice remembered for this tab.
     */
    applyProposalPrefill() {
      const draft = isSoDraft(this.proposal)
      this.soNumber = draft ? this.proposal.soNumber || '' : ''
      const storedClassification = this.proposal.needsBudget
      this.needsBudget =
        storedClassification === true || storedClassification === false
          ? storedClassification
          : this.atOc
            ? readRememberedBudget(this.proposal.id)
            : null
      this.chancellorNotes = draft ? this.proposal.remarks || '' : ''
      this.soFile = null
      this.removeSavedSoFile = false
      this.draftError = ''
      this.draftMessage = ''
      this.approvalError = ''
      if (this.$refs.soFileInput) {
        this.$refs.soFileInput.value = ''
      }
      this.formSnapshot = this.formSnapshotValue
    },
    handleSoFileChange(event) {
      const file = event.target.files[0]
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
      // A newly picked file replaces the stored draft document.
      this.removeSavedSoFile = false
      if (!file) {
        this.soFile = null
        return
      }
      const extension = file.name.includes('.')
        ? file.name.split('.').pop().toLowerCase()
        : ''
      if (!SO_EXTENSIONS.includes(extension)) {
        this.soFile = null
        event.target.value = ''
        this.approvalError = 'Invalid file format. Please upload the Special Order as a PDF, DOC or DOCX file.'
        return
      }
      if (file.size > SO_MAX_BYTES) {
        this.soFile = null
        event.target.value = ''
        this.approvalError = 'The Special Order document must be 25 MB or smaller.'
        return
      }
      this.soFile = file
    },
    removeSoFile() {
      this.soFile = null
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
      if (this.$refs.soFileInput) {
        this.$refs.soFileInput.value = ''
      }
    },
    markSavedSoFileRemoved() {
      this.removeSavedSoFile = true
      this.draftError = ''
      this.draftMessage = ''
    },
    async downloadSavedSoFile() {
      try {
        await downloadUploadedFile(this.proposal.soFilePath, this.proposal.soFileName)
      } catch (err) {
        console.error('Failed to download the saved SO document:', err)
        this.draftError = 'The saved SO document could not be downloaded.'
      }
    },
    closeConfirm() {
      // Never close the form while a request is in flight, so a double click or an
      // overlay click cannot hide (or repeat) a submission that is already running.
      if (this.busy) return
      // Unsaved entries would be lost: ask before closing.
      if (this.formDirty) {
        this.showDiscardConfirm = true
        return
      }
      this.hideConfirm()
    },
    hideConfirm() {
      this.showConfirm = false
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
    },
    confirmDiscard() {
      this.showDiscardConfirm = false
      // Restore the values that are actually saved on the proposal.
      this.applyProposalPrefill()
      this.hideConfirm()
    },
    async saveDraft() {
      if (this.busy) return
      this.draftError = ''
      this.draftMessage = ''
      if (!this.atOc) {
        this.draftError = 'Only a proposal awaiting final approval can save an SO draft.'
        return
      }
      if (!this.canSaveDraft) {
        this.draftError = 'Enter the SO number or attach the SO document before saving a draft.'
        return
      }

      this.draftSaving = true
      try {
        const proposalId = this.proposal.id || this.$route.params.id
        const formData = new FormData()
        formData.append('soNumber', String(this.soNumber || '').trim())
        if (this.needsBudget === true || this.needsBudget === false) {
          formData.append('needsBudget', String(this.needsBudget))
        }
        if (this.chancellorNotes && this.chancellorNotes.trim()) {
          formData.append('chancellorNotes', this.chancellorNotes.trim())
        }
        if (this.soFile) {
          formData.append('soFile', this.soFile)
        }
        if (this.removeSavedSoFile) {
          formData.append('removeSoFile', 'true')
        }

        const res = await api.put(`/api/proposals/${proposalId}/special-order-draft`, formData)
        const saved = res.data?.proposal
        if (saved) {
          // Merge only the SO fields: the page's other data (reviews, remarks) stays.
          this.proposal = {
            ...this.proposal,
            status: saved.status,
            soNumber: saved.soNumber,
            soFileName: saved.soFileName,
            soFilePath: saved.soFilePath,
            soUploadedAt: saved.soUploadedAt,
            needsBudget: saved.needsBudget,
            remarks: saved.remarks,
          }
          // The draft now stores the classification, so the tab copy follows the server.
          rememberBudget(
            proposalId,
            saved.needsBudget === true || saved.needsBudget === false ? saved.needsBudget : null,
          )
        }
        this.soFile = null
        this.removeSavedSoFile = false
        if (this.$refs.soFileInput) {
          this.$refs.soFileInput.value = ''
        }
        this.formSnapshot = this.formSnapshotValue
        this.draftMessage = res.data?.message || 'SO draft saved.'
      } catch (err) {
        console.error('Saving the SO draft failed:', err)
        const status = err.response?.status
        let errorMsg = err.response?.data?.error || err.response?.data?.message
        if (!errorMsg && status === 413) {
          errorMsg = 'The Special Order document is too large for the server. The maximum accepted size is 25 MB.'
        }
        if (!errorMsg) {
          errorMsg = err.message || 'Unknown error occurred.'
        }
        // Everything typed stays in place so the draft can be corrected and retried.
        this.draftError = errorMsg
      } finally {
        this.draftSaving = false
      }
    },
    async downloadDetails() {
      if (this.downloadLoading) return
      const proposalId = this.proposal.id || this.$route.params.id
      if (!proposalId) return

      this.downloadError = ''
      this.downloadLoading = true
      try {
        // The shared role-neutral document: re-reads the proposal and its reviews by id
        // so the printed record always matches what the server holds now.
        await downloadProposalRecord({
          proposalId,
          proposal: this.proposal,
          remarks: this.remarksResponse,
        })
      } catch (err) {
        console.error('Preparing the proposal details document failed:', err)
        this.downloadError = 'The proposal details could not be prepared. Please try again.'
      } finally {
        this.downloadLoading = false
      }
    },
    /**
     * Selecting a classification is exclusive by construction: the two radios form one
     * native group, and the model holds a single boolean. This handler only records the
     * choice (so it survives a refresh) - it never sends a request and never opens the
     * confirmation, which belongs to the explicit Approve action.
     */
    chooseBudget(value) {
      if (this.busy || !this.atOc) return
      // Only a real boolean can be selected; anything else clears the selection rather
      // than storing an ambiguous value.
      this.needsBudget = value === true ? true : value === false ? false : null
      rememberBudget(this.proposal.id, this.needsBudget)
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
    },
    /** "Approve for Implementation" opens the same confirmation for the chosen routing. */
    openApproval() {
      if (this.busy || !this.atOc) return
      if (this.needsBudget !== true && this.needsBudget !== false) return
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
      this.showConfirm = true
    },
    openReturn() {
      if (this.busy || !this.atOc) return
      this.returnError = ''
      this.showReturnConfirm = true
    },
    closeReturn() {
      if (this.busy) return
      this.showReturnConfirm = false
      this.returnError = ''
    },
    /**
     * Re-read the proposal after an action so the page shows the status the server
     * actually stored. Deliberately quiet: a failed refresh must not replace the
     * confirmed result with a page error.
     */
    async refreshProposalState() {
      const proposalId = this.proposal.id || this.$route.params.id
      if (!proposalId) return
      try {
        const res = await api.get(`/api/proposals/${proposalId}`)
        if (res.data) {
          this.proposal = res.data
          this.applyProposalPrefill()
        }
      } catch (err) {
        console.error('Refreshing the proposal after the action failed:', err)
      }
    },
    async confirmApproval() {
      if (!this.soNumber || !this.soNumber.trim()) {
        return
      }
      // One submission at a time: a second click while the first request is in
      // flight must not create a duplicate approval/SO record.
      if (this.busy) {
        return
      }
      // Exactly one classification must be selected. The API accepts a single boolean and
      // rejects anything else, but the form refuses to send an ambiguous request at all.
      if (this.needsBudget !== true && this.needsBudget !== false) {
        this.approvalError =
          'Select exactly one budget classification (With Budget or Without Budget) before approving.'
        return
      }
      if (!this.atOc) {
        this.approvalError = 'This proposal is no longer awaiting the Chancellor\'s final approval.'
        return
      }
      this.approvalError = ''
      this.draftError = ''
      this.draftMessage = ''
      this.actionLoading = true
      this.soUploading = true
      try {
        const proposalId = this.$route.params.id

        // Build FormData for multipart request. The Content-Type (including the
        // multipart boundary) is left to the browser/axios: setting it manually hides
        // the boundary and makes the request unreadable to the server.
        const formData = new FormData()
        formData.append('soNumber', this.soNumber.trim())
        formData.append('needsBudget', this.needsBudget)
        if (this.chancellorNotes && this.chancellorNotes.trim()) {
          formData.append('chancellorNotes', this.chancellorNotes.trim())
        }
        if (this.soFile) {
          formData.append('soFile', this.soFile)
        }
        if (this.removeSavedSoFile) {
          // A draft document the user removed must not be resurrected by the approval.
          formData.append('removeSoFile', 'true')
        }

        const res = await api.put(
          `/api/proposals/${proposalId}/approve-with-so`,
          formData
        )

        // The server confirms the whole operation; only now does the form close and
        // the success state appear. The page is refreshed first so the status shown
        // (FOR_OVCAF_APPROVAL or APPROVED) is the one the server actually stored.
        this.showConfirm = false
        await this.refreshProposalState()
        // The classification is now stored on the proposal; the tentative tab copy is no
        // longer needed and must not be re-applied to a later decision.
        rememberBudget(proposalId, null)
        this.successMessage = res.data?.message || 'Proposal approved with Special Order.'
        this.showSuccess = true
      } catch (err) {
        console.error('Approval failed:', err)
        const status = err.response?.status
        let errorMsg = err.response?.data?.error || err.response?.data?.message
        if (!errorMsg && status === 413) {
          errorMsg = 'The Special Order document is too large for the server. The maximum accepted size is 25 MB.'
        }
        if (!errorMsg) {
          errorMsg = err.message || 'Unknown error occurred.'
        }
        // The proposal was not changed. Keep the confirmation form (SO number and
        // file) open so the request can be corrected and retried, and never show the
        // green success state for a failed approval.
        this.approvalError = `Approval failed: ${errorMsg}`
      } finally {
        this.actionLoading = false
        this.soUploading = false
      }
    },
    /**
     * Return the proposal to OVCRIGE through its own endpoint. This is a real status
     * transition (FOR_OC_APPROVAL -> REC_APPROVED), so it is sent only after the
     * confirmation and reported only when the server accepts it. A failure never
     * reaches the green success dialog.
     */
    async returnToOvcrige() {
      if (this.busy) return
      if (!this.atOc) {
        this.returnError =
          'Only a proposal awaiting the Chancellor\'s final approval can be returned to OVCRIGE.'
        return
      }
      this.returnError = ''
      this.actionLoading = true
      try {
        const proposalId = this.proposal.id || this.$route.params.id
        const remarks = String(this.chancellorNotes || '').trim()
        const res = await api.put(`/api/proposals/${proposalId}/return-to-ovcrige`, {
          remarks: remarks || null,
        })
        this.showReturnConfirm = false
        await this.refreshProposalState()
        rememberBudget(proposalId, null)
        this.successMessage =
          res.data?.message || 'Proposal returned to OVCRIGE for re-forwarding.'
        this.showSuccess = true
      } catch (err) {
        console.error('Return to OVCRIGE failed:', err)
        const status = err.response?.status
        const errorMsg =
          err.response?.data?.error ||
          err.response?.data?.message ||
          (status === 409
            ? 'This proposal is no longer awaiting the Chancellor\'s final approval.'
            : '') ||
          err.message ||
          'The proposal could not be returned to OVCRIGE.'
        this.returnError = `Return failed: ${errorMsg}`
        // A conflict means the record moved on (another office acted first): re-read it
        // so the page stops offering an action the server no longer allows.
        if (status === 409) await this.refreshProposalState()
      } finally {
        this.actionLoading = false
      }
    },
    handleSuccessClose() {
      this.showSuccess = false
      this.$router.push('/oc-dashboard')
    },
  },
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.page {
  padding: 24px;
  background: #f1f5f9;
  min-height: 100vh;
  font-family: 'Segoe UI', Arial, sans-serif;
}

.back-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.download-error {
  margin: -8px 0 16px;
}

/* Layout only; .btn-secondary supplies the neutral colour. */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.back-btn:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.back-arrow {
  font-size: 16px;
  line-height: 1;
}

.center {
  text-align: center;
  padding: 60px 40px !important;
}

/* Layout */
.layout {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
  align-items: start;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

/* Cards */
.card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.card h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #f1f5f9;
}

/* Proposal Header */
.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.proposal-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.3;
}

/* Geometry only; the tone comes from assets/status.css. */
.status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Meta Grid */
.proposal-meta {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 16px;
}

@media (max-width: 600px) {
  .proposal-meta {
    grid-template-columns: 1fr 1fr;
  }
}

.meta-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}

.meta-value {
  font-weight: 600;
  color: #334155;
  margin: 4px 0 0 0;
  font-size: 14px;
}

/* Summary */
.summary-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  text-align: justify;
  margin: 0;
}

/* Textarea */
textarea {
  width: 100%;
  min-height: 130px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 14px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

/* Timeline */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  position: relative;
}

.timeline-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 32px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
  margin-top: 4px;
  flex-shrink: 0;
  z-index: 1;
}

.timeline-item.done .dot {
  background: var(--status-approved-solid);
}

.timeline-item.current .dot {
  background: var(--status-pending-solid);
}

.dot.pulse {
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.25);
}

.tl-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.tl-desc {
  font-size: 12px;
  color: #64748b;
  margin: 2px 0;
}

.timeline-item small {
  font-size: 11px;
  color: #94a3b8;
}

/* Budget Options */
.budget-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 14px 0;
}

.budget-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.budget-option {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.budget-option:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.budget-option.selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

/* A record that is no longer awaiting the Chancellor cannot be routed. */
.budget-option.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.budget-option.disabled:hover {
  border-color: #e2e8f0;
  background: transparent;
}

.budget-radio {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.budget-radio input[type="radio"] {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: #3b82f6;
  cursor: pointer;
}

.budget-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.budget-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
}

.budget-description {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

/* The one selected classification is stated in words, not only by a border colour. */
.budget-selected-tag {
  align-self: center;
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 12px;
}

/* Layout only; .btn-approve supplies the green. */
.btn-approve {
  flex: 1;
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-approve:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-approve.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-approve:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Layout only; .btn-return supplies the amber. */
.btn-return {
  padding: 14px 28px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-return:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hint {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 10px;
}

/* A record that has left the Chancellor has no actions; say why in place of the buttons. */
.hint.decision-locked {
  text-align: left;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
}

/* Error */
.error-text {
  color: #dc2626;
  font-size: 14px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.15s;
}

.modal {
  width: 440px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
  padding: 24px;
  animation: slideUp 0.2s ease;
}

.success-modal {
  width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.close {
  cursor: pointer;
  font-size: 24px;
  color: #94a3b8;
  line-height: 1;
  transition: color 0.2s;
}

.close:hover {
  color: #475569;
}

.modal-body {
  text-align: center;
  margin: 24px 0;
}

.check-icon {
  width: 56px;
  height: 56px;
  background: #dcfce7;
  color: #16a34a;
  border-radius: 50%;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px auto;
}

.check-icon.success {
  background: #dcfce7;
  color: #16a34a;
}

.modal-body h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #1e293b;
}

.modal-body p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-actions.center {
  justify-content: center;
}

/* Modal buttons: layout only; .btn-cancel / .btn-approve / .btn-primary supply colours. */
.btn-cancel {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-confirm {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* SO Modal Styles */
.so-modal {
  width: 500px;
}

.so-section {
  margin: 16px 0;
  text-align: left;
}

.so-field {
  margin-bottom: 14px;
}

.so-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.so-label .required {
  color: #dc2626;
}

.so-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}

.so-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.file-upload-wrapper {
  position: relative;
}

.so-file-input {
  width: 100%;
  padding: 8px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  background: #f8fafc;
}

.so-file-input:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.file-display {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
}

.file-icon {
  font-size: 16px;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #166534;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.file-remove:hover {
  color: #991b1b;
}

/* The SO document already stored by a draft. */
.saved-file {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.saved-file .file-name {
  color: #1d4ed8;
}

.saved-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.file-link {
  background: none;
  border: none;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 4px;
  text-decoration: underline;
}

.saved-file-note {
  margin-top: 8px;
  font-size: 12px;
  color: #b45309;
}

/* Draft save feedback inside the modal. */
.draft-success {
  margin-top: 14px !important;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534 !important;
  font-size: 13px !important;
  font-weight: 600;
  text-align: left;
}

.draft-saved-note {
  margin-top: 10px !important;
  font-size: 12px !important;
  color: #1d4ed8 !important;
  font-style: italic;
}

/* The three modal actions share the width evenly. */
.modal-actions .btn-secondary {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}

.modal-actions .btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.routing-info {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  text-align: left;
}

.routing-info.with-budget {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.routing-info.no-budget {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.routing-info p {
  font-size: 13px;
  color: #475569;
  margin: 0;
  line-height: 1.5;
}

.routing-info p strong {
  color: #1e293b;
}

.modal-hint {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 10px;
}

.modal-error {
  margin-top: 14px !important;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c !important;
  font-size: 13px !important;
  font-weight: 500;
  text-align: left;
  line-height: 1.5;
}

.return-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  text-align: left;
  margin: 8px 0 0;
}

.return-notes {
  margin-top: 12px !important;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155 !important;
  font-size: 12.5px !important;
  text-align: left;
  line-height: 1.5;
}

.return-icon {
  background: #fef3c7;
  color: #b45309;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>