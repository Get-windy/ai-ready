#!/bin/bash
# 配置文件验证脚本
# 支持多种配置文件的格式、语法和内容验证

set -euo pipefail

# 颜色输出
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 路径配置
readonly CONFIG_DIR="${CONFIG_DIR:-./config}"
readonly RULES_DIR="${RULES_DIR:-./config/rules}"

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
配置文件验证脚本 v1.0

用法: ./config-validator.sh <command> [options]

命令:
  init             初始化验证规则目录结构
  validate <file>  验证单个配置文件
  validate-all     验证所有配置文件
  check-syntax     检查配置文件语法
  check-security   检查安全配置
  check-consistency 检查配置一致性
  generate-report  生成验证报告
  list-rules       列出所有验证规则

验证规则:
  1. 语法验证 (YAML/JSON/PROPERTIES)
  2. 格式验证 (缩进、注释、键名)
  3. 内容验证 (必需字段、值范围、类型)
  4. 安全验证 (敏感信息、权限)
  5. 一致性验证 (跨文件一致性)

示例:
  ./config-validator.sh init
  ./config-validator.sh validate application.yml
  ./config-validator.sh validate-all
  ./config-validator.sh check-security
  ./config-validator.sh generate-report

配置目录: ${CONFIG_DIR}
规则目录: ${RULES_DIR}
EOF
}

# 检查必需命令
check_prerequisites() {
    local required_commands=(yq jq yamllint)
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
        log_info "  - yamllint: YAML语法检查 (pip install yamllint)"
        exit 1
    fi
}

# 初始化验证规则
init_validation_rules() {
    log_info "初始化验证规则目录结构..."
    
    mkdir -p "${RULES_DIR}"
    mkdir -p "${RULES_DIR}/syntax"
    mkdir -p "${RULES_DIR}/security"
    mkdir -p "${RULES_DIR}/content"
    mkdir -p "${RULES_DIR}/consistency"
    
    # 创建语法验证规则
    create_syntax_rules
    
    # 创建安全验证规则
    create_security_rules
    
    # 创建内容验证规则
    create_content_rules
    
    # 创建一致性验证规则
    create_consistency_rules
    
    log_success "验证规则目录结构初始化完成"
}

# 创建语法验证规则
create_syntax_rules() {
    local rules_file="${RULES_DIR}/syntax/rules.yml"
    
    cat > "$rules_file" << 'EOF'
# 语法验证规则

yaml:
  # YAML语法规则
  rules:
    - name: valid_yaml_syntax
      description: "YAML语法必须有效"
      severity: ERROR
      check: "yamllint"
    
    - name: consistent_indentation
      description: "缩进必须一致（2个空格）"
      severity: WARNING
      check: "indentation"
      pattern: "^[ ]{2,}[^ ]"
    
    - name: no_tabs
      description: "禁止使用制表符"
      severity: ERROR
      check: "no_tabs"
      pattern: "\t"
    
    - name: trailing_spaces
      description: "禁止行尾空格"
      severity: WARNING
      check: "trailing_spaces"
      pattern: "[ ]+$"
    
    - name: line_length
      description: "行长度不超过120字符"
      severity: WARNING
      check: "line_length"
      max_length: 120

json:
  # JSON语法规则
  rules:
    - name: valid_json_syntax
      description: "JSON语法必须有效"
      severity: ERROR
      check: "jq_parse"
    
    - name: no_trailing_commas
      description: "禁止尾随逗号"
      severity: ERROR
      check: "no_trailing_commas"
      pattern: ",\s*[}\]]"

properties:
  # Properties语法规则
  rules:
    - name: valid_properties_syntax
      description: "Properties语法必须有效"
      severity: ERROR
      check: "properties_syntax"
    
    - name: no_empty_keys
      description: "禁止空键名"
      severity: ERROR
      check: "no_empty_keys"
      pattern: "^[=]"
    
    - name: no_empty_values
      description: "禁止空值"
      severity: WARNING
      check: "no_empty_values"
      pattern: "=\s*$"

common:
  # 通用规则
  rules:
    - name: valid_encoding
      description: "必须使用UTF-8编码"
      severity: ERROR
      check: "encoding"
      encoding: "UTF-8"
    
    - name: no_bom
      description: "禁止BOM标记"
      severity: WARNING
      check: "no_bom"
    
    - name: line_endings
      description: "使用Unix换行符(LF)"
      severity: WARNING
      check: "line_endings"
      line_ending: "LF"
EOF

    log_info "创建语法验证规则: $rules_file"
}

