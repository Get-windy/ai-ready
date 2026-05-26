#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批次管理模块并发压力测试数据生成器
生成不同并发场景下的测试数据
"""

import random
import json
import csv
import os
from datetime import datetime, timedelta
from typing import List, Dict, Any
import argparse


class BatchTestDataGenerator:
    """批次测试数据生成器"""
    
    def __init__(self, base_batch_count: int = 10000):
        """
        初始化生成器
        
        Args:
            base_batch_count: 基础批次数据数量
        """
        self.base_batch_count = base_batch_count
        self.batch_types = ['PRODUCTION', 'QUALITY', 'SHIPPING', 'STORAGE', 'RAW_MATERIAL']
        self.statuses = ['CREATED', 'PROCESSING', 'QUALITY_CHECK', 'COMPLETED', 'CANCELLED']
        self.product_categories = ['ELECTRONICS', 'MECHANICAL', 'CHEMICAL', 'FOOD', 'PHARMACEUTICAL']
        self.supplier_regions = ['EAST', 'WEST', 'SOUTH', 'NORTH', 'CENTRAL']
        
    def generate_batch_data(self, count: int) -> List[Dict[str, Any]]:
        """
        生成批次测试数据
        
        Args:
            count: 生成批次数量
            
        Returns:
            批次数据列表
        """
        batches = []
        start_date = datetime.now() - timedelta(days=365)  # 一年内的数据
        
        for i in range(count):
            created_date = start_date + timedelta(days=random.randint(0, 365))
            updated_date = created_date + timedelta(hours=random.randint(1, 500))
            
            batch_type = random.choice(self.batch_types)
            status = random.choice(self.statuses)
            quantity = random.randint(100, 10000)
            unit_price = round(random.uniform(10.0, 1000.0), 2)
            total_amount = round(quantity * unit_price, 2)
            
            batch = {
                "batch_id": f"BATCH-{100000 + i:06d}",
                "batch_number": f"BT{created_date.strftime('%Y%m%d')}-{i:04d}",
                "batch_type": batch_type,
                "status": status,
                "created_at": created_date.isoformat(),
                "updated_at": updated_date.isoformat(),
                "supplier_id": f"SUP-{random.randint(1000, 9999):04d}",
                "supplier_name": f"供应商{random.randint(1, 100)}有限公司",
                "supplier_region": random.choice(self.supplier_regions),
                "product_id": f"PROD-{random.randint(10000, 99999):05d}",
                "product_name": f"产品{random.randint(1, 500)}",
                "product_category": random.choice(self.product_categories),
                "quantity": quantity,
                "unit": random.choice(['件', '千克', '升', '米']),
                "unit_price": unit_price,
                "total_amount": total_amount,
                "currency": "CNY",
                "contract_id": f"CONTRACT-{random.randint(100, 999):03d}",
                "warehouse_id": f"WH-{random.randint(1, 50):02d}",
                "quality_score": round(random.uniform(0.8, 1.0), 2),
                "notes": f"测试批次数据 #{i+1} - {batch_type} - {status}",
                "tags": self._generate_tags(batch_type, status)
            }
            
            # 添加特定业务字段
            if batch_type == 'PRODUCTION':
                batch["production_line"] = f"LINE-{random.randint(1, 10)}"
                batch["shift"] = random.choice(['DAY', 'NIGHT'])
            elif batch_type == 'QUALITY':
                batch["inspector_id"] = f"INSP-{random.randint(100, 999)}"
                batch["inspection_date"] = (created_date + timedelta(days=random.randint(1, 7))).isoformat()
            elif batch_type == 'SHIPPING':
                batch["carrier"] = random.choice(['顺丰', '中通', '圆通', 'EMS'])
                batch["tracking_number"] = f"TRK{random.randint(1000000000, 9999999999)}"
            
            batches.append(batch)
        
        return batches
    
    def _generate_tags(self, batch_type: str, status: str) -> List[str]:
        """生成批次标签"""
        tags = [batch_type, status]
        
        # 基于状态的标签
        if status in ['PROCESSING', 'QUALITY_CHECK']:
            tags.append('IN_PROGRESS')
        elif status == 'COMPLETED':
            tags.append('CLOSED')
        elif status == 'CANCELLED':
            tags.append('ABORTED')
            
        # 基于类型的标签
        if batch_type in ['PRODUCTION', 'RAW_MATERIAL']:
            tags.append('MANUFACTURING')
        elif batch_type in ['QUALITY', 'SHIPPING']:
            tags.append('LOGISTICS')
            
        # 随机添加业务标签
        business_tags = ['URGENT', 'VIP', 'BULK', 'EXPORT', 'DOMESTIC', 'SAMPLE']
        if random.random() > 0.7:  # 30%概率添加业务标签
            tags.append(random.choice(business_tags))
            
        return tags
    
    def generate_concurrent_test_data(self, scenario_name: str, user_count: int) -> Dict[str, Any]:
        """
        生成并发测试数据
        
        Args:
            scenario_name: 场景名称
            user_count: 用户数量
            
        Returns:
            并发测试数据
        """
        print(f"生成 {scenario_name} 测试数据，用户数: {user_count}")
        
        # 生成基础批次数据
        base_batches = self.generate_batch_data(self.base_batch_count)
        batch_ids = [batch["batch_id"] for batch in base_batches]
        
        # 生成用户数据
        users = []
        roles_distribution = {'ADMIN': 0.1, 'MANAGER': 0.2, 'OPERATOR': 0.4, 'VIEWER': 0.3}
        
        for user_id in range(1, user_count + 1):
            # 确定用户角色
            role_choice = random.random()
            cumulative = 0
            user_role = 'VIEWER'  # 默认
            
            for role, prob in roles_distribution.items():
                cumulative += prob
                if role_choice <= cumulative:
                    user_role = role
                    break
            
            # 确定用户操作频率
            if user_role == 'ADMIN':
                operation_freq = random.randint(5, 15)  # 管理员操作频繁
            elif user_role == 'MANAGER':
                operation_freq = random.randint(3, 10)  # 经理中等频率
            elif user_role == 'OPERATOR':
                operation_freq = random.randint(8, 20)  # 操作员高频率
            else:
                operation_freq = random.randint(1, 5)   # 查看员低频率
            
            # 分配批次（不同角色看到不同批次）
            if user_role in ['ADMIN', 'MANAGER']:
                assigned_batches = random.sample(batch_ids, random.randint(20, 50))
            elif user_role == 'OPERATOR':
                assigned_batches = random.sample(batch_ids, random.randint(10, 30))
            else:
                assigned_batches = random.sample(batch_ids, random.randint(5, 15))
            
            user_data = {
                "user_id": f"TEST-USER-{user_id:04d}",
                "username": f"tester{user_id:04d}",
                "role": user_role,
                "department": random.choice(['生产部', '质量部', '物流部', '采购部', '销售部']),
                "assigned_batches": assigned_batches,
                "operation_frequency": operation_freq,  # 操作频率（次/分钟）
                "preferred_operations": self._get_preferred_operations(user_role),
                "test_weight": round(random.uniform(0.5, 2.0), 2)  # 测试权重
            }
            users.append(user_data)
        
        # 生成测试场景配置
        test_scenario = {
            "scenario_name": scenario_name,
            "scenario_type": "CONCURRENT_PRESSURE_TEST",
            "user_count": user_count,
            "batch_count": len(base_batches),
            "test_start_time": datetime.now().isoformat(),
            "expected_duration_minutes": self._get_expected_duration(user_count),
            "target_throughput": self._calculate_target_throughput(user_count),
            "batches": base_batches,
            "users": users,
            "test_config": self._generate_test_config(scenario_name, user_count)
        }
        
        return test_scenario
    
    def _get_preferred_operations(self, role: str) -> List[str]:
        """获取用户偏好的操作类型"""
        operations_map = {
            'ADMIN': ['查询', '创建', '修改', '删除', '审批', '导出'],
            'MANAGER': ['查询', '审批', '统计', '导出', '监控'],
            'OPERATOR': ['查询', '创建', '修改', '导入', '处理'],
            'VIEWER': ['查询', '查看', '导出']
        }
        return operations_map.get(role, ['查询'])
    
    def _get_expected_duration(self, user_count: int) -> int:
        """计算预期测试持续时间（分钟）"""
        if user_count <= 100:
            return 30  # 30分钟
        elif user_count <= 500:
            return 45  # 45分钟
        elif user_count <= 1000:
            return 60  # 60分钟
        else:
            return 90  # 90分钟
    
    def _calculate_target_throughput(self, user_count: int) -> Dict[str, float]:
        """计算目标吞吐量"""
        base_tps = user_count * 0.5  # 假设每个用户平均0.5 TPS
        
        return {
            "min_tps": round(base_tps * 0.7, 2),
            "target_tps": round(base_tps, 2),
            "max_tps": round(base_tps * 1.3, 2),
            "error_rate_threshold": 0.01 if user_count <= 500 else 0.02,
            "response_time_p95_threshold_ms": 2000 if user_count <= 500 else 3000
        }
    
    def _generate_test_config(self, scenario_name: str, user_count: int) -> Dict[str, Any]:
        """生成测试配置"""
        config = {
            "scenario_name": scenario_name,
            "concurrent_users": user_count,
            "ramp_up_period_seconds": min(300, user_count * 2),  # 预热时间
            "steady_state_minutes": self._get_expected_duration(user_count),
            "ramp_down_period_seconds": 60,
            "sampling_interval_seconds": 5,
            "think_time_ms": {
                "min": 1000,
                "max": 5000
            },
            "test_data": {
                "batch_data_file": f"batch_data_{scenario_name}.json",
                "user_data_file": f"user_data_{scenario_name}.json",
                "test_script": f"jmeter_{scenario_name}.jmx"
            },
            "monitoring": {
                "system_metrics": True,
                "application_metrics": True,
                "database_metrics": True,
                "network_metrics": True
            }
        }
        
        # 根据场景调整配置
        if "1000并发" in scenario_name or "2000并发" in scenario_name:
            config["ramp_up_period_seconds"] = 600  # 高并发场景需要更长预热
            config["sampling_interval_seconds"] = 2  # 更频繁采样
            config["monitoring"]["detailed_logging"] = True
        
        return config
    
    def save_test_data(self, test_data: Dict[str, Any], output_dir: str = "test_data"):
        """
        保存测试数据到文件
        
        Args:
            test_data: 测试数据
            output_dir: 输出目录
        """
        os.makedirs(output_dir, exist_ok=True)
        
        scenario_name = test_data["scenario_name"]
        safe_name = scenario_name.replace(" ", "_").replace("并发", "concurrent")
        
        # 保存完整测试数据
        json_file = os.path.join(output_dir, f"{safe_name}_full.json")
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump(test_data, f, indent=2, ensure_ascii=False)
        print(f"保存完整测试数据到: {json_file}")
        
        # 分别保存批次数据和用户数据
        batches_file = os.path.join(output_dir, f"{safe_name}_batches.json")
        with open(batches_file, "w", encoding="utf-8") as f:
            json.dump(test_data["batches"], f, indent=2, ensure_ascii=False)
        
        users_file = os.path.join(output_dir, f"{safe_name}_users.json")
        with open(users_file, "w", encoding="utf-8") as f:
            json.dump(test_data["users"], f, indent=2, ensure_ascii=False)
        
        # 保存为CSV格式（便于JMeter使用）
        self._save_as_csv(test_data, output_dir, safe_name)
        
        # 生成测试配置文件
        config_file = os.path.join(output_dir, f"{safe_name}_config.json")
        with open(config_file, "w", encoding="utf-8") as f:
            json.dump(test_data["test_config"], f, indent=2, ensure_ascii=False)
        
        print(f"测试数据生成完成: {scenario_name}")
        print(f"  批次数据: {len(test_data['batches'])} 条")
        print(f"  用户数据: {len(test_data['users'])} 条")
        print(f"  目标TPS: {test_data['target_throughput']['target_tps']}")
    
    def _save_as_csv(self, test_data: Dict[str, Any], output_dir: str, safe_name: str):
        """保存为CSV格式"""
        # 批次数据CSV
        batches_csv = os.path.join(output_dir, f"{safe_name}_batches.csv")
        if test_data["batches"]:
            fieldnames = test_data["batches"][0].keys()
            with open(batches_csv, "w", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(f, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerows(test_data["batches"])
        
        # 用户数据CSV（简化版）
        users_csv = os.path.join(output_dir, f"{safe_name}_users.csv")
        if test_data["users"]:
            # 只保存关键用户字段
            simplified_users = []
            for user in test_data["users"]:
                simplified = {
                    "user_id": user["user_id"],
                    "username": user["username"],
                    "role": user["role"],
                    "department": user["department"],
                    "operation_frequency": user["operation_frequency"],
                    "assigned_batches_count": len(user["assigned_batches"])
                }
                simplified_users.append(simplified)
            
            fieldnames = simplified_users[0].keys()
            with open(users_csv, "w", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(f, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerows(simplified_users)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="批次管理模块并发压力测试数据生成器")
    parser.add_argument("--output", "-o", default="test_data", help="输出目录")
    parser.add_argument("--batch-count", "-b", type=int, default=10000, help="基础批次数据数量")
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("批次管理模块并发压力测试数据生成器")
    print(f"输出目录: {args.output}")
    print(f"基础批次数据数量: {args.batch_count}")
    print("=" * 60)
    
    # 创建生成器
    generator = BatchTestDataGenerator(base_batch_count=args.batch_count)
    
    # 生成不同并发场景的测试数据
    scenarios = [
        ("100并发场景-日常业务", 100),
        ("300并发场景-中等负载", 300),
        ("500并发场景-峰值业务", 500),
        ("800并发场景-高负载", 800),
        ("1000并发场景-极限负载", 1000),
        ("2000并发场景-压力测试", 2000)
    ]
    
    for scenario_name, user_count in scenarios:
        print(f"\n生成场景: {scenario_name}")
        test_data = generator.generate_concurrent_test_data(scenario_name, user_count)
        generator.save_test_data(test_data, args.output)
    
    print("\n" + "=" * 60)
    print("所有测试数据生成完成！")
    print(f"数据保存在: {os.path.abspath(args.output)}")
    print("=" * 60)


if __name__ == "__main__":
    main()