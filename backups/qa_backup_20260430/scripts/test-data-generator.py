#!/usr/bin/env python3
"""
测试数据准备脚本
用途: 为测试环境生成测试数据
版本: 1.0
"""

import os
import sys
import json
import random
import argparse
from datetime import datetime, timedelta
from typing import Dict, List, Any


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='测试数据生成工具')
    parser.add_argument('--output-dir', default='test_data',
                       help='输出目录')
    parser.add_argument('--user-count', type=int, default=1000,
                       help='用户数据数量')
    parser.add_argument('--order-count', type=int, default=5000,
                       help='订单数据数量')
    parser.add_argument('--product-count', type=int, default=200,
                       help='产品数据数量')
    parser.add_argument('--inventory-count', type=int, default=2000,
                       help='库存数据数量')
    return parser.parse_args()


class TestDataGenerator:
    """测试数据生成器"""
    
    def __init__(self):
        self.output_dir = 'test_data'
        
    def generate_users(self, count: int = 1000) -> List[Dict[str, Any]]:
        """生成用户测试数据"""
        users = []
        roles = ['admin', 'manager', 'user', 'operator']
        statuses = ['active', 'inactive', 'pending']
        
        for i in range(count):
            user = {
                "id": i + 1,
                "username": f"user_{i+1:04d}",
                "email": f"user{i+1:04d}@ai-ready.com",
                "password_hash": "$2a$10$example_hash_placeholder",
                "role": random.choice(roles),
                "status": random.choice(statuses),
                "display_name": f"用户 {i+1}",
                "phone": f"138{random.randint(10000000, 99999999)}",
                "created_at": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
                "updated_at": datetime.now().isoformat()
            }
            users.append(user)
        
        return users
    
    def generate_products(self, count: int = 200) -> List[Dict[str, Any]]:
        """生成产品测试数据"""
        products = []
        categories = ['Electronics', 'Clothing', 'Home', 'Office', 'Industrial']
        statuses = ['active', 'inactive', 'archived']
        
        for i in range(count):
            product = {
                "id": i + 1,
                "sku": f"PROD-{random.randint(10000, 99999)}",
                "name": f"产品 {i+1}",
                "description": f"产品描述 {i+1}",
                "category": random.choice(categories),
                "price": round(random.uniform(10.0, 10000.0), 2),
                "cost": round(random.uniform(5.0, 5000.0), 2),
                "status": random.choice(statuses),
                "created_at": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
                "updated_at": datetime.now().isoformat()
            }
            products.append(product)
        
        return products
    
    def generate_orders(self, count: int = 5000, user_ids: List[int] = None, 
                       product_ids: List[int] = None) -> List[Dict[str, Any]]:
        """生成订单测试数据"""
        orders = []
        statuses = ['pending', 'confirmed', 'shipped', 'delivered', 'cancelled', 'refunded']
        
        if user_ids is None:
            user_ids = list(range(1, 1001))
        if product_ids is None:
            product_ids = list(range(1, 201))
        
        for i in range(count):
            order = {
                "id": i + 1,
                "order_no": f"ORD-{datetime.now().strftime('%Y%m%d')}-{random.randint(10000, 99999)}",
                "user_id": random.choice(user_ids),
                "total_amount": round(random.uniform(100.0, 50000.0), 2),
                "discount_amount": round(random.uniform(0, 5000.0), 2),
                "final_amount": round(random.uniform(100.0, 50000.0), 2),
                "status": random.choice(statuses),
                "created_at": (datetime.now() - timedelta(days=random.randint(1, 30))).isoformat(),
                "updated_at": datetime.now().isoformat()
            }
            orders.append(order)
        
        return orders
    
    def generate_inventory(self, count: int = 2000, product_ids: List[int] = None) -> List[Dict[str, Any]]:
        """生成库存测试数据"""
        inventory_items = []
        statuses = ['available', 'reserved', 'out_of_stock', 'low_stock', 'damaged']
        
        if product_ids is None:
            product_ids = list(range(1, 201))
        
        for i in range(count):
            item = {
                "id": i + 1,
                "product_id": random.choice(product_ids),
                "warehouse_id": random.randint(1, 5),
                "quantity": random.randint(0, 1000),
                "reserved_quantity": random.randint(0, 100),
                "min_stock": random.randint(10, 100),
                "max_stock": random.randint(500, 2000),
                "status": random.choice(statuses),
                "last_update_time": datetime.now().isoformat()
            }
            inventory_items.append(item)
        
        return inventory_items
    
    def generate_suppliers(self, count: int = 50) -> List[Dict[str, Any]]:
        """生成供应商测试数据"""
        suppliers = []
        statuses = ['active', 'inactive', 'pending']
        credit_ratings = ['AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'C']
        
        for i in range(count):
            supplier = {
                "id": i + 1,
                "name": f"供应商 {i+1}",
                "code": f"SUP-{i+1:04d}",
                "contact_name": f"联系人 {i+1}",
                "phone": f"139{random.randint(10000000, 99999999)}",
                "email": f"supplier{i+1}@example.com",
                "address": f"地址 {i+1}",
                "credit_rating": random.choice(credit_ratings),
                "status": random.choice(statuses),
                "created_at": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
                "updated_at": datetime.now().isoformat()
            }
            suppliers.append(supplier)
        
        return suppliers
    
    def save_to_json(self, data: List[Dict[str, Any]], filename: str) -> str:
        """保存数据到JSON文件"""
        filepath = os.path.join(self.output_dir, filename)
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        print(f"✅ 已生成 {len(data)} 条数据: {filepath}")
        return filepath
    
    def save_to_sql(self, data: List[Dict[str, Any]], table_name: str, filename: str):
        """保存数据到SQL文件"""
        filepath = os.path.join(self.output_dir, filename)
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"-- {table_name} 数据插入脚本\n")
            f.write(f"-- 生成时间: {datetime.now().isoformat()}\n\n")
            
            for record in data:
                columns = ', '.join(record.keys())
                values = []
                for value in record.values():
                    if isinstance(value, str):
                        values.append(f"'{value.replace(\"'\", \"''\")}'")
                    elif value is None:
                        values.append('NULL')
                    else:
                        values.append(str(value))
                
                sql = f"INSERT INTO {table_name} ({columns}) VALUES ({', '.join(values)});\n"
                f.write(sql)
        
        print(f"✅ 已生成 {len(data)} 条SQL数据: {filepath}")
        return filepath


