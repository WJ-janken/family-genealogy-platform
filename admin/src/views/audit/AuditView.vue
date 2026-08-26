<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>审核中心</h2>
        <div class="header-actions">
          <el-radio-group v-model="statusFilter" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待审核</el-radio-button>
            <el-radio-button label="APPROVED">已通过</el-radio-button>
            <el-radio-button label="REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-table :data="auditList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="submitterName" label="提交人" width="100" />
        <el-table-column prop="action" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="actionType(row.action)">{{ actionText(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标成员" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column prop="reviewComment" label="审核意见" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">驳回</el-button>
            </template>
            <el-button type="primary" size="small" link @click="viewDetail(row)">详情</el-button>
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

    <!-- 审核详情对话框 -->
    <el-dialog v-model="showDetail" title="审核详情" width="600px">
      <el-descriptions :column="2" border v-if="currentAudit">
        <el-descriptions-item label="提交人">{{ currentAudit.submitterName }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ actionText(currentAudit.action) }}</el-descriptions-item>
        <el-descriptions-item label="目标成员">{{ currentAudit.targetName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(currentAudit.status) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ currentAudit.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="变更前数据" :span="2">
          <pre class="json-preview">{{ formatJson(currentAudit.beforeData) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="变更后数据" :span="2">
          <pre class="json-preview">{{ formatJson(currentAudit.afterData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="showReject" title="驳回原因" width="400px">
      <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="showReject = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="submitting">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const auditList = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statusFilter = ref('')

const showDetail = ref(false)
const currentAudit = ref<any>(null)
const showReject = ref(false)
const rejectComment = ref('')
const rejectTarget = ref<any>(null)
const submitting = ref(false)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await service.get('/admin/audits', { params })
    auditList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm(`确认通过「${row.targetName}」的${actionText(row.action)}申请？`, '确认审核')
  try {
    await service.put(`/admin/audits/${row.id}/approve`)
    ElMessage.success('审核通过')
    loadData()
  } catch (e) {}
}

function handleReject(row: any) {
  rejectTarget.value = row
  rejectComment.value = ''
  showReject.value = true
}

async function confirmReject() {
  if (!rejectComment.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  submitting.value = true
  try {
    await service.put(`/admin/audits/${rejectTarget.value.id}/reject`, {
      comment: rejectComment.value
    })
    ElMessage.success('已驳回')
    showReject.value = false
    loadData()
  } catch (e) {} finally {
    submitting.value = false
  }
}

function viewDetail(row: any) {
  currentAudit.value = row
  showDetail.value = true
}

function statusType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[status] || status
}

function actionType(action: string) {
  const map: Record<string, string> = { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger' }
  return map[action] || 'info'
}

function actionText(action: string) {
  const map: Record<string, string> = { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }
  return map[action] || action
}

function formatJson(data: any) {
  if (!data) return '-'
  try {
    return JSON.stringify(typeof data === 'string' ? JSON.parse(data) : data, null, 2)
  } catch {
    return String(data)
  }
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

.json-preview {
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
