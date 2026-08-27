import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const Layout = () => import('@/components/layout/Layout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      }
    ]
  },
  {
    path: '/member',
    component: Layout,
    meta: { title: '族谱管理', icon: 'Connection' },
    children: [
      {
        path: '',
        name: 'MemberList',
        component: () => import('@/views/member/MemberList.vue'),
        meta: { title: '成员列表' }
      },
      {
        path: 'tree',
        name: 'MemberTree',
        component: () => import('@/views/member/MemberTree.vue'),
        meta: { title: '世系结构' }
      },
      {
        path: ':id',
        name: 'MemberDetail',
        component: () => import('@/views/member/MemberDetail.vue'),
        meta: { title: '成员详情', hidden: true }
      }
    ]
  },
  {
    path: '/audit',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Audit',
        component: () => import('@/views/audit/AuditView.vue'),
        meta: { title: '审核中心', icon: 'Checked' }
      }
    ]
  },
  {
    path: '/chronicle',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Chronicle',
        component: () => import('@/views/chronicle/ChronicleList.vue'),
        meta: { title: '地方志管理', icon: 'Document' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    children: [
      {
        path: '',
        name: 'User',
        component: () => import('@/views/user/UserView.vue'),
        meta: { title: '用户管理', icon: 'User' }
      }
    ]
  },
  {
    path: '/import-export',
    component: Layout,
    children: [
      {
        path: '',
        name: 'ImportExport',
        component: () => import('@/views/import-export/ImportExportView.vue'),
        meta: { title: '导入导出', icon: 'Upload' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    meta: { title: '系统设置', icon: 'Setting' },
    children: [
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('@/views/system/ConfigView.vue'),
        meta: { title: '基本配置' }
      },
      {
        path: 'log',
        name: 'OperationLog',
        component: () => import('@/views/system/LogView.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },
  {
    path: '/generation-poetry',
    component: Layout,
    children: [
      {
        path: '',
        name: 'GenerationPoetry',
        component: () => import('@/views/genealogy/GenerationPoetryView.vue'),
        meta: { title: '字辈诗管理', icon: 'CollectionTag' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || ''} - 家族族谱管理后台`
  
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
