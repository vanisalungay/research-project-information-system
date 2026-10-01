<template>
  <div class="qpr-form-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← Back</button>
      <div>
        <h1>{{ isEdit ? 'Edit Quarterly Progress Report' : 'New Quarterly Progress Report' }}</h1>
        <p class="subtitle">{{ project?.projectTitle || '' }}</p>
      </div>
    </div>

    <div v-if="loading" class="state">Loading...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="!eligible" class="state notice">
      This project is not yet approved for implementation or does not have an issued Special Order (SO).
      Quarterly progress reports are only available for approved, SO-issued projects.
    </div>
    <template v-else>
      <QuarterlyReportForm
        :report="report"
        :saving="saving"
        @save-draft="save('DRAFT', $event)"
        @submit="save('SUBMITTED', $event)"
        @cancel="$router.back()"
      />
      <p v-if="saveError" class="state error">{{ saveError }}</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'

const route = useRoute()
const router = useRouter()
const proposalId = computed(() => route.params.proposalId)
const reportId = computed(() => route.params.reportId)
const isEdit = computed(() => !!reportId.value)

const project = ref(null)
const report = ref(null)
const loading = ref(true)
const error = ref('')
const saveError = ref('')
const saving = ref(false)

const eligible = computed(() => {
  const p = project.value
  if (!p) return false
  const hasSo = p.soNumber && p.soNumber.trim() !== ''
  const approved = p.status === 'APPROVED' || p.status === 'RELEASED'
  return hasSo && approved
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const p = await api.get(`/api/proposals/${proposalId.value}`)
    project.value = p.data
    if (isEdit.value) {
      const r = await api.get(`/api/quarterly-reports/${reportId.value}`)
      report.value = r.data
    } else {
      report.value = {
        projectTitle: p.data.projectTitle || '',
        projectLeader: p.data.projectLeader || '',
        projectDuration: p.data.duration || '',
        projectStartDate: p.data.startDate || '',
        projectEndDate: p.data.endDate || '',
        objectives: [],
        outputs: [],
      }
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load.'
  } finally {
    loading.value = false
  }
}

async function save(status, payload) {
  if (saving.value) return
  saving.value = true
  saveError.value = ''
  try {
    const body = { ...payload, proposalId: Number(proposalId.value), status }
    if (isEdit.value) {
      await api.put(`/api/quarterly-reports/${reportId.value}`, body)
    } else {
      await api.post('/api/quarterly-reports', body)
    }
    router.push(`/quarterly-reports/${proposalId.value}`)
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Failed to save report.'
  } finally {
    saving.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.qpr-form-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}
.back-btn {
  background: #fff;
  color: #0f172a;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.state {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 24px;
  border-radius: 8px;
  text-align: center;
  color: #64748b;
}
.state.error {
  color: #dc2626;
}
.state.notice {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}
</style>
