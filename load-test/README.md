# 压力测试文档

> FEAT-036: 预约系统压力测试

## 概述

本目录包含预约系统的 JMeter 压力测试计划和测试数据准备脚本。

## 前置要求

### 软件要求

- **JDK 21+**: 运行后端应用
- **JMeter 5.6+**: 执行压力测试
  - 下载地址: https://jmeter.apache.org/download_jmeter.cgi
- **本地服务**: 手动启动 MySQL、Redis
- **MySQL 8.0+**: 数据库
- **Redis 7.x**: 缓存服务

### 环境准备

1. **启动后端服务**

   ```bash
   # 使用 Maven
   ./mvnw spring-boot:run
   ```

   确保本地 MySQL、Redis、RabbitMQ 服务已启动。

2. **准备测试数据**

   ```bash
   # 连接数据库并执行数据准备脚本
   mysql -u root -p appointment_system < load-test/prepare-test-data.sql
   ```

## 测试场景

### 场景1: 用户注册压力测试

- **目标端点**: `POST /api/auth/register`
- **线程数**: 50 (可配置)
- **循环次数**: 10 (可配置)
- **测试内容**: 并发用户注册，验证系统处理高并发注册请求的能力

### 场景2: 用户登录压力测试

- **目标端点**: `POST /api/auth/login`
- **线程数**: 50 (可配置)
- **循环次数**: 10 (可配置)
- **测试内容**: 先注册后登录，验证认证系统在高并发下的表现

### 场景3: 查询任务压力测试

- **目标端点**:
  - `GET /api/tasks/{id}`
  - `GET /api/tasks/{id}/slots`
  - `GET /api/tasks/{id}/slots/available`
- **线程数**: 50 (可配置)
- **循环次数**: 10 (可配置)
- **测试内容**: 查询任务详情和可用时段，测试只读性能

### 场景4: 完整预约流程测试

- **流程**: 注册 → 登录 → 查询任务 → 查询时段 → 创建预约
- **线程数**: 50 (可配置)
- **循环次数**: 10 (可配置)
- **测试内容**: 完整业务流程压力测试

### 场景5: 并发预约压力测试

- **目标**: 测试乐观锁在高并发预约场景下的表现
- **线程数**: 100 (同时启动)
- **同步策略**: 使用 Synchronizing Timer 实现真正并发
- **测试内容**: 100个用户同时预约同一个时段，验证乐观锁正确性

## 运行测试

### 使用 JMeter GUI

```bash
# 启动 JMeter GUI
jmeter

# 打开测试计划
# File -> Open -> load-test/appointment-system-load-test.jmx
```

### 使用命令行 (推荐用于大规模测试)

```bash
# 基本运行
jmeter -n -t load-test/appointment-system-load-test.jmx -l results.jtl

# 生成 HTML 报告
jmeter -n -t load-test/appointment-system-load-test.jmx \
  -l results.jtl \
  -e -o html-report

# 使用自定义参数
jmeter -n -t load-test/appointment-system-load-test.jmx \
  -JTHREAD_COUNT=100 \
  -JRAMP_UP=30 \
  -JLOOP_COUNT=20 \
  -l results.jtl \
  -e -o html-report
```

### 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| BASE_URL | http://localhost:8080 | 后端服务地址 |
| THREAD_COUNT | 50 | 并发线程数 |
| RAMP_UP | 10 | 启动时间(秒) |
| LOOP_COUNT | 10 | 每个线程循环次数 |

## 性能指标

### 预期目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 平均响应时间 | < 500ms | 90% 请求 |
| 吞吐量 | > 100 TPS | 每秒事务数 |
| 错误率 | < 1% | 失败请求比例 |
| 并发预约成功率 | 100% | 乐观锁正确性 |

### 关键监控指标

1. **响应时间**
   - 平均响应时间
   - 90% 线 (90th percentile)
   - 99% 线 (99th percentile)
   - 最大响应时间

2. **吞吐量**
   - 每秒请求数 (RPS/QPS)
   - 每秒事务数 (TPS)

3. **错误率**
   - HTTP 4xx 错误
   - HTTP 5xx 错误
   - 超时错误

4. **系统资源**
   - CPU 使用率
   - 内存使用率
   - 数据库连接池
   - Redis 连接数

## 测试报告

### 查看结果

1. **HTML 报告**: 生成后打开 `html-report/index.html`
2. **JTL 文件**: 可导入 JMeter GUI 分析
3. **控制台输出**: 实时查看测试进度

### 报告分析

```bash
# 生成 CSV 格式报告
jmeter -n -t load-test/appointment-system-load-test.jmx \
  -l results.jtl \
  -Jjmeter.save.saveservice.output_format=csv

# 查看汇总统计
cat results.jtl | grep -E "summary"
```

## 性能优化建议

### 数据库优化

1. 确保索引正确创建
2. 使用连接池 (HikariCP)
3. 考虑读写分离

### 缓存优化

1. 启用 Redis 缓存
2. 配置合理的过期时间
3. 使用 Spring Cache 注解

### JVM 优化

```bash
# 建议的 JVM 参数
java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar
```

## 故障排除

### 常见问题

1. **连接超时**
   - 检查后端服务是否运行
   - 确认端口是否正确
   - 检查防火墙设置

2. **高错误率**
   - 查看后端日志
   - 检查数据库连接池
   - 确认 Redis 可用

3. **性能不达标**
   - 检查数据库慢查询
   - 分析 JVM 内存使用
   - 考虑增加资源

## 文件说明

```
load-test/
├── appointment-system-load-test.jmx  # JMeter 测试计划
├── prepare-test-data.sql             # 测试数据准备脚本
├── run-load-test.sh                  # Linux/Mac 运行脚本
├── run-load-test.bat                 # Windows 运行脚本
└── README.md                         # 本文档
```

## 附录

### API 端点汇总

| 端点 | 方法 | 认证 | 说明 |
|------|------|------|------|
| /api/auth/register | POST | 无 | 用户注册 |
| /api/auth/login | POST | 无 | 用户登录 |
| /api/auth/logout | POST | 需要 | 用户登出 |
| /api/auth/me | GET | 需要 | 获取当前用户 |
| /api/tasks/{id} | GET | 无 | 获取任务详情 |
| /api/tasks/{id}/slots | GET | 无 | 获取任务时段 |
| /api/tasks/{id}/slots/available | GET | 无 | 获取可用时段 |
| /api/bookings | POST | 需要 | 创建预约 |
| /api/bookings/my | GET | 需要 | 获取我的预约 |

### 测试用户数据格式

```json
{
  "username": "testuser_${timestamp}_${thread}",
  "password": "Test@123456",
  "email": "testuser_${timestamp}_${thread}@test.com",
  "role": "USER"
}
```
