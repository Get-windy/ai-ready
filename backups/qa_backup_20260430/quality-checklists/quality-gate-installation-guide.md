# Sprint 27+1 质量门禁检查系统安装指南

**版本**: 1.0  
**创建日期**: 2026-04-27  
**作者**: qa-lead  
**项目**: AI-Ready企业级ERP系统  
**Sprint**: Sprint 27+1  

---

## 一、系统概述

### 1.1 什么是质量门禁检查系统
质量门禁检查系统是为Sprint 27+1测试环境设计的自动化质量检查工具集，用于确保测试环境符合项目质量标准和DoD要求。

### 1.2 核心功能
- ✅ **自动化检查**: 自动执行服务器资源、软件环境、功能性能等检查
- ✅ **实时监控**: 监控测试环境状态和健康度
- ✅ **质量评估**: 根据检查结果评估环境质量
- ✅ **报告生成**: 生成详细的质量检查报告
- ✅ **问题跟踪**: 跟踪发现的质量问题

### 1.3 系统架构
```
质量门禁检查系统
├── 检查引擎 (Python脚本)
├── 检查脚本库
├── 报告生成器
├── 数据库 (检查结果存储)
└── Web界面 (可选)
```

---

## 二、安装要求

### 2.1 硬件要求
| 组件 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 2核心 | 4核心 |
| 内存 | 4GB | 8GB |
| 磁盘空间 | 10GB | 20GB |
| 网络 | 10Mbps | 100Mbps |

### 2.2 软件要求
| 软件 | 版本要求 | 说明 |
|------|----------|------|
| Python | 3.8+ | 必须 |
| pip | 最新版 | Python包管理器 |
| Git | 2.30+ | 可选，用于版本控制 |
| 操作系统 | Windows/Linux/macOS | 支持主流操作系统 |

### 2.3 Python依赖包
核心依赖包：
```
psutil>=5.9.0
requests>=2.28.0
paramiko>=3.1.0
pyyaml>=6.0
pymongo>=4.3.0 (可选)
jinja2>=3.1.0 (可选)
```

---

## 三、安装步骤

### 3.1 环境准备

#### 3.1.1 Windows系统
```powershell
# 1. 安装Python 3.8+
# 访问 https://www.python.org/downloads/ 下载安装包
# 安装时勾选 "Add Python to PATH"

# 2. 验证安装
python --version
pip --version

# 3. 安装Git (可选)
# 访问 https://git-scm.com/download/win 下载安装
```

#### 3.1.2 Linux系统 (Ubuntu/Debian)
```bash
# 1. 更新系统
sudo apt update
sudo apt upgrade -y

# 2. 安装Python和pip
sudo apt install python3 python3-pip python3-venv -y

# 3. 安装Git
sudo apt install git -y

# 4. 验证安装
python3 --version
pip3 --version
git --version
```

#### 3.1.3 macOS系统
```bash
# 1. 安装Homebrew (如果未安装)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. 安装Python
brew install python

# 3. 安装Git
brew install git

# 4. 验证安装
python3 --version
pip3 --version
git --version
```

### 3.2 获取质量门禁检查系统

#### 3.2.1 从项目目录获取
```bash
# 进入项目QA目录
cd I:\AI-Ready\qa

# 查看质量门禁检查文件
ls quality-checklists/
ls scripts/
```

#### 3.2.2 创建虚拟环境 (推荐)
```bash
# 创建虚拟环境目录
mkdir -p ~/quality-gate-env
cd ~/quality-gate-env

# 创建Python虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows
venv\Scripts\activate
# Linux/macOS
source venv/bin/activate
```

### 3.3 安装依赖包
```bash
# 确保在虚拟环境中
# 安装核心依赖
pip install psutil requests paramiko pyyaml

# 安装可选依赖
pip install pymongo jinja2

# 安装开发依赖
pip install pytest coverage black flake8
```

