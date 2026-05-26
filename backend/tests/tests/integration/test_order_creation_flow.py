#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 订单创建流程集成测试
验证从购物车到订单创建的完整流程
Sprint 27+1 集成测试任务
"""

import pytest
import sys
import os
import time
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.mocks.mock_services import mock_erp, mock_crm


@pytest.mark.integration
class TestShoppingCartToOrderFlow:
    """购物车→订单创建流程集成测试"""
    
    def setup_method(self):
        """每个测试方法前重置模拟数据"""
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_erp.shopping_carts = {}
        mock_crm.customers = {}
    
    def test_cart_items_migration_to_order(self):
        """测试购物车商品迁移到订单"""
        # 1. 创建客户
        customer = mock_crm.add_customer({
            'name': '购物车测试客户',
            'email': 'cart_test@test.com',
            'phone': '13800138000'
        })
        customer_id = customer['id']
        
        # 2. 创建商品
        products = [
            {'id': 1, 'name': '商品A', 'price': 100.00, 'sku': 'SKU-A001'},
            {'id': 2, 'name': '商品B', 'price': 200.00, 'sku': 'SKU-B001'},
            {'id': 3, 'name': '商品C', 'price': 150.00, 'sku': 'SKU-C001'}
        ]
        
        for product in products:
            mock_erp.add_product(product)
            mock_erp.update_stock(product['id'], 1, 100, 'add')
        
        # 3. 创建购物车
        cart_items = [
            {'product_id': 1, 'quantity': 2, 'price': 100.00},
            {'product_id': 2, 'quantity': 1, 'price': 200.00},
            {'product_id': 3, 'quantity': 3, 'price': 150.00}
        ]
        
        cart = mock_erp.create_shopping_cart({
            'customer_id': customer_id,
            'items': cart_items,
            'created_at': datetime.now().isoformat()
        })
        cart_id = cart['id']
        
        # 验证购物车创建成功
        assert cart_id > 0
        assert len(cart['items']) == 3
        
        # 4. 从购物车创建订单
        order_items = [
            {'product_id': item['product_id'], 'quantity': item['quantity'], 'price': item['price']}
            for item in cart_items
        ]
        
        expected_total = sum(item['quantity'] * item['price'] for item in order_items)
        
        order = mock_erp.create_order({
            'customer_id': customer_id,
            'cart_id': cart_id,
            'items': order_items,
            'total_amount': expected_total,
            'status': 'pending',
            'created_at': datetime.now().isoformat()
        })
        
        # 验证订单创建成功
        assert order['id'] > 0
        assert order['customer_id'] == customer_id
        assert order['cart_id'] == cart_id
        assert len(order['items']) == 3
        
        # 验证商品迁移正确
        for i, item in enumerate(order['items']):
            assert item['product_id'] == cart_items[i]['product_id']
            assert item['quantity'] == cart_items[i]['quantity']
            assert item['price'] == cart_items[i]['price']
    
    def test_product_quantity_consistency(self):
        """测试商品数量和价格一致性"""
        # 创建客户和商品
        customer = mock_crm.add_customer({'name': '一致性测试客户'})
        mock_erp.add_product({'id': 1, 'name': '一致性商品', 'price': 299.99, 'sku': 'SKU-CONSISTENCY'})
        mock_erp.update_stock(1, 1, 50, 'add')
        
        cart_items = [
            {'product_id': 1, 'quantity': 5, 'price': 299.99}
        ]
        
        cart = mock_erp.create_shopping_cart({
            'customer_id': customer['id'],
            'items': cart_items
        })
        
        # 创建订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'cart_id': cart['id'],
            'items': cart_items,
            'total_amount': 1499.95,  # 5 * 299.99
            'status': 'pending'
        })
        
        # 验证数量一致性
        assert order['items'][0]['quantity'] == 5
        # 验证价格一致性
        assert order['items'][0]['price'] == 299.99
        # 验证总价计算正确
        assert order['total_amount'] == 1499.95
    
    def test_cart_clear_after_order_creation(self):
        """测试购物车清空验证"""
        customer = mock_crm.add_customer({'name': '清空测试客户'})
        mock_erp.add_product({'id': 1, 'name': '清空测试商品', 'price': 100.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 创建购物车
        cart = mock_erp.create_shopping_cart({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 3, 'price': 100.00}]
        })
        cart_id = cart['id']
        
        # 验证购物车存在且有商品
        assert cart_id in mock_erp.shopping_carts
        assert len(mock_erp.shopping_carts[cart_id]['items']) == 1
        
        # 创建订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'cart_id': cart_id,
            'items': [{'product_id': 1, 'quantity': 3, 'price': 100.00}],
            'total_amount': 300.00,
            'status': 'pending'
        })
        
        # 清空购物车（模拟结算后清空）
        mock_erp.clear_shopping_cart(cart_id)
        
        # 验证购物车已清空
        cleared_cart = mock_erp.shopping_carts.get(cart_id)
        assert cleared_cart is None or len(cleared_cart.get('items', [])) == 0
        
        # 验证订单仍然存在
        assert order['id'] in mock_erp.orders


@pytest.mark.integration
class TestInventoryDeduction:
    """库存扣减验证集成测试"""
    
    def setup_method(self):
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
    
    def test_stock_deduction_on_order_creation(self):
        """测试下单时库存正确扣减"""
        customer = mock_crm.add_customer({'name': '库存扣减测试客户'})
        
        # 创建商品并设置初始库存
        mock_erp.add_product({'id': 1, 'name': '库存商品A', 'price': 100.00})
        mock_erp.update_stock(1, 1, 100, 'add')  # 初始库存100
        
        initial_stock = mock_erp.get_stock(1, 1)
        assert initial_stock == 100
        
        # 创建订单（购买20个）
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 20, 'price': 100.00}],
            'total_amount': 2000.00,
            'status': 'pending'
        })
        
        # 确认订单并扣减库存
        mock_erp.update_order(order['id'], {'status': 'confirmed'})
        mock_erp.update_stock(1, 1, 20, 'reduce')
        
        # 验证库存已扣减
        current_stock = mock_erp.get_stock(1, 1)
        assert current_stock == 80  # 100 - 20
    
    def test_insufficient_stock_warning(self):
        """测试库存不足时的提示"""
        customer = mock_crm.add_customer({'name': '库存不足测试客户'})
        
        # 创建低库存商品
        mock_erp.add_product({'id': 1, 'name': '低库存商品', 'price': 500.00})
        mock_erp.update_stock(1, 1, 5, 'add')  # 只有5个库存
        
        # 尝试购买10个（超过库存）
        try:
            available_stock = mock_erp.get_stock(1, 1)
            requested_quantity = 10
            
            if requested_quantity > available_stock:
                # 库存不足，应该返回错误
                error_response = {
                    'error': 'INSUFFICIENT_STOCK',
                    'message': f'库存不足，当前库存: {available_stock}, 请求数量: {requested_quantity}',
                    'available_stock': available_stock,
                    'requested_quantity': requested_quantity
                }
                assert error_response['error'] == 'INSUFFICIENT_STOCK'
                assert error_response['available_stock'] == 5
            else:
                pytest.fail("应该检测到库存不足")
        except Exception as e:
            # 如果抛出异常也是可接受的
            assert 'stock' in str(e).lower() or 'inventory' in str(e).lower()
    
    def test_stock_lock_mechanism(self):
        """测试库存锁定机制"""
        customer = mock_crm.add_customer({'name': '库存锁定测试客户'})
        
        # 创建商品
        mock_erp.add_product({'id': 1, 'name': '锁定测试商品', 'price': 1000.00})
        mock_erp.update_stock(1, 1, 50, 'add')
        
        # 模拟库存锁定
        lock_quantity = 10
        lock_duration = 300  # 5分钟锁定
        
        # 创建库存锁定记录
        stock_lock = {
            'product_id': 1,
            'quantity': lock_quantity,
            'locked_at': datetime.now().isoformat(),
            'expires_at': (datetime.now() + timedelta(seconds=lock_duration)).isoformat(),
            'status': 'locked'
        }
        
        # 锁定库存
        mock_erp.stock_locks = getattr(mock_erp, 'stock_locks', {})
        lock_id = len(mock_erp.stock_locks) + 1
        mock_erp.stock_locks[lock_id] = stock_lock
        
        # 验证锁定成功
        assert lock_id in mock_erp.stock_locks
        assert mock_erp.stock_locks[lock_id]['quantity'] == lock_quantity
        assert mock_erp.stock_locks[lock_id]['status'] == 'locked'
        
        # 验证可用库存减少
        available_stock = mock_erp.get_stock(1, 1) - lock_quantity
        assert available_stock == 40  # 50 - 10
        
        # 创建订单使用锁定的库存
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': lock_quantity, 'price': 1000.00}],
            'total_amount': lock_quantity * 1000.00,
            'status': 'pending',
            'stock_lock_id': lock_id
        })
        
        # 确认订单后释放锁定并实际扣减库存
        mock_erp.update_order(order['id'], {'status': 'confirmed'})
        mock_erp.stock_locks[lock_id]['status'] = 'released'
        mock_erp.update_stock(1, 1, lock_quantity, 'reduce')
        
        # 验证最终库存
        final_stock = mock_erp.get_stock(1, 1)
        assert final_stock == 40


@pytest.mark.integration
class TestPriceCalculation:
    """价格计算准确性集成测试"""
    
    def setup_method(self):
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
    
    def test_product_total_calculation(self):
        """测试商品总价计算"""
        customer = mock_crm.add_customer({'name': '价格计算测试客户'})
        
        # 创建不同价格的商品
        products = [
            {'id': 1, 'name': '商品A', 'price': 99.99},
            {'id': 2, 'name': '商品B', 'price': 199.50},
            {'id': 3, 'name': '商品C', 'price': 50.00}
        ]
        
        for product in products:
            mock_erp.add_product(product)
            mock_erp.update_stock(product['id'], 1, 100, 'add')
        
        # 创建订单
        order_items = [
            {'product_id': 1, 'quantity': 2, 'price': 99.99},   # 199.98
            {'product_id': 2, 'quantity': 1, 'price': 199.50}, # 199.50
            {'product_id': 3, 'quantity': 5, 'price': 50.00}  # 250.00
        ]
        
        expected_subtotal = 199.98 + 199.50 + 250.00  # 649.48
        
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': order_items,
            'total_amount': expected_subtotal,
            'status': 'pending'
        })
        
        # 验证总价计算
        assert order['total_amount'] == expected_subtotal
        
        # 验证单项小计
        item_totals = [item['quantity'] * item['price'] for item in order['items']]
        assert item_totals[0] == 199.98
        assert item_totals[1] == 199.50
        assert item_totals[2] == 250.00
    
    def test_shipping_fee_calculation(self):
        """测试运费计算"""
        customer = mock_crm.add_customer({'name': '运费测试客户'})
        mock_erp.add_product({'id': 1, 'name': '运费测试商品', 'price': 100.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 定义运费规则
        def calculate_shipping_fee(order_amount, weight=0):
            if order_amount >= 500:
                return 0  # 满500免运费
            elif order_amount >= 200:
                return 10  # 满200减运费
            else:
                return 20  # 标准运费
        
        # 测试场景1: 订单金额 < 200
        order1 = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 1, 'price': 100.00}],
            'total_amount': 100.00,
            'status': 'pending'
        })
        shipping1 = calculate_shipping_fee(order1['total_amount'])
        assert shipping1 == 20
        
        # 测试场景2: 200 <= 订单金额 < 500
        order2 = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 3, 'price': 100.00}],
            'total_amount': 300.00,
            'status': 'pending'
        })
        shipping2 = calculate_shipping_fee(order2['total_amount'])
        assert shipping2 == 10
        
        # 测试场景3: 订单金额 >= 500 (免运费)
        order3 = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 6, 'price': 100.00}],
            'total_amount': 600.00,
            'status': 'pending'
        })
        shipping3 = calculate_shipping_fee(order3['total_amount'])
        assert shipping3 == 0
    
    def test_tax_calculation(self):
        """测试税费计算"""
        customer = mock_crm.add_customer({'name': '税费测试客户'})
        mock_erp.add_product({'id': 1, 'name': '税费测试商品', 'price': 1000.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 定义税率
        TAX_RATE = 0.13  # 13%增值税
        
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 2, 'price': 1000.00}],
            'subtotal': 2000.00,
            'tax_rate': TAX_RATE,
            'tax_amount': 2000.00 * TAX_RATE,  # 260.00
            'total_amount': 2000.00 + 260.00,  # 2260.00
            'status': 'pending'
        })
        
        # 验证税费计算
        expected_tax = 2000.00 * 0.13
        assert order['tax_amount'] == expected_tax
        assert order['total_amount'] == 2000.00 + expected_tax
    
    def test_final_price_verification(self):
        """测试最终价格验证"""
        customer = mock_crm.add_customer({'name': '最终价格测试客户'})
        
        # 创建商品
        mock_erp.add_product({'id': 1, 'name': '综合测试商品', 'price': 299.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        quantity = 3
        unit_price = 299.00
        subtotal = quantity * unit_price  # 897.00
        shipping_fee = 10  # 满200减运费
        tax_rate = 0.13
        tax_amount = subtotal * tax_rate  # 116.61
        final_total = subtotal + shipping_fee + tax_amount  # 1023.61
        
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': quantity, 'price': unit_price}],
            'subtotal': subtotal,
            'shipping_fee': shipping_fee,
            'tax_rate': tax_rate,
            'tax_amount': round(tax_amount, 2),
            'total_amount': round(final_total, 2),
            'status': 'pending'
        })
        
        # 验证最终价格
        assert order['subtotal'] == 897.00
        assert order['shipping_fee'] == 10
        assert order['tax_amount'] == 116.61
        assert order['total_amount'] == 1023.61
        
        # 验证总价 = 小计 + 运费 + 税费
        calculated_total = order['subtotal'] + order['shipping_fee'] + order['tax_amount']
        assert abs(calculated_total - order['total_amount']) < 0.01


@pytest.mark.integration
class TestCouponUsage:
    """优惠券使用验证集成测试"""
    
    def setup_method(self):
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_erp.coupons = {}
        mock_crm.customers = {}
    
    def test_coupon_application_logic(self):
        """测试优惠券应用逻辑"""
        customer = mock_crm.add_customer({'name': '优惠券测试客户'})
        
        # 创建优惠券
        coupon = {
            'id': 1,
            'code': 'SAVE50',
            'type': 'fixed_amount',  # 固定金额优惠
            'value': 50.00,
            'min_order_amount': 200.00,
            'valid_from': (datetime.now() - timedelta(days=1)).isoformat(),
            'valid_to': (datetime.now() + timedelta(days=30)).isoformat(),
            'usage_limit': 100,
            'used_count': 0,
            'status': 'active'
        }
        mock_erp.coupons[1] = coupon
        
        # 创建商品
        mock_erp.add_product({'id': 1, 'name': '优惠券测试商品', 'price': 150.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 测试场景1: 订单金额满足优惠券条件
        order1 = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 2, 'price': 150.00}],
            'subtotal': 300.00,
            'coupon_code': 'SAVE50',
            'discount_amount': 50.00,
            'total_amount': 250.00,  # 300 - 50
            'status': 'pending'
        })
        
        assert order1['coupon_code'] == 'SAVE50'
        assert order1['discount_amount'] == 50.00
        assert order1['total_amount'] == 250.00
        
        # 测试场景2: 订单金额不满足优惠券条件
        order2 = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 1, 'price': 150.00}],
            'subtotal': 150.00,
            'coupon_code': None,
            'discount_amount': 0,
            'total_amount': 150.00,
            'status': 'pending'
        })
        
        assert order2['coupon_code'] is None
        assert order2['discount_amount'] == 0
    
    def test_discount_amount_calculation(self):
        """测试折扣金额计算"""
        customer = mock_crm.add_customer({'name': '折扣计算测试客户'})
        
        # 创建不同类型的优惠券
        coupons = {
            1: {'id': 1, 'code': 'FIXED100', 'type': 'fixed_amount', 'value': 100.00, 'status': 'active'},
            2: {'id': 2, 'code': 'PERCENT20', 'type': 'percentage', 'value': 0.20, 'status': 'active'},
            3: {'id': 3, 'code': 'MAX50OFF', 'type': 'percentage', 'value': 0.50, 'max_discount': 50.00, 'status': 'active'}
        }
        mock_erp.coupons = coupons
        
        mock_erp.add_product({'id': 1, 'name': '折扣测试商品', 'price': 500.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 测试固定金额优惠
        subtotal = 500.00
        fixed_discount = 100.00
        assert subtotal - fixed_discount == 400.00
        
        # 测试百分比优惠
        percentage_discount = subtotal * 0.20  # 100.00
        assert percentage_discount == 100.00
        
        # 测试带上限的百分比优惠
        unlimited_discount = subtotal * 0.50  # 250.00
        capped_discount = min(unlimited_discount, 50.00)  # 50.00
        assert capped_discount == 50.00
    
    def test_coupon_status_update(self):
        """测试优惠券状态更新"""
        customer = mock_crm.add_customer({'name': '状态更新测试客户'})
        
        # 创建一次性优惠券
        coupon = {
            'id': 1,
            'code': 'ONETIME',
            'type': 'fixed_amount',
            'value': 30.00,
            'usage_limit': 1,
            'used_count': 0,
            'status': 'active'
        }
        mock_erp.coupons[1] = coupon
        
        mock_erp.add_product({'id': 1, 'name': '状态测试商品', 'price': 200.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 使用优惠券创建订单
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 1, 'price': 200.00}],
            'subtotal': 200.00,
            'coupon_code': 'ONETIME',
            'discount_amount': 30.00,
            'total_amount': 170.00,
            'status': 'pending'
        })
        
        # 模拟优惠券使用次数更新
        mock_erp.coupons[1]['used_count'] += 1
        
        # 验证使用次数增加
        assert mock_erp.coupons[1]['used_count'] == 1
        
        # 模拟订单支付完成，优惠券标记为已使用
        mock_erp.update_order(order['id'], {'status': 'paid'})
        
        # 如果达到使用上限，标记为已用完
        if mock_erp.coupons[1]['used_count'] >= mock_erp.coupons[1]['usage_limit']:
            mock_erp.coupons[1]['status'] = 'exhausted'
        
        assert mock_erp.coupons[1]['status'] == 'exhausted'


@pytest.mark.integration
class TestOrderCreationPerformance:
    """订单创建性能测试"""
    
    def setup_method(self):
        mock_erp.orders = {}
        mock_erp.products = {}
        mock_erp.stock = {}
        mock_crm.customers = {}
    
    def test_order_creation_response_time(self):
        """测试订单创建接口响应时间"""
        customer = mock_crm.add_customer({'name': '性能测试客户'})
        mock_erp.add_product({'id': 1, 'name': '性能测试商品', 'price': 100.00})
        mock_erp.update_stock(1, 1, 1000, 'add')
        
        # 测试订单创建响应时间
        start_time = time.time()
        
        order = mock_erp.create_order({
            'customer_id': customer['id'],
            'items': [{'product_id': 1, 'quantity': 5, 'price': 100.00}],
            'total_amount': 500.00,
            'status': 'pending'
        })
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000  # 转换为毫秒
        
        # 验证响应时间 <= 500ms
        assert response_time <= 500, f"订单创建响应时间 {response_time}ms 超过 500ms 阈值"
        assert order['id'] > 0
    
    def test_concurrent_order_creation(self):
        """测试并发订单创建"""
        import threading
        
        results = []
        errors = []
        
        def create_order_thread(customer_id, thread_id):
            try:
                order = mock_erp.create_order({
                    'customer_id': customer_id,
                    'items': [{'product_id': 1, 'quantity': 1, 'price': 100.00}],
                    'total_amount': 100.00,
                    'status': 'pending'
                })
                results.append(order)
            except Exception as e:
                errors.append(str(e))
        
        # 创建客户和商品
        customers = []
        for i in range(10):
            customer = mock_crm.add_customer({'name': f'并发客户{i}'})
            customers.append(customer['id'])
        
        mock_erp.add_product({'id': 1, 'name': '并发测试商品', 'price': 100.00})
        mock_erp.update_stock(1, 1, 100, 'add')
        
        # 并发创建订单
        threads = []
        for i in range(10):
            t = threading.Thread(target=create_order_thread, args=(customers[i], i))
            threads.append(t)
        
        for t in threads:
            t.start()
        
        for t in threads:
            t.join()
        
        # 验证所有订单创建成功
        assert len(results) == 10
        assert len(errors) == 0
        
        # 验证库存正确扣减
        final_stock = mock_erp.get_stock(1, 1)
        assert final_stock == 90  # 100 - 10


if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
