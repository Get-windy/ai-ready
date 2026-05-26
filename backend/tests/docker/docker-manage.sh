#!/bin/bash

# Docker测试环境管理脚本
# 用于启动、停止、清理和管理测试容器

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.test-env.yml"

# 打印函数
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker守护进程未运行，请启动Docker Desktop"
        exit 1
    fi
    
    print_success "Docker已安装并运行"
}

# 检查Docker Compose是否安装
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    print_success "Docker Compose已安装"
}

# 显示帮助信息
show_help() {
    echo "Docker测试环境管理脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start        启动所有测试容器"
    echo "  stop         停止所有测试容器"
    echo "  restart      重启所有测试容器"
    echo "  down         停止并删除容器、网络"
    echo "  clean        停止并删除容器、网络和卷"
    echo "  status       显示容器状态"
    echo "  logs         查看日志 (docker logs -f <service>)"
    echo "  ps           列出容器"
    echo "  help         显示帮助信息"
    echo ""
    echo "例子:"
    echo "  $0 start                # 启动测试环境"
    echo "  $0 logs postgresql-test # 查看PostgreSQL日志"
    echo "  $0 clean                # 清理所有测试环境数据"
}

# 启动测试容器
start_containers() {
    print_info "启动Docker测试环境..."
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        print_error "未找到docker-compose文件: $COMPOSE_FILE"
        exit 1
    fi
    
    # 启动所有服务
    docker-compose -f "$COMPOSE_FILE" up -d
    
    print_success "所有测试容器已启动"
    
    # 检查容器健康状态
    print_info "等待容器启动并检查健康状态..."
    sleep 10
    
    # 检查每个服务的健康状态
    local services=("postgresql-test" "redis-test" "rabbitmq-test" "elasticsearch-test" "minio-test")
    for service in "${services[@]}"; do
        local health=$(docker ps --filter "name=$service" --format '{{.Status}}')
        print_info "$service: $health"
    done
    
    # 显示连接信息
    show_connection_info
}

# 停止测试容器
stop_containers() {
    print_info "停止Docker测试环境..."
    docker-compose -f "$COMPOSE_FILE" stop
    print_success "测试容器已停止"
}

# 重启测试容器
restart_containers() {
    print_info "重启Docker测试环境..."
    docker-compose -f "$COMPOSE_FILE" restart
    print_success "测试容器已重启"
}

# 停止并删除容器、网络
down_containers() {
    print_info "停止并删除Docker测试环境..."
    docker-compose -f "$COMPOSE_FILE" down
    print_success "测试容器和网络已删除"
}

# 停止、删除容器、网络和卷（完全清理）
clean_all() {
    print_warning "将删除所有测试容器、网络和卷数据！"
    read -p "确认操作? (y/N) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "完全清理Docker测试环境..."
        docker-compose -f "$COMPOSE_FILE" down -v
        print_success "测试环境已完全清理"
    else
        print_info "操作已取消"
    fi
}

# 显示容器状态
show_status() {
    print_info "Docker测试环境状态:"
    docker-compose -f "$COMPOSE_FILE" ps
}

# 查看日志
show_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        print_error "请指定服务名称: docker-manage.sh logs <service>"
        echo "可用服务: postgresql-test, redis-test, rabbitmq-test, elasticsearch-test, minio-test"
        exit 1
    fi
    
    print_info "查看服务日志: $service"
    docker-compose -f "$COMPOSE_FILE" logs -f "$service"
}

# 列出容器
list_containers() {
    print_info "Docker测试环境容器列表:"
    docker-compose -f "$COMPOSE_FILE" ps
}

# 显示连接信息
show_connection_info() {
    echo ""
    print_info "测试环境连接信息:"
    echo "-----------------------------------"
    echo "PostgreSQL:  localhost:5433"
    echo "  用户: test_user"
    echo "  密码: test_password"
    echo "  数据库: ai_ready_test"
    echo ""
    echo "Redis:       localhost:6380"
    echo "  端口: 6380"
    echo ""
    echo "RabbitMQ:    localhost:5673 (AMQP)"
    echo "             localhost:15673 (Management UI)"
    echo "  用户: test_user"
    echo "  密码: test_password"
    echo ""
    echo "Elasticsearch: localhost:9201"
    echo "  端口: 9201"
    echo "  注意: xpack.security已禁用"
    echo ""
    echo "MinIO:       localhost:9001 (API)"
    echo "             localhost:9002 (Console)"
    echo "  用户: test_user"
    echo "  密码: test_password123"
    echo "-----------------------------------"
}

# 主函数
main() {
    local command=$1
    
    check_docker
    check_docker_compose
    
    case "$command" in
        start)
            start_containers
            ;;
        stop)
            stop_containers
            ;;
        restart)
            restart_containers
            ;;
        down)
            down_containers
            ;;
        clean)
            clean_all
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        ps)
            list_containers
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
