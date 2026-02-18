# Nacos 配置中心使用指南

> 预约系统 Nacos 配置中心集成文档

---

## 1. 概述

本文档介绍如何在预约系统中使用 Nacos 配置中心，实现配置的集中管理和动态刷新。

### 什么是 Nacos？

Nacos 是阿里巴巴开源的一个更易于构建云原生应用的动态服务发现、配置管理和服务管理平台。

- **配置管理**：集中管理应用配置，支持多环境、多版本
- **动态刷新**：配置变更实时推送到应用，无需重启
- **服务发现**：支持服务注册与发现（可选）

---

## 2. 快速开始

### 2.1 启动 Nacos 服务器

#### 使用 Docker（推荐）

```bash
# 启动单机版 Nacos
docker run -d --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.0
```

#### 使用嵌入式 Nacos（开发测试）

```bash
# 下载 Nacos 服务器
wget https://github.com/alibaba/nacos/releases/download/v2.3.0/nacos-server.jar

# 启动 Nacos
java -jar nacos-server.jar --spring.profiles.active=standalone
```

### 2.2 访问 Nacos 控制台

- 地址：http://localhost:8848/nacos
- 用户名：`nacos`
- 密码：`nacos`

### 2.3 导入配置

参考 `config/nacos/import-guide.md` 文件，将对应环境的配置文件导入 Nacos。

### 2.4 启动应用

```bash
# 方式一：启用 Nacos 配置中心
export NACOS_CONFIG_ENABLED=true
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_NAMESPACE=dev

./mvnw spring-boot:run

# 方式二：禁用 Nacos，使用本地配置（默认）
export NACOS_CONFIG_ENABLED=false
./mvnw spring-boot:run
```

---

## 3. 配置说明

### 3.1 配置文件优先级

```
Nacos 配置（远程） > 本地 bootstrap.properties > 本地 application.properties
```

### 3.2 核心配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.cloud.nacos.config.server-addr` | Nacos 服务器地址 | 127.0.0.1:8848 |
| `spring.cloud.nacos.config.namespace` | 命名空间（环境隔离） | 空 |
| `spring.cloud.nacos.config.group` | 配置分组 | DEFAULT_GROUP |
| `spring.cloud.nacos.config.file-extension` | 配置文件格式 | properties |
| `spring.cloud.nacos.config.refresh-enabled` | 启用配置刷新 | true |
| `spring.cloud.nacos.config.enabled` | 启用 Nacos 配置 | false |

### 3.3 多环境配置

#### 开发环境（dev）

```properties
spring.cloud.nacos.config.namespace=dev
spring.cloud.nacos.discovery.namespace=dev
```

#### 测试环境（test）

```properties
spring.cloud.nacos.config.namespace=test
spring.cloud.nacos.discovery.namespace=test
```

#### 生产环境（prod）

```properties
spring.cloud.nacos.config.namespace=prod
spring.cloud.nacos.discovery.namespace=prod
```

---

## 4. 动态配置刷新

### 4.1 使用 @RefreshScope

在需要动态刷新的 Bean 上添加 `@RefreshScope` 注解：

```java
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.default-per-minute:60}")
    private int defaultPerMinute;

    // Getter 方法...
}
```

### 4.2 验证动态刷新

1. **修改 Nacos 配置**：
   - 打开 Nacos 控制台
   - 找到 `appointment_system.properties` 配置
   - 修改 `app.rate-limit.default-per-minute` 的值
   - 点击「发布」

2. **查看应用日志**：
   ```
   Nacos:Receive config change event: dataId=appointment_system, group=DEFAULT_GROUP
   Refresh Nacos config for: rateLimitConfig
   ```

3. **验证生效**：
   - 下一次请求将使用新的配置值

---

## 5. 本地开发模式

### 5.1 禁用 Nacos（推荐开发模式）

在开发阶段，建议使用本地配置，禁用 Nacos：

