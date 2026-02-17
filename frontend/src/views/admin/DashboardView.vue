<template>
  <div class="admin-dashboard-view">
    <h2 class="page-title">系统仪表板</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">用户总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">商户数量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">预约总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">API 调用量</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt-3">
      <template #header>
        <span>系统统计</span>
      </template>
      <p>系统统计数据 - 将在后续实现</p>
    </el-card>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1890ff;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-top: 8px;
}
</style>
