# Vue 3 前端开发指南

## 1. 项目概述

本项目前端采用 **Vue 3 + TypeScript + Element Plus** 技术栈，使用 Vite 作为构建工具。

### 1.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由 |
| Axios | 1.x | HTTP 客户端 |
| Day.js | 1.x | 日期处理 |
| VueUse | 10.x | 工具函数 |

## 2. 项目结构

```
frontend/
├── public/                     # 静态资源
│   └── favicon.ico
├── src/
│   ├── api/                    # API 接口
│   │   ├── auth.ts             # 认证接口
│   │   ├── merchant.ts         # 商户接口
│   │   ├── service.ts          # 服务项接口
│   │   └── booking.ts          # 预约接口
│   ├── assets/                 # 资源文件
│   │   ├── images/
│   │   └── styles/
│   │       ├── variables.scss  # SCSS 变量
│   │       └── global.scss     # 全局样式
│   ├── components/             # 通用组件
│   │   ├── common/
│   │   │   ├── PageHeader.vue
│   │   │   └── LoadingSpinner.vue
│   │   └── business/
│   │       ├── BookingCard.vue
│   │       └── TimeSlotPicker.vue
│   ├── composables/            # 组合式函数
│   │   ├── useAuth.ts
│   │   ├── useNotification.ts
│   │   └── useLoading.ts
│   ├── layouts/                # 布局组件
│   │   ├── DefaultLayout.vue
│   │   ├── MerchantLayout.vue
│   │   └── AdminLayout.vue
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── stores/                 # Pinia 状态
│   │   ├── auth.ts
│   │   ├── merchant.ts
│   │   └── booking.ts
│   ├── types/                  # TypeScript 类型
│   │   ├── api.ts
│   │   ├── user.ts
│   │   └── booking.ts
│   ├── utils/                  # 工具函数
│   │   ├── request.ts          # Axios 封装
│   │   ├── date.ts             # 日期工具
│   │   └── storage.ts          # 本地存储
│   ├── views/                  # 页面组件
│   │   ├── auth/
│   │   │   ├── LoginView.vue
│   │   │   └── RegisterView.vue
│   │   ├── merchant/
│   │   │   ├── DashboardView.vue
│   │   │   ├── ServicesView.vue
│   │   │   └── BookingsView.vue
│   │   ├── user/
│   │   │   ├── BookingView.vue
│   │   │   └── MyBookingsView.vue
│   │   └── admin/
│   │       └── StatsView.vue
│   ├── App.vue                 # 根组件
│   └── main.ts                 # 入口文件
├── .env                        # 环境变量
├── .env.development            # 开发环境
├── .env.production             # 生产环境
├── index.html                  # HTML 模板
├── package.json                # 依赖配置
├── tsconfig.json               # TypeScript 配置
├── vite.config.ts              # Vite 配置
└── README.md                   # 项目说明
```

## 3. 快速开始

### 3.1 安装依赖

```bash
cd frontend
npm install
```

### 3.2 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 3.3 构建生产版本

```bash
npm run build
```

### 3.4 预览生产版本

```bash
npm run preview
```

## 4. 核心配置

### 4.1 Vite 配置 (vite.config.ts)

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
})
```

### 4.2 TypeScript 配置 (tsconfig.json)

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### 4.3 环境变量

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=预约系统 - 开发环境

# .env.production
VITE_API_BASE_URL=https://api.example.com
VITE_APP_TITLE=预约系统
```

## 5. 网络请求

### 5.1 Axios 封装 (utils/request.ts)

```typescript
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true, // 携带 Cookie (Session)
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // Session 模式不需要手动添加 Token
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('没有权限访问')
    } else if (status === 429) {
      ElMessage.warning('请求过于频繁，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error('服务器错误，请稍后再试')
    } else {
      ElMessage.error(error.response?.data?.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

export default {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config)
  }
}
```

### 5.2 API 模块示例 (api/booking.ts)

```typescript
import request from '@/utils/request'
import type { Booking, BookingCreateRequest, TimeSlot } from '@/types/booking'

export const bookingApi = {
  // 获取可用时段
  getAvailableSlots(taskId: number, date: string): Promise<TimeSlot[]> {
    return request.get(`/api/tasks/${taskId}/slots`, {
      params: { date }
    })
  },

  // 创建预约
  create(data: BookingCreateRequest): Promise<Booking> {
    return request.post('/api/bookings', data)
  },

  // 获取我的预约
  getMyBookings(): Promise<Booking[]> {
    return request.get('/api/bookings/my')
  },

  // 取消预约
  cancel(bookingId: number): Promise<void> {
    return request.delete(`/api/bookings/${bookingId}`)
  }
}
```

## 6. 状态管理

