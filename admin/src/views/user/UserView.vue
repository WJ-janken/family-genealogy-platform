<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>用户管理</h2>
        <el-input v-model="keyword" placeholder="搜索昵称/手机号" clearable style="width: 220px" @keyup.enter="loadData" />
      </div>

      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)" size="small">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memberName" label="绑定成员" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="changeRole(row)">角色</el-button>
            <el-button :type="row.status === 1 ? 'danger' : 'success'" size="small" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </div>

    <!-- 角色分配对话框 -->
    <el-dialog v-model="showRoleDialog" title="分配角色" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">{{ currentUser?.nickname }}</el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRole" style="width: 100%">
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="成员" value="MEMBER" />
            <el-option label="访客" value="GUEST" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRole">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const userList = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const keyword = ref('')

const showRoleDialog = ref(false)
const currentUser = ref<any>(null)
const selectedRole = ref('')

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res: any = await service.get('/admin/users', { params })
    userList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

function changeRole(row: any) {
  currentUser.value = row
  selectedRole.value = row.role
  showRoleDialog.value = true
}

async function confirmRole() {
  try {
    await service.put(`/admin/users/${currentUser.value.id}/role`, { role: selectedRole.value })
    ElMessage.success('角色更新成功')
    showRoleDialog.value = false
    loadData()
  } catch (e) {}
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.nickname}」？`, '确认操作')
  try {
    await service.put(`/admin/users/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e) {}
}

function roleType(role: string) {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', ADMIN: 'warning', MEMBER: '', GUEST: 'info' }
  return map[role] || 'info'
}

function roleText(role: string) {
  const map: Record<string, string> = { SUPER_ADMIN: '超级管理员', ADMIN: '管理员', MEMBER: '成员', GUEST: '访客' }
  return map[role] || role
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; }
}
</style>
