<template>
  <button
    :type="type"
    class="back-button"
    :class="[`back-button--${variant}`]"
    :aria-label="accessibleLabel"
    @click="onClick"
  >
    <span class="back-button__icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
        stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 12H5" />
        <path d="M11 18l-6-6 6-6" />
      </svg>
    </span>
    <span class="back-button__label">{{ label }}</span>
  </button>
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { useRouter } from 'vue-router'

/**
 * The one Back control of RPIS.
 *
 * Default state: white pill, thin light-grey border, dark-navy circular icon with a
 * yellow left arrow, dark-navy label. Hover turns the icon circle yellow and the arrow
 * navy, and darkens the border. Keyboard focus keeps the default colours and adds a
 * bright-blue focus ring with a gap. Pressed uses a light-grey surface. The `navy`
 * variant is the same control for dark navy surfaces.
 *
 * Navigation:
 *   - `to`        go to that route (explicit destination).
 *   - otherwise   go back in history when there is a previous in-app entry, else go to
 *                 `fallback` (so a page opened directly by URL never leaves the user on a
 *                 dead end).
 *   - `navigate=false` leaves the click to the parent (`@click`), for pages whose Back
 *     button runs page logic (a wizard step, a component state change, a conditional
 *     destination). Passing `@click` without `navigate=false` also leaves it to the
 *     parent, so a custom handler can never be doubled.
 */
const props = defineProps({
  /** Visible label. Contextual labels are allowed (Back to Proposals, Back to Requests...). */
  label: { type: String, default: 'Back' },
  /** `default` for light surfaces, `navy` for dark navy surfaces. */
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'navy'].includes(value),
  },
  /** Explicit destination. Takes precedence over history. */
  to: { type: [String, Object], default: null },
  /** Safe destination when the page was opened directly and has no history entry. */
  fallback: { type: [String, Object], default: null },
  /** False when the parent handles the click itself. */
  navigate: { type: Boolean, default: true },
  type: { type: String, default: 'button' },
})

const router = useRouter()
const attrs = useAttrs()

/** A parent `@click` always owns the behaviour (the listener is inherited by the button). */
const hasCustomClick = computed(() => Boolean(attrs.onClick))
const accessibleLabel = computed(() => (props.label || 'Back').trim() || 'Back')

function hasInAppHistory() {
  if (typeof window === 'undefined') return false
  const state = window.history.state
  return Boolean(state && state.back)
}

function onClick() {
  if (!props.navigate || hasCustomClick.value) return

  if (props.to) {
    router.push(props.to)
    return
  }
  if (hasInAppHistory()) {
    router.back()
    return
  }
  if (props.fallback) {
    router.push(props.fallback)
    return
  }
  router.back()
}
</script>

<style scoped>
.back-button {
  /* Colours come from the application palette first, with the reference values as
     fallbacks so the control still looks right outside the shell. */
  --bb-navy: var(--rpis-sidebar-bg, #1c1a3c);
  --bb-navy-strong: var(--rpis-header-bg, #262342);
  --bb-yellow: var(--rpis-accent, #ffd000);
  --bb-surface: #ffffff;
  --bb-border: var(--ui-border-color, #dbe0ea);
  --bb-text: var(--bb-navy-strong);

  display: inline-flex;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
  height: var(--ui-control-height-md, 38px);
  max-width: 100%;
  padding: 3px 16px 3px 4px;
  border: 1px solid var(--bb-border);
  border-radius: var(--ui-radius-pill, 999px);
  background: var(--bb-surface);
  color: var(--bb-text);
  font-family: inherit;
  font-size: var(--ui-control-font-md, 14px);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-none);
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background-color 160ms ease, border-color 160ms ease, color 160ms ease;
}

.back-button__icon {
  flex: 0 0 var(--ui-control-icon-size, 30px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--ui-control-icon-size, 30px);
  height: var(--ui-control-icon-size, 30px);
  border-radius: 50%;
  background: var(--bb-navy);
  color: var(--bb-yellow);
  transition: background-color 160ms ease, color 160ms ease;
}

.back-button__icon svg {
  display: block;
  width: 16px;
  height: 16px;
}

.back-button__label {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Hover: yellow icon circle, navy arrow, darker outline. */
.back-button:hover {
  border-color: var(--bb-navy-strong);
}

.back-button:hover .back-button__icon {
  background: var(--bb-yellow);
  color: var(--bb-navy);
}

/* Keyboard focus: keep the default colours and show a clear blue ring with a gap. */
.back-button:focus-visible {
  outline: 2px solid var(--ui-focus-color, #2452ff);
  outline-offset: 3px;
}

/* Pressed: a quiet, light-grey surface and a slightly darker outline. */
.back-button:active {
  background: #f3f4f6;
  border-color: #9aa1b2;
}

.back-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* ===== Navy-surface variant ===== */
.back-button--navy {
  --bb-surface: rgba(255, 255, 255, 0.12);
  --bb-border: rgba(255, 255, 255, 0.28);
  --bb-text: var(--rpis-on-dark, #ffffff);
}

.back-button--navy .back-button__icon {
  background: var(--bb-yellow);
  color: var(--bb-navy);
}

.back-button--navy:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.55);
}

.back-button--navy:active {
  background: rgba(0, 0, 0, 0.18);
  border-color: rgba(255, 255, 255, 0.42);
}

@media (prefers-reduced-motion: reduce) {
  .back-button,
  .back-button__icon {
    transition: none;
  }
}
</style>
