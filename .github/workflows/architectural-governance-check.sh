#!/bin/bash

# 架构治理检查脚本
# 集成到CI/CD流水线中，用于自动执行架构合规性检查
# 基于紧急制动教训：防止架构问题积累导致的架构混乱

set -euo pipefail

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 检查目录结构合规性
check_directory_structure() {
    log_info "检查目录结构合规性..."
    
    local violations=0
    
    # 检查ERP模块目录结构
    local erp_modules=$(find backend -type d -name "erp-*" 2>/dev/null || true)
    local erp_dirs=$(find backend -type d -name "erp" 2>/dev/null || true)
    
    # 规则1：检查重复的ERP模块目录
    for dir in $erp_modules; do
        local module_name=$(basename "$dir")
        log_info "检查ERP模块: $module_name"
        
        # 检查是否有重复的erp/目录
        if [[ -d "backend/erp/$module_name" ]] && [[ "$dir" != "backend/erp/$module_name" ]]; then
            log_error "发现重复的ERP模块目录: $dir 和 backend/erp/$module_name"
            violations=$((violations + 1))
        fi
    done
    
    # 规则2：检查模块命名规范
    for dir in $erp_modules $erp_dirs; do
        local module_name=$(basename "$dir")
        # 检查是否使用一致的命名规范
        if [[ "$module_name" =~ ^erp-.*$ ]] || [[ "$module_name" == "erp" ]]; then
            log_success "模块 '$module_name' 符合命名规范"
        else
            log_warn "模块 '$module_name' 可能不符合命名规范"
        fi
    done
    
    # 规则3：检查目录深度
    find backend -type d -name "erp-*" | while read dir; do
        local depth=$(echo "$dir" | tr '/' '\n' | wc -l)
        if (( depth > 6 )); then
            log_warn "目录深度过大: $dir (深度: $depth)"
            violations=$((violations + 1))
        fi
    done
    
    if (( violations == 0 )); then
        log_success "目录结构合规性检查通过"
        return 0
    else
        log_error "发现 $violations 个目录结构问题"
        return 1
    fi
}

# 检查模块边界
check_module_boundaries() {
    log_info "检查模块边界..."
    
    local violations=0
    
    # 规则1：检查跨模块的Java包引用
    for pom_file in $(find backend -name "pom.xml" | grep -v "target/" | head -20); do
        local module_dir=$(dirname "$pom_file")
        local module_name=$(basename "$module_dir")
        
        # 检查该模块是否引用其他ERP模块的包
        if [[ -d "$module_dir/src" ]]; then
            find "$module_dir/src" -name "*.java" | head -10 | while read java_file; do
                if grep -q "import.*erp\." "$java_file"; then
                    local imports=$(grep -E "import.*erp\.[^.]+(\..+)?" "$java_file" | head -5)
                    while IFS= read -r import_line; do
                        if [[ "$import_line" =~ import\ ([a-zA-Z0-9._]+)\; ]]; then
                            local import_pkg="${BASH_REMATCH[1]}"
                            # 检查是否引用了其他ERP模块
                            if [[ "$import_pkg" =~ erp\.([^.]+)\. ]] && [[ "$module_name" != "erp-${BASH_REMATCH[1]}" ]]; then
                                log_warn "模块 '$module_name' 可能过度依赖其他模块: $import_pkg"
                                violations=$((violations + 1))
                            fi
                        fi
                    done <<< "$imports"
                fi
            done
        fi
    done
    
    # 规则2：检查循环依赖
    if (( violations == 0 )); then
        log_success "模块边界检查通过"
        return 0
    else
        log_error "发现 $violations 个模块边界问题"
        return 1
    fi
}

