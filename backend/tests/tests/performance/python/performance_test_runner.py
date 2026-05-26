#!/usr/bin/env python3
"""
AI-Ready 性能测试自动化执行脚本
Sprint 27+1 测试环境配置 - 性能测试自动化脚本开发

功能:
1. 执行JMeter性能测试脚本
2. 解析测试结果
3. 生成性能测试报告
4. 支持Prometheus/Grafana监控集成
"""

import os
import sys
import json
import subprocess
import argparse
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# 配置路径
JMETER_SCRIPTS_DIR = Path(__file__).parent.parent / "jmeter"
RESULTS_DIR = Path(__file__).parent.parent / "results"
DATA_DIR = Path(__file__).parent.parent / "data"
CONFIG_DIR = Path(__file__).parent.parent / "config"


class PerformanceTestRunner:
    """性能测试执行器"""
    
    def __init__(self, config_file: str = None):
        self.config = self._load_config(config_file)
        self.jmeter_path = self._find_jmeter()
        self.test_results = []
        self.start_time = None
        self.end_time = None
        
    def _load_config(self, config_file: str) -> Dict:
        """加载测试配置"""
        if config_file:
            config_path = Path(config_file)
            if config_path.exists():
                with open(config_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
        
        # 默认配置
        default_config_path = CONFIG_DIR / "performance_test_config.json"
        if default_config_path.exists():
            with open(default_config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        
        # 返回硬编码默认配置
        return {
            "server": {
                "baseUrl": "localhost",
                "port": 8080,
                "protocol": "http"
            },
            "scenarios": {
                "light": {"threads": 50, "rampup": 10, "loops": 50},
                "medium": {"threads": 100, "rampup": 15, "loops": 30},
                "heavy": {"threads": 200, "rampup": 30, "loops": 20}
            },
            "baseline": {
                "avgResponseTime": 100,
                "p95ResponseTime": 200,
                "errorRate": 1
            }
        }
    
    def _find_jmeter(self) -> str:
        """查找JMeter可执行文件"""
        # 常见JMeter安装位置
        common_paths = [
            "jmeter",  # 命令行直接可用
            "/usr/local/bin/jmeter",
            "/opt/jmeter/bin/jmeter",
            "/opt/apache-jmeter/bin/jmeter",
            "C:/Program Files/JMeter/bin/jmeter.bat",
            "C:/apache-jmeter/bin/jmeter.bat",
            os.environ.get("JMETER_HOME", "") + "/bin/jmeter" if os.environ.get("JMETER_HOME") else None
        ]
        
        for path in common_paths:
            if path:
                try:
                    result = subprocess.run([path, "--version"], capture_output=True, timeout=5)
                    if result.returncode == 0:
                        return path
                except Exception:
                    continue
        
        return "jmeter"  # 默认尝试命令行
    
    def run_test(self, scenario: str = "medium", test_script: str = None) -> Tuple[bool, Dict]:
        """运行性能测试"""
        self.start_time = datetime.now()
        
        # 确定测试脚本
        if test_script:
            jmx_file = Path(test_script)
        else:
            jmx_file = JMETER_SCRIPTS_DIR / "ai-ready-core-business-performance-test.jmx"
        
        if not jmx_file.exists():
            return False, {"error": f"JMeter脚本不存在: {jmx_file}"}
        
        # 创建结果目录
        results_dir = RESULTS_DIR / self.start_time.strftime("%Y%m%d_%H%M%S")
        results_dir.mkdir(parents=True, exist_ok=True)
        
        # 获取场景参数
        scenario_config = self.config.get("scenarios", {}).get(scenario, {})
        threads = scenario_config.get("threads", 100)
        rampup = scenario_config.get("rampup", 10)
        
        # 构建JMeter命令
        jtl_file = results_dir / "results.jtl"
        log_file = results_dir / "jmeter.log"
        
        cmd = [
            self.jmeter_path,
            "-n",  # 非GUI模式
            "-t", str(jmx_file),  # 测试计划
            "-l", str(jtl_file),  # 结果文件
            "-j", str(log_file),  # 日志文件
            "-JBASE_URL=" + self.config["server"]["baseUrl"],
            "-JPORT=" + str(self.config["server"]["port"]),
            "-Jthreads=" + str(threads),
            "-Jrampup=" + str(rampup),
            "-e",  # 生成报告
            "-o", str(results_dir / "html_report")  # HTML报告目录
        ]
        
        print(f"执行JMeter测试: {cmd}")
        
        try:
            result = subprocess.run(cmd, capture_output=True, timeout=600)
            self.end_time = datetime.now()
            
            if result.returncode == 0:
                # 解析结果
                test_result = self._parse_results(jtl_file)
                test_result["scenario"] = scenario
                test_result["duration_seconds"] = (self.end_time - self.start_time).total_seconds()
                test_result["results_dir"] = str(results_dir)
                
                # 生成报告
                self._generate_report(test_result, results_dir)
                
                return True, test_result
            else:
                return False, {
                    "error": f"JMeter执行失败",
                    "returncode": result.returncode,
                    "stderr": result.stderr.decode('utf-8', errors='ignore')
                }
                
        except subprocess.TimeoutExpired:
            return False, {"error": "测试超时（超过600秒）"}
        except Exception as e:
            return False, {"error": f"执行异常: {str(e)}"}
    
    def _parse_results(self, jtl_file: Path) -> Dict:
        """解析JMeter结果文件"""
        results = {
            "total_samples": 0,
            "success_count": 0,
            "failure_count": 0,
            "avg_response_time": 0,
            "min_response_time": 0,
            "max_response_time": 0,
            "p50_response_time": 0,
            "p90_response_time": 0,
            "p95_response_time": 0,
            "p99_response_time": 0,
            "throughput": 0,
            "error_rate": 0,
            "scenarios": {}
        }
        
        if not jtl_file.exists():
            return results
        
        response_times = []
        scenario_times = {}
        
        try:
            with open(jtl_file, 'r', encoding='utf-8') as f:
                # CSV格式解析
                lines = f.readlines()
                if len(lines) < 2:
                    return results
                
                # 解析header
                header = lines[0].strip().split(',')
                
                # 找到关键列索引
                time_stamp_idx = header.index('timeStamp') if 'timeStamp' in header else 0
                elapsed_idx = header.index('elapsed') if 'elapsed' in header else 1
                success_idx = header.index('success') if 'success' in header else 7
                label_idx = header.index('label') if 'label' in header else 2
                
                # 解析数据行
                for line in lines[1:]:
                    parts = line.strip().split(',')
                    if len(parts) > max(time_stamp_idx, elapsed_idx, success_idx, label_idx):
                        elapsed_time = float(parts[elapsed_idx])
                        is_success = parts[success_idx] == 'true'
                        label = parts[label_idx]
                        
                        results["total_samples"] += 1
                        if is_success:
                            results["success_count"] += 1
                        else:
                            results["failure_count"] += 1
                        
                        response_times.append(elapsed_time)
                        
                        # 按场景分组统计
                        if label not in scenario_times:
                            scenario_times[label] = []
                        scenario_times[label].append(elapsed_time)
                
                # 计算统计指标
                if response_times:
                    results["avg_response_time"] = sum(response_times) / len(response_times)
                    results["min_response_time"] = min(response_times)
                    results["max_response_time"] = max(response_times)
                    results["error_rate"] = results["failure_count"] / results["total_samples"] * 100
                    
                    # 排序计算百分位
                    sorted_times = sorted(response_times)
                    n = len(sorted_times)
                    results["p50_response_time"] = sorted_times[n // 2]
                    results["p90_response_time"] = sorted_times[int(n * 0.9)]
                    results["p95_response_time"] = sorted_times[int(n * 0.95)]
                    results["p99_response_time"] = sorted_times[int(n * 0.99)]
                
                # 场景统计
                for label, times in scenario_times.items():
                    results["scenarios"][label] = {
                        "count": len(times),
                        "avg_response_time": sum(times) / len(times) if times else 0,
                        "max_response_time": max(times) if times else 0,
                        "min_response_time": min(times) if times else 0,
                        "error_rate": sum(1 for t in times if t > 200) / len(times) * 100 if times else 0
                    }
                
                # 计算吞吐量
                if self.start_time and self.end_time:
                    duration = (self.end_time - self.start_time).total_seconds()
                    results["throughput"] = results["total_samples"] / duration if duration > 0 else 0
        
        except Exception as e:
            print(f"解析结果文件异常: {e}")
        
        return results
    
    def _generate_report(self, test_result: Dict, results_dir: Path):
        """生成性能测试报告"""
        report = {
            "test_info": {
                "name": "AI-Ready Core Business Performance Test",
                "scenario": test_result.get("scenario", "medium"),
                "start_time": self.start_time.isoformat() if self.start_time else None,
                "end_time": self.end_time.isoformat() if self.end_time else None,
                "duration_seconds": test_result.get("duration_seconds", 0),
                "results_dir": test_result.get("results_dir", str(results_dir))
            },
            "summary": {
                "total_samples": test_result["total_samples"],
                "success_rate": f"{test_result['success_count']/test_result['total_samples']*100:.2f}%" if test_result['total_samples'] > 0 else "0%",
                "error_rate": f"{test_result['error_rate']:.2f}%",
                "throughput": f"{test_result['throughput']:.2f} req/sec"
            },
            "response_times": {
                "avg": f"{test_result['avg_response_time']:.2f}ms",
                "min": f"{test_result['min_response_time']:.2f}ms",
                "max": f"{test_result['max_response_time']:.2f}ms",
                "p50": f"{test_result['p50_response_time']:.2f}ms",
                "p90": f"{test_result['p90_response_time']:.2f}ms",
                "p95": f"{test_result['p95_response_time']:.2f}ms",
                "p99": f"{test_result['p99_response_time']:.2f}ms"
            },
            "baseline_comparison": self._compare_baseline(test_result),
            "scenarios": test_result.get("scenarios", {}),
            "recommendations": self._generate_recommendations(test_result)
        }
        
        # 保存JSON报告
        json_report_file = results_dir / "performance_test_report.json"
        with open(json_report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        # 保存Markdown报告
        md_report_file = results_dir / "performance_test_report.md"
        md_content = self._format_md_report(report)
        with open(md_report_file, 'w', encoding='utf-8') as f:
            f.write(md_content)
        
        print(f"报告已生成: {json_report_file}")
        print(f"Markdown报告: {md_report_file}")
        
        return report
    
    def _compare_baseline(self, test_result: Dict) -> Dict:
        """与基准对比"""
        baseline = self.config.get("baseline", {})
        
        comparison = {
            "avg_response_time": {
                "actual": test_result["avg_response_time"],
                "baseline": baseline.get("avgResponseTime", 100),
                "status": "PASS" if test_result["avg_response_time"] <= baseline.get("avgResponseTime", 100) else "FAIL"
            },
            "p95_response_time": {
                "actual": test_result["p95_response_time"],
                "baseline": baseline.get("p95ResponseTime", 200),
                "status": "PASS" if test_result["p95_response_time"] <= baseline.get("p95ResponseTime", 200) else "FAIL"
            },
            "error_rate": {
                "actual": test_result["error_rate"],
                "baseline": baseline.get("errorRate", 1),
                "status": "PASS" if test_result["error_rate"] <= baseline.get("errorRate", 1) else "FAIL"
            }
        }
        
        # 计算总体状态
        all_pass = all(c["status"] == "PASS" for c in comparison.values())
        comparison["overall_status"] = "PASS" if all_pass else "FAIL"
        
        return comparison
    
    def _generate_recommendations(self, test_result: Dict) -> List[str]:
        """生成性能优化建议"""
        recommendations = []
        
        if test_result["avg_response_time"] > 100:
            recommendations.append("平均响应时间超过100ms，建议优化API处理逻辑或增加缓存")
        
        if test_result["p95_response_time"] > 200:
            recommendations.append("P95响应时间超过200ms，建议检查是否存在慢查询或资源瓶颈")
        
        if test_result["error_rate"] > 1:
            recommendations.append(f"错误率为{test_result['error_rate']:.2f}%，超过1%基准，建议检查服务稳定性")
        
        # 按场景分析
        for label, stats in test_result.get("scenarios", {}).items():
            if stats["avg_response_time"] > 150:
                recommendations.append(f"{label}场景响应时间偏高({stats['avg_response_time']:.2f}ms)，建议专项优化")
        
        if not recommendations:
            recommendations.append("性能表现良好，符合基准标准")
        
        return recommendations
    
    def _format_md_report(self, report: Dict) -> str:
        """格式化Markdown报告"""
        md = f"""# AI-Ready Core Business Performance Test Report

## Test Information

- **Test Name**: {report['test_info']['name']}
- **Scenario**: {report['test_info']['scenario']}
- **Start Time**: {report['test_info']['start_time']}
- **End Time**: {report['test_info']['end_time']}
- **Duration**: {report['test_info']['duration_seconds']} seconds

## Summary

| Metric | Value |
|--------|-------|
| Total Samples | {report['summary']['total_samples']} |
| Success Rate | {report['summary']['success_rate']} |
| Error Rate | {report['summary']['error_rate']} |
| Throughput | {report['summary']['throughput']} |

## Response Times

| Percentile | Response Time |
|------------|---------------|
| Average | {report['response_times']['avg']} |
| Min | {report['response_times']['min']} |
| Max | {report['response_times']['max']} |
| P50 | {report['response_times']['p50']} |
| P90 | {report['response_times']['p90']} |
| P95 | {report['response_times']['p95']} |
| P99 | {report['response_times']['p99']} |

## Baseline Comparison

| Metric | Actual | Baseline | Status |
|--------|--------|----------|--------|
| Avg Response Time | {report['baseline_comparison']['avg_response_time']['actual']:.2f}ms | {report['baseline_comparison']['avg_response_time']['baseline']}ms | {report['baseline_comparison']['avg_response_time']['status']} |
| P95 Response Time | {report['baseline_comparison']['p95_response_time']['actual']:.2f}ms | {report['baseline_comparison']['p95_response_time']['baseline']}ms | {report['baseline_comparison']['p95_response_time']['status']} |
| Error Rate | {report['baseline_comparison']['error_rate']['actual']:.2f}% | {report['baseline_comparison']['error_rate']['baseline']}% | {report['baseline_comparison']['error_rate']['status']} |

**Overall Status**: {report['baseline_comparison']['overall_status']}

## Scenario Details

"""
        
        for label, stats in report.get('scenarios', {}).items():
            md += f"""### {label}

| Metric | Value |
|--------|-------|
| Count | {stats['count']} |
| Avg Response Time | {stats['avg_response_time']:.2f}ms |
| Max Response Time | {stats['max_response_time']:.2f}ms |
| Min Response Time | {stats['min_response_time']:.2f}ms |

"""
        
        md += """## Recommendations

"""
        for rec in report.get('recommendations', []):
            md += f"- {rec}\n"
        
        md += f"""
---

**Generated at**: {datetime.now().isoformat()}
**Report Location**: {report['test_info']['results_dir']}
"""
        
        return md


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="AI-Ready 性能测试执行脚本")
    parser.add_argument("-c", "--config", help="配置文件路径")
    parser.add_argument("-s", "--scenario", default="medium", choices=["light", "medium", "heavy", "stress"],
                        help="测试场景 (light/medium/heavy/stress)")
    parser.add_argument("-t", "--test-script", help="指定JMeter测试脚本路径")
    parser.add_argument("-v", "--verbose", action="store_true", help="详细输出")
    
    args = parser.parse_args()
    
    runner = PerformanceTestRunner(args.config)
    
    print(f"=== AI-Ready Performance Test Runner ===")
    print(f"JMeter Path: {runner.jmeter_path}")
    print(f"Scenario: {args.scenario}")
    print(f"Config: {runner.config}")
    print(f"")
    
    success, result = runner.run_test(args.scenario, args.test_script)
    
    if success:
        print(f"\n=== Test Completed Successfully ===")
        print(f"Total Samples: {result['total_samples']}")
        print(f"Success Rate: {result['success_count']/result['total_samples']*100:.2f}%")
        print(f"Error Rate: {result['error_rate']:.2f}%")
        print(f"Avg Response Time: {result['avg_response_time']:.2f}ms")
        print(f"P95 Response Time: {result['p95_response_time']:.2f}ms")
        print(f"Throughput: {result['throughput']:.2f} req/sec")
        print(f"\nResults Directory: {result['results_dir']}")
    else:
        print(f"\n=== Test Failed ===")
        print(f"Error: {result.get('error', 'Unknown error')}")
        if args.verbose:
            print(f"Details: {result}")
    
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())