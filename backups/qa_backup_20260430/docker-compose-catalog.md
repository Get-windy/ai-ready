# Docker Compose 配置文件分类清单

## 概述
本文档整理了项目中所有 `docker-compose` 配置文件的用途、覆盖范围、适用场景，为容器化配置任务提供决策支持，避免重复工作。

## 分类标准
- **基础设施类**: 数据库、消息队列、缓存等基础服务
- **监控类**: Prometheus、Grafana、ELK等监控和日志服务
- **业务服务类**: 具体的业务应用服务
- **测试类**: 测试环境专用配置
- **部署类**: 生产或准生产环境部署配置

---

## 1. 基础设施类

### 1.1 数据库服务
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/infrastructure/docker/docker/postgresql/` | `docker-compose.yml` | PostgreSQL | PostgreSQL数据库服务 | 开发、测试环境数据库 |
| `backend/configs/configs/exporters/` | `docker-compose-exporters.yml` | 数据导出器 | 数据库导出服务 | 数据迁移和备份 |

### 1.2 消息与缓存
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/order/` | `docker-compose.infra.yml` | Redis、RabbitMQ | 订单模块基础设施 | 订单服务依赖的中间件 |

### 1.3 日志服务
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/configs/configs/logging/` | `docker-compose-elk.yml` | ELK Stack | 集中式日志收集 | 日志分析和监控 |
| `backend/infrastructure/docker/docker/logging/` | `docker-compose-logging.yml` | 日志服务 | 基础日志收集 | 开发环境日志 |

---

## 2. 监控类

### 2.1 监控告警系统
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/infrastructure/monitoring/` | `docker-compose.monitoring-optimized.yml` | Prometheus, Grafana, AlertManager | 监控告警系统优化版 | Sprint 29监控告警模块 |
| `backend/infrastructure/monitoring/sprint28-alerting/` | `docker-compose.yml` | Prometheus, Grafana | 监控告警基础配置 | Sprint 28监控告警模块 |

### 2.2 AI监控服务
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/ai/monitoring/intelligent-analysis/` | `docker-compose.yml` | AI智能分析监控 | AI服务专用监控 | AI模块监控 |

---

## 3. 业务服务类

### 3.1 AI服务
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/ai/` | `docker-compose.test.yml` | AI推理服务、PostgreSQL、Redis | AI服务测试环境 | AI模块测试 |
| `ai-monitoring/` | `docker-compose.yml` | AI监控服务 | AI服务监控 | AI服务监控部署 |

### 3.2 订单服务
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/order/` | `docker-compose.yml` | 订单服务 | 订单业务服务 | 订单模块开发测试 |
| `backend/order/` | `docker-compose-modified.yml` | 订单服务(修改版) | 订单服务定制配置 | 特定需求订单服务 |

---

## 4. 测试类

### 4.1 Sprint测试环境
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/infrastructure/docker/` | `docker-compose-sprint-27-1.yml` | 完整测试环境 | Sprint 27+1测试环境 | 数据库性能测试、压力测试 |
| `backend/infrastructure/docker/test-environment/` | `docker-compose.yml` | 测试环境 | 通用测试环境 | 常规测试 |
| `backend/infrastructure/docker/test-environment/` | `docker-compose-simple.yml` | 简化测试环境 | 轻量级测试环境 | 快速测试 |

### 4.2 测试环境备份
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/infrastructure/docker/` | `docker-compose-test.yml` | 测试环境 | 当前测试环境 | 日常测试 |
| `backend/infrastructure/docker/` | `docker-compose-test.backup.yml` | 测试环境备份 | 测试环境备份配置 | 恢复和对比 |

### 4.3 AI测试环境
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `ai/test/environment/sprint27-plus1/` | `docker-compose.yml` | AI测试环境 | AI模块测试环境 | AI功能测试 |
| `ai/test/environment/sprint27-plus1/` | `docker-compose.optimized.yml` | AI测试环境优化版 | 优化后的AI测试环境 | AI性能测试 |

---

## 5. 部署类

### 5.1 完整部署
| 文件路径 | 文件名 | 主要服务 | 用途说明 | 适用场景 |
|---------|--------|----------|----------|----------|
| `backend/infrastructure/docker/docker/` | `docker-compose.full.yml` | 完整服务栈 | 全功能部署配置 | 生产或演示环境 |

---

## 总结与建议

### 重复配置分析
1. **测试环境配置重复**:
   - 存在多个测试环境配置，建议统一管理
   - `docker-compose-test.yml` 和 `docker-compose-test.backup.yml` 功能相似

2. **监控配置分散**:
   - 监控配置分散在不同目录，建议集中管理
   - 考虑创建统一的监控配置模板

### 使用建议
1. **新项目启动**:
   - 使用 `backend/infrastructure/docker/docker-compose-sprint-27-1.yml` 作为基础模板
   - 根据需求选择基础设施配置

2. **监控部署**:
   - 使用 `backend/infrastructure/monitoring/docker-compose.monitoring-optimized.yml` 作为监控标准配置

3. **测试环境**:
   - 轻量测试: `backend/infrastructure/docker/test-environment/docker-compose-simple.yml`
   - 完整测试: `backend/infrastructure/docker/docker-compose-sprint-27-1.yml`

### 维护建议
1. 定期清理过时的备份配置
2. 建立配置文件的版本管理机制
3. 为每个配置文件添加清晰的注释和版本信息
4. 考虑使用环境变量和模板减少重复配置

---

## 文件统计
- **总计**: 18个docker-compose配置文件
- **基础设施类**: 5个
- **监控类**: 3个
- **业务服务类**: 4个
- **测试类**: 8个
- **部署类**: 1个

*最后更新: 2026-04-29*