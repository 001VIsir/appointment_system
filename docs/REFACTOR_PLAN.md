# 精简重构计划 - 快速开始版

> 分为三个阶段，从最简单到最复杂，按顺序执行

---

## 阶段一：立即可做（无风险）

### 1. 移除 RabbitMQ 依赖
- 文件: `pom.xml`
- 操作: 删除 `spring-boot-starter-amqp`
- 效果: 减少依赖，启动更快

### 2. 移除 WebSocket 配置
- 文件: `websocket/` 目录, `WebSocketConfig.java`
- 操作: 评估是否需要实时通知，如不需要直接删除
- 效果: 代码更简洁

### 3. 禁用 Nacos
- 状态: 已默认禁用，保持即可

---

## 阶段二：按需优化（可选）

### 4. 缓存优化
- 当前: Redis
- 可选: Caffeine (单机更快)
- 条件: 单机部署时

### 5. Session 优化
- 当前: Redis Session
- 可选: 内存 Session
- 条件: 单机部署时

### 6. 限流优化
- 当前: Redis 限流
- 可选: Bucket4j
- 条件: 单机部署时

---

## 阶段三：架构调整（需测试）

### 7. 异步处理
- 移除 RabbitMQ 后用 @Async
- 确保核心业务流程正常

### 8. 统计优化
- Redis 统计 → 数据库统计

### 9. 监控精简
- 保留 Actuator
- 简化 Prometheus

---

## 推荐执行顺序

```
Step 1: 移除 RabbitMQ (pom.xml) ──────────────── 10分钟
    ↓
Step 2: 评估 WebSocket 需求 ──────────────────── 30分钟
    ↓
Step 3: 测试编译和运行 ────────────────────────── 10分钟
    ↓
Step 4: (可选) 缓存/限流优化 ──────────────────── 1小时
    ↓
Step 5: 更新文档 ──────────────────────────────── 30分钟
```

---

## 执行检查清单

- [ ] Step 1: 移除 RabbitMQ 依赖
- [ ] Step 2: 评估 WebSocket 需求并处理
- [ ] Step 3: 编译测试通过
- [ ] Step 4: 运行功能验证
- [ ] Step 5: 更新 feature_list.json
- [ ] Step 6: 更新 progress.md
- [ ] Step 7: 提交代码
