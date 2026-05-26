"""
API测试工具类
提供通用的测试工具函数和类
"""

import json
import time
import logging
import hashlib
import random
import string
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional, Tuple
import requests
from requests.exceptions import RequestException, Timeout, ConnectionError

from config import config


class TestLogger:
    """测试日志记录器"""
    
    def __init__(self, name="API Tests"):
        self.logger = logging.getLogger(name)
        self.setup_logging()
    
    def setup_logging(self):
        """设置日志配置"""
        if not self.logger.handlers:
            handler = logging.FileHandler(config.logging_config["file"])
            formatter = logging.Formatter(config.logging_config["format"])
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)
            self.logger.setLevel(getattr(logging, config.logging_config["level"]))
    
    def log_test_start(self, test_name: str):
        """记录测试开始"""
        self.logger.info(f"🔵 开始测试: {test_name}")
        print(f"🔵 开始测试: {test_name}")
    
    def log_test_pass(self, test_name: str, details: str = ""):
        """记录测试通过"""
        self.logger.info(f"✅ 测试通过: {test_name} {details}")
        print(f"✅ 测试通过: {test_name}")
    
    def log_test_fail(self, test_name: str, error: str, details: str = ""):
        """记录测试失败"""
        self.logger.error(f"❌ 测试失败: {test_name} - {error} {details}")
        print(f"❌ 测试失败: {test_name} - {error}")
    
    def log_test_warning(self, test_name: str, warning: str):
        """记录测试警告"""
        self.logger.warning(f"⚠️ 测试警告: {test_name} - {warning}")
        print(f"⚠️ 测试警告: {test_name} - {warning}")
    
    def log_performance(self, test_name: str, response_time: float, threshold: float):
        """记录性能测试结果"""
        status = "✅" if response_time <= threshold else "❌"
        self.logger.info(f"{status} 性能测试: {test_name} - 响应时间: {response_time:.2f}ms (阈值: {threshold}ms)")
        print(f"{status} 性能测试: {test_name} - 响应时间: {response_time:.2f}ms")


class APIClient:
    """API客户端类"""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Sprint27+1-API-Test-Framework/1.0",
            "Content-Type": "application/json",
            "Accept": "application/json",
        })
        self.auth_token = None
        self.logger = TestLogger("APIClient")
    
    def authenticate(self, username: str = None, password: str = None) -> bool:
        """进行身份认证"""
        if username is None:
            username = config.auth_config["username"]
        if password is None:
            password = config.auth_config["password"]
        
        try:
            auth_endpoint = config.get_endpoint("auth")
            payload = {
                "username": username,
                "password": password
            }
            
            response = self.post(auth_endpoint, payload)
            
            if response.status_code == 200:
                data = response.json()
                if "token" in data:
                    self.auth_token = data["token"]
                    self.session.headers.update({
                        "Authorization": f"Bearer {self.auth_token}"
                    })
                    self.logger.log_test_pass("身份认证", f"用户: {username}")
                    return True
            
            self.logger.log_test_fail("身份认证", f"状态码: {response.status_code}")
            return False
            
        except Exception as e:
            self.logger.log_test_fail("身份认证", str(e))
            return False
    
    def request(self, method: str, url: str, **kwargs) -> requests.Response:
        """发送HTTP请求（带重试机制）"""
        retry_attempts = config.test_execution["retry_attempts"]
        retry_delay = config.test_execution["retry_delay_seconds"]
        
        for attempt in range(retry_attempts):
            try:
                response = self.session.request(
                    method, 
                    url, 
                    timeout=config.test_execution["timeout_seconds"],
                    verify=config.test_execution["verify_ssl"],
                    **kwargs
                )
                return response
                
            except Timeout:
                if attempt < retry_attempts - 1:
                    self.logger.log_test_warning("请求超时", f"第{attempt+1}次重试, URL: {url}")
                    time.sleep(retry_delay)
                else:
                    raise Timeout(f"请求超时，已达到最大重试次数: {url}")
            except ConnectionError:
                if attempt < retry_attempts - 1:
                    self.logger.log_test_warning("连接错误", f"第{attempt+1}次重试, URL: {url}")
                    time.sleep(retry_delay)
                else:
                    raise ConnectionError(f"连接错误，已达到最大重试次数: {url}")
            except RequestException as e:
                raise RequestException(f"请求异常: {str(e)}, URL: {url}")
        
        raise RequestException(f"请求失败，已达到最大重试次数: {url}")
    
    def get(self, url: str, **kwargs) -> requests.Response:
        """发送GET请求"""
        return self.request("GET", url, **kwargs)
    
    def post(self, url: str, data: Dict = None, **kwargs) -> requests.Response:
        """发送POST请求"""
        if data is not None:
            kwargs["json"] = data
        return self.request("POST", url, **kwargs)
    
    def put(self, url: str, data: Dict = None, **kwargs) -> requests.Response:
        """发送PUT请求"""
        if data is not None:
            kwargs["json"] = data
        return self.request("PUT", url, **kwargs)
    
    def delete(self, url: str, **kwargs) -> requests.Response:
        """发送DELETE请求"""
        return self.request("DELETE", url, **kwargs)
    
    def patch(self, url: str, data: Dict = None, **kwargs) -> requests.Response:
        """发送PATCH请求"""
        if data is not None:
            kwargs["json"] = data
        return self.request("PATCH", url, **kwargs)


