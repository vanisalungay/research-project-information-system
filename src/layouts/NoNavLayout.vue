<template>
  <div class="no-nav-layout">
    <main class="no-nav-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.no-nav-layout {
  min-height: 100vh;
  width: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.no-nav-content {
  width: 100%;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: block;
}
</style>
