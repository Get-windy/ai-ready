# AI-Ready 监控告警模块配置指南

## 概述

本指南详细说明AI-Ready测试环境监控告警模块的配置、部署和运维。

## 1. 架构概览

### 1.1 组件架构
```
┌─────────────────────────────────────────────────────────┐
│                   监控告警架构                            │
├─────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │Prometheus│  │Grafana  │  │Alertmanager│ │Exporters│   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
│       │            │            │            │         │
│  ┌──────────────────────────────────────────────────┐  │
│  │                 Docker Network                   │  │
│  └──────────────────────────────────────────────────┘  │
│       │            │            │            │         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │PostgreSQL│  │Redis    │  │Nginx    │  │应用服务  │   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 1.2 数据流
1. **指标收集**: Exporters收集各服务指标 → Prometheus
2. **告警评估**: Prometheus根据告警规则评估 → Alertmanager
3. **告警通知**: Alertmanager根据路由规则发送通知
4. **可视化**: Grafana从Prometheus获取数据展示

## 2. 部署配置

### 2.1 Docker Compose配置

#### 2.1.1 Prometheus配置
```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    restart: unless-stopped
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"
    networks:
      - ai-ready-test-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

#### 2.1.2 Alertmanager配置
```yaml
  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    restart: unless-stopped
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - ./alertmanager/ha-monitoring-alerts.yml:/etc/alertmanager/ha-monitoring-alerts.yml
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--cluster.advertise-address=alertmanager:9093'
    ports:
      - "9093:9093"
    networks:
      - ai-ready-test-network
    depends_on:
      - prometheus
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9093/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

#### 2.1.3 Grafana配置
```yaml
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    restart: unless-stopped
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    ports:
      - "3000:3000"
    networks:
      - ai-ready-test-network
    depends_on:
      - prometheus
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

### 2.2 网络配置
```yaml
networks:
  ai-ready-test-network:
    external: true
    name: ai-ready-test-network

volumes:
  prometheus_data:
    driver: local
  grafana_data:
    driver: local
```

## 3. 监控指标配置

### 3.1 Prometheus配置 (prometheus.yml)

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'ai-ready-test'
    environment: 'test'

# 告警规则文件
rule_files:
  - "alert_rules.yml"
  - "/etc/prometheus/ha-monitoring-alerts.yml"

# 抓取配置
scrape_configs:
  # Prometheus自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node Exporter (系统监控)
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 30s

  # PostgreSQL Exporter
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgresql-exporter:9187']
    scrape_interval: 30s
    params:
      dsn: ['postgresql://postgres:password@postgres:5432/postgres?sslmode=disable']

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
    scrape_interval: 30s

  # Nginx Exporter
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-exporter:9113']
    scrape_interval: 30s

  # 应用服务监控
  - job_name: 'application'
    static_configs:
      - targets:
        - 'app-service-1:8080'
        - 'app-service-2:8080'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  # cAdvisor (容器监控)
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    scrape_interval: 30s

# 告警管理器配置
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

### 3.2 告警规则配置 (alert_rules.yml)

