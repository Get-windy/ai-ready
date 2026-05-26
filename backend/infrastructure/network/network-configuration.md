# 测试环境网络配置文档

## 1. 网络架构概述

### 1.1 网络拓扑
```
┌─────────────────────────────────────────────────────────┐
│                   测试环境网络架构                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Web层     │    │  应用层     │    │  数据层     │ │
│  │ (10.0.1.0/24)│    │(10.0.2.0/24)│    │(10.0.3.0/24)│ │
│  │             │    │             │    │             │ │
│  │ • Nginx     │    │ • 用户服务  │    │ • PostgreSQL│ │
│  │ • HAProxy   │    │ • 订单服务  │    │ • Redis     │ │
│  │ • API网关   │    │ • 库存服务  │    │ • RabbitMQ  │ │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘ │
│         │                  │                  │        │
│  ┌──────┴──────────────────┴──────────────────┴──────┐ │
│  │                管理网络 (10.0.0.0/24)              │ │
│  │                                                    │ │
│  │ • 监控服务 (Prometheus, Grafana)                  │ │
│  │ • 配置中心 (Nacos)                                │ │
│  │ • 服务发现 (Consul)                               │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 1.2 IP地址规划
| 网络段 | CIDR | 用途 | 网关 | DNS服务器 |
|--------|------|------|------|-----------|
| 管理网络 | 10.0.0.0/24 | 监控、配置、服务发现 | 10.0.0.1 | 10.0.0.2 |
| Web层网络 | 10.0.1.0/24 | 负载均衡、API网关 | 10.0.1.1 | 10.0.0.2 |
| 应用层网络 | 10.0.2.0/24 | 业务微服务 | 10.0.2.1 | 10.0.0.2 |
| 数据层网络 | 10.0.3.0/24 | 数据库、缓存、消息队列 | 10.0.3.1 | 10.0.0.2 |

## 2. VLAN配置

### 2.1 VLAN分配
| VLAN ID | 名称 | 网络段 | 用途 |
|---------|------|--------|------|
| 100 | mgmt-vlan | 10.0.0.0/24 | 管理网络 |
| 101 | web-vlan | 10.0.1.0/24 | Web层服务 |
| 102 | app-vlan | 10.0.2.0/24 | 应用层服务 |
| 103 | data-vlan | 10.0.3.0/24 | 数据层服务 |

### 2.2 VLAN间路由策略
- 管理网络可以访问所有网络
- Web层可以访问应用层和数据层
- 应用层可以访问数据层
- 数据层仅允许被应用层访问

## 3. 防火墙规则

### 3.1 入站规则
| 源网络 | 目标网络 | 端口 | 协议 | 动作 | 说明 |
|--------|----------|------|------|------|------|
| 0.0.0.0/0 | 10.0.1.0/24 | 80,443 | TCP | 允许 | Web访问 |
| 10.0.0.0/24 | 10.0.0.0/24 | 全部 | 全部 | 允许 | 管理网络内部通信 |
| 10.0.1.0/24 | 10.0.2.0/24 | 8080-8090 | TCP | 允许 | Web到应用层 |
| 10.0.2.0/24 | 10.0.3.0/24 | 5432,6379,5672 | TCP | 允许 | 应用到数据层 |

### 3.2 出站规则
| 源网络 | 目标网络 | 端口 | 协议 | 动作 | 说明 |
|--------|----------|------|------|------|------|
| 10.0.0.0/24 | 0.0.0.0/0 | 53 | TCP/UDP | 允许 | DNS解析 |
| 10.0.0.0/24 | 0.0.0.0/0 | 443 | TCP | 允许 | HTTPS更新 |

## 4. 负载均衡器配置

### 4.1 Nginx配置
```nginx
# I:\AI-Ready\infra\network\nginx.conf
upstream user_service {
    server 10.0.2.10:8080;
    server 10.0.2.11:8080;
    server 10.0.2.12:8080;
}

