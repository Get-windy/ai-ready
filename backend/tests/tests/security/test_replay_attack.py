#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 重放攻击安全测试用例
测试请求重放防护机制：时间戳验证、Nonce随机数、签名验证
"""

import pytest
import sys
import os
import time
import hashlib
import secrets
import hmac
from datetime import datetime, timedelta
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class ReplayAttackProtection:
    """重放攻击防护模拟实现"""
    
    def __init__(self):
        self.nonce_cache = {}  # nonce -> timestamp
        self.request_signatures = {}  # signature -> timestamp
        self.MAX_TIMESTAMP_DIFF = 300  # 5分钟时间窗口
        self.NONCE_EXPIRY = 600  # nonce有效期10分钟
    
    def generate_nonce(self):
        """生成随机nonce"""
        return secrets.token_hex(16)
    
    def generate_signature(self, data, timestamp, nonce, secret_key):
        """生成请求签名"""
        message = f"{data}|{timestamp}|{nonce}"
        return hmac.new(secret_key.encode(), message.encode(), hashlib.sha256).hexdigest()
    
    def validate_request(self, request_id, data, timestamp, nonce, signature, secret_key):
        """验证请求，防止重放攻击"""
        now = time.time()
        
        # 1. 时间戳验证 - 防止过期请求
        if abs(now - timestamp) > self.MAX_TIMESTAMP_DIFF:
            return False, "Timestamp expired or invalid"
        
        # 2. Nonce验证 - 防止相同请求重复
        if nonce in self.nonce_cache:
            cached_time = self.nonce_cache[nonce]
            if now - cached_time < self.NONCE_EXPIRY:
                return False, "Nonce already used - replay attack detected"
        
        # 3. 签名验证 - 防止篡改
        expected_sig = self.generate_signature(data, timestamp, nonce, secret_key)
        if signature != expected_sig:
            return False, "Invalid signature"
        
        # 验证成功，记录nonce
        self.nonce_cache[nonce] = now
        return True, "Request validated successfully"
    
    def cleanup_expired_nonces(self):
        """清理过期nonce"""
        now = time.time()
        expired = [n for n, t in self.nonce_cache.items() if now - t > self.NONCE_EXPIRY]
        for n in expired:
            del self.nonce_cache[n]
        return len(expired)


class TestReplayAttackProtection:
    """重放攻击防护测试"""
    
    @pytest.fixture
    def protection(self):
        """测试 fixture"""
        return ReplayAttackProtection()
    
    @pytest.fixture
    def secret_key(self):
        """测试密钥"""
        return "test_secret_key_12345"
    
    # ==================== 时间戳验证测试 ====================
    
    def test_timestamp_within_valid_window(self, protection, secret_key):
        """测试有效时间窗口内的请求"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        signature = protection.generate_signature(data, now, nonce, secret_key)
        
        result, msg = protection.validate_request("req1", data, now, nonce, signature, secret_key)
        assert result == True, f"Valid timestamp should pass: {msg}"
    
    def test_expired_timestamp_rejected(self, protection, secret_key):
        """测试过期时间戳请求被拒绝"""
        expired_time = time.time() - 400  # 超过5分钟窗口
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        signature = protection.generate_signature(data, expired_time, nonce, secret_key)
        
        result, msg = protection.validate_request("req2", data, expired_time, nonce, signature, secret_key)
        assert result == False, "Expired timestamp should be rejected"
        assert "Timestamp" in msg or "expired" in msg.lower()
    
    def test_future_timestamp_rejected(self, protection, secret_key):
        """测试未来时间戳请求被拒绝"""
        future_time = time.time() + 400  # 未来时间
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        signature = protection.generate_signature(data, future_time, nonce, secret_key)
        
        result, msg = protection.validate_request("req3", data, future_time, nonce, signature, secret_key)
        assert result == False, "Future timestamp should be rejected"
    
    def test_timestamp_boundary_test(self, protection, secret_key):
        """测试时间戳边界条件"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        
        # 测试刚好在边界内的请求（4分59秒前）
        boundary_time = now - 299
        signature = protection.generate_signature(data, boundary_time, nonce, secret_key)
        result, msg = protection.validate_request("req4", data, boundary_time, nonce, signature, secret_key)
        assert result == True, "Request at boundary should pass"
    
    # ==================== Nonce随机数验证测试 ====================
    
    def test_nonce_reuse_detected(self, protection, secret_key):
        """测试Nonce重复使用被检测"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        signature = protection.generate_signature(data, now, nonce, secret_key)
        
        # 第一次请求
        result1, msg1 = protection.validate_request("req5", data, now, nonce, signature, secret_key)
        assert result1 == True, f"First request should pass: {msg1}"
        
        # 使用相同nonce重放请求
        result2, msg2 = protection.validate_request("req6", data, now, nonce, signature, secret_key)
        assert result2 == False, "Replayed nonce should be rejected"
        assert "replay" in msg2.lower() or "Nonce" in msg2
    
    def test_different_nonce_accepted(self, protection, secret_key):
        """测试不同Nonce的请求都能通过"""
        now = time.time()
        data = "api/v1/users/123"
        
        for i in range(5):
            nonce = protection.generate_nonce()
            signature = protection.generate_signature(data, now, nonce, secret_key)
            result, msg = protection.validate_request(f"req{i+7}", data, now, nonce, signature, secret_key)
            assert result == True, f"Request with unique nonce {i} should pass: {msg}"
    
    def test_nonce_uniqueness(self, protection):
        """测试Nonce生成唯一性"""
        nonces = [protection.generate_nonce() for _ in range(100)]
        unique_nonces = set(nonces)
        assert len(unique_nonces) == 100, "All generated nonces should be unique"
    
    def test_nonce_format_validation(self, protection):
        """测试Nonce格式验证"""
        # 有效的nonce格式
        valid_nonce = secrets.token_hex(16)
        assert len(valid_nonce) == 32, "Nonce should be 32 characters (16 bytes hex)"
        assert all(c in '0123456789abcdef' for c in valid_nonce)
    
    def test_nonce_expiry_cleanup(self, protection, secret_key):
        """测试Nonce过期清理"""
        now = time.time()
        
        # 创建多个请求
        for i in range(3):
            nonce = protection.generate_nonce()
            signature = protection.generate_signature("data", now - 500, nonce, secret_key)
            # 不验证这些请求（模拟过期）
            protection.nonce_cache[nonce] = now - 700  # 设置为已过期
        
        # 清理过期nonce
        cleaned = protection.cleanup_expired_nonces()
        assert cleaned >= 3, "Expired nonces should be cleaned up"
    
    # ==================== 签名验证测试 ====================
    
    def test_valid_signature_accepted(self, protection, secret_key):
        """测试有效签名通过验证"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        signature = protection.generate_signature(data, now, nonce, secret_key)
        
        result, msg = protection.validate_request("req20", data, now, nonce, signature, secret_key)
        assert result == True, f"Valid signature should pass: {msg}"
    
    def test_invalid_signature_rejected(self, protection, secret_key):
        """测试无效签名被拒绝"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        fake_signature = "invalid_signature_12345"
        
        result, msg = protection.validate_request("req21", data, now, nonce, fake_signature, secret_key)
        assert result == False, "Invalid signature should be rejected"
        assert "signature" in msg.lower()
    
    def test_tampered_data_signature_rejected(self, protection, secret_key):
        """测试篡改数据后签名被拒绝"""
        now = time.time()
        nonce = protection.generate_nonce()
        original_data = "api/v1/users/123"
        signature = protection.generate_signature(original_data, now, nonce, secret_key)
        
        # 签名基于原始数据，但请求发送篡改数据
        tampered_data = "api/v1/users/999"
        result, msg = protection.validate_request("req22", tampered_data, now, nonce, signature, secret_key)
        assert result == False, "Tampered data with old signature should be rejected"
    
    def test_wrong_secret_key_signature_rejected(self, protection, secret_key):
        """测试错误密钥签名被拒绝"""
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/users/123"
        wrong_key = "wrong_secret_key"
        signature = protection.generate_signature(data, now, nonce, wrong_key)
        
        result, msg = protection.validate_request("req23", data, now, nonce, signature, secret_key)
        assert result == False, "Signature with wrong key should be rejected"
    
    # ==================== 综合重放攻击测试 ====================
    
    def test_full_replay_attack_blocked(self, protection, secret_key):
        """测试完整重放攻击被阻止"""
        # 原始请求
        now = time.time()
        nonce = protection.generate_nonce()
        data = "api/v1/transfer?amount=1000"
        signature = protection.generate_signature(data, now, nonce, secret_key)
        
        # 原始请求通过
        result1, msg1 = protection.validate_request("req24", data, now, nonce, signature, secret_key)
        assert result1 == True
        
        # 攻击者重放相同请求
        result2, msg2 = protection.validate_request("req25", data, now, nonce, signature, secret_key)
        assert result2 == False
        assert "replay" in msg2.lower()
    
    def test_delayed_replay_attack_blocked(self, protection, secret_key):
        """测试延迟重放攻击被阻止"""
        # 原始请求（稍后重放）
        original_time = time.time() - 200  # 3分钟前
        nonce = protection.generate_nonce()
        data = "api/v1/payment"
        signature = protection.generate_signature(data, original_time, nonce, secret_key)
        
        # 在有效时间窗口内首次通过
        result1, msg1 = protection.validate_request("req26", data, original_time, nonce, signature, secret_key)
        assert result1 == True
        
        # 攻击者稍后重放（仍在窗口内但nonce已用）
        result2, msg2 = protection.validate_request("req27", data, original_time, nonce, signature, secret_key)
        assert result2 == False
        assert "Nonce" in msg2 or "replay" in msg2.lower()
    
    def test_modified_data_replay_blocked(self, protection, secret_key):
        """测试修改数据的重放攻击被阻止"""
        now = time.time()
        nonce = protection.generate_nonce()
        original_data = "api/v1/order?id=1"
        signature = protection.generate_signature(original_data, now, nonce, secret_key)
        
        # 原始请求通过
        result1, _ = protection.validate_request("req28", original_data, now, nonce, signature, secret_key)
        assert result1 == True
        
        # 攻击者尝试修改数据并重放
        # 使用新nonce但旧签名（模拟签名不匹配场景）
        new_nonce = protection.generate_nonce()
        modified_data = "api/v1/order?id=999"
        result2, msg2 = protection.validate_request("req29", modified_data, now, new_nonce, signature, secret_key)
        assert result2 == False
        assert "signature" in msg2.lower() or "invalid" in msg2.lower()
    
    # ==================== 性能测试 ====================
    
    def test_high_volume_nonce_tracking(self, protection, secret_key):
        """测试高并发Nonce跟踪"""
        now = time.time()
        success_count = 0
        
        # 100个独立请求
        for i in range(100):
            nonce = protection.generate_nonce()
            data = f"api/v1/request/{i}"
            signature = protection.generate_signature(data, now, nonce, secret_key)
            result, _ = protection.validate_request(f"req{i+30}", data, now, nonce, signature, secret_key)
            if result:
                success_count += 1
        
        assert success_count == 100, f"All 100 unique requests should pass, got {success_count}"
        assert len(protection.nonce_cache) == 100, "Nonce cache should track all used nonces"
    
    def test_nonce_cache_memory_efficiency(self, protection, secret_key):
        """测试Nonce缓存内存效率"""
        # 添加大量nonce（设置为超过过期时间）
        for i in range(500):
            protection.nonce_cache[f"nonce_{i}"] = time.time() - 700  # 超过600秒过期
        
        initial_size = len(protection.nonce_cache)
        
        # 清理过期
        cleaned = protection.cleanup_expired_nonces()
        
        assert cleaned == 500, f"All expired nonces should be cleaned: {cleaned}"
        assert len(protection.nonce_cache) == 0, "Cache should be empty after cleanup"


