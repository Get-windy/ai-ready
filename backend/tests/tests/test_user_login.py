"""
用户登录功能自动化测试脚本
pytest测试框架实现
覆盖正常登录、错误密码、锁定账号等核心场景
"""

import pytest
import requests
from typing import Dict, Any


class TestUserLogin:
    """用户登录功能测试类"""
    
    # 测试基础URL配置
    BASE_URL = "http://localhost:8080/api/v1"
    
    @pytest.fixture
    def setup(self):
        """测试环境准备"""
        # 这里可以添加更多的测试准备逻辑
        yield
        # 测试清理逻辑
    
    def test_normal_login_success(self, setup):
        """测试场景1: 正常登录成功"""
        # 测试数据
        test_data = {
            "username": "testuser",
            "password": "Test123!"
        }
        
        # 发送登录请求
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=test_data
        )
        
        # 验证响应
        assert response.status_code == 200, f"登录失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "token" in response_data, "响应中缺少token字段"
        assert response_data["code"] == 200, f"业务逻辑错误: {response_data.get('message')}"
        
        print(f"✓ 正常登录成功 - Token: {response_data['token'][:20]}...")
    
    def test_invalid_password(self, setup):
        """测试场景2: 错误密码"""
        test_data = {
            "username": "testuser",
            "password": "WrongPassword123!"
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=test_data
        )
        
        # 验证错误响应
        assert response.status_code == 401, f"期望401，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 401, "错误码不匹配"
        assert "密码错误" in response_data.get("message", ""), "错误信息不正确"
        
        print("✓ 错误密码验证通过")
    
    def test_user_locked(self, setup):
        """测试场景3: 账号锁定"""
        # 登录锁定账号
        locked_user = {
            "username": "lockeduser",
            "password": "Test123!"
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=locked_user
        )
        
        # 验证账号锁定响应
        assert response.status_code == 403, f"期望403，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 403, "账号锁定错误码不匹配"
        assert "锁定" in response_data.get("message", ""), "锁定信息不正确"
        
        print("✓ 账号锁定验证通过")
    
    def test_empty_username(self, setup):
        """测试场景4: 空用户名"""
        test_data = {
            "username": "",
            "password": "Test123!"
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=test_data
        )
        
        # 验证参数校验
        assert response.status_code == 400, f"期望400，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 400, "参数错误码不匹配"
        
        print("✓ 空用户名验证通过")
    
    def test_empty_password(self, setup):
        """测试场景5: 空密码"""
        test_data = {
            "username": "testuser",
            "password": ""
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=test_data
        )
        
        # 验证参数校验
        assert response.status_code == 400, f"期望400，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 400, "参数错误码不匹配"
        
        print("✓ 空密码验证通过")


def run_login_tests():
    """运行登录测试并生成报告"""
    print("=" * 60)
    print("用户登录功能测试执行")
    print("=" * 60)
    
    import subprocess
    import sys
    
    # 运行pytest
    result = subprocess.run(
        [sys.executable, "-m", "pytest", __file__, "-v", "--tb=short"],
        cwd="I:\\AI-Ready"
    )
    
    print("=" * 60)
    print("测试执行完成")
    print("=" * 60)
    
    return result.returncode


if __name__ == "__main__":
    exit(run_login_tests())
