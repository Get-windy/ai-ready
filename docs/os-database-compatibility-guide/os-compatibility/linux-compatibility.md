# Linux兼容性测试指南

## 1. 支持的发行版

| 发行版 | 版本 | 架构 | 包管理器 | 支持状态 |
|--------|------|------|----------|----------|
| Ubuntu | 20.04 LTS, 22.04 LTS | x64 | apt | ✅ 完全支持 |
| CentOS | 7, 8 | x64 | yum | ✅ 完全支持 |
| RHEL | 8, 9 | x64 | dnf | ✅ 完全支持 |
| Debian | 11, 12 | x64 | apt | ✅ 完全支持 |

## 2. 核心测试用例

### 2.1 系统依赖检查
```bash
#!/bin/bash
# check-system-dependencies.sh

REQUIRED_PACKAGES=(
    "openjdk-11-jdk"
    "python3"
    "nodejs"
    "nginx"
    "postgresql-client"
)

echo "检查系统依赖..."
for pkg in "${REQUIRED_PACKAGES[@]}"; do
    if ! command -v ${pkg%%-*} &> /dev/null; then
        echo "❌ 缺失: $pkg"
    else
        echo "✅ 已安装: $pkg"
    fi
done
```

### 2.2 权限和文件系统测试
```bash
# 测试文件权限
test_file_permissions() {
    TEST_DIR="/opt/price-strategy"
    
    # 创建测试目录
    sudo mkdir -p $TEST_DIR
    sudo chown -R price-strategy:price-strategy $TEST_DIR
    sudo chmod -R 750 $TEST_DIR
    
    # 验证权限
    stat -c "%a %U %G" $TEST_DIR | grep "750 price-strategy price-strategy"
}
```

### 2.3 系统服务管理
```bash
# systemd服务测试
test_systemd_service() {
    SERVICE_NAME="price-strategy"
    
    # 配置systemd服务
    cat > /etc/systemd/system/${SERVICE_NAME}.service << EOF
[Unit]
Description=Price Strategy Service
After=network.target

[Service]
Type=simple
User=price-strategy
WorkingDirectory=/opt/price-strategy
ExecStart=/usr/bin/java -jar price-strategy.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
    
    # 测试服务管理
    sudo systemctl daemon-reload
    sudo systemctl start $SERVICE_NAME
    sudo systemctl status $SERVICE_NAME
    sudo systemctl enable $SERVICE_NAME
}
```

## 3. 自动化测试脚本

```bash
#!/bin/bash
# run-linux-compatibility-tests.sh

set -e

echo "=== Linux兼容性测试开始 ==="

# 1. 系统信息收集
echo "收集系统信息..."
uname -a
lsb_release -a
java -version
python3 --version

# 2. 文件系统测试
echo "测试文件系统..."
test_file_system() {
    TEST_FILE="/tmp/price-strategy-test.txt"
    echo "test data" > $TEST_FILE
    cat $TEST_FILE
    rm $TEST_FILE
}

# 3. 网络配置测试
echo "测试网络配置..."
ping -c 3 8.8.8.8
curl -I https://api.example.com

# 4. 进程管理测试
echo "测试进程管理..."
ps aux | grep -i java || true

# 5. 日志系统测试
echo "测试日志系统..."
journalctl --since "1 hour ago" | head -20

echo "=== Linux兼容性测试完成 ==="
```

## 4. 已知问题和解决方案

| 问题 | 解决方案 |
|------|----------|
| SELinux阻止文件访问 | `setenforce 0` 或配置SELinux策略 |
| 防火墙阻止端口 | 配置firewalld或iptables规则 |
| 用户权限不足 | 配置sudo规则或使用适当用户 |
| 系统资源限制 | 调整ulimit和内核参数 |

## 5. 性能监控

```bash
# 监控系统资源使用
monitor_resources() {
    while true; do
        echo "CPU使用率: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}')%"
        echo "内存使用: $(free -m | awk '/Mem:/ {print $3"/"$2"MB"}')"
        echo "磁盘使用: $(df -h / | awk '/\// {print $5}')"
        sleep 10
    done
}
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-01