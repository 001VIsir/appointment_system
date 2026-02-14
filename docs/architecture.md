# 系统架构文档

## 1. 总体架构

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户端 / 商户端                                  │
│                    (Vue 3 + Element Plus + TypeScript)                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Spring Cloud Gateway                                  │
│              (路由 / 限流 / 认证 / 日志 / CORS)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│    Appointment        │ │    User Service       │ │   Stats Service       │
│    Service            │ │    (认证/商户/用户)    │ │   (统计/监控)          │
│    (预约核心)          │ │                       │ │                       │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘
         │                         │                         │
         └────────────┬────────────┴────────────┬────────────┘
                      ▼                         ▼
         ┌─────────────────────┐   ┌─────────────────────┐
         │      RabbitMQ       │   │       Redis         │
         │   (预约请求队列)     │   │ (Session/缓存/限流)  │
         └─────────────────────┘   └─────────────────────┘
                      │
                      ▼
         ┌─────────────────────────────────────────────────┐
         │                   MySQL 8                       │
         │              (主数据存储)                        │
         └─────────────────────────────────────────────────┘
```

### 1.2 技术栈概览

| 层级 | 技术选型 |
|------|----------|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| API 网关 | Spring Cloud Gateway 4.x |
| 后端框架 | Spring Boot 4.0.2 + Java 21 |
| ORM | Spring Data JPA + Hibernate |
| 安全框架 | Spring Security 6.x |
| 缓存 | Redis 7.x (Session + 缓存 + 限流) |
| 消息队列 | RabbitMQ 3.12 |
| 数据库 | MySQL 8.0 |
| 数据库迁移 | Flyway 10.x |
| API 文档 | Springdoc OpenAPI 2.x |
| 监控 | Micrometer + Prometheus |
| 容器化 | Docker + Docker Compose |

## 2. 后端架构

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot 4.0.2                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Controller  │  │  Service    │  │    Repository       │  │
│  │   (REST)    │──│  (Business) │──│  (Spring Data JPA)  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│         │                │                    │              │
│  ┌──────┴──────┐  ┌──────┴──────┐     ┌──────┴──────┐       │
│  │ Validation  │  │   Redis     │     │   MySQL 8   │       │
│  │ (Jakarta)   │  │ (Session+)  │     │ (Flyway)    │       │
│  └─────────────┘  └─────────────┘     └─────────────┘       │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Spring      │  │ Springdoc   │  │  Micrometer         │  │
│  │ Security    │  │ OpenAPI     │  │  (Prometheus)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块 | 职责 | 核心类 |
|------|------|--------|
| **Gateway** | API 网关，路由/限流/认证 | GatewayConfig, RateLimitFilter |
| **Auth** | 用户认证与授权 | AuthService, AuthController, SecurityConfig |
| **User** | 用户管理 | UserService, UserController, User实体 |
| **Merchant** | 商户管理 | MerchantService, MerchantController, MerchantProfile实体 |
| **Service Item** | 服务项目管理 | ServiceItemService, ServiceItemController |
| **Appointment** | 预约任务与时段 | AppointmentTask, AppointmentSlot, BookingService |
| **Stats** | 统计与监控 | StatisticsService, MetricsController |

### 2.3 包结构

```
org.example.appointment_system/
├── config/                 # 配置类
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── RabbitMQConfig.java
│   └── OpenApiConfig.java
├── controller/             # REST 控制器
│   ├── AuthController.java
│   ├── UserController.java
│   ├── MerchantController.java
│   ├── ServiceItemController.java
│   ├── BookingController.java
│   └── AdminController.java
├── service/                # 业务服务
│   ├── AuthService.java
│   ├── UserService.java
│   ├── MerchantService.java
│   ├── BookingService.java
│   └── SignedLinkService.java
├── repository/             # 数据访问
│   ├── UserRepository.java
│   ├── MerchantProfileRepository.java
│   ├── ServiceItemRepository.java
│   └── BookingRepository.java
├── entity/                 # 实体类
│   ├── User.java
│   ├── MerchantProfile.java
│   ├── ServiceItem.java
│   ├── AppointmentTask.java
│   ├── AppointmentSlot.java
│   └── Booking.java
├── dto/                    # 数据传输对象
│   ├── request/
│   └── response/
├── enums/                  # 枚举类型
│   ├── UserRole.java
│   ├── BookingStatus.java
│   └── ServiceCategory.java
├── exception/              # 异常处理
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   └── ErrorCode.java
├── filter/                 # 过滤器
│   └── RateLimitFilter.java
├── util/                   # 工具类
│   └── SignedLinkUtils.java
└── AppointmentSystemApplication.java
```

## 3. 前端架构

### 3.1 技术栈

```
┌─────────────────────────────────────────────────────────────┐
│                     Vue 3 + TypeScript                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Vite (构建工具)                      │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Pinia       │  │ Vue Router  │  │  Element Plus       │  │
│  │ (状态管理)   │  │ (路由)       │  │  (UI 组件库)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Axios       │  │ Day.js      │  │  VueUse             │  │
│  │ (HTTP)      │  │ (日期)       │  │  (工具函数)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 前端模块

