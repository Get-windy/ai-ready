#!/bin/sh
# Docker健康检查脚本

set -e

# 检查Nginx进程是否运行
if ! pgrep -x "nginx" > /dev/null; then
    echo "Nginx进程未运行"
    exit 1
fi

# 检查Nginx配置语法
if ! nginx -t > /dev/null 2>&1; then
    echo "Nginx配置语法错误"
    exit 1
fi

# 检查80端口是否监听
if ! netstat -tln | grep -q ':80 '; then
    echo "Nginx未监听80端口"
    exit 1
fi

# 检查健康检查端点
if ! curl -f http://localhost/health > /dev/null 2>&1; then
    echo "健康检查端点访问失败"
    exit 1
fi

# 检查静态文件服务
if ! curl -f http://localhost/favicon.ico > /dev/null 2>&1; then
    echo "静态文件服务异常"
    exit 1
fi

# 所有检查通过
echo "健康检查通过"
exit 0