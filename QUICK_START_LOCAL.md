# AI-Ready 本地快速启动指南

> **说明**: 由于Docker Hub访问受限，建议使用本地服务启动

---

## 方案A：本地服务启动（推荐）

### 1. 启动本地PostgreSQL

```powershell
# 检查PostgreSQL服务
Get-Service -Name postgresql* | Select-Object Name, Status

# 如果服务未运行，启动PostgreSQL
Start-Service postgresql-x64-14  # 根据实际版本调整

# 验证PostgreSQL运行
Test-NetConnection -ComputerName localhost -Port 5432
```

### 2. 启动本地Redis

```powershell
# 检查Redis服务
Get-Service -Name redis* | Select-Object Name, Status

# 如果服务未运行，启动Redis
Start-Service Redis

# 验证Redis运行
redis-cli ping
# 应返回: PONG
```

### 3. 启动AI-Ready应用

```powershell
# 进入项目目录
cd I:\AI-Ready

# 编译项目
mvn clean install -DskipTests

# 启动API服务
mvn spring-boot:run -pl core-api
```

### 4. 验证服务

```powershell
# 检查API服务是否启动
Test-NetConnection -ComputerName localhost -Port 8080

# 访问API文档
# http://localhost:8080/api/doc.html
```

---

## 方案B：Docker服务启动（备用）

如果需要使用Docker，建议：
1. 配置Docker镜像加速器（阿里云、腾讯云等）
2. 或者导入本地镜像文件

---

## 数据库配置

### PostgreSQL（本地开发）

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

### MySQL（生产环境）

- MySQL 8.0
- 支持高可用集群
- 定期备份策略

---

## 常见问题

### Q: PostgreSQL未安装
A: 下载PostgreSQL 14并安装：https://www.postgresql.org/download/windows/

### Q: Redis未安装
A: 下载Redis for Windows：https://github.com/microsoftarchive/redis/releases

### Q: Maven构建失败
A: 检查Java版本（需要JDK 17+）和Maven版本（需要3.6+）

---

## 启动成功标志

✅ **PostgreSQL**: `Test-NetConnection -ComputerName localhost -Port 5432` 返回 True  
✅ **Redis**: `redis-cli ping` 返回 PONG  
✅ **API服务**: `Test-NetConnection -ComputerName localhost -Port 8080` 返回 True  
✅ **API文档**: http://localhost:8080/api/doc.html 可访问