<template>
  <div class="submitted-page">
    <!-- PAGE TITLE -->
    <div class="page-header">
      <h2>Submitted Proposals</h2>
      <p>Review and manage all proposal submissions</p>
    </div>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <input type="text" placeholder="Search by title, proponent, or category..." />

      <select>
        <option>All Status</option>
        <option>Pending</option>
        <option>Revision</option>
        <option>Approved</option>
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
            <th>Assigned Reviewer</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr>
            <td>
              <strong>Community Development Program 2024</strong>
              <br />
              <small>Kalikasan Program</small>
            </td>
            <td>Dr. Allen Shippy</td>
            <td>2024-12-10</td>
            <td><span class="status pending">Pending</span></td>
            <td>
              <button class="assign-btn" @click="goToAssignReviewer">Assign</button>
            </td>
            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <tr>
            <td>
              <strong>Educational Infrastructure Project</strong>
              <br />
              <small>Tanglaw Program</small>
            </td>
            <td>Dr. Cat Moon</td>
            <td>2024-12-08</td>
            <td><span class="status pending">Endorsed</span></td>
            <td>Dr. Jane Smith</td>
            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <tr>
            <td>
              <strong>Healthcare Facility Upgrade</strong>
              <br />
              <small>Kalikasan Program</small>
            </td>
            <td>Dr. Meow Chan</td>
            <td>2024-12-05</td>
            <td><span class="status revision">Pending</span></td>
            <td>Dr. Sarah Lee</td>
            <td>
              <button class="review-btn" @click="goToReview">Review</button>
            </td>
          </tr>

          <tr>
            <td>
              <strong>Technology Innovation Initiative</strong>
              <br />
              <small>Tanglaw Program</small>
            </td>
            <td>Dr. Blair Gwen</td>
            <td>2024-12-03</td>
            <td><span class="status pending">Endorsed</span></td>
            <td>
              <button class="assign-btn" @click="goToAssignReviewer">Assign</button>
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

const goToAssignReviewer = () => {
  router.push('/assign-reviewer')
}
</script>

<style>
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
  text-align: left;
  padding: 12px;
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
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.pending {
  background: #ffe082;
}

.revision {
  background: #90caf9;
}
/* BUTTONS */
.assign-btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
}

.review-btn {
  padding: 6px 14px;
  border-radius: 6px;
  background: #6cb4ff;
  border: none;
  color: white;
  cursor: pointer;
}
</style>
