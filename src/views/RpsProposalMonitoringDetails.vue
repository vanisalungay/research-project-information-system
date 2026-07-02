<template>
  <div class="page">
    <!-- Back Navigation -->
    <div class="details-nav">
      <button class="btn btn-secondary btn-back" @click="goBackToList">
        ← Back to Proposals List
      </button>
      <span class="view-indicator-pill">Monitoring Details Mode</span>
    </div>

    <div v-if="proposal" class="details-content">
      <!-- Overall Summary Section (Top Section) -->
      <div class="summary-section">
        <div class="summary-grid">
          <div class="summary-info">
            <div class="title-row">
              <span class="category-badge">{{ proposal.program }}</span>
              <span class="id-text">Proposal ID: #{{ proposal.code }}</span>
            </div>
            <h3 class="proposal-title">{{ proposal.title }}</h3>
            
            <div class="details-grid">
              <div class="detail-item">
                <span class="label">Project Leader</span>
                <span class="value">{{ proposal.leader }}</span>
              </div>
              <div class="detail-item">
                <span class="label">College / Department</span>
                <span class="value">{{ proposal.college }} ({{ proposal.department }})</span>
              </div>
              <div class="detail-item">
                <span class="label">Date Submitted</span>
                <span class="value">{{ proposal.submittedDate }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Current Office</span>
                <span class="value current-office-highlight">{{ getCurrentOffice(proposal) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Overall Status</span>
                <span class="value overall-status-highlight">{{ getCurrentOverallStatus(proposal) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Approved Budget</span>
                <span class="value">₱{{ proposal.budget.toLocaleString() }}</span>
              </div>
            </div>
          </div>

          <!-- Circular Progress Gauge -->
          <div class="progress-gauge-container">
            <div class="circular-progress">
              <svg class="progress-ring" width="125" height="125">
                <circle
                  class="progress-ring__background"
                  stroke="#e2e8f0"
                  stroke-width="10"
                  fill="transparent"
                  r="50"
                  cx="62.5"
                  cy="62.5"
                />
                <circle
                  class="progress-ring__circle"
                  stroke="#4f46e5"
                  stroke-width="10"
                  fill="transparent"
                  r="50"
                  cx="62.5"
                  cy="62.5"
                  :stroke-dasharray="strokeDasharray"
                  :stroke-dashoffset="strokeDashoffset"
                />
              </svg>
              <div class="progress-text-overlay">
                <span class="percentage">{{ progressPercentage }}%</span>
                <span class="text-label">Progress</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Vertical Timeline Workflow Stepper -->
      <div class="timeline-container">
        <!-- Main timeline connector line -->
        <div class="timeline-line"></div>

        <!-- Stepper Node Cards -->
        <div 
          v-for="(stage, key) in workflowStages" 
          :key="key" 
          class="timeline-item"
          :class="{
            'item-waiting': stage.status === 'Waiting',
            'item-active': isStageActive(key),
            'item-approved': stage.status === 'Approved' || stage.status === 'Funds Released' || stage.status === 'Completed',
            'item-returned': stage.status === 'Returned for Revision' || stage.status === 'Ready for Release',
            'item-rejected': stage.status === 'Rejected'
          }"
        >
          <!-- Timeline Indicator Node Badge -->
          <div class="timeline-badge">
            <div class="badge-dot"></div>
          </div>

          <!-- Timeline Card -->
          <div class="timeline-card">
            <div class="card-header">
              <div>
                <span class="step-num">Stage {{ getStageNumber(key) }}</span>
                <h4 class="office-name">{{ stage.name }}</h4>
              </div>
              <span :class="['status-badge', getBadgeClass(stage.status)]">
                {{ stage.status }}
              </span>
            </div>

            <div class="card-body">
              <div class="card-meta">
                <div class="meta-field">
                  <span class="meta-label">Reviewed By:</span>
                  <span class="meta-value">{{ stage.reviewer || '-' }}</span>
                </div>
                <div class="meta-field">
                  <span class="meta-label">Decision Date:</span>
                  <span class="meta-value">{{ stage.date || '-' }}</span>
                </div>
              </div>

              <!-- Display remarks if any exist -->
              <div v-if="stage.remarks && stage.remarks !== '-'" class="card-remarks">
                <span class="remarks-label">Remarks from {{ getShortOfficeName(key) }}:</span>
                <p class="remarks-text">"{{ stage.remarks }}"</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else class="error-card">
      <div class="error-icon">⚠️</div>
      <h3>Proposal Not Found</h3>
      <p>The monitoring records for the requested proposal ID could not be located.</p>
      <button class="btn btn-primary" @click="goBackToList">Return to Proposals List</button>
    </div>
  </div>
</template>

<script>
const STORAGE_KEY = 'proposals_workflow_db';

export default {
  name: 'ProposalMonitoringDetails',

  data() {
    return {
      proposals: [],
      proposal: null
    }
  },

  computed: {
    workflowStages() {
      return this.proposal ? this.proposal.stages : {};
    },

    progressPercentage() {
      if (!this.proposal) return 0;
      return this.getProgressPercentage(this.proposal);
    },

    strokeDasharray() {
      const radius = 50;
      return 2 * Math.PI * radius; // Approx 314
    },

    strokeDashoffset() {
      const percentage = this.progressPercentage;
      const dasharray = this.strokeDasharray;
      return dasharray - (percentage / 100) * dasharray;
    }
  },

  methods: {
    getStageNumber(key) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      return order.indexOf(key) + 1;
    },

    getShortOfficeName(key) {
      const map = {
        proponent: 'Proponent',
        rii: 'RII',
        ovcrige: 'OVCRIGE',
        rec: 'REC',
        oc: 'OC',
        ovcaf: 'OVCAF',
        implementation: 'Implementation'
      };
      return map[key] || key.toUpperCase();
    },

    getCurrentOffice(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status === 'Rejected' || stage.status === 'Returned for Revision') {
          return this.getShortOfficeName(key);
        }
      }
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status !== 'Approved' && stage.status !== 'Funds Released' && stage.status !== 'Completed') {
          return this.getShortOfficeName(key);
        }
      }
      
      return 'Implementation';
    },

    getCurrentOverallStatus(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status === 'Rejected' || stage.status === 'Returned for Revision') {
          return stage.status;
        }
      }

      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status !== 'Approved' && stage.status !== 'Funds Released' && stage.status !== 'Completed') {
          if (stage.status === 'Waiting') {
            const prevIndex = order.indexOf(key) - 1;
            if (prevIndex >= 0) {
              const prevStage = proposal.stages[order[prevIndex]];
              if (prevStage.status === 'Approved' || prevStage.status === 'Funds Released') {
                return 'Under Review';
              }
            }
            return 'Waiting';
          }
          return stage.status;
        }
      }
      
      const lastStage = proposal.stages.implementation;
      return lastStage.status;
    },

    getProgressPercentage(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      let completedCount = 0;
      
      for (const key of order) {
        const stage = proposal.stages[key];
        const status = stage.status;
        if (status === 'Approved' || status === 'Funds Released' || status === 'Completed') {
          completedCount += 1.0;
        } else if (status === 'Under Review' || status === 'Ongoing') {
          completedCount += 0.5;
        } else if (status === 'Ready for Release' || status === 'For Implementation') {
          completedCount += 0.8;
        } else if (status === 'Returned for Revision') {
          completedCount += 0.2;
        } else if (status === 'Rejected') {
          completedCount += 0.1;
        }
      }
      return Math.min(100, Math.round((completedCount / order.length) * 100));
    },

    isStageActive(key) {
      if (!this.proposal) return false;
      const current = this.getCurrentOffice(this.proposal);
      return this.getShortOfficeName(key) === current;
    },

    getBadgeClass(status) {
      switch (status) {
        case 'Approved':
        case 'Completed':
        case 'Funds Released':
          return 'badge-green';
        case 'Returned for Revision':
        case 'Ready for Release':
          return 'badge-yellow';
        case 'Rejected':
          return 'badge-red';
        case 'Under Review':
        case 'For Implementation':
        case 'Ongoing':
        case 'Submitted':
          return 'badge-blue';
        case 'Waiting':
        default:
          return 'badge-gray';
      }
    },

    goBackToList() {
      this.$router.push('/rii-subproposal');
    },

    loadProposal() {
      this.proposals = [];
      this.proposal = null;
    },

    syncWithBudgetRelease() {
      const savedBudget = localStorage.getItem("budgetProposal");
      if (savedBudget && this.proposal) {
        try {
          const budgetObj = JSON.parse(savedBudget);
          const matchTitle = budgetObj.project_title || budgetObj.title;
          
          if (this.proposal.title === matchTitle || this.proposal.id === budgetObj.id) {
            const status = budgetObj.release_status || budgetObj.status;
            if (status === "Released") {
              this.proposal.stages.ovcaf.status = "Funds Released";
              this.proposal.stages.ovcaf.reviewer = budgetObj.released_by || "Budget Officer";
              this.proposal.stages.ovcaf.date = budgetObj.release_date || new Date().toLocaleDateString();
              this.proposal.stages.ovcaf.remarks = budgetObj.ovcaf_remarks || "Funds released successfully.";
              
              if (this.proposal.stages.implementation.status === "Waiting") {
                this.proposal.stages.implementation.status = "For Implementation";
                this.proposal.stages.implementation.date = new Date().toLocaleDateString();
                this.proposal.stages.implementation.reviewer = "Budget Release Sync";
                this.proposal.stages.implementation.remarks = "Project activated following funds release.";
              }
            } else if (status === "Returned") {
              this.proposal.stages.ovcaf.status = "Returned for Revision";
              this.proposal.stages.ovcaf.reviewer = "Budget Officer";
              this.proposal.stages.ovcaf.date = new Date().toLocaleDateString();
              this.proposal.stages.ovcaf.remarks = budgetObj.ovcaf_remarks || "Budget returned for adjustments.";
              
              this.proposal.stages.implementation.status = "Waiting";
              this.proposal.stages.implementation.date = "-";
              this.proposal.stages.implementation.reviewer = "-";
              this.proposal.stages.implementation.remarks = "-";
            }
            
            // Save synced state back to proposals list
            const index = this.proposals.findIndex(p => p.id === this.proposal.id);
            if (index !== -1) {
              this.proposals[index] = this.proposal;
              localStorage.setItem(STORAGE_KEY, JSON.stringify(this.proposals));
            }
          }
        } catch (e) {
          console.error("Failed to parse budgetProposal sync", e);
        }
      }
    }
  },

  mounted() {
    this.loadProposal();
  }
}
</script>

