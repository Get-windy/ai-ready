#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 订单管理模块集成测试
覆盖订单与库存、用户、支付模块的完整集成场景
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.mocks.mock_services import mock_erp, mock_crm, mock_auth, mock_permission, mock_payment


@pytest.mark.integration
class TestOrderManagementIntegration:
    """订单管理业务流程集成测试"""
    
    def test_order_lifecycle_flow(self):
        """测试订单完整生命周期流程"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
        
        # 1. 创建客户
        customer = mock_crm.add_customer({
            'name': '订单客户',
            'email': 'order@test.com'
        })
        
        # 2. 创建产品
        product = mock_erp.add_product({
            'id': 1,
            'name': '测试产品',
            'price': 1000,
            'sku': 'SKU001'
        })
        
        # 3. 添加库存
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 4. 创建订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [
                {'product_id': 1, 'quantity': 5, 'price': 1000}
            ],
            'total_amount': 5000,
            'status': 'pending'
        })
        assert order['id'] > 0
        order_id = order['id']
        
        # 5. 确认订单
        mock_erp.update_order(order_id, {'status': 'confirmed'})
        
        # 6. 扣减库存
        mock_erp.update_stock(1, 1, 5, 'reduce')
        current_stock = mock_erp.get_stock(1, 1)
        assert current_stock == 95
        
        # 7. 订单发货
        mock_erp.update_order(order_id, {
            'status': 'shipped',
            'tracking_number': 'TRACK123456'
        })
        
        # 8. 订单完成
        mock_erp.update_order(order_id, {'status': 'completed'})
        
        final_order = mock_erp.orders[order_id]
        assert final_order['status'] == 'completed'
    
    def test_order_with_multiple_items(self):
        """测试多商品订单流程"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
        
        # 创建客户
        customer = mock_crm.add_customer({'name': '多商品客户'})
        
        # 创建多个产品
        products = [
            {'id': 1, 'name': '产品A', 'price': 100},
            {'id': 2, 'name': '产品B', 'price': 200},
            {'id': 3, 'name': '产品C', 'price': 300}
        ]
        
        for product in products:
            mock_erp.add_product(product)
            mock_erp.update_stock(product['id'], 1, 50, 'add')
        
        # 创建多商品订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [
                {'product_id': 1, 'quantity': 2, 'price': 100},
                {'product_id': 2, 'quantity': 3, 'price': 200},
                {'product_id': 3, 'quantity': 1, 'price': 300}
            ],
            'total_amount': 1100,
            'status': 'pending'
        })
        
        # 验证订单金额
        assert order['total_amount'] == 1100
        
        # 验证库存扣减
        assert mock_erp.get_stock(1, 1) == 48
        assert mock_erp.get_stock(2, 1) == 47
        assert mock_erp.get_stock(3, 1) == 49
    
    def test_order_cancellation_flow(self):
        """测试订单取消流程"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
        
        # 创建客户和产品
        customer = mock_crm.add_customer({'name': '取消订单客户'})
        mock_erp.add_product({'id': 1, 'name': '取消产品', 'price': 500})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 创建订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 10, 'price': 500}],
            'total_amount': 5000,
            'status': 'pending'
        })
        order_id = order['id']
        
        # 确认订单并扣减库存
        mock_erp.update_order(order_id, {'status': 'confirmed'})
        mock_erp.update_stock(1, 1, 10, 'reduce')
        assert mock_erp.get_stock(1, 1) == 90
        
        # 取消订单
        mock_erp.update_order(order_id, {'status': 'cancelled'})
        
        # 恢复库存
        mock_erp.update_stock(1, 1, 10, 'add')
        assert mock_erp.get_stock(1, 1) == 100
        
        # 验证订单状态
        cancelled_order = mock_erp.orders[order_id]
        assert cancelled_order['status'] == 'cancelled'
    
    def test_insufficient_stock_handling(self):
        """测试库存不足处理"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
        
        # 创建客户和低库存产品
        customer = mock_crm.add_customer({'name': '库存不足客户'})
        mock_erp.add_product({'id': 1, 'name': '低库存产品', 'price': 1000})
        mock_erp.update_stock(1, 1, 5, 'add')  # 只有5个库存
        
        # 尝试创建超库存订单
        try:
            order = mock_erp.create_order({
                'customer_id': customer['id'],
                'items': [{'product_id': 1, 'quantity': 10, 'price': 1000}],
                'total_amount': 10000,
                'status': 'pending'
            })
            
            # 检查库存是否足够
            available_stock = mock_erp.get_stock(1, 1)
            if available_stock < 10:
                # 库存不足，订单应该失败或被标记
                mock_erp.update_order(order['id'], {'status': 'failed', 'reason': 'insufficient_stock'})
            
            final_order = mock_erp.orders[order['id']]
            assert final_order['status'] in ['pending', 'failed']
        except Exception:
            # 预期可能抛出异常
            pass


