#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
core-api模块单元测试
"""

import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestCoreAPIUnit:
    """core-api模块单元测试"""
    
    def test_api_client_initialization(self):
        """测试API客户端初始化"""
        from tests.utils.api_client import APIClient
        
        client = APIClient(base_url='http://localhost:8080/api/v1')
        
        assert client.base_url == 'http://localhost:8080/api/v1'
        assert client.timeout == 30
        assert client.session is not None
        assert 'Content-Type' in client.headers
        assert 'User-Agent' in client.headers
    
    def test_api_client_set_auth_token(self):
        """测试设置认证Token"""
        from tests.utils.api_client import APIClient
        
        client = APIClient(base_url='http://localhost:8080/api/v1')
        client.set_auth_token('test_token_12345')
        
        assert client.headers['Authorization'] == 'Bearer test_token_12345'
    
    def test_data_generator_user(self):
        """测试用户数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        user_data = generator.generate_user()
        
        assert 'username' in user_data
        assert 'email' in user_data
        assert 'phone' in user_data
        assert 'password' in user_data
        assert 'confirmPassword' in user_data
        assert 'realName' in user_data
        assert 'departmentId' in user_data
        assert 'position' in user_data
    
    def test_data_generator_order(self):
        """测试订单数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        order_data = generator.generate_order(product_count=3)
        
        assert 'productIds' in order_data
        assert 'quantity' in order_data
        assert 'note' in order_data
        assert 'totalAmount' in order_data
        assert len(order_data['productIds']) == 3
    
    def test_data_generator_product(self):
        """测试产品数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        product_data = generator.generate_product()
        
        assert 'name' in product_data
        assert 'code' in product_data
        assert 'category' in product_data
        assert 'price' in product_data
        assert 'stock' in product_data
        assert 'description' in product_data
    
    def test_data_generator_customer(self):
        """测试客户数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        customer_data = generator.generate_customer()
        
        assert 'name' in customer_data
        assert 'type' in customer_data
        assert 'level' in customer_data
        assert 'phone' in customer_data
        assert 'email' in customer_data
        assert 'address' in customer_data
    
    def test_data_generator_lead(self):
        """测试线索数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        lead_data = generator.generate_lead()
        
        assert 'customerId' in lead_data
        assert 'source' in lead_data
        assert 'status' in lead_data
        assert 'name' in lead_data
        assert 'description' in lead_data
    
    def test_data_generator_random(self):
        """测试随机数据生成"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        
        # 测试各种数据类型
        assert 'username' in generator.generate_random_data('user')
        assert 'productIds' in generator.generate_random_data('order')
        assert 'name' in generator.generate_random_data('product')
        assert 'name' in generator.generate_random_data('customer')
        assert 'customerId' in generator.generate_random_data('lead')
    
    def test_data_generator_invalid_type(self):
        """测试无效数据类型"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        
        with pytest.raises(ValueError):
            generator.generate_random_data('invalid_type')


class TestCoreAPIIntegration:
    """core-api模块集成测试"""
    
    def test_api_client_http_methods(self):
        """测试API客户端HTTP方法"""
        from tests.utils.api_client import APIClient
        
        client = APIClient(base_url='http://localhost:8080/api/v1')
        
        # 测试所有HTTP方法都存在
        assert hasattr(client, 'get')
        assert hasattr(client, 'post')
        assert hasattr(client, 'put')
        assert hasattr(client, 'delete')
        assert hasattr(client, 'patch')
    
    def test_data_generator多样性(self):
        """测试数据生成多样性"""
        from tests.utils.data_generator import DataGenerator
        
        generator = DataGenerator()
        
        # 生成10组用户数据，检查是否不同
        users = [generator.generate_user() for _ in range(10)]
        usernames = [u['username'] for u in users]
        
        # 所有用户名应该不同
        assert len(set(usernames)) == 10
