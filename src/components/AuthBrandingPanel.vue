<template>
  <!-- The left, dark-navy half of both authentication pages. It carries the actual
       MSU-Naawan seal and the institutional identity; the form on the right is the
       page's own content. -->
  <aside class="auth-brand" aria-label="Mindanao State University at Naawan">
    <span class="auth-brand__ring auth-brand__ring--top" aria-hidden="true"></span>
    <span class="auth-brand__ring auth-brand__ring--side" aria-hidden="true"></span>
    <span class="auth-brand__ring auth-brand__ring--bottom" aria-hidden="true"></span>
    <span class="auth-brand__ring auth-brand__ring--inner" aria-hidden="true"></span>

    <div class="auth-brand__content">
      <span class="auth-brand__logo">
        <img src="@/assets/images/logo.png" alt="MSU Naawan logo" />
      </span>

      <h1 class="auth-brand__title">Research Project Information System</h1>
      <span class="auth-brand__accent" aria-hidden="true"></span>

      <p class="auth-brand__tagline">Research and Publication Services</p>
      <p class="auth-brand__school">Mindanao State University at Naawan</p>
    </div>
  </aside>
</template>

<script setup>
// The panel's classes live in the shared authentication stylesheet, so Sign In and
// Sign Up render exactly the same branding block.
import '@/assets/auth.css'
</script>
