"""
用户信息管理功能自动化测试脚本
pytest测试框架实现
覆盖信息修改、密码修改、头像上传等核心场景
"""

import pytest
import requests
from typing import Dict, Any
import os


class TestUserInfoManagement:
    """用户信息管理功能测试类"""
    
    # 测试基础URL配置
    BASE_URL = "http://localhost:8080/api/v1"
    TASK_TOKEN = None  # 通过登录获取
    
    @pytest.fixture
    def setup(self):
        """测试环境准备 - 获取认证Token"""
        # 登录获取Token
        login_data = {
            "username": "testuser",
            "password": "Test123!"
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=login_data
        )
        
        assert response.status_code == 200, "登录失败"
        self.TASK_TOKEN = response.json()["token"]
        
        yield
        
        # 清理逻辑
        self.TASK_TOKEN = None
    
    def test_update_user_info(self, setup):
        """测试场景1: 更新用户信息"""
        headers = {"Authorization": f"Bearer {self.TASK_TOKEN}"}
        
        # 更新用户信息
        info_data = {
            "realName": "测试用户",
            "email": "test@example.com",
            "phone": "13800138000"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/info",
            json=info_data,
            headers=headers
        )
        
        # 验证响应
        assert response.status_code == 200, f"更新失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200, "更新失败"
        
        print("✓ 用户信息更新成功")
    
    def test_change_password(self, setup):
        """测试场景2: 修改密码"""
        headers = {"Authorization": f"Bearer {self.TASK_TOKEN}"}
        
        # 修改密码请求
        password_data = {
            "oldPassword": "Test123!",
            "newPassword": "NewTest123!",
            "confirmPassword": "NewTest123!"
        }
        
        response = requests.put(
            f"{self.BASE_URL}/users/change-password",
            json=password_data,
            headers=headers
        )
        
        # 验证响应
        assert response.status_code == 200, f"修改密码失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200, "密码修改失败"
        
        print("✓ 密码修改成功")
    
    def test_upload_avatar(self, setup):
        """测试场景3: 上传头像"""
        headers = {"Authorization": f"Bearer {self.TASK_TOKEN}"}
        
        # 创建测试图片文件
        test_image_path = "I:\\AI-Ready\\tests\\test_avatar.png"
        
        # 创建一个简单的PNG图片文件
        with open(test_image_path, "wb") as f:
            # 简单的PNG文件头
            f.write(b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\xcf\xc0\x00\x00\x00\x03\x00\x01\x00\x05\xfe\xe9\xd1\x00\x00\x00\x00IEND\xaeB`\x82')
        
        files = {"avatar": ("test_avatar.png", open(test_image_path, "rb"), "image/png")}
        
        response = requests.post(
            f"{self.BASE_URL}/users/avatar",
            headers=headers,
            files=files
        )
        
        # 验证响应
        assert response.status_code == 200, f"上传头像失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200, "头像上传失败"
        
        # 清理测试文件
        if os.path.exists(test_image_path):
            os.remove(test_image_path)
        
        print("✓ 头像上传成功")
    
    def test_get_user_info(self, setup):
        """测试场景4: 获取用户信息"""
        headers = {"Authorization": f"Bearer {self.TASK_TOKEN}"}
        
        response = requests.get(
            f"{self.BASE_URL}/users/info",
            headers=headers
        )
        
        # 验证响应
        assert response.status_code == 200, f"获取用户信息失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200, "获取用户信息失败"
        assert "realName" in response_data["data"], "响应中缺少用户信息"
        
        print("✓ 获取用户信息成功")
    
    def test_invalid_token(self, setup):
        """测试场景5: 无效Token"""
        headers = {"Authorization": "Bearer invalid_token"}
        
        response = requests.get(
            f"{self.BASE_URL}/users/info",
            headers=headers
        )
        
        # 验证认证失败
        assert response.status_code == 401, f"期望401，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 401, "认证失败码不匹配"
        
        print("✓ 无效Token验证通过")
    
    def test_missing_token(self, setup):
        """测试场景6: 缺少Token"""
        response = requests.get(
            f"{self.BASE_URL}/users/info"
        )
        
        # 验证认证失败
        assert response.status_code == 401, f"期望401，实际状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 401, "认证失败码不匹配"
        
        print("✓ 缺少Token验证通过")


def run_user_info_tests():
    """运行用户信息管理测试并生成报告"""
    print("=" * 60)
    print("用户信息管理功能测试执行")
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
    exit(run_user_info_tests())
