# 测试环境服务发现与配置管理文档

## 1. 服务发现架构概述

### 1.1 整体架构
```
┌─────────────────────────────────────────────────────────┐
│              服务发现与配置管理架构                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Consul    │    │   Nacos     │    │   Vault     │ │
│  │  集群       │    │  配置中心    │    │  密钥管理   │ │
│  │             │    │             │    │             │ │
│  │ • 服务注册  │    │ • 配置管理  │    │ • 密钥存储  │ │
│  │ • 健康检查  │    │ • 配置下发  │    │ • 动态凭证  │ │
│  │ • DNS查询   │    │ • 命名空间  │    │ • 访问控制  │ │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘ │
│         │                  │                  │        │
│  ┌──────┴──────────────────┴──────────────────┴──────┐ │
│  │                服务网格层                          │ │
│  │ • Envoy代理                                      │ │
│  │ • 流量管理                                       │ │
│  │ • 熔断限流                                       │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 1.2 组件部署规划
| 组件 | 实例数 | 网络地址 | 端口 | 功能 |
|------|--------|----------|------|------|
| Consul Server | 3 | 10.0.0.20-22 | 8500, 8600 | 服务发现、健康检查 |
| Consul Client | 每个服务节点 | 动态分配 | 8500 | 本地代理 |
| Nacos Server | 2 | 10.0.0.30-31 | 8848 | 配置中心 |
| Vault Server | 1 | 10.0.0.40 | 8200 | 密钥管理 |
| Envoy Proxy | 每个服务 | 动态分配 | 15000 | 服务网格 |

## 2. Consul服务发现配置

### 2.1 Consul服务器配置
```hcl
# I:\AI-Ready\infra\service-discovery\consul-server.hcl
datacenter = "ai-ready-test"
data_dir = "/opt/consul/data"
node_name = "consul-server-1"
server = true
bootstrap_expect = 3
bind_addr = "10.0.0.20"
client_addr = "0.0.0.0"
ui = true

# 集群通信
retry_join = ["10.0.0.21", "10.0.0.22"]

# 性能优化
performance {
  raft_multiplier = 1
}

# 服务定义示例
service {
  name = "user-service"
  id = "user-service-1"
  address = "10.0.2.10"
  port = 8080
  
  check {
    id = "api-health"
    name = "HTTP API health check"
    http = "http://10.0.2.10:8080/health"
    interval = "10s"
    timeout = "5s"
  }
}
```

### 2.2 Consul客户端配置
```hcl
# I:\AI-Ready\infra\service-discovery\consul-client.hcl
datacenter = "ai-ready-test"
data_dir = "/opt/consul/data"
node_name = "user-service-1"
bind_addr = "0.0.0.0"
client_addr = "0.0.0.0"

# 连接到Consul服务器
retry_join = ["10.0.0.20"]

# 服务注册
service {
  name = "user-service"
  id = "user-service-${HOSTNAME}"
  address = "${HOST_IP}"
  port = 8080
  tags = ["v1.0", "http"]
  
  check {
    http = "http://${HOST_IP}:8080/health"
    interval = "10s"
    timeout = "5s"
  }
}
```

### 2.3 Docker Compose配置
```yaml
# I:\AI-Ready\infra\service-discovery\consul-docker-compose.yml
version: '3.8'

services:
  consul-server1:
    image: consul:1.15
    command: agent -server -bootstrap-expect=3 -node=server1 -client=0.0.0.0 -bind=10.0.0.20
    volumes:
      - consul_data1:/consul/data
    ports:
      - "8500:8500"
      - "8600:8600/tcp"
      - "8600:8600/udp"
    networks:
      mgmt-net:
        ipv4_address: 10.0.0.20

  consul-server2:
    image: consul:1.15
    command: agent -server -bootstrap-expect=3 -node=server2 -client=0.0.0.0 -bind=10.0.0.21 -join=10.0.0.20
    volumes:
      - consul_data2:/consul/data
    networks:
      mgmt-net:
        ipv4_address: 10.0.0.21

  consul-server3:
    image: consul:1.15
    command: agent -server -bootstrap-expect=3 -node=server3 -client=0.0.0.0 -bind=10.0.0.22 -join=10.0.0.20
    volumes:
      - consul_data3:/consul/data
    networks:
      mgmt-net:
        ipv4_address: 10.0.0.22

