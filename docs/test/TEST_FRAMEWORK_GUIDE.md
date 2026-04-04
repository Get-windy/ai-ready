# AI-Ready 自动化测试框架使用指南

> 版本: v1.0 | 日期: 2026-03-30 | 编写: qa-lead

---

## 快速开始

### 1. 安装依赖

```bash
pip install pytest pytest-html pytest-cov pytest-xdist allure-pytest
pip install requests faker pymysql pyyaml
```

### 2. 运行测试

```bash
# 运行所有测试
pytest

# 运行冒烟测试
pytest -m smoke

# 运行API测试并生成HTML报告
pytest -m api --html=reports/test_report.html --self-contained-html

# 并行执行（4进程）
pytest -n 4

# 生成覆盖率报告
pytest --cov=src --cov-report=html:reports/coverage

# 使用运行脚本
python tests/scripts/run_tests.py --env=dev --api --html=reports/api_report.html
```

---

## 框架结构

```
tests/
├── pytest.ini           # pytest配置
├── conftest.py          # 全局fixture
├── api/                 # API测试
├── unit/                # 单元测试
├── integration/         # 集成测试
├── utils/               # 工具模块
│   ├── api_client.py    # API客户端
│   ├── data_generator.py # 数据生成器
│   ├── database_helper.py # 数据库工具
│   ├── retry_helper.py  # 重试机制
│   └── config_loader.py # 配置加载
├── scripts/             # 测试脚本
│   └── run_tests.py     # 运行脚本
└── reports/             # 测试报告
```

---

## 编写测试示例

### API测试模板

```python
import pytest
from utils.api_client import APIClient
from utils.data_generator import DataGenerator


@pytest.mark.api
@pytest.mark.smoke
class TestCustomerAPI:
    """客户API测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self, api_client, data_generator):
        self.client = api_client
        self.generator = data_generator
    
    def test_create_customer_success(self):
        """测试创建客户成功"""
        customer_data = self.generator.generate_customer()
        
        response = self.client.post('/customers', json=customer_data)
        
        assert response.status_code == 201
        assert response.json()['success'] is True
    
    @pytest.mark.critical
    def test_get_customer_by_id(self, authenticated_client):
        """测试查询客户"""
        response = authenticated_client.get('/customers/1')
        
        assert response.status_code == 200
```

---

## 测试标记

| 标记 | 说明 |
|-----|------|
| `@pytest.mark.smoke` | 冒烟测试 |
| `@pytest.mark.api` | API测试 |
| `@pytest.mark.ui` | UI测试 |
| `@pytest.mark.integration` | 集成测试 |
| `@pytest.mark.critical` | 关键测试 |
| `@pytest.mark.slow` | 慢速测试 |

---

## 报告生成

### HTML报告

```bash
pytest --html=reports/test_report.html --self-contained-html
```

### Allure报告

```bash
pytest --alluredir=reports/allure
allure generate reports/allure -o reports/allure-report --clean
allure open reports/allure-report
```

### 覆盖率报告

```bash
pytest --cov=src --cov-report=html --cov-report=term-missing
```

---

## 工具模块使用

### API客户端

```python
from utils.api_client import APIClient

client = APIClient(base_url='http://localhost:8080/api/v1')
client.set_auth_token('your_token')

# GET请求
response = client.get('/customers')

# POST请求
response = client.post('/customers', json={'name': '测试客户'})

# PUT请求
response = client.put('/customers/1', json={'name': '更新客户'})

# DELETE请求
response = client.delete('/customers/1')
```

### 数据生成器

```python
from utils.data_generator import DataGenerator

generator = DataGenerator()

# 生成用户数据
user = generator.generate_user()

# 生成客户数据
customer = generator.generate_customer()

# 生成订单数据
order = generator.generate_order(product_count=3)
```

### 数据库工具

```python
from utils.database_helper import DatabaseHelper

db = DatabaseHelper()

# 查询数据
results = db.query('SELECT * FROM customers WHERE name LIKE "test%"')

# 插入测试数据
db.insert('customers', {'name': '测试客户', 'phone': '13800000000'})

# 清理测试数据
db.cleanup_test_data('customers', prefix='test_')

db.close()
```

---

## 最佳实践

1. **测试命名**: `test_<功能>_<场景>_<预期>`
2. **断言具体**: 验证具体字段，不模糊断言
3. **数据隔离**: 每个测试独立准备和清理数据
4. **使用fixture**: 通过fixture共享测试资源
5. **合理标记**: 为测试添加合适的标记便于筛选

---

**qa-lead** | 2026-03-30