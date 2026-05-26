# Sprint 27+1 测试环境配置文档 - 健康检查指南

## 概述

本文档提供Sprint 27+1测试环境的健康检查操作指南，包括服务健康检查配置、健康检查脚本和健康检查命令。确保测试环境的稳定性和可靠性。

## 2. 健康检查脚本

### 2.1 PostgreSQL健康检查脚本

#### 2.1.1 基础健康检查脚本

**脚本文件**: `/usr/local/bin/postgresql-health-check.sh`

```bash
#!/bin/bash
# PostgreSQL健康检查脚本

set -e

# 检查PostgreSQL服务状态
check_postgresql_service() {
    if systemctl is-active --quiet postgresql; then
        echo "✅ PostgreSQL服务运行正常"
        return 0
    else
        echo "❌ PostgreSQL服务未运行"
        return 1
    fi
}

# 检查数据库连接
check_postgresql_connection() {
    if sudo -u postgres psql -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✅ PostgreSQL连接正常"
        return 0
    else
        echo "❌ PostgreSQL连接失败"
        return 1
    fi
}

# 检查数据库状态
check_postgresql_databases() {
    DATABASE_COUNT=$(sudo -u postgres psql -t -c "SELECT COUNT(*) FROM pg_database WHERE datname NOT IN ('template0', 'template1', 'postgres');" | tr -d ' ')
    
    if [ "$DATABASE_COUNT" -gt 0 ]; then
        echo "✅ 发现 $DATABASE_COUNT 个用户数据库"
        return 0
    else
        echo "⚠️  未发现用户数据库"
        return 0
    fi
}

# 检查表空间
check_postgresql_tablespaces() {
    TABLESPACE_COUNT=$(sudo -u postgres psql -t -c "SELECT COUNT(*) FROM pg_tablespace WHERE spcname NOT IN ('pg_default', 'pg_global');" | tr -d ' ')
    
    if [ "$TABLESPACE_COUNT" -gt 0 ]; then
        echo "✅ 发现 $TABLESPACE_COUNT 个自定义表空间"
        return 0
    else
        echo "ℹ️  使用默认表空间"
        return 0
    fi
}

# 检查活动连接数
check_postgresql_connections() {
    ACTIVE_CONNECTIONS=$(sudo -u postgres psql -t -c "SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active';" | tr -d ' ')
    IDLE_CONNECTIONS=$(sudo -u postgres psql -t -c "SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'idle';" | tr -d ' ')
    
    echo "📊 连接统计:"
    echo "  活动连接: $ACTIVE_CONNECTIONS"
    echo "  空闲连接: $IDLE_CONNECTIONS"
    
    TOTAL_CONNECTIONS=$((ACTIVE_CONNECTIONS + IDLE_CONNECTIONS))
    
    if [ "$TOTAL_CONNECTIONS" -gt 100 ]; then
        echo "⚠️  总连接数较高: $TOTAL_CONNECTIONS"
        return 0
    else
        echo "✅ 连接数正常"
        return 0
    fi
}

# 检查死锁
check_postgresql_deadlocks() {
    DEADLOCKS=$(sudo -u postgres psql -t -c "SELECT SUM(deadlocks) FROM pg_stat_database;" | tr -d ' ')
    
    if [ "$DEADLOCKS" -gt 0 ]; then
        echo "⚠️  发现 $DEADLOCKS 个死锁"
        return 0
    else
        echo "✅ 无死锁"
        return 0
    fi
}

# 检查数据库大小
check_postgresql_size() {
    sudo -u postgres psql -c "
    SELECT 
        datname as \"数据库\",
        pg_size_pretty(pg_database_size(datname)) as \"大小\",
        pg_database_size(datname) as \"字节数\"
    FROM pg_database 
    WHERE datname NOT IN ('template0', 'template1')
    ORDER BY pg_database_size(datname) DESC;
    "
}

# 主函数
main() {
    echo "=== PostgreSQL健康检查 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    check_postgresql_service || exit_code=1
    check_postgresql_connection || exit_code=1
    check_postgresql_databases
    check_postgresql_tablespaces
    check_postgresql_connections
    check_postgresql_deadlocks
    
    echo ""
    echo "=== 数据库大小统计 ==="
    check_postgresql_size
    
    echo ""
    echo "=== 检查完成 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有关键检查通过"
    else
        echo "❌ 部分关键检查失败"
    fi
    
    return $exit_code
}

# 执行检查
main "$@"
```

