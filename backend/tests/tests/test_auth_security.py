#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 认证与授权安全测试用例
涵盖登录认证、会话管理、权限控制、JWT Token安全测试
"""

import pytest
import sys
import os
import re
import time
import hashlib
import secrets
import jwt
from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestLoginAuthentication:
    """登录认证安全测试"""
    
    def test_brute_force_protection(self):
        """暴力破解防护测试 - 验证登录失败次数限制"""
        login_attempts = {}
        MAX_ATTEMPTS = 5
        LOCKOUT_DURATION = 300  # 5分钟
        
        def attempt_login(username, password, ip_address):
            """模拟登录尝试，带暴力破解防护"""
            key = f"{username}:{ip_address}"
            now = time.time()
            
            # 检查是否已被锁定
            if key in login_attempts:
                attempts, last_attempt, locked_until = login_attempts[key]
                if locked_until and now < locked_until:
                    return False, "Account temporarily locked due to multiple failed attempts"
                elif locked_until and now >= locked_until:
                    # 解锁
                    login_attempts[key] = (0, now, None)
            
            # 验证密码（模拟）
            is_valid = password == "correct_password"
            
            if not is_valid:
                attempts = login_attempts.get(key, (0, 0, None))[0] + 1
                locked_until = None
                if attempts >= MAX_ATTEMPTS:
                    locked_until = now + LOCKOUT_DURATION
                    return False, f"Account locked for {LOCKOUT_DURATION} seconds"
                login_attempts[key] = (attempts, now, locked_until)
                return False, "Invalid credentials"
            
            # 登录成功，重置计数
            login_attempts[key] = (0, now, None)
            return True, "Login successful"
        
        # 测试正常登录
        success, msg = attempt_login("admin", "correct_password", "192.168.1.1")
        assert success == True, f"Valid login should succeed: {msg}"
        
        # 测试暴力破解触发锁定
        for i in range(MAX_ATTEMPTS):
            success, msg = attempt_login("admin", "wrong_password", "192.168.1.1")
        
        # 第6次尝试应该被锁定
        success, msg = attempt_login("admin", "correct_password", "192.168.1.1")
        assert success == False, "Should be locked after max attempts"
        assert "locked" in msg.lower(), f"Should indicate account is locked: {msg}"
    
    def test_password_strength_validation(self):
        """密码强度验证测试"""
        def validate_password_strength(password):
            """验证密码强度"""
            errors = []
            
            if len(password) < 8:
                errors.append("Password must be at least 8 characters")
            
            if not re.search(r'[A-Z]', password):
                errors.append("Password must contain uppercase letter")
            
            if not re.search(r'[a-z]', password):
                errors.append("Password must contain lowercase letter")
            
            if not re.search(r'\d', password):
                errors.append("Password must contain digit")
            
            if not re.search(r'[!@#$%^&*(),.?":{}|<>]', password):
                errors.append("Password must contain special character")
            
            common_passwords = ['password', '123456', 'qwerty', 'admin', 'letmein']
            if password.lower() in common_passwords:
                errors.append("Password is too common")
            
            return len(errors) == 0, errors
        
        # 测试弱密码
        weak_passwords = [
            ("123", ["length", "uppercase", "lowercase", "special"]),
            ("password", ["uppercase", "digit", "special", "common"]),
            ("Password", ["digit", "special"]),
            ("Password1", ["special"]),
        ]
        
        for pwd, expected_errors in weak_passwords:
            is_valid, errors = validate_password_strength(pwd)
            assert is_valid == False, f"Password '{pwd}' should be rejected"
        
        # 测试强密码
        strong_passwords = [
            "MyStr0ng!Pass",
            "C0mpl3x@P@ssw0rd",
            "S3cur3#P@ss123"
        ]
        
        for pwd in strong_passwords:
            is_valid, errors = validate_password_strength(pwd)
            assert is_valid == True, f"Password '{pwd}' should be accepted: {errors}"
    
    def test_captcha_mechanism(self):
        """验证码机制测试"""
        captcha_store = {}
        
        def generate_captcha():
            """生成验证码"""
            captcha_code = ''.join(secrets.choice('ABCDEFGHJKLMNPQRSTUVWXYZ23456789') for _ in range(6))
            captcha_id = secrets.token_hex(16)
            captcha_store[captcha_id] = {
                'code': captcha_code,
                'expires': time.time() + 300,  # 5分钟有效期
                'attempts': 0
            }
            return captcha_id, captcha_code
        
        def verify_captcha(captcha_id, user_input):
            """验证验证码"""
            if captcha_id not in captcha_store:
                return False, "Invalid captcha ID"
            
            captcha = captcha_store[captcha_id]
            
            if time.time() > captcha['expires']:
                del captcha_store[captcha_id]
                return False, "Captcha expired"
            
            if captcha['attempts'] >= 3:
                del captcha_store[captcha_id]
                return False, "Too many attempts"
            
            captcha['attempts'] += 1
            
            if user_input.upper() != captcha['code']:
                return False, "Invalid captcha"
            
            del captcha_store[captcha_id]
            return True, "Captcha verified"
        
        # 测试验证码生成
        captcha_id, code = generate_captcha()
        assert len(captcha_id) == 32
        assert len(code) == 6
        assert captcha_id in captcha_store
        
        # 测试正确验证
        success, msg = verify_captcha(captcha_id, code)
        assert success == True, f"Valid captcha should pass: {msg}"
        assert captcha_id not in captcha_store  # 使用后删除
        
        # 测试错误验证码
        captcha_id, code = generate_captcha()
        success, msg = verify_captcha(captcha_id, "WRONG1")
        assert success == False, "Wrong captcha should fail"
        
        # 测试过期验证码
        captcha_id, code = generate_captcha()
        captcha_store[captcha_id]['expires'] = time.time() - 1  # 设置为已过期
        success, msg = verify_captcha(captcha_id, code)
        assert success == False, "Expired captcha should fail"
        assert "expired" in msg.lower()
    
    def test_account_lockout_mechanism(self):
        """账户锁定机制测试"""
        account_status = {}
        
        def check_account_status(username):
            """检查账户状态"""
            if username not in account_status:
                return True, "Account active"
            
            status = account_status[username]
            if status.get('locked_until'):
                if time.time() < status['locked_until']:
                    remaining = int(status['locked_until'] - time.time())
                    return False, f"Account locked. Try again in {remaining} seconds"
                else:
                    # 自动解锁
                    status['failed_attempts'] = 0
                    status['locked_until'] = None
                    return True, "Account unlocked"
            return True, "Account active"
        
        def record_failed_attempt(username, max_attempts=3, lockout_duration=600):
            """记录失败尝试"""
            if username not in account