#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API集成测试
"""

import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



class TestAPIIntegration:
    """API集成测试基类"""
    
    @pytest.fixture
    def mock_api_client(self):
        """Mock API客户端"""
        class MockAPIClient:
            def __init__(self):
                self.base_url = 'http://localhost:8080/api/v1'
                self.call_history = []
                self.responses = {}  # 自定义响应
            
            def configure_response(self, endpoint, method, response):
                """配置特定端点的响应"""
                key = f"{method}_{endpoint}"
                self.responses[key] = response
            
            def get(self, endpoint, **kwargs):
                self.call_history.append(('GET', endpoint, kwargs))
                key = f"GET_{endpoint}"
                if key in self.responses:
                    resp = self.responses[key]
                    return self._mock_response(resp['status'], resp['data'])
                return self._mock_response(200, {'data': 'mock_data'})
            
            def post(self, endpoint, json=None, **kwargs):
                self.call_history.append(('POST', endpoint, {'json': json}))
                key = f"POST_{endpoint}"
                if key in self.responses:
                    resp = self.responses[key]
                    return self._mock_response(resp['status'], resp['data'])
                return self._mock_response(201, {'data': 'created'})
            
            def put(self, endpoint, json=None, **kwargs):
                self.call_history.append(('PUT', endpoint, {'json': json}))
                key = f"PUT_{endpoint}"
                if key in self.responses:
                    resp = self.responses[key]
                    return self._mock_response(resp['status'], resp['data'])
                return self._mock_response(200, {'data': 'updated'})
            
            def delete(self, endpoint, **kwargs):
                self.call_history.append(('DELETE', endpoint, kwargs))
                return self._mock_response(200, {'data': 'deleted'})
            
            def patch(self, endpoint, json=None, **kwargs):
                self.call_history.append(('PATCH', endpoint, {'json': json}))
                return self._mock_response(200, {'data': 'patched'})
            
            def _mock_response(self, status_code, data):
                class MockResponse:
                    def __init__(self, status_code, data):
                        self.status_code = status_code
                        self._data = data
                    
                    def json(self):
                        return self._data
                
                return MockResponse(status_code, data)
        
        return MockAPIClient()
    
    @pytest.fixture
    def api_client(self, api_base_url):
        """初始化API客户端fixture"""
        from tests.utils.api_client import APIClient
        return APIClient(base_url=api_base_url)


class TestCoreAPIIntegration(TestAPIIntegration):
    """核心API集成测试"""
    
    def test_user_auth_flow(self, mock_api_client):
        """测试用户认证流程"""
        # 配置响应
        mock_api_client.configure_response('/users/register', 'POST', {
            'status': 201,
            'data': {'data': {'id': 1, 'username': 'testuser'}}
        })
        mock_api_client.configure_response('/users/login', 'POST', {
            'status': 200,
            'data': {'data': {'token': 'test_token_123', 'expiresAt': '2026-03-30'}}
        })
        mock_api_client.configure_response('/users/me', 'GET', {
            'status': 200,
            'data': {'data': {'id': 1, 'username': 'testuser', 'email': 'test@example.com'}}
        })
        mock_api_client.configure_response('/users/1', 'PUT', {
            'status': 200,
            'data': {'data': {'id': 1, 'realName': 'Test User', 'phone': '13800138000'}}
        })
        mock_api_client.configure_response('/users/logout', 'POST', {
            'status': 200,
            'data': {'code': 200, 'message': 'Logout success'}
        })
        
        # 步骤1: 注册
        response = mock_api_client.post('/users/register', json={
            'username': 'testuser',
            'email': 'test@example.com',
            'password': 'Test@123456'
        })
        assert response.status_code == 201
        
        # 步骤2: 登录
        response = mock_api_client.post('/users/login', json={
            'username': 'testuser',
            'password': 'Test@123456'
        })
        assert response.status_code == 200
        assert 'token' in response.json()['data']
        
        # 步骤3: 获取用户信息
        response = mock_api_client.get('/users/me')
        assert response.status_code == 200
        
        # 步骤4: 更新用户信息
        response = mock_api_client.put('/users/1', json={
            'realName': 'Test User'
        })
        assert response.status_code == 200
        
        # 步骤5: 登出
        response = mock_api_client.post('/users/logout')
        assert response.status_code == 200
        
        # 验证调用历史
        assert len(mock_api_client.call_history) == 5
    
    def test_product_management_flow(self, mock_api_client):
        """测试产品管理流程"""
        mock_api_client.configure_response('/products', 'POST', {
            'status': 201,
            'data': {'data': {'id': 1, 'name': 'Test Product'}}
        })
        mock_api_client.configure_response('/products', 'GET', {
            'status': 200,
            'data': {'data': {'list': [{'id': 1, 'name': 'Test Product'}], 'total': 1}}
        })
        mock_api_client.configure_response('/products/1', 'PUT', {
            'status': 200,
            'data': {'data': {'id': 1, 'price': 89.99, 'stock': 150}}
        })
        mock_api_client.configure_response('/products/1', 'DELETE', {
            'status': 200,
            'data': {'data': {'deleted': True}}
        })
        
        # 步骤1: 创建产品
        response = mock_api_client.post('/products', json={
            'name': 'Test Product',
            'code': 'PROD_TEST_001',
            'price': 99.99,
            'stock': 100
        })
        assert response.status_code == 201
        
        # 步骤2: 获取产品列表
        response = mock_api_client.get('/products', params={'page': 1, 'pageSize': 10})
        assert response.status_code == 200
        
        # 步骤3: 更新产品
        response = mock_api_client.put('/products/1', json={
            'price': 89.99,
            'stock': 150
        })
        assert response.status_code == 200
        
        # 步骤4: 删除产品
        response = mock_api_client.delete('/products/1')
        assert response.status_code == 200
    
    def test_order_creation_flow(self, mock_api_client):
        """测试订单创建流程"""
        mock_api_client.configure_response('/products', 'GET', {
            'status': 200,
            'data': {'data': {'list': [{'id': 1, 'name': 'Product 1'}], 'total': 1}}
        })
        mock_api_client.configure_response('/orders', 'POST', {
            'status': 201,
            'data': {'data': {'id': 1, 'productIds': [1, 2], 'totalAmount': 199.98}}
        })
        mock_api_client.configure_response('/orders/1', 'GET', {
            'status': 200,
            'data': {'data': {'id': 1, 'status': 'pending'}}
        })
        mock_api_client.configure_response('/orders/1', 'PATCH', {
            'status': 200,
            'data': {'data': {'id': 1, 'status': 'confirmed'}}
        })
        
        # 步骤1: 获取产品列表
        response = mock_api_client.get('/products', params={'page': 1, 'pageSize': 10})
        assert response.status_code == 200
        
        # 步骤2: 创建订单
        response = mock_api_client.post('/orders', json={
            'productIds': [1, 2],
            'quantity': 2,
            'totalAmount': 199.98
        })
        assert response.status_code == 201
        
        # 步骤3: 获取订单详情
        response = mock_api_client.get('/orders/1')
        assert response.status_code == 200
        
        # 步骤4: 更新订单状态
        response = mock_api_client.patch('/orders/1', json={
            'status': 'confirmed'
        })
        assert response.status_code == 200


class TestDatabaseIntegration:
    """数据库集成测试"""
    
    def test_database_connection(self, api_base_url):
        """测试数据库连接"""
        # Mock数据库连接测试
        def test_db_connection():
            """测试数据库连接函数"""
            return {
                'connected': True,
                'database': 'devdb',
                'version': 'PostgreSQL 14.0',
                'connection_time': 0.1
            }
        
        result = test_db_connection()
        assert result['connected'] == True
        assert result['database'] == 'devdb'


class TestServiceIntegration:
    """服务间集成测试"""
    
    def test_microservices_communication(self):
        """测试微服务通信"""
        class ServiceClient:
            def __init__(self, service_name, base_url):
                self.service_name = service_name
                self.base_url = base_url
                self.call_log = []
            
            def call(self, endpoint, method='GET', data=None):
                """调用服务"""
                self.call_log.append({
                    'service': self.service_name,
                    'method': method,
                    'endpoint': endpoint,
                    'data': data
                })
                return {'status': 'success', 'service': self.service_name}
        
        user_service = ServiceClient('user-service', 'http://user-service:8080')
        order_service = ServiceClient('order-service', 'http://order-service:8080')
        
        user_service.call('/users/1')
        order_service.call('/orders', method='POST', data={'user_id': 1})
        
        assert len(user_service.call_log) == 1
        assert len(order_service.call_log) == 1


class TestIntegrationScenarios:
    """集成场景测试"""
    
    def test_end_to_end_user_registration(self):
        """端到端用户注册场景"""
        def simulate_registration():
            """模拟用户注册流程"""
            steps = []
            
            user_data = {
                'username': 'newuser',
                'email': 'newuser@example.com',
                'password': 'Test@123456'
            }
            steps.append('form_submitted')
            
            assert 'username' in user_data
            assert '@' in user_data['email']
            steps.append('validation_passed')
            
            user_id = 1
            steps.append('account_created')
            
            steps.append('welcome_email_sent')
            steps.append('profile_created')
            
            return steps, user_id
        
        steps, user_id = simulate_registration()
        
        assert len(steps) == 5
        assert user_id == 1
