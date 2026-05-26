#!/bin/bash
# 配置文件生成脚本
# 支持从模板生成配置文件，支持变量替换和验证

set -euo pipefail

# 颜色输出
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 路径配置
readonly CONFIG_DIR="${CONFIG_DIR:-./config}"
readonly TEMPLATE_DIR="${TEMPLATE_DIR:-./config/templates}"
readonly OUTPUT_DIR="${OUTPUT_DIR:-./config/generated}"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
配置文件生成脚本 v1.0

用法: ./config-generator.sh <command> [options]

命令:
  init             初始化配置文件目录结构
  list             列出所有可用模板
  generate <type> <env> 生成配置文件
  validate <config> 验证配置文件
  diff <config1> <config2> 比较配置文件差异
  backup <config>  备份配置文件
  audit            审计配置文件完整性

配置文件类型:
  application    应用配置文件 (application.yml)
  database       数据库配置文件 (database.yml)
  redis         Redis配置文件 (redis.yml)
  rabbitmq      RabbitMQ配置文件 (rabbitmq.yml)
  security      安全配置文件 (security.yml)
  monitoring    监控配置文件 (monitoring.yml)

示例:
  ./config-generator.sh init
  ./config-generator.sh list
  ./config-generator.sh generate application development
  ./config-generator.sh validate application.yml

配置目录: ${CONFIG_DIR}
模板目录: ${TEMPLATE_DIR}
输出目录: ${OUTPUT_DIR}
EOF
}

