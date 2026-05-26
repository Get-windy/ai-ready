# 供应商门户自动化测试脚本

## 📋 项目概述

本目录包含供应商门户端到端自动化测试脚本，覆盖供应商注册、询价报价、订单执行、结算支付等核心业务流程。

## 📁 目录结构

```
automation-test-scripts/
├── supplier-lifecycle/          # 供应商生命周期测试
│   ├── test_registration_flow.py
│   ├── test_approval_flow.py
│   └── test_account_management.py
├── quotation-process/           # 询价报价流程测试
│   ├── test_rfq_flow.py
│   ├── test_quotation_submission.py
│   └── test_quotation_evaluation.py
├── order-execution/             # 订单执行流程测试
│   ├── test_order_creation.py
│   ├── test_order_confirmation.py
│   └── test_order_tracking.py
├── settlement-payment/          # 结算支付流程测试
│   ├── test_invoice_process.py
│   ├── test_payment_process.py
│   └── test_reconciliation.py
├── integration/                 # 集成测试
│   ├── test_supplier_integration.py
│   └── test_finance_integration.py
├── performance/                 # 性能测试
│   ├── test_concurrent_operations.py
│   └── test_load_testing.py
├── data/                        # 测试数据管理
│   ├── test_data_generator.py
│   ├── supplier_data.json
│   └── order_data.json
├── utils/                       # 工具类
│   ├── api_client.py
│   ├── db_helper.py
│   └── assertion_helper.py
├── config/                      # 配置文件
│   ├── test_config.yaml
│   └── environment_config.yaml
└── reports/                     # 报告生成
    ├── html_reporter.py
    └── junit_reporter.py
```

## 🚀 快速开始

### 环境准备
```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置环境
cp config/environment_config.yaml.example config/environment_config.yaml
# 编辑配置文件设置测试环境参数

# 3. 生成测试数据
python data/test_data_generator.py --count 100
```

### 执行测试
```bash
# 执行全部测试
python run_all_tests.py --environment test

# 执行冒烟测试
python run_smoke_tests.py --scenarios "registration,quotation"

# 执行特定场景测试
python -m pytest supplier-lifecycle/test_registration_flow.py -v
```

## 🔧 技术栈

- **测试框架**: pytest + unittest
- **API测试**: requests + httpx
- **UI测试**: Playwright/Selenium
- **数据库**: SQLAlchemy + pymysql
- **数据生成**: Faker
- **报告生成**: Allure + HTMLTestRunner
- **并发测试**: Locust + JMeter

## 📊 测试覆盖率

- **功能测试覆盖率**: ≥95%
- **API接口覆盖率**: ≥90%
- **业务场景覆盖率**: 100%
- **自动化测试率**: ≥80%

## 🔒 安全测试

包含以下安全测试场景：
1. 身份认证与授权测试
2. 输入验证与注入防护
3. 数据加密与传输安全
4. 会话管理与访问控制

## 📈 性能测试

性能测试指标：
- P95响应时间: ≤3秒
- 并发用户数: ≥500
- 系统可用性: ≥99.9%
- 错误率: <0.1%

## 📝 报告生成

测试执行后生成以下报告：
1. HTML格式测试报告
2. JUnit格式测试报告
3. Allure测试报告
4. 性能测试报告

## 🔄 CI/CD集成

支持以下CI/CD工具集成：
- Jenkins Pipeline
- GitLab CI/CD
- GitHub Actions
- Azure DevOps

## 📚 相关文档

- [供应商门户端到端测试方案](../supplier-portal-e2e-test-plan.md)
- [测试执行指南](../test-execution-guide.md)
- [API接口文档](docs/api-docs.md)
- [测试数据规范](docs/test-data-spec.md)

## 👥 维护团队

- **测试负责人**: QA-Lead
- **自动化测试工程师**: Automation-Specialist
- **性能测试工程师**: Performance-Expert
- **安全测试工程师**: Security-Expert

## 🐛 问题反馈

发现bug或需要改进，请提交到：
- JIRA项目: SUPPLIER-PORTAL-TEST
- GitHub Issues: https://github.com/ai-ready/supplier-portal-e2e/issues

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情