<template>
  <el-container class="default-layout">
    <el-header class="layout-header">
      <div class="header-left">
        <router-link to="/" class="logo">
          <el-icon :size="24"><Calendar /></el-icon>
          <span class="logo-text">预约系统</span>
        </router-link>
      </div>
      <div class="header-right">
        <template v-if="authStore.isAuthenticated">
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              <span>{{ authStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="myBookings">我的预约</el-dropdown-item>
                <el-dropdown-item v-if="authStore.isMerchant" command="merchant"
                  >商户中心</el-dropdown-item
                >
                <el-dropdown-item v-if="authStore.isAdmin" command="admin"
                  >管理后台</el-dropdown-item
                >
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main class="layout-main">
      <slot />
    </el-main>
    <el-footer class="layout-footer">
      <span>预约系统 © 2026</span>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const handleCommand = (command: string) => {
  switch (command) {
    case 'myBookings':
      router.push('/my-bookings')
      break
    case 'merchant':
      router.push('/merchant/dashboard')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      authStore.logout()
      router.push('/')
      break
  }
}
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  text-decoration: none;
  font-size: 18px;
  font-weight: 600;
}

.logo-text {
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}

.layout-main {
  background: #f5f7fa;
  padding: 0;
}

.layout-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  color: #909399;
  font-size: 14px;
  height: 50px;
}
</style>
