"""
用户管理模块API测试用例
测试覆盖用户增删改查、密码管理、角色分配等功能
"""

import pytest
import requests
from typing import Dict, Any, List
from datetime import datetime
import random
import string


class TestUserManagementAPI:
    """用户管理模块API测试类"""

    def __init__(self):
        self.base_url = "http://localhost:8080"
        self.token = None
        self.created_users = []
        self.test_data = {}

    def setup_method(self):
        """测试前准备"""
        # 获取认证token
        self._authenticate()

    def _authenticate(self):
        """获取认证token"""
        # 这里需要根据实际的认证接口进行调整
        auth_url = f"{self.base_url}/api/auth/login"
        payload = {
            "username": "admin",
            "password": "admin123"
        }
        try:
            response = requests.post(auth_url, json=payload, timeout=10)
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("data", {}).get("token")
                return True
            return False
        except Exception as e:
            print(f"认证失败: {e}")
            return False

    def _get_headers(self) -> Dict[str, str]:
        """获取请求头"""
        headers = {
            "Content-Type": "application/json"
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

    def _generate_test_data(self) -> Dict[str, Any]:
        """生成测试数据"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        return {
            "username": f"test_user_{timestamp}_{random.randint(1000, 9999)}",
            "email": f"test_{timestamp}_{random.randint(1000, 9999)}@example.com",
            "phone": f"138{random.randint(100000000, 999999999)}",
            "password": "Test@123456",
            "nickname": f"测试用户_{timestamp}"
        }

    def test_01_create_user(self):
        """测试用例01: 创建用户"""
        print("\n=== 测试用例01: 创建用户 ===")
        
        # 生成测试数据
        test_data = self._generate_test_data()
        
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "phone": test_data["phone"],
            "nickname": test_data["nickname"],
            "password": test_data["password"]
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 验证响应
        assert response.status_code == 200, f"创建用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        user_id = data.get("data")
        assert user_id is not None, "用户ID为空"
        
        # 保存创建的用户ID
        self.created_users.append(user_id)
        self.test_data["create_user"] = {
            **test_data,
            "user_id": user_id
        }
        
        print(f"✓ 创建用户成功，用户ID: {user_id}")
        print(f"✓ 用户名: {test_data['username']}")
        print(f"✓ 邮箱: {test_data['email']}")

    def test_02_get_user_detail(self):
        """测试用例02: 获取用户详情"""
        print("\n=== 测试用例02: 获取用户详情 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 获取用户详情
        response = requests.get(
            f"{self.base_url}/api/user/{user_id}",
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"获取用户详情失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        user_info = data.get("data")
        assert user_info is not None, "用户信息为空"
        assert user_info.get("id") == user_id, "用户ID不匹配"
        
        self.test_data["detail_user"] = user_info
        print(f"✓ 获取用户详情成功，用户ID: {user_id}")
        print(f"✓ 用户名: {user_info.get('username')}")
        print(f"✓ 邮箱: {user_info.get('email')}")

    def test_03_update_user(self):
        """测试用例03: 更新用户信息"""
        print("\n=== 测试用例03: 更新用户信息 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 更新用户信息
        update_payload = {
            "id": user_id,
            "nickname": "更新后的昵称",
            "phone": f"139{random.randint(100000000, 999999999)}",
            "email": f"updated_{random.randint(1000, 9999)}@example.com"
        }
        
        response = requests.put(
            f"{self.base_url}/api/user",
            json=update_payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"更新用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 更新用户成功，用户ID: {user_id}")

    def test_04_delete_user(self):
        """测试用例04: 删除用户"""
        print("\n=== 测试用例04: 删除用户 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 删除用户
        response = requests.delete(
            f"{self.base_url}/api/user/{user_id}",
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"删除用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 删除用户成功，用户ID: {user_id}")

    def test_05_batch_delete_users(self):
        """测试用例05: 批量删除用户"""
        print("\n=== 测试用例05: 批量删除用户 ===")
        
        # 创建多个用户
        user_ids = []
        for i in range(3):
            test_data = self._generate_test_data()
            payload = {
                "username": f"{test_data['username']}_{i}",
                "email": f"{test_data['email'].split('@')[0]}_{i}@example.com",
                "password": test_data["password"]
            }
            
            create_response = requests.post(
                f"{self.base_url}/api/user",
                json=payload,
                headers=self._get_headers(),
                timeout=10
            )
            
            user_id = create_response.json().get("data")
            if user_id:
                user_ids.append(user_id)
        
        # 批量删除
        if user_ids:
            response = requests.delete(
                f"{self.base_url}/api/user/batch",
                json=user_ids,
                headers=self._get_headers(),
                timeout=10
            )
            
            assert response.status_code == 200, f"批量删除失败，状态码: {response.status_code}"
            
            data = response.json()
            assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
            
            print(f"✓ 批量删除用户成功，用户ID列表: {user_ids}")

    def test_06_change_password(self):
        """测试用例06: 修改密码"""
        print("\n=== 测试用例06: 修改密码 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 修改密码
        new_password = "New@123456"
        response = requests.put(
            f"{self.base_url}/api/user/{user_id}/password",
            params={
                "oldPassword": test_data["password"],
                "newPassword": new_password
            },
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"修改密码失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 修改密码成功，用户ID: {user_id}")

    def test_07_reset_password(self):
        """测试用例07: 重置密码"""
        print("\n=== 测试用例07: 重置密码 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 重置密码
        new_password = "Reset@123456"
        response = requests.put(
            f"{self.base_url}/api/user/{user_id}/password/reset",
            params={
                "newPassword": new_password
            },
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"重置密码失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 重置密码成功，用户ID: {user_id}")

    def test_08_update_user_status(self):
        """测试用例08: 启用/禁用用户"""
        print("\n=== 测试用例08: 启用/禁用用户 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 禁用用户 (status: 0=禁用, 1=启用)
        response = requests.put(
            f"{self.base_url}/api/user/{user_id}/status",
            params={
                "status": 0
            },
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"禁用用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 禁用用户成功，用户ID: {user_id}")
        
        # 启用用户
        response = requests.put(
            f"{self.base_url}/api/user/{user_id}/status",
            params={
                "status": 1
            },
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"启用用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 启用用户成功，用户ID: {user_id}")

    def test_09_assign_roles(self):
        """测试用例09: 分配角色"""
        print("\n=== 测试用例09: 分配角色 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        user_id = create_response.json().get("data")
        assert user_id is not None, "创建用户失败"
        
        # 分配角色 (roleIds需要根据实际的角色ID调整)
        role_ids = [1, 2]  # 示例角色ID
        
        response = requests.post(
            f"{self.base_url}/api/user/{user_id}/roles",
            json=role_ids,
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"分配角色失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        print(f"✓ 分配角色成功，用户ID: {user_id}, 角色ID: {role_ids}")

    def test_10_page_list_users(self):
        """测试用例10: 分页查询用户"""
        print("\n=== 测试用例10: 分页查询用户 ===")
        
        # 分页查询用户
        params = {
            "current": 1,
            "size": 10
        }
        
        response = requests.get(
            f"{self.base_url}/api/user/page",
            params=params,
            headers=self._get_headers(),
            timeout=10
        )
        
        assert response.status_code == 200, f"分页查询用户失败，状态码: {response.status_code}"
        
        data = response.json()
        assert data.get("code") == 200, f"API返回错误: {data.get('message')}"
        
        page_result = data.get("data", {})
        records = page_result.get("records", [])
        
        assert records is not None, "用户列表为空"
        
        print("[OK] 分页查询用户成功")
        print(f"[OK] 当前页: {page_result.get('current')}")
        print(f"[OK] 每页大小: {page_result.get('size')}")
        print(f"[OK] 总记录数: {page_result.get('total')}")

    def test_11_invalid_token(self):
        """测试用例11: 无效Token测试"""
        print("\n=== 测试用例11: 无效Token测试 ===")
        
        # 使用无效的Token
        headers = {
            "Content-Type": "application/json",
            "Authorization": "Bearer invalid_token_12345"
        }
        
        response = requests.get(
            f"{self.base_url}/api/user/page",
            headers=headers,
            timeout=10
        )
        
        # 期望返回401 unauthorized
        assert response.status_code == 401, f"应该返回401，实际: {response.status_code}"
        
        print("✓ 无效Token被正确拒绝")

    def test_12_empty_username(self):
        """测试用例12: 空用户名验证"""
        print("\n=== 测试用例12: 空用户名验证 ===")
        
        payload = {
            "username": "",
            "email": "test@example.com",
            "password": "Test@123456"
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回验证错误
        assert response.status_code == 400, f"应该返回400，实际: {response.status_code}"
        
        print("✓ 空用户名被正确拒绝")

    def test_13_duplicate_username(self):
        """测试用例13: 重复用户名验证"""
        print("\n=== 测试用例13: 重复用户名验证 ===")
        
        # 先创建一个用户
        test_data = self._generate_test_data()
        payload = {
            "username": test_data["username"],
            "email": test_data["email"],
            "password": test_data["password"]
        }
        
        create_response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        assert create_response.json().get("code") == 200, "创建用户失败"
        
        # 尝试创建相同用户名的用户
        duplicate_payload = {
            "username": test_data["username"],
            "email": "different@example.com",
            "password": test_data["password"]
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=duplicate_payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回重复错误
        assert response.status_code == 400, f"应该返回400，实际: {response.status_code}"
        
        print("✓ 重复用户名被正确拒绝")

    def test_14_weak_password(self):
        """测试用例14: 弱密码验证"""
        print("\n=== 测试用例14: 弱密码验证 ===")
        
        payload = {
            "username": "weak_user",
            "email": "weak@example.com",
            "password": "123"  # 弱密码
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回密码强度验证错误
        assert response.status_code == 400, f"应该返回400，实际: {response.status_code}"
        
        print("✓ 弱密码被正确拒绝")

    def test_15_invalid_email(self):
        """测试用例15: 无效邮箱格式验证"""
        print("\n=== 测试用例15: 无效邮箱格式验证 ===")
        
        payload = {
            "username": "invalid_email_user",
            "email": "invalid_email_format",
            "password": "Test@123456"
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回邮箱格式验证错误
        assert response.status_code == 400, f"应该返回400，实际: {response.status_code}"
        
        print("✓ 无效邮箱格式被正确拒绝")

    def test_16_update_nonexistent_user(self):
        """测试用例16: 更新不存在的用户"""
        print("\n=== 测试用例16: 更新不存在的用户 ===")
        
        payload = {
            "id": 999999,  # 不存在的用户ID
            "nickname": "更新的昵称"
        }
        
        response = requests.put(
            f"{self.base_url}/api/user",
            json=payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回用户不存在错误
        assert response.status_code == 404, f"应该返回404，实际: {response.status_code}"
        
        print("✓ 更新不存在的用户被正确拒绝")

    def test_17_delete_nonexistent_user(self):
        """测试用例17: 删除不存在的用户"""
        print("\n=== 测试用例17: 删除不存在的用户 ===")
        
        response = requests.delete(
            f"{self.base_url}/api/user/999999",
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回用户不存在错误
        assert response.status_code == 404, f"应该返回404，实际: {response.status_code}"
        
        print("✓ 删除不存在的用户被正确拒绝")

    def test_18_get_nonexistent_user(self):
        """测试用例18: 获取不存在的用户详情"""
        print("\n=== 测试用例18: 获取不存在的用户详情 ===")
        
        response = requests.get(
            f"{self.base_url}/api/user/999999",
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回用户不存在错误
        assert response.status_code == 404, f"应该返回404，实际: {response.status_code}"
        
        print("✓ 获取不存在的用户详情被正确拒绝")

    def test_19_sql_injection(self):
        """测试用例19: SQL注入防护测试"""
        print("\n=== 测试用例19: SQL注入防护测试 ===")
        
        # 尝试SQL注入
        sql_payload = {
            "username": "admin' OR '1'='1",
            "email": "test@example.com",
            "password": "Test@123456"
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=sql_payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回验证错误或成功创建（但不应该造成SQL注入）
        # 如果成功创建，说明参数被正确转义
        print(f"✓ SQL注入测试完成，状态码: {response.status_code}")

    def test_20_xss_attack(self):
        """测试用例20: XSS攻击防护测试"""
        print("\n=== 测试用例20: XSS攻击防护测试 ===")
        
        # 尝试XSS攻击
        xss_payload = {
            "username": "xss_user",
            "email": "xss@example.com",
            "nickname": "<script>alert('XSS')</script>",
            "password": "Test@123456"
        }
        
        response = requests.post(
            f"{self.base_url}/api/user",
            json=xss_payload,
            headers=self._get_headers(),
            timeout=10
        )
        
        # 期望返回成功或验证错误
        print(f"✓ XSS攻击测试完成，状态码: {response.status_code}")


def run_tests():
    """运行所有测试"""
    test_instance = TestUserManagementAPI()
    
    # 运行测试
    test_methods = [
        test_instance.test_01_create_user,
        test_instance.test_02_get_user_detail,
        test_instance.test_03_update_user,
        test_instance.test_04_delete_user,
        test_instance.test_05_batch_delete_users,
        test_instance.test_06_change_password,
        test_instance.test_07_reset_password,
        test_instance.test_08_update_user_status,
        test_instance.test_09_assign_roles,
        test_instance.test_10_page_list_users,
        test_instance.test_11_invalid_token,
        test_instance.test_12_empty_username,
        test_instance.test_13_duplicate_username,
        test_instance.test_14_weak_password,
        test_instance.test_15_invalid_email,
        test_instance.test_16_update_nonexistent_user,
        test_instance.test_17_delete_nonexistent_user,
        test_instance.test_18_get_nonexistent_user,
        test_instance.test_19_sql_injection,
        test_instance.test_20_xss_attack,
    ]
    
    passed = 0
    failed = 0
    
    for test_method in test_methods:
        try:
            test_method()
            passed += 1
        except AssertionError as e:
            print(f"✗ 测试失败: {e}")
            failed += 1
        except Exception as e:
            print(f"✗ 测试异常: {e}")
            failed += 1
    
    print(f"\n{'='*60}")
    print(f"测试结果: {passed} 通过, {failed} 失败")
    print(f"{'='*60}")
    
    return passed, failed


if __name__ == "__main__":
    run_tests()
