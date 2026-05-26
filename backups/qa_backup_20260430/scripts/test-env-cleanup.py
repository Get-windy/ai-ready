#!/usr/bin/env python3
"""
测试环境清理脚本
用途: 清理测试环境数据和服务
版本: 1.0
"""

import os
import sys
import subprocess
import argparse
import json
import shutil
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test-env-cleanup.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='测试环境清理工具')
    parser.add_argument('--env', default='test', choices=['test', 'dev', 'prod'],
                       help='目标环境')
    parser.add_argument('--services', nargs='+', 
                       default=['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana'],
                       help='要清理的服务列表')
    parser.add_argument('--data-only', action='store_true',
                       help='仅清理数据,不清除容器')
    parser.add_argument('--backup', action='store_true',
                       help='清理前备份数据')
    parser.add_argument('--backup-dir', default='backup',
                       help='备份目录')
    parser.add_argument('--force', action='store_true',
                       help='强制清理,不提示确认')
    parser.add_argument('--verbose', action='store_true',
                       help='显示详细日志')
    return parser.parse_args()


class TestEnvironmentCleaner:
    """测试环境清理器"""
    
    def __init__(self, env: str = 'test', verbose: bool = False):
        self.env = env
        self.verbose = verbose
        self.start_time = None
        self.end_time = None
        
        # 服务容器名称
        self.containers = {
            'postgresql': 'ai-ready-postgresql',
            'redis': 'ai-ready-redis',
            'rabbitmq': 'ai-ready-rabbitmq',
            'prometheus': 'ai-ready-prometheus',
            'grafana': 'ai-ready-grafana',
            'application': 'ai-ready-application'
        }
        
        # 数据卷
        self.volumes = {
            'postgresql': 'ai-ready-pgdata',
            'redis': 'ai-ready-redisdata',
            'rabbitmq': 'ai-ready-rabbitmqdata',
            'grafana': 'ai-ready-grafanadata'
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
    
    def _confirm_action(self, message: str, force: bool = False) -> bool:
        """确认操作"""
        if force:
            return True
        
        response = input(f"{message} [y/N]: ").strip().lower()
        return response in ['y', 'yes']
    
    # ==================== 数据清理 ====================
    
    def cleanup_postgresql_data(self) -> bool:
        """清理 PostgreSQL 数据"""
        logger.info("🧹 清理 PostgreSQL 数据...")
        
        try:
            import psycopg2
            import time
            
            # 等待 PostgreSQL 就绪
            max_retries = 30
            for i in range(max_retries):
                try:
                    conn = psycopg2.connect(
                        host='localhost',
                        port=5432,
                        database='ai_ready_test',
                        user='test_user',
                        password='test_password'
                    )
                    cursor = conn.cursor()
                    
                    # 删除测试数据
                    tables = self._get_tables(conn)
                    for table in tables:
                        if table.startswith('test_') or table.endswith('_test'):
                            cursor.execute(f"DELETE FROM {table}")
                    
                    conn.commit()
                    conn.close()
                    
                    logger.info("✅ PostgreSQL 数据清理完成")
                    return True
                    
                except Exception as e:
                    if i == max_retries - 1:
                        raise e
                    time.sleep(2)
            
        except Exception as e:
            logger.error(f"❌ PostgreSQL 数据清理失败: {e}")
            return False
    
    def cleanup_redis_data(self) -> bool:
        """清理 Redis 数据"""
        logger.info("🧹 清理 Redis 数据...")
        
        try:
            import redis
            
            # 连接 Redis
            r = redis.Redis(host='localhost', port=6379, decode_responses=True)
            
            # 清空所有数据
            r.flushall()
            
            logger.info("✅ Redis 数据清理完成")
            return True
            
        except Exception as e:
            logger.error(f"❌ Redis 数据清理失败: {e}")
            return False
    
    def cleanup_rabbitmq_data(self) -> bool:
        """清理 RabbitMQ 数据"""
        logger.info("🧹 清理 RabbitMQ 数据...")
        
        try:
            # 删除所有队列
            self._run_command([
                'docker', 'exec', self.containers['rabbitmq'],
                'rabbitmqadmin', 'purge', 'queue', 'name=test-queue'
            ], check=False)
            
            # 删除测试用户
            self._run_command([
                'docker', 'exec', self.containers['rabbitmq'],
                'rabbitmqctl', 'delete_user', 'test_user'
            ], check=False)
            
            logger.info("✅ RabbitMQ 数据清理完成")
            return True
            
        except Exception as e:
            logger.error(f"❌ RabbitMQ 数据清理失败: {e}")
            return False
    
    def cleanup_application_data(self) -> bool:
        """清理应用数据"""
        logger.info("🧹 清理应用数据...")
        
        try:
            import requests
            import time
            
            # 等待应用服务就绪
            max_retries = 30
            for i in range(max_retries):
                try:
                    response = requests.get('http://localhost:8080/api/test/cleanup', timeout=5)
                    
                    if response.status_code == 200:
                        logger.info("✅ 应用数据清理完成")
                        return True
                    
                except Exception as e:
                    if i == max_retries - 1:
                        raise e
                    time.sleep(2)
            
        except Exception as e:
            logger.error(f"❌ 应用数据清理失败: {e}")
            return False
    
    def _get_tables(self, conn) -> List[str]:
        """获取数据库表列表"""
        cursor = conn.cursor()
        cursor.execute("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
        """)
        return [row[0] for row in cursor.fetchall()]
    
    # ==================== 备份数据 ====================
    
    def backup_postgresql(self) -> str:
        """备份 PostgreSQL 数据"""
        logger.info("💾 备份 PostgreSQL 数据...")
        
        backup_file = f"backup/postgresql-{datetime.now().strftime('%Y%m%d_%H%M%S')}.sql.gz"
        os.makedirs(os.path.dirname(backup_file), exist_ok=True)
        
        try:
            # 使用 pg_dump 备份
            cmd = [
                'docker', 'exec', self.containers['postgresql'],
                'pg_dump', '-U', 'test_user', 'ai_ready_test'
            ]
            
            with open(backup_file, 'w', encoding='utf-8') as f:
                subprocess.run(cmd, stdout=f, check=True)
            
            logger.info(f"✅ PostgreSQL 备份完成: {backup_file}")
            return backup_file
            
        except Exception as e:
            logger.error(f"❌ PostgreSQL 备份失败: {e}")
            return ''
    
    def backup_redis(self) -> str:
        """备份 Redis 数据"""
        logger.info("💾 备份 Redis 数据...")
        
        backup_file = f"backup/redis-{datetime.now().strftime('%Y%m%d_%H%M%S')}.rdb"
        os.makedirs(os.path.dirname(backup_file), exist_ok=True)
        
        try:
            # 使用 docker cp 复制 RDB 文件
            result = self._run_command([
                'docker', 'exec', self.containers['redis'],
                'redis-cli', 'bgsave'
            ])
            
            time.sleep(2)  # 等待备份完成
            
            self._run_command([
                'docker', 'cp',
                f"{self.containers['redis']}:/data/dump.rdb",
                backup_file
            ])
            
            logger.info(f"✅ Redis 备份完成: {backup_file}")
            return backup_file
            
        except Exception as e:
            logger.error(f"❌ Redis 备份失败: {e}")
            return ''
    
    # ==================== 服务清理 ====================
    
    def stop_service(self, service: str) -> bool:
        """停止服务"""
        container = self.containers.get(service)
        if not container:
            return True
        
        try:
            self._run_command(['docker', 'stop', container])
            self._run_command(['docker', 'rm', container])
            logger.info(f"✅ {service} 服务已停止")
            return True
        except Exception as e:
            logger.error(f"❌ 停止 {service} 服务失败: {e}")
            return False
    
    def cleanup_volume(self, volume: str) -> bool:
        """清理数据卷"""
        try:
            self._run_command(['docker', 'volume', 'rm', volume])
            logger.info(f"✅ 数据卷 {volume} 已清理")
            return True
        except Exception as e:
            logger.error(f"❌ 清理数据卷 {volume} 失败: {e}")
            return False
    
    def cleanup_container(self, container: str) -> bool:
        """清理容器"""
        try:
            self._run_command(['docker', 'rm', '-f', container])
            logger.info(f"✅ 容器 {container} 已清理")
            return True
        except Exception as e:
            logger.error(f"❌ 清理容器 {container} 失败: {e}")
            return False
    
    # ==================== 文件清理 ====================
    
    def cleanup_TEMP_dir(self) -> bool:
        """清理临时目录"""
        logger.info("🧹 清理临时目录...")
        
        temp_dirs = [
            'tmp',
            'temp',
            'logs'
        ]
        
        for temp_dir in temp_dirs:
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir)
                logger.info(f"✅ {temp_dir} 目录已清理")
        
        return True
    
    def cleanup_test_data(self) -> bool:
        """清理测试数据文件"""
        logger.info("🧹 清理测试数据文件...")
        
        test_data_dirs = [
            'test_data',
            'test-output',
            'test-reports'
        ]
        
        for test_data_dir in test_data_dirs:
            if os.path.exists(test_data_dir):
                shutil.rmtree(test_data_dir)
                logger.info(f"✅ {test_data_dir} 目录已清理")
        
        return True
    
    # ==================== 主流程 ====================
    
    def cleanup(self, services: List[str] = None, data_only: bool = False,
               backup_first: bool = False, force: bool = False) -> Dict[str, bool]:
        """清理测试环境"""
        self.start_time = datetime.now()
        logger.info("=" * 80)
        logger.info(f"测试环境清理开始 - 环境: {self.env}")
        logger.info("=" * 80)
        
        if services is None:
            services = ['postgresql', 'redis', 'rabbitmq', 'prometheus', 'grafana']
        
        results = {}
        
        try:
            # 1. 备份数据
            if backup_first and not data_only:
                if self._confirm_action("是否备份数据?", force):
                    results['backup_postgresql'] = self.backup_postgresql()
                    results['backup_redis'] = self.backup_redis()
            
            # 2. 清理数据
            for service in services:
                if service == 'postgresql':
                    results['cleanup_postgresql'] = self.cleanup_postgresql_data()
                elif service == 'redis':
                    results['cleanup_redis'] = self.cleanup_redis_data()
                elif service == 'rabbitmq':
                    results['cleanup_rabbitmq'] = self.cleanup_rabbitmq_data()
                elif service == 'application':
                    results['cleanup_application'] = self.cleanup_application_data()
                else:
                    logger.warning(f"未知服务: {service}")
                
                time.sleep(1)  # 等待操作完成
            
            # 3. 清理服务
            if not data_only:
                for service in services:
                    if service == 'postgresql':
                        results['stop_postgresql'] = self.stop_service('postgresql')
                    elif service == 'redis':
                        results['stop_redis'] = self.stop_service('redis')
                    elif service == 'rabbitmq':
                        results['stop_rabbitmq'] = self.stop_service('rabbitmq')
                    elif service == 'prometheus':
                        results['stop_prometheus'] = self.stop_service('prometheus')
                    elif service == 'grafana':
                        results['stop_grafana'] = self.stop_service('grafana')
                
                # 4. 清理容器和卷
                for service in services:
                    if service == 'postgresql':
                        results['cleanup_postgresql_volume'] = self.cleanup_volume('ai-ready-pgdata')
                    elif service == 'redis':
                        results['cleanup_redis_volume'] = self.cleanup_volume('ai-ready-redisdata')
                    elif service == 'rabbitmq':
                        results['cleanup_rabbitmq_volume'] = self.cleanup_volume('ai-ready-rabbitmqdata')
                    elif service == 'grafana':
                        results['cleanup_grafana_volume'] = self.cleanup_volume('ai-ready-grafanadata')
                
                # 5. 清理临时文件
                results['cleanup_TEMP_dirs'] = self.cleanup_TEMP_dir()
                results['cleanup_test_data_files'] = self.cleanup_test_data()
            
            self.end_time = datetime.now()
            
            # 输出结果
            logger.info("=" * 80)
            logger.info("环境清理完成!")
            logger.info(f"总耗时: {(self.end_time - self.start_time).total_seconds():.2f} 秒")
            logger.info("清理结果:")
            
            for service, result in results.items():
                status = "✅ 成功" if result else "❌ 失败"
                logger.info(f"  - {service}: {status}")
            
            return results
            
        except Exception as e:
            logger.error(f"环境清理异常: {e}")
            self.end_time = datetime.now()
            return {**results, 'error': str(e), 'duration': (self.end_time - self.start_time).total_seconds()}


def main():
    """主函数"""
    args = parse_args()
    
    # 设置详细日志
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # 创建清理器
    cleaner = TestEnvironmentCleaner(env=args.env, verbose=args.verbose)
    
    # 执行清理
    results = cleaner.cleanup(
        services=args.services,
        data_only=args.data_only,
        backup_first=args.backup,
        force=args.force
    )
    
    # 输出结果
    print("\n" + "=" * 80)
    print("测试环境清理结果:")
    print("=" * 80)
    
    all_success = all(v is True for k, v in results.items() if isinstance(v, bool))
    
    if all_success:
        print("✅ 环境清理成功!")
        sys.exit(0)
    else:
        print("❌ 环境清理失败:")
        for service, result in results.items():
            if isinstance(result, bool) and not result:
                print(f"  - {service}: 失败")
        sys.exit(1)


if __name__ == '__main__':
    main()