### 2.2 Redis健康检查脚本

#### 2.2.1 Redis健康检查脚本

**脚本文件**: `/usr/local/bin/redis-health-check.sh`

```bash
#!/bin/bash
# Redis健康检查脚本

set -e

REDIS_PORT=6380
REDIS_PASS=$(sudo grep "^requirepass" /etc/redis/ai-ready-test.conf 2>/dev/null | awk '{print $2}' || echo "")

# 检查Redis服务状态
check_redis_service() {
    if systemctl is-active --quiet redis-ai-ready-test.service; then
        echo "✅ Redis测试实例服务运行正常"
        return 0
    else
        echo "❌ Redis测试实例服务未运行"
        return 1
    fi
}

# 检查Redis连接
check_redis_connection() {
    if [ -n "$REDIS_PASS" ]; then
        if redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" ping | grep -q "PONG"; then
            echo "✅ Redis连接正常"
            return 0
        else
            echo "❌ Redis连接失败"
            return 1
        fi
    else
        if redis-cli -p "$REDIS_PORT" ping | grep -q "PONG"; then
            echo "✅ Redis连接正常（无密码）"
            return 0
        else
            echo "❌ Redis连接失败"
            return 1
        fi
    fi
}

# 检查Redis信息
check_redis_info() {
    if [ -n "$REDIS_PASS" ]; then
        redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info | head -20
    else
        redis-cli -p "$REDIS_PORT" info | head -20
    fi
}

# 检查Redis内存使用
check_redis_memory() {
    if [ -n "$REDIS_PASS" ]; then
        MEMORY_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info memory)
    else
        MEMORY_INFO=$(redis-cli -p "$REDIS_PORT" info memory)
    fi
    
    USED_MEMORY=$(echo "$MEMORY_INFO" | grep "^used_memory:" | cut -d: -f2)
    MAX_MEMORY=$(echo "$MEMORY_INFO" | grep "^maxmemory:" | cut -d: -f2)
    
    if [ "$MAX_MEMORY" -eq 0 ]; then
        echo "⚠️  Redis未设置内存限制"
        echo "   当前使用内存: $(numfmt --to=iec $USED_MEMORY)"
        return 0
    fi
    
    MEMORY_PERCENT=$((USED_MEMORY * 100 / MAX_MEMORY))
    
    if [ "$MEMORY_PERCENT" -gt 90 ]; then
        echo "❌ Redis内存使用率过高: ${MEMORY_PERCENT}%"
        echo "   使用内存: $(numfmt --to=iec $USED_MEMORY)"
        echo "   最大内存: $(numfmt --to=iec $MAX_MEMORY)"
        return 1
    elif [ "$MEMORY_PERCENT" -gt 70 ]; then
        echo "⚠️  Redis内存使用率较高: ${MEMORY_PERCENT}%"
        echo "   使用内存: $(numfmt --to=iec $USED_MEMORY)"
        echo "   最大内存: $(numfmt --to=iec $MAX_MEMORY)"
        return 0
    else
        echo "✅ Redis内存使用率正常: ${MEMORY_PERCENT}%"
        echo "   使用内存: $(numfmt --to=iec $USED_MEMORY)"
        echo "   最大内存: $(numfmt --to=iec $MAX_MEMORY)"
        return 0
    fi
}

# 检查Redis持久化
check_redis_persistence() {
    if [ -n "$REDIS_PASS" ]; then
        PERSISTENCE_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info persistence)
    else
        PERSISTENCE_INFO=$(redis-cli -p "$REDIS_PORT" info persistence)
    fi
    
    RDB_LAST_SAVE=$(echo "$PERSISTENCE_INFO" | grep "^rdb_last_save_time:" | cut -d: -f2)
    CURRENT_TIME=$(date +%s)
    TIME_DIFF=$((CURRENT_TIME - RDB_LAST_SAVE))
    
    if [ "$TIME_DIFF" -gt 3600 ]; then
        echo "⚠️  Redis RDB持久化超过1小时未执行"
        echo "   最后保存时间: $(date -d @$RDB_LAST_SAVE)"
        return 0
    else
        echo "✅ Redis RDB持久化正常"
        echo "   最后保存时间: $(date -d @$RDB_LAST_SAVE)"
        return 0
    fi
}

# 检查Redis键空间
check_redis_keys() {
    if [ -n "$REDIS_PASS" ]; then
        KEYS_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info keyspace)
    else
        KEYS_INFO=$(redis-cli -p "$REDIS_PORT" info keyspace)
    fi
    
    echo "📊 Redis键空间统计:"
    echo "$KEYS_INFO"
}

# 主函数
main() {
    echo "=== Redis健康检查 ==="
    echo "检查时间: $(date)"
    echo "端口: $REDIS_PORT"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    check_redis_service || exit_code=1
    check_redis_connection || exit_code=1
    
    echo ""
    echo "=== Redis基本信息 ==="
    check_redis_info
    
    echo ""
    check_redis_memory || exit_code=1
    check_redis_persistence
    
    echo ""
    check_redis_keys
    
    echo ""
    echo "=== 检查完成 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有关键检查通过"
    else
        echo "❌ 部分关键检查失败"
    fi
    
    return $exit_code
}

# 执行检查
main "$@"
```

