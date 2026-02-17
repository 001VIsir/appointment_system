<template>
  <div class="home-view">
    <div class="hero-section">
      <h1>预约管理系统</h1>
      <p class="subtitle">高效便捷的在线预约解决方案</p>
      <div class="action-buttons">
        <el-button type="primary" size="large" @click="$router.push('/register')">
          立即注册
        </el-button>
        <el-button size="large" @click="$router.push('/login')"> 登录 </el-button>
      </div>
    </div>
    <div class="features-section">
      <el-row :gutter="24">
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <el-icon :size="48" color="#409eff"><Calendar /></el-icon>
            <h3>便捷预约</h3>
            <p>通过签名链接快速预约，无需繁琐的注册流程</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <el-icon :size="48" color="#67c23a"><Shop /></el-icon>
            <h3>商户管理</h3>
            <p>灵活的服务配置和时段管理，满足各种业务需求</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <el-icon :size="48" color="#e6a23c"><Bell /></el-icon>
            <h3>实时通知</h3>
            <p>WebSocket 实时推送预约状态变更，及时响应</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.home-view {
  min-height: calc(100vh - 110px);
}

.hero-section {
  text-align: center;
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.hero-section h1 {
  font-size: 48px;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 20px;
  margin-bottom: 40px;
  opacity: 0.9;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.features-section {
  padding: 60px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  text-align: center;
  padding: 20px;
}

.feature-card h3 {
  margin: 20px 0 10px;
  font-size: 18px;
  color: #303133;
}

.feature-card p {
  color: #909399;
  font-size: 14px;
  line-height: 1.6;
}
</style>