# 检查依赖关系
check_dependencies() {
    log_info "检查依赖关系..."
    
    local violations=0
    
    # 检查pom.xml中的依赖版本一致性
    local all_poms=$(find backend -name "pom.xml" | grep -v "target/" | head -10)
    
    # 收集所有依赖版本
    declare -A dependency_versions
    
    for pom_file in $all_poms; do
        if [[ -f "$pom_file" ]]; then
            # 提取依赖信息
            local deps=$(grep -A 2 -B 2 "<artifactId>" "$pom_file" | grep -E "(groupId|artifactId|version)" || true)
            echo "$deps" | while read -r line; do
                if [[ "$line" =~ \<groupId\>(.+)\</groupId\> ]]; then
                    current_group="${BASH_REMATCH[1]}"
                elif [[ "$line" =~ \<artifactId\>(.+)\</artifactId\> ]]; then
                    current_artifact="${BASH_REMATCH[1]}"
                elif [[ "$line" =~ \<version\>(.+)\</version\> ]]; then
                    current_version="${BASH_REMATCH[1]}"
                    local key="$current_group:$current_artifact"
                    if [[ -n "$key" ]] && [[ -n "$current_version" ]]; then
                        if [[ -n "${dependency_versions[$key]}" ]] && [[ "${dependency_versions[$key]}" != "$current_version" ]]; then
                            log_warn "依赖版本不一致: $key - ${dependency_versions[$key]} vs $current_version"
                            violations=$((violations + 1))
                        else
                            dependency_versions["$key"]="$current_version"
                        fi
                    fi
                fi
            done
        fi
    done
    
    # 检查循环依赖（简化检查）
    for pom_file in $(echo "$all_poms" | head -5); do
        if grep -q "erp-" "$pom_file" 2>/dev/null; then
            local module_deps=$(grep -E "erp-[a-z-]+" "$pom_file" | grep -v "<!--" | head -5)
            while IFS= read -r dep; do
                if [[ "$dep" =~ erp-([a-z-]+) ]]; then
                    local dep_module="${BASH_REMATCH[1]}"
                    local current_module=$(basename $(dirname "$pom_file"))
                    # 简单检查可能的循环依赖
                    local dep_pom="backend/erp-$dep_module/pom.xml"
                    if [[ -f "$dep_pom" ]] && grep -q "$current_module" "$dep_pom" 2>/dev/null; then
                        log_error "可能发现循环依赖: $current_module ↔ $dep_module"
                        violations=$((violations + 1))
                    fi
                fi
            done <<< "$module_deps"
        fi
    done
    
    if (( violations == 0 )); then
        log_success "依赖关系检查通过"
        return 0
    else
        log_error "发现 $violations 个依赖关系问题"
        return 1
    fi
}

