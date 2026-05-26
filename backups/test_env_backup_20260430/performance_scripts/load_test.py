"""
Sprint 27+1 测试环境负载测试脚本
测试系统在高并发下的性能表现
"""

import time
import threading
import statistics
import logging
from typing import List, Dict, Any
from datetime import datetime
import requests

class LoadTest:
    """负载测试类"""
    
    def __init__(self, base_url: str, concurrent_users: int = 10, duration: int = 60):
        self.base_url = base_url
        self.concurrent_users = concurrent_users
        self.duration = duration
        self.results = []
        self.lock = threading.Lock()
        self.logger = self._setup_logging()
        
        # API端点
        self.endpoints = {
            'users': f"{base_url}/api/users",
            'orders': f"{base_url}/api/orders",
            'inventory': f"{base_url}/api/inventory/products"
        }
    
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        return logging.getLogger(__name__)
    
    def test_user_api(self, user_id: int) -> Dict[str, Any]:
        """测试用户API"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        
        try:
            # 测试获取用户列表
            response = requests.get(
                self.endpoints['users'],
                params={"page": 1, "size": 10},
                timeout=10
            )
            status_code = response.status_code
            success = response.status_code == 200
            
            if not success:
                error = f"状态码: {status_code}"
                
        except requests.exceptions.Timeout:
            error = "请求超时"
        except requests.exceptions.ConnectionError:
            error = "连接错误"
        except Exception as e:
            error = str(e)
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000  # 转换为毫秒
        
        return {
            'user_id': user_id,
            'endpoint': 'users',
            'success': success,
            'response_time_ms': response_time,
            'status_code': status_code,
            'error': error,
            'timestamp': datetime.now().isoformat()
        }
    
    def test_order_api(self, user_id: int) -> Dict[str, Any]:
        """测试订单API"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        
        try:
            # 测试获取订单列表
            response = requests.get(
                self.endpoints['orders'],
                params={"page": 1, "size": 5},
                timeout=10
            )
            status_code = response.status_code
            success = response.status_code == 200
            
            if not success:
                error = f"状态码: {status_code}"
                
        except requests.exceptions.Timeout:
            error = "请求超时"
        except requests.exceptions.ConnectionError:
            error = "连接错误"
        except Exception as e:
            error = str(e)
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000
        
        return {
            'user_id': user_id,
            'endpoint': 'orders',
            'success': success,
            'response_time_ms': response_time,
            'status_code': status_code,
            'error': error,
            'timestamp': datetime.now().isoformat()
        }
    
    def test_inventory_api(self, user_id: int) -> Dict[str, Any]:
        """测试库存API"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        
        try:
            # 测试获取产品列表
            response = requests.get(
                self.endpoints['inventory'],
                params={"page": 1, "size": 20},
                timeout=10
            )
            status_code = response.status_code
            success = response.status_code == 200
            
            if not success:
                error = f"状态码: {status_code}"
                
        except requests.exceptions.Timeout:
            error = "请求超时"
        except requests.exceptions.ConnectionError:
            error = "连接错误"
        except Exception as e:
            error = str(e)
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000
        
        return {
            'user_id': user_id,
            'endpoint': 'inventory',
            'success': success,
            'response_time_ms': response_time,
            'status_code': status_code,
            'error': error,
            'timestamp': datetime.now().isoformat()
        }
    
    def virtual_user(self, user_id: int, stop_event: threading.Event):
        """虚拟用户线程"""
        while not stop_event.is_set():
            # 随机选择一个API进行测试
            import random
            test_func = random.choice([
                self.test_user_api,
                self.test_order_api,
                self.test_inventory_api
            ])
            
            result = test_func(user_id)
            
            with self.lock:
                self.results.append(result)
            
            # 随机等待时间（0.5-2秒）
            time.sleep(random.uniform(0.5, 2.0))
    
    def run_test(self):
        """运行负载测试"""
        self.logger.info(f"开始负载测试: {self.concurrent_users}并发用户, 持续{self.duration}秒")
        self.logger.info(f"测试地址: {self.base_url}")
        
        # 创建停止事件
        stop_event = threading.Event()
        
        # 创建并启动虚拟用户线程
        threads = []
        for i in range(self.concurrent_users):
            thread = threading.Thread(
                target=self.virtual_user,
                args=(i, stop_event)
            )
            thread.daemon = True
            thread.start()
            threads.append(thread)
        
        # 运行指定时间
        self.logger.info(f"测试运行中... (将持续{self.duration}秒)")
        time.sleep(self.duration)
        
        # 停止测试
        stop_event.set()
        for thread in threads:
            thread.join(timeout=5)
        
        self.logger.info("负载测试完成")
        
        # 生成报告
        report = self.generate_report()
        self.logger.info("\n" + report)
        
        # 保存结果
        self.save_results()
        
        return report
    
    def generate_report(self) -> str:
        """生成测试报告"""
        if not self.results:
            return "没有测试结果"
        
        total_requests = len(self.results)
        successful_requests = sum(1 for r in self.results if r['success'])
        failed_requests = total_requests - successful_requests
        success_rate = (successful_requests / total_requests * 100) if total_requests > 0 else 0
        
        # 按端点统计
        endpoints_stats = {}
        for result in self.results:
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
        
        # 计算响应时间统计
        all_response_times = [r['response_time_ms'] for r in self.results if r['success']]
        if all_response_times:
            avg_response_time = statistics.mean(all_response_times)
            p95_response_time = statistics.quantiles(all_response_times, n=20)[-1]  # 95th percentile
            max_response_time = max(all_response_times)
            min_response_time = min(all_response_times)
        else:
            avg_response_time = p95_response_time = max_response_time = min_response_time = 0
        
        # 生成报告
        report = f"""
