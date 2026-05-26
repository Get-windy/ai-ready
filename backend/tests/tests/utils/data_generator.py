#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试数据生成器

提供各种测试数据的生成功能
"""

from faker import Faker
from typing import Dict, Any, List, Optional
import random
import string


class DataGenerator:
    """测试数据生成器"""
    
    def __init__(self, locale: str = "zh_CN"):
        self.fake = Faker(locale)
        self.fake.seed_instance(42)  # 设置随机种子，保证可重复性
    
    def generate_user(self, **overrides) -> Dict[str, Any]:
        """生成用户数据"""
        user = {
            "username": self.fake.user_name(),
            "email": self.fake.email(),
            "phone": self.fake.phone_number(),
            "name": self.fake.name(),
            "password": self.fake.password(length=12),
            "avatar": self.fake.image_url(),
            "status": random.choice(["active", "inactive", "pending"]),
            "created_at": self.fake.iso8601(),
            "updated_at": self.fake.iso8601()
        }
        user.update(overrides)
        return user
    
    def generate_company(self, **overrides) -> Dict[str, Any]:
        """生成公司数据"""
        company = {
            "name": self.fake.company(),
            "address": self.fake.address(),
            "phone": self.fake.phone_number(),
            "email": self.fake.company_email(),
            "website": self.fake.url(),
            "industry": random.choice(["科技", "金融", "制造", "零售", "医疗"]),
            "size": random.choice(["1-50", "51-200", "201-1000", "1000+"]),
            "status": "active"
        }
        company.update(overrides)
        return company
    
    def generate_order(self, **overrides) -> Dict[str, Any]:
        """生成订单数据"""
        order = {
            "order_no": f"ORD{self.fake.unique.random_number(digits=10)}",
            "customer_id": random.randint(1, 1000),
            "product_id": random.randint(1, 500),
            "quantity": random.randint(1, 100),
            "price": round(random.uniform(10, 10000), 2),
            "status": random.choice(["pending", "paid", "shipped", "delivered", "cancelled"]),
            "created_at": self.fake.iso8601(),
            "shipping_address": self.fake.address()
        }
        order["total_amount"] = order["quantity"] * order["price"]
        order.update(overrides)
        return order
    
    def generate_product(self, **overrides) -> Dict[str, Any]:
        """生成产品数据"""
        product = {
            "name": self.fake.catch_phrase(),
            "description": self.fake.text(max_nb_chars=200),
            "sku": f"SKU-{self.fake.unique.random_number(digits=8)}",
            "price": round(random.uniform(10, 10000), 2),
            "category": random.choice(["电子产品", "服装", "食品", "家居", "图书"]),
            "stock": random.randint(0, 10000),
            "status": random.choice(["active", "inactive", "out_of_stock"])
        }
        product.update(overrides)
        return product
    
    def generate_api_response(self, success: bool = True, **overrides) -> Dict[str, Any]:
        """生成API响应数据"""
        if success:
            response = {
                "code": 200,
                "message": "success",
                "data": overrides.get("data", {}),
                "timestamp": self.fake.iso8601()
            }
        else:
            response = {
                "code": overrides.get("code", 500),
                "message": overrides.get("message", "Internal Server Error"),
                "data": None,
                "timestamp": self.fake.iso8601()
            }
        response.update(overrides)
        return response
    
    def generate_pagination_data(self, items: List[Dict], page: int = 1, page_size: int = 10) -> Dict[str, Any]:
        """生成分页数据"""
        total = len(items)
        start = (page - 1) * page_size
        end = start + page_size
        
        return {
            "items": items[start:end],
            "total": total,
            "page": page,
            "page_size": page_size,
            "total_pages": (total + page_size - 1) // page_size
        }
    
    def generate_random_string(self, length: int = 10) -> str:
        """生成随机字符串"""
        return ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    def generate_random_number(self, min_value: int = 1, max_value: int = 100) -> int:
        """生成随机数字"""
        return random.randint(min_value, max_value)
    
    def generate_random_float(self, min_value: float = 0.0, max_value: float = 100.0) -> float:
        """生成随机浮点数"""
        return round(random.uniform(min_value, max_value), 2)
    
    def generate_batch_users(self, count: int = 10) -> List[Dict[str, Any]]:
        """批量生成用户数据"""
        return [self.generate_user() for _ in range(count)]
    
    def generate_batch_orders(self, count: int = 10) -> List[Dict[str, Any]]:
        """批量生成订单数据"""
        return [self.generate_order() for _ in range(count)]
    
    def generate_batch_products(self, count: int = 10) -> List[Dict[str, Any]]:
        """批量生成产品数据"""
        return [self.generate_product() for _ in range(count)]
