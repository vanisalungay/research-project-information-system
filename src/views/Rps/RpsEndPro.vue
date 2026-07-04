<template>
  <div class="proposal-page">

    <header class="proposal-header">
      <div class="header">
        <div>
          <h2>Review Summary</h2>
        </div>

        <button class="back-btn" @click="goBack">
          ← Back
        </button>
      </div>

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
          <button class="btn return" @click="$router.push('rps-return4rev')">
            ⟳ Return for Revision
          </button>

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
          <h2>Proposal Evaluation</h2>

          <div class="review-section">
            <h4>Reviewer Information</h4>

            <div class="form-group">
              <label>Reviewer Name</label>
              <input
                type="text"
                v-model="reviewer.name"
                placeholder="Enter your full name"
              >
            </div>

            <div class="form-group">
              <label>Position</label>
              <select v-model="reviewer.position">
                <option>RII Staff</option>
                <option>RII Director</option>
              </select>
            </div>
          </div>

          <div class="review-section">
            <h4>Evaluation Criteria</h4>

            <div class="criteria-card">
              <label>Relevance or Significance</label>

            <div class="readonly-rating">
              {{ getRatingLabel(evaluation.relevance.rating) }}
            </div>

            <div class="criteria-score">
              Score:
              <strong>{{ evaluation.relevance.rating || 0 }}</strong> /25
            </div>

              <div class="review-section">
                <h4>Reviewer Comments</h4>

                <div class="readonly-box">
                  {{ evaluation.relevance.comment || "No reviewer comments provided." }}
                </div>
              </div>
            </div>

            <div class="criteria-card">
              <label>Technical or Scientific Merit</label>

            <div class="readonly-rating">
              {{ getRatingLabel(evaluation.technical.rating) }}
            </div>

            <div class="criteria-score">
              Score:
              <strong>{{ evaluation.technical.rating || 0 }}</strong> /25
            </div>

            <div class="review-section">
                <h4>Reviewer Comments</h4>

                <div class="readonly-box">
                  {{ evaluation.technical.comment || "No reviewer comments provided." }}
                </div>
              </div>
            </div>

            <div class="criteria-card">
              <label>Budget Appropriateness</label>

            <div class="readonly-rating">
              {{ getRatingLabel(evaluation.budget.rating) }}
            </div>

            <div class="criteria-score">
              Score:
              <strong>{{ evaluation.budget.rating || 0 }}</strong> /25
            </div>

              <div class="review-section">
                <h4>Reviewer Comments</h4>

                <div class="readonly-box">
                  {{ evaluation.budget.comment || "No reviewer comments provided." }}
                </div>
              </div>
            </div>
                        
            <div class="criteria-card">
              <label>Competence of Proponent</label>

            <div class="readonly-rating">
              {{ getRatingLabel(evaluation.competence.rating) }}
            </div>

            <div class="criteria-score">
              Score:
              <strong>{{ evaluation.competence.rating || 0 }}</strong> /25
            </div>

              <div class="review-section">
                <h4>Reviewer Comments</h4>

                <div class="readonly-box">
                  {{ evaluation.competence.comment || "No reviewer comments provided." }}
                </div>
              </div>
            </div>
          </div>

          <div class="review-section">
            <h4>Overall Recommendation</h4>

            <div
              class="readonly-value"
              :class="evaluation.recommendation === 'Approve' ? 'approve-card' : 'return-card'"
            >
              {{ evaluation.recommendation === "Approve"
                  ? "Approved Proposal"
                  : "Returned for Revision" }}
            </div>
          </div>

          <div class="review-section">
            <h4>Reviewer Score</h4>

            <div class="score-card">
              <div class="score-circle">
                {{ reviewerPercentage }}%
              </div>

              <div class="score-info">
                <h3>{{ totalScore }}/100</h3>
                <p :class="isPassing ? 'pass-text' : 'fail-text'">
                  {{ isPassing ? 'Eligible for Approval' : 'Below Passing Score' }}
                </p>
              </div>
            </div>
          </div>

          <div class="review-section">
            <h4>Final Remarks</h4>
            <div class="review-section">
              <h4>Final Remarks</h4>

              <div class="readonly-box">
                {{ evaluation.finalRemarks || "No final remarks provided." }}
              </div>
            </div>
          </div>
          <hr class="review-divider">
          <div class="review-actions">
            <button class="edit-review-btn" @click="editReview">
              Edit Review
            </button>

            <button class="done-btn" @click="backToEndorsed">
              Done
            </button>
          </div>
        </div>
      </aside>

    </div>

  </div>