```yaml
# 系统资源告警
groups:
  - name: system_alerts
    rules:
      # CPU使用率告警
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          description: '实例 {{ $labels.instance }} 的CPU使用率超过85%持续5分钟'
          summary: '高CPU使用率告警'

      # 内存使用率告警
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          description: '实例 {{ $labels.instance }} 的内存使用率超过85%持续5分钟'
          summary: '高内存使用率告警'

      # 磁盘使用率告警
      - alert: HighDiskUsage
        expr: (node_filesystem_size_bytes{fstype!~"tmpfs|ramfs"} - node_filesystem_free_bytes{fstype!~"tmpfs|ramfs"}) / node_filesystem_size_bytes{fstype!~"tmpfs|ramfs"} * 100 > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          description: '实例 {{ $labels.instance }} 的磁盘 {{ $labels.mountpoint }} 使用率超过85%持续10分钟'
          summary: '高磁盘使用率告警'

  # 服务健康告警
  - name: service_alerts
    rules:
      # 服务不可用告警
      - alert: ServiceUnhealthy
        expr: up{job=~".*"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          description: '服务 {{ $labels.job }} (实例: {{ $labels.instance }}) 健康检查失败持续2分钟'
          summary: '服务不可用告警'

      # 服务错误率告警
      - alert: HighServiceErrorRate
        expr: sum(rate(http_requests_total{status=~"5.."}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service) * 100 > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          description: '服务 {{ $labels.service }} 的错误率超过1%持续5分钟'
          summary: '高服务错误率告警'

  # 数据库告警
  - name: database_alerts
    rules:
      # PostgreSQL连接数告警
      - alert: HighPostgreSQLConnections
        expr: pg_stat_database_numbackends{datname!~"template.*|postgres"} > (pg_settings_max_connections{setting} * 0.8)
        for: 5m
        labels:
          severity: warning
        annotations:
          description: 'PostgreSQL数据库 {{ $labels.datname }} 的连接数超过最大连接数的80%'
          summary: '高PostgreSQL连接数告警'

      # PostgreSQL复制延迟告警
      - alert: HighPostgreSQLReplicationLag
        expr: pg_replication_lag_seconds > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          description: 'PostgreSQL复制延迟超过10秒持续5分钟'
          summary: '高PostgreSQL复制延迟告警'

  # Redis告警
  - name: redis_alerts
    rules:
      # Redis内存使用率告警
      - alert: HighRedisMemoryUsage
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          description: 'Redis实例 {{ $labels.instance }} 的内存使用率超过85%'
          summary: '高Redis内存使用率告警'

      # Redis连接数告警
      - alert: HighRedisConnections
        expr: redis_connected_clients > (redis_maxclients * 0.8)
        for: 5m
        labels:
          severity: warning
        annotations:
          description: 'Redis实例 {{ $labels.instance }} 的连接数超过最大连接数的80%'
          summary: '高Redis连接数告警'
```

## 4. 告警通知配置

### 4.1 Alertmanager配置 (alertmanager.yml)

```yaml
# alertmanager.yml
global:
  # Slack配置（可选）
  # slack_api_url: 'https://hooks.slack.com/services/...'

  # 邮件配置
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'monitoring@ai-ready.local'
  smtp_auth_username: 'monitoring@ai-ready.local'
  smtp_auth_password: 'password'
  smtp_require_tls: true

  # 全局超时配置
  resolve_timeout: 5m

# 路由配置
route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'team-email'

  # 子路由规则
  routes:
    # 严重告警立即通知
    - match:
        severity: critical
      receiver: 'team-pager'
      group_wait: 10s
      repeat_interval: 5m

    # 警告告警常规通知
    - match:
        severity: warning
      receiver: 'team-email'
      group_wait: 30s
      repeat_interval: 12h

    # 数据库相关告警
    - match:
        service: 'database'
      receiver: 'dba-team'

    # 负载均衡器相关告警
    - match:
        service: 'loadbalancer'
      receiver: 'ops-team'

# 接收器配置
receivers:
  # 紧急告警接收器（电话/短信）
  - name: 'team-pager'
    email_configs:
      - to: 'ops-team@ai-ready.local'
        headers:
          subject: '[CRITICAL] AI-Ready 测试环境告警'
        # 邮件模板
        html: |
          <h2>紧急告警: {{ .GroupLabels.alertname }}</h2>
          <p><strong>告警级别:</strong> {{ .CommonLabels.severity }}</p>
          <p><strong>告警描述:</strong> {{ .CommonAnnotations.description }}</p>
          <p><strong>告警时间:</strong> {{ .StartsAt }}</p>
          <p><strong>影响服务:</strong> {{ .GroupLabels.service }}</p>
          <p><strong>处理手册:</strong> {{ .CommonAnnotations.runbook }}</p>

    # Slack配置（可选）
    # slack_configs:
    #   - channel: '#alerts-critical'
    #     title: '{{ .GroupLabels.alertname }}'
    #     text: '{{ .CommonAnnotations.description }}'
    #     color: 'danger'

  # 常规告警接收器（邮件）
  - name: 'team-email'
    email_configs:
      - to: 'team@ai-ready.local'
        headers:
          subject: '[WARNING] AI-Ready 测试环境告警'
        html: |
          <h2>告警通知: {{ .GroupLabels.alertname }}</h2>
          <p><strong>告警级别:</strong> {{ .CommonLabels.severity }}</p>
          <p><strong>告警描述:</strong> {{ .CommonAnnotations.description }}</p>
          <p><strong>告警时间:</strong> {{ .StartsAt }}</p>
          <p><strong>建议操作:</strong> 请检查相关服务状态</p>

  # 数据库团队接收器
  - name: 'dba-team'
    email_configs:
      - to: 'dba@ai-ready.local'
        headers:
          subject: '[DATABASE] AI-Ready 数据库告警'

  # 运维团队接收器
  - name: 'ops-team'
    email_configs:
      - to: 'ops@ai-ready.local'
        headers:
          subject: '[OPS] AI-Ready 运维告警'

# 告警抑制规则
inhibit_rules:
  # 当整个节点宕机时，抑制该节点上的所有服务告警
  - source_match:
      severity: 'critical'
      alertname: 'NodeDown'
    target_match:
      severity: 'warning'
    equal: ['instance']

  # 当数据库宕机时，抑制依赖数据库的应用告警
  - source_match:
      severity: 'critical'
      service: 'database'
    target_match:
      severity: 'warning'
    equal: ['cluster']
```

