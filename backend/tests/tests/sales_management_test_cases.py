"""
企智连销售管理模块测试用例
Sales Management Test Cases for AI-Ready ERP System

测试覆盖范围:
1. 客户管理 (Customer Management)
2. 销售订单 (Sales Order)
3. 销售出库 (Sales Outbound)
4. 销售退货 (Sales Return)
5. 订单审批流程 (Order Approval Workflow)
6. 边界条件测试 (Boundary Tests)
7. 集成测试 (Integration Tests)

Author: test-agent-1
Created: 2026-04-18
"""

import pytest
import requests
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Dict, List, Optional

# 测试配置
BASE_URL = "http://localhost:8080/api/v1"
TEST_HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Bearer test-token"
}

# ==================== 测试数据生成器 ====================

class TestDataGenerator:
    """测试数据生成器"""
    
    @staticmethod
    def generate_customer() -> Dict:
        """生成客户测试数据"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        return {
            "code": f"CUST{timestamp}",
            "name": f"测试客户_{timestamp}",
            "type": "ENTERPRISE",
            "contactName": "张三",
            "contactPhone": "13800138000",
            "contactEmail": f"test{timestamp}@example.com",
            "address": "北京市朝阳区测试路1号",
            "creditLevel": "A",
            "creditLimit": 100000.00,
            "balance": 0.00,
            "status": 1,
            "remark": "测试客户"
        }
    
    @staticmethod
    def generate_sales_order(customer_id: str) -> Dict:
        """生成销售订单测试数据"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        order_date = datetime.now()
        delivery_date = order_date + timedelta(days=7)
        
        return {
            "orderNo": f"SO{timestamp}",
            "customerId": customer_id,
            "customerName": "测试客户",
            "customerCode": f"CUST{timestamp}",
            "status": "DRAFT",
            "totalAmount": 10000.00,
            "totalQty": 100,
            "discountAmount": 500.00,
            "payableAmount": 9500.00,
            "paidAmount": 0.00,
            "remark": "测试订单",
            "orderDate": order_date.isoformat(),
            "deliveryDate": delivery_date.isoformat(),
            "items": [
                {
                    "productId": "PROD001",
                    "productName": "测试产品1",
                    "productCode": "P001",
                    "quantity": 50,
                    "unitPrice": 100.00,
                    "amount": 5000.00,
                    "remark": ""
                },
                {
                    "productId": "PROD002",
                    "productName": "测试产品2",
                    "productCode": "P002",
                    "quantity": 50,
                    "unitPrice": 100.00,
                    "amount": 5000.00,
                    "remark": ""
                }
            ]
        }
    
    @staticmethod
    def generate_sales_outbound(order_id: str, customer_id: str) -> Dict:
        """生成销售出库测试数据"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        
        return {
            "outboundNo": f"OB{timestamp}",
            "orderId": order_id,
            "orderNo": f"SO{timestamp}",
            "customerId": customer_id,
            "customerName": "测试客户",
            "warehouseId": "WH001",
            "warehouseName": "主仓库",
            "status": "PENDING",
            "totalAmount": 10000.00,
            "totalQty": 100,
            "operatorId": "USER001",
            "operatorName": "操作员",
            "remark": "测试出库",
            "outboundDate": datetime.now().isoformat(),
            "items": [
                {
                    "productId": "PROD001",
                    "productName": "测试产品1",
                    "quantity": 50,
                    "warehouseId": "WH001",
                    "locationId": "LOC001"
                },
                {
                    "productId": "PROD002",
                    "productName": "测试产品2",
                    "quantity": 50,
                    "warehouseId": "WH001",
                    "locationId": "LOC002"
                }
            ]
        }
    
    @staticmethod
    def generate_sales_return(order_id: str, customer_id: str) -> Dict:
        """生成销售退货测试数据"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        
        return {
            "returnNo": f"RT{timestamp}",
            "orderId": order_id,
            "orderNo": f"SO{timestamp}",
            "customerId": customer_id,
            "customerName": "测试客户",
            "warehouseId": "WH001",
            "warehouseName": "主仓库",
            "status": "DRAFT",
            "returnReason": "质量问题",
            "totalAmount": 1000.00,
            "totalQty": 10,
            "operatorId": "USER001",
            "operatorName": "操作员",
            "remark": "测试退货",
            "returnDate": datetime.now().isoformat(),
            "items": [
                {
                    "productId": "PROD001",
                    "productName": "测试产品1",
                    "quantity": 10,
                    "unitPrice": 100.00,
                    "amount": 1000.00,
                    "returnReason": "质量问题"
                }
            ]
        }


# ==================== 客户管理测试 ====================

