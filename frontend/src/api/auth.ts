import apiClient from './client'
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
  ApiResponse,
} from '@/types'

export const authApi = {
  // 用户登录
  login(data: LoginRequest) {
    return apiClient.post<ApiResponse<UserResponse>>('/auth/login', data)
  },

  // 用户注册
  register(data: RegisterRequest) {
    return apiClient.post<ApiResponse<UserResponse>>('/auth/register', data)
  },

  // 用户登出
  logout() {
    return apiClient.post<ApiResponse<void>>('/auth/logout')
  },

  // 获取当前用户信息
  getCurrentUser() {
    return apiClient.get<ApiResponse<UserResponse>>('/auth/me')
  },

  // 检查用户名是否可用
  checkUsername(username: string) {
    return apiClient.get<ApiResponse<boolean>>('/auth/check-username', {
      params: { username },
    })
  },

  // 检查邮箱是否可用
  checkEmail(email: string) {
    return apiClient.get<ApiResponse<boolean>>('/auth/check-email', {
      params: { email },
    })
  },
}
