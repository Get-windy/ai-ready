# macOS兼容性测试指南

## 1. 支持的版本

| macOS版本 | 代号 | 发布年份 | 支持状态 |
|-----------|------|----------|----------|
| Monterey | 12 | 2021 | ✅ 完全支持 |
| Ventura | 13 | 2022 | ✅ 完全支持 |
| Sonoma | 14 | 2023 | ✅ 完全支持 |

## 2. 核心测试用例

### 2.1 系统要求检查
```bash
#!/bin/bash
# check-macos-requirements.sh

echo "检查macOS系统要求..."

# 检查macOS版本
SW_VERS=$(sw_vers -productVersion)
echo "macOS版本: $SW_VERS"

# 检查架构
ARCH=$(uname -m)
echo "系统架构: $ARCH"

# 检查可用内存
MEMORY_GB=$(sysctl -n hw.memsize | awk '{print $0/1024/1024/1024}')
echo "内存大小: ${MEMORY_GB}GB"

# 检查磁盘空间
DISK_SPACE=$(df -h / | awk 'NR==2 {print $4}')
echo "可用磁盘空间: $DISK_SPACE"
```

### 2.2 Homebrew依赖管理
```bash
# 检查Homebrew和依赖包
check_brew_dependencies() {
    # 检查Homebrew是否安装
    if ! command -v brew &> /dev/null; then
        echo "安装Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    
    # 必要依赖包
    REQUIRED_BREWS=(
        "openjdk@11"
        "python@3.9"
        "node"
        "postgresql"
        "redis"
    )
    
    for brew_pkg in "${REQUIRED_BREWS[@]}"; do
        if ! brew list | grep -q "^${brew_pkg}$"; then
            echo "安装 $brew_pkg..."
            brew install $brew_pkg
        fi
    done
}
```

### 2.3 权限和安全测试
```bash
# 测试macOS权限系统
test_macos_permissions() {
    # 测试文件访问权限
    TEST_FILE="$HOME/price-strategy-test.txt"
    echo "test data" > "$TEST_FILE"
    
    # 验证文件权限
    ls -la "$TEST_FILE"
    
    # 测试应用程序沙箱（如果需要）
    if [[ "$ARCH" == "arm64" ]]; then
        echo "检查Rosetta 2兼容性..."
        softwareupdate --install-rosetta --agree-to-license
    fi
    
    # 清理测试文件
    rm "$TEST_FILE"
}
```

### 2.4 启动代理配置
```bash
# 配置macOS启动代理（LaunchAgent）
configure_launch_agent() {
    LAUNCH_AGENT_DIR="$HOME/Library/LaunchAgents"
    LAUNCH_AGENT_FILE="com.example.pricestrategy.plist"
    
    # 创建LaunchAgent配置
    cat > "$LAUNCH_AGENT_DIR/$LAUNCH_AGENT_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.example.pricestrategy</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/local/bin/java</string>
        <string>-jar</string>
        <string>/Applications/PriceStrategy/price-strategy.jar</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/tmp/price-strategy.log</string>
    <key>StandardErrorPath</key>
    <string>/tmp/price-strategy-error.log</string>
</dict>
</plist>
EOF
    
    # 加载和启动服务
    launchctl load "$LAUNCH_AGENT_DIR/$LAUNCH_AGENT_FILE"
    launchctl start com.example.pricestrategy
}
```

## 3. 自动化测试脚本

```bash
#!/bin/bash
# run-macos-compatibility-tests.sh

echo "=== macOS兼容性测试开始 ==="

# 1. 基础系统测试
echo "1. 系统基础测试..."
sw_vers
system_profiler SPHardwareDataType | grep -E "Model Identifier|Processor|Memory"

# 2. 网络连接测试
echo "2. 网络连接测试..."
ping -c 3 8.8.8.8
nc -z api.example.com 443 && echo "API端点可达" || echo "API端点不可达"

# 3. Java环境测试
echo "3. Java环境测试..."
if command -v java &> /dev/null; then
    java -version
else
    echo "Java未安装，通过Homebrew安装..."
    brew install openjdk@11
fi

# 4. 文件系统测试
echo "4. 文件系统测试..."
TEST_DIR="/tmp/price-strategy-test-$$"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"
echo "测试文件读写..." > test.txt
cat test.txt
cd ..
rm -rf "$TEST_DIR"

# 5. 权限测试
echo "5. 权限测试..."
touch /tmp/test-permission.txt 2>/dev/null && echo "临时目录可写" || echo "临时目录权限受限"

# 6. 内存和CPU测试
echo "6. 系统资源测试..."
top -l 1 -s 0 | grep "CPU usage"
vm_stat | grep "free"

echo "=== macOS兼容性测试完成 ==="
```

