## SQL注入、XSS等安全测试用例补充

### TC-SEC-INJ-001: 字符串型SQL注入

**测试载荷**:

| 注入类型 | 测试载荷 | 预期结果 |
|----------|----------|----------|
| 单引号逃逸 | `'` | 被过滤/转义 |
| 布尔注入 | `' OR '1'='1` | 被过滤/无异常 |
| 联合注入 | `' UNION SELECT * FROM users--` | 被过滤/无异常 |
| 注释注入 | `'--` | 被过滤/无异常 |
| 时间盲注 | `' OR SLEEP(5)--` | 被过滤/无延迟 |
| 堆叠注入 | `'; DROP TABLE users--` | 被过滤/无异常 |

**测试接口**: GET /api/v1/users?keyword={payload}

**预期结果**:
- 系统正常处理输入
- 不执行恶意SQL
- 不返回数据库错误信息
- 查询结果正常（无额外数据）

---

### TC-SEC-INJ-002: 数字型SQL注入

**测试载荷**:

| 注入类型 | 测试载荷 | 预期结果 |
|----------|----------|----------|
| 数字运算 | `1 OR 1=1` | 被过滤/无异常 |
| 注释 | `1--` | 被过滤/无异常 |
| 联合注入 | `1 UNION SELECT * FROM users` | 被过滤/无异常 |
| 布尔盲注 | `1 AND 1=1` vs `1 AND 1=2` | 结果一致（参数化） |

---

## XSS防护测试

### TC-SEC-XSS-001: 存储型XSS防护

**测试载荷**:

```html
<script>alert('XSS')</script>
<img src=x onerror=alert('XSS')>
<svg onload=alert('XSS')>
javascript:alert('XSS')
```

**测试步骤**:
1. 在客户名称字段输入XSS载荷
2. 保存客户信息
3. 查看客户列表/详情页

**预期结果**:
- 载荷被HTML实体编码
- 不执行JavaScript代码
- 页面正常显示文本内容

---

### TC-SEC-XSS-002: CSP策略检查

**检查项**:
- [ ] Content-Security-Policy头存在
- [ ] script-src限制为同源或特定域名
- [ ] style-src有适当限制
- [ ] img-src有适当限制
- [ ] 不允许unsafe-inline

---

## CSRF防护测试

### TC-SEC-CSRF-001: CSRF Token验证

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 无Token | POST请求不带CSRF Token | 403拒绝 |
| 错误Token | 使用错误的CSRF Token | 403拒绝 |
| 过期Token | 使用过期的CSRF Token | 403拒绝 |
| 正确Token | 使用有效的CSRF Token | 200成功 |

---

## API安全测试用例

### TC-SEC-API-001: 无Token访问受保护接口

**测试步骤**:
1. 不携带Authorization头
2. 访问需要认证的接口

**预期结果**:
- 返回401 Unauthorized
- 错误码：AUTH_REQUIRED

---

### TC-SEC-API-002: Token伪造测试

| 场景 | Token示例 | 预期结果 |
|------|-----------|----------|
| 空Token | "" | 401 |
| 无效格式 | "invalid_token" | 401 |
| 篡改签名 | 修改JWT最后部分 | 401 |
| 过期Token | 使用过期Token | 401 |
| 算法切换 | alg:none攻击 | 401 |

---

### TC-SEC-API-003: 参数长度限制

| 参数 | 正常长度 | 超长测试 | 预期结果 |
|------|----------|----------|----------|
| username | 3-20 | 10000字符 | 400拒绝 |
| password | 8-50 | 10000字符 | 400拒绝 |
| description | 0-500 | 10000字符 | 400拒绝 |

---

### TC-SEC-API-004: JSON Bombs防护

**测试载荷**:
- 深度嵌套：1000层JSON嵌套
- 大数组：100万元素数组
- 重复键：1000个重复键

**预期结果**:
- 返回400 Bad Request
- 不导致内存溢出

---

### TC-SEC-API-005: 接口限流测试

**测试步骤**:
1. 在1分钟内发送100次API请求
2. 观察响应

**预期结果**:
- 前N次（如80次）返回200
- 超出限制后返回429 Too Many Requests

