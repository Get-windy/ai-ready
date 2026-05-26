# 安全配置修复示例

## 1. JWT密钥管理修复

### 问题
JWT密钥硬编码在配置文件中：
```yaml
jwt:
  secret: [REDACTED]  # 硬编码密钥
```

### 修复方案
使用环境变量管理密钥：

#### 1.1 更新application.yml
```yaml
jwt:
  secret: ${JWT_SECRET:default_secure_secret_please_change_in_production}
  access-token-expiration: 7200
  refresh-token-expiration: 604800
```

#### 1.2 创建环境变量文件 (.env.test)
```bash
# JWT配置
JWT_SECRET=your_secure_random_jwt_secret_key_here_32_chars_min

# 数据库配置
DB_PASSWORD=StrongP@ssw0rd!2024
REDIS_PASSWORD=RedisSecureP@ss123

# API密钥
API_KEY=your_api_key_here
```

#### 1.3 更新docker-compose配置
```yaml
services:
  ai-service-test:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    env_file:
      - .env.test
```

## 2. Redis安全加固

### 问题
Redis无密码访问：
```yaml
redis-test:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  # 无密码配置
```

### 修复方案
#### 2.1 更新docker-compose.yml
```yaml
redis-test:
  image: redis:7-alpine
  container_name: redis-test
  ports:
    - "6379:6379"
  command: >
    redis-server
    --requirepass ${REDIS_PASSWORD}
    --appendonly yes
    --appendfsync everysec
    --save 900 1
    --save 300 10
    --save 60 10000
  environment:
    - REDIS_PASSWORD=${REDIS_PASSWORD}
  volumes:
    - redis-data-test:/data
    - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
  networks:
    - ai-test-network
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
    interval: 30s
    timeout: 10s
    retries: 5
```

#### 2.2 创建Redis配置文件 (redis.conf)
```conf
# 安全配置
requirepass ${REDIS_PASSWORD}
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command CONFIG ""

# 网络配置
bind 127.0.0.1
protected-mode yes
port 6379

# 内存配置
maxmemory 1gb
maxmemory-policy allkeys-lru

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 日志配置
loglevel notice
logfile /data/redis.log
```

## 3. 数据库安全加固

### 问题
使用弱密码：
```yaml
postgres-test:
  environment:
    POSTGRES_PASSWORD: test_password_123  # 弱密码
```

### 修复方案
#### 3.1 更新docker-compose.yml
```yaml
postgres-test:
  image: postgres:15-alpine
  container_name: postgres-test
  environment:
    POSTGRES_DB: ai_ready_test
    POSTGRES_USER: ai_ready_user
    POSTGRES_PASSWORD: ${DB_PASSWORD}
    POSTGRES_INITDB_ARGS: "--auth-host=scram-sha-256 --auth-local=scram-sha-256"
  ports:
    - "5432:5432"
  volumes:
    - postgres-data-test:/var/lib/postgresql/data
    - ./database/init-test.sql:/docker-entrypoint-initdb.d/init.sql
    - ./database/postgresql.conf:/var/lib/postgresql/data/postgresql.conf
    - ./database/pg_hba.conf:/var/lib/postgresql/data/pg_hba.conf
  networks:
    - ai-test-network
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ai_ready_user -d ai_ready_test"]
    interval: 30s
    timeout: 10s
    retries: 5
```

#### 3.2 创建PostgreSQL配置文件
**postgresql.conf:**
```conf
# 连接安全
listen_addresses = 'localhost'
port = 5432
max_connections = 100
ssl = on
ssl_cert_file = '/var/lib/postgresql/data/server.crt'
ssl_key_file = '/var/lib/postgresql/data/server.key'

# 认证
password_encryption = scram-sha-256

# 日志
log_destination = 'stderr'
logging_collector = on
log_directory = '/var/lib/postgresql/data/log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 10MB
log_min_duration_statement = 1000

# 审计
log_connections = on
log_disconnections = on
log_statement = 'ddl'
```

**pg_hba.conf:**
```conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             all                                     scram-sha-256
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
hostssl all             all             0.0.0.0/0               scram-sha-256
```

## 4. 应用安全头配置

### 4.1 Spring Boot安全配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .headers()
                .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:")
                .and()
                .frameOptions().deny()
                .xssProtection().block(true)
                .and()
            .csrf().disable() // 如果是API服务可以禁用，但需要其他防护
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
                .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