## 4. 已知问题和解决方案

### 4.1 常见兼容性问题

| 问题 | 影响版本 | 解决方案 |
|------|----------|----------|
| Gatekeeper阻止未签名应用 | macOS 10.15+ | 使用开发者ID签名或临时允许 |
| 隐私权限要求 | macOS 10.14+ | 在系统偏好设置中授权 |
| Rosetta 2兼容性 | Apple Silicon | 确保x86_64二进制支持 |
| 文件系统大小写敏感 | 所有版本 | 使用不区分大小写的APFS |
| 端口占用 | 所有版本 | 检查并释放被占用的端口 |

### 4.2 安全配置

```bash
# 配置macOS安全设置
configure_macos_security() {
    # 允许从任何来源下载的应用
    sudo spctl --master-disable
    
    # 配置防火墙（可选）
    sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
    
    # 配置屏幕录制权限（如果需要）
    # 需要通过系统偏好设置手动授权
}
```

## 5. 开发环境设置

### 5.1 Xcode命令行工具
```bash
# 安装Xcode命令行工具
install_xcode_tools() {
    if ! xcode-select -p &> /dev/null; then
        echo "安装Xcode命令行工具..."
        xcode-select --install
    fi
}
```

### 5.2 开发工具链
```bash
# 安装开发工具
install_dev_tools() {
    # Homebrew（如果未安装）
    if ! command -v brew &> /dev/null; then
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    
    # 开发工具
    brew install git
    brew install maven
    brew install gradle
    brew install docker --cask
}
```

## 6. 性能监控

### 6.1 系统监控脚本
```bash
#!/bin/bash
# monitor-macos-performance.sh

INTERVAL=5  # 监控间隔（秒）
DURATION=60 # 监控时长（秒）

echo "开始macOS性能监控 ($DURATION秒)..."
end_time=$((SECONDS + DURATION))

while [ $SECONDS -lt $end_time ]; do
    echo "=== $(date) ==="
    
    # CPU使用率
    cpu_usage=$(top -l 1 | grep "CPU usage" | awk '{print $3}' | tr -d '%')
    echo "CPU使用率: ${cpu_usage}%"
    
    # 内存使用
    memory_info=$(vm_stat | grep -E "Pages free|Pages active")
    echo "内存状态:"
    echo "$memory_info"
    
    # 磁盘I/O
    iostat -d 1 1 | grep -v "disk"
    
    # 网络连接
    netstat -an | grep ESTABLISHED | wc -l | xargs echo "活跃连接数:"
    
    sleep $INTERVAL
done

echo "性能监控完成"
```

## 7. CI/CD集成

### 7.1 GitHub Actions配置
```yaml
# .github/workflows/macos-tests.yml
name: macOS Compatibility Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  macos-tests:
    runs-on: macos-latest
    
    strategy:
      matrix:
        macos-version: [macos-12, macos-13, macos-14]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup macOS Test Environment
        run: |
          chmod +x ./os-database-compatibility-guide/scripts/setup-macos-env.sh
          ./os-database-compatibility-guide/scripts/setup-macos-env.sh
          
      - name: Run macOS Compatibility Tests
        run: |
          ./os-database-compatibility-guide/scripts/test-macos-compatibility.sh
          
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        with:
          name: macos-test-results-${{ matrix.macos-version }}
          path: test-results/macos/
```

## 8. 测试报告模板

### 8.1 测试结果汇总
```json
{
  "test_environment": {
    "macos_version": "14.0",
    "architecture": "arm64",
    "xcode_version": "15.0",
    "java_version": "11.0.20"
  },
  "test_results": {
    "system_checks": {
      "passed": 8,
      "failed": 0,
      "total": 8
    },
    "network_tests": {
      "passed": 3,
      "failed": 0,
      "total": 3
    },
    "permission_tests": {
      "passed": 4,
      "failed": 0,
      "total": 4
    },
    "performance_tests": {
      "passed": 5,
      "failed": 0,
      "total": 5
    }
  },
  "recommendations": [
    "确保所有二进制文件都有适当的代码签名",
    "在Apple Silicon上测试Rosetta 2兼容性",
    "配置必要的隐私权限"
  ]
}
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-01  
**测试环境**: macOS Sonoma 14.0 (Apple Silicon)  
**状态**: 已验证