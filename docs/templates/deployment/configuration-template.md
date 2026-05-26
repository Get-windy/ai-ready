# 测试环境配置文档模板

**模板版本**: v1.0  
**最后更新**: 2026-04-27  
**作者**: doc-writer  
**适用场景**: 测试环境配置文档

---

## 📋 目录

1. [文档信息](#文档信息)
2. [环境概述](#环境概述)
3. [基础设施配置](#基础设施配置)
4. [应用配置](#应用配置)
5. [数据库配置](#数据库配置)
6. [网络配置](#网络配置)
7. [配置验证](#配置验证)
8. [配置管理](#配置管理)

---

## 📝 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | [配置文档名称] |
| 版本号 | v1.0 |
| 创建日期 | [YYYY-MM-DD] |
| 最后更新 | [YYYY-MM-DD] |
| 作者 | [作者名] |
| 环境版本 | [环境版本] |
| 状态 | draft/active/retired |

---

## 🎯 环境概述

### 1.1 环境说明

[描述测试环境的用途和特点]

### 1.2 环境组成

| 组件 | 版本 | 数量 | 说明 |
|------|------|------|------|
| Web服务器 | [版本] | [数量] | [说明] |
| 应用服务器 | [版本] | [数量] | [说明] |
| 数据库服务器 | [版本] | [数量] | [说明] |
| 缓存服务器 | [版本] | [数量] | [说明] |
| 消息队列 | [版本] | [数量] | [说明] |

### 1.3 网络拓扑

```
┌─────────────────────────────────────────────────────┐
│                    外部网络                          │
└─────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────┐
│                防火墙/负载均衡                       │
│  ┌──────────────┐  ┌──────────────┐                │
│  │ Web Server 1 │  │ Web Server 2 │                │
│  └──────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────┐
│                  应用集群                           │
│  ┌──────────────┐  ┌──────────────┐                │
│  │ App Server 1 │  │ App Server 2 │                │
│  └──────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────┐
│                   数据集群                          │
│  ┌──────────────┐  ┌──────────────┐                │
│  │  DB Master   │  │  DB Slave    │                │
│  └──────────────┘  └──────────────┘                │
│  ┌──────────────┐                                  │
│  │   Cache      │                                  │
│  └──────────────┘                                  │
└─────────────────────────────────────────────────────┘
```

---

## 🏗️ 基础设施配置

### 2.1 服务器配置

#### 2.1.1 Web服务器

| 配置项 | 配置值 |
|--------|--------|
| 主机名 | [主机名] |
| IP地址 | [IP地址] |
| 操作系统 | [操作系统] |
| CPU | [配置] |
| 内存 | [配置] |
| 磁盘 | [配置] |
| 带宽 | [带宽] |

#### 2.1.2 应用服务器

| 配置项 | 配置值 |
|--------|--------|
| 主机名 | [主机名] |
| IP地址 | [IP地址] |
| 操作系统 | [操作系统] |
| CPU | [配置] |
| 内存 | [配置] |
| 磁盘 | [配置] |
| 带宽 | [带宽] |

#### 2.1.3 数据库服务器

| 配置项 | 配置值 |
|--------|--------|
| 主机名 | [主机名] |
| IP地址 | [IP地址] |
| 操作系统 | [操作系统] |
| CPU | [配置] |
| 内存 | [配置] |
| 磁盘 | [配置] |
| 带宽 | [带宽] |

### 2.2 基础软件安装

#### 2.2.1 操作系统

```bash
# 安装操作系统
[安装命令]

# 配置系统参数
vim /etc/sysctl.conf
```

#### 2.2.2 Docker安装

```bash
# 安装Docker
curl -fsSL https://get.docker.com | sh

# 启动Docker
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
docker run hello-world
```

---

## ⚙️ 应用配置

### 3.1 Web服务配置

#### 3.1.1 Nginx配置

```nginx
# /etc/nginx/nginx.conf
http {
    upstream app_backend {
        server [app-server-1]:[端口];
        server [app-server-2]:[端口];
    }

    server {
        listen 80;
        server_name [域名];

        location / {
            proxy_pass http://app_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        location /api/ {
            proxy_pass http://app_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

#### 3.1.2 应用配置

```yaml
# application.yml
server:
  port: [端口]
  context-path: /

spring:
  datasource:
    url: jdbc:mysql://[db-host]:[db-port]/[db-name]
    username: [username]
    password: [password]
  redis:
    host: [redis-host]
    port: [redis-port]
```

### 3.2 应用部署

```bash
# 构建应用
mvn clean package -DskipTests

# 部署应用
scp target/app.jar [app-server]:/path/to/deploy/

# 启动应用
nohup java -jar app.jar > app.log 2>&1 &

# 验证部署
curl http://[app-server]:[端口]/api/health
```

---

## 💾 数据库配置

### 4.1 MySQL配置

#### 4.1.1 主从复制配置

```sql
-- 主库配置
vim /etc/mysql/mysql.conf.d/mysqld.cnf

[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
relay-log = relay_log
relay-log-index = relay_log_index

-- 从库配置
vim /etc/mysql/mysql.conf.d/mysqld.cnf

[mysqld]
server-id = 2
log-bin = mysql-bin
binlog-format = ROW
relay-log = relay_log
relay-log-index = relay_log_index
```

#### 4.1.2 数据库账号

| 账号类型 | 用户名 | 密码 | 权限 | 用途 |
|---------|--------|------|------|------|
| 主库账号 | [主库用户] | [密码] | 全部权限 | 主从复制 |
| 应用账号 | [应用用户] | [密码] | 数据库权限 | 应用连接 |
| 管理账号 | [管理员] | [密码] | 管理权限 | 数据库管理 |

### 4.2 Redis配置

```yaml
# redis.conf
port [端口]
bind 0.0.0.0
protected-mode yes
tcp-backlog 511
timeout 0
tcp-keepalive 300
```

---

## 🌐 网络配置

### 5.1 防火墙配置

```bash
# 开放需要的端口
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow [自定义端口]/tcp

# 重启防火墙
ufw reload

# 查看状态
ufw status
```

### 5.2 DNS配置

```bash
# 配置本地DNS
vim /etc/hosts

[IP地址] [域名1]
[IP地址] [域名2]
[IP地址] [域名3]

# 重启DNS服务
systemctl restart nscd
```

---

## ✅ 配置验证

### 6.1 验证清单

#### 6.1.1 基础设施验证

- [ ] 服务器 hardware检查通过
- [ ] 操作系统版本正确
- [ ] 网络配置正确
- [ ] 防火墙配置正确

#### 6.1.2 软件验证

- [ ] Docker安装成功
- [ ] Web服务正常运行
- [ ] 应用服务正常运行
- [ ] 数据库服务正常运行
- [ ] 缓存服务正常运行

#### 6.1.3 连接验证

- [ ] 数据库连接正常
- [ ] Redis连接正常
- [ ] 消息队列连接正常
- [ ] 外部服务连接正常

### 6.2 验证脚本

```bash
#!/bin/bash
# 配置验证脚本

echo "=== 基础设施验证 ==="
df -h
free -h
uptime

echo "=== 服务状态验证 ==="
systemctl status nginx
systemctl status app
systemctl status mysql
systemctl status redis

echo "=== 网络连接验证 ==="
curl -s http://localhost:[端口]/health
mysql -u [用户] -p[密码] -h [地址] -e "SELECT 1"
redis-cli ping
```

---

## 🔧 配置管理

### 7.1 配置备份

```bash
# 备份配置文件
tar -czf config-backup-$(date +%Y%m%d).tar.gz /etc/[服务名]/

# 备份数据库配置
mysqldump -u [用户] -p[密码] --databases [数据库名] > db-config-backup-$(date +%Y%m%d).sql
```

### 7.2 配置更新流程

1. **准备阶段**
   - 备份现有配置
   - 测试配置变更
   - 通知相关方

2. **变更阶段**
   - 更新配置文件
   - 重启服务
   - 验证配置生效

3. **验证阶段**
   - 功能验证
   - 性能验证
   - 回滚准备

### 7.3 配置回滚

```bash
# 回滚配置
tar -xzf config-backup-[旧日期].tar.gz -C /

# 重启服务
systemctl restart [服务名]

# 验证回滚
curl http://localhost:[端口]/health
```

---

## 📚 附录

### A. 常用命令

```bash
# 查看服务状态
systemctl status [服务名]

# 查看端口监听
netstat -tlnp | grep [端口]

# 查看连接数
netstat -an | grep ESTABLISHED | wc -l

# 查看日志
tail -f /var/log/[服务名]/[日志文件]
```

### B. 配置文件说明

| 配置文件 | 路径 | 说明 |
|---------|------|------|
| Web配置 | [路径] | Web服务配置 |
| 应用配置 | [路径] | 应用服务配置 |
| 数据库配置 | [路径] | 数据库配置 |
| Redis配置 | [路径] | Redis配置 |

### C. 参考文档

- [Docker官方文档](https://docs.docker.com/)
- [Nginx配置手册](https://nginx.org/en/docs/)
- [MySQL配置指南](https://dev.mysql.com/doc/)

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**作者**: doc-writer
