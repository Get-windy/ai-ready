# 采购付款流程端到端测试环境配置

## 1. 环境架构

### 1.1 整体架构图
```
┌─────────────────────────────────────────────────────┐
│                端到端测试环境架构                     │
├─────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │  前端    │  │ API网关  │  │ 业务服务  │  │  数据库  │ │
│  │ 测试工具 │  │         │  │         │  │         │ │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘ │
│         │           │           │           │        │
│  ┌──────▼───────────▼───────────▼───────────▼──────┐ │
│  │             消息队列 (RabbitMQ)                  │ │
│  └─────────────────────────────────────────────────┘ │
│         │           │           │           │        │
│  ┌──────▼───────────▼───────────▼───────────▼──────┐ │
│  │             缓存服务 (Redis)                     │ │
│  └─────────────────────────────────────────────────┘ │
│         │           │           │           │        │
│  ┌──────▼───────────▼───────────▼───────────▼──────┐ │
│  │             监控系统 (Prometheus+Grafana)        │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

## 2. 环境组件配置

### 2.1 硬件配置要求
| 组件 | 配置要求 | 数量 | 说明 |
|------|----------|------|------|
| 应用服务器 | 8核CPU, 16GB内存, 200GB SSD | 2台 | 主备部署 |
| 数据库服务器 | 8核CPU, 32GB内存, 500GB SSD | 1台 | PostgreSQL集群 |
| 缓存服务器 | 4核CPU, 8GB内存, 100GB SSD | 1台 | Redis哨兵模式 |
| 消息队列服务器 | 4核CPU, 8GB内存, 100GB SSD | 1台 | RabbitMQ集群 |
| 监控服务器 | 4核CPU, 8GB内存, 200GB HDD | 1台 | Prometheus+Grafana |

### 2.2 软件版本要求
| 软件组件 | 版本 | 说明 |
|----------|------|------|
| 操作系统 | Ubuntu 22.04 LTS | 推荐使用Linux环境 |
| Java环境 | OpenJDK 17.0.8 | 必须使用LTS版本 |
| 数据库 | PostgreSQL 15.3 | 支持JSONB和分区表 |
| 缓存 | Redis 7.2.4 | 支持哨兵模式 |
| 消息队列 | RabbitMQ 3.12.0 | 支持消息持久化 |
| 应用框架 | Spring Boot 3.2.5 | 企业级开发框架 |
| 测试工具 | Playwright 1.40.0 | 端到端测试框架 |

## 3. 网络配置

### 3.1 网络拓扑
```
测试客户端 (192.168.100.0/24)
        │
        ▼
负载均衡器 (192.168.101.10)
        │
        ├─────────────┐
        ▼             ▼
应用服务器1      应用服务器2
(192.168.101.11) (192.168.101.12)
        │             │
        ▼             ▼
数据库集群 (192.168.102.0/24)
```

### 3.2 端口配置
| 服务 | 端口 | 协议 | 用途 |
|------|------|------|------|
| 前端应用 | 8080 | HTTP | 用户界面访问 |
| API网关 | 8081 | HTTP | API接口访问 |
| PostgreSQL | 5432 | TCP | 数据库连接 |
| Redis | 6379 | TCP | 缓存服务 |
| RabbitMQ | 5672 | TCP | 消息队列 |
| Prometheus | 9090 | HTTP | 监控数据采集 |
| Grafana | 3000 | HTTP | 监控可视化 |

## 4. 数据库配置

### 4.1 数据库初始化脚本
```sql
-- 创建测试数据库
CREATE DATABASE erp_purchase_test
    ENCODING 'UTF8'
    LC_COLLATE 'zh_CN.UTF-8'
    LC_CTYPE 'zh_CN.UTF-8'
    TEMPLATE template0;

-- 创建测试用户
CREATE USER test_user WITH PASSWORD 'Test@123456';
GRANT ALL PRIVILEGES ON DATABASE erp_purchase_test TO test_user;

-- 创建表空间（如果需要）
CREATE TABLESPACE test_tablespace
    LOCATION '/var/lib/postgresql/test_data';
