<template>
  <Teleport to="body">
    <div class="budget-template-overlay" @click.self="emit('close')">
      <div
        class="budget-template-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="line-item-budget-template-title"
      >
        <!-- HEADER: title, file name, Download and the close icon -->
        <header class="budget-template-header">
          <div class="budget-template-heading">
            <h2 id="line-item-budget-template-title">{{ title }}</h2>
            <p class="budget-template-file">{{ fileName }}</p>
          </div>
          <div class="budget-template-header-actions">
            <a class="btn-download" :href="url" :download="fileName">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              Download
            </a>
            <button
              ref="closeIconRef"
              class="close-btn"
              aria-label="Close template preview"
              @click="emit('close')"
            >
              ✕
            </button>
          </div>
        </header>

        <!-- BODY: the official template itself, rendered in place -->
        <div class="budget-template-body">
          <p v-if="previewKind === PREVIEW_PDF" class="budget-template-hint">
            Scroll inside the frame to read the whole template. If it does not appear, use Download
            to open it in your PDF viewer.
          </p>
          <p v-if="previewKind === PREVIEW_UNSUPPORTED" class="budget-template-state">
            In-browser preview is not available for {{ typeLabel }} files. Use Download to open the
            template in its own application.
          </p>
          <iframe
            v-else-if="previewKind === PREVIEW_PDF"
            class="budget-template-frame"
            :src="url"
            :title="title"
          ></iframe>
          <img v-else class="budget-template-image" :src="url" :alt="title" />
        </div>

        <!-- FOOTER: an always visible Close action besides the header icon -->
        <footer class="budget-template-footer">
          <button class="btn-close" @click="emit('close')">Close</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  PREVIEW_PDF,
  PREVIEW_UNSUPPORTED,
  fileTypeLabel,
  previewKindOf,
} from '@/utils/documentPreview'
import {
  LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME,
  LINE_ITEM_BUDGET_TEMPLATE_TITLE,
  LINE_ITEM_BUDGET_TEMPLATE_URL,
} from '@/utils/budgetTemplate'

/**
 * Read-only viewer for the official Line-Item Budget template of the Create
 * Proposal form (section 25).
 *
 * It displays the actual shipped template file - a PDF rendered by the browser's
 * own viewer inside an iframe (which scrolls internally, so a multi-page template
 * never moves the modal header), an image in an <img> (contained, never stretched
 * or cropped), and a readable message for formats the browser cannot display
 * in-app. The proponent therefore reads the template's format without downloading
 * it; Download stays available from the header.
 *
 * The modal is teleported to <body> so it is never clipped by the Create Proposal
 * modal it is opened from, and Escape closes it like the other modals of the app.
 * Nothing here edits the proposal: the template is reference material only.
 */
const props = defineProps({
  /** Modal title. */
  title: { type: String, default: LINE_ITEM_BUDGET_TEMPLATE_TITLE },
  /** Stored template file name, used for the viewer decision and Download. */
  fileName: { type: String, default: LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME },
  /** URL of the template (a static asset from public/templates). */
  url: { type: String, default: LINE_ITEM_BUDGET_TEMPLATE_URL },
})

const emit = defineEmits(['close'])

/** How the shipped template is displayed: 'pdf', 'image' or 'unsupported'. */
const previewKind = computed(() => previewKindOf(props.fileName))
/** Short file-type label shown in the fallback message ("PDF", "DOCX", ...). */
const typeLabel = computed(() => fileTypeLabel(props.fileName))

const closeIconRef = ref(null)

// Escape closes the modal, like the other modals in the application.
function onKeydown(e) {
  if (e.key === 'Escape') emit('close')
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  // Move the focus into the dialog so the keyboard (and Escape) reaches it.
  await nextTick()
  closeIconRef.value?.focus?.()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ============================================================
   Line-Item Budget template viewer
   The chrome mirrors the other document modals of the application: fixed
   overlay, white card, and a header holding the title, the file name, the
   Download action and the close button. It is teleported to <body>, so it also
   covers the Create Proposal modal it is opened from.
   ============================================================ */
.budget-template-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200; /* above the Create Proposal modal (1000) and its criteria modal (1100) */
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
}

.budget-template-modal {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1000px;
  /* The modal takes about 90% of the viewport height so most of the template is
     readable at once, and it stays inside the screen: the max-height below keeps
     it within the overlay's 20px padding above and below (100vh - 40px), and the
     header, the scrolling template area and the footer share that height. */
  height: 90vh;
  height: 90dvh; /* dynamic viewport unit where supported (mobile toolbars) */
  max-height: calc(100vh - 40px);
  max-height: calc(100dvh - 40px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden; /* the modal itself never scrolls - the template area does */
}

/* ===== HEADER ===== */
.budget-template-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.budget-template-heading {
  min-width: 0;
}

.budget-template-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.budget-template-file {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

.budget-template-header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

.close-btn:focus-visible,
.btn-download:focus-visible,
.btn-close:focus-visible {
  outline: 2px solid #4f46e5;
  outline-offset: 2px;
}

/* ===== TEMPLATE AREA =====
   The template fills the modal body: the PDF is rendered by the browser's own
   viewer inside the iframe (it scrolls internally), while an image or the
   fallback message can scroll in this area on a short screen. */
.budget-template-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
  background: #f8fafc;
}

.budget-template-hint {
  flex-shrink: 0;
  margin: 0;
  padding: 8px 24px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
  color: #64748b;
}

.budget-template-frame {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  border: 0;
  background: #fff;
}

.budget-template-image {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  padding: 16px;
  box-sizing: border-box;
  object-fit: contain; /* keeps the aspect ratio, never stretched or cropped */
}

.budget-template-state {
  margin: 0;
  padding: 32px 24px;
  text-align: center;
  font-size: 14px;
  line-height: 1.5;
  color: #64748b;
}

/* ===== FOOTER ===== */
.budget-template-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 14px 24px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}

.btn-close {
  padding: 10px 20px;
  background: #4f46e5;
  border: 1px solid #4f46e5;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-close:hover {
  background: #4338ca;
  border-color: #4338ca;
}

/* Small screens: the header actions wrap below the title instead of squeezing it
   away, the modal keeps its padding tight and Close spans the footer width. The
   90vh height still applies - only the cap is tightened to the overlay's 8px
   padding, so the modal can never extend beyond a short viewport. */
@media (max-width: 640px) {
  .budget-template-overlay {
    padding: 8px;
  }
  .budget-template-modal {
    max-height: calc(100vh - 16px);
    max-height: calc(100dvh - 16px);
  }
  .budget-template-header {
    flex-wrap: wrap;
    padding: 14px 16px;
  }
  .budget-template-heading {
    min-width: 160px;
  }
  .budget-template-footer {
    padding: 12px 16px;
  }
  .btn-close {
    width: 100%;
  }
}
</style>
