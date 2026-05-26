"""
订单管理模块自动化测试脚本
测试订单管理相关的功能
"""

import time
import logging
import requests
from typing import Dict, List, Any, Optional
from test_framework.test_runner import TestCase, TestSuite, TestStatus, TestRunner
from test_framework.data_manager import TestDataManager, TestDataSchemas

class OrderManagementTests:
    """订单管理测试类"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.data_manager = TestDataManager()
        self.logger = logging.getLogger(__name__)
        
        # 订单管理API端点
        self.endpoints = {
            'create_order': f"{base_url}/api/orders",
            'get_order': f"{base_url}/api/orders/{{order_id}}",
            'update_order': f"{base_url}/api/orders/{{order_id}}",
            'delete_order': f"{base_url}/api/orders/{{order_id}}",
            'list_orders': f"{base_url}/api/orders",
            'search_orders': f"{base_url}/api/orders/search",
            'update_status': f"{base_url}/api/orders/{{order_id}}/status",
            'get_order_items': f"{base_url}/api/orders/{{order_id}}/items"
        }
    
    def create_test_suite(self) -> TestSuite:
        """创建订单管理测试套件"""
        suite = TestSuite(
            name="订单管理功能测试套件",
            description="测试订单管理的增删改查、状态变更和搜索功能"
        )
        
        # 添加测试用例
        suite.add_test_case(self.test_order_creation())
        suite.add_test_case(self.test_order_retrieval())
        suite.add_test_case(self.test_order_update())
        suite.add_test_case(self.test_order_status_update())
        suite.add_test_case(self.test_order_deletion())
        suite.add_test_case(self.test_order_listing())
        suite.add_test_case(self.test_order_search())
        suite.add_test_case(self.test_order_items())
        suite.add_test_case(self.test_order_validation())
        
        return suite
    
    def generate_test_order(self) -> Dict[str, Any]:
        """生成测试订单数据"""
        schema = TestDataSchemas.order_schema()
        orders = self.data_manager.generate_data(schema, count=1)
        
        # 添加订单项
        orders[0]['items'] = [
            {
                'product_id': f"PROD_{i:06d}",
                'product_name': f"Test Product {i}",
                'quantity': self.data_manager.generate_number(1, 5),
                'price': self.data_manager.generate_number(10, 100)
            }
            for i in range(1, 4)  # 3个订单项
        ]
        
        # 计算总价
        orders[0]['total_price'] = sum(
            item['quantity'] * item['price'] 
            for item in orders[0]['items']
        )
        
        return orders[0]
    
    def test_order_creation(self) -> TestCase:
        """测试订单创建功能"""
        test_case = TestCase(
            name="test_create_order",
            description="测试创建新订单功能",
            tags=["creation", "api", "order"]
        )
        
        def execute():
            test_case.add_step("准备测试数据", TestStatus.PASSED)
            order_data = self.generate_test_order()
            
            test_case.add_step("发送创建订单请求", TestStatus.PASSED)
            response = requests.post(
                self.endpoints['create_order'],
                json=order_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 201, f"期望状态码201，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            response_data = response.json()
            assert 'id' in response_data, "响应中缺少订单ID"
            assert response_data['customer_id'] == order_data['customer_id'], "客户ID不匹配"
            assert 'items' in response_data, "响应中缺少订单项"
            assert len(response_data['items']) == len(order_data['items']), "订单项数量不匹配"
            
            # 保存订单ID供后续测试使用
            test_case.order_id = response_data['id']
            test_case.order_data = order_data
            
            test_case.add_step("订单创建测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_retrieval(self) -> TestCase:
        """测试订单查询功能"""
        test_case = TestCase(
            name="test_get_order",
            description="测试根据ID查询订单功能",
            tags=["retrieval", "api", "order"]
        )
        
        def execute():
            test_case.add_step("发送查询订单请求", TestStatus.PASSED)
            url = self.endpoints['get_order'].format(order_id=self.created_order_id)
            response = requests.get(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            order_data = response.json()
            assert order_data['id'] == self.created_order_id, "订单ID不匹配"
            assert 'customer_id' in order_data, "响应中缺少客户ID"
            assert 'status' in order_data, "响应中缺少订单状态"
            assert 'total_price' in order_data, "响应中缺少总价"
            
            test_case.add_step("订单查询测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_update(self) -> TestCase:
        """测试订单更新功能"""
        test_case = TestCase(
            name="test_update_order",
            description="测试更新订单信息功能",
            tags=["update", "api", "order"]
        )
        
        def execute():
            test_case.add_step("准备更新数据", TestStatus.PASSED)
            update_data = {
                "shipping_address": "北京市朝阳区测试地址",
                "contact_phone": "13800138000",
                "notes": "测试更新订单信息"
            }
            
            test_case.add_step("发送更新订单请求", TestStatus.PASSED)
            url = self.endpoints['update_order'].format(order_id=self.created_order_id)
            response = requests.put(
                url,
                json=update_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证更新结果", TestStatus.PASSED)
            updated_order = response.json()
            assert updated_order['shipping_address'] == update_data['shipping_address'], "地址更新失败"
            assert updated_order['contact_phone'] == update_data['contact_phone'], "电话更新失败"
            
            test_case.add_step("订单更新测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_status_update(self) -> TestCase:
        """测试订单状态更新功能"""
        test_case = TestCase(
            name="test_update_order_status",
            description="测试更新订单状态功能",
            tags=["status", "api", "order"]
        )
        
        def execute():
            test_statuses = ['processing', 'shipped', 'delivered']
            
            for new_status in test_statuses:
                test_case.add_step(f"更新订单状态为: {new_status}", TestStatus.PASSED)
                
                url = self.endpoints['update_status'].format(order_id=self.created_order_id)
                response = requests.patch(
                    url,
                    json={"status": new_status},
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                assert response.status_code == 200, f"状态更新失败: {new_status}"
                
                # 验证状态已更新
                get_url = self.endpoints['get_order'].format(order_id=self.created_order_id)
                order_response = requests.get(get_url, timeout=30)
                order_data = order_response.json()
                assert order_data['status'] == new_status, f"状态未正确更新为: {new_status}"
            
            test_case.add_step("订单状态更新测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_deletion(self) -> TestCase:
        """测试订单删除功能"""
        test_case = TestCase(
            name="test_delete_order",
            description="测试删除订单功能",
            tags=["deletion", "api", "order"]
        )
        
        def execute():
            test_case.add_step("发送删除订单请求", TestStatus.PASSED)
            url = self.endpoints['delete_order'].format(order_id=self.created_order_id)
            response = requests.delete(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 204, f"期望状态码204，实际{response.status_code}"
            
            test_case.add_step("验证订单已被删除", TestStatus.PASSED)
            get_response = requests.get(url, timeout=30)
            assert get_response.status_code == 404, "订单删除后仍可查询"
            
            test_case.add_step("订单删除测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_listing(self) -> TestCase:
        """测试订单列表功能"""
        test_case = TestCase(
            name="test_list_orders",
            description="测试获取订单列表功能",
            tags=["listing", "api", "order"]
        )
        
        def execute():
            test_case.add_step("发送获取订单列表请求", TestStatus.PASSED)
            response = requests.get(
                self.endpoints['list_orders'],
                params={
                    "page": 1,
                    "size": 20,
                    "sort": "order_date",
                    "direction": "desc"
                },
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据格式", TestStatus.PASSED)
            data = response.json()
            assert 'total' in data, "响应中缺少总数"
            assert 'orders' in data, "响应中缺少订单列表"
            assert isinstance(data['orders'], list), "订单列表不是数组"
            
            if data['orders']:
                order = data['orders'][0]
                assert 'id' in order, "订单缺少ID"
                assert 'customer_id' in order, "订单缺少客户ID"
                assert 'status' in order, "订单缺少状态"
            
            test_case.add_step("订单列表测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_search(self) -> TestCase:
        """测试订单搜索功能"""
        test_case = TestCase(
            name="test_search_orders",
            description="测试搜索订单功能",
            tags=["search", "api", "order"]
        )
        
        def execute():
            search_criteria = [
                {"customer_id": "CUST_", "expected_min": 0},
                {"status": "pending", "expected_min": 0},
                {"start_date": "2024-01-01", "end_date": "2024-12-31", "expected_min": 0}
            ]
            
            for criteria in search_criteria:
                test_case.add_step(f"搜索条件: {criteria}", TestStatus.PASSED)
                
                response = requests.get(
                    self.endpoints['search_orders'],
                    params=criteria,
                    timeout=30
                )
                
                assert response.status_code == 200, f"搜索失败: {criteria}"
                
                data = response.json()
                assert isinstance(data, list), "搜索结果应该是列表"
            
            test_case.add_step("订单搜索测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_items(self) -> TestCase:
        """测试订单项功能"""
        test_case = TestCase(
            name="test_order_items",
            description="测试获取订单项功能",
            tags=["items", "api", "order"]
        )
        
        def execute():
            test_case.add_step("发送获取订单项请求", TestStatus.PASSED)
            url = self.endpoints['get_order_items'].format(order_id=self.created_order_id)
            response = requests.get(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            items = response.json()
            assert isinstance(items, list), "订单项应该是列表"
            
            if items:
                item = items[0]
                assert 'product_id' in item, "订单项缺少产品ID"
                assert 'product_name' in item, "订单项缺少产品名称"
                assert 'quantity' in item, "订单项缺少数量"
                assert 'price' in item, "订单项缺少价格"
            
            test_case.add_step("订单项测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_order_validation(self) -> TestCase:
        """测试订单数据验证功能"""
        test_case = TestCase(
            name="test_order_validation",
            description="测试订单数据的验证规则",
            tags=["validation", "api", "order"]
        )
        
        def execute():
            test_cases = [
                {
                    "name": "无效客户ID",
                    "data": {"customer_id": "", "items": []},
                    "expected_status": 400
                },
                {
                    "name": "空订单项",
                    "data": {"customer_id": "CUST_001", "items": []},
                    "expected_status": 400
                },
                {
                    "name": "无效订单项数量",
                    "data": {
                        "customer_id": "CUST_001",
                        "items": [{"product_id": "PROD_001", "quantity": 0, "price": 10}]
                    },
                    "expected_status": 400
                },
                {
                    "name": "无效价格",
                    "data": {
                        "customer_id": "CUST_001",
                        "items": [{"product_id": "PROD_001", "quantity": 1, "price": -10}]
                    },
                    "expected_status": 400
                }
            ]
            
            for i, tc in enumerate(test_cases):
                test_case.add_step(f"测试验证场景: {tc['name']}", TestStatus.PASSED)
                
                response = requests.post(
                    self.endpoints['create_order'],
                    json=tc['data'],
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                assert response.status_code == tc['expected_status'], \
                    f"场景 '{tc['name']}' 期望状态码{tc['expected_status']}，实际{response.status_code}"
            
            test_case.add_step("订单验证测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case

def run_order_management_tests():
    """运行订单管理测试"""
    # 设置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    logger = logging.getLogger(__name__)
    logger.info("开始执行订单管理模块自动化测试")
    
    try:
        # 创建测试实例
        order_tests = OrderManagementTests()
        
        # 创建测试套件
        test_suite = order_tests.create_test_suite()
        
        # 创建测试运行器
        runner = TestRunner()
        runner.add_test_suite(test_suite)
        
        # 运行测试
        results = runner.run_all()
        
        # 生成报告
        report = runner.generate_report(results)
        logger.info("测试报告:\n" + report)
        
        # 保存报告到文件
        with open("order_management_test_report.txt", "w", encoding="utf-8") as f:
            f.write(report)
        
        logger.info("订单管理模块自动化测试完成")
        
        return results
        
    except Exception as e:
        logger.error(f"订单管理测试执行失败: {e}")
        raise

if __name__ == "__main__":
    run_order_management_tests()