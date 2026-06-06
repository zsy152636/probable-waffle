<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑题目' : '新增题目'"
    width="700px"
    @update:model-value="$emit('update:visible', $event)"
    destroy-on-close
  >
    <el-form :model="form" label-width="80px" ref="formRef" :rules="rules">
      <el-form-item label="题型" prop="questionType">
        <el-select v-model="form.questionType" :disabled="isEdit">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTI" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="填空题" value="FILL" />
          <el-option label="简答题" value="SHORT" />
        </el-select>
      </el-form-item>

      <el-form-item label="阅读材料">
        <el-input v-model="form.passage" type="textarea" :rows="3" placeholder="可选，阅读理解题的文章内容" />
      </el-form-item>

      <el-form-item label="题干" prop="questionTitle">
        <el-input v-model="form.questionTitle" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item
        v-if="hasOptions"
        label="选项"
        prop="options"
      >
        <div class="options-editor">
          <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
            <span class="option-label">{{ opt.label }}.</span>
            <el-input v-model="opt.text" class="option-input" />
            <el-checkbox v-model="opt.isCorrect">正确</el-checkbox>
            <el-button
              v-if="form.options.length > 2"
              type="danger" link size="small"
              @click="removeOption(idx)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button v-if="form.options.length < 6" type="primary" link @click="addOption">
            + 添加选项
          </el-button>
        </div>
      </el-form-item>

      <el-form-item v-if="form.questionType === 'FILL' || form.questionType === 'SHORT'" label="参考答案">
        <el-input v-model="form.options[0].text" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item v-if="form.questionType === 'JUDGE'" label="正确答案">
        <el-radio-group v-model="form.options[0].isCorrect">
          <el-radio :value="true">正确</el-radio>
          <el-radio :value="false">错误</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="解析" prop="analysis">
        <el-input v-model="form.analysis" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item label="难度">
        <el-rate v-model="form.difficulty" :max="3" show-text :texts="['简单', '中等', '困难']" />
      </el-form-item>

      <el-form-item label="分值">
        <el-input-number v-model="form.score" :min="1" :max="100" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ isEdit ? '保存' : '添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { QuestionItem, QuestionOption, QuestionType } from '@/types'

const props = defineProps<{
  visible: boolean
  question?: QuestionItem | null
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  submit: [data: QuestionItem]
}>()

const isEdit = computed(() => !!props.question?.id)

const defaultOptions = (): QuestionOption[] => [
  { label: 'A', text: '', isCorrect: false, sort: 0 },
  { label: 'B', text: '', isCorrect: false, sort: 1 },
  { label: 'C', text: '', isCorrect: false, sort: 2 },
  { label: 'D', text: '', isCorrect: false, sort: 3 }
]

const initForm = (): QuestionItem => ({
  documentId: '',
  questionType: 'SINGLE',
  questionTitle: '',
  passage: '',
  options: defaultOptions(),
  analysis: '',
  difficulty: 1,
  score: 5,
  sort: 0,
  status: 0
})

const form = reactive<QuestionItem>(initForm())
const formRef = ref<FormInstance>()
const submitting = ref(false)

const hasOptions = computed(() =>
  ['SINGLE', 'MULTI', 'JUDGE'].includes(form.questionType)
)

const rules: FormRules = {
  questionType: [{ required: true, message: '请选择题型' }],
  questionTitle: [{ required: true, message: '请输入题干' }]
}

const labels = 'ABCDEF'

function addOption() {
  form.options.push({
    label: labels[form.options.length],
    text: '',
    isCorrect: false,
    sort: form.options.length
  })
}

function removeOption(idx: number) {
  form.options.splice(idx, 1)
  form.options.forEach((opt, i) => {
    opt.label = labels[i]
    opt.sort = i
  })
}

watch(() => form.questionType, (type: QuestionType) => {
  if (type === 'JUDGE') {
    form.options = [
      { label: 'A', text: '正确', isCorrect: true, sort: 0 },
      { label: 'B', text: '错误', isCorrect: false, sort: 1 }
    ]
  } else if (type === 'FILL' || type === 'SHORT') {
    form.options = [{ label: '', text: '', isCorrect: true, sort: 0 }]
  } else {
    form.options = defaultOptions()
  }
})

watch(() => props.visible, (val) => {
  if (val && props.question) {
    Object.assign(form, JSON.parse(JSON.stringify(props.question)))
  } else if (val) {
    Object.assign(form, initForm())
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  emit('submit', JSON.parse(JSON.stringify(form)))
}
</script>

<style scoped>
.options-editor {
  width: 100%;
}
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.option-label {
  font-weight: 600;
  min-width: 28px;
}
.option-input {
  flex: 1;
}
</style>
