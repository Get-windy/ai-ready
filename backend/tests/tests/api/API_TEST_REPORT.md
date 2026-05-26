# AI-Ready API接口自动化测试报告

## 测试概要

- **测试日期**: 2026-04-01
- **测试框架**: pytest + requests
- **服务地址**: http://localhost:8080
- **测试范围**: 用户管理、ERP模块、CRM模块

---

## 一、测试脚本清单

### 1. 测试框架配置

| 文件 | 说明 |
|------|------|
| `conftest.py` | pytest配置、fixtures、API客户端封装 |
| `test_user_api.py` | 用户管理API测试(已存在) |
| `test_erp_api.py` | ERP模块API测试(新建) |
| `test_crm_api.py` | CRM模块API测试(新建) |

### 2. API客户端类

提供统一的REST API请求封装:
- GET/POST/PUT/DELETE/PATCH方法
- 自动Token认证
- 请求重试机制
- 超时控制

---

## 二、测试用例统计

### 用户管理API (test_user_api.py)

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| TestUserAPI | 6 | 用户注册/登录/信息/更新/登出 |
| TestProductAPI | 3 | 产品列表/详情/搜索 |
| TestOrderAPI | 3 | 订单创建/列表/详情 |
| **小计** | **12** | |

### ERP模块API (test_erp_api.py)

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| TestERPProductAPI | 5 | 产品分页/创建/更新/删除/搜索 |
| TestERPStockAPI | 5 | 库存列表/查询/入库/出库/盘点 |
| TestERPPurchaseAPI | 4 | 采购订单列表/创建/审批/取消 |
| TestERPSalesAPI | 3 | 销售订单列表/创建/发货 |
| TestERPAccountAPI | 3 | 会计科目列表/树形/余额 |
| TestERPReportAPI | 3 | 采购/销售/库存报表 |
| **小计** | **23** | |

### CRM模块API (test_crm_api.py)

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| TestCRMCustomerAPI | 6 | 客户列表/创建/详情/更新/搜索/等级 |
| TestCRMLeadAPI | 5 | 线索列表/创建/转化/分配/合格验证 |
| TestCRMOpportunityAPI | 5 | 商机列表/创建/阶段更新/赢单/输单 |
| TestCRMActivityAPI | 3 | 活动列表/创建/按客户查询 |
| TestCRMReportAPI | 4 | 客户分析/线索转化/商机管道/业绩报表 |
| TestCRMIntegrationAPI | 1 | 完整业务流程测试 |
| **小计** | **24** | |

### 总计

| 模块 | 用例数 |
|------|--------|
| 用户管理 | 12 |
| ERP模块 | 23 |
| CRM模块 | 24 |
| **总计** | **59** |

---

## 三、测试覆盖范围

### 用户管理API

| API端点 | 测试覆盖 |
|---------|---------|
| `/api/user/page` | ✅ 分页查询 |
| `/api/user/login` | ✅ 用户登录 |
| `/api/user/logout` | ✅ 用户登出 |
| `/api/user/register` | ✅ 用户注册 |
| `/api/user/{id}` | ✅ 用户详情/更新 |

### ERP模块API

| API端点 | 测试覆盖 |
|---------|---------|
| `/api/product/page` | ✅ 产品分页 |
| `/api/product/save` | ✅ 产品创建 |
| `/api/product/update` | ✅ 产品更新 |
| `/api/product/delete` | ✅ 产品删除 |
| `/api/stock/list` | ✅ 库存列表 |
| `/api/stock/in` | ✅ 入库操作 |
| `/api/stock/out` | ✅ 出库操作 |
| `/api/purchase/order/page` | ✅ 采购订单 |
| `/api/sales/order/page` | ✅ 销售订单 |
| `/api/account/list` | ✅ 会计科目 |
| `/api/report/*` | ✅ ERP报表 |

### CRM模块API

| API端点 | 测试覆盖 |
|---------|---------|
| `/api/customer/page` | ✅ 客户列表 |
| `/api/customer/save` | ✅ 客户创建 |
| `/api/customer/detail` | ✅ 客户详情 |
| `/api/lead/page` | ✅ 线索列表 |
| `/api/lead/save` | ✅ 线索创建 |
| `/api/lead/convert` | ✅ 线索转化 |
| `/api/opportunity/page` | ✅ 商机列表 |
| `/api/opportunity/save` | ✅ 商机创建 |
| `/api/opportunity/stage` | ✅ 阶段更新 |
| `/api/opportunity/win` | ✅ 赢单 |
| `/api/opportunity/lose` | ✅ 输单 |
| `/api/activity/page` | ✅ 活动列表 |
| `/api/report/*` | ✅ CRM报表 |

---

## 四、pytest标记说明

| 标记 | 说明 | 使用场景 |
|------|------|---------|
| `@pytest.mark.smoke` | 冒烟测试 | 核心功能快速验证 |
| `@pytest.mark.erp` | ERP测试 | ERP模块测试 |
| `@pytest.mark.crm` | CRM测试 | CRM模块测试 |
| `@pytest.mark.user` | 用户测试 | 用户模块测试 |
| `@pytest.mark.slow` | 慢速测试 | 耗时较长测试 |

---

## 五、执行命令

### 运行所有API测试
```bash
cd I:\AI-Ready
pytest tests/api -v
```

### 运行指定模块测试
```bash
# ERP模块
pytest tests/api/test_erp_api.py -v

# CRM模块
pytest tests/api/test_crm_api.py -v

# 用户管理
pytest tests/api/test_user_api.py -v
```

### 运行冒烟测试
```bash
pytest tests/api -v -m smoke
```

### 生成HTML报告
```bash
pytest tests/api -v --html=reports/api_test_report.html
```

### 按标记运行
```bash
pytest tests/api -v -m "erp or crm"
pytest tests/api -v -m "not slow"
```

---

## 六、测试环境配置

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `API_BASE_URL` | `http://localhost:8080` | API服务地址 |
| `TEST_TIMEOUT` | `30` | 请求超时秒数 |
| `RETRY_COUNT` | `3` | 重试次数 |

### 配置示例
```bash
# Windows PowerShell
$env:API_BASE_URL="http://192.168.1.100:8080"
pytest tests/api -v

# Linux/Mac
export API_BASE_URL="http://192.168.1.100:8080"
pytest tests/api -v
```

---

## 七、测试报告输出

执行测试后，将生成以下报告:
- 控制台输出: 测试执行详情
- HTML报告: `reports/api_test_report.html` (需安装pytest-html)
- JSON报告: 可通过插件生成

---

## 八、注意事项

1. **服务启动**: 测试前需确保API服务已启动
2. **认证Token**: 部分测试需要先登录获取Token
3. **数据隔离**: 测试数据建议使用独立数据库
4. **清理机制**: 测试后自动清理创建的测试数据

---

## 九、文件结构

```
I:\AI-Ready\tests\api\
├── conftest.py          # pytest配置和fixtures
├── test_user_api.py     # 用户管理API测试
├── test_erp_api.py      # ERP模块API测试
├── test_crm_api.py      # CRM模块API测试
├── __pycache__/         # Python缓存
└── reports/             # 测试报告目录(待生成)
```

---

*报告生成时间: 2026-04-01 20:14*
*生成者: test-agent-1*