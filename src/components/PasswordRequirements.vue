<template>
  <section :id="id" class="pw-req" :aria-labelledby="`${id}-title`">
    <p :id="`${id}-title`" class="pw-req__title">Password Requirements</p>
    <p class="pw-req__intro">{{ PASSWORD_REQUIREMENT_INTRO }}</p>

    <ul class="pw-req__list">
      <li
        v-for="check in checks"
        :key="check.key"
        class="pw-req__item"
        :class="{ 'is-met': check.met }"
      >
        <span class="pw-req__icon" aria-hidden="true">
          <svg v-if="check.met" viewBox="0 0 20 20" fill="none"
            stroke="currentColor" stroke-width="2.2" stroke-linecap="round"
            stroke-linejoin="round">
            <path d="m4 10.5 4 4 8-9" />
          </svg>
          <span v-else class="pw-req__dot"></span>
        </span>
        <span>
          <span class="pw-req__sr">{{ check.met ? 'Met: ' : 'Not met: ' }}</span>{{ check.label }}
        </span>
      </li>
    </ul>

    <p v-if="showMatch" class="pw-req__match" :class="{ 'is-met': match }">
      <span class="pw-req__icon" aria-hidden="true">
        <svg v-if="match" viewBox="0 0 20 20" fill="none" stroke="currentColor"
          stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m4 10.5 4 4 8-9" />
        </svg>
        <svg v-else viewBox="0 0 20 20" fill="none" stroke="currentColor"
          stroke-width="2.2" stroke-linecap="round">
          <path d="M5 5l10 10M15 5 5 15" />
        </svg>
      </span>
      {{ match ? 'Passwords match.' : 'Passwords do not match.' }}
    </p>

    <p class="pw-req__example">
      Example of a valid password: <code>{{ PASSWORD_EXAMPLE }}</code>
    </p>

    <p class="pw-req__sr" aria-live="polite">{{ status }}</p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import {
  PASSWORD_EXAMPLE,
  PASSWORD_REQUIREMENT_INTRO,
  passwordChecks,
} from '@/utils/passwordPolicy'

const props = defineProps({
  /** The id the surrounding input points at with aria-describedby. */
  id: { type: String, default: 'password-requirements' },
  /** The password being typed. Shown by reference only; never emitted or logged. */
  password: { type: String, default: '' },
  /**
   * The confirmation field, when the form has one. Leave null for a form that
   * does not confirm the password (none of the RPIS forms do today).
   */
  confirm: { type: String, default: null },
})

const checks = computed(() => passwordChecks(props.password))
const showMatch = computed(() => props.confirm !== null && props.confirm !== '')
const match = computed(() => props.password === props.confirm)
const status = computed(() => {
  const met = checks.value.filter((check) => check.met).length
  return `${met} of ${checks.value.length} password requirements met.`
})
</script>

<style scoped>
.pw-req {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 0.78rem;
  line-height: 1.45;
}

.pw-req__title {
  margin: 0 0 4px;
  font-weight: 700;
  color: #1c1a3c;
  font-size: 0.8125rem;
}

.pw-req__intro {
  margin: 0 0 8px;
  color: #475569;
}

.pw-req__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 4px;
}

.pw-req__item {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  color: #64748b;
}

.pw-req__item.is-met {
  color: #15803d;
}

.pw-req__icon {
  flex: 0 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 18px;
}

.pw-req__icon svg {
  width: 15px;
  height: 15px;
}

.pw-req__dot {
  width: 7px;
  height: 7px;
  border: 1.5px solid #94a3b8;
  border-radius: 50%;
}

.pw-req__match {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 8px 0 0;
  font-weight: 600;
  color: #b91c1c;
}

.pw-req__match.is-met {
  color: #15803d;
}

.pw-req__match .pw-req__icon {
  height: 16px;
}

.pw-req__example {
  margin: 8px 0 0;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  color: #475569;
}

.pw-req__example code {
  padding: 1px 5px;
  border-radius: 4px;
  background: #eef2ff;
  color: #1c1a3c;
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}

.pw-req__sr {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 480px) {
  .pw-req {
    padding: 10px 12px;
  }
}
</style>
