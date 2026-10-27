<template>
  <section class="tr-sheet">
    <!-- Institutional letterhead of the official report: the university seal already
         shipped with RPIS and the official text printed on the sample report. -->
    <header v-if="cover" class="tr-letterhead">
      <img class="tr-seal" :src="seal" alt="" aria-hidden="true" />
      <div class="tr-letterhead-text">
        <p class="tr-republic">{{ letterhead.republic }}</p>
        <p class="tr-university">{{ letterhead.university }}</p>
        <p class="tr-address">{{ letterhead.address }}</p>
        <p class="tr-website">{{ letterhead.website }}</p>
        <p class="tr-contact">{{ letterhead.contact }}</p>
      </div>
      <span class="tr-badge">{{ letterhead.badge }}</span>
    </header>

    <div class="tr-sheet-body">
      <slot />
    </div>

    <footer class="tr-sheet-footer">
      <span v-if="pageLabel" class="tr-page-number">{{ pageLabel }}</span>
    </footer>
  </section>
</template>

<script setup>
// Terminal Report document styling (page, letterhead, headings, tables, page numbers).
import '@/assets/terminalReportDocument.css'
import { TERMINAL_LETTERHEAD } from '@/utils/terminalReport'
import seal from '@/assets/images/msu-naawan-seal.jpg'

/**
 * One printed page of the Terminal Report document.
 *
 * The cover page carries the institutional letterhead (the university seal and the official
 * text printed on the official report) exactly as the sample does; every other page is a
 * plain page with the page number centred at the bottom, front matter in lowercase Roman
 * and the body in Arabic - the same page numbering the official format uses.
 *
 * Presentation only - no report data or logic lives here.
 */
defineProps({
  /** True only for the cover page, which carries the letterhead and no page number. */
  cover: { type: Boolean, default: false },
  /** "i", "12" - the page number printed at the bottom; empty on the cover page. */
  pageLabel: { type: String, default: '' },
})

const letterhead = TERMINAL_LETTERHEAD
</script>
