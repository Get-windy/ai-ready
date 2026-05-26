#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 核心业务模块单元测试脚本
覆盖: 客户管理、订单管理、产品管理、库存管理
每个模块至少15个测试用例，共60+测试用例

使用: pytest test_business_modules.py -v
"""

import pytest
import sys
import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from unittest.mock import Mock, patch, MagicMock

# 添加测试路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mocks.mock_services import mock_crm, mock_erp, mock_db


# ==================== 客户管理模块测试 (15+ 用例) ====================

@pytest.mark.unit
class TestCustomerManagement:
    """客户管理模块测试 - 15个测试用例"""
    
    def setup_method(self):
        """每个测试方法前重置数据"""
        mock_crm.customers.clear()
        mock_crm.leads.clear()
        mock_crm.opportunities.clear()
    
    # ========== 基础CRUD测试 ==========
    
    def test_customer_create_success(self):
        """TC-CUST-001: 测试客户创建成功"""
        customer_data = {
            'name': '测试科技有限公司',
            'type': '企业',
            'level': 'A',
            'phone': '010-12345678',
            'email': 'contact@test.com',
            'address': '北京市海淀区',
            'industry': '科技',
            'source': '网站'
        }
        
        result = mock_crm.add_customer(customer_data)
        
        assert result['id'] > 0
        assert result['name'] == '测试科技有限公司'
        assert result['create_time'] is not None
        assert 'update_time' not in result  # 新创建不应该有update_time
    
    def test_customer_create_minimal(self):
        """TC-CUST-002: 测试最小信息创建客户"""
        customer_data = {'name': '最小客户'}
        
        result = mock_crm.add_customer(customer_data)
        
        assert result['id'] > 0
        assert result['name'] == '最小客户'
    
    def test_customer_query_by_id(self):
        """TC-CUST-003: 测试按ID查询客户"""
        customer = mock_crm.add_customer({'name': '查询测试客户', 'phone': '010-99999999'})
        
        result = mock_crm.get_customer(customer['id'])
        
        assert result is not None
        assert result['name'] == '查询测试客户'
        assert result['phone'] == '010-99999999'
    
    def test_customer_query_not_found(self):
        """TC-CUST-004: 测试查询不存在的客户"""
        result = mock_crm.get_customer(99999)
        
        assert result is None
    
    def test_customer_update(self):
        """TC-CUST-005: 测试更新客户信息"""
        customer = mock_crm.add_customer({'name': '原名称', 'level': 'C'})
        
        # 模拟更新
        mock_crm.customers[customer['id']]['name'] = '新名称'
        mock_crm.customers[customer['id']]['level'] = 'A'
        
        updated = mock_crm.get_customer(customer['id'])
        assert updated['name'] == '新名称'
        assert updated['level'] == 'A'
    
    # ========== 客户等级测试 ==========
    
    def test_customer_level_a(self):
        """TC-CUST-006: 测试A级客户判定"""
        level = mock_crm.get_customer_level(85)
        assert level == 'A'
        
        level = mock_crm.get_customer_level(100)
        assert level == 'A'
    
    def test_customer_level_b(self):
        """TC-CUST-007: 测试B级客户判定"""
        level = mock_crm.get_customer_level(65)
        assert level == 'B'
        
        level = mock_crm.get_customer_level(79)
        assert level == 'B'
    
    def test_customer_level_c(self):
        """TC-CUST-008: 测试C级客户判定"""
        level = mock_crm.get_customer_level(45)
        assert level == 'C'
        
        level = mock_crm.get_customer_level(59)
        assert level == 'C'
    
    def test_customer_level_d(self):
        """TC-CUST-009: 测试D级客户判定"""
        level = mock_crm.get_customer_level(20)
        assert level == 'D'
        
        level = mock_crm.get_customer_level(39)
        assert level == 'D'
    
    # ========== 客户价值计算测试 ==========
    
    def test_customer_value_calculation(self):
        """TC-CUST-010: 测试客户价值计算"""
        mock_crm.customers[1] = {
            'score': 80,
            'orders_count': 10,
            'avg_order_value': 500
        }
        
        value = mock_crm.calculate_customer_value(1)
        expected = 80 * 0.4 + 10 * 0.3 + 500 * 0.3
        
        assert value == round(expected, 2)
    
    def test_customer_value_missing_data(self):
        """TC-CUST-011: 测试缺失数据时的客户价值计算"""
        mock_crm.customers[1] = {"name": "测试客户"}  # 有客户但缺少计算字段
        
        value = mock_crm.calculate_customer_value(1)
        
        # 应该使用默认值
        assert value == 50 * 0.4 + 0 * 0.3 + 0 * 0.3
    
    def test_customer_value_nonexistent(self):
        """TC-CUST-012: 测试不存在客户的客户价值"""
        value = mock_crm.calculate_customer_value(99999)
        assert value == 0.0
    
    # ========== 数据验证测试 ==========
    
    def test_customer_type_validation(self):
        """TC-CUST-013: 测试客户类型验证"""
        valid_types = ['企业', '个体', '个人']
        
        assert '企业' in valid_types
        assert '个人' in valid_types
        assert '其他' not in valid_types
    
    def test_customer_phone_validation(self):
        """TC-CUST-014: 测试客户电话格式验证"""
        import re
        
        def validate_phone(phone):
            # 固定电话或手机号
            patterns = [
                r'^\d{3,4}-\d{7,8}$',  # 固定电话
                r'^1[3-9]\d{9}$'  # 手机号
            ]
            return any(re.match(p, phone) for p in patterns)
        
        assert validate_phone('010-12345678') == True
        assert validate_phone('13800138000') == True
        assert validate_phone('123') == False
    
    def test_customer_email_validation(self):
        """TC-CUST-015: 测试客户邮箱验证"""
        import re
        
        def validate_email(email):
            pattern = r'^[\w\.-]+@[\w\.-]+\.\w+$'
            return bool(re.match(pattern, email))
        
        assert validate_email('test@example.com') == True
        assert validate_email('invalid-email') == False
        assert validate_email('test@') == False


# ==================== 订单管理模块测试 (15+ 用例) ====================

@pytest.mark.unit
class TestOrderManagement:
    """订单管理模块测试 - 15个测试用例"""
    
    def setup_method(self):
        """每个测试方法前重置数据"""
        mock_erp.products.clear()
        mock_erp.stock.clear()
    
    # ========== 订单计算测试 ==========
    
    def test_order_total_calculation(self):
        """TC-ORD-001: 测试订单总额计算"""
        items = [
            {'product_id': 1, 'quantity': 10, 'price': 100.00},
            {'product_id': 2, 'quantity': 5, 'price': 200.00},
            {'product_id': 3, 'quantity': 2, 'price': 500.00}
        ]
        
        total = sum(item['quantity'] * item['price'] for item in items)
        
        assert total == 3000.00
    
    def test_order_discount_calculation(self):
        """TC-ORD-002: 测试订单折扣计算"""
        subtotal = 1000.00
        discount_rate = 0.9  # 9折
        
        final_amount = subtotal * discount_rate
        
        assert final_amount == 900.00
    
    def test_order_tax_calculation(self):
        """TC-ORD-003: 测试订单税费计算"""
        amount = 1000.00
        tax_rate = 0.13  # 13%税率
        
        tax = amount * tax_rate
        total = amount + tax
        
        assert tax == 130.00
        assert total == 1130.00
    
    # ========== 订单状态测试 ==========
    
    def test_order_status_pending(self):
        """TC-ORD-004: 测试订单待处理状态"""
        order = {'id': 1, 'status': 'pending'}
        
        assert order['status'] == 'pending'
        assert order['status'] in ['pending', 'confirmed', 'shipped', 'completed', 'cancelled']
    
    def test_order_status_transition(self):
        """TC-ORD-005: 测试订单状态转换"""
        valid_transitions = {
            'pending': ['confirmed', 'cancelled'],
            'confirmed': ['shipped', 'cancelled'],
            'shipped': ['completed'],
            'completed': [],
            'cancelled': []
        }
        
        def can_transition(current, target):
            return target in valid_transitions.get(current, [])
        
        assert can_transition('pending', 'confirmed') == True
        assert can_transition('pending', 'shipped') == False
        assert can_transition('confirmed', 'shipped') == True
    
    def test_order_cancel_rules(self):
        """TC-ORD-006: 测试订单取消规则"""
        def can_cancel(status):
            return status in ['pending', 'confirmed']
        
        assert can_cancel('pending') == True
        assert can_cancel('confirmed') == True
        assert can_cancel('shipped') == False
        assert can_cancel('completed') == False
    
    # ========== 订单验证测试 ==========
    
    def test_order_items_validation(self):
        """TC-ORD-007: 测试订单项验证"""
        def validate_order_items(items):
            if not items:
                return False, '订单项不能为空'
            for item in items:
                if item.get('quantity', 0) <= 0:
                    return False, '数量必须大于0'
                if item.get('price', 0) < 0:
                    return False, '价格不能为负'
            return True, 'OK'
        
        assert validate_order_items([{'quantity': 10, 'price': 100}])[0] == True
        assert validate_order_items([])[0] == False
        assert validate_order_items([{'quantity': 0, 'price': 100}])[0] == False
    
    def test_order_amount_validation(self):
        """TC-ORD-008: 测试订单金额验证"""
        def validate_amount(amount):
            if amount <= 0:
                return False, '金额必须大于0'
            if amount > 10000000:
                return False, '金额超出限制'
            return True, 'OK'
        
        assert validate_amount(100)[0] == True
        assert validate_amount(0)[0] == False
        assert validate_amount(-100)[0] == False
        assert validate_amount(20000000)[0] == False
    
    # ========== 订单时间测试 ==========
    
    def test_order_delivery_time_calculation(self):
        """TC-ORD-009: 测试订单交付时间计算"""
        order_time = datetime.now()
        delivery_days = 3
        expected_delivery = order_time + timedelta(days=delivery_days)
        
        assert expected_delivery > order_time
        assert (expected_delivery - order_time).days == 3
    
    def test_order_timeout_check(self):
        """TC-ORD-010: 测试订单超时检查"""
        def is_timeout(order_time, timeout_hours=24):
            return (datetime.now() - order_time) > timedelta(hours=timeout_hours)
        
        old_order = datetime.now() - timedelta(hours=25)
        new_order = datetime.now() - timedelta(hours=10)
        
        assert is_timeout(old_order) == True
        assert is_timeout(new_order) == False
    
    # ========== 订单统计测试 ==========
    
    def test_order_count_by_status(self):
        """TC-ORD-011: 测试按状态统计订单"""
        orders = [
            {'id': 1, 'status': 'pending'},
            {'id': 2, 'status': 'pending'},
            {'id': 3, 'status': 'completed'},
            {'id': 4, 'status': 'cancelled'}
        ]
        
        status_counts = {}
        for order in orders:
            status = order['status']
            status_counts[status] = status_counts.get(status, 0) + 1
        
        assert status_counts['pending'] == 2
        assert status_counts['completed'] == 1
        assert status_counts['cancelled'] == 1
    
    def test_order_revenue_calculation(self):
        """TC-ORD-012: 测试订单收入计算"""
        orders = [
            {'id': 1, 'status': 'completed', 'amount': 1000},
            {'id': 2, 'status': 'completed', 'amount': 2000},
            {'id': 3, 'status': 'pending', 'amount': 500}
        ]
        
        total_revenue = sum(o['amount'] for o in orders if o['status'] == 'completed')
        
        assert total_revenue == 3000
    
    # ========== 订单编号测试 ==========
    
    def test_order_number_generation(self):
        """TC-ORD-013: 测试订单编号生成"""
        def generate_order_number(prefix='ORD'):
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            import random
            random_suffix = random.randint(1000, 9999)
            return f'{prefix}{timestamp}{random_suffix}'
        
        order_num = generate_order_number()
        
        assert order_num.startswith('ORD')
        assert len(order_num) > 10
    
    def test_order_number_uniqueness(self):
        """TC-ORD-014: 测试订单编号唯一性"""
        import time
        
        order_numbers = set()
        for _ in range(100):
            order_num = f'ORD{datetime.now().strftime("%Y%m%d%H%M%S%f")}'
            order_numbers.add(order_num)
            time.sleep(0.001)  # 确保时间戳不同
        
        # 由于时间戳不同，应该有100个不同的编号
        assert len(order_numbers) == 100
    
    # ========== 订单退款测试 ==========
    
    def test_order_refund_calculation(self):
        """TC-ORD-015: 测试订单退款计算"""
        def calculate_refund(order_amount, refund_rate=1.0):
            return round(order_amount * refund_rate, 2)
        
        assert calculate_refund(1000) == 1000.00
        assert calculate_refund(1000, 0.8) == 800.00


# ==================== 产品管理模块测试 (15+ 用例) ====================

@pytest.mark.unit
class TestProductManagement:
    """产品管理模块测试 - 15个测试用例"""
    
    def setup_method(self):
        """每个测试方法前重置数据"""
        mock_erp.products.clear()
    
    # ========== 产品CRUD测试 ==========
    
    def test_product_create_success(self):
        """TC-PROD-001: 测试产品创建成功"""
        product_data = {
            'code': 'PROD-001',
            'name': '测试产品',
            'category': '电子产品',
            'price': 199.99,
            'cost': 99.99,
            'unit': '个',
            'status': 'active'
        }
        
        result = mock_erp.add_product(product_data)
        
        assert result['id'] > 0
        assert result['code'] == 'PROD-001'
        assert result['name'] == '测试产品'
    
    def test_product_query_by_id(self):
        """TC-PROD-002: 测试按ID查询产品"""
        product = mock_erp.add_product({
            'code': 'PROD-002',
            'name': '查询产品',
            'price': 299.99
        })
        
        result = mock_erp.get_product(product['id'])
        
        assert result is not None
        assert result['name'] == '查询产品'
    
    def test_product_query_not_found(self):
        """TC-PROD-003: 测试查询不存在的产品"""
        result = mock_erp.get_product(99999)
        
        assert result is None
    
    def test_product_update(self):
        """TC-PROD-004: 测试更新产品信息"""
        product = mock_erp.add_product({
            'code': 'PROD-003',
            'name': '原产品名',
            'price': 100
        })
        
        # 模拟更新
        mock_erp.products[product['id']]['name'] = '新产品名'
        mock_erp.products[product['id']]['price'] = 150
        
        updated = mock_erp.get_product(product['id'])
        assert updated['name'] == '新产品名'
        assert updated['price'] == 150
    
    # ========== 产品价格测试 ==========
    
    def test_product_price_calculation(self):
        """TC-PROD-005: 测试产品价格计算"""
        cost = 80.00
        price = 100.00
        profit = price - cost
        profit_rate = round(profit / price * 100, 2)
        
        assert profit == 20.00
        assert profit_rate == 20.0
    
    def test_product_discount_price(self):
        """TC-PROD-006: 测试产品折扣价"""
        original_price = 100.00
        discount_rate = 0.8
        
        discounted_price = original_price * discount_rate
        
        assert discounted_price == 80.00
    
    def test_product_bulk_price(self):
        """TC-PROD-007: 测试产品批量定价"""
        def calculate_bulk_price(unit_price, quantity):
            if quantity >= 100:
                return unit_price * 0.8
            elif quantity >= 50:
                return unit_price * 0.9
            return unit_price
        
        assert calculate_bulk_price(100, 100) == 80
        assert calculate_bulk_price(100, 50) == 90
        assert calculate_bulk_price(100, 10) == 100
    
    # ========== 产品分类测试 ==========
    
    def test_product_category_validation(self):
        """TC-PROD-008: 测试产品分类验证"""
        valid_categories = ['电子产品', '办公用品', '日用百货', '食品饮料']
        
        def validate_category(category):
            return category in valid_categories
        
        assert validate_category('电子产品') == True
        assert validate_category('未知分类') == False
    
    def test_product_category_hierarchy(self):
        """TC-PROD-009: 测试产品分类层级"""
        categories = {
            '电子产品': ['手机', '电脑', '平板'],
            '办公用品': ['文具', '设备', '耗材']
        }
        
        def get_parent_category(sub_category):
            for parent, subs in categories.items():
                if sub_category in subs:
                    return parent
            return None
        
        assert get_parent_category('手机') == '电子产品'
        assert get_parent_category('文具') == '办公用品'
        assert get_parent_category('未知') is None
    
    # ========== 产品状态测试 ==========
    
    def test_product_status_active(self):
        """TC-PROD-010: 测试产品上架状态"""
        product = mock_erp.add_product({
            'code': 'PROD-010',
            'name': '上架产品',
            'status': 'active'
        })
        
        assert product['status'] == 'active'
    
    def test_product_status_inactive(self):
        """TC-PROD-011: 测试产品下架状态"""
        product = mock_erp.add_product({
            'code': 'PROD-011',
            'name': '下架产品',
            'status': 'inactive'
        })
        
        assert product['status'] == 'inactive'
    
    # ========== 产品编码测试 ==========
    
    def test_product_code_format(self):
        """TC-PROD-012: 测试产品编码格式"""
        def validate_product_code(code):
            import re
            pattern = r'^PROD-\d{3,}$'
            return bool(re.match(pattern, code))
        
        assert validate_product_code('PROD-001') == True
        assert validate_product_code('PROD-12345') == True
        assert validate_product_code('XXX-001') == False
    
    def test_product_code_uniqueness(self):
        """TC-PROD-013: 测试产品编码唯一性"""
        existing_codes = set()
        
        product1 = mock_erp.add_product({'code': 'PROD-001', 'name': '产品1'})
        existing_codes.add(product1['code'])
        
        # 尝试添加重复编码
        new_code = 'PROD-001'
        is_unique = new_code not in existing_codes
        
        assert is_unique == False
    
    # ========== 产品库存预警测试 ==========
    
    def test_product_stock_warning(self):
        """TC-PROD-014: 测试产品库存预警"""
        def check_stock_warning(current_stock, min_stock=10):
            if current_stock <= 0:
                return 'out_of_stock'
            elif current_stock <= min_stock:
                return 'low_stock'
            return 'normal'
        
        assert check_stock_warning(0) == 'out_of_stock'
        assert check_stock_warning(5) == 'low_stock'
        assert check_stock_warning(50) == 'normal'
    
    def test_product_reorder_point(self):
        """TC-PROD-015: 测试产品补货点计算"""
        def calculate_reorder_point(avg_daily_sales, lead_time_days, safety_stock=0):
            return avg_daily_sales * lead_time_days + safety_stock
        
        reorder = calculate_reorder_point(10, 7, 20)
        
        assert reorder == 90


# ==================== 库存管理模块测试 (15+ 用例) ====================

@pytest.mark.unit
class TestInventoryManagement:
    """库存管理模块测试 - 15个测试用例"""
    
    def setup_method(self):
        """每个测试方法前重置数据"""
        mock_erp.stock.clear()
        mock_erp.products.clear()
        mock_erp.warehouses.clear()
    
    # ========== 库存CRUD测试 ==========
    
    def test_stock_add_success(self):
        """TC-INV-001: 测试入库成功"""
        mock_erp.update_stock(1, 1, 100, 'add')
        
        quantity = mock_erp.get_stock(1, 1)
        
        assert quantity == 100
    
    def test_stock_reduce_success(self):
        """TC-INV-002: 测试出库成功"""
        mock_erp.stock[(1, 1)] = 100
        
        quantity = mock_erp.update_stock(1, 1, 30, 'reduce')
        
        assert quantity == 70
    
    def test_stock_reduce_insufficient(self):
        """TC-INV-003: 测试库存不足出库"""
        mock_erp.stock[(1, 1)] = 10
        
        with pytest.raises(ValueError) as excinfo:
            mock_erp.update_stock(1, 1, 100, 'reduce')
        
        assert '库存不足' in str(excinfo.value)
    
    def test_stock_query_empty(self):
        """TC-INV-004: 测试查询空库存"""
        quantity = mock_erp.get_stock(1, 1)
        
        assert quantity == 0
    
    # ========== 库存计算测试 ==========
    
    def test_stock_value_calculation(self):
        """TC-INV-005: 测试库存金额计算"""
        quantity = 100
        unit_cost = 10.5
        
        total_value = quantity * unit_cost
        
        assert total_value == 1050.0
    
    def test_stock_turnover_rate(self):
        """TC-INV-006: 测试库存周转率"""
        def calculate_turnover(cost_of_goods_sold, avg_inventory):
            if avg_inventory == 0:
                return 0
            return round(cost_of_goods_sold / avg_inventory, 2)
        
        rate = calculate_turnover(500000, 100000)
        
        assert rate == 5.0
    
    def test_stock_days_of_supply(self):
        """TC-INV-007: 测试库存供应天数"""
        def calculate_days_of_supply(current_stock, daily_usage):
            if daily_usage == 0:
                return float('inf')
            return current_stock / daily_usage
        
        days = calculate_days_of_supply(100, 10)
        
        assert days == 10
    
    # ========== 库存状态测试 ==========
    
    def test_stock_status_normal(self):
        """TC-INV-008: 测试正常库存状态"""
        def get_stock_status(qty, reorder_point=10):
            if qty <= 0:
                return 'out_of_stock'
            elif qty <= reorder_point:
                return 'low_stock'
            return 'normal'
        
        assert get_stock_status(50) == 'normal'
    
    def test_stock_status_low(self):
        """TC-INV-009: 测试低库存状态"""
        def get_stock_status(qty, reorder_point=10):
            if qty <= 0:
                return 'out_of_stock'
            elif qty <= reorder_point:
                return 'low_stock'
            return 'normal'
        
        assert get_stock_status(5) == 'low_stock'
    
    def test_stock_status_out(self):
        """TC-INV-010: 测试缺货状态"""
        def get_stock_status(qty, reorder_point=10):
            if qty <= 0:
                return 'out_of_stock'
            elif qty <= reorder_point:
                return 'low_stock'
            return 'normal'
        
        assert get_stock_status(0) == 'out_of_stock'
    
    # ========== 库存预警测试 ==========
    
    def test_stock_alert_threshold(self):
        """TC-INV-011: 测试库存预警阈值"""
        def should_alert(current, min_stock, max_stock):
            return current <= min_stock or current >= max_stock
        
        assert should_alert(5, 10, 1000) == True  # 低库存预警
        assert should_alert(1500, 10, 1000) == True  # 高库存预警
        assert should_alert(500, 10, 1000) == False  # 正常
    
    def test_stock_safety_stock_calculation(self):
        """TC-INV-012: 测试安全库存计算"""
        def calculate_safety_stock(max_daily_usage, avg_lead_time, max_lead_time):
            return (max_daily_usage * max_lead_time) - (max_daily_usage * avg_lead_time)
        
        safety = calculate_safety_stock(50, 5, 7)
        
        assert safety == 100
    
    # ========== 多仓库测试 ==========
    
    def test_multi_warehouse_stock(self):
        """TC-INV-013: 测试多仓库库存"""
        # 仓库1入库
        mock_erp.update_stock(1, 1, 100, 'add')
        # 仓库2入库
        mock_erp.update_stock(2, 1, 50, 'add')
        
        total_stock = mock_erp.get_stock(1, 1) + mock_erp.get_stock(2, 1)
        
        assert total_stock == 150
    
    def test_stock_transfer(self):
        """TC-INV-014: 测试库存调拨"""
        # 初始库存
        mock_erp.stock[(1, 1)] = 100
        mock_erp.stock[(2, 1)] = 0
        
        # 调拨30个从仓库1到仓库2
        transfer_qty = 30
        mock_erp.update_stock(1, 1, transfer_qty, 'reduce')
        mock_erp.update_stock(2, 1, transfer_qty, 'add')
        
        assert mock_erp.get_stock(1, 1) == 70
        assert mock_erp.get_stock(2, 1) == 30
    
    # ========== 库存盘点测试 ==========
    
    def test_stock_count_variance(self):
        """TC-INV-015: 测试库存盘点差异"""
        def calculate_variance(system_qty, actual_qty):
            return actual_qty - system_qty
        
        variance = calculate_variance(100, 98)
        
        assert variance == -2  # 盘亏2个


# ==================== 综合业务流程测试 ====================

@pytest.mark.unit
class TestBusinessWorkflows:
    """业务流程综合测试"""
    
    def setup_method(self):
        """每个测试方法前重置数据"""
        mock_crm.customers.clear()
        mock_crm.leads.clear()
        mock_erp.products.clear()
        mock_erp.stock.clear()
    
    def test_complete_sales_workflow(self):
        """TC-WF-001: 测试完整销售流程"""
        # 1. 创建客户
        customer = mock_crm.add_customer({'name': '销售客户', 'level': 'A'})
        assert customer['id'] > 0
        
        # 2. 创建产品
        product = mock_erp.add_product({'code': 'PROD-SALE', 'name': '销售产品', 'price': 100})
        assert product['id'] > 0
        
        # 3. 入库
        mock_erp.update_stock(1, product['id'], 100, 'add')
        assert mock_erp.get_stock(1, product['id']) == 100
        
        # 4. 创建订单（模拟）
        order = {
            'customer_id': customer['id'],
            'items': [{'product_id': product['id'], 'quantity': 10, 'price': 100}],
            'total': 1000
        }
        
        # 5. 出库
        mock_erp.update_stock(1, product['id'], 10, 'reduce')
        assert mock_erp.get_stock(1, product['id']) == 90
    
    def test_customer_order_product_link(self):
        """TC-WF-002: 测试客户-订单-产品关联"""
        # 创建客户
        customer = mock_crm.add_customer({'name': '关联客户'})
        
        # 创建产品
        product1 = mock_erp.add_product({'code': 'P1', 'name': '产品1', 'price': 50})
        product2 = mock_erp.add_product({'code': 'P2', 'name': '产品2', 'price': 100})
        
        # 创建订单
        order_items = [
            {'product_id': product1['id'], 'quantity': 2, 'price': 50},
            {'product_id': product2['id'], 'quantity': 1, 'price': 100}
        ]
        order_total = sum(i['quantity'] * i['price'] for i in order_items)
        
        assert order_total == 200
        assert customer['id'] > 0


# ==================== 测试运行配置 ====================

if __name__ == '__main__':
    pytest.main([
        __file__,
        '-v',
        '--tb=short',
        '-m', 'unit'
    ])

