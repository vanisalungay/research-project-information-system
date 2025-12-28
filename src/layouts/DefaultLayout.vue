<template>
  <div class="app-layout">
    <header>
      <HeaderLogo />
    </header>

    <div class="container">
      <aside class="sidebar">
        <NavMenu />
      </aside>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import HeaderLogo from '@/components/HeaderLogo.vue'
import NavMenu from '@/components/NavMenu.vue'
</script>

<style scoped>
header {
  background: #262342;
  color: #fff;
  padding: 24px;
  text-align: center;
  height: 169px;
  display: flex;
  align-items: center;
}

.app-layout {
  min-height: 100vh;
}

.container {
  display: flex;
  min-height: calc(100vh - 169px);
}

.sidebar {
  width: 246px;
  background: #ffd803;
}

.content {
  flex: 1;
  padding: 20px;
}
</style>
