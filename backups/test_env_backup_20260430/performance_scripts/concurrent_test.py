"""
Sprint 27+1 测试环境并发用户测试脚本
测试系统在不同并发用户数下的性能表现
"""

import time
import threading
import statistics
import logging
import random
from typing import List, Dict, Any, Optional
from datetime import datetime
import requests
import json
import matplotlib.pyplot as plt
import numpy as np

class ConcurrentTest:
    """并发用户测试类"""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.results_by_concurrency = {}
        self.logger = self._setup_logging()
        
        # 测试场景配置
        self.scenarios = [
            {
                'name': '低并发',
                'concurrent_users': 10,
                'duration': 60,
                'description': '低并发场景，模拟日常使用'
            },
            {
                'name': '中等并发',
                'concurrent_users': 50,
                'duration': 120,
                'description': '中等并发场景，模拟高峰时段'
            },
            {
                'name': '高并发',
                'concurrent_users': 100,
                'duration': 180,
                'description': '高并发场景，模拟促销活动'
            },
            {
                'name': '极限并发',
                'concurrent_users': 200,
                'duration': 240,
                'description': '极限并发场景，压力测试'
            }
        ]
        
        # API端点
        self.endpoints = [
            {
                'name': 'user_api',
                'url': f"{base_url}/api/users",
                'methods': ['GET', 'POST'],
                'weight': 3
            },
            {
                'name': 'order_api',
                'url': f"{base_url}/api/orders",
                'methods': ['GET', 'POST'],
                'weight': 4
            },
            {
                'name': 'inventory_api',
                'url': f"{base_url}/api/inventory/products",
                'methods': ['GET'],
                'weight': 3
            }
        ]
    
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        log_dir = "concurrent_logs"
        import os
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"concurrent_test_{timestamp}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        return logging.getLogger(__name__)
    
    def run_scenario(self, scenario: Dict[str, Any]) -> Dict[str, Any]:
        """运行单个并发场景"""
        concurrent_users = scenario['concurrent_users']
        duration = scenario['duration']
        
        self.logger.info(f"开始场景: {scenario['name']}")
        self.logger.info(f"并发用户: {concurrent_users}, 持续时间: {duration}秒")
        self.logger.info(f"描述: {scenario['description']}")
        
        # 运行测试
        results = self._run_concurrent_test(concurrent_users, duration)
        
        # 计算性能指标
        metrics = self._calculate_metrics(results)
        
        scenario_results = {
            'scenario': scenario,
            'results': results,
            'metrics': metrics
        }
        
        self.results_by_concurrency[concurrent_users] = scenario_results
        
        self.logger.info(f"场景完成: {scenario['name']}")
        self.logger.info(f"  请求数: {len(results)}")
        self.logger.info(f"  成功率: {metrics['success_rate']:.2f}%")
        self.logger.info(f"  平均响应时间: {metrics['avg_response_time']:.2f}ms")
        
        return scenario_results
    
    def _run_concurrent_test(self, concurrent_users: int, duration: int) -> List[Dict[str, Any]]:
        """运行并发测试"""
        results = []
        lock = threading.Lock()
        stop_event = threading.Event()
        
        def user_thread(user_id: int):
            """用户线程函数"""
            start_time = time.time()
            
            while not stop_event.is_set() and time.time() - start_time < duration:
                try:
                    # 随机选择API和HTTP方法
                    endpoint = random.choice(self.endpoints)
                    method = random.choice(endpoint['methods'])
                    
                    # 发送请求
                    request_start = time.time()
                    success = False
                    status_code = 0
                    error = None
                    
                    try:
                        if method == 'GET':
                            response = requests.get(
                                endpoint['url'],
                                params={"page": 1, "size": random.randint(5, 20)},
                                timeout=10
                            )
                        elif method == 'POST':
                            # 生成测试数据
                            test_data = {
                                "test_data": f"concurrent_test_{user_id}_{int(time.time())}",
                                "value": random.randint(1, 100)
                            }
                            response = requests.post(
                                endpoint['url'],
                                json=test_data,
                                timeout=10
                            )
                        
                        status_code = response.status_code
                        success = 200 <= status_code < 300
                        
                        if not success:
                            error = f"HTTP {status_code}"
                            
                    except requests.exceptions.Timeout:
                        error = "请求超时"
                    except requests.exceptions.ConnectionError:
                        error = "连接错误"
                    except Exception as e:
                        error = str(e)
                    
                    request_end = time.time()
                    response_time = (request_end - request_start) * 1000
                    
                    # 记录结果
                    result = {
                        'user_id': user_id,
                        'endpoint': endpoint['name'],
                        'method': method,
                        'success': success,
                        'response_time_ms': response_time,
                        'status_code': status_code,
                        'error': error,
                        'timestamp': datetime.now().isoformat()
                    }
                    
                    with lock:
                        results.append(result)
                    
                    # 随机等待时间
                    think_time = random.uniform(0.5, 2.0)
                    time.sleep(think_time)
                    
                except Exception as e:
                    self.logger.error(f"用户 {user_id} 线程错误: {e}")
                    time.sleep(1)
        
        # 创建并启动线程
        threads = []
        for i in range(concurrent_users):
            thread = threading.Thread(target=user_thread, args=(i,))
            thread.daemon = True
            thread.start()
            threads.append(thread)
        
        # 等待测试完成
        time.sleep(duration)
        
        # 停止所有线程
        stop_event.set()
        for thread in threads:
            thread.join(timeout=5)
        
        return results
    
    def _calculate_metrics(self, results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """计算性能指标"""
        if not results:
            return {}
        
        total_requests = len(results)
        successful_requests = sum(1 for r in results if r['success'])
        failed_requests = total_requests - successful_requests
        success_rate = (successful_requests / total_requests * 100) if total_requests > 0 else 0
        
        # 响应时间统计
        successful_times = [r['response_time_ms'] for r in results if r['success']]
        if successful_times:
            avg_response_time = statistics.mean(successful_times)
            p50_response_time = statistics.median(successful_times)
            p90_response_time = statistics.quantiles(successful_times, n=10)[-1] if len(successful_times) >= 10 else avg_response_time
            p95_response_time = statistics.quantiles(successful_times, n=20)[-1] if len(successful_times) >= 20 else avg_response_time
            max_response_time = max(successful_times)
            min_response_time = min(successful_times)
        else:
            avg_response_time = p50_response_time = p90_response_time = p95_response_time = 0
            max_response_time = min_response_time = 0
        
        # 吞吐量计算
        if results:
            timestamps = [datetime.fromisoformat(r['timestamp'].replace('Z', '+00:00')) for r in results]
            start_time = min(timestamps)
            end_time = max(timestamps)
            duration_seconds = (end_time - start_time).total_seconds()
            
            if duration_seconds > 0:
                throughput = total_requests / duration_seconds
            else:
                throughput = 0
        else:
            throughput = 0
        
        # 错误分析
        errors = {}
        for result in results:
            if not result['success'] and result['error']:
                error_type = result['error']
                if error_type not in errors:
                    errors[error_type] = 0
                errors[error_type] += 1
        
        # 按端点统计
        endpoints_stats = {}
        for result in results:
            endpoint = result['endpoint']
            if endpoint not in endpoints_stats:
                endpoints_stats[endpoint] = {
                    'total': 0,
                    'success': 0,
                    'response_times': []
                }
            
            endpoints_stats[endpoint]['total'] += 1
            if result['success']:
                endpoints_stats[endpoint]['success'] += 1
                endpoints_stats[endpoint]['response_times'].append(result['response_time_ms'])
        
        return {
            'total_requests': total_requests,
            'successful_requests': successful_requests,
            'failed_requests': failed_requests,
            'success_rate': success_rate,
            'avg_response_time': avg_response_time,
            'p50_response_time': p50_response_time,
            'p90_response_time': p90_response_time,
            'p95_response_time': p95_response_time,
            'max_response_time': max_response_time,
            'min_response_time': min_response_time,
            'throughput': throughput,
            'errors': errors,
            'endpoints_stats': endpoints_stats
        }
    
    def run_all_scenarios(self) -> Dict[int, Dict[str, Any]]:
        """运行所有并发场景"""
        self.logger.info("=" * 60)
        self.logger.info("开始并发用户测试")
        self.logger.info(f"测试地址: {self.base_url}")
        self.logger.info(f"测试场景数: {len(self.scenarios)}")
        self.logger.info("=" * 60)
        
        all_results = {}
        
        for scenario in self.scenarios:
            try:
                results = self.run_scenario(scenario)
                all_results[scenario['concurrent_users']] = results
                
                # 场景间休息
                if scenario != self.scenarios[-1]:
                    self.logger.info(f"场景间休息 10秒...")
                    time.sleep(10)
                    
            except Exception as e:
                self.logger.error(f"场景 {scenario['name']} 执行失败: {e}")
                continue
        
        return all_results
    
    def generate_comparison_report(self) -> str:
        """生成并发测试对比报告"""
        if not self.results_by_concurrency:
            return "没有测试结果"
        
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        report = f"""
并发用户测试对比报告
=====================
测试时间: {timestamp}
测试地址: {self.base_url}

并发场景对比:
"""
        
        # 创建对比表格
        concurrency_levels = sorted(self.results_by_concurrency.keys())
        
        # 表头
        report += "\n"
        report += "并发用户 | 请求数 | 成功率(%) | 平均响应时间(ms) | P95响应时间(ms) | 吞吐量(请求/秒) | 错误数\n"
        report += "-" * 100 + "\n"
        
        for concurrency in concurrency_levels:
            results = self.results_by_concurrency[concurrency]
            metrics = results['metrics']
            
            total_requests = metrics['total_requests']
            success_rate = metrics['success_rate']
            avg_response_time = metrics['avg_response_time']
            p95_response_time = metrics['p95_response_time']
            throughput = metrics['throughput']
            failed_requests = metrics['failed_requests']
            
            report += f"{concurrency:^10} | {total_requests:^8} | {success_rate:^10.2f} | {avg_response_time:^16.2f} | {p95_response_time:^16.2f} | {throughput:^16.2f} | {failed_requests:^8}\n"
        
        # 性能趋势分析
        report += "\n性能趋势分析:\n"
        report += "-" * 60 + "\n"
        
        # 收集趋势数据
        concurrency_list = []
        success_rates = []
        avg_response_times = []
        throughputs = []
        
        for concurrency in concurrency_levels:
            metrics = self.results_by_concurrency[concurrency]['metrics']
            
            concurrency_list.append(concurrency)
            success_rates.append(metrics['success_rate'])
            avg_response_times.append(metrics['avg_response_time'])
            throughputs.append(metrics['throughput'])
        
        # 成功率趋势
        report += "1. 成功率趋势:\n"
        for i, concurrency in enumerate(concurrency_list):
            success_rate = success_rates[i]
            if i == 0:
                report += f"   {concurrency}并发: {success_rate:.2f}% (基准)\n"
            else:
                prev_success_rate = success_rates[i-1]
                change = success_rate - prev_success_rate
                change_str = f"↑{change:.2f}%" if change > 0 else f"↓{abs(change):.2f}%"
                report += f"   {concurrency}并发: {success_rate:.2f}% ({change_str})\n"
        
        # 响应时间趋势
        report += "\n2. 响应时间趋势:\n"
        for i, concurrency in enumerate(concurrency_list):
            response_time = avg_response_times[i]
            if i == 0:
                report += f"   {concurrency}并发: {response_time:.2f}ms (基准)\n"
            else:
                prev_response_time = avg_response_times[i-1]
                change_pct = ((response_time - prev_response_time) / prev_response_time * 100) if prev_response_time > 0 else 0
                change_str = f"↑{change_pct:.1f}%" if change_pct > 0 else f"↓{abs(change_pct):.1f}%"
                report += f"   {concurrency}并发: {response_time:.2f}ms ({change_str})\n"
        
        # 吞吐量趋势
        report += "\n3. 吞吐量趋势:\n"
        for i, concurrency in enumerate(concurrency_list):
            throughput = throughputs[i]
            if i == 0:
                report += f"   {concurrency}并发: {throughput:.2f} 请求/秒 (基准)\n"
            else:
                prev_throughput = throughputs[i-1]
                change_pct = ((throughput - prev_throughput) / prev_throughput * 100) if prev_throughput > 0 else 0
                change_str = f"↑{change_pct:.1f}%" if change_pct > 0 else f"↓{abs(change_pct):.1f}%"
                report += f"   {concurrency}并发: {throughput:.2f} 请求/秒 ({change_str})\n"
        
        # 系统容量评估
        report += "\n系统容量评估:\n"
        report += "-" * 60 + "\n"
        
        # 找到性能拐点
        performance_degradation_point = None
        for i in range(1, len(concurrency_list)):
            success_rate = success_rates[i]
            prev_success_rate = success_rates[i-1]
            
            if success_rate < 95 and success_rate < prev_success_rate - 5:
                performance_degradation_point = concurrency_list[i]
                break
        
        if performance_degradation_point:
            report += f"⚠️  性能拐点: {performance_degradation_point} 并发用户\n"
            report += f"   在{performance_degradation_point}并发时，系统性能开始显著下降\n"
        else:
            report += "✅ 在测试范围内未发现明显性能拐点\n"
        
        # 推荐并发数
        max_concurrency = max(concurrency_list)
        max_success_rate = max(success_rates)
        
        # 找到成功率≥99%的最大并发数
        recommended_concurrency = None
        for concurrency, success_rate in zip(concurrency_list, success_rates):
            if success_rate >= 99:
                recommended_concurrency = concurrency
        
        if recommended_concurrency:
            report += f"✅ 推荐并发数: {recommended_concurrency}\n"
            report += f"   在此并发下，系统能保持99%以上的成功率\n"
        else:
            # 找到成功率≥95%的最大并发数
            for concurrency, success_rate in zip(concurrency_list, success_rates):
                if success_rate >= 95:
                    recommended_concurrency = concurrency
            
            if recommended_concurrency:
                report += f"⚠️  推荐并发数: {recommended_concurrency}\n"
                report += f"   在此并发下，系统能保持95%以上的成功率\n"
            else:
                report += "❌ 系统在测试并发范围内无法保持95%以上的成功率\n"
        
        # 系统瓶颈分析
        report += "\n系统瓶颈分析:\n"
        report += "-" * 60 + "\n"
        
        # 分析错误类型
        all_errors = {}
        for concurrency in concurrency_levels:
            errors = self.results_by_concurrency[concurrency]['metrics']['errors']
            for error_type, count in errors.items():
                if error_type not in all_errors:
                    all_errors[error_type] = 0
                all_errors[error_type] += count
        
        if all_errors:
            report += "主要错误类型:\n"
            for error_type, total_count in sorted(all_errors.items(), key=lambda x: x[1], reverse=True)[:5]:
                report += f"  • {error_type}: {total_count}次\n"
            
            # 根据错误类型给出建议
            if "请求超时" in all_errors:
                report += "\n建议:\n"
                report += "  1. 优化数据库查询，减少响应时间\n"
                report += "  2. 增加服务器资源或实施负载均衡\n"
                report += "  3. 实施请求队列和限流策略\n"
            
            if "连接错误" in all_errors:
                report += "\n建议:\n"
                report += "  1. 检查服务器连接池配置\n"
                report += "  2. 增加服务器最大连接数\n"
                report += "  3. 优化网络配置\n"
        else:
            report += "✅ 测试期间未出现系统错误\n"
        
        # 扩展性评估
        report += "\n系统扩展性评估:\n"
        report += "-" * 60 + "\n"
        
        # 计算扩展效率
        if len(concurrency_list) >= 2:
            first_concurrency = concurrency_list[0]
            last_concurrency = concurrency_list[-1]
            
            first_throughput = throughputs[0]
            last_throughput = throughputs[-1]
            
            concurrency_ratio = last_concurrency / first_concurrency
            throughput_ratio = last_throughput / first_throughput if first_throughput > 0 else 0
            
            scalability_efficiency = (throughput_ratio / concurrency_ratio * 100) if concurrency_ratio > 0 else 0
            
            report += f"并发增长: {first_concurrency} → {last_concurrency} ({concurrency_ratio:.1f}倍)\n"
            report += f"吞吐量增长: {first_throughput:.1f} → {last_throughput:.1f} ({throughput_ratio:.1f}倍)\n"
            report += f"扩展效率: {scalability_efficiency:.1f}%\n"
            
            if scalability_efficiency >= 80:
                report += "✅ 系统扩展性优秀\n"
            elif scalability_efficiency >= 60:
                report += "✅ 系统扩展性良好\n"
            elif scalability_efficiency >= 40:
                report += "⚠️  系统扩展性一般\n"
            else:
                report += "❌ 系统扩展性差，存在瓶颈\n"
        
        # 生成图表
        self._generate_charts()
        
        report += f"\n详细图表已生成: concurrent_test_charts.png\n"
        report += f"详细数据已保存到结果文件\n"
        
        return report
    
    def _generate_charts(self):
        """生成性能对比图表"""
        if not self.results_by_concurrency:
            return
        
        concurrency_levels = sorted(self.results_by_concurrency.keys())
        
        # 准备数据
        success_rates = []
        avg_response_times = []
        p95_response_times = []
        throughputs = []
        
        for concurrency in concurrency_levels:
            metrics = self.results_by_concurrency[concurrency]['metrics']
            success_rates.append(metrics['success_rate'])
            avg_response_times.append(metrics['avg_response_time'])
            p95_response_times.append(metrics['p95_response_time'])
            throughputs.append(metrics['throughput'])
        
        # 创建图表
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        fig.suptitle(f'并发用户测试性能对比 - {self.base_url}', fontsize=16)
        
        # 1. 成功率 vs 并发用户数
        ax1 = axes[0, 0]
        ax1.plot(concurrency_levels, success_rates, 'bo-', linewidth=2, markersize=8)
        ax1.set_xlabel('并发用户数')
        ax1.set_ylabel('成功率 (%)')
        ax1.set_title('成功率 vs 并发用户数')
        ax1.grid(True, alpha=0.3)
        ax1.axhline(y=95, color='r', linestyle='--', alpha=0.5, label='95% 阈值')
        ax1.legend()
        
        # 2. 平均响应时间 vs 并发用户数
        ax2 = axes[0, 1]
        ax2.plot(concurrency_levels, avg_response_times, 'go-', linewidth=2, markersize=8)
        ax2.set_xlabel('并发用户数')
        ax2.set_ylabel('平均响应时间 (ms)')
        ax2.set_title('平均响应时间 vs 并发用户数')
        ax2.grid(True, alpha=0.3)
        ax2.axhline(y=1000, color='r', linestyle='--', alpha=0.5, label='1秒阈值')
        ax2.legend()
        
        # 3. P95响应时间 vs 并发用户数
        ax3 = axes[1, 0]
        ax3.plot(concurrency_levels, p95_response_times, 'ro-', linewidth=2, markersize=8)
        ax3.set_xlabel('并发用户数')
        ax3.set_ylabel('P95响应时间 (ms)')
        ax3.set_title('P95响应时间 vs 并发用户数')
        ax3.grid(True, alpha=0.3)
        ax3.axhline(y=2000, color='r', linestyle='--', alpha=0.5, label='2秒阈值')
        ax3.legend()
        
        # 4. 吞吐量 vs 并发用户数
        ax4 = axes[1, 1]
        ax4.plot(concurrency_levels, throughputs, 'mo-', linewidth=2, markersize=8)
        ax4.set_xlabel('并发用户数')
        ax4.set_ylabel('吞吐量 (请求/秒)')
        ax4.set_title('吞吐量 vs 并发用户数')
        ax4.grid(True, alpha=0.3)
        
        # 调整布局
        plt.tight_layout()
        
        # 保存图表
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        chart_file = f"concurrent_test_charts_{timestamp}.png"
        plt.savefig(chart_file, dpi=300, bbox_inches='tight')
        plt.close()
        
        self.logger.info(f"性能图表已保存到: {chart_file}")
    
    def save_results(self):
        """保存测试结果到文件"""
        import json
        import os
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_dir = "concurrent_test_results"
        os.makedirs(results_dir, exist_ok=True)
        
        # 保存详细结果
        results_file = os.path.join(results_dir, f"concurrent_test_results_{timestamp}.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump({
                'config': {
                    'base_url': self.base_url,
                    'scenarios': self.scenarios
                },
                'results_by_concurrency': self.results_by_concurrency
            }, f, ensure_ascii=False, indent=2)
        
        # 生成并保存报告
        report = self.generate_comparison_report()
        report_file = os.path.join(results_dir, f"concurrent_test_report_{timestamp}.txt")
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        self.logger.info(f"测试结果已保存到: {results_file}")
        self.logger.info(f"测试报告已保存到: {report_file}")

def main():
    """主函数"""
    import sys
    
    # 默认参数
    base_url = "http://localhost:8080"
    
    # 解析命令行参数
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    # 运行并发测试
    try:
        concurrent_test = ConcurrentTest(base_url)
        all_results = concurrent_test.run_all_scenarios()
        
        # 生成对比报告
        report = concurrent_test.generate_comparison_report()
        
        # 保存结果
        concurrent_test.save_results()
        
        print("\n" + "="*60)
        print("并发用户测试完成!")
        print("="*60)
        print(report)
        
    except KeyboardInterrupt:
        print("\n并发测试被用户中断")
    except Exception as e:
        print(f"并发测试失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 检查matplotlib是否可用
    try:
        import matplotlib
        matplotlib.use('Agg')  # 使用非交互式后端
        main()
    except ImportError:
        print("警告: matplotlib 未安装，图表功能不可用")
        print("安装命令: pip install matplotlib")
        main()  # 仍然运行测试，只是不生成图表