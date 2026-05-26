# 测试环境配置指南

## 概述
本文档描述如何配置Sprint 27+1测试环境，用于执行用户管理模块的功能测试。

## 环境要求

### 硬件要求
- CPU: 4核以上
- 内存: 8GB以上
- 磁盘空间: 20GB以上

### 软件要求
- 操作系统: Windows 10/11, Linux, macOS
- Java: JDK 17+
- Maven: 3.8+
- PostgreSQL: 14+
- Docker: 20.10+ (可选，用于容器化部署)
- Python: 3.8+ (用于自动化测试脚本)

## 数据库配置

### PostgreSQL数据库配置

1. **创建测试数据库**
```sql
-- 创建测试数据库
CREATE DATABASE ai_ready_test;

-- 创建测试用户
CREATE USER test_user WITH PASSWORD 'TestPassword123';

-- 授予权限
GRANT ALL PRIVILEGES ON DATABASE ai_ready_test TO test_user;
```

2. **数据库连接配置**
在`application-test.properties`中配置：
```properties
# 数据库配置
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_ready_test
spring.datasource.username=test_user
spring.datasource.password=TestPassword123

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# 数据库连接池
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

## 服务配置

### 用户管理服务配置

1. **应用配置文件** (`application-test.yml`)
```yaml
server:
  port: 8080
  servlet:
    context-path: /
  
spring:
  application:
    name: user-management-service
  
  # 数据库配置（见上文）
  
  # Redis配置（用于会话管理）
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
  
  # JWT配置
  jwt:
    secret: test-jwt-secret-key-for-sprint-27+1
    expiration: 86400000  # 24小时
  
  # 安全配置
  security:
    user:
      name: admin
      password: admin123
  
# 日志配置
logging:
  level:
    cn.aiedge.user: DEBUG
    org.springframework.security: INFO
  file:
    name: logs/user-management-test.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Swagger配置
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha

# 测试数据配置
test:
  data:
    users:
      count: 100
      prefix: test_user_
    cleanup:
      enabled: true
      interval: 3600000  # 1小时
```

2. **启动脚本** (`start-test.sh` 或 `start-test.bat`)
```bash
#!/bin/bash
# start-test.sh - 启动测试环境

echo "正在启动用户管理模块测试环境..."

# 检查Java
if ! command -v java &> /dev/null; then
    echo "错误: Java未安装"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装"
    exit 1
fi

# 构建应用
echo "构建应用..."
cd backend/user
mvn clean package -DskipTests

# 启动应用
echo "启动应用..."
java -jar target/user-management-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=test \
  --server.port=8080 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/ai_ready_test \
  --spring.datasource.username=test_user \
  --spring.datasource.password=TestPassword123

# Windows批处理文件 (start-test.bat)
@echo off
echo 正在启动用户管理模块测试环境...

REM 检查Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo 错误: Java未安装
    exit /b 1
)

REM 检查Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo 错误: Maven未安装
    exit /b 1
)

REM 构建应用
echo 构建应用...
cd backend\user
call mvn clean package -DskipTests

REM 启动应用
echo 启动应用...
java -jar target\user-management-1.0.0-SNAPSHOT.jar ^
  --spring.profiles.active=test ^
  --server.port=8080 ^
  --spring.datasource.url=jdbc:postgresql://localhost:5432\ai_ready_test ^
  --spring.datasource.username=test_user ^
  --spring.datasource.password=TestPassword123
```

## 测试数据准备

### 基础测试数据

1. **创建测试用户脚本** (`create-test-users.sql`)
```sql
-- 清理现有测试数据
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE permissions CASCADE;

-- 创建角色
INSERT INTO roles (id, name, description, created_at) VALUES
(1, 'ADMIN', '系统管理员', NOW()),
(2, 'USER', '普通用户', NOW()),
(3, 'TESTER', '测试人员', NOW());

-- 创建权限
INSERT INTO permissions (id, name, description, created_at) VALUES
(1, 'USER_CREATE', '创建用户', NOW()),
(2, 'USER_READ', '查看用户', NOW()),
(3, 'USER_UPDATE', '更新用户', NOW()),
(4, 'USER_DELETE', '删除用户', NOW()),
(5, 'ROLE_MANAGE', '管理角色', NOW());

-- 关联角色和权限
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),  -- ADMIN有所有权限
(2, 2),  -- USER只能查看
(3, 1), (3, 2), (3, 3);  -- TESTER可以创建、查看、更新

-- 创建测试用户
INSERT INTO users (id, username, password, email, full_name, phone, status, created_at) VALUES
(1, 'admin', '$2a$10$YourHashedPasswordHere', 'admin@example.com', '系统管理员', '13800138000', 'ACTIVE', NOW()),
(2, 'test_user_1', '$2a$10$YourHashedPasswordHere', 'test1@example.com', '测试用户1', '13800138001', 'ACTIVE', NOW()),
(3, 'test_user_2', '$2a$10$YourHashedPasswordHere', 'test2@example.com', '测试用户2', '13800138002', 'ACTIVE', NOW()),
(4, 'disabled_user', '$2a$10$YourHashedPasswordHere', 'disabled@example.com', '禁用用户', '13800138003', 'DISABLED', NOW()),
(5, 'locked_user', '$2a$10$YourHashedPasswordHere', 'locked@example.com', '锁定用户', '13800138004', 'LOCKED', NOW());

