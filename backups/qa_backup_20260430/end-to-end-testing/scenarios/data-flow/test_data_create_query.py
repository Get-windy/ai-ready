"""
数据创建到查询端到端测试
测试完整数据流程：创建 -> 验证 -> 查询 -> 更新 -> 删除
"""

import pytest
import requests
import sys
import os

# 添加框架路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from framework.base_test import BaseE2ETest
from framework.config import TestConfig


class TestDataCreateQuery(BaseE2ETest):
    """数据创建到查询端到端测试"""
    
    def __init__(self):
        super().__init__()
        self.config = TestConfig()
        self.api_base_url = self.config.get('api.base_url', 'http://localhost:8080/api')
        self.test_data_records = []
        self.auth_token = None
    
    def _get_auth_header(self):
        """获取认证头"""
        if self.auth_token:
            return {'Authorization': f'Bearer {self.auth_token}'}
        return {}
    
    def test_data_creation_flow(self):
        """测试数据创建流程"""
        self.setup_method()
        
        try:
            # 步骤1: 生成测试数据
            test_product = self.utils.generate_test_product()
            self.add_test_step(
                "生成测试产品数据",
                {'product_name': test_product['name']},
                'passed'
            )
            
            # 步骤2: 创建数据
            response = requests.post(
                f"{self.api_base_url}/products",
                json=test_product,
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(response.status_code, 201, "创建数据应返回201状态码")
            
            response_data = response.json()
            self.assert_true('id' in response_data, "响应应包含id")
            
            product_id = response_data['id']
            test_product['id'] = product_id
            self.test_data_records.append(test_product)
            
            self.add_test_step(
                "创建数据记录",
                {'product_id': product_id, 'status': 'created'},
                'passed'
            )
            
            # 步骤3: 验证数据创建
            verify_response = requests.get(
                f"{self.api_base_url}/products/{product_id}",
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(verify_response.status_code, 200, "数据查询应返回200状态码")
            
            verify_data = verify_response.json()
            self.assert_equal(verify_data['name'], test_product['name'], "产品名称应匹配")
            self.assert_equal(verify_data['price'], test_product['price'], "产品价格应匹配")
            
            self.add_test_step(
                "验证数据创建",
                {'product_id': product_id, 'data_valid': True},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "数据创建流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_data_query_flow(self):
        """测试数据查询流程"""
        self.setup_method()
        
        try:
            # 步骤1: 查询所有数据
            response = requests.get(
                f"{self.api_base_url}/products",
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "数据列表查询应返回200状态码")
            
            response_data = response.json()
            self.assert_true('items' in response_data, "响应应包含items")
            self.assert_true('total' in response_data, "响应应包含total")
            
            self.add_test_step(
                "查询所有数据",
                {'total_count': response_data.get('total', 0)},
                'passed'
            )
            
            # 步骤2: 条件查询
            search_params = {
                'category': 'electronics',
                'min_price': 100,
                'max_price': 500
            }
            
            response = requests.get(
                f"{self.api_base_url}/products/search",
                params=search_params,
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "条件查询应返回200状态码")
            
            self.add_test_step(
                "条件查询数据",
                {'search_params': search_params},
                'passed'
            )
            
            # 步骤3: 分页查询
            page_params = {
                'page': 1,
                'size': 10,
                'sort': 'created_at',
                'order': 'desc'
            }
            
            response = requests.get(
                f"{self.api_base_url}/products",
                params=page_params,
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "分页查询应返回200状态码")
            
            response_data = response.json()
            self.assert_true('page' in response_data, "响应应包含page信息")
            
            self.add_test_step(
                "分页查询数据",
                {'page': page_params['page'], 'size': page_params['size']},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "数据查询流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test_data_update_flow(self):
        """测试数据更新流程"""
        self.setup_method()
        
        try:
            # 确保有测试数据
            if not self.test_data_records:
                test_product = self.utils.generate_test_product()
                test_product['id'] = self.utils.generate_unique_id("PROD_")
                self.test_data_records.append(test_product)
            
            product = self.test_data_records[0]
            product_id = product['id']
            
            # 步骤1: 更新数据
            update_data = {
                'name': f"{product['name']}_updated",
                'price': product['price'] * 1.1,  # 涨价10%
                'stock': product['stock'] + 100
            }
            
            response = requests.put(
                f"{self.api_base_url}/products/{product_id}",
                json=update_data,
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(response.status_code, 200, "数据更新应返回200状态码")
            
            self.add_test_step(
                "更新数据记录",
                {'product_id': product_id, 'updates': list(update_data.keys())},
                'passed'
            )
            
            # 步骤2: 验证更新
            verify_response = requests.get(
                f"{self.api_base_url}/products/{product_id}",
                headers=self._get_auth_header(),
                timeout=30
            )
            
            self.assert_equal(verify_response.status_code, 200, "更新后查询应返回200状态码")
            
            verify_data = verify_response.json()
            self.assert_equal(verify_data['name'], update_data['name'], "名称应已更新")
            self.assert_equal(verify_data['price'], update_data['price'], "价格应已更新")
            
            self.add_test_step(
                "验证数据更新",
                {'product_id': product_id, 'update_verified': True},
                'passed'
            )
            
        except Exception as e:
            self.add_test_step(
                "数据更新流程",
                {'error': str(e)},
                'failed'
            )
            raise
        finally:
            self.teardown_method()
    
    def test