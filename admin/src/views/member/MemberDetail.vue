<template>
  <div class="page-container">
    <div class="page-card" v-loading="loading">
      <div class="page-header">
        <h2>{{ isNew ? '新增成员' : '成员详情' }}</h2>
        <el-button @click="$router.back()">返回</el-button>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 700px;">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="谱名" prop="name">
              <el-input v-model="form.name" placeholder="辈分名，如郑志泉" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="俗名">
              <el-input v-model="form.aliasName" placeholder="户口名/日常用名，如郑二狗" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="辈分字">
              <el-input v-model="form.generationChar" placeholder="如：志" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
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
              <el-input-number v-model="form.generation" :min="1" :max="50" style="width: 100%" />
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
          <el-input-number v-model="form.parentId" :min="0" controls-position="right" style="width: 200px" />
        </el-form-item>

        <el-form-item label="出生地">
          <el-input v-model="form.birthPlace" />
        </el-form-item>

        <el-form-item label="头像URL">
          <el-input v-model="form.avatarUrl" placeholder="输入图片 URL" />
        </el-form-item>

        <el-form-item label="生平简介">
          <el-input v-model="form.biography" type="textarea" :rows="5" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ isNew ? '创建' : '保存修改' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import service from '@/api/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const isNew = computed(() => route.params.id === 'new')

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
  avatarUrl: '',
  biography: '',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  generation: [{ required: true, message: '请输入世代', trigger: 'blur' }]
}

onMounted(async () => {
  // 从 query 获取默认值
  if (route.query.parentId) form.parentId = Number(route.query.parentId)
  if (route.query.generation) form.generation = Number(route.query.generation)

  if (!isNew.value) {
    loading.value = true
    try {
      const res: any = await service.get(`/admin/members/${route.params.id}`)
      if (res) Object.assign(form, res)
    } catch (e) {} finally {
      loading.value = false
    }
  }
})

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isNew.value) {
      await service.post('/admin/members', form)
      ElMessage.success('创建成功')
    } else {
      await service.put(`/admin/members/${form.id}`, form)
      ElMessage.success('保存成功')
    }
    router.back()
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
  margin-bottom: 24px;
  h2 { margin: 0; }
}
</style>
