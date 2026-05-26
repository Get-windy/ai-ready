# 测试数据管理与质量保证体系

## 1. 概述

本文档定义了AI-Ready项目的测试数据管理与质量保证体系，确保测试数据的准确性、一致性和可重复性。

## 2. 测试数据管理体系设计

### 2.1 测试数据分类和分级标准

#### 数据分类：
- **基础数据**: 用户、产品、供应商等基础信息
- **业务数据**: 订单、库存、交易等业务过程数据
- **性能数据**: 用于性能测试的大规模数据
- **异常数据**: 错误、边界条件、异常场景数据
- **安全数据**: 权限、角色、安全测试相关数据

#### 数据分级：
- **P0级（核心数据）**: 项目启动必须的核心测试数据
- **P1级（关键数据）**: 主要业务场景测试数据
- **P2级（完整数据）**: 完整业务流程测试数据
- **P3级（边缘数据）**: 边界条件和异常场景数据

### 2.2 测试数据生成和清理策略

#### 数据生成策略：
1. **基础数据生成**: 使用确定性算法生成基础数据
2. **业务数据生成**: 基于业务流程规则生成业务数据
3. **性能数据生成**: 使用批量生成工具生成大规模数据
4. **异常数据生成**: 专门设计异常场景数据

#### 数据清理策略：
1. **测试前清理**: 每次测试前清理上一次的测试数据
2. **测试后归档**: 测试结束后归档重要测试数据
3. **定期清理**: 定期清理过期和无用测试数据
4. **版本管理**: 对测试数据文件进行版本控制

### 2.3 测试数据版本管理和追溯机制

#### 版本管理：
- 所有测试数据文件使用Git进行版本控制
- 每个测试数据集有唯一的版本号
- 记录数据变更历史和原因
- 支持数据回滚和对比

#### 追溯机制：
- 记录测试数据生成时间和来源
- 建立数据与测试用例的关联关系
- 实现数据变更的完整追溯
- 提供数据使用统计和报告

### 2.4 测试数据安全合规标准

#### 数据安全：
- 禁止在测试数据中使用真实个人信息
- 使用数据脱敏和匿名化技术
- 加密存储敏感测试数据
- 控制测试数据的访问权限

#### 合规标准：
- 遵循GDPR等数据保护法规
- 建立测试数据使用审批流程
- 定期进行数据安全审计
- 提供数据合规性报告

## 3. 测试数据生成工具开发

### 3.1 基础测试数据生成脚本

