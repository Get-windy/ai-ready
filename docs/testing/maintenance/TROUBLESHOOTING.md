# 常见问题排查指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [环境启动问题](#环境启动问题)
3. [服务连接问题](#服务连接问题)
4. [数据库问题](#数据库问题)
5. [API接口问题](#api接口问题)
6. [性能问题](#性能问题)
7. [监控告警问题](#监控告警问题)
8. [数据问题](#数据问题)
9. [网络问题](#网络问题)
10. [安全认证问题](#安全认证问题)
11. [高级排查工具](#高级排查工具)

---

## 概述

### 排查原则

1. **从简单到复杂**: 先检查常见问题，再深入排查
2. **从外到内**: 先检查网络和连接，再检查服务内部
3. **从现象到原因**: 根据错误现象定位可能的原因
4. **记录排查过程**: 记录所有排查步骤和发现

### 常用工具

| 工具 | 用途 | 安装方式 |
|------|------|---------|
| **docker-compose** | 容器管理 | 系统预装 |
| **curl** | API测试 | `apt-get install curl` |
| **ping/telnet** | 网络测试 | 系统预装 |
| **jq** | JSON处理 | `apt-get install jq` |
| **postgresql-client** | 数据库连接 | `apt-get install postgresql-client` |
| **redis-cli** | Redis连接 | `apt-get install redis-tools` |

## 环境启动问题

### 问题1: Docker Compose启动失败

#### 现象
```bash
$ docker-compose -f docker-compose.test.yml up -d
ERROR: Couldn't connect to Docker daemon at http+docker://localhost - is it running?
```

#### 排查步骤

1. **检查Docker服务状态**
   ```bash
   # 检查Docker服务是否运行
   systemctl status docker
   
   # 如果未运行，启动Docker服务
   sudo systemctl start docker
   ```

2. **检查Docker权限**
   ```bash
   # 检查当前用户是否在docker组
   groups $USER
   
   # 如果不在，添加用户到docker组
   sudo usermod -aG docker $USER
   # 需要重新登录生效
   ```

3. **检查端口冲突**
   ```bash
   # 检查端口是否被占用
   netstat -tulpn | grep :8080
   
   # 如果端口被占用，修改docker-compose配置或停止占用进程
   ```

#### 解决方案
- 确保Docker服务正常运行
- 确保当前用户有Docker权限
- 解决端口冲突问题

### 问题2: 服务启动后立即退出

#### 现象
```bash
$ docker-compose -f docker-compose.test.yml ps
NAME                COMMAND                  STATE     PORTS
test-env-api        "java -jar app.jar"      Exit 1
```

#### 排查步骤

1. **查看服务日志**
   ```bash
   # 查看特定服务日志
   docker-compose -f docker-compose.test.yml logs api
   
   # 查看所有服务日志
   docker-compose -f docker-compose.test.yml logs
   ```

2. **检查环境变量配置**
   ```bash
   # 检查环境变量文件
   cat .env.test | grep API_
   
   # 检查必需的环境变量
   docker-compose -f docker-compose.test.yml config | grep -A5 -B5 api
   ```

3. **检查依赖服务**
   ```bash
   # 检查数据库是否正常
   docker-compose -f docker-compose.test.yml ps | grep postgres
   
   # 检查Redis是否正常
   docker-compose -f docker-compose.test.yml ps | grep redis
   ```

#### 解决方案
- 根据日志错误信息修复配置问题
- 确保依赖服务正常运行
- 检查内存和资源限制

### 问题3: 健康检查失败

#### 现象
```bash
$ docker-compose -f docker-compose.test.yml ps
NAME                STATE    HEALTH STATUS
test-env-api        Running  Unhealthy
```

#### 排查步骤

1. **检查健康检查配置**
   ```bash
   # 查看健康检查定义
   docker-compose -f docker-compose.test.yml config | grep -A10 healthcheck
   
   # 手动执行健康检查
   curl -f http://localhost:8080/health || echo "健康检查失败"
   ```

2. **检查服务实际状态**
   ```bash
   # 检查服务进程
   docker exec test-env-api ps aux
   
   # 检查服务端口
   docker exec test-env-api netstat -tulpn
   ```

3. **检查资源限制**
   ```bash
   # 检查容器资源使用
   docker stats test-env-api --no-stream
   
   # 检查日志中的OOM错误
   docker logs test-env-api | grep -i "out of memory\|oom"
   ```

#### 解决方案
- 调整健康检查超时时间
- 增加容器资源限制
- 修复服务内部问题

## 服务连接问题

### 问题4: 服务无法访问

#### 现象
```bash
$ curl http://localhost:8080/health
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

#### 排查步骤

1. **检查服务是否运行**
   ```bash
   # 检查容器状态
   docker ps | grep test-env-api
   
   # 检查服务端口映射
   docker port test-env-api 8080
   ```

2. **检查防火墙设置**
   ```bash
   # 检查防火墙状态
   sudo ufw status
   
   # 如果防火墙开启，添加规则
   sudo ufw allow 8080/tcp
   ```

3. **检查网络配置**
   ```bash
   # 检查Docker网络
   docker network ls
   
   # 检查容器网络连接
   docker network inspect test-env_default
   ```

#### 解决方案
- 确保服务正常运行
- 配置正确的防火墙规则
- 检查网络配置

### 问题5: 服务间通信失败

#### 现象
```bash
# API服务无法连接数据库
ERROR: could not connect to server: Connection refused
```

#### 排查步骤

1. **检查依赖服务**
   ```bash
   # 检查数据库服务状态
   docker-compose -f docker-compose.test.yml ps postgres
   
   # 检查数据库日志
   docker-compose -f docker-compose.test.yml logs postgres
   ```

2. **测试网络连通性**
   ```bash
   # 从API容器测试连接数据库
   docker exec test-env-api ping postgres
   
   # 测试端口连通性
   docker exec test-env-api telnet postgres 5432
   ```

3. **检查连接配置**
   ```bash
   # 检查数据库连接配置
   docker exec test-env-api env | grep DB_
   
   # 检查数据库认证配置
   docker exec test-env-postgres cat /var/lib/postgresql/data/pg_hba.conf
   ```

#### 解决方案
- 确保依赖服务正常运行
- 检查网络连通性
- 验证连接配置

## 数据库问题

### 问题6: 数据库连接失败

#### 现象
```bash
ERROR: FATAL: password authentication failed for user "postgres"
```

#### 排查步骤

1. **检查数据库用户和密码**
   ```bash
   # 检查环境变量
   echo $POSTGRES_PASSWORD
   
   # 检查docker-compose配置
   grep POSTGRES_PASSWORD docker-compose.test.yml
   ```

2. **检查数据库权限**
   ```bash
   # 进入数据库容器
   docker exec -it test-env-postgres psql -U postgres
   
   # 检查用户权限
   \du
   
   # 检查数据库权限
   \l
   ```

3. **检查数据库状态**
   ```bash
   # 检查数据库服务状态
   docker exec test-env-postgres pg_isready
   
   # 检查数据库日志
   docker logs test-env-postgres --tail 50
   ```

#### 解决方案
- 使用正确的数据库密码
- 检查用户权限配置
- 重启数据库服务

### 问题7: 数据库性能问题

#### 现象
```bash
# 查询响应缓慢
[SLOW QUERY] SELECT * FROM users WHERE ...
```

#### 排查步骤

1. **检查数据库负载**
   ```bash
   # 查看数据库连接数
   docker exec test-env-postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
   
   # 查看慢查询
   docker exec test-env-postgres psql -U postgres -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"
   ```

2. **检查数据库配置**
   ```bash
   # 查看数据库配置
   docker exec test-env-postgres psql -U postgres -c "SHOW ALL;" | grep -E "(shared_buffers|work_mem|maintenance_work_mem)"
   ```

3. **检查索引使用**
   ```bash
   # 检查表索引
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "\di"
   
   # 检查索引使用情况
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT * FROM pg_stat_user_indexes;"
   ```

#### 解决方案
- 优化慢查询
- 添加缺失的索引
- 调整数据库配置参数

### 问题8: 数据库空间不足

#### 现象
```bash
ERROR: could not extend file "base/16384/12345": No space left on device
```

#### 排查步骤

1. **检查磁盘空间**
   ```bash
   # 检查主机磁盘空间
   df -h
   
   # 检查Docker磁盘使用
   docker system df
   ```

2. **检查数据库大小**
   ```bash
   # 检查数据库大小
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT pg_database_size('ai_ready') / 1024 / 1024 as size_mb;"
   
   # 检查表大小
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC LIMIT 10;"
   ```

3. **清理无用数据**
   ```bash
   # 清理测试数据
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "DELETE FROM test_data WHERE created_at < NOW() - INTERVAL '7 days';"
   
   # 清理WAL日志
   docker exec test-env-postgres psql -U postgres -c "SELECT pg_current_wal_lsn();"
   ```

#### 解决方案
- 清理无用数据
- 扩展磁盘空间
- 优化数据存储

## API接口问题

### 问题9: API响应错误

#### 现象
```bash
$ curl http://localhost:8080/api/v1/users
{"error": "Internal Server Error", "status": 500}
```

#### 排查步骤

1. **查看API日志**
   ```bash
   # 查看API服务日志
   docker-compose -f docker-compose.test.yml logs api --tail 100
   
   # 查看错误详情
   docker exec test-env-api cat /app/logs/application.log | tail -50
   ```

2. **检查API配置**
   ```bash
   # 检查API配置
   docker exec test-env-api cat /app/config/application.yml
   
   # 检查环境变量
   docker exec test-env-api env | grep -E "(SPRING|DATABASE|REDIS)"
   ```

3. **调试API端点**
   ```bash
   # 使用详细模式调用API
   curl -v http://localhost:8080/api/v1/users
   
   # 检查健康端点
   curl http://localhost:8080/health
   ```

#### 解决方案
- 根据日志修复代码错误
- 检查配置正确性
- 重启API服务

### 问题10: API认证失败

#### 现象
```bash
$ curl -H "Authorization: Bearer invalid-token" http://localhost:8080/api/v1/users
{"error": "Unauthorized", "status": 401}
```

#### 排查步骤

1. **检查认证配置**
   ```bash
   # 检查JWT配置
   docker exec test-env-api env | grep JWT
   
   # 检查认证服务
   curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
   ```

2. **验证Token**
   ```bash
   # 获取有效Token
   TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
   
   # 使用Token测试
   curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
   ```

3. **检查权限配置**
   ```bash
   # 检查用户权限
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT * FROM user_roles WHERE username='admin';"
   ```

#### 解决方案
- 使用正确的认证信息
- 检查JWT配置
- 验证用户权限

## 性能问题

### 问题11: API响应缓慢

#### 现象
```bash
# API响应时间超过2秒
$ time curl http://localhost:8080/api/v1/users
real    0m3.456s
```

#### 排查步骤

1. **检查服务负载**
   ```bash
   # 查看容器资源使用
   docker stats test-env-api --no-stream
   
   # 查看API服务CPU/内存使用
   docker exec test-env-api top -b -n 1 | head -20
   ```

2. **检查数据库性能**
   ```bash
   # 检查数据库连接池
   docker exec test-env-api curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
   
   # 检查慢查询
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
   ```

3. **检查缓存命中率**
   ```bash
   # 检查Redis状态
   docker exec test-env-redis redis-cli info stats | grep -E "(keyspace_hits|keyspace_misses)"
   
   # 检查缓存配置
   docker exec test-env-api env | grep CACHE
   ```

#### 解决方案
- 优化数据库查询
- 增加缓存使用
- 调整服务资源配置

### 问题12: 内存泄漏

#### 现象
```bash
# 服务内存使用持续增长
$ docker stats test-env-api --no-stream
CONTAINER   CPU %   MEM USAGE / LIMIT   MEM %
test-env-api 45.23%  1.2GiB / 2GiB       60.00%
```

#### 排查步骤

1. **检查内存使用趋势**
   ```bash
   # 监控内存使用
   watch -n 5 'docker stats test-env-api --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"'
   
   # 查看GC日志
   docker exec test-env-api cat /app/logs/gc.log | tail -50
   ```

2. **生成堆转储**
   ```bash
   # 获取进程ID
   PID=$(docker inspect test-env-api --format '{{.State.Pid}}')
   
   # 生成堆转储（需要jmap）
   jmap -dump:live,format=b,file=heapdump.hprof $PID
   ```

3. **分析内存使用**
   ```bash
   # 使用jstat查看GC情况
   jstat -gc $PID 1000 10
   
   # 使用jconsole连接
   jconsole localhost:<jmx-port>
   ```

#### 解决方案
- 优化代码内存使用
- 调整JVM参数
- 定期重启服务

## 监控告警问题

### 问题13: 监控数据缺失

#### 现象
```bash
# Grafana面板显示"No Data"
```

#### 排查步骤

1. **检查Prometheus状态**
   ```bash
   # 检查Prometheus服务
   docker-compose -f docker-compose.test.yml ps prometheus
   
   # 检查Prometheus目标
   curl http://localhost:9090/api/v1/targets
   ```

2. **检查指标暴露**
   ```bash
   # 检查应用指标端点
   curl http://localhost:8080/actuator/prometheus
   
   # 检查服务发现
   curl http://localhost:8080/actuator/health
   ```

3. **检查网络连通性**
   ```bash
   # 从Prometheus容器测试连接
   docker exec test-env-prometheus curl -s http://api:8080/actuator/prometheus | head -5
   ```

#### 解决方案
- 确保应用正确暴露指标
- 检查Prometheus配置
- 验证网络连通性

### 问题14: 告警未触发

#### 现象
```bash
# 服务异常但未收到告警
```

#### 排查步骤

1. **检查AlertManager配置**
   ```bash
   # 检查AlertManager状态
   docker-compose -f docker-compose.test.yml ps alertmanager
   
   # 检查告警规则
   docker exec test-env-prometheus cat /etc/prometheus/alert_rules.yml | head -50
   ```

2. **检查告警路由**
   ```bash
   # 查看告警配置
   docker exec test-env-alertmanager cat /etc/alertmanager/alertmanager.yml
   
   # 检查告警历史
   curl http://localhost:9093/api/v2/alerts
   ```

3. **测试告警触发**
   ```bash
   # 手动触发告警测试
   curl -X POST http://localhost:9093/api/v1/alerts -d '[{"labels":{"alertname":"TestAlert"}}]'
   ```

#### 解决方案
- 检查告警规则配置
- 验证告警路由设置
- 测试告警通知渠道

## 数据问题

### 问题15: 数据不一致

#### 现象
```bash
# 不同服务间的数据不一致
```

#### 排查步骤

1. **检查数据同步**
   ```bash
   # 检查数据库数据
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT count(*) FROM users;"
   
   # 检查缓存数据
   docker exec test-env-redis redis-cli keys "user:*" | wc -l
   ```

2. **检查事务处理**
   ```bash
   # 查看事务日志
   docker exec test-env-api cat /app/logs/transaction.log | tail -50
   
   # 检查数据库事务
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT * FROM pg_stat_activity WHERE state = 'idle in transaction';"
   ```

3. **数据一致性检查**
   ```bash
   # 运行数据一致性检查脚本
   ./scripts/check-data-consistency.sh
   ```

#### 解决方案
- 修复数据同步逻辑
- 优化事务处理
- 实现数据校验机制

### 问题16: 测试数据污染

#### 现象
```bash
# 测试数据影响其他测试用例
```

#### 排查步骤

1. **检查数据隔离**
   ```bash
   # 检查测试数据范围
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT DISTINCT test_suite FROM test_data;"
   
   # 检查数据清理情况
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT count(*) FROM orphan_data;"
   ```

2. **检查测试框架配置**
   ```bash
   # 检查测试配置
   cat test-config.yml | grep -A5 -B5 "data.isolation"
   
   # 检查测试清理钩子
   grep -r "@After" src/test/
   ```

3. **分析数据流向**
   ```bash
   # 跟踪数据创建和使用
   ./scripts/trace-data-flow.sh --test-case test_user_creation
   ```

#### 解决方案
- 加强数据隔离机制
- 完善测试清理逻辑
- 使用独立测试数据库

## 网络问题

### 问题17: 网络延迟高

#### 现象
```bash
# 服务间通信延迟高
$ ping postgres
PING postgres (172.20.0.3) 56(84) bytes of data.
64 bytes from 172.20.0.3: icmp_seq=1 ttl=64 time=150.3 ms
```

#### 排查步骤

1. **检查网络拓扑**
   ```bash
   # 查看Docker网络
   docker network inspect test-env_default
   
   # 检查网络配置
   docker exec test-env-api ip addr show
   ```

2. **检查网络负载**
   ```bash
   # 检查网络流量
   docker exec test-env-api iftop -i eth0
   
   # 检查网络连接数
   docker exec test-env-api netstat -an | grep ESTABLISHED | wc -l
   ```

3. **检查DNS解析**
   ```bash
   # 检查DNS解析
   docker exec test-env-api nslookup postgres
   
   # 检查hosts文件
   docker exec test-env-api cat /etc/hosts
   ```

#### 解决方案
- 优化网络配置
- 减少网络跳转
- 使用更高效的通信协议

### 问题18: 端口冲突

#### 现象
```bash
# 服务启动失败，端口已被占用
ERROR: for test-env-api  Cannot start service api: driver failed programming external connectivity on endpoint test-env-api: Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use
```

#### 排查步骤

1. **查找占用进程**
   ```bash
   # 查找占用端口的进程
   sudo lsof -i :8080
   
   # 使用netstat查找
   sudo netstat -tulpn | grep :8080
   ```

2. **检查服务配置**
   ```bash
   # 检查端口映射配置
   grep -A2 -B2 "ports" docker-compose.test.yml
   
   # 检查已运行的服务
   docker-compose -f docker-compose.test.yml ps
   ```

3. **选择替代端口**
   ```bash
   # 查找可用端口
   ./scripts/find-available-port.sh --start 8080 --end 8090
   ```

#### 解决方案
- 停止占用进程
- 修改服务端口
- 使用端口映射

## 安全认证问题

### 问题19: SSL/TLS证书问题

#### 现象
```bash
# HTTPS连接失败
curl: (60) SSL certificate problem: self signed certificate
```

#### 排查步骤

1. **检查证书配置**
   ```bash
   # 检查证书文件
   ls -la /opt/ai-ready/certs/
   
   # 检查证书有效期
   openssl x509 -in /opt/ai-ready/certs/server.crt -text -noout | grep -A2 -B2 "Not"
   ```

2. **检查服务配置**
   ```bash
   # 检查SSL配置
   docker exec test-env-api cat /app/config/ssl.properties
   
   # 检查Nginx配置
   docker exec test-env-nginx cat /etc/nginx/nginx.conf | grep -A5 -B5 ssl
   ```

3. **测试SSL连接**
   ```bash
   # 测试SSL连接
   openssl s_client -connect localhost:8443 -showcerts
   
   # 使用curl跳过证书验证（仅测试）
   curl -k https://localhost:8443/health
   ```

#### 解决方案
- 更新SSL证书
- 配置正确的证书链
- 调整客户端证书验证

### 问题20: 访问控制问题

#### 现象
```bash
# 未授权访问成功
$ curl http://localhost:8080/api/v1/admin/users
[返回管理员数据]
```

#### 排查步骤

1. **检查权限配置**
   ```bash
   # 检查权限配置
   docker exec test-env-api cat /app/config/security.yml
   
   # 检查角色权限映射
   docker exec test-env-postgres psql -U postgres -d ai_ready -c "SELECT * FROM role_permissions;"
   ```

2. **测试访问控制**
   ```bash
   # 使用普通用户测试
   USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"user","password":"user123"}' | jq -r '.token')
   
   curl -H "Authorization: Bearer $USER_TOKEN" http://localhost:8080/api/v1/admin/users
   ```

3. **检查安全头**
   ```bash
   # 检查安全响应头
   curl -I http://localhost:8080/api/v1/admin/users | grep -E "(X-Content-Type-Options|X-Frame-Options|Content-Security-Policy)"
   ```

#### 解决方案
- 修复权限配置
- 加强访问控制
- 添加安全响应头

## 高级排查工具

### 性能分析工具

#### APM工具
```bash
# 使用Java Flight Recorder
docker exec test-env-api jcmd 1 JFR.start duration=60s filename=/tmp/recording.jfr

# 使用async-profiler
docker exec test-env-api ./profiler.sh -d 30 -f /tmp/flamegraph.html 1
```

#### 网络分析工具
```bash
# 使用tcpdump抓包
docker exec test-env-api tcpdump -i eth0 -w /tmp/capture.pcap

# 使用Wireshark分析（需要在主机分析）
docker cp test-env-api:/tmp/capture.pcap .
wireshark capture.pcap
```

### 日志分析工具

#### ELK Stack
```bash
# 发送日志到ELK
docker exec test-env-api curl -X POST http://elk:9200/logs/_doc -H "Content-Type: application/json" -d '{"message":"test log", "level":"INFO"}'

# 查询日志
curl -X GET "http://elk:9200/logs/_search?q=level:ERROR"
```

#### 日志聚合
```bash
# 使用Loki聚合日志
docker-compose -f docker-compose.test.yml logs --tail=1000 --timestamps | ./scripts/send-to-loki.sh

# 查询聚合日志
logcli query '{app="ai-ready-api"} |= "ERROR"'
```

### 监控告警工具

#### 自定义监控
```bash
# 添加自定义指标
docker exec test-env-api curl -X POST http://localhost:8080/actuator/metrics/custom.business.transaction.count -H "Content-Type: application/json" -d '{"value": 100}'

# 创建自定义告警规则
cat > custom_alerts.yml << EOF
groups:
  - name: business_alerts
    rules:
    - alert: HighTransactionRate
      expr: rate(custom_business_transaction_count_total[5m]) > 1000
      for: 5m
EOF
docker cp custom_alerts.yml test-env-prometheus:/etc/prometheus/
```

#### 告警测试
```bash
# 测试告警规则
curl -X POST http://localhost:9090/api/v1/rules/reload

# 查看告警状态
curl http://localhost:9090/api/v1/alerts
```

### 自动化排查脚本

#### 环境诊断脚本
```bash
#!/bin/bash
# environment-diagnostic.sh

echo "=== 环境诊断报告 ==="
echo "生成时间: $(date)"
echo ""

echo "1. 服务状态检查"
docker-compose -f docker-compose.test.yml ps
echo ""

echo "2. 资源使用情况"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
echo ""

echo "3. 网络连通性测试"
for service in api postgres redis; do
  if docker-compose -f docker-compose.test.yml ps $service | grep -q "Up"; then
    echo "✓ $service 运行正常"
  else
    echo "✗ $service 运行异常"
  fi
done
echo ""

echo "4. 关键指标检查"
curl -s http://localhost:8080/health | jq .
echo ""

echo "5. 最近错误日志"
docker-compose -f docker-compose.test.yml logs --tail=20 | grep -E "(ERROR|WARN|Exception)"
```

#### 问题自动修复脚本
```bash
#!/bin/bash
# auto-fix-common-issues.sh

echo "开始自动修复常见问题..."

# 1. 检查并重启异常服务
for service in $(docker-compose -f docker-compose.test.yml ps --services | xargs); do
  status=$(docker-compose -f docker-compose.test.yml ps $service | grep -o "Exit")
  if [ "$status" = "Exit" ]; then
    echo "重启异常服务: $service"
    docker-compose -f docker-compose.test.yml restart $service
  fi
done

# 2. 清理Docker资源
echo "清理Docker资源..."
docker system prune -f

# 3. 检查磁盘空间
echo "检查磁盘空间..."
df -h /

# 4. 重启健康检查失败的服务
for container in $(docker ps --filter "health=unhealthy" --format "{{.Names}}"); do
  echo "重启健康检查失败容器: $container"
  docker restart $container
done

echo "自动修复完成！"
```

---

**文档版本**: 1.0.0  
**最后更新**: 2026-04-27  
**维护责任人**: devops-engineer  
**审核状态**: ✅ 已审核

> **注意**: 本指南会随环境更新而更新，建议定期查看最新版本。对于复杂问题，建议联系技术支持团队。