### 4.2 通知模板配置

#### 4.2.1 邮件模板 (email.tmpl)
```html
{{ define "email.default.html" }}
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AI-Ready 监控告警</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .alert { border-left: 4px solid #f0ad4e; padding: 10px; margin: 10px 0; }
        .critical { border-left-color: #d9534f; }
        .warning { border-left-color: #f0ad4e; }
        .info { border-left-color: #5bc0de; }
        .header { background-color: #f8f9fa; padding: 15px; border-bottom: 1px solid #dee2e6; }
        .footer { background-color: #f8f9fa; padding: 10px; border-top: 1px solid #dee2e6; font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
    <div class="header">
        <h1>AI-Ready 监控告警通知</h1>
        <p>环境: {{ .GroupLabels.environment }} | 集群: {{ .GroupLabels.cluster }}</p>
    </div>

    <div class="alert {{ .CommonLabels.severity }}">
        <h2>{{ .GroupLabels.alertname }}</h2>
        <p><strong>告警级别:</strong> {{ .CommonLabels.severity }}</p>
        <p><strong>告警描述:</strong> {{ .CommonAnnotations.description }}</p>
        <p><strong>开始时间:</strong> {{ .StartsAt }}</p>
        <p><strong>结束时间:</strong> {{ .EndsAt }}</p>
        <p><strong>影响服务:</strong> {{ .GroupLabels.service }}</p>
        
        {{ if .CommonAnnotations.runbook }}
        <p><strong>处理手册:</strong> <a href="{{ .CommonAnnotations.runbook }}">查看详细处理步骤</a></p>
        {{ end }}
        
        <h3>告警标签</h3>
        <ul>
        {{ range $key, $value := .CommonLabels }}
            <li><strong>{{ $key }}:</strong> {{ $value }}</li>
        {{ end }}
        </ul>
    </div>

    <div class="footer">
        <p>这是自动生成的监控告警通知，请勿直接回复此邮件。</p>
        <p>AI-Ready 监控系统 | 生成时间: {{ now }}</p>
    </div>
</body>
</html>
{{ end }}
```

## 5. Grafana配置

### 5.1 数据源配置 (datasources.yml)

```yaml
# grafana/provisioning/datasources/datasources.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: 15s
      queryTimeout: 60s
      httpMethod: POST
      manageAlerts: true
      prometheusType: Prometheus
      prometheusVersion: 2.45.0
      cacheLevel: 'High'
      disableRecordingRules: false
      incrementalQueryOverlapWindow: 10m
    secureJsonData:
      tlsSkipVerify: true
```

### 5.2 仪表板配置 (dashboards.yml)

```yaml
# grafana/provisioning/dashboards/dashboards.yml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: true
```

### 5.3 仪表板JSON配置