```python
# I:\AI-Ready\qa\test-data-generator.py
"""
基础测试数据生成脚本
支持生成用户、产品、订单等基础测试数据
"""

import json
import random
import datetime
from typing import Dict, List, Any

class TestDataGenerator:
    """测试数据生成器基类"""
    
    def __init__(self, seed: int = 42):
        """初始化生成器"""
        random.seed(seed)
        
    def generate_user_data(self, count: int = 100) -> List[Dict]:
        """生成用户数据"""
        users = []
        for i in range(count):
            user = {
                "user_id": f"USER_{str(i+1).zfill(6)}",
                "username": f"testuser{i+1}",
                "email": f"testuser{i+1}@example.com",
                "phone": self._generate_phone(),
                "status": random.choice(["active", "inactive", "blocked"]),
                "created_at": self._generate_timestamp(days_back=365),
                "last_login": self._generate_timestamp(days_back=30),
                "role": random.choice(["admin", "user", "manager", "operator"])
            }
            users.append(user)
        return users
    
    def generate_product_data(self, count: int = 50) -> List[Dict]:
        """生成产品数据"""
        products = []
        categories = ["电子产品", "办公用品", "家居用品", "食品饮料", "服装鞋帽"]
        for i in range(count):
            product = {
                "product_id": f"PROD_{str(i+1).zfill(6)}",
                "product_code": f"P{str(i+1).zfill(5)}",
                "product_name": f"测试产品{i+1}",
                "category": random.choice(categories),
                "unit_price": round(random.uniform(10, 1000), 2),
                "stock_quantity": random.randint(0, 1000),
                "status": random.choice(["active", "discontinued", "out_of_stock"]),
                "created_at": self._generate_timestamp(days_back=180)
            }
            products.append(product)
        return products
    
    def generate_order_data(self, user_count: int = 100, 
                          product_count: int = 50,
                          order_count: int = 200) -> List[Dict]:
        """生成订单数据"""
        orders = []
        for i in range(order_count):
            order = {
                "order_id": f"ORDER_{str(i+1).zfill(8)}",
                "user_id": f"USER_{str(random.randint(1, user_count)).zfill(6)}",
                "order_date": self._generate_timestamp(days_back=30),
                "total_amount": round(random.uniform(100, 10000), 2),
                "status": random.choice(["pending", "paid", "shipped", "delivered", "cancelled"]),
                "items": []
            }
            
            # 生成订单项
            item_count = random.randint(1, 5)
            for j in range(item_count):
                product_id = f"PROD_{str(random.randint(1, product_count)).zfill(6)}"
                item = {
                    "product_id": product_id,
                    "quantity": random.randint(1, 10),
                    "unit_price": round(random.uniform(10, 1000), 2)
                }
                order["items"].append(item)
            
            orders.append(order)
        return orders
    
    def _generate_phone(self) -> str:
        """生成手机号"""
        prefix = random.choice(["13", "15", "18", "19"])
        suffix = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        return f"{prefix}{suffix}"
    
    def _generate_timestamp(self, days_back: int = 30) -> str:
        """生成时间戳"""
        now = datetime.datetime.now()
        random_days = random.randint(0, days_back)
        random_hours = random.randint(0, 23)
        random_minutes = random.randint(0, 59)
        random_seconds = random.randint(0, 59)
        
        past_time = now - datetime.timedelta(
            days=random_days,
            hours=random_hours,
            minutes=random_minutes,
            seconds=random_seconds
        )
        return past_time.isoformat()
    
    def save_to_json(self, data: List[Dict], filename: str):
        """保存数据到JSON文件"""
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"数据已保存到 {filename}，共 {len(data)} 条记录")

# 使用示例
if __name__ == "__main__":
    generator = TestDataGenerator()
    
    # 生成基础数据
    users = generator.generate_user_data(100)
    products = generator.generate_product_data(50)
    orders = generator.generate_order_data(100, 50, 200)
    
    # 保存数据
    generator.save_to_json(users, "test-data/users.json")
    generator.save_to_json(products, "test-data/products.json")
    generator.save_to_json(orders, "test-data/orders.json")
```

### 3.2 业务场景测试数据生成工具

