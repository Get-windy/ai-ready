#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 安全扫描套件
执行代码安全扫描、依赖安全扫描、配置安全检查、权限配置检查
"""

import os
import re
import json
import subprocess
from datetime import datetime
from typing import Dict, List, Any
from dataclasses import dataclass, field

# 扫描结果
SCAN_RESULTS = {
    "scan_time": "",
    "code_scan": {"issues": [], "count": 0},
    "dependency_scan": {"issues": [], "count": 0},
    "config_scan": {"issues": [], "count": 0},
    "permission_scan": {"issues": [], "count": 0},
    "summary": {}
}

# 安全规则
SECURITY_RULES = {
    "code": {
        "sql_injection": [r"executeQuery\s*\(\s*.*\+", r"createStatement\s*\(\s*\)"],
        "xss": [r"response\.write\s*\(\s*.*request", r"innerHTML\s*="],
        "hardcoded_secrets": [r"password\s*=\s*[\"'][^\"']+[\"']", r"api[_-]?key\s*=\s*[\"'][^\"']+[\"']"],
        "weak_crypto": [r"MD5\s*\(", r"DES\s*\(", r"SHA1\s*\("],
        "dangerous_functions": [r"Runtime\.exec", r"ProcessBuilder", r"eval\s*\("],
    },
    "config": {
        "debug_mode": [r"debug\s*=\s*true", r"DEBUG\s*=\s*true"],
        "weak_ssl": [r"TLSv1[^2]", r"SSLv3"],
        "cors_wildcard": [r"Access-Control-Allow-Origin\s*:\s*\*"],
        "exposed_endpoints": [r"actuator", r"swagger-ui", r"/health"],
    }
}

# 已知漏洞依赖
VULNERABLE_DEPENDENCIES = {
    "log4j": {"cve": "CVE-2021-44228", "severity": "Critical", "versions": "<2.17.0"},
    "spring-beans": {"cve": "CVE-2022-22965", "severity": "High", "versions": "5.3.0-5.3.17"},
    "jackson-databind": {"cve": "CVE-2022-42003", "severity": "High", "versions": "<2.14.0"},
}


@dataclass
class SecurityIssue:
    """安全问题"""
    category: str
    rule: str
    file: str
    line: int
    severity: str
    description: str
    recommendation: str


def scan_java_file(filepath: str) -> List[SecurityIssue]:
    """扫描Java文件"""
    issues = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            lines = content.split('\n')
            
            for line_num, line in enumerate(lines, 1):
                # SQL注入检查
                if re.search(r"executeQuery\s*\([^)]*\+", line):
                    issues.append(SecurityIssue(
                        category="code",
                        rule="sql_injection",
                        file=filepath,
                        line=line_num,
                        severity="High",
                        description="Potential SQL injection: String concatenation in query",
                        recommendation="Use parameterized queries"
                    ))
                
                # 硬编码密码检查
                if re.search(r"password\s*=\s*[\"'][^\"']{4,}[\"']", line, re.IGNORECASE):
                    issues.append(SecurityIssue(
                        category="code",
                        rule="hardcoded_secret",
                        file=filepath,
                        line=line_num,
                        severity="Medium",
                        description="Hardcoded password detected",
                        recommendation="Use environment variables or secret manager"
                    ))
                
                # 危险函数检查
                if re.search(r"Runtime\.exec|ProcessBuilder", line):
                    issues.append(SecurityIssue(
                        category="code",
                        rule="dangerous_function",
                        file=filepath,
                        line=line_num,
                        severity="High",
                        description="Command execution detected",
                        recommendation="Validate and sanitize all inputs"
                    ))
                
                # 弱加密检查
                if re.search(r"MD5|DES|SHA1", line) and not line.strip().startswith("//"):
                    issues.append(SecurityIssue(
                        category="code",
                        rule="weak_crypto",
                        file=filepath,
                        line=line_num,
                        severity="Medium",
                        description="Weak cryptographic algorithm",
                        recommendation="Use SHA-256 or stronger algorithms"
                    ))
    except Exception:
        pass
    return issues


def scan_config_file(filepath: str) -> List[SecurityIssue]:
    """扫描配置文件"""
    issues = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            lines = content.split('\n')
            
            for line_num, line in enumerate(lines, 1):
                # 调试模式检查
                if re.search(r"debug\s*[:=]\s*true", line, re.IGNORECASE):
                    issues.append(SecurityIssue(
                        category="config",
                        rule="debug_mode",
                        file=filepath,
                        line=line_num,
                        severity="Low",
                        description="Debug mode enabled",
                        recommendation="Disable debug mode in production"
                    ))
                
                # CORS通配符检查
                if re.search(r"allowedOrigins\s*[:=]\s*\*", line):
                    issues.append(SecurityIssue(
                        category="config",
                        rule="cors_wildcard",
                        file=filepath,
                        line=line_num,
                        severity="Medium",
                        description="CORS wildcard origin",
                        recommendation="Specify allowed origins explicitly"
                    ))
                
                # 硬编码凭证检查
                if re.search(r"(password|secret|key)\s*[:=]\s*[\"'][^\"']+[\"']", line, re.IGNORECASE):
                    if not any(x in line.lower() for x in ['placeholder', 'example', 'xxx']):
                        issues.append(SecurityIssue(
                            category="config",
                            rule="hardcoded_credential",
                            file=filepath,
                            line=line_num,
                            severity="High",
                            description="Hardcoded credential in config",
                            recommendation="Use environment variables"
                        ))
    except Exception:
        pass
    return issues


def scan_dependencies(project_dir: str) -> List[SecurityIssue]:
    """扫描依赖项"""
    issues = []
    pom_path = os.path.join(project_dir, "pom.xml")
    
    if os.path.exists(pom_path):
        try:
            with open(pom_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查已知漏洞依赖
            for dep, vuln in VULNERABLE_DEPENDENCIES.items():
                if dep.lower() in content.lower():
                    issues.append(SecurityIssue(
                        category="dependency",
                        rule="vulnerable_dependency",
                        file="pom.xml",
                        line=0,
                        severity=vuln["severity"],
                        description=f"Potential vulnerability: {dep} - {vuln['cve']}",
                        recommendation=f"Update {dep} to latest version"
                    ))
        except Exception:
            pass
    
    return issues


def scan_permissions(project_dir: str) -> List[SecurityIssue]:
    """扫描权限配置"""
    issues = []
    
    # 检查敏感文件权限
    sensitive_files = [
        ".env",
        "application-secret.yml",
        "application-secret.properties",
        "id_rsa",
        ".pem"
    ]
    
    for root, dirs, files in os.walk(project_dir):
        for file in files:
            if file in sensitive_files:
                filepath = os.path.join(root, file)
                issues.append(SecurityIssue(
                    category="permission",
                    rule="sensitive_file",
                    file=filepath,
                    line=0,
                    severity="High",
                    description=f"Sensitive file found: {file}",
                    recommendation="Remove from repository or encrypt"
                ))
    
    # 检查权限注解使用
    java_files = []
    for root, dirs, files in os.walk(project_dir):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))
    
    files_without_permission = 0
    for filepath in java_files[:50]:  # 检查前50个文件
        try:
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                if "@RestController" in content or "@Controller" in content:
                    if "@SaCheckPermission" not in content and "@PreAuthorize" not in content:
                        files_without_permission += 1
        except:
            pass
    
    if files_without_permission > 0:
        issues.append(SecurityIssue(
            category="permission",
            rule="missing_permission_annotation",
            file="multiple",
            line=0,
            severity="Medium",
            description=f"{files_without_permission} controller files may lack permission annotations",
            recommendation="Add permission annotations to all sensitive endpoints"
        ))
    
    return issues


def run_security_scan(project_dir: str) -> Dict[str, Any]:
    """运行安全扫描"""
    SCAN_RESULTS["scan_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 安全扫描")
    print("=" * 60)
    
    # 代码安全扫描
    print("\n[1/4] 代码安全扫描...")
    code_issues = []
    for root, dirs, files in os.walk(project_dir):
        # 排除目录
        dirs[:] = [d for d in dirs if d not in ['target', 'node_modules', '.git', '__pycache__']]
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                code_issues.extend(scan_java_file(filepath))
    
    SCAN_RESULTS["code_scan"]["issues"] = [
        {"category": i.category, "rule": i.rule, "file": i.file, "line": i.line, 
         "severity": i.severity, "description": i.description, "recommendation": i.recommendation}
        for i in code_issues[:50]  # 限制输出
    ]
    SCAN_RESULTS["code_scan"]["count"] = len(code_issues)
    print(f"  发现 {len(code_issues)} 个代码安全问题")
    
    # 依赖安全扫描
    print("\n[2/4] 依赖安全扫描...")
    dep_issues = scan_dependencies(project_dir)
    SCAN_RESULTS["dependency_scan"]["issues"] = [
        {"category": i.category, "rule": i.rule, "file": i.file, "line": i.line,
         "severity": i.severity, "description": i.description, "recommendation": i.recommendation}
        for i in dep_issues
    ]
    SCAN_RESULTS["dependency_scan"]["count"] = len(dep_issues)
    print(f"  发现 {len(dep_issues)} 个依赖安全问题")
    
    # 配置安全扫描
    print("\n[3/4] 配置安全扫描...")
    config_issues = []
    for root, dirs, files in os.walk(project_dir):
        dirs[:] = [d for d in dirs if d not in ['target', 'node_modules', '.git']]
        for file in files:
            if file.endswith(('.yml', '.yaml', '.properties', '.xml', '.json')):
                filepath = os.path.join(root, file)
                config_issues.extend(scan_config_file(filepath))
    
    SCAN_RESULTS["config_scan"]["issues"] = [
        {"category": i.category, "rule": i.rule, "file": i.file, "line": i.line,
         "severity": i.severity, "description": i.description, "recommendation": i.recommendation}
        for i in config_issues[:30]
    ]
    SCAN_RESULTS["config_scan"]["count"] = len(config_issues)
    print(f"  发现 {len(config_issues)} 个配置安全问题")
    
    # 权限配置扫描
    print("\n[4/4] 权限配置扫描...")
    perm_issues = scan_permissions(project_dir)
    SCAN_RESULTS["permission_scan"]["issues"] = [
        {"category": i.category, "rule": i.rule, "file": i.file, "line": i.line,
         "severity": i.severity, "description": i.description, "recommendation": i.recommendation}
        for i in perm_issues
    ]
    SCAN_RESULTS["permission_scan"]["count"] = len(perm_issues)
    print(f"  发现 {len(perm_issues)} 个权限配置问题")
    
    # 汇总
    total_issues = len(code_issues) + len(dep_issues) + len(config_issues) + len(perm_issues)
    critical = sum(1 for i in code_issues + dep_issues + config_issues + perm_issues if i.severity == "Critical")
    high = sum(1 for i in code_issues + dep_issues + config_issues + perm_issues if i.severity == "High")
    medium = sum(1 for i in code_issues + dep_issues + config_issues + perm_issues if i.severity == "Medium")
    low = sum(1 for i in code_issues + dep_issues + config_issues + perm_issues if i.severity == "Low")
    
    # 计算安全评分
    score = max(0, 100 - (critical * 20 + high * 10 + medium * 5 + low * 2))
    
    SCAN_RESULTS["summary"] = {
        "total_issues": total_issues,
        "critical": critical,
        "high": high,
        "medium": medium,
        "low": low,
        "security_score": score
    }
    
    print("\n" + "=" * 60)
    print("扫描完成")
    print("=" * 60)
    print(f"总问题数: {total_issues}")
    print(f"严重: {critical}, 高危: {high}, 中危: {medium}, 低危: {low}")
    print(f"安全评分: {score}/100")
    
    return SCAN_RESULTS


def generate_report(results: Dict[str, Any], output_path: str):
    """生成扫描报告"""
    report = f"""# AI-Ready 安全扫描报告

