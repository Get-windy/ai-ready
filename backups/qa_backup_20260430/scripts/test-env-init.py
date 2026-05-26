#!/usr/bin/env python3
"""
测试环境初始化脚本
用途: 初始化测试环境,部署所需服务和数据
版本: 1.0
"""

import os
import sys
import json
import subprocess
import time
import argparse
from datetime import datetime
from typing import Dict, List, Any, Optional
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test-env-init.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='测试环境初始化工具')
    parser.add_argument('--env', default='test', choices=['test', 'dev', 'prod'],
                       help='目标环境')
    parser.add_argument('--services', nargs='+', 
                       default=['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana'],
                       help='要初始化的服务列表')
    parser.add_argument('--skip-verification', action='store_true',
                       help='跳过验证步骤')
    parser.add_argument('--verbose', action='store_true',
                       help='显示详细日志')
    return parser.parse_args()


class TestEnvironmentInitializer:
    """测试环境初始化器"""
    
    def __init__(self, env: str = 'test', verbose: bool = False):
        self.env = env
        self.verbose = verbose
        self.services_status = {}
        self.start_time = None
        self.end_time = None
        
        # 服务配置
        self.services = {
            'postgresql': {
                'host': 'localhost',
                'port': 5432,
                'container_name': 'ai-ready-postgresql',
                'image': 'postgres:14.2',
                'volumes': {
                    'pgdata': '/var/lib/postgresql/data',
                    'init_scripts': '/docker-entrypoint-initdb.d'
                },
                'env_vars': {
                    'POSTGRES_USER': 'test_user',
                    'POSTGRES_PASSWORD': 'test_password',
                    'POSTGRES_DB': 'ai_ready_test'
                }
            },
            'redis': {
                'host': 'localhost',
                'port': 6379,
                'container_name': 'ai-ready-redis',
                'image': 'redis:7.0',
                'volumes': {
                    'redisdata': '/data'
                }
            },
            'rabbitmq': {
                'host': 'localhost',
                'port': 5672,
                'management_port': 15672,
                'container_name': 'ai-ready-rabbitmq',
                'image': 'rabbitmq:3.12-management',
                'env_vars': {
                    'RABBITMQ_DEFAULT_USER': 'guest',
                    'RABBITMQ_DEFAULT_PASS': 'guest'
                }
            },
            'prometheus': {
                'host': 'localhost',
                'port': 9090,
                'container_name': 'ai-ready-prometheus',
                'image': 'prom/prometheus:v2.45',
                'ports': ['9090:9090'],
                'volumes': {
                    './config/prometheus.yml': '/etc/prometheus/prometheus.yml'
                }
            },
            'grafana': {
                'host': 'localhost',
                'port': 3000,
                'container_name': 'ai-ready-grafana',
                'image': 'grafana/grafana:10.2',
                'ports': ['3000:3000'],
                'volumes': {
                    'grafana-data': '/var/lib/grafana'
                }
            },
            'application': {
                'host': 'localhost',
                'port': 8080,
                'context_path': '/api',
                'health_check': '/actuator/health'
            }
        }
        
        self.config = self._load_config()
    
    def _load_config(self) -> Dict:
        """加载配置文件"""
        config_file = f'config/env-{self.env}.json'
        if os.path.exists(config_file):
            with open(config_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    
    def _log(self, level: str, message: str, details: str = ''):
        """记录日志"""
        if self.verbose:
            logger.debug(f"[{level}] {message} {details}")
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
    
    def _check_service_health(self, service: str, timeout: int = 30) -> bool:
        """检查服务健康状态"""
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            try:
                if service == 'postgresql':
                    result = self._run_command([
                        'docker', 'exec', self.services['postgresql']['container_name'],
                        'pg_isready', '-U', 'test_user'
                    ])
                    return result.returncode == 0
                
                elif service == 'redis':
                    result = self._run_command([
                        'docker', 'exec', self.services['redis']['container_name'],
                        'redis-cli', 'ping'
                    ])
                    return result.stdout.strip() == 'PONG'
                
                elif service == 'rabbitmq':
                    result = self._run_command([
                        'docker', 'exec', self.services['rabbitmq']['container_name'],
                        'rabbitmq-diagnostics', 'ping'
                    ])
                    return result.returncode == 0
                
                elif service == 'prometheus':
                    import requests
                    response = requests.get(f"http://localhost:9090/api/v1/status/config", timeout=5)
                    return response.status_code == 200
                
                elif service == 'grafana':
                    import requests
                    response = requests.get(f"http://localhost:3000/api/health", timeout=5)
                    return response.status_code == 200
                
                elif service == 'application':
                    import requests
                    response = requests.get(f"http://localhost:8080{self.services['application']['health_check']}", timeout=5)
                    return response.status_code == 200
                
            except Exception as e:
                self._log('DEBUG', f'{service} 健康检查异常: {e}')
            
            time.sleep(2)
        
        return False
    
    # ==================== 服务部署 ====================
    
    def deploy_postgresql(self) -> bool:
        """部署 PostgreSQL"""
        logger.info("🚀 部署 PostgreSQL...")
        
        # 检查容器是否存在
        result = self._run_command(['docker', 'ps', '-a', '--filter', 'name=ai-ready-postgresql', '--format', '{{.Names}}'])
        container_exists = 'ai-ready-postgresql' in result.stdout
        
        if not container_exists:
            # 创建数据卷
            self._run_command(['docker', 'volume', 'create', 'ai-ready-pgdata'])
            
            # 启动容器
            env_vars = []
            for key, value in self.services['postgresql']['env_vars'].items():
                env_vars.extend(['-e', f'{key}={value}'])
            
            self._run_command([
                'docker', 'run', '-d',
                '--name', 'ai-ready-postgresql',
                '--network', 'ai-ready-network',
                '-p', '5432:5432',
            ] + env_vars + [
                '-v', 'ai-ready-pgdata:/var/lib/postgresql/data',
                '-v', './init-scripts:/docker-entrypoint-initdb.d',
                self.services['postgresql']['image']
            ])
        
        # 等待服务就绪
        if not self._check_service_health('postgresql'):
            logger.error("❌ PostgreSQL 部署失败")
            return False
        
        logger.info("✅ PostgreSQL 部署成功")
        self.services_status['postgresql'] = 'running'
        return True
    
    def deploy_redis(self) -> bool:
        """部署 Redis"""
        logger.info("🚀 部署 Redis...")
        
        # 检查容器是否存在
        result = self._run_command(['docker', 'ps', '-a', '--filter', 'name=ai-ready-redis', '--format', '{{.Names}}'])
        container_exists = 'ai-ready-redis' in result.stdout
        
        if not container_exists:
            self._run_command([
                'docker', 'run', '-d',
                '--name', 'ai-ready-redis',
                '--network', 'ai-ready-network',
                '-p', '6379:6379',
                self.services['redis']['image']
            ])
        
        if not self._check_service_health('redis'):
            logger.error("❌ Redis 部署失败")
            return False
        
        logger.info("✅ Redis 部署成功")
        self.services_status['redis'] = 'running'
        return True
    
    def deploy_rabbitmq(self) -> bool:
        """部署 RabbitMQ"""
        logger.info("🚀 部署 RabbitMQ...")
        
        # 检查容器是否存在
        result = self._run_command(['docker', 'ps', '-a', '--filter', 'name=ai-ready-rabbitmq', '--format', '{{.Names}}'])
        container_exists = 'ai-ready-rabbitmq' in result.stdout
        
        if not container_exists:
            env_vars = []
            for key, value in self.services['rabbitmq']['env_vars'].items():
                env_vars.extend(['-e', f'{key}={value}'])
            
            self._run_command([
                'docker', 'run', '-d',
                '--name', 'ai-ready-rabbitmq',
                '--network', 'ai-ready-network',
                '-p', '5672:5672',
                '-p', '15672:15672',
            ] + env_vars + [
                self.services['rabbitmq']['image']
            ])
        
        if not self._check_service_health('rabbitmq'):
            logger.error("❌ RabbitMQ 部署失败")
            return False
        
        logger.info("✅ RabbitMQ 部署成功")
        self.services_status['rabbitmq'] = 'running'
        return True
    
    def deploy_prometheus(self) -> bool:
        """部署 Prometheus"""
        logger.info("🚀 部署 Prometheus...")
        
        # 检查容器是否存在
        result = self._run_command(['docker', 'ps', '-a', '--filter', 'name=ai-ready-prometheus', '--format', '{{.Names}}'])
        container_exists = 'ai-ready-prometheus' in result.stdout
        
        if not container_exists:
            self._run_command([
                'docker', 'run', '-d',
                '--name', 'ai-ready-prometheus',
                '--network', 'ai-ready-network',
                '-p', '9090:9090',
                '-v', f'{os.getcwd()}/config/prometheus.yml:/etc/prometheus/prometheus.yml',
                self.services['prometheus']['image']
            ])
        
        if not self._check_service_health('prometheus'):
            logger.error("❌ Prometheus 部署失败")
            return False
        
        logger.info("✅ Prometheus 部署成功")
        self.services_status['prometheus'] = 'running'
        return True
    
    def deploy_grafana(self) -> bool:
        """部署 Grafana"""
        logger.info("🚀 部署 Grafana...")
        
        # 检查容器是否存在
        result = self._run_command(['docker', 'ps', '-a', '--filter', 'name=ai-ready-grafana', '--format', '{{.Names}}'])
        container_exists = 'ai-ready-grafana' in result.stdout
        
        if not container_exists:
            self._run_command([
                'docker', 'run', '-d',
                '--name', 'ai-ready-grafana',
                '--network', 'ai-ready-network',
                '-p', '3000:3000',
                self.services['grafana']['image']
            ])
        
        if not self._check_service_health('grafana'):
            logger.error("❌ Grafana 部署失败")
            return False
        
        logger.info("✅ Grafana 部署成功")
        self.services_status['grafana'] = 'running'
        return True
    
    def deploy_application(self) -> bool:
        """部署应用服务"""
        logger.info("🚀 部署应用服务...")
        
        # 检查是否运行了应用
        if not self._check_service_health('application'):
            logger.error("❌ 应用服务部署失败")
            return False
        
        logger.info("✅ 应用服务部署成功")
        self.services_status['application'] = 'running'
        return True
    
    # ==================== 环境配置 ====================
    
    def setup_network(self) -> bool:
        """设置 Docker 网络"""
        logger.info("🔧 设置 Docker 网络...")
        
        result = self._run_command(['docker', 'network', 'ls', '--format', '{{.Name}}'])
        if 'ai-ready-network' not in result.stdout:
            self._run_command(['docker', 'network', 'create', 'ai-ready-network'])
            logger.info("✅ Docker 网络创建成功")
        else:
            logger.info("✅ Docker 网络已存在")
        
        return True
    
    def setup_database(self) -> bool:
        """初始化数据库"""
        logger.info("🔧 初始化数据库...")
        
        try:
            import psycopg2
            import time
            
            # 等待 PostgreSQL 就绪
            max_retries = 30
            for i in range(max_retries):
                try:
                    conn = psycopg2.connect(
                        host=self.services['postgresql']['host'],
                        port=self.services['postgresql']['port'],
                        database='ai_ready_test',
                        user='test_user',
                        password='test_password'
                    )
                    conn.close()
                    break
                except Exception as e:
                    if i == max_retries - 1:
                        raise e
                    time.sleep(2)
            
            # 创建测试数据库
            conn = psycopg2.connect(
                host=self.services['postgresql']['host'],
                port=self.services['postgresql']['port'],
                database='ai_ready_test',
                user='test_user',
                password='test_password'
            )
            cursor = conn.cursor()
            
            # 执行初始化脚本
            init_script = './init-scripts/setup-schema.sql'
            if os.path.exists(init_script):
                with open(init_script, 'r', encoding='utf-8') as f:
                    sql = f.read()
                    cursor.execute(sql)
            
            conn.commit()
            conn.close()
            
            logger.info("✅ 数据库初始化成功")
            return True
            
        except Exception as e:
            logger.error(f"❌ 数据库初始化失败: {e}")
            return False
    
    def setup_monitoring(self) -> bool:
        """配置监控系统"""
        logger.info("🔧 配置监控系统...")
        
        try:
            import requests
            
            # 配置 Prometheus 数据源
            prometheus_url = 'http://localhost:9090'
            response = requests.get(f"{prometheus_url}/api/v1/status/config", timeout=5)
            
            if response.status_code == 200:
                logger.info("✅ Prometheus 配置成功")
            
            # 配置 Grafana 数据源
            grafana_url = 'http://localhost:3000'
            response = requests.get(f"{grafana_url}/api/health", timeout=5)
            
            if response.status_code == 200:
                logger.info("✅ Grafana 配置成功")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ 监控系统配置失败: {e}")
            return False
    
    # ==================== 主流程 ====================
    
    def initialize(self, services: List[str] = None) -> Dict[str, bool]:
        """初始化测试环境"""
        self.start_time = datetime.now()
        logger.info("=" * 80)
        logger.info(f"测试环境初始化开始 - 环境: {self.env}")
        logger.info("=" * 80)
        
        if services is None:
            services = ['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana', 'application']
        
        results = {}
        
        try:
            # 1. 设置网络
            results['network'] = self.setup_network()
            
            # 2. 部署服务
            for service in services:
                if service == 'postgresql':
                    results['postgresql'] = self.deploy_postgresql()
                elif service == 'redis':
                    results['redis'] = self.deploy_redis()
                elif service == 'rabbitmq':
                    results['rabbitmq'] = self.deploy_rabbitmq()
                elif service == 'prometheus':
                    results['prometheus'] = self.deploy_prometheus()
                elif service == 'grafana':
                    results['grafana'] = self.deploy_grafana()
                elif service == 'application':
                    results['application'] = self.deploy_application()
                else:
                    logger.warning(f"未知服务: {service}")
                
                time.sleep(2)  # 等待服务稳定
            
            # 3. 配置环境
            results['database'] = self.setup_database()
            results['monitoring'] = self.setup_monitoring()
            
            # 4. 验证环境
            if not results.get('skip_verification', False):
                results['verification'] = self.verify_environment()
            
            self.end_time = datetime.now()
            
            # 输出结果
            logger.info("=" * 80)
            logger.info("环境初始化完成!")
            logger.info(f"总耗时: {(self.end_time - self.start_time).total_seconds():.2f} 秒")
            logger.info("服务状态:")
            
            for service, status in results.items():
                if isinstance(status, bool):
                    status_str = "✅ 成功" if status else "❌ 失败"
                    logger.info(f"  - {service}: {status_str}")
            
            return results
            
        except Exception as e:
            logger.error(f"环境初始化异常: {e}")
            self.end_time = datetime.now()
            return {**results, 'error': str(e), 'duration': (self.end_time - self.start_time).total_seconds()}
    
    def verify_environment(self) -> bool:
        """验证环境"""
        logger.info("🔍 验证环境...")
        
        all_healthy = True
        for service in ['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana', 'application']:
            healthy = self._check_service_health(service, timeout=10)
            status = "✅" if healthy else "❌"
            logger.info(f"  {status} {service}")
            if not healthy:
                all_healthy = False
        
        return all_healthy


def main():
    """主函数"""
    args = parse_args()
    
    # 设置详细日志
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # 创建初始化器
    initializer = TestEnvironmentInitializer(env=args.env, verbose=args.verbose)
    
    # 执行初始化
    results = initializer.initialize(services=args.services)
    
    # 输出结果
    print("\n" + "=" * 80)
    print("测试环境初始化结果:")
    print("=" * 80)
    
    all_success = all(v is True for k, v in results.items() if isinstance(v, bool))
    
    if all_success:
        print("✅ 环境初始化成功!")
        sys.exit(0)
    else:
        print("❌ 环境初始化失败:")
        for service, result in results.items():
            if isinstance(result, bool) and not result:
                print(f"  - {service}: 失败")
        sys.exit(1)


if __name__ == '__main__':
    main()
