<template>
  <div class="proposal-page">

    <header class="proposal-header">
      <div class="header-title">
        <div>
          <h2>Budget Processing</h2>
          <p>Review approved budgets and processs fund release</p>
        </div>
      </div>
            <button class="back-btn" @click="goBack">← Back</button>

    </header>

    <div class="proposal-layout">

      <section class="proposal-content">
        <div class="section-block">

          <h3>Research Information</h3>

          <div class="grid-2">
            <div><span>Researcher</span><p>{{ proposal.researcher }}</p></div>
            <div><span>Department</span><p>{{ proposal.department }}</p></div>
            <div><span>College</span><p>{{ proposal.college }}</p></div>
            <div><span>Date Submitted</span><p>{{proposal.date_submitted}}</p></div>
            <div><span>Project Duration</span><p>{{ proposal.project_duration }}</p></div>
            <div><span>Research Type</span><p>{{proposal.research_type}}</p></div>
          </div>

        </div>

        <div class="section-block">

          <h3>Project Profile</h3>

          <div class="grid-2">
            <div><span>Program Title</span><p>{{ proposal.program_title }}</p></div>
            <div><span>Project Title</span><p>{{proposal.project_title}}</p></div>
            <div><span>Project Leader</span><p>{{ proposal.project_leader }}</p></div>
            <div><span>Start Date</span><p>{{proposal.project_start_date}}</p></div>
            <div><span>End Date</span><p>{{proposal.project_end_date}}</p></div>
            <div><span>Duration</span><p>{{ proposal.project_duration }}</p></div>
          </div>

        </div>

        <div class="section-block">
          <h3>(2) Cooperating Agency / IES</h3>

          <div class="grid-2">
            <div>
              <span>Agency</span>
              <p>{{ proposal.agency_name }}</p>
            </div>

            <div>
              <span>Address</span>
              <p>{{ proposal.address }}</p>
            </div>
          </div>
        </div>

        <div class="section-block">
          <h3>(3) Site of Implementation</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Country</th>
                <th>Region</th>
                <th>Province</th>
                <th>Municipality</th>
                <th>Barangay</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{proposal.country}}</td>
                <td>{{proposal.region}}</td>
                <td>{{proposal.province}}</td>
                <td>{{proposal.municipality}}</td>
                <td>{{proposal.barangay}}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section-block">
          <h3>(4) Type of Research</h3>
          <p>{{proposal.research_type}}</p>
        </div>

        <div class="section-block">
          <h3>(5) Priority Agenda</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Program</th>
                <th>Selected</th>
                <th>Commodity / Sector</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{proposal.program}}</td>
                <td>{{proposal.selected}}</td>
                <td>{{proposal.commodity_sector}}</td>
              </tr>

              <tr>
                <td>{{proposal.program}}</td>
                <td>{{proposal.selected}}</td>
                <td>{{proposal.commodity_sector}}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section-block">
          <h3>(6) Innovation Goals</h3>
          <div class="document-box">
            {{ proposal.innovation_goals }}
          </div>
        </div>

        <div class="section-block">
          <h3>(7) Sector Relevance</h3>
          <div class="document-box">
            {{ proposal.sector_relevance }}
          </div>
        </div>

        <div class="section-block">
          <h3>(8) Sustainable Development Goals</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Goal</th>
                <th>Description</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{ proposal.goal }}</td>
                <td>{{ proposal.goal_description }}</td>
              </tr>

              <tr>
                <td>{{ proposal.goal }}</td>
                <td>{{ proposal.goal_description }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section-block">
          <h3>(9) Executive Summary</h3>
          <div class="document-box">
           {{ proposal.executive_summary }}
          </div>
        </div>

        <div class="section-block">
          <h3>(10) Introduction</h3>
          <div class="document-box">
           {{ proposal.introduction }}
          </div>
        </div>  
        
        <div class="section-block">
          <h3>(11) Review of Literature</h3>
          <div class="document-box">
          {{proposal.review_of_literature}}
          </div>
        </div>

        <div class="section-block">
          <h3>(12) Methodology</h3>
          <div class="document-box">
          {{proposal.methodology}}
          </div>
        </div>

        <div class="section-block">
          <h3>(13) Technology Roadmap</h3>

          <div class="attachment-card">
            <div class="attachment-info">
              <div class="attachment-icon">
                📄
              </div>

              <div class="attachment-details">
                <h4>technology-roadmap.pdf</h4>
                <p>PDF Document</p>
              </div>
            </div>

            <div class="attachment-buttons">
              <button
                class="view-btn"
                @click="viewAttachment('technology-roadmap.pdf')"
              >
                👁 View
              </button>

              <button
                class="download-btn"
                @click="downloadAttachment('technology-roadmap.pdf')"
              >
                ⬇ Download
              </button>
            </div>
          </div>
        </div>

        <div class="section-block">
          <h3>(14) Expected Outputs (6Ps)</h3>
          <div class="document-box">
          {{proposal.expected_outputs}}
          </div>
        </div>

        <div class="section-block">
          <h3>(15) Potential Outcomes</h3>
          <div class="document-box">
          {{proposal.potential_outcomes}}
          </div>
        </div>

        <div class="section-block">
          <h3>(16) Potential Impacts</h3>

          <h4>Economic Impact</h4>
          <div class="document-box">
            {{proposal.potential_impacts_economic}}
          </div>

          <h4>Social / Ethical Impact</h4>
          <div class="document-box">
            {{proposal.potential_impacts_social_ethical}}
          </div>
        </div>

        <div class="section-block">
          <h3>(17) Target Beneficiaries</h3>
          <div class="document-box">
            {{proposal.target_beneficiaries}}
          </div>
        </div>

        <div class="section-block">
          <h3>(18) Sustainability Plan</h3>
          <div class="document-box">
          {{proposal.sustainability_plan}}
          </div>
        </div>

        <div class="section-block">
          <h3>(19) Gender and Development (GAD) Score</h3>

          <div class="attachment-card">
            <div class="attachment-info">
              <div class="attachment-icon">
                📄
              </div>

              <div class="attachment-details">
                <h4>gad-score.pdf</h4>
                <p>PDF Document</p>
              </div>
            </div>

            <div class="attachment-buttons">
              <button
                class="view-btn"
                @click="viewAttachment('gad-score.pdf')"
              >
                👁 View
              </button>

              <button
                class="download-btn"
                @click="downloadAttachment('gad-score.pdf')"
              >
                ⬇ Download
              </button>
            </div>
          </div>
        </div>

        <div class="section-block">
          <h3>(20) Limitations of the Project</h3>
          <div class="document-box">
          {{proposal.project_limitations}}
          </div>
        </div>

        <div class="section-block">
          <h3>(21) Risk Management Plan</h3>
          <div class="document-box">
            {{proposal.risk_management_plan}}
          </div>
        </div>

        <div class="section-block">
          <h3>(22) Logical Framework (Outcome and Output Indicators)</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Outcome Indicators</th>
                <th>Output Indicators</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{proposal.outcome_indicators}}</td>
                <td>{{proposal.output_indicators}}</td>
              </tr>

              <tr>
                <td>{{proposal.outcome_indicators}}</td>
                <td>{{proposal.output_indicators}}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section-block">
          <h3>(23) Literature Cited</h3>
          <div class="document-box">
            {{proposal.literature_cited}} 
          </div>
        </div>

        <div class="section-block">
          <h3>(24) Personnel Requirement</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Position</th>
                <th>% Time</th>
                <th>Responsibilities</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{proposal.position}}</td>
                <td>{{proposal.time_percentage}}</td>
                <td>{{proposal.responsibilities}}</td>
              </tr>

              <tr>
                <td>{{proposal.position}}</td>
                <td>{{proposal.time_percentage}}</td>
                <td>{{proposal.responsibilities}}</td>
              </tr>
            </tbody>
          </table>
        </div>

      <div class="section-block">
        <h3>(25) Line Item Budget Requirement</h3>

        <div class="attachment-card">
          <div class="attachment-info">
            <div class="attachment-icon">
              📄
            </div>

            <div class="attachment-details">
              <h4>Project-Line-Item-Budget.pdf</h4>
              <p>PDF Document</p>
            </div>
          </div>

          <div class="attachment-buttons">
            <button
              class="view-btn"
              @click="viewAttachment('Project-Line-Item-Budget.pdf')"
            >
              👁 View
            </button>

            <button
              class="download-btn"
              @click="downloadAttachment('Project-Line-Item-Budget.pdf')"
            >
              ⬇ Download
            </button>
          </div>
        </div>
      </div>

        <div class="section-block">
          <h3>(26) Other Ongoing Projects</h3>

          <table class="proposal-table">
            <thead>
              <tr>
                <th>Project Title</th>
                <th>Funding Agency</th>
                <th>Role</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>{{proposal.project_title}}</td>
                <td>{{proposal.funding_agency}}</td>
                <td>{{proposal.role}}</td>
              </tr>

              <tr>
                <td>{{proposal.project_title}}</td>
                <td>{{proposal.funding_agency}}</td>
                <td>{{proposal.role}}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section-block">
          <h3>(27) Other Supporting Documents</h3>

          <div
            v-for="attachment in proposal.attachments"
            :key="attachment.id"
            class="attachment-card"
          >

            <div class="attachment-info">

              <div class="attachment-icon">
                📄
              </div>

              <div class="attachment-details">

                <h4>{{ attachment.fileName }}</h4>

                <p>PDF Document</p>

              </div>

            </div>

            <div class="attachment-buttons">

              <button
                class="view-btn"
                @click="viewAttachment(attachment.filePath)"
              >
                👁 View
              </button>

              <button
                class="download-btn"
                @click="downloadAttachment(attachment.filePath)"
              >
                ⬇ Download
              </button>

            </div>

          </div>
        </div>

      </section>

      <aside class="proposal-sidebar">

        <div class="review-card">

        <h2>Budget Processing</h2>

        <div class="review-section">
        <h4>Budget Information</h4>

       <div class="info-row">
        <label>Requested Budget</label>
        <p>
          ₱{{ Number(proposal.requested_budget || 0).toLocaleString() }}
        </p>
      </div>

      <div class="info-row">
        <label>Approved Budget</label>
        <p>
          ₱{{ Number(proposal.approved_budget || 0).toLocaleString() }}
        </p>
      </div>

        </div>
            <div class="review-section">
              <h4>Budget Attachment</h4>

              <div class="budget-file">

                <div class="budget-file-icon">
                  📄
                </div>

                <div class="budget-file-info">
                  <strong>Project-Line-Item-Budget.pdf</strong>
                  <small>PDF Document</small>
                </div>

                <div class="budget-file-actions">

                  <button
                    class="mini-btn view-btn"
                    @click="viewAttachment('Project-Line-Item-Budget.pdf')"
                  >
                    👁
                  </button>

                  <button
                    class="mini-btn download-btn"
                    @click="downloadAttachment('Project-Line-Item-Budget.pdf')"
                  >
                    ⬇
                  </button>

                </div>

              </div>

            </div>
        
      <div class="review-section">
        <h4>OVCAF Remarks</h4>

      <div class="readonly-box">
        {{ proposal.ovcaf_remarks || "No remarks available." }}
      </div>
