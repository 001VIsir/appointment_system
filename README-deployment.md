# Docker 部署文档

本文档详细说明如何使用 Docker 和 Docker Compose 部署预约系统。

## 目录

- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署模式](#部署模式)
- [常用命令](#常用命令)
- [故障排除](#故障排除)
- [生产环境建议](#生产环境建议)

## 前置要求

- Docker Engine 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd appointment_system
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量（重要！）
vim .env
```

**必须修改的配置项：**
- `DB_ROOT_PASSWORD`: MySQL root 密码
- `DB_PASSWORD`: 应用数据库密码
- `REDIS_PASSWORD`: Redis 密码
- `RABBITMQ_PASSWORD`: RabbitMQ 密码
- `SIGNED_LINK_SECRET`: 签名链接密钥（至少32字符）

### 3. 启动服务

#### 开发环境

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

#### 生产环境

```bash
# 使用生产配置启动
docker-compose -f docker-compose.prod.yml up -d

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 4. 访问应用

- **前端**: http://localhost (生产) 或 http://localhost:5173 (开发)
- **后端 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **RabbitMQ 管理界面**: http://localhost:15672

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 | 必须修改 |
|--------|------|--------|----------|
| `DB_ROOT_PASSWORD` | MySQL root 密码 | `root123` | ✅ |
| `DB_NAME` | 数据库名称 | `yuyue` | - |
| `DB_USERNAME` | 数据库用户名 | `app_user` | - |
| `DB_PASSWORD` | 数据库密码 | `app123` | ✅ |
| `REDIS_PASSWORD` | Redis 密码 | - | ✅ |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `guest` | ✅ |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `guest` | ✅ |
| `SIGNED_LINK_SECRET` | 签名链接密钥 | - | ✅ |
| `CORS_ALLOWED_ORIGINS` | 允许的跨域源 | `http://localhost:5173` | - |
| `RATE_LIMIT_MAX_REQUESTS` | 限流最大请求数 | `100` | - |
| `RATE_LIMIT_WINDOW_SECONDS` | 限流时间窗口（秒） | `60` | - |

### 端口映射

| 服务 | 容器端口 | 主机端口 | 说明 |
|------|----------|----------|------|
| 前端 | 80 | 80/5173 | Nginx/Vite 开发服务器 |
| 后端 | 8080 | 8080 | Spring Boot |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | 消息队列 |
| RabbitMQ Management | 15672 | 15672 | 管理界面 |

## 部署模式

### 开发模式 (docker-compose.yml)

特点：
- 前端使用 Vite 开发服务器，支持热重载
- 源代码挂载到容器，实时更新
- 数据库密码可选
- 适合本地开发和测试

启动命令：
```bash
docker-compose up -d
```

### 生产模式 (docker-compose.prod.yml)

特点：
- 前端使用 Nginx 服务静态文件
- 强制要求所有密码配置
- 更严格的健康检查
- 更大的 JVM 内存配置
- 自动重启策略

启动命令：
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 常用命令

### 服务管理

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启所有服务
docker-compose restart

# 停止并删除所有容器、网络、卷
docker-compose down -v

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service_name]
```

### 单独管理服务

```bash
# 只启动数据库服务
docker-compose up -d mysql redis

# 重启后端服务
docker-compose restart backend

# 查看后端日志
docker-compose logs -f backend

# 进入后端容器
docker-compose exec backend sh

# 进入 MySQL 容器
docker-compose exec mysql bash
mysql -u root -p
```

### 数据备份

```bash
# 备份 MySQL 数据库
docker-compose exec mysql mysqldump -u root -p${DB_ROOT_PASSWORD} ${DB_NAME} > backup_$(date +%Y%m%d).sql

# 恢复数据库
cat backup_20260218.sql | docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} ${DB_NAME}
```

### 镜像管理

```bash
# 重新构建镜像
docker-compose build

# 强制重新构建（不使用缓存）
docker-compose build --no-cache

# 拉取基础镜像
docker-compose pull
```

## 故障排除

### 1. 容器启动失败

**检查日志：**
```bash
docker-compose logs backend
```

**常见原因：**
- 数据库未就绪：等待健康检查通过
- 端口冲突：检查端口是否被占用
- 内存不足：增加 Docker 内存限制

### 2. 数据库连接失败

**检查 MySQL 状态：**
```bash
docker-compose ps mysql
docker-compose logs mysql
```

**解决方案：**
- 等待 MySQL 完全启动（约 30 秒）
- 检查数据库用户名和密码配置
- 确认数据库已创建

### 3. Redis 连接失败

**检查 Redis 状态：**
```bash
docker-compose exec redis redis-cli -a ${REDIS_PASSWORD} ping
```

**解决方案：**
- 确认 Redis 密码配置正确
- 检查 Redis 日志

### 4. 前端无法访问后端 API

**检查 CORS 配置：**
```bash
# 确认 CORS_ALLOWED_ORIGINS 包含前端地址
echo $CORS_ALLOWED_ORIGINS
```

**解决方案：**
- 在 .env 文件中添加前端地址到 `CORS_ALLOWED_ORIGINS`
- 重启后端服务

### 5. 健康检查失败

**查看健康检查日志：**
```bash
docker inspect --format='{{json .State.Health}}' appointment-backend | jq
```

**解决方案：**
- 检查应用日志
- 确认端口 8080 可访问
- 增加启动等待时间

## 生产环境建议

### 安全性

1. **强密码策略**
   - 使用强随机密码（至少 16 字符）
   - 定期更换密码
   - 不要在代码仓库中提交 .env 文件

2. **网络安全**
   - 不要暴露数据库端口到公网
   - 使用防火墙限制访问
   - 配置 SSL/TLS 证书

3. **应用安全**
   - 定期更新依赖版本
   - 启用 Spring Security
   - 配置适当的 CORS 策略

### 性能优化

1. **JVM 调优**
   ```bash
   # 在 .env 中配置
   JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
   ```

2. **数据库优化**
   - 配置合适的连接池大小
   - 定期优化表和索引
   - 设置慢查询日志

3. **Redis 优化**
   - 配置最大内存限制
   - 设置合适的淘汰策略

### 监控和日志

1. **启用 Prometheus 监控**
   - 访问 http://localhost:8080/actuator/prometheus
   - 配置 Prometheus 采集

2. **日志管理**
   - 日志文件位于 `./logs` 目录
   - 配置日志轮转
   - 集成 ELK 或其他日志系统

3. **健康检查**
   - 定期检查 `/actuator/health` 端点
   - 配置告警规则

### 备份策略

1. **数据库备份**
   ```bash
   # 每日备份脚本
   0 2 * * * docker-compose exec -T mysql mysqldump -u root -p${DB_ROOT_PASSWORD} ${DB_NAME} | gzip > /backup/db_$(date +\%Y\%m\%d).sql.gz
   ```

2. **Redis 备份**
   - Redis 自动生成 RDB 快照
   - 数据持久化到 `redis_data` 卷

3. **配置备份**
   - 定期备份 .env 文件
   - 备份 docker-compose.yml

### 高可用部署

1. **使用 Docker Swarm 或 Kubernetes**
2. **配置数据库主从复制**
3. **Redis 集群模式**
4. **RabbitMQ 集群模式**
5. **负载均衡配置**

## 附录

### Docker Compose 文件对比

| 特性 | docker-compose.yml | docker-compose.prod.yml |
|------|-------------------|------------------------|
| 前端模式 | Vite 开发服务器 | Nginx 生产服务器 |
| 代码挂载 | ✅ | ❌ |
| 端口暴露 | 所有服务 | 仅前端 80 |
| 重启策略 | unless-stopped | always |
| 密码要求 | 可选 | 必须 |
| JVM 内存 | 512M-1G | 1G-2G |

### 有用的链接

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/gs/spring-boot-docker/)
- [Vue.js Docker 部署](https://vuejs.org/guide/scaling-up/ssr.html)

## 支持

如有问题，请查看：
1. 项目文档：`docs/` 目录
2. API 文档：http://localhost:8080/swagger-ui.html
3. Issue Tracker：[GitHub Issues]
