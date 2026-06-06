<template>
  <div class="quiz-take">
    <!-- ===== 答题模式 ===== -->
    <template v-if="mode === 'quiz'">
      <div class="quiz-header">
        <div class="quiz-header-left">
          <el-button @click="$router.push('/app/test-paper')">
            <el-icon><ArrowLeft /></el-icon>返回试题列表
          </el-button>
          <span class="quiz-doc-name">文档: {{ paper?.docName }}</span>
        </div>
        <div class="quiz-header-right">
          <span class="timer" :class="{ warning: remainingSeconds < 300 }">
            <el-icon><Clock /></el-icon>
            剩余时间: {{ formatSeconds(remainingSeconds) }}
          </span>
          <span class="progress-text">进度: {{ answeredCount }} / {{ paper?.totalCount }}</span>
        </div>
      </div>

      <el-row :gutter="20">
        <el-col :span="18">
          <el-card class="questions-panel" v-loading="loading">
            <template v-if="paper?.questions">
              <template v-for="(group, gIdx) in questionGroups" :key="group.type">
                <div class="question-group">
                  <h3 class="group-title" :class="'type-' + group.type">{{ group.label }}（{{ group.questions.length }}题，共{{ group.totalScore }}分）</h3>

                  <div
                    v-for="(q, idx) in group.questions"
                    :key="q.id"
                    class="question-block"
                    :class="'qtype-' + q.questionType"
                    :ref="el => { if (el) questionRefs[q.id] = el as HTMLElement }"
                  >
                    <div class="question-stem">
                      <span class="q-num">{{ group.startIdx + idx + 1 }}.</span>
                      <span>{{ q.questionTitle }}</span>
                      <el-tag size="small" type="primary">{{ q.score }}分</el-tag>
                    </div>

                    <!-- 阅读材料 -->
                    <div class="quiz-passage" v-if="q.passage">
                      <div class="quiz-passage-label">阅读材料</div>
                      <p class="quiz-passage-text">{{ q.passage }}</p>
                    </div>

                    <!-- 选项 -->
                    <div class="q-options" v-if="hasOptions(q.questionType)">
                      <template v-if="q.questionType === 'SINGLE'">
                        <el-radio-group
                          :model-value="(userAnswers[q.id] || [''])[0]"
                          @update:model-value="setAnswer(q.id, [$event as string])"
                        >
                          <div v-for="opt in q.options" :key="opt.label" class="option-row">
                            <el-radio :value="opt.label">
                              {{ opt.label }}. {{ opt.text }}
                            </el-radio>
                          </div>
                        </el-radio-group>
                      </template>

                      <template v-else-if="q.questionType === 'MULTI'">
                        <el-checkbox-group
                          :model-value="userAnswers[q.id] || []"
                          @update:model-value="setAnswer(q.id, $event as string[])"
                        >
                          <div v-for="opt in q.options" :key="opt.label" class="option-row">
                            <el-checkbox :value="opt.label">
                              {{ opt.label }}. {{ opt.text }}
                            </el-checkbox>
                          </div>
                        </el-checkbox-group>
                      </template>

                      <template v-else-if="q.questionType === 'JUDGE'">
                        <el-radio-group
                          :model-value="(userAnswers[q.id] || [''])[0]"
                          @update:model-value="setAnswer(q.id, [$event as string])"
                        >
                          <div class="option-row">
                            <el-radio value="正确">正确</el-radio>
                          </div>
                          <div class="option-row">
                            <el-radio value="错误">错误</el-radio>
                          </div>
                        </el-radio-group>
                      </template>
                    </div>

                    <!-- 填空 / 简答 -->
                    <div v-else class="q-options">
                      <el-input
                        v-if="q.questionType === 'FILL'"
                        :model-value="(userAnswers[q.id] || [''])[0]"
                        @update:model-value="setAnswer(q.id, [$event as string])"
                        placeholder="请输入答案"
                      />
                      <el-input
                        v-else
                        :model-value="(userAnswers[q.id] || [''])[0]"
                        @update:model-value="setAnswer(q.id, [$event as string])"
                        type="textarea"
                        :rows="3"
                        placeholder="请输入回答"
                      />
                    </div>
                  </div>
                </div>
              </template>
            </template>
          </el-card>
        </el-col>

        <!-- 答题卡 -->
        <el-col :span="6">
          <div class="answer-sheet">
            <el-card>
              <template #header>
                <span>答题卡</span>
              </template>
              <div class="sheet-grid" v-if="paper">
                <div
                  v-for="(q, idx) in paper.questions"
                  :key="q.id"
                  class="sheet-item"
                  :class="{
                    answered: userAnswers[q.id]?.length,
                    current: false
                  }"
                  @click="scrollToQuestion(q.id)"
                >
                  {{ idx + 1 }}
                </div>
              </div>
              <div class="sheet-legend">
                <span class="legend-item"><span class="dot answered-dot"></span>已答</span>
                <span class="legend-item"><span class="dot unanswered-dot"></span>未答</span>
              </div>
            </el-card>

            <el-button
              type="danger"
              size="large"
              class="submit-btn"
              @click="handleSubmit"
              :loading="submitting"
            >
              交卷
            </el-button>
          </div>
        </el-col>
      </el-row>

      <el-dialog
        v-model="submitDialogVisible"
        title="确认交卷"
        width="360px"
      >
        <p>还有 <strong>{{ unansweredCount }}</strong> 道题未作答，确定交卷？</p>
        <template #footer>
          <el-button @click="submitDialogVisible = false">继续答题</el-button>
          <el-button type="danger" @click="doSubmit">确认交卷</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- ===== 成绩展示模式 ===== -->
    <template v-else-if="mode === 'result'">
      <div class="result-header">
        <el-button @click="$router.push('/app/test-paper')">
          <el-icon><ArrowLeft /></el-icon>返回试题列表
        </el-button>
        <span class="result-title">{{ paper?.docName }} — 答题成绩</span>
      </div>

      <div class="result-summary" v-loading="resultLoading">
        <el-card v-if="result">
          <el-row :gutter="20" class="summary-stats">
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-num primary">{{ result.userScore }} / {{ result.totalScore }}</div>
                <div class="stat-label">得分</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-num success">{{ result.correctCount }} / {{ result.totalCount }}</div>
                <div class="stat-label">答对题数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-num warning">{{ formatSeconds(result.durationSeconds) }}</div>
                <div class="stat-label">用时</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <div v-if="result" class="result-details">
          <div
            v-for="(detail, idx) in result.details"
            :key="detail.questionId"
            class="detail-item"
            :class="{ correct: detail.isCorrect, wrong: !detail.isCorrect }"
          >
            <div class="detail-header">
              <span class="detail-index">{{ idx + 1 }}.</span>
              <span v-if="detail.isCorrect" class="mark correct-mark">
                <el-icon><Check /></el-icon> +{{ detail.earnedScore }}
              </span>
              <span v-else class="mark wrong-mark">
                <el-icon><Close /></el-icon> 0 / {{ detail.score }}
              </span>
              <el-tag size="small">{{ getQuestionTypeLabel(detail.questionType) }}</el-tag>
            </div>
            <!-- 阅读材料 -->
            <div class="quiz-passage" v-if="detail.passage">
              <div class="quiz-passage-label">阅读材料</div>
              <p class="quiz-passage-text">{{ detail.passage }}</p>
            </div>
            <div class="detail-title">{{ detail.questionTitle }}</div>
            <!-- 选项列表 -->
            <div v-if="detail.options?.length" class="detail-options">
              <div
                v-for="opt in detail.options"
                :key="opt.label"
                class="result-option"
                :class="{
                  'option-correct': opt.isCorrect,
                  'option-user-wrong': !opt.isCorrect && detail.userAnswer.includes(opt.label)
                }"
              >
                <span class="opt-label">{{ opt.label }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
                <el-icon v-if="opt.isCorrect" class="opt-icon-correct"><Check /></el-icon>
                <el-icon v-else-if="detail.userAnswer.includes(opt.label)" class="opt-icon-wrong-result"><Close /></el-icon>
              </div>
            </div>
            <div class="detail-answers">
              <div class="user-answer">
                <span class="label">你的答案：</span>
                <span :class="{ correct: detail.isCorrect }">{{ detail.userAnswer.join(', ') || '未作答' }}</span>
                <el-icon v-if="detail.isCorrect" class="icon-correct"><Check /></el-icon>
                <el-icon v-else class="icon-wrong"><Close /></el-icon>
              </div>
              <div v-if="!detail.isCorrect" class="correct-answer">
                <span class="label">正确答案：</span>
                <span class="correct-text">{{ detail.correctAnswer.join(', ') }}</span>
              </div>
            </div>
            <div v-if="detail.analysis" class="detail-analysis">
              <strong>解析：</strong>{{ detail.analysis }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 交卷批改遮罩（根层级，独立于 mode 切换，避免闪烁） -->
    <div v-if="submitting" class="grading-overlay">
      <div class="grading-box">
        <el-icon class="grading-spinner" :size="48"><Loading /></el-icon>
        <p class="grading-text">正在批改试卷，请稍候...</p>
        <p class="grading-sub">系统正在验证答案并计算得分</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Clock, Check, Close, Loading } from '@element-plus/icons-vue'
import { getQuizPaper, submitQuiz, getQuizResultById } from '@/api/quiz'
import { formatSeconds, getQuestionTypeLabel } from '@/utils'
import type { QuizPaper, PaperQuestion, QuizResultData, QuestionType } from '@/types'

const route = useRoute()
const router = useRouter()

// Determine mode from route
const paperId = route.params.paperId as string
const mode = computed<'quiz' | 'result'>(() => route.params.resultId ? 'result' : 'quiz')

const loading = ref(false)
const paper = ref<QuizPaper | null>(null)

// Quiz mode state
const userAnswers = reactive<Record<string, string[]>>({})
const startTime = ref('')
const remainingSeconds = ref(1800) // default 30 min
const submitting = ref(false)
const submitDialogVisible = ref(false)
const questionRefs: Record<string, HTMLElement> = {}
let timerInterval: ReturnType<typeof setInterval> | null = null

// Result mode state
const resultLoading = ref(false)
const result = ref<QuizResultData | null>(null)

const answeredCount = computed(() =>
  Object.values(userAnswers).filter(a => a.length > 0 && a[0] !== '').length
)

const unansweredCount = computed(() =>
  (paper.value?.totalCount || 0) - answeredCount.value
)

const questionGroups = computed(() => {
  if (!paper.value) return []
  const types = ['SINGLE', 'MULTI', 'JUDGE', 'FILL', 'SHORT'] as QuestionType[]
  let startIdx = 0
  return types
    .map(type => {
      const questions = paper.value!.questions.filter(q => q.questionType === type)
      if (questions.length === 0) return null
      const group = {
        type,
        label: getQuestionTypeLabel(type),
        questions,
        totalScore: questions.reduce((s, q) => s + q.score, 0),
        startIdx
      }
      startIdx += questions.length
      return group
    })
    .filter(Boolean) as {
      type: string
      label: string
      questions: PaperQuestion[]
      totalScore: number
      startIdx: number
    }[]
})

function hasOptions(type: QuestionType) {
  return ['SINGLE', 'MULTI', 'JUDGE'].includes(type)
}

function setAnswer(questionId: string, answer: string[]) {
  userAnswers[questionId] = answer
  saveToLocal()
}

function saveToLocal() {
  const backup = {
    paperId,
    answers: userAnswers,
    startTime: startTime.value,
    remaining: remainingSeconds.value
  }
  localStorage.setItem(`quiz_backup_${paperId}`, JSON.stringify(backup))
}

function loadFromLocal() {
  const raw = localStorage.getItem(`quiz_backup_${paperId}`)
  if (!raw) return
  try {
    const backup = JSON.parse(raw)
    if (backup.paperId === paperId) {
      Object.assign(userAnswers, backup.answers)
      remainingSeconds.value = backup.remaining
      startTime.value = backup.startTime || new Date().toISOString()
    }
  } catch { /* ignore */ }
}

function startTimer() {
  timerInterval = setInterval(() => {
    remainingSeconds.value--
    if (remainingSeconds.value <= 0) {
      clearInterval(timerInterval!)
      ElMessage.warning('时间到，自动交卷')
      doSubmit()
    }
  }, 1000)
}

function scrollToQuestion(id: string) {
  questionRefs[id]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function handleSubmit() {
  submitDialogVisible.value = true
}

async function doSubmit() {
  if (!paper.value || submitting.value) return
  submitting.value = true
  submitDialogVisible.value = false
  try {
    if (timerInterval) clearInterval(timerInterval)

    const answers = Object.entries(userAnswers).map(([questionId, answer]) => ({
      questionId,
      userAnswer: answer
    }))

    const { data } = await submitQuiz(paperId, {
      startTime: startTime.value || new Date().toISOString(),
      answers
    })

    localStorage.removeItem(`quiz_backup_${paperId}`)
    router.replace(`/app/test-paper/${paperId}/quiz/result/${data.data.resultId}`)
  } catch {
    ElMessage.error('交卷失败，请稍后重试')
    submitDialogVisible.value = true
  } finally {
    submitting.value = false
  }
}

async function fetchPaper() {
  loading.value = true
  try {
    const { data } = await getQuizPaper(paperId)
    paper.value = data.data
    remainingSeconds.value = (data.data.durationMinutes || 30) * 60
    startTime.value = new Date().toISOString()
  } finally {
    loading.value = false
  }
}

async function fetchResult() {
  const rid = route.params.resultId as string | undefined
  if (!rid) return
  resultLoading.value = true
  try {
    const { data } = await getQuizResultById(rid)
    result.value = data.data
    paper.value = {
      paperId: '',
      testPaperId: data.data.testPaperId || '',
      documentId: data.data.documentId || '',
      docName: data.data.docName || '',
      totalScore: data.data.totalScore,
      totalCount: data.data.totalCount,
      durationMinutes: 0,
      questions: []
    }
  } finally {
    resultLoading.value = false
  }
}

onMounted(async () => {
  if (mode.value === 'quiz') {
    loadFromLocal()
    if (!paper.value) {
      await fetchPaper()
      paper.value && startTimer()
    } else {
      startTimer()
    }
  } else {
    await fetchResult()
  }
})

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval)
})

