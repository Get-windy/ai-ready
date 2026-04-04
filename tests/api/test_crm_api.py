#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
API自动化测试 - CRM模块API
测试范围: 客户、线索、商机、活动、报表
"""

import pytest
import json
from typing import Dict, Any
import random
from datetime import datetime, timedelta


class TestCRMCustomerAPI:
    """CRM客户API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_customer_list(self, client):
        """[冒烟] 客户列表查询"""
        response = client.get('/api/customer/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
        data = response.json()
        assert 'code' in data
    
    def test_customer_create(self, client):
        """客户创建"""
        customer_data = {
            'name': f'测试客户_{random.randint(1000,9999)}',
            'type': 'enterprise',  # enterprise/personal
            'industry': '制造业',
            'level': 'A',  # A/B/C/D
            'source': '网络推广',
            'phone': f'138{random.randint(10000000,99999999)}',
            'email': f'customer{random.randint(1000,9999)}@test.com',
            'address': '测试地址',
            'remark': '自动化测试创建'
        }
        response = client.post('/api/customer/save', json=customer_data)
        assert response.status_code in [200, 201]
    
    def test_customer_detail(self, client):
        """客户详情查询"""
        response = client.get('/api/customer/detail', params={'id': 1})
        assert response.status_code in [200, 404]
    
    def test_customer_update(self, client):
        """客户更新"""
        update_data = {
            'id': 1,
            'level': 'B',
            'remark': '更新客户等级'
        }
        response = client.put('/api/customer/update', json=update_data)
        assert response.status_code in [200, 204]
    
    def test_customer_search(self, client):
        """客户搜索"""
        response = client.get('/api/customer/search', params={'keyword': '测试'})
        assert response.status_code == 200
    
    def test_customer_level_update(self, client):
        """客户等级变更"""
        response = client.post('/api/customer/level', params={'id': 1, 'level': 'A'})
        assert response.status_code in [200, 400, 404]


class TestCRMLeadAPI:
    """CRM线索API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_lead_list(self, client):
        """[冒烟] 线索列表"""
        response = client.get('/api/lead/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
    
    def test_lead_create(self, client):
        """线索创建"""
        lead_data = {
            'name': f'线索_{random.randint(1000,9999)}',
            'company': '测试公司',
            'phone': f'139{random.randint(10000000,99999999)}',
            'email': f'lead{random.randint(1000,9999)}@test.com',
            'source': '展会',
            'status': 'new',  # new/contacting/qualified/unqualified
            'ownerId': 1,
            'remark': '自动化测试线索'
        }
        response = client.post('/api/lead/save', json=lead_data)
        assert response.status_code in [200, 201]
    
    def test_lead_convert(self, client):
        """线索转化为客户"""
        convert_data = {
            'leadId': 1,
            'createCustomer': True,
            'createOpportunity': True,
            'opportunityName': '转化商机'
        }
        response = client.post('/api/lead/convert', json=convert_data)
        assert response.status_code in [200, 400, 404]
    
    def test_lead_assign(self, client):
        """线索分配"""
        response = client.post('/api/lead/assign', params={'leadId': 1, 'ownerId': 2})
        assert response.status_code in [200, 400, 404]
    
    def test_lead_qualify(self, client):
        """线索合格验证"""
        response = client.post('/api/lead/qualify', params={'leadId': 1})
        assert response.status_code in [200, 400, 404]


class TestCRMOpportunityAPI:
    """CRM商机API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_opportunity_list(self, client):
        """[冒烟] 商机列表"""
        response = client.get('/api/opportunity/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
    
    def test_opportunity_create(self, client):
        """商机创建"""
        opp_data = {
            'name': f'商机_{random.randint(1000,9999)}',
            'customerId': 1,
            'expectedAmount': random.uniform(10000, 100000),
            'expectedDate': (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d'),
            'stage': 'initial',  # initial/negotiation/proposal/closing
            'probability': 30,
            'ownerId': 1,
            'remark': '自动化测试商机'
        }
        response = client.post('/api/opportunity/save', json=opp_data)
        assert response.status_code in [200, 201]
    
    def test_opportunity_stage_update(self, client):
        """商机阶段更新"""
        response = client.post('/api/opportunity/stage', params={
            'id': 1, 
            'stage': 'negotiation',
            'probability': 60
        })
        assert response.status_code in [200, 400, 404]
    
    def test_opportunity_win(self, client):
        """商机赢单"""
        response = client.post('/api/opportunity/win', params={'id': 1})
        assert response.status_code in [200, 400, 404]
    
    def test_opportunity_lose(self, client):
        """商机输单"""
        response = client.post('/api/opportunity/lose', params={'id': 1, 'reason': '价格过高'})
        assert response.status_code in [200, 400, 404]


class TestCRMActivityAPI:
    """CRM活动API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    @pytest.mark.smoke
    def test_activity_list(self, client):
        """[冒烟] 活动列表"""
        response = client.get('/api/activity/page', params={'pageNum': 1, 'pageSize': 10})
        assert response.status_code == 200
    
    def test_activity_create(self, client):
        """活动创建"""
        activity_data = {
            'type': 'call',  # call/meeting/email/visit
            'customerId': 1,
            'subject': '客户跟进电话',
            'content': '自动化测试活动记录',
            'startTime': datetime.now().strftime('%Y-%m-%d %H:%M'),
            'endTime': (datetime.now() + timedelta(hours=1)).strftime('%Y-%m-%d %H:%M'),
            'ownerId': 1
        }
        response = client.post('/api/activity/save', json=activity_data)
        assert response.status_code in [200, 201]
    
    def test_activity_by_customer(self, client):
        """按客户查询活动"""
        response = client.get('/api/activity/customer', params={'customerId': 1})
        assert response.status_code in [200, 404]


class TestCRMReportAPI:
    """CRM报表API测试"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    def test_customer_report(self, client):
        """客户分析报表"""
        response = client.get('/api/report/customer', params={'month': '2026-03'})
        assert response.status_code in [200, 404]
    
    def test_lead_conversion_report(self, client):
        """线索转化报表"""
        response = client.get('/api/report/lead/conversion')
        assert response.status_code in [200, 404]
    
    def test_opportunity_pipeline(self, client):
        """商机管道分析"""
        response = client.get('/api/report/opportunity/pipeline')
        assert response.status_code in [200, 404]
    
    def test_sales_performance(self, client):
        """销售业绩报表"""
        response = client.get('/api/report/sales/performance', params={'month': '2026-03'})
        assert response.status_code in [200, 404]


class TestCRMIntegrationAPI:
    """CRM集成API测试 - 完整业务流程"""
    
    @pytest.fixture(scope='class')
    def client(self, api_client):
        return api_client
    
    def test_lead_to_order_flow(self, client):
        """线索->客户->商机->订单完整流程"""
        # 1. 创建线索
        lead_data = {
            'name': f'流程测试线索_{random.randint(1000,9999)}',
            'company': '流程测试公司',
            'phone': '13800138000',
            'source': '测试',
            'status': 'new'
        }
        lead_resp = client.post('/api/lead/save', json=lead_data)
        assert lead_resp.status_code in [200, 201]
        
        # 2. 线索转化
        if lead_resp.status_code in [200, 201]:
            lead_id = lead_resp.json().get('data', {}).get('id', 1)
            convert_resp = client.post('/api/lead/convert', json={'leadId': lead_id})
            assert convert_resp.status_code in [200, 400, 404]


# pytest标记
pytestmark = pytest.mark.crm