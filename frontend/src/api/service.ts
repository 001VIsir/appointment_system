import apiClient from './client'
import type { ServiceItem, ServiceItemRequest, ApiResponse } from '@/types'

export const serviceApi = {
  // 获取所有服务项
  getAll() {
    return apiClient.get<ApiResponse<ServiceItem[]>>('/merchants/services')
  },

  // 获取活跃服务项
  getActive() {
    return apiClient.get<ApiResponse<ServiceItem[]>>('/merchants/services/active')
  },

  // 获取已删除服务项
  getInactive() {
    return apiClient.get<ApiResponse<ServiceItem[]>>('/merchants/services/inactive')
  },

  // 获取服务项详情
  getById(id: number) {
    return apiClient.get<ApiResponse<ServiceItem>>(`/merchants/services/${id}`)
  },

  // 创建服务项
  create(data: ServiceItemRequest) {
    return apiClient.post<ApiResponse<ServiceItem>>('/merchants/services', data)
  },

  // 更新服务项
  update(id: number, data: ServiceItemRequest) {
    return apiClient.put<ApiResponse<ServiceItem>>(`/merchants/services/${id}`, data)
  },

  // 删除服务项（软删除）
  delete(id: number) {
    return apiClient.delete<ApiResponse<void>>(`/merchants/services/${id}`)
  },

  // 重新激活服务项
  reactivate(id: number) {
    return apiClient.post<ApiResponse<ServiceItem>>(`/merchants/services/${id}/reactivate`)
  },

  // 统计服务项数量
  count(active?: boolean) {
    const params = active !== undefined ? { active } : {}
    return apiClient.get<ApiResponse<number>>('/merchants/services/count', { params })
  },

  // 检查服务名称是否存在
  checkNameExists(name: string) {
    return apiClient.get<ApiResponse<boolean>>('/merchants/services/exists', {
      params: { name },
    })
  },
}
