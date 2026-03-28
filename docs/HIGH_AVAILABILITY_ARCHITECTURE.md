# 智企连·AI-Ready 高可用架构设计

**版本**: v1.0  
**日期**: 2026-03-27  
**作者**: devops-engineer  
**项目**: 智企连·AI-Ready

---

## 执行摘要

本文档定义智企连·AI-Ready 项目的高可用架构设计，包括负载均衡、故障转移和性能优化三大核心模块。目标是实现：

- **可用性目标**: 99.9% (年度停机时间 < 8.76小时)
- **RTO (恢复时间目标)**: < 15分钟
- **RPO (恢复点目标)**: < 5分钟
- **性能目标**: API响应时间 < 200ms (P95)

---

## 1. 架构总览

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        智企连·AI-Ready 高可用架构                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                          ┌─────────────────┐                                │
│                          │   CDN / WAF     │                                │
│                          │  (阿里云/腾讯云) │                                │
│                          └────────┬────────┘                                │
│                                   │                                         │
│                    ┌──────────────▼──────────────┐                          │
│                    │       负载均衡层            │                          │
│                    │  ┌───────┐     ┌───────┐   │                          │
│                    │  │NGINX  │────▶│NGINX  │   │                          │
│                    │  │(主)   │     │(备)   │   │                          │
│                    │  └───────┘     └───────┘   │                          │
│                    │     Keepalived + VRRP      │                          │
│                    └──────────────┬──────────────┘                          │
│                                   │                                         │
│    ┌──────────────────────────────┼──────────────────────────────┐          │
│    │                              │                              │          │
│    │    ┌─────────────────────────┼─────────────────────────┐    │          │
│    │    │                        应用层                      │    │          │
│    │    │  ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐      │    │          │
│    │    │  │ API   │  │ API   │  │ API   │  │ API   │      │    │          │
│    │    │  │Node 1 │  │Node 2 │  │Node 3 │  │Node N │      │    │          │
│    │    │  └───────┘  └───────┘  └───────┘  └───────┘      │    │          │
│    │    │        Spring Boot 3.2.x + Maven                 │    │          │
│    │    └─────────────────────────┬─────────────────────────┘    │          │
│    │                              │                              │          │
│    │    ┌─────────────────────────┼─────────────────────────┐    │          │
│    │    │                       缓存层                       │    │          │
│    │    │     ┌──────────────────────────────────┐          │    │          │
│    │    │     │        Redis Cluster             │          │    │          │
│    │    │     │  ┌─────┐  ┌─────┐  ┌─────┐      │          │    │          │
│    │    │     │  │Master│  │Slave│  │Slave│     │          │    │          │
│    │    │     │  └─────┘  └─────┘  └─────┘      │          │    │          │
│    │    │     │        Redis Sentinel           │          │    │          │
│    │    │     └──────────────────────────────────┘          │    │          │
│    │    └─────────────────────────┬─────────────────────────┘    │          │
│    │                              │                              │          │
│    │    ┌─────────────────────────┼─────────────────────────┐    │          │
│    │    │                       数据层                       │    │          │
│    │    │  ┌───────────────────────────────────────┐        │    │          │
│    │    │  │         PostgreSQL Cluster            │        │    │          │
│    │    │  │  ┌─────┐     ┌─────┐     ┌─────┐     │        │    │          │
│    │    │  │  │Primary │──▶│Replica│──▶│Replica│   │        │    │          │
│    │    │  │  └─────┘     └─────┘     └─────┘     │        │    │          │
│    │    │  │       Patroni + etcd                  │        │    │          │
│    │    │  └───────────────────────────────────────┘        │    │          │
│    │    └───────────────────────────────────────────────────┘    │          │
│    │                                                             │          │
│    └─────────────────────────────────────────────────────────────┘          │
│                                                                             │
│    ┌─────────────────────────────────────────────────────────────┐          │
│    │                       监控与告警                             │          │
│    │  Prometheus + Grafana + AlertManager + Loki                 │          │
│    └─────────────────────────────────────────────────────────────┘          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 架构层级

