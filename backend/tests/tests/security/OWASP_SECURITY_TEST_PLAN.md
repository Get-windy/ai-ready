# AI-Ready 安全测试方案 (OWASP Top 10 2021)

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档版本 | v1.0.0 |
| 创建日期 | 2026-04-12 |
| 适用范围 | AI-Ready 项目全系统 |
| 测试标准 | OWASP Top 10 2021 |

---

## 目录

1. [概述](#概述)
2. [OWASP Top 10 测试覆盖](#owasp-top-10-测试覆盖)
3. [测试执行计划](#测试执行计划)
4. [测试工具与环境](#测试工具与环境)
5. [测试报告模板](#测试报告模板)

---

## 概述

本安全测试方案基于 OWASP Top 10 2021 标准，针对 AI-Ready 项目进行全面的安全性测试。测试范围包括：

- SQL注入测试
- XSS跨站脚本测试
- CSRF跨站请求伪造测试
- 权限绕过测试
- 安全配置检查
- 敏感数据保护

---

## OWASP Top 10 测试覆盖

### A01:2021 - 失效的访问控制 (Broken Access Control)

**测试目标**: 验证系统是否正确实施访问控制

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A01-01 | 水平权限提升 | 用户A尝试访问用户B的资源 | 访问被拒绝(403) |
| A01-02 | 垂直权限提升 | 普通用户尝试执行管理员操作 | 操作被拒绝(403) |
| A01-03 | IDOR攻击 | 修改URL参数访问其他用户数据 | 访问被拒绝或数据脱敏 |
| A01-04 | 未授权API访问 | 不带Token访问敏感API | 返回401未认证 |
| A01-05 | 目录遍历 | 尝试访问../etc/passwd等路径 | 访问被拒绝 |

**测试脚本**: `test_penetration.py::TestAuthorizationBypassPenetration`

---

### A02:2021 - 加密机制失效 (Cryptographic Failures)

**测试目标**: 验证数据加密和传输安全

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A02-01 | 密码传输加密 | 检查登录请求是否使用HTTPS | 强制HTTPS |
| A02-02 | 密码存储安全 | 验证密码是否使用BCrypt/Argon2 | 使用强哈希算法 |
| A02-03 | 敏感数据脱敏 | 检查API响应中的手机号/邮箱 | 已脱敏处理 |
| A02-04 | 弱加密算法 | 检查是否使用MD5/SHA1/DES | 未使用弱算法 |
| A02-05 | 密钥管理 | 检查密钥是否硬编码 | 使用环境变量 |

**测试脚本**: `test_api_security.py::TestDataSecurity`

---

### A03:2021 - 注入攻击 (Injection)

**测试目标**: 验证SQL注入、命令注入等防护

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A03-01 | SQL注入-登录 | 提交 `' OR '1'='1` 作为用户名 | 登录失败 |
| A03-02 | SQL注入-查询 | 在搜索框输入 `1' UNION SELECT * FROM users--` | 返回正常结果或错误但不泄露数据 |
| A03-03 | SQL注入-时间盲注 | 提交 `1' AND SLEEP(5)--` | 正常响应时间 |
| A03-04 | 命令注入 | 尝试 `; rm -rf /` 等命令 | 命令不执行 |
| A03-05 | NoSQL注入 | 尝试MongoDB注入payload | 被过滤或转义 |

**测试脚本**: `test_penetration.py::TestSQLInjectionPenetration`

**Payload列表**:
```python
sql_payloads = [
    "' OR '1'='1",
    "' OR '1'='1' --",
    "admin'--",
    "1' AND '1'='1",
    "' UNION SELECT NULL--",
    "1; DROP TABLE users--",
    "'; EXEC xp_cmdshell('dir')--",
    "1' AND SLEEP(5)--",
    "%27%20OR%20%271%27%3D%271",  # URL编码
]
```

---

### A04:2021 - 不安全设计 (Insecure Design)

**测试目标**: 验证安全设计原则的实施

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A04-01 | 业务逻辑漏洞 | 测试价格篡改、数量负数等 | 服务端验证 |
| A04-02 | 竞态条件 | 并发执行敏感操作 | 数据一致性保护 |
| A04-03 | 缺乏限流 | 高频请求API | 触发速率限制(429) |
| A04-04 | 缺乏审计日志 | 检查敏感操作是否记录 | 有完整日志 |

---

### A05:2021 - 安全配置错误 (Security Misconfiguration)

**测试目标**: 验证系统配置安全性

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A05-01 | 调试模式 | 检查是否启用debug模式 | 生产环境关闭 |
| A05-02 | 默认凭证 | 检查默认密码是否已修改 | 已修改 |
| A05-03 | 敏感端点暴露 | 访问/actuator、/swagger等 | 需要认证或禁用 |
| A05-04 | 错误信息泄露 | 触发错误查看响应 | 不泄露堆栈信息 |
| A05-05 | 安全响应头 | 检查X-Frame-Options等头 | 已配置 |

**测试脚本**: `test_penetration.py::TestSecurityConfiguration`

**安全头检查清单**:
```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
```

---

### A06:2021 - 易受攻击和过时组件 (Vulnerable and Outdated Components)

**测试目标**: 验证依赖组件安全性

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A06-01 | 已知漏洞依赖 | 扫描log4j、spring等组件 | 无高危CVE |
| A06-02 | 过期依赖 | 检查依赖版本 | 使用最新稳定版 |
| A06-03 | 未使用依赖 | 检查pom.xml | 移除未使用依赖 |

**测试脚本**: `security_scan.py::scan_dependencies`

**已知漏洞检查清单**:
- CVE-2021-44228 (Log4j)
- CVE-2022-22965 (Spring4Shell)
- CVE-2022-42003 (Jackson)

---

### A07:2021 - 身份识别和认证失效 (Identification and Authentication Failures)

**测试目标**: 验证认证机制安全性

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A07-01 | 暴力破解防护 | 多次错误登录尝试 | 账户锁定或验证码 |
| A07-02 | 弱密码策略 | 尝试简单密码 | 被拒绝 |
| A07-03 | 会话固定攻击 | 登录后检查session ID | 生成新session |
| A07-04 | JWT安全 | 尝试算法混淆攻击 | 拒绝none算法 |
| A07-05 | 密码重置漏洞 | 尝试重置其他用户密码 | 需要验证 |

**测试脚本**: `test_penetration.py::TestAuthenticationBypassPenetration`

---

### A08:2021 - 软件和数据完整性故障 (Software and Data Integrity Failures)

**测试目标**: 验证数据完整性保护

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A08-01 | 反序列化安全 | 尝试恶意序列化数据 | 被过滤 |
| A08-02 | 依赖完整性 | 检查是否使用签名依赖 | 使用官方仓库 |
| A08-03 | 自动更新安全 | 检查更新机制 | 使用HTTPS和签名验证 |

---

### A09:2021 - 安全日志和监控失效 (Security Logging and Monitoring Failures)

**测试目标**: 验证日志和监控机制

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A09-01 | 登录日志 | 执行登录操作 | 记录时间/IP/结果 |
| A09-02 | 敏感操作日志 | 执行删除/修改操作 | 有操作日志 |
| A09-03 | 异常日志 | 触发异常 | 记录异常信息 |
| A09-04 | 日志完整性 | 检查日志是否可篡改 | 日志受保护 |

---

### A10:2021 - 服务端请求伪造 (SSRF)

**测试目标**: 验证SSRF防护

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| A10-01 | 内网访问 | 尝试访问127.0.0.1/localhost | 被拒绝 |
| A10-02 | 元数据服务 | 尝试访问云厂商元数据API | 被拒绝 |
| A10-03 | 文件协议 | 尝试file://协议 | 被拒绝 |

---

## XSS 专项测试 (A07相关)

**测试目标**: 验证XSS防护机制

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| XSS-01 | 反射型XSS | 提交 `<script>alert('XSS')</script>` | 被过滤或转义 |
| XSS-02 | 存储型XSS | 在昵称/备注中存储XSS payload | 存储时转义 |
| XSS-03 | DOM型XSS | 通过hash/location注入 | 输入被清理 |
| XSS-04 | 事件处理器 | 测试onerror、onload等 | 被过滤 |
| XSS-05 | 编码绕过 | 使用HTML实体编码 | 被正确解析 |

**测试脚本**: `test_penetration.py::TestXSSPenetration`

**XSS Payload列表**:
```python
xss_payloads = [
    "<script>alert('XSS')</script>",
    "<img src=x onerror=alert('XSS')>",
    "<svg onload=alert('XSS')>",
    "<body onload=alert('XSS')>",
    "<iframe src='javascript:alert(1)'>",
    "<input onfocus=alert(1) autofocus>",
    "<marquee onstart=alert(1)>",
    "<details open ontoggle=alert(1)>",
    "<ScRiPt>alert('XSS')</sCrIpT>",  # 大小写绕过
    "&lt;script&gt;alert('XSS')&lt;/script&gt;",  # 实体编码
]
```

---

## CSRF 专项测试 (A01相关)

**测试目标**: 验证CSRF防护机制

**测试用例**:
| ID | 测试项 | 测试方法 | 预期结果 |
|----|--------|----------|----------|
| CSRF-01 | CSRF Token | 检查表单是否有Token | 有Token验证 |
| CSRF-02 | SameSite Cookie | 检查Cookie属性 | SameSite=Strict/Lax |
| CSRF-03 | 跨域请求 | 从其他域名发起POST | 被拒绝 |
| CSRF-04 | 状态变更保护 | 无Token执行修改操作 | 被拒绝 |

**测试脚本**: `test_penetration.py::TestCSRFPenetration`

---

## 测试执行计划

### 阶段一: 静态安全扫描
```bash
# 代码安全扫描
python tests/security/security_scan.py

# 依赖漏洞扫描
# 使用OWASP Dependency Check
```

### 阶段二: 动态渗透测试
```bash
# SQL注入测试
pytest tests/security/test_penetration.py::TestSQLInjectionPenetration -v

# XSS测试
pytest tests/security/test_penetration.py::TestXSSPenetration -v

# CSRF测试
pytest tests/security/test_penetration.py::TestCSRFPenetration -v

# 认证绕过测试
pytest tests/security/test_penetration.py::TestAuthenticationBypassPenetration -v

# 权限绕过测试
pytest tests/security/test_penetration.py::TestAuthorizationBypassPenetration -v
```

### 阶段三: API安全测试
```bash
# API安全测试
pytest tests/security/test_api_security.py -v

# 完整安全测试套件
pytest tests/security/ -v --tb=short
```

---

## 测试工具与环境

### 工具清单
| 工具 | 用途 | 版本 |
|------|------|------|
| pytest | 测试框架 | 9.0+ |
| requests | HTTP请求 | 2.31+ |
| OWASP ZAP | 自动化安全扫描 | 2.14+ |
| Burp Suite | 手动渗透测试 | 专业版 |

### 测试环境
```yaml
环境配置:
  BASE_URL: http://localhost:8080
  API_BASE: http://localhost:8080/api
  数据库: PostgreSQL 15
  缓存: Redis 7
```

---

## 测试报告模板

### 执行摘要
```
测试日期: 2026-04-12
测试范围: AI-Ready全系统
测试标准: OWASP Top 10 2021

总体评分: XX/100
风险等级: 低/中/高/严重

漏洞统计:
- 严重: X个
- 高危: X个
- 中危: X个
- 低危: X个
```

### 详细结果
| OWASP类别 | 测试项 | 状态 | 风险等级 |
|-----------|--------|------|----------|
| A01 | 水平权限提升 | PASS/FAIL | 高 |
| A03 | SQL注入 | PASS/FAIL | 严重 |
| ... | ... | ... | ... |

### 修复建议
1. **立即修复** (严重/高危)
2. **计划修复** (中危)
3. **建议修复** (低危)

---