# 检查必需命令
check_prerequisites() {
    local required_commands=(yq jq diff)
    local missing_commands=()
    
    for cmd in "${required_commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            missing_commands+=("$cmd")
        fi
    done
    
    if [ ${#missing_commands[@]} -gt 0 ]; then
        log_error "缺少必需命令: ${missing_commands[*]}"
        log_info "请安装:"
        log_info "  - yq: YAML处理器 (pip install yq)"
        log_info "  - jq: JSON处理器 (apt-get install jq)"
        log_info "  - diff: 文件差异比较 (系统自带)"
        exit 1
    fi
}

# 初始化目录结构
init_config_structure() {
    log_info "初始化配置文件目录结构..."
    
    mkdir -p "${CONFIG_DIR}"
    mkdir -p "${TEMPLATE_DIR}"
    mkdir -p "${OUTPUT_DIR}"
    mkdir -p "${CONFIG_DIR}/backup"
    mkdir -p "${CONFIG_DIR}/audit"
    
    # 创建基础模板
    create_application_template
    create_database_template
    create_redis_template
    create_security_template
    create_monitoring_template
    
    log_success "配置文件目录结构初始化完成"
}

# 创建应用配置模板
create_application_template() {
    local template_file="${TEMPLATE_DIR}/application.yml.template"
    
    cat > "$template_file" << 'EOF'
# 应用配置文件模板
# 环境: ${ENV}
# 生成时间: ${TIMESTAMP}

server:
  port: ${APP_PORT:-8080}
  servlet:
    context-path: ${CONTEXT_PATH:-/api}
    session:
      timeout: ${SESSION_TIMEOUT:-30m}

spring:
  application:
    name: ${APP_NAME:-AI-Ready}
  profiles:
    active: ${ENV:-development}
  
  # MVC配置
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher
  resources:
    static-locations: classpath:/static/
  
  # 文件上传
  servlet:
    multipart:
      max-file-size: ${MAX_FILE_SIZE:-10MB}
      max-request-size: ${MAX_REQUEST_SIZE:-100MB}

# 日志配置
logging:
  level:
    root: ${LOG_LEVEL:-INFO}
    cn.aiedge: ${APP_LOG_LEVEL:-DEBUG}
    org.springframework: WARN
  file:
    name: ${LOG_FILE:-logs/app.log}
    max-size: ${LOG_MAX_SIZE:-10MB}
    max-history: ${LOG_MAX_HISTORY:-30}
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 外部服务
external:
  services:
    api:
      timeout: ${API_TIMEOUT:-30}
      retry:
        max-attempts: ${RETRY_MAX_ATTEMPTS:-3}
        backoff-delay: ${RETRY_BACKOFF_DELAY:-1000}
    cache:
      enabled: ${CACHE_ENABLED:-true}
      ttl: ${CACHE_TTL:-300}
  
  # 监控配置
  monitoring:
    enabled: ${MONITORING_ENABLED:-true}
    endpoint: ${MONITORING_ENDPOINT:-/actuator}
    metrics:
      enabled: ${METRICS_ENABLED:-true}
      export:
        prometheus:
          enabled: ${PROMETHEUS_ENABLED:-true}
EOF

    log_info "创建应用配置模板: $template_file"
}

# 创建数据库配置模板
create_database_template() {
    local template_file="${TEMPLATE_DIR}/database.yml.template"
    
    cat > "$template_file" << 'EOF'
# 数据库配置文件模板
# 环境: ${ENV}

spring:
  datasource:
    # 主数据源
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      connection-timeout: ${CONNECTION_TIMEOUT:-30000}
      maximum-pool-size: ${MAX_POOL_SIZE:-10}
      minimum-idle: ${MIN_IDLE:-5}
      idle-timeout: ${IDLE_TIMEOUT:-600000}
      max-lifetime: ${MAX_LIFETIME:-1800000}
      pool-name: ${POOL_NAME:-HikariPool}
  
  # JPA配置
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: ${DDL_AUTO:-validate}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: ${FORMAT_SQL:-true}
        show_sql: ${SHOW_SQL:-false}
        jdbc:
          batch_size: ${BATCH_SIZE:-20}
          fetch_size: ${FETCH_SIZE:-100}
        connection:
          provider_disables_autocommit: true

# MyBatis配置
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
    cache-enabled: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# 多数据源配置（如果需要）
datasource:
  secondary:
    enabled: ${SECONDARY_ENABLED:-false}
    url: jdbc:postgresql://${DB_HOST_SECONDARY}:${DB_PORT_SECONDARY}/${DB_NAME_SECONDARY}
    username: ${DB_USER_SECONDARY}
    password: ${DB_PASSWORD_SECONDARY}
EOF

    log_info "创建数据库配置模板: $template_file"
}

# 创建Redis配置模板
create_redis_template() {
    local template_file="${TEMPLATE_DIR}/redis.yml.template"
    
    cat > "$template_file" << 'EOF'
# Redis配置文件模板
# 环境: ${ENV}

spring:
  redis:
    host: ${REDIS_HOST:-localhost}
    port: ${REDIS_PORT:-6379}
    password: ${REDIS_PASSWORD:-}
    database: ${REDIS_DATABASE:-0}
    timeout: ${REDIS_TIMEOUT:-3000}
    lettuce:
      pool:
        max-active: ${REDIS_MAX_ACTIVE:-8}
        max-idle: ${REDIS_MAX_IDLE:-8}
        min-idle: ${REDIS_MIN_IDLE:-0}
        max-wait: ${REDIS_MAX_WAIT:-3000}
      shutdown-timeout: ${REDIS_SHUTDOWN_TIMEOUT:-100}
  
  cache:
    type: redis
    redis:
      time-to-live: ${CACHE_TTL:-300000}
      cache-null-values: ${CACHE_NULL_VALUES:-false}
      key-prefix: ${CACHE_KEY_PREFIX:-cache:}
      use-key-prefix: true

# Redis Sentinel配置（如果需要）
redis:
  sentinel:
    enabled: ${SENTINEL_ENABLED:-false}
    master: ${SENTINEL_MASTER:-mymaster}
    nodes: ${SENTINEL_NODES:-localhost:26379,localhost:26380,localhost:26381}

# Redis集群配置（如果需要）
redis:
  cluster:
    enabled: ${CLUSTER_ENABLED:-false}
    nodes: ${CLUSTER_NODES:-localhost:7000,localhost:7001,localhost:7002}
    max-redirects: ${CLUSTER_MAX_REDIRECTS:-3}
EOF

    log_info "创建Redis配置模板: $template_file"
}

# 创建安全配置模板
create_security_template() {
    local template_file="${TEMPLATE_DIR}/security.yml.template"
    
    cat > "$template_file" << 'EOF'
# 安全配置文件模板
# 环境: ${ENV}

security:
  # JWT配置
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:-86400000}
    header: ${JWT_HEADER:-Authorization}
    prefix: ${JWT_PREFIX:-Bearer }
    issuer: ${JWT_ISSUER:-ai-ready}
  
  # CORS配置
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:-*}
    allowed-methods: ${CORS_ALLOWED_METHODS:-GET,POST,PUT,DELETE,PATCH,OPTIONS}
    allowed-headers: ${CORS_ALLOWED_HEADERS:-*}
    allow-credentials: ${CORS_ALLOW_CREDENTIALS:-true}
    max-age: ${CORS_MAX_AGE:-3600}
  
  # 密码策略
  password:
    strength: ${PASSWORD_STRENGTH:-medium}
    min-length: ${PASSWORD_MIN_LENGTH:-8}
    max-length: ${PASSWORD_MAX_LENGTH:-32}
    require-special-chars: ${PASSWORD_REQUIRE_SPECIAL_CHARS:-true}
    require-numbers: ${PASSWORD_REQUIRE_NUMBERS:-true}
    require-uppercase: ${PASSWORD_REQUIRE_UPPERCASE:-true}
  
  # 会话管理
  session:
    timeout: ${SESSION_TIMEOUT:-1800}
    max-sessions: ${MAX_SESSIONS:-1}
    concurrent-sessions: ${CONCURRENT_SESSIONS:-false}
  
  # 加密配置
  encryption:
    algorithm: ${ENCRYPTION_ALGORITHM:-AES/GCM/NoPadding}
    key: ${ENCRYPTION_KEY}
    iv: ${ENCRYPTION_IV}
  
  # 审计日志
  audit:
    enabled: ${AUDIT_ENABLED:-true}
    log-success: ${AUDIT_LOG_SUCCESS:-true}
    log-failure: ${AUDIT_LOG_FAILURE:-true}
EOF

    log_info "创建安全配置模板: $template_file"
}

