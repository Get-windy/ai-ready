"""
测试配置管理
管理所有测试相关的配置
"""

import os
import yaml
import json
from typing import Dict, Any, Optional
from pathlib import Path


class TestConfig:
    """测试配置管理器"""
    
    def __init__(self, config_file: Optional[str] = None):
        self._config = {}
        self._config_file = config_file or self._find_config_file()
        self._load_config()
    
    def _find_config_file(self) -> str:
        """查找配置文件"""
        # 可能的配置文件路径
        possible_paths = [
            'config/test-config.yaml',
            'config/test-config.yml',
            'test-config.yaml',
            'test-config.yml',
            '../config/test-config.yaml',
            '../../config/test-config.yaml'
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                return path
        
        # 如果没有找到配置文件，使用默认配置
        return ''
    
    def _load_config(self):
        """加载配置文件"""
        if self._config_file and os.path.exists(self._config_file):
            try:
                with open(self._config_file, 'r', encoding='utf-8') as f:
                    if self._config_file.endswith('.yaml') or self._config_file.endswith('.yml'):
                        self._config = yaml.safe_load(f)
                    else:
                        self._config = json.load(f)
            except Exception as e:
                print(f"加载配置文件失败: {e}")
                self._config = self._get_default_config()
        else:
            self._config = self._get_default_config()
    
    def _get_default_config(self) -> Dict[str, Any]:
        """获取默认配置"""
        return {
            'environment': {
                'name': 'test',
                'url': 'http://localhost:8080',
                'timeout': 30
            },
            'database': {
                'host': 'localhost',
                'port': 5432,
                'name': 'test_db',
                'user': 'test_user',
                'password': 'test_password'
            },
            'api': {
                'base_url': 'http://localhost:8080/api',
                'auth_token': 'test_token',
                'timeout': 30
            },
            'ui': {
                'browser': 'chrome',
                'headless': True,
                'slow_mo': 100,
                'viewport': {'width': 1920, 'height': 1080}
            },
            'message_queue': {
                'host': 'localhost',
                'port': 5672,
                'username': 'guest',
                'password': 'guest',
                'vhost': '/'
            },
            'logging': {
                'level': 'INFO',
                'directory': 'logs',
                'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
            },
            'report': {
                'directory': 'reports',
                'format': 'html',
                'include_screenshots': True
            },
            'test_data': {
                'directory': 'test_data',
                'cleanup_after_test': True,
                'generate_on_start': True
            },
            'performance': {
                'thresholds': {
                    'page_load': 3.0,  # 秒
                    'api_response': 1.0,  # 秒
                    'database_query': 0.5  # 秒
                }
            }
        }
    
    def get(self, key: str, default: Any = None) -> Any:
        """获取配置值"""
        keys = key.split('.')
        value = self._config
        
        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default
        
        return value
    
    def set(self, key: str, value: Any):
        """设置配置值"""
        keys = key.split('.')
        config = self._config
        
        for i, k in enumerate(keys[:-1]):
            if k not in config:
                config[k] = {}
            config = config[k]
        
        config[keys[-1]] = value
    
    def save(self, file_path: Optional[str] = None):
        """保存配置到文件"""
        save_path = file_path or self._config_file
        if not save_path:
            save_path = 'config/test-config.yaml'
        
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        
        with open(save_path, 'w', encoding='utf-8') as f:
            if save_path.endswith('.yaml') or save_path.endswith('.yml'):
                yaml.dump(self._config, f, default_flow_style=False, allow_unicode=True)
            else:
                json.dump(self._config, f, indent=2, ensure_ascii=False)
    
    def get_environment_config(self) -> Dict[str, Any]:
        """获取环境配置"""
        return self.get('environment', {})
    
    def get_database_config(self) -> Dict[str, Any]:
        """获取数据库配置"""
        return self.get('database', {})
    
    def get_api_config(self) -> Dict[str, Any]:
        """获取API配置"""
        return self.get('api', {})
    
    def get_ui_config(self) -> Dict[str, Any]:
        """获取UI配置"""
        return self.get('ui', {})
    
    def get_mq_config(self) -> Dict[str, Any]:
        """获取消息队列配置"""
        return self.get('message_queue', {})
    
    def get_test_data_config(self) -> Dict[str, Any]:
        """获取测试数据配置"""
        return self.get('test_data', {})
    
    def update_from_env(self):
        """从环境变量更新配置"""
        env_mappings = {
            'TEST_ENVIRONMENT_URL': 'environment.url',
            'TEST_DATABASE_HOST': 'database.host',
            'TEST_DATABASE_NAME': 'database.name',
            'TEST_API_BASE_URL': 'api.base_url',
            'TEST_API_AUTH_TOKEN': 'api.auth_token'
        }
        
        for env_var, config_key in env_mappings.items():
            env_value = os.getenv(env_var)
            if env_value:
                self.set(config_key, env_value)


if __name__ == "__main__":
    # 测试配置管理器
    config = TestConfig()
    
    print("当前配置:")
    print(f"环境URL: {config.get('environment.url')}")
    print(f"数据库主机: {config.get('database.host')}")
    print(f"API基础URL: {config.get('api.base_url')}")
    
    # 测试设置新值
    config.set('environment.name', 'staging')
    print(f"更新后的环境名称: {config.get('environment.name')}")
    
    # 保存配置
    config.save('test-config.yaml')
    print("配置已保存到 test-config.yaml")