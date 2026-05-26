"""
测试断言工具类
提供丰富的断言函数，用于验证接口响应的正确性
"""
import json
from typing import Any, Dict, List, Optional, Union
from datetime import datetime, timedelta

import jsonschema
from pydantic import BaseModel, ValidationError


class APIResponseValidator:
    """API响应验证器"""
    
    @staticmethod
    def assert_status_code(response, expected_status_code: int):
        """断言状态码"""
        assert response.status_code == expected_status_code, \
            f"期望状态码 {expected_status_code}，实际 {response.status_code}，响应: {response.text}"
    
    @staticmethod
    def assert_response_time(response, max_time_ms: int):
        """断言响应时间"""
        response_time = response.elapsed.total_seconds() * 1000
        assert response_time <= max_time_ms, \
            f"响应时间超时: {response_time:.2f}ms > {max_time_ms}ms"
    
    @staticmethod
    def assert_json_structure(response, expected_keys: List[str]):
        """断言JSON结构包含指定键"""
        response_data = response.json()
        for key in expected_keys:
            assert key in response_data, f"响应缺少关键字段: {key}"
    
    @staticmethod
    def assert_data_type(value, expected_type, field_name: str = ""):
        """断言数据类型"""
        assert isinstance(value, expected_type), \
            f"{field_name} 期望类型 {expected_type.__name__}，实际类型 {type(value).__name__}"
    
    @staticmethod
    def assert_not_empty(value, field_name: str = ""):
        """断言非空"""
        if isinstance(value, str):
            assert value.strip() != "", f"{field_name} 不能为空字符串"
        elif isinstance(value, (list, dict, set)):
            assert len(value) > 0, f"{field_name} 不能为空"
        else:
            assert value is not None, f"{field_name} 不能为None"
    
    @staticmethod
    def assert_in_range(value, min_value: Optional[float] = None, 
                       max_value: Optional[float] = None, field_name: str = ""):
        """断言数值范围"""
        if min_value is not None:
            assert value >= min_value, f"{field_name} 值 {value} 小于最小值 {min_value}"
        if max_value is not None:
            assert value <= max_value, f"{field_name} 值 {value} 大于最大值 {max_value}"
    
    @staticmethod
    def assert_timestamp_recent(timestamp: Union[int, str], 
                               max_days_old: int = 30, field_name: str = ""):
        """断言时间戳是最近的"""
        if isinstance(timestamp, str):
            dt = datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
        else:
            dt = datetime.fromtimestamp(timestamp / 1000)  # 假设是毫秒时间戳
        
        max_age = datetime.now() - timedelta(days=max_days_old)
        assert dt >= max_age, f"{field_name} 时间 {dt} 超过 {max_days_old} 天"
    
    @staticmethod
    def assert_positive(value, field_name: str = ""):
        """断言正数"""
        assert value > 0, f"{field_name} 必须是正数，实际值: {value}"
    
    @staticmethod
    def assert_negative(value, field_name: str = ""):
        """断言负数"""
        assert value < 0, f"{field_name} 必须是负数，实际值: {value}"


