<template>
  <div class="submitted-page">
    <!-- PAGE TITLE -->
    <div class="page-header">
      <h2>Submitted Proposals</h2>
      <p>Review and manage all proposal submissions</p>
    </div>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <input
        type="text"
        placeholder="Search by title, proponent, or category..."
      />

      <select>
        <option>All Status</option>
        <option>Pending</option>
        <option>Endorsed</option>
        <option>Under REC Evaluation</option>
        <option>Returned for Revision</option>
      </select>

      <button class="filter-btn">More Filters</button>
    </div>

    <!-- TABLE -->
    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Proponent</th>
            <th>Date Submitted</th>
            <th>Status</th>
            <th>REC Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <!-- Proposal 1 -->
          <tr>
            <td>
              <strong>Community Development Program 2024</strong><br />
              <small>Kalikasan Program</small>
            </td>

            <td>Dr. Allen Shippy</td>
            <td>2024-12-10</td>

            <td>
              <span class="status pending">Pending</span>
            </td>

            <td>
              <span class="status waiting">Pending Endorsement</span>
            </td>

            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <!-- Proposal 2 -->
          <tr>
            <td>
              <strong>Educational Infrastructure Project</strong><br />
              <small>Tanglaw Program</small>
            </td>

            <td>Dr. Cat Moon</td>
            <td>2024-12-08</td>

            <td>
              <span class="status endorsed">Endorsed</span>
            </td>

            <td>
              <span class="status endorsed-rec">Endorsed to REC</span>
            </td>

            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <!-- Proposal 3 -->
          <tr>
            <td>
              <strong>Healthcare Facility Upgrade</strong><br />
              <small>Kalikasan Program</small>
            </td>

            <td>Dr. Meow Chan</td>
            <td>2024-12-05</td>

            <td>
              <span class="status revision">For Revision</span>
            </td>

            <td>
              <span class="status revision">Returned for Revision</span>
            </td>

            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <!-- Proposal 4 -->
          <tr>
            <td>
              <strong>Technology Innovation Initiative</strong><br />
              <small>Tanglaw Program</small>
            </td>

            <td>Dr. Blair Gwen</td>
            <td>2024-12-03</td>

            <td>
              <span class="status reviewing">Under REC Evaluation</span>
            </td>

            <td>
              <span class="status reviewing">Under REC Evaluation</span>
            </td>

            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToReview = () => {
  router.push('/review-prop')
}
</script>

<style scoped>
.submitted-page {
  padding: 25px;
}

.page-header h2 {
  margin: 0;
}

.page-header p {
  color: #666;
  margin-top: 5px;
}

/* FILTER BAR */

.filter-bar {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}

.filter-bar input {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
}

.filter-bar select {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
}

.filter-btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
}

/* TABLE */

.table-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: #2f2b57;
  color: white;
  padding: 12px;
  text-align: left;
}

td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

small {
  color: #777;
}

/* STATUS */

.status {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.pending {
  background: #FEF3C7;
  color: #92400E;
}

.waiting {
  background: #F3F4F6;
  color: #4B5563;
}

.endorsed {
  background: #DCFCE7;
  color: #166534;
}

.endorsed-rec {
  background: #DBEAFE;
  color: #1D4ED8;
}

.reviewing {
  background: #E0E7FF;
  color: #4338CA;
}

.revision {
  background: #FEE2E2;
  color: #B91C1C;
}

/* BUTTON */

.review-btn {
  padding: 7px 16px;
  border: none;
  border-radius: 6px;
  background: #3B82F6;
  color: white;
  cursor: pointer;
  transition: .2s;
}

.review-btn:hover {
  background: #2563EB;
}
</style>