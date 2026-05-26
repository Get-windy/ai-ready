"""
测试数据管理模块
提供测试数据的生成、管理和清理功能
"""

import json
import csv
import random
import string
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
from pathlib import Path

class TestDataManager:
    """测试数据管理类"""
    
    def __init__(self, data_dir: str = "./test_data"):
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.logger = logging.getLogger(__name__)
        
        # 预定义的数据生成器
        self.generators = {
            'string': self.generate_string,
            'number': self.generate_number,
            'email': self.generate_email,
            'phone': self.generate_phone,
            'date': self.generate_date,
            'boolean': self.generate_boolean,
            'choice': self.generate_choice
        }
    
    def generate_string(self, length: int = 10, prefix: str = "", suffix: str = "") -> str:
        """生成随机字符串"""
        chars = string.ascii_letters + string.digits
        random_str = ''.join(random.choice(chars) for _ in range(length))
        return f"{prefix}{random_str}{suffix}"
    
    def generate_number(self, min_value: int = 1, max_value: int = 100) -> int:
        """生成随机数字"""
        return random.randint(min_value, max_value)
    
    def generate_email(self, domain: str = "example.com") -> str:
        """生成随机邮箱地址"""
        username = self.generate_string(8)
        return f"{username}@{domain}"
    
    def generate_phone(self) -> str:
        """生成随机手机号"""
        prefix = random.choice(['130', '131', '132', '133', '134', '135', '136', '137', '138', '139',
                               '150', '151', '152', '153', '155', '156', '157', '158', '159',
                               '180', '181', '182', '183', '184', '185', '186', '187', '188', '189'])
        suffix = ''.join(random.choice(string.digits) for _ in range(8))
        return f"{prefix}{suffix}"
    
    def generate_date(self, start_date: str = "2024-01-01", end_date: str = "2024-12-31") -> str:
        """生成随机日期"""
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        delta = end - start
        random_days = random.randint(0, delta.days)
        random_date = start + timedelta(days=random_days)
        
        return random_date.strftime("%Y-%m-%d")
    
    def generate_boolean(self) -> bool:
        """生成随机布尔值"""
        return random.choice([True, False])
    
    def generate_choice(self, choices: List[Any]) -> Any:
        """从列表中随机选择"""
        return random.choice(choices)
    
    def generate_data(self, schema: Dict[str, Any], count: int = 1) -> List[Dict[str, Any]]:
        """根据schema生成测试数据"""
        data_list = []
        
        for i in range(count):
            data = {}
            for field, field_schema in schema.items():
                if isinstance(field_schema, dict):
                    # 复杂字段类型
                    field_type = field_schema.get('type', 'string')
                    generator = self.generators.get(field_type)
                    
                    if generator:
                        # 调用对应的生成器
                        if field_type == 'string':
                            data[field] = generator(
                                length=field_schema.get('length', 10),
                                prefix=field_schema.get('prefix', ''),
                                suffix=field_schema.get('suffix', '')
                            )
                        elif field_type == 'number':
                            data[field] = generator(
                                min_value=field_schema.get('min', 1),
                                max_value=field_schema.get('max', 100)
                            )
                        elif field_type == 'email':
                            data[field] = generator(domain=field_schema.get('domain', 'example.com'))
                        elif field_type == 'date':
                            data[field] = generator(
                                start_date=field_schema.get('start_date', '2024-01-01'),
                                end_date=field_schema.get('end_date', '2024-12-31')
                            )
                        elif field_type == 'choice':
                            data[field] = generator(choices=field_schema.get('choices', []))
                        else:
                            data[field] = generator()
                    else:
                        # 默认生成字符串
                        data[field] = self.generate_string()
                else:
                    # 简单字段类型，直接使用值
                    data[field] = field_schema
            
            data_list.append(data)
        
        self.logger.info(f"生成了 {count} 条测试数据")
        return data_list
    
    def save_data(self, data: List[Dict[str, Any]], filename: str, format: str = "json"):
        """保存测试数据到文件"""
        filepath = self.data_dir / filename
        
        try:
            if format.lower() == "json":
                with open(filepath.with_suffix('.json'), 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
            elif format.lower() == "csv":
                if data:
                    fieldnames = data[0].keys()
                    with open(filepath.with_suffix('.csv'), 'w', newline='', encoding='utf-8') as f:
                        writer = csv.DictWriter(f, fieldnames=fieldnames)
                        writer.writeheader()
                        writer.writerows(data)
            else:
                raise ValueError(f"不支持的格式: {format}")
            
            self.logger.info(f"测试数据已保存到: {filepath}")
            return str(filepath)
            
        except Exception as e:
            self.logger.error(f"保存测试数据失败: {e}")
            raise
    
    def load_data(self, filename: str, format: str = "json") -> List[Dict[str, Any]]:
        """从文件加载测试数据"""
        filepath = self.data_dir / filename
        
        try:
            if format.lower() == "json":
                with open(filepath.with_suffix('.json'), 'r', encoding='utf-8') as f:
                    data = json.load(f)
            elif format.lower() == "csv":
                with open(filepath.with_suffix('.csv'), 'r', encoding='utf-8') as f:
                    reader = csv.DictReader(f)
                    data = list(reader)
            else:
                raise ValueError(f"不支持的格式: {format}")
            
            self.logger.info(f"从 {filepath} 加载了 {len(data)} 条测试数据")
            return data
            
        except Exception as e:
            self.logger.error(f"加载测试数据失败: {e}")
            raise
    
    def cleanup_data(self, pattern: str = "*"):
        """清理测试数据文件"""
        try:
            files = list(self.data_dir.glob(pattern))
            for file in files:
                if file.is_file():
                    file.unlink()
                    self.logger.debug(f"已删除文件: {file}")
            
            self.logger.info(f"清理了 {len(files)} 个测试数据文件")
            
        except Exception as e:
            self.logger.error(f"清理测试数据失败: {e}")
            raise

# 预定义的测试数据schema
class TestDataSchemas:
    """测试数据schema定义"""
    
    @staticmethod
    def user_schema() -> Dict[str, Any]:
        """用户数据schema"""
        return {
            'username': {
                'type': 'string',
                'length': 8,
                'prefix': 'user_'
            },
            'email': {
                'type': 'email',
                'domain': 'test.com'
            },
            'phone': {
                'type': 'string',
                'length': 11
            },
            'age': {
                'type': 'number',
                'min': 18,
                'max': 60
            },
            'active': {
                'type': 'boolean'
            },
            'created_at': {
                'type': 'date',
                'start_date': '2024-01-01',
                'end_date': '2024-12-31'
            }
        }
    
    @staticmethod
    def order_schema() -> Dict[str, Any]:
        """订单数据schema"""
        return {
            'order_id': {
                'type': 'string',
                'length': 10,
                'prefix': 'ORD_'
            },
            'customer_id': {
                'type': 'string',
                'length': 8,
                'prefix': 'CUST_'
            },
            'product_id': {
                'type': 'string',
                'length': 6,
                'prefix': 'PROD_'
            },
            'quantity': {
                'type': 'number',
                'min': 1,
                'max': 10
            },
            'price': {
                'type': 'number',
                'min': 10,
                'max': 1000
            },
            'status': {
                'type': 'choice',
                'choices': ['pending', 'processing', 'shipped', 'delivered', 'cancelled']
            },
            'order_date': {
                'type': 'date',
                'start_date': '2024-01-01',
                'end_date': '2024-12-31'
            }
        }
    
    @staticmethod
    def inventory_schema() -> Dict[str, Any]:
        """库存数据schema"""
        return {
            'product_id': {
                'type': 'string',
                'length': 6,
                'prefix': 'PROD_'
            },
            'product_name': {
                'type': 'string',
                'length': 15,
                'prefix': 'Product '
            },
            'category': {
                'type': 'choice',
                'choices': ['Electronics', 'Clothing', 'Books', 'Home', 'Sports']
            },
            'quantity': {
                'type': 'number',
                'min': 0,
                'max': 1000
            },
            'price': {
                'type': 'number',
                'min': 1,
                'max': 500
            },
            'in_stock': {
                'type': 'boolean'
            },
            'last_updated': {
                'type': 'date',
                'start_date': '2024-01-01',
                'end_date': '2024-12-31'
            }
        }