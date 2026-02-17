import apiClient from './client'
import type {
  SystemStatsResponse,
  BookingStatsResponse,
  UserStatsResponse,
  ApiResponse,
} from '@/types'

export const adminApi = {
  // 获取系统指标
  getMetrics() {
    return apiClient.get<ApiResponse<SystemStatsResponse>>('/admin/metrics')
  },

  // 获取预约统计
  getBookingStats(startDate?: string, endDate?: string) {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return apiClient.get<ApiResponse<BookingStatsResponse>>('/admin/stats/bookings', {
      params,
    })
  },

  // 获取用户统计
  getUserStats() {
    return apiClient.get<ApiResponse<UserStatsResponse>>('/admin/stats/users')
  },

  // 获取仪表板综合统计
  getDashboard() {
    return apiClient.get<
      ApiResponse<{
        bookings: BookingStatsResponse
        users: UserStatsResponse
        system: SystemStatsResponse
      }>
    >('/admin/stats/dashboard')
  },
}
