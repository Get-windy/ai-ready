#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
重试机制 - 处理不稳定测试
"""

import time
import functools
from typing import Callable, Type, Tuple
import logging

logger = logging.getLogger(__name__)


def retry(
    max_attempts: int = 3,
    delay: float = 1.0,
    backoff: float = 2.0,
    exceptions: Tuple[Type[Exception], ...] = (Exception,)
):
    """
    重试装饰器
    
    Args:
        max_attempts: 最大尝试次数
        delay: 初始延迟（秒）
        backoff: 延迟增长倍数
        exceptions: 要捕获的异常类型
    
    Example:
        @retry(max_attempts=3, delay=1)
        def test_api_call():
            response = client.get('/api/endpoint')
            assert response.status_code == 200
    """
    def decorator(func: Callable) -> Callable:
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            current_delay = delay
            last_exception = None
            
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    last_exception = e
                    logger.warning(
                        f"第 {attempt}/{max_attempts} 次尝试失败: {e}"
                    )
                    
                    if attempt < max_attempts:
                        logger.info(f"等待 {current_delay} 秒后重试...")
                        time.sleep(current_delay)
                        current_delay *= backoff
            
            # 所有尝试都失败
            logger.error(f"所有 {max_attempts} 次尝试都失败")
            raise last_exception
        
        return wrapper
    return decorator


class RetryHelper:
    """重试辅助类"""
    
    def __init__(
        self,
        max_attempts: int = 3,
        delay: float = 1.0,
        backoff: float = 2.0
    ):
        self.max_attempts = max_attempts
        self.delay = delay
        self.backoff = backoff
    
    def execute(self, func: Callable, *args, **kwargs):
        """
        执行带重试的函数
        
        Args:
            func: 要执行的函数
            *args: 函数参数
            **kwargs: 函数关键字参数
        
        Returns:
            函数返回值
        """
        current_delay = self.delay
        last_exception = None
        
        for attempt in range(1, self.max_attempts + 1):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                last_exception = e
                logger.warning(f"第 {attempt} 次尝试失败: {e}")
                
                if attempt < self.max_attempts:
                    time.sleep(current_delay)
                    current_delay *= self.backoff
        
        raise last_exception