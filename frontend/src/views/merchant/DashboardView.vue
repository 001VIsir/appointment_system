<template>
  <div class="merchant-dashboard-view">
    <h2 class="page-title">商户仪表板</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.todayBookings }}</div>
          <div class="stat-label">今日预约</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.pendingBookings }}</div>
          <div class="stat-label">待确认</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.serviceCount }}</div>
          <div class="stat-label">服务项目</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.activeTasks }}</div>
          <div class="stat-label">活跃任务</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 更多统计 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value text-success">{{ stats.confirmedBookings }}</div>
          <div class="stat-label">已确认</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value text-info">{{ stats.completedBookings }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value text-danger">{{ stats.cancelledBookings }}</div>
          <div class="stat-label">已取消</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.completionRate }}%</div>
          <div class="stat-label">完成率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近预约 -->
    <el-card class="mt-3">
      <template #header>
        <div class="card-header">
          <span>最近预约</span>
          <el-button type="primary" size="small" link @click="$router.push('/merchant/bookings')">
            查看全部
          </el-button>
        </div>
      </template>

      <el-table v-loading="loadingBookings" :data="recentBookings" style="width: 100%">
        <el-table-column label="预约时段" min-width="180">
          <template #default="{ row }">
            <div v-if="row.slot">
              {{ formatDate(row.slot.startTime) }}
              {{ formatTime(row.slot.startTime) }} - {{ formatTime(row.slot.endTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="服务" min-width="120">
          <template #default="{ row }">
            {{ row.service?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status as BookingStatus)" size="small">
              {{ statusLabels[row.status as BookingStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" link @click="handleConfirm(row)">
                确认
              </el-button>
            </template>
            <template v-else-if="row.status === 'CONFIRMED'">
              <el-button type="primary" size="small" link @click="handleComplete(row)">
                完成
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loadingBookings && recentBookings.length === 0" description="暂无预约" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi, serviceApi, taskApi, bookingApi } from '@/api'
import type { Booking, BookingStatus } from '@/types'

const loadingBookings = ref(false)
const recentBookings = ref<Booking[]>([])

const stats = reactive({
  todayBookings: 0,
  pendingBookings: 0,
  confirmedBookings: 0,
  completedBookings: 0,
  cancelledBookings: 0,
  serviceCount: 0,
  activeTasks: 0,
  completionRate: 0,
})

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

const loadStats = async () => {
  try {
    // 加载预约统计
    const bookingStatsRes = await merchantApi.getStats()
    const bookingStats = bookingStatsRes.data.data
    stats.todayBookings = bookingStats.todayBookings || 0
    stats.pendingBookings = bookingStats.pendingBookings || 0
    stats.confirmedBookings = bookingStats.confirmedBookings || 0
    stats.completedBookings = bookingStats.completedBookings || 0
    stats.cancelledBookings = bookingStats.cancelledBookings || 0
    stats.completionRate = Math.round(bookingStats.completionRate || 0)

    // 加载服务数量
    const serviceCountRes = await serviceApi.count(true)
    stats.serviceCount = serviceCountRes.data.data || 0

    // 加载活跃任务数
    const tasksRes = await taskApi.getActive()
    stats.activeTasks = tasksRes.data.data?.length || 0
  } catch {
    // 忽略错误
  }
}

const loadRecentBookings = async () => {
  loadingBookings.value = true
  try {
    const response = await bookingApi.getMerchantBookings(0, 5)
    recentBookings.value = response.data.data?.content || []
  } catch {
    // 忽略错误
  } finally {
    loadingBookings.value = false
  }
}

const handleConfirm = async (booking: Booking) => {
  try {
    await bookingApi.confirm(booking.id)
    ElMessage.success('确认成功')
    await Promise.all([loadStats(), loadRecentBookings()])
  } catch {
    ElMessage.error('确认失败')
  }
}

const handleComplete = async (booking: Booking) => {
  try {
    await bookingApi.complete(booking.id)
    ElMessage.success('操作成功')
    await Promise.all([loadStats(), loadRecentBookings()])
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadStats()
  loadRecentBookings()
})
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
}

.text-success {
  color: #67c23a;
}

.text-info {
  color: #909399;
}

.text-danger {
  color: #f56c6c;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-top: 8px;
}

.mt-3 {
  margin-top: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