# 创建监控配置模板
create_monitoring_template() {
    local template_file="${TEMPLATE_DIR}/monitoring.yml.template"
    
    cat > "$template_file" << 'EOF'
# 监控配置文件模板
# 环境: ${ENV}

management:
  endpoints:
    web:
      exposure:
        include: ${ENDPOINTS_INCLUDE:-health,info,metrics,prometheus}
      base-path: ${ENDPOINTS_BASE_PATH:-/actuator}
    jmx:
      exposure:
        include: ${JMX_ENDPOINTS_INCLUDE:-*}
  
  endpoint:
    health:
      show-details: ${HEALTH_SHOW_DETAILS:-always}
      probes:
        enabled: ${PROBES_ENABLED:-true}
    metrics:
      enabled: ${METRICS_ENABLED:-true}
  
  metrics:
    export:
      prometheus:
        enabled: ${PROMETHEUS_ENABLED:-true}
        step: ${PROMETHEUS_STEP:-1m}
      simple:
        enabled: ${SIMPLE_METRICS_ENABLED:-false}
    tags:
      application: ${APP_NAME:-AI-Ready}
      environment: ${ENV}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: ${SLA_HTTP_SERVER_REQUESTS:-100ms,200ms,500ms}
  
  # 追踪配置
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:-1.0}
  
  # 日志监控
  loggers:
    enabled: ${LOGGERS_ENABLED:-true}

# 自定义监控配置
monitoring:
  alert:
    enabled: ${ALERT_ENABLED:-true}
    rules:
      - name: high_cpu_usage
        condition: ${HIGH_CPU_CONDITION:-system.cpu.usage > 0.8}
        severity: ${HIGH_CPU_SEVERITY:-HIGH}
        duration: ${HIGH_CPU_DURATION:-5m}
      - name: high_memory_usage
        condition: ${HIGH_MEMORY_CONDITION:-jvm.memory.used / jvm.memory.max > 0.8}
        severity: ${HIGH_MEMORY_SEVERITY:-HIGH}
        duration: ${HIGH_MEMORY_DURATION:-5m}
  
  dashboard:
    enabled: ${DASHBOARD_ENABLED:-true}
    refresh-interval: ${DASHBOARD_REFRESH_INTERVAL:-30s}
  
  reporting:
    enabled: ${REPORTING_ENABLED:-true}
    schedule: ${REPORTING_SCHEDULE:-0 0 9 * * ?}  # 每天9点
EOF

    log_info "创建监控配置模板: $template_file"
}

# 列出所有模板
list_templates() {
    log_info "可用配置模板:"
    
    if [ ! -d "$TEMPLATE_DIR" ]; then
        log_error "模板目录不存在: $TEMPLATE_DIR"
        return 1
    fi
    
    local templates=$(find "$TEMPLATE_DIR" -name "*.template" -type f | sort)
    
    if [ -z "$templates" ]; then
        log_info "没有找到模板文件"
        return 0
    fi
    
    for template in $templates; do
        local template_name=$(basename "$template" .template)
        local file_size=$(stat -c%s "$template" 2>/dev/null || stat -f%z "$template")
        local variable_count=$(grep -o '\${[^}]*}' "$template" | sort -u | wc -l)
        
        echo -e "  ${GREEN}${template_name}${NC}:"
        echo "    文件: $template"
        echo "    大小: $((file_size / 1024))KB"
        echo "    变量: $variable_count 个"
        
        # 显示示例变量
        if [ "$variable_count" -gt 0 ]; then
            echo "    示例: $(grep -o '\${[^}]*}' "$template" | sort -u | head -3 | tr '\n' ' ')"
        fi
        echo
    done
}

