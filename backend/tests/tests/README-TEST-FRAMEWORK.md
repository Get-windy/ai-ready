# AI-Ready 自动化测试框架

## 框架概述

本测试框架基于 **pytest** 构建，支持多种测试类型和并行执行，提供丰富的报告生成功能。

## 技术栈

- **测试框架**: pytest 7.4+
- **并行测试**: pytest-xdist
- **报告生成**: pytest-html, pytest-json-report
- **代码覆盖率**: pytest-cov
- **HTTP客户端**: httpx, requests
- **数据生成**: faker
- **性能测试**: pytest-benchmark

## 安装

### 自动安装

```bash
python setup_test_framework.py
```

### 手动安装

```bash
pip install -r requirements-test.txt
```

## 目录结构

```
tests/
├── api/                    # API测试
├── e2e/                    # 端到端测试
├── integration/            # 集成测试
├── unit/                   # 单元测试
├── performance/            # 性能测试
├── security/               # 安全测试
├── fixtures/               # 测试夹具
├── data/                   # 测试数据
├── reports/                # 测试报告
├── conftest.py             # 全局配置
├── pytest.ini              # pytest配置
├── run_tests.py            # 测试运行器
├── report_generator.py     # 报告生成器
└── test_template.py        # 测试模板
```

## 使用方法

### 运行所有测试

```bash
pytest tests/ -v
```

### 运行特定标记的测试

```bash
# 运行冒烟测试
pytest tests/ -m smoke -v

# 运行API测试
pytest tests/ -m api -v

# 运行集成测试
pytest tests/ -m integration -v
```

### 并行测试

```bash
# 自动检测CPU核心数并并行运行
pytest tests/ -n auto -v

# 指定并行进程数
pytest tests/ -n 4 -v

# 使用loadfile分发模式
pytest tests/ -n auto --dist=loadfile -v
```

### 生成报告

```bash
# HTML报告
pytest tests/ --html=reports/report.html --self-contained-html

# JSON报告
pytest tests/ --json-report --json-report-file=reports/report.json

# 覆盖率报告
pytest tests/ --cov=src --cov-report=html:reports/coverage

# 组合报告
pytest tests/ -v \
  --html=reports/report.html \
  --json-report \
  --cov=src \
  --cov-report=html:reports/coverage
```

### 使用测试运行器

```bash
# 运行所有测试
python run_tests.py all --report --coverage

# 运行冒烟测试
python run_tests.py smoke --parallel

# 运行回归测试
python run_tests.py regression --parallel --workers=4 --report

# 运行性能测试
python run_tests.py performance

# 运行安全测试
python run_tests.py security --report
```

## 标记说明

| 标记 | 说明 | 使用场景 |
|------|------|----------|
| `smoke` | 冒烟测试 | 快速验证核心功能 |
| `regression` | 回归测试 | 验证功能完整性 |
| `api` | API测试 | 接口测试 |
| `e2e` | 端到端测试 | 全流程测试 |
| `integration` | 集成测试 | 模块集成测试 |
| `unit` | 单元测试 | 单元测试 |
| `performance` | 性能测试 | 性能验证 |
| `security` | 安全测试 | 安全验证 |
| `slow` | 慢速测试 | 耗时较长的测试 |
| `serial` | 串行测试 | 不支持并行的测试 |

## 配置文件

### pytest.ini

基础配置文件，包含标记定义和基本选项。

### pytest-parallel.ini

并行测试配置，启用 `-n auto` 自动并行执行。

### pytest-report.ini

报告生成配置，启用HTML、JSON和覆盖率报告。

## 测试模板

参考 `test_template.py` 创建新的测试用例。

## 报告生成

### 生成报告

```python
from report_generator import TestReportGenerator

generator = TestReportGenerator("reports")

# 生成HTML报告
generator.generate_html_report(test_results, "report.html")

# 生成JSON报告
generator.generate_json_report(test_results, "report.json")

# 生成JUnit XML报告
generator.generate_junit_xml(test_results, "junit.xml")
```

### 合并报告

```python
generator.merge_reports(
    ["report1.json", "report2.json"],
    "merged_report.html"
)
```

## 最佳实践

1. **标记分类**: 为每个测试添加适当的标记，便于筛选执行
2. **Fixture复用**: 使用conftest.py定义共享fixture
3. **数据隔离**: 每个测试使用独立的数据，避免相互影响
4. **清理资源**: 使用yield fixture确保资源清理
5. **参数化测试**: 使用@pytest.mark.parametrize减少重复代码
6. **并行测试**: 无状态测试使用并行执行提高效率
7. **超时设置**: 为测试设置合理的超时时间

## 故障排除

### 测试发现失败

```bash
# 检查测试文件命名
# 确保文件名为 test_*.py 或 *_test.py
# 确保类名为 Test*
# 确保方法名为 test_*

# 查看收集的测试
pytest --collect-only
```

### 并行测试失败

```bash
# 标记串行测试
@pytest.mark.serial

def test_serial_only():
    pass

# 排除串行测试
pytest tests/ -n auto -m "not serial"
```

### 覆盖率收集失败

```bash
# 确保安装了pytest-cov
pip install pytest-cov

# 指定源代码路径
pytest tests/ --cov=src --cov-report=html
```
