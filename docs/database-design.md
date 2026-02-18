# 数据库设计文档

## 1. 概述

本文档描述预约系统的数据库表结构设计。系统使用 MySQL 8.0，字符集为 utf8mb4。

## 2. 核心表结构

### 2.1 users - 用户表

存储所有用户账户（管理员、商户、普通用户）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密的密码 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 邮箱 |
| role | ENUM | NOT NULL, DEFAULT 'USER' | 角色：ADMIN, MERCHANT, USER |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 账户是否启用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_users_username (username)
- idx_users_email (email)
- idx_users_role (role)

---

### 2.2 merchant_profiles - 商户信息表

存储商户的 business 信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, UNIQUE | 关联 users 表 |
| business_name | VARCHAR(100) | NOT NULL | 商户名称 |
| description | TEXT | | 商户描述 |
| phone | VARCHAR(20) | | 联系电话 |
| address | VARCHAR(255) | | 商户地址 |
| settings | JSON | | 商户设置（会话超时、通知等） |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_merchant_user_id (user_id)
- idx_merchant_business_name (business_name)

---

### 2.3 service_items - 服务项目表

商户提供的服务项目。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| merchant_id | BIGINT | NOT NULL | 关联 merchant_profiles 表 |
| name | VARCHAR(100) | NOT NULL | 服务名称 |
| description | TEXT | | 服务描述 |
| category | ENUM | NOT NULL, DEFAULT 'GENERAL' | 分类：GENERAL, MEDICAL, BEAUTY, CONSULTATION, EDUCATION, FITNESS, OTHER |
| duration | INT | NOT NULL, DEFAULT 30 | 服务时长（分钟） |
| price | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | 价格 |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_service_merchant_id (merchant_id)
- idx_service_category (category)
- idx_service_active (active)

---

### 2.4 appointment_tasks - 预约任务表

商户创建的预约任务（按日期）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| service_id | BIGINT | NOT NULL | 关联 service_items 表 |
| title | VARCHAR(100) | NOT NULL | 任务标题 |
| description | TEXT | | 任务描述 |
| task_date | DATE | NOT NULL | 任务日期 |
| total_capacity | INT | NOT NULL, DEFAULT 1 | 最大预约数 |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_task_service_id (service_id)
- idx_task_date (task_date)
- idx_task_active (active)
- idx_task_service_date (service_id, task_date)

---

### 2.5 appointment_slots - 预约时段表

预约任务内的时间段。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| task_id | BIGINT | NOT NULL | 关联 appointment_tasks 表 |
| start_time | TIME | NOT NULL | 时段开始时间 |
| end_time | TIME | NOT NULL | 时段结束时间 |
| capacity | INT | NOT NULL, DEFAULT 1 | 该时段最大预约数 |
| booked_count | INT | NOT NULL, DEFAULT 0 | 已预约数量 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_slot_task_id (task_id)
- idx_slot_time (start_time, end_time)

**约束：**
- capacity > 0
- booked_count >= 0 AND booked_count <= capacity

---

### 2.6 bookings - 预约记录表

用户的预约记录，使用乐观锁防止并发超卖。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 关联 users 表 |
| slot_id | BIGINT | NOT NULL | 关联 appointment_slots 表 |
| status | ENUM | NOT NULL, DEFAULT 'PENDING' | 状态：PENDING, CONFIRMED, CANCELLED, COMPLETED |
| remark | TEXT | | 用户备注 |
| version | INT | NOT NULL, DEFAULT 0 | 乐观锁版本号 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_booking_user_id (user_id)
- idx_booking_slot_id (slot_id)
- idx_booking_status (status)
- idx_booking_created (created_at)

**约束：**
- UNIQUE KEY uk_user_slot (user_id, slot_id) - 防止重复预约

---

## 3. 设计要点

### 3.1 时间处理

- 所有时间存储在 UTC，序列化到客户端时使用 ISO 8601 格式并带有 +08:00 时区偏移。
- 建议在应用层处理时区转换。

### 3.2 并发控制

- 预约表使用乐观锁（version 字段）防止高并发下的超卖问题。
- 时段表通过 booked_count 和 capacity 字段控制预约数量。

### 3.3 权限模型

- 用户角色：admin（管理员）、merchant（商户）、user（普通用户）
- 商户只能管理自己的服务项目和预约任务
- 普通用户只能查看和预约

### 3.4 商户设置

- merchant_profiles.settings 字段使用 JSON 格式存储商户自定义设置
- 包括会话超时、通知偏好等配置

## 4. 关系图

```
users (1) ----< (N) merchant_profiles
merchant_profiles (1) ----< (N) service_items
service_items (1) ----< (N) appointment_tasks
appointment_tasks (1) ----< (N) appointment_slots
appointment_slots (1) ----< (N) bookings
users (1) ----< (N) bookings
```
