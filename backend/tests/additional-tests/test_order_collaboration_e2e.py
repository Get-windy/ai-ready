"""
订单协同流程端到端测试
测试供应商协同门户的完整订单协同流程
"""

import pytest
import requests
import json
import time
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, List

# 测试配置
BASE_URL = "http://localhost:8080"
API_PREFIX = "/api"
ORDER_API = f"{BASE_URL}{API_PREFIX}/order"
PURCHASE_ORDER_API = f"{BASE_URL}{API_PREFIX}/erp/purchase/order"
SUPPLIER_PORTAL_API = f"{BASE_URL}{API_PREFIX}/supplier-portal"

# 测试数据
TEST_ORDER_DATA = {
    "orderNo": "ORD_TEST_001",
    "quoteId": 1001,
    "quoteNo": "QUOTE_001",
    "supplierId": 2001,
    "supplierName": "供应商A科技有限公司",
    "purchaserId": 1001,
    "purchaserName": "张三",
    "departmentId": 101,
    "departmentName": "采购部",
    "orderType": "PURCHASE",
    "currency": "CNY",
    "totalAmount": 108480.00,
    "taxAmount": 12480.00,
    "grandTotal": 108480.00,
    "paymentTerms": "30%预付，70%货到付款",
    "deliveryAddress": "北京市海淀区中关村大街1号",
    "deliveryDeadline": (datetime.now() + timedelta(days=7)).isoformat(),
    "remark": "请按时交付，质量需符合要求",
    "items": [
        {
            "itemNo": "ITEM_001",
            "productName": "办公电脑",
            "productCode": "PC-001",
            "specification": "i5/16GB/512GB SSD",
            "quantity": 20,
            "unit": "台",
            "unitPrice": 4800.00,
            "totalPrice": 96000.00,
            "taxRate": 13.0,
            "taxAmount": 12480.00,
            "grandTotal": 108480.00,
            "remark": "含正版Windows 11专业版"
        }
    ]
}

TEST_SHIPMENT_DATA = {
    "shipmentNo": "SHIP_TEST_001",
    "orderId": None,
    "orderNo": "ORD_TEST_001",
    "shipmentDate": (datetime.now() + timedelta(days=3)).isoformat(),
    "estimatedArrival": (datetime.now() + timedelta(days=5)).isoformat(),
    "carrier": "顺丰速运",
    "trackingNo": "SF1234567890",
    "shipmentStatus": "SHIPPED",
    "items": [
        {
            "orderItemId": 1,
            "productName": "办公电脑",
            "quantity": 20,
            "shippedQuantity": 20,
            "remark": "包装完好"
        }
    ],
    "remark": "货物已发出，请注意查收"
}


class OrderCollaborationE2ETest:
    """订单协同流程端到端测试类"""
    
    def setup_method(self):
        """每个测试方法的前置准备"""
        self.session = requests.Session()
        self.test_results: List[Dict] = []
        self.created_order_id = None
        self.order_no = None
        self.shipment_id = None
        
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
    
    def teardown_method(self):
        """每个测试方法的后置清理"""
        if self.shipment_id:
            try:
                self.session.delete(f"{PURCHASE_ORDER_API}/shipments/{self.shipment_id}", timeout=5)
            except:
                pass
        
        if self.created_order_id:
            try:
                self.session.delete(f"{ORDER_API}/{self.created_order_id}", timeout=5)
            except:
                pass
    
    def log_test_step(self, step_name: str, result: bool, details: str = "") -> bool:
        """记录测试步骤结果"""
        self.test_results.append({
            "step": step_name,
            "result": "通过" if result else "失败",
            "details": details,
            "timestamp": datetime.now().isoformat()
        })
        status = "[PASS]" if result else "[FAIL]"
        print(f"{status} {step_name}: {details}")
        return result
    
    def test_01_create_order_from_quote(self):
        """测试基于报价生成订单"""
        print("\n=== 测试步骤1: 基于报价生成订单 ===")
        
        try:
            response = self.session.post(ORDER_API, json=TEST_ORDER_DATA, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                self.created_order_id = data.get('id') or data.get('data')
                self.order_no = data.get('orderNo') or TEST_ORDER_DATA['orderNo']
                return self.log_test_step("基于报价生成订单", True, f"订单ID: {self.created_order_id}, 编号: {self.order_no}")
            else:
                return self.log_test_step("基于报价生成订单", False, f"状态码: {response.status_code}")
        except Exception as e:
            return self.log_test_step("基于报价生成订单", False, f"异常: {str(e)}")
    
    def test_02_confirm_order_info(self):
        """测试订单信息确认"""
        print("\n=== 测试步骤2: 订单信息确认 ===")
        
        if not self.created_order_id:
            return self.log_test_step("订单信息确认", False, "前置条件失败: 订单未创建")
        
        try:
            response = self.session.get(f"{ORDER_API}/{self.created_order_id}", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                order_data = data.get('data', data)
                items_count = len(order_data.get('items', []))
                total_amount = order_data.get('totalAmount')
                return self.log_test_step("订单信息确认", True, f"订单项数: {items_count}, 总金额: {total_amount}")
            else:
                return self.log_test_step("订单信息确认", False, f"状态码: {response.status_code}")
        except Exception as e:
            return self.log_test_step("订单信息确认", False, f"异常: {str(e)}")
    
    def test_03_send_order_to_supplier(self):
        """测试订单发送给供应商"""
        print("\n=== 测试步骤3: 订单发送给供应商 ===")
        
        if not self.created_order_id:
            return self.log_test_step("订单发送给供应商", False, "前置条件失败: 订单未创建")
        
        try:
            response = self.session.put(f"{ORDER_API}/{self.created_order_id}/status", params={"status": 1}, timeout=10)
            
            if response.status_code == 200:
                return self.log_test_step("订单发送给供应商", True, f"订单 {self.order_no} 已发送给供应商")
            else:
                return self.log_test_step("订单发送给供应商", False, f"状态码: {response.status_code}")
        except Exception as e:
            return self.log_test_step("订单发送给供应商", False, f"异常: {str(e)}")
    
    def test_04_receive_order_notification(self):
        """测试订单接收通知"""
        print("\n=== 测试步骤4: 订单接收通知 ===")
        
        if not self.order_no:
            return self.log_test_step("订单接收通知", False, "前置条件失败: 订单未创建")
        
        try:
            response = self.session.get(f"{SUPPLIER_PORTAL_API}/orders/received", params={"supplierId": 2001, "status": "PENDING"}, timeout=10)
            
            if response.status_code == 200:
                return self.log_test_step("订单接收通知", True, "供应商门户API可访问")
            else:
                return self.log_test_step("订单接收通知", False, f"状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            return self.log_test_step("订单接收通知", False, "连接错误: 供应商门户服务未启动")
        except Exception as e:
            return self.log_test_step("订单接收通知", False, f"异常: {str(e)}")
    
    def test_05_view_order_details(self):
        """测试订单详情查看"""
        print("\n=== 测试步骤5: 订单详情查看 ===")
        
        if not self.created_order_id:
            return self.log_test_step("订单详情查看", False, "前置条件失败: 订单未创建")
        
        try:
            response = self.session.get(f"{ORDER_API}/{self.created_order_id}", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                order_data = data.get('data', data)
                order_no = order_data.get('orderNo')
                items_count = len(order_data.get('items', []))
                return self.log_test_step("订单详情查看", True, f"订单编号: {order_no}, 商品项数: {items_count}")
            else:
                return self.log_test_step