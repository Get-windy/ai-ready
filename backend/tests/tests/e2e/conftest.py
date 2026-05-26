#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready E2E测试配置文件
"""

import pytest
import os


def pytest_configure(config):
    """pytest配置"""
    # 注册自定义标记
    config.addinivalue_line(
        "markers", "e2e: end-to-end test"
    )
    config.addinivalue_line(
        "markers", "user_flow: user registration/login flow test"
    )
    config.addinivalue_line(
        "markers", "erp_flow: ERP order processing flow test"
    )
    config.addinivalue_line(
        "markers", "crm_flow: CRM customer management flow test"
    )
    config.addinivalue_line(
        "markers", "multi_module: multi-module collaboration test"
    )


def pytest_collection_modifyitems(config, items):
    """为所有测试添加e2e标记"""
    for item in items:
        if "e2e" in str(item.fspath):
            item.add_marker(pytest.mark.e2e)
