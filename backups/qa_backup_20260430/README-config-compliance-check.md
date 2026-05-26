# 配置合规性检查自动化脚本

## 概述

本脚本是AI-Ready测试环境质量检查自动化框架的子任务3，用于自动执行测试环境的配置合规性检查。该脚本基于预定义的合规性规则库，检查操作系统、安全、网络、应用程序等配置是否符合标准和最佳实践。

## 功能特性

### 1. 多维度配置检查
- ✅ **操作系统配置**: 用户密码策略、SSH配置、系统日志配置
- ✅ **安全配置**: 防火墙状态、强制访问控制、关键文件权限
- ✅ **网络配置**: DNS配置、NTP时间同步、网络接口配置
- ✅ **应用程序配置**: Java配置、日志配置、数据库连接配置
- ✅ **数据库配置**: MySQL配置、Redis配置、连接池配置
- ✅ **监控配置**: Prometheus配置、日志收集配置
- ✅ **性能配置**: 系统资源限制、内核参数优化

### 2. 灵活的检查方法
- **命令输出检查**: 执行系统命令并验证输出
- **文件内容检查**: 检查配置文件内容和格式
- **文件存在性检查**: 验证关键文件是否存在
- **服务状态检查**: 检查系统服务的运行状态
- **环境变量检查**: 验证环境变量设置
- **端口监听检查**: 检查端口监听状态
- **自定义检查**: 支持Python自定义检查逻辑

### 3. 合规标准支持
- **企业基线标准**: 企业级系统配置的最低合规要求
- **安全最佳实践**: 遵循行业安全最佳实践
- **性能优化标准**: 系统性能优化的配置要求

### 4. 报告功能
- ✅ JSON格式报告（机器可读）
- ✅ HTML格式报告（可视化展示）
- ✅ Markdown格式报告（文档化）
- ✅ 合规性摘要和详细结果
- ✅ 修复建议和自动化脚本

### 5. 高级功能
- ✅ 多平台支持（Linux、Windows）
- ✅ 规则库管理（YAML格式）
- ✅ 合规性评估和评分
- ✅ 风险等级划分
- ✅ 自动化修复脚本生成

## 快速开始

### 1. 安装依赖

```bash
# 安装Python依赖
pip install -r requirements.txt
```

**requirements.txt:**
```
pyyaml>=6.0
requests>=2.28.0
psutil>=5.9.0
```

### 2. 运行检查

#### 基本使用：
```bash
# 使用默认配置运行检查
python config-compliance-check-script.py

# 使用指定配置文件
python config-compliance-check-script.py --config compliance-config.yaml

# 指定规则库文件
python config-compliance-check-script.py --rules compliance-rules-library.yaml

# 指定输出目录和格式
python config-compliance-check-script.py --output ./compliance-reports --format all

# 启用严格模式（所有检查必须通过）
python config-compliance-check-script.py --strict

# 生成修复建议
python config-compliance-check-script.py --remediation
```

#### 命令行参数：
| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `--config` | `-c` | 主配置文件路径 | `compliance-config.yaml` |
| `--rules` | `-r` | 规则库文件路径 | `compliance-rules-library.yaml` |
| `--output` | `-o` | 输出目录 | `./compliance-reports` |
| `--format` | `-f` | 输出格式（json/html/markdown/all） | `all` |
| `--strict` | `-s` | 严格模式（所有检查必须通过） | `false` |
| `--remediation` | `-m` | 生成修复建议 | `false` |
| `--category` | `-C` | 指定检查类别（多个用逗号分隔） | `all` |
| `--verbose` | `-v` | 启用详细输出 | `false` |

### 3. 查看结果

检查完成后，报告会保存在指定的输出目录中：
```
compliance-reports/
├── config-compliance-report-20260429_150022.json
├── config-compliance-report-20260429_150022.html
├── config-compliance-report-20260429_150022.md
└── remediation-plan-20260429_150022.sh
```

## 配置说明

### 1. 主配置文件示例

```yaml
# compliance-config.yaml
environment:
  name: "ai-ready-test"
  type: "test-environment"
  compliance_standard: "enterprise-baseline"

compliance:
  enabled_categories:
    - "os_config"
    - "security_config"
    - "network_config"
    - "application_config"
  strict_mode: false
  auto_remediation: false

reporting:
  output_formats: ["json", "html", "markdown"]
  output_dir: "./compliance-reports"
  generate_remediation_plan: true
```

### 2. 规则库管理

规则库使用YAML格式，支持丰富的规则定义：

```yaml
# 规则定义示例
os_config:
  rules:
    - id: "os_user_password_policy"
      name: "用户密码策略检查"
      description: "检查密码复杂度、过期策略等"
      severity: "high"
      platforms: ["linux", "windows"]
      check_method: "command"
      
      linux:
        command: "grep -E '^PASS_MAX_DAYS' /etc/login.defs"
        expected_patterns:
          - r"PASS_MAX_DAYS\s+(\d+)"
        expected_values:
          "PASS_MAX_DAYS": {"max": 90}
      
      remediation: |
        编辑 /etc/login.defs 文件
        设置 PASS_MAX_DAYS 90
```