# 创建安全验证规则
create_security_rules() {
    local rules_file="${RULES_DIR}/security/rules.yml"
    
    cat > "$rules_file" << 'EOF'
# 安全验证规则

sensitive_data:
  # 敏感数据规则
  rules:
    - name: no_hardcoded_passwords
      description: "禁止硬编码密码"
      severity: CRITICAL
      check: "hardcoded_password"
      patterns:
        - "password.*=.*[^${]"
        - "passwd.*=.*[^${]"
        - "secret.*=.*[^${]"
        - "key.*=.*[^${]"
    
    - name: no_weak_passwords
      description: "禁止弱密码"
      severity: HIGH
      check: "weak_password"
      patterns:
        - "password.*=.*123456"
        - "password.*=.*password"
        - "password.*=.*admin"
        - "password.*=.*qwerty"
    
    - name: no_plaintext_credentials
      description: "禁止明文存储凭证"
      severity: HIGH
      check: "plaintext_credentials"
      patterns:
        - "username.*=.*[^${]"
        - "user.*=.*[^${]"
        - "credential.*=.*[^${]"
        - "token.*=.*[^${]"

permissions:
  # 权限规则
  rules:
    - name: no_excessive_permissions
      description: "禁止过度权限"
      severity: MEDIUM
      check: "excessive_permissions"
      patterns:
        - "permission.*=.*admin"
        - "role.*=.*root"
        - "access.*=.*all"
    
    - name: secure_defaults
      description: "安全默认值"
      severity: MEDIUM
      check: "secure_defaults"
      patterns:
        - "debug.*=.*true"
        - "trace.*=.*true"
        - "verbose.*=.*true"

encryption:
  # 加密规则
  rules:
    - name: encryption_required
      description: "敏感数据必须加密"
      severity: HIGH
      check: "encryption_required"
      fields:
        - "jwt.secret"
        - "encryption.key"
        - "database.password"
        - "redis.password"
    
    - name: weak_encryption
      description: "禁止弱加密算法"
      severity: HIGH
      check: "weak_encryption"
      patterns:
        - "algorithm.*=.*DES"
        - "algorithm.*=.*RC4"
        - "algorithm.*=.*MD5"

network:
  # 网络安全规则
  rules:
    - name: insecure_protocols
      description: "禁止不安全协议"
      severity: HIGH
      check: "insecure_protocols"
      patterns:
        - "http://"
        - "ftp://"
        - "telnet://"
    
    - name: localhost_access
      description: "禁止公开访问localhost"
      severity: MEDIUM
      check: "localhost_access"
      patterns:
        - "host.*=.*localhost"
        - "host.*=.*127.0.0.1"
EOF

    log_info "创建安全验证规则: $rules_file"
}

