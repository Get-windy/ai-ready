#!/bin/bash
# 监控告警模块部署脚本
# 文件名: deploy-monitoring.sh
# 版本: v1.0.0
# 创建日期: 2026-04-27
# 描述: 部署监控告警模块完整环境

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 '$1' 未安装，请先安装"
        exit 1
    fi
}

# 检查Docker和Docker Compose
check_docker() {
    log_info "检查Docker环境..."
    
    if ! docker --version &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! docker-compose --version &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    log_success "Docker环境检查通过"
    log_info "Docker版本: $(docker --version | cut -d' ' -f3 | cut -d',' -f1)"
    log_info "Docker Compose版本: $(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)"
}

# 检查端口占用
check_ports() {
    log_info "检查端口占用情况..."
    
    local ports=("5433" "6380" "5673" "15673" "9090" "9093" "3000" "9101" "8081" "80" "443")
    local occupied_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -i :$port &> /dev/null; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -gt 0 ]; then
        log_warning "以下端口已被占用: ${occupied_ports[*]}"
        read -p "是否继续部署？(y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "用户取消部署"
            exit 0
        fi
    else
        log_success "端口检查通过"
    fi
}

# 创建目录结构
create_directories() {
    log_info "创建目录结构..."
    
    local directories=(
        "prometheus/rules"
        "prometheus/file_sd"
        "alertmanager/templates"
        "grafana/provisioning/datasources"
        "grafana/provisioning/dashboards"
        "grafana/dashboards"
        "nginx/conf.d"
        "nginx/ssl"
        "nginx/html"
        "init-scripts"
        "logs/prometheus"
        "logs/grafana"
        "logs/alertmanager"
        "logs/monitoring-api"
        "logs/nginx"
        "custom-exporter/config"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log_info "创建目录: $dir"
        fi
    done
    
    log_success "目录结构创建完成"
}

# 创建初始化脚本
create_init_scripts() {
    log_info "创建数据库初始化脚本..."
    
    cat > init-scripts/postgres-init.sql << 'EOF'
-- 监控数据库初始化脚本
-- 创建监控数据库用户和表结构

-- 创建监控数据库
CREATE DATABASE monitoring_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- 切换到监控数据库
\c monitoring_db;

-- 创建监控用户
CREATE USER monitoring_user WITH PASSWORD 'monitoring_pass_123';

-- 授予权限
GRANT ALL PRIVILEGES ON DATABASE monitoring_db TO monitoring_user;
GRANT CREATE ON DATABASE monitoring_db TO monitoring_user;

-- 创建监控表结构
-- 告警记录表
CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    alert_name VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    instance VARCHAR(255),
    summary TEXT,
    description TEXT,
    status VARCHAR(50) DEFAULT 'firing',
    starts_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP,
    labels JSONB,
    annotations JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 监控指标表
CREATE TABLE IF NOT EXISTS metrics (
    id SERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    labels JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 服务健康检查表
CREATE TABLE IF NOT EXISTS health_checks (
    id SERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    response_time_ms INTEGER,
    last_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_starts_at ON alerts(starts_at);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp);
CREATE INDEX idx_health_checks_last_check ON health_checks(last_check);

-- 创建视图
CREATE VIEW alert_summary AS
SELECT 
    alert_name,
    severity,
    COUNT(*) as total_count,
    COUNT(CASE WHEN status = 'firing' THEN 1 END) as firing_count,
    COUNT(CASE WHEN status = 'resolved' THEN 1 END) as resolved_count,
    MIN(starts_at) as first_occurrence,
    MAX(updated_at) as last_updated
FROM alerts
GROUP BY alert_name, severity;

-- 授予用户权限
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO monitoring_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO monitoring_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO monitoring_user;

-- 创建更新触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为表添加触发器
CREATE TRIGGER update_alerts_updated_at BEFORE UPDATE ON alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_metrics_updated_at BEFORE UPDATE ON metrics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_health_checks_updated_at BEFORE UPDATE ON health_checks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入初始数据
INSERT INTO health_checks (service_name, endpoint, status) VALUES
('prometheus', 'http://prometheus:9090/-/healthy', 'healthy'),
('alertmanager', 'http://alertmanager:9093/-/healthy', 'healthy'),
('grafana', 'http://grafana:3000/api/health', 'healthy');

-- 输出完成信息
SELECT '监控数据库初始化完成' as message;
EOF
    
    log_success "数据库初始化脚本创建完成"
}

# 创建Nginx配置
create_nginx_config() {
    log_info "创建Nginx配置..."
    
    # 主配置文件
    cat > nginx/nginx.conf << 'EOF'
# Nginx主配置文件 - 监控告警模块
user  nginx;
worker_processes  auto;
error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;

events {
    worker_connections  1024;
    use epoll;
    multi_accept on;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for" '
                    'rt=$request_time uct="$upstream_connect_time" '
                    'uht="$upstream_header_time" urt="$upstream_response_time"';

    access_log  /var/log/nginx/access.log  main;
    
    # 基础配置
    sendfile        on;
    tcp_nopush      on;
    tcp_nodelay     on;
    keepalive_timeout  65;
    types_hash_max_size 2048;
    server_tokens off;
    
    # 限制配置
    client_max_body_size 10m;
    client_body_buffer_size 128k;
    client_header_buffer_size 1k;
    large_client_header_buffers 4 4k;
    
    # Gzip配置
    gzip on;
    gzip_disable "msie6";
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;
    
    # 包含虚拟主机配置
    include /etc/nginx/conf.d/*.conf;
}
EOF
    
    # Grafana虚拟主机配置
    cat > nginx/conf.d/grafana.conf << 'EOF'
# Grafana反向代理配置
server {
    listen 80;
    server_name grafana.ai-ready.local;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 访问日志
    access_log /var/log/nginx/grafana.access.log main;
    error_log /var/log/nginx/grafana.error.log warn;
    
    location / {
        proxy_pass http://grafana-monitoring:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 缓冲区设置
        proxy_buffering off;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # 健康检查端点
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://grafana-monitoring:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 缓存设置
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF
    
    # Prometheus虚拟主机配置
    cat > nginx/conf.d/prometheus.conf << 'EOF'
# Prometheus反向代理配置
server {
    listen 80;
    server_name prometheus.ai-ready.local;
    
    # 安全头
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 基础认证
    auth_basic "Prometheus监控系统";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    # 访问日志
    access_log /var/log/nginx/prometheus.access.log main;
    error_log /var/log/nginx/prometheus.error.log warn;
    
    location / {
        proxy_pass http://prometheus-monitoring:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲区设置
        proxy_buffering off;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # API端点特殊处理
    location /api/ {
        proxy_pass http://prometheus-monitoring:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 允许跨域
        add_header Access-Control-Allow-Origin "*" always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization" always;
        add_header Access-Control-Expose-Headers "Content-Length,Content-Range" always;
        
        # 预检请求处理
        if ($request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin "*" always;
            add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
            add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization" always;
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type 'text/plain; charset=utf-8';
            add_header Content-Length 0;
            return 204;
        }
    }
    
    # 健康检查端点
    location /-/healthy {
        proxy_pass http://prometheus-monitoring:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        access_log off;
    }
    
    # 就绪检查端点
    location /-/ready {
        proxy_pass http://prometheus-monitoring:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        access_log off;
    }
}
EOF
    
    # AlertManager虚拟主机配置
    cat > nginx/conf.d/alertmanager.conf << 'EOF'
# AlertManager反向代理配置
server {
    listen 80;
    server_name alertmanager.ai-ready.local;
    
    # 安全头
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 基础认证
    auth_basic "AlertManager告警系统";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    # 访问日志
    access_log /var/log/nginx/alertmanager.access.log main;
    error_log /var/log/nginx/alertmanager.error.log warn;
    
    location / {
        proxy_pass http://alertmanager-monitoring:9093;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲区设置
        proxy_buffering off;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # 健康检查端点
    location /-/healthy {
        proxy_pass http://alertmanager-monitoring:9093;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        access_log off;
    }
}
EOF
    
    # 监控API虚拟主机配置
    cat > nginx/conf.d/monitoring-api.conf << 'EOF'
# 监控API反向代理配置
server {
    listen 80;
    server_name monitoring-api.ai-ready.local;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 访问日志
    access_log /var/log/nginx/monitoring-api.access.log main;
    error_log /var/log/nginx/monitoring-api.error.log warn;
    
    location / {
        proxy_pass http://monitoring-api-service:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲区设置
        proxy_buffering off;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # Actuator端点
    location /actuator/ {
        proxy_pass http://monitoring-api-service:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 限制访问
        allow 172.30.0.0/24;
        allow 127.0.0.1;
        deny all;
    }
    
    # API文档
    location /swagger-ui/ {
        proxy_pass http://monitoring-api-service:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # 健康检查端点
    location /health {
        proxy_pass http://monitoring-api-service:8080/actuator/health;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        access_log off;
    }
    
    # 指标端点
    location /metrics {
        proxy_pass http://monitoring-api-service:8080/actuator/prometheus;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 允许Prometheus访问
        allow 172.30.0.0/24;
        deny all;
    }
}
EOF
    
    # 创建默认页面
    cat > nginx/html/index.html << 'EOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI-Ready 监控告警系统</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 40px 20px;
        }
        
        .header {
            text-align: center;
            margin-bottom: 50px;
        }
        
        .logo {
            width: 120px;
            height: 120px;
            margin: 0 auto 20px;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 48px;
            color: white;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
        }
        
        h1 {
            font-size: 2.8rem;
            color: white;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
        }
        
        .subtitle {
            font-size: 1.2rem;
            color: rgba(255, 255, 255, 0.9);
            margin-bottom: 30px;
        }
        
        .services-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 30px;
            margin-bottom: 40px;
        }
        
        .service-card {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 15px;
            padding: 30px;
            text-align: center;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
        }
        
        .service-card:hover {
            transform: translateY(-10px);
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
        }
        
        .service-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
        }
        
        .service-card.prometheus::before {
            background: linear-gradient(90deg, #e6522c, #e65100);
        }
        
        .service-card.grafana::before {
            background: linear-gradient(90deg, #f46800, #ff8c00);
        }
        
        .service-card.alertmanager::before {
            background: linear-gradient(90deg, #f44336, #d32f2f);
        }
        
        .service-card.api::before {
            background: linear-gradient(90deg, #2196f3, #1976d2);
        }
        
        .service-icon {
            font-size: 48px;
            margin-bottom: 20px;
            display: block;
        }
        
        .service-card h3 {
            font-size: 1.5rem;
            margin-bottom: 15px;
            color: #333;
        }
        
        .service-card p {
            color: #666;
            margin-bottom: 25px;
            font-size: 0.95rem;
        }
        
        .btn {
            display: inline-block;
            padding: 12px 30px;
            background: linear-gradient(90deg, #667eea, #764ba2);
            color: white;
            text-decoration: none;
            border-radius: 50px;
            font-weight: 600;
            transition: all 0.3s ease;
            border: none;
            cursor: pointer;
            font-size: 1rem;
        }
        
        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 7px 14px rgba(102, 126, 234, 0.3);
        }
        
        .btn.prometheus {
            background: linear-gradient(90deg, #e6522c, #e65100);
        }
        
        .btn.grafana {
            background: linear-gradient(90deg, #f46800, #ff8c00);
        }
        
        .btn.alertmanager {
            background: linear-gradient(90deg, #f44336, #d32f2f);
        }
        
        .btn.api {
            background: linear-gradient(90deg, #2196f3, #1976d2);
        }
        
        .status-section {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 15px;
            padding: 30px;
            margin-top: 40px;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
        }
        
        .status-section h2 {
            font-size: 1.8rem;
            margin-bottom: 20px;
            color: #333;
            text-align: center;
        }
        
        .status-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
        }
        
        .status-item {
            text-align: center;
            padding: 20px;
            background: rgba(102, 126, 234, 0.05);
            border-radius: 10px;
            transition: all 0.3s ease;
        }
        
        .status-item:hover {
            background: rgba(102, 126, 234, 0.1);
        }
        
        .status-item h4 {
            font-size: 1.1rem;
            margin-bottom: 10px;
            color: #555;
        }
        
        .status-value {
            font-size: 1.3rem;
            font-weight: 600;
            color: #667eea;
        }
        
        .footer {
            text-align: center;
            margin-top: 50px;
            color: rgba(255, 255, 255, 0.8);
            font-size: 0.9rem;
        }
        
        @media (max-width: 768px) {
            h1 {
                font-size: 2.2rem;
            }
            
            .services-grid {
                grid-template-columns: 1fr;
            }
            
            .container {
                padding: 20px 10px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">⚡</div>
            <h1>AI-Ready 监控告警系统</h1>
            <p class="subtitle">全面的系统监控、告警管理和可视化平台</p>
        </div>
        
        <div class="services-grid">
            <div class="service-card prometheus">
                <span class="service-icon">📊</span>
                <h3>Prometheus</h3>
                <p>强大的监控数据收集和存储系统，支持多维数据模型和灵活的查询语言。</p>
                <a href="http://prometheus.ai-ready.local" class="btn prometheus" target="_blank">访问 Prometheus</a>
            </div>
            
            <div class="service-card grafana">
                <span class="service-icon">📈</span>
                <h3>Grafana</h3>
                <p>功能丰富的监控数据可视化平台，提供实时仪表盘和图表展示。</p>
                <a href="http://grafana.ai-ready.local" class="btn grafana" target="_blank">访问 Grafana</a>
            </div>
            
            <div class="service-card alertmanager">
                <span class="service-icon">🚨</span>
                <h3>AlertManager</h3>
                <p>智能告警管理系统，支持多通道通知和告警路由策略。</p>
                <a href="http://alertmanager.ai-ready.local" class="btn alertmanager" target="_blank">访问 AlertManager</a>
            </div>
            
            <div class="service-card api">
                <span class="service-icon">🔧</span>
                <h3>监控API</h3>
                <p>RESTful API服务，提供监控数据访问和告警管理接口。</p>
                <a href="http://monitoring-api.ai-ready.local" class="btn api" target="_blank">访问 API</a>
            </div>
        </div>
        
        <div class="status-section">
            <h2>系统状态概览</h2>
            <div class="status-grid">
                <div class="status-item">
                    <h4>服务状态</h4>
                    <div class="status-value">运行中</div>
                </div>
                <div class="status-item">
                    <h4>告警数量</h4>
                    <div class="status-value">0</div>
                </div>
                <div class="status-item">
                    <h4>数据保留</h4>
                    <div class="status-value">30天</div>
                </div>
                <div class="status-item">
                    <h4>监控目标</h4>
                    <div class="status-value">12个</div>
                </div>
            </div>
        </div>
        
        <div class="footer">
            <p>© 2026 AI-Ready 监控告警系统 | 版本: v1.0.0 | 最后更新: 2026-04-27</p>
            <p>如有问题，请联系: infrastructure-team@ai-ready.local</p>
        </div>
    </div>
    
    <script>
        // 动态更新状态信息
        async function updateStatus() {
            try {
                // 这里可以添加实际的状态检查逻辑
                const services = ['prometheus', 'grafana', 'alertmanager', 'api'];
                let allHealthy = true;
                
                for (const service of services) {
                    try {
                        const response = await fetch(`http://${service}.ai-ready.local/health`);
                        if (!response.ok) {
                            allHealthy = false;
                            break;
                        }
                    } catch (error) {
                        allHealthy = false;
                        break;
                    }
                }
                
                const statusElement = document.querySelector('.status-item:nth-child(1) .status-value');
                statusElement.textContent = allHealthy ? '运行中' : '部分异常';
                statusElement.style.color = allHealthy ? '#4CAF50' : '#F44336';
                
            } catch (error) {
                console.log('状态检查失败:', error);
            }
        }
        
        // 页面加载时更新状态
        document.addEventListener('DOMContentLoaded', updateStatus);
        
        // 每30秒更新一次状态
        setInterval(updateStatus, 30000);
    </script>
</body>
</html>
EOF
    
    log_success "Nginx配置创建完成"
}

# 创建基础认证文件
create_auth_file() {
    log_info "创建基础认证文件..."
    
    # 使用openssl生成密码（如果可用）
    if command -v openssl &> /dev/null; then
        local password=$(openssl rand -base64 12)
        echo "admin:$(openssl passwd -apr1 $password)" > nginx/.htpasswd
        log_info "生成的管理员密码: $password"
        log_warning "请保存此密码，稍后无法查看"
    else
        # 使用固定密码（仅用于开发环境）
        echo "admin:\$apr1\$w5kRv/1M\$c5R8W5r2J8Q7H9Y1V3T6B." > nginx/.htpasswd
        log_warning "使用固定密码: admin123 (仅开发环境使用)"
    fi
    
    log_success "基础认证文件创建完成"
}

# 创建启动脚本
create_startup_script() {
    log_info "创建启动脚本..."
    
    cat > start-monitoring.sh << 'EOF'
#!/bin/bash
# 监控告警模块启动脚本
# 使用: ./start-monitoring.sh [环境]

set -e

ENVIRONMENT=${1:-"development"}
COMPOSE_FILE="monitoring-alerting-deployment.yml"

echo "=========================================="
echo "启动 AI-Ready 监控告警模块"
echo "环境: $ENVIRONMENT"
echo "时间: $(date)"
echo "=========================================="

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "错误: 找不到Docker Compose文件: $COMPOSE_FILE"
    exit 1
fi

# 根据环境设置变量
case "$ENVIRONMENT" in
    "development")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-dev"
        export NODE_ENV="development"
        ;;
    "staging")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-staging"
        export NODE_ENV="staging"
        ;;
    "production")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-prod"
        export NODE_ENV="production"
        ;;
    *)
        echo "错误: 未知环境 '$ENVIRONMENT'，可用环境: development, staging, production"
        exit 1
        ;;
esac

echo "项目名称: $COMPOSE_PROJECT_NAME"
echo "Node环境: $NODE_ENV"

# 启动服务
echo "启动Docker Compose服务..."
docker-compose -f "$COMPOSE_FILE" up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose -f "$COMPOSE_FILE" ps

# 显示访问信息
echo ""
echo "=========================================="
echo "监控告警模块启动完成！"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  Prometheus:      http://localhost:9090"
echo "  Grafana:         http://localhost:3000 (admin/admin123)"
echo "  AlertManager:    http://localhost:9093"
echo "  监控API:         http://localhost:8081"
echo "  Nginx门户:       http://localhost"
echo ""
echo "数据库连接:"
echo "  PostgreSQL:      localhost:5433 (monitoring_user/monitoring_pass_123)"
echo "  Redis:           localhost:6380 (密码: redis_monitoring_pass_123)"
echo "  RabbitMQ管理:    http://localhost:15673 (monitoring_rabbit/rabbit_monitoring_pass_123)"
echo ""
echo "管理命令:"
echo "  查看日志:        docker-compose -f $COMPOSE_FILE logs -f"
echo "  停止服务:        docker-compose -f $COMPOSE_FILE down"
echo "  重启服务:        docker-compose -f $COMPOSE_FILE restart
  查看状态:        docker-compose -f $COMPOSE_FILE ps
  清理数据:        docker-compose -f $COMPOSE_FILE down -v

# 健康检查
check_health() {
    echo "\n执行健康检查..."
    
    local services=("prometheus" "grafana" "alertmanager" "postgres" "redis" "rabbitmq" "nginx")
    local healthy=true
    
    for service in "${services[@]}"; do
        if docker-compose -f "$COMPOSE_FILE" ps | grep -q "${service}.*Up"; then
            echo "  ✓ ${service}: 运行正常"
        else
            echo "  ✗ ${service}: 运行异常"
            healthy=false
        fi
    done
    
    if [ "$healthy" = true ]; then
        echo "\n✅ 所有服务健康检查通过！"
    else
        echo "\n⚠️  部分服务运行异常，请检查日志"
        echo "查看日志命令: docker-compose -f $COMPOSE_FILE logs"
    fi
}

check_health

echo "\n部署完成！系统将在30秒后完全就绪。"
echo "请访问上述地址验证部署结果。"
"}]}]}