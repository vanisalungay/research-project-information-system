<template>
  <div class="page">

    <!-- Header -->
    <div class="header">
      <h2>Proposal Inbox</h2>
      <p>Manage all submitted research proposals.</p>
    </div>

    <!-- Search -->
    <div class="search-container">
      <input
        type="text"
        v-model="search"
        placeholder="Search proposal title or researcher..."
        class="search-input"
      />
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }} ({{ tab.count }})
      </button>
    </div>

    <!-- Proposal List -->
    <div class="proposal-list">

      <!-- Empty State -->
      <div
        v-if="filteredProposals.length === 0"
        class="empty-state"
      >
        <div class="empty-icon-wrapper">
          📄
        </div>

        <h3>No Proposals Yet</h3>

        <p>
          Submitted research proposals will appear here
          once proponents submit their proposals.
        </p>
      </div>

      <!-- Proposal Row -->
      <div
        v-for="proposal in filteredProposals"
        :key="proposal.id"
        class="proposal-row"
        @click="openProposal(proposal)"
      >        <!-- Left -->
        <div class="left">

          <div class="icon">
            📄
          </div>

          <div class="content">

            <div class="title-row">
              <h4>{{ proposal.title }}</h4>

              <span
                v-if="proposal.status === 'Pending Review'"
                class="status pending"
              >
                Pending
              </span>

              <span
                v-else-if="proposal.status === 'Under Review'"
                class="status review"
              >
                Under Review
              </span>

              <span
                v-else-if="proposal.status === 'Draft'"
                class="status draft"
              >
                Draft
              </span>

              <span
                v-else-if="proposal.status === 'Returned'"
                class="status returned"
              >
                Returned
              </span>

            </div>

            <div class="info-line">
              <span>{{ proposal.researcher }}</span>
              <span>•</span>
              <span>{{ proposal.department }}</span>
            </div>

            <small class="time">
              {{ proposal.date }}
            </small>

          </div>

        </div>

        <!-- Right -->
        <div class="arrow">
          →
        </div>

      </div>

    </div>

  </div>
</template>

<script>
export default {
  name: "ProposalInbox",

  data() {
    return {
      search: "",
      activeTab: "all",

      // Temporary data
      // Ilisi lang ni ug API response kung naa na ang backend
      proposals: [
      ],
    };
  },

  computed: {
    tabs() {
      return [
        {
          key: "all",
          label: "All",
          count: this.proposals.length,
        },
        {
          key: "pending",
          label: "Pending",
          count: this.proposals.filter(
            (p) => p.status === "Pending Review"
          ).length,
        },
        {
          key: "review",
          label: "Under Review",
          count: this.proposals.filter(
            (p) => p.status === "Under Review"
          ).length,
        },
        {
          key: "draft",
          label: "Draft",
          count: this.proposals.filter(
            (p) => p.status === "Draft"
          ).length,
        },
        {
          key: "returned",
          label: "Returned",
          count: this.proposals.filter(
            (p) => p.status === "Returned"
          ).length,
        },
      ];
    },

    filteredProposals() {
      let result = this.proposals;

      // Filter pinaagi sa status
      if (this.activeTab === "pending") {
        result = result.filter(
          (p) => p.status === "Pending Review"
        );
      }

      if (this.activeTab === "review") {
        result = result.filter(
          (p) => p.status === "Under Review"
        );
      }

      if (this.activeTab === "draft") {
        result = result.filter(
          (p) => p.status === "Draft"
        );
      }

      if (this.activeTab === "returned") {
        result = result.filter(
          (p) => p.status === "Returned"
        );
      }

      // Search proposal title or researcher
      if (this.search.trim() !== "") {
        const keyword = this.search.toLowerCase();

        result = result.filter(
          (p) =>
            p.title.toLowerCase().includes(keyword) ||
            p.researcher.toLowerCase().includes(keyword)
        );
      }

      return result;
    },
  },

  methods: {
    openProposal(proposal){
      switch(proposal.status){

        case "Pending Review":
          this.$router.push({
            name:"ProposalDetails",
            params:{ id:proposal.id }
          });
          break;

        case "Draft":
          this.$router.push({
            name:"ProposalReview",
            params:{ id:proposal.id }
          });
          break;

        case "Returned":
        case "Rejected":
          this.$router.push({
            name:"ReviewResultViewer",
            params:{ id:proposal.id }
          });
          break;
      }
    }
  }
};
</script>