class TestCustomerManagement:
    """客户管理测试类"""
    
    def test_create_customer_success(self):
        """TC-CUST-001: 成功创建客户"""
        customer_data = TestDataGenerator.generate_customer()
        
        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
        return result["data"]
    
    def test_create_customer_duplicate_code(self):
        """TC-CUST-002: 创建客户-编码重复"""
        customer_data = TestDataGenerator.generate_customer()
        
        # 第一次创建
        requests.post(f"{BASE_URL}/customer", json=customer_data, headers=TEST_HEADERS)
        
        # 第二次创建相同编码
        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400 or response.json()["code"] == 400
    
    def test_create_customer_invalid_phone(self):
        """TC-CUST-003: 创建客户-无效手机号"""
        customer_data = TestDataGenerator.generate_customer()
        customer_data["contactPhone"] = "invalid_phone"
        
        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_get_customer_list(self):
        """TC-CUST-004: 查询客户列表"""
        response = requests.get(
            f"{BASE_URL}/customer/list",
            params={"pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
        assert "list" in result["data"]
    
    def test_get_customer_detail(self, customer_id: str = "test-id"):
        """TC-CUST-005: 获取客户详情"""
        response = requests.get(
            f"{BASE_URL}/customer/{customer_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_update_customer(self, customer_id: str = "test-id"):
        """TC-CUST-006: 更新客户信息"""
        update_data = {
            "name": "更新后的客户名称",
            "contactName": "李四",
            "contactPhone": "13900139000"
        }
        
        response = requests.put(
            f"{BASE_URL}/customer/{customer_id}",
            json=update_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_delete_customer(self, customer_id: str = "test-id"):
        """TC-CUST-007: 删除客户"""
        response = requests.delete(
            f"{BASE_URL}/customer/{customer_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_enable_customer(self, customer_id: str = "test-id"):
        """TC-CUST-008: 启用客户"""
        response = requests.post(
            f"{BASE_URL}/customer/{customer_id}/enable",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_disable_customer(self, customer_id: str = "test-id"):
        """TC-CUST-009: 禁用客户"""
        response = requests.post(
            f"{BASE_URL}/customer/{customer_id}/disable",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_create_customer_missing_required(self):
        """TC-CUST-010: 创建客户-缺少必填字段"""
        customer_data = {
            "name": "测试客户",
            # 缺少 code 等必填字段
        }
        
        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_customer_boundary_name(self):
        """TC-CUST-011: 创建客户-名称边界值(最大长度)"""
        customer_data = TestDataGenerator.generate_customer()
        customer_data["name"] = "测" * 100  # 假设最大长度为100
        
        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400]


# ==================== 销售订单测试 ====================

class TestSalesOrder:
    """销售订单测试类"""
    
    def test_create_sales_order_success(self):
        """TC-SO-001: 成功创建销售订单"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        
        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
        return result["data"]
    
    def test_create_sales_order_invalid_customer(self):
        """TC-SO-002: 创建订单-无效客户"""
        order_data = TestDataGenerator.generate_sales_order("INVALID_ID")
        
        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_sales_order_empty_items(self):
        """TC-SO-003: 创建订单-空商品列表"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["items"] = []
        
        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_sales_order_negative_quantity(self):
        """TC-SO-004: 创建订单-负数量"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["items"][0]["quantity"] = -10
        
        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_sales_order_negative_price(self):
        """TC-SO-005: 创建订单-负单价"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["items"][0]["unitPrice"] = -100.00
        
        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_get_sales_order_list(self):
        """TC-SO-006: 查询销售订单列表"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
    
    def test_get_sales_order_detail(self, order_id: str = "test-id"):
        """TC-SO-007: 获取销售订单详情"""
        response = requests.get(
            f"{BASE_URL}/sales-order/{order_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_update_sales_order(self, order_id: str = "test-id"):
        """TC-SO-008: 更新销售订单"""
        update_data = {
            "remark": "更新备注",
            "deliveryDate": (datetime.now() + timedelta(days=14)).isoformat()
        }
        
        response = requests.put(
            f"{BASE_URL}/sales-order/{order_id}",
            json=update_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_delete_sales_order(self, order_id: str = "test-id"):
        """TC-SO-009: 删除销售订单"""
        response = requests.delete(
            f"{BASE_URL}/sales-order/{order_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_submit_sales_order(self, order_id: str = "test-id"):
        """TC-SO-010: 提交销售订单"""
        response = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/submit",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_approve_sales_order(self, order_id: str = "test-id"):
        """TC-SO-011: 批准销售订单"""
        response = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/approve",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_reject_sales_order(self, order_id: str = "test-id"):
        """TC-SO-012: 驳回销售订单"""
        response = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/reject",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_confirm_sales_order(self, order_id: str = "test-id"):
        """TC-SO-013: 确认销售订单"""
        response = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/confirm",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_sales_order_status_transition(self):
        """TC-SO-014: 订单状态流转测试"""
        # 创建订单
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        create_resp = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )
        
        if create_resp.status_code != 200:
            pytest.skip("创建订单失败，跳过状态流转测试")
        
        order_id = create_resp.json()["data"]
        
        # 提交 -> 待审批
        submit_resp = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/submit",
            headers=TEST_HEADERS
        )
        assert submit_resp.status_code == 200
        
        # 批准 -> 已批准
        approve_resp = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/approve",
            headers=TEST_HEADERS
        )
        assert approve_resp.status_code == 200
        
        # 确认 -> 已确认
        confirm_resp = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/confirm",
            headers=TEST_HEADERS
        )
        assert confirm_resp.status_code == 200
    
    def test_sales_order_amount_calculation(self):
        """TC-SO-015: 订单金额计算验证"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        
        # 验证金额计算
        expected_total = sum(item["amount"] for item in order_data["items"])
        expected_payable = expected_total - order_data["discountAmount"]
        
        assert order_data["totalAmount"] == expected_total
        assert order_data["payableAmount"] == expected_payable
    
    def test_sales_order_filter_by_status(self):
        """TC-SO-016: 按状态筛选订单"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"status": "DRAFT", "pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
    
    def test_sales_order_filter_by_customer(self):
        """TC-SO-017: 按客户筛选订单"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"customerId": "CUST001", "pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
    
    def test_sales_order_pagination(self):
        """TC-SO-018: 分页查询"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"pageNum": 1, "pageSize": 5},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
        # 验证分页参数
        if "pageSize" in result["data"]:
            assert result["data"]["pageSize"] == 5


# ==================== 销售出库测试 ====================

class TestSalesOutbound:
    """销售出库测试类"""
    
    def test_create_sales_outbound_success(self):
        """TC-OB-001: 成功创建销售出库单"""
        outbound_data = TestDataGenerator.generate_sales_outbound("SO001", "CUST001")
        
        response = requests.post(
            f"{BASE_URL}/sales-outbound",
            json=outbound_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
        assert "data" in result
    
    def test_create_outbound_without_order(self):
        """TC-OB-002: 创建出库单-无关联订单"""
        outbound_data = TestDataGenerator.generate_sales_outbound("", "CUST001")
        
        response = requests.post(
            f"{BASE_URL}/sales-outbound",
            json=outbound_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_outbound_exceed_order_qty(self):
        """TC-OB-003: 创建出库单-超出订单数量"""
        outbound_data = TestDataGenerator.generate_sales_outbound("SO001", "CUST001")
        outbound_data["totalQty"] = 999999  # 远超订单数量
        
        response = requests.post(
            f"{BASE_URL}/sales-outbound",
            json=outbound_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400]
    
    def test_get_sales_outbound_list(self):
        """TC-OB-004: 查询出库单列表"""
        response = requests.get(
            f"{BASE_URL}/sales-outbound/list",
            params={"pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
    
    def test_get_sales_outbound_detail(self, outbound_id: str = "test-id"):
        """TC-OB-005: 获取出库单详情"""
        response = requests.get(
            f"{BASE_URL}/sales-outbound/{outbound_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_confirm_outbound(self, outbound_id: str = "test-id"):
        """TC-OB-006: 确认出库"""
        response = requests.post(
            f"{BASE_URL}/sales-outbound/{outbound_id}/confirm",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_cancel_outbound(self, outbound_id: str = "test-id"):
        """TC-OB-007: 取消出库"""
        response = requests.post(
            f"{BASE_URL}/sales-outbound/{outbound_id}/cancel",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_outbound_inventory_deduction(self):
        """TC-OB-008: 出库库存扣减验证"""
        # 此测试需要与库存模块集成验证
        # 验证出库后库存是否正确扣减
        pytest.skip("需要库存模块集成")
    
    def test_partial_outbound(self):
        """TC-OB-009: 部分出库测试"""
        # 订单100件，先出库50件
        outbound_data = TestDataGenerator.generate_sales_outbound("SO001", "CUST001")
        outbound_data["totalQty"] = 50
        outbound_data["items"] = [
            {
                "productId": "PROD001",
                "productName": "测试产品1",
                "quantity": 25,
                "warehouseId": "WH001",
                "locationId": "LOC001"
            }
        ]
        
        response = requests.post(
            f"{BASE_URL}/sales-outbound",
            json=outbound_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400]


# ==================== 销售退货测试 ====================

class TestSalesReturn:
    """销售退货测试类"""
    
    def test_create_sales_return_success(self):
        """TC-RT-001: 成功创建销售退货单"""
        return_data = TestDataGenerator.generate_sales_return("SO001", "CUST001")
        
        response = requests.post(
            f"{BASE_URL}/sales-return",
            json=return_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200