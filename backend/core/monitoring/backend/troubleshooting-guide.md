# AI-Ready 监控告警模块故障排查指南

## 1. 服务启动问题

### 1.1 监控告警后端服务无法启动

**症状**:
- 容器启动失败
- 服务端口无法访问
- 日志显示启动错误

**排查步骤**:

#### 1.1.1 检查依赖服务
```bash
# 检查PostgreSQL是否运行
docker exec ai-ready-postgres pg_isready

# 检查Redis是否运行
docker exec ai-ready-redis redis-cli ping

# 检查网络连接
docker network ls | grep monitoring
```

#### 1.1.2 检查配置
```bash
# 检查环境变量配置
docker inspect ai-ready-monitoring-backend-test | grep -A5 "Env"

# 检查配置文件
cat I:\AI-Ready\monitoring\backend\config\application-test.yml
```

#### 1.1.3 查看日志
```bash
# 查看容器日志
docker logs ai-ready-monitoring-backend-test

# 查看应用日志
docker exec ai-ready-monitoring-backend-test tail -f /app/logs/application.log
```

### 1.2 数据库连接问题

**症状**:
- "Connection refused" 错误
- "Authentication failed" 错误
- 数据库表不存在

**解决方案**:

#### 1.2.1 检查数据库服务
```bash
# 确认数据库容器运行状态
docker ps | grep postgres

# 检查数据库端口
netstat -an | grep 5439

# 测试数据库连接
docker exec ai-ready-monitoring-postgres-test psql -U monitoring_test_user -d monitoring_test_db -c "\l"
```

#### 1.2.2 检查数据库用户和权限
```sql
-- 连接PostgreSQL后执行
SELECT usename FROM pg_user WHERE usename = 'monitoring_test_user';
SELECT datname FROM pg_database WHERE datname = 'monitoring_test_db';
```

#### 1.2.3 检查初始化脚本
```bash
# 查看初始化脚本
cat I:\AI-Ready\monitoring\backend\init-scripts\postgres-init.sql

# 手动执行初始化
docker exec -i ai-ready-monitoring-postgres-test psql -U monitoring_test_user -d monitoring_test_db < init-scripts/postgres-init.sql
```

## 2. 告警功能问题

### 2.1 告警规则不触发

**症状**:
- 配置了告警规则但没有触发
- 指标数据正常但无告警

**排查步骤**:

#### 2.1.1 检查告警规则配置
```bash
# 查看告警规则文件
ls -la I:\AI-Ready\monitoring\backend\config\alert-rules\

# 检查规则语法
yamllint I:\AI-Ready\monitoring\backend\config\alert-rules\*.yaml
```

#### 2.1.2 检查告警规则引擎
```bash
# 查看告警规则引擎日志
docker logs ai-ready-monitoring-backend-test | grep AlertRuleEngine

# 检查定时任务执行
docker exec ai-ready-monitoring-backend-test grep "evaluateRules" /app/logs/application.log
```

#### 2.1.3 验证指标数据
```bash
# 查询最近指标数据
curl http://localhost:8090/api/v1/metrics?metricName=cpu_usage&limit=10

# 检查Prometheus指标
curl http://localhost:9090/api/v1/query?query=up
```

### 2.2 告警通知不发送

**症状**:
- 告警触发但未收到通知
- 通知渠道配置无效

**排查步骤**:

#### 2.2.1 检查通知服务配置
```bash
# 查看通知配置
cat I:\AI-Ready\monitoring\backend\config\notification.yml

# 测试邮件配置
docker exec ai-ready-monitoring-backend-test curl -s http://localhost:8090/actuator/health | grep mail
```

#### 2.2.2 检查通知服务日志
```bash
# 查看通知服务日志
docker logs ai-ready-monitoring-backend-test | grep NotificationService

# 检查通知发送记录
docker exec ai-ready-monitoring-backend-test grep "Notification sent" /app/logs/application.log
```

## 3. 性能问题

### 3.1 高CPU使用率

**症状**:
- 监控服务CPU使用率持续高位
- 响应变慢

**解决方案**:

#### 3.1.1 调整JVM参数
```yaml
# 在docker-compose.yml中调整
environment:
  JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### 3.1.2 优化查询
```sql
-- 添加索引
CREATE INDEX idx_metric_timestamp ON metrics(timestamp);
CREATE INDEX idx_alert_status ON alert_history(status);
```

#### 3.1.3 调整检查频率
```yaml
# 调整告警规则检查频率
alert:
  check-interval: 60s  # 从30秒调整为60秒
```

### 3.2 高内存使用率

**症状**:
- 内存使用持续增长
- 频繁GC

**解决方案**:

#### 3.2.1 监控内存使用
```bash
# 查看JVM内存使用
docker exec ai-ready-monitoring-backend-test jstat -gc <pid>

# 查看堆内存
docker exec ai-ready-monitoring-backend-test jmap -heap <pid>
```

#### 3.2.2 优化数据处理
```yaml
# 配置数据保留策略
metrics:
  retention-days: 7  # 指标数据保留7天
alerts:
  retention-days: 30  # 告警数据保留30天
```

#### 3.2.3 启用内存转储
```yaml
# 配置内存转储
environment:
  JAVA_OPTS: "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
```

## 4. 数据问题

### 4.1 指标数据缺失

**症状**:
- 某些指标没有数据
- 数据间隔异常

**排查步骤**:

#### 4.1.1 检查指标采集
```bash
# 查看指标采集器日志
docker logs ai-ready-monitoring-backend-test | grep MetricCollector