### 2.3 RabbitMQ健康检查脚本

#### 2.3.1 RabbitMQ健康检查脚本

**脚本文件**: `/usr/local/bin/rabbitmq-health-check.sh`

```bash
#!/bin/bash
# RabbitMQ健康检查脚本

set -e

# 检查RabbitMQ服务状态
check_rabbitmq_service() {
    if systemctl is-active --quiet rabbitmq-server; then
        echo "✅ RabbitMQ服务运行正常"
        return 0
    else
        echo "❌ RabbitMQ服务未运行"
        return 1
    fi
}

# 检查RabbitMQ节点状态
check_rabbitmq_node() {
    if sudo rabbitmqctl status > /dev/null 2>&1; then
        echo "✅ RabbitMQ节点状态正常"
        return 0
    else
        echo "❌ RabbitMQ节点状态异常"
        return 1
    fi
}

# 检查虚拟主机
check_rabbitmq_vhosts() {
    VHOSTS=$(sudo rabbitmqctl list_vhosts --quiet | wc -l)
    
    if [ "$VHOSTS" -gt 0 ]; then
        echo "✅ 发现 $VHOSTS 个虚拟主机"
        
        # 列出虚拟主机
        echo "虚拟主机列表:"
        sudo rabbitmqctl list_vhosts --quiet | while read vhost; do
            echo "  - $vhost"
        done
        return 0
    else
        echo "⚠️  未发现虚拟主机"
        return 0
    fi
}

# 检查用户
check_rabbitmq_users() {
    USERS=$(sudo rabbitmqctl list_users --quiet | wc -l)
    
    if [ "$USERS" -gt 0 ]; then
        echo "✅ 发现 $USERS 个用户"
        return 0
    else
        echo "⚠️  未发现用户"
        return 0
    fi
}

# 检查队列
check_rabbitmq_queues() {
    QUEUES=$(sudo rabbitmqctl list_queues --quiet | wc -l)
    
    if [ "$QUEUES" -gt 0 ]; then
        echo "✅ 发现 $QUEUES 个队列"
        
        # 显示队列统计
        echo "队列统计:"
        sudo rabbitmqctl list_queues name messages consumers --quiet | head -10
        return 0
    else
        echo "⚠️  未发现队列"
        return 0
    fi
}

# 检查交换器
check_rabbitmq_exchanges() {
    EXCHANGES=$(sudo rabbitmqctl list_exchanges --quiet | wc -l)
    
    if [ "$EXCHANGES" -gt 0 ]; then
        echo "✅ 发现 $EXCHANGES 个交换器"
        return 0
    else
        echo "⚠️  未发现交换器"
        return 0
    fi
}

# 检查连接
check_rabbitmq_connections() {
    CONNECTIONS=$(sudo rabbitmqctl list_connections --quiet | wc -l)
    
    if [ "$CONNECTIONS" -gt 0 ]; then
        echo "✅ 发现 $CONNECTIONS 个连接"
        return 0
    else
        echo "ℹ️  当前无活动连接"
        return 0
    fi
}

# 检查通道
check_rabbitmq_channels() {
    CHANNELS=$(sudo rabbitmqctl list_channels --quiet | wc -l)
    
    if [ "$CHANNELS" -gt 0 ]; then
        echo "✅ 发现 $CHANNELS 个通道"
        return 0
    else
        echo "ℹ️  当前无活动通道"
        return 0
    fi
}

# 检查管理插件
check_rabbitmq_management() {
    if sudo rabbitmq-plugins list | grep -q "rabbitmq_management.*\[E\*\]"; then
        echo "✅ RabbitMQ管理插件已启用"
        echo "   管理界面: http://localhost:15672"
        return 0
    else
        echo "⚠️  RabbitMQ管理插件未启用"
        return 0
    fi
}

# 主函数
main() {
    echo "=== RabbitMQ健康检查 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    check_rabbitmq_service || exit_code=1
    check_rabbitmq_node || exit_code=1
    
    echo ""
    check_rabbitmq_vhosts
    check_rabbitmq_users
    check_rabbitmq_queues
    check_rabbitmq_exchanges
    check_rabbitmq_connections
    check_rabbitmq_channels
    
    echo ""
    check_rabbitmq_management
    
    echo ""
    echo "=== 检查完成 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有关键检查通过"
    else
        echo "❌ 部分关键检查失败"
    fi
    
    return $exit_code
}

# 执行检查
main "$@"
```

