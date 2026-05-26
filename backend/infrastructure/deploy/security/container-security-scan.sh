#!/bin/bash
# ==========================================
# 容器安全扫描脚本
# AI-Ready 测试环境
# ==========================================

set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 扫描结果目录
SCAN_DIR="/tmp/security-scan-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$SCAN_DIR"

echo "========================================"
echo "  AI-Ready 容器安全扫描"
echo "  时间: $(date)"
echo "========================================"

# 1. Docker安全基线检查
echo -e "\n${YELLOW}[1/6] Docker安全基线检查${NC}"

# 1.1 检查Docker版本
echo -e "\n[1.1] Docker版本信息:"
docker version --format 'Server Version: {{.Server.Version}}' 2>/dev/null || echo "Docker未运行"

# 1.2 检查Docker守护进程配置
echo -e "\n[1.2] Docker守护进程安全配置:"
if [ -f /etc/docker/daemon.json ]; then
    echo "Docker守护进程配置:"
    cat /etc/docker/daemon.json | grep -E '"userns-remap"|"live-restore"|"userland-proxy"|"no-new-privileges"' || echo "  未找到安全配置"
else
    echo -e "${RED}  警告: /etc/docker/daemon.json 不存在${NC}"
fi

# 1.3 检查容器运行时
echo -e "\n[1.3] 容器运行时检查:"
docker info 2>/dev/null | grep -E 'Security Options|Cgroup Driver|Logging Driver' || echo "无法获取Docker信息"

# 2. 镜像安全扫描
echo -e "\n${YELLOW}[2/6] 镜像安全扫描${NC}"

# 2.1 列出所有镜像
echo -e "\n[2.1] 本地镜像列表:"
docker images --format "{{.Repository}}:{{.Tag}} ({{.Size}})" | head -20

# 2.2 使用Trivy扫描镜像 (如果已安装)
echo -e "\n[2.2] 镜像漏洞扫描:"
if command -v trivy &> /dev/null; then
    echo "使用Trivy进行扫描..."
    docker images --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>" | while read image; do
        echo -e "\n  扫描镜像: $image"
        trivy image --severity HIGH,CRITICAL --no-progress "$image" 2>/dev/null | tee "$SCAN_DIR/trivy-$(echo $image | tr '/' '_').txt" || echo "  扫描失败"
    done
else
    echo -e "${YELLOW}  Trivy未安装，跳过镜像漏洞扫描${NC}"
    echo "  安装命令: curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin"
fi

# 3. 容器运行时安全检查
echo -e "\n${YELLOW}[3/6] 容器运行时安全检查${NC}"

# 3.1 特权容器检查
echo -e "\n[3.1] 特权容器检查:"
PRIVILEGED=$(docker ps -q | xargs -I {} docker inspect --format='{{.Name}}: {{.HostConfig.Privileged}}' {} 2>/dev/null | grep "true" || true)
if [ -n "$PRIVILEGED" ]; then
    echo -e "${RED}  发现特权容器:${NC}"
    echo "$PRIVILEGED"
else
    echo -e "${GREEN}  ✓ 未发现特权容器${NC}"
fi

# 3.2 容器能力检查
echo -e "\n[3.2] 容器能力(Capabilities)检查:"
docker ps -q | while read container; do
    CAPS=$(docker inspect --format='{{.Name}}: {{.HostConfig.CapAdd}} {{.HostConfig.CapDrop}}' "$container" 2>/dev/null)
    if echo "$CAPS" | grep -q "CapAdd"; then
        echo "  $CAPS"
    fi
done

# 3.3 容器资源限制检查
echo -e "\n[3.3] 容器资源限制检查:"
docker ps -q | while read container; do
    NAME=$(docker inspect --format='{{.Name}}' "$container" | sed 's/^\///')
    MEM_LIMIT=$(docker inspect --format='{{.HostConfig.Memory}}' "$container")
    CPU_LIMIT=$(docker inspect --format='{{.HostConfig.CpuQuota}}' "$container")
    
    if [ "$MEM_LIMIT" = "0" ]; then
        echo -e "  ${YELLOW}$NAME: 未设置内存限制${NC}"
    else
        echo "  $NAME: 内存限制=$(($MEM_LIMIT / 1024 / 1024))MB"
    fi
    
    if [ "$CPU_LIMIT" = "0" ]; then
        echo -e "  ${YELLOW}$NAME: 未设置CPU限制${NC}"
    else
        echo "  $NAME: CPU限制=$CPU_LIMIT"
    fi