| 模块 | 功能 | 页面 |
|------|------|------|
| **Auth** | 认证 | 登录、注册、找回密码 |
| **Merchant** | 商户管理 | 商户信息、服务项管理 |
| **Appointment** | 预约管理 | 任务创建、时段管理、预约列表 |
| **User** | 用户端 | 预约页面、我的预约 |
| **Admin** | 后台管理 | 统计看板、用户管理 |

## 4. 高并发处理策略

### 4.1 API 网关层

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  Rate Limiting (基于 Redis + Lua 脚本)                   ││
│  │  - 滑动时间窗限流                                        ││
│  │  - 返回 429 + X-RateLimit-* 响应头                       ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │  Request Routing                                         ││
│  │  - 路径匹配 /api/**                                      ││
│  │  - 负载均衡                                              ││
│  │  - 熔断降级                                              ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │  Security                                                ││
│  │  - CORS 处理                                             ││
│  │  - Session 验证                                          ││
│  │  - 请求日志                                              ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.2 预约并发控制

#### 方案一：乐观锁（推荐）
```java
@Version
private Integer version;

// 预约时自动检查版本号，冲突抛出 OptimisticLockingFailureException
```

#### 方案二：消息队列（高并发场景）
```
用户请求 → API Gateway → RabbitMQ 队列 → 消费者处理 → 结果通知
```

**RabbitMQ 预约流程：**
1. 用户提交预约请求
2. Gateway 将请求发送到 `booking.request` 队列
3. 消费者从队列获取请求，执行预约逻辑
4. 结果发送到 `booking.result` 队列
5. 前端通过轮询或 WebSocket 获取结果

### 4.3 缓存策略

| 数据类型 | 缓存时长 | 缓存策略 |
|----------|----------|----------|
| 商户信息 | 5 分钟 | Cache-Aside |
| 服务项列表 | 30 秒 | Cache-Aside |
| 可用时段 | 不缓存 | 实时查询 |
| 用户 Session | 4 小时 | Redis Session |

### 4.4 数据库优化

- **索引优化**：为高频查询字段添加索引
- **连接池**：HikariCP，最大连接数 20
- **读写分离**：未来可扩展主从架构

## 5. 安全架构

### 5.1 认证流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  用户    │────▶│  Gateway │────▶│  Redis   │────▶│  Backend │
│          │     │  (验证)   │     │ (Session)│     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
      │                │                                 │
      │   登录请求      │                                 │
      │ ──────────────▶│                                 │
      │                │    创建 Session                  │
      │                │ ───────────────────────────────▶│
      │                │                                 │
      │   Set-Cookie   │    返回 Session ID              │
      │ ◀──────────────│◀───────────────────────────────│
```

### 5.2 角色权限

| 角色 | 权限 |
|------|------|
| **USER** | 查看可用预约、创建预约、查看自己的预约 |
| **MERCHANT** | 管理商户信息、管理服务项、创建预约任务、查看预约 |
| **ADMIN** | 全部权限 + 用户管理 + 系统统计 |

### 5.3 签名链接

用于外部访问（如分享预约链接）：

```
签名链接格式:
/book/{taskId}?token={signature}&exp={expiry}

签名算法:
signature = HMAC-SHA256(secret, taskId + expiry)
```

## 6. 可观测性

### 6.1 监控指标

| 指标类型 | 指标名称 | 说明 |
|----------|----------|------|
| 请求量 | `http_requests_total` | 总请求数 |
| 错误率 | `http_errors_total` | 错误请求数 |
| 响应时间 | `http_request_duration_seconds` | 请求耗时分布 |
| 限流 | `rate_limit_hits_total` | 限流触发次数 |
| 预约 | `booking_total`, `booking_success_total` | 预约相关指标 |

### 6.2 日志规范

```json
{
  "timestamp": "2024-01-15T10:30:00.000+08:00",
  "level": "INFO",
  "traceId": "abc123",
  "spanId": "def456",
  "service": "appointment-service",
  "message": "Booking created successfully",
  "context": {
    "userId": "user001",
    "bookingId": "booking001"
  }
}
```

## 7. 扩展性设计

### 7.1 水平扩展

- **无状态设计**：Session 存储在 Redis，支持多实例部署
- **数据库分片**：未来可按商户 ID 进行分片
- **消息队列**：RabbitMQ 支持集群模式

### 7.2 API 版本化

```
/api/v1/bookings    # 当前版本
/api/v2/bookings    # 未来版本（通过 Gateway 路由）
```

### 7.3 微服务演进路径

```
当前: 单体应用
  ↓
阶段1: 拆分 Gateway + 核心服务
  ↓
阶段2: 拆分 用户服务、预约服务、统计服务
  ↓
阶段3: 完整微服务架构（服务注册、配置中心、链路追踪）
```

## 8. 压测与容量规划

### 8.1 压测场景

| 场景 | 并发数 | 目标 TPS | P99 响应时间 |
|------|--------|----------|--------------|
| 用户注册 | 100 | 50 | < 500ms |
| 用户登录 | 200 | 100 | < 200ms |
| 查询任务 | 500 | 300 | < 100ms |
| 创建预约 | 300 | 100 | < 300ms |

### 8.2 压测工具

- **JMeter**：场景压测
- **Gatling**：高并发压测
- **wrk**：快速基准测试

### 8.3 关键指标

- P95/P99 响应时间
- 错误率（目标 < 0.1%）
- 限流命中率
- 数据库连接池使用率
- Redis 内存使用率
