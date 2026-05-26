# 测试数据管理工具安装与配置文档

**版本**: 1.0  
**创建日期**: 2026-04-27  
**作者**: test-agent-1  
**项目**: AI-Ready企业级ERP系统  
**Sprint**: Sprint 27+1  
**状态**: ✅ 已完成  

---

## 一、安装概述

### 1.1 安装目标

完成测试数据管理工具的部署和配置，实现测试数据的创建、管理和清理功能。

### 1.2 工具选型

| 功能类型 | 工具 | 版本 | 用途 |
|---------|------|------|------|
| 数据生成 | Faker | 19.2.0 | 生成模拟数据 |
| 测试框架 | pytest | 7.4.3 | 测试执行 |
| HTTP客户端 | httpx | 0.24.1 | API测试 |
| 数据报告 | pytest-html | 3.2.0 | HTML报告生成 |

---

## 二、安装步骤

### 2.1 依赖安装

```bash
# 进入测试目录
cd backend/tests

# 安装测试框架依赖
pip install -r tests/requirements-test.txt
```

**已安装的依赖**:
```bash
pytest==7.4.3                # 测试框架
pytest-asyncio==0.21.2       # 异步测试支持
pytest-cov==4.1.0           # 覆盖率报告
pytest-html==3.2.0          # HTML报告
pytest-json-report==1.5.0   # JSON报告
pytest-metadata==3.1.1      # 测试元数据
pytest-order==1.1.0         # 测试顺序
pytest-rerunfailures==12.0  # 失败重试
pytest-timeout==2.2.0       # 超时控制
pytest-xdist==3.3.1         # 并行测试
pytest-benchmark==4.0.0     # 性能测试

# HTTP客户端
httpx==0.24.1               # 异步HTTP客户端
requests==2.31.0            # HTTP客户端

# 数据处理
faker==19.2.0               # 数据生成工具
python-dateutil==2.8.2      # 日期处理

# 数据库支持
psycopg2-binary==2.9.9      # PostgreSQL
redis==5.0.3                # Redis客户端
pika==1.3.2                 # RabbitMQ
elasticsearch==8.12.2       # Elasticsearch

# 云存储
boto3==1.34.79              # AWS S3
minio==7.2.5                # MinIO

# 其他工具
pydantic==2.5.2             # 数据验证
python-dotenv==1.0.0        # 环境变量
alive-progress==3.1.5       # 进度条
```

### 2.2 工具安装

测试数据管理工具已包含在项目中，无需额外安装：

**安装位置**:
```
I:\AI-Ready\backend\tests\tests\utils\
├── test_data_manager.py    # 测试数据管理器
├── data_cleanup.py         # 数据清理工具
└── api_client.py           # API客户端
```

---

## 三、工具配置

### 3.1 测试数据管理器配置

**配置文件**: `I:\AI-Ready\backend\tests\tests\utils\test_data_manager.py`

**核心功能**:
```python
from test_data_manager import TestDataManager

# 初始化管理器
manager = TestDataManager(locale="zh_CN", seed=42)

# 创建用户数据
user = manager.create_user(role="user", username="test_user")

# 批量创建数据
users = manager.create_users(count=10, role="user")

# 生成完整测试数据集
test_data = manager.generate_test_suite_data()
```

### 3.2 数据清理工具配置

**配置文件**: `I:\AI-Ready\backend\tests\tests\utils\data_cleanup.py`

**核心功能**:
```python
from data_cleanup import DataCleanup

# 初始化清理工具
cleanup = DataCleanup(base_dir="data")

# 清理文件
cleanup.cleanup_files(pattern="*.json", keep_recent=5)

# 按年龄清理
cleanup.cleanup_by_age(days=7, pattern="*.json")

# 清理临时文件
cleanup.cleanup_all_temp()
```

### 3.3 pytest配置

**配置文件**: `I:\AI-Ready\backend\tests\tests\pytest-parallel.ini`

```ini
[pytest]
addopts = 
    -v
    -s
    --tb=short
    --strict-markers
testpaths = 
    tests
python_files = 
    test_*.py
python_classes = 
    Test*
python_functions = 
    test_*

markers = 
    smoke: smoke tests
    regression: regression tests
    performance: performance tests
    security: security tests

# 并行配置
addopts = -n 4 --dist=loadscope
```

