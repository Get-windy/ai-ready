#!/bin/bash

# =================================================================
# ERP批次管理模块一键部署脚本
# 版本：v1.0.0
# 作者：ERP DevOps Team
# =================================================================

set -e

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

# 帮助信息
show_help() {
    echo "ERP批次管理模块部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --build             构建项目并生成Docker镜像"
    echo "  --deploy            部署服务（Docker Compose）"
    echo "  --stop              停止服务"
    echo "  --restart           重启服务"
    echo "  --status            查看服务状态"
    echo "  --logs [服务名]     查看服务日志"
    echo "  --backup            备份数据库和配置"
    echo "  --restore           从备份恢复"
    echo "  --clean             清理临时文件和镜像"
    echo "  --test              运行部署测试"
    echo "  --monitor           启动监控系统"
    echo "  --help              显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --build --deploy   # 构建并部署"
    echo "  $0 --status           # 查看服务状态"
    echo "  $0 --logs erp-batch   # 查看批次服务日志"
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    # 检查Maven（如果需要构建）
    if [[ "$1" == "--build" ]]; then
        if ! command -v mvn &> /dev/null; then
            log_error "Maven未安装，请先安装Maven"
            exit 1
        fi
    fi
    
    log_success "依赖检查通过"
}

# 加载环境变量
load_env() {
    if [ -f "../env/.env" ]; then
        log_info "加载环境变量..."
        source "../env/.env"
    elif [ -f ".env" ]; then
        log_info "加载环境变量..."
        source ".env"
    else
        log_warning "未找到.env文件，使用默认配置"
    fi
}

# 构建项目
build_project() {
    log_info "开始构建批次管理模块..."
    
    cd ../.. || {
        log_error "无法切换到项目根目录"
        exit 1
    }
    
    # 清理并构建
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        log_success "项目构建成功"
        
        # 构建Docker镜像
        log_info "构建Docker镜像..."
        docker build -t erp-batch:${TAG:-latest} -f deploy/docker/Dockerfile .
        
        if [ $? -eq 0 ]; then
            log_success "Docker镜像构建成功"
        else
            log_error "Docker镜像构建失败"
            exit 1
        fi
    else
        log_error "项目构建失败"
        exit 1
    fi
    
    cd - > /dev/null
}

# 部署服务
deploy_services() {
    log_info "开始部署批次管理服务..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    # 创建必要的目录
    mkdir -p ../logs
    mkdir -p ../backups
    mkdir -p ../monitoring/prometheus
    mkdir -p ../monitoring/grafana/provisioning
    mkdir -p ../monitoring/grafana/dashboards
    
    # 启动服务
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        log_success "服务启动成功"
        
        # 等待服务完全启动
        log_info "等待服务就绪..."
        sleep 10
        
        # 检查服务状态
        check_services_status
    else
        log_error "服务启动失败"
        exit 1
    fi
    
    cd - > /dev/null
}

# 检查服务状态
check_services_status() {
    log_info "检查服务运行状态..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    echo ""
    echo "=== 服务运行状态 ==="
    docker-compose ps
    
    echo ""
    echo "=== 容器资源使用情况 ==="
    docker stats --no-stream $(docker-compose ps -q) 2>/dev/null || echo "无法获取资源使用情况"
    
    # 检查应用健康状态
    log_info "检查应用健康状态..."
    local max_retries=10
    local retry_count=0
    
    while [ $retry_count -lt $max_retries ]; do
        if curl -s http://localhost:18080/actuator/health > /dev/null 2>&1; then
            log_success "应用健康检查通过"
            break
        else
            log_warning "应用尚未就绪，等待重试... ($((retry_count + 1))/$max_retries)"
            retry_count=$((retry_count + 1))
            sleep 5
        fi
    done
    
    if [ $retry_count -eq $max_retries ]; then
        log_error "应用健康检查失败"
        exit 1
    fi
    
    cd - > /dev/null
}

# 停止服务
stop_services() {
    log_info "停止批次管理服务..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    docker-compose down
    
    if [ $? -eq 0 ]; then
        log_success "服务已停止"
    else
        log_error "服务停止失败"
        exit 1
    fi
    
    cd - > /dev/null
}

# 重启服务
restart_services() {
    log_info "重启批次管理服务..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    docker-compose restart
    
    if [ $? -eq 0 ]; then
        log_success "服务重启成功"
        sleep 5
        check_services_status
    else
        log_error "服务重启失败"
        exit 1
    fi
    
    cd - > /dev/null
}

# 查看日志
view_logs() {
    local service_name=$1
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    if [ -z "$service_name" ]; then
        log_info "查看所有服务日志（Ctrl+C退出）..."
        docker-compose logs -f
    else
        log_info "查看服务 $service_name 日志（Ctrl+C退出）..."
        docker-compose logs -f "$service_name"
    fi
    
    cd - > /dev/null
}

