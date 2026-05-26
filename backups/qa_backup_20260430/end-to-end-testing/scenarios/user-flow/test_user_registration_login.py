"""
用户注册到登录端到端测试
测试完整用户流程：注册 -> 验证 -> 登录 -> 密码重置
"""

import pytest
import requests
import sys
import os

# 添加框架路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from framework.base_test import BaseE2ETest
from framework.config import TestConfig


class TestUserRegistrationLogin(BaseE2ETest):
    """用户注册到登录端到端测试"""
    
    def __init__(self):
        super().__init__()
        self.config = TestConfig()
        self.api_base_url = self.config.get('api.base_url', 'http://localhost:8080/api')
        self.test_user = None
    
    def test_user_registration_flow(self):
        """测试用户注册完整流程"""
        self.setup_method()
        
        try:
            # 步骤1: 生成测试用户数据
            test_user = self.utils.generate_test_user()
            self.add_test_step(
                "生成测试用户数据",
                {'username': test_user['username'], 'email': test_user['email']},
                'passed'
            )
            
            # 步骤2: 提交注册请求
            registration_data = {
                'username': test_user['username'],
                'email': test_user['email'],
                'phone': test_user['phone'],
                'password': test_user['password'],
                'name': test_user['name']
            }
            
            response = requests.post(
                f"{self.api_base_url}/auth/register",
                json=registration_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "注册请求应返回201状态码")
            
            response_data = response.json()
            self.assert_true('user_id' in response_data, "响应应包含user_id")
            
            self.add_test_step(
                "提交注册请求",
                {'status_code': response.status_code, 'user_id': response_data.get('user_id')},
                'passed'
            )
            
            # 步骤3: 验证用户已创建
            user_id = response_data['user_id']
            verify_response = requests.get(
                f"{self.api_base_url}/users/{user_id}",
                timeout=30
            )
            
            self.assert_equal(verify_response.status_code, 200, "用户查询应返回200状态码")
            
            self.add_test_step(
                "验证用户已创建",
                {'user_id': user_id, 'status': 'created'},
                'passed'
            )
            
            # 保存测试用户
            self.test_user = test_user
            self.test_user['user_id'] = user_id
            
        except Exception as e:
            self.add_test_step(
                "用户注册流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_user_login_flow(self):
        """测试用户登录完整流程"""
        self.setup_method()
        
        try:
            # 确保有测试用户
            if not self.test_user:
                self.test_user = self.utils.generate_test_user()
                # 模拟注册
                self.test_user['user_id'] = self.utils.generate_unique_id("USER_")
            
            # 步骤1: 提交登录请求
            login_data = {
                'username': self.test_user['username'],
                'password': self.test_user['password']
            }
            
            response = requests.post(
                f"{self.api_base_url}/auth/login",
                json=login_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "登录请求应返回200状态码")
            
            response_data = response.json()
            self.assert_true('token' in response_data, "响应应包含token")
            self.assert_true('user' in response_data, "响应应包含user信息")
            
            self.add_test_step(
                "提交登录请求",
                {'status_code': response.status_code, 'has_token': True},
                'passed'
            )
            
            # 步骤2: 验证token有效
            token = response_data['token']
            verify_response = requests.get(
                f"{self.api_base_url}/auth/verify",
                headers={'Authorization': f'Bearer {token}'},
                timeout=30
            )
            
            self.assert_equal(verify_response.status_code, 200, "Token验证应返回200状态码")
            
            self.add_test_step(
                "验证token有效",
                {'token_valid': True},
                'passed'
            )
            
            # 步骤3: 获取用户信息
            user_response = requests.get(
                f"{self.api_base_url}/users/me",
                headers={'Authorization': f'Bearer {token}'},
                timeout=30
            )
            
            self.assert_equal(user_response.status_code, 200, "用户信息查询应返回200状态码")
            
            user_data = user_response.json()
            self.assert_equal(user_data['username'], self.test_user['username'], "用户名应匹配")
            
            self.add_test_step(
                "获取用户信息",
                {'username': user_data['username']},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "用户登录流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_password_reset_flow(self):
        """测试密码重置流程"""
        self.setup_method()
        
        try:
            # 确保有测试用户
            if not self.test_user:
                self.test_user = self.utils.generate_test_user()
            
            # 步骤1: 请求密码重置
            reset_request_data = {
                'email': self.test_user['email']
            }
            
            response = requests.post(
                f"{self.api_base_url}/auth/password-reset-request",
                json=reset_request_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "密码重置请求应返回200状态码")
            
            self.add_test_step(
                "请求密码重置",
                {'email': self.test_user['email']},
                'passed'
            )
            
            # 步骤2: 验证重置token (模拟)
            reset_token = self.utils.generate_random_string(32)
            
            # 步骤3: 使用新密码重置
            new_password = self.utils.generate_random_string(12)
            reset_data = {
                'token': reset_token,
                'new_password': new_password
            }
            
            response = requests.post(
                f"{self.api_base_url}/auth/password-reset",
                json=reset_data,
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "密码重置应返回200状态码")
            
            self.add_test_step(
                "重置密码",
                {'status': 'success'},
                'passed'
            )
            
            # 更新测试用户密码
            self.test_user['password'] = new_password
            
        except Exception as e:
            self.add_test_step(
                "密码重置流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_complete_user_flow(self):
        """测试完整用户流程"""
        self.setup_method()
        
        try:
            # 执行完整流程
            self.test_user_registration_flow()
            self.test_user_login_flow()
            self.test_password_reset_flow()
            
            self.add_test_step(
                "完整用户流程",
                {'status': 'completed'},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "完整用户流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])