# 测试数据质量保证体系

## 1. 测试数据质量检查规则

### 1.1 数据完整性规则

#### 必填字段检查：
```python
# 必填字段规则定义
REQUIRED_FIELDS = {
    "users": ["user_id", "username", "email", "status"],
    "products": ["product_id", "product_code", "product_name", "category", "unit_price", "status"],
    "orders": ["order_id", "user_id", "order_date", "total_amount", "status"],
    "invoices": ["invoice_id", "invoice_no", "invoice_date", "customer_id", "total_amount", "status"]
}

# 数据完整性检查函数
def check_data_completeness(data: List[Dict], data_type: str) -> Dict:
    """
    检查数据完整性
    """
    results = {
        "data_type": data_type,
        "total_records": len(data),
        "missing_required_fields": [],
        "completeness_rate": 0.0
    }
    
    if data_type not in REQUIRED_FIELDS:
        return results
    
    required_fields = REQUIRED_FIELDS[data_type]
    missing_count = 0
    
    for record in data:
        for field in required_fields:
            if field not in record or record[field] is None or record[field] == "":
                missing_count += 1
                results["missing_required_fields"].append({
                    "record_id": record.get("id", "unknown"),
                    "field": field,
                    "value": record.get(field)
                })
                break  # 只记录每个记录的第一个缺失字段
    
    if len(data) > 0:
        results["completeness_rate"] = round((1 - missing_count / len(data)) * 100, 2)
    
    return results
```

#### 数据关联性检查：
```python
def check_data_relationships(users: List[Dict], orders: List[Dict]) -> Dict:
    """
    检查数据关联性
    """
    results = {
        "total_orders": len(orders),
        "orders_with_valid_users": 0,
        "orders_with_invalid_users": [],
        "orphan_orders": [],
        "relationship_integrity_rate": 0.0
    }
    
    # 创建用户ID集合用于快速查找
    user_ids = {user["user_id"] for user in users}
    
    for order in orders:
        user_id = order.get("user_id")
        if user_id in user_ids:
            results["orders_with_valid_users"] += 1
        else:
            results["orders_with_invalid_users"].append({
                "order_id": order.get("order_id"),
                "user_id": user_id
            })
    
    # 计算关系完整性率
    if len(orders) > 0:
        results["relationship_integrity_rate"] = round(
            results["orders_with_valid_users"] / len(orders) * 100, 2
        )
    
    return results
```

### 1.2 数据准确性规则

