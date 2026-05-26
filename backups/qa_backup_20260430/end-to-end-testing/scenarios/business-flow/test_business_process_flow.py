"""
业务流程端到端测试
测试完整业务流程：订单创建 -> 支付处理 -> 库存管理 -> 物流跟踪
"""

import pytest
import requests
import sys
import os
import time
from datetime import datetime

# 添加框架路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from framework.base_test import BaseE2ETest
from framework.config import TestConfig
from framework.utils import TestUtils


class TestBusinessProcessFlow(BaseE2ETest):
    """业务流程端到端测试"""
    
    def __init__(self):
        super().__init__()
        self.config = TestConfig()
        self.utils = TestUtils()
        self.api_base_url = self.config.get('api.base_url', 'http://localhost:8080/api')
        self.test_order = None
        self.test_payment = None
        self.test_inventory = None
        self.test_logistics = None
    
    def test_order_creation_flow(self):
        """测试订单创建流程"""
        self.setup_method()
        
        try:
            # 步骤1: 创建测试用户
            test_user = self.utils.generate_test_user()
            self.add_test_step(
                "生成测试用户",
                {'username': test_user['username']},
                'passed'
            )
            
            # 步骤2: 创建测试产品
            test_product = self.utils.generate_test_product()
            test_product['stock'] = 100  # 确保有足够库存
            
            response = requests.post(
                f"{self.api_base_url}/products",
                json=test_product,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "产品创建应返回201状态码")
            product_data = response.json()
            product_id = product_data['id']
            
            self.add_test_step(
                "创建测试产品",
                {'product_id': product_id, 'name': test_product['name']},
                'passed'
            )
            
            # 步骤3: 创建订单
            order_items = [{
                'product_id': product_id,
                'quantity': 2,
                'unit_price': test_product['price']
            }]
            
            order_data = {
                'customer_name': test_user['name'],
                'customer_email': test_user['email'],
                'customer_phone': test_user['phone'],
                'shipping_address': test_user['address'],
                'items': order_items,
                'total_amount': test_product['price'] * 2
            }
            
            response = requests.post(
                f"{self.api_base_url}/orders",
                json=order_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "订单创建应返回201状态码")
            
            order_data = response.json()
            self.assert_true('order_id' in order_data, "响应应包含order_id")
            self.assert_true('status' in order_data, "响应应包含status")
            
            order_id = order_data['order_id']
            self.test_order = {
                'order_id': order_id,
                'status': order_data['status'],
                'customer_email': order_data['customer_email'],
                'total_amount': order_data['total_amount']
            }
            
            self.add_test_step(
                "创建订单",
                {'order_id': order_id, 'status': order_data['status']},
                'passed'
            )
            
            # 步骤4: 验证订单状态
            response = requests.get(
                f"{self.api_base_url}/orders/{order_id}",
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "订单查询应返回200状态码")
            
            verify_data = response.json()
            self.assert_equal(verify_data['order_id'], order_id, "订单ID应匹配")
            self.assert_equal(verify_data['status'], 'pending', "初始状态应为pending")
            
            self.add_test_step(
                "验证订单状态",
                {'order_id': order_id, 'status': verify_data['status']},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "订单创建流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_payment_processing_flow(self):
        """测试支付处理流程"""
        self.setup_method()
        
        try:
            # 确保有测试订单
            if not self.test_order:
                self.test_order = {
                    'order_id': self.utils.generate_unique_id("ORD_"),
                    'status': 'pending',
                    'total_amount': 199.99
                }
            
            order_id = self.test_order['order_id']
            
            # 步骤1: 提交支付
            payment_data = {
                'order_id': order_id,
                'payment_method': 'credit_card',
                'card_number': '4111111111111111',
                'expiry_date': '12/26',
                'cvv': '123',
                'amount': self.test_order['total_amount']
            }
            
            response = requests.post(
                f"{self.api_base_url}/payments",
                json=payment_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "支付创建应返回201状态码")
            
            payment_data = response.json()
            self.assert_true('payment_id' in payment_data, "响应应包含payment_id")
            self.assert_true('status' in payment_data, "响应应包含status")
            
            payment_id = payment_data['payment_id']
            self.test_payment = {
                'payment_id': payment_id,
                'status': payment_data['status'],
                'amount': payment_data['amount']
            }
            
            self.add_test_step(
                "提交支付",
                {'payment_id': payment_id, 'status': payment_data['status']},
                'passed'
            )
            
            # 步骤2: 处理支付
            process_response = requests.post(
                f"{self.api_base_url}/payments/{payment_id}/process",
                timeout=30
            )
            
            self.assert_equal(process_response.status_code, 200, "支付处理应返回200状态码")
            
            process_data = process_response.json()
            self.assert_equal(process_data['status'], 'completed', "支付状态应为completed")
            
            self.add_test_step(
                "处理支付",
                {'payment_id': payment_id, 'status': process_data['status']},
                'passed'
            )
            
            # 步骤3: 更新订单状态
            update_data = {'status': 'paid'}
            response = requests.put(
                f"{self.api_base_url}/orders/{order_id}/status",
                json=update_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "订单状态更新应返回200状态码")
            
            self.test_order['status'] = 'paid'
            
            self.add_test_step(
                "更新订单状态",
                {'order_id': order_id, 'new_status': 'paid'},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "支付处理流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_inventory_management_flow(self):
        """测试库存管理流程"""
        self.setup_method()
        
        try:
            # 确保有测试订单
            if not self.test_order:
                self.test_order = {
                    'order_id': self.utils.generate_unique_id("ORD_"),
                    'status': 'paid'
                }
            
            order_id = self.test_order['order_id']
            
            # 步骤1: 检查库存
            response = requests.get(
                f"{self.api_base_url}/inventory/check",
                params={'order_id': order_id},
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "库存检查应返回200状态码")
            
            inventory_data = response.json()
            self.assert_true('available' in inventory_data, "响应应包含available状态")
            
            self.add_test_step(
                "检查库存",
                {'order_id': order_id, 'available': inventory_data['available']},
                'passed'
            )
            
            # 步骤2: 预留库存
            if inventory_data['available']:
                reserve_data = {'order_id': order_id}
                response = requests.post(
                    f"{self.api_base_url}/inventory/reserve",
                    json=reserve_data,
                    timeout=30
                )
                
                self.assert_equal(response.status_code, 200, "库存预留应返回200状态码")
                
                reserve_data = response.json()
                self.assert_equal(reserve_data['status'], 'reserved', "库存状态应为reserved")
                
                self.add_test_step(
                    "预留库存",
                    {'order_id': order_id, 'status': 'reserved'},
                    'passed'
                )
                
                # 步骤3: 更新订单状态
                update_data = {'status': 'processing'}
                response = requests.put(
                    f"{self.api_base_url}/orders/{order_id}/status",
                    json=update_data,
                    timeout=30
                )
                
                self.assert_equal(response.status_code, 200, "订单状态更新应返回200状态码")
                
                self.test_order['status'] = 'processing'
                
                self.add_test_step(
                    "更新订单状态为处理中",
                    {'order_id': order_id, 'new_status': 'processing'},
                    'passed'
                )
            
        except Exception as e:
            self.add_test_step(
                "库存管理流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_logistics_tracking_flow(self):
        """测试物流跟踪流程"""
        self.setup_method()
        
        try:
            # 确保有测试订单
            if not self.test_order:
                self.test_order = {
                    'order_id': self.utils.generate_unique_id("ORD_"),
                    'status': 'processing'
                }
            
            order_id = self.test_order['order_id']
            
            # 步骤1: 创建物流订单
            logistics_data = {
                'order_id': order_id,
                'shipping_method': 'express',
                'destination_address': '北京市朝阳区',
                'estimated_delivery': (datetime.now().timestamp() + 3*24*60*60) * 1000  # 3天后
            }
            
            response = requests.post(
                f"{self.api_base_url}/logistics",
                json=logistics_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "物流创建应返回201状态码")
            
            logistics_data = response.json()
            self.assert_true('tracking_number' in logistics_data, "响应应包含tracking_number")
            self.assert_true('status' in logistics_data, "响应应包含status")
            
            tracking_number = logistics_data['tracking_number']
            self.test_logistics = {
                'tracking_number': tracking_number,
                'status': logistics_data['status']
            }
            
            self.add_test_step(
                "创建物流订单",
                {'tracking_number': tracking_number, 'status': logistics_data['status']},
                'passed'
            )
            
            # 步骤2: 更新订单状态
            update_data = {'status': 'shipped'}
            response = requests.put(
                f"{self.api_base_url}/orders/{order_id}/status",
                json=update_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "订单状态更新应返回200状态码")
            
            self.test_order['status'] = 'shipped'
            
            self.add_test_step(
                "更新订单状态为已发货",
                {'order_id': order_id, 'new_status': 'shipped'},
                'passed'
            )
            
            # 步骤3: 跟踪物流状态
            response = requests.get(
                f"{self.api_base_url}/logistics/{tracking_number}/track",
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "物流跟踪应返回200状态码")
            
            track_data = response.json()
            self.assert_true('current_status' in track_data, "响应应包含current_status")
            self.assert_true('history' in track_data, "响应应包含history")
            
            self.add_test_step(
                "跟踪物流状态",
                {'tracking_number': tracking_number, 'status': track_data['current_status']},
                'passed'
            )
            
            # 步骤4: 订单完成
            update_data = {'status': 'delivered'}
            response = requests.put(
                f"{self.api_base_url}/orders/{order_id}/status",
                json=update_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "订单完成应返回200状态码")
            
            self.test_order['status'] = 'delivered'
            
            self.add_test_step(
                "订单完成交付",
                {'order_id': order_id, 'final_status': 'delivered'},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "物流跟踪流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_complete_business_flow(self):
        """测试完整业务流程"""
        self.setup_method()
        
        try:
            # 执行完整业务流程
            self.test_order_creation_flow()
            self.test_payment_processing_flow()
            self.test_inventory_management_flow()
            self.test_logistics_tracking_flow()
            
            self.add_test_step(
                "完整业务流程",
                {'status': 'completed'},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "完整业务流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])