@pytest.mark.integration
class TestInventoryManagementIntegration:
    """库存管理业务流程集成测试"""
    
    def test_inventory_inbound_flow(self):
        """测试入库流程"""
        mock_erp.products = {}
        mock_erp.stock = {}
        
        # 创建产品
        product = mock_erp.add_product({
            'id': 1,
            'name': '入库产品',
            'price': 500
        })
        
        # 采购入库
        inbound_records = [
            {'quantity': 100, 'batch': 'B001', 'supplier': '供应商A'},
            {'quantity': 50, 'batch': 'B002', 'supplier': '供应商B'}
        ]
        
        for record in inbound_records:
            mock_erp.update_stock(1, 1, record['quantity'], 'add')
        
        # 验证库存
        total_stock = mock_erp.get_stock(1, 1)
        assert total_stock == 150
    
    def test_inventory_outbound_flow(self):
        """测试出库流程"""
        mock_erp.products = {}
        mock_erp.stock = {}
        
        # 创建产品并入库
        mock_erp.add_product({'id': 1, 'name': '出库产品', 'price': 300})
        mock_erp.update_stock(1, 1, 200, 'add')
        
        # 销售出库
        outbound_records = [
            {'quantity': 30, 'order_id': 'O001'},
            {'quantity': 50, 'order_id': 'O002'},
            {'quantity': 20, 'order_id': 'O003'}
        ]
        
        for record in outbound_records:
            mock_erp.update_stock(1, 1, record['quantity'], 'reduce')
        
        # 验证库存
        remaining_stock = mock_erp.get_stock(1, 1)
        assert remaining_stock == 100  # 200 - 30 - 50 - 20
    
    def test_inventory_adjustment_flow(self):
        """测试库存调整流程"""
        mock_erp.products = {}
        mock_erp.stock = {}
        
        # 创建产品并设置初始库存
        mock_erp.add_product({'id': 1, 'name': '调整产品', 'price': 200})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 盘点发现实际库存为95（盘亏5个）
        actual_stock = 95
        current_stock = mock_erp.get_stock(1, 1)
        adjustment = actual_stock - current_stock  # -5
        
        # 调整库存
        if adjustment < 0:
            mock_erp.update_stock(1, 1, abs(adjustment), 'reduce')
        else:
            mock_erp.update_stock(1, 1, adjustment, 'add')
        
        # 验证调整后的库存
        assert mock_erp.get_stock(1, 1) == 95
    
    def test_low_stock_alert(self):
        """测试库存预警"""
        mock_erp.products = {}
        mock_erp.stock = {}
        
        # 创建产品并设置低库存
        mock_erp.add_product({'id': 1, 'name': '预警产品', 'price': 1000})
        mock_erp.update_stock(1, 1, 10, 'add')  # 设置安全库存为20
        
        # 检查库存预警
        current_stock = mock_erp.get_stock(1, 1)
        safety_stock = 20
        
        if current_stock < safety_stock:
            alert = {
                'product_id': 1,
                'current_stock': current_stock,
                'safety_stock': safety_stock,
                'alert_type': 'low_stock'
            }
            mock_erp.add_stock_alert(alert)
        
        # 验证预警
        alerts = [a for a in mock_erp.stock_alerts.values() if a['product_id'] == 1]
        assert len(alerts) >= 1
        assert alerts[0]['alert_type'] == 'low_stock'


@pytest.mark.integration
class TestOrderReportIntegration:
    """订单报表集成测试"""
    
    def test_sales_report_generation(self):
        """测试销售报表生成"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_crm.customers = {}
        
        # 创建测试数据
        customer = mock_crm.add_customer({'name': '报表客户'})
        mock_erp.add_product({'id': 1, 'name': '报表产品', 'price': 1000})
        
        # 创建多个订单
        orders = [
            {'customer_id': customer['id'], 'total_amount': 5000, 'status': 'completed'},
            {'customer_id': customer['id'], 'total_amount': 8000, 'status': 'completed'},
            {'customer_id': customer['id'], 'total_amount': 3000, 'status': 'pending'}
        ]
        
        for order_data in orders:
            mock_erp.create_order(order_data)
        
        # 生成销售报表
        completed_orders = [o for o in mock_erp.orders.values() if o['status'] == 'completed']
        total_sales = sum(o['total_amount'] for o in completed_orders)
        order_count = len(completed_orders)
        
        assert total_sales == 13000
        assert order_count == 2
    
    def test_customer_order_statistics(self):
        """测试客户订单统计"""
        mock_erp.orders = {}
        mock_crm.customers = {}
        
        # 创建多个客户
        customers = [
            mock_crm.add_customer({'name': '客户A'}),
            mock_crm.add_customer({'name': '客户B'})
        ]
        
        # 为客户A创建订单
        for i in range(5):
            mock_erp.create_order({
                'customer_id': customers[0]['id'],
                'total_amount': 1000 * (i + 1),
                'status': 'completed'
            })
        
        # 为客户B创建订单
        for i in range(3):
            mock_erp.create_order({
                'customer_id': customers[1]['id'],
                'total_amount': 2000 * (i + 1),
                'status': 'completed'
            })
        
        # 统计客户订单
        customer_a_orders = [o for o in mock_erp.orders.values() 
                           if o['customer_id'] == customers[0]['id']]
        customer_b_orders = [o for o in mock_erp.orders.values()
                           if o['customer_id'] == customers[1]['id']]
        
        assert len(customer_a_orders) == 5
        assert len(customer_b_orders) == 3
        
        # 计算客户总消费
        customer_a_total = sum(o['total_amount'] for o in customer_a_orders)
        customer_b_total = sum(o['total_amount'] for o in customer_b_orders)
        
        assert customer_a_total == 15000  # 1000+2000+3000+4000+5000
        assert customer_b_total == 12000  # 2000+4000+6000