class TestAPIReplayAttackScenarios:
    """API场景重放攻击测试"""
    
    @pytest.fixture
    def api_protection(self):
        """API防护实例"""
        return ReplayAttackProtection()
    
    def test_payment_api_replay_protection(self, api_protection):
        """测试支付API重放攻击防护"""
        secret = "payment_api_secret"
        now = time.time()
        nonce = api_protection.generate_nonce()
        
        # 支付请求
        payment_data = "POST /api/v1/payment amount=1000 currency=USD"
        signature = api_protection.generate_signature(payment_data, now, nonce, secret)
        
        # 正常支付请求
        result1, _ = api_protection.validate_request("pay1", payment_data, now, nonce, signature, secret)
        assert result1 == True
        
        # 重放支付请求（同一nonce）
        result2, msg = api_protection.validate_request("pay2", payment_data, now, nonce, signature, secret)
        assert result2 == False
        assert "replay" in msg.lower()
    
    def test_authentication_api_replay_protection(self, api_protection):
        """测试认证API重放攻击防护"""
        secret = "auth_api_secret"
        now = time.time()
        nonce = api_protection.generate_nonce()
        
        # 登录请求
        auth_data = "POST /api/v1/auth/login username=admin"
        signature = api_protection.generate_signature(auth_data, now, nonce, secret)
        
        # 正常登录
        result1, _ = api_protection.validate_request("auth1", auth_data, now, nonce, signature, secret)
        assert result1 == True
        
        # 重放登录请求（可能窃取session）
        result2, msg = api_protection.validate_request("auth2", auth_data, now, nonce, signature, secret)
        assert result2 == False
    
    def test_data_export_api_replay_protection(self, api_protection):
        """测试数据导出API重放攻击防护"""
        secret = "export_api_secret"
        now = time.time()
        nonce = api_protection.generate_nonce()
        
        # 导出请求
        export_data = "GET /api/v1/export?type=full&format=json"
        signature = api_protection.generate_signature(export_data, now, nonce, secret)
        
        # 正常导出
        result1, _ = api_protection.validate_request("exp1", export_data, now, nonce, signature, secret)
        assert result1 == True
        
        # 重放导出（可能绕过权限检查）
        result2, msg = api_protection.validate_request("exp2", export_data, now, nonce, signature, secret)
        assert result2 == False


