# 智企连·AI-Ready 环境搭建指南

**版本**: v1.0  
**日期**: 2026-03-27  
**作者**: devops-engineer  
**项目**: 智企连·AI-Ready

---

## 执行摘要

本文档提供智企连·AI-Ready 项目的完整开发环境搭建指南，包括 Java 17、Node.js、PostgreSQL、Redis 等基础环境的配置，以及项目初始化步骤。

---

## 1. 环境检查结果

### 1.1 当前系统环境

| 组件 | 状态 | 版本 | 说明 |
|------|------|------|------|
| Java JDK | ✅ 已安装 | 17.0.10 | 满足项目要求 |
| Maven | ✅ 已安装 | 3.9.6 | 满足项目要求 |
| Node.js | ✅ 已安装 | v24.11.1 | 满足项目要求 |
| npm | ✅ 已安装 | 11.8.0 | 满足项目要求 |
| PostgreSQL | ⚠️ 需安装 | - | 可通过Docker或本地安装 |
| Redis | ⚠️ 需安装 | - | 可通过Docker或本地安装 |
| Docker | ⚠️ 可选 | - | 推荐用于数据库服务 |

### 1.2 环境变量检查

```powershell
# 检查Java环境
java -version
# 输出: java version "17.0.10" 2024-01-16 LTS

# 检查Maven环境
mvn -version
# 输出: Apache Maven 3.9.6

# 检查Node.js环境
node --version
# 输出: v24.11.1

# 检查npm环境
npm --version
# 输出: 11.8.0
```

---

## 2. 数据库环境配置

### 2.1 PostgreSQL 安装 (推荐使用 Docker)

#### 方式一：Docker 安装 (推荐)

```powershell
# 拉取 PostgreSQL 镜像
docker pull postgres:14

# 启动 PostgreSQL 容器
docker run -d `
    --name ai-ready-postgres `
    -e POSTGRES_USER=devuser `
    -e POSTGRES_PASSWORD="Dev@2026#Local" `
    -e POSTGRES_DB=devdb `
    -p 5432:5432 `
    -v ai-ready-pgdata:/var/lib/postgresql/data `
    postgres:14

# 验证容器运行状态
docker ps | findstr ai-ready-postgres

# 连接测试
docker exec -it ai-ready-postgres psql -U devuser -d devdb -c "SELECT version();"
```

#### 方式二：使用项目 Docker Compose

```powershell
# 进入项目目录
cd I:\AI-Ready\docker\postgresql

# 创建实际的 docker-compose.yml 文件
# (从配置文件中提取，去除markdown格式)

# 启动服务
docker-compose up -d

# 查看状态
docker-compose ps
```

#### 方式三：本地安装 PostgreSQL

1. 下载 PostgreSQL 14+: https://www.postgresql.org/download/windows/
2. 安装时设置密码: `Dev@2026#Local`
3. 创建数据库: `devdb`
4. 创建用户: `devuser`

### 2.2 PostgreSQL 连接配置

```yaml
# application-dev.yml 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: devuser
    password: Dev@2026#Local
    driver-class-name: org.postgresql.Driver
```

### 2.3 数据库初始化

```sql
-- 连接数据库
-- psql -U devuser -d devdb

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 创建基础表结构 (示例)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- 插入测试数据
INSERT INTO users (username, email, password_hash) VALUES
('admin', 'admin@ai-ready.local', '$2a$10$...');
```

---

## 3. Redis 配置

### 3.1 Redis 安装 (推荐使用 Docker)

```powershell
# 拉取 Redis 镜像
docker pull redis:7-alpine

# 启动 Redis 容器
docker run -d `
    --name ai-ready-redis `
    -p 6379:6379 `
    -v ai-ready-redisdata:/data `
    redis:7-alpine `
    redis-server --appendonly yes

# 验证连接
docker exec -it ai-ready-redis redis-cli ping
# 输出: PONG
```

### 3.2 Redis 连接配置

```yaml
# application-dev.yml Redis配置
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 4. 项目初始化

### 4.1 项目结构