watch(() => route.params.resultId, async (newVal) => {
  if (newVal) {
    await fetchResult()
  }
})
</script>

<style scoped>
/* ---- Quiz header ---- */
.quiz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
  padding: 14px 20px;
  background: linear-gradient(135deg, #fafbfe 0%, #f3f5ff 100%);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
}
.quiz-header-left, .quiz-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.quiz-doc-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -.01em;
}
.timer {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.timer.warning {
  color: var(--danger);
  animation: pulse-warn 1s ease-in-out infinite;
}
@keyframes pulse-warn {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.progress-text {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* ---- Question blocks ---- */
.questions-panel {
  margin-bottom: var(--space-4);
}
.question-group {
  margin-bottom: var(--space-8);
}
.group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 var(--space-4) 0;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  letter-spacing: -.01em;
  display: inline-block;
}
/* Question type colors */
.group-title.type-SINGLE { background: #efeffc; color: #4f4fd9; }
.group-title.type-MULTI  { background: #e8f8f4; color: #0ea882; }
.group-title.type-JUDGE  { background: #fff8f2; color: #d97706; }
.group-title.type-FILL   { background: #fef2f6; color: #e4547c; }
.group-title.type-SHORT  { background: #f3f0ff; color: #7c3aed; }

.question-block {
  padding: 20px 24px;
  margin-bottom: 14px;
  border: 1px solid var(--border-default);
  border-left: 3px solid var(--border-default);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: var(--bg-surface);
  transition: border-color .15s;
}
.question-block:hover {
  border-color: var(--border-focus);
  border-left-color: var(--border-focus);
}
/* Type-colored left border per question */
.question-block.qtype-SINGLE { border-left-color: #c5c5f2; }
.question-block.qtype-SINGLE:hover { border-left-color: #4f4fd9; }
.question-block.qtype-MULTI  { border-left-color: #a3e4d2; }
.question-block.qtype-MULTI:hover  { border-left-color: #0ea882; }
.question-block.qtype-JUDGE  { border-left-color: #f5d4a8; }
.question-block.qtype-JUDGE:hover  { border-left-color: #d97706; }
.question-block.qtype-FILL   { border-left-color: #f5c2d4; }
.question-block.qtype-FILL:hover   { border-left-color: #e4547c; }
.question-block.qtype-SHORT  { border-left-color: #d5c8f5; }
.question-block.qtype-SHORT:hover  { border-left-color: #7c3aed; }

.question-stem {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.7;
}
.q-num {
  color: var(--accent);
  font-weight: 700;
  font-size: 17px;
  min-width: 32px;
}
.q-options {
  margin-left: 36px;
}
.option-row {
  margin-bottom: 10px;
  padding: 6px 0;
  font-size: 15px;
}
.option-row :deep(.el-radio),
.option-row :deep(.el-checkbox) {
  font-size: 15px;
  height: auto;
  padding: 4px 0;
}
.option-row :deep(.el-radio__label),
.option-row :deep(.el-checkbox__label) {
  font-size: 15px;
  padding-left: 10px;
}

/* ---- Answer sheet ---- */
.answer-sheet {
  position: sticky;
  top: 24px;
}
.sheet-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.sheet-item {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all .15s ease;
  border: 2px solid transparent;
}
.sheet-item.answered {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: var(--accent);
}
.sheet-item:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.sheet-legend {
  display: flex;
  gap: var(--space-4);
  margin-top: var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
.answered-dot { background: var(--accent-soft); border: 2px solid var(--accent); }
.unanswered-dot { background: var(--bg-muted); border: 1px solid var(--border-default); }
.submit-btn {
  width: 100%;
  margin-top: var(--space-4);
}

/* ---- Result ---- */
.result-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}
.result-title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -.02em;
}
.summary-stats {
  text-align: center;
}
.stat-box {
  padding: var(--space-6);
}
.stat-num {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -.02em;
  line-height: 1.1;
}
.stat-num.primary { color: var(--accent); }
.stat-num.success { color: var(--success); }
.stat-num.warning { color: var(--warning); }
.stat-label {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  margin-top: var(--space-2);
  font-weight: 500;
}
.result-details {
  margin-top: var(--space-6);
}
.detail-item {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: var(--space-5);
  margin-bottom: var(--space-3);
  background: var(--bg-surface);
}
.detail-item.correct {
  border-left: 3px solid var(--success);
}
.detail-item.wrong {
  border-left: 3px solid var(--danger);
}
.detail-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.mark {
  font-weight: 600;
  font-size: var(--text-sm);
  display: flex;
  align-items: center;
  gap: 3px;
}
.correct-mark { color: var(--success); }
.wrong-mark { color: var(--danger); }
.detail-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: var(--space-2);
  color: var(--text-primary);
  line-height: 1.6;
}
.detail-options {
  margin-bottom: var(--space-3);
}
.result-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px var(--space-3);
  margin-bottom: 4px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  border: 1px solid var(--border-subtle);
  font-size: var(--text-sm);
  transition: none;
}
.result-option.option-correct {
  background: var(--success-soft);
  border-color: #b7d9c4;
}
.result-option.option-user-wrong {
  background: var(--danger-soft);
  border-color: #f5c2c2;
}
.opt-label {
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 22px;
  font-size: var(--text-sm);
}
.opt-text {
  flex: 1;
  color: var(--text-primary);
}
.opt-icon-correct { color: var(--success); }
.opt-icon-wrong-result { color: var(--danger); }
.detail-answers {
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
}
.user-answer, .correct-answer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 3px;
}
.label {
  color: var(--text-tertiary);
}
.correct-text {
  color: var(--success);
  font-weight: 500;
}
.icon-correct { color: var(--success); }
.icon-wrong { color: var(--danger); }
.detail-analysis {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
}

/* ---- Passage ---- */
.quiz-passage {
  margin: var(--space-2) 0 var(--space-3) 36px;
  padding: var(--space-4);
  background: linear-gradient(135deg, #f8f9fd 0%, #f0f2fc 100%);
  border-left: 3px solid var(--accent);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.quiz-passage-label {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--accent);
  margin-bottom: var(--space-1);
  text-transform: uppercase;
  letter-spacing: .08em;
}
.quiz-passage-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.8;
  margin: 0;
  text-indent: 2em;
}

/* ---- Grading overlay ---- */
.grading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 23, 26, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}
.grading-box {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: 48px 64px;
  text-align: center;
  border: 1px solid var(--border-default);
}
.grading-spinner {
  color: var(--text-tertiary);
  animation: grading-spin 1s linear infinite;
}
@keyframes grading-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.grading-text {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: var(--space-5) 0 var(--space-2) 0;
  letter-spacing: -.01em;
}
.grading-sub {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}
</style>
