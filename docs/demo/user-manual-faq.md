# 常见问题解答 (FAQ)

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: mnj0j12k  
**最后更新**: 2026-04-27  

---

## 目录

1. [环境搭建问题](#环境搭建问题)
2. [服务配置问题](#服务配置问题)
3. [监控告警问题](#监控告警问题)
4. [性能优化问题](#性能优化问题)
5. [故障排查问题](#故障排查问题)
6. [其他问题](#其他问题)

---

## 环境搭建问题

### Q1: Docker安装失败怎么办？

**问题描述**：安装Docker时提示系统不支持或安装失败。

**解决方案**：

1. **检查系统要求**
   - Windows 10/11 专业版或企业版（64位）
   - 启用Hyper-V和容器功能
   - 至少4GB内存

2. **启用WSL2**
   ```powershell
   wsl --install
   wsl --set-default-version 2
   ```

3. **检查虚拟化支持**
   ```powershell
   systeminfo | findstr /B /C:"Hyper-V 要求"
   ```

4. **重新安装Docker**
   - 下载最新版Docker Desktop
   - 以管理员身份运行安装程序
   - 重启电脑

**参考文档**：[Docker官方安装指南](https://docs.docker.com/desktop/install/windows-install/)

---

### Q2: 端口被占用怎么办？

**问题描述**：启动服务时提示端口已被占用。

**解决方案**：

1. **查找占用端口的进程**
   ```powershell
   netstat -ano | findstr :5433
   ```

2. **结束占用进程**
   ```powershell
   taskkill /PID <PID> /F
   ```

3. **修改端口映射**
   编辑`monitoring-alerting-deployment.yml`：
   ```yaml
   ports:
     - "5434:5432"  # 修改宿主机端口
   ```

**常见端口占用**：
| 服务 | 默认端口 | 替代端口 |
|------|---------|---------|
| PostgreSQL | 5433 | 5434 |
| Redis | 6380 | 6381 |
| RabbitMQ | 5673 | 5674 |
| Prometheus | 9090 | 9091 |
| Grafana | 3000 | 3001 |

---

### Q3: 内存不足无法启动怎么办？

**问题描述**：Docker提示内存不足，无法启动容器。

**解决方案**：

1. **增加Docker内存限制**
   - 打开Docker Desktop设置
   - Resources > Memory
   - 设置为至少8GB

2. **减少服务内存使用**
   ```yaml
   # 修改PostgreSQL内存
   command: >
     postgres
     -c shared_buffers=128MB
     -c effective_cache_size=512MB
   
   # 修改Redis内存
   command: redis-server --maxmemory 128mb
   ```

3. **关闭不必要的服务**
   ```bash
   # 只启动必要服务
   docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring
   ```

---

## 服务配置问题

### Q4: 如何修改数据库密码？

**问题描述**：需要修改PostgreSQL或Redis的默认密码。

**解决方案**：

1. **修改PostgreSQL密码**
   ```bash
   # 进入容器
   docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db
   
   # 修改密码
   ALTER USER monitoring_user WITH PASSWORD 'new_password';
   ```

2. **修改Redis密码**
   ```bash
   # 进入容器
   docker exec -it redis-monitoring redis-cli
   
   # 修改密码
   CONFIG SET requirepass new_password
   ```

3. **更新环境变量**
   修改`monitoring-alerting-deployment.yml`中的环境变量，然后重启服务。

---

### Q5: 如何添加新的监控目标？

**问题描述**：需要监控新的服务或应用。

**解决方案**：

1. **编辑Prometheus配置**
   ```yaml
   scrape_configs:
     - job_name: 'new-service'
       static_configs:
         - targets: ['new-service:8080']
       metrics_path: '/actuator/prometheus'
   ```

2. **重新加载Prometheus配置**
   ```bash
   curl -X POST http://localhost:9090/-/reload
   ```

3. **验证目标状态**
   - 打开Prometheus：http://localhost:9090
   - 导航到"Status" > "Targets"
   - 查看新目标状态

---

### Q6: Grafana默认密码是什么？

**问题描述**：首次登录Grafana不知道默认密码。

**解决方案**：

1. **默认凭据**
   - 用户名：admin
   - 密码：admin

2. **首次登录修改密码**
   - 登录后会提示修改密码
   - 建议设置强密码

3. **重置密码**
   ```bash
   docker exec -it grafana grafana-cli admin reset-admin-password new_password
   ```

---

## 监控告警问题

### Q7: 告警没有触发怎么办？

**问题描述**：配置了告警规则，但告警没有触发。

**解决方案**：

1. **检查告警规则语法**
   ```bash
   # 检查Prometheus规则
   docker exec -it prometheus promtool check rules /etc/prometheus/alerts.yml
   ```

2. **验证表达式**
   - 打开Prometheus：http://localhost:9090
   - 在查询框输入告警表达式
   - 查看是否有数据返回

3. **检查告警状态**
   - 导航到"Alerts"页面
   - 查看告警状态（Inactive/Pending/Firing）

4. **检查AlertManager配置**
   ```bash
   docker logs alertmanager
   ```

---

### Q8: 告警通知没有收到怎么办？

**问题描述**：告警触发了，但没有收到通知。

**解决方案**：

1. **检查AlertManager配置**
   ```yaml
   route:
     receiver: 'dingtalk-webhook'
     group_by: ['alertname']
     group_wait: 30s
     group_interval: 5m
     repeat_interval: 1h
   ```

2. **检查通知渠道**
   - 确认webhook URL正确
   - 测试webhook是否可达
   - 检查防火墙设置

3. **查看AlertManager日志**
   ```bash
   docker logs alertmanager
   ```

4. **测试通知**
   ```bash
   curl -X POST http://localhost:9093/-/reload
   ```

---

### Q9: 如何临时禁用告警？

**问题描述**：需要进行维护，临时禁用某些告警。

**解决方案**：

1. **静默告警**
   - 打开AlertManager：http://localhost:9093
   - 点击"New Silence"
   - 设置静默时间和匹配条件

2. **修改告警规则**
   ```yaml
   # 注释掉不需要的告警规则
   # - alert: HighCPUUsage
   #   expr: ...
   ```

3. **停止AlertManager**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml stop alertmanager
   ```

---

## 性能优化问题

### Q10: 监控数据查询很慢怎么办？

**问题描述**：Grafana仪表盘加载缓慢，查询超时。

**解决方案**：

1. **优化查询时间范围**
   - 缩小时间范围
   - 使用合适的采样间隔

2. **优化查询表达式**
   ```promql
   # 不好的查询
   rate(http_requests_total[1h])
   
   # 优化后的查询
   rate(http_requests_total[5m])
   ```

3. **增加Prometheus资源**
   ```yaml
   # 增加内存限制
   deploy:
     resources:
       limits:
         memory: 2G
   ```

4. **启用查询缓存**
   ```yaml
   # 在Grafana中启用缓存
   [cache]
   enabled = true
   ```

---

### Q11: 数据库连接池不足怎么办？

**问题描述**：应用提示数据库连接池耗尽。

**解决方案**：

1. **增加最大连接数**
   ```yaml
   command: >
     postgres
     -c max_connections=300
   ```

2. **优化连接池配置**
   ```yaml
   # 应用端配置
   spring.datasource.hikari.maximum-pool-size=50
   spring.datasource.hikari.minimum-idle=10
   spring.datasource.hikari.connection-timeout=30000
   spring.datasource.hikari.idle-timeout=600000
   spring.datasource.hikari.max-lifetime=1800000
   ```

3. **监控连接使用情况**
   ```sql
   SELECT * FROM pg_stat_activity WHERE state = 'active';
   ```

---

### Q12: Redis内存使用过高怎么办？

**问题描述**：Redis内存使用率持续增长。

**解决方案**：

1. **查看内存使用情况**
   ```bash
   docker exec -it redis-monitoring redis-cli INFO memory
   ```

2. **设置内存上限**
   ```yaml
   command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
   ```

3. **清理过期键**
   ```bash
   docker exec -it redis-monitoring redis-cli --eval cleanup.lua
   ```

4. **优化数据结构**
   - 使用Hash替代String存储对象
   - 设置合理的过期时间
   - 使用Pipeline减少内存碎片

---

## 故障排查问题

### Q13: 服务无法启动怎么排查？

**问题描述**：Docker容器启动后立即退出。

**解决方案**：

1. **查看容器日志**
   ```bash
   docker logs <container_name>
   ```

2. **检查配置文件**
   ```bash
   # 检查YAML语法
   docker exec -it <container> cat /etc/prometheus/prometheus.yml
   ```

3. **检查资源限制**
   ```bash
   docker stats
   ```

4. **检查依赖服务**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml ps
   ```

**常见原因**：
| 现象 | 原因 | 解决方案 |
|------|------|---------|
| 立即退出 | 配置错误 | 检查配置文件语法 |
| 启动慢 | 资源不足 | 增加内存/CPU限制 |
| 连接失败 | 网络问题 | 检查网络配置 |
| 权限错误 | 权限不足 | 检查文件权限 |

---

### Q14: 监控数据丢失怎么办？

**问题描述**：Prometheus中历史监控数据丢失。

**解决方案**：

1. **检查存储空间**
   ```bash
   docker exec -it prometheus df -h /prometheus
   ```

2. **检查数据保留策略**
   ```yaml
   storage:
     tsdb:
       retention: 30d
   ```

3. **检查数据目录权限**
   ```bash
   docker exec -it prometheus ls -la /prometheus
   ```

4. **备份恢复**
   ```bash
   # 从备份恢复
   docker cp backup/prometheus-data prometheus:/prometheus
   ```

---

### Q15: Grafana仪表盘显示"No Data"怎么办？

**问题描述**：Grafana面板显示"No Data"或数据为空。

**解决方案**：

1. **检查数据源**
   - 打开Grafana配置
   - 检查Prometheus数据源状态
   - 测试数据源连接

2. **检查查询表达式**
   ```promql
   # 在Prometheus中测试查询
   up{job="prometheus"}
   ```

3. **检查时间范围**
   - 确保时间范围设置正确
   - 检查数据是否存在

4. **检查权限**
   ```bash
   # 检查Prometheus访问权限
   curl http://localhost:9090/api/v1/query?query=up
   ```

---

## 其他问题

### Q16: 如何备份和恢复整个环境？

**解决方案**：

1. **备份数据**
   ```bash
   # 备份PostgreSQL
   docker exec postgres-monitoring pg_dump -U monitoring_user monitoring_db > backup.sql
   
   # 备份Redis
   docker exec redis-monitoring redis-cli BGSAVE
   docker cp redis-monitoring:/data/dump.rdb backup/
   
   # 备份Grafana配置
   docker cp grafana:/var/lib/grafana backup/
   ```

2. **备份配置文件**
   ```bash
   cp monitoring-alerting-deployment.yml backup/
   cp -r prometheus/ backup/
   cp -r alertmanager/ backup/
   cp -r grafana/ backup/
   ```

3. **恢复环境**
   ```bash
   # 恢复配置文件
   cp backup/monitoring-alerting-deployment.yml .
   
   # 恢复数据
   docker exec -i postgres-monitoring psql -U monitoring_user -d monitoring_db < backup.sql
   docker cp backup/dump.rdb redis-monitoring:/data/
   docker cp backup/grafana grafana:/var/lib/
   
   # 重启服务
   docker-compose -f monitoring-alerting-deployment.yml restart
   ```

---

### Q17: 如何升级服务版本？

**解决方案**：

1. **查看当前版本**
   ```bash
   docker images
   ```

2. **修改镜像版本**
   ```yaml
   postgres-monitoring:
     image: postgres:16-alpine  # 升级到16版本
   ```

3. **备份数据**
   ```bash
   docker exec postgres-monitoring pg_dumpall -U monitoring_user > backup.sql
   ```

4. **重新部署**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml pull
   docker-compose -f monitoring-alerting-deployment.yml up -d
   ```

---

### Q18: 如何查看服务运行状态？

**解决方案**：

1. **查看容器状态**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml ps
   ```

2. **查看资源使用**
   ```bash
   docker stats
   ```

3. **查看服务日志**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml logs -f
   ```

4. **健康检查**
   ```bash
   curl http://localhost:9090/-/healthy  # Prometheus
   curl http://localhost:3000/api/health  # Grafana
   ```

---

### Q19: 如何重置整个环境？

**解决方案**：

1. **停止所有服务**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml down
   ```

2. **删除数据卷**
   ```bash
   docker volume prune
   ```

3. **重新启动**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml up -d
   ```

**警告**：此操作将删除所有数据，请确保已备份重要数据。

---

### Q20: 如何获取技术支持？

**联系方式**：

| 支持类型 | 联系人 | 联系方式 |
|---------|--------|---------|
| 技术支援 | devops-engineer | devops@ai-ready.com |
| 文档反馈 | doc-writer | docs@ai-ready.com |
| 项目管理 | coordinator | pm@ai-ready.com |
| 紧急问题 | - | 24小时值班电话 |

**反馈方式**：
1. 提交Issue到项目仓库
2. 发送邮件到技术支持邮箱
3. 联系项目 coordinator

---

## 快速参考

### 常用命令速查

| 命令 | 用途 |
|------|------|
| `docker-compose up -d` | 启动所有服务 |
| `docker-compose down` | 停止所有服务 |
| `docker-compose ps` | 查看服务状态 |
| `docker-compose logs -f` | 查看实时日志 |
| `docker stats` | 查看资源使用 |

### 常用端口速查

| 服务 | 端口 | 用途 |
|------|------|------|
| PostgreSQL | 5433 | 数据库 |
| Redis | 6380 | 缓存 |
| RabbitMQ | 5673 | 消息队列 |
| Prometheus | 9090 | 监控 |
| AlertManager | 9093 | 告警 |
| Grafana | 3000 | 可视化 |
| Nginx | 80 | 反向代理 |

### 常用URL速查

| 服务 | URL | 默认账号 |
|------|-----|---------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| RabbitMQ | http://localhost:15672 | guest/guest |

---

## 相关文档

- [快速入门指南](user-manual-quick-start.md)
- [功能使用手册](user-manual-functional.md)
- [管理员配置手册](user-manual-admin.md)
- [快速入门视频脚本](videos/quick-start-script.md)
- [功能演示视频脚本](videos/features-script.md)
- [部署运维视频脚本](videos/deployment-script.md)

---

## 更新日志

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.0.0 | 2026-04-27 | 初始版本，包含20个常见问题 |

---

**文档状态**: ✅ 已完成  
**最后更新**: 2026-04-27  
**作者**: mnj0j12k