#### 5.3.1 系统总览仪表板 (system-overview.json)
```json
{
  "dashboard": {
    "title": "AI-Ready 系统总览",
    "tags": ["system", "overview"],
    "timezone": "browser",
    "panels": [
      {
        "title": "CPU使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "title": "内存使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100",
            "legendFormat": "{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      }
    ],
    "refresh": "30s"
  }
}
```

## 6. 部署和运维

### 6.1 部署脚本

```bash
#!/bin/bash
# deploy-monitoring-alerts.sh

set -e

# 配置变量
MONITORING_DIR="I:\AI-Ready\deploy\monitoring-alerts"
DOCKER_COMPOSE_FILE="${MONITORING_DIR}/docker-compose.monitoring.yml"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查目录
check_directories() {
    if [ ! -d "$MONITORING_DIR" ]; then
        log_error "监控目录不存在: $MONITORING_DIR"
        exit 1
    fi
    
    if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
        log_error "Docker Compose文件不存在: $DOCKER_COMPOSE_FILE"
        exit 1
    fi
}

# 启动监控服务
start_monitoring() {
    log_info "启动监控告警服务..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d
    
    if [ $? -eq 0 ]; then
        log_info "监控服务启动成功"
        log_info "访问地址:"
        log_info "  Prometheus: http://localhost:9090"
        log_info "  Grafana: http://localhost:3000 (admin/admin)"
        log_info "  Alertmanager: http://localhost:9093"
    else
        log_error "监控服务启动失败"
        exit 1
    fi
}

# 停止监控服务
stop_monitoring() {
    log_info "停止监控告警服务..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" down
}

# 重启监控服务
restart_monitoring() {
    log_info "重启监控告警服务..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" restart
}

# 查看服务状态
status_monitoring() {
    log_info "查看监控服务状态..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" ps
}

# 验证配置
validate_config() {
    log_info "验证监控配置..."
    
    # 验证Prometheus配置
    docker-compose -f "$DOCKER_COMPOSE_FILE" exec prometheus promtool check config /etc/prometheus/prometheus.yml
    
    # 验证Alertmanager配置
    docker-compose -f "$DOCKER_COMPOSE_FILE" exec alertmanager amtool check-config /etc/alertmanager/alertmanager.yml
    
    log_info "配置验证完成"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    # 检查Prometheus健康状态
    if curl -s http://localhost:9090/-/healthy > /dev/null; then
        log_info "Prometheus: ✅ 健康"
    else
        log_error "Prometheus: ❌ 不健康"
    fi
    
    # 检查Grafana健康状态
    if curl -s http://localhost:3000/api/health > /dev/null; then
        log_info "Grafana: ✅ 健康"
    else
        log_error "Grafana: ❌ 不健康"
    fi
    
    # 检查Alertmanager健康状态
    if curl -s http://localhost:9093/-/healthy > /dev/null; then
        log_info "Alertmanager: ✅ 健康"
    else
        log_error "Alertmanager: ❌ 不健康"
    fi
}

# 清理数据
cleanup() {
    log_warn "清理监控数据..."
    read -p "确认要清理监控数据吗？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose -f "$DOCKER_COMPOSE_FILE" down -v
        log_info "监控数据已清理"
    else
        log_info "取消清理操作"
    fi
}

# 显示帮助
show_help() {
    echo "AI-Ready 监控告警部署脚本"
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start     启动监控服务"
    echo "  stop      停止监控服务"
    echo "  restart   重启监控服务"
    echo "  status    查看服务状态"
    echo "  validate  验证配置"
    echo "  health    健康检查"
    echo "  cleanup   清理数据"
    echo "  help      显示帮助"
}

# 主函数
main() {
    check_directories
    
    case "$1" in
        start)
            start_monitoring
            ;;
        stop)
            stop_monitoring
            ;;
        restart)
            restart_monitoring
            ;;
        status)
            status_monitoring
            ;;
        validate)
            validate_config
            ;;
        health)
            health_check
            ;;
        cleanup)
            cleanup
            ;;
        help|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@"
```

### 6.2 运维命令参考

