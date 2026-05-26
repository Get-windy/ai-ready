#!/usr/bin/env python3
"""
企智连数据同步性能测试数据生成器
生成10万+条测试记录用于性能测试
"""

import json
import random
import uuid
import argparse
from datetime import datetime, timedelta
from typing import List, Dict, Any
import os


def generate_test_record(record_id: int, large_field_size: int = 1000) -> Dict[str, Any]:
    """生成单条测试记录
    
    Args:
        record_id: 记录编号
        large_field_size: 大文本字段大小(字符数)
    
    Returns:
        测试记录字典
    """
    created_at = datetime.now() - timedelta(
        days=random.randint(0, 365),
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59)
    )
    
    return {
        "id": str(uuid.uuid4()),
        "record_number": record_id,
        "name": f"Performance Test Record {record_id}",
        "description": f"This is a performance test record with ID {record_id} generated for data sync testing",
        "status": random.choice(["active", "inactive", "pending", "suspended"]),
        "category": random.choice(["A", "B", "C", "D", "E"]),
        "priority": random.randint(1, 5),
        "value": round(random.uniform(1.0, 100000.0), 2),
        "quantity": random.randint(1, 10000),
        "weight": round(random.uniform(0.1, 1000.0), 3),
        "created_at": created_at.isoformat(),
        "updated_at": (created_at + timedelta(hours=random.randint(1, 720))).isoformat(),
        "expired_at": (created_at + timedelta(days=random.randint(30, 1095))).isoformat(),
        "metadata": {
            "source": "performance_test",
            "version": "1.0",
            "test_suite": "data_sync_performance",
            "tags": [f"tag_{i}" for i in range(random.randint(1, 10))],
            "attributes": {
                "color": random.choice(["red", "blue", "green", "yellow", "black"]),
                "size": random.choice(["small", "medium", "large"]),
                "region": random.choice(["north", "south", "east", "west"])
            }
        },
        "large_text": "A" * large_field_size,
        "binary_data": None,
        "nested_object": {
            "level1": {
                "level2": {
                    "level3": {
                        "data": f"nested_data_{record_id}",
                        "timestamp": datetime.now().isoformat()
                    }
                }
            }
        },
        "array_data": [random.randint(1, 100) for _ in range(random.randint(5, 20))],
        "coordinates": {
            "lat": round(random.uniform(-90.0, 90.0), 6),
            "lng": round(random.uniform(-180.0, 180.0), 6)
        }
    }


def generate_test_data(count: int, output_file: str, large_field_size: int = 1000) -> Dict[str, Any]:
    """生成指定数量的测试数据
    
    Args:
        count: 记录数量
        output_file: 输出文件路径
        large_field_size: 大文本字段大小
    
    Returns:
        生成结果统计
    """
    print(f"开始生成 {count} 条测试记录...")
    start_time = datetime.now()
    
    records = []
    batch_size = 1000
    
    for i in range(count):
        record = generate_test_record(i, large_field_size)
        records.append(record)
        
        # 每1000条记录显示进度
        if (i + 1) % batch_size == 0:
            progress = (i + 1) / count * 100
            print(f"  进度: {i + 1}/{count} ({progress:.1f}%)")
    
    # 保存到文件
    print(f"正在保存到文件: {output_file}")
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(records, f, ensure_ascii=False, indent=None)
    
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    
    # 计算文件大小
    file_size_bytes = os.path.getsize(output_file)
    file_size_mb = file_size_bytes / (1024 * 1024)
    
    # 计算平均记录大小
    avg_record_size = file_size_bytes / count
    
    result = {
        "total_records": count,
        "file_path": output_file,
        "file_size_bytes": file_size_bytes,
        "file_size_mb": round(file_size_mb, 2),
        "avg_record_size_bytes": round(avg_record_size, 2),
        "generation_time_seconds": round(duration, 2),
        "generation_rate": round(count / duration, 2),
        "timestamp": datetime.now().isoformat()
    }
    
    print(f"\n生成完成!")
    print(f"  总记录数: {count}")
    print(f"  文件大小: {file_size_mb:.2f} MB")
    print(f"  平均记录大小: {avg_record_size:.2f} bytes")
    print(f"  生成时间: {duration:.2f} 秒")
    print(f"  生成速率: {result['generation_rate']:.2f} 记录/秒")
    
    return result


