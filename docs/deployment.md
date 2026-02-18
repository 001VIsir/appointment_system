# 部署指南

> 本文档适用于本地开发环境（Windows）

## 1. 环境要求

### 1.1 开发环境

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | Java 开发环境 |
| Node.js | 18+ | 前端构建 |
| Maven | 3.9+ | Java 构建工具 |
| MySQL | 8.0 | 主数据库（本地安装） |
| Redis | 7.x | 缓存/Session（本地安装） |
| RabbitMQ | 3.12 | 消息队列（本地安装） |

### 1.2 本地服务安装

#### MySQL 8.0 安装
- 下载地址：https://dev.mysql.com/downloads/mysql/
- 端口：3306
- 创建数据库：`CREATE DATABASE appointment_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`

#### Redis 7.x 安装
- 下载地址：https://redis.io/download/
- 端口：6379
- 或使用 Windows 兼容版本：https://github.com/tporadowski/redis/releases

#### RabbitMQ 3.12 安装
- 下载地址：https://www.rabbitmq.com/download.html
- 端口：5672（AMQP）/ 15672（管理界面）
- 需要安装 Erlang：https://www.erlang.org/downloads

## 2. 快速开始

### 2.1 克隆项目

```bash
git clone <repository-url>
cd appointment_system
```

### 2.2 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
notepad .env
```

确保 `.env` 文件中的数据库、Redis、RabbitMQ 配置与本地安装一致。

### 2.3 启动后端

```bash
# 使用 Maven 启动
./mvnw spring-boot:run
```

后端启动后会：
1. 连接 MySQL 运行 Flyway 迁移
2. 连接 Redis 存储 Session
3. 连接 RabbitMQ 监听消息

### 2.4 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 2.5 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | http://localhost:8080 | Spring Boot |
| API 文档 | http://localhost:8080/swagger-ui.html | Swagger UI |
| 前端应用 | http://localhost:5173 | Vue 3 开发服务器 |
| RabbitMQ 管理 | http://localhost:15672 | 用户名/密码: guest/guest |

## 3. 项目结构

```
appointment_system/
├── src/                    # 后端源码
│   ├── main/java/         # Java 源代码
│   └── main/resources/    # 配置文件
│       └── db/migration/  # Flyway 数据库迁移
├── frontend/              # 前端源码
├── docs/                  # 项目文档
├── init-db/               # 数据库初始化脚本
└── logs/                  # 日志目录
```

## 4. 常用命令

### 后端

```bash
# 编译项目
./mvnw clean compile

# 运行测试
./mvnw test

# 打包
./mvnw clean package -DskipTests

# 运行打包后的 JAR
java -jar target/appointment_system-0.0.1-SNAPSHOT.jar
```

### 前端

```bash
# 安装依赖
cd frontend
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 5. 配置说明

### 数据库配置
在 `.env` 或 `application.properties` 中配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/appointment_system
spring.datasource.username=root
spring.datasource.password=your_password
```

### Redis 配置

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### RabbitMQ 配置

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

## 6. 故障排查

### 数据库连接失败
- 检查 MySQL 服务是否启动
- 验证端口 3306 是否被占用
- 确认用户名密码正确

### Redis 连接失败
- 检查 Redis 服务是否启动
- 验证端口 6379 是否可访问

### RabbitMQ 连接失败
- 检查 RabbitMQ 服务是否启动
- 确认 Erlang 环境变量配置正确

### 端口被占用
- Windows 使用 `netstat -ano` 查看端口占用
- 修改 `application.properties` 中的 server.port