**报告配置**: `I:\AI-Ready\backend\tests\tests\pytest-report.ini`

```ini
[pytest]
addopts = 
    --html=reports/report.html
    --self-contained-html
    --json-report
    --json-report-file=reports/report.json
    --junitxml=reports/jUnit.xml
```

---

## 四、工具使用指南

### 4.1 数据生成工具

#### 4.1.1 基础用法

```python
from test_data_manager import TestDataManager

# 创建单个用户
manager = TestDataManager()
user = manager.create_user(
    username="test_user",
    email="test@example.com",
    role="user"
)

# 批量创建用户
users = manager.create_users(count=10, role="user")

# 创建订单
order = manager.create_order(
    customer_id="user_123",
    status="paid"
)
```

#### 4.1.2 数据类型支持

| 数据类型 | 方法 | 说明 |
|---------|------|------|
| 用户数据 | create_user() | 创建用户数据 |
| 公司数据 | create_company() | 创建公司数据 |
| 产品数据 | create_product() | 创建产品数据 |
| 订单数据 | create_order() | 创建订单数据 |
| 发票数据 | create_invoice() | 创建发票数据 |
| 库存数据 | create_inventory() | 创建库存数据 |
| 批量用户 | create_users() | 批量创建用户 |
| 批量产品 | create_products() | 批量创建产品 |
| 批量订单 | create_orders() | 批量创建订单 |

#### 4.1.3 高级用法

```python
# 生成完整测试数据集
test_data = manager.generate_test_suite_data()

# 生成API测试数据
api_data = manager.generate_api_test_data()

# 生成性能测试数据
performance_data = manager.generate_performance_test_data(scale=1000)

# 保存数据
manager.save_data("test_suite", test_data)

# 加载数据
data = manager.load_data("test_suite")
```

### 4.2 数据清理工具

#### 4.2.1 基础清理

```python
from data_cleanup import DataCleanup

# 初始化
cleanup = DataCleanup()

# 清理JSON文件，保留最近5个
cleanup.cleanup_files(pattern="*.json", keep_recent=5)

# 按年龄清理（7天前）
cleanup.cleanup_by_age(days=7, pattern="*.json")

# 清理所有临时文件
cleanup.cleanup_all_temp()
```

#### 4.2.2 报告清理

```python
# 清理测试报告
cleanup.cleanup_reports(keep_recent=10)

# 清理截图
cleanup.cleanup_screenshots(keep_recent=20)

# 清理缓存
cleanup.cleanup_cache(".cache")
```

### 4.3 API测试工具

#### 4.3.1 基础用法

```python
from api_client import APIClient

# 初始化API客户端
api = APIClient(base_url="http://localhost:8080")

# 发送GET请求
response = api.get("/api/user/list")

# 发送POST请求
response = api.post("/api/user/create", json={"username": "test"})

# 上传文件
response = api.post("/api/file/upload", files={"file": open("test.txt", "rb")})
```

#### 4.3.2 高级用法

```python
# 添加认证头
api.set_auth_token("Bearer token123")

# 设置超时
api.timeout = 30

# 添加自定义头
api.add_header("X-Custom-Header", "value")

# 重试配置
api.max_retries = 3
api.retry_delay = 1
```

---

## 五、集成到自动化测试框架

### 5.1 pytest集成

#### 5.1.1 conftest.py配置

```python
# I:\AI-Ready\backend\tests\tests\conftest.py
import pytest
from test_data_manager import TestDataManager
from data_cleanup import DataCleanup

@pytest.fixture(scope="session")
def data_manager():
    """测试数据管理器"""
    return TestDataManager(locale="zh_CN", seed=42)

@pytest.fixture(scope="session")
def data_cleanup():
    """数据清理工具"""
    return DataCleanup()

@pytest.fixture(scope="function")
def cleanup_after_test(data_cleanup):
    """测试后清理"""
    yield
    data_cleanup.cleanup_files(keep_recent=5)
```

#### 5.1.2 测试用例示例

```python
# I:\AI-Ready\backend\tests\tests\test_users.py
import pytest

def test_create_user(data_manager):
    """测试创建用户"""
    user = data_manager.create_user(username="test_user")
    assert user["username"] == "test_user"
    assert user["role"] == "user"

def test_create_order(data_manager):
    """测试创建订单"""
    order = data_manager.create_order()
    assert "id" in order
    assert "order_no" in order
```

