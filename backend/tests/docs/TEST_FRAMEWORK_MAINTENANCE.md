# 自动化测试框架维护指南

## 概述

本文档提供了AI-Ready项目自动化测试框架的维护指南，包括配置管理、最佳实践、故障排除和优化建议。

## 目录结构

```
backend/tests/
├── api/                    # API测试
│   ├── test_*.py          # API测试文件
│   └── conftest.py        # API测试配置
├── unit/                  # 单元测试
│   ├── test_*.py          # 单元测试文件
│   └── conftest.py        # 单元测试配置
├── integration/           # 集成测试
│   ├── test_*.py          # 集成测试文件
│   └── conftest.py        # 集成测试配置
├── e2e/                   # 端到端测试
│   ├── test_*.py          # E2E测试文件
│   └── conftest.py        # E2E测试配置
├── performance/           # 性能测试
│   ├── test_*.py          # 性能测试文件
│   ├── scripts/           # 性能测试脚本
│   └── config/            # 性能测试配置
├── reports/               # 测试报告
│   ├── junit.xml          # JUnit格式报告
│   ├── report.html        # HTML报告
│   └── coverage-html/     # 覆盖率报告
├── scripts/               # 测试脚本
│   └── test-execution-script.sh  # 测试执行脚本
├── docs/                  # 测试文档
│   ├── TEST_FRAMEWORK_MAINTENANCE.md  # 本文件
│   └── api_automation_test_template.md # API测试模板
├── docker/                # 测试容器配置
│   ├── docker-compose.test-env.yml
│   └── Dockerfile
└── results/               # 测试结果
```

## 配置文件说明

### 1. pytest.ini (主配置文件)

**位置**: `backend/tests/pytest.ini`

**关键配置项**:
- `addopts`: 执行选项，包括并行执行、报告生成等
- `markers`: 测试标记系统，用于分类和组织测试
- `timeout`: 单个测试超时时间
- `cov_fail_under`: 覆盖率最低要求

### 2. conftest.py (测试夹具配置)

每个测试目录都可以有自己的`conftest.py`文件，用于定义该目录特有的测试夹具。

**示例**:
```python
import pytest
from backend.core.database import get_test_db

@pytest.fixture(scope="session")
def test_database():
    """提供测试数据库连接"""
    db = get_test_db()
    yield db
    db.cleanup()
```

## 测试标记系统

### 测试类型标记
- `@pytest.mark.unit`: 单元测试
- `@pytest.mark.integration`: 集成测试
- `@pytest.mark.api`: API测试
- `@pytest.mark.e2e`: 端到端测试
- `@pytest.mark.performance`: 性能测试

### 执行速度标记
- `@pytest.mark.fast`: 快速测试（< 100ms）
- `@pytest.mark.medium`: 中等速度测试（100ms - 1s）
- `@pytest.mark.slow`: 慢速测试（> 1s）

### 执行策略标记
- `@pytest.mark.smoke`: 冒烟测试（快速验证核心功能）
- `@pytest.mark.regression`: 回归测试
- `@pytest.mark.flaky`: 不稳定的测试（需要修复）

### 使用示例
```python
import pytest

@pytest.mark.unit
@pytest.mark.fast
def test_user_creation():
    """快速单元测试"""
    pass

@pytest.mark.e2e
@pytest.mark.slow
def test_order_workflow():
    """慢速端到端测试"""
    pass
```

## 测试执行策略

### 1. 快速测试套件
```bash
# 只运行快速测试
pytest -m "fast"

# 运行快速单元测试
pytest -m "unit and fast"

# 运行快速API测试
pytest -m "api and fast"
```

### 2. CI/CD流水线
```bash
# 开发阶段：运行快速测试
pytest -m "fast" --tb=short

# 合并请求：运行所有非慢速测试
pytest -m "not slow" --junitxml=reports/junit.xml

# 发布阶段：运行所有测试
pytest --junitxml=reports/junit.xml --html=reports/report.html
```

### 3. 并行执行
```bash
# 自动根据CPU核心数并行执行
pytest -n auto

# 指定并行进程数
pytest -n 4
```

## 测试数据管理

