#!/usr/bin/env python3
"""
订单管理模块功能测试
Sprint 27+1 测试环境专项
测试内容：订单CRUD、状态流转、库存联动、统计报表
"""

import pytest
import requests
import json
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any

# 测试配置
BASE_URL = "http://localhost:8080"
API_PREFIX = "/api/v1/order"
TEST_TIMEOUT = 30

# 测试数据
TEST_CUSTOMER_ID = "CUST001"
TEST_PRODUCT_ID = "PROD001"
TEST_PRODUCT_SKU = "SKU001"

class TestOrderCRUD:
    """订单CRUD功能测试"""
    
    def test_order_create(self):
        """ORD_001: 订单创建测试"""
        url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "productName": "测试商品",
                    "quantity": 5,
                    "unitPrice": 100.00,
                    "totalPrice": 500.00
                }
            ],
            "totalAmount": 500.00,
            "discountAmount": 0,
            "payableAmount": 500.00,
            "shippingAddress": {
                "province": "广东省",
                "city": "深圳市",
                "district": "南山区",
                "address": "科技园测试路1号"
            },
            "remark": "订单创建测试",
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "orderId" in data["data"]
        assert "orderNo" in data["data"]
        self.order_id = data["data"]["orderId"]
        self.order_no = data["data"]["orderNo"]
        print(f"✅ 订单创建成功: 订单号 {self.order_no}")
        return self.order_id, self.order_no
    
    def test_order_query(self):
        """ORD_004: 订单查询测试"""
        order_id, order_no = self.test_order_create()
        
        # 按ID查询
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert data["data"]["orderId"] == order_id
        print(f"✅ 订单查询成功(按ID): {data['data']['orderNo']}")
        
        # 按订单号查询
        url = f"{BASE_URL}{API_PREFIX}/no/{order_no}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert data["data"]["orderNo"] == order_no
        print(f"✅ 订单查询成功(按订单号): {data['data']['orderNo']}")
        
        return order_id
    
    def test_order_update(self):
        """ORD_002: 订单编辑测试"""
        order_id = self.test_order_query()
        
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        payload = {
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "productName": "测试商品",
                    "quantity": 10,  # 修改数量
                    "unitPrice": 100.00,
                    "totalPrice": 1000.00
                }
            ],
            "totalAmount": 1000.00,
            "payableAmount": 1000.00,
            "remark": "订单编辑测试 - 修改数量",
            "operator": "TEST_USER"
        }
        response = requests.put(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 订单编辑成功")
        return order_id
    
    def test_order_cancel(self):
        """ORD_003: 订单取消测试"""
        # 创建新订单用于取消测试
        url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 1, "unitPrice": 100, "totalPrice": 100}],
            "totalAmount": 100,
            "payableAmount": 100,
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        order_id = response.json()["data"]["orderId"]
        
        # 取消订单
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/cancel"
        payload = {
            "cancelReason": "客户取消",
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        
        # 验证订单状态已更新为已取消
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.json()["data"]["status"] == "cancelled"
        print(f"✅ 订单取消成功")
    
    def test_order_list_query(self):
        """测试订单列表查询"""
        url = f"{BASE_URL}{API_PREFIX}"
        params = {
            "page": 1,
            "size": 10,
            "customerId": TEST_CUSTOMER_ID,
            "startDate": (datetime.now() - timedelta(days=7)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "list" in data["data"]
        print(f"✅ 订单列表查询成功: 共{data['data'].get('total', 0)}条记录")


class TestOrderStatusFlow:
    """订单状态流转测试"""
    
    def create_test_order(self):
        """辅助方法：创建测试订单"""
        url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 2, "unitPrice": 100, "totalPrice": 200}],
            "totalAmount": 200,
            "payableAmount": 200,
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        return response.json()["data"]["orderId"]
    
    def test_status_pending_to_paid(self):
        """测试状态流转：待支付 -> 已支付"""
        order_id = self.create_test_order()
        
        # 更新状态为已支付
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/status"
        payload = {
            "status": "paid",
            "paymentInfo": {
                "paymentMethod": "alipay",
                "paymentNo": "PAY20260426001",
                "paymentTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "paymentAmount": 200
            },
            "operator": "TEST_USER"
        }
        response = requests.patch(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        
        # 验证状态
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.json()["data"]["status"] == "paid"
        print(f"✅ 状态流转测试成功: pending -> paid")
    
    def test_status_paid_to_shipped(self):
        """测试状态流转：已支付 -> 已发货"""
        order_id = self.create_test_order()
        
        # 先支付
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/status"
        requests.patch(url, json={"status": "paid", "operator": "TEST_USER"}, timeout=TEST_TIMEOUT)
        
        # 再发货
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/shipping"
        payload = {
            "logisticsCompany": "顺丰速运",
            "trackingNo": "SF1234567890",
            "shippingTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        assert response.json()["code"] == 200
        
        # 验证状态
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.json()["data"]["status"] == "shipped"
        print(f"✅ 状态流转测试成功: paid -> shipped")
    
    def test_status_shipped_to_completed(self):
        """测试状态流转：已发货 -> 已完成"""
        order_id = self.create_test_order()
        
        # 先支付
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/status"
        requests.patch(url, json={"status": "paid", "operator": "TEST_USER"}, timeout=TEST_TIMEOUT)
        
        # 再发货
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/shipping"
        requests.post(url, json={"logisticsCompany": "顺丰", "trackingNo": "SF123", "operator": "TEST_USER"}, timeout=TEST_TIMEOUT)
        
        # 最后完成
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/complete"
        payload = {
            "completeTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        assert response.json()["code"] == 200
        
        # 验证状态
        url = f"{BASE_URL}{API_PREFIX}/{order_id}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.json()["data"]["status"] == "completed"
        print(f"✅ 状态流转测试成功: shipped -> completed")
    
    def test_status_history_tracking(self):
        """测试订单状态历史跟踪"""
        order_id = self.create_test_order()
        
        url = f"{BASE_URL}{API_PREFIX}/{order_id}/status/history"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert len(data["data"]) > 0
        print(f"✅ 状态历史跟踪成功: 共{len(data['data'])}条记录")


class TestOrderInventoryIntegration:
    """订单与库存联动测试"""
    
    def test_order_create_deduct_inventory(self):
        """测试订单创建扣减库存"""
        # 1. 查询当前库存
        stock_url = f"{BASE_URL}/api/erp/stock/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(stock_url, timeout=TEST_TIMEOUT)
        initial_stock = response.json()["data"]["availableQuantity"]
        print(f"初始库存: {initial_stock}")
        
        # 2. 创建订单
        order_url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 3, "unitPrice": 100, "totalPrice": 300}],
            "totalAmount": 300,
            "payableAmount": 300,
            "operator": "TEST_USER"
        }
        response = requests.post(order_url, json=payload, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        
        # 3. 验证库存已扣减
        response = requests.get(stock_url, timeout=TEST_TIMEOUT)
        current_stock = response.json()["data"]["availableQuantity"]
        
        assert current_stock == initial_stock - 3, f"库存扣减失败: 期望{initial_stock-3}, 实际{current_stock}"
        print(f"✅ 订单创建扣减库存成功: {initial_stock} -> {current_stock}")
    
    def test_order_cancel_restore_inventory(self):
        """测试订单取消恢复库存"""
        # 1. 查询当前库存
        stock_url = f"{BASE_URL}/api/erp/stock/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(stock_url, timeout=TEST_TIMEOUT)
        initial_stock = response.json()["data"]["availableQuantity"]
        
        # 2. 创建订单
        order_url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 2, "unitPrice": 100, "totalPrice": 200}],
            "totalAmount": 200,
            "payableAmount": 200,
            "operator": "TEST_USER"
        }
        response = requests.post(order_url, json=payload, timeout=TEST_TIMEOUT)
        order_id = response.json()["data"]["orderId"]
        
        # 3. 取消订单
        cancel_url = f"{BASE_URL}{API_PREFIX}/{order_id}/cancel"
        requests.post(cancel_url, json={"cancelReason": "测试取消", "operator": "TEST_USER"}, timeout=TEST_TIMEOUT)
        
        # 4. 验证库存已恢复
        response = requests.get(stock_url, timeout=TEST_TIMEOUT)
        current_stock = response.json()["data"]["availableQuantity"]
        
        assert current_stock == initial_stock, f"库存恢复失败: 期望{initial_stock}, 实际{current_stock}"
        print(f"✅ 订单取消恢复库存成功: {initial_stock} -> {current_stock}")
    
    def test_insufficient_inventory_prevention(self):
        """测试库存不足阻止下单"""
        # 1. 查询当前库存
        stock_url = f"{BASE_URL}/api/erp/stock/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(stock_url, timeout=TEST_TIMEOUT)
        available_stock = response.json()["data"]["availableQuantity"]
        
        # 2. 尝试下单超过库存数量
        order_url = f"{BASE_URL}{API_PREFIX}"
        payload = {
            "customerId": TEST_CUSTOMER_ID,
            "customerName": "测试客户",
            "orderType": "sales",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": available_stock + 1000, "unitPrice": 100, "totalPrice": 100000}],
            "totalAmount": 100000,
            "payableAmount": 100000,
            "operator": "TEST_USER"
        }
        response = requests.post(order_url, json=payload, timeout=TEST_TIMEOUT)
        
        # 应该返回错误
        assert response.status_code == 400 or response.json()["code"] != 200
        print(f"✅ 库存不足阻止下单测试成功")


