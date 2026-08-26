<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>族谱成员管理</h2>
        <div class="header-actions">
          <el-input v-model="keyword" placeholder="搜索姓名" clearable style="width: 200px" @keyup.enter="loadData" />
          <el-button type="primary" @click="handleAdd">新增成员</el-button>
        </div>
      </div>

      <el-table :data="memberList" v-loading="loading" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="谱名" width="120" />
        <el-table-column prop="aliasName" label="俗名" width="120" />
        <el-table-column prop="generationChar" label="辈分字" width="80" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ row.gender === 'F' ? '女' : '男' }}</template>
        </el-table-column>
        <el-table-column prop="generation" label="世代" width="70" />
        <el-table-column prop="branch" label="房支" width="120" />
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column prop="parentName" label="父节点" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
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
        layout="total, sizes, prev, pager, next"
        :page-sizes="[20, 50, 100]"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showForm" :title="formMode === 'add' ? '新增成员' : '编辑成员'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="谱名" prop="name">
              <el-input v-model="form.name" placeholder="辈分名，如郑志泉" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="俗名">
              <el-input v-model="form.aliasName" placeholder="户口名/日常用名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="辈分字">
              <el-input v-model="form.generationChar" placeholder="如：志" style="width: 120px" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio label="M">男</el-radio>
                <el-radio label="F">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="世代" prop="generation">
              <el-input-number v-model="form.generation" :min="1" :max="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房支">
              <el-input v-model="form.branch" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="逝世日期">
              <el-date-picker v-model="form.deathDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="父节点ID">
          <el-input-number v-model="form.parentId" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="出生地">
          <el-input v-model="form.birthPlace" />
        </el-form-item>
        <el-form-item label="生平简介">
          <el-input v-model="form.biography" type="textarea" :rows="3" />
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
const memberList = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const keyword = ref('')

const showForm = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  id: null as number | null,
  name: '',
  aliasName: '',
  generationChar: '',
  gender: 'M',
  generation: 1,
  branch: '',
  birthDate: '',
  deathDate: '',
  parentId: null as number | null,
  birthPlace: '',
  biography: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  generation: [{ required: true, message: '请输入世代', trigger: 'blur' }]
}

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res: any = await service.get('/admin/members', { params })
    memberList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

function handleAdd() {
  formMode.value = 'add'
  Object.assign(form, { id: null, name: '', aliasName: '', generationChar: '', gender: 'M', generation: 1, branch: '', birthDate: '', deathDate: '', parentId: null, birthPlace: '', biography: '' })
  showForm.value = true
}

function handleEdit(row: any) {
  formMode.value = 'edit'
  Object.assign(form, row)
  showForm.value = true
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除成员「${row.name}」？此操作不可恢复。`, '确认删除', { type: 'warning' })
  try {
    await service.delete(`/admin/members/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (formMode.value === 'add') {
      await service.post('/admin/members', form)
      ElMessage.success('新增成功')
    } else {
      await service.put(`/admin/members/${form.id}`, form)
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
  .header-actions { display: flex; gap: 12px; }
}
</style>
