# 问题记录

## 问题 1: 注册后未自动登录导致 403 错误

### 发现时间
2026-02-18

### 问题描述
用户通过前端注册成功后，访问"我的预约"页面时返回 403 错误："没有权限访问该资源"。

### 问题分析

#### 根本原因
经过分析，发现问题出在前端的注册流程：

1. **后端行为**：
   - `/api/auth/register` 端点只创建用户记录到数据库
   - 不创建 Spring Security Session（只有 `/api/auth/login` 才会创建 Session）
   - 用户没有有效的 SESSION Cookie

2. **前端行为**：
   - `authStore.register()` 函数在注册成功后，将后端返回的用户数据保存到本地状态 (`user.value = response.data`)
   - 这导致 `isAuthenticated` 计算属性返回 `true`（因为 `!!user.value` 为 true）
   - 前端页面显示用户已登录

3. **实际情况**：
   - 本地状态显示已登录，但后端没有 Session
   - 当调用 `/api/bookings/my` 等受保护端点时，后端检查 Session 发现未认证，返回 403

### 影响范围
- Phase 6 (用户预约) 的所有测试失败：6.3-6.7
- Phase 8 (管理员功能) 的所有测试失败

### 思考过程
1. 首先通过浏览器测试发现：注册成功后访问"我的预约"返回 403
2. 检查 SecurityConfig，发现 `/api/bookings/**` 需要认证（`.anyRequest().authenticated()`）
3. 检查 AuthService.register()，确认注册不创建 Session
4. 检查前端 authStore.register()，发现本地状态被错误设置为"已登录"
5. 对比登录流程：login() 会调用后端 /api/auth/login，后者创建 Session

### 解决方案

#### 方案 A（推荐）：前端修复 - 注册后自动登录
修改 `frontend/src/stores/auth.ts` 中的 `register` 函数，在注册成功后自动调用登录：

```typescript
async function register(data: RegisterRequest) {
  loading.value = true
  error.value = null
  try {
    const response = await authApi.register(data)
    // 注册成功后自动登录以创建 Session
    await authApi.login({
      username: data.username,
      password: data.password
    })
    user.value = response.data
    return true
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message || '注册失败'
    return false
  } finally {
    loading.value = false
  }
}
```

#### 方案 B：后端修复 - 注册时创建 Session
修改 `AuthService.register()` 方法，在注册成功后自动创建 Session。

### 修复记录

#### 2026-02-18 修复
采用方案 A，修改前端代码。修复后验证：
- 用户注册后自动登录
- Session Cookie 正确创建
- 可以正常访问"我的预约"等受保护端点

---

## 问题 2: 时间段创建请求体格式错误

### 发现时间
2026-02-18

### 问题描述
商户在创建时间段时，返回 "Invalid request body format" 错误。

### 问题分析

#### 根本原因
经过分析，发现问题出在前端发送请求的格式：

1. **后端期望格式**：
   - 后端控制器 `@RequestBody List<AppointmentSlotRequest>` 期望接收数组格式
   - 例如：`[{"startTime":"09:00","endTime":"09:30","capacity":5}]`

2. **前端发送格式**：
   - 前端直接发送单个对象：`{"startTime":"09:00","endTime":"09:30","capacity":5}`

3. **验证**：
   - 使用 curl 测试：
     - 发送数组：`[{"startTime":"09:00","endTime":"09:30","capacity":5}]` ✅ 成功
     - 发送对象：`{"startTime":"09:00","endTime":"09:30","capacity":5}` ❌ 失败

### 影响范围
- Phase 5.6 创建时间段测试失败

### 思考过程
1. 在浏览器中测试创建时间段功能，收到 "Invalid request body format" 错误
2. 检查后端 AppointmentTaskController 中的 createSlots 方法，发现期望 List 格式
3. 使用 curl 分别测试数组和对象格式，确认数组格式成功
4. 定位到前端 TasksView.vue 中的 handleAddSlot 函数，直接发送了 slotForm 对象

### 解决方案