# 创建内容验证规则
create_content_rules() {
    local rules_file="${RULES_DIR}/content/rules.yml"
    
    cat > "$rules_file" << 'EOF'
# 内容验证规则

required_fields:
  # 必需字段规则
  rules:
    - name: required_database_fields
      description: "数据库配置必需字段"
      severity: ERROR
      check: "required_fields"
      fields:
        - "database.host"
        - "database.port"
        - "database.name"
        - "database.username"
      file_pattern: "*database*"
    
    - name: required_server_fields
      description: "服务器配置必需字段"
      severity: ERROR
      check: "required_fields"
      fields:
        - "server.port"
        - "server.host"
      file_pattern: "*application*"
    
    - name: required_security_fields
      description: "安全配置必需字段"
      severity: ERROR
      check: "required_fields"
      fields:
        - "security.jwt.secret"
        - "security.cors.allowed-origins"
      file_pattern: "*security*"

value_ranges:
  # 值范围规则
  rules:
    - name: port_range
      description: "端口必须在有效范围"
      severity: ERROR
      check: "value_range"
      field: "server.port"
      min: 1024
      max: 65535
    
    - name: timeout_range
      description: "超时时间必须在合理范围"
      severity: WARNING
      check: "value_range"
      field: "timeout"
      min: 1
      max: 300
    
    - name: pool_size_range
      description: "连接池大小必须在合理范围"
      severity: WARNING
      check: "value_range"
      field: "pool.size"
      min: 1
      max: 100

data_types:
  # 数据类型规则
  rules:
    - name: numeric_type
      description: "数值字段必须是数字"
      severity: ERROR
      check: "data_type"
      fields:
        - "port"
        - "timeout"
        - "size"
        - "count"
      type: "number"
    
    - name: boolean_type
      description: "布尔字段必须是true/false"
      severity: ERROR
      check: "data_type"
      fields:
        - "enabled"
        - "debug"
        - "verbose"
      type: "boolean"
    
    - name: string_type
      description: "字符串字段必须是文本"
      severity: WARNING
      check: "data_type"
      fields:
        - "name"
        - "host"
        - "path"
      type: "string"

format:
  # 格式规则
  rules:
    - name: url_format
      description: "URL必须符合格式"
      severity: ERROR
      check: "format"
      fields:
        - "url"
        - "endpoint"
        - "host"
      pattern: "^(http|https)://[a-zA-Z0-9.-]+(:[0-9]+)?(/[a-zA-Z0-9._-]*)*$"
    
    - name: email_format
      description: "邮箱必须符合格式"
      severity: ERROR
      check: "format"
      fields:
        - "email"
        - "contact"
      pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    
    - name: ip_format
      description: "IP地址必须符合格式"
      severity: ERROR
      check: "format"
      fields:
        - "ip"
        - "host"
      pattern: "^([0-9]{1,3}\.){3}[0-9]{1,3}$"
EOF

    log_info "创建内容验证规则: $rules_file"
}

# 创建一致性验证规则
create_consistency_rules() {
    local rules_file="${RULES_DIR}/consistency/rules.yml"
    
    cat > "$rules_file" << 'EOF'
# 一致性验证规则

cross_file:
  # 跨文件一致性规则
  rules:
    - name: database_consistency
      description: "数据库配置必须一致"
      severity: ERROR
      check: "cross_file_consistency"
      fields:
        - "database.host"
        - "database.port"
        - "database.name"
      file_patterns:
        - "*application*"
        - "*database*"
    
    - name: server_consistency
      description: "服务器配置必须一致"
      severity: ERROR
      check: "cross_file_consistency"
      fields:
        - "server.port"
        - "server.host"
      file_patterns:
        - "*application*"
        - "*server*"

environment:
  # 环境一致性规则
  rules:
    - name: env_specific_configs
      description: "环境特定配置必须存在"
      severity: WARNING
      check: "environment_consistency"
      environments:
        - "development"
        - "test"
        - "production"
      required_configs:
        - "database"
        - "server"
        - "security"
    
    - name: env_differences
      description: "环境间必须有差异"
      severity: INFO
      check: "environment_differences"
      fields:
        - "database.host"
        - "server.port"
        - "security.jwt.secret"

version:
  # 版本一致性规则
  rules:
    - name: version_consistency
      description: "版本号必须一致"
      severity: ERROR
      check: "version_consistency"
      fields:
        - "version"
        - "app.version"
      file_patterns:
        - "*application*"
        - "*pom.xml"
        - "*package.json"
    
    - name: dependency_versions
      description: "依赖版本必须一致"
      severity: WARNING
      check: "dependency_versions"
      dependencies:
        - "spring-boot"
        - "postgresql"
        - "redis"
EOF

    log_info "创建一致性验证规则: $rules_file"
}

