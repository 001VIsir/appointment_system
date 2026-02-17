<template>
  <div class="merchant-dashboard-view">
    <h2 class="page-title">商户仪表板</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">今日预约</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">待确认</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">服务项目</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">--</div>
          <div class="stat-label">活跃任务</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt-3">
      <template #header>
        <span>最近预约</span>
      </template>
      <p>预约列表 - 将在 FEAT-031 实现</p>
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
  color: #409eff;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-top: 8px;
}
</style>
