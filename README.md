# 预约系统 (Appointment System)

基于 Spring Boot + Vue 3 的商户预约管理平台

## 项目简介

这是一个面向 ToC 的预约服务平台，支持：
- 商户创建和管理预约任务
- 用户通过签名链接进行预约
- 管理员进行平台统计和监管

## 技术栈

### 后端
- Spring Boot 3.2.x + Java 17
- Spring Data JPA + Hibernate
- Spring Security 6.x
- Redis (Session + 缓存 + 限流 + 统计)
- MySQL 8.0
- Flyway (数据库迁移)
- Spring Boot Actuator (健康检查)
- OpenAPI (API 文档)

### 前端
- Vue 3 + TypeScript
- Element Plus
- Pinia (状态管理)
- Vite (构建工具)

## 快速开始

### 前置要求

- JDK 17+
- Node.js 18+
- Maven 3.9+
- MySQL 8.0（本地安装）
- Redis 7.x（本地安装）

### 启动项目

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，配置数据库、Redis 连接信息

# 2. 启动后端
./mvnw spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

### 访问地址

- 前端应用：http://localhost:5173
- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/swagger-ui.html

## 项目结构

```
appointment_system/
├── src/                    # 后端源码
│   ├── main/java/         # Java 源代码
│   └── main/resources/    # 配置文件
├── frontend/              # 前端源码
├── docs/                  # 项目文档
├── tests/                 # 测试脚本
└── init-db/               # 数据库初始化脚本
```

## 文档

- [系统架构](docs/architecture.md)
- [技术栈](docs/tech-stack.md)
- [数据库设计](docs/database-design.md)
- [部署指南](docs/deployment.md)
- [用户指南](docs/user-guide.md)
- [面试文档](docs/interview-notes.md)

## License

MIT
