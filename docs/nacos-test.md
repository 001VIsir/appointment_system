# Nacos 配置中心测试文档

> 本文档详细记录 Nacos 配置中心的测试步骤和验证方法

---

## 1. 测试环境

### 1.1 前置条件

| 组件 | 要求 | 说明 |
|------|------|------|
| JDK | 21+ | 已安装 |
| Maven | 3.9+ | 已安装 |
| MySQL | 8.0+ | 已启动，端口 3306 |
| Redis | 7.x | 已启动，端口 6379 |
| RabbitMQ | 3.12+ | 已启动，端口 5672 |
| Nacos | 2.x | 需要启动 |

### 1.2 Nacos 启动方式

#### 方式一：Docker 启动（推荐）

```bash
# 启动 Nacos 单机版
docker run -d --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e JVM_XMS=256m \
  -e JVM_XMX=512m \
  nacos/nacos-server:v2.3.0
```

#### 方式二：直接启动（需要 Java）

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/v2.3.0/nacos-server.jar

# 启动 Nacos
java -jar nacos-server.jar --spring.profiles.active=standalone
```

#### 方式三：Windows 启动

```bat
:: 下载 Nacos
curl -L -o nacos-server.jar https://github.com/alibaba/nacos/releases/download/v2.3.0/nacos-server.jar

:: 启动 Nacos
java -Xms256m -Xmx512m -jar nacos-server.jar --spring.profiles.active=standalone
```

---

## 2. Nacos 控制台操作

### 2.1 访问 Nacos 控制台

1. 打开浏览器访问：http://localhost:8848/nacos
2. 用户名：`nacos`
3. 密码：`nacos`

```
┌─────────────────────────────────────────────────────────────┐
│                         Nacos 控制台                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│    [logo] Nacos   🔍 搜索配置              👤 nacos        │
│                                                             │
│    ─────────────────────────────────────────────────────    │
│    │ 配置管理 │ 服务管理 │ 集群管理 │ 命名空间 │            │
│    ─────────────────────────────────────────────────────    │
│                                                             │
│    配置列表                                                 │
│    ┌───────────────────────────────────────────────────┐   │
│    │ + 导出  + 导入  刷新                                 │   │
│    ├───────────────────────────────────────────────────┤   │
│    │ Data ID          │ Group          │  操作          │   │
│    │ ─────────────────────────────────────────────────  │   │
│    │                  │                │                │   │
│    └───────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 创建命名空间

1. 点击左侧「命名空间」
2. 点击「创建命名空间」
3. 填写信息：

| 字段 | 值 |
|------|-----|
| 命名空间 ID | dev |
| 命名空间名称 | 开发环境 |
| 命名空间描述 | 开发环境配置 |

重复创建 test（测试环境）和 prod（生产环境）。

```
┌─────────────────────────────────────────────────────────────┐
│                      创建命名空间                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  命名空间 ID:  [dev                        ] *              │
│  命名空间名称: [开发环境                    ] *              │
│  命名空间描述: [开发环境配置                  ]              │
│                                                             │
│          [取消]                    [确定]                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 导入配置

1. 点击「配置管理」→「配置列表」
2. 点击右上角「+ 导入配置」
3. 选择文件：`config/nacos/appointment_system-dev.properties`
4. 配置格式：`Properties`
5. 选择目标命名空间：`dev`
6. 点击「上传」

```
┌─────────────────────────────────────────────────────────────┐
│                      导入配置                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  选择文件: [选择文件...] appointment_system-dev.properties  │
│                                                             │
│  配置格式:  (●) Properties  ( ) JSON  ( ) YAML            │
│                                                             │
│  目标命名空间: [dev ▼]                                     │
│                                                             │
│  目标配置分组: [DEFAULT_GROUP ▼]                            │
│                                                             │
│          [取消]                    [上传并导入]              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 测试场景

### 场景一：本地配置模式（默认）

**目标**：验证应用在不使用 Nacos 时能正常运行

**步骤**：

1. **确认 Nacos 配置已禁用**
   ```bash
   # 不设置任何 Nacos 环境变量，使用默认值 false
   export NACOS_CONFIG_ENABLED=false
   ```

2. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **验证日志**
   ```
   预期日志：
   - Nacos Config: disabled
   - 使用本地 application.properties 配置
   - 应用正常启动
   ```

4. **测试 API**
   ```bash
   curl http://localhost:8080/actuator/health
   # 预期返回：{"status":"UP"}
   ```

**预期结果**：
- ✅ 应用正常启动
- ✅ 可以正常登录、预约
- ✅ 无 Nacos 相关连接日志

---

### 场景二：Nacos 配置中心模式

**目标**：验证应用能够从 Nacos 读取配置

**前置条件**：
- Nacos 已启动
- 已导入配置到 Nacos

**步骤**：

1. **启用 Nacos 配置**
   ```bash
   export NACOS_CONFIG_ENABLED=true
   export NACOS_SERVER_ADDR=127.0.0.1:8848
   export NACOS_NAMESPACE=dev
   ```

2. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **验证日志**
   ```
   预期日志：
   - Nacos Config: dataId=appointment_system, group=DEFAULT_GROUP
   - Nacos config loaded: {...}
   - Bean initialized with config from Nacos
   ```

4. **测试 API**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

**预期结果**：
- ✅ 应用启动时从 Nacos 读取配置
- ✅ 配置值来自 Nacos 而非本地文件

---

### 场景三：动态配置刷新

**目标**：验证修改 Nacos 配置后，应用无需重启即可生效

**前置条件**：
- 场景二已验证通过

**步骤**：

1. **确认限流初始状态**
   ```bash
   # 假设当前 app.rate-limit.enabled=false
   ```

2. **通过 Nacos 控制台修改配置**
   - 找到配置 `appointment_system.properties`
   - 修改 `app.rate-limit.enabled=true`
   - 点击「发布」

3. **观察日志**
   ```
   预期日志：
   Nacos:Receive config change event: dataId=appointment_system
   Refresh Nacos config for: rateLimitConfig
   Nacos config changed
   ```

4. **验证生效**
   ```bash
   # 下一次请求应该使用新的限流配置
   ```

**预期结果**：
- ✅ 配置变更后应用日志显示收到通知
- ✅ 限流配置自动更新
- ✅ 无需重启应用

---

### 场景四：Nacos 不可用时的兜底

**目标**：验证 Nacos 不可用时，应用使用本地配置继续运行

**步骤**：

1. **停止 Nacos**
   ```bash
   docker stop nacos
   # 或者
   # netstat 找到进程并 kill
   ```

2. **启用 Nacos 配置但 Nacos 不可用**
   ```bash
   export NACOS_CONFIG_ENABLED=true
   export NACOS_SERVER_ADDR=127.0.0.1:8848
   ```

3. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **验证日志**
   ```
   预期日志：
   - Nacos config server not available, use local config
   - Fallback to local configuration
   - 应用使用本地 application.properties 启动
   ```

**预期结果**：
- ✅ 应用启动成功
- ✅ 使用本地配置运行
- ✅ 无需手动干预

---

### 场景五：多环境配置切换

**目标**：验证不同环境配置的正确切换

**步骤**：

1. **分别导入配置到不同命名空间**
   - dev 命名空间：数据库 localhost:3306
   - test 命名空间：数据库 mysql-test:3306
   - prod 命名空间：数据库 mysql-prod:3306

2. **切换 dev 环境**
   ```bash
   export NACOS_NAMESPACE=dev
   # 启动应用，连接 localhost:3306
   ```

3. **切换 test 环境**
   ```bash
   export NACOS_NAMESPACE=test
   # 启动应用，连接 mysql-test:3306
   ```

4. **切换 prod 环境**
   ```bash
   export NACOS_NAMESPACE=prod
   # 启动应用，连接 mysql-prod:3306
   ```

**预期结果**：
- ✅ 每套环境使用对应的数据库配置
- ✅ 环境完全隔离，互不影响

---

## 4. API 测试

### 4.1 配置相关 API

#### 获取配置

```bash
curl -X GET "http://localhost:8848/nacos/v1/cs/configs?dataId=appointment_system.properties&group=DEFAULT_GROUP&namespaceId=dev"
```

