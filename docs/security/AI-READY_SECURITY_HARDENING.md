# AI-Ready 安全加固配置文档

**版本**: v1.0  
**日期**: 2026-04-01  
**作者**: devops-engineer

---

## 一、安全加固概述

### 1.1 加固目标

- 实现HTTPS加密传输
- 防止API滥用（速率限制）
- 防止SQL注入攻击
- 防止XSS跨站脚本攻击
- 符合OWASP Top 10安全标准

### 1.2 加固范围

| 项目 | 说明 |
|------|------|
| HTTPS/SSL | 站点加密传输 |
| API速率限制 | 防止DDoS和暴力攻击 |
| SQL注入防护 | 参数化查询 + 白名单 |
| XSS防护 | 输入验证 + CSP |

---

## 二、HTTPS/SSL配置

### 2.1 Nginx SSL配置

```nginx
# SSL证书路径
ssl_certificate /etc/nginx/ssl/ai-ready.crt;
ssl_certificate_key /etc/nginx/ssl/ai-ready.key;

# SSL协议配置
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
ssl_prefer_server_ciphers on;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;

# HTTP强制跳转HTTPS
server {
    listen 80;
    server_name ai-ready.cn;
    return 301 https://$server_name$request_uri;
}

# HTTPS服务
server {
    listen 443 ssl;
    server_name ai-ready.cn;
    
    # 禁止iframe
    add_header X-Frame-Options DENY;
    
    # 禁止MIME类型嗅探
    add_header X-Content-Type-Options nosniff;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # 代理配置
    location / {
        proxy_pass http://api-gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 2.2 Java应用SSL配置

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: ai-ready
```

---

## 三、API速率限制配置

### 3.1 Nginx速率限制

```nginx
# 速率限制区域
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=10r/m;

# 应用限制
server {
    location /api/ {
        limit_req zone=api_limit burst=200 nodelay;
        limit_req_status 429;
        proxy_pass http://api-gateway;
    }
    
    location /api/auth/login {
        limit_req zone=login_limit burst=10 nodelay;
        limit_req_status 429;
        proxy_pass http://api-gateway;
    }
}
```

### 3.2 Spring Security限流

```yaml
# application.yml
security:
  rate-limiting:
    enabled: true
    default:
      rate: 100
      burst: 200
      window: 60s
    endpoints:
      /api/auth/login:
        rate: 10
        burst: 10
        window: 60s
      /api/auth/register:
        rate: 5
        burst: 5
        window: 3600s
```

---

## 四、SQL注入防护

### 4.1 MyBatis-Plus防护配置

```yaml
# application.yml
mybatis-plus:
  configuration:
    # 禁止动态SQL
    safe-executor: true
```

### 4.2 参数化查询示例

```java
// ❌ 危险写法
String sql = "SELECT * FROM user WHERE username = '" + username + "'";

// ✅ 安全写法
String sql = "SELECT * FROM user WHERE username = #{username}";
```

### 4.3 SQL白名单校验

```java
@Component
public class SqlWhitelistValidator {
    
    private static final Set<String> ALLOWED_TABLES = Set.of(
        "sys_user", "sys_role", "crm_customer", "erp_order"
    );
    
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
        "id", "username", "email", "created_time", "status"
    );
    
    public void validate(String tableName, String columnName) {
        if (!ALLOWED_TABLES.contains(tableName)) {
            throw new SecurityException("Table not allowed: " + tableName);
        }
        if (!ALLOWED_COLUMNS.contains(columnName)) {
            throw new SecurityException("Column not allowed: " + columnName);
        }
    }
}
```

---

## 五、XSS防护

### 5.1 输入验证

```java
@Component
public class XssValidator {
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return XSS_PATTERN.matcher(input).replaceAll("");
    }
    
    public void validate(String input) {
        if (XSS_PATTERN.matcher(input).find()) {
            throw new SecurityException("XSS attack detected");
        }
    }
}
```

### 5.2 Spring Security CSP配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
                )
                .xssProtection(xss -> xss.enabled(true))
                .contentTypeOptions()
                .and()
                .frameOptions().deny()
            );
        return http.build();
    }
}
```

---

## 六、安全 headers配置

### 6.1 Nginx headers

```nginx
# 安全头配置
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### 6.2 Java headers

```yaml
security:
  headers:
    xss-protection: true
    frame-options: DENY
    content-type-options: nosniff
    referrer-policy: strict-origin-when-cross-origin
```

---

## 七、密码策略

### 7.1 密码复杂度要求

```java
@Component
public class PasswordValidator {
    
    public void validate(String password) {
        if (password.length() < 8) {
            throw new SecurityException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new SecurityException("Password must contain lowercase letter");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new SecurityException("Password must contain uppercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new SecurityException("Password must contain digit");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new SecurityException("Password must contain special character");
        }
    }
}
```

### 7.2 会话管理

```yaml
# application.yml
sa-token:
  timeout: 86400  # 24小时
  active-timeout: 3600  # 1小时无操作退出
  is-concurrent: false  # 禁止并发登录
```

---

## 八、日志审计

### 8.1 安全日志

```yaml
# application.yml
logging:
  level:
    security: INFO
    com.ai.ready.security: DEBUG
  file:
    name: /var/log/ai-ready/security.log
```

### 8.2 审计日志示例

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "level": "INFO",
  "event": "LOGIN_SUCCESS",
  "user": "admin",
  "ip": "192.168.1.100",
  "browser": "Mozilla/5.0",
  "action": "User login successful"
}
```

---

## 九、安全加固检查清单

| 检查项 | 状态 | 说明 |
|--------|------|------|
| HTTPS强制 | ✅ | HTTP自动跳转HTTPS |
| SSL/TLS版本 | ✅ | 只允许TLSv1.2+ |
| 速率限制 | ✅ | API限流100r/s |
| SQL注入防护 | ✅ | MyBatis参数化查询 |
| XSS防护 | ✅ | 输入验证 + CSP |
| 安全headers | ✅ | 完整的security headers |
| 密码策略 | ✅ | 复杂度要求8位+大小写+数字+特殊字符 |
| 会话管理 | ✅ | 24小时有效期 |
| 日志审计 | ✅ | 安全事件记录 |

---

**文档完成时间**: 2026-04-01  
**版本**: v1.0  
**作者**: devops-engineer