## 扫描概览

| 项目 | 数值 |
|------|------|
| 扫描时间 | {results["scan_time"]} |
| 总问题数 | {results["summary"]["total_issues"]} |
| 严重 | {results["summary"]["critical"]} 🔴 |
| 高危 | {results["summary"]["high"]} 🟠 |
| 中危 | {results["summary"]["medium"]} 🟡 |
| 低危 | {results["summary"]["low"]} 🟢 |
| 安全评分 | **{results["summary"]["security_score"]}/100** |

---

## 1. 代码安全扫描

发现 **{results["code_scan"]["count"]}** 个代码安全问题

"""
    
    if results["code_scan"]["issues"]:
        report += "| 严重性 | 文件 | 问题描述 | 建议 |\n"
        report += "|--------|------|----------|------|\n"
        for issue in results["code_scan"]["issues"][:20]:
            report += f"| {issue['severity']} | {issue['file'].split('/')[-1]} | {issue['description']} | {issue['recommendation']} |\n"
    else:
        report += "✅ 未发现代码安全问题\n"
    
    report += f"""
---

## 2. 依赖安全扫描

发现 **{results["dependency_scan"]["count"]}** 个依赖安全问题

"""
    
    if results["dependency_scan"]["issues"]:
        report += "| 严重性 | 依赖 | CVE | 建议 |\n"
        report += "|--------|------|-----|------|\n"
        for issue in results["dependency_scan"]["issues"]:
            report += f"| {issue['severity']} | {issue['file']} | {issue['description']} | {issue['recommendation']} |\n"
    else:
        report += "✅ 未发现依赖安全问题\n"
    
    report += f"""
