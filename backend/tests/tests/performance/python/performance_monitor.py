#!/usr/bin/env python3
"""
AI-Ready 性能测试监控脚本
Sprint 27+1 测试环境配置 - 性能测试自动化脚本开发

功能:
1. Prometheus监控集成
2. Grafana Dashboard配置
3. 实时性能指标采集
4. 监控数据推送
"""

import json
import requests
import time
import threading
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional


class PerformanceMonitor:
    """性能监控器 - Prometheus/Grafana集成"""
    
    def __init__(self, config: Dict = None):
        self.config = config or {
            "prometheus": {
                "enabled": False,
                "url": "http://localhost:9090",
                "pushInterval": 10
            },
            "grafana": {
                "enabled": False,
                "url": "http://localhost:3000",
                "dashboardId": "jmeter-performance"
            },
            "influxdb": {
                "enabled": False,
                "url": "http://localhost:8086",
                "org": "ai-ready",
                "bucket": "jmeter_results"
            }
        }
        
        self.metrics = {}
        self.monitoring_thread = None
        self.is_monitoring = False
    
    def start_monitoring(self):
        """启动监控"""
        if self.is_monitoring:
            return
        
        self.is_monitoring = True
        self.monitoring_thread = threading.Thread(target=self._monitor_loop)
        self.monitoring_thread.daemon = True
        self.monitoring_thread.start()
        print("性能监控已启动")
    
    def stop_monitoring(self):
        """停止监控"""
        self.is_monitoring = False
        if self.monitoring_thread:
            self.monitoring_thread.join(timeout=5)
        print("性能监控已停止")
    
    def _monitor_loop(self):
        """监控循环"""
        while self.is_monitoring:
            try:
                self._collect_metrics()
                if self.config["prometheus"]["enabled"]:
                    self._push_to_prometheus()
                if self.config["influxdb"]["enabled"]:
                    self._push_to_influxdb()
                
                time.sleep(self.config["prometheus"]["pushInterval"])
            except Exception as e:
                print(f"监控异常: {e}")
                time.sleep(5)
    
    def _collect_metrics(self):
        """采集性能指标"""
        self.metrics = {
            "timestamp": datetime.now().isoformat(),
            "test_name": "AI-Ready Core Business Performance Test",
            "metrics": {
                "response_time_avg": self.metrics.get("response_time_avg", 0),
                "response_time_p95": self.metrics.get("response_time_p95", 0),
                "throughput": self.metrics.get("throughput", 0),
                "error_rate": self.metrics.get("error_rate", 0),
                "active_threads": self.metrics.get("active_threads", 0),
                "completed_samples": self.metrics.get("completed_samples", 0)
            }
        }
    
    def _push_to_prometheus(self):
        """推送到Prometheus Pushgateway"""
        if not self.config["prometheus"]["enabled"]:
            return
        
        pushgateway_url = f"{self.config['prometheus']['url']}/metrics/job/jmeter_performance"
        
        # 构建Prometheus格式数据
        metrics_data = ""
        for key, value in self.metrics["metrics"].items():
            metrics_data += f"jmeter_{key} {value}\n"
        
        try:
            response = requests.post(pushgateway_url, data=metrics_data, timeout=5)
            if response.status_code == 202:
                print(f"指标已推送到Prometheus: {len(metrics_data)} bytes")
        except Exception as e:
            print(f"推送Prometheus失败: {e}")
    
    def _push_to_influxdb(self):
        """推送到InfluxDB"""
        if not self.config["influxdb"]["enabled"]:
            return
        
        influxdb_url = f"{self.config['influxdb']['url']}/api/v2/write"
        
        # 构建InfluxDB Line Protocol格式
        line_protocol = f"jmeter_performance "
        for key, value in self.metrics["metrics"].items():
            line_protocol += f"{key}={value},"
        line_protocol = line_protocol.rstrip(',')
        
        headers = {
            "Authorization": f"Token {self.config['influxdb'].get('token', '')}",
            "Content-Type": "text/plain"
        }
        
        params = {
            "org": self.config["influxdb"]["org"],
            "bucket": self.config["influxdb"]["bucket"],
            "precision": "s"
        }
        
        try:
            response = requests.post(influxdb_url, data=line_protocol, headers=headers, params=params, timeout=5)
            if response.status_code == 204:
                print(f"指标已推送到InfluxDB")
        except Exception as e:
            print(f"推送InfluxDB失败: {e}")
    
    def update_metrics(self, metrics: Dict):
        """更新监控指标"""
        self.metrics["metrics"].update(metrics)
    
    def get_current_metrics(self) -> Dict:
        """获取当前指标"""
        return self.metrics
    
    def generate_grafana_dashboard(self, output_dir: Path = None) -> Dict:
        """生成Grafana Dashboard配置"""
        dashboard = {
            "dashboard": {
                "id": None,
                "title": "AI-Ready Performance Test Dashboard",
                "tags": ["jmeter", "performance", "ai-ready"],
                "timezone": "browser",
                "schemaVersion": 16,
                "version": 0,
                "refresh": "10s",
                "panels": [
                    {
                        "id": 1,
                        "title": "响应时间趋势",
                        "type": "graph",
                        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
                        "targets": [
                            {
                                "expr": "jmeter_response_time_avg",
                                "legendFormat": "平均响应时间"
                            },
                            {
                                "expr": "jmeter_response_time_p95",
                                "legendFormat": "P95响应时间"
                            }
                        ],
                        "yaxes": [
                            {"format": "ms", "label": "响应时间"},
                            {"format": "short"}
                        ]
                    },
                    {
                        "id": 2,
                        "title": "吞吐量",
                        "type": "graph",
                        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0},
                        "targets": [
                            {
                                "expr": "jmeter_throughput",
                                "legendFormat": "吞吐量 (req/sec)"
                            }
                        ],
                        "yaxes": [
                            {"format": "reqps", "label": "吞吐量"},
                            {"format": "short"}
                        ]
                    },
                    {
                        "id": 3,
                        "title": "错误率",
                        "type": "gauge",
                        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 8},
                        "targets": [
                            {
                                "expr": "jmeter_error_rate",
                                "legendFormat": "错误率 %"
                            }
                        ],
                        "options": {
                            "max": 100,
                            "min": 0,
                            "thresholds": [
                                {"value": 0, "color": "green"},
                                {"value": 1, "color": "yellow"},
                                {"value": 5, "color": "red"}
                            ]
                        }
                    },
                    {
                        "id": 4,
                        "title": "活跃线程数",
                        "type": "stat",
                        "gridPos": {"h": 8, "w": 6, "x": 6, "y": 8},
                        "targets": [
                            {
                                "expr": "jmeter_active_threads",
                                "legendFormat": "活跃线程"
                            }
                        ]
                    },
                    {
                        "id": 5,
                        "title": "已完成样本数",
                        "type": "stat",
                        "gridPos": {"h": 8, "w": 6, "x": 12, "y": 8},
                        "targets": [
                            {
                                "expr": "jmeter_completed_samples",
                                "legendFormat": "已完成样本"
                            }
                        ]
                    }
                ],
                "templating": {
                    "list": []
                }
            },
            "overwrite": True
        }
        
        if output_dir:
            output_dir.mkdir(parents=True, exist_ok=True)
            output_file = output_dir / "grafana_dashboard.json"
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(dashboard, f, ensure_ascii=False, indent=2)
            print(f"Grafana Dashboard配置已生成: {output_file}")
        
        return dashboard
    
    def generate_prometheus_rules(self, output_dir: Path = None) -> str:
        """生成Prometheus告警规则"""
        rules = """groups:
  - name: jmeter_performance_alerts
    rules:
      - alert: HighResponseTime
        expr: jmeter_response_time_avg > 100
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "平均响应时间过高"
          description: "平均响应时间 {{ $value }}ms 超过100ms基准"
      
      - alert: HighP95ResponseTime
        expr: jmeter_response_time_p95 > 200
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "P95响应时间过高"
          description: "P95响应时间 {{ $value }}ms 超过200ms基准"
      
      - alert: HighErrorRate
        expr: jmeter_error_rate > 1
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "错误率过高"
          description: "错误率 {{ $value }}% 超过1%基准"
      
      - alert: LowThroughput
        expr: jmeter_throughput < 50
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "吞吐量过低"
          description: "吞吐量 {{ $value }} req/sec 低于50 req/sec基准"
"""
        
        if output_dir:
            output_dir.mkdir(parents=True, exist_ok=True)
            output_file = output_dir / "prometheus_alert_rules.yml"
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(rules)
            print(f"Prometheus告警规则已生成: {output_file}")
        
        return rules


