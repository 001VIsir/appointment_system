# Database Design

## Core tables

### users
- id (PK, BIGINT, auto)
- username (VARCHAR(50), unique, not null)
- password (VARCHAR(255), not null)
- email (VARCHAR(100), not null)
- role (VARCHAR(20), not null) // ADMIN, MERCHANT, USER
- enabled (BOOLEAN, not null)
- created_at (DATETIME, not null)
- updated_at (DATETIME, not null)

### merchant_profiles
- id (PK, BIGINT, auto)
- user_id (FK -> users.id, unique, not null)
- display_name (VARCHAR(80), not null)
- description (VARCHAR(200))
- session_timeout_minutes (INT, not null)

### service_items
- id (PK, BIGINT, auto)
- merchant_id (FK -> merchant_profiles.id, not null)
- name (VARCHAR(100), not null)
- category (VARCHAR(30), not null) // SPORTS, DINING, TRAVEL, OTHER
- duration_minutes (INT, not null)
- description (VARCHAR(200))
- active (BOOLEAN, not null)
- created_at (DATETIME, not null)
- updated_at (DATETIME, not null)

### bookings
- id (PK, BIGINT, auto)
- service_id (FK -> service_items.id, not null)
- user_id (FK -> users.id, not null)
- start_time (DATETIME, not null)
- end_time (DATETIME, not null)
- status (VARCHAR(20), not null) // CREATED, UPDATED, CANCELED
- created_at (DATETIME, not null)
- updated_at (DATETIME, not null)

## Notes
- Times are stored in UTC and serialized to clients in ISO 8601 with +08:00 offset.
- Session timeout is per-merchant and applied at login.

