-- =====================================================
-- 压力测试数据准备脚本
-- FEAT-036: 压力测试
-- =====================================================
-- 使用方法:
-- mysql -u root -p appointment_system < prepare-test-data.sql
-- =====================================================

-- 清理旧的测试数据（可选，谨慎使用）
-- DELETE FROM bookings WHERE remark LIKE '%Load test%';
-- DELETE FROM appointment_slots WHERE task_id IN (SELECT id FROM appointment_tasks WHERE title LIKE '%Load Test%');
-- DELETE FROM appointment_tasks WHERE title LIKE '%Load Test%';
-- DELETE FROM service_items WHERE name LIKE '%Load Test%';
-- DELETE FROM merchant_profiles WHERE business_name LIKE '%Load Test%';
-- DELETE FROM users WHERE username LIKE 'test_%' OR username LIKE 'loadtest_%';

-- =====================================================
-- 1. 创建测试商户
-- =====================================================

-- 创建测试商户用户
INSERT INTO users (username, password, email, role, created_at, updated_at)
VALUES
    ('loadtest_merchant1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'merchant1@loadtest.com', 'MERCHANT', NOW(), NOW()),
    ('loadtest_merchant2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'merchant2@loadtest.com', 'MERCHANT', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 创建商户档案
INSERT INTO merchant_profiles (user_id, business_name, description, phone, address, settings, created_at, updated_at)
SELECT id, 'Load Test 商户 1', '压力测试专用商户', '13800000001', '测试地址1', '{"sessionTimeout": 1800}', NOW(), NOW()
FROM users WHERE username = 'loadtest_merchant1'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO merchant_profiles (user_id, business_name, description, phone, address, settings, created_at, updated_at)
SELECT id, 'Load Test 商户 2', '压力测试专用商户', '13800000002', '测试地址2', '{"sessionTimeout": 1800}', NOW(), NOW()
FROM users WHERE username = 'loadtest_merchant2'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 2. 创建测试服务项
-- =====================================================

INSERT INTO service_items (merchant_id, name, description, category, duration_minutes, price, active, created_at, updated_at)
SELECT mp.id, 'Load Test 服务 A', '压力测试服务A', 'CONSULTATION', 60, 100.00, true, NOW(), NOW()
FROM merchant_profiles mp
JOIN users u ON mp.user_id = u.id
WHERE u.username = 'loadtest_merchant1'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO service_items (merchant_id, name, description, category, duration_minutes, price, active, created_at, updated_at)
SELECT mp.id, 'Load Test 服务 B', '压力测试服务B', 'CONSULTATION', 30, 50.00, true, NOW(), NOW()
FROM merchant_profiles mp
JOIN users u ON mp.user_id = u.id
WHERE u.username = 'loadtest_merchant1'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 3. 创建测试预约任务 (用于压测)
-- =====================================================

-- 创建一个高容量的任务用于并发测试
INSERT INTO appointment_tasks (service_id, title, description, task_date, total_capacity, active, created_at, updated_at)
SELECT si.id, 'Load Test 预约任务 - 高容量', '用于压力测试的高容量任务', CURDATE() + INTERVAL 7 DAY, 1000, true, NOW(), NOW()
FROM service_items si
WHERE si.name = 'Load Test 服务 A'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 创建普通测试任务
INSERT INTO appointment_tasks (service_id, title, description, task_date, total_capacity, active, created_at, updated_at)
SELECT si.id, 'Load Test 预约任务 - 普通', '用于压力测试的普通任务', CURDATE() + INTERVAL 1 DAY, 100, true, NOW(), NOW()
FROM service_items si
WHERE si.name = 'Load Test 服务 B'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 4. 创建测试时段 (用于压测)
-- =====================================================

-- 为高容量任务创建时段
INSERT INTO appointment_slots (task_id, start_time, end_time, capacity, booked_count, created_at, updated_at)
SELECT at.id,
       CONCAT(CURDATE() + INTERVAL 7 DAY, ' 09:00:00'),
       CONCAT(CURDATE() + INTERVAL 7 DAY, ' 10:00:00'),
       500, 0, NOW(), NOW()
FROM appointment_tasks at
WHERE at.title = 'Load Test 预约任务 - 高容量'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO appointment_slots (task_id, start_time, end_time, capacity, booked_count, created_at, updated_at)
SELECT at.id,
       CONCAT(CURDATE() + INTERVAL 7 DAY, ' 10:00:00'),
       CONCAT(CURDATE() + INTERVAL 7 DAY, ' 11:00:00'),
       500, 0, NOW(), NOW()
FROM appointment_tasks at
WHERE at.title = 'Load Test 预约任务 - 高容量'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 为普通任务创建多个时段
INSERT INTO appointment_slots (task_id, start_time, end_time, capacity, booked_count, created_at, updated_at)
SELECT at.id,
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 09:00:00'),
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 09:30:00'),
       10, 0, NOW(), NOW()
FROM appointment_tasks at
WHERE at.title = 'Load Test 预约任务 - 普通'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO appointment_slots (task_id, start_time, end_time, capacity, booked_count, created_at, updated_at)
SELECT at.id,
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 09:30:00'),
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:00:00'),
       10, 0, NOW(), NOW()
FROM appointment_tasks at
WHERE at.title = 'Load Test 预约任务 - 普通'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO appointment_slots (task_id, start_time, end_time, capacity, booked_count, created_at, updated_at)
SELECT at.id,
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:00:00'),
       CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:30:00'),
       10, 0, NOW(), NOW()
FROM appointment_tasks at
WHERE at.title = 'Load Test 预约任务 - 普通'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 5. 验证数据
-- =====================================================

SELECT '=== 压力测试数据准备完成 ===' AS Status;

SELECT '--- 测试用户 ---' AS Info;
SELECT id, username, email, role FROM users WHERE username LIKE 'loadtest_%';

SELECT '--- 商户档案 ---' AS Info;
SELECT mp.id, u.username, mp.business_name
FROM merchant_profiles mp
JOIN users u ON mp.user_id = u.id
WHERE u.username LIKE 'loadtest_%';

SELECT '--- 服务项 ---' AS Info;
SELECT id, name, category, duration_minutes, price
FROM service_items
WHERE name LIKE 'Load Test%';

SELECT '--- 预约任务 ---' AS Info;
SELECT id, title, task_date, total_capacity, active
FROM appointment_tasks
WHERE title LIKE 'Load Test%';

SELECT '--- 时段 ---' AS Info;
SELECT s.id, t.title, s.start_time, s.end_time, s.capacity, s.booked_count
FROM appointment_slots s
JOIN appointment_tasks t ON s.task_id = t.id
WHERE t.title LIKE 'Load Test%';

-- =====================================================
-- 6. 输出关键ID供压测使用
-- =====================================================

SELECT '=== 压力测试关键ID ===' AS Info;
SELECT
    t.id AS task_id,
    s.id AS slot_id,
    t.title AS task_title,
    s.capacity AS slot_capacity
FROM appointment_tasks t
JOIN appointment_slots s ON t.id = s.task_id
WHERE t.title = 'Load Test 预约任务 - 高容量'
ORDER BY s.id
LIMIT 1;