class PerformanceResultAnalyzer:
    """性能结果分析器"""
    
    def __init__(self):
        self.results = None
        self.baseline = {
            "avgResponseTime": 100,
            "p95ResponseTime": 200,
            "errorRate": 1,
            "throughput": 100
        }
    
    def load_results(self, results_file: Path) -> Dict:
        """加载测试结果"""
        with open(results_file, 'r', encoding='utf-8') as f:
            self.results = json.load(f)
        return self.results
    
    def analyze_performance(self) -> Dict:
        """分析性能表现"""
        if not self.results:
            return {}
        
        analysis = {
            "overall_status": self._evaluate_overall(),
            "performance_score": self._calculate_performance_score(),
            "baseline_comparison": self._compare_with_baseline(),
            "bottleneck_analysis": self._identify_bottlenecks(),
            "recommendations": self._generate_recommendations()
        }
        
        return analysis
    
    def _evaluate_overall(self) -> str:
        """评估整体状态"""
        if not self.results:
            return "UNKNOWN"
        
        avg_time = self.results.get("avg_response_time", 0)
        p95_time = self.results.get("p95_response_time", 0)
        error_rate = self.results.get("error_rate", 0)
        
        if avg_time <= self.baseline["avgResponseTime"] and \
           p95_time <= self.baseline["p95ResponseTime"] and \
           error_rate <= self.baseline["errorRate"]:
            return "EXCELLENT"
        
        if avg_time <= self.baseline["avgResponseTime"] * 1.2 and \
           p95_time <= self.baseline["p95ResponseTime"] * 1.2 and \
           error_rate <= self.baseline["errorRate"] * 2:
            return "GOOD"
        
        if avg_time <= self.baseline["avgResponseTime"] * 1.5 and \
           p95_time <= self.baseline["p95ResponseTime"] * 1.5 and \
           error_rate <= self.baseline["errorRate"] * 3:
            return "ACCEPTABLE"
        
        return "POOR"
    
    def _calculate_performance_score(self) -> int:
        """计算性能评分 (0-100)"""
        if not self.results:
            return 0
        
        # 响应时间评分 (40分)
        avg_time = self.results.get("avg_response_time", 0)
        avg_score = max(0, min(40, 40 - (avg_time - self.baseline["avgResponseTime"]) / 10))
        
        # P95响应时间评分 (30分)
        p95_time = self.results.get("p95_response_time", 0)
        p95_score = max(0, min(30, 30 - (p95_time - self.baseline["p95ResponseTime"]) / 10))
        
        # 错误率评分 (20分)
        error_rate = self.results.get("error_rate", 0)
        error_score = max(0, min(20, 20 - error_rate * 2))
        
        # 吞吐量评分 (10分)
        throughput = self.results.get("throughput", 0)
        throughput_score = max(0, min(10, throughput / self.baseline["throughput"] * 10))
        
        return int(avg_score + p95_score + error_score + throughput_score)
    
    def _compare_with_baseline(self) -> Dict:
        """与基准对比"""
        comparison = {}
        
        for key, baseline_value in self.baseline.items():
            actual_value = self.results.get(key.replace("ResponseTime", "_response_time").replace("Rate", "_rate"), 0)
            
            status = "PASS" if actual_value <= baseline_value else "FAIL"
            deviation = (actual_value - baseline_value) / baseline_value * 100
            
            comparison[key] = {
                "baseline": baseline_value,
                "actual": actual_value,
                "deviation": f"{deviation:.2f}%",
                "status": status
            }
        
        return comparison
    
    def _identify_bottlenecks(self) -> List[str]:
        """识别性能瓶颈"""
        bottlenecks = []
        
        if not self.results:
            return bottlenecks
        
        avg_time = self.results.get("avg_response_time", 0)
        p95_time = self.results.get("p95_response_time", 0)
        max_time = self.results.get("max_response_time", 0)
        error_rate = self.results.get("error_rate", 0)
        
        if p95_time > avg_time * 2:
            bottlenecks.append(f"P95响应时间({p95_time}ms)是平均响应时间({avg_time}ms)的2倍以上，存在长尾延迟问题")
        
        if max_time > p95_time * 2:
            bottlenecks.append(f"最大响应时间({max_time}ms)异常，可能存在极端慢请求")
        
        if error_rate > self.baseline["errorRate"]:
            bottlenecks.append(f"错误率({error_rate:.2f}%)超过基准({self.baseline['errorRate']}%)，系统稳定性存在问题")
        
        # 按场景分析
        scenarios = self.results.get("scenarios", {})
        for scenario_name, scenario_stats in scenarios.items():
            if scenario_stats.get("avg_response_time", 0) > self.baseline["avgResponseTime"] * 1.5:
                bottlenecks.append(f"{scenario_name}场景响应时间偏高，需要专项优化")
        
        if not bottlenecks:
            bottlenecks.append("未发现明显性能瓶颈")
        
        return bottlenecks
    
    def _generate_recommendations(self) -> List[str]:
        """生成优化建议"""
        recommendations = []
        
        overall = self._evaluate_overall()
        
        if overall == "EXCELLENT":
            recommendations.append("性能表现优秀，建议保持现有配置")
        elif overall == "GOOD":
            recommendations.append("性能表现良好，可考虑小范围优化")
        elif overall == "ACCEPTABLE":
            recommendations.append("性能表现一般，建议进行系统性优化")
        else:
            recommendations.append("性能表现不佳，建议进行全面性能优化")
        
        # 具体建议
        avg_time = self.results.get("avg_response_time", 0)
        if avg_time > self.baseline["avgResponseTime"]:
            recommendations.append("优化API处理逻辑，减少不必要的计算和数据库查询")
            recommendations.append("考虑引入缓存机制，减少重复计算")
        
        p95_time = self.results.get("p95_response_time", 0)
        if p95_time > self.baseline["p95ResponseTime"]:
            recommendations.append("分析长尾延迟原因，优化慢查询")
            recommendations.append("增加连接池和线程池配置")
        
        error_rate = self.results.get("error_rate", 0)
        if error_rate > self.baseline["errorRate"]:
            recommendations.append("检查服务稳定性，修复可能导致错误的代码")
            recommendations.append("增加重试机制和降级策略")
        
        return recommendations
    
    def generate_analysis_report(self, output_dir: Path) -> Path:
        """生成分析报告"""
        analysis = self.analyze_performance()
        
        output_dir.mkdir(parents=True, exist_ok=True)
        output_file = output_dir / "performance_analysis_report.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(analysis, f, ensure_ascii=False, indent=2)
        
        print(f"性能分析报告已生成: {output_file}")
        return output_file


def main():
    """主函数"""
    print("=== AI-Ready Performance Monitor ===")
    
    # 创建监控器
    monitor = PerformanceMonitor()
    
    # 生成Grafana Dashboard
    dashboard = monitor.generate_grafana_dashboard(Path(__file__).parent.parent / "config")
    
    # 生成Prometheus告警规则
    rules = monitor.generate_prometheus_rules(Path(__file__).parent.parent / "config")
    
    print("\n监控配置已生成，请配置Prometheus和Grafana后启用监控")
    
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())