```python
# I:\AI-Ready\qa\business-scenario-generator.py
"""
业务场景测试数据生成工具
生成完整的业务场景测试数据
"""

import json
from datetime import datetime, timedelta
from test_data_generator import TestDataGenerator

class BusinessScenarioGenerator(TestDataGenerator):
    """业务场景数据生成器"""
    
    def generate_inventory_scenario(self):
        """生成库存管理场景数据"""
        scenario = {
            "name": "库存管理完整业务流程",
            "description": "模拟从采购入库到销售出库的完整库存管理流程",
            "data_sets": {
                "suppliers": self._generate_suppliers(10),
                "warehouses": self._generate_warehouses(3),
                "purchase_orders": self._generate_purchase_orders(5),
                "sales_orders": self._generate_sales_orders(10),
                "stock_transfers": self._generate_stock_transfers(5)
            }
        }
        return scenario
    
    def generate_finance_scenario(self):
        """生成财务管理场景数据"""
        scenario = {
            "name": "财务管理完整业务流程",
            "description": "模拟会计凭证、财务报表、预算管理等财务流程",
            "data_sets": {
                "accounts": self._generate_accounts(50),
                "vouchers": self._generate_vouchers(100),
                "invoices": self._generate_invoices(30),
                "payments": self._generate_payments(20),
                "budgets": self._generate_budgets(10)
            }
        }
        return scenario
    
    def _generate_suppliers(self, count: int) -> List[Dict]:
        """生成供应商数据"""
        suppliers = []
        for i in range(count):
            supplier = {
                "supplier_id": f"SUP_{str(i+1).zfill(6)}",
                "supplier_name": f"测试供应商{i+1}",
                "contact_person": f"联系人{i+1}",
                "phone": self._generate_phone(),
                "email": f"supplier{i+1}@example.com",
                "status": random.choice(["active", "inactive"]),
                "rating": random.choice(["A", "B", "C"]),
                "payment_terms": random.choice(["NET30", "NET60", "COD"])
            }
            suppliers.append(supplier)
        return suppliers
    
    def _generate_warehouses(self, count: int) -> List[Dict]:
        """生成仓库数据"""
        warehouses = []
        locations = ["北京", "上海", "广州", "深圳", "成都", "武汉", "西安"]
        for i in range(count):
            warehouse = {
                "warehouse_id": f"WH_{str(i+1).zfill(4)}",
                "warehouse_name": f"{random.choice(locations)}仓库{i+1}",
                "location": random.choice(locations),
                "type": random.choice(["main", "regional", "transit"]),
                "capacity": random.randint(1000, 10000),
                "manager": f"仓库经理{i+1}",
                "phone": self._generate_phone()
            }
            warehouses.append(warehouse)
        return warehouses
    
    def _generate_purchase_orders(self, count: int) -> List[Dict]:
        """生成采购订单数据"""
        purchase_orders = []
        for i in range(count):
            po = {
                "po_id": f"PO_{str(i+1).zfill(8)}",
                "supplier_id": f"SUP_{str(random.randint(1, 10)).zfill(6)}",
                "order_date": self._generate_timestamp(days_back=30),
                "expected_delivery_date": self._generate_future_date(days_ahead=7),
                "total_amount": round(random.uniform(1000, 50000), 2),
                "status": random.choice(["draft", "confirmed", "in_transit", "delivered", "paid"]),
                "items": []
            }
            
            item_count = random.randint(1, 8)
            for j in range(item_count):
                item = {
                    "product_id": f"PROD_{str(random.randint(1, 50)).zfill(6)}",
                    "quantity": random.randint(10, 100),
                    "unit_price": round(random.uniform(10, 1000), 2),
                    "expected_delivery": self._generate_future_date(days_ahead=random.randint(1, 14))
                }
                po["items"].append(item)
            
            purchase_orders.append(po)
        return purchase_orders
    
    def _generate_sales_orders(self, count: int) -> List[Dict]:
        """生成销售订单数据"""
        sales_orders = []
        for i in range(count):
            so = {
                "so_id": f"SO_{str(i+1).zfill(8)}",
                "customer_id": f"CUST_{str(random.randint(1, 100)).zfill(6)}",
                "order_date": self._generate_timestamp(days_back=30),
                "required_delivery_date": self._generate_future_date(days_ahead=random.randint(1, 7)),
                "total_amount": round(random.uniform(500, 20000), 2),
                "status": random.choice(["draft", "confirmed", "picking", "shipped", "delivered", "invoiced", "paid"]),
                "shipping_address": self._generate_address(),
                "items": []
            }
            
            item_count = random.randint(1, 5)
            for j in range(item_count):
                item = {
                    "product_id": f"PROD_{str(random.randint(1, 50)).zfill(6)}",
                    "quantity": random.randint(1, 20),
                    "unit_price": round(random.uniform(10, 1000), 2),
                    "warehouse_id": f"WH_{str(random.randint(1, 3)).zfill(4)}"
                }
                so["items"].append(item)
            
            sales_orders.append(so)
        return sales_orders
    
    def _generate_stock_transfers(self, count: int) -> List[Dict]:
        """生成库存调拨数据"""
        transfers = []
        for i in range(count):
            transfer = {
                "transfer_id": f"TR_{str(i+1).zfill(8)}",
                "from_warehouse": f"WH_{str(random.randint(1, 3)).zfill(4)}",
                "to_warehouse": f"WH_{str(random.randint(1, 3)).zfill(4)}",
                "transfer_date": self._generate_timestamp(days_back=15),
                "status": random.choice(["draft", "confirmed", "in_transit", "completed"]),
                "reason": random.choice(["库存平衡", "销售需求", "仓库调整", "质量检查"]),
                "items": []
            }
            
            item_count = random.randint(1, 3)
            for j in range(item_count):
                item = {
                    "product_id": f"PROD_{str(random.randint(1, 50)).zfill(6)}",
                    "quantity": random.randint(10, 50),
                    "batch_no": f"BATCH_{random.randint(1000, 9999)}"
                }
                transfer["items"].append(item)
            
            transfers.append(transfer)
        return transfers
    
    def _generate_accounts(self, count: int) -> List[Dict]:
        """生成会计科目数据"""
        accounts = []
        account_types = ["资产", "负债", "权益", "收入", "费用"]
        for i in range(count):
            account = {
                "account_code": f"{random.randint(1000, 9999)}",
                "account_name": f"测试科目{i+1}",
                "account_type": random.choice(account_types),
                "balance_type": random.choice(["debit", "credit"]),
                "opening_balance": round(random.uniform(-10000, 10000), 2),
                "current_balance": round(random.uniform(-50000, 50000), 2),
                "is_active": random.choice([True, False])
            }
            accounts.append(account)
        return accounts
    
    def _generate_vouchers(self, count: int) -> List[Dict]:
        """生成会计凭证数据"""
        vouchers = []
        for i in range(count):
            voucher = {
                "voucher_id": f"VOUCHER_{str(i+1).zfill(8)}",
                "voucher_date": self._generate_timestamp(days_back=60),
                "voucher_type": random.choice(["普通", "收款", "付款", "转账"]),
                "description": f"测试凭证{i+1}",
                "total_amount": round(random.uniform(100, 10000), 2),
                "entries": []
            }
            
            # 生成分录（必须平衡）
            entry_count = random.randint(2, 5)
            total_debit = 0
            total_credit = 0
            
            for j in range(entry_count):
                is_debit = random.choice([True, False])
                amount = round(random.uniform(10, 1000), 2)
                
                if is_debit:
                    total_debit += amount
                else:
                    total_credit += amount
                
                entry = {
                    "account_code": f"{random.randint(1000, 9999)}",
                    "debit_amount": amount if is_debit else 0,
                    "credit_amount": amount if not is_debit else 0,
                    "description": f"测试分录{j+1}"
                }
                voucher["entries"].append(entry)
            
            # 确保凭证平衡
            if total_debit != total_credit:
                diff = round(total_debit - total_credit, 2)
                if diff > 0:
                    # 添加贷方分录平衡
                    voucher["entries"].append({
                        "account_code": "9999",
                        "debit_amount": 0,
                        "credit_amount": diff,
                        "description": "平衡调整"
                    })
                else:
                    # 添加借方分录平衡
                    voucher["entries"].append({
                        "account_code": "9999",
                        "debit_amount": -diff,
                        "credit_amount": 0,
                        "description": "平衡调整"
                    })
            
            vouchers.append(voucher)
        return vouchers
    
    def _generate_invoices(self, count: int) -> List[Dict]:
        """生成发票数据"""
        invoices = []
        for i in range(count):
            invoice = {
                "invoice_id": f"INV_{str(i+1).zfill(8)}",
                "invoice_no": f"发票号{i+1:08d}",
                "invoice_date": self._generate_timestamp(days_back=30),
                "customer_id": f"CUST_{str(random.randint(1, 100)).zfill(6)}",
                "customer_name": f"测试客户{i+1}",
                "tax_id": f"税号{random.randint(100000000000000000, 999999999999999999)}",
                "address": self._generate_address(),
                "subtotal": round(random.uniform(500, 5000), 2),
                "tax_rate": round(random.uniform(0, 0.13), 2),
                "tax_amount": 0,
                "total_amount": 0,
                "status": random.choice(["draft", "issued", "paid", "cancelled"]),
                "items": []
            }
            
            item_count = random.randint(1, 5)
            subtotal = 0
            for j in range(item_count):
                quantity = random.randint(1, 10)
                unit_price = round(random.uniform(10, 500), 2)
                amount = round(quantity * unit_price, 2)
                subtotal += amount
                
                item = {
                    "product_id": f"PROD_{str(random.randint(1, 50)).zfill(6)}",
                    "product_name": f"测试产品{random.randint(1, 50)}",
                    "quantity": quantity,
                    "unit_price": unit_price,
                    "amount": amount
                }
                invoice["items"].append(item)
            
            invoice["subtotal"] = round(subtotal, 2)
            invoice["tax_amount"] = round(subtotal * invoice["tax_rate"], 2)
            invoice["total_amount"] = round(invoice["subtotal"] + invoice["tax_amount"], 2)
            
            invoices.append(invoice)
        return invoices
    
    def _generate_payments(self, count: int) -> List[Dict]:
        """生成付款数据"""
        payments = []
        for i in range(count):
            payment = {
                "payment_id": f"PAY_{str(i+1).zfill(8)}",
                "payment_date": self._generate_timestamp(days_back=30),
                "payment_type": random.choice(["现金", "银行转账", "支票", "信用卡"]),
                "payer": f"付款方{i+1}",
                "payee": f"收款方{i+1}",
                "amount": round(random.uniform(100, 10000), 2),
                "currency": "CNY",
                "status": random.choice(["pending", "processing", "completed", "failed"]),
                "reference_no": f"REF{random.randint(100000, 999999)}",
                "description": f"测试付款{i+1}"
            }
            payments.append(payment)
        return payments
    
    def _generate_budgets(self, count: int) -> List[Dict]:
        """生成预算数据"""
        budgets = []
        departments = ["销售部", "市场部", "研发部", "行政部", "财务部", "生产部"]
        for i in range(count):
            budget = {
                "budget_id": f"BUDGET_{str(i+1).zfill(6)}",
                "fiscal_year": 2026,
                "department": random.choice(departments),
                "budget_category": random.choice(["人员费用", "办公费用", "差旅费用", "设备采购", "市场推广"]),
                "planned_amount": round(random.uniform(10000, 500000), 2),
                "actual_amount": round(random.uniform(8000, 600000), 2),
                "variance": 0,
                "variance_percentage": 0,
                "status": random.choice(["draft", "approved", "in_execution", "completed", "closed"])
            }
            budget["variance"] = round(budget["actual_amount"] - budget["planned_amount"], 2)
            if budget["planned_amount"] != 0:
                budget["variance_percentage"] = round((budget["variance"] / budget["planned_amount"]) * 100, 2)
            
            budgets.append(budget)
        return budgets
    
    def _generate_address(self) -> str:
        """生成地址"""
        cities = ["北京市", "上海市", "广州市", "深圳市", "成都市", "武汉市", "西安市"]
        districts = ["朝阳区", "浦东新区", "天河区", "南山区", "武侯区", "江汉区", "雁塔区"]
        streets = ["人民路", "解放路", "中山路", "建设路", "新华路", "长江路", "黄河路"]
        
        city = random.choice(cities)
        district = random.choice(districts)
        street = random.choice(streets)
        number = random.randint(1, 999)
        
        return f"{city}{district}{street}{number}号"
    
    def _generate_future_date(self, days_ahead: int = 30) -> str:
        """生成未来日期"""
        future_date = datetime.now() + timedelta(days=random.randint(1, days_ahead))
        return future_date.isoformat()

# 使用示例
if __name__ == "__main__":
    generator = BusinessScenarioGenerator()
    
    # 生成库存管理场景
    inventory_scenario = generator.generate_inventory_scenario()
    
    # 生成财务管理场景
    finance_scenario = generator.generate_finance_scenario()
    
    # 保存场景数据
    with open("test-data/inventory_scenario.json", 'w', encoding='utf-8') as f:
        json.dump(inventory_scenario, f, ensure_ascii=False, indent=2)
    
    with open("test-data/finance_scenario.json", 'w', encoding='utf-8') as f:
        json.dump(finance_scenario, f, ensure_ascii=False, indent=2)
    
    print("业务场景测试数据已生成并保存")
```

