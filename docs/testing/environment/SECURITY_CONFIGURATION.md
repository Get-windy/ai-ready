# 测试环境安全配置文档

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

---

## 目录

1. [概述](#1-概述)
2. [安全原则与策略](#2-安全原则与策略)
3. [访问控制与认证](#3-访问控制与认证)
4. [网络与通信安全](#4-网络与通信安全)
5. [数据安全与加密](#5-数据安全与加密)
6. [应用安全配置](#6-应用安全配置)
7. [监控与审计](#7-监控与审计)
8. [安全事件响应](#8-安全事件响应)
9. [安全检查清单](#9-安全检查清单)
10. [附录](#10-附录)

---

## 1. 概述

### 1.1 文档目的

本文档定义了 Sprint 27+1 测试环境的完整安全配置规范，包括访问控制、网络安全、数据安全、应用安全和安全监控等方面的配置要求。确保测试环境在提供高效服务的同时，满足基本的安全标准和合规要求。

### 1.2 适用范围

- **环境范围**: Sprint 27+1 测试环境所有组件
- **技术范围**: Docker容器、网络、数据库、API服务
- **人员范围**: 运维工程师、开发工程师、测试工程师
- **生命周期**: 环境部署、运行、维护、销毁全过程

### 1.3 安全目标

| 安全目标 | 具体指标 | 说明 |
|---------|---------|------|
| **机密性** | 数据加密传输率 ≥ 95% | 敏感数据传输必须加密 |
| **完整性** | 数据完整性检查率 100% | 所有关键数据都有完整性检查 |
| **可用性** | 服务可用性 ≥ 99.5% | 安全措施不影响正常服务 |
| **可审计性** | 安全日志保留 ≥ 90天 | 所有安全事件可追溯 |
| **合规性** | 符合公司安全基线 | 满足企业内部安全标准 |

---

## 2. 安全原则与策略

### 2.1 安全设计原则

**最小权限原则**
```bash
# Docker容器以非root用户运行
docker run --user=1000:1000 app-image

# 数据库用户权限最小化
GRANT SELECT, INSERT, UPDATE, DELETE ON database.* TO 'app_user'@'%';
```

**纵深防御原则**
```
┌─────────────────────────────────────────────────────────┐
│                    纵深防御层级                          │
└─────────────────────────────────────────────────────────┘
         │
         ├─→ [1] 网络层防御
         │    ├─ 防火墙规则
         │    ├─ 网络隔离
         │    └─ 入侵检测
         │
         ├─→ [2] 主机层防御
         │    ├─ 操作系统加固
         │    ├─ 容器安全配置
         │    └─ 安全补丁管理
         │
         ├─→ [3] 应用层防御
         │    ├─ 输入验证
         │    ├─ 身份认证
         │    └─ 访问控制
         │
         ├─→ [4] 数据层防御
         │    ├─ 数据加密
         │    ├─ 数据备份
         │    └─ 数据脱敏
         │
         └─────────────────────────────────────────────────────┘
```

**零信任原则**
- 默认不信任所有网络流量
- 身份验证是访问任何资源的前提
- 最小权限授予
- 持续验证和监控

### 2.2 安全策略

**访问控制策略**：
- 所有访问必须经过身份认证
- 基于角色的访问控制（RBAC）
- 定期审计访问权限
- 离职人员立即撤销权限

**数据保护策略**：
- 敏感数据必须加密存储
- 测试数据脱敏处理
- 数据传输使用TLS加密
- 定期备份和恢复测试

**网络安全策略**：
- 网络分区和隔离
- 防火墙默认拒绝
- 入侵检测和防护
- 网络流量监控

---

## 3. 访问控制与认证

### 3.1 用户身份认证

**认证方式**：

| 认证类型 | 适用场景 | 配置方法 |
|---------|---------|---------|
| **用户名密码** | Web界面登录 | Spring Security + JWT |
| **API密钥** | API调用认证 | API Gateway + Auth Server |
| **证书认证** | 服务间通信 | mTLS双向认证 |
| **SSH密钥** | 服务器访问 | SSH公钥认证 |

**Spring Security配置示例**：

```yaml
# application-security.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-server:8080/realms/ai-ready
    user:
      name: admin
      password: ${ADMIN_PASSWORD}
      roles: ADMIN
    
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400  # 24小时
    issuer: AI-Ready-Test
```

### 3.2 基于角色的访问控制（RBAC）

**角色定义**：

| 角色 | 权限 | 适用人员 |
|------|------|---------|
| **超级管理员** | 所有权限 | 运维负责人 |
| **系统管理员** | 环境管理、配置修改 | DevOps工程师 |
| **开发工程师** | 应用部署、日志查看 | 开发团队 |
| **测试工程师** | 测试执行、结果查看 | 测试团队 |
| **只读用户** | 查看权限 | 产品经理、观察者 |

**权限矩阵**：

| 资源/操作 | 超级管理员 | 系统管理员 | 开发工程师 | 测试工程师 | 只读用户 |
|-----------|-----------|-----------|-----------|-----------|---------|
| 环境部署 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 配置修改 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 服务重启 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 数据备份 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 数据恢复 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 日志查看 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 监控查看 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 用户管理 | ✅ | ✅ | ❌ | ❌ | ❌ |

### 3.3 多因素认证（MFA）

**MFA配置**：

```yaml
# MFA配置
multi-factor-auth:
  enabled: true
  methods:
    - totp:  # 时间型一次性密码
      issuer: AI-Ready-Test
      digits: 6
      period: 30
    - email: # 邮箱验证码
      sender: no-reply@ai-ready.local
      subject: "AI-Ready测试环境验证码"
      valid-minutes: 10
  required-roles:
    - SUPER_ADMIN
    - SYSTEM_ADMIN
```

**TOTP配置步骤**：

```bash
# 生成TOTP密钥
docker run --rm -v /etc/ai-ready/mfa:/data oathtool --totp --generate-secret

# 配置TOTP应用
# 1. 扫描二维码或输入密钥
# 2. 输入6位验证码确认
# 3. 保存恢复密钥
```

---

## 4. 网络与通信安全

### 4.1 网络隔离与分段

**Docker网络配置**：

```yaml
# docker-compose.security.yml
networks:
  public:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1
  
  private:
    driver: bridge
    internal: true  # 内部网络，不暴露给主机
    ipam:
      config:
        - subnet: 192.168.100.0/24
          gateway: 192.168.100.1
  
  database:
    driver: bridge
    internal: true
    ipam:
      config:
        - subnet: 10.10.10.0/24
          gateway: 10.10.10.1
```

**网络分段策略**：

| 网络段 | 子网 | 允许访问 | 说明 |
|-------|------|---------|------|
| **公共网络** | 172.20.0.0/16 | 互联网 → 反向代理 | 对外服务网络 |
| **应用网络** | 192.168.100.0/24 | 应用服务间通信 | 内部服务网络 |
| **数据库网络** | 10.10.10.0/24 | 应用 → 数据库 | 数据库专用网络 |
| **管理网络** | 10.20.20.0/24 | 管理员 → 管理服务 | 管理专用网络 |

### 4.2 防火墙配置

**Docker防火墙规则**：

```bash
# 创建防火墙链
sudo iptables -N DOCKER-TEST

# 默认策略
sudo iptables -P INPUT DROP
sudo iptables -P FORWARD DROP
sudo iptables -P OUTPUT ACCEPT

# 允许本地回环
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A OUTPUT -o lo -j ACCEPT

# 允许已建立的连接
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 允许SSH（仅管理员IP）
sudo iptables -A INPUT -p tcp --dport 22 -s 10.0.0.0/8 -j ACCEPT

# 允许HTTP/HTTPS（公共访问）
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 允许监控端口（内部网络）
sudo iptables -A INPUT -p tcp --dport 9090 -s 192.168.100.0/24 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 3000 -s 192.168.100.0/24 -j ACCEPT

# 保存规则
sudo iptables-save > /etc/iptables/rules.v4
```

### 4.3 TLS/SSL配置

**Nginx SSL配置**：

```nginx
# nginx/ssl.conf
ssl_certificate /etc/nginx/ssl/ai-ready.crt;
ssl_certificate_key /etc/nginx/ssl/ai-ready.key;

ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
ssl_prefer_server_ciphers off;

ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
ssl_session_tickets off;

# HSTS (HTTP Strict Transport Security)
add_header Strict-Transport-Security "max-age=63072000" always;

# 证书生成脚本
#!/bin/bash
# generate-ssl-cert.sh
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/ai-ready.key \
  -out /etc/nginx/ssl/ai-ready.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=AI-Ready/CN=test-env.ai-ready.local"
```

**mTLS双向认证配置**：

```yaml
# 服务间mTLS配置
services:
  app-service:
    environment:
      - MTLS_ENABLED=true
      - MTLS_CA_CERT=/certs/ca.crt
      - MTLS_SERVER_CERT=/certs/server.crt
      - MTLS_SERVER_KEY=/certs/server.key
      - MTLS_CLIENT_CERT=/certs/client.crt
      - MTLS_CLIENT_KEY=/certs/client.key
    volumes:
      - ./certs:/certs:ro
```

---

## 5. 数据安全与加密

### 5.1 数据库安全配置

**PostgreSQL安全配置**：

```sql
-- PostgreSQL安全配置脚本
-- 1. 创建最小权限用户
CREATE USER app_user WITH PASSWORD '${DB_PASSWORD}' NOSUPERUSER NOCREATEDB NOCREATEROLE;

-- 2. 创建数据库
CREATE DATABASE ai_ready_test WITH OWNER app_user ENCODING 'UTF8' LC_COLLATE 'en_US.utf8' LC_CTYPE 'en_US.utf8';

-- 3. 配置连接限制
ALTER USER app_user CONNECTION LIMIT 10;

-- 4. 配置SSL连接
ALTER SYSTEM SET ssl = on;
ALTER SYSTEM SET ssl_cert_file = '/var/lib/postgresql/data/server.crt';
ALTER SYSTEM SET ssl_key_file = '/var/lib/postgresql/data/server.key';

-- 5. 配置密码加密
ALTER SYSTEM SET password_encryption = 'scram-sha-256';

-- 6. 配置访问控制
-- pg_hba.conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD
hostssl all             app_user        192.168.100.0/24        scram-sha-256
hostssl all             admin_user      10.20.20.0/24           scram-sha-256
hostssl all             all             0.0.0.0/0               reject
```

**Redis安全配置**：

```yaml
# Redis安全配置
services:
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes --appendfsync everysec
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
    networks:
      - database
```

### 5.2 数据加密

**应用层数据加密**：

```java
// Spring Boot数据加密配置
@Configuration
public class EncryptionConfig {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionKey, "deadbeef");
    }
    
    @Bean
    public JasyptEncryptor jasyptEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(encryptionKey);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
```

**敏感字段加密示例**：

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String email;
    
    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;
    
    @Column(nullable = false)
    private String passwordHash;  // bcrypt加密
}

// 加密转换器
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private TextEncryptor textEncryptor;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? textEncryptor.encrypt(attribute) : null;
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? textEncryptor.decrypt(dbData) : null;
    }
}
```

### 5.3 数据脱敏

**测试数据脱敏规则**：

```yaml
# data-masking-rules.yml
masking:
  rules:
    - table: users
      columns:
        - name: email
          type: email
          mask: "user######@masked.com"
        - name: phone
          type: phone
          mask: "138****####"
        - name: id_card
          type: id_card
          mask: "110101******####XX"
        - name: real_name
          type: name
          mask: "张*"
    
    - table: orders
      columns:
        - name: address
          type: address
          mask: "北京市**区****路###号"
        - name: receiver_phone
          type: phone
          mask: "138****####"
```

**脱敏脚本示例**：

```python
# data-masking.py
import hashlib
import random
import string

def mask_email(email):
    """邮箱脱敏"""
    if '@' not in email:
        return email
    local_part, domain = email.split('@')
    if len(local_part) <= 2:
        return f"{local_part[0]}***@{domain}"
    return f"{local_part[0]}***{local_part[-1]}@{domain}"

def mask_phone(phone):
    """手机号脱敏"""
    if len(phone) != 11:
        return phone
    return f"{phone[:3]}****{phone[-4:]}"

def mask_id_card(id_card):
    """身份证脱敏"""
    if len(id_card) != 18:
        return id_card
    return f"{id_card[:6]}******{id_card[-4:]}"

def generate_test_data(original_data, masking_rules):
    """生成脱敏测试数据"""
    masked_data = original_data.copy()
    for rule in masking_rules:
        table = rule['table']
        for column_rule in rule['columns']:
            col_name = column_rule['name']
            mask_type = column_rule['type']
            if col_name in masked_data:
                if mask_type == 'email':
                    masked_data[col_name] = mask_email(masked_data[col_name])
                elif mask_type == 'phone':
                    masked_data[col_name] = mask_phone(masked_data[col_name])
                elif mask_type == 'id_card':
                    masked_data[col_name] = mask_id_card(masked_data[col_name])
    return masked_data
```

---

## 6. 应用安全配置

### 6.1 Web应用安全

**Spring Security配置**：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF保护
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/public/**")
            )
            
            // CORS配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 会话管理
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            
            // 认证配置
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            
            // 表单登录
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/api/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // 记住我功能
            .rememberMe(remember -> remember
                .key("uniqueAndSecret")
                .tokenValiditySeconds(86400)  # 24小时
            )
            
            // 登出配置
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
            )
            
            // 安全头配置
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data: https:; font-src 'self' https://cdn.jsdelivr.net;")
                )
                .frameOptions(frame -> frame
                    .sameOrigin()
                )
                .xssProtection(xss -> xss
                    .enable(true)
                    .block(true)
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );
        
        return http.build();
    }
}
```

### 6.2 API安全

**API网关安全配置**：

```yaml
# API网关配置
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userService
                fallbackUri: forward:/fallback/user
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
            - name: JwtAuthentication
              args:
                secret: ${JWT_SECRET}
                issuer: AI-Ready-Test
            - name: AuditLog
              args:
                enabled: true
                log-request: true
                log-response: true
```

**API速率限制**：

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
    
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .defaultIfEmpty("anonymous");
    }
}
```

### 6.3 输入验证与输出编码

**输入验证配置**：

```java
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    
    @PostMapping
    public ResponseEntity<User> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        // 自动验证请求体
        User user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @PathVariable @Min(1) Long id) {
        // 路径参数验证
        User user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam @Size(min = 1, max = 50) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // 查询参数验证
        Page<User> users = userService.searchUsers(keyword, page, size);
        return ResponseEntity.ok(users);
    }
}

// 请求体验证
@Data
public class CreateUserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "密码必须包含大小写字母、数字和特殊字符")
    private String password;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

**输出编码配置**：

```java
@Configuration
public class OutputEncodingConfig {
    
    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setWriteAcceptCharset(false);
        return converter;
    }
    
    @Bean
    public FilterRegistrationBean<XSSFilter> xssFilter() {
        FilterRegistrationBean<XSSFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new XSSFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}

// XSS过滤器
public class XSSFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 设置响应头防止XSS
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // 包装请求进行XSS过滤
        XSSRequestWrapper wrappedRequest = new XSSRequestWrapper(httpRequest);
        chain.doFilter(wrappedRequest, response);
    }
}

// XSS请求包装器
public class XSSRequestWrapper extends HttpServletRequestWrapper {
    
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src[\r\n]*=[\r\n]*\\'(.*?)\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };
    
    public XSSRequestWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
    }
    
    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = stripXSS(values[i]);
        }
        return encodedValues;
    }
    
    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return stripXSS(value);
    }
    
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return stripXSS(value);
    }
    
    private String stripXSS(String value) {
        if (value != null) {
            // 转义HTML特殊字符
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
            value = value.replaceAll("'", "&#39;");
            value = value.replaceAll("\"", "&quot;");
            
            // 移除XSS模式
            for (Pattern pattern : XSS_PATTERNS) {
                value = pattern.matcher(value).replaceAll("");
            }
        }
        return value;
    }
}
```

---

## 7. 监控与审计

### 7.1 安全日志配置

**日志配置**：

```yaml
# logback-security.xml
<configuration>
    <!-- 安全审计日志 -->
    <appender name="SECURITY_AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security/security-audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security/security-audit-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>90</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 认证授权日志 -->
    <appender name="AUTH_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security/auth.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security/auth-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>90</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>50MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} | %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 安全事件日志器 -->
    <logger name="SECURITY_AUDIT_LOGGER" level="INFO" additivity="false">
        <appender-ref ref="SECURITY_AUDIT"/>
    </logger>
    
    <logger name="AUTH_LOGGER" level="INFO" additivity="false">
        <appender-ref ref="AUTH_LOG"/>
    </logger>
</configuration>
```

**审计事件记录**：

```java
@Component
public class SecurityAuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT_LOGGER");
    private static final Logger authLogger = LoggerFactory.getLogger("AUTH_LOGGER");
    
    public void logAuthenticationSuccess(String username, String ip, String userAgent) {
        auditLogger.info("AUTH_SUCCESS | username={} | ip={} | userAgent={}", 
            username, ip, userAgent);
    }
    
    public void logAuthenticationFailure(String username, String ip, String reason) {
        auditLogger.warn("AUTH_FAILURE | username={} | ip={} | reason={}", 
            username, ip, reason);
    }
    
    public void logAuthorizationFailure(String username, String resource, String action) {
        auditLogger.warn("AUTHZ_FAILURE | username={} | resource={} | action={}", 
            username, resource, action);
    }
    
    public void logSensitiveOperation(String username, String operation, String details) {
        auditLogger.info("SENSITIVE_OPERATION | username={} | operation={} | details={}", 
            username, operation, details);
    }
    
    public void logSecurityEvent(String eventType, String severity, String details) {
        switch (severity) {
            case "CRITICAL":
                auditLogger.error("SECURITY_EVENT | type={} | severity={} | {}", 
                    eventType, severity, details);
                break;
            case "HIGH":
                auditLogger.warn("SECURITY_EVENT | type={} | severity={} | {}", 
                    eventType, severity, details);
                break;
            default:
                auditLogger.info("SECURITY_EVENT | type={} | severity={} | {}", 
                    eventType, severity, details);
        }
    }
}
```

### 7.2 安全监控

**Prometheus安全指标**：

```yaml
# prometheus/security-alerts.yml
groups:
  - name: security_alerts
    rules:
      # 认证失败告警
      - alert: HighAuthFailureRate
        expr: rate(auth_failures_total[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "高认证失败率"
          description: "过去5分钟认证失败次数超过10次，可能存在暴力破解攻击"
      
      # 异常访问模式
      - alert: UnusualAccessPattern
        expr: rate(http_requests_total{status=~"4..|5.."}[5m]) > 100
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "异常访问模式"
          description: "HTTP错误率异常升高，可能存在扫描或攻击行为"
      
      # 权限提升尝试
      - alert: PrivilegeEscalationAttempt
        expr: authz_failures_total{reason="INSUFFICIENT_PRIVILEGES"} > 5
        for: 5m
        labels:
          severity: high
        annotations:
          summary: "权限提升尝试"
          description: "检测到多次权限提升尝试"
      
      # 敏感操作监控
      - alert: SensitiveOperationRate
        expr: rate(sensitive_operations_total[10m]) > 20
        for: 2m
        labels:
          severity: medium
        annotations:
          summary: "敏感操作频率异常"
          description: "敏感操作频率异常升高"
```

**Grafana安全仪表盘**：

```json
{
  "dashboard": {
    "title": "安全监控仪表盘",
    "panels": [
      {
        "title": "认证成功/失败率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(auth_success_total[5m])",
            "legendFormat": "认证成功"
          },
          {
            "expr": "rate(auth_failures_total[5m])",
            "legendFormat": "认证失败"
          }
        ]
      },
      {
        "title": "权限检查结果",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(authz_results_total) by (result)"
          }
        ]
      },
      {
        "title": "安全事件统计",