# 生成配置文件
generate_config() {
    local config_type="$1"
    local env_name="$2"
    local template_file="${TEMPLATE_DIR}/${config_type}.yml.template"
    local output_file="${OUTPUT_DIR}/${config_type}-${env_name}.yml"
    
    if [ ! -f "$template_file" ]; then
        log_error "模板文件不存在: $template_file"
        log_info "可用模板:"
        list_templates
        return 1
    fi
    
    log_info "生成配置文件: $config_type (环境: $env_name)"
    
    # 加载环境变量
    local env_file="./env/.env.$env_name"
    if [ ! -f "$env_file" ]; then
        log_warn "环境文件不存在: $env_file，使用默认值"
    else
        log_info "加载环境变量: $env_file"
        # 导出环境变量
        set -a
        source "$env_file" 2>/dev/null || true
        set +a
    fi
    
    # 设置默认变量
    export ENV="$env_name"
    export TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    export APP_NAME="${APP_NAME:-AI-Ready}"
    
    # 生成配置文件
    log_info "从模板生成: $template_file"
    
    # 使用envsubst替换变量（如果可用）
    if command -v envsubst &> /dev/null; then
        envsubst < "$template_file" > "$output_file"
        log_info "使用envsubst替换变量"
    else
        # 简单的变量替换
        cp "$template_file" "$output_file"
        for var in $(grep -o '\${[^}]*}' "$template_file" | sort -u | sed 's/\${//;s/}//'); do
            local value="${!var:-}"
            if [ -n "$value" ]; then
                sed -i "s#\${$var}#$value#g" "$output_file"
            fi
        done
        log_info "使用sed替换变量"
    fi
    
    # 验证生成的配置文件
    if validate_config "$output_file"; then
        log_success "配置文件已生成: $output_file"
        log_info "文件大小: $(stat -c%s "$output_file" 2>/dev/null || stat -f%z "$output_file") 字节"
    else
        log_warn "配置文件生成完成但验证失败: $output_file"
    fi
}

# 验证配置文件
validate_config() {
    local config_file="$1"
    
    if [ ! -f "$config_file" ]; then
        log_error "配置文件不存在: $config_file"
        return 1
    fi
    
    log_info "验证配置文件: $config_file"
    
    local validation_errors=0
    local file_ext="${config_file##*.}"
    
    # 根据文件类型验证
    case "$file_ext" in
        yml|yaml)
            if ! yq eval '.' "$config_file" > /dev/null 2>&1; then
                log_error "YAML格式无效"
                validation_errors=$((validation_errors + 1))
            fi
            ;;
        json)
            if ! jq '.' "$config_file" > /dev/null 2>&1; then
                log_error "JSON格式无效"
                validation_errors=$((validation_errors + 1))
            fi
            ;;
        properties)
            # 简单的properties验证
            if grep -q "^\s*=\|=\s*$" "$config_file"; then
                log_error "Properties格式错误：空键或空值"
                validation_errors=$((validation_errors + 1))
            fi
            ;;
    esac
    
    # 检查未替换的变量
    if grep -q '\${' "$config_file"; then
        local unresolved_vars=$(grep -o '\${[^}]*}' "$config_file" | sort -u)
        log_warn "发现未替换的变量:"
        echo "$unresolved_vars"
        validation_errors=$((validation_errors + 1))
    fi
    
    # 检查敏感信息
    if grep -qi "password\|secret\|key\|token\|credential" "$config_file"; then
        log_warn "检测到可能包含敏感信息的配置"
    fi
    
    if [ $validation_errors -eq 0 ]; then
        log_success "配置文件验证通过"
        return 0
    else
        log_error "配置文件验证失败，发现 $validation_errors 个问题"
        return 1
    fi
}

# 比较配置文件差异
diff_configs() {
    local config1="$1"
    local config2="$2"
    
    if [ ! -f "$config1" ] || [ ! -f "$config2" ]; then
        log_error "配置文件不存在"
        return 1
    fi
    
    log_info "比较配置文件:"
    log_info "  文件1: $config1"
    log_info "  文件2: $config2"
    
    # 使用diff比较差异
    if command -v colordiff &> /dev/null; then
        colordiff -u "$config1" "$config2" || true
    else
        diff -u "$config1" "$config2" || true
    fi
    
    # 统计差异
    local diff_count=$(diff -u "$config1" "$config2" | grep -E "^[+-]" | grep -v "^[+-]{3}" | wc -l)
    log_info "差异数量: $diff_count"
}

