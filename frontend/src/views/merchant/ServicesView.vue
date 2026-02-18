<template>
  <div class="services-view">
    <h2 class="page-title">服务管理</h2>

    <!-- 操作栏 -->
    <el-card class="mb-3">
      <div class="toolbar">
        <el-radio-group v-model="filterStatus" @change="loadServices">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="active">活跃</el-radio-button>
          <el-radio-button value="inactive">已删除</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          添加服务
        </el-button>
      </div>
    </el-card>

    <!-- 服务列表 -->
    <el-card v-loading="loading">
      <el-table :data="services" style="width: 100%">
        <el-table-column prop="name" label="服务名称" min-width="150" />
        <el-table-column prop="category" label="类别" width="120">
          <template #default="{ row }">
            <el-tag>{{ categoryLabels[row.category as ServiceCategory] || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100">
          <template #default="{ row }">
            {{ row.duration }} 分钟
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '活跃' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">
              编辑
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
            <el-button
              v-else
              type="success"
              size="small"
              link
              @click="handleReactivate(row)"
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && services.length === 0" description="暂无服务项" />
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingService ? '编辑服务' : '添加服务'"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入服务名称" />
        </el-form-item>
        <el-form-item label="服务类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option
              v-for="(label, value) in categoryLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="服务描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入服务描述"
          />
        </el-form-item>
        <el-form-item label="服务时长" prop="duration">
          <el-input-number v-model="form.duration" :min="5" :max="480" />
          <span class="ml-2">分钟</span>
        </el-form-item>
        <el-form-item label="服务价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
          <span class="ml-2">元</span>
        </el-form-item>
        <el-form-item label="立即激活">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingService ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { serviceApi } from '@/api'
import type { ServiceItem, ServiceCategory, ServiceItemRequest } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const services = ref<ServiceItem[]>([])
const filterStatus = ref<'all' | 'active' | 'inactive'>('all')
const showCreateDialog = ref(false)
const editingService = ref<ServiceItem | null>(null)

const formRef = ref<FormInstance>()

const categoryLabels: Record<ServiceCategory, string> = {
  GENERAL: '综合服务',
  MEDICAL: '医疗健康',
  BEAUTY: '美容美发',
  CONSULTATION: '咨询服务',
  EDUCATION: '教育培训',
  FITNESS: '健身运动',
  OTHER: '其他',
}

const form = reactive<ServiceItemRequest>({
  name: '',
  description: '',
  category: 'GENERAL',
  duration: 30,
  price: 0,
  active: true,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入服务名称', trigger: 'blur' },
    { min: 2, max: 100, message: '服务名称长度为2-100个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择服务类别', trigger: 'change' }],
  duration: [{ required: true, message: '请输入服务时长', trigger: 'blur' }],
  price: [{ required: true, message: '请输入服务价格', trigger: 'blur' }],
}

const loadServices = async () => {
  loading.value = true
  try {
    let response
    if (filterStatus.value === 'active') {
      response = await serviceApi.getActive()
    } else if (filterStatus.value === 'inactive') {
      response = await serviceApi.getInactive()
    } else {
      response = await serviceApi.getAll()
    }
    services.value = response.data
  } catch {
    ElMessage.error('加载服务列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (service: ServiceItem) => {
  editingService.value = service
  form.name = service.name
  form.description = service.description || ''
  form.category = service.category
  form.duration = service.duration
  form.price = service.price
  form.active = service.active
  showCreateDialog.value = true
}

const handleDelete = async (service: ServiceItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除服务"${service.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await serviceApi.delete(service.id)
    ElMessage.success('删除成功')
    await loadServices()
  } catch {
    // 用户取消
  }
}

const handleReactivate = async (service: ServiceItem) => {
  try {
    await serviceApi.reactivate(service.id)
    ElMessage.success('恢复成功')
    await loadServices()
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
        if (editingService.value) {
          await serviceApi.update(editingService.value.id, form)
          ElMessage.success('更新成功')
        } else {
          await serviceApi.create(form)
          ElMessage.success('创建成功')
        }
        showCreateDialog.value = false
        await loadServices()
      } catch {
        ElMessage.error(editingService.value ? '更新失败' : '创建失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const resetForm = () => {
  editingService.value = null
  form.name = ''
  form.description = ''
  form.category = 'GENERAL'
  form.duration = 30
  form.price = 0
  form.active = true
  formRef.value?.resetFields()
}

onMounted(() => {
  loadServices()
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

.ml-2 {
  margin-left: 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
