<template>
  <div class="layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
        <div class="logo">
          <span v-if="!appStore.sidebarCollapsed">家族族谱管理</span>
          <span v-else>族</span>
        </div>
        <el-menu
          :default-active="route.path"
          :collapse="appStore.sidebarCollapsed"
          background-color="#1A1A2E"
          text-color="#bfcbd9"
          active-text-color="#D4A574"
          router
        >
          <template v-for="item in menuRoutes" :key="item.path">
            <!-- 单级菜单 -->
            <el-menu-item
              v-if="!item.children || item.children.length === 1"
              :index="getMenuPath(item)"
            >
              <el-icon><component :is="getMenuIcon(item)" /></el-icon>
              <template #title>{{ getMenuTitle(item) }}</template>
            </el-menu-item>
            <!-- 多级菜单 -->
            <el-sub-menu v-else :index="item.path">
              <template #title>
                <el-icon><component :is="item.meta?.icon" /></el-icon>
                <span>{{ item.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children.filter(c => !c.meta?.hidden)"
                :key="child.path"
                :index="`${item.path}/${child.path}`"
              >
                {{ child.meta?.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <!-- 顶栏 -->
        <el-header class="header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                {{ item.meta?.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                  {{ userStore.userInfo?.nickname?.[0] || 'A' }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const menuRoutes = computed(() => {
  return router.options.routes.filter(r => r.path !== '/login' && !r.meta?.hidden)
})

const breadcrumbs = computed(() => {
  return route.matched.filter(r => r.meta?.title)
})

function getMenuPath(item: any) {
  if (item.children?.length === 1) {
    return `${item.path}/${item.children[0].path}`.replace('//', '/')
  }
  return item.path
}

function getMenuIcon(item: any) {
  return item.meta?.icon || item.children?.[0]?.meta?.icon || 'Menu'
}

function getMenuTitle(item: any) {
  if (item.children?.length === 1) {
    return item.children[0].meta?.title || item.meta?.title
  }
  return item.meta?.title
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout {
  width: 100%;
  height: 100%;
}

.sidebar {
  background: #1A1A2E;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #D4A574;
    font-size: 18px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  .el-menu {
    border-right: none;
  }
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #666;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }

  .username {
    font-size: 14px;
    color: #333;
  }
}

.main-content {
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>