### 3.4 配置质量门禁检查系统

#### 3.4.1 创建配置文件
```bash
# 创建配置目录
mkdir -p ~/.quality-gate
cd ~/.quality-gate

# 创建配置文件
cat > config.yaml << 'EOF'
# 质量门禁检查系统配置
version: "1.0"
environment: "test"

# 检查配置
checks:
  server_resources:
    enabled: true
    interval: 3600  # 1小时
  os_security:
    enabled: true
    interval: 86400  # 24小时
  network_connectivity:
    enabled: true
    interval: 300  # 5分钟
  runtime_versions:
    enabled: true
    interval: 3600
  database_services:
    enabled: true
    interval: 300
  service_status:
    enabled: true
    interval: 60  # 1分钟

# 报告配置
reporting:
  output_dir: "./reports"
  format: ["json", "html"]
  retention_days: 30

# 通知配置
notifications:
  email:
    enabled: false
    smtp_server: "smtp.example.com"
    smtp_port: 587
    from_address: "quality-gate@example.com"
    to_addresses: ["admin@example.com"]
  slack:
    enabled: false
    webhook_url: ""
  dingtalk:
    enabled: false
    webhook_url: ""

# 数据库配置 (可选)
database:
  enabled: false
  type: "mongodb"  # 或 "sqlite"
  connection_string: "mongodb://localhost:27017"
  database_name: "quality_gate"
EOF
```

#### 3.4.2 创建环境变量文件
```bash
# 创建环境变量文件
cat > .env << 'EOF'
# 质量门禁检查系统环境变量
QUALITY_GATE_ENV=test
QUALITY_GATE_CONFIG_PATH=~/.quality-gate/config.yaml
QUALITY_GATE_LOG_LEVEL=INFO
QUALITY_GATE_OUTPUT_DIR=./reports

# 目标环境配置
TARGET_ENVIRONMENT=test
TARGET_HOST=localhost
TARGET_USER=admin
# TARGET_PASSWORD=  # 在安全位置设置
EOF

# 设置环境变量
# Windows
setx QUALITY_GATE_ENV test
# Linux/macOS
export QUALITY_GATE_ENV=test
```

### 3.5 验证安装
```bash
# 进入脚本目录
cd I:\AI-Ready\qa\scripts

# 运行测试检查
python quality-gate-check-server-resources.py --help

# 运行完整检查
python run-quality-gate-checks.py --quick

# 验证安装结果
if [ $? -eq 0 ]; then
    echo "✅ 质量门禁检查系统安装成功"
else
    echo "❌ 安装失败，请检查错误信息"
fi
```

---

## 四、使用指南

### 4.1 基本使用

#### 4.1.1 运行单个检查
```bash
# 进入脚本目录
cd I:\AI-Ready\qa\scripts

# 运行服务器资源检查
python quality-gate-check-server-resources.py

# 运行操作系统安全检查
python quality-gate-check-os-security.py

# 运行网络连通性检查
python quality-gate-check-network-connectivity.py
```

#### 4.1.2 运行完整检查
```bash
# 运行所有检查
python run-quality-gate-checks.py --all

# 运行快速检查（关键检查）
python run-quality-gate-checks.py --quick

# 运行指定检查
python run-quality-gate-checks.py --checks server_resources network_connectivity

# 跳过某些检查
python run-quality-gate-checks.py --all --skip os_security
```

#### 4.1.3 生成HTML报告
```bash
# 运行检查并生成HTML报告
python run-quality-gate-checks.py --all --html

# 报告文件位置
# HTML报告: quality-gate-results/quality-gate-report.html
# JSON摘要: quality-gate-results/quality-gate-summary.json
# 详细报告: quality-gate-results/reports/
```

### 4.2 高级功能

