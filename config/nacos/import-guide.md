# Nacos 配置导入指南

> 本目录包含各环境的 Nacos 配置文件，可直接导入使用

## 文件说明

| 文件名 | 环境 | 说明 |
|--------|------|------|
| `appointment_system-dev.properties` | 开发环境 | 开发调试使用 |
| `appointment_system-test.properties` | 测试环境 | 测试环境使用 |
| `appointment_system-prod.properties` | 生产环境 | 生产环境使用 |

## 导入步骤

### 方式一：Nacos 控制台导入

1. 启动 Nacos 服务器
   ```bash
   # 使用 Docker 快速启动
   docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
     -e MODE=standalone \
     -e SPRING_DATASOURCE_PLATFORM=mysql \
     -e MYSQL_SERVICE_HOST=mysql \
     -e MYSQL_SERVICE_PORT=3306 \
     -e MYSQL_SERVICE_DB_NAME=nacos_config \
     -e MYSQL_SERVICE_USER=nacos \
     -e MYSQL_SERVICE_PASSWORD=nacos \
     nacos/nacos-server:v2.3.0
   ```

2. 访问 Nacos 控制台：http://localhost:8848/nacos
   - 用户名：`nacos`
   - 密码：`nacos`

3. 进入「配置管理」->「配置列表」

4. 点击「导入配置」按钮

5. 选择对应的环境文件（properties 格式）

6. 设置配置格式为 `Properties`

7. 点击「上传」完成导入

### 方式二：Nacos API 导入

```bash
# 导入配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=appointment_system.properties" \
  -d "group=DEFAULT_GROUP" \
  -d "type=properties" \
  -d "content=$(cat appointment_system-dev.properties | python3 -c "import sys,urllib.parse;print(urllib.parse.quote(sys.stdin.read()))")"
```

### 方式三：Nacos CLI 导入

```bash
# 安装 nacos-cli
npm install -g nacos-cli

# 导入配置
nacos config import \
  --server-address localhost:8848 \
  --namespace dev \
  --username nacos \
  --password nacos \
  --file appointment_system-dev.properties
```

## 命名空间设置

建议在 Nacos 中创建以下命名空间：

| 命名空间 ID | 命名空间名称 | 用途 |
|-------------|--------------|------|
| `dev` | 开发环境 | 开发调试 |
| `test` | 测试环境 | 测试验证 |
| `prod` | 生产环境 | 正式生产 |

### 创建命名空间

1. 进入 Nacos 控制台
2. 点击「命名空间」->「创建命名空间」
3. 填写命名空间信息并保存

## 配置分组

默认使用 `DEFAULT_GROUP` 分组。

如需自定义分组，可在 bootstrap.properties 中修改：

```properties
spring.cloud.nacos.config.group=YOUR_GROUP_NAME
```

## 环境切换

### 方式一：修改 bootstrap.properties

```properties
spring.cloud.nacos.config.namespace=dev
```

### 方式二：通过启动参数

```bash
java -jar appointment_system.jar \
  --spring.cloud.nacos.config.namespace=prod \
  --spring.profiles.active=prod
```

### 方式三：通过环境变量

```bash
export NACOS_NAMESPACE=prod
java -jar appointment_system.jar
```

## 验证配置

### 检查配置是否加载

1. 启动应用，查看日志
2. 应该看到类似以下日志：
   ```
   Nacos Config: dataId=appointment_system, group=DEFAULT_GROUP, config={...}
   Nacos Config Center: endpoint: 127.0.0.1:8848
   ```

### 检查配置刷新

1. 在 Nacos 控制台修改配置
2. 查看应用日志，应该看到：
   ```
   Refresh Nacos config: dataId=appointment_system
   Nacos config changed: {...}
   ```

## 常见问题

### Q1: Nacos 启动失败？

**A**: 检查端口是否被占用，Nacos 默认使用 8848 端口。

### Q2: 配置导入后应用无法启动？

**A**: 检查数据库、Redis 等连接配置是否正确，确保依赖服务已启动。

### Q3: 如何实现配置加密？

**A**: Nacos 支持配置加密插件，可以安装 `nacos-config-crypt` 插件实现配置加密。

### Q4: 本地开发和 Nacos 配置冲突？

**A**: 设置 `spring.cloud.nacos.config.enabled=false` 使用本地配置，或设置 `NACOS_CONFIG_ENABLED=false`。

---

## 扩展阅读

- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba Nacos Config](https://spring-cloud-alibaba-group.github.io/github-pages/2024.0.0.0/zh-cn碑陶Spring%20Cloud%20Alibaba%20Nacos%20Config.html)
- [Nacos 配置管理最佳实践](https://nacos.io/zh-cn/blog/config-best-practice.html)
