"""
AI-Ready 核心API模块单元测试
UserManager模块测试 - 20个测试用例
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime

# 假设的UserManager类
class UserManager:
    def __init__(self, db_connection):
        self.db = db_connection
    
    def create_user(self, username, email, password, role='user'):
        """创建用户"""
        if not username or not email or not password:
            raise ValueError("用户名、邮箱和密码不能为空")
        if self.db.get_user_by_username(username):
            raise ValueError("用户名已存在")
        if self.db.get_user_by_email(email):
            raise ValueError("邮箱已存在")
        return self.db.create_user(username, email, password, role)
    
    def get_user(self, user_id):
        """获取用户"""
        if not user_id:
            raise ValueError("用户ID不能为空")
        return self.db.get_user(user_id)
    
    def update_user(self, user_id, **kwargs):
        """更新用户"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        return self.db.update_user(user_id, kwargs)
    
    def delete_user(self, user_id):
        """删除用户"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        return self.db.delete_user(user_id)
    
    def list_users(self, page=1, size=10, role=None):
        """列出用户"""
        return self.db.list_users(page, size, role)
    
    def change_password(self, user_id, old_password, new_password):
        """修改密码"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        if user['password'] != old_password:
            raise ValueError("原密码错误")
        return self.db.update_user(user_id, {'password': new_password})
    
    def assign_role(self, user_id, role):
        """分配角色"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        return self.db.update_user(user_id, {'role': role})
    
    def disable_user(self, user_id):
        """禁用用户"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        return self.db.update_user(user_id, {'status': 'disabled'})
    
    def enable_user(self, user_id):
        """启用用户"""
        user = self.db.get_user(user_id)
        if not user:
            raise ValueError("用户不存在")
        return self.db.update_user(user_id, {'status': 'enabled'})


