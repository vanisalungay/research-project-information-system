<template>
  <div class="rps-hub">
    <!-- PAGE HEADING -->
    <div class="hub-header">
      <h1 class="hub-title">{{ activeMeta.title }}</h1>
      <p class="hub-subtitle">{{ activeMeta.description }}</p>
    </div>

    <!-- CATEGORY BUTTONS -->
    <div class="category-tabs" role="tablist" aria-label="Proposal categories">
      <button
        type="button"
        role="tab"
        class="category-btn"
        :class="{ active: activeCategory === 'all' }"
        :aria-selected="activeCategory === 'all'"
        @click="activeCategory = 'all'"
      >
        <svg class="cat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        <span>All Submitted</span>
      </button>

      <button
        type="button"
        role="tab"
        class="category-btn"
        :class="{ active: activeCategory === 'revision' }"
        :aria-selected="activeCategory === 'revision'"
        @click="activeCategory = 'revision'"
      >
        <svg class="cat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="1 4 1 10 7 10"></polyline>
          <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
        </svg>
        <span>Revision Requests</span>
      </button>

      <button
        type="button"
        role="tab"
        class="category-btn"
        :class="{ active: activeCategory === 'endorsed' }"
        :aria-selected="activeCategory === 'endorsed'"
        @click="activeCategory = 'endorsed'"
      >
        <svg class="cat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <span>Endorsed Proposals</span>
      </button>
    </div>

    <!-- ACTIVE CATEGORY CONTENT -->
    <div class="hub-body">
      <KeepAlive>
        <component :is="activePanel" />
      </KeepAlive>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import RpsSubmittedProposalsPanel from './RpsSubmittedProposalsPanel.vue'
import RpsRevisionRequestsPanel from './RpsRevisionRequestsPanel.vue'
import RpsEndorsedProPanel from './RpsEndorsedProPanel.vue'

// Category buttons only switch which existing view is displayed. They never
// trigger proposal status changes or workflow operations.
const panels = {
  all: RpsSubmittedProposalsPanel,
  revision: RpsRevisionRequestsPanel,
  endorsed: RpsEndorsedProPanel,
}

const activeCategory = ref('all')

const activePanel = computed(() => panels[activeCategory.value] || panels.all)

// Heading + description for each category, driven by the same selected-category
// state used by the category buttons and the content switcher.
const categoryMeta = {
  all: {
    title: 'Submitted Proposals',
    description: 'Review and manage all proposals forwarded to RPS.',
  },
  revision: {
    title: 'Revision Requests',
    description: 'Review and manage proposals returned for revision.',
  },
  endorsed: {
    title: 'Endorsed Proposals',
    description: 'Review and manage proposals endorsed through the existing RPS process.',
  },
}

const activeMeta = computed(() => categoryMeta[activeCategory.value] || categoryMeta.all)
</script>

<style scoped>
.rps-hub {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  box-sizing: border-box;
  padding: 14px 16px 24px;
  background: #f3f4f6;
  font-family: 'Inter', sans-serif;
}

/* HEADING */
.hub-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #20234f;
  line-height: 1.2;
}

.hub-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 400;
  color: #78809a;
  line-height: 1.4;
}

/* CATEGORY BUTTONS */
.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 14px;
}

.category-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #e3e6ef;
  background: #ffffff;
  color: #25245b;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.category-btn:hover {
  border-color: #25245b;
}

.category-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.category-btn.active {
  background: #25245b;
  border-color: #25245b;
  color: #ffd400;
  font-weight: 600;
}

.cat-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* BODY */
.hub-body {
  margin-top: 12px;
}

@media (max-width: 640px) {
  .hub-title {
    font-size: 19px;
  }

  .category-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .category-btn {
    flex-shrink: 0;
  }
}
</style>