# 验证单个配置文件
validate_single_file() {
    local config_file="$1"
    local validation_result=true
    
    if [ ! -f "$config_file" ]; then
        log_error "配置文件不存在: $config_file"
        return 1
    fi
    
    log_info "验证配置文件: $config_file"
    
    # 检查文件扩展名
    local file_ext="${config_file##*.}"
    
    # 执行各种验证
    log_info "1. 语法验证..."
    if ! validate_syntax "$config_file" "$file_ext"; then
        validation_result=false
    fi
    
    log_info "2. 格式验证..."
    if ! validate_format "$config_file" "$file_ext"; then
        validation_result=false
    fi
    
    log_info "3. 内容验证..."
    if ! validate_content "$config_file"; then
        validation_result=false
    fi
    
    log_info "4. 安全验证..."
    if ! validate_security "$config_file"; then
        validation_result=false
    fi
    
    # 输出验证结果
    if [ "$validation_result" = true ]; then
        log_success "配置文件验证通过: $config_file"
        return 0
    else
        log_error "配置文件验证失败: $config_file"
        return 1
    fi
}

# 验证语法
validate_syntax() {
    local config_file="$1"
    local file_ext="$2"
    
    case "$file_ext" in
        yml|yaml)
            if ! yamllint "$config_file" 2>/dev/null; then
                log_error "YAML语法错误: $config_file"
                return 1
            fi
            ;;
        json)
            if ! jq '.' "$config_file" > /dev/null 2>&1; then
                log_error "JSON语法错误: $config_file"
                return 1
            fi
            ;;
        properties)
            # 简单的properties语法检查
            if grep -q "^\s*=\|=\s*$" "$config_file"; then
                log_error "Properties语法错误: 空键或空值"
                return 1
            fi
            ;;
        *)
            log_warn "未知文件类型: $file_ext，跳过语法验证"
            ;;
    esac
    
    return 0
}

# 验证格式
validate_format() {
    local config_file="$1"
    local file_ext="$2"
    local format_errors=0
    
    # 检查缩进（针对YAML）
    if [ "$file_ext" = "yml" ] || [ "$file_ext" = "yaml" ]; then
        if grep -q "^\t" "$config_file"; then
            log_error "检测到制表符缩进，应使用空格"
            format_errors=$((format_errors + 1))
        fi
    fi
    
    # 检查行尾空格
    if grep -q "[ ]+$" "$config_file"; then
        log_warn "检测到行尾空格"
        format_errors=$((format_errors + 1))
    fi
    
    # 检查行长度
    local long_lines=$(awk 'length($0) > 120 {print NR": "length($0)" chars"}' "$config_file")
    if [ -n "$long_lines" ]; then
        log_warn "检测到超长行:"
        echo "$long_lines"
        format_errors=$((format_errors + 1))
    fi
    
    # 检查编码
    if file "$config_file" | grep -q "UTF-8 Unicode"; then
        log_success "文件编码: UTF-8"
    else
        log_warn "文件编码可能不是UTF-8"
        format_errors=$((format_errors + 1))
    fi
    
    return $format_errors
}

# 验证内容
validate_content() {
    local config_file="$1"
    local content_errors=0
    
    # 检查必需字段
    if [[ "$config_file" == *database* ]]; then
        local required_fields=("host" "port" "name" "username")
        for field in "${required_fields[@]}"; do
            if ! grep -q -i "$field" "$config_file"; then
                log_error "缺失必需字段: $field"
                content_errors=$((content_errors + 1))
            fi
        done
    fi
    
    # 检查端口范围
    local port=$(grep -i "port" "$config_file" | grep -o '[0-9]\+' | head -1)
    if [ -n "$port" ] && ([ "$port" -lt 1024 ] || [ "$port" -gt 65535 ]); then
        log_error "端口号超出范围: $port (应在1024-65535之间)"
        content_errors=$((content_errors + 1))
    fi
    
    # 检查URL格式
    local urls=$(grep -o "https\?://[^ ]*" "$config_file")
    for url in $urls; do
        if ! [[ $url =~ ^https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,} ]]; then
            log_warn "URL格式可能不正确: $url"
            content_errors=$((content_errors + 1))
        fi
    done
    
    return $content_errors
}

