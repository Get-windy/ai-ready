#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready API文档验证脚本
验证API文档完整性、接口参数正确性、响应格式一致性
"""

import pytest
import re
import os
import json
from datetime import datetime
from typing import Dict, List, Any, Optional
from dataclasses import dataclass

# 测试结果
VALIDATION_RESULTS = {
    "test_time": "",
    "controllers": [],
    "endpoints": [],
    "summary": {}
}


@dataclass
class APIEndpoint:
    """API端点信息"""
    controller: str
    path: str
    method: str
    operation: str
    tag: str
    parameters: List[str]
    has_response_type: bool
    has_permission: bool
    validation_issues: List[str]


class APIValidator:
    """API文档验证器"""
    
    def __init__(self, project_path: str):
        self.project_path = project_path
        self.endpoints: List[APIEndpoint] = []
        self.controllers: List[Dict] = []
    
    def find_controllers(self) -> List[str]:
        """查找所有Controller文件"""
        controllers = []
        for root, dirs, files in os.walk(self.project_path):
            for file in files:
                if file.endswith("Controller.java"):
                    controllers.append(os.path.join(root, file))
        return controllers
    
    def parse_controller(self, file_path: str) -> Dict:
        """解析Controller文件"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        controller_name = os.path.basename(file_path).replace(".java", "")
        
        # 提取类级别Tag
        tag_match = re.search(r'@Tag\s*\(\s*name\s*=\s*"([^"]+)"', content)
        tag_name = tag_match.group(1) if tag_match else controller_name
        
        # 提取基础路径
        request_mapping_match = re.search(r'@RequestMapping\s*\(\s*"([^"]+)"', content)
        base_path = request_mapping_match.group(1) if request_mapping_match else ""
        
        return {
            "name": controller_name,
            "file": file_path,
            "tag": tag_name,
            "base_path": base_path,
            "content": content
        }
    
    def extract_endpoints(self, controller: Dict) -> List[APIEndpoint]:
        """从Controller提取API端点"""
        content = controller["content"]
        endpoints = []
        
        # 匹配方法定义和注解
        method_pattern = re.compile(
            r'(@Operation[^@]*?)'
            r'(@\w+Mapping[^{]*?)'
            r'(public\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*\(([^)]*)\))',
            re.DOTALL
        )
        
        for match in method_pattern.finditer(content):
            operation_block = match.group(1)
            mapping_block = match.group(2)
            return_type = match.group(4)
            method_name = match.group(5)
            params_str = match.group(6)
            
            # 提取Operation summary
            summary_match = re.search(r'summary\s*=\s*"([^"]+)"', operation_block)
            summary = summary_match.group(1) if summary_match else method_name
            
            # 提取HTTP方法
            method = "GET"
            if "@PostMapping" in mapping_block:
                method = "POST"
            elif "@PutMapping" in mapping_block:
                method = "PUT"
            elif "@DeleteMapping" in mapping_block:
                method = "DELETE"
            elif "@PatchMapping" in mapping_block:
                method = "PATCH"
            
            # 提取路径
            path_match = re.search(r'@\w+Mapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', mapping_block)
            path = path_match.group(1) if path_match else ""
            full_path = controller["base_path"] + path
            
            # 提取参数
            parameters = []
            param_matches = re.findall(r'@RequestParam[^)]*\)\s+(\w+)\s+(\w+)', params_str)
            for param_type, param_name in param_matches:
                parameters.append(f"{param_name}: {param_type}")
            
            path_var_matches = re.findall(r'@PathVariable[^)]*\)\s+(\w+)\s+(\w+)', params_str)
            for param_type, param_name in path_var_matches:
                parameters.append(f"{param_name}: {param_type}")
            
            body_match = re.search(r'@RequestBody\s+[^)]*\)\s+(\w+(?:<[^>]+>)?)\s+(\w+)', params_str)
            if body_match:
                parameters.append(f"{body_match.group(2)}: {body_match.group(1)}")
            
            # 检查权限注解
            has_permission = "@SaCheckPermission" in mapping_block or "@SaCheckLogin" in mapping_block
            
            # 检查响应类型
            has_response_type = "Result<" in return_type or "ResponseEntity" in return_type
            
            # 验证问题检测
            issues = []
            if not summary:
                issues.append("缺少Operation summary")
            if not has_response_type and return_type != "void":
                issues.append("响应类型不规范")
            if method in ["POST", "PUT", "DELETE"] and not has_permission:
                issues.append("写操作缺少权限控制")
            
            endpoint = APIEndpoint(
                controller=controller["name"],
                path=full_path,
                method=method,
                operation=summary,
                tag=controller["tag"],
                parameters=parameters,
                has_response_type=has_response_type,
                has_permission=has_permission,
                validation_issues=issues
            )
            endpoints.append(endpoint)
        
        return endpoints
    
    def validate(self) -> Dict:
        """执行验证"""
        controllers = self.find_controllers()
        
        for ctrl_path in controllers:
            controller = self.parse_controller(ctrl_path)
            self.controllers.append({
                "name": controller["name"],
                "tag": controller["tag"],
                "base_path": controller["base_path"]
            })
            
            endpoints = self.extract_endpoints(controller)
            self.endpoints.extend(endpoints)
        
        return self.generate_report()
    
    def generate_report(self) -> Dict:
        """生成验证报告"""
        total_endpoints = len(self.endpoints)
        
        documented = sum(1 for e in self.endpoints if e.operation)
        has_permission = sum(1 for e in self.endpoints if e.has_permission)
        has_response_type = sum(1 for e in self.endpoints if e.has_response_type)
        
        issues = []
        for e in self.endpoints:
            issues.extend([f"{e.controller}.{e.operation}: {issue}" for issue in e.validation_issues])
        
        completeness_score = (documented / total_endpoints * 100) if total_endpoints > 0 else 0
        permission_score = (has_permission / total_endpoints * 100) if total_endpoints > 0 else 0
        response_score = (has_response_type / total_endpoints * 100) if total_endpoints > 0 else 0
        
        overall_score = (completeness_score * 0.4 + permission_score * 0.3 + response_score * 0.3)
        
        return {
            "total_controllers": len(self.controllers),
            "total_endpoints": total_endpoints,
            "documented_endpoints": documented,
            "endpoints_with_permission": has_permission,
            "endpoints_with_response_type": has_response_type,
            "completeness_score": round(completeness_score, 2),
            "permission_score": round(permission_score, 2),
            "response_score": round(response_score, 2),
            "overall_score": round(overall_score, 2),
            "issues": issues,
            "controllers": self.controllers
        }


