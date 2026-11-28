<template>
  <div class="page">
    <div class="page-header">
      <h2>Endorsed Proposals</h2>
      <p class="subtitle">Proposals endorsed by RPS and forwarded to OVCRIGE.</p>
    </div>

    <RpsEndorsedProPanel />
  </div>
</template>

<script setup>
import RpsEndorsedProPanel from './RpsEndorsedProPanel.vue'
</script>

<style scoped>
.page {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  box-sizing: border-box;
  padding: 14px 16px 24px;
  background: #f3f4f6;
  font-family: var(--ui-font-ui);
}

.page-header h2 {
  margin: 0;
  font-size: var(--ui-font-page-title);
  font-weight: var(--ui-weight-bold);
  color: #20234f;
  line-height: var(--ui-line-display);
}

.subtitle {
  margin: 4px 0 14px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-regular);
  color: var(--ui-text-muted);
  line-height: var(--ui-line-snug);
}

@media (max-width: 640px) {
  .page-header h2 {
    font-size: var(--ui-font-page-title);
    font-weight: var(--ui-weight-bold);
  }
}
</style>
