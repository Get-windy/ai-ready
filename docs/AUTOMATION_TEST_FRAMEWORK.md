# AI-Ready 自动化测试框架文档

**版本**: 1.0.0  
**更新日期**: 2026-04-03  
**维护者**: test-agent-1

---

## 📚 目录

1. [框架概述](#1-框架概述)
2. [测试框架设计](#2-测试框架设计)
3. [测试脚本编写指南](#3-测试脚本编写指南)
4. [测试数据管理](#4-测试数据管理)
5. [测试报告生成](#5-测试报告生成)
6. [最佳实践](#6-最佳实践)

---

## 1. 框架概述

### 1.1 测试框架架构

```
AI-Ready/
├── tests/
│   ├── api/                    # API接口测试
│   │   ├── test_api_comprehensive.py
│   │   └── test_api_automation.py
│   ├── e2e/                    # 端到端测试
│   │   ├── test_e2e_flows.py
│   │   └── conftest.py
│   ├── unit/                   # 单元测试
│   ├── integration/            # 集成测试
│   ├── security/               # 安全测试
│   ├── mocks/                  # Mock数据
│   ├── reports/                # 测试报告
│   ├── docs/                   # 测试文档
│   ├── conftest.py             # pytest配置
│   └── pytest.ini              # pytest配置文件
```

### 1.2 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 测试框架 | pytest | 9.0+ |
| HTTP客户端 | requests | 2.31+ |
| 断言库 | pytest-assert | 内置 |
| 报告生成 | pytest-html | 4.1+ |
| 覆盖率 | pytest-cov | 5.0+ |
| Mock | pytest-mock | 3.12+ |
| 前端测试 | Vitest | 1.2+ |

---

## 2. 测试框架设计

### 2.1 设计原则

1. **模块化**: 测试按功能模块组织
2. **可复用**: 公共组件封装为fixture
3. **可维护**: 清晰的目录结构和命名规范
4. **可扩展**: 易于添加新测试用例
5. **独立性**: 测试用例相互独立，无依赖

### 2.2 测试分层

```
┌─────────────────────────────────────┐
│          E2E Tests (端到端)          │
│     验证完整业务流程                  │
├─────────────────────────────────────┤
│       Integration Tests (集成)       │
│     验证模块间交互                    │
├─────────────────────────────────────┤
│          API Tests (接口)            │
│     验证API端点功能                   │
├─────────────────────────────────────┤
│          Unit Tests (单元)           │
│     验证函数/方法逻辑                 │
└─────────────────────────────────────┘
```

### 2.3 配置文件

**pytest.ini**
```ini
[pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
addopts = -v --tb=short --html=tests/reports/report.html
markers =
    api: API接口测试
    e2e: 端到端测试
    unit: 单元测试
    integration: 集成测试
    security: 安全测试
```

**conftest.py**
```python
import pytest
import requests

@pytest.fixture(scope="session")
def base_url():
    return "http://localhost:8080"

@pytest.fixture(scope="session")
def api_client(base_url):
    """API客户端fixture"""
    session = requests.Session()
    session.headers.update({
        "Content-Type": "application/json",
        "Accept": "application/json"
    })
    yield session
    session.close()
```

---

## 3. 测试脚本编写指南

### 3.1 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 测试文件 | test_<module>.py | test_user_api.py |
| 测试类 | Test<Feature> | TestUserAPI |
| 测试方法 | test_<scenario> | test_login_success |
| Fixture | <purpose>_fixture | auth_client |

### 3.2 测试用例模板

```python
import pytest

class TestFeature:
    """功能测试类"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前置条件"""
        # 初始化操作
        yield
        # 清理操作
    
    def test_scenario_success(self):
        """测试成功场景"""
        # Given: 准备测试数据
        data = {"key": "value"}
        
        # When: 执行测试操作
        result = some_function(data)
        
        # Then: 验证结果
        assert result["success"] is True
    
    def test_scenario_failure(self):
        """测试失败场景"""
        with pytest.raises(ValueError):
            some_function(invalid_data)
```

### 3.3 API测试示例

```python
class TestUserAPI:
    """用户管理API测试"""
    
    def test_login_success(self, api_client, base_url):
        """测试登录成功"""
        response = api_client.post(
            f"{base_url}/api/user/login",
            json={"username": "admin", "password": "Admin@123"}
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "token" in data["data"]
    
    @pytest.mark.parametrize("username,password,expected", [
        ("admin", "wrong", 401),
        ("unknown", "password", 401),
        ("", "", 400),
    ])
    def test_login_invalid(self, api_client, base_url, username, password, expected):
        """测试登录失败场景"""
        response = api_client.post(
            f"{base_url}/api/user/login",
            json={"username": username, "password": password}
        )
        assert response.status_code == expected
```

### 3.4 E2E测试示例

```python
class TestOrderFlow:
    """订单流程E2E测试"""
    
    @pytest.fixture(autouse=True)
    def setup_login(self, api_client, base_url):
        """登录获取Token"""
        response = api_client.post(
            f"{base_url}/api/user/login",
            json={"username": "testuser", "password": "Test@123"}
        )
        token = response.json()["data"]["token"]
        api_client.headers["Authorization"] = f"Bearer {token}"
        yield
        api_client.headers.pop("Authorization", None)
    
    def test_create_order_flow(self, api_client, base_url):
        """测试创建订单完整流程"""
        # 1. 创建订单
        order_data = {"productId": 1, "quantity": 10}
        response = api_client.post(f"{base_url}/api/order", json=order_data)
        assert response.status_code == 200
        order_id = response.json()["data"]["id"]
        
        # 2. 验证订单状态
        response = api_client.get(f"{base_url}/api/order/{order_id}")
        assert response.json()["data"]["status"] == "CREATED"
        
        # 3. 支付订单
        response = api_client.post(f"{base_url}/api/order/{order_id}/pay")
        assert response.status_code == 200
```

---

## 4. 测试数据管理

### 4.1 数据存储结构

```
tests/
├── mocks/
│   ├── users.json           # 用户测试数据
│   ├── orders.json          # 订单测试数据
│   ├── products.json        # 产品测试数据
│   └── responses/           # Mock响应数据
│       ├── success.json
│       └── error.json
```

### 4.2 数据加载工具

```python
import json
from pathlib import Path

def load_mock_data(filename: str) -> dict:
    """加载Mock数据"""
    mock_path = Path(__file__).parent / "mocks" / filename
    with open(mock_path, "r", encoding="utf-8") as f:
        return json.load(f)

def generate_test_user():
    """生成测试用户数据"""
    import uuid
    return {
        "username": f"test_{uuid.uuid4().hex[:8]}",
        "password": "Test@123456",
        "email": f"test_{uuid.uuid4().hex[:8]}@test.com"
    }
```

### 4.3 数据库测试数据

```python
@pytest.fixture(scope="function")
def db_session():
    """数据库会话fixture"""
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker
    
    engine = create_engine("sqlite:///:memory:")
    Session = sessionmaker(bind=engine)
    session = Session()
    
    # 初始化测试数据
    init_test_data(session)
    
    yield session
    
    # 清理测试数据
    session.rollback()
    session.close()
```

### 4.4 测试数据隔离

```python
@pytest.fixture(scope="function")
def isolated_test_data(db_session):
    """隔离的测试数据"""
    # 创建测试数据
    user = User(username="test_user")
    db_session.add(user)
    db_session.commit()
    
    yield user
    
    # 清理测试数据
    db_session.delete(user)
    db_session.commit()
```

---

## 5. 测试报告生成

### 5.1 HTML报告

```bash
# 生成HTML报告
pytest tests/ -v --html=tests/reports/report.html --self-contained-html

# 带覆盖率报告
pytest tests/ -v --cov=src --cov-report=html:tests/reports/coverage
```

### 5.2 JUnit XML报告

```bash
# 生成JUnit XML报告(用于CI/CD)
pytest tests/ -v --junitxml=tests/reports/junit.xml
```

### 5.3 自定义报告模板

```python
# tests/conftest.py
def pytest_sessionfinish(session, exitstatus):
    """测试结束后生成自定义报告"""
    import json
    from datetime import datetime
    
    report = {
        "timestamp": datetime.now().isoformat(),
        "exit_status": exitstatus,
        "total_tests": session.testscollected,
        "passed": session.testsfailed == 0
    }
    
    with open("tests/reports/summary.json", "w") as f:
        json.dump(report, f, indent=2)
```

### 5.4 报告内容

| 报告类型 | 内容 | 文件 |
|---------|------|------|
| HTML报告 | 详细测试结果、失败截图 | report.html |
| 覆盖率报告 | 代码覆盖率统计 | coverage/index.html |
| JUnit XML | CI/CD集成用 | junit.xml |
| JSON摘要 | 自定义统计 | summary.json |

---

## 6. 最佳实践

### 6.1 测试编写原则

1. **AAA模式**: Arrange(准备) → Act(执行) → Assert(断言)
2. **单一职责**: 每个测试方法只验证一个功能点
3. **独立运行**: 测试不应依赖其他测试的执行结果
4. **可重复性**: 多次运行结果一致
5. **快速执行**: 合理使用fixture作用域

### 6.2 性能测试建议

```python
import pytest
import time

class TestPerformance:
    @pytest.mark.benchmark
    def test_api_response_time(self, api_client, base_url):
        """测试API响应时间"""
        start = time.time()
        response = api_client.get(f"{base_url}/api/user/list")
        duration = (time.time() - start) * 1000
        
        assert response.status_code == 200
        assert duration < 200, f"响应时间过长: {duration}ms"
```

### 6.3 并行执行

```bash
# 安装pytest-xdist
pip install pytest-xdist

# 并行执行测试
pytest tests/ -n auto
```

### 6.4 CI/CD集成

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - run: pip install -r requirements-test.txt
      - run: pytest tests/ -v --junitxml=reports/junit.xml
      - uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: reports/
```

---

## 📁 文档清单

| 文档 | 说明 |
|------|------|
| `docs/AUTOMATION_TEST_FRAMEWORK.md` | 本文档 |
| `tests/pytest.ini` | pytest配置 |
| `tests/conftest.py` | 公共fixture |
| `tests/mocks/` | 测试数据 |

---

## 🔗 相关链接

- [pytest官方文档](https://docs.pytest.org/)
- [requests文档](https://requests.readthedocs.io/)
- [pytest-html插件](https://pytest-html.readthedocs.io/)

---

**文档版本**: 1.0.0  
**最后更新**: 2026-04-03  
**维护者**: test-agent-1
