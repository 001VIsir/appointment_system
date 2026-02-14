# API Documentation

## 第三方接入
- 商户自建前端/小程序接入说明见 `docs/partner-api.md`。
- 所有 API 时间字段统一使用 ISO 8601（北京时间）。
- 浏览器端跨域访问需开启 `withCredentials` 并配置 CORS。

Base URL: `/api`

## Auth

### POST `/auth/register`
Request:
```json
{
  "username": "user01",
  "password": "secret123",
  "email": "user01@example.com",
  "role": "USER"
}
```
Response:
```json
{
  "data": {
    "id": 1,
    "username": "user01",
    "email": "user01@example.com",
    "role": "USER"
  }
}
```

### POST `/auth/login`
Request:
```json
{
  "username": "user01",
  "password": "secret123"
}
```
Response:
```json
{
  "data": {
    "id": 1,
    "username": "user01",
    "email": "user01@example.com",
    "role": "USER"
  }
}
```

### POST `/auth/logout`
Response:
```json
{ "data": "ok" }
```

## Merchant Profile

### GET `/merchant/profile`
Response:
```json
{
  "data": {
    "id": 10,
    "displayName": "Gym A",
    "description": "24H Gym",
    "sessionTimeoutMinutes": 240
  }
}
```

### PUT `/merchant/profile`
Request:
```json
{
  "displayName": "Gym A",
  "description": "24H Gym",
  "sessionTimeoutMinutes": 240
}
```
Response: same as GET

## Services

### GET `/services/public`
Response:
```json
{
  "data": [
    {
      "id": 100,
      "merchantId": 10,
      "name": "Gym booking",
      "category": "SPORTS",
      "durationMinutes": 60,
      "description": "Gym room",
      "active": true
    }
  ]
}
```

### GET `/services`
Merchant only. Response: same as public list.

### POST `/services`
Merchant only. Request:
```json
{
  "name": "Gym booking",
  "category": "SPORTS",
  "durationMinutes": 60,
  "description": "Gym room",
  "active": true
}
```
Response: service item

### PUT `/services/{id}`
Merchant only. Request: same as create.

## Bookings

### POST `/bookings`
User only.
Request:
```json
{
  "serviceId": 100,
  "startTime": "2026-02-01T10:00:00+08:00",
  "endTime": "2026-02-01T11:00:00+08:00"
}
```
Response:
```json
{
  "data": {
    "id": 200,
    "serviceId": 100,
    "userId": 1,
    "startTime": "2026-02-01T10:00:00+08:00",
    "endTime": "2026-02-01T11:00:00+08:00",
    "status": "CREATED"
  }
}
```

### PUT `/bookings/{id}`
User only. Request: same as create with new time.

### DELETE `/bookings/{id}`
User or admin.

### GET `/bookings`
User only. Response: list of bookings.

## Statistics

### GET `/stats/service/{serviceId}`
Merchant only.
Response:
```json
{ "data": { "total": 10, "canceled": 2 } }
```

### GET `/stats/merchant`
Merchant only.
Response:
```json
{ "data": { "services": 2, "orders": 40, "canceled": 3 } }
```

### GET `/stats/admin/summary`
Admin only.
Response:
```json
{ "data": { "merchants": 3, "services": 12 } }
```

## Appointment Tasks

### POST `/tasks`
Merchant only. Create a task with slots.
Request:
```json
{
  "title": "景点A",
  "category": "TRAVEL",
  "startTime": "2026-02-01T08:00:00+08:00",
  "endTime": "2026-02-02T20:00:00+08:00",
  "description": "全天可预约",
  "active": true,
  "slots": [
    {
      "title": "上午场",
      "startTime": "2026-02-01T08:00:00+08:00",
      "endTime": "2026-02-01T12:00:00+08:00",
      "capacity": 100,
      "location": "北门"
    }
  ]
}
```
Response:
```json
{
  "data": {
    "id": 1,
    "merchantId": 10,
    "title": "景点A",
    "category": "TRAVEL",
    "startTime": "2026-02-01T08:00:00+08:00",
    "endTime": "2026-02-02T20:00:00+08:00",
    "description": "全天可预约",
    "active": true,
    "slots": []
  }
}
```

### GET `/tasks/{taskId}`
Authenticated users. Query task by id.

### POST `/tasks/{taskId}/signed-link`
Merchant only. Generate a short-lived signed link.
Request params: `expiresInSeconds` (default 3600)

### GET `/tasks/public/signed/{signed}`
Public. Query task by signed link.

### POST `/tasks/public/signed/{signed}/bookings`
User only. Create booking for a slot.
Request:
```json
{ "slotId": 10 }
```

### GET `/tasks/bookings`
User only. List bookings.

### DELETE `/tasks/bookings/{id}`
User or admin. Cancel booking.

## Token Admin

Token 模式已停用。

## Appointment Stats

### GET `/stats/appointments/merchant`
Merchant only.

### GET `/stats/appointments/admin`
Admin only.

### GET `/stats/admin/tasks`
Admin only. List all merchants and their tasks.

## Errors

```json
{ "code": "VALIDATION_ERROR", "message": "field:reason" }
```