volumes:
  consul_data1:
  consul_data2:
  consul_data3:

networks:
  mgmt-net:
    external: true
    name: mgmt-net
```

## 3. Nacos配置中心

### 3.1 Nacos服务器配置
```properties
# I:\AI-Ready\infra\service-discovery\nacos-application.properties
server.port=8848
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://10.0.3.10:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true
db.user=test_user
db.password=test_password123

# 集群配置
nacos.core.cluster.member.list=10.0.0.30:8848,10.0.0.31:8848

# 命名空间配置
nacos.namespace.default=test
```

### 3.2 配置管理示例
```yaml
# I:\AI-Ready\infra\service-discovery\nacos-config-example.yml
# Data ID: user-service-dev.yml
# Group: DEFAULT_GROUP
# Namespace: test

server:
  port: 8080
  servlet:
    context-path: /api/user

spring:
  datasource:
    url: jdbc:postgresql://test-db.ai-ready.local:5432/ai_ready_test
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  redis:
    host: test-redis.ai-ready.local
    port: 6379
    password: ${REDIS_PASSWORD}

logging:
  level:
    cn.aiedge: DEBUG
  file:
    name: /var/log/user-service.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 3.3 配置热更新
```java
// Spring Cloud Alibaba Nacos配置热更新示例
@RestController
@RefreshScope
public class ConfigController {
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @GetMapping("/config")
    public String getConfig() {
        return "Context Path: " + contextPath;
    }
}
```

## 4. Vault密钥管理

### 4.1 Vault服务器配置
```hcl
# I:\AI-Ready\infra\service-discovery\vault-config.hcl
storage "file" {
  path = "/vault/data"
}

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_disable = 1  # 测试环境禁用TLS
}

api_addr = "http://10.0.0.40:8200"
ui = true
```

### 4.2 密钥存储示例
```bash
#!/bin/bash
# I:\AI-Ready\infra\service-discovery\vault-setup.sh

# 初始化Vault
vault operator init -key-shares=5 -key-threshold=3

# 解封Vault（使用3个密钥）
vault operator unseal
vault operator unseal
vault operator unseal

# 启用KV secrets引擎
vault secrets enable -path=secret kv-v2

# 存储数据库密码
vault kv put secret/database/postgres \
  username=test_user \
  password=test_password123 \
  host=test-db.ai-ready.local \
  port=5432

# 存储Redis密码
vault kv put secret/cache/redis \
  password=redis_test_password \
  host=test-redis.ai-ready.local \
  port=6379

# 创建应用策略
vault policy write user-service - <<EOF
path "secret/data/database/postgres" {
  capabilities = ["read"]
}

path "secret/data/cache/redis" {
  capabilities = ["read"]
}
EOF

# 启用AppRole认证
vault auth enable approle

# 创建AppRole
vault write auth/approle/role/user-service \
  secret_id_ttl=24h \
  token_num_uses=10 \
  token_ttl=20m \
  token_max_ttl=30m \
  policies="user-service"
```

### 4.3 应用集成示例
```java
// Spring Cloud Vault配置
@Configuration
public class VaultConfig {
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(
            URI.create("http://10.0.0.40:8200")
        );
        TokenAuthentication auth = new TokenAuthentication("hvs.xxxxxxxx");
        return new VaultTemplate(endpoint, auth);
    }
}
```

## 5. 服务网格配置（Envoy）