</div>
      <div class="review-section">
        <h4>Release Status</h4>

        <div
          class="readonly-value"
          :class="{
            'approve-card': status === 'Ready for Release',
            'return-card': status === 'Returned',
            'released-card': status === 'Released'
          }"
        >
          {{ status }}
        </div>
      </div>

      <!-- READY FOR RELEASE -->
      <div v-if="status === 'Ready for Release'">

        <hr class="review-divider">

        <div class="review-actions">

          <button
            class="edit-review-btn"
            @click="returnBudget"
          >
            Return Budget
          </button>

          <button
            class="done-btn"
            @click="releaseFunds"
          >
            Release Funds
          </button>

        </div>

      </div>

      <!-- RETURNED -->
      <div v-else-if="status === 'Returned'">

        <div class="review-section">
          <h4>Reason for Return</h4>

          <div class="readonly-box">
            {{ returnReason }}
          </div>
        </div>

        <div class="review-actions">
          <button
            class="done-btn"
            @click="goBack"
          >
            Back
          </button>
        </div>

      </div>

      <!-- RELEASED -->
      <div v-else>

        <div class="review-section">

          <h4>Release Information</h4>

          <div class="info-row">
            <label>Date Released</label>
            <p>{{ releaseInfo.date }}</p>
          </div>

          <div class="info-row">
            <label>Released By</label>
            <p>{{ releaseInfo.releasedBy }}</p>
          </div>

          <div class="info-row">
            <label>Reference No.</label>
            <p>{{ releaseInfo.referenceNo }}</p>
          </div>

        </div>

        <div class="review-actions">

          <button
            class="done-btn"
            @click="goBack"
          >
            Back
          </button>

        </div>

      </div>

        </div>

        </aside>
    </div>

    <ConfirmDialog
      v-if="dialog.show"
      :type="dialog.type"
      :variant="dialog.variant"
      :title="dialog.title"
      :message="dialog.message"
      :confirmText="dialog.confirmText"
      :cancelText="dialog.cancelText"
      @confirm="dialog.onConfirm"
      @cancel="dialog.onCancel"
      @close="dialog.show = false"
    />
  </div>
