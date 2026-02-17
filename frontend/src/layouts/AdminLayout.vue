<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="layout-aside">
      <div class="sidebar-header">
        <el-icon :size="24"><Setting /></el-icon>
        <span class="sidebar-title">管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#rgba(255, 255, 255, 0.65)"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>系统仪表板</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button text @click="goHome">
          <el-icon><Back /></el-icon>
          <span>返回首页</span>
        </el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              <span>{{ authStore.user?.username }}</span>
              <el-tag size="small" type="danger" class="admin-tag">管理员</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回首页</el-dropdown-item>
                <el-dropdown-item command="merchant">商户中心</el-dropdown-item>
                <el-dropdown-item command="myBookings">我的预约</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const meta = route.meta
  return (meta?.title as string) || '管理后台'
})

const goHome = () => {
  router.push('/')
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'home':
      router.push('/')
      break
    case 'merchant':
      router.push('/merchant/dashboard')
      break
    case 'myBookings':
      router.push('/my-bookings')
      break
    case 'logout':
      authStore.logout()
      router.push('/')
      break
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.layout-aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
}

.el-menu {
  border-right: none;
  flex: 1;
}

.sidebar-footer {
  padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-footer .el-button {
  color: rgba(255, 255, 255, 0.65);
  width: 100%;
  justify-content: flex-start;
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

.header-left .page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}

.admin-tag {
  margin-left: 4px;
}

.layout-main {
  background: #f0f2f5;
  padding: 20px;
}
</style>
