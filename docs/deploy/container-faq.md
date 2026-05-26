# 容器化部署故障排查手册

> **Sprint 27+1 测试环境优化**  
> **最后更新**: 2026-04-27

---

## 一、部署失败排查

### 1.1 Docker Compose 启动失败

**症状**: `docker-compose up` 命令失败

**排查步骤**:

1. **检查Docker服务状态**
   ```bash
   docker --version
   docker info
   systemctl status docker  # Linux
   ```

2. **检查配置文件语法**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml config
   ```

3. **查看详细错误日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml up --force-recreate
   ```

**常见错误**:

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| Service is already active | 端口已被占用 | 修改端口映射，或停止占用进程 |
| Volume not found | 数据卷不存在 | 运行 `docker-compose down` 清理 |
| Network not found | 网络不存在 | 添加 `networks:` 配置 |

### 1.2 服务健康检查失败

**症状**: 容器启动后立即退出

**排查步骤**:

1. **查看容器日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs -f backend
   ```

2. **检查健康检查配置**
   ```yaml
   healthcheck:
     test: ["CMD", "wget", "--no-verbose", "--spider", "http://localhost:8081/actuator/health"]
     interval: 30s
     timeout: 10s
     retries: 3
   ```

3. **测试健康检查命令**
   ```bash
   docker exec -it qizhilian-backend wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health
   ```

---

## 二、运行时故障排查

### 2.1 后端服务无法访问

**症状**: 访问 `http://localhost:80/health` 无响应

**排查步骤**:

1. **检查容器状态**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml ps
   ```

2. **检查容器日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs backend
   ```

3. **检查端口占用**
   ```bash
   # Linux
   netstat -tulpn | grep 8081
   
   # Windows
   netstat -ano | findstr :8081
   ```

4. **测试容器内服务**
   ```bash
   docker exec -it qizhilian-backend wget -qO- http://localhost:8081/actuator/health
   ```

### 2.2 数据库连接失败

**症状**: 后端日志显示 `Connection refused` 或 `Connection timeout`

**排查步骤**:

1. **检查PostgreSQL容器状态**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml ps postgres
   ```

2. **检查PostgreSQL日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs postgres
   ```

3. **测试数据库连接**
   ```bash
   docker exec -it qizhilian-postgres psql -U appuser -d qizhilian -c "SELECT 1"
   ```

4. **检查网络连接**
   ```bash
   docker exec -it qizhilian-backend ping postgres
   docker exec -it qizhilian-backend nc -zv postgres 5432
   ```

**配置检查**:

```yaml
# 确保backend的环境变量正确
environment:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/qizhilian
```

### 2.3 Redis连接失败

**症状**: 后端日志显示 `Redis connection failed`

**排查步骤**:

1. **检查Redis容器状态**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml ps redis
   ```

2. **测试Redis连接**
   ```bash
   docker exec -it qizhilian-redis redis-cli ping
   ```

3. **检查后端日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs backend | grep Redis
   ```

### 2.4 RabbitMQ连接失败

**症状**: 后端日志显示 `RabbitMQ connection failed`

**排查步骤**:

1. **检查RabbitMQ容器状态**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml ps rabbitmq
   ```

2. **测试RabbitMQ连接**
   ```bash
   docker exec -it qizhilian-rabbitmq rabbitmq-diagnostics check_running
   ```

3. **查看RabbitMQ日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs rabbitmq
   ```

---

## 三、性能问题排查

### 3.1 服务响应慢

**症状**: API响应时间超过2秒

**排查步骤**:

1. **检查系统资源**
   ```bash
   docker stats  # 查看容器资源使用
   ```

2. **检查CPU使用**
   ```bash
   docker top qizhilian-backend
   ```

3. **检查JVM内存**
   ```bash
   # 进入容器
   docker exec -it qizhilian-backend sh
   
   # 查看Java进程
   ps aux | grep java
   
   # 查看JVM内存
   jstat -gc <pid>
   ```

4. **检查数据库性能**
   ```bash
   # 连接到PostgreSQL
   docker exec -it qizhilian-postgres psql -U appuser -d qizhilian
   
   # 查看慢查询
   SELECT * FROM pg_stat_activity WHERE state = 'active' AND query_duration > '5s';
   ```

### 3.2 内存泄漏

**症状**: 容器OOM（Out of Memory）被杀死

**排查步骤**:

1. **检查OOM日志**
   ```bash
   docker events --filter event=oom
   ```

2. **检查容器退出原因**
   ```bash
   docker inspect qizhilian-backend --format='{{.State.OOMKilled}}'
   ```

3. **生成堆转储**
   ```bash
   # 在容器内执行
   docker exec -it qizhilian-backend jmap -dump:format=b,file=/app/heapdump.hprof <pid>
   ```

4. **分析堆转储**
   - 使用 Eclipse MAT 或 VisualVM 打开 heapdump.hprof