# 检查架构规范
check_architecture_standards() {
    log_info "检查架构规范..."
    
    local violations=0
    
    # 规则1：检查API路径规范
    local api_files=$(find backend -name "*Controller.java" -o -name "*Api.java" | head -10)
    for api_file in $api_files; do
        if [[ -f "$api_file" ]]; then
            local annotations=$(grep -E "@(RestController|Controller|RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)" "$api_file" | head -5)
            while IFS= read -r annotation; do
                if [[ "$annotation" =~ \"/api/erp/ ]] || [[ "$annotation" =~ value\s*=\s*\{\s*\"(.*)\"\} ]]; then
                    local path="${BASH_REMATCH[1]}"
                    if [[ -n "$path" ]] && [[ ! "$path" =~ ^/api/erp/ ]]; then
                        log_warn "API路径不符合规范: $path (应该以 /api/erp/ 开头)"
                        violations=$((violations + 1))
                    fi
                fi
            done <<< "$annotations"
        fi
    done
    
    # 规则2：检查包命名规范
    local java_files=$(find backend -name "*.java" | grep -v "target/" | head -20)
    for java_file in $java_files; do
        if [[ -f "$java_file" ]]; then
            local package_line=$(grep "^package " "$java_file" | head -1)
            if [[ "$package_line" =~ package\ ([a-zA-Z0-9._]+)\; ]]; then
                local package_name="${BASH_REMATCH[1]}"
                # 检查是否使用正确的包命名
                if [[ "$package_name" =~ ^cn\.aiedge\.erp\. ]] || [[ "$package_name" =~ ^cn\.aiedge\.([^.]+)\. ]]; then
                    log_success "包 '$package_name' 符合命名规范"
                else
                    log_warn "包 '$package_name' 可能不符合命名规范"
                fi
            fi
        fi
    done
    
    # 规则3：检查DTO/Entity命名规范
    for java_file in $(find backend -name "*.java" | xargs grep -l "class.*DTO\|class.*Entity\|class.*VO" 2>/dev/null | head -10); do
        local class_name=$(grep -E "class [A-Z][a-zA-Z0-9]*" "$java_file" | head -1)
        if [[ "$class_name" =~ class\ ([A-Z][a-zA-Z0-9]*)(DTO|Entity|VO)? ]]; then
            local class="${BASH_REMATCH[1]}"
            local suffix="${BASH_REMATCH[2]}"
            if [[ -n "$suffix" ]]; then
                log_success "类 '$class$suffix' 符合命名规范"
            else
                log_warn "类 '$class' 缺少标准的后缀 (DTO/Entity/VO)"
                violations=$((violations + 1))
            fi
        fi
    done
    
    if (( violations == 0 )); then
        log_success "架构规范检查通过"
        return 0
    else
        log_error "发现 $violations 个架构规范问题"
        return 1
    fi
}

# 生成架构检查报告
generate_architecture_report() {
    log_info "生成架构检查报告..."
    
    local report_file="architecture-governance-report.md"
    
    cat > "$report_file" << EOF
# 架构治理检查报告

## 报告信息
- **项目**: AI-Ready ERP系统
- **检查时间**: $(date)
- **Git提交**: $(git rev-parse --short HEAD 2>/dev/null || echo "N/A")
- **分支**: $(git branch --show-current 2>/dev/null || echo "N/A")

## 检查结果

### 1. 目录结构合规性
EOF

    check_directory_structure >> "$report_file" 2>&1
    local dir_status=$?
    
    cat >> "$report_file" << EOF

### 2. 模块边界检查
EOF
    
    check_module_boundaries >> "$report_file" 2>&1
    local module_status=$?
    
    cat >> "$report_file" << EOF

### 3. 依赖关系检查
EOF
    
    check_dependencies >> "$report_file" 2>&1
    local dep_status=$?
    
    cat >> "$report_file" << EOF

### 4. 架构规范检查
EOF
    
    check_architecture_standards >> "$report_file" 2>&1
    local std_status=$?
    
    cat >> "$report_file" << EOF

## 总体评估
EOF

    local total_violations=$((dir_status + module_status + dep_status + std_status))
    
    if (( total_violations == 0 )); then
        cat >> "$report_file" << EOF
✅ **所有架构检查通过**

系统架构符合治理要求，可以继续后续的CI/CD流程。
EOF
        log_success "架构检查全部通过"
        return 0
    else
        cat >> "$report_file" << EOF
❌ **发现架构问题**

发现 $total_violations 个架构检查项目失败，需要修复后才能继续。

## 建议修复措施
1. 检查并修复目录结构问题
2. 明确模块边界，减少不必要的跨模块依赖
3. 统一依赖版本，避免版本冲突
4. 遵循架构规范，确保代码一致性
EOF
        log_error "架构检查失败: $total_violations 个项目未通过"
        return 1
    fi
}

# 主函数
main() {
    log_info "开始架构治理检查..."
    
    # 确保在项目根目录
    if [[ ! -d "backend" ]]; then
        log_error "不在项目根目录或backend目录不存在"
        exit 1
    fi
    
    # 执行架构检查
    local overall_status=0
    
    # 1. 检查目录结构
    if ! check_directory_structure; then
        overall_status=1
    fi
    
    # 2. 检查模块边界
    if ! check_module_boundaries; then
        overall_status=1
    fi
    
    # 3. 检查依赖关系
    if ! check_dependencies; then
        overall_status=1
    fi
    
    # 4. 检查架构规范
    if ! check_architecture_standards; then
        overall_status=1
    fi
    
    # 5. 生成报告
    generate_architecture_report
    
    # 上传报告
    if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
        log_info "上传架构检查报告..."
        echo "ARCHITECTURE_REPORT_PATH=architecture-governance-report.md" >> $GITHUB_ENV
        echo "ARCHITECTURE_CHECK_STATUS=$overall_status" >> $GITHUB_ENV
    fi
    
    if (( overall_status == 0 )); then
        log_success "架构治理检查完成，所有检查通过"
        exit 0
    else
        log_error "架构治理检查完成，发现架构问题"
        exit 1
    fi
}

# 执行主函数
main "$@"