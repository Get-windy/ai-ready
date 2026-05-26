#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 敏感操作审计安全测试用例
测试审计日志记录、完整性、不可篡改性等安全机制
"""

import pytest
import sys
import os
import time
import hashlib
from datetime import datetime
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class AuditLogSystem:
    """审计日志系统模拟器"""
    
    def __init__(self):
        self.audit_logs = []
        self.log_hashes = {}  # 用于验证日志完整性
        self.immutable_flags = set()  # 已标记为不可变的日志索引
    
    def log_operation(self, user_id, operation, resource_type, resource_id, details, reason=None):
        """记录敏感操作"""
        log_entry = {
            "id": len(self.audit_logs),
            "timestamp": time.time(),
            "user_id": user_id,
            "operation": operation,  # create, update, delete, read, export
            "resource_type": resource_type,
            "resource_id": resource_id,
            "details": details,
            "reason": reason,
            "ip_address": "192.168.1.1",  # 模拟IP
            "session_id": f"session_{user_id}",
            "status": "success"
        }
        
        # 计算日志哈希用于完整性验证
        log_hash = self._compute_hash(log_entry)
        log_entry["hash"] = log_hash
        
        self.audit_logs.append(log_entry)
        self.log_hashes[log_entry["id"]] = log_hash
        
        return log_entry["id"]
    
    def _compute_hash(self, log_entry):
        """计算日志哈希"""
        content = f"{log_entry['timestamp']}|{log_entry['user_id']}|{log_entry['operation']}|{log_entry['resource_type']}|{log_entry['resource_id']}|{log_entry['details']}"
        return hashlib.sha256(content.encode()).hexdigest()
    
    def verify_integrity(self, log_id):
        """验证日志完整性"""
        if log_id >= len(self.audit_logs):
            return False, "Log not found"
        
        log_entry = self.audit_logs[log_id]
        expected_hash = self._compute_hash({
            "timestamp": log_entry["timestamp"],
            "user_id": log_entry["user_id"],
            "operation": log_entry["operation"],
            "resource_type": log_entry["resource_type"],
            "resource_id": log_entry["resource_id"],
            "details": log_entry["details"]
        })
        
        if log_entry["hash"] != expected_hash:
            return False, "Log integrity compromised"
        
        return True, "Log integrity verified"
    
    def mark_immutable(self, log_id):
        """标记日志为不可变"""
        if log_id >= len(self.audit_logs):
            return False, "Log not found"
        self.immutable_flags.add(log_id)
        return True, "Log marked as immutable"
    
    def is_immutable(self, log_id):
        """检查日志是否不可变"""
        return log_id in self.immutable_flags
    
    def modify_log(self, log_id, new_details):
        """尝试修改日志 - 应被阻止"""
        if log_id in self.immutable_flags:
            return False, "Log is immutable - modification denied"
        if log_id >= len(self.audit_logs):
            return False, "Log not found"
        
        # 模拟修改（实际系统应完全禁止）
        self.audit_logs[log_id]["details"] = new_details
        self.audit_logs[log_id]["modified_at"] = time.time()
        return True, "Log modified (should not happen in production)"
    
    def delete_log(self, log_id):
        """尝试删除日志 - 应被阻止"""
        if log_id in self.immutable_flags:
            return False, "Log is immutable - deletion denied"
        if log_id >= len(self.audit_logs):
            return False, "Log not found"
        
        # 模拟删除（实际系统应完全禁止）
        self.audit_logs[log_id]["deleted"] = True
        return True, "Log deleted (should not happen in production)"
    
    def query_logs(self, user_id=None, operation=None, resource_type=None, resource_id=None, start_time=None, end_time=None):
        """查询审计日志"""
        results = []
        for log in self.audit_logs:
            if log.get("deleted"):
                continue
            if user_id and log["user_id"] != user_id:
                continue
            if operation and log["operation"] != operation:
                continue
            if resource_type and log["resource_type"] != resource_type:
                continue
            if resource_id and log["resource_id"] != resource_id:
                continue
            if start_time and log["timestamp"] < start_time:
                continue
            if end_time and log["timestamp"] > end_time:
                continue
            results.append(log)
        return results
    
    def get_user_operation_count(self, user_id):
        """获取用户操作统计"""
        return len([l for l in self.audit_logs if l["user_id"] == user_id and not l.get("deleted")])
    
    def get_sensitive_operations(self):
        """获取敏感操作列表"""
        sensitive_ops = ["delete", "export", "update", "bypass"]
        return [l for l in self.audit_logs if l["operation"] in sensitive_ops and not l.get("deleted")]


class TestSensitiveOperationAudit:
    """敏感操作审计安全测试"""
    
    @pytest.fixture
    def audit_system(self):
        """测试 fixture"""
        return AuditLogSystem()
    
    # ==================== 审计日志记录测试 ====================
    
    def test_log_creation_on_sensitive_operation(self, audit_system):
        """测试敏感操作自动记录日志"""
        log_id = audit_system.log_operation(
            user_id="admin1",
            operation="delete",
            resource_type="user",
            resource_id="user123",
            details="Deleted user account",
            reason="Security violation"
        )
        
        assert log_id >= 0, "Log should be created"
        assert len(audit_system.audit_logs) == 1
        
        log = audit_system.audit_logs[log_id]
        assert log["user_id"] == "admin1"
        assert log["operation"] == "delete"
        assert log["resource_type"] == "user"
        assert log["reason"] == "Security violation"
    
    def test_log_contains_required_fields(self, audit_system):
        """测试日志包含必要字段"""
        log_id = audit_system.log_operation(
            user_id="user1",
            operation="update",
            resource_type="document",
            resource_id="doc456",
            details="Updated document content"
        )
        
        log = audit_system.audit_logs[log_id]
        
        # 验证必要字段
        required_fields = ["id", "timestamp", "user_id", "operation", "resource_type", "resource_id", "details", "hash"]
        for field in required_fields:
            assert field in log, f"Log should contain field: {field}"
    
    def test_log_timestamp_accuracy(self, audit_system):
        """测试日志时间戳准确性"""
        before = time.time()
        log_id = audit_system.log_operation(
            user_id="user1",
            operation="create",
            resource_type="file",
            resource_id="file789",
            details="Created new file"
        )
        after = time.time()
        
        log = audit_system.audit_logs[log_id]
        assert log["timestamp"] >= before
        assert log["timestamp"] <= after
    
    def test_multiple_operations_logged(self, audit_system):
        """测试多次操作都被记录"""
        operations = [
            ("create", "document", "doc1"),
            ("update", "document", "doc1"),
            ("delete", "document", "doc1")
        ]
        
        for op, res_type, res_id in operations:
            audit_system.log_operation("user1", op, res_type, res_id, f"{op} {res_type}")
        
        assert len(audit_system.audit_logs) == 3
    
    # ==================== 审计日志完整性测试 ====================
    
    def test_log_hash_integrity(self, audit_system):
        """测试日志哈希完整性"""
        log_id = audit_system.log_operation(
            user_id="admin1",
            operation="delete",
            resource_type="user",
            resource_id="user123",
            details="Deleted user"
        )
        
        result, msg = audit_system.verify_integrity(log_id)
        assert result == True, f"Integrity should be verified: {msg}"
    
    def test_log_hash_unique(self, audit_system):
        """测试日志哈希唯一性"""
        log_id1 = audit_system.log_operation("user1", "create", "doc", "doc1", "Created doc1")
        log_id2 = audit_system.log_operation("user2", "create", "doc", "doc1", "Created doc1")
        
        hash1 = audit_system.audit_logs[log_id1]["hash"]
        hash2 = audit_system.audit_logs[log_id2]["hash"]
        
        # 不同用户的操作应有不同哈希
        assert hash1 != hash2, "Different operations should have different hashes"
    
    def test_log_integrity_after_immutable(self, audit_system):
        """测试不可变日志完整性"""
        log_id = audit_system.log_operation("admin1", "delete", "user", "u1", "Deleted user")
        audit_system.mark_immutable(log_id)
        
        result, msg = audit_system.verify_integrity(log_id)
        assert result == True
    
    # ==================== 审计日志不可篡改性测试 ====================
    
    def test_immutable_log_modification_blocked(self, audit_system):
        """测试不可变日志修改被阻止"""
        log_id = audit_system.log_operation("admin1", "delete", "user", "u1", "Deleted user")
        audit_system.mark_immutable(log_id)
        
        result, msg = audit_system.modify_log(log_id, "Modified details")
        assert result == False, "Immutable log should not be modifiable"
        assert "immutable" in msg.lower()
    
    def test_immutable_log_deletion_blocked(self, audit_system):
        """测试不可变日志删除被阻止"""
        log_id = audit_system.log_operation("admin1", "delete", "user", "u1", "Deleted user")
        audit_system.mark_immutable(log_id)
        
        result, msg = audit_system.delete_log(log_id)
        assert result == False, "Immutable log should not be deletable"
        assert "immutable" in msg.lower()
    
    def test_non_immutable_log_can_be_modified(self, audit_system):
        """测试未标记不可变的日志可被修改（警告：生产环境应禁止）"""
        log_id = audit_system.log_operation("user1", "create", "doc", "d1", "Created doc")
        
        # 未标记不可变
        result, _ = audit_system.modify_log(log_id, "Modified")
        assert result == True  # 模拟中允许，但生产系统应禁止
    
    # ==================== 审计日志查询测试 ====================
    
    def test_query_by_user(self, audit_system):
        """测试按用户查询日志"""
        audit_system.log_operation("user1", "create", "doc", "d1", "Created")
        audit_system.log_operation("user2", "create", "doc", "d2", "Created")
        audit_system.log_operation("user1", "update", "doc", "d1", "Updated")
        
        logs = audit_system.query_logs(user_id="user1")
        assert len(logs) == 2
        assert all(l["user_id"] == "user1" for l in logs)
    
    def test_query_by_operation(self, audit_system):
        """测试按操作类型查询"""
        audit_system.log_operation("user1", "create", "doc", "d1", "Created")
        audit_system.log_operation("user1", "delete", "doc", "d1", "Deleted")
        audit_system.log_operation("user1", "create", "doc", "d2", "Created")
        
        logs = audit_system.query_logs(operation="delete")
        assert len(logs) == 1
        assert logs[0]["operation"] == "delete"
    
    def test_query_by_time_range(self, audit_system):
        """测试按时间范围查询"""
        now = time.time()
        
        audit_system.log_operation("u1", "create", "doc", "d1", "Created")
        audit_system.log_operation("u1", "create", "doc", "d2", "Created")
        
        logs = audit_system.query_logs(start_time=now - 10, end_time=now + 10)
        assert len(logs) >= 2
    
    # ==================== 敏感操作统计测试 ====================
    
    def test_user_operation_count(self, audit_system):
        """测试用户操作计数"""
        audit_system.log_operation("user1", "create", "doc", "d1", "Created")
        audit_system.log_operation("user1", "update", "doc", "d1", "Updated")
        audit_system.log_operation("user2", "create", "doc", "d2", "Created")
        
        count = audit_system.get_user_operation_count("user1")
        assert count == 2
    
    def test_sensitive_operations_identification(self, audit_system):
        """测试敏感操作识别"""
        audit_system.log_operation("u1", "create", "doc", "d1", "Created")
        audit_system.log_operation("admin1", "delete", "user", "u1", "Deleted user")
        audit_system.log_operation("admin1", "bypass", "workflow", "w1", "Bypassed approval")
        audit_system.log_operation("u1", "read", "doc", "d1", "Read doc")
        
        sensitive = audit_system.get_sensitive_operations()
        assert len(sensitive) == 2  # delete + bypass
        assert all(l["operation"] in ["delete", "bypass", "export", "update"] for l in sensitive)
    
    # ==================== OWASP审计要求测试 ====================
    
    def test_owasp_a09_audit_logging(self, audit_system):
        """OWASP A09: Security Logging and Monitoring Failures"""
        # 验证安全相关操作被记录
        log_id = audit_system.log_operation(
            user_id="admin1",
            operation="delete",
            resource_type="user",
            resource_id="user123",
            details="Deleted user account",
            reason="Security violation"
        )
        
        log = audit_system.audit_logs[log_id]
        assert log["operation"] == "delete"
        assert log["reason"] is not None, "Security operations should have reason recorded"
    
    def test_audit_log_complete_audit_trail(self, audit_system):
        """测试完整审计链"""
        # 模拟一个完整的操作链
        audit_system.log_operation("user1", "create", "doc", "d1", "Created document")
        audit_system.log_operation("user1", "update", "doc", "d1", "Updated content")
        audit_system.log_operation("manager1", "approve", "doc", "d1", "Approved document")
        audit_system.log_operation("admin1", "delete", "doc", "d1", "Deleted document")
        
        # 查询完整链
        logs = audit_system.query_logs(resource_type="doc", resource_id="d1")
        assert len(logs) == 4, "Complete audit trail should be preserved"


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])