```
I:\AI-Ready
├── pom.xml                    # 父 POM (待创建)
├── core-base/                 # 基础支撑模块
├── core-workflow/             # 工作流模块
├── core-report/               # 报表模块
├── core-agent/                # Agent调用层
├── core-api/                  # API接口定义
├── core-common/               # 公共工具类
├── erp/                       # ERP模块群
│   ├── erp-purchase/
│   ├── erp-sale/
│   ├── erp-stock/
│   ├── erp-warehouse/
│   └── erp-account/
├── crm/                       # CRM模块群
│   ├── crm-lead/
│   ├── crm-opportunity/
│   ├── crm-customer/
│   └── crm-activity/
├── smart-admin-web/           # 前端项目 (Vue3)
├── erp-mobile/                # 移动端项目 (UniApp)
├── docker/                    # Docker配置
└── docs/                      # 文档目录
```

### 4.2 父 POM 配置

创建 `I:\AI-Ready\pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>cn.aiedge</groupId>
    <artifactId>ai-ready</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>智企连·AI-Ready</name>
    <description>企业智能管理系统</description>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Spring Boot -->
        <spring-boot.version>3.2.5</spring-boot.version>
        
        <!-- 数据库 -->
        <postgresql.version>42.7.3</postgresql.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        
        <!-- 安全 -->
        <sa-token.version>1.37.0</sa-token.version>
        
        <!-- 工具 -->
        <hutool.version>5.8.26</hutool.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- PostgreSQL -->
            <dependency>
                <groupId>org.postgresql</groupId>
                <artifactId>postgresql</artifactId>
                <version>${postgresql.version}</version>
            </dependency>

            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <modules>
        <module>core-base</module>
        <module>core-common</module>
        <module>core-api</module>
    </modules>
</project>
```

### 4.3 核心模块 POM 示例

创建 `I:\AI-Ready\core-base\pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>cn.aiedge</groupId>
        <artifactId>ai-ready</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>core-base</artifactId>
    <packaging>jar</packaging>

    <name>核心基础模块</name>
    <description>用户、权限、配置、字典等基础功能</description>

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 5. 前端环境配置

### 5.1 前端依赖安装

```powershell
# 进入前端目录
cd I:\AI-Ready\smart-admin-web

# 安装 pnpm (推荐)
npm install -g pnpm

# 安装依赖
pnpm install

# 或使用 npm
npm install
```

### 5.2 前端配置文件

创建 `I:\AI-Ready\smart-admin-web\.env.development`:

```env
# 开发环境配置
VITE_APP_TITLE=智企连·AI-Ready
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws
```

---

## 6. 环境验证脚本

### 6.1 完整环境验证脚本

```powershell
# I:\AI-Ready\scripts\verify-environment.ps1

Write-Host "==========================================" -ForegroundColor Green
Write-Host "智企连·AI-Ready 环境验证" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

$allPassed = $true

# 1. 检查 Java
Write-Host "`n1. 检查 Java 17..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "17"
    if ($javaVersion) {
        Write-Host "   ✅ Java 17 已安装" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Java 版本不正确" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "   ❌ Java 未安装" -ForegroundColor Red
    $allPassed = $false
}

# 2. 检查 Maven
Write-Host "`n2. 检查 Maven..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    if ($mvnVersion) {
        Write-Host "   ✅ Maven 已安装" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Maven 未安装" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "   ❌ Maven 未安装" -ForegroundColor Red
    $allPassed = $false
}

# 3. 检查 Node.js
Write-Host "`n3. 检查 Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($nodeVersion) {
        Write-Host "   ✅ Node.js $nodeVersion 已安装" -ForegroundColor Green
    }
} catch {
    Write-Host "   ❌ Node.js 未安装" -ForegroundColor Red
    $allPassed = $false
}