def main():
    """主函数"""
    args = parse_args()
    
    # 创建生成器
    generator = TestDataGenerator()
    generator.output_dir = args.output_dir
    
    print(f"📋 测试数据生成器 v1.0")
    print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    
    # 生成用户数据
    print("\n📝 生成用户数据...")
    users = generator.generate_users(args.user_count)
    generator.save_to_json(users, 'users.json')
    generator.save_to_sql(users, 'sys_user', 'users.sql')
    
    # 生成产品数据
    print("\n📝 生成产品数据...")
    products = generator.generate_products(args.product_count)
    generator.save_to_json(products, 'products.json')
    generator.save_to_sql(products, 'erp_product', 'products.sql')
    
    # 生成订单数据
    print("\n📝 生成订单数据...")
    user_ids = [u['id'] for u in generator.generate_users(100)]  # 简化,只取100个用户
    products_ids = [p['id'] for p in generator.generate_products(50)]  # 简化,只取50个产品
    orders = generator.generate_orders(args.order_count, user_ids, products_ids)
    generator.save_to_json(orders, 'orders.json')
    generator.save_to_sql(orders, 'erp_order', 'orders.sql')
    
    # 生成库存数据
    print("\n📝 生成库存数据...")
    products_ids = [p['id'] for p in generator.generate_products(50)]  # 简化
    inventory = generator.generate_inventory(args.inventory_count, products_ids)
    generator.save_to_json(inventory, 'inventory.json')
    generator.save_to_sql(inventory, 'erp_inventory', 'inventory.sql')
    
    # 生成供应商数据
    print("\n📝 生成供应商数据...")
    suppliers = generator.generate_suppliers(50)
    generator.save_to_json(suppliers, 'suppliers.json')
    generator.save_to_sql(suppliers, 'res_partner', 'suppliers.sql')
    
    print("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print(f"✅ 测试数据生成完成!")
    print(f"📁 输出目录: {args.output_dir}")
    print(f"📊 生成统计: 用户{len(users)}条, 产品{len(products)}条, 订单{len(orders)}条, 库存{len(inventory)}条, 供应商{len(suppliers)}条")


if __name__ == '__main__':
    main()
