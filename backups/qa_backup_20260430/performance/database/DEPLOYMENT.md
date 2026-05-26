# Sprint 27+1 数据库性能测试部署指南

## 概述

本文档介绍如何部署和运行Sprint 27+1测试环境的数据库性能测试系统。

## 系统架构

### 组件说明
1. **测试环境** (`docker-compose-sprint-27-1.yml`)
   - 4个PostgreSQL数据库实例
   - 4个Redis缓存实例
   - API Gateway服务
   - 业务微服务（库存、财务、AI）
   - 监控系统（Prometheus + Grafana）
   - 数据库性能测试容器

2. **性能测试工具**
   - 数据库性能分析器
   - 性能测试执行器
   - 压力测试执行器
   - 报告生成器

### 网络架构
```
┌─────────────────────────────────────────────────────────────┐
│                    Sprint 27+1 测试网络                     │
├─────────────┬─────────────┬─────────────┬─────────────┬─────┤
│   主数据库   │  库存数据库  │  财务数据库  │   AI数据库   │监控│
│  5432/tcp   │  5434/tcp   │  5433/tcp   │  5435/tcp   │服务│
├─────────────┼─────────────┼─────────────┼─────────────┼─────┤
│   主缓存     │  库存缓存    │  财务缓存    │   AI缓存     │    │
│  6379/tcp   │  6374/tcp   │  6373/tcp   │  6375/tcp   │    │
└─────────────┴─────────────┴─────────────┴─────────────┴─────┘
                           │
                    ┌──────┴──────┐
                    │ API Gateway │
                    │  8080/tcp   │
                    └──────┬──────┘
            ┌──────────────┼──────────────┐
            │              │              │
    ┌───────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐
    │ 库存管理服务  │ │ 财务管理服务 │ │ AI智能服务  │
    │  8082/tcp    │ │  8081/tcp  │ │  8083/tcp  │
    └──────────────┘ └────────────┘ └────────────┘
```

## 部署步骤

### 前提条件
1. **操作系统**: Windows 10/11 或 Windows Server 2019+
2. **Docker**: Docker Desktop 4.20+ 或 Docker Engine 24.0+
3. **内存**: 至少8GB可用内存
4. **磁盘**: 至少10GB可用空间
5. **网络**: 可访问Docker Hub

### 步骤1: 环境准备
```powershell
# 1.1 进入项目目录
cd I:\AI-Ready\backend\infrastructure\docker

# 1.2 检查Docker状态
docker version
docker info

# 1.3 检查端口占用
netstat -ano | findstr :5432
netstat -ano | findstr :5433
netstat -ano | findstr :5434
netstat -ano | findstr :5435
netstat -ano | findstr :8080-8084
netstat -ano | findstr :9090
netstat -ano | findstr :3000
```

### 步骤2: 启动测试环境
```powershell
# 2.1 使用启动脚本（推荐）
.\start-sprint-27-1.ps1

# 2.2 或手动启动
docker-compose -f docker-compose-sprint-27-1.yml up -d

# 2.3 验证启动状态
docker-compose -f docker-compose-sprint-27-1.yml ps
docker-compose -f docker-compose-sprint-27-1.yml logs --tail=50
```

### 步骤3: 运行性能测试
```powershell
# 3.1 进入性能测试容器
docker exec -it sprint-db-performance-test-27-1 bash

# 3.2 在容器内运行测试
cd /app
python performance_test_runner.py

# 3.3 或直接运行测试（从宿主机）
docker exec sprint-db-performance-test-27-1 python /app/performance_test_runner.py
```

### 步骤4: 查看测试结果
```powershell
# 4.1 查看测试结果文件
docker exec sprint-db-performance-test-27-1 ls -la /app/results/

# 4.2 复制测试结果到本地
docker cp sprint-db-performance-test-27-1:/app/results/ ./local-results/

# 4.3 查看生成的报告
Get-ChildItem .\local-results\ -Recurse -Filter *.md
```

## 配置说明

