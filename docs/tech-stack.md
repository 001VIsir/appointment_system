# 技术栈文档

## 1. 技术栈总览

本项目采用企业级主流技术栈，前后端分离架构。

```
┌─────────────────────────────────────────────────────────────────┐
│                         技术栈总览                               │
├─────────────────────────────────────────────────────────────────┤
│  前端    │  Vue 3 + TypeScript + Element Plus + Vite            │
│  网关    │  Spring Cloud Gateway 4.x                            │
│  后端    │  Spring Boot 4.0.2 + Java 21                         │
│  ORM     │  Spring Data JPA + Hibernate                         │
│  安全    │  Spring Security 6.x                                 │
│  缓存    │  Redis 7.x                                           │
│  消息    │  RabbitMQ 3.12                                       │
│  数据库  │  MySQL 8.0                                            │
│  迁移    │  Flyway 10.x                                         │
│  文档    │  Springdoc OpenAPI 2.x                               │
│  监控    │  Micrometer + Prometheus                             │
│  容器    │  - (本地开发不使用容器)                             │
└─────────────────────────────────────────────────────────────────┘
```

## 2. 后端技术栈

### 2.1 核心框架

| 组件 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Spring Boot** | 4.0.2 | 基础框架 | 主流企业级 Java 框架 |
| **Java** | 21 LTS | 运行环境 | 长期支持版本，支持虚拟线程 |
| **Maven** | 3.9+ | 构建工具 | 依赖管理和构建 |

### 2.2 Web 层

| 组件 | 版本 | 用途 |
|------|------|------|
| **Spring Web MVC** | - | REST API |
| **Spring Cloud Gateway** | 4.x | API 网关 |
| **Jakarta Validation** | 3.x | 参数校验 |
| **Springdoc OpenAPI** | 2.x | API 文档 |

### 2.3 数据访问层

| 组件 | 版本 | 用途 |
|------|------|------|
| **Spring Data JPA** | - | ORM 框架 |
| **Hibernate** | 6.x | JPA 实现 |
| **Flyway** | 10.x | 数据库迁移 |
| **HikariCP** | - | 数据库连接池 |
| **MySQL Connector** | 8.x | MySQL 驱动 |

### 2.4 安全框架

| 组件 | 版本 | 用途 |
|------|------|------|
| **Spring Security** | 6.x | 认证授权 |
| **Spring Session** | - | 分布式 Session |
| **BCrypt** | - | 密码加密 |

### 2.5 缓存与消息

| 组件 | 版本 | 用途 |
|------|------|------|
| **Spring Data Redis** | - | Redis 集成 |
| **Lettuce** | - | Redis 客户端 |
| **Spring AMQP** | - | RabbitMQ 集成 |

### 2.6 监控与运维

| 组件 | 版本 | 用途 |
|------|------|------|
| **Spring Actuator** | - | 健康检查 |
| **Micrometer** | - | 指标采集 |
| **Prometheus** | - | 指标存储 |

### 2.7 配置中心

| 组件 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Nacos** | 2.x | 配置中心 | 集中管理配置，支持动态刷新 |
| **Spring Cloud Alibaba** | 2023.0.x | Nacos 集成 | 提供配置中心和注册中心支持 |

**Nacos 使用场景：**
- 集中管理应用配置（数据库、Redis、限流等）
- 多环境配置隔离（dev/test/prod）
- 运行时配置动态刷新
- 为微服务架构做准备

### 2.8 工具库

| 组件 | 版本 | 用途 |
|------|------|------|
| **Lombok** | 1.18.x | 代码简化 |
| **Jackson** | - | JSON 序列化 |
| **SLF4J + Logback** | - | 日志框架 |

### 2.8 后端依赖清单 (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>

    <!-- Session -->
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>

    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- Monitoring -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Nacos Config (配置中心) -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- Nacos Discovery (服务注册与发现) -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Spring Cloud Bootstrap -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-bootstrap</artifactId>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 3. 前端技术栈

### 3.1 核心框架

| 组件 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Vue** | 3.4+ | 前端框架 | Composition API |
| **TypeScript** | 5.x | 类型系统 | 类型安全 |
| **Vite** | 5.x | 构建工具 | 快速开发体验 |

### 3.2 UI 与组件

| 组件 | 版本 | 用途 |
|------|------|------|
| **Element Plus** | 2.x | UI 组件库 |
| **@element-plus/icons-vue** | - | 图标库 |

### 3.3 状态与路由

| 组件 | 版本 | 用途 |
|------|------|------|
| **Pinia** | 2.x | 状态管理 |
| **Vue Router** | 4.x | 路由管理 |

### 3.4 网络与工具

| 组件 | 版本 | 用途 |
|------|------|------|
| **Axios** | 1.x | HTTP 客户端 |
| **Day.js** | 1.x | 日期处理 |
| **VueUse** | 10.x | 工具函数 |

