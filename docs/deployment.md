# 部署指南

## 1. 环境要求

### 1.1 开发环境

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | Java 开发环境 |
| Node.js | 18+ | 前端构建 |
| Docker | 24+ | 容器化 |
| Docker Compose | 2.x | 本地编排 |
| Maven | 3.9+ | Java 构建工具 |

### 1.2 生产环境

| 组件 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0 | 主数据库 |
| Redis | 7.x | 缓存/Session |
| RabbitMQ | 3.12 | 消息队列 |

## 2. 快速开始（Docker Compose）

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
vim .env
```

### 2.3 启动所有服务

```bash
# 启动所有服务（MySQL + Redis + RabbitMQ + Backend + Frontend）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 2.4 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | http://localhost:8080 | Spring Boot |
| API 文档 | http://localhost:8080/swagger-ui.html | Swagger UI |
| 前端应用 | http://localhost:5173 | Vue 3 开发服务器 |
| RabbitMQ 管理 | http://localhost:15672 | 用户名/密码: guest/guest |

## 3. 本地开发（不使用 Docker）

### 3.1 启动基础设施

```bash
# 仅启动 MySQL + Redis + RabbitMQ
docker-compose up -d mysql redis rabbitmq
```

### 3.2 配置数据库

```bash
# 连接 MySQL
docker exec -it appointment-mysql mysql -uroot -proot123

# 创建数据库（如果不存在）
CREATE DATABASE yuyue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3.3 配置后端

编辑 `src/main/resources/application.properties`：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/yuyue
spring.datasource.username=root
spring.datasource.password=root123

# Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379

# RabbitMQ 配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# 应用配置
app.cors.allowed-origins=http://localhost:5173
app.rate-limit.max-requests=100
app.rate-limit.window-seconds=60
app.signed-link.secret=your-secret-key-here
```

### 3.4 运行后端

```bash
# 使用 Maven 运行
./mvnw spring-boot:run

# 或编译后运行
./mvnw clean package -DskipTests
java -jar target/appointment_system-0.0.1-SNAPSHOT.jar
```

### 3.5 运行前端

```bash
cd frontend
npm install
npm run dev
```

## 4. 生产部署

### 4.1 构建镜像

```bash
# 构建后端镜像
docker build -t appointment-backend:latest .

# 构建前端镜像
cd frontend
docker build -t appointment-frontend:latest .
```

### 4.2 环境变量配置

生产环境必须配置的环境变量：

```bash
# 数据库
DB_HOST=mysql.production
DB_PORT=3306
DB_NAME=yuyue
DB_USERNAME=app_user
DB_PASSWORD=<secure-password>

# Redis
REDIS_HOST=redis.production
REDIS_PORT=6379
REDIS_PASSWORD=<secure-password>

# RabbitMQ
RABBITMQ_HOST=rabbitmq.production
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=app_user
RABBITMQ_PASSWORD=<secure-password>

# 安全配置
APP_SIGNED_LINK_SECRET=<random-256-bit-secret>
SESSION_SECRET=<random-secret>

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

### 4.3 Docker Compose 生产配置

创建 `docker-compose.prod.yml`：

```yaml
version: '3.8'

services:
  backend:
    image: appointment-backend:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_DATA_REDIS_HOST=${REDIS_HOST}
      - SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
    depends_on:
      - mysql
      - redis
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    image: appointment-frontend:latest
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: always
```

### 4.4 Kubernetes 部署（可选）

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: appointment-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: appointment-backend
  template:
    metadata:
      labels:
        app: appointment-backend
    spec:
      containers:
      - name: backend
        image: appointment-backend:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: appointment-config
        - secretRef:
            name: appointment-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

## 5. 数据库迁移

### 5.1 Flyway 配置

项目使用 Flyway 进行数据库版本管理：

```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 5.2 迁移文件命名规范

```
db/migration/
├── V1__Init_schema.sql           # 初始化表结构
├── V2__Add_user_indexes.sql      # 添加索引
├── V3__Add_merchant_settings.sql # 新增字段
└── ...
```

### 5.3 执行迁移

```bash
# 应用启动时自动执行迁移
./mvnw spring-boot:run

# 或手动执行
./mvnw flyway:migrate
```

## 6. 监控与日志

### 6.1 健康检查

```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

### 6.2 日志配置

```yaml
# logback-spring.xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### 6.3 Prometheus + Grafana

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'appointment-system'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

## 7. 故障排查

### 7.1 常见问题

**数据库连接失败**
```bash
# 检查 MySQL 状态
docker-compose ps mysql
docker-compose logs mysql

# 测试连接
docker exec -it appointment-mysql mysql -uroot -proot123
```

**Redis 连接失败**
```bash
# 检查 Redis 状态
docker-compose ps redis
docker-compose logs redis

# 测试连接
docker exec -it appointment-redis redis-cli ping
```

**RabbitMQ 连接失败**
```bash
# 检查 RabbitMQ 状态
docker-compose ps rabbitmq
docker-compose logs rabbitmq

# 访问管理界面
open http://localhost:15672
```

### 7.2 日志查看

```bash
# 查看后端日志
docker-compose logs -f backend

# 查看所有服务日志
docker-compose logs -f

# 查看最近 100 行日志
docker-compose logs --tail=100 backend
```

## 8. 备份与恢复

### 8.1 数据库备份

```bash
# 手动备份
docker exec appointment-mysql mysqldump -uroot -proot123 yuyue > backup_$(date +%Y%m%d).sql

# 定时备份（crontab）
0 2 * * * docker exec appointment-mysql mysqldump -uroot -proot123 yuyue > /backup/yuyue_$(date +\%Y\%m\%d).sql
```

### 8.2 数据库恢复

```bash
# 恢复备份
cat backup_20240115.sql | docker exec -i appointment-mysql mysql -uroot -proot123 yuyue
```

## 9. 安全配置

### 9.1 生产环境检查清单

- [ ] 更改默认数据库密码
- [ ] 配置 Redis 密码
- [ ] 配置 RabbitMQ 用户和权限
- [ ] 设置 `APP_SIGNED_LINK_SECRET` 为强随机值
- [ ] 配置 HTTPS（使用 Nginx 反向代理）
- [ ] 配置防火墙规则
- [ ] 启用数据库 SSL 连接
- [ ] 配置日志脱敏

### 9.2 Nginx 反向代理

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://frontend:80;
        proxy_set_header Host $host;
    }
}
```

## 10. 性能调优

### 10.1 JVM 参数

```bash
java -Xms512m -Xmx1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar appointment_system.jar
```

### 10.2 数据库连接池

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

### 10.3 Redis 连接池

```properties
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
```