#### 数据格式验证：
```python
import re
from datetime import datetime

class DataFormatValidator:
    """数据格式验证器"""
    
    # 正则表达式模式
    PATTERNS = {
        "email": r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
        "phone": r'^1[3-9]\d{9}$',  # 中国手机号
        "id_card": r'^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[1-2]\d|3[0-1])\d{3}[0-9Xx]$',
        "date": r'^\d{4}-\d{2}-\d{2}$',
        "datetime": r'^\d{4}-\d{2}-\d{2}[T\s]\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$',
        "url": r'^https?://[^\s/$.?#].[^\s]*$',
        "ip_address": r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$'
    }
    
    def validate_email(self, email: str) -> bool:
        """验证邮箱格式"""
        return bool(re.match(self.PATTERNS["email"], email))
    
    def validate_phone(self, phone: str) -> bool:
        """验证手机号格式"""
        return bool(re.match(self.PATTERNS["phone"], phone))
    
    def validate_date(self, date_str: str) -> bool:
        """验证日期格式"""
        if not re.match(self.PATTERNS["date"], date_str):
            return False
        
        try:
            datetime.strptime(date_str, "%Y-%m-%d")
            return True
        except ValueError:
            return False
    
    def validate_datetime(self, datetime_str: str) -> bool:
        """验证日期时间格式"""
        if not re.match(self.PATTERNS["datetime"], datetime_str):
            return False
        
        try:
            # 尝试解析ISO格式
            datetime.fromisoformat(datetime_str.replace('Z', '+00:00'))
            return True
        except ValueError:
            return False
    
    def validate_numeric_range(self, value: Any, min_value: float = None, 
                              max_value: float = None) -> bool:
        """验证数值范围"""
        if not isinstance(value, (int, float)):
            try:
                value = float(value)
            except (ValueError, TypeError):
                return False
        
        if min_value is not None and value < min_value:
            return False
        
        if max_value is not None and value > max_value:
            return False
        
        return True
    
    def validate_enum(self, value: Any, allowed_values: List) -> bool:
        """验证枚举值"""
        return value in allowed_values
    
    def validate_record(self, record: Dict, validation_rules: Dict) -> Dict:
        """
        验证单个记录
        """
        validation_results = {
            "record_id": record.get("id", "unknown"),
            "is_valid": True,
            "errors": []
        }
        
        for field, rules in validation_rules.items():
            value = record.get(field)
            
            # 检查必填字段
            if rules.get("required", False) and (value is None or value == ""):
                validation_results["is_valid"] = False
                validation_results["errors"].append(f"字段 '{field}' 是必填字段")
                continue
            
            # 如果字段为空且不是必填，跳过其他验证
            if value is None or value == "":
                continue
            
            # 验证格式
            if "format" in rules:
                format_validator = getattr(self, f"validate_{rules['format']}", None)
                if format_validator and not format_validator(value):
                    validation_results["is_valid"] = False
                    validation_results["errors"].append(
                        f"字段 '{field}' 格式不正确，应为 {rules['format']} 格式"
                    )
            
            # 验证数值范围
            if "min" in rules or "max" in rules:
                if not self.validate_numeric_range(value, rules.get("min"), rules.get("max")):
                    validation_results["is_valid"] = False
                    validation_results["errors"].append(
                        f"字段 '{field}' 值 {value} 超出范围 [{rules.get('min', '无限制')}, {rules.get('max', '无限制')}]"
                    )
            
            # 验证枚举值
            if "enum" in rules:
                if not self.validate_enum(value, rules["enum"]):
                    validation_results["is_valid"] = False
                    validation_results["errors"].append(
                        f"字段 '{field}' 值 {value} 不在允许的值列表中: {rules['enum']}"
                    )
            
            # 验证正则表达式
            if "pattern" in rules:
                if not re.match(rules["pattern"], str(value)):
                    validation_results["is_valid"] = False
                    validation_results["errors"].append(
                        f"字段 '{field}' 值 {value} 不符合正则表达式模式"
                    )
        
        return validation_results
```

#### 数据一致性检查：
```python
def check_data_consistency(data: List[Dict], consistency_rules: Dict) -> Dict:
    """
    检查数据一致性
    """
    results = {
        "total_records": len(data),
        "inconsistent_records": [],
        "consistency_rate": 0.0
    }
    
    for record in data:
        record_errors = []
        
        for rule_name, rule_config in consistency_rules.items():
            if rule_config["type"] == "cross_field":
                # 跨字段一致性检查
                field1 = record.get(rule_config["field1"])
                field2 = record.get(rule_config["field2"])
                
                if field1 is not None and field2 is not None:
                    if rule_config.get("operation") == "sum":
                        expected_sum = sum(record.get(f, 0) for f in rule_config.get("fields", []))
                        actual_sum = record.get(rule_config["total_field"], 0)
                        
                        if abs(expected_sum - actual_sum) > 0.01:  # 允许微小误差
                            record_errors.append(f"{rule_name}: 字段总和 {expected_sum} 不等于 {actual_sum}")
                    
                    elif rule_config.get("operation") == "compare":
                        if not eval(f"{field1} {rule_config['operator']} {field2}"):
                            record_errors.append(
                                f"{rule_name}: {field1} {rule_config['operator']} {field2} 不成立"
                            )
            
            elif rule_config["type"] == "business_rule":
                # 业务规则检查
                if not rule_config["validator"](record):
                    record_errors.append(f"{rule_name}: 违反业务规则")
        
        if record_errors:
            results["inconsistent_records"].append({
                "record_id": record.get("id", "unknown"),
                "errors": record_errors
            })
    
    # 计算一致性率
    if len(data) > 0:
        results["consistency_rate"] = round(
            (1 - len(results["inconsistent_records"]) / len(data)) * 100, 2
        )
    
    return results
```

### 1.3 数据唯一性规则

