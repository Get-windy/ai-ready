# Sprint 27+1 端到端自动化测试

## 项目概述

为AI-Ready项目Sprint 27+1测试环境开发的端到端自动化测试框架。

## 功能特性

- ✅ 用户注册到登录完整流程测试
- ✅ 数据创建到查询完整流程测试
- ✅ 业务流程端到端测试
- ✅ 异常恢复端到端测试
- ✅ 自动化测试报告生成
- ✅ 测试数据管理

## 目录结构

```
qa/end-to-end-testing/
├── framework/              # 测试框架核心
│   ├── base_test.py       # 基础测试类
│   ├── config.py          # 配置管理
│   └── utils.py           # 工具函数
├── scenarios/             # 测试场景
│   ├── user-flow/        # 用户流程测试
│   ├── data-flow/        # 数据流程测试
│   ├── business-flow/    # 业务流程测试
│   └── recovery-flow/    # 异常恢复测试
├── run_e2e_tests.py      # 测试运行器
└── README.md             # 项目说明
```

## 快速开始

### 安装依赖

```bash
pip install pytest requests faker pyyaml
```

### 运行测试

```bash
# 运行所有测试
python run_e2e_tests.py

# 运行特定类别测试
python run_e2e_tests.py --category user-flow

# 运行并生成报告
python run_e2e_tests.py --report
```

### 使用Pytest运行

```bash
# 运行所有测试
pytest scenarios/ -v

# 运行特定测试文件
pytest scenarios/user-flow/test_user_registration_login.py -v

# 生成HTML报告
pytest scenarios/ -v --html=reports/report.html
```

## 测试场景

### 1. 用户注册到登录流程
- 新用户注册
- 邮箱验证
- 首次登录
- 密码重置
- 多设备登录管理

### 2. 数据创建到查询流程
- 创建数据记录
- 数据验证
- 数据查询
- 数据更新
- 数据删除

### 3. 业务流程端到端
- 订单创建流程
- 支付处理流程
- 库存管理流程
- 物流跟踪流程

### 4. 异常恢复流程
- 服务中断恢复
- 数据库故障恢复
- 网络中断恢复
- 数据一致性恢复

## 配置说明

测试配置通过`framework/config.py`管理，支持：
- 环境变量覆盖
- 配置文件加载
- 默认配置

## 报告生成

测试执行后会自动生成：
- JSON格式报告
- Markdown格式报告
- 详细日志文件

## 注意事项

1. 确保测试环境API服务已启动
2. 配置正确的数据库连接信息
3. 检查网络连接状态
4. 定期清理测试数据

## 维护者

- 项目: AI-Ready Sprint 27+1
- 创建时间: 2026-04-29
- 状态: 进行中