<template>
  <div class="profile-view">
    <h2 class="page-title">商户档案</h2>

    <!-- 无档案时显示创建表单 -->
    <el-card v-if="!hasProfile && !loading">
      <template #header>
        <span>创建商户档案</span>
      </template>
      <el-form ref="createFormRef" :model="createForm" :rules="rules" label-width="100px">
        <el-form-item label="商户名称" prop="businessName">
          <el-input v-model="createForm.businessName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="商户描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商户描述"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="商户地址" prop="address">
          <el-input v-model="createForm.address" placeholder="请输入商户地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleCreate">
            创建档案
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 有档案时显示档案信息和设置 -->
    <template v-if="hasProfile && profile">
      <!-- 基本信息 -->
      <el-card class="mb-3">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-button type="primary" size="small" @click="showEditDialog = true">
              编辑
            </el-button>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商户名称">{{ profile.businessName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ profile.phone || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="商户地址" :span="2">
            {{ profile.address || '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="商户描述" :span="2">
            {{ profile.description || '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(profile.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(profile.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 商户设置 -->
      <el-card>
        <template #header>
          <div class="card-header">
            <span>商户设置</span>
            <el-button type="primary" size="small" @click="showSettingsDialog = true">
              编辑
            </el-button>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="会话超时">
            {{ settings?.sessionTimeout || 240 }} 分钟
          </el-descriptions-item>
          <el-descriptions-item label="邮件通知">
            <el-tag :type="settings?.notificationsEnabled ? 'success' : 'info'">
              {{ settings?.notificationsEnabled ? '已开启' : '已关闭' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="时区">{{ settings?.timezone || 'Asia/Shanghai' }}</el-descriptions-item>
          <el-descriptions-item label="提前预约天数">
            {{ settings?.bookingAdvanceDays || 30 }} 天
          </el-descriptions-item>
          <el-descriptions-item label="取消截止时间">
            {{ settings?.cancelDeadlineHours || 24 }} 小时前
          </el-descriptions-item>
          <el-descriptions-item label="自动确认预约">
            <el-tag :type="settings?.autoConfirmBookings ? 'success' : 'info'">
              {{ settings?.autoConfirmBookings ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="每日最大预约数">
            {{ settings?.maxBookingsPerUserPerDay || '不限制' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>

    <!-- 编辑基本信息对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑商户信息" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="商户名称" prop="businessName">
          <el-input v-model="editForm.businessName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="商户描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商户描述"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="商户地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入商户地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑设置对话框 -->
    <el-dialog v-model="showSettingsDialog" title="商户设置" width="500px">
      <el-form ref="settingsFormRef" :model="settingsForm" label-width="120px">
        <el-form-item label="会话超时">
          <el-input-number v-model="settingsForm.sessionTimeout" :min="30" :max="1440" />
          <span class="ml-2">分钟</span>
        </el-form-item>
        <el-form-item label="邮件通知">
          <el-switch v-model="settingsForm.notificationsEnabled" />
        </el-form-item>
        <el-form-item label="时区">
          <el-select v-model="settingsForm.timezone" placeholder="请选择时区">
            <el-option label="北京时间 (Asia/Shanghai)" value="Asia/Shanghai" />
            <el-option label="香港时间 (Asia/Hong_Kong)" value="Asia/Hong_Kong" />
            <el-option label="东京时间 (Asia/Tokyo)" value="Asia/Tokyo" />
            <el-option label="纽约时间 (America/New_York)" value="America/New_York" />
          </el-select>
        </el-form-item>
        <el-form-item label="提前预约天数">
          <el-input-number v-model="settingsForm.bookingAdvanceDays" :min="1" :max="365" />
          <span class="ml-2">天</span>
        </el-form-item>
        <el-form-item label="取消截止时间">
          <el-input-number v-model="settingsForm.cancelDeadlineHours" :min="1" :max="72" />
          <span class="ml-2">小时前</span>
        </el-form-item>
        <el-form-item label="自动确认预约">
          <el-switch v-model="settingsForm.autoConfirmBookings" />
        </el-form-item>
        <el-form-item label="每日最大预约数">
          <el-input-number v-model="settingsForm.maxBookingsPerUserPerDay" :min="0" />
          <span class="ml-2">（0 表示不限制）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettingsDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdateSettings">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { merchantApi } from '@/api'
import type { MerchantProfile, MerchantSettings } from '@/types'

const loading = ref(true)
const submitting = ref(false)
const hasProfile = ref(false)
const profile = ref<MerchantProfile | null>(null)
const settings = ref<MerchantSettings | null>(null)

const showEditDialog = ref(false)
const showSettingsDialog = ref(false)

const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()
const settingsFormRef = ref<FormInstance>()

// 设置表单引用（用于将来可能的验证）
void settingsFormRef

const createForm = reactive({
  businessName: '',
  description: '',
  phone: '',
  address: '',
})

const editForm = reactive({
  businessName: '',
  description: '',
  phone: '',
  address: '',
})

const settingsForm = reactive<MerchantSettings>({
  sessionTimeout: 240,
  notificationsEnabled: false,
  timezone: 'Asia/Shanghai',
  bookingAdvanceDays: 30,
  cancelDeadlineHours: 24,
  autoConfirmBookings: false,
  maxBookingsPerUserPerDay: 0,
})

const rules: FormRules = {
  businessName: [
    { required: true, message: '请输入商户名称', trigger: 'blur' },
    { min: 2, max: 100, message: '商户名称长度为2-100个字符', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' },
  ],
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadProfile = async () => {
  loading.value = true
  try {
    const response = await merchantApi.hasProfile()
    hasProfile.value = response.data.data

    if (hasProfile.value) {
      const profileRes = await merchantApi.getProfile()
      profile.value = profileRes.data.data

      // 加载设置
      const settingsRes = await merchantApi.getSettings()
      settings.value = settingsRes.data.data

      // 初始化编辑表单
      editForm.businessName = profile.value.businessName
      editForm.description = profile.value.description || ''
      editForm.phone = profile.value.phone || ''
      editForm.address = profile.value.address || ''

      // 初始化设置表单
      if (settings.value) {
        Object.assign(settingsForm, settings.value)
      }
    }
  } catch {
    ElMessage.error('加载档案失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await merchantApi.createProfile(createForm)
        ElMessage.success('创建成功')
        await loadProfile()
      } catch {
        ElMessage.error('创建失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleUpdate = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await merchantApi.updateProfile(editForm)
        ElMessage.success('更新成功')
        showEditDialog.value = false
        await loadProfile()
      } catch {
        ElMessage.error('更新失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleUpdateSettings = async () => {
  submitting.value = true
  try {
    await merchantApi.updateSettings(settingsForm)
    ElMessage.success('设置已保存')
    showSettingsDialog.value = false
    await loadProfile()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteProfile = async () => {
  try {
    await ElMessageBox.confirm('确定要删除商户档案吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await merchantApi.deleteProfile()
    ElMessage.success('删除成功')
    hasProfile.value = false
    profile.value = null
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadProfile()
})

defineExpose({
  handleDeleteProfile,
})
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb-3 {
  margin-bottom: 16px;
}

.ml-2 {
  margin-left: 8px;
}
</style>
