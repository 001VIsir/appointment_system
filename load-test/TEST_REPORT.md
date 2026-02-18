# 压力测试报告

> 测试日期: 2026-02-18
> 测试工具: Apache JMeter 5.6.3

## 测试环境

| 项目 | 配置 |
|------|------|
| 后端服务 | http://localhost:8080 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.x |
| 测试工具 | JMeter 5.6.3 |
| JMeter安装路径 | D:\jmeter |

---

## 测试结果汇总

### 测试1: 100并发 (50线程, 10秒, 5循环)

| 指标 | 值 |
|------|-----|
| 总请求数 | 8900 |
| 成功请求 | 4800 (53.93%) |
| 失败请求 | 4100 (46.07%) |
| 吞吐量 | 456.32 req/s |
| 平均响应时间 | 231.80 ms |

### 测试2: 高并发 (100线程, 30秒, 20循环)

| 指标 | 值 |
|------|-----|
| 总请求数 | 8900 |
| 成功请求 | 7729 (86.84%) |
| 失败请求 | 1171 (13.16%) |
| 吞吐量 | 425.15 req/s |
| 平均响应时间 | 271.21 ms |
| P90 响应时间 | 648 ms |
| P99 响应时间 | 1128 ms |

### 测试3: 极端并发 (500线程, 60秒, 10循环)

| 指标 | 值 |
|------|-----|
| 总请求数 | 7900 |
| 吞吐量 | 418.3 req/s |
| 平均响应时间 | 247 ms |
| 最大响应时间 | 1838 ms |
| 总错误率 | 15.58% |

---

## 各场景详细结果

### 通过的测试 ✅

| 场景 | 错误率 | 平均响应时间 | 吞吐量 |
|------|--------|-------------|--------|
| 用户注册 | 0% | 475-608ms | 36-38/s |
| 用户登录 | 0% | 374-544ms | 29-34/s |
| 查询任务 | 0% | 259-326ms | 27-30/s |
| 查询时段 | 0% | 311ms | 28/s |
| 查询可用时段 | 0% | 295ms | 28/s |

### 预期内的失败 (乐观锁测试) ⚠️

| 场景 | 错误率 | 原因 |
|------|--------|------|
| 创建预约 | 19.8-61.6% | 时段容量有限 |
| 并发预约 | 72-79% | 乐观锁正确拒绝超售 |

---

## 问题修复记录

### 已修复的问题

1. **JMeter HTTP请求默认值配置错误**
   - 修复前: BASE_URL 变量在 path 字段
   - 修复后: 正确设置 domain/port/protocol

2. **数据库字段名不匹配**
   - duration_minutes → duration

3. **测试数据准备**
   - 预置 task_id=1 及其时段

---

## 性能评估

### 系统能力

- ✅ 支持 400+ QPS
- ✅ P99 响应时间 < 1200ms
- ✅ 核心 API 高并发下稳定
- ✅ 乐观锁正确处理并发预约

### 瓶颈分析

- 数据库连接池可能需要调优
- 时段容量需要合理规划
- Session 存储可能需要优化

---

## 报告文件

- `results_hc.jtl` - 高并发测试原始数据
- `html-report-hc/index.html` - 高并发HTML报告
- `results_extreme.jtl` - 极端测试原始数据
- `html-report-extreme/index.html` - 极端测试HTML报告

---

## 运行测试命令

```bash
# 高并发测试 (100线程)
powershell -Command "Set-Location 'D:\jmeter\bin'; .\jmeter.bat -n -t 'C:\Users\VISIR\IdeaProjects\appointment_system\load-test\appointment-system-load-test.jmx' -JTHREAD_COUNT=100 -JRAMP_UP=30 -JLOOP_COUNT=20 -l results.jtl -e -o html-report"

# 极端并发测试 (500线程)
powershell -Command "Set-Location 'D:\jmeter\bin'; .\jmeter.bat -n -t 'C:\Users\VISIR\IdeaProjects\appointment_system\load-test\appointment-system-load-test.jmx' -JTHREAD_COUNT=500 -JRAMP_UP=60 -JLOOP_COUNT=10 -l results.jtl -e -o html-report"
```
