<template>
  <div class="dashboard-wrapper">
    <!-- HEADER -->
    <div class="header">
      <div class="header-title">
        <div class="header-main">OVCAF DASHBOARD</div>
        <div class="header-sub">Office of the Vice Chancellor for Admin and Finance</div>
      </div>
    </div>

    <div class="search-container">
      <div class="search-box">
        <div class="search-icon">≡</div>
        <input type="text" placeholder="Search proposal file" />
        <div class="search-icon">🔍</div>
      </div>
    </div>

    <div class="stats-container">
      <div class="stat-card">
        <div class="stat-title">ENDORSED PROPOSALS</div>
        <div class="stat-number">0</div>
      </div>

      <div class="stat-card">
        <div class="stat-title">PENDING EVALUATIONS</div>
        <div class="stat-number">1</div>
      </div>

      <div class="stat-card">
        <div class="stat-title">COMPLETED REVIEWS</div>
        <div class="stat-number">0</div>
      </div>
    </div>

    <div class="table-title">APPROVED PROPOSALS REQUESTING FUNDS</div>

    <div class="table-wrapper">
      <div class="table-header">
        <div class="th">Proposal Title</div>
        <div class="th">Proponent Name</div>
        <div class="th">Amount Requested</div>
        <div class="th">Action</div>
      </div>

      <div class="table-row" v-for="(proposal, index) in proposals" :key="index">
        <div class="td">{{ proposal.title }}</div>
        <div class="td">{{ proposal.proponent }}</div>
        <div class="td">{{ proposal.amount }}</div>
        <div class="td">
          <div class="validate-btn" @click="$router.push('ovcaf-validate')">Validate</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OvcafDashboard',

  data() {
    return {
      proposals: [
        {
          title: 'Community Development Program 2024',
          proponent: 'Dr. Aljon Shipay',
          amount: '₱ 250,000.00',
        },
        {
          title: '',
          proponent: '',
          amount: '',
        },
        {
          title: '',
          proponent: '',
          amount: '',
        },
        {
          title: '',
          proponent: '',
          amount: '',
        },
        {
          title: '',
          proponent: '',
          amount: '',
        },
      ],
    }
  },
}
</script>

<style scoped>
.dashboard-wrapper {
  font-family: Arial, sans-serif;
  background: #ffffff;
  padding: 20px;
  width: 135%;
}

.header {
  background: #2c2b4f;
  padding: 30px 30px;
}

.header-main {
  font-size: 32px;
  font-weight: bold;
  color: #ffc800;
}

.header-sub {
  font-size: 14px;
  color: #ffffff;
  margin-top: 5px;
}

.search-container {
  display: flex;
  justify-content: flex-end;
  margin: 15px 0 30px 0;
}

.search-box {
  background: #eae7f2;
  display: flex;
  align-items: center;
  padding: 8px 15px;
  border-radius: 25px;
  width: 500px;
}

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  margin: 0 10px;
  width: 100%;
}

.search-icon {
  font-size: 14px;
}

.stats-container {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border: 2px solid #2c2b4f;
  padding: 25px;
  flex: 1;
  text-align: center;
}

.stat-title {
  font-weight: bold;
  font-size: 14px;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  margin-top: 10px;
}

.table-title {
  font-weight: bold;
  text-align: center;
  margin-bottom: 10px;
}

.table-wrapper {
  border: 3px solid #2c2b4f;
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 2fr 2fr 1.5fr 1fr;
}

.table-header {
  background: #2c2b4f;
  color: white;
  font-weight: bold;
}

.th,
.td {
  border: 2px solid #2c2b4f;
  padding: 10px;
}
.table-row {
  height: 50px;
}
.validate-btn {
  background: #2c2b4f;
  color: #ffc800;
  padding: 6px 12px;
  text-align: center;
  border-radius: 10px;
  font-size: 12px;
  cursor: pointer;
}
</style>
