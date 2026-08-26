<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>数据导入导出</h2>
      </div>

      <el-tabs v-model="activeTab">
        <!-- 导出 -->
        <el-tab-pane label="数据导出" name="export">
          <div class="export-section">
            <p class="section-desc">将族谱数据导出为文件，支持 Excel、CSV、GEDCOM 格式。</p>
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="export-card" @click="exportData('excel')">
                  <div class="export-icon excel">📊</div>
                  <div class="export-title">导出 Excel</div>
                  <div class="export-desc">标准 .xlsx 格式，适合编辑和打印</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="export-card" @click="exportData('csv')">
                  <div class="export-icon csv">📄</div>
                  <div class="export-title">导出 CSV</div>
                  <div class="export-desc">通用文本格式，兼容性最好</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="export-card" @click="exportData('gedcom')">
                  <div class="export-icon gedcom">🌳</div>
                  <div class="export-title">导出 GEDCOM</div>
                  <div class="export-desc">族谱标准格式，可导入其他族谱软件</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 导入 -->
        <el-tab-pane label="数据导入" name="import">
          <div class="import-section">
            <el-alert type="warning" :closable="false" style="margin-bottom: 20px">
              <template #title>
                导入前请确认数据格式正确。建议先下载模板填写后再导入。
              </template>
            </el-alert>

            <el-form label-width="100px">
              <el-form-item label="文件格式">
                <el-radio-group v-model="importFormat">
                  <el-radio label="excel">Excel (.xlsx)</el-radio>
                  <el-radio label="csv">CSV (.csv)</el-radio>
                  <el-radio label="gedcom">GEDCOM (.ged)</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="导入模式">
                <el-radio-group v-model="importMode">
                  <el-radio label="append">追加（保留现有数据）</el-radio>
                  <el-radio label="merge">合并（按ID匹配更新）</el-radio>
                  <el-radio label="overwrite">覆盖（清空后导入）</el-radio>
                </el-radio-group>
                <div class="mode-tip" v-if="importMode === 'overwrite'">
                  <el-text type="danger">⚠️ 覆盖模式将清空所有现有数据，请谨慎操作！</el-text>
                </div>
              </el-form-item>
              <el-form-item label="选择文件">
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :limit="1"
                  :accept="acceptTypes"
                  :on-change="handleFileChange"
                  drag
                >
                  <el-icon style="font-size: 40px; color: #8B4513;"><Upload /></el-icon>
                  <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
                  <template #tip>
                    <div class="el-upload__tip">文件大小不超过 10MB</div>
                  </template>
                </el-upload>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="doImport" :loading="importing" :disabled="!selectedFile">
                  开始导入
                </el-button>
                <el-button @click="downloadTemplate">下载导入模板</el-button>
              </el-form-item>
            </el-form>

            <!-- 导入结果 -->
            <el-result v-if="importResult" :icon="importResult.failed > 0 ? 'warning' : 'success'" :title="importResultTitle">
              <template #sub-title>
                <p>总计: {{ importResult.total }} 条 | 新增: {{ importResult.created }} | 更新: {{ importResult.updated }} | 失败: {{ importResult.failed }}</p>
              </template>
            </el-result>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import service from '@/api/request'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const activeTab = ref('export')
const importFormat = ref('excel')
const importMode = ref('append')
const selectedFile = ref<File | null>(null)
const importing = ref(false)
const importResult = ref<any>(null)
const uploadRef = ref()

const acceptTypes = computed(() => {
  const map: Record<string, string> = { excel: '.xlsx,.xls', csv: '.csv', gedcom: '.ged,.gedcom' }
  return map[importFormat.value]
})

const importResultTitle = computed(() => {
  if (!importResult.value) return ''
  return importResult.value.failed > 0 ? '导入完成（部分失败）' : '导入成功'
})

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

async function exportData(format: string) {
  try {
    const response = await service.get(`/import-export/export/${format}`, { responseType: 'blob' })
    const blob = new Blob([response as any])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const extMap: Record<string, string> = { excel: '.xlsx', csv: '.csv', gedcom: '.ged' }
    a.download = `族谱数据${extMap[format]}`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function doImport() {
  if (!selectedFile.value) return

  importing.value = true
  importResult.value = null

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('mode', importMode.value)

  try {
    const res: any = await service.post(`/import-export/import/${importFormat.value}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    importResult.value = res
    ElMessage.success('导入完成')
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

function downloadTemplate() {
  window.open('/api/import-export/template/excel', '_blank')
}
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 20px;
  h2 { margin: 0; }
}

.section-desc {
  color: #666;
  margin-bottom: 20px;
}

.export-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #8B4513;
    box-shadow: 0 4px 12px rgba(139, 69, 19, 0.1);
    transform: translateY(-2px);
  }

  .export-icon {
    font-size: 40px;
    margin-bottom: 12px;
  }

  .export-title {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 8px;
  }

  .export-desc {
    font-size: 13px;
    color: #999;
  }
}

.mode-tip {
  margin-top: 8px;
}

.import-section {
  max-width: 700px;
}
</style>
