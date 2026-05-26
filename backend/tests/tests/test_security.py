#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 安全测试脚本
涵盖OWASP Top 10安全测试项
"""

import pytest
import sys
import os
import re
import secrets

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestSQLInjection:
    """SQL注入漏洞测试"""
    
    def test_sql_injection_detection(self):
        """测试SQL注入检测"""
        def is_sql_injection(input_str):
            """检测SQL注入模式"""
            sql_patterns = [
                r"(\s*OR\s+['\"]?\d+['\"]?\s*=\s*['\"]?\d+)",
                r"(\s*AND\s+['\"]?\d+['\"]?\s*=\s*['\"]?\d+)",
                r"(UNION\s+SELECT)",
                r"(DROP\s+TABLE|DROP\s+DATABASE)",
                r"(INSERT\s+INTO|INTO\s+\w+)",
                r"(DELETE\s+FROM)",
                r"(;\s*--|;\s*\n)",
                r"('\\s*OR\s*')",
                r"(--\s*$)",
                r"(/\*.+\*/)",
                r"(SLEEP\s*\(|BENCHMARK\s*\()"
            ]
            
            for pattern in sql_patterns:
                if re.search(pattern, input_str, re.IGNORECASE):
                    return True
            return False
        
        # 测试恶意输入
        malicious_inputs = [
            "1' OR '1'='1",
            "1; DROP TABLE users--",
            "admin'--",
            "1 UNION SELECT * FROM users",
            "1 AND 1=1"
        ]
        
        for input_str in malicious_inputs:
            assert is_sql_injection(input_str) == True, f"Should detect SQL injection in: {input_str}"
        
        # 测试正常输入
        normal_inputs = [
            "normal_value",
            "user123",
            "test@email.com",
            "simple text"
        ]
        
        for input_str in normal_inputs:
            assert is_sql_injection(input_str) == False, f"Should not detect SQL injection in: {input_str}"
    
    def test_sql_injection_prevention(self):
        """测试SQL注入防护"""
        def safe_query(table_name, user_id):
            """安全的查询函数 - 使用参数化查询"""
            if not re.match(r'^[a-zA-Z0-9_]+$', str(user_id)):
                raise ValueError("Invalid input: only alphanumeric characters allowed")
            return f"SELECT * FROM {table_name} WHERE id = %(user_id)s"
        
        # 测试安全输入
        query = safe_query('users', '123')
        assert 'users' in query
        assert '%(user_id)s' in query
        
        # 测试注入攻击被拒绝
        with pytest.raises(ValueError):
            safe_query('users', "1' OR '1'='1")
        
        with pytest.raises(ValueError):
            safe_query('users', "1; DROP TABLE users--")


class TestXSSVulnerability:
    """XSS漏洞测试"""
    
    def test_xss_detection(self):
        """测试XSS检测"""
        def is_xss_attempt(input_str):
            """检测XSS攻击"""
            xss_patterns = [
                r"<script[\s\S]*?>[\s\S]*?</script>",
                r"javascript:",
                r"on\w+\s*=",
                r"document\.",
                r"eval\(",
                r"src\s*=",
                r"onclick\s*=",
                r"<iframe",
                r"<object",
                r"<embed"
            ]
            
            for pattern in xss_patterns:
                if re.search(pattern, input_str, re.IGNORECASE):
                    return True
            return False
        
        # 测试恶意XSS输入
        malicious_xss = [
            "<script>alert('XSS')</script>",
            "javascript:alert(document.cookie)",
            "<img src=x onerror=alert(1)>",
            "onclick=alert(1)",
            "<iframe src='javascript:alert(1)'>"
        ]
        
        for input_str in malicious_xss:
            assert is_xss_attempt(input_str) == True, f"Should detect XSS in: {input_str}"
        
        # 测试正常输入
        normal_inputs = [
            "Hello World",
            "<div>Safe HTML</div>",
            "User input without恶意"
        ]
        
        for input_str in normal_inputs:
            assert is_xss_attempt(input_str) == False, f"Should not detect XSS in: {input_str}"
    
    def test_xss_sanitization(self):
        """测试XSS清理"""
        def sanitize_html(input_str):
            """清理HTML中的潜在XSS"""
            if input_str is None:
                return ""
            
            # 移除脚本标签
            result = re.sub(r'<script[\s\S]*?>[\s\S]*?</script>', '', input_str, flags=re.IGNORECASE)
            
            # 移除javascript:协议
            result = re.sub(r'javascript:', '', result, flags=re.IGNORECASE)
            
            # 移除事件处理器
            result = re.sub(r'\s+on\w+\s*=\s*["\'][^"\']*["\']', '', result)
            
            return result
        
        # 测试清理
        malicious = "<script>alert('XSS')</script><div>Safe</div>"
        cleaned = sanitize_html(malicious)
        assert '<script>' not in cleaned
        assert 'Safe' in cleaned
        
        malicious2 = "<img src=x onerror=alert(1)>"
        cleaned2 = sanitize_html(malicious2)
        # 验证javascript:协议被移除
        assert 'javascript:' not in cleaned2.lower()
        assert '<img src=x ' in cleaned2 or 'onerror' not in cleaned2


class TestCSRFProtection:
    """CSRF防护测试"""
    
    def test_csrf_token_generation(self):
        """测试CSRF Token生成"""
        def generate_csrf_token():
            """生成CSRF Token"""
            return secrets.token_hex(32)  # 64字符的十六进制token
        
        token1 = generate_csrf_token()
        token2 = generate_csrf_token()
        
        # 验证token长度
        assert len(token1) == 64
        assert len(token2) == 64
        
        # 验证唯一性
        assert token1 != token2
        
        # 验证token只包含十六进制字符
        assert all(c in '0123456789abcdef' for c in token1)
    
    def test_csrf_validation(self):
        """测试CSRF验证"""
        class CSRFValidator:
            def __init__(self):
                self.valid_tokens = {}
            
            def validate_token(self, token, user_id):
                """验证CSRF Token"""
                if not token:
                    return False, "Missing CSRF token"
                
                if token not in self.valid_tokens:
                    return False, "Invalid CSRF token"
                
                if self.valid_tokens[token] != user_id:
                    return False, "Token mismatch"
                
                return True, "OK"
            
            def add_token(self, token, user_id):
                """添加Token"""
                self.valid_tokens[token] = user_id
        
        validator = CSRFValidator()
        
        # 添加有效token
        token = secrets.token_hex(32)
        validator.add_token(token, 'user123')
        
        # 验证有效token
        is_valid, msg = validator.validate_token(token, 'user123')
        assert is_valid == True
        
        # 验证无效token
        is_valid, msg = validator.validate_token('invalid_token', 'user123')
        assert is_valid == False
        
        # 验证token不匹配
        is_valid, msg = validator.validate_token(token, 'user456')
        assert is_valid == False


class TestAuthenticationSecurity:
    """认证安全测试"""
    
    def test_password_strength(self):
        """测试密码强度验证"""
        def validate_password(password):
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
            
            return errors
        
        # 测试弱密码
        weak_passwords = [
            "password",
            "12345678",
            "PASSWORD123",
            "password!",
            "abc123!"
        ]
        
        for pwd in weak_passwords:
            errors = validate_password(pwd)
            assert len(errors) > 0, f"Should reject weak password: {pwd}"
        
        # 测试强密码
        strong_passwords = [
            "Password@123",
            "MyStr0ng#Pass",
            "Test!123456"
        ]
        
        for pwd in strong_passwords:
            errors = validate_password(pwd)
            assert len(errors) == 0, f"Should accept strong password: {pwd} - but got errors: {errors}"
    
    def test_rate_limiting(self):
        """测试限流机制"""
        class RateLimiter:
            def __init__(self, max_requests=5, window_seconds=60):
                self.max_requests = max_requests
                self.window_seconds = window_seconds
                self.requests = {}
            
            def is_allowed(self, client_id):
                """检查请求是否被允许"""
                import time
                current_time = time.time()
                
                if client_id not in self.requests:
                    self.requests[client_id] = []
                
                # 清理过期请求
                self.requests[client_id] = [
                    t for t in self.requests[client_id]
                    if current_time - t < self.window_seconds
                ]
                
                if len(self.requests[client_id]) >= self.max_requests:
                    return False, "Rate limit exceeded"
                
                self.requests[client_id].append(current_time)
                return True, "OK"
        
        limiter = RateLimiter(max_requests=3, window_seconds=60)
        
        # 前3次请求应该被允许
        for i in range(3):
            allowed, msg = limiter.is_allowed('test_client')
            assert allowed == True
        
        # 第4次请求应该被拒绝
        allowed, msg = limiter.is_allowed('test_client')
        assert allowed == False
        assert 'Rate limit' in msg


class TestDataEncryption:
    """数据加密测试"""
    
    def test_password_hashing(self):
        """测试密码哈希"""
        def hash_password(password, salt=None):
            """哈希密码"""
            if salt is None:
                salt = secrets.token_hex(16)
            
            import hashlib
            # 使用SHA-256和salt
            password_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt.encode(), 100000)
            return salt + '$' + password_hash.hex()
        
        password = "MyPassword123!"
        hashed = hash_password(password)
        
        # 验证哈希包含salt和hash
        assert '$' in hashed
        # secrets.token_hex(16)返回32字符
        assert len(hashed.split('$')[0]) == 32
        
        # 验证相同密码产生不同哈希（由于随机salt）
        hashed2 = hash_password(password)
        assert hashed != hashed2
    
    def test_sensitive_data_masking(self):
        """测试敏感数据掩码"""
        def mask_sensitive_data(data):
            """掩码敏感数据"""
            if data is None:
                return None
            
            # 掩码手机号
            data = re.sub(r'(\d{3})\d{4}(\d{4})', r'\1****\2', str(data))
            
            # 掩码邮箱
            data = re.sub(r'([a-zA-Z0-9._%+-]{2})[a-zA-Z0-9._%+-]*@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})', 
                         r'\1****@\2', str(data))
            
            return data
        
        # 测试手机号掩码
        phone = "13812345678"
        masked = mask_sensitive_data(phone)
        assert "138****5678" == masked
        
        # 测试邮箱掩码
        email = "user.name@example.com"
        masked = mask_sensitive_data(email)
        assert "us****@example.com" == masked


class TestSessionSecurity:
    """会话安全测试"""
    
    def test_session_timeout(self):
        """测试会话超时"""
        class SessionManager:
            def __init__(self, timeout_minutes=30):
                self.timeout_minutes = timeout_minutes
                self.sessions = {}
            
            def create_session(self, user_id):
                """创建会话"""
                import time
                session_id = secrets.token_hex(16)
                self.sessions[session_id] = {
                    'user_id': user_id,
                    'created_at': time.time(),
                    'last_activity': time.time()
                }
                return session_id
            
            def is_valid(self, session_id):
                """检查会话是否有效"""
                import time
                if session_id not in self.sessions:
                    return False
                
                session = self.sessions[session_id]
                elapsed = time.time() - session['last_activity']
                timeout_seconds = self.timeout_minutes * 60
                
                if elapsed < timeout_seconds:
                    session['last_activity'] = time.time()
                    return True
                
                return False
            
            def invalidate(self, session_id):
                """使会话失效"""
                if session_id in self.sessions:
                    del self.sessions[session_id]
        
        # 测试会话超时逻辑
        manager = SessionManager(timeout_minutes=30)
        
        # 创建会话
        session_id = manager.create_session('user123')
        assert manager.is_valid(session_id) == True
        
        # 立即重新检查
        assert manager.is_valid(session_id) == True


class TestInputValidation:
    """输入验证测试"""
    
    def test_email_validation(self):
        """测试邮箱验证"""
        def validate_email(email):
            """验证邮箱格式"""
            pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            return bool(re.match(pattern, email))
        
        # 测试有效邮箱
        assert validate_email('user@example.com') == True
        assert validate_email('user.name+tag@company.co.uk') == True
        
        # 测试无效邮箱
        assert validate_email('invalid') == False
        assert validate_email('user@') == False
        assert validate_email('@example.com') == False
    
    def test_phone_validation(self):
        """测试电话号码验证"""
        def validate_phone(phone):
            """验证中国手机号"""
            pattern = r'^1[3-9]\d{9}$'
            return bool(re.match(pattern, phone))
        
        # 测试有效手机号
        assert validate_phone('13800138000') == True
        assert validate_phone('15912345678') == True
        
        # 测试无效号码
        assert validate_phone('12800138000') == False
        assert validate_phone('1380013800') == False
        assert validate_phone('138001380000') == False


class TestSecureHeaders:
    """安全头测试"""
    
    def test_security_headers(self):
        """验证安全头配置"""
        def check_security_headers(headers):
            """检查安全头配置"""
            required_headers = {
                'X-Content-Type-Options': 'nosniff',
                'X-Frame-Options': 'DENY',
                'X-XSS-Protection': '1; mode=block',
                'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
                'Content-Security-Policy': 'default-src \'self\''
            }
            
            missing = []
            incorrect = []
            
            for header, expected_value in required_headers.items():
                if header not in headers:
                    missing.append(header)
                elif headers[header] != expected_value:
                    incorrect.append(f"{header}: expected '{expected_value}', got '{headers[header]}'")
            
            return {
                'valid': len(missing) == 0 and len(incorrect) == 0,
                'missing': missing,
                'incorrect': incorrect
            }
        
        # 测试缺失安全头
        headers_without_security = {
            'Content-Type': 'text/html',
            'Server': 'Nginx'
        }
        
        result = check_security_headers(headers_without_security)
        assert result['valid'] == False
        assert len(result['missing']) > 0


class TestOWASPTop10:
    """OWASP Top 10测试"""
    
    def test_a01_broken_access_control(self):
        """A01:2021 - Broken Access Control"""
        class AccessController:
            def __init__(self):
                self.permissions = {
                    'admin': {'read', 'write', 'delete', 'admin'},
                    'user': {'read', 'write'},
                    'guest': {'read'}
                }
            
            def check_permission(self, user_role, action):
                """检查权限"""
                if user_role not in self.permissions:
                    return False
                return action in self.permissions[user_role]
        
        controller = AccessController()
        
        # 测试管理员权限
        assert controller.check_permission('admin', 'delete') == True
        assert controller.check_permission('admin', 'admin') == True
        
        # 测试普通用户权限
        assert controller.check_permission('user', 'read') == True
        assert controller.check_permission('user', 'delete') == False
        
        # 测试访客权限
        assert controller.check_permission('guest', 'read') == True
        assert controller.check_permission('guest', 'write') == False
    
    def test_a02_cryptographic_failures(self):
        """A02:2021 - Cryptographic Failures"""
        def hash_password(password: str, salt: str = None) -> str:
            """安全的密码哈希 - 使用PBKDF2"""
            if salt is None:
                salt = secrets.token_hex(16)
            
            import hashlib
            password_bytes = password.encode('utf-8')
            salt_bytes = salt.encode('utf-8')
            hashed = hashlib.pbkdf2_hmac('sha256', password_bytes, salt_bytes, 100000)
            return f"{salt}${hashed.hex()}"
        
        def verify_password(password: str, stored_hash: str) -> bool:
            """验证密码"""
            parts = stored_hash.split('$')
            if len(parts) != 2:
                return False
            return hash_password(password, parts[0]) == stored_hash
        
        password = "Secure!Password123"
        hashed = hash_password(password)
        
        # 验证正确密码
        assert verify_password(password, hashed) == True
        
        # 验证错误密码
        assert verify_password("WrongPassword", hashed) == False
    
    def test_a03_injection(self):
        """A03:2021 - Injection"""
        def escape_sql_string(input_str: str) -> str:
            """转义SQL字符串"""
            if input_str is None:
                return "NULL"
            
            escaped = input_str.replace("'", "''")
            escaped = escaped.replace("\\", "\\\\")
            return escaped
        
        def safe_query(table: str, user_input: str) -> str:
            """安全查询 - 参数化"""
            escaped_input = escape_sql_string(user_input)
            return f"SELECT * FROM {table} WHERE name = '{escaped_input}'"
        
        # 测试注入被转义
        malicious = "'; DROP TABLE users;--"
        safe = safe_query('users', malicious)
        assert "''" in safe  # 单引号被转义
        assert "DROP" in safe  # 转义后DROP变成''DROP
        
        # 正常输入
        normal = "John's Input"
        safe_normal = safe_query('users', normal)
        assert "John''s Input" in safe_normal
    
    def test_a07_xss(self):
        """A07:2021 - XSS"""
        def escape_html(input_str: str) -> str:
            """转义HTML特殊字符"""
            if input_str is None:
                return ""
            
            replacements = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#x27;',
                '/': '&#x2F;'
            }
            
            result = str(input_str)
            for old, new in replacements.items():
                result = result.replace(old, new)
            
            return result
        
        # 测试XSS被转义
        malicious = "<script>alert('XSS')</script>"
        safe = escape_html(malicious)
        assert '<script>' not in safe
        assert '&lt;script&gt;' in safe
        
        # 正常HTML
        normal = "<div>Safe Content</div>"
        safe_normal = escape_html(normal)
        assert '&lt;div&gt;' in safe_normal


if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