### 2.4 Prometheus健康检查脚本

#### 2.4.1 Prometheus健康检查脚本

**脚本文件**: `/usr/local/bin/prometheus-health-check.sh`

```bash
#!/bin/bash
# Prometheus健康检查脚本

set -e

PROMETHEUS_URL="http://localhost:9090"

# 检查Prometheus服务状态
check_prometheus_service() {
    if systemctl is-active --quiet prometheus; then
        echo "✅ Prometheus服务运行正常"
        return 0
    else
        echo "❌ Prometheus服务未运行"
        return 1
    fi
}

# 检查Prometheus API
check_prometheus_api() {
    if curl -s "$PROMETHEUS_URL/-/healthy" | grep -q "Prometheus is Healthy"; then
        echo "✅ Prometheus API健康检查通过"
        return 0
    else
        echo "❌ Prometheus API健康检查失败"
        return 1
    fi
}

# 检查Prometheus就绪状态
check_prometheus_ready() {
    if curl -s "$PROMETHEUS_URL/-/ready" | grep -q "Prometheus is Ready"; then
        echo "✅ Prometheus就绪检查通过"
        return 0
    else
        echo "❌ Prometheus就绪检查失败"
        return 1
    fi
}

# 检查目标状态
check_prometheus_targets() {
    TARGETS_JSON=$(curl -s "$PROMETHEUS_URL/api/v1/targets")
    
    # 解析JSON获取目标状态
    UP_TARGETS=$(echo "$TARGETS_JSON" | grep -o '"health":"up"' | wc -l)
    DOWN_TARGETS=$(echo "$TARGETS_JSON" | grep -o '"health":"down"' | wc -l)
    
    echo "📊 监控目标状态:"
    echo "  在线目标: $UP_TARGETS"
    echo "  离线目标: $DOWN_TARGETS"
    
    if [ "$DOWN_TARGETS" -gt 0 ]; then
        echo "⚠️  发现 $DOWN_TARGETS 个离线目标"
        
        # 提取离线目标名称
        echo "离线目标列表:"
        echo "$TARGETS_JSON" | grep -B5 '"health":"down"' | grep '"labels"' | sed 's/.*"instance":"\([^"]*\)".*/\1/' | sort -u
        return 0
    else
        echo "✅ 所有监控目标在线"
        return 0
    fi
}

# 检查规则
check_prometheus_rules() {
    RULES_JSON=$(curl -s "$PROMETHEUS_URL/api/v1/rules")
    
    RULE_COUNT=$(echo "$RULES_JSON" | grep -o '"name"' | wc -l)
    
    if [ "$RULE_COUNT" -gt 0 ]; then
        echo "✅ 发现 $RULE_COUNT 个规则"
        return 0
    else
        echo "⚠️  未发现规则"
        return 0
    fi
}

# 检查告警
check_prometheus_alerts() {
    ALERTS_JSON=$(curl -s "$PROMETHEUS_URL/api/v1/alerts")
    
    FIRING_ALERTS=$(echo "$ALERTS_JSON" | grep -o '"state":"firing"' | wc -l)
    
    if [ "$FIRING_ALERTS" -gt 0 ]; then
        echo "❌ 发现 $FIRING_ALERTS 个触发告警"
        
        # 提取告警名称
        echo "触发告警列表:"
        echo "$ALERTS_JSON" | grep -B5 '"state":"firing"' | grep '"alertname"' | sed 's/.*"alertname":"\([^"]*\)".*/\1/' | sort -u
        return 1
    else
        echo "✅ 无触发告警"
        return 0
    fi
}

# 检查存储
check_prometheus_storage() {
    TSDB_JSON=$(curl -s "$PROMETHEUS_URL/api/v1/status/tsdb")
    
    # 提取头块统计
    HEAD_STATS=$(echo "$TSDB_JSON" | grep -A10 '"headStats"')
    
    if [ -n "$HEAD_STATS" ]; then
        echo "📊 TSDB存储统计:"
        echo "$HEAD_STATS" | grep -E '(numSeries|numLabelPairs|numChunks)' | sed 's/.*"\([^"]*\)":\([^,]*\).*/\1: \2/'
        return 0
    else
        echo "⚠️  无法获取TSDB存储统计"
        return 0
    fi
}

# 主函数
main() {
    echo "=== Prometheus健康检查 ==="
    echo "检查时间: $(date)"
    echo "URL: $PROMETHEUS_URL"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    check_prometheus_service || exit_code=1
    check_prometheus_api || exit_code=1
    check_prometheus_ready || exit_code=1
    
    echo ""
    check_prometheus_targets
    check_prometheus_rules
    check_prometheus_alerts || exit_code=1
    check_prometheus_storage
    
    echo ""
    echo "=== 检查完成 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有关键检查通过"
    else
        echo "❌ 部分关键检查失败"
    fi
    
    return $exit_code
}

# 执行检查
main "$@"
```