| 层级 | 组件 | 可用性 | 说明 |
|------|------|--------|------|
| 接入层 | CDN/WAF | 99.99% | DDoS防护、静态资源加速 |
| 负载均衡层 | NGINX + Keepalived | 99.99% | 双机热备、健康检查 |
| 应用层 | Spring Boot 集群 | 99.95% | 多实例部署、自动伸缩 |
| 缓存层 | Redis Cluster | 99.99% | 主从复制、哨兵模式 |
| 数据层 | PostgreSQL Cluster | 99.99% | 主从复制、自动故障转移 |

---

## 2. 负载均衡设计

### 2.1 NGINX 负载均衡配置

#### 主配置文件

```nginx
# /etc/nginx/nginx.conf

user nginx;
worker_processes auto;
worker_rlimit_nofile 65535;

error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                     '$status $body_bytes_sent "$http_referer" '
                     '"$http_user_agent" "$http_x_forwarded_for" '
                     'rt=$request_time uct="$upstream_connect_time" '
                     'uht="$upstream_header_time" urt="$upstream_response_time"';

    access_log /var/log/nginx/access.log main;

    # 性能优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript 
               application/xml application/xml+rss text/javascript application/x-javascript;

    # 上游服务器定义
    upstream ai_ready_backend {
        least_conn;  # 最少连接算法
        
        server 10.0.1.101:8080 weight=5 max_fails=3 fail_timeout=30s;
        server 10.0.1.102:8080 weight=5 max_fails=3 fail_timeout=30s;
        server 10.0.1.103:8080 weight=5 max_fails=3 fail_timeout=30s;
        
        # 备用服务器
        server 10.0.1.104:8080 backup;
        
        # 长连接配置
        keepalive 32;
        keepalive_requests 1000;
        keepalive_timeout 60s;
    }

    # 限流配置
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_conn_zone $binary_remote_addr zone=conn_limit:10m;

    # 服务器配置
    include /etc/nginx/conf.d/*.conf;
}
```

#### 站点配置

```nginx
# /etc/nginx/conf.d/ai-ready.conf

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name api.ai-ready.cn www.ai-ready.cn;
    
    # ACME challenge for Let's Encrypt
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS 主配置
server {
    listen 443 ssl http2;
    server_name api.ai-ready.cn;

    # SSL 配置
    ssl_certificate /etc/nginx/ssl/ai-ready.cn.crt;
    ssl_certificate_key /etc/nginx/ssl/ai-ready.cn.key;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    # 现代SSL配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Content-Security-Policy "default-src 'self'" always;

    # 日志
    access_log /var/log/nginx/ai-ready-access.log main;
    error_log /var/log/nginx/ai-ready-error.log;

    # API 代理
    location /api/ {
        # 限流
        limit_req zone=api_limit burst=200 nodelay;
        limit_conn conn_limit 50;

        # 代理配置
        proxy_pass http://ai_ready_backend/;
        proxy_http_version 1.1;
        
        # 请求头
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 长连接
        proxy_set_header Connection "";
        
        # 超时配置
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲配置
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 32k;
        proxy_busy_buffers_size 64k;
        
        # 错误处理
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
        proxy_next_upstream_tries 3;
    }

    # 健康检查端点
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    # 状态端点
    location /nginx_status {
        stub_status on;
        access_log off;
        allow 10.0.0.0/8;
        deny all;
    }
}
```

### 2.2 Keepalived 高可用配置

#### 主节点配置

```conf
# /etc/keepalived/keepalived.conf (主节点)

! Configuration File for keepalived

global_defs {
    router_id NGINX_MASTER
    script_user root
    enable_script_security
}

# 健康检查脚本
vrrp_script check_nginx {
    script "/etc/keepalived/check_nginx.sh"
    interval 2
    weight -20
    fall 3
    rise 2
}

# VRRP 实例
vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    
    # 认证
    authentication {
        auth_type PASS
        auth_pass ai-ready-2026
    }
    
    # VIP
    virtual_ipaddress {
        10.0.1.100/24 dev eth0 label eth0:1
    }
    
    # 健康检查
    track_script {
        check_nginx
    }
    
    # 通知脚本
    notify_master "/etc/keepalived/notify.sh master"
    notify_backup "/etc/keepalived/notify.sh backup"
    notify_fault "/etc/keepalived/notify.sh fault"
}
```