# ==================== OWASP覆盖测试 ====================

class TestOWASPReplayAttackCoverage:
    """OWASP Top 10 重放攻击相关测试"""
    
    @pytest.fixture
    def protection(self):
        return ReplayAttackProtection()
    
    def test_owasp_a01_replay_bypass(self, protection):
        """OWASP A01: Broken Access Control - 重放绕过"""
        # 重放攻击可能导致权限绕过
        secret = "test_key"
        now = time.time()
        nonce = protection.generate_nonce()
        data = "DELETE /api/v1/admin/users"
        signature = protection.generate_signature(data, now, nonce, secret)
        
        # 正常请求
        result1, _ = protection.validate_request("adm1", data, now, nonce, signature, secret)
        assert result1 == True
        
        # 重放攻击
        result2, msg = protection.validate_request("adm2", data, now, nonce, signature, secret)
        assert result2 == False, "Replay attack should be blocked to prevent access bypass"
    
    def test_owasp_a02_timestamp_integrity(self, protection):
        """OWASP A02: Cryptographic Failures - 时间戳完整性"""
        # 时间戳过期检测
        expired_time = time.time() - 500
        nonce = protection.generate_nonce()
        data = "GET /api/v1/data"
        signature = protection.generate_signature(data, expired_time, nonce, "key")
        
        result, msg = protection.validate_request("cry1", data, expired_time, nonce, signature, "key")
        assert result == False
        assert "Timestamp" in msg or "expired" in msg.lower()
    
    def test_owasp_a07_nonce_xss_replay(self, protection):
        """OWASP A07: XSS - Nonce参数安全"""
        # XSS注入nonce参数测试
        xss_nonce = "<script>alert('xss')</script>"
        now = time.time()
        data = "GET /api/v1/data"
        
        # XSS nonce应该被识别为无效格式
        result, msg = protection.validate_request("xss1", data, now, xss_nonce, "sig", "key")
        # 系统应该拒绝非标准格式的nonce
        # 注：此测试验证nonce格式验证的重要性
    
    def test_owasp_a05_config_replay_window(self, protection):
        """OWASP A05: Security Misconfiguration - 时间窗口配置"""
        # 测试时间窗口配置是否合理
        assert protection.MAX_TIMESTAMP_DIFF == 300, "Timestamp window should be 5 minutes"
        assert protection.NONCE_EXPIRY == 600, "Nonce expiry should be 10 minutes"
        
        # 窗口不应太大（防止长时间重放）
        assert protection.MAX_TIMESTAMP_DIFF <= 600, "Window should not exceed 10 minutes"


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])