#### 4.2.1 定时检查
```bash
# 创建定时任务 (Linux crontab)
crontab -e

# 添加以下内容（每小时执行一次）
0 * * * * cd /path/to/qa/scripts && python run-quality-gate-checks.py --quick >> /var/log/quality-gate.log 2>&1

# 创建定时任务 (Windows Task Scheduler)
# 1. 打开任务计划程序
# 2. 创建基本任务
# 3. 设置触发器：每天每小时
# 4. 设置操作：启动程序
#    程序：python.exe
#    参数：run-quality-gate-checks.py --quick
#    起始于：I:\AI-Ready\qa\scripts
```

#### 4.2.2 集成到CI/CD流水线
```yaml
# GitHub Actions 示例
name: Quality Gate Check

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  quality-gate:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.9'
    
    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install psutil requests
        
    - name: Run quality gate checks
      run: |
        cd qa/scripts
        python run-quality-gate-checks.py --quick
        
    - name: Upload reports
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: quality-gate-reports
        path: qa/scripts/quality-gate-results/
```

#### 4.2.3 API接口 (可选)
```python
# 使用Python API调用质量门禁检查
from quality_gate_sdk import QualityGateClient

# 创建客户端
client = QualityGateClient(
    base_url="http://localhost:8000",
    api_key="your-api-key"
)

# 运行检查
result = client.run_checks(
    checks=["server_resources", "network_connectivity"],
    environment="test"
)

# 获取检查结果
report = client.get_report(result.report_id)

# 导出报告
client.export_report(report.id, format="html")
```

### 4.3 监控和告警

#### 4.3.1 设置监控仪表板
```bash
# 安装Grafana (可选)
# 参考: https://grafana.com/docs/grafana/latest/installation/

# 配置数据源
# 1. 添加Prometheus数据源
# 2. 导入质量门禁检查仪表板
# 3. 配置告警规则
```

#### 4.3.2 配置告警通知
```yaml
# 修改配置文件 config.yaml
notifications:
  email:
    enabled: true
    smtp_server: "smtp.gmail.com"
    smtp_port: 587
    from_address: "quality-gate@your-company.com"
    to_addresses: ["team@your-company.com", "manager@your-company.com"]
    
  dingtalk:
    enabled: true
    webhook_url: "https://oapi.dingtalk.com/robot/send?access_token=your_token"
    
  slack:
    enabled: true
    webhook_url: "https://hooks.slack.com/services/your/webhook"
```

---

## 五、维护和故障排除

### 5.1 日常维护

#### 5.1.1 更新检查脚本
```bash
# 从Git仓库更新
cd I:\AI-Ready
git pull origin main

# 或手动更新
# 1. 备份现有脚本
cp -r qa/scripts qa/scripts_backup_$(date +%Y%m%d)

# 2. 复制新脚本
# 3. 测试新脚本
cd qa/scripts
python run-quality-gate-checks.py --quick
```

#### 5.1.2 清理报告文件
```bash
# 自动清理旧报告
find ./quality-gate-results/reports -name "*.json" -mtime +30 -delete
find ./quality-gate-results/reports -name "*.log" -mtime +30 -delete

# 或使用脚本清理
python cleanup-old-reports.py --days 30
```

#### 5.1.3 备份检查配置
```bash
# 备份配置文件
tar -czf quality-gate-backup-$(date +%Y%m%d).tar.gz \
    ~/.quality-gate \
    I:\AI-Ready\qa\quality-checklists \
    I:\AI-Ready\qa\scripts

# 存储到安全位置
# 云存储: s3, gcs, azure blob
# 本地存储: NAS, 备份服务器
```

### 5.2 故障排除

#### 5.2.1 常见问题

**问题1: Python依赖安装失败**
```bash
# 解决方案:
# 1. 更新pip
python -m pip install --upgrade pip

# 2. 使用国内镜像源
pip install -i https://pypi.tuna.tsinghua.edu.cn/simple psutil requests

# 3. 安装特定版本
pip install psutil==5.9.0
```

