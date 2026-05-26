# Sprint 27+1 测试环境常见问题排查指南

## 目录

1. [常见问题清单](#1-常见问题清单)
2. [快速诊断步骤](#2-快速诊断步骤)
3. [故障排查脚本](#3-故障排查脚本)
4. [最佳实践建议](#4-最佳实践建议)
5. [附录](#5-附录)

---

## 1. 常见问题清单

### 1.1 服务启动问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 容器无法启动 | Docker配置错误、端口冲突、资源不足 | 🔴 紧急 |
| 服务启动后立即退出 | 配置文件错误、依赖服务未启动 | 🔴 紧急 |
| 服务启动超时 | 数据库连接失败、网络延迟 | 🟡 警告 |
| 部分服务启动失败 | 服务间依赖问题、环境变量缺失 | 🟡 警告 |
| 服务状态不稳定 | 内存不足、CPU过载、磁盘空间不足 | 🟡 警告 |

### 1.2 网络连接问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 无法访问Web界面 | 防火墙阻止、端口未开放、服务未启动 | 🔴 紧急 |
| API接口无响应 | 后端服务异常、负载过高、网络故障 | 🔴 紧急 |
| 数据库连接失败 | 数据库服务异常、网络配置错误、认证失败 | 🔴 紧急 |
| 服务间通信失败 | 网络策略限制、DNS解析失败、端口错误 | 🟡 警告 |
| 外部服务调用失败 | 外部API不可用、网络出口限制 | 🟡 警告 |

### 1.3 性能问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 页面加载缓慢 | 前端资源过大、网络延迟、CDN问题 | 🟡 警告 |
| API响应延迟 | 后端处理耗时、数据库查询慢、缓存失效 | 🟡 警告 |
| 内存使用过高 | 内存泄漏、缓存过大、并发过高 | 🔴 紧急 |
| CPU使用率过高 | 死循环、计算密集型任务、并发过高 | 🔴 紧急 |
| 磁盘空间不足 | 日志文件过大、临时文件未清理 | 🟡 警告 |

### 1.4 数据问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 数据不一致 | 事务未提交、缓存不一致、同步延迟 | 🟡 警告 |
| 数据丢失 | 数据库故障、误删除、备份失效 | 🔴 紧急 |
| 数据导入失败 | 数据格式错误、字段不匹配、权限不足 | 🟡 警告 |
| 数据导出异常 | 文件权限问题、磁盘空间不足、网络中断 | 🟡 警告 |
| 数据同步失败 | 网络中断、配置错误、服务异常 | 🟡 警告 |

### 1.5 监控告警问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 监控数据缺失 | Prometheus配置错误、服务未暴露指标 | 🟡 警告 |
| 告警未触发 | 告警规则配置错误、阈值设置不当 | 🟡 警告 |
| 告警通知失败 | 通知渠道配置错误、网络限制 | 🟡 警告 |
| Grafana仪表盘异常 | 数据源配置错误、查询语法错误 | 🟡 警告 |
| 监控服务宕机 | 资源不足、配置错误、依赖服务异常 | 🔴 紧急 |

### 1.6 安全相关问题

| 问题现象 | 可能原因 | 紧急程度 |
|----------|----------|----------|
| 认证失败 | 密码错误、账户锁定、认证服务异常 | 🔴 紧急 |
| 权限不足 | 角色配置错误、权限分配不当 | 🟡 警告 |
| SSL证书问题 | 证书过期、证书配置错误 | 🟡 警告 |
| 安全扫描告警 | 漏洞未修复、配置不安全 | 🔴 紧急 |
| 异常访问尝试 | 暴力破解、恶意扫描 | 🔴 紧急 |

---

## 2. 快速诊断步骤

### 2.1 服务状态诊断

#### 步骤1：检查容器状态
```bash
# 查看所有容器状态
docker-compose ps

# 查看指定容器状态
docker-compose ps order-service

# 查看容器日志
docker-compose logs order-service --tail=100

# 查看容器资源使用
docker stats --no-stream
```

#### 步骤2：检查服务健康状态
```bash
# 检查服务健康端点
curl -f http://localhost:8080/actuator/health

# 检查数据库连接
curl -f http://localhost:8080/actuator/health/db

# 检查Redis连接
curl -f http://localhost:8080/actuator/health/redis
```

#### 步骤3：检查端口监听
```bash
# 检查端口是否监听
netstat -tlnp | grep :8080

# 使用nc检查端口连通性
nc -zv localhost 8080

# 使用telnet检查端口
telnet localhost 8080
```

### 2.2 网络连接诊断

#### 步骤1：检查网络连通性
```bash
# 检查本地网络
ping -c 4 localhost

# 检查服务间网络
ping -c 4 test-db.example.com

# 检查外部网络
ping -c 4 8.8.8.8
```

#### 步骤2：检查DNS解析
```bash
# 检查DNS解析
nslookup test-db.example.com

# 使用dig检查DNS
dig test-db.example.com

# 检查hosts文件
cat /etc/hosts | grep test-db
```

#### 步骤3：检查防火墙规则
```bash
# 检查防火墙状态（Linux）
sudo ufw status

# 检查防火墙状态（Windows）
netsh advfirewall show allprofiles

# 检查端口是否开放
sudo ufw status verbose | grep 8080
```

### 2.3 性能问题诊断

#### 步骤1：检查系统资源
```bash
# 查看CPU使用率
top -b -n 1 | head -20

# 查看内存使用
free -h

# 查看磁盘使用
df -h

# 查看磁盘IO
iostat -x 1 5
```

#### 步骤2：检查服务性能
```bash
# 检查API响应时间
time curl -o /dev/null -s -w "%{time_total}\n" http://localhost:8080/api/orders

# 检查数据库查询性能
mysql -h test-db -u test_user -p -e "SHOW PROCESSLIST;"

# 检查慢查询日志
tail -f /var/log/mysql/mysql-slow.log
```

#### 步骤3：检查JVM性能（Java服务）
```bash
# 查看JVM内存使用
jstat -gc <pid> 1000 5

# 查看线程状态
jstack <pid> | head -100

# 查看堆内存使用
jmap -heap <pid>
```

### 2.4 数据问题诊断

#### 步骤1：检查数据库状态
```bash
# 检查数据库连接数
mysql -h test-db -u test_user -p -e "SHOW STATUS LIKE 'Threads_connected';"

# 检查表状态
mysql -h test-db -u test_user -p -e "SHOW TABLE STATUS FROM ai_ready;"

# 检查锁情况
mysql -h test-db -u test_user -p -e "SHOW ENGINE INNODB STATUS\G"
```

#### 步骤2：检查数据一致性
```bash
# 检查数据行数
mysql -h test-db -u test_user -p -e "SELECT COUNT(*) FROM orders;"

# 检查最近数据
mysql -h test-db -u test_user -p -e "SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;"

# 检查数据完整性
mysql -h test-db -u test_user -p -e "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema = 'ai_ready';"
```

#### 步骤3：检查备份状态
```bash
# 检查备份文件
ls -lh /opt/ai-ready/backups/

# 检查备份完整性
md5sum /opt/ai-ready/backups/backup_20260427.sql

# 检查备份时间
stat /opt/ai-ready/backups/backup_20260427.sql
```

### 2.5 监控告警诊断

#### 步骤1：检查Prometheus状态
```bash
# 检查Prometheus服务状态
curl -f http://localhost:9090/-/healthy

# 检查指标收集
curl http://localhost:9090/api/v1/targets

# 检查告警规则
curl http://localhost:9090/api/v1/rules
```

#### 步骤2：检查AlertManager状态
```bash
# 检查AlertManager服务状态
curl -f http://localhost:9093/-/healthy

# 检查告警列表
curl http://localhost:9093/api/v1/alerts

# 检查静默规则
curl http://localhost:9093/api/v1/silences
```

#### 步骤3：检查Grafana状态
```bash
# 检查Grafana服务状态
curl -f http://localhost:3000/api/health

# 检查数据源
curl -u admin:admin123 http://localhost:3000/api/datasources

# 检查仪表盘
curl -u admin:admin123 http://localhost:3000/api/search?type=dash-db
```

---

## 3. 故障排查脚本

### 3.1 一键诊断脚本

创建 `/opt/ai-ready/scripts/diagnostics/quick-diagnose.sh`：
```bash
#!/bin/bash

# 一键诊断脚本
# 快速检查测试环境常见问题

echo "=== Sprint 27+1 测试环境一键诊断 ==="
echo "开始时间: $(date)"
echo ""

# 1. 检查系统资源
echo "1. 系统资源检查:"
echo "----------------"
free -h | grep -E "total|available"
df -h / | grep -v Filesystem
uptime
echo ""

# 2. 检查容器状态
echo "2. 容器状态检查:"
echo "----------------"
docker-compose ps
echo ""

# 3. 检查服务健康
echo "3. 服务健康检查:"
echo "----------------"
for service in order-service inventory-service purchase-service finance-service; do
    echo -n "$service: "
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "FAIL"
done
echo ""

# 4. 检查端口监听
echo "4. 端口监听检查:"
echo "----------------"
for port in 8080 3306 6379 9090 3000 9093; do
    echo -n "端口 $port: "
    nc -zv localhost $port >/dev/null 2>&1 && echo "OPEN" || echo "CLOSED"
done
echo ""

# 5. 检查数据库连接
echo "5. 数据库连接检查:"
echo "----------------"
mysql -h test-db -u test_user -ptest_password -e "SELECT '数据库连接正常' as status;" 2>/dev/null || echo "数据库连接失败"
echo ""

# 6. 检查监控服务
echo "6. 监控服务检查:"
echo "----------------"
curl -s http://localhost:9090/-/healthy >/dev/null && echo "Prometheus: 正常" || echo "Prometheus: 异常"
curl -s http://localhost:3000/api/health >/dev/null && echo "Grafana: 正常" || echo "Grafana: 异常"
echo ""

echo "诊断完成时间: $(date)"
echo "=== 诊断结束 ==="
```

### 3.2 服务故障排查脚本

创建 `/opt/ai-ready/scripts/diagnostics/service-troubleshoot.sh`：
```bash
#!/bin/bash

# 服务故障排查脚本
# 参数: 服务名称

SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "请指定服务名称: $0 <service-name>"
    echo "可用服务: order-service, inventory-service, purchase-service, finance-service"
    exit 1
fi

echo "=== 服务故障排查: $SERVICE_NAME ==="
echo "开始时间: $(date)"
echo ""

# 1. 检查容器状态
echo "1. 容器状态检查:"
docker-compose ps $SERVICE_NAME
echo ""

# 2. 检查容器日志
echo "2. 最近100行日志:"
docker-compose logs $SERVICE_NAME --tail=100
echo ""

# 3. 检查资源使用
echo "3. 容器资源使用:"
docker stats $SERVICE_NAME --no-stream
echo ""

# 4. 检查服务端口
echo "4. 服务端口检查:"
PORT=$(docker-compose port $SERVICE_NAME 8080 2>/dev/null | cut -d: -f2)
if [ -n "$PORT" ]; then
    echo "服务端口: $PORT"
    nc -zv localhost $PORT >/dev/null 2>&1 && echo "端口状态: 开放" || echo "端口状态: 关闭"
else
    echo "未找到服务端口"
fi
echo ""

# 5. 检查健康端点
echo "5. 健康端点检查:"
HEALTH_URL="http://localhost:$PORT/actuator/health"
curl -s $HEALTH_URL | head -20
echo ""

# 6. 检查依赖服务
echo "6. 依赖服务检查:"
case $SERVICE_NAME in
    "order-service")
        echo "依赖: MySQL, Redis"
        mysql -h test-db -u test_user -ptest_password -e "SELECT 'MySQL连接正常' as status;" 2>/dev/null || echo "MySQL连接失败"
        redis-cli -h test-redis -p 6379 ping 2>/dev/null | grep -q PONG && echo "Redis连接正常" || echo "Redis连接失败"
        ;;
    "inventory-service")
        echo "依赖: MySQL"
        mysql -h test-db -u test_user -ptest_password -e "SELECT 'MySQL连接正常' as status;" 2>/dev/null || echo "MySQL连接失败"
        ;;
    "purchase-service"|"finance-service")
        echo "依赖: MySQL, Redis"
        mysql -h test-db -u test_user -ptest_password -e "SELECT 'MySQL连接正常' as status;" 2>/dev/null || echo "MySQL连接失败"
        redis-cli -h test-redis -p 6379 ping 2>/dev/null | grep -q PONG && echo "Redis连接正常" || echo "Redis连接失败"
        ;;
esac
echo ""

echo "排查完成时间: $(date)"
echo "=== 排查结束 ==="
```

### 3.3 数据库故障排查脚本

创建 `/opt/ai-ready/scripts/diagnostics/database-troubleshoot.sh`：
```bash
#!/bin/bash

# 数据库故障排查脚本

echo "=== 数据库故障排查 ==="
echo "开始时间: $(date)"
echo ""

# 1. 检查数据库服务状态
echo "1. 数据库服务状态:"
docker-compose ps mysql
echo ""

# 2. 检查数据库连接
echo "2. 数据库连接测试:"
mysql -h test-db -u test_user -ptest_password -e "SELECT '连接成功' as status, NOW() as time;" 2>/dev/null || {
    echo "连接失败，检查以下可能原因:"
    echo "  - 数据库服务未启动"
    echo "  - 网络连接问题"
    echo "  - 认证信息错误"
    echo "  - 防火墙阻止"
}
echo ""

# 3. 检查数据库性能
echo "3. 数据库性能检查:"
mysql -h test-db -u test_user -ptest_password -e "
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Queries';
SHOW STATUS LIKE 'Slow_queries';
SHOW STATUS LIKE 'Innodb_buffer_pool_reads';
" 2>/dev/null || echo "无法获取性能指标"
echo ""

# 4. 检查数据库空间
echo "4. 数据库空间检查:"
mysql -h test-db -u test_user -ptest_password -e "
SELECT 
    table_schema as '数据库',
    SUM(data_length + index_length) / 1024 / 1024 as '大小(MB)',
    COUNT(*) as '表数量'
FROM information_schema.tables 
WHERE table_schema = 'ai_ready'
GROUP BY table_schema;
" 2>/dev/null || echo "无法获取空间信息"
echo ""

# 5. 检查数据库锁
echo "5. 数据库锁检查:"
mysql -h test-db -u test_user -ptest_password -e "
SELECT 
    trx_id,
    trx_state,
    trx_started,
    trx_mysql_thread_id,
    trx_query
FROM information_schema.innodb_trx
WHERE trx_state = 'LOCK WAIT';
" 2>/dev/null || echo "无法获取锁信息"
echo ""

# 6. 检查慢查询
echo "6. 慢查询检查:"
if [ -f "/var/lib/mysql/mysql-slow.log" ]; then
    echo "最近5条慢查询:"
    tail -5 /var/lib/mysql/mysql-slow.log
else
    echo "慢查询日志未启用或不存在"
fi
echo ""

echo "排查完成时间: $(date)"
echo "=== 排查结束 ==="
```

### 3.4 监控告警故障排查脚本

创建 `/opt/ai-ready/scripts/diagnostics/monitoring-troubleshoot.sh`：
```bash
#!/bin/bash

# 监控告警故障排查脚本

echo "=== 监控告警故障排查 ==="
echo "开始时间: $(date)"
echo ""

# 1. 检查Prometheus状态
echo "1. Prometheus状态检查:"
echo "健康状态:"
curl -s http://localhost:9090/-/healthy || echo "Prometheus服务不可达"
echo ""

echo "目标状态:"
curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | select(.health == "up") | .scrapeUrl' 2>/dev/null || echo "无法获取目标状态"
echo ""

# 2. 检查AlertManager状态
echo "2. AlertManager状态检查:"
echo "健康状态:"
curl -s http://localhost:9093/-/healthy || echo "AlertManager服务不可达"
echo ""

echo "当前告警:"
curl -s http://localhost:9093/api/v1/alerts | jq -r '.data[] | .labels.alertname' 2>/dev/null || echo "无法获取告警列表"
echo ""

# 3. 检查Grafana状态
echo "3. Grafana状态检查:"
echo "健康状态:"
curl -s http://localhost:3000/api/health || echo "Grafana服务不可达"
echo ""

# 4. 检查指标收集
echo "4. 指标收集检查:"
echo "服务指标:"
curl -s http://localhost:8080/actuator/prometheus | grep -E "^jvm_" | head -5 2>/dev/null || echo "无法获取服务指标"
echo ""

# 5. 检查告警规则
echo "5. 告警规则检查:"
curl -s http://localhost:9090/api/v1/rules | jq -r '.data.groups[].rules[].name' 2>/dev/null || echo "无法获取告警规则"
echo ""

# 6. 检查通知通道
echo "6. 通知通道检查:"
echo "检查企业微信配置:"
grep -i "wechat" /opt/ai-ready/config/alertmanager.yml 2>/dev/null || echo "未找到企业微信配置"
echo ""

echo "检查邮件配置:"
grep -i "email" /opt/ai-ready/config/alertmanager.yml 2>/dev/null || echo "未找到邮件配置"
echo ""

echo "排查完成时间: $(date)"
echo "=== 排查结束 ==="
```

---

## 4. 最佳实践建议

### 4.1 预防性维护

#### 定期检查清单
1. **每日检查**
   - 检查服务健康状态
   - 检查系统资源使用
   - 查看监控告警
   - 检查日志文件大小

2. **每周检查**
   - 数据库备份验证
   - 磁盘空间清理
   - 安全扫描执行
   - 性能指标分析

3. **每月检查**
   - 系统补丁更新
   - 配置文件审查
   - 权限分配审核
   - 监控规则优化

#### 自动化监控
```yaml
# Prometheus告警规则示例
groups:
  - name: preventive_maintenance
    rules:
      - alert: DiskSpaceLow
        expr: node_filesystem_free_bytes{fstype!="tmpfs"} / node_filesystem_size_bytes{fstype!="tmpfs"} * 100 < 20
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "磁盘空间不足 {{ $labels.instance }}"
          description: "{{ $labels.mountpoint }} 磁盘空间仅剩 {{ $value }}%"
```

### 4.2 故障处理流程

#### 标准故障处理步骤
1. **确认故障**
   - 确认故障现象和影响范围
   - 收集相关日志和监控数据
   - 确定故障紧急程度

2. **临时恢复**
   - 执行快速恢复操作（重启服务、清理资源）
   - 缓解故障影响范围
   - 通知相关用户

3. **根本原因分析**
   - 分析日志和监控数据
   - 定位根本原因
   - 制定修复方案

4. **永久修复**
   - 实施修复方案
   - 验证修复效果
   - 更新相关文档

#### 故障记录模板
```markdown
## 故障记录

### 基本信息
- **故障时间**: 2026-04-27 10:30
- **发现人**: 张三
- **影响范围**: 订单服务无法访问
- **紧急程度**: 🔴 紧急

### 故障现象
1. 订单管理页面无法加载
2. API接口返回500错误
3. 数据库连接异常

### 临时恢复
1. 重启订单服务容器
2. 清理Redis缓存
3. 临时恢复服务访问

### 根本原因
1. 数据库连接池耗尽
2. Redis连接数超过限制
3. 未配置连接数监控

### 永久修复
1. 调整数据库连接池配置
2. 增加Redis连接数限制监控
3. 添加连接数预警规则

### 经验教训
1. 需要监控数据库连接数
2. 增加Redis连接数限制检查
3. 定期检查连接池配置
```

### 4.3 性能优化建议

#### 数据库优化
1. **索引优化**
   ```sql
   -- 添加缺失索引
   CREATE INDEX idx_orders_created_at ON orders(created_at);
   CREATE INDEX idx_orders_status ON orders(status);
   
   -- 删除无用索引
   DROP INDEX idx_old_unused ON old_table;
   ```

2. **查询优化**
   ```sql
   -- 避免SELECT *
   SELECT id, name, status FROM orders WHERE status = 'pending';
   
   -- 使用EXPLAIN分析查询
   EXPLAIN SELECT * FROM orders WHERE customer_id = 123;
   
   -- 分页查询优化
   SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET 0;
   ```

3. **连接池配置**
   ```yaml
   # 数据库连接池配置
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
         idle-timeout: 600000
         max-lifetime: 1800000
   ```

#### 缓存优化
1. **Redis配置优化**
   ```yaml
   # Redis配置
   spring:
     redis:
       lettuce:
         pool:
           max-active: 20
           max-idle: 10
           min-idle: 5
       timeout: 5000
   ```

2. **缓存策略**
   ```java
   // 缓存策略示例
   @Cacheable(value = "orders", key = "#orderId", 
              unless = "#result == null",
              cacheManager = "redisCacheManager")
   public Order getOrderById(Long orderId) {
       return orderRepository.findById(orderId).orElse(null);
   }
   ```

3. **缓存清理**
   ```bash
   # 定期清理缓存
   redis-cli -h test-redis KEYS "cache:*" | xargs redis-cli -h test-redis DEL
   ```

### 4.4 安全最佳实践

#### 访问控制
1. **最小权限原则**
   - 为每个服务创建独立的数据库用户
   - 限制数据库用户的权限
   - 定期审查权限分配

2. **网络隔离**
   ```yaml
   # Docker网络配置
   services:
     order-service:
       networks:
         - backend
       
     mysql:
       networks:
         - backend
       
   networks:
     backend:
       driver: bridge
   ```

3. **安全扫描**
   ```bash
   # 容器安全扫描
   docker scan ai-ready/order-service:latest
   
   # 依赖安全扫描
   npm audit
   mvn dependency-check:check
   ```

#### 监控审计
1. **访问日志记录**
   ```yaml
   # 访问日志配置
   logging:
     level:
       org.springframework.security: DEBUG
     file:
       name: /var/log/ai-ready/security.log
   ```

2. **异常访问检测**
   ```sql
   -- 检测异常登录尝试
   SELECT user_id, login_time, ip_address
   FROM login_log
   WHERE login_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
   GROUP BY user_id, ip_address
   HAVING COUNT(*) > 10;
   ```

3. **定期审计**
   ```bash
   # 检查系统日志
   journalctl --since "1 hour ago" | grep -i "fail\|error\|denied"
   
   # 检查安全事件
   grep -i "sshd" /var/log/auth.log | tail -20
   ```

---

## 5. 附录

### 5.1 紧急联系人

| 角色 | 姓名 | 联系方式 | 职责 |
|------|------|----------|------|
| 系统管理员 | 张三 | 13800138000 | 系统故障处理 |
| 数据库管理员 | 李四 | 13900139000 | 数据库故障处理 |
| 开发负责人 | 王五 | 13700137000 | 代码问题处理 |
| 监控负责人 | 赵六 | 13600136000 | 监控告警处理 |

### 5.2 常用命令速查

| 操作 | 命令 |
|------|------|
| 查看容器日志 | `docker-compose logs -f [service]` |
| 重启服务 | `docker-compose restart [service]` |
| 查看服务状态 | `docker-compose ps` |
| 检查端口 | `netstat -tlnp \| grep :[port]` |
| 检查进程 | `ps aux \| grep [service]` |
| 查看内存 | `free -h` |
| 查看磁盘 | `df -h` |
| 查看负载 | `uptime` |

### 5.3 故障代码对照表

| 故障代码 | 含义 | 处理建议 |
|----------|------|----------|
| ERR-001 | 数据库连接失败 | 检查数据库服务状态和网络连接 |
| ERR-002 | 服务启动失败 | 检查配置文件和环境变量 |
| ERR-003 | 内存不足 | 增加内存或优化内存使用 |
| ERR-004 | 磁盘空间不足 | 清理日志和临时文件 |
| ERR-005 | 网络连接超时 | 检查网络配置和防火墙规则 |
| ERR-006 | 权限不足 | 检查文件权限和用户权限 |
| ERR-007 | 配置文件错误 | 验证配置文件格式和内容 |
| ERR-008 | 依赖服务异常 | 检查依赖服务状态 |

### 5.4 故障恢复时间目标

| 故障等级 | 恢复时间目标 | 业务影响 |
|----------|--------------|----------|
| P1（严重） | 1小时内 | 核心业务完全中断 |
| P2（高） | 4小时内 | 核心业务部分中断 |
| P3（中） | 24小时内 | 非核心业务受影响 |
| P4（低） | 72小时内 | 轻微影响，可接受 |

### 5.5 文档更新记录

| 版本 | 更新日期 | 更新内容 | 更新人 |
|------|----------|----------|--------|
| v1.0 | 2026-04-27 | 初始版本创建 | 前端开发工程师 |
| v1.1 | 2026-04-27 | 添加故障排查脚本 | 前端开发工程师 |
| v1.2 | 2026-04-27 | 完善最佳实践部分 | 前端开发工程师 |

---

**文档维护**: AI-Ready测试团队  
**紧急支持**: support@example.com  
**非紧急支持**: test-support@example.com  
**文档位置**: `I:\AI-Ready\docs\testing\deploy\TROUBLESHOOTING.md`