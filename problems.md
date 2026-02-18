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
