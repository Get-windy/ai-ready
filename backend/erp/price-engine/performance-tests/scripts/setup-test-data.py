#!/usr/bin/env python3
"""
价格策略性能测试数据生成脚本
用于生成不同规模的测试数据
"""

import json
import random
import argparse
from datetime import datetime, timedelta
from typing import List, Dict, Any


class TestDataGenerator:
    """测试数据生成器"""
    
    def __init__(self):
        self.products = [f"P{str(i).zfill(3)}" for i in range(1, 1001)]
        self.customers = [f"CUST{str(i).zfill(3)}" for i in range(1, 501)]
        self.customer_levels = ["BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"]
        self.condition_types = ["CUSTOMER_LEVEL", "QUANTITY", "TIME", "PRODUCT", "REGION"]
        self.action_types = ["FIXED_DISCOUNT", "PERCENTAGE_DISCOUNT", "SET_PRICE", "FREE_SHIPPING"]
        self.regions = ["NORTH", "SOUTH", "EAST", "WEST", "CENTRAL"]
        
    def generate_strategy(self, strategy_id: int) -> Dict[str, Any]:
        """生成单个价格策略"""
        condition_type = random.choice(self.condition_types)
        
        if condition_type == "CUSTOMER_LEVEL":
            condition_value = random.choice(self.customer_levels)
        elif condition_type == "QUANTITY":
            condition_value = str(random.randint(10, 1000))
        elif condition_type == "TIME":
            quarter = random.choice(["Q1", "Q2", "Q3", "Q4"])
            condition_value = quarter
        elif condition_type == "PRODUCT":
            condition_value = random.choice(self.products[:50])
        else:  # REGION
            condition_value = random.choice(self.regions)
        
        action_type = random.choice(self.action_types)
        
        if action_type == "FIXED_DISCOUNT":
            action_value = random.randint(5, 100)
        elif action_type == "PERCENTAGE_DISCOUNT":
            action_value = random.randint(5, 50)
        elif action_type == "SET_PRICE":
            action_value = random.randint(50, 1000)
        else:  # FREE_SHIPPING
            action_value = True
        
        start_date = datetime.now()
        end_date = start_date + timedelta(days=random.randint(30, 365))
        
        applicable_products = random.sample(self.products[:100], random.randint(1, 10))
        
        return {
            "strategyId": f"PRICE-{strategy_id:06d}",
            "strategyName": f"策略-{strategy_id}",
            "conditionType": condition_type,
            "conditionValue": condition_value,
            "actionType": action_type,
            "actionValue": action_value,
            "priority": random.randint(1, 1000),
            "validFrom": start_date.strftime("%Y-%m-%d"),
            "validTo": end_date.strftime("%Y-%m-%d"),
            "applicableProducts": applicable_products,
            "description": f"这是第{strategy_id}个价格策略，用于性能测试",
            "createdBy": "system",
            "createdAt": datetime.now().isoformat(),
            "updatedAt": datetime.now().isoformat()
        }
    
    def generate_price_request(self, request_id: int) -> Dict[str, Any]:
        """生成价格计算请求"""
        product_id = random.choice(self.products)
        customer_id = random.choice(self.customers)
        customer_level = random.choice(self.customer_levels)
        
        return {
            "requestId": f"REQ-{request_id:06d}",
            "productId": product_id,
            "customerId": customer_id,
            "customerLevel": customer_level,
            "originalPrice": round(random.uniform(10.0, 1000.0), 2),
            "quantity": random.randint(1, 100),
            "transactionDate": datetime.now().strftime("%Y-%m-%d"),
            "currency": "CNY",
            "region": random.choice(self.regions),
            "channel": random.choice(["ONLINE", "STORE", "MOBILE", "PHONE"])
        }
    
    def generate_batch(self, size: int, data_type: str = "strategy") -> List[Dict[str, Any]]:
        """批量生成数据"""
        data = []
        for i in range(1, size + 1):
            if data_type == "strategy":
                data.append(self.generate_strategy(i))
            else:
                data.append(self.generate_price_request(i))
        return data


def main():
    parser = argparse.ArgumentParser(description="生成价格策略性能测试数据")
    parser.add_argument("--size", type=int, default=1000, help="数据规模")
    parser.add_argument("--type", choices=["strategy", "request"], default="strategy", help="数据类型")
    parser.add_argument("--output", type=str, default="test-data.json", help="输出文件")
    
    args = parser.parse_args()
    
    print(f"开始生成{args.size}条{args.type}测试数据...")
    
    generator = TestDataGenerator()
    data = generator.generate_batch(args.size, args.type)
    
    with open(args.output, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"数据已生成到 {args.output}")
    print(f"总计: {len(data)} 条记录")
    
    # 显示样本数据
    print("\n样本数据:")
    print(json.dumps(data[0], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()