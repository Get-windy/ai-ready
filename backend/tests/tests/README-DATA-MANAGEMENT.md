# 测试数据管理工具

## 概述

测试数据管理工具提供完整的测试数据生命周期管理，包括数据生成、持久化和清理。

## 核心组件

### 1. TestDataManager

测试数据管理器，支持多种业务数据生成。

#### 使用方法

```python
from utils.test_data_manager import TestDataManager

# 创建管理器
manager = TestDataManager(seed=42)  # 设置随机种子保证可重复性

# 生成单个用户
user = manager.create_user(role="admin")

# 批量生成用户
users = manager.create_users(count=10)

# 生成完整测试数据集
test_data = manager.generate_test_suite_data()

# 保存到文件
manager.save_data("test_suite", test_data)

# 从文件加载
data = manager.load_data("test_suite")
```

#### 支持的数据类型

| 数据类型 | 方法 | 说明 |
|---------|------|------|
| 用户 | create_user() | 生成用户数据 |
| 公司 | create_company() | 生成公司数据 |
| 产品 | create_product() | 生成产品数据 |
| 订单 | create_order() | 生成订单数据 |
| 发票 | create_invoice() | 生成发票数据 |
| 库存 | create_inventory() | 生成库存数据 |

#### 数据生成模板

```python
# API测试数据
api_data = manager.generate_api_test_data()

# 性能测试数据（大数据量）
perf_data = manager.generate_performance_test_data(scale=1000)

# 完整测试套件数据
suite_data = manager.generate_test_suite_data()
```

### 2. DataCleanup

数据清理工具，管理测试数据的清理。

#### 使用方法

```python
from utils.data_cleanup import DataCleanup

# 创建清理工具
cleanup = DataCleanup()

# 清理过期文件（7天前）
cleanup.cleanup_by_age(days=7)

# 保留最近5个文件
cleanup.cleanup_files(keep_recent=5)

# 清理测试报告
cleanup.cleanup_reports(keep_recent=10)

# 清理截图
cleanup.cleanup_screenshots(keep_recent=20)

# 清理所有临时文件
cleanup.cleanup_all_temp()
```

## 数据文件位置

```
tests/
├── data/                    # 测试数据目录
│   ├── test_suite.json      # 完整测试数据集
│   ├── api_test.json        # API测试数据
│   └── perf_test.json       # 性能测试数据
```

## 最佳实践

### 1. 数据隔离

```python
@pytest.fixture
def test_user():
    """每个测试使用独立的数据"""
    manager = TestDataManager()
    user = manager.create_user()
    yield user
    # 清理数据
```

### 2. 数据复用

```python
@pytest.fixture(scope="module")
def shared_data():
    """模块级别共享数据"""
    manager = TestDataManager(seed=42)
    data = manager.generate_test_suite_data()
    yield data
```

### 3. 大数据量测试

```python
def test_performance_with_large_data():
    """使用大数据量进行性能测试"""
    manager = TestDataManager()
    large_data = manager.generate_performance_test_data(scale=10000)
    # 执行性能测试
```

### 4. 数据清理策略

```python
import pytest

@pytest.fixture(autouse=True)
def cleanup_after_test():
    """每个测试后自动清理"""
    yield
    cleanup = DataCleanup()
    cleanup.cleanup_files(keep_recent=3)
```

## 与测试框架集成

### conftest.py配置

```python
import pytest
from utils.test_data_manager import TestDataManager
from utils.data_cleanup import DataCleanup

@pytest.fixture(scope="session")
def test_data_manager():
    """全局测试数据管理器"""
    return TestDataManager(seed=42)

@pytest.fixture(scope="function")
def test_user(test_data_manager):
    """测试用户fixture"""
    return test_data_manager.create_user()

@pytest.fixture(scope="function")
def test_company(test_data_manager):
    """测试公司fixture"""
    return test_data_manager.create_company()

@pytest.fixture(scope="session", autouse=True)
def cleanup_temp_files():
    """会话结束后清理"""
    yield
    cleanup = DataCleanup()
    cleanup.cleanup_all_temp()
```

## 命令行工具

### 生成测试数据

```bash
# 生成完整测试数据集
python -m utils.test_data_manager

# 生成指定数量的数据
python -c "
from utils.test_data_manager import TestDataManager
m = TestDataManager()
m.save_data('users', m.create_users(100))
"
```

### 清理数据

```bash
# 清理过期文件
python -m utils.data_cleanup

# 保留最近N个文件
python -c "
from utils.data_cleanup import DataCleanup
c = DataCleanup()
c.cleanup_files(keep_recent=5)
"
```

## 配置选项

### 本地化

```python
# 中文数据
manager = TestDataManager(locale="zh_CN")

# 英文数据
manager = TestDataManager(locale="en_US")
```

### 随机种子

```python
# 固定种子保证可重复性
manager = TestDataManager(seed=42)

# 随机种子
manager = TestDataManager(seed=None)
```

## 扩展开发

### 自定义数据类型

```python
class CustomDataManager(TestDataManager):
    def create_custom_entity(self, **kwargs):
        """创建自定义实体"""
        entity = {
            "id": self.fake.uuid4(),
            "name": kwargs.get("name", self.fake.word()),
            "custom_field": kwargs.get("custom_field", "default")
        }
        entity.update(kwargs)
        return entity
```

## 注意事项

1. **数据量控制**: 性能测试时注意内存使用
2. **敏感数据**: 避免生成真实的敏感信息
3. **数据清理**: 定期清理过期数据文件
4. **种子设置**: 固定种子保证测试可重复性
