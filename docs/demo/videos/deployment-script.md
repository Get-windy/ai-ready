# 部署运维视频脚本

**视频时长**：10分钟  
**目标观众**：DevOps工程师、系统管理员  
**演示环境**：Sprint 27+1测试环境  
**技术栈**：PostgreSQL + Redis + RabbitMQ + Prometheus + AlertManager + Grafana  

---

## 00:00-01:00 引言

**画面**：欢迎界面，项目Logo  
**配音**：
```
欢迎观看AI-Ready测试环境部署运维视频。
本视频将带您了解测试环境的部署和日常运维操作。
整个演示约10分钟，涵盖部署、配置、监控和故障排查。
```

---

## 01:00-02:00 部署流程

**画面**：展示部署全流程  
**配音**：
```
**1. 部署流程**

让我们首先了解测试环境的完整部署流程。

**1.1 部署前准备**
- 检查系统要求
- 检查Docker和Docker Compose版本
- 准备部署文件
- 创建部署目录

**1.2 部署文件**
- monitoring-alerting-deployment.yml - 主配置文件
- prometheus.yml - Prometheus配置
- alertmanager.yml - AlertManager配置
- grafana.ini - Grafana配置
- nginx.conf - Nginx配置

**1.3 部署命令**
```bash
# 进入部署目录
cd deploy

# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 查看部署日志
docker-compose -f monitoring-alerting-deployment.yml logs -f

# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

**1.4 部署验证**
- 检查所有服务状态
- 验证各服务端口
- 测试服务间连接
- 验证监控数据采集
```

**操作演示**：
1. 显示部署目录结构
2. 执行部署命令
3. 显示服务状态

---

## 02:00-03:30 服务配置

**画面**：展示服务配置文件  
**配音**：
```
**2. 服务配置**

接下来，让我们了解各服务的配置方法。

**2.1 PostgreSQL配置**
- 修改docker-compose.yml中的环境变量
- 配置连接池参数
- 配置性能参数
- 重启服务生效

**2.2 Redis配置**
- 修改docker-compose.yml中的启动命令
- 配置内存限制
- 配置持久化参数
- 重启服务生效

**2.3 RabbitMQ配置**
- 修改docker-compose.yml中的环境变量
- 配置用户权限
- 配置队列参数
- 重启服务生效

**2.4 Prometheus配置**
- 修改prometheus.yml配置文件
- 添加目标服务
- 配置告警规则
- 重启Prometheus

**2.5 AlertManager配置**
- 修改alertmanager.yml配置文件
- 配置告警路由
- 配置通知模板
- 重启AlertManager

**2.6 Grafana配置**
- 修改grafana.ini配置文件
- 配置数据源
- 配置仪表盘
- 重启Grafana

**2.7 Nginx配置**
- 修改nginx.conf配置文件
- 配置反向代理
- 配置SSL
- 重启Nginx
```

**操作演示**：
1. 显示各个配置文件
2. 修改一个配置示例
3. 显示重启服务命令

---

## 03:30-05:00 日常运维

**画面**：展示日常运维操作  
**配音**：
```
**3. 日常运维**

现在，让我们了解日常运维操作。

**3.1 服务管理**
```bash
# 启动服务
docker-compose -f monitoring-alerting-deployment.yml start <service>

# 停止服务
docker-compose -f monitoring-alerting-deployment.yml stop <service>

# 重启服务
docker-compose -f monitoring-alerting-deployment.yml restart <service>

# 更新服务
docker-compose -f monitoring-alerting-deployment.yml up -d <service>

# 查看服务日志
docker-compose -f monitoring-alerting-deployment.yml logs -f <service>
```

**3.2 容器管理**
```bash
# 查看容器列表
docker ps -a

# 进入容器
docker exec -it <container_name> /bin/bash

# 查看容器状态
docker inspect <container_name>

# 查看容器资源使用
docker stats
```

