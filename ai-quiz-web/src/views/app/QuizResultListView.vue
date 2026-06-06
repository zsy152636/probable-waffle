<template>
  <div class="quiz-result-list">
    <el-card>
      <div class="toolbar">
        <div></div>
        <el-input
          v-model="keyword"
          placeholder="搜索试题名称..."
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
        <el-table-column prop="docName" label="试题名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="得分" width="120" align="center">
          <template #default="{ row }">
            <span :class="scoreClass(row)">{{ row.userScore }} / {{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="答对题数" width="120" align="center">
          <template #default="{ row }">{{ row.correctCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="用时" width="100" align="center">
          <template #default="{ row }">{{ formatSeconds(row.durationSeconds) }}</template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="$router.push(`/app/test-paper/${row.testPaperId}/quiz/result/${row.id}`)"
            >
              查看
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getQuizResults, deleteQuizResult } from '@/api/quiz'
import { formatSeconds } from '@/utils'
import type { QuizResultItem } from '@/types'

const loading = ref(false)
const tableData = ref<QuizResultItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

function scoreClass(row: QuizResultItem) {
  const ratio = row.userScore / row.totalScore
  if (ratio >= 0.8) return 'score-good'
  if (ratio >= 0.6) return 'score-ok'
  return 'score-low'
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await getQuizResults({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    tableData.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: QuizResultItem) {
  await ElMessageBox.confirm(`确认删除「${row.docName}」的答题记录？`, '提示', { type: 'warning' })
  await deleteQuizResult(row.id)
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
.score-good { color: #67c23a; font-weight: 600; }
.score-ok { color: #e6a23c; font-weight: 600; }
.score-low { color: #f56c6c; font-weight: 600; }
</style>
