# 智企连·AI-Ready PostgreSQL 开发配置

## 本地 PostgreSQL 配置

### 连接信息

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

### 数据库初始化脚本

```sql
-- 创建数据库
CREATE DATABASE devdb WITH OWNER = devuser;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 创建表空间（可选）
CREATE TABLESPACE ts_data LOCATION '/data/postgresql/data';
CREATE TABLESPACE ts_index LOCATION '/data/postgresql/index';
```

### Java 连接配置

```yaml
# application.yml
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

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## Docker PostgreSQL 配置

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgresql:
    image: postgres:14
    container_name: ai-ready-postgres
    restart: always
    environment:
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: Dev@2026#Local
      POSTGRES_DB: devdb
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devuser -d devdb"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:

networks:
  ai-ready-network:
    driver: bridge
```

### 启动命令

```bash
# 启动 PostgreSQL
docker-compose -f docker/postgresql/docker-compose.yml up -d

# 查看状态
docker-compose -f docker/postgresql/docker-compose.yml ps

# 查看日志
docker-compose -f docker/postgresql/docker-compose.yml logs -f

# 停止服务
docker-compose -f docker/postgresql/docker-compose.yml down
```

## 验证连接

### 使用 psql 测试

```bash
# 本地连接
psql -h localhost -p 5432 -U devuser -d devdb -W

# Docker 容器连接
docker exec -it ai-ready-postgres psql -U devuser -d devdb
```

### 使用 Java 测试

```java
@Test
public void testPostgresConnection() {
    String url = "jdbc:postgresql://localhost:5432/devdb?useSSL=false";
    String username = "devuser";
    String password = "Dev@2026#Local";
    
    try (Connection conn = DriverManager.getConnection(url, username, password)) {
        System.out.println("PostgreSQL 连接成功！");
        System.out.println("数据库版本: " + conn.getMetaData().getDatabaseProductName() + " " + 
                          conn.getMetaData().getDatabaseProductVersion());
    } catch (SQLException e) {
        System.err.println("连接失败: " + e.getMessage());
    }
}
```

## 常见问题

### 1. 连接被拒绝

```bash
# 检查 PostgreSQL 服务状态
sudo systemctl status postgresql

# 启动服务
sudo systemctl start postgresql

# 检查端口监听
netstat -an | grep 5432
```

### 2. 认证失败

检查 `pg_hba.conf` 配置：

```
# 允许本地连接
local   all             all                                     trust
host    all             all             127.0.0.1/32            md5
host    all             all             ::1/128                 md5
```

### 3. 数据库不存在

```sql
-- 创建数据库
CREATE DATABASE devdb;

-- 验证数据库
\l
```

## 性能优化建议

### 连接池配置

```yaml
# HikariCP 配置
spring.datasource.hikari:
  minimum-idle: 5                # 最小空闲连接数
  maximum-pool-size: 20          # 最大连接数
  connection-timeout: 30000      # 连接超时时间 (ms)
  idle-timeout: 600000           # 空闲连接超时 (ms)
  max-lifetime: 1800000          # 连接最大生存时间 (ms)
  connection-test-query: SELECT 1
```

### PostgreSQL 优化

```sql
-- 调整连接数
ALTER SYSTEM SET max_connections = 200;

-- 调整共享缓冲区
ALTER SYSTEM SET shared_buffers = 256MB;

# 重启 PostgreSQL
SELECT pg_reload_conf();
```

## 监控指标

```sql
-- 查看连接数
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- 查看数据库大小
SELECT pg_size_pretty(pg_database_size('devdb'));

-- 查看表大小
SELECT relname, pg_size_pretty(pg_relation_size(relid)) 
FROM pg_stat_user_tables 
ORDER BY pg_relation_size(relid) DESC;

-- 查看慢查询
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;
```