### 数据库配置
| 数据库 | 主机 | 端口 | 用户名 | 密码 | 数据库名 |
|--------|------|------|--------|------|----------|
| 主数据库 | sprint-postgres-main-27-1 | 5432 | sprint_gateway_user | sprint_gateway_pass_2026 | sprint_gateway_db |
| 库存数据库 | sprint-postgres-inventory-27-1 | 5432 | sprint_inventory_user | sprint_inventory_pass_2026 | sprint_inventory_db |
| 财务数据库 | sprint-postgres-finance-27-1 | 5432 | sprint_finance_user | sprint_finance_pass_2026 | sprint_finance_db |
| AI数据库 | sprint-postgres-ai-27-1 | 5432 | sprint_ai_user | sprint_ai_pass_2026 | sprint_ai_db |

### Redis配置
| Redis实例 | 主机 | 端口 | 密码 | 数据库数 |
|-----------|------|------|------|----------|
| 主缓存 | sprint-redis-main-27-1 | 6379 | sprint_redis_pass_2026 | 16 |
| 库存缓存 | sprint-redis-inventory-27-1 | 6379 | sprint_redis_inventory_pass_2026 | 4 |
| 财务缓存 | sprint-redis-finance-27-1 | 6379 | sprint_redis_finance_pass_2026 | 4 |
| AI缓存 | sprint-redis-ai-27-1 | 6379 | sprint_redis_ai_pass_2026 | 8 |

### 监控配置
| 服务 | URL | 用户名 | 密码 |
|------|-----|--------|------|
| Prometheus | http://localhost:9090 | - | - |
| Grafana | http://localhost:3000 | sprint_admin | sprint_admin_2026 |

## 测试场景

### 场景1: 数据库性能分析
```python
# 分析数据库配置和性能瓶颈
python performance_test_runner.py --mode analysis
```

### 场景2: 性能基准测试
```python
# 执行标准性能测试
python performance_test_runner.py --mode performance
```

### 场景3: 压力测试
```python
# 执行高并发压力测试
python performance_test_runner.py --mode stress
```

### 场景4: 完整测试套件
```python
# 执行所有测试
python performance_test_runner.py --mode full
```

## 测试指标

### 性能指标
1. **响应时间**
   - 简单查询: ≤10ms
   - 复杂查询: ≤100ms
   - 聚合查询: ≤50ms

2. **吞吐量**
   - 单数据库: ≥1000 QPS
   - 全系统: ≥5000 QPS

3. **并发能力**
   - 稳定并发: ≥100用户
   - 峰值并发: ≥200用户

4. **资源使用**
   - CPU使用率: ≤80%
   - 内存使用率: ≤85%
   - 连接池使用率: ≤80%

### 质量指标
1. **成功率**: ≥99.9%
2. **错误率**: ≤0.1%
3. **超时率**: ≤0.5%

## 故障排除

### 常见问题1: 端口冲突
```
错误: Port is already allocated
```
**解决方案**:
1. 停止占用端口的进程
2. 修改docker-compose文件中的端口映射
3. 使用不同的端口范围

### 常见问题2: 容器启动失败
```
错误: Container exited with code 1
```
**解决方案**:
1. 查看容器日志: `docker logs <container_id>`
2. 检查数据库连接配置
3. 验证网络配置

### 常见问题3: 性能测试失败
```
错误: Connection refused
```
**解决方案**:
1. 检查数据库服务是否正常运行
2. 验证连接字符串配置
3. 检查网络连接和防火墙设置

### 常见问题4: 内存不足
```
错误: Out of memory
```
**解决方案**:
1. 增加Docker内存分配
2. 减少测试数据量
3. 优化测试脚本内存使用

## 监控和维护

### 实时监控
```powershell
# 查看容器状态
docker-compose -f docker-compose-sprint-27-1.yml ps

# 查看实时日志
docker-compose -f docker-compose-sprint-27-1.yml logs -f

# 查看资源使用
docker stats
```

### 性能监控
1. **Prometheus指标**
   - http://localhost:9090/targets
   - http://localhost:9090/graph

2. **Grafana仪表盘**
   - http://localhost:3000
   - 预置仪表盘: Database Performance

