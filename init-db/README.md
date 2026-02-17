# 数据库初始化脚本

这个目录包含 MySQL 容器启动时自动执行的初始化脚本。

## 用途

Docker Compose 配置中，MySQL 容器会自动执行此目录下的 `.sql`、`.sh` 和 `.sql.gz` 文件。

**注意**: 本项目使用 Flyway 进行数据库迁移管理，因此此目录通常为空。

## Flyway vs Init Scripts

- **Flyway**（推荐）: 用于版本化的数据库迁移，位于 `src/main/resources/db/migration/`
- **Init Scripts**（可选）: 用于容器首次启动时的初始化，位于此目录

## 何时使用 Init Scripts

仅在以下场景中使用此目录：

1. 创建 Flyway 运行所需的额外数据库或用户
2. 执行 Flyway 之前的系统级配置
3. 设置时区或字符集等全局配置

## 示例脚本

如果需要使用初始化脚本，可以创建如下文件：

```sql
-- 00-init-users.sql
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'app123';
GRANT ALL PRIVILEGES ON yuyue.* TO 'app_user'@'%';
FLUSH PRIVILEGES;
```

脚本会按照文件名的字母顺序执行。
