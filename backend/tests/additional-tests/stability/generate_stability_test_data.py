#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
稳定性测试数据生成脚本
用于生成稳定性测试、性能测试和压测测试所需的各种测试数据
"""

import json
import random
import datetime
import hashlib
from typing import List, Dict, Any
import uuid


class StabilityTestDataGenerator:
    """稳定性测试数据生成器"""
    
    def __init__(self):
        self.departments = ["IT部", "行政部", "人力资源部", "财务部", "市场部", "销售部", "研发部"]
        self.users = ["张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"]
        self.suppliers = ["阿里云", "腾讯云", "华为云", "微软", "IBM", "Oracle", "新供应商A", "新供应商B"]
        self.items = ["服务器", "笔记本电脑", "办公用品", "软件许可", "云服务", "咨询服务", "培训服务"]
        
        # 模拟订单数据
        self.orders = []
        for i in range(100):
            order = {
                "id": f"ORD-{uuid.uuid4().hex[:8]}",
                "user_id": f"USER-{random.randint(1, 1000):04d}",
                "product": random.choice(self.items),
                "quantity": random.randint(1, 10),
                "amount": round(random.uniform(100, 10000), 2),
                "status": random.choice(["pending", "processing", "completed", "cancelled"]),
                "created_at": (datetime.datetime.now() - datetime.timedelta(days=random.randint(1, 30))).isoformat()
            }
            self.orders.append(order)
        
        # 模拟用户数据
        self.users_data = []
        for i in range(500):
            user = {
                "id": f"USER-{i:04d}",
                "name": f"用户{i}",
                "email": f"user{i}@example.com",
                "department": random.choice(self.departments),
                "role": random.choice(["user", "admin", "manager"]),
                "created_at": (datetime.datetime.now() - datetime.timedelta(days=random.randint(1, 180))).isoformat()
            }
            self.users_data.append(user)
    
    def generate_stability_test_data(self, count: int = 1000) -> Dict:
        """生成稳定性测试数据集"""
        stability_data = {
            "test_type": "stability",
            "test_duration_hours": 24,
            "test_scenarios": [],
            "test_data": []
        }
        
        # 生成测试场景
        scenarios = [
            {
                "name": "continuous_availability_test",
                "description": "持续可用性测试 - 模拟24小时持续请求",
                "request_rate": "100 RPS",
                "duration": "24 hours"
            },
            {
                "name": "crash_recovery_test",
                "description": "崩溃恢复测试 - 模拟服务崩溃后的恢复",
                "crash_frequency": "每4小时模拟一次",
                "recovery_timeout": "60秒"
            },
            {
                "name": "resource_leak_test",
                "description": "资源泄漏检测 - 模拟长时间运行的资源使用",
                "monitoring_items": ["memory", "cpu", "threads", "connections"]
            },
            {
                "name": "deadlock_detection_test",
                "description": "死锁检测 - 模拟并发事务死锁场景",
                "concurrent_transactions": 100,
                "lock_timeout": "30秒"
            }
        ]
        
        stability_data["test_scenarios"] = scenarios
        
        # 生成稳定性测试数据
        for i in range(count):
            test_data = {
                "request_id": f"REQ-{uuid.uuid4().hex[:12]}",
                "timestamp": (datetime.datetime.now() - datetime.timedelta(hours=24-i/100)).isoformat(),
                "test_scenario": random.choice([s["name"] for s in scenarios]),
                "endpoint": self._generate_endpoint(),
                "method": random.choice(["GET", "POST", "PUT", "DELETE"]),
                "request_data": self._generate_request_data(),
                "expected_response": {
                    "status_code": 200,
                    "response_time_ms": random.randint(50, 500)
                }
            }
            stability_data["test_data"].append(test_data)
        
        return stability_data
    
    def generate_stress_test_data(self, user_count: int = 1000) -> Dict:
        """生成压力测试数据"""
        stress_data = {
            "test_type": "stress",
            "concurrent_users": user_count,
            "test_scenarios": [],
            "load_patterns": []
        }
        
        # 生成压力测试场景
        scenarios = [
            {
                "name": "peak_load_test",
                "description": "峰值负载测试",
                "concurrent_users": user_count,
                "duration_minutes": 30,
                "rps_target": user_count * 10
            },
            {
                "name": "soak_test",
                "description": "耐力测试 - 长时间高负载",
                "concurrent_users": int(user_count * 0.8),
                "duration_minutes": 120,
                "rps_target": user_count * 5
            },
            {
                "name": "spike_test",
                "description": "突增负载测试",
                "concurrent_users": user_count,
                "spike_users": int(user_count * 0.2),
                "spike_duration_seconds": 60,
                "rps_target": user_count * 20
            }
        ]
        
        stress_data["test_scenarios"] = scenarios
        
        # 生成负载模式
        patterns = [
            {
                "name": "constant_load",
                "description": "恒定负载",
                "concurrent_users": int(user_count * 0.5),
                "rps": int(user_count * 2)
            },
            {
                "name": "gradual_increase",
                "description": "逐步递增负载",
                "start_users": int(user_count * 0.1),
                "end_users": user_count,
                "increment_interval_seconds": 60
            },
            {
                "name": "intermittent_load",
                "description": "间歇性负载",
                "active_duration_minutes": 10,
                "idle_duration_minutes": 5
            }
        ]
        
        stress_data["load_patterns"] = patterns
        
        return stress_data
    
    def generate_boundary_test_data(self) -> Dict:
        """生成边界测试数据"""
        boundary_data = {
            "test_type": "boundary",
            "test_categories": [],
            "test_cases": []
        }
        
        # 生成测试类别
        categories = [
            {
                "name": "input_validation",
                "description": "输入验证边界测试",
                "test_cases": [
                    {
                        "name": "max_length",
                        "description": "最大长度边界",
                        "field": "username",
                        "max_length": 50,
                        "test_value": "a" * 50
                    },
                    {
                        "name": "min_length",
                        "description": "最小长度边界",
                        "field": "username",
                        "min_length": 3,
                        "test_value": "ab"
                    },
                    {
                        "name": "max_value",
                        "description": "最大值边界",
                        "field": "age",
                        "max_value": 150,
                        "test_value": 150
                    },
                    {
                        "name": "min_value",
                        "description": "最小值边界",
                        "field": "age",
                        "min_value": 0,
                        "test_value": -1
                    }
                ]
            },
            {
                "name": "performance_threshold",
                "description": "性能阈值边界测试",
                "test_cases": [
                    {
                        "name": "max_concurrent_users",
                        "description": "最大并发用户数",
                        "threshold": 10000,
                        "test_value": 10000
                    },
                    {
                        "name": "max_response_time",
                        "description": "最大响应时间",
                        "threshold_ms": 5000,
                        "test_value": 5000
                    },
                    {
                        "name": "max_data_size",
                        "description": "最大数据量",
                        "threshold_mb": 100,
                        "test_value": 100
                    }
                ]
            },
            {
                "name": "resource_limits",
                "description": "资源限制边界测试",
                "test_cases": [
                    {
                        "name": "max_memory_usage",
                        "description": "最大内存使用",
                        "threshold_mb": 8000,
                        "test_value": 8000
                    },
                    {
                        "name": "max_cpu_usage",
                        "description": "最大CPU使用",
                        "threshold_percent": 95,
                        "test_value": 95
                    },
                    {
                        "name": "max_connections",
                        "description": "最大连接数",
                        "threshold": 10000,
                        "test_value": 10000
                    }
                ]
            }
        ]
        
        boundary_data["test_categories"] = categories
        
        # 生成测试用例
        for category in categories:
            for case in category["test_cases"]:
                test_case = {
                    "category": category["name"],
                    "name": case["name"],
                    "description": case["description"],
                    "boundary_value": case.get("test_value"),
                    "expected_behavior": "should_handlegracefully",
                    "pass_criteria": "system_stable"
                }
                boundary_data["test_cases"].append(test_case)
        
        return boundary_data
    
    def generate_large_volume_test_data(self, record_count: int = 10000) -> Dict:
        """生成大数据量测试数据"""
        large_volume_data = {
            "test_type": "large_volume",
            "record_count": record_count,
            "data_sets": []
        }
        
        # 生成数据集
        datasets = [
            {
                "name": "user_data",
                "description": "用户数据 - 大量用户记录",
                "record_count": 100000,
                "fields": ["id", "name", "email", "department", "created_at"]
            },
            {
                "name": "order_data",
                "description": "订单数据 - 大量订单记录",
                "record_count": 500000,
                "fields": ["id", "user_id", "product", "quantity", "amount", "status"]
            },
            {
                "name": "log_data",
                "description": "日志数据 - 大量日志记录",
                "record_count": 1000000,
                "fields": ["id", "timestamp", "level", "message", "user_id"]
            }
        ]
        
        large_volume_data["data_sets"] = datasets
        
        # 生成大数据量测试场景
        scenarios = [
            {
                "name": "batch_processing",
                "description": "批量处理测试",
                "batch_size": 10000,
                "operations": ["select", "insert", "update", "delete"]
            },
            {
                "name": "query_performance",
                "description": "查询性能测试",
                "query_types": ["range_query", "join_query", "aggregate_query"]
            },
            {
                "name": "index_efficiency",
                "description": "索引效率测试",
                "indexed_fields": ["user_id", "created_at", "status"]
            }
        ]
        
        large_volume_data["test_scenarios"] = scenarios
        
        return large_volume_data
    
    def generate_concurrent_execution_data(self, thread_count: int = 100) -> Dict:
        """生成并发执行测试数据"""
        concurrent_data = {
            "test_type": "concurrent",
            "thread_count": thread_count,
            "test_scenarios": [],
            "deadlock_scenarios": []
        }
        
        # 生成并发测试场景
        scenarios = [
            {
                "name": "concurrent_read",
                "description": "并发读取测试",
                "threads": thread_count,
                "operation": "SELECT",
                "duration_minutes": 10
            },
            {
                "name": "concurrent_write",
                "description": "并发写入测试",
                "threads": thread_count,
                "operation": "INSERT",
                "duration_minutes": 10
            },
            {
                "name": "mixed_concurrent",
                "description": "混合并发测试",
                "threads": thread_count,
                "operations": ["SELECT", "INSERT", "UPDATE", "DELETE"],
                "duration_minutes": 15
            }
        ]
        
        concurrent_data["test_scenarios"] = scenarios
        
        # 生成死锁场景
        deadlock_scenarios = [
            {
                "name": "two_table_deadlock",
                "description": "两表死锁场景",
                "threads": 2,
                "table1": "users",
                "table2": "orders",
                "lock_order1": ["table1", "table2"],
                "lock_order2": ["table2", "table1"],
                "timeout_seconds": 30
            },
            {
                "name": "multiple_table_deadlock",
                "description": "多表死锁场景",
                "threads": 3,
                "tables": ["users", "orders", "products"],
                "lock_order1": ["users", "orders", "products"],
                "lock_order2": ["orders", "products", "users"],
                "lock_order3": ["products", "users", "orders"],
                "timeout_seconds": 30
            }
        ]
        
        concurrent_data["deadlock_scenarios"] = deadlock_scenarios
        
        return concurrent_data
    
    def _generate_endpoint(self) -> str:
        """生成API端点"""
        endpoints = [
            "/api/v1/users/login",
            "/api/v1/users/register",
            "/api/v1/orders",
            "/api/v1/orders/{id}",
            "/api/v1/products",
            "/api/v1/inventory/check",
            "/api/v1/inventory/deduct",
            "/api/v1/analytics/metrics",
            "/api/v1/notifications",
            "/api/v1/config"
        ]
        return random.choice(endpoints)
    
    def _generate_request_data(self) -> Dict:
        """生成请求数据"""
        request_types = [
            {"type": "login", "username": f"user{random.randint(1, 1000)}", "password": "password123"},
            {"type": "order_create", "user_id": f"USER-{random.randint(1, 1000):04d}", "product": random.choice(self.items), "quantity": random.randint(1, 10)},
            {"type": "query", "user_id": f"USER-{random.randint(1, 1000):04d}", "date_range": {"start": (datetime.datetime.now() - datetime.timedelta(days=7)).isoformat(), "end": datetime.datetime.now().isoformat()}}
        ]
        return random.choice(request_types)
    
    def save_test_data(self, data: Dict, filename: str):
        """保存测试数据到文件"""
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"测试数据已保存到: {filename}")
    
    def generate_all_stability_test_data(self):
        """生成所有稳定性测试数据"""
        print("开始生成稳定性测试数据...")
        
        # 生成稳定性测试数据集
        print("1. 生成稳定性测试数据集...")
        stability_data = self.generate_stability_test_data(1000)
        self.save_test_data(stability_data, "stability_test_data.json")
        
        # 生成压力测试数据
        print("2. 生成压力测试数据...")
        stress_data = self.generate_stress_test_data(1000)
        self.save_test_data(stress_data, "stress_test_data.json")
        
        # 生成边界测试数据
        print("3. 生成边界测试数据...")
        boundary_data = self.generate_boundary_test_data()
        self.save_test_data(boundary_data, "boundary_test_data.json")
        
        # 生成大数据量测试数据
        print("4. 生成大数据量测试数据...")
        large_volume_data = self.generate_large_volume_test_data(10000)
        self.save_test_data(large_volume_data, "large_volume_test_data.json")
        
        # 生成并发执行测试数据
        print("5. 生成并发执行测试数据...")
        concurrent_data = self.generate_concurrent_execution_data(100)
        self.save_test_data(concurrent_data, "concurrent_execution_data.json")
        
        # 生成测试数据汇总报告
        print("6. 生成测试数据汇总报告...")
        summary = {
            "generated_at": datetime.datetime.now().isoformat(),
            "data_files": {
                "stability_test_data.json": f"{len(stability_data['test_data'])} 条测试数据",
                "stress_test_data.json": f"{len(stress_data['test_scenarios'])} 个测试场景",
                "boundary_test_data.json": f"{len(boundary_data['test_cases'])} 个测试用例",
                "large_volume_test_data.json": f"{large_volume_data['record_count']} 条记录",
                "concurrent_execution_data.json": f"{len(concurrent_data['deadlock_scenarios'])} 个死锁场景"
            },
            "test_coverage": {
                "stability_test": "24小时持续运行测试场景",
                "stress_test": "峰值/耐力/突增负载测试",
                "boundary_test": "输入/性能/资源边界测试",
                "large_volume_test": "大数据量处理测试",
                "concurrent_test": "并发执行/死锁检测测试"
            }
        }
        
        self.save_test_data(summary, "stability_test_data_summary.json")
        print("稳定性测试数据生成完成！")


def main():
    """主函数"""
    generator = StabilityTestDataGenerator()
    
    print("=" * 60)
    print("稳定性测试数据生成工具")
    print("=" * 60)
    print("1. 生成所有稳定性测试数据")
    print("2. 生成稳定性测试数据集")
    print("3. 生成压力测试数据")
    print("4. 生成边界测试数据")
    print("5. 生成大数据量测试数据")
    print("6. 生成并发执行测试数据")
    print("0. 退出")
    
    try:
        choice = input("请选择操作 (0-6): ").strip()
        
        if choice == "1":
            generator.generate_all_stability_test_data()
        elif choice == "2":
            data = generator.generate_stability_test_data(1000)
            generator.save_test_data(data, "stability_test_data.json")
        elif choice == "3":
            data = generator.generate_stress_test_data(1000)
            generator.save_test_data(data, "stress_test_data.json")
        elif choice == "4":
            data = generator.generate_boundary_test_data()
            generator.save_test_data(data, "boundary_test_data.json")
        elif choice == "5":
            data = generator.generate_large_volume_test_data(10000)
            generator.save_test_data(data, "large_volume_test_data.json")
        elif choice == "6":
            data = generator.generate_concurrent_execution_data(100)
            generator.save_test_data(data, "concurrent_execution_data.json")
        elif choice == "0":
            print("退出程序")
        else:
            print("无效选择")
    except Exception as e:
        print(f"生成测试数据时出错: {e}")


if __name__ == "__main__":
    main()
