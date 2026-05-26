# 用户管理模块测试环境部署指南

## 概述
本文档描述了如何在Sprint 27+1测试环境中部署用户管理模块。用户管理模块是AI-Ready系统的核心模块，提供用户认证、用户管理、角色管理和权限管理等功能。

## 部署环境要求

### 硬件要求
- **CPU**: 4核心或以上
- **内存**: 8GB或以上
- **磁盘**: 50GB可用空间
- **网络**: 100Mbps带宽

### 软件要求
- **操作系统**: Ubuntu 20.04 LTS / CentOS 8 / Windows Server 2019
- **Docker**: 20.10.0或以上
- **Docker Compose**: 2.0.0或以上
- **Java**: JDK 17或以上
- **Node.js**: 16.0.0或以上（前端部署）
- **数据库**: PostgreSQL 14.0或以上
- **缓存**: Redis 6.0或以上

### 网络要求
- **端口**: 8080 (应用), 5432 (数据库), 6379 (Redis), 9090 (监控)
- **域名**: test-ai-ready.example.com
- **SSL证书**: 需要有效的SSL证书

## 部署架构

### 架构图
```
┌─────────────────────────────────────────────────────────┐
│                   负载均衡器 (Nginx)                    │
│                    test-ai-ready.example.com            │
└──────────────────────────┬──────────────────────────────┘
                           │
                 ┌─────────┼─────────┐
                 │         │         │
        ┌────────▼─┐ ┌────▼────┐ ┌──▼────────┐
        │ 应用实例1 │ │应用实例2│ │ 应用实例3 │
        │ :8080    │ │ :8081   │ │ :8082     │
        └─────┬────┘ └────┬────┘ └────┬──────┘
              │           │           │
        ┌─────┴───────────┴───────────┴──────┐
        │           PostgreSQL数据库          │
        │              :5432                  │
        └──────────────────┬──────────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │              Redis缓存              │
        │              :6379                  │
        └─────────────────────────────────────┘
```

### 组件说明
1. **Nginx**: 反向代理和负载均衡
2. **应用实例**: 用户管理模块的Spring Boot应用
3. **PostgreSQL**: 主要数据存储
4. **Redis**: 会话缓存和权限缓存

## 部署步骤

### 步骤1: 环境准备

#### 1.1 安装Docker和Docker Compose
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install docker.io docker-compose -y

# CentOS/RHEL
sudo yum install docker docker-compose -y
sudo systemctl start docker
sudo systemctl enable docker
```

#### 1.2 创建部署目录
```bash
mkdir -p /opt/ai-ready/user-management
cd /opt/ai-ready/user-management
```

#### 1.3 创建环境变量文件
```bash
cat > .env << 'EOF'
# 应用配置
APP_NAME=user-management
APP_VERSION=1.0.0
APP_PORT=8080
APP_PROFILE=test

# 数据库配置
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ai_ready_test
DB_USER=ai_ready_user
DB_PASSWORD=SecurePassword123!

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=RedisPassword123!

# JWT配置
JWT_SECRET=YourJwtSecretKeyForTestingEnvironment1234567890
JWT_EXPIRATION=86400

# 邮件配置（测试环境）
SMTP_HOST=smtp.test.com
SMTP_PORT=587
SMTP_USERNAME=test@example.com
SMTP_PASSWORD=TestPassword123!
SMTP_FROM=noreply@test-ai-ready.example.com
EOF
```

### 步骤2: 数据库部署

#### 2.1 创建数据库初始化脚本
```bash
cat > init-db.sql << 'EOF'
-- 创建数据库（如果不存在）
SELECT 'CREATE DATABASE ai_ready_test'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ai_ready_test')\gexec

-- 切换到新数据库
\c ai_ready_test

-- 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    gender INT DEFAULT 0,
    status INT DEFAULT 1,
    dept_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50)
);

