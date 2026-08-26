<template>
  <div class="page-container">
    <div class="page-card">
      <h2 class="page-title">数据概览</h2>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card stat-primary">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalMembers }}</div>
              <div class="stat-label">族人总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-success">
            <div class="stat-icon"><el-icon><Connection /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.generations }}</div>
              <div class="stat-label">世代数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-warning">
            <div class="stat-icon"><el-icon><Checked /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingAudits }}</div>
              <div class="stat-label">待审核</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-info">
            <div class="stat-icon"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">注册用户</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <div class="page-card">
          <h3>最近审核</h3>
          <el-table :data="recentAudits" stripe size="small">
            <el-table-column prop="submitterName" label="提交人" width="100" />
            <el-table-column prop="action" label="操作" width="80" />
            <el-table-column prop="targetName" label="目标" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h3>最近操作日志</h3>
          <el-timeline>
            <el-timeline-item
              v-for="log in recentLogs"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
            >
              {{ log.description }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import service from '@/api/request'

const stats = ref({
  totalMembers: 0,
  generations: 0,
  pendingAudits: 0,
  totalUsers: 0
})

const recentAudits = ref<any[]>([])
const recentLogs = ref<any[]>([])

onMounted(async () => {
  try {
    const [s, audits, logs] = await Promise.all([
      service.get('/admin/statistics'),
      service.get('/admin/audits/recent'),
      service.get('/admin/logs/recent')
    ])
    stats.value = s as any || stats.value
    recentAudits.value = (audits as any) || []
    recentLogs.value = (logs as any) || []
  } catch (e) {
    // 静默处理
  }
})

function statusType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[status] || status
}
</script>

<style scoped lang="scss">
.page-title {
  margin-bottom: 20px;
  color: #333;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin-right: 16px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #333;
  }

  .stat-label {
    font-size: 13px;
    color: #999;
    margin-top: 4px;
  }

  &.stat-primary .stat-icon { background: rgba(139, 69, 19, 0.1); color: #8B4513; }
  &.stat-success .stat-icon { background: rgba(76, 175, 80, 0.1); color: #4CAF50; }
  &.stat-warning .stat-icon { background: rgba(255, 152, 0, 0.1); color: #FF9800; }
  &.stat-info .stat-icon { background: rgba(33, 150, 243, 0.1); color: #2196F3; }
}

h3 {
  margin-bottom: 16px;
  color: #333;
}
</style>