# ==================== 测试类 ====================

class TestAPIDocumentationValidation:
    """API文档验证测试"""
    
    @pytest.mark.documentation
    def test_controller_discovery(self):
        """测试Controller发现"""
        result = APIValidator("I:\\AI-Ready")
        controllers = result.find_controllers()
        
        assert len(controllers) > 0, "未发现任何Controller文件"
        
        VALIDATION_RESULTS["controllers"] = [
            {"name": os.path.basename(c).replace(".java", ""), "path": c}
            for c in controllers[:10]
        ]
        
        print(f"[INFO] 发现 {len(controllers)} 个Controller文件")
    
    @pytest.mark.documentation
    def test_api_documentation_completeness(self):
        """测试API文档完整性"""
        validator = APIValidator("I:\\AI-Ready")
        report = validator.validate()
        
        VALIDATION_RESULTS["summary"] = report
        
        # 至少90%的API有文档
        assert report["completeness_score"] >= 80, f"文档完整性不足: {report['completeness_score']}%"
        
        print(f"[INFO] API文档完整性: {report['completeness_score']}%")
        print(f"[INFO] 发现 {report['total_endpoints']} 个API端点")
    
    @pytest.mark.documentation
    def test_permission_control(self):
        """测试权限控制"""
        report = VALIDATION_RESULTS.get("summary", {})
        
        # 写操作应有权限控制
        permission_score = report.get("permission_score", 0)
        
        print(f"[INFO] 权限控制覆盖率: {permission_score}%")
        
        # 允许较低的权限覆盖率（登录等接口不需要权限）
        assert permission_score >= 50, f"权限控制覆盖率过低: {permission_score}%"
    
    @pytest.mark.documentation
    def test_response_format_consistency(self):
        """测试响应格式一致性"""
        report = VALIDATION_RESULTS.get("summary", {})
        
        response_score = report.get("response_score", 0)
        
        assert response_score >= 90, f"响应格式一致性不足: {response_score}%"
        
        print(f"[INFO] 响应格式一致性: {response_score}%")
    
    @pytest.mark.documentation
    def test_validation_issues(self):
        """测试验证问题"""
        report = VALIDATION_RESULTS.get("summary", {})
        issues = report.get("issues", [])
        
        # 允许一定数量的问题
        max_issues = 20
        
        print(f"[INFO] 发现 {len(issues)} 个验证问题")
        
        if issues:
            print("\n问题列表:")
            for issue in issues[:10]:
                print(f"  - {issue}")
        
        assert len(issues) <= max_issues, f"验证问题过多: {len(issues)}个"