#### 主键唯一性检查：
```python
def check_unique_constraints(data: List[Dict], unique_fields: List[str]) -> Dict:
    """
    检查唯一性约束
    """
    results = {
        "total_records": len(data),
        "duplicate_records": [],
        "unique_fields": unique_fields,
        "uniqueness_rate": 0.0
    }
    
    # 创建字段值到记录ID的映射
    field_value_map = {}
    
    for i, record in enumerate(data):
        for field in unique_fields:
            value = record.get(field)
            if value is not None:
                key = f"{field}:{value}"
                
                if key in field_value_map:
                    # 发现重复
                    results["duplicate_records"].append({
                        "field": field,
                        "value": value,
                        "record_ids": field_value_map[key] + [i],
                        "duplicate_count": len(field_value_map[key]) + 1
                    })
                else:
                    field_value_map[key] = [i]
    
    # 计算唯一性率
    if len(data) > 0:
        unique_count = len(data) - len(results["duplicate_records"])
        results["uniqueness_rate"] = round(unique_count / len(data) * 100, 2)
    
    return results
```

#### 业务唯一性检查：
```python
def check_business_uniqueness(orders: List[Dict]) -> Dict:
    """
    检查业务唯一性（如订单号不能重复）
    """
    results = {
        "total_orders": len(orders),
        "duplicate_order_nos": {},
        "uniqueness_rate": 0.0
    }
    
    order_no_count = {}
    
    for order in orders:
        order_no = order.get("order_no")
        if order_no:
            order_no_count[order_no] = order_no_count.get(order_no, 0) + 1
    
    # 找出重复的订单号
    for order_no, count in order_no_count.items():
        if count > 1:
            results["duplicate_order_nos"][order_no] = count
    
    # 计算唯一性率
    if len(orders) > 0:
        unique_count = len([o for o in orders if order_no_count.get(o.get("order_no", ""), 0) == 1])
        results["uniqueness_rate"] = round(unique_count / len(orders) * 100, 2)
    
    return results
```

## 2. 测试数据验证和校验脚本

### 2.1 综合数据质量检查工具

