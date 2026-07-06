<template>
  <div class="page">
    <!-- Header -->

    <div class="page-header">
      <div>
        <h2>Approved Proposals</h2>
        <p>Review and monitor approved proposals for OVCRIGE endorsement.</p>
      </div>
    </div>

    <!-- Search -->
    <div class="toolbar">
      <input
        v-model="search"
        type="text"
        class="search"
        placeholder="Search Proposal Code, Title or Project Leader..."
      />
    </div>

    <!-- Table -->
    <div class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>Proposal Code</th>
            <th>Proposal Title</th>
            <th>Project Leader</th>
            <th>Reviewer Score</th>
            <th>Status</th>
            <th width="180">Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="proposal in filteredProposals"
            :key="proposal.id"
          >
            <td>{{ proposal.code }}</td>
            <td>{{ proposal.title }}</td>
            <td>{{ proposal.leader }}</td>
            <td>{{ proposal.reviewerScore }}%</td>

            <td class="status-cell">
              <span
                class="status"
                :class="{
                  pending: proposal.status === 'Waiting for OVCRIGE Endorsement',
                  approved: proposal.status === 'Approved by OVCRIGE',
                  returned: proposal.status === 'Returned by OVCRIGE',
                  rejected: proposal.status === 'Rejected by OVCRIGE',
                  endorsed: proposal.status === 'Sent to OVCRIGE'
                }"
              >
                {{ proposal.status }}
              </span>
            </td>

            <td class="actions">
              <button
                class="view-btn"
                @click="viewProposal(proposal)"
              >
                View
              </button>

              <button
                class="endorse-btn"
                @click="endorseProposal(proposal)"
                :disabled="proposal.status !== 'Waiting for OVCRIGE Endorsement'"
              >
                Endorse
              </button>
            </td>
          </tr>

          <tr v-if="filteredProposals.length === 0">
            <td colspan="6" class="empty">
              No approved proposals found.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
export default {
  name: "RIIEndorsedProposals",

  data() {
    return {
      search: "",

      proposals: []
    }
  },

  computed: {
    filteredProposals() {
      const keyword = this.search.toLowerCase().trim()

      return this.proposals.filter(proposal =>
        proposal.code.toLowerCase().includes(keyword) ||
        proposal.title.toLowerCase().includes(keyword) ||
        proposal.leader.toLowerCase().includes(keyword)
      )
    }
  },

  methods: {
    viewProposal(proposal) {
      this.$router.push("/rps-endorse")
    },

    endorseProposal(proposal) {
      proposal.status = "Sent to OVCRIGE"
      alert("Proposal successfully endorsed to OVCRIGE.")
    }
  }
}
</script>

<style scoped>
.page{
  padding:25px;
  font-family:Segoe UI,sans-serif;
  background:#f5f7fb;
  min-height:100vh;
  width: 136%;
}
.page-header{
  margin-bottom:20px;
}

.page-header h2{
  margin:0;
  color:#1e293b;
  font-size:28px;
  font-weight:700;
}

.page-header p{
  margin-top:5px;
  color:#64748b;
  font-size:14px;
}

.toolbar{
  margin-bottom:20px;
}

.search{
  width:100%;
  padding:12px 15px;
  border:1px solid #d1d5db;
  border-radius:8px;
  font-size:14px;
  outline:none;
  transition:.2s;
  box-sizing:border-box;
}

.search:focus{
  border-color:#2563eb;
}

.table-container{
  background:#fff;
  border-radius:12px;
  overflow:hidden;
  box-shadow:0 4px 12px rgba(0,0,0,.08);
}

.table{
  width:100%;
  border-collapse:collapse;
}

.table thead tr{
  background:#1e40af !important;
}

.table thead th{
  background: #1e293b !important;
  color:#fff !important;
 
}

.table th{
  padding:15px;
  text-align:left;
  font-size:14px;
}

.table td{
  padding:15px;
  border-bottom:1px solid #e5e7eb;
  font-size:14px;
  color:#374151;
  vertical-align:middle;
}

.table tbody tr:hover{
  background:#f8fafc;
}

.view-btn{
  background:#3b82f6;
  color:#fff;
  border:none;
  padding:8px 14px;
  border-radius:6px;
  cursor:pointer;
  margin-right:8px;
}

.view-btn:hover{
  background:#2563eb;
}

.endorse-btn{
  background:#16a34a;
  color:#fff;
  border:none;
  padding:8px 14px;
  border-radius:6px;
  cursor:pointer;
}

.endorse-btn:hover{
  background:#15803d;
}

.empty{
  text-align:center;
  padding:30px;
  color:#94a3b8;
}

.status{
  padding:6px 12px;
  border-radius:20px;
  font-size:12px;
  font-weight:600;
}

.status-cell{
  vertical-align:middle;
}

.status{
  display:inline-block;
}

.pending{
  background:#FEF3C7;
  color:#92400E;
}

.endorsed{
  background:#DBEAFE;
  color:#1D4ED8;
}

.approved{
  background:#DCFCE7;
  color:#166534;
}

.returned{
  background:#FDE68A;
  color:#B45309;
}

.rejected{
  background:#FEE2E2;
  color:#B91C1C;
}

.actions{
  display:flex;
  gap:8px;
  align-items:center;
}

.view-btn,
.endorse-btn{
  border:none;
  border-radius:6px;
  padding:8px 14px;
  font-size:13px;
  font-weight:600;
  cursor:pointer;
  transition:.2s;
}

.view-btn{
  background:#3b82f6;
  color:white;
}

.view-btn:hover{
  background:#2563eb;
}

.endorse-btn{
  background:#16a34a;
  color:white;
}

.endorse-btn:hover{
  background:#15803d;
}

.endorse-btn:disabled{
  background:#cbd5e1;
  cursor:not-allowed;
}
</style>