class SupplierSchemaValidator:
    """供应商数据模式验证器"""
    
    @staticmethod
    def validate_supplier_info(data: Dict[str, Any]):
        """验证供应商信息"""
        # 必填字段
        required_fields = ['id', 'name', 'status', 'contact_person', 'phone', 'email']
        for field in required_fields:
            assert field in data, f"供应商信息缺少必填字段: {field}"
        
        # 数据类型验证
        assert isinstance(data['id'], (str, int)), "供应商ID必须是字符串或整数"
        assert isinstance(data['name'], str), "供应商名称必须是字符串"
        assert isinstance(data['contact_person'], str), "联系人必须是字符串"
        assert isinstance(data['phone'], str), "电话必须是字符串"
        assert isinstance(data['email'], str), "邮箱必须是字符串"
        
        # 格式验证
        assert '@' in data['email'], "邮箱格式不正确"
        assert len(data['phone']) >= 8, "电话格式不正确"
        
        return True
    
    @staticmethod
    def validate_contract_info(data: Dict[str, Any]):
        """验证合同信息"""
        # 必填字段
        required_fields = ['id', 'supplier_id', 'contract_no', 'start_date', 
                          'end_date', 'status', 'amount']
        for field in required_fields:
            assert field in data, f"合同信息缺少必填字段: {field}"
        
        # 数据类型验证
        assert isinstance(data['id'], (str, int)), "合同ID必须是字符串或整数"
        assert isinstance(data['supplier_id'], (str, int)), "供应商ID必须是字符串或整数"
        assert isinstance(data['contract_no'], str), "合同编号必须是字符串"
        assert isinstance(data['status'], str), "合同状态必须是字符串"
        assert isinstance(data['amount'], (int, float)), "合同金额必须是数字"
        
        # 业务逻辑验证
        assert data['end_date'] > data['start_date'], "结束日期必须晚于开始日期"
        assert data['amount'] >= 0, "合同金额不能为负数"
        
        return True
    
    @staticmethod
    def validate_order_info(data: Dict[str, Any]):
        """验证订单信息"""
        # 必填字段
        required_fields = ['id', 'order_no', 'supplier_id', 'product_name', 
                          'quantity', 'unit_price', 'total_amount', 'status']
        for field in required_fields:
            assert field in data, f"订单信息缺少必填字段: {field}"
        
        # 数据类型验证
        assert isinstance(data['id'], (str, int)), "订单ID必须是字符串或整数"
        assert isinstance(data['order_no'], str), "订单编号必须是字符串"
        assert isinstance(data['supplier_id'], (str, int)), "供应商ID必须是字符串或整数"
        assert isinstance(data['product_name'], str), "产品名称必须是字符串"
        assert isinstance(data['quantity'], (int, float)), "数量必须是数字"
        assert isinstance(data['unit_price'], (int, float)), "单价必须是数字"
        assert isinstance(data['total_amount'], (int, float)), "总金额必须是数字"
        assert isinstance(data['status'], str), "订单状态必须是字符串"
        
        # 业务逻辑验证
        assert data['quantity'] > 0, "数量必须大于0"
        assert data['unit_price'] >= 0, "单价不能为负数"
        assert data['total_amount'] >= 0, "总金额不能为负数"
        assert abs(data['total_amount'] - data['quantity'] * data['unit_price']) < 0.01, \
            "总金额计算不正确"
        
        return True


class PerformanceValidator:
    """性能验证器"""
    
    @staticmethod
    def assert_performance_metrics(metrics: Dict[str, Any], sla_config: Dict[str, Any]):
        """断言性能指标满足SLA要求"""
        sla_requirements = sla_config.get('sla_requirements', {})
        
        # 响应时间验证
        if 'response_time_95th' in sla_requirements:
            max_response_time = sla_requirements['response_time_95th']
            actual_response_time = metrics.get('p95_response_time')
            if actual_response_time:
                assert actual_response_time <= max_response_time, \
                    f"95%响应时间 {actual_response_time}ms 超过SLA限制 {max_response_time}ms"
        
        # 错误率验证
        if 'error_rate' in sla_requirements:
            max_error_rate = sla_requirements['error_rate']
            actual_error_rate = metrics.get('error_rate', 0)
            assert actual_error_rate <= max_error_rate, \
                f"错误率 {actual_error_rate:.2%} 超过SLA限制 {max_error_rate:.2%}"
        
        # 吞吐量验证
        if 'min_throughput' in sla_requirements:
            min_throughput = sla_requirements['min_throughput']
            actual_throughput = metrics.get('throughput', 0)
            assert actual_throughput >= min_throughput, \
                f"吞吐量 {actual_throughput} req/s 低于SLA限制 {min_throughput} req/s"
        
        return True
    
    @staticmethod
    def assert_concurrent_performance(metrics_list: List[Dict[str, Any]], 
                                     concurrent_users: int):
        """断言并发性能"""
        for metrics in metrics_list:
            # 验证每个并发级别下的性能
            assert metrics.get('concurrent_users') == concurrent_users, \
                f"并发用户数不匹配: {metrics.get('concurrent_users')} vs {concurrent_users}"
            
            # 错误率检查
            assert metrics.get('error_rate', 0) < 0.05, \
                f"并发 {concurrent_users} 用户时错误率过高: {metrics.get('error_rate'):.2%}"
            
            # 平均响应时间检查
            assert metrics.get('avg_response_time', 0) < 5000, \
                f"并发 {concurrent_users} 用户时平均响应时间过长: {metrics.get('avg_response_time'):.0f}ms"
        
        return True