```python
# I:\AI-Ready\qa\test-data-quality-checker.py
"""
测试数据质量检查工具
综合检查测试数据的完整性、准确性、一致性、唯一性
"""

import json
import csv
import pandas as pd
from datetime import datetime
from typing import Dict, List, Any, Tuple
from data_format_validator import DataFormatValidator
from collections import defaultdict

class TestDataQualityChecker:
    """测试数据质量检查器"""
    
    def __init__(self):
        self.validator = DataFormatValidator()
        self.validation_results = {}
        
    def load_data(self, file_path: str, file_type: str = "auto") -> List[Dict]:
        """加载数据文件"""
        if file_type == "auto":
            if file_path.endswith(".json"):
                file_type = "json"
            elif file_path.endswith(".csv"):
                file_type = "csv"
            elif file_path.endswith(".xlsx") or file_path.endswith(".xls"):
                file_type = "excel"
        
        try:
            if file_type == "json":
                with open(file_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            elif file_type == "csv":
                data = []
                with open(file_path, 'r', encoding='utf-8') as f:
                    reader = csv.DictReader(f)
                    for row in reader:
                        data.append(row)
                return data
            elif file_type == "excel":
                df = pd.read_excel(file_path)
                return df.to_dict('records')
            else:
                raise ValueError(f"不支持的文件类型: {file_type}")
        except Exception as e:
            print(f"加载文件 {file_path} 失败: {e}")
            return []
    
    def check_data_quality(self, data: List[Dict], data_type: str, 
                          quality_rules: Dict) -> Dict:
        """
        综合检查数据质量
        """
        results = {
            "data_type": data_type,
            "check_time": datetime.now().isoformat(),
            "total_records": len(data),
            "quality_score": 0.0,
            "detailed_results": {}
        }
        
        # 1. 检查数据完整性
        if "completeness" in quality_rules:
            completeness_results = self.check_data_completeness(
                data, quality_rules["completeness"]
            )
            results["detailed_results"]["completeness"] = completeness_results
        
        # 2. 检查数据准确性
        if "accuracy" in quality_rules:
            accuracy_results = self.check_data_accuracy(
                data, quality_rules["accuracy"]
            )
            results["detailed_results"]["accuracy"] = accuracy_results
        
        # 3. 检查数据一致性
        if "consistency" in quality_rules:
            consistency_results = self.check_data_consistency(
                data, quality_rules["consistency"]
            )
            results["detailed_results"]["consistency"] = consistency_results
        
        # 4. 检查数据唯一性
        if "uniqueness" in quality_rules:
            uniqueness_results = self.check_data_uniqueness(
                data, quality_rules["uniqueness"]
            )
            results["detailed_results"]["uniqueness"] = uniqueness_results
        
        # 5. 检查数据时效性
        if "timeliness" in quality_rules:
            timeliness_results = self.check_data_timeliness(
                data, quality_rules["timeliness"]
            )
            results["detailed_results"]["timeliness"] = timeliness_results
        
        # 计算总体质量得分
        quality_score = self.calculate_quality_score(results["detailed_results"])
        results["quality_score"] = quality_score
        
        # 添加质量等级
        results["quality_grade"] = self.get_quality_grade(quality_score)
        
        return results
    
    def check_data_completeness(self, data: List[Dict], completeness_rules: Dict) -> Dict:
        """检查数据完整性"""
        results = {
            "total_fields_checked": 0,
            "missing_fields": defaultdict(list),
            "completeness_rate": 0.0,
            "field_level_results": {}
        }
        
        total_checks = 0
        passed_checks = 0
        
        for field, rules in completeness_rules.items():
            field_results = {
                "required": rules.get("required", False),
                "missing_count": 0,
                "present_count": 0,
                "completeness_rate": 0.0
            }
            
            for record in data:
                total_checks += 1
                value = record.get(field)
                
                # 检查必填字段
                if rules.get("required", False):
                    if value is None or value == "":
                        field_results["missing_count"] += 1
                        results["missing_fields"][field].append(record.get("id", "unknown"))
                    else:
                        field_results["present_count"] += 1
                        passed_checks += 1
                else:
                    # 非必填字段，只要存在就计为通过
                    if value is not None and value != "":
                        field_results["present_count"] += 1
                        passed_checks += 1
            
            # 计算字段级别的完整性率
            if len(data) > 0:
                field_results["completeness_rate"] = round(
                    field_results["present_count"] / len(data) * 100, 2
                )
            
            results["field_level_results"][field] = field_results
        
        # 计算总体完整性率
        if total_checks > 0:
            results["completeness_rate"] = round(passed_checks / total_checks * 100, 2)
        
        results["total_fields_checked"] = len(completeness_rules)
        
        return results
    
    def check_data_accuracy(self, data: List[Dict], accuracy_rules: Dict) -> Dict:
        """检查数据准确性"""
        results = {
            "total_fields_checked": 0,
            "inaccurate_records": [],
            "accuracy_rate": 0.0,
            "field_level_results": {}
        }
        
        total_checks = 0
        passed_checks = 0
        
        for field, rules in accuracy_rules.items():
            field_results = {
                "validation_rules": rules,
                "inaccurate_count": 0,
                "accurate_count": 0,
                "accuracy_rate": 0.0,
                "errors": []
            }
            
            for record in data:
                total_checks += 1
                value = record.get(field)
                
                # 如果字段为空，跳过验证
                if value is None or value == "":
                    passed_checks += 1
                    field_results["accurate_count"] += 1
                    continue
                
                is_valid = True
                
                # 验证格式
                if "format" in rules:
                    validator_name = f"validate_{rules['format']}"
                    validator = getattr(self.validator, validator_name, None)
                    if validator and not validator(value):
                        is_valid = False
                        field_results["errors"].append(
                            f"记录 {record.get('id', 'unknown')}: 格式不正确"
                        )
                
                # 验证数值范围
                if is_valid and ("min" in rules or "max" in rules):
                    if not self.validator.validate_numeric_range(
                        value, rules.get("min"), rules.get("max")
                    ):
                        is_valid = False
                        field_results["errors"].append(
                            f"记录 {record.get('id', 'unknown')}: 值超出范围"
                        )
                
                # 验证枚举值
                if is_valid and "enum" in rules:
                    if not self.validator.validate_enum(value, rules["enum"]):
                        is_valid = False
                        field_results["errors"].append(
                            f"记录 {record.get('id', 'unknown')}: 值不在允许列表中"
                        )
                
                # 验证正则表达式
                if is_valid and "pattern" in rules:
                    if not re.match(rules["pattern"], str(value)):
                        is_valid = False
                        field_results["errors"].append(
                            f"记录 {record.get('id', 'unknown')}: 不符合正则表达式"
                        )
                
                if is_valid:
                    passed_checks += 1
                    field_results["accurate_count"] += 1
                else:
                    field_results["inaccurate_count"] += 1
                    results["inaccurate_records"].append({
                        "record_id": record.get("id", "unknown"),
                        "field": field,
                        "value": value,
                        "rules": rules
                    })
            
            # 计算字段级别的准确率
            if len(data) > 0:
                field_results["accuracy_rate"] = round(
                    field_results["accurate_count"] / len(data) * 100, 2
                )
            
            results["field_level_results"][field] = field_results
        
        # 计算总体准确率
        if total_checks > 0:
            results["accuracy_rate"] = round(passed_checks / total_checks * 100, 2)
        
        results["total_fields_checked"] = len(accuracy_rules)
        
        return results
    
    def check_data_consistency(self, data: List[Dict], consistency_rules: Dict) -> Dict:
        """检查数据一致性"""
        results = {
            "total_rules_checked": 0,
            "inconsistent_records": [],
            "consistency_rate": 0.0,
            "rule_level_results": {}
        }
        
        total_checks = 0
        passed_checks = 0
        
        for rule_name, rule_config in consistency_rules.items():
            rule_results = {
                "rule_config": rule_config,
                "inconsistent_count": 0,
                "consistent_count": 0,
                "consistency_rate": 0.0,
                "errors": []
            }
            
            for record in data:
                total_checks += 1
                
                if rule_config["type"] == "cross_field":
                    # 跨字段一致性检查
                    is_consistent = self.check_cross_field_consistency(record, rule_config)
                elif rule_config["type"] == "business_rule":
                    # 业务规则检查
                    is_consistent = rule_config["validator"](record)
                else:
                    is_consistent = True
                
                if is_consistent:
                    passed_checks += 1
                    rule_results["consistent_count"] += 1
                else:
                    rule_results["inconsistent_count"] += 1
                    rule_results["errors"].append({
                        "record_id": record.get("id", "unknown"),
                        "message": f"违反规则: {rule_name}"
                    })
                    
                    results["inconsistent_records"].append({
                        "record_id": record.get("id", "unknown"),
                        "rule_name": rule_name,
                        "rule_config": rule_config
                    })
            
            # 计算规则级别的一致性率
            if len(data) > 0:
                rule_results["consistency_rate"] = round(
                    rule_results["consistent_count"] / len(data) * 100, 2
                )
            
            results["rule_level_results"][rule_name] = rule_results
        
        # 计算总体一致性率
        if total_checks > 0:
            results["consistency_rate"] = round(passed_checks / total_checks * 100, 2)
        
        results["total_rules_checked"] = len(consistency_rules)
        
        return results
    
    def check_cross_field_consistency(self, record: Dict, rule_config: Dict) -> bool:
        """检查跨字段一致性"""
        if rule_config.get("operation") == "sum":
            # 求和检查
            fields = rule_config.get("fields", [])
            total_field = rule_config.get("total_field")
            
            if not total_field:
                return True
            
            expected_sum = sum(float(record.get(f, 0)) for f in fields)
            actual_sum = float(record.get(total_field, 0))
            
            # 允许微小误差
            return abs(expected_sum - actual_sum) <= 0.01
        
        elif rule_config.get("operation") == "compare":
            # 比较检查
            field1 = record.get(rule_config["field1"])
            field2 = record.get(rule_config["field2"])
            operator = rule_config.get("operator", "==")
            
            if field1 is None or field2 is None:
                return True
            
            try:
                return eval(f"{field1} {operator} {field2}")
            except:
                return False
        
        elif rule_config.get("operation") == "dependency":
            # 依赖关系检查
            condition_field = rule_config.get("condition_field")
            condition_value = rule_config.get("condition_value")
            dependent_field = rule_config.get("dependent_field")
            dependent_value = rule_config.get("dependent_value")
            
            if not condition_field or not dependent_field:
                return True
            
            record_condition = record.get(condition_field)
            record_dependent = record.get(dependent_field)
            
            if record_condition == condition_value:
                return record_dependent == dependent_value
            else:
                return True
        
        return True
    
    def check_data_uniqueness(self, data: List[Dict], uniqueness_rules: Dict) -> Dict:
        """检查数据唯一性"""
        results = {
            "total_fields_checked": 0,
            "duplicate_records": [],
            "uniqueness_rate": 0.0,
            "field_level_results": {}
        }
        
        for field, is_unique in uniqueness_rules.items():
            if not is_unique:
                continue
            
            field_results = {
                "is_unique": True,
                "duplicate_count": 0,
                "unique_count": 0,
                "uniqueness_rate": 0.0,
                "duplicate_values": []
            }
            
            # 统计每个值的出现次数
            value_count = defaultdict(int)
            value_records = defaultdict(list)
            
            for i, record in enumerate(data):
                value = record.get(field)
                if value is not None and value != "":
                    value_count[value] += 1
                    value_records[value].append({
                        "record_id": record.get("id", f"index_{i}"),
                        "index": i
                    })
            
            # 找出重复值
            duplicates = {value: count for value, count in value_count.items() if count > 1}
            
            if duplicates:
                field_results["is_unique"] = False
                field_results["duplicate_count"] = len(duplicates)
                field_results["unique_count"] = len(value_count) - len(duplicates)
                
                for value, count in duplicates.items():
                    field_results["duplicate_values"].append({
                        "value": value,
                        "count": count,
                        "records": value_records[value]
                    })
                    
                    # 添加到总体重复记录
                    for record_info in value_records[value]:
                        results["duplicate_records"].append({
                            "field": field,
                            "value": value,
                            "record_id": record_info["record_id"],
                            "duplicate_count": count
                        })
            else:
                field_results["unique_count"] = len(value_count)
            
            # 计算字段级别的唯一性率
            if len(data) > 0:
                unique_records = len(data) - sum(count-1 for count in duplicates.values())
                field_results["uniqueness_rate"] = round(unique_records / len(data) * 100, 2)
            
            results["field_level_results"][field] = field_results
        
        # 计算总体唯一性率
        if len(data) > 0:
            total_duplicates = len(set(r["record_id"] for r in results["duplicate_records"]))
            unique_records = len(data) - total_duplicates
            results["uniqueness_rate"] = round(unique_records / len(data) * 100, 2)
        
        results["total_fields_checked"] = sum(1 for v in uniqueness_rules.values() if v)
        
        return results
    
    def check_data_timeliness(self, data: List[Dict], timeliness_rules: Dict) -> Dict:
        """检查数据时效性"""
        results = {
            "total_fields_checked": 0,
            "outdated_records": [],
            "timeliness_rate": 0.0,
            "field_level_results": {}
        }
        
        total_checks = 0
        passed_checks = 0
        
        for field, rules in timeliness_rules.items():
            field_results = {
                "max_age_days": rules.get("max_age_days"),
                "outdated_count": 0,
                "timely_count": 0,
                "timeliness_rate": 0.0,
                "outdated_values": []
            }
            
            for record in data:
                total_checks += 1
                value = record.get(field)
                
                if value is None or value == "":
                    passed_checks += 1
                    field_results["timely_count"] += 1
                    continue
                
                try:
                    # 尝试解析日期时间
                    if isinstance(value, str):
                        # 移除时区信息以便解析
                        if 'Z' in value:
                            value = value.replace('Z', '+00:00')
                        
                        record_date = datetime.fromisoformat(value)
                    elif isinstance(value, (int, float)):
                        # 假设是Unix时间戳
                        record_date = datetime.fromtimestamp(value / 1000)  # 假设是毫秒级时间戳
                    else:
                        # 无法解析，视为无效
                        field_results["outdated_count"] += 1
                        field_results["outdated_values"].append({
                            "record_id": record.get("id", "unknown"),
                            "value": value,
                            "reason": "无法解析日期时间"
                        })
                        continue
                    
                    # 检查是否过期
                    now = datetime.now()
                    age_days = (now - record_date).days
                    
                    if age_days <= rules.get("max_age_days", 365):
                        passed_checks += 1
                        field_results["timely_count"] += 1
                    else:
                        field_results["outdated_count"] += 1
                        field_results["outdated_values"].append({
                            "record_id": record.get("id", "unknown"),
                            "value": value,
                            "age_days": age_days,
                            "max_allowed_days": rules.get("max_age_days")
                        })
                        
                        results["outdated_records"].append({
                            "record_id": record.get("id", "unknown"),
                            "field": field,
                            "value": value,
                            "age_days": age_days,
                            "max_allowed_days": rules.get("max_age_days")
                        })
                        
                except Exception as e:
                    # 解析失败
                    field_results["outdated_count"] += 1
                    field_results["outdated_values"].append({
                        "record_id": record.get("id", "unknown"),
                        "value": value,
                        "reason": f"解析失败: {str(e)}"
                    })
            
            # 计算字段级别的时效性率
            if len(data) > 0:
                field_results["timeliness_rate"] = round(
                    field_results["timely_count"] / len(data) * 100, 2
                )
            
            results["field_level_results"][field] = field_results
        
        # 计算总体时效性率
        if total_checks > 0:
            results["timeliness_rate"] = round(passed_checks / total_checks * 100, 2)
        
        results["total_fields_checked"] = len(timeliness_rules)
        
        return results
    
    def calculate_quality_score(self, detailed_results: Dict) -> float:
        """计算总体质量得分"""
        weights = {
            "completeness": 0.25,
            "accuracy": 0.30,
            "consistency": 0.20,
            "uniqueness": 0.15,
            "timeliness": 0.10
        }
        
        total_score = 0.0
        total_weight = 0.0
        
        for dimension, weight in weights.items():
            if dimension in detailed_results:
                dimension_rate = detailed_results[dimension].get(f"{dimension}_rate", 0.0)
                total_score += dimension_rate * weight
                total_weight += weight
        
        # 如果某些维度未检查，调整权重
        if total_weight > 0:
            return round(total_score / total_weight, 2)
        else:
            return 0.0
    
    def get_quality_grade(self, score: float) -> str:
        """获取质量等级"""
        if score >= 90:
            return "A (优秀)"
        elif score >= 80:
            return "B (良好)"
        elif score >= 70:
            return "C (合格)"
        elif score >= 60:
            return "D (需要改进)"
        else:
            return "F (不合格)"
    
    def generate_quality_report(self, quality_results: Dict, output_file: str):
        """生成质量报告"""
        report = {
            "report_title": "测试数据质量检查报告",
            "generated_at": datetime.now().isoformat(),
            "summary": {
                "data_type": quality_results.get("data_type"),
                "total_records": quality_results.get("total_records"),
                "quality_score": quality_results.get("quality_score"),
                "quality_grade": quality_results.get("quality_grade")
            },
            "detailed_results": quality_results.get("detailed_results", {}),
            "recommendations": self.generate_recommendations(quality_results)
        }
        
        # 保存报告
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"质量报告已保存到: {output_file}")
        
        # 打印简要报告
        self.print_summary_report(report)
    
    def generate_recommendations(self, quality_results: Dict) -> List[str]:
        """生成改进建议"""
        recommendations = []
        
        detailed_results = quality_results.get("detailed_results", {})
        
        # 根据各维度分数生成建议
        for dimension in ["completeness", "accuracy", "consistency", "uniqueness", "timeliness"]:
            if dimension in detailed_results:
                rate = detailed_results[dimension].get(f"{dimension}_rate", 0.0)
                
                if rate < 80:
                    if dimension == "completeness":
                        recommendations.append(
                            f"数据完整性({rate}%): 需要检查缺失字段，特别是必填字段"
                        )
                    elif dimension == "accuracy":
                        recommendations.append(
                            f"数据准确性({rate}%): 需要验证数据格式和取值范围"
                        )
                    elif dimension == "consistency":
                        recommendations.append(
                            f"数据一致性({rate}%): 需要检查数据间的逻辑关系"
                        )
                    elif dimension == "uniqueness":
                        recommendations.append(
                            f"数据唯一性({rate}%): 需要处理重复数据"
                        )
                    elif dimension == "timeliness":
                        recommendations.append(
                            f"数据时效性({rate}%): 需要更新过期数据"
                        )
        
        # 根据总体分数生成总体建议
        score = quality_results.get("quality_score", 0.0)
        if score < 60:
            recommendations.append("总体数据质量不合格，需要全面检查和修复")
        elif score < 70:
            recommendations.append("数据质量需要显著改进")
        elif score < 80:
            recommendations.append("数据质量基本合格，但仍有改进空间")
        elif score < 90:
            recommendations.append("数据质量良好，可以进一步优化")
        else:
            recommendations.append("数据质量优秀，继续保持")
        
        return recommendations
    
    def print_summary_report(self, report: Dict):
        """打印简要报告"""
        print("\n" + "="*60)
        print("测试数据质量检查报告 - 摘要")
