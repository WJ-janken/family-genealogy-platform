<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h2>世系结构</h2>
        <div class="header-actions">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="tree">树形视图</el-radio-button>
            <el-radio-button label="table">表格视图</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 树形视图 -->
      <div v-if="viewMode === 'tree'" class="tree-view">
        <el-tree
          :data="treeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          v-loading="loading"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <span class="node-name" :class="{ female: data.gender === 'F' }">
                {{ data.name }}
              </span>
              <span class="node-info">
                第{{ data.generation }}世
                <template v-if="data.branch"> · {{ data.branch }}</template>
              </span>
              <span class="node-actions">
                <el-button type="primary" size="small" link @click="editNode(data)">编辑</el-button>
                <el-button type="success" size="small" link @click="addChild(data)">添加子节点</el-button>
              </span>
            </div>
          </template>
        </el-tree>

        <el-empty v-if="!loading && treeData.length === 0" description="暂无族谱数据" />
      </div>

      <!-- 表格视图 -->
      <div v-else>
        <el-table :data="flatList" v-loading="loading" stripe row-key="id" default-expand-all :tree-props="{ children: 'children' }">
          <el-table-column prop="name" label="姓名" width="150" />
          <el-table-column prop="gender" label="性别" width="60">
            <template #default="{ row }">{{ row.gender === 'F' ? '女' : '男' }}</template>
          </el-table-column>
          <el-table-column prop="generation" label="世代" width="70" />
          <el-table-column prop="branch" label="房支" width="100" />
          <el-table-column prop="birthDate" label="出生" width="110" />
          <el-table-column prop="deathDate" label="逝世" width="110" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import service from '@/api/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const viewMode = ref('tree')
const treeData = ref<any[]>([])
const flatList = ref<any[]>([])

const treeProps = {
  children: 'children',
  label: 'name'
}

onMounted(() => loadTree())

async function loadTree() {
  loading.value = true
  try {
    const data: any = await service.get('/admin/members/tree')
    treeData.value = Array.isArray(data) ? data : [data]
    flatList.value = treeData.value
  } catch (e) {} finally {
    loading.value = false
  }
}

function editNode(data: any) {
  router.push(`/member/${data.id}`)
}

function addChild(data: any) {
  router.push(`/member/new?parentId=${data.id}&generation=${(data.generation || 0) + 1}`)
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

.tree-view {
  max-height: calc(100vh - 240px);
  overflow: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
  width: 100%;

  .node-name {
    font-weight: 500;
    color: #2196F3;

    &.female {
      color: #E91E63;
    }
  }

  .node-info {
    font-size: 12px;
    color: #999;
  }

  .node-actions {
    margin-left: auto;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .node-actions {
    opacity: 1;
  }
}
</style>
