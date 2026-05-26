"""
测试框架基类
"""

import pytest
import logging
import time
from datetime import datetime
from typing import Dict, Any, List, Optional
from abc import ABC, abstractmethod


class BaseTest(ABC):
    """测试基类"""
    
    def __init__(self, test_config: Dict = None):
        self.test_config = test_config or {}
        self.logger = self._setup_logger()
        self.start_time = None
        self.end_time = None
        self.test_result = None
        self.test_data = {}
        
    def _setup_logger(self) -> logging.Logger:
        """设置日志记录器"""
        logger = logging.getLogger(f"{self.__class__.__module__}.{self.__class__.__name__}")
        logger.setLevel(logging.INFO)
        return logger
    
    @abstractmethod
    def setup(self):
        """测试设置"""
        pass
    
    @abstractmethod
    def execute(self):
        """测试执行"""
        pass
    
    @abstractmethod
    def teardown(self):
        """测试清理"""
        pass
    
    def run(self) -> Dict[str, Any]:
        """运行测试"""
        self.start_time = datetime.now()
        
        try:
            self.logger.info(f"开始测试: {self.__class__.__name__}")
            
            # 执行测试流程
            self.setup()
            self.execute()
            
            # 测试通过
            self.test_result = {
                "status": "PASSED",
                "message": "测试执行成功",
                "duration": (datetime.now() - self.start_time).total_seconds(),
            }
            
        except AssertionError as e:
            # 断言失败
            self.test_result = {
                "status": "FAILED",
                "message": f"断言失败: {str(e)}",
                "duration": (datetime.now() - self.start_time).total_seconds(),
                "error_type": "AssertionError",
                "error_message": str(e),
            }
            self.logger.error(f"测试失败: {e}")
            
        except Exception as e:
            # 其他异常
            self.test_result = {
                "status": "ERROR",
                "message": f"测试执行异常: {str(e)}",
                "duration": (datetime.now() - self.start_time).total_seconds(),
                "error_type": type(e).__name__,
                "error_message": str(e),
                "traceback": self._get_traceback(e),
            }
            self.logger.error(f"测试异常: {e}", exc_info=True)
            
        finally:
            # 清理资源
            try:
                self.teardown()
            except Exception as e:
                self.logger.error(f"清理过程异常: {e}")
            
            self.end_time = datetime.now()
            
            # 记录测试结果
            self._record_result()
            
        return self.test_result
    
    def _get_traceback(self, exception: Exception) -> str:
        """获取异常堆栈信息"""
        import traceback
        return traceback.format_exc()
    
    def _record_result(self):
        """记录测试结果"""
        if self.test_result:
            self.logger.info(f"测试完成: {self.test_result['status']} - {self.test_result['message']}")
    
    def assert_equal(self, actual, expected, message: str = ""):
        """断言相等"""
        if actual != expected:
            error_msg = f"期望值: {expected}, 实际值: {actual}"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)
    
    def assert_not_equal(self, actual, expected, message: str = ""):
        """断言不相等"""
        if actual == expected:
            error_msg = f"期望值不等于: {expected}, 实际值: {actual}"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)
    
    def assert_true(self, condition, message: str = ""):
        """断言为真"""
        if not condition:
            error_msg = f"期望条件为真，实际为假"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)
    
    def assert_false(self, condition, message: str = ""):
        """断言为假"""
        if condition:
            error_msg = f"期望条件为假，实际为真"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)
    
    def assert_contains(self, container, item, message: str = ""):
        """断言包含"""
        if item not in container:
            error_msg = f"期望包含: {item}, 实际容器: {container}"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)
    
    def assert_not_contains(self, container, item, message: str = ""):
        """断言不包含"""
        if item in container:
            error_msg = f"期望不包含: {item}, 实际容器: {container}"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)


