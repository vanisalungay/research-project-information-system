<template>
  <span class="password-field">
    <input
      ref="inputRef"
      v-bind="$attrs"
      :type="visible ? 'text' : 'password'"
      :value="model"
      @input="model = $event.target.value"
    />
    <button
      type="button"
      class="password-field__toggle"
      :aria-label="visible ? 'Hide password' : 'Show password'"
      :aria-pressed="visible"
      :title="visible ? 'Hide password' : 'Show password'"
      @click="toggle"
    >
      <svg v-if="visible" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m3 3 18 18" />
        <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
        <path d="M9.9 5.2A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a17.8 17.8 0 0 1-3.2 4.2" />
        <path d="M6.1 6.1A17.6 17.6 0 0 0 2 12s3.5 7 10 7a10.9 10.9 0 0 0 4.1-.8" />
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    </button>
  </span>
</template>

<script setup>
import { ref } from 'vue'

/**
 * The one password input of RPIS.
 *
 * It is a normal input (id, class, placeholder, autocomplete, required and any other
 * attribute pass straight through) with a show/hide eye button inside its right edge.
 * The password is always hidden when the field is first rendered. Each instance owns
 * its own visibility state, so a password and its confirmation reveal independently.
 *
 * Toggling only changes the input's `type`: the bound value is untouched, validation
 * still reads the same value, and submitting the surrounding form sends it unchanged.
 * A mouse click returns focus to the input so typing can continue immediately; keyboard
 * activation keeps focus on the toggle so its state can still be announced.
 */
defineOptions({ inheritAttrs: false })

const model = defineModel({ type: String, default: '' })
const visible = ref(false)
const inputRef = ref(null)

function toggle(event) {
  visible.value = !visible.value
  if (event?.detail > 0) inputRef.value?.focus()
}
</script>

<style scoped>
.password-field {
  position: relative;
  display: block;
  width: 100%;
}

/* Keep the input's own size, border, radius, colours and focus styles; reserve room on
   the right for the eye so text never runs underneath it. */
.password-field > input {
  box-sizing: border-box;
  width: 100%;
  padding-right: 44px;
}

/* Edge (and legacy IE) draw their own password reveal control inside a password input.
   The component already supplies the eye toggle below, so hide the browser's duplicate -
   this is what showed two eyes on the sign-in form. The custom toggle keeps working. */
.password-field > input::-ms-reveal,
.password-field > input::-ms-clear {
  display: none;
}

.password-field__toggle {
  position: absolute;
  top: 50%;
  right: 4px;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease;
}

.password-field__toggle svg {
  display: block;
  width: 18px;
  height: 18px;
}

.password-field__toggle:hover {
  background: rgba(15, 23, 42, 0.07);
  color: #1c1a3c;
}

.password-field__toggle:focus-visible {
  outline: 2px solid var(--ui-focus-color, #2452ff);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .password-field__toggle {
    transition: none;
  }
}
</style>
