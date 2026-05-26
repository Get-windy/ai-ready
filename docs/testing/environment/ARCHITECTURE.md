# 测试环境架构设计文档

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [系统架构](#系统架构)
3. [组件依赖关系](#组件依赖关系)
4. [网络拓扑](#网络拓扑)
5. [数据流向](#数据流向)
6. [部署拓扑](#部署拓扑)

---

## 概述

### 设计目标

测试环境架构设计旨在提供一个与生产环境相同的技术栈和配置，用于：

- 功能测试和集成测试
- 性能测试和压力测试
- 自动化测试和回归测试
- 系统调优和优化验证

### 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| PostgreSQL | 15.x | 关系型数据库 |
| Redis | 7.x | 缓存服务 |
| RabbitMQ | 3.12.x | 消息队列 |
| Prometheus | 2.45.x | 监控数据收集 |
| AlertManager | 0.25.x | 告警管理 |
| Grafana | 10.0.x | 监控可视化 |
| Nginx | alpine | 负载均衡和反向代理 |
| Spring Boot | 3.2.x | 应用服务器 |

---

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          用户访问层                                 │
│                              Nginx                                  │
│                    ┌─────────────────────────┐                     │
│                    │  80: Grafana            │                     │
│                    │  443: 监控API           │                     │
│                    └─────────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        应用服务层                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │  Prometheus      │  │  AlertManager    │  │  custom-exporter │   │
│  │  9090            │  │  9093            │  │  9101            │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                     monitoring-api (Spring Boot)             │   │
│  │                        8081                                  │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       数据存储层                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ PostgreSQL   │  │   Redis      │  │ RabbitMQ     │              │
│  │  5433        │  │   6380       │  │  5673        │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

### 架构组件说明

#### 1. 监控数据收集层 (Prometheus)

**功能**:
- 定期从目标服务采集监控指标
- 存储时间序列数据
- 执行告警规则评估

**配置要点**:
- 采集间隔: 30秒
- 数据保留: 30天
- 存储大小: 10GB
- 自动发现: 通过file_sd配置文件服务发现

#### 2. 告警管理层 (AlertManager)

**功能**:
- 接收来自Prometheus的告警
- 告警路由和分组
- 通知渠道集成

**支持渠道**:
- 钉钉机器人
- 企业微信机器人
- 邮件

#### 3. 监控可视化层 (Grafana)

**功能**:
- 展示监控指标dashboard
- 自定义查询和图表
- 告警视图

**配置要点**:
- 数据源: Prometheus
- 默认用户: admin/admin123
- 插件: piechart, clock, simple-json-datasource

#### 4. 自定义导出器 (custom-exporter)

**功能**:
- 从数据库、Redis、RabbitMQ获取业务指标
- 转换为Prometheus格式
- 暴露 /metrics 端点

**监控指标**:
- 数据库连接池状态
- Redis缓存命中率
- RabbitMQ队列积压

#### 5. API服务层 (monitoring-api)

**功能**:
- 提供监控告警相关的REST API
- 业务指标计算和聚合
- 告警规则管理

**端点**:
- `/api/v1/alerts` - 告警规则管理
- `/api/v1/metrics` - 业务指标
- `/api/v1/dashboard` - Dashboard配置

#### 6. 网关层 (Nginx)

**功能**:
- 反向代理所有服务
- 负载均衡
- SSL终止
- 访问控制

**配置要点**:
- 静态资源缓存
- 请求大小限制
- 访问日志记录

---

## 组件依赖关系

### 启动顺序

```
1. PostgreSQL          (基础数据库)
   ↓
2. Redis              (缓存服务)
   ↓
3. RabbitMQ           (消息队列)
   ↓
4. Prometheus         (监控主体)
   ↓
5. AlertManager       (告警处理)
   ↓
6. Grafana            (可视化)
   ↓
7. custom-exporter    (业务导出器)
   ↓
8. monitoring-api     (API服务)
   ↓
9. Nginx              (网关层)
```

### 依赖关系图

```
PostgreSQL ──────┐
                 │
Redis ───────────┼──────► Prometheus ─────► AlertManager
                 │          ▲                  ▲
RabbitMQ ────────┘          │                  │
                            │                  │
                    custom-exporter──────┐    │
                                         │    │
                           monitoring-api──────► Nginx
```

### 健康检查依赖

| 服务 | 健康检查端点 | 依赖服务 | 启动等待时间 |
|------|-------------|---------|-------------|
| Prometheus | /-/healthy | PostgreSQL, Redis, RabbitMQ | 40s |
| AlertManager | /-/healthy | Prometheus | 30s |
| Grafana | /api/health | Prometheus | 60s |
| custom-exporter | /health | PostgreSQL, Redis, RabbitMQ | 40s |
| monitoring-api | /actuator/health | PostgreSQL, Redis, RabbitMQ, Prometheus | 90s |
| Nginx | nginx -t | 所有上游服务 | - |

---

## 网络拓扑

### 网络配置

| 网络名称 | 子网 | 网关 | 用途 |
|---------|------|------|------|
| monitoring-network | 172.30.0.0/24 | 172.30.0.1 | 所有容器通信 |

### 容器网络

```
┌─────────────────────────────────────────────────────────────┐
│                    monitoring-network                        │
│  172.30.0.1  ──────  monitoring-bridge                      │
│                                                              │
│  172.30.0.2  postgres-monitoring                            │
│  172.30.0.3  redis-monitoring                               │
│  172.30.0.4  rabbitmq-monitoring                           │
│  172.30.0.5  prometheus                                     │
│  172.30.0.6  alertmanager                                   │
│  172.30.0.7  grafana                                        │
│  172.30.0.8  custom-exporter                                │
│  172.30.0.9  monitoring-api                                 │
│  172.30.0.10 nginx                                          │
└─────────────────────────────────────────────────────────────┘
```

### 端口映射

| 服务 | 主机端口 | 容器端口 | 用途 |
|------|---------|----------|------|
| PostgreSQL | 5433 | 5432 | 数据库访问 |
| Redis | 6380 | 6379 | Redis访问 |
| RabbitMQ | 5673 | 5672 | AMQP协议 |
| RabbitMQ Management | 15673 | 15672 | 管理界面 |
| Prometheus | 9090 | 9090 | 监控数据 |
| AlertManager | 9093 | 9093 | 告警管理 |
| Grafana | 3000 | 3000 | 可视化界面 |
| custom-exporter | 9101 | 9101 | 指标导出 |
| monitoring-api | 8081 | 8080 | API服务 |
| Nginx HTTP | 80 | 80 | Web服务 |
| Nginx HTTPS | 443 | 443 | HTTPS服务 |

---

## 数据流向

### 监控数据流

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 业务服务    │────▶│ custom-     │────▶│  Prometheus │
│ (应用指标)  │    │ exporter    │    │  (收集存储) │
└─────────────┘    └─────────────┘    └─────────────┘
                                          │
                                          ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 业务服务    │◀────│ custom-     │◀────│  Prometheus │
│ (获取指标)  │    │ exporter    │    │  (评估告警) │
└─────────────┘    └─────────────┘    └─────────────┘
                                          │
                                          ▼
                                  ┌─────────────┐
                                  │ AlertManager│
                                  │ (告警处理)  │
                                  └─────────────┘
                                          │
                                          ▼
                                  ┌─────────────┐
                                  │  Grafana    │
                                  │ (可视化)    │
                                  └─────────────┘
```

### 告警流程

1. **数据采集**: Prometheus每30秒从所有目标采集指标
2. **规则评估**: Prometheus评估告警规则，触发告警
3. **告警发送**: Prometheus将告警发送给AlertManager
4. **告警路由**: AlertManager根据配置路由告警
5. **通知发送**: 通过钉钉/企业微信/邮件发送通知

---

## 部署拓扑

### 单机部署结构

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Host                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Docker Containers                        │   │
│  │                                                        │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐    │   │
│  │  │PostgreSQL│ │  Redis  │ │ RabbitMQ│ │Prometheus│    │   │
│  │  │  5433   │ │  6380   │ │  5673   │ │  9090   │    │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘    │   │
│  │                                                        │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐    │   │
│  │  │AlertMgr │ │  Grafana│ │CustomEx │ │ API svc │    │   │
│  │  │  9093   │ │  3000   │ │  9101   │ │  8081   │    │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘    │   │
│  │                                                        │   │
│  │  ┌─────────┐                                         │   │
│  │  │  Nginx  │                                         │   │
│  │  │   80    │                                         │   │
│  │  └─────────┘                                         │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 存储卷映射

| 卷名 | 宿主机路径 | 容器路径 | 用途 |
|------|-----------|---------|------|
| prometheus-data | ./volumes/prometheus | /prometheus | Prometheus数据 |
| grafana-data | ./volumes/grafana | /var/lib/grafana | Grafana数据 |
| alertmanager-data | ./volumes/alertmanager | /alertmanager | AlertManager数据 |
| postgres-data | ./volumes/postgres | /var/lib/postgresql/data | PostgreSQL数据 |
| redis-data | ./volumes/redis | /data | Redis数据 |
| rabbitmq-data | ./volumes/rabbitmq | /var/lib/rabbitmq | RabbitMQ数据 |

### 目录结构

```
I:\AI-Ready\deploy\
├── monitoring-alerting-deployment.yml      # 主配置文件
├── volumes/                                # 数据卷目录
│   ├── prometheus/
│   ├── grafana/
│   ├── alertmanager/
│   ├── postgres/
│   ├── redis/
│   └── rabbitmq/
├── init-scripts/                          # 初始化脚本
│   └── postgres-init.sql
├── prometheus/                            # Prometheus配置
│   ├── prometheus.yml
│   ├── rules/
│   └── file_sd/
├── alertmanager/                          # AlertManager配置
│   ├── alertmanager.yml
│   └── templates/
├── grafana/                               # Grafana配置
│   ├── provisioning/
│   └── dashboards/
├── nginx/                                 # Nginx配置
│   ├── nginx.conf
│   ├── conf.d/
│   ├── ssl/
│   └── html/
└── custom-exporter/                       # 自定义导出器
    ├── Dockerfile
    └── config/
```

---

## 高可用设计

### 组件冗余

| 组件 | 副本数 | 高可用方式 |
|------|-------|-----------|
| PostgreSQL | 1 | 主从复制 (规划中) |
| Redis | 1 | Redis Cluster (规划中) |
| RabbitMQ | 1 | 镜像队列 (规划中) |
| Prometheus | 1 | 远程存储 (规划中) |
| Grafana | 1 | 无状态，可横向扩展 |
| AlertManager | 1 | 告警集群 (规划中) |

### 容灾策略

- **数据备份**: PostgreSQL每日备份，保留7天
- **监控告警**: Prometheus和AlertManager独立部署
- **服务发现**: 使用Docker网络自动发现

### 恢复流程

1. **服务中断**: 检查服务健康状态
2. **故障诊断**: 查看容器日志
3. **快速恢复**: 重启故障服务
4. **数据恢复**: 从备份恢复数据

---

## 监控指标

### 关键监控指标

| 指标类别 | 指标名称 | 告警阈值 |
|---------|---------|---------|
| CPU使用率 | node_cpu_seconds_total | > 80% |
| 内存使用率 | node_memory_MemAvailable_bytes | < 20% |
| 磁盘使用率 | node_filesystem_avail_bytes | > 85% |
| 数据库连接 | postgres_qsort_active | > 100 |
| Redis内存 | redis_used_memory_human | > 200MB |
| 消息队列积压 | rabbitmq_queue_messages_ready | > 1000 |

### Dashboard指标

- 系统资源监控 (CPU/内存/磁盘/网络)
- 服务健康状态
- 数据库性能指标
- Redis性能指标
- RabbitMQ性能指标
- Prometheus自监控
- AlertManager告警状态
- Grafana使用情况

---

## 安全配置

### 访问控制

- 数据库密码: 环境变量加密存储
- Redis密码: 启用认证
- RabbitMQ: 启用用户认证
- Grafana: 禁用匿名访问
- AlertManager: 通过Prometheus认证

### 数据安全

- 敏感数据: 环境变量存储
- 通信加密: Docker网络隔离
- 日志脱敏: Nginx访问日志脱敏

---

## 扩展性设计

### 横向扩展

- **Grafana**: 可部署多个实例，前端负载均衡
- **自定义导出器**: 可部署多个实例，Prometheus轮询

### 纵向扩展

- **PostgreSQL**: 增加内存和CPU
- **Redis**: 增加内存
- **Prometheus**: 增加存储和内存

### 监控Expand

- **新服务**: 添加file_sd配置
- **新指标**: 编写exporter或使用/blackbox
- **新告警**: 修改Prometheus rules

---

## 维护指南

### 日常维护

- 检查服务健康状态
- 查看容器日志
- 监控资源使用情况
- 备份重要数据

### 定期维护

- 每周: 检查备份
- 每月: 性能评估
- 每季度: 架构 review

### 故障排查

1. 查看容器状态: `docker-compose ps`
2. 查看容器日志: `docker-compose logs -f <service>`
3. 检查网络: `docker network inspect monitoring-network`
4. 健康检查: 访问各服务健康端点

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker Compose官方文档](https://docs.docker.com/compose/)
- [Prometheus官方文档](https://prometheus.io/docs/introduction/overview/)
- [Grafana官方文档](https://grafana.com/docs/)
- [PostgreSQL官方文档](https://www.postgresql.org/docs/)
- [Redis官方文档](https://redis.io/documentation)
- [RabbitMQ官方文档](https://www.rabbitmq.com/documentation.html)