**3.3 卷管理**
```bash
# 查看卷列表
docker volume ls

# 查看卷详情
docker volume inspect <volume_name>

# 删除未使用的卷
docker volume prune
```

**3.4 网络管理**
```bash
# 查看网络列表
docker network ls

# 查看网络详情
docker network inspect monitoring-network

# 删除网络
docker network rm monitoring-network
```

**3.5 备份和恢复**
```bash
# 备份PostgreSQL
docker exec postgres-monitoring pg_dump -U monitoring_user monitoring_db > backup.sql

# 恢复PostgreSQL
docker exec -i postgres-monitoring psql -U monitoring_user monitoring_db < backup.sql

# 备份Redis
docker exec redis-monitoring redis-cli -a redis_monitoring_pass_123 BGSAVE

# 备份数据卷
tar -czvf backup.tar.gz volumes/
```

**操作演示**：
1. 显示服务日志查看命令
2. 显示备份命令
3. 显示卷管理命令

---

## 05:00-07:00 性能监控

**画面**：展示性能监控指标  
**配音**：
```
**4. 性能监控**

接下来，让我们了解性能监控方法。

**4.1 系统性能**
```bash
# 查看容器资源使用
docker stats

# 查看系统负载
docker exec -it <container_name> top

# 查看磁盘IO
docker exec -it <container_name> iotop
```

**4.2 数据库性能**
```bash
# 查看PostgreSQL连接
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db -c 'SELECT * FROM pg_stat_activity;'

# 查看慢查询
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db -c 'SELECT * FROM pg_stat_statements;'

# 查看表大小
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db -c 'SELECT pg_size_pretty(pg_total_relation_size(''table_name''));'
```

**4.3 缓存性能**
```bash
# 查看Redis内存
docker exec -it redis-monitoring redis-cli -a redis_monitoring_pass_123 info memory

# 查看Redis键数量
docker exec -it redis-monitoring redis-cli -a redis_monitoring_pass_123 dbsize

# 查看Redis性能
docker exec -it redis-monitoring redis-cli -a redis_monitoring_pass_123 --latency
```

**4.4 消息队列性能**
```bash
# 查看RabbitMQ队列
docker exec -it rabbitmq-monitoring rabbitmqctl list_queues

# 查看RabbitMQ消费者
docker exec -it rabbitmq-monitoring rabbitmqctl list_consumers

# 查看RabbitMQ性能
docker exec -it rabbitmq-monitoring rabbitmq-diagnostics status
```

**4.5 Prometheus性能**
```bash
# 查看Prometheus状态
curl http://localhost:9090/api/v1/status/config

# 查看Prometheus指标
curl http://localhost:9090/api/v1/label

# 查看Prometheus存储大小
curl http://localhost:9090/api/v1/status/tsdb
```

**操作演示**：
1. 显示性能监控命令
2. 显示性能指标输出
3. 分析性能瓶颈

---

## 07:00-08:30 故障排查

**画面**：展示故障排查步骤  
**配音**：
```
**5. 故障排查**

最后，让我们了解常见的故障排查方法。

**5.1 故障诊断流程**
1. 检查服务状态
2. 查看服务日志
3. 检查资源使用
4. 分析错误信息
5. 执行修复操作
6. 验证修复效果

**5.2 常见问题**

**问题1：服务无法启动**
```bash
# 检查Docker是否运行
systemctl status docker

# 检查端口占用
netstat -tlnp | grep <port>

# 检查配置文件
docker-compose -f monitoring-alerting-deployment.yml config

# 查看日志
docker-compose -f monitoring-alerting-deployment.yml logs <service>
```

**问题2：服务响应缓慢**
```bash
# 检查资源使用
docker stats

# 检查容器CPU限制
docker inspect <container_name> | grep Cpu

# 检查容器内存限制
docker inspect <container_name> | grep Memory

# 查看慢查询
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db -c 'EXPLAIN ANALYZE SELECT ...;'
```

