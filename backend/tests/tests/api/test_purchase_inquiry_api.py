#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
采购询价模块功能测试
测试范围: 询价单创建、询价条件设置、供应商邀请等核心功能

测试目标:
1. 验证询价单页面加载和创建功能
2. 验证询价商品/服务选择功能
3. 验证询价条件设置功能
4. 验证供应商邀请功能
"""

import pytest
import random
import sys
import os
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.test_helpers import assert_success, assert_field_exists


class TestPurchaseInquiryAPI:
    """采购询价模块API功能测试"""

    @pytest.fixture(scope='class')
    def client(self, api_client):
        """带认证的API客户端"""
        return api_client

    @pytest.fixture
    def inquiry_data(self):
        """生成询价单测试数据"""
        return {
            'inquiryNo': f'INQ{datetime.now().strftime("%Y%m%d")}{random.randint(1000, 9999)}',
            'title': f'测试询价单_{random.randint(1000, 9999)}',
            'inquiryType': 'PRODUCT',
            'requirementDesc': '自动化测试创建的询价需求',
            'urgencyLevel': 'NORMAL',
            'deadlineDate': (datetime.now() + timedelta(days=7)).isoformat(),
            'departmentId': 1,
            'requesterId': 1,
            'purchaserId': 1,
            'createdBy': 1
        }

    # ==================== 询价单创建测试 ====================

    @pytest.mark.smoke
    def test_inquiry_list_page(self, client):
        """[冒烟] 询价单列表页面加载"""
        response = client.get('/api/erp/purchase/inquiry')
        data = assert_success(response, msg="询价单列表查询")
        assert isinstance(data, list), "响应应该是一个列表"

    def test_inquiry_create(self, client, inquiry_data):
        """询价单创建 - 验证基本信息填写和保存功能"""
        response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        data = assert_success(response, expected_status=[200, 201], msg="询价单创建")
        assert_field_exists(data, 'id', msg="创建后应返回询价单ID")
        assert_field_exists(data, 'inquiryNo', msg="创建后应返回询价单编号")
        assert data['title'] == inquiry_data['title'], "标题应匹配"
        assert data['status'] == 'DRAFT', "新建询价单状态应为草稿"

    def test_inquiry_create_required_fields_validation(self, client):
        """询价单创建 - 验证必填字段校验"""
        # 缺少必填字段 title
        invalid_data = {
            'inquiryNo': f'INQ{random.randint(10000, 99999)}',
            'inquiryType': 'PRODUCT',
            'deadlineDate': (datetime.now() + timedelta(days=7)).isoformat(),
            'purchaserId': 1,
            'createdBy': 1
        }
        response = client.post('/api/erp/purchase/inquiry', json=invalid_data)
        # 应该返回400错误
        assert response.status_code in [400, 422], "缺少必填字段应返回400或422错误"

    def test_inquiry_create_missing_deadline(self, client):
        """询价单创建 - 验证截止日期必填校验"""
        invalid_data = {
            'inquiryNo': f'INQ{random.randint(10000, 99999)}',
            'title': '测试询价单',
            'inquiryType': 'PRODUCT',
            'purchaserId': 1,
            'createdBy': 1
        }
        response = client.post('/api/erp/purchase/inquiry', json=invalid_data)
        assert response.status_code in [400, 422], "缺少截止日期应返回400或422错误"

    # ==================== 询价单查询测试 ====================

    def test_inquiry_get_by_id(self, client, inquiry_data):
        """根据ID查询询价单详情"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 查询详情
        response = client.get(f'/api/erp/purchase/inquiry/{inquiry_id}')
        data = assert_success(response, msg="询价单详情查询")
        assert data['id'] == inquiry_id, "ID应匹配"
        assert data['inquiryNo'] == inquiry_data['inquiryNo'], "询价单编号应匹配"

    def test_inquiry_get_by_no(self, client, inquiry_data):
        """根据编号查询询价单"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_no = create_data['inquiryNo']

        # 根据编号查询
        response = client.get(f'/api/erp/purchase/inquiry/no/{inquiry_no}')
        data = assert_success(response, msg="根据编号查询询价单")
        assert data['inquiryNo'] == inquiry_no, "询价单编号应匹配"

    def test_inquiry_get_by_status(self, client):
        """根据状态查询询价单列表"""
        response = client.get('/api/erp/purchase/inquiry/status/DRAFT')
        data = assert_success(response, msg="根据状态查询询价单")
        assert isinstance(data, list), "响应应该是一个列表"

    def test_inquiry_get_by_purchaser(self, client):
        """根据采购员查询询价单列表"""
        response = client.get('/api/erp/purchase/inquiry/purchaser/1')
        data = assert_success(response, msg="根据采购员查询询价单")
        assert isinstance(data, list), "响应应该是一个列表"

    # ==================== 询价单更新测试 ====================

    def test_inquiry_update(self, client, inquiry_data):
        """询价单更新功能测试"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 更新询价单
        update_data = {
            'title': f'更新后的标题_{random.randint(1000, 9999)}',
            'requirementDesc': '更新后的需求描述',
            'urgencyLevel': 'HIGH'
        }
        response = client.put(f'/api/erp/purchase/inquiry/{inquiry_id}', json=update_data)
        data = assert_success(response, msg="询价单更新")
        assert data['title'] == update_data['title'], "标题应更新"
        assert data['urgencyLevel'] == update_data['urgencyLevel'], "紧急程度应更新"

    # ==================== 询价单状态流转测试 ====================

    def test_inquiry_publish(self, client, inquiry_data):
        """询价单发布功能测试"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 发布询价单
        response = client.post(f'/api/erp/purchase/inquiry/{inquiry_id}/publish')
        data = assert_success(response, msg="询价单发布")
        assert data['status'] == 'PUBLISHED', "发布后状态应为已发布"
        assert_field_exists(data, 'publishDate', msg="发布后应有发布日期")

    def test_inquiry_close(self, client, inquiry_data):
        """询价单关闭功能测试"""
        # 先创建并发布一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 先发布
        client.post(f'/api/erp/purchase/inquiry/{inquiry_id}/publish')

        # 关闭询价单
        response = client.post(f'/api/erp/purchase/inquiry/{inquiry_id}/close')
        data = assert_success(response, msg="询价单关闭")
        assert data['status'] == 'CLOSED', "关闭后状态应为已关闭"

    def test_inquiry_cancel(self, client, inquiry_data):
        """询价单取消功能测试"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 取消询价单
        response = client.post(f'/api/erp/purchase/inquiry/{inquiry_id}/cancel')
        data = assert_success(response, msg="询价单取消")
        assert data['status'] == 'CANCELLED', "取消后状态应为已取消"

    # ==================== 询价单删除测试 ====================

    def test_inquiry_delete(self, client, inquiry_data):
        """询价单删除功能测试"""
        # 先创建一个询价单
        create_response = client.post('/api/erp/purchase/inquiry', json=inquiry_data)
        create_data = assert_success(create_response, expected_status=[200, 201])
        inquiry_id = create_data['id']

        # 删除询价单
        response = client.delete(f'/api/erp/purchase/inquiry/{inquiry_id}')
        assert response.status_code in [200, 204], "删除应成功"

        # 验证已删除
        get_response = client.get(f'/api/erp/purchase/inquiry/{inquiry_id}')
        assert get_response.status_code in [404, 400], "删除后应无法查询到"


# pytest标记
pytestmark = [pytest.mark.erp, pytest.mark.purchase, pytest.mark.inquiry]
