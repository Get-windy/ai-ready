#!/usr/bin/env python3
"""
测试报告生成器

生成标准化的测试报告，支持多种格式输出
"""

import json
import csv
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict
import jinja2

@dataclass
class TestSummary:
    """测试摘要数据类"""
    total_tests: int
    passed_tests: int
    failed_tests: int
    skipped_tests: int
    total_duration: float
    start_time: datetime
    end_time: datetime
    success_rate: float
    
    @classmethod
    def from_results(cls, results: List[Any], start_time: datetime, end_time: datetime):
        """从测试结果创建摘要"""
        total = len(results)
        passed = sum(1 for r in results if getattr(r, 'status', '').lower() in ['passed', 'pass'])
        failed = sum(1 for r in results if getattr(r, 'status', '').lower() in ['failed', 'fail'])
        skipped = sum(1 for r in results if getattr(r, 'status', '').lower() in ['skipped', 'skip'])
        
        total_duration = sum(getattr(r, 'duration', 0) for r in results)
        success_rate = (passed / total * 100) if total > 0 else 0
        
        return cls(
            total_tests=total,
            passed_tests=passed,
            failed_tests=failed,
            skipped_tests=skipped,
            total_duration=total_duration,
            start_time=start_time,
            end_time=end_time,
            success_rate=success_rate
        )

