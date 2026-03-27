# 智企连·AI-Ready 项目配置

## 本地开发配置

### 数据库配置

#### PostgreSQL (推荐)

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

### 启动 PostgreSQL 服务

```bash
# 启动 PostgreSQL
docker-compose -f docker/postgresql/docker-compose.yml up -d

# 验证连接
docker exec -it ai-ready-postgres psql -U devuser -d devdb -c "SELECT version();"
```

### 配置文件

#### application.yml (Spring Boot)

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

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 项目目录结构

```
I:\AI-Ready
├── pom.xml                         # 父 POM
├── README.md                       # 项目说明
├── core-base                       # 基础支撑模块
├── core-workflow                   # 工作流模块
├── core-report                     # 报表与打印模块
├── core-agent                      # Agent 调用层模块
├── core-api                        # API 接口定义
├── core-common                     # 公共工具类
├── erp                             # ERP 模块群
├── crm                             # CRM 模块群
├── smart-admin-web                 # 管理前端
├── erp-mobile                      # 移动端
├── docs                            # 文档目录
│   ├── database                    # 数据库文档
│   ├── development-guide.md        # 开发指南
│   └── api-reference.md            # API 参考
├── docker                          # Docker 配置
│   └── postgresql
│       └── docker-compose.yml
├── scripts                         # 脚本目录
└── .gitignore
```

## 环境变量

### 开发环境

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

### 生产环境

```bash
# PostgreSQL 配置
POSTGRES_HOST=prod-db.ai-ready.cn
POSTGRES_PORT=5432
POSTGRES_USER=produser
POSTGRES_PASSWORD=xxxxx
POSTGRES_DB=proddb

# Redis 配置
REDIS_HOST=prod-redis.ai-ready.cn
REDIS_PORT=6379
REDIS_PASSWORD=xxxxx

# 其他配置
APP_ENV=production
SERVER_PORT=8080
```

## 数据库初始化

### 创建数据库

```sql
-- 创建数据库
CREATE DATABASE devdb WITH OWNER = devuser;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
```

### 初始化表结构

```bash
# 执行 SQL 脚本
psql -h localhost -p 5432 -U devuser -d devdb -f scripts/init-schema.sql
```

## Git 配置

### 本地 Git 配置

```bash
# 配置用户信息
git config --global user.name "AI-Ready Bot"
git config --global user.email "bot@aiedge.cn"

# 配置代理（如需要）
git config --global http.proxy http://proxy.ai-ready.cn:8080

# 配置自动换行
git config --global core.autocrlf false
```

### 远程仓库

```bash
# 添加远程仓库
git remote add origin https://gitee.com/CozyNook/ai-ready.git
git remote set-url --add origin https://github.com/Get-windy/ai-ready.git

# 推送代码
git push origin main
```

## 验证配置

### 1. 检查 PostgreSQL 连接

```bash
# 使用 psql 测试
psql -h localhost -p 5432 -U devuser -d devdb -c "SELECT 1;"

# 使用 Docker 测试
docker exec ai-ready-postgres pg_isready -U devuser -d devdb
```

### 2. 检查数据库表

```bash
# 列出所有表
psql -h localhost -p 5432 -U devuser -d devdb -c "\dt"
```

### 3. 检查数据库大小

```bash
# 查看数据库大小
psql -h localhost -p 5432 -U devuser -d devdb -c "SELECT pg_size_pretty(pg_database_size('devdb'));"
```