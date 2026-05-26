# AI-Ready 测试环境演示网站

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**项目**: AI-Ready Sprint 27+1  
**技术栈**: Prometheus + AlertManager + Grafana + PostgreSQL + Redis + RabbitMQ  

---

## 目录

1. [快速入门](#快速入门)
2. [演示视频](#演示视频)
3. [用户手册](#用户手册)
4. [技术文档](#技术文档)
5. [环境预览](#环境预览)

---

## 快速入门

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 8GB内存
- 20GB磁盘空间

### 快速开始

```bash
# 进入部署目录
cd I:\AI-Ready\deploy

# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 验证服务

1. **PostgreSQL**: http://localhost:5433
2. **Redis**: http://localhost:6380
3. **RabbitMQ**: http://localhost:15673
4. **Prometheus**: http://localhost:9090
5. **AlertManager**: http://localhost:9093
6. **Grafana**: http://localhost:3000
7. **Nginx**: http://localhost

---

## 演示视频

### 快速入门演示（5分钟）

**视频内容**：
- 环境准备
- 环境部署
- 服务验证
- 基本导航
- 常用命令

**观看方式**：
- [视频链接]（待录制）
- [视频脚本](videos/quick-start-script.md)

### 功能演示（15分钟）

**视频内容**：
- 系统监控
- 数据库监控
- 缓存监控
- 消息队列监控
- 告警功能
- 可视化仪表盘

**观看方式**：
- [视频链接]（待录制）
- [视频脚本](videos/features-script.md)

### 部署运维演示（10分钟）

**视频内容**：
- 部署流程
- 服务配置
- 日常运维
- 性能监控
- 故障排查
- 自动化运维

**观看方式**：
- [视频链接]（待录制）
- [视频脚本](videos/deployment-script.md)

---

## 用户手册

### 快速入门指南

**内容概要**：
- 系统要求
- 快速部署
- 验证环境
- 基本使用
- 常见问题

**文档位置**：[user-manual-quick-start.md](user-manual-quick-start.md)

### 功能使用手册

**内容概要**：
- 系统监控
- 数据库监控
- 缓存监控
- 消息队列监控
- 告警管理
- 可视化仪表盘

**文档位置**：[user-manual-functional.md](user-manual-functional.md)

### 管理员配置手册

**内容概要**：
- 高级配置
- 性能调优
- 安全配置
- 备份恢复
- 故障恢复

**文档位置**：[user-manual-admin.md](user-manual-admin.md)

### 常见问题解答

**内容概要**：
- 环境搭建问题
- 服务配置问题
- 运维问题
- 故障排查

**文档位置**：[user-manual-faq.md](user-manual-faq.md)

---

## 技术文档

### 环境配置文档

| 文档 | 描述 | 大小 | 位置 |
|------|------|------|------|
| README | 测试环境总览 | 3,628字节 | docs/testing/README.md |
| ARCHITECTURE | 架构设计 | 12,537字节 | docs/testing/environment/ARCHITECTURE.md |
| ENVIRONMENT_VARIABLES | 环境变量配置 | 8,651字节 | docs/testing/environment/ENVIRONMENT_VARIABLES.md |
| SERVICE_DEPENDENCIES | 服务依赖关系 | 18,730字节 | docs/testing/environment/SERVICE_DEPENDENCIES.md |
| NETWORK_TOPOLOGY | 网络拓扑图 | 22,593字节 | docs/testing/environment/NETWORK_TOPOLOGY.md |

### 部署操作文档

| 文档 | 描述 | 大小 | 位置 |
|------|------|------|------|
| DOCKER_COMPOSE_DEPLOY | Docker Compose部署指南 | 18,556字节 | docs/testing/deploy/DOCKER_COMPOSE_DEPLOY.md |
| MANUAL_DEPLOY | 手动部署操作手册 | 21,350字节 | docs/testing/deploy/MANUAL_DEPLOY.md |
| INIT_SCRIPTS | 环境初始化脚本说明 | 待创建 | docs/testing/deploy/INIT_SCRIPTS.md |
| HEALTH_CHECK | 健康检查操作指南 | 待创建 | docs/testing/deploy/HEALTH_CHECK.md |

### 演示文档

| 文档 | 描述 | 大小 | 位置 |
|------|------|------|------|
| validation-report | 验证报告 | 5,580字节 | docs/demo/validation-report.md |

---

## 环境预览

### 监控指标预览

#### 系统监控

- CPU使用率：实时监控
- 内存使用率：实时监控
- 磁盘使用率：实时监控
- 网络流量：实时监控

#### 数据库监控

- 连接池状态：监控中
- 查询性能：监控中
- 数据库资源：监控中

#### 缓存监控

- 缓存命中率：监控中
- 内存使用：监控中
- 键值对统计：监控中

#### 消息队列监控

- 队列深度：监控中
- 消费者状态：监控中
- 消息吞吐量：监控中

### 告警预览

#### 配置的告警规则

1. **高CPU使用率告警**
   - 阈值：80%
   - 持续时间：5分钟
   - 通知渠道：钉钉、企业微信、邮件

2. **高内存使用率告警**
   - 阈值：80%
   - 持续时间：5分钟
   - 通知渠道：钉钉、企业微信、邮件

3. **高磁盘使用率告警**
   - 阈值：85%
   - 持续时间：5分钟
   - 通知渠道：钉钉、企业微信、邮件

### 可视化仪表盘预览

#### 预定义仪表盘

1. **系统监控仪表盘**
   - CPU使用率趋势
   - 内存使用趋势
   - 磁盘使用趋势
   - 网络流量趋势

2. **数据库监控仪表盘**
   - 连接池状态
   - 查询性能
   - 数据库资源

3. **缓存监控仪表盘**
   - 缓存命中率
   - 内存使用
   - 键值对统计

4. **消息队列监控仪表盘**
   - 队列深度
   - 消费者状态
   - 消息吞吐量

---

## 支持信息

### 技术支援

- **负责人**: devops-engineer
- **联系方式**: devops@ai-ready.com

### 文档反馈

- **负责人**: doc-writer
- **联系方式**: docs@ai-ready.com

### 项目管理

- **负责人**: coordinator
- **联系方式**: pm@ai-ready.com

---

## 版本信息

| 项目 | 版本 | 日期 |
|------|------|------|
| 演示网站 | 1.0.0 | 2026-04-27 |
| 监控告警模块 | 1.0.0 | 2026-04-27 |
| 测试环境配置 | 1.0.0 | 2026-04-27 |

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| 1.0.0 | 2026-04-27 | 初始版本 | mnj0j12k |

---

## 联系我们

如有任何问题或建议，请通过以下方式联系我们：

- **技术支援**: devops@ai-ready.com
- **文档反馈**: docs@ai-ready.com
- **项目管理**: pm@ai-ready.com

**项目官网**: https://ai-ready.com