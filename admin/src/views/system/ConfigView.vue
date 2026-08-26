<template>
  <div class="page-container">
    <div class="page-card">
      <h2 style="margin-bottom: 20px;">系统配置</h2>

      <el-form :model="config" label-width="120px" style="max-width: 600px;">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="家族名称">
          <el-input v-model="config.familyName" />
        </el-form-item>
        <el-form-item label="堂号">
          <el-input v-model="config.hallName" />
        </el-form-item>
        <el-form-item label="始祖姓名">
          <el-input v-model="config.ancestorName" />
        </el-form-item>

        <el-divider content-position="left">字辈配置</el-divider>
        <el-form-item label="字辈序列">
          <el-input v-model="config.generationChars" type="textarea" :rows="3" placeholder="按世代顺序输入字辈，每个字代表一代" />
          <div class="form-tip">例如：仁义礼智信温良恭俭让</div>
        </el-form-item>
        <el-form-item label="起始世代">
          <el-input-number v-model="config.generationStart" :min="1" />
          <span class="form-tip" style="margin-left: 12px;">字辈序列对应的起始世代</span>
        </el-form-item>

        <el-divider content-position="left">显示设置</el-divider>
        <el-form-item label="默认布局">
          <el-radio-group v-model="config.defaultLayout">
            <el-radio label="vertical">竖向</el-radio>
            <el-radio label="horizontal">横向</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每页显示数">
          <el-input-number v-model="config.pageSize" :min="10" :max="100" :step="10" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage } from 'element-plus'

const saving = ref(false)
const config = reactive({
  familyName: '',
  hallName: '',
  ancestorName: '',
  generationChars: '',
  generationStart: 1,
  defaultLayout: 'vertical',
  pageSize: 20
})

onMounted(async () => {
  try {
    const res: any = await service.get('/admin/system/config')
    if (res) Object.assign(config, res)
  } catch (e) {}
})

async function saveConfig() {
  saving.value = true
  try {
    await service.put('/admin/system/config', config)
    ElMessage.success('配置保存成功')
  } catch (e) {} finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
