import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import type { UserResponse, LoginRequest, RegisterRequest, UserRole } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<UserResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isMerchant = computed(() => user.value?.role === 'MERCHANT')
  const isUser = computed(() => user.value?.role === 'USER')

  // 方法
  async function login(credentials: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authApi.login(credentials)
      // Backend returns UserResponse directly, not wrapped in { data: ... }
      user.value = response.data
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authApi.register(data)
      // 注册成功后自动登录以创建 Session
      await authApi.login({
        username: data.username,
        password: data.password
      })
      user.value = response.data
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || '注册失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    loading.value = true
    try {
      await authApi.logout()
    } finally {
      user.value = null
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    loading.value = true
    try {
      const response = await authApi.getCurrentUser()
      // Backend returns UserResponse directly
      user.value = response.data
      return user.value
    } catch {
      user.value = null
      throw new Error('获取用户信息失败')
    } finally {
      loading.value = false
    }
  }

  function hasRole(role: UserRole): boolean {
    return user.value?.role === role
  }

  function hasAnyRole(roles: UserRole[]): boolean {
    return roles.includes(user.value?.role as UserRole)
  }

  function clearError() {
    error.value = null
  }

  return {
    // 状态
    user,
    loading,
    error,
    // 计算属性
    isAuthenticated,
    isAdmin,
    isMerchant,
    isUser,
    // 方法
    login,
    register,
    logout,
    fetchCurrentUser,
    hasRole,
    hasAnyRole,
    clearError,
  }
})
