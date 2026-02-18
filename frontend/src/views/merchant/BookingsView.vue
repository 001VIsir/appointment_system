<template>
  <div class="bookings-view">
    <h2 class="page-title">预约管理</h2>

    <!-- 操作栏 -->
    <el-card class="mb-3">
      <div class="toolbar">
        <el-radio-group v-model="filterStatus" @change="loadBookings">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="PENDING">待确认</el-radio-button>
          <el-radio-button value="CONFIRMED">已确认</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="CANCELLED">已取消</el-radio-button>
        </el-radio-group>
        <el-button @click="loadBookings">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 预约列表 -->
    <el-card v-loading="loading">
      <el-table :data="bookings" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="预约时段" min-width="180">
          <template #default="{ row }">
            <div v-if="row.slot">
              {{ formatDate(row.slot.startTime) }}
              {{ formatTime(row.slot.startTime) }} - {{ formatTime(row.slot.endTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="服务/任务" min-width="150">
          <template #default="{ row }">
            <div v-if="row.service">{{ row.service.name }}</div>
            <div v-if="row.task" class="text-muted">{{ row.task.title }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户ID" width="100">
          <template #default="{ row }">
            {{ row.userId }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status as BookingStatus)">
              {{ statusLabels[row.status as BookingStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120">
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" link @click="handleConfirm(row)">
                确认
              </el-button>
              <el-button type="danger" size="small" link @click="handleCancel(row)">
                取消
              </el-button>
            </template>
            <template v-else-if="row.status === 'CONFIRMED'">
              <el-button type="primary" size="small" link @click="handleComplete(row)">
                完成
              </el-button>
              <el-button type="danger" size="small" link @click="handleCancel(row)">
                取消
              </el-button>
            </template>
            <template v-else>
              <span class="text-muted">无操作</span>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadBookings"
          @current-change="loadBookings"
        />
      </div>

      <el-empty v-if="!loading && bookings.length === 0" description="暂无预约" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingApi } from '@/api'
import type { Booking, BookingStatus } from '@/types'

const loading = ref(false)
const bookings = ref<Booking[]>([])
const filterStatus = ref<'all' | BookingStatus>('all')

const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)

const statusLabels: Record<BookingStatus, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const getStatusType = (status: BookingStatus) => {
  const types: Record<BookingStatus, string> = {
    PENDING: 'warning',
    CONFIRMED: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger',
  }
  return types[status]
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(11, 16)
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadBookings = async () => {
  loading.value = true
  try {
    const page = currentPage.value - 1

    if (filterStatus.value === 'all') {
      const response = await bookingApi.getMerchantBookings(page, pageSize.value)
      const pageData = response.data
      bookings.value = pageData.content || []
      totalElements.value = pageData.totalElements || 0
    } else {
      const response = await bookingApi.getMerchantBookingsByStatus(filterStatus.value)
      const data = response.data
      bookings.value = Array.isArray(data) ? data : []
      totalElements.value = bookings.value.length
    }
  } catch {
    ElMessage.error('加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleConfirm = async (booking: Booking) => {
  try {
    await ElMessageBox.confirm('确定要确认该预约吗？', '确认预约', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await bookingApi.confirm(booking.id)
    ElMessage.success('确认成功')
    await loadBookings()
  } catch {
    // 用户取消
  }
}

const handleComplete = async (booking: Booking) => {
  try {
    await ElMessageBox.confirm('确定要将该预约标记为完成吗？', '完成预约', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await bookingApi.complete(booking.id)
    ElMessage.success('操作成功')
    await loadBookings()
  } catch {
    // 用户取消
  }
}

const handleCancel = async (booking: Booking) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？此操作不可恢复。', '取消预约', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await bookingApi.merchantCancel(booking.id)
    ElMessage.success('取消成功')
    await loadBookings()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadBookings()
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

.text-muted {
  color: #909399;
  font-size: 12px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
