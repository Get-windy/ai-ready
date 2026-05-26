#!/usr/bin/env python3
"""
批次管理性能测试数据生成脚本
版本: v1.0.0
用途: 生成批次创建、查询、价格计算等性能测试所需的数据
"""

import csv
import json
import random
import datetime
import uuid
from pathlib import Path

class TestDataGenerator:
    def __init__(self, base_dir: str):
        self.base_dir = Path(base_dir)
        self.data_dir = self.base_dir / "data"
        self.data_dir.mkdir(parents=True, exist_ok=True)
        
        # 产品数据
        self.products = [
            {"code": "P001", "name": "智能手机", "category": "电子产品"},
            {"code": "P002", "name": "笔记本电脑", "category": "电子产品"},
            {"code": "P003", "name": "办公桌", "category": "办公家具"},
            {"code": "P004", "name": "办公椅", "category": "办公家具"},
            {"code": "P005", "name": "A4打印纸", "category": "办公耗材"},
            {"code": "P006", "name": "墨盒", "category": "办公耗材"},
            {"code": "P007", "name": "服务器", "category": "IT设备"},
            {"code": "P008", "name": "网络交换机", "category": "IT设备"},
            {"code": "P009", "name": "咖啡机", "category": "生活电器"},
            {"code": "P010", "name": "饮水机", "category": "生活电器"},
        ]
        
        # 批次状态
        self.batch_statuses = ["CREATED", "INBOUND", "OUTBOUND", "QUALITY_CHECK", "COMPLETED", "CANCELLED"]
        
        # 来源类型
        self.source_types = ["PURCHASE", "PRODUCTION", "TRANSFER", "RETURN", "ADJUSTMENT"]
        
    def generate_batch_data(self, count: int = 1000000):
        """生成批次数据"""
        csv_file = self.data_dir / "batch-data.csv"
        
        print(f"生成批次数据: {count} 条")
        print(f"输出文件: {csv_file}")
        
        with open(csv_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            # 写入表头
            writer.writerow([
                'batch_no', 'product_code', 'product_name', 'quantity', 
                'unit_price', 'total_amount', 'status', 'source_type', 'remark'
            ])
            
            for i in range(count):
                product = random.choice(self.products)
                batch_no = f"BATCH-{datetime.datetime.now().strftime('%Y%m%d')}-{i+1:06d}"
                quantity = random.randint(10, 1000)
                unit_price = round(random.uniform(10.0, 1000.0), 2)
                total_amount = round(quantity * unit_price, 2)
                status = random.choice(self.batch_statuses)
                source_type = random.choice(self.source_types)
                remark = f"测试批次数据 {i+1}"
                
                writer.writerow([
                    batch_no, product['code'], product['name'], quantity,
                    unit_price, total_amount, status, source_type, remark
                ])
                
                if (i + 1) % 100000 == 0:
                    print(f"已生成: {i+1} 条")
        
        print(f"批次数据生成完成: {csv_file}")
        return csv_file
    
    def generate_price_rules(self, count: int = 100):
        """生成价格规则数据"""
        json_file = self.data_dir / "price-rules.json"
        
        print(f"生成价格规则: {count} 条")
        
        price_rules = []
        rule_types = ["FIXED", "PERCENTAGE", "TIERED", "DYNAMIC"]
        
        for i in range(count):
            rule = {
                "id": str(uuid.uuid4()),
                "name": f"价格规则 {i+1}",
                "type": random.choice(rule_types),
                "product_codes": random.sample([p['code'] for p in self.products], random.randint(1, 3)),
                "priority": random.randint(1, 10),
                "enabled": random.choice([True, False]),
                "conditions": self._generate_price_conditions(),
                "actions": self._generate_price_actions()
            }
            price_rules.append(rule)
        
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(price_rules, f, ensure_ascii=False, indent=2)
        
        print(f"价格规则生成完成: {json_file}")
        return json_file
    
    def _generate_price_conditions(self):
        """生成价格条件"""
        condition_types = ["QUANTITY", "CUSTOMER_LEVEL", "TIME_PERIOD", "PRODUCT_CATEGORY"]
        
        conditions = []
        for i in range(random.randint(1, 3)):
            condition = {
                "type": random.choice(condition_types),
                "operator": random.choice([">", ">=", "<", "<=", "==", "!="]),
                "value": random.choice(["100", "GOLD", "VIP", "9:00-18:00", "电子产品"])
            }
            conditions.append(condition)
        
        return conditions
    
    def _generate_price_actions(self):
        """生成价格动作"""
        action_types = ["DISCOUNT_PERCENTAGE", "DISCOUNT_FIXED", "SET_PRICE", "ADD_SURCHARGE"]
        
        actions = []
        for i in range(random.randint(1, 2)):
            action = {
                "type": random.choice(action_types),
                "value": str(round(random.uniform(0.1, 0.3), 2)) if random.choice([True, False]) else str(random.randint(10, 100))
            }
            actions.append(action)
        
        return actions
    
    def generate_query_conditions(self, count: int = 1000):
        """生成查询条件数据"""
        json_file = self.data_dir / "query-conditions.json"
        
        print(f"生成查询条件: {count} 条")
        
        query_conditions = []
        
        for i in range(count):
            condition_type = random.choice([
                "SIMPLE", "COMPLEX", "RANGE", "MULTI_FIELD", "FUZZY"
            ])
            
            if condition_type == "SIMPLE":
                condition = {
                    "type": "SIMPLE",
                    "product_code": random.choice([p['code'] for p in self.products]),
                    "status": random.choice(self.batch_statuses)
                }
            elif condition_type == "COMPLEX":
                condition = {
                    "type": "COMPLEX",
                    "product_codes": random.sample([p['code'] for p in self.products], random.randint(1, 3)),
                    "statuses": random.sample(self.batch_statuses, random.randint(1, 3)),
                    "source_types": random.sample(self.source_types, random.randint(1, 2)),
                    "date_range": {
                        "start": (datetime.datetime.now() - datetime.timedelta(days=30)).strftime("%Y-%m-%d"),
                        "end": datetime.datetime.now().strftime("%Y-%m-%d")
                    }
                }
            elif condition_type == "RANGE":
                condition = {
                    "type": "RANGE",
                    "quantity_min": random.randint(10, 100),
                    "quantity_max": random.randint(500, 1000),
                    "amount_min": random.randint(1000, 5000),
                    "amount_max": random.randint(10000, 50000)
                }
            elif condition_type == "MULTI_FIELD":
                condition = {
                    "type": "MULTI_FIELD",
                    "product_code": random.choice([p['code'] for p in self.products]),
                    "source_type": random.choice(self.source_types),
                    "status": random.choice(self.batch_statuses),
                    "date_from": (datetime.datetime.now() - datetime.timedelta(days=7)).strftime("%Y-%m-%d")
                }
            else:  # FUZZY
                condition = {
                    "type": "FUZZY",
                    "keyword": random.choice(["测试", "批次", "产品", "办公", "电子"]),
                    "search_fields": random.sample(["batch_no", "product_name", "remark"], random.randint(1, 3))
                }
            
            condition["id"] = i + 1
            query_conditions.append(condition)
        
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(query_conditions, f, ensure_ascii=False, indent=2)
        
        print(f"查询条件生成完成: {json_file}")
        return json_file
    
    def generate_traceability_data(self, count: int = 1000000):
        """生成追溯关系数据"""
        csv_file = self.data_dir / "traceability-data.csv"
        
        print(f"生成追溯关系数据: {count} 条")
        
        with open(csv_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            # 写入表头
            writer.writerow([
                'batch_id', 'parent_batch_id', 'relation_type', 
                'product_code', 'quantity', 'operation_time', 'operator'
            ])
            
            batch_ids = list(range(1, 1000001))  # 假设有100万个批次
            
            for i in range(count):
                batch_id = random.choice(batch_ids)
                # 随机选择一个父批次（可能为空）
                parent_batch_id = random.choice([random.choice(batch_ids), None])
                relation_types = ["PRODUCTION", "SPLIT", "MERGE", "TRANSFER", "QUALITY"]
                relation_type = random.choice(relation_types)
                product_code = random.choice([p['code'] for p in self.products])
                quantity = random.randint(1, 100)
                operation_time = (datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(0, 365),
                    hours=random.randint(0, 23)
                )).strftime("%Y-%m-%d %H:%M:%S")
                operator = f"operator_{random.randint(1, 100)}"
                
                writer.writerow([
                    batch_id, parent_batch_id, relation_type,
                    product_code, quantity, operation_time, operator
                ])
                
                if (i + 1) % 100000 == 0:
                    print(f"已生成: {i+1} 条")
        
        print(f"追溯关系数据生成完成: {csv_file}")
        return csv_file
    
    def generate_all_data(self):
        """生成所有测试数据"""
        print("开始生成所有测试数据...")
        print("=" * 50)
        
        # 1. 生成批次数据
        batch_data_file = self.generate_batch_data(1000000)  # 100万条
        
        # 2. 生成价格规则
        price_rules_file = self.generate_price_rules(100)  # 100条规则
        
        # 3. 生成查询条件
        query_conditions_file = self.generate_query_conditions(1000)  # 1000个查询条件
        
        # 4. 生成追溯关系数据
        traceability_file = self.generate_traceability_data(1000000)  # 100万条追溯关系
        
        print("=" * 50)
        print("所有测试数据生成完成！")
        print(f"数据目录: {self.data_dir}")
        
        # 生成数据统计报告
        self.generate_data_statistics()
        
        return {
            "batch_data": batch_data_file,
            "price_rules": price_rules_file,
            "query_conditions": query_conditions_file,
            "traceability_data": traceability_file
        }
    
    def generate_data_statistics(self):
        """生成数据统计报告"""
        report_file = self.data_dir / "data-statistics.md"
        
        statistics = {
            "批次数据": {
                "文件": "batch-data.csv",
                "记录数": 1000000,
                "字段": ["batch_no", "product_code", "product_name", "quantity", 
                        "unit_price", "total_amount", "status", "source_type", "remark"],
                "产品种类": len(self.products),
                "批次状态": len(self.batch_statuses),
                "来源类型": len(self.source_types)
            },
            "价格规则": {
                "文件": "price-rules.json",
                "规则数": 100,
                "规则类型": ["FIXED", "PERCENTAGE", "TIERED", "DYNAMIC"],
                "优先级范围": "1-10"
            },
            "查询条件": {
                "文件": "query-conditions.json",
                "条件数": 1000,
                "条件类型": ["SIMPLE", "COMPLEX", "RANGE", "MULTI_FIELD", "FUZZY"]
            },
            "追溯关系": {
                "文件": "traceability-data.csv",
                "记录数": 1000000,
                "字段": ["batch_id", "parent_batch_id", "relation_type", 
                        "product_code", "quantity", "operation_time", "operator"],
                "关系类型": ["PRODUCTION", "SPLIT", "MERGE", "TRANSFER", "QUALITY"]
            }
        }
        
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("# 性能测试数据统计报告\n\n")
            f.write(f"生成时间: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            
            for category, stats in statistics.items():
                f.write(f"## {category}\n\n")
                f.write(f"- **文件**: `{stats['文件']}`\n")
                f.write(f"- **记录数**: {stats['记录数']:,} 条\n")
                
                if '字段' in stats:
                    f.write(f"- **字段**: {', '.join(stats['字段'])}\n")
                
                if '产品种类' in stats:
                    f.write(f"- **产品种类**: {stats['产品种类']} 种\n")
                
                if '规则类型' in stats:
                    f.write(f"- **规则类型**: {', '.join(stats['规则类型'])}\n")
                
                if '条件类型' in stats:
                    f.write(f"- **条件类型**: {', '.join(stats['条件类型'])}\n")
                
                if '关系类型' in stats:
                    f.write(f"- **关系类型**: {', '.join(stats['关系类型'])}\n")
                
                f.write("\n")
        
        print(f"数据统计报告生成完成: {report_file}")
        return report_file

def main():
    """主函数"""
    # 设置基础目录
    base_dir = Path(__file__).parent.parent
    
    print("=" * 50)
    print("批次管理性能测试数据生成工具")
    print("版本: v1.0.0")
    print("=" * 50)
    
    try:
        # 创建数据生成器
        generator = TestDataGenerator(base_dir)
        
        # 生成所有测试数据
        generated_files = generator.generate_all_data()
        
        print("\n生成的文件:")
        for file_type, file_path in generated_files.items():
            print(f"  - {file_type}: {file_path}")
        
        print("\n使用说明:")
        print("1. 将生成的CSV文件用于JMeter CSV Data Set")
        print("2. JSON文件用于API测试参数")
        print("3. 运行性能测试前确保数据已导入数据库")
        
    except Exception as e:
        print(f"数据生成失败: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())