响应：
```properties
spring.application.name=appointment_system
spring.datasource.url=jdbc:mysql://localhost:3306/...
...
```

#### 发布配置

```bash
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=appointment_system.properties" \
  -d "group=DEFAULT_GROUP" \
  -d "namespaceId=dev" \
  -d "content=spring.application.name=appointment_system"
```

#### 删除配置

```bash
curl -X DELETE "http://localhost:8848/nacos/v1/cs/configs?dataId=appointment_system.properties&group=DEFAULT_GROUP&namespaceId=dev"
```

### 4.2 命名空间相关 API

#### 创建命名空间

```bash
curl -X POST "http://localhost:8848/nacos/v1/console/namespaces" \
  -d "customNamespaceId=dev&namespaceName=开发环境&namespaceDesc=开发环境配置"
```

#### 获取命名空间列表

```bash
curl -X GET "http://localhost:8848/nacos/v1/console/namespaces"
```

---

## 5. 验证检查清单

### 5.1 功能验证

| 编号 | 测试项 | 预期结果 | 实际结果 | 状态 |
|------|--------|----------|----------|------|
| 1 | 本地配置模式启动 | 应用正常启动 | | |
| 2 | Nacos 配置模式启动 | 从 Nacos 读取配置 | | |
| 3 | 动态配置刷新 | 配置变更自动生效 | | |
| 4 | Nacos 不可用兜底 | 使用本地配置 | | |
| 5 | 多环境切换 | 各环境配置隔离 | | |

### 5.2 配置项验证

| 配置项 | Nacos 值 | 应用读取值 | 验证 |
|--------|----------|------------|------|
| app.rate-limit.enabled | true/false | | |
| app.rate-limit.default-per-minute | 60/100 | | |
| logging.level | DEBUG/INFO | | |

---

## 6. 常见问题排查

### 问题一：应用启动报 "Nacos connection refused"

**原因**：Nacos 服务器未启动或地址错误

**解决**：
```bash
# 检查 Nacos 是否启动
docker ps | grep nacos

# 检查端口
netstat -an | grep 8848

# 确认地址配置
export NACOS_SERVER_ADDR=127.0.0.1:8848
```

### 问题二：配置读取不到

**原因**：dataId 或 group 不匹配

**解决**：
```properties
# 确认 bootstrap.properties 中的配置
spring.application.name=appointment_system  # 对应 dataId
spring.cloud.nacos.config.group=DEFAULT_GROUP # 对应 group
```

### 问题三：动态刷新不生效

**原因**：Bean 没有加 @RefreshScope 注解

**解决**：
```java
@Component
@RefreshScope  // 必须添加这个注解
public class RateLimitConfig {
    // ...
}
```

---

## 7. 测试记录模板

```
┌─────────────────────────────────────────────────────────────┐
│                    测试记录                                   │
├─────────────────────────────────────────────────────────────┤
│ 测试日期：____年__月__日                                     │
│ 测试人员：__________                                        │
│ Nacos 版本：__________                                      │
│ 应用版本：__________                                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 测试场景：__________                                        │
│                                                             │
│ 测试步骤：                                                  │
│ 1. _______________                                         │
│ 2. _______________                                         │
│ 3. _______________                                         │
│                                                             │
│ 预期结果：__________                                        │
│ 实际结果：__________                                        │
│                                                             │
│ 测试结论：□ 通过  □ 失败                                    │
│ 发现问题：__________                                        │
│ 改进建议：__________                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 测试完成确认

完成所有测试后，请确认：

- [ ] 场景一：本地配置模式 - 功能正常
- [ ] 场景二：Nacos 配置模式 - 功能正常
- [ ] 场景三：动态配置刷新 - 功能正常
- [ ] 场景四：兜底机制 - 功能正常
- [ ] 场景五：多环境切换 - 功能正常
- [ ] 文档已更新

---

## 9. 相关文档

- [Nacos 使用指南](nacos-usage.md) - 详细使用说明
- [Nacos 原理深度解析](nacos-principle.md) - 技术原理
- [配置导入指南](../config/nacos/import-guide.md) - 导入步骤
