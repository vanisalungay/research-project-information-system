<template>
  <small class="person-name-hint">
    <span class="person-name-hint__format">{{ PERSON_NAME_FORMAT }}</span>
    <span class="person-name-hint__example">Example: {{ PERSON_NAME_EXAMPLE }}</span>
  </small>
</template>

<script setup>
import { PERSON_NAME_EXAMPLE, PERSON_NAME_FORMAT } from '@/utils/personName'
</script>

<style scoped>
/* Inherits the surrounding text colour so the hint reads correctly on the light
   forms and on the dark profile card alike; only the size and spacing are ours. */
.person-name-hint {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.4;
  color: inherit;
  opacity: 0.72;
}

.person-name-hint__format,
.person-name-hint__example {
  display: block;
}
.person-name-hint__example {
  margin-top: 1px;
}
</style>
