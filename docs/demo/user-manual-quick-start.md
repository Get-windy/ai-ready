# 快速入门指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: mnj0j12k  
**最后更新**: 2026-04-27  

---

## 目录

1. [简介](#简介)
2. [系统要求](#系统要求)
3. [快速部署](#快速部署)
4. [验证环境](#验证环境)
5. [基本使用](#基本使用)
6. [常见问题](#常见问题)

---

## 简介

AI-Ready测试环境是一个完整的监控告警解决方案，包含以下组件：

- **PostgreSQL**：关系型数据库，用于存储监控数据
- **Redis**：缓存服务，提升数据访问速度
- **RabbitMQ**：消息队列，支持异步消息传递
- **Prometheus**：监控数据收集和存储
- **AlertManager**：告警管理和通知
- **Grafana**：监控数据可视化
- **Nginx**：反向代理和负载均衡

---

## 系统要求

### 硬件要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2核 | 4核或更多 |
| 内存 | 8GB | 16GB或更多 |
| 磁盘 | 20GB | 50GB或更多 |
| 网络 | 100Mbps | 1Gbps或更高 |

### 软件要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 容器编排 |
| 操作系统 | Windows/Linux/macOS | 支持主流操作系统 |

### 端口要求

确保以下端口没有被占用：

| 端口 | 服务 | 说明 |
|------|------|------|
| 5433 | PostgreSQL | 数据库服务 |
| 6380 | Redis | 缓存服务 |
| 5673 | RabbitMQ | 消息队列 |
| 15673 | RabbitMQ Management | 管理界面 |
| 9090 | Prometheus | 监控服务 |
| 9093 | AlertManager | 告警服务 |
| 3000 | Grafana | 可视化服务 |
| 80 | Nginx | Web服务 |

---

## 快速部署

### 步骤1：下载部署文件

1. 访问项目代码仓库
2. 下载`monitoring-alerting-deployment.yml`文件
3. 创建部署目录，例如：`I:\AI-Ready\deploy`

### 步骤2：启动环境

打开命令行终端，执行以下命令：

```bash
# 进入部署目录
cd I:\AI-Ready\deploy

# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d
```

### 步骤3：等待服务启动

服务启动需要约2-3分钟，请耐心等待。

### 步骤4：验证服务状态

```bash
# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

确保所有服务状态为"Up"。

---

## 验证环境

### 验证数据库服务

```bash
# 检查PostgreSQL
docker exec -it postgres-monitoring pg_isready -U monitoring_user
```

### 验证缓存服务

```bash
# 检查Redis
docker exec -it redis-monitoring redis-cli -a redis_monitoring_pass_123 ping
# 期望返回：PONG
```

### 验证消息队列

打开浏览器，访问：

```
http://localhost:15673
```

使用以下 credentials 登录：

- 用户名：`monitoring_rabbit`
- 密码：`rabbit_monitoring_pass_123`

### 验证监控服务

打开浏览器，访问：

```
http://localhost:9090
```

点击"Status" > "Targets"，确保所有目标状态为"UP"。

### 验证可视化服务

打开浏览器，访问：

```
http://localhost:3000
```

使用以下 credentials 登录：

- 用户名：`admin`
- 密码：`admin123`

---

## 基本使用

### 查看监控指标

1. 打开Grafana：http://localhost:3000
2. 登录后，进入"Dashboards"页面
3. 选择预定义的仪表盘
4. 查看实时监控数据

### 接收告警通知

1. 查看AlertManager：http://localhost:9093
2. 配置钉钉或企业微信机器人
3. 在Grafana中配置告警规则
4. 当指标超过阈值时，会自动发送告警通知

### 自定义仪表盘

1. 打开Grafana
2. 点击"Create" > "New Dashboard"
3. 点击"Add visualization"
4. 选择数据源
5. 配置查询表达式
6. 保存仪表盘

---

## 常见问题

### 问题1：服务无法启动

**症状**：服务状态为"Created"或"Exit"

**解决方案**：
```bash
# 查看服务日志
docker-compose -f monitoring-alerting-deployment.yml logs <service_name>

# 检查端口占用
netstat -tlnp | grep <port>

# 修改端口配置或停止占用端口的服务
```

### 问题2：服务响应缓慢

**症状**：页面加载慢，API响应慢

**解决方案**：
```bash
# 检查资源使用
docker stats

# 检查容器日志
docker-compose -f monitoring-alerting-deployment.yml logs <service_name>
```

### 问题3：监控数据丢失

**症状**：Grafana中看不到历史数据

**解决方案**：
```bash
# 检查Prometheus存储
docker exec -it prometheus ls /prometheus

# 检查磁盘空间
df -h
```

### 问题4：告警未发送

**症状**：告警触发但没有收到通知

**解决方案**：
```bash
# 检查AlertManager状态
curl http://localhost:9093/api/v1/status

# 查看AlertManager日志
docker-compose -f monitoring-alerting-deployment.yml logs alertmanager
```

---

## 下一步

- 阅读[功能使用手册](user-manual-functional.md)了解详细功能
- 阅读[管理员配置手册](user-manual-admin.md)了解高级配置
- 阅读[常见问题解答](user-manual-faq.md)获取更多帮助

---

## 联系我们

如有问题，请联系：

- **技术支援**: devops-engineer
- **文档反馈**: doc-writer
- **项目管理**: coordinator