class TestOrderStatistics:
    """订单统计报表功能测试"""
    
    def test_order_summary_statistics(self):
        """测试订单汇总统计"""
        url = f"{BASE_URL}{API_PREFIX}/statistics/summary"
        params = {
            "startDate": (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "totalOrders" in data["data"]
        assert "totalAmount" in data["data"]
        print(f"✅ 订单汇总统计查询成功: {data['data']}")
    
    def test_order_status_statistics(self):
        """测试订单状态分布统计"""
        url = f"{BASE_URL}{API_PREFIX}/statistics/status"
        params = {
            "startDate": (datetime.now() - timedelta(days=7)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        print(f"✅ 订单状态分布统计查询成功: {data['data']}")
    
    def test_order_customer_statistics(self):
        """测试客户订单统计"""
        url = f"{BASE_URL}{API_PREFIX}/statistics/customer"
        params = {
            "customerId": TEST_CUSTOMER_ID,
            "startDate": (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "totalOrders" in data["data"]
        assert "totalAmount" in data["data"]
        print(f"✅ 客户订单统计查询成功: {data['data']}")
    
    def test_order_product_statistics(self):
        """测试商品订单统计"""
        url = f"{BASE_URL}{API_PREFIX}/statistics/product"
        params = {
            "productId": TEST_PRODUCT_ID,
            "startDate": (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "totalOrders" in data["data"]
        assert "totalQuantity" in data["data"]
        print(f"✅ 商品订单统计查询成功: {data['data']}")
    
    def test_order_trend_analysis(self):
        """测试订单趋势分析"""
        url = f"{BASE_URL}{API_PREFIX}/statistics/trend"
        params = {
            "period": "daily",
            "days": 30
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert isinstance(data["data"], list)
        print(f"✅ 订单趋势分析查询成功: 共{len(data['data'])}天数据")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])