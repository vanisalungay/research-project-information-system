<template>
  <div>
    <div class="layout">
      <!-- CONTENT -->
      <div>
        <!-- HEADER -->
        <div class="page-header">
          <div>
            <h2>Community Development Program 2024</h2>
            <span class="badge pending">Pending Release</span>
            <p class="proposal-id">Proposal ID: #6</p>
          </div>

          <div class="header-actions">
            <button class="outline" @click="showModal = true">See Detailed Proposal</button>
            <button class="outline" @click="downloadAll">Download All Documents</button>
          </div>
        </div>

        <div class="grid">
          <!-- LEFT COLUMN -->
          <div class="left">
            <!-- PROPOSAL INFO -->
            <section class="card">
              <h3>Proposal Information</h3>

              <div class="info-grid">
                <div>
                  <label>Category</label>
                  <p>Community Development</p>
                </div>

                <div>
                  <label>Proponent / Organization</label>
                  <p>Dr. Alien Shippy</p>
                </div>

                <div>
                  <label>Contact Person</label>
                  <p>Alien Shippy</p>
                </div>

                <div>
                  <label>Email</label>
                  <p>alienshippy@msunawan.edu.ph</p>
                </div>

                <div>
                  <label>Phone</label>
                  <p>+1 (555) 123-4567</p>
                </div>

                <div>
                  <label>Total Budget</label>
                  <p>₱150,000</p>
                </div>

                <div>
                  <label>Duration</label>
                  <p>12 months</p>
                </div>

                <div>
                  <label>Submitted Date</label>
                  <p>2024-12-10</p>
                </div>
              </div>
            </section>

            <!-- TIMELINE -->
            <section class="card">
              <h3>Proposal History & Timeline</h3>

              <ul class="timeline">
                <li class="done">
                  <span class="dot"></span>
                  <div>
                    <strong>Submitted to System</strong>
                    <small>2024-12-10</small>
                  </div>
                </li>

                <li class="done">
                  <span class="dot"></span>
                  <div>
                    <strong>OVCRIGE Review</strong>
                    <small>2024-12-10</small>
                  </div>
                </li>

                <li class="done">
                  <span class="dot"></span>
                  <div>
                    <strong>OVCRIGE Endorsed</strong>
                    <small>2024-12-11</small>
                  </div>
                </li>

                <li class="current">
                  <span class="dot"></span>
                  <div>
                    <strong>RII Review</strong>
                    <small>In Progress</small>
                  </div>
                </li>
              </ul>
            </section>

            <!-- OVCRIGE DECISION -->
            <section class="card highlight">
              <div class="decision-header">
                <h3>OVCRIGE Decision</h3>
                <span class="badge endorsed">Endorsed</span>
              </div>

              <p class="decision-date">Decision Date: 2024-12-11</p>

              <label>Reviewer Score</label>
              <div class="score-bar">
                <div class="score-fill"></div>
              </div>

              <p class="recommendation">
                <strong>Recommendation:</strong> Strongly recommended for approval
              </p>
            </section>

            <!-- ACTIONS -->
            <section class="card">
              <h3>RII Actions</h3>

              <div class="actions">
                <button class="primary" @click="$router.push('rii-endorse')">
                  Endorse Proposal
                </button>
                <button class="secondary" @click="$router.push('fundrelease')">
                  Release Funds
                </button>
              </div>
            </section>
          </div>

          <!-- RIGHT COLUMN -->
          <aside class="right">
            <section class="card">
              <h3>Attachments</h3>

              <ul class="files">
                <li>
                  <span>📄 project-proposal.pdf</span>
                  <small>2.4 MB</small>
                </li>
                <li>
                  <span>📄 budget-breakdown.xlsx</span>
                  <small>156 KB</small>
                </li>
                <li>
                  <span>📄 timeline.pdf</span>
                  <small>89 KB</small>
                </li>
                <li>
                  <span>📄 endorsement-letter.pdf</span>
                  <small>120 KB</small>
                </li>
              </ul>
            </section>
          </aside>
        </div>
      </div>
    </div>
    <!-- DETAILED PROPOSAL MODAL -->
    <div v-if="showModal" class="modal-overlay">
      <div class="proposal-modal">
        <!-- HEADER -->
        <div class="proposal-header">
          <button class="download-btn" @click="downloadForm">Download Form</button>
          <button class="close-btn" @click="showModal = false">✕</button>
        </div>

        <!-- BODY -->
        <div class="proposal-body">
          <!-- 1. PROJECT PROFILE -->
          <h4>1. PROJECT PROFILE</h4>

          <div class="two-column">
            <div>
              <p>
                <strong>Program Title:</strong> Community Water and Filtration Enhancement Program
              </p>
              <p><strong>Project Title:</strong> Community-Based Water Filtration System</p>
              <p><strong>Project Leader/Sex:</strong> Engr. John Delos Reyes / Male</p>
              <p><strong>Project Duration:</strong> 5 months</p>
              <p><strong>Start Date:</strong> March 10, 2025</p>
              <p><strong>End Date:</strong> July 30, 2025</p>
            </div>

            <div>
              <p><strong>Address/Telephone/Fax/Email</strong></p>
              <p>Brgy. San Miguel, Naawan, Misamis Oriental</p>
              <p>Tel: (088) 556-1234</p>
              <p>Fax: (088) 556-5678</p>
              <p>Email: jdelosreyes@msunaawan.edu.ph</p>
            </div>
          </div>

          <p>
            <strong>Implementing College (Department):</strong>
            College of Engineering – Department of Environmental Engineering
          </p>

          <!-- 2. COOPERATING AGENCY/IES -->
          <h4>2. COOPERATING AGENCY/IES</h4>
          <p>
            <strong>A.</strong> Barangay San Miguel Local Government Unit<br />
            San Miguel, Naawan, Misamis Oriental
          </p>
          <p>
            <strong>B.</strong> Philippine Institute of Environmental Technology (PIET)<br />
            Quezon City, Metro Manila
          </p>

          <!-- 3. SITE(S) OF IMPLEMENTATION -->
          <h4>3. SITE(S) OF IMPLEMENTATION</h4>

          <table class="impl-table">
            <thead>
              <tr>
                <th>Impl. Site No.</th>
                <th>Country</th>
                <th>Region</th>
                <th>Province</th>
                <th>District</th>
                <th>Municipality</th>
                <th>Barangay</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>1.</td>
                <td>Philippines</td>
                <td>Region X</td>
                <td>Misamis Oriental</td>
                <td>District 2</td>
                <td>Naawan</td>
                <td>San Miguel</td>
              </tr>
              <tr>
                <td>2.</td>
                <td>Philippines</td>
                <td>Region X</td>
                <td>Misamis Oriental</td>
                <td>District 2</td>
                <td>Naawan</td>
                <td>Linangkayan</td>
              </tr>
            </tbody>
          </table>

          <!-- 4. TYPE OF RESEARCH -->
          <h4>4. TYPE OF RESEARCH</h4>
          <p>◯ Basic &nbsp;&nbsp;&nbsp; ⬤ Applied</p>

          <!-- 5. PRIORITY AGENDA -->
          <h4>5. PRIORITY AGENDA</h4>
          <p>◯ DAGAT PROGRAM – Commodity:</p>
          <p>◯ PUNLA PROGRAM – Priority Topic:</p>
          <p>⬤ KALIKASAN PROGRAM – Sector: Environmental Sustainability</p>
          <p>◯ NEGOSYO PROGRAM – Sector:</p>
          <p>◯ TANGLAW PROGRAM – Sector:</p>

          <!-- 6. INNOVATION GOALS -->
          <h4>6. INNOVATION GOALS</h4>
          <p>
            To develop a low-cost, community-operated water filtration system using locally
            available materials that improves access to clean drinking water in rural barangays.
          </p>

          <!-- 7. SECTOR RELEVANCE -->
          <h4>7. SECTOR RELEVANCE</h4>
          <p>Environment; Public Health; Community Development</p>

          <!-- 8. SDG -->
          <h4>8. APPLICABLE SUSTAINABLE DEVELOPMENT GOAL (SDG) ADDRESSED</h4>

          <!-- 9. EXECUTIVE SUMMARY -->
          <h4>9. EXECUTIVE SUMMARY</h4>
          <p>
            This project aims to design, build, and implement a sustainable community-based water
            filtration system that provides clean and safe drinking water to underserved households
            in Naawan, Misamis Oriental. Using affordable, locally-sourced materials and
            participatory community involvement, the project will install two functional filtration
            units capable of supplying potable water to approximately 240 households. Training on
            system maintenance and water safety monitoring will be conducted to ensure long-term
            sustainability.
          </p>

          <!-- 10. INTRODUCTION -->
          <h4>10. INTRODUCTION</h4>

          <p><strong>10.1 Rationale / Significance</strong></p>
          <p>
            Access to clean water remains a pressing concern in rural communities, where households
            rely on untreated groundwater or surface water. This increases vulnerability to
            waterborne diseases.
          </p>

          <p><strong>10.2 Scientific Basis / Theoretical Framework</strong></p>
          <p>
            The system integrates sedimentation, activated carbon filtration, and slow-sand
            filtration based on WHO water safety guidelines.
          </p>

          <p><strong>10.3 Objectives</strong></p>
          <p><strong>General Objective:</strong></p>
          <p>
            To provide a low-cost, sustainable filtration system to improve drinking water quality
            in target barangays.
          </p>

          <p><strong>Specific Objectives:</strong></p>
          <ul>
            <li>To design a multi-stage filtration system using locally available materials.</li>
            <li>To install the system in two pilot communities.</li>
            <li>To conduct water quality testing before and after installation.</li>
            <li>To train local residents on system operation and maintenance.</li>
          </ul>

          <!-- 11. REVIEW OF LITERATURE -->
          <h4>11. REVIEW OF LITERATURE</h4>
          <p>
            Previous studies highlight the effectiveness of slow-sand filtration and activated
            carbon in improving microbial and chemical water quality.
          </p>

          <!-- 12. METHODOLOGY -->
          <h4>12. METHODOLOGY</h4>
          <ul>
            <li>Site assessment and community consultation</li>
            <li>System design and material sourcing</li>
            <li>Construction and installation of filtration unit</li>
            <li>Training workshops for community operators</li>
            <li>Water testing and monitoring (monthly)</li>
            <li>Evaluation and documentation</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProposalDetails',
  data() {
    return {
      showModal: false,
      files: [
        'project-proposal.pdf',
        'budget-breakdown.xlsx',
        'timeline.pdf',
        'endorsement-letter.pdf',
      ],
    }
  },
  methods: {
    downloadFile(fileName) {
      const link = document.createElement('a')
      link.href = `/files/${fileName}`
      link.download = fileName
      link.href = '/files/proposal-form.pdf'
      link.download = 'proposal-form.pdf'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    downloadAll() {
      this.files.forEach((file, index) => {
        setTimeout(() => {
          this.downloadFile(file)
        }, index * 300) 
      })
    },

    downloadForm() {
      this.downloadFile('proposal-form.pdf')
    },
  },
}
</script>

