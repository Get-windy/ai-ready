# 批次管理模块安全测试自动化脚本

## 目录结构

```
security-test-automation-scripts/
├── README.md                          # 本文档
├── scripts/                           # 主要脚本目录
│   ├── sast/                          # 静态应用安全测试脚本
│   │   ├── run_sonarqube_scan.sh      # SonarQube扫描脚本
│   │   ├── run_checkmarx_scan.sh      # Checkmarx扫描脚本
│   │   └── analyze_sast_results.py    # SAST结果分析脚本
│   ├── dast/                          # 动态应用安全测试脚本
│   │   ├── run_zap_scan.sh            # OWASP ZAP扫描脚本
│   │   ├── run_burp_scan.sh           # Burp Suite扫描脚本
│   │   └── analyze_dast_results.py    # DAST结果分析脚本
│   ├── sca/                           # 软件成分分析脚本
│   │   ├── run_dependency_check.sh    # Dependency-Check扫描脚本
│   │   ├── run_snyk_scan.sh           # Snyk扫描脚本
│   │   └── analyze_sca_results.py     # SCA结果分析脚本
│   └── utils/                         # 工具脚本
│       ├── generate_test_data.py      # 测试数据生成脚本
│       ├── security_report_generator.py # 安全报告生成脚本
│       └── notification_sender.py     # 通知发送脚本
├── config/                            # 配置文件
│   ├── zap_config.yaml               # OWASP ZAP配置
│   ├── sonarqube_config.yaml         # SonarQube配置
│   ├── dependency_check_config.yaml  # Dependency-Check配置
│   └── security_gates.yaml           # 安全门禁配置
├── templates/                         # 模板文件
│   ├── security_test_case_template.yaml # 安全测试用例模板
│   ├── vulnerability_report_template.md  # 漏洞报告模板
│   └── security_metrics_template.yaml   # 安全度量模板
├── data/                             # 测试数据
│   ├── attack_payloads/              # 攻击payload数据
│   │   ├── sql_injection_payloads.txt
│   │   ├── xss_payloads.txt
│   │   ├── path_traversal_payloads.txt
│   │   └── command_injection_payloads.txt
│   └── test_scenarios/               # 测试场景数据
│       ├── batch_creation_scenarios.yaml
│       ├── state_transition_scenarios.yaml
│       └── query_scenarios.yaml
├── docker/                           # Docker配置
│   ├── docker-compose-security.yml   # 安全测试环境Docker配置
│   ├── Dockerfile.zap                # OWASP ZAP Docker镜像
│   └── Dockerfile.sonarqube          # SonarQube Docker镜像
└── ci-cd/                            # CI/CD集成配置
    ├── jenkins-pipeline-security.groovy
    ├── gitlab-ci-security.yml
    └── github-actions-security.yaml
```

## 快速开始

### 1. 环境准备
```bash
# 安装Python依赖
pip install -r requirements.txt

# 安装安全工具（可选）
./scripts/utils/install_security_tools.sh
```

### 2. 运行安全测试
```bash
# 运行SAST扫描
./scripts/sast/run_sonarqube_scan.sh

# 运行SCA扫描
./scripts/sca/run_dependency_check.sh

# 运行DAST扫描（需要先启动应用）
./scripts/dast/run_zap_scan.sh http://localhost:8080
```

### 3. 生成报告
```bash
# 生成安全测试报告
python scripts/utils/security_report_generator.py \
  --sast-reports reports/sast/ \
  --sca-reports reports/sca/ \
  --dast-reports reports/dast/ \
  --output reports/security_summary.html
```

## 脚本说明

### SAST脚本
- **run_sonarqube_scan.sh**: 运行SonarQube代码扫描
- **run_checkmarx_scan.sh**: 运行Checkmarx代码扫描
- **analyze_sast_results.py**: 分析SAST扫描结果，生成统计报告

