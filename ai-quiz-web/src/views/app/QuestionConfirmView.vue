<template>
  <div class="question-confirm">
    <el-card>
      <div class="header-bar">
        <div>
          <el-button @click="$router.push('/app/document')">
            <el-icon><ArrowLeft /></el-icon>返回文档列表
          </el-button>
          <span class="doc-title">文档：{{ docName }}</span>
        </div>
        <div>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable class="filter-select" @change="onFilterChange">
            <el-option label="全部" :value="undefined" />
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
          </el-select>
          <el-select v-model="filterType" placeholder="题型筛选" clearable class="filter-select" @change="onFilterChange">
            <el-option label="全部题型" value="" />
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="简答题" value="SHORT" />
          </el-select>
          <el-button @click="handleSelectAll">全选 / 取消全选</el-button>
          <el-button type="warning" @click="handleRegenerate">
            <el-icon><Refresh /></el-icon>重新生成
          </el-button>
          <el-button type="info" @click="handleCancelGenerate">
            取消生成
          </el-button>
          <el-button
            type="success"
            :disabled="selectedIds.length === 0"
            @click="handleBatchConfirm"
          >
            批量确认 ({{ selectedIds.length }})
          </el-button>
          <el-button
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>

      <div v-loading="loading" class="questions-container">
        <template v-if="groupedQuestions.length === 0">
          <el-empty description="暂未生成题目" />
        </template>

        <template v-for="group in groupedQuestions" :key="group.type">
          <div class="type-section">
            <h3 class="type-header">
              <el-checkbox
                :model-value="isTypeAllSelected(group.type)"
                :indeterminate="isTypeIndeterminate(group.type)"
                @change="(val: boolean) => handleTypeSelectAll(group.type, val)"
              />
              {{ group.label }}（{{ group.questions.length }}题）
            </h3>

            <QuestionCard
              v-for="(q, idx) in group.questions"
              :key="q.id"
              :question="q"
              :index="(page - 1) * pageSize + idx + 1"
              :selectable="q.status !== 1"
              :selected="selectedIds.includes(q.id!)"
              :show-actions="true"
              @toggle-select="toggleSelect"
              @edit="handleEditQuestion"
              @delete="handleDeleteSingle"
            />
          </div>
        </template>
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchQuestions"
        @current-change="fetchQuestions"
        class="pagination"
      />
    </el-card>

    <QuestionForm
      v-model:visible="formVisible"
      :question="editingQuestion"
      @submit="handleSaveQuestion"
    />

    <el-dialog v-model="regenerateVisible" title="重新生成题目" width="420px" destroy-on-close>
      <div class="config-section">
        <div class="config-label">生成数量</div>
        <el-radio-group v-model="genCount">
          <el-radio-button :value="20">20</el-radio-button>
          <el-radio-button :value="50">50</el-radio-button>
          <el-radio-button :value="60">60</el-radio-button>
        </el-radio-group>
      </div>
      <div class="config-section" style="margin-top:16px;">
        <el-checkbox v-model="includeFill">包含填空题</el-checkbox>
      </div>
      <div class="config-section" style="margin-top:16px;">
        <div class="config-label">出题方向（选填）</div>
        <el-input
          v-model="direction"
          type="textarea"
          :rows="3"
          placeholder="例如：文科可围绕知识点生成阅读理解类题型；理科可出冷门易错题型，侧重公式推导和概念辨析"
        />
      </div>
      <template #footer>
        <el-button @click="regenerateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRegenerateConfirm" :loading="regenerating">
          开始生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import * as questionApi from '@/api/question'
import { getDocumentById } from '@/api/document'
import { getQuestionTypeLabel } from '@/utils'
import type { QuestionItem } from '@/types'
import QuestionCard from '@/components/QuestionCard.vue'
import QuestionForm from '@/components/QuestionForm.vue'

const route = useRoute()
const router = useRouter()
const docId = route.params.docId as string
const docName = ref('')
const loading = ref(false)

const questions = ref<QuestionItem[]>([])
const selectedIds = ref<string[]>([])
const filterStatus = ref<number | undefined>(undefined)
const filterType = ref('')
const page = ref(1)
const pageSize = ref(50)
const total = ref(0)

const formVisible = ref(false)
const editingQuestion = ref<QuestionItem | null>(null)

const regenerateVisible = ref(false)
const regenerating = ref(false)
const genCount = ref(20)
const includeFill = ref(false)
const direction = ref('')

const groupedQuestions = computed(() => {
  const types = ['SINGLE', 'MULTI', 'JUDGE', 'FILL', 'SHORT']
  return types
    .map(type => ({
      type,
      label: getQuestionTypeLabel(type),
      questions: questions.value.filter(q => q.questionType === type)
    }))
    .filter(g => g.questions.length > 0)
})