**问题3：监控数据丢失**
```bash
# 检查Prometheus存储
docker exec -it prometheus ls /prometheus

# 检查Prometheus日志
docker-compose -f monitoring-alerting-deployment.yml logs prometheus

# 检查磁盘空间
df -h

# 恢复数据
docker exec -it postgres-monitoring pg_restore -U monitoring_user -d monitoring_db backup.dump
```

**问题4：告警未发送**
```bash
# 检查AlertManager状态
curl http://localhost:9093/api/v1/status

# 检查告警路由
curl http://localhost:9093/api/v1/routes

# 查看AlertManager日志
docker-compose -f monitoring-alerting-deployment.yml logs alertmanager

# 重启AlertManager
docker-compose -f monitoring-alerting-deployment.yml restart alertmanager
```

**5.3 故障排查工具**
```bash
# 网络诊断
docker exec -it prometheus ping <service>
docker exec -it prometheus curl http://<service>:<port>

# 文件系统诊断
docker exec -it postgres-monitoring df -h /var/lib/postgresql/data

# 进程诊断
docker exec -it <service> ps aux

# 日志诊断
docker-compose -f monitoring-alerting-deployment.yml logs <service> | grep error
```

**操作演示**：
1. 模拟一个故障场景
2. 展示故障排查步骤
3. 执行修复操作

---

## 08:30-10:00 自动化运维

**画面**：展示自动化运维脚本  
**配音**：
```
**6. 自动化运维**

最后，让我们了解自动化运维方法。

**6.1 一键部署脚本**
```bash
# 创建部署脚本
cat > deploy.sh <<EOF
#!/bin/bash
cd deploy
docker-compose -f monitoring-alerting-deployment.yml up -d
sleep 10
docker-compose -f monitoring-alerting-deployment.yml ps
EOF

# 执行部署脚本
chmod +x deploy.sh
./deploy.sh
```

**6.2 健康检查脚本**
```bash
# 创建健康检查脚本
cat > health-check.sh <<EOF
#!/bin/bash
services=("postgres-monitoring" "redis-monitoring" "rabbitmq-monitoring" "prometheus" "alertmanager" "grafana" "custom-exporter" "monitoring-api" "nginx")
for service in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Status}}' $service 2>/dev/null)
    echo "$service: $status"
done
EOF

# 执行健康检查
chmod +x health-check.sh
./health-check.sh
```

**6.3 自动备份脚本**
```bash
# 创建备份脚本
cat > backup.sh <<EOF
#!/bin/bash
BACKUP_DIR="/path/to/backup"
DATE=$(date +%Y%m%d_%H%M%S)
docker exec postgres-monitoring pg_dump -U monitoring_user monitoring_db > $BACKUP_DIR/backup_$DATE.sql
docker exec redis-monitoring redis-cli -a redis_monitoring_pass_123 BGSAVE
docker-compose -f monitoring-alerting-deployment.yml down -v
tar -czvf $BACKUP_DIR/backup_$DATE.tar.gz volumes/
EOF

# 执行备份脚本
chmod +x backup.sh
./backup.sh
```

**6.4 日志收集脚本**
```bash
# 创建日志收集脚本
cat > collect-logs.sh <<EOF
#!/bin/bash
LOG_DIR="./logs"
mkdir -p $LOG_DIR
docker-compose -f monitoring-alerting-deployment.yml logs > $LOG_DIR/all-services.log
docker-compose -f monitoring-alerting-deployment.yml logs prometheus > $LOG_DIR/prometheus.log
docker-compose -f monitoring-alerting-deployment.yml logs alertmanager > $LOG_DIR/alertmanager.log
EOF

# 执行日志收集
chmod +x collect-logs.sh
./collect-logs.sh
```

**操作演示**：
1. 显示自动化脚本
2. 执行自动化脚本
3. 展示脚本输出
```