**临时解决方案**:
```yaml
# 增加内存限制
deploy:
  resources:
    limits:
      memory: 1536M  # 从1G增加到1.5G
```

---

## 四、数据备份与恢复

### 4.1 备份验证

**验证PostgreSQL备份**:
```bash
# 创建测试数据库
docker exec -it qizhilian-postgres psql -U appuser -c "CREATE DATABASE qizhilian_test;"

# 恢复备份
docker exec -i qizhilian-postgres psql -U appuser -d qizhilian_test < /tmp/postgres_backup.sql

# 验证数据
docker exec -it qizhilian-postgres psql -U appuser -d qizhilian_test -c "SELECT COUNT(*) FROM users;"
```

**验证Redis备份**:
```bash
# 检查备份文件
docker exec -it qizhilian-redis ls -lh /data/dump.rdb

# 恢复备份
docker cp redis_backup.rdb qizhilian-redis:/data/dump.rdb
docker exec -it qizhilian-redis redis-cli BGSAVE
```

### 4.2 自动化备份验证

创建每日备份验证脚本 `scripts/backup-verify.sh`:
```bash
#!/bin/bash
BACKUP_DATE=$(date +%Y%m%d)

# 验证PostgreSQL备份
if [ -f "backups/test/$BACKUP_DATE/postgres_$BACKUP_DATE.sql" ]; then
    echo "PostgreSQL备份存在: $BACKUP_DATE"
else
    echo "[ERROR] PostgreSQL备份缺失: $BACKUP_DATE"
    exit 1
fi

# 验证Redis备份
if [ -f "backups/test/$BACKUP_DATE/redis_dump.rdb" ]; then
    echo "Redis备份存在: $BACKUP_DATE"
else
    echo "[ERROR] Redis备份缺失: $BACKUP_DATE"
    exit 1
fi

echo "所有备份验证通过"
```

---

## 五、日志收集与分析

### 5.1 容器日志位置

| 服务 | 日志位置 | 查看命令 |
|------|---------|---------|
| 后端 | `/app/logs` | `docker logs qizhilian-backend` |
| PostgreSQL | `/var/lib/postgresql/data/log` | `docker logs qizhilian-postgres` |
| Redis | 日志输出到stdout | `docker logs qizhilian-redis` |
| RabbitMQ | `/var/log/rabbitmq` | `docker logs qizhilian-rabbitmq` |
| Nginx | `/var/log/nginx` | `docker logs qizhilian-nginx` |

### 5.2 实时日志监控

```bash
# 单个服务日志
docker-compose -f infra/docker/docker-compose.optimized.yml logs -f backend

# 所有服务日志
docker-compose -f infra/docker/docker-compose.optimized.yml logs -f

# 搜索特定关键词
docker-compose -f infra/docker/docker-compose.optimized.yml logs | grep "ERROR"
```

### 5.3 查看最近100行日志

```bash
docker-compose -f infra/docker/docker-compose.optimized.yml logs --tail=100 backend
```

---

## 六、紧急情况处理

### 6.1 服务崩溃

**症状**: 服务容器退出

**处理步骤**:

1. **查看退出原因**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml ps
   docker inspect <container_id> --format='{{.State.ExitCode}}'
   docker inspect <container_id> --format='{{.State.Error}}'
   ```

2. **查看最终日志**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml logs --tail=100 <service>
   ```

3. **重启服务**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml restart <service>
   ```

4. **强制重建**
   ```bash
   docker-compose -f infra/docker/docker-compose.optimized.yml up -d --force-recreate <service>
   ```

### 6.2 数据库崩溃

**症状**: PostgreSQL容器退出

**处理步骤**:

1. **检查数据卷挂载**
   ```bash
   docker inspect qizhilian-postgres | grep Mounts
   ```

2. **检查数据文件**
   ```bash
   docker exec -it qizhilian-postgres ls -lh /var/lib/postgresql/data
   ```

3. **尝试修复**
   ```bash
   docker exec -it qizhilian-postgres pg_ctl -D /var/lib/postgresql/data start
   ```

4. **回滚到备份**
   ```bash
   scripts/deploy/optimized/rollback.sh <backup_timestamp>
   ```

---

## 七、最佳实践 checklist

### 部署前检查
- [ ] Docker和docker-compose已安装
- [ ] 磁盘空间充足（>10GB）
- [ ] 端口未被占用
- [ ] 配置文件已备份
- [ ] 备份脚本已运行

### 部署后检查
- [ ] 所有容器状态正常
- [ ] 健康检查通过
- [ ] API可正常访问
- [ ] 数据库连接正常
- [ ] 监控数据正常采集

### 运行时检查
- [ ] 每日备份已执行
- [ ] 告警阈值合理
- [ ] 日志大小正常（<100MB/服务）
- [ ] 磁盘空间>50%

---

*手册完成时间: 2026-04-27*  
*更新版本: v1.0*