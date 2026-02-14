-- =============================================
-- Appointment System - Initial Schema Migration
-- Version: 1.0
-- Description: Creates all core tables for the appointment platform
-- =============================================

-- =============================================
-- 1. Users Table
-- Stores all user accounts (Admin, Merchant, User)
-- =============================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('ADMIN', 'MERCHANT', 'USER') NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

-- =============================================
-- 2. Merchant Profiles Table
-- Stores merchant business information
-- =============================================
CREATE TABLE merchant_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    business_name VARCHAR(100) NOT NULL,
    description TEXT,
    phone VARCHAR(20),
    address VARCHAR(255),
    settings JSON COMMENT 'Merchant settings as JSON (session timeout, notifications, etc.)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_merchant_user_id (user_id),
    INDEX idx_merchant_business_name (business_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Merchant business profiles';

-- =============================================
-- 3. Service Items Table
-- Services offered by merchants
-- =============================================
CREATE TABLE service_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category ENUM('GENERAL', 'MEDICAL', 'BEAUTY', 'CONSULTATION', 'EDUCATION', 'FITNESS', 'OTHER') NOT NULL DEFAULT 'GENERAL',
    duration INT NOT NULL DEFAULT 30 COMMENT 'Service duration in minutes',
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_service_merchant_id (merchant_id),
    INDEX idx_service_category (category),
    INDEX idx_service_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Service items offered by merchants';

-- =============================================
-- 4. Appointment Tasks Table
-- Appointment tasks created by merchants for booking
-- =============================================
CREATE TABLE appointment_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    task_date DATE NOT NULL COMMENT 'Date of the appointment task',
    total_capacity INT NOT NULL DEFAULT 1 COMMENT 'Maximum total bookings for this task',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_service FOREIGN KEY (service_id) REFERENCES service_items(id) ON DELETE CASCADE,
    INDEX idx_task_service_id (service_id),
    INDEX idx_task_date (task_date),
    INDEX idx_task_active (active),
    INDEX idx_task_service_date (service_id, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appointment tasks for booking';

-- =============================================
-- 5. Appointment Slots Table
-- Time slots within an appointment task
-- =============================================
CREATE TABLE appointment_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    start_time TIME NOT NULL COMMENT 'Slot start time',
    end_time TIME NOT NULL COMMENT 'Slot end time',
    capacity INT NOT NULL DEFAULT 1 COMMENT 'Maximum bookings for this slot',
    booked_count INT NOT NULL DEFAULT 0 COMMENT 'Current number of bookings',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_slot_task FOREIGN KEY (task_id) REFERENCES appointment_tasks(id) ON DELETE CASCADE,
    INDEX idx_slot_task_id (task_id),
    INDEX idx_slot_time (start_time, end_time),
    CONSTRAINT chk_slot_capacity CHECK (capacity > 0),
    CONSTRAINT chk_slot_booked CHECK (booked_count >= 0 AND booked_count <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Time slots for appointment tasks';

-- =============================================
-- 6. Bookings Table
-- User bookings for appointment slots
-- Uses optimistic locking via version column
-- =============================================
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    remark TEXT COMMENT 'User remark for the booking',
    version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES appointment_slots(id) ON DELETE CASCADE,
    INDEX idx_booking_user_id (user_id),
    INDEX idx_booking_slot_id (slot_id),
    INDEX idx_booking_status (status),
    INDEX idx_booking_created (created_at),
    UNIQUE KEY uk_user_slot (user_id, slot_id) COMMENT 'Prevent duplicate bookings'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User bookings';

-- =============================================
-- 7. Flyway Schema History Table (managed by Flyway)
-- Note: Flyway creates this automatically, this is just for documentation
-- =============================================

-- =============================================
-- Initial Data (Optional - for testing)
-- =============================================

-- Insert default admin user (password: admin123)
-- INSERT INTO users (username, password, email, role, enabled)
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6V.S', 'admin@example.com', 'ADMIN', TRUE);