```

### 4.2 测试数据表结构
```sql
-- 供应商表
CREATE TABLE supplier_test (
    id BIGSERIAL PRIMARY KEY,
    supplier_code VARCHAR(50) NOT NULL UNIQUE,
    supplier_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 采购订单表
CREATE TABLE purchase_order_test (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier_test(id)
);

-- 发票表
CREATE TABLE invoice_test (
    id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    invoice_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES purchase_order_test(id)
);

-- 付款表
CREATE TABLE payment_test (
    id BIGSERIAL PRIMARY KEY,
    payment_no VARCHAR(50) NOT NULL UNIQUE,
    invoice_id BIGINT NOT NULL,
    payment_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    payment_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoice_test(id)
);
```

## 5. 测试数据初始化

### 5.1 基础测试数据
```sql
-- 插入供应商测试数据
INSERT INTO supplier_test (supplier_code, supplier_name, status) VALUES
('SUP001', '测试供应商A', 'APPROVED'),
('SUP002', '测试供应商B', 'APPROVED'),
('SUP003', '测试供应商C', 'PENDING'),
('SUP004', '测试供应商D', 'REJECTED');

-- 插入采购订单测试数据
INSERT INTO purchase_order_test (order_no, supplier_id, total_amount, status) VALUES
('PO20240504001', 1, 10000.00, 'CONFIRMED'),
('PO20240504002', 2, 25000.50, 'PENDING'),
('PO20240504003', 1, 15000.00, 'COMPLETED');

-- 插入发票测试数据
INSERT INTO invoice_test (invoice_no, order_id, invoice_amount, status) VALUES
('INV20240504001', 1, 10000.00, 'VERIFIED'),
('INV20240504002', 3, 15000.00, 'PENDING');

-- 插入付款测试数据
INSERT INTO payment_test (payment_no, invoice_id, payment_amount, status) VALUES
('PAY20240504001', 1, 10000.00, 'COMPLETED');
```

### 5.2 测试数据生成脚本
创建 `generate-test-data.py`：
```python
#!/usr/bin/env python3
"""
采购付款流程测试数据生成脚本
"""

import random
from datetime import datetime, timedelta

def generate_suppliers(count=10):
    """生成供应商测试数据"""
    suppliers = []
    for i in range(1, count + 1):
        suppliers.append({
            'supplier_code': f'SUP{i:03d}',
            'supplier_name': f'测试供应商{i}',
            'status': random.choice(['APPROVED', 'PENDING', 'REJECTED'])
        })
    return suppliers

def generate_purchase_orders(suppliers, count=20):
    """生成采购订单测试数据"""
    orders = []
    for i in range(1, count + 1):
        supplier = random.choice(suppliers)
        orders.append({
            'order_no': f'PO{datetime.now().strftime("%Y%m%d")}{i:03d}',
            'supplier_id': supplier['id'],
            'total_amount': round(random.uniform(1000, 50000), 2),
            'status': random.choice(['DRAFT', 'CONFIRMED', 'COMPLETED', 'CANCELLED'])
        })
    return orders

# 更多数据生成函数...
```

## 6. 监控配置

### 6.1 Prometheus配置
创建 `prometheus.yml`：
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'erp-purchase-test'
    static_configs:
      - targets: ['192.168.101.11:8080', '192.168.101.12:8080']
        labels:
          environment: 'test'
          application: 'erp-purchase'
  
  - job_name: 'postgres-exporter'
    static_configs:
      - targets: ['192.168.102.10:9187']
  
  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['192.168.103.10:9121']
  
  - job_name: 'rabbitmq-exporter'
    static_configs:
      - targets: ['192.168.104.10:9419']
```

### 6.2 Grafana仪表板配置
创建关键监控指标：
1. **应用性能指标**
   - 请求响应时间 (p50, p95, p99)
   - 请求吞吐量 (QPS)
   - 错误率 (4xx, 5xx)

2. **数据库指标**
   - 连接数
   - 查询性能
   - 锁等待时间

3. **业务指标**
   - 采购订单创建速率
   - 付款处理成功率
   - 流程完成时间

## 7. 环境验证检查清单

### 7.1 预测试环境验证
- [ ] 所有服务启动正常
- [ ] 数据库连接正常
- [ ] 缓存服务正常
- [ ] 消息队列正常
- [ ] 网络连通性正常

### 7.2 功能验证
- [ ] 用户登录功能正常
- [ ] 供应商管理功能正常
- [ ] 采购订单功能正常
- [ ] 付款功能正常

### 7.3 性能验证
- [ ] 单接口响应时间 < 2秒
- [ ] 并发测试通过
- [ ] 内存使用正常
- [ ] CPU使用率正常

## 8. 故障恢复方案

### 8.1 环境故障恢复
1. **数据库故障**：从备份恢复，切换备用节点
2. **应用故障**：重启服务，检查日志
3. **网络故障**：检查防火墙，重启网络服务

### 8.2 数据故障恢复
1. **测试数据损坏**：从数据备份恢复
2. **配置错误**：回滚到正确配置版本
3. **环境污染**：重新初始化测试环境

## 9. 环境维护计划

### 9.1 日常维护
- 每日检查服务状态
- 每周清理测试数据
- 每月备份环境配置

### 9.2 定期更新
- 每季度更新软件版本
- 每半年进行环境优化
- 每年进行环境重建

---
**配置版本**：v1.0  
**生效日期**：2026-05-04  
**维护团队**：测试团队  
**审核状态**：待审核