#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API自动化测试 - 用户相关API
"""

import pytest
import json
from typing import Dict, Any


class TestUserAPI:
    """用户API测试"""
    
    @pytest.fixture(scope='class')
    def api_client(self, api_base_url):
        """API客户端fixture"""
        from utils.api_client import APIClient
        return APIClient(base_url=api_base_url)
    
    @pytest.fixture(scope='class')
    def token_client(self, api_base_url, auth_token):
        """带认证Token的API客户端"""
        from utils.api_client import APIClient
        client = APIClient(base_url=api_base_url)
        client.set_auth_token(auth_token)
        return client
    
    def test_user_registration(self, api_client):
        """测试用户注册"""
        # 生成测试数据
        generator = DataGenerator()
        user_data = generator.generate_user()
        
        # 发送注册请求
        response = api_client.post('/users/register', json=user_data)
        
        # 验证响应
        assert response.status_code == 201, f"注册失败: {response.text}"
        data = response.json()['data']
        assert data['username'] == user_data['username']
        assert 'id' in data
        
    def test_user_login(self, api_client):
        """测试用户登录"""
        # 生成测试数据
        generator = DataGenerator()
        user_data = generator.generate_user()
        
        # 先注册
        api_client.post('/users/register', json=user_data)
        
        # 登录
        response = api_client.post('/users/login', json={
            'username': user_data['username'],
            'password': user_data['password']
        })
        
        # 验证响应
        assert response.status_code == 200, f"登录失败: {response.text}"
        data = response.json()['data']
        assert 'token' in data
        assert 'expiresAt' in data
        
    @pytest.mark.smoke
    def test_get_current_user(self, token_client):
        """测试获取当前用户信息(冒烟)"""
        response = token_client.get('/users/me')
        
        assert response.status_code == 200, f"获取用户信息失败: {response.text}"
        data = response.json()['data']
        assert 'id' in data
        assert 'username' in data
        assert 'email' in data
        
    def test_update_user_profile(self, token_client):
        """测试更新用户资料"""
        # 获取当前用户信息
        response = token_client.get('/users/me')
        user_id = response.json()['data']['id']
        
        # 更新资料
        update_data = {
            'realName': 'Test User Updated',
            'phone': '13800138000',
            'avatar': 'https://example.com/avatar.jpg'
        }
        response = token_client.put(f'/users/{user_id}', json=update_data)
        
        # 验证响应
        assert response.status_code == 200, f"更新资料失败: {response.text}"
        data = response.json()['data']
        assert data['realName'] == update_data['realName']
        assert data['phone'] == update_data['phone']
        
    def test_user_logout(self, token_client):
        """测试用户登出"""
        response = token_client.post('/users/logout')
        
        assert response.status_code == 200, f"登出失败: {response.text}"
        data = response.json()
        assert data['code'] == 200


class TestProductAPI:
    """产品API测试"""
    
    @pytest.fixture(scope='class')
    def api_client(self, api_base_url):
        """API客户端fixture"""
        from utils.api_client import APIClient
        return APIClient(base_url=api_base_url)
    
    @pytest.fixture(scope='class')
    def token_client(self, api_base_url, auth_token):
        """带认证Token的API客户端"""
        from utils.api_client import APIClient
        client = APIClient(base_url=api_base_url)
        client.set_auth_token(auth_token)
        return client
    
    def test_get_products_list(self, api_client):
        """测试获取产品列表"""
        response = api_client.get('/products', params={
            'page': 1,
            'pageSize': 10
        })
        
        assert response.status_code == 200, f"获取产品列表失败: {response.text}"
        data = response.json()['data']
        assert 'list' in data
        assert 'total' in data
        assert len(data['list']) <= 10
        
    def test_get_product_detail(self, api_client):
        """测试获取产品详情"""
        # 先获取产品列表
        response = api_client.get('/products', params={
            'page': 1,
            'pageSize': 1
        })
        products = response.json()['data']['list']
        
        if products:
            product_id = products[0]['id']
            response = api_client.get(f'/products/{product_id}')
            
            assert response.status_code == 200, f"获取产品详情失败: {response.text}"
            data = response.json()['data']
            assert 'id' in data
            assert 'name' in data
            assert 'price' in data
            
    def test_search_products(self, api_client):
        """测试产品搜索"""
        response = api_client.get('/products/search', params={
            'keyword': '测试',
            'page': 1,
            'pageSize': 10
        })
        
        assert response.status_code == 200, f"产品搜索失败: {response.text}"
        data = response.json()['data']
        assert 'list' in data
        assert 'total' in data


class TestOrderAPI:
    """订单API测试"""
    
    @pytest.fixture(scope='class')
    def api_client(self, api_base_url):
        """API客户端fixture"""
        from utils.api_client import APIClient
        return APIClient(base_url=api_base_url)
    
    @pytest.fixture(scope='class')
    def token_client(self, api_base_url, auth_token):
        """带认证Token的API客户端"""
        from utils.api_client import APIClient
        client = APIClient(base_url=api_base_url)
        client.set_auth_token(auth_token)
        return client
    
    def test_create_order(self, token_client):
        """测试创建订单"""
        generator = DataGenerator()
        order_data = generator.generate_order(product_count=1)
        
        response = token_client.post('/orders', json=order_data)
        
        # 验证响应
        assert response.status_code == 201, f"创建订单失败: {response.text}"
        data = response.json()['data']
        assert 'id' in data
        assert data['totalAmount'] == order_data['totalAmount']
        
    def test_get_orders_list(self, token_client):
        """测试获取订单列表"""
        response = token_client.get('/orders', params={
            'page': 1,
            'pageSize': 10,
            'status': 'pending'
        })
        
        assert response.status_code == 200, f"获取订单列表失败: {response.text}"
        data = response.json()['data']
        assert 'list' in data
        assert 'total' in data
        
    def test_get_order_detail(self, token_client):
        """测试获取订单详情"""
        # 获取订单列表
        response = token_client.get('/orders', params={
            'page': 1,
            'pageSize': 1
        })
        orders = response.json()['data']['list']
        
        if orders:
            order_id = orders[0]['id']
            response = token_client.get(f'/orders/{order_id}')
            
            assert response.status_code == 200, f"获取订单详情失败: {response.text}"
            data = response.json()['data']
            assert 'id' in data
            assert 'status' in data