### 3. 自定义规则

#### 添加新的检查规则：

1. 在规则库文件中添加新规则
2. 定义检查方法和预期结果
3. 提供修复建议

```yaml
custom_category:
  rules:
    - id: "custom_app_check"
      name: "自定义应用程序检查"
      description: "检查自定义应用程序配置"
      severity: "medium"
      check_method: "custom"
      custom_check: |
        # Python自定义检查逻辑
        import os
        config_file = "/opt/myapp/config.yaml"
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                content = f.read()
                return "debug: false" in content
        return False
      remediation: "修改应用程序配置文件，禁用调试模式"
```

#### 使用自定义检查脚本：

```bash
# 创建自定义检查脚本
cat > custom-check.py << 'EOF'
def check_custom_app(config):
    """自定义应用程序检查"""
    import os
    import yaml
    
    config_file = config.get("config_path", "/opt/myapp/config.yaml")
    if not os.path.exists(config_file):
        return {"status": "NON_COMPLIANT", "message": "配置文件不存在"}
    
    with open(config_file, 'r') as f:
        app_config = yaml.safe_load(f)
    
    if app_config.get("debug", True):
        return {"status": "NON_COMPLIANT", "message": "调试模式已启用"}
    else:
        return {"status": "COMPLIANT", "message": "配置符合要求"}
EOF

# 在规则中引用自定义脚本
```

## 集成到CI/CD

