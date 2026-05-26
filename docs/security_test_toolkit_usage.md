# 安全测试工具包使用说明

## 概述

本工具包用于对批次序列号管理模块进行自动化安全测试。支持SQL注入、XSS、路径遍历、安全头等多种安全测试。

## 快速开始

### 1. 安装依赖

```bash
# 确保已安装Python 3.8+
python --version

# 安装所需库
pip install requests pyyaml
```

### 2. 配置测试环境

编辑 `security_test_config.yaml` 文件，配置以下内容：

- `api_endpoints.base_url`: API基础地址
- `test_users`: 测试用户账号
- `test_endpoints`: 要测试的API端点

### 3. 运行安全测试

#### 方式一：运行完整测试套件

```bash
# 进入脚本目录
cd I:\AI-Ready\scripts

# 运行完整测试
python security_test_toolkit.py
```

#### 方式二：使用自定义配置

```python
from security_test_toolkit import SecurityTestToolkit

# 创建测试工具实例
toolkit = SecurityTestToolkit("custom_config.yaml")

# 定义要测试的端点
endpoints = [
    {
        "path": "/api/batch",
        "method": "GET",
        "params": {"id": "1"}
    },
    {
        "path": "/api/batch/create",
        "method": "POST",
        "params": {"batch_number": "TEST001"}
    }
]

# 运行测试
report = toolkit.run_comprehensive_test(endpoints)

# 生成报告
toolkit.generate_report("security_report.json")
```

#### 方式三：运行单个测试

```python
from security_test_toolkit import SecurityTestToolkit

toolkit = SecurityTestToolkit()

# SQL注入测试
sql_result = toolkit.test_sql_injection(
    endpoint="/api/batch",
    method="GET",
    params={"id": "1"}
)

# XSS测试
xss_result = toolkit.test_xss(
    endpoint="/api/batch/create",
    method="POST",
    params={"name": "test"}
)

# 安全头测试
headers_result = toolkit.test_security_headers("/api/batch")

# 生成报告
report_json = toolkit.generate_report()
```

## 测试类型说明

### 1. SQL注入测试
检测API端点是否存在SQL注入漏洞。

**测试方法**：
- 向参数注入SQL payload
- 分析响应中是否包含数据库错误信息
- 检查响应时间延迟（时间盲注）

**检测的payload类型**：
- 布尔型盲注: `' OR '1'='1`
- 联合查询注入: `' UNION SELECT NULL, NULL--`
- 时间盲注: `' OR SLEEP(5)--`
- 报错注入: `' AND 1=CONVERT(int, (SELECT @@version))--`

### 2. XSS跨站脚本测试
检测反射型XSS漏洞。

**测试方法**：
- 注入XSS payload到参数
- 检查payload是否被反射回响应
- 验证HTML编码和过滤机制

**检测的payload类型**：
- 脚本标签: `<script>alert('XSS')</script>`
- 事件处理器: `<img src=x onerror=alert('XSS')>`
- JavaScript协议: `javascript:alert('XSS')`
- SVG注入: `<svg/onload=alert('XSS')>`

### 3. 路径遍历测试
检测文件路径遍历漏洞。

**测试方法**：
- 尝试访问系统文件
- 检查是否返回文件内容
- 验证路径过滤机制

**检测的payload类型**：
- Unix路径: `../../../etc/passwd`
- Windows路径: `..\..\..\windows\system32\config\SAM`
- 配置文件: `/etc/nginx/nginx.conf`

### 4. 安全头测试
验证HTTP安全头配置。

**检查的安全头**：
- `X-Frame-Options`: 防止点击劫持
- `X-Content-Type-Options`: 防止MIME类型混淆
- `X-XSS-Protection`: XSS保护
- `Strict-Transport-Security`: 强制HTTPS
- `Content-Security-Policy`: 内容安全策略
- `Referrer-Policy`: 引用者策略

## 配置说明

### 主要配置项

```yaml
api_endpoints:
  base_url: "http://localhost:8080"  # API基础地址
  
test_users:
  admin:
    username: "admin"
    password: "Admin@123"
    role: "administrator"

test_data:
  sql_injection_payloads:  # SQL注入测试payload
    - "' OR '1'='1"
    - "' UNION SELECT NULL, NULL--"
  
  xss_payloads:  # XSS测试payload
    - "<script>alert('XSS')</script>"
    - "<img src=x onerror=alert('XSS')>"

security_headers:
  expected_values:  # 期望的安全头值
    X-Frame-Options: "DENY"
    X-Content-Type-Options: "nosniff"
```

### 测试端点配置

```yaml
test_endpoints:
  batch_endpoints:
    - path: "/api/batch"
      method: "GET"
      params:
        id: "1"
        name: "test"
      requires_auth: true
      role_required: ["administrator", "batch_manager"]
    
    - path: "/api/batch/create"
      method: "POST"
      params:
        batch_number: "BATCH001"
        product_id: "123"
      requires_auth: true
      role_required: ["administrator", "batch_manager"]
```

## 测试报告

### 报告格式

工具包生成两种格式的报告：

1. **JSON报告** (`security_report.json`):
   - 结构化数据
   - 适合自动化处理
   - 包含所有测试详情

