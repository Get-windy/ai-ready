#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 测试数据生成器
支持生成用户、订单、客户等测试数据
"""

import random
import string
import json
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
import hashlib


@dataclass
class DataGeneratorConfig:
    """数据生成器配置"""
    default_tenant_id: int = 1
    default_status: int = 1
    password_length: int = 12
    phone_prefix: str = "138"


class DataGenerator:
    """测试数据生成器"""
    
    def __init__(self, config: DataGeneratorConfig = None):
        self.config = config or DataGeneratorConfig()
    
    # ==================== 基础数据生成 ====================
    
    @staticmethod
    def generate_random_string(length: int = 8) -> str:
        """生成随机字符串"""
        return ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    @staticmethod
    def generate_random_password(length: int = 12) -> str:
        """生成随机密码"""
        chars = string.ascii_letters + string.digits + "!@#$%^&*"
        return ''.join(random.choices(chars, k=length))
    
    @staticmethod
    def generate_random_phone() -> str:
        """生成随机手机号"""
        prefixes = ["138", "139", "186", "187", "150", "151", "177", "188"]
        return random.choice(prefixes) + ''.join(random.choices(string.digits, k=8))
    
    @staticmethod
    def generate_random_email(domain: str = "test.com") -> str:
        """生成随机邮箱"""
        username = ''.join(random.choices(string.ascii_lowercase, k=8))
        return f"{username}@{domain}"
    
    @staticmethod
    def generate_random_date(start_days: int = -30, end_days: int = 0) -> str:
        """生成随机日期"""
        start = datetime.now() + timedelta(days=start_days)
        end = datetime.now() + timedelta(days=end_days)
        delta = end - start
        random_days = random.randint(0, delta.days)
        return (start + timedelta(days=random_days)).strftime("%Y-%m-%d")
    
    @staticmethod
    def generate_random_datetime(start_days: int = -30, end_days: int = 0) -> str:
        """生成随机日期时间"""
        start = datetime.now() + timedelta(days=start_days)
        end = datetime.now() + timedelta(days=end_days)
        delta = end - start
        random_seconds = random.randint(0, int(delta.total_seconds()))
        return (start + timedelta(seconds=random_seconds)).strftime("%Y-%m-%d %H:%M:%S")
    
    # ==================== 用户数据 ====================
    
    def generate_user(self, index: int = 0) -> Dict[str, Any]:
        """生成单个用户数据"""
        return {
            "username": f"test_user_{index}_{self.generate_random_string(4)}",
            "password": self.generate_random_password(self.config.password_length),
            "email": self.generate_random_email(),
            "phone": self.generate_random_phone(),
            "tenantId": self.config.default_tenant_id,
            "status": self.config.default_status,
            "createTime": self.generate_random_datetime(),
            "roleId": random.randint(1, 5)
        }
    
    def generate_users(self, count: int) -> List[Dict[str, Any]]:
        """生成批量用户数据"""
        return [self.generate_user(i) for i in range(count)]
    
    # ==================== 角色数据 ====================
    
    def generate_role(self, index: int = 0) -> Dict[str, Any]:
        """生成单个角色数据"""
        role_names = ["管理员", "普通用户", "访客", "审核员", "操作员"]
        return {
            "roleName": f"{role_names[index % len(role_names)]}_{index}",
            "roleCode": f"ROLE_{index}",
            "description": f"测试角色{index}",
            "tenantId": self.config.default_tenant_id,
            "status": self.config.default_status,
            "createTime": self.generate_random_datetime()
        }
    
    def generate_roles(self, count: int) -> List[Dict[str, Any]]:
        """生成批量角色数据"""
        return [self.generate_role(i) for i in range(count)]
    
    # ==================== 客户数据 ====================
    
    def generate_customer(self, index: int = 0) -> Dict[str, Any]:
        """生成单个客户数据"""
        customer_types = ["企业", "个人", "政府", "学校"]
        sources = ["网站", "电话", "转介绍", "展会"]
        return {
            "customerName": f"测试客户{index}",
            "customerType": random.choice(customer_types),
            "source": random.choice(sources),
            "contactPerson": f"联系人{index}",
            "phone": self.generate_random_phone(),
            "email": self.generate_random_email(),
            "address": f"测试地址{index}号",
            "tenantId": self.config.default_tenant_id,
            "status": self.config.default_status,
            "createTime": self.generate_random_datetime(-365, -30)
        }
    
    def generate_customers(self, count: int) -> List[Dict[str, Any]]:
        """生成批量客户数据"""
        return [self.generate_customer(i) for i in range(count)]
    
    # ==================== 订单数据 ====================
    
    def generate_order(self, index: int = 0, customer_id: int = None) -> Dict[str, Any]:
        """生成单个订单数据"""
        statuses = ["pending", "paid", "shipped", "completed", "cancelled"]
        return {
            "orderNo": f"ORD{datetime.now().strftime('%Y%m%d')}{index:06d}",
            "customerId": customer_id or random.randint(1, 100),
            "totalAmount": round(random.uniform(100, 50000), 2),
            "paidAmount": round(random.uniform(0, 50000), 2),
            "status": random.choice(statuses),
            "paymentMethod": random.choice(["alipay", "wechat", "bank", "cash"]),
            "remark": f"测试订单备注{index}",
            "tenantId": self.config.default_tenant_id,
            "createTime": self.generate_random_datetime(-30, 0)
        }
    
    def generate_orders(self, count: int, customer_ids: List[int] = None) -> List[Dict[str, Any]]:
        """生成批量订单数据"""
        orders = []
        for i in range(count):
            customer_id = random.choice(customer_ids) if customer_ids else None
            orders.append(self.generate_order(i, customer_id))
        return orders
    
    # ==================== 产品数据 ====================
    
    def generate_product(self, index: int = 0) -> Dict[str, Any]:
        """生成单个产品数据"""
        categories = ["电子产品", "服装", "食品", "家居", "办公用品"]
        return {
            "productCode": f"PRD{index:06d}",
            "productName": f"测试产品{index}",
            "category": random.choice(categories),
            "price": round(random.uniform(10, 10000), 2),
            "stock": random.randint(0, 1000),
            "description": f"测试产品描述{index}",
            "tenantId": self.config.default_tenant_id,
            "status": self.config.default_status,
            "createTime": self.generate_random_datetime(-180, 0)
        }
    
    def generate_products(self, count: int) -> List[Dict[str, Any]]:
        """生成批量产品数据"""
        return [self.generate_product(i) for i in range(count)]
    
    # ==================== 日志数据 ====================
    
    def generate_login_log(self, index: int = 0, user_id: int = None) -> Dict[str, Any]:
        """生成登录日志"""
        return {
            "userId": user_id or random.randint(1, 100),
            "username": f"test_user_{random.randint(1, 100)}",
            "ip": f"{random.randint(1,255)}.{random.randint(1,255)}.{random.randint(1,255)}.{random.randint(1,255)}",
            "location": random.choice(["北京", "上海", "广州", "深圳", "杭州"]),
            "browser": random.choice(["Chrome", "Firefox", "Edge", "Safari"]),
            "os": random.choice(["Windows", "MacOS", "Linux", "Android", "iOS"]),
            "status": random.choice([0, 1]),
            "msg": random.choice(["登录成功", "登录失败", "密码错误"]),
            "loginTime": self.generate_random_datetime(-7, 0)
        }
    
    def generate_login_logs(self, count: int, user_ids: List[int] = None) -> List[Dict[str, Any]]:
        """生成批量登录日志"""
        logs = []
        for i in range(count):
            user_id = random.choice(user_ids) if user_ids else None
            logs.append(self.generate_login_log(i, user_id))
        return logs
    
    # ==================== 导出功能 ====================
    
    def export_to_json(self, data: List[Dict], filename: str):
        """导出为JSON文件"""
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"数据已导出: {filename} ({len(data)}条)")
    
    def export_to_sql(self, data: List[Dict], table_name: str, filename: str):
        """导出为SQL INSERT语句"""
        with open(filename, 'w', encoding='utf-8') as f:
            for row in data:
                columns = ', '.join(row.keys())
                values = ', '.join([f"'{v}'" if isinstance(v, str) else str(v) for v in row.values()])
                f.write(f"INSERT INTO {table_name} ({columns}) VALUES ({values});\n")
        print(f"SQL已导出: {filename} ({len(data)}条)")


def main():
    """主函数"""
    print("=" * 60)
    print("AI-Ready 测试数据生成器")
    print("=" * 60)
    
    generator = DataGenerator()
    
    # 生成各类测试数据
    print("\n生成用户数据...")
    users = generator.generate_users(100)
    print(f"  生成 {len(users)} 条用户数据")
    
    print("\n生成角色数据...")
    roles = generator.generate_roles(10)
    print(f"  生成 {len(roles)} 条角色数据")
    
    print("\n生成客户数据...")
    customers = generator.generate_customers(50)
    print(f"  生成 {len(customers)} 条客户数据")
    
    print("\n生成订单数据...")
    orders = generator.generate_orders(200, [c.get('id', i) for i, c in enumerate(customers, 1)])
    print(f"  生成 {len(orders)} 条订单数据")
    
    print("\n生成产品数据...")
    products = generator.generate_products(30)
    print(f"  生成 {len(products)} 条产品数据")
    
    print("\n生成登录日志...")
    logs = generator.generate_login_logs(500)
    print(f"  生成 {len(logs)} 条登录日志")
    
    # 导出数据
    import os
    output_dir = "tests/performance/test_data"
    os.makedirs(output_dir, exist_ok=True)
    
    print("\n导出数据...")
    generator.export_to_json(users, f"{output_dir}/users.json")
    generator.export_to_json(orders, f"{output_dir}/orders.json")
    generator.export_to_json(products, f"{output_dir}/products.json")
    
    print("\n" + "=" * 60)
    print("测试数据生成完成")
    print("=" * 60)


if __name__ == "__main__":
    main()