<style scoped>

.page{
    width:500%;
    max-width:135%;
    padding:24px;
    font-family:Arial, sans-serif;
    background:#f7f8fc;
    box-sizing:border-box;
}

/* Header */

.header h2{
    margin:0;
    font-size:28px;
    color:#111827;
}

.header p{
    margin-top:6px;
    color:#6b7280;
}

/* Search */

.search-container{
    margin:22px 0 14px;
}

.search-input{
    width:100%;
    padding:11px 16px;
    border:1px solid #d1d5db;
    border-radius:10px;
    font-size:14px;
    outline:none;
    box-sizing:border-box;
}

.search-input:focus{
    border-color:#4f46e5;
}

/* Tabs */

.tabs{
    display:flex;
    gap:10px;
    margin-bottom:18px;
}

.tabs button{
    border:none;
    background:#e5e7eb;
    padding:8px 16px;
    border-radius:20px;
    cursor:pointer;
    font-size:13px;
    transition:.2s;
}

.tabs button:hover{
    background:#d1d5db;
}

.tabs .active{
    background:#4f46e5;
    color:white;
}

/* List */

.proposal-list{
    background:#fff;
    border-radius:12px;
    border:1px solid #e5e7eb;
    overflow:hidden;
}

/* Proposal Row */

.proposal-row{
    display:flex;
    justify-content:space-between;
    align-items:center;
    padding:12px 18px;
    border-bottom:1px solid #ececec;
    cursor:pointer;
    transition:.2s;
}

.proposal-row:last-child{
    border-bottom:none;
}

.proposal-row:hover{
    background:#f9fafb;
}

/* Left */

.left{
    display:flex;
    align-items:center;
    gap:12px;
    flex:1;
}

.icon{
    width:38px;
    height:38px;
    display:flex;
    justify-content:center;
    align-items:center;
    border-radius:50%;
    background:#eef2ff;
    font-size:18px;
    flex-shrink:0;
}

/* Content */

.content{
    flex:1;
}

.title-row{
    display:flex;
    align-items:center;
    gap:10px;
}

.title-row h4{
    margin:0;
    font-size:15px;
    color:#1f2937;
}

.info-line{
    display:flex;
    gap:6px;
    margin-top:4px;
    font-size:13px;
    color:#6b7280;
}

.time{
    display:block;
    margin-top:4px;
    color:#9ca3af;
    font-size:12px;
}

/* Status */

.status{
    padding:3px 10px;
    border-radius:20px;
    font-size:11px;
    font-weight:600;
}

.pending{
    background:#fef3c7;
    color:#92400e;
}

.review{
  background:#ede9fe;
  color:#6d28d9;
}

.status.draft{
  background:#dbeafe;
  color:#1d4ed8;
}

.returned{
    background:#fee2e2;
    color:#b91c1c;
}

/* Arrow */

.arrow{
    font-size:20px;
    color:#9ca3af;
}

/* Empty State */

.empty-state{
    display:flex;
    flex-direction:column;
    justify-content:center;
    align-items:center;
    text-align:center;
    padding:80px 20px;
}

.empty-icon-wrapper{
    width:90px;
    height:90px;
    border-radius:50%;
    background:#f3f4f6;
    display:flex;
    justify-content:center;
    align-items:center;
    font-size:42px;
    margin-bottom:20px;
}

.empty-state h3{
    margin:0;
    color:#374151;
    font-size:22px;
}

.empty-state p{
    margin-top:10px;
    color:#6b7280;
    line-height:1.6;
    max-width:420px;
}

</style>