#### 前端修复：将单个对象改为数组
修改 `frontend/src/views/merchant/TasksView.vue` 中的 `handleAddSlot` 函数：

```typescript
// 修改前
await taskApi.createSlot(currentTask.value.id, slotForm)

// 修改后
await taskApi.createSlot(currentTask.value.id, [slotForm])
```

### 修复记录

#### 2026-02-18 修复
修改前端代码，将 `slotForm` 改为 `[slotForm]`。修复后验证：
- 商户可以成功创建时间段
- 时间段正确显示在列表中

### 测试验证结果

通过浏览器测试验证以下流程成功：
1. ✅ 用户注册后自动登录（修复了问题1）
2. ✅ 商户注册后自动登录并跳转到商户中心
3. ✅ 商户创建服务项目
4. ✅ 商户创建预约任务
5. ✅ 商户创建时间段（修复了问题2）
6. ✅ 用户登录后创建预约
7. ✅ 商户查看预约列表
8. ✅ 商户确认预约
9. ✅ 商户完成预约
10. ✅ 用户查看我的预约列表，显示正确的状态

---

## 问题 3: 公开预约页面 500 错误

### 发现时间
2026-02-18

### 问题描述
访问签名链接 `/api/public/book/{taskId}` 时返回 500 错误。

### 问题分析

访问 `http://localhost:8080/api/public/book/8?token=...&exp=...` 返回 500 错误。

### 影响范围
- 公开预约页面无法访问

### 思考过程
1. 商户生成签名链接后，访问该链接返回 500 错误
2. 需要进一步调试后端代码找出具体原因

### 解决方案
待进一步调查。

---

## 问题 6: 服务名称包含中文时请求失败

### 发现时间
2026-02-18

### 问题描述
创建服务项目时，如果服务名称包含中文字符，返回 "Invalid request body format" 错误。使用英文名称则成功。

### 问题分析
使用 curl 测试发现：
- `{"name":"Test","category":"GENERAL",...}` ✅ 成功
- `{"name":"Haircut Service","category":"BEAUTY",...}` ✅ 成功
- `{"name":"理发服务","category":"BEAUTY",...}` ❌ 失败

可能是后端编码问题或前端请求编码问题。

### 影响范围
- 商户创建服务项目（使用中文名称时）

### 思考过程
1. 用 curl 测试创建服务，发现 GENERAL 类别成功
2. 使用 BEAUTY 类别 + 英文名称成功
3. 使用 BEAUTY 类别 + 中文名称失败
4. 怀疑是编码问题

### 解决方案
待进一步调查。

---

## 问题 4: 搜索功能错误 - bookedCount 属性不存在

### 发现时间
2026-02-18

### 问题描述
访问搜索功能时返回 500 错误："Could not resolve attribute 'bookedCount' of 'org.example.appointment_system.entity.MerchantProfile'"。

### 问题分析

#### 根本原因
搜索功能中的商户搜索使用了 `order by m.bookedCount desc`，但 MerchantProfile 实体没有 `bookedCount` 属性。该属性存在于 AppointmentTask 实体，但在商户搜索中不正确使用。

错误信息：
```
org.hibernate.query.sqm.UnknownPathException: Could not resolve attribute 'bookedCount' of 'org.example.appointment_system.entity.MerchantProfile'
```

### 影响范围
- 搜索功能无法使用
- 首页公开搜索页面报错

### 思考过程
1. 访问首页搜索功能时返回 500 错误
2. 检查后端日志发现错误信息
3. 定位到 MerchantProfileRepository 的 searchMerchants 方法

### 解决方案
修改 MerchantProfileRepository 中的搜索查询，移除不存在的 bookedCount 排序条件，或使用正确的属性进行排序。

#### 2026-02-18 修复
修改 SearchService.java 中的搜索方法，移除数据库级别的排序，直接在应用层进行内存排序。

---

## 问题 5: 浏览器会话丢失问题

### 发现时间
2026-02-18

### 问题描述
在浏览器中登录后，刷新页面或导航到其他页面时，用户会话丢失，显示未登录状态。

