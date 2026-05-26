"""
用户管理模块自动化测试脚本
测试用户管理相关的功能
"""

import time
import logging
import requests
from typing import Dict, List, Any, Optional
from test_framework.test_runner import TestCase, TestSuite, TestStatus, TestRunner
from test_framework.data_manager import TestDataManager, TestDataSchemas

class UserManagementTests:
    """用户管理测试类"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.data_manager = TestDataManager()
        self.logger = logging.getLogger(__name__)
        
        # 用户管理API端点
        self.endpoints = {
            'create_user': f"{base_url}/api/users",
            'get_user': f"{base_url}/api/users/{{user_id}}",
            'update_user': f"{base_url}/api/users/{{user_id}}",
            'delete_user': f"{base_url}/api/users/{{user_id}}",
            'list_users': f"{base_url}/api/users",
            'search_users': f"{base_url}/api/users/search"
        }
    
    def create_test_suite(self) -> TestSuite:
        """创建用户管理测试套件"""
        suite = TestSuite(
            name="用户管理功能测试套件",
            description="测试用户管理的增删改查和搜索功能"
        )
        
        # 添加测试用例
        suite.add_test_case(self.test_user_creation())
        suite.add_test_case(self.test_user_retrieval())
        suite.add_test_case(self.test_user_update())
        suite.add_test_case(self.test_user_deletion())
        suite.add_test_case(self.test_user_listing())
        suite.add_test_case(self.test_user_search())
        suite.add_test_case(self.test_user_validation())
        
        return suite
    
    def generate_test_user(self) -> Dict[str, Any]:
        """生成测试用户数据"""
        schema = TestDataSchemas.user_schema()
        users = self.data_manager.generate_data(schema, count=1)
        return users[0]
    
    def test_user_creation(self) -> TestCase:
        """测试用户创建功能"""
        test_case = TestCase(
            name="test_create_user",
            description="测试创建新用户功能",
            tags=["creation", "api", "user"]
        )
        
        def execute():
            test_case.add_step("准备测试数据", TestStatus.PASSED)
            user_data = self.generate_test_user()
            
            test_case.add_step("发送创建用户请求", TestStatus.PASSED)
            response = requests.post(
                self.endpoints['create_user'],
                json=user_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 201, f"期望状态码201，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            response_data = response.json()
            assert 'id' in response_data, "响应中缺少用户ID"
            assert response_data['username'] == user_data['username'], "用户名不匹配"
            
            # 保存用户ID供后续测试使用
            test_case.user_id = response_data['id']
            test_case.user_data = user_data
            
            test_case.add_step("用户创建测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_retrieval(self) -> TestCase:
        """测试用户查询功能"""
        test_case = TestCase(
            name="test_get_user",
            description="测试根据ID查询用户功能",
            tags=["retrieval", "api", "user"]
        )
        
        def execute():
            # 这个测试依赖于前一个测试创建的用户
            test_case.add_step("检查前置条件", TestStatus.PASSED)
            if not hasattr(self, 'created_user_id'):
                test_case.add_step("跳过测试：没有已创建的用户", TestStatus.SKIPPED)
                return
            
            test_case.add_step("发送查询用户请求", TestStatus.PASSED)
            url = self.endpoints['get_user'].format(user_id=self.created_user_id)
            response = requests.get(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据", TestStatus.PASSED)
            user_data = response.json()
            assert user_data['id'] == self.created_user_id, "用户ID不匹配"
            
            test_case.add_step("用户查询测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_update(self) -> TestCase:
        """测试用户更新功能"""
        test_case = TestCase(
            name="test_update_user",
            description="测试更新用户信息功能",
            tags=["update", "api", "user"]
        )
        
        def execute():
            test_case.add_step("准备更新数据", TestStatus.PASSED)
            update_data = {
                "email": f"updated_{int(time.time())}@test.com",
                "phone": "13800138000"
            }
            
            test_case.add_step("发送更新用户请求", TestStatus.PASSED)
            url = self.endpoints['update_user'].format(user_id=self.created_user_id)
            response = requests.put(
                url,
                json=update_data,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证更新结果", TestStatus.PASSED)
            updated_user = response.json()
            assert updated_user['email'] == update_data['email'], "邮箱更新失败"
            assert updated_user['phone'] == update_data['phone'], "电话更新失败"
            
            test_case.add_step("用户更新测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_deletion(self) -> TestCase:
        """测试用户删除功能"""
        test_case = TestCase(
            name="test_delete_user",
            description="测试删除用户功能",
            tags=["deletion", "api", "user"]
        )
        
        def execute():
            test_case.add_step("发送删除用户请求", TestStatus.PASSED)
            url = self.endpoints['delete_user'].format(user_id=self.created_user_id)
            response = requests.delete(url, timeout=30)
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 204, f"期望状态码204，实际{response.status_code}"
            
            test_case.add_step("验证用户已被删除", TestStatus.PASSED)
            get_response = requests.get(url, timeout=30)
            assert get_response.status_code == 404, "用户删除后仍可查询"
            
            test_case.add_step("用户删除测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_listing(self) -> TestCase:
        """测试用户列表功能"""
        test_case = TestCase(
            name="test_list_users",
            description="测试获取用户列表功能",
            tags=["listing", "api", "user"]
        )
        
        def execute():
            test_case.add_step("发送获取用户列表请求", TestStatus.PASSED)
            response = requests.get(
                self.endpoints['list_users'],
                params={"page": 1, "size": 10},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证响应数据格式", TestStatus.PASSED)
            data = response.json()
            assert 'total' in data, "响应中缺少总数"
            assert 'users' in data, "响应中缺少用户列表"
            assert isinstance(data['users'], list), "用户列表不是数组"
            
            test_case.add_step("用户列表测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_search(self) -> TestCase:
        """测试用户搜索功能"""
        test_case = TestCase(
            name="test_search_users",
            description="测试搜索用户功能",
            tags=["search", "api", "user"]
        )
        
        def execute():
            test_case.add_step("发送搜索用户请求", TestStatus.PASSED)
            response = requests.get(
                self.endpoints['search_users'],
                params={"keyword": "test", "field": "username"},
                timeout=30
            )
            
            test_case.add_step("验证响应状态码", TestStatus.PASSED)
            assert response.status_code == 200, f"期望状态码200，实际{response.status_code}"
            
            test_case.add_step("验证搜索结果", TestStatus.PASSED)
            data = response.json()
            assert isinstance(data, list), "搜索结果应该是列表"
            
            test_case.add_step("用户搜索测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case
    
    def test_user_validation(self) -> TestCase:
        """测试用户数据验证功能"""
        test_case = TestCase(
            name="test_user_validation",
            description="测试用户数据的验证规则",
            tags=["validation", "api", "user"]
        )
        
        def execute():
            test_cases = [
                {
                    "name": "无效邮箱",
                    "data": {"username": "testuser", "email": "invalid-email"},
                    "expected_status": 400
                },
                {
                    "name": "空用户名",
                    "data": {"username": "", "email": "test@example.com"},
                    "expected_status": 400
                },
                {
                    "name": "过短密码",
                    "data": {"username": "testuser", "email": "test@example.com", "password": "123"},
                    "expected_status": 400
                }
            ]
            
            for i, tc in enumerate(test_cases):
                test_case.add_step(f"测试验证场景: {tc['name']}", TestStatus.PASSED)
                
                response = requests.post(
                    self.endpoints['create_user'],
                    json=tc['data'],
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                assert response.status_code == tc['expected_status'], \
                    f"场景 '{tc['name']}' 期望状态码{tc['expected_status']}，实际{response.status_code}"
            
            test_case.add_step("用户验证测试完成", TestStatus.PASSED)
        
        test_case.execute = execute
        return test_case

def run_user_management_tests():
    """运行用户管理测试"""
    # 设置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    logger = logging.getLogger(__name__)
    logger.info("开始执行用户管理模块自动化测试")
    
    try:
        # 创建测试实例
        user_tests = UserManagementTests()
        
        # 创建测试套件
        test_suite = user_tests.create_test_suite()
        
        # 创建测试运行器
        runner = TestRunner()
        runner.add_test_suite(test_suite)
        
        # 运行测试
        results = runner.run_all()
        
        # 生成报告
        report = runner.generate_report(results)
        logger.info("测试报告:\n" + report)
        
        # 保存报告到文件
        with open("user_management_test_report.txt", "w", encoding="utf-8") as f:
            f.write(report)
        
        logger.info("用户管理模块自动化测试完成")
        
        return results
        
    except Exception as e:
        logger.error(f"用户管理测试执行失败: {e}")
        raise

if __name__ == "__main__":
    run_user_management_tests()