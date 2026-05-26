#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 本地测试配置
用于本地开发环境的单元测试，不依赖远程服务
"""

import os
import sys

# 测试环境配置
TEST_ENV = 'local'

# 数据库配置 - 本地模拟
DATABASE_CONFIG = {
    'local': {
        'type': 'sqlite',  # 使用SQLite进行本地测试
        'path': ':memory:',  # 内存数据库
        'echo': False
    },
    'test': {
        'type': 'postgresql',
        'host': 'localhost',
        'port': 5432,
        'database': 'ai_ready_test',
        'user': 'test_user',
        'password': 'test_password'
    }
}

# API配置 - 本地Mock
API_CONFIG = {
    'base_url': 'http://localhost:8080/api/v1',
    'timeout': 30,
    'retry_count': 3,
    'mock_enabled': True  # 本地测试启用Mock
}

# 用户测试数据
TEST_USERS = {
    'admin': {
        'id': 1,
        'username': 'admin',
        'password': 'Admin@123456',
        'email': 'admin@test.com',
        'phone': '13800138000',
        'real_name': '系统管理员',
        'department_id': 1,
        'position': '管理员',
        'status': 0,
        'user_type': 0,
        'tenant_id': 1
    },
    'user': {
        'id': 2,
        'username': 'testuser',
        'password': 'Test@123456',
        'email': 'testuser@test.com',
        'phone': '13900139000',
        'real_name': '测试用户',
        'department_id': 2,
        'position': '测试工程师',
        'status': 0,
        'user_type': 1,
        'tenant_id': 1
    },
    'manager': {
        'id': 3,
        'username': 'manager',
        'password': 'Manager@123456',
        'email': 'manager@test.com',
        'phone': '13700137000',
        'real_name': '项目经理',
        'department_id': 2,
        'position': '项目经理',
        'status': 0,
        'user_type': 1,
        'tenant_id': 1
    }
}

# 权限测试数据
TEST_PERMISSIONS = {
    'roles': [
        {'id': 1, 'role_name': '超级管理员', 'role_code': 'SUPER_ADMIN', 'permissions': ['*']},
        {'id': 2, 'role_name': '管理员', 'role_code': 'ADMIN', 'permissions': ['user:*', 'dept:*', 'role:read']},
        {'id': 3, 'role_name': '普通用户', 'role_code': 'USER', 'permissions': ['user:read', 'user:update:self']},
        {'id': 4, 'role_name': 'ERP管理员', 'role_code': 'ERP_ADMIN', 'permissions': ['erp:*']},
        {'id': 5, 'role_name': 'CRM管理员', 'role_code': 'CRM_ADMIN', 'permissions': ['crm:*']}
    ],
    'menus': [
        {'id': 1, 'menu_name': '系统管理', 'menu_code': 'system', 'path': '/system', 'parent_id': 0},
        {'id': 2, 'menu_name': '用户管理', 'menu_code': 'user', 'path': '/system/user', 'parent_id': 1},
        {'id': 3, 'menu_name': '角色管理', 'menu_code': 'role', 'path': '/system/role', 'parent_id': 1},
        {'id': 4, 'menu_name': 'ERP管理', 'menu_code': 'erp', 'path': '/erp', 'parent_id': 0},
        {'id': 5, 'menu_name': '库存管理', 'menu_code': 'stock', 'path': '/erp/stock', 'parent_id': 4},
        {'id': 6, 'menu_name': 'CRM管理', 'menu_code': 'crm', 'path': '/crm', 'parent_id': 0},
        {'id': 7, 'menu_name': '客户管理', 'menu_code': 'customer', 'path': '/crm/customer', 'parent_id': 6}
    ]
}

# ERP测试数据
TEST_ERP_DATA = {
    'accounts': [
        {'id': 1, 'code': '1001', 'name': '库存现金', 'type': 'asset', 'balance': 50000.00},
        {'id': 2, 'code': '1002', 'name': '银行存款', 'type': 'asset', 'balance': 500000.00},
        {'id': 3, 'code': '2001', 'name': '短期借款', 'type': 'liability', 'balance': 100000.00},
        {'id': 4, 'code': '3001', 'name': '实收资本', 'type': 'equity', 'balance': 400000.00},
        {'id': 5, 'code': '4001', 'name': '主营业务收入', 'type': 'revenue', 'balance': 200000.00},
        {'id': 6, 'code': '5001', 'name': '主营业务成本', 'type': 'expense', 'balance': 150000.00}
    ],
    'products': [
        {'id': 1, 'code': 'PROD001', 'name': '产品A', 'category': '电子', 'price': 100.00, 'stock': 1000, 'unit_cost': 80.00},
        {'id': 2, 'code': 'PROD002', 'name': '产品B', 'category': '家居', 'price': 200.00, 'stock': 500, 'unit_cost': 150.00},
        {'id': 3, 'code': 'PROD003', 'name': '产品C', 'category': '服装', 'price': 50.00, 'stock': 2000, 'unit_cost': 35.00}
    ],
    'warehouses': [
        {'id': 1, 'name': '主仓库', 'code': 'WH001', 'location': '北京', 'capacity': 10000},
        {'id': 2, 'name': '分仓库', 'code': 'WH002', 'location': '上海', 'capacity': 5000}
    ]
}

# CRM测试数据
TEST_CRM_DATA = {
    'customers': [
        {'id': 1, 'name': '测试公司A', 'type': '企业', 'level': 'A', 'phone': '010-12345678', 'email': 'companyA@test.com'},
        {'id': 2, 'name': '测试公司B', 'type': '企业', 'level': 'B', 'phone': '010-87654321', 'email': 'companyB@test.com'},
        {'id': 3, 'name': '个人客户C', 'type': '个人', 'level': 'C', 'phone': '13800138001', 'email': 'personC@test.com'}
    ],
    'leads': [
        {'id': 1, 'customer_id': 1, 'source': '网站', 'status': '新线索', 'name': '产品咨询', 'expected_amount': 50000},
        {'id': 2, 'customer_id': 2, 'source': '电话', 'status': '跟进中', 'name': '批量采购', 'expected_amount': 100000},
        {'id': 3, 'customer_id': 3, 'source': '推荐', 'status': '已转化', 'name': '试用申请', 'expected_amount': 10000}
    ],
    'opportunities': [
        {'id': 1, 'customer_id': 1, 'name': '企业采购项目', 'amount': 50000, 'stage': '需求确认', 'win_rate': 30},
        {'id': 2, 'customer_id': 2, 'name': '年度合作', 'amount': 200000, 'stage': '方案提供', 'win_rate': 60},
        {'id': 3, 'customer_id': 3, 'name': '首次购买', 'amount': 5000, 'stage': '成交阶段', 'win_rate': 90}
    ],
    'activities': [
        {'id': 1, 'type': 'call', 'customer_id': 1, 'description': '电话回访', 'duration': 15},
        {'id': 2, 'type': 'visit', 'customer_id': 2, 'description': '上门拜访', 'duration': 60},
        {'id': 3, 'type': 'email', 'customer_id': 3, 'description': '发送报价', 'duration': 5}
    ]
}

# 测试配置
TEST_SETTINGS = {
    'parallel_enabled': True,
    'max_workers': 4,
    'timeout': 300,
    'coverage_enabled': True,
    'coverage_threshold': 80,
    'report_format': 'html',
    'log_level': 'INFO'
}