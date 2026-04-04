#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
配置加载器 - 环境配置管理
"""

import os
import yaml
from typing import Dict, Any
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class ConfigLoader:
    """配置加载器"""
    
    def __init__(self, config_dir: str = None):
        """
        初始化配置加载器
        
        Args:
            config_dir: 配置目录路径
        """
        self.config_dir = Path(config_dir) if config_dir else Path('configs')
        self.config_cache: Dict[str, Any] = {}
        self._load_all_configs()
    
    def _load_all_configs(self):
        """加载所有配置文件"""
        env = os.environ.get('TEST_ENV', 'dev')
        
        # 加载基础配置
        base_config_file = self.config_dir / 'base.yaml'
        if base_config_file.exists():
            self.config_cache['base'] = self._load_yaml(base_config_file)
        
        # 加载环境配置
        env_config_file = self.config_dir / f'{env}.yaml'
        if env_config_file.exists():
            self.config_cache['env'] = self._load_yaml(env_config_file)
        else:
            self.config_cache['env'] = {}
        
        logger.info(f"已加载配置，环境: {env}")
    
    def _load_yaml(self, file_path: Path) -> Dict:
        """加载YAML文件"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f) or {}
        except Exception as e:
            logger.error(f"加载配置文件 {file_path} 失败: {e}")
            return {}
    
    def get(self, key: str, default: Any = None) -> Any:
        """
        获取配置值
        
        Args:
            key: 配置键（支持嵌套，如 'api.timeout'）
            default: 默认值
        
        Returns:
            配置值
        """
        # 合并基础和环境配置
        merged_config = {**self.config_cache.get('base', {}), 
                        **self.config_cache.get('env', {})}
        
        # 支持嵌套键
        keys = key.split('.')
        value = merged_config
        
        for k in keys:
            if isinstance(value, dict):
                value = value.get(k)
            else:
                return default
        
        return value if value is not None else default
    
    def get_all(self) -> Dict:
        """获取所有配置"""
        return {**self.config_cache.get('base', {}), 
                **self.config_cache.get('env', {})}