-- 关联用户和角色
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),  -- admin是ADMIN
(2, 2),  -- test_user_1是USER
(3, 3),  -- test_user_2是TESTER
(4, 2),  -- disabled_user是USER
(5, 2);  -- locked_user是USER
```

### 自动化测试数据生成脚本

创建Python脚本自动生成测试数据：

```python
# generate-test-data.py
import json
import random
from datetime import datetime

def generate_users(count=100):
    """生成测试用户数据"""
    users = []
    for i in range(1, count + 1):
        user = {
            "id": i,
            "username": f"test_user_{i:03d}",
            "password": "Test@12345",
            "email": f"test{i:03d}@example.com",
            "fullName": f"测试用户{i:03d}",
            "phone": f"138{random.randint(10000000, 99999999):08d}",
            "status": random.choice(["ACTIVE", "INACTIVE", "DISABLED"]),
            "createdAt": datetime.now().isoformat()
        }
        users.append(user)
    return users

if __name__ == "__main__":
    users = generate_users(100)
    
    # 保存为JSON
    with open('test-data/users.json', 'w', encoding='utf-8') as f:
        json.dump(users, f, ensure_ascii=False, indent=2)
    
    # 保存为SQL
    with open('test-data/users.sql', 'w', encoding='utf-8') as f:
        f.write("INSERT INTO users (id, username, password, email, full_name, phone, status, created_at) VALUES\n")
        for i, user in enumerate(users):
            values = f"({user['id']}, '{user['username']}', '$2a$10$YourHashedPasswordHere', '{user['email']}', '{user['fullName']}', '{user['phone']}', '{user['status']}', NOW())"
            if i < len(users) - 1:
                values += ",\n"
            else:
                values += ";\n"
            f.write(values)
    
    print(f"已生成 {len(users)} 个测试用户")
```

## 监控和日志

### 应用监控配置

1. **Spring Boot Actuator配置**
```properties
# Actuator配置
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true

# 自定义健康检查
management.health.redis.enabled=true
management.health.db.enabled=true
```

2. **日志监控脚本** (`monitor-logs.sh`)
```bash
#!/bin/bash
# monitor-logs.sh - 监控应用日志

LOG_FILE="logs/user-management-test.log"

echo "监控日志文件: $LOG_FILE"
echo "按Ctrl+C停止监控"
echo ""

tail -f "$LOG_FILE" | grep -E "(ERROR|WARN|INFO|DEBUG)"
```

### 性能监控

1. **JMeter测试计划** (`performance-test.jmx`)
- 配置并发用户数: 100
- 测试持续时间: 10分钟
- 测试场景: 用户注册、登录、查询

2. **监控指标**
- 响应时间: < 200ms (P95)
- 错误率: < 1%
- 吞吐量: > 100 req/sec

## 环境验证

### 验证脚本 (`validate-environment.py`)
```python
import requests
import time

def validate_environment(base_url="http://localhost:8080"):
    """验证测试环境"""
    print("验证测试环境...")
    
    tests = [
        ("健康检查", f"{base_url}/actuator/health", 200),
        ("API文档", f"{base_url}/swagger-ui.html", 200),
        ("用户API", f"{base_url}/api/v1/users", 200),
    ]
    
    all_passed = True
    for name, url, expected_status in tests:
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == expected_status:
                print(f"✓ {name}: 通过")
            else:
                print(f"✗ {name}: 失败 (状态码: {response.status_code})")
                all_passed = False
        except Exception as e:
            print(f"✗ {name}: 失败 ({str(e)})")
            all_passed = False
    
    return all_passed

if __name__ == "__main__":
    if validate_environment():
        print("\n环境验证通过!")
        exit(0)
    else:
        print("\n环境验证失败!")
        exit(1)
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查PostgreSQL服务是否运行
   - 验证数据库连接参数
   - 检查防火墙设置

2. **应用启动失败**
   - 检查端口8080是否被占用
   - 查看应用日志
   - 验证Java版本

3. **测试失败**
   - 检查测试数据是否正确
   - 验证API端点
   - 查看响应日志

### 调试命令

```bash
# 检查端口占用
netstat -ano | findstr :8080

# 检查Java进程
jps -l

# 查看应用日志
tail -f logs/user-management-test.log

# 测试数据库连接
psql -h localhost -U test_user -d ai_ready_test
```

## 清理脚本

### 环境清理 (`cleanup-environment.sh`)
```bash
#!/bin/bash
# cleanup-environment.sh - 清理测试环境

echo "清理测试环境..."

# 停止应用
pkill -f "user-management.*jar"

# 清理日志
rm -f logs/*.log

# 清理数据库
psql -h localhost -U test_user -d ai_ready_test << EOF
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE permissions CASCADE;
EOF

echo "清理完成"
```

---

## 快速开始

1. **初始化环境**
```bash
# 克隆代码
git clone <repository-url>
cd ai-ready

# 配置数据库
psql -U postgres -f scripts/database-init.sql

# 启动服务
cd backend/user
mvn spring-boot:run -Dspring.profiles.active=test
```

2. **运行测试**
```bash
# 运行自动化测试
python qa/scripts/user-management/api-test-automation.py

# 运行性能测试
jmeter -n -t qa/scripts/user-management/performance-test.jmx
```

3. **查看报告**
- 测试报告: `test-report.html`
- 性能报告: `performance-report.html`
- 日志文件: `logs/user-management-test.log`