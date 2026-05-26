#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 业务逻辑绕过安全测试用例
测试业务流程跳跃、权限绕过、状态操控等安全漏洞
"""

import pytest
import sys
import os
import time

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class BusinessWorkflow:
    """业务流程模拟器"""
    
    def __init__(self):
        self.workflows = {}
        self.approvals = {}
        self.permissions = {
            "user": ["create", "view"],
            "manager": ["create", "view", "approve", "reject"],
            "admin": ["create", "view", "approve", "reject", "delete", "bypass"]
        }
    
    def create_workflow(self, user_id, user_role, workflow_type):
        if "create" not in self.permissions.get(user_role, []):
            return None, "Permission denied"
        workflow_id = f"wf_{int(time.time())}_{user_id}"
        self.workflows[workflow_id] = {
            "creator": user_id,
            "role": user_role,
            "type": workflow_type,
            "current_step": 0,
            "status": "active",
            "created_at": time.time()
        }
        return workflow_id, "Workflow created"
    
    def advance_step(self, workflow_id, user_id, user_role, action):
        if workflow_id not in self.workflows:
            return False, "Workflow not found"
        wf = self.workflows[workflow_id]
        if action in ["approve", "reject"] and "approve" not in self.permissions.get(user_role, []):
            return False, "Permission denied for approval"
        current = wf["current_step"]
        expected = {0: ["submit"], 1: ["review"], 2: ["approve", "reject"], 3: ["complete"]}
        if current in expected and action not in expected[current]:
            return False, f"Invalid action for step {current}"
        if action == "submit":
            wf["current_step"] = 1
        elif action == "review":
            wf["current_step"] = 2
        elif action == "approve":
            wf["current_step"] = 3
            self.approvals[workflow_id] = {"approver": user_id, "approved_at": time.time()}
        elif action == "reject":
            wf["current_step"] = 0
            wf["status"] = "rejected"
        elif action == "complete":
            wf["current_step"] = 4
            wf["status"] = "completed"
        return True, "Step advanced"
    
    def delete_workflow(self, workflow_id, user_id, user_role):
        if workflow_id not in self.workflows:
            return False, "Workflow not found"
        if "delete" not in self.permissions.get(user_role, []):
            return False, "Permission denied - admin only"
        del self.workflows[workflow_id]
        return True, "Workflow deleted"
    
    def bypass_approval(self, workflow_id, user_id, user_role):
        if workflow_id not in self.workflows:
            return False, "Workflow not found"
        if "bypass" not in self.permissions.get(user_role, []):
            return False, "Permission denied - bypass requires admin"
        wf = self.workflows[workflow_id]
        wf["current_step"] = 3
        wf["status"] = "approved"
        self.approvals[workflow_id] = {"approver": user_id, "bypassed": True}
        return True, "Workflow bypassed"


class TestBusinessLogicBypass:
    """业务逻辑绕过安全测试"""
    
    @pytest.fixture
    def workflow(self):
        return BusinessWorkflow()
    
    def test_step_sequence_enforcement(self, workflow):
        """测试步骤顺序强制执行"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        assert wf_id is not None
        result, _ = workflow.advance_step(wf_id, "user1", "user", "submit")
        assert result == True
        result, msg = workflow.advance_step(wf_id, "manager1", "manager", "approve")
        assert result == False
        assert "Invalid action" in msg
    
    def test_user_cannot_approve(self, workflow):
        """测试普通用户无法审批"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        workflow.advance_step(wf_id, "user1", "user", "submit")
        workflow.advance_step(wf_id, "manager1", "manager", "review")
        result, msg = workflow.advance_step(wf_id, "user2", "user", "approve")
        assert result == False
        assert "Permission denied" in msg
    
    def test_unauthorized_delete_blocked(self, workflow):
        """测试未授权删除被阻止"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        result, msg = workflow.delete_workflow(wf_id, "user1", "user")
        assert result == False
        assert "admin only" in msg
    
    def test_manager_cannot_bypass(self, workflow):
        """测试经理无法绕过审批"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        result, msg = workflow.bypass_approval(wf_id, "manager1", "manager")
        assert result == False
        assert "bypass requires admin" in msg
    
    def test_admin_can_bypass(self, workflow):
        """测试管理员可以绕过审批"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        result, _ = workflow.bypass_approval(wf_id, "admin1", "admin")
        assert result == True
        wf = workflow.workflows[wf_id]
        assert wf["current_step"] == 3
        assert wf["status"] == "approved"
    
    def test_direct_complete_without_approval(self, workflow):
        """测试未经审批直接完成被阻止"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        workflow.advance_step(wf_id, "user1", "user", "submit")
        workflow.advance_step(wf_id, "manager1", "manager", "review")
        result, _ = workflow.advance_step(wf_id, "manager1", "manager", "complete")
        assert result == False
    
    def test_backward_step_manipulation(self, workflow):
        """测试逆向步骤操控被阻止"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        workflow.advance_step(wf_id, "user1", "user", "submit")
        workflow.advance_step(wf_id, "manager1", "manager", "review")
        workflow.advance_step(wf_id, "manager1", "manager", "approve")
        result, _ = workflow.advance_step(wf_id, "user1", "user", "submit")
        assert result == False
    
    def test_invalid_workflow_id(self, workflow):
        """测试无效工作流ID"""
        result, msg = workflow.advance_step("invalid_id", "user1", "user", "submit")
        assert result == False
        assert "not found" in msg
    
    def test_admin_delete_workflow(self, workflow):
        """测试管理员删除工作流"""
        wf_id, _ = workflow.create_workflow("user1", "user", "purchase_request")
        result, _ = workflow.delete_workflow(wf_id, "admin1", "admin")
        assert result == True
        assert wf_id not in workflow.workflows


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
