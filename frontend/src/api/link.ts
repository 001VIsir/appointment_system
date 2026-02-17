import apiClient from './client'
import type { SignedLinkResponse, ApiResponse } from '@/types'

export const linkApi = {
  // 生成签名链接
  generate(taskId: number) {
    return apiClient.post<ApiResponse<SignedLinkResponse>>('/merchants/links', {
      taskId,
    })
  },

  // 验证签名链接
  verify(taskId: number, token: string, exp: string) {
    return apiClient.get<ApiResponse<{ valid: boolean; taskId: number }>>(
      '/public/links/verify',
      {
        params: { taskId, token, exp },
      }
    )
  },
}