# 验证安全
validate_security() {
    local config_file="$1"
    local security_errors=0
    
    # 检查硬编码密码
    if grep -q "password.*=.*[^${]" "$config_file" 2>/dev/null; then
        log_error "检测到硬编码密码"
        security_errors=$((security_errors + 1))
    fi
    
    # 检查弱密码
    if grep -q "password.*=.*123456\|password.*=.*password\|password.*=.*admin" "$config_file" 2>/dev/null; then
        log_error "检测到弱密码"
        security_errors=$((security_errors + 1))
    fi
    
    # 检查明文凭证
    if grep -q "username.*=.*[^${]\|user.*=.*[^${]" "$config_file" 2>/dev/null; then
        log_warn "检测到明文凭证"
        security_errors=$((security_errors + 1))
    fi
    
    # 检查HTTP协议
    if grep -q "http://" "$config_file" 2>/dev/null; then
        log_warn "检测到HTTP协议，建议使用HTTPS"
        security_errors=$((security_errors + 1))
    fi
    
    return $security_errors
}

# 验证所有配置文件
validate_all_files() {
    local config_dir="${CONFIG_DIR}/generated"
    local total_files=0
    local passed_files=0
    local failed_files=0
    
    if [ ! -d "$config_dir" ]; then
        log_error "配置目录不存在: $config_dir"
        return 1
    fi
    
    log_info "开始验证所有配置文件..."
    
    # 查找所有配置文件
    local config_files=$(find "$config_dir" -type f \( -name "*.yml" -o -name "*.yaml" -o -name "*.json" -o -name "*.properties" \) | sort)
    
    if [ -z "$config_files" ]; then
        log_info "没有找到配置文件"
        return 0
    fi
    
    for config_file in $config_files; do
        total_files=$((total_files + 1))
        
        echo "========================================"
        log_info "验证文件 $total_files: $(basename "$config_file")"
        
        if validate_single_file "$config_file"; then
            passed_files=$((passed_files + 1))
        else
            failed_files=$((failed_files + 1))
        fi
    done
    
    echo "========================================"
    log_info "验证完成"
    echo "总文件数: $total_files"
    echo "通过: $passed_files"
    echo "失败: $failed_files"
    
    if [ $failed_files -eq 0 ]; then
        log_success "所有配置文件验证通过"
        return 0
    else
        log_error "有 $failed_files 个配置文件验证失败"
        return 1
    fi
}

