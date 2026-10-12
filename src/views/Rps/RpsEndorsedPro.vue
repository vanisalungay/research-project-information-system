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
  font-family: 'Inter', sans-serif;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #20234f;
  line-height: 1.2;
}

.subtitle {
  margin: 4px 0 14px;
  font-size: 12px;
  font-weight: 400;
  color: #78809a;
  line-height: 1.4;
}

@media (max-width: 640px) {
  .page-header h2 {
    font-size: 19px;
  }
}
</style>
