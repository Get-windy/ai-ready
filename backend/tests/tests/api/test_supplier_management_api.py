#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
供应商管理模块功能测试
测试范围: 供应商注册、资质审核、信息管理等核心功能

测试目标:
1. 验证供应商注册功能
2. 验证资质审核流程
3. 验证供应商信息管理功能
4. 验证供应商查询与筛选功能
"""

import pytest
import random
import sys
import os
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.test_helpers import assert_success, assert_field_exists


class TestSupplierManagementAPI:
    """供应商管理模块API功能测试"""

    @pytest.fixture(scope='class')
    def client(self, api_client):
        """带认证的API客户端"""
        return api_client

    @pytest.fixture
    def supplier_data(self):
        """生成供应商测试数据"""
        return {
            'supplierCode': f'SUP{datetime.now().strftime("%Y%m%d")}{random.randint(1000, 9999)}',
            'supplierName': f'测试供应商_{random.randint(1000, 9999)}',
            'supplierType': 'MANUFACTURER',
            'businessScope': '电子产品制造',
            'contactName': f'联系人_{random.randint(100, 999)}',
            'contactPhone': f'138{random.randint(10000000, 99999999)}',
            'contactEmail': f'supplier{random.randint(1000, 9999)}@test.com',
            'registeredAddress': '北京市朝阳区测试路123号',
            'businessLicenseNo': f'BL{random.randint(100000000, 999999999)}',
            'taxId': f'TAX{random.randint(100000000, 999999999)}',
            'bankName': '测试银行',
            'bankAccount': f'{random.randint(100000000000, 999999999999)}',
            'accountName': '测试供应商公司',
            'cooperationStatus': 1,
            'supplierLevel': 'A',
            'status': 'ACTIVE'
        }

    # ==================== 供应商注册测试 ====================

    @pytest.mark.smoke
    def test_supplier_create_page(self, client):
        """[冒烟] 验证供应商注册页面加载"""
        response = client.post('/api/supplier/page', json={
            'pageNum': 1,
            'pageSize': 10
        })
        data = assert_success(response, msg="供应商列表查询")
        assert_field_exists(data, 'list', msg="响应应包含供应商列表")
        assert_field_exists(data, 'total', msg="响应应包含总数")

    def test_supplier_create(self, client, supplier_data):
        """供应商注册 - 验证信息填写和提交功能"""
        response = client.post('/api/supplier', json=supplier_data)
        data = assert_success(response, expected_status=[200, 201], msg="供应商创建")
        assert_field_exists(data, 'id', msg="创建后应返回供应商ID")
        assert_field_exists(data, 'supplierCode', msg="创建后应返回供应商编码")
        assert data['supplierName'] == supplier_data['supplierName'], "供应商名称应匹配"

    def test_supplier_create_required_fields_validation(self, client):
        """供应商注册 - 验证必填字段校验"""
        # 缺少必填字段 supplierName
        invalid_data = {
            'supplierCode': f'SUP{random.randint(10000, 99999)}',
            'contactName': '测试联系人',
            'contactPhone': '13800138000'
        }
        response = client.post('/api/supplier', json=invalid_data)
        # 应该返回400错误
        assert response.status_code in [400, 422], "缺少必填字段应返回400或422错误"

    def test_supplier_create_duplicate_code(self, client, supplier_data):
        """供应商注册 - 验证重复编码校验"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        assert_success(create_response, expected_status=[200, 201])

        # 尝试使用相同编码创建
        duplicate_data = supplier_data.copy()
        duplicate_data['supplierName'] = '另一个供应商'
        response = client.post('/api/supplier', json=duplicate_data)
        # 应该返回错误，编码已存在
        assert response.status_code in [400, 409], "重复编码应返回400或409错误"

    # ==================== 供应商查询测试 ====================

    def test_supplier_get_by_id(self, client, supplier_data):
        """根据ID查询供应商详情"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 查询详情
        response = client.get(f'/api/supplier/{supplier_id}')
        data = assert_success(response, msg="供应商详情查询")
        assert data['id'] == supplier_id, "ID应匹配"
        assert data['supplierCode'] == supplier_data['supplierCode'], "供应商编码应匹配"

    def test_supplier_get_by_code(self, client, supplier_data):
        """根据编码查询供应商"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_code = create_data['supplierCode']

        # 根据编码查询
        response = client.get(f'/api/supplier/code/{supplier_code}')
        data = assert_success(response, msg="根据编码查询供应商")
        assert data['supplierCode'] == supplier_code, "供应商编码应匹配"

    def test_supplier_list_query(self, client):
        """供应商列表查询与筛选"""
        query_data = {
            'pageNum': 1,
            'pageSize': 10,
            'status': 'ACTIVE'
        }
        response = client.post('/api/supplier/page', json=query_data)
        data = assert_success(response, msg="供应商分页查询")
        assert_field_exists(data, 'list', msg="响应应包含列表数据")
        assert_field_exists(data, 'total', msg="响应应包含总数")
        assert isinstance(data['list'], list), "列表应为数组"

    def test_supplier_list_with_filters(self, client):
        """供应商列表 - 带筛选条件查询"""
        query_data = {
            'pageNum': 1,
            'pageSize': 10,
            'supplierLevel': 'A',
            'cooperationStatus': 1,
            'status': 'ACTIVE'
        }
        response = client.post('/api/supplier/page', json=query_data)
        data = assert_success(response, msg="带筛选条件的供应商查询")
        assert_field_exists(data, 'list', msg="响应应包含列表数据")

    # ==================== 供应商信息更新测试 ====================

    def test_supplier_update(self, client, supplier_data):
        """供应商信息更新功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 更新供应商信息
        update_data = {
            'id': supplier_id,
            'supplierCode': supplier_data['supplierCode'],
            'supplierName': f'更新后的名称_{random.randint(1000, 9999)}',
            'contactName': '更新后的联系人',
            'contactPhone': f'139{random.randint(10000000, 99999999)}',
            'supplierType': supplier_data['supplierType'],
            'status': supplier_data['status']
        }
        response = client.put('/api/supplier', json=update_data)
        data = assert_success(response, msg="供应商信息更新")
        assert data['supplierName'] == update_data['supplierName'], "供应商名称应更新"
        assert data['contactName'] == update_data['contactName'], "联系人应更新"

    def test_supplier_update_contact_info(self, client, supplier_data):
        """供应商联系人信息更新"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 更新联系人信息
        update_data = {
            'id': supplier_id,
            'supplierCode': supplier_data['supplierCode'],
            'supplierName': supplier_data['supplierName'],
            'contactName': '新联系人',
            'contactPhone': '13888888888',
            'contactEmail': 'newcontact@test.com',
            'supplierType': supplier_data['supplierType'],
            'status': supplier_data['status']
        }
        response = client.put('/api/supplier', json=update_data)
        data = assert_success(response, msg="联系人信息更新")
        assert data['contactName'] == '新联系人', "联系人应更新"
        assert data['contactPhone'] == '13888888888', "联系电话应更新"

    # ==================== 供应商等级与状态管理测试 ====================

    def test_supplier_update_level(self, client, supplier_data):
        """供应商等级更新功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 更新等级
        response = client.post(f'/api/supplier/{supplier_id}/update-level', params={
            'supplierLevel': 'B',
            'reason': '测试等级变更'
        })
        data = assert_success(response, msg="供应商等级更新")
        assert data == True, "等级更新应返回True"

    def test_supplier_update_cooperation_status(self, client, supplier_data):
        """供应商合作状态更新功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 更新合作状态
        response = client.post(f'/api/supplier/{supplier_id}/update-cooperation-status', params={
            'cooperationStatus': 2,
            'reason': '测试合作状态变更'
        })
        data = assert_success(response, msg="供应商合作状态更新")
        assert data == True, "合作状态更新应返回True"

    # ==================== 供应商门户管理测试 ====================

    def test_supplier_activate_portal(self, client, supplier_data):
        """供应商门户激活功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 激活门户
        response = client.post(f'/api/supplier/{supplier_id}/activate-portal', params={
            'portalAccountId': f'PORTAL_{random.randint(10000, 99999)}'
        })
        data = assert_success(response, msg="供应商门户激活")
        assert data == True, "门户激活应返回True"

    def test_supplier_disable_portal(self, client, supplier_data):
        """供应商门户禁用功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 先激活门户
        client.post(f'/api/supplier/{supplier_id}/activate-portal', params={
            'portalAccountId': f'PORTAL_{random.randint(10000, 99999)}'
        })

        # 禁用门户
        response = client.post(f'/api/supplier/{supplier_id}/disable-portal', params={
            'reason': '测试禁用门户'
        })
        data = assert_success(response, msg="供应商门户禁用")
        assert data == True, "门户禁用应返回True"

    # ==================== 供应商绩效评估测试 ====================

    def test_supplier_performance_evaluate(self, client, supplier_data):
        """供应商绩效评估功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 提交绩效评估
        performance_data = {
            'supplierId': supplier_id,
            'periodType': 1,  # 月度
            'period': '2026-04',
            'qualityScore': random.randint(80, 100),
            'deliveryScore': random.randint(80, 100),
            'serviceScore': random.randint(80, 100),
            'priceScore': random.randint(80, 100),
            'remark': '自动化测试绩效评估'
        }
        response = client.post('/api/supplier/performance/evaluate', json=performance_data)
        data = assert_success(response, msg="供应商绩效评估")
        assert data == True, "绩效评估应返回True"

    def test_supplier_performance_history(self, client, supplier_data):
        """供应商绩效历史查询"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 查询绩效历史
        response = client.get(f'/api/supplier/{supplier_id}/performance/history', params={
            'periodType': 1,
            'limit': 10
        })
        data = assert_success(response, msg="供应商绩效历史查询")
        assert isinstance(data, list), "响应应为列表"

    def test_supplier_comprehensive_score(self, client, supplier_data):
        """供应商综合评分查询"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 查询综合评分
        response = client.get(f'/api/supplier/{supplier_id}/comprehensive-score')
        data = assert_success(response, msg="供应商综合评分查询")
        assert isinstance(data, (int, float)), "综合评分应为数字"

    # ==================== 供应商导入导出测试 ====================

    def test_supplier_export(self, client):
        """供应商导出功能"""
        query_data = {
            'pageNum': 1,
            'pageSize': 100,
            'status': 'ACTIVE'
        }
        response = client.post('/api/supplier/export', json=query_data)
        data = assert_success(response, msg="供应商导出")
        assert isinstance(data, list), "导出应返回列表"

    def test_supplier_validate(self, client, supplier_data):
        """供应商信息验证功能"""
        response = client.post('/api/supplier/validate', json=supplier_data)
        data = assert_success(response, msg="供应商信息验证")
        assert isinstance(data, bool), "验证结果应为布尔值"

    # ==================== 供应商统计测试 ====================

    def test_supplier_statistics(self, client):
        """供应商统计信息查询"""
        response = client.get('/api/supplier/statistics')
        data = assert_success(response, msg="供应商统计信息")
        assert data is not None, "统计信息不应为空"

    # ==================== 供应商删除测试 ====================

    def test_supplier_delete(self, client, supplier_data):
        """供应商删除功能"""
        # 先创建一个供应商
        create_response = client.post('/api/supplier', json=supplier_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        supplier_id = create_data['id']

        # 删除供应商
        response = client.delete(f'/api/supplier/{supplier_id}')
        data = assert_success(response, msg="供应商删除")
        assert data == True, "删除应返回True"

    def test_supplier_batch_delete(self, client, supplier_data):
        """供应商批量删除功能"""
        # 创建多个供应商
        supplier_ids = []
        for i in range(3):
            data = supplier_data.copy()
            data['supplierCode'] = f'SUP{datetime.now().strftime("%Y%m%d")}{random.randint(1000, 9999)}_{i}'
            data['supplierName'] = f'测试供应商_{random.randint(1000, 9999)}_{i}'
            create_response = client.post('/api/supplier', json=data)
            create_data = assert_success(create_response, expected_status=[200, 201])
            supplier_ids.append(create_data['id'])

        # 批量删除
        response = client.delete('/api/supplier/batch', json=supplier_ids)
        data = assert_success(response, msg="供应商批量删除")
        assert data == True, "批量删除应返回True"


# pytest标记
pytestmark = [pytest.mark.erp, pytest.mark.supplier, pytest.mark.management]
