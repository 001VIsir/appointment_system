import apiClient from './client'
import type {
  AppointmentTask,
  AppointmentTaskRequest,
  AppointmentSlot,
  AppointmentSlotRequest,
  ApiResponse,
} from '@/types'

export const taskApi = {
  // 获取所有任务
  getAll() {
    return apiClient.get<ApiResponse<AppointmentTask[]>>('/merchants/tasks')
  },

  // 获取活跃任务
  getActive() {
    return apiClient.get<ApiResponse<AppointmentTask[]>>('/merchants/tasks/active')
  },

  // 获取已删除任务
  getInactive() {
    return apiClient.get<ApiResponse<AppointmentTask[]>>('/merchants/tasks/inactive')
  },

  // 按服务查询任务
  getByService(serviceId: number) {
    return apiClient.get<ApiResponse<AppointmentTask[]>>(
      `/merchants/tasks/by-service/${serviceId}`
    )
  },

  // 按日期范围查询任务
  getByDateRange(startDate: string, endDate: string) {
    return apiClient.get<ApiResponse<AppointmentTask[]>>('/merchants/tasks/by-date', {
      params: { startDate, endDate },
    })
  },

  // 获取任务详情（商户端）
  getById(id: number) {
    return apiClient.get<ApiResponse<AppointmentTask>>(`/merchants/tasks/${id}`)
  },

  // 公开获取任务详情（签名链接）
  getPublicById(id: number) {
    return apiClient.get<ApiResponse<AppointmentTask>>(`/tasks/${id}`)
  },

  // 创建任务
  create(data: AppointmentTaskRequest) {
    return apiClient.post<ApiResponse<AppointmentTask>>('/merchants/tasks', data)
  },

  // 更新任务
  update(id: number, data: AppointmentTaskRequest) {
    return apiClient.put<ApiResponse<AppointmentTask>>(`/merchants/tasks/${id}`, data)
  },

  // 删除任务（软删除）
  delete(id: number) {
    return apiClient.delete<ApiResponse<void>>(`/merchants/tasks/${id}`)
  },

  // 重新激活任务
  reactivate(id: number) {
    return apiClient.post<ApiResponse<AppointmentTask>>(`/merchants/tasks/${id}/reactivate`)
  },

  // 统计任务数
  count() {
    return apiClient.get<ApiResponse<number>>('/merchants/tasks/count')
  },

  // ========== 时段管理 ==========

  // 创建时段
  createSlot(taskId: number, data: AppointmentSlotRequest) {
    return apiClient.post<ApiResponse<AppointmentSlot>>(
      `/merchants/tasks/${taskId}/slots`,
      data
    )
  },

  // 批量创建时段
  createSlots(taskId: number, slots: AppointmentSlotRequest[]) {
    return apiClient.post<ApiResponse<AppointmentSlot[]>>(
      `/merchants/tasks/${taskId}/slots/batch`,
      slots
    )
  },

  // 获取时段列表（商户端）
  getSlots(taskId: number) {
    return apiClient.get<ApiResponse<AppointmentSlot[]>>(
      `/merchants/tasks/${taskId}/slots`
    )
  },

  // 删除时段
  deleteSlot(taskId: number, slotId: number) {
    return apiClient.delete<ApiResponse<void>>(
      `/merchants/tasks/${taskId}/slots/${slotId}`
    )
  },
}