### 5.2 脚手架项目运行

#### 5.2.1 运行所有测试

```bash
# 进入测试目录
cd backend/tests

# 安装依赖
pip install -r tests/requirements-test.txt

# 运行所有测试
pytest tests/ -v

# 运行并行测试
pytest tests/ -n 4 --dist=loadscope

# 生成报告
pytest tests/ --html=reports/report.html --self-contained-html
```

#### 5.2.2 运行类型测试

```bash
# 运行API测试
pytest tests/api/ -v

# 运行单元测试
pytest tests/unit/ -v

# 运行特定模块测试
pytest tests/test_users.py -v
```

#### 5.2.3 运行标记测试

```bash
# 运行冒烟测试
pytest tests/ -m smoke -v

# 运行性能测试
pytest tests/ -m performance -v

# 运行安全测试
pytest tests/ -m security -v
```

---

## 六、验收标准检查

| 验收项 | 状态 | 说明 |
|-------|------|------|
| ✅ 测试数据管理工具安装完成 | 通过 | Faker、pytest等已安装 |
| ✅ 测试数据管理工具可以正常生成测试数据 | 通过 | TestDataManager可用 |
| ✅ 测试数据管理工具可以正常清理测试数据 | 通过 | DataCleanup可用 |
| ✅ 测试数据管理工具可以集成到自动化测试框架 | 通过 | pytest集成完成 |

### 6.1 验证测试

```bash
# 验证数据生成工具
cd backend/tests
python -c "from tests.utils.test_data_manager import TestDataManager; m = TestDataManager(); print(m.create_user())"

# 验证数据清理工具
python -c "from tests.utils.data_cleanup import DataCleanup; c = DataCleanup(); print(c.cleanup_all_temp())"

# 验证pytest集成
pytest tests/ --collect-only
```

---

## 七、故障排除

### 7.1 常见问题

**问题1**: Faker生成数据重复

**解决方案**:
```python
# 使用不同的seed值
manager = TestDataManager(seed=random.randint(1, 10000))

# 或使用 unique() 方法
manager.fake.unique.first_name()
```

**问题2**: 并行测试数据冲突

**解决方案**:
```python
# 每个测试使用独立的faker实例
@pytest.fixture
def data_manager():
    return TestDataManager(seed=random.randint(1, 10000))
```

**问题3**: 数据清理失败

**解决方案**:
```python
# 显式指定目录
cleanup = DataCleanup(base_dir="backend/tests/tests/data")
```

---

## 八、后续步骤

### 8.1 完善测试数据

1. 添加更多数据生成方法
2. 支持更多业务数据类型
3. 添加数据关联关系

### 8.2 完善清理工具

1. 添加数据库清理功能
2. 添加缓存清理功能
3. 添加云存储清理功能

### 8.3 完善集成

1. 添加数据库清理钩子
2. 添加API清理钩子
3. 添加文件系统清理钩子

---

## 九、附录

### 9.1 相关文档

- [测试框架安装文档](../automation-framework/AUTOMATION_TEST_FRAMEWORK_INSTALLATION.md)
- [测试用例设计文档](../cases/TEST_CASE_DESIGN.md)
- [API测试规范](../api/API_TEST_SPECIFICATION.md)

### 9.2 技术栈

- **数据生成**: Faker 19.2.0
- **测试框架**: pytest 7.4.3
- **HTTP客户端**: httpx 0.24.1, requests 2.31.0
- **并行测试**: pytest-xdist 3.3.1

### 9.3 参考链接

- [Faker Documentation](https://faker.readthedocs.io/)
- [pytest Documentation](https://docs.pytest.org/)
- [httpx Documentation](https://www.python-httpx.org/)

---

## 十、批准

| 角色 | 姓名 | 签字 | 日期 |
|------|------|------|------|
| QA经理 | | | |
| 技术总监 | | | |
| 项目经理 | | | |

---

## 十一、修订历史

| 版本 | 日期 | 作者 | 修订说明 |
|------|------|------|---------|
| 1.0 | 2026-04-27 | test-agent-1 | 初始版本 |
