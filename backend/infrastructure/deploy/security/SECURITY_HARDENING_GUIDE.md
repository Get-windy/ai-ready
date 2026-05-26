# AI-Ready 测试环境安全加固实施指南

**版本**: v1.0.0
**日期**: 2026-04-29
**适用环境**: Sprint 27+1 测试环境

---

## 目录

1. [概述](#1-概述)
2. [操作系统安全加固](#2-操作系统安全加固)
3. [网络安全加固](#3-网络安全加固)
4. [数据库安全加固](#4-数据库安全加固)
5. [容器安全加固](#5-容器安全加固)
6. [应用安全加固](#6-应用安全加固)
7. [监控与审计](#7-监控与审计)
8. [应急响应](#8-应急响应)

---

## 1. 概述

### 1.1 加固目标
- 确保测试环境符合企业安全基线要求
- 防范常见安全威胁（OWASP Top 10）
- 满足等保2.0三级基本要求
- 建立持续安全监控机制

### 1.2 加固范围
| 层级 | 组件 | 状态 |
|------|------|------|
| 基础设施 | 操作系统 | ✅ 已加固 |
| 基础设施 | 网络 | ✅ 已加固 |
| 数据层 | PostgreSQL | ✅ 已加固 |
| 数据层 | Redis | ✅ 已加固 |
| 数据层 | Kafka | ✅ 已加固 |
| 容器层 | Docker | ⚠️ 部分加固 |
| 应用层 | API Gateway | ⏸️ 等待启动 |

### 1.3 参考标准
- GB/T 22239-2019 信息安全技术 网络安全等级保护基本要求
- CIS Docker Benchmark v1.5.0
- OWASP Top 10 (2021)
- NIST Cybersecurity Framework

---

## 2. 操作系统安全加固

### 2.1 用户管理

#### 2.1.1 创建专用用户
```bash
# 创建系统用户（无登录权限）
useradd -r -s /bin/false ai-ready

# 创建应用目录
mkdir -p /opt/ai-ready
chown ai-ready:ai-ready /opt/ai-ready
chmod 755 /opt/ai-ready
```

#### 2.1.2 配置密码策略
```bash
# 安装密码质量检查工具
apt-get install libpam-pwquality  # Debian/Ubuntu
yum install pam_pwquality         # RHEL/CentOS

# 配置密码策略 (/etc/security/pwquality.conf)
minlen = 12
minclass = 3
maxrepeat = 2
dcredit = -1
ucredit = -1
ocredit = -1
lcredit = -1
```

#### 2.1.3 配置SSH安全
```bash
# 编辑 /etc/ssh/sshd_config
PermitRootLogin no
PasswordAuthentication no  # 推荐使用密钥认证
PubkeyAuthentication yes
MaxAuthTries 3
ClientAliveInterval 300
ClientAliveCountMax 2
Protocol 2

# 重启SSH服务
systemctl restart sshd
```

### 2.2 文件权限

#### 2.2.1 关键文件权限
```bash
# 系统文件
chmod 644 /etc/passwd
chmod 000 /etc/shadow
chmod 644 /etc/group
chmod 600 /etc/ssh/sshd_config

# Docker相关
chmod 660 /var/run/docker.sock
chown root:docker /var/run/docker.sock
```

#### 2.2.2 日志文件权限
```bash
mkdir -p /var/log/ai-ready
chown ai-ready:ai-ready /var/log/ai-ready
chmod 750 /var/log/ai-ready
```

### 2.3 服务管理

#### 2.3.1 禁用不必要服务
```bash
# 列出所有运行中的服务
systemctl list-units --type=service --state=running

# 禁用危险服务
systemctl disable telnet
systemctl disable ftp
systemctl disable nfs-server
systemctl disable smb
```

#### 2.3.2 启用审计服务
```bash
# 安装审计工具
apt-get install auditd audispd-plugins

# 启动并启用服务
systemctl enable auditd
systemctl start auditd

# 配置审计规则 (/etc/audit/rules.d/ai-ready.rules)
-w /etc/passwd -p wa -k identity_changes
-w /etc/group -p wa -k identity_changes
-w /etc/ssh/sshd_config -p wa -k ssh_config_changes
-w /var/log/ai-ready -p wa -k app_logs
```

---

## 3. 网络安全加固

### 3.1 防火墙配置

#### 3.1.1 使用iptables
```bash
#!/bin/bash
# 清空现有规则
iptables -F
iptables -X

# 默认策略
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# 允许回环接口
iptables -A INPUT -i lo -j ACCEPT

# 允许已建立的连接
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# SSH (仅限内网)
iptables -A INPUT -p tcp --dport 22 -s 192.168.0.0/16 -j ACCEPT

# API Gateway
iptables -A INPUT -p tcp --dport 8080 -m limit --limit 100/minute -j ACCEPT

# PostgreSQL (仅Docker网络)
iptables -A INPUT -p tcp --dport 5432 -s 172.17.0.0/16 -j ACCEPT

# Redis (仅Docker网络)
iptables -A INPUT -p tcp --dport 6379 -s 172.17.0.0/16 -j ACCEPT

# 保存规则
iptables-save > /etc/iptables/rules.v4
```

#### 3.1.2 使用nftables (推荐)
```bash
#!/usr/sbin/nft -f

flush ruleset

table inet filter {
    chain input {
        type filter hook input priority 0; policy drop;
        
        # 允许回环
        iif "lo" accept
        
        # 允许已建立的连接
        ct state established,related accept
        
        # 允许ICMP
        ip protocol icmp accept
        ip6 nexthdr icmpv6 accept
        
        # SSH (内网)
        tcp dport 22 ip saddr 192.168.0.0/16 accept
        
        # API Gateway (限流)
        tcp dport 8080 limit rate 100/minute accept
        
        # 数据库 (Docker网络)
        tcp dport { 5432, 6379, 9092 } ip saddr 172.17.0.0/16 accept
        
        # 监控 (内网)
        tcp dport { 9090, 3000 } ip saddr 192.168.0.0/16 accept
    }
    
    chain forward {
        type filter hook forward priority 0; policy drop;
    }
    
    chain output {
        type filter hook output priority 0; policy accept;
    }
}
```

### 3.2 Docker网络安全

#### 3.2.1 创建隔离网络
```bash
# 应用网络
docker network create \
  --driver bridge \
  --subnet 172.20.0.0/16 \
  --gateway 172.20.0.1 \
  ai-ready-test-network

# 数据库网络 (内部)
docker network create \
  --driver bridge \
  --subnet 172.21.0.0/16 \
  --gateway 172.21.0.1 \
  --internal \
  ai-ready-db-network
```

#### 3.2.2 网络策略 (Kubernetes)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: ai-ready-test
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-api-ingress
  namespace: ai-ready-test
spec:
  podSelector:
    matchLabels:
      app: api-gateway
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 8080
```

---

## 4. 数据库安全加固

### 4.1 PostgreSQL加固

#### 4.1.1 用户和权限
```sql
-- 创建应用用户
CREATE USER ai_ready_app WITH PASSWORD 'ChangeMe123!';

-- 授予数据库连接权限
GRANT CONNECT ON DATABASE ai_ready_test TO ai_ready_app;

-- 授予schema使用权限
GRANT USAGE ON SCHEMA public TO ai_ready_app;

-- 授予表操作权限
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ai_ready_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ai_ready_app;

-- 回收公共权限
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
```

#### 4.1.2 配置pg_hba.conf
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             postgres                                peer
host    ai_ready_test   ai_ready_app    172.17.0.0/16           scram-sha-256
host    ai_ready_test   ai_ready_app    127.0.0.1/32            scram-sha-256
host    all             all             0.0.0.0/0               reject
```

#### 4.1.3 启用审计日志
```sql
-- postgresql.conf
log_connections = on
log_disconnections = on
log_duration = on
log_min_duration_statement = 1000
log_checkpoints = on
log_lock_waits = on
```

### 4.2 Redis加固

#### 4.2.1 基本安全配置
```bash
# redis.conf
requirepass YourStrongPassword123!
bind 127.0.0.1 172.17.0.1
protected-mode yes

# 重命名危险命令
rename-command CONFIG CONFIG_a7f3d9e2
rename-command FLUSHDB FLUSHDB_a7f3d9e2
rename-command FLUSHALL FLUSHALL_a7f3d9e2
rename-command DEBUG DEBUG_a7f3d9e2
rename-command SHUTDOWN SHUTDOWN_a7f3d9e2
```

### 4.3 Kafka加固

#### 4.3.1 SASL认证配置
```properties
# server.properties
security.inter.broker.protocol=SASL_SSL
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN

# 启用ACL
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:admin
```

---

## 5. 容器安全加固

### 5.1 Docker守护进程配置

#### 5.1.1 daemon.json
```json
{
  "userns-remap": "default",
  "live-restore": true,
  "userland-proxy": false,
  "no-new-privileges": true,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "seccomp-profile": "/etc/docker/seccomp-default.json"
}
```

### 5.2 容器运行时安全

#### 5.2.1 安全运行参数
```bash
docker run -d \
  --name ai-ready-app \
  --user ai-ready \
  --read-only \
  --security-opt no-new-privileges:true \
  --cap-drop ALL \
  --cap-add NET_BIND_SERVICE \
  --memory 512m \
  --memory-swap 512m \
  --cpus 1.0 \
  --pids-limit 100 \
  --health-cmd "curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval 30s \
  --health-retries 3 \
  ai-ready/app:latest
```

### 5.3 镜像安全扫描

#### 5.3.1 使用Trivy扫描
```bash
# 安装Trivy
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin

# 扫描镜像
trivy image --severity HIGH,CRITICAL ai-ready/app:latest

# 生成报告
trivy image --format json --output scan-report.json ai-ready/app:latest
```

---

## 6. 应用安全加固

### 6.1 API Gateway安全

#### 6.1.1 认证配置
```yaml
# 启用JWT认证
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.ai-ready.cn
          jwk-set-uri: https://auth.ai-ready.cn/.well-known/jwks.json
```

#### 6.1.2 限流配置
```yaml
# 速率限制
spring:
  cloud:
    gateway:
      default-filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20
          key-resolver: "#{@ipKeyResolver}"
```

#### 6.1.3 CORS配置
```yaml
# 跨域配置
gateway:
  security:
    enable-cors: true
    cors:
      allowed-origins:
        - "https://app.ai-ready.cn"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers:
        - "*"
      allow-credentials: true
      max-age: 3600
```

### 6.2 输入验证

#### 6.2.1 SQL注入防护
```java
// 使用预编译语句
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);

// 使用MyBatis-Plus Wrapper
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("username", username);  // 自动转义
```

#### 6.2.2 XSS防护
```java
// 输出编码
String safeOutput = HtmlUtils.htmlEscape(userInput);

// 使用Spring Security的防火墙
httpSecurity.headers()
    .xssProtection()
    .and()
    .contentSecurityPolicy("default-src 'self'");
```

---

## 7. 监控与审计

### 7.1 安全监控

#### 7.1.1 告警规则
```yaml
# Prometheus告警规则
groups:
- name: security-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      
  - alert: UnauthorizedAccess
    expr: increase(http_requests_total{status="401"}[5m]) > 100
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Multiple unauthorized access attempts"
```

### 7.2 日志审计

#### 7.2.1 审计日志配置
```yaml
# 审计日志配置
audit:
  enabled: true
  log-path: /var/log/ai-ready/audit
  events:
    - user_login
    - user_logout
    - data_access
    - config_change
    - permission_change
  retention-days: 90
```

---

## 8. 应急响应

### 8.1 安全事件分级

| 级别 | 描述 | 响应时间 | 示例 |
|------|------|----------|------|
| P0 | 严重 | 15分钟 | 数据泄露、系统入侵 |
| P1 | 高危 | 1小时 | 大量异常访问、服务中断 |
| P2 | 中危 | 4小时 | 个别漏洞利用尝试 |
| P3 | 低危 | 24小时 | 扫描探测、信息收集 |

### 8.2 应急响应流程

```
1. 检测
   └── 监控告警 / 安全报告 / 日志分析
   
2. 遏制
   ├── 隔离受影响系统
   ├── 阻断攻击源IP
   └── 启用备用服务
   
3. 根除
   ├── 分析攻击路径
   ├── 修复漏洞
   └── 清除恶意代码
   
4. 恢复
   ├── 验证系统完整性
   ├── 恢复服务
   └── 加强监控
   
5. 复盘
   ├── 事件分析
   ├── 改进措施
   └── 更新安全策略
```

### 8.3 应急联系人

| 角色 | 联系人 | 联系方式 |
|------|--------|----------|
| 安全负责人 | - | - |
| 运维负责人 | devops-engineer | - |
| 开发负责人 | team-member | - |
| 管理层 | coordinator | - |

---

## 附录

### A. 配置文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| OS安全基线 | `deploy/security/os-security-baseline.yml` | 操作系统加固配置 |
| 网络安全基线 | `deploy/security/network-security-baseline.yml` | 网络策略配置 |
| 数据库安全基线 | `deploy/security/database-security-baseline.yml` | 数据库加固配置 |
| 容器扫描脚本 | `deploy/security/container-security-scan.sh` | 自动化扫描脚本 |
| 合规报告 | `deploy/security/security-compliance-report.md` | 合规检查报告 |

### B. 参考文档

- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Docker Security](https://docs.docker.com/engine/security/)

---

**文档维护**: devops-engineer
**最后更新**: 2026-04-29
