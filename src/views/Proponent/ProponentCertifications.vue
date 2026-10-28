<template>
  <!-- The "Issued Certifications" section of the consolidated My Endorsements page: the
       official endorsement letters the Dean has issued. An entry exists only after an
       approval - the backend creates it inside the approval transaction - so a request is
       never shown as an issued document before it has been approved. -->
  <div>
    <div v-if="loading" class="end-card is-empty">
      <p>Loading your certifications…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <button class="end-btn" @click="load">Retry</button>
      </div>
    </div>

    <template v-else>
      <div v-if="actionError" class="end-banner is-error">
        <span>{{ actionError }}</span>
      </div>

      <div v-if="certifications.length === 0" class="end-card is-empty">
        <p>
          You have no certification documents yet. Once a Dean endorses one of your
          projects, the official endorsement letter is filed here.
        </p>
        <div class="end-actions">
          <button class="end-btn is-primary" type="button" @click="emit('open-requests')">
            View my endorsement requests
          </button>
        </div>
      </div>

      <div v-else class="end-card end-table-scroll">
        <table class="end-table">
          <thead>
            <tr>
              <th>Document</th>
              <th>Type</th>
              <th>Proposal title</th>
              <th>Endorsement date</th>
              <th>Issuing Dean</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in certifications" :key="item.id">
              <td>
                {{ item.title }}
                <div class="end-muted">{{ item.fileName }}</div>
              </td>
              <td>{{ item.documentTypeLabel || documentTypeLabel(item.documentType) }}</td>
              <td>
                <router-link
                  v-if="item.endorsementRequestId"
                  class="end-btn is-link"
                  :to="`/endorsements/${item.endorsementRequestId}`"
                >
                  {{ endorsementTitle(item) }}
                </router-link>
                <span v-else>{{ endorsementTitle(item) }}</span>
              </td>
              <td>{{ formatEndorsementDate(item.endorsementDate) || '—' }}</td>
              <td>{{ item.issuingDeanName || '—' }}</td>
              <td>
                <span class="end-badge is-approved">{{ item.status }}</span>
              </td>
              <td>
                <template v-if="item.documentAvailable">
                  <button class="end-btn is-link" @click="preview(item)">View</button>
                  <button class="end-btn is-link" @click="download(item)">Download</button>
                </template>
                <span v-else class="end-muted">Not available</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import '@/assets/endorsement.css'
import {
  documentTypeLabel,
  fetchCertificationDocument,
  fetchCertifications,
  formatEndorsementDate,
  openDocumentBlob,
  saveDocumentBlob,
} from '@/utils/endorsement.js'

/**
 * `open-requests` asks the page to switch back to the request list: the empty state is the
 * only place this section needs to move the Proponent on. The section never navigates on
 * its own.
 */
const emit = defineEmits(['open-requests'])

const certifications = ref([])
const loading = ref(true)
const error = ref('')
const actionError = ref('')

/** The proposal title is part of the certification title, minus its type prefix. */
function endorsementTitle(item) {
  const title = String(item.title || '')
  const prefix = 'Endorsement Letter - '
  return title.startsWith(prefix) ? title.slice(prefix.length) : title
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    certifications.value = await fetchCertifications()
  } catch (err) {
    error.value = err?.response?.data?.error || 'Your certifications could not be loaded.'
  } finally {
    loading.value = false
  }
}

/** The same stored document is served for the preview and the download. */
async function preview(item) {
  actionError.value = ''
  try {
    const blob = await fetchCertificationDocument(item.id)
    openDocumentBlob(blob, item.fileName)
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The document could not be opened.'
  }
}

async function download(item) {
  actionError.value = ''
  try {
    const blob = await fetchCertificationDocument(item.id, { download: true })
    saveDocumentBlob(blob, item.fileName || 'certification.pdf')
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The document could not be downloaded.'
  }
}

onMounted(load)
</script>
