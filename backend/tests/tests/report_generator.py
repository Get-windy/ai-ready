#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试报告生成器
支持HTML、JSON、JUnit XML格式报告
"""

import json
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional
import html


class TestReportGenerator:
    """测试报告生成器"""
    
    def __init__(self, output_dir: str = "reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
    def generate_html_report(self, test_results: Dict, output_file: Optional[str] = None):
        """生成HTML报告"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = self.output_dir / f"test_report_{timestamp}.html"
        else:
            output_file = self.output_dir / output_file
        
        # 计算统计信息
        total = test_results.get("total", 0)
        passed = test_results.get("passed", 0)
        failed = test_results.get("failed", 0)
        skipped = test_results.get("skipped", 0)
        error = test_results.get("error", 0)
        duration = test_results.get("duration", 0)
        
        pass_rate = (passed / total * 100) if total > 0 else 0
        
        # 生成HTML
        html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>测试报告 - {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: #f5f5f5;
            padding: 20px;
            line-height: 1.6;
        }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        .header {{ 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }}
        .header h1 {{ font-size: 28px; margin-bottom: 10px; }}
        .header p {{ opacity: 0.9; }}
        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }}
        .card {{
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .card h3 {{ font-size: 14px; color: #666; margin-bottom: 10px; text-transform: uppercase; }}
        .card .value {{ font-size: 36px; font-weight: bold; color: #333; }}
        .card.passed .value {{ color: #28a745; }}
        .card.failed .value {{ color: #dc3545; }}
        .card.skipped .value {{ color: #ffc107; }}
        .card.error .value {{ color: #fd7e14; }}
        .progress-bar {{
            width: 100%;
            height: 20px;
            background: #e9ecef;
            border-radius: 10px;
            overflow: hidden;
            margin-top: 15px;
        }}
        .progress-fill {{
            height: 100%;
            background: linear-gradient(90deg, #28a745, #20c997);
            border-radius: 10px;
            transition: width 0.3s ease;
        }}
        .details {{
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .details h2 {{ margin-bottom: 20px; color: #333; }}
        table {{
            width: 100%;
            border-collapse: collapse;
        }}
        th, td {{
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #dee2e6;
        }}
        th {{
            background: #f8f9fa;
            font-weight: 600;
            color: #495057;
        }}
        .status {{
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
        }}
        .status.passed {{ background: #d4edda; color: #155724; }}
        .status.failed {{ background: #f8d7da; color: #721c24; }}
        .status.skipped {{ background: #fff3cd; color: #856404; }}
        .status.error {{ background: #ffe0b2; color: #e65100; }}
        .footer {{
            text-align: center;
            padding: 20px;
            color: #666;
            margin-top: 30px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🧪 测试执行报告</h1>
            <p>生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}</p>
        </div>
        
        <div class="summary">
            <div class="card">
                <h3>总测试数</h3>
                <div class="value">{total}</div>
            </div>
            <div class="card passed">
                <h3>通过</h3>
                <div class="value">{passed}</div>
            </div>
            <div class="card failed">
                <h3>失败</h3>
                <div class="value">{failed}</div>
            </div>
            <div class="card skipped">
                <h3>跳过</h3>
                <div class="value">{skipped}</div>
            </div>
            <div class="card error">
                <h3>错误</h3>
                <div class="value">{error}</div>
            </div>
            <div class="card">
                <h3>执行时间</h3>
                <div class="value">{duration:.2f}s</div>
            </div>
        </div>
        
        <div class="card">
            <h3>通过率</h3>
            <div class="value" style="font-size: 48px;">{pass_rate:.1f}%</div>
            <div class="progress-bar">
                <div class="progress-fill" style="width: {pass_rate}%"></div>
            </div>
        </div>
        
        <div class="details" style="margin-top: 30px;">
            <h2>测试详情</h2>
            <table>
                <thead>
                    <tr>
                        <th>测试名称</th>
                        <th>状态</th>
                        <th>耗时</th>
                        <th>标记</th>
                    </tr>
                </thead>
                <tbody>
"""
        
        # 添加测试详情行
        tests = test_results.get("tests", [])
        for test in tests:
            name = html.escape(test.get("name", ""))
            status = test.get("status", "unknown")
            duration_test = test.get("duration", 0)
            markers = ", ".join(test.get("markers", []))
            
            html_content += f"""
                    <tr>
                        <td>{name}</td>
                        <td><span class="status {status}">{status.upper()}</span></td>
                        <td>{duration_test:.3f}s</td>
                        <td>{markers}</td>
                    </tr>
"""
        
        html_content += """
                </tbody>
            </table>
        </div>
        
        <div class="footer">
            <p>AI-Ready 自动化测试框架 | Generated by Test Report Generator</p>
        </div>
    </div>
</body>
</html>
"""
        
        # 写入文件
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(html_content)
        
        print(f"HTML报告已生成: {output_file}")
        return output_file
    
    def generate_junit_xml(self, test_results: Dict, output_file: Optional[str] = None):
        """生成JUnit XML报告"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = self.output_dir / f"junit_report_{timestamp}.xml"
        else:
            output_file = self.output_dir / output_file
        
        # 创建XML根元素
        testsuites = ET.Element("testsuites")
        testsuite = ET.SubElement(testsuites, "testsuite")
        testsuite.set("name", "AI-Ready Test Suite")
        testsuite.set("tests", str(test_results.get("total", 0)))
        testsuite.set("failures", str(test_results.get("failed", 0)))
        testsuite.set("errors", str(test_results.get("error", 0)))
        testsuite.set("skipped", str(test_results.get("skipped", 0)))
        testsuite.set("time", str(test_results.get("duration", 0)))
        
        # 添加测试用例
        for test in test_results.get("tests", []):
            testcase = ET.SubElement(testsuite, "testcase")
            testcase.set("name", test.get("name", ""))
            testcase.set("time", str(test.get("duration", 0)))
            testcase.set("classname", test.get("classname", ""))
            
            status = test.get("status", "")
            if status == "failed":
                failure = ET.SubElement(testcase, "failure")
                failure.set("message", test.get("error_message", "Test failed"))
                failure.text = test.get("error_trace", "")
            elif status == "skipped":
                ET.SubElement(testcase, "skipped")
            elif status == "error":
                error = ET.SubElement(testcase, "error")
                error.set("message", test.get("error_message", "Test error"))
                error.text = test.get("error_trace", "")
        
        # 写入文件
        tree = ET.ElementTree(testsuites)
        tree.write(output_file, encoding="utf-8", xml_declaration=True)
        
        print(f"JUnit XML报告已生成: {output_file}")
        return output_file
    
    def generate_json_report(self, test_results: Dict, output_file: Optional[str] = None):
        """生成JSON报告"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = self.output_dir / f"test_report_{timestamp}.json"
        else:
            output_file = self.output_dir / output_file
        
        # 添加元数据
        report = {
            "metadata": {
                "generated_at": datetime.now().isoformat(),
                "framework": "pytest",
                "version": "1.0.0"
            },
            "summary": {
                "total": test_results.get("total", 0),
                "passed": test_results.get("passed", 0),
                "failed": test_results.get("failed", 0),
                "skipped": test_results.get("skipped", 0),
                "error": test_results.get("error", 0),
                "duration": test_results.get("duration", 0),
                "pass_rate": (test_results.get("passed", 0) / test_results.get("total", 1) * 100)
            },
            "tests": test_results.get("tests", [])
        }
        
        # 写入文件
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"JSON报告已生成: {output_file}")
        return output_file
    
    def merge_reports(self, report_files: List[str], output_file: str):
        """合并多个测试报告"""
        merged = {
            "total": 0,
            "passed": 0,
            "failed": 0,
            "skipped": 0,
            "error": 0,
            "duration": 0,
            "tests": []
        }
        
        for report_file in report_files:
            with open(report_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            
            merged["total"] += data.get("total", 0)
            merged["passed"] += data.get("passed", 0)
            merged["failed"] += data.get("failed", 0)
            merged["skipped"] += data.get("skipped", 0)
            merged["error"] += data.get("error", 0)
            merged["duration"] += data.get("duration", 0)
            merged["tests"].extend(data.get("tests", []))
        
        # 生成合并后的报告
        return self.generate_html_report(merged, output_file)


if __name__ == "__main__":
    # 示例用法
    generator = TestReportGenerator()
    
    # 示例测试数据
    sample_results = {
        "total": 100,
        "passed": 95,
        "failed": 3,
        "skipped": 1,
        "error": 1,
        "duration": 120.5,
        "tests": [
            {
                "name": "test_user_login",
                "status": "passed",
                "duration": 0.5,
                "markers": ["api", "smoke"],
                "classname": "tests.api.test_auth"
            },
            {
                "name": "test_data_export",
                "status": "failed",
                "duration": 2.0,
                "markers": ["integration"],
                "classname": "tests.integration.test_export",
                "error_message": "Assertion failed",
                "error_trace": "Traceback..."
            }
        ]
    }
    
    generator.generate_html_report(sample_results)
    generator.generate_json_report(sample_results)
    generator.generate_junit_xml(sample_results)