2. **Markdown报告** (`security_report.md`):
   - 人类可读格式
   - 包含执行摘要
   - 详细测试结果
   - 安全建议

### 报告内容

报告包含以下部分：

1. **执行摘要**
   - 测试统计
   - 安全评分
   - 总体风险评估

2. **测试结果详情**
   - 每个测试的详细结果
   - 发现的漏洞
   - 响应信息

3. **安全建议**
   - 按严重程度分类
   - 修复建议
   - OWASP参考

4. **修复计划**
   - 优先级排序
   - 时间估计
   - 资源需求

### 示例报告片段

```json
{
  "report_metadata": {
    "generated_at": "2024-05-05T14:40:00",
    "total_tests": 15,
    "vulnerable_tests": 2,
    "safe_tests": 13,
    "security_score": 86.67
  },
  "test_results": [
    {
      "test_name": "SQL注入测试",
      "endpoint": "/api/batch",
      "vulnerable": 1,
      "safe": 4,
      "details": [...]
    }
  ],
  "recommendations": [
    {
      "severity": "HIGH",
      "test": "SQL注入测试",
      "recommendation": "实施参数化查询",
      "reference": "OWASP A03: Injection"
    }
  ]
}
```

## 高级用法

### 1. 自定义测试场景

```python
from security_test_toolkit import SecurityTestToolkit

# 创建自定义配置
custom_config = {
    "api_endpoints": {
        "base_url": "https://api.example.com"
    },
    "test_data": {
        "sql_injection_payloads": [
            # 添加自定义payload
            "' OR EXISTS(SELECT * FROM users)--",
            "' UNION SELECT column_name FROM information_schema.columns--"
        ]
    }
}

# 保存自定义配置
import yaml
with open("custom_test_config.yaml", "w") as f:
    yaml.dump(custom_config, f)

# 使用自定义配置运行测试
toolkit = SecurityTestToolkit("custom_test_config.yaml")
```

### 2. 集成到CI/CD流水线

```yaml
# .gitlab-ci.yml 示例
stages:
  - test
  - security

security_test:
  stage: security
  script:
    - python security_test_toolkit.py
  artifacts:
    paths:
      - security_report.json
      - security_report.md
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
```

```yaml
# Jenkinsfile 示例
pipeline {
  agent any
  stages {
    stage('Security Test') {
      steps {
        script {
          sh 'python security_test_toolkit.py'
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'security_report.*'
        }
      }
    }
  }
}
```

### 3. 扩展测试功能

```python
from security_test_toolkit import SecurityTestToolkit

class ExtendedSecurityToolkit(SecurityTestToolkit):
    """扩展的安全测试工具包"""
    
    def test_jwt_security(self, endpoint: str, token: str) -> Dict:
        """JWT安全测试"""
        # 实现JWT安全测试逻辑
        pass
    
    def test_rate_limiting(self, endpoint: str, requests_per_minute: int = 100) -> Dict:
        """速率限制测试"""
        # 实现速率限制测试逻辑
        pass
    
    def test_api_fuzzing(self, endpoint: str, fuzz_patterns: List[str]) -> Dict:
        """API模糊测试"""
        # 实现模糊测试逻辑
        pass

# 使用扩展工具包
extended_toolkit = ExtendedSecurityToolkit()
```

## 故障排除

### 常见问题

1. **连接超时**
   ```
   错误: Connection timeout
   解决方案: 检查base_url配置，确保API服务正在运行
   ```

2. **认证失败**
   ```
   错误: 401 Unauthorized
   解决方案: 检查test_users配置，确保用户名密码正确
   ```

3. **配置文件错误**
   ```
   错误: YAML parsing error
   解决方案: 检查YAML文件格式，确保缩进正确
   ```

4. **依赖缺失**
   ```
   错误: ModuleNotFoundError: No module named 'requests'
   解决方案: 运行 pip install requests pyyaml
   ```

### 调试模式

启用调试模式获取详细日志：

```python
import logging
logging.basicConfig(level=logging.DEBUG)

toolkit = SecurityTestToolkit()
```

### 模拟模式

在没有实际API环境时使用模拟模式：

```yaml
# 在配置文件中启用模拟模式
execution:
  mock_mode: true
```

## 最佳实践

### 1. 测试环境准备
- 使用独立的测试环境
- 准备测试数据
- 配置适当的权限

### 2. 测试执行
- 先运行基本测试
- 逐步增加测试复杂度
- 记录所有测试结果

### 3. 结果分析
- 优先处理高风险漏洞
- 验证漏洞真实性
- 制定修复计划

### 4. 持续改进
- 定期更新测试payload
- 学习新的攻击技术
- 优化测试策略

## 安全注意事项

### 测试环境要求
1. **仅在生产环境测试**：获得明确授权
2. **使用测试数据**：避免影响真实数据
3. **控制测试范围**：只测试授权范围
4. **遵守法律法规**：遵守相关网络安全法

### 数据保护
1. **不存储敏感信息**：测试后清理数据
2. **加密测试报告**：保护测试结果
3. **限制访问权限**：只允许授权人员访问

## 支持与反馈

如有问题或建议，请：

1. 检查日志文件
2. 查阅本文档
3. 联系安全团队

---

**版本**: 1.0.0  
**最后更新**: 2024-05-05  
**维护者**: 安全测试团队