# 备份配置文件
backup_config() {
    local config_file="$1"
    local backup_dir="${CONFIG_DIR}/backup"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${backup_dir}/$(basename "$config_file")_${timestamp}.bak"
    
    if [ ! -f "$config_file" ]; then
        log_error "配置文件不存在: $config_file"
        return 1
    fi
    
    mkdir -p "$backup_dir"
    
    # 创建备份
    cp "$config_file" "$backup_file"
    
    # 记录备份元数据
    local metadata_file="${backup_file}.meta"
    cat > "$metadata_file" << EOF
配置文件备份元数据
备份时间: $(date)
源文件: $config_file
文件大小: $(stat -c%s "$config_file" 2>/dev/null || stat -f%z "$config_file")
MD5校验和: $(md5sum "$config_file" | cut -d' ' -f1)
环境: $(grep -o "环境: [^#]*" "$config_file" | head -1 || echo "未知")
EOF
    
    log_success "配置文件已备份: $backup_file"
    log_info "备份元数据: $metadata_file"
    
    # 清理旧备份（保留最近20个）
    find "$backup_dir" -name "*$(basename "$config_file")*.bak*" -type f | sort -r | tail -n +21 | xargs rm -f 2>/dev/null || true
}

# 审计配置文件完整性
audit_configs() {
    local audit_dir="${CONFIG_DIR}/audit"
    local audit_file="${audit_dir}/audit_$(date +%Y%m%d_%H%M%S).json"
    
    mkdir -p "$audit_dir"
    
    log_info "开始审计配置文件完整性..."
    
    local audit_data="{\"timestamp\": \"$(date -Iseconds)\", \"configs\": []}"
    
    # 审计所有配置文件
    for config in "$OUTPUT_DIR"/*.yml "$OUTPUT_DIR"/*.yaml "$OUTPUT_DIR"/*.json "$OUTPUT_DIR"/*.properties; do
        [ -f "$config" ] || continue
        
        local config_name=$(basename "$config")
        local file_size=$(stat -c%s "$config" 2>/dev/null || stat -f%z "$config")
        local md5_sum=$(md5sum "$config" | cut -d' ' -f1)
        local validation_result="passed"
        
        if ! validate_config "$config" &> /dev/null; then
            validation_result="failed"
        fi
        
        # 添加到审计数据
        audit_data=$(echo "$audit_data" | jq --arg name "$config_name" \
            --arg size "$file_size" \
            --arg md5 "$md5_sum" \
            --arg result "$validation_result" \
            --arg path "$config" \
            '.configs += [{"name": $name, "size": $size|tonumber, "md5": $md5, "validation": $result, "path": $path}]')
    done
    
    # 写入审计文件
    echo "$audit_data" | jq '.' > "$audit_file"
    
    log_success "配置文件审计完成: $audit_file"
    
    # 生成审计报告
    local total_configs=$(echo "$audit_data" | jq '.configs | length')
    local passed_configs=$(echo "$audit_data" | jq '[.configs[] | select(.validation == "passed")] | length')
    local failed_configs=$((total_configs - passed_configs))
    
    echo "=== 审计报告 ==="
    echo "总配置文件数: $total_configs"
    echo "验证通过: $passed_configs"
    echo "验证失败: $failed_configs"
    echo "审计文件: $audit_file"
    
    if [ "$failed_configs" -gt 0 ]; then
        log_error "发现验证失败的配置文件"
        return 1
    fi
}

# 主函数
main() {
    check_prerequisites
    
    local command="${1:-help}"
    
    case "$command" in
        init)
            init_config_structure
            ;;
        list)
            list_templates
            ;;
        generate)
            if [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
                log_error "请指定配置类型和环境名称"
                show_help
                exit 1
            fi
            generate_config "$2" "$3"
            ;;
        validate)
            if [ -z "${2:-}" ]; then
                log_error "请指定配置文件路径"
                show_help
                exit 1
            fi
            validate_config "$2"
            ;;
        diff)
            if [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
                log_error "请指定两个配置文件路径"
                show_help
                exit 1
            fi
            diff_configs "$2" "$3"
            ;;
        backup)
            if [ -z "${2:-}" ]; then
                log_error "请指定配置文件路径"
                show_help
                exit 1
            fi
            backup_config "$2"
            ;;
        audit)
            audit_configs
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    main "$@"
fi