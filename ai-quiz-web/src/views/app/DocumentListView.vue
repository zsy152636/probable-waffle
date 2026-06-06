<template>
  <div class="document-list">
    <el-card>
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="uploadVisible = true" v-if="hasPerm('doc:upload')">
            <el-icon><Upload /></el-icon>上传文档
          </el-button>
          <el-select
            v-model="docType"
            placeholder="文档类型"
            clearable
            style="width: 130px; margin-left: 10px"
            @change="onFilterChange"
          >
            <el-option label="全部" value="" />
            <el-option label="TXT" value="TXT" />
            <el-option label="Word" value="DOCX" />
            <el-option label="PDF" value="PDF" />
          </el-select>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索文档名称..."
          clearable
          style="width: 240px"
          @clear="fetchList"
          @keyup.enter="fetchList"
        >
          <template #append>
            <el-button @click="fetchList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" border>
        <el-table-column prop="docNo" label="文档编号" width="200" show-overflow-tooltip />
        <el-table-column label="文档名称" min-width="200">
          <template #default="{ row }">
            <div>
              <span>{{ row.docName }}</span>
              <el-tag v-if="row.hasPaper" type="success" size="small" class="tag" effect="plain">
                试题已生成
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="docTypeTag(row.docType)">{{ row.docType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题目进度" width="160" align="center">
          <template #default="{ row }">
            <template v-if="row.questionCount > 0">
              <span :style="{ color: row.confirmedCount === row.questionCount ? 'var(--success)' : 'var(--warning)' }">
                {{ row.confirmedCount }} / {{ row.questionCount }}
              </span>
              <span v-if="row.confirmedCount < row.questionCount" class="tag">
                <el-tag type="warning" size="small">待确认</el-tag>
              </span>
            </template>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="questionGenTime" label="题目生成时间" width="170">
          <template #default="{ row }">
            {{ row.questionGenTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadUserName" label="上传用户" width="100" />
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.questionCount === 0"
              type="success" link
              @click="handleGenerate(row)"
              v-show="hasPerm('doc:genQuestion')"
            >
              题目生成
            </el-button>
            <el-button
              v-if="row.questionCount > 0"
              type="warning" link
              @click="$router.push(`/app/document/${row.id}/questions`)"
              v-show="hasPerm('question:confirm')"
            >
              查看题目
            </el-button>
            <el-button
              type="primary" link
              @click="handleEdit(row)"
              v-if="hasPerm('doc:update')"
            >
              修改
            </el-button>
            <el-button
              type="danger" link
              @click="handleDelete(row)"
              v-if="hasPerm('doc:delete')"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchList"
        @current-change="fetchList"
        class="pagination"
      />
    </el-card>

    <UploadDocument v-model:visible="uploadVisible" @success="fetchList" />

    <el-dialog v-model="editVisible" title="修改文档" width="400px" destroy-on-close>
      <el-form :model="editForm" ref="editFormRef" :rules="editRules" label-width="80px">
        <el-form-item label="文档名称" prop="docName">
          <el-input v-model="editForm.docName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editSubmitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generateVisible" title="题目生成配置" width="520px" destroy-on-close>
      <div class="generate-config">
        <div class="config-section">
          <div class="config-label">生成数量</div>
          <el-radio-group v-model="genConfig.totalCount">
            <el-radio-button :value="20">20</el-radio-button>
            <el-radio-button :value="50">50</el-radio-button>
            <el-radio-button :value="60">60</el-radio-button>
          </el-radio-group>
        </div>

        <div class="config-section">
          <el-checkbox v-model="genConfig.includeFill">包含填空题</el-checkbox>
        </div>

        <div class="config-section">
          <div class="config-label">出题方向（选填）</div>
          <el-input
            v-model="genConfig.direction"
            type="textarea"
            :rows="3"
            placeholder="例如：文科可围绕知识点生成阅读理解类题型；理科可出冷门易错题型，侧重公式推导和概念辨析"
          />
        </div>

        <div class="config-section" v-if="!genConfig.includeFill">
          <div class="config-label">题型比例</div>
          <div class="ratio-sliders">
            <div class="ratio-item">
              <span class="ratio-name">单选题</span>
              <el-slider
                v-model="genConfig.typeRatios.single"
                :min="0"
                :max="100"
                show-input
                @input="onSingleChange"
              />
              <span class="ratio-count">{{ singleCount }}题</span>
            </div>
            <div class="ratio-item">
              <span class="ratio-name">多选题</span>
              <el-slider
                v-model="genConfig.typeRatios.multi"
                :min="0"
                :max="100"
                show-input
                @input="onMultiChange"
              />
              <span class="ratio-count">{{ multiCount }}题</span>
            </div>
            <div class="ratio-item">
              <span class="ratio-name">判断题</span>
              <el-slider
                v-model="genConfig.typeRatios.judge"
                :min="0"
                :max="100"
                show-input
                @input="onJudgeChange"
              />
              <span class="ratio-count">{{ judgeCount }}题</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartGenerate" :loading="generating">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Upload, Search } from '@element-plus/icons-vue'
import * as docApi from '@/api/document'
import { generateQuestions } from '@/api/question'
import { hasPerm } from '@/utils'
import type { DocumentItem, GenerateConfig } from '@/types'
import UploadDocument from '@/components/UploadDocument.vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref<DocumentItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const docType = ref('')

const uploadVisible = ref(false)
const editVisible = ref(false)
const editingDoc = ref<DocumentItem | null>(null)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({ docName: '' })
const editRules: FormRules = {
  docName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }]
}

const generateVisible = ref(false)
const generating = ref(false)
const currentGenDocId = ref('')

const genConfig = reactive<GenerateConfig>({
  totalCount: 20,
  includeFill: false,
  typeRatios: { single: 50, multi: 30, judge: 20 },
  direction: ''
})

const singleCount = computed(() => {
  const n = Math.floor(genConfig.totalCount * genConfig.typeRatios.single / 100)
  const m = Math.floor(genConfig.totalCount * genConfig.typeRatios.multi / 100)
  const j = Math.floor(genConfig.totalCount * genConfig.typeRatios.judge / 100)
  return n + (genConfig.totalCount - n - m - j)
})
const multiCount = computed(() => Math.floor(genConfig.totalCount * genConfig.typeRatios.multi / 100))
const judgeCount = computed(() => Math.floor(genConfig.totalCount * genConfig.typeRatios.judge / 100))

function onSingleChange(val: number) {
  const remain = 100 - val
  const ratio = genConfig.typeRatios.multi + genConfig.typeRatios.judge
  if (ratio > 0) {
    genConfig.typeRatios.multi = Math.round(remain * genConfig.typeRatios.multi / ratio)
    genConfig.typeRatios.judge = remain - genConfig.typeRatios.multi
  } else {
    genConfig.typeRatios.multi = Math.floor(remain / 2)
    genConfig.typeRatios.judge = remain - genConfig.typeRatios.multi
  }
}

function onMultiChange(val: number) {
  const remain = 100 - val
  const ratio = genConfig.typeRatios.single + genConfig.typeRatios.judge
  if (ratio > 0) {
    genConfig.typeRatios.single = Math.round(remain * genConfig.typeRatios.single / ratio)
    genConfig.typeRatios.judge = remain - genConfig.typeRatios.single
  } else {
    genConfig.typeRatios.single = Math.floor(remain / 2)
    genConfig.typeRatios.judge = remain - genConfig.typeRatios.single
  }
}

function onJudgeChange(val: number) {
  const remain = 100 - val
  const ratio = genConfig.typeRatios.single + genConfig.typeRatios.multi
  if (ratio > 0) {
    genConfig.typeRatios.single = Math.round(remain * genConfig.typeRatios.single / ratio)
    genConfig.typeRatios.multi = remain - genConfig.typeRatios.single
  } else {
    genConfig.typeRatios.single = Math.floor(remain / 2)
    genConfig.typeRatios.multi = remain - genConfig.typeRatios.single
  }
}

function docTypeTag(type: string) {
  const map: Record<string, string> = { TXT: '', DOCX: 'primary', PDF: 'danger', MD: 'success' }
  return map[type] || ''
}

function onFilterChange() {
  page.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await docApi.getDocuments({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      docType: docType.value || undefined
    })
    tableData.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function handleEdit(row: DocumentItem) {
  editingDoc.value = row
  editForm.docName = row.docName
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editFormRef.value || !editingDoc.value) return
  await editFormRef.value.validate()
  editSubmitting.value = true
  try {
    await docApi.updateDocument(editingDoc.value.id, { docName: editForm.docName })
    ElMessage.success('修改成功')
    editVisible.value = false
    fetchList()
  } finally {
    editSubmitting.value = false
  }
}

async function handleDelete(row: DocumentItem) {
  await ElMessageBox.confirm(`确认删除文档「${row.docName}」？删除将同时移除关联题目`, '提示', { type: 'warning' })
  await docApi.deleteDocument(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

function handleGenerate(row: DocumentItem) {
  currentGenDocId.value = row.id
  Object.assign(genConfig, {
    totalCount: 20,
    includeFill: false,
    typeRatios: { single: 50, multi: 30, judge: 20 },
    direction: ''
  })
  generateVisible.value = true
}

async function handleStartGenerate() {
  generating.value = true
  try {
    await generateQuestions(currentGenDocId.value, JSON.parse(JSON.stringify(genConfig)))
    ElMessage.success('题目生成成功，正在跳转确认页...')
    generateVisible.value = false
    router.push(`/app/document/${currentGenDocId.value}/questions`)
  } finally {
    generating.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.tag {
  margin-left: 8px;
}
.no-data {
  color: var(--text-tertiary);
}
.generate-config {
  padding: 8px 0;
}
.config-section {
  margin-bottom: 20px;
}
.config-label {
  font-weight: 600;
  margin-bottom: 10px;
}
.ratio-sliders {
  margin-top: 8px;
}
.ratio-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.ratio-name {
  font-weight: 500;
  min-width: 56px;
}
.ratio-item .el-slider {
  flex: 1;
}
.ratio-count {
  min-width: 36px;
  color: #909399;
  font-size: 13px;
}
</style>