function onFilterChange() {
  page.value = 1
  fetchQuestions()
}

function toggleSelect(id: string) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function getSelectableIds(type?: string) {
  return questions.value
    .filter(q => q.status !== 1)
    .filter(q => !type || q.questionType === type)
    .map(q => q.id!)
}

function isTypeAllSelected(type: string) {
  const ids = getSelectableIds(type)
  return ids.length > 0 && ids.every(id => selectedIds.value.includes(id))
}

function isTypeIndeterminate(type: string) {
  const ids = getSelectableIds(type)
  const selected = ids.filter(id => selectedIds.value.includes(id)).length
  return selected > 0 && selected < ids.length
}

function handleTypeSelectAll(type: string, checked: boolean) {
  const ids = getSelectableIds(type)
  if (checked) {
    ids.forEach(id => {
      if (!selectedIds.value.includes(id)) {
        selectedIds.value.push(id)
      }
    })
  } else {
    selectedIds.value = selectedIds.value.filter(id => !ids.includes(id))
  }
}

function handleSelectAll() {
  const allIds = getSelectableIds()
  const allSelected = allIds.every(id => selectedIds.value.includes(id))
  if (allSelected) {
    selectedIds.value = selectedIds.value.filter(id => !allIds.includes(id))
  } else {
    allIds.forEach(id => {
      if (!selectedIds.value.includes(id)) {
        selectedIds.value.push(id)
      }
    })
  }
}

async function fetchQuestions() {
  loading.value = true
  try {
    const { data: docData } = await getDocumentById(docId)
    docName.value = docData.data.docName

    const { data } = await questionApi.getDocumentQuestions(docId, {
      status: filterStatus.value,
      questionType: filterType.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    questions.value = data.data.records
    total.value = data.data.total
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function handleEditQuestion(q: QuestionItem) {
  editingQuestion.value = q
  formVisible.value = true
}

async function handleSaveQuestion(data: QuestionItem) {
  if (data.id) {
    await questionApi.updateQuestion(data.id, data)
    ElMessage.success('题目已更新')
  }
  formVisible.value = false
  fetchQuestions()
}

async function handleDeleteSingle(q: QuestionItem) {
  await ElMessageBox.confirm('确认删除此题？', '提示', { type: 'warning' })
  await questionApi.deleteQuestion(q.id!)
  ElMessage.success('已删除')
  fetchQuestions()
}

async function handleBatchConfirm() {
  await ElMessageBox.confirm(
    `确认 ${selectedIds.value.length} 道题目？确认后自动生成试题。`,
    '提示',
    { type: 'info' }
  )
  try {
    const { data } = await questionApi.batchConfirmQuestions(selectedIds.value)
    const paperId = data.data?.paperId
    if (paperId) {
      ElMessage.success('确认成功，试题已生成！可前往试题列表查看。')
    } else {
      ElMessage.success('确认成功')
    }
    selectedIds.value = []
    fetchQuestions()
  } catch { /* ignore */ }
}

function handleRegenerate() {
  genCount.value = 20
  includeFill.value = false
  direction.value = ''
  regenerateVisible.value = true
}

async function handleRegenerateConfirm() {
  regenerating.value = true
  try {
    await questionApi.generateQuestions(docId, {
      totalCount: genCount.value,
      includeFill: includeFill.value,
      typeRatios: { single: 50, multi: 30, judge: 20 },
      direction: direction.value || undefined
    })
    ElMessage.success('题目已重新生成')
    regenerateVisible.value = false
    selectedIds.value = []
    fetchQuestions()
  } finally {
    regenerating.value = false
  }
}

async function handleCancelGenerate() {
  await ElMessageBox.confirm(
    '确认取消生成？所有题目将被删除，该操作不可恢复。',
    '提示',
    { type: 'warning' }
  )
  await questionApi.cancelGenerateQuestions(docId)
  ElMessage.success('已取消生成')
  router.push('/app/document')
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(
    `确认删除 ${selectedIds.value.length} 道题目？删除不可恢复。`,
    '提示',
    { type: 'warning' }
  )
  await questionApi.batchDeleteQuestions(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchQuestions()
}

onMounted(fetchQuestions)
</script>

<style scoped>
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.doc-title {
  font-size: 16px;
  font-weight: 600;
  margin-left: 12px;
  color: var(--text-primary);
}
.filter-select {
  width: 130px;
  margin-right: 8px;
}
.type-section {
  margin-bottom: 24px;
}
.type-header {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--accent);
}
.config-section .config-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 10px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
