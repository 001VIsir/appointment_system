# 商户自建前端接入指南（第三方接入）

## 1. 接入目标

商户可以自行开发网页、小程序等前端，并直接调用本系统 API 完成注册、登录、任务查询与预约。
本系统提供统一后端能力，商户只需按规范接入即可。

---

## 2. 接入方式

### 2.1 会话模式（推荐）

当前系统使用 Session + Redis 保存登录状态。
前端请求需携带 Cookie（浏览器端必须开启跨域携带凭证）。

### 2.2 技术栈推荐

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| Axios | 1.x | HTTP 客户端 |
| TypeScript | 5.x | 类型系统（可选） |
| Pinia | 2.x | 状态管理（可选） |

---

## 3. 前端集成示例

### 3.1 Axios 配置（Vue 3）

```typescript
// api/request.ts
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://api.example.com',
  timeout: 10000,
  withCredentials: true, // 携带 Cookie
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // 未登录，跳转登录页
      window.location.href = '/login'
    } else if (error.response?.status === 429) {
      console.warn('请求过于频繁，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default api
```

### 3.2 认证 API（Vue 3 + TypeScript）

```typescript
// api/auth.ts
import api from './request'

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm extends LoginForm {
  email: string
  role: 'USER' | 'MERCHANT'
}

export interface User {
  id: number
  username: string
  email: string
  role: string
}

export const authApi = {
  // 用户注册
  register(data: RegisterForm): Promise<User> {
    return api.post('/api/auth/register', data)
  },

  // 用户登录
  login(data: LoginForm): Promise<User> {
    return api.post('/api/auth/login', data)
  },

  // 用户登出
  logout(): Promise<void> {
    return api.post('/api/auth/logout')
  },

  // 获取当前用户
  me(): Promise<User> {
    return api.get('/api/auth/me')
  }
}
```

### 3.3 Pinia Store 示例

```typescript
// stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginForm, type RegisterForm, type User } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => !!user.value)
  const isMerchant = computed(() => user.value?.role === 'MERCHANT')

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

  async function logout() {
    await authApi.logout()
    user.value = null
  }

  async function fetchUser() {
    try {
      user.value = await authApi.me()
    } catch {
      user.value = null
    }
  }

  return {
    user,
    loading,
    isAuthenticated,
    isMerchant,
    login,
    register,
    logout,
    fetchUser
  }
})
```

### 3.4 任务 API

```typescript
// api/task.ts
import api from './request'

export interface Task {
  id: number
  title: string
  description: string
  date: string
  totalCapacity: number
  active: boolean
}

export interface TimeSlot {
  id: number
  startTime: string
  endTime: string
  capacity: number
  bookedCount: number
  available: number
}

export interface SignedLink {
  url: string
  expiresAt: string
}

export const taskApi = {
  // 查询商户任务列表
  list(): Promise<Task[]> {
    return api.get('/api/tasks')
  },

  // 查询任务详情
  get(taskId: number): Promise<Task> {
    return api.get(`/api/tasks/${taskId}`)
  },

  // 获取可用时段
  getSlots(taskId: number, date: string): Promise<TimeSlot[]> {
    return api.get(`/api/tasks/${taskId}/slots`, { params: { date } })
  },

  // 生成签名链接
  generateSignedLink(taskId: number, expiryMinutes: number = 60): Promise<SignedLink> {
    return api.post(`/api/tasks/${taskId}/signed-link`, { expiryMinutes })
  },

  // 通过签名链接查询任务
  getBySignedLink(signed: string): Promise<Task> {
    return api.get(`/api/tasks/public/signed/${signed}`)
  }
}
```

### 3.5 预约 API

