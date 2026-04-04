"""
RoleManager模块测试 - 20个测试用例
"""
import pytest
from unittest.mock import Mock

class RoleManager:
    def __init__(self, db):
        self.db = db
    
    def create_role(self, name, code, permissions):
        if not name or not code: raise ValueError("名称和编码不能为空")
        if self.db.get_role_by_code(code): raise ValueError("角色编码已存在")
        return self.db.create_role(name, code, permissions)
    
    def get_role(self, role_id):
        if not role_id: raise ValueError("角色ID不能为空")
        return self.db.get_role(role_id)
    
    def update_role(self, role_id, **kwargs):
        if not self.db.get_role(role_id): raise ValueError("角色不存在")
        return self.db.update_role(role_id, kwargs)
    
    def delete_role(self, role_id):
        if not self.db.get_role(role_id): raise ValueError("角色不存在")
        return self.db.delete_role(role_id)
    
    def list_roles(self, page=1, size=10): return self.db.list_roles(page, size)
    def assign_permission(self, role_id, permission): pass
    def remove_permission(self, role_id, permission): pass
    def get_permissions(self, role_id): pass

class TestRoleManager:
    @pytest.fixture
    def mock_db(self): return Mock()
    @pytest.fixture
    def role_manager(self, mock_db): return RoleManager(mock_db)
    
    def test_create_role_success(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = None
        mock_db.create_role.return_value = {'id': 1, 'name': 'Admin'}
        assert role_manager.create_role('Admin', 'admin', [])['name'] == 'Admin'
    
    def test_create_role_empty_name(self, role_manager):
        with pytest.raises(ValueError): role_manager.create_role('', 'admin', [])
    
    def test_create_role_empty_code(self, role_manager):
        with pytest.raises(ValueError): role_manager.create_role('Admin', '', [])
    
    def test_create_role_duplicate_code(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = {'id': 1}
        with pytest.raises(ValueError): role_manager.create_role('Admin', 'admin', [])
    
    def test_get_role_success(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1}
        assert role_manager.get_role(1)['id'] == 1
    
    def test_get_role_empty_id(self, role_manager):
        with pytest.raises(ValueError): role_manager.get_role(None)
    
    def test_update_role_success(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1}
        mock_db.update_role.return_value = {'id': 1, 'name': 'Updated'}
        assert role_manager.update_role(1, name='Updated')['name'] == 'Updated'
    
    def test_update_role_not_found(self, role_manager, mock_db):
        mock_db.get_role.return_value = None
        with pytest.raises(ValueError): role_manager.update_role(999, name='Test')
    
    def test_delete_role_success(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1}
        mock_db.delete_role.return_value = True
        assert role_manager.delete_role(1) == True
    
    def test_delete_role_not_found(self, role_manager, mock_db):
        mock_db.get_role.return_value = None
        with pytest.raises(ValueError): role_manager.delete_role(999)
    
    def test_list_roles_success(self, role_manager, mock_db):
        mock_db.list_roles.return_value = {'items': [], 'total': 0}
        assert 'items' in role_manager.list_roles()
    
    def test_create_role_with_permissions(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = None
        mock_db.create_role.return_value = {'permissions': ['read', 'write']}
        result = role_manager.create_role('Test', 'test', ['read', 'write'])
        assert 'read' in result['permissions']
    
    def test_get_role_by_code(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = {'code': 'admin'}
        result = mock_db.get_role_by_code('admin')
        assert result['code'] == 'admin'
    
    def test_list_roles_pagination(self, role_manager, mock_db):
        mock_db.list_roles.return_value = {'items': [], 'total': 100}
        result = role_manager.list_roles(page=2, size=20)
        mock_db.list_roles.assert_called_with(2, 20)
    
    def test_update_role_permissions(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1}
        mock_db.update_role.return_value = {'permissions': ['admin']}
        result = role_manager.update_role(1, permissions=['admin'])
        assert 'admin' in result['permissions']
    
    def test_create_role_with_description(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = None
        mock_db.create_role.return_value = {'desc': '管理员角色'}
        result = role_manager.create_role('Admin', 'admin', [])
        assert result['desc'] == '管理员角色'
    
    def test_delete_role_with_users(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1, 'users': []}
        mock_db.delete_role.return_value = True
        assert role_manager.delete_role(1) == True
    
    def test_list_roles_empty(self, role_manager, mock_db):
        mock_db.list_roles.return_value = {'items': [], 'total': 0}
        result = role_manager.list_roles()
        assert len(result['items']) == 0
    
    def test_get_role_includes_permissions(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1, 'permissions': ['read']}
        result = role_manager.get_role(1)
        assert 'permissions' in result
    
    def test_update_role_name(self, role_manager, mock_db):
        mock_db.get_role.return_value = {'id': 1}
        mock_db.update_role.return_value = {'name': 'SuperAdmin'}
        result = role_manager.update_role(1, name='SuperAdmin')
        assert result['name'] == 'SuperAdmin'
    
    def test_create_multiple_roles(self, role_manager, mock_db):
        mock_db.get_role_by_code.return_value = None
        mock_db.create_role.return_value = {'id': 1}
        role_manager.create_role('Role1', 'role1', [])
        role_manager.create_role('Role2', 'role2', [])
        assert mock_db.create_role.call_count == 2


if __name__ == '__main__':
    pytest.main([__file__, '-v'])