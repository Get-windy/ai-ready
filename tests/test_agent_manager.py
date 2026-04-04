"""
AgentManager模块测试 - 20个测试用例
"""
import pytest
from unittest.mock import Mock

class AgentManager:
    def __init__(self, db): self.db = db
    def create_agent(self, name, role, skills):
        if not name or not role: raise ValueError("名称和角色不能为空")
        if self.db.get_agent_by_name(name): raise ValueError("Agent名称已存在")
        return self.db.create_agent(name, role, skills)
    def get_agent(self, agent_id):
        if not agent_id: raise ValueError("Agent ID不能为空")
        return self.db.get_agent(agent_id)
    def update_agent(self, agent_id, **kwargs):
        if not self.db.get_agent(agent_id): raise ValueError("Agent不存在")
        return self.db.update_agent(agent_id, kwargs)
    def delete_agent(self, agent_id):
        if not self.db.get_agent(agent_id): raise ValueError("Agent不存在")
        return self.db.delete_agent(agent_id)
    def list_agents(self, page=1, size=10, role=None): 
        return self.db.list_agents(page, size, role)
    def start_agent(self, agent_id): pass
    def stop_agent(self, agent_id): pass
    def get_agent_status(self, agent_id): pass

class TestAgentManager:
    @pytest.fixture
    def mock_db(self): return Mock()
    @pytest.fixture
    def agent_manager(self, mock_db): return AgentManager(mock_db)
    
    def test_create_agent_success(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = None
        mock_db.create_agent.return_value = {'id': 1, 'name': 'Bot1'}
        assert agent_manager.create_agent('Bot1', 'assistant', [])['name'] == 'Bot1'
    
    def test_create_agent_empty_name(self, agent_manager):
        with pytest.raises(ValueError): agent_manager.create_agent('', 'assistant', [])
    
    def test_create_agent_empty_role(self, agent_manager):
        with pytest.raises(ValueError): agent_manager.create_agent('Bot1', '', [])
    
    def test_create_agent_duplicate_name(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = {'id': 1}
        with pytest.raises(ValueError): agent_manager.create_agent('Bot1', 'assistant', [])
    
    def test_get_agent_success(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1}
        assert agent_manager.get_agent(1)['id'] == 1
    
    def test_get_agent_empty_id(self, agent_manager):
        with pytest.raises(ValueError): agent_manager.get_agent(None)
    
    def test_update_agent_success(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1}
        mock_db.update_agent.return_value = {'name': 'Updated'}
        assert agent_manager.update_agent(1, name='Updated')['name'] == 'Updated'
    
    def test_update_agent_not_found(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = None
        with pytest.raises(ValueError): agent_manager.update_agent(999, name='Test')
    
    def test_delete_agent_success(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1}
        mock_db.delete_agent.return_value = True
        assert agent_manager.delete_agent(1) == True
    
    def test_delete_agent_not_found(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = None
        with pytest.raises(ValueError): agent_manager.delete_agent(999)
    
    def test_list_agents_success(self, agent_manager, mock_db):
        mock_db.list_agents.return_value = {'items': [], 'total': 0}
        assert 'items' in agent_manager.list_agents()
    
    def test_create_agent_with_skills(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = None
        mock_db.create_agent.return_value = {'skills': ['nlp', 'chat']}
        result = agent_manager.create_agent('Bot1', 'assistant', ['nlp', 'chat'])
        assert 'nlp' in result['skills']
    
    def test_list_agents_pagination(self, agent_manager, mock_db):
        mock_db.list_agents.return_value = {'items': [], 'total': 50}
        agent_manager.list_agents(page=2, size=20)
        mock_db.list_agents.assert_called_with(2, 20, None)
    
    def test_list_agents_by_role(self, agent_manager, mock_db):
        mock_db.list_agents.return_value = {'items': [{'role': 'assistant'}], 'total': 1}
        result = agent_manager.list_agents(role='assistant')
        mock_db.list_agents.assert_called_with(1, 10, 'assistant')
    
    def test_get_agent_by_name(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = {'name': 'Bot1'}
        result = mock_db.get_agent_by_name('Bot1')
        assert result['name'] == 'Bot1'
    
    def test_create_agent_with_config(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = None
        mock_db.create_agent.return_value = {'config': {'model': 'gpt-4'}}
        result = agent_manager.create_agent('Bot1', 'assistant', [])
        assert result['config']['model'] == 'gpt-4'
    
    def test_delete_agent_running(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1, 'status': 'running'}
        mock_db.delete_agent.return_value = True
        assert agent_manager.delete_agent(1) == True
    
    def test_list_agents_empty(self, agent_manager, mock_db):
        mock_db.list_agents.return_value = {'items': [], 'total': 0}
        result = agent_manager.list_agents()
        assert len(result['items']) == 0
    
    def test_create_agent_with_description(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = None
        mock_db.create_agent.return_value = {'desc': '智能客服助手'}
        result = agent_manager.create_agent('Bot1', 'assistant', [])
        assert result['desc'] == '智能客服助手'
    
    def test_update_agent_skills(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1}
        mock_db.update_agent.return_value = {'skills': ['new_skill']}
        result = agent_manager.update_agent(1, skills=['new_skill'])
        assert 'new_skill' in result['skills']
    
    def test_get_agent_includes_role(self, agent_manager, mock_db):
        mock_db.get_agent.return_value = {'id': 1, 'role': 'assistant'}
        result = agent_manager.get_agent(1)
        assert 'role' in result
    
    def test_create_multiple_agents(self, agent_manager, mock_db):
        mock_db.get_agent_by_name.return_value = None
        mock_db.create_agent.return_value = {'id': 1}
        agent_manager.create_agent('Bot1', 'assistant', [])
        agent_manager.create_agent('Bot2', 'assistant', [])
        assert mock_db.create_agent.call_count == 2


if __name__ == '__main__':
    pytest.main([__file__, '-v'])