</template>

<script>
import ConfirmDialog from '@/components/ConfirmDialog.vue'

export default {
  name: "RpsBudgetRelease",
  components: { ConfirmDialog },

  data() {
  return {
    dialog: {
      show: false, type: 'info', variant: 'alert', title: '', message: '',
      confirmText: 'OK', cancelText: 'Cancel',
      onConfirm: () => {}, onCancel: () => {},
    },
    reviewer: {
      name: "",
      position: "RII Staff",
      certified: false
    },

    reviewStarted: false,

    review: {
      status: "Pending Review"
    },

    // MAIN STATUS OF BUDGET PROCESSING
    status: "Ready for Release",

    // if returned
    returnReason: "",

    // if released
    releaseInfo: {
      date: "",
      releasedBy: "",
      referenceNo: ""
    },

    evaluation: {
      relevance: { rating: "", comment: "" },
      technical: { rating: "", comment: "" },
      budget: { rating: "", comment: "" },
      competence: { rating: "", comment: "" },
      recommendation: "",
      overallComment: "",
      finalRemarks: ""
    },

    proposal: {
      researcher: "",
      department: "",
      college: "",
      projectTitle: "",
      programTitle: "",

      requested_budget: 0,
      approved_budget: 0,
      budget_status: "",
      ovcaf_remarks: "",

      attachments: []
    }
  };
},


  methods: {
    _showDialog(message, options = {}) {
      return new Promise((resolve) => {
        this.dialog.type = options.type || 'info'
        this.dialog.variant = options.variant || 'alert'
        this.dialog.title = options.title || 'Notice'
        this.dialog.message = message
        this.dialog.confirmText = options.confirmText || 'OK'
        this.dialog.cancelText = options.cancelText || 'Cancel'
        this.dialog.onConfirm = () => { this.dialog.show = false; resolve(true) }
        this.dialog.onCancel = () => { this.dialog.show = false; resolve(false) }
        this.dialog.show = true
      })
    },
    _showAlert(message, options = {}) { return this._showDialog(message, { ...options, variant: 'alert' }) },
    _showConfirm(message, options = {}) { return this._showDialog(message, { ...options, variant: 'confirm' }) },

    async downloadProposal() {
      await this._showAlert("Download Proposal PDF", { type: 'info', title: 'Download' });
    },
    viewAttachment(filePath){

      window.open("http://localhost:8080/uploads/"+filePath,
      "_blank"
      )
    },

    downloadAttachment(filePath){
    const link=document.createElement("a");
    link.href="http://localhost:8080/uploads/"+filePath;
    link.download="";
    link.click();
    },

  backToEndorsed(){
    this.$router.push("/riiendorsed-prop")
  },

  async releaseFunds(){
    const confirmed = await this._showConfirm("Are you sure you want to release the funds?", {
      title: 'Release Funds',
      type: 'warning',
      confirmText: 'Release'
    });
    if(!confirmed) return;
    this.status = "Released";
    this.releaseInfo = {
      date: new Date().toLocaleDateString(),
      releasedBy: this.reviewer.name || "Budget Officer",
      referenceNo: "BR-" + Date.now()
    };
    await this._showAlert("Funds released successfully.", { type: 'success', title: 'Funds Released' });
  },

  async returnBudget() {
    if (!this.evaluation.finalRemarks.trim()) {
      await this._showAlert("Please enter OVCAF remarks first.", { type: 'warning', title: 'Remarks Required' });
      return;
    }

    this.releaseStatus = "Returned";
    this.proposal.release_status = "Returned";
    this.proposal.ovcaf_remarks = this.evaluation.finalRemarks;

    localStorage.setItem("budgetProposal", JSON.stringify(this.proposal));

    await this._showAlert("Budget returned successfully.", { type: 'success', title: 'Budget Returned' });
    this.goBack();
  },



},
  mounted(){
    const saved = localStorage.getItem("budgetProposal");

    if (saved) {
      this.proposal = JSON.parse(saved);
    };
    
    this.proposal.attachments=[
    {
      id:1,
      fileName:"Budget.pdf",
      filePath:"Budget.pdf"
    },
    {
      id:2,
      fileName:"Ethics.pdf",
      filePath:"Ethics.pdf"
    },
    {
      id:3,
      fileName:"GAD.pdf",
      filePath:"GAD.pdf"
    }
  ];

  },

  computed: {
  totalScore() {
    return Number(this.evaluation.relevance.rating || 0) +
           Number(this.evaluation.technical.rating || 0) +
           Number(this.evaluation.budget.rating || 0) +
           Number(this.evaluation.competence.rating || 0);
  },
    reviewerPercentage() {
      return this.totalScore;
    },
    isPassing() {
      return this.reviewerPercentage >= 80;
    }
  },
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.proposal-page {
  width: 135%;
  min-height: 100vh;
  padding: 16px;
  background: #f5f6fa;
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 16px;
}
.back-btn{
  background:#3b82f6;
  color:#fff;
  border:none;
  padding:10px 18px;
  border-radius:8px;
  font-size:14px;
  font-weight:600;
  cursor:pointer;


}

.proposal-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
  align-items: start;
}

