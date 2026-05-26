#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 自动化测试框架配置
pytest fixtures and common utilities
"""

import pytest
import requests
import json
import os
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

# 测试配置
BASE_URL = os.environ.get("TEST_BASE_URL", "http://localhost:8080")
API_TIMEOUT = int(os.environ.get("API_TIMEOUT", 30))


@pytest.fixture(scope="session")
def api_client():
    """API客户端fixture"""
    class APIClient:
        def __init__(self, base_url=BASE_URL):
            self.base_url = base_url
            self.session = requests.Session()
            self.session.headers.update({
                "Content-Type": "application/json",
                "Accept": "application/json"
            })
        
        def get(self, path, params=None):
            """GET请求"""
            response = self.session.get(f"{self.base_url}{path}", params=params, timeout=API_TIMEOUT)
            return response
        
        def post(self, path, data=None, json_data=None):
            """POST请求"""
            response = self.session.post(f"{self.base_url}{path}", data=data, json=json_data, timeout=API_TIMEOUT)
            return response
        
        def put(self, path, data=None, json_data=None):
            """PUT请求"""
            response = self.session.put(f"{self.base_url}{path}", data=data, json=json_data, timeout=API_TIMEOUT)
            return response
        
        def delete(self, path):
            """DELETE请求"""
            response = self.session.delete(f"{self.base_url}{path}", timeout=API_TIMEOUT)
            return response
        
        def health_check(self):
            """健康检查"""
            return self.get("/api/health")
    
    client = APIClient()
    yield client
    client.session.close()


@pytest.fixture(scope="session")
def test_config():
    """测试配置fixture"""
    return {
        "base_url": BASE_URL,
        "timeout": API_TIMEOUT,
        "tenant_id": 1,
        "test_user": {
            "username": "test_user",
            "password": "test_password"
        }
    }


@pytest.fixture(scope="function")
def test_data():
    """测试数据fixture"""
    return {
        "user": {
            "username": "automation_test_user",
            "email": "automation@test.com",
            "phone": "13800138000"
        },
        "role": {
            "name": "automation_test_role",
            "code": "ATR001",
            "description": "Automation test role"
        }
    }


@pytest.fixture(autouse=True)
def setup_test_environment(api_client):
    """自动设置测试环境"""
    # 健康检查确保服务可用
    try:
        response = api_client.health_check()
        if response.status_code != 200:
            pytest.skip("API service not available")
    except requests.exceptions.ConnectionError:
        pytest.skip("Cannot connect to API service")


# Pytest hooks
def pytest_configure(config):
    """pytest配置钩子"""
    config.addinivalue_line(
        "markers", "api: mark test as API automation test"
    )
    config.addinivalue_line(
        "markers", "ui: mark test as UI automation test"
    )
    config.addinivalue_line(
        "markers", "smoke: mark test as smoke test"
    )
    config.addinivalue_line(
        "markers", "regression: mark test as regression test"
    )


def pytest_collection_modifyitems(config, items):
    """pytest收集修改钩子"""
    for item in items:
        if "api" in item.nodeid:
            item.add_marker(pytest.mark.api)
        if "ui" in item.nodeid:
            item.add_marker(pytest.mark.ui)