done

# 4. 网络安全检查
echo -e "\n${YELLOW}[4/6] 容器网络安全检查${NC}"

# 4.1 检查Docker网络
echo -e "\n[4.1] Docker网络配置:"
docker network ls --format "{{.Name}} ({{.Driver}})" | while read network; do
    echo "  $network"
done

# 4.2 检查暴露端口
echo -e "\n[4.2] 容器端口暴露:"
docker ps --format "{{.Names}}: {{.Ports}}" | while read line; do
    echo "  $line"
done

# 5. 挂载安全检查
echo -e "\n${YELLOW}[5/6] 挂载安全检查${NC}"

echo -e "\n[5.1] 敏感目录挂载检查:"
docker ps -q | while read container; do
    MOUNTS=$(docker inspect --format='{{.Name}}: {{range .Mounts}}{{.Source}} -> {{.Destination}}; {{end}}' "$container" 2>/dev/null)
    if echo "$MOUNTS" | grep -qE '/etc|/var/run/docker\.sock|/proc|/sys'; then
        echo -e "  ${RED}警告: $MOUNTS${NC}"
    fi
done

# 6. 生成报告
echo -e "\n${YELLOW}[6/6] 生成安全报告${NC}"

REPORT_FILE="$SCAN_DIR/security-scan-report.md"
cat > "$REPORT_FILE" << EOF
# 容器安全扫描报告

## 扫描信息
- **时间**: $(date)
- **扫描目录**: $SCAN_DIR
- **Docker版本**: $(docker version --format '{{.Server.Version}}' 2>/dev/null || echo 'N/A')

## 扫描结果摘要

### 1. 容器统计
- **运行中容器**: $(docker ps -q | wc -l)
- **总镜像数**: $(docker images -q | wc -l)
- **特权容器**: $(docker ps -q | xargs -I {} docker inspect --format='{{.HostConfig.Privileged}}' {} 2>/dev/null | grep -c "true" || echo "0")

### 2. 安全发现

$(if [ -n "$PRIVILEGED" ]; then echo "#### ⚠️ 特权容器"; echo "\`\`\`"; echo "$PRIVILEGED"; echo "\`\`\`"; else echo "#### ✓ 未发现特权容器"; fi)

### 3. 建议修复项
1. 为所有容器设置内存和CPU限制
2. 避免使用特权模式运行容器
3. 定期更新基础镜像
4. 启用Docker内容信任 (DCT)

## 详细扫描结果
- 扫描日志目录: $SCAN_DIR

EOF

echo -e "\n${GREEN}扫描完成!${NC}"
echo "报告位置: $REPORT_FILE"
echo "扫描日志目录: $SCAN_DIR"

# 显示关键发现
echo -e "\n${YELLOW}关键安全发现:${NC}"
PRIV_COUNT=$(docker ps -q | xargs -I {} docker inspect --format='{{.HostConfig.Privileged}}' {} 2>/dev/null | grep -c "true" || echo "0")
echo "  - 特权容器: $PRIV_COUNT"

NO_MEM_LIMIT=$(docker ps -q | while read c; do docker inspect --format='{{.HostConfig.Memory}}' "$c"; done | grep -c "^0$" || echo "0")
echo "  - 未设置内存限制的容器: $NO_MEM_LIMIT"

NO_CPU_LIMIT=$(docker ps -q | while read c; do docker inspect --format='{{.HostConfig.CpuQuota}}' "$c"; done | grep -c "^0$" || echo "0")
echo "  - 未设置CPU限制的容器: $NO_CPU_LIMIT"

exit 0