.proposal-content {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  min-height: 700px;
  padding: 16px;
}

.proposal-sidebar{
background:#fff;
border:1px solid #dbe3ef;
border-radius:12px;
padding:20px;
box-shadow:0 8px 24px rgba(0,0,0,.06);
}

/* Reviewer Panel Styles */

.review-card h3 {
  font-size: 18px;
  color: #1f2937;
  margin-bottom: 4px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.form-group input {
  width: 100%;
  height: 42px;     
  padding: 0 12px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

.form-group select {
  padding: 10px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #2563eb;
}



.section-block {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}

.section-block h3 {
  font-size: 15px;
  margin-bottom: 10px;
  color: #1f2937;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.grid-2 span {
  font-size: 12px;
  color: #6b7280;
}

.grid-2 p {
  font-size: 14px;
  color: #111827;
  margin: 2px 0 0;
  font-weight: 500;
}

.proposal-table{
width:100%;
border-collapse:collapse;
margin-top:10px;
}

.proposal-table th{
background:#2563eb;
color:#fff;
padding:10px;
font-size:13px;
text-align:left;
}

.proposal-table td{
border:1px solid #e5e7eb;
padding:10px;
font-size:13px;
}

.document-box{
margin-top:10px;
padding:14px;
background:#f9fafb;
border:1px solid #e5e7eb;
border-radius:6px;
line-height:1.7;
font-size:14px;
white-space:pre-wrap;
}

/* ===========================
ATTACHMENT CARDS
=========================== */

.attachment-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px;
  margin-bottom: 14px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: .2s;
}

.attachment-card:hover {
  box-shadow: 0 3px 10px rgba(0,0,0,.08);
  transform: translateY(-1px);
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.attachment-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  background: #eff6ff;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
}

.attachment-details h4 {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.attachment-details p {
  font-size: 12px;
  color: #6b7280;
}

.attachment-buttons {
  display: flex;
  gap: 10px;
}

.view-btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: .2s;
}


.download-btn {
  background: #16a34a;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: .2s;
}

.download-btn:hover {
  background: #15803d;
}

@media (max-width: 768px) {
  .attachment-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .attachment-buttons {
    width: 100%;
  }

  .attachment-buttons button {
    flex: 1;
  }
}

/* ===========================
REVIEW PANEL
=========================== */

.review-card{
display:flex;
flex-direction:column;
gap:18px;
}

.review-section{
margin-bottom:22px;
background:#ffffff;
border:1px solid #e5e7eb;
border-radius:10px;
padding:16px;
}

.review-section h4{
font-size:15px;
font-weight:700;
color:#1e3a8a;
margin-bottom:12px;
padding-bottom:6px;
border-bottom:2px solid #e5e7eb;
}

.form-group{
display:flex;
flex-direction:column;
margin-bottom:14px;
}

.form-group label{
font-size:13px;
font-weight:600;
margin-bottom:6px;
color:#374151;
}

.form-group input,
.form-group select{
width:100%;
padding:10px;
border:1px solid #d1d5db;
border-radius:6px;
font-size:14px;
outline:none;
transition:.2s;
}

.form-group input:focus,
.form-group select:focus{
border-color:#2563eb;
}


/* ===========================
OVERALL
=========================== */

.review-section textarea{
width:100%;
padding:10px;
margin-top:10px;
border:1px solid #d1d5db;
border-radius:6px;
resize:vertical;
font-size:13px;
}

.review-section select{
width:100%;
padding:10px;
border:1px solid #d1d5db;
border-radius:6px;
font-size:14px;
}

.readonly-box{
  background:#f8fafc;
  border:1px solid #dbe3ee;
  border-radius:8px;
  padding:12px;
  min-height:90px;
  color:#334155;
  line-height:1.6;
  white-space:pre-wrap;
}


.review-actions{
  display:flex;
  gap:10px;
  margin-top:25px;
}

.edit-review-btn,
.done-btn{
  flex:1;
  padding:12px;
  border:none;
  border-radius:8px;
  font-size:14px;
  font-weight:600;
  cursor:pointer;
  transition:.2s;
}

.edit-review-btn{
  background:#f59e0b;
  color:#fff;
}

.edit-review-btn:hover{
  background:#d97706;
}

.done-btn{
  background:#16a34a;
  color:#fff;
}

.done-btn:hover{
  background:#15803d;
}



.budget-file{
display:flex;
align-items:center;
gap:10px;
padding:12px;
background:#fff;
border:1px solid #e5e7eb;
border-radius:10px;
}

.budget-file-icon{
width:42px;
height:42px;
display:flex;
align-items:center;
justify-content:center;
background:#eef4ff;
border-radius:8px;
font-size:22px;
flex-shrink:0;
}

.budget-file-info{
flex:1;
min-width:0;
}

.budget-file-info strong{
display:block;
font-size:13px;
font-weight:600;
color:#1e3a8a;
overflow:hidden;
text-overflow:ellipsis;
white-space:nowrap;
}

.budget-file-info small{
font-size:11px;
color:#64748b;
}

.budget-file-actions{
display:flex;
gap:6px;
flex-shrink:0;
}

.mini-btn{
width:34px;
height:34px;
padding:0;
display:flex;
align-items:center;
justify-content:center;
font-size:14px;
border-radius:8px;
}

.mini-btn.view-btn{
background:#2563eb;
color:#fff;
}

.mini-btn.download-btn{
background:#16a34a;
color:#fff;
}

.mini-btn:hover{
opacity:.9;
}

.released-card{
background:#dcfce7;
color:#166534;
font-weight:700;
padding:12px;
border-radius:8px;
text-align:center;
border:1px solid #22c55e;
}

.done-btn:disabled{
background:#94a3b8;
cursor:not-allowed;
}

/* ===========================
STATUS BADGES
=========================== */

.readonly-value{
  padding:12px;
  border-radius:8px;
  text-align:center;
  font-weight:700;
  font-size:14px;
}

.approve-card{
  background:#dcfce7;
  color:#15803d;
  border:1px solid #86efac;
}

.return-card{
  background:#fee2e2;
  color:#b91c1c;
  border:1px solid #fca5a5;
}

.released-card{
  background:#dbeafe;
  color:#1d4ed8;
  border:1px solid #93c5fd;
}

/* ===========================
INFO ROWS
=========================== */

.info-row{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:10px 0;
  border-bottom:1px solid #e5e7eb;
}

.info-row:last-child{
  border-bottom:none;
}

.info-row label{
  font-size:13px;
  font-weight:600;
  color:#6b7280;
}

.info-row p{
  font-size:14px;
  font-weight:600;
  color:#111827;
}
</style>