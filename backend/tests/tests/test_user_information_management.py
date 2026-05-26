#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户信息管理自动化测试脚本
pytest测试框架实现
覆盖用户信息查询、修改、头像上传等核心场景
"""

import pytest
import requests
from typing import Dict, Any
import time
import os


class TestUserInfoManagement:
    """用户信息管理功能测试类"""
    
    # 测试基础URL配置
    BASE_URL = "http://localhost:8080/api/v1"
    TOKEN = None
    
    @pytest.fixture(scope='class', autouse=True)
    def setup_class(self):
        """测试环境准备 - 获取认证Token"""
        # 配置文件读取或使用默认值
        self.BASE_URL = os.getenv('API_BASE_URL', self.BASE_URL)
        
        # 登录获取Token
        login_data = {
            "username": "admin",
            "password": os.getenv('ADMIN_PASSWORD', 'Admin123!')
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=login_data,
            timeout=10
        )
        
        if response.status_code == 200:
            self.TOKEN = response.json().get("token")
            print("✓ 成功获取认证Token")
        else:
            pytest.fail(f"登录失败，状态码: {response.status_code}, 响应: {response.text}")
        
        yield
        
        # 清理逻辑
        self.TOKEN = None
    
    def test_get_user_info(self, setup_class):
        """测试场景1: 查询用户个人信息"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 获取当前用户信息
        response = requests.get(
            f"{self.BASE_URL}/users/me",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"获取用户信息失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "id" in response_data["data"], "响应缺少用户ID"
        assert "username" in response_data["data"], "响应缺少用户名"
        assert "email" in response_data["data"], "响应缺少邮箱"
        
        print(f"✓ 用户信息查询成功，用户ID: {response_data['data']['id']}")
    
    def test_get_user_account_info(self, setup_class):
        """测试场景2: 查询用户账户信息"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 获取当前用户账户信息
        response = requests.get(
            f"{self.BASE_URL}/users/me/account",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"获取账户信息失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "id" in response_data["data"], "响应缺少用户ID"
        assert "status" in response_data["data"], "响应缺少账户状态"
        assert "createdAt" in response_data["data"], "响应缺少创建时间"
        
        print(f"✓ 用户账户信息查询成功")
    
    def test_update_user_basic_info(self, setup_class):
        """测试场景3: 更新用户基本信息"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 更新基本信息
        update_data = {
            "realName": f"测试用户_{int(time.time())}",
            "phone": "13800138000",
            "gender": "male",
            "birthDate": "1990-01-01"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/me/profile",
            json=update_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"更新基本信息失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["data"]["realName"] == update_data["realName"], "姓名未更新"
        assert response_data["data"]["phone"] == update_data["phone"], "电话未更新"
        
        print("✓ 用户基本信息更新成功")
    
    def test_update_user_contact_info(self, setup_class):
        """测试场景4: 更新用户联系信息"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 更新联系信息
        update_data = {
            "email": f"test_{int(time.time())}@example.com",
            "phone": "13800138001",
            "address": "北京市朝阳区测试街道123号",
            "postcode": "100000"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/me/contact",
            json=update_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"更新联系信息失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "email" in response_data["data"], "邮箱未返回"
        assert "phone" in response_data["data"], "电话未返回"
        
        print("✓ 用户联系信息更新成功")
    
    def test_update_user_security_settings(self, setup_class):
        """测试场景5: 更新用户安全设置"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 更新安全设置
        update_data = {
            "twoFactorEnabled": False,
            "loginAlarmEnabled": True,
            "loginLocation": "北京"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/me/security",
            json=update_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"更新安全设置失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["data"]["loginAlarmEnabled"] == True, "登录告警设置未更新"
        
        print("✓ 用户安全设置更新成功")
    
    def test_upload_user_avatar(self, setup_class):
        """测试场景6: 上传用户头像"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 创建测试图片文件
        test_image_path = "I:\\AI-Ready\\tests\\test_avatar_temp.png"
        
        # 创建一个简单的PNG图片文件
        with open(test_image_path, "wb") as f:
            # PNG文件头
            f.write(b'\x89PNG\r\n\x1a\n')
            # 简单的PNG数据
            for i in range(100):
                f.write(bytes([i % 256] * 10))
        
        try:
            # 上传头像
            with open(test_image_path, "rb") as f:
                files = {'avatar': ('avatar.png', f, 'image/png')}
                response = requests.post(
                    f"{self.BASE_URL}/users/me/avatar",
                    headers=headers,
                    files=files,
                    timeout=10
                )
            
            assert response.status_code == 200, f"上传头像失败，状态码: {response.status_code}"
            
            response_data = response.json()
            assert "avatarUrl" in response_data["data"], "响应缺少头像URL"
            
            print(f"✓ 用户头像上传成功，URL: {response_data['data']['avatarUrl']}")
            
        finally:
            # 清理测试文件
            if os.path.exists(test_image_path):
                os.remove(test_image_path)
    
    def test_change_user_password(self, setup_class):
        """测试场景7: 修改用户密码"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 修改密码
        password_data = {
            "oldPassword": os.getenv('ADMIN_PASSWORD', 'Admin123!'),
            "newPassword": f"NewTest123_{int(time.time())}!",
            "confirmPassword": f"NewTest123_{int(time.time())}!"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/me/password",
            json=password_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"修改密码失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200, "密码修改失败"
        
        print("✓ 用户密码修改成功")
        
        # 恢复原始密码（可选，如果需要）
        restore_data = {
            "oldPassword": f"NewTest123_{int(time.time())}!",
            "newPassword": os.getenv('ADMIN_PASSWORD', 'Admin123!'),
            "confirmPassword": os.getenv('ADMIN_PASSWORD', 'Admin123!')
        }
        
        # 注释掉恢复密码步骤，避免影响其他测试
        # requests.put(f"{self.BASE_URL}/users/me/password", json=restore_data, headers=headers)
    
    def test_query_user_list(self, setup_class):
        """测试场景8: 查询用户列表"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 查询用户列表
        response = requests.get(
            f"{self.BASE_URL}/users?page=1&size=10",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"查询用户列表失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "items" in response_data["data"], "响应缺少items字段"
        assert "total" in response_data["data"], "响应缺少total字段"
        
        print(f"✓ 用户列表查询成功，共 {response_data['data']['total']} 个用户")
    
    def test_search_users_by_name(self, setup_class):
        """测试场景9: 按姓名搜索用户"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 创建测试用户
        user_data = {
            "username": f"search_user_{int(time.time())}",
            "password": "Test123!",
            "email": f"search_user_{int(time.time())}@example.com",
            "realName": f"搜索测试用户_{int(time.time())}"
        }
        
        create_headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        create_response = requests.post(
            f"{self.BASE_URL}/users",
            json=user_data,
            headers=create_headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建测试用户失败"
        
        # 搜索用户
        search_name = user_data["realName"]
        response = requests.get(
            f"{self.BASE_URL}/users/search?name={search_name}",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"搜索用户失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert len(response_data["data"]["items"]) > 0, "搜索结果为空"
        
        print(f"✓ 用户搜索成功，找到 {len(response_data['data']['items'])} 个匹配用户")
    
    def test_get_user_permissions(self, setup_class):
        """测试场景10: 查询用户权限"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 获取当前用户权限
        response = requests.get(
            f"{self.BASE_URL}/users/me/permissions",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"获取用户权限失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "permissions" in response_data["data"], "响应缺少permissions字段"
        
        print(f"✓ 用户权限查询成功，权限数量: {len(response_data['data']['permissions'])}")
    
    def test_get_user_roles(self, setup_class):
        """测试场景11: 查询用户角色"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 获取当前用户角色
        response = requests.get(
            f"{self.BASE_URL}/users/me/roles",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"获取用户角色失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "roles" in response_data["data"], "响应缺少roles字段"
        
        print(f"✓ 用户角色查询成功，角色数量: {len(response_data['data']['roles'])}")
    
    def test_update_user_preference(self, setup_class):
        """测试场景12: 更新用户偏好设置"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 更新偏好设置
        preference_data = {
            "language": "zh-CN",
            "timezone": "Asia/Shanghai",
            "theme": "light",
            "dateFormat": "YYYY-MM-DD",
            "timeFormat": "HH:mm:ss"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/me/preference",
            json=preference_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"更新偏好设置失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["data"]["language"] == "zh-CN", "语言偏好未更新"
        
        print("✓ 用户偏好设置更新成功")
    
    def test_query_user_activity_log(self, setup_class):
        """测试场景13: 查询用户活动日志"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 查询活动日志
        response = requests.get(
            f"{self.BASE_URL}/users/me/logs?page=1&size=10",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"查询活动日志失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "items" in response_data["data"], "响应缺少items字段"
        
        print(f"✓ 用户活动日志查询成功，记录数量: {len(response_data['data']['items'])}")
    
    def test_get_user_statistics(self, setup_class):
        """测试场景14: 查询用户统计数据"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 获取用户统计数据
        response = requests.get(
            f"{self.BASE_URL}/users/me/statistics",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"获取用户统计数据失败，状态码: {response.status_code}"
        
        response_data = response.json()
        # 不.assertEquals前 requirements check
        print(f"✓ 用户统计数据查询成功")
    
    def test_export_user_data(self, setup_class):
        """测试场景15: 导出用户数据"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 导出用户数据
        response = requests.get(
            f"{self.BASE_URL}/users/me/export",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"导出用户数据失败，状态码: {response.status_code}"
        
        # 验证返回的是CSV或JSON格式
        content_type = response.headers.get('Content-Type', '')
        assert 'text/csv' in content_type or 'application/json' in content_type, "响应格式不正确"
        
        print(f"✓ 用户数据导出成功，Content-Type: {content_type}")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
