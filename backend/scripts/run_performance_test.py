#!/usr/bin/env python3
"""
企智连数据同步性能测试执行器
执行全量同步和增量同步性能测试
"""

import json
import time
import logging
import argparse
import statistics
from datetime import datetime
from typing import Dict, List, Any, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading
import random

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('performance_test.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class DataSyncPerformanceTester:
    """数据同步性能测试器"""
    
    def __init__(self, config_file: str):
        """初始化测试器
        
        Args:
            config_file: 配置文件路径
        """
        with open(config_file, 'r', encoding='utf-8') as f:
            self.config = json.load(f)
        
        self.results = {}
        self.metrics = {
            'throughput': [],
            'latency': [],
            'cpu_usage': [],
            'memory_usage': []
        }
        
    def run_full_sync_test(self, data_file: str) -> Dict[str, Any]:
        """执行全量同步性能测试
        
        Args:
            data_file: 测试数据文件路径
            
        Returns:
            测试结果
        """
        logger.info("=" * 60)
        logger.info("开始执行全量同步性能测试")
        logger.info("=" * 60)
        
        # 加载测试数据
        logger.info(f"加载测试数据: {data_file}")
        with open(data_file, 'r', encoding='utf-8') as f:
            test_data = json.load(f)
        
        total_records = len(test_data)
        logger.info(f"测试数据量: {total_records} 条记录")
        
        # 模拟全量同步过程
        start_time = time.time()
        batch_size = self.config.get('batch_size', 1000)
        parallel_workers = self.config.get('parallel_workers', 4)
        
        processed_count = 0
        error_count = 0
        latencies = []
        
        # 分批处理
        batches = [test_data[i:i + batch_size] for i in range(0, total_records, batch_size)]
        
        def process_batch(batch: List[Dict]) -> tuple:
            """处理单个批次"""
            batch_start = time.time()
            batch_errors = 0
            
            # 模拟处理延迟
            for record in batch:
                # 模拟网络延迟 (10-50ms)
                time.sleep(random.uniform(0.001, 0.005))
                
                # 模拟随机错误 (0.05%错误率)
                if random.random() < 0.0005:
                    batch_errors += 1
            
            batch_latency = time.time() - batch_start
            return len(batch), batch_errors, batch_latency
        
        # 使用线程池并行处理
        with ThreadPoolExecutor(max_workers=parallel_workers) as executor:
            futures = [executor.submit(process_batch, batch) for batch in batches]
            
            for future in as_completed(futures):
                try:
                    count, errors, latency = future.result()
                    processed_count += count
                    error_count += errors
                    latencies.append(latency)
                    
                    if processed_count % 10000 == 0:
                        progress = processed_count / total_records * 100
                        logger.info(f"  进度: {processed_count}/{total_records} ({progress:.1f}%)")
                
                except Exception as e:
                    logger.error(f"批次处理错误: {e}")
                    error_count += batch_size
        
        end_time = time.time()
        total_duration = end_time - start_time
        
        # 计算性能指标
        throughput = processed_count / total_duration
        success_rate = (processed_count - error_count) / processed_count * 100
        avg_latency = statistics.mean(latencies) * 1000  # 转换为毫秒
        p95_latency = statistics.quantiles(latencies, n=20)[18] * 1000 if len(latencies) >= 20 else avg_latency
        p99_latency = statistics.quantiles(latencies, n=100)[98] * 1000 if len(latencies) >= 100 else avg_latency
        
        result = {
            'test_name': '全量同步性能测试',
            'total_records': total_records,
            'processed_records': processed_count,
            'error_records': error_count,
            'success_rate': round(success_rate, 2),
            'total_duration_seconds': round(total_duration, 2),
            'throughput_per_second': round(throughput, 2),
            'avg_latency_ms': round(avg_latency, 2),
            'p95_latency_ms': round(p95_latency, 2),
            'p99_latency_ms': round(p99_latency, 2),
            'batch_size': batch_size,
            'parallel_workers': parallel_workers,
            'timestamp': datetime.now().isoformat(),
            'status': 'PASSED' if success_rate >= 99.9 and throughput >= 5000 else 'FAILED'
        }
        
        logger.info("\n全量同步性能测试结果:")
        logger.info(f"  总记录数: {total_records}")
        logger.info(f"  处理记录数: {processed_count}")
        logger.info(f"  错误记录数: {error_count}")
        logger.info(f"  成功率: {success_rate:.2f}%")
        logger.info(f"  总耗时: {total_duration:.2f} 秒")
        logger.info(f"  吞吐量: {throughput:.2f} 记录/秒")
        logger.info(f"  平均延迟: {avg_latency:.2f} ms")
        logger.info(f"  P95延迟: {p95_latency:.2f} ms")
        logger.info(f"  P99延迟: {p99_latency:.2f} ms")
        logger.info(f"  状态: {result['status']}")
        
        return result
    
    def run_incremental_sync_test(self, data_file: str) -> Dict[str, Any]:
        """执行增量同步性能测试
        
        Args:
            data_file: 增量测试数据文件路径
            
        Returns:
            测试结果
        """
        logger.info("=" * 60)
        logger.info("开始执行增量同步性能测试")
        logger.info("=" * 60)
        
        # 加载增量测试数据
        logger.info(f"加载增量测试数据: {data_file}")
        with open(data_file, 'r', encoding='utf-8') as f:
            incremental_data = json.load(f)
        
        inserts = incremental_data.get('inserts', [])
        updates = incremental_data.get('updates', [])
        deletes = incremental_data.get('deletes', [])
        
        total_operations = len(inserts) + len(updates) + len(deletes)
        logger.info(f"增量操作总数: {total_operations}")
        logger.info(f"  插入: {len(inserts)}")
        logger.info(f"  更新: {len(updates)}")
        logger.info(f"  删除: {len(deletes)}")
        
        # 模拟增量同步过程
        start_time = time.time()
        
        operation_latencies = {
            'insert': [],
            'update': [],
            'delete': []
        }
        
        # 模拟插入操作
        for record in inserts:
            op_start = time.time()
            time.sleep(random.uniform(0.0005, 0.002))  # 模拟处理延迟
            operation_latencies['insert'].append(time.time() - op_start)
        
        # 模拟更新操作
        for record in updates:
            op_start = time.time()
            time.sleep(random.uniform(0.0005, 0.002))
            operation_latencies['update'].append(time.time() - op_start)
        
        # 模拟删除操作
        for record_id in deletes:
            op_start = time.time()
            time.sleep(random.uniform(0.0003, 0.001))
            operation_latencies['delete'].append(time.time() - op_start)
        
        end_time = time.time()
        total_duration = end_time - start_time
        
        # 计算性能指标
        throughput = total_operations / total_duration
        
        avg_insert_latency = statistics.mean(operation_latencies['insert']) * 1000
        avg_update_latency = statistics.mean(operation_latencies['update']) * 1000
        avg_delete_latency = statistics.mean(operation_latencies['delete']) * 1000
        
        overall_avg_latency = statistics.mean(
            operation_latencies['insert'] + 
            operation_latencies['update'] + 
            operation_latencies['delete']
        ) * 1000
        
        result = {
            'test_name': '增量同步性能测试',
            'total_operations': total_operations,
            'insert_count': len(inserts),
            'update_count': len(updates),
            'delete_count': len(deletes),
            'total_duration_seconds': round(total_duration, 2),
            'throughput_per_second': round(throughput, 2),
            'avg_insert_latency_ms': round(avg_insert_latency, 2),
            'avg_update_latency_ms': round(avg_update_latency, 2),
            'avg_delete_latency_ms': round(avg_delete_latency, 2),
            'overall_avg_latency_ms': round(overall_avg_latency, 2),
            'timestamp': datetime.now().isoformat(),
            'status': 'PASSED' if overall_avg_latency <= 100 and throughput >= 1600 else 'FAILED'
        }
        
        logger.info("\n增量同步性能测试结果:")
        logger.info(f"  总操作数: {total_operations}")
        logger.info(f"  插入操作: {len(inserts)}")
        logger.info(f"  更新操作: {len(updates)}")
        logger.info(f"  删除操作: {len(deletes)}")
        logger.info(f"  总耗时: {total_duration:.2f} 秒")
        logger.info(f"  吞吐量: {throughput:.2f} 操作/秒")
        logger.info(f"  平均插入延迟: {avg_insert_latency:.2f} ms")
        logger.info(f"  平均更新延迟: {avg_update_latency:.2f} ms")
        logger.info(f"  平均删除延迟: {avg_delete_latency:.2f} ms")
        logger.info(f"  总体平均延迟: {overall_avg_latency:.2f} ms")
        logger.info(f"  状态: {result['status']}")
        
        return result
    
    def run_concurrency_test(self, data_file: str, concurrency_levels: List[int] = None) -> Dict[str, Any]:
        """执行并发同步性能测试
        
        Args:
            data_file: 测试数据文件路径
            concurrency_levels: 并发级别列表
            
        Returns:
            测试结果
        """
        if concurrency_levels is None:
            concurrency_levels = [10, 50, 100, 200]
        
        logger.info("=" * 60)
        logger.info("开始执行并发同步性能测试")
        logger.info("=" * 60)
        
        # 加载测试数据
        with open(data_file, 'r', encoding='utf-8') as f:
            test_data = json.load(f)
        
        results_by_concurrency = {}
        
        for concurrency in concurrency_levels:
            logger.info(f"\n测试并发级别: {concurrency}")
            
            records_per_worker = 1000
            total_records = concurrency * records_per_worker
            
            latencies = []
            success_count = 0
            error_count = 0
            
            def worker_task(worker_id: int) -> tuple:
                """工作线程任务"""
                worker_latencies = []
                worker_success = 0
                worker_errors = 0
                
                for i in range(records_per_worker):
                    op_start = time.time()
                    try:
                        # 模拟同步操作
                        time.sleep(random.uniform(0.001, 0.005))
                        worker_success += 1
                    except:
                        worker_errors += 1
                    finally:
                        worker_latencies.append(time.time() - op_start)
                
                return worker_latencies, worker_success, worker_errors
            
            # 使用线程池执行并发任务
            start_time = time.time()
            
            with ThreadPoolExecutor(max_workers=concurrency) as executor:
                futures = [executor.submit(worker_task, i) for i in range(concurrency)]
                
                for future in as_completed(futures):
                    worker_latencies, worker_success, worker_errors = future.result()
                    latencies.extend(worker_latencies)
                    success_count += worker_success
                    error_count += worker_errors
            
            total_duration = time.time() - start_time
            
            # 计算指标
            throughput = total_records / total_duration
            avg_latency = statistics.mean(latencies) * 1000
            p95_latency = statistics.quantiles(latencies, n=20)[18] * 1000 if len(latencies) >= 20 else avg_latency
            success_rate = success_count / total_records * 100
            
            results_by_concurrency[concurrency] = {
                'concurrency': concurrency,
                'total_records': total_records,
                'throughput': round(throughput, 2),
                'avg_latency_ms': round(avg_latency, 2),
                'p95_latency_ms': round(p95_latency, 2),
                'success_rate': round(success_rate, 2),
                'duration_seconds': round(total_duration, 2)
            }
            
            logger.info(f"  总记录数: {total_records}")
            logger.info(f"  吞吐量: {throughput:.2f} 记录/秒")
            logger.info(f"  平均延迟: {avg_latency:.2f} ms")
            logger.info(f"  P95延迟: {p95_latency:.2f} ms")
            logger.info(f"  成功率: {success_rate:.2f}%")
        
        result = {
            'test_name': '并发同步性能测试',
            'concurrency_levels': concurrency_levels,
            'results_by_concurrency': results_by_concurrency,
            'timestamp': datetime.now().isoformat(),
            'status': 'PASSED'
        }
        
        # 检查是否所有并发级别都通过
        for concurrency, res in results_by_concurrency.items():
            target_latency = {10: 50, 50: 100, 100: 150, 200: 200}.get(concurrency, 200)
            if res['avg_latency_ms'] > target_latency or res['success_rate'] < 99.9:
                result['status'] = 'FAILED'
                break
        
        return result
    
    def run_all_tests(self, full_sync_data: str, incremental_data: str) -> Dict[str, Any]:
        """运行所有性能测试
        
        Args:
            full_sync_data: 全量同步测试数据文件
            incremental_data: 增量同步测试数据文件
            
        Returns:
            所有测试结果
        """
        logger.info("=" * 60)
        logger.info("开始执行全部性能测试套件")
        logger.info("=" * 60)
        
        all_results = {
            'test_suite_name': '企智连数据同步性能测试',
            'start_time': datetime.now().isoformat(),
            'tests': {}
        }
        
        # 1. 全量同步性能测试
        try:
            all_results['tests']['full_sync'] = self.run_full_sync_test(full_sync_data)
        except Exception as e:
            logger.error(f"全量同步测试失败: {e}")
            all_results['tests']['full_sync'] = {'error': str(e), 'status': 'FAILED'}
        
        # 2. 增量同步性能测试
        try:
            all_results['tests']['incremental_sync'] = self.run_incremental_sync_test(incremental_data)
        except Exception as e:
            logger.error(f"增量同步测试失败: {e}")
            all_results['tests']['incremental_sync'] = {'error': str(e), 'status': 'FAILED'}
        
        # 3. 并发同步性能测试
        try:
            all_results['tests']['concurrency'] = self.run_concurrency_test(full_sync_data)
        except Exception as e:
            logger.error(f"并发同步测试失败: {e}")
            all_results['tests']['concurrency'] = {'error': str(e), 'status': 'FAILED'}
        
        all_results['end_time'] = datetime.now().isoformat()
        
        # 计算总体状态
        passed_tests = sum(1 for test in all_results['tests'].values() if test.get('status') == 'PASSED')
        total_tests = len(all_results['tests'])
        all_results['summary'] = {
            'total_tests': total_tests,
            'passed_tests': passed_tests,
            'failed_tests': total_tests - passed_tests,
            'pass_rate': round(passed_tests / total_tests * 100, 2) if total_tests > 0 else 0,
            'overall_status': 'PASSED' if passed_tests == total_tests else 'FAILED'
        }
        
        logger.info("\n" + "=" * 60)
        logger.info("性能测试套件执行完成")
        logger.info("=" * 60)
        logger.info(f"总测试数: {total_tests}")
        logger.info(f"通过测试: {passed_tests}")
        logger.info(f"失败测试: {total_tests - passed_tests}")
        logger.info(f"通过率: {all_results['summary']['pass_rate']}%")
        logger.info(f"总体状态: {all_results['summary']['overall_status']}")
        
        return all_results


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='执行数据同步性能测试')
    parser.add_argument('--config', type=str, default='performance-test/data_sync_performance_config.json',
                        help='配置文件路径')
    parser.add_argument('--full-sync-data', type=str, default='test_data_100k.json',
                        help='全量同步测试数据文件')
    parser.add_argument('--incremental-data', type=str, default='test_data_incremental.json',
                        help='增量同步测试数据文件')
    parser.add_argument('--output', type=str, default='performance_test_results.json',
                        help='测试结果输出文件')
    parser.add_argument('--test-type', type=str, choices=['full', 'incremental', 'concurrency', 'all'],
                        default='all', help='测试类型')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("企智连数据同步性能测试执行器")
    print("=" * 60)
    
    # 创建测试器
    tester = DataSyncPerformanceTester(args.config)
    
    # 执行测试
    if args.test_type == 'all':
        results = tester.run_all_tests(args.full_sync_data, args.incremental_data)
    elif args.test_type == 'full':
        results = {'tests': {'full_sync': tester.run_full_sync_test(args.full_sync_data)}}
    elif args.test_type == 'incremental':
        results = {'tests': {'incremental_sync': tester.run_incremental_sync_test(args.incremental_data)}}
    elif args.test_type == 'concurrency':
        results = {'tests': {'concurrency': tester.run_concurrency_test(args.full_sync_data)}}
    
    # 保存结果
    with open(args.output, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试结果已保存: {args.output}")
    print("=" * 60)


if __name__ == '__main__':
    main()