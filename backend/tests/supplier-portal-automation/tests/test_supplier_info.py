#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
供应商信息管理接口自动化测试脚本
测试供应商信息的增删改查、搜索、批量操作等功能
"""

import pytest
import json
import os
import sys
from pathlib import Path

# 添加项目根目录到系统路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root / 'utils'))

from api_client import SupplierAPIClient
from assertions import (
    APIResponseValidator,
    SupplierSchemaValidator,
    PaginationValidator
)
from config import test_config

class TestSupplierInfoAPI:
    """供应商信息管理接口测试类"""
    
    @classmethod
    def setup_class(cls):
        """测试类级别的初始化"""
        cls.client = SupplierAPIClient()
        cls.api_validator = APIResponseValidator()
        cls.schema_validator = SupplierSchemaValidator()
        cls.pagination_validator = PaginationValidator()
        
        # 加载测试数据
        data_dir = project_root / 'test_data'
        with open(data_dir / 'supplier_data.json', 'r', encoding='utf-8') as f:
            cls.test_data = json.load(f)
        
        # 创建测试环境的必要数据
        cls.setup_test_data()
    
    @classmethod
    def setup_test_data(cls):
        """创建测试环境需要的初始数据"""
        # 清理可能存在的测试数据
        cleanup_suppliers = cls.test_data['cleanup_suppliers']
        for supplier_code in cleanup_suppliers:
            try:
                cls.client.delete_supplier(supplier_code)
            except:
                pass
    
    def setup_method(self):
        """每个测试方法执行前的准备"""
        # 确保测试环境干净
        pass
    
    def teardown_method(self):
        """每个测试方法执行后的清理"""
        # 可以根据需要清理测试创建的数据
        pass
    
    def test_01_supplier_create_success(self):
        """测试供应商创建成功场景"""
        supplier_data = self.test_data['suppliers'][0]
        
        # 1. 发送创建请求
        response = self.client.create_supplier(supplier_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 201)
        self.api_validator.validate_response_data_present(response, ['supplier_id'])
        
        # 3. 验证返回的数据结构
        result = response['data']
        self.schema_validator.validate_supplier_basic_info(result)
        self.schema_validator.validate_supplier_contact_info(result)
        self.schema_validator.validate_supplier_qualification_info(result)
        
        # 4. 验证数据一致性
        assert result['supplier_code'] == supplier_data['supplier_code']
        assert result['supplier_name'] == supplier_data['supplier_name']
        assert result['status'] == 'active'
        
        # 保存创建的供应商ID供后续测试使用
        self.created_supplier_id = result['supplier_id']
        self.created_supplier_code = result['supplier_code']
    
    def test_02_supplier_create_duplicate_code(self):
        """测试供应商代码重复的场景"""
        supplier_data = self.test_data['suppliers'][0].copy()
        
        # 1. 发送创建请求（应该会失败）
        response = self.client.create_supplier(supplier_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 409)  # Conflict
        self.api_validator.validate_error_message(response, '供应商代码已存在')
    
    def test_03_supplier_get_by_id_success(self):
        """测试通过ID获取供应商信息"""
        # 1. 发送获取请求
        response = self.client.get_supplier(self.created_supplier_id)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['supplier_id'])
        
        # 3. 验证数据结构
        result = response['data']
        self.schema_validator.validate_supplier_basic_info(result)
        self.schema_validator.validate_supplier_contact_info(result)
        self.schema_validator.validate_supplier_qualification_info(result)
        
        # 4. 验证数据正确性
        assert result['supplier_id'] == self.created_supplier_id
        assert result['supplier_code'] == self.created_supplier_code
    
    def test_04_supplier_get_by_id_not_found(self):
        """测试获取不存在的供应商"""
        # 1. 发送获取请求
        response = self.client.get_supplier('non_existent_supplier_999')
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 404)
        self.api_validator.validate_error_message(response, '供应商不存在')
    
    def test_05_supplier_update_success(self):
        """测试供应商信息更新成功"""
        update_data = self.test_data['update_scenarios']['basic_update']
        update_data['supplier_id'] = self.created_supplier_id
        
        # 1. 发送更新请求
        response = self.client.update_supplier(update_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['supplier_id'])
        
        # 3. 验证更新后的数据
        result = response['data']
        assert result['supplier_name'] == update_data['supplier_name']
        assert result['contact_person'] == update_data['contact_person']
        assert result['contact_phone'] == update_data['contact_phone']
    
    def test_06_supplier_search_by_name(self):
        """测试按供应商名称搜索"""
        search_params = {
            'keyword': self.test_data['search_test_cases']['name_search']['keyword'],
            'page': 1,
            'page_size': 10
        }
        
        # 1. 发送搜索请求
        response = self.client.search_suppliers(search_params)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['items', 'total', 'page', 'page_size'])
        
        # 3. 验证分页结构
        result = response['data']
        self.pagination_validator.validate_pagination_structure(result)
        
        # 4. 验证搜索结果的正确性
        assert result['total'] >= 1
        assert any(self.created_supplier_code in item['supplier_code'] 
                  for item in result['items'])
    
    def test_07_supplier_search_by_status(self):
        """测试按状态搜索供应商"""
        search_params = {
            'status': 'active',
            'page': 1,
            'page_size': 10
        }
        
        # 1. 发送搜索请求
        response = self.client.search_suppliers(search_params)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证搜索结果
        result = response['data']
        self.pagination_validator.validate_pagination_structure(result)
        
        # 4. 验证所有结果都是active状态
        for item in result['items']:
            assert item['status'] == 'active'
    
    def test_08_supplier_bulk_operations(self):
        """测试供应商批量操作"""
        bulk_data = self.test_data['bulk_operations']['bulk_update']
        
        # 1. 发送批量更新请求
        response = self.client.bulk_update_suppliers(bulk_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['success_count', 'failed_count', 'details'])
        
        # 3. 验证批量操作结果
        result = response['data']
        assert result['success_count'] >= 1
        assert 'details' in result
        assert isinstance(result['details'], list)
    
    def test_09_supplier_status_transition(self):
        """测试供应商状态流转"""
        status_transitions = self.test_data['status_transitions']
        
        for transition in status_transitions:
            status_data = {
                'supplier_id': self.created_supplier_id,
                'status': transition['target_status'],
                'reason': transition['reason']
            }
            
            # 1. 发送状态更新请求
            response = self.client.update_supplier_status(status_data)
            
            # 2. 验证响应
            self.api_validator.validate_response_structure(response)
            self.api_validator.validate_status_code(response, 200)
            
            # 3. 验证状态更新
            result = response['data']
            assert result['status'] == transition['target_status']
            
            # 4. 验证是否可以获取更新后的状态
            get_response = self.client.get_supplier(self.created_supplier_id)
            assert get_response['data']['status'] == transition['target_status']
    
    def test_10_supplier_delete_success(self):
        """测试供应商删除成功"""
        # 1. 发送删除请求
        response = self.client.delete_supplier(self.created_supplier_code)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_success_message(response, '删除成功')
        
        # 3. 验证删除后无法再获取
        get_response = self.client.get_supplier(self.created_supplier_id)
        self.api_validator.validate_status_code(get_response, 404)
    
    def test_11_supplier_delete_not_found(self):
        """测试删除不存在的供应商"""
        # 1. 发送删除请求
        response = self.client.delete_supplier('non_existent_supplier_999')
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 404)
        self.api_validator.validate_error_message(response, '供应商不存在')
    
    def test_12_supplier_export_import(self):
        """测试供应商数据导出导入"""
        # 1. 导出供应商数据
        export_response = self.client.export_suppliers({'format': 'excel'})
        
        # 2. 验证导出响应
        self.api_validator.validate_response_structure(export_response)
        self.api_validator.validate_status_code(export_response, 200)
        self.api_validator.validate_response_data_present(export_response, ['file_url', 'file_size'])
        
        # 3. 准备导入数据
        import_data = self.test_data['bulk_operations']['bulk_create']
        
        # 4. 发送导入请求
        import_response = self.client.import_suppliers(import_data)
        
        # 5. 验证导入响应
        self.api_validator.validate_response_structure(import_response)
        self.api_validator.validate_status_code(import_response, 200)
        self.api_validator.validate_response_data_present(import_response, ['import_id', 'total_count', 'success_count'])


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])