### 2.5 一键健康检查脚本

#### 2.5.1 综合健康检查脚本

**脚本文件**: `/usr/local/bin/all-health-check.sh`

```bash
#!/bin/bash
# 一键健康检查脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_info() {
    echo -e "${BLUE}📋 $1${NC}"
}

# 检查目录
check_directory() {
    local dir="$1"
    local description="$2"
    
    if [ -d "$dir" ]; then
        log_success "$description 目录存在: $dir"
        
        # 检查权限
        if [ -w "$dir" ]; then
            log_success "  目录可写"
        else
            log_warning "  目录不可写"
        fi
        
        # 检查磁盘空间
        local free_space=$(df -h "$dir" | awk 'NR==2 {print $4}')
        log_info "  可用空间: $free_space"
    else
        log_error "$description 目录不存在: $dir"
        return 1
    fi
}

# 检查服务
check_service() {
    local service="$1"
    local description="$2"
    
    if systemctl is-active --quiet "$service"; then
        log_success "$description 服务运行正常"
        return 0
    else
        log_error "$description 服务未运行"
        return 1
    fi
}

# 检查端口
check_port() {
    local port="$1"
    local description="$2"
    
    if netstat -tln | grep -q ":$port "; then
        log_success "$description 端口监听正常 (端口: $port)"
        return 0
    else
        log_error "$description 端口未监听 (端口: $port)"
        return 1
    fi
}

# 检查HTTP服务
check_http_service() {
    local url="$1"
    local description="$2"
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        log_success "$description HTTP服务正常 (URL: $url)"
        return 0
    else
        log_error "$description HTTP服务异常 (URL: $url)"
        return 1
    fi
}

# 检查磁盘使用率
check_disk_usage() {
    local threshold="$1"
    
    df -h | grep -v "^Filesystem" | while read line; do
        local filesystem=$(echo "$line" | awk '{print $1}')
        local usage=$(echo "$line" | awk '{print $5}' | sed 's/%//')
        local mount=$(echo "$line" | awk '{print $6}')
        
        if [ "$usage" -ge "$threshold" ]; then
            log_error "磁盘使用率过高: $filesystem ($mount) - $usage%"
        elif [ "$usage" -ge 80 ]; then
            log_warning "磁盘使用率较高: $filesystem ($mount) - $usage%"
        else
            log_success "磁盘使用率正常: $filesystem ($mount) - $usage%"
        fi
    done
}

# 检查内存使用率
check_memory_usage() {
    local total_mem=$(free -m | awk '/^Mem:/{print $2}')
    local used_mem=$(free -m | awk '/^Mem:/{print $3}')
    local usage=$((used_mem * 100 / total_mem))
    
    if [ "$usage" -ge 90 ]; then
        log_error "内存使用率过高: $usage% (${used_mem}M/${total_mem}M)"
        return 1
    elif [ "$usage" -ge 70 ]; then
        log_warning "内存使用率较高: $usage% (${used_mem}M/${total_mem}M)"
        return 0
    else
        log_success "内存使用率正常: $usage% (${used_mem}M/${total_mem}M)"
        return 0
    fi
}

# 检查CPU使用率
check_cpu_usage() {
    local usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 | cut -d'.' -f1)
    
    if [ "$usage" -ge 90 ]; then
        log_error "CPU使用率过高: $usage%"
        return 1
    elif [ "$usage" -ge 70 ]; then
        log_warning "CPU使用率较高: $usage%"
        return 0
    else
        log_success "CPU使用率正常: $usage%"
        return 0
    fi
}

# 检查负载
check_load_average() {
    local load1=$(uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | tr -d ' ')
    local cores=$(nproc)
    local threshold=$(echo "$cores * 1.5" | bc)
    
    if (( $(echo "$load1 > $threshold" | bc -l) )); then
        log_error "系统负载过高: $load1 (CPU核心数: $cores)"
        return 1
    elif (( $(echo "$load1 > $cores" | bc -l) )); then
        log_warning "系统负载较高: $load1 (CPU核心数: $cores)"
        return 0
    else
        log_success "系统负载正常: $load1 (CPU核心数: $cores)"
        return 0
    fi
}

# 检查网络连接
check_network() {
    if ping -c 1 -W 2 8.8.8.8 > /dev/null 2>&1; then
        log_success "外网连接正常"
        return 0
    else
        log_error "外网连接失败"
        return 1
    fi
}

# 检查时间同步
check_ntp() {
    if timedatectl status | grep -q "synchronized: yes"; then
        log_success "时间同步正常"
        return 0
    else
        log_warning "时间未同步"
        return 0
    fi
}

# 检查防火墙
check_firewall() {
    if command -v ufw > /dev/null 2>&1; then
        if ufw status | grep -q "Status: active"; then
            log_info "防火墙状态: 启用"
            return 0
        else
            log_warning "防火墙状态: 禁用"
            return 0
        fi
    elif command -v firewall-cmd > /dev/null 2>&1; then
        if firewall-cmd --state 2>/dev/null | grep -q "running"; then
            log_info "防火墙状态: 启用 (firewalld)"
            return 0
        else
            log_warning "防火墙状态: 禁用"
            return 0
        fi
    else
        log_info "未检测到防火墙"
        return 0
    fi
}

# 检查Docker
check_docker() {
    if command -v docker > /dev/null 2>&1; then
        if systemctl is-active --quiet docker; then
            log_success "Docker服务运行正常"
            
            # 检查Docker容器
            local container_count=$(docker ps -q | wc -l)
            log_info "运行中的容器: $container_count"
            return 0
        else
            log_warning "Docker服务未运行"
            return 0
        fi
    else
        log_info "Docker未安装"
        return 0
    fi
}

# 主函数
main() {
    echo "=== 一键健康检查 ==="
    echo "检查时间: $(date)"
    echo "主机名: $(hostname)"
    echo "操作系统: $(lsb_release -d 2>/dev/null | cut -f2- || cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '\"')"
    echo "内核版本: $(uname -r)"
    echo ""
    
    local exit_code=0
    
    log_info "=== 系统资源检查 ==="
    check_disk_usage 90
    check_memory_usage || exit_code=1
    check_cpu_usage || exit_code=1
    check_load_average || exit_code=1
    check_network || exit_code=1
    check_ntp
    check_firewall
    
    echo ""
    log_info "=== 目录检查 ==="
    check_directory "/var/backups/postgresql/ai-ready-test" "PostgreSQL备份"
    check_directory "/var/backups/redis/ai-ready-test" "Redis备份"
    check_directory "/var/backups/rabbitmq/ai-ready-test" "RabbitMQ备份"
    check_directory "/var/log/ai-ready-test" "应用日志"
    
    echo ""
    log_info "=== 服务检查 ==="
    check_service "postgresql" "PostgreSQL"
    check_service "redis-ai-ready-test.service" "Redis测试实例"
    check_service "rabbitmq-server" "RabbitMQ"
    check_service "prometheus" "Prometheus"
    check_service "alertmanager" "AlertManager"
    check_service "grafana-server" "Grafana"
    
    echo ""
    log_info "=== 端口检查 ==="
    check_port "5432" "PostgreSQL"
    check_port "6380" "Redis测试实例"
    check_port "5672" "RabbitMQ AMQP"
    check_port "15672" "RabbitMQ管理界面"
    check_port "9090" "Prometheus"
    check_port "9093" "AlertManager"
    check_port "3000" "Grafana"
    check_port "9100" "Node Exporter"
    check_port "9187" "PostgreSQL Exporter"
    check_port "9121" "Redis Exporter"
    
    echo ""
    log_info "=== HTTP服务检查 ==="
    check_http_service "http://localhost:9090" "Prometheus"
    check_http_service "http://localhost:9093" "AlertManager"
    check_http_service "http://localhost:3000" "Grafana"
    
    echo ""
    log_info "=== 其他检查 ==="
    check_docker
    
    echo ""
    echo "=== 检查完成 ==="
    echo "总结时间: $(date)"
    echo ""
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ 所有关键检查通过${NC}"
        echo "测试环境健康状态: 良好"
    else
        echo -e "${RED}❌ 部分关键检查失败${NC}"
        echo "测试环境健康状态: 需要关注"
    fi
    
    echo ""
    echo "建议:"
    echo "1. 定期运行健康检查脚本"
    echo "2. 监控关键指标"
    echo "3. 及时处理告警"
    echo "4. 定期备份数据"
    
    return $exit_code
}

# 执行检查
main "$@"
```

