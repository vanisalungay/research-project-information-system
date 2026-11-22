<template>
  <article class="gad-question">
    <div class="gad-question-main">
      <div class="gad-question-head">
        <span class="gad-question-number">{{ item.key }}</span>
        <p class="gad-question-text">{{ item.question }}</p>
      </div>
      <p v-if="item.alt" class="gad-question-extra">
        <span class="gad-or">OR</span> {{ item.alt }}
      </p>
      <p v-if="item.conditional" class="gad-question-conditional">{{ item.conditional }}</p>

      <div class="gad-answer-row">
        <div class="gad-answer-buttons" role="group" :aria-label="`Answer for item ${item.key}`">
          <button
            v-for="option in options"
            :key="option.key"
            type="button"
            class="gad-answer"
            :class="[
              `is-${option.key.toLowerCase()}`,
              { 'is-selected': response === option.key },
            ]"
            :aria-pressed="response === option.key"
            :disabled="readonly"
            @click="choose(option.key)"
          >
            <span class="gad-answer-label">{{ option.label }}</span>
            <span class="gad-answer-score">{{ option.points }} pts</span>
          </button>
        </div>

        <span class="gad-question-score">
          Score {{ earned === null ? '—' : formatGadScore(earned) }} / {{ gadScoreValueLabel(max) }}
        </span>

        <button
          v-if="!readonly"
          type="button"
          class="gad-clear"
          :disabled="!response"
          @click="emit('clear')"
        >
          Clear
        </button>
      </div>
    </div>

    <div class="gad-question-comments">
      <label :for="commentId" class="gad-comments-label">
        Comments / gender issues identified
      </label>
      <textarea
        v-if="!readonly"
        :id="commentId"
        class="gad-comments"
        :value="comment"
        rows="2"
        placeholder="Add a short note..."
        @input="emit('update:comment', $event.target.value)"
      ></textarea>
      <p v-else class="gad-comments-readonly">{{ comment || '—' }}</p>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import {
  GAD_RESPONSE_OPTIONS,
  GAD_SCALES,
  formatGadScore,
  gadItemMaxScore,
  gadScoreValueLabel,
} from '@/utils/gadChecklist'

const props = defineProps({
  item: { type: Object, required: true },
  response: { type: String, default: '' },
  comment: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:response', 'update:comment', 'clear'])

const scale = computed(() => GAD_SCALES[props.item.scale] || {})

/** The three answers with the configured score of this item's own scale. */
const options = computed(() =>
  GAD_RESPONSE_OPTIONS.map((option) => {
    const score = scale.value[option.key] ?? 0
    return { ...option, score, points: gadScoreValueLabel(score) }
  }),
)

const earned = computed(() => {
  if (!props.response) return null
  const score = scale.value[props.response]
  return Number.isFinite(Number(score)) ? Number(score) : null
})

const max = computed(() => gadItemMaxScore(props.item.key))

const commentId = computed(() => `gad-comment-${String(props.item.key).replace(/\W/g, '-')}`)

function choose(key) {
  if (props.readonly) return
  emit('update:response', key)
}
</script>

<style scoped>
.gad-question {
  display: grid;
  grid-template-columns: minmax(0, 1.27fr) minmax(0, 1fr);
  gap: 14px;
  padding: 11px 12px;
  border-top: 1px solid #eceef6;
}

.gad-question-main {
  min-width: 0;
}

.gad-question-head {
  display: flex;
  align-items: flex-start;
  gap: 7px;
}

.gad-question-number {
  flex: 0 0 auto;
  min-width: 19px;
  padding: 1px 5px;
  border-radius: 6px;
  background: #e8eefc;
  color: #29417a;
  font-size: 9.5px;
  font-weight: 700;
  line-height: 1.5;
  text-align: center;
}

.gad-question-text {
  margin: 0;
  color: #1f2440;
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1.45;
}

.gad-question-extra,
.gad-question-conditional {
  margin: 4px 0 0 26px;
  color: #4b516e;
  font-size: 10.5px;
  line-height: 1.4;
}

.gad-question-conditional {
  color: #7a5a1d;
  font-weight: 600;
}

.gad-or {
  font-weight: 800;
  color: #b45b3e;
}

.gad-answer-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 8px;
}

.gad-answer-buttons {
  display: flex;
  align-items: stretch;
  gap: 6px;
}

.gad-answer {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  padding: 5px 8px;
  border: 1px solid #dfe3ef;
  border-radius: 9px;
  background: #ffffff;
  color: #23294a;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
}

.gad-answer-label {
  font-size: 10.5px;
  font-weight: 700;
  line-height: 1.2;
}

.gad-answer-score {
  margin-top: 1px;
  color: #7b8198;
  font-size: 9px;
  line-height: 1.2;
}

.gad-answer:disabled {
  cursor: default;
}

.gad-answer:not(:disabled):hover {
  border-color: #b9c3dd;
  background: #f8f9fd;
}

.gad-answer.is-yes.is-selected {
  border-color: #58a77b;
  background: #e0f5e8;
  color: #248b58;
}

.gad-answer.is-yes.is-selected .gad-answer-score {
  color: #3f8a63;
}

.gad-answer.is-partly.is-selected {
  border-color: #d7a34b;
  background: #fff0d6;
  color: #a66a12;
}

.gad-answer.is-partly.is-selected .gad-answer-score {
  color: #a66a12;
}

.gad-answer.is-no.is-selected {
  border-color: #e2a0a0;
  background: #fdecec;
  color: #b04545;
}

.gad-answer.is-no.is-selected .gad-answer-score {
  color: #b04545;
}

.gad-question-score {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f2f3f9;
  color: #2a2f50;
  font-size: 9.5px;
  font-weight: 700;
  white-space: nowrap;
}

.gad-clear {
  padding: 2px 3px;
  border: 0;
  background: none;
  color: #5a607c;
  font-family: inherit;
  font-size: 9.5px;
  font-weight: 600;
  text-decoration: underline;
  cursor: pointer;
}

.gad-clear:disabled {
  color: #b1b5c8;
  cursor: default;
  text-decoration: none;
}

.gad-question-comments {
  min-width: 0;
}

.gad-comments-label {
  display: block;
  margin-bottom: 4px;
  color: #5c627e;
  font-size: 9.5px;
  font-weight: 600;
  line-height: 1.3;
}

.gad-comments {
  box-sizing: border-box;
  width: 100%;
  min-height: 50px;
  padding: 6px 8px;
  border: 1px solid #e1e4ef;
  border-radius: 8px;
  background: #ffffff;
  color: #23294a;
  font-family: inherit;
  font-size: 10.5px;
  line-height: 1.4;
  resize: vertical;
}

.gad-comments:focus {
  outline: none;
  border-color: #8ea4d8;
  box-shadow: 0 0 0 2px rgba(89, 122, 198, 0.12);
}

.gad-comments-readonly {
  margin: 0;
  padding: 6px 8px;
  min-height: 38px;
  border: 1px solid #eceef6;
  border-radius: 8px;
  color: #4b516e;
  font-size: 10.5px;
  white-space: pre-wrap;
}

@media (max-width: 860px) {
  .gad-question {
    grid-template-columns: 1fr;
    gap: 9px;
  }
}

@media (max-width: 480px) {
  .gad-answer {
    min-width: 50px;
    padding: 5px 6px;
  }

  .gad-answer-row {
    gap: 6px;
  }

  .gad-question-score {
    order: 3;
  }
}
</style>
