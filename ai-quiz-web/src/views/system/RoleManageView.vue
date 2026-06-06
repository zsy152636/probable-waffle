<template>
  <div class="role-manage">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd" v-if="hasPerm('role:add')">
          <el-icon><Plus /></el-icon>新增角色
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="180" show-overflow-tooltip />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)" v-if="hasPerm('role:update')">修改</el-button>
            <el-button type="warning" link @click="handleAssignPerm(row)" v-if="hasPerm('role:assignPerm')">分配权限</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="hasPerm('role:delete')">删除</el-button>
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
      :title="isEdit ? '修改角色' : '新增角色'"
      width="450px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '新增' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" title="分配权限" width="500px" destroy-on-close>
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerms" :loading="savingPerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as roleApi from '@/api/role'
import { useUserStore } from '@/stores/user'
import { hasPerm } from '@/utils'
import type { RoleInfo, RoleForm, MenuItem } from '@/types'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref<RoleInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const initForm = (): RoleForm => ({
  roleName: '',
  roleCode: '',
  description: ''
})
const form = reactive<RoleForm>(initForm())

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const permDialogVisible = ref(false)
const checkedMenuIds = ref<number[]>([])
const savingPerms = ref(false)
const currentRoleId = ref('')
const treeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<MenuItem[]>([])

async function fetchList() {
  loading.value = true
  try {
    const { data } = await roleApi.getRoles({ page: page.value, pageSize: pageSize.value })
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

function handleEdit(row: RoleInfo) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await roleApi.updateRole(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await roleApi.addRole(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: RoleInfo) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await roleApi.deleteRole(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

async function handleAssignPerm(row: RoleInfo) {
  currentRoleId.value = row.id
  menuTree.value = userStore.menus
  try {
    const { data } = await roleApi.getRoleMenus(row.id)
    checkedMenuIds.value = data.data
  } catch {
    checkedMenuIds.value = []
  }
  permDialogVisible.value = true
}

async function handleSavePerms() {
  const keys = treeRef.value?.getCheckedKeys(false) as number[]
  savingPerms.value = true
  try {
    await roleApi.assignRoleMenus(currentRoleId.value, { menuIds: keys || [] })
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } finally {
    savingPerms.value = false
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
</style>
