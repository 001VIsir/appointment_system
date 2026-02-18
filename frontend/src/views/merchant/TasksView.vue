<template>
  <div class="tasks-view">
    <h2 class="page-title">任务管理</h2>

    <!-- 操作栏 -->
    <el-card class="mb-3">
      <div class="toolbar">
        <div class="filter-group">
          <el-radio-group v-model="filterStatus" @change="loadTasks">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="active">活跃</el-radio-button>
            <el-radio-button value="inactive">已删除</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="filterService"
            placeholder="按服务筛选"
            clearable
            @change="loadTasks"
            style="margin-left: 16px; width: 200px"
          >
            <el-option
              v-for="service in services"
              :key="service.id"
              :label="service.name"
              :value="service.id"
            />
          </el-select>
        </div>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建任务
        </el-button>
      </div>
    </el-card>

    <!-- 任务列表 -->
    <el-card v-loading="loading">
      <el-table :data="tasks" style="width: 100%">
        <el-table-column prop="title" label="任务标题" min-width="150" />
        <el-table-column prop="serviceName" label="所属服务" width="120" />
        <el-table-column prop="taskDate" label="预约日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.taskDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalCapacity" label="总容量" width="100" />
        <el-table-column label="预约情况" width="120">
          <template #default="{ row }">
            {{ row.totalBookedCount || 0 }} / {{ row.totalSlotCapacity || row.totalCapacity }}
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '活跃' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="primary" size="small" link @click="handleManageSlots(row)">
              时段管理
            </el-button>
            <el-button type="primary" size="small" link @click="handleGenerateLink(row)">
              生成链接
            </el-button>
            <el-button
              v-if="row.active"
              type="danger"
              size="small"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button v-else type="success" size="small" link @click="handleReactivate(row)">
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </el-card>

    <!-- 创建/编辑任务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingTask ? '编辑任务' : '创建任务'"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属服务" prop="serviceId">
          <el-select v-model="form.serviceId" placeholder="请选择服务" style="width: 100%">
            <el-option
              v-for="service in activeServices"
              :key="service.id"
              :label="service.name"
              :value="service.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
          />
        </el-form-item>
        <el-form-item label="预约日期" prop="taskDate">
          <el-date-picker
            v-model="form.taskDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总容量" prop="totalCapacity">
          <el-input-number v-model="form.totalCapacity" :min="1" />
        </el-form-item>
        <el-form-item label="立即激活">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTask ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 时段管理对话框 -->
    <el-dialog v-model="showSlotsDialog" title="时段管理" width="800px">
      <div class="slots-toolbar">
        <el-button type="primary" size="small" @click="showAddSlotDialog = true">
          添加时段
        </el-button>
      </div>
      <el-table :data="slots" v-loading="loadingSlots">
        <el-table-column prop="startTime" label="开始时间" width="120">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="120">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容量" width="100" />
        <el-table-column prop="bookedCount" label="已预约" width="100" />
        <el-table-column label="可用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.capacity - row.bookedCount > 0 ? 'success' : 'danger'">
              {{ row.capacity - row.bookedCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.bookedCount === 0"
              type="danger"
              size="small"
              link
              @click="handleDeleteSlot(row)"
            >
              删除
            </el-button>
            <span v-else class="text-muted">有预约</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 添加时段表单 -->
      <el-divider v-if="showAddSlotDialog" content-position="left">添加时段</el-divider>
      <el-form v-if="showAddSlotDialog" :inline="true" :model="slotForm" class="slot-form">
        <el-form-item label="开始时间">
          <el-time-select
            v-model="slotForm.startTime"
            :max-time="slotForm.endTime"
            placeholder="开始时间"
            start="06:00"
            step="00:30"
            end="22:00"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select
            v-model="slotForm.endTime"
            :min-time="slotForm.startTime"
            placeholder="结束时间"
            start="06:00"
            step="00:30"
            end="22:00"
          />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="slotForm.capacity" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="addingSlot" @click="handleAddSlot">
            添加
          </el-button>
          <el-button @click="showAddSlotDialog = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 生成链接对话框 -->
    <el-dialog v-model="showLinkDialog" title="签名链接" width="500px">
      <el-form label-width="80px">
        <el-form-item label="任务">
          <el-input :value="currentTask?.title" disabled />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="generatedLink" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-input :value="linkExpiresAt" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLinkDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { taskApi, serviceApi, linkApi } from '@/api'
import type {
  AppointmentTask,
  AppointmentSlot,
  ServiceItem,
  AppointmentTaskRequest,
  AppointmentSlotRequest,
} from '@/types'

const loading = ref(false)
const submitting = ref(false)
const loadingSlots = ref(false)
const addingSlot = ref(false)

const tasks = ref<AppointmentTask[]>([])
const services = ref<ServiceItem[]>([])
const slots = ref<AppointmentSlot[]>([])

const filterStatus = ref<'all' | 'active' | 'inactive'>('all')
const filterService = ref<number | undefined>(undefined)

const showCreateDialog = ref(false)
const showSlotsDialog = ref(false)
const showAddSlotDialog = ref(false)
const showLinkDialog = ref(false)

const editingTask = ref<AppointmentTask | null>(null)
const currentTask = ref<AppointmentTask | null>(null)
const generatedLink = ref('')
const linkExpiresAt = ref('')

const formRef = ref<FormInstance>()

const activeServices = computed(() => services.value.filter((s) => s.active))

const form = reactive<AppointmentTaskRequest>({
  serviceId: 0,
  title: '',
  description: '',
  taskDate: '',
  totalCapacity: 10,
  active: true,
})

const slotForm = reactive<AppointmentSlotRequest>({
  startTime: '',
  endTime: '',
  capacity: 5,
})

const rules: FormRules = {
  serviceId: [{ required: true, message: '请选择服务', trigger: 'change' }],
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { min: 2, max: 100, message: '任务标题长度为2-100个字符', trigger: 'blur' },
  ],
  taskDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  totalCapacity: [{ required: true, message: '请输入总容量', trigger: 'blur' }],
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return '-'
  return timeStr.substring(0, 5)
}

const loadServices = async () => {
  try {
    const response = await serviceApi.getActive()
    services.value = response.data
  } catch {
    // ignore
  }
}

const loadTasks = async () => {
  loading.value = true
  try {
    let response
    if (filterStatus.value === 'active') {
      response = await taskApi.getActive()
    } else if (filterStatus.value === 'inactive') {
      response = await taskApi.getInactive()
    } else {
      response = await taskApi.getAll()
    }
    let taskList = response.data

    if (filterService.value) {
      taskList = taskList.filter((t) => t.serviceId === filterService.value)
    }
    tasks.value = taskList
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const loadSlots = async (taskId: number) => {
  loadingSlots.value = true
  try {
    const response = await taskApi.getSlots(taskId)
    slots.value = response.data
  } catch {
    ElMessage.error('加载时段失败')
  } finally {
    loadingSlots.value = false
  }
}

const handleEdit = (task: AppointmentTask) => {
  editingTask.value = task
  form.serviceId = task.serviceId
  form.title = task.title
  form.description = task.description || ''
  // 确保 taskDate 是字符串格式
  const dateStr = task.taskDate || ''
  form.taskDate = dateStr.includes('T') ? (dateStr.split('T')[0] || '') : dateStr
  form.totalCapacity = task.totalCapacity
  form.active = task.active
  showCreateDialog.value = true
}

const handleManageSlots = (task: AppointmentTask) => {
  currentTask.value = task
  showSlotsDialog.value = true
  showAddSlotDialog.value = false
  loadSlots(task.id)
}

const handleGenerateLink = async (task: AppointmentTask) => {
  try {
    const response = await linkApi.generate(task.id)
    const data = response.data
    generatedLink.value = `${window.location.origin}${data.link}`
    linkExpiresAt.value = new Date(data.expiresAtIso).toLocaleString('zh-CN')
    currentTask.value = task
    showLinkDialog.value = true
  } catch {
    ElMessage.error('生成链接失败')
  }
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(generatedLink.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const handleDelete = async (task: AppointmentTask) => {
  try {
    await ElMessageBox.confirm(`确定要删除任务"${task.title}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await taskApi.delete(task.id)
    ElMessage.success('删除成功')
    await loadTasks()
  } catch {
    // 用户取消
  }
}

const handleReactivate = async (task: AppointmentTask) => {
  try {
    await taskApi.reactivate(task.id)
    ElMessage.success('恢复成功')
    await loadTasks()
  } catch {
    ElMessage.error('恢复失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingTask.value) {
          await taskApi.update(editingTask.value.id, form)
          ElMessage.success('更新成功')
        } else {
          await taskApi.create(form)
          ElMessage.success('创建成功')
        }
        showCreateDialog.value = false
        await loadTasks()
      } catch {
        ElMessage.error(editingTask.value ? '更新失败' : '创建失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleAddSlot = async () => {
  if (!currentTask.value || !slotForm.startTime || !slotForm.endTime) {
    ElMessage.warning('请填写完整时段信息')
    return
  }

  addingSlot.value = true
  try {
    // 后端期望数组格式，所以用 [slotForm]
    await taskApi.createSlot(currentTask.value.id, [slotForm])
    ElMessage.success('添加成功')
    slotForm.startTime = ''
    slotForm.endTime = ''
    slotForm.capacity = 5
    await loadSlots(currentTask.value.id)
  } catch {
    ElMessage.error('添加失败')
  } finally {
    addingSlot.value = false
  }
}

const handleDeleteSlot = async (slot: AppointmentSlot) => {
  try {
    await ElMessageBox.confirm('确定要删除该时段吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (currentTask.value) {
      await taskApi.deleteSlot(currentTask.value.id, slot.id)
      ElMessage.success('删除成功')
      await loadSlots(currentTask.value.id)
    }
  } catch {
    // 用户取消
  }
}

const resetForm = () => {
  editingTask.value = null
  form.serviceId = 0
  form.title = ''
  form.description = ''
  form.taskDate = ''
  form.totalCapacity = 10
  form.active = true
  formRef.value?.resetFields()
}

onMounted(() => {
  loadServices()
  loadTasks()
})
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.mb-3 {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
}

.slots-toolbar {
  margin-bottom: 16px;
}

.slot-form {
  margin-top: 16px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