```bash
# 1. 启动监控服务
./deploy-monitoring-alerts.sh start

# 2. 查看服务状态
./deploy-monitoring-alerts.sh status

# 3. 验证配置
./deploy-monitoring-alerts.sh validate

# 4. 健康检查
./deploy-monitoring-alerts.sh health

# 5. 重启服务
./deploy-monitoring-alerts.sh restart

# 6. 停止服务
./deploy-monitoring-alerts.sh stop

# 7. 清理数据
./deploy-monitoring-alerts.sh cleanup
```

## 7. 故障排查

### 7.1 常见问题

#### 7.1.1 Prometheus无法启动
```bash
# 检查配置语法
docker-compose exec prometheus promtool check config /etc/prometheus/prometheus.yml

# 查看日志
docker-compose logs prometheus

# 检查端口占用
netstat -tlnp | grep 9090
```

#### 7.1.2 Alertmanager无法发送邮件
```bash
# 测试邮件配置
docker-compose exec alertmanager amtool config test /etc/alertmanager/alertmanager.yml

# 查看邮件日志
docker-compose logs alertmanager

# 手动测试邮件发送
echo "Test email" | mail -s "Test" recipient@example.com
```

#### 7.1.3 Grafana无法连接Prometheus
```bash
# 检查网络连接
docker-compose exec grafana ping prometheus

# 检查数据源配置
curl http://localhost:3000/api/datasources

# 查看Grafana日志
docker-compose logs grafana
```

### 7.2 性能优化

#### 7.2.1 调整资源限制
```yaml
# docker-compose.monitoring.yml 资源优化
services:
  prometheus:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
  
  grafana:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

#### 7.2.2 优化采集频率
```yaml
# prometheus.yml 优化采集频率
global:
  scrape_interval: 30s  # 从15s增加到30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'node'
    scrape_interval: 60s  # 系统监控可以更慢
    
  - job_name: 'application'
    scrape_interval: 15s  # 应用监控保持较快频率
```

## 8. 最佳实践

### 8.1 告警设计原则

1. **避免告警风暴**: 使用告警分组和抑制规则
2. **设置合理阈值**: 基于历史数据设置阈值
3. **提供处理手册**: 每个告警都应有runbook链接
4. **分级通知**: 不同级别告警使用不同通知渠道

### 8.2 监控指标选择

1. **关键指标优先**: 监控影响业务的核心指标
2. **避免过度监控**: 只监控真正重要的指标
3. **设置基线**: 基于历史数据建立性能基线
4. **定期审查**: 定期审查和优化监控指标

### 8.3 运维管理

1. **定期备份**: 定期备份监控数据和配置
2. **容量规划**: 监控存储使用情况，提前扩容
3. **版本管理**: 使用Git管理所有配置文件
4. **文档更新**: 及时更新运维文档和故障处理手册

## 9. 附录

### 9.1 配置文件清单

```
I:\AI-Ready\deploy\monitoring-alerts\
├── docker-compose.monitoring.yml      # Docker Compose配置
├── prometheus\
│   ├── prometheus.yml                 # Prometheus主配置
│   └── alert_rules.yml                # 告警规则
├── alertmanager\
│   ├── alertmanager.yml               # Alertmanager主配置
│   └── ha-monitoring-alerts.yml       # 高可用告警配置
├── grafana\
│   ├── provisioning\                  # Grafana自动配置
│   └── dashboards\                    # 仪表板JSON文件
└── docs\                              # 文档目录
```

### 9.2 相关链接

- Prometheus官方文档: https://prometheus.io/docs/
- Grafana官方文档: https://grafana.com/docs/
- Alertmanager官方文档: https://prometheus.io/docs/alerting/latest/alertmanager/
- 最佳实践指南: https://prometheus.io/docs/practices/

### 9.3 版本信息

- **Prometheus版本**: 2.45.0+
- **Grafana版本**: 10.0.0+
- **Alertmanager版本**: 0.25.0+
- **配置版本**: v1.0.0
- **更新日期**: 2026-04-30

---
**文档维护**: devops-engineer  
**最后更新**: 2026-04-30  
**适用环境**: AI-Ready测试环境  
**Sprint**: 28+1