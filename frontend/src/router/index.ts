import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // 公开页面
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页', layout: 'default' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', layout: 'none' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', layout: 'none' },
  },
  {
    path: '/book/:taskId',
    name: 'PublicBooking',
    component: () => import('@/views/user/PublicBookingView.vue'),
    meta: { title: '预约', layout: 'default' },
  },

  // 用户页面（需要登录）
  {
    path: '/my-bookings',
    name: 'MyBookings',
    component: () => import('@/views/user/MyBookingsView.vue'),
    meta: {
      title: '我的预约',
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN'],
      layout: 'default',
    },
  },

  // 商户页面（需要 MERCHANT 角色）
  {
    path: '/merchant',
    name: 'MerchantRoot',
    redirect: '/merchant/dashboard',
  },
  {
    path: '/merchant/dashboard',
    name: 'MerchantDashboard',
    component: () => import('@/views/merchant/DashboardView.vue'),
    meta: {
      title: '商户仪表板',
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN'],
      layout: 'merchant',
    },
  },
  {
    path: '/merchant/profile',
    name: 'MerchantProfile',
    component: () => import('@/views/merchant/ProfileView.vue'),
    meta: {
      title: '商户档案',
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN'],
      layout: 'merchant',
    },
  },
  {
    path: '/merchant/services',
    name: 'MerchantServices',
    component: () => import('@/views/merchant/ServicesView.vue'),
    meta: {
      title: '服务管理',
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN'],
      layout: 'merchant',
    },
  },
  {
    path: '/merchant/tasks',
    name: 'MerchantTasks',
    component: () => import('@/views/merchant/TasksView.vue'),
    meta: {
      title: '任务管理',
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN'],
      layout: 'merchant',
    },
  },
  {
    path: '/merchant/bookings',
    name: 'MerchantBookings',
    component: () => import('@/views/merchant/BookingsView.vue'),
    meta: {
      title: '预约管理',
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN'],
      layout: 'merchant',
    },
  },

  // 管理员页面（需要 ADMIN 角色）
  {
    path: '/admin',
    name: 'AdminRoot',
    redirect: '/admin/dashboard',
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/DashboardView.vue'),
    meta: {
      title: '管理仪表板',
      requiresAuth: true,
      roles: ['ADMIN'],
      layout: 'admin',
    },
  },

  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面未找到', layout: 'none' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const meta = to.meta as {
    title?: string
    requiresAuth?: boolean
    roles?: string[]
    layout?: string
  }

  // 设置页面标题
  document.title = meta.title ? `${meta.title} - 预约系统` : '预约系统'

  // 检查是否需要认证
  if (meta.requiresAuth) {
    // 动态导入 auth store 避免循环依赖
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()

    // 检查是否已登录
    if (!authStore.isAuthenticated) {
      // 尝试获取当前用户信息
      try {
        await authStore.fetchCurrentUser()
      } catch {
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }

    // 检查角色权限
    if (meta.roles && meta.roles.length > 0) {
      if (!authStore.hasAnyRole(meta.roles as ('ADMIN' | 'MERCHANT' | 'USER')[])) {
        next({ name: 'Home' })
        return
      }
    }
  }

  next()
})

export default router
