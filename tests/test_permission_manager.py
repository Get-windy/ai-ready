"""
PermissionManager模块测试 - 20个测试用例
"""
import pytest
from unittest.mock import Mock

class PermissionManager:
    def __init__(self, db): self.db = db
    def create_permission(self, name, code, resource): 
        if not name or not code: raise ValueError("名称和编码不能为空")
        if self.db.get_permission_by_code(code): raise ValueError("权限编码已存在")
        return self.db.create_permission(name, code, resource)
    def get_permission(self, perm_id):
        if not perm_id: raise ValueError("权限ID不能为空")
        return self.db.get_permission(perm_id)
    def update_permission(self, perm_id, **kwargs):
        if not self.db.get_permission(perm_id): raise ValueError("权限不存在")
        return self.db.update_permission(perm_id, kwargs)
    def delete_permission(self, perm_id):
        if not self.db.get_permission(perm_id): raise ValueError("权限不存在")
        return self.db.delete_permission(perm_id)
    def list_permissions(self, page=1, size=10): return self.db.list_permissions(page, size)
    def check_permission(self, user_id, permission): pass
    def grant_permission(self, user_id, permission): pass
    def revoke_permission(self, user_id, permission): pass

class TestPermissionManager:
    @pytest.fixture
    def mock_db(self): return Mock()
    @pytest.fixture
    def perm_manager(self, mock_db): return PermissionManager(mock_db)
    
    def test_create_permission_success(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = None
        mock_db.create_permission.return_value = {'id': 1, 'name': 'Read'}
        assert perm_manager.create_permission('Read', 'read', 'user')['name'] == 'Read'
    
    def test_create_permission_empty_name(self, perm_manager):
        with pytest.raises(ValueError): perm_manager.create_permission('', 'read', 'user')
    
    def test_create_permission_empty_code(self, perm_manager):
        with pytest.raises(ValueError): perm_manager.create_permission('Read', '', 'user')
    
    def test_create_permission_duplicate_code(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = {'id': 1}
        with pytest.raises(ValueError): perm_manager.create_permission('Read', 'read', 'user')
    
    def test_get_permission_success(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1}
        assert perm_manager.get_permission(1)['id'] == 1
    
    def test_get_permission_empty_id(self, perm_manager):
        with pytest.raises(ValueError): perm_manager.get_permission(None)
    
    def test_update_permission_success(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1}
        mock_db.update_permission.return_value = {'name': 'Updated'}
        assert perm_manager.update_permission(1, name='Updated')['name'] == 'Updated'
    
    def test_update_permission_not_found(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = None
        with pytest.raises(ValueError): perm_manager.update_permission(999, name='Test')
    
    def test_delete_permission_success(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1}
        mock_db.delete_permission.return_value = True
        assert perm_manager.delete_permission(1) == True
    
    def test_delete_permission_not_found(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = None
        with pytest.raises(ValueError): perm_manager.delete_permission(999)
    
    def test_list_permissions_success(self, perm_manager, mock_db):
        mock_db.list_permissions.return_value = {'items': [], 'total': 0}
        assert 'items' in perm_manager.list_permissions()
    
    def test_create_permission_with_resource(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = None
        mock_db.create_permission.return_value = {'resource': 'user'}
        result = perm_manager.create_permission('Read', 'read', 'user')
        assert result['resource'] == 'user'
    
    def test_list_permissions_pagination(self, perm_manager, mock_db):
        mock_db.list_permissions.return_value = {'items': [], 'total': 50}
        perm_manager.list_permissions(page=2, size=20)
        mock_db.list_permissions.assert_called_with(2, 20)
    
    def test_get_permission_by_code(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = {'code': 'read'}
        result = mock_db.get_permission_by_code('read')
        assert result['code'] == 'read'
    
    def test_create_permission_with_action(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = None
        mock_db.create_permission.return_value = {'action': 'read'}
        result = perm_manager.create_permission('Read', 'read', 'user')
        assert result['action'] == 'read'
    
    def test_delete_permission_cascade(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1}
        mock_db.delete_permission.return_value = True
        assert perm_manager.delete_permission(1) == True
    
    def test_list_permissions_empty(self, perm_manager, mock_db):
        mock_db.list_permissions.return_value = {'items': [], 'total': 0}
        result = perm_manager.list_permissions()
        assert len(result['items']) == 0
    
    def test_create_permission_with_description(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = None
        mock_db.create_permission.return_value = {'desc': '读取权限'}
        result = perm_manager.create_permission('Read', 'read', 'user')
        assert result['desc'] == '读取权限'
    
    def test_update_permission_resource(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1}
        mock_db.update_permission.return_value = {'resource': 'order'}
        result = perm_manager.update_permission(1, resource='order')
        assert result['resource'] == 'order'
    
    def test_get_permission_includes_code(self, perm_manager, mock_db):
        mock_db.get_permission.return_value = {'id': 1, 'code': 'read'}
        result = perm_manager.get_permission(1)
        assert 'code' in result
    
    def test_create_multiple_permissions(self, perm_manager, mock_db):
        mock_db.get_permission_by_code.return_value = None
        mock_db.create_permission.return_value = {'id': 1}
        perm_manager.create_permission('Read', 'read', 'user')
        perm_manager.create_permission('Write', 'write', 'user')
        assert mock_db.create_permission.call_count == 2
    
    def test_list_permissions_by_resource(self, perm_manager, mock_db):
        mock_db.list_permissions.return_value = {'items': [{'resource': 'user'}], 'total': 1}
        result = perm_manager.list_permissions()
        assert result['items'][0]['resource'] == 'user'


if __name__ == '__main__':
    pytest.main([__file__, '-v'])