```typescript
// api/booking.ts
import api from './request'

export interface Booking {
  id: number
  taskId: number
  slotId: number
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'
  remark?: string
  createdAt: string
}

export interface CreateBookingRequest {
  slotId: number
  remark?: string
}

export const bookingApi = {
  // 通过签名链接创建预约
  createWithSignedLink(signed: string, data: CreateBookingRequest): Promise<Booking> {
    return api.post(`/api/tasks/public/signed/${signed}/bookings`, data)
  },

  // 获取我的预约列表
  getMyBookings(): Promise<Booking[]> {
    return api.get('/api/bookings/my')
  },

  // 取消预约
  cancel(bookingId: number): Promise<void> {
    return api.delete(`/api/bookings/${bookingId}`)
  }
}
```

### 3.6 Vue 组件示例

```vue
<!-- views/BookingView.vue -->
<template>
  <div class="booking-page">
    <h1>{{ task?.title }}</h1>
    <p>{{ task?.description }}</p>

    <!-- 日期选择 -->
    <el-date-picker
      v-model="selectedDate"
      type="date"
      placeholder="选择日期"
      @change="loadSlots"
    />

    <!-- 时段选择 -->
    <div v-if="slots.length > 0" class="slots">
      <div
        v-for="slot in slots"
        :key="slot.id"
        class="slot-item"
        :class="{ disabled: slot.available === 0, selected: selectedSlot?.id === slot.id }"
        @click="slot.available > 0 && (selectedSlot = slot)"
      >
        <span>{{ slot.startTime }} - {{ slot.endTime }}</span>
        <span>剩余: {{ slot.available }}/{{ slot.capacity }}</span>
      </div>
    </div>

    <!-- 预约按钮 -->
    <el-button
      type="primary"
      :disabled="!selectedSlot"
      :loading="loading"
      @click="handleBooking"
    >
      确认预约
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi, type Task, type TimeSlot } from '@/api/task'
import { bookingApi } from '@/api/booking'
import dayjs from 'dayjs'

const route = useRoute()
const signed = route.params.signed as string

const task = ref<Task | null>(null)
const slots = ref<TimeSlot[]>([])
const selectedDate = ref('')
const selectedSlot = ref<TimeSlot | null>(null)
const loading = ref(false)

onMounted(async () => {
  try {
    task.value = await taskApi.getBySignedLink(signed)
  } catch (error) {
    ElMessage.error('链接无效或已过期')
  }
})

async function loadSlots() {
  if (!task.value || !selectedDate.value) return
  const date = dayjs(selectedDate.value).format('YYYY-MM-DD')
  slots.value = await taskApi.getSlots(task.value.id, date)
  selectedSlot.value = null
}

async function handleBooking() {
  if (!selectedSlot.value) return

  loading.value = true
  try {
    await bookingApi.createWithSignedLink(signed, {
      slotId: selectedSlot.value.id
    })
    ElMessage.success('预约成功！')
    // 跳转到预约列表或显示成功信息
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '预约失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.slot-item {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 8px;
}

.slot-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slot-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}
</style>
```

---

## 4. 必要 API 清单

### 4.1 认证相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| POST | `/api/auth/logout` | 用户登出 | 是 |
| GET | `/api/auth/me` | 获取当前用户 | 是 |

### 4.2 任务相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/tasks` | 查询商户任务列表 | 是 |
| GET | `/api/tasks/{taskId}` | 查询任务详情 | 是 |
| GET | `/api/tasks/{taskId}/slots` | 获取可用时段 | 否 |
| POST | `/api/tasks/{taskId}/signed-link` | 生成签名链接 | 是 |
| GET | `/api/tasks/public/signed/{signed}` | 通过签名查询任务 | 否 |

### 4.3 预约相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/tasks/public/signed/{signed}/bookings` | 创建预约 | 否 |
| GET | `/api/bookings/my` | 获取我的预约 | 是 |
| DELETE | `/api/bookings/{id}` | 取消预约 | 是 |

---

## 5. 高并发与稳定性策略

### 5.1 接口限流

系统已内置 Redis 限流，配置参数：
- `app.rate-limit.max-requests`: 时间窗口内最大请求数
- `app.rate-limit.window-seconds`: 时间窗口（秒）

