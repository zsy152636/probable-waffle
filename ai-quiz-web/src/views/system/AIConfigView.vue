<template>
  <div class="ai-config">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd" v-if="hasPerm('ai:add')">
          <el-icon><Plus /></el-icon>新增配置
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" border>
        <el-table-column prop="providerName" label="厂商" width="120" />
        <el-table-column prop="apiUrl" label="API地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" width="140" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="100" align="center" />
        <el-table-column prop="temperature" label="Temperature" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)" v-if="hasPerm('ai:update')">修改</el-button>
            <el-button type="warning" link @click="handleActivate(row)" v-if="hasPerm('ai:update') && row.isActive !== 1">启用</el-button>
            <el-button type="info" link @click="handleDeactivate(row)" v-if="hasPerm('ai:update') && row.isActive === 1">禁用</el-button>
            <el-button type="success" link @click="handleTest(row)" v-if="hasPerm('ai:test')">测试连接</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="hasPerm('ai:delete')">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改配置' : '新增配置'"
      width="560px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="厂商名称" prop="providerName">
          <el-input v-model="form.providerName" placeholder="如 OpenAI / DeepSeek / 通义千问" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="如 https://api.deepseek.com/v1/chat/completions" />
        </el-form-item>
        <el-form-item label="API Token" prop="apiToken">
          <el-input v-model="form.apiToken" type="password" show-password placeholder="sk-xxxxxxxx" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 deepseek-chat / gpt-4o" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="256" :max="32768" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '新增' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="testDialogVisible"
      :title="testDialogTitle"
      width="560px"
      destroy-on-close
    >
      <div style="margin-bottom:12px;color:var(--text-tertiary);">大模型回复：</div>
      <div v-if="testLoading" style="text-align:center;padding:24px;">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p style="margin-top:8px;">正在测试连接...</p>
      </div>
      <el-input
        v-else
        type="textarea"
        :rows="6"
        :model-value="testResult"
        readonly
      />
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import * as aiConfigApi from '@/api/aiConfig'
import { hasPerm } from '@/utils'
import type { AIConfigItem, AIConfigForm } from '@/types'

const loading = ref(false)
const tableData = ref<AIConfigItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const testDialogVisible = ref(false)
const testDialogTitle = ref('')
const testResult = ref('')
const testLoading = ref(false)

const initForm = (): AIConfigForm => ({
  providerName: '',
  apiUrl: '',
  apiToken: '',
  modelName: '',
  maxTokens: 4096,
  temperature: 0.7
})
const form = reactive<AIConfigForm>(initForm())

const rules: FormRules = {
  providerName: [{ required: true, message: '请输入厂商名称' }],
  apiUrl: [{ required: true, message: '请输入API地址' }],
  apiToken: [{ required: true, message: '请输入API Token' }],
  modelName: [{ required: true, message: '请输入模型名称' }]
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await aiConfigApi.getAIConfigs({ page: page.value, pageSize: pageSize.value })
    tableData.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editingId.value = ''
  Object.assign(form, initForm())
  dialogVisible.value = true
}

function handleEdit(row: AIConfigItem) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    providerName: row.providerName,
    apiUrl: row.apiUrl,
    apiToken: row.apiToken,
    modelName: row.modelName,
    maxTokens: row.maxTokens,
    temperature: row.temperature
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await aiConfigApi.updateAIConfig(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await aiConfigApi.addAIConfig(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleActivate(row: AIConfigItem) {
  try {
    await aiConfigApi.activateAIConfig(row.id)
    ElMessage.success(`已启用「${row.providerName}」`)
    fetchList()
  } catch { /* error already shown by interceptor */ }
}

async function handleDeactivate(row: AIConfigItem) {
  try {
    await aiConfigApi.deactivateAIConfig(row.id)
    ElMessage.success(`已禁用「${row.providerName}」`)
    fetchList()
  } catch { /* error already shown by interceptor */ }
}

async function handleTest(row: AIConfigItem) {
  testDialogTitle.value = `测试连接 - ${row.providerName}`
  testLoading.value = true
  testResult.value = ''
  testDialogVisible.value = true
  try {
    const { data } = await aiConfigApi.testAIConnection(row.id)
    testResult.value = (data.data as string) || '(未收到回复)'
  } catch {
    testResult.value = '连接失败，请检查配置信息'
  } finally {
    testLoading.value = false
  }
}

async function handleDelete(row: AIConfigItem) {
  await ElMessageBox.confirm(`确认删除配置「${row.providerName}」？`, '提示', { type: 'warning' })
  await aiConfigApi.deleteAIConfig(row.id)
  ElMessage.success('删除成功')
  fetchList()
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
</style>
