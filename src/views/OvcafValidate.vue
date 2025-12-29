<template>
  <div class="page-wrapper">
    <!-- HEADER TITLE -->
    <div class="header-banner">
      <h1>PROJECT DETAILS</h1>
    </div>

    <!-- ACTION BUTTONS -->
    <div class="action-buttons">
      <button class="btn-yellow" @click="approveEndorse">Approve & Endorse to RII</button>
      <button class="btn-yellow" @click="returnProponent">Return to Proponent</button>
      <button class="btn-yellow reject" @click="showRejectModal = true">Reject Request</button>
    </div>

    <!-- SCROLLABLE CONTENT AREA -->
    <div class="content-area">
      <!-- BODY CONTENT -->
      <div class="proposal-body">
        <!-- === CONTENT START (your content pasted exactly) === -->

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
          <strong>Implementing College (Department):</strong> College of Engineering – Department of
          Environmental Engineering
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
          To develop a low-cost, community-operated water filtration system using locally available
          materials that improves access to clean drinking water in rural barangays.
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
          filtration system that provides clean and safe drinking water to underserved households in
          Naawan, Misamis Oriental...
        </p>

        <!-- === CONTENT END === -->
      </div>

      <button class="download-btn" @click="downloadProject">DOWNLOAD PROJECT</button>
    </div>

    <!-- APPROVE CONFIRMATION MODAL -->
    <div class="overlay" v-if="showApproveConfirm">
      <div class="confirm-box">
        <p class="confirm-text">
          Are you sure you want to approve this fund request and endorse it to RII for release?
        </p>

        <div class="confirm-actions">
          <button class="btn-confirm" @click="approveConfirmed">CONFIRM</button>
          <button class="btn-cancel" @click="showApproveConfirm = false">CANCEL</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MESSAGE MODAL -->
    <div class="overlay" v-if="showSuccess">
      <div class="success-box">Fund request approved and endorsed to RII.</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const showRejectModal = ref(false)
const rejectReason = ref('')

const showApproveConfirm = ref(false)
const showSuccess = ref(false)

const approveEndorse = () => {
  showApproveConfirm.value = true
}

const approveConfirmed = () => {
  showApproveConfirm.value = false
  showSuccess.value = true
  setTimeout(() => {
    showSuccess.value = false
  }, 3000)
}

const returnProponent = () => {
  alert('✔ Returned to proponent successfully')
}

const confirmReject = () => {
  if (!rejectReason.value.trim()) {
    alert('⚠ Please provide a rejection reason')
    return
  }
  alert('❌ Request rejected.\nReason: ' + rejectReason.value)
  showRejectModal.value = false
  rejectReason.value = ''
}

const downloadProject = () => {
  const link = document.createElement('a')
  link.href = '/files/project-proposal.pdf'
  link.download = 'project-proposal.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.page-wrapper {
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 135%;
}

.header-banner {
  background: #1c2145;
  padding: 20px 30px;
  color: white;
  font-weight: bold;
}

.header-banner h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
}

.action-buttons {
  width: 100%;
  padding: 20px 30px;
  background: white;
  border-bottom: 2px solid #e6e6e6;
  display: flex;
  justify-content: center;
  gap: 20px;
  box-sizing: border-box;
}

.content-area {
  padding: 20px 40px;
  overflow-y: auto;
}

.btn-yellow {
  background: #ffd400;
  border: none;
  padding: 15px 25px;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
  font-size: 15px;
}

.reject {
  background: #ffd400;
}

.proposal-body h4 {
  margin-top: 35px;
  margin-bottom: 10px;
  font-weight: bold;
  font-size: 18px;
}

.proposal-body p,
.proposal-body ul {
  line-height: 1.6;
  font-size: 15px;
}

.two-column {
  display: flex;
  justify-content: space-between;
  gap: 40px;
}

.two-column > div {
  width: 48%;
}

.impl-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
  font-size: 15px;
}

.impl-table th,
.impl-table td {
  border: 1px solid black;
  padding: 8px;
}

.download-btn {
  margin-left: auto;
  display: block;
  background: #ffd800;
  border: none;
  padding: 12px 20px;
  border-radius: 10px;
  cursor: pointer;
}

/* ======== MODAL STYLES BELOW (UNCHANGED CONTENT ABOVE) ======== */

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.confirm-box {
  background: #3c376b;
  width: 600px;
  padding: 35px 30px;
  border-radius: 40px;
  color: #ffd700;
  text-align: center;
}

.confirm-text {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 35px;
  line-height: 1.4;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 60px;
}

.btn-confirm {
  background: #ffd700;
  color: black;
  border: none;
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}

.btn-cancel {
  background: #ff0000;
  color: white;
  border: none;
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}

.success-box {
  background: #3c376b;
  color: #ffd700;
  padding: 15px 40px;
  border-radius: 40px;
  font-weight: bold;
  font-size: 18px;
}
</style>