```bash
# 方式一：环境变量
export NACOS_CONFIG_ENABLED=false

# 方式二：启动参数
java -jar appointment_system.jar --spring.cloud.nacos.config.enabled=false

# 方式三：application.properties
spring.cloud.nacos.config.enabled=false
```

### 5.2 本地配置优先级

当 Nacos 不可用时，应用会使用本地 `application.properties` 中的配置作为兜底。

本地配置文件位置：
- `src/main/resources/application.properties`

---

## 6. 服务注册与发现（可选）

### 6.1 启用服务注册

如果需要在 Nacos 中注册服务，修改配置：

```properties
# 启用服务注册
spring.cloud.nacos.discovery.enabled=true

# Nacos 服务器地址
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848

# 命名空间
spring.cloud.nacos.discovery.namespace=dev

# 分组
spring.cloud.nacos.discovery.group=DEFAULT_GROUP
```

### 6.2 服务注册信息

注册后，服务信息如下：

| 属性 | 值 |
|------|-----|
| 服务名 | appointment_system |
| 端口 | 8080 |
| 健康检查 | 默认开启 |

### 6.3 服务发现使用

```java
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@Autowired
private DiscoveryClient discoveryClient;

// 获取服务实例列表
List<ServiceInstance> instances = discoveryClient.getInstances("appointment_system");
for (ServiceInstance instance : instances) {
    System.out.println(instance.getHost() + ":" + instance.getPort());
}
```

---

## 7. 常见问题

### Q1: Nacos 启动报端口占用？

```bash
# 查看端口占用
netstat -an | grep 8848

# 停止占用进程
kill -9 <PID>
```

### Q2: 配置导入后应用读取不到？

1. 检查 `dataId` 是否正确（应为 `appointment_system.properties`）
2. 检查 `group` 是否匹配
3. 检查命名空间是否正确

### Q3: 如何实现配置加密？

1. 安装 Nacos 配置加密插件
2. 在配置值前添加 `{cipher}` 前缀：
   ```
   database.password={cipher}abc123def456
   ```

### Q4: Nacos 不可用时应用会崩溃吗？

不会。应用会使用本地 `application.properties` 作为兜底配置。

### Q5: 如何查看配置加载日志？

```properties
logging.level.com.alibaba.nacos=DEBUG
```

---

## 8. 生产环境注意事项

### 8.1 高可用部署

生产环境建议部署 Nacos 集群：

```
┌─────────────┐
│   Nacos     │
│  Cluster    │
│ (3节点+)     │
└──────┬──────┘
       │
┌──────┴──────┐
│   Nginx     │
│   LB        │
└──────┬──────┘
       │
┌──────┴──────┐
│ Application │
└─────────────┘
```

### 8.2 安全配置

1. **开启认证**：
   ```properties
   nacos.core.auth.enabled=true
   nacos.core.auth.server.identity.key=serverIdentity
   nacos.core.auth.server.identity.value=security
   ```

2. **使用 HTTPS**：
   ```properties
   spring.cloud.nacos.config.server-addr=https://nacos.example.com:8848
   spring.cloud.nacos.discovery.server-addr=https://nacos.example.com:8848
   ```

3. **配置权限控制**：
   - 使用 RAM 或 LDAP 进行权限管理
   - 限制生产环境配置的修改权限

### 8.3 监控告警

1. 配置 Nacos 监控指标导出到 Prometheus
2. 配置告警规则：
   - 配置变更告警
   - 服务异常告警

---

## 9. 参考资料

- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba Nacos Config](https://spring-cloud-alibaba-group.github.io/github-pages/2024.0.0.0/zh-cn碑陶Spring%20Cloud%20Alibaba%20Nacos%20Config.html)
- [Spring Cloud Alibaba Nacos Discovery](https://spring-cloud-alibaba-group.github.io/github-pages/2024.0.0.0/zh-cn碑陶Spring%20Cloud%20Alibaba%20Nacos%20Discovery.html)

---

## 10. 更新日志

### 2026-02-18

- 初始版本创建
- 支持配置中心功能
- 支持动态配置刷新
- 支持多环境配置
- 提供本地配置兜底方案
