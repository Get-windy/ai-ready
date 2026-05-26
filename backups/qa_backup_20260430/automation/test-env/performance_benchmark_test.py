#!/usr/bin/env python3
"""
性能基准自动化测试脚本
用途: 执行测试环境性能基准自动化测试
作者: qa-lead
版本: 1.0
"""

import os
import sys
import json
import time
import requests
import psycopg2
import redis
from datetime import datetime
from typing import Dict, List, Any
import logging
import threading
import statistics

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('performance_benchmark.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class PerformanceBenchmark:
    """性能基准测试"""
    
    def __init__(self):
        self.results = {}
        self.config = {
            'postgresql': {
                'host': 'localhost',
                'port': 5432,
                'database': 'ai_ready_test',
                'user': 'test_user',
                'password': 'test_password'
            },
            'redis': {
                'host': 'localhost',
                'port': 6379,
                'password': None
            },
            'application': {
                'url': 'http://localhost:8080'
            }
        }
    
    def calculate_percentiles(self, values: List[float]) -> Dict[str, float]:
        """计算响应时间百分位数"""
        sorted_values = sorted(values)
        n = len(sorted_values)
        
        return {
            'p50': sorted_values[n//2] if n > 0 else 0,
            'p95': sorted_values[int(n*0.95)] if n > 0 else 0,
            'p99': sorted_values[int(n*0.99)] if n > 0 else 0,
            'avg': statistics.mean(values) if values else 0,
            'min': min(values) if values else 0,
            'max': max(values) if values else 0
        }
    
    def test_database_performance(self, iterations: int = 1000):
        """P01: PostgreSQL查询性能测试"""
        test_id = "P01"
        logger.info(f"执行 {test_id}: PostgreSQL查询性能测试 ({iterations}次)")
        
        response_times = []
        
        try:
            conn = psycopg2.connect(
                host=self.config['postgresql']['host'],
                port=self.config['postgresql']['port'],
                database=self.config['postgresql']['database'],
                user=self.config['postgresql']['user'],
                password=self.config['postgresql']['password']
            )
            
            cursor = conn.cursor()
            
            for i in range(iterations):
                start_time = time.time()
                
                cursor.execute("SELECT id, username, email FROM users WHERE id = %s", (i % 100 + 1,))
                result = cursor.fetchone()
                
                response_time = time.time() - start_time
                response_times.append(response_time)
            
            cursor.close()
            conn.close()
            
            percentiles = self.calculate_percentiles(response_times)
            
            # 验收标准: P99≤100ms
            passed = percentiles['p99'] * 1000 <= 100
            
            self.results[test_id] = {
                'name': 'PostgreSQL查询性能测试',
                'passed': passed,
                'iterations': iterations,
                'percentiles': {
                    'p50_ms': percentiles['p50'] * 1000,
                    'p95_ms': percentiles['p95'] * 1000,
                    'p99_ms': percentiles['p99'] * 1000,
                    'avg_ms': percentiles['avg'] * 1000,
                    'min_ms': percentiles['min'] * 1000,
                    'max_ms': percentiles['max'] * 1000
                }
            }
            
            if passed:
                logger.info(f"✅ {test_id} 通过: P99={percentiles['p99']*1000:.2f}ms ≤ 100ms")
            else:
                logger.error(f"❌ {test_id} 失败: P99={percentiles['p99']*1000:.2f}ms > 100ms")
        
        except Exception as e:
            logger.error(f"❌ {test_id} 失败: {str(e)}")
            self.results[test_id] = {
                'name': 'PostgreSQL查询性能测试',
                'passed': False,
                'error': str(e)
            }
    
    def test_redis_read_performance(self, iterations: int = 1000):
        """P05: Redis读取性能测试"""
        test_id = "P05"
        logger.info(f"执行 {test_id}: Redis读取性能测试 ({iterations}次)")
        
        response_times = []
        
        try:
            r = redis.Redis(
                host=self.config['redis']['host'],
                port=self.config['redis']['port'],
                password=self.config['redis']['password'],
                decode_responses=True
            )
            
            # 预先写入测试数据
            for i in range(100):
                r.set(f'test_key_{i}', f'test_value_{i}')
            
            for i in range(iterations):
                start_time = time.time()
                
                value = r.get(f'test_key_{i % 100}')
                
                response_time = time.time() - start_time
                response_times.append(response_time)
            
            # 清理测试数据
            for i in range(100):
                r.delete(f'test_key_{i}')
            
            percentiles = self.calculate_percentiles(response_times)
            
            # 验收标准: P99≤5ms
            passed = percentiles['p99'] * 1000 <= 5
            
            self.results[test_id] = {
                'name': 'Redis读取性能测试',
                'passed': passed,
                'iterations': iterations,
                'percentiles': {
                    'p50_ms': percentiles['p50'] * 1000,
                    'p95_ms': percentiles['p95'] * 1000,
                    'p99_ms': percentiles['p99'] * 1000,
                    'avg_ms': percentiles['avg'] * 1000
                }
            }
            
            if passed:
                logger.info(f"✅ {test_id} 通过: P99={percentiles['p99']*1000:.2f}ms ≤ 5ms")
            else:
                logger.error(f"❌ {test_id} 失败: P99={percentiles['p99']*1000:.2f}ms > 5ms")
        
        except Exception as e:
            logger.error(f"❌ {test_id} 失败: {str(e)}")
            self.results[test_id] = {
                'name': 'Redis读取性能测试',
                'passed': False,
                'error': str(e)
            }
    
    def test_api_concurrent_performance(self, concurrent: int = 100, iterations: int = 100):
        """P14: API接口并发性能测试"""
        test_id = "P14"
        logger.info(f"执行 {test_id}: API接口并发性能测试 ({concurrent}并发, {iterations}次)")
        
        all_response_times = []
        all_errors = []
        lock = threading.Lock()
        
        def make_requests(thread_id: int):
            """单线程执行请求"""
            thread_times = []
            thread_errors = 0
            
            for i in range(iterations):
                try:
                    start_time = time.time()
                    response = requests.get(
                        f"{self.config['application']['url']}/api/v1/users",
                        timeout=5
                    )
                    response_time = time.time() - start_time
                    
                    if response.status_code != 200:
                        thread_errors += 1
                    
                    thread_times.append(response_time)
                
                except:
                    thread_errors += 1
            
            with lock:
                all_response_times.extend(thread_times)
                all_errors.append(thread_errors)
        
        try:
            # 创建并启动线程
            threads = []
            for i in range(concurrent):
                thread = threading.Thread(target=make_requests, args=(i,))
                threads.append(thread)
                thread.start()
            
            # 等待所有线程完成
            for thread in threads:
                thread.join()
            
            # 计算结果
            total_requests = concurrent * iterations
            total_errors = sum(all_errors)
            error_rate = (total_errors / total_requests) * 100
            
            percentiles = self.calculate_percentiles(all_response_times)
            
            # 验收标准: 错误率≤1%
            passed = error_rate <= 1
            
            self.results[test_id] = {
                'name': 'API接口并发性能测试',
                'passed': passed,
                'concurrent': concurrent,
                'iterations_per_thread': iterations,
                'total_requests': total_requests,
                'total_errors': total_errors,
                'error_rate': error_rate,
                'percentiles': {
                    'p50_ms': percentiles['p50'] * 1000,
                    'p95_ms': percentiles['p95'] * 1000,
                    'p99_ms': percentiles['p99'] * 1000,
                    'avg_ms': percentiles['avg'] * 1000
                }
            }
            
            if passed:
                logger.info(f"✅ {test_id} 通过: 错误率={error_rate:.2f}% ≤ 1%")
            else:
                logger.error(f"❌ {test_id} 失败: 错误率={error_rate:.2f}% > 1%")
        
        except Exception as e:
            logger.error(f"❌ {test_id} 失败: {str(e)}")
            self.results[test_id] = {
                'name': 'API接口并发性能测试',
                'passed': False,
                'error': str(e)
            }
    
    def generate_report(self) -> Dict:
        """生成性能基准测试报告"""
        passed_count = sum(1 for r in self.results.values() if r.get('passed', False))
        failed_count = len(self.results) - passed_count
        
        report = {
            'test_date': datetime.now().isoformat(),
            'test_executor': 'qa-lead',
            'summary': {
                'total_tests': len(self.results),
                'passed': passed_count,
                'failed': failed_count,
                'pass_rate': (passed_count / len(self.results) * 100) if self.results else 0
            },
            'results': self.results,
            'quality_assessment': self._assess_quality()
        }
        
        # 保存报告
        with open('performance_benchmark_report.json', 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"性能基准测试报告已生成: performance_benchmark_report.json")
        
        return report
    
    def _assess_quality(self) -> Dict:
        """质量评估"""
        if not self.results:
            return {'score': 0, 'grade': 'D', 'status': '无测试结果'}
        
        # 计算质量评分
        passed_rate = sum(1 for r in self.results.values() if r.get('passed', False)) / len(self.results)
        
        # 性能评分 (基于P99响应时间)
        performance_scores = []
        for test_id, result in self.results.items():
            if 'percentiles' in result:
                p99_ms = result['percentiles'].get('p99_ms', 1000)
                
                # 根据验收标准计算得分
                if test_id == 'P01':  # PostgreSQL: P99≤100ms
                    score = max(0, 100 - (p99_ms - 100) / 10)
                elif test_id == 'P05':  # Redis: P99≤5ms
                    score = max(0, 100 - (p99_ms - 5) / 0.5)
                else:  # API: P99≤500ms
                    score = max(0, 100 - (p99_ms - 500) / 50)
                
                performance_scores.append(max(0, min(100, score)))
        
        avg_performance_score = statistics.mean(performance_scores) if performance_scores else 0
        
        # 综合评分
        overall_score = passed_rate * 50 + avg_performance_score * 0.5
        
        # 质量等级
        if overall_score >= 90:
            grade = 'S'
            status = '优秀'
        elif overall_score >= 80:
            grade = 'A'
            status = '良好'
        elif overall_score >= 70:
            grade = 'B'
            status = '合格'
        elif overall_score >= 60:
            grade = 'C'
            status = '待改进'
        else:
            grade = 'D'
            status = '不合格'
        
        return {
            'score': overall_score,
            'grade': grade,
            'status': status,
            'passed_rate': passed_rate * 100,
            'avg_performance_score': avg_performance_score
        }
    
    def run_all_tests(self):
        """执行所有性能基准测试"""
        logger.info("===== 开始执行性能基准测试 =====")
        
        # 数据库性能测试
        self.test_database_performance(1000)
        
        # 缓存性能测试
        self.test_redis_read_performance(1000)
        
        # API并发性能测试
        self.test_api_concurrent_performance(100, 100)
        
        logger.info("===== 性能基准测试完成 =====")
        
        # 生成报告
        return self.generate_report()


def main():
    """主函数"""
    benchmark = PerformanceBenchmark()
    report = benchmark.run_all_tests()
    
    # 打印摘要
    print(f"\n===== 测试摘要 =====")
    print(f"总测试数: {report['summary']['total_tests']}")
    print(f"通过数: {report['summary']['passed']}")
    print(f"失败数: {report['summary']['failed']}")
    print(f"通过率: {report['summary']['pass_rate']:.2f}%")
    print(f"质量评分: {report['quality_assessment']['score']:.2f}")
    print(f"质量等级: {report['quality_assessment']['grade']} ({report['quality_assessment']['status']})")


if __name__ == '__main__':
    main()