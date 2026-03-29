#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API客户端 - 封装HTTP请求
"""

import requests
from typing import Optional, Dict, Any


class APIClient:
    """API客户端封装"""
    
    def __init__(self, base_url: str, timeout: int = 30):
        """
        初始化API客户端
        
        Args:
            base_url: API基础URL
            timeout: 请求超时时间(秒)
        """
        self.base_url = base_url
        self.timeout = timeout
        self.session = requests.Session()
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'AI-Ready-Test-Automation/1.0'
        }
    
    def set_auth_token(self, token: str):
        """设置认证Token"""
        self.headers['Authorization'] = f'Bearer {token}'
    
    def get(self, endpoint: str, **kwargs) -> requests.Response:
        """
        GET请求
        
        Args:
            endpoint: API端点(相对于base_url)
            **kwargs: 其他请求参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.base_url}{endpoint}"
        return self.session.get(
            url, 
            headers=self.headers,
            timeout=self.timeout, 
            **kwargs
        )
    
    def post(self, endpoint: str, json: Dict[str, Any] = None, **kwargs) -> requests.Response:
        """
        POST请求
        
        Args:
            endpoint: API端点
            json: JSON数据
            **kwargs: 其他请求参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.base_url}{endpoint}"
        return self.session.post(
            url, 
            json=json,
            headers=self.headers,
            timeout=self.timeout, 
            **kwargs
        )
    
    def put(self, endpoint: str, json: Dict[str, Any] = None, **kwargs) -> requests.Response:
        """
        PUT请求
        
        Args:
            endpoint: API端点
            json: JSON数据
            **kwargs: 其他请求参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.base_url}{endpoint}"
        return self.session.put(
            url, 
            json=json,
            headers=self.headers,
            timeout=self.timeout, 
            **kwargs
        )
    
    def delete(self, endpoint: str, **kwargs) -> requests.Response:
        """
        DELETE请求
        
        Args:
            endpoint: API端点
            **kwargs: 其他请求参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.base_url}{endpoint}"
        return self.session.delete(
            url, 
            headers=self.headers,
            timeout=self.timeout, 
            **kwargs
        )
    
    def patch(self, endpoint: str, json: Dict[str, Any] = None, **kwargs) -> requests.Response:
        """
        PATCH请求
        
        Args:
            endpoint: API端点
            json: JSON数据
            **kwargs: 其他请求参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.base_url}{endpoint}"
        return self.session.patch(
            url, 
            json=json,
            headers=self.headers,
            timeout=self.timeout, 
            **kwargs
        )