class TestReportGenerator:
    """测试报告生成器"""
    
    def __init__(self, output_dir: str = "./reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 初始化Jinja2模板引擎
        self.template_loader = jinja2.DictLoader({
            'html_report': self._get_html_template(),
            'markdown_report': self._get_markdown_template(),
            'text_summary': self._get_text_template()
        })
        self.template_env = jinja2.Environment(loader=self.template_loader)
    
    def generate_report(self, results: List[Any], metadata: Dict[str, Any] = None) -> Dict[str, Path]:
        """生成测试报告"""
        if metadata is None:
            metadata = {}
        
        # 确保所有结果都有to_dict方法
        results_data = []
        for result in results:
            if hasattr(result, 'to_dict'):
                results_data.append(result.to_dict())
            elif isinstance(result, dict):
                results_data.append(result)
            else:
                # 尝试转换为字典
                try:
                    results_data.append(asdict(result))
                except:
                    results_data.append({'data': str(result)})
        
        # 创建测试摘要
        start_time = metadata.get('start_time', datetime.now())
        end_time = metadata.get('end_time', datetime.now())
        summary = TestSummary.from_results(results, start_time, end_time)
        
        # 准备报告数据
        report_data = {
            'summary': asdict(summary),
            'results': results_data,
            'metadata': metadata,
            'generated_at': datetime.now().isoformat(),
            'total_count': len(results),
            'failed_results': [r for r in results_data if r.get('status', '').lower() in ['failed', 'fail']],
            'passed_results': [r for r in results_data if r.get('status', '').lower() in ['passed', 'pass']],
            'skipped_results': [r for r in results_data if r.get('status', '').lower() in ['skipped', 'skip']]
        }
        
        # 生成各种格式的报告
        report_files = {}
        
        # 1. JSON报告
        json_report = self._generate_json_report(report_data)
        report_files['json'] = json_report
        
        # 2. HTML报告
        html_report = self._generate_html_report(report_data)
        report_files['html'] = html_report
        
        # 3. Markdown报告
        markdown_report = self._generate_markdown_report(report_data)
        report_files['markdown'] = markdown_report
        
        # 4. CSV报告
        csv_report = self._generate_csv_report(results_data)
        report_files['csv'] = csv_report
        
        # 5. 文本摘要
        text_report = self._generate_text_report(report_data)
        report_files['text'] = text_report
        
        return report_files
    
    def _generate_json_report(self, report_data: Dict[str, Any]) -> Path:
        """生成JSON格式报告"""
        filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2, default=str)
        
        print(f"[OK] JSON报告已生成: {filename}")
        return filename
    
    def _generate_html_report(self, report_data: Dict[str, Any]) -> Path:
        """生成HTML格式报告"""
        try:
            template = self.template_env.get_template('html_report')
            html_content = template.render(**report_data)
            
            filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.html"
            
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(html_content)
            
            print(f"[OK] HTML报告已生成: {filename}")
            return filename
            
        except Exception as e:
            print(f"[WARN] HTML报告生成失败: {e}")
            # 生成简单的HTML报告
            return self._generate_simple_html_report(report_data)
    
    def _generate_markdown_report(self, report_data: Dict[str, Any]) -> Path:
        """生成Markdown格式报告"""
        try:
            template = self.template_env.get_template('markdown_report')
            markdown_content = template.render(**report_data)
            
            filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
            
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(markdown_content)
            
            print(f"[OK] Markdown报告已生成: {filename}")
            return filename
            
        except Exception as e:
            print(f"[WARN] Markdown报告生成失败: {e}")
            # 生成简单的Markdown报告
            return self._generate_simple_markdown_report(report_data)
    
    def _generate_csv_report(self, results_data: List[Dict[str, Any]]) -> Path:
        """生成CSV格式报告"""
        if not results_data:
            filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
            with open(filename, 'w', encoding='utf-8') as f:
                f.write("No test results\n")
            return filename
        
        try:
            # 扁平化数据结构
            flat_data = []
            for result in results_data:
                flat_result = self._flatten_dict(result)
                flat_data.append(flat_result)
            
            if flat_data:
                fieldnames = flat_data[0].keys()
                filename = self.output_dir / f"test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
                
                with open(filename, 'w', encoding='utf-8', newline='') as f:
                    writer = csv.DictWriter(f, fieldnames=fieldnames)
                    writer.writeheader()
                    writer.writerows(flat_data)
                
                print(f"[OK] CSV报告已生成: {filename}")
                return filename
                
        except Exception as e:
            print(f"[WARN] CSV报告生成失败: {e}")
        
        # 生成简单的CSV报告
        return self._generate_simple_csv_report(results_data)
    
    def _generate_text_report(self, report_data: Dict[str, Any]) -> Path:
        """生成文本格式报告"""
        try:
            template = self.template_env.get_template('text_summary')
            text_content = template.render(**report_data)
            
            filename = self.output_dir / f"test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
            
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(text_content)
            
            print(f"[OK] 文本报告已生成: {filename}")
            return filename
            
        except Exception as e:
            print(f"[WARN] 文本报告生成失败: {e}")
            # 生成简单的文本报告
            return self._generate_simple_text_report(report_data)
    
    def _flatten_dict(self, d: Dict[str, Any], parent_key: str = '', sep: str = '_') -> Dict[str, Any]:
        """扁平化字典"""
        items = []
        for k, v in d.items():
            new_key = f"{parent_key}{sep}{k}" if parent_key else k
            if isinstance(v, dict):
                items.extend(self._flatten_dict(v, new_key, sep=sep).items())
            elif isinstance(v, list):
                # 处理列表，转换为字符串
                items.append((new_key, json.dumps(v, ensure_ascii=False, default=str)))
            else:
                items.append((new_key, v))
        return dict(items)
    
    def _generate_simple_html_report(self, report_data: Dict[str, Any]) -> Path:
        """生成简单的HTML报告"""
        summary = report_data.get('summary', {})
        
        html_content = f"""<!DOCTYPE html>
<html>
<head>
    <title>测试报告 - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .summary {{ background-color: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px; }}
        .success {{ color: green; font-weight: bold; }}
        .failure {{ color: red; font-weight: bold; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #f2f2f2; }}
        tr:nth-child(even) {{ background-color: #f9f9f9; }}
    </style>
</head>
<body>
    <h1>测试报告</h1>
    <div class="summary">
        <h2>测试摘要</h2>
        <p>总测试数: {summary.get('total_tests', 0)}</p>
        <p class="success">通过: {summary.get('passed_tests', 0)}</p>
        <p class="failure">失败: {summary.get('failed_tests', 0)}</p>
        <p>跳过: {summary.get('skipped_tests', 0)}</p>
        <p>成功率: {summary.get('success_rate', 0):.1f}%</p>
        <p>总耗时: {summary.get('total_duration', 0):.2f}秒</p>
        <p>开始时间: {summary.get('start_time', 'N/A')}</p>
        <p>结束时间: {summary.get('end_time', 'N/A')}</p>
    </div>
    
    <h2>测试结果详情</h2>
    <table>
        <tr>
            <th>测试ID</th>
            <th>测试名称</th>
            <th>状态</th>
            <th>耗时(秒)</th>
            <th>错误信息</th>
        </tr>
        {"".join(f'''
        <tr>
            <td>{r.get('test_id', 'N/A')}</td>
            <td>{r.get('test_name', 'N/A')}</td>
            <td class="{r.get('status', '').lower()}">{r.get('status', 'N/A')}</td>
            <td>{r.get('duration', 0):.2f}</td>
            <td>{r.get('error_message', '')[:100]}</td>
        </tr>
        ''' for r in report_data.get('results', []))}
    </table>
    
    <p>报告生成时间: {report_data.get('generated_at', 'N/A')}</p>
</body>
</html>"""
        
        filename = self.output_dir / f"simple_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.html"
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        return filename
    
    def _generate_simple_markdown_report(self, report_data: Dict[str, Any]) -> Path:
        """生成简单的Markdown报告"""
        summary = report_data.get('summary', {})
        
        markdown_content = f"""# 测试报告

## 测试摘要
- **总测试数**: {summary.get('total_tests', 0)}
- **通过**: {summary.get('passed_tests', 0)}
- **失败**: {summary.get('failed_tests', 0)}
- **跳过**: {summary.get('skipped_tests', 0)}
- **成功率**: {summary.get('success_rate', 0):.1f}%
- **总耗时**: {summary.get('total_duration', 0):.2f}秒
- **开始时间**: {summary.get('start_time', 'N/A')}
- **结束时间**: {summary.get('end_time', 'N/A')}

## 测试结果详情

| 测试ID | 测试名称 | 状态 | 耗时(秒) | 错误信息 |
|--------|----------|------|----------|----------|
{"".join(f"| {r.get('test_id', 'N/A')} | {r.get('test_name', 'N/A')} | {r.get('status', 'N/A')} | {r.get('duration', 0):.2f} | {str(r.get('error_message', ''))[:50]} |" for r in report_data.get('results', []))}

## 报告信息
- **生成时间**: {report_data.get('generated_at', 'N/A')}
- **总记录数**: {len(report_data.get('results', []))}
"""
        
        filename = self.output_dir / f"simple_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(markdown_content)
        
        return filename
    
    def _generate_simple_csv_report(self, results_data: List[Dict[str, Any]]) -> Path:
        """生成简单的CSV报告"""
        filename = self.output_dir / f"simple_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        
        if not results_data:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write("test_id,test_name,status,duration,error_message\n")
                f.write("no_results,N/A,N/A,0,No test results\n")
            return filename
        
        with open(filename, 'w', encoding='utf-8', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['test_id', 'test_name', 'status', 'duration', 'error_message'])
            
            for result in results_data:
                writer.writerow([
                    result.get('test_id', 'N/A'),
                    result.get('test_name', 'N/A'),
                    result.get('status', 'N/A'),
                    result.get('duration', 0),
                    str(result.get('error_message', ''))[:200]
                ])
        
        return filename
    
    def _generate_simple_text_report(self, report_data: Dict[str, Any]) -> Path:
        """生成简单的文本报告"""
        summary = report_data.get('summary', {})
        
        text_content = f"""测试报告
==========

测试摘要
--------
总测试数: {summary.get('total_tests', 0)}
通过: {summary.get('passed_tests', 0)}
失败: {summary.get('failed_tests', 0)}
跳过: {summary.get('skipped_tests', 0)}
成功率: {summary.get('success_rate', 0):.1f}%
总耗时: {summary.get('total_duration', 0):.2f}秒
开始时间: {summary.get('start_time', 'N/A')}
结束时间: {summary.get('end_time', 'N/A')}

测试结果详情
--------
{"".join(f"{r.get('test_id', 'N/A')} - {r.get('test_name', 'N/A')} [{r.get('status', 'N/A')}] {r.get('duration', 0):.2f}s" + (f" - {r.get('error_message', '')[:50]}" if r.get('error_message') else '') + "\\n" for r in report_data.get('results', []))}

报告信息
--------
生成时间: {report_data.get('generated_at', 'N/A')}
总记录数: {len(report_data.get('results', []))}
"""
        
        filename = self.output_dir / f"simple_test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(text_content)
        
        return filename
    
    def _get_html_template(self) -> str:
        """HTML模板"""
        return """<!DOCTYPE html>
<html>
<head>
    <title>测试报告 - {{ generated_at }}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .summary { background-color: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
        .summary-item { background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .summary-item h3 { margin-top: 0; }
        .status-passed { color: green; font-weight: bold; }
        .status-failed { color: red; font-weight: bold; }
        .status-skipped { color: orange; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #f2f2f2; font-weight: bold; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        tr:hover { background-color: #f5f5f5; }
        .error-details { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    </style>
</head>
<body>
    <h1>测试报告</h1>
    <p>生成时间: {{ generated_at }}</p>
    
    <div class="summary">
        <h2>测试摘要</h2>
        <div class="summary-grid">
            <div class="summary-item">
                <h3>总测试数</h3>
                <p>{{ summary.total_tests }}</p>
            </div>
            <div class="summary-item">
                <h3 class="status-passed">通过</h3>
                <p>{{ summary.passed_tests }} ({{ summary.success_rate|round(1) }}%)</p>
            </div>
            <div class="summary-item">
                <h3 class="status-failed">失败</h3>
                <p>{{ summary.failed_tests }}</p>
            </div>
            <div class="summary-item">
                <h3 class="status-skipped">跳过</h3>
                <p>{{ summary.skipped_tests }}</p>
            </div>
            <div class="summary-item">
                <h3>总耗时</h3>
                <p>{{ summary.total_duration|round(2) }} 秒</p>
            </div>
            <div class="summary-item">
                <h3>时间范围</h3>
                <p>{{ summary.start_time }} - {{ summary.end_time }}</p>
            </div>
        </div>
    </div>
    
    <h2>测试结果详情 ({{ total_count }} 条记录)</h2>
    
    {% if failed_results %}
    <h3 class="status-failed">失败测试 ({{ failed_results|length }})</h3>
    <table>
        <tr>
            <th>测试ID</th>
            <th>测试名称</th>
            <th>状态</th>
            <th>耗时(秒)</th>
            <th>错误信息</th>
        </tr>
        {% for result in failed_results %}
        <tr>
            <td>{{ result.test_id }}</td>
            <td>{{ result.test_name }}</td>
            <td class="status-failed">{{ result.status }}</td>
            <td>{{ result.duration|round(2) }}</td>
            <td class="error-details">{{ result.error_message }}</td>
        </tr>
        {% endfor %}
    </table>
    {% endif %}
    
    <h3>所有测试结果</h3>
    <table>
        <tr>
            <th>测试ID</th>
            <th>测试名称</th>
            <th>状态</th>
            <th>耗时(秒)</th>
            <th>开始时间</th>
            <th>结束时间</th>
        </tr>
        {% for result in results %}
        <tr>
            <td>{{ result.test_id }}</td>
            <td>{{ result.test_name }}</td>
            <td class="status-{{ result.status }}">{{ result.status }}</td>
            <td>{{ result.duration|round(2) }}</td>
            <td>{{ result.start_time }}</td>
            <td>{{ result.end_time }}</td>
        </tr>
        {% endfor %}
    </table>
    
    <h3>元数据</h3>
    <pre>{{ metadata|tojson(indent=2) }}</pre>
</body>
</html>"""
    
    def _get_markdown_template(self) -> str:
        """Markdown模板"""
        return """# 测试报告

## 测试摘要

| 指标 | 值 |
|------|-----|
| 总测试数 | {{ summary.total_tests }} |
| 通过测试 | {{ summary.passed_tests }} |
| 失败测试 | {{ summary.failed_tests }} |
| 跳过测试 | {{ summary.skipped_tests }} |
| 成功率 | {{ summary.success_rate|round(1) }}% |
| 总耗时 | {{ summary.total_duration|round(2) }} 秒 |
| 开始时间 | {{ summary.start_time }} |
| 结束时间 | {{ summary.end_time }} |

## 测试结果详情

### 失败测试 ({{ failed_results|length }})
{% if failed_results %}
| 测试ID | 测试名称 | 状态 | 耗时(秒) | 错误信息 |
|--------|----------|------|----------|----------|
{% for result in failed_results %}
| {{ result.test_id }} | {{ result.test_name }} | {{ result.status }} | {{ result.duration|round(2) }} | {{ result.error_message }} |
{% endfor %}
{% else %}
无失败测试
{% endif %}

### 所有测试结果 ({{ total_count }})
| 测试ID | 测试名称 | 状态 | 耗时(秒) | 开始时间 | 结束时间 |
|--------|----------|------|----------|----------|----------|
{% for result in results %}
| {{ result.test_id }} | {{ result.test_name }} | {{ result.status }} | {{ result.duration|round(2) }} | {{ result.start_time }} | {{ result.end_time }} |
{% endfor %}

## 元数据
```json
{{ metadata|tojson(indent=2) }}
```

---
*报告生成时间: {{ generated_at }}*
"""
    
    def _get_text_template(self) -> str:
        """文本模板"""
        return """测试报告
==========

测试摘要
--------
总测试数: {{ summary.total_tests }}
通过测试: {{ summary.passed_tests }}
失败测试: {{ summary.failed_tests }}
跳过测试: {{ summary.skipped_tests }}
成功率: {{ summary.success_rate|round(1) }}%
总耗时: {{ summary.total_duration|round(2) }} 秒
开始时间: {{ summary.start_time }}
结束时间: {{ summary.end_time }}

失败测试详情
--------
{% if failed_results %}
{% for result in failed_results %}
- {{ result.test_name }} ({{ result.test_id }})
  状态: {{ result.status }}
  耗时: {{ result.duration|round(2) }}秒
  错误: {{ result.error_message }}
{% endfor %}
{% else %}
无失败测试
{% endif %}

测试统计
--------
总记录数: {{ total_count }}
失败数: {{ failed_results|length }}
通过数: {{ passed_results|length }}
跳过数: {{ skipped_results|length }}

报告信息
--------
生成时间: {{ generated_at }}

"""

# 使用示例
if __name__ == "__main__":
    print("测试报告生成器")
    print("=" * 60)
    
    # 创建报告生成器
    generator = TestReportGenerator()
    
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
        'start_time': datetime.now() - timedelta(minutes=10),
        'end_time': datetime.now()
    }
    
    report_files = generator.generate_report(mock_results, metadata)
    
    print("\n生成的报告文件:")
    for format_name, filepath in report_files.items():
        print(f"  {format_name}: {filepath.name}")
    
    print("\n[OK] 测试报告生成器已就绪！")