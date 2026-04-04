# AI-Ready 本地集成测试设计方案

## 一、测试目标

验证AI-Ready系统各模块之间的协同工作能力，确保：
1. 模块间接口调用正确
2. 业务流程完整可执行
3. 数据流转正确无误
4. 多租户数据隔离有效

## 二、测试环境配置

### 2.1 本地环境要求
- Python 3.10+
- pytest 测试框架
- 内存数据库（Mock）
- 无需远程服务依赖

### 2.2 Mock服务配置
| Mock服务 | 用途 | 替代 |
|----------|------|------|
| MockDatabase | 内存数据库 | PostgreSQL/MySQL |
| MockAuthService | 认证服务 | Sa-Token服务 |
| MockPermissionService | 权限服务 | 权限管理模块 |
| MockERPService | ERP服务 | ERP模块 |
| MockCRMService | CRM服务 | CRM模块 |

## 三、集成测试场景设计

### 3.1 用户-权限模块集成测试

#### 场景1: 用户登录权限检查流程
```
用户登录 → Token生成 → Token验证 → 权限分配 → 权限检查
```

**测试用例:**
- TC-INT-001: 用户登录后获取Token并验证权限
- TC-INT-002: 跨模块权限强制执行（ERP管理员无CRM权限）

#### 场景2: 多租户数据隔离
```
创建租户A用户 → 创建租户B用户 → 验证数据隔离
```

**测试用例:**
- TC-INT-003: 租户数据隔离验证

### 3.2 ERP-CRM模块集成测试

#### 场景3: 客户下单-库存扣减流程
```
创建客户 → 创建产品 → 添加库存 → 创建商机 → 扣减库存
```

**测试用例:**
- TC-INT-004: 客户下单完整流程
- TC-INT-005: 库存不足异常处理

#### 场景4: 线索转化订单流程
```
创建线索 → 线索跟进 → 线索转化 → 创建商机 → 成交
```

**测试用例:**
- TC-INT-006: 线索到订单完整流程

### 3.3 业务流程集成测试

#### 场景5: 完整客户旅程
```
创建客户 → 创建线索 → 线索跟进 → 线索转化 → 创建商机 → 商机推进 → 客户升级
```

**测试用例:**
- TC-INT-007: 完整客户生命周期

#### 场景6: 库存管理流程
```
添加产品 → 采购入库 → 销售出库 → 库存检查
```

**测试用例:**
- TC-INT-008: 库存更新流程

### 3.4 跨模块报表集成测试

#### 场景7: 跨模块数据汇总
```
CRM客户数据 + ERP库存数据 → 生成综合报表
```

**测试用例:**
- TC-INT-009: 跨模块报表数据

## 四、测试脚本结构

```
tests/
├── integration/
│   ├── __init__.py
│   ├── conftest.py              # 集成测试配置
│   ├── test_user_permission.py  # 用户-权限集成
│   ├── test_erp_crm.py          # ERP-CRM集成
│   ├── test_business_flow.py    # 业务流程集成
│   └── test_multi_tenant.py     # 多租户集成
├── mocks/
│   └── mock_services.py         # Mock服务
└── reports/
    └── INTEGRATION_TEST_REPORT.md
```

## 五、执行方案

### 5.1 执行命令

```bash
# 执行所有集成测试
pytest tests/integration -v -m integration

# 执行特定场景
pytest tests/integration/test_user_permission.py -v
pytest tests/integration/test_erp_crm.py -v

# 生成覆盖率报告
pytest tests/integration --cov=. --cov-report=html
```

### 5.2 CI/CD集成

```yaml
# .github/workflows/integration-test.yml
name: Integration Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.10'
      - run: pip install pytest pytest-cov
      - run: pytest tests/integration -v --cov=.
```

## 六、预期结果

| 测试场景 | 预期结果 |
|----------|----------|
| 用户-权限集成 | 权限检查正确执行，跨模块隔离有效 |
| ERP-CRM集成 | 数据流转正确，库存扣减准确 |
| 业务流程集成 | 完整流程可执行，状态转换正确 |
| 多租户集成 | 数据隔离有效，无跨租户访问 |

## 七、风险与依赖

### 7.1 风险
- Mock服务可能与真实服务行为不一致
- 边界情况可能未完全覆盖

### 7.2 依赖
- pytest 测试框架
- Mock服务正确实现
- 测试数据准备完整

---

*文档版本: 1.0*
*创建日期: 2026-04-01*
*作者: test-agent-1*