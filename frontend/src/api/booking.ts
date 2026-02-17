import apiClient from './client'
import type {
  Booking,
  BookingRequest,
  BookingStatus,
  AppointmentSlot,
  ApiResponse,
  PageResponse,
} from '@/types'

export const bookingApi = {
  // ========== 用户端 API ==========

  // 创建预约
  create(data: BookingRequest) {
    return apiClient.post<ApiResponse<Booking>>('/bookings', data)
  },

  // 获取我的预约列表（分页）
  getMyBookings(page = 0, size = 10) {
    return apiClient.get<ApiResponse<PageResponse<Booking>>>('/bookings/my', {
      params: { page, size },
    })
  },

  // 获取我的活跃预约
  getMyActiveBookings() {
    return apiClient.get<ApiResponse<Booking[]>>('/bookings/my/active')
  },

  // 按状态获取我的预约
  getMyBookingsByStatus(status: BookingStatus) {
    return apiClient.get<ApiResponse<Booking[]>>(`/bookings/my/status/${status}`)
  },

  // 获取预约详情
  getById(id: number) {
    return apiClient.get<ApiResponse<Booking>>(`/bookings/${id}`)
  },

  // 取消预约
  cancel(id: number) {
    return apiClient.delete<ApiResponse<Booking>>(`/bookings/${id}`)
  },

  // 统计我的预约数
  countMyBookings() {
    return apiClient.get<ApiResponse<number>>('/bookings/my/count')
  },

  // 统计我的活跃预约数
  countMyActiveBookings() {
    return apiClient.get<ApiResponse<number>>('/bookings/my/count/active')
  },

  // 检查是否已预约某时段
  hasBookingForSlot(slotId: number) {
    return apiClient.get<ApiResponse<boolean>>(`/bookings/my/has-booking/${slotId}`)
  },

  // ========== 商户端 API ==========

  // 获取商户预约列表（分页）
  getMerchantBookings(page = 0, size = 10) {
    return apiClient.get<ApiResponse<PageResponse<Booking>>>('/merchants/bookings', {
      params: { page, size },
    })
  },

  // 按状态获取商户预约
  getMerchantBookingsByStatus(status: BookingStatus) {
    return apiClient.get<ApiResponse<Booking[]>>(`/merchants/bookings/status/${status}`)
  },

  // 确认预约
  confirm(id: number) {
    return apiClient.put<ApiResponse<Booking>>(`/merchants/bookings/${id}/confirm`)
  },

  // 完成预约
  complete(id: number) {
    return apiClient.put<ApiResponse<Booking>>(`/merchants/bookings/${id}/complete`)
  },

  // 商户取消预约
  merchantCancel(id: number) {
    return apiClient.delete<ApiResponse<Booking>>(`/merchants/bookings/${id}`)
  },

  // ========== 时段 API ==========

  // 获取任务时段列表
  getTaskSlots(taskId: number) {
    return apiClient.get<ApiResponse<AppointmentSlot[]>>(`/tasks/${taskId}/slots`)
  },

  // 获取有容量的时段
  getAvailableSlots(taskId: number) {
    return apiClient.get<ApiResponse<AppointmentSlot[]>>(
      `/tasks/${taskId}/slots/available`
    )
  },

  // 获取时段详情
  getSlotById(slotId: number) {
    return apiClient.get<ApiResponse<AppointmentSlot>>(`/slots/${slotId}`)
  },
}
