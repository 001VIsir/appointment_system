// API 响应类型
export interface ApiResponse<T> {
  data: T
  message?: string
  code?: number
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  role: UserRole
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export type UserRole = 'ADMIN' | 'MERCHANT' | 'USER'

// 认证相关类型
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  role?: UserRole
}

export interface UserResponse {
  id: number
  username: string
  email: string
  role: UserRole
  enabled: boolean
  createdAt: string
}

// 商户相关类型
export interface MerchantProfile {
  id: number
  userId: number
  username: string
  businessName: string
  description?: string
  phone?: string
  address?: string
  settings?: MerchantSettings
  createdAt: string
  updatedAt: string
}

export interface MerchantSettings {
  sessionTimeout?: number
  notificationsEnabled?: boolean
  timezone?: string
  bookingAdvanceDays?: number
  cancelDeadlineHours?: number
  autoConfirmBookings?: boolean
  maxBookingsPerUserPerDay?: number
}

export interface MerchantProfileRequest {
  businessName: string
  description?: string
  phone?: string
  address?: string
}

export interface MerchantSettingsRequest {
  sessionTimeout?: number
  notificationsEnabled?: boolean
  timezone?: string
  bookingAdvanceDays?: number
  cancelDeadlineHours?: number
  autoConfirmBookings?: boolean
  maxBookingsPerUserPerDay?: number
}

// 服务项相关类型
export interface ServiceItem {
  id: number
  merchantId: number
  merchantName: string
  name: string
  description?: string
  category: ServiceCategory
  duration: number
  price: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export type ServiceCategory =
  | 'GENERAL'
  | 'MEDICAL'
  | 'BEAUTY'
  | 'CONSULTATION'
  | 'EDUCATION'
  | 'FITNESS'
  | 'OTHER'

export interface ServiceItemRequest {
  name: string
  description?: string
  category: ServiceCategory
  duration: number
  price: number
  active?: boolean
}

// 预约任务相关类型
export interface AppointmentTask {
  id: number
  serviceId: number
  serviceName: string
  title: string
  description?: string
  taskDate: string
  totalCapacity: number
  active: boolean
  slotCount?: number
  totalSlotCapacity?: number
  totalBookedCount?: number
  createdAt: string
  updatedAt: string
}

export interface AppointmentTaskRequest {
  serviceId: number
  title: string
  description?: string
  taskDate: string
  totalCapacity: number
  active?: boolean
}

// 时段相关类型
export interface AppointmentSlot {
  id: number
  taskId: number
  startTime: string
  endTime: string
  capacity: number
  bookedCount: number
  hasCapacity?: boolean
  availableCapacity?: number
  createdAt: string
  updatedAt: string
}

export interface AppointmentSlotRequest {
  startTime: string
  endTime: string
  capacity: number
}

// 预约相关类型
export interface Booking {
  id: number
  userId: number
  slotId: number
  status: BookingStatus
  remark?: string
  version: number
  slot?: AppointmentSlot
  task?: AppointmentTask
  service?: ServiceItem
  merchant?: MerchantProfile
  createdAt: string
  updatedAt: string
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'

export interface BookingRequest {
  slotId: number
  remark?: string
}

// 签名链接相关类型
export interface SignedLinkResponse {
  link: string
  fullUrl: string
  taskId: number
  expiresAt: string
  expiresAtIso: string
  valid: boolean
}

// 统计相关类型
export interface BookingStatsResponse {
  totalBookings: number
  activeBookings: number
  pendingBookings: number
  confirmedBookings: number
  completedBookings: number
  cancelledBookings: number
  todayBookings: number
  dateRangeBookings: number
  completionRate: number
  cancellationRate: number
  confirmationRate: number
}

export interface UserStatsResponse {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  adminCount: number
  merchantCount: number
  userCount: number
  todayRegistrations: number
  thisWeekRegistrations: number
  thisMonthRegistrations: number
  usersWithBookings: number
  merchantsWithServices: number
}

export interface SystemStatsResponse {
  todayApiCalls: number
  lastHourApiCalls: number
  averageCallsPerMinute: number
  totalErrors: number
  clientErrors: number
  serverErrors: number
  averageResponseTime: number
  maxResponseTime: number
  minResponseTime: number
  p95ResponseTime: number
  activeSessions: number
  heapMemoryUsed: number
  heapMemoryMax: number
  uptime: number
}

// 路由元信息类型
export interface RouteMeta {
  title?: string
  requiresAuth?: boolean
  roles?: UserRole[]
  layout?: 'default' | 'merchant' | 'admin' | 'none'
  [key: string]: unknown
}
