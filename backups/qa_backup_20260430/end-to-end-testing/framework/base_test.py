"""
端到端测试基础类
提供所有测试用例的公共功能
"""

import os
import sys
import time
import logging
import json
import pytest
from typing import Dict, Any, Optional
from datetime import datetime

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from framework.config import TestConfig
from framework.utils import TestUtils


class BaseE2ETest:
    """端到端测试基础类"""
    
    def __init__(self):
        self.config = TestConfig()
        self.utils = TestUtils()
        self.test_start_time = None
        self.test_data = {}
        
        # 设置日志
        self.logger = logging.getLogger(self.__class__.__name__)
        if not self.logger.handlers:
            self._setup_logging()
    
    def _setup_logging(self):
        """设置日志配置"""
        log_dir = self.config.get('logging.directory', 'logs')
        os.makedirs(log_dir, exist_ok=True)
        
        log_file = os.path.join(
            log_dir, 
            f"{self.__class__.__name__}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        )
        
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        
        # 文件处理器
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(formatter)
        
        # 控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setFormatter(formatter)
        
        self.logger.addHandler(file_handler)
        self.logger.addHandler(console_handler)
        self.logger.setLevel(logging.INFO)
    
    def setup_method(self):
        """测试方法前置设置"""
        self.test_start_time = time.time()
        self.logger.info(f"开始测试: {self.__class__.__name__}")
        
        # 初始化测试数据
        self.test_data = {
            'test_name': self.__class__.__name__,
            'start_time': datetime.now().isoformat(),
            'steps': []
        }
    
    def teardown_method(self):
        """测试方法后置清理"""
        duration = time.time() - self.test_start_time
        self.test_data['end_time'] = datetime.now().isoformat()
        self.test_data['duration'] = duration
        self.test_data['status'] = 'passed'
        
        # 保存测试数据
        self._save_test_data()
        
        self.logger.info(f"测试完成: {self.__class__.__name__}, 耗时: {duration:.2f}秒")
    
    def _save_test_data(self):
        """保存测试数据到JSON文件"""
        report_dir = self.config.get('report.directory', 'reports')
        os.makedirs(report_dir, exist_ok=True)
        
        report_file = os.path.join(
            report_dir,
            f"{self.__class__.__name__}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        )
        
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(self.test_data, f, indent=2, ensure_ascii=False)
    
    def add_test_step(self, step_name: str, details: Dict[str, Any], status: str = 'passed'):
        """添加测试步骤记录"""
        step_data = {
            'name': step_name,
            'details': details,
            'status': status,
            'timestamp': datetime.now().isoformat()
        }
        self.test_data['steps'].append(step_data)
        
        if status == 'failed':
            self.test_data['status'] = 'failed'
            self.logger.error(f"测试步骤失败: {step_name}")
        else:
            self.logger.info(f"测试步骤完成: {step_name}")
    
    def assert_true(self, condition: bool, message: str = ""):
        """断言条件为真"""
        if not condition:
            self.add_test_step(
                f"断言失败: {message}",
                {'condition': condition, 'message': message},
                'failed'
            )
            raise AssertionError(message)
        self.add_test_step(
            f"断言通过: {message}",
            {'condition': condition, 'message': message},
            'passed'
        )
    
    def assert_equal(self, actual, expected, message: str = ""):
        """断言两个值相等"""
        if actual != expected:
            self.add_test_step(
                f"断言失败: {message}",
                {'actual': actual, 'expected': expected, 'message': message},
                'failed'
            )
            raise AssertionError(f"{message}: 实际值={actual}, 期望值={expected}")
        self.add_test_step(
            f"断言通过: {message}",
            {'actual': actual, 'expected': expected, 'message': message},
            'passed'
        )
    
    def assert_not_none(self, value, message: str = ""):
        """断言值不为None"""
        if value is None:
            self.add_test_step(
                f"断言失败: {message}",
                {'value': value, 'message': message},
                'failed'
            )
            raise AssertionError(f"{message}: 值为None")
        self.add_test_step(
            f"断言通过: {message}",
            {'value': value, 'message': message},
            'passed'
        )
    
    def wait_for_condition(self, condition_func, timeout: int = 30, interval: float = 1.0):
        """等待条件满足"""
        start_time = time.time()
        while time.time() - start_time < timeout:
            if condition_func():
                return True
            time.sleep(interval)
        return False


@pytest.fixture(scope="function")
def e2e_test_base():
    """Pytest fixture for base test class"""
    test_instance = BaseE2ETest()
    yield test_instance
    # 清理工作由teardown_method处理


if __name__ == "__main__":
    # 测试基础类
    test = BaseE2ETest()
    test.setup_method()
    
    # 测试断言方法
    try:
        test.assert_true(True, "基础断言测试")
        test.assert_equal(1, 1, "相等断言测试")
        test.assert_not_none("test", "非空断言测试")
        print("基础测试类功能正常")
    except AssertionError as e:
        print(f"测试失败: {e}")
    
    test.teardown_method()