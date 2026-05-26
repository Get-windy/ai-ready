"""
库存管理模块自动化测试脚本
测试库存管理相关的功能
"""

import time
import logging
import requests
from typing import Dict, List, Any, Optional
from test_framework.test_runner import TestCase, TestSuite, TestStatus, TestRunner
from test_framework.data_manager import TestDataManager, TestDataSchemas

class InventoryManagementTests:
    """库存管理测试类"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.data_manager = TestDataManager()
        self.logger = logging.getLogger(__name__)
        
        # 库存管理API端点
        self.endpoints = {
            'create_product': f"{base_url}/api/inventory/products",
            'get_product': f"{base_url}/api/inventory/products/{{product_id}}",
            'update_product': f"{base_url}/api/inventory/products/{{product_id}}",
            'delete_product': f"{base_url}/api/inventory/products/{{product_id}}",
            'list_products': f"{base_url}/api/inventory/products",
            'search_products': f"{base_url}/api/inventory/products/search",
            'update_stock': f"{base_url}/api/inventory/products/{{product_id}}/stock",
            'check_availability': f"{base_url}/api/inventory/availability",
            'low_stock_alerts': f"{base_url}/api/inventory/alerts/low-stock",
            'inventory_report': f"{base_url}/api/inventory/report"
        }
    
    def create_test_suite(self) -> TestSuite:
        """创建库存管理测试套件"""
        suite = TestSuite(
            name="库存管理功能测试套件",
            description="测试库存管理的增删改查、库存更新、可用性检查和报表功能"
        )
        
        # 添加测试用例
        suite.add_test_case(self.test_product_creation())
        suite.add_test_case(self.test_product_retrieval())
        suite.add_test_case(self.test_product_update())
        suite.add_test_case(self.test_stock_update())
        suite.add_test_case(self.test_product_deletion())
        suite.add_test_case(self.test_product_listing())
        suite.add_test_case(self.test_product_search())
        suite.add_test_case(self.test_availability_check())
        suite.add_test_case(self.test_low_stock_alerts())
        suite.add_test_case(self.test_inventory_report())
        suite.add_test_case(self.test_inventory_validation())
        
        return suite
    
    def generate_test_product(self) -> Dict[str, Any]:
        """生成测试产品数据"""
        schema = TestDataSchemas.inventory_schema()
        products = self.data_manager.generate_data(schema, count=1)
        
        # 添加额外字段
        products[0]['sku'] = f"SKU_{int(time.time()) % 1000000:06d}"
        products[0]['description'] = f"测试产品描述 {products[0]['product_name']}"
        products[0]['supplier'] = f"供应商_{self.data_manager.generate_string(6)}"
        products[0]['reorder_point'] = self.data_manager.generate_number(10, 50)
        
        return products[0]
    
    def test_product_creation(self) -> TestCase:
        """测试产品创建功能"""
        test_case = TestCase(
            name="test_create_product",
            description="测试创建新产品功能",
            tags=["creation", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("准备测试数据", TestStatus.PASSED)
            product_data = self.generate_test_product()
            
            test_case.add_step("发送创建产品请求", TestStatus.PASSED)
            response = requests.post(
                self.endpoints['create_product'],
                json=product_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 201, f"期望状态码201，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            response_data = response.json()
            assert 'id' in response_data, "响应中缺少产品ID"
            assert response_data['sku'] == product_data['sku'], "SKU不匹配"
            assert response_data['product_name'] == product_data['product_name'], "产品名称不匹配"
            assert response_data['quantity'] == product_data['quantity'], "库存数量不匹配"
            
            # 保存产品ID供后续测试使用
            test_case.product_id = response_data['id']
            test_case.product_data = product_data
            
            test_case.add_step("产品创建测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_product_retrieval(self) -> TestCase:
        """测试产品查询功能"""
        test_case = TestCase(
            name="test_get_product",
            description="测试根据ID查询产品功能",
            tags=["retrieval", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("发送查询产品请求", TestStatus.PASSED)
            url = self.endpoints['get_product'].format(product_id=self.created_product_id)
            response = requests.get(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            product_data = response.json()
            assert product_data['id'] == self.created_product_id, "产品ID不匹配"
            assert 'sku' in product_data, "响应中缺少SKU"
            assert 'product_name' in product_data, "响应中缺少产品名称"
            assert 'quantity' in product_data, "响应中缺少库存数量"
            assert 'price' in product_data, "响应中缺少价格"
            assert 'in_stock' in product_data, "响应中缺少库存状态"
            
            test_case.add_step("产品查询测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_product_update(self) -> TestCase:
        """测试产品更新功能"""
        test_case = TestCase(
            name="test_update_product",
            description="测试更新产品信息功能",
            tags=["update", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("准备更新数据", TestStatus.PASSED)
            update_data = {
                "product_name": f"更新产品_{int(time.time()) % 1000}",
                "description": "这是更新后的产品描述",
                "price": self.data_manager.generate_number(100, 500),
                "reorder_point": self.data_manager.generate_number(20, 100)
            }
            
            test_case.add_step("发送更新产品请求", TestStatus.PASSED)
            url = self.endpoints['update_product'].format(product_id=self.created_product_id)
            response = requests.put(
                url,
                json=update_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证更新结果", TestStatus.PASSED)
            updated_product = response.json()
            assert updated_product['product_name'] == update_data['product_name'], "产品名称更新失败"
            assert updated_product['description'] == update_data['description'], "描述更新失败"
            assert updated_product['price'] == update_data['price'], "价格更新失败"
            
            test_case.add_step("产品更新测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_stock_update(self) -> TestCase:
        """测试库存更新功能"""
        test_case = TestCase(
            name="test_update_stock",
            description="测试更新产品库存功能",
            tags=["stock", "api", "inventory"]
        )
        
        def execute():
            stock_operations = [
                {"operation": "add", "quantity": 10, "reason": "采购入库"},
                {"operation": "subtract", "quantity": 5, "reason": "销售出库"},
                {"operation": "set", "quantity": 50, "reason": "库存调整"}
            ]
            
            for operation in stock_operations:
                test_case.add_step(
                    f"库存操作: {operation['operation']} {operation['quantity']} 件", 
                    TestStatus.PASSED
                )
                
                url = self.endpoints['update_stock'].format(product_id=self.created_product_id)
                response = requests.patch(
                    url,
                    json={
                        "operation": operation['operation'],
                        "quantity": operation['quantity'],
                        "reason": operation['reason']
                    },
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                assert response.status_code == 200, f"库存操作失败: {operation}"
                
                # 验证库存已更新
                get_url = self.endpoints['get_product'].format(product_id=self.created_product_id)
                product_response = requests.get(get_url, timeout=30)
                product_data = product_response.json()
                
                # 验证库存数量
                quantity = product_data['quantity']
                assert quantity >= 0, "库存数量不能为负数"
                
                # 验证库存状态
                in_stock = product_data['in_stock']
                if operation['operation'] == 'set':
                    assert quantity == operation['quantity'], f"设置库存失败: 期望{operation['quantity']}，实际{quantity}"
                
                test_case.add_step(f"当前库存: {quantity}件, 库存状态: {in_stock}", TestStatus.PASSED)
            
            test_case.add_step("库存更新测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_product_deletion(self) -> TestCase:
        """测试产品删除功能"""
        test_case = TestCase(
            name="test_delete_product",
            description="测试删除产品功能",
            tags=["deletion", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("发送删除产品请求", TestStatus.PASSED)
            url = self.endpoints['delete_product'].format(product_id=self.created_product_id)
            response = requests.delete(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 204, f"期望状态码204，实际{response.status_code}"
            
            test_case.add_step("验证产品已被删除", TestStatus.PASSED)
            get_response = requests.get(url, timeout=30)
            assert get_response.status_code == 404, "产品删除后仍可查询"
            
            test_case.add_step("产品删除测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_product_listing(self) -> TestCase:
        """测试产品列表功能"""
        test_case = TestCase(
            name="test_list_products",
            description="测试获取产品列表功能",
            tags=["listing", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("发送获取产品列表请求", TestStatus.PASSED)
            response = requests.get(
                self.endpoints['list_products'],
                params={
                    "page": 1,
                    "size": 50,
                    "sort": "product_name",
                    "category": "",
                    "in_stock_only": "false"
                },
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据格式", TestStatus.PASSED)
            data = response.json()
            assert 'total' in data, "响应中缺少总数"
            assert 'products' in data, "响应中缺少产品列表"
            assert isinstance(data['products'], list), "产品列表不是数组"
            
            if data['products']:
                product = data['products'][0]
                assert 'id' in product, "产品缺少ID"
                assert 'sku' in product, "产品缺少SKU"
                assert 'product_name' in product, "产品缺少名称"
                assert 'quantity' in product, "产品缺少库存数量"
                assert 'price' in product, "产品缺少价格"
            
            test_case.add_step("产品列表测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_product_search(self) -> TestCase:
        """测试产品搜索功能"""
        test_case = TestCase(
            name="test_search_products",
            description="测试搜索产品功能",
            tags=["search", "api", "inventory"]
        )
        
        def execute():
            search_criteria = [
                {"keyword": "test", "field": "product_name"},
                {"category": "Electronics", "min_price": 0, "max_price": 1000},
                {"in_stock": "true", "sort": "price", "direction": "asc"}
            ]
            
            for criteria in search_criteria:
                test_case.add_step(f"搜索条件: {criteria}", TestStatus.PASSED)
                
                response = requests.get(
                    self.endpoints['search_products'],
                    params=criteria,
                    timeout=30
                )
                
                assert response.status_code == 200, f"搜索失败: {criteria}"
                
                data = response.json()
                assert isinstance(data, list), "搜索结果应该是列表"
                
                if data:
                    product = data[0]
                    assert 'product_name' in product, "产品缺少名称"
            
            test_case.add_step("产品搜索测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_availability_check(self) -> TestCase:
        """测试产品可用性检查功能"""
        test_case = TestCase(
            name="test_check_availability",
            description="测试检查产品可用性功能",
            tags=["availability", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("准备可用性检查请求", TestStatus.PASSED)
            check_items = [
                {"product_id": self.created_product_id, "quantity": 2},
                {"product_id": "PROD_999999", "quantity": 1},  # 不存在的产品
                {"sku": "SKU_TEST_001", "quantity": 5}
            ]
            
            test_case.add_step("发送可用性检查请求", TestStatus.PASSED)
            response = requests.post(
                self.endpoints['check_availability'],
                json={"items": check_items},
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            availability_data = response.json()
            assert 'results' in availability_data, "响应中缺少检查结果"
            assert isinstance(availability_data['results'], list), "结果应该是列表"
            
            for result in availability_data['results']:
                assert 'available' in result, "结果中缺少可用性状态"
                assert 'product_id' in result or 'sku' in result, "结果中缺少产品标识"
                if 'available' in result and result['available']:
                    assert 'available_quantity' in result, "可用时缺少可用数量"
            
            test_case.add_step("可用性检查测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_low_stock_alerts(self) -> TestCase:
        """测试低库存预警功能"""
        test_case = TestCase(
            name="test_low_stock_alerts",
            description="测试低库存预警功能",
            tags=["alerts", "api", "inventory"]
        )
        
        def execute():
            test_case.add_step("发送获取低库存预警请求", TestStatus.PASSED)
            response = requests.get(
                self.endpoints['low_stock_alerts'],
                params={"threshold": 10},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            alerts = response.json()
            assert isinstance(alerts, list), "预警列表应该是数组"
            
            if alerts:
                alert = alerts[0]
                assert 'product_id' in alert, "预警缺少产品ID"
                assert 'product_name' in alert, "预警缺少产品名称"
                assert 'current_stock' in alert, "预警缺少当前库存"
                assert 'reorder_point' in alert, "预警缺少补货点"
                assert current_stock <= reorder_point, "当前库存应小于等于补货点"
            
            test_case.add_step("低库存预警测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_inventory_report(self) -> TestCase:
        """测试库存报表功能"""
        test_case = TestCase(
            name="test_inventory_report",
            description="测试生成库存报表功能",
            tags=["report", "api", "inventory"]
        )
        
        def execute():
            report_types = [
                {"type": "summary", "format": "json"},
                {"type": "category", "group_by": "category", "format": "json"},
                {"type": "value", "calculate_total_value": "true", "format": "json"}
            ]
            
            for report_params in report_types:
                test_case.add_step(f"生成报表类型: {report_params['type']}", TestStatus.PASSED)
                
                response = requests.get(
                    self.endpoints['inventory_report'],
                    params=report_params,
                    timeout=30
                )
                
                assert response.status_code == 200, f"报表生成失败: {report_params}"
                
                report_data = response.json()
                assert 'report_type' in report_data, "报表缺少类型"
                assert 'generated_at' in report_data, "报表缺少生成时间"
                
                if report_params['type'] == 'summary':
                    assert 'total_products' in report_data, "汇总报表缺少产品总数"
                    assert 'total_value' in report_data, "汇总报表缺少总价值"
                elif report_params['type'] == 'category':
                    assert 'categories' in report_data, "分类报表缺少分类数据"
            
            test_case.add_step("库存报表测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_inventory_validation(self) -> TestCase:
        """测试库存数据验证功能"""
        test_case = TestCase(
            name="test_inventory_validation",
            description="测试库存数据的验证规则",
            tags=["validation", "api", "inventory"]
        )
        
        def execute():
            test_cases = [
                {
                    "name": "无效SKU",
                    "data": {"sku": "", "product_name": "测试产品", "quantity": 10, "price": 100},
                    "expected_status": 400
                },
                {
                    "name": "空产品名称",
                    "data": {"sku": "SKU_TEST_001", "product_name": "", "quantity": 10, "price": 100},
                    "expected_status": 400
                },
                {
                    "name": "负库存数量",
                    "data": {"sku": "SKU_TEST_002", "product_name": "测试产品", "quantity": -5, "price": 100},
                    "expected_status": 400
                },
                {
                    "name": "零或负价格",
                    "data": {"sku": "SKU_TEST_003", "product_name": "测试产品", "quantity": 10, "price": 0},
                    "expected_status": 400
                },
                {
                    "name": "无效分类",
                    "data": {"sku": "SKU_TEST_004", "product_name": "测试产品", "quantity": 10, "price": 100, "category": "InvalidCategory"},
                    "expected_status": 400
                }
            ]
            
            for i, tc in enumerate(test_cases):
                test_case.add_step(f"测试验证场景: {tc['name']}", TestStatus.PASSED)
                
                response = requests.post(
                    self.endpoints['create_product'],
                    json=tc['data'],
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                assert response.status_code == tc['expected_status'], \
                    f"场景 '{tc['name']}' 期望状态码{tc['expected_status']}，实际{response.status_code}"
            
            test_case.add_step("库存验证测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case

def run_inventory_management_tests():
    """运行库存管理测试"""
    # 设置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    logger = logging.getLogger(__name__)
    logger.info("开始执行库存管理模块自动化测试")
    
    try:
        # 创建测试实例
        inventory_tests = InventoryManagementTests()
        
        # 创建测试套件
        test_suite = inventory_tests.create_test_suite()
        
        # 创建测试运行器
        runner = TestRunner()
        runner.add_test_suite(test_suite)
        
        # 运行测试
        results = runner.run_all()
        
        # 生成报告
        report = runner.generate_report(results)
        logger.info("测试报告:\n" + report)
        
        # 保存报告到文件
        with open("inventory_management_test_report.txt", "w", encoding="utf-8") as f:
            f.write(report)
        
        logger.info("库存管理模块自动化测试完成")
        
        return results
        
    except Exception as e:
        logger.error(f"库存管理测试执行失败: {e}")
        raise

if __name__ == "__main__":
    run_inventory_management_tests()