### 3.3 性能测试负载数据生成工具

```python
# I:\AI-Ready\qa\performance-data-generator.py
"""
性能测试负载数据生成工具
生成大规模性能测试数据
"""

import json
import random
import time
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed

class PerformanceDataGenerator:
    """性能测试数据生成器"""
    
    def __init__(self, batch_size: int = 1000):
        self.batch_size = batch_size
        
    def generate_large_user_dataset(self, total_count: int = 100000) -> List[Dict]:
        """生成大规模用户数据集"""
        print(f"开始生成 {total_count} 条用户数据...")
        start_time = time.time()
        
        users = []
        for i in range(total_count):
            if i > 0 and i % 10000 == 0:
                elapsed = time.time() - start_time
                print(f"已生成 {i} 条数据，耗时 {elapsed:.2f} 秒")
            
            user = {
                "user_id": f"PERF_USER_{str(i+1).zfill(8)}",
                "username": f"perfuser{i+1}",
                "email": f"perfuser{i+1}@example.com",
                "phone": self._generate_phone(),
                "registration_date": self._generate_timestamp(days_back=365),
                "last_login": self._generate_timestamp(days_back=random.randint(0, 30)),
                "login_count": random.randint(0, 1000),
                "total_orders": random.randint(0, 100),
                "total_spent": round(random.uniform(0, 100000), 2),
                "status": random.choice(["active", "inactive", "blocked"]),
                "user_level": random.choice(["bronze", "silver", "gold", "platinum", "diamond"]),
                "preferences": {
                    "language": random.choice(["zh-CN", "en-US"]),
                    "timezone": random.choice(["Asia/Shanghai", "America/New_York", "Europe/London"]),
                    "notification_enabled": random.choice([True, False])
                },
                "address": self._generate_address(),
                "tags": random.sample(["VIP", "新用户", "老用户", "高价值", "促销敏感", "投诉用户"], 
                                    random.randint(0, 3))
            }
            users.append(user)
        
        elapsed = time.time() - start_time
        print(f"用户数据生成完成，共 {len(users)} 条数据，总耗时 {elapsed:.2f} 秒")
        return users
    
    def generate_large_order_dataset(self, total_count: int = 50000, 
                                   user_count: int = 100000) -> List[Dict]:
        """生成大规模订单数据集"""
        print(f"开始生成 {total_count} 条订单数据...")
        start_time = time.time()
        
        orders = []
        for i in range(total_count):
            if i > 0 and i % 10000 == 0:
                elapsed = time.time() - start_time
                print(f"已生成 {i} 条数据，耗时 {elapsed:.2f} 秒")
            
            order = {
                "order_id": f"PERF_ORDER_{str(i+1).zfill(10)}",
                "user_id": f"PERF_USER_{str(random.randint(1, user_count)).zfill(8)}",
                "order_date": self._generate_timestamp(days_back=30),
                "total_amount": round(random.uniform(10, 10000), 2),
                "discount_amount": round(random.uniform(0, 1000), 2),
                "shipping_amount": round(random.uniform(0, 100), 2),
                "tax_amount": round(random.uniform(0, 500), 2),
                "final_amount": 0,
                "status": self._weighted_choice({
                    "pending": 10,
                    "paid": 60,
                    "shipped": 20,
                    "delivered": 8,
                    "cancelled": 2
                }),
                "payment_method": random.choice(["alipay", "wechat_pay", "bank_transfer", "credit_card"]),
                "shipping_method": random.choice(["standard", "express", "overnight"]),
                "shipping_address": self._generate_address(),
                "billing_address": self._generate_address(),
                "items": []
            }
            
            # 计算最终金额
            order["final_amount"] = round(
                order["total_amount"] - order["discount_amount"] + 
                order["shipping_amount"] + order["tax_amount"], 2
            )
            
            # 生成订单项
            item_count = random.randint(1, 8)
            for j in range(item_count):
                product_id = f"PERF_PROD_{str(random.randint(1, 10000)).zfill(8)}"
                quantity = random.randint(1, 10)
                unit_price = round(random.uniform(1, 1000), 2)
                discount_rate = round(random.uniform(0, 0.3), 2)
                
                item = {
                    "product_id": product_id,
                    "product_name": f"性能测试产品{random.randint(1, 10000)}",
                    "quantity": quantity,
                    "unit_price": unit_price,
                    "discount_rate": discount_rate,
                    "discount_amount": round(quantity * unit_price * discount_rate, 2),
                    "final_price": round(quantity * unit_price * (1 - discount_rate), 2),
                    "category": random.choice(["电子产品", "办公用品", "家居用品", "食品饮料", "服装鞋帽"])
                }
                order["items"].append(item)
            
            orders.append(order)
        
        elapsed = time.time() - start_time
        print(f"订单数据生成完成，共 {len(orders)} 条数据，总耗时 {elapsed:.2f} 秒")
        return orders
    
    def generate_large_product_dataset(self, total_count: int = 10000) -> List[Dict]:
        """生成大规模产品数据集"""
        print(f"开始生成 {total_count} 条产品数据...")
        start_time = time.time()
        
        products = []
        categories = ["电子产品", "办公用品", "家居用品", "食品饮料", "服装鞋帽", 
                     "图书音像", "运动户外", "美妆个护", "母婴用品", "汽车用品"]
        brands = ["品牌A", "品牌B", "品牌C", "品牌D", "品牌E", "品牌F", "品牌G", "品牌H"]
        
        for i in range(total_count):
            if i > 0 and i % 2000 == 0:
                elapsed = time.time() - start_time
                print(f"已生成 {i} 条数据，耗时 {elapsed:.2f} 秒")
            
            product = {
                "product_id": f"PERF_PROD_{str(i+1).zfill(8)}",
                "product_code": f"PERF_{str(i+1).zfill(10)}",
                "product_name": f"性能测试产品{i+1}",
                "category": random.choice(categories),
                "brand": random.choice(brands),
                "description": f"这是性能测试产品{i+1}的详细描述，用于大规模性能测试场景。",
                "specifications": {
                    "weight": f"{random.uniform(0.1, 10.0):.2f}kg",
                    "dimensions": f"{random.uniform(10, 100):.0f}x{random.uniform(10, 100):.0f}x{random.uniform(1, 20):.0f}cm",
                    "material": random.choice(["塑料", "金属", "木材", "布料", "玻璃"]),
                    "color": random.choice(["黑色", "白色", "红色", "蓝色", "绿色", "银色"])
                },
                "unit_price": round(random.uniform(1, 1000), 2),
                "cost_price": round(random.uniform(0.5, 800), 2),
                "stock_quantity": random.randint(0, 10000),
                "safety_stock": random.randint(10, 100),
                "reorder_point": random.randint(50, 500),
                "status": self._weighted_choice({
                    "active": 80,
                    "out_of_stock": 10,
                    "discontinued": 5,
                    "coming_soon": 5
                }),
                "rating": round(random.uniform(1, 5), 1),
                "review_count": random.randint(0, 10000),
                "sales_count": random.randint(0, 50000),
                "created_at": self._generate_timestamp(days_back=365),
                "updated_at": self._generate_timestamp(days_back=random.randint(0, 30)),
                "attributes": {
                    "is_featured": random.choice([True, False]),
                    "is_new": random.choice([True, False]),
                    "is_hot": random.choice([True, False]),
                    "has_warranty": random.choice([True, False]),
                    "warranty_period": random.randint(0, 24)
                },
                "tags": random.sample(["热销", "新品", "推荐", "限时优惠", "包邮", "进口", "国产"], 
                                    random.randint(0, 4))
            }
            products.append(product)
        
        elapsed = time.time() - start_time
        print(f"产品数据生成完成，共 {len(products)} 条数据，总耗时 {elapsed:.2f} 秒")
        return products
    
    def generate_concurrent_data(self, data_type: str, total_count: int, 
                               thread_count: int = 4) -> List[Dict]:
        """并发生成数据"""
        print(f"开始并发生成 {total_count} 条{data_type}数据，使用 {thread_count} 个线程...")
        start_time = time.time()
        
        # 计算每个线程的任务量
        batch_size = total_count // thread_count
        remainder = total_count % thread_count
        
        all_data = []
        
        def generate_batch(thread_id: int, count: int) -> List[Dict]:
            """生成一个批次的数据"""
            batch_data = []
            start_idx = thread_id * batch_size
            
            for i in range(count):
                if data_type == "user":
                    data = self._generate_single_user(start_idx + i)
                elif data_type == "order":
                    data = self._generate_single_order(start_idx + i)
                elif data_type == "product":
                    data = self._generate_single_product(start_idx + i)
                else:
                    data = {}
                
                batch_data.append(data)
            
            return batch_data
        
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            futures = []
            
            # 提交任务
            for thread_id in range(thread_count):
                count = batch_size
                if thread_id == thread_count - 1:
                    count += remainder  # 最后一个线程处理余数
                
                future = executor.submit(generate_batch, thread_id, count)
                futures.append(future)
            
            # 收集结果
            for future in as_completed(futures):
                batch_data = future.result()
                all_data.extend(batch_data)
        
        elapsed = time.time() - start_time
        print(f"并发数据生成完成，共 {len(all_data)} 条数据，总耗时 {elapsed:.2f} 秒")
        return all_data
    
    def _generate_single_user(self, index: int) -> Dict:
        """生成单个用户数据"""
        return {
            "user_id": f"CONC_USER_{str(index+1).zfill(8)}",
            "username": f"concuser{index+1}",
            "email": f"concuser{index+1}@example.com",
            "status": "active"
        }
    
    def _generate_single_order