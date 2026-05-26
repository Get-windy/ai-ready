#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试框架入口 - 定义全局fixture和配置
"""

import pytest
import os
from typing import Dict, Any


def pytest_addoption(parser):
    """添加pytest命令行选项"""
    parser.addoption(
        "--env",
        action="store",
        default="dev",
        help="测试环境: dev/test/staging/prod"
    )
    parser.addoption(
        "--smoke",
        action="store_true",
        help="只运行冒烟测试"
    )
    parser.addoption(
        "--api-only",
        action="store_true",
        help="只运行API测试"
    )
    parser.addoption(
        "--ui-only",
        action="store_true",
        help="只运行UI测试"
    )


@pytest.fixture(scope='session')
def api_base_url(request):
    """获取API基础URL"""
    env = request.config.getoption("--env")
    base_urls = {
        'dev': 'http://localhost:8080/api/v1',
        'test': 'http://test.ai-ready.local/api/v1',
        'staging': 'http://staging.ai-ready.local/api/v1',
        'prod': 'https://api.ai-ready.local/api/v1'
    }
    return base_urls.get(env, base_urls['dev'])


@pytest.fixture(scope='session')
def ui_base_url(request):
    """获取UI基础URL"""
    env = request.config.getoption("--env")
    base_urls = {
        'dev': 'http://localhost:3000',
        'test': 'http://test.ai-ready.local',
        'staging': 'http://staging.ai-ready.local',
        'prod': 'https://www.ai-ready.local'
    }
    return base_urls.get(env, base_urls['dev'])


@pytest.fixture(scope='module')
def test_user(api_base_url):
    """创建测试用户fixture"""
    from utils.api_client import APIClient
    from utils.data_generator import DataGenerator
    
    client = APIClient(base_url=api_base_url)
    generator = DataGenerator()
    
    # 生成测试用户数据
    user_data = generator.generate_user()
    
    # 注册用户
    response = client.post('/users/register', json=user_data)
    
    if response.status_code == 201:
        user_id = response.json()['data']['id']
        
        # 返回用户数据和ID
        yield user_data, user_id
        
        # 清理: 删除用户
        client.delete(f'/users/{user_id}')
    else:
        pytest.fail(f"用户注册失败: {response.text}")


@pytest.fixture(scope='module')
def auth_token(api_base_url, test_user):
    """获取认证token fixture"""
    from utils.api_client import APIClient
    
    user_data, user_id = test_user
    client = APIClient(base_url=api_base_url)
    
    # 登录获取token
    response = client.post('/users/login', json={
        'username': user_data['username'],
        'password': user_data['password']
    })
    
    if response.status_code == 200:
        token = response.json()['data']['token']
        yield token
    else:
        pytest.fail(f"登录失败: {response.text}")