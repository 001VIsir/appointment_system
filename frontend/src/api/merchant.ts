import apiClient from './client'
import type {
  MerchantProfile,
  MerchantProfileRequest,
  MerchantSettings,
  MerchantSettingsRequest,
  BookingStatsResponse,
  ApiResponse,
} from '@/types'

export const merchantApi = {
  // 获取商户档案
  getProfile() {
    return apiClient.get<ApiResponse<MerchantProfile>>('/merchants/profile')
  },

  // 创建商户档案
  createProfile(data: MerchantProfileRequest) {
    return apiClient.post<ApiResponse<MerchantProfile>>('/merchants/profile', data)
  },

  // 更新商户档案
  updateProfile(data: MerchantProfileRequest) {
    return apiClient.put<ApiResponse<MerchantProfile>>('/merchants/profile', data)
  },

  // 删除商户档案
  deleteProfile() {
    return apiClient.delete<ApiResponse<void>>('/merchants/profile')
  },

  // 检查档案是否存在
  hasProfile() {
    return apiClient.get<ApiResponse<boolean>>('/merchants/profile/exists')
  },

  // 获取商户设置
  getSettings() {
    return apiClient.get<ApiResponse<MerchantSettings>>('/merchants/settings')
  },

  // 更新商户设置
  updateSettings(data: MerchantSettingsRequest) {
    return apiClient.put<ApiResponse<MerchantSettings>>('/merchants/settings', data)
  },

  // 获取商户预约统计
  getStats() {
    return apiClient.get<ApiResponse<BookingStatsResponse>>('/merchants/stats')
  },
}