class TestDataGenerator:
    """测试数据生成器"""
    
    @staticmethod
    def random_string(length: int = 10) -> str:
        """生成随机字符串"""
        letters = string.ascii_letters + string.digits
        return ''.join(random.choice(letters) for _ in range(length))
    
    @staticmethod
    def random_email() -> str:
        """生成随机邮箱"""
        domain = random.choice(["example.com", "test.com", "demo.com"])
        username = TestDataGenerator.random_string(8)
        return f"{username}@{domain}"
    
    @staticmethod
    def random_number(min_val: int = 1, max_val: int = 1000) -> int:
        """生成随机数字"""
        return random.randint(min_val, max_val)
    
    @staticmethod
    def random_float(min_val: float = 1.0, max_val: float = 1000.0) -> float:
        """生成随机浮点数"""
        return round(random.uniform(min_val, max_val), 2)
    
    @staticmethod
    def random_date(start_date: str = "2024-01-01", end_date: str = "2024-12-31") -> str:
        """生成随机日期"""
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        random_date = start + timedelta(days=random.randint(0, (end - start).days))
        return random_date.strftime("%Y-%m-%d")
    
    @staticmethod
    def generate_user_data() -> Dict[str, Any]:
        """生成用户测试数据"""
        return {
            "username": f"user_{TestDataGenerator.random_string(8)}",
            "email": TestDataGenerator.random_email(),
            "password": "TestPassword123!",
            "first_name": random.choice(["John", "Jane", "Mike", "Sarah", "David"]),
            "last_name": random.choice(["Smith", "Johnson", "Brown", "Davis", "Wilson"]),
            "age": TestDataGenerator.random_number(18, 65),
            "active": random.choice([True, False])
        }
    
    @staticmethod
    def generate_product_data() -> Dict[str, Any]:
        """生成产品测试数据"""
        categories = ["electronics", "clothing", "books", "home", "sports"]
        return {
            "name": f"Product {TestDataGenerator.random_string(6)}",
            "description": f"Description for product {TestDataGenerator.random_string(10)}",
            "price": TestDataGenerator.random_float(10.0, 1000.0),
            "stock_quantity": TestDataGenerator.random_number(0, 1000),
            "category": random.choice(categories),
            "sku": f"SKU-{TestDataGenerator.random_string(8).upper()}"
        }
    
    @staticmethod
    def generate_order_data(user_id: int = None) -> Dict[str, Any]:
        """生成订单测试数据"""
        if user_id is None:
            user_id = TestDataGenerator.random_number(1, 100)
        
        items = []
        total_amount = 0
        for _ in range(random.randint(1, 5)):
            item = {
                "product_id": TestDataGenerator.random_number(1, 100),
                "quantity": TestDataGenerator.random_number(1, 10),
                "price": TestDataGenerator.random_float(5.0, 500.0)
            }
            items.append(item)
            total_amount += item["quantity"] * item["price"]
        
        return {
            "user_id": user_id,
            "items": items,
            "total_amount": round(total_amount, 2),
            "order_date": TestDataGenerator.random_date(),
            "status": random.choice(["pending", "processing", "shipped", "delivered", "cancelled"])
        }


class TestValidator:
    """测试验证器"""
    
    @staticmethod
    def validate_response_status(response: requests.Response, expected_status: int) -> bool:
        """验证响应状态码"""
        return response.status_code == expected_status
    
    @staticmethod
    def validate_response_time(response: requests.Response, max_time_ms: float) -> bool:
        """验证响应时间"""
        response_time_ms = response.elapsed.total_seconds() * 1000
        return response_time_ms <= max_time_ms
    
    @staticmethod
    def validate_json_schema(response: requests.Response, schema: Dict) -> bool:
        """验证JSON响应结构"""
        try:
            data = response.json()
            # 简化的schema验证
            for key, expected_type in schema.items():
                if key not in data:
                    return False
                if not isinstance(data[key], expected_type):
                    return False
            return True
        except:
            return False
    
    @staticmethod
    def validate_error_response(response: requests.Response, expected_error_code: str = None) -> bool:
        """验证错误响应"""
        if response.status_code >= 400:
            try:
                error_data = response.json()
                if expected_error_code and "code" in error_data:
                    return error_data["code"] == expected_error_code
                return True
            except:
                return response.status_code >= 400
        return False
    
    @staticmethod
    def validate_pagination(response: requests.Response) -> bool:
        """验证分页响应"""
        try:
            data = response.json()
            required_keys = ["data", "total", "page", "per_page", "total_pages"]
            return all(key in data for key in required_keys)
        except:
            return False


# 全局工具实例
logger = TestLogger()
api_client = APIClient()
data_generator = TestDataGenerator()
validator = TestValidator()


if __name__ == "__main__":
    # 测试工具功能
    print("测试工具类功能验证:")
    print(f"随机字符串: {data_generator.random_string()}")
    print(f"随机邮箱: {data_generator.random_email()}")
    print(f"用户数据示例: {data_generator.generate_user_data()}")
    print(f"产品数据示例: {data_generator.generate_product_data()}")
    logger.log_test_start("工具类测试")
    logger.log_test_pass("工具类测试", "所有工具函数正常工作")