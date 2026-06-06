<template>
  <div class="question-card" :class="{ confirmed: question.status === 1 }">
    <div class="question-passage" v-if="question.passage">
      <div class="passage-label">阅读材料</div>
      <p class="passage-text">{{ question.passage }}</p>
    </div>

    <div class="question-header">
      <el-checkbox
        v-if="selectable"
        :model-value="selected"
        @update:model-value="$emit('toggle-select', question.id!)"
      />
      <span class="question-type-label">
        <el-tag :type="typeTagColor" size="small">{{ typeLabel }}</el-tag>
      </span>
      <span class="question-index">{{ index }}. {{ question.questionTitle }}</span>
      <span class="question-score">({{ question.score }}分)</span>
    </div>

    <div class="question-options" v-if="hasOptions">
      <div
        v-for="opt in question.options"
        :key="opt.label"
        class="option-item"
        :class="{ 'is-correct': opt.isCorrect }"
      >
        <span class="option-label">{{ opt.label }}.</span>
        <span class="option-text">{{ opt.text }}</span>
        <el-icon v-if="opt.isCorrect" class="correct-icon"><Check /></el-icon>
      </div>
    </div>

    <div class="question-answer" v-if="question.questionType === 'FILL' || question.questionType === 'SHORT'">
      <span class="answer-label">参考答案：</span>
      <span>{{ question.options?.[0]?.text || '' }}</span>
    </div>

    <div class="question-footer">
      <span v-if="question.analysis" class="analysis">
        <strong>解析：</strong>{{ question.analysis }}
      </span>
      <span class="meta">
        <span class="difficulty">难度：{{ difficultyStars }}</span>
        <span class="score">分值：{{ question.score }}</span>
        <span v-if="question.status === 1" class="confirmed-badge">
          <el-tag type="success" size="small">已确认</el-tag>
        </span>
      </span>
    </div>

    <div class="question-actions" v-if="showActions">
      <el-button type="primary" link size="small" @click="$emit('edit', question)">
        <el-icon><Edit /></el-icon>编辑
      </el-button>
      <el-button type="danger" link size="small" @click="$emit('delete', question)">
        <el-icon><Delete /></el-icon>删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QuestionItem, QuestionType } from '@/types'
import { getQuestionTypeLabel, getDifficultyStars } from '@/utils'

const props = withDefaults(defineProps<{
  question: QuestionItem
  index: number
  selectable?: boolean
  selected?: boolean
  showActions?: boolean
}>(), {
  selectable: false,
  selected: false,
  showActions: false
})

defineEmits<{
  'toggle-select': [id: string]
  edit: [question: QuestionItem]
  delete: [question: QuestionItem]
}>()

const typeLabel = computed(() => getQuestionTypeLabel(props.question.questionType))
const difficultyStars = computed(() => getDifficultyStars(props.question.difficulty))

const typeTagColor = computed(() => {
  const map: Record<QuestionType, string> = {
    SINGLE: 'primary',
    MULTI: 'success',
    JUDGE: 'warning',
    FILL: 'info',
    SHORT: 'danger'
  }
  return map[props.question.questionType] || 'info'
})

const hasOptions = computed(() =>
  ['SINGLE', 'MULTI', 'JUDGE'].includes(props.question.questionType)
)
</script>

<style scoped>
.question-card {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
  margin-bottom: var(--space-3);
  background: var(--bg-surface);
  transition: border-color .15s ease;
}
.question-card:hover {
  border-color: var(--border-focus);
}
.question-passage {
  background: var(--bg-muted);
  border-left: 3px solid var(--border-focus);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  padding: var(--space-3) var(--space-4);
  margin-bottom: 14px;
}
.passage-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-tertiary);
  margin-bottom: 6px;
  letter-spacing: .05em;
  text-transform: uppercase;
}
.passage-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-secondary);
  text-indent: 2em;
}
.question-card.confirmed {
  background: var(--success-soft);
  border-color: #b7d9c4;
}
.question-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}
.question-index {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}
.question-score {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}
.question-options {
  margin-left: 24px;
  margin-bottom: var(--space-2);
}
.option-item {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  line-height: 1.8;
}
.option-item.is-correct {
  background: var(--success-soft);
  color: var(--success);
  font-weight: 500;
}
.option-label {
  font-weight: 600;
  margin-right: 4px;
  min-width: 24px;
}
.correct-icon {
  margin-left: 8px;
  color: var(--success);
}
.question-answer {
  margin-left: 24px;
  margin-bottom: var(--space-2);
  color: var(--success);
}
.question-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--border-subtle);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}
.analysis {
  flex: 1;
  color: var(--text-secondary);
}
.meta {
  display: flex;
  gap: var(--space-3);
}
.question-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--border-subtle);
}
</style>
