<template>
  <div class="public-booking-view page-container">
    <!-- 加载中 -->
    <el-card v-if="initialLoading" class="main-card">
      <el-skeleton :rows="8" animated />
    </el-card>

    <!-- 错误状态 -->
    <el-card v-else-if="error" class="main-card error-card">
      <el-result icon="error" title="无法加载预约页面" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 预约内容 -->
    <template v-else>
      <!-- 任务信息卡片 -->
      <el-card class="task-info-card">
        <template #header>
          <div class="card-header">
            <span class="title">{{ task?.title || '预约服务' }}</span>
            <el-tag v-if="task" type="success">可预约</el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务名称">
            {{ task?.serviceName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="预约日期">
            {{ formatDate(task?.taskDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="总容量">
            {{ task?.totalCapacity || 0 }} 人
          </el-descriptions-item>
          <el-descriptions-item label="已预约">
            {{ task?.totalBookedCount || 0 }} 人
          </el-descriptions-item>
          <el-descriptions-item v-if="task?.description" label="说明" :span="2">
            {{ task.description }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 选择时段 -->
      <el-card class="slot-card">
        <template #header>
          <div class="card-header">
            <span class="title">选择时段</span>
            <el-button text @click="refreshSlots">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <TimeSlotPicker
          ref="slotPickerRef"
          :task-id="taskId"
          :only-available="true"
          @select="handleSlotSelect"
        />
      </el-card>

      <!-- 预约表单 -->
      <el-card v-if="selectedSlot" class="form-card">
        <template #header>
          <span class="title">填写预约信息</span>
        </template>

        <el-alert type="info" :closable="false" class="mb-3">
          <template #title>
            已选时段：{{ formatDateTime(selectedSlot.startTime) }} -
            {{ formatTime(selectedSlot.endTime) }}
          </template>
        </el-alert>

        <!-- 未登录提示 -->
        <el-alert v-if="!authStore.isAuthenticated" type="warning" :closable="false" class="mb-3">
          <template #title>您需要登录后才能预约</template>
          <template #default>
            <el-button type="primary" size="small" @click="showLoginDialog = true">
              立即登录
            </el-button>
            <el-button size="small" @click="showRegisterDialog = true"> 注册账号 </el-button>
          </template>
        </el-alert>

        <!-- 预约表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          :disabled="!authStore.isAuthenticated"
        >
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入预约备注（可选）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!authStore.isAuthenticated"
              @click="handleSubmit"
            >
              提交预约
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 预约成功 -->
      <el-card v-if="bookingSuccess" class="success-card">
        <el-result icon="success" title="预约成功！" sub-title="您可以在「我的预约」中查看详情">
          <template #extra>
            <el-button type="primary" @click="$router.push('/my-bookings')">
              查看我的预约
            </el-button>
            <el-button @click="resetBooking">继续预约</el-button>
          </template>
        </el-result>
      </el-card>
    </template>

    <!-- 登录弹窗 -->
    <el-dialog v-model="showLoginDialog" title="登录" width="400px" destroy-on-close>
      <el-form :model="loginForm" label-width="80px" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLoginDialog = false">取消</el-button>
        <el-button type="primary" :loading="loginLoading" @click="handleLogin">登录</el-button>
      </template>
    </el-dialog>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegisterDialog" title="注册" width="450px" destroy-on-close>
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="3-50个字符" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="6-100个字符"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">
          注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { taskApi, bookingApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import TimeSlotPicker from '@/components/business/TimeSlotPicker.vue'
import type { AppointmentTask, AppointmentSlot, Booking } from '@/types'

const route = useRoute()
const authStore = useAuthStore()

// 路由参数
const taskId = computed(() => Number(route.params.taskId))
const token = computed(() => route.query.token as string | undefined)
const exp = computed(() => route.query.exp as string | undefined)

// 状态
const initialLoading = ref(true)
const error = ref<string | null>(null)
const task = ref<AppointmentTask | null>(null)
const selectedSlot = ref<AppointmentSlot | null>(null)
const submitting = ref(false)
const bookingSuccess = ref(false)
const bookingResult = ref<Booking | null>(null)

// 组件引用
const slotPickerRef = ref<InstanceType<typeof TimeSlotPicker> | null>(null)
const formRef = ref<FormInstance | null>(null)
const registerFormRef = ref<FormInstance | null>(null)

// 表单数据
const form = reactive({
  remark: '',
})

const rules: FormRules = {
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }],
}

// 登录/注册
const showLoginDialog = ref(false)
const showRegisterDialog = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
})

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为6-100个字符', trigger: 'blur' },
  ],
}

// 格式化函数
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(11, 16)
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

// 加载任务信息
const loadTask = async () => {
  initialLoading.value = true
  error.value = null

  try {
    // 验证签名链接参数
    if (!taskId.value) {
      error.value = '无效的预约链接'
      return
    }

    // 如果有签名参数，验证签名
    if (token.value || exp.value) {
      try {
        await taskApi.getPublicById(taskId.value)
      } catch {
        error.value = '链接已过期或无效'
        return
      }
    }

    // 获取任务详情
    const response = await taskApi.getPublicById(taskId.value)
    task.value = response.data

    if (!task.value) {
      error.value = '预约任务不存在'
      return
    }

    if (!task.value.active) {
      error.value = '该预约任务已暂停'
      return
    }
  } catch {
    error.value = '加载预约信息失败，请稍后重试'
  } finally {
    initialLoading.value = false
  }
}

// 选择时段
const handleSlotSelect = (slot: AppointmentSlot | null) => {
  selectedSlot.value = slot
}

// 刷新时段
const refreshSlots = () => {
  slotPickerRef.value?.refresh()
}

// 提交预约
const handleSubmit = async () => {
  if (!selectedSlot.value) {
    ElMessage.warning('请先选择预约时段')
    return
  }

  if (!authStore.isAuthenticated) {
    showLoginDialog.value = true
    return
  }

  await formRef.value?.validate()

  submitting.value = true
  try {
    const response = await bookingApi.create({
      slotId: selectedSlot.value.id,
      remark: form.remark || undefined,
    })
    bookingResult.value = response.data
    bookingSuccess.value = true
    ElMessage.success('预约成功！')
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '预约失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  form.remark = ''
  selectedSlot.value = null
  formRef.value?.resetFields()
}

// 重置预约（继续预约）
const resetBooking = () => {
  bookingSuccess.value = false
  bookingResult.value = null
  handleReset()
  refreshSlots()
}

// 登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loginLoading.value = true
  try {
    const success = await authStore.login({
      username: loginForm.username,
      password: loginForm.password,
    })
    if (success) {
      ElMessage.success('登录成功')
      showLoginDialog.value = false
      loginForm.username = ''
      loginForm.password = ''
    } else {
      ElMessage.error(authStore.error || '登录失败')
    }
  } finally {
    loginLoading.value = false
  }
}

// 注册
const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate()
  if (!valid) return

  registerLoading.value = true
  try {
    const success = await authStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      role: 'USER',
    })
    if (success) {
      ElMessage.success('注册成功')
      showRegisterDialog.value = false
      registerForm.username = ''
      registerForm.email = ''
      registerForm.password = ''
    } else {
      ElMessage.error(authStore.error || '注册失败')
    }
  } finally {
    registerLoading.value = false
  }
}

onMounted(() => {
  // 尝试获取当前用户
  if (!authStore.isAuthenticated) {
    authStore.fetchCurrentUser().catch(() => {
      // 忽略错误，用户可能未登录
    })
  }

  loadTask()
})
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.main-card,
.task-info-card,
.slot-card,
.form-card,
.success-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
}

.mb-3 {
  margin-bottom: 16px;
}

.error-card {
  margin-top: 40px;
}

:deep(.el-descriptions__label) {
  width: 80px;
}
</style>
