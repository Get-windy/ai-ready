#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API自动化测试配置文件
提供测试 fixtures 和公共配置
"""

import pytest
import requests
import json
import os
from typing import Dict, Any, Optional


# ========== 配置常量 ==========
BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:8080')
TEST_TIMEOUT = int(os.getenv('TEST_TIMEOUT', '30'))
RETRY_COUNT = int(os.getenv('RETRY_COUNT', '3'))


# ========== API客户端类 ==========
class APIClient:
    """REST API客户端封装"""
    
    def __init__(self, base_url: str, timeout: int = TEST_TIMEOUT):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
        self.token: Optional[str] = None
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    
    def set_auth_token(self, token: str):
        """设置认证Token"""
        self.token = token
        self.headers['Authorization'] = f'Bearer {token}'
    
    def _request(self, method: str, path: str, **kwargs) -> requests.Response:
        """发送HTTP请求"""
        url = f'{self.base_url}{path}'
        kwargs.setdefault('timeout', self.timeout)
        kwargs.setdefault('headers', self.headers)
        
        # 添加重试逻辑
        for attempt in range(RETRY_COUNT):
            try:
                response = self.session.request(method, url, **kwargs)
                return response
            except requests.exceptions.RequestException as e:
                if attempt == RETRY_COUNT - 1:
                    raise
                continue
    
    def get(self, path: str, params: Dict = None) -> requests.Response:
        """GET请求"""
        return self._request('GET', path, params=params)
    
    def post(self, path: str, json: Dict = None, data: Dict = None) -> requests.Response:
        """POST请求"""
        return self._request('POST', path, json=json, data=data)
    
    def put(self, path: str, json: Dict = None) -> requests.Response:
        """PUT请求"""
        return self._request('PUT', path, json=json)
    
    def delete(self, path: str, params: Dict = None) -> requests.Response:
        """DELETE请求"""
        return self._request('DELETE', path, params=params)
    
    def patch(self, path: str, json: Dict = None) -> requests.Response:
        """PATCH请求"""
        return self._request('PATCH', path, json=json)


# ========== pytest fixtures ==========
@pytest.fixture(scope='session')
def api_base_url():
    """API基础URL"""
    return BASE_URL


@pytest.fixture(scope='session')
def api_client(api_base_url):
    """API客户端(会话级别)"""
    return APIClient(api_base_url)


@pytest.fixture(scope='session')
def auth_token(api_client):
    """获取认证Token"""
    # 尝试登录获取Token
    login_data = {
        'username': 'admin',
        'password': 'admin123'
    }
    
    response = api_client.post('/api/login', json=login_data)
    
    if response.status_code == 200:
        data = response.json()
        # 根据实际API返回格式提取Token
        token = data.get('data', data.get('token', None))
        if token:
            api_client.set_auth_token(token)
            return token
    
    # 如果登录失败，返回空Token(部分测试可能不需要认证)
    return None


@pytest.fixture(scope='function')
def clean_test_data(api_client, auth_token):
    """清理测试数据"""
    # 测试前准备
    yield
    
    # 测试后清理(可选)
    # 根据项目需求清理创建的测试数据


@pytest.fixture(scope='session')
def test_config():
    """测试配置"""
    return {
        'base_url': BASE_URL,
        'timeout': TEST_TIMEOUT,
        'retry_count': RETRY_COUNT,
        'test_user': {
            'username': 'test_user',
            'password': 'test123',
            'email': 'test@example.com'
        }
    }


# ========== pytest hooks ==========
def pytest_configure(config):
    """pytest配置钩子"""
    # 注册自定义标记
    config.addinivalue_line('markers', 'smoke: 冒烟测试标记')
    config.addinivalue_line('markers', 'erp: ERP模块测试')
    config.addinivalue_line('markers', 'crm: CRM模块测试')
    config.addinivalue_line('markers', 'user: 用户模块测试')
    config.addinivalue_line('markers', 'slow: 慢速测试标记')


def pytest_collection_modifyitems(config, items):
    """修改测试项"""
    # 为没有标记的测试添加默认标记
    for item in items:
        if not any(item.get_closest_marker(name) for name in ['smoke', 'erp', 'crm', 'user', 'slow']):
            item.add_marker(pytest.mark.general)


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """生成测试报告"""
    outcome = yield
    report = outcome.get_result()
    
    # 记录失败详情
    if report.when == 'call' and report.failed:
        # 可以在这里添加失败截图、日志等
        pass