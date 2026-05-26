"""
测试工具函数
提供测试过程中常用的工具函数
"""

import os
import json
import time
import hashlib
import random
import string
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional
from faker import Faker


class TestUtils:
    """测试工具类"""
    
    def __init__(self):
        self.faker = Faker('zh_CN')  # 使用中文数据
    
    def generate_random_string(self, length: int = 10) -> str:
        """生成随机字符串"""
        return ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    def generate_random_email(self) -> str:
        """生成随机邮箱"""
        return self.faker.email()
    
    def generate_random_phone(self) -> str:
        """生成随机手机号"""
        return self.faker.phone_number()
    
    def generate_random_name(self) -> str:
        """生成随机姓名"""
        return self.faker.name()
    
    def generate_random_address(self) -> str:
        """生成随机地址"""
        return self.faker.address()
    
    def generate_random_company(self) -> str:
        """生成随机公司名称"""
        return self.faker.company()
    
    def generate_test_user(self) -> Dict[str, Any]:
        """生成测试用户数据"""
        return {
            'username': self.generate_random_string(8),
            'email': self.generate_random_email(),
            'phone': self.generate_random_phone(),
            'password': self.generate_random_string(12),
            'name': self.generate_random_name(),
            'created_at': datetime.now().isoformat()
        }
    
    def generate_test_order(self) -> Dict[str, Any]:
        """生成测试订单数据"""
        return {
            'order_id': f"ORD{self.generate_random_string(8).upper()}",
            'customer_name': self.generate_random_name(),
            'customer_phone': self.generate_random_phone(),
            'address': self.generate_random_address(),
            'amount': round(random.uniform(100, 10000), 2),
            'status': random.choice(['pending', 'processing', 'shipped', 'delivered']),
            'created_at': datetime.now().isoformat()
        }
    
    def generate_test_product(self) -> Dict[str, Any]:
        """生成测试产品数据"""
        return {
            'product_id': f"PRD{self.generate_random_string(6).upper()}",
            'name': self.faker.word(),
            'description': self.faker.text(max_nb_chars=200),
            'price': round(random.uniform(10, 1000), 2),
            'stock': random.randint(0, 1000),
            'category': random.choice(['electronics', 'clothing', 'food', 'books', 'home'])
        }
    
    def calculate_md5(self, data: str) -> str:
        """计算MD5哈希"""
        return hashlib.md5(data.encode('utf-8')).hexdigest()
    
    def calculate_sha256(self, data: str) -> str:
        """计算SHA256哈希"""
        return hashlib.sha256(data.encode('utf-8')).hexdigest()
    
    def timestamp_to_datetime(self, timestamp: int) -> datetime:
        """时间戳转日期时间"""
        return datetime.fromtimestamp(timestamp)
    
    def datetime_to_timestamp(self, dt: datetime) -> int:
        """日期时间转时间戳"""
        return int(dt.timestamp())
    
    def format_datetime(self, dt: datetime, format_str: str = '%Y-%m-%d %H:%M:%S') -> str:
        """格式化日期时间"""
        return dt.strftime(format_str)
    
    def parse_datetime(self, date_str: str, format_str: str = '%Y-%m-%d %H:%M:%S') -> datetime:
        """解析日期时间字符串"""
        return datetime.strptime(date_str, format_str)
    
    def wait_seconds(self, seconds: float):
        """等待指定秒数"""
        time.sleep(seconds)
    
    def retry_operation(self, operation, max_retries: int = 3, delay: float = 1.0):
        """重试操作"""
        for attempt in range(max_retries):
            try:
                return operation()
            except Exception as e:
                if attempt == max_retries - 1:
                    raise e
                time.sleep(delay)
        return None
    
    def save_json_file(self, data: Any, file_path: str):
        """保存JSON文件"""
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
    
    def load_json_file(self, file_path: str) -> Any:
        """加载JSON文件"""
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    
    def generate_unique_id(self, prefix: str = "") -> str:
        """生成唯一ID"""
        timestamp = int(time.time() * 1000)
        random_suffix = self.generate_random_string(6)
        return f"{prefix}{timestamp}{random_suffix}"
    
    def mask_sensitive_data(self, data: str, visible_chars: int = 4) -> str:
        """脱敏敏感数据"""
        if len(data) <= visible_chars * 2:
            return '*' * len(data)
        return data[:visible_chars] + '*' * (len(data) - visible_chars * 2) + data[-visible_chars:]


if __name__ == "__main__":
    # 测试工具函数
    utils = TestUtils()
    
    print("测试工具函数示例:")
    print(f"随机字符串: {utils.generate_random_string(10)}")
    print(f"随机邮箱: {utils.generate_random_email()}")
    print(f"随机手机号: {utils.generate_random_phone()}")
    print(f"随机姓名: {utils.generate_random_name()}")
    
    print("\n测试用户数据:")
    user = utils.generate_test_user()
    print(json.dumps(user, indent=2, ensure_ascii=False))
    
    print("\n测试订单数据:")
    order = utils.generate_test_order()
    print(json.dumps(order, indent=2, ensure_ascii=False))