def validate_test_data(records: List[Dict[str, Any]]) -> Dict[str, Any]:
    """验证测试数据完整性
    
    Args:
        records: 测试记录列表
    
    Returns:
        验证结果统计
    """
    print("\n开始验证测试数据...")
    
    total_records = len(records)
    unique_ids = len(set(r["id"] for r in records))
    
    # 验证必填字段
    required_fields = ["id", "name", "status", "created_at", "updated_at"]
    valid_records = 0
    
    for record in records:
        has_all_fields = all(field in record for field in required_fields)
        if has_all_fields:
            valid_records += 1
    
    # 验证状态字段
    valid_status_values = ["active", "inactive", "pending", "suspended"]
    valid_status_count = sum(1 for r in records if r.get("status") in valid_status_values)
    
    # 验证类别字段
    valid_categories = ["A", "B", "C", "D", "E"]
    valid_category_count = sum(1 for r in records if r.get("category") in valid_categories)
    
    # 计算平均记录大小
    total_size = sum(len(json.dumps(r)) for r in records)
    avg_record_size = total_size / total_records
    
    result = {
        "total_records": total_records,
        "unique_ids": unique_ids,
        "id_uniqueness_rate": round(unique_ids / total_records * 100, 2),
        "valid_records": valid_records,
        "valid_record_rate": round(valid_records / total_records * 100, 2),
        "valid_status_count": valid_status_count,
        "valid_status_rate": round(valid_status_count / total_records * 100, 2),
        "valid_category_count": valid_category_count,
        "valid_category_rate": round(valid_category_count / total_records * 100, 2),
        "avg_record_size_bytes": round(avg_record_size, 2),
        "total_data_size_mb": round(total_size / (1024 * 1024), 2)
    }
    
    print(f"  总记录数: {total_records}")
    print(f"  唯一ID数: {unique_ids} (唯一率: {result['id_uniqueness_rate']}%)")
    print(f"  有效记录数: {valid_records} (有效率: {result['valid_record_rate']}%)")
    print(f"  有效状态数: {valid_status_count}")
    print(f"  有效类别数: {valid_category_count}")
    print(f"  平均记录大小: {avg_record_size:.2f} bytes")
    
    return result


def generate_incremental_data(base_count: int, increment_count: int, output_file: str):
    """生成增量测试数据
    
    Args:
        base_count: 基础数据量
        increment_count: 增量数据量
        output_file: 输出文件路径
    """
    print(f"\n生成增量测试数据...")
    print(f"  基础数据: {base_count} 条")
    print(f"  增量数据: {increment_count} 条")
    
    # 生成基础数据
    base_records = [generate_test_record(i) for i in range(base_count)]
    
    # 生成增量数据（模拟新增、更新、删除）
    incremental_records = {
        "inserts": [],
        "updates": [],
        "deletes": []
    }
    
    # 新增记录
    for i in range(increment_count):
        record = generate_test_record(base_count + i)
        incremental_records["inserts"].append(record)
    
    # 更新记录（从基础数据中随机选择）
    update_count = int(increment_count * 0.5)  # 50%更新
    update_indices = random.sample(range(base_count), update_count)
    for idx in update_indices:
        record = base_records[idx].copy()
        record["updated_at"] = datetime.now().isoformat()
        record["name"] = f"{record['name']} - Updated"
        incremental_records["updates"].append(record)
    
    # 删除记录（从基础数据中随机选择）
    delete_count = int(increment_count * 0.1)  # 10%删除
    delete_indices = random.sample(range(base_count), delete_count)
    for idx in delete_indices:
        incremental_records["deletes"].append(base_records[idx]["id"])
    
    # 保存增量数据
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(incremental_records, f, ensure_ascii=False, indent=2)
    
    file_size_mb = os.path.getsize(output_file) / (1024 * 1024)
    
    print(f"\n增量数据生成完成!")
    print(f"  新增: {len(incremental_records['inserts'])} 条")
    print(f"  更新: {len(incremental_records['updates'])} 条")
    print(f"  删除: {len(incremental_records['deletes'])} 条")
    print(f"  文件大小: {file_size_mb:.2f} MB")
    
    return {
        "base_count": base_count,
        "insert_count": len(incremental_records["inserts"]),
        "update_count": len(incremental_records["updates"]),
        "delete_count": len(incremental_records["deletes"]),
        "total_operations": sum([
            len(incremental_records["inserts"]),
            len(incremental_records["updates"]),
            len(incremental_records["deletes"])
        ]),
        "file_path": output_file,
        "file_size_mb": round(file_size_mb, 2)
    }


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='生成数据同步性能测试数据')
    parser.add_argument('--count', type=int, default=100000, help='记录数量 (默认: 100000)')
    parser.add_argument('--output', type=str, default='test_data.json', help='输出文件路径')
    parser.add_argument('--large-field-size', type=int, default=1000, help='大文本字段大小 (默认: 1000)')
    parser.add_argument('--validate', action='store_true', help='验证生成的数据')
    parser.add_argument('--incremental', action='store_true', help='生成增量数据')
    parser.add_argument('--base-count', type=int, default=100000, help='基础数据量 (增量模式)')
    parser.add_argument('--increment-count', type=int, default=10000, help='增量数据量 (增量模式)')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("企智连数据同步性能测试数据生成器")
    print("=" * 60)
    
    if args.incremental:
        # 生成增量数据
        result = generate_incremental_data(
            args.base_count,
            args.increment_count,
            args.output
        )
    else:
        # 生成全量数据
        result = generate_test_data(
            args.count,
            args.output,
            args.large_field_size
        )
        
        # 验证数据（如果需要）
        if args.validate:
            with open(args.output, 'r', encoding='utf-8') as f:
                records = json.load(f)
            validation_result = validate_test_data(records)
            result["validation"] = validation_result
    
    # 保存生成报告
    report_file = args.output.replace('.json', '_report.json')
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    
    print(f"\n生成报告已保存: {report_file}")
    print("=" * 60)


if __name__ == '__main__':
    main()