### 数据备份
```powershell
# 备份数据库数据
docker exec sprint-postgres-main-27-1 pg_dump -U sprint_gateway_user sprint_gateway_db > backup_main.sql

# 备份测试结果
docker cp sprint-db-performance-test-27-1:/app/results/ ./backup-results/
```

### 清理维护
```powershell
# 停止所有容器
docker-compose -f docker-compose-sprint-27-1.yml down

# 清理数据卷（谨慎操作）
docker-compose -f docker-compose-sprint-27-1.yml down -v

# 清理镜像
docker image prune -a

# 清理未使用的资源
docker system prune -a
```

## 扩展配置

### 自定义测试参数
创建 `config/test_config.yaml`:
```yaml
performance_test:
  databases:
    - name: main
      connection_string: "host=sprint-postgres-main-27-1 port=5432 dbname=sprint_gateway_db user=sprint_gateway_user password=sprint_gateway_pass_2026"
    
  test_scenarios:
    basic_queries:
      iterations: 1000
      concurrency: 10
    
    stress_test:
      max_users: 200
      ramp_up_time: 60
      duration: 300
    
  thresholds:
    response_time:
      simple_query: 10
      complex_query: 100
      aggregate_query: 50
    
    success_rate: 99.9
    error_rate: 0.1
```

### 添加自定义测试
创建 `tests/custom_test.py`:
```python
import psycopg2
import time

class CustomPerformanceTest:
    def __init__(self, config):
        self.config = config
    
    def run_custom_test(self):
        # 实现自定义测试逻辑
        pass
    
    def generate_custom_report(self):
        # 生成自定义报告
        pass
```

## 最佳实践

### 测试环境管理
1. **环境隔离**: 每个测试使用独立的环境
2. **数据隔离**: 测试数据与生产数据完全分离
3. **版本控制**: 所有配置和脚本纳入版本控制

### 测试执行
1. **预热阶段**: 测试前进行系统预热
2. **逐步加压**: 从低并发逐步增加到高并发
3. **结果验证**: 测试后验证数据一致性

### 报告生成
1. **自动生成**: 测试完成后自动生成报告
2. **历史对比**: 与历史测试结果对比
3. **趋势分析**: 分析性能变化趋势

### 持续改进
1. **定期测试**: 建立定期测试机制
2. **问题跟踪**: 建立问题跟踪和解决流程
3. **知识库**: 建立性能测试知识库

## 附录

### A. 命令速查
```powershell
# 启动环境
.\start-sprint-27-1.ps1

# 停止环境
docker-compose -f docker-compose-sprint-27-1.yml down

# 重启环境
docker-compose -f docker-compose-sprint-27-1.yml restart

# 查看日志
docker-compose -f docker-compose-sprint-27-1.yml logs -f api-gateway

# 进入容器
docker exec -it sprint-postgres-main-27-1 bash

# 运行测试
docker exec sprint-db-performance-test-27-1 python /app/performance_test_runner.py

# 查看测试结果
docker exec sprint-db-performance-test-27-1 ls -la /app/results/
```

### B. 文件说明
```
backend/infrastructure/docker/
├── docker-compose-sprint-27-1.yml    # 测试环境配置
├── start-sprint-27-1.ps1             # 启动脚本
├── init-scripts/                     # 数据库初始化脚本
├── prometheus-sprint/                # Prometheus配置
└── grafana-sprint/                   # Grafana配置

qa/performance/database/
├── Dockerfile.performance            # 测试容器Dockerfile
├── performance_test_runner.py        # 测试主程序
├── performance_test_utils.py         # 测试工具类
├── report_generator.py               # 报告生成器
├── requirements.txt                  # Python依赖
└── DEPLOYMENT.md                     # 部署文档
```

### C. 联系方式
- **项目负责人**: 运维工程师
- **技术支持**: devops-engineer
- **文档维护**: doc-writer
- **测试执行**: qa-lead

### D. 版本历史
| 版本 | 日期 | 说明 | 作者 |
|------|------|------|------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |
| 1.0.1 | 2026-04-27 | 完善部署步骤 | devops-engineer |

---

**最后更新**: 2026-04-27  
**文档状态**: 正式发布  
**适用环境**: Sprint 27+1测试环境