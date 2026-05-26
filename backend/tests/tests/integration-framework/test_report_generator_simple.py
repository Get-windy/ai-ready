#!/usr/bin/env python3
"""
测试报告生成器（简化版）

生成标准化的测试报告，支持多种格式输出
"""

import json
import csv
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any

class TestReportGeneratorSimple:
    """测试报告生成器（简化版）"""
    
    def __init__(self, output_dir: str = "./reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def generate_report(self, results: List[Any], metadata: Dict[str, Any] = None) -> Dict[str, Path]:
        """生成测试报告"""
        if metadata is None:
            metadata = {}
        
        # 转换结果为字典
        results_data = []
        for result in results:
            if hasattr(result, 'to_dict'):
                results_data.append(result.to_dict())
            elif isinstance(result, dict):
                results_data.append(result)
            else:
                # 简单转换
                results_data.append({'data': str(result)})
        
        # 准备报告数据
        report_data = {
            'summary': self._create_summary(results_data),
            'results': results_data,
            'metadata': metadata,
            'generated_at': datetime.now().isoformat(),
            'total_count': len(results_data)
        }
        
        # 生成各种格式的报告
        report_files = {}
        
        # 1. JSON报告
        json_report = self._generate_json_report(report_data)
        report_files['json'] = json_report
        
        # 2. Markdown报告
        markdown_report = self._generate_markdown_report(report_data)
        report_files['markdown'] = markdown_report
        
        # 3. 文本摘要
        text_report = self._generate_text_report(report_data)
        report_files['text'] = text_report
        
        return report_files
    
    def _create_summary(self, results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """创建测试摘要"""
        total = len(results)
        passed = sum(1 for r in results if self._is_passed(r))
        failed = sum(1 for r in results if self._is_failed(r))
        skipped = sum(1 for r in results if self._is_skipped(r))
        
        total_duration = sum(float(r.get('duration', 0)) for r in results)
        success_rate = (passed / total * 100) if total > 0 else 0
        
        return {
            'total_tests': total,
            'passed_tests': passed,
            'failed_tests': failed,
            'skipped_tests': skipped,
            'total_duration': total_duration,
            'success_rate': success_rate,
            'generated_at': datetime.now().isoformat()
        }
    
    def _is_passed(self, result: Dict[str, Any]) -> bool:
        """判断测试是否通过"""
        status = str(result.get('status', '')).lower()
        return status in ['passed', 'pass', 'success']
    
    def _is_failed(self, result: Dict[str, Any]) -> bool:
        """判断测试是否失败"""
        status = str(result.get('status', '')).lower()
        return status in ['failed', 'fail', 'error', 'failure']
    
    def _is_skipped(self, result: Dict[str, Any]) -> bool:
        """判断测试是否跳过"""
        status = str(result.get('status', '')).lower()
        return status in ['skipped', 'skip', 'ignored']
    
    def _generate_json_report(self, report_data: Dict[str, Any]) -> Path:
        """生成JSON格式报告"""
        filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2, default=str)
        
        print(f"[OK] JSON报告已生成: {filename}")
        return filename
    
    def _generate_markdown_report(self, report_data: Dict[str, Any]) -> Path:
        """生成Markdown格式报告"""
        summary = report_data['summary']
        results = report_data['results']
        
        markdown_content = f"""# 测试报告

## 测试摘要

| 指标 | 值 |
|------|-----|
| 总测试数 | {summary['total_tests']} |
| 通过测试 | {summary['passed_tests']} |
| 失败测试 | {summary['failed_tests']} |
| 跳过测试 | {summary['skipped_tests']} |
| 成功率 | {summary['success_rate']:.1f}% |
| 总耗时 | {summary['total_duration']:.2f} 秒 |

## 测试结果详情 ({len(results)} 条记录)

| 测试ID | 测试名称 | 状态 | 耗时(秒) | 错误信息 |
|--------|----------|------|----------|----------|
"""
        
        for result in results:
            test_id = result.get('test_id', 'N/A')
            test_name = result.get('test_name', 'N/A')
            status = result.get('status', 'N/A')
            duration = result.get('duration', 0)
            error_msg = str(result.get('error_message', ''))[:100] if result.get('error_message') else ''
            
            markdown_content += f"| {test_id} | {test_name} | {status} | {duration:.2f} | {error_msg} |\n"
        
        markdown_content += f"""
## 元数据
```json
{json.dumps(report_data['metadata'], ensure_ascii=False, indent=2)}
```

---
*报告生成时间: {report_data['generated_at']}*
"""
        
        filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(markdown_content)
        
        print(f"[OK] Markdown报告已生成: {filename}")
        return filename
    
    def _generate_text_report(self, report_data: Dict[str, Any]) -> Path:
        """生成文本格式报告"""
        summary = report_data['summary']
        results = report_data['results']
        
        text_content = f"""测试报告
==========

测试摘要
--------
总测试数: {summary['total_tests']}
通过测试: {summary['passed_tests']}
失败测试: {summary['failed_tests']}
跳过测试: {summary['skipped_tests']}
成功率: {summary['success_rate']:.1f}%
总耗时: {summary['total_duration']:.2f} 秒

测试结果详情 ({len(results)} 条记录)
--------
"""
        
        for result in results:
            test_id = result.get('test_id', 'N/A')
            test_name = result.get('test_name', 'N/A')
            status = result.get('status', 'N/A')
            duration = result.get('duration', 0)
            error_msg = str(result.get('error_message', '')) if result.get('error_message') else ''
            
            text_content += f"- {test_name} ({test_id}) [{status}] {duration:.2f}s"
            if error_msg:
                text_content += f" - {error_msg[:100]}"
            text_content += "\n"
        
        text_content += f"""
报告信息
--------
生成时间: {report_data['generated_at']}
元数据: {json.dumps(report_data['metadata'], ensure_ascii=False)}
"""
        
        filename = self.output_dir / f"test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(text_content)
        
        print(f"[OK] 文本报告已生成: {filename}")
        return filename

# 使用示例
if __name__ == "__main__":
    print("测试报告生成器（简化版）")
    print("=" * 60)
    
    # 创建报告生成器
    generator = TestReportGeneratorSimple()
    
    # 创建示例测试结果
    from datetime import datetime, timedelta
    
    class MockTestResult:
        def __init__(self, test_id, test_name, status, duration):
            self.test_id = test_id
            self.test_name = test_name
            self.status = status
            self.duration = duration
            self.start_time = datetime.now() - timedelta(seconds=duration)
            self.end_time = datetime.now()
            self.error_message = None if status == 'passed' else f"测试 {test_id} 失败"
        
        def to_dict(self):
            return {
                'test_id': self.test_id,
                'test_name': self.test_name,
                'status': self.status,
                'duration': self.duration,
                'start_time': self.start_time.isoformat(),
                'end_time': self.end_time.isoformat(),
                'error_message': self.error_message
            }
    
    # 生成示例数据
    mock_results = [
        MockTestResult("test_001", "用户登录测试", "passed", 1.2),
        MockTestResult("test_002", "订单创建测试", "passed", 2.5),
        MockTestResult("test_003", "库存查询测试", "failed", 3.1),
        MockTestResult("test_004", "支付流程测试", "passed", 1.8),
        MockTestResult("test_005", "数据导出测试", "skipped", 0.0)
    ]
    
    # 生成报告
    metadata = {
        'project': 'AI-Ready',
        'sprint': 'Sprint 27+1',
        'environment': 'test',
        'test_type': 'integration',
        'start_time': (datetime.now() - timedelta(minutes=10)).isoformat(),
        'end_time': datetime.now().isoformat()
    }
    
    report_files = generator.generate_report(mock_results, metadata)
    
    print("\n生成的报告文件:")
    for format_name, filepath in report_files.items():
        print(f"  {format_name}: {filepath.name}")
    
    print("\n[OK] 测试报告生成器已就绪！")