class SecurityValidator:
    """安全验证器"""
    
    @staticmethod
    def assert_no_sensitive_data(response_data: Dict[str, Any]):
        """断言响应中不包含敏感数据"""
        sensitive_fields = ['password', 'token', 'secret_key', 'private_key', 
                           'credit_card', 'ssn', 'passport']
        
        def check_dict(data: Any, path: str = ""):
            if isinstance(data, dict):
                for key, value in data.items():
                    full_path = f"{path}.{key}" if path else key
                    # 检查字段名
                    for sensitive_field in sensitive_fields:
                        if sensitive_field in key.lower():
                            raise AssertionError(f"响应包含敏感字段: {full_path}")
                    # 递归检查嵌套结构
                    check_dict(value, full_path)
            elif isinstance(data, list):
                for i, item in enumerate(data):
                    check_dict(item, f"{path}[{i}]")
        
        check_dict(response_data)
        return True
    
    @staticmethod
    def assert_encrypted_fields(response_data: Dict[str, Any], 
                               encrypted_fields: List[str]):
        """断言特定字段已加密"""
        for field_path in encrypted_fields:
            # 解析字段路径
            parts = field_path.split('.')
            data = response_data
            for part in parts:
                if part.endswith('[]'):
                    # 处理数组
                    array_field = part[:-2]
                    if array_field in data and isinstance(data[array_field], list):
                        for item in data[array_field]:
                            assert '***' in str(item) or len(str(item)) == 32 or len(str(item)) == 64, \
                                f"字段 {field_path} 可能未加密"
                        break
                elif part in data:
                    data = data[part]
                else:
                    break
            
            if isinstance(data, dict) or isinstance(data, list):
                # 已经是嵌套结构，继续检查
                continue
            elif isinstance(data, str):
                # 检查是否为加密格式
                assert '***' in data or len(data) == 32 or len(data) == 64, \
                    f"字段 {field_path} 可能未加密: {data}"
        
        return True


class PaginationValidator:
    """分页验证器"""
    
    @staticmethod
    def validate_pagination_response(response_data: Dict[str, Any], 
                                    expected_page_size: Optional[int] = None):
        """验证分页响应结构"""
        # 检查分页字段
        assert 'page' in response_data, "分页响应缺少page字段"
        assert 'page_size' in response_data, "分页响应缺少page_size字段"
        assert 'total' in response_data, "分页响应缺少total字段"
        assert 'items' in response_data, "分页响应缺少items字段"
        
        # 数据类型验证
        assert isinstance(response_data['page'], int), "page必须是整数"
        assert isinstance(response_data['page_size'], int), "page_size必须是整数"
        assert isinstance(response_data['total'], int), "total必须是整数"
        assert isinstance(response_data['items'], list), "items必须是列表"
        
        # 逻辑验证
        assert response_data['page'] >= 1, "page必须大于等于1"
        assert response_data['page_size'] > 0, "page_size必须大于0"
        assert response_data['total'] >= 0, "total不能为负数"
        
        # 检查items数量不超过page_size
        if expected_page_size:
            assert response_data['page_size'] == expected_page_size, \
                f"page_size {response_data['page_size']} 不等于期望值 {expected_page_size}"
        
        assert len(response_data['items']) <= response_data['page_size'], \
            f"items数量 {len(response_data['items'])} 超过page_size {response_data['page_size']}"
        
        return True


def assert_api_response(response, expected_status_code: int = 200,
                       expected_keys: Optional[List[str]] = None,
                       max_time_ms: Optional[int] = None):
    """
    综合API响应断言
    
    Args:
        response: requests.Response对象
        expected_status_code: 期望的状态码
        expected_keys: 期望包含的键列表
        max_time_ms: 最大响应时间（毫秒）
    """
    validator = APIResponseValidator()
    
    # 状态码断言
    validator.assert_status_code(response, expected_status_code)
    
    # 响应时间断言
    if max_time_ms:
        validator.assert_response_time(response, max_time_ms)
    
    # JSON结构断言
    if expected_status_code == 200:
        assert response.headers.get('Content-Type', '').startswith('application/json'), \
            "响应Content-Type不是JSON格式"
        
        # 检查JSON格式
        try:
            response_data = response.json()
        except json.JSONDecodeError:
            raise AssertionError("响应不是有效的JSON格式")
        
        # 检查关键字段
        if expected_keys:
            validator.assert_json_structure(response, expected_keys)
        
        return response_data
    
    return None


def assert_business_logic(response_data: Dict[str, Any], 
                         validator_class: type,
                         validation_method: str):
    """
    断言业务逻辑正确性
    
    Args:
        response_data: 响应数据
        validator_class: 验证器类
        validation_method: 验证方法名
    """
    validator = validator_class()
    
    if hasattr(validator, validation_method):
        validation_func = getattr(validator, validation_method)
        return validation_func(response_data)
    else:
        raise ValueError(f"验证器 {validator_class.__name__} 没有方法 {validation_method}")


def assert_performance_meets_sla(metrics: Dict[str, Any], sla_config: Dict[str, Any]):
    """断言性能满足SLA要求"""
    return PerformanceValidator.assert_performance_metrics(metrics, sla_config)


def assert_security_compliance(response_data: Dict[str, Any], 
                             encrypted_fields: Optional[List[str]] = None):
    """断言安全合规性"""
    SecurityValidator.assert_no_sensitive_data(response_data)
    
    if encrypted_fields:
        SecurityValidator.assert_encrypted_fields(response_data, encrypted_fields)
    
    return True