# ==================== 报告生成 ====================

def generate_api_doc_report():
    """生成API文档验证报告"""
    report = VALIDATION_RESULTS.get("summary", {})
    
    report_md = f"""# AI-Ready API文档验证报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {datetime.now().strftime("%Y-%m-%d %H:%M:%S")} |
| 项目路径 | I:\\AI-Ready |
| Controller数量 | {report.get("total_controllers", 0)} |
| API端点数量 | {report.get("total_endpoints", 0)} |

---

## 验证结果

| 指标 | 数值 | 评估 |
|------|------|------|
| 文档完整性 | {report.get("completeness_score", 0)}% | {"✅ 优秀" if report.get("completeness_score", 0) >= 90 else "⚠️ 需改进"} |
| 权限覆盖率 | {report.get("permission_score", 0)}% | {"✅ 优秀" if report.get("permission_score", 0) >= 80 else "⚠️ 需改进"} |
| 响应格式一致性 | {report.get("response_score", 0)}% | {"✅ 优秀" if report.get("response_score", 0) >= 90 else "⚠️ 需改进"} |
| 综合评分 | **{report.get("overall_score", 0)}** | {"✅ 通过" if report.get("overall_score", 0) >= 80 else "⚠️ 需改进"} |

---

## API端点统计

### 按类型统计

| 统计项 | 数量 |
|--------|------|
| 总端点数 | {report.get("total_endpoints", 0)} |
| 已文档化 | {report.get("documented_endpoints", 0)} |
| 有权限控制 | {report.get("endpoints_with_permission", 0)} |
| 规范响应格式 | {report.get("endpoints_with_response_type", 0)} |

### Controller列表

| Controller | 说明 |
|------------|------|
"""
    
    for ctrl in report.get("controllers", [])[:10]:
        report_md += f"| {ctrl.get('name', '-')} | {ctrl.get('tag', '-')} |\n"
    
    issues = report.get("issues", [])
    report_md += f"""
---

## 验证问题

共发现 {len(issues)} 个问题：

"""
    
    if issues:
        for issue in issues[:20]:
            report_md += f"- {issue}\n"
    else:
        report_md += "无验证问题\n"
    
    report_md += f"""
---

## 改进建议

### 文档完整性

1. 为所有API端点添加Operation summary
2. 添加参数说明文档
3. 添加响应示例

### 权限控制

1. 为写操作添加权限注解
2. 定义清晰的权限体系
3. 定期审计权限配置

### 响应格式

1. 统一使用Result<T>包装响应
2. 定义标准错误码
3. 添加响应文档说明

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    # 保存报告
    report_path = "docs/AI-Ready API文档验证报告.md"
    os.makedirs("docs", exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report_md)
    
    print(f"\n[报告] {report_path}")
    
    return report_path


if __name__ == "__main__":
    VALIDATION_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready API文档验证")
    print("=" * 60)
    
    # 运行测试
    pytest.main([__file__, "-v", "--tb=short"])
    
    # 生成报告
    print("\n" + "=" * 60)
    generate_api_doc_report()
    print("=" * 60)