# ========== 测试类 ==========
class TestUserManager:
    """UserManager单元测试"""
    
    @pytest.fixture
    def mock_db(self):
        """Mock数据库连接"""
        return Mock()
    
    @pytest.fixture
    def user_manager(self, mock_db):
        """创建UserManager实例"""
        return UserManager(mock_db)
    
    # 测试用例1: 成功创建用户
    def test_create_user_success(self, user_manager, mock_db):
        mock_db.get_user_by_username.return_value = None
        mock_db.get_user_by_email.return_value = None
        mock_db.create_user.return_value = {'id': 1, 'username': 'test', 'email': 'test@example.com'}
        
        result = user_manager.create_user('test', 'test@example.com', 'password123')
        assert result['username'] == 'test'
        mock_db.create_user.assert_called_once()
    
    # 测试用例2: 用户名为空抛出异常
    def test_create_user_empty_username(self, user_manager):
        with pytest.raises(ValueError, match="用户名、邮箱和密码不能为空"):
            user_manager.create_user('', 'test@example.com', 'password123')
    
    # 测试用例3: 邮箱为空抛出异常
    def test_create_user_empty_email(self, user_manager):
        with pytest.raises(ValueError, match="用户名、邮箱和密码不能为空"):
            user_manager.create_user('test', '', 'password123')
    
    # 测试用例4: 密码为空抛出异常
    def test_create_user_empty_password(self, user_manager):
        with pytest.raises(ValueError, match="用户名、邮箱和密码不能为空"):
            user_manager.create_user('test', 'test@example.com', '')
    
    # 测试用例5: 用户名已存在抛出异常
    def test_create_user_duplicate_username(self, user_manager, mock_db):
        mock_db.get_user_by_username.return_value = {'id': 1, 'username': 'test'}
        
        with pytest.raises(ValueError, match="用户名已存在"):
            user_manager.create_user('test', 'test@example.com', 'password123')
    
    # 测试用例6: 邮箱已存在抛出异常
    def test_create_user_duplicate_email(self, user_manager, mock_db):
        mock_db.get_user_by_username.return_value = None
        mock_db.get_user_by_email.return_value = {'id': 1, 'email': 'test@example.com'}
        
        with pytest.raises(ValueError, match="邮箱已存在"):
            user_manager.create_user('test', 'test@example.com', 'password123')
    
    # 测试用例7: 成功获取用户
    def test_get_user_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'username': 'test'}
        
        result = user_manager.get_user(1)
        assert result['id'] == 1
    
    # 测试用例8: 用户ID为空抛出异常
    def test_get_user_empty_id(self, user_manager):
        with pytest.raises(ValueError, match="用户ID不能为空"):
            user_manager.get_user(None)
    
    # 测试用例9: 成功更新用户
    def test_update_user_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'username': 'test'}
        mock_db.update_user.return_value = {'id': 1, 'username': 'test_updated'}
        
        result = user_manager.update_user(1, username='test_updated')
        assert result['username'] == 'test_updated'
    
    # 测试用例10: 更新不存在的用户抛出异常
    def test_update_user_not_found(self, user_manager, mock_db):
        mock_db.get_user.return_value = None
        
        with pytest.raises(ValueError, match="用户不存在"):
            user_manager.update_user(999, username='test')
    
    # 测试用例11: 成功删除用户
    def test_delete_user_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'username': 'test'}
        mock_db.delete_user.return_value = True
        
        result = user_manager.delete_user(1)
        assert result == True
    
    # 测试用例12: 删除不存在的用户抛出异常
    def test_delete_user_not_found(self, user_manager, mock_db):
        mock_db.get_user.return_value = None
        
        with pytest.raises(ValueError, match="用户不存在"):
            user_manager.delete_user(999)
    
    # 测试用例13: 成功列出用户
    def test_list_users_success(self, user_manager, mock_db):
        mock_db.list_users.return_value = {'items': [], 'total': 0}
        
        result = user_manager.list_users(page=1, size=10)
        assert 'items' in result
    
    # 测试用例14: 成功修改密码
    def test_change_password_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'password': 'old_password'}
        mock_db.update_user.return_value = {'id': 1, 'password': 'new_password'}
        
        result = user_manager.change_password(1, 'old_password', 'new_password')
        assert result['password'] == 'new_password'
    
    # 测试用例15: 原密码错误抛出异常
    def test_change_password_wrong_old(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'password': 'correct_password'}
        
        with pytest.raises(ValueError, match="原密码错误"):
            user_manager.change_password(1, 'wrong_password', 'new_password')
    
    # 测试用例16: 成功分配角色
    def test_assign_role_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'role': 'user'}
        mock_db.update_user.return_value = {'id': 1, 'role': 'admin'}
        
        result = user_manager.assign_role(1, 'admin')
        assert result['role'] == 'admin'
    
    # 测试用例17: 成功禁用用户
    def test_disable_user_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'status': 'enabled'}
        mock_db.update_user.return_value = {'id': 1, 'status': 'disabled'}
        
        result = user_manager.disable_user(1)
        assert result['status'] == 'disabled'
    
    # 测试用例18: 成功启用用户
    def test_enable_user_success(self, user_manager, mock_db):
        mock_db.get_user.return_value = {'id': 1, 'status': 'disabled'}
        mock_db.update_user.return_value = {'id': 1, 'status': 'enabled'}
        
        result = user_manager.enable_user(1)
        assert result['status'] == 'enabled'
    
    # 测试用例19: 创建用户时指定角色
    def test_create_user_with_role(self, user_manager, mock_db):
        mock_db.get_user_by_username.return_value = None
        mock_db.get_user_by_email.return_value = None
        mock_db.create_user.return_value = {'id': 1, 'username': 'admin', 'role': 'admin'}
        
        result = user_manager.create_user('admin', 'admin@example.com', 'password', 'admin')
        assert result['role'] == 'admin'
    
    # 测试用例20: 列出用户时过滤角色
    def test_list_users_with_role_filter(self, user_manager, mock_db):
        mock_db.list_users.return_value = {'items': [{'role': 'admin'}], 'total': 1}
        
        result = user_manager.list_users(role='admin')
        mock_db.list_users.assert_called_with(1, 10, 'admin')


if __name__ == '__main__':
    pytest.main([__file__, '-v'])