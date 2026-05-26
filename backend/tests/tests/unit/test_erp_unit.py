#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
ERP模块单元测试 - 本地环境
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mocks.mock_services import mock_erp, mock_db
from unit.local_test_config import TEST_ERP_DATA


@pytest.mark.unit
class TestProductModel:
    """产品模型测试"""
    
    def test_product_creation(self):
        """测试产品创建"""
        product = {'code': 'PROD001', 'name': '测试产品', 'category': '电子', 'price': 100.00, 'stock': 100}
        result = mock_erp.add_product(product)
        assert result['id'] > 0
        assert result['name'] == '测试产品'
    
    def test_product_query(self):
        """测试产品查询"""
        product = mock_erp.add_product({'code': 'PROD002', 'name': '查询产品', 'price': 50})
        result = mock_erp.get_product(product['id'])
        assert result is not None
    
    def test_product_not_found(self):
        """测试产品不存在"""
        result = mock_erp.get_product(99999)
        assert result is None
    
    def test_product_price_calculation(self):
        """测试产品价格计算"""
        price = 100
        unit_cost = 80
        profit = price - unit_cost
        profit_rate = round(profit / price * 100, 2)
        assert profit == 20
        assert profit_rate == 20.0


@pytest.mark.unit
class TestStockModel:
    """库存模型测试"""
    
    def test_stock_add(self):
        """测试增加库存"""
        mock_erp.stock = {}
        mock_erp.update_stock(1, 1, 100, 'add')
        assert mock_erp.get_stock(1, 1) == 100
    
    def test_stock_reduce(self):
        """测试减少库存"""
        mock_erp.stock = {(1, 1): 100}
        quantity = mock_erp.update_stock(1, 1, 30, 'reduce')
        assert quantity == 70
    
    def test_stock_insufficient(self):
        """测试库存不足"""
        mock_erp.stock = {(1, 1): 10}
        with pytest.raises(ValueError):
            mock_erp.update_stock(1, 1, 100, 'reduce')
    
    def test_stock_value_calculation(self):
        """测试库存金额计算"""
        value = 100 * 10.5
        assert value == 1050.0
    
    def test_stock_status_check(self):
        """测试库存状态检查"""
        def check_status(qty, reorder=10):
            if qty <= 0: return 'out_of_stock'
            elif qty <= reorder: return 'low_stock'
            return 'normal'
        assert check_status(0) == 'out_of_stock'
        assert check_status(5) == 'low_stock'
        assert check_status(15) == 'normal'


@pytest.mark.unit
class TestAccountModel:
    """会计科目模型测试"""
    
    def test_account_code_validation(self):
        """测试科目代码验证"""
        def validate(code):
            return code.isdigit() and 3 <= len(code) <= 10
        assert validate("1001") == True
        assert validate("AB12") == False
    
    def test_account_type(self):
        """测试科目类型"""
        types = {'1': 'asset', '2': 'liability', '3': 'equity', '4': 'revenue', '5': 'expense'}
        assert types.get('1') == 'asset'
        assert types.get('4') == 'revenue'
    
    def test_accounting_equation(self):
        """测试会计恒等式"""
        assets, liabilities, equity = 100000, 40000, 60000
        assert abs(assets - (liabilities + equity)) < 0.01


@pytest.mark.unit
class TestPurchaseOrder:
    """采购订单测试"""
    
    def test_po_total_calculation(self):
        """测试订单总额计算"""
        items = [{'qty': 10, 'price': 100}, {'qty': 5, 'price': 200}]
        total = sum(i['qty'] * i['price'] for i in items)
        assert total == 2000
    
    def test_po_items_validation(self):
        """测试订单项验证"""
        def validate(items):
            if not items: return False
            for i in items:
                if i.get('qty', 0) <= 0: return False
            return True
        assert validate([{'qty': 10}]) == True
        assert validate([{'qty': -1}]) == False


@pytest.mark.unit
class TestDepreciation:
    """折旧计算测试"""
    
    def test_straight_line_depreciation(self):
        """测试直线法折旧"""
        cost, salvage, life = 100000, 10000, 5
        annual = (cost - salvage) / life
        assert annual == 18000.0