### DAST脚本
- **run_zap_scan.sh**: 运行OWASP ZAP安全扫描
- **run_burp_scan.sh**: 运行Burp Suite安全扫描
- **analyze_dast_results.py**: 分析DAST扫描结果，评估风险等级

### SCA脚本
- **run_dependency_check.sh**: 运行Dependency-Check依赖扫描
- **run_snyk_scan.sh**: 运行Snyk安全扫描
- **analyze_sca_results.py**: 分析SCA扫描结果，识别已知漏洞

### 工具脚本
- **generate_test_data.py**: 生成安全测试数据
- **security_report_generator.py**: 生成统一安全报告
- **notification_sender.py**: 发送安全通知（邮件、Slack等）

## 配置说明

### OWASP ZAP配置 (config/zap_config.yaml)
```yaml
zap:
  proxy:
    host: localhost
    port: 8080
  scanner:
    policy: "Default Policy"
    strength: "MEDIUM"
    max_rule_duration: 10
  spider:
    max_depth: 5
    max_children: 100
  context:
    name: "batch-management-context"
    login_url: "/api/auth/login"
    logout_url: "/api/auth/logout"
```

### SonarQube配置 (config/sonarqube_config.yaml)
```yaml
sonar:
  host: http://localhost:9000
  token: ${SONAR_TOKEN}
  project:
    key: "erp-batch-management"
    name: "ERP批次管理模块"
    version: "1.0.0"
  analysis:
    sources: "src/main/java"
    exclusions: "**/*Test.java,**/test/**"
  quality_gate:
    wait: true
    timeout_minutes: 5
```

### 安全门禁配置 (config/security_gates.yaml)
```yaml
gates:
  sast:
    max_critical: 0
    max_high: 0
    max_medium: 5
    max_low: 10
  sca:
    max_critical: 0
    max_high: 1
    max_medium: 3
    max_low: 5
  dast:
    max_high: 0
    max_medium: 2
    max_low: 5
```

## CI/CD集成

### Jenkins流水线示例
```groovy
pipeline {
    agent any
    stages {
        stage('Security Test') {
            steps {
                script {
                    // SAST扫描
                    sh './scripts/sast/run_sonarqube_scan.sh'
                    
                    // SCA扫描
                    sh './scripts/sca/run_dependency_check.sh'
                    
                    // DAST扫描（仅在生产环境）
                    if (env.BRANCH_NAME == 'main') {
                        sh './scripts/dast/run_zap_scan.sh ${STAGING_URL}'
                    }
                }
            }
        }
    }
}
```

### GitLab CI示例
```yaml
security_test:
  stage: test
  script:
    - python -m pip install -r requirements.txt
    - ./scripts/sast/run_sonarqube_scan.sh
    - ./scripts/sca/run_dependency_check.sh
  artifacts:
    paths:
      - reports/
    expire_in: 1 week
```

## 测试数据

### 攻击Payloads
攻击payload数据存储在 `data/attack_payloads/` 目录中，包括：
- SQL注入payloads
- XSS攻击payloads
- 路径遍历payloads
- 命令注入payloads

### 测试场景
业务测试场景存储在 `data/test_scenarios/` 目录中，包括：
- 批次创建场景
- 状态流转场景
- 查询场景
- 关联服务场景

## 贡献指南

### 添加新脚本
1. 在适当的目录下创建新脚本
2. 更新README.md中的文档
3. 添加单元测试（如果适用）
4. 更新requirements.txt（如果添加新依赖）

### 报告问题
1. 在项目issue跟踪器中创建issue
2. 提供详细的复现步骤
3. 包含相关的日志和配置信息

## 许可证
本项目使用MIT许可证。详见LICENSE文件。

## 维护者
- test-agent-1 (安全测试方案设计)
- test-agent-2 (安全测试执行)

## 更新日志
- **2026-05-01**: 初始版本创建，包含基础脚本结构
- **2026-05-02**: 计划添加Docker支持和CI/CD集成