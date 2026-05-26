# 测试环境脚本说明

## 概述

本目录包含 Sprint 27+1 测试环境配置专项所需的脚本，用于测试环境的初始化、验证、清理和数据管理。

## 脚本列表

### 1. 测试数据生成脚本 (`test-data-generator.py`)

**用途**: 生成测试所需的数据文件和SQL脚本

**功能**:
- 生成用户数据（支持自定义数量）
- 生成产品数据
- 生成订单数据
- 生成库存数据
- 生成供应商数据
- 支持JSON和SQL格式输出

**使用方法**:
```bash
python test-data-generator.py [选项]
```

**参数**:
- `--output-dir`: 输出目录（默认: test_data）
- `--user-count`: 用户数据数量（默认: 1000）
- `--order-count`: 订单数据数量（默认: 5000）
- `--product-count`: 产品数据数量（默认: 200）
- `--inventory-count`: 库存数据数量（默认: 2000）

**示例**:
```bash
python test-data-generator.py --user-count 500 --order-count 2000
```

### 2. 测试环境初始化脚本 (`test-env-init.py`)

**用途**: 初始化测试环境，部署所需服务

**功能**:
- 设置Docker网络
- 部署PostgreSQL数据库
- 部署Redis缓存
- 部署RabbitMQ消息队列
- 部署Prometheus监控
- 部署Grafana仪表盘
- 初始化数据库
- 配置监控系统

**使用方法**:
```bash
python test-env-init.py [选项]
```

**参数**:
- `--env`: 目标环境（test/dev/prod，默认: test）
- `--services`: 要初始化的服务列表
- `--skip-verification`: 跳过验证步骤
- `--verbose`: 显示详细日志

**示例**:
```bash
python test-env-init.py --env test --services postgresql redis rabbitmq
```

### 3. 测试环境清理脚本 (`test-env-cleanup.py`)

**用途**: 清理测试环境数据和服务

**功能**:
- 清理PostgreSQL数据
- 清理Redis数据
- 清理RabbitMQ数据
- 停止Docker容器
- 清理数据卷
- 清理临时文件
- 支持数据备份

**使用方法**:
```bash
python test-env-cleanup.py [选项]
```

**参数**:
- `--env`: 目标环境（默认: test）
- `--services`: 要清理的服务列表
- `--data-only`: 仅清理数据，不清除容器
- `--backup`: 清理前备份数据
- `--force`: 强制清理，不提示确认
- `--verbose`: 显示详细日志

**示例**:
```bash
python test-env-cleanup.py --backup --force
```

### 4. 测试环境验证脚本 (`test-env-verification.py`)

**用途**: 验证测试环境服务和数据的完整性和可用性

**功能**:
- 验证PostgreSQL连接和查询性能
- 验证Redis读写操作
- 验证RabbitMQ消息队列
- 验证Prometheus指标采集
- 验证Grafana仪表盘
- 验证应用服务健康状态
- 验证数据完整性

**使用方法**:
```bash
python test-env-verification.py [选项]
```

**参数**:
- `--env`: 目标环境（默认: test）
- `--services`: 要验证的服务列表
- `--output`: 验证报告输出文件（默认: test-env-verification-report.json）
- `--verbose`: 显示详细日志

**示例**:
```bash
python test-env-verification.py --services postgresql redis --output report.json
```

## 脚本依赖

### Python依赖
- `psycopg2` - PostgreSQL连接
- `redis` - Redis连接
- `pika` - RabbitMQ连接
- `requests` - HTTP请求

### 系统依赖
- Docker - 容器管理
- Python 3.8+

## 安装依赖

```bash
pip install psycopg2-binary redis pika requests
```

## 目录结构

```
qa/scripts/
├── test-data-generator.py      # 测试数据生成
├── test-env-init.py            # 环境初始化
├── test-env-cleanup.py         # 环境清理
├── test-env-verification.py    # 环境验证
├── README.md                   # 本说明文档
└── test_data/                  # 生成的测试数据目录
    ├── users.json
    ├── products.json
    ├── orders.json
    ├── inventory.json
    └── suppliers.json
```

## 使用流程

### 1. 初始化测试环境
```bash
python test-env-init.py --env test
```

### 2. 生成测试数据
```bash
python test-data-generator.py --user-count 1000 --order-count 5000
```

### 3. 验证环境
```bash
python test-env-verification.py --env test
```

### 4. 测试完成后清理
```bash
python test-env-cleanup.py --env test --backup
```

## 注意事项

1. **数据安全**: 生产环境使用前请备份重要数据
2. **权限要求**: 需要Docker操作权限
3. **网络要求**: 确保Docker网络配置正确
4. **日志查看**: 脚本会生成日志文件，可用于问题排查

## 故障排除

### 常见问题

1. **Docker连接失败**
   - 检查Docker服务是否运行
   - 确认当前用户有Docker权限

2. **数据库连接超时**
   - 检查数据库服务是否启动
   - 确认连接配置正确

3. **端口冲突**
   - 检查端口是否被占用
   - 修改配置中的端口设置

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-04-28 | 初始版本，包含基础功能 |

## 维护者

- **团队**: AI-Ready QA Team
- **项目**: Sprint 27+1: 测试环境配置专项