### 1. Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('配置合规性检查') {
            steps {
                script {
                    sh '''
                        python config-compliance-check-script.py \
                            --config compliance-config.yaml \
                            --rules compliance-rules-library.yaml \
                            --output ./compliance-reports \
                            --strict
                    '''
                    
                    // 检查合规性结果
                    def report = readJSON file: 'compliance-reports/latest-report.json'
                    if (report.overall_compliance != 'FULLY_COMPLIANT') {
                        // 生成修复计划
                        sh 'python config-compliance-check-script.py --remediation'
                        
                        // 可选：自动应用修复
                        if (params.AUTO_REMEDIATE) {
                            sh 'bash compliance-reports/latest-remediation.sh'
                        }
                        
                        error "配置合规性检查失败: ${report.overall_compliance}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'compliance-reports/**', fingerprint: true
        }
    }
}
```

### 2. GitLab CI

```yaml
compliance-check:
  stage: test
  script:
    - |
      python config-compliance-check-script.py \
        --config compliance-config.yaml \
        --rules compliance-rules-library.yaml \
        --output ./compliance-reports \
        --format json
      
      # 检查合规性
      COMPLIANCE_STATUS=$(jq -r '.overall_compliance' compliance-reports/*.json)
      if [ "$COMPLIANCE_STATUS" != "FULLY_COMPLIANT" ]; then
        echo "配置合规性检查失败: $COMPLIANCE_STATUS"
        
        # 生成修复建议
        python config-compliance-check-script.py --remediation
        
        # 可选：上传修复计划为工件
        cat compliance-reports/*-remediation.sh
        
        exit 1
      fi
  artifacts:
    paths:
      - compliance-reports/
    expire_in: 1 week
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH == "main"
```

### 3. GitHub Actions

```yaml
name: Configuration Compliance Check

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  compliance-check:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.9'
    
    - name: Install dependencies
      run: pip install -r requirements.txt
    
    - name: Run compliance check
      run: |
        python config-compliance-check-script.py \
          --config compliance-config.yaml \
          --rules compliance-rules-library.yaml \
          --output ./compliance-reports \
          --strict
    
    - name: Check compliance status
      run: |
        REPORT_FILE=$(find compliance-reports -name "*.json" | head -1)
        COMPLIANCE_STATUS=$(jq -r '.overall_compliance' "$REPORT_FILE")
        
        if [ "$COMPLIANCE_STATUS" != "FULLY_COMPLIANT" ]; then
          echo "::error::Configuration compliance check failed: $COMPLIANCE_STATUS"
          
          # 生成修复计划
          python config-compliance-check-script.py --remediation
          
          # 输出修复建议
          echo "## Remediation Plan"
          cat compliance-reports/*-remediation.sh
          
          exit 1
        fi
    
    - name: Upload compliance reports
      uses: actions/upload-artifact@v3
      with:
        name: compliance-reports
        path: compliance-reports/
```

## 扩展开发

### 1. 添加新的检查方法

在脚本中添加新的检查方法类：

```python
class NewCheckMethod:
    """新的检查方法实现"""
    
    @staticmethod
    def check(config):
        """执行检查"""
        try:
            # 实现检查逻辑
            result = perform_check(config)
            
            if result["success"]:
                return {
                    "status": "COMPLIANT",
                    "message": "检查通过",
                    "details": result["data"]
                }
            else:
                return {
                    "status": "NON_COMPLIANT",
                    "message": "检查失败",
                    "details": result["errors"]
                }
                
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"检查异常: {str(e)}",
                "details": {"error": str(e)}
            }
```

### 2. 集成外部合规标准

```python
class ExternalComplianceStandard:
    """外部合规标准集成"""
    
    def __init__(self, standard_name):
        self.standard_name = standard_name
        self.rules = self.load_external_rules()
    
    def load_external_rules(self):
        """从外部API或文件加载规则"""
        # 示例：从合规标准API加载
        import requests
        response = requests.get(
            f"https://compliance-api.example.com/standards/{self.standard_name}/rules"
        )
        return response.json()
    
    def evaluate_compliance(self, check_results):
        """根据外部标准评估合规性"""
        # 实现评估逻辑
        pass
```

### 3. 数据库存储和查询

```python
class ComplianceDatabase:
    """合规性检查结果数据库存储"""
    
    def __init__(self, db_url):
        self.db_url = db_url
        self.engine = create_engine(db_url)
    
    def save_check_result(self, result):
        """保存检查结果"""
        with self.engine.connect() as conn:
            conn.execute(
                "INSERT INTO compliance_results (check_id, status, timestamp, details) "
                "VALUES (:check_id, :status, :timestamp, :details)",
                {
                    "check_id": result["check_id"],
                    "status": result["status"],
                    "timestamp": datetime.now(),
                    "details": json.dumps(result["details"])
                }
            )
    
    def get_compliance_history(self, environment, days=30):
        """获取合规性历史记录"""
        query = """
            SELECT * FROM compliance_results
            WHERE environment = :environment
            AND timestamp >= :start_date
            ORDER BY timestamp DESC
        """
        # 执行查询
        pass
```

## 故障排除

### 1. 常见问题

#### Q: 某些检查命令需要sudo权限
**解决**: 配置sudo权限或使用特权用户运行脚本
```bash
# 使用sudo运行
sudo python config-compliance-check-script.py --config sudo-config.yaml

# 或者在配置中指定sudo命令
command: "sudo grep 'pattern' /etc/securefile"
```

#### Q: Windows和Linux检查方法不同
**解决**: 在规则中定义平台特定的检查方法
```yaml
rules:
  - id: "multi_platform_check"
    platforms: ["linux", "windows"]
    linux:
      command: "systemctl is-active service"
    windows:
      command: "sc query ServiceName"
```

#### Q: 检查结果不一致
**解决**: 启用调试模式查看详细输出
```bash
python config-compliance-check-script.py --verbose --debug
```

#### Q: 规则库文件过大
**解决**: 按类别拆分规则库文件
```bash
# 主规则库引用子规则库
python config-compliance-check-script.py \
  --rules os-rules.yaml,security-rules.yaml,network-rules.yaml
```

### 2. 调试模式

启用调试模式获取更多信息：

```bash
# 设置环境变量
export COMPLIANCE_DEBUG=1
export COMPLIANCE_LOG_LEVEL=DEBUG

# 运行脚本
python config-compliance-check-script.py --verbose --debug

# 查看详细日志
tail -f compliance-check-debug.log
```

### 3. 性能优化

对于大规模环境，优化检查性能：

```yaml
performance:
  parallel_checks: true
  max_workers: 10
  cache_results: true
  cache_ttl: 300  # 缓存5分钟
  
  # 跳过耗时较长的检查
  skip_slow_checks: false
  slow_check_timeout: 30
```

## 安全注意事项

### 1. 权限管理
- 脚本可能需要特权访问系统配置
- 使用最小权限原则
- 避免在配置文件中存储敏感信息

### 2. 数据安全
- 检查结果可能包含系统敏感信息
- 实现报告数据的脱敏处理
- 设置适当的报告访问控制

### 3. 网络安全
- 远程检查需要安全的网络连接
- 使用SSH隧道或VPN进行安全检查
- 实现请求认证和加密

## 版本历史

### v1.0.0 (2026-04-29)
- ✅ 初始版本发布
- ✅ 支持7个配置类别检查
- ✅ 包含25个预定义合规规则
- ✅ 多平台支持（Linux/Windows）
- ✅ 多格式报告生成
- ✅ 修复建议生成

### v0.9.0 (2026-04-28)
- 🔄 草案版本
- 🔄 基本框架实现
- 🔄 初步规则库定义

### 后续计划
- 🔄 集成更多合规标准（ISO 27001, PCI DSS等）
- 🔄 支持容器环境检查（Docker, Kubernetes）
- 🔄 实现实时合规性监控
- 🔄 添加机器学习异常检测
- 🔄 支持云环境配置检查（AWS, Azure, GCP）

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目负责人: qa-lead
- 项目: AI-Ready测试环境配置专项
- Sprint: Sprint 27+1
- 任务ID: task_1777438750224_y4l4dft4a
- 子任务: 子任务3 - 配置合规性检查脚本开发