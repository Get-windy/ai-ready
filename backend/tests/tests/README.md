# AI-Ready 自动化测试框架

AI-Ready项目的自动化测试框架，支持API测试和E2E测试。

## 目录结构

```
tests/
├── api/                    # API测试 (pytest)
│   └── test_ai_approval.py
├── e2e/                    # E2E测试 (Playwright)
│   └── ai-dialog.spec.ts
├── fixtures/               # 测试数据
│   └── test-data.json
├── utils/                  # 工具函数
│   ├── api-client.py
│   ├── global-setup.ts
│   └── global-teardown.ts
├── .github/workflows/      # CI/CD配置
│   └── automated-tests.yml
├── package.json            # Node.js依赖
├── requirements.txt        # Python依赖
├── playwright.config.ts    # Playwright配置
├── pytest.ini             # pytest配置
└── README.md              # 本文档
```

## 环境要求

- Node.js >= 18
- Python >= 3.11
- Chrome/Firefox/Safari浏览器

## 安装

```bash
# 安装Node.js依赖
npm install

# 安装Python依赖
pip install -r requirements.txt

# 安装Playwright浏览器
npx playwright install
```

## 配置

创建 `.env` 文件：

```env
TEST_BASE_URL=http://localhost:3000
TEST_USERNAME=test_user
TEST_PASSWORD=Test@123456
```

## 运行测试

### API测试

```bash
# 运行所有API测试
pytest tests/api -v

# 运行特定模块测试
pytest tests/api/test_ai_approval.py -v

# 运行带标记的测试
pytest tests/api -v -m ai
pytest tests/api -v -m approval

# 生成覆盖率报告
pytest tests/api -v --cov=src --cov-report=html
```

### E2E测试

```bash
# 运行所有E2E测试
npx playwright test

# 运行特定测试文件
npx playwright test ai-dialog.spec.ts

# 运行特定项目（浏览器）
npx playwright test --project=chromium

# 调试模式
npx playwright test --debug

# UI模式
npx playwright test --ui

# 生成报告
npx playwright show-report
```

### 运行所有测试

```bash
npm run test:all
```

## CI/CD集成

测试会自动在以下情况触发：
- 推送到 main 或 develop 分支
- 创建 Pull Request
- 每天凌晨2点定时运行

## 测试报告

- **HTML报告**: `reports/html/index.html`
- **JUnit报告**: `reports/junit/results.xml`
- **Allure报告**: `reports/allure/`
- **覆盖率报告**: `coverage/html/index.html`

## AI模块测试覆盖

| 模块 | API测试 | E2E测试 |
|------|---------|---------|
| 智能审批 | ✅ | ✅ |
| 智能推荐 | ✅ | - |
| 智能报表 | - | ✅ |
| 智能搜索 | ✅ | - |
| 智能对话 | ✅ | ✅ |

## 维护

- 文档维护: qa-lead
- 最后更新: 2026-04-13
