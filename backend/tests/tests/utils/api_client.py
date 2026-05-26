#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API客户端工具

提供HTTP请求封装和响应处理
"""

import httpx
from typing import Dict, Any, Optional
from urllib.parse import urljoin


class APIClient:
    """API客户端"""
    
    def __init__(self, base_url: str, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.client = httpx.Client(timeout=timeout)
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    
    def set_token(self, token: str):
        """设置认证token"""
        self.headers["Authorization"] = f"Bearer {token}"
    
    def request(self, method: str, path: str, **kwargs) -> httpx.Response:
        """发送HTTP请求"""
        url = urljoin(self.base_url + "/", path.lstrip("/"))
        
        # 合并headers
        headers = kwargs.pop("headers", {})
        merged_headers = {**self.headers, **headers}
        
        response = self.client.request(
            method=method,
            url=url,
            headers=merged_headers,
            **kwargs
        )
        return response
    
    def get(self, path: str, **kwargs) -> httpx.Response:
        """GET请求"""
        return self.request("GET", path, **kwargs)
    
    def post(self, path: str, **kwargs) -> httpx.Response:
        """POST请求"""
        return self.request("POST", path, **kwargs)
    
    def put(self, path: str, **kwargs) -> httpx.Response:
        """PUT请求"""
        return self.request("PUT", path, **kwargs)
    
    def patch(self, path: str, **kwargs) -> httpx.Response:
        """PATCH请求"""
        return self.request("PATCH", path, **kwargs)
    
    def delete(self, path: str, **kwargs) -> httpx.Response:
        """DELETE请求"""
        return self.request("DELETE", path, **kwargs)
    
    def close(self):
        """关闭客户端"""
        self.client.close()
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


class AsyncAPIClient:
    """异步API客户端"""
    
    def __init__(self, base_url: str, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.client = httpx.AsyncClient(timeout=timeout)
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    
    def set_token(self, token: str):
        """设置认证token"""
        self.headers["Authorization"] = f"Bearer {token}"
    
    async def request(self, method: str, path: str, **kwargs) -> httpx.Response:
        """发送HTTP请求"""
        url = urljoin(self.base_url + "/", path.lstrip("/"))
        
        headers = kwargs.pop("headers", {})
        merged_headers = {**self.headers, **headers}
        
        response = await self.client.request(
            method=method,
            url=url,
            headers=merged_headers,
            **kwargs
        )
        return response
    
    async def get(self, path: str, **kwargs) -> httpx.Response:
        """GET请求"""
        return await self.request("GET", path, **kwargs)
    
    async def post(self, path: str, **kwargs) -> httpx.Response:
        """POST请求"""
        return await self.request("POST", path, **kwargs)
    
    async def put(self, path: str, **kwargs) -> httpx.Response:
        """PUT请求"""
        return await self.request("PUT", path, **kwargs)
    
    async def patch(self, path: str, **kwargs) -> httpx.Response:
        """PATCH请求"""
        return await self.request("PATCH", path, **kwargs)
    
    async def delete(self, path: str, **kwargs) -> httpx.Response:
        """DELETE请求"""
        return await self.request("DELETE", path, **kwargs)
    
    async def close(self):
        """关闭客户端"""
        await self.client.aclose()
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.close()