**问题2: 检查脚本权限不足**
```bash
# 解决方案:
# 1. 检查文件权限
chmod +x I:\AI-Ready\qa\scripts\*.py

# 2. 以管理员身份运行
# Windows: 以管理员身份运行PowerShell
# Linux: sudo python run-quality-gate-checks.py
```

**问题3: 网络检查失败**
```bash
# 解决方案:
# 1. 检查网络连接
ping 8.8.8.8

# 2. 配置代理
export http_proxy=http://proxy.example.com:8080
export https_proxy=http://proxy.example.com:8080

# 3. 修改检查目标
# 编辑 network_connectivity.py，修改测试目标
```

**问题4: 报告生成失败**
```bash
# 解决方案:
# 1. 检查磁盘空间
df -h

# 2. 检查目录权限
ls -la ./quality-gate-results/

# 3. 手动创建目录
mkdir -p ./quality-gate-results/reports
```

#### 5.2.2 调试模式
```bash
# 启用详细日志
export QUALITY_GATE_LOG_LEVEL=DEBUG

# 运行检查并输出详细日志
python run-quality-gate-checks.py --all --verbose 2>&1 | tee debug.log

# 分析日志文件
grep -i "error\|fail\|exception" debug.log
```

#### 5.2.3 获取帮助
```bash
# 查看帮助文档
python run-quality-gate-checks.py --help

# 查看脚本文档
pydoc I:\AI-Ready\qa\scripts\quality-gate-check-server-resources.py

# 联系支持
# 1. 查看项目文档
# 2. 提交Issue到Git仓库
# 3. 联系质量保障团队
```

### 5.3 性能优化

#### 5.3.1 优化检查频率
```yaml
# 调整检查间隔 (config.yaml)
checks:
  server_resources:
    interval: 7200  # 2小时（原1小时）
  os_security:
    interval: 172800  # 48小时（原24小时）
  network_connectivity:
    interval: 600  # 10分钟（原5分钟）
```

#### 5.3.2 并行执行检查
```python
# 修改 run-quality-gate-checks.py，使用多线程
import concurrent.futures

def run_checks_parallel(checks):
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        futures = {executor.submit(run_check, check): check for check in checks}
        results = []
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
    return results
```

#### 5.3.3 缓存检查结果
```python
# 实现结果缓存
import hashlib
import pickle
import os

def get_cache_key(check_name, params):
    key_str = f"{check_name}:{json.dumps(params, sort_keys=True)}"
    return hashlib.md5(key_str.encode()).hexdigest()

def cached_check(check_func, cache_dir=".cache", ttl=3600):
    def wrapper(*args, **kwargs):
        cache_key = get_cache_key(check_func.__name__, kwargs)
        cache_file = os.path.join(cache_dir, f"{cache_key}.pkl")
        
        # 检查缓存
        if os.path.exists(cache_file):
            mtime = os.path.getmtime(cache_file)
            if time.time() - mtime < ttl:
                with open(cache_file, 'rb') as f:
                    return pickle.load(f)
        
        # 执行检查
        result = check_func(*args, **kwargs)
        
        # 保存缓存
        os.makedirs(cache_dir, exist_ok=True)
        with open(cache_file, 'wb') as f:
            pickle.dump(result, f)
        
        return result
    return wrapper
```

---

## 六、安全考虑

### 6.1 安全最佳实践

#### 6.1.1 凭证管理
```bash
# 使用环境变量存储敏感信息
export TARGET_PASSWORD=$(cat /path/to/secure/password-file)

# 或使用密钥管理服务
# AWS Secrets Manager, Azure Key Vault, HashiCorp Vault
```

#### 6.1.2 访问控制
```yaml
# 配置访问控制列表 (acl.yaml)
allowed_users:
  - username: "admin"
    role: "administrator"
    permissions: ["read", "write", "execute", "delete"]
  - username: "viewer"
    role: "viewer"
    permissions: ["read"]
  
allowed_ips:
  - "192.168.1.0/24"
  - "10.0.0.0/8"
  
rate_limiting:
  requests_per_minute: 60
  burst_limit: 10
```

