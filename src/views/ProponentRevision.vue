<template>
  <div class="revision-wrapper">
    <!-- PAGE HEADER -->
    <div class="revision-header">
      <h2>Revision Dashboard</h2>

      <div class="revision-count">{{ revisions.length }} Proposals Need Revision</div>
    </div>

    <!-- REVISION CARDS -->
    <div v-for="revision in revisions" :key="revision.id" class="revision-card">
      <!-- CARD HEADER -->
      <div class="card-header">
        <div>
          <h3>{{ revision.title }}</h3>
          <span class="revision-tag">Revision Required</span>
          <p class="proposal-id">ID: {{ revision.code }}</p>
        </div>

        <div class="deadline">
          <p>Revision Deadline</p>
          <span>{{ revision.deadline }}</span>
        </div>
      </div>

      <!-- REVIEWER COMMENTS -->
      <div class="comments-box">
        <strong>Reviewer Comments:</strong>
        <p>{{ revision.comment }}</p>
      </div>

      <!-- ACTION BUTTONS -->
      <div class="card-actions">
        <button class="submit-btn" @click="submitRevision(revision.id)">✏️ Submit Revision</button>

        <button class="details-btn" @click="viewDetails(revision.id)">👁 View Details</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

type Revision = {
  id: number
  title: string
  code: string
  comment: string
  deadline: string
}

const revisions = ref<Revision[]>([
  {
    id: 1,
    title: 'Community Development Program 2024',
    code: '2024-PUNLADAGAT-COMDEV-001',
    comment:
      'Please clarify the methodology section and provide more details on the community engagement strategy.',
    deadline: '5 days left',
  },
  {
    id: 2,
    title: 'Healthcare Facility Upgrade',
    code: '2024-TANGLAW-HEALTH-002',
    comment: 'Additional literature review required. Please expand on the sampling methodology.',
    deadline: '5 days left',
  },
])

const submitRevision = () => {
  router.push('/submit-revision')
}

const viewDetails = (id: number) => {
  router.push(`proponent-prop-details`)
}
</script>

<style scoped>
.revision-wrapper {
  padding: 24px;
}

/* HEADER */
.revision-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.revision-header h2 {
  font-size: 22px;
  font-weight: bold;
  color: #1f1f3d;
}

.revision-count {
  background: #fff4cc;
  color: #000000;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: bold;
}

/* CARD */
.revision-card {
  background: #fffbea;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ffe28a;
}

/* CARD HEADER */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1f1f3d;
}

.revision-tag {
  display: inline-block;
  background: #fff0b3;
  color: #8a6d00;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  margin-left: 6px;
}

.proposal-id {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

/* DEADLINE */
.deadline {
  text-align: right;
  font-size: 12px;
  color: #666;
}

.deadline span {
  display: block;
  color: #ff9800;
  font-weight: bold;
}

/* COMMENTS */
.comments-box {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-top: 14px;
  font-size: 14px;
  color: #333;
}

/* ACTIONS */
.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.submit-btn {
  background: #ffd803;
  border: none;
  padding: 8px 14px;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
  color: #1f1f3d;
}

.details-btn {
  background: white;
  border: 1px solid #ddd;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
}

.submit-btn:hover,
.details-btn:hover {
  opacity: 0.85;
}
</style>
