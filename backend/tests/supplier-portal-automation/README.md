# 供应商协同门户接口自动化测试

## 概述
本目录包含供应商协同门户核心接口的自动化测试脚本，确保接口功能正确性和稳定性。

## 测试范围
1. **供应商信息管理接口测试**
   - 供应商注册和认证
   - 供应商信息查询和更新
   - 供应商资质管理

2. **合同管理接口测试**
   - 合同创建和审核
   - 合同查询和统计
   - 合同状态流转

3. **订单协同接口测试**
   - 订单发布和接收
   - 订单状态跟踪
   - 订单异常处理

## 技术栈
- **测试框架**: Pytest + Requests
- **测试环境**: Docker + PostgreSQL + Redis
- **性能测试**: Locust
- **持续集成**: GitHub Actions

## 目录结构
```
supplier-portal-automation/
├── README.md
├── requirements.txt
├── config/
│   ├── test_config.yaml
│   └── environment_config.yaml
├── tests/
│   ├── test_supplier_info.py
│   ├── test_contract_management.py
│   ├── test_order_collaboration.py
│   └── conftest.py
├── data/
│   ├── supplier_data.py
│   ├── contract_data.py
│   └── order_data.py
├── utils/
│   ├── api_client.py
│   ├── data_generator.py
│   └── assertions.py
└── scripts/
    ├── setup_test_env.sh
    ├── run_tests.sh
    └── cleanup.sh
```

## 快速开始
1. 安装依赖：`pip install -r requirements.txt`
2. 配置环境：复制 `config/test_config.yaml.example` 为 `config/test_config.yaml`
3. 启动测试环境：`./scripts/setup_test_env.sh`
4. 运行测试：`./scripts/run_tests.sh`
5. 清理环境：`./scripts/cleanup.sh`