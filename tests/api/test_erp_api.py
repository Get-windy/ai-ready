#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API自动化测试 - ERP模块API
测试范围: 产品、库存、采购订单、销售订单、财务科目
"""

import pytest
import json
from typing import Dict, Any
import random


class TestERPProductAPI:
    """ERP产品API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        """带认证的API客户端"""
        return api_client
    
    @pytest.mark.smoke
    def test_product_list_page(self, client):
        """[冒烟] 产品分页查询"""
        response = client.get('/api/product/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
        data = response.json()
        assert 'code' in data
        assert data['code'] == 200
    
    def test_product_create(self, client):
        """产品创建"""
        product_data = {
            'name': f'测试产品_{random.randint(1000,9999)}',
            'code': f'PRD{random.randint(10000,99999)}',
            'category': '电子产品',
            'price': random.uniform(100, 5000),
            'unit': '件',
            'description': '自动化测试创建的产品'
        }
        response = client.post('/api/product/save', json=product_data)
        assert response.status_code in [200, 201]
    
    def test_product_update(self, client):
        """产品更新"""
        update_data = {
            'id': 1,
            'name': '更新后的产品名称',
            'price': 999.99
        }
        response = client.put('/api/product/update', json=update_data)
        assert response.status_code in [200, 204]
    
    def test_product_delete(self, client):
        """产品删除"""
        response = client.delete('/api/product/delete', params={'id': 999})
        # 允许404(不存在)或200(成功)
        assert response.status_code in [200, 204, 404]
    
    def test_product_search(self, client):
        """产品搜索"""
        response = client.get('/api/product/search', params={'keyword': '测试'})
        assert response.status_code == 200


class TestERPStockAPI:
    """ERP库存API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_stock_list(self, client):
        """[冒烟] 库存列表查询"""
        response = client.get('/api/stock/list', params={'pageNum': 1, 'pageSize': 20})
        assert response.status_code == 200
    
    def test_stock_by_product(self, client):
        """按产品查询库存"""
        response = client.get('/api/stock/product', params={'productId': 1})
        assert response.status_code in [200, 404]
    
    def test_stock_in(self, client):
        """库存入库"""
        stock_data = {
            'productId': 1,
            'quantity': 100,
            'warehouseId': 1,
            'batchNo': f'B{random.randint(10000,99999)}',
            'remark': '自动化测试入库'
        }
        response = client.post('/api/stock/in', json=stock_data)
        assert response.status_code in [200, 201]
    
    def test_stock_out(self, client):
        """库存出库"""
        stock_data = {
            'productId': 1,
            'quantity': 10,
            'warehouseId': 1,
            'remark': '自动化测试出库'
        }
        response = client.post('/api/stock/out', json=stock_data)
        assert response.status_code in [200, 201, 400]  # 可能库存不足
    
    def test_stock_check(self, client):
        """库存盘点检查"""
        response = client.get('/api/stock/check', params={'warehouseId': 1})
        assert response.status_code in [200, 404]


class TestERPPurchaseAPI:
    """ERP采购订单API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_purchase_order_list(self, client):
        """[冒烟] 采购订单列表"""
        response = client.get('/api/purchase/order/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
    
    def test_purchase_order_create(self, client):
        """创建采购订单"""
        order_data = {
            'supplierId': 1,
            'items': [
                {'productId': 1, 'quantity': 50, 'price': 100.0},
                {'productId': 2, 'quantity': 30, 'price': 200.0}
            ],
            'expectedDate': '2026-04-15',
            'remark': '自动化测试采购订单'
        }
        response = client.post('/api/purchase/order/save', json=order_data)
        assert response.status_code in [200, 201]
    
    def test_purchase_order_approve(self, client):
        """采购订单审批"""
        response = client.post('/api/purchase/order/approve', params={'orderId': 1})
        assert response.status_code in [200, 400, 404]
    
    def test_purchase_order_cancel(self, client):
        """采购订单取消"""
        response = client.post('/api/purchase/order/cancel', params={'orderId': 999})
        assert response.status_code in [200, 400, 404]


class TestERPSalesAPI:
    """ERP销售订单API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_sales_order_list(self, client):
        """[冒烟] 销售订单列表"""
        response = client.get('/api/sales/order/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
    
    def test_sales_order_create(self, client):
        """创建销售订单"""
        order_data = {
            'customerId': 1,
            'items': [
                {'productId': 1, 'quantity': 5, 'price': 150.0}
            ],
            'deliveryDate': '2026-04-10',
            'remark': '自动化测试销售订单'
        }
        response = client.post('/api/sales/order/save', json=order_data)
        assert response.status_code in [200, 201]
    
    def test_sales_order_deliver(self, client):
        """销售订单发货"""
        response = client.post('/api/sales/order/deliver', params={'orderId': 1})
        assert response.status_code in [200, 400, 404]


class TestERPAccountAPI:
    """ERP财务科目API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_account_list(self, client):
        """[冒烟] 会计科目列表"""
        response = client.get('/api/account/list')
        assert response.status_code == 200
    
    def test_account_tree(self, client):
        """会计科目树形结构"""
        response = client.get('/api/account/tree')
        assert response.status_code in [200, 404]
    
    def test_account_balance(self, client):
        """科目余额查询"""
        response = client.get('/api/account/balance', params={'accountId': 1})
        assert response.status_code in [200, 404]


class TestERPReportAPI:
    """ERP报表API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    def test_purchase_report(self, client):
        """采购报表"""
        response = client.get('/api/report/purchase', params={'month': '2026-03'})
        assert response.status_code in [200, 404]
    
    def test_sales_report(self, client):
        """销售报表"""
        response = client.get('/api/report/sales', params={'month': '2026-03'})
        assert response.status_code in [200, 404]
    
    def test_inventory_report(self, client):
        """库存报表"""
        response = client.get('/api/report/inventory')
        assert response.status_code in [200, 404]


# pytest标记
pytestmark = pytest.mark.erp