</template>

<script>
export default {
  name: "RpsProposalDetails",

  data() {
    return {
      reviewer: {
        name: "",
        position: "RII Staff",
        certified: false
      },
      reviewStarted: false,
      review: {
        status: "Pending Review"
      },
      evaluation: {
        relevance:{rating:"",comment:""},
        technical:{rating:"",comment:""},
        budget:{rating:"",comment:""},
        competence:{rating:"",comment:""},
        recommendation: "",
        overallComment:"",
        finalRemarks: ""
      },

      proposal: {
      researcher: "",
      department: "",
      college: "",
      projectTitle: "",
      programTitle: "",

      attachments: []
      }
    };
  },

  methods: {
    goToReview() {
      this.$router.push({
        name: "ProposalReview",
        params: {
          id: this.$route.params.id
        }
      });
    },

    getRatingLabel(score){
      const ratings={
        25:"Excellent",
        20:"Very Good",
        15:"Good",
        10:"Fair",
        5:"Poor",
        0:"Not Acceptable"
      }

      return `${ratings[score] || "Not Rated"} (${score || 0})`
    },

    goBack(){
      this.$router.push("riiendorsed-prop")
    },

    downloadProposal() {
      alert("Download Proposal PDF");
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

    saveDraft() {
    alert("Draft successfully saved.");
    },

    submitReview() {
    if (!this.evaluation.recommendation) {
    alert("Please select an Overall Recommendation.");
    return;
    }

    if (
     this.evaluation.recommendation == "Return" &&
     !this.evaluation.finalRemarks.trim()
    ) {
    alert("Final Remarks are required.");
    return;
    }

    if (
    this.reviewerPercentage < 80 &&
    this.evaluation.recommendation == "Approve"
     ) {
    alert("Proposal cannot be approved because the score is below 80%.");
    return;
    }

    const confirmSubmit = confirm(
    "Are you sure you want to submit this review?"
    );

    if (!confirmSubmit) {
    return;
    }

    alert("Review submitted successfully.");
    },


  editReview(){
    localStorage.setItem(
      "reviewProposal",
      JSON.stringify(this.proposal)
    )
    this.$router.push("proposal/:id/review")
  },

  backToEndorsed(){
    this.$router.push("/riiendorsed-prop")
  }

},
  mounted(){
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

.header-title {
  text-align: center;
}

.header-title h2 {
  font-size: 20px;
  color: #1f2937;
}

.header-title span {
  font-size: 13px;
  color: #6b7280;
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

.review-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

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

.required {
  color: #dc2626;
}

.checkbox-group {
  flex-direction: row;
  align-items: flex-start;
  gap: 8px;
}

.checkbox-group input {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  margin-top: 3px;
}

.checkbox-group label {
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
  flex: 1;
}

.begin-btn {
  padding: 11px;
  border: none;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: .2s;
}

.begin-btn:hover {
  background: #1d4ed8;
}

.review-note {
  padding: 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: #1e3a8a;
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

.attachment-box{
margin-top:10px;
padding:14px;
border:2px dashed #cbd5e1;
border-radius:6px;
background:#f8fafc;
font-size:14px;
color:#2563eb;
font-weight:600;
cursor:pointer;
}

.attachment-box:hover{
background:#eef6ff;
}

.section-block h4{
margin:14px 0 8px;
font-size:14px;
color:#374151;
}

.review-started{
display:flex;
flex-direction:column;
gap:14px;
}

.progress-card{
padding:10px;
border:1px solid #e5e7eb;
border-radius:6px;
background:#fafafa;
}

.progress-card p{
font-size:13px;
font-weight:600;
margin-bottom:4px;
}

.progress-card span{
font-size:14px;
color:#374151;
}

.save-btn,
.forward-btn,
.return-btn{
padding:11px;
border:none;
border-radius:6px;
cursor:pointer;
font-weight:600;
}

.save-btn{
background:#2563eb;
color:white;
}

.forward-btn{
background:#16a34a;
color:white;
}

.return-btn{
background:#dc2626;
color:white;
}

.header-actions{
display:flex;
align-items:center;
gap:10px;
}

.download-btn{
background:#16a34a;
color:white;
border:none;
padding:10px 18px;
border-radius:6px;
cursor:pointer;
font-weight:600;
transition:.2s;
}

.download-btn:hover{
background:#15803d;
}

/* ===========================
ATTACHMENTS
=========================== */

.attachment-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  margin-bottom: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: .2s;
}

.attachment-box:hover {
  background: #f3f4f6;
}

.attachment-box span {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.attachment-actions {
  display: flex;
  gap: 10px;
}

.attachment-actions button {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: .2s;
}

.attachment-actions button:first-child {
  background: #2563eb;
  color: white;
}

.attachment-actions button:first-child:hover {
  background: #1d4ed8;
}

.attachment-actions button:last-child {
  background: #16a34a;
  color: white;
}

.attachment-actions button:last-child:hover {
  background: #15803d;
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

.view-btn:hover {
  background: #1d4ed8;
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
}

.review-section{
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
CRITERIA
=========================== */

.criteria-card{
background:#f8fafc;
border:1px solid #dbeafe;
border-radius:10px;
padding:14px;
margin-bottom:14px;
transition:.2s;
}

.criteria-card:hover{
border-color:#3b82f6;
box-shadow:0 0 8px rgba(59,130,246,.15);
}

.criteria-card:first-child{
margin-top:0;
}

.criteria-card label{
font-weight:700;
font-size:14px;
color:#111827;
display:block;
margin-bottom:8px;
}


.criteria-card select{
width:100%;
padding:10px;
border-radius:8px;
border:1px solid #cbd5e1;
margin-bottom:10px;
background:white;
}

.criteria-card textarea{
width:100%;
min-height:90px;
padding:12px 14px;
border:1px solid #cbd5e1;
border-radius:8px;
background:#fff;
resize:vertical;

font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
font-size:14px;
line-height:1.6;
color:#1f2937;

transition:.2s;
}

.criteria-card textarea::placeholder{
color:#9ca3af;
font-size:13px;
font-style:italic;
}

.criteria-card textarea:focus{
outline:none;
border-color:#2563eb;
box-shadow:0 0 0 3px rgba(37,99,235,.15);
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

/* ===========================
BUTTONS
=========================== */

.review-buttons{
display:flex;
flex-direction:column;
gap:10px;
margin-top:18px;
}

.save-btn{
background:#2563eb;
color:#fff;
border:none;
padding:12px;
border-radius:8px;
cursor:pointer;
font-weight:600;
}

.save-btn:hover{
background:#1d4ed8;
}

.submit-btn{
background:#16a34a;
color:#fff;
border:none;
padding:12px;
border-radius:8px;
cursor:pointer;
font-weight:600;
}

.submit-btn:hover{
background:#15803d;
}

.recommendation-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 12px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  cursor: pointer;
  background: #f8fafc;
  transition: 0.2s;
}

.radio-option:hover {
  background: #eff6ff;
}

.radio-option input {
  transform: scale(1.2);
}

.radio-option span {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.score-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 10px;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #2563eb;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.score-info h3 {
  font-size: 24px;
  margin-bottom: 6px;
}

.pass-text {
  color: #16a34a;
  font-weight: 600;
}

.fail-text {
  color: #dc2626;
  font-weight: 600;
}

.final-remarks {
  width: 100%;
  padding: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  min-height: 140px;
  transition: 0.2s;
}

.final-remarks:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.final-remarks::placeholder {
  color: #9ca3af;
  font-style: italic;
}

.readonly-rating{
  background:#f8fafc;
  border:1px solid #dbe3ee;
  border-radius:8px;
  padding:10px 14px;
  font-weight:600;
  color:#334155;
  margin-bottom:10px;
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
  margin-top:25px;
  display:flex;
  justify-content:flex-end;
}

.edit-review-btn{
  display:flex;
  align-items:center;
  justify-content:center;
  gap:8px;
  width:100%;
  padding:12px 18px;
  border:none;
  border-radius:10px;
  background:#2563eb;
  color:#fff;
  font-size:14px;
  font-weight:600;
  cursor:pointer;
  transition:.25s ease;
  box-shadow:0 3px 10px rgba(37,99,235,.25);
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
</style>