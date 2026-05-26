#!/usr/bin/env python3
"""
测试监控系统验证脚本
用于验证测试监控系统的各个组件是否正常工作
"""

import os
import sys
import time
import json
import requests
import logging
from datetime import datetime
from typing import Dict, List, Optional, Tuple

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class TestMonitoringValidator:
    """测试监控系统验证器"""
    
    def __init__(self):
        self.base_urls = {
            'test_monitor': 'http://localhost:8100',
            'prometheus': 'http://localhost:9091',
            'alertmanager': 'http://localhost:9094',
            'grafana': 'http://localhost:3100',
            'test_report': 'http://localhost:8200'
        }
        
        self.results = {
            'services': {},
            'endpoints': {},
            'metrics': {},
            'alerts': {},
            'overall': 'PENDING'
        }
        
        self.timeout = 10
        self.retry_count = 3
        self.retry_delay = 2
    
    def validate_all(self) -> Dict[str, any]:
        """验证所有组件"""
        logger.info("开始验证测试监控系统...")
        
        try:
            # 1. 验证服务连通性
            self._validate_services()
            
            # 2. 验证API端点
            self._validate_endpoints()
            
            # 3. 验证监控指标
            self._validate_metrics()
            
            # 4. 验证告警系统
            self._validate_alerts()
            
            # 5. 验证数据流
            self._validate_data_flow()
            
            # 6. 生成验证报告
            report = self._generate_report()
            
            return report
            
        except Exception as e:
            logger.error(f"验证过程中发生错误: {str(e)}")
            self.results['overall'] = 'FAILED'
            self.results['error'] = str(e)
            return self.results
    
    def _validate_services(self):
        """验证服务连通性"""
        logger.info("验证服务连通性...")
        
        services_to_check = [
            ('test_monitor', '测试监控服务'),
            ('prometheus', 'Prometheus'),
            ('grafana', 'Grafana'),
            ('alertmanager', 'AlertManager')
        ]
        
        for service_key, service_name in services_to_check:
            url = self.base_urls.get(service_key)
            if not url:
                self.results['services'][service_key] = {
                    'status': 'SKIPPED',
                    'message': f'未配置{service_name}URL'
                }
                continue
            
            for i in range(self.retry_count):
                try:
                    response = requests.get(url, timeout=self.timeout)
                    if response.status_code == 200:
                        self.results['services'][service_key] = {
                            'status': 'PASSED',
                            'message': f'{service_name}服务正常 (HTTP {response.status_code})',
                            'response_time': response.elapsed.total_seconds()
                        }
                        break
                    else:
                        self.results['services'][service_key] = {
                            'status': 'FAILED',
                            'message': f'{service_name}返回异常状态码: {response.status_code}'
                        }
                except requests.exceptions.ConnectionError:
                    if i == self.retry_count - 1:
                        self.results['services'][service_key] = {
                            'status': 'FAILED',
                            'message': f'无法连接到{service_name}'
                        }
                    else:
                        time.sleep(self.retry_delay)
                except requests.exceptions.Timeout:
                    if i == self.retry_count - 1:
                        self.results['services'][service_key] = {
                            'status': 'FAILED',
                            'message': f'{service_name}连接超时'
                        }
                    else:
                        time.sleep(self.retry_delay)
                except Exception as e:
                    self.results['services'][service_key] = {
                        'status': 'FAILED',
                        'message': f'{service_name}验证失败: {str(e)}'
                    }
                    break
    
    def _validate_endpoints(self):
        """验证API端点"""
        logger.info("验证API端点...")
        
        endpoints_to_check = [
            ('test_monitor', '/health', '健康检查'),
            ('test_monitor', '/metrics', 'Prometheus指标'),
            ('test_monitor', '/report', '测试报告'),
            ('prometheus', '/api/v1/query?query=up', 'Prometheus查询API'),
            ('grafana', '/api/health', 'Grafana健康检查'),
            ('alertmanager', '/api/v2/status', 'AlertManager状态')
        ]
        
        for service_key, endpoint, endpoint_name in endpoints_to_check:
            url = self.base_urls.get(service_key)
            if not url:
                self.results['endpoints'][f'{service_key}{endpoint}'] = {
                    'status': 'SKIPPED',
                    'message': f'未配置{service_key}服务URL'
                }
                continue
            
            full_url = f"{url}{endpoint}"
            
            try:
                response = requests.get(full_url, timeout=self.timeout)
                
                if response.status_code == 200:
                    # 检查响应内容
                    content_valid = self._validate_endpoint_content(service_key, endpoint, response)
                    
                    self.results['endpoints'][f'{service_key}{endpoint}'] = {
                        'status': 'PASSED' if content_valid else 'WARNING',
                        'message': f'{endpoint_name}端点正常 (HTTP {response.status_code})',
                        'content_valid': content_valid,
                        'response_time': response.elapsed.total_seconds()
                    }
                else:
                    self.results['endpoints'][f'{service_key}{endpoint}'] = {
                        'status': 'FAILED',
                        'message': f'{endpoint_name}返回异常状态码: {response.status_code}'
                    }
                    
            except Exception as e:
                self.results['endpoints'][f'{service_key}{endpoint}'] = {
                    'status': 'FAILED',
                    'message': f'{endpoint_name}验证失败: {str(e)}'
                }
    
    def _validate_endpoint_content(self, service_key: str, endpoint: str, response) -> bool:
        """验证端点响应内容"""
        try:
            if service_key == 'test_monitor' and endpoint == '/health':
                data = response.json()
                return data.get('status') == 'healthy'
            
            elif service_key == 'test_monitor' and endpoint == '/metrics':
                # 检查是否为有效的Prometheus指标格式
                content = response.text
                return 'HELP' in content and 'TYPE' in content
            
            elif service_key == 'test_monitor' and endpoint == '/report':
                data = response.json()
                return 'summary' in data and 'modules' in data
            
            elif service_key == 'prometheus' and endpoint.startswith('/api/v1/query'):
                data = response.json()
                return data.get('status') == 'success'
            
            elif service_key == 'grafana' and endpoint == '/api/health':
                data = response.json()
                return data.get('database') == 'ok'
            
            elif service_key == 'alertmanager' and endpoint == '/api/v2/status':
                data = response.json()
                return 'cluster' in data
            
            return True
            
        except:
            return False
    
    def _validate_metrics(self):
        """验证监控指标"""
        logger.info("验证监控指标...")
        
        # 测试监控服务指标
        try:
            url = f"{self.base_urls['test_monitor']}/metrics"
            response = requests.get(url, timeout=self.timeout)
            
            if response.status_code == 200:
                metrics_text = response.text
                
                # 检查关键指标是否存在
                key_metrics = [
                    'test_execution_total',
                    'test_execution_duration_seconds',
                    'test_success_rate',
                    'test_failure_rate',
                    'test_cpu_usage_percent',
                    'test_memory_usage_mb'
                ]
                
                found_metrics = []
                missing_metrics = []
                
                for metric in key_metrics:
                    if metric in metrics_text:
                        found_metrics.append(metric)
                    else:
                        missing_metrics.append(metric)
                
                self.results['metrics']['test_monitor'] = {
                    'status': 'PASSED' if not missing_metrics else 'WARNING',
                    'message': f'找到 {len(found_metrics)}/{len(key_metrics)} 个关键指标',
                    'found': found_metrics,
                    'missing': missing_metrics,
                    'total_metrics': len(metrics_text.split('\n'))
                }
            else:
                self.results['metrics']['test_monitor'] = {
                    'status': 'FAILED',
                    'message': f'获取指标失败: HTTP {response.status_code}'
                }
                
        except Exception as e:
            self.results['metrics']['test_monitor'] = {
                'status': 'FAILED',
                'message': f'验证指标失败: {str(e)}'
            }
        
        # Prometheus指标
        try:
            url = f"{self.base_urls['prometheus']}/api/v1/label/__name__/values"
            response = requests.get(url, timeout=self.timeout)
            
            if response.status_code == 200:
                data = response.json()
                if data.get('status') == 'success':
                    metric_names = data.get('data', [])
                    
                    self.results['metrics']['prometheus'] = {
                        'status': 'PASSED',
                        'message': f'Prometheus中有 {len(metric_names)} 个指标',
                        'sample_metrics': metric_names[:10]  # 显示前10个指标作为示例
                    }
                else:
                    self.results['metrics']['prometheus'] = {
                        'status': 'FAILED',
                        'message': 'Prometheus返回失败状态'
                    }
            else:
                self.results['metrics']['prometheus'] = {
                    'status': 'FAILED',
                    'message': f'获取Prometheus指标失败: HTTP {response.status_code}'
                }
                
        except Exception as e:
            self.results['metrics']['prometheus'] = {
                'status': 'FAILED',
                'message': f'验证Prometheus指标失败: {str(e)}'
            }
    
    def _validate_alerts(self):
        """验证告警系统"""
        logger.info("验证告警系统...")
        
        # 检查Prometheus告警规则
        try:
            url = f"{self.base_urls['prometheus']}/api/v1/rules"
            response = requests.get(url, timeout=self.timeout)
            
            if response.status_code == 200:
                data = response.json()
                if data.get('status') == 'success':
                    rules = data.get('data', {}).get('groups', [])
                    
                    alert_rules = []
                    for group in rules:
                        for rule in group.get('rules', []):
                            if rule.get('type') == 'alerting':
                                alert_rules.append({
                                    'name': rule.get('name'),
                                    'state': rule.get('state'),
                                    'health': rule.get('health')
                                })
                    
                    self.results['alerts']['prometheus_rules'] = {
                        'status': 'PASSED',
                        'message': f'找到 {len(alert_rules)} 个告警规则',
                        'rules': alert_rules
                    }
                else:
                    self.results['alerts']['prometheus_rules'] = {
                        'status': 'FAILED',
                        'message': 'Prometheus规则API返回失败状态'
                    }
            else:
                self.results['alerts']['prometheus_rules'] = {
                    'status': 'FAILED',
                    'message': f'获取Prometheus规则失败: HTTP {response.status_code}'
                }
                
        except Exception as e:
            self.results['alerts']['prometheus_rules'] = {
                'status': 'FAILED',
                'message': f'验证Prometheus规则失败: {str(e)}'
            }
        
        # 检查AlertManager状态
        try:
            url = f"{self.base_urls['alertmanager']}/api/v2/status"
            response = requests.get(url, timeout=self.timeout)
            
            if response.status_code == 200:
                data = response.json()
                
                self.results['alerts']['alertmanager'] = {
                    'status': 'PASSED',
                    'message': 'AlertManager运行正常',
                    'cluster_status': data.get('cluster', {}).get('status'),
                    'version': data.get('versionInfo', {}).get('version')
                }
            else:
                self.results['alerts']['alertmanager'] = {
                    'status': 'FAILED',
                    'message': f'获取AlertManager状态失败: HTTP {response.status_code}'
                }
                
        except Exception as e:
            self.results['alerts']['alertmanager'] = {
                'status': 'FAILED',
                'message': f'验证AlertManager失败: {str(e)}'
            }
    
    def _validate_data_flow(self):
        """验证数据流"""
        logger.info("验证数据流...")
        
        # 发送测试数据到监控服务
        try:
            url = f"{self.base_urls['test_monitor']}/record-test"
            
            test_data = {
                'test_id': 'validation_test_001',
                'test_name': '监控系统验证测试',
                'module': 'test_monitoring',
                'test_type': 'integration',
                'status': 'passed',
                'duration_seconds': 0.5,
                'metadata': {
                    'validation': True,
                    'timestamp': time.time()
                }
            }
            
            response = requests.post(url, json=test_data, timeout=self.timeout)
            
            if response.status_code == 200:
                result = response.json()
                
                # 验证数据是否被正确记录
                report_url = f"{self.base_urls['test_monitor']}/report"
                report_response = requests.get(report_url, timeout=self.timeout)
                
                if report_response.status_code == 200:
                    report_data = report_response.json()
                    total_tests = report_data.get('summary', {}).get('total_tests', 0)
                    
                    self.results['data_flow'] = {
                        'status': 'PASSED',
                        'message': f'数据流验证成功，总测试数: {total_tests}',
                        'test_recorded': True,
                        'test_id': test_data['test_id']
                    }
                else:
                    self.results['data_flow'] = {
                        'status': 'WARNING',
                        'message': '测试数据已发送但无法验证报告',
                        'test_recorded': True
                    }
            else:
                self.results['data_flow'] = {
                    'status': 'FAILED',
                    'message': f'发送测试数据失败: HTTP {response.status_code}'
                }
                
        except Exception as e:
            self.results['data_flow'] = {
                'status': 'FAILED',
                'message': f'数据流验证失败: {str(e)}'
            }
    
    def _generate_report(self) -> Dict[str, any]:
        """生成验证报告"""
        logger.info("生成验证报告...")
        
        # 统计结果
        total_checks = 0
        passed_checks = 0
        failed_checks = 0
        warning_checks = 0
        
        # 遍历所有结果
        for category, checks in self.results.items():
            if isinstance(checks, dict):
                for check_name, check_result in checks.items():
                    if isinstance(check_result, dict):
                        total_checks += 1
                        status = check_result.get('status', 'UNKNOWN')
                        
                        if status == 'PASSED':
                            passed_checks += 1
                        elif status == 'FAILED':
                            failed_checks += 1
                        elif status == 'WARNING':
                            warning_checks += 1
        
        # 确定总体状态
        if failed_checks > 0:
            overall_status = 'FAILED'
        elif warning_checks > 0:
            overall_status = 'WARNING'
        elif passed_checks > 0:
            overall_status = 'PASSED'
        else:
            overall_status = 'UNKNOWN'
        
        self.results['summary'] = {
            'total_checks': total_checks,
            'passed_checks': passed_checks,
            'failed_checks': failed_checks,
            'warning_checks': warning_checks,
            'pass_rate': (passed_checks / total_checks * 100) if total_checks > 0 else 0,
            'overall_status': overall_status,
            'timestamp': datetime.now().isoformat(),
            'validator_version': '1.0.0'
        }
        
        self.results['overall'] = overall_status
        
        return self.results
    
    def print_report(self, report: Dict[str, any]):
        """打印验证报告"""
        print("\n" + "="*80)
        print("AI-Ready 测试监控系统验证报告")
        print("="*80)
        
        summary = report.get('summary', {})
        print(f"\n总体状态: {summary.get('overall_status', 'UNKNOWN')}")
        print(f"检查总数: {summary.get('total_checks', 0)}")
        print(f"通过检查: {summary.get('passed_checks', 0)}")
        print(f"失败检查: {summary.get('failed_checks', 0)}")
        print(f"警告检查: {summary.get('warning_checks', 0)}")
        print(f"通过率: {summary.get('pass_rate', 0):.1f}%")
        print(f"验证时间: {summary.get('timestamp', 'N/A')}")
        
        print("\n" + "-"*80)
        print("详细检查结果")
        print("-"*80)
        
        # 打印每个类别的检查结果
        for category in ['services', 'endpoints', 'metrics', 'alerts', 'data_flow']:
            if category in report and report[category]:
                print(f"\n{category.upper()}:")
                for check_name, check_result in report[category].items():
                    if isinstance(check_result, dict):
                        status = check_result.get('status', 'UNKNOWN')
                        message = check_result.get('message', '')
                        
                        status_symbol = {
                            'PASSED': '✅',
                            'FAILED': '❌',
                            'WARNING': '⚠️',
                            'SKIPPED': '⏭️',
                            'UNKNOWN': '❓'
                        }.get(status, '❓')
                        
                        print(f"  {status_symbol} {check_name}: {message}")
        
        print("\n" + "-"*80)
        print("建议操作")
        print("-"*80)
        
        overall_status = summary.get('overall_status', 'UNKNOWN')
        if overall_status == 'PASSED':
            print("✅ 所有检查通过，测试监控系统运行正常")
            print("建议: 可以开始使用测试监控系统")
        elif overall_status == 'WARNING':
            print("⚠️  部分检查有警告，测试监控系统基本正常")
            print("建议: 检查警告项，确保关键功能正常")
        elif overall_status == 'FAILED':
            print("❌ 有检查失败，测试监控系统可能有问题")
            print("建议:")
            print("  1. 检查Docker服务是否运行")
            print("  2. 检查端口是否被占用")
            print("  3. 查看服务日志: docker-compose logs")
            print("  4. 重启服务: docker-compose restart")
        else:
            print("❓ 验证结果未知")
            print("建议: 重新运行验证脚本")
        
        print("\n" + "="*80)
        
        # 保存报告到文件
        report_file = 'test_monitoring_validation_report.json'
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"\n详细报告已保存到: {report_file}")

def main():
    """主函数"""
    validator = TestMonitoringValidator()
    
    print("AI-Ready 测试监控系统验证脚本")
    print("版本: 1.0.0")
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 运行验证
    report = validator.validate_all()
    
    # 打印报告
    validator.print_report(report)
    
    # 根据验证结果返回退出码
    overall_status = report.get('overall', 'UNKNOWN')
    if overall_status == 'FAILED':
        sys.exit(1)
    elif overall_status == 'WARNING':
        sys.exit(2)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()