### 5.2 超时与重试

**客户端建议配置：**
```typescript
const api = axios.create({
  timeout: 5000 // 5 秒超时
})
```

**重试策略（指数退避）：**
```typescript
async function retryRequest(fn: () => Promise<any>, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn()
    } catch (error) {
      if (i === maxRetries - 1) throw error
      await new Promise(r => setTimeout(r, 1000 * Math.pow(2, i)))
    }
  }
}
```

### 5.3 缓存策略

| 数据类型 | 建议缓存时长 | 说明 |
|----------|--------------|------|
| 任务详情 | 30 秒 | 可在前端缓存 |
| 时段列表 | 不缓存 | 需实时查询 |
| 签名链接 | 10-60 分钟 | 根据过期时间 |

### 5.4 并发控制

- 高并发预约使用**乐观锁**控制
- 超量请求返回 **429 Too Many Requests**
- 冲突时返回 **409 Conflict**

---

## 6. 错误处理

### 6.1 HTTP 状态码

| 状态码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 参数校验失败 | 检查请求参数 |
| 401 | 未登录 | 跳转登录页 |
| 403 | 权限不足 | 提示用户 |
| 404 | 资源不存在 | 提示用户 |
| 409 | 冲突（如重复预约） | 提示用户重试 |
| 429 | 请求过于频繁 | 稍后重试 |
| 500 | 服务器错误 | 联系管理员 |

### 6.2 错误响应格式

```json
{
  "code": "VALIDATION_ERROR",
  "message": "参数校验失败",
  "details": {
    "username": "用户名不能为空"
  },
  "timestamp": "2024-01-15T10:30:00.000+08:00"
}
```

### 6.3 Vue 错误处理示例

```typescript
// utils/errorHandler.ts
import { ElMessage } from 'element-plus'

export function handleApiError(error: any) {
  const status = error.response?.status
  const message = error.response?.data?.message

  switch (status) {
    case 400:
      ElMessage.error(message || '请求参数错误')
      break
    case 401:
      ElMessage.warning('请先登录')
      window.location.href = '/login'
      break
    case 403:
      ElMessage.error('没有权限访问')
      break
    case 429:
      ElMessage.warning('请求过于频繁，请稍后再试')
      break
    case 500:
      ElMessage.error('服务器错误，请稍后再试')
      break
    default:
      ElMessage.error(message || '请求失败')
  }
}
```

---

## 7. 监控与告警

系统内置 API 调用统计：

```
GET /api/admin/metrics
```

返回各接口调用次数、错误数、平均耗时等指标。

---

## 8. 安全说明

- 所有敏感信息必须走 **HTTPS**
- 登录密码采用 **BCrypt** 加密存储
- 防 SQL 注入：所有数据库访问采用 **JPA/ORM**
- 防 XSS：前端自行过滤输入，后端仅存储原值
- **CORS**：需在后端配置允许的域名

---

## 9. 示例接入流程

```
1. 商户前端调用 POST /api/auth/register 注册商户账号
2. 调用 POST /api/auth/login 登录
3. 登录后调用 GET /api/tasks 创建/查询任务
4. 调用 POST /api/tasks/{taskId}/signed-link 生成签名链接
5. 商户把签名链接配置到自己的页面
6. 普通用户通过签名链接查询任务并预约
7. 用户可调用 GET /api/bookings/my 查看自己的预约
```

---

## 10. 常见问题 FAQ

**Q: 为什么请求返回 401？**
A: 未登录或 Session 已过期，请重新登录。

**Q: 为什么跨域请求失败？**
A: 确保后端已配置 CORS，且前端请求设置了 `withCredentials: true`。

**Q: 签名链接有效期多长？**
A: 默认 1 小时，可通过 `expiryMinutes` 参数自定义（最长 24 小时）。

**Q: 如何处理预约冲突？**
A: 系统使用乐观锁，冲突时会返回 409 错误，前端应提示用户刷新并重试。
