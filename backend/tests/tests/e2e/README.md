# AI-Ready E2E测试套件

## Sprint 27+1测试环境配置 - E2E测试脚本开发

### 测试概述

本测试套件覆盖AI-Ready系统的4大核心业务流程：

| 测试套件 | 测试用例数 | 文件路径 | 覆盖范围 |
|---------|-----------|---------|---------|
| 用户注册登录流程 | 11 | tests/e2e/user-auth.spec.ts | 注册、登录、登出、表单验证、多语言、响应式设计 |
| 订单创建支付流程 | 14 | tests/e2e/order-flow.spec.ts | 创建订单、支付、订单详情、取消、搜索、退款 |
| 库存管理流程 | 12 | tests/e2e/inventory-flow.spec.ts | 库存查询、调整、预警、盘点、调拨、报表 |
| CRM客户管理流程 | 13 | tests/e2e/crm-flow.spec.ts | 客户创建、跟进、编辑、筛选、报表、合并 |

**总计：40个E2E测试用例**

### 环境要求

- Node.js >= 18.0
- Playwright >= 1.40.0
- 测试环境: http://localhost:8080 (可通过环境变量配置)

### 安装依赖

```bash
npm install @playwright/test -D
npx playwright install
```

### 测试配置

#### 环境变量配置

```bash
# 配置测试URL（可选）
export E2E_BASE_URL=http://localhost:8080

# 或使用环境文件
echo "E2E_BASE_URL=http://localhost:8080" > .env.test
```

#### Playwright配置文件

配置文件路径: `playwright.config.ts`

主要配置项:
- 测试目录: `./tests/e2e`
- 浏览器支持: Chromium, Firefox, WebKit
- 移动端支持: Pixel 5, iPhone 12
- 失败时截图/视频/trace保留
- 测试超时: 30秒

### 执行测试

#### 1. 执行所有测试

```bash
npx playwright test
```

#### 2. 执行指定测试套件

```bash
# 执行用户注册登录流程测试
npx playwright test user-auth.spec.ts

# 执行订单创建支付流程测试
npx playwright test order-flow.spec.ts

# 执行库存管理流程测试
npx playwright test inventory-flow.spec.ts

# 执行CRM客户管理流程测试
npx playwright test crm-flow.spec.ts
```

#### 3. 执行指定浏览器测试

```bash
# Chromium
npx playwright test --project=chromium

# Firefox
npx playwright test --project=firefox

# WebKit
npx playwright test --project=webkit

# 所有浏览器
npx playwright test --project=chromium --project=firefox --project=webkit
```

#### 4. 有头模式运行（调试）

```bash
npx playwright test --headed
npx playwright test --debug
```

#### 5. 使用执行脚本

```bash
# 执行脚本方式
node tests/e2e/run-e2e-tests.js

# 指定浏览器和测试套件
node tests/e2e/run-e2e-tests.js --browser=chromium --suite=user-auth

# 指定测试URL
node tests/e2e/run-e2e-tests.js --base-url=http://localhost:8080

# 有头模式
node tests/e2e/run-e2e-tests.js --headed

# 调试模式
node tests/e2e/run-e2e-tests.js --debug

# 生成测试报告
node tests/e2e/run-e2e-tests.js --report
```

### 测试报告

#### 查看HTML报告

```bash
npx playwright show-report
```

报告路径: `playwright-report/index.html`

#### JSON结果文件

结果路径: `test-results/results.json`

### 测试数据

测试数据配置文件: `tests/data/e2e/test_data_config.json`

包含:
- 测试用户: testuser001, testvip001, testadmin001
- 测试客户: 张三, VIP客户李四
- 测试商品: 商品A, 商品B, VIP商品C
- 测试仓库: 北京仓库

### 测试数据清理

测试数据清理脚本: `tests/data/e2e/test_data_cleaner.py`

功能:
- 清理测试数据
- 重置测试状态
- API数据清理

### 文件结构

```
tests/
├── e2e/
│   ├── user-auth.spec.ts        # 用户注册登录流程测试
│   ├── order-flow.spec.ts       # 订单创建支付流程测试
│   ├── inventory-flow.spec.ts   # 库存管理流程测试
│   ├── crm-flow.spec.ts         # CRM客户管理流程测试
│   ├── ai-dialog.spec.ts        # AI对话框功能测试
│   └── run-e2e-tests.js         # 测试执行脚本
├── data/
│   └── e2e/
│       ├── test_data_config.json    # 测试数据配置
│       ├── test_data_cleaner.py     # 测试数据清理脚本
│       └── playwright.config.json   # Playwright场景配置
playwright.config.ts             # Playwright主配置文件
test-results/                    # 测试结果目录
playwright-report/               # 测试报告目录
```

### CI/CD集成

#### GitHub Actions示例

```yaml
name: E2E Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: 18
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npx playwright test
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

### 注意事项

1. **测试环境依赖**: 测试需要运行中的AI-Ready服务（localhost:8080）
2. **测试数据隔离**: 建议使用独立的测试数据库
3. **并发执行**: CI环境使用单worker避免并发冲突
4. **失败重试**: CI环境自动重试2次
5. **浏览器安装**: 首次运行需安装浏览器内核

### 联系信息

- 任务ID: task_1777012789841_59bkkxcke
- Sprint: 27+1
- 测试负责人: test-agent-2

---

**文档生成时间**: 2026-04-24
**版本**: 1.0