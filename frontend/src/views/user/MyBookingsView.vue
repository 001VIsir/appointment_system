<template>
  <div class="my-bookings-view page-container">
    <h2 class="page-title">我的预约</h2>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <div class="filter-bar">
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

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总预约数</div>
          </div>
          <el-icon class="stat-icon" :size="40"><Tickets /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-value text-warning">{{ stats.pending }}</div>
            <div class="stat-label">待确认</div>
          </div>
          <el-icon class="stat-icon text-warning" :size="40"><Clock /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-value text-success">{{ stats.confirmed }}</div>
            <div class="stat-label">已确认</div>
          </div>
          <el-icon class="stat-icon text-success" :size="40"><CircleCheck /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-value text-info">{{ stats.completed }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <el-icon class="stat-icon text-info" :size="40"><Finished /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预约列表 -->
    <el-card v-loading="loading" class="list-card">
      <el-empty v-if="!loading && bookings.length === 0" description="暂无预约记录">
        <el-button type="primary" @click="$router.push('/')">去预约</el-button>
      </el-empty>

      <div v-else class="bookings-list">
        <div v-for="booking in bookings" :key="booking.id" class="booking-item">
          <div class="booking-main">
            <div class="booking-header">
              <span class="booking-id">#{{ booking.id }}</span>
              <el-tag :type="getStatusType(booking.status as BookingStatus)" size="small">
                {{ statusLabels[booking.status as BookingStatus] }}
              </el-tag>
            </div>

            <div class="booking-info">
              <div class="info-row">
                <el-icon><Calendar /></el-icon>
                <span class="info-label">预约时间：</span>
                <span v-if="booking.slot" class="info-value">
                  {{ formatDate(booking.slot.startTime) }}
                  {{ formatTime(booking.slot.startTime) }} - {{ formatTime(booking.slot.endTime) }}
                </span>
              </div>

              <div class="info-row">
                <el-icon><Goods /></el-icon>
                <span class="info-label">服务项目：</span>
                <span class="info-value">
                  {{ booking.service?.name || '-' }}
                  <span v-if="booking.task" class="task-title">（{{ booking.task.title }}）</span>
                </span>
              </div>

              <div v-if="booking.merchant" class="info-row">
                <el-icon><Shop /></el-icon>
                <span class="info-label">商户：</span>
                <span class="info-value">{{ booking.merchant.businessName }}</span>
              </div>

              <div v-if="booking.remark" class="info-row">
                <el-icon><Document /></el-icon>
                <span class="info-label">备注：</span>
                <span class="info-value">{{ booking.remark }}</span>
              </div>

              <div class="info-row">
                <el-icon><Timer /></el-icon>
                <span class="info-label">创建时间：</span>
                <span class="info-value">{{ formatDateTime(booking.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="booking-actions">
            <template v-if="canCancel(booking.status as BookingStatus)">
              <el-button type="danger" size="small" @click="handleCancel(booking)">
                取消预约
              </el-button>
            </template>
            <template v-else>
              <span class="text-muted">{{ getActionText(booking.status as BookingStatus) }}</span>
            </template>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalElements > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next"
          @size-change="loadBookings"
          @current-change="loadBookings"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingApi } from '@/api'
import type { Booking, BookingStatus } from '@/types'

const loading = ref(false)
const bookings = ref<Booking[]>([])
const filterStatus = ref<'all' | BookingStatus>('all')

const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)

const stats = reactive({
  total: 0,
  pending: 0,
  confirmed: 0,
  completed: 0,
})

const statusLabels: Record<BookingStatus, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const getStatusType = (status: BookingStatus) => {
  const types: Record<BookingStatus, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'warning',
    CONFIRMED: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger',
  }
  return types[status]
}

const canCancel = (status: BookingStatus) => {
  return status === 'PENDING' || status === 'CONFIRMED'
}

const getActionText = (status: BookingStatus) => {
  const texts: Record<BookingStatus, string> = {
    PENDING: '等待商户确认',
    CONFIRMED: '请按时赴约',
    COMPLETED: '预约已完成',
    CANCELLED: '预约已取消',
  }
  return texts[status]
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
      const response = await bookingApi.getMyBookings(page, pageSize.value)
      const pageData = response.data
      bookings.value = pageData.content || []
      totalElements.value = pageData.totalElements || 0
    } else {
      const response = await bookingApi.getMyBookingsByStatus(filterStatus.value)
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

const loadStats = async () => {
  try {
    // 加载总数
    const totalRes = await bookingApi.countMyBookings()
    stats.total = totalRes.data || 0

    // 按状态统计
    const statusList: BookingStatus[] = ['PENDING', 'CONFIRMED', 'COMPLETED']
    for (const status of statusList) {
      const res = await bookingApi.getMyBookingsByStatus(status)
      stats[status.toLowerCase() as keyof typeof stats] = (res.data || []).length
    }
  } catch {
    // 忽略统计错误
  }
}

const handleCancel = async (booking: Booking) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该预约吗？取消后可能无法再次预约相同时段。',
      '取消预约',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '暂不取消',
        type: 'warning',
      }
    )

    await bookingApi.cancel(booking.id)
    ElMessage.success('预约已取消')
    await loadBookings()
    await loadStats()
  } catch {
    // 用户取消操作
  }
}

onMounted(() => {
  loadBookings()
  loadStats()
})
</script>

<style scoped>
.page-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 16px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
  line-height: 1.2;
}

.stat-value.text-warning {
  color: #e6a23c;
}

.stat-value.text-success {
  color: #67c23a;
}

.stat-value.text-info {
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-icon {
  color: #409eff;
  opacity: 0.3;
}

.stat-icon.text-warning {
  color: #e6a23c;
}

.stat-icon.text-success {
  color: #67c23a;
}

.stat-icon.text-info {
  color: #909399;
}

.list-card {
  min-height: 300px;
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.booking-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.booking-item:hover {
  border-color: #c0c4cc;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.booking-main {
  flex: 1;
}

.booking-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.booking-id {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.booking-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.info-row .el-icon {
  color: #909399;
}

.info-label {
  color: #909399;
  min-width: 70px;
}

.info-value {
  color: #303133;
}

.task-title {
  color: #909399;
  font-size: 12px;
}

.booking-actions {
  margin-left: 20px;
  display: flex;
  align-items: center;
}

.text-muted {
  color: #c0c4cc;
  font-size: 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