<style scoped>
.page {
  padding: 30px;
  font-family: 'Poppins', 'Inter', sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
  color: #1e293b;
  width: 100%;
  box-sizing: border-box;
}

.details-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.btn {
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background-color: #4f46e5;
  color: white;
}

.btn-primary:hover {
  background-color: #4338ca;
}

.btn-secondary {
  background-color: white;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.btn-secondary:hover {
  background-color: #f1f5f9;
}

.btn-back {
  font-size: 12.5px;
  font-weight: 600;
  padding: 8px 16px;
}

.view-indicator-pill {
  background-color: #e0e7ff;
  color: #4338ca;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.summary-section {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.02);
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 30px;
  align-items: center;
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.category-badge {
  background: #eff6ff;
  color: #1d4ed8;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 6px;
}

.id-text {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.proposal-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 18px 0;
  line-height: 1.4;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.detail-item .value {
  font-size: 13.5px;
  color: #334155;
  font-weight: 500;
}

.current-office-highlight {
  color: #4f46e5 !important;
  font-weight: 700 !important;
}

.overall-status-highlight {
  color: #0f172a !important;
  font-weight: 700 !important;
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.progress-gauge-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.circular-progress {
  position: relative;
  width: 125px;
  height: 125px;
}

.progress-ring__circle {
  transition: stroke-dashoffset 0.35s;
  transform: rotate(-90deg);
  transform-origin: 50% 50%;
}

.progress-text-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-text-overlay .percentage {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.progress-text-overlay .text-label {
  font-size: 10px;
  text-transform: uppercase;
  font-weight: 600;
  color: #64748b;
}

.timeline-container {
  position: relative;
  padding: 10px 0;
  margin-bottom: 40px;
}

.timeline-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 20px;
  width: 4px;
  background-color: #cbd5e1;
  border-radius: 2px;
  z-index: 1;
}

.timeline-item {
  position: relative;
  display: flex;
  margin-bottom: 24px;
  z-index: 2;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-badge {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: #e2e8f0;
  border: 4px solid #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.06);
  flex-shrink: 0;
  z-index: 3;
}

.badge-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background-color: #94a3b8;
}

.timeline-card {
  margin-left: 20px;
  background-color: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 24px;
  flex-grow: 1;
  box-shadow: 0 2px 6px rgba(0,0,0,0.01);
  transition: all 0.2s;
}

.timeline-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.step-num {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  color: #64748b;
  letter-spacing: 0.5px;
}

.office-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 2px 0 0 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 600;
  display: inline-block;
}

.badge-gray {
  background-color: #f1f5f9;
  color: #64748b;
}

.badge-blue {
  background-color: #dbeafe;
  color: #1e40af;
}

.badge-green {
  background-color: #dcfce7;
  color: #166534;
}

.badge-yellow {
  background-color: #fef3c7;
  color: #92400e;
}

.badge-red {
  background-color: #fee2e2;
  color: #991b1b;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.meta-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.meta-label {
  color: #64748b;
  font-weight: 500;
}

.meta-value {
  color: #334155;
  font-weight: 600;
}

.card-remarks {
  background-color: #f8fafc;
  border-left: 3px solid #cbd5e1;
  padding: 10px 14px;
  border-radius: 4px;
  margin-top: 4px;
}

.remarks-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  display: block;
  margin-bottom: 2px;
}

.remarks-text {
  font-size: 13px;
  color: #475569;
  margin: 0;
  font-style: italic;
  line-height: 1.4;
}

.item-waiting .timeline-badge {
  background-color: #e2e8f0;
}
.item-waiting .badge-dot {
  background-color: #94a3b8;
}
.item-waiting .timeline-card {
  opacity: 0.6;
  border-style: dashed;
}

.item-active .timeline-badge {
  background-color: #dbeafe;
  border-color: #3b82f6;
  animation: pulse-ring 2s infinite;
}
.item-active .badge-dot {
  background-color: #1d4ed8;
}
.item-active .timeline-card {
  border-color: #93c5fd;
}

.item-approved .timeline-badge {
  background-color: #dcfce7;
  border-color: #22c55e;
}
.item-approved .badge-dot {
  background-color: #15803d;
}

.item-returned .timeline-badge {
  background-color: #fef3c7;
  border-color: #f59e0b;
}
.item-returned .badge-dot {
  background-color: #b45309;
}
.item-returned .timeline-card {
  border-color: #fcd34d;
}
.item-returned .card-remarks {
  border-left-color: #f59e0b;
  background-color: #fffbeb;
}

.item-rejected .timeline-badge {
  background-color: #fee2e2;
  border-color: #ef4444;
}
.item-rejected .badge-dot {
  background-color: #b91c1c;
}
.item-rejected .timeline-card {
  border-color: #fca5a5;
}
.item-rejected .card-remarks {
  border-left-color: #ef4444;
  background-color: #fef2f2;
}

.error-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  max-width: 500px;
  margin: 100px auto;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-card h3 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.error-card p {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 24px 0;
}

@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(59, 130, 246, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
  }
}
</style>
