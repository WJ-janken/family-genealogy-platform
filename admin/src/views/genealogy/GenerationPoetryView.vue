<template>
  <div class="page-container">
    <div class="page-header">
      <h2>字辈诗/字辈歌管理</h2>
      <el-button type="primary" @click="showAddDialog">添加字辈诗</el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-container">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input v-model="searchForm.surname" placeholder="请输入姓氏" clearable @change="search" />
        </el-col>
        <el-col :span="6">
          <el-input v-model="searchForm.branch" placeholder="请输入分支" clearable @change="search" />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="surname" label="姓氏" width="120" />
      <el-table-column prop="branchName" label="分支" width="150" />
      <el-table-column prop="title" label="标题" width="200" />
      <el-table-column prop="generationSequence" label="字辈序列" width="200">
        <template #default="{ row }">
          <span>{{ formatGenerationSequence(row.generationSequence) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="startGeneration" label="起始世代" width="100" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
          <el-button size="small" type="primary" @click="editItem(row)">编辑</el-button>
          <el-button size="small" :type="row.enabled ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="deleteItem(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      :page-sizes="[10, 20, 30, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      style="margin-top: 20px; justify-content: center"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="60%" destroy-on-close>
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓氏" prop="surname">
              <el-input v-model="formData.surname" placeholder="请输入姓氏" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分支" prop="branchName">
              <el-input v-model="formData.branchName" placeholder="请输入分支名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入字辈诗标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="字辈序列" prop="generationSequence">
              <el-input 
                v-model="formData.generationSequence" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入字辈序列，如：仁义礼智信温良恭俭让" 
              />
              <div class="form-tip">按世代顺序输入字辈，每个字代表一代</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="起始世代" prop="startGeneration">
              <el-input-number v-model="formData.startGeneration" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="formData.enabled" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="字辈释义" prop="interpretation">
              <el-input 
                v-model="formData.interpretation" 
                type="textarea" 
                :rows="5" 
                placeholder="请输入字辈诗的释义和背景介绍" 
              />
              <div class="form-tip">详细解释字辈诗的含义、来源和文化背景</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input 
                v-model="formData.remark" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入备注信息" 
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailVisible" title="字辈诗详情" width="60%" destroy-on-close>
      <div class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓氏">{{ detailData.surname }}</el-descriptions-item>
          <el-descriptions-item label="分支">{{ detailData.branchName }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detailData.title }}</el-descriptions-item>
          <el-descriptions-item label="字辈序列">{{ formatGenerationSequence(detailData.generationSequence) }}</el-descriptions-item>
          <el-descriptions-item label="起始世代">{{ detailData.startGeneration }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="detailData.enabled ? 'success' : 'danger'">
              {{ detailData.enabled ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
          <h4>字辈释义</h4>
          <p>{{ detailData.interpretation || '暂无释义' }}</p>
        </div>
        
        <div class="detail-section" v-if="detailData.remark">
          <h4>备注</h4>
          <p>{{ detailData.remark }}</p>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据
const list = ref<any[]>([])
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const detailData = ref({})

// 分页
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  surname: '',
  branch: ''
})

// 表单数据
const formData = reactive({
  id: undefined,
  surname: '',
  branchName: '',
  title: '',
  generationSequence: '',
  interpretation: '',
  startGeneration: 1,
  enabled: true,
  remark: ''
})

// 表单验证规则
const formRules = {
  surname: [{ required: true, message: '请输入姓氏', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  generationSequence: [{ required: true, message: '请输入字辈序列', trigger: 'blur' }]
}

const formRef = ref()

// 获取列表
async function getList() {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.size,
      surname: searchForm.surname || undefined,
      branch: searchForm.branch || undefined
    }
    const res: any = await service.get('/generation-poetry', { params })
    list.value = res.records
    pagination.total = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 搜索
function search() {
  pagination.current = 1
  getList()
}

// 重置搜索
function resetSearch() {
  searchForm.surname = ''
  searchForm.branch = ''
  pagination.current = 1
  getList()
}

// 分页变化
function handleSizeChange(size: number) {
  pagination.size = size
  getList()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  getList()
}

// 显示添加对话框
function showAddDialog() {
  dialogTitle.value = '添加字辈诗'
  Object.keys(formData).forEach(key => {
    if (key !== 'id') {
      (formData as any)[key] = key === 'startGeneration' ? 1 : key === 'enabled' ? true : ''
    }
  })
  dialogVisible.value = true
}

// 编辑项目
function editItem(row: any) {
  dialogTitle.value = '编辑字辈诗'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看详情
function viewDetail(row: any) {
  detailData.value = row
  detailVisible.value = true
}

// 提交表单
async function submitForm() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (formData.id) {
          res = await service.put(`/generation-poetry/${formData.id}`, formData)
          ElMessage.success('更新成功')
        } else {
          res = await service.post('/generation-poetry', formData)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        getList()
      } catch (e) {
        console.error(e)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除项目
async function deleteItem(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条字辈诗吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await service.delete(`/generation-poetry/${id}`)
    ElMessage.success('删除成功')
    getList()
  } catch (e) {
    console.error(e)
  }
}

// 切换状态
async function toggleStatus(row: any) {
  try {
    const newStatus = !row.enabled
    await service.put(`/generation-poetry/${row.id}/enable`, {}, {
      params: { enabled: newStatus }
    })
    row.enabled = newStatus
    ElMessage.success(newStatus ? '启用成功' : '禁用成功')
  } catch (e) {
    console.error(e)
  }
}

// 格式化字辈序列显示
function formatGenerationSequence(sequence: string) {
  if (!sequence) return ''
  // 每4个字加一个空格便于阅读
  return sequence.replace(/(.{4})/g, '$1 ')
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-container {
  margin-bottom: 20px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.detail-content {
  .detail-section {
    margin-top: 20px;
    
    h4 {
      margin: 10px 0 5px 0;
      font-weight: bold;
    }
    
    p {
      line-height: 1.6;
      white-space: pre-wrap;
    }
  }
}
</style>