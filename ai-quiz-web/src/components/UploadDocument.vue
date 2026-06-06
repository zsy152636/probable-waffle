<template>
  <el-dialog
    :model-value="visible"
    title="上传文档"
    width="480px"
    @update:model-value="$emit('update:visible', $event)"
    destroy-on-close
  >
    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      multiple
      :auto-upload="false"
      :accept="'.txt,.docx,.pdf,.md'"
      :on-change="handleFileChange"
      :on-remove="handleRemove"
      :file-list="fileList"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="upload-text">将文件拖到此处，或点击上传</div>
      <template #tip>
        <div class="upload-tip">支持 TXT / Word(.docx) / PDF / Markdown(.md) 格式，单个文件不超过 20MB</div>
      </template>
    </el-upload>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="files.length === 0">
        上传
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { uploadDocuments } from '@/api/document'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  success: []
}>()

const files = ref<File[]>([])
const fileList = ref<UploadFile[]>([])
const uploadRef = ref<UploadInstance>()
const uploading = ref(false)

const allowedExt = ['txt', 'docx', 'pdf', 'md']

function handleFileChange(uploadFile: UploadFile) {
  const f = uploadFile.raw
  if (!f) return
  const maxSize = 20 * 1024 * 1024
  if (f.size > maxSize) {
    ElMessage.error(`「${f.name}」超过 20MB 限制`)
    fileList.value = fileList.value.filter(item => item.uid !== uploadFile.uid)
    return
  }
  const ext = f.name.split('.').pop()?.toLowerCase()
  if (!allowedExt.includes(ext || '')) {
    ElMessage.error(`「${f.name}」格式不支持`)
    fileList.value = fileList.value.filter(item => item.uid !== uploadFile.uid)
    return
  }
  files.value.push(f)
}

function handleRemove(uploadFile: UploadFile) {
  files.value = files.value.filter(f => f.name !== uploadFile.name || f.size !== uploadFile.size)
}

async function handleUpload() {
  if (files.value.length === 0) return
  uploading.value = true
  try {
    const formData = new FormData()
    files.value.forEach(f => formData.append('files', f))
    await uploadDocuments(formData)
    ElMessage.success(`成功上传 ${files.value.length} 个文件`)
    emit('success')
    emit('update:visible', false)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-area {
  width: 100%;
}
.upload-icon {
  font-size: 48px;
  color: var(--highlight);
}
.upload-text {
  margin-top: 8px;
  color: var(--text-secondary);
}
.upload-tip {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 8px;
}
</style>