class APITest(BaseTest):
    """API测试基类"""
    
    def __init__(self, test_config: Dict = None):
        super().__init__(test_config)
        self.api_client = None
        self.response = None
        
    def setup(self):
        """API测试设置"""
        from .clients.api_client import APIClient
        
        # 初始化API客户端
        base_url = self.test_config.get("api_base_url", "http://localhost:8080")
        self.api_client = APIClient(base_url=base_url)
        
        self.logger.info(f"API测试设置完成，基础URL: {base_url}")
    
    def assert_status_code(self, expected_code: int, message: str = ""):
        """断言状态码"""
        if self.response is None:
            raise AssertionError("响应对象为空")
        
        actual_code = self.response.status_code
        self.assert_equal(actual_code, expected_code, 
                         message or f"状态码断言失败")
    
    def assert_response_schema(self, schema: Dict, message: str = ""):
        """断言响应格式"""
        if self.response is None:
            raise AssertionError("响应对象为空")
        
        # 这里可以集成jsonschema验证
        response_data = self.response.json()
        
        # 简单的schema验证
        for key, expected_type in schema.items():
            if key not in response_data:
                raise AssertionError(f"缺少字段: {key}")
            
            actual_type = type(response_data[key]).__name__
            expected_type_name = expected_type.__name__ if hasattr(expected_type, '__name__') else str(expected_type)
            
            if actual_type != expected_type_name:
                raise AssertionError(f"字段类型不匹配: {key} - 期望: {expected_type_name}, 实际: {actual_type}")


class UITest(BaseTest):
    """UI测试基类"""
    
    def __init__(self, test_config: Dict = None):
        super().__init__(test_config)
        self.driver = None
        self.page_objects = {}
        
    def setup(self):
        """UI测试设置"""
        from selenium import webdriver
        from selenium.webdriver.chrome.options import Options
        
        # 配置浏览器选项
        options = Options()
        
        # 根据配置添加选项
        if self.test_config.get("headless", True):
            options.add_argument("--headless")
        
        if self.test_config.get("disable_gpu", True):
            options.add_argument("--disable-gpu")
        
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        
        # 初始化浏览器驱动
        self.driver = webdriver.Chrome(options=options)
        self.driver.implicitly_wait(self.test_config.get("implicit_wait", 10))
        
        self.logger.info("UI测试设置完成，浏览器已启动")
    
    def teardown(self):
        """UI测试清理"""
        if self.driver:
            self.driver.quit()
            self.logger.info("浏览器已关闭")


class PerformanceTest(BaseTest):
    """性能测试基类"""
    
    def __init__(self, test_config: Dict = None):
        super().__init__(test_config)
        self.metrics = {}
        self.performance_data = {}
        
    def setup(self):
        """性能测试设置"""
        self.logger.info("性能测试设置完成")
    
    def record_metric(self, metric_name: str, value: Any, timestamp: datetime = None):
        """记录性能指标"""
        if timestamp is None:
            timestamp = datetime.now()
        
        if metric_name not in self.metrics:
            self.metrics[metric_name] = []
        
        self.metrics[metric_name].append({
            "value": value,
            "timestamp": timestamp.isoformat(),
        })
    
    def assert_performance(self, metric_name: str, threshold: float, 
                          comparison: str = "lt", message: str = ""):
        """断言性能指标"""
        if metric_name not in self.metrics:
            raise AssertionError(f"未找到性能指标: {metric_name}")
        
        values = [m["value"] for m in self.metrics[metric_name]]
        avg_value = sum(values) / len(values)
        
        if comparison == "lt":  # 小于
            condition = avg_value < threshold
            comparison_text = "小于"
        elif comparison == "lte":  # 小于等于
            condition = avg_value <= threshold
            comparison_text = "小于等于"
        elif comparison == "gt":  # 大于
            condition = avg_value > threshold
            comparison_text = "大于"
        elif comparison == "gte":  # 大于等于
            condition = avg_value >= threshold
            comparison_text = "大于等于"
        else:
            raise ValueError(f"不支持的比较操作: {comparison}")
        
        if not condition:
            error_msg = f"性能指标 {metric_name} 期望{comparison_text} {threshold}, 实际值: {avg_value:.2f}"
            if message:
                error_msg = f"{message} - {error_msg}"
            raise AssertionError(error_msg)