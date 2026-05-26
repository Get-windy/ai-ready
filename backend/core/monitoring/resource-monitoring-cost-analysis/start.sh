#!/bin/bash

# 资源监控与成本分析模块快速启动脚本

echo "========================================="
echo "  资源监控与成本分析模块快速启动"
echo "  Sprint 27+1 测试环境"
echo "========================================="
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker服务未运行"
    echo "请先启动Docker服务"
    exit 1
fi

# 检查docker-compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: docker-compose未安装"
    echo "请先安装docker-compose"
    exit 1
fi

# 显示菜单
echo "请选择操作:"
echo "1. 初始化环境并部署"
echo "2. 仅部署监控栈"
echo "3. 启动监控栈"
echo "4. 停止监控栈"
echo "5. 重启监控栈"
echo "6. 查看状态"
echo "7. 查看日志"
echo "8. 运行测试"
echo "9. 备份数据"
echo "0. 退出"
echo ""

read -p "请输入选择 (0-9): " choice

case $choice in
    1)
        echo "正在初始化环境并部署..."
        ./deploy.sh --init
        ./deploy.sh --deploy
        ;;
    2)
        echo "正在部署监控栈..."
        ./deploy.sh --deploy
        ;;
    3)
        echo "正在启动监控栈..."
        docker-compose up -d
        echo "监控栈已启动"
        ;;
    4)
        echo "正在停止监控栈..."
        docker-compose down
        echo "监控栈已停止"
        ;;
    5)
        echo "正在重启监控栈..."
        docker-compose restart
        echo "监控栈已重启"
        ;;
    6)
        echo "监控栈状态:"
        docker-compose ps
        echo ""
        echo "访问地址:"
        echo "  Prometheus: http://localhost:9090"
        echo "  Grafana:    http://localhost:3000"
        echo "  监控服务:   http://localhost:8000"
        ;;
    7)
        echo "请选择查看哪个服务的日志:"
        echo "1. 资源监控服务"
        echo "2. Prometheus"
        echo "3. Grafana"
        echo "4. 所有服务"
        read -p "请输入选择 (1-4): " log_choice
        
        case $log_choice in
            1)
                docker-compose logs -f resource-monitoring-service
                ;;
            2)
                docker-compose logs -f prometheus
                ;;
            3)
                docker-compose logs -f grafana
                ;;
            4)
                docker-compose logs -f
                ;;
            *)
                echo "无效选择"
                ;;
        esac
        ;;
    8)
        echo "正在运行测试..."
        ./deploy.sh --test
        ;;
    9)
        echo "正在备份数据..."
        ./deploy.sh --backup
        ;;
    0)
        echo "退出"
        exit 0
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo ""
echo "操作完成！"