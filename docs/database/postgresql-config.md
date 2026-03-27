# PostgreSQL 开发环境配置

## 本地 PostgreSQL 连接信息

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

## 数据库初始化

### 创建数据库

```sql
CREATE DATABASE devdb WITH OWNER = devuser;
```

### 创建扩展

```sql
-- PostgreSQL 扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS ".postgis";
```

### 表空间配置

```sql
-- 创建表空间（可选）
CREATE TABLESPACE ts_data LOCATION '/data/postgresql/data';
CREATE TABLESPACE ts_index LOCATION '/data/postgresql/index';
```

## 连接配置

### JDBC 配置 (Spring Boot)

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
```

### MyBatis-Plus 配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: input
      table-underline: true
```

## 开发建议

### SQL 格式化

- 使用小写关键字
- 表名使用复数形式
- 字段名使用下划线命名
- 主键命名为 `id`
- 创建时间命名为 `create_time`
- 修改时间命名为 `update_time`

### 索引优化

- 主键自动创建唯一索引
- 外键字段创建索引
- 查询条件频繁使用的字段创建索引
- 复合索引遵循最左前缀原则

### 数据类型选择

- 整数: `integer` (4字节)
- 大整数: `bigint` (8字节)
- 字符串: `varchar(n)` 或 `text`
- 日期时间: `timestamp without time zone`
- 布尔: `boolean`
- JSON: `jsonb`

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

## 故障排查

### 常见错误

1. **Connection Refused**
   - 检查 PostgreSQL 服务是否启动
   - 检查端口 5432 是否被占用
   - 检查防火墙设置

2. **Authentication Failed**
   - 检查用户名密码是否正确
   - 检查 pg_hba.conf 配置
   - 临时解决方案：切换到 trust 认证

3. **Database Does Not Exist**
   - 创建数据库: `CREATE DATABASE devdb;`
   - 检查数据库是否存在: `\l`

### 日志查看

```bash
# PostgreSQL 日志位置
/var/log/postgresql/

# 查看日志
tail -f /var/log/postgresql/postgresql-*.log
```

### 连接测试

```bash
# 使用 psql 测试连接
psql -h localhost -p 5432 -U devuser -d devdb -W

# 使用 telnet 测试端口
telnet localhost 5432

# 使用 netstat 查看端口监听
netstat -an | grep 5432
```