---

## 3. 配置安全扫描

发现 **{results["config_scan"]["count"]}** 个配置安全问题

"""
    
    if results["config_scan"]["issues"]:
        report += "| 严重性 | 文件 | 问题描述 | 建议 |\n"
        report += "|--------|------|----------|------|\n"
        for issue in results["config_scan"]["issues"][:15]:
            report += f"| {issue['severity']} | {issue['file'].split('/')[-1]} | {issue['description']} | {issue['recommendation']} |\n"
    else:
        report += "✅ 未发现配置安全问题\n"
    
    report += f"""
---

## 4. 权限配置扫描

发现 **{results["permission_scan"]["count"]}** 个权限配置问题

"""
    
    if results["permission_scan"]["issues"]:
        report += "| 严重性 | 问题描述 | 建议 |\n"
        report += "|--------|----------|------|\n"
        for issue in results["permission_scan"]["issues"]:
            report += f"| {issue['severity']} | {issue['description']} | {issue['recommendation']} |\n"
    else:
        report += "✅ 未发现权限配置问题\n"
    
    report += f"""
---

## 安全评估

### 风险等级分布

| 等级 | 数量 | 影响 |
|------|------|------|
| 严重(Critical) | {results["summary"]["critical"]} | 需立即修复 |
| 高危(High) | {results["summary"]["high"]} | 需优先修复 |
| 中危(Medium) | {results["summary"]["medium"]} | 应计划修复 |
| 低危(Low) | {results["summary"]["low"]} | 建议修复 |

