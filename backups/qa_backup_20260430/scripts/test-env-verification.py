#!/usr/bin/env python3
"""
测试环境验证脚本
用途: 验证测试环境服务和数据的完整性和可用性
版本: 1.0
"""

import os
import sys
import json
import argparse
import subprocess
import logging
import time
from datetime import datetime
from typing import Dict, List, Any, Optional

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test-env-verification.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='测试环境验证工具')
    parser.add_argument('--env', default='test', choices=['test', 'dev', 'prod'],
                       help='目标环境')
    parser.add_argument('--services', nargs='+', 
                       default=['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana', 'application'],
                       help='要验证的服务列表')
    parser.add_argument('--output', default='test-env-verification-report.json',
                       help='验证报告输出文件')
    parser.add_argument('--verbose', action='store_true',
                       help='显示详细日志')
    return parser.parse_args()


class TestEnvironmentVerifier:
    """测试环境验证器"""
    
    def __init__(self, env: str = 'test', verbose: bool = False):
        self.env = env
        self.verbose = verbose
        self.results = []
        self.passed_count = 0
        self.failed_count = 0
        self.skipped_count = 0
        self.start_time = None
        self.end_time = None
        
        # 服务配置
        self.services = {
            'postgresql': {
                'host': 'localhost',
                'port': 5432,
                'database': 'ai_ready_test',
                'user': 'test_user',
                'password': 'test_password'
            },
            'redis': {
                'host': 'localhost',
                'port': 6379
            },
            'rabbitmq': {
                'host': 'localhost',
                'port': 5672,
                'management_port': 15672
            },
            'prometheus': {
                'host': 'localhost',
                'port': 9090
            },
            'grafana': {
                'host': 'localhost',
                'port': 3000
            },
            'application': {
                'host': 'localhost',
                'port': 8080,
                'health_check': '/actuator/health'
            }
        }
        
        # 性能基准
        self.benchmarks = {
            'postgresql': {
                'max_response_time': 100,  # ms
                'max_connections': 100,
                'query_timeout': 5  # seconds
            },
            'redis': {
                'max_response_time': 5,  # ms
                'cache_hit_rate': 95  # %
            },
            'rabbitmq': {
                'max_response_time': 100,  # ms
                'max_queue_depth': 1000
            },
            'application': {
                'max_response_time': 500,  # ms
                'max_error_rate': 1  # %
            }
        }
    
    def _log(self, level: str, message: str):
        """记录日志"""
        if self.verbose:
            logger.debug(f"[{level}] {message}")
        else:
            logger.info(f"[{level}] {message}")
    
    def _run_command(self, command: List[str], check: bool = True) -> subprocess.CompletedProcess:
        """运行命令"""
        try:
            result = subprocess.run(command, capture_output=True, text=True, check=check)
            return result
        except subprocess.CalledProcessError as e:
            logger.error(f"命令执行失败: {' '.join(command)}")
            logger.error(f"错误输出: {e.stderr}")
            raise
    
    def add_result(self, test_id: str, test_name: str, status: str, 
                   details: str = "", metrics: Dict = None):
        """添加验证结果"""
        result = {
            'test_id': test_id,
            'test_name': test_name,
            'status': status,
            'details': details,
            'metrics': metrics or {},
            'timestamp': datetime.now().isoformat()
        }
        self.results.append(result)
        
        if status == 'PASSED':
            self.passed_count += 1
            logger.info(f"✅ [{test_id}] {test_name}")
        elif status == 'FAILED':
            self.failed_count += 1
            logger.error(f"❌ [{test_id}] {test_name} - {details}")
        else:
            self.skipped_count += 1
            logger.warning(f"⏭️ [{test_id}] {test_name} - {details}")
    
    # ==================== PostgreSQL 验证 ====================
    
    def verify_postgresql(self) -> bool:
        """验证 PostgreSQL 服务"""
        logger.info("🔍 验证 PostgreSQL...")
        
        try:
            import psycopg2
            
            # 测试连接
            start_time = time.time()
            conn = psycopg2.connect(
                host=self.services['postgresql']['host'],
                port=self.services['postgresql']['port'],
                database=self.services['postgresql']['database'],
                user=self.services['postgresql']['user'],
                password=self.services['postgresql']['password']
            )
            response_time = (time.time() - start_time) * 1000
            
            # 验证响应时间
            if response_time > self.benchmarks['postgresql']['max_response_time']:
                self.add_result('DB01', 'PostgreSQL 连接测试', 'FAILED',
                              f"响应时间 {response_time:.2f}ms 超过阈值 {self.benchmarks['postgresql']['max_response_time']}ms")
                return False
            
            self.add_result('DB01', 'PostgreSQL 连接测试', 'PASSED',
                          f"响应时间: {response_time:.2f}ms")
            
            # 验证基本查询
            cursor = conn.cursor()
            cursor.execute("SELECT version()")
            version = cursor.fetchone()[0]
            
            self.add_result('DB02', 'PostgreSQL 基本查询', 'PASSED',
                          f"版本: {version}")
            
            # 验证连接数
            cursor.execute("SELECT count(*) FROM pg_stat_activity")
            connections = cursor.fetchone()[0]
            
            if connections > self.benchmarks['postgresql']['max_connections']:
                self.add_result('DB03', 'PostgreSQL 连接数', 'FAILED',
                              f"当前连接数 {connections} 超过阈值 {self.benchmarks['postgresql']['max_connections']}")
            else:
                self.add_result('DB03', 'PostgreSQL 连接数', 'PASSED',
                              f"当前连接数: {connections}")
            
            # 验证表存在
            cursor.execute("""
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public'
            """)
            tables = [row[0] for row in cursor.fetchall()]
            
            self.add_result('DB04', 'PostgreSQL 表验证', 'PASSED',
                          f"共有 {len(tables)} 张表")
            
            conn.close()
            
            logger.info("✅ PostgreSQL 验证完成")
            return True
            
        except Exception as e:
            self.add_result('DB00', 'PostgreSQL 验证', 'FAILED', str(e))
            logger.error(f"❌ PostgreSQL 验证失败: {e}")
            return False
    
    # ==================== Redis 验证 ====================
    
    def verify_redis(self) -> bool:
        """验证 Redis 服务"""
        logger.info("🔍 验证 Redis...")
        
        try:
            import redis
            
            # 测试连接
            start_time = time.time()
            r = redis.Redis(
                host=self.services['redis']['host'],
                port=self.services['redis']['port'],
                decode_responses=True
            )
            response = r.ping()
            response_time = (time.time() - start_time) * 1000
            
            if response:
                self.add_result('CACHE01', 'Redis 连接测试', 'PASSED',
                              f"响应时间: {response_time:.2f}ms")
            else:
                self.add_result('CACHE01', 'Redis 连接测试', 'FAILED',
                              "Ping 失败")
                return False
            
            # 验证读写
            test_key = f'test_key_{datetime.now().strftime("%Y%m%d%H%M%S")}'
            test_value = 'test_value'
            
            start_time = time.time()
            r.set(test_key, test_value)
            value = r.get(test_key)
            response_time = (time.time() - start_time) * 1000
            
            if value == test_value:
                self.add_result('CACHE02', 'Redis 读写测试', 'PASSED',
                              f"读写时间: {response_time:.2f}ms")
            else:
                self.add_result('CACHE02', 'Redis 读写测试', 'FAILED',
                              "读写数据不一致")
            
            # 清理测试数据
            r.delete(test_key)
            
            # 验证内存使用
            info = r.info()
            used_memory = info.get('used_memory_human', 'N/A')
            
            self.add_result('CACHE03', 'Redis 内存使用', 'PASSED',
                          f"已用内存: {used_memory}")
            
            logger.info("✅ Redis 验证完成")
            return True
            
        except Exception as e:
            self.add_result('CACHE00', 'Redis 验证', 'FAILED', str(e))
            logger.error(f"❌ Redis 验证失败: {e}")
            return False
    
    # ==================== RabbitMQ 验证 ====================
    
    def verify_rabbitmq(self) -> bool:
        """验证 RabbitMQ 服务"""
        logger.info("🔍 验证 RabbitMQ...")
        
        try:
            import requests
            
            # 测试管理接口
            url = f"http://{self.services['rabbitmq']['host']}:{self.services['rabbitmq']['management_port']}/api/overview"
            start_time = time.time()
            response = requests.get(url, auth=('guest', 'guest'), timeout=5)
            response_time = (time.time() - start_time) * 1000
            
            if response.status_code == 200:
                data = response.json()
                self.add_result('MQ01', 'RabbitMQ 管理接口', 'PASSED',
                              f"响应时间: {response_time:.2f}ms, 版本: {data.get('rabbitmq_version', 'N/A')}")
            else:
                self.add_result('MQ01', 'RabbitMQ 管理接口', 'FAILED',
                              f"状态码: {response.status_code}")
                return False
            
            # 验证队列状态
            url = f"http://{self.services['rabbitmq']['host']}:{self.services['rabbitmq']['management_port']}/api/queues"
            response = requests.get(url, auth=('guest', 'guest'), timeout=5)
            
            if response.status_code == 200:
                queues = response.json()
                self.add_result('MQ02', 'RabbitMQ 队列状态', 'PASSED',
                              f"队列数量: {len(queues)}")
            else:
                self.add_result('MQ02', 'RabbitMQ 队列状态', 'FAILED',
                              f"状态码: {response.status_code}")
            
            logger.info("✅ RabbitMQ 验证完成")
            return True
            
        except Exception as e:
            self.add_result('MQ00', 'RabbitMQ 验证', 'FAILED', str(e))
            logger.error(f"❌ RabbitMQ 验证失败: {e}")
            return False
    
    # ==================== Prometheus 验证 ====================
    
    def verify_prometheus(self) -> bool:
        """验证 Prometheus 服务"""
        logger.info("🔍 验证 Prometheus...")
        
        try:
            import requests
            
            # 测试 API
            url = f"http://{self.services['prometheus']['host']}:{self.services['prometheus']['port']}/api/v1/status/config"
            start_time = time.time()
            response = requests.get(url, timeout=5)
            response_time = (time.time() - start_time) * 1000
            
            if response.status_code == 200:
                self.add_result('MON01', 'Prometheus API', 'PASSED',
                              f"响应时间: {response_time:.2f}ms")
            else:
                self.add_result('MON01', 'Prometheus API', 'FAILED',
                              f"状态码: {response.status_code}")
                return False
            
            # 验证指标
            url = f"http://{self.services['prometheus']['host']}:{self.services['prometheus']['port']}/api/v1/label/__name__/values"
            response = requests.get(url, timeout=5)
            
            if response.status_code == 200:
                metrics = response.json().get('data', [])
                self.add_result('MON02', 'Prometheus 指标', 'PASSED',
                              f"指标数量: {len(metrics)}")
            else:
                self.add_result('MON02', 'Prometheus 指标', 'FAILED',
                              f"状态码: {response.status_code}")
            
            logger.info("✅ Prometheus 验证完成")
            return True
            
        except Exception as e:
            self.add_result('MON00', 'Prometheus 验证', 'FAILED', str(e))
            logger.error(f"❌ Prometheus 验证失败: {e}")
            return False
    
    # ==================== Grafana 验证 ====================
    
    def verify_grafana(self) -> bool:
        """验证 Grafana 服务"""
        logger.info("🔍 验证 Grafana...")
        
        try:
            import requests
            
            # 测试健康检查
            url = f"http://{self.services['grafana']['host']}:{self.services['grafana']['port']}/api/health"
            start_time = time.time()
            response = requests.get(url, timeout=5)
            response_time = (time.time() - start_time) * 1000
            
            if response.status_code == 200:
                data = response.json()
                self.add_result('DASH01', 'Grafana 健康检查', 'PASSED',
                              f"响应时间: {response_time:.2f}ms, 状态: {data.get('database', 'N/A')}")
            else:
                self.add_result('DASH01', 'Grafana 健康检查', 'FAILED',
                              f"状态码: {response.status_code}")
                return False
            
            logger.info("✅ Grafana 验证完成")
            return True
            
        except Exception as e:
            self.add_result('DASH00', 'Grafana 验证', 'FAILED', str(e))
            logger.error(f"❌ Grafana 验证失败: {e}")
            return False
    
    # ==================== 应用服务验证 ====================
    
    def verify_application(self) -> bool:
        """验证应用服务"""
        logger.info("🔍 验证应用服务...")
        
        try:
            import requests
            
            # 测试健康检查
            url = f"http://{self.services['application']['host']}:{self.services['application']['port']}{self.services['application']['health_check']}"
            start_time = time.time()
            response = requests.get(url, timeout=5)
            response_time = (time.time() - start_time) * 1000
            
            if response.status_code == 200:
                data = response.json()
                self.add_result('APP01', '应用服务健康检查', 'PASSED',
                              f"响应时间: {response_time:.2f}ms, 状态: {data.get('status', 'N/A')}")
            else:
                self.add_result('APP01', '应用服务健康检查', 'FAILED',
                              f"状态码: {response.status_code}")
                return False
            
            # 验证 API 可用性
            url = f"http://{self.services['application']['host']}:{self.services['application']['port']}/api/test/status"
            response = requests.get(url, timeout=5)
            
            if response.status_code == 200:
                self.add_result('APP02', '应用服务 API', 'PASSED',
                              "API 响应正常")
            else:
                self.add_result('APP02', '应用服务 API', 'FAILED',
                              f"状态码: {response.status_code}")
            
            logger.info("✅ 应用服务验证完成")
            return True
            
        except Exception as e:
            self.add_result('APP00', '应用服务验证', 'FAILED', str(e))
            logger.error(f"❌ 应用服务验证失败: {e}")
            return False
    
    # ==================== 数据验证 ====================
    
    def verify_data_integrity(self) -> bool:
        """验证数据完整性"""
        logger.info("🔍 验证数据完整性...")
        
        try:
            import psycopg2
            
            conn = psycopg2.connect(
                host=self.services['postgresql']['host'],
                port=self.services['postgresql']['port'],
                database=self.services['postgresql']['database'],
                user=self.services['postgresql']['user'],
                password=self.services['postgresql']['password']
            )
            cursor = conn.cursor()
            
            # 验证关键表
            tables_to_check = [
                'sys_user',
                'erp_product',
                'erp_order',
                'erp_inventory',
                'res_partner'
            ]
            
            for table in tables_to_check:
                try:
                    cursor.execute(f"SELECT count(*) FROM {table}")
                    count = cursor.fetchone()[0]
                    
                    if count > 0:
                        self.add_result(f'DATA_{table}', f'表 {table} 数据', 'PASSED',
                                      f"数据条数: {count}")
                    else:
                        self.add_result(f'DATA_{table}', f'表 {table} 数据', 'FAILED',
                                      "表中无数据")
                except Exception as e:
                    self.add_result(f'DATA_{table}', f'表 {table} 数据', 'FAILED',
                                  str(e))
            
            conn.close()
            
            logger.info("✅ 数据完整性验证完成")
            return True
            
        except Exception as e:
            self.add_result('DATA00', '数据完整性验证', 'FAILED', str(e))
            logger.error(f"❌ 数据完整性验证失败: {e}")
            return False
    
    # ==================== 主流程 ====================
    
    def verify(self, services: List[str] = None) -> Dict[str, Any]:
        """执行测试环境验证"""
        self.start_time = datetime.now()
        logger.info("=" * 80)
        logger.info(f"测试环境验证开始 - 环境: {self.env}")
        logger.info("=" * 80)
        
        if services is None:
            services = ['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana', 'application']
        
        results = {}
        
        try:
            # 验证服务
            for service in services:
                if service == 'postgresql':
                    results['postgresql'] = self.verify_postgresql()
                elif service == 'redis':
                    results['redis'] = self.verify_redis()
                elif service == 'rabbitmq':
                    results['rabbitmq'] = self.verify_rabbitmq()
                elif service == 'prometheus':
                    results['prometheus'] = self.verify_prometheus()
                elif service == 'grafana':
                    results['grafana'] = self.verify_grafana()
                elif service == 'application':
                    results['application'] = self.verify_application()
                else:
                    logger.warning(f"未知服务: {service}")
            
            # 验证数据
            results['data'] = self.verify_data_integrity()
            
            self.end_time = datetime.now()
            
            # 输出结果
            logger.info("=" * 80)
            logger.info("环境验证完成!")
            logger.info(f"总耗时: {(self.end_time - self.start_time).total_seconds():.2f} 秒")
            logger.info(f"通过: {self.passed_count}, 失败: {self.failed_count}, 跳过: {self.skipped_count}")
            
            return results
            
        except Exception as e:
            logger.error(f"环境验证异常: {e}")
            self.end_time = datetime.now()
            return {**results, 'error': str(e), 'duration': (self.end_time - self.start_time).total_seconds()}
    
    def generate_report(self, output_file: str = 'test-env-verification-report.json'):
        """生成验证报告"""
        report = {
            'timestamp': datetime.now().isoformat(),
            'environment': self.env,
            'summary': {
                'total': len(self.results),
                'passed': self.passed_count,
                'failed': self.failed_count,
                'skipped': self.skipped_count,
                'pass_rate': f"{(self.passed_count / len(self.results) * 100):.2f}%" if self.results else "0%"
            },
            'results': self.results,
            'duration': (self.end_time - self.start_time).total_seconds() if self.end_time else 0
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"✅ 验证报告已生成: {output_file}")
        return report


def main():
    """主函数"""
    args = parse_args()
    
    # 设置详细日志
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # 创建验证器
    verifier = TestEnvironmentVerifier(env=args.env, verbose=args.verbose)
    
    # 执行验证
    results = verifier.verify(services=args.services)
    
    # 生成报告
    report = verifier.generate_report(output_file=args.output)
    
    # 输出结果
    print("\n" + "=" * 80)
    print("测试环境验证结果:")
    print("=" * 80)
    print(f"总测试数: {report['summary']['total']}")
    print(f"通过: {report['summary']['passed']}")
    print(f"失败: {report['summary']['failed']}")
    print(f"跳过: {report['summary']['skipped']}")
    print(f"通过率: {report['summary']['pass_rate']}")
    print(f"报告文件: {args.output}")
    
    if report['summary']['failed'] == 0:
        print("✅ 所有验证通过!")
        sys.exit(0)
    else:
        print("❌ 部分验证失败:")
        for result in verifier.results:
            if result['status'] == 'FAILED':
                print(f"  - {result['test_id']}: {result['test_name']} - {result['details']}")
        sys.exit(1)


if __name__ == '__main__':
    main()
