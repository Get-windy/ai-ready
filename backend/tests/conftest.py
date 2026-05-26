"""
测试框架全局配置
提供所有测试共享的夹具和配置
"""

import pytest
import os
import sys
from datetime import datetime
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# 测试配置
TEST_ENV = os.getenv("TEST_ENV", "local")
TEST_TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")

# 日志配置
import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@pytest.fixture(scope="session")
def test_environment():
    """提供测试环境信息"""
    return {
        "env": TEST_ENV,
        "timestamp": TEST_TIMESTAMP,
        "root_dir": str(project_root)
    }

@pytest.fixture(scope="session")
def test_data_dir():
    """提供测试数据目录路径"""
    data_dir = project_root / "backend" / "tests" / "test-data"
    data_dir.mkdir(parents=True, exist_ok=True)
    return data_dir

@pytest.fixture(scope="session")
def test_reports_dir():
    """提供测试报告目录路径"""
    reports_dir = project_root / "backend" / "tests" / "reports"
    reports_dir.mkdir(parents=True, exist_ok=True)
    return reports_dir

@pytest.fixture(scope="session")
def test_logs_dir():
    """提供测试日志目录路径"""
    logs_dir = project_root / "backend" / "tests" / "logs"
    logs_dir.mkdir(parents=True, exist_ok=True)
    return logs_dir

@pytest.fixture(scope="function")
def test_logger():
    """提供测试专用的日志器"""
    return logger

@pytest.fixture(scope="function")
def clean_test_data(test_data_dir):
    """清理测试数据（在每个测试后执行）"""
    # 测试前：记录开始
    logger.info("开始测试")
    
    yield
    
    # 测试后：记录结束
    logger.info("测试完成")
    
    # 这里可以添加数据清理逻辑
    # 例如：删除测试生成的文件，重置数据库等

@pytest.fixture(scope="session", autouse=True)
def setup_test_environment():
    """设置测试环境（在整个测试会话开始时执行）"""
    logger.info(f"设置测试环境: {TEST_ENV}")
    logger.info(f"项目根目录: {project_root}")
    
    # 这里可以添加全局设置逻辑
    # 例如：启动测试数据库，设置环境变量等
    
    yield
    
    # 测试会话结束后清理
    logger.info("清理测试环境")

@pytest.fixture(scope="function", autouse=True)
def test_timing():
    """测量测试执行时间"""
    import time
    start_time = time.time()
    
    yield
    
    end_time = time.time()
    duration = end_time - start_time
    if duration > 1.0:
        logger.warning(f"测试执行时间较长: {duration:.2f}秒")
    else:
        logger.debug(f"测试执行时间: {duration:.2f}秒")

# 自定义pytest钩子
def pytest_sessionstart(session):
    """测试会话开始时调用"""
    logger.info("=" * 60)
    logger.info("开始测试会话")
    logger.info("=" * 60)

def pytest_sessionfinish(session, exitstatus):
    """测试会话结束时调用"""
    logger.info("=" * 60)
    logger.info(f"测试会话结束，退出状态: {exitstatus}")
    logger.info("=" * 60)

def pytest_runtest_logstart(nodeid, location):
    """每个测试开始时记录日志"""
    logger.info(f"开始测试: {nodeid}")

def pytest_runtest_logfinish(nodeid, location):
    """每个测试结束时记录日志"""
    logger.info(f"完成测试: {nodeid}")

# 自定义标记验证
def pytest_configure(config):
    """配置pytest，添加自定义标记"""
    config.addinivalue_line(
        "markers", "slow: 标记为慢速测试（执行时间 > 1秒）"
    )
    config.addinivalue_line(
        "markers", "flaky: 标记为不稳定测试（可能随机失败）"
    )
    config.addinivalue_line(
        "markers", "integration: 集成测试"
    )
    config.addinivalue_line(
        "markers", "e2e: 端到端测试"
    )

# 测试数据生成器
@pytest.fixture
def generate_test_id():
    """生成唯一的测试ID"""
    import uuid
    return lambda: f"test_{uuid.uuid4().hex[:8]}"

@pytest.fixture
def random_string():
    """生成随机字符串"""
    import random
    import string
    return lambda length=10: ''.join(
        random.choices(string.ascii_letters + string.digits, k=length)
    )

# 断言辅助函数
def assert_dict_contains(expected, actual, path=""):
    """断言字典包含预期内容"""
    for key, value in expected.items():
        full_path = f"{path}.{key}" if path else key
        assert key in actual, f"缺少键: {full_path}"
        
        if isinstance(value, dict):
            assert_dict_contains(value, actual[key], full_path)
        else:
            assert actual[key] == value, f"键值不匹配: {full_path} = {actual[key]} (期望: {value})"

# 自定义断言
class CustomAssertions:
    """自定义断言类"""
    
    @staticmethod
    def assert_response_success(response, status_code=200):
        """断言响应成功"""
        assert response.status_code == status_code, \
            f"响应状态码错误: {response.status_code} (期望: {status_code})"
        
        if hasattr(response, 'json'):
            data = response.json()
            assert data.get("success", True), f"响应未成功: {data}"
    
    @staticmethod
    def assert_list_length(items, expected_length):
        """断言列表长度"""
        assert len(items) == expected_length, \
            f"列表长度错误: {len(items)} (期望: {expected_length})"

@pytest.fixture
def assertions():
    """提供自定义断言"""
    return CustomAssertions()

# 测试跳过条件
def skip_if_ci():
    """如果在CI环境中则跳过测试"""
    return pytest.mark.skipif(
        os.getenv("CI") == "true",
        reason="在CI环境中跳过此测试"
    )

def skip_if_not_local():
    """如果不在本地环境则跳过测试"""
    return pytest.mark.skipif(
        TEST_ENV != "local",
        reason="只在本地环境运行此测试"
    )