# 备份数据
backup_data() {
    log_info "开始备份批次管理数据..."
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_dir="../backups/backup_$timestamp"
    
    mkdir -p "$backup_dir"
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    # 备份数据库
    log_info "备份PostgreSQL数据库..."
    docker-compose exec -T postgres pg_dump -U ${DB_USER:-erp_user} erp_batch_db > "${backup_dir}/erp_batch_db.sql"
    
    # 备份Redis数据
    log_info "备份Redis数据..."
    docker-compose exec -T redis redis-cli --rdb /data/dump.rdb > /dev/null 2>&1
    docker cp erp-batch-redis:/data/dump.rdb "${backup_dir}/redis_dump.rdb" 2>/dev/null || true
    
    # 备份配置文件
    log_info "备份配置文件..."
    cp ../../src/main/resources/*.yml "${backup_dir}/" 2>/dev/null || true
    cp ../../deploy/env/.env "${backup_dir}/" 2>/dev/null || true
    cp docker-compose.yml "${backup_dir}/"
    
    # 创建备份元数据
    cat > "${backup_dir}/backup_metadata.json" << EOF
{
    "backup_time": "$(date -Iseconds)",
    "backup_type": "full",
    "services": ["postgres", "redis", "rabbitmq"],
    "version": "1.0.0",
    "description": "ERP批次管理模块全量备份"
}
EOF
    
    # 压缩备份
    cd ../backups || exit 1
    tar -czf "backup_${timestamp}.tar.gz" "backup_$timestamp"
    rm -rf "backup_$timestamp"
    
    log_success "备份完成：backup_${timestamp}.tar.gz"
    ls -lh "backup_${timestamp}.tar.gz"
    
    cd - > /dev/null
}

# 清理资源
clean_resources() {
    log_info "清理临时资源和镜像..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    # 停止并删除容器
    docker-compose down -v
    
    # 删除未使用的镜像
    docker image prune -f
    
    # 删除未使用的卷
    docker volume prune -f
    
    # 清理构建缓存
    cd ../.. || exit 1
    mvn clean 2>/dev/null || true
    
    log_success "资源清理完成"
    
    cd - > /dev/null
}

# 运行部署测试
run_deployment_tests() {
    log_info "运行部署测试..."
    
    # 测试1：服务可访问性
    log_info "测试1：检查服务可访问性..."
    if curl -s http://localhost:18080/actuator/health | grep -q '"status":"UP"'; then
        log_success "服务可访问性测试通过"
    else
        log_error "服务可访问性测试失败"
        return 1
    fi
    
    # 测试2：数据库连接
    log_info "测试2：检查数据库连接..."
    cd ../docker || return 1
    if docker-compose exec -T postgres pg_isready -U ${DB_USER:-erp_user}; then
        log_success "数据库连接测试通过"
    else
        log_error "数据库连接测试失败"
        return 1
    fi
    
    # 测试3：API功能测试
    log_info "测试3：测试批次管理API..."
    if curl -s http://localhost:18080/api/v1/batches | grep -q '\[\]'; then
        log_success "API功能测试通过"
    else
        log_warning "API返回异常或需要认证"
    fi
    
    cd - > /dev/null
    log_success "部署测试完成"
}

# 启动监控系统
start_monitoring() {
    log_info "启动监控系统..."
    
    cd ../docker || {
        log_error "无法切换到docker目录"
        exit 1
    }
    
    # 启动监控服务
    docker-compose up -d prometheus grafana
    
    log_success "监控系统已启动"
    echo ""
    echo "监控系统访问地址:"
    echo "  - Prometheus: http://localhost:19090"
    echo "  - Grafana:    http://localhost:13000 (admin/admin)"
    echo ""
    echo "等待监控服务启动..."
    sleep 10
    
    # 导入Grafana仪表板
    log_info "配置Grafana监控面板..."
    # 这里可以添加自动导入仪表板的逻辑
    
    cd - > /dev/null
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    # 加载环境变量
    load_env
    
    # 检查依赖
    check_dependencies "$1"
    
    # 处理命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --build)
                build_project
                shift
                ;;
            --deploy)
                deploy_services
                shift
                ;;
            --stop)
                stop_services
                shift
                ;;
            --restart)
                restart_services
                shift
                ;;
            --status)
                check_services_status
                shift
                ;;
            --logs)
                view_logs "$2"
                shift 2
                ;;
            --backup)
                backup_data
                shift
                ;;
            --clean)
                clean_resources
                shift
                ;;
            --test)
                run_deployment_tests
                shift
                ;;
            --monitor)
                start_monitoring
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 执行主函数
main "$@"