CREATE DATABASE IF NOT EXISTS yuyue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE yuyue;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS merchant_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  display_name VARCHAR(80) NOT NULL,
  description VARCHAR(200),
  session_timeout_minutes INT NOT NULL,
  CONSTRAINT fk_merchant_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS service_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(30) NOT NULL,
  duration_minutes INT NOT NULL,
  description VARCHAR(200),
  active BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_service_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id)
);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_booking_service FOREIGN KEY (service_id) REFERENCES service_items(id),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS appointment_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(40) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  description VARCHAR(600),
  active BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_task_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id)
);

CREATE TABLE IF NOT EXISTS appointment_slots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  capacity INT NOT NULL,
  booked_count INT NOT NULL,
  location VARCHAR(200),
  version INT NOT NULL,
  CONSTRAINT fk_slot_task FOREIGN KEY (task_id) REFERENCES appointment_tasks(id)
);

CREATE TABLE IF NOT EXISTS appointment_bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slot_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES appointment_slots(id),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_task_merchant ON appointment_tasks(merchant_id);
CREATE INDEX idx_slot_task ON appointment_slots(task_id);
CREATE INDEX idx_booking_slot ON appointment_bookings(slot_id);
CREATE INDEX idx_booking_user ON appointment_bookings(user_id);
