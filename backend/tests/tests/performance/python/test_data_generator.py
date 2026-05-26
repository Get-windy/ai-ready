#!/usr/bin/env python3
"""
AI-Ready 性能测试数据生成脚本
Sprint 27+1 测试环境配置 - 性能测试自动化脚本开发

功能:
1. 生成测试用户数据
2. 生成测试商品数据
3. 生成测试订单数据
4. 支持CSV和JSON格式导出
"""

import csv
import json
import random
import string
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List

# 数据输出目录
DATA_DIR = Path(__file__).parent.parent / "data"


class TestDataGenerator:
    """测试数据生成器"""
    
    def __init__(self, seed: int = None):
        if seed:
            random.seed(seed)
        
        self.users = []
        self.products = []
        self.orders = []
        self.warehouses = []
        self.customers = []
    
    def generate_users(self, count: int = 200) -> List[Dict]:
        """生成测试用户数据"""
        self.users = []
        
        user_types = ["normal", "vip", "admin"]
        type_distribution = {
            "normal": 0.75,  # 75%
            "vip": 0.20,     # 20%
            "admin": 0.05    # 5%
        }
        
        for i in range(1, count + 1):
            # 确定用户类型
            rand = random.random()
            if rand < type_distribution["normal"]:
                user_type = "normal"
            elif rand < type_distribution["normal"] + type_distribution["vip"]:
                user_type = "vip"
            else:
                user_type = "admin"
            
            user = {
                "userId": i,
                "username": f"testuser{i:03d}",
                "password": f"password{i:03d}",
                "email": f"testuser{i:03d}@ai-ready.test",
                "phone": f"138{random.randint(10000000, 99999999)}",
                "userType": user_type,
                "tenantId": 1,
                "status": "ACTIVE",
                "createdAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            self.users.append(user)
        
        return self.users
    
    def generate_products(self, count: int = 50) -> List[Dict]:
        """生成测试商品数据"""
        self.products = []
        
        categories = ["electronics", "clothing", "food", "office", "other"]
        category_distribution = [0.25, 0.20, 0.15, 0.20, 0.20]
        
        for i in range(1, count + 1):
            # 确定商品类别
            category = random.choices(categories, weights=category_distribution)[0]
            
            # 根据类别生成价格范围
            if category == "electronics":
                price = random.uniform(500, 5000)
            elif category == "clothing":
                price = random.uniform(50, 500)
            elif category == "food":
                price = random.uniform(10, 100)
            elif category == "office":
                price = random.uniform(20, 200)
            else:
                price = random.uniform(30, 300)
            
            product = {
                "productId": i,
                "productCode": f"PRD{i:04d}",
                "productName": f"{category.capitalize()} Product {i:03d}",
                "category": category,
                "price": round(price, 2),
                "unit": random.choice(["件", "个", "包", "箱", "套"]),
                "minStock": random.randint(10, 50),
                "maxStock": random.randint(200, 1000),
                "tenantId": 1,
                "status": "ACTIVE",
                "createdAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            self.products.append(product)
        
        return self.products
    
    def generate_orders(self, count: int = 100, users: List = None, products: List = None) -> List[Dict]:
        """生成测试订单数据"""
        self.orders = []
        
        if not users:
            users = self.users if self.users else self.generate_users(200)
        if not products:
            products = self.products if self.products else self.generate_products(50)
        
        order_types = ["NORMAL", "VIP", "URGENT", "BULK"]
        order_statuses = ["CREATED", "CONFIRMED", "PROCESSING", "COMPLETED", "CANCELLED"]
        
        for i in range(1, count + 1):
            user = random.choice(users)
            product = random.choice(products)
            
            quantity = random.randint(1, 10)
            unit_price = product["price"]
            total_amount = round(quantity * unit_price, 2)
            
            # 订单时间（最近30天内）
            order_date = datetime.now() - timedelta(days=random.randint(1, 30))
            
            order = {
                "orderId": i,
                "orderCode": f"ORD{i:06d}",
                "userId": user["userId"],
                "username": user["username"],
                "customerId": random.randint(1, 20),
                "productId": product["productId"],
                "productName": product["productName"],
                "quantity": quantity,
                "unitPrice": unit_price,
                "totalAmount": total_amount,
                "orderType": random.choice(order_types),
                "orderStatus": random.choices(order_statuses, weights=[0.1, 0.15, 0.20, 0.45, 0.10])[0],
                "tenantId": 1,
                "createdAt": order_date.strftime("%Y-%m-%d %H:%M:%S")
            }
            self.orders.append(order)
        
        return self.orders
    
    def generate_warehouses(self, count: int = 5) -> List[Dict]:
        """生成仓库数据"""
        self.warehouses = []
        
        warehouse_names = ["北京仓库", "上海仓库", "广州仓库", "成都仓库", "武汉仓库"]
        
        for i in range(1, count + 1):
            warehouse = {
                "warehouseId": i,
                "warehouseCode": f"WH{i:02d}",
                "warehouseName": warehouse_names[i-1] if i <= len(warehouse_names) else f"仓库{i}",
                "location": f"城市{i}",
                "capacity": random.randint(10000, 50000),
                "manager": f"manager{i:03d}",
                "tenantId": 1,
                "status": "ACTIVE"
            }
            self.warehouses.append(warehouse)
        
        return self.warehouses
    
    def generate_customers(self, count: int = 20) -> List[Dict]:
        """生成客户数据"""
        self.customers = []
        
        customer_types = ["INDIVIDUAL", "CORPORATE", "VIP"]
        
        for i in range(1, count + 1):
            customer = {
                "customerId": i,
                "customerCode": f"CUS{i:04d}",
                "customerName": f"客户{i:03d}",
                "customerType": random.choice(customer_types),
                "contactPhone": f"139{random.randint(10000000, 99999999)}",
                "contactEmail": f"customer{i:03d}@test.com",
                "address": f"测试地址{i:03d}",
                "tenantId": 1,
                "status": "ACTIVE"
            }
            self.customers.append(customer)
        
        return self.customers
    
    def export_to_csv(self, data: List[Dict], filename: str, output_dir: Path = DATA_DIR):
        """导出CSV格式"""
        output_dir.mkdir(parents=True, exist_ok=True)
        output_file = output_dir / filename
        
        if not data:
            return None
        
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=data[0].keys())
            writer.writeheader()
            writer.writerows(data)
        
        print(f"CSV文件已生成: {output_file} ({len(data)}条记录)")
        return output_file
    
    def export_to_json(self, data: List[Dict], filename: str, output_dir: Path = DATA_DIR):
        """导出JSON格式"""
        output_dir.mkdir(parents=True, exist_ok=True)
        output_file = output_dir / filename
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        print(f"JSON文件已生成: {output_file} ({len(data)}条记录)")
        return output_file
    
    def generate_all_test_data(self, 
                                user_count: int = 200,
                                product_count: int = 50,
                                order_count: int = 100,
                                warehouse_count: int = 5,
                                customer_count: int = 20) -> Dict:
        """生成所有测试数据"""
        print("=== 开始生成测试数据 ===")
        
        # 生成各类数据
        users = self.generate_users(user_count)
        products = self.generate_products(product_count)
        warehouses = self.generate_warehouses(warehouse_count)
        customers = self.generate_customers(customer_count)
        orders = self.generate_orders(order_count, users, products)
        
        # 导出CSV
        csv_files = {
            "users": self.export_to_csv(users, "test_users.csv"),
            "products": self.export_to_csv(products, "test_products.csv"),
            "orders": self.export_to_csv(orders, "test_orders.csv"),
            "warehouses": self.export_to_csv(warehouses, "test_warehouses.csv"),
            "customers": self.export_to_csv(customers, "test_customers.csv")
        }
        
        # 导出JSON
        json_files = {
            "users": self.export_to_json(users, "test_users.json"),
            "products": self.export_to_json(products, "test_products.json"),
            "orders": self.export_to_json(orders, "test_orders.json"),
            "warehouses": self.export_to_json(warehouses, "test_warehouses.json"),
            "customers": self.export_to_json(customers, "test_customers.json")
        }
        
        # 生成数据摘要
        summary = {
            "generated_at": datetime.now().isoformat(),
            "statistics": {
                "users": {
                    "total": len(users),
                    "by_type": {
                        "normal": len([u for u in users if u["userType"] == "normal"]),
                        "vip": len([u for u in users if u["userType"] == "vip"]),
                        "admin": len([u for u in users if u["userType"] == "admin"])
                    }
                },
                "products": {
                    "total": len(products),
                    "by_category": {}
                },
                "orders": {
                    "total": len(orders),
                    "by_status": {}
                },
                "warehouses": {
                    "total": len(warehouses)
                },
                "customers": {
                    "total": len(customers)
                }
            },
            "output_files": {
                "csv": {k: str(v) for k, v in csv_files.items()},
                "json": {k: str(v) for k, v in json_files.items()}
            }
        }
        
        # 统计商品类别分布
        for product in products:
            category = product["category"]
            if category not in summary["statistics"]["products"]["by_category"]:
                summary["statistics"]["products"]["by_category"][category] = 0
            summary["statistics"]["products"]["by_category"][category] += 1
        
        # 统计订单状态分布
        for order in orders:
            status = order["orderStatus"]
            if status not in summary["statistics"]["orders"]["by_status"]:
                summary["statistics"]["orders"]["by_status"][status] = 0
            summary["statistics"]["orders"]["by_status"][status] += 1
        
        # 导出摘要
        summary_file = self.export_to_json(summary, "test_data_summary.json")
        
        print("\n=== 测试数据生成完成 ===")
        print(f"用户: {len(users)}条")
        print(f"商品: {len(products)}条")
        print(f"订单: {len(orders)}条")
        print(f"仓库: {len(warehouses)}条")
        print(f"客户: {len(customers)}条")
        
        return summary


def main():
    """主函数"""
    generator = TestDataGenerator(seed=42)  # 固定seed保证数据可复现
    
    summary = generator.generate_all_test_data(
        user_count=200,
        product_count=50,
        order_count=100,
        warehouse_count=5,
        customer_count=20
    )
    
    print(f"\n数据摘要已保存: {summary['output_files']['json']['users']}")
    
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())