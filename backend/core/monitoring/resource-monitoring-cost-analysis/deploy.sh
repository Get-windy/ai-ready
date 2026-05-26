#!/bin/bash

# 资源监控与成本分析模块部署脚本
# 版本: 1.0.0

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

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 $1 未找到，请先安装"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    echo "资源监控与成本分析模块部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help          显示此帮助信息"
    echo "  -i, --init          初始化部署环境"
    echo "  -d, --deploy        部署监控栈"
    echo "  -u, --upgrade       升级监控栈"
    echo "  -s, --stop          停止监控栈"
    echo "  -r, --restart       重启监控栈"
    echo "  -c, --clean         清理部署环境"
    echo "  -t, --test          运行测试"
    echo "  -m, --monitor       显示监控状态"
    echo "  -b, --backup        备份数据"
    echo "  -v, --version       显示版本信息"
    echo ""
    echo "示例:"
    echo "  $0 --init           初始化环境"
    echo "  $0 --deploy         部署监控栈"
    echo "  $0 --stop           停止监控栈"
}

# 显示版本信息
show_version() {
    echo "资源监控与成本分析模块部署脚本 v1.0.0"
    echo "发布日期: 2026-04-29"
    echo "适用于: Sprint 27+1 测试环境"
}

# 初始化部署环境
init_environment() {
    log_info "开始初始化部署环境..."
    
    # 检查必要命令
    check_command docker
    check_command docker-compose
    check_command python3
    check_command curl
    
    # 创建必要的目录
    log_info "创建目录结构..."
    mkdir -p config/prometheus config/grafana config/alertmanager config/blackbox
    mkdir -p config/nginx/ssl config/postgres config/fluentd
    mkdir -p data/prometheus data/grafana data/alertmanager data/postgres data/redis
    mkdir -p logs/nginx logs/fluentd logs/application
    mkdir -p dashboards backups
    
    # 复制配置文件
    log_info "复制配置文件..."
    if [ -f "config/prometheus/prometheus.yml" ]; then
        log_warning "Prometheus配置文件已存在，跳过"
    else
        cp -n templates/prometheus.yml config/prometheus/prometheus.yml
    fi
    
    if [ -f "config/grafana/datasources/prometheus.yml" ]; then
        log_warning "Grafana数据源配置文件已存在，跳过"
    else
        mkdir -p config/grafana/datasources
        cp -n templates/grafana-datasource.yml config/grafana/datasources/prometheus.yml
    fi
    
    if [ -f "config/alertmanager/alertmanager.yml" ]; then
        log_warning "Alertmanager配置文件已存在，跳过"
    else
        cp -n templates/alertmanager.yml config/alertmanager/alertmanager.yml
    fi
    
    # 生成SSL证书（如果不存在）
    if [ ! -f "config/nginx/ssl/server.crt" ]; then
        log_info "生成自签名SSL证书..."
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout config/nginx/ssl/server.key \
            -out config/nginx/ssl/server.crt \
            -subj "/C=CN/ST=Beijing/L=Beijing/O=AI-Ready/CN=monitoring.local" 2>/dev/null || \
            log_warning "SSL证书生成失败，请手动生成"
    fi
    
    # 检查Python依赖
    log_info "检查Python依赖..."
    if [ -f "requirements.txt" ]; then
        pip3 install -r requirements.txt --user || log_warning "Python依赖安装失败"
    fi
    
    # 设置权限
    log_info "设置目录权限..."
    chmod -R 755 config data logs dashboards
    chmod +x scripts/*.sh 2>/dev/null || true
    
    log_success "部署环境初始化完成"
}

# 部署监控栈
deploy_stack() {
    log_info "开始部署监控栈..."
    
    # 检查Docker是否运行
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker服务未运行，请先启动Docker"
        exit 1
    fi
    
    # 构建镜像
    log_info "构建资源监控服务镜像..."
    docker-compose build resource-monitoring-service || {
        log_error "构建镜像失败"
        exit 1
    }
    
    # 启动服务
    log_info "启动监控栈..."
    docker-compose up -d || {
        log_error "启动监控栈失败"
        exit 1
    }
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_services_status
    
    log_success "监控栈部署完成"
}

# 检查服务状态
check_services_status() {
    log_info "检查服务状态..."
    
    services=("prometheus" "grafana" "alertmanager" "resource-monitoring-service" "postgres" "redis")
    
    for service in "${services[@]}"; do
        if docker-compose ps | grep -q "${service}.*Up"; then
            log_success "服务 ${service} 运行正常"
        else
            log_error "服务 ${service} 运行异常"
        fi
    done
    
    # 检查端口访问
    log_info "检查端口访问..."
    
    # Prometheus
    if curl -s http://localhost:9090/-/healthy > /dev/null; then
        log_success "Prometheus (9090) 可访问"
    else
        log_error "Prometheus (9090) 不可访问"
    fi
    
    # Grafana
    if curl -s http://localhost:3000/api/health > /dev/null; then
        log_success "Grafana (3000) 可访问"
    else
        log_error "Grafana (3000) 不可访问"
    fi
    
    # 资源监控服务
    if curl -s http://localhost:8000/health > /dev/null; then
        log_success "资源监控服务 (8000) 可访问"
    else
        log_error "资源监控服务 (8000) 不可访问"
    fi
}

# 升级监控栈
upgrade_stack() {
    log_info "开始升级监控栈..."
    
    # 拉取最新镜像
    log_info "拉取最新镜像..."
    docker-compose pull || log_warning "拉取镜像失败，继续使用本地镜像"
    
    # 停止并删除旧容器
    log_info "停止旧容器..."
    docker-compose down
    
    # 启动新容器
    log_info "启动新容器..."
    docker-compose up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_services_status
    
    log_success "监控栈升级完成"
}

# 停止监控栈
stop_stack() {
    log_info "停止监控栈..."
    docker-compose down
    log_success "监控栈已停止"
}

# 重启监控栈
restart_stack() {
    log_info "重启监控栈..."
    docker-compose restart
    sleep 10
    check_services_status
    log_success "监控栈已重启"
}

# 清理部署环境
clean_environment() {
    log_warning "即将清理部署环境，此操作不可逆！"
    read -p "是否继续? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "开始清理部署环境..."
        
        # 停止并删除容器
        docker-compose down -v
        
        # 删除数据卷
        docker volume prune -f
        
        # 删除生成的目录（保留配置文件）
        rm -rf data logs dashboards backups
        
        # 重新创建必要的目录
        mkdir -p data logs
        
        log_success "部署环境清理完成"
    else
        log_info "取消清理操作"
    fi
}

# 运行测试
run_tests() {
    log_info "开始运行测试..."
    
    # 检查Python测试
    if [ -d "tests" ]; then
        log_info "运行Python单元测试..."
        python3 -m pytest tests/unit/ -v || log_warning "单元测试失败"
        
        log_info "运行集成测试..."
        python3 -m pytest tests/integration/ -v || log_warning "集成测试失败"
    fi
    
    # 检查API端点
    log_info "检查API端点..."
    
    endpoints=(
        "http://localhost:8000/health"
        "http://localhost:8000/api/v1/metrics/resource"
        "http://localhost:8000/api/v1/cost/summary"
    )
    
    for endpoint in "${endpoints[@]}"; do
        if curl -s $endpoint > /dev/null; then
            log_success "API端点 ${endpoint} 可访问"
        else
            log_error "API端点 ${endpoint} 不可访问"
        fi
    done
    
    log_success "测试完成"
}

# 显示监控状态
show_monitor_status() {
    log_info "监控状态:"
    
    echo ""
    echo "服务状态:"
    docker-compose ps
    
    echo ""
    echo "资源使用:"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" | head -10
    
    echo ""
    echo "最近日志:"
    docker-compose logs --tail=10 resource-monitoring-service
    
    echo ""
    echo "访问地址:"
    echo "  Prometheus: http://localhost:9090"
    echo "  Grafana:    http://localhost:3000 (admin/admin)"
    echo "  监控服务:   http://localhost:8000"
    echo "  Alertmanager: http://localhost:9093"
}

# 备份数据
backup_data() {
    log_info "开始备份数据..."
    
    backup_dir="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p $backup_dir
    
    # 备份数据库
    log_info "备份PostgreSQL数据库..."
    docker-compose exec -T postgres pg_dump -U monitoring_user monitoring > $backup_dir/postgres_backup.sql
    
    # 备份Prometheus数据
    log_info "备份Prometheus数据..."
    docker cp prometheus:/prometheus $backup_dir/prometheus_data 2>/dev/null || \
        log_warning "Prometheus数据备份失败"
    
    # 备份Grafana数据
    log_info "备份Grafana数据..."
    docker cp grafana:/var/lib/grafana $backup_dir/grafana_data 2>/dev/null || \
        log_warning "Grafana数据备份失败"
    
    # 备份配置文件
    log_info "备份配置文件..."
    cp -r config $backup_dir/
    
    # 创建压缩包
    log_info "创建备份压缩包..."
    tar -czf $backup_dir.tar.gz -C backups $(basename $backup_dir)
    
    # 清理临时目录
    rm -rf $backup_dir
    
    log_success "数据备份完成: $backup_dir.tar.gz"
    
    # 显示备份信息
    echo ""
    echo "备份信息:"
    echo "  文件: $backup_dir.tar.gz"
    echo "  大小: $(du -h $backup_dir.tar.gz | cut -f1)"
    echo "  包含: PostgreSQL数据库, Prometheus数据, Grafana数据, 配置文件"
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    case $1 in
        -h|--help)
            show_help
            ;;
        -v|--version)
            show_version
            ;;
        -i|--init)
            init_environment
            ;;
        -d|--deploy)
            deploy_stack
            ;;
        -u|--upgrade)
            upgrade_stack
            ;;
        -s|--stop)
            stop_stack
            ;;
        -r|--restart)
            restart_stack
            ;;
        -c|--clean)
            clean_environment
            ;;
        -t|--test)
            run_tests
            ;;
        -m|--monitor)
            show_monitor_status
            ;;
        -b|--backup)
            backup_data
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"