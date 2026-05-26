# 快速入门演示视频脚本

**视频时长**：5分钟  
**目标观众**：新用户、快速上手者  
**演示环境**：Sprint 27+1测试环境  
**技术栈**：PostgreSQL + Redis + RabbitMQ + Prometheus + AlertManager + Grafana  

---

## 00:00-00:30 引言

**画面**：欢迎界面，项目Logo  
**配音**：
```
欢迎使用AI-Ready测试环境快速入门演示。
本视频将带您快速了解测试环境的搭建和基本使用。
整个演示约5分钟，让您快速上手。
```

---

## 00:30-01:30 环境准备

**画面**：展示环境配置清单  
**配音**：
```
首先，让我们准备测试环境。

**系统要求**：
- 操作系统：Windows/Linux/macOS
- Docker版本：20.10+
- Docker Compose：2.0+
- 内存：至少8GB
- 磁盘：至少20GB可用空间

**所需软件**：
- Docker Desktop 或 Docker Engine
- 文本编辑器（推荐VS Code）
- 浏览器（推荐Chrome/Firefox）

**环境配置**：
- PostgreSQL: 5433端口
- Redis: 6380端口
- RabbitMQ: 5673端口
- Prometheus: 9090端口
- AlertManager: 9093端口
- Grafana: 3000端口
- Nginx: 80端口
```

**操作演示**：
1. 显示检查命令 `docker --version`
2. 显示检查命令 `docker-compose --version`
3. 显示检查命令 `free -h` (Linux) 或活动监视器 (macOS)

---

## 01:30-02:30 环境部署

**画面**：命令行操作，启动环境  
**配音**：
```
接下来，我们部署测试环境。

**步骤1：获取部署文件**
- 访问项目代码仓库
- 下载monitoring-alerting-deployment.yml文件
- 创建部署目录

**步骤2：启动环境**
```
# 进入部署目录
cd deploy

# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

**步骤3：验证服务**
- 等待服务启动完成（约2-3分钟）
- 检查各服务健康状态
```

**操作演示**：
1. 显示进入目录并启动命令
2. 显示服务启动日志
3. 显示服务状态检查结果

---

## 02:30-03:30 服务验证

**画面**：验证各服务是否正常运行  
**配音**：
```
服务启动后，让我们验证各服务是否正常。

**1. 数据库服务 (PostgreSQL)**
- 端口：5433
- 验证命令：
```bash
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db -c '\dt'
```

**2. 缓存服务 (Redis)**
- 端口：6380
- 验证命令：
```bash
docker exec -it redis-monitoring redis-cli -a redis_monitoring_pass_123 ping
```
期望返回：PONG

**3. 消息队列 (RabbitMQ)**
- AMQP端口：5673
- 管理界面：http://localhost:15673
- 默认账号：monitoring_rabbit / rabbit_monitoring_pass_123

**4. 监控服务 (Prometheus)**
- 地址：http://localhost:9090
- 验证端点：http://localhost:9090/api/v1/status

**5. 告警服务 (AlertManager)**
- 地址：http://localhost:9093
- 验证端点：http://localhost:9093/api/v1/status

**6. 可视化 (Grafana)**
- 地址：http://localhost:3000
- 默认账号：admin / admin123
- 验证端点：http://localhost:3000/api/health
```

**操作演示**：
1. 显示PostgreSQL连接和表查询
2. 显示Redis ping命令返回PONG
3. 显示RabbitMQ管理界面
4. 显示Prometheus状态页面
5. 显示AlertManager状态页面
6. 显示Grafana登录页面

---

## 03:30-04:00 基本导航

**画面**：展示Grafana仪表盘  
**配音**：
```
现在，让我们快速浏览各服务的基本导航。

**Grafana仪表盘**：
- 登录后，进入欢迎界面
- 查看预定义的仪表盘
- 创建自定义仪表盘
- 配置告警通知

**Prometheus控制台**：
- 查看当前活跃的告警
- 执行查询表达式
- 查看目标服务状态
- 查看告警规则

**AlertManager管理**：
- 查看当前告警
- 配置告警路由
- 管理通知模板

**Nginx代理**：
- 访问 http://localhost 查看代理配置
- 所有服务通过Nginx统一入口访问
```

**操作演示**：
1. 显示Grafana主界面
2. 显示Prometheus查询界面
3. 显示AlertManager告警列表

---

## 04:00-04:30 常用命令

**画面**：命令行演示常用操作  
**配音**：
```
掌握这些常用命令，帮助您高效管理测试环境。

**服务管理**：
```bash
# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps

# 停止所有服务
docker-compose -f monitoring-alerting-deployment.yml down

# 重启服务
docker-compose -f monitoring-alerting-deployment.yml restart <service>

# 查看服务日志
docker-compose -f monitoring-alerting-deployment.yml logs -f <service>

# 进入容器
docker exec -it <container_name> /bin/bash
```

**诊断命令**：
```bash
# 检查网络
docker network inspect monitoring-network

# 检查卷
docker volume ls

# 查看容器资源使用
docker stats
```

**清理操作**：
```bash
# 停止并删除所有容器
docker-compose -f monitoring-alerting-deployment.yml down -v

# 删除所有未使用的卷
docker volume prune
```