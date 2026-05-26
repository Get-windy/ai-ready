#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 客户管理业务流程集成测试
覆盖客户生命周期管理的完整流程
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.mocks.mock_services import mock_crm, mock_db


@pytest.mark.integration
class TestCustomerManagementIntegration:
    """客户管理业务流程集成测试"""
    
    def test_customer_lifecycle_flow(self):
        """测试客户完整生命周期流程"""
        mock_crm.customers = {}
        mock_crm.leads = {}
        mock_crm.opportunities = {}
        
        # 1. 创建客户
        customer = mock_crm.add_customer({
            'name': '生命周期客户',
            'email': 'lifecycle@test.com',
            'phone': '13800138001',
            'level': 'C',
            'industry': '科技'
        })
        assert customer['id'] > 0
        customer_id = customer['id']
        
        # 2. 创建线索
        lead = mock_crm.add_lead({
            'customer_id': customer_id,
            'name': '购买意向',
            'source': '官网',
            'status': '新线索'
        })
        assert lead['id'] > 0
        
        # 3. 线索跟进
        mock_crm.update_lead(lead['id'], {'status': '跟进中', 'follow_up_count': 1})
        updated_lead = mock_crm.leads[lead['id']]
        assert updated_lead['status'] == '跟进中'
        
        # 4. 线索转化
        conversion = mock_crm.convert_lead(lead['id'])
        assert conversion['success'] == True
        assert mock_crm.leads[lead['id']]['status'] == '已转化'
        
        # 5. 创建商机
        opportunity = mock_crm.add_opportunity({
            'customer_id': customer_id,
            'name': '软件采购项目',
            'amount': 100000,
            'stage': '需求确认',
            'win_rate': 30
        })
        assert opportunity['id'] > 0
        
        # 6. 商机推进
        mock_crm.update_opportunity(opportunity['id'], {
            'stage': '方案演示',
            'win_rate': 60
        })
        
        # 7. 商机成交
        mock_crm.update_opportunity(opportunity['id'], {
            'stage': '成交',
            'win_rate': 100,
            'status': 'won'
        })
        
        # 8. 客户等级提升
        mock_crm.update_customer(customer_id, {'level': 'A'})
        final_customer = mock_crm.customers[customer_id]
        assert final_customer['level'] == 'A'
    
    def test_customer_classification_flow(self):
        """测试客户分类管理流程"""
        mock_crm.customers = {}
        
        # 创建不同等级的客户
        customers = [
            {'name': 'A级客户1', 'level': 'A', 'industry': '金融'},
            {'name': 'A级客户2', 'level': 'A', 'industry': '科技'},
            {'name': 'B级客户1', 'level': 'B', 'industry': '制造'},
            {'name': 'C级客户1', 'level': 'C', 'industry': '零售'}
        ]
        
        for customer_data in customers:
            mock_crm.add_customer(customer_data)
        
        # 按等级筛选
        a_customers = [c for c in mock_crm.customers.values() if c['level'] == 'A']
        b_customers = [c for c in mock_crm.customers.values() if c['level'] == 'B']
        c_customers = [c for c in mock_crm.customers.values() if c['level'] == 'C']
        
        assert len(a_customers) == 2
        assert len(b_customers) == 1
        assert len(c_customers) == 1
    
    def test_lead_management_flow(self):
        """测试线索管理流程"""
        mock_crm.leads = {}
        mock_crm.customers = {}
        
        # 创建客户
        customer = mock_crm.add_customer({'name': '线索客户'})
        
        # 创建多个线索
        leads = [
            {'customer_id': customer['id'], 'name': '线索1', 'source': '官网', 'status': '新线索'},
            {'customer_id': customer['id'], 'name': '线索2', 'source': '展会', 'status': '新线索'},
            {'customer_id': customer['id'], 'name': '线索3', 'source': '推荐', 'status': '跟进中'}
        ]
        
        created_leads = []
        for lead_data in leads:
            lead = mock_crm.add_lead(lead_data)
            created_leads.append(lead)
        
        # 验证线索统计
        all_leads = list(mock_crm.leads.values())
        new_leads = [l for l in all_leads if l['status'] == '新线索']
        following_leads = [l for l in all_leads if l['status'] == '跟进中']
        
        assert len(all_leads) == 3
        assert len(new_leads) == 2
        assert len(following_leads) == 1
    
    def test_opportunity_pipeline_flow(self):
        """测试商机Pipeline流程"""
        mock_crm.opportunities = {}
        mock_crm.customers = {}
        
        # 创建客户
        customer = mock_crm.add_customer({'name': '商机客户'})
        
        # 创建不同阶段的商机
        opportunities = [
            {'customer_id': customer['id'], 'name': '商机1', 'stage': '需求确认', 'amount': 50000},
            {'customer_id': customer['id'], 'name': '商机2', 'stage': '方案演示', 'amount': 80000},
            {'customer_id': customer['id'], 'name': '商机3', 'stage': '商务谈判', 'amount': 120000},
            {'customer_id': customer['id'], 'name': '商机4', 'stage': '成交', 'amount': 200000}
        ]
        
        for opp_data in opportunities:
            mock_crm.add_opportunity(opp_data)
        
        # 验证Pipeline
        all_opps = list(mock_crm.opportunities.values())
        
        stages = {}
        for opp in all_opps:
            stage = opp['stage']
            stages[stage] = stages.get(stage, 0) + 1
        
        assert len(all_opps) == 4
        assert stages.get('需求确认') == 1
        assert stages.get('方案演示') == 1
        assert stages.get('商务谈判') == 1
        assert stages.get('成交') == 1
        
        # 计算Pipeline总值
        total_amount = sum(opp['amount'] for opp in all_opps)
        assert total_amount == 450000


@pytest.mark.integration
class TestCustomerInteractionFlow:
    """客户交互流程集成测试"""
    
    def test_customer_contact_history(self):
        """测试客户联系历史记录"""
        mock_crm.customers = {}
        mock_crm.contacts = {}
        
        # 创建客户
        customer = mock_crm.add_customer({'name': '联系历史客户'})
        
        # 添加联系记录
        contacts = [
            {'customer_id': customer['id'], 'type': '电话', 'content': '初次联系', 'date': '2026-04-01'},
            {'customer_id': customer['id'], 'type': '邮件', 'content': '发送报价', 'date': '2026-04-05'},
            {'customer_id': customer['id'], 'type': '会议', 'content': '产品演示', 'date': '2026-04-10'}
        ]
        
        for contact in contacts:
            mock_crm.add_contact(contact)
        
        # 验证联系历史
        customer_contacts = [c for c in mock_crm.contacts.values() 
                           if c['customer_id'] == customer['id']]
        assert len(customer_contacts) == 3
    
    def test_customer_activity_tracking(self):
        """测试客户活动跟踪"""
        mock_crm.customers = {}
        mock_crm.activities = {}
        
        customer = mock_crm.add_customer({'name': '活动跟踪客户'})
        
        # 记录客户活动
        activities = [
            {'customer_id': customer['id'], 'type': '访问官网', 'timestamp': '2026-04-01 10:00'},
            {'customer_id': customer['id'], 'type': '下载白皮书', 'timestamp': '2026-04-01 10:15'},
            {'customer_id': customer['id'], 'type': '申请试用', 'timestamp': '2026-04-01 10:30'}
        ]
        
        for activity in activities:
            mock_crm.add_activity(activity)
        
        # 验证活动跟踪
        customer_activities = [a for a in mock_crm.activities.values()
                             if a['customer_id'] == customer['id']]
        assert len(customer_activities) == 3