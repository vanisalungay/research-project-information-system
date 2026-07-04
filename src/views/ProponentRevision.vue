<template>
  <div class="revision-wrapper">
    <!-- HEADER -->
    <div class="revision-header">
      <h2>Revision Dashboard</h2>

      <div class="revision-count">
        {{ revisions.length }} Proposals Need Revision
      </div>
    </div>

    <!-- LOADING -->
    <p v-if="loading">Loading...</p>

    <!-- ERROR -->
    <p v-if="error" class="error-text">
      {{ error }}
    </p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && revisions.length === 0">
      No revisions found.
    </p>

    <!-- CARDS -->
    <div
      v-for="revision in revisions"
      :key="revision.id"
      class="revision-card"
    >
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

      <div class="comments-box">
        <strong>Reviewer Comments:</strong>
        <p>{{ revision.comment }}</p>
      </div>

      <div class="card-actions">
        <button
          class="submit-btn"
          @click="submitRevision(revision.id)"
        >
          ✏️ Submit Revision
        </button>

        <button
          class="details-btn"
          @click="viewDetails(revision.id)"
        >
          👁 View Details
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

/**
 * EMPTY STATE (NO MOCK DATA)
 */
const revisions = ref<any[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

/**
 * FETCH FROM BACKEND
 * Expected:
 * GET /api/revisions
 */
async function fetchRevisions() {
  loading.value = true
  error.value = null

  try {
    const res = await axios.get('/api/revisions')

    // supports either:
    // 1. [ ... ]
    // 2. { data: [ ... ] }
    revisions.value = Array.isArray(res.data)
      ? res.data
      : res.data.data || []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load revisions.'
    revisions.value = []
  } finally {
    loading.value = false
  }
}

/**
 * ROUTING (backend-ready)
 */
const submitRevision = (id: number) => {
  router.push(`/submit-revision/${id}`)
}

const viewDetails = (id: number) => {
  router.push(`/proponent-prop-details/${id}`)
}

onMounted(() => {
  fetchRevisions()
})
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