### 5.1 Envoy代理配置
```yaml
# I:\AI-Ready\infra\service-discovery\envoy-config.yaml
static_resources:
  listeners:
  - name: user_service_listener
    address:
      socket_address:
        address: 0.0.0.0
        port_value: 15000
    filter_chains:
    - filters:
      - name: envoy.filters.network.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: user_service
              domains: ["*"]
              routes:
              - match:
                  prefix: "/"
                route:
                  cluster: user_service_cluster
          http_filters:
          - name: envoy.filters.http.router
            typed_config:
              "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router

  clusters:
  - name: user_service_cluster
    connect_timeout: 0.25s
    type: strict_dns
    lb_policy: round_robin
    load_assignment:
      cluster_name: user_service_cluster
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: user-service.service.consul
                port_value: 8080
```

### 5.2 健康检查配置
```yaml
health_checks:
- timeout: 5s
  interval: 10s
  unhealthy_threshold: 3
  healthy_threshold: 2
  http_health_check:
    path: "/health"
    expected_statuses:
      start: 200
      end: 299
```

## 6. 服务发现验证

### 6.1 服务注册验证脚本
```bash
#!/bin/bash
# I:\AI-Ready\infra\service-discovery\validate-service-discovery.sh

echo "=== 服务发现验证 ==="

# 验证Consul健康状态
echo "1. 验证Consul集群状态..."
consul members
consul operator raft list-peers

# 验证服务注册
echo "2. 验证服务注册..."
curl -s http://10.0.0.20:8500/v1/catalog/services | jq .

# 验证服务健康检查
echo "3. 验证服务健康检查..."
curl -s http://10.0.0.20:8500/v1/health/service/user-service | jq .

# 验证DNS解析
echo "4. 验证DNS解析..."
dig @10.0.0.20 -p 8600 user-service.service.consul

# 验证Nacos配置中心
echo "5. 验证Nacos配置中心..."
curl -s http://10.0.0.30:8848/nacos/v1/cs/configs?dataId=user-service-dev.yml&group=DEFAULT_GROUP

# 验证Vault密钥管理
echo "6. 验证Vault密钥管理..."
vault status
vault kv get secret/database/postgres

echo "=== 服务发现验证完成 ==="
```

### 6.2 配置下发验证
```bash
#!/bin/bash
# I:\AI-Ready\infra\service-discovery\validate-config-center.sh

echo "=== 配置中心验证 ==="

# 发布配置
echo "1. 发布测试配置..."
curl -X POST "http://10.0.0.30:8848/nacos/v1/cs/configs" \
  -d "dataId=test-config.yml&group=DEFAULT_GROUP&content=test: value"

# 获取配置
echo "2. 获取测试配置..."
curl -s "http://10.0.0.30:8848/nacos/v1/cs/configs?dataId=test-config.yml&group=DEFAULT_GROUP"

# 监听配置变化
echo "3. 测试配置监听..."
curl -s "http://10.0.0.30:8848/nacos/v1/cs/configs/listener" \
  -d "Listening-Configs=test-config.yml%02DEFAULT_GROUP%02123456789012345678%01"

echo "=== 配置中心验证完成 ==="
```

## 7. 监控与告警

### 7.1 Consul监控
```yaml
# I:\AI-Ready\infra\service-discovery\consul-monitoring.yml
scrape_configs:
  - job_name: 'consul'
    consul_sd_configs:
      - server: '10.0.0.20:8500'
    relabel_configs:
      - source_labels: [__meta_consul_service]
        target_label: job
      - source_labels: [__meta_consul_service_id]
        target_label: instance
```

### 7.2 关键指标监控
- 服务注册数量
- 健康检查成功率
- 配置更新频率
- 密钥访问次数
- 服务调用延迟

## 8. 部署说明

### 8.1 部署顺序
1. 部署Consul集群（3节点）
2. 部署Nacos配置中心（2节点）
3. 部署Vault密钥管理
4. 配置Envoy代理
5. 集成应用服务
6. 配置监控告警

### 8.2 验证清单
- [ ] Consul集群状态正常
- [ ] 服务能够成功注册
- [ ] 健康检查正常工作
- [ ] 配置能够动态下发
- [ ] 密钥能够安全访问
- [ ] 服务发现能够正常解析
- [ ] 监控数据能够正常采集

---

**文档版本**: 1.0  
**创建时间**: 2026-04-27  
**最后更新**: 2026-04-27  
**负责人**: team-member