#### 6.1.3 审计日志
```python
# 启用详细审计日志
import logging

audit_logger = logging.getLogger('quality_gate_audit')
audit_handler = logging.FileHandler('/var/log/quality-gate-audit.log')
audit_handler.setFormatter(logging.Formatter(
    '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
))
audit_logger.addHandler(audit_handler)
audit_logger.setLevel(logging.INFO)

# 记录所有检查操作
def audit_log(check_name, user, action, result):
    audit_logger.info(
        f"Check: {check_name}, User: {user}, "
        f"Action: {action}, Result: {result}"
    )
```

### 6.2 合规性检查

#### 6.2.1 数据保护
```yaml
# 配置数据保护策略
data_protection:
  encryption:
    enabled: true
    algorithm: "AES-256-GCM"
    key_rotation_days: 90
    
  data_retention:
    reports: 365  # 天
    logs: 180
    audit_trails: 1095  # 3年
    
  data_deletion:
    auto_delete: true
    schedule: "0 2 * * *"  # 每天2点
```

#### 6.2.2 合规性检查
```bash
# 运行合规性检查
python compliance-check.py --standard gdpr
python compliance-check.py --standard iso27001
python compliance-check.py --standard soc2
```

---

## 七、扩展和定制

### 7.1 添加自定义检查

#### 7.1.1 创建自定义检查脚本
```python
# custom_check_example.py
#!/usr/bin/env python3

import json
import sys
from datetime import datetime

def check_custom_requirement():
    """检查自定义需求"""
    result = {
        "check_id": "custom_requirement",
        "check_name": "自定义需求检查",
        "requirement": "满足业务特定需求",
        "actual_value": "检查中...",
        "status": "RUNNING",
        "passed": False,
        "details": {}
    }
    
    try:
        # 实现自定义检查逻辑
        # 例如：检查特定服务、验证业务规则等
        
        # 模拟检查结果
        result["actual_value"] = "满足要求"
        result["status"] = "PASSED"
        result["passed"] = True
        result["details"] = {
            "checked_at": datetime.now().isoformat(),
            "custom_metric": 95.5
        }
        
    except Exception as e:
        result["actual_value"] = "检查失败"
        result["status"] = "FAILED"
        result["error"] = str(e)
    
    return result

def main():
    result = check_custom_requirement()
    print(json.dumps(result, ensure_ascii=False, indent=2))
    
    # 根据检查结果返回退出码
    sys.exit(0 if result["passed"] else 1)

if __name__ == "__main__":
    main()
```

#### 7.1.2 集成到质量门禁系统
```yaml
# 在配置文件中添加自定义检查
checks:
  custom_requirement:
    enabled: true
    script: "custom_check_example.py"
    interval: 3600
    parameters:
      threshold: 90
      timeout: 30
```

### 7.2 开发插件系统

#### 7.2.1 插件接口
```python
# plugin_interface.py
from abc import ABC, abstractmethod
from typing import Dict, Any

class QualityGatePlugin(ABC):
    """质量门禁插件接口"""
    
    @abstractmethod
    def get_plugin_info(self) -> Dict[str, Any]:
        """获取插件信息"""
        pass
    
    @abstractmethod
    def execute_check(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行检查"""
        pass
    
    @abstractmethod
    def validate_configuration(self, config: Dict[str, Any]) -> bool:
        """验证配置"""
        pass
```

