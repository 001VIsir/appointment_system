# Windows + WSL2 Docker 部署快速指南

## 🚀 快速部署（5分钟）

### 1. 配置环境变量

**在 Windows PowerShell 或 Git Bash 中：**

```bash
# 进入项目目录
cd C:\Users\VISIR\IdeaProjects\appointment_system

# 复制环境变量文件
copy .env.docker .env

# 用记事本或 VS Code 编辑 .env 文件
notepad .env
# 或
code .env
```

**⚠️ 必须修改以下密码：**
- `DB_ROOT_PASSWORD`
- `DB_PASSWORD`
- `REDIS_PASSWORD`
- `RABBITMQ_PASSWORD`
- `SIGNED_LINK_SECRET`

### 2. 启动服务

**打开 WSL2 终端：**

```bash
# 方式1：从 Windows 进入 WSL2
wsl

# 导航到项目目录
cd /mnt/c/Users/VISIR/IdeaProjects/appointment_system

# 验证 .env 文件存在
cat .env
```

**启动开发环境：**

```bash
# 构建并启动所有服务（首次运行较慢，需要下载镜像）
docker-compose up -d

# 查看启动日志
docker-compose logs -f

# 检查服务状态
docker-compose ps
```

**或启动生产环境：**

```bash
docker-compose -f docker-compose.prod.yml up -d
docker-compose -f docker-compose.prod.yml logs -f
```

### 3. 验证部署

等待所有服务启动完成（约 2-3 分钟），然后访问：

- **前端（开发模式）**: http://localhost:5173
- **前端（生产模式）**: http://localhost
- **后端 API**: http://localhost:8080
- **Swagger 文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health
- **RabbitMQ 管理**: http://localhost:15672 (用户名/密码见 .env)

### 4. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# 查看最近 100 行日志
docker-compose logs --tail=100 backend
```

### 5. 常用命令

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（清空所有数据）
docker-compose down -v

# 重启服务
docker-compose restart

# 重新构建镜像
docker-compose build

# 进入容器
docker-compose exec backend sh
docker-compose exec mysql bash

# 查看容器资源使用
docker stats
```

## 🔧 故障排除

### 问题1：端口被占用

**错误信息：**
```
Error: bind: address already in use
```

**解决方案：**
```bash
# Windows PowerShell 查看端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3306
netstat -ano | findstr :5173

# 停止占用端口的程序，或修改 .env 中的端口配置
```

### 问题2：服务启动失败

**检查步骤：**
```bash
# 1. 查看服务状态
docker-compose ps

# 2. 查看失败服务的日志
docker-compose logs backend

# 3. 检查健康状态
docker inspect appointment-backend | grep -A 10 "Health"

# 4. 重启单个服务
docker-compose restart backend
```

### 问题3：数据库连接失败

**等待 MySQL 完全启动（约 30-60 秒）：**

```bash
# 查看 MySQL 日志
docker-compose logs mysql

# 检查 MySQL 是否就绪
docker-compose exec mysql mysql -u root -p${DB_ROOT_PASSWORD} -e "SELECT 1"
```

### 问题4：前端无法访问后端

**检查 CORS 配置：**

```bash
# 确保 .env 中的 CORS_ALLOWED_ORIGINS 包含前端地址
cat .env | grep CORS

# 如果前端运行在 localhost:5173，应该配置：
# CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### 问题5：Docker Desktop 内存不足

**解决方案：**
1. 打开 Docker Desktop
2. 进入 Settings → Resources
3. 增加内存到至少 4GB
4. 点击 "Apply & Restart"

## 📊 性能监控

### 查看容器资源使用

```bash
# 实时监控
docker stats

# 查看特定容器
docker stats appointment-backend appointment-mysql
```

### 查看应用指标

```bash
# Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# 健康检查
curl http://localhost:8080/actuator/health | jq
```

## 🔄 更新部署

### 代码更新后重新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 或分步执行
docker-compose build backend
docker-compose up -d backend
```

### 数据库迁移

Flyway 会自动执行数据库迁移，查看迁移状态：

```bash
# 进入后端容器
docker-compose exec backend sh

# 查看日志
tail -f logs/appointment_system.log
```

## 💾 数据备份

### 备份 MySQL 数据库

```bash
# 在 WSL2 中执行
docker-compose exec mysql mysqldump -u root -p${DB_ROOT_PASSWORD} yuyue > backup_$(date +%Y%m%d_%H%M%S).sql
```

### 恢复数据库

```bash
cat backup_20260218_123456.sql | docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} yuyue
```

## 🎯 开发模式 vs 生产模式

| 特性 | 开发模式 | 生产模式 |
|------|---------|---------|
| 启动命令 | `docker-compose up -d` | `docker-compose -f docker-compose.prod.yml up -d` |
| 前端 | Vite 开发服务器（热重载） | Nginx（静态文件） |
| 前端端口 | 5173 | 80 |
| 代码挂载 | ✅ | ❌ |
| JVM 内存 | 512M-1G | 1G-2G |
| 适合场景 | 本地开发调试 | 生产部署 |

## 🌐 访问地址汇总

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端（开发） | http://localhost:5173 | Vite 开发服务器 |
| 前端（生产） | http://localhost | Nginx |
| 后端 API | http://localhost:8080 | Spring Boot |
| Swagger UI | http://localhost:8080/swagger-ui.html | API 文档 |
| API Docs | http://localhost:8080/api-docs | OpenAPI JSON |
| 健康检查 | http://localhost:8080/actuator/health | 健康状态 |
| Prometheus | http://localhost:8080/actuator/prometheus | 指标 |
| RabbitMQ | http://localhost:15672 | 管理界面 |

## 📝 下一步

1. **测试所有功能**：访问前端，测试注册、登录、预约等功能
2. **查看日志**：确保没有错误
3. **监控性能**：使用 Docker stats 和 Prometheus 指标
4. **配置生产环境**：修改所有密码，配置 SSL 证书

## ⚠️ 安全提示

**在生产环境部署前务必：**
1. ✅ 修改 `.env` 中的所有密码
2. ✅ 使用强随机密码（至少 16 字符）
3. ✅ 配置防火墙规则
4. ✅ 启用 HTTPS
5. ✅ 定期备份数据
6. ✅ 定期更新 Docker 镜像

---

**遇到问题？**
1. 查看 `docker-compose logs` 日志
2. 检查 `.env` 配置
3. 确认端口没有被占用
4. 确保 Docker Desktop 有足够资源（至少 4GB 内存）