## 3. 健康检查命令

### 3.1 Docker容器健康检查命令

#### 3.1.1 检查所有容器状态

```bash
# 查看所有容器状态
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看运行中的容器
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看容器资源使用情况
docker stats --no-stream

# 查看容器日志（最近100行）
docker logs --tail 100 <container_name>
```

#### 3.1.2 检查特定容器

```bash
# 检查PostgreSQL容器
docker exec postgres-container pg_isready -U postgres

# 检查Redis容器
docker exec redis-container redis-cli ping

# 检查RabbitMQ容器
docker exec rabbitmq-container rabbitmqctl status

# 检查容器健康状态
docker inspect --format='{{.State.Health.Status}}' <container_name>
```

#### 3.1.3 容器资源监控

```bash
# 查看容器CPU使用率
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}"

# 查看容器内存使用
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}"

# 查看容器网络IO
docker stats --no-stream --format "table {{.Name}}\t{{.NetIO}}"

# 查看容器磁盘IO
docker stats --no-stream --format "table {{.Name}}\t{{.BlockIO}}"
```

### 3.2 服务状态检查命令

#### 3.2.1 系统服务检查

```bash
# 检查服务状态
systemctl status <service_name>

# 检查服务是否运行
systemctl is-active <service_name>

# 检查服务是否启用
systemctl is-enabled <service_name>

# 查看所有服务状态
systemctl list-units --type=service --state=running

# 查看失败的服务
systemctl list-units --type=service --state=failed
```

#### 3.2.2 进程检查

```bash
# 查看进程
ps aux | grep <process_name>

# 查看进程树
pstree -p

# 查看CPU使用率最高的进程
ps aux --sort=-%cpu | head -10

# 查看内存使用率最高的进程
ps aux --sort=-%mem | head -10
```

#### 3.2.3 端口检查

```bash
#