# 检查配置文件语法
check_syntax() {
    local config_dir="${CONFIG_DIR}/generated"
    
    if [ ! -d "$config_dir" ]; then
        log_error "配置目录不存在: $config_dir"
        return 1
    fi
    
    log_info "检查配置文件语法..."
    
    local syntax_errors=0
    
    # 检查YAML文件
    for yaml_file in "$config_dir"/*.yml "$config_dir"/*.yaml; do
        [ -f "$yaml_file" ] || continue
        
        if ! yamllint "$yaml_file" 2>/dev/null; then
            log_error "YAML语法错误: $yaml_file"
            syntax_errors=$((syntax_errors + 1))
        fi
    done
    
    # 检查JSON文件
    for json_file in "$config_dir"/*.json; do
        [ -f "$json_file" ] || continue
        
        if ! jq '.' "$json_file" > /dev/null 2>&1; then
            log_error "JSON语法错误: $json_file"
            syntax_errors=$((syntax_errors + 1))
        fi
    done
    
    if [ $syntax_errors -eq 0 ]; then
        log_success "所有配置文件语法正确"
        return 0
    else
        log_error "发现 $syntax_errors 个语法错误"
        return 1
    fi
}

# 检查安全配置
check_security() {
    local config_dir="${CONFIG_DIR}/generated"
    
    if [ ! -d "$config_dir" ]; then
        log_error "配置目录不存在: $config_dir"
        return 1
    fi
    
    log_info "检查安全配置..."
    
    local security_issues=0
    
    for config_file in "$config_dir"/*; do
        [ -f "$config_file" ] || continue
        
        if ! validate_security "$config_file"; then
            security_issues=$((security_issues + 1))
        fi
    done
    
    if [ $security_issues -eq 0 ]; then
        log_success "未发现安全配置问题"
        return 0
    else
        log_error "发现 $security_issues 个安全配置问题"
        return 1
    fi
}

# 检查配置一致性
check_consistency() {
    local config_dir="${CONFIG_DIR}/generated"
    
    if [ ! -d "$config_dir" ]; then
        log_error "配置目录不存在: $config_dir"
        return 1
    fi
    
    log_info "检查配置一致性..."
    
    local consistency_issues=0
    
    # 检查数据库配置一致性
    local db_configs=$(find "$config_dir" -name "*database*" -type f)
    if [ -n "$db_configs" ]; then
        local first_db_config=$(echo "$db_configs" | head -1)
        local first_host=$(grep -i "host" "$first_db_config" | head -1)
        
        for db_config in $db_configs; do
            local current_host=$(grep -i "host" "$db_config" | head -1)
            if [ "$first_host" != "$current_host" ]; then
                log_warn "数据库主机配置不一致: $db_config"
                consistency_issues=$((consistency_issues + 1))
            fi
        done
    fi
    
    # 检查端口一致性
    local port_configs=$(grep -r "port" "$config_dir" | grep -v "transport\|mail")
    if [ -n "$port_configs" ]; then
        local unique_ports=$(echo "$port_configs" | grep -o '[0-9]\+' | sort -u)
        if [ $(echo "$unique_ports" | wc -l) -gt 1 ]; then
            log_warn "检测到多个不同的端口配置"
            consistency_issues=$((consistency_issues + 1))
        fi
    fi
    
    if [ $consistency_issues -eq 0 ]; then
        log_success "配置一致性检查通过"
        return 0
    else
        log_warn "发现 $consistency_issues 个一致性问题"
        return 1
    fi
}

# 生成验证报告
generate_report() {
    local report_dir="./reports"
    local report_file="${report_dir}/config-validation-$(date +%Y%m%d_%H%M%S).json"
    
    mkdir -p "$report_dir"
    
    log_info "生成验证报告..."
    
    local report_data="{\"timestamp\": \"$(date -Iseconds)\", \"validation\": {}}"
    
    # 收集验证结果
    local config_dir="${CONFIG_DIR}/generated"
    if [ -d "$config_dir" ]; then
        for config_file in "$config_dir"/*; do
            [ -f "$config_file" ] || continue
            
            local config_name=$(basename "$config_file")
            local validation_result="passed"
            
            if ! validate_single_file "$config_file" &> /dev/null; then
                validation_result="failed"
            fi
            
            report_data=$(echo "$report_data" | jq --arg name "$config_name" \
                --arg result "$validation_result" \
                --arg path "$config_file" \
                '.validation[$name] = {"result": $result, "path": $path}')
        done
    fi
    
    # 写入报告文件
    echo "$report_data" | jq '.' > "$report_file"
    
    log_success "验证报告已生成: $report_file"
    
    # 生成统计信息
    local total_files=$(echo "$report_data" | jq '.validation | length')
    local passed_files=$(echo "$report_data" | jq '[.validation[] | select(.result == "passed")] | length')
    local failed_files=$((total_files - passed_files))
    
    echo "=== 验证报告统计 ==="
    echo "总配置文件数: $total_files"
    echo "验证通过: $passed_files"
    echo "验证失败: $failed_files"
    echo "报告文件: $report_file"
}

# 列出验证规则
list_rules() {
    log_info "验证规则列表:"
    
    if [ ! -d "$RULES_DIR" ]; then
        log_error "规则目录不存在: $RULES_DIR"
        return 1
    fi
    
    for rule_file in "$RULES_DIR"/*/*.yml; do
        [ -f "$rule_file" ] || continue
        
        local rule_type=$(basename "$(dirname "$rule_file")")
        echo -e "${BLUE}$rule_type 规则:${NC}"
        
        # 简单显示规则内容
        grep -E "name:|description:|severity:" "$rule_file" | head -10 | while read line; do
            echo "  $line"
        done
        echo
    done
}

# 主函数
main() {
    check_prerequisites
    
    local command="${1:-help}"
    
    case "$command" in
        init)
            init_validation_rules
            ;;
        validate)
            if [ -z "${2:-}" ]; then
                log_error "请指定配置文件路径"
                show_help
                exit 1
            fi
            validate_single_file "$2"
            ;;
        validate-all)
            validate_all_files
            ;;
        check-syntax)
            check_syntax
            ;;
        check-security)
            check_security
            ;;
        check-consistency)
            check_consistency
            ;;
        generate-report)
            generate_report
            ;;
        list-rules)
            list_rules
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