# 4. 检查 PostgreSQL 连接
Write-Host "`n4. 检查 PostgreSQL..." -ForegroundColor Yellow
try {
    $pgRunning = docker ps 2>&1 | Select-String "ai-ready-postgres"
    if ($pgRunning) {
        Write-Host "   ✅ PostgreSQL Docker 容器运行中" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ PostgreSQL 未运行，请启动: docker start ai-ready-postgres" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ⚠️ 无法检查 Docker，请手动验证 PostgreSQL" -ForegroundColor Yellow
}

# 5. 检查 Redis 连接
Write-Host "`n5. 检查 Redis..." -ForegroundColor Yellow
try {
    $redisRunning = docker ps 2>&1 | Select-String "ai-ready-redis"
    if ($redisRunning) {
        Write-Host "   ✅ Redis Docker 容器运行中" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ Redis 未运行，请启动: docker start ai-ready-redis" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ⚠️ 无法检查 Docker，请手动验证 Redis" -ForegroundColor Yellow
}

# 6. 检查项目结构
Write-Host "`n6. 检查项目结构..." -ForegroundColor Yellow
$requiredDirs = @("core-base", "core-common", "smart-admin-web", "docker")
foreach ($dir in $requiredDirs) {
    if (Test-Path "I:\AI-Ready\$dir") {
        Write-Host "   ✅ $dir 目录存在" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ $dir 目录不存在" -ForegroundColor Yellow
    }
}

Write-Host "`n==========================================" -ForegroundColor Green
if ($allPassed) {
    Write-Host "✅ 环境验证通过！" -ForegroundColor Green
} else {
    Write-Host "⚠️ 部分环境需要配置，请检查上述标记项" -ForegroundColor Yellow
}
Write-Host "==========================================" -ForegroundColor Green
```

---

## 7. 快速启动指南

### 7.1 一键启动脚本

```powershell
# I:\AI-Ready\scripts\quick-start.ps1

Write-Host "==========================================" -ForegroundColor Green
Write-Host "智企连·AI-Ready 快速启动" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

# 1. 启动数据库服务
Write-Host "`n1. 启动数据库服务..." -ForegroundColor Yellow
docker start ai-ready-postgres 2>$null
docker start ai-ready-redis 2>$null

# 2. 等待服务就绪
Write-Host "`n2. 等待服务就绪..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 3. 构建项目 (如果 pom.xml 存在)
Write-Host "`n3. 构建项目..." -ForegroundColor Yellow
if (Test-Path "I:\AI-Ready\pom.xml") {
    cd I:\AI-Ready
    mvn clean install -DskipTests
} else {
    Write-Host "   ⚠️ pom.xml 不存在，跳过构建" -ForegroundColor Yellow
}

# 4. 启动前端开发服务器 (如果目录存在)
Write-Host "`n4. 启动前端..." -ForegroundColor Yellow
if (Test-Path "I:\AI-Ready\smart-admin-web\package.json") {
    cd I:\AI-Ready\smart-admin-web
    Start-Process npm -ArgumentList "run", "dev"
} else {
    Write-Host "   ⚠️ 前端项目未初始化" -ForegroundColor Yellow
}

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "✅ 启动完成！" -ForegroundColor Green
Write-Host "后端API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "前端界面: http://localhost:5173" -ForegroundColor Cyan
Write-Host "pgAdmin: http://localhost:8080" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Green
```

---

## 8. 常见问题

### 8.1 Docker 相关

**问题**: Docker 命令不可用
**解决**: 安装 Docker Desktop 或使用本地安装的 PostgreSQL/Redis

**问题**: 端口被占用
**解决**: 修改 docker-compose.yml 中的端口映射

### 8.2 数据库相关

**问题**: 连接被拒绝
**解决**: 检查 PostgreSQL 是否启动，验证用户名密码

**问题**: 数据库不存在
**解决**: 手动创建数据库: `CREATE DATABASE devdb;`

### 8.3 构建相关

**问题**: Maven 依赖下载失败
**解决**: 检查网络，配置国内镜像源

---

## 9. 总结

### 9.1 环境要求清单

| 组件 | 最低版本 | 推荐版本 | 状态 |
|------|----------|----------|------|
| Java JDK | 17 | 17.0.10 | ✅ 已就绪 |
| Maven | 3.6+ | 3.9.6 | ✅ 已就绪 |
| Node.js | 18+ | 24.11.1 | ✅ 已就绪 |
| npm | 9+ | 11.8.0 | ✅ 已就绪 |
| PostgreSQL | 14+ | 14 | ⚠️ 需启动 |
| Redis | 7+ | 7-alpine | ⚠️ 需启动 |

### 9.2 下一步

1. 启动 PostgreSQL 和 Redis 服务
2. 创建 pom.xml 文件
3. 初始化各模块代码
4. 配置前端项目
5. 运行项目验证

---

**文档完成时间**: 2026-03-27  
**作者**: devops-engineer