### 4.2 安全响应头配置
```yaml
# 在application.yml中添加
server:
  compression:
    enabled: true
  servlet:
    session:
      timeout: 30m
      cookie:
        http-only: true
        secure: true
        same-site: strict

# 自定义安全头
security:
  headers:
    content-security-policy: "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';"
    x-frame-options: DENY
    x-content-type-options: nosniff
    x-xss-protection: "1; mode=block"
    referrer-policy: strict-origin-when-cross-origin
```

## 5. 网络加固配置

### 5.1 防火墙规则示例
```bash
# 只允许必要的端口
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 允许SSH
sudo ufw allow 22/tcp

# 允许HTTP/HTTPS（如果需要）
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 允许应用端口（内部访问）
sudo ufw allow from 192.168.1.0/24 to any port 8080
sudo ufw allow from 192.168.1.0/24 to any port 5432
sudo ufw allow from 192.168.1.0/24 to any port 6379

# 启用防火墙
sudo ufw enable
```

### 5.2 Docker网络配置
```yaml
networks:
  ai-test-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
    # 可选：启用网络加密
    # driver_opts:
    #   com.docker.network.driver.mtu: "1500"
    #   com.docker.network.enable_ipv6: "false"
```

## 6. 监控和日志配置

### 6.1 安全日志配置
```yaml
logging:
  level:
    root: WARN
    org.springframework.security: DEBUG
    com.aiready: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n%throwable"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n%throwable"
  file:
    name: logs/application-security.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 100MB
```

### 6.2 Prometheus安全监控
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "security_rules.yml"

scrape_configs:
  - job_name: 'ai-service-security'
    static_configs:
      - targets: ['ai-service-test:8000']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'postgres-security'
    static_configs:
      - targets: ['postgres-test:9187']  # PostgreSQL exporter
    scrape_interval: 30s

  - job_name: 'redis-security'
    static_configs:
      - targets: ['redis-test:9121']  # Redis exporter
    scrape_interval: 30s
```

## 7. 自动化安全检查脚本

### 7.1 安全配置检查脚本
```bash
#!/bin/bash
# security-check.sh

echo "=== 安全配置检查 ==="

# 检查环境变量
echo "1. 检查环境变量..."
if [ -z "$JWT_SECRET" ]; then
    echo "❌ JWT_SECRET未设置"
else
    echo "✅ JWT_SECRET已设置"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ DB_PASSWORD未设置"
else
    echo "✅ DB_PASSWORD已设置"
fi

# 检查端口暴露
echo "2. 检查端口暴露..."
netstat -tulpn | grep LISTEN

# 检查文件权限
echo "3. 检查文件权限..."
find . -name "*.env*" -type f -exec ls -la {} \;
find . -name "*.yml" -type f -exec grep -l "password\|secret\|token" {} \;

# 检查依赖漏洞
echo "4. 检查依赖漏洞..."
if command -v npm &> /dev/null; then
    npm audit
fi

if command -v mvn &> /dev/null; then
    mvn dependency-check:check
fi

echo "=== 检查完成 ==="
```

## 8. 紧急响应检查清单

### 8.1 安全事件响应
```markdown
# 安全事件响应检查清单

## 发现安全事件时
1. [ ] 立即隔离受影响系统
2. [ ] 通知安全团队和负责人
3. [ ] 收集证据和日志
4. [ ] 评估影响范围
5. [ ] 制定修复计划

## 恢复步骤
1. [ ] 修复安全漏洞
2. [ ] 重置受影响凭证
3. [ ] 更新安全配置
4. [ ] 验证修复效果
5. [ ] 监控异常活动

## 事后分析
1. [ ] 根本原因分析
2. [ ] 改进措施制定
3. [ ] 更新安全策略
4. [ ] 团队培训
5. [ ] 文档更新
```

## 总结

这些配置修复示例提供了从高风险问题到最佳实践的完整解决方案。建议按照以下优先级实施：

1. **立即实施**: JWT密钥管理、Redis密码、数据库强密码
2. **短期实施**: 安全头配置、网络加固、监控增强
3. **长期实施**: 自动化安全、零信任架构、合规建设

记住：安全是一个持续的过程，而不是一次性的任务。定期审查和更新安全配置至关重要。