#### 备节点配置

```conf
# /etc/keepalived/keepalived.conf (备节点)

! Configuration File for keepalived

global_defs {
    router_id NGINX_BACKUP
    script_user root
    enable_script_security
}

vrrp_script check_nginx {
    script "/etc/keepalived/check_nginx.sh"
    interval 2
    weight -20
    fall 3
    rise 2
}

vrrp_instance VI_1 {
    state BACKUP
    interface eth0
    virtual_router_id 51
    priority 90
    advert_int 1
    
    authentication {
        auth_type PASS
        auth_pass ai-ready-2026
    }
    
    virtual_ipaddress {
        10.0.1.100/24 dev eth0 label eth0:1
    }
    
    track_script {
        check_nginx
    }
    
    notify_master "/etc/keepalived/notify.sh master"
    notify_backup "/etc/keepalived/notify.sh backup"
    notify_fault "/etc/keepalived/notify.sh fault"
}
```

#### 健康检查脚本

```bash
#!/bin/bash
# /etc/keepalived/check_nginx.sh

# 检查 NGINX 进程
if [ "$(pgrep nginx | wc -l)" -eq 0 ]; then
    echo "NGINX is not running"
    exit 1
fi

# 检查 NGINX 健康端点
if ! curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/health | grep -q "200"; then
    echo "NGINX health check failed"
    exit 1
fi

exit 0
```

#### 通知脚本

```bash
#!/bin/bash
# /etc/keepalived/notify.sh

STATE=$1
VIP="10.0.1.100"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

case $STATE in
    "master")
        echo "[$TIMESTAMP] Transitioning to MASTER state" >> /var/log/keepalived/notify.log
        # 通知运维团队
        curl -s -X POST "${SLACK_WEBHOOK}" \
            -H 'Content-type: application/json' \
            -d "{\"text\":\"🔴 NGINX Master 切换: $HOSTNAME 成为 MASTER (VIP: $VIP)\"}"
        ;;
    "backup")
        echo "[$TIMESTAMP] Transitioning to BACKUP state" >> /var/log/keepalived/notify.log
        ;;
    "fault")
        echo "[$TIMESTAMP] Transitioning to FAULT state" >> /var/log/keepalived/notify.log
        curl -s -X POST "${SLACK_WEBHOOK}" \
            -H 'Content-type: application/json' \
            -d "{\"text\":\"🚨 NGINX Keepalived FAULT: $HOSTNAME 进入故障状态\"}"
        ;;
esac
```

### 2.3 服务高可用设计

#### 应用集群部署

```yaml
# docker-compose.yml - 应用集群

version: '3.8'

services:
  app:
    image: ai-ready/app:${APP_VERSION:-latest}
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
        failure_action: rollback
      rollback_config:
        parallelism: 1
        delay: 10s
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
        window: 120s
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-primary:5432/proddb
      - SPRING_REDIS_HOST=redis-master
      - JAVA_OPTS=-Xms1g -Xmx1.5g -XX:+UseG1GC
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - ai-ready-network

networks:
  ai-ready-network:
    external: true
```

---

## 3. 故障转移设计

### 3.1 服务故障转移

#### 健康检查配置

```yaml
# Spring Boot Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    liveness:
      enabled: true
    readiness:
      enabled: true
    db:
      enabled: true
    redis:
      enabled: true
```

#### 健康检查端点

| 端点 | 用途 | 检查内容 |
|------|------|----------|
| `/actuator/health/liveness` | 存活探针 | JVM状态 |
| `/actuator/health/readiness` | 就绪探针 | 数据库、Redis连接 |
| `/actuator/health` | 完整健康检查 | 所有组件 |

#### Kubernetes 部署配置

```yaml
# kubernetes/deployment.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-ready-api
  labels:
    app: ai-ready-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-ready-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ai-ready-api
    spec:
      containers:
        - name: api
          image: ai-ready/api:latest
          ports:
            - containerPort: 8080
          
          # 资源限制
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          
          # 存活探针
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          
          # 就绪探针
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          
          # 环境变量
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: ai-ready-secrets
                  key: database-url
            - name: SPRING_REDIS_HOST
              value: "redis-master"
          
          # 配置挂载
          volumeMounts:
            - name: config
              mountPath: /app/config
              readOnly: true
      
      volumes:
        - name: config
          configMap:
            name: ai-ready-config

---
apiVersion: v1
kind: Service
metadata:
  name: ai-ready-api
spec:
  selector:
    app: ai-ready-api
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### 3.2 数据库故障转移

#### PostgreSQL 主从复制配置

```yaml
# docker-compose.yml - PostgreSQL Cluster