# 检查采集配置
cat I:\AI-Ready\monitoring\backend\config\metrics-collector.yml
```

#### 4.1.2 验证数据源
```bash
# 测试数据源连接
docker exec ai-ready-monitoring-backend-test curl http://localhost:8081/actuator/metrics

# 检查Prometheus抓取目标
curl http://localhost:9090/api/v1/targets
```

### 4.2 数据不一致

**症状**:
- 同一指标在不同系统显示不同值
- 历史数据异常

**解决方案**:

#### 4.2.1 检查时区配置
```bash
# 检查容器时区
docker exec ai-ready-monitoring-backend-test date
docker exec ai-ready-monitoring-backend-test cat /etc/timezone

# 检查应用时区配置
docker exec ai-ready-monitoring-backend-test grep "spring.jackson.time-zone" /app/config/application.yml
```

#### 4.2.2 验证数据同步
```bash
# 检查数据库同步
docker exec ai-ready-monitoring-postgres-test psql -U monitoring_test_user -d monitoring_test_db -c "SELECT now(), COUNT(*) FROM metrics;"

# 检查数据更新时间
docker exec ai-ready-monitoring-postgres-test psql -U monitoring_test_user -d monitoring_test_db -c "SELECT MAX(timestamp) FROM metrics;"
```

## 5. 网络问题

### 5.1 服务无法访问

**症状**:
- 服务端口无法连接
- 跨服务调用失败

**排查步骤**:

#### 5.1.1 检查网络配置
```bash
# 查看容器网络
docker inspect ai-ready-monitoring-backend-test | grep -A10 "NetworkSettings"

# 测试网络连通性
docker exec ai-ready-monitoring-backend-test ping postgres-monitoring
docker exec ai-ready-monitoring-backend-test curl http://prometheus:9090
```

#### 5.1.2 检查防火墙和端口
```bash
# 检查端口监听
netstat -an | grep 8090

# 检查防火墙规则
netsh advfirewall firewall show rule name=all | findstr 8090
```

## 6. 常见错误代码

### 6.1 数据库相关错误

| 错误代码 | 含义 | 解决方案 |
|---------|------|----------|
| 08001 | 无法连接到数据库 | 检查数据库服务是否运行，网络是否通畅 |
| 28P01 | 认证失败 | 检查数据库用户名和密码 |
| 3D000 | 数据库不存在 | 创建数据库或检查数据库名 |
| 42P01 | 表不存在 | 执行数据库初始化脚本 |

### 6.2 应用相关错误

| 错误代码 | 含义 | 解决方案 |
|---------|------|----------|
| 500 | 服务器内部错误 | 查看应用日志，检查配置 |
| 503 | 服务不可用 | 检查依赖服务，重启应用 |
| 400 | 请求参数错误 | 检查API调用参数 |
| 404 | 资源不存在 | 检查URL路径和资源ID |

## 7. 快速恢复步骤

### 7.1 服务完全不可用

```bash
# 1. 停止所有监控相关服务
docker-compose -f docker-compose-simple.yml down

# 2. 清理数据（谨慎操作）
rm -rf I:\AI-Ready\monitoring\backend\data\postgres-test\*

# 3. 重新启动服务
docker-compose -f docker-compose-simple.yml up -d

# 4. 检查服务状态
docker-compose -f docker-compose-simple.yml ps
docker-compose -f docker-compose-simple.yml logs -f
```

### 7.2 数据异常恢复

```bash
# 1. 备份当前数据
docker exec ai-ready-monitoring-postgres-test pg_dump -U monitoring_test_user monitoring_test_db > backup_$(date +%Y%m%d).sql

# 2. 清理异常数据
docker exec ai-ready-monitoring-postgres-test psql -U monitoring_test_user -d monitoring_test_db -c "DELETE FROM metrics WHERE timestamp < NOW() - INTERVAL '1 hour';"

# 3. 重启数据采集
docker restart ai-ready-monitoring-backend-test
```

## 8. 监控告警模块自监控

### 8.1 关键监控指标

```yaml
# 建议监控的指标
监控项:
  - 服务健康状态: http://localhost:8090/actuator/health
  - JVM内存使用: http://localhost:8090/actuator/metrics/jvm.memory.used
  - 数据库连接数: http://localhost:8090/actuator/metrics/hikaricp.connections.active
  - 请求响应时间: http://localhost:8090/actuator/metrics/http.server.requests
  - 告警触发次数: http://localhost:8090/actuator/metrics/alerts.triggered.count
```

### 8.2 告警规则示例

```yaml
# 监控告警模块自监控规则
rules:
  - rule_name: monitoring_service_down
    description: 监控告警服务不可用
    condition: up{job="monitoring-backend"} == 0
    duration: 1m
    severity: critical
    notifications:
      - email
      - webhook
    
  - rule_name: monitoring_high_memory
    description: 监控告警服务内存使用过高
    condition: process_resident_memory_bytes{job="monitoring-backend"} > 800MB
    duration: 5m
    severity: warning
    notifications:
      - email
```

## 9. 联系支持

### 9.1 内部支持
- **运维团队**: 查看日志和配置
- **开发团队**: 代码和功能问题
- **DBA团队**: 数据库相关问题

### 9.2 外部资源
- **Spring Boot文档**: https://spring.io/projects/spring-boot
- **Prometheus文档**: https://prometheus.io/docs/
- **Docker文档**: https://docs.docker.com/
- **PostgreSQL文档**: https://www.postgresql.org/docs/

---

**最后更新**: 2026-04-30  
**维护者**: AI-Ready 运维团队  
**版本**: v1.0.0