-- 创建角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    type INT NOT NULL,
    path VARCHAR(500),
    method VARCHAR(20),
    description VARCHAR(500),
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- 创建角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- 创建索引
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_phone ON sys_user(phone);
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_role_code ON sys_role(code);
CREATE INDEX idx_permission_code ON sys_permission(code);
CREATE INDEX idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_user_role_role ON sys_user_role(role_id);
CREATE INDEX idx_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX idx_role_permission_permission ON sys_role_permission(permission_id);

-- 插入默认数据
INSERT INTO sys_user (username, password, real_name, email, phone, status, gender) 
VALUES ('admin', '$2a$10$Egp1/gvFlt7zhlXVfEFl4Ou2XrZ8v8z5q5p5q5p5q5p5q5p5q5p5q5', '管理员', 'admin@test-ai-ready.example.com', '13800138000', 1, 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_role (name, code, description, status) 
VALUES 
    ('管理员', 'admin', '系统管理员角色', 1),
    ('用户', 'user', '普通用户角色', 1),
    ('访客', 'guest', '访客角色', 1)
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_permission (name, code, type, path, method, description, status) 
VALUES 
    ('用户管理', 'user:manage', 3, '/api/user/**', '*', '用户管理权限', 1),
    ('角色管理', 'role:manage', 3, '/api/role/**', '*', '角色管理权限', 1),
    ('权限管理', 'permission:manage', 3, '/api/permission/**', '*', '权限管理权限', 1),
    ('登录', 'auth:login', 3, '/api/auth/login', 'POST', '登录权限', 1),
    ('登出', 'auth:logout', 3, '/api/auth/logout', 'POST', '登出权限', 1),
    ('获取用户信息', 'auth:user-info', 3, '/api/auth/user-info', 'GET', '获取用户信息权限', 1)
ON CONFLICT (code) DO NOTHING;

-- 分配管理员角色
INSERT INTO sys_user_role (user_id, role_id) 
SELECT u.id, r.id FROM sys_user u, sys_role r 
WHERE u.username = 'admin' AND r.code = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 为管理员角色分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.code = 'admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 为用户角色分配基本权限
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.code = 'user' AND p.code IN ('auth:login', 'auth:logout', 'auth:user-info')
ON CONFLICT (role_id, permission_id) DO NOTHING;
EOF
```

#### 2.2 创建Docker Compose文件
```bash
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  # PostgreSQL数据库
  postgres:
    image: postgres:14-alpine
    container_name: ai-ready-postgres
    restart: always
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis缓存
  redis:
    image: redis:6-alpine
    container_name: ai-ready-redis
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # 用户管理应用
  user-management-app:
    image: ai-ready/user-management:${APP_VERSION}
    container_name: ai-ready-user-management
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${APP_PROFILE}
      SPRING_DATASOURCE_URL: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: ${REDIS_HOST}
      SPRING_REDIS_PORT: ${REDIS_PORT}
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
    ports:
      - "${APP_PORT}:8080"
    volumes:
      - app_logs:/app/logs
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/auth/check-username?username=admin"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Nginx反向代理
  nginx:
    image: nginx:1.21-alpine
    container_name: ai-ready-nginx
    restart: always
    depends_on:
      - user-management-app
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    networks:
      - ai-ready-network

volumes:
  postgres_data:
  redis_data:
  app_logs:

networks:
  ai-ready-network:
    driver: bridge
EOF
```

#### 2.3 创建Nginx配置
```bash
cat > nginx.conf << 'EOF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/atom+xml image/svg+xml;

    # 上游服务配置
    upstream user_management {
        least_conn;
        server user-management-app:8080 max_fails=3 fail_timeout=30s;
    }

    # HTTP服务器（重定向到HTTPS）
    server {
        listen 80;
        server_name test-ai-ready.example.com;
        
        # 重定向到HTTPS
        return 301 https://$server_name$request_uri;
    }

    # HTTPS服务器
    server {
        listen 443 ssl http2;
        server_name test-ai-ready.example.com;

        # SSL证书配置
        ssl_certificate /etc/nginx/ssl/test-ai-ready.example.com.crt;
        ssl_certificate_key /etc/nginx/ssl/test-ai-ready.example.com.key;
        
        # SSL协议配置
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;
        ssl_session_timeout 1d;
        ssl_session_cache shared:SSL:50m;
        ssl_session_tickets off;

        # 安全头
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

        # 静态文件缓存
        location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # API路由
        location /api/ {
            proxy_pass http://user_management;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # 超时设置
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
            
            # 缓存设置
            proxy_buffering on;
            proxy_buffer_size 4k;
            proxy_buffers 8 4k;
            proxy_busy_buffers_size 8k;
        }

        # Swagger文档
        location /swagger-ui/ {
            proxy_pass http://user_management;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 健康检查
        location /health {
            proxy_pass http://user_management/actuator/health;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            access_log off;
        }

        # 根路径重定向到Swagger
        location = / {
            return 302 /swagger-ui/index.html;
        }

        # 错误页面
        error_page 404 /404.html;
        location = /404.html {
            internal;
        }
        
        error_page 500 502 503 504 /50x.html;
        location = /50x.html {
            internal;
        }
    }
}
EOF
```

### 步骤3: 应用部署

#### 3.1 构建应用镜像
```bash
cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制JAR文件
COPY target/user-management-*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建非root用户
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
```

#### 3.2 创建部署脚本
```bash
cat > deploy.sh << 'EOF'
#!/bin/bash

set -e

echo "=========================================="
echo "开始部署用户管理模块到测试环境"
echo "=========================================="

# 加载环境变量
if [ -f .env ]; then
    source .env
    echo "环境变量加载成功"
else
    echo "错误: .env文件不存在"
    exit 1
fi

# 检查Docker和Docker Compose
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装"
    exit 1
fi

echo "Docker版本: $(docker --version)"
echo "Docker Compose版本: $(docker-compose --version)"

# 创建SSL证书目录
mkdir -p ssl

# 检查SSL证书（测试环境可以使用自签名证书）
if [ ! -f "ssl/test-ai-ready.example.com.crt" ] || [ ! -f "ssl/test-ai-ready.example.com.key" ]; then
    echo "生成自签名SSL证书..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ssl/test-ai-ready.example.com.key \
        -out ssl/test-ai-ready.example.com.crt \
        -subj "/C=CN/ST=Beijing/L=Beijing/O=AI-Ready/CN=test-ai-ready.example.com"
    echo "SSL证书生成完成"
fi

# 停止并删除旧容器
echo "停止并删除旧容器..."
docker-compose down --remove-orphans

# 清理旧镜像
echo "清理旧镜像..."
docker image prune -f

# 构建新镜像
echo "构建应用镜像..."
docker build -t ai-ready/user-management:${APP_VERSION} .

# 启动服务
echo "启动服务..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 检查服务状态
echo "检查服务状态..."
for i in {1..10}; do
    if curl -f http://localhost:${APP_PORT}/actuator/health > /dev/null 2>&1; then
        echo "应用服务启动成功"
        break
    fi
    
    if [ $i -eq 10 ]; then
        echo "错误: 应用服务启动失败"
        docker-compose logs user-management-app
        exit 1
    fi
    
    echo "等待应用服务启动... ($i/10)"
    sleep 10
done

# 检查数据库连接
echo "检查数据库连接..."
docker-compose exec -T postgres pg_isready -U ${DB_USER} -d ${DB_NAME}

# 检查Redis连接
echo "检查Redis连接..."
docker-compose exec -T redis redis-cli -a ${REDIS_PASSWORD} ping

# 检查Nginx状态
echo "检查Nginx状态..."
if curl -f http://localhost/health > /dev/null 2>&1; then
    echo "Nginx服务启动成功"
else
    echo "警告: Nginx服务可能未启动"
fi

echo "=========================================="
echo "用户管理模块部署完成"
echo "=========================================="
echo ""
echo "访问地址: https://test-ai-ready.example.com"
echo "Swagger文档: https://test-ai-ready.example.com/swagger-ui/index.html"
echo "默认管理员账号: admin / admin123"
echo ""
echo "服务状态:"
echo "1. PostgreSQL数据库: localhost:5432"
echo "2. Redis缓存: localhost:6379"
echo "3. 用户管理应用: localhost:${APP_PORT}"
echo "4. Nginx代理: localhost:80/443"
echo ""
echo "查看日志:"
echo "  docker-compose logs -f user-management-app"
echo "停止服务:"
echo "  docker-compose down"
echo "重启服务:"
echo "  docker-compose restart"
echo ""
echo "=========================================="
EOF

chmod +x deploy.sh
```

#### 3.3 创建健康检查脚本
```bash
cat > health-check.sh << 'EOF'
#!/bin/bash

set -e

echo "=========================================="
echo "用户管理模块健康检查"
echo "=========================================="

# 加载环境变量
if [ -f .env ]; then
    source .env
else
    echo "错误: .env文件不存在"
    exit 1
fi

# 检查容器状态
echo "1. 检查容器状态..."
docker-compose ps

# 检查应用健康状态
echo ""
echo "2. 检查应用健康状态..."
if curl -f http://localhost:${APP_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ 应用健康状态: UP"
else
    echo "❌ 应用健康状态: DOWN"
    exit 1
fi

# 检查数据库连接
echo ""
echo "3. 检查数据库连接..."
if docker-compose exec -T postgres pg_isready -U ${DB_USER} -d ${DB_NAME} > /dev/null 2>&1; then
    echo "✅ 数据库连接: OK"
else
    echo "❌ 数据库连接: FAILED"
    exit 1
fi

# 检查Redis连接
echo ""
echo "4. 检查Redis连接..."
if docker-compose exec -T redis redis-cli -a ${REDIS_PASSWORD} ping > /dev/null 2>&1; then
    echo "✅ Redis连接: OK"
else
    echo "❌ Redis连接: FAILED"
    exit 1
fi

# 检查API接口
echo ""
echo "5. 检查API接口..."
if curl -f http://localhost:${APP_PORT}/api/auth/check-username?username=admin > /dev/null 2>&1; then
    echo "✅ API接口: OK"
else
    echo "❌ API接口: FAILED"
    exit 1
fi

# 检查Nginx代理
echo ""
echo "6. 检查Nginx代理..."
if curl -f https://test-ai-ready.example.com/health > /dev/null 2>&1; then
    echo "✅ Nginx代理: OK"
else
    echo "⚠️  Nginx代理: 可能未配置域名解析"
    echo "   可以使用本地测试: curl -f http://localhost/health"
fi

# 检查磁盘空间
echo ""
echo "7. 检查磁盘空间..."
df -h / | tail -1

# 检查内存使用
echo ""
echo "8. 检查内存使用..."
free -h

echo ""
echo "=========================================="
echo "健康检查完成"
echo "=========================================="
EOF

chmod +x health-check.sh
```

### 步骤4: 监控和日志配置

#### 4.1 创建监控配置
```bash
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'user-management'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-management-app:8080']
        labels:
          application: 'user-management'
          environment: 'test'
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
        labels:
          application: 'postgres'
          environment: 'test'
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
        labels:
          application: 'redis'
          environment: 'test'
  
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
        labels:
          application: 'node'
          environment: 'test'
EOF
```

#### 4.2 创建日志配置
```bash
cat > logback-spring.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="/app/logs"/>
    <property name="LOG_FILE" value="user-management"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 错误日志单独输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>500MB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 访问日志 -->
    <appender name="ACCESS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}-access.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %m%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-access.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- SQL日志 -->
    <appender name="SQL_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}-sql.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-sql.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 日志级别配置 -->
    <logger name="org.springframework" level="INFO"/>
    <logger name="com.zaxxer.hikari" level="INFO"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="cn.aiedge" level="DEBUG"/>
    
    <!-- SQL日志 -->
    <logger name="org.hibernate.SQL" level="DEBUG" additivity="false">
        <appender-ref ref="SQL_FILE"/>
    </logger>
    
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE" additivity="false">
        <appender-ref ref="SQL_FILE"/>
    </logger>

    <!-- 访问日志 -->
    <logger name="ACCESS" level="INFO" additivity="false">
        <appender-ref ref="ACCESS_FILE"/>
    </logger>

    <!-- 根日志 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
</configuration>
EOF
```

### 步骤5: 备份和恢复

#### 5.1 创建备份脚本
```bash
cat > backup.sh << 'EOF'
#!/bin/bash

set -e

echo "=========================================="
echo "用户管理模块数据备份"
echo "=========================================="

# 加载环境变量
if [ -f .env ]; then
    source .env
else
    echo "错误: .env文件不存在"
    exit 1
fi

# 创建备份目录
BACKUP_DIR="/opt/ai-ready/backups"
mkdir -p ${BACKUP_DIR}

# 生成备份文件名
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/user-management-backup-${TIMESTAMP}.sql"

echo "开始备份数据库..."
docker-compose exec -T postgres pg_dump -U ${DB_USER} -d ${DB_NAME} > ${BACKUP_FILE}

# 压缩备份文件
gzip ${BACKUP_FILE}
BACKUP_FILE_GZ="${BACKUP_FILE}.gz"

echo "备份完成: ${BACKUP_FILE_GZ}"
echo "备份大小: $(du -h ${BACKUP_FILE_GZ} | cut -f1)"

# 保留最近7天的备份
echo "清理旧备份..."
find ${BACKUP_DIR} -name "user-management-backup-*.sql.gz" -mtime +7 -delete

echo "=========================================="
echo "备份完成"
echo "=========================================="
EOF

chmod +x backup.sh
```

#### 5.2 创建恢复脚本
```bash
cat > restore.sh << 'EOF'
#!/bin/bash

set -e

echo "=========================================="
echo "用户管理模块数据恢复"
echo "=========================================="

# 加载环境变量
if [ -f .env ]; then
    source .env
else
    echo "错误: .env文件不存在"
    exit 1
fi

# 检查备份文件
BACKUP_DIR="/opt/ai-ready/backups"
if [ $# -eq 0 ]; then
    # 显示可用的备份文件
    echo "可用备份文件:"
    ls -lh ${BACKUP_DIR}/user-management-backup-*.sql.gz 2>/dev/null | sort -r || echo "无备份文件"
    echo ""
    echo "用法: $0 <备份文件>"
    exit 1
fi

BACKUP_FILE=$1
if [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

# 确认恢复
read -p "确定要恢复备份吗？这将覆盖现有数据！(y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "恢复已取消"
    exit 0
fi

echo "停止应用服务..."
docker-compose stop user-management-app

echo "恢复数据库..."
gunzip -c ${BACKUP_FILE} | docker-compose exec -T postgres psql -U ${DB_USER} -d ${DB_NAME}

echo "启动应用服务..."
docker-compose start user-management-app

echo "等待应用启动..."
sleep 30

echo "检查应用状态..."
if curl -f http://localhost:${APP_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ 恢复完成，应用运行正常"
else
    echo "❌ 恢复完成，但应用启动失败"
    echo "查看日志: docker-compose logs user-management-app"
    exit 1
fi

echo "=========================================="
echo "数据恢复完成"
echo "=========================================="
EOF

chmod +x restore.sh
```

## 部署验证

### 1. 执行部署
```bash
# 1. 进入部署目录
cd /opt/ai-ready/user-management

# 2. 执行部署
./deploy.sh
```

### 2. 验证部署
```bash
# 执行健康检查
./health-check.sh

# 测试API接口
curl -X POST "https://test-ai-ready.example.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 测试用户管理接口
curl -X GET "https://test-ai-ready.example.com/api/user/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"
```

### 3. 监控部署状态
```bash
# 查看容器状态
docker-compose ps

# 查看应用日志
docker-compose logs -f user-management-app

# 查看数据库