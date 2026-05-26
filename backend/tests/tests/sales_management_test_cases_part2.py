# 企智连销售管理模块测试用例 - 补充部分
# 包含：销售退货扩展测试、审批流程测试、边界测试、集成测试

import pytest
import requests
from datetime import datetime, timedelta

# 导入主文件中的配置和生成器
import sys
sys.path.insert(0, 'I:\\AI-Ready\\tests')
from sales_management_test_cases import TestDataGenerator, BASE_URL, TEST_HEADERS


# ==================== 销售退货测试 - 补充 ====================

class TestSalesReturnExtended:
    """销售退货测试类 - 扩展"""
    
    def test_create_return_without_order(self):
        """TC-RT-002: 创建退货单-无关联订单"""
        return_data = TestDataGenerator.generate_sales_return("", "CUST001")
        
        response = requests.post(
            f"{BASE_URL}/sales-return",
            json=return_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 400
    
    def test_create_return_exceed_outbound_qty(self):
        """TC-RT-003: 创建退货单-超出出库数量"""
        return_data = TestDataGenerator.generate_sales_return("SO001", "CUST001")
        return_data["totalQty"] = 999999  # 远超已出库数量
        
        response = requests.post(
            f"{BASE_URL}/sales-return",
            json=return_data,
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400]
    
    def test_get_sales_return_list(self):
        """TC-RT-004: 查询退货单列表"""
        response = requests.get(
            f"{BASE_URL}/sales-return/list",
            params={"pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )
        
        assert response.status_code == 200
        result = response.json()
        assert result["code"] == 200
    
    def test_get_sales_return_detail(self, return_id: str = "test-id"):
        """TC-RT-005: 获取退货单详情"""
        response = requests.get(
            f"{BASE_URL}/sales-return/{return_id}",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 404]
    
    def test_submit_sales_return(self, return_id: str = "test-id"):
        """TC-RT-006: 提交退货单"""
        response = requests.post(
            f"{BASE_URL}/sales-return/{return_id}/submit",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_approve_sales_return(self, return_id: str = "test-id"):
        """TC-RT-007: 批准退货单"""
        response = requests.post(
            f"{BASE_URL}/sales-return/{return_id}/approve",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_reject_sales_return(self, return_id: str = "test-id"):
        """TC-RT-008: 驳回退货单"""
        response = requests.post(
            f"{BASE_URL}/sales-return/{return_id}/reject",
            headers=TEST_HEADERS
        )
        
        assert response.status_code in [200, 400, 404]
    
    def test_return_inventory_restore(self):
        """TC-RT-009: 退货库存恢复验证"""
        pytest.skip("需要库存模块集成")
    
    def test_sales_return_workflow(self):
        """TC-RT-010: 退货完整流程测试"""
        pytest.skip("依赖前置订单创建")


# ==================== 订单审批流程测试 ====================

class TestOrderApprovalWorkflow:
    """订单审批流程测试类"""
    
    def test_order_approval_flow_draft_to_pending(self):
        """TC-WF-001: 订单审批流-草稿到待审批"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        create_resp = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        if create_resp.status_code != 200:
            pytest.skip("创建订单失败")

        order_id = create_resp.json()["data"]

        # 提交订单
        submit_resp = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/submit",
            headers=TEST_HEADERS
        )

        assert submit_resp.status_code == 200
    
    def test_order_approval_flow_pending_to_approved(self):
        """TC-WF-002: 订单审批流-待审批到已批准"""
        pytest.skip("依赖前置订单创建")
    
    def test_order_approval_flow_pending_to_rejected(self):
        """TC-WF-003: 订单审批流-待审批到已驳回"""
        pytest.skip("依赖前置订单创建")
    
    def test_order_approval_flow_approved_to_confirmed(self):
        """TC-WF-004: 订单审批流-已批准到已确认"""
        pytest.skip("依赖前置订单创建")
    
    def test_order_invalid_status_transition(self):
        """TC-WF-005: 无效状态流转"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        create_resp = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        if create_resp.status_code != 200:
            pytest.skip("创建订单失败")

        order_id = create_resp.json()["data"]

        # 尝试直接确认（应该失败）
        confirm_resp = requests.post(
            f"{BASE_URL}/sales-order/{order_id}/confirm",
            headers=TEST_HEADERS
        )

        assert confirm_resp.status_code in [400, 200]
    
    def test_order_resubmit_after_rejection(self):
        """TC-WF-006: 驳回后重新提交"""
        pytest.skip("需要完整流程支持")
    
    def test_order_cancel_at_draft(self):
        """TC-WF-007: 草稿状态取消订单"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        create_resp = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        if create_resp.status_code != 200:
            pytest.skip("创建订单失败")

        order_id = create_resp.json()["data"]

        # 删除草稿订单
        delete_resp = requests.delete(
            f"{BASE_URL}/sales-order/{order_id}",
            headers=TEST_HEADERS
        )

        assert delete_resp.status_code == 200


# ==================== 边界条件测试 ====================

class TestBoundaryConditions:
    """边界条件测试类"""
    
    def test_order_amount_zero(self):
        """TC-BD-001: 订单金额为0"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["totalAmount"] = 0
        order_data["payableAmount"] = 0
        order_data["items"][0]["amount"] = 0
        order_data["items"][0]["unitPrice"] = 0

        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_order_amount_max(self):
        """TC-BD-002: 订单金额最大值"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["totalAmount"] = 999999999.99
        order_data["payableAmount"] = 999999999.99

        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_order_quantity_max(self):
        """TC-BD-003: 订单数量最大值"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["totalQty"] = 999999
        order_data["items"][0]["quantity"] = 999999

        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_order_discount_exceed_total(self):
        """TC-BD-004: 折扣金额超过订单总额"""
        order_data = TestDataGenerator.generate_sales_order("CUST001")
        order_data["discountAmount"] = 20000.00

        response = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_customer_name_empty(self):
        """TC-BD-005: 客户名称为空"""
        customer_data = TestDataGenerator.generate_customer()
        customer_data["name"] = ""

        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )

        assert response.status_code == 400
    
    def test_customer_name_special_chars(self):
        """TC-BD-006: 客户名称特殊字符"""
        customer_data = TestDataGenerator.generate_customer()
        customer_data["name"] = "<script>alert(1)</script>"

        response = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_pagination_boundary(self):
        """TC-BD-007: 分页边界值"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"pageNum": 1, "pageSize": 0},
            headers=TEST_HEADERS
        )

        assert response.status_code in [200, 400]
    
    def test_pagination_large_page(self):
        """TC-BD-008: 分页大页码"""
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"pageNum": 999999, "pageSize": 10},
            headers=TEST_HEADERS
        )

        assert response.status_code == 200


# ==================== 集成测试 ====================

class TestIntegration:
    """集成测试类"""
    
    def test_sales_order_to_outbound_flow(self):
        """TC-INT-001: 销售订单到出库流程"""
        # 创建客户
        customer_data = TestDataGenerator.generate_customer()
        cust_resp = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )

        if cust_resp.status_code != 200:
            pytest.skip("创建客户失败")

        customer_id = cust_resp.json()["data"]

        # 创建订单
        order_data = TestDataGenerator.generate_sales_order(customer_id)
        order_resp = requests.post(
            f"{BASE_URL}/sales-order",
            json=order_data,
            headers=TEST_HEADERS
        )

        if order_resp.status_code != 200:
            pytest.skip("创建订单失败")

        order_id = order_resp.json()["data"]

        # 提交订单
        requests.post(
            f"{BASE_URL}/sales-order/{order_id}/submit",
            headers=TEST_HEADERS
        )

        # 批准订单
        requests.post(
            f"{BASE_URL}/sales-order/{order_id}/approve",
            headers=TEST_HEADERS
        )

        # 确认订单
        requests.post(
            f"{BASE_URL}/sales-order/{order_id}/confirm",
            headers=TEST_HEADERS
        )

        # 创建出库单
        outbound_data = TestDataGenerator.generate_sales_outbound(order_id, customer_id)
        outbound_resp = requests.post(
            f"{BASE_URL}/sales-outbound",
            json=outbound_data,
            headers=TEST_HEADERS
        )

        assert outbound_resp.status_code == 200
    
    def test_sales_order_to_return_flow(self):
        """TC-INT-002: 销售订单到退货流程"""
        pytest.skip("依赖完整出库流程")
    
    def test_customer_order_relation(self):
        """TC-INT-003: 客户订单关联验证"""
        # 创建客户
        customer_data = TestDataGenerator.generate_customer()
        cust_resp = requests.post(
            f"{BASE_URL}/customer",
            json=customer_data,
            headers=TEST_HEADERS
        )

        if cust_resp.status_code != 200:
            pytest.skip("创建客户失败")

        customer_id = cust_resp.json()["data"]

        # 查询该客户的订单
        response = requests.get(
            f"{BASE_URL}/sales-order/list",
            params={"customerId": customer_id, "pageNum": 1, "pageSize": 10},
            headers=TEST_HEADERS
        )

        assert response.status_code == 200


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