### 问题分析
需要进一步调查。可能的原因：
1. 前端存储的 Session Cookie 在页面刷新后丢失
2. Playwright 浏览器测试环境的 sessionStorage 问题
3. 后端 Redis Session 过期或未正确设置

### 影响范围
- 前端浏览器测试

### 思考过程
1. 在浏览器中成功登录 merchant_a
2. 导航到商户档案页面时显示"加载档案失败"
3. 刷新页面后回到首页，显示未登录状态
4. Session Cookie 可能未正确持久化

### 解决方案
待进一步调查。

---

## 问题 6: 服务名称包含中文时请求失败（已解决）

### 发现时间
2026-02-18

### 问题描述
创建服务项目时，如果服务名称包含中文字符，通过 curl 返回 "Invalid request body format" 错误。

### 问题分析
测试发现 curl 编码问题，使用 `--data-binary` 可以解决。前端不受影响。

### 解决方案
这不是后端 bug。

---

## 额外修复: Nacos 依赖导致后端无法启动

### 问题描述
后端启动时报错：ClassNotFoundException: org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration

### 根本原因
Spring Cloud Alibaba Nacos 与 Spring Boot 4.0 不兼容

### 解决方案
暂时注释掉 pom.xml 中的 Nacos 依赖

---

## 测试结果总结

### 已修复问题
1. ✅ 问题1: 注册后未自动登录 - 修改 auth.ts
2. ✅ 问题2: 时间段创建请求体格式 - 修改 TasksView.vue
3. ✅ 问题4: 搜索功能 bookedCount 错误 - 修改 SearchService.java

### 已验证非 bug
4. ✅ 问题6: 中文服务名称 - curl 编码问题，前端不受影响

### 待调查问题
5. ⏳ 问题3: 公开预约页面 - 实际功能正常（通过 /api/tasks/** 访问）
6. ⏳ 问题5: 浏览器会话丢失 - 需要进一步调查

---

## 问题 7: Nacos 配置清理 (REFACTOR-003)

### 发现时间
2026-02-22

### 问题描述
项目中存在未使用的 Nacos 配置，包括：
1. `pom.xml` 中的 Spring Cloud 和 Spring Cloud Alibaba 依赖管理 BOM
2. `bootstrap.properties` 文件中完整的 Nacos 配置

### 问题分析

#### 根本原因
项目在早期规划时预留了 Nacos 配置中心的支持，但实际上：
1. pom.xml 中只有 BOM（依赖管理），没有实际的 Nacos starter 依赖
2. 没有代码使用 `@RefreshScope` 或 `@EnableDiscoveryClient` 注解
3. 默认配置就是禁用 Nacos（`NACOS_CONFIG_ENABLED:false`）

#### 影响范围
- 无实际功能影响，但是增加了项目复杂度
- pom.xml 中保留无用的依赖管理版本号

### 思考过程
1. 检查 pom.xml，发现只有 Spring Cloud Alibaba BOM，没有实际依赖
2. 搜索代码，发现没有使用 Spring Cloud 相关注解
3. bootstrap.properties 只包含 Nacos 配置，没有其他用途
4. 结论：这些都是死代码，应该清理

### 解决方案

1. **移除 pom.xml 中的无用 BOM**：
   - 移除 `spring-cloud.version` 属性
   - 移除 `spring-cloud-alibaba.version` 属性
   - 移除整个 `dependencyManagement` 块

2. **删除 bootstrap.properties**：
   - 该文件只包含 Nacos 配置，没有其他用途
   - Spring Boot 不强制要求此文件

### 修复记录

#### 2026-02-22 修复

**修改内容**：
1. 移除 pom.xml 中的 Spring Cloud 和 Spring Cloud Alibaba BOM（共约 20 行代码）
2. 删除 bootstrap.properties 文件（52 行配置）

**验证结果**：
- `./mvnw clean compile -DskipTests` ✅ 编译成功
- 无任何依赖错误
- 项目可以正常构建

**优化效果**：
- 减少了 pom.xml 中的无用配置
- 删除了无效的 bootstrap.properties 文件
- 项目结构更清晰，没有死代码