version: '3.8'

services:
  # PostgreSQL Primary
  postgres-primary:
    image: postgres:14
    container_name: ai-ready-postgres-primary
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
      PG_REP_USER: replicator
      PG_REP_PASSWORD: ${PG_REP_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_primary_data:/var/lib/postgresql/data
      - ./postgresql/primary:/etc/postgresql
    command: >
      postgres
      -c wal_level=replica
      -c max_wal_senders=3
      -c max_replication_slots=3
      -c hot_standby=on
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - postgres-network

  # PostgreSQL Replica
  postgres-replica:
    image: postgres:14
    container_name: ai-ready-postgres-replica
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
      PG_PRIMARY_HOST: postgres-primary
      PG_REP_USER: replicator
      PG_REP_PASSWORD: ${PG_REP_PASSWORD}
    ports:
      - "5433:5432"
    volumes:
      - postgres_replica_data:/var/lib/postgresql/data
    depends_on:
      - postgres-primary
    command: >
      postgres
      -c hot_standby=on
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - postgres-network

  # PgBouncer 连接池
  pgbouncer:
    image: edoburu/pgbouncer:latest
    container_name: ai-ready-pgbouncer
    environment:
      DATABASE_URL: "postgres://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres-primary:5432/${POSTGRES_DB}"
      PGBOUNCER_POOL_MODE: transaction
      PGBOUNCER_MAX_CLIENT_CONN: 500
      PGBOUNCER_DEFAULT_POOL_SIZE: 25
      PGBOUNCER_RESERVE_POOL_SIZE: 5
    ports:
      - "6432:5432"
    depends_on:
      - postgres-primary
    networks:
      - postgres-network

volumes:
  postgres_primary_data:
  postgres_replica_data:

networks:
  postgres-network:
    driver: bridge
```

#### Patroni 高可用配置

```yaml
# patroni.yml

scope: ai-ready-cluster
namespace: /db/
name: postgresql0

restapi:
  listen: 0.0.0.0:8008
  connect_address: 10.0.1.10:8008

etcd:
  hosts: 10.0.1.20:2379,10.0.1.21:2379,10.0.1.22:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        wal_level: replica
        hot_standby: "on"
        wal_keep_segments: 8
        max_wal_senders: 5
        max_replication_slots: 5
        hot_standby_feedback: "on"
        
  initdb:
    - encoding: UTF8
    - data-checksums

  pg_hba:
    - host replication replicator 127.0.0.1/32 trust
    - host replication replicator 0.0.0.0/0 md5
    - host all all 0.0.0.0/0 md5
    - local all all trust

  users:
    admin:
      password: ${ADMIN_PASSWORD}
      options:
        - createrole
        - createdb

postgresql:
  listen: 0.0.0.0:5432
  connect_address: 10.0.1.10:5432
  data_dir: /var/lib/postgresql/data
  pgpass: /tmp/pgpass
  authentication:
    replication:
      username: replicator
      password: ${REPLICATION_PASSWORD}
    superuser:
      username: postgres
      password: ${POSTGRES_PASSWORD}

tags:
  nofailover: false
  noloadbalance: false
  clonefrom: false
  nosync: false
```

### 3.3 自动恢复机制

#### 服务自动恢复

```yaml
# 自动恢复配置
recovery:
  services:
    # API 服务恢复
    - name: api-recovery
      trigger:
        alert: "ServiceDown"
        duration: "2m"
      actions:
        - type: restart
          max_attempts: 3
          cooldown: "5m"
        - type: scale_up
          replicas: 5
        - type: notify
          channels: ["slack", "email"]
          
    # 数据库恢复
    - name: database-recovery
      trigger:
        alert: "PostgreSQLDown"
        duration: "1m"
      actions:
        - type: failover
          target: replica
        - type: notify
          channels: ["slack", "phone"]
          priority: "critical"
          
    # Redis 恢复
    - name: redis-recovery
      trigger:
        alert: "RedisDown"
        duration: "1m"
      actions:
        - type: sentinel_failover
        - type: clear_cache
        - type: notify
          channels: ["slack"]
```

#### 自动恢复脚本

```python
#!/usr/bin/env python3
# auto_recovery.py

import os
import time
import logging
import requests
from dataclasses import dataclass
from typing import Optional

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class RecoveryConfig:
    max_attempts: int = 3
    cooldown_seconds: int = 300
    check_interval: int = 30

class ServiceRecovery:
    def __init__(self, service_name: str, config: RecoveryConfig):
        self.service_name = service_name
        self.config = config
        self.attempt_count = 0
        
    def check_health(self, url: str) -> bool:
        try:
            response = requests.get(url, timeout=10)
            return response.status_code == 200
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return False
            
    def restart_service(self) -> bool:
        logger.info(f"Restarting {self.service_name}...")
        result = os.system(f"docker restart {self.service_name}")
        return result == 0
        
    def notify(self, message: str, channels: list):
        webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
        if webhook_url:
            requests.post(webhook_url, json={"text": message})
            
    def recover(self, health_url: str) -> bool:
        while self.attempt_count < self.config.max_attempts:
            self.attempt_count += 1
            logger.info(f"Recovery attempt {self.attempt_count}/{self.config.max_attempts}")
            
            # 重启服务
            if self.restart_service():
                # 等待服务启动
                time.sleep(30)
                
                # 检查健康状态
                if self.check_health(health_url):
                    logger.info(f"Recovery successful for {self.service_name}")
                    self.notify(f"✅ {self.service_name} 恢复成功", ["slack"])
                    return True
                    
            # 冷却等待
            if self.attempt_count < self.config.max_attempts:
                logger.info(f"Waiting {self.config.cooldown_seconds}s before next attempt...")
                time.sleep(self.config.cooldown_seconds)
                
        logger.error(f"Recovery failed for {self.service_name}")
        self.notify(f"🚨 {self.service_name} 恢复失败，需要人工干预", ["slack", "email"])
        return False

if __name__ == "__main__":
    config = RecoveryConfig()
    recovery = ServiceRecovery("ai-ready-api", config)
    recovery.recover("http://localhost:8080/actuator/health")
```

---

## 4. 性能优化

### 4.1 缓存策略设计

#### Redis 集群配置

```yaml
# docker-compose.yml - Redis Cluster

version: '3.8'

services:
  redis-master:
    image: redis:7-alpine
    container_name: ai-ready-redis-master
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 2gb
      --maxmemory-policy allkeys-lru
      --save 900 1
      --save 300 10
      --save 60 10000
      --appendonly yes
      --appendfsync everysec
    ports:
      - "6379:6379"
    volumes:
      - redis_master_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - redis-network

  redis-replica:
    image: redis:7-alpine
    container_name: ai-ready-redis-replica
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --replicaof redis-master 6379
      --masterauth ${REDIS_PASSWORD}
      --maxmemory 2gb
      --maxmemory-policy allkeys-lru
    ports:
      - "6380:6379"
    volumes:
      - redis_replica_data:/data
    depends_on:
      - redis-master
    networks:
      - redis-network

  redis-sentinel:
    image: redis:7-alpine
    container_name: ai-ready-redis-sentinel
    command: >
      redis-sentinel /etc/redis/sentinel.conf
    ports:
      - "26379:26379"
    volumes:
      - ./redis/sentinel.conf:/etc/redis/sentinel.conf
    depends_on:
      - redis-master
      - redis-replica
    networks:
      - redis-network

volumes:
  redis_master_data:
  redis_replica_data:

networks:
  redis-network:
    driver: bridge
```

#### 缓存策略

```java
// Spring Cache 配置
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户信息缓存 - 1小时
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 配置缓存 - 24小时
        cacheConfigurations.put("configs", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 权限缓存 - 2小时
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // 热点数据缓存 - 5分钟
        cacheConfigurations.put("hotdata", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}

// 缓存使用示例
@Service
public class UserService {

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
```

### 4.2 数据库优化

#### 连接池配置

```yaml
# application.yml

spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      pool-name: AIReadyHikariPool
      leak-detection-threshold: 60000
```

#### 慢查询优化

```sql
-- 启用慢查询日志
ALTER SYSTEM SET log_min_duration_statement = 1000; -- 记录超过1秒的查询
SELECT pg_reload_conf();

-- 创建索引建议
-- 用户表
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
CREATE INDEX CONCURRENTLY idx_users_status ON users(status);
CREATE INDEX CONCURRENTLY idx_users_created_at ON users(created_at);

-- 订单表
CREATE INDEX CONCURRENTLY idx_orders_user_id ON orders(user_id);
CREATE INDEX CONCURRENTLY idx_orders_status ON orders(status);
CREATE INDEX CONCURRENTLY idx_orders_created_at ON orders(created_at);
CREATE INDEX CONCURRENTLY idx_orders_user_status ON orders(user_id, status);

-- 日志表 (分区)
CREATE TABLE system_logs (
    id BIGSERIAL,
    level VARCHAR(10),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 按月分区
CREATE TABLE system_logs_2026_01 PARTITION OF system_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE system_logs_2026_02 PARTITION OF system_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE system_logs_2026_03 PARTITION OF system_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- 定期分析
VACUUM ANALYZE users;
VACUUM ANALYZE orders;
```

### 4.3 CDN 配置

#### 阿里云 CDN 配置

```json
{
  "DomainName": "static.ai-ready.cn",
  "Sources": [
    {
      "SourceType": "oss",
      "SourcePort": 443,
      "Source": "ai-ready-static.oss-cn-hangzhou.aliyuncs.com",
      "Priority": 20
    }
  ],
  "CacheConfig": {
    "CacheContent": [
      {
        "CacheType": "suffix",
        "CacheContents": [".jpg", ".jpeg", ".png", ".gif", ".webp"],
        "TTL": 31536000
      },
      {
        "CacheType": "suffix",
        "CacheContents": [".css", ".js"],
        "TTL": 86400
      },
      {
        "CacheType": "suffix",
        "CacheContents": [".woff", ".woff2", ".ttf", ".eot"],
        "TTL": 31536000
      }
    ]
  },
  "HttpsConfig": {
    "Enable": true,
    "CertName": "ai-ready.cn",
    "SSLProtocol": "TLSv1.2 TLSv1.3",
    "ForceRedirect": "https"
  }
}
```

#### 静态资源缓存策略

```nginx
# 静态资源服务器配置
server {
    listen 443 ssl http2;
    server_name static.ai-ready.cn;

    # 静态资源根目录
    root /var/www/static;

    # 图片缓存
    location ~* \.(jpg|jpeg|png|gif|webp|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header X-Content-Type-Options "nosniff";
        access_log off;
    }

    # CSS/JS 缓存
    location ~* \.(css|js)$ {
        expires 1d;
        add_header Cache-Control "public";
        access_log off;
    }

    # 字体缓存
    location ~* \.(woff|woff2|ttf|eot|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*";
        access_log off;
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1000;
}
```

---

## 5. 监控与告警

### 5.1 监控架构

```
┌─────────────────────────────────────────────────────────────────┐
│                       监控架构                                   │
│                                                                 │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐   │
│  │ Node      │  │ App       │  │ PostgreSQL│  │ Redis     │   │
│  │ Exporter  │  │ Exporter  │  │ Exporter  │  │ Exporter  │   │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘   │
│        │              │              │              │          │
│        └──────────────┴──────────────┴──────────────┘          │
│                              │                                  │
│                      ┌───────▼───────┐                          │
│                      │  Prometheus   │                          │
│                      │  (指标采集)   │                          │
│                      └───────┬───────┘                          │
│                              │                                  │
│              ┌───────────────┼───────────────┐                  │
│              │               │               │                  │
│        ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐           │
│        │ Grafana   │   │AlertManager│   │  Loki     │           │
│        │ (可视化)  │   │ (告警)    │   │ (日志)    │           │
│        └───────────┘   └───────────┘   └───────────┘           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 关键告警规则

```yaml
# ai-ready-alerts.yml

groups:
  - name: ai-ready-availability
    rules:
      # 可用性告警
      - alert: AIRReadyAPIDown
        expr: up{job="ai-ready-api"} == 0
        for: 1m
        labels:
          severity: critical
          priority: p0
        annotations:
          summary: "AI-Ready API 宕机"
          description: "实例 {{ $labels.instance }} 已宕机超过1分钟"

      - alert: AIRReadyHighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "AI-Ready API 高错误率"
          description: "5xx错误率 {{ $value | humanizePercentage }}"

      # 数据库告警
      - alert: PostgreSQLDown
        expr: pg_up == 0
        for: 1m
        labels:
          severity: critical
          priority: p0
        annotations:
          summary: "PostgreSQL 宕机"
          description: "实例 {{ $labels.instance }} 已宕机"

      - alert: PostgreSQLReplicationLag
        expr: pg_replication_lag_seconds > 30
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 复制延迟过高"
          description: "复制延迟 {{ $value }}秒"

      # Redis 告警
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
          priority: p0
        annotations:
          summary: "Redis 宕机"

      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 内存使用率过高"
          description: "内存使用率 {{ $value | humanizePercentage }}"
```

---

## 6. 部署方案

### 6.1 生产环境部署清单

| 组件 | 副本数 | 配置 | 端口 |
|------|--------|------|------|
| NGINX (主) | 1 | 4C/8G | 80, 443 |
| NGINX (备) | 1 | 4C/8G | 80, 443 |
| API 服务 | 3+ | 4C/8G | 8080 |
| PostgreSQL (主) | 1 | 8C/32G | 5432 |
| PostgreSQL (从) | 2 | 8C/32G | 5432 |
| Redis (主) | 1 | 4C/16G | 6379 |
| Redis (从) | 2 | 4C/16G | 6379 |
| Prometheus | 1 | 4C/8G | 9090 |
| Grafana | 1 | 2C/4G | 3000 |

### 6.2 部署脚本

```bash
#!/bin/bash
# deploy.sh

set -e

VERSION=${1:-latest}
ENVIRONMENT=${2:-staging}

echo "======================================"
echo "AI-Ready Deployment Script"
echo "Version: $VERSION"
echo "Environment: $ENVIRONMENT"
echo "======================================"

# 1. 拉取最新镜像
echo "Pulling images..."
docker pull ai-ready/api:$VERSION

# 2. 数据库备份
echo "Backing up database..."
docker exec ai-ready-postgres-primary pg_dump -U devuser proddb > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. 滚动更新
echo "Rolling update..."
docker-compose -f docker-compose.$ENVIRONMENT.yml up -d --no-deps --build api

# 4. 健康检查
echo "Health check..."
for i in {1..30}; do
    if curl -s -f http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ Health check passed"
        break
    fi
    echo "Waiting for health check... ($i/30)"
    sleep 10
done

# 5. 验证部署
echo "Verifying deployment..."
curl -s http://localhost:8080/actuator/health | jq .

echo "======================================"
echo "✅ Deployment completed successfully"
echo "======================================"
```

---

## 7. 总结

### 7.1 架构特点

| 特性 | 设计 | 预期效果 |
|------|------|----------|
| 高可用 | 多层冗余 + 自动故障转移 | 99.9% 可用性 |
| 高性能 | 负载均衡 + 缓存 + CDN | < 200ms 响应时间 |
| 可扩展 | 水平扩展 + 自动伸缩 | 支持 10x 流量增长 |
| 可观测 | 全链路监控 + 日志分析 | 快速定位问题 |

### 7.2 关键指标

| 指标 | 目标 | 说明 |
|------|------|------|
| SLA | 99.9% | 年度停机时间 < 8.76小时 |
| RTO | < 15分钟 | 故障恢复时间 |
| RPO | < 5分钟 | 数据恢复点 |
| API响应时间 | < 200ms (P95) | 95%请求响应时间 |

### 7.3 后续优化

1. 📋 **异地多活**: 实现跨区域高可用
2. 📋 **灰度发布**: 实现渐进式发布策略
3. 📋 **智能运维**: AI辅助故障诊断和预测

---

**文档完成时间**: 2026-03-27  
**作者**: devops-engineer  
**下一次评审**: 2026-04-27