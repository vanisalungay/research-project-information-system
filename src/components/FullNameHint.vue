<template>
  <small class="full-name-hint">
    <span class="full-name-hint__format">First Name Last Name</span>
    <span class="full-name-hint__example">Example: Juan Dela Cruz</span>
  </small>
</template>

<script setup>
/**
 * The one hint for account-creation and profile name fields: First Name first, then
 * Last Name, with no comma format. Proposal/personnel name fields keep PersonNameHint
 * because they legitimately accept the institution's "Last, First, M., Jr." order.
 */
</script>

<style scoped>
.full-name-hint {
  display: block;
  margin-top: 4px;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: inherit;
  opacity: 0.72;
}

.full-name-hint__format,
.full-name-hint__example {
  display: block;
}

.full-name-hint__example {
  margin-top: 1px;
}
</style>
