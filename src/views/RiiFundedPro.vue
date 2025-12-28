<template>
  <div class="page">
    <!-- Back -->
    <button class="back-btn" @click="$router.push('funded-prop')">← Back to Proposal</button>

    <!-- Header -->
    <h2>Release Funds</h2>
    <p class="subtitle">Process fund release for approved proposal</p>

    <div class="layout">
      <!-- LEFT -->
      <div class="left">
        <!-- Proposal Summary -->
        <div class="card">
          <h3>Proposal Summary</h3>

          <div class="summary-grid">
            <div>
              <label>Title</label>
              <p>Educational Infrastructure Project</p>
            </div>

            <div>
              <label>Proponent</label>
              <p>Dr. Cat Moon</p>
            </div>

            <div>
              <label>Category</label>
              <p>Tanglaw Program</p>
            </div>

            <div>
              <label>Endorsed Date</label>
              <p>2024-12-09</p>
            </div>
          </div>

          <div class="budget-cards">
            <div class="budget blue">
              <label>Total Budget</label>
              <strong>₱180,000</strong>
            </div>

            <div class="budget green">
              <label>Released</label>
              <strong>₱180,000</strong>
            </div>

            <div class="budget orange">
              <label>Remaining</label>
              <strong>₱0</strong>
            </div>
          </div>
        </div>

        <!-- Fund Release Details (DISABLED) -->
        <div class="card disabled">
          <h3>Fund Release Details</h3>

          <label>Amount to Release (USD)</label>
          <input type="number" value="0.00" disabled />
          <small class="info">Maximum available: ₱0</small>

          <label>Release Date</label>
          <input type="date" disabled />

          <label>Remarks / Notes</label>
          <textarea disabled placeholder="No further releases allowed."></textarea>
        </div>
      </div>

      <!-- RIGHT -->
      <div class="right">
        <div class="card">
          <span class="status">Approved & Funded</span>

          <p class="note">
            This proposal has already received its full approved funding. No further fund releases
            are allowed.
          </p>
        </div>

        <div class="card">
          <h3>Release Summary</h3>

          <div class="summary-row">
            <span>Amount to Release</span>
            <strong class="highlight">₱0.00</strong>
          </div>

          <div class="summary-row">
            <span>Previously Released</span>
            <span>₱180,000</span>
          </div>

          <div class="summary-row">
            <span>Remaining After Release</span>
            <span>₱0</span>
          </div>

          <button class="release-btn" disabled>Fully Funded</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ReleaseFunds',
}
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: Arial, sans-serif;
  width: 500%;
  max-width: 135%;
}

.back-btn {
  background: none;
  border: none;
  color: #4f46e5;
  cursor: pointer;
  margin-bottom: 12px;
}

.subtitle {
  color: #666;
  margin-bottom: 20px;
}

.layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #eee;
}

.disabled {
  opacity: 0.6;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

label {
  font-size: 12px;
  color: #777;
}

input,
textarea {
  width: 100%;
  padding: 8px;
  margin: 6px 0 12px;
}

textarea {
  min-height: 80px;
}

.budget-cards {
  display: flex;
  gap: 12px;
}

.budget {
  flex: 1;
  padding: 12px;
  border-radius: 6px;
}

.blue {
  background: #eef2ff;
}

.green {
  background: #ecfdf5;
}

.orange {
  background: #fff7ed;
}

.status {
  display: inline-block;
  background: #dcfce7;
  color: #166534;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.note {
  font-size: 13px;
  color: #555;
  margin-top: 12px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
}

.highlight {
  color: #f59e0b;
}

.release-btn {
  width: 100%;
  margin-top: 16px;
  padding: 10px;
  background: #ccc;
  color: white;
  border: none;
  border-radius: 6px;
}
</style>
