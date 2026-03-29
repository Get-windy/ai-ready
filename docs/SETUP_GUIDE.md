# 智企连·AI-Ready 本地开发环境搭建指南

> **快速入门** | 让新团队成员 30 分钟内完成开发环境配置  
> **版本**: v1.1 | **更新日期**: 2026-03-29 | **作者**: devops-engineer

---

## 📋 目录

1. [环境要求](#1-环境要求)
2. [安装步骤](#2-安装步骤)
3. [配置文件说明](#3-配置文件说明)
4. [项目启动](#4-项目启动)
5. [验证脚本](#5-验证脚本)
6. [常见问题](#6-常见问题)

---

## 1. 环境要求

### 1.1 必备软件清单

| 软件 | 版本要求 | 检查命令 | 下载地址 |
|------|----------|----------|----------|
| **Java JDK** | 17+ | `java -version` | [Oracle JDK](https://www.oracle.com/java/) / [OpenJDK](https://adoptium.net/) |
| **Maven** | 3.6+ | `mvn -version` | [Maven 官网](https://maven.apache.org/) |
| **Node.js** | 18+ | `node --version` | [Node.js 官网](https://nodejs.org/) |
| **npm/pnpm** | 9+ | `npm --version` | npm 随 Node.js 安装 |
| **Git** | 2.30+ | `git --version` | [Git 官网](https://git-scm.com/) |

### 1.2 数据库服务

| 服务 | 版本 | 端口 | 推荐安装方式 |
|------|------|------|--------------|
| **PostgreSQL** | 14+ | 5432 | Docker |
| **Redis** | 7+ | 6379 | Docker |

### 1.3 推荐 IDE

- **后端**: IntelliJ IDEA 2023+ (Ultimate 或 Community)
- **前端**: VS Code + Volar 插件
- **通用**: JetBrains Fleet / Eclipse

---

## 2. 安装步骤

### 2.1 Step 1: 安装 Java JDK 17

#### Windows 安装

```powershell
# 方式一：使用 scoop (推荐)
scoop install openjdk17

# 方式二：手动安装
# 1. 下载 JDK 17: https://adoptium.net/
# 2. 安装到 C:\Program Files\Java\jdk-17
# 3. 配置环境变量:

# 设置 JAVA_HOME
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")

# 添加到 PATH
[System.Environment]::SetEnvironmentVariable("Path", "$env:Path;C:\Program Files\Java\jdk-17\bin", "User")

# 验证安装
java -version
# 输出: openjdk version "17.0.x"
```

#### 环境变量验证

```powershell
# 检查 JAVA_HOME
echo $env:JAVA_HOME
# 应输出: C:\Program Files\Java\jdk-17 (或类似路径)

# 检查 Java 版本
java -version
javac -version
```

### 2.2 Step 2: 安装 Maven

```powershell
# 方式一：使用 scoop (推荐)
scoop install maven

# 方式二：手动安装
# 1. 下载 Maven: https://maven.apache.org/download.cgi
# 2. 解压到 C:\Program Files\Apache\maven
# 3. 配置环境变量:

[System.Environment]::SetEnvironmentVariable("M2_HOME", "C:\Program Files\Apache\maven", "User")
[System.Environment]::SetEnvironmentVariable("Path", "$env:Path;C:\Program Files\Apache\maven\bin", "User")

# 验证安装
mvn -version
# 输出: Apache Maven 3.9.x
```

#### Maven 配置优化

创建或修改 `%USERPROFILE%\.m2\settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  
  <!-- 使用阿里云镜像加速 -->
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>

  <!-- 本地仓库路径 (可选修改) -->
  <localRepository>D:\maven\repository</localRepository>
</settings>
```

### 2.3 Step 3: 安装 Node.js

```powershell
# 方式一：使用 scoop (推荐)
scoop install nodejs-lts

# 方式二：使用官方安装包
# 下载: https://nodejs.org/ (选择 LTS 版本)

# 验证安装
node --version   # 应输出 v18.x 或更高
npm --version    # 应输出 9.x 或更高

# 安装 pnpm (推荐用于前端项目管理)
npm install -g pnpm
pnpm --version
```

### 2.4 Step 4: 安装 Git

```powershell
# 使用 scoop 安装
scoop install git

# 验证安装
git --version
# 输出: git version 2.x.x

# 配置用户信息
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

### 2.5 Step 5: 启动数据库服务 (Docker)

> **前提**: 已安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

```powershell
# ===== PostgreSQL =====
# 拉取镜像
docker pull postgres:14-alpine

# 启动容器
docker run -d `
    --name ai-ready-postgres `
    -e POSTGRES_USER=devuser `
    -e POSTGRES_PASSWORD="Dev@2026#Local" `
    -e POSTGRES_DB=devdb `
    -p 5432:5432 `
    -v ai-ready-pgdata:/var/lib/postgresql/data `
    postgres:14-alpine

# 验证 PostgreSQL
docker exec -it ai-ready-postgres psql -U devuser -d devdb -c "SELECT version();"

# ===== Redis =====
# 拉取镜像
docker pull redis:7-alpine

# 启动容器
docker run -d `
    --name ai-ready-redis `
    -p 6379:6379 `
    -v ai-ready-redisdata:/data `
    redis:7-alpine `
    redis-server --appendonly yes

# 验证 Redis
docker exec -it ai-ready-redis redis-cli ping
# 输出: PONG
```

#### 无 Docker 安装方式

如果无法使用 Docker，可本地安装：

- **PostgreSQL**: https://www.postgresql.org/download/windows/
- **Redis (Windows)**: https://github.com/microsoftarchive/redis/releases

### 2.6 Step 6: 克隆项目代码

```powershell
# 克隆项目
cd I:\                           # 或你喜欢的开发目录
git clone https://gitee.com/CozyNook/ai-ready.git AI-Ready

# 进入项目目录
cd I:\AI-Ready

# 查看项目结构
tree /F /A
```

---

## 3. 配置文件说明

### 3.1 后端配置文件

配置文件位置: `core-base/src/main/resources/application.yml`

#### application.yml (主配置)

```yaml
# 服务端口
server:
  port: 8080

# Spring 配置
spring:
  application:
    name: ai-ready-core
  
  # 数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: devuser
    password: Dev@2026#Local
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  # JPA 配置
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: 
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

# Sa-Token 配置
sa-token:
  token-name: satoken
  timeout: 86400          # token 有效期 (秒)
  active-timeout: -1      # token 临时有效期
  is-concurrent: true     # 是否允许同账号多端登录
  is-share: true          # 多人共享 token
  token-style: uuid       # token 风格
  is-log: true            # 是否输出日志

# Knife4j API 文档
knife4j:
  enable: true
  setting:
    language: zh_cn
```

#### application-dev.yml (开发环境)

```yaml
# 开发环境配置 - 覆盖主配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb
    username: devuser
    password: Dev@2026#Local

  jpa:
    show-sql: true

logging:
  level:
    cn.aiedge: DEBUG
    org.springframework.web: DEBUG
```

### 3.2 前端配置文件

配置文件位置: `smart-admin-web/.env.development`

```env
# 开发环境配置
VITE_APP_TITLE=智企连·AI-Ready
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws
VITE_ENABLE_MOCK=false

# 功能开关
VITE_ENABLE_DEBUG=true
VITE_ENABLE_DEVTOOLS=true
```

#### vite.config.ts (构建配置)

关键配置说明:

```typescript
export default defineConfig({
  server: {
    port: 3000,                    // 前端开发服务器端口
    proxy: {
      '/api': {                    // API 代理到后端
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // ... 其他配置已在项目文件中定义
})
```

---

## 4. 项目启动

### 4.1 后端启动

```powershell
# 进入项目根目录
cd I:\AI-Ready

# 方式一：Maven 命令启动
mvn clean install -DskipTests
cd core-base
mvn spring-boot:run

# 方式二：IDE 启动
# 在 IntelliJ IDEA 中:
# 1. 打开项目 I:\AI-Ready
# 2. 找到 core-base/src/main/java/.../Application.java
# 3. 右键 -> Run 'Application'

# 验证启动
curl http://localhost:8080/actuator/health
# 应返回: {"status":"UP"}

# API 文档地址
# http://localhost:8080/doc.html
```

### 4.2 前端启动

```powershell
# 进入前端目录
cd I:\AI-Ready\smart-admin-web

# 安装依赖 (首次或依赖变更时)
pnpm install
# 或
npm install

# 启动开发服务器
pnpm dev
# 或
npm run dev

# 访问地址
# http://localhost:3000
```

### 4.3 一键启动脚本

使用项目提供的快速启动脚本：

```powershell
# 运行一键启动脚本
cd I:\AI-Ready\scripts
.\quick-start.ps1
```

---

## 5. 验证脚本

### 5.1 环境检查脚本

**位置**: `I:\AI-Ready\scripts\verify-environment.ps1`

运行方式：

```powershell
cd I:\AI-Ready\scripts
.\verify-environment.ps1
```

脚本输出示例：

```
==========================================
智企连·AI-Ready 环境验证
==========================================

1. 检查 Java 17...
   ✅ Java 17.0.10 已安装

2. 检查 Maven...
   ✅ Apache Maven 3.9.6 已安装

3. 检查 Node.js...
   ✅ Node.js v24.11.1 已安装

4. 检查 PostgreSQL...
   ✅ PostgreSQL Docker 容器运行中

5. 检查 Redis...
   ✅ Redis Docker 容器运行中

6. 检查项目结构...
   ✅ core-base 目录存在
   ✅ core-common 目录存在
   ✅ smart-admin-web 目录存在

==========================================
✅ 环境验证通过！
==========================================
```

### 5.2 一键启动脚本

**位置**: `I:\AI-Ready\scripts\quick-start.ps1`

功能：
- 启动 PostgreSQL 和 Redis Docker 容器
- 构建 Maven 项目
- 启动前端开发服务器

---

## 6. 常见问题

### 6.1 Java 相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `java -version` 不显示 17 | JDK 版本不对或环境变量错误 | 重新安装 JDK 17，检查 JAVA_HOME |
| Maven 编译报错 | JDK 版本不匹配 | 确认 pom.xml 中 java.version=17 |
| `UnsupportedClassVersionError` | 运行时 JDK 版本低于编译版本 | 升级运行环境 JDK |

### 6.2 Maven 相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 依赖下载失败 | 网络问题或仓库不可用 | 配置阿里云镜像源 |
| `mvn not found` | Maven 未安装或 PATH 未配置 | 安装 Maven，配置环境变量 |
| 编译内存溢出 | Maven 内存配置过低 | 设置 `MAVEN_OPTS=-Xmx1024m` |

**Maven 镜像配置 (阿里云)**:

```xml
<mirror>
  <id>aliyun</id>
  <url>https://maven.aliyun.com/repository/public</url>
  <mirrorOf>central</mirrorOf>
</mirror>
```

### 6.3 数据库相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| PostgreSQL 连接被拒绝 | 服务未启动或端口错误 | 启动 Docker 容器，检查端口 |
| 认证失败 | 用户名密码错误 | 确认 devuser/Dev@2026#Local |
| 数据库不存在 | devdb 未创建 | 创建数据库: `CREATE DATABASE devdb;` |

**PostgreSQL 连接测试**:

```powershell
# Docker 方式
docker exec -it ai-ready-postgres psql -U devuser -d devdb -c "SELECT 1;"

# 本地安装方式
psql -h localhost -p 5432 -U devuser -d devdb -c "SELECT 1;"
```

### 6.4 Redis 相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| Redis 连接失败 | 服务未启动 | 启动 Docker: `docker start ai-ready-redis` |
| `NOAUTH Authentication required` | Redis 设置了密码 | 检查 application.yml 中 redis.password |

**Redis 连接测试**:

```powershell
docker exec -it ai-ready-redis redis-cli ping
# 应返回: PONG
```

### 6.5 前端相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `npm install` 失败 | 网络问题或 registry 不通 | 切换 npm 镜像源 |
| Vite 启动报错 | Node.js 版本过低 | 升级到 Node.js 18+ |
| 页面空白 | 后端 API 未启动 | 先启动后端服务 |
| API 请求 CORS 错误 | 代理配置错误 | 检查 vite.config.ts proxy 配置 |

**npm 镜像配置 (淘宝源)**:

```powershell
npm config set registry https://registry.npmmirror.com
# 或使用 pnpm
pnpm config set registry https://registry.npmmirror.com
```

### 6.6 Docker 相关

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| Docker 命令不可用 | Docker Desktop 未安装 | 安装 Docker Desktop |
| 端口 5432 被占用 | 本地 PostgreSQL 已运行 | 停止本地 PostgreSQL 或修改 Docker 端口 |
| 容器启动失败 | 镜像未拉取或配置错误 | 检查 docker logs ai-ready-postgres |

---

## 📚 相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 环境搭建详解 | `docs/ENVIRONMENT_SETUP.md` | 更详细的环境配置说明 |
| 配置参考 | `docs/configuration.md` | 配置文件详细说明 |
| 部署指南 | `docs/DEPLOYMENT_GUIDE.md` | 生产环境部署流程 |
| API 参考 | `docs/API_REFERENCE.md` | API 接口文档 |
| 文档索引 | `docs/DOCUMENT_INDEX.md` | 所有文档目录 |

---

## ✅ 环境搭建完成检查

完成以下检查后，即可开始开发：

```
□ Java 17 已安装 (java -version 验证)
□ Maven 3.6+ 已安装 (mvn -version 验证)
□ Node.js 18+ 已安装 (node --version 验证)
□ Git 已配置 (git config --list 验证)
□ PostgreSQL 运行中 (docker ps | grep ai-ready-postgres)
□ Redis 运行中 (docker ps | grep ai-ready-redis)
□ 后端可启动 (mvn spring-boot:run 成功)
□ 前端可启动 (pnpm dev 成功)
□ API 文档可访问 (http://localhost:8080/doc.html)
```

---

> **需要帮助?**  
> - 项目群组: sessionKey=group:group_1774582947832_uxqojz  
> - 技术支持: support@aiedge.cn