---

### TC-SEC-API-006: 登录限流测试

**测试步骤**:
1. 在1分钟内使用同一账号登录失败10次
2. 观察账号状态

**预期结果**:
- 账号被临时锁定
- 锁定时间递增

---

### TC-SEC-API-007: 错误信息隐藏

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 数据库错误 | 触发SQL错误 | 不暴露SQL语句 |
| 堆栈泄露 | 触发异常 | 不返回堆栈信息 |
| 路径泄露 | 访问不存在的文件 | 不暴露服务器路径 |

---

### TC-SEC-API-008: 路径遍历防护

**测试载荷**:
```
../../../etc/passwd
..\..\..\windows\system32\config\sam
....//....//....//etc/passwd
```

**预期结果**:
- 返回400或404
- 不返回系统文件内容

---

## Web安全测试用例

### TC-SEC-WEB-001: HTTP安全头检查

| 响应头 | 推荐值 |
|--------|--------|
| Strict-Transport-Security | max-age=31536000 |
| X-Content-Type-Options | nosniff |
| X-Frame-Options | DENY |
| X-XSS-Protection | 1; mode=block |
| Content-Security-Policy | 具体策略 |

---

### TC-SEC-WEB-002: 敏感文件访问测试

**测试路径**:
```
/.git/config
/.env
/WEB-INF/web.xml
/config/application.yml
/api-docs
/swagger-ui.html
/actuator/env
```

**预期结果**:
- 返回404或403
- 不暴露敏感配置文件

---

### TC-SEC-WEB-003: Session固定攻击防护

**测试步骤**:
1. 未登录前获取Session ID
2. 使用Session ID登录
3. 登录后检查Session ID是否变化

**预期结果**:
- 登录后Session ID重新生成
- 旧Session ID失效

---

### TC-SEC-WEB-004: Cookie安全属性

| Cookie | HttpOnly | Secure | SameSite |
|--------|----------|--------|----------|
| SessionID | ✅ | ✅ | Strict |
| AuthToken | ✅ | ✅ | Strict |
| RefreshToken | ✅ | ✅ | Strict |

---

## 移动端安全测试用例

### TC-SEC-MOB-001: 设备绑定验证

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 新设备登录 | 在新设备首次登录 | 需要额外验证 |
| 设备Token | 更换设备使用原Token | Token失效 |
| 设备管理 | 查看已登录设备 | 显示设备列表 |

---

### TC-SEC-MOB-002: 根/越狱检测

**预期结果**:
- 检测到Root/越狱设备时发出警告
- 可选择限制部分功能
- 记录安全日志

---

### TC-SEC-MOB-003: 证书固定验证

**预期结果**:
- 移动端应用验证服务器证书
- 不信任用户自定义证书
- 防止中间人攻击

---

## 测试执行指南

### 测试环境准备

| 环境 | 用途 | 配置要求 |
|------|------|----------|
| 开发环境 | 安全测试开发 | 独立数据库，测试数据 |
| 测试环境 | 自动化安全测试 | 与生产环境一致的安全配置 |
| 预发布环境 | 回归测试 | 生产数据脱敏 |

### 测试工具

| 工具 | 用途 |
|------|------|
| OWASP ZAP | Web应用安全扫描 |
| Burp Suite | 手动渗透测试 |
| sqlmap | SQL注入测试 |
| Postman | API安全测试 |
| Nmap | 端口扫描 |

### 测试执行流程

1. **静态分析**: 代码审计、配置检查
2. **动态扫描**: 自动化工具扫描
3. **手工测试**: 针对业务逻辑的渗透测试
4. **漏洞修复**: 修复高危漏洞
5. **复测验证**: 漏洞复测
6. **报告输出**: 安全测试报告

---

## 交付物清单

- [x] SECURITY_TEST_CASES.md（安全测试用例文档）
- [x] 安全测试用例清单（本文档）
- [ ] 安全测试工具配置
- [ ] 漏洞扫描报告模板
- [ ] 安全测试执行报告

---

**文档版本**: v1.0  
**更新日期**: 2026-04-15  
**负责人**: QA Lead