#### 7.2.2 示例插件
```python
# security_scan_plugin.py
import requests
from plugin_interface import QualityGatePlugin

class SecurityScanPlugin(QualityGatePlugin):
    
    def get_plugin_info(self):
        return {
            "name": "Security Scan Plugin",
            "version": "1.0",
            "author": "qa-lead",
            "description": "安全扫描插件",
            "supported_checks": ["vulnerability_scan", "security_audit"]
        }
    
    def execute_check(self, parameters):
        # 实现安全扫描逻辑
        target_url = parameters.get("target_url")
        scan_type = parameters.get("scan_type", "quick")
        
        # 调用安全扫描API
        result = {
            "check_name": f"Security Scan - {scan_type}",
            "status": "COMPLETED",
            "vulnerabilities_found": 0,
            "scan_details": {}
        }
        
        return result
    
    def validate_configuration(self, config):
        required_fields = ["target_url", "api_key"]
        return all(field in config for field in required_fields)
```

---

## 八、附录

### 8.1 文件结构
```
I:\AI-Ready\qa\
├── quality-checklists\                    # 质量检查清单
│   ├── test-environment-quality-checklist.md
│   ├── sprint27-plus1-quality-gate-checklist.md  # Sprint 27+1质量门禁清单
│   └── quality-gate-installation-guide.md        # 本安装指南
│
├── scripts\                               # 检查脚本
│   ├── quality-gate-check-server-resources.py
│   ├── quality-gate-check-os-security.py
│   ├── quality-gate-check-network-connectivity.py
│   ├── quality-gate-check-runtime-versions.py
│   ├── quality-gate-check-database-services.py
│   ├── quality-gate-check-service-status.py
│   ├── quality-gate-test-core-functionality.py
│   └── run-quality-gate-checks.py        # 主执行脚本
│
├── templates\                            # 报告模板（可选）
├── plugins\                              # 插件目录（可选）
└── docs\                                 # 文档目录
```

### 8.2 命令速查表
| 命令 | 描述 | 示例 |
|------|------|------|
| `python run-quality-gate-checks.py --quick` | 快速检查 | `python run-quality-gate-checks.py --quick` |
| `python run-quality-gate-checks.py --all` | 完整检查 | `python run-quality-gate-checks.py --all` |
| `python run-quality-gate-checks.py --checks name1 name2` | 指定检查 | `python run-quality-gate-checks.py --checks server_resources network` |
| `python run-quality-gate-checks.py --html` | 生成HTML报告 | `python run-quality-gate-checks.py --all --html` |
| `python quality-gate-check-server-resources.py` | 单个检查 | `python quality-gate-check-server-resources.py` |

### 8.3 故障代码
| 错误代码 | 含义 | 解决方案 |
|----------|------|----------|
| QG-001 | 依赖包缺失 | 安装缺失的Python包 |
| QG-002 | 权限不足 | 以管理员/root身份运行 |
| QG-003 | 网络连接失败 | 检查网络配置和防火墙 |
| QG-004 | 磁盘空间不足 | 清理磁盘空间 |
| QG-005 | 配置错误 | 检查配置文件格式和内容 |
| QG-006 | 脚本执行超时 | 增加超时时间或优化脚本 |
| QG-007 | 目标服务不可用 | 检查目标服务状态 |
| QG-008 | 报告生成失败 | 检查输出目录权限 |

### 8.4 联系支持
- **项目仓库**: [AI-Ready项目](https://github.com/your-org/ai-ready)
- **问题跟踪**: 提交Issue到项目仓库
- **文档**: 查看项目文档目录
- **邮件**: quality-team@your-company.com
- **紧急支持**: 联系项目协调员

### 8.5 版本历史
| 版本 | 日期 | 描述 | 作者 |
|------|------|------|------|
| 1.0 | 2026-04-27 | 初始版本，创建完整安装指南 | qa-lead |
| 1.1 | 2026-04-28 | 添加故障排除章节 | qa-lead |
| 1.2 | 2026-04-29 | 添加安全考虑和扩展章节 | qa-lead |

---

**文档维护**: 
- 定期更新以反映系统变更
- 记录所有配置变更
- 保持与最新代码版本同步

**反馈和改进**: 
欢迎提供反馈和改进建议，帮助我们改进质量门禁检查系统。