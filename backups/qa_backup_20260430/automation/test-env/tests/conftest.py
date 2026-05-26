"""
pytest配置文件 - 定义共享的测试夹具和配置

这个文件包含所有测试共享的夹具、钩子和配置。
pytest会自动发现并使用此文件中的夹具。
"""

import pytest
import os
import sys
import logging
from pathlib import Path
from typing import Dict, Any, Generator

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(project_root / 'logs' / 'pytest.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


def pytest_configure(config):
    """pytest配置钩子"""
    # 添加自定义标记
    config.addinivalue_line(
        "markers", "slow: 标记为慢速测试（执行时间>1秒）"
    )
    config.addinivalue_line(
        "markers", "integration: 集成测试"
    )
    config.addinivalue_line(
        "markers", "e2e: 端到端测试"
    )
    config.addinivalue_line(
        "markers", "performance: 性能测试"
    )
    config.addinivalue_line(
        "markers", "api: API测试"
    )
    config.addinivalue_line(
        "markers", "ui: UI测试"
    )
    config.addinivalue_line(
        "markers", "smoke: 冒烟测试"
    )
    config.addinivalue_line(
        "markers", "regression: 回归测试"
    )
    
    logger.info("pytest配置完成")


def pytest_sessionstart(session):
    """测试会话开始钩子"""
    logger.info("测试会话开始")
    
    # 创建必要的目录
    for dir_name in ['logs', 'reports', 'test_data']:
        dir_path = project_root / dir_name
        dir_path.mkdir(exist_ok=True)
        logger.debug(f"创建目录: {dir_path}")


def pytest_sessionfinish(session, exitstatus):
    """测试会话结束钩子"""
    logger.info(f"测试会话结束，退出状态: {exitstatus}")
    
    # 生成会话摘要
    if hasattr(session, 'testscollected'):
        logger.info(f"收集的测试用例数: {session.testscollected}")
    if hasattr(session, 'testfailed'):
        logger.info(f"失败的测试用例数: {session.testfailed}")
    if hasattr(session, 'testpassed'):
        logger.info(f"通过的测试用例数: {session.testpassed}")


# ========== 共享夹具定义 ==========

@pytest.fixture(scope="session")
def project_root_path() -> Path:
    """返回项目根目录路径"""
    return project_root


@pytest.fixture(scope="session")
def test_config() -> Dict[str, Any]:
    """返回测试配置"""
    import yaml
    
    config_path = project_root / "optimized_framework" / "config" / "global.yaml"
    with open(config_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)
    
    # 加载环境特定的配置
    env = os.getenv("TEST_ENV", "test")
    env_config_path = project_root / "optimized_framework" / "config" / "environments" / f"{env}.yaml"
    
    if env_config_path.exists():
        with open(env_config_path, 'r', encoding='utf-8') as f:
            env_config = yaml.safe_load(f)
        config.update(env_config)
    
    return config


@pytest.fixture(scope="function")
def logger_fixture() -> Generator[logging.Logger, None, None]:
    """为每个测试函数提供日志记录器"""
    test_logger = logging.getLogger("test")
    yield test_logger
    
    # 清理日志处理器
    for handler in test_logger.handlers[:]:
        handler.close()
        test_logger.removeHandler(handler)


@pytest.fixture(scope="function")
def test_data_dir(project_root_path: Path) -> Path:
    """返回测试数据目录路径"""
    data_dir = project_root_path / "tests" / "data"
    data_dir.mkdir(exist_ok=True)
    return data_dir


@pytest.fixture(scope="function")
def temp_dir(project_root_path: Path) -> Path:
    """返回临时目录路径"""
    temp_dir = project_root_path / "temp"
    temp_dir.mkdir(exist_ok=True)
    yield temp_dir
    
    # 测试结束后清理临时目录
    import shutil
    if temp_dir.exists():
        shutil.rmtree(temp_dir)


@pytest.fixture(scope="session")
def db_connection(test_config: Dict[str, Any]):
    """数据库连接夹具"""
    import psycopg2
    
    db_config = test_config.get('database', {}).get('postgresql', {})
    
    if not db_config:
        pytest.skip("数据库配置未找到")
    
    try:
        conn = psycopg2.connect(
            host=db_config.get('host', 'localhost'),
            port=db_config.get('port', 5432),
            database=db_config.get('database', 'test'),
            user=db_config.get('user', 'test_user'),
            password=db_config.get('password', 'test_password')
        )
        logger.info("数据库连接成功")
        yield conn
    except Exception as e:
        logger.error(f"数据库连接失败: {e}")
        pytest.skip(f"数据库连接失败: {e}")
    finally:
        if 'conn' in locals():
            conn.close()
            logger.info("数据库连接已关闭")


@pytest.fixture(scope="function")
def redis_client(test_config: Dict[str, Any]):
    """Redis客户端夹具"""
    import redis
    
    redis_config = test_config.get('database', {}).get('redis', {})
    
    if not redis_config:
        pytest.skip("Redis配置未找到")
    
    try:
        client = redis.Redis(
            host=redis_config.get('host', 'localhost'),
            port=redis_config.get('port', 6379),
            db=redis_config.get('db', 0),
            password=redis_config.get('password'),
            decode_responses=True
        )
        # 测试连接
        client.ping()
        logger.info("Redis连接成功")
        yield client
    except Exception as e:
        logger.error(f"Redis连接失败: {e}")
        pytest.skip(f"Redis连接失败: {e}")
    finally:
        if 'client' in locals():
            client.close()
            logger.info("Redis连接已关闭")


@pytest.fixture(scope="function")
def api_client(test_config: Dict[str, Any]):
    """API客户端夹具"""
    import requests
    
    api_config = test_config.get('api_gateway', {})
    
    if not api_config:
        pytest.skip("API网关配置未找到")
    
    base_url = api_config.get('url', 'http://localhost:8080')
    timeout = api_config.get('timeout', 30)
    
    session = requests.Session()
    session.headers.update({
        'User-Agent': 'AI-Ready-Test-Framework/1.0',
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    })
    
    yield {
        'session': session,
        'base_url': base_url,
        'timeout': timeout
    }
    
    session.close()


# ========== 自定义钩子 ==========

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """生成测试报告"""
    outcome = yield
    report = outcome.get_result()
    
    if report.when == "call":
        # 记录测试结果
        test_name = item.name
        duration = report.duration
        
        if report.passed:
            logger.info(f"测试通过: {test_name} (耗时: {duration:.2f}s)")
        elif report.failed:
            logger.error(f"测试失败: {test_name} (耗时: {duration:.2f}s)")
            if call.excinfo:
                logger.error(f"异常信息: {call.excinfo.value}")
        elif report.skipped:
            logger.warning(f"测试跳过: {test_name} (原因: {report.longrepr})")


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_protocol(item, nextitem):
    """测试协议钩子 - 用于测试前后的额外处理"""
    logger.info(f"开始测试: {item.name}")
    
    # 执行测试
    outcome = yield
    
    logger.info(f"结束测试: {item.name}")
    
    return outcome.get_result()