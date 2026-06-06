<template>
  <div class="test-paper-list">
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
        <el-table-column prop="paperNo" label="试题编号" width="200" show-overflow-tooltip />
        <el-table-column prop="paperName" label="试题名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="单选题" width="80" align="center">
          <template #default="{ row }">{{ row.singleCount }}</template>
        </el-table-column>
        <el-table-column label="多选题" width="80" align="center">
          <template #default="{ row }">{{ row.multiCount }}</template>
        </el-table-column>
        <el-table-column label="判断题" width="80" align="center">
          <template #default="{ row }">{{ row.judgeCount }}</template>
        </el-table-column>
        <el-table-column label="填空题" width="80" align="center">
          <template #default="{ row }">{{ row.fillCount }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" @click="$router.push(`/app/test-paper/${row.id}/quiz`)">
              测试
            </el-button>
            <el-button
              type="danger" link
              @click="handleDelete(row)"
              v-if="hasPerm('paper:delete')"
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
import * as tpApi from '@/api/testPaper'
import { hasPerm } from '@/utils'
import type { TestPaperItem } from '@/types'

const loading = ref(false)
const tableData = ref<TestPaperItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function fetchList() {
  loading.value = true
  try {
    const { data } = await tpApi.getTestPapers({
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

async function handleDelete(row: TestPaperItem) {
  await ElMessageBox.confirm(`确认删除试题「${row.paperName}」？`, '提示', { type: 'warning' })
  await tpApi.deleteTestPaper(row.id)
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
