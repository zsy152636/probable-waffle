<template>
  <div class="question-gen-config">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增配置
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" border>
        <el-table-column prop="name" label="配置名称" min-width="160" />
        <el-table-column label="题型组合" min-width="300">
          <template #default="{ row }">
            <el-tag
              v-for="t in (row.types || [])"
              :key="t.questionType"
              size="small"
              class="type-tag"
            >
              {{ getTypeLabel(t.questionType) }} ×{{ t.count }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总题数" width="80" align="center" />
        <el-table-column label="阅读材料" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.needPassage === 1 ? 'warning' : 'info'" size="small">
              {{ row.needPassage === 1 ? '需要' : '不需要' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="620px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="如：政考标准配置" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="简要描述此配置的用途" />
        </el-form-item>

        <el-form-item label="题型明细">
          <div class="type-table">
            <div
              v-for="(row, idx) in form.types"
              :key="idx"
              class="type-row"
            >
              <el-select v-model="row.questionType" class="type-select" @change="onTypesChange">
                <el-option
                  v-for="qt in allQuestionTypes"
                  :key="qt.value"
                  :label="qt.label"
                  :value="qt.value"
                />
              </el-select>
              <el-input-number
                v-model="row.count"
                :min="0"
                :max="100"
                size="small"
                class="type-count"
                @change="onTypesChange"
              />
              <span class="type-unit">道</span>
              <el-button
                v-if="(form.types || []).length > 1"
                type="danger" link size="small"
                @click="removeType(idx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button
              v-if="(form.types || []).length < 7"
              type="primary" link
              @click="addType"
              class="add-type-btn"
            >
              + 添加题型
            </el-button>
            <div class="type-total">
              总题数：<strong>{{ computedTotal }}</strong> 道
            </div>
          </div>
        </el-form-item>

        <el-form-item label="强制阅读材料">
          <el-switch v-model="form.needPassage" :active-value="1" :inactive-value="0" />
          <span class="form-hint">启用后，在出题方向中体现选择题需附带passage</span>
        </el-form-item>

        <el-form-item label="出题方向">
          <el-input
            v-model="form.direction"
            type="textarea"
            :rows="4"
            placeholder="如：侧重政策理解与分析，选择题需附带约200字阅读材料..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { GenConfigItem, GenConfigTypeItem, QuestionType } from '@/types'
import * as configApi from '@/api/questionGenConfig'
import { getQuestionTypeLabel } from '@/utils'

const TYPE_LABEL_MAP: Record<string, string> = {
  SINGLE: '选择题', MULTI: '多选题', JUDGE: '判断题',
  FILL: '填空题', SHORT: '简答题', CODE: '代码题', CALCULATION: '计算大题'
}

const allQuestionTypes = Object.entries(TYPE_LABEL_MAP).map(([value, label]) => ({ value, label }))

const loading = ref(false)
const tableData = ref<GenConfigItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

function blankTypes(): GenConfigTypeItem[] {
  return [{ questionType: 'SINGLE' as QuestionType, count: 0 }]
}

const form = reactive<GenConfigItem>({
  name: '',
  description: '',
  totalCount: 0,
  needPassage: 0,
  direction: '',
  sort: 0,
  isActive: 1,
  types: blankTypes()
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }]
}

const computedTotal = computed(() =>
  (form.types || []).reduce((sum, t) => sum + (t.count || 0), 0)
)

function onTypesChange() {
  form.totalCount = computedTotal.value
}

function addType() {
  form.types!.push({ questionType: 'SINGLE' as QuestionType, count: 0 })
}

function removeType(idx: number) {
  form.types!.splice(idx, 1)
  onTypesChange()
}

function getTypeLabel(type: string) {
  return getQuestionTypeLabel(type)
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await configApi.listConfigs({ pageSize: 100 })
    tableData.value = data.data.records
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editingId.value = ''
  Object.assign(form, {
    name: '',
    description: '',
    totalCount: 0,
    needPassage: 0,
    direction: '',
    sort: 0,
    isActive: 1,
    types: blankTypes()
  })
  dialogVisible.value = true
}

function handleEdit(row: GenConfigItem) {
  isEdit.value = true
  editingId.value = row.id!
  Object.assign(form, {
    name: row.name,
    description: row.description,
    totalCount: row.totalCount,
    needPassage: row.needPassage,
    direction: row.direction || '',
    sort: row.sort,
    isActive: row.isActive,
    types: (row.types && row.types.length > 0)
      ? row.types.map(t => ({ questionType: t.questionType, count: t.count, id: t.id }))
      : blankTypes()
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  // 过滤掉 count=0 的题型
  form.types = (form.types || []).filter(t => t.count > 0)
  if (form.types.length === 0) {
    ElMessage.warning('至少需要设置一种题型')
    return
  }
  form.totalCount = computedTotal.value

  submitting.value = true
  try {
    if (isEdit.value) {
      await configApi.updateConfig(editingId.value, JSON.parse(JSON.stringify(form)))
      ElMessage.success('配置已更新')
    } else {
      await configApi.saveConfig(JSON.parse(JSON.stringify(form)))
      ElMessage.success('配置已创建')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: GenConfigItem) {
  await ElMessageBox.confirm(`确认删除配置「${row.name}」？`, '提示', { type: 'warning' })
  await configApi.deleteConfig(row.id!)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.type-tag { margin-right: 6px; margin-bottom: 4px; }
.type-table {
  width: 100%;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  background: var(--bg-muted);
}
.type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.type-select { width: 140px; }
.type-count { width: 120px; }
.type-unit { color: var(--text-tertiary); font-size: 13px; }
.add-type-btn { margin-top: 4px; }
.type-total {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
  font-size: 14px;
  color: var(--text-secondary);
}
.form-hint {
  margin-left: 8px;
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>