负载测试报告
===============
测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
测试配置:
  并发用户数: {self.concurrent_users}
  测试时长: {self.duration}秒
  测试地址: {self.base_url}

总体统计:
  总请求数: {total_requests}
  成功请求: {successful_requests}
  失败请求: {failed_requests}
  成功率: {success_rate:.2f}%
  平均响应时间: {avg_response_time:.2f}ms
  P95响应时间: {p95_response_time:.2f}ms
  最大响应时间: {max_response_time:.2f}ms
  最小响应时间: {min_response_time:.2f}ms

各端点性能统计:
"""
        
        for endpoint, stats in endpoints_stats.items():
            endpoint_total = stats['total']
            endpoint_success = stats['success']
            endpoint_success_rate = (endpoint_success / endpoint_total * 100) if endpoint_total > 0 else 0
            
            if stats['response_times']:
                endpoint_avg = statistics.mean(stats['response_times'])
                endpoint_p95 = statistics.quantiles(stats['response_times'], n=20)[-1] if len(stats['response_times']) >= 20 else endpoint_avg
            else:
                endpoint_avg = endpoint_p95 = 0
            
            report += f"""
  {endpoint}:
    请求数: {endpoint_total}
    成功率: {endpoint_success_rate:.2f}%
    平均响应时间: {endpoint_avg:.2f}ms
    P95响应时间: {endpoint_p95:.2f}ms
"""
        
        # 错误分析
        errors = {}
        for result in self.results:
            if not result['success'] and result['error']:
                error_type = result['error']
                if error_type not in errors:
                    errors[error_type] = 0
                errors[error_type] += 1
        
        if errors:
            report += "\n错误分析:\n"
            for error_type, count in errors.items():
                report += f"  {error_type}: {count}次\n"
        
        # 性能评估
        report += "\n性能评估:\n"
        if success_rate < 95:
            report += "  ❌ 成功率低于95%，系统稳定性需要改进\n"
        else:
            report += "  ✅ 成功率良好\n"
        
        if avg_response_time > 1000:
            report += "  ❌ 平均响应时间超过1秒，性能需要优化\n"
        elif avg_response_time > 500:
            report += "  ⚠️ 平均响应时间超过500ms，建议优化\n"
        else:
            report += "  ✅ 响应时间良好\n"
        
        if p95_response_time > 2000:
            report += "  ❌ P95响应时间超过2秒，用户体验可能受影响\n"
        elif p95_response_time > 1000:
            report += "  ⚠️ P95响应时间超过1秒，建议关注\n"
        else:
            report += "  ✅ P95响应时间良好\n"
        
        return report
    
    def save_results(self):
        """保存测试结果到文件"""
        import json
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"load_test_results_{timestamp}.json"
        
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(self.results, f, ensure_ascii=False, indent=2)
            
            self.logger.info(f"测试结果已保存到: {filename}")
            
        except Exception as e:
            self.logger.error(f"保存测试结果失败: {e}")

def main():
    """主函数"""
    import sys
    
    # 参数解析
    base_url = "http://localhost:8080"
    concurrent_users = 10
    duration = 60
    
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    if len(sys.argv) > 2:
        try:
            concurrent_users = int(sys.argv[2])
        except ValueError:
            print(f"无效的并发用户数: {sys.argv[2]}")
            return
    
    if len(sys.argv) > 3:
        try:
            duration = int(sys.argv[3])
        except ValueError:
            print(f"无效的测试时长: {sys.argv[3]}")
            return
    
    # 运行负载测试
    try:
        load_test = LoadTest(base_url, concurrent_users, duration)
        report = load_test.run_test()
        
        # 保存报告
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_filename = f"load_test_report_{timestamp}.txt"
        
        with open(report_filename, 'w', encoding='utf-8') as f:
            f.write(report)
        
        print(f"\n详细报告已保存到: {report_filename}")
        
    except KeyboardInterrupt:
        print("\n测试被用户中断")
    except Exception as e:
        print(f"负载测试失败: {e}")

if __name__ == "__main__":
    main()