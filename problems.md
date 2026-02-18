# 问题记录

> 记录开发过程中遇到的问题、原因分析、思考过程和解决方案。

---

## 2026-02-18

### 问题 1: 前端 API 响应格式不匹配

**问题描述：**
前端登录和注册功能失败，控制台显示用户信息无法正确获取。

**原因分析：**
- 后端 `AuthController` 的登录和注册接口直接返回 `UserResponse` 对象
- 前端 `auth.ts` store 错误地使用 `response.data.data` 访问响应数据
- 实际响应结构是 `{ data: UserResponse }`，不是 `{ data: { data: UserResponse } }`

**思考过程：**
1. 首先发现前端登录后用户信息为空
2. 检查浏览器控制台，发现网络请求成功但数据解析错误
3. 对比后端 Controller 代码和前端 API 调用代码
4. 发现响应格式与前端预期不一致

**解决方案：**
修改 `frontend/src/stores/auth.ts`，将：
```typescript
user.value = response.data.data
```
改为：
```typescript
user.value = response.data
```

**涉及文件：**
- `frontend/src/stores/auth.ts`

---

### 问题 2: Redis Session 序列化失败

**问题描述：**
用户登录后，Session 无法正确存储到 Redis，分布式部署时认证失败。

**原因分析：**
- RedisConfig 使用 `GenericJackson2JsonRedisSerializer` 进行 Session 序列化
- Spring Security 的 `UserDetails` 对象包含复杂对象结构
- JSON 序列化无法处理 UserDetails 中的非标准字段

**思考过程：**
1. 测试环境单机运行正常，分布式环境失败
2. 检查 Redis，发现 Session 数据格式异常
3. 查看日志，发现序列化错误
4. 搜索 Spring Session + Spring Security 兼容性问题
5. 确认需要使用 JDK 序列化替代 JSON 序列化

**解决方案：**

1. 修改 `RedisConfig.java`:
```java
// 改为使用 JDK 序列化
return RedisSerializer.java();
```

2. 修改 `User.java`，实现 Serializable:
```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

3. 修改 `CustomUserDetails.java`，实现 Serializable:
```java
public class CustomUserDetails implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

**涉及文件：**
- `src/main/java/org/example/appointment_system/config/RedisConfig.java`
- `src/main/java/org/example/appointment_system/entity/User.java`
- `src/main/java/org/example/appointment_system/security/CustomUserDetails.java`

---

### 问题 3: 敏感配置意外修改

**问题描述：**
git 状态显示 `.env.docker` 和 `docker-compose.yml` 等配置文件被修改。

**原因分析：**
- 这些文件包含本地数据库密码等敏感信息
- 之前的修改可能是为了本地测试
- 不应该将这些敏感信息提交到版本控制

**解决方案：**
使用 `git checkout` 恢复这些文件到原始状态：
```bash
git checkout -- .env.docker docker-compose.yml src/main/resources/application.properties
```

**经验教训：**
- 本地开发环境的敏感配置应该使用 `.gitignore` 忽略
- 或使用环境变量覆盖，而非修改配置文件

---

## 总结

本次会话共遇到 3 个问题，全部已解决：

| 问题 | 状态 |
|------|------|
| 前端 API 响应格式不匹配 | ✅ 已修复 |
| Redis Session 序列化失败 | ✅ 已修复 |
| 敏感配置意外修改 | ✅ 已恢复 |

所有修复已提交到 Git：
- `d7aeea5` - fix: 修复前端 auth store API 响应格式
- `2fe09f8` - fix: 修复 Redis Session 序列化兼容性
