<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>地方志管理</h2>
        <el-button type="primary" @click="handleAdd">新增条目</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="showForm" :title="formMode === 'add' ? '新增地方志' : '编辑地方志'" width="700px" top="5vh">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="家族历史" value="历史" />
            <el-option label="地理风貌" value="地理" />
            <el-option label="风俗文化" value="风俗" />
            <el-option label="祠堂宗庙" value="祠堂" />
            <el-option label="名人轶事" value="名人" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverUrl" placeholder="输入图片 URL 或上传" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持富文本内容" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showForm = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  id: null as number | null,
  title: '',
  category: '',
  content: '',
  coverUrl: '',
  sortOrder: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res: any = await service.get('/admin/chronicles', { params: { page: page.value, pageSize: pageSize.value } })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

function handleAdd() {
  formMode.value = 'add'
  Object.assign(form, { id: null, title: '', category: '', content: '', coverUrl: '', sortOrder: 0 })
  showForm.value = true
}

function handleEdit(row: any) {
  formMode.value = 'edit'
  Object.assign(form, row)
  showForm.value = true
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '确认删除', { type: 'warning' })
  try {
    await service.delete(`/admin/chronicles/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (formMode.value === 'add') {
      await service.post('/admin/chronicles', form)
      ElMessage.success('新增成功')
    } else {
      await service.put(`/admin/chronicles/${form.id}`, form)
      ElMessage.success('更新成功')
    }
    showForm.value = false
    loadData()
  } catch (e) {} finally {
    submitting.value = false
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
</style>