### 3.5 前端依赖清单 (package.json)

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.4.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.6.0",
    "dayjs": "^1.11.0",
    "@vueuse/core": "^10.7.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "vue-tsc": "^1.8.0",
    "@types/node": "^20.10.0",
    "sass": "^1.69.0",
    "eslint": "^8.55.0",
    "@typescript-eslint/eslint-plugin": "^6.13.0",
    "@typescript-eslint/parser": "^6.13.0",
    "eslint-plugin-vue": "^9.19.0",
    "prettier": "^3.1.0"
  }
}
```

## 4. 基础设施

### 4.1 数据库

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| **MySQL** | 8.0 | 主数据存储 | 3306 |

**MySQL 配置要点：**
- 字符集：utf8mb4
- 排序规则：utf8mb4_unicode_ci
- 存储引擎：InnoDB
- 事务隔离：READ COMMITTED

### 4.2 缓存

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| **Redis** | 7.x | Session/缓存/限流 | 6379 |

**Redis 使用场景：**
- Spring Session 存储
- 接口缓存
- 限流计数器
- 分布式锁

### 4.3 消息队列

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| **RabbitMQ** | 3.12 | 异步消息处理 | 5672 (AMQP) / 15672 (管理) |

**RabbitMQ 使用场景：**
- 预约请求队列
- 异步通知
- 削峰填谷

### 4.4 配置中心

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| **Nacos** | 2.x | 配置中心 + 服务注册 | 8848 |

**Nacos 使用场景：**
- 集中管理应用配置
- 多环境配置隔离（dev/test/prod）
- 配置动态刷新（无需重启）
- 服务注册与发现（可选）

### 4.5 容器化

> 注意：本项目不使用 Docker 容器化，使用本地开发模式。

## 5. 开发工具

### 5.1 IDE 推荐

| 工具 | 用途 |
|------|------|
| **IntelliJ IDEA** | 后端开发 |
| **VS Code** | 前端开发 |

### 5.2 VS Code 插件

```
- Vue - Official (Vue.volar)
- TypeScript Vue Plugin (Vue.vscode-typescript-vue-plugin)
- ESLint (dbaeumer.vscode-eslint)
- Prettier (esbenp.prettier-vscode)
- Element Plus Snippets (aliariff.vscode-element-plus-snippets)
```

### 5.3 IntelliJ 插件

```
- Lombok
- Spring Boot Helper
- MyBatisX (可选)
```

## 6. 版本兼容性矩阵

| 组件 | 最低版本 | 推荐版本 | 最高测试版本 |
|------|----------|----------|--------------|
| Java | 21 | 21 | 21 |
| Spring Boot | 4.0.0 | 4.0.2 | 4.0.x |
| Spring Cloud | 2023.0.x | 2023.0.3 | 2023.0.x |
| Spring Cloud Alibaba | 2023.0.x | 2023.0.1.2 | 2023.0.x |
| Nacos | 2.x | 2.3.0 | 2.x |
| MySQL | 8.0.30 | 8.0.35 | 8.0.x |
| Redis | 7.0 | 7.2 | 7.x |
| RabbitMQ | 3.11 | 3.12 | 3.12.x |
| Node.js | 18 | 20 LTS | 21 |
| Vue | 3.3 | 3.4 | 3.4.x |
| Docker | - | - | - (不使用) |

## 7. 技术选型理由

### 7.1 后端选型

| 选择 | 替代方案 | 选择理由 |
|------|----------|----------|
| Spring Boot | Quarkus, Micronaut | 生态成熟，企业主流 |
| Spring Data JPA | MyBatis, JOOQ | 减少样板代码，对象映射 |
| Redis | Memcached, Ehcache | 支持持久化，数据结构丰富 |
| RabbitMQ | Kafka, RocketMQ | 功能完善，管理界面友好 |
| Flyway | Liquibase | 配置简单，SQL 优先 |

### 7.2 前端选型

| 选择 | 替代方案 | 选择理由 |
|------|----------|----------|
| Vue 3 | React, Angular | 学习曲线平缓，性能优秀 |
| Element Plus | Ant Design Vue, Naive UI | 组件丰富，文档完善 |
| Pinia | Vuex | 更好的 TypeScript 支持 |
| Vite | Webpack | 开发体验更好，构建更快 |
| TypeScript | JavaScript | 类型安全，重构友好 |

## 8. 未来技术演进

### 8.1 短期计划

- [x] 接入 Nacos 配置中心 (2026-02-18)
- [ ] 接入 ELK 日志系统
- [ ] 配置 Grafana 监控面板
- [ ] 添加分布式追踪 (Sleuth/Zipkin)

### 8.2 中期计划

- [ ] 微服务拆分（使用 Nacos 注册中心）
- [ ] 数据库读写分离
- [ ] 接入 Kubernetes

### 8.3 长期计划

- [ ] 服务网格 (Istio)
- [ ] 多云部署
- [ ] A/B 测试平台
