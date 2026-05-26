#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
合同管理接口自动化测试脚本
测试合同的创建、审核、生效、变更、终止等全生命周期操作
"""

import pytest
import json
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta

# 添加项目根目录到系统路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root / 'utils'))

from api_client import ContractAPIClient
from assertions import (
    APIResponseValidator,
    ContractSchemaValidator,
    BusinessLogicValidator,
    PaginationValidator
)
from config import test_config

class TestContractAPI:
    """合同管理接口测试类"""
    
    @classmethod
    def setup_class(cls):
        """测试类级别的初始化"""
        cls.client = ContractAPIClient()
        cls.api_validator = APIResponseValidator()
        cls.schema_validator = ContractSchemaValidator()
        cls.business_validator = BusinessLogicValidator()
        cls.pagination_validator = PaginationValidator()
        
        # 加载测试数据
        data_dir = project_root / 'test_data'
        with open(data_dir / 'contract_data.json', 'r', encoding='utf-8') as f:
            cls.test_data = json.load(f)
        
        # 创建测试环境的必要数据
        cls.setup_test_data()
    
    @classmethod
    def setup_test_data(cls):
        """创建测试环境需要的初始数据"""
        # 清理可能存在的测试数据
        cleanup_contracts = cls.test_data['cleanup_contracts']
        for contract_code in cleanup_contracts:
            try:
                cls.client.delete_contract(contract_code)
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
    
    def test_01_contract_create_success(self):
        """测试合同创建成功场景"""
        contract_data = self.test_data['contracts'][0]
        
        # 1. 发送创建请求
        response = self.client.create_contract(contract_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 201)
        self.api_validator.validate_response_data_present(response, ['contract_id'])
        
        # 3. 验证返回的数据结构
        result = response['data']
        self.schema_validator.validate_contract_basic_info(result)
        self.schema_validator.validate_contract_terms(result)
        self.schema_validator.validate_contract_parties(result)
        
        # 4. 验证数据一致性
        assert result['contract_code'] == contract_data['contract_code']
        assert result['contract_name'] == contract_data['contract_name']
        assert result['status'] == 'draft'  # 新合同应为草稿状态
        
        # 保存创建的合同ID供后续测试使用
        self.created_contract_id = result['contract_id']
        self.created_contract_code = result['contract_code']
    
    def test_02_contract_create_validation_fail(self):
        """测试合同创建参数验证失败"""
        invalid_contract = self.test_data['invalid_contracts']['missing_required_fields']
        
        # 1. 发送创建请求（应该会失败）
        response = self.client.create_contract(invalid_contract)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 400)  # Bad Request
        self.api_validator.validate_error_message(response, '缺少必填字段')
        self.api_validator.validate_error_details(response, ['contract_name'])
    
    def test_03_contract_get_by_id_success(self):
        """测试通过ID获取合同信息"""
        # 1. 发送获取请求
        response = self.client.get_contract(self.created_contract_id)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['contract_id'])
        
        # 3. 验证数据结构
        result = response['data']
        self.schema_validator.validate_contract_basic_info(result)
        self.schema_validator.validate_contract_terms(result)
        self.schema_validator.validate_contract_parties(result)
        
        # 4. 验证数据正确性
        assert result['contract_id'] == self.created_contract_id
        assert result['contract_code'] == self.created_contract_code
    
    def test_04_contract_submit_for_review(self):
        """测试合同提交审核"""
        submit_data = {
            'contract_id': self.created_contract_id,
            'submit_comment': '合同已准备就绪，申请审核',
            'reviewer_ids': ['reviewer_001', 'reviewer_002']
        }
        
        # 1. 发送提交审核请求
        response = self.client.submit_contract_for_review(submit_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证状态变更
        result = response['data']
        assert result['status'] == 'under_review'
        assert 'review_history' in result
        
        # 4. 验证审核流程已启动
        get_response = self.client.get_contract(self.created_contract_id)
        contract = get_response['data']
        assert contract['status'] == 'under_review'
        assert len(contract['review_history']) > 0
    
    def test_05_contract_review_approve(self):
        """测试合同审核通过"""
        review_data = {
            'contract_id': self.created_contract_id,
            'review_result': 'approve',
            'review_comment': '合同条款清晰，符合公司规定，同意签署',
            'reviewer_id': 'reviewer_001'
        }
        
        # 1. 发送审核请求
        response = self.client.review_contract(review_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证审核结果
        result = response['data']
        assert result['review_result'] == 'approve'
        assert 'next_reviewer' in result
        
        # 4. 验证合同状态（可能需要所有审核人都通过）
        get_response = self.client.get_contract(self.created_contract_id)
        contract = get_response['data']
        # 根据审核规则验证状态
    
    def test_06_contract_activate_success(self):
        """测试合同生效"""
        # 假设所有审核已完成，合同可以生效
        activate_data = {
            'contract_id': self.created_contract_id,
            'effective_date': (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d'),
            'activation_comment': '合同审核通过，正式生效'
        }
        
        # 1. 发送生效请求
        response = self.client.activate_contract(activate_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证状态变更
        result = response['data']
        assert result['status'] == 'active'
        assert result['effective_date'] == activate_data['effective_date']
        
        # 4. 验证业务逻辑
        self.business_validator.validate_contract_activation_conditions(result)
    
    def test_07_contract_modification_request(self):
        """测试合同变更申请"""
        modification_data = {
            'contract_id': self.created_contract_id,
            'modification_type': 'terms_change',
            'reason': '市场需求变化，需要调整付款条款',
            'changes': {
                'payment_terms': '30天内付款，调整为45天内付款',
                'new_terms': self.test_data['contracts'][1]['terms']
            },
            'requested_by': 'supplier_001'
        }
        
        # 1. 发送变更申请
        response = self.client.request_contract_modification(modification_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证变更流程启动
        result = response['data']
        assert result['modification_status'] == 'pending'
        assert 'modification_request_id' in result
    
    def test_08_contract_search_by_status(self):
        """测试按状态搜索合同"""
        search_params = {
            'status': 'active',
            'page': 1,
            'page_size': 10,
            'start_date': (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d'),
            'end_date': datetime.now().strftime('%Y-%m-%d')
        }
        
        # 1. 发送搜索请求
        response = self.client.search_contracts(search_params)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['items', 'total', 'page', 'page_size'])
        
        # 3. 验证分页结构
        result = response['data']
        self.pagination_validator.validate_pagination_structure(result)
        
        # 4. 验证搜索结果的正确性
        if result['total'] > 0:
            for item in result['items']:
                assert item['status'] == 'active'
                # 验证日期范围
                if 'effective_date' in item and item['effective_date']:
                    effective_date = datetime.strptime(item['effective_date'], '%Y-%m-%d')
                    assert effective_date >= datetime.strptime(search_params['start_date'], '%Y-%m-%d')
    
    def test_09_contract_search_by_party(self):
        """测试按参与方搜索合同"""
        search_params = {
            'supplier_id': 'supplier_001',
            'company_id': 'company_001',
            'page': 1,
            'page_size': 10
        }
        
        # 1. 发送搜索请求
        response = self.client.search_contracts(search_params)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证搜索结果
        result = response['data']
        self.pagination_validator.validate_pagination_structure(result)
        
        # 4. 验证参与方匹配
        if result['total'] > 0:
            for item in result['items']:
                parties = item['parties']
                supplier_found = any(party['party_id'] == search_params['supplier_id'] 
                                   for party in parties)
                company_found = any(party['party_id'] == search_params['company_id'] 
                                  for party in parties)
                assert supplier_found or company_found
    
    def test_10_contract_termination_request(self):
        """测试合同终止申请"""
        termination_data = {
            'contract_id': self.created_contract_id,
            'termination_reason': '双方协商一致终止',
            'termination_date': (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d'),
            'termination_type': 'mutual_agreement',
            'requested_by': 'company_001'
        }
        
        # 1. 发送终止申请
        response = self.client.request_contract_termination(termination_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证终止流程启动
        result = response['data']
        assert result['termination_status'] == 'pending'
        assert 'termination_request_id' in result
    
    def test_11_contract_renewal_process(self):
        """测试合同续约流程"""
        renewal_data = {
            'contract_id': self.created_contract_id,
            'renewal_period': 12,  # 12个月
            'renewal_terms': {
                'new_end_date': (datetime.now() + timedelta(days=365)).strftime('%Y-%m-%d'),
                'price_adjustment': 5.0,  # 价格调整5%
                'other_terms': '其他条款保持不变'
            },
            'requested_by': 'supplier_001'
        }
        
        # 1. 发送续约申请
        response = self.client.request_contract_renewal(renewal_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证续约流程启动
        result = response['data']
        assert result['renewal_status'] == 'pending'
        assert 'renewal_request_id' in result
    
    def test_12_contract_performance_monitoring(self):
        """测试合同履约监控"""
        # 1. 获取合同履约状态
        response = self.client.get_contract_performance(self.created_contract_id)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证履约数据
        result = response['data']
        assert 'performance_score' in result
        assert 'compliance_rate' in result
        assert 'milestone_completion' in result
        assert 'issue_tracking' in result
        
        # 4. 验证业务逻辑
        self.business_validator.validate_contract_performance_metrics(result)
    
    def test_13_contract_export_report(self):
        """测试合同报表导出"""
        export_params = {
            'report_type': 'contract_summary',
            'format': 'excel',
            'filters': {
                'status': ['active', 'under_review'],
                'start_date': (datetime.now() - timedelta(days=90)).strftime('%Y-%m-%d')
            }
        }
        
        # 1. 发送导出请求
        response = self.client.export_contract_report(export_params)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['file_url', 'file_size', 'generated_at'])
        
        # 3. 验证报表信息
        result = response['data']
        assert 'report_id' in result
        assert result['file_size'] > 0
    
    def test_14_contract_audit_trail(self):
        """测试合同操作审计日志"""
        # 1. 获取合同审计日志
        response = self.client.get_contract_audit_trail(self.created_contract_id)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        
        # 3. 验证审计日志结构
        result = response['data']
        assert 'audit_logs' in result
        assert isinstance(result['audit_logs'], list)
        
        # 4. 验证审计日志内容
        if len(result['audit_logs']) > 0:
            for log in result['audit_logs']:
                assert 'action' in log
                assert 'actor' in log
                assert 'timestamp' in log
                assert 'details' in log
    
    def test_15_contract_bulk_operations(self):
        """测试合同批量操作"""
        bulk_data = self.test_data['bulk_operations']['bulk_status_update']
        
        # 1. 发送批量更新请求
        response = self.client.bulk_update_contracts(bulk_data)
        
        # 2. 验证响应
        self.api_validator.validate_response_structure(response)
        self.api_validator.validate_status_code(response, 200)
        self.api_validator.validate_response_data_present(response, ['success_count', 'failed_count', 'details'])
        
        # 3. 验证批量操作结果
        result = response['data']
        assert result['success_count'] >= 0
        assert 'details' in result
        assert isinstance(result['details'], list)


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])