### 6.1 Pinia Store 示例 (stores/auth.ts)

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { User, LoginForm, RegisterForm } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const loading = ref(false)

  // Getters
  const isAuthenticated = computed(() => !!user.value)
  const isMerchant = computed(() => user.value?.role === 'MERCHANT')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  // Actions
  async function login(form: LoginForm) {
    loading.value = true
    try {
      user.value = await authApi.login(form)
    } finally {
      loading.value = false
    }
  }

  async function register(form: RegisterForm) {
    loading.value = true
    try {
      user.value = await authApi.register(form)
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    try {
      user.value = await authApi.me()
    } catch {
      user.value = null
    }
  }

  async function logout() {
    await authApi.logout()
    user.value = null
  }

  return {
    user,
    loading,
    isAuthenticated,
    isMerchant,
    isAdmin,
    login,
    register,
    fetchCurrentUser,
    logout
  }
})
```

## 7. 路由配置

### 7.1 路由示例 (router/index.ts)

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guest: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/HomeView.vue')
        },
        {
          path: 'book/:taskId',
          name: 'Booking',
          component: () => import('@/views/user/BookingView.vue'),
          props: true
        },
        {
          path: 'my-bookings',
          name: 'MyBookings',
          component: () => import('@/views/user/MyBookingsView.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },
    {
      path: '/merchant',
      component: () => import('@/layouts/MerchantLayout.vue'),
      meta: { requiresAuth: true, requiresMerchant: true },
      children: [
        {
          path: '',
          name: 'MerchantDashboard',
          component: () => import('@/views/merchant/DashboardView.vue')
        },
        {
          path: 'services',
          name: 'MerchantServices',
          component: () => import('@/views/merchant/ServicesView.vue')
        },
        {
          path: 'tasks',
          name: 'MerchantTasks',
          component: () => import('@/views/merchant/TasksView.vue')
        },
        {
          path: 'bookings',
          name: 'MerchantBookings',
          component: () => import('@/views/merchant/BookingsView.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: 'stats',
          name: 'AdminStats',
          component: () => import('@/views/admin/StatsView.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 获取当前用户信息
  if (!authStore.user && !to.meta.guest) {
    await authStore.fetchCurrentUser()
  }

  // 需要登录但未登录
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 需要商户权限
  if (to.meta.requiresMerchant && !authStore.isMerchant) {
    return next({ name: 'Home' })
  }

  // 需要管理员权限
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next({ name: 'Home' })
  }

  // 已登录用户访问登录/注册页
  if (to.meta.guest && authStore.isAuthenticated) {
    return next({ name: 'Home' })
  }

  next()
})

export default router
```

## 8. 组件开发规范

### 8.1 组件模板

```vue
<template>
  <div class="booking-card">
    <el-card>
      <template #header>
        <span>{{ title }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="form.date" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="时段" prop="slotId">
          <el-select v-model="form.slotId" placeholder="选择时段">
            <el-option v-for="slot in slots" :key="slot.id" :label="slot.label" :value="slot.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确认预约</el-button>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { TimeSlot } from '@/types/booking'

// Props
interface Props {
  taskId: number
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '预约'
})

// Emits
const emit = defineEmits<{
  success: [bookingId: number]
  cancel: []
}>()

// State
const formRef = ref<FormInstance>()
const loading = ref(false)
const slots = ref<TimeSlot[]>([])

const form = reactive({
  date: '',
  slotId: null as number | null
})

const rules: FormRules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  slotId: [{ required: true, message: '请选择时段', trigger: 'change' }]
}

// Methods
async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    // 调用 API
    // const booking = await bookingApi.create({ ... })
    // emit('success', booking.id)
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  emit('cancel')
}
</script>

<style scoped lang="scss">
.booking-card {
  max-width: 500px;
  margin: 0 auto;
}
</style>
```

### 8.2 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `BookingCard.vue` |
| 页面文件 | PascalCase + View | `BookingView.vue` |
| 组合式函数 | camelCase + use | `useAuth.ts` |
| Store | camelCase | `auth.ts` |
| API 模块 | camelCase + Api | `bookingApi` |
| 类型文件 | camelCase | `booking.ts` |

## 9. Element Plus 使用

### 9.1 按需导入

```typescript
// main.ts
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)

app.use(ElementPlus, { locale: zhCn })
app.use(createPinia())
app.use(router)

// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
```

### 9.2 常用组件

```vue
<!-- 表单 -->
<el-form ref="formRef" :model="form" :rules="rules">
  <el-form-item label="用户名" prop="username">
    <el-input v-model="form.username" />
  </el-form-item>
</el-form>

<!-- 表格 -->
<el-table :data="tableData">
  <el-table-column prop="name" label="名称" />
  <el-table-column prop="status" label="状态">
    <template #default="{ row }">
      <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
        {{ row.status }}
      </el-tag>
    </template>
  </el-table-column>
</el-table>

<!-- 对话框 -->
<el-dialog v-model="dialogVisible" title="提示">
  <span>确定要删除吗？</span>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleConfirm">确定</el-button>
  </template>
</el-dialog>

<!-- 消息提示 -->
<ElMessage.success('操作成功')
<ElMessage.error('操作失败')
<ElNotification({ title: '通知', message: '有新预约', type: 'success' })
```

## 10. 类型定义

### 10.1 类型文件示例 (types/booking.ts)

```typescript
export interface TimeSlot {
  id: number
  startTime: string
  endTime: string
  capacity: number
  booked: number
  available: number
}

export interface Booking {
  id: number
  taskId: number
  slotId: number
  userId: number
  status: BookingStatus
  createdAt: string
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'

export interface BookingCreateRequest {
  taskId: number
  slotId: number
  remark?: string
}

export interface BookingQuery {
  taskId?: number
  status?: BookingStatus
  date?: string
  page?: number
  size?: number
}
```

## 11. 开发建议

### 11.1 性能优化

- 使用 `defineAsyncComponent` 懒加载页面组件
- 合理使用 `computed` 和 `watch`
- 大列表使用虚拟滚动
- 图片使用懒加载

### 11.2 代码规范

- 使用 ESLint + Prettier 格式化代码
- 遵循 Vue 官方风格指南
- 组件 Props 添加类型定义
- 复杂逻辑添加注释

### 11.3 Git 提交规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具
```
