# 智企连·AI-Ready 项目配置

## PostgreSQL 数据库配置（本地开发）

### 连接信息

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

### PostgreSQL 服务启动

```bash
# 使用 Docker 启动 PostgreSQL
docker-compose -f docker/postgresql/docker-compose.yml up -d

# 验证连接
docker exec -it ai-ready-postgres psql -U devuser -d devdb -c "SELECT 1;"
```

### Spring Boot 配置

#### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: devuser
    password: Dev@2026#Local
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

### MyBatis-Plus 配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 环境变量

### 开发环境 (.env)

```bash
# PostgreSQL 配置
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=devuser
POSTGRES_PASSWORD=Dev@2026#Local
POSTGRES_DB=devdb

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 其他配置
APP_ENV=development
SERVER_PORT=8080
```

## 快速开始

### 1. 启动 PostgreSQL

```bash
docker-compose -f docker/postgresql/docker-compose.yml up -d
```

### 2. 初始化数据库

```bash
# 连接到 PostgreSQL
docker exec -it ai-ready-postgres psql -U devuser -d devdb

# 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
```

### 3. 运行应用

```bash
# Maven 构建
mvn clean install

# 运行应用
mvn spring-boot:run
```

## 数据库初始化脚本

### 初始化表结构

```bash
psql -h localhost -p 5432 -U devuser -d devdb -f scripts/init-schema.sql
```

### 初始化数据

```bash
psql -h localhost -p 5432 -U devuser -d devdb -f scripts/init-data.sql
```

## 常见问题

### 1. 连接被拒绝

```bash
# 检查 PostgreSQL 是否运行
docker ps | grep postgresql

# 查看日志
docker logs ai-ready-postgres
```

### 2. 认证失败

检查 `pg_hba.conf` 配置，确保允许 md5 认证。

### 3. 数据库不存在

```sql
CREATE DATABASE devdb;
```

## 监控

```sql
-- 查看连接数
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- 查看数据库大小
SELECT pg_size_pretty(pg_database_size('devdb'));

-- 查看表大小
SELECT relname, pg_size_pretty(pg_relation_size(relid)) 
FROM pg_stat_user_tables ORDER BY pg_relation_size(relid) DESC;
```

---
最后更新: 2026-03-27 11:56