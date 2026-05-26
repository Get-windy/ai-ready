"""
自动化测试框架配置模块
管理测试环境配置、测试数据配置和框架设置
"""

import os
import yaml
import logging
from typing import Dict, Any, Optional

class TestConfig:
    """测试配置管理类"""
    
    def __init__(self, config_file: str = None):
        """
        初始化测试配置
        
        Args:
            config_file: 配置文件路径，如果为None则使用默认配置
        """
        self.config = {}
        self.logger = logging.getLogger(__name__)
        
        if config_file and os.path.exists(config_file):
            self.load_config(config_file)
        else:
            self.load_default_config()
    
    def load_config(self, config_file: str):
        """从YAML文件加载配置"""
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                self.config = yaml.safe_load(f)
            self.logger.info(f"成功加载配置文件: {config_file}")
        except Exception as e:
            self.logger.error(f"加载配置文件失败: {e}")
            self.load_default_config()
    
    def load_default_config(self):
        """加载默认配置"""
        self.config = {
            'test': {
                'environment': 'test',
                'base_url': 'http://localhost:8080',
                'timeout': 30,
                'retry_count': 3,
                'retry_delay': 1
            },
            'database': {
                'host': 'localhost',
                'port': 5432,
                'name': 'ai_ready_test',
                'user': 'test_user',
                'password': 'test_password'
            },
            'logging': {
                'level': 'INFO',
                'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                'file': 'test_automation.log'
            },
            'reporting': {
                'format': 'html',
                'output_dir': './reports',
                'screenshot_on_failure': True
            },
            'data': {
                'fixtures_dir': './test_data',
                'generators_dir': './data_generators'
            }
        }
        self.logger.info("使用默认配置")
    
    def get(self, key: str, default: Any = None) -> Any:
        """获取配置值"""
        keys = key.split('.')
        value = self.config
        
        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default
        
        return value
    
    def set(self, key: str, value: Any):
        """设置配置值"""
        keys = key.split('.')
        config = self.config
        
        for i, k in enumerate(keys[:-1]):
            if k not in config:
                config[k] = {}
            config = config[k]
        
        config[keys[-1]] = value
        self.logger.debug(f"设置配置: {key} = {value}")

# 全局配置实例
test_config = TestConfig()