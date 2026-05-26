#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试数据管理器

提供测试数据的创建、管理和清理功能
"""

import json
import random
from pathlib import Path
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
from faker import Faker


class TestDataManager:
    """测试数据管理器"""
    
    def __init__(self, locale: str = "zh_CN", seed: Optional[int] = None):
        self.fake = Faker(locale)
        if seed:
            self.fake.seed_instance(seed)
        
        self.data_cache = {}
        self.data_dir = Path(__file__).parent.parent / "data"
        self.data_dir.mkdir(exist_ok=True)
    
    # ========== 用户数据 ==========
    
    def create_user(self, role: str = "user", **kwargs) -> Dict[str, Any]:
        """创建用户数据"""
        user = {
            "id": self.fake.uuid4(),
            "username": kwargs.get("username", self.fake.user_name()),
            "email": kwargs.get("email", self.fake.email()),
            "phone": kwargs.get("phone", self.fake.phone_number()),
            "name": kwargs.get("name", self.fake.name()),
            "password": kwargs.get("password", self.fake.password(length=12)),
            "role": role,
            "status": kwargs.get("status", "active"),
            "avatar": kwargs.get("avatar", self.fake.image_url()),
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat(),
            "last_login_at": kwargs.get("last_login_at", None)
        }
        user.update(kwargs)
        return user
    
    def create_admin(self, **kwargs) -> Dict[str, Any]:
        """创建管理员数据"""
        return self.create_user(role="admin", **kwargs)
    
    def create_users(self, count: int = 10, role: str = "user") -> List[Dict[str, Any]]:
        """批量创建用户"""
        return [self.create_user(role=role) for _ in range(count)]
    
    # ========== 公司数据 ==========
    
    def create_company(self, **kwargs) -> Dict[str, Any]:
        """创建公司数据"""
        company = {
            "id": self.fake.uuid4(),
            "name": kwargs.get("name", self.fake.company()),
            "code": kwargs.get("code", self.fake.unique.random_number(digits=6)),
            "address": kwargs.get("address", self.fake.address()),
            "phone": kwargs.get("phone", self.fake.phone_number()),
            "email": kwargs.get("email", self.fake.company_email()),
            "website": kwargs.get("website", self.fake.url()),
            "industry": kwargs.get("industry", random.choice(["科技", "金融", "制造", "零售", "医疗", "教育"])),
            "scale": kwargs.get("scale", random.choice(["1-50人", "51-200人", "201-1000人", "1000人以上"])),
            "status": kwargs.get("status", "active"),
            "created_at": datetime.now().isoformat()
        }
        company.update(kwargs)
        return company
    
    def create_companies(self, count: int = 5) -> List[Dict[str, Any]]:
        """批量创建公司"""
        return [self.create_company() for _ in range(count)]
    
    # ========== 产品数据 ==========
    
    def create_product(self, **kwargs) -> Dict[str, Any]:
        """创建产品数据"""
        cost_price = kwargs.get("cost_price", round(random.uniform(10, 5000), 2))
        markup = kwargs.get("markup", random.uniform(1.2, 3.0))
        
        product = {
            "id": self.fake.uuid4(),
            "name": kwargs.get("name", self.fake.catch_phrase()),
            "code": kwargs.get("code", f"SKU-{self.fake.unique.random_number(digits=8)}"),
            "description": kwargs.get("description", self.fake.text(max_nb_chars=200)),
            "category": kwargs.get("category", random.choice(["电子产品", "服装", "食品", "家居", "图书", "美妆", "运动"])),
            "cost_price": cost_price,
            "sale_price": round(cost_price * markup, 2),
            "stock": kwargs.get("stock", random.randint(0, 10000)),
            "unit": kwargs.get("unit", random.choice(["件", "个", "盒", "瓶", "套"])),
            "status": kwargs.get("status", random.choice(["active", "active", "active", "inactive"])),
            "images": kwargs.get("images", [self.fake.image_url() for _ in range(random.randint(1, 5))]),
            "created_at": datetime.now().isoformat()
        }
        product.update(kwargs)
        return product
    
    def create_products(self, count: int = 20) -> List[Dict[str, Any]]:
        """批量创建产品"""
        return [self.create_product() for _ in range(count)]
    
    # ========== 订单数据 ==========
    
    def create_order(self, **kwargs) -> Dict[str, Any]:
        """创建订单数据"""
        items = kwargs.get("items", [])
        if not items:
            items = [{
                "product_id": self.fake.uuid4(),
                "product_name": self.fake.catch_phrase(),
                "quantity": random.randint(1, 10),
                "unit_price": round(random.uniform(10, 1000), 2)
            } for _ in range(random.randint(1, 5))]
        
        total_amount = sum(item["quantity"] * item["unit_price"] for item in items)
        
        order = {
            "id": self.fake.uuid4(),
            "order_no": kwargs.get("order_no", f"ORD{datetime.now().strftime('%Y%m%d')}{self.fake.unique.random_number(digits=6)}"),
            "customer_id": kwargs.get("customer_id", self.fake.uuid4()),
            "customer_name": kwargs.get("customer_name", self.fake.name()),
            "items": items,
            "total_amount": round(total_amount, 2),
            "discount": kwargs.get("discount", round(random.uniform(0, 0.3), 2)),
            "final_amount": round(total_amount * (1 - kwargs.get("discount", 0)), 2),
            "status": kwargs.get("status", random.choice(["pending", "paid", "shipped", "delivered", "cancelled"])),
            "shipping_address": kwargs.get("shipping_address", self.fake.address()),
            "remark": kwargs.get("remark", self.fake.sentence()),
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        order.update(kwargs)
        return order
    
    def create_orders(self, count: int = 50) -> List[Dict[str, Any]]:
        """批量创建订单"""
        return [self.create_order() for _ in range(count)]
    
    # ========== 财务数据 ==========
    
    def create_invoice(self, **kwargs) -> Dict[str, Any]:
        """创建发票数据"""
        amount = kwargs.get("amount", round(random.uniform(100, 100000), 2))
        
        invoice = {
            "id": self.fake.uuid4(),
            "invoice_no": kwargs.get("invoice_no", f"INV{self.fake.unique.random_number(digits=10)}"),
            "type": kwargs.get("type", random.choice(["增值税专票", "增值税普票", "电子发票"])),
            "amount": amount,
            "tax_rate": kwargs.get("tax_rate", random.choice([0.06, 0.09, 0.13])),
            "tax_amount": round(amount * kwargs.get("tax_rate", 0.13), 2),
            "total_amount": round(amount * (1 + kwargs.get("tax_rate", 0.13)), 2),
            "buyer_name": kwargs.get("buyer_name", self.fake.company()),
            "buyer_tax_no": kwargs.get("buyer_tax_no", self.fake.unique.random_number(digits=15)),
            "seller_name": kwargs.get("seller_name", self.fake.company()),
            "seller_tax_no": kwargs.get("seller_tax_no", self.fake.unique.random_number(digits=15)),
            "status": kwargs.get("status", random.choice(["draft", "issued", "void", "archived"])),
            "issue_date": kwargs.get("issue_date", self.fake.date_between(start_date="-1y", end_date="today").isoformat()),
            "created_at": datetime.now().isoformat()
        }
        invoice.update(kwargs)
        return invoice
    
    def create_invoices(self, count: int = 30) -> List[Dict[str, Any]]:
        """批量创建发票"""
        return [self.create_invoice() for _ in range(count)]
    
    # ========== 库存数据 ==========
    
    def create_inventory(self, **kwargs) -> Dict[str, Any]:
        """创建库存数据"""
        inventory = {
            "id": self.fake.uuid4(),
            "product_id": kwargs.get("product_id", self.fake.uuid4()),
            "product_name": kwargs.get("product_name", self.fake.catch_phrase()),
            "warehouse_id": kwargs.get("warehouse_id", self.fake.uuid4()),
            "warehouse_name": kwargs.get("warehouse_name", f"仓库{random.randint(1, 10)}"),
            "quantity": kwargs.get("quantity", random.randint(0, 10000)),
            "available_quantity": kwargs.get("available_quantity", random.randint(0, 8000)),
            "reserved_quantity": kwargs.get("reserved_quantity", random.randint(0, 2000)),
            "unit": kwargs.get("unit", random.choice(["件", "个", "盒", "瓶", "套"])),
            "last_updated": datetime.now().isoformat()
        }
        inventory.update(kwargs)
        return inventory
    
    def create_inventories(self, count: int = 100) -> List[Dict[str, Any]]:
        """批量创建库存"""
        return [self.create_inventory() for _ in range(count)]
    
    # ========== 数据持久化 ==========
    
    def save_data(self, name: str, data: Any):
        """保存数据到文件"""
        file_path = self.data_dir / f"{name}.json"
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"数据已保存: {file_path}")
    
    def load_data(self, name: str) -> Any:
        """从文件加载数据"""
        file_path = self.data_dir / f"{name}.json"
        if file_path.exists():
            with open(file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        return None
    
    def clear_cache(self):
        """清理缓存数据"""
        self.data_cache.clear()
        print("数据缓存已清理")
    
    def clear_files(self, pattern: str = "*.json"):
        """清理数据文件"""
        for file_path in self.data_dir.glob(pattern):
            file_path.unlink()
            print(f"已删除: {file_path}")
    
    # ========== 数据生成模板 ==========
    
    def generate_test_suite_data(self) -> Dict[str, Any]:
        """生成完整的测试数据集"""
        return {
            "users": self.create_users(20),
            "admins": self.create_users(5, role="admin"),
            "companies": self.create_companies(10),
            "products": self.create_products(50),
            "orders": self.create_orders(100),
            "invoices": self.create_invoices(50),
            "inventories": self.create_inventories(200)
        }
    
    def generate_api_test_data(self) -> Dict[str, Any]:
        """生成API测试数据"""
        return {
            "valid_user": self.create_user(),
            "invalid_user": {"username": "", "email": "invalid"},
            "valid_order": self.create_order(),
            "empty_order": {"items": []},
            "valid_product": self.create_product(),
            "out_of_stock_product": self.create_product(stock=0)
        }
    
    def generate_performance_test_data(self, scale: int = 1000) -> Dict[str, Any]:
        """生成性能测试数据"""
        return {
            "large_user_list": self.create_users(scale),
            "large_product_list": self.create_products(scale),
            "large_order_list": self.create_orders(scale)
        }


if __name__ == "__main__":
    # 示例用法
    manager = TestDataManager(seed=42)
    
    # 生成测试数据
    test_data = manager.generate_test_suite_data()
    
    # 保存到文件
    manager.save_data("test_suite", test_data)
    
    # 生成API测试数据
    api_data = manager.generate_api_test_data()
    manager.save_data("api_test", api_data)
    
    print("测试数据生成完成！")
    print(f"用户数量: {len(test_data['users'])}")
    print(f"产品数量: {len(test_data['products'])}")
    print(f"订单数量: {len(test_data['orders'])}")
