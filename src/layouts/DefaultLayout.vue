<template>
  <div class="app-layout">
    <header>
      <HeaderLogo />
    </header>

    <aside class="sidebar">
      <NavMenu />
    </aside>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import HeaderLogo from '@/components/HeaderLogo.vue'
import NavMenu from '@/components/NavMenu.vue'
</script>

<style scoped>
.app-layout {
  display: grid;
  grid-template-rows: 169px 1fr;
  grid-template-columns: 246px 1fr;
  height: 100vh;
  overflow: hidden;
}

header {
  grid-column: 1 / -1;
  background: #262342;
  color: #fff;
  padding: 24px;
  display: flex;
  align-items: center;
}

.sidebar {
  background: #ffd803;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.content {
  overflow-y: auto;
  padding: 20px;
}
</style>