### 安全评分说明

- 90-100分: 安全状况良好
- 70-89分: 存在一定风险
- 50-69分: 风险较高
- 0-49分: 严重安全风险

---

## 改进建议

"""
    
    if results["summary"]["critical"] > 0:
        report += "### 🔴 立即行动\n\n"
        report += "- 修复所有严重级别漏洞\n"
        report += "- 更新存在CVE漏洞的依赖\n"
        report += "- 移除硬编码凭证\n\n"
    
    if results["summary"]["high"] > 0:
        report += "### 🟠 优先修复\n\n"
        report += "- 修复高危代码安全问题\n"
        report += "- 加强输入验证\n"
        report += "- 完善权限控制\n\n"
    
    if results["summary"]["medium"] > 0:
        report += "### 🟡 计划修复\n\n"
        report += "- 更新弱加密算法\n"
        report += "- 关闭生产环境调试模式\n"
        report += "- 规范配置管理\n\n"
    
    report += f"""
---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    # 保存报告
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n报告已保存: {output_path}")
    
    return report


if __name__ == "__main__":
    project_dir = r"I:\AI-Ready"
    
    results = run_security_scan(project_dir)
    
    report_path = os.path.join(project_dir, "docs", "AI-Ready安全扫描报告_20260403.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    generate_report(results, report_path)
    
    # 保存JSON结果
    json_path = os.path.join(project_dir, "docs", "security_scan_results.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已保存: {json_path}")