<style scoped>
/* BASE */
* {
  box-sizing: border-box;
  font-family: 'Segoe UI', sans-serif;
}

.app {
  background: #f5f6fa;
  height: 100vh;
}

/* LAYOUT */
.layout {
  display: flex;
  min-height: calc(100vh - 72px);
  width: max-content;
}

/* CONTENT */
.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0px;
}

.proposal-id {
  color: black;
  font-size: 13px;
}

.header-actions button {
  margin-left: 10px;
}

/* GRID */
.grid {
  display: grid;
  grid-template-columns: 3fr 1.2fr;
  gap: 20px;
}

/* CARDS */
.card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.highlight {
  background: #f0fff6;
}

/* INFO GRID */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

label {
  font-size: 12px;
  color: gray;
}

/* TIMELINE */
.timeline {
  list-style: none;
  padding-left: 0;
}

.timeline li {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ccc;
  margin-top: 6px;
}

.done .dot {
  background: #2ecc71;
}

.current .dot {
  background: #f1c40f;
}

/* BADGES */
.badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.endorsed {
  background: #c8e6c9;
  color: #1b5e20;
}

/* DECISION */
.score-bar {
  background: #ddd;
  border-radius: 6px;
  height: 8px;
  margin: 8px 0 12px;
}

.score-fill {
  width: 85%;
  height: 100%;
  background: #2c274c;
  border-radius: 6px;
}

/* ACTIONS */
.actions {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.primary {
  background: #ffd803;
  border: none;
  padding: 10px;
  width: 100%;
  border-radius: 8px;
  font-weight: bold;
}

.secondary {
  background: #ffd803;
  border: none;
  padding: 10px;
  width: 100%;
  border-radius: 8px;
  font-weight: bold;
}

.danger {
  background: #918d8d;
  border: none;
  padding: 10px;
  border-radius: 8px;
  width: 100%;
  font-weight: bold;
}

/* FILES */
.files {
  list-style: none;
  padding-left: 0;
}

.files li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.outline {
  border: 1px solid #ddd;
  background: white;
  padding: 8px 12px;
  border-radius: 8px;
  color: #2c274c;
}

/* MODAL */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.proposal-modal {
  background: #fff;
  width: 85%;
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 6px;
  padding: 25px;
  font-size: 13px;
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.download-btn {
  background: #ffd803;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
}

.close-btn {
  border: none;
  background: none;
  font-size: 18px;
  cursor: pointer;
}

.proposal-body h4 {
  margin-top: 20px;
  font-size: 14px;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.impl-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.impl-table th,
.impl-table td {
  border: 1px solid #000;
  padding: 4px;
  text-align: center;
}
</style>
