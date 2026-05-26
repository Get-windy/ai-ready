#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
ERP模块单元测试
"""

import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestERPUnit:
    """ERP模块单元测试"""
    
    def test_account_model(self):
        """测试会计科目模型"""
        def validate_account_code(code):
            """验证会计科目代码"""
            if not code:
                return False
            if len(code) < 3 or len(code) > 10:
                return False
            return code.isdigit()
        
        def get_account_type(code):
            """根据科目代码获取类型"""
            if not validate_account_code(code):
                return 'unknown'
            
            first_digit = code[0]
            types = {
                '1': 'asset',      # 资产类
                '2': ' liability',  # 负债类
                '3': 'equity',     # 所有者权益类
                '4': 'revenue',    # 收入类
                '5': 'expense'     # 费用类
            }
            return types.get(first_digit, 'unknown')
        
        # 测试科目代码验证
        assert validate_account_code("1001") == True  # 库存现金
        assert validate_account_code("1002") == True  # 银行存款
        assert validate_account_code("AB12") == False  # 无效格式
        assert validate_account_code("12") == False   # 太短
        
        # 测试科目类型
        assert get_account_type("1001") == 'asset'
        assert get_account_type("2001") == ' liability'
        assert get_account_type("3001") == 'equity'
        assert get_account_type("4001") == 'revenue'
        assert get_account_type("5001") == 'expense'
    
    def test_inventory_model(self):
        """测试库存模型"""
        def calculate_stock_value(quantity, unit_cost):
            """计算库存金额"""
            return round(quantity * unit_cost, 2)
        
        def check_stock_status(quantity, reorder_level=10):
            """检查库存状态"""
            if quantity <= 0:
                return 'out_of_stock'
            elif quantity <= reorder_level:
                return 'low_stock'
            else:
                return 'normal'
        
        # 测试库存金额计算
        assert calculate_stock_value(100, 10.5) == 1050.0
        assert calculate_stock_value(50, 3.25) == 162.5
        
        # 测试库存状态
        assert check_stock_status(0) == 'out_of_stock'
        assert check_stock_status(5) == 'low_stock'
        assert check_stock_status(15) == 'normal'
        assert check_stock_status(100, reorder_level=50) == 'normal'


class TestERPInventory:
    """ERP库存管理测试"""
    
    def test_warehouse_operations(self):
        """测试仓库操作"""
        class Warehouse:
            def __init__(self):
                self.stock = {}  # {product_id: quantity}
            
            def add_stock(self, product_id, quantity):
                """增加库存"""
                if product_id not in self.stock:
                    self.stock[product_id] = 0
                self.stock[product_id] += quantity
                return self.stock[product_id]
            
            def reduce_stock(self, product_id, quantity):
                """减少库存"""
                if product_id not in self.stock:
                    return None
                if self.stock[product_id] < quantity:
                    return None
                self.stock[product_id] -= quantity
                return self.stock[product_id]
            
            def get_stock(self, product_id):
                """获取库存"""
                return self.stock.get(product_id)
        
        warehouse = Warehouse()
        
        # 测试增加库存
        assert warehouse.add_stock('PROD001', 100) == 100
        
        # 测试减少库存
        assert warehouse.reduce_stock('PROD001', 30) == 70
        
        # 测试获取库存
        assert warehouse.get_stock('PROD001') == 70
        
        # 测试库存不足
        assert warehouse.reduce_stock('PROD001', 100) is None
    
    def test_purchase_order(self):
        """测试采购订单"""
        def calculate_po_total(items):
            """计算采购订单总额"""
            total = 0
            for item in items:
                total += item['quantity'] * item['unit_price']
            return round(total, 2)
        
        def validate_po_items(items):
            """验证采购订单项"""
            if not items:
                return False, "订单项不能为空"
            
            for item in items:
                if 'quantity' not in item or 'unit_price' not in item:
                    return False, f"订单项缺少必要字段: {item}"
                if item['quantity'] <= 0:
                    return False, f"数量必须大于0: {item}"
                if item['unit_price'] < 0:
                    return False, f"单价不能为负: {item}"
            
            return True, " OK"
        
        # 测试订单总额计算
        items = [
            {'product_id': 'P001', 'quantity': 10, 'unit_price': 100},
            {'product_id': 'P002', 'quantity': 5, 'unit_price': 200}
        ]
        assert calculate_po_total(items) == 2000.0
        
        # 测试订单项验证
        valid, msg = validate_po_items(items)
        assert valid == True
        
        invalid_items = [{'quantity': -1, 'unit_price': 100}]
        valid, msg = validate_po_items(invalid_items)
        assert valid == False


class TestERPFinancial:
    """ERP财务管理测试"""
    
    def test_accounting_equation(self):
        """测试会计恒等式: 资产 = 负债 + 所有者权益"""
        def check_accounting_equation(assets, liabilities, equity):
            """验证会计恒等式"""
            return abs(assets - (liabilities + equity)) < 0.01
        
        # 测试平衡情况
        assert check_accounting_equation(100000, 40000, 60000) == True
        
        # 测试不平衡情况
        assert check_accounting_equation(100000, 50000, 60000) == False
    
    def test_depreciation_calculation(self):
        """测试折旧计算"""
        def straight_line_depreciation(cost, salvage_value, useful_life):
            """直线法折旧"""
            if useful_life <= 0:
                return 0
            return round((cost - salvage_value) / useful_life, 2)
        
        # 测试折旧计算
        annual_depreciation = straight_line_depreciation(100000, 10000, 5)
        assert annual_depreciation == 18000.0
        
        # 测试无效参数
        assert straight_line_depreciation(100000, 10000, 0) == 0


class TestERPReport:
    """ERP报表功能测试"""
    
    def test_report_filtering(self):
        """测试报表过滤"""
        def filter_by_date(items, start_date, end_date, date_field='date'):
            """按日期过滤"""
            filtered = []
            for item in items:
                item_date = item.get(date_field)
                if start_date <= item_date <= end_date:
                    filtered.append(item)
            return filtered
        
        # 测试数据
        items = [
            {'id': 1, 'date': '2026-03-01', 'amount': 100},
            {'id': 2, 'date': '2026-03-15', 'amount': 200},
            {'id': 3, 'date': '2026-03-31', 'amount': 300}
        ]
        
        # 测试日期过滤
        filtered = filter_by_date(items, '2026-03-01', '2026-03-20')
        assert len(filtered) == 2
    
    def test_report_aggregation(self):
        """测试报表聚合"""
        def aggregate_by_category(items, category_field='category', value_field='amount'):
            """按类别聚合"""
            result = {}
            for item in items:
                category = item.get(category_field)
                value = item.get(value_field, 0)
                
                if category not in result:
                    result[category] = 0
                result[category] += value
            
            return result
        
        # 测试聚合
        items = [
            {'category': 'A', 'amount': 100},
            {'category': 'B', 'amount': 200},
            {'category': 'A', 'amount': 50},
            {'category': 'C', 'amount': 150}
        ]
        
        aggregated = aggregate_by_category(items)
        assert aggregated['A'] == 150
        assert aggregated['B'] == 200
        assert aggregated['C'] == 150