upstream order_service {
    server 10.0.2.20:8081;
    server 10.0.2.21:8081;
    server 10.0.2.22:8081;
}

server {
    listen 80;
    server_name test-api.ai-ready.local;
    
    location /api/user/ {
        proxy_pass http://user_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /api/order/ {
        proxy_pass http://order_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 4.2 HAProxy配置
```haproxy
# I:\AI-Ready\infra\network\haproxy.cfg
global
    log /dev/log local0
    maxconn 4000
    user haproxy
    group haproxy

defaults
    log global
    mode http
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms

frontend http_front
    bind *:80
    default_backend http_back

backend http_back
    balance roundrobin
    server web1 10.0.1.10:80 check
    server web2 10.0.1.11:80 check
    server web3 10.0.1.12:80 check
```

## 5. DNS配置

### 5.1 内部域名解析
| 域名 | IP地址 | 类型 | 说明 |
|------|--------|------|------|
| test.ai-ready.local | 10.0.1.100 | A | 测试环境入口 |
| test-api.ai-ready.local | 10.0.1.101 | A | API网关 |
| test-db.ai-ready.local | 10.0.3.10 | A | 数据库主节点 |
| test-redis.ai-ready.local | 10.0.3.20 | A | Redis缓存 |
| test-monitor.ai-ready.local | 10.0.0.10 | A | 监控面板 |

### 5.2 DNS服务器配置
```bind
# I:\AI-Ready\infra\network\named.conf.local
zone "ai-ready.local" {
    type master;
    file "/etc/bind/db.ai-ready.local";
    allow-transfer { 10.0.0.2; };
};
```

## 6. 网络验证脚本

### 6.1 网络连通性测试
```bash
#!/bin/bash
# I:\AI-Ready\infra\network\validate-network.sh

echo "=== 网络连通性测试 ==="

# 测试网关连通性
echo "1. 测试网关连通性..."
ping -c 3 10.0.0.1 && echo "✓ 管理网关正常" || echo "✗ 管理网关异常"
ping -c 3 10.0.1.1 && echo "✓ Web网关正常" || echo "✗ Web网关异常"
ping -c 3 10.0.2.1 && echo "✓ 应用网关正常" || echo "✗ 应用网关异常"
ping -c 3 10.0.3.1 && echo "✓ 数据网关正常" || echo "✗ 数据网关异常"

# 测试DNS解析
echo "2. 测试DNS解析..."
nslookup test.ai-ready.local 10.0.0.2 && echo "✓ DNS解析正常" || echo "✗ DNS解析异常"

# 测试服务端口
echo "3. 测试服务端口..."
nc -zv 10.0.1.100 80 && echo "✓ Web服务端口正常" || echo "✗ Web服务端口异常"
nc -zv 10.0.2.10 8080 && echo "✓ 用户服务端口正常" || echo "✗ 用户服务端口异常"
nc -zv 10.0.3.10 5432 && echo "✓ 数据库端口正常" || echo "✗ 数据库端口异常"

echo "=== 网络测试完成 ==="
```

## 7. 部署说明

### 7.1 Docker网络配置
```yaml
# I:\AI-Ready\infra\network\docker-network.yml
version: '3.8'

networks:
  mgmt-net:
    driver: bridge
    ipam:
      config:
        - subnet: 10.0.0.0/24
  web-net:
    driver: bridge
    ipam:
      config:
        - subnet: 10.0.1.0/24
  app-net:
    driver: bridge
    ipam:
      config:
        - subnet: 10.0.2.0/24
  data-net:
    driver: bridge
    ipam:
      config:
        - subnet: 10.0.3.0/24
```

### 7.2 部署步骤
1. 创建Docker网络：`docker network create --subnet=10.0.0.0/24 mgmt-net`
2. 配置防火墙规则
3. 部署负载均衡器
4. 配置DNS服务器
5. 运行网络验证脚本

---

**文档版本**: 1.0  
**创建时间**: 2026-04-27  
**最后更新**: 2026-04-27  
**负责人**: team-member