### 1. 测试数据目录
```
test-data/
├── fixtures/          # 测试夹具数据
├── scenarios/         # 测试场景数据
├── performance/       # 性能测试数据
└── validation/        # 验证数据
```

### 2. 数据生成策略
- **单元测试**: 使用模拟数据
- **集成测试**: 使用测试数据库
- **E2E测试**: 使用预置的真实数据
- **性能测试**: 使用大规模生成数据

### 3. 数据清理
```python
@pytest.fixture(autouse=True)
def cleanup_test_data():
    """测试后自动清理数据"""
    yield
    cleanup_database()
```

## 测试报告

### 1. HTML报告
**位置**: `reports/report.html`
**生成命令**: `pytest --html=reports/report.html`

### 2. JUnit XML报告
**位置**: `reports/junit.xml`
**用途**: CI/CD工具集成

### 3. 覆盖率报告
**位置**: `reports/coverage-html/index.html`
**生成命令**: `pytest --cov=backend --cov-report=html`

## 性能优化

### 1. 并行执行
- 配置: `-n auto` 或 `-n 4`
- 适用场景: 独立测试用例
- 不适用场景: 共享资源的测试

### 2. 测试隔离
```python
# 使用事务回滚
@pytest.fixture
def db_session():
    session = create_session()
    transaction = session.begin()
    yield session
    transaction.rollback()
```

### 3. 资源重用
```python
# 会话级夹具，避免重复创建
@pytest.fixture(scope="session")
def database_connection():
    conn = create_connection()
    yield conn
    conn.close()
```

## 故障排除

### 常见问题

#### 1. 测试超时
**症状**: `pytest-timeout` 插件报告超时
**解决方案**:
- 检查测试是否包含无限循环
- 增加超时时间: `timeout = 600`
- 标记为慢速测试: `@pytest.mark.slow`

#### 2. 内存泄漏
**症状**: 测试运行后内存不释放
**解决方案**:
- 检查夹具是否正确清理资源
- 使用 `pytest-leaks` 插件检测
- 确保数据库连接正确关闭

#### 3. 测试不稳定
**症状**: 测试结果时好时坏
**解决方案**:
- 添加重试机制
- 标记为不稳定: `@pytest.mark.flaky`
- 使用 `pytest-rerunfailures` 插件

#### 4. 并行执行冲突
**症状**: 并行测试失败，串行测试成功
**解决方案**:
- 确保测试相互独立
- 使用不同的测试数据库
- 添加适当的同步机制

## 最佳实践

### 1. 测试组织
- 按功能模块组织测试文件
- 使用清晰的命名约定
- 保持测试文件大小适中（< 1000行）

### 2. 测试质量
- 每个测试只验证一个功能点
- 使用有意义的断言消息
- 避免测试之间的依赖

### 3. 维护性
- 定期清理过时的测试
- 更新测试以适应代码变更
- 记录测试决策和假设

### 4. 性能
- 快速测试应该真的快速（< 100ms）
- 使用适当的测试标记
- 优化测试数据加载

## 监控与告警

### 1. 测试执行监控
- 跟踪测试执行时间趋势
- 监控测试失败率
- 记录测试覆盖率变化

### 2. 告警规则
- 测试失败率 > 10% 时告警
- 测试执行时间增加 > 50% 时告警
- 覆盖率下降 > 5% 时告警

### 3. 报告模板
```python
# 自定义报告生成
def generate_test_report():
    """生成自定义测试报告"""
    pass
```

## 版本兼容性

### 支持的Python版本
- Python 3.8+
- pytest 7.0+
- pytest插件: 最新稳定版

### 数据库兼容性
- PostgreSQL 12+
- MySQL 8.0+
- SQLite (用于快速测试)

## 更新日志

### 2026-04-30
- 创建统一的pytest配置
- 建立测试标记系统
- 添加并行执行支持
- 创建维护文档

### 2026-04-29
- 优化测试数据管理
- 改进测试报告生成
- 添加性能测试框架

## 贡献指南

1. 遵循测试代码规范
2. 为新功能添加测试
3. 保持测试独立性和可重复性
4. 更新相关文档
5. 运行现有测试确保兼容性

## 联系方式

- 测试负责人: test-agent-1
- 项目组: ai-ready
- 问题反馈: 通过项目组频道报告