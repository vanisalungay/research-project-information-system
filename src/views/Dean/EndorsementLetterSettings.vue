<template>
  <div class="end-page">
    <div class="end-page-header">
      <h2>Endorsement Letter Settings</h2>
      <p class="end-subtitle">
        The institutional facts every official endorsement letter is addressed with.
        <template v-if="canEdit">Only an RPS administrator can change them.</template>
        <template v-else>They are maintained by an RPS administrator.</template>
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading the settings…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <button class="end-btn" @click="load">Retry</button>
      </div>
    </div>

    <template v-else>
      <div v-if="notice" class="end-banner is-info">
        <span>{{ notice }}</span>
      </div>
      <div v-if="saveMessage" class="end-banner is-success">
        <span>{{ saveMessage }}</span>
      </div>
      <div v-if="saveError" class="end-banner is-error">
        <span>{{ saveError }}</span>
      </div>

      <div class="end-card">
        <h3 class="end-section-title">Letter addressee</h3>

        <div v-if="!canEdit" class="end-grid">
          <div class="end-field">
            <span class="end-label">Director</span>
            <span class="end-value">{{ form.directorName }} {{ form.directorSurname }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">Designation</span>
            <span class="end-value">{{ form.directorDesignation }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">Office</span>
            <span class="end-value">{{ form.officeName }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">University</span>
            <span class="end-value">{{ form.universityName }}</span>
          </div>
        </div>

        <form v-else @submit.prevent="save">
          <div class="end-grid">
            <div class="end-field">
              <label for="directorName">Director's given name(s) <span class="end-req">*</span></label>
              <input id="directorName" v-model="form.directorName" class="end-input" type="text" />
            </div>
            <div class="end-field">
              <label for="directorSurname">Director's surname <span class="end-req">*</span></label>
              <input
                id="directorSurname"
                v-model="form.directorSurname"
                class="end-input"
                type="text"
              />
            </div>
            <div class="end-field">
              <label for="directorDesignation">Designation</label>
              <input
                id="directorDesignation"
                v-model="form.directorDesignation"
                class="end-input"
                type="text"
                placeholder="Director"
              />
            </div>
            <div class="end-field">
              <label for="officeName">Office</label>
              <input id="officeName" v-model="form.officeName" class="end-input" type="text" />
            </div>
            <div class="end-field">
              <label for="universityName">University</label>
              <input
                id="universityName"
                v-model="form.universityName"
                class="end-input"
                type="text"
              />
            </div>
          </div>

          <p class="end-muted">
            The letter is refused until the Director's name and surname are supplied: the
            official letter is addressed to the ORPS Director, and no name is ever invented.
          </p>

          <div class="end-actions">
            <button class="end-btn is-primary" type="submit" :disabled="saving">
              {{ saving ? 'Saving…' : 'Save Settings' }}
            </button>
          </div>
        </form>
      </div>

      <div v-if="settings && settings.updatedByName" class="end-card">
        <p class="end-muted">
          Last changed by {{ settings.updatedByName }}
          on {{ formatEndorsementTimestamp(settings.updatedAt) }}.
        </p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import '@/assets/endorsement.css'
import { UserRole, useUserDataStore } from '@/stores/userData'
import {
  buildLetterSettingsPayload,
  fetchLetterSettings,
  formatEndorsementTimestamp,
  letterSettingsNotice,
  saveLetterSettings,
} from '@/utils/endorsement.js'

const userStore = useUserDataStore()
/** Only an RPS_ADMIN may change the settings; everyone else reads them. */
const canEdit = computed(() => userStore.user?.role === UserRole.RPS_ADMIN)

const settings = ref(null)
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const saveMessage = ref('')
const saveError = ref('')

const form = reactive({
  directorName: '',
  directorSurname: '',
  directorDesignation: '',
  officeName: '',
  universityName: '',
})

const notice = computed(() => letterSettingsNotice(settings.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    settings.value = await fetchLetterSettings()
    form.directorName = settings.value?.directorName || ''
    form.directorSurname = settings.value?.directorSurname || ''
    form.directorDesignation = settings.value?.directorDesignation || ''
    form.officeName = settings.value?.officeName || ''
    form.universityName = settings.value?.universityName || ''
  } catch (err) {
    error.value = err?.response?.data?.error || 'The letter settings could not be loaded.'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  saveMessage.value = ''
  saveError.value = ''
  try {
    settings.value = await saveLetterSettings(buildLetterSettingsPayload(form))
    saveMessage.value = 'The endorsement letter settings were saved.'
  } catch (err) {
    saveError.value = err?.response?.data?.error || 'The settings could not be saved.'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
